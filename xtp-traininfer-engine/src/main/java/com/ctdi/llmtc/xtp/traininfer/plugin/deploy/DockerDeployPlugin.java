package com.ctdi.llmtc.xtp.traininfer.plugin.deploy;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ctdi.llmtc.xtp.traininfer.beans.param.*;
import com.ctdi.llmtc.xtp.traininfer.beans.req.ResReq;
import com.ctdi.llmtc.xtp.traininfer.beans.req.TaskReq;
import com.ctdi.llmtc.xtp.traininfer.beans.req.TrainReq;
import com.ctdi.llmtc.xtp.traininfer.beans.resp.NodeResp;
import com.ctdi.llmtc.xtp.traininfer.beans.resp.TaskResp;
import com.ctdi.llmtc.xtp.traininfer.config.ReportConfig;
import com.ctdi.llmtc.xtp.traininfer.constant.ModelConstants;
import com.ctdi.llmtc.xtp.traininfer.constant.NodeStatusEnum;
import com.ctdi.llmtc.xtp.traininfer.constant.StatusEnum;
import com.ctdi.llmtc.xtp.traininfer.util.FileUtil;
import com.ctdi.llmtc.xtp.traininfer.util.JSONUtil;
import com.ctdi.llmtc.xtp.traininfer.util.OperateResult;
import com.ctdi.llmtc.xtp.traininfer.util.YmlUtil;
import com.ctdi.llmtc.xtp.traininfer.util.ssh.RemoteDockerHost;
import com.ctdi.llmtc.xtp.traininfer.util.ssh.SSHClientHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Docker部署插件实现
 * 通过SSH连接到远程Docker主机执行Docker命令
 *
 * @author yangla
 * @since 2025/1/27
 */
@Slf4j
@Component
public class DockerDeployPlugin extends AbstractDeployPlugin {

    @Value("${cluster.docker-host.ip:127.0.0.1}")
    private String hostIp;

    @Value("${cluster.docker-host.port:22}")
    private int hostPort;

    @Value("${cluster.docker-host.username:root}")
    private String hostUser;

    @Value("${cluster.docker-host.password:}")
    private String hostPwd;

    @Value("${cluster.deploy-mode:k8s}")
    private String deployMode;

    @Autowired
    private ReportConfig reportConfig;

    @Override
    public boolean deployApps(TrainReq trainReq) {
        try {
            String yamlFileName = modelPathUtil.getYamlFilePath(trainReq.getTaskId(), trainReq.getOp()) + "/"
                    + ModelConstants.DOCKER_COMPOSE_CONFIG_FILE;
            this.createFromYml(yamlFileName);
        } catch (Exception e) {
            log.info("deployApps error.", e);
            return false;
        }
        return true;
    }

    @Override
    public void genDpoConfig(DPOTask dpoTask, String op) {
        // 写入到输出文件
        String trainTplPath = modelPathUtil.getTemplateDir(op, deployMode) + ModelConstants.DOCKER_TPL_CONF;
        String dpoDirPath = modelPathUtil.getDpoDirPath(dpoTask.getTaskId());
        String outputYamlPath = dpoDirPath + "/" + ModelConstants.DOCKER_COMPOSE_CONFIG_FILE;
        YmlUtil.genConfig(dpoTask.getTaskId(), trainTplPath, outputYamlPath);
    }

    @Override
    public void genTrainConfig(TrainTask trainTask, String op) {
        // 写入到输出文件
        String trainTplPath = modelPathUtil.getTemplateDir(op, deployMode) + ModelConstants.DOCKER_TPL_CONF;
        String trainDirPath = modelPathUtil.getTrainDirPath(trainTask.getTaskId());
        String outputYamlPath = trainDirPath + "/" + ModelConstants.DOCKER_COMPOSE_CONFIG_FILE;
        YmlUtil.genConfig(trainTask.getTaskId(), trainTplPath, outputYamlPath);
    }

