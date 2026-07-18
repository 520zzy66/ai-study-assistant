<template>
  <div class="dashboard-page">
    <!-- Welcome -->
    <div class="welcome-section">
      <div class="welcome-text">
        <h1 class="welcome-title">{{ greeting }}，{{ userStore.userInfo?.nickname || '同学' }}</h1>
        <p class="welcome-date">{{ todayDate }}</p>
      </div>
      <p class="welcome-slogan">持续学习，每天进步一点点</p>
    </div>

    <!-- Stats Grid -->
    <div class="stats-grid">
      <BaseStatisticCard
        v-for="stat in stats"
        :key="stat.key"
        :icon="stat.icon"
        :icon-bg="stat.iconBg"
        :icon-color="stat.iconColor"
        :label="stat.label"
        :value="stat.value"
        :trend="stat.trend"
        :trend-value="stat.trendValue"
        :loading="loading.stats"
        clickable
        :to="stat.path"
      />
    </div>

    <!-- Content Grid: 2fr main + 1fr side -->
    <div class="dashboard-grid">
      <!-- Main Column -->
      <div class="dashboard-main">
        <!-- Progress & Charts Row -->
        <div class="progress-row">
          <!-- Study Progress Ring -->
          <BaseCard title="学习进度" class="progress-card" :loading="loading.progress">
            <div class="ring-center">
              <ProgressRing
                :percentage="studyProgress"
                :size="140"
                :stroke-width="10"
                color="var(--color-primary)"
                bg-color="var(--surface-container)"
              />
              <p class="ring-hint">{{ progressHint }}</p>
              <el-button
                v-if="!latestPlan"
                text
                type="primary"
                size="small"
                @click="navigateTo('/ai/plan')"
              >
                去制定计划
              </el-button>
            </div>
          </BaseCard>

          <!-- Exam Countdown -->
          <BaseCard class="countdown-card" :loading="loading.progress">
            <CountdownTimer
              :title="countdownTitle"
              :target-date="examDate"
              :total-days="planTotalDays"
            />
          </BaseCard>

          <!-- Weekly Activity -->
          <BaseCard title="本周学习活跃" class="chart-card" :loading="loading.activity">
            <AreaChart :data="weeklyActivity" :height="200" />
          </BaseCard>
        </div>

        <!-- Quick Actions -->
        <BaseCard title="快速开始">
          <div class="quick-actions">
            <button
              v-for="action in quickActions"
              :key="action.path"
              class="quick-action-btn"
              type="button"
              @click="navigateTo(action.path)"
            >
              <div class="qa-icon" :style="{ background: action.bg, color: action.color }">
                <el-icon :size="22"><component :is="action.icon" /></el-icon>
              </div>
              <span class="qa-label">{{ action.label }}</span>
            </button>
          </div>
        </BaseCard>

        <!-- Personalized Recommendations -->
        <BaseCard title="为你推荐" :loading="loading.recommendations">
          <template v-if="recommendations.length > 0" #header-action>
            <el-button text type="primary" size="small" @click="$router.push('/ai/resource-package')">资源工坊</el-button>
          </template>
          <div v-if="recommendations.length > 0" class="recommendation-grid">
            <button
              v-for="item in recommendations"
              :key="item.key"
              class="recommendation-item"
              type="button"
              @click="navigateTo(item.path)"
            >
              <div class="recommendation-icon" :style="{ background: item.bg, color: item.color }">
                <el-icon :size="20"><component :is="item.icon" /></el-icon>
              </div>
              <div class="recommendation-content">
                <div class="recommendation-title">{{ item.title }}</div>
                <p>{{ item.description }}</p>
              </div>
              <el-icon :size="16" class="recommendation-arrow"><ArrowRight /></el-icon>
            </button>
          </div>
          <div v-else-if="!loading.recommendations" class="recommendation-empty">
            上传资料并完善学习画像后，首页会自动推送更适合你的资源入口
          </div>
        </BaseCard>

        <!-- Continue Learning -->
        <BaseCard title="继续学习" :loading="loading.materials" :isEmpty="recentMaterials.length === 0 && !loading.materials"
          empty-icon="Document" empty-text="上传你的第一份资料，开始学习之旅">
          <template v-if="!loading.materials && recentMaterials.length > 0" #header-action>
            <el-button text type="primary" size="small" @click="$router.push('/material')">查看全部</el-button>
          </template>
          <div v-if="recentMaterials.length > 0" class="learning-list">
            <button
              v-for="item in recentMaterials"
              :key="item.id"
              class="learning-item"
              type="button"
              @click="navigateTo(materialEntryPath(item))"
            >
              <div class="learning-icon">
                <el-icon :size="20"><Document /></el-icon>
              </div>
              <div class="learning-info">
                <div class="learning-name truncate">{{ materialDisplayName(item) }}</div>
                <div class="learning-meta">{{ materialStatusText(item) }} · {{ formatDate(item.createTime) }}</div>
              </div>
              <el-icon :size="16" class="learning-arrow"><ArrowRight /></el-icon>
            </button>
          </div>
        </BaseCard>
      </div>

      <!-- Side Column -->
      <div class="dashboard-side">
        <!-- Today's Tasks -->
        <BaseCard title="今日任务" :loading="loading.plan" :isEmpty="todayTasks.length === 0 && !loading.plan"
          empty-icon="Calendar" empty-text="制定学习计划，让学习更有节奏">
          <template #header-action>
            <el-button text type="primary" size="small" @click="$router.push('/ai/plan')">
              {{ todayTasks.length > 0 ? '查看计划' : '创建计划' }}
            </el-button>
          </template>
          <div v-if="todayTasks.length > 0" class="task-list">
            <label
              v-for="task in todayTasks"
              :key="task.id"
              class="task-item"
              :class="{ completed: task.completed }"
            >
              <el-checkbox
                :model-value="task.completed"
                :disabled="task.updating"
                @change="(value) => toggleTask(task, value)"
              >
                <span class="task-title">{{ task.title }}</span>
              </el-checkbox>
              <span class="task-time">{{ task.estimatedMinutes }} 分钟</span>
            </label>
          </div>
        </BaseCard>

        <!-- Recent Activity -->
        <BaseCard title="最近动态" :loading="loading.activity" :isEmpty="recentActivities.length === 0 && !loading.activity"
          empty-icon="Clock" empty-text="开始使用 AI 功能，记录学习轨迹">
          <div v-if="recentActivities.length > 0" class="activity-list">
            <button
              v-for="item in recentActivities"
              :key="item.id"
              class="activity-item"
              type="button"
              @click="navigateTo(item.path)"
            >
              <div class="activity-dot" :class="`dot-${item.type}`" />
              <div class="activity-content">
                <div class="activity-text">{{ item.text }}</div>
                <div class="activity-time">{{ item.time }}</div>
              </div>
              <el-icon :size="14" class="activity-arrow"><ArrowRight /></el-icon>
            </button>
          </div>
        </BaseCard>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getMaterialList } from '@/api/material'
