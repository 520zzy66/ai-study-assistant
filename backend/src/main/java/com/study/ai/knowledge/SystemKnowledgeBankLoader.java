package com.study.ai.knowledge;

import com.study.ai.parser.ChunkSplitter;
import com.study.ai.parser.DocumentParser;
import com.study.entity.SystemKnowledgeImportLog;
import com.study.mapper.SystemKnowledgeImportLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 系统知识库加载器 — 项目启动时扫描指定目录并增量向量化入库
 *
 * <p><b>幂等设计</b>：通过文件内容 MD5 判断是否需要重新入库。
 * 首次启动全量入库；后续启动仅处理新增或内容变更的文件。
 *
 * <p><b>目录结构要求</b>（resources/knowledge-bank/）：
 * <pre>
 * knowledge-bank/
 * ├── civil/           → 领域 = CIVIL
 * │   ├── xingce/      → 科目 = 行测
 * │   │   ├── 判断推理/ → 模块 = 判断推理
 * │   │   └── ...
 * │   └── shenlun/
 * ├── graduate/        → 领域 = GRADUATE
 * └── general/         → 领域 = GENERAL
 * </pre>
 *
 * <p><b>向量化元数据</b>：每个切片附带来源信息（domain/subject/module/filePath 等），
 * 专家 Agent 可通过元数据过滤做精确检索。
 *
 * @author AI Study Assistant
 */
@Slf4j
@Component
public class SystemKnowledgeBankLoader implements ApplicationRunner {

    /** 知识库根目录（相对项目根目录 或 绝对路径） */
    @Value("${ai.knowledge-bank.root:resources/knowledge-bank}")
    private String knowledgeBankRoot;

