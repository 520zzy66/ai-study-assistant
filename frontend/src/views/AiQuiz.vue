<template>
  <div class="quiz-page">
    <BasePageHeader
      title="AI 自动出题"
      description="选择学习资料或文件夹，AI 自动生成练习题并智能判分"
    />

    <div class="quiz-layout">
      <!-- Main Question Area -->
      <div class="quiz-main">
        <!-- Config Panel -->
        <BaseCard class="config-card">
          <div class="quiz-config">
            <div class="config-row">
              <div class="config-field" style="flex:2;min-width:200px;">
                <label class="config-label">选择文件夹或资料</label>
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
              <div class="config-field" style="width:120px;">
                <label class="config-label">难度</label>
                <el-select v-model="quizForm.difficulty" style="width:100%;">
                  <el-option label="简单" value="easy" />
                  <el-option label="中等" value="medium" />
                  <el-option label="困难" value="hard" />
                </el-select>
              </div>
              <div class="config-field" style="flex:1; min-width:160px;">
                <label class="config-label">批次名称（可选）</label>
                <el-input v-model="quizForm.batchName" placeholder="自动生成：资料名+时间" size="small" clearable />
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
              <div class="config-field" style="width:100px;">
                <label class="config-label">填空题</label>
                <el-input-number v-model="quizForm.fillBlankCount" :min="0" :max="10" style="width:100%;" size="small" />
              </div>
              <div class="config-field" style="width:100px;">
                <label class="config-label">多选题</label>
                <el-input-number v-model="quizForm.multiChoiceCount" :min="0" :max="10" style="width:100%;" size="small" />
              </div>
              <div class="config-field" style="width:100px;">
                <label class="config-label">数学题</label>
                <el-input-number v-model="quizForm.mathFillCount" :min="0" :max="10" style="width:100%;" size="small" />
              </div>
            </div>
            <div class="config-action">
              <button
                class="generate-btn"
                :disabled="!selectedInfo || generating"
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

        <!-- Generating Progress -->
        <BaseCard v-if="generating" class="generate-progress-card">
          <div class="generate-progress-wrap">
            <el-icon :size="32" class="spinning" color="var(--color-primary)"><Loading /></el-icon>
            <div class="generate-progress-info">
              <p class="generate-progress-title">AI 正在生成题目...</p>
              <p class="generate-progress-msg">{{ generateMsg }}</p>
              <div class="generate-progress-bar">
                <div class="generate-progress-fill" :style="{ width: generateProgress + '%' }" />
              </div>
            </div>
          </div>
        </BaseCard>

        <!-- Generate Error -->
        <BaseCard v-if="generateError && !generating" class="generate-error-card">
          <div class="generate-error-wrap">
            <el-icon :size="32" color="var(--color-error)"><CircleCloseFilled /></el-icon>
            <div class="generate-error-info">
              <p class="generate-error-title">题目生成失败</p>
              <p class="generate-error-msg">{{ generateError }}</p>
            </div>
            <button class="retry-btn" @click="handleGenerate">
              <el-icon><Refresh /></el-icon> 重试
            </button>
          </div>
        </BaseCard>

        <!-- Questions -->
        <template v-if="questions.length > 0">
          <!-- Question Cards -->
          <BaseCard
            v-for="(q, index) in questions"
            :key="index"
            :id="'question-' + index"
            class="question-card"
            :class="{
              'is-correct': showAnswers && isCorrect(q, index),
              'is-wrong': showAnswers && !isCorrect(q, index),
              'is-current': currentQuestionIndex === index
            }"
          >
            <div class="question-header">
              <div class="question-meta">
                <el-tag :type="getTypeTag(q.type)" size="small" effect="light">
                  {{ getTypeLabel(q.type) }}
                </el-tag>
                <el-tag v-if="q.difficulty" :type="getDifficultyTag(q.difficulty)" size="small" effect="plain">
                  {{ getDifficultyLabel(q.difficulty) }}
                </el-tag>
                <span class="question-number">第 {{ index + 1 }} 题</span>
              </div>
              <div v-if="showAnswers" class="question-result">
                <el-icon v-if="isCorrect(q, index)" :size="20" color="var(--color-success)"><CircleCheckFilled /></el-icon>
                <el-icon v-else :size="20" color="var(--color-error)"><CircleCloseFilled /></el-icon>
              </div>
            </div>

            <p class="question-text">{{ q.question }}</p>

            <!-- Options (choice type) -->
            <div v-if="q.type === 'choice'" class="options-list">
              <div
                v-for="(value, key) in q.options"
                :key="key"
                class="option-item"
                :class="{
                  selected: userAnswers[index] === key,
                  correct: showAnswers && key === q.answer,
                  wrong: showAnswers && userAnswers[index] === key && key !== q.answer
                }"
                @click="!showAnswers && selectOption(index, key)"
              >
                <span class="option-key">{{ key }}</span>
                <span class="option-value">{{ value }}</span>
                <el-icon v-if="showAnswers && key === q.answer" class="correct-icon" :size="16">
                  <CircleCheckFilled />
                </el-icon>
              </div>
            </div>

            <!-- Judge options -->
            <div v-else-if="q.type === 'judge'" class="options-list">
              <div
                v-for="opt in [{ key: 'T', label: '正确' }, { key: 'F', label: '错误' }]"
                :key="opt.key"
                class="option-item"
                :class="{
                  selected: userAnswers[index] === opt.key,
                  correct: showAnswers && opt.key === q.answer,
                  wrong: showAnswers && userAnswers[index] === opt.key && opt.key !== q.answer
                }"
                @click="!showAnswers && selectOption(index, opt.key)"
              >
                <span class="option-key">{{ opt.key === 'T' ? '✓' : '✗' }}</span>
                <span class="option-value">{{ opt.label }}</span>
                <el-icon v-if="showAnswers && opt.key === q.answer" class="correct-icon" :size="16">
                  <CircleCheckFilled />
                </el-icon>
              </div>
            </div>

            <!-- Short answer input -->
            <div v-else-if="q.type === 'short_answer'" class="short-answer">
              <el-input
                v-model="userAnswers[index]"
                type="textarea"
                :rows="3"
                placeholder="请输入你的答案..."
                :disabled="showAnswers"
              />
            </div>

            <!-- Fill blank input -->
            <div v-else-if="q.type === 'fill_blank'" class="fill-blank">
              <el-input
                v-model="userAnswers[index]"
                placeholder="请填入答案..."
                :disabled="showAnswers"
                clearable
              />
            </div>

            <!-- Multi choice (checkboxes) -->
            <div v-else-if="q.type === 'multi_choice'" class="options-list multi-choice">
              <div
                v-for="(value, key) in q.options"
                :key="key"
                class="option-item multi"
                :class="{
                  selected: isMultiSelected(index, key),
                  correct: showAnswers && isMultiCorrect(key, q.answer),
                  wrong: showAnswers && isMultiSelected(index, key) && !isMultiCorrect(key, q.answer)
                }"
                @click="!showAnswers && toggleMultiOption(index, key)"
              >
                <span class="option-checkbox">
                  <el-icon v-if="isMultiSelected(index, key)"><CircleCheckFilled /></el-icon>
                  <el-icon v-else><CircleCheck /></el-icon>
                </span>
                <span class="option-key">{{ key }}</span>
                <span class="option-value">{{ value }}</span>
                <el-icon v-if="showAnswers && isMultiCorrect(key, q.answer)" class="correct-icon" :size="16">
                  <CircleCheckFilled />
                </el-icon>
              </div>
            </div>

            <!-- Math fill input -->
            <div v-else-if="q.type === 'math_fill'" class="math-fill">
              <el-input
                v-model="userAnswers[index]"
                placeholder="输入数值或表达式（如 1/3、0.5、√2）"
                :disabled="showAnswers"
                clearable
              />
            </div>

            <!-- Answer explanation (shown after submit) -->
            <div v-if="showAnswers && q.explanation" class="answer-explanation">
              <div class="explanation-header">
                <el-icon><InfoFilled /></el-icon>
                <span>答案解析</span>
              </div>
              <p class="explanation-text">{{ q.explanation }}</p>
              <div v-if="q.type === 'short_answer' && q.score !== undefined" class="score-display">
                <span class="score-label">得分：</span>
                <span class="score-value" :class="{ 'full-score': q.score === 100 }">{{ q.score }} 分</span>
              </div>
            </div>
          </BaseCard>

          <!-- Action Bar -->
          <BaseCard class="action-card">
            <div class="action-bar">
              <div class="action-info">
                <span v-if="!showAnswers">
                  已答 {{ userAnswers.filter(a => a || a === 0).length }}/{{ questions.length }} 题
                </span>
                <span v-else>
                  得分：<strong>{{ scorePercent }}</strong> 分（{{ correctCount }}/{{ questions.length }} 正确）
                </span>
                <span class="timer">
                  <el-icon><Timer /></el-icon>
                  {{ timerDisplay }}
                </span>
              </div>
              <div class="action-buttons">
                <button v-if="!showAnswers" class="submit-btn" :disabled="submitting" @click="handleSubmit">
                  <el-icon :class="{ 'is-loading': submitting }">
                    <Loading v-if="submitting" />
                    <Check v-else />
                  </el-icon>
                  {{ submitting ? 'AI 正在判分中...' : '提交答案' }}
                </button>
                <button v-else class="reset-btn" @click="handleReset">
                  <el-icon><Refresh /></el-icon> 重新作答
                </button>
              </div>
            </div>
          </BaseCard>
        </template>

        <!-- Empty State -->
        <div v-if="!generating && !generateError && questions.length === 0" class="empty-state">
          <AppEmpty
            icon="Edit"
            title="选择资料开始出题"
            description="AI 将根据学习资料自动生成练习题"
          />
        </div>
      </div>

      <!-- Right Sidebar -->
      <div v-if="questions.length > 0" class="quiz-sidebar">
        <!-- Question Navigator -->
        <BaseCard title="题目导航" class="panel-card">
          <div class="question-nav">
            <button
              v-for="(q, index) in questions"
              :key="index"
              class="nav-item"
              :class="{
                active: currentQuestionIndex === index,
                answered: userAnswers[index] || userAnswers[index] === 0,
                correct: showAnswers && isCorrect(q, index),
                wrong: showAnswers && !isCorrect(q, index),
                flagged: false
              }"
              @click="scrollToQuestion(index)"
            >
              {{ index + 1 }}
            </button>
          </div>
        </BaseCard>

        <!-- Current Question Info -->
        <BaseCard title="当前题目" class="panel-card" v-if="!showAnswers">
          <div class="current-info">
            <p class="current-number">第 {{ currentQuestionIndex + 1 }} 题</p>
            <p class="current-type">{{ getTypeLabel(questions[currentQuestionIndex]?.type) }}</p>
            <button class="flag-btn" @click="flagQuestion(currentQuestionIndex)">
              <el-icon :size="14"><Flag /></el-icon> 标记题目
            </button>
          </div>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { MagicStick, Loading, Check, Close, Refresh, Flag, CircleCloseFilled, Folder, Document, InfoFilled, Timer, CircleCheckFilled, CircleCheck } from '@element-plus/icons-vue'