import { getChatHistory, getQuizHistory } from '@/api/history'
import { getUserProfile } from '@/api/user'
import {
  getPlanProgressList,
  getPlanProgressStats,
  listPlans,
  listResourcePackageTasks,
  updatePlanProgress
} from '@/api/ai'
import { Document, ArrowRight, Upload, ChatDotRound, EditPen, Calendar } from '@element-plus/icons-vue'
import BaseCard from '@/components/common/BaseCard.vue'
import BaseStatisticCard from '@/components/common/BaseStatisticCard.vue'
import ProgressRing from '@/components/common/ProgressRing.vue'
import AreaChart from '@/components/common/AreaChart.vue'
import CountdownTimer from '@/components/common/CountdownTimer.vue'

const router = useRouter()
const userStore = useUserStore()

const loading = reactive({
  stats: false,
  materials: false,
  activity: false,
  plan: false,
  progress: false,
  recommendations: false
})

// Stats
const stats = ref([
  { key: 'hours', label: '实际学习', value: '0h', icon: 'Clock', iconBg: 'var(--bg-sidebar)', iconColor: 'var(--color-text-secondary)', trend: null, trendValue: '来自学习计划', path: '/ai/plan' },
  { key: 'material', label: '学习资料', value: 0, icon: 'Document', iconBg: 'var(--bg-tag-green)', iconColor: 'var(--color-primary)', trend: null, trendValue: '', path: '/material' },
  { key: 'summary', label: '已总结资料', value: 0, icon: 'DocumentCopy', iconBg: 'var(--bg-tag-blue)', iconColor: 'var(--color-tag-blue)', trend: null, trendValue: '', path: '/ai/summary' },
  { key: 'quiz', label: '练习次数', value: 0, icon: 'EditPen', iconBg: 'var(--bg-tag-amber)', iconColor: 'var(--color-tag-amber)', trend: null, trendValue: '', path: '/ai/quiz' }
])

