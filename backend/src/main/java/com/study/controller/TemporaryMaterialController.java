package com.study.controller;

import com.study.common.Result;
import com.study.dto.request.TemporaryMaterialPromoteRequest;
import com.study.service.TemporaryMaterialService;
import com.study.vo.TemporaryMaterialVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 会话临时资料接口。
 */
@Tag(name = "临时资料", description = "AI 问答临时资料上传、查询、转正式和删除")
@RestController
@RequestMapping("/material/temporary")
@RequiredArgsConstructor
public class TemporaryMaterialController {

    private final TemporaryMaterialService temporaryMaterialService;

    /** 上传临时资料。 */
    @Operation(summary = "上传临时资料", description = "资料保留7天，不会自动进入我的资料")
    @PostMapping("/upload")
    public Result<TemporaryMaterialVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("conversationId") String conversationId) {
        return Result.success("上传成功", temporaryMaterialService.upload(file, conversationId));
    }

    /** 查询临时资料列表。 */
    @Operation(summary = "查询临时资料")
    @GetMapping
    public Result<List<TemporaryMaterialVO>> list() {
        return Result.success(temporaryMaterialService.list());
    }

    /** 查询临时资料详情。 */
    @Operation(summary = "查询临时资料详情")
    @GetMapping("/{uploadToken}")
    public Result<TemporaryMaterialVO> detail(@PathVariable String uploadToken) {
        return Result.success(temporaryMaterialService.getDetail(uploadToken));
    }

    /** 删除临时资料。 */
    @Operation(summary = "删除临时资料")
    @DeleteMapping("/{uploadToken}")
    public Result<Void> delete(@PathVariable String uploadToken) {
        temporaryMaterialService.delete(uploadToken);
        return Result.successMsg("删除成功");
    }

    /** 添加到我的资料。 */
    @Operation(summary = "添加临时资料到我的资料")
    @PostMapping("/{uploadToken}/promote")
    public Result<Long> promote(@PathVariable String uploadToken,
                                @Valid @RequestBody TemporaryMaterialPromoteRequest request) {
        return Result.success("添加成功", temporaryMaterialService.promote(uploadToken, request));
    }
}
