<template>
  <div class="qa-page">
    <div class="page-header">
      <h2>AI 问答</h2>
      <p>基于学习资料提问或自由对话，AI 学习助手为你解答</p>
    </div>

    <div class="qa-layout">
      <!-- Main Area -->
      <div class="qa-main">
        <!-- Question Input -->
        <div class="qa-input-card">
          <div class="qa-input-icon">
            <el-icon :size="22"><MagicStick /></el-icon>
          </div>
          <div class="qa-input-body">
            <el-input
              v-model="inputQuestion"
              type="textarea"
              :rows="2"
              placeholder="输入你的问题，AI 学习助手为你解答..."
              resize="none"
              :disabled="loading"
              @keydown.enter.exact.prevent="handleAsk"
            />
            <div class="qa-input-footer">
              <span class="qa-input-hint">
                <template v-if="selectedMaterial">基于：{{ selectedMaterial.originalName }}</template>
                <template v-else>通用对话模式 — 可直接提问任何学习问题</template>
              </span>
              <button
                class="qa-ask-btn"
                :disabled="!canAsk || loading"
                @click="handleAsk"
              >
                <el-icon v-if="!loading" :size="16"><Promotion /></el-icon>
                <el-icon v-else :size="16" class="spinning"><Loading /></el-icon>
                {{ loading ? '生成中' : '提问' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Loading State -->
        <div v-if="loading" class="qa-loading">
          <BaseCard>
            <!-- RAG 模式：三步进度 -->
            <div v-if="selectedMaterial" class="qa-progress-steps">
              <div class="progress-step" :class="{ active: loadingStep >= 1, done: loadingStep > 1 }">
                <div class="step-dot"><el-icon v-if="loadingStep > 1" :size="14"><Check /></el-icon><span v-else>1</span></div>
                <span class="step-label">读取文档内容</span>
              </div>
              <div class="step-line" :class="{ active: loadingStep >= 2 }" />
              <div class="progress-step" :class="{ active: loadingStep >= 2, done: loadingStep > 2 }">
                <div class="step-dot"><el-icon v-if="loadingStep > 2" :size="14"><Check /></el-icon><span v-else>2</span></div>
                <span class="step-label">检索相关知识</span>
              </div>
              <div class="step-line" :class="{ active: loadingStep >= 3 }" />
              <div class="progress-step" :class="{ active: loadingStep >= 3 }">
                <div class="step-dot"><span>3</span></div>
                <span class="step-label">AI 生成答案</span>
              </div>
            </div>
            <!-- 通用模式：单步加载 -->
            <div v-else class="qa-loading-simple">
              <el-icon :size="24" class="spinning"><Loading /></el-icon>
              <span>AI 正在生成答案...</span>
            </div>
          </BaseCard>
        </div>

        <!-- Empty -->
        <div v-else-if="!currentAnswer" class="qa-empty">
          <AppEmpty
            icon="ChatDotRound"
            title="输入问题开始AI问答"
            :description="selectedMaterial ? 'AI 将基于已选资料为你生成答案' : '通用对话模式，可以提问任何学习问题'"
          />
          <!-- Suggested Questions -->
          <div class="suggested-questions">
            <div class="suggested-label">{{ selectedMaterial ? '推荐问题' : '试试这些问题' }}</div>
            <div class="question-chips">
              <button
                v-for="q in suggestedQuestions"
                :key="q"
                class="question-chip"
                @click="askQuestion(q)"
              >{{ q }}</button>
            </div>
          </div>
        </div>

        <!-- Answer -->
        <template v-else>
          <!-- Synthesis Answer Card -->
          <BaseCard class="answer-card">
            <template #header>
              <div class="answer-header">
                <el-icon :size="18"><MagicStick /></el-icon>
                <span>AI 综合答案</span>
              </div>
            </template>
            <template #header-action>
              <div class="answer-actions">
                <el-button text size="small" @click="copyAnswer"><el-icon :size="14"><CopyDocument /></el-icon> 复制</el-button>
                <el-button text size="small" @click="handleAsk"><el-icon :size="14"><Refresh /></el-icon> 重新生成</el-button>
              </div>
            </template>
            <div class="answer-body markdown-body" v-html="renderMarkdown(currentAnswer)" />
          </BaseCard>

          <!-- Vector Reference Data -->
          <div v-if="currentSources.length > 0" class="reference-section">
            <h3 class="reference-title">
              <el-icon :size="16"><Collection /></el-icon>
              向量参考数据
            </h3>
            <div class="reference-grid">
              <div v-for="(source, i) in currentSources" :key="i" class="reference-card">
                <div class="ref-header">
                  <el-icon :size="14"><Document /></el-icon>
                  <span class="ref-doc truncate">{{ source.docName || '切片 ' + (source.chunkIndex || i + 1) }}</span>
                </div>
                <div class="ref-meta">
                  <span class="ref-page">{{ source.page ? `第 ${source.page} 页` : '' }}</span>
                  <span class="ref-score">匹配 {{ ((source.score || 0) * 100).toFixed(0) }}%</span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- Sidebar -->
      <div class="qa-sidebar">
        <!-- Material Selector -->
        <BaseCard title="选择资料（可选）">
          <el-select
            v-model="selectedMaterialId"
            placeholder="不选则为通用对话"
            filterable
            clearable
            style="width:100%;"
            @change="onMaterialChange"
          >
            <el-option
              v-for="item in materialList"
              :key="item.id"
              :label="item.originalName"
              :value="item.id"
            />
          </el-select>
          <div v-if="selectedMaterial" class="sb-material-info">
            <div class="sb-material-icon">
              <el-icon :size="16"><Document /></el-icon>
            </div>
            <div class="sb-material-name truncate">{{ selectedMaterial.originalName }}</div>
          </div>
        </BaseCard>

        <!-- History -->
        <BaseCard title="对话历史" padding="sm">
          <div class="history-list">
            <div
              v-for="(item, i) in historyList"
              :key="i"
              class="history-item"
              @click="askQuestion(item)"
            >
              <el-icon :size="14"><ChatDotRound /></el-icon>
              <span class="history-text truncate">{{ item }}</span>
            </div>
            <div v-if="historyList.length === 0" class="history-empty-text">
              暂无历史问题
            </div>
          </div>
        </BaseCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, Promotion, Loading, Check, Refresh, CopyDocument, Collection, Document, ChatDotRound } from '@element-plus/icons-vue'
import { useMarkdown } from '@/composables/useMarkdown'
import { askQuestion as apiAsk } from '@/api/ai'
import { loadAvailableMaterials } from '@/api/material'
import BaseCard from '@/components/common/BaseCard.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'

const route = useRoute()
const { renderMarkdown } = useMarkdown()

const materialList = ref([])
const selectedMaterialId = ref('')
const inputQuestion = ref('')
const currentAnswer = ref('')
const currentSources = ref([])
const loading = ref(false)
const loadingStep = ref(1)
let loadingTimer = null

const historyList = ref([])

const suggestedQuestions = computed(() => {
  if (selectedMaterial.value) {
    return [
      '这份资料的核心观点是什么？',
      '请帮我梳理一下知识结构',
      '这个概念怎么理解？',
      '有哪些需要注意的关键点？'
    ]
  }
  return [
    '如何高效学习一门新学科？',
    '什么是费曼学习法？',
    '如何制定合理的学习计划？',
    '怎样提高记忆效率？'
  ]
})

const selectedMaterial = computed(() =>
  materialList.value.find(m => m.id === selectedMaterialId.value) || null
)

const canAsk = computed(() =>
  inputQuestion.value.trim() && !loading.value
)

function onMaterialChange() {
  currentAnswer.value = ''
  currentSources.value = []
}

async function handleAsk() {
  if (!canAsk.value) return
  await performAsk(inputQuestion.value.trim())
}

function askQuestion(q) {
  inputQuestion.value = q
  performAsk(q)
}

async function performAsk(question) {
  loading.value = true
  loadingStep.value = 1
  currentAnswer.value = ''
  currentSources.value = []

  // Simulate progress steps (the API doesn't provide streaming progress)
  loadingTimer = setInterval(() => {
    if (loadingStep.value < 3) loadingStep.value++
  }, 1500)

  try {
    const data = await apiAsk({
      materialId: selectedMaterialId.value || null,
      question
    })
    currentAnswer.value = data.answer || data
    currentSources.value = data.sources || []
    // Add to history
    if (!historyList.value.includes(question)) {
      historyList.value.unshift(question)
      if (historyList.value.length > 10) historyList.value.pop()
    }
  } catch (error) {
    currentAnswer.value = '抱歉，答案生成失败，请稍后重试。'
  } finally {
    clearInterval(loadingTimer)
    loading.value = false
  }
}

function copyAnswer() {
  navigator.clipboard.writeText(currentAnswer.value).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}

async function loadMaterials() {
  materialList.value = await loadAvailableMaterials()
  const queryId = route.query.materialId
  if (queryId) selectedMaterialId.value = Number(queryId)
}

onMounted(() => loadMaterials())

watch(() => route.query.materialId, newId => {
  if (newId) selectedMaterialId.value = Number(newId)
})
</script>

<style scoped>
.qa-page { width: 100%; }

.qa-layout {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
  align-items: start;
}

.qa-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Input Card */
.qa-input-card {
  display: flex;
  gap: 16px;
  padding: 20px 24px;
  background: var(--blue-50);
  border: 1px solid var(--blue-100);
  border-radius: var(--radius-lg);
}

.qa-input-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.qa-input-body {
  flex: 1;
  min-width: 0;
}

.qa-input-body :deep(.el-textarea__inner) {
  background: var(--surface-card);
  border-color: var(--outline);
  border-radius: var(--radius-md);
}

.qa-input-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.qa-input-hint {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
}

.qa-ask-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 20px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #fff;
  font-size: var(--text-ui);
  font-weight: 500;
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-default);
}

