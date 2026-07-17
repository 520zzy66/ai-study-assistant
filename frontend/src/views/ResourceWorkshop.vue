<template>
  <div class="resource-workshop-page">
    <BasePageHeader
      title="资源工坊"
      description="基于单份学习资料生成讲解文档、导图、题库、学习路径和多模态脚本包"
    />

    <div class="workshop-grid">
      <div class="config-column">
        <BaseCard title="生成配置" class="config-card" padding="lg">
          <el-form label-position="top">
            <el-form-item label="学习资料">
              <el-select
                v-model="form.materialId"
                filterable
                placeholder="选择一份已处理完成的资料"
                :loading="loadingMaterials"
              >
                <el-option
                  v-for="item in materials"
                  :key="item.id"
                  :label="item.originalName || item.fileName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="学习目标">
              <el-input
                v-model="form.goal"
                maxlength="200"
                show-word-limit
                placeholder="例如：两周内掌握决策树核心原理并能完成习题"
              />
            </el-form-item>

            <div class="form-row">
              <el-form-item label="截止日期">
                <el-date-picker
                  v-model="form.examDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="不填默认 14 天"
                />
              </el-form-item>
              <el-form-item label="每日时长">
                <el-input-number
                  v-model="form.dailyHours"
                  :min="1"
                  :max="12"
                  controls-position="right"
                />
              </el-form-item>
            </div>

            <el-form-item label="难度">
              <el-segmented v-model="form.difficulty" :options="difficultyOptions" />
            </el-form-item>

            <el-form-item label="生成内容">
              <div class="resource-options">
                <el-checkbox v-model="form.includeSummary">讲解文档</el-checkbox>
                <el-checkbox v-model="form.includeMindMap">思维导图</el-checkbox>
                <el-checkbox v-model="form.includeQuiz">个性化题库</el-checkbox>
                <el-checkbox v-model="form.includePlan">学习路径</el-checkbox>
                <el-checkbox v-model="form.includeMultimodalScript">多模态脚本包</el-checkbox>
              </div>
            </el-form-item>

            <el-button
              type="primary"
              class="generate-button"
              :loading="generating"
              @click="handleGenerate"
            >
              生成资源包
            </el-button>
          </el-form>
        </BaseCard>

        <BaseCard title="最近资源包" padding="sm" :loading="loadingHistory">
          <div v-if="historyTasks.length > 0" class="history-list">
            <button
              v-for="task in historyTasks"
              :key="task.taskId"
              class="history-item"
              type="button"
              @click="restorePackage(task)"
            >
              <div class="history-title">{{ parsePackageTitle(task) }}</div>
              <div class="history-meta">
                <span>{{ formatStatus(task.status) }}</span>
                <span>{{ formatTime(task.updateTime || task.createTime) }}</span>
              </div>
            </button>
          </div>
          <div v-else class="history-empty">暂无历史资源包</div>
        </BaseCard>
      </div>

      <div class="result-column">
        <BaseCard title="生成进度" padding="lg">
          <div class="progress-panel">
            <el-progress :percentage="progress" :status="progressStatus" />
            <p class="progress-message">{{ progressMessage }}</p>
          </div>
        </BaseCard>

        <BaseCard
          title="资源包结果"
          padding="lg"
        >
          <template v-if="packageResult" #header-action>
            <div class="package-actions">
              <el-button :icon="CopyDocument" size="small" @click="copyPresentationSummary">复制演示摘要</el-button>
              <el-button :icon="Download" size="small" @click="exportMarkdown">导出 Markdown</el-button>
              <el-button :icon="Download" size="small" @click="exportJson">导出 JSON</el-button>
            </div>
          </template>
          <div v-if="packageResult" class="package-result">
            <div class="package-meta">
              <div>
                <span class="meta-label">资料</span>
                <strong>{{ packageResult.materialName }}</strong>
              </div>
              <div>
                <span class="meta-label">目标</span>
                <strong>{{ packageResult.goal }}</strong>
              </div>
            </div>

            <AgentTraceTimeline v-if="packageResult.agentTrace" :trace="packageResult.agentTrace" />
            <div v-else class="agent-chain">
              <span v-for="agent in packageResult.agents" :key="agent" class="agent-chip">
                {{ agent }}
              </span>
            </div>

            <div class="package-overview">
              <div
                v-for="item in resourceSummary"
                :key="item.key"
                class="overview-item"
                :class="{ active: item.ready }"
              >
                <el-icon :size="18"><component :is="item.icon" /></el-icon>
                <span>{{ item.label }}</span>
                <strong>{{ item.ready ? '已生成' : '未生成' }}</strong>
              </div>
            </div>

            <div v-if="hasReviewInfo" class="review-panel">
              <div v-if="presentationNotes.length > 0" class="review-section">
                <span class="review-label">演示亮点</span>
                <ul>
                  <li v-for="note in presentationNotes" :key="note">{{ note }}</li>
                </ul>
              </div>
              <div v-if="agentDetails.length > 0" class="review-section">
                <span class="review-label">多智能体分工</span>
                <div class="agent-detail-list">
                  <div v-for="agent in agentDetails" :key="agent.name" class="agent-detail-item">
                    <strong>{{ agent.name }}</strong>
                    <p>{{ agent.role }}</p>
                    <span>{{ agent.output }}</span>
                  </div>
                </div>
              </div>
              <div v-if="qualityChecks" class="review-section">
                <span class="review-label">质量与安全说明</span>
                <div class="quality-grid">
                  <div>
                    <span>资料状态</span>
                    <strong>{{ qualityChecks.materialReady ? '已校验' : '未校验' }}</strong>
                  </div>
                  <div>
                    <span>生成资源数</span>
                    <strong>{{ qualityChecks.resourceCount || resourceSummary.filter(item => item.ready).length }}</strong>
                  </div>
                  <div>
                    <span>多模态模式</span>
                    <strong>{{ qualityChecks.multimodalMode || '-' }}</strong>
                  </div>
                </div>
                <p>{{ qualityChecks.grounding }}</p>
                <p>{{ qualityChecks.safety }}</p>
              </div>
            </div>

            <el-tabs v-model="activeTab" class="result-tabs">
              <el-tab-pane v-if="resources.summary" label="讲解文档" name="summary">
                <article class="markdown-body" v-html="renderMarkdown(resources.summary.content)" />
              </el-tab-pane>

              <el-tab-pane v-if="mindMapData" label="思维导图" name="mindMap">
                <MindMapTree :data="mindMapData" />
              </el-tab-pane>

              <el-tab-pane v-if="resources.quiz" label="题库" name="quiz">
                <div class="quiz-list">
                  <div
                    v-for="question in quizQuestions"
                    :key="question.id || question.question"
                    class="quiz-item"
                  >
                    <div class="quiz-title">{{ question.question }}</div>
                    <div v-if="question.options" class="quiz-options">
                      <span v-for="(value, key) in question.options" :key="key">
                        {{ key }}. {{ value }}
                      </span>
                    </div>
                    <div class="quiz-answer">答案：{{ question.answer }}</div>
                    <div class="quiz-explanation">{{ question.explanation }}</div>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane v-if="resources.plan" label="学习路径" name="plan">
                <div class="plan-list">
                  <div
                    v-for="item in planItems"
                    :key="item.day"
                    class="plan-item"
                  >
                    <div class="plan-day">Day {{ item.day }} · {{ item.date }}</div>
                    <div class="plan-topics">{{ (item.topics || []).join('、') }}</div>
                    <p>{{ item.tasks }}</p>
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane v-if="multimodal" label="多模态脚本" name="multimodal">
                <div class="script-section">
                  <h3>PPT 大纲</h3>
                  <div v-for="slide in multimodal.pptOutline || []" :key="slide.page" class="script-item">
                    <strong>{{ slide.page }}. {{ slide.title }}</strong>
                    <ul>
                      <li v-for="point in slide.bullets || []" :key="point">{{ point }}</li>
                    </ul>
                    <p>{{ slide.visualSuggestion }}</p>
                  </div>

                  <h3>图像提示词</h3>
                  <div v-for="prompt in multimodal.imagePrompts || []" :key="prompt.title" class="script-item">
                    <strong>{{ prompt.title }}</strong>
                    <p>{{ prompt.prompt }}</p>
                  </div>

                  <h3>语音讲解稿</h3>
                  <article class="markdown-body" v-html="renderMarkdown(multimodal.audioScript || '')" />

                  <h3>微课分镜</h3>
                  <div v-for="scene in multimodal.videoStoryboard || []" :key="scene.scene" class="script-item">
                    <strong>镜头 {{ scene.scene }} · {{ scene.duration }}</strong>
                    <p>{{ scene.visual }}</p>
                    <p>{{ scene.voiceover }}</p>
                    <span>{{ scene.learnerAction }}</span>
                  </div>

                  <h3>实操案例</h3>
                  <div v-if="multimodal.practiceCase" class="script-item">
                    <strong>{{ multimodal.practiceCase.title }}</strong>
                    <p>{{ multimodal.practiceCase.objective }}</p>
                    <ol>
                      <li v-for="step in multimodal.practiceCase.steps || []" :key="step">{{ step }}</li>
                    </ol>
                    <p>{{ multimodal.practiceCase.deliverable }}</p>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
          <div v-else class="empty-result">
            <el-icon :size="28"><Collection /></el-icon>
            <p>配置资料后生成第一份个性化资源包</p>
          </div>
        </BaseCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute } from 'vue-router'
