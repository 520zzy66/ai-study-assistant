<template>
  <div class="quiz-page">
    <div class="page-header">
      <h2>AI 自动出题</h2>
      <p>选择学习资料，AI 自动生成练习题并智能判分</p>
    </div>

    <div class="quiz-layout">
      <!-- Main Question Area -->
      <div class="quiz-main">
        <!-- Config Panel -->
        <BaseCard class="config-card">
          <div class="quiz-config">
            <div class="config-row">
              <div class="config-field" style="flex:2;min-width:200px;">
                <label class="config-label">选择资料</label>
                <el-select v-model="quizForm.materialId" placeholder="请选择资料" filterable style="width:100%;">
                  <el-option v-for="item in materialList" :key="item.id" :label="item.originalName" :value="item.id" />
                </el-select>
              </div>
              <div class="config-field" style="width:120px;">
                <label class="config-label">难度</label>
                <el-select v-model="quizForm.difficulty" style="width:100%;">
                  <el-option label="简单" value="easy" />
                  <el-option label="中等" value="medium" />
                  <el-option label="困难" value="hard" />
                </el-select>
              </div>
              <div class="config-field" style="width:100px;">
                <label class="config-label">单选题</label>
                <el-input-number v-model="quizForm.choiceCount" :min="0" :max="10" style="width:100%;" size="small" />
              </div>
              <div class="config-field" style="width:100px;">
                <label class="config-label">判断题</label>
                <el-input-number v-model="quizForm.judgeCount" :min="0" :max="10" style="width:100%;" size="small" />
              </div>
              <div class="config-field" style="width:100px;">
                <label class="config-label">简答题</label>
                <el-input-number v-model="quizForm.shortAnswerCount" :min="0" :max="5" style="width:100%;" size="small" />
              </div>
            </div>
            <div class="config-action">
              <button
                class="generate-btn"
                :disabled="!quizForm.materialId || generating"
                @click="handleGenerate"
              >
                <el-icon :size="16" :class="{ spinning: generating }">
                  <Loading v-if="generating" /><MagicStick v-else />
                </el-icon>
                {{ generating ? '生成中...' : '生成题目' }}
              </button>
            </div>
          </div>
        </BaseCard>

        <!-- Empty -->
        <div v-if="questions.length === 0 && !generating" class="quiz-empty-area">
          <AppEmpty icon="EditPen" title="选择资料开始出题" description="AI 将根据学习资料自动生成练习题" />
        </div>

        <!-- Questions -->
        <div v-else class="questions-area">
          <div
            v-for="(q, index) in questions"
            :key="index"
            class="question-card"
            :class="{
              'q-correct': showAnswers && isCorrect(q, index),
              'q-wrong': showAnswers && !isCorrect(q, index)
            }"
          >
            <!-- Header -->
            <div class="q-header">
              <div class="q-number">{{ index + 1 }}</div>
              <div class="q-tags">
                <el-tag size="small" :type="getTypeTag(q.type)">{{ getTypeLabel(q.type) }}</el-tag>
                <el-tag size="small" :type="getDifficultyTag(q.difficulty)">{{ getDifficultyLabel(q.difficulty) }}</el-tag>
              </div>
            </div>

            <!-- Question text -->
            <div class="q-text">{{ q.question }}</div>

            <!-- Code block if present -->
            <div v-if="q.codeBlock" class="q-code-block">
              <pre><code>{{ q.codeBlock }}</code></pre>
            </div>

            <!-- Options -->
            <div v-if="q.type === 'choice'" class="q-options">
              <div
                v-for="(val, key) in q.options"
                :key="key"
                class="q-option"
                :class="{
                  'opt-selected': userAnswers[index] === key,
                  'opt-correct': showAnswers && key === q.answer,
                  'opt-wrong': showAnswers && userAnswers[index] === key && key !== q.answer
                }"
                @click="!showAnswers && (userAnswers[index] = key)"
              >
                <span class="opt-key">{{ key }}</span>
                <span class="opt-val">{{ val }}</span>
              </div>
            </div>

            <div v-else-if="q.type === 'judge'" class="q-options">
              <div
                v-for="opt in ['true', 'false']"
                :key="opt"
                class="q-option"
                :class="{
                  'opt-selected': userAnswers[index] === opt,
                  'opt-correct': showAnswers && opt === q.answer,
                  'opt-wrong': showAnswers && userAnswers[index] === opt && opt !== q.answer
                }"
                @click="!showAnswers && (userAnswers[index] = opt)"
              >
                <span class="opt-val">{{ opt === 'true' ? '✓ 正确' : '✗ 错误' }}</span>
              </div>
            </div>

            <div v-else-if="q.type === 'short_answer'" class="q-options">
              <el-input
                v-model="userAnswers[index]"
                type="textarea"
                :rows="3"
                placeholder="请输入你的答案"
                :disabled="showAnswers"
              />
            </div>

            <!-- Answer Result -->
            <div v-if="showAnswers" class="q-result">
              <div class="result-badge" :class="isCorrect(q, index) ? 'badge-ok' : 'badge-fail'">
                <el-icon :size="14"><Check v-if="isCorrect(q, index)" /><Close v-else /></el-icon>
                {{ isCorrect(q, index) ? '回答正确' : '回答错误' }}
              </div>
              <div class="result-detail">
                <div class="result-row">
                  <span class="result-label">正确答案</span>
                  <span class="result-value">{{ q.answer }}</span>
                </div>
                <div v-if="q.explanation" class="result-row">
                  <span class="result-label">解析</span>
                  <span class="result-value">{{ q.explanation }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Submit Area -->
          <div v-if="questions.length > 0" class="submit-area">
            <button v-if="!showAnswers" class="submit-btn" @click="handleSubmit">提交答案</button>
            <button v-else class="submit-btn retry-btn" @click="handleReset">
              <el-icon :size="16"><Refresh /></el-icon> 重新答题
            </button>
          </div>
        </div>
      </div>

      <!-- Right Panel -->
      <div v-if="questions.length > 0" class="quiz-panel">
        <!-- Timer -->
        <BaseCard class="panel-card">
          <div class="timer-display">
            <div class="timer-label">计时器</div>
            <div class="timer-value">{{ timerDisplay }}</div>
          </div>
        </BaseCard>

        <!-- Answer State Grid -->
        <BaseCard title="答题状态" class="panel-card">
          <div class="answer-grid">
            <div
              v-for="(q, i) in questions"
              :key="'ag-'+i"
              class="answer-square"
              :class="{
                'sq-current': !showAnswers && i === currentQuestionIndex,
                'sq-answered': !showAnswers && userAnswers[i],
                'sq-correct': showAnswers && isCorrect(q, i),
                'sq-wrong': showAnswers && !isCorrect(q, i)
              }"
              @click="scrollToQuestion(i)"
            >
              {{ i + 1 }}
            </div>
          </div>
          <button class="flag-btn" @click="flagQuestion(currentQuestionIndex)">
            <el-icon :size="14"><Flag /></el-icon> 标记题目
          </button>
        </BaseCard>

        <!-- Score Ring -->
        <BaseCard title="得分" class="panel-card" v-if="showAnswers">
          <div class="score-center">
            <ProgressRing :percentage="scorePercent" :size="130" :stroke-width="10" :color="scoreColor" />
            <p class="score-text">{{ correctCount }}/{{ questions.length }} 正确</p>
          </div>
        </BaseCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MagicStick, Loading, Check, Close, Refresh, Flag } from '@element-plus/icons-vue'
