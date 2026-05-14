package com.ctdi.llmtc.xtp.traininfer.beans.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author yangla
 * @since 2025/6/11
 */
@Data
public class TrainTask {
    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("dataset")
    private String dataset;

    @JsonProperty("num_train_epochs")
    private Integer numTrainEpochs;

    @JsonProperty("learning_rate")
    private Float learningRate;

    @JsonProperty("per_device_train_batch_size")
    private Integer perDeviceTrainBatchSize;

    @JsonProperty("gradient_accumulation_steps")
    private Integer gradientAccumulationSteps;

    @JsonProperty("warmup_ratio")
    private Float warmupRatio;

    @JsonProperty("cutoff_len")
    private Integer cutoffLen;

    @JsonProperty("max_samples")
    private Integer maxSamples;

    @JsonProperty("strategy_distributed")
    private Boolean strategyDistributed;

    @JsonProperty("strategy_deepspeed")
    private Integer strategyDeepspeed;

    @JsonProperty("strategy_train")
    private String strategyTrain;

    @JsonProperty("deepspeed")
    private String deepspeed;

    @JsonProperty("model_name_or_path")
    private String modelNameOrPath;

    @JsonProperty("output_dir")
    private String outputDir;
}
