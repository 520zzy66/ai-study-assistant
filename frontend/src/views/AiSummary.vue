<template>
  <div class="summary-page">
    <BasePageHeader
      title="AI 知识总结"
      description="选择学习资料或文件夹，生成结构化的知识总结"
    />

    <div class="summary-layout">
      <!-- Left Panel -->
      <div class="summary-sidebar">
        <BaseCard title="选择资料" class="sidebar-card">
          <!-- 级联选择器 -->
          <div class="material-selector">
            <span class="selector-label">选择文件夹或资料</span>
            <el-cascader
              v-model="cascaderValue"
              :options="cascaderOptions"
              :props="cascaderProps"
              placeholder="请选择..."
              clearable
              style="width: 100%"
              @change="handleCascaderChange"
            >
              <template #default="{ node, data }">
                <div class="cascader-node">
                  <el-icon v-if="data.isFolder" class="cascader-icon folder-icon">
                    <Folder />
                  </el-icon>
                  <el-icon v-else class="cascader-icon material-icon">
                    <Document />
                  </el-icon>
                  <span class="cascader-label">{{ data.label }}</span>
                  <span v-if="data.isFolder && data.materialCount" class="cascader-count">
                    {{ data.materialCount }} 份资料
                  </span>
                </div>
              </template>
            </el-cascader>
          </div>

          <!-- 选中项信息 -->
          <div v-if="selectedInfo" class="selected-info">
            <div class="selected-path">
              <el-icon><Location /></el-icon>
              <span>{{ selectedDisplayPath }}</span>
            </div>
            <div v-if="selectedInfo.type === 'material' && selectedMaterial" class="material-meta">
              <div class="meta-item">
                <span class="meta-label">文件名</span>
                <span class="meta-value truncate">{{ selectedMaterial.originalName }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">类型</span>
                <span class="meta-value">{{ selectedMaterial.fileType?.toUpperCase() }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">大小</span>
                <span class="meta-value">{{ formatFileSize(selectedMaterial.fileSize) }}</span>
              </div>
              <div class="meta-item">
                <span class="meta-label">状态</span>
                <el-tag :type="getStatusType(selectedMaterial.status)" size="small" effect="light">
                  {{ getStatusLabel(selectedMaterial.status) }}
                </el-tag>
              </div>
            </div>
            <div v-else-if="selectedInfo.type === 'folder'" class="folder-meta">
              <el-icon :size="16"><Folder /></el-icon>
              <span>将对此文件夹下所有资料进行综合总结</span>
            </div>
          </div>

          <el-button
            type="primary"
            class="generate-btn"
            :loading="generating"
            :disabled="!selectedInfo"
            @click="handleGenerate"
          >
            <el-icon><MagicStick /></el-icon>
            生成总结
          </el-button>
        </BaseCard>
      </div>

      <!-- Right Panel -->
      <div class="summary-main">
        <BaseCard class="content-card" :padding="'none'">
          <!-- Toolbar -->
          <div v-if="summary || generating" class="content-toolbar">
            <div class="toolbar-left">
              <span class="content-title">知识总结</span>
            </div>
            <div class="toolbar-right">
              <el-button text :icon="CopyDocument" :disabled="!summary" @click="handleCopy">复制</el-button>
              <el-button text :icon="Refresh" :disabled="!summary" @click="handleRegenerate">重新生成</el-button>
              <el-button text :icon="Download" :disabled="!summary" @click="handleDownload">下载</el-button>
            </div>
          </div>

          <!-- Loading (folder mode) -->
          <div v-if="generating && !streaming" class="generating-state">
            <el-progress :percentage="progress" :stroke-width="6" class="generate-progress" />
            <div class="generate-status">
              <el-icon class="status-icon" :size="16"><Loading /></el-icon>
              <span>{{ generateStatus }}</span>
            </div>
            <div class="generate-steps">
              <div
                v-for="(step, index) in generateSteps"
                :key="index"
                class="generate-step"
                :class="{ active: progress >= step.percent, done: progress > step.percent }"
              >
                <el-icon :size="14"><CircleCheck v-if="progress > step.percent" /><CircleCheckFilled v-else-if="progress >= step.percent" /><CircleCheck v-else /></el-icon>
                <span>{{ step.label }}</span>
              </div>
            </div>
          </div>

          <!-- Streaming Content -->
          <div v-else-if="streaming || summary" class="summary-body">
            <!-- 流式进度条 -->
            <div v-if="streaming" class="stream-progress">
              <el-progress :percentage="progress" :stroke-width="3" :show-text="false" />
              <span class="stream-status">{{ generateStatus }}</span>
            </div>

            <!-- 总结内容 -->
            <div class="summary-content markdown-body" v-html="renderMarkdown(summary)" />
            <span v-if="streaming" class="streaming-cursor" />

            <!-- 思维导图展示区 -->
            <div v-if="summary && !streaming" class="mindmap-section">
              <div class="mindmap-header">
                <span class="mindmap-title">📊 知识思维导图</span>
                <el-tag v-if="mindMapLoading" type="info" size="small" effect="light">生成中...</el-tag>
                <el-button v-if="mindMap" text size="small" type="primary" @click="goToMindmapWorkbench">
                  <el-icon><Connection /></el-icon> 在工作台中编辑
                </el-button>
              </div>
              <div v-if="mindMapLoading" class="mindmap-loading">
                <el-icon class="status-icon" :size="20"><Loading /></el-icon>
                <span>正在生成思维导图...</span>
              </div>
              <div v-else-if="mindMap" class="mindmap-container">
                <!-- 只读模式： readonly=true 禁用双击编辑 -->
                <InteractiveMindMap :data="mindMap" :readonly="true" />
              </div>
              <div v-else class="mindmap-container" style="text-align: center; padding: 30px 0;">
                <el-button type="primary" @click="goToMindmapWorkbench">
                  <el-icon><Connection /></el-icon> 前往导图工作台生成导图
                </el-button>
              </div>
            </div>
          </div>

          <!-- Empty -->
          <div v-else class="summary-empty">
            <AppEmpty
              icon="Document"
              title="选择资料开始总结"
              description="AI 将自动提取核心知识点、重要概念和学习建议"
            />
          </div>
        </BaseCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, Refresh, Download, MagicStick, Loading, CircleCheck, CircleCheckFilled, Folder, Document, Location, Connection } from '@element-plus/icons-vue'
import { formatFileSize, getStatusLabel, getStatusType } from '@/utils/format'
import { useMarkdown } from '@/composables/useMarkdown'
import { generateSummaryAsync, generateSummaryStream, generateMindMap, getMindMap } from '@/api/ai'
import { loadAvailableMaterials } from '@/api/material'
import { getFolderTree } from '@/api/materialFolder'
import { buildCascaderOptions, parseCascaderValue, getCascaderLabel } from '@/utils/folderTreeHelper'
import { useTaskStore } from '@/stores/task'
import BaseCard from '@/components/common/BaseCard.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'
import InteractiveMindMap from '@/components/common/InteractiveMindMap.vue'

const route = useRoute()
const router = useRouter()
const { renderMarkdown } = useMarkdown()
const taskStore = useTaskStore()

const folderTree = ref([])
const materialList = ref([])
const cascaderValue = ref([])
const summary = ref('')
const generating = ref(false)
const streaming = ref(false)
const progress = ref(0)
const generateStatus = ref('准备中...')
const mindMap = ref(null)
const mindMapLoading = ref(false)
let abortStream = null

const generateSteps = [
  { label: '读取文档', percent: 10 },
  { label: '提取重点', percent: 40 },
  { label: '生成内容', percent: 70 },
  { label: '格式化输出', percent: 95 }
]

// 级联选择器配置
const cascaderProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  checkStrictly: false, // 只能选择叶子节点或文件夹节点
  emitPath: true // 返回完整路径
}

