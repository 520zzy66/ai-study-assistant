<template>
  <div class="plan-page">
    <BasePageHeader
      title="AI 学习计划"
      description="输入学习目标和时间，AI 帮你制定每日学习任务"
    />

    <!-- Form Card -->
    <BaseCard title="生成学习计划" class="plan-form-card">
      <el-form :model="planForm" label-position="top" class="plan-form">
        <div class="form-grid">
          <el-form-item label="学习目标" required class="form-full">
            <el-input v-model="planForm.goal" placeholder="例如：掌握操作系统核心概念，通过期末考试" />
          </el-form-item>
          <el-form-item label="考试日期" required>
            <el-date-picker
              v-model="planForm.examDate"
              type="date"
              placeholder="选择考试日期"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledDate"
              style="width: 100%;"
            />
          </el-form-item>
          <el-form-item label="每日学习时长" required>
            <div class="hours-input">
              <el-input-number v-model="planForm.dailyHours" :min="1" :max="12" />
              <span class="unit-label">小时</span>
            </div>
          </el-form-item>
          <el-form-item label="关联资料" class="form-full">
            <el-select v-model="planForm.materialIds" multiple placeholder="选择关联资料（可选）" style="width: 100%;">
              <el-option
                v-for="item in materialList"
                :key="item.id"
                :label="item.originalName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-actions">
          <el-button type="primary" size="large" :loading="generating" @click="handleGenerate">
            <el-icon><MagicStick /></el-icon>
            {{ generating ? '生成中...' : '生成学习计划' }}
          </el-button>
        </div>

        <!-- Generating Progress -->
        <div v-if="generating" class="generate-progress-wrap">
          <el-icon :size="24" class="spinning" color="var(--color-primary)"><Loading /></el-icon>
          <div class="generate-progress-info">
            <p class="generate-progress-msg">{{ generateMsg }}</p>
            <div class="generate-progress-bar">
              <div class="generate-progress-fill" :style="{ width: generateProgress + '%' }" />
            </div>
          </div>
        </div>
      </el-form>
    </BaseCard>

    <!-- Result -->
    <div v-if="plan.length > 0" class="plan-result">
      <!-- Progress Stats -->
      <div class="progress-stats-card">
        <div class="stats-header">
          <h3>学习进度</h3>
          <el-button v-if="currentPlanId" text type="primary" @click="loadProgress">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
        <div class="stats-content">
          <div class="stat-item">
            <el-progress
              type="circle"
              :percentage="progressPercent"
              :width="80"
              :stroke-width="8"
              :color="progressColor"
            />
          </div>
          <div class="stat-details">
            <div class="stat-row">
              <span class="stat-label">已完成</span>
              <span class="stat-value">{{ completedDays }} / {{ totalDays }} 天</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">实际学习</span>
              <span class="stat-value">{{ totalActualHours }} 小时</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">考试日期</span>
              <span class="stat-value">{{ planForm.examDate }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="plan-info-card">
        <div class="plan-info-main">
          <h3 class="plan-title">{{ planTitle }}</h3>
          <p class="plan-goal">{{ planGoal }}</p>
        </div>
        <div class="plan-meta">
          <div class="meta-item">
            <span class="meta-value">{{ totalDays }}</span>
            <span class="meta-label">天</span>
          </div>
          <div class="meta-divider" />
          <div class="meta-item">
            <span class="meta-value">{{ planForm.dailyHours }}</span>
            <span class="meta-label">小时/天</span>
          </div>
        </div>
      </div>

      <div class="plan-timeline">
        <div v-for="(day, index) in plan" :key="index" class="plan-day" :class="{ completed: isDayCompleted(day.day) }">
          <div class="day-marker">
            <el-checkbox
              :model-value="isDayCompleted(day.day)"
              @change="(val) => toggleDayComplete(day.day, val)"
              class="day-checkbox"
            />
            <div v-if="index < plan.length - 1" class="day-line" />
          </div>
          <div class="day-content">
            <div class="day-header">
              <span class="day-label">第 {{ day.day }} 天</span>
              <span class="day-date">{{ day.date }} {{ day.weekday }}</span>
              <el-tag v-if="isDayCompleted(day.day)" type="success" size="small">已完成</el-tag>
            </div>
            <div class="day-card">
              <div class="day-topics">{{ day.topics?.join('、') }}</div>
              <div class="day-detail">
                <span class="detail-label">预计时长</span>
                <span class="detail-value">{{ day.duration }}</span>
              </div>
              <div class="day-detail">
                <span class="detail-label">具体任务</span>
                <span class="detail-value">{{ day.tasks }}</span>
              </div>
              <div v-if="day.materials?.length > 0" class="day-detail">
                <span class="detail-label">参考资料</span>
                <span class="detail-value">{{ day.materials.join('、') }}</span>
              </div>
              <!-- 实际学习时长输入 -->
              <div v-if="isDayCompleted(day.day)" class="day-actual">
                <el-input-number
                  v-model="dayActualHours[day.day]"
                  :min="0"
                  :max="24"
                  :step="0.5"
                  size="small"
                  @change="(val) => updateActualHours(day.day, val)"
                />
                <span class="unit-label">实际学习时长（小时）</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty -->
    <div v-else class="plan-empty">
      <AppEmpty
        icon="Calendar"
        title="填写信息后生成计划"
        description="AI 将根据你的目标和时间安排制定个性化学习计划"
        compact
      />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick, Loading, Refresh } from '@element-plus/icons-vue'
