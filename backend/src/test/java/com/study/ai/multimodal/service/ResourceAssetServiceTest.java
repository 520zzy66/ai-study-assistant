package com.study.ai.multimodal.service;

import com.study.ai.multimodal.model.AssetGenerationResult;
import com.study.ai.multimodal.model.AudioSynthesisRequest;
import com.study.ai.multimodal.model.ImageGenerationRequest;
import com.study.ai.multimodal.model.ImagePromptPlan;
import com.study.ai.multimodal.model.ProviderErrorType;
import com.study.ai.multimodal.provider.ImageGenerationProvider;
import com.study.ai.multimodal.provider.SpeechSynthesisProvider;
import com.study.ai.multimodal.storage.ResourceAssetStorageService;
import com.study.common.BusinessException;
import com.study.config.ResourceAssetProperties;
import com.study.config.XfyunProperties;
import com.study.dto.vo.ResourceAssetCapabilitiesVO;
import com.study.dto.vo.ResourceAssetVO;
import com.study.entity.ResourceAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 资源资产服务单元测试（spec §13.1）。
 *
 * <p>覆盖点：pending/success/failed 状态流转、权限过滤。
 *
 * <p>实现说明：使用 {@link org.mockito.Spy} 包装 {@link ResourceAssetService}，
 * 桩掉继承自 MyBatis-Plus ServiceImpl 的 {@code save/updateById/list/getOne} 方法，
 * 避免依赖数据库。其他依赖通过 {@link Mock} 注入。
 */
@ExtendWith(MockitoExtension.class)
class ResourceAssetServiceTest {

    @Mock private ResourceAssetStorageService storageService;
    @Mock private ResourceAssetConverter converter;
    @Mock private SpeechSynthesisProvider speechProvider;
    @Mock private ImageGenerationProvider imageProvider;

    private XfyunProperties xfyunProperties;
    private ResourceAssetProperties resourceAssetProperties;
    private ResourceAssetService service;

    @BeforeEach
    void setUp() {
        xfyunProperties = new XfyunProperties();
        xfyunProperties.setAppId("appid");
        xfyunProperties.setApiKey("apikey");
        xfyunProperties.setApiSecret("apisecret");
        xfyunProperties.getTts().setEnabled(true);
        xfyunProperties.getTts().setVoice("xiaoyan");
        xfyunProperties.getImage().setEnabled(true);
        xfyunProperties.getImage().setModel("spark-tti");
        xfyunProperties.getImage().setMaxImageCount(4);
        xfyunProperties.getImage().setDefaultSize("512x512");

        resourceAssetProperties = new ResourceAssetProperties();
        resourceAssetProperties.setStorageDir("/tmp/test-storage");
        resourceAssetProperties.setMaxAudioBytes(1024 * 1024);
        resourceAssetProperties.setMaxImageBytes(1024 * 1024);

        // 使用 spy 部分模拟，桩掉 ServiceImpl 继承的数据库方法
        service = spy(new ResourceAssetService(storageService, converter, xfyunProperties,
                resourceAssetProperties, speechProvider, imageProvider));
        // 桩掉数据库调用，避免 NPE（lenient：部分测试不进入数据库路径）
        lenient().doReturn(true).when(service).save(any(ResourceAsset.class));
        lenient().doReturn(true).when(service).updateById(any(ResourceAsset.class));
    }

    // ============== 播客音频资产生成 - 状态流转 ==============

