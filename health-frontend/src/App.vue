<template>
  <div class="app">
    <!-- 加载中：什么都不显示 -->
    <div v-if="isLoading" class="loading-screen">
      <div class="loading-spinner"></div>
    </div>

    <!-- 未登录时只显示 LandingPage -->
    <LandingPage
        v-else-if="!isLoggedIn"
        @open-login="openLoginModal"
        @open-register="openRegisterModal"
    />

    <!-- 登录弹窗组件 -->
    <LoginModal
        v-if="showLoginModal"
        @close="closeModals"
        @switch-to-register="switchToRegister"
        @login-success="handleLoginSuccess"
    />

    <!-- 注册弹窗组件 -->
    <RegisterModal
        v-if="showRegisterModal"
        @close="closeModals"
        @switch-to-login="switchToLogin"
        @register-success="handleRegisterSuccess"
    />

    <!-- 登录后显示主界面 -->
    <div v-else-if="isLoggedIn" class="main-interface">
      <!-- 动态背景 -->
      <div class="interface-video-bg">
        <video autoplay loop muted playsinline class="interface-video">
          <source src="/videos/223691.mp4" type="video/mp4" />
        </video>
        <div class="interface-overlay-light"></div>
      </div>

      <div class="interface-content">
        <nav class="glass-nav">
          <div class="nav-left">
            <div class="avatar-wrapper">
              <img
                  v-if="userAvatar"
                  :src="getFullImageUrl(userAvatar)"
                  class="nav-avatar"
                  @error="handleAvatarError"
              />
              <span v-else class="nav-avatar-placeholder">👤</span>
            </div>
            <span class="user-greeting">你好，{{ userInfo?.username }}</span>
          </div>
          <div class="nav-right">
            <button class="nav-icon-btn" @click="goToProfile" title="个人中心">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M12 12C14.21 12 16 10.21 16 8C16 5.79 14.21 4 12 4C9.79 4 8 5.79 8 8C8 10.21 9.79 12 12 12Z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M20 21V19C20 16.8 18.2 15 16 15H8C5.8 15 4 16.8 4 19V21" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
            <button class="nav-icon-btn logout-btn-icon" @click="logout" title="退出">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M9 21H5C4.46957 21 3.96086 20.7893 3.58579 20.4142C3.21071 20.0391 3 19.5304 3 19V5C3 4.46957 3.21071 3.96086 3.58579 3.58579C3.96086 3.21071 4.46957 3 5 3H9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M16 17L21 12L16 7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M21 12H9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
        </nav>

        <!-- 管理员不显示顶部菜单栏，普通用户显示 -->
        <div class="tabs-wrapper" v-if="userInfo?.role !== 'ADMIN'">
          <div class="tabs">
            <button
                v-for="tab in tabs"
                :key="tab.key"
                :class="{active: currentTab === tab.key}"
                @click="currentTab = tab.key"
            >
              {{ tab.label }}
            </button>
          </div>
        </div>

        <div class="content-area">
          <HealthData
              v-if="currentTab === 'health'"
              :userInfo="userInfo"
              :healthForm="healthForm"
              @update:healthForm="healthForm = $event"
              @refresh="refreshData"
          />
          <TaskPage v-if="currentTab === 'task'" :userId="userInfo.id" />
          <SportPage v-if="currentTab === 'sport'" :userId="userInfo.id" />
          <DietPage v-if="currentTab === 'diet'" :userId="userInfo.id" />
          <Community
              v-if="currentTab === 'community'"
              :userId="userInfo.id"
              :routePostId="routePostId"
              @view-profile="handleViewProfile"
          />
          <div v-if="currentTab === 'healthAnalysis'" class="glass-card">
            <HealthAnalysis :userId="userInfo.id" />
          </div>
          <div v-if="currentTab === 'article'" class="glass-card">
            <HealthArticle :userId="userInfo.id" />
          </div>
          <div v-if="currentTab === 'profile'" class="glass-card">
            <UserProfile :userId="userInfo.id" />
          </div>
          <div v-if="currentTab === 'userProfile'" class="glass-card">
            <UserProfile :userId="viewingUserId" />
            <button @click="backToCommunity" class="back-btn">← 返回社区</button>
          </div>
          <div v-if="currentTab === 'favorites'" class="glass-card">
            <MyFavorites :userId="userInfo.id" />
          </div>
          <AdminPage v-if="currentTab === 'admin'" :userId="userInfo.id" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import LandingPage from './components/LandingPage.vue'
