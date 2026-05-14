package com.ctdi.llmtc.xtp.traininfer.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * @author yangla
 * @since 2025/6/5
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "cluster.zone", havingValue = "QD")
public class QDTrainServiceImpl extends TrainServiceImpl {

}
