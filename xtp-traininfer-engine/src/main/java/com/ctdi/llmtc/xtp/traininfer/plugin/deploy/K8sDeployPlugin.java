package com.ctdi.llmtc.xtp.traininfer.plugin.deploy;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ctdi.llmtc.xtp.traininfer.beans.param.*;
import com.ctdi.llmtc.xtp.traininfer.beans.req.ResReq;
import com.ctdi.llmtc.xtp.traininfer.beans.req.TaskReq;
import com.ctdi.llmtc.xtp.traininfer.beans.req.TrainReq;
import com.ctdi.llmtc.xtp.traininfer.beans.resp.NodeResp;
import com.ctdi.llmtc.xtp.traininfer.beans.resp.TaskResp;
import com.ctdi.llmtc.xtp.traininfer.constant.ModelConstants;
import com.ctdi.llmtc.xtp.traininfer.constant.NodeStatusEnum;
import com.ctdi.llmtc.xtp.traininfer.constant.StatusEnum;
import com.ctdi.llmtc.xtp.traininfer.util.*;
import io.fabric8.kubernetes.api.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * K8s部署插件实现
 *
 * @author yangla
 * @since 2025/1/27
 */
@Slf4j
@Component
public class K8sDeployPlugin extends AbstractDeployPlugin {

    /** k8s工具实例 */
    private static final K8sUtil k8sUtil = K8sUtil.getInstance();

    private static final Pattern GPU_PATTERN = Pattern.compile("NVIDIA_VISIBLE_DEVICES=(.+?)\n|ASCEND_VISIBLE_DEVICES=(.+?)\n");

    @Override
    public boolean deployApps(TrainReq trainReq) {
        try {
            String op = trainReq.getOp();
            String yamlFileName = modelPathUtil.getYamlFilePath(trainReq.getTaskId(), op) + "/" + ModelConstants.K8S_CONFIG_FILE;
            if (ModelConstants.X86_ARCH.equals(getRunEnv()) || !ModelConstants.IS_VOLCANO) {
                k8sUtil.createFromYml(yamlFileName);
            } else {
                InputStream inputStream = new FileInputStream(yamlFileName);
                Yaml yaml = new Yaml();
                Iterable<Object> docs = yaml.loadAll(inputStream);
                // 遍历 YAML 文档，找到 Service 部分
                for (Object doc : docs) {
                    Map<String, Object> yamlDoc = (Map<String, Object>) doc;
                    String apiVersion = (String)yamlDoc.get("apiVersion");
                    String kind = (String)yamlDoc.get("kind");
                    if ("batch.volcano.sh/v1alpha1".equals(apiVersion) && "Job".equals(kind)) {
                        k8sUtil.createVcCrd(ModelConstants.NS_TRAIN, JSONUtil.toJsonStr(yamlDoc));
                    } else if ("Service".equals(kind)) {
                        k8sUtil.createService(ModelConstants.NS_TRAIN, yamlDoc);
                    }
                }
            }
        } catch (Exception e) {
            log.info("deployApps error.", e);
            return false;
        }
        return true;
    }

    @Override
    public void genDpoConfig(DPOTask dpoTask, String op) {
        String modelName = dpoTask.getModelName();

        String trainK8sTplName = ModelConstants.TRAIN_K8S_TPL_CONF.get(ModelConstants.TRAIN_OTHER_K8S_TPL);
        if (ModelConstants.QWEN_2_72B.equals(modelName)) {
            trainK8sTplName = ModelConstants.TRAIN_K8S_TPL_CONF.get(ModelConstants.TRAIN_DISTRIBUTED_K8S_TPL);
        }

        // 写入到输出文件
        String trainK8sTplPath = modelPathUtil.getTplDir(op) + trainK8sTplName;
        String dpoDirPath = modelPathUtil.getDpoDirPath(dpoTask.getTaskId());
        String outputYamlPath = dpoDirPath + "/" + ModelConstants.K8S_CONFIG_FILE;
        YmlUtil.genConfig(dpoTask.getTaskId(), trainK8sTplPath, outputYamlPath);
    }

    @Override
    public void genTrainConfig(TrainTask trainTask, String op) {
        boolean strategyDistributed = trainTask.getStrategyDistributed();
        String strategyTrain = trainTask.getStrategyTrain();
        String modelName = trainTask.getModelName();

        String trainK8sTplName = ModelConstants.TRAIN_K8S_TPL_CONF.get(ModelConstants.TRAIN_OTHER_K8S_TPL);
        if (strategyDistributed || ModelConstants.TRAIN_METHOD_FULL.equals(strategyTrain)) {
            trainK8sTplName = ModelConstants.TRAIN_K8S_TPL_CONF.get(ModelConstants.TRAIN_DISTRIBUTED_K8S_TPL);
        } else if (ModelConstants.QWEN_2_72B.equals(modelName)
                || ModelConstants.QWEN_2_5_72B_INSTRUCT.equals(modelName)) {
            trainK8sTplName = ModelConstants.TRAIN_K8S_TPL_CONF.get(ModelConstants.TRAIN_DISTRIBUTED_K8S_TPL);
        }

        // 写入到输出文件
        String trainK8sTplPath = modelPathUtil.getTplDir(op) + trainK8sTplName;
        String trainDirPath = modelPathUtil.getTrainDirPath(trainTask.getTaskId());
        String outputYamlPath = trainDirPath + "/" + ModelConstants.K8S_CONFIG_FILE;
        YmlUtil.genConfig(trainTask.getTaskId(), trainK8sTplPath, outputYamlPath);
    }

