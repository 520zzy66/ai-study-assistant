<template>
  <div class="dashboard-page">
    <!-- Welcome Section -->
    <div class="welcome-section">
      <h1 class="welcome-title">{{ greeting }}，{{ userStore.userInfo?.nickname || '同学' }}</h1>
      <p class="welcome-subtitle">今天也要好好学习，继续加油吧</p>
    </div>

    <!-- Stats Grid -->
    <div class="stats-grid">
      <div
        v-for="stat in stats"
        :key="stat.key"
        class="stat-card"
        :class="`stat-${stat.theme}`"
        @click="navigateTo(stat.path)"
      >
        <div class="stat-icon">
          <el-icon :size="24"><component :is="stat.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- Main Content Grid -->
    <div class="dashboard-grid">
      <!-- Left Column -->
      <div class="dashboard-main">
        <!-- Quick Actions -->
        <el-card class="dashboard-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">快速开始</span>
            </div>
          </template>
          <div class="quick-actions">
            <button
              v-for="action in quickActions"
              :key="action.path"
              class="quick-action-btn"
              @click="navigateTo(action.path)"
            >
              <el-icon :size="22"><component :is="action.icon" /></el-icon>
              <span>{{ action.label }}</span>
            </button>
          </div>
        </el-card>

        <!-- Continue Learning -->
        <el-card class="dashboard-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">继续学习</span>
              <el-button text type="primary" size="small" @click="$router.push('/material')">
                查看全部
              </el-button>
            </div>
          </template>

          <div v-if="loading.materials" class="section-loading">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else-if="recentMaterials.length === 0" class="embedded-empty">
            <AppEmpty
              icon="Document"
              title="暂无学习资料"
              description="上传你的第一份资料，开始学习之旅"
              compact
            >
              <template #action>
                <el-button type="primary" @click="$router.push('/material')">上传资料</el-button>
              </template>
            </AppEmpty>
          </div>
          <div v-else class="learning-list">
            <div
              v-for="material in recentMaterials"
              :key="material.id"
              class="learning-item"
              @click="navigateTo(`/ai/summary?materialId=${material.id}`)"
            >
              <div class="learning-icon">
                <el-icon :size="20"><Document /></el-icon>
              </div>
              <div class="learning-info">
                <div class="learning-name truncate">{{ material.fileName }}</div>
                <div class="learning-meta">{{ formatDate(material.createTime) }}</div>
              </div>
              <el-button text type="primary" size="small" @click.stop="navigateTo(`/ai/chat?materialId=${material.id}`)">
                提问
              </el-button>
            </div>
          </div>
        </el-card>
      </div>

      <!-- Right Column -->
      <div class="dashboard-side">
        <!-- Today's Plan -->
        <el-card class="dashboard-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">今日计划</span>
            </div>
          </template>

          <div v-if="loading.plan" class="section-loading">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else-if="todayTasks.length === 0" class="embedded-empty">
            <AppEmpty
              icon="Calendar"
              title="今日暂无任务"
              description="制定学习计划，让学习更有节奏"
              compact
            >
              <template #action>
                <el-button type="primary" @click="$router.push('/ai/plan')">制定计划</el-button>
              </template>
            </AppEmpty>
          </div>
          <div v-else class="task-list">
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
        </el-card>

        <!-- Recent Activity -->
        <el-card class="dashboard-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title">最近动态</span>
            </div>
          </template>

          <div v-if="loading.activity" class="section-loading">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else-if="recentActivities.length === 0" class="embedded-empty">
            <AppEmpty
              icon="Clock"
              title="暂无学习记录"
              description="开始使用 AI 功能，记录你的学习轨迹"
              compact
            />
          </div>
          <div v-else class="activity-list">
            <div v-for="activity in recentActivities" :key="activity.id" class="activity-item">
              <div class="activity-icon" :class="`activity-${activity.type}`">
                <el-icon :size="14"><component :is="activity.icon" /></el-icon>
              </div>
              <div class="activity-content">
                <div class="activity-text">{{ activity.text }}</div>
                <div class="activity-time">{{ activity.time }}</div>
              </div>
            </div>
          </div>
        </el-card>
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
import AppEmpty from '@/components/common/AppEmpty.vue'

const router = useRouter()
const userStore = useUserStore()

const loading = reactive({
  materials: false,
  activity: false,
  plan: false
})

const stats = ref([
  { key: 'material', label: '学习资料', value: 0, icon: 'Document', theme: 'primary', path: '/material' },
  { key: 'chat', label: 'AI 对话', value: 0, icon: 'ChatDotRound', theme: 'blue', path: '/ai/chat' },
  { key: 'quiz', label: '练习题目', value: 0, icon: 'EditPen', theme: 'amber', path: '/ai/quiz' },
  { key: 'wrong', label: '待复习错题', value: 0, icon: 'Notebook', theme: 'rose', path: '/quiz/wrong' }
])

const quickActions = [
  { label: '上传资料', icon: 'Upload', path: '/material' },
  { label: 'AI 问答', icon: 'ChatDotRound', path: '/ai/chat' },
  { label: 'AI 出题', icon: 'EditPen', path: '/ai/quiz' },
  { label: '学习计划', icon: 'Calendar', path: '/ai/plan' }
]

const recentMaterials = ref([])
const recentActivities = ref([])
const todayTasks = ref([])

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

