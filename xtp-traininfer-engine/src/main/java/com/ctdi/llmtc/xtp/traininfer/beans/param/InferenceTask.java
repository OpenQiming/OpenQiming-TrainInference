package com.ctdi.llmtc.xtp.traininfer.beans.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author yangla
 * @since 2025/6/11
 */
@Data
public class InferenceTask {
    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("use_lora")
    private Boolean useLora;

    @JsonProperty("lora_task_id")
    private String loraTaskId;

    @JsonProperty("quantization")
    private Boolean quantization;

    @JsonProperty("model_template")
    private String modelTemplate;

    @JsonProperty("model_name_or_path")
    private String modelNameOrPath;

    @JsonProperty("template")
    private String template;

    @JsonProperty("adapter_name_or_path")
    private String adapterNameOrPath;
}
