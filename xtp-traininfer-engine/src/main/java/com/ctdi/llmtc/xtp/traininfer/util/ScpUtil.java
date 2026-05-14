package com.ctdi.llmtc.xtp.traininfer.util;

import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.extra.ssh.Sftp;
import com.ctdi.llmtc.xtp.traininfer.config.ScpConfig;
import com.jcraft.jsch.JSch;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public class ScpUtil {

    public static void uploadDirectory(ScpConfig cfg, String localDir, String remoteDir) {
        com.jcraft.jsch.Session session = null;
        Sftp sftp = null;
        try {
            if (cfg.getPassword() != null && !cfg.getPassword().isEmpty()) {
                session = JschUtil.getSession(cfg.getHost(), cfg.getPort(), cfg.getUsername(), cfg.getPassword());
            } else if (cfg.getPrivateKeyPath() != null && !cfg.getPrivateKeyPath().isEmpty()) {
                try {
                    JSch jsch = new JSch();
                    jsch.addIdentity(cfg.getPrivateKeyPath());
                    session = jsch.getSession(cfg.getUsername(), cfg.getHost(), cfg.getPort());
                    java.util.Properties config = new java.util.Properties();
                    config.put("StrictHostKeyChecking", "no");
                    session.setConfig(config);
                    session.connect();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new IllegalArgumentException("SCP config missing credentials");
            }

            sftp = new Sftp(session);
            if (Boolean.TRUE.equals(cfg.getCleanBefore())) {
                try {
                    sftp.delDir(remoteDir);
                } catch (Exception e) {
                    log.warn("delete remote dir ignore error. dir: {}", remoteDir);
                }
            }
            sftp.mkDirs(remoteDir);
            File root = new File(localDir);
            if (!root.exists() || !root.isDirectory()) {
                throw new IllegalArgumentException("Local path not a directory: " + localDir);
            }
            uploadRecursively(sftp, root, remoteDir);
        } finally {
            if (sftp != null) {
                try { sftp.close(); } catch (Exception ignore) {}
            }
            if (session != null) {
                try { JschUtil.close(session); } catch (Exception ignore) {}
            }
        }
    }

    private static void uploadRecursively(Sftp sftp, File local, String remoteDir) {
        if (local.isDirectory()) {
            String targetDir = remoteDir + "/" + local.getName();
            sftp.mkDirs(targetDir);
            File[] children = local.listFiles();
            if (children != null) {
                for (File child : children) {
                    uploadRecursively(sftp, child, targetDir);
                }
            }
            return;
        }
        sftp.upload(remoteDir, local);
    }
}


