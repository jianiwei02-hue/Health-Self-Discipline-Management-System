<template>
  <div class="task-container">
    <!-- 顶部统计卡片 + 提醒按钮 -->
    <div class="stats-header">
      <div class="stats-row">
        <div class="glass-card stat-card">
          <div class="stat-value">{{ todayTasks.length }}</div>
          <div class="stat-label">今日任务</div>
        </div>
        <div class="glass-card stat-card">
          <div class="stat-value">{{ completionRate }}%</div>
          <div class="stat-label">完成率</div>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: completionRate + '%' }"></div>
          </div>
        </div>
        <div class="glass-card stat-card">
          <div class="stat-value">{{ consecutiveDays }}</div>
          <div class="stat-label">连续打卡</div>
        </div>
      </div>

      <!-- 提醒按钮区域 -->
      <div class="reminder-area">
        <button class="reminder-btn" @click="openReminderSettings" :class="{ 'has-reminder': reminderEnabled }">
          <span class="bell-icon">🔔</span>
          <span v-if="uncompletedCount > 0" class="badge">{{ uncompletedCount }}</span>
        </button>
        <span v-if="reminderEnabled && reminderTime" class="reminder-hint">
          ⏰ 每日 {{ reminderTime }} 提醒
        </span>
        <span v-else-if="!reminderEnabled" class="reminder-hint off">
          🔕 提醒已关闭
        </span>
      </div>
    </div>

    <!-- 添加任务卡片 -->
    <div class="glass-card">
      <div class="card-header">
        <span class="card-icon">✅</span>
        <h4>添加今日任务</h4>
      </div>
      <div class="form-row">
        <input
            v-model="taskForm.taskName"
            placeholder="任务名称（如：跑步5公里）"
            class="glass-input"
            @keyup.enter="createTask"
        />

        <div class="custom-select" ref="selectTriggerRef">
          <div class="custom-select-trigger" @click="toggleDropdown">
            <span class="select-value">{{ getTypeIcon(taskForm.taskType) }}</span>
            <span class="select-arrow">▼</span>
          </div>
        </div>

        <button class="glass-btn primary" @click="createTask">添加任务</button>
      </div>

      <Teleport to="body">
        <div
            v-if="isDropdownOpen"
            class="custom-select-dropdown-fixed"
            :style="dropdownStyle"
            @click.stop
        >
          <div
              v-for="option in taskTypeOptions"
              :key="option.value"
              class="custom-select-option"
              :class="{ active: taskForm.taskType === option.value }"
              @click="selectOption(option.value)"
          >
            <span class="option-icon">{{ option.icon }}</span>
            <span>{{ option.label }}</span>
          </div>
        </div>
      </Teleport>

      <div class="smart-tags" v-if="!taskForm.taskName">
        <span class="tag-label">💡 猜你想做：</span>
        <button
            v-for="suggestion in suggestions"
            :key="suggestion"
            class="suggestion-tag"
            @click="taskForm.taskName = suggestion"
        >
          {{ suggestion }}
        </button>
      </div>
    </div>

    <!-- 今日任务列表 - 内联编辑 -->
    <div class="glass-card">
      <div class="card-header">
        <span class="card-icon">📋</span>
        <h4>今日任务列表</h4>
        <span class="count-badge">{{ completedCount }}/{{ todayTasks.length }}</span>
      </div>
      <div class="task-list">
        <div
            v-for="task in todayTasks"
            :key="task.id"
            class="task-item"
            :class="{ 'task-completed': task.status === 1, 'editing-mode': editingTask && editingTask.id === task.id }"
        >
          <template v-if="!editingTask || editingTask.id !== task.id">
            <div class="task-left">
              <span class="task-icon">{{ getTaskIcon(task.taskType) }}</span>
              <span class="task-name" :class="{ completed: task.status === 1 }">{{ task.taskName }}</span>
            </div>
            <div class="task-right">
              <span class="task-badge" :class="getTypeClass(task.taskType)">
                {{ getTypeName(task.taskType) }}
              </span>
              <button v-if="task.status === 0" class="action-btn edit" @click="startEditTask(task)">
                ✏️ 编辑
              </button>
              <button v-if="task.status === 0" class="check-btn" @click="completeTask(task.id)">
                打卡 ✓
              </button>
              <span v-else class="completed-badge">✓ 已完成</span>
            </div>
          </template>

          <template v-else>
            <div class="edit-fields">
              <input v-model="editingTask.taskName" class="edit-input" placeholder="任务名称" />
              <div class="type-selector-inline">
                <button
                    v-for="option in taskTypeOptions"
                    :key="option.value"
                    class="type-option-inline"
                    :class="{ active: editingTask.taskType === option.value }"
                    @click="editingTask.taskType = option.value"
                >
                  {{ option.icon }} {{ option.label }}
                </button>
              </div>
            </div>
            <div class="task-right">
              <button class="action-btn save" @click="saveEditTask">✓ 保存</button>
              <button class="action-btn cancel" @click="cancelEditTask">✗ 取消</button>
            </div>
          </template>
        </div>
        <div v-if="todayTasks.length === 0" class="empty-state">
          <span class="empty-icon">📭</span>
          <p>暂无任务，添加一个吧</p>
          <p class="empty-hint">点击上方输入框开始你的自律之旅~</p>
        </div>
      </div>
    </div>

    <!-- 我的勋章 -->
    <div class="glass-card">
      <div class="card-header">
        <span class="card-icon">🏅</span>
        <h4>我的勋章</h4>
        <span class="count-badge">{{ myMedals.length }}/{{ medalRules.length }}</span>
      </div>
      <div class="medals-grid">
        <div
            v-for="medal in medalRules"
            :key="medal.id"
            class="medal-item"
            :class="{ earned: isMedalEarned(medal), locked: !isMedalEarned(medal) }"
            :title="getMedalHint(medal)"
        >
          <span class="medal-icon">{{ medal.medalIcon || medal.icon || '🏅' }}</span>
          <span class="medal-name">{{ medal.medalName }}</span>
          <span class="medal-condition">{{ medal.description || medal.condition }}</span>
        </div>
      </div>
      <div v-if="medalRules.length === 0" class="empty-state small">
        <span class="empty-icon">🔥</span>
        <p>加载勋章中...</p>
      </div>
    </div>

    <!-- 提醒设置弹窗（关键修复：居中显示，无需滚动） -->
    <Teleport to="body">
      <div v-if="showReminderModal" class="modal-overlay-fixed" @click.self="closeReminderModal">
        <div class="modal-content-fixed glass">
          <div class="modal-header">
            <span class="modal-icon">⏰</span>
            <h3>打卡提醒设置</h3>
            <button class="modal-close" @click="closeReminderModal">✕</button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>开启提醒</label>
              <div class="toggle-switch">
                <input type="checkbox" v-model="reminderEnabled" class="toggle-input" id="reminderToggle">
                <label class="toggle-label" for="reminderToggle"></label>
              </div>
            </div>
            <div class="form-group" v-if="reminderEnabled">
              <label>提醒时间</label>
              <input type="time" v-model="reminderTime" class="glass-input" />
            </div>
            <div class="form-group" v-if="reminderEnabled && notificationPermission !== 'granted'">
              <button class="glass-btn primary" @click="requestNotificationPermission">
                🔔 开启浏览器通知
              </button>
              <p class="hint-text">开启后，每天会在指定时间弹出提醒</p>
            </div>
            <div class="form-group" v-if="reminderEnabled && notificationPermission === 'granted'">
              <p class="success-text">✅ 通知已开启，每日 {{ reminderTime }} 会提醒你打卡</p>
            </div>
          </div>
          <div class="modal-footer">
            <button class="glass-btn cancel" @click="closeReminderModal">取消</button>
            <button class="glass-btn save" @click="saveReminderSettings">保存</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import axios from 'axios'

