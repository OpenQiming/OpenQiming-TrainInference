package com.ctdi.llmtc.xtp.traininfer.util.ssh;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

/**
 * 基于 SSH 的远程 Docker 操作工具。
 * 提供：运行容器、查询容器状态/日志、删除容器等操作。
 */
public class RemoteDockerHost {

    private final String host;
    private final int port;
    private final String username;
    private final String privateKeyPath; // 可为空
    private final String password;       // 可为空

    public RemoteDockerHost(String host, int port, String username, String privateKeyPath, String password) {
        this.host = Objects.requireNonNull(host, "host");
        this.port = port <= 0 ? 22 : port;
        this.username = Objects.requireNonNull(username, "username");
        this.privateKeyPath = privateKeyPath;
        this.password = password;
    }

    public SSHClientHelper newClient() throws IOException {
        SSHClientHelper client = new SSHClientHelper(host, port, username, privateKeyPath, password);
        client.connect();
        return client;
    }

    /**
     * 通过 docker compose 启动服务。
     * 等价于：docker compose -f <yaml> up [-d]
     */
    public SSHClientHelper.ExecResult dockerComposeUp(String composeYamlPath, boolean detach, Duration timeout) throws IOException {
        String cmd = "docker compose -f " + quote(composeYamlPath) + " up" + (detach ? " -d" : "");
        return execBash(cmd, timeout, false);
    }

    /**
     * 通过 docker compose 启动服务（默认 10 分钟超时）。
     */
    public SSHClientHelper.ExecResult dockerComposeUp(String composeYamlPath, boolean detach) throws IOException {
        return dockerComposeUp(composeYamlPath, detach, Duration.ofMinutes(10));
    }

    /**
     * 查询 docker compose 编排的进程状态。
     * 等价于：docker compose -f <yaml> ps
     */
    public SSHClientHelper.ExecResult dockerComposePs(String composeYamlPath, Duration timeout) throws IOException {
        String cmd = "docker compose -f " + quote(composeYamlPath) + " ps -a";
        return execBash(cmd, timeout, false);
    }

    /**
     * 查询 docker compose 编排的进程状态（默认 2 分钟超时）。
     */
    public SSHClientHelper.ExecResult dockerComposePs(String composeYamlPath) throws IOException {
        return dockerComposePs(composeYamlPath, Duration.ofMinutes(2));
    }

    /**
     * 通过 docker compose 停止并清理资源。
     * 等价于：docker compose -f <yaml> down
     */
    public SSHClientHelper.ExecResult dockerComposeDown(String composeYamlPath, Duration timeout) throws IOException {
        String cmd = "docker compose -f " + quote(composeYamlPath) + " down";
        return execBash(cmd, timeout, false);
    }

    /**
     * 通过 docker compose 停止并清理资源（默认 5 分钟超时）。
     */
    public SSHClientHelper.ExecResult dockerComposeDown(String composeYamlPath) throws IOException {
        return dockerComposeDown(composeYamlPath, Duration.ofMinutes(5));
    }

    /**
     * 便捷：上传本地脚本到远端并前台（带 PTY）执行。
     */
    public SSHClientHelper.ExecResult uploadAndRunWithPty(String localScriptPath, String remoteScriptPath, Duration timeout) throws IOException {
        try (SSHClientHelper client = newClient()) {
            client.uploadFile(localScriptPath, remoteScriptPath, 0700);
            String cmd = "bash -lc '" + remoteScriptPath + "'";
            return client.exec(cmd, timeout, true);
        }
    }

    /**
     * 便捷：上传本地脚本到远端并后台执行，日志重定向到远端文件。
     */
    public void uploadAndRunAsync(String localScriptPath, String remoteScriptPath, String logFile) throws IOException {
        try (SSHClientHelper client = newClient()) {
            client.uploadFile(localScriptPath, remoteScriptPath, 0700);
            String cmd = "bash -lc 'nohup " + remoteScriptPath + " > " + logFile + " 2>&1 & disown'";
            client.exec(cmd, Duration.ofMinutes(2), false);
        }
    }

    /**
     * 便捷：上传脚本内容（字符串）到远端并前台（带 PTY）执行。
     */
    public SSHClientHelper.ExecResult uploadAndRunShWithPty(String scriptContent, String remoteScriptPath, Duration timeout) throws IOException {
        try (SSHClientHelper client = newClient()) {
            client.uploadTxt(scriptContent, remoteScriptPath, 0700);
            String cmd = "bash -lc '" + remoteScriptPath + "'";
            return client.exec(cmd, timeout, true);
        }
    }

