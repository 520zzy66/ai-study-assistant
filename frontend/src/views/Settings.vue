<template>
  <div class="settings-page">
    <BasePageHeader
      title="设置"
      description="管理账号安全与偏好设置"
    />

    <BaseCard class="settings-card" :padding="'lg'">
      <div class="settings-section">
        <h3 class="section-title">账号安全</h3>
        <el-form label-position="top">
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
        <div class="section-actions">
          <el-button type="primary" @click="handleChangePassword">修改密码</el-button>
        </div>
      </div>

      <el-divider />

      <div class="settings-section">
        <h3 class="section-title">通知偏好</h3>
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

      <el-divider />

      <div class="settings-section">
        <h3 class="section-title">关于</h3>
        <p class="about-text">AI Study Assistant v0.0.1</p>
      </div>
    </BaseCard>
  </div>
</template>

<script setup>
import { reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'
import BaseCard from '@/components/common/BaseCard.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const preferences = reactive({
  studyReminder: JSON.parse(localStorage.getItem('pref_studyReminder') ?? 'true'),
  weeklyReport: JSON.parse(localStorage.getItem('pref_weeklyReport') ?? 'false')
})

// 持久化偏好设置
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
  max-width: 720px;
}

.settings-card {
  border-radius: var(--radius-lg);
}

.settings-section {
  padding: var(--space-4) 0;
}

.section-title {
  font-size: var(--text-heading-3);
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: var(--space-4);
}

.section-actions {
  display: flex;
  justify-content: flex-end;
}

.preference-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) 0;
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

.about-text {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}
</style>