const props = defineProps({ userId: Number })
const API_BASE = 'http://localhost:8080/api'

// 数据
const todayTasks = ref([])
const myMedals = ref([])
const medalRules = ref([])  // 勋章规则列表
const consecutiveDays = ref(0)
const taskForm = ref({ taskName: '', taskType: 'SPORT' })
const isDropdownOpen = ref(false)
const selectTriggerRef = ref(null)
const dropdownStyle = ref({})

// 提醒功能
const showReminderModal = ref(false)
const reminderEnabled = ref(false)
const reminderTime = ref('20:00')
const notificationPermission = ref('default')
let reminderInterval = null

const uncompletedCount = computed(() => {
  return todayTasks.value.filter(t => t.status === 0).length
})

const requestNotificationPermission = async () => {
  if (!('Notification' in window)) {
    alert('您的浏览器不支持通知功能')
    return
  }
  const permission = await Notification.requestPermission()
  notificationPermission.value = permission
  if (permission === 'granted') {
    localStorage.setItem('reminderEnabled', reminderEnabled.value)
    localStorage.setItem('reminderTime', reminderTime.value)
    localStorage.setItem('notificationPermission', permission)
  }
}

const checkAndNotify = () => {
  if (!reminderEnabled.value) return
  if (notificationPermission.value !== 'granted') return

  const now = new Date()
  const [hour, minute] = reminderTime.value.split(':')
  const currentHour = now.getHours()
  const currentMinute = now.getMinutes()

  if (currentHour === parseInt(hour) && currentMinute === parseInt(minute)) {
    const lastNotify = localStorage.getItem('lastNotifyDate')
    const today = new Date().toDateString()

    if (lastNotify !== today && uncompletedCount.value > 0) {
      new Notification('打卡提醒', {
        body: `今天还有 ${uncompletedCount.value} 个任务未完成，快去打卡吧！`,
        icon: '/favicon.ico',
        silent: false
      })
      localStorage.setItem('lastNotifyDate', today)
    }
  }
}