import { Collection, CopyDocument, Download } from '@element-plus/icons-vue'
import { loadReadyMaterials } from '@/api/material'
import { generateResourcePackageAsync, listResourcePackageTasks } from '@/api/ai'
import { useTaskStore } from '@/stores/task'
import { useMarkdown } from '@/composables/useMarkdown'
import BaseCard from '@/components/common/BaseCard.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'
import MindMapTree from '@/components/common/MindMapTree.vue'
import AgentTraceTimeline from '@/components/agent/AgentTraceTimeline.vue'

const taskStore = useTaskStore()
const { renderMarkdown } = useMarkdown()
const route = useRoute()

const materials = ref([])
const loadingMaterials = ref(false)
const loadingHistory = ref(false)
const generating = ref(false)
const progress = ref(0)
const progressMessage = ref('等待生成任务')
const progressStatus = ref('')
const packageResult = ref(null)
const historyTasks = ref([])
const activeTab = ref('summary')

const difficultyOptions = [
  { label: '基础', value: 'easy' },
  { label: '适中', value: 'medium' },
  { label: '进阶', value: 'hard' }
]

const form = reactive({
  materialId: null,
  goal: '',
  examDate: '',
  dailyHours: 2,
  difficulty: 'medium',
  includeSummary: true,
  includeMindMap: true,
  includeQuiz: true,
  includePlan: true,
  includeMultimodalScript: true
})