    @Override
    public void genEvalConfig(EvalTask evalTask, String op) {
        // 写入到输出文件
        String trainTplPath = modelPathUtil.getTemplateDir(op, deployMode) + ModelConstants.DOCKER_TPL_CONF;
        String evalDirPath = modelPathUtil.getEvalDirPath(evalTask.getTaskId());
        String outputYamlPath = evalDirPath + "/" + ModelConstants.DOCKER_COMPOSE_CONFIG_FILE;
        YmlUtil.genConfig(evalTask.getTaskId(), trainTplPath, outputYamlPath);
    }

    @Override
    public void genInferConfig(InferenceTask inferTask, String op, String projectSpaceId) {
        // 写入到输出文件
        String inferTplPath = modelPathUtil.getTemplateDir(op, deployMode) + ModelConstants.DOCKER_TPL_CONF;
        String inferDirPath = modelPathUtil.getInferenceDirPath(inferTask.getTaskId());
        if (ModelConstants.OP_INFER_EVAL.equals(op)) {
            inferDirPath = modelPathUtil.getInferenceEvalDirPath(inferTask.getTaskId());
        }
        String outputYamlPath = inferDirPath + "/" + ModelConstants.DOCKER_COMPOSE_CONFIG_FILE;
        YmlUtil.genConfig(inferTask, inferTplPath, outputYamlPath, projectSpaceId);
    }

    @Override
    public OperateResult<String> delTrain(String taskId) {
        String yamlFileName = modelPathUtil.getYamlFilePath(taskId, ModelConstants.OP_TRAIN) + "/" + ModelConstants.DOCKER_COMPOSE_CONFIG_FILE;
        if (!FileUtil.exist(yamlFileName)) {
            yamlFileName = modelPathUtil.getYamlFilePath(taskId, ModelConstants.OP_DPO) + "/" + ModelConstants.DOCKER_COMPOSE_CONFIG_FILE;
        }

        if (!FileUtil.exist(yamlFileName)) {
            FileUtil.delTrainFile(modelPathUtil, taskId); // python找不到目录也试图删除下
            log.info("delete fail, yml file not found. yamlFileName: {}", yamlFileName);
            return OperateResult.success();
        }
        this.deleteFromYml(yamlFileName);
        FileUtil.delTrainFile(modelPathUtil, taskId);
        return OperateResult.success();
    }

    @Override
    public OperateResult<String> delInfer(String taskId, String op) {
        String yamlFileName = modelPathUtil.getYamlFilePath(taskId, op) + "/" + ModelConstants.DOCKER_COMPOSE_CONFIG_FILE;
        this.deleteFromYml(yamlFileName);
        FileUtil.delInferFile(op, modelPathUtil, taskId);
        return OperateResult.success();
    }

    @Override
    public OperateResult<String> eval(TrainReq trainReq) {
        try {
            RemoteDockerHost docker = new RemoteDockerHost(hostIp, hostPort, hostUser, null, hostPwd);
            String op = trainReq.getOp();
            String taskId = trainReq.getTaskId();
            String yamlFileName = modelPathUtil.getYamlFilePath(taskId, op) + "/" + ModelConstants.DOCKER_COMPOSE_CONFIG_FILE;
            SSHClientHelper.ExecResult execResult = docker.dockerComposeUp(yamlFileName, true, Duration.ofSeconds(60));
            log.info("[{}] docker compose deploy success. result: {}", trainReq.getTaskId(), execResult);
            if (execResult.exitCode() != 0 && StrUtil.isNotEmpty(execResult.stderr())) {
                log.error("submit eval error. {}", StatusEnum.TASK_SUBMIT_FAIL.getMsg());
                FileUtil.delEvalFile(modelPathUtil, taskId);
                return OperateResult.fail(StatusEnum.TASK_SUBMIT_FAIL);
            }
            return OperateResult.success();
        } catch (Exception e) {
            log.info("[{}] docker compose deploy success error.", trainReq.getTaskId(), e);
            return OperateResult.fail(StatusEnum.TASK_SUBMIT_FAIL);
        }
    }