const startReminderCheck = () => {
  if (reminderInterval) clearInterval(reminderInterval)
  reminderInterval = setInterval(checkAndNotify, 60000)
}

const loadReminderSettings = () => {
  const saved = localStorage.getItem('reminderEnabled')
  reminderEnabled.value = saved === 'true'
  const savedTime = localStorage.getItem('reminderTime')
  if (savedTime) reminderTime.value = savedTime
  const savedPermission = localStorage.getItem('notificationPermission')
  if (savedPermission === 'granted') {
    notificationPermission.value = savedPermission
  } else if ('Notification' in window && Notification.permission === 'granted') {
    notificationPermission.value = 'granted'
  }
}

const saveReminderSettings = () => {
  localStorage.setItem('reminderEnabled', reminderEnabled.value)
  localStorage.setItem('reminderTime', reminderTime.value)
  if (notificationPermission.value === 'granted') {
    localStorage.setItem('notificationPermission', 'granted')
  }
  closeReminderModal()
  alert('提醒设置已保存')
}

// 关键修复：打开弹窗时不需要任何位置计算，直接显示，弹窗样式已固定居中
const openReminderSettings = () => {
  showReminderModal.value = true
}

const closeReminderModal = () => {
  showReminderModal.value = false
}

// 内联编辑
const editingTask = ref(null)
const taskTypeOptions = [
  { value: 'SPORT', label: '运动', icon: '🏃' },
  { value: 'DIET', label: '饮食', icon: '🥗' },
  { value: 'SLEEP', label: '作息', icon: '😴' },
  { value: 'OTHER', label: '其他', icon: '✨' }
]

const suggestions = ['晨跑 3km', '喝 8 杯水', '深蹲 50 个', '阅读 30 分钟', '拉伸 10 分钟']

