<template>
  <div class="resource-workshop-page">
    <BasePageHeader
      title="资源工坊"
      description="基于单份学习资料生成讲解文档、导图、题库、学习路径和多模态资源"
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
                <el-checkbox v-model="form.includeMultimodalScript">多模态资源</el-checkbox>
              </div>
            </el-form-item>

            <!-- 多模态拓展（spec §10.3） -->
            <el-form-item v-if="form.includeMultimodalScript" label="多模态拓展">
              <div class="multimodal-options">
                <div class="multimodal-toggle-row">
                  <el-checkbox
                    v-model="form.includePodcastAudio"
                    :disabled="!capabilities?.ttsEnabled"
                  >
                    播客音频
                    <span v-if="!capabilities?.ttsEnabled" class="opt-hint">（讯飞 TTS 未启用）</span>
                  </el-checkbox>
                  <el-checkbox
                    v-model="form.includeKnowledgeImages"
                    :disabled="!capabilities?.imageEnabled"
                  >
                    知识配图
                    <span v-if="!capabilities?.imageEnabled" class="opt-hint">（讯飞文生图未启用）</span>
                  </el-checkbox>
                </div>

                <div v-if="form.includePodcastAudio" class="multimodal-field-row">
                  <label class="field-label">发音人</label>
                  <el-select v-model="form.ttsVoice" size="small" placeholder="默认发音人">
                    <el-option
                      v-for="voice in capabilities?.voices || []"
                      :key="voice.id"
                      :label="formatVoiceLabel(voice)"
                      :value="voice.id"
                    />
                  </el-select>
                  <p v-if="selectedVoiceNeedsPermission" class="voice-permission-hint">
                    该发音人为讯飞超自然发音人，需在讯飞控制台「语音合成 → 发音人管理」中单独开通权限，
                    未开通时合成会返回错误码 10043。
                  </p>
                </div>

                <div v-if="form.includePodcastAudio" class="multimodal-field-row">
                  <label class="field-label">播客风格</label>
                  <el-segmented
                    v-model="form.podcastStyle"
                    :options="podcastStyleOptions"
                    size="small"
                  />
                </div>

                <div v-if="form.includeKnowledgeImages" class="multimodal-field-row">
                  <label class="field-label">配图风格</label>
                  <el-segmented
                    v-model="form.imageStyle"
                    :options="imageStyleOptions"
                    size="small"
                  />
                </div>

                <div v-if="form.includeKnowledgeImages" class="multimodal-field-row">
                  <label class="field-label">配图数量</label>
                  <el-input-number
                    v-model="form.imageCount"
                    :min="1"
                    :max="capabilities?.maxImageCount || 4"
                    size="small"
                    controls-position="right"
                  />
                </div>
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
              <el-button :icon="CopyDocument" size="small" @click="copyPresentationSummary">复制资源摘要</el-button>
              <el-button :icon="Download" size="small" @click="exportMarkdown">导出 Markdown</el-button>
              <el-button :icon="Download" size="small" @click="exportPdf">导出 PDF</el-button>
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
                <span class="review-label">学习建议</span>
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
                <span class="review-label">内容校验</span>
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
                <div v-if="qualityChecks.multimodalFailures" class="multimodal-failures">
                  <span class="failure-title">
                    多模态资产失败 {{ qualityChecks.multimodalFailures.totalFailed }} 个
                  </span>
                  <ul>
                    <li v-for="detail in qualityChecks.multimodalFailures.details || []" :key="detail">
                      {{ detail }}
                    </li>
                  </ul>
                  <p class="failure-suggestion">{{ qualityChecks.multimodalFailures.suggestion }}</p>
                </div>
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

              <el-tab-pane
                v-if="hasMultimodalAssets"
                label="多模态资源"
                name="assets"
              >
                <ResourceAssetPanel
                  :assets="packageAssets"
                  @retry-success="onAssetRetrySuccess"
                  @retry-failed="onAssetRetryFailed"
                />
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
import {
  generateResourcePackageAsync,
  getResourceAssetCapabilities,
  listResourcePackageAssets,
  listResourcePackageTasks
} from '@/api/ai'
import { useTaskStore } from '@/stores/task'
import { useMarkdown } from '@/composables/useMarkdown'
import BaseCard from '@/components/common/BaseCard.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'
import MindMapTree from '@/components/common/MindMapTree.vue'
import AgentTraceTimeline from '@/components/agent/AgentTraceTimeline.vue'
import ResourceAssetPanel from '@/components/resource-assets/ResourceAssetPanel.vue'

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
const capabilities = ref(null)

