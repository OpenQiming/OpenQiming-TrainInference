package com.ctdi.llmtc.xtp.traininfer.util;

import com.ctdi.llmtc.xtp.traininfer.constant.ModelConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author yangla
 * @since 2025/6/12
 */
@Slf4j
public class FileUtil {

    public static void delTrainFile(ModelPathUtil pathUtil, String taskId) {
        try {
            // 删除训练YAML文件等
            String trainYmlPath = pathUtil.getTrainDirPath(taskId);
            if (exist(trainYmlPath)) {
                rmTree(trainYmlPath);
            }
            String dpoYmlPath = pathUtil.getDpoDirPath(taskId);
            if (exist(dpoYmlPath)) {
                rmTree(dpoYmlPath);
            }
            // 删除训练YAML文件等
            String modelFilePath = pathUtil.getModelFilePath(taskId);
            if (exist(modelFilePath)) {
                rmTree(modelFilePath);
            }
            // 删除日志文件
            String supervisorLog = pathUtil.getTrainLogFilePath(taskId, "supervisor");
            if (exist(supervisorLog)) {
                remove(supervisorLog);
            }
            String workerLog = pathUtil.getTrainLogFilePath(taskId, "worker");
            if (exist(workerLog)) {
                remove(workerLog);
            }
            log.info("delete train file success. taskId: {}", taskId);
        } catch (Exception e) {
            log.error("delete train file error.", e);
        }
    }

    public static void delEvalFile(ModelPathUtil pathUtil, String taskId) {
        try {
            // 删除训练YAML文件等
            String evalYmlPath = pathUtil.getEvalDirPath(taskId);
            if (exist(evalYmlPath)) {
                rmTree(evalYmlPath);
            }
            log.info("delete eval file success. taskId: {}", taskId);
        } catch (Exception e) {
            log.error("delete eval file error.", e);
        }
    }

    public static void delInferFile(String op, ModelPathUtil pathUtil, String taskId) {
        try {
            // 删除训练YAML文件等
            String inferYmlPath = pathUtil.getInferenceDirPath(taskId);
            if (ModelConstants.OP_INFER_EVAL.equals(op)) {
                inferYmlPath = pathUtil.getInferenceEvalDirPath(taskId);
            }

            if (exist(inferYmlPath)) {
                rmTree(inferYmlPath);
            }
            // 删除日志文件
            String inferLogFilePath = pathUtil.getInferenceLogFilePath(taskId);
            remove(inferLogFilePath);

            // 下面的代码忽略,inference_offset.json这个文件在inference_watch_run中被使用了，
            // 但是inference_watch_run任务被注释不用了 TODO

            //with open('inference_offset.json', 'r', encoding='utf-8') as file:
            //data = json.load(file)
            //if task_id in data.keys():
            //del data[task_id]
            //with open('inference_offset.json', 'w', encoding='utf-8') as file:
            //json.dump(data, file, ensure_ascii=False, indent=4)
            log.info("delete inference file success. taskId: {}", taskId);
        } catch (Exception e) {
            log.error("delete inference file error.", e);
        }
    }

    public static void rmTree(String filePath) {
        try {
            FileUtils.deleteDirectory(new File(filePath));
        } catch (Exception e) {
            log.error("rmTree file error.", e);
        }
    }

    public static void copyTree(String srcPath, String dstPath) {
        try {
            cn.hutool.core.io.FileUtil.copyContent(
                    new File(srcPath),
                    new File(dstPath),
                    true
            );
        } catch (Exception e) {
            log.error("copyTree file error.", e);
        }
    }

    public static boolean exist(String filePath) {
        return cn.hutool.core.io.FileUtil.exist(filePath);
    }

    public static List<String> readLines(String filePath) {
        return cn.hutool.core.io.FileUtil.readLines(filePath, StandardCharsets.UTF_8);
    }

    public static File mkdir(String filePath) {
        return cn.hutool.core.io.FileUtil.mkdir(filePath);
    }

    public static boolean remove(String filePath) {
        return cn.hutool.core.io.FileUtil.del(filePath);
    }

}