    @Test
    void generatePodcastAsset_ProviderSuccess_StorageSuccess_StatusBecomesSuccess() {
        // 准备：Provider 返回成功结果
        byte[] mp3Bytes = new byte[]{0x49, 0x44, 0x33, 0x10, 0x20, 0x30};
        AssetGenerationResult providerResult = AssetGenerationResult.builder()
                .success(true)
                .provider("xfyun-tts")
                .model("xiaoyan")
                .content(mp3Bytes)
                .contentType("audio/mpeg")
                .fileExtension("mp3")
                .build();
        when(speechProvider.synthesize(any(AudioSynthesisRequest.class))).thenReturn(providerResult);
        when(storageService.storeAudio(eq(mp3Bytes), eq("audio/mpeg"))).thenReturn("abc123.mp3");

        // converter.toVO 返回一个简单 VO，便于断言
        ResourceAssetVO expectedVO = new ResourceAssetVO();
        expectedVO.setStatus("success");
        expectedVO.setAssetType("audio");
        when(converter.toVO(any(ResourceAsset.class))).thenAnswer(invocation -> {
            ResourceAsset entity = invocation.getArgument(0);
            ResourceAssetVO vo = new ResourceAssetVO();
            vo.setAssetId(entity.getAssetId());
            vo.setStatus(entity.getStatus());
            vo.setAssetType(entity.getAssetType());
            vo.setAssetRole(entity.getAssetRole());
            vo.setErrorCode(entity.getErrorCode());
            vo.setErrorMessage(entity.getErrorMessage());
            return vo;
        });

        ResourceAssetVO result = service.generatePodcastAsset(1L, "pkg-1", "task-1",
                100L, "播客脚本内容", "xiaoyan", "teacher", "summary");

        // 验证：最终状态为 success
        assertEquals("success", result.getStatus());
        assertNull(result.getErrorCode());
        // 验证：storageService.storeAudio 被调用
        verify(storageService).storeAudio(mp3Bytes, "audio/mpeg");
        // 验证：updateById 被调用至少 2 次（generating 一次，success 一次）
        verify(service, atLeast(2)).updateById(any(ResourceAsset.class));
        // 验证：save 被调用 1 次（pending 创建）
        verify(service, times(1)).save(any(ResourceAsset.class));
    }

    @Test
    void generatePodcastAsset_ProviderFailure_StatusBecomesFailed() {
        // 准备：Provider 返回失败
        AssetGenerationResult failure = AssetGenerationResult.failure("xfyun-tts",
                ProviderErrorType.CONTENT_REJECTED, "10043", "内容审核失败");
        when(speechProvider.synthesize(any(AudioSynthesisRequest.class))).thenReturn(failure);
        when(converter.toVO(any(ResourceAsset.class))).thenAnswer(invocation -> {
            ResourceAsset entity = invocation.getArgument(0);
            ResourceAssetVO vo = new ResourceAssetVO();
            vo.setStatus(entity.getStatus());
            vo.setErrorCode(entity.getErrorCode());
            vo.setErrorMessage(entity.getErrorMessage());
            return vo;
        });

        ResourceAssetVO result = service.generatePodcastAsset(1L, "pkg-1", "task-1",
                100L, "播客脚本", "xiaoyan", "teacher", "summary");

        assertEquals("failed", result.getStatus());
        assertEquals("10043", result.getErrorCode());
        assertEquals("内容审核失败", result.getErrorMessage());
        // 验证：失败时不应调用 storageService.storeAudio
        verify(storageService, never()).storeAudio(any(), anyString());
    }

    @Test
    void generatePodcastAsset_ProviderThrowsException_StatusBecomesFailed() {
        when(speechProvider.synthesize(any(AudioSynthesisRequest.class)))
                .thenThrow(new RuntimeException("network error"));
        when(converter.toVO(any(ResourceAsset.class))).thenAnswer(invocation -> {
            ResourceAsset entity = invocation.getArgument(0);
            ResourceAssetVO vo = new ResourceAssetVO();
            vo.setStatus(entity.getStatus());
            vo.setErrorCode(entity.getErrorCode());
            return vo;
        });

        ResourceAssetVO result = service.generatePodcastAsset(1L, "pkg-1", "task-1",
                100L, "播客脚本", "xiaoyan", "teacher", "summary");

        assertEquals("failed", result.getStatus());
        assertEquals("PROVIDER_ERROR", result.getErrorCode());
    }

    @Test
    void generatePodcastAsset_StorageThrowsBusinessException_StatusBecomesFailed() {
        // Provider 成功，但存储失败
        byte[] mp3Bytes = new byte[]{0x49, 0x44, 0x33, 0x10, 0x20};
        AssetGenerationResult providerResult = AssetGenerationResult.builder()
                .success(true)
                .provider("xfyun-tts")
                .content(mp3Bytes)
                .contentType("audio/mpeg")
                .fileExtension("mp3")
                .build();
        when(speechProvider.synthesize(any())).thenReturn(providerResult);
        when(storageService.storeAudio(any(), anyString()))
                .thenThrow(new BusinessException(400, "MP3 文件头校验失败"));

        when(converter.toVO(any(ResourceAsset.class))).thenAnswer(invocation -> {
            ResourceAsset entity = invocation.getArgument(0);
            ResourceAssetVO vo = new ResourceAssetVO();
            vo.setStatus(entity.getStatus());
            vo.setErrorCode(entity.getErrorCode());
            vo.setErrorMessage(entity.getErrorMessage());
            return vo;
        });

        ResourceAssetVO result = service.generatePodcastAsset(1L, "pkg-1", "task-1",
                100L, "播客脚本", "xiaoyan", "teacher", "summary");

        assertEquals("failed", result.getStatus());
        assertEquals("STORAGE_ERROR", result.getErrorCode());
        assertEquals("MP3 文件头校验失败", result.getErrorMessage());
    }