    @Override
    public void genEvalConfig(EvalTask evalTask, String op) {
        String modelTemplate = evalTask.getModelTemplate();
        String evalK8sTplName = ModelConstants.EVAL_K8S_TPL_CONF.get(ModelConstants.EVAL_OTHER_K8S_TPL);
        if (ModelConstants.QWEN_2_72B.equals(modelTemplate)) {
            evalK8sTplName = ModelConstants.EVAL_K8S_TPL_CONF.get(ModelConstants.EVAL_8GPU_K8S_TPL);
        }

        // 写入到输出文件
        String trainK8sTplPath = modelPathUtil.getTplDir(op) + evalK8sTplName;
        String evalDirPath = modelPathUtil.getEvalDirPath(evalTask.getTaskId());
        String outputYamlPath = evalDirPath + "/" + ModelConstants.K8S_CONFIG_FILE;
        YmlUtil.genConfig(evalTask.getTaskId(), trainK8sTplPath, outputYamlPath);
    }

    @Override
    public void genInferConfig(InferenceTask inferTask, String op, String projectSpaceId) {
        String modelTemplate = inferTask.getModelTemplate();
        String inferK8sTplName = ModelConstants.INFER_K8S_TPL_CONF.get(modelTemplate);

        // 写入到输出文件
        String inferK8sTplPath = modelPathUtil.getTplDir(op) + inferK8sTplName;
        String inferDirPath = modelPathUtil.getInferenceDirPath(inferTask.getTaskId());
        if (ModelConstants.OP_INFER_EVAL.equals(op)) {
            inferDirPath = modelPathUtil.getInferenceEvalDirPath(inferTask.getTaskId());
        }
        String outputYamlPath = inferDirPath + "/" + ModelConstants.K8S_CONFIG_FILE;
        YmlUtil.genConfig(inferTask, inferK8sTplPath, outputYamlPath, projectSpaceId);
    }

    @Override
    public OperateResult<String> delTrain(String taskId) {
        if (ModelConstants.X86_ARCH.equals(getRunEnv()) || !ModelConstants.IS_VOLCANO) {
            String yamlFileName = modelPathUtil.getYamlFilePath(taskId, ModelConstants.OP_TRAIN) + "/" + ModelConstants.K8S_CONFIG_FILE;
            if (!FileUtil.exist(yamlFileName)) {
                yamlFileName = modelPathUtil.getYamlFilePath(taskId, ModelConstants.OP_DPO) + "/" + ModelConstants.K8S_CONFIG_FILE;
            }

            if (!FileUtil.exist(yamlFileName)) {
                FileUtil.delTrainFile(modelPathUtil, taskId); // python找不到目录也试图删除下
                log.info("delete fail, yml file not found. yamlFileName: {}", yamlFileName);
                return OperateResult.success(); // TODO python逻辑是这样的
            }
            k8sUtil.deleteFromYml(ModelConstants.OP_TRAIN, yamlFileName);
        } else {
            k8sUtil.deleteVolcanoJob(taskId);
        }
        FileUtil.delTrainFile(modelPathUtil, taskId);
        return OperateResult.success();
    }

    @Override
    public OperateResult<String> delInfer(String taskId, String op) {
        k8sUtil.delVs(taskId);
        String yamlFileName = modelPathUtil.getYamlFilePath(taskId, op) + "/" + ModelConstants.K8S_CONFIG_FILE;
        k8sUtil.deleteFromYml(op, yamlFileName);
        k8sUtil.deleteHPA(ModelConstants.NS_INFERENCE, taskId);
        FileUtil.delInferFile(op, modelPathUtil, taskId);
        return OperateResult.success();
    }

    @Override
    public OperateResult<String> eval(TrainReq trainReq) {
        String taskId = trainReq.getTaskId();
        String yamlFileName = modelPathUtil.getYamlFilePath(taskId, trainReq.getOp()) + "/" + ModelConstants.K8S_CONFIG_FILE;
        List<HasMetadata> metadataList = k8sUtil.createFromYml(yamlFileName);
        if (ObjUtil.isEmpty(metadataList)) {
            log.error("submit eval error. {}", StatusEnum.TASK_SUBMIT_FAIL.getMsg());
            FileUtil.delEvalFile(modelPathUtil, taskId);
            return OperateResult.fail(StatusEnum.TASK_SUBMIT_FAIL);
        }
        return OperateResult.success();
    }

