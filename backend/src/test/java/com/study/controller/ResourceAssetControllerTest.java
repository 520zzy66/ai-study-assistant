package com.study.controller;

import com.study.ai.multimodal.service.ResourceAssetService;
import com.study.ai.multimodal.storage.ResourceAssetStorageService;
import com.study.common.BusinessException;
import com.study.common.Result;
import com.study.common.UserContext;
import com.study.dto.vo.ResourceAssetCapabilitiesVO;
import com.study.dto.vo.ResourceAssetVO;
import com.study.dto.vo.VoiceOption;
import com.study.entity.ResourceAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 资源资产控制器单元测试（spec §13.1）。
 *
 * <p>覆盖点：能力查询、列表、下载、重试权限。
 *
 * <p>实现说明：通过 {@link UserContext#setCurrentUser} 模拟登录用户，
 * 不启动 Spring MVC 上下文，直接调用 Controller 方法并断言返回值。
 */
@ExtendWith(MockitoExtension.class)
class ResourceAssetControllerTest {

    @Mock private ResourceAssetService assetService;
    @Mock private ResourceAssetStorageService storageService;

    @InjectMocks
    private ResourceAssetController controller;

    @BeforeEach
    void setUp() {
        UserContext.setCurrentUser(new UserContext.UserInfo(1L, "test-user"));
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // ============== GET /resource-assets/capabilities ==============

    @Test
    void capabilities_ReturnsResultWithVO() {
        ResourceAssetCapabilitiesVO vo = new ResourceAssetCapabilitiesVO();
        vo.setTtsEnabled(true);
        vo.setImageEnabled(false);
        vo.setVoices(List.of(new VoiceOption("xiaoyan", "小燕", "female")));
        vo.setImageStyles(List.of("clean_edu", "diagram"));
        vo.setMaxImageCount(4);
        vo.setDefaultVoice("xiaoyan");
        vo.setDefaultImageStyle("clean_edu");
        when(assetService.getCapabilities()).thenReturn(vo);

        Result<ResourceAssetCapabilitiesVO> result = controller.capabilities();

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertSame(vo, result.getData());
        assertTrue(result.getData().getTtsEnabled());
        assertFalse(result.getData().getImageEnabled());
        verify(assetService).getCapabilities();
    }

    @Test
    void capabilities_DoesNotRequireUserIdCheck() {
        // capabilities 不需要 userId，调用前清理 ThreadLocal
        UserContext.clear();
        when(assetService.getCapabilities()).thenReturn(new ResourceAssetCapabilitiesVO());

        assertDoesNotThrow(() -> controller.capabilities());

        // 恢复 ThreadLocal 给后续测试
        UserContext.setCurrentUser(new UserContext.UserInfo(1L, "test-user"));
    }

    // ============== GET /resource-packages/{packageId}/assets ==============

    @Test
    void listPackageAssets_ReturnsFilteredAssetsForCurrentUser() {
        ResourceAssetVO vo1 = new ResourceAssetVO();
        vo1.setAssetId("asset-1");
        vo1.setAssetType("audio");
        vo1.setStatus("success");
        ResourceAssetVO vo2 = new ResourceAssetVO();
        vo2.setAssetId("asset-2");
        vo2.setAssetType("image");
        vo2.setStatus("success");
        when(assetService.listByPackageId(eq("pkg-1"), eq(1L)))
                .thenReturn(List.of(vo1, vo2));

        Result<List<ResourceAssetVO>> result = controller.listPackageAssets("pkg-1");

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertEquals(2, result.getData().size());
        // 验证：userId 来自 UserContext
        verify(assetService).listByPackageId("pkg-1", 1L);
    }

    @Test
    void listPackageAssets_EmptyPackage_ReturnsEmptyList() {
        when(assetService.listByPackageId(anyString(), anyLong()))
                .thenReturn(List.of());

        Result<List<ResourceAssetVO>> result = controller.listPackageAssets("pkg-empty");

        assertEquals(200, result.getCode());
        assertNotNull(result.getData());
        assertTrue(result.getData().isEmpty());
    }

    // ============== GET /resource-assets/{assetId}/download ==============

    @Test
    void download_Success_ReturnsFileBytes() {
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset-1");
        asset.setUserId(1L);
        asset.setStoragePath("abc123.mp3");
        asset.setFileName("audio_xiaoyan.mp3");
        asset.setContentType("audio/mpeg");
        when(assetService.getOwnedAsset("asset-1", 1L)).thenReturn(asset);
        when(storageService.exists("abc123.mp3")).thenReturn(true);

        byte[] content = new byte[]{0x49, 0x44, 0x33, 0x10, 0x20, 0x30};
        when(storageService.load("abc123.mp3")).thenReturn(content);

        ResponseEntity<org.springframework.core.io.Resource> response = controller.download("asset-1");

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof ByteArrayResource);
        ByteArrayResource resource = (ByteArrayResource) response.getBody();
        assertEquals(content.length, resource.contentLength());

        // 验证 Content-Type
        assertEquals(MediaType.parseMediaType("audio/mpeg"), response.getHeaders().getContentType());
        // 验证 Content-Disposition 为 inline
        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.startsWith("inline"));
        assertTrue(contentDisposition.contains("audio_xiaoyan.mp3"));
        // 验证 Content-Length
        assertEquals(String.valueOf(content.length),
                response.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH));
    }

    @Test
    void download_AssetNotOwned_Throws404() {
        when(assetService.getOwnedAsset("asset-other", 1L))
                .thenThrow(new BusinessException(404, "资产不存在或无访问权限"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.download("asset-other"));
        assertEquals(404, ex.getCode());
        // 验证：未访问 storageService
        verify(storageService, never()).exists(anyString());
        verify(storageService, never()).load(anyString());
    }

    @Test
    void download_StoragePathNull_Throws404AndMarksFileMissing() {
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset-1");
        asset.setUserId(1L);
        asset.setStoragePath(null);  // 路径为空
        when(assetService.getOwnedAsset("asset-1", 1L)).thenReturn(asset);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.download("asset-1"));
        assertEquals(404, ex.getCode());
        // 验证：标记文件缺失
        verify(assetService).markFileMissing(asset);
        // 验证：未调用 load
        verify(storageService, never()).load(anyString());
    }

    @Test
    void download_StoragePathBlank_Throws404AndMarksFileMissing() {
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset-1");
        asset.setUserId(1L);
        asset.setStoragePath("   ");
        when(assetService.getOwnedAsset("asset-1", 1L)).thenReturn(asset);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.download("asset-1"));
        assertEquals(404, ex.getCode());
        verify(assetService).markFileMissing(asset);
    }

    @Test
    void download_FileNotExists_Throws404AndMarksFileMissing() {
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset-1");
        asset.setUserId(1L);
        asset.setStoragePath("missing.mp3");
        when(assetService.getOwnedAsset("asset-1", 1L)).thenReturn(asset);
        when(storageService.exists("missing.mp3")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.download("asset-1"));
        assertEquals(404, ex.getCode());
        // 验证：标记文件缺失
        verify(assetService).markFileMissing(asset);
        // 验证：未调用 load
        verify(storageService, never()).load(anyString());
    }

    @Test
    void download_NullContentType_FallsBackToOctetStream() {
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset-1");
        asset.setUserId(1L);
        asset.setStoragePath("file.bin");
        asset.setFileName("file.bin");
        asset.setContentType(null);  // null → 应回退到 application/octet-stream
        when(assetService.getOwnedAsset("asset-1", 1L)).thenReturn(asset);
        when(storageService.exists("file.bin")).thenReturn(true);
        when(storageService.load("file.bin")).thenReturn(new byte[]{0x01, 0x02, 0x03});

        ResponseEntity<org.springframework.core.io.Resource> response = controller.download("asset-1");

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
    }

    @Test
    void download_FilenameWithTraversalChars_SanitizedInContentDisposition() {
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset-1");
        asset.setUserId(1L);
        asset.setStoragePath("file.png");
        asset.setFileName("../../etc/passwd.png");  // 恶意文件名
        asset.setContentType("image/png");
        when(assetService.getOwnedAsset("asset-1", 1L)).thenReturn(asset);
        when(storageService.exists("file.png")).thenReturn(true);
        when(storageService.load("file.png")).thenReturn(new byte[]{0x01});

        ResponseEntity<org.springframework.core.io.Resource> response = controller.download("asset-1");

        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(contentDisposition);
        // 验证：文件名已被清洗，不应包含 ".." 或 "/"
        assertFalse(contentDisposition.contains(".."), "Content-Disposition 不应包含路径穿越字符");
        assertFalse(contentDisposition.contains("/etc"), "Content-Disposition 不应包含路径");
        // 应包含文件名最后一段
        assertTrue(contentDisposition.contains("passwd.png"));
    }

    // ============== POST /resource-assets/{assetId}/retry ==============

    @Test
    void retry_Success_ReturnsUpdatedVO() {
        ResourceAssetVO vo = new ResourceAssetVO();
        vo.setAssetId("asset-1");
        vo.setStatus("success");
        when(assetService.retryAsset("asset-1", 1L)).thenReturn(vo);

        Result<ResourceAssetVO> result = controller.retry("asset-1");

        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertSame(vo, result.getData());
        // 验证：消息为 "已重新生成"
        assertEquals("已重新生成", result.getMessage());
        verify(assetService).retryAsset("asset-1", 1L);
    }

    @Test
    void retry_AssetNotFailed_Throws400() {
        when(assetService.retryAsset(anyString(), anyLong()))
                .thenThrow(new BusinessException(400, "只允许重试 failed 状态的资产"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.retry("asset-1"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("failed"));
    }

    @Test
    void retry_AssetNotOwned_Throws404() {
        when(assetService.retryAsset(anyString(), anyLong()))
                .thenThrow(new BusinessException(404, "资产不存在或无访问权限"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.retry("asset-other"));
        assertEquals(404, ex.getCode());
    }

    @Test
    void retry_AudioAssetMissingOriginalText_Throws400() {
        when(assetService.retryAsset(anyString(), anyLong()))
                .thenThrow(new BusinessException(400, "原始脚本缺失，无法重试"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.retry("asset-1"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("原始脚本"));
    }

    @Test
    void retry_ImageAssetMissingOriginalPrompt_Throws400() {
        when(assetService.retryAsset(anyString(), anyLong()))
                .thenThrow(new BusinessException(400, "原始提示词缺失，无法重试"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.retry("asset-1"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("原始提示词"));
    }

    @Test
    void retry_UnsupportedAssetType_Throws400() {
        when(assetService.retryAsset(anyString(), anyLong()))
                .thenThrow(new BusinessException(400, "不支持的资产类型: video"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> controller.retry("asset-1"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不支持的资产类型"));
    }

    // ============== 用户上下文 - 权限校验 ==============

    @Test
    void listPackageAssets_UsesCurrentUserIdFromContext() {
        // 设置不同的 userId 验证
        UserContext.clear();
        UserContext.setCurrentUser(new UserContext.UserInfo(42L, "another-user"));
        when(assetService.listByPackageId(anyString(), eq(42L)))
                .thenReturn(List.of());

        controller.listPackageAssets("pkg-42");

        // 验证：调用时使用的是 42L 而非 1L
        verify(assetService).listByPackageId("pkg-42", 42L);
    }

    @Test
    void download_UsesCurrentUserIdFromContext() {
        UserContext.clear();
        UserContext.setCurrentUser(new UserContext.UserInfo(99L, "third-user"));
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset-99");
        asset.setUserId(99L);
        asset.setStoragePath("file.mp3");
        asset.setFileName("file.mp3");
        asset.setContentType("audio/mpeg");
        when(assetService.getOwnedAsset("asset-99", 99L)).thenReturn(asset);
        when(storageService.exists("file.mp3")).thenReturn(true);
        when(storageService.load("file.mp3")).thenReturn(new byte[]{0x01});

        controller.download("asset-99");

        verify(assetService).getOwnedAsset("asset-99", 99L);
    }
}
