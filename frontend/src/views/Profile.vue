<template>
  <div class="profile-page">
    <BasePageHeader
      title="用户中心"
      description="管理你的个人信息和学习画像"
    />

    <!-- 基本信息卡片 -->
    <BaseCard class="profile-card" :padding="'lg'">
      <template #header>
        <h3 class="card-title">基本信息</h3>
      </template>

      <div class="profile-layout">
        <!-- 左侧头像 -->
        <div class="profile-avatar-section">
          <el-avatar :size="72" class="profile-avatar">
            {{ (userStore.userInfo?.nickname || '用')[0] }}
          </el-avatar>
          <p class="avatar-hint">头像由昵称自动生成</p>
        </div>

        <!-- 右侧表单 -->
        <el-form label-position="top" class="profile-form">
          <div class="form-grid">
            <el-form-item label="昵称">
              <el-input v-model="basicForm.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model="basicForm.username" disabled />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="basicForm.email" disabled />
            </el-form-item>
            <el-form-item label="注册时间">
              <el-input v-model="basicForm.createdAt" disabled />
            </el-form-item>
          </div>
          <div class="profile-actions">
            <el-button type="primary" @click="handleSaveBasic">保存修改</el-button>
          </div>
        </el-form>
      </div>
    </BaseCard>

    <!-- 学习画像卡片 -->
    <BaseCard class="profile-card" :padding="'lg'">
      <template #header>
        <div class="card-header-with-action">
          <h3 class="card-title">学习画像</h3>
          <el-tag v-if="profileData.compressedProfile" type="success" size="small">
            AI 已分析
          </el-tag>
        </div>
      </template>

      <!-- AI 压缩画像展示 -->
      <div v-if="profileData.compressedProfile" class="ai-profile-section">
        <div class="ai-profile-label">
          <el-icon><MagicStick /></el-icon>
          <span>AI 学习画像分析</span>
        </div>
        <div class="ai-profile-content">
          {{ profileData.compressedProfile }}
        </div>
        <div class="ai-profile-meta">
          <span v-if="profileData.compressedAt">
            更新于 {{ formatTime(profileData.compressedAt) }}
          </span>
        </div>
      </div>

      <!-- 画像编辑表单 -->
      <el-form label-position="top" class="profile-form">
        <div class="form-grid form-grid-3">
          <el-form-item label="年龄">
            <el-input-number
              v-model="profileForm.age"
              :min="6"
              :max="80"
              placeholder="请输入年龄"
              controls-position="right"
            />
          </el-form-item>

          <el-form-item label="学籍">
            <el-select v-model="profileForm.education" placeholder="请选择学籍" clearable>
              <el-option label="高中" value="high_school" />
              <el-option label="大专" value="junior_college" />
              <el-option label="本科" value="undergraduate" />
              <el-option label="硕士" value="master" />
              <el-option label="博士" value="doctor" />
              <el-option label="在职" value="working" />
              <el-option label="应届生" value="fresh_graduate" />
            </el-select>
          </el-form-item>

          <el-form-item label="目标考试">
            <el-input v-model="profileForm.targetExam" placeholder="如：考研数学一、考公、雅思" />
          </el-form-item>

          <el-form-item label="学习科目">
            <el-input v-model="profileForm.studySubject" placeholder="如：高等数学、英语、行测" />
          </el-form-item>

          <el-form-item label="每日学习目标（分钟）">
            <el-input-number
              v-model="profileForm.dailyStudyGoalMinutes"
              :min="10"
              :max="720"
              :step="10"
              controls-position="right"
            />
          </el-form-item>

          <el-form-item label="难度偏好">
            <el-select v-model="profileForm.preferredDifficulty" placeholder="请选择难度偏好">
              <el-option label="简单" value="easy" />
              <el-option label="适中" value="normal" />
              <el-option label="困难" value="hard" />
            </el-select>
          </el-form-item>

          <el-form-item label="学习风格">
            <el-select v-model="profileForm.learningStyle" placeholder="请选择学习风格">
              <el-option label="视觉型（图表、视频）" value="visual" />
              <el-option label="听觉型（讲解、音频）" value="auditory" />
              <el-option label="实践型（做题、实操）" value="practice" />
            </el-select>
          </el-form-item>
        </div>

        <!-- 薄弱知识点 -->
        <el-form-item label="薄弱知识点">
          <div class="tag-input-wrapper">
            <el-tag
              v-for="(tag, index) in profileForm.weakPoints"
              :key="index"
              closable
              type="danger"
              @close="removeWeakPoint(index)"
            >
              {{ tag }}
            </el-tag>
            <el-input
              v-if="weakPointInputVisible"
              ref="weakPointInputRef"
              v-model="weakPointInputValue"
              class="tag-input"
              size="small"
              @keyup.enter="addWeakPoint"
              @blur="addWeakPoint"
            />
            <el-button v-else class="tag-add-btn" size="small" @click="showWeakPointInput">
              + 添加知识点
            </el-button>
          </div>
        </el-form-item>

        <!-- 擅长知识点 -->
        <el-form-item label="擅长知识点">
          <div class="tag-input-wrapper">
            <el-tag
              v-for="(tag, index) in profileForm.strongPoints"
              :key="index"
              closable
              type="success"
              @close="removeStrongPoint(index)"
            >
              {{ tag }}
            </el-tag>
            <el-input
              v-if="strongPointInputVisible"
              ref="strongPointInputRef"
              v-model="strongPointInputValue"
              class="tag-input"
              size="small"
              @keyup.enter="addStrongPoint"
              @blur="addStrongPoint"
            />
            <el-button v-else class="tag-add-btn" size="small" @click="showStrongPointInput">
              + 添加知识点
            </el-button>
          </div>
        </el-form-item>

        <div class="profile-actions">
          <el-button type="primary" :loading="saving" @click="handleSaveProfile">
            保存画像
          </el-button>
        </div>
      </el-form>

      <!-- 学习统计 -->
      <div class="stats-section">
        <h4 class="stats-title">学习统计</h4>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-value">{{ profileData.totalStudyDays || 0 }}</div>
            <div class="stat-label">累计学习天数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ profileData.totalQuestions || 0 }}</div>
            <div class="stat-label">累计答题数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ formatAccuracy(profileData.accuracyRate) }}%</div>
            <div class="stat-label">答题正确率</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ profileData.overallLevel || 0 }}</div>
            <div class="stat-label">综合水平</div>
          </div>
        </div>
      </div>
    </BaseCard>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { MagicStick } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getUserProfile, updateUserProfile } from '@/api/user'
