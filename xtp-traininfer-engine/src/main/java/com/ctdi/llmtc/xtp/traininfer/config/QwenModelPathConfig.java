package com.ctdi.llmtc.xtp.traininfer.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * 千问模型路径配置
 */
@Configuration
@ConfigurationProperties(prefix = "model-path.qwen")
@Data
@Component
public class QwenModelPathConfig {

    private String aarch64BaseDir;

    private String otherBaseDir;

    private String aarch64DataSetDir;

    private String otherDataSetDir;

    private String modelOutputDir;

    private String modelInputDir;

    private String templateDir;

    private String intentSyncDir;

}