const completionRate = computed(() => {
  if (todayTasks.value.length === 0) return 0
  const completed = todayTasks.value.filter(t => t.status === 1).length
  return Math.round((completed / todayTasks.value.length) * 100)
})

const completedCount = computed(() => todayTasks.value.filter(t => t.status === 1).length)

const getTypeIcon = (type) => {
  const option = taskTypeOptions.find(opt => opt.value === type)
  return option ? option.icon : '🏃'
}

const getTaskIcon = (type) => {
  const icons = { SPORT: '🏃', DIET: '🥗', SLEEP: '😴', OTHER: '✨' }
  return icons[type] || '📝'
}

const getTypeName = (type) => {
  const names = { SPORT: '运动', DIET: '饮食', SLEEP: '作息', OTHER: '其他' }
  return names[type] || '其他'
}

const getTypeClass = (type) => {
  const classes = {
    SPORT: 'badge-sport',
    DIET: 'badge-diet',
    SLEEP: 'badge-sleep',
    OTHER: 'badge-other'
  }
  return classes[type] || 'badge-other'
}

// 检查勋章是否已获得
const isMedalEarned = (medal) => {
  return myMedals.value.some(m => m.medalId === medal.id)
}

const getMedalHint = (medal) => {
  return isMedalEarned(medal) ? `✨ 已获得：${medal.medalName}` : `🔒 解锁条件：${medal.description || medal.condition}`
}

const updateDropdownPosition = () => {
  if (!selectTriggerRef.value) return
  const rect = selectTriggerRef.value.getBoundingClientRect()
  dropdownStyle.value = {
    position: 'fixed',
    top: `${rect.bottom + 4}px`,
    left: `${rect.left}px`,
    minWidth: `${rect.width}px`,
    zIndex: 99999
  }
}

const toggleDropdown = () => {
  if (isDropdownOpen.value) {
    isDropdownOpen.value = false
  } else {
    updateDropdownPosition()
    isDropdownOpen.value = true
  }
}

const selectOption = (value) => {
  taskForm.value.taskType = value
  isDropdownOpen.value = false
}

const handleClickOutside = (event) => {
  if (selectTriggerRef.value && !selectTriggerRef.value.contains(event.target)) {
    isDropdownOpen.value = false
  }
}

const startEditTask = (task) => {
  editingTask.value = { ...task }
}

const cancelEditTask = () => {
  editingTask.value = null
}

const saveEditTask = async () => {
  if (!editingTask.value) return
  if (!editingTask.value.taskName || editingTask.value.taskName.trim() === '') {
    alert('任务名称不能为空')
    return
  }

  try {
    const response = await axios.put(`${API_BASE}/task/update/${editingTask.value.id}`, null, {
      params: {
        taskName: editingTask.value.taskName.trim(),
        taskType: editingTask.value.taskType
      }
    })
    if (response.data.success) {
      alert('修改成功')
      editingTask.value = null
      await loadTodayTasks()
      await loadConsecutiveDays()
    } else {
      alert(response.data.message || '修改失败')
    }
  } catch (e) {
    console.error('修改失败', e)
    alert('修改失败: ' + (e.response?.data?.message || e.message))
  }
}

const loadTodayTasks = async () => {
  if (!props.userId) return
  try {
    const res = await axios.get(`${API_BASE}/task/today/${props.userId}`)
    todayTasks.value = res.data || []
  } catch (e) {
    console.error('加载任务失败', e)
    todayTasks.value = []
  }
}

const loadUserMedals = async () => {
  if (!props.userId) return
  try {
    const res = await axios.get(`${API_BASE}/medal/user/${props.userId}`)
    myMedals.value = res.data || []
  } catch (e) {
    console.error('加载用户勋章失败', e)
    myMedals.value = []
  }
}

// 加载所有勋章规则
const loadMedalRules = async () => {
  try {
    const res = await axios.get(`${API_BASE}/medal/rules`)
    medalRules.value = res.data || []
  } catch (e) {
    console.error('加载勋章规则失败', e)
    medalRules.value = []
  }
}

