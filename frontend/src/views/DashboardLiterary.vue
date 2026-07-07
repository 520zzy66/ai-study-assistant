<template>
  <div class="dashboard-page-literary">
    <!-- Welcome Header: Apple Style Large Typography -->
    <div class="welcome-section">
      <div class="welcome-text">
        <h1 class="welcome-title">{{ greeting }}，{{ userStore.userInfo?.nickname || '同学' }}</h1>
        <p class="welcome-date">{{ todayDate }}</p>
      </div>
      <p class="welcome-slogan">持续学习，每天进步一点点</p>
    </div>

    <!-- Stats Grid: 4-Column Apple-style Flat Widget Tiles with Active Tap feedback -->
    <div class="stats-grid">
      <div
        v-for="stat in stats"
        :key="stat.key"
        class="stat-widget-card"
        @click="navigateTo(stat.path)"
      >
        <div class="stat-widget-header">
          <span class="stat-widget-title">{{ stat.label }}</span>
          <div class="stat-widget-icon" :style="{ color: stat.iconColor, background: stat.iconBg }">
            <el-icon :size="14"><component :is="stat.icon" /></el-icon>
          </div>
        </div>
        <div class="stat-widget-value">{{ stat.value }}</div>
        <div v-if="stat.trendValue" class="stat-widget-trend">
          {{ stat.trendValue }}
        </div>
      </div>
    </div>

    <!-- Main Content Grid -->
    <div class="dashboard-grid">
      <!-- Left Column: Primary Study Panels -->
      <div class="dashboard-main">
        <!-- Progress Row: Circular Progress, Countdown Timer & Area Chart -->
        <div class="progress-row">
          <!-- Study Progress (Apple Ring) -->
          <div class="literary-card">
            <h3 class="literary-card-title">学习进度</h3>
            <div class="ring-center">
              <ProgressRing :percentage="studyProgress" :size="110" :stroke-width="8" />
              <p class="ring-hint">总体完成度</p>
            </div>
          </div>

          <!-- Exam Countdown -->
          <div class="literary-card">
            <h3 class="literary-card-title">考试倒计时</h3>
            <div class="countdown-center">
              <CountdownTimer
                :target-date="examDate"
                :total-days="90"
              />
            </div>
          </div>

          <!-- Weekly Activity Area Chart -->
          <div class="literary-card col-span-chart">
            <h3 class="literary-card-title">本周学习活跃</h3>
            <div class="chart-wrapper">
              <AreaChart :data="weeklyActivity" :height="130" />
            </div>
          </div>
        </div>

        <!-- Quick Actions (Notion Grid Style) -->
        <div class="literary-card">
          <h3 class="literary-card-title">快速开始</h3>
          <div class="quick-actions-grid">
            <button
              v-for="action in quickActions"
              :key="action.path"
              class="quick-action-widget"
              @click="navigateTo(action.path)"
            >
              <div class="qa-widget-icon" :style="{ background: action.bg, color: action.color }">
                <el-icon :size="18"><component :is="action.icon" /></el-icon>
              </div>
              <span class="qa-widget-label">{{ action.label }}</span>
            </button>
          </div>
        </div>

        <!-- Continue Learning (Booklet Cards with Clip Tags) -->
        <div class="literary-card">
          <div class="card-title-bar">
            <h3 class="literary-card-title">继续学习</h3>
            <el-button
              v-if="recentMaterials.length > 0"
              text
              size="small"
              class="literary-text-btn"
              @click="navigateTo('/material')"
            >
              查看全部
            </el-button>
          </div>
          
          <!-- Empty State -->
          <div v-if="recentMaterials.length === 0" class="literary-empty-state">
            <el-icon :size="24"><Document /></el-icon>
            <p>上传你的第一份资料，开始学习之旅</p>
          </div>

          <!-- Material List -->
          <div v-else class="learning-booklet-list">
            <div
              v-for="item in recentMaterials"
              :key="item.id"
              class="learning-booklet-item"
              @click="navigateTo(`/ai/summary?materialId=${item.id}`)"
            >
              <div class="booklet-clip-tag">
                <svg viewBox="0 0 24 24" width="14" height="14" fill="currentColor"><path d="M16.5 6v11.5c0 2.21-1.79 4-4 4s-4-1.79-4-4V5c0-1.38 1.12-2.5 2.5-2.5s2.5 1.12 2.5 2.5v10.5c0 .55-.45 1-1 1s-1-.45-1-1V6H10v9.5c0 1.93 1.57 3.5 3.5 3.5s3.5-1.57 3.5-3.5V5c0-2.76-2.24-5-5-5S7 2.24 7 5v12.5c0 3.59 2.91 6.5 6.5 6.5s6.5-2.91 6.5-6.5V6h-1.5z"/></svg>
              </div>
              <div class="booklet-info">
                <div class="booklet-name">{{ item.originalName || item.fileName }}</div>
                <div class="booklet-meta">{{ formatDate(item.createTime) }} · 就绪</div>
              </div>
              <el-icon :size="14" class="booklet-arrow"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>

      <!-- Right Column: Secondary Side Panels -->
      <div class="dashboard-side">
        <!-- Today's Task Checklist -->
        <div class="literary-card">
          <div class="card-title-bar">
            <h3 class="literary-card-title">今日任务</h3>
            <el-button
              v-if="todayTasks.length > 0"
              text
              size="small"
              class="literary-text-btn"
              @click="navigateTo('/ai/plan')"
            >
              查看计划
            </el-button>
          </div>

          <div v-if="todayTasks.length === 0" class="literary-empty-state">
            <el-icon :size="24"><Calendar /></el-icon>
            <p>制定学习计划，让学习更有节奏</p>
          </div>

          <div v-else class="task-checklist">
            <label
              v-for="task in todayTasks"
              :key="task.id"
              class="task-check-item"
              :class="{ completed: task.completed }"
            >
              <div class="check-left">
                <input type="checkbox" v-model="task.completed" @change="toggleTask(task)" class="literary-checkbox">
                <span class="task-check-title">{{ task.title }}</span>
              </div>
              <span class="task-check-time">{{ task.estimatedMinutes }}m</span>
            </label>
          </div>
        </div>

        <!-- Recent Log (Quiet Flat Timeline) -->
        <div class="literary-card">
          <h3 class="literary-card-title">最近动态</h3>
          
          <div v-if="recentActivities.length === 0" class="literary-empty-state">
            <el-icon :size="24"><Clock /></el-icon>
            <p>记录您的学习与问答足迹</p>
          </div>

          <div v-else class="literary-timeline">
            <div v-for="item in recentActivities" :key="item.id" class="timeline-log-item">
              <div class="timeline-log-dot" :class="`dot-${item.type}`" />
              <div class="timeline-log-content">
                <div class="timeline-log-text">{{ item.text }}</div>
                <div class="timeline-log-time">{{ item.time }}</div>
              </div>
            </div>
          </div>
        </div>
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

