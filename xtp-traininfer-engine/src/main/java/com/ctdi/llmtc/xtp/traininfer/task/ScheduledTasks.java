package com.ctdi.llmtc.xtp.traininfer.task;

import com.ctdi.llmtc.xtp.traininfer.beans.param.PodInfo;
import com.ctdi.llmtc.xtp.traininfer.config.ReportConfig;
import com.ctdi.llmtc.xtp.traininfer.plugin.deploy.DeployPlugin;
import com.ctdi.llmtc.xtp.traininfer.plugin.deploy.DeployPluginFactory;
import com.ctdi.llmtc.xtp.traininfer.service.ResourceService;
import com.ctdi.llmtc.xtp.traininfer.service.TrainService;
import com.ctdi.llmtc.xtp.traininfer.util.HttpUtil;
import com.ctdi.llmtc.xtp.traininfer.util.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author yangla
 * @since 2025/8/1
 */
@Slf4j
@Component
public class ScheduledTasks {

    @Value("${cluster.zone:GZ}")
    private String zone;

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private TrainService trainService;

    @Autowired
    private ReportConfig reportConfig;

    @Autowired
    private DeployPluginFactory deployPluginFactory;

    @Scheduled(cron = "#{@reportConfig.resCron}")
    public void resReport() {
        if (!reportConfig.isResEnabled()) {
            log.debug("ScheduledTasks resource report is disabled, Skipping execution.");
            return;
        }

        log.info("ScheduledTasks resource report start.");
        List<PodInfo> podInfoList = resourceService.resReport(zone);
        String url = reportConfig.getBaseUrl() + reportConfig.getResPath();
        String jsonBody = JSONUtil.toJsonStr(podInfoList);
        log.debug("Resource report body: {}", jsonBody);
        HttpUtil.report(url, jsonBody);
        log.info("ScheduledTasks resource report end.");
    }

    @Scheduled(cron = "#{@reportConfig.callbackCron}")
    public void callback() {
        if (!reportConfig.isCallbackEnabled()) {
            log.debug("ScheduledTasks callback report is disabled, Skipping execution.");
            return;
        }
        log.info("ScheduledTasks callback report start.");
        DeployPlugin deployPlugin = deployPluginFactory.getDeployPlugin();
        deployPlugin.callback(zone);

        log.info("ScheduledTasks callback report end.");
    }
}
