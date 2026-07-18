package com.study.ai.multimodal.storage;

import com.study.common.BusinessException;
import com.study.config.ResourceAssetProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * 资源资产文件存储服务。
 *
 * <p>负责将音频/图片字节持久化到本地存储目录，并提供读取接口。
 * 安全约束：
 * <ul>
 *   <li>文件名使用 UUID，不使用用户输入。</li>
 *   <li>{@code storagePath} 仅保存相对路径，禁止暴露绝对路径。</li>
 *   <li>写入前校验大小、文件头和内容类型白名单。</li>
 *   <li>读取时校验路径必须位于存储根目录下，防穿越。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceAssetStorageService {

    /** 音频允许的 Content-Type */
    private static final String AUDIO_CONTENT_TYPE = "audio/mpeg";

    /** 图片允许的 Content-Type 集合 */
    private static final java.util.Set<String> IMAGE_CONTENT_TYPES =
            java.util.Set.of("image/png", "image/jpeg", "image/webp");

    /** 图片允许的扩展名白名单（spec §11.3：图片只允许 PNG/JPEG/WebP） */
    private static final java.util.Set<String> ALLOWED_IMAGE_EXTENSIONS =
            java.util.Set.of("png", "jpg", "jpeg", "webp");

    private final ResourceAssetProperties properties;

    private Path storageRoot;

    @PostConstruct
    void init() {
        String dir = properties.getStorageDir();
        if (dir == null || dir.isBlank()) {
            throw new IllegalStateException("resource-asset.storage-dir 未配置");
        }
        storageRoot = Paths.get(dir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
            log.info("ResourceAsset storage root initialized: {}", storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("初始化资源资产存储目录失败: " + dir, e);
        }
    }

    /**
     * 保存音频字节，返回相对存储路径。
     *
     * @param content      MP3 字节
     * @param contentType  Content-Type，必须为 audio/mpeg
     * @return 相对存储路径，如 {uuid}.mp3
     */
    public String storeAudio(byte[] content, String contentType) {
        validateContentSize(content, properties.getMaxAudioBytes(), "audio");
        validateContentType(contentType, AUDIO_CONTENT_TYPE, "audio");
        validateAudioHeader(content);
        String fileName = UUID.randomUUID().toString().replace("-", "") + ".mp3";
        return writeInternal(content, fileName);
    }

    /**
     * 保存图片字节，返回相对存储路径。
     *
     * @param content      图片字节
     * @param contentType  Content-Type，必须为 image/png / image/jpeg / image/webp
     * @param fileExtension 文件扩展名（不含点）
     * @return 相对存储路径，如 {uuid}.png
     */
    public String storeImage(byte[] content, String contentType, String fileExtension) {
        validateContentSize(content, properties.getMaxImageBytes(), "image");
        validateContentType(contentType, IMAGE_CONTENT_TYPES, "image");
        validateImageHeader(content);
        String safeExt = sanitizeExtension(fileExtension, contentType);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + safeExt;
        return writeInternal(content, fileName);
    }

    /**
     * 读取资产文件字节。路径穿越时抛出 BusinessException。
     *
     * @param storagePath 相对存储路径
     * @return 文件字节
     */
    public byte[] load(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            throw new BusinessException(404, "资产存储路径为空");
        }
        Path resolved = resolveAndValidate(storagePath);
        if (!Files.exists(resolved)) {
            throw new BusinessException(404, "资产文件不存在");
        }
        try {
            return Files.readAllBytes(resolved);
        } catch (IOException e) {
            log.warn("读取资产文件失败: path={}", storagePath);
            throw new BusinessException(500, "读取资产文件失败");
        }
    }

    /**
     * 判断资产文件是否存在。
     */
    public boolean exists(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return false;
        }
        try {
            Path resolved = resolveAndValidate(storagePath);
            return Files.exists(resolved);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析相对路径并校验是否位于存储根目录下。
     */
    private Path resolveAndValidate(String storagePath) {
        String normalized = storagePath.replace("\\", "/");
        if (normalized.contains("..")) {
            throw new BusinessException(400, "非法存储路径");
        }
        Path resolved = storageRoot.resolve(normalized).normalize();
        if (!resolved.startsWith(storageRoot)) {
            throw new BusinessException(400, "非法存储路径");
        }
        return resolved;
    }

    /**
     * 写入文件，返回相对路径。
     */
    private String writeInternal(byte[] content, String fileName) {
        try {
            Path target = storageRoot.resolve(fileName).normalize();
            if (!target.startsWith(storageRoot)) {
                throw new BusinessException(400, "非法存储路径");
            }
            Files.write(target, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return fileName;
        } catch (IOException e) {
            log.warn("写入资产文件失败: fileName={}", fileName);
            throw new BusinessException(500, "写入资产文件失败");
        }
    }

    /**
     * 校验文件大小。
     */
    private void validateContentSize(byte[] content, long maxSize, String type) {
        if (content == null || content.length == 0) {
            throw new BusinessException(400, "资产内容为空");
        }
        if (content.length > maxSize) {
            throw new BusinessException(400, type + " 资产超出大小限制");
        }
    }

    /**
     * 校验 Content-Type。
     */
    private void validateContentType(String contentType, String allowed, String type) {
        if (!allowed.equals(contentType)) {
            throw new BusinessException(400, type + " 资产 Content-Type 非法");
        }
    }

    private void validateContentType(String contentType, java.util.Set<String> allowed, String type) {
        if (contentType == null || !allowed.contains(contentType)) {
            throw new BusinessException(400, type + " 资产 Content-Type 非法");
        }
    }

    /**
     * 校验 MP3 文件头（ID3 或 frame sync）。
     */
    private void validateAudioHeader(byte[] bytes) {
        if (bytes.length < 3) {
            throw new BusinessException(400, "音频字节过短");
        }
        // ID3v2: "ID3"
        boolean id3 = (bytes[0] & 0xFF) == 0x49 && (bytes[1] & 0xFF) == 0x44 && (bytes[2] & 0xFF) == 0x33;
        // MP3 frame sync: 0xFFEx 或 0xFFFx
        boolean frameSync = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xE0) == 0xE0;
        if (!id3 && !frameSync) {
            throw new BusinessException(400, "MP3 文件头校验失败");
        }
    }

    /**
     * 校验图片文件头（PNG/JPEG/WebP）。
     */
    private void validateImageHeader(byte[] bytes) {
        if (bytes.length < 4) {
            throw new BusinessException(400, "图片字节过短");
        }
        boolean png = (bytes[0] & 0xFF) == 0x89 && (bytes[1] & 0xFF) == 0x50
                && (bytes[2] & 0xFF) == 0x4E && (bytes[3] & 0xFF) == 0x47;
        boolean jpeg = (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
        boolean webp = bytes.length >= 12
                && (bytes[0] & 0xFF) == 0x52 && (bytes[1] & 0xFF) == 0x49
                && (bytes[2] & 0xFF) == 0x46 && (bytes[3] & 0xFF) == 0x46
                && (bytes[8] & 0xFF) == 0x57 && (bytes[9] & 0xFF) == 0x45
                && (bytes[10] & 0xFF) == 0x42 && (bytes[11] & 0xFF) == 0x50;
        if (!png && !jpeg && !webp) {
            throw new BusinessException(400, "图片文件头校验失败");
        }
    }

    /**
     * 根据内容类型规整扩展名。spec §11.3：图片只允许 PNG/JPEG/WebP。
     * 非法扩展名或不在白名单中的扩展名回退到 Content-Type。
     */
    private String sanitizeExtension(String extension, String contentType) {
        if (extension != null && !extension.isBlank()) {
            String safe = extension.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (ALLOWED_IMAGE_EXTENSIONS.contains(safe)) {
                return safe;
            }
        }
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> "png";
        };
    }
}
