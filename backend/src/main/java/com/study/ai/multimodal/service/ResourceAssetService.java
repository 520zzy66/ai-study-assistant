package com.study.ai.multimodal.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.study.dto.vo.VoiceOption;
import com.study.entity.ResourceAsset;
import com.study.mapper.ResourceAssetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 资源资产服务。
 *
 * <p>职责：
 * <ul>
 *   <li>创建 pending 资产记录；</li>
 *   <li>调用 Provider 生成内容；</li>
 *   <li>调用 {@link ResourceAssetStorageService} 保存文件；</li>
 *   <li>更新资产状态；</li>
 *   <li>查询当前用户可访问的资源包资产；</li>
 *   <li>生成下载响应；</li>
 *   <li>重试失败资产。</li>
 * </ul>
 *
 * <p>约束：
 * <ul>
 *   <li>必须校验 userId，禁止跨用户读取资产；</li>
 *   <li>文件名使用 UUID，不使用用户输入；</li>
 *   <li>storagePath 保存相对路径，不保存绝对路径到前端；</li>
 *   <li>下载接口通过后端读取文件返回，不直接暴露磁盘路径。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceAssetService extends ServiceImpl<ResourceAssetMapper, ResourceAsset> {

    /** 资产类型 - 音频 */
    public static final String ASSET_TYPE_AUDIO = "audio";
    /** 资产类型 - 图片 */
    public static final String ASSET_TYPE_IMAGE = "image";
    /** 资产角色 - 播客 */
    public static final String ASSET_ROLE_PODCAST = "podcast";
    /** 资产角色 - 封面 */
    public static final String ASSET_ROLE_COVER = "cover";
    /** 资产角色 - 解析图 */
    public static final String ASSET_ROLE_EXPLANATION = "explanation";

    /** 资产状态 - pending */
    public static final String STATUS_PENDING = "pending";
    /** 资产状态 - generating */
    public static final String STATUS_GENERATING = "generating";
    /** 资产状态 - success */
    public static final String STATUS_SUCCESS = "success";
    /** 资产状态 - failed */
    public static final String STATUS_FAILED = "failed";
    /** 资产状态 - cancelled */
    public static final String STATUS_CANCELLED = "cancelled";

    /** 讯飞 TTS 提供方 */
    public static final String PROVIDER_XFYUN_TTS = "xfyun-tts";
    /** 讯飞文生图提供方 */
    public static final String PROVIDER_XFYUN_TTI = "xfyun-tti";
    /** 讯飞 HiDream 图片提供方 */
    public static final String PROVIDER_XFYUN_HIDREAM = "xfyun-hidream";

    /**
     * 支持的发音人列表（MVP 静态，可后续接入讯飞声音列表接口）。
     *
     * <p>needsPermission 标注：
     * <ul>
     *   <li>{@code false}：普通发音人，默认可用（如 xiaoyan）</li>
     *   <li>{@code true}：超自然发音人（ais* 开头），需在讯飞控制台单独开通权限，
     *       未开通时调用会返回错误码 10043</li>
     * </ul>
     */
    private static final List<VoiceOption> SUPPORTED_VOICES = List.of(
            new VoiceOption("xiaoyan", "讯飞小燕", "female", false),
            new VoiceOption("aisjiuxu", "讯飞许久", "male", true),
            new VoiceOption("aisxping", "讯飞小萍", "female", true),
            new VoiceOption("aisjinger", "讯飞小婧", "female", true),
            new VoiceOption("aisbabyxu", "讯飞许小宝", "neutral", true)
    );

    /** 支持的图片风格 */
    private static final List<String> SUPPORTED_IMAGE_STYLES = List.of(
            "clean_edu", "diagram", "blackboard", "isometric"
    );

    private final ResourceAssetStorageService storageService;
    private final ResourceAssetConverter converter;
    private final XfyunProperties xfyunProperties;
    private final ResourceAssetProperties resourceAssetProperties;
    private final SpeechSynthesisProvider speechProvider;
    private final ImageGenerationProvider imageProvider;

    /**
     * 生成播客音频资产。
     *
     * @param userId        当前用户 ID
     * @param packageId     所属资源包 ID
     * @param taskId        生成任务 ID（可空）
     * @param materialId    关联资料 ID（可空）
     * @param script        已清洗的播客脚本
     * @param voice         发音人 ID（为空使用默认）
     * @param podcastStyle  播客风格
     * @param sourceKeys    来源资源 key 列表（逗号分隔）
     * @return 资产 VO
     */
    public ResourceAssetVO generatePodcastAsset(Long userId, String packageId, String taskId,
                                                Long materialId, String script, String voice,
                                                String podcastStyle, String sourceKeys) {
        // 创建 pending 资产记录
        ResourceAsset asset = createPendingAsset(userId, packageId, taskId, materialId,
                ASSET_TYPE_AUDIO, ASSET_ROLE_PODCAST, PROVIDER_XFYUN_TTS,
                xfyunProperties.getTts().getVoice(),
                summarizePrompt(script), sourceKeys);

        // 标记 generating
        asset.setStatus(STATUS_GENERATING);
        updateById(asset);

        // 构造 Provider 请求
        AudioSynthesisRequest request = new AudioSynthesisRequest();
        request.setUserId(userId);
        request.setMaterialId(materialId);
        request.setPackageId(packageId);
        request.setTaskId(taskId);
        request.setPromptSummary(asset.getPromptSummary());
        request.setText(script);
        request.setVoice(voice);
        request.setPodcastStyle(podcastStyle);
        request.setSourceKeys(sourceKeys);

        AssetGenerationResult result;
        try {
            result = speechProvider.synthesize(request);
        } catch (Exception e) {
            log.warn("Speech provider invoke failed: {}", e.getClass().getSimpleName());
            result = AssetGenerationResult.failure(PROVIDER_XFYUN_TTS, ProviderErrorType.UNKNOWN,
                    "PROVIDER_ERROR", "语音合成调用异常");
        }

        // 处理结果
        if (!result.isSuccess()) {
            asset.setStatus(STATUS_FAILED);
            asset.setErrorCode(result.getErrorCode());
            asset.setErrorMessage(result.getErrorMessage());
            updateById(asset);
            log.warn("Podcast asset generation failed: assetId={}, errorCode={}",
                    asset.getAssetId(), result.getErrorCode());
            return converter.toVO(asset);
        }

        try {
            String storagePath = storageService.storeAudio(result.getContent(), result.getContentType());
            asset.setStatus(STATUS_SUCCESS);
            asset.setFileName(buildFileName(result.getFileExtension()));
            asset.setContentType(result.getContentType());
            asset.setStoragePath(storagePath);
            asset.setSizeBytes((long) result.getContent().length);
            asset.setModel(result.getModel());
            asset.setMetadata(buildAudioMetadata(result.getMetadata(), script, voice, podcastStyle));
            updateById(asset);
            log.info("Podcast asset generated: assetId={}, sizeBytes={}",
                    asset.getAssetId(), asset.getSizeBytes());
        } catch (BusinessException e) {
            asset.setStatus(STATUS_FAILED);
            asset.setErrorCode("STORAGE_ERROR");
            asset.setErrorMessage(e.getMessage());
            updateById(asset);
            log.warn("Podcast asset storage failed: assetId={}", asset.getAssetId());
        }
        return converter.toVO(asset);
    }

    /**
     * 生成图片资产列表。
     *
     * @param userId       当前用户 ID
     * @param packageId    所属资源包 ID
     * @param taskId       生成任务 ID（可空）
     * @param materialId   关联资料 ID（可空）
     * @param promptPlan   图片提示词计划（封面 + 解析图）
     * @param imageStyle   图片风格
     * @param imageCount   期望生成的图片数量（含封面）
     * @param defaultWidth  默认宽度
     * @param defaultHeight 默认高度
     * @return 资产 VO 列表
     */
    public List<ResourceAssetVO> generateImageAssets(Long userId, String packageId, String taskId,
                                                     Long materialId, ImagePromptPlan promptPlan,
                                                     String imageStyle, int imageCount,
                                                     int defaultWidth, int defaultHeight) {
        List<ResourceAssetVO> results = new ArrayList<>();

        // 1. 封面图
        if (promptPlan != null && promptPlan.getCover() != null) {
            ImagePromptPlan.CoverPrompt cover = promptPlan.getCover();
            ResourceAssetVO coverVo = generateSingleImage(userId, packageId, taskId, materialId,
                    ASSET_ROLE_COVER, cover.getTitle(),
                    cover.getPrompt(), cover.getNegativePrompt(),
                    imageStyle, defaultWidth, defaultHeight, null);
            results.add(coverVo);
        }

        // 2. 解析图（按 imageCount-1 截取，最少 0 张）
        if (promptPlan != null && promptPlan.getExplanations() != null && imageCount > 1) {
            int explanationCount = Math.min(imageCount - 1, promptPlan.getExplanations().size());
            for (int i = 0; i < explanationCount; i++) {
                ImagePromptPlan.ExplanationPrompt explanation = promptPlan.getExplanations().get(i);
                ResourceAssetVO explanationVo = generateSingleImage(userId, packageId, taskId, materialId,
                        ASSET_ROLE_EXPLANATION, explanation.getKnowledgePoint(),
                        explanation.getPrompt(), explanation.getNegativePrompt(),
                        imageStyle, defaultWidth, defaultHeight, explanation.getKnowledgePoint());
                results.add(explanationVo);
            }
        }
        return results;
    }

    /**
     * 生成单张图片资产。
     */
    private ResourceAssetVO generateSingleImage(Long userId, String packageId, String taskId, Long materialId,
                                                String assetRole, String title, String prompt, String negativePrompt,
                                                String imageStyle, int width, int height, String knowledgePoint) {
        ResourceAsset asset = createPendingAsset(userId, packageId, taskId, materialId,
                ASSET_TYPE_IMAGE, assetRole, imageProviderName(),
                xfyunProperties.getImage().getModel(),
                summarizePrompt(prompt), null);
        asset.setMetadata(buildImageMetadata(null, prompt, negativePrompt,
                imageStyle, width, height, knowledgePoint));

        asset.setStatus(STATUS_GENERATING);
        updateById(asset);

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setUserId(userId);
        request.setMaterialId(materialId);
        request.setPackageId(packageId);
        request.setTaskId(taskId);
        request.setPromptSummary(asset.getPromptSummary());
        request.setPrompt(prompt);
        request.setNegativePrompt(negativePrompt);
        request.setImageStyle(imageStyle);
        request.setWidth(width);
        request.setHeight(height);
        request.setAssetRole(assetRole);
        request.setKnowledgePoint(knowledgePoint);

        List<AssetGenerationResult> results;
        try {
            results = imageProvider.generate(request);
        } catch (Exception e) {
            log.warn("Image provider invoke failed: {}", e.getClass().getSimpleName());
            results = List.of(AssetGenerationResult.failure(imageProviderName(), ProviderErrorType.UNKNOWN,
                    "PROVIDER_ERROR", "文生图调用异常"));
        }

        if (results == null || results.isEmpty()) {
            asset.setStatus(STATUS_FAILED);
            asset.setErrorCode("EMPTY_RESULT");
            asset.setErrorMessage("图片生成结果为空");
            asset.setMetadata(buildImageMetadata(asset.getMetadata(), prompt, negativePrompt,
                    imageStyle, width, height, knowledgePoint));
            updateById(asset);
            return converter.toVO(asset);
        }

        AssetGenerationResult result = results.get(0);
        if (!result.isSuccess()) {
            asset.setStatus(STATUS_FAILED);
            asset.setErrorCode(result.getErrorCode());
            asset.setErrorMessage(result.getErrorMessage());
            asset.setMetadata(buildImageMetadata(result.getMetadata(), prompt, negativePrompt,
                    imageStyle, width, height, knowledgePoint));
            updateById(asset);
            log.warn("Image asset generation failed: assetId={}, role={}, errorCode={}",
                    asset.getAssetId(), assetRole, result.getErrorCode());
            return converter.toVO(asset);
        }

        try {
            String storagePath = storageService.storeImage(result.getContent(), result.getContentType(),
                    result.getFileExtension());
            asset.setStatus(STATUS_SUCCESS);
            asset.setFileName(buildFileName(result.getFileExtension()));
            asset.setContentType(result.getContentType());
            asset.setStoragePath(storagePath);
            asset.setSizeBytes((long) result.getContent().length);
            asset.setWidth(result.getWidth() != null ? result.getWidth() : width);
            asset.setHeight(result.getHeight() != null ? result.getHeight() : height);
            asset.setMetadata(buildImageMetadata(result.getMetadata(), prompt, negativePrompt,
                    imageStyle, width, height, knowledgePoint));
            updateById(asset);
            log.info("Image asset generated: assetId={}, role={}, sizeBytes={}",
                    asset.getAssetId(), assetRole, asset.getSizeBytes());
        } catch (BusinessException e) {
            asset.setStatus(STATUS_FAILED);
            asset.setErrorCode("STORAGE_ERROR");
            asset.setErrorMessage(e.getMessage());
            asset.setMetadata(buildImageMetadata(result.getMetadata(), prompt, negativePrompt,
                    imageStyle, width, height, knowledgePoint));
            updateById(asset);
            log.warn("Image asset storage failed: assetId={}", asset.getAssetId());
        }
        return converter.toVO(asset);
    }

    private String imageProviderName() {
        return "hidream".equalsIgnoreCase(xfyunProperties.getImage().getProvider())
                ? PROVIDER_XFYUN_HIDREAM : PROVIDER_XFYUN_TTI;
    }

    /**
     * 创建 pending 资产记录。
     */
    private ResourceAsset createPendingAsset(Long userId, String packageId, String taskId, Long materialId,
                                             String assetType, String assetRole, String provider,
                                             String model, String promptSummary, String sourceKeys) {
        ResourceAsset asset = new ResourceAsset();
        asset.setAssetId("asset_" + UUID.randomUUID().toString().replace("-", ""));
        asset.setPackageId(packageId);
        asset.setTaskId(taskId);
        asset.setUserId(userId);
        asset.setMaterialId(materialId);
        asset.setAssetType(assetType);
        asset.setAssetRole(assetRole);
        asset.setStatus(STATUS_PENDING);
        asset.setProvider(provider);
        asset.setModel(model);
        asset.setPromptSummary(promptSummary);
        asset.setSourceKeys(sourceKeys);
        asset.setCreatedAt(LocalDateTime.now());
        asset.setUpdatedAt(LocalDateTime.now());
        save(asset);
        return asset;
    }

    /**
     * 查询指定资源包下的资产列表，仅返回当前用户拥有的资产。
     */
    public List<ResourceAssetVO> listByPackageId(String packageId, Long userId) {
        if (packageId == null || packageId.isBlank() || userId == null) {
            return List.of();
        }
        LambdaQueryWrapper<ResourceAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResourceAsset::getPackageId, packageId)
                .eq(ResourceAsset::getUserId, userId)
                .orderByAsc(ResourceAsset::getCreatedAt);
        List<ResourceAsset> entities = list(wrapper);
        return entities.stream().map(converter::toVO).toList();
    }

    /**
     * 根据 assetId 查询资产实体，校验 userId 归属。
     *
     * @param assetId 资产 UUID
     * @param userId  当前用户 ID
     * @return 资产实体
     * @throws BusinessException 资产不存在或不属于当前用户
     */
    public ResourceAsset getOwnedAsset(String assetId, Long userId) {
        if (assetId == null || assetId.isBlank()) {
            throw new BusinessException(400, "assetId 不能为空");
        }
        LambdaQueryWrapper<ResourceAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResourceAsset::getAssetId, assetId).eq(ResourceAsset::getUserId, userId);
        ResourceAsset asset = getOne(wrapper);
        if (asset == null) {
            throw new BusinessException(404, "资产不存在或无访问权限");
        }
        return asset;
    }

    /**
     * 标记资产文件缺失（下载接口发现磁盘文件不存在时调用）。
     */
    public void markFileMissing(ResourceAsset asset) {
        asset.setStatus(STATUS_FAILED);
        asset.setErrorCode("FILE_MISSING");
        asset.setErrorMessage("文件缺失");
        asset.setUpdatedAt(LocalDateTime.now());
        updateById(asset);
        log.warn("Asset file marked missing: assetId={}", asset.getAssetId());
    }

    /**
     * 重试失败资产。
     *
     * <p>规则：
     * <ul>
     *   <li>只允许重试 failed 状态资产；</li>
     *   <li>音频资产复用 metadata.originalText 重新合成；</li>
     *   <li>图片资产复用 metadata.originalPrompt 重新生成；</li>
     *   <li>重试成功后更新原资产记录，不新建资产；</li>
     *   <li>如果原始 prompt 缺失，返回 400。</li>
     * </ul>
     *
     * @param assetId 资产 UUID
     * @param userId  当前用户 ID
     * @return 更新后的资产 VO
     */
    public ResourceAssetVO retryAsset(String assetId, Long userId) {
        ResourceAsset asset = getOwnedAsset(assetId, userId);
        if (!STATUS_FAILED.equals(asset.getStatus())) {
            throw new BusinessException(400, "只允许重试 failed 状态的资产");
        }
        Map<String, Object> metadata = asset.getMetadata() != null ? asset.getMetadata() : new HashMap<>();

        if (ASSET_TYPE_AUDIO.equals(asset.getAssetType())) {
            String originalText = (String) metadata.get("originalText");
            if (originalText == null || originalText.isBlank()) {
                throw new BusinessException(400, "原始脚本缺失，无法重试");
            }
            String voice = (String) metadata.getOrDefault("voice", xfyunProperties.getTts().getVoice());
            String podcastStyle = (String) metadata.getOrDefault("podcastStyle", "teacher");
            return retryPodcastAssetInPlace(asset, originalText, voice, podcastStyle);
        }

        if (ASSET_TYPE_IMAGE.equals(asset.getAssetType())) {
            String originalPrompt = (String) metadata.get("originalPrompt");
            if (originalPrompt == null || originalPrompt.isBlank()) {
                throw new BusinessException(400, "原始提示词缺失，无法重试");
            }
            String negativePrompt = (String) metadata.get("negativePrompt");
            Integer width = (Integer) metadata.get("width");
            Integer height = (Integer) metadata.get("height");
            String imageStyle = (String) metadata.getOrDefault("imageStyle", "clean_edu");

            // 删除原资产（重试不新建资产，但 generateSingleImage 会新建；这里改为先删除原资产再生成）
            // 简化处理：先删除原资产，再调用单图生成流程，由调用方决定是否更新原 ID
            // 为保持"重试成功后更新原资产记录"的语义，这里改为：仅更新原资产的状态字段
            return retryImageAssetInPlace(asset, originalPrompt, negativePrompt, imageStyle, width, height);
        }

        throw new BusinessException(400, "不支持的资产类型: " + asset.getAssetType());
    }

    /**
     * 原地重试图片资产（更新原资产记录，不新建）。
     */
    private ResourceAssetVO retryImageAssetInPlace(ResourceAsset asset, String prompt, String negativePrompt,
                                                   String imageStyle, Integer width, Integer height) {
        asset.setStatus(STATUS_GENERATING);
        asset.setErrorCode(null);
        asset.setErrorMessage(null);
        asset.setUpdatedAt(LocalDateTime.now());
        updateById(asset);

        ImageGenerationRequest request = new ImageGenerationRequest();
        request.setUserId(asset.getUserId());
        request.setMaterialId(asset.getMaterialId());
        request.setPackageId(asset.getPackageId());
        request.setTaskId(asset.getTaskId());
        request.setPromptSummary(asset.getPromptSummary());
        request.setPrompt(prompt);
        request.setNegativePrompt(negativePrompt);
        request.setImageStyle(imageStyle);
        request.setWidth(width);
        request.setHeight(height);
        request.setAssetRole(asset.getAssetRole());
        request.setKnowledgePoint((String) metadataValue(asset.getMetadata(), "knowledgePoint"));

        List<AssetGenerationResult> results;
        try {
            results = imageProvider.generate(request);
        } catch (Exception e) {
            results = List.of(AssetGenerationResult.failure(imageProviderName(), ProviderErrorType.UNKNOWN,
                    "PROVIDER_ERROR", "文生图调用异常"));
        }

        if (results == null || results.isEmpty() || !results.get(0).isSuccess()) {
            AssetGenerationResult result = (results == null || results.isEmpty()) ? null : results.get(0);
            asset.setStatus(STATUS_FAILED);
            asset.setErrorCode(result != null ? result.getErrorCode() : "EMPTY_RESULT");
            asset.setErrorMessage(result != null ? result.getErrorMessage() : "图片生成结果为空");
            asset.setMetadata(buildImageMetadata(result != null ? result.getMetadata() : asset.getMetadata(),
                    prompt, negativePrompt, imageStyle, width, height,
                    (String) metadataValue(asset.getMetadata(), "knowledgePoint")));
            asset.setUpdatedAt(LocalDateTime.now());
            updateById(asset);
            return converter.toVO(asset);
        }

        AssetGenerationResult result = results.get(0);
        try {
            String storagePath = storageService.storeImage(result.getContent(), result.getContentType(),
                    result.getFileExtension());
            asset.setStatus(STATUS_SUCCESS);
            asset.setFileName(buildFileName(result.getFileExtension()));
            asset.setContentType(result.getContentType());
            asset.setStoragePath(storagePath);
            asset.setSizeBytes((long) result.getContent().length);
            if (result.getWidth() != null) asset.setWidth(result.getWidth());
            if (result.getHeight() != null) asset.setHeight(result.getHeight());
            asset.setMetadata(buildImageMetadata(result.getMetadata(), prompt, negativePrompt,
                    imageStyle, width, height,
                    (String) metadataValue(asset.getMetadata(), "knowledgePoint")));
            asset.setUpdatedAt(LocalDateTime.now());
            updateById(asset);
            clearAssetError(asset);
        } catch (BusinessException e) {
            asset.setStatus(STATUS_FAILED);
            asset.setErrorCode("STORAGE_ERROR");
            asset.setErrorMessage(e.getMessage());
            asset.setMetadata(buildImageMetadata(result.getMetadata(), prompt, negativePrompt,
                    imageStyle, width, height,
                    (String) metadataValue(asset.getMetadata(), "knowledgePoint")));
            asset.setUpdatedAt(LocalDateTime.now());
            updateById(asset);
        }
        return converter.toVO(asset);
    }

    /**
     * 原地重试音频资产，保持 assetId 不变，保证前端按原卡片更新。
     */
    private ResourceAssetVO retryPodcastAssetInPlace(ResourceAsset asset, String originalText,
                                                     String voice, String podcastStyle) {
        asset.setStatus(STATUS_GENERATING);
        asset.setErrorCode(null);
        asset.setErrorMessage(null);
        asset.setUpdatedAt(LocalDateTime.now());
        updateById(asset);

        AudioSynthesisRequest request = new AudioSynthesisRequest();
        request.setUserId(asset.getUserId());
        request.setMaterialId(asset.getMaterialId());
        request.setPackageId(asset.getPackageId());
        request.setTaskId(asset.getTaskId());
        request.setPromptSummary(asset.getPromptSummary());
        request.setText(originalText);
        request.setVoice(voice);
        request.setPodcastStyle(podcastStyle);
        request.setSourceKeys(asset.getSourceKeys());

        AssetGenerationResult result;
        try {
            result = speechProvider.synthesize(request);
        } catch (Exception e) {
            result = AssetGenerationResult.failure(PROVIDER_XFYUN_TTS, ProviderErrorType.UNKNOWN,
                    "PROVIDER_ERROR", "语音合成调用异常");
        }

        if (!result.isSuccess()) {
            asset.setStatus(STATUS_FAILED);
            asset.setErrorCode(result.getErrorCode());
            asset.setErrorMessage(result.getErrorMessage());
            asset.setUpdatedAt(LocalDateTime.now());
            updateById(asset);
            return converter.toVO(asset);
        }

        try {
            String storagePath = storageService.storeAudio(result.getContent(), result.getContentType());
            asset.setStatus(STATUS_SUCCESS);
            asset.setFileName(buildFileName(result.getFileExtension()));
            asset.setContentType(result.getContentType());
            asset.setStoragePath(storagePath);
            asset.setSizeBytes((long) result.getContent().length);
            asset.setModel(result.getModel());
            asset.setMetadata(buildAudioMetadata(result.getMetadata(), originalText, voice, podcastStyle));
            asset.setUpdatedAt(LocalDateTime.now());
            updateById(asset);
            clearAssetError(asset);
        } catch (BusinessException e) {
            asset.setStatus(STATUS_FAILED);
            asset.setErrorCode("STORAGE_ERROR");
            asset.setErrorMessage(e.getMessage());
            asset.setUpdatedAt(LocalDateTime.now());
            updateById(asset);
        }
        return converter.toVO(asset);
    }

    /**
     * 获取资源资产能力视图。
     */
    public ResourceAssetCapabilitiesVO getCapabilities() {
        ResourceAssetCapabilitiesVO vo = new ResourceAssetCapabilitiesVO();
        vo.setTtsEnabled(xfyunProperties.isTtsConfigured());
        vo.setImageEnabled(xfyunProperties.isImageConfigured());
        vo.setVoices(SUPPORTED_VOICES);
        vo.setImageStyles(SUPPORTED_IMAGE_STYLES);
        vo.setMaxImageCount(xfyunProperties.getImage().getMaxImageCount());
        vo.setDefaultVoice(xfyunProperties.getTts().getVoice());
        vo.setDefaultImageStyle("clean_edu");
        return vo;
    }

    /**
     * 构造文件名（UUID 命名）。
     */
    private String buildFileName(String extension) {
        String safeExt = (extension == null || extension.isBlank()) ? "bin"
                : extension.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        return UUID.randomUUID().toString().replace("-", "") + "." + safeExt;
    }

    /**
     * 提取提示词摘要（脱敏后写入资产元数据）。
     */
    private String summarizePrompt(String prompt) {
        if (prompt == null) {
            return null;
        }
        // 截断至 300 字符，避免占用过大数据库存储
        return prompt.length() > 300 ? prompt.substring(0, 300) + "..." : prompt;
    }

    private Object metadataValue(Map<String, Object> metadata, String key) {
        return metadata != null ? metadata.get(key) : null;
    }

    private void clearAssetError(ResourceAsset asset) {
        if (asset == null || asset.getId() == null) {
            return;
        }
        lambdaUpdate()
                .eq(ResourceAsset::getId, asset.getId())
                .set(ResourceAsset::getErrorCode, null)
                .set(ResourceAsset::getErrorMessage, null)
                .update();
        asset.setErrorCode(null);
        asset.setErrorMessage(null);
    }

    /**
     * 构造音频资产元数据，保存原始脚本以便重试。
     */
    private Map<String, Object> buildAudioMetadata(Map<String, Object> providerMetadata,
                                                   String originalText, String voice, String podcastStyle) {
        Map<String, Object> metadata = new HashMap<>();
        if (providerMetadata != null) {
            metadata.putAll(providerMetadata);
        }
        metadata.put("originalText", originalText);
        metadata.put("voice", voice != null ? voice : xfyunProperties.getTts().getVoice());
        metadata.put("podcastStyle", podcastStyle != null ? podcastStyle : "teacher");
        return metadata;
    }

    /**
     * 构造图片资产元数据，保存原始提示词以便重试。
     */
    private Map<String, Object> buildImageMetadata(Map<String, Object> providerMetadata,
                                                   String originalPrompt, String negativePrompt,
                                                   String imageStyle, Integer width, Integer height,
                                                   String knowledgePoint) {
        Map<String, Object> metadata = new HashMap<>();
        if (providerMetadata != null) {
            metadata.putAll(providerMetadata);
        }
        metadata.put("originalPrompt", originalPrompt);
        if (negativePrompt != null) metadata.put("negativePrompt", negativePrompt);
        metadata.put("imageStyle", imageStyle != null ? imageStyle : "clean_edu");
        if (width != null) metadata.put("width", width);
        if (height != null) metadata.put("height", height);
        if (knowledgePoint != null) metadata.put("knowledgePoint", knowledgePoint);
        return metadata;
    }
}