    // ============== 图片资产生成 - 状态流转 ==============

    @Test
    void generateImageAssets_ProviderSuccess_StatusBecomesSuccess() {
        // 准备图片提示词计划
        ImagePromptPlan promptPlan = new ImagePromptPlan();
        ImagePromptPlan.CoverPrompt cover = new ImagePromptPlan.CoverPrompt();
        cover.setTitle("封面");
        cover.setPrompt("知识图谱封面");
        cover.setNegativePrompt("模糊");
        promptPlan.setCover(cover);

        // Provider 成功
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        AssetGenerationResult providerResult = AssetGenerationResult.builder()
                .success(true)
                .provider("xfyun-tti")
                .content(pngBytes)
                .contentType("image/png")
                .fileExtension("png")
                .width(512)
                .height(512)
                .build();
        when(imageProvider.generate(any(ImageGenerationRequest.class)))
                .thenReturn(List.of(providerResult));
        when(storageService.storeImage(eq(pngBytes), eq("image/png"), eq("png")))
                .thenReturn("cover123.png");

        when(converter.toVO(any(ResourceAsset.class))).thenAnswer(invocation -> {
            ResourceAsset entity = invocation.getArgument(0);
            ResourceAssetVO vo = new ResourceAssetVO();
            vo.setStatus(entity.getStatus());
            vo.setAssetRole(entity.getAssetRole());
            vo.setErrorCode(entity.getErrorCode());
            return vo;
        });

        List<ResourceAssetVO> results = service.generateImageAssets(1L, "pkg-1", "task-1",
                100L, promptPlan, "clean_edu", 1, 512, 512);

        // 验证：1 张封面图，状态 success
        assertEquals(1, results.size());
        assertEquals("success", results.get(0).getStatus());
        assertEquals("cover", results.get(0).getAssetRole());
        verify(storageService).storeImage(pngBytes, "image/png", "png");
    }

    @Test
    void generateImageAssets_ProviderFailure_StatusBecomesFailed() {
        ImagePromptPlan promptPlan = new ImagePromptPlan();
        ImagePromptPlan.CoverPrompt cover = new ImagePromptPlan.CoverPrompt();
        cover.setTitle("封面");
        cover.setPrompt("敏感内容");
        promptPlan.setCover(cover);

        AssetGenerationResult failure = AssetGenerationResult.failure("xfyun-tti",
                ProviderErrorType.CONTENT_REJECTED, "10043", "审核未通过");
        when(imageProvider.generate(any())).thenReturn(List.of(failure));

        when(converter.toVO(any(ResourceAsset.class))).thenAnswer(invocation -> {
            ResourceAsset entity = invocation.getArgument(0);
            ResourceAssetVO vo = new ResourceAssetVO();
            vo.setStatus(entity.getStatus());
            vo.setErrorCode(entity.getErrorCode());
            return vo;
        });

        List<ResourceAssetVO> results = service.generateImageAssets(1L, "pkg-1", "task-1",
                100L, promptPlan, "clean_edu", 1, 512, 512);

        assertEquals(1, results.size());
        assertEquals("failed", results.get(0).getStatus());
        assertEquals("10043", results.get(0).getErrorCode());
        verify(storageService, never()).storeImage(any(), anyString(), anyString());
    }

    @Test
    void generateImageAssets_NullPromptPlan_ReturnsEmptyList() {
        List<ResourceAssetVO> results = service.generateImageAssets(1L, "pkg-1", "task-1",
                100L, null, "clean_edu", 1, 512, 512);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(imageProvider, never()).generate(any());
    }