const resources = computed(() => packageResult.value?.resources || {})

const mindMapData = computed(() => {
  const content = resources.value.mindMap?.content
  if (!content) return null
  if (typeof content === 'object') return content
  try {
    return JSON.parse(content)
  } catch {
    return null
  }
})

const quizQuestions = computed(() => resources.value.quiz?.content?.questions || [])
const planItems = computed(() => resources.value.plan?.content?.plan || [])
const multimodal = computed(() => resources.value.multimodalScript?.content || null)
const agentDetails = computed(() => packageResult.value?.agentDetails || [])
const qualityChecks = computed(() => packageResult.value?.qualityChecks || null)
const presentationNotes = computed(() => packageResult.value?.presentationNotes || [])
const hasReviewInfo = computed(() => agentDetails.value.length > 0 || presentationNotes.value.length > 0 || qualityChecks.value)
const resourceSummary = computed(() => {
  const manifest = packageResult.value?.resourceManifest
  if (Array.isArray(manifest) && manifest.length > 0) {
    return manifest.map(item => ({
      key: item.key,
      label: item.title,
      icon: getResourceIcon(item.key),
      ready: Boolean(item.generated)
    }))
  }
  return [
    { key: 'summary', label: '讲解文档', icon: 'DocumentCopy', ready: Boolean(resources.value.summary) },
    { key: 'mindMap', label: '思维导图', icon: 'Share', ready: Boolean(mindMapData.value) },
    { key: 'quiz', label: '个性化题库', icon: 'EditPen', ready: Boolean(resources.value.quiz) },
    { key: 'plan', label: '学习路径', icon: 'Calendar', ready: Boolean(resources.value.plan) },
    { key: 'multimodalScript', label: '多模态脚本', icon: 'Film', ready: Boolean(multimodal.value) }
  ]
})

