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
              <ProgressRing :percentage="studyProgress" :size="140" :stroke-width="10" />
              <p class="ring-hint">总体完成度</p>
            </div>
          </BaseCard>

          <!-- Exam Countdown -->
          <BaseCard class="countdown-card" :loading="loading.progress">
            <CountdownTimer
              title="距离考试"
              :target-date="examDate"
              :total-days="90"
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
              @click="navigateTo(action.path)"
            >
              <div class="qa-icon" :style="{ background: action.bg, color: action.color }">
                <el-icon :size="22"><component :is="action.icon" /></el-icon>
              </div>
              <span class="qa-label">{{ action.label }}</span>
            </button>
          </div>
        </BaseCard>

        <!-- Continue Learning -->
        <BaseCard title="继续学习" :loading="loading.materials" :isEmpty="recentMaterials.length === 0 && !loading.materials"
          empty-icon="Document" empty-text="上传你的第一份资料，开始学习之旅">
          <template v-if="!loading.materials && recentMaterials.length > 0" #header-action>
            <el-button text type="primary" size="small" @click="$router.push('/material')">查看全部</el-button>
          </template>
          <div v-if="recentMaterials.length > 0" class="learning-list">
            <div
              v-for="item in recentMaterials"
              :key="item.id"
              class="learning-item"
              @click="navigateTo(`/ai/summary?materialId=${item.id}`)"
            >
              <div class="learning-icon">
                <el-icon :size="20"><Document /></el-icon>
              </div>
              <div class="learning-info">
                <div class="learning-name truncate">{{ item.fileName }}</div>
                <div class="learning-meta">{{ formatDate(item.createTime) }}</div>
              </div>
              <el-icon :size="16" class="learning-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </BaseCard>
      </div>

      <!-- Side Column -->
      <div class="dashboard-side">
        <!-- Today's Tasks -->
        <BaseCard title="今日任务" :loading="loading.plan" :isEmpty="todayTasks.length === 0 && !loading.plan"
          empty-icon="Calendar" empty-text="制定学习计划，让学习更有节奏">
          <template v-if="todayTasks.length > 0" #header-action>
            <el-button text type="primary" size="small" @click="$router.push('/ai/plan')">查看计划</el-button>
          </template>
          <div v-if="todayTasks.length > 0" class="task-list">
            <label
              v-for="task in todayTasks"
              :key="task.id"
              class="task-item"
              :class="{ completed: task.completed }"
            >
              <el-checkbox v-model="task.completed" @change="toggleTask(task)">
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
            <div v-for="item in recentActivities" :key="item.id" class="activity-item">
              <div class="activity-dot" :class="`dot-${item.type}`" />
              <div class="activity-content">
                <div class="activity-text">{{ item.text }}</div>
                <div class="activity-time">{{ item.time }}</div>
              </div>
            </div>
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
  progress: false
})

// Stats
const stats = ref([
  { key: 'hours', label: '学习时长', value: '0h', icon: 'Clock', iconBg: '#eff6ff', iconColor: '#2563eb', trend: null, trendValue: '', path: '/history' },
  { key: 'material', label: '学习资料', value: 0, icon: 'Document', iconBg: '#f0fdf4', iconColor: '#16a34a', trend: null, trendValue: '', path: '/material' },
  { key: 'summary', label: 'AI 总结', value: 0, icon: 'MagicStick', iconBg: '#fef3c7', iconColor: '#d97706', trend: null, trendValue: '', path: '/ai/summary' },
  { key: 'quiz', label: '练习次数', value: 0, icon: 'EditPen', iconBg: '#fef2f2', iconColor: '#dc2626', trend: null, trendValue: '', path: '/ai/quiz' }
])

const quickActions = [
  { label: '上传资料', icon: 'Upload', path: '/material', bg: '#eff6ff', color: '#2563eb' },
  { label: 'AI 问答', icon: 'ChatDotRound', path: '/ai/chat', bg: '#f0fdf4', color: '#16a34a' },
  { label: 'AI 出题', icon: 'EditPen', path: '/ai/quiz', bg: '#fef3c7', color: '#d97706' },
  { label: '学习计划', icon: 'Calendar', path: '/ai/plan', bg: '#f3e8ff', color: '#7c3aed' }
]

const recentMaterials = ref([])
const recentActivities = ref([])
const todayTasks = ref([])
const studyProgress = ref(0)
const examDate = ref('')

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

function navigateTo(path) { router.push(path) }

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function toggleTask(task) { /* TODO: API */ }

async function loadMaterials() {
  loading.materials = true
  try {
    const res = await getMaterialList({ page: 1, size: 5 })
    recentMaterials.value = res.records || []
    const materialStat = stats.value.find(s => s.key === 'material')
    if (materialStat) materialStat.value = res.total || 0
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
      text: `提问了 "${(item.question || '问题').slice(0, 20)}..."`,
      time: formatDate(item.createTime)
    }))
    const quizItems = (quizRes.records || []).map(item => ({
      id: `quiz-${item.id}`, type: 'quiz',
      text: '完成了一次练习',
      time: formatDate(item.createTime)
    }))
    recentActivities.value = [...chatItems, ...quizItems]
      .sort((a, b) => new Date(b.time) - new Date(a.time)).slice(0, 5)

    const summaryStat = stats.value.find(s => s.key === 'summary')
    if (summaryStat) summaryStat.value = chatRes.total || 0
    const quizStat = stats.value.find(s => s.key === 'quiz')
    if (quizStat) quizStat.value = quizRes.total || 0
  } catch { recentActivities.value = [] }
  finally { loading.activity = false }
}

function loadPlan() {
  loading.plan = true
  // TODO: real plan API
  todayTasks.value = []
  loading.plan = false
}

function loadProgress() {
  loading.progress = true
  // TODO: real progress API
  loading.progress = false
}

onMounted(() => {
  loadMaterials()
  loadActivity()
  loadPlan()
  loadProgress()
})
</script>

<style scoped>
.dashboard-page {
  width: 100%;
}

/* Welcome */
.welcome-section {
  margin-bottom: 32px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.welcome-title {
  font-size: var(--text-hero);
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
  gap: 16px;
  margin-bottom: 32px;
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
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.quick-action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px 12px;
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  background: var(--surface-card);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
}

.quick-action-btn:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.quick-action-btn:active { transform: scale(0.98); }

.qa-icon {
  width: 44px;
  height: 44px;
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

/* Learning List */
.learning-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.learning-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
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
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

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
  .welcome-section { flex-direction: column; align-items: flex-start; }
}
</style>
