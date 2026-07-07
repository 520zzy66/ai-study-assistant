package com.study.controller;

import com.study.common.Result;
import com.study.common.UserContext;
import com.study.dto.request.FolderCreateRequest;
import com.study.dto.request.FolderUpdateRequest;
import com.study.dto.request.MaterialMoveRequest;
import com.study.service.MaterialFolderService;
import com.study.vo.FolderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资料文件夹控制器
 */
@Tag(name = "资料文件夹", description = "文件夹 CRUD、资料移动到文件夹")
@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
public class MaterialFolderController {

    private final MaterialFolderService folderService;

    @Operation(summary = "获取文件夹树")
    @GetMapping("/tree")
    public Result<List<FolderVO>> getFolderTree() {
        Long userId = UserContext.getCurrentUserId();
        return Result.success(folderService.listFolderTree(userId));
    }

    @Operation(summary = "创建文件夹")
    @PostMapping
    public Result<FolderVO> createFolder(@Valid @RequestBody FolderCreateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success("创建成功", folderService.createFolder(userId, request));
    }

    @Operation(summary = "更新文件夹（重命名/移动）")
    @PutMapping("/{id}")
    public Result<FolderVO> updateFolder(@PathVariable Long id,
                                         @Valid @RequestBody FolderUpdateRequest request) {
        Long userId = UserContext.getCurrentUserId();
        return Result.success("更新成功", folderService.updateFolder(userId, id, request));
    }

    @Operation(summary = "删除文件夹")
    @DeleteMapping("/{id}")
    public Result<Void> deleteFolder(@PathVariable Long id) {
        Long userId = UserContext.getCurrentUserId();
        folderService.deleteFolder(id, userId);
        return Result.successMsg("删除成功");
    }
}
