package com.ctdi.llmtc.xtp.traininfer.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.yaml.YamlUtil;
import com.ctdi.llmtc.xtp.traininfer.beans.param.InferenceTask;
import com.ctdi.llmtc.xtp.traininfer.constant.ModelConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author yangla
 * @since 2025/6/11
 */
@Slf4j
public class YmlUtil {

    public static void dump(Object object, String path) throws IOException {
        YAMLFactory yamlFactory = new YAMLFactory();
        yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        yamlFactory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        yamlFactory.enable(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS_AS_STRINGS); // 仅纯数字加引号
        ObjectMapper mapper = new ObjectMapper(yamlFactory);
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        mapper.writeValue(new File(path), object);
    }

    public static <T> T load(String path, Class<T> cls) {
        return YamlUtil.loadByPath(path, cls);
    }

    public static void genConfig(String taskId, String tplPath, String outPath) {
        // 读取文件内容
        String content = FileUtil.readString(tplPath, StandardCharsets.UTF_8);
        // 替换所有 <taskid> 占位符
        content = StrUtil.replace(content, "<taskid>", taskId);
        cn.hutool.core.io.FileUtil.writeString(content, outPath, StandardCharsets.UTF_8);
    }

    public static void genConfig(InferenceTask inferTask, String tplPath, String outPath, String projectSpaceId) {
        String taskId = inferTask.getTaskId();
        // 读取文件内容
        String content = FileUtil.readString(tplPath, StandardCharsets.UTF_8);
        // 替换所有 <taskid> 占位符
        content = StrUtil.replace(content, "<taskid>", taskId);

        if (!ModelConstants.PS_ID_PUBLIC_TRAIN.equals(projectSpaceId)
                && !ModelConstants.PS_ID_PUBLIC_INFER.equals(projectSpaceId)) {
            // 替换 inferencewx: 'true'
            String searchTxt = ModelConstants.INFER_LABEL_KEY + ": 'true'";
            String replaceTxt = ModelConstants.PS_LABEL_KEY + ": '" + projectSpaceId + "'";
            content = StrUtil.replace(content, searchTxt, replaceTxt);

            String priorityClassName = ModelConstants.PS_INFER_PRIORITY_CONF.get(inferTask.getModelName());
            log.info("TaskId priorityClassName is: {}. taskId: {}, modeName: {}", priorityClassName, taskId, inferTask.getModelName());
            if (StrUtil.isNotEmpty(priorityClassName)) {
                // 增加优先级priorityClassName
                content = content.replaceAll(
                        "(\\s+)nodeSelector:",
                        "$1priorityClassName: " + priorityClassName + "$1nodeSelector:");
            }

        }
        cn.hutool.core.io.FileUtil.writeString(content, outPath, StandardCharsets.UTF_8);
    }

    public static void genParamConfig(Map<String, Object> updatesMap, String tplPath, String outPath) throws IOException {
        Map<String, Object> trainParamMap = YmlUtil.load(tplPath, Map.class);
        for (Map.Entry<String, Object> entry : updatesMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (trainParamMap.containsKey(key)) {
                trainParamMap.put(key, value);
                log.info("Success: Key [{}] was modified to {}.", key, value);
            } else {
                log.info("Warning: Key [{}] not found in the YAML file.", key);
            }
        }
        YmlUtil.dump(trainParamMap, outPath);
    }

}
