<template>
  <div class="login-page">
    <!-- Left Brand -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-icon">AI</div>
        <h1 class="brand-title">智能学习助手</h1>
        <p class="brand-desc">
          上传学习资料，AI 帮你总结知识、生成题目、制定计划。<br />
          让学习更高效，让知识更有条理。
        </p>
        <div class="brand-features">
          <div class="feature-item">
            <el-icon :size="18"><Document /></el-icon>
            <span>文档智能总结</span>
          </div>
          <div class="feature-item">
            <el-icon :size="18"><ChatDotRound /></el-icon>
            <span>RAG 知识问答</span>
          </div>
          <div class="feature-item">
            <el-icon :size="18"><Edit /></el-icon>
            <span>AI 自动出题</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Right Form -->
    <div class="login-form-area">
      <div class="login-form-wrapper">
        <div class="form-header">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">登录你的账号继续学习</p>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名"
              size="large"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              show-password
              size="large"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-footer">
          <span class="footer-text">没有账号？</span>
          <el-link type="primary" :underline="false" @click="showRegister = true">立即注册</el-link>
        </div>
      </div>
    </div>

    <!-- Register Dialog -->
    <el-dialog v-model="showRegister" title="注册新账号" width="440px" :close-on-click-modal="false">
      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="registerForm.username" placeholder="4-20位，字母数字下划线" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="6-20位" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="可选" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="registerForm.email" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" @click="handleRegister">注册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const registerFormRef = ref(null)
const loading = ref(false)
const registerLoading = ref(false)
const showRegister = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  email: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^\w{4,20}$/, message: '用户名4-20位，仅字母数字下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码6-20位', trigger: 'blur' }
  ]
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error('登录失败', error)
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  await registerFormRef.value.validate()
  registerLoading.value = true
  try {
    await userStore.register(registerForm)
    ElMessage.success('注册成功，请登录')
    showRegister.value = false
  } catch (error) {
    console.error('注册失败', error)
  } finally {
    registerLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  background: var(--surface-page);
}

/* Brand */
.login-brand {
  flex: 1;
  background: linear-gradient(135deg, var(--teal-50) 0%, var(--teal-100) 50%, var(--teal-200) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-12);
  position: relative;
  overflow: hidden;
}

.login-brand::before {
  content: '';
  position: absolute;
  top: -30%;
  right: -20%;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(5, 150, 105, 0.08) 0%, transparent 70%);
  border-radius: 50%;
}

.brand-content {
  max-width: 400px;
  position: relative;
}

.brand-icon {
  width: 48px;
  height: 48px;
  background: var(--color-primary);
  color: var(--color-on-primary);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-heading-3);
  font-weight: 700;
  letter-spacing: -0.02em;
  margin-bottom: var(--space-6);
}

.brand-title {
  font-size: var(--text-hero);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.03em;
  line-height: 1.2;
  margin-bottom: var(--space-4);
}

.brand-desc {
  font-size: var(--text-body-large);
  line-height: 1.7;
  color: var(--color-text-secondary);
  margin-bottom: var(--space-8);
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.feature-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--text-body);
  color: var(--color-text-secondary);
  font-weight: 500;
}

.feature-item .el-icon {
  color: var(--color-primary);
}

/* Form */
.login-form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-12);
  background: var(--surface-card);
}

.login-form-wrapper {
  width: 100%;
  max-width: 360px;
}

.form-title {
  font-size: var(--text-heading-1);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
  margin-bottom: var(--space-1);
}

.form-subtitle {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-8);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.login-btn {
  width: 100%;
  margin-top: var(--space-2);
}

.login-footer {
  text-align: center;
  margin-top: var(--space-6);
  font-size: var(--text-small);
}

.footer-text {
  color: var(--color-text-secondary);
  margin-right: var(--space-1);
}

/* Responsive */
@media (max-width: 768px) {
  .login-page {
    flex-direction: column;
  }

  .login-brand {
    padding: var(--space-8) var(--space-6);
    min-height: auto;
  }

  .brand-title {
    font-size: var(--text-display);
  }

  .brand-features {
    display: none;
  }

  .login-form-area {
    padding: var(--space-8) var(--space-6);
  }
}
</style>
