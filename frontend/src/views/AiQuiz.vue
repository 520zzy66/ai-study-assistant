<template>
  <div class="quiz-page">
    <div class="page-header">
      <h2>AI 自动出题</h2>
      <p>选择学习资料，AI 自动生成练习题并智能判分</p>
    </div>

    <el-card shadow="never" class="quiz-card">
      <!-- Config Panel -->
      <div class="quiz-config">
        <div class="config-row">
          <div class="config-field">
            <label class="config-label">选择资料</label>
            <el-select v-model="quizForm.materialId" placeholder="请选择资料" filterable style="width: 100%;">
              <el-option
                v-for="item in materialList"
                :key="item.id"
                :label="item.originalName"
                :value="item.id"
              />
            </el-select>
          </div>
          <div class="config-field" style="width: 160px;">
            <label class="config-label">难度</label>
            <el-select v-model="quizForm.difficulty" style="width: 100%;">
              <el-option label="简单" value="easy" />
              <el-option label="中等" value="medium" />
              <el-option label="困难" value="hard" />
            </el-select>
          </div>
          <div class="config-field" style="width: 140px;">
            <label class="config-label">单选题</label>
            <el-input-number v-model="quizForm.choiceCount" :min="0" :max="10" style="width: 100%;" />
          </div>
          <div class="config-field" style="width: 140px;">
            <label class="config-label">判断题</label>
            <el-input-number v-model="quizForm.judgeCount" :min="0" :max="10" style="width: 100%;" />
          </div>
          <div class="config-field" style="width: 140px;">
            <label class="config-label">简答题</label>
            <el-input-number v-model="quizForm.shortAnswerCount" :min="0" :max="5" style="width: 100%;" />
          </div>
        </div>
        <div class="config-actions">
          <el-button
            type="primary"
            size="large"
            :loading="generating"
            :disabled="!quizForm.materialId"
            @click="handleGenerate"
          >
            <el-icon><MagicStick /></el-icon>
            生成题目
          </el-button>
        </div>
      </div>

      <!-- Question List -->
      <div v-if="questions.length > 0" class="questions-area">
        <div class="questions-summary">
          <span>共 {{ questions.length }} 题</span>
          <el-button v-if="!showAnswers" type="success" @click="handleSubmit">提交答案</el-button>
          <el-button v-else type="primary" @click="handleReset">重新答题</el-button>
        </div>

        <div
          v-for="(q, index) in questions"
          :key="index"
          class="question-card"
          :class="getQuestionStatusClass(q, index)"
        >
          <div class="question-header">
            <div class="question-index">{{ index + 1 }}</div>
            <div class="question-tags">
              <el-tag size="small" :type="getTypeTag(q.type)">{{ getTypeLabel(q.type) }}</el-tag>
              <el-tag size="small" :type="getDifficultyTag(q.difficulty)">{{ getDifficultyLabel(q.difficulty) }}</el-tag>
            </div>
          </div>

          <div class="question-text">{{ q.question }}</div>

          <!-- Choice -->
          <div v-if="q.type === 'choice'" class="question-options">
            <el-radio-group v-model="userAnswers[index]">
              <div v-for="(val, key) in q.options" :key="key" class="option-item">
                <el-radio :value="key">{{ key }}. {{ val }}</el-radio>
              </div>
            </el-radio-group>
          </div>

          <!-- Judge -->
          <div v-else-if="q.type === 'judge'" class="question-options">
            <el-radio-group v-model="userAnswers[index]">
              <el-radio value="true" style="margin-right: var(--space-6);">正确</el-radio>
              <el-radio value="false">错误</el-radio>
            </el-radio-group>
          </div>

          <!-- Short Answer -->
          <div v-else-if="q.type === 'short_answer'" class="question-options">
            <el-input
              v-model="userAnswers[index]"
              type="textarea"
              :rows="3"
              placeholder="请输入你的答案"
            />
          </div>

          <!-- Answer Section -->
          <div v-if="showAnswers" class="answer-section">
            <div class="answer-result" :class="isCorrect(q, index) ? 'result-correct' : 'result-wrong'">
              <el-icon :size="14"><Check v-if="isCorrect(q, index)" /><Close v-else /></el-icon>
              <span>{{ isCorrect(q, index) ? '回答正确' : '回答错误' }}</span>
            </div>
            <div class="answer-detail">
              <div class="answer-row">
                <span class="answer-label">正确答案</span>
                <span class="answer-value">{{ q.answer }}</span>
              </div>
              <div v-if="q.explanation" class="answer-row">
                <span class="answer-label">解析</span>
                <span class="answer-value">{{ q.explanation }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="submit-area">
          <el-button v-if="!showAnswers" type="success" size="large" @click="handleSubmit">提交答案</el-button>
          <el-button v-else type="primary" size="large" @click="handleReset">重新答题</el-button>
        </div>
      </div>

      <!-- Empty State -->
      <div v-else class="quiz-empty">
        <AppEmpty
          icon="EditPen"
          title="选择资料开始出题"
          description="AI 将根据学习资料自动生成练习题"
          compact
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Check, Close } from '@element-plus/icons-vue'
import { generateQuiz, submitAnswers } from '@/api/ai'
import { loadReadyMaterials } from '@/api/material'
import AppEmpty from '@/components/common/AppEmpty.vue'