    @Override
    public OperateResult<String> inferSubmit(TrainReq trainReq) {
        String taskId = trainReq.getTaskId();
        String yamlFileName = modelPathUtil.getYamlFilePath(taskId, trainReq.getOp()) + "/" + ModelConstants.DOCKER_COMPOSE_CONFIG_FILE;
        SSHClientHelper.ExecResult execResult = this.createFromYml(yamlFileName);
        if (execResult.exitCode() != 0 && StrUtil.isNotEmpty(execResult.stderr())) {
            return OperateResult.fail(StatusEnum.INFER_CONFIG_GENERATE_FAIL);
        }
        return OperateResult.success();
    }

    @Override
    public String inferStatus(String taskId) {
        return this.getDockerStatus("xtp-inference-" + taskId);
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

        // 写入到输出文件
        String trainParamTplPath = modelPathUtil.getTemplateDir(op, deployMode) + ModelConstants.DOCKER_PARAM_LORA_TPL_CONF;
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

        String modelName = trainTask.getModelName();
        String modelNameOrPath = ModelConstants.MODEL_CONF.get(modelName);
        trainTask.setModelNameOrPath(modelNameOrPath);

        // 写入到输出文件
        String trainParamTplPath = modelPathUtil.getTemplateDir(op, deployMode) + ModelConstants.DOCKER_PARAM_LORA_TPL_CONF;
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
        String evalParamTplName = ModelConstants.DOCKER_PARAM_TPL_CONF;
        if (useLora) {
            evalParamTplName = ModelConstants.DOCKER_PARAM_LORA_TPL_CONF;
        }

        // 写入到输出文件
        String evalParamTplPath = modelPathUtil.getTemplateDir(op, deployMode) + evalParamTplName;
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
        String inferParamTplName = ModelConstants.DOCKER_PARAM_TPL_CONF;
        if (useLora) {
            inferParamTplName = ModelConstants.DOCKER_PARAM_LORA_TPL_CONF;
        }

        // 写入到输出文件
        String inferParamTplPath = modelPathUtil.getTemplateDir(op, deployMode) + inferParamTplName;
        String inferDirPath = modelPathUtil.getInferenceDirPath(inferTask.getTaskId());
        if (ModelConstants.OP_INFER_EVAL.equals(op)) {
            inferDirPath = modelPathUtil.getInferenceEvalDirPath(inferTask.getTaskId());
        }
        String outputYamlPath = inferDirPath + "/" + ModelConstants.INFER_PARAM_CONFIG_FILE;
        Map<String, Object> updatesMap = JSONUtil.convertValue(inferTask);
        YmlUtil.genParamConfig(updatesMap, inferParamTplPath, outputYamlPath);
    }

    public SSHClientHelper.ExecResult createFromYml(String yamlPath) {
        try {
            RemoteDockerHost docker = new RemoteDockerHost(hostIp, hostPort, hostUser, null, hostPwd);
            SSHClientHelper.ExecResult execResult = docker.dockerComposeUp(yamlPath, true, Duration.ofSeconds(60));
            log.info("docker compose up success. yml: {}, result: {}",  yamlPath, execResult);
            return execResult;
        } catch (Exception e) {
            log.info("[{}] docker compose up error.", yamlPath, e);
            return null;
        }
    }

    public SSHClientHelper.ExecResult deleteFromYml(String yamlPath) {
        try {
            RemoteDockerHost docker = new RemoteDockerHost(hostIp, hostPort, hostUser, null, hostPwd);
            SSHClientHelper.ExecResult execResult = docker.dockerComposeDown(yamlPath, Duration.ofSeconds(60));
            log.info("docker compose down success. yml: {}, result: {}", yamlPath, execResult);
            return execResult;
        } catch (Exception e) {
            log.info("[{}] docker compose down error.", yamlPath, e);
            return null;
        }
    }

