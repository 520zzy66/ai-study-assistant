package com.study.ai.agent.config;

import com.study.ai.agent.AgentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent ChatClient 工厂
 *
 * <p>从 resources/agents/agent-{id}.yml 加载 Agent 配置，
 * 为每个 Agent 创建独立的 ChatClient 实例并缓存。
 *
 * <p>配置加载流程：
 * <ol>
 *   <li>getOrCreate("multimodal") → 读取 agents/agent-multimodal.yml</li>
 *   <li>Jackson YAML → AgentConfig POJO（自动 kebab-case → camelCase）</li>
 *   <li>解析 ${ENV_VAR} 占位符</li>
 *   <li>根据是否配置独立 base-url 决定复用全局 Builder 或创建独立 ChatModel</li>
 *   <li>加载 System Prompt 文件 → 注入 defaultSystem</li>
 *   <li>缓存到 ConcurrentHashMap，避免重复创建</li>
 * </ol>
 *
 * @author AI Study Assistant
 */
@Slf4j
@Component
public class AgentClientFactory {

    /** 全局 ChatClient.Builder（继承 application.yml 的 spring.ai.openai.* 配置） */
    private final ChatClient.Builder globalBuilder;

    /** ChatClient 实例缓存，key = agentId */
    private final Map<String, ChatClient> cache = new ConcurrentHashMap<>();

    /** SnakeYAML 解析器（Spring Boot 内置，无需额外依赖） */
    private static final Yaml YAML = new Yaml();

    /** Spring Environment resolves placeholders including nested defaults. */
    private final Environment environment;

    public AgentClientFactory(ChatClient.Builder globalBuilder, Environment environment) {
        this.globalBuilder = globalBuilder;
        this.environment = environment;
    }

    /**
     * 根据 agentId 获取或创建 ChatClient
     *
     * @param agentId Agent 标识（对应文件名 agent-{id}.yml 中 {id} 部分）
     * @return 配置好的 ChatClient 实例
     */
    public ChatClient getOrCreate(String agentId) {
        return cache.computeIfAbsent(agentId, id -> {
            AgentConfig config = loadConfig(id);
            return buildClient(config);
        });
    }

