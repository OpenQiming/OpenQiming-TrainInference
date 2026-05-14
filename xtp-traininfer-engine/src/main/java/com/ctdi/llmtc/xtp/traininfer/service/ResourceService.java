package com.ctdi.llmtc.xtp.traininfer.service;

import com.ctdi.llmtc.xtp.traininfer.beans.param.PodInfo;
import com.ctdi.llmtc.xtp.traininfer.beans.req.ResReq;
import com.ctdi.llmtc.xtp.traininfer.beans.req.TaskReq;
import com.ctdi.llmtc.xtp.traininfer.beans.req.TrainReq;
import com.ctdi.llmtc.xtp.traininfer.beans.resp.NodeResp;
import com.ctdi.llmtc.xtp.traininfer.beans.resp.TaskResp;
import com.ctdi.llmtc.xtp.traininfer.util.OperateResult;

import java.util.List;

/**
 * @author yangla
 * @since 2025/6/5
 */
public interface ResourceService {

    /**
     * 查询集群所有节点以及每个节点对应的标签
     *
     * @return 返回节点标签信息
     */
    List<NodeResp> getNodeInfo();

    /**
     * 获取推理任务所占节点及所用卡数
     *
     * @return 推理任务所占节点及所用卡数
     */
    List<TaskResp> getTaskInfo(TaskReq taskReq);

    /**
     * 资源申请
     *
     * @return 申请结果
     */
    OperateResult<String> resSubmit(ResReq resReq);

    /**
     * 资源校验
     *
     * @return 申请结果
     */
    OperateResult<String> resCheck(ResReq resReq);


    /**
     * 资源上报
     *
     * @return 资源列表
     */
    List<PodInfo> resReport(String clusterZone);

}