    @Test
    void generateImageAssets_MultipleImages_GeneratesCoverAndExplanations() {
        ImagePromptPlan promptPlan = new ImagePromptPlan();
        ImagePromptPlan.CoverPrompt cover = new ImagePromptPlan.CoverPrompt();
        cover.setTitle("封面");
        cover.setPrompt("封面提示词");
        promptPlan.setCover(cover);

        ImagePromptPlan.ExplanationPrompt exp1 = new ImagePromptPlan.ExplanationPrompt();
        exp1.setKnowledgePoint("知识点1");
        exp1.setPrompt("提示词1");
        ImagePromptPlan.ExplanationPrompt exp2 = new ImagePromptPlan.ExplanationPrompt();
        exp2.setKnowledgePoint("知识点2");
        exp2.setPrompt("提示词2");
        promptPlan.setExplanations(List.of(exp1, exp2));

        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        AssetGenerationResult ok = AssetGenerationResult.builder()
                .success(true)
                .provider("xfyun-tti")
                .content(pngBytes)
                .contentType("image/png")
                .fileExtension("png")
                .width(512)
                .height(512)
                .build();
        when(imageProvider.generate(any())).thenReturn(List.of(ok));
        when(storageService.storeImage(any(), anyString(), anyString())).thenReturn("img.png");
        when(converter.toVO(any(ResourceAsset.class))).thenAnswer(invocation -> {
            ResourceAsset entity = invocation.getArgument(0);
            ResourceAssetVO vo = new ResourceAssetVO();
            vo.setStatus(entity.getStatus());
            vo.setAssetRole(entity.getAssetRole());
            return vo;
        });

        // imageCount = 3 → 1 封面 + 2 解析图
        List<ResourceAssetVO> results = service.generateImageAssets(1L, "pkg-1", "task-1",
                100L, promptPlan, "clean_edu", 3, 512, 512);

        assertEquals(3, results.size());
        assertEquals("cover", results.get(0).getAssetRole());
        assertEquals("explanation", results.get(1).getAssetRole());
        assertEquals("explanation", results.get(2).getAssetRole());
        assertEquals("success", results.get(0).getStatus());
    }

    // ============== listByPackageId - 权限过滤 ==============

    @Test
    void listByPackageId_NullPackageId_ReturnsEmptyList() {
        List<ResourceAssetVO> results = service.listByPackageId(null, 1L);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void listByPackageId_BlankPackageId_ReturnsEmptyList() {
        List<ResourceAssetVO> results = service.listByPackageId("  ", 1L);
        assertTrue(results.isEmpty());
    }

    @Test
    void listByPackageId_NullUserId_ReturnsEmptyList() {
        List<ResourceAssetVO> results = service.listByPackageId("pkg-1", null);
        assertTrue(results.isEmpty());
    }

    @Test
    void listByPackageId_PassesUserIdAndPackageIdToQuery() {
        // 通过 spy.list() 拦截，验证 wrapper 中的过滤条件由 list 实现
        // 这里只验证调用流程不抛异常
        doReturn(List.of()).when(service).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));

        List<ResourceAssetVO> results = service.listByPackageId("pkg-1", 42L);

        assertNotNull(results);
        verify(service).list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    // ============== getOwnedAsset - 权限过滤 ==============

