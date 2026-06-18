package com.study.ai.validator;

import com.study.common.BusinessException;
import com.study.common.Constants;
import com.study.entity.LearningMaterial;
import com.study.mapper.LearningMaterialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 资料校验器
 * 提供统一的资料归属和状态校验逻辑，避免多个 AI Service 重复代码
 */
@Component
@RequiredArgsConstructor
public class MaterialValidator {

    private final LearningMaterialMapper materialMapper;

    /**
     * 校验资料归属和状态，返回校验通过的资料实体
     *
     * @param materialId 资料ID
     * @param userId     当前用户ID
     * @return 校验通过的资料实体
     * @throws BusinessException 资料不存在、无权访问、或资料未就绪
     */
    public LearningMaterial validateAndGet(Long materialId, Long userId) {
        LearningMaterial material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(404, "资料不存在");
        }
        if (!material.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该资源");
        }
        if (!Constants.STATUS_READY.equals(material.getStatus())) {
            throw new BusinessException(3001, "资料尚未处理完成，请稍后再试");
        }
        return material;
    }

    /**
     * 校验资料归属（不限制状态），返回校验通过的资料实体
     * 用于总结等允许 processing 状态的场景
     *
     * @param materialId 资料ID
     * @param userId     当前用户ID
     * @return 校验通过的资料实体
     * @throws BusinessException 资料不存在或无权访问
     */
    public LearningMaterial validateOwnership(Long materialId, Long userId) {
        LearningMaterial material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new BusinessException(404, "资料不存在");
        }
        if (!material.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该资源");
        }
        return material;
    }
}
