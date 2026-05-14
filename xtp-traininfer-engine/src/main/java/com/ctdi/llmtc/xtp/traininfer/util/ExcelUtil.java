package com.ctdi.llmtc.xtp.traininfer.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yangla
 * @since 2025/6/9
 */
@Slf4j
public class ExcelUtil {

    /**
     * 读取excel
     *
     * @param fileName 读取excel的文件名称
     *
     * @return datalist
     */
    public static List<Map<String, String>> readExcelToMap(String fileName) {
        List<Map<String, String>> dataList = new ArrayList<>();

        ExcelTypeEnum excelType = ExcelUtil.getExcelType(fileName);
        EasyExcel.read(fileName, new AnalysisEventListener<Map<String, String>>() {
            private Map<Integer, String> headMap;

            @Override
            public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                this.headMap = headMap;
            }

            @Override
            public void invoke(Map<String, String> valueData, AnalysisContext context) {
                HashMap<String, String> paramsMap = new HashMap<>();
                for (int i = 0; i < valueData.size(); i++) {
                    String key = headMap.get(i);
                    String value = String.valueOf(valueData.get(i));
                    paramsMap.put(key, value);
                }
                dataList.add(paramsMap);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                log.info("Excel读取完成,文件名:" + fileName + ",行数：" + dataList.size());

            }
        }).excelType(excelType).doReadAll();
        return dataList;
    }

    public static ExcelTypeEnum getExcelType (String fileName) {
        ExcelTypeEnum excelType = ExcelTypeEnum.XLSX;
        File file = new File(fileName);
        try (InputStream is = new FileInputStream(file)) {
            // 尝试用HSSF（.xls）解析
            try {
                new HSSFWorkbook(is); // 成功则是.xls
                excelType = ExcelTypeEnum.XLS;
            } catch (Exception e) {
                // 否则按.xlsx处理
            }
        } catch (Exception e) {
            log.error("getExcelType error.", e);
        }
        return excelType;
    }
}
