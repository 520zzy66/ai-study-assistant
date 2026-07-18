package com.study.ai.multimodal.service;

import com.study.config.ResourceAssetProperties;
import com.study.dto.vo.ResourceAssetVO;
import com.study.entity.ResourceAsset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 资源资产 Entity ↔ VO 转换器。
 *
 * <p>统一拼接 previewUrl / downloadUrl，避免在多处重复构造。
 * 不输出 storagePath 等磁盘敏感字段。
 */
@Component
@RequiredArgsConstructor
public class ResourceAssetConverter {

    private final ResourceAssetProperties properties;

    /**
     * 将 Entity 转换为 VO，附带 previewUrl / downloadUrl。
     *
     * @param entity 资源资产实体
     * @return 视图对象
     */
    public ResourceAssetVO toVO(ResourceAsset entity) {
        if (entity == null) {
            return null;
        }
        ResourceAssetVO vo = new ResourceAssetVO();
        vo.setAssetId(entity.getAssetId());
        vo.setPackageId(entity.getPackageId());
        vo.setTaskId(entity.getTaskId());
        vo.setMaterialId(entity.getMaterialId());
        vo.setAssetType(entity.getAssetType());
        vo.setAssetRole(entity.getAssetRole());
        vo.setStatus(entity.getStatus());
        vo.setProvider(entity.getProvider());
        vo.setModel(entity.getModel());
        vo.setFileName(entity.getFileName());
        vo.setContentType(entity.getContentType());
        vo.setSizeBytes(entity.getSizeBytes());
        vo.setDurationSeconds(entity.getDurationSeconds());
        vo.setWidth(entity.getWidth());
        vo.setHeight(entity.getHeight());
        vo.setPromptSummary(entity.getPromptSummary());
        vo.setErrorCode(entity.getErrorCode());
        vo.setErrorMessage(entity.getErrorMessage());
        vo.setCreatedAt(entity.getCreatedAt());

        // 仅成功资产设置下载链接，避免前端对 failed 资产发起无意义的 404 请求
        if ("success".equals(entity.getStatus())) {
            String prefix = properties.getPublicUrlPrefix();
            String assetId = entity.getAssetId();
            if (assetId != null && !assetId.isBlank()) {
                String base = prefix + "/" + assetId + "/download";
                vo.setPreviewUrl(base);
                vo.setDownloadUrl(base);
            }
        }
        return vo;
    }
}
