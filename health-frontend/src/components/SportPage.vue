<template>
  <div class="sport-container">
    <!-- 顶部统计卡片 -->
    <div class="stats-row">
      <div class="stat-card glass">
        <div class="stat-value">{{ dailySport.totalCaloriesBurned || 0 }}</div>
        <div class="stat-label">今日消耗 (大卡)</div>
      </div>
      <div class="stat-card glass">
        <div class="stat-value">{{ statsData.totalCalories || 0 }}</div>
        <div class="stat-label">本周总消耗</div>
      </div>
      <div class="stat-card glass">
        <div class="stat-value">{{ statsData.avgCalories || 0 }}</div>
        <div class="stat-label">日均消耗</div>
      </div>
      <div class="stat-card glass">
        <div class="stat-value">{{ weeklyWorkoutCount }}</div>
        <div class="stat-label">本周运动次数</div>
      </div>
    </div>

    <!-- 添加运动记录 -->
    <div class="glass-card add-record-card">
      <div class="card-header">
        <span class="card-icon">🏃</span>
        <h4>添加运动记录</h4>
      </div>
      <div class="form-row three-columns">
        <div class="search-select" ref="searchSelectRef">
          <input
              type="text"
              v-model="sportForm.sportName"
              @input="onSportNameInput"
              @focus="showSportDropdown = true"
              placeholder="输入运动名称"
              class="glass-input"
          />
          <div v-if="showSportDropdown && filteredSportList.length > 0" class="dropdown-list">
            <div v-for="item in filteredSportList" :key="item.id" @click="selectSport(item)" class="dropdown-item">
              {{ item.name }}
            </div>
          </div>
        </div>
        <input
            v-model="sportForm.duration"
            placeholder="时长(分钟)"
            type="number"
            class="glass-input no-spinner"
            @input="calculateEstimatedCalories"
        />
        <input
            v-model="sportForm.calories"
            placeholder="消耗热量(大卡)"
            type="number"
            class="glass-input no-spinner"
            @input="onCaloriesManualInput"
        />
        <button class="glass-btn primary" @click="addSportRecord">添加运动</button>
      </div>
      <div v-if="estimatedCalories > 0 && !sportForm.calories" class="estimated-info glass">
        💡 预计消耗：<strong>{{ estimatedCalories }}</strong> 大卡
        <span class="calc-note">(基于体重 {{ userWeight }}kg 计算)</span>
      </div>
      <div v-if="sportForm.calories > 0 && sportForm.calories != estimatedCalories" class="estimated-info glass manual">
        ✏️ 自定义热量：<strong>{{ sportForm.calories }}</strong> 大卡
      </div>
    </div>

    <!-- 今日运动记录 -->
    <div class="glass-card">
      <div class="card-header">
        <span class="card-icon">📋</span>
        <h4>今日运动记录</h4>
        <button class="export-btn" @click="exportSportData">📤 导出Excel</button>
      </div>
      <div class="record-list">
        <div v-for="record in dailySport.records" :key="record.id" class="record-item" :class="{ 'editing': editingRecord && editingRecord.id === record.id }">
          <template v-if="!editingRecord || editingRecord.id !== record.id">
            <div class="record-info">
              <div class="record-name">{{ record.sportName }}</div>
              <div class="record-detail">
                <span>⏱️ {{ record.duration }} 分钟</span>
                <span>🔥 {{ record.caloriesBurned }} 大卡</span>
              </div>
            </div>
            <div class="record-actions">
              <button class="action-btn edit" @click="startEdit(record)">✏️ 修改</button>
              <button class="action-btn delete" @click="deleteSportRecord(record.id)">🗑️ 删除</button>
            </div>
          </template>
          <template v-else>
            <div class="record-info edit-mode">
              <div class="edit-field">
                <label>运动名称</label>
                <input v-model="editingRecord.sportName" class="glass-input-sm" />
              </div>
              <div class="edit-field">
                <label>时长(分钟)</label>
                <input v-model.number="editingRecord.duration" type="number" class="glass-input-sm no-spinner" @input="recalculateEditCalories" />
              </div>
              <div class="edit-field">
                <label>消耗热量</label>
                <input v-model.number="editingRecord.caloriesBurned" type="number" class="glass-input-sm no-spinner" />
              </div>
            </div>
            <div class="record-actions">
              <button class="action-btn save" @click="saveEdit">✓ 保存</button>
              <button class="action-btn cancel" @click="cancelEdit">✗ 取消</button>
            </div>
          </template>
        </div>
      </div>
      <p v-if="!dailySport.records || dailySport.records.length === 0" class="empty-state">暂无运动记录，添加一条吧 🏃</p>
    </div>

    <!-- ========== 本周运动消耗趋势（折线面积图）========== -->
    <div class="glass-card">
      <div class="card-header">
        <span class="card-icon">📊</span>
        <h4>本周运动消耗趋势</h4>
      </div>

      <!-- 有数据时显示图表 -->
      <div v-if="!statsLoading && hasChartData" class="chart-wrapper">
        <div id="sportTrendChart" class="trend-chart"></div>

        <!-- 每日消耗明细 -->
        <div class="daily-detail">
          <div class="detail-title">📋 每日消耗明细</div>
          <div class="detail-items">
            <div v-for="(item, index) in weekData" :key="index" class="detail-item" :class="{ 'today': item.isToday }">
              <span class="detail-weekday">{{ item.weekday }}</span>
              <span class="detail-date">{{ item.date }}</span>
              <div class="detail-bar">
                <div class="detail-bar-fill" :style="{ width: getBarWidth(item.calories) + '%' }"></div>
              </div>
              <span class="detail-value">{{ item.calories }} 大卡</span>
              <span v-if="item.isToday" class="today-badge">今日</span>
            </div>
          </div>
        </div>

        <!-- 目标进度 -->
        <div class="goal-progress" v-if="weeklyGoal > 0">
          <div class="goal-header">
            <span>🎯 本周目标 {{ weeklyGoal }} 大卡</span>
            <span>{{ goalProgress }}% 完成</span>
          </div>
          <div class="goal-bar">
            <div class="goal-fill" :style="{ width: goalProgress + '%' }"></div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!statsLoading && !hasChartData" class="empty-chart">
        <div class="empty-icon">📭</div>
        <p>本周还没有运动记录</p>
        <p class="empty-hint">添加运动，开始你的健康之旅 🏃</p>
      </div>

      <!-- 加载中 -->
      <div v-else class="loading-chart">加载中...</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'
