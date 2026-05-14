package com.ctdi.llmtc.xtp.traininfer.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ctdi.llmtc.xtp.traininfer.constant.ModelConstants;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.autoscaling.v2.*;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import io.fabric8.kubernetes.client.utils.Serialization;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * k8s操作工具类
 * @author yangla
 * @since 2025/5/28
 */
@Slf4j
public class K8sUtil {

    private static KubernetesClient client;

    private K8sUtil() {
        try {
            String kubeConfigPath = findKubeConfig();
            log.info("kubeConfig file path: {}", kubeConfigPath);
            Config config = Config.fromKubeconfig(FileUtil.readString(new File(kubeConfigPath), StandardCharsets.UTF_8));
            config.setConnectionTimeout(10000);
            config.setRequestTimeout(10000);
            client = new KubernetesClientBuilder().withConfig(config).build();
        } catch (Exception e) {
            log.error("Kubernetes client init fail.", e);
        }
    }

    private static String findKubeConfig() {
        // 1. 检查classpath下的config文件
        URL kubeConfig = ResourceUtil.getResource("kubeConfig");
        if (kubeConfig != null) {
            String classpathConfig = kubeConfig.getPath();
            if (FileUtil.exist(classpathConfig)) {
                return classpathConfig;
            }
        }

        // 2. 检查用户目录下的~/.kube/config
        String userHomeConfig = System.getProperty("user.home") + "/.kube/config";
        if (FileUtil.exist(userHomeConfig)) {
            return userHomeConfig;
        }

        // 3. 检查环境变量KUBECONFIG
        String envConfig = System.getenv("KUBE_CONFIG");
        if (envConfig != null && !envConfig.isEmpty()) {
            return envConfig;
        }

        throw new RuntimeException("未找到有效的Kubernetes配置文件");
    }

    public static K8sUtil getInstance() {
        return K8sUtilHolder.instance;
    }

    private static class K8sUtilHolder {
        private static final K8sUtil instance = new K8sUtil();
    }

    public List<HasMetadata> createFromYml(String yamlPath) {
        try (InputStream is = new FileInputStream(yamlPath)) {
            //return client.load(is).serverSideApply(); // 等价于kubectl apply
            return client.load(is).create();
        } catch (Exception e) {
            log.error("createFromYml error.", e);
            return null;
        }
    }