// 构建级联选择器选项
const cascaderOptions = computed(() => {
  return buildCascaderOptions(folderTree.value, materialList.value)
})

// 解析选中项
const selectedInfo = computed(() => {
  if (!cascaderValue.value || cascaderValue.value.length === 0) return null
  return parseCascaderValue(cascaderValue.value, cascaderOptions.value)
})

// 显示路径
const selectedDisplayPath = computed(() => {
  if (!cascaderValue.value || cascaderValue.value.length === 0) return ''
  return getCascaderLabel(cascaderValue.value, cascaderOptions.value)
})

// 选中的资料（仅当选择的是资料时）
const selectedMaterial = computed(() => {
  if (!selectedInfo.value || selectedInfo.value.type !== 'material') return null
  return materialList.value.find(m => m.id === selectedInfo.value.id) || null
})

// 级联选择器变化处理
function handleCascaderChange(value) {
  cascaderValue.value = value || []
}

function goToMindmapWorkbench() {
  if (!selectedInfo.value || selectedInfo.value.type !== 'material') return
  router.push(`/ai/mindmap?materialId=${selectedInfo.value.id}`)
}

async function handleGenerate() {
  if (!selectedInfo.value) {
    ElMessage.warning('请先选择资料或文件夹')
    return
  }
  if (summary.value) {
    try {
      await ElMessageBox.confirm('重新生成将覆盖当前总结，是否继续？', '确认重新生成', {
        type: 'warning'
      })
    } catch {
      return
    }
  }

  // 文件夹总结使用异步任务模式（无法流式）
  if (selectedInfo.value.type === 'folder') {
    await handleGenerateFolder()
    return
  }

  // 资料总结使用流式模式
  await handleGenerateStream()
}