const loadConsecutiveDays = async () => {
  if (!props.userId) return
  try {
    const res = await axios.get(`${API_BASE}/task/consecutive/${props.userId}`)
    consecutiveDays.value = typeof res.data === 'number' ? res.data : (res.data?.data || 0)
  } catch (e) {
    console.error('加载连续打卡失败', e)
    consecutiveDays.value = 0
  }
}

const createTask = async () => {
  if (!taskForm.value.taskName) {
    alert('请输入任务名称')
    return
  }
  try {
    await axios.post(`${API_BASE}/task/create`, null, {
      params: {
        userId: props.userId,
        taskName: taskForm.value.taskName,
        taskType: taskForm.value.taskType
      }
    })
    taskForm.value.taskName = ''
    await loadTodayTasks()
    await loadConsecutiveDays()
  } catch (e) {
    console.error('创建失败', e)
    alert('创建失败')
  }
}

const completeTask = async (taskId) => {
  try {
    const res = await axios.post(`${API_BASE}/task/complete`, null, {
      params: { taskId }
    })
    if (res.data.success) {
      await loadTodayTasks()
      await loadUserMedals()
      await loadConsecutiveDays()
    } else {
      alert(res.data.message || '打卡失败')
    }
  } catch (e) {
    console.error('打卡失败', e)
    alert('打卡失败')
  }
}

const handleResize = () => {
  if (isDropdownOpen.value) {
    updateDropdownPosition()
  }
}

onMounted(() => {
  loadTodayTasks()
  loadUserMedals()
  loadMedalRules()  // 加载勋章规则
  loadConsecutiveDays()
  loadReminderSettings()
  startReminderCheck()
  document.addEventListener('click', handleClickOutside)
  window.addEventListener('resize', handleResize)
  window.addEventListener('scroll', handleResize)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('scroll', handleResize)
  if (reminderInterval) clearInterval(reminderInterval)
})
</script>

<style scoped>
/* 样式保持你原来的不变，这里省略因为太长，用你原来的样式即可 */
.task-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.stats-header {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  flex-wrap: wrap;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  flex: 1;
}

.reminder-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 80px;
}

.reminder-btn {
  position: relative;
  background: rgba(64, 224, 208, 0.15);
  border: 1px solid rgba(64, 224, 208, 0.3);
  width: 56px;
  height: 56px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  transition: all 0.2s;
}

.reminder-btn:hover {
  background: rgba(64, 224, 208, 0.3);
  transform: scale(1.05);
}

.reminder-btn.has-reminder {
  background: rgba(64, 224, 208, 0.25);
  border-color: #40E0D0;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(64, 224, 208, 0.4); }
  70% { box-shadow: 0 0 0 10px rgba(64, 224, 208, 0); }
  100% { box-shadow: 0 0 0 0 rgba(64, 224, 208, 0); }
}

.badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #ff4444;
  color: white;
  font-size: 11px;
  font-weight: 600;
  min-width: 20px;
  height: 20px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
}

.reminder-hint {
  font-size: 11px;
  color: #40E0D0;
  text-align: center;
  white-space: nowrap;
}

.reminder-hint.off {
  color: rgba(255, 255, 255, 0.4);
}

/* 弹窗：无遮罩层，仅弹窗本身居中显示 */
.modal-overlay-fixed {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000000;
  pointer-events: none;
}

.modal-content-fixed {
  width: 90%;
  max-width: 380px;
  border-radius: 28px;
  overflow: hidden;
  background: rgba(20, 20, 35, 0.96);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.5);
  pointer-events: auto;
  animation: modalFadeIn 0.2s ease-out;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px 20px 12px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.modal-icon {
  font-size: 24px;
}

.modal-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: white;
  margin: 0;
  flex: 1;
}

.modal-close {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  font-size: 18px;
  cursor: pointer;
  color: white;
  transition: all 0.2s;
}