import { generatePlanAsync, updatePlanProgress, getPlanProgressList, getPlanProgressStats } from '@/api/ai'
import { loadReadyMaterials } from '@/api/material'
import { useTaskStore } from '@/stores/task'
import BaseCard from '@/components/common/BaseCard.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'
import AppEmpty from '@/components/common/AppEmpty.vue'

const taskStore = useTaskStore()
const materialList = ref([])
const plan = ref([])
const planTitle = ref('')
const planGoal = ref('')
const totalDays = ref(0)
const generating = ref(false)
const generateProgress = ref(0)
const generateMsg = ref('')
const currentPlanId = ref(null)

// 进度追踪相关
const completedDays = ref(0)
const totalActualHours = ref(0)
const dayCompletedMap = ref({})
const dayActualHours = ref({})

const planForm = reactive({
  goal: '',
  examDate: '',
  dailyHours: 2,
  materialIds: []
})

const progressPercent = computed(() => {
  return totalDays.value > 0 ? Math.round(completedDays.value * 100 / totalDays.value) : 0
})

const progressColor = computed(() => {
  if (progressPercent.value >= 80) return '#16a34a'
  if (progressPercent.value >= 50) return '#f59e0b'
  return '#3b82f6'
})

function disabledDate(time) {
  return time.getTime() < Date.now() - 8.64e7
}

async function handleGenerate() {
  if (!planForm.goal) {
    ElMessage.warning('请输入学习目标')
    return
  }
  if (!planForm.examDate) {
    ElMessage.warning('请选择考试日期')
    return
  }

  generating.value = true
  generateProgress.value = 0
  generateMsg.value = '正在创建任务...'
  plan.value = []

  try {
    const { taskId } = await generatePlanAsync(planForm)
    taskStore.watchTask(taskId, 'plan', {
      onProgress(pct, msg) {
        generateProgress.value = pct
        generateMsg.value = msg
      },
      onSuccess(result) {
        plan.value = result.plan || []
        planTitle.value = result.title || '学习计划'
        planGoal.value = result.goal || planForm.goal
        totalDays.value = result.totalDays || plan.value.length
        generating.value = false
        ElMessage.success('学习计划生成成功')
      },
      onError(errMsg) {
        generating.value = false
        ElMessage.error(errMsg || '学习计划生成失败')
      }
    })
  } catch {
    generating.value = false
    ElMessage.error('创建任务失败')
  }
}

// 进度追踪函数
function isDayCompleted(dayIndex) {
  return dayCompletedMap.value[dayIndex] || false
}

async function toggleDayComplete(dayIndex, completed) {
  if (!currentPlanId.value) {
    ElMessage.warning('请先保存计划')
    return
  }
  try {
    await updatePlanProgress(currentPlanId.value, dayIndex, { completed })
    dayCompletedMap.value[dayIndex] = completed
    if (completed) {
      completedDays.value++
    } else {
      completedDays.value--
    }
    ElMessage.success(completed ? '已标记完成' : '已取消完成')
  } catch (err) {
    ElMessage.error(err?.message || '更新失败')
  }
}

async function updateActualHours(dayIndex, hours) {
  if (!currentPlanId.value) return
  try {
    await updatePlanProgress(currentPlanId.value, dayIndex, { actualHours: hours })
    loadProgressStats()
  } catch (err) {
    ElMessage.error(err?.message || '更新失败')
  }
}

async function loadProgress() {
  if (!currentPlanId.value) return
  try {
    const progressList = await getPlanProgressList(currentPlanId.value)
    const completedMap = {}
    const hoursMap = {}
    let completed = 0
    let totalHours = 0

    progressList.forEach(p => {
      if (p.completed) {
        completedMap[p.dayIndex] = true
        completed++
      }
      if (p.actualHours) {
        hoursMap[p.dayIndex] = p.actualHours
        totalHours += p.actualHours
      }
    })

    dayCompletedMap.value = completedMap
    dayActualHours.value = hoursMap
    completedDays.value = completed
    totalActualHours.value = totalHours
  } catch (err) {
    console.error('加载进度失败:', err)
  }
}

