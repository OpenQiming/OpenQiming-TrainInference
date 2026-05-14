package com.ctdi.llmtc.xtp.traininfer.beans.req;

import com.ctdi.llmtc.xtp.traininfer.util.validator.Groups;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * @author yangla
 * @since 2025/7/28
 */
@Data
public class ResReq implements Serializable {

    @JsonProperty("model_name")
    @NotNull(message = "model_name不能为空！", groups = {Groups.RES_CHECK.class})
    private String modelName;

    @JsonProperty("project_space_id")
    @NotNull(message = "项目空间不能为空！", groups = {Groups.RES_SUBMIT.class, Groups.RES_CHECK.class})
    private String projectSpaceId;

    @JsonProperty("host_list")
    @NotNull(message = "主机列表不能为空！", groups = {Groups.RES_SUBMIT.class})
    private List<String> hostList;

    @JsonProperty("cluster_zone")
    @NotNull(message = "集群区域不能为空！", groups = {Groups.RES_SUBMIT.class, Groups.RES_CHECK.class})
    private String clusterZone;
}
