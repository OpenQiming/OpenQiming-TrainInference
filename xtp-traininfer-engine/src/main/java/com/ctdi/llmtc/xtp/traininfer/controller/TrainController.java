package com.ctdi.llmtc.xtp.traininfer.controller;

import cn.hutool.core.util.StrUtil;
import com.ctdi.llmtc.xtp.traininfer.beans.req.TrainReq;
import com.ctdi.llmtc.xtp.traininfer.constant.ModelConstants;
import com.ctdi.llmtc.xtp.traininfer.constant.StatusEnum;
import com.ctdi.llmtc.xtp.traininfer.service.TrainService;
import com.ctdi.llmtc.xtp.traininfer.util.JSONUtil;
import com.ctdi.llmtc.xtp.traininfer.util.OperateResult;
import com.ctdi.llmtc.xtp.traininfer.util.validator.Groups;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 训练对外接口控制器类。
 *
 * @author yangla
 * @since 2025/5/28
 */
@RestController
@Slf4j
public class TrainController {

    @Value("${cluster.zone:GZ}")
    private String zone;

    @Autowired
    private TrainService trainService;

    @PostMapping("/train/submit")
    public OperateResult<String> trainSubmit(@RequestBody TrainReq trainReq) {
        log.info("[{}] TrainController train submit request. [{}]", trainReq.getTaskId(), JSONUtil.toJSONString(trainReq));

        String clusterZone = trainReq.getClusterZone();
        if (StrUtil.isNotEmpty(clusterZone) && !clusterZone.equals(this.zone)) {
            log.info("[{}] Cluster zone is error. current zone: [{}], request zone: [{}]", trainReq.getTaskId(), this.zone, clusterZone);
            return OperateResult.fail(StatusEnum.CLUSTER_ZONE_IS_ERROR);
        }

        OperateResult<String> result = trainService.trainSubmit(trainReq);
        log.info("[{}] TrainController train submit response. [{}]", trainReq.getTaskId(), JSONUtil.toJSONString(result));
        return result;
    }

    @GetMapping("/train/delete/{taskId}/{clusterZone}")
    public OperateResult<String> delTrain(@PathVariable("taskId") String taskId,
                                          @PathVariable(value = "clusterZone") String clusterZone) {
        log.info("[{}] TrainController train delete request. [{}/{}]", taskId, taskId, clusterZone);

        if (StrUtil.isNotEmpty(clusterZone) && !clusterZone.equals(this.zone)) {
            log.info("[{}] Cluster zone is error. current zone: [{}], request zone: [{}]", taskId, this.zone, clusterZone);
            return OperateResult.fail(StatusEnum.CLUSTER_ZONE_IS_ERROR);
        }

        OperateResult<String> result = trainService.delTrain(taskId);
        log.info("[{}] TrainController train delete response. [{}]", taskId, result);
        return result;
    }

    @PostMapping("/train/eval")
    public OperateResult<String> eval(@Validated(Groups.EVAL.class) @RequestBody TrainReq trainReq) {
        log.info("[{}] TrainController train eval request. [{}]", trainReq.getTaskId(), JSONUtil.toJSONString(trainReq));

        String clusterZone = trainReq.getClusterZone();
        if (StrUtil.isNotEmpty(clusterZone) && !clusterZone.equals(this.zone)) {
            log.info("[{}] Cluster zone is error. current zone: [{}], request zone: [{}]", trainReq.getTaskId(), this.zone, clusterZone);
            return OperateResult.fail(StatusEnum.CLUSTER_ZONE_IS_ERROR);
        }

        OperateResult<String> result = trainService.eval(trainReq);
        log.info("[{}] TrainController train eval response. [{}]", trainReq.getTaskId(), JSONUtil.toJSONString(result));
        return result;
    }

    @PostMapping("/inference/submit")
    public OperateResult<String> inferSubmit(@Validated(Groups.INFERENCE.class) @RequestBody TrainReq trainReq) {
        log.info("[{}] TrainController inference submit request. [{}]", trainReq.getTaskId(), JSONUtil.toJSONString(trainReq));

        String clusterZone = trainReq.getClusterZone();
        if (StrUtil.isNotEmpty(clusterZone) && !clusterZone.equals(this.zone)) {
            log.info("[{}] Cluster zone is error. current zone: [{}], request zone: [{}]", trainReq.getTaskId(), this.zone, clusterZone);
            return OperateResult.fail(StatusEnum.CLUSTER_ZONE_IS_ERROR);
        }
        trainReq.setOp(ModelConstants.OP_INFER);
        OperateResult<String> result = trainService.inferSubmit(trainReq);
        log.info("[{}] TrainController inference submit response. [{}]", trainReq.getTaskId(), JSONUtil.toJSONString(result));
        return result;
    }