onMounted(() => {
  loadMaterials()
  loadHistory()
})

watch(() => route.query.materialId, () => {
  applyQueryMaterial()
})

async function loadMaterials() {
  loadingMaterials.value = true
  try {
    materials.value = await loadReadyMaterials()
    applyQueryMaterial()
  } catch {
    materials.value = []
  } finally {
    loadingMaterials.value = false
  }
}

function applyQueryMaterial() {
  const queryMaterialId = Number(route.query.materialId)
  if (queryMaterialId && materials.value.some(item => item.id === queryMaterialId)) {
    form.materialId = queryMaterialId
  }
}

async function loadHistory() {
  loadingHistory.value = true
  try {
    historyTasks.value = await listResourcePackageTasks(10)
  } catch {
    historyTasks.value = []
  } finally {
    loadingHistory.value = false
  }
}

async function handleGenerate() {
  if (!form.materialId) {
    ElMessage.warning('请先选择学习资料')
    return
  }
  if (!hasSelectedResource()) {
    ElMessage.warning('至少选择一种资源类型')
    return
  }

  generating.value = true
  progress.value = 0
  progressStatus.value = ''
  progressMessage.value = '正在创建资源包任务...'
  packageResult.value = null

  try {
    const res = await generateResourcePackageAsync({ ...form })
    const taskId = res.taskId
    taskStore.watchTask(taskId, 'resource_package', {
      onProgress(value, message) {
        progress.value = value || 0
        progressMessage.value = message || '资源包生成中...'
      },
      onSuccess(result) {
        generating.value = false
        progress.value = 100
        progressStatus.value = 'success'
        progressMessage.value = '资源包生成完成'
        packageResult.value = result
        activeTab.value = firstAvailableTab()
        loadHistory()
        ElMessage.success('资源包生成完成')
      },
      onError(error) {
        generating.value = false
        progressStatus.value = 'exception'
        progressMessage.value = error || '资源包生成失败'
        ElMessage.error(progressMessage.value)
      }
    })
  } catch {
    generating.value = false
    progressStatus.value = 'exception'
    progressMessage.value = '创建任务失败'
    ElMessage.error('创建资源包任务失败')
  }
}

function hasSelectedResource() {
  return form.includeSummary || form.includeMindMap || form.includeQuiz || form.includePlan || form.includeMultimodalScript
}

function firstAvailableTab() {
  if (resources.value.summary) return 'summary'
  if (resources.value.mindMap) return 'mindMap'
  if (resources.value.quiz) return 'quiz'
  if (resources.value.plan) return 'plan'
  return 'multimodal'
}

function restorePackage(task) {
  if (task.status !== 'success') {
    ElMessage.info(task.errorMsg || task.message || '该资源包尚未生成成功')
    return
  }
  let result = task.result
  if (typeof result === 'string') {
    try {
      result = JSON.parse(result)
    } catch {
      ElMessage.warning('历史资源包数据格式异常')
      return
    }
  }
  packageResult.value = result
  progress.value = 100
  progressStatus.value = 'success'
  progressMessage.value = '已恢复历史资源包'
  activeTab.value = firstAvailableTab()
}

function parsePackageTitle(task) {
  let result = task.result
  if (typeof result === 'string') {
    try { result = JSON.parse(result) } catch { result = null }
  }
  return result?.materialName || task.message || '资源包任务'
}

function formatStatus(status) {
  const map = {
    pending: '等待中',
    running: '生成中',
    success: '已完成',
    failed: '失败',
    cancelled: '已取消'
  }
  return map[status] || status
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function exportMarkdown() {
  if (!packageResult.value) return
  downloadTextFile(buildPackageMarkdown(), `${buildExportFileName()}.md`, 'text/markdown;charset=utf-8')
  ElMessage.success('Markdown 资源包已导出')
}

function exportJson() {
  if (!packageResult.value) return
  const content = JSON.stringify(packageResult.value, null, 2)
  downloadTextFile(content, `${buildExportFileName()}.json`, 'application/json;charset=utf-8')
  ElMessage.success('JSON 资源包已导出')
}

async function copyPresentationSummary() {
  if (!packageResult.value) return
  const content = buildPresentationSummary()
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('演示摘要已复制')
  } catch {
    downloadTextFile(content, `${buildExportFileName()}-演示摘要.txt`, 'text/plain;charset=utf-8')
    ElMessage.success('浏览器未开放剪贴板权限，已导出演示摘要')
  }
}

