package com.ctdi.llmtc.xtp.traininfer.beans.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author yangla
 * @since 2025/6/6
 */
@Data
public class TaskReq implements Serializable {

    @JsonProperty("task_id")
    private String taskId;

    @JsonProperty("task_type")
    private String taskType;

    @JsonProperty("cluster_zone")
    private String clusterZone;
}