    @PostMapping("/inference/eval/submit")
    public OperateResult<String> inferEvalSubmit(@Validated(Groups.INFERENCE.class) @RequestBody TrainReq trainReq) {
        log.info("[{}] TrainController inference eval submit request. [{}]", trainReq.getTaskId(), JSONUtil.toJSONString(trainReq));

        String clusterZone = trainReq.getClusterZone();
        if (StrUtil.isNotEmpty(clusterZone) && !clusterZone.equals(this.zone)) {
            log.info("[{}] Cluster zone is error. current zone: [{}], request zone: [{}]", trainReq.getTaskId(), this.zone, clusterZone);
            return OperateResult.fail(StatusEnum.CLUSTER_ZONE_IS_ERROR);
        }
        trainReq.setOp(ModelConstants.OP_INFER_EVAL);
        OperateResult<String> result = trainService.inferSubmit(trainReq);
        log.info("[{}] TrainController inference eval submit response. [{}]", trainReq.getTaskId(), JSONUtil.toJSONString(result));
        return result;
    }

    @GetMapping("/inference/delete/{taskId}/{clusterZone}")
    public OperateResult<String> delInfer(@PathVariable("taskId") String taskId,
                                          @PathVariable(value = "clusterZone") String clusterZone) {
        log.info("[{}] TrainController inference delete request. [{}/{}]", taskId, taskId, clusterZone);

        if (StrUtil.isNotEmpty(clusterZone) && !clusterZone.equals(this.zone)) {
            log.info("[{}] Cluster zone is error. current zone: [{}], request zone: [{}]", taskId, this.zone, clusterZone);
            return OperateResult.fail(StatusEnum.CLUSTER_ZONE_IS_ERROR);
        }

        OperateResult<String> result = trainService.delInfer(taskId, ModelConstants.OP_INFER);
        log.info("[{}] TrainController inference delete response. [{}]", taskId, result);
        return result;
    }

    @GetMapping("/inference/eval/delete/{taskId}/{clusterZone}")
    public OperateResult<String> delInferEval(@PathVariable("taskId") String taskId,
                                              @PathVariable(value = "clusterZone") String clusterZone) {
        log.info("[{}] TrainController inference eval delete request. [{}/{}]", taskId, taskId, clusterZone);

        if (StrUtil.isNotEmpty(clusterZone) && !clusterZone.equals(this.zone)) {
            log.info("[{}] Cluster zone is error. current zone: [{}], request zone: [{}]", taskId, this.zone, clusterZone);
            return OperateResult.fail(StatusEnum.CLUSTER_ZONE_IS_ERROR);
        }

        OperateResult<String> result = trainService.delInfer(taskId, ModelConstants.OP_INFER_EVAL);
        log.info("[{}] TrainController inference eval delete response. [{}]", taskId, result);
        return result;
    }

    @GetMapping("/inference/status/{taskId}/{clusterZone}")
    public Map<String, String> inferStatus(@PathVariable("taskId") String taskId,
                                           @PathVariable(value = "clusterZone") String clusterZone) {
        log.info("[{}] TrainController inference status request. [{}/{}]", taskId, taskId, clusterZone);

        Map<String, String> result = new HashMap<>();
        if (StrUtil.isNotEmpty(clusterZone) && !clusterZone.equals(this.zone)) {
            log.info("[{}] Cluster zone is error. current zone: [{}], request zone: [{}]", taskId, this.zone, clusterZone);
            return result;
        }

        String podStatus = trainService.inferStatus(taskId);
        result.put("status", podStatus);
        log.info("[{}] TrainController inference status response. [{}]", taskId, result);
        return result;
    }

    @GetMapping("/intent/sync/{taskId}/{clusterZone}")
    public OperateResult<String> syncIntentFile(@PathVariable("taskId") String taskId,
                                                @PathVariable(value = "clusterZone") String clusterZone) {
        log.info("[{}] TrainController sync intent file request. [{}/{}]", taskId, taskId, clusterZone);

        if (StrUtil.isNotEmpty(clusterZone) && !clusterZone.equals(this.zone)) {
            log.info("[{}] Cluster zone is error. current zone: [{}], request zone: [{}]", taskId, this.zone, clusterZone);
            return OperateResult.fail(StatusEnum.CLUSTER_ZONE_IS_ERROR);
        }

        OperateResult<String> result = trainService.intentSync(taskId);
        log.info("[{}] TrainController sync intent file response. [{}]", taskId, result);
        return result;
    }

    @GetMapping("/intent/scp-sync/{taskId}")
    public OperateResult<String> scpSyncIntentFile(@PathVariable("taskId") String taskId) {
        log.info("[{}] TrainController scp sync intent file request. [{}]", taskId, taskId);
        OperateResult<String> result = trainService.intentScpSync(taskId);
        log.info("[{}] TrainController scp sync intent file response. [{}]", taskId, result);
        return result;
    }

}
