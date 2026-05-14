package com.ctdi.llmtc.xtp.traininfer.service.impl;

import com.ctdi.llmtc.xtp.traininfer.beans.req.ResReq;
import com.ctdi.llmtc.xtp.traininfer.util.OperateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author yangla
 * @since 2025/6/5
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "cluster.zone", havingValue = "CH")
public class CHResourceServiceImpl extends ResourceServiceImpl {

    @Override
    public OperateResult<String> resCheck(ResReq resReq) {
        return OperateResult.success();
    }

}
