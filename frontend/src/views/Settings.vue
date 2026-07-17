<template>
  <div class="settings-page">
    <BasePageHeader
      title="设置"
      description="管理账号安全与偏好设置"
    />

    <div class="settings-grid">
      <BaseCard class="settings-card" title="外观">
        <div class="appearance-setting">
          <div>
            <div class="preference-title">界面主题</div>
            <div class="preference-desc">可跟随电脑系统，也可以固定使用浅色或深色</div>
          </div>
          <el-radio-group
            :model-value="uiStore.themeMode"
            aria-label="界面主题"
            @change="uiStore.setThemeMode"
          >
            <el-radio-button value="system">
              <el-icon><Monitor /></el-icon>
              <span>跟随系统</span>
            </el-radio-button>
            <el-radio-button value="light">
              <el-icon><Sunny /></el-icon>
              <span>浅色</span>
            </el-radio-button>
            <el-radio-button value="dark">
              <el-icon><Moon /></el-icon>
              <span>深色</span>
            </el-radio-button>
          </el-radio-group>
        </div>
        <div class="theme-status">当前显示：{{ uiStore.resolvedTheme === 'dark' ? '深色' : '浅色' }}</div>
      </BaseCard>

      <!-- 账号安全 -->
      <BaseCard class="settings-card" title="账号安全">
        <el-form label-position="top" class="settings-form">
          <el-form-item label="当前密码">
            <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
          </el-form-item>
        </el-form>
        <template #footer>
          <div class="section-actions">
            <el-button type="primary" @click="handleChangePassword">修改密码</el-button>
          </div>
        </template>
      </BaseCard>

      <!-- 通知偏好 -->
      <BaseCard class="settings-card" title="通知偏好">
        <div class="preference-list">
          <div class="preference-item">
            <div>
              <div class="preference-title">学习提醒</div>
              <div class="preference-desc">按计划发送学习提醒</div>
            </div>
            <el-switch v-model="preferences.studyReminder" />
          </div>
          <div class="preference-item">
            <div>
              <div class="preference-title">每周学习报告</div>
              <div class="preference-desc">每周汇总学习进度</div>
            </div>
            <el-switch v-model="preferences.weeklyReport" />
          </div>
        </div>
      </BaseCard>

      <!-- 关于 -->
      <BaseCard class="settings-card" title="关于">
        <div class="about-info">
          <div class="about-row">
            <span class="about-label">版本</span>
            <span class="about-value">v0.0.1</span>
          </div>
          <div class="about-row">
            <span class="about-label">技术栈</span>
            <span class="about-value">Spring Boot 3 + Vue 3 + Spring AI</span>
          </div>
        </div>
      </BaseCard>
    </div>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'
import { useUiStore } from '@/stores/ui'
import BaseCard from '@/components/common/BaseCard.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'

const uiStore = useUiStore()

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const preferences = reactive({
  studyReminder: JSON.parse(localStorage.getItem('pref_studyReminder') ?? 'true'),
  weeklyReport: JSON.parse(localStorage.getItem('pref_weeklyReport') ?? 'false')
})

watch(preferences, (val) => {
  localStorage.setItem('pref_studyReminder', JSON.stringify(val.studyReminder))
  localStorage.setItem('pref_weeklyReport', JSON.stringify(val.weeklyReport))
}, { deep: true })

async function handleChangePassword() {
  if (!passwordForm.oldPassword) {
    ElMessage.warning('请输入当前密码')
    return
  }
  if (!passwordForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    return
  }
  try {
    await api.put('/user/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } catch (error) {
    // api 拦截器已处理错误提示
  }
}
</script>

<style scoped>
.settings-page {
  width: 100%;
  max-width: 820px;
}

.settings-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.settings-card {
  border-radius: var(--radius-lg);
}

.appearance-setting {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-6);
}

.appearance-setting :deep(.el-radio-button__inner) {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.theme-status {
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--outline-variant);
  color: var(--color-text-tertiary);
  font-size: var(--text-small);
}

.settings-form :deep(.el-form-item) {
  margin-bottom: var(--space-4);
}

.settings-form :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.section-actions {
  display: flex;
  justify-content: flex-end;
}

.preference-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.preference-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) 0;
}

.preference-item + .preference-item {
  border-top: 1px solid var(--outline-variant);
}

.preference-title {
  font-size: var(--text-body);
  font-weight: 500;
  color: var(--color-text-primary);
}

.preference-desc {
  font-size: var(--text-small);
  color: var(--color-text-tertiary);
  margin-top: var(--space-1);
}

.about-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.about-row {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.about-label {
  font-size: var(--text-ui);
  color: var(--color-text-tertiary);
  flex-shrink: 0;
  width: 64px;
}

.about-value {
  font-size: var(--text-body);
  color: var(--color-text-primary);
}

@media (max-width: 767px) {
  .appearance-setting {
    align-items: stretch;
    flex-direction: column;
  }

  .appearance-setting :deep(.el-radio-group) {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
  }

  .appearance-setting :deep(.el-radio-button__inner) {
    justify-content: center;
    width: 100%;
    padding-inline: var(--space-2);
  }
}
</style>