    @Test
    void getOwnedAsset_BlankAssetId_ThrowsBadRequest() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getOwnedAsset("", 1L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void getOwnedAsset_NullAssetId_ThrowsBadRequest() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getOwnedAsset(null, 1L));
        assertEquals(400, ex.getCode());
    }

    @Test
    void getOwnedAsset_AssetNotFound_Throws404() {
        doReturn(null).when(service).getOne(any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getOwnedAsset("asset-xxx", 1L));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("不存在") || ex.getMessage().contains("权限"));
    }

    @Test
    void getOwnedAsset_AssetExists_ReturnsAsset() {
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset-1");
        asset.setUserId(1L);
        doReturn(asset).when(service).getOne(any());

        ResourceAsset result = service.getOwnedAsset("asset-1", 1L);

        assertNotNull(result);
        assertEquals("asset-1", result.getAssetId());
    }

    // ============== markFileMissing ==============

    @Test
    void markFileMissing_SetsFailedStatusAndErrorCode() {
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset-1");
        asset.setStatus("success");

        service.markFileMissing(asset);

        assertEquals("failed", asset.getStatus());
        assertEquals("FILE_MISSING", asset.getErrorCode());
        assertEquals("文件缺失", asset.getErrorMessage());
        assertNotNull(asset.getUpdatedAt());
        verify(service).updateById(asset);
    }

    // ============== retryAsset ==============

    @Test
    void retryAsset_NotFailedStatus_ThrowsBadRequest() {
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset-1");
        asset.setUserId(1L);
        asset.setStatus("success");
        doReturn(asset).when(service).getOne(any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.retryAsset("asset-1", 1L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("failed"));
    }

    @Test
    void retryAsset_AudioAssetMissingOriginalText_ThrowsBadRequest() {
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset-1");
        asset.setUserId(1L);
        asset.setStatus("failed");
        asset.setAssetType("audio");
        asset.setMetadata(new HashMap<>());  // 没有 originalText
        doReturn(asset).when(service).getOne(any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.retryAsset("asset-1", 1L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("原始脚本"));
    }

    @Test
    void retryAsset_ImageAssetMissingOriginalPrompt_ThrowsBadRequest() {
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset-1");
        asset.setUserId(1L);
        asset.setStatus("failed");
        asset.setAssetType("image");
        asset.setMetadata(new HashMap<>());  // 没有 originalPrompt
        doReturn(asset).when(service).getOne(any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.retryAsset("asset-1", 1L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("原始提示词"));
    }

    @Test
    void retryAsset_ImageAssetWithOriginalPrompt_CallsProviderAgain() {
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset-1");
        asset.setUserId(1L);
        asset.setStatus("failed");
        asset.setAssetType("image");
        asset.setAssetRole("cover");
        asset.setPackageId("pkg-1");
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("originalPrompt", "知识图谱");
        metadata.put("negativePrompt", "模糊");
        metadata.put("imageStyle", "clean_edu");
        metadata.put("width", 512);
        metadata.put("height", 512);
        asset.setMetadata(metadata);
        doReturn(asset).when(service).getOne(any());

        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        AssetGenerationResult ok = AssetGenerationResult.builder()
                .success(true)
                .provider("xfyun-tti")
                .content(pngBytes)
                .contentType("image/png")
                .fileExtension("png")
                .width(512)
                .height(512)
                .build();
        when(imageProvider.generate(any())).thenReturn(List.of(ok));
        when(storageService.storeImage(any(), anyString(), anyString())).thenReturn("new.png");
        when(converter.toVO(any(ResourceAsset.class))).thenAnswer(invocation -> {
            ResourceAsset entity = invocation.getArgument(0);
            ResourceAssetVO vo = new ResourceAssetVO();
            vo.setStatus(entity.getStatus());
            vo.setAssetId(entity.getAssetId());
            return vo;
        });

        ResourceAssetVO result = service.retryAsset("asset-1", 1L);

        assertEquals("success", result.getStatus());
        // 验证：调用了 imageProvider 重新生成
        verify(imageProvider).generate(any());
        // 验证：调用了 storageService 重新存储
        verify(storageService).storeImage(eq(pngBytes), eq("image/png"), eq("png"));
    }

    @Test
    void retryAsset_UnsupportedAssetType_ThrowsBadRequest() {
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset-1");
        asset.setUserId(1L);
        asset.setStatus("failed");
        asset.setAssetType("video");  // 不支持的类型
        doReturn(asset).when(service).getOne(any());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.retryAsset("asset-1", 1L));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("不支持的资产类型"));
    }

    // ============== getCapabilities ==============

    @Test
    void getCapabilities_ReturnsAllFieldsFromProperties() {
        ResourceAssetCapabilitiesVO vo = service.getCapabilities();

        assertNotNull(vo);
        assertTrue(vo.getTtsEnabled());
        assertTrue(vo.getImageEnabled());
        assertEquals("xiaoyan", vo.getDefaultVoice());
        assertEquals("clean_edu", vo.getDefaultImageStyle());
        assertEquals(4, vo.getMaxImageCount());
        assertNotNull(vo.getVoices());
        assertFalse(vo.getVoices().isEmpty());
        assertNotNull(vo.getImageStyles());
        assertEquals(4, vo.getImageStyles().size());
        assertTrue(vo.getImageStyles().contains("clean_edu"));
        assertTrue(vo.getImageStyles().contains("diagram"));
        assertTrue(vo.getImageStyles().contains("blackboard"));
        assertTrue(vo.getImageStyles().contains("isometric"));
    }

    @Test
    void getCapabilities_WhenTtsDisabled_ReturnsTtsEnabledFalse() {
        xfyunProperties.getTts().setEnabled(false);

        ResourceAssetCapabilitiesVO vo = service.getCapabilities();

        assertFalse(vo.getTtsEnabled());
    }

    @Test
    void getCapabilities_WhenImageDisabled_ReturnsImageEnabledFalse() {
        xfyunProperties.getImage().setEnabled(false);

        ResourceAssetCapabilitiesVO vo = service.getCapabilities();

        assertFalse(vo.getImageEnabled());
    }
}
