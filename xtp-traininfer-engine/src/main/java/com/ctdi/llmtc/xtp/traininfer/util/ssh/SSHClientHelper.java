package com.ctdi.llmtc.xtp.traininfer.util.ssh;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.InMemorySourceFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;

/**
 * 轻量 SSH 工具：负责连接、认证、上传与命令执行（可选择 PTY）。
 */
public class SSHClientHelper implements AutoCloseable {

    private final String host;
    private final int port;
    private final String username;
    private final String privateKeyPath;
    private final String password;
    private SSHClient ssh;

    public SSHClientHelper(String host, int port, String username, String privateKeyPath, String password) {
        this.host = Objects.requireNonNull(host, "host");
        this.port = port <= 0 ? 22 : port;
        this.username = Objects.requireNonNull(username, "username");
        this.privateKeyPath = privateKeyPath;
        this.password = password;
    }

    public void connect() throws IOException {
        ssh = new SSHClient();
        // 生产建议改为固定主机指纹校验
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.connect(host, port);

        if (privateKeyPath != null && !privateKeyPath.isBlank()) {
            ssh.authPublickey(username, privateKeyPath);
        } else if (password != null) {
            ssh.authPassword(username, password);
        } else {
            throw new IllegalArgumentException("Either privateKeyPath or password must be provided.");
        }
    }

    public void uploadTxt(String content, String remotePath, int chmod) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        try (SFTPClient sftp = ssh.newSFTPClient()) {
            InMemorySourceFile src = new InMemorySourceFile() {
                @Override
                public String getName() { return remotePath; }

                @Override
                public long getLength() { return bytes.length; }

                @Override
                public ByteArrayInputStream getInputStream() { return new ByteArrayInputStream(bytes); }
            };
            sftp.put(src, remotePath);
            if (chmod > 0) {
                sftp.chmod(remotePath, chmod);
            }
        }
    }

    public ExecResult exec(String command, Duration timeout, boolean allocatePty) throws IOException {
        try (Session session = ssh.startSession()) {
            if (allocatePty) {
                session.allocatePTY("xterm", 80, 24, 0, 0, Collections.emptyMap());
            }
            try (Session.Command cmd = session.exec(command)) {
                cmd.join(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
                byte[] out = cmd.getInputStream().readAllBytes();
                byte[] err = cmd.getErrorStream().readAllBytes();
                Integer code = cmd.getExitStatus();
                return new ExecResult(code == null ? -1 : code,
                        new String(out, StandardCharsets.UTF_8),
                        new String(err, StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * 使用 SCP 上传本地文件到远程路径，可选 chmod。
     */
    public void uploadFile(String localPath, String remotePath, int chmod) throws IOException {
        ssh.newSCPFileTransfer().upload(new net.schmizz.sshj.xfer.FileSystemFile(localPath), remotePath);
        if (chmod > 0) {
            try (SFTPClient sftp = ssh.newSFTPClient()) {
                sftp.chmod(remotePath, chmod);
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (ssh != null) {
            ssh.disconnect();
            ssh.close();
        }
    }

    public record ExecResult(int exitCode, String stdout, String stderr) { }
}


