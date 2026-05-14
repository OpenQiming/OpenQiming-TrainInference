package com.ctdi.llmtc.xtp.traininfer.plugin.deploy;

import cn.hutool.core.util.StrUtil;
import com.ctdi.llmtc.xtp.traininfer.beans.param.DPOTask;
import com.ctdi.llmtc.xtp.traininfer.beans.param.EvalTask;
import com.ctdi.llmtc.xtp.traininfer.beans.param.InferenceTask;
import com.ctdi.llmtc.xtp.traininfer.beans.param.TrainTask;
import com.ctdi.llmtc.xtp.traininfer.beans.req.TrainReq;
import com.ctdi.llmtc.xtp.traininfer.constant.ModelConstants;
import com.ctdi.llmtc.xtp.traininfer.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 部署插件抽象基类
 * 提供通用的实现逻辑
 *
 * @author yangla
 * @since 2025/1/27
 */
@Slf4j
public abstract class AbstractDeployPlugin implements DeployPlugin {

    @Autowired
    protected ModelPathUtil modelPathUtil;

    @Override
    public boolean genConfig(TrainReq trainReq) {
        try {
            String op = trainReq.getOp();
            String taskId = trainReq.getTaskId();
            String taskDirPath = modelPathUtil.getYamlFilePath(taskId, op);
            if (!FileUtil.exist(taskDirPath)) {
                FileUtil.mkdir(taskDirPath);
            }

            switch (trainReq.getOp()) {
                case ModelConstants.OP_DPO -> {
                    DPOTask dpoTask = new DPOTask();
                    BeanUtils.copyProperties(trainReq, dpoTask);
                    genDpoConfig(dpoTask, op);
                    genDpoParamFile(dpoTask, op);
                }
                case ModelConstants.OP_TRAIN -> {
                    TrainTask trainTask = new TrainTask();
                    BeanUtils.copyProperties(trainReq, trainTask);
                    genTrainConfig(trainTask, op);
                    genTrainParamFile(trainTask, op);
                }
                case ModelConstants.OP_EVAL -> {
                    EvalTask evalTask = new EvalTask();
                    BeanUtils.copyProperties(trainReq, evalTask);
                    genEvalConfig(evalTask, op);
                    genEvalParamFile(evalTask, op);
                }
                case ModelConstants.OP_INFER, ModelConstants.OP_INFER_EVAL -> {
                    InferenceTask inferTask = new InferenceTask();
                    BeanUtils.copyProperties(trainReq, inferTask);
                    genInferConfig(inferTask, op, trainReq.getProjectSpaceId());
                    genInferParamFile(inferTask, op);
                }
            }
        } catch (Exception e) {
            log.info("genConfig error.", e);
            return false;
        }
        return true;
    }

    public Map<String, Object> loadTrainIterInfo(String taskId) {
        try {
            String logPath = modelPathUtil.getTrainLogFilePath(taskId, "supervisor");
            if (!FileUtil.exist(logPath)) {
                return new HashMap<>(Map.of("id", taskId, "trainingLoss", ""));
            }

            // 读取配置文件获取 total_epoch
            int totalEpoch = 0;
            String configPath1 = modelPathUtil.getTrainConfigFilePath(taskId);
            String configPath2 = modelPathUtil.getDpoConfigFilePath(taskId);
            if (FileUtil.exist(configPath1)) {
                Map<String, Object> data = YmlUtil.load(configPath1, Map.class);
                totalEpoch = ((Number) data.get("num_train_epochs")).intValue();
            } else if (FileUtil.exist(configPath2)) {
                Map<String, Object> data = YmlUtil.load(configPath1, Map.class);
                totalEpoch = ((Number) data.get("num_train_epochs")).intValue();
            }

            // 读取日志文件
            List<String> lines = FileUtil.readLines(logPath);

            Pattern lossPattern = Pattern.compile("'loss':([^,]*),");
            Pattern epochPattern = Pattern.compile("'epoch':([^,}]*)}");
            Pattern timePattern = Pattern.compile("\\b(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2}) - INFO - llamafactory\\.model\\.loader");

            List<String> lossList = new ArrayList<>();
            int runTime = 0;
            double epoch = 0.0;
            LocalDateTime givenTime = null;

            for (String line : lines) {
                Matcher lossMatcher = lossPattern.matcher(line);
                if (lossMatcher.find()) {
                    lossList.add(lossMatcher.group(1).trim());
                }

                Matcher epochMatcher = epochPattern.matcher(line);
                if (epochMatcher.find()) {
                    epoch = Double.parseDouble(epochMatcher.group(1).trim());
                }

                Matcher timeMatcher = timePattern.matcher(line);
                if (timeMatcher.find()) {
                    String timeStr = timeMatcher.group(1);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
                    givenTime = LocalDateTime.parse(timeStr, formatter);
                }
            }

            // 计算运行时间（秒）
            if (givenTime != null) {
                LocalDateTime now = LocalDateTime.now();
                long diffSeconds = java.time.Duration.between(givenTime, now).getSeconds();
                runTime = (int) Math.max(diffSeconds, 0);

                // 特殊处理 aarch64 架构（需检测系统架构）
                String arch = System.getProperty("os.arch");
                if ("aarch64".equals(arch) && runTime > 8 * 3600) {
                    runTime -= 8 * 3600;
                }
            }

            int remainTime = 0;
            int defaultRemain = 180;

            if (epoch != 0 && totalEpoch != 0 && runTime != 0) {
                remainTime = (int) (totalEpoch * (runTime / epoch) - runTime);
                log.info("[{}] total_epoch={}, run_time={}, epoch={}, remainTime={}",
                        taskId, totalEpoch, runTime, epoch, remainTime);
            } else {
                log.info("[{}] insufficient data: total_epoch={}, run_time={}, epoch={}, use_default={}",
                        taskId, totalEpoch, runTime, epoch, defaultRemain);
                remainTime = defaultRemain;
            }
            return new HashMap<>(Map.of(
                    "id", taskId,
                    "trainingLoss", String.join(",", lossList),
                    "runtime", runTime,
                    "remainTime", remainTime,
                    "iterateCurr", lossList.size()
            ));
        } catch (Exception e) {
            log.error("[{}]loadTrainIterInfo error.", taskId, e);
            return new HashMap<>(Map.of("id", taskId, "trainingLoss", ""));
        }
    }