import eventBus from '@/utils/eventBus'

const props = defineProps({ userId: Number })
const API_BASE = 'http://localhost:8080/api'

// 用户体重
const userWeight = ref(65)

const dailySport = ref({ records: [], totalCaloriesBurned: 0 })
const sportLibrary = ref([])
const sportSearchText = ref('')
const showSportDropdown = ref(false)
const filteredSportList = ref([])
const sportForm = ref({ sportName: '', duration: '', calories: '' })
const editingRecord = ref(null)
const estimatedCalories = ref(0)
const statsData = ref({ dates: [], calories: [], totalCalories: 0, avgCalories: 0 })
const statsLoading = ref(false)
let trendChart = null
let retryCount = 0
const searchSelectRef = ref(null)

// 本周数据
const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const weekData = ref([])
const weeklyGoal = ref(2000)

// 计算属性
const weeklyWorkoutCount = computed(() => statsData.value.calories?.filter(c => c > 0).length || 0)
const hasChartData = computed(() => weekData.value.some(d => d.calories > 0))
const goalProgress = computed(() => {
  const total = statsData.value.totalCalories || 0
  return Math.min(100, Math.round((total / weeklyGoal.value) * 100))
})

// 运动热量估算表（MET 值）
const sportMetMap = {
  '跑步': 8.0, '慢跑': 7.0, '快跑': 10.0, '散步': 3.5, '快走': 5.0,
  '跳绳': 10.0, '游泳': 7.0, '骑行': 6.0, '瑜伽': 3.0, '深蹲': 5.0,
  '俯卧撑': 4.0, '引体向上': 5.0, '篮球': 7.0, '足球': 8.0, '羽毛球': 6.0,
  '帕梅拉': 5.0, '拉伸': 4.5, '仰卧起坐': 4.0, '平板支撑': 3.5,
  '波比跳': 9.0, '开合跳': 8.0, '高抬腿': 8.0, '战绳': 9.0, '壶铃': 7.0
}

