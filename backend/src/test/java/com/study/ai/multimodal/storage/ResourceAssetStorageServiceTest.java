package com.study.ai.multimodal.storage;

import com.study.common.BusinessException;
import com.study.config.ResourceAssetProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 资源资产文件存储服务测试（spec §13.1）。
 *
 * <p>覆盖点：路径穿越防护、大小限制、文件头校验。
 *
 * <p>实现说明：使用 {@link TempDir} 提供隔离的临时存储根目录，手动调用
 * {@link ResourceAssetStorageService#init()} 触发 @PostConstruct 逻辑。
 */
class ResourceAssetStorageServiceTest {

    @TempDir
    Path tempDir;

    private ResourceAssetProperties properties;
    private ResourceAssetStorageService storageService;

    @BeforeEach
    void setUp() {
        properties = new ResourceAssetProperties();
        properties.setStorageDir(tempDir.toString());
        properties.setMaxAudioBytes(1024);    // 1KB 限制便于测试
        properties.setMaxImageBytes(2048);    // 2KB
        storageService = new ResourceAssetStorageService(properties);
        // 手动触发 @PostConstruct
        storageService.init();
    }

    // ============== 音频存储 ==============

    @Test
    void storeAudio_ValidMp3WithId3Header_ReturnsRelativePath() {
        // ID3v2 文件头
        byte[] mp3Bytes = new byte[]{
                0x49, 0x44, 0x33, 0x10, 0x20, 0x30, 0x40, 0x50
        };

        String storagePath = storageService.storeAudio(mp3Bytes, "audio/mpeg");

        assertNotNull(storagePath);
        assertTrue(storagePath.endsWith(".mp3"));
        // 文件应实际写入到 tempDir
        assertTrue(Files.exists(tempDir.resolve(storagePath)));
    }

    @Test
    void storeAudio_ValidMp3WithFrameSyncHeader_ReturnsRelativePath() {
        // MP3 frame sync: 0xFF 0xE0
        byte[] mp3Bytes = new byte[]{
                (byte) 0xFF, (byte) 0xE0, 0x10, 0x20, 0x30, 0x40
        };

        String storagePath = storageService.storeAudio(mp3Bytes, "audio/mpeg");

        assertNotNull(storagePath);
        assertTrue(storagePath.endsWith(".mp3"));
    }

    @Test
    void storeAudio_EmptyContent_ThrowsException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storageService.storeAudio(new byte[0], "audio/mpeg"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("空"));
    }

    @Test
    void storeAudio_ExceedsMaxSize_ThrowsException() {
        // 构造超过 maxAudioBytes (1024) 的字节数组
        byte[] largeBytes = new byte[1025];
        largeBytes[0] = 0x49;
        largeBytes[1] = 0x44;
        largeBytes[2] = 0x33;

        BusinessException ex = assertThrows(BusinessException.class,
                () -> storageService.storeAudio(largeBytes, "audio/mpeg"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("大小限制"));
    }

    @Test
    void storeAudio_InvalidContentType_ThrowsException() {
        byte[] mp3Bytes = new byte[]{0x49, 0x44, 0x33, 0x10};

        BusinessException ex = assertThrows(BusinessException.class,
                () -> storageService.storeAudio(mp3Bytes, "audio/wav"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("Content-Type"));
    }

    @Test
    void storeAudio_NullContentType_ThrowsException() {
        byte[] mp3Bytes = new byte[]{0x49, 0x44, 0x33, 0x10};

        BusinessException ex = assertThrows(BusinessException.class,
                () -> storageService.storeAudio(mp3Bytes, null));
        assertEquals(400, ex.getCode());
    }

    @Test
    void storeAudio_InvalidHeader_ThrowsException() {
        // 不带 ID3 也不带 frame sync
        byte[] invalidBytes = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05};

        BusinessException ex = assertThrows(BusinessException.class,
                () -> storageService.storeAudio(invalidBytes, "audio/mpeg"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("文件头"));
    }

    @Test
    void storeAudio_TooShortBytes_ThrowsException() {
        byte[] shortBytes = new byte[]{0x01, 0x02};

        BusinessException ex = assertThrows(BusinessException.class,
                () -> storageService.storeAudio(shortBytes, "audio/mpeg"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("过短"));
    }

    // ============== 图片存储 ==============

    @Test
    void storeImage_ValidPng_ReturnsRelativePath() {
        byte[] pngBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };

        String storagePath = storageService.storeImage(pngBytes, "image/png", "png");

        assertNotNull(storagePath);
        assertTrue(storagePath.endsWith(".png"));
        assertTrue(Files.exists(tempDir.resolve(storagePath)));
    }

    @Test
    void storeImage_ValidJpeg_ReturnsRelativePath() {
        byte[] jpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x10, 0x20, 0x30, 0x40
        };

        String storagePath = storageService.storeImage(jpegBytes, "image/jpeg", "jpg");

        assertNotNull(storagePath);
        assertTrue(storagePath.endsWith(".jpg"));
    }

    @Test
    void storeImage_ValidWebp_ReturnsRelativePath() {
        // WebP: 52 49 46 46 ... 57 45 42 50
        byte[] webpBytes = new byte[12];
        webpBytes[0] = 0x52; webpBytes[1] = 0x49; webpBytes[2] = 0x46; webpBytes[3] = 0x46;
        webpBytes[4] = 0x10; webpBytes[5] = 0x20; webpBytes[6] = 0x30; webpBytes[7] = 0x40;
        webpBytes[8] = 0x57; webpBytes[9] = 0x45; webpBytes[10] = 0x42; webpBytes[11] = 0x50;

        String storagePath = storageService.storeImage(webpBytes, "image/webp", "webp");

        assertNotNull(storagePath);
        assertTrue(storagePath.endsWith(".webp"));
    }

    @Test
    void storeImage_ExceedsMaxSize_ThrowsException() {
        // maxImageBytes = 2048
        byte[] largeBytes = new byte[2049];
        largeBytes[0] = (byte) 0x89;
        largeBytes[1] = 0x50;
        largeBytes[2] = 0x4E;
        largeBytes[3] = 0x47;

        BusinessException ex = assertThrows(BusinessException.class,
                () -> storageService.storeImage(largeBytes, "image/png", "png"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("大小限制"));
    }

    @Test
    void storeImage_InvalidContentType_ThrowsException() {
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};

        BusinessException ex = assertThrows(BusinessException.class,
                () -> storageService.storeImage(pngBytes, "image/gif", "gif"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("Content-Type"));
    }

    @Test
    void storeImage_InvalidHeader_ThrowsException() {
        // 不带 PNG/JPEG/WebP 头
        byte[] invalidBytes = new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06};

        BusinessException ex = assertThrows(BusinessException.class,
                () -> storageService.storeImage(invalidBytes, "image/png", "png"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("文件头"));
    }

    @Test
    void storeImage_InvalidExtension_SanitizedToContentType() {
        byte[] pngBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };

        // 传入非法扩展名，应被规整为 png
        String storagePath = storageService.storeImage(pngBytes, "image/png", "../../etc/passwd");
        assertNotNull(storagePath);
        assertTrue(storagePath.endsWith(".png"), "非法扩展名应被清洗为 png");
    }

    @Test
    void storeImage_NullExtension_FallsBackToContentType() {
        byte[] jpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x10, 0x20
        };

        String storagePath = storageService.storeImage(jpegBytes, "image/jpeg", null);
        assertNotNull(storagePath);
        assertTrue(storagePath.endsWith(".jpg"), "null 扩展名应回退为 jpg");
    }

    // ============== 路径穿越防护 ==============

    @Test
    void load_PathWithDoubleDot_ThrowsPathTraversal() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storageService.load("../../etc/passwd"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("非法"));
    }

    @Test
    void load_AbsolutePathOutsideRoot_ThrowsPathTraversal() {
        // 构造绝对路径尝试越权
        String outsidePath = tempDir.getParent().resolve("../../../etc/passwd").toString();
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storageService.load(outsidePath));
        // 即使 normalize 后还在 root 外，也应被拦截
        // 注意：路径中含 ".." 会先被字符串检查拦截
        assertTrue(ex.getCode() == 400 || ex.getCode() == 404);
    }

    @Test
    void load_NullPath_ThrowsNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storageService.load(null));
        assertEquals(404, ex.getCode());
    }

    @Test
    void load_BlankPath_ThrowsNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storageService.load("   "));
        assertEquals(404, ex.getCode());
    }

    @Test
    void load_NonExistentFile_ThrowsNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> storageService.load("nonexistent.mp3"));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("不存在"));
    }

    @Test
    void load_ValidFile_ReturnsContent() throws Exception {
        byte[] pngBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };

        String storagePath = storageService.storeImage(pngBytes, "image/png", "png");
        byte[] loaded = storageService.load(storagePath);

        assertArrayEquals(pngBytes, loaded);
    }

    // ============== exists ==============

    @Test
    void exists_NullPath_ReturnsFalse() {
        assertFalse(storageService.exists(null));
    }

    @Test
    void exists_BlankPath_ReturnsFalse() {
        assertFalse(storageService.exists("   "));
    }

    @Test
    void exists_PathWithDoubleDot_ReturnsFalse() {
        assertFalse(storageService.exists("../../etc/passwd"));
    }

    @Test
    void exists_NonExistentFile_ReturnsFalse() {
        assertFalse(storageService.exists("nonexistent.png"));
    }

    @Test
    void exists_ExistingFile_ReturnsTrue() {
        byte[] pngBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
        };
        String storagePath = storageService.storeImage(pngBytes, "image/png", "png");
        assertTrue(storageService.exists(storagePath));
    }

    // ============== storageRoot 初始化 ==============

    @Test
    void init_BlankStorageDir_ThrowsIllegalState() {
        ResourceAssetProperties badProps = new ResourceAssetProperties();
        badProps.setStorageDir("");
        ResourceAssetStorageService badService = new ResourceAssetStorageService(badProps);

        assertThrows(IllegalStateException.class, badService::init);
    }

    @Test
    void init_NonExistentDir_CreatesDirectory() {
        Path newDir = tempDir.resolve("new-storage");
        ResourceAssetProperties newProps = new ResourceAssetProperties();
        newProps.setStorageDir(newDir.toString());
        ResourceAssetStorageService newService = new ResourceAssetStorageService(newProps);

        newService.init();

        assertTrue(Files.exists(newDir) && Files.isDirectory(newDir));
    }
}