    @Override
    public OperateResult<String> inferSubmit(TrainReq trainReq) {
        String taskId = trainReq.getTaskId();
        if (!k8sUtil.createVs(taskId)) {
            return OperateResult.fail(StatusEnum.CREATE_VS_FAIL);
        }

        String yamlFileName = modelPathUtil.getYamlFilePath(taskId, trainReq.getOp()) + "/" + ModelConstants.K8S_CONFIG_FILE;
        List<HasMetadata> metadataList = k8sUtil.createFromYml(yamlFileName);
        if (ObjUtil.isEmpty(metadataList)) {
            k8sUtil.delVs(taskId);
            return OperateResult.fail(StatusEnum.INFER_CONFIG_GENERATE_FAIL);
        }
        k8sUtil.createHPA(ModelConstants.NS_INFERENCE, taskId);
        return OperateResult.success();
    }

    @Override
    public String inferStatus(String taskId) {
        return k8sUtil.getPodStatus(ModelConstants.NS_INFERENCE,
                ModelConstants.INFER_DEPLOY_PREFIX + taskId);
    }

    @Override
    public void genDpoParamFile(DPOTask dpoTask, String op) throws IOException {
        dpoTask.setOutputDir(ModelConstants.SAVE_DIR + dpoTask.getTaskId());
        dpoTask.setAdapterNameOrPath(ModelConstants.SAVE_DIR + dpoTask.getAdapterNameOrPath());

        // model_name_or_path，取决于model_name
        String modelName = dpoTask.getModelName();
        String deepspeed = ModelConstants.DEEPSPEED_CONF2.get(modelName);
        dpoTask.setDeepspeed(deepspeed);

        String modelNameOrPath = ModelConstants.MODEL_CONF.get(modelName);
        dpoTask.setModelNameOrPath(modelNameOrPath);

        String trainParamTplName = ModelConstants.TRAIN_PARAM_TPL_CONF.get(ModelConstants.TRAIN_METHOD_DPO);

        // 写入到输出文件
        String trainParamTplPath = modelPathUtil.getTplDir(op) + trainParamTplName;
        String dpoDirPath = modelPathUtil.getDpoDirPath(dpoTask.getTaskId());
        String outputYamlPath = dpoDirPath + "/" + ModelConstants.TRAIN_PARAM_CONFIG_FILE;
        Map<String, Object> updatesMap = JSONUtil.convertValue(dpoTask);

        // TODO 下面这块逻辑应该不需要，带验证
        //# 针对全参训练后进行dpo的情况 删除lora adapter参数
        //if data['adapter_name_or_path'] == '/app/saves/output_dir/':
        //del data['adapter_name_or_path']

        YmlUtil.genParamConfig(updatesMap, trainParamTplPath, outputYamlPath);
    }

    @Override
    public void genTrainParamFile(TrainTask trainTask, String op) throws IOException {
        trainTask.setOutputDir(ModelConstants.SAVE_DIR + trainTask.getTaskId());

        // deepspeed文件路径，取决于strategy_deepspeed
        String deepspeed = ModelConstants.DEEPSPEED_CONF.get(trainTask.getStrategyDeepspeed());
        trainTask.setDeepspeed(deepspeed);

        // model_name_or_path，取决于model_name
        // 暂不提供qwen1.5_14b, qwen2_72b以外的选项
        // 在qwen2_72b的情况，强制启用zero3
        String modelName = trainTask.getModelName();
        String modelNameOrPath = ModelConstants.MODEL_CONF.get(modelName);
        if (ModelConstants.QWEN_2_72B.equals(modelName) || ModelConstants.QWEN_2_5_72B_INSTRUCT.equals(modelName)) {
            trainTask.setDeepspeed(ModelConstants.DEEPSPEED_CONF.get(3));
        }
        trainTask.setModelNameOrPath(modelNameOrPath);

        // 模板选择，取决于distributed，
        String strategyTrain = trainTask.getStrategyTrain();
        // 目前由于deepspeed torchrun适配单GPU 不对是否启用deepspeed进行区分 统一采用ds模板运行单机单卡 单机双卡 多机多卡
        String trainParamTplName = ModelConstants.TRAIN_PARAM_TPL_CONF.get(strategyTrain);
        if (ModelConstants.TRAIN_METHOD_FULL.equals(strategyTrain)
                && ModelConstants.QWEN_2_1_5B.equals(modelName)) {
            trainParamTplName = ModelConstants.TRAIN_PARAM_TPL_CONF.get(ModelConstants.TRAIN_METHOD_FULL);
        }

        // 写入到输出文件
        String trainParamTplPath = modelPathUtil.getTplDir(op) + trainParamTplName;
        String trainDirPath = modelPathUtil.getTrainDirPath(trainTask.getTaskId());
        String outputYamlPath = trainDirPath + "/" + ModelConstants.TRAIN_PARAM_CONFIG_FILE;
        Map<String, Object> updatesMap = JSONUtil.convertValue(trainTask);
        YmlUtil.genParamConfig(updatesMap, trainParamTplPath, outputYamlPath);
    }

