package com.ctdi.llmtc.xtp.traininfer.constant;

public enum StatusEnum {
    DATASET_NOT_EXIST("数据集不存在"),
    CONFIG_GENERATE_FAIL("部署文件创建失败"),
    CREATE_VS_FAIL("虚拟服务创建失败"),
    DEL_VS_FAIL("虚拟服务删除失败"),
    DEL_INFER_RES_FAIL("删除推理集群资源失败"),
    INFER_CONFIG_GENERATE_FAIL("推理部署文件提交失败"),
    TASK_SUBMIT_FAIL("任务提交失败"),
    IR_FILE_NOT_EXIST("意图训练结果文件不存在"),
    RES_NODE_NOT_EXIST("资源主机不存在"),
    RES_NODE_NOT_AVAILABLE("资源主机不可用"),
    RES_HOSTNAME_HAS_USED("资源主机已经被占用"),
    CLUSTER_ZONE_IS_ERROR("集群区域码不正确"),
    TRAIN_NO_NEED_CHECK("训练任务不校验资源情况"),
    INFER_RES_NOT_ENOUGH("推理资源不足"),
    INFER_CARD_NUM_NOT_FOUND("模型需要的卡数没有找到");

    StatusEnum(String msg) {
        this.msg = msg;
    };

    /**
     * 消息名
     */
    private final String msg;

    public String getMsg() {
        return msg;
    }

}
