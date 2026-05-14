package com.ctdi.llmtc.xtp.traininfer.service.impl;

import com.ctdi.llmtc.xtp.traininfer.beans.param.PodInfo;
import com.ctdi.llmtc.xtp.traininfer.beans.req.ResReq;
import com.ctdi.llmtc.xtp.traininfer.beans.req.TaskReq;
import com.ctdi.llmtc.xtp.traininfer.beans.resp.NodeResp;
import com.ctdi.llmtc.xtp.traininfer.beans.resp.TaskResp;
import com.ctdi.llmtc.xtp.traininfer.plugin.deploy.DeployPlugin;
import com.ctdi.llmtc.xtp.traininfer.plugin.deploy.DeployPluginFactory;
import com.ctdi.llmtc.xtp.traininfer.service.ResourceService;
import com.ctdi.llmtc.xtp.traininfer.util.OperateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author yangla
 * @since 2025/6/5
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "cluster.zone", havingValue = "GZ")
public class ResourceServiceImpl implements ResourceService {
    @Autowired
    private DeployPluginFactory deployPluginFactory;

    @Override
    public List<NodeResp> getNodeInfo() {
        DeployPlugin deployPlugin = deployPluginFactory.getDeployPlugin();
        return deployPlugin.getNodeInfo();
    }

    @Override
    public List<TaskResp> getTaskInfo(TaskReq taskReq) {
        DeployPlugin deployPlugin = deployPluginFactory.getDeployPlugin();
        return deployPlugin.getTaskInfo(taskReq);
    }

    @Override
    public OperateResult<String> resSubmit(ResReq resReq) {
        DeployPlugin deployPlugin = deployPluginFactory.getDeployPlugin();
        return deployPlugin.resSubmit(resReq);
    }

    @Override
    public OperateResult<String> resCheck(ResReq resReq) {
        DeployPlugin deployPlugin = deployPluginFactory.getDeployPlugin();
        return deployPlugin.resCheck(resReq);
    }

    @Override
    public List<PodInfo> resReport(String clusterZone) {
        DeployPlugin deployPlugin = deployPluginFactory.getDeployPlugin();
        return deployPlugin.resReport(clusterZone);
    }
}