.modal-close:hover {
  background: rgba(255, 255, 255, 0.2);
}

.modal-body {
  padding: 20px;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 12px 20px 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 8px;
}

.hint-text {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 6px;
}

.success-text {
  font-size: 12px;
  color: #40E0D0;
  margin-top: 6px;
}

.toggle-switch {
  position: relative;
  width: 52px;
  height: 28px;
}

.toggle-input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-label {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 28px;
  transition: 0.3s;
}

.toggle-label:before {
  position: absolute;
  content: "";
  height: 22px;
  width: 22px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  border-radius: 50%;
  transition: 0.3s;
}

.toggle-input:checked + .toggle-label {
  background-color: #40E0D0;
}

.toggle-input:checked + .toggle-label:before {
  transform: translateX(24px);
}

.glass-card {
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 24px;
  transition: all 0.2s;
}

.stat-card {
  text-align: center;
  padding: 20px !important;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #40E0D0;
}

.stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 6px;
}

.progress-bar {
  margin-top: 12px;
  height: 4px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #40E0D0;
  border-radius: 4px;
  transition: width 0.3s ease;
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
  font-weight: 500;
  color: white;
  margin: 0;
  flex: 1;
}

.count-badge {
  background: rgba(255, 255, 255, 0.1);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.form-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.glass-input,
.custom-select-trigger {
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  padding: 12px 16px;
  color: white;
  font-size: 14px;
  outline: none;
  transition: all 0.2s ease;
}

.glass-input {
  flex: 2;
}

.custom-select {
  position: relative;
  flex: 1;
  min-width: 70px;
}

.custom-select-trigger {
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.glass-btn {
  padding: 12px 28px;
  border-radius: 40px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
}

.glass-btn.primary {
  background: rgba(64, 224, 208, 0.2);
  border: 1px solid rgba(64, 224, 208, 0.4);
  color: #40E0D0;
}

.glass-btn.save {
  background: rgba(64, 224, 208, 0.2);
  border: 1px solid rgba(64, 224, 208, 0.4);
  color: #40E0D0;
}

.glass-btn.cancel {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.7);
}

.smart-tags {
  margin-top: 16px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.suggestion-tag {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  padding: 6px 14px;
  font-size: 12px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.2s;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.3s ease;
}

.task-item.task-completed {
  opacity: 0.7;
  background: rgba(255, 255, 255, 0.02);
}

.task-item.editing-mode {
  background: rgba(64, 224, 208, 0.1);
  border-color: rgba(64, 224, 208, 0.3);
  flex-direction: column;
  align-items: stretch;
  gap: 12px;
}

.task-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-icon {
  font-size: 20px;
}

/* 任务名称字体白色 */
.task-name {
  font-size: 15px;
  color: white;
}

.task-name.completed {
  text-decoration: line-through;
  color: rgba(255, 255, 255, 0.4);
}

.task-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 500;
}

.badge-sport {
  background: rgba(64, 224, 208, 0.2);
  color: #40E0D0;
}

.badge-diet {
  background: rgba(255, 193, 7, 0.2);
  color: #ffc107;
}

.badge-sleep {
  background: rgba(155, 89, 182, 0.2);
  color: #9b59b6;
}

.badge-other {
  background: rgba(200, 200, 200, 0.2);
  color: #ccc;
}

.check-btn, .action-btn {
  background: rgba(64, 224, 208, 0.2);
  border: 1px solid rgba(64, 224, 208, 0.4);
  padding: 6px 16px;
  border-radius: 30px;
  font-size: 12px;
  cursor: pointer;
  color: #40E0D0;
  transition: all 0.2s;
}

.action-btn.edit {
  background: rgba(255, 193, 7, 0.2);
  border-color: rgba(255, 193, 7, 0.4);
  color: #ffc107;
}

.action-btn.save {
  background: rgba(76, 175, 80, 0.2);
  border-color: rgba(76, 175, 80, 0.4);
  color: #4caf50;
}

.action-btn.cancel {
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.7);
}