import LoginModal from './components/LoginModal.vue'
import RegisterModal from './components/RegisterModal.vue'
import HealthData from './components/HealthData.vue'
import TaskPage from './components/TaskPage.vue'
import SportPage from './components/SportPage.vue'
import DietPage from './components/DietPage.vue'
import AdminPage from './components/AdminPage.vue'
import UserProfile from './components/UserProfile.vue'
import Community from './components/Community.vue'
import HealthArticle from './components/HealthArticle.vue'
import HealthAnalysis from './components/HealthAnalysis.vue'
import MyFavorites from './components/MyFavorites.vue'
import { ref, onMounted, watch, nextTick, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import axios from 'axios'
import * as echarts from 'echarts'

const API_BASE = 'http://localhost:8080/api'

const router = useRouter()
const route = useRoute()

const routePostId = ref(null)

const isLoading = ref(true)
const isLoggedIn = ref(false)
const userInfo = ref(null)
const currentTab = ref('task')
const userAvatar = ref('')
const showLoginModal = ref(false)
const showRegisterModal = ref(false)
const viewingUserId = ref(null)

const healthForm = ref({
  height: '', weight: '', gender: '0', age: '', bodyFat: '',
  chest: '', waist: '', hip: '', thigh: '', restingHeartRate: '',
  sleepDuration: '', waterIntake: ''
})

const weightHistory = ref({ dates: [], weights: [] })
let weightChart = null

let isUpdatingFromRoute = false

// 普通用户的菜单
const userTabs = [
  { key: 'task', label: '打卡任务' },
  { key: 'sport', label: '运动记录' },
  { key: 'diet', label: '饮食记录' },
  { key: 'health', label: '健康数据' },
  { key: 'healthAnalysis', label: '健康分析' },
  { key: 'article', label: '健康资讯' },
  { key: 'community', label: '健康社区' },
]

// 管理员的菜单（只有管理后台）
const adminTabs = [
  { key: 'admin', label: '管理后台' },
]

// 根据角色动态计算菜单
const tabs = computed(() => {
  if (userInfo.value?.role === 'ADMIN') {
    return adminTabs
  }
  return userTabs
})

const isValidTab = (tabKey) => {
  // 管理员只能访问 admin
  if (userInfo.value?.role === 'ADMIN') {
    return tabKey === 'admin'
  }
  // 普通用户可以访问的 tabs
  const validTabs = [
    'health', 'task', 'sport', 'diet', 'community',
    'healthAnalysis', 'article', 'profile', 'userProfile', 'favorites'
  ]
  return validTabs.includes(tabKey)
}

const getTabFromPath = (path) => {
  if (path === '/community') return 'community'
  if (path === '/health-data') return 'health'
  if (path === '/task-page') return 'task'
  if (path === '/sport-page') return 'sport'
  if (path === '/diet-page') return 'diet'
  if (path === '/health-analysis') return 'healthAnalysis'
  if (path.startsWith('/profile/')) return 'profile'
  if (path.startsWith('/favorites/')) return 'favorites'
  return null
}

const goToProfile = () => {
  if (userInfo.value?.id) {
    currentTab.value = 'profile'
    router.push(`/profile/${userInfo.value.id}`)
  }
}

watch(() => route.path, (newPath, oldPath) => {
  if (newPath === oldPath) return

  const newTab = getTabFromPath(newPath)
  if (newTab && newTab !== currentTab.value) {
    isUpdatingFromRoute = true
    currentTab.value = newTab
    nextTick(() => {
      isUpdatingFromRoute = false
    })
  }
  if (route.query.postId) {
    routePostId.value = route.query.postId
  } else {
    routePostId.value = null
  }
}, { immediate: true })

watch(currentTab, (newTab) => {
  if (!isLoggedIn.value) return
  if (isUpdatingFromRoute) return

  const tabToPath = {
    health: '/health-data',
    task: '/task-page',
    sport: '/sport-page',
    diet: '/diet-page',
    community: '/community',
    healthAnalysis: '/health-analysis',
    profile: `/profile/${userInfo.value?.id}`,
    favorites: `/favorites/${userInfo.value?.id}`
  }

  const targetPath = tabToPath[newTab]
  if (targetPath && route.path !== targetPath) {
    router.push(targetPath)
  }
})

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
    span.className = 'nav-avatar-placeholder'
    parent.appendChild(span)
  }
}