import api from '@/api'
import BaseCard from '@/components/common/BaseCard.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'

const userStore = useUserStore()
const saving = ref(false)

// 基本信息表单
const basicForm = reactive({
  nickname: '',
  email: '',
  username: '',
  createdAt: ''
})

// 画像表单
const profileForm = reactive({
  age: null,
  education: '',
  targetExam: '',
  studySubject: '',
  dailyStudyGoalMinutes: 60,
  preferredDifficulty: 'normal',
  learningStyle: '',
  weakPoints: [],
  strongPoints: []
})

// 画像数据（包含AI压缩画像和统计数据）
const profileData = reactive({
  compressedProfile: '',
  compressedAt: null,
  totalStudyDays: 0,
  totalQuestions: 0,
  accuracyRate: 0,
  overallLevel: 50
})

// 薄弱知识点输入
const weakPointInputVisible = ref(false)
const weakPointInputValue = ref('')
const weakPointInputRef = ref(null)

// 擅长知识点输入
const strongPointInputVisible = ref(false)
const strongPointInputValue = ref('')
const strongPointInputRef = ref(null)

onMounted(async () => {
  await Promise.all([
    loadBasicInfo(),
    loadProfile()
  ])
})

/** 加载基本信息 */
async function loadBasicInfo() {
  try {
    const res = await api.get('/user/profile')
    const info = res.data
    basicForm.nickname = info.nickname || ''
    basicForm.email = info.email || ''
    basicForm.username = info.username || ''
    basicForm.createdAt = info.createTime || ''
    userStore.userInfo = info
  } catch {
    const info = userStore.userInfo || {}
    basicForm.nickname = info.nickname || ''
    basicForm.email = info.email || ''
    basicForm.username = info.username || ''
    basicForm.createdAt = info.createTime || ''
  }
}

/** 加载用户画像 */
async function loadProfile() {
  try {
    const res = await getUserProfile()
    const data = res.data

    // 填充画像表单
    profileForm.age = data.age || null
    profileForm.education = data.education || ''
    profileForm.targetExam = data.targetExam || ''
    profileForm.studySubject = data.studySubject || ''
    profileForm.dailyStudyGoalMinutes = data.dailyStudyGoalMinutes || 60
    profileForm.preferredDifficulty = data.preferredDifficulty || 'normal'
    profileForm.learningStyle = data.learningStyle || ''
    profileForm.weakPoints = data.weakPoints || []
    profileForm.strongPoints = data.strongPoints || []

    // 填充统计数据和AI画像
    profileData.compressedProfile = data.compressedProfile || ''
    profileData.compressedAt = data.compressedAt || null
    profileData.totalStudyDays = data.totalStudyDays || 0
    profileData.totalQuestions = data.totalQuestions || 0
    profileData.accuracyRate = data.accuracyRate || 0
    profileData.overallLevel = data.overallLevel || 50
  } catch (error) {
    console.error('加载用户画像失败:', error)
  }
}

