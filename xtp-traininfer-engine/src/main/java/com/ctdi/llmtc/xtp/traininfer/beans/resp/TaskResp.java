package com.ctdi.llmtc.xtp.traininfer.beans.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Map;

/**
 * @author yangla
 * @since 2025/6/6
 */
@Data
@Accessors(chain = true)
public class TaskResp implements Serializable {

    @JsonProperty("node_name")
    private String nodeName;

    @JsonProperty("gpu_ids")
    private String[] gpuIds;

}
