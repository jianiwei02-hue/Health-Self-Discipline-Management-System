<template>
  <!-- 蒙层 - 右侧弹窗 -->
  <div class="login-modal-overlay" @click.self="closeModals">
    <div class="login-card">
      <button class="close-btn" @click="closeModals">✕</button>

      <div class="login-header">
        <h2>欢迎回来</h2>
      </div>

      <!-- 登录表单 -->
      <div class="login-form">
        <div class="input-group">
          <input
              type="text"
              placeholder="账号 / 手机号 / 邮箱"
              class="modern-input"
              v-model="loginForm.username"
              @keyup.enter="handleLogin"
          />
        </div>

        <div class="input-group">
          <input
              :type="showPassword ? 'text' : 'password'"
              placeholder="密码"
              class="modern-input"
              v-model="loginForm.password"
              @keyup.enter="handleLogin"
          />
          <span class="eye-icon" @click="showPassword = !showPassword">
            {{ showPassword ? '🙈' : '👁️' }}
          </span>
        </div>

        <div class="options-row">
          <label class="checkbox-label">
            <input type="checkbox" v-model="rememberMe" />
            <span>记住密码</span>
          </label>
          <a href="#" class="forgot-link" @click.prevent="forgotPassword">忘记密码？</a>
        </div>

        <button class="login-btn" @click="handleLogin" :disabled="isLoading">
          <span v-if="isLoading" class="spinner"></span>
          <span v-else>登 录</span>
        </button>
      </div>

      <!-- 其他登录方式 -->
      <div class="other-methods">
        <div class="divider">
          <span>其他登录方式</span>
        </div>

        <div class="method-row">
          <button class="method-btn" @click="switchToSmsLogin">验证码登录</button>
          <button class="method-btn" @click="switchToEmailLogin">邮箱登录</button>
          <button class="method-btn" @click="handleWechatLogin">微信登录</button>
        </div>
      </div>

      <p class="signup-text">
        还没有账号？<a href="#" @click.prevent="switchToRegister">立即注册</a>
      </p>

      <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
    </div>
  </div>

  <!-- 短信验证码登录 -->
  <div v-if="showSms" class="login-modal-overlay" @click.self="closeSms">
    <div class="login-card">
      <button class="close-btn" @click="closeSms">✕</button>
      <button class="back-btn" @click="showSms = false">← 返回</button>

      <div class="login-header">
        <h2>验证码登录</h2>
      </div>

      <div class="login-form">
        <div class="input-group">
          <input
              type="tel"
              placeholder="手机号"
              class="modern-input"
              v-model="smsForm.phone"
          />
        </div>

        <div class="code-group">
          <input
              type="text"
              placeholder="验证码"
              class="modern-input code-input"
              v-model="smsForm.code"
          />
          <button
              class="send-code-btn"
              @click="sendSmsCode"
              :disabled="smsCountdown > 0 || !isValidPhone"
          >
            {{ smsCountdown > 0 ? `${smsCountdown}s` : '获取验证码' }}
          </button>
        </div>

        <button class="login-btn" @click="handleSmsLogin" :disabled="smsLoading">
          <span v-if="smsLoading" class="spinner"></span>
          <span v-else>登 录</span>
        </button>
      </div>

      <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
    </div>
  </div>

  <!-- 邮箱登录 -->
  <div v-if="showEmail" class="login-modal-overlay" @click.self="closeEmail">
    <div class="login-card">
      <button class="close-btn" @click="closeEmail">✕</button>
      <button class="back-btn" @click="showEmail = false">← 返回</button>

      <div class="login-header">
        <h2>邮箱登录</h2>
      </div>

      <div class="login-form">
        <div class="input-group">
          <input
              type="email"
              placeholder="邮箱地址"
              class="modern-input"
              v-model="emailForm.email"
          />
        </div>

        <div class="input-group">
          <input
              :type="showEmailPassword ? 'text' : 'password'"
              placeholder="密码"
              class="modern-input"
              v-model="emailForm.password"
          />
          <span class="eye-icon" @click="showEmailPassword = !showEmailPassword">
            {{ showEmailPassword ? '🙈' : '👁️' }}
          </span>
        </div>

        <div class="options-row">
          <label class="checkbox-label">
            <input type="checkbox" v-model="rememberEmail" />
            <span>记住账号</span>
          </label>
        </div>

        <button class="login-btn" @click="handleEmailLogin" :disabled="emailLoading">
          <span v-if="emailLoading" class="spinner"></span>
          <span v-else>登 录</span>
        </button>
      </div>

      <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import axios from 'axios'

const API_BASE = 'http://localhost:8080/api'

const emit = defineEmits(['close', 'switchToRegister', 'loginSuccess'])

// 弹窗显示
const showSms = ref(false)
const showEmail = ref(false)

// 表单数据
const loginForm = ref({ username: '', password: '' })
const smsForm = ref({ phone: '', code: '' })
const emailForm = ref({ email: '', password: '' })

// UI 状态
const showPassword = ref(false)
const showEmailPassword = ref(false)
const isLoading = ref(false)
const smsLoading = ref(false)
const emailLoading = ref(false)
const rememberMe = ref(false)
const rememberEmail = ref(false)
const errorMsg = ref('')