const loadUserAvatar = async () => {
  if (!userInfo.value?.id) return
  try {
    const res = await axios.get(`${API_BASE}/user/${userInfo.value.id}`)
    if (res.data && res.data.avatar) {
      userAvatar.value = res.data.avatar
    }
  } catch (e) {
    console.error('加载头像失败', e)
  }
}

const handleViewProfile = (targetUserId) => {
  if (targetUserId === 'self') {
    currentTab.value = 'profile'
    viewingUserId.value = null
    router.push(`/profile/${userInfo.value?.id}`)
  } else {
    viewingUserId.value = targetUserId
    currentTab.value = 'userProfile'
  }
}

const backToCommunity = () => {
  currentTab.value = 'community'
  viewingUserId.value = null
}

const openLoginModal = () => {
  showLoginModal.value = true
  showRegisterModal.value = false
}

const openRegisterModal = () => {
  showRegisterModal.value = true
  showLoginModal.value = false
}

const switchToRegister = () => {
  showLoginModal.value = false
  showRegisterModal.value = true
}

const switchToLogin = () => {
  showRegisterModal.value = false
  showLoginModal.value = true
}

const closeModals = () => {
  showLoginModal.value = false
  showRegisterModal.value = false
}

const handleLoginSuccess = async (userData) => {
  // 统一使用 localStorage
  localStorage.setItem('isLoggedIn', 'true')
  localStorage.setItem('userInfo', JSON.stringify(userData))

  isLoggedIn.value = true
  userInfo.value = {
    id: userData.id,
    username: userData.username,
    role: userData.role,
    currentWeight: null,
    currentHeight: null
  }

  // 如果是管理员，自动切换到管理后台
  if (userData.role === 'ADMIN') {
    currentTab.value = 'admin'
    router.push('/admin')
  }

  const userRes = await axios.get(`${API_BASE}/user/${userInfo.value.id}`)
  userInfo.value.currentWeight = userRes.data.currentWeight
  userInfo.value.currentHeight = userRes.data.currentHeight
  await loadUserAvatar()
}

const handleRegisterSuccess = (username) => {
  showRegisterModal.value = false
  showLoginModal.value = true
}

const refreshData = async () => {
  if (!userInfo.value) return
  try {
    const res = await axios.get(`${API_BASE}/user/${userInfo.value.id}`)
    userInfo.value.currentWeight = res.data.currentWeight
    userInfo.value.currentHeight = res.data.currentHeight
  } catch (e) {
    console.error(e)
  }
}

const logout = () => {
  isLoggedIn.value = false
  userInfo.value = null
  userAvatar.value = ''
  currentTab.value = 'task'
  viewingUserId.value = null
  // 清除 localStorage 中的登录信息
  localStorage.removeItem('isLoggedIn')
  localStorage.removeItem('userInfo')
  localStorage.removeItem('currentTab')
}

const loadWeightHistory = async () => {
  if (!userInfo.value) return
  try {
    const res = await axios.get(`${API_BASE}/health-analysis/weight-history/${userInfo.value.id}`)
    weightHistory.value = res.data
    if (weightChart && weightHistory.value.dates.length > 0) {
      weightChart.setOption({ xAxis: { data: weightHistory.value.dates }, series: [{ data: weightHistory.value.weights }] })
    }
  } catch (e) { console.error('加载体重数据失败', e) }
}

