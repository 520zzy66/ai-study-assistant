package com.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.study.ai.rag.MaterialVectorIndexService;
import com.study.ai.rag.TemporaryMaterialVectorIndexService;
import com.study.common.BusinessException;
import com.study.common.Constants;
import com.study.common.UserContext;
import com.study.dto.request.TemporaryMaterialPromoteRequest;
import com.study.entity.LearningMaterial;
import com.study.entity.MaterialChunk;
import com.study.entity.MaterialFolder;
import com.study.entity.TemporaryMaterial;
import com.study.entity.TemporaryMaterialChunk;
import com.study.mapper.LearningMaterialMapper;
import com.study.mapper.MaterialChunkMapper;
import com.study.mapper.TemporaryMaterialChunkMapper;
import com.study.mapper.TemporaryMaterialMapper;
import com.study.service.TemporaryMaterialService;
import com.study.vo.TemporaryMaterialVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 会话临时资料服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemporaryMaterialServiceImpl implements TemporaryMaterialService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "pdf", "doc", "docx", "txt", "md", "png", "jpg", "jpeg", "webp");
    private static final int TTL_DAYS = 7;
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;

    private final TemporaryMaterialMapper temporaryMaterialMapper;
    private final TemporaryMaterialChunkMapper temporaryChunkMapper;
    private final TemporaryMaterialAsyncProcessor asyncProcessor;
    private final TemporaryMaterialVectorIndexService temporaryVectorIndexService;
    private final LearningMaterialMapper materialMapper;
    private final MaterialChunkMapper materialChunkMapper;
    private final MaterialVectorIndexService materialVectorIndexService;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.max-size:52428800}")
    private long maxSize;

    @Override
    public TemporaryMaterialVO upload(MultipartFile file, String conversationId) {
        validateUpload(file, conversationId);
        Long userId = UserContext.getCurrentUserId();
        String originalName = file.getOriginalFilename();
        String fileType = extension(originalName);
        String token = UUID.randomUUID().toString().replace("-", "");
        String storedName = token + "." + fileType;
        String relativePath = "temporary/" + userId + "/"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "/" + storedName;
        Path fullPath = safePath(relativePath);

        try {
            Files.createDirectories(fullPath.getParent());
            file.transferTo(fullPath);
        } catch (IOException e) {
            throw new BusinessException(500, "临时资料保存失败");
        }

        TemporaryMaterial material = new TemporaryMaterial();
        material.setUploadToken(token);
        material.setUserId(userId);
        material.setConversationId(conversationId);
        material.setOriginalName(originalName);
        material.setStoredName(storedName);
        material.setFileType(fileType);
        material.setFileSize(file.getSize());
        material.setFilePath(relativePath);
        material.setStatus("processing");
        material.setChunkCount(0);
        material.setExpiresAt(LocalDateTime.now().plusDays(TTL_DAYS));
        material.setDeleted(0);
        try {
            temporaryMaterialMapper.insert(material);
        } catch (Exception e) {
            deleteFile(fullPath);
            throw e;
        }
        asyncProcessor.process(material.getId());
        return toVO(material);
    }

    @Override
    public List<TemporaryMaterialVO> list() {
        Long userId = UserContext.getCurrentUserId();
        return temporaryMaterialMapper.selectList(new LambdaQueryWrapper<TemporaryMaterial>()
                        .eq(TemporaryMaterial::getUserId, userId)
                        .in(TemporaryMaterial::getStatus,
                                "processing", "indexing", "ready", "failed", "converting", "deleting")
                        .gt(TemporaryMaterial::getExpiresAt, LocalDateTime.now())
                        .orderByDesc(TemporaryMaterial::getCreateTime))
                .stream().map(this::toVO).toList();
    }

    @Override
    public TemporaryMaterialVO getDetail(String uploadToken) {
        return toVO(requireOwned(uploadToken));
    }

    @Override
    public void delete(String uploadToken) {
        TemporaryMaterial material = requireOwned(uploadToken);
        if ("indexing".equals(material.getStatus()) || "converting".equals(material.getStatus())) {
            throw new BusinessException(409, "资料正在完成索引或转存，请稍后再试");
        }
        if (!"deleting".equals(material.getStatus())) {
            int claimed = temporaryMaterialMapper.update(null,
                    new LambdaUpdateWrapper<TemporaryMaterial>()
                            .eq(TemporaryMaterial::getId, material.getId())
                            .eq(TemporaryMaterial::getStatus, material.getStatus())
                            .set(TemporaryMaterial::getStatus, "deleting"));
            if (claimed == 0) {
                throw new BusinessException(409, "资料状态已变化，请刷新后重试");
            }
        }
        cleanupTemporaryContent(material, true);
    }

    @Override
    @Transactional
    public Long promote(String uploadToken, TemporaryMaterialPromoteRequest request) {
        TemporaryMaterial temporary = requireOwned(uploadToken);
        if ("converted".equals(temporary.getStatus()) && temporary.getConvertedMaterialId() != null) {
            return temporary.getConvertedMaterialId();
        }
        if (!"ready".equals(temporary.getStatus())) {
            throw new BusinessException(400, "仅处理完成的临时资料可以添加到我的资料");
        }
        int claimed = temporaryMaterialMapper.update(null,
                new LambdaUpdateWrapper<TemporaryMaterial>()
                        .eq(TemporaryMaterial::getId, temporary.getId())
                        .eq(TemporaryMaterial::getStatus, "ready")
                        .set(TemporaryMaterial::getStatus, "converting"));
        if (claimed == 0) {
            TemporaryMaterial latest = temporaryMaterialMapper.selectById(temporary.getId());
            if (latest != null && "converted".equals(latest.getStatus())
                    && latest.getConvertedMaterialId() != null) {
                return latest.getConvertedMaterialId();
            }
            throw new BusinessException(409, "临时资料状态已变化，请刷新后重试");
        }
        Long userId = UserContext.getCurrentUserId();
        validateFolder(userId, request.getFolderId());

        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + temporary.getFileType();
        String officialPath = userId + "/" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "/" + storedName;
        Path target = safePath(officialPath);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(safePath(temporary.getFilePath()), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(500, "临时资料文件转存失败");
        }

        List<MaterialChunk> officialChunks = new java.util.ArrayList<>();
        try {
            LearningMaterial material = new LearningMaterial();
            material.setUserId(userId);
            material.setOriginalName(temporary.getOriginalName());
            material.setStoredName(storedName);
            material.setFileType(temporary.getFileType());
            material.setFileSize(temporary.getFileSize());
            material.setFilePath(officialPath);
            material.setCategory(StringUtils.hasText(request.getCategory()) ? request.getCategory().trim() : null);
            material.setFolderId(request.getFolderId());
            material.setSummary(temporary.getSummary());
            material.setStatus(Constants.STATUS_READY);
            material.setChunkCount(temporary.getChunkCount());
            material.setSource(Constants.SOURCE_USER);
            material.setRetryCount(0);
            material.setDeleted(0);
            materialMapper.insert(material);

            List<TemporaryMaterialChunk> temporaryChunks = getChunks(temporary.getId());
            officialChunks = new java.util.ArrayList<>(temporaryChunks.size());
            for (TemporaryMaterialChunk source : temporaryChunks) {
                MaterialChunk chunk = new MaterialChunk();
                chunk.setMaterialId(material.getId());
                chunk.setUserId(userId);
                chunk.setFolderId(request.getFolderId());
                chunk.setChunkIndex(source.getChunkIndex());
                chunk.setContent(source.getContent());
                chunk.setChunkSize(source.getChunkSize());
                officialChunks.add(chunk);
            }
            Db.saveBatch(officialChunks, 100);
            officialChunks = materialChunkMapper.selectList(
                    new LambdaQueryWrapper<MaterialChunk>()
                            .eq(MaterialChunk::getMaterialId, material.getId())
                            .eq(MaterialChunk::getUserId, userId)
                            .orderByAsc(MaterialChunk::getChunkIndex));
            materialVectorIndexService.indexChunks(officialChunks, temporary.getFileType());

            temporary.setStatus("converted");
            temporary.setConvertedMaterialId(material.getId());
            temporaryMaterialMapper.updateById(temporary);
            cleanupTemporaryContent(temporary, false);
            return material.getId();
        } catch (Exception e) {
            if (!officialChunks.isEmpty()) {
                try {
                    materialVectorIndexService.deleteChunks(officialChunks);
                } catch (Exception cleanupError) {
                    log.warn("回滚正式资料向量失败: token={}", uploadToken, cleanupError);
                }
            }
            deleteFile(target);
            if (e instanceof DuplicateKeyException) {
                throw new BusinessException(400, "同名资料已存在于我的资料中");
            }
            throw e;
        }
    }

    @Override
    public TemporaryMaterial requireReady(Long userId, String conversationId, String uploadToken) {
        validateToken(uploadToken);
        TemporaryMaterial material = temporaryMaterialMapper.selectOne(
                new LambdaQueryWrapper<TemporaryMaterial>()
                        .eq(TemporaryMaterial::getUploadToken, uploadToken)
                        .eq(TemporaryMaterial::getUserId, userId)
                        .eq(TemporaryMaterial::getConversationId, conversationId));
        if (material == null || material.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException(404, "临时资料不存在或已过期");
        }
        if (!"ready".equals(material.getStatus())) {
            throw new BusinessException(400, "临时资料尚未处理完成");
        }
        return material;
    }

    @Override
    public List<TemporaryMaterialChunk> getChunks(Long temporaryMaterialId) {
        return temporaryChunkMapper.selectList(new LambdaQueryWrapper<TemporaryMaterialChunk>()
                .eq(TemporaryMaterialChunk::getTemporaryMaterialId, temporaryMaterialId)
                .orderByAsc(TemporaryMaterialChunk::getChunkIndex));
    }

    @Override
    public void saveSummary(Long temporaryMaterialId, String summary) {
        TemporaryMaterial update = new TemporaryMaterial();
        update.setId(temporaryMaterialId);
        update.setSummary(summary);
        temporaryMaterialMapper.updateById(update);
    }

    /** 每小时清理已过期临时资料。 */
    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void cleanupExpired() {
        List<TemporaryMaterial> expired = temporaryMaterialMapper.selectList(
                new LambdaQueryWrapper<TemporaryMaterial>()
                        .lt(TemporaryMaterial::getExpiresAt, LocalDateTime.now()));
        for (TemporaryMaterial material : expired) {
            try {
                if ("indexing".equals(material.getStatus()) || "converting".equals(material.getStatus())) {
                    continue;
                }
                int claimed = temporaryMaterialMapper.update(null,
                        new LambdaUpdateWrapper<TemporaryMaterial>()
                                .eq(TemporaryMaterial::getId, material.getId())
                                .eq(TemporaryMaterial::getStatus, material.getStatus())
                                .set(TemporaryMaterial::getStatus, "deleting"));
                if (claimed == 0) continue;
                cleanupTemporaryContent(material, true);
            } catch (Exception e) {
                log.warn("过期临时资料清理失败: token={}", material.getUploadToken(), e);
            }
        }
    }

    private void cleanupTemporaryContent(TemporaryMaterial material, boolean deleteRecord) {
        List<TemporaryMaterialChunk> chunks = getChunks(material.getId());
        temporaryVectorIndexService.deleteChunks(chunks);
        temporaryChunkMapper.delete(new LambdaQueryWrapper<TemporaryMaterialChunk>()
                .eq(TemporaryMaterialChunk::getTemporaryMaterialId, material.getId()));
        deleteFile(safePath(material.getFilePath()));
        if (deleteRecord) {
            temporaryMaterialMapper.deleteById(material.getId());
        }
    }

    private TemporaryMaterial requireOwned(String uploadToken) {
        validateToken(uploadToken);
        TemporaryMaterial material = temporaryMaterialMapper.selectOne(
                new LambdaQueryWrapper<TemporaryMaterial>()
                        .eq(TemporaryMaterial::getUploadToken, uploadToken)
                        .eq(TemporaryMaterial::getUserId, UserContext.getCurrentUserId()));
        if (material == null) {
            throw new BusinessException(404, "临时资料不存在");
        }
        return material;
    }

    private void validateToken(String uploadToken) {
        if (!StringUtils.hasText(uploadToken) || !uploadToken.matches("[a-fA-F0-9]{32}")) {
            throw new BusinessException(400, "临时资料令牌格式不正确");
        }
    }

    private void validateFolder(Long userId, Long folderId) {
        if (folderId == null) {
            return;
        }
        Long count = com.baomidou.mybatisplus.extension.toolkit.Db.lambdaQuery(MaterialFolder.class)
                .eq(MaterialFolder::getId, folderId)
                .eq(MaterialFolder::getUserId, userId)
                .count();
        if (count == 0) {
            throw new BusinessException(404, "目标文件夹不存在");
        }
    }

    private void validateUpload(MultipartFile file, String conversationId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }
        if (!StringUtils.hasText(conversationId) || conversationId.length() > 64) {
            throw new BusinessException(400, "会话ID不能为空且不能超过64个字符");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException(2003, "文件大小超过50MB限制");
        }
        String type = extension(file.getOriginalFilename());
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(2001, "仅支持 PDF、Word、TXT、Markdown 和常见图片格式");
        }
        if (Set.of("png", "jpg", "jpeg", "webp").contains(type)
                && file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(2003, "图片大小不能超过10MB");
        }
        validateMagicNumber(file, type);
    }

    private void validateMagicNumber(MultipartFile file, String type) {
        try (InputStream input = file.getInputStream()) {
            byte[] header = input.readNBytes(12);
            boolean valid = switch (type) {
                case "pdf" -> startsWith(header, new byte[]{0x25, 0x50, 0x44, 0x46});
                case "doc" -> startsWith(header, new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0});
                case "docx" -> startsWith(header, new byte[]{0x50, 0x4B, 0x03, 0x04});
                case "png" -> startsWith(header, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
                case "jpg", "jpeg" -> startsWith(header, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
                case "webp" -> header.length >= 12
                        && new String(header, 0, 4).equals("RIFF")
                        && new String(header, 8, 4).equals("WEBP");
                default -> true;
            };
            if (!valid) {
                throw new BusinessException(2002, "文件内容与扩展名不匹配");
            }
        } catch (IOException e) {
            throw new BusinessException(500, "文件校验失败");
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) return false;
        }
        return true;
    }

    private String extension(String name) {
        if (!StringUtils.hasText(name) || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }

    private Path safePath(String relativePath) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(400, "非法文件路径");
        }
        return target;
    }

    private void deleteFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("临时资料文件删除失败: {}", path, e);
        }
    }

    private TemporaryMaterialVO toVO(TemporaryMaterial material) {
        TemporaryMaterialVO vo = new TemporaryMaterialVO();
        BeanUtils.copyProperties(material, vo);
        return vo;
    }
}
