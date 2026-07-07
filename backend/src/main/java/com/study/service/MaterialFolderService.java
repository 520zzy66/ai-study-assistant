package com.study.service;

import com.study.dto.request.FolderCreateRequest;
import com.study.dto.request.FolderUpdateRequest;
import com.study.vo.FolderVO;

import java.util.List;

/**
 * 资料文件夹服务接口
 */
public interface MaterialFolderService {

    /**
     * 创建文件夹
     *
     * @param userId  当前用户 ID
     * @param request 创建请求
     * @return 新文件夹 VO
     */
    FolderVO createFolder(Long userId, FolderCreateRequest request);

    /**
     * 更新文件夹（重命名 / 移动）
     *
     * @param userId  当前用户 ID
     * @param id      文件夹 ID
     * @param request 更新请求
     * @return 更新后的 VO
     */
    FolderVO updateFolder(Long userId, Long id, FolderUpdateRequest request);

    /**
     * 删除文件夹（逻辑删除）
     * <p>删除前会检查是否有子文件夹，有则拒绝；
     * 关联的资料会被移出该文件夹（folder_id 置为 NULL）。</p>
     *
     * @param userId 当前用户 ID
     * @param id     文件夹 ID
     */
    void deleteFolder(Long userId, Long id);

    /**
     * 获取当前用户的文件夹树
     *
     * @param userId 当前用户 ID
     * @return 根文件夹列表（含 children）
     */
    List<FolderVO> listFolderTree(Long userId);
}