.qa-ask-btn:hover { background: var(--color-primary-hover); }
.qa-ask-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.qa-ask-btn:active { transform: scale(0.98); }

.spinning { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Simple Loading (通用模式) */
.qa-loading-simple {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 32px 0;
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}

/* Progress Steps */
.qa-progress-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 24px 0;
}

.progress-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  opacity: 0.4;
  transition: opacity var(--duration-normal) var(--ease-default);
}

.progress-step.active { opacity: 0.8; }
.progress-step.done { opacity: 1; }

.step-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--surface-container);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-small);
  font-weight: 600;
  transition: all var(--duration-normal) var(--ease-default);
}

.progress-step.active .step-dot { background: var(--color-primary); color: #fff; }
.progress-step.done .step-dot { background: var(--color-success); color: #fff; }

.step-line {
  width: 60px;
  height: 2px;
  background: var(--outline);
  margin: 0 8px 22px;
  transition: background var(--duration-normal) var(--ease-default);
}

.step-line.active { background: var(--color-primary); }

.step-label {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
  font-weight: 500;
}

/* Empty */
.qa-empty {
  padding: 48px 0;
}

.suggested-questions {
  margin-top: 24px;
  text-align: center;
}

.suggested-label {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-bottom: 12px;
}

.question-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.question-chip {
  padding: 8px 16px;
  border: 1px solid var(--outline);
  border-radius: var(--radius-full);
  background: var(--surface-card);
  color: var(--color-text-secondary);
  font-size: var(--text-small);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.question-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: var(--color-primary-light);
}

/* Answer */
.answer-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.answer-actions {
  display: flex;
  gap: 4px;
}

.answer-body {
  font-size: var(--text-body);
  line-height: 1.8;
}

/* Reference */
.reference-section {
  margin-top: 4px;
}

.reference-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 16px;
}

.reference-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.reference-card {
  padding: 16px;
  background: var(--surface-card);
  border: 1px solid var(--outline);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-xs);
}