/**
 * 格式化发音人显示文案：在性别后追加「需开通」标识（仅超自然发音人）。
 * 后端 VoiceOption.needsPermission 标识该发音人是否需要在讯飞控制台单独开通权限。
 */
function formatVoiceLabel(voice) {
  if (!voice) return ''
  const genderText = voice.gender === 'female' ? '女' : voice.gender === 'male' ? '男' : '中性'
  const suffix = voice.needsPermission ? ' · 需开通' : ''
  return `${voice.name}（${genderText}）${suffix}`
}

/** 当前选中的发音人是否需要开通权限（用于显示提示） */
const selectedVoiceNeedsPermission = computed(() => {
  if (!form.ttsVoice) return false
  const voices = capabilities.value?.voices || []
  const selected = voices.find(v => v.id === form.ttsVoice)
  return selected?.needsPermission === true
})

const difficultyOptions = [
  { label: '基础', value: 'easy' },
  { label: '适中', value: 'medium' },
  { label: '进阶', value: 'hard' }
]

const podcastStyleOptions = [
  { label: '教师讲解', value: 'teacher' },
  { label: '复习串讲', value: 'review' },
  { label: '故事化', value: 'story' }
]

const imageStyleOptions = [
  { label: '教育插画', value: 'clean_edu' },
  { label: '结构图解', value: 'diagram' },
  { label: '黑板手绘', value: 'blackboard' },
  { label: '等距风格', value: 'isometric' }
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
  includeMultimodalScript: true,
  // 多模态拓展字段（spec §10.3）
  includePodcastAudio: false,
  podcastStyle: 'teacher',
  ttsVoice: '',
  includeKnowledgeImages: false,
  imageCount: 1,
  imageStyle: 'clean_edu'
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
const agentDetails = computed(() => packageResult.value?.agentDetails || [])
const qualityChecks = computed(() => packageResult.value?.qualityChecks || null)
const presentationNotes = computed(() => packageResult.value?.presentationNotes || [])
const hasReviewInfo = computed(() => agentDetails.value.length > 0 || presentationNotes.value.length > 0 || qualityChecks.value)
const packageAssets = computed(() => packageResult.value?.assets || [])
const hasMultimodalAssets = computed(() => Array.isArray(packageAssets.value) && packageAssets.value.length > 0)
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
    { key: 'assets', label: '多模态资源', icon: 'Film', ready: Boolean(hasMultimodalAssets.value) }
  ]
})