.completed-badge {
  font-size: 13px;
  color: #40E0D0;
}

.edit-fields {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.edit-input {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  padding: 10px 16px;
  color: white;
  font-size: 14px;
  width: 100%;
  outline: none;
}

.type-selector-inline {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.type-option-inline {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 30px;
  padding: 6px 14px;
  font-size: 12px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.2s;
}

.type-option-inline.active {
  background: rgba(64, 224, 208, 0.2);
  border-color: #40E0D0;
  color: #40E0D0;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
}

.empty-icon {
  font-size: 48px;
  opacity: 0.5;
  margin-bottom: 12px;
  display: inline-block;
}

/* ========== 勋章区域样式优化 ========== */
.medals-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(130px, 1fr));
  gap: 16px;
}

.medal-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 8px;
  padding: 18px 12px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.3s ease;
}

/* 已获得勋章样式 - 更明显的点亮效果 */
.medal-item.earned {
  background: linear-gradient(135deg, rgba(64, 224, 208, 0.2), rgba(64, 224, 208, 0.08));
  border: 1px solid rgba(64, 224, 208, 0.6);
  box-shadow: 0 0 20px rgba(64, 224, 208, 0.3);
  transform: translateY(-2px);
}

.medal-item.earned:hover {
  transform: translateY(-4px);
  box-shadow: 0 0 30px rgba(64, 224, 208, 0.5);
}

/* 未获得勋章样式 - 半透明灰色 */
.medal-item.locked {
  opacity: 0.5;
  filter: grayscale(0.4);
}

/* 勋章图标 */
.medal-icon {
  font-size: 42px;
  transition: all 0.3s;
}

.medal-item.earned .medal-icon {
  font-size: 48px;
  filter: drop-shadow(0 0 8px rgba(255, 215, 0, 0.5));
  animation: medalGlow 2s ease-in-out infinite;
}

@keyframes medalGlow {
  0%, 100% { filter: drop-shadow(0 0 5px rgba(255, 215, 0, 0.3)); }
  50% { filter: drop-shadow(0 0 15px rgba(255, 215, 0, 0.7)); }
}

/* 勋章名称 - 白色 */
.medal-name {
  font-size: 14px;
  font-weight: 600;
  color: white;
  letter-spacing: 0.5px;
}

.medal-item.earned .medal-name {
  color: #40E0D0;
  text-shadow: 0 0 5px rgba(64, 224, 208, 0.5);
}

/* 勋章条件 - 白色半透明 */
.medal-condition {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
}

.medal-item.earned .medal-condition {
  color: rgba(255, 255, 255, 0.7);
}

@media (max-width: 768px) {
  .stats-header {
    flex-direction: column;
  }
  .stats-row {
    grid-template-columns: 1fr;
  }
  .reminder-area {
    flex-direction: row;
    justify-content: flex-end;
  }
  .form-row {
    flex-direction: column;
  }
  .task-right {
    flex-direction: column;
    align-items: flex-end;
    gap: 8px;
  }
  .medals-grid {
    grid-template-columns: repeat(auto-fill, minmax(110px, 1fr));
  }
  .medal-icon {
    font-size: 36px;
  }
  .medal-item.earned .medal-icon {
    font-size: 40px;
  }
}
</style>

<style>
.custom-select-dropdown-fixed {
  background: rgba(20, 20, 35, 0.95);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 16px;
  overflow-y: auto;
  max-height: 200px;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
}

.custom-select-dropdown-fixed .custom-select-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.8);
  transition: all 0.2s;
}

.custom-select-dropdown-fixed .custom-select-option:hover {
  background: rgba(64, 224, 208, 0.2);
  color: #40E0D0;
}

.custom-select-dropdown-fixed .custom-select-option.active {
  background: rgba(64, 224, 208, 0.15);
  color: #40E0D0;
}
</style>