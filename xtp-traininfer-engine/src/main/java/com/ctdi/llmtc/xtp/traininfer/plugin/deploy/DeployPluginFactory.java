package com.ctdi.llmtc.xtp.traininfer.plugin.deploy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 部署插件工厂类
 * 根据配置选择不同的部署插件
 *
 * @author yangla
 * @since 2025/1/27
 */
@Slf4j
@Component
public class DeployPluginFactory {

    @Value("${cluster.deploy-mode:k8s}")
    private String deployMode;

    @Autowired
    private K8sDeployPlugin k8sDeployPlugin;

    @Autowired
    private DockerDeployPlugin dockerDeployPlugin;

    /**
     * 获取部署插件实例
     * @return 部署插件实例
     */
    public DeployPlugin getDeployPlugin() {
        switch (deployMode.toLowerCase()) {
            case "k8s" -> {
                log.info("Using K8s deployment plugin");
                return k8sDeployPlugin;
            }
            case "docker" -> {
                log.info("Using Docker deployment plugin");
                return dockerDeployPlugin;
            }
            default -> {
                log.warn("Unknown deploy mode: {}, falling back to K8s", deployMode);
                return k8sDeployPlugin;
            }
        }
    }

    /**
     * 获取当前部署模式
     * @return 部署模式
     */
    public String getDeployMode() {
        return deployMode;
    }
}
