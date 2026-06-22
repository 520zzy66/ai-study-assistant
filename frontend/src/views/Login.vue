<template>
  <div class="login-page">
    <!-- Background Decorations -->
    <div class="bg-decoration">
      <div class="bg-blob bg-blob-1" />
      <div class="bg-blob bg-blob-2" />
      <div class="bg-blob bg-blob-3" />
      <div class="bg-grid" />
    </div>

    <!-- Login Card -->
    <div class="login-card">
      <!-- Left: Brand -->
      <div class="login-brand">
        <div class="brand-content">
          <div class="brand-mark">
            <el-icon :size="32"><Reading /></el-icon>
          </div>
          <h1 class="brand-title">AI Study<br />Assistant</h1>
          <p class="brand-desc">
            上传学习资料，AI 帮你总结知识、生成题目、制定计划。让学习更高效，让知识更有条理。
          </p>
          <div class="brand-features">
            <div class="feature-item">
              <div class="feature-dot" />
              <span>AI 智能总结文档</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot" />
              <span>基于资料的问答</span>
            </div>
            <div class="feature-item">
              <div class="feature-dot" />
              <span>自动出题与判分</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Form -->
      <div class="login-form-area">
        <div class="form-content">
          <div class="form-header">
            <h2 class="form-title">欢迎回来</h2>
            <p class="form-subtitle">登录你的账号继续学习</p>
          </div>

          <el-form :model="form" :rules="rules" ref="formRef" class="login-form" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input
                v-model="form.username"
                placeholder="用户名"
                size="large"
                :prefix-icon="User"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="密码"
                show-password
                size="large"
                :prefix-icon="Lock"
              />
            </el-form-item>
            <div class="form-options">
              <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            </div>
            <el-button
              type="primary"
              size="large"
              :loading="loading"
              class="login-btn"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form>

          <div class="login-footer">
            <span class="footer-text">没有账号？</span>
            <el-link type="primary" :underline="false" @click="showRegister = true">立即注册</el-link>
          </div>
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
import { User, Lock, Reading } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref(null)
const registerFormRef = ref(null)
const loading = ref(false)
const registerLoading = ref(false)
const showRegister = ref(false)
const rememberMe = ref(false)

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
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: linear-gradient(160deg, #f8fafc 0%, #f1f5f9 40%, #eff6ff 100%);
  position: relative;
  overflow: hidden;
}

/* ---- Background Decoration ---- */
.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

/* Soft glowing orbs */
.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}

.bg-blob-1 {
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.15) 0%, transparent 70%);
  top: -15%;
  right: -8%;
  animation: float-blob 20s ease-in-out infinite;
}

.bg-blob-2 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, rgba(59, 130, 246, 0.12) 0%, transparent 70%);
  bottom: -12%;
  left: -6%;
  animation: float-blob 24s ease-in-out infinite reverse;
}

.bg-blob-3 {
  width: 350px;
  height: 350px;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.08) 0%, transparent 70%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: float-blob 18s ease-in-out infinite 5s;
}

@keyframes float-blob {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -20px) scale(1.05); }
  50% { transform: translate(-15px, 25px) scale(0.95); }
  75% { transform: translate(-25px, -15px) scale(1.02); }
}

/* Subtle geometric grid */
.bg-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  mask-image: radial-gradient(ellipse 80% 60% at 50% 40%, black 30%, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse 80% 60% at 50% 40%, black 30%, transparent 70%);
}

/* Card */
.login-card {
  display: flex;
  width: 100%;
  max-width: 1200px;
  min-height: 700px;
  background: var(--surface-card);
  border-radius: var(--radius-xl);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.08), 0 0 0 1px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  position: relative;
  z-index: 1;
}

/* ---- Left Brand ---- */
.login-brand {
  flex: 1;
  background: linear-gradient(135deg, var(--blue-50) 0%, #dbeafe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 72px;
  position: relative;
}

.brand-content {
  max-width: 340px;
}

.brand-mark {
  width: 64px;
  height: 64px;
  background: var(--color-primary);
  color: #fff;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 32px;
}

.brand-title {
  font-size: 42px;
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.03em;
  line-height: 1.15;
  margin-bottom: 20px;
}

.brand-desc {
  font-size: var(--text-body);
  line-height: 1.7;
  color: var(--color-text-secondary);
  margin-bottom: 32px;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: var(--text-ui);
  color: var(--color-text-secondary);
  font-weight: 500;
}

.feature-dot {
  width: 6px;
  height: 6px;
  background: var(--color-primary);
  border-radius: 50%;
  flex-shrink: 0;
}

/* ---- Right Form ---- */
.login-form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 72px;
  background: var(--surface-card);
}

.form-content {
  width: 100%;
  max-width: 380px;
}

.form-content {
  width: 100%;
  max-width: 340px;
}

.form-header {
  margin-bottom: 32px;
}

.form-title {
  font-size: var(--text-hero);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
  margin-bottom: 6px;
}

.form-subtitle {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.form-options {
  display: flex;
  justify-content: flex-start;
  margin-top: -4px;
}

.login-btn {
  width: 100%;
  height: 48px !important;
  font-size: var(--text-body) !important;
  font-weight: 600 !important;
  margin-top: 4px;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: var(--text-ui);
}

.footer-text {
  color: var(--color-text-secondary);
  margin-right: 4px;
}

/* ---- Responsive ---- */
@media (max-width: 768px) {
  .login-page {
    padding: 16px;
    background-image: none;
  }

  .login-card {
    flex-direction: column;
    max-width: 420px;
    min-height: auto;
  }

  .login-brand {
    padding: 32px 24px 16px;
    background: var(--surface-card);
  }

  .brand-mark { width: 44px; height: 44px; margin-bottom: 16px; }
  .brand-title { font-size: 24px; }
  .brand-desc { font-size: var(--text-ui); margin-bottom: 20px; }
  .brand-features { display: none; }

  .login-form-area {
    padding: 16px 24px 32px;
  }
}
</style>