    /**
     * 根据 agentId 加载 AgentConfig（用于需要访问配置元数据的场景，如读取 domainKeywords）
     *
     * @param agentId Agent 标识
     * @return AgentConfig，未找到时返回 null
     */
    public AgentConfig loadConfigOnly(String agentId) {
        String path = "agents/agent-" + agentId + ".yml";
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            Map<String, Object> raw = YAML.load(in);
            Map<String, Object> map = kebabToCamelKeys(raw);
            return assembleConfig(map);
        } catch (Exception e) {
            log.warn("加载 Agent 配置失败: {}", path, e);
            return null;
        }
    }

    /**
     * 将 YAML Map 组装为 AgentConfig（供内部复用）
     */
    @SuppressWarnings("unchecked")
    private AgentConfig assembleConfig(Map<String, Object> map) {
        AgentConfig config = new AgentConfig();
        config.setId(str(map, "id"));
        config.setName(str(map, "name"));
        config.setDescription(str(map, "description"));
        config.setIcon(str(map, "icon"));
        config.setOrder(intv(map, "order"));
        config.setSystemPromptFile(str(map, "systemPromptFile"));

        // 解析 domainKeywords（兼容 String 和 Map 两种格式）
        config.setDomainKeywords(parseDomainKeywords(map.get("domainKeywords")));

        // 解析 type 枚举
        String typeStr = str(map, "type");
        if (typeStr != null) {
            try {
                config.setType(AgentType.valueOf(typeStr));
            } catch (IllegalArgumentException e) {
                log.warn("未知 AgentType: {}", typeStr);
            }
        }

        // 解析 model 子配置
        Object modelObj = map.get("model");
        if (modelObj instanceof Map) {
            Map<String, Object> modelMap = (Map<String, Object>) modelObj;
            AgentConfig.ModelConfig model = new AgentConfig.ModelConfig();
            model.setBaseUrl(str(modelMap, "baseUrl"));
            model.setApiKey(str(modelMap, "apiKey"));
            model.setModel(str(modelMap, "model"));
            model.setTemperature(doublev(modelMap, "temperature", 0.7));
            config.setModel(model);
        }

        // 解析 supportedDomains
        if (map.get("supportedDomains") instanceof List domainList) {
            config.setSupportedDomains((java.util.List<String>) domainList);
        }

        // 解析 routing 阈值子配置
        Object routingObj = map.get("routing");
        if (routingObj instanceof Map) {
            Map<String, Object> routingMap = (Map<String, Object>) routingObj;
            AgentConfig.RoutingThresholds rt = new AgentConfig.RoutingThresholds();
            rt.setExpertThreshold(doublev(routingMap, "expertThreshold", 0.80));
            rt.setSuggestionThreshold(doublev(routingMap, "suggestionThreshold", 0.60));
            config.setRouting(rt);
        }

        return config;
    }

    /**
     * 解析 domainKeywords：
     * - String 格式：直接存入 singleDomainKeywords
     * - Map 格式：KEY(domain) → comma-separated keywords
     *
     * @return Map<domain, List<keyword>>
     */
    @SuppressWarnings("unchecked")
    private Map<String, List<String>> parseDomainKeywords(Object raw) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (raw == null) {
            return result;
        }
        if (raw instanceof String) {
            // 旧格式：单字符串 → 空 key
            List<String> keywords = parseCommaList((String) raw);
            if (!keywords.isEmpty()) {
                result.put("", keywords);
            }
        } else if (raw instanceof Map) {
            // 新格式：domain → keyword-string
            Map<String, Object> src = (Map<String, Object>) raw;
            for (Map.Entry<String, Object> entry : src.entrySet()) {
                List<String> kw = parseCommaList(String.valueOf(entry.getValue()));
                if (!kw.isEmpty()) {
                    result.put(entry.getKey(), kw);
                }
            }
        }
        return result;
    }

    /** 解析逗号分隔的关键词列表（去空、去重） */
    private List<String> parseCommaList(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(text.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
    }

    /**
     * 从 classpath 加载 YAML 配置
     *
     * <p>使用 SnakeYAML 解析 → kebab-case 转 camelCase → 手动组装 AgentConfig POJO。
     *
     * @param agentId Agent 标识
     * @return AgentConfig POJO
     */
    @SuppressWarnings("unchecked")
    private AgentConfig loadConfig(String agentId) {
        String path = "agents/agent-" + agentId + ".yml";
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            Map<String, Object> raw = YAML.load(in);
            Map<String, Object> map = kebabToCamelKeys(raw);
            AgentConfig config = assembleConfig(map);
            log.info("Agent 配置加载成功: agentId={}, model={}", agentId,
                    config.getModel() != null ? config.getModel().getModel() : "global");
            return config;
        } catch (Exception e) {
            throw new RuntimeException("加载 Agent 配置失败: " + path, e);
        }
    }

    // ===================== YAML 解析辅助方法 =====================

    /** 递归将 Map 的 key 从 kebab-case 转为 camelCase */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> kebabToCamelKeys(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String camelKey = kebabToCamel(entry.getKey());
            Object value = entry.getValue();
            if (value instanceof Map) {
                value = kebabToCamelKeys((Map<String, Object>) value);
            }
            result.put(camelKey, value);
        }
        return result;
    }

    /** kebab-case → camelCase */
    private static String kebabToCamel(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean upper = false;
        for (char c : s.toCharArray()) {
            if (c == '-') {
                upper = true;
            } else if (upper) {
                sb.append(Character.toUpperCase(c));
                upper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private static int intv(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        return 0;
    }

    private static double doublev(Map<String, Object> map, String key, double def) {
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        return def;
    }

    /**
     * 根据 AgentConfig 构建 ChatClient
     *
     * <p>如果配置了独立 base-url + api-key（占位符已解析），则创建独立 ChatModel；
     * 否则复用全局 Builder，仅覆盖 temperature 和 system prompt。
     */
    private ChatClient buildClient(AgentConfig config) {
        AgentConfig.ModelConfig model = config.getModel();
        if (model == null) {
            return globalBuilder.build();
        }

        String baseUrl = resolve(model.getBaseUrl());
        String apiKey = resolve(model.getApiKey());
        String modelName = resolve(model.getModel());
        double temperature = model.getTemperature();
        String systemPrompt = loadSystemPrompt(config.getSystemPromptFile());

        // 配置了独立 base-url + api-key → 独立 ChatModel
        if (baseUrl != null && !baseUrl.isBlank() && apiKey != null && !apiKey.isBlank()) {
            log.info("Agent '{}' 使用独立 ChatModel: model={}, temperature={}",
                    config.getId(), modelName, temperature);
            var apiBuilder = OpenAiApi.builder()
                    .baseUrl(baseUrl)
                    .apiKey(new SimpleApiKey(apiKey));
            if (baseUrl.endsWith("/v1")) {
                apiBuilder.completionsPath("/chat/completions");
            }
            var api = apiBuilder.build();
            var chatModel = OpenAiChatModel.builder()
                    .openAiApi(api)
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model(modelName)
                            .temperature(temperature)
                            .build())
                    .build();
            return ChatClient.builder(chatModel)
                    .defaultSystem(systemPrompt)
                    .build();
        }

        // 复用全局 Builder（占位符未配置时默认走此分支）
        log.info("Agent '{}' 复用全局 Builder: temperature={}", config.getId(), temperature);
        return globalBuilder
                .defaultSystem(systemPrompt)
                .defaultOptions(OpenAiChatOptions.builder()
                        .temperature(temperature)
                        .build())
                .build();
    }

    /**
     * 解析 ${VAR_NAME} 或 ${VAR_NAME:defaultValue} 占位符
     *
     * <p>查找顺序：环境变量 → 系统属性 → 默认值
     */
    private String resolve(String value) {
        if (value == null) return null;
        String resolved = environment.resolvePlaceholders(value);
        return resolved.contains("${") ? "" : resolved;
    }

    /**
     * 从 classpath 读取 System Prompt 文件内容
     */
    private String loadSystemPrompt(String path) {
        if (path == null || path.isBlank()) {
            return "你是一个 AI 学习助手的专家。";
        }
        try {
            return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("System prompt 文件未找到，使用默认: {}", path);
            return "你是一个 AI 学习助手的专家。";
        }
    }
}