const materialList = ref([])
const questions = ref([])
const userAnswers = ref([])
const showAnswers = ref(false)
const generating = ref(false)
const batchId = ref('')

const quizForm = reactive({
  materialId: '',
  choiceCount: 5,
  judgeCount: 3,
  shortAnswerCount: 2,
  difficulty: 'medium'
})

function getTypeTag(type) {
  const map = { choice: 'primary', judge: 'success', short_answer: 'warning' }
  return map[type] || 'info'
}

function getTypeLabel(type) {
  const map = { choice: '单选题', judge: '判断题', short_answer: '简答题' }
  return map[type] || type
}

function getDifficultyTag(difficulty) {
  const map = { easy: 'success', medium: 'warning', hard: 'danger' }
  return map[difficulty] || 'info'
}

function getDifficultyLabel(difficulty) {
  const map = { easy: '简单', medium: '中等', hard: '困难' }
  return map[difficulty] || difficulty
}

function getQuestionStatusClass(q, index) {
  if (!showAnswers.value) return ''
  return isCorrect(q, index) ? 'status-correct' : 'status-wrong'
}

function isCorrect(q, index) {
  const userAnswer = userAnswers.value[index]
  if (!userAnswer) return false
  return userAnswer.toString().toLowerCase() === q.answer.toString().toLowerCase()
}

async function handleGenerate() {
  if (!quizForm.materialId) {
    ElMessage.warning('请先选择资料')
    return
  }
  generating.value = true
  try {
    const data = await generateQuiz(quizForm.materialId, quizForm)
    questions.value = data.questions || []
    batchId.value = data.batchId || ''
    userAnswers.value = new Array(questions.value.length).fill('')
    showAnswers.value = false
    ElMessage.success('题目生成成功')
  } catch (error) {
    questions.value = []
  } finally {
    generating.value = false
  }
}

async function handleSubmit() {
  const unanswered = userAnswers.value.findIndex((a) => !a && a !== 0)
  if (unanswered !== -1) {
    ElMessage.warning(`请完成第 ${unanswered + 1} 题`)
    return
  }

  try {
    const answers = questions.value.map((q, i) => ({
      questionId: q.id || i,
      answer: userAnswers.value[i]
    }))
    if (batchId.value) {
      await submitAnswers(batchId.value, answers)
    }
    showAnswers.value = true
    ElMessage.success('答案已提交')
  } catch (error) {
    // handled by interceptor
  }
}

function handleReset() {
  userAnswers.value = new Array(questions.value.length).fill('')
  showAnswers.value = false
}

onMounted(async () => {
  materialList.value = await loadReadyMaterials()
})
</script>

<style scoped>
.quiz-page {
  width: 100%;
}

.quiz-card {
  border-radius: var(--radius-lg);
}

.quiz-card :deep(.el-card__body) {
  padding: var(--space-5);
}

.quiz-config {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--outline-variant);
}

.config-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
}

.config-field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  flex: 1;
  min-width: 140px;
}

.config-label {
  font-size: var(--text-ui);
  font-weight: 500;
  color: var(--color-text-secondary);
}

.config-actions {
  display: flex;
  justify-content: flex-end;
}

.questions-area {
  margin-top: var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.questions-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}

.question-card {
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  background: var(--surface-card);
  transition: border-color var(--duration-fast) var(--ease-default);
}

.question-card.status-correct {
  border-color: var(--color-success);
  background: #f0fdf4;
}

.question-card.status-wrong {
  border-color: var(--color-error);
  background: #fff1f2;
}

.question-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.question-index {
  width: 28px;
  height: 28px;
  background: var(--surface-container);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-small);
  font-weight: 600;
  color: var(--color-text-secondary);
}

.question-tags {
  display: flex;
  gap: var(--space-2);
}

.question-text {
  font-size: var(--text-body);
  line-height: 1.7;
  color: var(--color-text-primary);
  margin-bottom: var(--space-4);
}

.question-options {
  margin-bottom: var(--space-4);
}

.option-item {
  margin: var(--space-2) 0;
}

.answer-section {
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--outline-variant);
}

.answer-result {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--text-small);
  font-weight: 600;
  margin-bottom: var(--space-3);
}

.result-correct {
  background: #dcfce7;
  color: #16a34a;
}

.result-wrong {
  background: #ffe4e6;
  color: #e11d48;
}

.answer-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.answer-row {
  display: flex;
  gap: var(--space-2);
}

.answer-label {
  font-size: var(--text-small);
  font-weight: 600;
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.answer-value {
  font-size: var(--text-small);
  color: var(--color-text-primary);
  line-height: 1.6;
}

.submit-area {
  display: flex;
  justify-content: center;
  padding: var(--space-4) 0;
}

.quiz-empty :deep(.app-empty) {
  padding: var(--space-16) 0;
}

@media (max-width: 767px) {
  .config-row {
    flex-direction: column;
  }

  .config-field {
    width: 100% !important;
  }
}
</style>
