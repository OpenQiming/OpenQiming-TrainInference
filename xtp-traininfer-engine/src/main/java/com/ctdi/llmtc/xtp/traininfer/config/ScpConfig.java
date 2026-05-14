package com.ctdi.llmtc.xtp.traininfer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "intent.scp")
public class ScpConfig {

    /** 目标主机 */
    private String host;

    /** 端口，默认 22 */
    private Integer port = 22;

    /** 用户名 */
    private String username;

    /** 密码（与 privateKeyPath 二选一） */
    private String password;

    /** 私钥路径（与 password 二选一） */
    private String privateKeyPath;

    /** 远端基础目录，例如 /data/saves/output_dir */
    private String remoteBaseDir;

    /** 上传前清理远端 {base}/{taskId} */
    private Boolean cleanBefore = true;
}