/** 保存基本信息 */
async function handleSaveBasic() {
  if (!basicForm.nickname?.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  try {
    await api.put('/user/profile', { nickname: basicForm.nickname.trim() })
    await userStore.getUserInfo()
    ElMessage.success('保存成功')
  } catch {
    ElMessage.error('保存失败')
  }
}

/** 保存用户画像 */
async function handleSaveProfile() {
  saving.value = true
  try {
    await updateUserProfile({
      age: profileForm.age,
      education: profileForm.education || undefined,
      targetExam: profileForm.targetExam || undefined,
      studySubject: profileForm.studySubject || undefined,
      dailyStudyGoalMinutes: profileForm.dailyStudyGoalMinutes,
      preferredDifficulty: profileForm.preferredDifficulty || undefined,
      learningStyle: profileForm.learningStyle || undefined,
      weakPoints: profileForm.weakPoints.length > 0 ? profileForm.weakPoints : undefined,
      strongPoints: profileForm.strongPoints.length > 0 ? profileForm.strongPoints : undefined
    })
    ElMessage.success('画像保存成功')
  } catch {
    ElMessage.error('画像保存失败')
  } finally {
    saving.value = false
  }
}

/** 移除薄弱知识点 */
function removeWeakPoint(index) {
  profileForm.weakPoints.splice(index, 1)
}

/** 显示薄弱知识点输入框 */
function showWeakPointInput() {
  weakPointInputVisible.value = true
  nextTick(() => {
    weakPointInputRef.value?.focus()
  })
}

/** 添加薄弱知识点 */
function addWeakPoint() {
  const value = weakPointInputValue.value.trim()
  if (value && !profileForm.weakPoints.includes(value)) {
    profileForm.weakPoints.push(value)
  }
  weakPointInputVisible.value = false
  weakPointInputValue.value = ''
}

/** 移除擅长知识点 */
function removeStrongPoint(index) {
  profileForm.strongPoints.splice(index, 1)
}

/** 显示擅长知识点输入框 */
function showStrongPointInput() {
  strongPointInputVisible.value = true
  nextTick(() => {
    strongPointInputRef.value?.focus()
  })
}

/** 添加擅长知识点 */
function addStrongPoint() {
  const value = strongPointInputValue.value.trim()
  if (value && !profileForm.strongPoints.includes(value)) {
    profileForm.strongPoints.push(value)
  }
  strongPointInputVisible.value = false
  strongPointInputValue.value = ''
}

/** 格式化时间 */
function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/** 格式化正确率 */
function formatAccuracy(rate) {
  if (!rate) return '0'
  return Number(rate).toFixed(1)
}
</script>

<style scoped>
.profile-page {
  width: 100%;
  max-width: 960px;
}

.profile-card {
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-5);
}

.card-title {
  font-size: var(--text-heading-2);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.card-header-with-action {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.profile-layout {
  display: flex;
  gap: var(--space-8);
  align-items: flex-start;
}

.profile-avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  padding-top: var(--space-2);
}

.profile-avatar {
  background: var(--surface-active);
  color: var(--color-primary);
  font-size: var(--text-heading-1);
  font-weight: 600;
}

.avatar-hint {
  margin-top: var(--space-2);
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  cursor: pointer;
}

.avatar-hint:hover {
  color: var(--color-primary);
}

.profile-form {
  flex: 1;
  min-width: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.form-grid-3 {
  grid-template-columns: 1fr 1fr 1fr;
}

.profile-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.profile-form :deep(.el-form-item__label) {
  font-weight: 500;
}

.profile-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-6);
}

/* AI 画像展示区域 */
.ai-profile-section {
  background: var(--color-primary-light-9);
  border: 1px solid var(--outline);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  margin-bottom: var(--space-6);
}

.ai-profile-label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-small);
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: var(--space-2);
}

.ai-profile-content {
  font-size: var(--text-body);
  color: var(--color-text-primary);
  line-height: 1.8;
  white-space: pre-wrap;
}

.ai-profile-meta {
  margin-top: var(--space-2);
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
}

/* 标签输入 */
.tag-input-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  align-items: center;
}

.tag-input {
  width: 120px;
}

.tag-add-btn {
  border-style: dashed;
}

/* 学习统计 */
.stats-section {
  margin-top: var(--space-8);
  padding-top: var(--space-6);
  border-top: 1px solid var(--color-border);
}

.stats-title {
  font-size: var(--text-heading-2);
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 var(--space-4) 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
}

.stat-item {
  text-align: left;
  padding: var(--space-4);
  background: var(--surface-container-low);
  border-radius: var(--radius-md);
}

.stat-value {
  font-size: var(--text-heading-1);
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: var(--space-1);
}

.stat-label {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
}

/* Element Plus 输入框样式覆盖 */
.profile-form :deep(.el-input-number) {
  width: 100%;
}

.profile-form :deep(.el-select) {
  width: 100%;
}

@media (max-width: 768px) {
  .profile-layout {
    flex-direction: column;
    align-items: center;
  }

  .form-grid,
  .form-grid-3 {
    grid-template-columns: 1fr;
  }

  .profile-form {
    width: 100%;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
