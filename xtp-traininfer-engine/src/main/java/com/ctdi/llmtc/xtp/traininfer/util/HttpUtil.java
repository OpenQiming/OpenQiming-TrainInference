package com.ctdi.llmtc.xtp.traininfer.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yangla
 * @since 2025/8/6
 */
@Slf4j
public class HttpUtil {

    public static String report(String url, String data) {
        log.info("Data report start. url: {}", url);
        try {
            HttpResponse response = HttpRequest.post(url)
                    .body(data).timeout(5000)
                    .contentType("application/json;charset=utf-8")
                    .execute();
            log.info("Data report status: {}, body: {}, url: {}", response.getStatus(), response.body(), url);
            return response.body();
        } catch (Exception e) {
            log.error("Data report error.", e);
        }
        return null;
    }
}
