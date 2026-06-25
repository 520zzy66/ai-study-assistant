<template>
  <div class="profile-page">
    <BasePageHeader
      title="用户中心"
      description="管理你的个人信息"
    />

    <BaseCard class="profile-card" :padding="'lg'">
      <div class="profile-layout">
        <!-- 左侧头像 -->
        <div class="profile-avatar-section">
          <el-avatar :size="72" class="profile-avatar">
            {{ (userStore.userInfo?.nickname || '用')[0] }}
          </el-avatar>
          <p class="avatar-hint">点击更换头像</p>
        </div>

        <!-- 右侧表单 -->
        <el-form label-position="top" class="profile-form">
          <div class="form-grid">
            <el-form-item label="昵称">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model="form.username" disabled />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="form.email" disabled />
            </el-form-item>
            <el-form-item label="注册时间">
              <el-input v-model="form.createdAt" disabled />
            </el-form-item>
          </div>
          <div class="profile-actions">
            <el-button type="primary" @click="handleSave">保存修改</el-button>
          </div>
        </el-form>
      </div>
    </BaseCard>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import api from '@/api'
import BaseCard from '@/components/common/BaseCard.vue'
import BasePageHeader from '@/components/common/BasePageHeader.vue'

const userStore = useUserStore()

const form = reactive({
  nickname: '',
  email: '',
  username: '',
  createdAt: ''
})

onMounted(async () => {
  try {
    const res = await api.get('/user/profile')
    const info = res.data
    form.nickname = info.nickname || ''
    form.email = info.email || ''
    form.username = info.username || ''
    form.createdAt = info.createTime || ''
    userStore.userInfo = info
  } catch {
    const info = userStore.userInfo || {}
    form.nickname = info.nickname || ''
    form.email = info.email || ''
    form.username = info.username || ''
    form.createdAt = info.createTime || ''
  }
})

async function handleSave() {
  if (!form.nickname?.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  try {
    await api.put('/user/profile', { nickname: form.nickname.trim() })
    await userStore.getUserInfo()
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败')
  }
}
</script>

<style scoped>
.profile-page {
  width: 100%;
  max-width: 800px;
}

.profile-card {
  border-radius: var(--radius-lg);
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

@media (max-width: 640px) {
  .profile-layout {
    flex-direction: column;
    align-items: center;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .profile-form {
    width: 100%;
  }
}
</style>