// 计算每分钟消耗 - 优先使用数据库的 met_value
const caloriesPerMinute = computed(() => {
  if (!sportForm.value.sportName) return 0
  const sportName = sportForm.value.sportName

  const librarySport = sportLibrary.value.find(s => s.name === sportName)
  if (librarySport && librarySport.metValue) {
    return Math.round(librarySport.metValue * userWeight.value * 0.0175)
  }

  if (librarySport && librarySport.unit && librarySport.unit.includes('分钟')) {
    const baseMinutes = parseInt(librarySport.unit)
    if (baseMinutes > 0 && librarySport.calories > 0) {
      return Math.round(librarySport.calories / baseMinutes)
    }
  }

  let met = 5.0
  for (const [key, value] of Object.entries(sportMetMap)) {
    if (sportName.includes(key)) { met = value; break }
  }
  return Math.round(met * userWeight.value * 0.0175)
})

// ========== 触发数据更新事件 ==========
const emitSportDataChange = () => {
  eventBus.emit('sport-data-changed')
}

const calculateEstimatedCalories = () => {
  if (!sportForm.value.sportName || !sportForm.value.duration) {
    estimatedCalories.value = 0
    return
  }
  const minutes = parseFloat(sportForm.value.duration)
  if (isNaN(minutes) || minutes <= 0) {
    estimatedCalories.value = 0
    return
  }
  estimatedCalories.value = Math.round(caloriesPerMinute.value * minutes)
}

const onCaloriesManualInput = () => {
  if (sportForm.value.calories) {
    estimatedCalories.value = 0
  } else {
    calculateEstimatedCalories()
  }
}

const recalculateEditCalories = () => {
  if (!editingRecord.value) return
  const minutes = editingRecord.value.duration
  const sportName = editingRecord.value.sportName

  const librarySport = sportLibrary.value.find(s => s.name === sportName)
  let met = 5.0

  if (librarySport && librarySport.metValue) {
    met = librarySport.metValue
  } else {
    for (const [key, value] of Object.entries(sportMetMap)) {
      if (sportName.includes(key)) { met = value; break }
    }
  }

  const perMinute = Math.round(met * userWeight.value * 0.0175)
  editingRecord.value.caloriesBurned = Math.round(perMinute * minutes)
}

// 加载数据
const loadUserWeight = async () => {
  if (!props.userId) return
  try {
    const res = await axios.get(`${API_BASE}/user/${props.userId}`)
    if (res.data && res.data.currentWeight) userWeight.value = res.data.currentWeight
  } catch (e) { console.error(e) }
}

const loadSportLibrary = async () => {
  try {
    const res = await axios.get(`${API_BASE}/admin/library/type/SPORT`)
    sportLibrary.value = res.data
    filteredSportList.value = res.data
  } catch (e) { console.error(e) }
}

const loadDailySport = async () => {
  if (!props.userId) return
  try {
    const res = await axios.get(`${API_BASE}/sport/daily/${props.userId}`)
    dailySport.value = res.data
  } catch (e) { console.error(e) }
}

const loadSportStats = async () => {
  if (!props.userId) return
  statsLoading.value = true
  retryCount = 0
  try {
    const res = await axios.get(`${API_BASE}/sport/stats/${props.userId}`, { params: { period: 'week' } })
    statsData.value = res.data
    buildWeekData()
    await nextTick()
    renderTrendChart()
  } catch (e) {
    console.error('加载统计数据失败:', e)
  }
  finally { statsLoading.value = false }
}

const buildWeekData = () => {
  const today = new Date()
  const dayOfWeek = today.getDay()
  const mondayOffset = dayOfWeek === 0 ? -6 : -(dayOfWeek - 1)

  const weekDates = []
  for (let i = 0; i < 7; i++) {
    const date = new Date(today)
    date.setDate(today.getDate() + mondayOffset + i)
    weekDates.push(date.toISOString().slice(0, 10))
  }

  const caloriesMap = {}
  if (statsData.value.dates && statsData.value.calories) {
    for (let i = 0; i < statsData.value.dates.length; i++) {
      caloriesMap[statsData.value.dates[i]] = statsData.value.calories[i]
    }
  }

  weekData.value = weekDates.map((date, index) => {
    const dateObj = new Date(date)
    return {
      date: `${dateObj.getMonth() + 1}/${dateObj.getDate()}`,
      weekday: weekDays[index],
      calories: caloriesMap[date] || 0,
      isToday: date === today.toISOString().slice(0, 10),
      fullDate: date,
      percent: 0
    }
  })

  const maxCalories = Math.max(...weekData.value.map(d => d.calories), 1)
  weekData.value.forEach(item => {
    item.percent = Math.min(100, Math.round((item.calories / maxCalories) * 100))
  })
}

