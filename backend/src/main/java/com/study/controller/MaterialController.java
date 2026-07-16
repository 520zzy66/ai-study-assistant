package com.study.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.common.Result;
import com.study.common.UserContext;
import com.study.dto.request.MaterialListRequest;
import com.study.dto.request.MaterialMoveRequest;
import com.study.service.MaterialService;
import com.study.vo.MaterialUploadVO;
import com.study.vo.MaterialVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 学习资料控制器
 */
@Tag(name = "学习资料管理", description = "学习资料上传、查询、删除等接口")
@RestController
@RequestMapping("/material")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    /**
     * 上传文件
     *
     * @param file     文件（multipart）
     * @param category 分类（可选）
     * @return 上传结果
     */
    @Operation(summary = "上传学习资料", description = "上传 PDF/DOC/DOCX/TXT 格式的学习资料文件")
    @PostMapping("/upload")
    public Result<MaterialUploadVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "folderId", required = false) Long folderId) {
        MaterialUploadVO vo = materialService.upload(file, category, folderId);
        return Result.success("上传成功", vo);
    }

    /**
     * 获取资料列表（分页）
     *
     * @param request 查询条件
     * @return 分页结果
     */
    @Operation(summary = "获取资料列表", description = "分页查询当前用户的学习资料列表")
    @GetMapping("/list")
    public Result<Page<MaterialVO>> list(@Valid MaterialListRequest request) {
        Page<MaterialVO> page = materialService.list(request);
        return Result.success(page);
    }

    /**
     * 获取资料详情
     *
     * @param id 资料ID
     * @return 资料详情（含摘要）
     */
    @Operation(summary = "获取资料详情", description = "获取指定学习资料的详细信息，包含 AI 生成的摘要")
    @GetMapping("/{id}")
    public Result<MaterialVO> getDetail(@PathVariable Long id) {
        MaterialVO vo = materialService.getDetail(id);
        return Result.success(vo);
    }

    /**
     * 预览文件
     *
     * @param id 资料ID
     * @return 文件资源流
     */
    @Operation(summary = "预览学习资料", description = "返回文件的字节流，用于前端预览")
    @GetMapping("/{id}/preview")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> preview(@PathVariable Long id) {
        org.springframework.core.io.Resource resource = materialService.preview(id);
        
        String contentType = "application/octet-stream";
        String filename = resource.getFilename();
        if (filename != null) {
            if (filename.toLowerCase().endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (filename.toLowerCase().endsWith(".txt")) {
                contentType = "text/plain";
            } else if (filename.toLowerCase().endsWith(".doc")) {
                contentType = "application/msword";
            } else if (filename.toLowerCase().endsWith(".docx")) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            }
        }
        
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(contentType))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * 删除资料
     *
     * @param id 资料ID
     * @return 操作结果
     */
    @Operation(summary = "删除学习资料", description = "删除指定的学习资料及其关联的切片数据")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return Result.successMsg("删除成功");
    }

    /**
     * 重新处理失败的资料
     *
     * @param id 资料ID
     * @return 操作结果
     */
    @Operation(summary = "重新处理资料", description = "对处理失败的资料重新执行解析和切片流程")
    @PostMapping("/{id}/retry")
    public Result<Void> retry(@PathVariable Long id) {
        materialService.retryProcess(id);
        return Result.successMsg("已加入处理队列");
    }

    /**
     * 查询系统资料库（公共预置资料）
     *
     * @param keyword  搜索关键词（可选）
     * @param category 分类筛选（可选）
     * @param page     页码
     * @param size     每页大小
     * @return 分页结果
     */
    @Operation(summary = "系统资料库", description = "查询系统预置的公共学习资料")
    @GetMapping("/library")
    public Result<Page<MaterialVO>> library(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<MaterialVO> result = materialService.listLibrary(keyword, category, page, size);
        return Result.success(result);
    }

    /**
     * 将系统资料添加到我的资料库
     *
     * @param id 系统资料ID
     * @return 新资料ID
     */
    @Operation(summary = "添加到我的资料库", description = "将系统资料库的资料复制到当前用户的资料库")
    @PostMapping("/library/{id}/copy")
    public Result<Long> copyToMyLibrary(@PathVariable Long id) {
        Long newId = materialService.copyToMyLibrary(id);
        return Result.success("添加成功", newId);
    }

    /**
     * 获取所有可用资料（用户自己的 + 系统资料库），用于 AI 功能下拉选择
     *
     * @return 资料列表
     */
    @Operation(summary = "获取所有可用资料", description = "获取当前用户的资料和系统资料库，用于 AI 功能选择")
    @GetMapping("/available")
    public Result<List<MaterialVO>> available() {
        List<MaterialVO> list = materialService.listAvailable();
        return Result.success(list);
    }

    /**
     * 批量移动资料到文件夹
     *
     * @param request 移动请求（包含资料ID列表和目标文件夹ID）
     * @return 操作结果
     */
    @Operation(summary = "移动资料到文件夹", description = "批量移动资料到指定文件夹，folderId为null时移出文件夹")
    @PutMapping("/move")
    public Result<Void> moveMaterials(@Valid @RequestBody MaterialMoveRequest request) {
        Long userId = UserContext.getCurrentUserId();
        materialService.moveMaterials(userId, request.getMaterialIds(), request.getFolderId());
        return Result.successMsg("移动成功");
    }
}
