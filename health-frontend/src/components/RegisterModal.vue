<template>
  <!-- 蒙层 - 右侧弹窗 -->
  <div class="register-modal-overlay" @click.self="closeModals">
    <div class="register-card">
      <button class="close-btn" @click="closeModals">✕</button>

      <div class="register-header">
        <h2>创建账号</h2>
      </div>

      <!-- 注册方式切换 -->
      <div class="register-tabs">
        <button
            :class="{active: registerType === 'normal'}"
            @click="registerType = 'normal'"
        >
          普通注册
        </button>
        <button
            :class="{active: registerType === 'phone'}"
            @click="registerType = 'phone'"
        >
          手机注册
        </button>
      </div>

      <!-- 普通注册 -->
      <div v-show="registerType === 'normal'" class="register-fields">
        <div class="input-group">
          <input
              v-model="registerForm.username"
              type="text"
              placeholder="用户名"
              class="modern-input"
          />
        </div>

        <div class="input-group">
          <input
              v-model="registerForm.email"
              type="email"
              placeholder="邮箱（选填）"
              class="modern-input"
          />
        </div>

        <div class="input-group">
          <input
              v-model="registerForm.password"
              :type="showRegPassword ? 'text' : 'password'"
              placeholder="密码"
              class="modern-input"
          />
          <span class="eye-icon" @click="showRegPassword = !showRegPassword">
            {{ showRegPassword ? '🙈' : '👁️' }}
          </span>
        </div>

        <div class="input-group">
          <input
              v-model="registerForm.confirmPassword"
              :type="showRegConfirm ? 'text' : 'password'"
              placeholder="确认密码"
              class="modern-input"
          />
          <span class="eye-icon" @click="showRegConfirm = !showRegConfirm">
            {{ showRegConfirm ? '🙈' : '👁️' }}
          </span>
        </div>
      </div>

      <!-- 手机注册 -->
      <div v-show="registerType === 'phone'" class="register-fields">
        <div class="input-group">
          <input
              v-model="registerForm.phone"
              type="tel"
              placeholder="手机号"
              class="modern-input"
          />
        </div>

        <div class="code-group">
          <input
              v-model="registerForm.smsCode"
              type="text"
              placeholder="验证码"
              class="modern-input code-input"
          />
          <button
              class="send-code-btn"
              @click="sendRegisterSmsCode"
              :disabled="registerCountdown > 0 || !isValidRegisterPhone"
          >
            {{ registerCountdown > 0 ? `${registerCountdown}s` : '获取验证码' }}
          </button>
        </div>

        <div class="input-group">
          <input
              v-model="registerForm.password"
              :type="showRegPassword ? 'text' : 'password'"
              placeholder="密码"
              class="modern-input"
          />
          <span class="eye-icon" @click="showRegPassword = !showRegPassword">
            {{ showRegPassword ? '🙈' : '👁️' }}
          </span>
        </div>

        <div class="input-group">
          <input
              v-model="registerForm.confirmPassword"
              :type="showRegConfirm ? 'text' : 'password'"
              placeholder="确认密码"
              class="modern-input"
          />
          <span class="eye-icon" @click="showRegConfirm = !showRegConfirm">
            {{ showRegConfirm ? '🙈' : '👁️' }}
          </span>
        </div>
      </div>

      <button class="register-submit-btn" @click="handleRegister">注 册</button>

      <!-- 其他注册方式 -->
      <div class="other-methods">
        <div class="divider">
          <span>其他注册方式</span>
        </div>

        <div class="method-row">
          <button class="method-btn" @click="handleWechatRegister">微信注册</button>
        </div>
      </div>

      <p class="login-text">
        已有账号？<a href="#" @click.prevent="switchToLogin">立即登录</a>
      </p>

      <div v-if="errorMsg" class="error-msg">{{ errorMsg }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import axios from 'axios'

const API_BASE = 'http://localhost:8080/api'

const emit = defineEmits(['close', 'switchToLogin', 'registerSuccess'])

const registerType = ref('normal')
const showRegPassword = ref(false)
const showRegConfirm = ref(false)
const registerCountdown = ref(0)
const errorMsg = ref('')

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  email: '',
  phone: '',
  smsCode: ''
})

const isValidRegisterPhone = computed(() => /^1[3-9]\d{9}$/.test(registerForm.value.phone))

const closeModals = () => {
  emit('close')
}

const switchToLogin = () => {
  emit('switchToLogin')
}

// 发送注册短信验证码
const sendRegisterSmsCode = async () => {
  if (!isValidRegisterPhone.value) {
    errorMsg.value = '请输入正确的手机号'
    setTimeout(() => { errorMsg.value = '' }, 2000)
    return
  }

  try {
    await axios.post(`${API_BASE}/user/send-register-sms`, null, { params: { phone: registerForm.value.phone } })
    errorMsg.value = '验证码已发送'
    setTimeout(() => { errorMsg.value = '' }, 2000)

    registerCountdown.value = 60
    const timer = setInterval(() => {
      if (registerCountdown.value > 0) {
        registerCountdown.value--
      } else {
        clearInterval(timer)
      }
    }, 1000)
  } catch (e) {
    errorMsg.value = '发送失败，请稍后重试'
    setTimeout(() => { errorMsg.value = '' }, 2000)
  }
}

