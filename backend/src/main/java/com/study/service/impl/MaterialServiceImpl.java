package com.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.study.common.BusinessException;
import com.study.common.UserContext;
import com.study.dto.request.MaterialListRequest;
import com.study.entity.LearningMaterial;
import com.study.entity.MaterialChunk;
import com.study.mapper.LearningMaterialMapper;
import com.study.mapper.MaterialChunkMapper;
import com.study.service.MaterialService;
import com.study.vo.MaterialUploadVO;
import com.study.vo.MaterialVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 学习资料服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final LearningMaterialMapper materialMapper;
    private final MaterialChunkMapper chunkMapper;
    private final MaterialAsyncProcessor asyncProcessor;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${file.allowed-types:pdf,doc,docx,txt}")
    private String allowedTypes;

    @Value("${file.max-size:52428800}")
    private long maxSize;

    /** 允许的文件扩展名集合（从配置初始化） */
    private Set<String> allowedExtensionSet;

    /** Magic Number 校验映射（十六进制，与 bytesToHex 输出一致） */
    private static final Map<String, String> MAGIC_NUMBERS = Map.of(
            "pdf",  "25504446",    // %PDF
            "doc",  "D0CF11E0",    // OLE2
            "docx", "504B0304"     // ZIP
            // txt 无固定 magic number
    );

    @PostConstruct
    public void init() {
        allowedExtensionSet = Set.of(allowedTypes.split(","));
    }

    @Override
    public MaterialUploadVO upload(MultipartFile file, String category) {
        // 1. 基础校验
        if (file == null || file.isEmpty()) {
            throw new BusinessException(2004, "文件为空");
        }

        if (file.getSize() > maxSize) {
            throw new BusinessException(2003, "文件大小超过限制（50MB）");
        }

        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName)) {
            throw new BusinessException(2004, "文件名不能为空");
        }

        // 2. 文件类型校验（扩展名 + Magic Number）
        String ext = getFileExtension(originalName);
        validateFileType(ext, file);

        // 3. category 长度校验
        if (StringUtils.hasText(category) && category.length() > 50) {
            throw new BusinessException(400, "分类名称最多50个字符");
        }

        // 4. 生成存储路径：./uploads/{userId}/{yyyyMMdd}/{uuid}.{ext}
        Long userId = UserContext.getCurrentUserId();
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        String relativePath = userId + "/" + dateDir + "/" + storedName;
        Path fullPath = Paths.get(uploadDir, relativePath);

        // 5. 保存文件到磁盘
        try {
            Files.createDirectories(fullPath.getParent());
            file.transferTo(fullPath.toFile());
            log.info("文件保存成功: path={}", fullPath);
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException(500, "文件保存失败");
        }

        // 6. 保存数据库记录（失败时清理磁盘文件）
        Long materialId;
        try {
            materialId = insertMaterialRecord(userId, originalName, storedName, ext,
                    file.getSize(), relativePath, category);
        } catch (Exception e) {
            try { Files.deleteIfExists(fullPath); } catch (IOException ignored) {}
            throw e;
        }

        // 7. 异步处理（通过 Spring 代理调用，传入 userId 避免竞态）
        asyncProcessor.processMaterial(materialId, fullPath.toString(), ext, userId);

        // 8. 构建响应
        MaterialUploadVO vo = new MaterialUploadVO();
        vo.setId(materialId);
        vo.setOriginalName(originalName);
        vo.setFileType(ext);
        vo.setFileSize(file.getSize());
        vo.setCategory(category);
        vo.setStatus("processing");
        vo.setCreateTime(LocalDateTime.now());
        return vo;
    }

    /**
     * 插入资料记录（单条 INSERT，依赖 auto-commit）
     */
    private Long insertMaterialRecord(Long userId, String originalName, String storedName,
                                      String fileType, Long fileSize, String filePath,
                                      String category) {
        LearningMaterial material = new LearningMaterial();
        material.setUserId(userId);
        material.setOriginalName(originalName);
        material.setStoredName(storedName);
        material.setFileType(fileType);
        material.setFileSize(fileSize);
        material.setFilePath(filePath);
        material.setCategory(category);
        material.setStatus("processing");
        material.setChunkCount(0);
        materialMapper.insert(material);
        return material.getId();
    }

    @Override
    public Page<MaterialVO> list(MaterialListRequest request) {
        Long userId = UserContext.getCurrentUserId();

        Page<LearningMaterial> page = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<LearningMaterial> wrapper = new LambdaQueryWrapper<LearningMaterial>()
                .eq(LearningMaterial::getUserId, userId)
                .eq(StringUtils.hasText(request.getStatus()),
                        LearningMaterial::getStatus, request.getStatus())
                .eq(StringUtils.hasText(request.getCategory()),
                        LearningMaterial::getCategory, request.getCategory())
                .orderByDesc(LearningMaterial::getCreateTime);

        Page<LearningMaterial> result = materialMapper.selectPage(page, wrapper);

        // 转换为 VO
        Page<MaterialVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<MaterialVO> voList = result.getRecords().stream().map(m -> {
            MaterialVO vo = new MaterialVO();
            BeanUtils.copyProperties(m, vo);
            return vo;
        }).toList();
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public MaterialVO getDetail(Long id) {
        Long userId = UserContext.getCurrentUserId();

        LearningMaterial material = materialMapper.selectById(id);
        if (material == null) {
            throw new BusinessException(404, "资料不存在");
        }
        if (!material.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权访问该资源");
        }

        MaterialVO vo = new MaterialVO();
        BeanUtils.copyProperties(material, vo);
        return vo;
    }

    @Override
    public void delete(Long id) {
        Long userId = UserContext.getCurrentUserId();

        LearningMaterial material = materialMapper.selectById(id);
        if (material == null) {
            throw new BusinessException(404, "资料不存在");
        }
        if (!material.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除该资源");
        }

        // 逻辑删除资料（@TableLogic 自动处理）
        materialMapper.deleteById(id);

        // 逻辑删除关联的切片（与 Spec 2.4 保持一致）
        LambdaQueryWrapper<MaterialChunk> wrapper = new LambdaQueryWrapper<MaterialChunk>()
                .eq(MaterialChunk::getMaterialId, id);
        chunkMapper.delete(wrapper);  // MaterialChunk 已添加 @TableLogic，自动执行逻辑删除

        // 删除磁盘上的物理文件（filePath 是相对路径，需拼接 uploadDir）
        try {
            Path filePath = Paths.get(uploadDir, material.getFilePath());
            Files.deleteIfExists(filePath);
            log.info("磁盘文件已删除: {}", filePath);
        } catch (IOException e) {
            // 文件删除失败不影响业务，只记录日志
            log.warn("磁盘文件删除失败: {}", material.getFilePath(), e);
        }

        log.info("资料删除成功: id={}", id);
    }

    @Override
    public void retryProcess(Long id) {
        Long userId = UserContext.getCurrentUserId();

        LearningMaterial material = materialMapper.selectById(id);
        if (material == null) {
            throw new BusinessException(404, "资料不存在");
        }
        if (!material.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权操作该资源");
        }
        if (!"failed".equals(material.getStatus())) {
            throw new BusinessException(400, "仅失败状态的资料可重试");
        }

        asyncProcessor.retryProcess(id);
    }

    /**
     * 校验文件类型（扩展名 + Magic Number）
     */
    private void validateFileType(String ext, MultipartFile file) {
        if (!allowedExtensionSet.contains(ext)) {
            throw new BusinessException(2001, "不支持的文件类型");
        }

        String magic = MAGIC_NUMBERS.get(ext);
        if (magic != null) {
            try {
                byte[] header = new byte[magic.length() / 2];
                try (InputStream is = file.getInputStream()) {
                    int read = is.read(header);
                    if (read < header.length) {
                        throw new BusinessException(2002, "文件内容与扩展名不匹配");
                    }
                }
                String fileMagic = bytesToHex(header);
                if (!fileMagic.toUpperCase().startsWith(magic.toUpperCase())) {
                    throw new BusinessException(2002, "文件内容与扩展名不匹配");
                }
            } catch (IOException e) {
                throw new BusinessException(500, "文件读取失败");
            }
        }
    }

    /**
     * 获取文件扩展名（小写），规范化处理去除路径部分
     */
    private String getFileExtension(String filename) {
        // 规范化：去除路径部分
        filename = Paths.get(filename).getFileName().toString();
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

}
