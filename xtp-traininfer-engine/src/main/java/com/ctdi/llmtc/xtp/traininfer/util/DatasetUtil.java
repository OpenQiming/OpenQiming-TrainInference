package com.ctdi.llmtc.xtp.traininfer.util;

import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yangla
 * @since 2025/6/7
 */
@Slf4j
public class DatasetUtil {

    public static boolean convertDataset(ModelPathUtil pathUtil, String fn, String suffix) {
        try {
            String sourcePath = pathUtil.getSrcDatasetFilePath(fn, suffix);
            if (!FileUtil.exist(sourcePath)) {
                log.error("Source dataset file is not exist. {}", sourcePath);
                return false;
            }

            String dataInfoFilePath = pathUtil.getDatainfoFilePath();
            Map<String, Object> dataInfo = JSONUtil.readJsonFile(dataInfoFilePath, new TypeReference<>() {});

            List<Map<String, Object>> dataSet = new ArrayList<>();
            if ("xlsx".equalsIgnoreCase(suffix)) {
                excelToJson(sourcePath, fn, dataSet, dataInfo);
            } else if ("jsonl".equalsIgnoreCase(suffix)) {
                jsonlToJson(sourcePath, fn, dataSet, dataInfo);
            } else if ("json".equalsIgnoreCase(suffix)) {
                jsonToJson(sourcePath, fn, dataSet, dataInfo);
            }

            String dstPath = pathUtil.getDstDatasetFilePath(fn);
            JSONUtil.writeJsonToFile(dataSet, dstPath);
            JSONUtil.writeJsonToFile(dataInfo, dataInfoFilePath);
        } catch (Exception e) {
            log.error("convertDataset error", e);
            return false;
        }
        return true;
    }

    private static void excelToJson(String sourcePath, String fn,
                                       List<Map<String, Object>> dataSet,
                                       Map<String, Object> dataInfo) {
        List<Map<String, String>> rows = ExcelUtil.readExcelToMap(sourcePath);
        if (containsColumns(rows, Arrays.asList("回答1", "回答2"))) {
            for (Map<String, String> row : rows) {
                String answer1 = row.get("回答1");
                String answer2 = row.get("回答2");

                String chosenAnswer = "回答1".equals(row.get("选择回答")) ? answer1 : answer2;
                String rejectedAnswer = "回答1".equals(row.get("选择回答")) ? answer2 : answer1;

                Map<String, Object> conversation = new LinkedHashMap<>();
                conversation.put("conversations", Collections.singletonList(
                        Map.of("from", "human", "value", row.get("用户问题"))
                ));
                conversation.put("chosen", Map.of("from", "gpt", "value", chosenAnswer));
                conversation.put("rejected", Map.of("from", "gpt", "value", rejectedAnswer));
                dataSet.add(conversation);
            }

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("file_name", fn + ".json");
            info.put("ranking", true);
            info.put("formatting", "sharegpt");

            Map<String, String> columns = new LinkedHashMap<>();
            columns.put("messages", "conversations");
            columns.put("chosen", "chosen");
            columns.put("rejected", "rejected");
            info.put("columns", columns);
            dataInfo.put(fn, info);
        } else {
            // 普通数据集：按“问题ID”分组
            Map<String, List<Map<String, String>>> grouped = rows.stream()
                    .collect(Collectors.groupingBy(r -> r.get("问题ID")));

            for (String key : grouped.keySet()) {
                List<Map<String, String>> groupData = grouped.get(key);
                List<String> questions = groupData.stream().map(r -> r.get("问题")).toList();
                List<String> answers = groupData.stream().map(r -> r.get("答案")).toList();

                List<List<String>> history = new ArrayList<>();
                for (int i = 1; i < questions.size(); i++) {
                    history.add(Arrays.asList(questions.get(i), answers.get(i)));
                }

                dataSet.add(new LinkedHashMap<>() {{
                    put("instruction", questions.get(0));
                    put("input", "");
                    put("output", answers.get(0));
                    put("history", history);
                }});
            }

            Map<String, Object> info = new LinkedHashMap<>();
            info.put("file_name", fn + ".json");

            Map<String, String> columns = new LinkedHashMap<>();
            columns.put("prompt", "instruction");
            columns.put("query", "input");
            columns.put("response", "output");
            columns.put("history", "history");
            info.put("columns", columns);
            dataInfo.put(fn, info);
        }
    }

    private static void jsonlToJson(String sourcePath, String fn,
                List<Map<String, Object>> dataSet,
                Map<String, Object> dataInfo) throws IOException {
        List<Map<String, Object>> dataLines = JSONUtil.readJsonFile(sourcePath, new TypeReference<>() {});
        for (Map<String, Object> obj : dataLines) {
            List<Map<String, String>> conversations = (List<Map<String, String>>) obj.get("conversations");
            Map<String, Object> chat = new LinkedHashMap<>();
            chat.put("input", "");
            for (Map<String, String> item : conversations) {
                String role = item.get("role");
                String content = item.get("content");
                if ("user".equals(role)) {
                    chat.put("instruction", content);
                } else if ("assistant".equals(role)) {
                    chat.put("output", content);
                }
            }
            dataSet.add(chat);
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("file_name", fn + ".json");
        dataInfo.put(fn, info);
    }

    private static void jsonToJson(String sourcePath, String fn,
                                    List<Map<String, Object>> dataSet,
                                    Map<String, Object> dataInfo) throws IOException {
        List<Map<String, Object>> dataLines = JSONUtil.readJsonFile(sourcePath, new TypeReference<>() {});
        for (Map<String, Object> obj : dataLines) {
            List<Map<String, String>> conversations = (List<Map<String, String>>) obj.get("conversations");
            Map<String, Object> chat = new LinkedHashMap<>();
            chat.put("input", "");
            for (Map<String, String> item : conversations) {
                String role = item.get("role");
                String content = item.get("content");
                if ("user".equals(role)) {
                    chat.put("input", content);
                } else if ("assistant".equals(role)) {
                    chat.put("output", content);
                } else if ("instruction".equals(role)) {
                    chat.put("instruction", content);
                }
            }
            dataSet.add(chat);
        }

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("file_name", fn + ".json");

        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("prompt", "instruction");
        columns.put("query", "input");
        columns.put("response", "output");
        info.put("columns", columns);
        dataInfo.put(fn, info);
    }

    private static boolean containsColumns(List<Map<String, String>> rows, List<String> cols) {
        if (rows.isEmpty()) return false;
        Set<String> keys = rows.get(0).keySet();
        return keys.containsAll(cols);
    }

}
