package com.study.service;

import com.study.common.BusinessException;
import com.study.common.SecurityUtils;
import com.study.common.UserContext;
import com.study.entity.LearningMaterial;
import com.study.mapper.LearningMaterialMapper;
import com.study.service.impl.MaterialAsyncProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 在线资源搜索与导入服务
 * 从互联网搜索学习资料并导入到系统中
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineResourceService {

    private final LearningMaterialMapper materialMapper;
    private final MaterialAsyncProcessor asyncProcessor;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private static final String DDG_URL = "https://lite.duckduckgo.com/lite/";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    /**
     * 搜索在线资源
     * 使用 DuckDuckGo Lite（免费免 Key），解析 HTML 结果
     *
     * @param keyword 搜索关键词
     * @param page    页码（DDG 默认每页约 20 条）
     */
    public Map<String, Object> search(String keyword, int page) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            String url = DDG_URL + "?q=" + java.net.URLEncoder.encode(keyword, "UTF-8");
            // DDG Lite 分页参数
            if (page > 1) {
                url += "&s=" + ((page - 1) * 20);
            }

            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .get();

            // 解析结果行：<tr class="result-snippet"> 包含标题链接和摘要
            Elements rows = doc.select("tr.result-snippet");
            for (Element row : rows) {
                Element linkEl = row.selectFirst("a.result-link");
                Element snippetEl = row.selectFirst("td.result-snippet");

                if (linkEl == null) continue;
                String title = linkEl.text().trim();
                String href = linkEl.attr("href");
                String snippet = snippetEl != null ? snippetEl.text().trim() : "";

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", UUID.nameUUIDFromBytes(href.getBytes()).toString());
                item.put("title", title);
                item.put("url", href);
                item.put("snippet", snippet.length() > 300 ? snippet.substring(0, 300) + "..." : snippet);
                item.put("source", extractDomain(href));
                results.add(item);
            }

            log.info("在线搜索完成: keyword={}, results={}", keyword, results.size());
        } catch (Exception e) {
            log.error("在线搜索失败: keyword={}", keyword, e);
            // 不需要抛出异常，返回空结果让前端显示友好提示
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("results", results);
        result.put("total", results.size());
        result.put("page", page);
        return result;
    }

    /**
     * 导入在线资源
     * 抓取网页正文 → 保存为 txt → 走现有处理管线
     */
    public Map<String, Object> importResource(String url, String title) {
        Long userId = UserContext.getCurrentUserId();

        // 0. SSRF 防护：校验 URL 安全性
        SecurityUtils.validateUrl(url);

        // 1. 抓取网页内容
        String content;
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(15000)
                    .get();
            // 移除 script/style 标签
            doc.select("script, style, nav, footer, header, aside").remove();
            content = doc.body().text();
            if (content.length() < 100) {
                throw new BusinessException(400, "网页内容过短，无法导入");
            }
        } catch (IOException e) {
            log.error("网页抓取失败: url={}", url, e);
            throw new BusinessException(400, "网页抓取失败，请检查链接是否可访问");
        }

        // 2. 生成文件名并保存（使用 UUID 避免路径穿越和文件名冲突）
        String ext = "txt";
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relativePath = userId + "/" + dateDir + "/" + storedName;
        Path fullPath = Paths.get(uploadDir, relativePath);

        try {
            Files.createDirectories(fullPath.getParent());
            Files.writeString(fullPath, content);
        } catch (IOException e) {
            throw new BusinessException(500, "文件保存失败");
        }

        // 3. 插入数据库记录
        LearningMaterial material = new LearningMaterial();
        material.setUserId(userId);
        material.setOriginalName(title);
        material.setStoredName(storedName);
        material.setFileType(ext);
        material.setFileSize((long) content.length());
        material.setFilePath(relativePath);
        material.setCategory("在线资源");
        material.setStatus("processing");
        material.setChunkCount(0);
        material.setSource("online");
        materialMapper.insert(material);

        // 4. 异步处理（走现有管线：解析→切片→向量化）
        asyncProcessor.processMaterial(material.getId(), fullPath.toString(), ext, userId);

        log.info("在线资源导入成功: url={}, materialId={}", url, material.getId());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("materialId", material.getId());
        result.put("status", "processing");
        result.put("title", title);
        return result;
    }

    /**
     * 查询导入状态
     */
    public Map<String, Object> getImportStatus(Long materialId) {
        Long userId = UserContext.getCurrentUserId();
        LearningMaterial material = materialMapper.selectById(materialId);
        if (material == null || !material.getUserId().equals(userId)) {
            throw new BusinessException(404, "资料不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("materialId", material.getId());
        result.put("status", material.getStatus());
        result.put("errorMsg", material.getErrorMsg());
        return result;
    }

    private String extractDomain(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            return host != null ? host.replace("www.", "") : url;
        } catch (Exception e) {
            return url;
        }
    }
}