const stats = ref([
  { key: 'hours', label: '学习时长', value: '0h', icon: 'Clock', iconBg: 'rgba(59, 130, 246, 0.08)', iconColor: '#3b82f6', trendValue: '', path: '/history' },
  { key: 'material', label: '学习资料', value: 0, icon: 'Document', iconBg: 'rgba(45, 90, 39, 0.08)', iconColor: '#2d5a27', trendValue: '', path: '/material' },
  { key: 'summary', label: 'AI 总结', value: 0, icon: 'MagicStick', iconBg: 'rgba(217, 119, 6, 0.08)', iconColor: '#d97706', trendValue: '', path: '/ai/summary' },
  { key: 'quiz', label: '练习次数', value: 0, icon: 'EditPen', iconBg: 'rgba(220, 38, 38, 0.08)', iconColor: '#dc2626', trendValue: '', path: '/ai/quiz' }
])

const quickActions = [
  { label: '上传资料', icon: 'Upload', path: '/material', bg: 'rgba(59, 130, 246, 0.08)', color: '#3b82f6' },
  { label: 'AI 问答', icon: 'ChatDotRound', path: '/ai/chat', bg: 'rgba(45, 90, 39, 0.08)', color: '#2d5a27' },
  { label: 'AI 出题', icon: 'EditPen', path: '/ai/quiz', bg: 'rgba(217, 119, 6, 0.08)', color: '#d97706' },
  { label: '学习计划', icon: 'Calendar', path: '/ai/plan', bg: 'rgba(124, 58, 237, 0.08)', color: '#7c3aed' }
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

function toggleTask(task) { /* API binding is preserved */ }

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
      text: `提问了 "${(item.userMessage || item.question || '问题').slice(0, 15)}..."`,
      time: formatDate(item.createTime),
      sortTime: item.createTime
    }))
    const quizItems = (quizRes.records || []).map(item => ({
      id: `quiz-${item.id}`, type: 'quiz',
      text: '完成了一次练习',
      time: formatDate(item.createTime),
      sortTime: item.createTime
    }))
    recentActivities.value = [...chatItems, ...quizItems]
      .sort((a, b) => new Date(b.sortTime) - new Date(a.sortTime)).slice(0, 5)

    const summaryStat = stats.value.find(s => s.key === 'summary')
    if (summaryStat) summaryStat.value = chatRes.total || 0
    const quizStat = stats.value.find(s => s.key === 'quiz')
    if (quizStat) quizStat.value = quizRes.total || 0
  } catch { recentActivities.value = [] }
  finally { loading.activity = false }
}