function buildExportFileName() {
  const name = packageResult.value?.materialName || 'resource-package'
  return `资源包-${name}`.replace(/[\\/:*?"<>|]/g, '-').slice(0, 80)
}

function downloadTextFile(content, fileName, type) {
  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(url), 0)
}

function buildPackageMarkdown() {
  const result = packageResult.value
  const lines = [
    `# ${result.materialName || '个性化学习资源包'}`,
    '',
    '## 资源包信息',
    '',
    `- 学习资料：${result.materialName || '-'}`,
    `- 学习目标：${result.goal || '-'}`,
    `- 难度：${formatDifficulty(result.difficulty)}`,
    `- 截止日期：${result.examDate || '-'}`,
    `- 每日学习时长：${result.dailyHours || '-'} 小时`,
    '',
    '## 多智能体协作链路',
    '',
    ...buildAgentMarkdownLines(),
    '',
    '## 资源包清单',
    ''
  ]

  resourceSummary.value.forEach(item => {
    lines.push(`- ${item.ready ? '[x]' : '[ ]'} ${item.label}`)
  })
  lines.push('')
  appendReviewMarkdown(lines)
  appendSummaryMarkdown(lines)
  appendMindMapMarkdown(lines)
  appendQuizMarkdown(lines)
  appendPlanMarkdown(lines)
  appendMultimodalMarkdown(lines)

  return lines.join('\n')
}

function buildAgentMarkdownLines() {
  if (agentDetails.value.length > 0) {
    return agentDetails.value.map(agent => `- **${agent.name}**：${agent.role}（输出：${agent.output}）`)
  }
  return (packageResult.value?.agents || []).map(agent => `- ${agent}`)
}

function appendReviewMarkdown(lines) {
  if (!hasReviewInfo.value) return
  lines.push('## 评审说明', '')
  if (presentationNotes.value.length > 0) {
    lines.push('### 演示亮点', '')
    presentationNotes.value.forEach(note => lines.push(`- ${note}`))
    lines.push('')
  }
  if (qualityChecks.value) {
    lines.push('### 质量与安全说明', '')
    lines.push(`- 资料状态：${qualityChecks.value.materialReady ? '已校验' : '未校验'}`)
    lines.push(`- 生成资源数：${qualityChecks.value.resourceCount || resourceSummary.value.filter(item => item.ready).length}`)
    lines.push(`- 多模态模式：${qualityChecks.value.multimodalMode || '-'}`)
    lines.push(`- 内容依据：${qualityChecks.value.grounding || '-'}`)
    lines.push(`- 安全说明：${qualityChecks.value.safety || '-'}`, '')
  }
}

function buildPresentationSummary() {
  const result = packageResult.value
  const generatedResources = resourceSummary.value
    .filter(item => item.ready)
    .map(item => item.label)
    .join('、')
  const notes = presentationNotes.value.length > 0
    ? presentationNotes.value.map(note => `- ${note}`).join('\n')
    : '- 支持从单份学习资料生成个性化资源包，并可导出用于初赛材料。'
  return [
    `资源包：${result.materialName || '-'}`,
    `学习目标：${result.goal || '-'}`,
    `已生成资源：${generatedResources || '-'}`,
    '',
    '演示亮点：',
    notes,
    '',
    `质量说明：${qualityChecks.value?.grounding || '基于用户学习资料生成'}`
  ].join('\n')
}

function appendSummaryMarkdown(lines) {
  if (!resources.value.summary?.content) return
  lines.push('## 讲解文档', '', resources.value.summary.content, '')
}

function appendMindMapMarkdown(lines) {
  if (!mindMapData.value) return
  lines.push('## 思维导图', '', '```json', JSON.stringify(mindMapData.value, null, 2), '```', '')
}

function appendQuizMarkdown(lines) {
  if (quizQuestions.value.length === 0) return
  lines.push('## 个性化题库', '')
  quizQuestions.value.forEach((question, index) => {
    lines.push(`### 题目 ${index + 1}`, '', question.question || '')
    if (question.options) {
      Object.entries(question.options).forEach(([key, value]) => {
        lines.push(`- ${key}. ${value}`)
      })
    }
    lines.push('', `**答案：** ${question.answer || '-'}`, '', `**解析：** ${question.explanation || '-'}`, '')
  })
}

function appendPlanMarkdown(lines) {
  if (planItems.value.length === 0) return
  lines.push('## 学习路径', '')
  planItems.value.forEach(item => {
    lines.push(`### Day ${item.day} ${item.date || ''}`, '')
    lines.push(`- 主题：${(item.topics || []).join('、') || '-'}`)
    lines.push(`- 任务：${item.tasks || '-'}`, '')
  })
}

function appendMultimodalMarkdown(lines) {
  if (!multimodal.value) return
  const data = multimodal.value
  lines.push('## 多模态脚本包', '', '### PPT 大纲', '')
  ;(data.pptOutline || []).forEach(slide => {
    lines.push(`#### ${slide.page}. ${slide.title}`, '')
    ;(slide.bullets || []).forEach(point => lines.push(`- ${point}`))
    lines.push(`- 视觉建议：${slide.visualSuggestion || '-'}`, '')
  })

  lines.push('### 图像提示词', '')
  ;(data.imagePrompts || []).forEach(prompt => {
    lines.push(`#### ${prompt.title}`, '', prompt.prompt || '-', '')
  })

  lines.push('### 语音讲解稿', '', data.audioScript || '-', '', '### 微课分镜', '')
  ;(data.videoStoryboard || []).forEach(scene => {
    lines.push(`#### 镜头 ${scene.scene} ${scene.duration || ''}`, '')
    lines.push(`- 画面：${scene.visual || '-'}`)
    lines.push(`- 旁白：${scene.voiceover || '-'}`)
    lines.push(`- 学习者动作：${scene.learnerAction || '-'}`, '')
  })

  if (data.practiceCase) {
    lines.push('### 实操案例', '', `#### ${data.practiceCase.title || '实践项目'}`, '')
    lines.push(`- 目标：${data.practiceCase.objective || '-'}`)
    ;(data.practiceCase.steps || []).forEach((step, index) => lines.push(`${index + 1}. ${step}`))
    lines.push(`- 交付物：${data.practiceCase.deliverable || '-'}`, '')
  }
}

function formatDifficulty(value) {
  const option = difficultyOptions.find(item => item.value === value)
  return option?.label || value || '-'
}

function getResourceIcon(key) {
  const map = {
    summary: 'DocumentCopy',
    mindMap: 'Share',
    quiz: 'EditPen',
    plan: 'Calendar',
    multimodalScript: 'Film'
  }
  return map[key] || 'Collection'
}
</script>

<style scoped>
.resource-workshop-page {
  width: 100%;
  max-width: var(--content-work-width);
}

.workshop-grid {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: var(--space-6);
  align-items: start;
}

.config-column {
  position: sticky;
  top: calc(var(--header-height) + var(--space-4));
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.config-card {
  min-width: 0;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: var(--space-3);
}

.resource-options {
  display: grid;
  gap: var(--space-2);
}

.generate-button {
  width: 100%;
  height: 44px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.history-item {
  width: 100%;
  padding: var(--space-3);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.history-item:hover {
  background: var(--surface-hover);
  border-color: var(--outline);
}

.history-title {
  color: var(--color-text-primary);
  font-size: var(--text-ui);
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-meta {
  display: flex;
  justify-content: space-between;
  gap: var(--space-2);
  margin-top: var(--space-1);
  color: var(--color-text-tertiary);
  font-size: var(--text-small);
}

.history-empty {
  padding: var(--space-4);
  color: var(--color-text-tertiary);
  font-size: var(--text-ui);
  text-align: center;
}

.result-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  min-width: 0;
}

.progress-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.progress-message {
  color: var(--color-text-secondary);
  font-size: var(--text-ui);
  margin: 0;
}

.empty-result {
  min-height: 180px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  color: var(--color-text-tertiary);
  text-align: center;
}

.empty-result p {
  margin: 0;
  font-size: var(--text-ui);
}

.package-result {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.package-meta {
  display: grid;
  grid-template-columns: 1fr 1.5fr;
  gap: var(--space-4);
  padding: var(--space-4);
  background: var(--surface-container-low);
  border-radius: var(--radius-md);
}

.package-meta strong {
  display: block;
  color: var(--color-text-primary);
  font-size: var(--text-ui);
  margin-top: var(--space-1);
}

.meta-label {
  color: var(--color-text-tertiary);
  font-size: var(--text-small);
}

.agent-chain {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.agent-chip {
  padding: 4px 10px;
  border-radius: var(--radius-md);
  background: var(--surface-container-low);
  color: var(--color-text-secondary);
  font-size: var(--text-small);
}

.package-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.package-overview {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--space-2);
}

.overview-item {
  min-height: 72px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: var(--space-3);
  border: 1px solid var(--outline);
  border-radius: var(--radius-md);
  background: var(--surface-container-low);
  color: var(--color-text-tertiary);
}

.overview-item.active {
  color: var(--color-primary);
  border-color: var(--color-primary);
  background: var(--bg-tag-green);
}

.overview-item span {
  color: var(--color-text-primary);
  font-size: var(--text-small);
  font-weight: 600;
}

.overview-item strong {
  color: inherit;
  font-size: var(--text-small);
  font-weight: 500;
}

.review-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--outline);
  border-radius: var(--radius-md);
  background: var(--surface-container-low);
}

.review-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.review-label {
  color: var(--color-text-tertiary);
  font-size: var(--text-small);
  font-weight: 600;
}

.review-section ul {
  margin: 0;
  padding-left: 18px;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.agent-detail-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
}

.agent-detail-item {
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--surface-card);
}

.agent-detail-item strong {
  color: var(--color-text-primary);
  font-size: var(--text-ui);
}

.agent-detail-item p {
  margin: var(--space-1) 0;
  color: var(--color-text-secondary);
  font-size: var(--text-small);
  line-height: 1.6;
}

.agent-detail-item span {
  color: var(--color-text-tertiary);
  font-size: var(--text-small);
}

.quality-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-2);
}