// 倒计时
const smsCountdown = ref(0)

// 计算属性
const isValidPhone = computed(() => /^1[3-9]\d{9}$/.test(smsForm.value.phone))

// 方法
const closeModals = () => {
  emit('close')
}

const closeSms = () => {
  showSms.value = false
  errorMsg.value = ''
}

const closeEmail = () => {
  showEmail.value = false
  errorMsg.value = ''
}

const switchToSmsLogin = () => {
  showSms.value = true
}

const switchToEmailLogin = () => {
  showEmail.value = true
}

const switchToRegister = () => {
  emit('switchToRegister')
}

const forgotPassword = () => alert('请联系管理员重置密码')

// 发送短信验证码
const sendSmsCode = async () => {
  if (!isValidPhone.value) {
    errorMsg.value = '请输入正确的手机号'
    setTimeout(() => { errorMsg.value = '' }, 2000)
    return
  }

  try {
    await axios.post(`${API_BASE}/user/send-sms-code`, null, { params: { phone: smsForm.value.phone } })
    errorMsg.value = '验证码已发送'
    setTimeout(() => { errorMsg.value = '' }, 2000)

    smsCountdown.value = 60
    const timer = setInterval(() => {
      if (smsCountdown.value > 0) {
        smsCountdown.value--
      } else {
        clearInterval(timer)
      }
    }, 1000)
  } catch (e) {
    errorMsg.value = '发送失败，请稍后重试'
    setTimeout(() => { errorMsg.value = '' }, 2000)
  }
}

// 密码登录
const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    errorMsg.value = '请输入账号和密码'
    setTimeout(() => { errorMsg.value = '' }, 2000)
    return
  }
  isLoading.value = true
  try {
    const res = await axios.post(`${API_BASE}/user/login`, null, {
      params: { username: loginForm.value.username, password: loginForm.value.password }
    })
    if (res.data.success) {
      if (rememberMe.value) {
        localStorage.setItem('savedUsername', loginForm.value.username)
        localStorage.setItem('savedPassword', loginForm.value.password)
      } else {
        localStorage.removeItem('savedUsername')
        localStorage.removeItem('savedPassword')
      }

      emit('loginSuccess', {
        id: res.data.userId,
        username: res.data.username,
        role: res.data.role
      })
      closeModals()
    } else {
      errorMsg.value = res.data.message
      setTimeout(() => { errorMsg.value = '' }, 2000)
    }
  } catch (e) {
    errorMsg.value = '网络错误'
    setTimeout(() => { errorMsg.value = '' }, 2000)
  } finally {
    isLoading.value = false
  }
}

// 短信登录
const handleSmsLogin = async () => {
  if (!smsForm.value.phone || !smsForm.value.code) {
    errorMsg.value = '请输入手机号和验证码'
    setTimeout(() => { errorMsg.value = '' }, 2000)
    return
  }

  smsLoading.value = true
  try {
    const res = await axios.post(`${API_BASE}/user/sms-login`, null, {
      params: { phone: smsForm.value.phone, code: smsForm.value.code }
    })
    if (res.data.success) {
      emit('loginSuccess', {
        id: res.data.userId,
        username: res.data.username,
        role: res.data.role
      })
      closeSms()
    } else {
      errorMsg.value = res.data.message
      setTimeout(() => { errorMsg.value = '' }, 2000)
    }
  } catch (e) {
    errorMsg.value = '登录失败，请检查验证码'
    setTimeout(() => { errorMsg.value = '' }, 2000)
  } finally {
    smsLoading.value = false
  }
}

// 邮箱登录
const handleEmailLogin = async () => {
  if (!emailForm.value.email || !emailForm.value.password) {
    errorMsg.value = '请输入邮箱和密码'
    setTimeout(() => { errorMsg.value = '' }, 2000)
    return
  }

  emailLoading.value = true
  try {
    const res = await axios.post(`${API_BASE}/user/email-login`, null, {
      params: { email: emailForm.value.email, password: emailForm.value.password }
    })
    if (res.data.success) {
      if (rememberEmail.value) {
        localStorage.setItem('savedEmail', emailForm.value.email)
      } else {
        localStorage.removeItem('savedEmail')
      }

      emit('loginSuccess', {
        id: res.data.userId,
        username: res.data.username,
        role: res.data.role
      })
      closeEmail()
    } else {
      errorMsg.value = res.data.message
      setTimeout(() => { errorMsg.value = '' }, 2000)
    }
  } catch (e) {
    errorMsg.value = '登录失败'
    setTimeout(() => { errorMsg.value = '' }, 2000)
  } finally {
    emailLoading.value = false
  }
}

// 微信登录
const handleWechatLogin = () => {
  const wechatAppId = 'YOUR_WECHAT_APP_ID'
  const redirectUri = encodeURIComponent(`${window.location.origin}/wechat/callback`)
  window.location.href = `https://open.weixin.qq.com/connect/qrconnect?appid=${wechatAppId}&redirect_uri=${redirectUri}&response_type=code&scope=snsapi_login&state=STATE#wechat_redirect`
}

