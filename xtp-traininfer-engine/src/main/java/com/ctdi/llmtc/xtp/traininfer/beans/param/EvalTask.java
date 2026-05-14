package com.ctdi.llmtc.xtp.traininfer.beans.param;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author yangla
 * @since 2025/6/6
 */
@Data
public class EvalTask {

    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("model_name")
    private String modelName;

    @JsonProperty("model_template")
    private String modelTemplate;

    @JsonProperty("use_lora")
    private Boolean useLora;

    @JsonProperty("lora_task_id")
    private String loraTaskId;

    @JsonProperty("save_dir")
    private String saveDir;

    @JsonProperty("model_name_or_path")
    private String modelNameOrPath;

    @JsonProperty("template")
    private String template;

    @JsonProperty("adapter_name_or_path")
    private String adapterNameOrPath;

}
