package com.ctdi.llmtc.xtp.traininfer.beans.param;

import lombok.Data;

/**
 * @author yangla
 * @since 2025/8/1
 */
@Data
public class PodInfo {

    private String podName;

    private String podIp;

    private String hostName;

    private String hostIp;

    private String hostType;

    private Integer hostStatus;

    private String projectSpaceId;

    private Integer cardNum;

    private String runTime;

    private String status;

    private String clusterZone;

}