const renderTrendChart = () => {
  if (!hasChartData.value) {
    console.log('本周无运动数据，跳过图表渲染')
    return
  }

  const chartDom = document.getElementById('sportTrendChart')

  if (!chartDom) {
    if (retryCount < 10) {
      retryCount++
      console.log(`图表容器未找到，第${retryCount}次重试...`)
      setTimeout(() => renderTrendChart(), 200)
    } else {
      console.error('图表容器未找到，放弃重试')
    }
    return
  }

  if (!chartDom.offsetParent) {
    console.log('图表容器不可见，等待...')
    setTimeout(() => renderTrendChart(), 200)
    return
  }

  if (trendChart) trendChart.dispose()

  const dates = weekData.value.map(d => `${d.weekday}\n${d.date}`)
  const calories = weekData.value.map(d => d.calories)
  const todayIndex = weekData.value.findIndex(d => d.isToday)

  trendChart = echarts.init(chartDom)
  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => {
        const data = params[0]
        const dayData = weekData.value[data.dataIndex]
        return `${dayData.weekday} ${dayData.date}<br/>🔥 消耗热量: ${data.value} 大卡`
      }
    },
    grid: {
      top: '10%',
      left: '8%',
      right: '5%',
      bottom: '8%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        fontSize: 11,
        color: (value, index) => index === todayIndex ? '#40E0D0' : 'rgba(255,255,255,0.6)'
      }
    },
    yAxis: {
      type: 'value',
      name: '大卡',
      nameTextStyle: { color: 'rgba(255,255,255,0.5)', fontSize: 11 },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } },
      axisLabel: { color: 'rgba(255,255,255,0.5)' }
    },
    series: [{
      data: calories,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { width: 3, color: '#40E0D0' },
      areaStyle: {
        opacity: 0.3,
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#40E0D0' },
            { offset: 1, color: 'rgba(64,224,208,0)' }
          ]
        }
      },
      itemStyle: {
        color: (params) => params.dataIndex === todayIndex ? '#F59E0B' : '#40E0D0',
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: true,
        position: 'top',
        formatter: '{c}',
        fontSize: 10,
        color: (params) => params.dataIndex === todayIndex ? '#F59E0B' : '#40E0D0'
      }
    }]
  })

  console.log('图表渲染成功')
}

const getBarWidth = (calories) => {
  const max = Math.max(...weekData.value.map(d => d.calories), 1)
  return Math.min(100, (calories / max) * 100)
}

const onSportNameInput = () => {
  sportSearchText.value = sportForm.value.sportName
  filterSportList()
  calculateEstimatedCalories()
}

const filterSportList = () => {
  if (!sportSearchText.value) {
    filteredSportList.value = sportLibrary.value
  } else {
    filteredSportList.value = sportLibrary.value.filter(item =>
        item.name.toLowerCase().includes(sportSearchText.value.toLowerCase())
    )
  }
  showSportDropdown.value = true
}

const selectSport = (sport) => {
  sportForm.value.sportName = sport.name
  sportSearchText.value = sport.name
  showSportDropdown.value = false
  calculateEstimatedCalories()
}

// ========== 添加运动记录（带输入校验） ==========
const addSportRecord = async () => {
  if (!sportForm.value.sportName || sportForm.value.sportName.trim() === '') {
    alert('请输入运动名称')
    return
  }

  let duration = parseFloat(sportForm.value.duration)
  if (isNaN(duration) || duration <= 0) {
    alert('请填写有效的时长（分钟），必须大于0')
    return
  }
  if (duration > 480) {
    alert('时长不能超过480分钟（8小时），请检查输入')
    return
  }

  let caloriesBurned = 0
  if (sportForm.value.calories && parseFloat(sportForm.value.calories) > 0) {
    caloriesBurned = parseFloat(sportForm.value.calories)
    if (caloriesBurned > 5000) {
      alert('热量不能超过5000大卡，请检查输入')
      return
    }
  } else {
    caloriesBurned = Math.round(caloriesPerMinute.value * duration)
    if (caloriesBurned <= 0) {
      caloriesBurned = Math.round(5 * userWeight.value * 0.0175 * duration)
    }
  }

  const today = new Date().toISOString().split('T')[0]

  try {
    const response = await axios.post(`${API_BASE}/sport/add`, null, {
      params: {
        userId: props.userId,
        sportName: sportForm.value.sportName.trim(),
        duration: Math.round(duration),
        caloriesBurned: Math.round(caloriesBurned),
        recordDate: today
      }
    })

    if (response.data.success) {
      alert('添加成功')
      sportForm.value = { sportName: '', duration: '', calories: '' }
      sportSearchText.value = ''
      estimatedCalories.value = 0
      showSportDropdown.value = false

      await loadDailySport()
      await loadSportStats()

      // 触发全局事件，通知健康数据页面刷新
      emitSportDataChange()
    } else {
      alert(response.data.message || '添加失败')
    }
  } catch (e) {
    console.error('添加失败', e)
    alert('添加失败: ' + (e.response?.data?.message || e.message))
  }
}