function loadPlan() {
  loading.plan = true
  todayTasks.value = [
    { id: 1, title: '阅读《操作系统导论》第三章', completed: false, estimatedMinutes: 45 },
    { id: 2, title: '完成国考申论真题模拟训练', completed: true, estimatedMinutes: 90 }
  ]
  loading.plan = false
}

function loadProgress() {
  loading.progress = true
  studyProgress.value = 65
  examDate.value = '2026-11-20'
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
/* ---------- 自给自足的纸张感主题作用域 ---------- */
.dashboard-page-literary {
  --bg-page: #faf9f6;
  --bg-card: #ffffff;
  --border-color: rgba(28, 28, 26, 0.08);
  --border-color-hover: rgba(28, 28, 26, 0.15);
  
  --text-primary: #1c1c1a;
  --text-secondary: #4a4a46;
  --text-tertiary: #7d7d77;
  
  --color-primary: #2d5a27;         /* 森林绿 */
  --color-primary-light: rgba(45, 90, 39, 0.05);
  --color-accent: #0071e3;          /* 苹果蓝 */
  
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.02), 0 4px 12px rgba(28, 28, 26, 0.02);
  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 14px;
  --transition-default: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
  --font-serif: "Georgia", "华文宋体", serif;

  width: 100%;
  padding: 4px;
}

/* Dark theme scoped override */
:global(.dark) .dashboard-page-literary {
  --bg-page: #141413;
  --bg-card: #1c1b1a;
  --border-color: rgba(230, 229, 223, 0.08);
  --border-color-hover: rgba(230, 229, 223, 0.15);
  --text-primary: #e6e5df;
  --text-secondary: #b2b1a9;
  --text-tertiary: #7d7c75;
  --color-primary: #528b4f;
  --color-primary-light: rgba(82, 139, 79, 0.08);
}

/* Welcome Header */
.welcome-section {
  margin-bottom: 28px;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.welcome-title {
  font-family: 'Outfit', sans-serif;
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  margin-bottom: 4px;
}

.welcome-date {
  font-size: 13px;
  color: var(--text-tertiary);
}

.welcome-slogan {
  font-size: 14px;
  color: var(--text-secondary);
  font-weight: 500;
}

/* Stats Widgets (Apple Card Grid) */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}

.stat-widget-card {
  background-color: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  box-shadow: var(--shadow-sm);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: var(--transition-default);
}

.stat-widget-card:hover {
  border-color: var(--border-color-hover);
  transform: translateY(-1px);
}

.stat-widget-card:active {
  transform: scale(0.97); /* 物理点按收缩反馈 */
}

.stat-widget-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-widget-title {
  font-size: 12px;
  color: var(--text-tertiary);
  font-weight: 500;
}