    @Override
    public void genEvalParamFile(EvalTask evalTask, String op) throws IOException {
        String modelName = evalTask.getModelName();

        String modelNameOrPath = ModelConstants.MODEL_CONF.get(modelName);
        evalTask.setModelNameOrPath(modelNameOrPath);
        evalTask.setTemplate("qwen");

        // 对于非基模模型 直接传入用户想要用来推理的已训练任务的taskid
        if (StrUtil.isEmpty(modelNameOrPath)) {
            evalTask.setModelNameOrPath(ModelConstants.SAVE_DIR + modelName);
        }
        evalTask.setSaveDir(ModelConstants.SAVE_DIR + evalTask.getTaskId() + "/eval");

        // 将lora路径进行转换，若不合并lora,该参数不会生效
        evalTask.setAdapterNameOrPath(ModelConstants.SAVE_DIR + evalTask.getLoraTaskId());


        boolean useLora = evalTask.getUseLora();
        String evalParamTplName = ModelConstants.EVAL_PARAM_TPL_CONF.get(ModelConstants.METHOD_OTHER);
        if (useLora) {
            evalParamTplName = ModelConstants.EVAL_PARAM_TPL_CONF.get(ModelConstants.METHOD_USER_LORA);
        }

        // 写入到输出文件
        String evalParamTplPath = modelPathUtil.getTplDir(op) + evalParamTplName;
        String trainDirPath = modelPathUtil.getEvalDirPath(evalTask.getTaskId());
        String outputYamlPath = trainDirPath + "/" + ModelConstants.EVAL_PARAM_CONFIG_FILE;
        Map<String, Object> updatesMap = JSONUtil.convertValue(evalTask);
        YmlUtil.genParamConfig(updatesMap, evalParamTplPath, outputYamlPath);
    }

    @Override
    public void genInferParamFile(InferenceTask inferTask, String op) throws IOException {
        String modelName = inferTask.getModelName();

        String modelNameOrPath = ModelConstants.MODEL_CONF.get(modelName);
        inferTask.setModelNameOrPath(modelNameOrPath);
        inferTask.setTemplate("qwen");

        // 对于非基模模型 直接传入用户想要用来推理的已训练任务的taskid
        if (StrUtil.isEmpty(modelNameOrPath)) {
            inferTask.setModelNameOrPath(ModelConstants.SAVE_DIR + modelName);
        }
        // 将lora路径进行转换，若不合并lora,该参数不会生效
        inferTask.setAdapterNameOrPath(ModelConstants.SAVE_DIR + inferTask.getLoraTaskId());

        boolean useLora = inferTask.getUseLora();
        String inferParamTplName = ModelConstants.INFER_PARAM_TPL_CONF.get(ModelConstants.METHOD_OTHER);
        if (useLora) {
            inferParamTplName = ModelConstants.INFER_PARAM_TPL_CONF.get(ModelConstants.METHOD_USER_LORA);
        }

        // 写入到输出文件
        String inferParamTplPath = modelPathUtil.getTplDir(op) + inferParamTplName;
        String inferDirPath = modelPathUtil.getInferenceDirPath(inferTask.getTaskId());
        if (ModelConstants.OP_INFER_EVAL.equals(op)) {
            inferDirPath = modelPathUtil.getInferenceEvalDirPath(inferTask.getTaskId());
        }
        String outputYamlPath = inferDirPath + "/" + ModelConstants.INFER_PARAM_CONFIG_FILE;
        Map<String, Object> updatesMap = JSONUtil.convertValue(inferTask);
        YmlUtil.genParamConfig(updatesMap, inferParamTplPath, outputYamlPath);
    }

    @Override
    public List<NodeResp> getNodeInfo() {
        List<NodeResp> data = new ArrayList<>();
        List<Node> nodeList = k8sUtil.getNodeList();
        for (Node node : nodeList) {
            // 检查节点是否可调度
            boolean isSchedulingDisabled = node.getStatus().getConditions().stream()
                    .anyMatch(condition -> "SchedulingDisabled".equals(condition.getType()));

            if (isSchedulingDisabled) {
                continue;
            }
            // 获取节点IP
            String ip = node.getStatus().getAddresses().stream()
                    .findFirst()
                    .map(NodeAddress::getAddress)
                    .orElse("");

            // 排除master节点
            if (ObjectUtil.isEmpty(node.getMetadata().getLabels())
                    || node.getMetadata().getName().contains("master")) {
                continue;
            }

            Map<String, Boolean> labelsDict = new HashMap<>();
            Map<String, String> labels = node.getMetadata().getLabels();

            Optional.ofNullable(labels.get("train"))
                    .ifPresent(v -> labelsDict.put("train", "true".equals(v)));
            Optional.ofNullable(labels.get("inference"))
                    .ifPresent(v -> labelsDict.put("inference", "true".equals(v)));
            // 添加节点信息
            NodeResp resp = new NodeResp();
            resp.setNodeName(node.getMetadata().getName());
            resp.setNodeIp(ip);
            resp.setNodeLabels(labelsDict);
            data.add(resp);
        }
        return data;
    }