onMounted(async () => {
  // 从 localStorage 读取登录状态
  const savedIsLoggedIn = localStorage.getItem('isLoggedIn')
  const savedUserInfo = localStorage.getItem('userInfo')
  const savedCurrentTab = localStorage.getItem('currentTab')
  const savedViewingUserId = localStorage.getItem('viewingUserId')

  if (savedIsLoggedIn === 'true' && savedUserInfo) {
    isLoggedIn.value = true
    userInfo.value = JSON.parse(savedUserInfo)

    await nextTick()

    // 如果是管理员，自动切换到管理后台
    if (userInfo.value?.role === 'ADMIN') {
      currentTab.value = 'admin'
    }

    // 恢复之前所在的 tab（仅对非管理员）
    if (savedCurrentTab && isValidTab(savedCurrentTab) && userInfo.value?.role !== 'ADMIN') {
      currentTab.value = savedCurrentTab
      if (savedCurrentTab === 'userProfile' && savedViewingUserId) {
        viewingUserId.value = parseInt(savedViewingUserId)
      }
    }

    refreshData()
    loadUserAvatar()
    setTimeout(() => {
      const chartDom = document.getElementById('weightChart')
      if (chartDom) {
        weightChart = echarts.init(chartDom)
        weightChart.setOption({
          title: { text: '体重变化趋势', textStyle: { color: '#fff' } },
          tooltip: { trigger: 'axis' },
          xAxis: { type: 'category', name: '日期', nameTextStyle: { color: 'rgba(255,255,255,0.6)' }, axisLabel: { color: 'rgba(255,255,255,0.6)' } },
          yAxis: { type: 'value', name: '体重(kg)', nameTextStyle: { color: 'rgba(255,255,255,0.6)' }, axisLabel: { color: 'rgba(255,255,255,0.6)' } },
          series: [{ type: 'line', data: [], smooth: true, lineStyle: { color: '#40E0D0', width: 3 } }]
        })
      }
      loadWeightHistory()
    }, 500)
  }

  isLoading.value = false
})

watch(currentTab, (newTab) => {
  if (isLoggedIn.value && newTab) {
    localStorage.setItem('currentTab', newTab)
    if (newTab === 'userProfile' && viewingUserId.value) {
      localStorage.setItem('viewingUserId', viewingUserId.value)
    } else if (newTab !== 'userProfile') {
      localStorage.removeItem('viewingUserId')
    }
  }
})

watch(isLoggedIn, (newVal) => {
  if (newVal) {
    setTimeout(async () => {
      await loadWeightHistory()
    }, 500)
  }
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Inter', 'PingFang SC', 'Microsoft YaHei', sans-serif;
  background: transparent;
}

.loading-screen {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 3px solid rgba(255, 255, 255, 0.2);
  border-top-color: #40E0D0;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.main-interface {
  position: relative;
  min-height: 100vh;
}

.interface-video-bg {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  overflow: hidden;
}

.interface-video {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 30%;
}

.interface-overlay-light {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.35);
  z-index: 1;
}

.interface-content {
  position: relative;
  z-index: 2;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 32px;
}

.glass-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 60px;
  margin-bottom: 28px;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar-wrapper {
  width: 44px;
  height: 44px;
}

.nav-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #40E0D0;
}

.nav-avatar-placeholder {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(64, 224, 208, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #40E0D0;
}

.user-greeting {
  font-size: 16px;
  font-weight: 500;
  color: white;
}

.nav-right {
  display: flex;
  gap: 12px;
}

.nav-icon-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.2s;
}

.nav-icon-btn:hover {
  background: rgba(64, 224, 208, 0.2);
  border-color: #40E0D0;
  color: #40E0D0;
}

.back-btn {
  margin-top: 20px;
  padding: 10px 20px;
  background: rgba(64, 224, 208, 0.2);
  border: 1px solid rgba(64, 224, 208, 0.4);
  border-radius: 30px;
  color: #40E0D0;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(64, 224, 208, 0.35);
}

.tabs-wrapper {
  margin-bottom: 28px;
  overflow-x: auto;
}

.tabs {
  display: flex;
  gap: 8px;
  background: rgba(0, 0, 0, 0.12);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 60px;
  padding: 6px;
  width: fit-content;
}

.tabs button {
  background: transparent;
  border: none;
  padding: 10px 28px;
  border-radius: 40px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.6);
  white-space: nowrap;
  transition: all 0.2s;
}

.tabs button:hover {
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.05);
}

.tabs button.active {
  background: rgba(64, 224, 208, 0.2);
  color: #40E0D0;
}

.content-area {
  background: rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  padding: 24px;
  min-height: 400px;
}

.glass-card {
  background: rgba(0, 0, 0, 0.1);
  backdrop-filter: blur(6px);
  border-radius: 20px;
  padding: 24px;
}

@media (max-width: 768px) {
  .interface-content {
    padding: 16px;
  }
  .tabs button {
    padding: 6px 18px;
    font-size: 12px;
  }
  .glass-nav {
    padding: 10px 16px;
  }
  .avatar-wrapper,
  .nav-avatar,
  .nav-avatar-placeholder {
    width: 36px;
    height: 36px;
  }
}
</style>