    /**
     * 便捷：上传脚本内容（字符串）到远端并后台执行，日志重定向到远端文件。
     */
    public void uploadAndRunShAsync(String scriptContent, String remoteScriptPath, String logFile) throws IOException {
        try (SSHClientHelper client = newClient()) {
            client.uploadTxt(scriptContent, remoteScriptPath, 0700);
            String cmd = "bash -lc 'nohup " + remoteScriptPath + " > " + logFile + " 2>&1 & disown'";
            client.exec(cmd, Duration.ofMinutes(2), false);
        }
    }

    /**
     * 上传并执行训练脚本（前台，带 PTY）。
     */
    public SSHClientHelper.ExecResult runWithPty(String scriptContent, String remotePath, Duration timeout) throws IOException {
        try (SSHClientHelper client = newClient()) {
            client.uploadTxt(scriptContent, remotePath, 0700);
            String cmd = "bash -lc '" + remotePath + "'";
            return client.exec(cmd, timeout, true);
        }
    }

    /**
     * 上传并后台执行脚本（日志重定向）。
     */
    public void runAsync(String scriptContent, String remotePath, String logFile) throws IOException {
        try (SSHClientHelper client = newClient()) {
            client.uploadTxt(scriptContent, remotePath, 0700);
            String cmd = "bash -lc 'nohup " + remotePath + " > " + logFile + " 2>&1 & disown'";
            client.exec(cmd, Duration.ofMinutes(2), false);
        }
    }

    /**
     * 直接执行远程命令（bash -lc 包裹），可选择是否分配 PTY。
     * 适合复杂命令（管道/引号）以及需要 -it 的 docker 命令。
     */
    public SSHClientHelper.ExecResult execBash(String bashCommand, Duration timeout, boolean allocatePty) throws IOException {
        try (SSHClientHelper client = newClient()) {
            String cmd = "bash -lc '" + escape(bashCommand) + "'";
            return client.exec(cmd, timeout, allocatePty);
        }
    }

    /**
     * 直接执行远程命令（原样执行，不包裹 bash -lc）。
     */
    public SSHClientHelper.ExecResult execRaw(String command, Duration timeout, boolean allocatePty) throws IOException {
        try (SSHClientHelper client = newClient()) {
            return client.exec(command, timeout, allocatePty);
        }
    }

    /**
     * 便捷：构建不带 TTY 的 LLaMA-Factory 训练命令（适合后台执行）。
     */
    public static String buildLlamaFactoryTrainCommandBackground(String taskId) {
        // 直接复用前台构建后做后台安全转换，确保与前台参数一致性
        String foreground = buildLlamaFactoryTrainCommand(taskId);
        return toBackgroundSafe(foreground);
    }

    /**
     * 将 docker run 命令转换为“后台安全”形式：
     * - 把 "-it" 替换为 "-i"
     * - 若存在独立 "-t"，将其移除
     */
    public static String toBackgroundSafe(String dockerRunCommand) {
        if (dockerRunCommand == null || dockerRunCommand.isBlank()) return dockerRunCommand;
        String cmd = dockerRunCommand;
        // 常见情形：-it（中间或开头结尾都有可能），统一替换为 -i
        cmd = cmd.replaceAll("(?<=\\s)-it(?=\\s)|^-it(?=\\s)|(?<=\\s)-it$", "-i");
        // 独立存在的 -t 删除
        cmd = cmd.replaceAll("(?<=\\s)-t(?=\\s)|^-t(?=\\s)|(?<=\\s)-t$", "");
        // 归一化多余空格
        return cmd.trim().replaceAll("\\s+", " ");
    }