    @Override
    public List<TaskResp> getTaskInfo(TaskReq taskReq) {
        List<TaskResp> data = new ArrayList<>();

        String taskType = taskReq.getTaskType();
        String taskId = taskReq.getTaskId();
        String namespace = taskType.equals("train") ? ModelConstants.NS_TRAIN : ModelConstants.NS_INFERENCE;

        List<Pod> podList = k8sUtil.getPodList(namespace);
        podList.stream().filter(pod -> pod.getMetadata().getName().contains(taskId)).forEach(p -> {
            String podName = p.getMetadata().getName();
            String containerName = p.getMetadata().getAnnotations()
                    .get("kubectl.kubernetes.io/default-container");
            String[] command = {"/bin/sh", "-c", "env"};
            String output = k8sUtil.execCmdInPod(namespace, podName, containerName, command);

            //output = """
            //            INFERENCE_SERVICE_1854818810332794880_PORT_8000_TCP_ADDR=192.141.91.221
            //            INFERENCE_SERVICE_1862066671378710528_PORT_8000_TCP_PORT=8000
            //            INFERENCE_SERVICE_1878693311079395328_SERVICE_HOST=192.132.164.149
            //            INFERENCE_SERVICE_1869674831576522752_PORT=tcp://192.134.19.71:8000
            //            ASCEND_VISIBLE_DEVICES=5,6
            //            INFERENCE_SERVICE_1862066671378710528_SERVICE_PORT=8000
            //            INFERENCE_SERVICE_1878629340280401920_PORT_8000_TCP_PROTO=tcp
            //            INFERENCE_SERVICE_1877552569998073856_PORT_8000_TCP_PORT=8000
            //        """;

            Matcher matcher = GPU_PATTERN.matcher(output);
            if (matcher.find()) {
                String gpuIdsStr = matcher.group().trim()
                        .replace("ASCEND_VISIBLE_DEVICES=", "")
                        .replace("NVIDIA_VISIBLE_DEVICES=", "");
                String[] gpuIds = gpuIdsStr.split(",");
                TaskResp resp = new TaskResp();
                resp.setNodeName(p.getSpec().getNodeName());
                resp.setGpuIds(gpuIds);
                data.add(resp);
            }
        });
        return data;
    }

    @Override
    public OperateResult<String> resSubmit(ResReq resReq) {
        String projectSpaceId = resReq.getProjectSpaceId();
        List<String> hostList = resReq.getHostList();

        for (String hostName : hostList) {
            Node node = k8sUtil.getNodeByName(hostName);
            if (ObjectUtil.isEmpty(node)) {
                log.info("HostName is not found. hostName: {}", hostName);
                return OperateResult.fail(StatusEnum.RES_NODE_NOT_EXIST);
            }

            if (!isNodeReady(node)) {
                log.info("HostName is not ready. hostName: {}", hostName);
                return OperateResult.fail(StatusEnum.RES_NODE_NOT_AVAILABLE);
            }

            if (node.getSpec().getUnschedulable() != null && node.getSpec().getUnschedulable()) {
                log.info("HostName is not schedulable. hostName: {}", hostName);
                return OperateResult.fail(StatusEnum.RES_NODE_NOT_AVAILABLE);
            }

            if (!hasAscend910(node)) {
                log.info("HostName capacity huawei.com/Ascend910 is zero. hostName: {}", hostName);
                return OperateResult.fail(StatusEnum.RES_NODE_NOT_AVAILABLE);
            }

            Map<String, String> nodeLabels = k8sUtil.getNodeLabels(hostName);
            String ps = nodeLabels.get(ModelConstants.PS_LABEL_KEY);
            if (StrUtil.isNotEmpty(ps)) {
                log.info("Host are already used by project space. host: {}, projectSpace: {}", hostName, ps);
                return OperateResult.fail(StatusEnum.RES_HOSTNAME_HAS_USED);
            }

            String train = nodeLabels.get(ModelConstants.TRAIN_LABEL_KEY);
            if ("true".equals(train)) {
                log.info("Host has been marked as train. host: {}", hostName);
                return OperateResult.fail(StatusEnum.RES_HOSTNAME_HAS_USED);
            }

            String eval = nodeLabels.get(ModelConstants.EVAL_LABEL_KEY);  // 评估归属到训练中
            if ("true".equals(eval)) {
                log.info("Host has been marked as eval. host: {}", hostName);
                return OperateResult.fail(StatusEnum.RES_HOSTNAME_HAS_USED);
            }

            String infer = nodeLabels.get(ModelConstants.INFER_LABEL_KEY);
            if ("true".equals(infer)) {
                log.info("Host has been marked as inference. host: {}", hostName);
                return OperateResult.fail(StatusEnum.RES_HOSTNAME_HAS_USED);
            }
        }

        hostList.forEach(hostName ->
                k8sUtil.updateNodeLabels(hostName, Map.of(ModelConstants.PS_LABEL_KEY, projectSpaceId))
        );
        return OperateResult.success();
    }