/**
 * 流式生成资料总结（SSE 打字机效果）
 */
async function handleGenerateStream() {
  const materialId = selectedInfo.value.id

  generating.value = true
  streaming.value = true
  progress.value = 0
  summary.value = ''
  mindMap.value = null
  generateStatus.value = '正在连接 AI...'

  // 模拟进度（流式模式下根据 token 到达推进）
  let tokenCount = 0
  const progressTimer = setInterval(() => {
    if (streaming.value && progress.value < 90) {
      progress.value = Math.min(90, progress.value + 2)
    }
  }, 500)

  abortStream = generateSummaryStream(materialId, {
    onToken(token, fullText) {
      summary.value = fullText
      tokenCount++
      if (tokenCount === 1) {
        generateStatus.value = '正在生成总结...'
        progress.value = 20
      }
    },
    onComplete(fullText) {
      clearInterval(progressTimer)
      streaming.value = false
      progress.value = 95
      generateStatus.value = '正在生成思维导图...'

      // 流式完成，触发导图生成
      loadMindMap(materialId)
    },
    onError(err) {
      clearInterval(progressTimer)
      generating.value = false
      streaming.value = false
      progress.value = 0
      ElMessage.error(err.message || '总结生成失败')
    }
  })
}

/**
 * 异步生成文件夹总结
 */
async function handleGenerateFolder() {
  generating.value = true
  streaming.value = false
  progress.value = 0
  summary.value = ''
  mindMap.value = null
  generateStatus.value = '正在创建任务...'

  try {
    const options = { force: false, folderId: selectedInfo.value.id }
    const { taskId } = await generateSummaryAsync(null, options)

    taskStore.watchTask(taskId, 'summary', {
      onProgress(pct, msg) {
        progress.value = pct
        generateStatus.value = msg
      },
      onSuccess(result) {
        summary.value = result.summary || result
        generating.value = false
        ElMessage.success('总结生成成功')
      },
      onError(errMsg) {
        generating.value = false
        ElMessage.error(errMsg || '总结生成失败')
      }
    })
  } catch {
    generating.value = false
    ElMessage.error('创建任务失败')
  }
}

/**
 * 加载思维导图（优先生成，已有则直接读取）
 */
async function loadMindMap(materialId) {
  mindMapLoading.value = true
  try {
    // 先尝试生成思维导图
    const result = await generateMindMap(materialId)
    if (result?.mindMap) {
      try {
        mindMap.value = JSON.parse(result.mindMap)
      } catch {
        mindMap.value = result.mindMap
      }
    }
  } catch (err) {
    console.warn('思维导图生成失败:', err)
    // 降级：尝试读取已有的
    try {
      const existing = await getMindMap(materialId)
      if (existing?.mindMap) {
        try {
          mindMap.value = JSON.parse(existing.mindMap)
        } catch {
          mindMap.value = existing.mindMap
        }
      }
    } catch {
      // 静默失败
    }
  } finally {
    mindMapLoading.value = false
    generating.value = false
    progress.value = 100
    generateStatus.value = '完成'
    ElMessage.success('总结生成成功')
  }
}