    public String getDockerStatus(String name) {
        try {
            RemoteDockerHost docker = new RemoteDockerHost(hostIp, hostPort, hostUser, null, hostPwd);
            String cmd = "docker ps -a --filter 'name=" + name + "' --format '{{.Status}}'";
            SSHClientHelper.ExecResult execResult = docker.execBash(cmd, Duration.ofSeconds(30), false);
            String output = execResult.stdout();
            if (StrUtil.isEmpty(output)) {
                return "Error";
            }
            if (output.contains("Up")) {
                return ModelConstants.POD_STATUS_RUNNING;
            } else {
                return ModelConstants.POD_STATUS_FAILED;
            }
        } catch (Exception e) {
            log.info("[{}] get docker status error.", name, e);
            return "Error";
        }
    }

    @Override
    public List<NodeResp> getNodeInfo() {
        List<NodeResp> data = new ArrayList<>();
        // 添加节点信息
        NodeResp resp = new NodeResp();
        resp.setNodeName(hostIp);
        resp.setNodeIp(hostIp);
        resp.setNodeLabels(Map.of());
        data.add(resp);
        return data;
    }

    @Override
    public List<TaskResp> getTaskInfo(TaskReq taskReq) {
        List<TaskResp> data = new ArrayList<>();
        TaskResp resp = new TaskResp();
        resp.setNodeName(hostIp);
        resp.setGpuIds(new String[]{"0", "1", "2", "3", "4", "5", "6", "7"});
        data.add(resp);
        return data;
    }

    @Override
    public OperateResult<String> resSubmit(ResReq resReq) {
        return OperateResult.success();
    }

    @Override
    public OperateResult<String> resCheck(ResReq resReq) {
        return OperateResult.success();
    }

    @Override
    public List<PodInfo> resReport(String clusterZone) {
        List<PodInfo> podInfoList = new ArrayList<>();
        try {
            RemoteDockerHost docker = new RemoteDockerHost(hostIp, hostPort, hostUser, null, hostPwd);
            String cmd = "docker ps --filter 'label=com.docker.compose.group=xtp-docker-inference' --format '{{.Status}}|{{.Names}}'";
            SSHClientHelper.ExecResult execResult = docker.execBash(cmd, Duration.ofSeconds(30), false);
            String output = execResult.stdout();
            String[] lines = output.trim().split("\n");
            if (lines.length <= 0) {
                return podInfoList;
            }

            // 跳过表头（第一行）
            for (String s : lines) {
                PodInfo podInfo = new PodInfo();
                podInfo.setClusterZone(clusterZone);
                podInfo.setHostName(hostIp);
                podInfo.setHostType(ModelConstants.CLUSTER_TYPE.get(clusterZone));
                podInfo.setHostIp(hostIp);
                podInfo.setCardNum(8);
                podInfo.setHostStatus(NodeStatusEnum.OK.getStatus());
                podInfo.setPodIp("");
                String line = s.trim();
                if (StrUtil.isEmpty(line)) continue;

                // 按两个及以上空格分割（避免字段内空格被切）
                String[] columns = line.split("\\|", 2);
                if (columns[0].contains("Up")) {
                    podInfo.setStatus(ModelConstants.POD_STATUS_RUNNING);
                    podInfo.setRunTime(columns[0].replace("Up", "").replace("ago", ""));
                }
                podInfo.setProjectSpaceId("");
                podInfo.setPodName(columns[1]);
                podInfoList.add(podInfo);
            }
        } catch (Exception e) {
            log.error("resReport error", e);
        }
        return podInfoList;
    }