const startEdit = (record) => {
  editingRecord.value = { ...record }
}

const cancelEdit = () => {
  editingRecord.value = null
}

// ========== 保存修改（带输入校验） ==========
const saveEdit = async () => {
  if (!editingRecord.value) return

  if (!editingRecord.value.sportName || editingRecord.value.sportName.trim() === '') {
    alert('运动名称不能为空')
    return
  }

  let duration = parseFloat(editingRecord.value.duration)
  if (isNaN(duration) || duration <= 0) {
    alert('请填写有效的时长（分钟），必须大于0')
    return
  }
  if (duration > 480) {
    alert('时长不能超过480分钟（8小时）')
    return
  }

  let calories = parseFloat(editingRecord.value.caloriesBurned)
  if (isNaN(calories) || calories <= 0) {
    alert('请填写有效的热量值，必须大于0')
    return
  }
  if (calories > 5000) {
    alert('热量不能超过5000大卡')
    return
  }

  try {
    await axios.put(`${API_BASE}/sport/update/${editingRecord.value.id}`, null, {
      params: {
        userId: props.userId,
        sportName: editingRecord.value.sportName.trim(),
        duration: Math.round(duration),
        caloriesBurned: Math.round(calories)
      }
    })
    alert('修改成功')
    editingRecord.value = null
    await loadDailySport()
    await loadSportStats()

    // 触发全局事件，通知健康数据页面刷新
    emitSportDataChange()
  } catch (e) {
    console.error('修改失败', e)
    alert('修改失败: ' + (e.response?.data?.message || e.message))
  }
}

const deleteSportRecord = async (recordId) => {
  if (!confirm('确定删除吗？')) return
  try {
    await axios.delete(`${API_BASE}/sport/delete/${recordId}`, { params: { userId: props.userId } })
    alert('删除成功')
    await loadDailySport()
    await loadSportStats()

    // 触发全局事件，通知健康数据页面刷新
    emitSportDataChange()
  } catch (e) {
    console.error('删除失败', e)
    alert('删除失败: ' + (e.response?.data?.message || e.message))
  }
}

const exportSportData = () => window.open(`${API_BASE}/export/sport/${props.userId}`)

const handleResize = () => trendChart?.resize()

watch(() => props.userId, (newVal) => { if (newVal) { loadDailySport(); loadSportStats(); loadUserWeight() } })

onMounted(() => {
  loadSportLibrary()
  if (props.userId) { loadDailySport(); loadSportStats(); loadUserWeight() }

  document.addEventListener('click', (e) => {
    if (searchSelectRef.value && !searchSelectRef.value.contains(e.target)) {
      showSportDropdown.value = false
    }
  })

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (trendChart) trendChart.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.sport-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.glass-card {
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 24px;
  overflow: visible;
  position: relative;
}

.add-record-card {
  position: relative;
  z-index: 1000;
}

.glass {
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
  border-radius: 20px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #40E0D0;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.card-icon {
  font-size: 24px;
}

.card-header h4 {
  font-size: 18px;
  font-weight: 600;
  color: white;
  margin: 0;
  flex: 1;
}

.form-row.three-columns {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr auto;
  gap: 16px;
  align-items: center;
}

@media (max-width: 768px) {
  .form-row.three-columns {
    grid-template-columns: 1fr;
  }
}

.glass-input {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 40px;
  padding: 12px 20px;
  color: white;
  font-size: 14px;
  outline: none;
  width: 100%;
  transition: all 0.2s;
}

.glass-input:focus {
  border-color: #40E0D0;
}

.no-spinner::-webkit-inner-spin-button,
.no-spinner::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.no-spinner {
  -moz-appearance: textfield;
  appearance: textfield;
}

.search-select {
  position: relative;
  width: 100%;
  overflow: visible;
}

.dropdown-list {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  max-height: 200px;
  overflow-y: auto;
  border-radius: 16px;
  z-index: 100000;
  margin-top: 8px;
  background: rgba(20, 20, 35, 0.95);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.3);
}

.dropdown-list::-webkit-scrollbar {
  width: 6px;
}

.dropdown-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.08);
  border-radius: 3px;
}