    @Override
    public OperateResult<String> resCheck(ResReq resReq) {
        String psId = resReq.getProjectSpaceId();

        // 训练不会长期占用资源,不用校验
        if (ModelConstants.PS_ID_PUBLIC_TRAIN.equals(psId)) {
            log.info("No need to check resources when train. projectSpaceId: [{}]", psId);
            return OperateResult.fail(StatusEnum.TRAIN_NO_NEED_CHECK);
        }

        Map<String, String> labelMap = new HashMap<>();
        if (psId.equals(ModelConstants.PS_ID_PUBLIC_INFER)) {
            labelMap.put(ModelConstants.INFER_LABEL_KEY, "true");
        } else {
            labelMap.put(ModelConstants.PS_LABEL_KEY, psId);
        }

        List<String> nodesByLabels = new ArrayList<>();
        List<Node> nodeList = k8sUtil.getNodesByLabels(labelMap);

        List<String> skipNodes = new ArrayList<>();
        for (Node node : nodeList) {
            String nodeName = node.getMetadata().getName();
            if (!isNodeReady(node)) {
                skipNodes.add(nodeName + " is not ready");
                continue;
            }
            if (node.getSpec().getUnschedulable() != null && node.getSpec().getUnschedulable()) {
                skipNodes.add(nodeName + " is not schedulable");
                continue;
            }

            if (!hasAscend910(node)) {
                skipNodes.add(nodeName + " capacity huawei.com/Ascend910 is zero");
                continue;
            }
            nodesByLabels.add(nodeName);
        }

        log.info("skipNodes list: {}, usedNodes list : {}, projectSpaceId: [{}]", skipNodes, nodesByLabels, psId);

        // 查询推理命名空间中所有的pod
        List<Pod> podList = k8sUtil.getPodList(ModelConstants.NS_INFERENCE);

        List<String> skipPods = new ArrayList<>();
        List<String> podOfCards = new ArrayList<>();
        int usedCardNum = 0;
        for (Pod pod : podList) {
            String podName = pod.getMetadata().getName();
            PodSpec spec = pod.getSpec();
            PodStatus status = pod.getStatus();
            String phase = status.getPhase();
            String nodeName = spec.getNodeName();
            if(!nodesByLabels.contains(nodeName)) {
                continue;
            }

            if (!ModelConstants.POD_STATUS_RUNNING.equals(phase)) {
                skipPods.add(podName + " status is: " + phase);
                continue;
            }

            // nodeSelector标签不过滤了,即使不是合法的标签，也会占用卡,也需要计算占用卡数。
            // 这些pod可能是人为部署，没走训推平台

            int cardNum = this.getCardNum(pod);
            podOfCards.add(podName + "===" + cardNum);

            usedCardNum += cardNum;
        }

        // 需要的卡数
        Integer needCardNum = ModelConstants.MODEL_NEED_CARD_CONF.get(resReq.getModelName());
        if (needCardNum == null) {
            log.info("The card num of model not found. modelName: {}", resReq.getModelName());
            return OperateResult.fail(StatusEnum.INFER_CARD_NUM_NOT_FOUND);
        }

        // 总卡数
        int totalCardNum = nodesByLabels.size() * 8;

        log.info("skipPods list: {}, podOfCards list: {}. projectSpaceId: [{}], totalCardNum: {}, usedCardNum: {}, needCardNum: {}",
                skipPods, podOfCards, psId, totalCardNum, usedCardNum, needCardNum);

        // 剩余卡数
        int leftCardNum = totalCardNum - usedCardNum;
        if (leftCardNum < needCardNum) {
            log.info("Not enough resources. projectSpaceId: [{}], needCardNum: [{}], leftCardNum: [{}], totalCardNum: [{}]",
                    psId, needCardNum, leftCardNum, totalCardNum);
            return OperateResult.fail(StatusEnum.INFER_RES_NOT_ENOUGH);
        }
        return OperateResult.success();
    }

