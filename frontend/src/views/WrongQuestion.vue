<template>
  <div class="wrong-question-page">
    <BasePageHeader
      title="错题本"
      description="回顾错题，针对性复习，巩固薄弱知识点"
    />

    <!-- Stats Panel -->
    <BaseCard v-if="stats" class="stats-card">
      <template #header>
        <div class="stats-header">
          <span class="stats-title">错题统计</span>
          <div class="stats-filter">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              size="small"
              style="width: 260px;"
              @change="loadStats"
            />
          </div>
        </div>
      </template>

      <!-- Summary -->
      <div class="stats-summary">
        <div class="stat-item">
          <div class="stat-value">{{ stats.totalCount }}</div>
          <div class="stat-label">总错题</div>
        </div>
        <div class="stat-item warn">
          <div class="stat-value">{{ stats.unmasteredCount }}</div>
          <div class="stat-label">未掌握</div>
        </div>
        <div class="stat-item success">
          <div class="stat-value">{{ stats.masteredCount }}</div>
          <div class="stat-label">已掌握</div>
        </div>
      </div>

      <!-- Charts Row -->
      <div class="charts-row">
        <!-- Type Distribution -->
        <div class="chart-box">
          <div class="chart-title">题型分布</div>
          <div class="bar-chart">
            <div v-for="(count, type) in stats.byType" :key="type" class="bar-item">
              <span class="bar-label">{{ getTypeLabel(type) }}</span>
              <div class="bar-track">
                <div class="bar-fill" :style="{ width: getBarWidth(count, typeMax) + '%' }"></div>
              </div>
              <span class="bar-value">{{ count }}</span>
            </div>
          </div>
        </div>

        <!-- Material Distribution -->
        <div class="chart-box">
          <div class="chart-title">来源分布 Top5</div>
          <div class="bar-chart">
            <div v-for="item in topMaterials" :key="item.name" class="bar-item">
              <span class="bar-label source-label">{{ item.name }}</span>
              <div class="bar-track">
                <div class="bar-fill source-fill" :style="{ width: getBarWidth(item.count, materialMax) + '%' }"></div>
              </div>
              <span class="bar-value">{{ item.count }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Date Trend -->
      <div v-if="dateTrend.length > 1" class="trend-section">
        <div class="chart-title">错题趋势（最近30天）</div>
        <svg class="line-chart" :viewBox="`0 0 ${svgWidth} ${svgHeight}`" preserveAspectRatio="none">
          <!-- Grid -->
          <line v-for="i in 4" :key="'g'+i"
            :x1="padding" :y1="padding + (i-1) * gridStep"
            :x2="svgWidth - padding" :y2="padding + (i-1) * gridStep"
            stroke="var(--outline-variant)" stroke-width="0.5" />
          <!-- Line -->
          <polyline :points="trendPoints" fill="none" stroke="var(--color-primary)" stroke-width="2" stroke-linejoin="round" />
          <!-- Dots -->
          <circle v-for="(pt, i) in trendDots" :key="'d'+i"
            :cx="pt.x" :cy="pt.y" r="3" fill="var(--color-primary)" />
          <!-- X labels -->
          <text v-for="(pt, i) in trendDots" :key="'xl'+i"
            v-if="i % Math.max(1, Math.floor(dateTrend.length / 7)) === 0"
            :x="pt.x" :y="svgHeight - 2"
            text-anchor="middle" font-size="10" fill="var(--color-text-secondary)">
            {{ dateTrend[i].date.slice(5) }}
          </text>
        </svg>
      </div>
    </BaseCard>

    <!-- Main Content -->
    <BaseCard class="wrong-question-card" :padding="'none'">
      <!-- Toolbar -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="queryParams.materialId" placeholder="全部资料" clearable style="width: 220px;" @change="handleSearch">
            <el-option
              v-for="item in materialList"
              :key="item.id"
              :label="item.originalName"
              :value="item.id"
            />
          </el-select>
          <el-select v-model="queryParams.isMastered" placeholder="全部状态" clearable style="width: 140px;" @change="handleSearch">
            <el-option label="未掌握" :value="false" />
            <el-option label="已掌握" :value="true" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button text @click="handleReset">重置</el-button>
          <el-button
            :disabled="selectedWrongQuestions.length === 0"
            :loading="exportSelectedLoading"
            @click="handleExportSelectedPdf"
          >
            导出选中（{{ selectedWrongQuestions.length }}）
          </el-button>
          <el-button @click="handleExportPdf" :loading="exportLoading">
            导出全部 PDF
          </el-button>
          <el-button type="primary" @click="handleRepractice" :loading="repracticeLoading">
            重做错题
          </el-button>
        </div>
      </div>

      <!-- Table -->
      <el-table
        :data="wrongList"
        v-loading="loading"
        class="wrong-table"
        @selection-change="handleWrongSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column label="题型" width="90">
          <template #default="{ row }">
            <span class="type-tag">{{ getTypeLabel(row.questionType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="题目" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="question-text">{{ row.question }}</span>
          </template>
        </el-table-column>
        <el-table-column label="你的答案" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="answer-wrong">{{ row.userAnswer }}</span>
          </template>
        </el-table-column>
        <el-table-column label="正确答案" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="answer-correct">{{ row.correctAnswer }}</span>
          </template>
        </el-table-column>
        <el-table-column label="错误次数" width="100" align="center">
          <template #default="{ row }">
            <span class="wrong-count">{{ row.wrongCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="source-text">{{ row.materialName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button size="small" text type="primary" @click="handleView(row)">详情</el-button>
              <el-button
                v-if="!row.isMastered"
                size="small"
                text
                type="success"
                @click="handleMaster(row)"
              >
                标记掌握
              </el-button>
              <span v-else class="mastered-badge">已掌握</span>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- Empty -->
      <div v-if="!loading && wrongList.length === 0" class="table-empty">
        <AppEmpty
          icon="Notebook"
          title="暂无错题"
          description="完成 AI 出题练习，答错的题目会自动收录"
          compact
        />
      </div>

      <!-- Pagination -->
      <div v-if="total > 0" class="table-footer">
        <span class="total-text">共 {{ total }} 道错题</span>
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="sizes, prev, pager, next"
          @size-change="handleSearch"
          @current-change="handleSearch"
        />
      </div>
    </BaseCard>

    <!-- Detail Drawer -->
    <el-drawer v-model="showDetail" title="错题详情" size="480px">
      <div v-if="currentQuestion" class="detail-content">
        <div class="detail-section">
          <div class="detail-label">题目</div>
          <div class="detail-value">{{ currentQuestion.question }}</div>
        </div>
        <div class="detail-row">
          <div class="detail-section">
            <div class="detail-label">你的答案</div>
            <div class="detail-value answer-wrong">{{ currentQuestion.userAnswer }}</div>
          </div>
          <div class="detail-section">
            <div class="detail-label">正确答案</div>
            <div class="detail-value answer-correct">{{ currentQuestion.correctAnswer }}</div>
          </div>
        </div>
        <div class="detail-section">
          <div class="detail-label">题型</div>
          <div class="detail-value">{{ getTypeLabel(currentQuestion.questionType) }}</div>
        </div>
        <div class="detail-section">
          <div class="detail-label">错误次数</div>
          <div class="detail-value">{{ currentQuestion.wrongCount }}</div>
        </div>
        <div v-if="currentQuestion.explanation" class="detail-section">
          <div class="detail-label">解析</div>
          <div class="detail-value">{{ currentQuestion.explanation }}</div>
        </div>
      </div>
    </el-drawer>

    <!-- Repractice Dialog (Flashcard Mode) -->
    <el-dialog
      v-model="repracticeMode"
      title="错题重做 (记忆卡片)"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
      @close="exitRepractice"
    >
      <div v-if="repracticeList.length > 0 && repracticeIndex < repracticeList.length" class="flashcard-container">
        <div class="flashcard-header">
          <span class="progress-text">进度：{{ repracticeIndex + 1 }} / {{ repracticeList.length }}</span>
          <span class="type-tag">{{ getTypeLabel(repracticeList[repracticeIndex].questionType) }}</span>
        </div>
        
        <div class="flashcard-question">
          {{ repracticeList[repracticeIndex].question }}
        </div>
        
        <div v-if="!repracticeFlipped" class="flashcard-input-area">
          <div v-if="repracticeList[repracticeIndex].options" class="choice-actions">
            <el-checkbox-group v-if="repracticeList[repracticeIndex].questionType === 'multi_choice'" v-model="currentMultiChoiceAnswer">
              <el-checkbox v-for="(val, key) in repracticeList[repracticeIndex].options" :key="key" :label="key" :value="key" class="option-item">
                {{ key }}. {{ val }}
              </el-checkbox>
            </el-checkbox-group>
            <el-radio-group v-else v-model="currentRepracticeAnswer">
              <el-radio v-for="(val, key) in repracticeList[repracticeIndex].options" :key="key" :label="key" :value="key" class="option-item">
                {{ key }}. {{ val }}
              </el-radio>
            </el-radio-group>
          </div>
          <div v-else-if="repracticeList[repracticeIndex].questionType === 'judge'" class="judge-actions">
            <el-button @click="currentRepracticeAnswer = 'T'" :type="currentRepracticeAnswer === 'T' ? 'primary' : 'default'">正确 (T)</el-button>
            <el-button @click="currentRepracticeAnswer = 'F'" :type="currentRepracticeAnswer === 'F' ? 'primary' : 'default'">错误 (F)</el-button>
          </div>
          <div v-else class="text-input-actions">
            <el-input 
              v-model="currentRepracticeAnswer" 
              :type="repracticeList[repracticeIndex].questionType === 'short_answer' ? 'textarea' : 'text'"
              :rows="3"
              placeholder="请输入你的答案（选择题可直接输入选项字母）"
            />
          </div>
          <div class="flashcard-action" style="margin-top: 20px;">
            <el-button type="primary" size="large" @click="checkRepracticeAnswer" :disabled="!currentRepracticeAnswer">提交验证</el-button>
          </div>
        </div>
        
        <div v-else class="flashcard-answer-box">
          <div class="repractice-result" :class="{ correct: isRepracticeCorrect, wrong: !isRepracticeCorrect }">
            <el-icon :size="24">
              <CircleCheckFilled v-if="isRepracticeCorrect" />
              <CircleCloseFilled v-else />
            </el-icon>
            <span>{{ isRepracticeCorrect ? '回答正确！' : '回答错误' }}</span>
          </div>
          <div class="detail-row">
            <div class="detail-section">
              <div class="detail-label">你的回答</div>
              <div class="detail-value" :class="{ 'answer-wrong': !isRepracticeCorrect, 'answer-correct': isRepracticeCorrect }">{{ getDisplayAnswer(repracticeList[repracticeIndex]) }}</div>
            </div>
            <div class="detail-section">
              <div class="detail-label">正确答案</div>
              <div class="detail-value answer-correct">{{ repracticeList[repracticeIndex].correctAnswer }}</div>
            </div>
          </div>
          <div class="flashcard-actions">
            <el-button v-if="!isRepracticeCorrect" @click="nextRepracticeQuestion(false)">还需要复习</el-button>
            <el-button type="success" @click="nextRepracticeQuestion(true)">已掌握</el-button>
          </div>
        </div>
      </div>
      <div v-else class="flashcard-finish">
        <el-icon :size="48" color="var(--color-success)"><CircleCheckFilled /></el-icon>
        <h3>本轮复习完成！</h3>
        <el-button type="primary" @click="repracticeMode = false">关闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getWrongQuestions, markWrongQuestionMastered, getRepracticeQuestions, getWrongQuestionStats, exportWrongQuestionsPdf, exportSelectedWrongQuestionsPdf } from '@/api/quiz'
import { loadAvailableMaterials } from '@/api/material'
import { CircleCheckFilled, CircleCloseFilled } from '@element-plus/icons-vue'
import BaseCard from '@/components/common/BaseCard.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'

// ========== 基础数据 ==========
const loading = ref(false)
const wrongList = ref([])
const total = ref(0)
const materialList = ref([])
const showDetail = ref(false)
const currentQuestion = ref(null)
const selectedWrongQuestions = ref([])

const queryParams = reactive({
  materialId: '',
  isMastered: '',
  page: 1,
  size: 10
})

// ========== 统计数据 ==========
const stats = ref(null)
const dateRange = ref([])

// ========== 重做模式 ==========
const repracticeMode = ref(false)
const repracticeLoading = ref(false)
const repracticeList = ref([])
const repracticeIndex = ref(0)
const repracticeFlipped = ref(false)
const currentRepracticeAnswer = ref('')
const currentMultiChoiceAnswer = ref([])
const isRepracticeCorrect = ref(false)

// ========== 导出 ==========
const exportLoading = ref(false)
const exportSelectedLoading = ref(false)

// 统计图表辅助
const typeMax = computed(() => {
  if (!stats.value?.byType) return 1
  return Math.max(...Object.values(stats.value.byType), 1)
})

const materialMax = computed(() => {
  if (!stats.value?.byMaterial) return 1
  const sorted = Object.entries(stats.value.byMaterial)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
  return Math.max(...sorted.map(e => e[1]), 1)
})

const topMaterials = computed(() => {
  if (!stats.value?.byMaterial) return []
  return Object.entries(stats.value.byMaterial)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([name, count]) => ({ name: name.length > 12 ? name.slice(0, 12) + '…' : name, count }))
})

// SVG 趋势图参数
const svgWidth = 600
const svgHeight = 120
const padding = 30
const gridStep = (svgHeight - padding * 2) / 3

const dateTrend = computed(() => {
  if (!stats.value?.byDate) return []
  return Object.entries(stats.value.byDate)
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([date, count]) => ({ date, count }))
})

const trendPoints = computed(() => {
  return trendDots.value.map(pt => `${pt.x},${pt.y}`).join(' ')
})

const trendDots = computed(() => {
  const data = dateTrend.value
  if (data.length === 0) return []
  const maxCount = Math.max(...data.map(d => d.count), 1)
  const chartW = svgWidth - padding * 2
  const chartH = svgHeight - padding * 2

  return data.map((d, i) => ({
    x: padding + (i / Math.max(data.length - 1, 1)) * chartW,
    y: padding + chartH - (d.count / maxCount) * chartH
  }))
})

// ========== 方法 ==========
function getTypeLabel(type) {
  const map = {
    choice: '单选', judge: '判断', short_answer: '简答',
    fill_blank: '填空', multi_choice: '多选', math_fill: '数学'
  }
  return map[type] || type
}

function getBarWidth(count, max) {
  return max > 0 ? (count / max) * 100 : 0
}

async function loadMaterials() {
  try {
    materialList.value = await loadAvailableMaterials()
  } catch {
    materialList.value = []
  }
}

async function loadStats() {
  try {
    const params = {}
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0].toISOString().split('T')[0]
      params.endDate = dateRange.value[1].toISOString().split('T')[0]
    }
    stats.value = await getWrongQuestionStats(params)
  } catch {
    stats.value = null
  }
}

async function handleSearch() {
  loading.value = true
  try {
    const params = { ...queryParams }
    if (params.isMastered === '') delete params.isMastered
    const data = await getWrongQuestions(params)
    if (Array.isArray(data)) {
      wrongList.value = data
      total.value = data.length
    } else {
      wrongList.value = data.records || []
      total.value = data.total || 0
    }
    selectedWrongQuestions.value = []
  } catch {
    wrongList.value = []
    selectedWrongQuestions.value = []
  } finally {
    loading.value = false
  }
}

function handleReset() {
  queryParams.materialId = ''
  queryParams.isMastered = ''
  queryParams.page = 1
  handleSearch()
}

function handleView(row) {
  currentQuestion.value = row
  showDetail.value = true
}

async function handleMaster(row) {
  try {
    await markWrongQuestionMastered(row.id)
    ElMessage.success('已标记为掌握')
    handleSearch()
    loadStats()
  } catch {
    // handled by interceptor
  }
}

async function handleRepractice() {
  repracticeLoading.value = true
  try {
    const data = await getRepracticeQuestions(10)
    if (Array.isArray(data) && data.length > 0) {
      repracticeList.value = data
      repracticeIndex.value = 0
      repracticeFlipped.value = false
      currentRepracticeAnswer.value = ''
      currentMultiChoiceAnswer.value = []
      isRepracticeCorrect.value = false
      repracticeMode.value = true
    } else {
      ElMessage.info('没有未掌握的错题')
    }
  } catch {
    // handled by interceptor
  } finally {
    repracticeLoading.value = false
  }
}

function checkRepracticeAnswer() {
  const q = repracticeList.value[repracticeIndex.value]
  const refAnswer = String(q.correctAnswer).trim().toLowerCase()
  let userAns = ''
  
  if (q.questionType === 'multi_choice' && q.options) {
    userAns = currentMultiChoiceAnswer.value.sort().join(',').toLowerCase()
  } else {
    userAns = String(currentRepracticeAnswer.value).trim().toLowerCase()
  }
  
  if (q.questionType === 'multi_choice') {
    const refSet = new Set(refAnswer.split(/[,，、\s]+/))
    const userSet = new Set(userAns.split(/[,，、\s]+/))
    isRepracticeCorrect.value = refSet.size > 0 && refSet.size === userSet.size && [...refSet].every(x => userSet.has(x))
  } else {
    isRepracticeCorrect.value = refAnswer === userAns
  }
  repracticeFlipped.value = true
}

function getDisplayAnswer(q) {
  if (q.questionType === 'multi_choice' && q.options) {
    return currentMultiChoiceAnswer.value.sort().join(', ')
  }
  return currentRepracticeAnswer.value
}

async function nextRepracticeQuestion(mastered) {
  if (mastered) {
    try {
      await markWrongQuestionMastered(repracticeList.value[repracticeIndex.value].id)
      ElMessage.success('已标记为掌握')
      // 后台更新列表
      handleSearch()
      loadStats()
    } catch {
      // API 报错的话直接返回不往下走，如果不需要阻塞也可以继续。这里选择如果异常就不进入下一题
      return
    }
  }
  repracticeIndex.value++
  repracticeFlipped.value = false
  currentRepracticeAnswer.value = ''
  currentMultiChoiceAnswer.value = []
  isRepracticeCorrect.value = false
}

function exitRepractice() {
  repracticeMode.value = false
  repracticeList.value = []
  repracticeIndex.value = 0
  currentRepracticeAnswer.value = ''
  currentMultiChoiceAnswer.value = []
  isRepracticeCorrect.value = false
}

function handleWrongSelectionChange(selection) {
  selectedWrongQuestions.value = selection
}

function downloadPdfBlob(blob, filename) {
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

async function handleExportSelectedPdf() {
  if (selectedWrongQuestions.value.length === 0) {
    ElMessage.warning('请先选择要导出的错题')
    return
  }

  exportSelectedLoading.value = true
  try {
    const ids = selectedWrongQuestions.value.map(item => item.id)
    const blob = await exportSelectedWrongQuestionsPdf(ids)
    const date = new Date().toISOString().split('T')[0]
    downloadPdfBlob(blob, `错题本-选中题目-${date}.pdf`)
    ElMessage.success('PDF 导出成功')
  } catch (error) {
    if (!error?.message) ElMessage.error('PDF 导出失败')
  } finally {
    exportSelectedLoading.value = false
  }
}

async function handleExportPdf() {
  exportLoading.value = true
  try {
    const blob = await exportWrongQuestionsPdf()
    const date = new Date().toISOString().split('T')[0]
    downloadPdfBlob(blob, `错题本-全部-${date}.pdf`)
    ElMessage.success('PDF 导出成功')
  } catch (error) {
    if (!error?.message) ElMessage.error('PDF 导出失败')
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => {
  loadMaterials()
  handleSearch()
  loadStats()
})
</script>

<style scoped>
.wrong-question-page {
  width: 100%;
}

/* ========== Stats ========== */
.stats-card {
  margin-bottom: var(--space-6);
  border-radius: var(--radius-lg);
}

.stats-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.stats-title {
  font-size: var(--text-h3);
  font-weight: 600;
  color: var(--color-text-primary);
}

.stats-summary {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
}

.stat-item {
  text-align: left;
  padding: var(--space-3) var(--space-5);
  background: var(--surface-container);
  border-radius: var(--radius-md);
  min-width: 112px;
}

.stat-item.warn .stat-value {
  color: var(--color-warning);
}

.stat-item.success .stat-value {
  color: var(--color-success);
}

.stat-value {
  font-size: var(--text-h2);
  font-weight: 700;
  color: var(--color-text-primary);
}

.stat-label {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
  margin-top: var(--space-1);
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-5);
  margin-bottom: var(--space-4);
}

.chart-box {
  background: var(--surface-container-low);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

.chart-title {
  font-size: var(--text-small);
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: var(--space-3);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.bar-chart {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.bar-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.bar-label {
  width: 48px;
  font-size: var(--text-small);
  color: var(--color-text-secondary);
  text-align: right;
  flex-shrink: 0;
}

.source-label {
  width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bar-track {
  flex: 1;
  height: 16px;
  background: var(--surface-container-low);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: var(--radius-sm);
  transition: width 0.4s ease;
  min-width: 2px;
}

.source-fill {
  background: var(--color-success);
}

.bar-value {
  width: 32px;
  font-size: var(--text-small);
  font-weight: 600;
  color: var(--color-text-primary);
  text-align: left;
}

.trend-section {
  margin-top: var(--space-3);
}

.line-chart {
  width: 100%;
  height: 120px;
  margin-top: var(--space-2);
}

/* ========== Main Card ========== */
.wrong-question-card {
  border-radius: var(--radius-lg);
}

.wrong-question-card :deep(.card-body) {
  padding: var(--space-5);
}

.wrong-table {
  margin-top: var(--space-4);
}

.type-tag {
  font-size: var(--text-small);
  font-weight: 500;
  color: var(--color-text-secondary);
  background: var(--surface-container);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
}

.question-text {
  font-size: var(--text-body);
  color: var(--color-text-primary);
}

.answer-wrong {
  color: var(--color-error);
  font-weight: 500;
}

.answer-correct {
  color: var(--color-success);
  font-weight: 500;
}

.wrong-count {
  font-weight: 600;
  color: var(--color-error);
}

.source-text {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
}

.mastered-badge {
  font-size: var(--text-small);
  color: var(--color-success);
  font-weight: 500;
  padding: 0 var(--space-2);
}

.table-empty {
  border-top: 1px solid var(--outline-variant);
}

.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--outline-variant);
}

.total-text {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
}

/* ========== Repractice Banner ========== */
.repractice-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  margin: var(--space-4) var(--space-5) 0;
  background: var(--color-primary-container);
  border-radius: var(--radius-md);
}

.repractice-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--text-body);
  color: var(--color-on-primary-container);
}

.repractice-tag {
  font-size: var(--text-small);
  font-weight: 600;
  background: var(--color-primary);
  color: var(--color-on-primary);
  padding: 2px 10px;
  border-radius: var(--radius-sm);
}

/* ========== Detail Drawer ========== */
.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.detail-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.detail-label {
  font-size: var(--text-small);
  font-weight: 600;
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.04em;
}

.detail-value {
  font-size: var(--text-body);
  color: var(--color-text-primary);
  line-height: 1.6;
}

@media (max-width: 767px) {
  .stats-summary {
    flex-wrap: wrap;
  }

  .charts-row {
    grid-template-columns: 1fr;
  }

  .table-footer {
    flex-direction: column;
    gap: var(--space-3);
    align-items: flex-start;
  }
}

/* ========== Flashcard Dialog ========== */
.flashcard-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  min-height: 250px;
}

.flashcard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--outline-variant);
}

