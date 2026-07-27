<template>
  <div class="profile-container">
    <!-- 头部信息区域 -->
    <div class="glass-card profile-header">
      <div class="avatar-section">
        <div v-if="isSelf" class="avatar-edit">
          <AvatarSelector
              :userId="userId"
              :currentAvatarUrl="userInfo.avatar"
              @avatar-updated="handleAvatarUpdated"
          />
        </div>
        <div v-else class="avatar-view">
          <img
              v-if="userInfo.avatar"
              :src="getFullImageUrl(userInfo.avatar)"
              class="avatar-img-large"
              @error="handleAvatarError"
          />
          <span v-else class="avatar-placeholder-large">👤</span>
        </div>

        <h2 class="username">{{ userInfo.nickname || userInfo.username }}</h2>
        <p class="user-badge">{{ userInfo.role === 'ADMIN' ? '管理员' : '健康达人' }}</p>

        <button
            v-if="!isSelf"
            @click="toggleFollow"
            class="follow-btn"
            :class="{ following: isFollowing }"
        >
          {{ isFollowing ? '✓ 已关注' : '+ 关注' }}
        </button>
      </div>
    </div>

    <!-- 统计数据卡片 -->
    <div class="glass-card stats-card">
      <div class="stats-grid">
        <div class="stat-item clickable" @click="openPostsModal">
          <span class="stat-icon">📝</span>
          <div class="stat-info">
            <span class="stat-value">{{ userInfo.postCount || 0 }}</span>
            <span class="stat-label">动态</span>
          </div>
        </div>
        <div class="stat-item clickable" @click="openFollowersModal">
          <span class="stat-icon">👥</span>
          <div class="stat-info">
            <span class="stat-value">{{ followerCount }}</span>
            <span class="stat-label">粉丝</span>
          </div>
        </div>
        <div class="stat-item clickable" @click="openFollowingModal">
          <span class="stat-icon">❤️</span>
          <div class="stat-info">
            <span class="stat-value">{{ followingCount }}</span>
            <span class="stat-label">关注</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 基本信息卡片 -->
    <div class="glass-card">
      <div class="card-header">
        <span class="card-icon">📝</span>
        <h4>基本信息</h4>
      </div>
      <div class="form-group">
        <label>昵称</label>
        <input type="text" v-model="profileForm.nickname" placeholder="请输入昵称" :disabled="!isSelf" />
      </div>
      <div class="form-row">
        <div class="form-group">
          <label>性别</label>
          <select v-model="profileForm.gender" :disabled="!isSelf">
            <option value="">保密</option>
            <option value="0">女</option>
            <option value="1">男</option>
          </select>
        </div>
        <div class="form-group">
          <label>年龄</label>
          <input type="number" v-model="profileForm.age" placeholder="年龄" :disabled="!isSelf" />
        </div>
      </div>
      <div class="form-group" v-if="userInfo.registerTime">
        <label>注册时间</label>
        <input type="text" :value="formatDate(userInfo.registerTime)" disabled class="readonly-input" />
      </div>

      <button v-if="isSelf" @click="openPasswordModal" class="password-btn">修改密码</button>
      <button v-if="isSelf" @click="updateProfile" class="save-btn">保存修改</button>
    </div>

    <!-- 安全设置卡片（新增） -->
    <div v-if="isSelf" class="glass-card">
      <div class="card-header">
        <span class="card-icon">🔐</span>
        <h4>安全设置</h4>
      </div>

      <!-- 手机号绑定 -->
      <div class="bind-item">
        <div class="bind-left">
          <span class="bind-icon">📱</span>
          <div class="bind-info">
            <div class="bind-label">手机号</div>
            <div class="bind-status" :class="{ bound: bindInfo.hasPhone }">
              {{ bindInfo.hasPhone ? bindInfo.maskedPhone : '未绑定' }}
            </div>
          </div>
        </div>
        <button
            v-if="!bindInfo.hasPhone"
            class="bind-btn"
            @click="openBindPhoneModal"
        >
          去绑定
        </button>
        <button
            v-else
            class="unbind-btn"
            @click="openUnbindPhoneModal"
        >
          解绑
        </button>
      </div>

      <!-- 邮箱绑定 -->
      <div class="bind-item">
        <div class="bind-left">
          <span class="bind-icon">📧</span>
          <div class="bind-info">
            <div class="bind-label">邮箱</div>
            <div class="bind-status" :class="{ bound: bindInfo.hasEmail }">
              {{ bindInfo.hasEmail ? bindInfo.maskedEmail : '未绑定' }}
            </div>
          </div>
        </div>
        <button
            v-if="!bindInfo.hasEmail"
            class="bind-btn"
            @click="openBindEmailModal"
        >
          去绑定
        </button>
        <button
            v-else
            class="unbind-btn"
            @click="openUnbindEmailModal"
        >
          解绑
        </button>
      </div>

      <!-- 微信绑定 -->
      <div class="bind-item">
        <div class="bind-left">
          <span class="bind-icon">💬</span>
          <div class="bind-info">
            <div class="bind-label">微信</div>
            <div class="bind-status" :class="{ bound: bindInfo.hasWechat }">
              {{ bindInfo.hasWechat ? '已绑定' : '未绑定' }}
            </div>
          </div>
        </div>
        <button
            v-if="!bindInfo.hasWechat"
            class="bind-btn wechat-bind"
            @click="handleWechatBind"
        >
          去绑定
        </button>
        <button
            v-else
            class="unbind-btn"
            @click="openUnbindWechatModal"
        >
          解绑
        </button>
      </div>
    </div>

    <!-- 我的收藏卡片 -->
    <div v-if="isSelf" class="glass-card">
      <div class="card-header">
        <span class="card-icon">⭐</span>
        <h4>我的收藏</h4>
      </div>
      <div class="collection-item" @click="goToFavorites">
        <div class="collection-left">
          <span class="collection-icon">📚</span>
          <div class="collection-info">
            <div class="collection-label">收藏的文章</div>
            <div class="collection-desc">查看您收藏过的健康资讯</div>
          </div>
        </div>
        <span class="arrow-icon">→</span>
      </div>
    </div>

    <!-- 退出登录 -->
    <div class="glass-card logout-card" v-if="isSelf">
      <button @click="logout" class="logout-btn">退出登录</button>
    </div>

    <!-- 消息提示 -->
    <div v-if="message" :class="['message', messageType]">
      {{ message }}
    </div>
  </div>

  <!-- 使用 Teleport 将弹窗传送到 body 下 -->
  <Teleport to="body">
    <!-- 动态列表弹窗 -->
    <div v-if="showPostsModal" class="modal-overlay" @click="showPostsModal = false">
      <div class="modal-container-large" @click.stop>
        <div class="modal-header-white">
          <h3>我的动态 <span class="post-count-tag">{{ userPosts.length }}条</span></h3>
          <button class="close-btn-white" @click="showPostsModal = false">×</button>
        </div>
        <div class="modal-body-posts">
          <div v-if="loadingPosts" class="loading-state">
            <div class="loading-spinner"></div>
            <p>加载中...</p>
          </div>
          <div v-else-if="userPosts.length === 0" class="empty-tip-white">
            <div class="empty-icon">📭</div>
            <p>暂无动态</p>
            <p class="empty-hint">去社区发布你的第一条动态吧~</p>
          </div>
          <div v-else class="posts-list-modal">
            <div v-for="post in sortedPosts" :key="post.id" class="post-card-modal" @click="goToPost(post.id)">
              <div class="post-content-modal">{{ post.content || '📷 [图片]' }}</div>
              <div class="post-meta-modal">
                <span class="post-time-modal">🕐 {{ formatTime(post.createTime) }}</span>
                <div class="post-stats-modal">
                  <span>❤️ {{ post.likeCount || 0 }}</span>
                  <span>💬 {{ post.commentCount || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 粉丝列表弹窗 -->
    <div v-if="showFollowersModal" class="modal-overlay" @click="showFollowersModal = false">
      <div class="modal-container-white" @click.stop>
        <div class="modal-header-white">
          <h3>粉丝列表</h3>
          <button class="close-btn-white" @click="showFollowersModal = false">×</button>
        </div>
        <div class="modal-body-white">
          <div v-for="follower in followersList" :key="follower.id" class="user-item-white" @click="goToUserProfile(follower.userId)">
            <div class="user-avatar-white">
              <img :src="getFullImageUrl(follower.avatar)" @error="handleAvatarError" />
            </div>
            <div class="user-name-white">{{ follower.nickname || follower.username }}</div>
          </div>
          <div v-if="followersList.length === 0" class="empty-tip-white">暂无粉丝</div>
        </div>
      </div>
    </div>

    <!-- 关注列表弹窗 -->
    <div v-if="showFollowingModal" class="modal-overlay" @click="showFollowingModal = false">
      <div class="modal-container-white" @click.stop>
        <div class="modal-header-white">
          <h3>关注列表</h3>
          <button class="close-btn-white" @click="showFollowingModal = false">×</button>
        </div>
        <div class="modal-body-white">
          <div v-for="following in followingList" :key="following.id" class="user-item-white" @click="goToUserProfile(following.userId)">
            <div class="user-avatar-white">
              <img :src="getFullImageUrl(following.avatar)" @error="handleAvatarError" />
            </div>
            <div class="user-name-white">{{ following.nickname || following.username }}</div>
          </div>
          <div v-if="followingList.length === 0" class="empty-tip-white">暂无关注</div>
        </div>
      </div>
    </div>

    <!-- 修改密码弹窗 -->
    <div v-if="showPasswordModal" class="modal-overlay" @click="showPasswordModal = false">
      <div class="password-modal-container" @click.stop>
        <div class="password-modal-header">
          <h3>修改密码</h3>
          <button class="password-modal-close" @click="closePasswordModal">×</button>
        </div>
        <div class="password-modal-body">
          <div class="password-form-group">
            <label>原密码</label>
            <input type="password" v-model="passwordForm.oldPassword" placeholder="请输入原密码" />
          </div>
          <div class="password-form-group">
            <label>新密码</label>
            <input type="password" v-model="passwordForm.newPassword" placeholder="请输入新密码（至少6位）" />
          </div>
          <div class="password-form-group">
            <label>确认新密码</label>
            <input type="password" v-model="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
          </div>
        </div>
        <div class="password-modal-footer">
          <button class="password-cancel-btn" @click="closePasswordModal">取消</button>
          <button class="password-confirm-btn" @click="handleChangePassword">确认修改</button>
        </div>
      </div>
    </div>

    <!-- ========== 绑定手机号弹窗 ========== -->
    <div v-if="showBindPhoneModal" class="modal-overlay" @click="showBindPhoneModal = false">
      <div class="bind-modal-container" @click.stop>
        <div class="bind-modal-header">
          <h3>绑定手机号</h3>
          <button class="bind-modal-close" @click="closeBindPhoneModal">×</button>
        </div>
        <div class="bind-modal-body">
          <div class="bind-form-group">
            <label>手机号</label>
            <input type="tel" v-model="bindPhoneForm.phone" placeholder="请输入手机号" />
          </div>
          <div class="bind-code-group">
            <input type="text" v-model="bindPhoneForm.code" placeholder="验证码" class="code-input" />
            <button class="send-code-btn" @click="sendBindSmsCode" :disabled="bindSmsCountdown > 0 || !isValidBindPhone">
              {{ bindSmsCountdown > 0 ? `${bindSmsCountdown}s` : '获取验证码' }}
            </button>
          </div>
        </div>
        <div class="bind-modal-footer">
          <button class="bind-cancel-btn" @click="closeBindPhoneModal">取消</button>
          <button class="bind-confirm-btn" @click="handleBindPhone" :disabled="bindPhoneLoading">
            {{ bindPhoneLoading ? '绑定中...' : '确认绑定' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ========== 绑定邮箱弹窗 ========== -->
    <div v-if="showBindEmailModal" class="modal-overlay" @click="showBindEmailModal = false">
      <div class="bind-modal-container" @click.stop>
        <div class="bind-modal-header">
          <h3>绑定邮箱</h3>
          <button class="bind-modal-close" @click="closeBindEmailModal">×</button>
        </div>
        <div class="bind-modal-body">
          <div class="bind-form-group">
            <label>邮箱地址</label>
            <input type="email" v-model="bindEmailForm.email" placeholder="请输入邮箱地址" />
          </div>
          <div class="bind-code-group">
            <input type="text" v-model="bindEmailForm.code" placeholder="验证码" class="code-input" />
            <button class="send-code-btn" @click="sendBindEmailCode" :disabled="bindEmailCountdown > 0 || !isValidBindEmail">
              {{ bindEmailCountdown > 0 ? `${bindEmailCountdown}s` : '获取验证码' }}
            </button>
          </div>
        </div>
        <div class="bind-modal-footer">
          <button class="bind-cancel-btn" @click="closeBindEmailModal">取消</button>
          <button class="bind-confirm-btn" @click="handleBindEmail" :disabled="bindEmailLoading">
            {{ bindEmailLoading ? '绑定中...' : '确认绑定' }}
          </button>
        </div>
      </div>
    </div>

    <!-- ========== 解绑确认弹窗 ========== -->
    <div v-if="showUnbindConfirm" class="modal-overlay" @click="showUnbindConfirm = false">
      <div class="confirm-modal-container" @click.stop>
        <div class="confirm-modal-header">
          <h3>确认解绑</h3>
        </div>
        <div class="confirm-modal-body">
          <p>确定要解绑{{ unbindTarget === 'phone' ? '手机号' : unbindTarget === 'email' ? '邮箱' : '微信' }}吗？</p>
          <p class="confirm-hint">解绑后，您将无法使用该方式登录。</p>
        </div>
        <div class="confirm-modal-footer">
          <button class="confirm-cancel-btn" @click="closeUnbindModal">取消</button>
          <button class="confirm-confirm-btn" @click="handleUnbind">确认解绑</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import AvatarSelector from './AvatarSelector.vue'

const props = defineProps({
  userId: {
    type: Number,
    required: true
  }
})

const router = useRouter()
const API_BASE = 'http://localhost:8080/api'

// 获取当前登录用户
const currentUser = computed(() => {
  const saved = localStorage.getItem('userInfo')
  return saved ? JSON.parse(saved) : null
})

// 判断是否是自己的主页
const isSelf = computed(() => {
  return currentUser.value?.id === props.userId
})

// 用户信息
const userInfo = ref({
  avatar: '',
  username: '',
  nickname: '',
  gender: '',
  age: '',
  role: '',
  registerTime: '',
  postCount: 0
})

const profileForm = reactive({
  nickname: '',
  gender: '',
  age: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 绑定信息
const bindInfo = ref({
  hasPhone: false,
  maskedPhone: '',
  hasEmail: false,
  maskedEmail: '',
  hasWechat: false,
  maskedWechat: ''
})

// 手机号绑定弹窗
const showBindPhoneModal = ref(false)
const bindPhoneForm = reactive({ phone: '', code: '' })
const bindSmsCountdown = ref(0)
const bindPhoneLoading = ref(false)
let bindSmsTimer = null

// 邮箱绑定弹窗
const showBindEmailModal = ref(false)
const bindEmailForm = reactive({ email: '', code: '' })
const bindEmailCountdown = ref(0)
const bindEmailLoading = ref(false)
let bindEmailTimer = null

// 解绑确认弹窗
const showUnbindConfirm = ref(false)
const unbindTarget = ref('') // 'phone', 'email', 'wechat'

// 关注相关
const isFollowing = ref(false)
const followerCount = ref(0)
const followingCount = ref(0)
const followersList = ref([])
const followingList = ref([])
const showFollowersModal = ref(false)
const showFollowingModal = ref(false)

// 动态弹窗
const showPostsModal = ref(false)

// 修改密码弹窗
const showPasswordModal = ref(false)

// 用户帖子
const userPosts = ref([])
const loadingPosts = ref(false)

const message = ref('')
const messageType = ref('success')

// 计算属性
const isValidBindPhone = computed(() => /^1[3-9]\d{9}$/.test(bindPhoneForm.phone))
const isValidBindEmail = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(bindEmailForm.email))

// 按时间倒序排序
const sortedPosts = computed(() => {
  return [...userPosts.value].sort((a, b) => {
    return new Date(b.createTime) - new Date(a.createTime)
  })
})

const showMessage = (msg, type = 'success') => {
  message.value = msg
  messageType.value = type
  setTimeout(() => {
    message.value = ''
  }, 3000)
}

const getFullImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/uploads')) return `http://localhost:8080${url}`
  return url
}

const handleAvatarError = (e) => {
  e.target.style.display = 'none'
  const parent = e.target.parentElement
  if (parent && !parent.querySelector('span')) {
    const span = document.createElement('span')
    span.textContent = '👤'
    parent.appendChild(span)
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / (60 * 1000))}分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / (60 * 60 * 1000))}小时前`
  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 加载绑定信息
const loadBindInfo = async () => {
  try {
    const res = await axios.get(`${API_BASE}/user/bind-info/${props.userId}`)
    if (res.data.success) {
      bindInfo.value = {
        hasPhone: res.data.hasPhone,
        maskedPhone: res.data.maskedPhone || '',
        hasEmail: res.data.hasEmail,
        maskedEmail: res.data.maskedEmail || '',
        hasWechat: res.data.hasWechat,
        maskedWechat: res.data.maskedWechat || ''
      }
    }
  } catch (e) {
    console.error('加载绑定信息失败', e)
  }
}

// 发送绑定手机验证码
const sendBindSmsCode = async () => {
  if (!isValidBindPhone.value) {
    showMessage('请输入正确的手机号', 'error')
    return
  }

  try {
    await axios.post(`${API_BASE}/user/send-bind-sms`, null, {
      params: { phone: bindPhoneForm.phone }
    })
    showMessage('验证码已发送（测试验证码：123456）')

    bindSmsCountdown.value = 60
    if (bindSmsTimer) clearInterval(bindSmsTimer)
    bindSmsTimer = setInterval(() => {
      if (bindSmsCountdown.value > 0) {
        bindSmsCountdown.value--
      } else {
        clearInterval(bindSmsTimer)
      }
    }, 1000)
  } catch (e) {
    showMessage('发送失败，请重试', 'error')
  }
}

// 绑定手机号
const handleBindPhone = async () => {
  if (!bindPhoneForm.phone) {
    showMessage('请输入手机号', 'error')
    return
  }
  if (!bindPhoneForm.code) {
    showMessage('请输入验证码', 'error')
    return
  }

  bindPhoneLoading.value = true
  try {
    const res = await axios.post(`${API_BASE}/user/bind-phone`, null, {
      params: {
        userId: props.userId,
        phone: bindPhoneForm.phone,
        code: bindPhoneForm.code
      }
    })
    if (res.data.success) {
      showMessage('手机号绑定成功')
      closeBindPhoneModal()
      await loadBindInfo()
      await loadUserProfile()
    } else {
      showMessage(res.data.message, 'error')
    }
  } catch (e) {
    showMessage('绑定失败，请重试', 'error')
  } finally {
    bindPhoneLoading.value = false
  }
}

const closeBindPhoneModal = () => {
  showBindPhoneModal.value = false
  bindPhoneForm.phone = ''
  bindPhoneForm.code = ''
  if (bindSmsTimer) clearInterval(bindSmsTimer)
  bindSmsCountdown.value = 0
}

const openBindPhoneModal = () => {
  bindPhoneForm.phone = ''
  bindPhoneForm.code = ''
  showBindPhoneModal.value = true
}

// 发送绑定邮箱验证码
const sendBindEmailCode = async () => {
  if (!isValidBindEmail.value) {
    showMessage('请输入正确的邮箱地址', 'error')
    return
  }

  try {
    await axios.post(`${API_BASE}/user/send-bind-email-code`, null, {
      params: { email: bindEmailForm.email }
    })
    showMessage('验证码已发送（测试验证码：123456）')

    bindEmailCountdown.value = 60
    if (bindEmailTimer) clearInterval(bindEmailTimer)
    bindEmailTimer = setInterval(() => {
      if (bindEmailCountdown.value > 0) {
        bindEmailCountdown.value--
      } else {
        clearInterval(bindEmailTimer)
      }
    }, 1000)
  } catch (e) {
    showMessage('发送失败，请重试', 'error')
  }
}

// 绑定邮箱
const handleBindEmail = async () => {
  if (!bindEmailForm.email) {
    showMessage('请输入邮箱地址', 'error')
    return
  }
  if (!bindEmailForm.code) {
    showMessage('请输入验证码', 'error')
    return
  }

  bindEmailLoading.value = true
  try {
    const res = await axios.post(`${API_BASE}/user/bind-email`, null, {
      params: {
        userId: props.userId,
        email: bindEmailForm.email,
        code: bindEmailForm.code
      }
    })
    if (res.data.success) {
      showMessage('邮箱绑定成功')
      closeBindEmailModal()
      await loadBindInfo()
      await loadUserProfile()
    } else {
      showMessage(res.data.message, 'error')
    }
  } catch (e) {
    showMessage('绑定失败，请重试', 'error')
  } finally {
    bindEmailLoading.value = false
  }
}

const closeBindEmailModal = () => {
  showBindEmailModal.value = false
  bindEmailForm.email = ''
  bindEmailForm.code = ''
  if (bindEmailTimer) clearInterval(bindEmailTimer)
  bindEmailCountdown.value = 0
}

const openBindEmailModal = () => {
  bindEmailForm.email = ''
  bindEmailForm.code = ''
  showBindEmailModal.value = true
}

// 微信绑定（暂未开放）
const handleWechatBind = () => {
  showMessage('微信绑定功能即将开放，敬请期待', 'error')
}

// 解绑相关
const openUnbindPhoneModal = () => {
  unbindTarget.value = 'phone'
  showUnbindConfirm.value = true
}

const openUnbindEmailModal = () => {
  unbindTarget.value = 'email'
  showUnbindConfirm.value = true
}

const openUnbindWechatModal = () => {
  showMessage('微信解绑请联系客服', 'error')
}

const closeUnbindModal = () => {
  showUnbindConfirm.value = false
  unbindTarget.value = ''
}

const handleUnbind = async () => {
  try {
    let res
    if (unbindTarget.value === 'phone') {
      res = await axios.post(`${API_BASE}/user/unbind-phone`, null, {
        params: { userId: props.userId }
      })
    } else if (unbindTarget.value === 'email') {
      res = await axios.post(`${API_BASE}/user/unbind-email`, null, {
        params: { userId: props.userId }
      })
    } else {
      return
    }

    if (res.data.success) {
      showMessage(res.data.message)
      closeUnbindModal()
      await loadBindInfo()
      await loadUserProfile()
    } else {
      showMessage(res.data.message, 'error')
    }
  } catch (e) {
    showMessage('操作失败，请重试', 'error')
  }
}

// 加载用户信息
const loadUserProfile = async () => {
  try {
    const res = await axios.get(`${API_BASE}/user/profile/${props.userId}`)
    if (res.data.success) {
      userInfo.value = {
        ...res.data.user,
        registerTime: res.data.user.createTime || '',
        postCount: res.data.postCount || 0
      }
      followerCount.value = res.data.followerCount || 0
      followingCount.value = res.data.followingCount || 0
      profileForm.nickname = res.data.user.nickname || ''
      profileForm.gender = res.data.user.gender || ''
      profileForm.age = res.data.user.age || ''
    }
  } catch (e) {
    console.error('加载用户信息失败', e)
  }
}

// 加载用户帖子
const loadUserPosts = async () => {
  loadingPosts.value = true
  try {
    const res = await axios.get(`${API_BASE}/community/user-posts/${props.userId}`)
    if (Array.isArray(res.data)) {
      userPosts.value = res.data
    } else if (res.data.data && Array.isArray(res.data.data)) {
      userPosts.value = res.data.data
    } else {
      userPosts.value = []
    }
  } catch (e) {
    console.error('加载用户帖子失败', e)
    userPosts.value = []
  } finally {
    loadingPosts.value = false
  }
}

// 加载关注状态
const loadFollowStatus = async () => {
  if (isSelf.value) return

  try {
    const res = await axios.get(`${API_BASE}/follow/status`, {
      params: { followerId: currentUser.value?.id, followedId: props.userId }
    })
    isFollowing.value = res.data.isFollowing
  } catch (e) {
    console.error('加载关注状态失败', e)
  }
}

// 关注/取消关注
const toggleFollow = async () => {
  try {
    if (isFollowing.value) {
      await axios.delete(`${API_BASE}/follow/unfollow`, {
        params: { followerId: currentUser.value?.id, followedId: props.userId }
      })
      isFollowing.value = false
      followerCount.value--
      showMessage('已取消关注')
    } else {
      await axios.post(`${API_BASE}/follow/follow`, null, {
        params: { followerId: currentUser.value?.id, followedId: props.userId }
      })
      isFollowing.value = true
      followerCount.value++
      showMessage('关注成功')
    }
  } catch (e) {
    showMessage('操作失败', 'error')
  }
}

// 打开动态列表弹窗
const openPostsModal = async () => {
  if (userPosts.value.length === 0) {
    await loadUserPosts()
  }
  showPostsModal.value = true
}

// 打开粉丝列表
const openFollowersModal = async () => {
  try {
    const res = await axios.get(`${API_BASE}/follow/followers/${props.userId}`)
    followersList.value = res.data || []
    showFollowersModal.value = true
  } catch (e) {
    console.error('加载粉丝列表失败', e)
  }
}

// 打开关注列表
const openFollowingModal = async () => {
  try {
    const res = await axios.get(`${API_BASE}/follow/following/${props.userId}`)
    followingList.value = res.data || []
    showFollowingModal.value = true
  } catch (e) {
    console.error('加载关注列表失败', e)
  }
}

// 打开修改密码弹窗
const openPasswordModal = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  showPasswordModal.value = true
}

// 关闭修改密码弹窗
const closePasswordModal = () => {
  showPasswordModal.value = false
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordForm.oldPassword) {
    showMessage('请输入原密码', 'error')
    return
  }
  if (!passwordForm.newPassword) {
    showMessage('请输入新密码', 'error')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    showMessage('新密码长度至少6位', 'error')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    showMessage('两次输入的新密码不一致', 'error')
    return
  }

  try {
    const res = await axios.post(`${API_BASE}/user/changePassword`, null, {
      params: {
        userId: props.userId,
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      }
    })
    if (res.data.success) {
      showMessage('密码修改成功，请重新登录', 'success')
      closePasswordModal()
      setTimeout(() => {
        localStorage.removeItem('savedUsername')
        localStorage.removeItem('savedPassword')
        window.location.reload()
      }, 2000)
    } else {
      showMessage(res.data.message, 'error')
    }
  } catch (e) {
    showMessage('修改失败，请重试', 'error')
  }
}

// 跳转用户主页
const goToUserProfile = (targetUserId) => {
  if (targetUserId === props.userId) return
  showFollowersModal.value = false
  showFollowingModal.value = false
  router.push(`/profile/${targetUserId}`)
}

// 跳转帖子详情
const goToPost = (postId) => {
  if (!postId) {
    showMessage('无法跳转：帖子ID无效', 'error')
    return
  }
  showPostsModal.value = false
  setTimeout(() => {
    router.push({ path: '/community', query: { postId: postId } })
  }, 50)
}

// 跳转到收藏页面
const goToFavorites = () => {
  router.push(`/favorites/${props.userId}`)
}

// 更新个人信息
const updateProfile = async () => {
  try {
    const res = await axios.post(`${API_BASE}/user/updateProfile`, null, {
      params: {
        userId: props.userId,
        nickname: profileForm.nickname || null,
        gender: profileForm.gender || null,
        age: profileForm.age || null
      }
    })
    if (res.data.success) {
      showMessage('个人信息更新成功！')
      loadUserProfile()
      window.dispatchEvent(new CustomEvent('userInfoUpdated'))
    } else {
      showMessage(res.data.message, 'error')
    }
  } catch (e) {
    showMessage('更新失败，请重试', 'error')
  }
}

// 退出登录
const logout = () => {
  localStorage.removeItem('isLoggedIn')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('savedUsername')
  localStorage.removeItem('savedPassword')
  window.location.reload()
}

// 头像更新回调
const handleAvatarUpdated = (newAvatar) => {
  if (userInfo.value) {
    userInfo.value.avatar = newAvatar
  }
  loadUserProfile()
  showMessage('头像更新成功！')
}

onMounted(() => {
  loadUserProfile()
  loadUserPosts()
  if (isSelf.value) {
    loadBindInfo()
  }
  if (!isSelf.value) {
    loadFollowStatus()
  }
})
</script>

<style scoped>
.profile-container {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.glass-card {
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 24px;
  transition: all 0.2s;
}

.profile-header {
  text-align: center;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.avatar-edit, .avatar-view {
  width: 100px;
  height: 100px;
}

.avatar-img-large {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #40E0D0;
}

.avatar-placeholder-large {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #40E0D0, #2BA0D0);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  color: white;
}

.username {
  font-size: 24px;
  font-weight: 600;
  color: white;
  margin-top: 12px;
}

.user-badge {
  display: inline-block;
  background: rgba(64, 224, 208, 0.2);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  color: #40E0D0;
}

.follow-btn {
  padding: 8px 24px;
  border-radius: 40px;
  font-size: 14px;
  cursor: pointer;
  background: rgba(64, 224, 208, 0.2);
  border: 1px solid rgba(64, 224, 208, 0.4);
  color: #40E0D0;
  transition: all 0.2s;
  margin-top: 8px;
}

.follow-btn:hover {
  background: rgba(64, 224, 208, 0.35);
}

.follow-btn.following {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.3);
  color: white;
}

.stats-grid {
  display: flex;
  justify-content: space-around;
}

.stat-item {
  text-align: center;
  padding: 8px 16px;
  border-radius: 16px;
  transition: background 0.2s;
}

.stat-item.clickable {
  cursor: pointer;
}

.stat-item.clickable:hover {
  background: rgba(255, 255, 255, 0.05);
}

.stat-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #40E0D0;
  display: block;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.card-icon {
  font-size: 24px;
}

.card-header h4 {
  font-size: 18px;
  font-weight: 600;
  color: white;
  margin: 0;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 6px;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 10px 14px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 12px;
  color: white;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
}

.form-group input:focus,
.form-group select:focus {
  border-color: #40E0D0;
}

.form-group input:disabled,
.form-group select:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.readonly-input {
  background: rgba(255, 255, 255, 0.03);
  color: rgba(255, 255, 255, 0.4);
  cursor: not-allowed;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.password-btn {
  width: 100%;
  padding: 12px;
  background: rgba(64, 224, 208, 0.1);
  border: 1px solid rgba(64, 224, 208, 0.3);
  border-radius: 40px;
  color: #40E0D0;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin-bottom: 12px;
}

.password-btn:hover {
  background: rgba(64, 224, 208, 0.25);
  transform: translateY(-2px);
}

.save-btn {
  width: 100%;
  padding: 12px;
  background: rgba(64, 224, 208, 0.2);
  border: 1px solid rgba(64, 224, 208, 0.4);
  border-radius: 40px;
  color: #40E0D0;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 8px;
}

.save-btn:hover {
  background: rgba(64, 224, 208, 0.35);
  transform: translateY(-2px);
}

/* 安全设置样式 */
.bind-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.bind-item:last-child {
  border-bottom: none;
}

.bind-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.bind-icon {
  font-size: 24px;
}

.bind-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bind-label {
  font-size: 14px;
  color: white;
  font-weight: 500;
}

.bind-status {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.bind-status.bound {
  color: #40E0D0;
}

.bind-btn {
  padding: 6px 20px;
  background: rgba(64, 224, 208, 0.15);
  border: 1px solid rgba(64, 224, 208, 0.3);
  border-radius: 30px;
  font-size: 13px;
  cursor: pointer;
  color: #40E0D0;
  transition: all 0.2s;
}

.bind-btn:hover {
  background: rgba(64, 224, 208, 0.3);
}

.unbind-btn {
  padding: 6px 20px;
  background: rgba(255, 100, 100, 0.1);
  border: 1px solid rgba(255, 100, 100, 0.3);
  border-radius: 30px;
  font-size: 13px;
  cursor: pointer;
  color: #ff8888;
  transition: all 0.2s;
}

.unbind-btn:hover {
  background: rgba(255, 100, 100, 0.2);
}

/* 我的收藏卡片样式 */
.collection-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  cursor: pointer;
  transition: all 0.2s;
  border-radius: 16px;
}

.collection-item:hover {
  background: rgba(255, 255, 255, 0.05);
  padding: 14px 12px;
  margin: 0 -12px;
}

.collection-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collection-icon {
  font-size: 28px;
}

.collection-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.collection-label {
  font-size: 15px;
  color: white;
  font-weight: 500;
}

.collection-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
}

.arrow-icon {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.4);
  transition: transform 0.2s;
}

.collection-item:hover .arrow-icon {
  transform: translateX(4px);
  color: #40E0D0;
}

.logout-btn {
  width: 100%;
  padding: 12px;
  background: rgba(229, 115, 115, 0.15);
  border: 1px solid rgba(229, 115, 115, 0.4);
  border-radius: 40px;
  color: #ff8888;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: rgba(229, 115, 115, 0.25);
  transform: translateY(-2px);
}

.message {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  border-radius: 40px;
  font-size: 14px;
  z-index: 1100;
  white-space: nowrap;
  backdrop-filter: blur(8px);
}

.message.success {
  background: rgba(64, 224, 208, 0.2);
  color: #40E0D0;
  border: 1px solid rgba(64, 224, 208, 0.3);
}

.message.error {
  background: rgba(229, 115, 115, 0.2);
  color: #ff8888;
  border: 1px solid rgba(229, 115, 115, 0.3);
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: rgba(64, 224, 208, 0.3);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(64, 224, 208, 0.5);
}

@media (max-width: 768px) {
  .profile-container {
    padding: 0 12px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    flex-wrap: wrap;
  }
}
</style>

<!-- 全局样式（非 scoped，用于 Teleport 弹窗） -->
<style>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
}

.modal-container-large {
  background: #ffffff;
  border-radius: 24px;
  width: 90%;
  max-width: 600px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.modal-container-white {
  background: #ffffff;
  border-radius: 24px;
  width: 90%;
  max-width: 380px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.modal-header-white {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e8e8e8;
  background: #ffffff;
  flex-shrink: 0;
}

.modal-header-white h3 {
  color: #333;
  font-size: 16px;
  margin: 0;
  font-weight: 600;
}

.post-count-tag {
  font-size: 12px;
  color: #999;
  margin-left: 8px;
  font-weight: normal;
}

.close-btn-white {
  background: none;
  border: none;
  font-size: 22px;
  cursor: pointer;
  color: #999;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.close-btn-white:hover {
  background: #f0f0f0;
  color: #666;
}

.modal-body-posts {
  padding: 16px;
  flex: 1;
  overflow-y: auto;
  max-height: calc(85vh - 60px);
}

.modal-body-posts::-webkit-scrollbar {
  width: 4px;
}

.modal-body-posts::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 2px;
}

.modal-body-posts::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 2px;
}

.posts-list-modal {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-card-modal {
  background: #f9f9f9;
  border-radius: 16px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.post-card-modal:hover {
  background: #f0f0f0;
  transform: translateX(4px);
}

.post-content-modal {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  margin-bottom: 12px;
}

.post-meta-modal {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.post-stats-modal {
  display: flex;
  gap: 16px;
}

.modal-body-white {
  padding: 12px;
  flex: 1;
  overflow-y: auto;
  max-height: calc(80vh - 60px);
}

.modal-body-white::-webkit-scrollbar {
  width: 4px;
}

.modal-body-white::-webkit-scrollbar-track {
  background: #f0f0f0;
  border-radius: 2px;
}

.modal-body-white::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 2px;
}

.user-item-white {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.user-item-white:hover {
  background: #f5f5f5;
}

.user-avatar-white {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #40E0D0, #2BA0D0);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-avatar-white img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-name-white {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.empty-tip-white {
  text-align: center;
  padding: 40px 20px;
  color: #999;
}

.empty-hint {
  font-size: 12px;
  color: #bbb;
  margin-top: 8px;
}

.loading-state {
  text-align: center;
  padding: 40px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f0f0f0;
  border-top-color: #7CB342;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

.password-modal-container {
  background: #ffffff;
  border-radius: 24px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.password-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid #e8e8e8;
}

.password-modal-header h3 {
  color: #333;
  font-size: 18px;
  margin: 0;
  font-weight: 600;
}

.password-modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.password-modal-close:hover {
  background: #f0f0f0;
  color: #666;
}

.password-modal-body {
  padding: 24px;
}

.password-form-group {
  margin-bottom: 20px;
}

.password-form-group label {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
  font-weight: 500;
}

.password-form-group input {
  width: 100%;
  padding: 12px 16px;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
}

.password-form-group input:focus {
  border-color: #7CB342;
  background: #ffffff;
}

.password-modal-footer {
  padding: 16px 24px 24px 24px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  border-top: 1px solid #e8e8e8;
}

.password-cancel-btn {
  padding: 10px 24px;
  background: #f5f5f5;
  border: none;
  border-radius: 30px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  color: #666;
}

.password-cancel-btn:hover {
  background: #e8e8e8;
}

.password-confirm-btn {
  padding: 10px 28px;
  background: #7CB342;
  border: none;
  border-radius: 30px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  color: white;
  font-weight: 500;
}

.password-confirm-btn:hover {
  background: #689F38;
  transform: translateY(-1px);
}

/* 绑定弹窗样式 */
.bind-modal-container {
  background: #ffffff;
  border-radius: 24px;
  width: 90%;
  max-width: 380px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.bind-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 24px;
  border-bottom: 1px solid #e8e8e8;
}

.bind-modal-header h3 {
  color: #333;
  font-size: 18px;
  margin: 0;
  font-weight: 600;
}

.bind-modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.bind-modal-close:hover {
  background: #f0f0f0;
  color: #666;
}

.bind-modal-body {
  padding: 24px;
}

.bind-form-group {
  margin-bottom: 20px;
}

.bind-form-group label {
  display: block;
  font-size: 13px;
  color: #666;
  margin-bottom: 8px;
  font-weight: 500;
}

.bind-form-group input {
  width: 100%;
  padding: 12px 16px;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
}

.bind-form-group input:focus {
  border-color: #7CB342;
  background: #ffffff;
}

.bind-code-group {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.bind-code-group .code-input {
  flex: 1;
  padding: 12px 16px;
  background: #f5f5f5;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 14px;
  outline: none;
}

.bind-code-group .code-input:focus {
  border-color: #7CB342;
  background: #ffffff;
}

.send-code-btn {
  padding: 0 18px;
  background: #f0f0f0;
  border: 1px solid #e0e0e0;
  border-radius: 12px;
  font-size: 13px;
  cursor: pointer;
  color: #666;
  white-space: nowrap;
  transition: all 0.2s;
}

.send-code-btn:hover:not(:disabled) {
  background: #e8e8e8;
}

.send-code-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.bind-modal-footer {
  padding: 16px 24px 24px 24px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  border-top: 1px solid #e8e8e8;
}

.bind-cancel-btn {
  padding: 10px 24px;
  background: #f5f5f5;
  border: none;
  border-radius: 30px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  color: #666;
}

.bind-cancel-btn:hover {
  background: #e8e8e8;
}

.bind-confirm-btn {
  padding: 10px 28px;
  background: #7CB342;
  border: none;
  border-radius: 30px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  color: white;
  font-weight: 500;
}

.bind-confirm-btn:hover:not(:disabled) {
  background: #689F38;
  transform: translateY(-1px);
}

.bind-confirm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 确认弹窗样式 */
.confirm-modal-container {
  background: #ffffff;
  border-radius: 24px;
  width: 90%;
  max-width: 340px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.confirm-modal-header {
  padding: 20px 24px 0 24px;
}

.confirm-modal-header h3 {
  color: #333;
  font-size: 18px;
  margin: 0;
  font-weight: 600;
  text-align: center;
}

.confirm-modal-body {
  padding: 20px 24px;
  text-align: center;
}

.confirm-modal-body p {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.confirm-hint {
  font-size: 12px !important;
  color: #999 !important;
  margin-top: 8px !important;
}

.confirm-modal-footer {
  padding: 0 24px 20px 24px;
  display: flex;
  gap: 12px;
  justify-content: center;
}

.confirm-cancel-btn {
  padding: 10px 24px;
  background: #f5f5f5;
  border: none;
  border-radius: 30px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  color: #666;
  flex: 1;
}

.confirm-cancel-btn:hover {
  background: #e8e8e8;
}

.confirm-confirm-btn {
  padding: 10px 24px;
  background: #ff6b6b;
  border: none;
  border-radius: 30px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  color: white;
  font-weight: 500;
  flex: 1;
}

.confirm-confirm-btn:hover {
  background: #ff5252;
  transform: translateY(-1px);
}
</style>