import { generateQuiz, submitAnswers, generateQuizAsync } from '@/api/ai'
import { loadAvailableMaterials } from '@/api/material'
import { getFolderTree } from '@/api/materialFolder'
import { buildCascaderOptions, parseCascaderValue, getCascaderLabel } from '@/utils/folderTreeHelper'
import { useTaskStore } from '@/stores/task'
import BaseCard from '@/components/common/BaseCard.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'
import ProgressRing from '@/components/common/ProgressRing.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'

const route = useRoute()
const taskStore = useTaskStore()
const folderTree = ref([])
const materialList = ref([])
const cascaderValue = ref([])
const questions = ref([])
const userAnswers = ref([])
const showAnswers = ref(false)
const generating = ref(false)
const submitting = ref(false)
const generateProgress = ref(0)
const generateMsg = ref('')
const generateError = ref('')
const batchId = ref('')
const currentQuestionIndex = ref(0)

// Timer
const timerSeconds = ref(0)
let timerInterval = null

const quizForm = reactive({
  batchName: '',
  choiceCount: 5,
  judgeCount: 3,
  shortAnswerCount: 2,
  fillBlankCount: 0,
  multiChoiceCount: 0,
  mathFillCount: 0,
  difficulty: 'medium'
})

// 级联选择器配置
const cascaderProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  checkStrictly: false,
  emitPath: true
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