.dropdown-list::-webkit-scrollbar-thumb {
  background: rgba(64, 224, 208, 0.5);
  border-radius: 3px;
}

.dropdown-list::-webkit-scrollbar-thumb:hover {
  background: #40E0D0;
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
  background: rgba(64, 224, 208, 0.6);
}

.dropdown-item {
  padding: 12px 16px;
  cursor: pointer;
  color: white !important;
  font-size: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.2s;
}

.dropdown-item:hover {
  background: rgba(64, 224, 208, 0.2);
  color: #40E0D0 !important;
}

.dropdown-item:last-child {
  border-bottom: none;
}

.estimated-info {
  margin-top: 16px;
  padding: 12px;
  border-radius: 12px;
  font-size: 13px;
  color: #40E0D0;
}

.calc-note {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  margin-left: 8px;
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

.glass-btn.primary:hover {
  background: rgba(64, 224, 208, 0.35);
  transform: translateY(-2px);
}

.export-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 40px;
  padding: 6px 16px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  font-size: 12px;
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.record-item.editing {
  background: rgba(64, 224, 208, 0.1);
  border-color: rgba(64, 224, 208, 0.3);
}

.record-info {
  flex: 1;
}

.record-name {
  font-size: 16px;
  font-weight: 500;
  color: white;
  margin-bottom: 6px;
}

.record-detail {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.record-detail span:last-child {
  color: #40E0D0;
}

.edit-mode {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.edit-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.edit-field label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.glass-input-sm {
  background: rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  padding: 6px 12px;
  color: white;
  font-size: 13px;
  width: 120px;
}

.glass-input-sm.no-spinner::-webkit-inner-spin-button,
.glass-input-sm.no-spinner::-webkit-outer-spin-button {
  -webkit-appearance: none;
  margin: 0;
}

.record-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  border: none;
  transition: all 0.2s;
}

.action-btn.edit {
  background: rgba(64, 224, 208, 0.15);
  color: #40E0D0;
}

.action-btn.delete {
  background: rgba(229, 115, 115, 0.15);
  color: #ff8888;
}

.action-btn.save {
  background: rgba(76, 175, 80, 0.2);
  color: #4caf50;
}

.action-btn.cancel {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
}

.chart-wrapper {
  width: 100%;
}

.trend-chart {
  width: 100%;
  height: 320px;
}

.daily-detail {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.detail-title {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 16px;
}

.detail-items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 12px;
}

.detail-item.today {
  background: rgba(64, 224, 208, 0.08);
  border: 1px solid rgba(64, 224, 208, 0.2);
}

.detail-weekday {
  width: 36px;
  font-size: 12px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.7);
}

.detail-date {
  width: 40px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.detail-bar {
  flex: 1;
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
  overflow: hidden;
}

.detail-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #40E0D0, #2BA0D0);
  border-radius: 3px;
}

.detail-value {
  width: 60px;
  text-align: right;
  font-size: 12px;
  color: #40E0D0;
}

.today-badge {
  font-size: 10px;
  padding: 2px 8px;
  background: rgba(64, 224, 208, 0.2);
  border-radius: 20px;
  color: #40E0D0;
}

.goal-progress {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.goal-header {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 8px;
}

.goal-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 3px;
  overflow: hidden;
}

.goal-fill {
  height: 100%;
  background: linear-gradient(90deg, #F59E0B, #40E0D0);
  border-radius: 3px;
}

.empty-chart, .loading-chart {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-chart p {
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
  margin: 8px 0;
}

.empty-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
}

.empty-state, .loading-state {
  text-align: center;
  padding: 40px;
  color: rgba(255, 255, 255, 0.4);
  font-size: 14px;
}
</style>