    /**
     * 通用：构建 docker run 命令字符串。
     * - image: 例如 10.238.190.42:18080/library/qwen2_npu:v5
     * - name: 容器名
     * - useTty: 是否添加 -it（前台交互需要）
     * - devices/volumes: 传入 /src:/dst 或 /dev/davinciX
     * - envs: 环境变量键值
     * - network: 例如 host（可为 null 表示不指定）
     * - innerCommand: 容器内命令，例如 /bin/bash -c "echo hi"
     */
    public static String buildDockerRunCommand(
            String image,
            String name,
            boolean useTty,
            List<String> devices,
            List<String> volumes,
            Map<String, String> envs,
            String network,
            String innerCommand,
            boolean privileged,
            boolean removeOnExit
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("docker run ");
        if (useTty) sb.append("-it ");
        if (removeOnExit) sb.append("--rm ");
        if (privileged) sb.append("--privileged ");
        if (name != null && !name.isBlank()) sb.append("--name ").append(quote(name)).append(' ');
        if (network != null && !network.isBlank()) sb.append("--network ").append(network).append(' ');

        if (devices != null) {
            for (String d : devices) {
                if (d != null && !d.isBlank()) sb.append("--device=").append(d).append(' ');
            }
        }
        if (volumes != null) {
            for (String v : volumes) {
                if (v != null && !v.isBlank()) sb.append("-v ").append(v).append(' ');
            }
        }
        if (envs != null) {
            for (Map.Entry<String, String> e : envs.entrySet()) {
                sb.append("-e ").append(e.getKey()).append("=").append(quote(e.getValue())).append(' ');
            }
        }

        sb.append(image).append(' ');
        if (innerCommand != null && !innerCommand.isBlank()) {
            sb.append(innerCommand);
        }
        return sb.toString().trim();
    }

    /**
     * 基于现有 LLaMA-Factory 训练命令的便捷构建器。
     * 仅需传入 taskId（用于拼接 yaml 路径与日志文件名）；其余路径和镜像按现有命令使用默认值。
     */
    public static String buildLlamaFactoryTrainCommand(String taskId) {
        String image = "10.238.190.42:18080/library/qwen2_npu:v5";
        String name = "llamafactory_train";
        boolean useTty = true;
        boolean removeOnExit = true;
        boolean privileged = true;
        String network = "host";

        List<String> devices = new ArrayList<>();
        for (int i = 0; i < 8; i++) devices.add("/dev/davinci" + i);

        List<String> volumes = List.of(
                "/mnt/hpfs/cyc_qwen2_workspace/saves:/app/saves",
                "/mnt/hpfs/cyc_qwen2_workspace/llm_models:/llm_models",
                "/mnt/hpfs/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples:/app/examples",
                "/mnt/hpfs/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/data:/app/data",
                "/usr/local/Ascend/driver:/usr/local/Ascend/driver",
                "/usr/local/Ascend/add-ons/:/usr/local/Ascend/add-ons/",
                "/usr/local/sbin/npu-smi:/usr/local/sbin/npu-smi",
                "/mnt/hpfs/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory:/root/dg/LLaMA-Factory",
                "/var/log/npu:/usr/slog",
                "/mnt/hpfs/models:/mnt/hpfs/models"
        );

        Map<String, String> envs = new LinkedHashMap<>();
        envs.put("FORCE_TORCHRUN", "1");
        envs.put("LD_LIBRARY_PATH",
                "/usr/local/Ascend/ascend-toolkit/latest/tools/aml/lib64:" +
                        "/usr/local/Ascend/ascend-toolkit/latest/tools/aml/lib64/plugin:" +
                        "/usr/local/Ascend/ascend-toolkit/latest/lib64:" +
                        "/usr/local/Ascend/ascend-toolkit/latest/lib64/plugin/opskernel:" +
                        "/usr/local/Ascend/ascend-toolkit/latest/lib64/plugin/nnengine:" +
                        "/usr/local/Ascend/ascend-toolkit/latest/opp/built-in/op_impl/ai_core/tbe/op_tiling/lib/linux/aarch64:" +
                        "/usr/local/Ascend/driver/lib64/common:" +
                        "/usr/local/Ascend/driver/lib64/driver");
        envs.put("ASCEND_TOOLKIT_HOME", "/usr/local/Ascend/ascend-toolkit/latest");
        envs.put("PYTHONPATH",
                "/usr/local/Ascend/ascend-toolkit/latest/python/site-packages:" +
                        "/usr/local/Ascend/ascend-toolkit/latest/opp/built-in/op_impl/ai_core/tbe");
        envs.put("PATH",
                "/usr/local/Ascend/ascend-toolkit/latest/bin:" +
                        "/usr/local/Ascend/ascend-toolkit/latest/compiler/ccec_compiler/bin:" +
                        "/usr/local/Ascend/ascend-toolkit/latest/tools/ccec_compiler/bin:" +
                        "/usr/local/python3.8/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin");
        envs.put("ASCEND_AICPU_PATH", "/usr/local/Ascend/ascend-toolkit/latest");
        envs.put("ASCEND_OPP_PATH", "/usr/local/Ascend/ascend-toolkit/latest/opp");
        envs.put("TOOLCHAIN_HOME", "/usr/local/Ascend/ascend-toolkit/latest/toolkit");
        envs.put("ASCEND_HOME_PATH", "/usr/local/Ascend/ascend-toolkit/latest");

        String inner = "/bin/bash -c \"llamafactory-cli train /app/examples/auto_generate_train_yaml/task/" +
                taskId + "/train_in_docker.yaml | tee /app/saves/output_dir/" + taskId + "-supervisor-log\"";

        return buildDockerRunCommand(
                image,
                name,
                useTty,
                devices,
                volumes,
                envs,
                network,
                inner,
                privileged,
                removeOnExit
        );
    }

