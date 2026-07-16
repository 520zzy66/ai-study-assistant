package com.study.ai.knowledge;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.study.mapper.SystemKnowledgeImportLogMapper;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SystemKnowledgeBankLoaderTest {

    @Test
    public void testResolveKnowledgeBankRoot() throws Exception {
        // Mock 依赖的 mapper，因为构造函数需要它
        SystemKnowledgeImportLogMapper importLogMapper = Mockito.mock(SystemKnowledgeImportLogMapper.class);
        SystemKnowledgeBankLoader loader = new SystemKnowledgeBankLoader(importLogMapper);

        // 利用反射将 loader 的 knowledgeBankRoot 设为默认的 "resources/knowledge-bank"
        java.lang.reflect.Field field = SystemKnowledgeBankLoader.class.getDeclaredField("knowledgeBankRoot");
        field.setAccessible(true);
        field.set(loader, "resources/knowledge-bank");

        File rootDir = loader.resolveKnowledgeBankRoot();
        
        assertNotNull(rootDir, "知识库目录应该能被成功探测并返回");
        assertTrue(rootDir.exists(), "探测到的目录必须存在");
        assertTrue(rootDir.isDirectory(), "探测到的路径必须是目录");
        
        // 打印探测到的绝对路径
        System.out.println("成功探测到知识库目录：" + rootDir.getAbsolutePath());
    }
}
