package com.study.controller;

import com.study.ai.multimodal.service.ResourceAssetService;
import com.study.ai.multimodal.storage.ResourceAssetStorageService;
import com.study.common.BusinessException;
import com.study.common.Result;
import com.study.common.SecurityUtils;
import com.study.common.UserContext;
import com.study.dto.vo.ResourceAssetCapabilitiesVO;
import com.study.dto.vo.ResourceAssetVO;
import com.study.entity.ResourceAsset;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 资源资产控制器（spec §9）。
 *
 * <p>提供多模态资产能力查询、列表、下载和重试接口。
 * 所有接口均要求登录，并通过 {@link UserContext} 校验资产归属。
 */
@Slf4j
@Tag(name = "资源资产管理", description = "多模态资产能力查询、列表、下载和重试接口")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ResourceAssetController {

    private final ResourceAssetService assetService;
    private final ResourceAssetStorageService storageService;

    /**
     * 查询多模态资产能力（spec §9.1）。
     *
     * @return 能力视图（不包含任何密钥）
     */
    @Operation(summary = "查询资源资产能力", description = "返回 TTS/文生图可用性、发音人列表、图片风格列表等")
    @GetMapping("/resource-assets/capabilities")
    public Result<ResourceAssetCapabilitiesVO> capabilities() {
        return Result.success(assetService.getCapabilities());
    }

    /**
     * 查询资源包下的资产列表（spec §9.2）。
     *
     * @param packageId 资源包 ID
     * @return 当前用户拥有的资产列表
     */
    @Operation(summary = "查询资源包资产", description = "返回指定资源包下当前用户拥有的多模态资产")
    @GetMapping("/resource-packages/{packageId}/assets")
    public Result<List<ResourceAssetVO>> listPackageAssets(@PathVariable String packageId) {
        Long userId = UserContext.getCurrentUserId();
        List<ResourceAssetVO> assets = assetService.listByPackageId(packageId, userId);
        return Result.success(assets);
    }

    /**
     * 下载资产文件（spec §9.3）。
     *
     * <p>规则：
     * <ul>
     *   <li>校验资产归属；</li>
     *   <li>文件不存在时返回 404，并把资产状态更新为 failed，错误码 FILE_MISSING；</li>
     *   <li>Content-Type 使用资产记录中的类型；</li>
     *   <li>Content-Disposition 使用安全文件名。</li>
     * </ul>
     *
     * @param assetId 资产 UUID
     * @return 文件字节流
     */
    @Operation(summary = "下载资源资产", description = "返回资产文件字节流，inline 预览或 attachment 下载")
    @GetMapping("/resource-assets/{assetId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable String assetId) {
        Long userId = UserContext.getCurrentUserId();
        ResourceAsset asset = assetService.getOwnedAsset(assetId, userId);

        String storagePath = asset.getStoragePath();
        if (storagePath == null || storagePath.isBlank() || !storageService.exists(storagePath)) {
            // 文件缺失：更新资产状态后返回 404
            assetService.markFileMissing(asset);
            throw new BusinessException(404, "资产文件不存在或已被清理");
        }

        byte[] content = storageService.load(storagePath);
        ByteArrayResource resource = new ByteArrayResource(content);

        String contentType = asset.getContentType() != null ? asset.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String safeFileName = SecurityUtils.sanitizeFilename(asset.getFileName());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFileName + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(content.length))
                .body(resource);
    }

    /**
     * 重试失败资产（spec §9.4）。
     *
     * <p>规则：
     * <ul>
     *   <li>只允许重试 failed 状态资产；</li>
     *   <li>复用原始脚本/提示词；</li>
     *   <li>重试成功后更新原资产记录，不新建资产。</li>
     * </ul>
     *
     * @param assetId 资产 UUID
     * @return 更新后的资产 VO
     */
    @Operation(summary = "重试失败资产", description = "对 failed 状态的资产复用原始 prompt 重新生成")
    @PostMapping("/resource-assets/{assetId}/retry")
    public Result<ResourceAssetVO> retry(@PathVariable String assetId) {
        Long userId = UserContext.getCurrentUserId();
        ResourceAssetVO vo = assetService.retryAsset(assetId, userId);
        return Result.success("已重新生成", vo);
    }
}