// 级联选择器变化处理
function handleCascaderChange(value) {
  cascaderValue.value = value || []
}

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

function getTypeTag(t) {
  return { choice: '', judge: 'success', short_answer: 'warning', fill_blank: 'info', multi_choice: 'danger', math_fill: 'warning' }[t] || 'info'
}
function getTypeLabel(t) {
  return { choice: '单选题', judge: '判断题', short_answer: '简答题', fill_blank: '填空题', multi_choice: '多选题', math_fill: '数学题' }[t] || t
}
function getDifficultyTag(d) { return { easy: 'success', medium: 'warning', hard: 'danger' }[d] || 'info' }
function getDifficultyLabel(d) { return { easy: '简单', medium: '中等', hard: '困难' }[d] || d }

function isCorrect(q, index) {
  const ua = userAnswers.value[index]
  if (!ua) return false
  // 多选题：集合比较
  if (q.type === 'multi_choice') {
    const refSet = parseChoiceSet(q.answer)
    const stuSet = parseChoiceSet(ua)
    return refSet.size === stuSet.size && [...refSet].every(x => stuSet.has(x))
  }
  return String(ua).toLowerCase() === String(q.answer).toLowerCase()
}

function parseChoiceSet(answer) {
  if (!answer) return new Set()
  return new Set(answer.split(/[,，、\s]+/).map(s => s.trim().toUpperCase()).filter(Boolean))
}

