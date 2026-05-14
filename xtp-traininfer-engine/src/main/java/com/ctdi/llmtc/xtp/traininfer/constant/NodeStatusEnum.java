package com.ctdi.llmtc.xtp.traininfer.constant;

public enum NodeStatusEnum {
    OK(0, "正常"),
    NOT_READY(1, "节点NotReady"),
    PUGIN_NG(2, "Ascend设备插件不异常"),
    AO(3, "节点异常占用");

    NodeStatusEnum(int status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    private final int status;

    private final String msg;

    public int getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }

}
