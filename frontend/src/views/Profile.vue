<template>
  <div class="profile-page">
    <BasePageHeader
      title="用户中心"
      description="管理你的个人信息"
    />

    <BaseCard class="profile-card" :padding="'lg'">
      <div class="profile-avatar-section">
        <el-avatar :size="80" class="profile-avatar">
          {{ (userStore.userInfo?.nickname || '用')[0] }}
        </el-avatar>
        <p class="avatar-hint">点击更换头像</p>
      </div>

      <el-form label-position="top" class="profile-form">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" disabled />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        <el-form-item label="注册时间">
          <el-input v-model="form.createdAt" disabled />
        </el-form-item>
      </el-form>

      <div class="profile-actions">
        <el-button type="primary" @click="handleSave">保存修改</el-button>
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
  // 优先从接口获取最新用户信息
  try {
    const res = await api.get('/user/profile')
    const info = res.data
    form.nickname = info.nickname || ''
    form.email = info.email || ''
    form.username = info.username || ''
    form.createdAt = info.createTime || ''
    userStore.userInfo = info
  } catch {
    // 降级使用 store 中的缓存数据
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
  max-width: 560px;
}

.profile-card {
  border-radius: var(--radius-lg);
}

.profile-avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: var(--space-6);
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
}

.profile-form :deep(.el-form-item__label) {
  font-weight: 500;
}

.profile-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-6);
}
</style>