const quickActions = [
  { label: '上传资料', icon: 'Upload', path: '/material', bg: 'var(--bg-sidebar)', color: 'var(--text-secondary)' },
  { label: 'AI 问答', icon: 'ChatDotRound', path: '/ai/chat', bg: 'var(--bg-tag-green)', color: 'var(--color-primary)' },
  { label: '资源工坊', icon: 'Box', path: '/ai/resource-package', bg: 'var(--bg-tag-blue)', color: 'var(--color-tag-blue)' },
  { label: 'AI 出题', icon: 'EditPen', path: '/ai/quiz', bg: 'var(--bg-tag-amber)', color: 'var(--color-tag-amber)' },
  { label: '学习计划', icon: 'Calendar', path: '/ai/plan', bg: 'var(--bg-tag-blue)', color: 'var(--color-tag-blue)' }
]

const recentMaterials = ref([])
const recentActivities = ref([])
const recommendations = ref([])
const todayTasks = ref([])
const studyProgress = ref(0)
const examDate = ref('')
const latestPlan = ref(null)
const latestPlanItems = ref([])
const planProgressMap = ref({})
const planTotalDays = ref(90)

const weeklyActivity = ref([
  { label: '周一', value: 0 },
  { label: '周二', value: 0 },
  { label: '周三', value: 0 },
  { label: '周四', value: 0 },
  { label: '周五', value: 0 },
  { label: '周六', value: 0 },
  { label: '周日', value: 0 }
])

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const todayDate = computed(() => {
  const d = new Date()
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${weekdays[d.getDay()]}`
})

const progressHint = computed(() => {
  if (!latestPlan.value) return '暂无计划进度'
  const completed = Object.values(planProgressMap.value).filter(Boolean).length
  return `已完成 ${completed} / ${planTotalDays.value} 天`
})

const countdownTitle = computed(() => latestPlan.value?.goal ? '距离目标日期' : '距离考试')

function navigateTo(path) { router.push(path) }

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function toNumber(value, fallback = 0) {
  const num = Number(value)
  return Number.isFinite(num) ? num : fallback
}

function parseJson(value, fallback) {
  if (!value) return fallback
  if (Array.isArray(value) || typeof value === 'object') return value
  try {
    return JSON.parse(value)
  } catch {
    return fallback
  }
}

function materialDisplayName(item) {
  return item?.originalName || item?.fileName || '未命名资料'
}

function materialEntryPath(item) {
  if (item?.summary) return `/ai/summary?materialId=${item.id}`
  return `/ai/chat?materialId=${item.id}`
}

function materialStatusText(item) {
  if (item?.status && item.status !== 'ready') return '处理中'
  if (item?.summary) return '已有总结'
  return '可问答'
}

function getPlanDayIndex(plan) {
  if (!plan?.examDate || !plan?.totalDays) return 1
  const exam = new Date(`${plan.examDate}T00:00:00`)
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const daysRemaining = Math.max(0, Math.ceil((exam - today) / 86400000))
  const elapsed = toNumber(plan.totalDays) - daysRemaining
  return Math.min(Math.max(elapsed + 1, 1), toNumber(plan.totalDays, 1))
}

function normalizePlanItems(plan) {
  const list = parseJson(plan?.planContent, [])
  return Array.isArray(list) ? list : []
}

function normalizeDurationMinutes(value, fallbackHours) {
  if (typeof value === 'number') return value <= 24 ? value * 60 : value
  const text = String(value || '')
  const hourMatch = text.match(/([\d.]+)\s*小?时/)
  if (hourMatch) return Math.round(toNumber(hourMatch[1], fallbackHours) * 60)
  const minuteMatch = text.match(/(\d+)\s*分/)
  if (minuteMatch) return toNumber(minuteMatch[1], fallbackHours * 60)
  return fallbackHours * 60
}

function buildTodayTasks(plan, items) {
  if (!plan || items.length === 0) return []
  const preferredDay = getPlanDayIndex(plan)
  const firstUnfinished = items.find(item => !planProgressMap.value[toNumber(item.day)])
  const item = items.find(day => toNumber(day.day) === preferredDay) || firstUnfinished || items[0]
  if (!item) return []

  const dayIndex = toNumber(item.day, preferredDay)
  const tasks = Array.isArray(item.tasks) ? item.tasks.join('、') : item.tasks
  const topics = Array.isArray(item.topics) ? item.topics.join('、') : item.topics
  const title = topics ? `第 ${dayIndex} 天：${topics}` : (tasks || `第 ${dayIndex} 天学习任务`)

  return [{
    id: `${plan.id}-${dayIndex}`,
    dayIndex,
    title,
    completed: Boolean(planProgressMap.value[dayIndex]),
    estimatedMinutes: normalizeDurationMinutes(item.duration, toNumber(plan.dailyHours, 2)),
    updating: false
  }]
}

async function toggleTask(task, completed) {
  if (!latestPlan.value?.id) return
  task.updating = true
  try {
    await updatePlanProgress(latestPlan.value.id, task.dayIndex, { completed })
    planProgressMap.value = {
      ...planProgressMap.value,
      [task.dayIndex]: completed
    }
    task.completed = completed
    await loadProgressStats(latestPlan.value.id)
  } finally {
    task.updating = false
  }
}

function normalizeList(value) {
  if (Array.isArray(value)) return value
  if (!value) return []
  if (typeof value === 'string') {
    return value.split(/[,，、\n]/).map(item => item.trim()).filter(Boolean)
  }
  return []
}

function extractPackageTitle(task) {
  let result = task?.result
  if (typeof result === 'string') {
    try { result = JSON.parse(result) } catch { result = null }
  }
  return result?.materialName || task?.message || '最近资源包'
}

async function loadRecommendations() {
  loading.recommendations = true
  try {
    const [profileResult, packageTasks] = await Promise.all([
      getUserProfile().catch(() => ({ data: null })),
      listResourcePackageTasks(5).catch(() => [])
    ])
    const profile = profileResult?.data || {}
    const weakPoints = normalizeList(profile.weakPoints)
    const items = []

    if (!profile.studySubject || !profile.learningStyle || weakPoints.length === 0) {
      items.push({
        key: 'profile',
        title: '完善学习画像',
        description: '补齐科目、风格和薄弱点后，资源生成会更贴合你的目标。',
        icon: 'User',
        path: '/profile',
        bg: 'var(--bg-tag-green)',
        color: 'var(--color-primary)'
      })
    }

    const latestMaterial = recentMaterials.value[0]
    if (latestMaterial) {
      items.push({
        key: `material-${latestMaterial.id}`,
        title: '生成个性化资源包',
        description: `基于「${materialDisplayName(latestMaterial)}」生成讲解、导图、题库和学习路径。`,
        icon: 'Box',
        path: `/ai/resource-package?materialId=${latestMaterial.id}`,
        bg: 'var(--bg-tag-blue)',
        color: 'var(--color-tag-blue)'
      })
    }

    if (weakPoints.length > 0) {
      items.push({
        key: 'weak-points',
        title: '优先复习薄弱点',
        description: `围绕 ${weakPoints.slice(0, 2).join('、')} 安排针对性练习和错题巩固。`,
        icon: 'EditPen',
        path: '/quiz/wrong',
        bg: 'var(--bg-tag-amber)',
        color: 'var(--color-tag-amber)'
      })
    }

    const latestPackage = (packageTasks || []).find(task => task.status === 'success')
    if (latestPackage) {
      items.push({
        key: `package-${latestPackage.taskId}`,
        title: '继续查看最近资源包',
        description: `恢复「${extractPackageTitle(latestPackage)}」的多模态学习资源结果。`,
        icon: 'Collection',
        path: '/ai/resource-package',
        bg: 'var(--surface-container-low)',
        color: 'var(--color-text-secondary)'
      })
    }

    recommendations.value = items.slice(0, 4)
  } catch {
    recommendations.value = []
  } finally {
    loading.recommendations = false
  }
}

async function loadMaterials() {
  loading.materials = true
  try {
    const res = await getMaterialList({ page: 1, size: 100 })
    const materials = res.records || []
    recentMaterials.value = materials.slice(0, 5)
    const materialStat = stats.value.find(s => s.key === 'material')
    if (materialStat) materialStat.value = res.total || 0
    const summaryStat = stats.value.find(s => s.key === 'summary')
    if (summaryStat) {
      const summaryCount = materials.filter(item => Boolean(item.summary)).length
      summaryStat.value = summaryCount
      summaryStat.trend = summaryCount > 0 ? 'flat' : null
      summaryStat.trendValue = summaryCount > 0 ? '可继续沉淀' : ''
    }
  } catch { recentMaterials.value = [] }
  finally { loading.materials = false }
}

async function loadActivity() {
  loading.activity = true
  try {
    const [chatRes, quizRes] = await Promise.all([
      getChatHistory({ page: 1, size: 3 }),
      getQuizHistory({ page: 1, size: 3 })
    ])
    const chatItems = (chatRes.records || []).map(item => ({
      id: `chat-${item.id}`, type: 'chat',
      text: `提问了 "${(item.userMessage || item.question || '问题').slice(0, 20)}..."`,
      time: formatDate(item.createTime),
      sortTime: item.createTime,
      path: '/history'
    }))
    const quizItems = (quizRes.records || []).map(item => ({
      id: `quiz-${item.id}`, type: 'quiz',
      text: '完成了一次练习',
      time: formatDate(item.createTime),
      sortTime: item.createTime,
      path: '/history'
    }))
    recentActivities.value = [...chatItems, ...quizItems]
      .sort((a, b) => new Date(b.sortTime) - new Date(a.sortTime)).slice(0, 5)

    const quizStat = stats.value.find(s => s.key === 'quiz')
    if (quizStat) quizStat.value = quizRes.total || 0

    const hoursStat = stats.value.find(s => s.key === 'hours')
    const chatCount = toNumber(chatRes.total)
    if (hoursStat && !latestPlan.value && chatCount > 0) {
      hoursStat.trend = 'flat'
      hoursStat.trendValue = `${chatCount} 次问答记录`
    }
  } catch { recentActivities.value = [] }
  finally { loading.activity = false }
}

async function loadProgressStats(planId) {
  const hoursStat = stats.value.find(s => s.key === 'hours')
  try {
    const data = await getPlanProgressStats(planId)
    studyProgress.value = toNumber(data.progressPercent)
    if (hoursStat) {
      const hours = toNumber(data.totalActualHours)
      hoursStat.value = hours > 0 ? `${hours}h` : '0h'
      hoursStat.trend = studyProgress.value > 0 ? 'up' : 'flat'
      hoursStat.trendValue = studyProgress.value > 0 ? `计划 ${studyProgress.value}%` : '等待打卡'
    }
  } catch {
    studyProgress.value = 0
  }
}

async function loadPlan() {
  loading.plan = true
  loading.progress = true
  try {
    const plans = await listPlans().catch(() => [])
    latestPlan.value = Array.isArray(plans) ? plans[0] : null
    if (!latestPlan.value) {
      todayTasks.value = []
      studyProgress.value = 0
      examDate.value = ''
      planTotalDays.value = 90
      return
    }

    examDate.value = latestPlan.value.examDate || ''
    planTotalDays.value = toNumber(latestPlan.value.totalDays, 90)
    latestPlanItems.value = normalizePlanItems(latestPlan.value)

    const progressList = await getPlanProgressList(latestPlan.value.id).catch(() => [])
    const nextProgressMap = {}
    progressList.forEach(item => {
      nextProgressMap[item.dayIndex] = Boolean(item.completed)
    })
    planProgressMap.value = nextProgressMap

    todayTasks.value = buildTodayTasks(latestPlan.value, latestPlanItems.value)
    await loadProgressStats(latestPlan.value.id)
  } finally {
    loading.plan = false
    loading.progress = false
  }
}

function loadProgress() {
  if (!latestPlan.value) studyProgress.value = 0
}

onMounted(async () => {
  await loadMaterials()
  await Promise.all([
    loadRecommendations(),
    loadActivity(),
    loadPlan()
  ])
  loadProgress()
})
</script>

<style scoped>
.dashboard-page {
  width: 100%;
}

/* Welcome */
.welcome-section {
  margin-bottom: var(--space-8);
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.welcome-title {
  font-size: var(--text-display);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
  margin-bottom: 4px;
}

.welcome-date {
  font-size: var(--text-ui);
  color: var(--color-text-tertiary);
}

.welcome-slogan {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
  font-weight: 500;
}

/* Stats */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-3);
  margin-bottom: var(--space-6);
}

/* Dashboard Grid */
.dashboard-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.dashboard-main,
.dashboard-side {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Progress Row */
.progress-row {
  display: grid;
  grid-template-columns: 1fr 1fr 1.5fr;
  gap: 16px;
}

.ring-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
}

.ring-hint {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-top: 12px;
}

/* Quick Actions */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.quick-action-btn {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  background: var(--surface-container-low);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.quick-action-btn:hover {
  border-color: var(--outline);
  background: var(--surface-card);
}

.quick-action-btn:focus-visible,
.recommendation-item:focus-visible,
.learning-item:focus-visible,
.activity-item:focus-visible {
  outline: 2px solid var(--color-ring);
  outline-offset: 2px;
}

.quick-action-btn:active { transform: scale(0.98); }

.qa-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
}

.qa-label {
  font-size: var(--text-small);
  font-weight: 500;
  color: var(--color-text-primary);
}

/* Recommendations */
.recommendation-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.recommendation-item {
  min-height: 92px;
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-4);
  border: 1px solid var(--outline);
  border-radius: var(--radius-md);
  background: var(--surface-container-low);
  cursor: pointer;
  text-align: left;
  transition: all var(--duration-fast) var(--ease-default);
}

.recommendation-item:hover {
  background: var(--surface-card);
  border-color: var(--outline-variant);
}

.recommendation-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.recommendation-content {
  flex: 1;
  min-width: 0;
}

.recommendation-title {
  color: var(--color-text-primary);
  font-size: var(--text-ui);
  font-weight: 600;
  line-height: 1.4;
}

.recommendation-content p {
  margin: var(--space-1) 0 0;
  color: var(--color-text-secondary);
  font-size: var(--text-small);
  line-height: 1.6;
}

.recommendation-arrow {
  color: var(--color-text-tertiary);
  flex-shrink: 0;
  margin-top: 10px;
}

.recommendation-empty {
  padding: var(--space-4);
  color: var(--color-text-tertiary);
  font-size: var(--text-ui);
  text-align: center;
}

/* Learning List */
.learning-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.learning-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 0;
  background: transparent;
  text-align: left;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.learning-item:hover { background: var(--surface-hover); }

.learning-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--blue-50);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.learning-info {
  flex: 1;
  min-width: 0;
}

.learning-name {
  font-size: var(--text-ui);
  font-weight: 500;
  color: var(--color-text-primary);
}

.learning-meta {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

.learning-arrow {
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

/* Task List */
.task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.task-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.task-item:hover { background: var(--surface-hover); }
.task-item.completed .task-title { text-decoration: line-through; color: var(--color-text-tertiary); }

.task-title { font-size: var(--text-ui); color: var(--color-text-primary); }
.task-time { font-size: var(--text-small); color: var(--color-text-tertiary); flex-shrink: 0; }

/* Activity */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.activity-item {
  width: 100%;
  display: flex;
  align-items: flex-start;
  padding: 4px;
  border: 0;
  border-radius: var(--radius-md);
  background: transparent;
  gap: 10px;
  text-align: left;
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.activity-item:hover { background: var(--surface-hover); }

.activity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.dot-chat { background: #2563eb; }
.dot-quiz { background: #f59e0b; }
.dot-summary { background: #16a34a; }
.dot-plan { background: #7c3aed; }

.activity-text { font-size: var(--text-ui); color: var(--color-text-primary); line-height: 1.5; }
.activity-time { font-size: var(--text-small); color: var(--color-text-tertiary); margin-top: 2px; }
.activity-arrow {
  color: var(--color-text-tertiary);
  margin-left: auto;
  margin-top: 3px;
  flex-shrink: 0;
}

/* Responsive */
@media (max-width: 1279px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .dashboard-grid { grid-template-columns: 1fr; }
  .progress-row { grid-template-columns: 1fr 1fr 1fr; }
  .quick-actions { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 767px) {
  .stats-grid { grid-template-columns: 1fr; }
  .progress-row { grid-template-columns: 1fr; }
  .quick-actions { grid-template-columns: repeat(2, 1fr); }
  .recommendation-grid { grid-template-columns: 1fr; }
  .welcome-section { flex-direction: column; align-items: flex-start; }
}
</style>
