# 系统知识库（System Knowledge Bank）

> 项目启动时由 `SystemKnowledgeBankLoader` 自动扫描并向量化入库。

## 目录结构

```
knowledge-bank/
├── civil/                  # 考公领域（domain=CIVIL）
│   ├── xingce/             # 行测（subject=行测）
│   │   ├── 判断推理/        # 模块=判断推理
│   │   ├── 资料分析/        # 模块=资料分析
│   │   ├── 数量关系/        # 模块=数量关系
│   │   ├── 言语理解/        # 模块=言语理解
│   │   └── 常识判断/        # 模块=常识判断
│   ├── shenlun/            # 申论（subject=申论）
│   │   ├── 大作文/          # 模块=大作文
│   │   ├── 应用文/          # 模块=应用文
│   │   └── 材料分析/        # 模块=材料分析
│   ├── mianshi/            # 面试（subject=面试）
│   │   ├── 结构化面试/      # 模块=结构化面试
│   │   └── 无领导小组/      # 模块=无领导小组
│   └── shizeng/            # 时政热点
│       ├── 2024/
│       └── 2025/
├── graduate/               # 考研领域（domain=GRADUATE）
│   ├── math/               # 数学（subject=数学）
│   │   ├── 高数/
│   │   ├── 线性代数/
│   │   └── 概率论/
│   ├── english/            # 英语（subject=英语）
│   │   ├── 阅读理解/
│   │   └── 写作翻译/
│   └── politics/           # 政治（subject=政治）
│       ├── 马原/
│       ├── 毛中特/
│       ├── 史纲/
│       └── 思修/
└── general/                # 通用（domain=GENERAL）
    ├── study-method/       # 学习方法
    └── psychology/         # 备考心理
```

## 使用方式

1. 把 PDF/DOC/DOCX/TXT 资料拖入对应子目录
2. 重新启动项目（或由 `SystemKnowledgeBankLoader` 自动检测）
3. 启动日志会打印入库进度

## 支持的文件格式

| 格式 | 说明 |
|------|------|
| PDF | Schery 先生成文字（先 OCR），推荐 50 页以内 |
| DOC/DOCX | 直接解析，保留段落结构 |
| TXT | 直接解析（UTF-8 编码） |
| MD | Markdown 格式，保留标题层级 |

## 幂等规则

- **同一文件未变更** → 第二次启动跳过，不重复入库
- **文件内容变更** → MD5 变了，自动删除旧向量并重新入库
- **手动强制重新入库** → 修改文件（加个空格）或删除 `system_knowledge_import_log` 表中的对应记录

## 元数据说明

入库时每个切片自动附带以下元数据（供 Agent 按领域过滤检索）：

```json
{
  "source": "system_knowledge_bank",
  "domain": "CIVIL",
  "subject": "行测",
  "module": "判断推理",
  "filePath": "civil/xingce/判断推理/逻辑判断.pdf",
  "chunkIndex": 3,
  "chunkTotal": 15,
  "importBatch": "2024-07-04T10:30:00"
}
```