    @Override
    public void callback(String clusterZone) {
        try {
            RemoteDockerHost docker = new RemoteDockerHost(hostIp, hostPort, hostUser, null, hostPwd);
            String cmd = "docker ps -a --filter 'label=com.docker.compose.group=xtp-docker-train' --format '{{.Status}}|{{.Names}}'";
            SSHClientHelper.ExecResult execResult = docker.execBash(cmd, Duration.ofSeconds(30), false);
            String output = execResult.stdout();
            String[] lines = output.trim().split("\n");
            if (lines.length <= 0) {
                log.info("No training tasks exist. Msg: {}", output);
                return;
            }

            // 跳过表头（第一行）
            for (String s : lines) {
                String line = s.trim();
                // 按两个及以上空格分割（避免字段内空格被切）
                String[] columns = line.split("\\|", 2);
                log.debug("training task detail: {}", line);
                String status = columns[0];
                String name = columns[1];

                String[] nameArr = name.split("-");
                String taskId = nameArr[nameArr.length - 1];

                if (name.startsWith("xtp-training")) {
                    if (status.contains("Up") || status.contains("Exited (0)")) {
                        Map<String, Object> iterMap = this.loadTrainIterInfo(taskId);
                        log.info("[{}] load train iter info is: {}, status: {}", taskId, iterMap, status);
                        iterMap.put("status", "training");

                        String lossUrl = reportConfig.getBaseUrl() + reportConfig.getCallbackTrainPath();
                        if (status.contains("Exited (0)")) { // Succeeded
                            // 获取loss图描述
                            String trend_result = this.trainLossTrend(taskId, reportConfig.getCallbackMinicpmModelUrl());
                            iterMap.put("lossTrend", trend_result);
                            // 剩余训练时间置0
                            iterMap.put("remainTime", 0);
                            // 若没有loss，即训练失败
                            if (ObjectUtil.isEmpty(iterMap.get("trainingLoss"))
                                    || !FileUtil.exist(modelPathUtil.getModelFilePath(taskId))) {
                                iterMap.put("status", "failed");
                                lossCallback(iterMap, lossUrl, taskId);
                            } else {
                                iterMap.put("status", "completed");
                                String yamlFileName = modelPathUtil.getYamlFilePath(taskId, ModelConstants.OP_TRAIN) + "/" + ModelConstants.DOCKER_COMPOSE_CONFIG_FILE;
                                if (!FileUtil.exist(yamlFileName)) {
                                    yamlFileName = modelPathUtil.getYamlFilePath(taskId, ModelConstants.OP_DPO) + "/" + ModelConstants.DOCKER_COMPOSE_CONFIG_FILE;
                                }
                                this.deleteFromYml(yamlFileName);
                            }
                            log.info("[train:{{}}] [{}] ===> deleted", taskId, status);
                        }
                        if (ObjectUtil.isNotEmpty(iterMap.get("trainingLoss"))) {
                            lossCallback(iterMap, lossUrl, taskId);
                        }
                    }
                } else if (name.startsWith("xtp-eval")) {
                    Map<String, Object> evalResultMap = new HashMap<>();
                    if (status.contains("Up")) {
                        continue;
                    }
                    String yamlFileName = modelPathUtil.getYamlFilePath(taskId, ModelConstants.OP_EVAL) + "/" + ModelConstants.DOCKER_COMPOSE_CONFIG_FILE;
                    if (status.contains("Exited (0)")) { // Succeeded
                        evalResultMap = this.loadEvalLog(taskId);
                        evalResultMap.put("taskId", taskId);
                        evalResultMap.put("status", "completed");
                    } else { // 'Failed','Error'
                        evalResultMap.put("taskId", taskId);
                        evalResultMap.put("status", "failed");
                    }
                    String lossUrl = reportConfig.getBaseUrl() + reportConfig.getCallbackEvalPath();
                    evalCallback(evalResultMap, lossUrl, taskId);
                    this.deleteFromYml(yamlFileName);
                    FileUtil.delEvalFile(modelPathUtil, taskId);
                    log.info("[eval:{{}}] [{}] ===> deleted", taskId, status);
                }
            }
        } catch (Exception e) {
            log.error("callback error", e);
        }
    }
}