function selectOption(index, key) {
  userAnswers.value[index] = key
}

/** 多选题：勾选/取消勾选选项 */
function toggleMultiOption(index, key) {
  const current = userAnswers.value[index] || ''
  const set = parseChoiceSet(current)
  if (set.has(key)) {
    set.delete(key)
  } else {
    set.add(key)
  }
  userAnswers.value[index] = [...set].sort().join(',')
}

/** 多选题：判断选项是否被选中 */
function isMultiSelected(index, key) {
  const current = userAnswers.value[index] || ''
  return parseChoiceSet(current).has(key)
}

/** 多选题：判断选项是否在正确答案中 */
function isMultiCorrect(key, answer) {
  return parseChoiceSet(answer).has(key)
}

function startTimer() {
  timerSeconds.value = 0
  clearInterval(timerInterval)
  timerInterval = setInterval(() => { timerSeconds.value++ }, 1000)
}

function stopTimer() { clearInterval(timerInterval) }

async function handleGenerate() {
  if (!selectedInfo.value) { ElMessage.warning('请先选择资料或文件夹'); return }
  generating.value = true
  generateError.value = ''
  generateProgress.value = 0
  generateMsg.value = '正在创建任务...'
  try {
    const params = { ...quizForm }

    // 根据选择类型传递不同参数
    if (selectedInfo.value.type === 'folder') {
      params.folderId = selectedInfo.value.id
    }

    const materialId = selectedInfo.value.type === 'material' ? selectedInfo.value.id : null
    const { taskId } = await generateQuizAsync(materialId, params)

    taskStore.watchTask(taskId, 'quiz', {
      onProgress(pct, msg) {
        generateProgress.value = pct
        generateMsg.value = msg
      },
      onSuccess(result) {
        questions.value = result.questions || []
        batchId.value = result.batchId || ''
        userAnswers.value = new Array(questions.value.length).fill('')
        showAnswers.value = false
        currentQuestionIndex.value = 0
        startTimer()
        generating.value = false
        generateError.value = ''
        ElMessage.success(`成功生成 ${questions.value.length} 道题目`)
      },
      onError(errMsg) {
        generating.value = false
        generateError.value = errMsg || '题目生成失败，请稍后重试'
        ElMessage.error(errMsg || '题目生成失败')
      }
    })
  } catch (err) {
    generating.value = false
    generateError.value = err?.message || '创建任务失败，请检查网络连接'
    ElMessage.error('创建任务失败')
  }
}