    public String trainLossTrend(String taskId, String url) {
        String imagePath = modelPathUtil.getModelFilePath(taskId) + "/training_loss.png";
        if (!FileUtil.exist(imagePath)) {
            return "";
        }

        try {
            // 2. 读取图片并转为 Base64
            byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 3. 构造请求数据
            String prompt = "模型训练loss曲线可以从以下几个角度进行分析：" +
                    "<趋势分析>下降速度：观察损失函数下降的速度，特别是在训练初期，可以了解学习率是否设置得当。" +
                    "平稳阶段：损失函数达到一个平稳期的时间，以及这个平稳期的损失值大小，可以帮助判断模型是否已经收敛。" +
                    "最终损失值：模型训练结束时的损失值反映了模型对训练数据拟合的程度。" +
                    "<波动性> 振荡幅度：如果损失值在训练过程中出现较大的波动，可能意味着学习率设置过高或者数据中存在异常点。" +
                    "周期性：某些情况下，损失可能会呈现出周期性的变化，这可能是由于数据集中的特定模式或者是训练过程中的优化器问题。" +
                    "<异常检测> 突变点：损失曲线中突然的变化可能指示了数据中的异常情况，或者是训练过程中出现了问题，例如权重初始化不当、数据不平衡等。" +
                    "梯度消失/爆炸：在深度网络中，如果损失曲线表现出异常行为，如急剧增加或减少，可能是梯度消失或梯度爆炸的问题。" +
                    "<超参数调优> 学习率：通过调整学习率观察损失曲线的变化，可以帮助找到最优的学习率。" +
                    "批量大小：批量大小的不同设置也会对损失曲线产生影响，较小的批量大小可能导致更不稳定的损失曲线，而较大的批量大小可能会导致更平滑但下降速度较慢的曲线。" +
                    "请基于以上内容，简要分析这张loss曲线图，并提出简短建议，字数控制在100字左右";

            Map<String, Object> requestData = new HashMap<>();
            requestData.put("prompt", prompt);
            requestData.put("model", "aiden_lu/minicpm-v2.6:Q4_K_M");
            requestData.put("stream", false);
            requestData.put("images", new String[]{base64Image});

            String jsonBody = JSONUtil.toJsonStr(requestData);
            String report = HttpUtil.report(url, jsonBody);

            if (StrUtil.isEmpty(report)) {
                log.info("[{}] train_loss_trend result has error ", taskId);
                return "";
            }
            Map<String, String> resultMap = JSONUtil.readValue(report);
            String evalResult = resultMap.get("response");
            log.info("[{}] train_loss_trend result: {} ", taskId, evalResult);
        } catch (Exception e) {
            log.error("[{}]  train_loss_trend error.", taskId, e);
        }
        return "";
    }

    public void lossCallback (Map<String, Object> iterMap, String lossUrl, String taskId) {
        String jsonBody = JSONUtil.toJsonStr(iterMap);
        String result = HttpUtil.report(lossUrl, jsonBody);
        log.info("[{}] callback_loss result: {}", taskId, result);
    }

    public void evalCallback (Map<String, Object> iterMap, String evalUrl, String taskId) {
        String jsonBody = JSONUtil.toJsonStr(iterMap);
        String result = HttpUtil.report(evalUrl, jsonBody);
        log.info("[{}] callback_tokens result: {}", taskId, result);
    }

    public Map<String, Object> loadEvalLog(String taskId) {
        Map<String, Object> evalResult = new HashMap<>();

        // key 映射表（与 Python 一致）
        Map<String, String> keyMap = new HashMap<>();
        keyMap.put("Average", "average");
        keyMap.put("STEM", "stem");
        keyMap.put("Social Sciences", "socialScience");
        keyMap.put("Humanities", "humanity");
        keyMap.put("Other", "other");

        String logPath = modelPathUtil.getEvalLogFilePath(taskId);
        if (!FileUtil.exist(logPath)) {
            return evalResult;
        }
        try {
            List<String> lines = FileUtil.readLines(logPath);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                // 按第一个 ':' 分割（避免值中包含冒号）
                int colonIndex = line.indexOf(':');
                if (colonIndex == -1) continue;

                String key = line.substring(0, colonIndex).trim();
                String valueStr = line.substring(colonIndex + 1).trim();

                String mappedKey = keyMap.get(key);
                if (mappedKey == null) {
                    log.warn("Unrecognized key in eval log: {}", key);
                    continue;
                }
                double value = Double.parseDouble(valueStr);
                evalResult.put(mappedKey, value);
            }
        } catch (Exception e) {
            log.error("Failed to read eval log file for task: {}", taskId, e);
        }
        return evalResult;
    }

    public String getRunEnv() {
        String arch = System.getProperty("os.arch");
        log.info("Current systemt arch is: {}", arch);
        if ("aarch64".equals(arch)) {
            return ModelConstants.ARM_ARCH;
        } else {
            return ModelConstants.X86_ARCH;
        }
    }

}