    private static String quote(String val) {
        if (val == null) return "";
        if (val.contains(" ") || val.contains("\"") || val.contains("'")) {
            return '"' + val.replace("\"", "\\\"") + '"';
        }
        return val;
    }

    /**
     * 查询容器是否存在/运行状态。
     */
    public String inspectContainer(String containerName) throws IOException {
        try (SSHClientHelper client = newClient()) {
            String cmd = "bash -lc 'docker ps -a --filter name=" + escape(containerName) + " --format \"{{.Names}} {{.Status}}\"'";
            SSHClientHelper.ExecResult res = client.exec(cmd, Duration.ofSeconds(30), false);
            return res.stdout().trim();
        }
    }

    /**
     * 拉取容器日志。
     */
    public String containerLogs(String containerName, int tail) throws IOException {
        try (SSHClientHelper client = newClient()) {
            String cmd = "bash -lc 'docker logs --tail=" + tail + " " + escape(containerName) + "'";
            SSHClientHelper.ExecResult res = client.exec(cmd, Duration.ofMinutes(5), false);
            return res.stdout();
        }
    }

    /**
     * 停止并删除容器（存在则执行）。
     */
    public void stopAndRemove(String containerName) throws IOException {
        try (SSHClientHelper client = newClient()) {
            String stop = "bash -lc 'docker ps -q --filter name=" + escape(containerName) + " | xargs -r docker stop'";
            client.exec(stop, Duration.ofMinutes(2), false);
            String rm = "bash -lc 'docker ps -aq --filter name=" + escape(containerName) + " | xargs -r docker rm -f'";
            client.exec(rm, Duration.ofMinutes(2), false);
        }
    }

    private String escape(String s) {
        return s.replace("'", "'\\''");
    }