.ref-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  color: var(--color-text-secondary);
  font-size: var(--text-small);
}

.ref-doc {
  font-weight: 500;
}

.ref-meta {
  display: flex;
  justify-content: space-between;
  font-size: var(--text-micro);
  color: var(--color-text-tertiary);
}

.ref-score {
  color: var(--color-primary);
  font-weight: 600;
}

/* Sidebar */
.qa-sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sb-material-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  padding: 10px 12px;
  background: var(--blue-50);
  border-radius: var(--radius-md);
}

.sb-material-icon {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: var(--surface-card);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.sb-material-name {
  font-size: var(--text-small);
  font-weight: 500;
  color: var(--color-text-primary);
}

.history-list {
  display: flex;
  flex-direction: column;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: var(--text-small);
  color: var(--color-text-secondary);
  transition: background-color var(--duration-fast) var(--ease-default);
}

.history-item:hover { background: var(--surface-hover); color: var(--color-text-primary); }

.history-empty-text {
  padding: 24px 8px;
  text-align: center;
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
}

/* Responsive */
@media (max-width: 1279px) {
  .qa-layout { grid-template-columns: 1fr; }
  .qa-sidebar { display: none; }
}

@media (max-width: 767px) {
  .reference-grid { grid-template-columns: 1fr; }
  .qa-progress-steps { flex-direction: column; gap: 12px; }
  .step-line { width: 2px; height: 24px; margin: 0; }
}
</style>