// 加载记住的账号
const loadSavedCredentials = () => {
  const savedUsername = localStorage.getItem('savedUsername')
  const savedPassword = localStorage.getItem('savedPassword')
  const savedEmail = localStorage.getItem('savedEmail')

  if (savedUsername && savedPassword) {
    loginForm.value.username = savedUsername
    loginForm.value.password = savedPassword
    rememberMe.value = true
  }

  if (savedEmail) {
    emailForm.value.email = savedEmail
    rememberEmail.value = true
  }
}

loadSavedCredentials()
</script>

<style scoped>
/* ✅ 强制去掉浏览器自动填充的背景色 */
input:-webkit-autofill,
input:-webkit-autofill:hover,
input:-webkit-autofill:focus,
input:-webkit-autofill:active {
  -webkit-background-clip: text;
  -webkit-text-fill-color: white;
  transition: background-color 5000s ease-in-out 0s;
  background-color: transparent !important;
  caret-color: white;
}

/* 兼容普通 input 背景 */
input {
  background-color: transparent;
}

/* 蒙层 - 右侧 */
.login-modal-overlay {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background: rgba(0, 0, 0, 0.25);
  display: flex;
  align-items: center;
  justify-content: flex-end;
  z-index: 2000;
}

/* 卡片 - 小圆角、微微透明、简约 */
.login-card {
  width: 380px;
  max-width: 85%;
  background: rgba(20, 20, 28, 0.65);
  backdrop-filter: blur(16px);
  border-radius: 20px;
  padding: 40px 32px;
  position: relative;
  color: #fff;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  animation: fadeIn 0.3s ease-out;
  margin-right: 20px;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 关闭按钮 */
.close-btn {
  position: absolute;
  top: 20px;
  right: 20px;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  font-size: 20px;
  cursor: pointer;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

/* 返回按钮 */
.back-btn {
  position: absolute;
  top: 20px;
  left: 20px;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 20px;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

/* 头部 */
.login-header {
  margin-bottom: 32px;
  text-align: left;
}

.login-header h2 {
  font-size: 28px;
  font-weight: 500;
  color: white;
  margin: 0;
  letter-spacing: -0.5px;
}

/* 表单 */
.input-group {
  position: relative;
  margin-bottom: 20px;
}

.modern-input {
  width: 100%;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  padding: 12px 16px;
  color: white;
  font-size: 15px;
  outline: none;
  transition: all 0.2s;
}

.modern-input:focus {
  border-color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
}

.modern-input::placeholder {
  color: rgba(255, 255, 255, 0.35);
  font-weight: 400;
}

.eye-icon {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: rgba(255, 255, 255, 0.4);
  font-size: 16px;
  transition: color 0.2s;
}

.eye-icon:hover {
  color: white;
}

/* 验证码组 */
.code-group {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.code-input {
  flex: 1;
}

.send-code-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  padding: 0 18px;
  font-size: 13px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  white-space: nowrap;
  transition: all 0.2s;
}

.send-code-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.15);
  color: white;
}

.send-code-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 选项行 */
.options-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  margin: 8px 0 28px 0;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.6);
}

.checkbox-label input {
  width: 14px;
  height: 14px;
  cursor: pointer;
  accent-color: white;
}

.forgot-link {
  color: rgba(255, 255, 255, 0.5);
  text-decoration: none;
  font-size: 13px;
  transition: color 0.2s;
}

.forgot-link:hover {
  color: white;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  padding: 12px;
  background: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  cursor: pointer;
  transition: all 0.2s;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 其他登录方式 */
.other-methods {
  margin-top: 28px;
}

.divider {
  position: relative;
  text-align: center;
  margin: 20px 0 16px 0;
}

.divider::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
}

.divider span {
  position: relative;
  background: transparent;
  padding: 0 12px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 1px;
}

/* 三个按钮一排 */
.method-row {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.method-btn {
  flex: 1;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 30px;
  padding: 10px 0;
  font-size: 13px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.2s;
  text-align: center;
}

.method-btn:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
}

/* 注册链接 */
.signup-text {
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 28px;
}

.signup-text a {
  color: white;
  text-decoration: none;
  font-weight: 500;
  margin-left: 4px;
}

.signup-text a:hover {
  text-decoration: underline;
}

/* 错误提示 */
.error-msg {
  margin-top: 16px;
  padding: 10px;
  background: rgba(229, 115, 115, 0.15);
  border-radius: 10px;
  text-align: center;
  font-size: 12px;
  color: #ff9a9a;
}

/* 加载动画 */
.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(0, 0, 0, 0.1);
  border-top-color: #1a1a2e;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 响应式 */
@media (max-width: 480px) {
  .login-card {
    width: calc(100% - 20px);
    margin-right: 10px;
    padding: 32px 24px;
  }

  .code-group {
    flex-direction: column;
  }

  .send-code-btn {
    padding: 10px;
  }

  .method-row {
    gap: 8px;
  }

  .method-btn {
    font-size: 11px;
    padding: 8px 0;
  }
}
</style>