onMounted(() => {
  loadMaterials()
  loadHistory()
  loadCapabilities()
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

async function loadCapabilities() {
  try {
    capabilities.value = await getResourceAssetCapabilities()
    // 加载完成后回填默认值（仅当用户未自定义时）
    if (capabilities.value?.defaultVoice && !form.ttsVoice) {
      form.ttsVoice = capabilities.value.defaultVoice
    }
    if (capabilities.value?.defaultImageStyle && form.imageStyle === 'clean_edu') {
      form.imageStyle = capabilities.value.defaultImageStyle
    }
    // 能力未启用时，关闭对应开关
    if (!capabilities.value?.ttsEnabled) {
      form.includePodcastAudio = false
    }
    if (!capabilities.value?.imageEnabled) {
      form.includeKnowledgeImages = false
    }
  } catch {
    // 静默失败，仅记录 null（前端会按未启用处理）
    capabilities.value = null
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
  if (hasMultimodalAssets.value) return 'assets'
  return 'summary'
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
  // 历史任务可能缺少 assets 字段，按 packageId 补齐（spec §10.3）
  ensurePackageAssets(result)
}

async function ensurePackageAssets(result) {
  if (!result) return
  if (Array.isArray(result.assets) && result.assets.length > 0) return
  const packageId = result.packageId
  if (!packageId) return
  try {
    const assets = await listResourcePackageAssets(packageId)
    if (Array.isArray(assets) && assets.length > 0) {
      // 触发响应式更新
      packageResult.value = { ...packageResult.value, assets }
    }
  } catch {
    // 静默失败，不影响主资源包恢复
  }
}

function onAssetRetrySuccess(updatedAsset) {
  if (!packageResult.value || !Array.isArray(packageResult.value.assets)) return
  if (!updatedAsset?.assetId) return
  const assets = packageResult.value.assets.map(item =>
    item.assetId === updatedAsset.assetId ? updatedAsset : item
  )
  packageResult.value = { ...packageResult.value, assets }
}

function onAssetRetryFailed() {
  // 失败提示由子组件自行处理；这里保留接口以便后续扩展
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

function exportPdf() {
  if (!packageResult.value) return
  const printWindow = window.open('', '_blank')
  if (!printWindow) {
    ElMessage.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  printWindow.opener = null
  printWindow.document.write(buildPackagePdfHtml())
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
  }, 500)
  ElMessage.success('已打开 PDF 打印页面，可选择“另存为 PDF”')
}

async function copyPresentationSummary() {
  if (!packageResult.value) return
  const content = buildPresentationSummary()
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('资源摘要已复制')
  } catch {
    downloadTextFile(content, `${buildExportFileName()}-资源摘要.txt`, 'text/plain;charset=utf-8')
    ElMessage.success('浏览器未开放剪贴板权限，已导出资源摘要')
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

function buildPackagePdfHtml() {
  const result = packageResult.value || {}
  const imageAssets = packageAssets.value
    .filter(asset => asset.assetType === 'image' && asset.status === 'success' && asset.previewUrl)
    .map(appendAssetAccessToken)
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <title>${escapeHtml(buildExportFileName())}</title>
  <style>
    * { box-sizing: border-box; }
    body {
      margin: 0;
      padding: 32px;
      color: #1f2937;
      background: #f6f7f9;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Microsoft YaHei", sans-serif;
      line-height: 1.75;
    }
    .page {
      max-width: 920px;
      margin: 0 auto;
      padding: 40px;
      background: #fff;
      border: 1px solid #e5e7eb;
    }
    h1 { margin: 0 0 12px; font-size: 30px; line-height: 1.25; color: #111827; }
    h2 { margin: 34px 0 14px; padding-bottom: 8px; border-bottom: 1px solid #e5e7eb; font-size: 20px; color: #111827; }
    h3 { margin: 24px 0 8px; font-size: 16px; color: #111827; }
    p { margin: 8px 0; }
    ul, ol { padding-left: 22px; }
    blockquote { margin: 12px 0; padding: 8px 14px; border-left: 4px solid #94a3b8; background: #f8fafc; color: #475569; }
    code, pre { background: #f3f4f6; border-radius: 4px; }
    pre { padding: 12px; white-space: pre-wrap; word-break: break-word; }
    table { width: 100%; border-collapse: collapse; margin: 12px 0; }
    th, td { border: 1px solid #e5e7eb; padding: 8px; text-align: left; }
    .meta-grid, .overview-grid, .quality-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px;
      margin: 20px 0;
    }
    .meta-card, .overview-card, .quality-card, .agent-card, .question-card, .plan-card {
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      padding: 12px 14px;
      background: #f9fafb;
      break-inside: avoid;
    }
    .label { display: block; color: #6b7280; font-size: 12px; }
    .value { display: block; margin-top: 4px; color: #111827; font-weight: 650; }
    .overview-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
    .overview-card.ready { border-color: #16a34a; background: #f0fdf4; }
    .agent-list, .question-list, .plan-list, .image-grid { display: grid; gap: 12px; }
    .agent-list { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .image-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .image-card img {
      width: 100%;
      aspect-ratio: 1 / 1;
      object-fit: cover;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      display: block;
      background: #f3f4f6;
    }
    .image-caption { margin-top: 8px; color: #4b5563; font-size: 13px; }
    .mindmap-tree ul { margin: 4px 0 4px 18px; padding-left: 16px; border-left: 1px dashed #d1d5db; }
    .footnote { margin-top: 32px; color: #6b7280; font-size: 12px; }
    @media print {
      body { padding: 0; background: #fff; }
      .page { max-width: none; border: 0; padding: 0; }
      h2 { break-after: avoid; }
      .meta-card, .overview-card, .quality-card, .agent-card, .question-card, .plan-card, .image-card { break-inside: avoid; }
    }
  </style>
</head>
<body>
  <main class="page">
    <h1>${escapeHtml(result.materialName || '个性化学习资源包')}</h1>
    <p>${escapeHtml(result.goal || '基于学习资料自动生成的个性化资源组合')}</p>
    <section class="meta-grid">
      ${buildPdfMetaCard('学习资料', result.materialName || '-')}
      ${buildPdfMetaCard('学习目标', result.goal || '-')}
      ${buildPdfMetaCard('难度', formatDifficulty(result.difficulty))}
      ${buildPdfMetaCard('学习节奏', `${result.examDate || '-'} / 每日 ${result.dailyHours || '-'} 小时`)}
    </section>
    <h2>资源清单</h2>
    <section class="overview-grid">${resourceSummary.value.map(item => `
      <div class="overview-card ${item.ready ? 'ready' : ''}">
        <span class="label">${escapeHtml(item.label)}</span>
        <span class="value">${item.ready ? '已生成' : '未生成'}</span>
      </div>`).join('')}
    </section>
    ${buildPdfReviewSection()}
    ${buildPdfImageSection(imageAssets)}
    ${buildPdfSummarySection()}
    ${buildPdfMindMapSection()}
    ${buildPdfQuizSection()}
    ${buildPdfPlanSection()}
    <p class="footnote">本资源包由 AI Study Assistant 生成，供学习参考；音频资源未纳入 PDF。</p>
  </main>
</body>
</html>`
}

function buildPdfMetaCard(label, value) {
  return `<div class="meta-card"><span class="label">${escapeHtml(label)}</span><span class="value">${escapeHtml(value)}</span></div>`
}

function buildPdfReviewSection() {
  const blocks = []
  if (agentDetails.value.length > 0) {
    blocks.push(`<h2>多智能体协作</h2><section class="agent-list">${agentDetails.value.map(agent => `
      <div class="agent-card">
        <strong>${escapeHtml(agent.name || '-')}</strong>
        <p>${escapeHtml(agent.role || '')}</p>
        <span class="label">输出：${escapeHtml(agent.output || '-')}</span>
      </div>`).join('')}</section>`)
  }
  if (presentationNotes.value.length > 0) {
    blocks.push(`<h2>学习亮点</h2><ul>${presentationNotes.value.map(note => `<li>${escapeHtml(note)}</li>`).join('')}</ul>`)
  }
  if (qualityChecks.value) {
    blocks.push(`<h2>质量与安全说明</h2><section class="quality-grid">
      ${buildPdfMetaCard('资料状态', qualityChecks.value.materialReady ? '已校验' : '未校验')}
      ${buildPdfMetaCard('生成资源数', qualityChecks.value.resourceCount || resourceSummary.value.filter(item => item.ready).length)}
      ${buildPdfMetaCard('多模态模式', qualityChecks.value.multimodalMode || '-')}
      ${buildPdfMetaCard('内容依据', qualityChecks.value.grounding || '-')}
    </section><p>${escapeHtml(qualityChecks.value.safety || '')}</p>`)
  }
  return blocks.join('')
}

function buildPdfImageSection(imageAssets) {
  if (imageAssets.length === 0) return ''
  return `<h2>知识配图</h2><section class="image-grid">${imageAssets.map(asset => `
    <figure class="image-card">
      <img src="${escapeHtml(asset.previewUrl)}" alt="${escapeHtml(asset.promptSummary || '知识配图')}" />
      <figcaption class="image-caption">${escapeHtml(asset.assetRole === 'cover' ? '资源包封面' : (asset.promptSummary || '知识点图解'))}</figcaption>
    </figure>`).join('')}</section>`
}

function buildPdfSummarySection() {
  if (!resources.value.summary?.content) return ''
  return `<h2>讲解文档</h2><article>${renderMarkdown(resources.value.summary.content)}</article>`
}

function buildPdfMindMapSection() {
  if (!mindMapData.value) return ''
  return `<h2>思维导图</h2><div class="mindmap-tree">${buildMindMapHtml(mindMapData.value)}</div>`
}

function buildPdfQuizSection() {
  if (quizQuestions.value.length === 0) return ''
  return `<h2>个性化题库</h2><section class="question-list">${quizQuestions.value.map((question, index) => `
    <div class="question-card">
      <h3>题目 ${index + 1}</h3>
      <p>${escapeHtml(question.question || '')}</p>
      ${question.options ? `<ul>${Object.entries(question.options).map(([key, value]) => `<li>${escapeHtml(key)}. ${escapeHtml(value)}</li>`).join('')}</ul>` : ''}
      <p><strong>答案：</strong>${escapeHtml(question.answer || '-')}</p>
      <p><strong>解析：</strong>${escapeHtml(question.explanation || '-')}</p>
    </div>`).join('')}</section>`
}

function buildPdfPlanSection() {
  if (planItems.value.length === 0) return ''
  return `<h2>学习路径</h2><section class="plan-list">${planItems.value.map(item => `
    <div class="plan-card">
      <h3>Day ${escapeHtml(item.day || '-')} ${escapeHtml(item.date || '')}</h3>
      <p><strong>主题：</strong>${escapeHtml((item.topics || []).join('、') || '-')}</p>
      <p>${escapeHtml(item.tasks || '-')}</p>
    </div>`).join('')}</section>`
}

function buildMindMapHtml(node) {
  if (!node) return ''
  const text = node.topic || node.title || node.name || node.label || node.text || node.data?.text || '知识点'
  const children = node.children || node.data?.children || []
  return `<ul><li><strong>${escapeHtml(text)}</strong>${Array.isArray(children) && children.length > 0 ? children.map(child => buildMindMapHtml(child)).join('') : ''}</li></ul>`
}

function appendAssetAccessToken(asset) {
  const token = localStorage.getItem('token')
  if (!token || !asset.previewUrl) return asset
  const sep = asset.previewUrl.includes('?') ? '&' : '?'
  return {
    ...asset,
    previewUrl: `${asset.previewUrl}${sep}access_token=${encodeURIComponent(token)}`
  }
}

function escapeHtml(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
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
  lines.push('## 学习资源说明', '')
  if (presentationNotes.value.length > 0) {
    lines.push('### 学习建议', '')
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
    : '- 支持从单份学习资料生成个性化资源包，并可导出后继续复习。'
  return [
    `资源包：${result.materialName || '-'}`,
    `学习目标：${result.goal || '-'}`,
    `已生成资源：${generatedResources || '-'}`,
    '',
    '学习建议：',
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
    assets: 'Film'
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
  grid-template-columns: minmax(430px, 480px) minmax(0, 1fr);
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

.config-card :deep(.el-select),
.config-card :deep(.el-date-editor),
.config-card :deep(.el-input),
.config-card :deep(.el-input-number) {
  width: 100%;
}

.form-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: var(--space-3);
}

.resource-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-2);
}

.resource-options :deep(.el-checkbox) {
  margin-right: 0;
  white-space: normal;
}

.config-card :deep(.el-segmented) {
  max-width: 100%;
  overflow-x: auto;
}

.multimodal-options {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid var(--outline);
  border-radius: var(--radius-md);
  background: var(--surface-container-low);
}

.multimodal-toggle-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-2);
}

.multimodal-toggle-row :deep(.el-checkbox) {
  margin-right: 0;
  white-space: normal;
}

.multimodal-field-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  align-items: center;
  gap: var(--space-2);
}

.field-label {
  color: var(--color-text-secondary);
  font-size: var(--text-small);
}

.opt-hint {
  margin-left: 4px;
  color: var(--color-text-tertiary);
  font-size: var(--text-micro);
}

.voice-permission-hint {
  grid-column: 1 / -1;
  margin: 0;
  padding: var(--space-1) var(--space-2);
  color: var(--color-warning);
  font-size: var(--text-micro);
  line-height: 1.5;
  background: var(--color-warning-bg);
  border-radius: var(--radius-sm);
}

.multimodal-failures {
  margin-top: var(--space-2);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  background: var(--color-warning-bg);
  border: 1px solid var(--color-warning);
}

.failure-title {
  display: block;
  color: var(--color-warning-on);
  font-size: var(--text-small);
  font-weight: 600;
}

.multimodal-failures ul {
  margin: var(--space-2) 0 0;
  padding-left: 18px;
  color: var(--color-text-secondary);
  font-size: var(--text-small);
  line-height: 1.7;
}

.failure-suggestion {
  margin: var(--space-2) 0 0;
  color: var(--color-warning-on);
  font-size: var(--text-small);
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
.plan-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.quiz-item,
.plan-item {
  padding: var(--space-4);
  border: 1px solid var(--outline);
  border-radius: var(--radius-md);
  background: var(--surface-container-low);
}

.quiz-title,
.plan-day {
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
.plan-item p {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.plan-topics {
  margin: var(--space-2) 0;
  color: var(--color-primary);
  font-weight: 500;
}

@media (max-width: 1180px) {
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

  .resource-options {
    grid-template-columns: 1fr;
  }

  .package-overview {
    grid-template-columns: 1fr;
  }

  .multimodal-toggle-row {
    grid-template-columns: 1fr;
  }

  .multimodal-field-row {
    grid-template-columns: 1fr;
  }

  .multimodal-field-row .field-label {
    margin-bottom: 2px;
  }
}
</style>
