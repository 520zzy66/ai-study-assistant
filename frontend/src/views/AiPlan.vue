<template>
  <div class="plan-page">
    <div class="page-header">
      <h2>AI 学习计划</h2>
      <p>输入学习目标和时间，AI 帮你制定每日学习任务</p>
    </div>

    <!-- Form Card -->
    <el-card shadow="never" class="plan-form-card">
      <div class="plan-form-title">生成学习计划</div>
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
            生成学习计划
          </el-button>
        </div>
      </el-form>
    </el-card>

    <!-- Result -->
    <div v-if="plan.length > 0" class="plan-result">
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
        <div v-for="(day, index) in plan" :key="index" class="plan-day">
          <div class="day-marker">
            <div class="day-dot" />
            <div v-if="index < plan.length - 1" class="day-line" />
          </div>
          <div class="day-content">
            <div class="day-header">
              <span class="day-label">第 {{ day.day }} 天</span>
              <span class="day-date">{{ day.date }} {{ day.weekday }}</span>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { generatePlan } from '@/api/ai'
import { loadReadyMaterials } from '@/api/material'
import AppEmpty from '@/components/common/AppEmpty.vue'

const materialList = ref([])
const plan = ref([])
const planTitle = ref('')
const planGoal = ref('')
const totalDays = ref(0)
const generating = ref(false)

const planForm = reactive({
  goal: '',
  examDate: '',
  dailyHours: 2,
  materialIds: []
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
  try {
    const data = await generatePlan(planForm)
    plan.value = data.plan || []
    planTitle.value = data.title || '学习计划'
    planGoal.value = data.goal || planForm.goal
    totalDays.value = data.totalDays || plan.value.length
    ElMessage.success('学习计划生成成功')
  } catch (error) {
    plan.value = []
  } finally {
    generating.value = false
  }
}

onMounted(async () => {
  materialList.value = await loadReadyMaterials()
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

.day-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-2);
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
  padding: var(--space-16) 0;
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