async function handleSubmit() {
  const unanswered = userAnswers.value.findIndex(a => !a && String(a).trim() === '')
  if (unanswered !== -1) {
    try {
      await ElMessageBox.confirm(
        `还有题目未作答（如第 ${unanswered + 1} 题），确定要提前交卷吗？`,
        '提示',
        { confirmButtonText: '强制交卷', cancelButtonText: '继续作答', type: 'warning' }
      )
    } catch {
      return
    }
  }
  
  submitting.value = true
  try {
    const answers = questions.value.map((q, i) => ({ questionId: q.id || i, answer: userAnswers.value[i] || '' }))
    if (batchId.value) await submitAnswers(batchId.value, answers)
    stopTimer()
    showAnswers.value = true
    ElMessage.success('答案已提交')
  } catch (err) {
    ElMessage.error(err?.message || '提交失败，请重试')
  } finally {
    submitting.value = false
  }
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

  // 恢复未完成的任务
  const activeTask = taskStore.getFirstActiveOfType('quiz')
  if (activeTask) {
    generating.value = true
    generateMsg.value = '恢复任务中...'
    taskStore.watchTask(activeTask.taskId, 'quiz', {
      onProgress(pct, msg) {
        generateProgress.value = pct
        generateMsg.value = msg
      },
      onSuccess(result) {
        questions.value = result.questions || []
        batchId.value = result.batchId || ''
        userAnswers.value = new Array(questions.value.length).fill('')
        showAnswers.value = false
        currentQuestionIndex.value = 0
        startTimer()
        generating.value = false
        generateError.value = ''
        ElMessage.success(`成功生成 ${questions.value.length} 道题目`)
      },
      onError(errMsg) {
        generating.value = false
        generateError.value = errMsg || '题目生成失败'
      }
    })
  }
})

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

onUnmounted(() => {
  stopTimer()
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
.quiz-page {
  width: 100%;
}

.quiz-layout {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: var(--space-6);
  align-items: start;
}

.config-card {
  border-radius: var(--radius-lg);
}

.quiz-config {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.config-row {
  display: flex;
  gap: var(--space-4);
  flex-wrap: wrap;
}

.config-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
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

/* Generate Button */
.generate-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 10px 24px;
  background: var(--color-primary);
  color: var(--color-on-primary);
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-ui);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.generate-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
}

.generate-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Progress Card */
.generate-progress-card {
  border-radius: var(--radius-lg);
}

.generate-progress-wrap {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  padding: var(--space-4);
}

.generate-progress-info {
  flex: 1;
}

.generate-progress-title {
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--space-2);
}

.generate-progress-msg {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-3);
}

.generate-progress-bar {
  height: 6px;
  background: var(--surface-container);
  border-radius: 3px;
  overflow: hidden;
}

.generate-progress-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 3px;
  transition: width 0.3s ease;
}

/* Error Card */
.generate-error-card {
  border-radius: var(--radius-lg);
  border-left: 4px solid var(--color-error);
}

.generate-error-wrap {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
}

.generate-error-info {
  flex: 1;
}

.generate-error-title {
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-error);
  margin-bottom: var(--space-2);
}

.generate-error-msg {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}

.retry-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 8px 16px;
  background: transparent;
  color: var(--color-primary);
  border: 1px solid var(--color-primary);
  border-radius: var(--radius-md);
  font-size: var(--text-ui);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.retry-btn:hover {
  background: var(--color-primary-light-9);
}

/* Question Cards */
.question-card {
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-4);
  transition: all var(--duration-fast) var(--ease-default);
}

.question-card.is-current {
  border-left: 4px solid var(--color-primary);
}

.question-card.is-correct {
  border-left: 4px solid var(--color-success);
}

.question-card.is-wrong {
  border-left: 4px solid var(--color-error);
}

.question-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-4);
}

.question-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.question-number {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
}

.question-text {
  font-size: var(--text-body);
  color: var(--color-text-primary);
  line-height: 1.6;
  margin-bottom: var(--space-5);
}

/* Options */
.options-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.option-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 12px 16px;
  background: var(--surface-container-low);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.option-item:hover:not(.correct):not(.wrong) {
  background: var(--surface-container);
}

.option-item.selected {
  border-color: var(--color-primary);
  background: var(--color-primary-light-9);
}