.progress-text {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
  font-weight: 500;
}

.flashcard-question {
  font-size: var(--text-h3);
  color: var(--color-text-primary);
  line-height: 1.6;
  font-weight: 500;
  margin-top: var(--space-2);
}

.flashcard-input-area {
  margin-top: auto;
  padding-top: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.judge-actions {
  display: flex;
  gap: var(--space-4);
  justify-content: center;
}

.text-input-actions {
  width: 100%;
}

.choice-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  width: 100%;
}

.choice-actions :deep(.el-radio-group),
.choice-actions :deep(.el-checkbox-group) {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  align-items: flex-start;
  width: 100%;
}

.option-item {
  width: 100%;
  margin-right: 0 !important;
  white-space: normal;
  height: auto;
  padding: var(--space-3);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-md);
  transition: all 0.2s;
}

.option-item:hover {
  background-color: var(--surface-container-high);
}

.option-item.is-checked {
  border-color: var(--color-primary);
  background-color: rgba(var(--color-primary-rgb), 0.05);
}

.flashcard-action {
  text-align: center;
}

.flashcard-answer-box {
  margin-top: auto;
  background: var(--surface-container);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
  animation: fadeIn 0.4s ease;
}

.repractice-result {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-h3);
  font-weight: 600;
}

.repractice-result.correct {
  color: var(--color-success);
}

.repractice-result.wrong {
  color: var(--color-error);
}

.flashcard-actions {
  display: flex;
  gap: var(--space-3);
  justify-content: flex-end;
  border-top: 1px solid var(--outline-variant);
  padding-top: var(--space-4);
}

.flashcard-finish {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  padding: var(--space-6) 0;
  color: var(--color-text-primary);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