async function loadProgressStats() {
  if (!currentPlanId.value) return
  try {
    const stats = await getPlanProgressStats(currentPlanId.value)
    completedDays.value = stats.completedDays || 0
    totalActualHours.value = stats.totalActualHours || 0
  } catch (err) {
    console.error('加载统计失败:', err)
  }
}

onMounted(async () => {
  materialList.value = await loadReadyMaterials()
  // 恢复未完成的任务
  const activeTask = taskStore.getFirstActiveOfType('plan')
  if (activeTask) {
    generating.value = true
    generateMsg.value = '恢复任务中...'
    taskStore.watchTask(activeTask.taskId, 'plan', {
      onProgress(pct, msg) {
        generateProgress.value = pct
        generateMsg.value = msg
      },
      onSuccess(result) {
        plan.value = result.plan || []
        planTitle.value = result.title || '学习计划'
        planGoal.value = result.goal || ''
        totalDays.value = result.totalDays || plan.value.length
        generating.value = false
        ElMessage.success('学习计划生成成功')
      },
      onError(errMsg) {
        generating.value = false
        ElMessage.error(errMsg || '学习计划生成失败')
      }
    })
  }
})
</script>

<style scoped>
.plan-page {
  width: 100%;
  max-width: 800px;
}

.plan-form-card {
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-6);
}

.plan-form-card :deep(.el-card__body) {
  padding: var(--space-5);
}

.plan-form-title {
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--space-4);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.form-full {
  grid-column: 1 / -1;
}

.hours-input {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.unit-label {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-2);
}

/* Generate Progress */
.generate-progress-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
  padding: 16px;
  background: var(--surface-container-low);
  border-radius: var(--radius-md);
}

.generate-progress-info {
  flex: 1;
  min-width: 0;
}

.generate-progress-msg {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
  margin: 0 0 8px 0;
}

.generate-progress-bar {
  width: 100%;
  height: 6px;
  border-radius: 3px;
  background: var(--surface-container);
  overflow: hidden;
}

.generate-progress-fill {
  height: 100%;
  border-radius: 3px;
  background: var(--color-primary);
  transition: width 0.5s var(--ease-default);
}

.spinning { animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* Progress Stats */
.progress-stats-card {
  background: var(--surface-card);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}

.stats-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.stats-header h3 {
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.stats-content {
  display: flex;
  align-items: center;
  gap: var(--space-6);
}

.stat-details {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}

.stat-value {
  font-size: var(--text-body);
  font-weight: 600;
  color: var(--color-text-primary);
}

/* Plan Result */
.plan-result {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.plan-info-card {
  background: var(--surface-card);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
}

.plan-title {
  font-size: var(--text-heading-2);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
}

.plan-goal {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
  margin-top: var(--space-1);
  margin-bottom: 0;
}

.plan-meta {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.meta-item {
  display: flex;
  align-items: baseline;
  gap: var(--space-1);
}

.meta-value {
  font-size: var(--text-display);
  font-weight: 700;
  color: var(--color-primary);
  line-height: 1;
}

.meta-label {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
}

.meta-divider {
  width: 1px;
  height: 32px;
  background: var(--outline-variant);
}

/* Timeline */
.plan-timeline {
  display: flex;
  flex-direction: column;
}

.plan-day {
  display: flex;
  gap: var(--space-4);
}

.day-marker {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 24px;
}

.day-dot {
  width: 12px;
  height: 12px;
  background: var(--color-primary);
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}

.day-line {
  width: 2px;
  flex: 1;
  background: var(--outline-variant);
  margin-top: 4px;
}

.day-content {
  flex: 1;
  padding-bottom: var(--space-5);
}

.plan-day.completed .day-content {
  opacity: 0.7;
}

.plan-day.completed .day-card {
  background: var(--color-success-bg, #f0fdf4);
  border-color: var(--color-success, #16a34a);
}

.day-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-2);
}

.day-checkbox {
  margin-top: 4px;
}

.day-actual {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--outline-variant);
}

.day-actual .unit-label {
  font-size: var(--text-small);
  color: var(--color-text-secondary);
}

.day-label {
  font-size: var(--text-body);
  font-weight: 600;
  color: var(--color-text-primary);
}

.day-date {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
}

.day-card {
  background: var(--surface-card);
  border: 1px solid var(--outline);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.day-topics {
  font-size: var(--text-body);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--space-3);
}

.day-detail {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-2);
  font-size: var(--text-body);
  line-height: 1.6;
}

.detail-label {
  font-weight: 500;
  color: var(--color-text-secondary);
  flex-shrink: 0;
}

.detail-value {
  color: var(--color-text-primary);
}

.plan-empty :deep(.app-empty) {
  padding: var(--space-10) 0;
}

@media (max-width: 767px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .plan-info-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .plan-meta {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
