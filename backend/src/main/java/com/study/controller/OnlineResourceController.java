package com.study.controller;

import com.study.common.Result;
import com.study.dto.request.ImportResourceRequest;
import com.study.service.OnlineResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 在线资源控制器
 * 支持搜索互联网学习资源并导入到系统资料库
 */
@Tag(name = "在线资源", description = "搜索和导入互联网学习资源")
@RestController
@RequestMapping("/resource")
@RequiredArgsConstructor
public class OnlineResourceController {

    private final OnlineResourceService resourceService;

    /**
     * 搜索在线资源
     */
    @Operation(summary = "搜索在线资源", description = "通过 DuckDuckGo 搜索互联网学习资料")
    @GetMapping("/search")
    public Result<Map<String, Object>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page) {
        return Result.success(resourceService.search(keyword, page));
    }

    /**
     * 导入在线资源
     */
    @Operation(summary = "导入在线资源", description = "抓取网页内容并导入为学习资料")
    @PostMapping("/import")
    public Result<Map<String, Object>> importResource(@Valid @RequestBody ImportResourceRequest request) {
        String title = request.getTitle();
        if (title == null || title.isBlank()) {
            title = request.getUrl();
        }
        return Result.success(resourceService.importResource(request.getUrl(), title));
    }

    /**
     * 查询导入状态
     */
    @Operation(summary = "导入进度", description = "查询在线资源导入的处理进度")
    @GetMapping("/import/{materialId}/status")
    public Result<Map<String, Object>> getImportStatus(@PathVariable Long materialId) {
        return Result.success(resourceService.getImportStatus(materialId));
    }
}