    @Override
    public List<PodInfo> resReport(String clusterZone) {
        List<PodInfo> podInfoList = new ArrayList<>();
        Set<String> hasPodNodeSet = new HashSet<>();
        try {
            Set<String> allNodeList = new HashSet<>();
            Set<String> trainNodeList = k8sUtil.getNodesByLabels(Map.of(ModelConstants.TRAIN_LABEL_KEY, "true"))
                    .stream().map(node -> node.getMetadata().getName()).collect(Collectors.toSet());
            Set<String> evalNodeList = k8sUtil.getNodesByLabels(Map.of(ModelConstants.EVAL_LABEL_KEY, "true"))
                    .stream().map(node -> node.getMetadata().getName()).collect(Collectors.toSet());
            Set<String> inferNodeList = k8sUtil.getNodesByLabels(Map.of(ModelConstants.INFER_LABEL_KEY, "true"))
                    .stream().map(node -> node.getMetadata().getName()).collect(Collectors.toSet());
            Set<String> pjNodeList = k8sUtil.getNodesBySelector(ModelConstants.PS_LABEL_KEY)
                    .stream().map(node -> node.getMetadata().getName()).collect(Collectors.toSet());

            // 不能调度的节点 node.getSpec().getUnschedulable()
            Set<String> unschedulableNodeList = k8sUtil.getUnschedulableNodes().stream()
                    .map(node -> node.getMetadata().getName()).collect(Collectors.toSet());

            allNodeList.addAll(trainNodeList);
            allNodeList.addAll(evalNodeList);
            allNodeList.addAll(inferNodeList);
            allNodeList.addAll(pjNodeList);
            allNodeList.removeAll(unschedulableNodeList);

            Set<String> ngNodeSet = new HashSet<>(); // 被异常占用的节点
            setPodInfoList(allNodeList, podInfoList, ngNodeSet, hasPodNodeSet, clusterZone);

            // 获取没有pod的节点,这些节点有三种情况: 正常的节点，notready节点，插件有问题的节点
            List<String> noPodNodeList = new ArrayList<>(CollUtil.subtract(allNodeList, hasPodNodeSet).stream().toList());
            // 没有pod的节点可能也是被异常占用的节点，需要单独处理，故移除
            noPodNodeList.removeAll(ngNodeSet);

            log.info("allAvailableNodes: {}, trainNodeList: {}, evalNodeList: {}, inferNodeList: {}, pjNodeList: {}, noPodNodeList: {}, ngNodeSet: {}, unschedulableNodeList: {}",
                    allNodeList, trainNodeList, evalNodeList, inferNodeList, pjNodeList, noPodNodeList, ngNodeSet, unschedulableNodeList);
            setNodeInfoList(noPodNodeList, podInfoList, clusterZone);
            // 异常占用的节点
            setNodeInfoList(ngNodeSet, podInfoList, clusterZone);

            // 去除被异常占用的节点(ngNodeSet)的pod
            podInfoList = podInfoList.stream().filter(podInfo -> StrUtil.isEmpty(podInfo.getProjectSpaceId()) || !ngNodeSet.contains(podInfo.getHostName())).toList();
        } catch (Exception e) {
            log.error("resReport has error.", e);
        }
        return podInfoList;
    }

    @Override
    public void callback(String clusterZone) {

    }

    private void setPodInfoList(Set<String> allNodeList, List<PodInfo> podInfoList, Set<String> ngNodeSet, Set<String> hasPodNodeSet, String clusterZone) {
        List<Pod> trainPodList = k8sUtil.getPodList(ModelConstants.NS_TRAIN);
        List<Pod> inferPodList = k8sUtil.getPodList(ModelConstants.NS_INFERENCE);

        List<Pod> podList = new ArrayList<>();
        podList.addAll(trainPodList);
        podList.addAll(inferPodList);

        for (Pod pod : podList) {
            PodInfo podInfo = new PodInfo();
            PodStatus status = pod.getStatus();
            String phase = status.getPhase();
            ObjectMeta metadata = pod.getMetadata();

            if (!ModelConstants.POD_STATUS_RUNNING.equals(phase)) {
                log.debug("Pod [{}] status is [{}] skip, ", metadata.getName(), phase);
                continue;
            }

            podInfo.setPodName(metadata.getName());
            podInfo.setPodIp(status.getPodIP());
            podInfo.setStatus(phase);
            PodSpec spec = pod.getSpec();

            String nodeName = spec.getNodeName();
            // pod所在的节点不在节点列表中，跳过
            if (!allNodeList.contains(nodeName)) {
                continue;
            }
            podInfo.setHostName(nodeName);
            podInfo.setHostType(ModelConstants.CLUSTER_TYPE.get(clusterZone));
            podInfo.setHostIp(status.getHostIP());
            podInfo.setHostStatus(NodeStatusEnum.OK.getStatus());

            // 获取 nodeSelector 标签
            Map<String, String> nodeSelector = spec.getNodeSelector() != null ? spec.getNodeSelector() : Map.of();

            String ps = nodeSelector.get(ModelConstants.PS_LABEL_KEY);
            String projectSpaceId = "";
            if (StrUtil.isNotEmpty(ps)) {
                projectSpaceId = ps;
            }

            String train = nodeSelector.get(ModelConstants.TRAIN_LABEL_KEY);
            if ("true".equals(train)) {
                projectSpaceId = ModelConstants.PS_ID_PUBLIC_TRAIN;
            }

            String eval = nodeSelector.get(ModelConstants.EVAL_LABEL_KEY);
            if ("true".equals(eval)) {
                projectSpaceId = ModelConstants.PS_ID_PUBLIC_TRAIN; // 评估归属到训练中
            }

            String infer = nodeSelector.get(ModelConstants.INFER_LABEL_KEY);
            if ("true".equals(infer)) {
                projectSpaceId = ModelConstants.PS_ID_PUBLIC_INFER;
            }

            // projectSpaceId为空说明有非系统创建的pod,认为是异常节点
            if (StrUtil.isEmpty(projectSpaceId)) {
                ngNodeSet.add(nodeName);
                continue;
            }

            podInfo.setProjectSpaceId(projectSpaceId);

            String ageStr = "";
            String creationTimestamp = pod.getMetadata().getCreationTimestamp();
            if (StrUtil.isNotEmpty(creationTimestamp)) {
                Instant creationInstant = Instant.parse(creationTimestamp);
                java.time.Duration age = java.time.Duration.between(creationInstant, Instant.now());
                ageStr = fmtDuration(age);
            }
            podInfo.setRunTime(ageStr);
            podInfo.setCardNum(getCardNum(pod));
            podInfoList.add(podInfo);

            // 记录有pod的节点名
            hasPodNodeSet.add(podInfo.getHostName());
        }
    }

