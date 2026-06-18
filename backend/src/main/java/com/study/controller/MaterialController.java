package com.study.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.common.Result;
import com.study.dto.request.MaterialListRequest;
import com.study.service.MaterialService;
import com.study.vo.MaterialUploadVO;
import com.study.vo.MaterialVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            @RequestParam(value = "category", required = false) String category) {
        MaterialUploadVO vo = materialService.upload(file, category);
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
}