    public static void main(String[] args) throws Exception {
        RemoteDockerHost docker = new RemoteDockerHost(
                "192.168.102.38", 22, "root",
                null, "root" // 或 null, 使用密码
        );


        SSHClientHelper.ExecResult execResult = docker.dockerComposeUp("/root/docker-compose/test1.yaml", true);
        execResult = docker.dockerComposePs("/root/docker-compose/test.yaml", Duration.ofSeconds(30));
        System.out.println(execResult.stdout());
        String s = extractStatus(execResult.stdout());



        execResult = docker.dockerComposePs("/root/docker-compose/test1.yaml");
        execResult = docker.dockerComposeDown("/root/docker-compose/test1.yaml");

        docker.dockerComposeUp("/root/docker-compose/test1.yaml", true, Duration.ofMinutes(20));
        docker.dockerComposePs("/root/docker-compose/test1.yaml", Duration.ofSeconds(30));
        docker.dockerComposeDown("/root/docker-compose/test1.yaml", Duration.ofMinutes(8));


        //String script = """
        //        #!/usr/bin/env bash
        //        set -euo pipefail
        //        docker run -it --rm --privileged --name llamafactory_train --network host \
        //        --device=/dev/davinci0 --device=/dev/davinci1 --device=/dev/davinci2 --device=/dev/davinci3 \
        //        --device=/dev/davinci4 --device=/dev/davinci5 --device=/dev/davinci6 --device=/dev/davinci7 \
        //        -v /mnt/hpfs/cyc_qwen2_workspace/saves:/app/saves \
        //        -v /mnt/hpfs/cyc_qwen2_workspace/llm_models:/llm_models \
        //        -v /mnt/hpfs/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/examples:/app/examples \
        //        -v /mnt/hpfs/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory/data:/app/data \
        //        -v /usr/local/Ascend/driver:/usr/local/Ascend/driver \
        //        -v /usr/local/Ascend/add-ons/:/usr/local/Ascend/add-ons/ \
        //        -v /usr/local/sbin/npu-smi:/usr/local/sbin/npu-smi \
        //        -v /mnt/hpfs/cyc_qwen2_workspace/qwen2_folder_modified/codes/LLaMA-Factory:/root/dg/LLaMA-Factory \
        //        -v /var/log/npu:/usr/slog \
        //        -v /mnt/hpfs/models:/mnt/hpfs/models \
        //        -e FORCE_TORCHRUN=1 \
        //        -e LD_LIBRARY_PATH="/usr/local/Ascend/ascend-toolkit/latest/tools/aml/lib64:/usr/local/Ascend/ascend-toolkit/latest/tools/aml/lib64/plugin:/usr/local/Ascend/ascend-toolkit/latest/lib64:/usr/local/Ascend/ascend-toolkit/latest/lib64/plugin/opskernel:/usr/local/Ascend/ascend-toolkit/latest/lib64/plugin/nnengine:/usr/local/Ascend/ascend-toolkit/latest/opp/built-in/op_impl/ai_core/tbe/op_tiling/lib/linux/aarch64:/usr/local/Ascend/driver/lib64/common:/usr/local/Ascend/driver/lib64/driver" \
        //        -e ASCEND_TOOLKIT_HOME="/usr/local/Ascend/ascend-toolkit/latest" \
        //        -e PYTHONPATH="/usr/local/Ascend/ascend-toolkit/latest/python/site-packages:/usr/local/Ascend/ascend-toolkit/latest/opp/built-in/op_impl/ai_core/tbe" \
        //        -e PATH="/usr/local/Ascend/ascend-toolkit/latest/bin:/usr/local/Ascend/ascend-toolkit/latest/compiler/ccec_compiler/bin:/usr/local/Ascend/ascend-toolkit/latest/tools/ccec_compiler/bin:/usr/local/python3.8/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin" \
        //        -e ASCEND_AICPU_PATH="/usr/local/Ascend/ascend-toolkit/latest" \
        //        -e ASCEND_OPP_PATH="/usr/local/Ascend/ascend-toolkit/latest/opp" \
        //        -e TOOLCHAIN_HOME="/usr/local/Ascend/ascend-toolkit/latest/toolkit" \
        //        -e ASCEND_HOME_PATH="/usr/local/Ascend/ascend-toolkit/latest" \
        //        10.238.190.42:18080/library/qwen2_npu:v5 /bin/bash -c "llamafactory-cli train /app/examples/auto_generate_train_yaml/task/1922493374352326656/train_in_docker.yaml | tee /app/saves/output_dir/1922493374352326656-supervisor-log"
        //        """;
        //
        //script = """
        //        #!/usr/bin/env bash
        //        set -euo pipefail
        //        docker ps
        //        """;
        //
        //SSHClientHelper.ExecResult res = docker.runWithPty(script, "/tmp/run_train.sh", Duration.ofHours(24));
        //System.out.println(res.stdout());
        //System.err.println(res.stderr());
        //docker.runAsync(script, "/tmp/run_train.sh", "/tmp/llama_train.log");
        //
        //
        //RemoteDockerHost docker2 = new RemoteDockerHost("10.0.0.1", 22, "root", "C:/Users/you/.ssh/id_rsa", null);
        //
        //// 前台（适合含 -it 的命令）
        //docker2.uploadAndRunWithPty("C:/tmp/run_train.sh", "/tmp/run_train.sh", Duration.ofHours(24));
        //
        //// 后台（不建议 -t；日志在远端）
        //docker2.uploadAndRunAsync("C:/tmp/run_train.sh", "/tmp/run_train.sh", "/var/log/llama_train.log");
        //
        //
        //String script2 = """
        //        #!/usr/bin/env bash
        //        set -euo pipefail
        //        echo hello
        //        """;
        //
        //// 前台执行
        //docker2.uploadAndRunShWithPty(script2, "/tmp/run_train.sh", Duration.ofMinutes(10));
        //
        //// 后台执行
        //docker2.uploadAndRunShAsync(script2, "/tmp/run_train.sh", "/var/log/train.log");
    }

    public static String extractStatus(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        // 按两个及以上空白字符分割（兼容空格、制表符等）
        String[] columns = line.trim().split("\\s{2,}");

        // 至少需要7列（含表头对应的7个字段）
        if (columns.length < 7) {
            // 如果 COMMAND 很短，可能列数少？但 STATUS 通常是倒数第二
            if (columns.length >= 2) {
                return columns[columns.length - 2];
            }
            throw new IllegalArgumentException("无法解析 docker ps 行: " + line);
        }

        // 正常情况：STATUS 是第6列（索引5）或倒数第二列
        return columns[columns.length - 2];
    }
}