function navigateTo(path) {
  router.push(path)
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

function toggleTask(task) {
  // TODO: call API to update task status
  console.log('toggle task', task)
}

async function loadMaterials() {
  loading.materials = true
  try {
    const res = await getMaterialList({ page: 1, size: 5 })
    recentMaterials.value = res.records || []
    stats.value.find(s => s.key === 'material').value = res.total || 0
  } catch (error) {
    recentMaterials.value = []
  } finally {
    loading.materials = false
  }
}

async function loadActivity() {
  loading.activity = true
  try {
    const [chatRes, quizRes] = await Promise.all([
      getChatHistory({ page: 1, size: 3 }),
      getQuizHistory({ page: 1, size: 3 })
    ])

    const chatItems = (chatRes.records || []).map(item => ({
      id: `chat-${item.id}`,
      type: 'chat',
      icon: 'ChatDotRound',
      text: `提问了 "${item.question?.slice(0, 20) || '问题'}..."`,
      time: formatDate(item.createTime)
    }))

    const quizItems = (quizRes.records || []).map(item => ({
      id: `quiz-${item.id}`,
      type: 'quiz',
      icon: 'EditPen',
      text: `完成了一次练习`,
      time: formatDate(item.createTime)
    }))

    recentActivities.value = [...chatItems, ...quizItems]
      .sort((a, b) => new Date(b.time) - new Date(a.time))
      .slice(0, 5)

    stats.value.find(s => s.key === 'chat').value = chatRes.total || 0
    stats.value.find(s => s.key === 'quiz').value = quizRes.total || 0
  } catch (error) {
    recentActivities.value = []
  } finally {
    loading.activity = false
  }
}

async function loadPlan() {
  loading.plan = true
  try {
    // TODO: replace with real plan API when available
    todayTasks.value = []
  } finally {
    loading.plan = false
  }
}

onMounted(() => {
  loadMaterials()
  loadActivity()
  loadPlan()
})
</script>

<style scoped>
.dashboard-page {
  max-width: 1200px;
}

.welcome-section {
  margin-bottom: var(--space-6);
}

.welcome-title {
  font-size: var(--text-display);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
  margin-bottom: var(--space-1);
}

.welcome-subtitle {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
  margin-bottom: 0;
}

/* Stats */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.stat-card {
  background: var(--surface-card);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  display: flex;
  align-items: center;
  gap: var(--space-4);
  cursor: pointer;
  transition: box-shadow var(--duration-normal) var(--ease-default),
              transform var(--duration-normal) var(--ease-default);
}

.stat-card:hover {
  box-shadow: var(--shadow-2);
  transform: translateY(-1px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-primary .stat-icon { background: var(--teal-50); color: var(--color-primary); }
.stat-blue .stat-icon { background: #eff6ff; color: #3b82f6; }
.stat-amber .stat-icon { background: #fffbeb; color: #f59e0b; }
.stat-rose .stat-icon { background: #fff1f2; color: #f43f5e; }

.stat-value {
  font-size: var(--text-display);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
  line-height: 1;
}

.stat-label {
  font-size: var(--text-ui);
  color: var(--color-text-secondary);
  margin-top: var(--space-1);
}

/* Dashboard Grid */
.dashboard-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: var(--space-6);
}

.dashboard-main,
.dashboard-side {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.dashboard-card {
  border-radius: var(--radius-lg);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-text-primary);
}

/* Quick Actions */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
}

.quick-action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  padding: var(--space-5) var(--space-3);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  background: var(--surface-card);
  color: var(--color-text-primary);
  font-size: var(--text-ui);
  font-weight: 500;
  cursor: pointer;
  transition: box-shadow var(--duration-normal) var(--ease-default),
              transform var(--duration-normal) var(--ease-default),
              border-color var(--duration-fast) var(--ease-default);
}

.quick-action-btn:hover {
  border-color: var(--color-primary);
  box-shadow: var(--shadow-2);
  transform: translateY(-1px);
}

.quick-action-btn:active {
  transform: scale(0.98);
}

.quick-action-btn .el-icon {
  color: var(--color-primary);
}

/* Learning List */
.learning-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.learning-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.learning-item:hover {
  background: var(--surface-hover);
}

.learning-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--surface-container);
  color: var(--color-text-secondary);
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
  font-size: var(--text-body);
  font-weight: 500;
  color: var(--color-text-primary);
}

.learning-meta {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-top: var(--space-1);
}

/* Task List */
.task-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.task-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-2) var(--space-1);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);
}

.task-item:hover {
  background: var(--surface-hover);
}

.task-item.completed .task-title {
  text-decoration: line-through;
  color: var(--color-text-tertiary);
}

.task-title {
  font-size: var(--text-body);
  color: var(--color-text-primary);
}

.task-time {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  flex-shrink: 0;
}

/* Activity */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}

.activity-icon {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

.activity-chat .activity-icon { background: #eff6ff; color: #3b82f6; }
.activity-quiz .activity-icon { background: #fffbeb; color: #f59e0b; }
.activity-summary .activity-icon { background: var(--teal-50); color: var(--color-primary); }
.activity-plan .activity-icon { background: #f3e8ff; color: #8b5cf6; }

.activity-content {
  flex: 1;
  min-width: 0;
}

.activity-text {
  font-size: var(--text-body);
  color: var(--color-text-primary);
  line-height: 1.5;
}

.activity-time {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-top: var(--space-1);
}

.section-loading {
  padding: var(--space-4) 0;
}

.embedded-empty :deep(.app-empty) {
  padding: var(--space-8) 0;
}

@media (max-width: 1279px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .dashboard-grid { grid-template-columns: 1fr; }
  .quick-actions { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 767px) {
  .stats-grid { grid-template-columns: 1fr; }
  .quick-actions { grid-template-columns: 1fr; }
}
</style>
