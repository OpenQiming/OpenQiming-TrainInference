package com.ctdi.llmtc.xtp.traininfer.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ctdi.llmtc.xtp.traininfer.beans.req.ResReq;
import com.ctdi.llmtc.xtp.traininfer.beans.req.TaskReq;
import com.ctdi.llmtc.xtp.traininfer.beans.resp.NodeResp;
import com.ctdi.llmtc.xtp.traininfer.beans.resp.TaskResp;
import com.ctdi.llmtc.xtp.traininfer.constant.StatusEnum;
import com.ctdi.llmtc.xtp.traininfer.service.ResourceService;
import com.ctdi.llmtc.xtp.traininfer.util.JSONUtil;
import com.ctdi.llmtc.xtp.traininfer.util.OperateResult;
import com.ctdi.llmtc.xtp.traininfer.util.OperateResult2;
import com.ctdi.llmtc.xtp.traininfer.util.validator.Groups;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 资源对外接口控制器类。
 *
 */
@RestController
//@RequestMapping("/resource")
@Slf4j
public class ResourceController {

    @Value("${cluster.zone:GZ}")
    private String zone;

    @Autowired
    private ResourceService resourceService;

    @GetMapping("/nodes/info/{clusterZone}")
    public OperateResult2<List<NodeResp>> queryNodesInfo(@PathVariable(value = "clusterZone", required = false) String clusterZone) {
        log.info("ResourceController nodes info request. [{}]", clusterZone);

        if (StrUtil.isNotEmpty(clusterZone) && !clusterZone.equals(this.zone)) {
            log.info("Cluster zone is error. current zone: [{}], request zone: [{}]", this.zone, clusterZone);
            return OperateResult2.fail(new ArrayList<>());
        }

        List<NodeResp> data = resourceService.getNodeInfo();
        OperateResult2<List<NodeResp>> result;
        if (ObjectUtil.isNotEmpty(data)) {
            result = OperateResult2.success(data);
        } else {
            result = OperateResult2.fail(data);
        }
        log.info("ResourceController nodes info query success");
        return result;
    }

    @PostMapping("/task/info")
    public OperateResult2<List<TaskResp>> queryTaskInfo(@RequestBody TaskReq taskReq) {
        log.info("[{}] ResourceController task info request. [{}]", taskReq.getTaskId(), JSONUtil.toJSONString(taskReq));

        String clusterZone = taskReq.getClusterZone();
        if (StrUtil.isNotEmpty(clusterZone) && !clusterZone.equals(this.zone)) {
            log.info("[{}] Cluster zone is error. current zone: [{}], request zone: [{}]", taskReq.getTaskId(), this.zone, clusterZone);
            return OperateResult2.fail(new ArrayList<>());
        }

        List<TaskResp> data = resourceService.getTaskInfo(taskReq);
        OperateResult2<List<TaskResp>> result;
        if (ObjectUtil.isNotEmpty(data)) {
            result = OperateResult2.success(data);
        } else {
            result = OperateResult2.fail(data);
        }
        log.info("[{}] ResourceController task info response. [{}]", taskReq.getTaskId(), JSONUtil.toJSONString(result));
        return result;
    }

    @PostMapping("/resource/submit")
    public OperateResult<String> resSubmit(@Validated(Groups.RES_SUBMIT.class) @RequestBody ResReq resReq) {
        log.info("ResourceController resource submit request. [{}]", JSONUtil.toJSONString(resReq));

        String clusterZone = resReq.getClusterZone();
        if (StrUtil.isNotEmpty(clusterZone) && !clusterZone.equals(this.zone)) {
            log.info("Cluster zone is error. current zone: [{}], request zone: [{}]", this.zone, clusterZone);
            return OperateResult.fail(StatusEnum.CLUSTER_ZONE_IS_ERROR);
        }

        OperateResult<String> result = resourceService.resSubmit(resReq);
        log.info("ResourceController resource submit response. [{}]", JSONUtil.toJSONString(result));
        return result;
    }

    @PostMapping("/resource/check")
    public OperateResult<String> resCheck(@Validated(Groups.RES_CHECK.class) @RequestBody ResReq resReq) {
        log.info("ResourceController resource check request. [{}]", JSONUtil.toJSONString(resReq));

        String clusterZone = resReq.getClusterZone();
        if (StrUtil.isNotEmpty(clusterZone) && !clusterZone.equals(this.zone)) {
            log.info("Cluster zone is error. current zone: [{}], request zone: [{}]", this.zone, clusterZone);
            return OperateResult.fail(StatusEnum.CLUSTER_ZONE_IS_ERROR);
        }

        OperateResult<String> result = resourceService.resCheck(resReq);
        log.info("ResourceController resource check response. [{}]", JSONUtil.toJSONString(result));
        return result;
    }

}