    public boolean deleteFromYml(String op, String yamlPath) {
        try {
            String namespace = switch (op) {
                case ModelConstants.OP_EVAL, ModelConstants.OP_TRAIN -> ModelConstants.NS_TRAIN;
                case ModelConstants.OP_INFER, ModelConstants.OP_INFER_EVAL -> ModelConstants.NS_INFERENCE;
                default -> throw new IllegalArgumentException("Unsupported operation: " + op);
            };

            if (!FileUtil.exist(yamlPath)) {
                log.info("delete_from_yaml: {} not found", yamlPath);
                return false;
            }

            List<Pod> pods = this.getPodList(namespace);
            List<Service> services = this.getServiceList(namespace);
            List<Job> jobs = this.getJobList(namespace);
            List<Deployment> deployments = this.getDeploymentList(namespace);

            Set<String> podNames = getNames(pods);
            Set<String> serviceNames = getNames(services);
            Set<String> jobNames = getNames(jobs);
            Set<String> deploymentNames = getNames(deployments);

            // 解析 YAML 文件
            try (InputStream is = new FileInputStream(yamlPath)) {
                Iterable<Object> docs = new Yaml().loadAll(is);
                for (Object yamlDoc : docs) {
                    Map<String, Object> doc = (Map<String, Object>) yamlDoc;
                    String apiVersion = (String) doc.get("apiVersion");
                    String kind = (String) doc.get("kind");

                    Map<String, Object> metadata = (Map<String, Object>) doc.get("metadata");
                    String name = (String) metadata.get("name");
                    if (kind == null || name == null) continue;

                    switch (kind) {
                        case "Pod" -> {
                            Optional<String> podToDelete = findMatchingPodName(name, podNames);
                            podToDelete.ifPresent(s -> this.deletePod(namespace, s));
                        }
                        case "Deployment" -> {
                            if (deploymentNames.contains(name)) {
                                this.deleteDeployment(namespace, name);
                            }
                        }
                        case "Job" -> {
                            if (jobNames.contains(name)) {
                                this.deleteJob(namespace, name);
                                Optional<String> podToDelete = findMatchingPodName(name, podNames);
                                podToDelete.ifPresent(s -> this.deletePod(namespace, s));
                            }
                        }
                        case "Service" -> {
                            if (serviceNames.contains(name)) {
                                this.deleteService(namespace, name);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("deleteFromYml error", e);
            return false;
        }
        return true;
    }

    public boolean deleteVolcanoJob(String taskId) {
        try {
            String group = "batch.volcano.sh";
            String version = "v1alpha1";
            String plural = "jobs";
            String supervisorName = "deepspeed-training-supervisor-" + taskId;
            String workerName = "deepspeed-training-worker-" + taskId;
            String serviceName = "deepspeed-training-supervisor-service-" + taskId;
            this.deleteCrd(ModelConstants.NS_TRAIN, plural, version, group, supervisorName);
            this.deleteCrd(ModelConstants.NS_TRAIN, plural, version, group, workerName);
            this.deleteService(ModelConstants.NS_TRAIN, serviceName);
            return true;

        } catch (Exception e) {
            log.error("deleteVolcanoJob error", e);
            return false;
        }
    }

    // 匹配 Pod 名称（类似前缀匹配）
    private Optional<String> findMatchingPodName(String prefix, Set<String> podNames) {
        return podNames.stream()
                .filter(name -> {
                    String[] parts = name.split("-");
                    if (parts.length <= 1) return false;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < parts.length - 1; i++) {
                        sb.append(parts[i]);
                        if (i < parts.length - 2) sb.append("-");
                    }
                    return sb.toString().equals(prefix);
                }).findFirst();
    }


    private <T extends HasMetadata> Set<String> getNames(List<T> items) {
        Set<String> names = new HashSet<>();
        for (T item : items) {
            names.add(item.getMetadata().getName());
        }
        return names;
    }

    public List<Node> getNodeList() {
        return client.nodes().list().getItems();
    }

    public Node getNodeByName(String nodeName) {
        return client.nodes().withName(nodeName).get();
    }

    public Map<String, String> getNodeLabels(String nodeName) {
        Node node = client.nodes().withName(nodeName).get();
        return node.getMetadata().getLabels();
    }
    public List<Node> getNodesByLabels(Map<String, String> labels) {
        return client.nodes()
                .withLabels(labels)
                .list()
                .getItems().stream().toList();
    }

    public List<Node> getNodesBySelector(String labelSelector) {
        return client.nodes()
                .withLabelSelector(labelSelector)
                .list()
                .getItems().stream().toList();
    }

    public List<Node> getUnschedulableNodes() {
        return client.nodes().list().getItems().stream()
                .filter(node -> Boolean.TRUE.equals(
                        Optional.ofNullable(node.getSpec())
                                .map(NodeSpec::getUnschedulable)
                                .orElse(false)
                )).collect(Collectors.toList());
    }

    public Node updateNodeLabels(String nodeName, Map<String, String> labelMap) {
        Node node = client.nodes().withName(nodeName).get();
        node.getMetadata().getLabels().putAll(labelMap);
        return client.nodes().resource(node).update();
    }

    public Node deleteNodeLabel(String nodeName, String labelName) {
        Node node = client.nodes().withName(nodeName).get();
        node.getMetadata().getLabels().remove(labelName);
        return client.nodes().resource(node).update();
    }

    public List<Job> getJobList(String namespace) {
        return client.batch().v1().jobs().inNamespace(namespace).list().getItems();
    }

    public Job getJob(String namespace, String jobName) {
        return client.batch().v1().jobs().inNamespace(namespace).withName(jobName).get();
    }

    public List<StatusDetails> deleteJob(String namespace, String jobName) {
        // DeletionPropagation.ORPHAN 不删除关联的POD
        return client.batch().v1().jobs().inNamespace(namespace).withName(jobName)
                .withPropagationPolicy(DeletionPropagation.ORPHAN).delete();
    }

    public List<Pod> getPodList(String namespace) {
        return client.pods().inNamespace(namespace).list().getItems();
    }

    public Pod getPod(String namespace, String podName) {
        return client.pods().inNamespace(namespace).withName(podName).get();
    }

    public String getPodLog(String namespace, String podName) {
        return client.pods().inNamespace(namespace).withName(podName).getLog();
    }

    public String execCmdInPod(String namespace, String podName,
                                 String containerName, String[] cmd) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final CountDownLatch execLatch = new CountDownLatch(1);
        try {
            client.pods()
                    .inNamespace(namespace)
                    .withName(podName)
                    .inContainer(containerName)
                    .writingOutput(outputStream)
                    .writingError(outputStream)
                    .usingListener(new ExecListener() {
                        @Override
                        public void onOpen() {
                            log.info("Execution started.");
                        }

                        @Override
                        public void onFailure(Throwable t, Response response) {
                            log.error("Execution failed. response: {}", response, t);
                            execLatch.countDown();
                        }

                        @Override
                        public void onClose(int code, String reason) {
                            log.info("Execution completed. code: {}, resson: {}", code, reason);
                            execLatch.countDown();
                        }
                    })
                    .exec(cmd);
            // 等待命令执行完成（可设置超时）
            execLatch.await(10, TimeUnit.SECONDS);
            return outputStream.toString();
        } catch (Exception e) {
            log.error("Failed to execute command in pod.", e);
            return null;
        }
    }

    public List<StatusDetails> deletePod(String namespace, String podName) {
        return client.pods().inNamespace(namespace).withName(podName).delete();
    }

    public Service createService(String namespace, Map<String, Object> yamlDoc) {
        Service service = client.getKubernetesSerialization()
                .convertValue(yamlDoc, Service.class);
        return client.resource(service).inNamespace(namespace).create();
    }

    public List<StatusDetails> deleteService(String namespace, String serviceName) {
        return client.services().inNamespace(namespace).withName(serviceName).delete();
    }

    public List<Service> getServiceList(String namespace) {
        return client.services().inNamespace(namespace).list().getItems();
    }

    public List<Deployment> getDeploymentList(String namespace) {
        return client.apps().deployments().inNamespace(namespace).list().getItems();
    }

    public Deployment getDeployment(String namespace, String deploymentName) {
        return client.apps().deployments().inNamespace(namespace).withName(deploymentName).get();
    }

    public List<StatusDetails> deleteDeployment(String namespace, String deployName) {
        return client.apps().deployments().inNamespace(namespace).withName(deployName).delete();
    }

    public List<GenericKubernetesResource> getCrdList(String namespace, String plural, String version, String group) {
        CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                .withPlural(plural)
                .withVersion(version)
                .withGroup(group)
                .withScope("Namespaced")
                .build();

        return client.genericKubernetesResources(crdContext)
                .inNamespace(namespace).list().getItems();
    }

    public List<StatusDetails> deleteCrd(String namespace, String plural, String version,
                                                     String group, String crdName) {
        CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                .withPlural(plural)
                .withVersion(version)
                .withGroup(group)
                .withScope("Namespaced")
                .build();
        return client.genericKubernetesResources(crdContext).inNamespace(namespace)
                .withName(crdName).delete();
    }

    public List<GenericKubernetesResource> getVsCrdList(String namespace) {
        CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                .withPlural("virtualservices")
                .withVersion("v1alpha3")
                .withGroup("networking.istio.io")
                .withScope("Namespaced")
                .build();

        return client.genericKubernetesResources(crdContext)
                .inNamespace(namespace).list().getItems();
    }

    public GenericKubernetesResource createVsCrd(String namespace, String body) {
        CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                .withPlural("virtualservices")
                .withVersion("v1alpha3")
                .withGroup("networking.istio.io")
                .withScope("Namespaced")
                .build();

        GenericKubernetesResource gkr = Serialization.unmarshal(body, GenericKubernetesResource.class);
        return client.genericKubernetesResources(crdContext)
                .inNamespace(namespace).resource(gkr).create();
    }

    public GenericKubernetesResource updateVsCrd(String namespace, String body) {
        CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                .withPlural("virtualservices")
                .withVersion("v1alpha3")
                .withGroup("networking.istio.io")
                .withScope("Namespaced")
                .build();
        GenericKubernetesResource gkr = Serialization.unmarshal(body, GenericKubernetesResource.class);
        return client.genericKubernetesResources(crdContext)
                .inNamespace(namespace).resource(gkr).patch();
    }

    public GenericKubernetesResource createVcCrd(String namespace, String body) {
        CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                .withPlural("jobs")
                .withVersion("v1alpha1")
                .withGroup("batch.volcano.sh")
                .withScope("Namespaced")
                .build();

        GenericKubernetesResource gkr = Serialization.unmarshal(body, GenericKubernetesResource.class);
        return client.genericKubernetesResources(crdContext)
                .inNamespace(namespace).resource(gkr).create();
    }

    public GenericKubernetesResource updateVcCrd(String namespace, String body) {
        CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                .withPlural("jobs")
                .withVersion("v1alpha1")
                .withGroup("batch.volcano.sh")
                .withScope("Namespaced")
                .build();

        GenericKubernetesResource gkr = Serialization.unmarshal(body, GenericKubernetesResource.class);
        return client.genericKubernetesResources(crdContext)
                .inNamespace(namespace).resource(gkr).update();
    }

    public HorizontalPodAutoscaler createHPA(String namespace, String taskId) {
        HorizontalPodAutoscaler hpa = new HorizontalPodAutoscalerBuilder()
                .withApiVersion("autoscaling/v2")
                .withKind("HorizontalPodAutoscaler")
                .withMetadata(new ObjectMetaBuilder()
                        .withName("hpa-inference-" + taskId)
                        .withNamespace(namespace)
                        .build())
                .withSpec(new HorizontalPodAutoscalerSpecBuilder()
                        .withNewScaleTargetRef()
                        .withApiVersion("apps/v1")
                        .withKind("Deployment")
                        .withName("qwen2-vllm-deployment-" + taskId)
                        .endScaleTargetRef()
                        .withMinReplicas(1)
                        .withMaxReplicas(1)
                        .withMetrics(
                                new MetricSpecBuilder()
                                        .withType("Pods")
                                        .withPods(new PodsMetricSourceBuilder()
                                                .withMetric(new MetricIdentifierBuilder()
                                                        .withName("istio_requests_num")
                                                        .build())
                                                .withTarget(new MetricTargetBuilder()
                                                        .withType("AverageValue")
                                                        .withAverageValue(new Quantity("5"))
                                                        .build())
                                                .build())
                                        .build()
                        )
                        .withBehavior(new HorizontalPodAutoscalerBehaviorBuilder()
                                .withScaleDown(new HPAScalingRulesBuilder()
                                        .withStabilizationWindowSeconds(180)
                                        .withPolicies(
                                                new HPAScalingPolicyBuilder()
                                                        .withType("Percent")
                                                        .withValue(50)
                                                        .withPeriodSeconds(30)
                                                        .build()
                                        )
                                        .build())
                                .withScaleUp(new HPAScalingRulesBuilder()
                                        .withStabilizationWindowSeconds(0)
                                        .withSelectPolicy("Max")
                                        .withPolicies(
                                                new HPAScalingPolicyBuilder()
                                                        .withType("Percent")
                                                        .withValue(50)
                                                        .withPeriodSeconds(15)
                                                        .build(),
                                                new HPAScalingPolicyBuilder()
                                                        .withType("Pods")
                                                        .withValue(2)
                                                        .withPeriodSeconds(15)
                                                        .build()
                                        )
                                        .build())
                                .build())
                        .build())
                .build();

        return client.autoscaling()
                .v2()
                .horizontalPodAutoscalers()
                .inNamespace(namespace)
                .resource(hpa)
                .create();
    }

    public List<StatusDetails> deleteHPA(String namespace, String taskId) {
        return client.autoscaling().v2().horizontalPodAutoscalers()
                .inNamespace(namespace)
                .withName("hpa-inference-" + taskId)
                .withPropagationPolicy(DeletionPropagation.FOREGROUND)
                .withGracePeriod(5L)
                .delete();
    }

    public boolean createVs(String taskId) {
        try {
            List<Service> serviceList = this.getServiceList(ModelConstants.NS_INFERENCE);
            List<String> serviceNames = serviceList.stream()
                    .map(s -> s.getMetadata().getName())
                    .filter(name -> name.startsWith("inference-service-"))
                    .toList();

            List<String> taskIds = serviceNames.stream()
                    .map(name -> name.split("-")[name.split("-").length - 1])
                    .collect(Collectors.toList());
            taskIds.add(taskId);

            String vs = JSONBuilder.buildVs(taskIds);
            log.debug("create vs : {}. taskIds: {}", vs, taskId);

            List<GenericKubernetesResource> vsCrdList = this.getVsCrdList(ModelConstants.NS_INFERENCE);
            if (ObjectUtil.isNotEmpty(vsCrdList)) {
                this.updateVsCrd(ModelConstants.NS_INFERENCE, vs);
            } else {
                this.createVsCrd(ModelConstants.NS_INFERENCE, vs);
            }
        } catch (Exception e) {
            log.error("createVs error.", e);
            return false;
        }
        return true;
    }


    public boolean delVs(String taskId) {
        try {
            List<Service> serviceList = this.getServiceList(ModelConstants.NS_INFERENCE);
            List<String> serviceNames = serviceList.stream()
                    .map(s -> s.getMetadata().getName())
                    .filter(name -> name.startsWith("inference-service-"))
                    .toList();

            List<String> taskIds = serviceNames.stream()
                    .map(name -> name.split("-")[name.split("-").length - 1])
                    .collect(Collectors.toList());
            taskIds.remove(taskId);

            String vs = JSONBuilder.buildVs(taskIds);
            log.debug("delete vs : {}. taskIds: {}", vs, taskIds);

            this.updateVsCrd(ModelConstants.NS_INFERENCE, vs);
        } catch (Exception e) {
            log.error("delVs error.", e);
            return false;
        }
        return true;
    }

    public String getPodStatus(String namespace, String namePrefix) {
        try {
            // 获取所有Pod名称
            List<Pod> podList = this.getPodList(namespace);
            List<String> podNames = podList.stream()
                    .map(Pod::getMetadata)
                    .map(ObjectMeta::getName)
                    .toList();

            // 过滤Pod名称
            List<String> filterPodNames = podNames.stream()
                    .filter(name -> name.contains(namePrefix)).toList();

            if (ObjectUtil.isEmpty(filterPodNames)) {
                return ModelConstants.POD_STATUS_FAILED;
            }

            for (String podName : filterPodNames) {
                Pod pod = this.getPod(namespace, podName);
                PodStatus podStatus = pod.getStatus();
                String phase = podStatus.getPhase(); // Pending, Running, Succeeded, Failed, Unknown
                if (!ModelConstants.POD_STATUS_RUNNING.equals(phase)) {
                    return phase;
                }

                List<ContainerStatus> containerStatuses = podStatus.getContainerStatuses();
                for (ContainerStatus status : containerStatuses) {
                    boolean ready = status.getReady();
                    int restartCount = status.getRestartCount();
                    if (!podName.startsWith(ModelConstants.INFER_DEPLOY_PREFIX)) {
                        continue;
                    }
                    if (!ready && restartCount > ModelConstants.POD_RESTART_LIMIT) {
                        // 删除Pod
                        this.deletePod(namespace, podName);
                        log.info("The pod restart over the threshold. namespace: {}, podName: {}, restartLimt: {}",
                                namespace, podName, ModelConstants.POD_RESTART_LIMIT);
                        return ModelConstants.POD_STATUS_FAILED;
                    }

                    if (!ready && restartCount < ModelConstants.POD_RESTART_LIMIT) {
                        log.info("The pod restart not over the threshold. namespace: {}, podName: {}, restartLimt: {}",
                                namespace, podName, ModelConstants.POD_RESTART_LIMIT);
                        return ModelConstants.POD_STATUS_PENDING;
                    }
                }
            }
            return ModelConstants.POD_STATUS_RUNNING;
        } catch (Exception e) {
            log.error("getPodStatus error.", e);
            return "Error";
        }
    }

}