.stat-widget-icon {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-widget-value {
  font-size: 24px;
  font-family: 'Outfit', sans-serif;
  font-weight: 600;
  color: var(--text-primary);
}

.stat-widget-trend {
  font-size: 11px;
  color: var(--text-tertiary);
}

/* Main Grid Layout */
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

/* Literary BaseCard */
.literary-card {
  background-color: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  padding: 24px;
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.literary-card-title {
  font-size: 12px;
  color: var(--text-tertiary);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.literary-text-btn {
  font-size: 12px;
  color: var(--color-accent) !important;
  font-weight: 500;
}

/* Progress Row Layout */
.progress-row {
  display: grid;
  grid-template-columns: 1.1fr 1fr 1.6fr;
  gap: 16px;
}

.col-span-chart {
  grid-column: span 1;
}

.ring-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
}

.ring-hint {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 12px;
}

.countdown-center {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.chart-wrapper {
  padding-top: 8px;
}

/* Quick Actions Widget */
.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.quick-action-widget {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 16px 12px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  cursor: pointer;
  transition: var(--transition-default);
}

.quick-action-widget:hover {
  border-color: var(--border-color-hover);
  transform: translateY(-1px);
}

.quick-action-widget:active {
  transform: scale(0.97);
}

.qa-widget-icon {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
}

.qa-widget-label {
  font-size: 12px;
  font-weight: 500;
  color: var(--text-primary);
}

/* Learning Booklet (Booklist Style) */
.learning-booklet-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.learning-booklet-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  border: 1px solid transparent;
  cursor: pointer;
  transition: var(--transition-default);
}

.learning-booklet-item:hover {
  background: var(--surface-hover);
  border-color: var(--border-color);
}

.booklet-clip-tag {
  width: 30px;
  height: 30px;
  border-radius: var(--radius-sm);
  background: var(--bg-tag-green);
  color: var(--color-tag-green);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.booklet-info {
  flex: 1;
  min-width: 0;
}

.booklet-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.booklet-meta {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 2px;
}

.booklet-arrow {
  color: var(--text-tertiary);
  flex-shrink: 0;
}

/* Task Checklist */
.task-checklist {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.task-check-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  background-color: var(--bg-page);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: var(--transition-default);
}

.task-check-item:hover {
  border-color: var(--border-color-hover);
}

.check-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 极简手账式打勾框 */
.literary-checkbox {
  appearance: none;
  width: 16px;
  height: 16px;
  border: 1.5px solid var(--text-tertiary);
  border-radius: 3px;
  outline: none;
  cursor: pointer;
  position: relative;
  transition: var(--transition-default);
}

.literary-checkbox:checked {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
}

.literary-checkbox:checked::after {
  content: '✓';
  color: white;
  font-size: 11px;
  position: absolute;
  top: -1px;
  left: 2px;
}

.task-check-item.completed .task-check-title {
  text-decoration: line-through;
  color: var(--text-tertiary);
}

.task-check-title {
  font-size: 13px;
  color: var(--text-primary);
}

.task-check-time {
  font-size: 11px;
  color: var(--text-tertiary);
  flex-shrink: 0;
}

/* Quiet Flat Timeline */
.literary-timeline {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-log-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.timeline-log-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.dot-chat { background: var(--color-accent); }
.dot-quiz { background: var(--color-tag-amber); }

.timeline-log-text {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.4;
}

.timeline-log-time {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 1px;
}

.literary-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px 0;
  color: var(--text-tertiary);
  font-size: 12px;
  gap: 8px;
}

/* Responsive Grid Adapters */
@media (max-width: 1279px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .dashboard-grid { grid-template-columns: 1fr; }
  .progress-row { grid-template-columns: 1fr 1fr 1fr; }
  .quick-actions-grid { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 767px) {
  .stats-grid { grid-template-columns: 1fr; }
  .progress-row { grid-template-columns: 1fr; }
  .quick-actions-grid { grid-template-columns: repeat(2, 1fr); }
  .welcome-section { flex-direction: column; align-items: flex-start; }
}
</style>
