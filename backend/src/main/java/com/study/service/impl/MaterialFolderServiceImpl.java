package com.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.study.common.BusinessException;
import com.study.dto.request.FolderCreateRequest;
import com.study.dto.request.FolderUpdateRequest;
import com.study.entity.LearningMaterial;
import com.study.entity.MaterialFolder;
import com.study.mapper.LearningMaterialMapper;
import com.study.mapper.MaterialFolderMapper;
import com.study.service.MaterialFolderService;
import com.study.vo.FolderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 资料文件夹服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialFolderServiceImpl implements MaterialFolderService {

    private final MaterialFolderMapper folderMapper;
    private final LearningMaterialMapper materialMapper;

    @Override
    @Transactional
    public FolderVO createFolder(Long userId, FolderCreateRequest request) {
        // 校验父文件夹归属
        if (request.getParentId() != null) {
            MaterialFolder parent = folderMapper.selectById(request.getParentId());
            if (parent == null || !parent.getUserId().equals(userId)) {
                throw new BusinessException(2001, "父文件夹不存在");
            }
            // 防止循环嵌套：父文件夹不能是当前已存在文件夹的子节点（新建场景不涉及）
        }

        // B12 修复：利用 DB 唯一索引 + 异常捕获替代 TOCTOU 竞态的 selectCount
        // 先 insert，若违反唯一约束（uk_folder_user_parent_name）则提示重名
        MaterialFolder folder = new MaterialFolder();
        folder.setUserId(userId);
        folder.setName(request.getName());
        folder.setParentId(request.getParentId());
        folder.setSortOrder(0);
        try {
            folderMapper.insert(folder);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(2002, "当前层级下已有同名文件夹");
        }

        log.info("创建文件夹: userId={}, name={}, parentId={}", userId, request.getName(), request.getParentId());
        return toVO(folder);
    }

    @Override
    @Transactional
    public FolderVO updateFolder(Long userId, Long id, FolderUpdateRequest request) {
        MaterialFolder folder = folderMapper.selectById(id);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException(2001, "文件夹不存在");
        }

        // 不能移动到自身或子孙节点（防循环）
        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new BusinessException(2003, "不能将文件夹移动到自身");
            }
            if (isDescendant(userId, id, request.getParentId())) {
                throw new BusinessException(2003, "不能将文件夹移动到其子文件夹");
            }
            // 校验目标父文件夹归属
            MaterialFolder targetParent = folderMapper.selectById(request.getParentId());
            if (targetParent == null || !targetParent.getUserId().equals(userId)) {
                throw new BusinessException(2001, "目标文件夹不存在");
            }
        }

        // 同级重名检查（排除自身）
        Long siblingCount = folderMapper.selectCount(
                new LambdaQueryWrapper<MaterialFolder>()
                        .eq(MaterialFolder::getUserId, userId)
                        .eq(MaterialFolder::getParentId, request.getParentId())
                        .eq(MaterialFolder::getName, request.getName())
                        .ne(MaterialFolder::getId, id)
        );
        if (siblingCount > 0) {
            throw new BusinessException(2002, "当前层级下已有同名文件夹");
        }

        folder.setName(request.getName());
        folder.setParentId(request.getParentId());
        folder.setUpdateTime(LocalDateTime.now());
        folderMapper.updateById(folder);

        log.info("更新文件夹: id={}, name={}, parentId={}", id, request.getName(), request.getParentId());
        return toVO(folder);
    }

    @Override
    @Transactional
    public void deleteFolder(Long userId, Long id) {
        MaterialFolder folder = folderMapper.selectById(id);
        if (folder == null || !folder.getUserId().equals(userId)) {
            throw new BusinessException(2001, "文件夹不存在");
        }

        // 检查是否有子文件夹
        Long childCount = folderMapper.selectCount(
                new LambdaQueryWrapper<MaterialFolder>()
                        .eq(MaterialFolder::getParentId, id)
        );
        if (childCount > 0) {
            throw new BusinessException(2004, "请先删除该文件夹下的所有子文件夹");
        }

        // 将该文件夹下的资料的 folder_id 置为 NULL（移出文件夹）
        LambdaUpdateWrapper<LearningMaterial> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(LearningMaterial::getFolderId, null)
                .eq(LearningMaterial::getFolderId, id)
                .eq(LearningMaterial::getUserId, userId);
        materialMapper.update(null, updateWrapper);

        // 逻辑删除文件夹
        folderMapper.deleteById(id);

        log.info("删除文件夹: id={}, userId={}", id, userId);
    }

    @Override
    public List<FolderVO> listFolderTree(Long userId) {
        // 一次性查出当前用户所有文件夹
        List<MaterialFolder> allFolders = folderMapper.selectList(
                new LambdaQueryWrapper<MaterialFolder>()
                        .eq(MaterialFolder::getUserId, userId)
                        .orderByAsc(MaterialFolder::getParentId)
                        .orderByAsc(MaterialFolder::getSortOrder)
                        .orderByAsc(MaterialFolder::getId)
        );

        // 统计每个文件夹下的资料数量
        Map<Long, Integer> materialCountMap = new HashMap<>();
        for (MaterialFolder f : allFolders) {
            Long count = materialMapper.selectCount(
                    new LambdaQueryWrapper<LearningMaterial>()
                            .eq(LearningMaterial::getFolderId, f.getId())
                            .eq(LearningMaterial::getUserId, userId)
            );
            materialCountMap.put(f.getId(), count.intValue());
        }

        // 先全部转 VO，用 id 索引
        Map<Long, FolderVO> voMap = new HashMap<>();
        for (MaterialFolder f : allFolders) {
            FolderVO vo = toVO(f);
            vo.setMaterialCount(materialCountMap.getOrDefault(f.getId(), 0));
            vo.setChildren(new ArrayList<>());
            voMap.put(vo.getId(), vo);
        }

        // 构建树：非根文件夹 attach 到父节点的 children
        List<FolderVO> roots = new ArrayList<>();
        for (MaterialFolder f : allFolders) {
            FolderVO vo = voMap.get(f.getId());
            if (f.getParentId() == null) {
                roots.add(vo);
            } else {
                FolderVO parent = voMap.get(f.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                } else {
                    // 父节点已被删除或不存在 → 提升为根
                    roots.add(vo);
                }
            }
        }
        return roots;
    }

    // ===================== 私有方法 =====================

    /**
     * 判断 targetId 是否是 folderId 的子孙节点
     */
    private boolean isDescendant(Long userId, Long folderId, Long targetId) {
        // 逐级向上遍历 targetId 的父链，看是否能遇到 folderId
        Set<Long> visited = new HashSet<>();
        Long current = targetId;
        while (current != null && !visited.contains(current)) {
            if (current.equals(folderId)) return true;
            visited.add(current);
            MaterialFolder node = folderMapper.selectById(current);
            if (node == null || !node.getUserId().equals(userId)) break;
            current = node.getParentId();
        }
        return false;
    }

    private FolderVO toVO(MaterialFolder folder) {
        FolderVO vo = new FolderVO();
        vo.setId(folder.getId());
        vo.setName(folder.getName());
        vo.setParentId(folder.getParentId());
        vo.setSortOrder(folder.getSortOrder());
        vo.setCreateTime(folder.getCreateTime());
        return vo;
    }
}