import { generateQuiz, submitAnswers } from '@/api/ai'
import { loadAvailableMaterials } from '@/api/material'
import BaseCard from '@/components/common/BaseCard.vue'
import ProgressRing from '@/components/common/ProgressRing.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'

const route = useRoute()
const materialList = ref([])
const questions = ref([])
const userAnswers = ref([])
const showAnswers = ref(false)
const generating = ref(false)
const batchId = ref('')
const currentQuestionIndex = ref(0)

// Timer
const timerSeconds = ref(0)
let timerInterval = null

const quizForm = reactive({
  materialId: '',
  choiceCount: 5,
  judgeCount: 3,
  shortAnswerCount: 2,
  difficulty: 'medium'
})

const timerDisplay = computed(() => {
  const h = Math.floor(timerSeconds.value / 3600)
  const m = Math.floor((timerSeconds.value % 3600) / 60)
  const s = timerSeconds.value % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const correctCount = computed(() =>
  questions.value.filter((q, i) => isCorrect(q, i)).length
)

const scorePercent = computed(() =>
  questions.value.length ? Math.round((correctCount.value / questions.value.length) * 100) : 0
)

const scoreColor = computed(() =>
  scorePercent.value >= 80 ? '#16a34a' : scorePercent.value >= 60 ? '#f59e0b' : '#dc2626'
)

function getTypeTag(t) { return { choice: '', judge: 'success', short_answer: 'warning' }[t] || 'info' }
function getTypeLabel(t) { return { choice: '单选题', judge: '判断题', short_answer: '简答题' }[t] || t }
function getDifficultyTag(d) { return { easy: 'success', medium: 'warning', hard: 'danger' }[d] || 'info' }
function getDifficultyLabel(d) { return { easy: '简单', medium: '中等', hard: '困难' }[d] || d }

function isCorrect(q, index) {
  const ua = userAnswers.value[index]
  if (!ua) return false
  return String(ua).toLowerCase() === String(q.answer).toLowerCase()
}

function startTimer() {
  timerSeconds.value = 0
  clearInterval(timerInterval)
  timerInterval = setInterval(() => { timerSeconds.value++ }, 1000)
}

function stopTimer() { clearInterval(timerInterval) }

async function handleGenerate() {
  if (!quizForm.materialId) { ElMessage.warning('请先选择资料'); return }
  generating.value = true
  try {
    const data = await generateQuiz(quizForm.materialId, quizForm)
    questions.value = data.questions || []
    batchId.value = data.batchId || ''
    userAnswers.value = new Array(questions.value.length).fill('')
    showAnswers.value = false
    currentQuestionIndex.value = 0
    startTimer()
    ElMessage.success(`成功生成 ${questions.value.length} 道题目`)
  } catch { questions.value = [] }
  finally { generating.value = false }
}

async function handleSubmit() {
  const unanswered = userAnswers.value.findIndex(a => !a && a !== 0)
  if (unanswered !== -1) { ElMessage.warning(`请完成第 ${unanswered + 1} 题`); return }
  stopTimer()
  try {
    const answers = questions.value.map((q, i) => ({ questionId: q.id || i, answer: userAnswers.value[i] }))
    if (batchId.value) await submitAnswers(batchId.value, answers)
    showAnswers.value = true
    ElMessage.success('答案已提交')
  } catch { /* handled by interceptor */ }
}

function handleReset() {
  userAnswers.value = new Array(questions.value.length).fill('')
  showAnswers.value = false
  startTimer()
}

function scrollToQuestion(i) {
  currentQuestionIndex.value = i
  const cards = document.querySelectorAll('.question-card')
  if (cards[i]) cards[i].scrollIntoView({ behavior: 'smooth', block: 'center' })
}

function flagQuestion(i) {
  ElMessage.info(`已标记第 ${i + 1} 题`)
}

onMounted(async () => {
  materialList.value = await loadAvailableMaterials()
  // 支持从资料库跳转自动选择
  const queryId = route.query.materialId
  if (queryId) {
    quizForm.materialId = Number(queryId)
  }
})

watch(() => route.query.materialId, (newId) => {
  if (newId) {
    quizForm.materialId = Number(newId)
  }
})

onUnmounted(() => stopTimer())
</script>

<style scoped>
.quiz-page { width: 100%; }

.quiz-layout {
  display: grid;
  grid-template-columns: 1fr 260px;
  gap: 24px;
  align-items: start;
}

.quiz-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Config */
.config-card { padding: 0; }
.config-card :deep(.card-body) { padding: 20px 24px; }

.quiz-config {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.config-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.config-label {
  font-size: var(--text-small);
  font-weight: 500;
  color: var(--color-text-secondary);
}

.config-action {
  display: flex;
  justify-content: flex-end;
}

.generate-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 40px;
  padding: 0 24px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #fff;
  font-size: var(--text-ui);
  font-weight: 500;
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-default);
}

.generate-btn:hover { background: var(--color-primary-hover); }
.generate-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.spinning { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.quiz-empty-area {
  padding: 48px 0;
}

/* Questions */
.questions-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.question-card {
  background: var(--surface-card);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-xs);
  transition: border-color var(--duration-fast) var(--ease-default),
              background-color var(--duration-fast) var(--ease-default);
}

.question-card.q-correct { border-color: #bbf7d0; background: #f0fdf4; }
.question-card.q-wrong { border-color: #fecaca; background: #fef2f2; }

.q-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.q-number {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  background: var(--blue-50);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-small);
  font-weight: 700;
}

.q-tags { display: flex; gap: 6px; }

.q-text {
  font-size: var(--text-body);
  line-height: 1.7;
  color: var(--color-text-primary);
  margin-bottom: 16px;
  font-weight: 500;
}

.q-code-block {
  background: #1e1e2e;
  border-radius: var(--radius-md);
  padding: 16px;
  margin-bottom: 16px;
  overflow-x: auto;
}

.q-code-block pre { margin: 0; }
.q-code-block code { color: #cdd6f4; font-family: var(--font-mono); font-size: var(--text-small); line-height: 1.6; }

/* Options */
.q-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.q-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid var(--outline);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.q-option:hover { border-color: var(--color-primary); }

.opt-selected {
  border-color: var(--color-primary);
  background: var(--blue-50);
}

.opt-correct {
  border-color: #86efac;
  background: #f0fdf4;
}

.opt-wrong {
  border-color: #fca5a5;
  background: #fef2f2;
}

.opt-key {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  background: var(--surface-container);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-small);
  font-weight: 600;
  flex-shrink: 0;
}

.opt-selected .opt-key { background: var(--color-primary); color: #fff; }
.opt-correct .opt-key { background: #16a34a; color: #fff; }
.opt-wrong .opt-key { background: #dc2626; color: #fff; }

.opt-val { font-size: var(--text-ui); color: var(--color-text-primary); }

/* Result */
.q-result {
  padding-top: 16px;
  border-top: 1px solid var(--outline-variant);
}

.result-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: var(--radius-sm);
  font-size: var(--text-small);
  font-weight: 600;
  margin-bottom: 12px;
}

.badge-ok { background: #dcfce7; color: #16a34a; }
.badge-fail { background: #ffe4e6; color: #e11d48; }

.result-detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-row {
  display: flex;
  gap: 8px;
}

.result-label {
  font-size: var(--text-small);
  font-weight: 600;
  color: var(--color-text-secondary);
  flex-shrink: 0;
  width: 64px;
}

.result-value {
  font-size: var(--text-small);
  color: var(--color-text-primary);
  line-height: 1.6;
}

/* Submit */
.submit-area {
  display: flex;
  justify-content: center;
  padding: 8px 0;
}

.submit-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  padding: 0 32px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-success);
  color: #fff;
  font-size: var(--text-body);
  font-weight: 600;
  cursor: pointer;
  transition: background var(--duration-fast) var(--ease-default);
}

.submit-btn:hover { background: #15803d; }
.retry-btn { background: var(--color-primary); }
.retry-btn:hover { background: var(--color-primary-hover); }

/* Right Panel */
.quiz-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
  position: sticky;
  top: calc(var(--header-height) + 32px);
}

.panel-card :deep(.card-body) { padding: 20px; }

.timer-display {
  text-align: center;
}

.timer-label {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
  margin-bottom: 4px;
}

.timer-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-text-primary);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
}

.answer-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.answer-square {
  aspect-ratio: 1;
  border-radius: var(--radius-sm);
  background: var(--surface-container);
  color: var(--color-text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-small);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.answer-square:hover { background: var(--surface-hover); }
.sq-current { border: 2px solid var(--color-primary); }
.sq-answered { background: var(--blue-100); color: var(--color-primary); }
.sq-correct { background: #dcfce7; color: #16a34a; }
.sq-wrong { background: #ffe4e6; color: #e11d48; }

.flag-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  width: 100%;
  justify-content: center;
  padding: 8px 0;
  border: 1px solid var(--outline);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  color: var(--color-text-secondary);
  font-size: var(--text-small);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.flag-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }

.score-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
}

.score-text {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
  margin-top: 12px;
}

/* Responsive */
@media (max-width: 1279px) {
  .quiz-layout { grid-template-columns: 1fr; }
  .quiz-panel {
    position: static;
    display: grid;
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 767px) {
  .config-row { flex-direction: column; }
  .config-field { width: 100% !important; }
  .quiz-panel { grid-template-columns: 1fr; }
}
</style>
