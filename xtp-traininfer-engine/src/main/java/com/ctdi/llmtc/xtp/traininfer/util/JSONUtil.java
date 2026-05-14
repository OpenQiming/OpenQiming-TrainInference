package com.ctdi.llmtc.xtp.traininfer.util;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yangla
 * @since 2025/6/10
 */
public class JSONUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Map<String, Object> convertValue(Object object) {
        return mapper.convertValue(object, new TypeReference<>() {});
    }

    public static String toJSONString(Object object) {
        try {
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            return "";
        }
    }

    public static String toJsonStr(Object object) {
        return cn.hutool.json.JSONUtil.toJsonStr(object);
    }

    public static <T> T readJsonFile(String path, TypeReference<T> typeRef) throws IOException {
        File file = new File(path);
        return mapper.readValue(file, typeRef);
    }

    public static <T> List<T> readJsonLinesFile(String path, TypeReference<T> typeReference) throws IOException {
        File file = new File(path);
        List<T> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    T obj = mapper.readValue(line, typeReference);
                    result.add(obj);
                }
            }
        }
        return result;
    }

    public static void writeJsonToFile(Object obj, String path) throws IOException {
        SpaceIndenter instance = new SpaceIndenter();
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentObjectsWith(instance);
        prettyPrinter.indentArraysWith(instance);

        // 序列化成带缩进格式的字符串
        String jsonStr = mapper.writer(prettyPrinter).writeValueAsString(obj);
        jsonStr = jsonStr.replaceAll("\\s*:\\s*", ": "); // 去掉冒号前后的空格
        // 写入文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(jsonStr);
        }
    }

    public static String toPrettyJson(String json) throws IOException {
        SpaceIndenter instance = new SpaceIndenter();
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentObjectsWith(instance);
        prettyPrinter.indentArraysWith(instance);

        Map<String, Object> map = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});;
        // 序列化成带缩进格式的字符串
        String jsonStr = mapper.writer(prettyPrinter).writeValueAsString(map);
        jsonStr = jsonStr.replaceAll("\\s*:\\s*", ": "); // 去掉冒号前后的空格
        return jsonStr;
    }

    public static void writeJsonToFile2(Object obj, String path) throws IOException {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String jsonStr = mapper.writeValueAsString(obj);
        jsonStr = jsonStr.replaceAll("\\s*:\\s*", ": "); // 去掉冒号前后的空格
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            writer.write(jsonStr);
        }
    }

    public static void writeJsonToFile3(Object obj, String path) throws IOException {
        try (Writer writer = new FileWriter(path)) {
            writer.write(JSON.toJSONString(obj, true));
        }
    }

    static class SpaceIndenter implements DefaultPrettyPrinter.Indenter {
        @Override
        public void writeIndentation(JsonGenerator g, int level) throws IOException {
            g.writeRaw('\n');
            for (int i = 0; i < level * 4; ++i) {
                g.writeRaw(' ');
            }
        }

        @Override
        public boolean isInline() {
            return false;
        }
    }

    public static Map<String, String> readValue(String object) throws JsonProcessingException {
        return mapper.readValue(object, Map.class);
    }

    public static void main(String[] args) throws IOException {
        //List<Map<String, String>> list = new ArrayList<>();
        //Map<String, String> map1 = new HashMap<>();
        //map1.put("aaaa", "11111");
        //map1.put("bbb", "222222");
        //list.add(map1);
        //
        //Map<String, String> map2 = new HashMap<>();
        //map2.put("cccc", "3333");
        //map2.put("ddddd", "4444");
        //list.add(map2);
        //
        //String dd = "C:\Users\yangla\Desktop\1\\3.json";
        //writeJsonToFile(list, dd);

        String path = "C:\\Users\\yangla\\Desktop\\1\\3.jsonl";
        //String path = "C:\Users\yangla\Desktop\1\\2.json";

        // 示例1：读取为 List<Map<String, Object>>
        List<Map<String, Object>> listData = readJsonFile(path, new TypeReference<>() {});
        System.out.println("List<Map> size: " + listData.size());




    }
}