.option-item.correct {
  border-color: var(--color-success);
  background: var(--color-success-light-9);
}

.option-item.wrong {
  border-color: var(--color-error);
  background: var(--color-error-light-9);
}

.option-key {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: var(--surface-card);
  border-radius: 50%;
  font-size: var(--text-small);
  font-weight: 600;
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.option-value {
  flex: 1;
  font-size: var(--text-body);
  color: var(--color-text-primary);
}

.correct-icon {
  margin-left: auto;
}

/* Short Answer */
.short-answer {
  margin-bottom: var(--space-4);
}

.fill-blank,
.math-fill {
  margin-bottom: var(--space-4);
}

.fill-blank .el-input,
.math-fill .el-input {
  max-width: 400px;
}

/* Multi choice checkbox style */
.multi-choice .option-item {
  cursor: pointer;
}

.option-checkbox {
  display: inline-flex;
  align-items: center;
  margin-right: var(--space-2);
  color: var(--color-text-tertiary);
}

.option-item.multi.selected .option-checkbox {
  color: var(--color-primary);
}

/* Answer Explanation */
.answer-explanation {
  margin-top: var(--space-5);
  padding: var(--space-4);
  background: var(--surface-container-low);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--color-primary);
}

.explanation-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-ui);
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: var(--space-3);
}

.explanation-text {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.score-display {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--outline-variant);
}

.score-label {
  font-size: var(--text-ui);
  color: var(--color-text-secondary);
}

.score-value {
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-warning);
}

.score-value.full-score {
  color: var(--color-success);
}

/* Action Card */
.action-card {
  border-radius: var(--radius-lg);
  position: sticky;
  bottom: var(--space-4);
  z-index: 10;
}

.action-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.action-info {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  font-size: var(--text-ui);
  color: var(--color-text-secondary);
}

.timer {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-weight: 500;
}

.submit-btn,
.reset-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 10px 24px;
  border: none;
  border-radius: var(--radius-md);
  font-size: var(--text-ui);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.submit-btn {
  background: var(--color-primary);
  color: var(--color-on-primary);
}

.submit-btn:hover {
  background: var(--color-primary-hover);
}

.reset-btn {
  background: var(--surface-container);
  color: var(--color-text-primary);
}

.reset-btn:hover {
  background: var(--surface-container-high);
}

/* Empty State */
.empty-state {
  padding: var(--space-10) 0;
}

/* Sidebar */
.quiz-sidebar {
  position: sticky;
  top: calc(var(--header-height) + var(--space-6));
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.panel-card {
  border-radius: var(--radius-lg);
}

.question-nav {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: var(--space-2);
}

.nav-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: var(--surface-container-low);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  font-size: var(--text-ui);
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.nav-item:hover {
  background: var(--surface-container);
}

.nav-item.active {
  border-color: var(--color-primary);
  background: var(--color-primary-light-9);
  color: var(--color-primary);
}

.nav-item.answered {
  background: var(--color-primary-light-9);
  color: var(--color-primary);
}

.nav-item.correct {
  background: var(--color-success-light-9);
  color: var(--color-success);
  border-color: var(--color-success);
}

.nav-item.wrong {
  background: var(--color-error-light-9);
  color: var(--color-error);
  border-color: var(--color-error);
}

.current-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.current-number {
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-text-primary);
}

.current-type {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}

.flag-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: 8px 12px;
  background: transparent;
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-md);
  font-size: var(--text-small);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.flag-btn:hover {
  background: var(--surface-container);
  color: var(--color-warning);
  border-color: var(--color-warning);
}

.score-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
}

.score-text {
  font-size: var(--text-ui);
  font-weight: 500;
  color: var(--color-text-secondary);
}

/* Responsive */
@media (max-width: 1279px) {
  .quiz-layout {
    grid-template-columns: 1fr;
  }

  .quiz-sidebar {
    position: static;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .quiz-sidebar > * {
    flex: 1;
    min-width: 200px;
  }
}

@media (max-width: 767px) {
  .config-row {
    flex-direction: column;
  }

  .config-field {
    width: 100% !important;
  }

  .action-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .action-info {
    justify-content: center;
  }

  .submit-btn,
  .reset-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