.quality-grid div {
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--surface-card);
}

.quality-grid span {
  display: block;
  color: var(--color-text-tertiary);
  font-size: var(--text-small);
}

.quality-grid strong {
  display: block;
  margin-top: 2px;
  color: var(--color-text-primary);
  font-size: var(--text-ui);
}

.review-section > p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--text-small);
  line-height: 1.7;
}

.result-tabs :deep(.el-tabs__content) {
  padding-top: var(--space-4);
}

.markdown-body {
  color: var(--color-text-primary);
  line-height: 1.75;
}

.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: var(--space-5) 0 var(--space-3);
}

.quiz-list,
.plan-list,
.script-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.quiz-item,
.plan-item,
.script-item {
  padding: var(--space-4);
  border: 1px solid var(--outline);
  border-radius: var(--radius-md);
  background: var(--surface-container-low);
}

.quiz-title,
.plan-day,
.script-item strong {
  color: var(--color-text-primary);
  font-weight: 600;
}

.quiz-options {
  display: grid;
  gap: var(--space-1);
  margin: var(--space-3) 0;
  color: var(--color-text-secondary);
}

.quiz-answer {
  color: var(--color-primary);
  font-weight: 600;
  margin-bottom: var(--space-2);
}

.quiz-explanation,
.plan-item p,
.script-item p,
.script-item span {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.plan-topics {
  margin: var(--space-2) 0;
  color: var(--color-primary);
  font-weight: 500;
}

.script-section h3 {
  font-size: var(--text-heading-3);
  color: var(--color-text-primary);
  margin: var(--space-2) 0 0;
}

@media (max-width: 1023px) {
  .workshop-grid {
    grid-template-columns: 1fr;
  }

  .config-card {
    position: static;
  }

  .config-column {
    position: static;
  }

  .package-overview {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .agent-detail-list,
  .quality-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .form-row,
  .package-meta {
    grid-template-columns: 1fr;
  }

  .package-overview {
    grid-template-columns: 1fr;
  }
}
</style>
