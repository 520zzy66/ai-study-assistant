<template>
  <div class="login-page">
    <!-- Login Card -->
    <div class="login-card">
      <!-- Left: Brand -->
      <div class="login-brand">
        <div class="brand-content">
          <div class="brand-icon-row">
            <div class="brand-mark">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/>
                <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>
              </svg>
            </div>
            <span class="brand-badge">AI 驱动</span>
          </div>
          <h1 class="brand-title">AI Study<br/>Assistant</h1>
          <p class="brand-desc">
            你的智能学习伙伴。上传资料，AI 帮你总结知识、答疑解惑、生成练习、规划学习路径。
          </p>
          <div class="brand-features">
            <div class="feature-card">
              <div class="feature-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                  <line x1="16" y1="13" x2="8" y2="13"/>
                  <line x1="16" y1="17" x2="8" y2="17"/>
                </svg>
              </div>
              <div>
                <div class="feature-name">智能总结</div>
                <div class="feature-hint">一键提取文档核心知识</div>
              </div>
            </div>
            <div class="feature-card">
              <div class="feature-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                  <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
                  <line x1="12" y1="17" x2="12.01" y2="17"/>
                </svg>
              </div>
              <div>
                <div class="feature-name">文档问答</div>
                <div class="feature-hint">基于资料精准答疑</div>
              </div>
            </div>
            <div class="feature-card">
              <div class="feature-icon">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M9 11l3 3L22 4"/>
                  <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
                </svg>
              </div>
              <div>
                <div class="feature-name">自动出题</div>
                <div class="feature-hint">AI 出题 + 智能判分</div>
              </div>
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
import { User, Lock } from '@element-plus/icons-vue'
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
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
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
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return
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
  padding: clamp(var(--space-4), 4vw, var(--space-12));
  background: var(--surface-page);
}

.login-card {
  display: flex;
  width: min(1100px, 92vw);
  min-height: 600px;
  background: var(--surface-card);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  border: 1px solid var(--outline);
}

/* ---- Left Brand ---- */
.login-brand {
  flex: 1;
  background: linear-gradient(145deg, #1f3d1c 0%, var(--color-primary) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-10) var(--space-8);
  position: relative;
  overflow: hidden;
}

/* Subtle dot pattern */
.login-brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 20% 18%, rgba(255,255,255,0.12), transparent 36%),
                    radial-gradient(circle at 82% 78%, rgba(255,255,255,0.08), transparent 30%);
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 320px;
}

.brand-icon-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-8);
}

.brand-mark {
  width: 48px;
  height: 48px;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  padding: var(--space-1) var(--space-3);
  background: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.9);
  border-radius: var(--radius-full);
  font-size: var(--text-small);
  font-weight: 600;
  letter-spacing: 0.02em;
  backdrop-filter: blur(4px);
}

.brand-title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  letter-spacing: -0.03em;
  line-height: 1.15;
  margin-bottom: var(--space-5);
}

.brand-desc {
  font-size: var(--text-body);
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.75);
  margin-bottom: var(--space-8);
}

/* Feature cards */
.brand-features {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.feature-card {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-md);
  border: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(4px);
}

.feature-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.feature-name {
  font-size: var(--text-ui);
  font-weight: 600;
  color: #fff;
}

.feature-hint {
  font-size: var(--text-small);
  color: rgba(255, 255, 255, 0.6);
  margin-top: 2px;
}

/* ---- Right Form ---- */
.login-form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-10) var(--space-8);
  background: var(--surface-card);
}

.form-content {
  width: 100%;
  max-width: 380px;
}

.form-header {
  margin-bottom: var(--space-8);
}

.form-title {
  font-size: var(--text-hero);
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: -0.02em;
  margin-bottom: var(--space-2);
}

.form-subtitle {
  font-size: var(--text-body);
  color: var(--color-text-secondary);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.form-options {
  display: flex;
  justify-content: flex-start;
  margin-top: calc(-1 * var(--space-1));
}

.login-btn {
  width: 100%;
  height: 48px !important;
  font-size: var(--text-body) !important;
  font-weight: 600 !important;
  margin-top: var(--space-1);
  border-radius: var(--radius-md) !important;
  letter-spacing: 0.02em;
}

.login-footer {
  text-align: center;
  margin-top: var(--space-6);
  font-size: var(--text-ui);
}

.footer-text {
  color: var(--color-text-tertiary);
  margin-right: var(--space-1);
}

/* ---- Responsive ---- */
@media (max-width: 1024px) {
  .login-card {
    width: min(920px, 94vw);
  }
}

@media (max-width: 768px) {
  .login-page {
    padding: var(--space-4);
  }

  .login-card {
    width: 100%;
    max-width: 420px;
    flex-direction: column;
  }

  .login-brand {
    padding: var(--space-8) var(--space-6);
  }

  .brand-title { font-size: 28px; }
  .brand-desc { font-size: var(--text-ui); margin-bottom: var(--space-5); }
  .brand-features { display: none; }

  .login-form-area {
    padding: var(--space-6);
  }
}
</style>