function handleCopy() {
  navigator.clipboard.writeText(summary.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

async function handleRegenerate() {
  await handleGenerate()
}

function handleDownload() {
  const blob = new Blob([summary.value], { type: 'text/markdown' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${selectedDisplayPath.value || 'summary'}.md`
  a.click()
  URL.revokeObjectURL(url)
}

async function loadMaterials() {
  const [materials, folders] = await Promise.all([
    loadAvailableMaterials(),
    getFolderTree()
  ])
  materialList.value = materials || []
  folderTree.value = folders || []

  // 支持从资料库跳转自动选择
  const queryId = route.query.materialId
  if (queryId) {
    const material = materialList.value.find(m => m.id === Number(queryId))
    if (material) {
      // 构建级联选择器的值路径
      const path = []
      if (material.folderId) {
        // 查找文件夹路径
        const folderPath = findFolderPath(folderTree.value, material.folderId)
        if (folderPath) {
          path.push(...folderPath.map(f => `folder_${f.id}`))
        }
      }
      path.push(`material_${material.id}`)
      cascaderValue.value = path
    }
  }
}

// 查找文件夹路径（用于自动选中）
function findFolderPath(tree, targetId, path = []) {
  for (const node of tree) {
    if (node.id === targetId) {
      return [...path, { id: node.id, name: node.name }]
    }
    if (node.children?.length) {
      const found = findFolderPath(node.children, targetId, [...path, { id: node.id, name: node.name }])
      if (found) return found
    }
  }
  return null
}

onMounted(async () => {
  await loadMaterials()
  // 恢复未完成的任务
  const activeTask = taskStore.getFirstActiveOfType('summary')
  if (activeTask) {
    generating.value = true
    generateStatus.value = '恢复任务中...'
    taskStore.watchTask(activeTask.taskId, 'summary', {
      onProgress(pct, msg) {
        progress.value = pct
        generateStatus.value = msg
      },
      onSuccess(result) {
        summary.value = result.summary || result
        generating.value = false
        ElMessage.success('总结生成成功')
      },
      onError(errMsg) {
        generating.value = false
        ElMessage.error(errMsg || '总结生成失败')
      }
    })
  }
})

watch(() => route.query.materialId, (newId) => {
  if (newId) {
    const material = materialList.value.find(m => m.id === Number(newId))
    if (material) {
      const path = []
      if (material.folderId) {
        const folderPath = findFolderPath(folderTree.value, material.folderId)
        if (folderPath) {
          path.push(...folderPath.map(f => `folder_${f.id}`))
        }
      }
      path.push(`material_${material.id}`)
      cascaderValue.value = path
    }
  }
})
</script>

<style scoped>
.summary-page {
  width: 100%;
  max-width: var(--content-work-width);
}

.summary-layout {
  display: grid;
  grid-template-columns: minmax(260px, 300px) minmax(0, 1fr);
  gap: var(--space-6);
  align-items: start;
}

.summary-sidebar {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
}

.sidebar-card,
.content-card {
  border-radius: var(--radius-lg);
}

.content-card {
  min-height: 560px;
}

.material-selector {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-bottom: var(--space-5);
}

.selector-label {
  font-size: var(--text-ui);
  font-weight: 500;
  color: var(--color-text-secondary);
}

/* 级联选择器节点样式 */
.cascader-node {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.cascader-icon {
  flex-shrink: 0;
  font-size: 16px;
}

.folder-icon {
  color: var(--color-primary);
}

.material-icon {
  color: var(--color-text-tertiary);
}

.cascader-label {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cascader-count {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-left: auto;
}

/* 选中项信息 */
.selected-info {
  margin-bottom: var(--space-5);
  padding: var(--space-4);
  background: var(--surface-container-low);
  border-radius: var(--radius-md);
}

.selected-path {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-ui);
  font-weight: 500;
  color: var(--color-primary);
  margin-bottom: var(--space-3);
}

.selected-path .el-icon {
  flex-shrink: 0;
}

.material-meta {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.meta-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-2);
}

.meta-label {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

.meta-value {
  font-size: var(--text-body);
  color: var(--color-text-primary);
  text-align: right;
}

.folder-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-small);
  color: var(--color-text-secondary);
}

.folder-meta .el-icon {
  color: var(--color-primary);
}

.generate-btn {
  width: 100%;
}

.content-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--outline-variant);
}

.content-title {
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-text-primary);
}

.summary-content {
  min-height: 200px;
  max-width: var(--content-reading-width);
  margin-inline: auto;
  padding: var(--space-4) var(--space-2) var(--space-8);
}

.summary-empty :deep(.app-empty) {
  padding: var(--space-10) 0;
}

.generating-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-10) var(--space-8);
}

.generate-progress {
  width: 100%;
  max-width: 400px;
  margin-bottom: var(--space-5);
}

.generate-status {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-body);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-6);
}

.status-icon {
  animation: spin 1.5s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.generate-steps {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  width: 100%;
  max-width: 400px;
}

.generate-step {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-body);
  color: var(--color-text-tertiary);
  transition: color var(--duration-fast) var(--ease-default);
}

.generate-step.active {
  color: var(--color-text-primary);
  font-weight: 500;
}

.generate-step.done {
  color: var(--color-success);
}

/* 流式渲染 */
.summary-body {
  padding: var(--space-2) clamp(0px, 2vw, var(--space-6));
}

.stream-progress {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--outline-variant);
}

.stream-progress :deep(.el-progress) {
  flex: 1;
}

.stream-status {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: var(--color-primary);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: blink 0.8s step-end infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

/* 思维导图区域 */
.mindmap-section {
  margin-top: var(--space-6);
  padding-top: var(--space-5);
  border-top: 1px solid var(--outline-variant);
}

.mindmap-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.mindmap-title {
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-text-primary);
}

.mindmap-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  padding: var(--space-8) 0;
  color: var(--color-text-secondary);
}

.mindmap-container {
  overflow-x: auto;
  padding: var(--space-2) 0;
}

@media (max-width: 1279px) {
  .summary-layout {
    grid-template-columns: 1fr;
  }

  .summary-sidebar {
    position: static;
  }
}
</style>