    /** 支持的文件扩展名 */
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "doc", "docx", "txt", "md");

    /** 每次启动的批次号（基于当前时间） */
    private String currentBatch;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DocumentParser documentParser;

    @Autowired
    private ChunkSplitter chunkSplitter;

    private final SystemKnowledgeImportLogMapper importLogMapper;

    SystemKnowledgeBankLoader(SystemKnowledgeImportLogMapper importLogMapper) {
        this.importLogMapper = importLogMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 生成本次启动的批次号（用于 importBatch 字段）
        currentBatch = LocalDateTime.now().toString();

        File rootFile = resolveKnowledgeBankRoot();
        if (rootFile == null) {
            log.info("[KnowledgeBank] 知识库目录不存在：{}，跳过初始化", knowledgeBankRoot);
            return;
        }

        // 1. 递归扫描所有支持的文件
        List<File> allFiles = scanDirectory(rootFile);
        if (allFiles.isEmpty()) {
            log.info("[KnowledgeBank] 目录下无支持的文件，跳过初始化");
            return;
        }

        log.info("[KnowledgeBank] 扫描到 {} 个文件，开始增量入库...（根目录：{}）",
                allFiles.size(), knowledgeBankRoot);

        // 2. 过滤：只处理需要入库的文件（MD5 判断）
        List<File> toImport = allFiles.stream()
                .filter(this::needsImport)
                .toList();

        if (toImport.isEmpty()) {
            log.info("[KnowledgeBank] 所有文件已入库且无变更，启动跳过");
            return;
        }

        log.info("[KnowledgeBank] 需要入库 {} 个文件（新增或已更新）", toImport.size());

        // 3. 逐个入库
        int successCount = 0;
        int failCount = 0;
        for (File file : toImport) {
            try {
                importSingleFile(file, rootFile);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.warn("[KnowledgeBank] 入库失败：{} - {}", file.getPath(), e.getMessage());
            }
        }

        log.info("[KnowledgeBank] 增量入库完成：成功 {} 个，失败 {} 个，总库 {} 个文件",
                successCount, failCount, importLogMapper.countImportedFiles());
    }

    // ===================== 文件扫描 =====================

    /**
     * 递归扫描目录中的所有支持文件
     *
     * @param root 根目录
     * @return 符合条件的文件列表
     */
    private List<File> scanDirectory(File root) {
        List<File> files = new ArrayList<>();
        File[] children = root.listFiles();
        if (children == null) return files;

        for (File child : children) {
            if (child.isDirectory()) {
                files.addAll(scanDirectory(child));
            } else if (isSupportedFile(child)) {
                files.add(child);
            }
        }
        return files;
    }

    /**
     * 判断文件是否为支持的知识库格式
     */
    private boolean isSupportedFile(File file) {
        if (!file.isFile()) return false;
        String ext = getFileExtension(file.getName()).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(ext);
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex >= 0 && dotIndex < fileName.length() - 1)
                ? fileName.substring(dotIndex + 1)
                : "";
    }

    // ===================== 幂等判断 =====================

    /**
     * 判断文件是否需要入库
     *
     * <p>判定条件（满足任一即需要入库）：
     * <ul>
     *   <li>文件从未导入（数据库无记录）</li>
     *   <li>文件内容 MD5 变更（内容被修改过）</li>
     *   <li>文件大小变更（快速预检，避免不必要的 MD5 计算）</li>
     * </ul>
     *
     * @param file 待检查文件
     * @return true = 需要入库；false = 跳过
     */
    private boolean needsImport(File file) {
        String relativePath = getRelativePath(file);
        SystemKnowledgeImportLog existingLog = importLogMapper.selectByFilePath(relativePath);

        // 从未导入 → 需要入库
        if (existingLog == null) {
            log.debug("[KnowledgeBank] 新文件待入库：{}", relativePath);
            return true;
        }

        // 文件大小变了 → 大概率内容变了，需要入库
        if (existingLog.getFileSize() == null || existingLog.getFileSize() != file.length()) {
            log.debug("[KnowledgeBank] 文件大小变更，需要重新入库：{}", relativePath);
            return true;
        }

        // 大小一样 → 计算 MD5 确认内容是否变更
        try {
            String currentMd5 = computeFileMd5(file);
            boolean changed = !currentMd5.equals(existingLog.getFileMd5());
            if (changed) {
                log.debug("[KnowledgeBank] 文件内容 MD5 变更，需要重新入库：{}", relativePath);
            }
            return changed;
        } catch (Exception e) {
            log.warn("[KnowledgeBank] 计算 MD5 失败，强制重新入库：{} - {}", relativePath, e.getMessage());
            return true;
        }
    }

    /**
     * 计算文件内容的 MD5 哈希（方案二：内容 hash）
     *
     * @param file 文件
     * @return 32 小写十六进制 MD5 字符串
     * @throws Exception 读取或计算失败
     */
    private String computeFileMd5(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ===================== 单文件入库 =====================

    /**
     * 导入单个知识库文件：解析 → 切片 → 向量化入库 → 记录日志
     *
     * @param file     文件对象
     * @param rootFile 知识库根目录（用于计算相对路径）
     */
    private void importSingleFile(File file, File rootFile) throws Exception {
        String relativePath = getRelativePath(file, rootFile);
        String fileExt = getFileExtension(file.getName()).toLowerCase();

        log.info("[KnowledgeBank] 开始入库：{}", relativePath);

        // 1. 解析文件内容
        String text;
        try (InputStream input = new FileInputStream(file)) {
            text = documentParser.parse(input, fileExt);
        }

        if (text == null || text.isBlank()) {
            log.warn("[KnowledgeBank] 文件内容为空，跳过：{}", relativePath);
            return;
        }

        // 2. 文本切片
        List<String> chunks = chunkSplitter.split(text);
        if (chunks.isEmpty()) {
            log.warn("[KnowledgeBank] 切片为空，跳过：{}", relativePath);
            return;
        }

        // 3. 推断元数据
        Map<String, Object> baseMetadata = inferMetadata(relativePath);

        // 4. 如果文件之前已入库，先删除旧向量（避免重复）
        deleteOldVectors(relativePath);

        // 5. 构造 Document 列表（附带元数据）
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> chunkMeta = new HashMap<>(baseMetadata);
            chunkMeta.put("chunkIndex", i);
            chunkMeta.put("chunkTotal", chunks.size());
            documents.add(new Document(chunks.get(i), chunkMeta));
        }

        // 6. 向量化入库
        vectorStore.add(documents);

        // 7. 记录/更新导入日志
        String md5 = computeFileMd5(file);
        upsertImportLog(relativePath, md5, file.length(), chunks.size());

        log.info("[KnowledgeBank] 已入库：{} ({} 片, MD5={}, 批次={})",
                relativePath, chunks.size(), md5.substring(0, 8) + "...", currentBatch);
    }

    /**
     * 从文件路径推断向量元数据（domain/subject/module）
     *
     * <p>路径规则：
     * <ul>
     *   <li>路径包含 civil/考公      → domain = CIVIL</li>
     *   <li>路径包含 graduate/考研   → domain = GRADUATE</li>
     *   <li>其他                     → domain = GENERAL</li>
     * </ul>
     *
     * @param relativePath 相对路径（如 civil/xingce/判断推理/逻辑判断.pdf）
     * @return 元数据 Map
     */
    private Map<String, Object> inferMetadata(String relativePath) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("source", "system_knowledge_bank");
        meta.put("filePath", relativePath);
        meta.put("importBatch", currentBatch);

        // 动态提取叶子文件夹名称（直接所属父目录名）作为元数据
        String pathForFolder = relativePath.replace("\\", "/");
        int lastSlash = pathForFolder.lastIndexOf('/');
        if (lastSlash > 0) {
            String subStr = pathForFolder.substring(0, lastSlash);
            int prevSlash = subStr.lastIndexOf('/');
            String folderName = prevSlash >= 0 ? subStr.substring(prevSlash + 1) : subStr;
            meta.put("folderName", folderName);
        } else {
            meta.put("folderName", "root");
        }

        // 统一转为小写做判断
        String path = relativePath.toLowerCase().replace("\\", "/");

        // 领域判断
        if (path.contains("civil") || path.contains("考公")) {
            meta.put("domain", "CIVIL");
        } else if (path.contains("graduate") || path.contains("考研")) {
            meta.put("domain", "GRADUATE");
        } else {
            meta.put("domain", "GENERAL");
        }

        // 科目判断（考公）
        if ("CIVIL".equals(meta.get("domain"))) {
            if (path.contains("xingce") || path.contains("行测")) {
                meta.put("subject", "行测");
                // 模块判断
                if (path.contains("判断推理") || path.contains("logic")) meta.put("module", "判断推理");
                else if (path.contains("资料分析") || path.contains("data_analysis")) meta.put("module", "资料分析");
                else if (path.contains("数量关系") || path.contains("math")) meta.put("module", "数量关系");
                else if (path.contains("言语理解") || path.contains("verbal")) meta.put("module", "言语理解");
                else if (path.contains("常识判断") || path.contains("common_sense")) meta.put("module", "常识判断");
            } else if (path.contains("shenlun") || path.contains("申论")) {
                meta.put("subject", "申论");
                if (path.contains("大作文") || path.contains("essay")) meta.put("module", "大作文");
                else if (path.contains("应用文") || path.contains("practical")) meta.put("module", "应用文");
                else if (path.contains("材料分析") || path.contains("material")) meta.put("module", "材料分析");
            } else if (path.contains("mianshi") || path.contains("面试")) {
                meta.put("subject", "面试");
            }
        }

        // 科目判断（考研）
        if ("GRADUATE".equals(meta.get("domain"))) {
            if (path.contains("math") || path.contains("数学") || path.contains("高数")) {
                meta.put("subject", "数学");
            } else if (path.contains("english") || path.contains("英语")) {
                meta.put("subject", "英语");
            } else if (path.contains("politics") || path.contains("政治") || path.contains("思政")) {
                meta.put("subject", "政治");
            } else if (path.contains("专业课") || path.contains("major")) {
                meta.put("subject", "专业课");
            }
        }

        return meta;
    }

    /**
     * 删除指定文件路径的旧向量（确保重复入库不叠加）
     *
     * <p>通过元数据 `filePath` 精确匹配旧切片。
     */
    private void deleteOldVectors(String relativePath) {
        try {
            // 按元数据 filePath 精确匹配旧切片并删除
            String filterExpression = "filePath == '" + relativePath.replace("'", "\\'") + "'";
            var searchRequest = org.springframework.ai.vectorstore.SearchRequest.builder()
                    .query("")
                    .topK(1000)
                    .filterExpression(filterExpression)
                    .build();
            var oldDocs = vectorStore.similaritySearch(searchRequest);
            if (oldDocs != null && !oldDocs.isEmpty()) {
                List<String> oldIds = oldDocs.stream()
                        .map(doc -> getIdFromDocument(doc))
                        .filter(id -> id != null && !id.isBlank())
                        .collect(java.util.stream.Collectors.toList());
                if (!oldIds.isEmpty()) {
                    vectorStore.delete(oldIds);
                    log.debug("[KnowledgeBank] 删除旧向量 {} 个", oldIds.size());
                }
            }
        } catch (Exception e) {
            log.warn("[KnowledgeBank] 删除旧向量失败（可能是首次入库）：{}", e.getMessage());
        }
    }

    /**
     * 从 Document 的 metadata 中提取 filePath 用于匹配
     */
    private String getIdFromDocument(Document doc) {
        if (doc.getId() != null) return doc.getId();
        Object id = doc.getMetadata().get("id");
        return id != null ? id.toString() : null;
    }

    /**
     * 插入或更新导入日志
     */
    private void upsertImportLog(String relativePath, String md5, long fileSize, int chunkCount) {
        SystemKnowledgeImportLog log = importLogMapper.selectByFilePath(relativePath);
        if (log == null) {
            log = new SystemKnowledgeImportLog();
            log.setFilePath(relativePath);
            log.setKnowledgeRoot(knowledgeBankRoot);
        }
        log.setFileMd5(md5);
        log.setFileSize(fileSize);
        log.setChunkCount(chunkCount);
        log.setImportBatch(currentBatch);

        if (log.getId() == null) {
            importLogMapper.insert(log);
        } else {
            importLogMapper.updateById(log);
        }
    }

    // ===================== 工具方法 =====================

    /**
     * 计算文件相对于知识库根目录的相对路径（使用成员变量 knowledgeBankRoot）
     */
    private String getRelativePath(File file) {
        Path rootPath = Paths.get(knowledgeBankRoot).toAbsolutePath().normalize();
        Path filePath = file.toPath().toAbsolutePath().normalize();
        if (filePath.startsWith(rootPath)) {
            return rootPath.relativize(filePath).toString().replace("\\", "/");
        }
        return file.getPath().replace("\\", "/");
    }

    /**
     * 计算文件相对于指定根目录的相对路径
     */
    private String getRelativePath(File file, File rootFile) {
        Path rootPath = rootFile.toPath().toAbsolutePath().normalize();
        Path filePath = file.toPath().toAbsolutePath().normalize();
        if (filePath.startsWith(rootPath)) {
            return rootPath.relativize(filePath).toString().replace("\\", "/");
        }
        return rootFile.toPath().relativize(file.toPath()).toString().replace("\\", "/");
    }

    /**
     * 解析并探测知识库根目录。如果配置的路径不存在，则自动尝试备选相对路径，并更新 knowledgeBankRoot 成员变量。
     *
     * @return 实际存在的知识库根目录，若均不存在则返回 null
     */
    public File resolveKnowledgeBankRoot() {
        Path rootPath = Paths.get(knowledgeBankRoot);
        File rootFile = rootPath.toFile();

        if (rootFile.exists() && rootFile.isDirectory()) {
            return rootFile;
        }

        // 自动探测逻辑：如果配置的路径不存在，尝试从常见路径中探测
        String[] fallbackPaths = {
            "src/main/resources/knowledge-bank",
            "backend/src/main/resources/knowledge-bank",
            "resources/knowledge-bank"
        };
        
        for (String fallback : fallbackPaths) {
            File temp = new File(fallback);
            if (temp.exists() && temp.isDirectory()) {
                log.info("[KnowledgeBank] 自动探测到实际知识库目录：{}", fallback);
                this.knowledgeBankRoot = fallback; // 更新成员变量以保持一致
                return temp;
            }
        }
        
        return null;
    }
}
