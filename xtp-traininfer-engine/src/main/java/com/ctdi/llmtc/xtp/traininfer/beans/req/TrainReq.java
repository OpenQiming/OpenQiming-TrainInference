package com.ctdi.llmtc.xtp.traininfer.beans.req;

import com.ctdi.llmtc.xtp.traininfer.util.validator.Groups;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author yangla
 * @since 2025/6/6
 */
@Data
public class TrainReq implements Serializable {

    @JsonProperty("task_id")
    @NotNull(message = "taskId不能为空！", groups = {Groups.TRAIN.class, Groups.DPO.class, Groups.EVAL.class, Groups.INFERENCE.class})
    private String taskId;

    @JsonProperty("model_name")
    @NotNull(message = "modelName不能为空！", groups = {Groups.TRAIN.class, Groups.DPO.class, Groups.EVAL.class, Groups.INFERENCE.class})
    private String modelName;

    @JsonProperty("dataset")
    @NotNull(message = "dataset不能为空！", groups = {Groups.TRAIN.class, Groups.DPO.class})
    private String dataset;

    @JsonProperty("num_train_epochs")
    @NotNull(message = "numTrainEpochs不能为空！", groups = {Groups.TRAIN.class, Groups.DPO.class})
    private Integer numTrainEpochs;

    @JsonProperty("warmup_ratio")
    @NotNull(message = "warmupRatio不能为空！", groups = {Groups.TRAIN.class, Groups.DPO.class})
    private Float warmupRatio;

    @JsonProperty("cutoff_len")
    @NotNull(message = "cutoffLen不能为空！", groups = {Groups.TRAIN.class, Groups.DPO.class})
    private Integer cutoffLen;

    @JsonProperty("max_samples")
    @NotNull(message = "maxSamples不能为空！", groups = {Groups.TRAIN.class, Groups.DPO.class})
    private Integer maxSamples;

    @JsonProperty("model_template")
    @NotNull(message = "modelTemplate不能为空！", groups = {Groups.DPO.class, Groups.EVAL.class, Groups.INFERENCE.class})
    private String modelTemplate;

    @JsonProperty("adapter_name_or_path")
    @NotNull(message = "adapterNameOrPath不能为空！", groups = {Groups.DPO.class})
    private String adapterNameOrPath;

    @JsonProperty("learning_rate")
    @NotNull(message = "learningRate不能为空！", groups = {Groups.TRAIN.class})
    private Float learningRate;

    @JsonProperty("per_device_train_batch_size")
    @NotNull(message = "perDeviceTrainBatchSize不能为空！", groups = {Groups.TRAIN.class})
    private Integer perDeviceTrainBatchSize;

    @JsonProperty("gradient_accumulation_steps")
    @NotNull(message = "gradientAccumulationSteps不能为空！", groups = {Groups.TRAIN.class})
    private Integer gradientAccumulationSteps;

    @JsonProperty("strategy_distributed")
    @NotNull(message = "strategyDistributed不能为空！", groups = {Groups.TRAIN.class})
    private Boolean strategyDistributed;

    @JsonProperty("strategy_deepspeed")
    @NotNull(message = "strategyDeepspeed不能为空！", groups = {Groups.TRAIN.class})
    private Integer strategyDeepspeed;

    @JsonProperty("strategy_train")
    @NotNull(message = "strategyTrain不能为空！", groups = {Groups.TRAIN.class})
    private String strategyTrain;

    @JsonProperty("use_lora")
    @NotNull(message = "useLora不能为空！", groups = {Groups.EVAL.class, Groups.INFERENCE.class})
    private Boolean useLora;

    @JsonProperty("lora_task_id")
    @NotNull(message = "loraTaskId不能为空！", groups = {Groups.EVAL.class, Groups.INFERENCE.class})
    private String loraTaskId;

    @JsonProperty("quantization")
    @NotNull(message = "quantization不能为空！", groups = {Groups.INFERENCE.class})
    private Boolean quantization;

    private String op;

    @JsonProperty("cluster_zone")
    @NotNull(message = "集群区域码不能为空！", groups = {Groups.TRAIN.class, Groups.DPO.class, Groups.EVAL.class, Groups.INFERENCE.class})
    private String clusterZone;

    @JsonProperty("project_space_id")
    @NotNull(message = "项目空间ID不能为空！", groups = {Groups.TRAIN.class, Groups.DPO.class, Groups.EVAL.class, Groups.INFERENCE.class})
    private String projectSpaceId;
}
