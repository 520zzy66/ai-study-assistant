<template>
  <div class="summary-page">
    <BasePageHeader
      title="AI 知识总结"
      description="选择学习资料，生成结构化的知识总结"
    />

    <div class="summary-layout">
      <!-- Left Panel -->
      <div class="summary-sidebar">
        <BaseCard title="选择资料" class="sidebar-card">
          <div class="material-selector">
            <el-select
              v-model="selectedMaterialId"
              placeholder="请选择资料"
              filterable
              style="width:100%;"
            >
              <el-option
                v-for="item in materialList"
                :key="item.id"
                :label="item.originalName"
                :value="item.id"
              />
            </el-select>
          </div>

          <div v-if="selectedMaterial" class="material-meta">
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

          <el-button
            type="primary"
            class="generate-btn"
            :loading="generating"
            :disabled="!selectedMaterialId"
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

          <!-- Loading -->
          <div v-if="generating" class="generating-state">
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

          <!-- Content -->
          <div v-else-if="summary" class="summary-content markdown-body" v-html="renderMarkdown(summary)" />

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
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, Refresh, Download, MagicStick, Loading, CircleCheck, CircleCheckFilled } from '@element-plus/icons-vue'
import { formatFileSize, getStatusLabel, getStatusType } from '@/utils/format'
import { useMarkdown } from '@/composables/useMarkdown'
import { generateSummaryAsync } from '@/api/ai'
import { loadAvailableMaterials } from '@/api/material'
import { useTaskStore } from '@/stores/task'
import BaseCard from '@/components/common/BaseCard.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'

const route = useRoute()
const { renderMarkdown } = useMarkdown()
const taskStore = useTaskStore()

const materialList = ref([])
const selectedMaterialId = ref('')
const summary = ref('')
const generating = ref(false)
const progress = ref(0)
const generateStatus = ref('准备中...')

const generateSteps = [
  { label: '读取文档', percent: 10 },
  { label: '提取重点', percent: 40 },
  { label: '生成内容', percent: 70 },
  { label: '格式化输出', percent: 95 }
]

const selectedMaterial = computed(() => {
  return materialList.value.find(item => item.id === selectedMaterialId.value) || null
})


async function handleGenerate() {
  if (!selectedMaterialId.value) {
    ElMessage.warning('请先选择资料')
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

  generating.value = true
  progress.value = 0
  summary.value = ''
  generateStatus.value = '正在创建任务...'

  try {
    const { taskId } = await generateSummaryAsync(selectedMaterialId.value)
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
  a.download = `${selectedMaterial.value?.originalName || 'summary'}.md`
  a.click()
  URL.revokeObjectURL(url)
}

async function loadMaterials() {
  materialList.value = await loadAvailableMaterials()
  const queryId = route.query.materialId
  if (queryId) {
    selectedMaterialId.value = Number(queryId)
  }
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
    selectedMaterialId.value = Number(newId)
  }
})
</script>

<style scoped>
.summary-page {
  width: 100%;
}

.summary-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
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

.material-select {
  width: 100%;
}

.material-meta {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  padding: var(--space-4);
  background: var(--surface-container-low);
  border-radius: var(--radius-md);
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
  padding: var(--space-2);
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

@media (max-width: 1279px) {
  .summary-layout {
    grid-template-columns: 1fr;
  }

  .summary-sidebar {
    position: static;
  }
}
</style>
