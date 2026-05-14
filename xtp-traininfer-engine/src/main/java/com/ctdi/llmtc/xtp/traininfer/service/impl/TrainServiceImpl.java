package com.ctdi.llmtc.xtp.traininfer.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ctdi.llmtc.xtp.traininfer.beans.req.TrainReq;
import com.ctdi.llmtc.xtp.traininfer.config.ScpConfig;
import com.ctdi.llmtc.xtp.traininfer.constant.ModelConstants;
import com.ctdi.llmtc.xtp.traininfer.constant.StatusEnum;
import com.ctdi.llmtc.xtp.traininfer.plugin.deploy.DeployPlugin;
import com.ctdi.llmtc.xtp.traininfer.plugin.deploy.DeployPluginFactory;
import com.ctdi.llmtc.xtp.traininfer.service.TrainService;
import com.ctdi.llmtc.xtp.traininfer.util.*;
import com.ctdi.llmtc.xtp.traininfer.util.validator.Groups;
import com.ctdi.llmtc.xtp.traininfer.util.validator.ValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author yangla
 * @since 2025/6/5
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "cluster.zone", havingValue = "GZ")
public class TrainServiceImpl implements TrainService {

    @Autowired
    private ModelPathUtil modelPathUtil;

    @Autowired
    private DeployPluginFactory deployPluginFactory;

    @Autowired
    private ScpConfig scpConfig;

    @Override
    public OperateResult<String> trainSubmit(TrainReq trainReq) {
        if (StrUtil.isNotEmpty(trainReq.getAdapterNameOrPath())) {
            trainReq.setOp(ModelConstants.OP_DPO);
            // 手动触发校验而不是在controller使用@Validated方式，是因为controller里Groups无法分区是DPO还是TRAIN
            ValidationUtils.validate(trainReq, Groups.DPO.class);
        } else {
            trainReq.setOp(ModelConstants.OP_TRAIN);
            ValidationUtils.validate(trainReq, Groups.TRAIN.class);
        }

        String dataset = trainReq.getDataset();
        String suffix = dataset.substring(dataset.lastIndexOf('.') + 1);
        String fn = dataset.replace("." + suffix, "");

        String matchedDs = ModelConstants.SYSTEM_DATASET.stream()
                .filter(dataset::contains)
                .findFirst()
                .orElse(null);

        if (StrUtil.isNotEmpty(matchedDs)) {
            trainReq.setDataset(matchedDs);
        } else {
            if (!DatasetUtil.convertDataset(modelPathUtil, fn, suffix)) {
                log.error("submit train error. {}", StatusEnum.DATASET_NOT_EXIST.getMsg());
                return OperateResult.fail(StatusEnum.DATASET_NOT_EXIST);
            }
            trainReq.setDataset(fn);
        }

        DeployPlugin deployPlugin = deployPluginFactory.getDeployPlugin();
        if (!deployPlugin.genConfig(trainReq)) {
            log.error("submit train error. {}", StatusEnum.CONFIG_GENERATE_FAIL.getMsg());
            return OperateResult.fail(StatusEnum.CONFIG_GENERATE_FAIL);
        }

        if (!deployPlugin.deployApps(trainReq)) {
            log.error("submit train error. {}", StatusEnum.TASK_SUBMIT_FAIL.getMsg());
            return OperateResult.fail(StatusEnum.TASK_SUBMIT_FAIL);
        }
        return OperateResult.success();
    }

    @Override
    public OperateResult<String> delTrain(String taskId) {
        DeployPlugin deployPlugin = deployPluginFactory.getDeployPlugin();
        return deployPlugin.delTrain(taskId);
    }

    @Override
    public OperateResult<String> delInfer(String taskId, String op) {
        DeployPlugin deployPlugin = deployPluginFactory.getDeployPlugin();
        return deployPlugin.delInfer(taskId, op);
    }

    @Override
    public OperateResult<String> eval(TrainReq trainReq) {
        DeployPlugin deployPlugin = deployPluginFactory.getDeployPlugin();
        ValidationUtils.validate(trainReq, Groups.EVAL.class);

        trainReq.setOp(ModelConstants.OP_EVAL);
        if (!deployPlugin.genConfig(trainReq)) {
            log.error("submit eval error. {}", StatusEnum.CONFIG_GENERATE_FAIL.getMsg());
            return OperateResult.fail(StatusEnum.CONFIG_GENERATE_FAIL);
        }

        return deployPlugin.eval(trainReq);
    }

    @Override
    public OperateResult<String> inferSubmit(TrainReq trainReq) {
        DeployPlugin deployPlugin = deployPluginFactory.getDeployPlugin();
        if (!deployPlugin.genConfig(trainReq)) {
            log.error("submit inference error. {}", StatusEnum.INFER_CONFIG_GENERATE_FAIL.getMsg());
            return OperateResult.fail(StatusEnum.INFER_CONFIG_GENERATE_FAIL);
        }

        return deployPlugin.inferSubmit(trainReq);
    }

    @Override
    public String inferStatus(String taskId) {
        return deployPluginFactory.getDeployPlugin().inferStatus(taskId);
    }

    @Override
    public OperateResult<String> intentSync(String taskId) {
        String srcIntentPath = modelPathUtil.getModelFilePath(taskId);
        log.info("sync intent file start. src_intent_path: {}", srcIntentPath);
        if (!FileUtil.exist(srcIntentPath)) {
            return OperateResult.fail(StatusEnum.IR_FILE_NOT_EXIST);
        }

        String destIntentPath = modelPathUtil.getIntentSyncPath(taskId);
        if (FileUtil.exist(destIntentPath)) {
            FileUtil.rmTree(destIntentPath);
            log.info("delete existing IR file: {}", destIntentPath);
        }

        FileUtil.copyTree(srcIntentPath, destIntentPath);
        log.info("sync intent file success. srcIntentPath: {}", srcIntentPath);
        return OperateResult.success();
    }

    @Override
    public OperateResult<String> intentScpSync(String taskId) {
        String srcIntentPath = modelPathUtil.getModelFilePath(taskId);
        log.info("scp sync intent file start. src_intent_path: {}", srcIntentPath);
        if (!FileUtil.exist(srcIntentPath)) {
            return OperateResult.fail(StatusEnum.IR_FILE_NOT_EXIST);
        }

        String remoteBase = scpConfig.getRemoteBaseDir();
        if (cn.hutool.core.util.StrUtil.isBlank(remoteBase)) {
            log.error("scp sync error. remoteBaseDir is blank in config");
            return OperateResult.fail("remoteBaseDir not configured");
        }
        String remoteTaskDir = (remoteBase.endsWith("/")) ? remoteBase : (remoteBase + "/");
        try {
            ScpUtil.uploadDirectory(scpConfig, srcIntentPath, remoteTaskDir);
        } catch (Exception e) {
            log.error("scp sync error.", e);
            return OperateResult.fail("scp upload failed: " + e.getMessage());
        }
        log.info("scp sync intent file success. remoteTaskDir: {}", remoteTaskDir);
        return OperateResult.success();
    }
}
