package com.study.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JSON 工具类
 * 提供 AI 响应解析相关的通用方法
 */
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        OBJECT_MAPPER.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private JsonUtils() {
        // 工具类不允许实例化
    }

    /**
     * 清理 AI 返回的 JSON 字符串
     * 移除可能的 markdown 代码块标记（```json ... ```）
     *
     * @param response AI 返回的原始响应
     * @return 清理后的 JSON 字符串，如果输入为 null 则返回 null
     */
    public static String cleanJsonResponse(String response) {
        if (response == null) {
            return null;
        }
        String json = response.trim();
        if (json.startsWith("```")) {
            json = json.replaceAll("```(json)?", "").trim();
        }
        return json;
    }

    /**
     * 对象转 JSON 字符串
     * 序列化失败时返回默认值
     *
     * @param obj        要序列化的对象
     * @param defaultVal 序列化失败时的默认值
     * @return JSON 字符串
     */
    public static String toJson(Object obj, String defaultVal) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            org.slf4j.LoggerFactory.getLogger(JsonUtils.class).error("JSON 序列化失败", e);
            return defaultVal;
        }
    }

    /**
     * 对象转 JSON 字符串（序列化失败返回 "{}"）
     *
     * @param obj 要序列化的对象
     * @return JSON 字符串
     */
    public static String toJson(Object obj) {
        return toJson(obj, "{}");
    }

    /**
     * JSON 字符串转对象
     *
     * @param json JSON 字符串
     * @param typeReference 类型引用
     * @return 转换后的对象，转换失败抛出异常
     */
    public static <T> T fromJson(String json, com.fasterxml.jackson.core.type.TypeReference<T> typeReference) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, typeReference);
    }
}