    private void setNodeInfoList(List<String> noPodNodeList, List<PodInfo> podInfoList, String clusterZone) {
        for (String nodeName : noPodNodeList) {
            PodInfo podInfo = new PodInfo();
            Node node = k8sUtil.getNodeByName(nodeName);
            podInfo.setHostName(node.getMetadata().getName());
            podInfo.setHostType(ModelConstants.CLUSTER_TYPE.get(clusterZone));
            String nodeIp = node.getStatus().getAddresses().stream()
                    .filter(addr -> "InternalIP".equals(addr.getType()))
                    .findFirst()
                    .map(NodeAddress::getAddress)
                    .orElse("");
            podInfo.setHostIp(nodeIp);
            podInfo.setCardNum(8);
            podInfo.setHostStatus(NodeStatusEnum.OK.getStatus());
            if (!isNodeReady(node)) {
                podInfo.setHostStatus(NodeStatusEnum.NOT_READY.getStatus());
            }
            if (node.getSpec().getUnschedulable() != null && node.getSpec().getUnschedulable()) {
                podInfo.setHostStatus(NodeStatusEnum.NOT_READY.getStatus());
            }
            if (!hasAscend910(node)) {
                podInfo.setHostStatus(NodeStatusEnum.PUGIN_NG.getStatus());
            }
            podInfoList.add(podInfo);
        }
    }

    private void setNodeInfoList(Set<String> ngNodeSet, List<PodInfo> podInfoList, String clusterZone) {
        for (String nodeName : ngNodeSet) {
            PodInfo podInfo = new PodInfo();
            Node node = k8sUtil.getNodeByName(nodeName);
            podInfo.setHostName(node.getMetadata().getName());
            podInfo.setHostType(ModelConstants.CLUSTER_TYPE.get(clusterZone));
            String nodeIp = node.getStatus().getAddresses().stream()
                    .filter(addr -> "InternalIP".equals(addr.getType()))
                    .findFirst()
                    .map(NodeAddress::getAddress)
                    .orElse("");
            podInfo.setHostIp(nodeIp);
            podInfo.setCardNum(8);
            podInfo.setHostStatus(NodeStatusEnum.AO.getStatus());
            podInfoList.add(podInfo);
        }
    }

    /**
     * 判断节点是否处于 Ready 状态
     */
    private boolean isNodeReady(Node node) {
        if (node.getStatus() == null || node.getStatus().getConditions() == null) {
            return false;
        }
        return node.getStatus().getConditions().stream()
                .filter(condition -> "Ready".equals(condition.getType()))
                .anyMatch(condition -> "True".equals(condition.getStatus()));
    }


    private boolean hasAscend910(Node node) {
        NodeStatus status = node.getStatus();
        if (status == null) {
            return false;
        }

        // 获取 Capacity 和 Allocatable
        Map<String, Quantity> capacity = status.getCapacity();
        Map<String, Quantity> allocatable = status.getAllocatable();

        // 关键判断 1 & 2: Capacity 和 Allocatable 都必须大于 0
        boolean hasCapacity = getResourceQuantity(capacity) > 0;
        boolean hasAllocatable = getResourceQuantity(allocatable) > 0;
        return hasCapacity && hasAllocatable;
    }

    /**
     * 从资源Map中获取指定资源的数量
     */
    private long getResourceQuantity(Map<String, Quantity> resourceMap) {
        return Optional.ofNullable(resourceMap)
                .map(map -> map.get(ModelConstants.ASCEND_RESOURCE_NAME))
                .map(Quantity::getNumericalAmount)
                .map(Number::longValue)
                .orElse(0L);
    }


    private String fmtDuration(java.time.Duration duration) {
        double seconds = duration.getSeconds();
        if (seconds < 60) {
            return String.format("%.1f", seconds) + "s";
        }
        double minutes = seconds / 60.0;
        if (minutes < 60) {
            return String.format("%.1f", minutes) + "m";
        }
        double hours = minutes / 60.0;
        if (hours < 24) {
            return String.format("%.1f", hours) + "h";
        }
        double days = hours / 24.0;
        return String.format("%.1f", days) + "d";
    }

    private int getCardNum(Pod pod) {
        int cardNum = 0;
        // 遍历 Pod 中的容器
        List<Container> containers = pod.getSpec().getContainers();
        for (Container container : containers) {
            // 获取资源请求（requests）
            Map<String, Quantity> requests = container.getResources().getRequests();
            Quantity npuRequest = requests.get("huawei.com/Ascend910");

            if (npuRequest == null) {
                continue;
            }
            cardNum = Integer.parseInt(npuRequest.getAmount());
            log.debug("Container: {}======{}", container.getName(), cardNum);
            break;
        }
        return cardNum;
    }

}
