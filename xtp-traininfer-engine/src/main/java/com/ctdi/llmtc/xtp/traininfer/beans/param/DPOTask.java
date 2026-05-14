package com.ctdi.llmtc.xtp.traininfer.beans.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author yangla
 * @since 2025/6/11
 */
@Data
public class DPOTask {
    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("dataset")
    private String dataset;

    @JsonProperty("num_train_epochs")
    private Integer numTrainEpochs;

    @JsonProperty("warmup_ratio")
    private Float warmupRatio;

    @JsonProperty("cutoff_len")
    private Integer cutoffLen;

    @JsonProperty("max_samples")
    private Integer maxSamples;

    @JsonProperty("adapter_name_or_path")
    private String adapterNameOrPath;

    @JsonProperty("output_dir")
    private String outputDir;

    @JsonProperty("deepspeed")
    private String deepspeed;

    @JsonProperty("model_name_or_path")
    private String modelNameOrPath;

}
