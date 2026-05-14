package com.ctdi.llmtc.xtp.traininfer.plugin.deploy;

import com.ctdi.llmtc.xtp.traininfer.beans.param.*;
import com.ctdi.llmtc.xtp.traininfer.beans.req.ResReq;
import com.ctdi.llmtc.xtp.traininfer.beans.req.TaskReq;
import com.ctdi.llmtc.xtp.traininfer.beans.req.TrainReq;
import com.ctdi.llmtc.xtp.traininfer.beans.resp.NodeResp;
import com.ctdi.llmtc.xtp.traininfer.beans.resp.TaskResp;
import com.ctdi.llmtc.xtp.traininfer.util.OperateResult;

import java.io.IOException;
import java.util.List;

/**
 * 部署插件接口
 * 支持不同的部署模式：K8s、Docker等
 *
 * @author yangla
 * @since 2025/1/27
 */
public interface DeployPlugin {

    /**
     * 生成配置文件
     * @param trainReq 训练请求
     * @return 是否成功
     */
    boolean genConfig(TrainReq trainReq);

    /**
     * 部署资源
     * @param trainReq 训练请求
     * @return 是否成功
     */
    boolean deployApps(TrainReq trainReq);

    /**
     * 生成DPO任务配置文件
     * @param dpoTask DPO任务
     * @param op 操作类型
     */
    void genDpoConfig(DPOTask dpoTask, String op);

    /**
     * 生成DPO任务参数文件
     * @param dpoTask DPO任务
     * @param op 操作类型
     * @throws IOException IO异常
     */
    void genDpoParamFile(DPOTask dpoTask, String op) throws IOException;

    /**
     * 生成训练任务配置文件
     * @param trainTask 训练任务
     * @param op 操作类型
     */
    void genTrainConfig(TrainTask trainTask, String op);

    /**
     * 生成训练任务参数文件
     * @param trainTask 训练任务
     * @param op 操作类型
     * @throws IOException IO异常
     */
    void genTrainParamFile(TrainTask trainTask, String op) throws IOException;

    /**
     * 生成评估任务配置文件
     * @param evalTask 评估任务
     * @param op 操作类型
     */
    void genEvalConfig(EvalTask evalTask, String op);

    /**
     * 生成评估任务参数文件
     * @param evalTask 评估任务
     * @param op 操作类型
     * @throws IOException IO异常
     */
    void genEvalParamFile(EvalTask evalTask, String op) throws IOException;

    /**
     * 生成推理任务配置文件
     * @param inferTask 推理任务
     * @param op 操作类型
     * @param projectSpaceId 项目空间ID
     */
    void genInferConfig(InferenceTask inferTask, String op, String projectSpaceId);

    /**
     * 生成推理任务参数文件
     * @param inferTask 推理任务
     * @param op 操作类型
     * @throws IOException IO异常
     */
    void genInferParamFile(InferenceTask inferTask, String op) throws IOException;

    /**
     * 删除训练任务
     * @param taskId 任务ID
     * @return 结果
     */
    OperateResult<String> delTrain(String taskId);

    /**
     * 删除推理任务
     * @param taskId 任务ID
     * @param op 操作类型
     * @return 结果
     */
    OperateResult<String> delInfer(String taskId, String op);

    /**
     * 评估
     * @param trainReq 评估任务
     * @return 结果
     */
    OperateResult<String> eval(TrainReq trainReq);

    /**
     * 推理部署
     * @param trainReq 评估任务
     * @return 结果
     */
    OperateResult<String> inferSubmit(TrainReq trainReq);

    /**
     * 推理状态查询
     * @param taskId 任务ID
     * @return 结果
     */
    String inferStatus(String taskId);

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

    void callback(String clusterZone);
}
