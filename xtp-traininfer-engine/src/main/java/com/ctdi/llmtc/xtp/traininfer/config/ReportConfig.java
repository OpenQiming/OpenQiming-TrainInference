package com.ctdi.llmtc.xtp.traininfer.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * 千问模型路径配置
 */
@Configuration
@ConfigurationProperties(prefix = "report")
@Data
@Component
public class ReportConfig {

    private String baseUrl;

    private String resPath;

    private String resCron;

    private boolean resEnabled;

    private String callbackTrainPath;

    private String callbackEvalPath;

    private String callbackCron;

    private String callbackMinicpmModelUrl;

    private boolean callbackEnabled;

}
