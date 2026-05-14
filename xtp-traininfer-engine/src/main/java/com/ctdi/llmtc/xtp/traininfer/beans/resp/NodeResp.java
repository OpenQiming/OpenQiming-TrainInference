package com.ctdi.llmtc.xtp.traininfer.beans.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author yangla
 * @since 2025/6/6
 */
@Data
public class NodeResp implements Serializable {

    @JsonProperty("node_name")
    private String nodeName;

    @JsonProperty("node_ip")
    private String nodeIp;

    @JsonProperty("node_labels")
    private Map<String, Boolean> nodeLabels;

}