// 注册
const handleRegister = async () => {
  if (registerType.value === 'normal') {
    if (!registerForm.value.username || !registerForm.value.password) {
      errorMsg.value = '请填写用户名和密码'
      setTimeout(() => { errorMsg.value = '' }, 2000)
      return
    }
    if (registerForm.value.password !== registerForm.value.confirmPassword) {
      errorMsg.value = '两次密码不一致'
      setTimeout(() => { errorMsg.value = '' }, 2000)
      return
    }
    try {
      const res = await axios.post(`${API_BASE}/user/register`, null, {
        params: {
          username: registerForm.value.username,
          password: registerForm.value.password,
          email: registerForm.value.email || null
        }
      })
      if (res.data.success) {
        errorMsg.value = '注册成功，请登录'
        setTimeout(() => { errorMsg.value = '' }, 2000)
        setTimeout(() => {
          emit('registerSuccess', registerForm.value.username)
          closeModals()
        }, 1500)
      } else {
        errorMsg.value = res.data.message
        setTimeout(() => { errorMsg.value = '' }, 2000)
      }
    } catch (e) {
      errorMsg.value = '网络错误'
      setTimeout(() => { errorMsg.value = '' }, 2000)
    }
  } else if (registerType.value === 'phone') {
    if (!registerForm.value.phone || !registerForm.value.smsCode || !registerForm.value.password) {
      errorMsg.value = '请填写手机号、验证码和密码'
      setTimeout(() => { errorMsg.value = '' }, 2000)
      return
    }
    if (registerForm.value.password !== registerForm.value.confirmPassword) {
      errorMsg.value = '两次密码不一致'
      setTimeout(() => { errorMsg.value = '' }, 2000)
      return
    }
    try {
      const res = await axios.post(`${API_BASE}/user/phone-register`, null, {
        params: {
          phone: registerForm.value.phone,
          code: registerForm.value.smsCode,
          password: registerForm.value.password
        }
      })
      if (res.data.success) {
        errorMsg.value = '注册成功，请登录'
        setTimeout(() => { errorMsg.value = '' }, 2000)
        setTimeout(() => {
          emit('registerSuccess', registerForm.value.phone)
          closeModals()
        }, 1500)
      } else {
        errorMsg.value = res.data.message
        setTimeout(() => { errorMsg.value = '' }, 2000)
      }
    } catch (e) {
      errorMsg.value = '注册失败'
      setTimeout(() => { errorMsg.value = '' }, 2000)
    }
  }
}

// 微信注册
const handleWechatRegister = () => {
  const wechatAppId = 'YOUR_WECHAT_APP_ID'
  const redirectUri = encodeURIComponent(`${window.location.origin}/wechat/callback`)
  window.location.href = `https://open.weixin.qq.com/connect/qrconnect?appid=${wechatAppId}&redirect_uri=${redirectUri}&response_type=code&scope=snsapi_login&state=STATE#wechat_redirect`
}
</script>

<style scoped>
/* 蒙层 - 右侧 */
.register-modal-overlay {
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

/* 卡片 */
.register-card {
  width: 380px;
  max-width: 85%;
  background: rgba(20, 20, 28, 0.65);
  backdrop-filter: blur(16px);
  border-radius: 20px;
  padding: 36px 32px;
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
  top: 18px;
  right: 18px;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.5);
  font-size: 18px;
  cursor: pointer;
  width: 28px;
  height: 28px;
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

/* 头部 */
.register-header {
  margin-bottom: 24px;
  text-align: left;
}

.register-header h2 {
  font-size: 26px;
  font-weight: 500;
  color: white;
  margin: 0;
  letter-spacing: -0.5px;
}

/* 注册方式切换 */
.register-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  padding-bottom: 10px;
}

.register-tabs button {
  flex: 1;
  background: transparent;
  border: none;
  padding: 6px 0;
  font-size: 14px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.5);
  transition: all 0.2s;
  border-radius: 0;
}

.register-tabs button.active {
  color: white;
  border-bottom: 2px solid white;
}

/* 表单区域 */
.register-fields {
  width: 100%;
}

/* 输入框 - 统一半透明样式 */
.input-group {
  position: relative;
  margin-bottom: 16px;
}

.modern-input {
  width: 100%;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  padding: 11px 16px;
  color: white;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.modern-input:focus {
  border-color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
}

.modern-input::placeholder {
  color: rgba(255, 255, 255, 0.35);
  font-weight: 400;
}

/* 确保所有输入框样式一致 */
.modern-input:-webkit-autofill,
.modern-input:-webkit-autofill:hover,
.modern-input:-webkit-autofill:focus,
.modern-input:-webkit-autofill:active {
  -webkit-background-clip: text;
  -webkit-text-fill-color: white;
  transition: background-color 5000s ease-in-out 0s;
}

.eye-icon {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  cursor: pointer;
  color: rgba(255, 255, 255, 0.4);
  font-size: 15px;
  transition: color 0.2s;
  z-index: 1;
}

.eye-icon:hover {
  color: white;
}

/* 验证码组 */
.code-group {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.code-input {
  flex: 1;
}

.send-code-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 12px;
  padding: 0 16px;
  font-size: 12px;
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

/* 注册按钮 */
.register-submit-btn {
  width: 100%;
  padding: 11px;
  background: white;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 8px;
}

.register-submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
}

/* 其他注册方式 */
.other-methods {
  margin-top: 24px;
}

.divider {
  position: relative;
  text-align: center;
  margin: 16px 0 12px 0;
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
  padding: 8px 0;
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

/* 登录链接 */
.login-text {
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 24px;
}

.login-text a {
  color: white;
  text-decoration: none;
  font-weight: 500;
  margin-left: 4px;
}

.login-text a:hover {
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

/* 响应式 */
@media (max-width: 480px) {
  .register-card {
    width: calc(100% - 20px);
    margin-right: 10px;
    padding: 28px 24px;
  }

  .code-group {
    flex-direction: column;
  }

  .send-code-btn {
    padding: 10px;
  }
}
</style>