<template>
  <div class="health-analysis-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>健康分析</h1>
      <p>基于你的数据，提供个性化建议</p>
    </div>

    <!-- 日期筛选 -->
    <div class="period-filter">
      <button
          v-for="p in periods"
          :key="p.value"
          :class="{ active: selectedPeriod === p.value }"
          @click="switchPeriod(p.value)"
      >
        {{ p.label }}
      </button>
      <button
          :class="{ active: selectedPeriod === 'custom' }"
          @click="openCustomDatePicker"
      >
        自定义
      </button>
    </div>

    <!-- 自定义日期选择器 - 毛玻璃风格 -->
    <div v-if="showCustomDate" class="custom-date-panel glass-card">
      <div class="custom-date-header">
        <span class="panel-icon">📅</span>
        <span class="panel-title">自定义日期范围</span>
        <button class="panel-close" @click="closeCustomDatePicker">✕</button>
      </div>
      <div class="custom-date-body">
        <div class="date-input-group">
          <label>开始日期</label>
          <input type="date" v-model="customStartDate" class="glass-input" />
        </div>
        <div class="date-arrow">→</div>
        <div class="date-input-group">
          <label>结束日期</label>
          <input type="date" v-model="customEndDate" class="glass-input" />
        </div>
      </div>
      <div class="custom-date-footer">
        <button class="btn-secondary" @click="closeCustomDatePicker">取消</button>
        <button class="btn-primary" @click="loadCustomReport" :disabled="loading">查询</button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载数据中...</p>
    </div>

    <div v-else>
      <!-- 健康综合评分卡片 -->
      <div class="glass-card score-card">
        <div class="score-header">
          <span class="score-icon">🏆</span>
          <h4>健康综合评分</h4>
        </div>
        <div class="score-main">
          <div class="score-circle">
            <svg width="120" height="120" viewBox="0 0 120 120">
              <circle cx="60" cy="60" r="54" fill="none" stroke="rgba(255,255,255,0.1)" stroke-width="8"/>
              <circle cx="60" cy="60" r="54" fill="none" stroke="#40E0D0" stroke-width="8"
                      :stroke-dasharray="339.3" :stroke-dashoffset="339.3 - (339.3 * (healthScore.totalScore || 0) / 100)"
                      stroke-linecap="round" transform="rotate(-90 60 60)"/>
            </svg>
            <div class="score-number">{{ healthScore.totalScore || 0 }}</div>
            <div class="score-level">{{ healthScore.level || '待加强' }}</div>
          </div>
          <div class="score-details">
            <div class="score-item">
              <span class="score-label">BMI</span>
              <div class="score-bar-bg"><div class="score-bar" :style="{ width: (healthScore.details?.bmiScore || 0) + '%', maxWidth: '25%' }"></div></div>
              <span class="score-value">{{ healthScore.details?.bmiScore || 0 }}/25</span>
            </div>
            <div class="score-item">
              <span class="score-label">运动</span>
              <div class="score-bar-bg"><div class="score-bar" :style="{ width: (healthScore.details?.sportScore || 0) + '%', maxWidth: '25%' }"></div></div>
              <span class="score-value">{{ healthScore.details?.sportScore || 0 }}/25</span>
            </div>
            <div class="score-item">
              <span class="score-label">饮食</span>
              <div class="score-bar-bg"><div class="score-bar" :style="{ width: (healthScore.details?.dietScore || 0) + '%', maxWidth: '20%' }"></div></div>
              <span class="score-value">{{ healthScore.details?.dietScore || 0 }}/20</span>
            </div>
            <div class="score-item">
              <span class="score-label">睡眠</span>
              <div class="score-bar-bg"><div class="score-bar" :style="{ width: (healthScore.details?.sleepScore || 0) + '%', maxWidth: '15%' }"></div></div>
              <span class="score-value">{{ healthScore.details?.sleepScore || 0 }}/15</span>
            </div>
            <div class="score-item">
              <span class="score-label">打卡</span>
              <div class="score-bar-bg"><div class="score-bar" :style="{ width: (healthScore.details?.checkScore || 0) + '%', maxWidth: '15%' }"></div></div>
              <span class="score-value">{{ healthScore.details?.checkScore || 0 }}/15</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 核心指标卡片（4个） -->
      <div class="core-metrics">
        <div class="core-card glass-card">
          <div class="core-icon">📊</div>
          <div class="core-value">{{ bmi }}</div>
          <div class="core-label">BMI · {{ bmiStatus }}</div>
        </div>
        <div class="core-card glass-card">
          <div class="core-icon">⚖️</div>
          <div class="core-value">{{ latestWeight || '--' }}</div>
          <div class="core-label">体重(kg)</div>
          <div class="core-change" :class="weightChangeClass">{{ weightChangeText }}</div>
        </div>
        <div class="core-card glass-card">
          <div class="core-icon">🏃</div>
          <div class="core-value">{{ sportAnalysis.weeklyCount || 0 }}</div>
          <div class="core-label">本周运动(次)</div>
          <div class="core-change" :class="sportChangeClass">{{ sportChangeText }}</div>
        </div>
        <div class="core-card glass-card">
          <div class="core-icon">🔥</div>
          <div class="core-value">{{ calorieBalance.netBalance || 0 }}</div>
          <div class="core-label">热量平衡</div>
          <div class="core-status">{{ calorieBalance.status || '平衡' }}</div>
        </div>
      </div>

      <!-- 热量平衡详情卡片 -->
      <div class="glass-card calorie-card">
        <div class="card-header">
          <span class="card-icon">🔥</span>
          <h4>本周热量平衡</h4>
        </div>
        <div class="balance-bars">
          <div class="balance-item">
            <div class="balance-label">🍽️ 饮食摄入</div>
            <div class="balance-bar-bg">
              <div class="balance-bar intake-bar" :style="{ width: intakePercent + '%' }"></div>
            </div>
            <div class="balance-value">{{ calorieBalance.totalIntake || 0 }} 大卡</div>
          </div>
          <div class="balance-item">
            <div class="balance-label">🏃 运动消耗</div>
            <div class="balance-bar-bg">
              <div class="balance-bar burn-bar" :style="{ width: burnPercent + '%' }"></div>
            </div>
            <div class="balance-value">{{ calorieBalance.totalBurn || 0 }} 大卡</div>
          </div>
        </div>
        <div class="balance-result" :class="balanceResultClass">
          💡 {{ balanceAdvice }}
        </div>
        <div class="balance-analogy" v-if="calorieBalance.netBalance !== 0">
          <span class="analogy-icon">🍎</span>
          <span class="analogy-text">{{ calorieAnalogy }}</span>
        </div>
      </div>

      <!-- 运动分析和营养分析两列 -->
      <div class="two-cols">
        <!-- 运动分析卡片 -->
        <div class="glass-card">
          <div class="card-header">
            <span class="card-icon">🏃</span>
            <h4>运动分析</h4>
          </div>
          <div class="stat-row">
            <div class="stat-item">
              <div class="stat-value-large">{{ sportAnalysis.weeklyCount || 0 }}</div>
              <div class="stat-label">本周运动次数</div>
              <div class="stat-trend" :class="sportChangeClass">{{ sportChangeText }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-value-large">{{ sportAnalysis.totalCalories || 0 }}</div>
              <div class="stat-label">总消耗(大卡)</div>
              <div class="stat-trend" :class="sportCalChangeClass">{{ sportCalChangeText }}</div>
            </div>
          </div>
          <div class="stat-detail">
            <span>🏆 最爱运动：{{ sportAnalysis.favoriteSport || '暂无' }}</span>
          </div>
        </div>

        <!-- 营养分析卡片 -->
        <div class="glass-card">
          <div class="card-header">
            <span class="card-icon">🥗</span>
            <h4>本周营养分析</h4>
            <span class="date-hint">({{ periodText }})</span>
          </div>
          <div class="nutrition-pie">
            <div class="pie-item">
              <div class="pie-bar protein" :style="{ width: nutritionAnalysis.proteinPercent + '%' }"></div>
              <span>蛋白质</span>
              <span class="nutrition-gram">{{ nutritionAnalysis.proteinPercent }}% ({{ nutritionAnalysis.protein }}g)</span>
            </div>
            <div class="pie-item">
              <div class="pie-bar fat" :style="{ width: nutritionAnalysis.fatPercent + '%' }"></div>
              <span>脂肪</span>
              <span class="nutrition-gram">{{ nutritionAnalysis.fatPercent }}% ({{ nutritionAnalysis.fat }}g)</span>
            </div>
            <div class="pie-item">
              <div class="pie-bar carbs" :style="{ width: nutritionAnalysis.carbsPercent + '%' }"></div>
              <span>碳水</span>
              <span class="nutrition-gram">{{ nutritionAnalysis.carbsPercent }}% ({{ nutritionAnalysis.carbs }}g)</span>
            </div>
          </div>
          <div class="stat-detail">
            <span>🌾 膳食纤维：{{ nutritionAnalysis.fiber || 0 }}g</span>
            <span>⚡ 总热量：{{ Math.round((nutritionAnalysis.protein || 0) * 4 + (nutritionAnalysis.fat || 0) * 9 + (nutritionAnalysis.carbs || 0) * 4) }} 大卡</span>
          </div>
        </div>
      </div>

      <!-- 智能建议卡片 -->
      <div class="glass-card insights-card">
        <div class="card-header">
          <span class="card-icon">💡</span>
          <h4>智能建议</h4>
        </div>
        <div class="insights-list">
          <div v-for="(insight, idx) in smartInsights" :key="idx" class="insight-item">
            {{ insight }}
          </div>
          <div v-if="smartInsights.length === 0" class="insight-item">暂无建议，继续记录数据吧~</div>
        </div>
      </div>

      <!-- 体重 vs 运动关联趋势图 -->
      <div class="glass-card chart-card">
        <div class="chart-header">
          <span class="chart-title">📉 体重 vs 运动消耗</span>
          <span class="chart-unit">(过去14天)</span>
        </div>
        <canvas ref="correlationChartCanvas" class="chart-canvas"></canvas>
      </div>

      <!-- BMI趋势图 -->
      <div class="glass-card chart-card">
        <div class="chart-header">
          <span class="chart-title">📈 BMI趋势</span>
          <span class="chart-unit">(过去{{ periodText }})</span>
        </div>
        <canvas ref="bmiChartCanvas" class="chart-canvas"></canvas>
      </div>

      <!-- 健康报告 -->
      <div class="glass-card">
        <div class="report-header">
          <h4>📈 健康报告</h4>
          <div class="report-actions">
            <div class="quick-periods">
              <button class="quick-period-btn" @click="setQuickPeriod('week')">最近7天</button>
              <button class="quick-period-btn" @click="setQuickPeriod('month')">最近30天</button>
              <button class="quick-period-btn" @click="setQuickPeriod('thisMonth')">本月</button>
            </div>
            <input type="date" v-model="startDate" class="glass-input date-input" />
            <span>至</span>
            <input type="date" v-model="endDate" class="glass-input date-input" />
            <button @click="loadReport" class="btn-primary" :disabled="reportLoading">生成</button>
          </div>
        </div>

        <div v-if="report" class="report-content">
          <div class="report-date">{{ report.startDate }} 至 {{ report.endDate }}</div>
          <div class="stats">
            <div class="stat">
              <div class="stat-label">体重变化</div>
              <div class="stat-value" :style="{color: report.weightChange <= 0 ? '#7CB342' : '#E57373'}">
                {{ report.weightChange > 0 ? '+' : '' }}{{ report.weightChange }} kg
              </div>
            </div>
            <div class="stat">
              <div class="stat-label">平均睡眠</div>
              <div class="stat-value">{{ report.avgSleep }} h</div>
            </div>
            <div class="stat">
              <div class="stat-label">总饮水量</div>
              <div class="stat-value">{{ report.totalWater }} ml</div>
            </div>
          </div>
          <div class="advice">💡 {{ report.advice }}</div>
        </div>
        <div v-else class="empty-report">
          <span>📋</span>
          <p>选择日期范围后点击「生成」查看健康分析</p>
        </div>
      </div>

      <!-- 健康小贴士 -->
      <div class="glass-card">
        <h4>💡 健康小贴士</h4>
        <div v-for="(tip, i) in displayTips" :key="i" class="tip">✨ {{ tip }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import axios from 'axios'
import Chart from 'chart.js/auto'

const props = defineProps({ userId: { type: Number, required: true } })
const API_BASE = 'http://localhost:8080/api'

// 数据
const loading = ref(false)
const reportLoading = ref(false)
const report = ref(null)
const bmi = ref(19.1)
const selectedPeriod = ref('week')
const showCustomDate = ref(false)
const customStartDate = ref('')
const customEndDate = ref('')

// 健康报告的日期范围
const startDate = ref('')
const endDate = ref('')

// 最新指标
const latestWeight = ref(null)
const latestHeight = ref(null)
const latestHeartRate = ref(null)
const latestBloodPressure = ref(null)

// 趋势数据
const trendData = ref({ dates: [], bmiTrend: [] })

// 新增分析数据
const healthScore = ref({ totalScore: 0, level: '待加强', details: {} })
const calorieBalance = ref({ totalIntake: 0, totalBurn: 0, netBalance: 0, status: '平衡' })
const sportAnalysis = ref({ weeklyCount: 0, weeklyChange: 0, totalCalories: 0, caloriesChange: 0, favoriteSport: '' })
const nutritionAnalysis = ref({ protein: 0, fat: 0, carbs: 0, fiber: 0, proteinPercent: 0, fatPercent: 0, carbsPercent: 0 })
const smartInsights = ref([])
const correlationData = ref({ dates: [], weights: [], calories: [] })

// 图表实例
let bmiChart = null
let correlationChart = null
let retryTimer = null

const bmiChartCanvas = ref(null)
const correlationChartCanvas = ref(null)

const periods = [
  { value: 'week', label: '近一周' },
  { value: 'month', label: '近一月' },
  { value: 'year', label: '近一年' }
]

const periodText = computed(() => {
  const map = { week: '7天', month: '30天', year: '365天' }
  return map[selectedPeriod.value] || '30天'
})

// BMI 计算
const bmiStatus = computed(() => {
  if (bmi.value < 18.5) return '偏瘦'
  if (bmi.value < 24) return '正常'
  if (bmi.value < 28) return '偏重'
  return '肥胖'
})

// 热量平衡计算
const maxBalance = computed(() => Math.max(calorieBalance.value.totalIntake, calorieBalance.value.totalBurn, 1))
const intakePercent = computed(() => (calorieBalance.value.totalIntake / maxBalance.value) * 100)
const burnPercent = computed(() => (calorieBalance.value.totalBurn / maxBalance.value) * 100)

const balanceResultClass = computed(() => {
  const net = calorieBalance.value.netBalance
  if (net < 0) return 'deficit'
  if (net > 500) return 'surplus'
  return 'balance'
})

const balanceAdvice = computed(() => {
  const net = calorieBalance.value.netBalance
  if (net < -500) return `热量赤字 ${-net} 大卡，减脂效果明显，注意不要过度节食`
  if (net < 0) return `热量赤字 ${-net} 大卡，温和减脂中`
  if (net <= 100) return '热量平衡，体重维持良好'
  if (net <= 500) return `热量盈余 ${net} 大卡，轻微增重`
  return `热量盈余 ${net} 大卡，建议增加运动或减少高热量食物`
})

const calorieAnalogy = computed(() => {
  const net = calorieBalance.value.netBalance
  const absNet = Math.abs(net)
  if (net < 0) {
    if (absNet < 100) return `🎉 热量赤字 ${absNet} 大卡，相当于慢跑 10 分钟`
    if (absNet < 200) return `🎉 热量赤字 ${absNet} 大卡，相当于慢跑 25 分钟`
    if (absNet < 300) return `🎉 热量赤字 ${absNet} 大卡，相当于慢跑 40 分钟`
    if (absNet < 500) return `🎉 热量赤字 ${absNet} 大卡，相当于慢跑 60 分钟`
    return `🎉 热量赤字 ${absNet} 大卡，减脂效果明显！`
  } else if (net > 0) {
    if (absNet < 100) return `⚠️ 热量盈余 ${absNet} 大卡，≈ 半个苹果`
    if (absNet < 200) return `⚠️ 热量盈余 ${absNet} 大卡，≈ 1个苹果`
    if (absNet < 300) return `⚠️ 热量盈余 ${absNet} 大卡，≈ 1碗米饭`
    if (absNet < 500) return `⚠️ 热量盈余 ${absNet} 大卡，≈ 1个汉堡`
    return `⚠️ 热量盈余 ${absNet} 大卡，建议增加运动`
  }
  return `⚖️ 热量平衡，体重维持良好`
})

const weightChangeText = computed(() => '')
const weightChangeClass = computed(() => '')

const sportChangeText = computed(() => {
  const change = sportAnalysis.value.weeklyChange
  if (change > 0) return `+${change}`
  if (change < 0) return change.toString()
  return '持平'
})
const sportChangeClass = computed(() => {
  const change = sportAnalysis.value.weeklyChange
  if (change > 0) return 'positive'
  if (change < 0) return 'negative'
  return 'neutral'
})

const sportCalChangeText = computed(() => {
  const change = sportAnalysis.value.caloriesChange
  if (change > 0) return `+${change} 大卡`
  if (change < 0) return `${change} 大卡`
  return '持平'
})
const sportCalChangeClass = computed(() => {
  const change = sportAnalysis.value.caloriesChange
  if (change > 0) return 'positive'
  if (change < 0) return 'negative'
  return 'neutral'
})

const displayTips = ref([
  '早起喝一杯温水，促进新陈代谢',
  '每餐七分饱，细嚼慢咽',
  '保证7-8小时睡眠',
  '每周称重一次，记录变化'
])

const setQuickPeriod = (period) => {
  const today = new Date()
  let start = new Date()
  if (period === 'week') {
    start.setDate(today.getDate() - 7)
  } else if (period === 'month') {
    start.setDate(today.getDate() - 30)
  } else if (period === 'thisMonth') {
    start = new Date(today.getFullYear(), today.getMonth(), 1)
  }
  startDate.value = start.toISOString().split('T')[0]
  endDate.value = today.toISOString().split('T')[0]
  loadReport()
}

const setDefaultDateRange = () => {
  const today = new Date()
  const sevenDaysAgo = new Date()
  sevenDaysAgo.setDate(today.getDate() - 7)
  endDate.value = today.toISOString().split('T')[0]
  startDate.value = sevenDaysAgo.toISOString().split('T')[0]
}

const filterData = (data, defaultValue = 0) => {
  if (!data || data.length === 0) return []
  let lastValid = defaultValue
  return data.map(val => {
    if (val !== null && val !== undefined && !isNaN(val)) {
      lastValid = val
      return val
    }
    return lastValid
  })
}

const switchPeriod = async (period) => {
  selectedPeriod.value = period
  showCustomDate.value = false
  await Promise.all([loadTrendData(), loadCorrelationTrend()])
}

const openCustomDatePicker = () => {
  showCustomDate.value = true
}

const closeCustomDatePicker = () => {
  showCustomDate.value = false
}

const loadHealthScore = async () => {
  try {
    const res = await axios.get(`${API_BASE}/health-analysis/health-score/${props.userId}`)
    if (res.data.success) {
      healthScore.value = res.data
    }
  } catch (e) {
    console.error('加载健康评分失败', e)
  }
}

const loadCalorieBalance = async () => {
  try {
    const res = await axios.get(`${API_BASE}/health-analysis/calorie-balance/${props.userId}`)
    calorieBalance.value = res.data
  } catch (e) {
    console.error('加载热量平衡失败', e)
  }
}

const loadSportAnalysis = async () => {
  try {
    const res = await axios.get(`${API_BASE}/health-analysis/sport-analysis/${props.userId}`)
    sportAnalysis.value = res.data
  } catch (e) {
    console.error('加载运动分析失败', e)
  }
}

const loadNutritionAnalysis = async () => {
  try {
    const res = await axios.get(`${API_BASE}/health-analysis/nutrition-analysis/${props.userId}`)
    nutritionAnalysis.value = res.data
  } catch (e) {
    console.error('加载营养分析失败', e)
  }
}

const loadSmartInsights = async () => {
  try {
    const res = await axios.get(`${API_BASE}/health-analysis/insights/${props.userId}`)
    smartInsights.value = res.data || []
  } catch (e) {
    console.error('加载智能建议失败', e)
  }
}

const loadCorrelationTrend = async () => {
  try {
    const res = await axios.get(`${API_BASE}/health-analysis/correlation-trend/${props.userId}`)
    correlationData.value = res.data
    await nextTick()
    initCorrelationChart()
  } catch (e) {
    console.error('加载关联趋势失败', e)
  }
}

const loadTrendData = async () => {
  loading.value = true
  try {
    const res = await axios.get(`${API_BASE}/health-analysis/trend/${props.userId}`, {
      params: { period: selectedPeriod.value }
    })
    if (res.data && res.data.dates && res.data.dates.length > 0) {
      const filteredBmi = filterData(res.data.bmiTrend, bmi.value)
      trendData.value = {
        dates: res.data.dates,
        bmiTrend: filteredBmi
      }
      await nextTick()
      initCharts()
    }
  } catch (error) {
    console.error('加载趋势数据失败', error)
  } finally {
    loading.value = false
  }
}

const loadCustomReport = async () => {
  if (!customStartDate.value || !customEndDate.value) {
    alert('请选择开始和结束日期')
    return
  }
  loading.value = true
  try {
    const res = await axios.get(`${API_BASE}/health-analysis/custom-report/${props.userId}`, {
      params: {
        startDate: customStartDate.value,
        endDate: customEndDate.value
      }
    })
    if (res.data && res.data.dates) {
      const filteredBmi = filterData(res.data.bmiTrend, bmi.value)
      trendData.value = {
        dates: res.data.dates,
        bmiTrend: filteredBmi
      }
      await nextTick()
      initCharts()
    }
    closeCustomDatePicker()
  } catch (error) {
    console.error('加载自定义报告失败', error)
  } finally {
    loading.value = false
  }
}

const loadReport = async () => {
  if (!startDate.value || !endDate.value) {
    alert('请选择开始日期和结束日期')
    return
  }
  reportLoading.value = true
  try {
    const res = await axios.get(`${API_BASE}/health-analysis/report/range/${props.userId}`, {
      params: {
        startDate: startDate.value,
        endDate: endDate.value
      }
    })
    if (res.data && res.data.success) {
      report.value = res.data
    } else {
      alert(res.data?.message || '获取报告失败')
    }
  } catch (e) {
    console.error('加载失败', e)
    report.value = {
      startDate: startDate.value,
      endDate: endDate.value,
      weightChange: 0,
      avgSleep: 7.5,
      totalWater: 10500,
      advice: '体重保持稳定，继续保持良好习惯。'
    }
  } finally {
    reportLoading.value = false
  }
}

const loadHealthStatus = async () => {
  try {
    const res = await axios.get(`${API_BASE}/health-analysis/status/${props.userId}`)
    if (res.data) {
      bmi.value = res.data.bmi || 19.1
      latestWeight.value = res.data.weight
      latestHeight.value = res.data.height
    }
  } catch (error) {
    console.error('加载健康状态失败', error)
  }
}

const loadHealthTips = async () => {
  try {
    const res = await axios.get(`${API_BASE}/health-analysis/tips/${props.userId}`)
    if (res.data && Array.isArray(res.data) && res.data.length > 0) {
      displayTips.value = res.data
    }
  } catch (error) {
    console.error('加载健康小贴士失败', error)
  }
}

const initCorrelationChart = () => {
  if (!correlationChartCanvas.value) return
  if (correlationChart) correlationChart.destroy()

  const dates = correlationData.value.dates || []
  const weights = correlationData.value.weights || []
  const calories = correlationData.value.calories || []

  if (dates.length === 0) return

  const validWeights = weights.filter(w => w !== null && w !== undefined)
  const minWeight = validWeights.length > 0 ? Math.min(...validWeights) - 0.5 : 40
  const maxWeight = validWeights.length > 0 ? Math.max(...validWeights) + 0.5 : 80
  const weightStep = Math.ceil((maxWeight - minWeight) / 5) || 1

  correlationChart = new Chart(correlationChartCanvas.value, {
    type: 'line',
    data: {
      labels: dates,
      datasets: [
        {
          label: '⚖️ 体重(kg)',
          data: weights,
          type: 'line',
          borderColor: '#40E0D0',
          backgroundColor: 'rgba(64, 224, 208, 0.05)',
          borderWidth: 3,
          tension: 0.3,
          fill: true,
          pointBackgroundColor: '#40E0D0',
          pointBorderColor: '#fff',
          pointRadius: 5,
          pointHoverRadius: 7,
          pointBorderWidth: 2,
          yAxisID: 'y'
        },
        {
          label: '🏃 运动消耗(大卡)',
          data: calories,
          type: 'bar',
          backgroundColor: 'rgba(255, 152, 0, 0.7)',
          borderRadius: 6,
          borderSkipped: false,
          yAxisID: 'y1',
          barPercentage: 0.7,
          categoryPercentage: 0.8
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      plugins: {
        legend: { position: 'top', labels: { color: 'rgba(255,255,255,0.8)', usePointStyle: true, boxWidth: 10 } },
        tooltip: { mode: 'index', intersect: false, backgroundColor: 'rgba(0,0,0,0.8)', titleColor: '#40E0D0', bodyColor: 'rgba(255,255,255,0.9)', borderColor: '#40E0D0', borderWidth: 1 }
      },
      scales: {
        y: { title: { display: true, text: '体重(kg)', color: '#40E0D0', font: { weight: 'bold', size: 12 } }, min: Math.floor(minWeight), max: Math.ceil(maxWeight), grid: { color: 'rgba(255,255,255,0.08)' }, ticks: { color: 'rgba(255,255,255,0.7)', stepSize: weightStep, callback: (val) => val + ' kg' } },
        y1: { position: 'right', title: { display: true, text: '消耗(大卡)', color: '#FF9800', font: { weight: 'bold', size: 12 } }, grid: { drawOnChartArea: false }, ticks: { color: '#FF9800', callback: (val) => val + ' 大卡' } },
        x: { ticks: { color: 'rgba(255,255,255,0.7)', maxRotation: 45, autoSkip: true, maxTicksLimit: 7 }, grid: { display: false } }
      }
    }
  })
}

const clearRetryTimer = () => {
  if (retryTimer) {
    clearTimeout(retryTimer)
    retryTimer = null
  }
}

const initCharts = () => {
  clearRetryTimer()
  if (!bmiChartCanvas.value) {
    retryTimer = setTimeout(() => {
      if (bmiChartCanvas.value) {
        initCharts()
      }
    }, 100)
    return
  }

  const dates = trendData.value.dates || []
  const bmiData = trendData.value.bmiTrend || []

  if (dates.length === 0) return

  if (bmiChart) bmiChart.destroy()

  bmiChart = new Chart(bmiChartCanvas.value, {
    type: 'line',
    data: {
      labels: dates,
      datasets: [{
        label: '📊 BMI',
        data: bmiData,
        borderColor: '#40E0D0',
        backgroundColor: 'rgba(64, 224, 208, 0.08)',
        borderWidth: 3,
        tension: 0.3,
        fill: true,
        pointBackgroundColor: '#40E0D0',
        pointBorderColor: '#fff',
        pointRadius: 5,
        pointHoverRadius: 7,
        pointBorderWidth: 2
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: { position: 'top', labels: { color: 'rgba(255,255,255,0.8)' } },
        tooltip: { backgroundColor: 'rgba(0,0,0,0.8)', titleColor: '#40E0D0', bodyColor: 'rgba(255,255,255,0.9)', borderColor: '#40E0D0', borderWidth: 1 }
      },
      scales: {
        y: { beginAtZero: false, grid: { color: 'rgba(255,255,255,0.08)' }, ticks: { color: 'rgba(255,255,255,0.7)' }, title: { display: true, text: 'BMI', color: '#40E0D0' } },
        x: { ticks: { color: 'rgba(255,255,255,0.7)', maxRotation: 45, autoSkip: true, maxTicksLimit: 6 }, grid: { display: false } }
      }
    }
  })
}

const loadAllAnalysisData = async () => {
  await Promise.all([
    loadHealthScore(),
    loadCalorieBalance(),
    loadSportAnalysis(),
    loadNutritionAnalysis(),
    loadSmartInsights(),
    loadCorrelationTrend()
  ])
}

watch(selectedPeriod, async () => {
  await loadTrendData()
})

onMounted(async () => {
  setDefaultDateRange()
  await loadHealthStatus()
  await loadTrendData()
  await loadReport()
  await loadHealthTips()
  await loadAllAnalysisData()
})

onUnmounted(() => {
  clearRetryTimer()
  if (bmiChart) bmiChart.destroy()
  if (correlationChart) correlationChart.destroy()
})
</script>

<style scoped>
.health-analysis-container {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 20px 24px 40px;
}

.page-header {
  text-align: center;
  margin-bottom: 8px;
}

.page-header h1 {
  color: white;
  font-size: 28px;
  margin-bottom: 8px;
  font-weight: 600;
}

.page-header p {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.glass-card {
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 24px;
  transition: all 0.2s;
}

.glass-card:hover {
  background: rgba(0, 0, 0, 0.35);
}

/* 健康综合评分卡片 */
.score-card {
  margin-bottom: 0;
}

.score-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.score-icon {
  font-size: 28px;
}

.score-header h4 {
  font-size: 18px;
  font-weight: 500;
  color: white;
  margin: 0;
}

.score-main {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 40px;
  flex-wrap: wrap;
}

.score-circle {
  position: relative;
  width: 120px;
  height: 120px;
}

.score-number {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 32px;
  font-weight: 700;
  color: #40E0D0;
}

.score-level {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.score-details {
  flex: 1;
  min-width: 200px;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.score-label {
  width: 40px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  text-align: left;
}

.score-bar-bg {
  flex: 1;
  height: 8px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  overflow: hidden;
}

.score-bar {
  height: 100%;
  background: linear-gradient(90deg, #40E0D0, #2BA0D0);
  border-radius: 4px;
  transition: width 0.5s;
}

.score-value {
  width: 45px;
  font-size: 12px;
  color: #40E0D0;
  text-align: right;
}

/* 核心指标卡片 */
.core-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.core-card {
  text-align: center;
  padding: 20px;
  position: relative;
}

.core-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.core-value {
  font-size: 36px;
  font-weight: 700;
  color: #40E0D0;
}

.core-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 4px;
}

.core-change {
  font-size: 12px;
  margin-top: 6px;
}

.core-change.positive { color: #4CAF50; }
.core-change.negative { color: #F44336; }
.core-status {
  font-size: 12px;
  margin-top: 6px;
  padding: 2px 8px;
  display: inline-block;
  border-radius: 20px;
  background: rgba(64, 224, 208, 0.2);
  color: #40E0D0;
}

/* 热量平衡 */
.calorie-card .card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.card-header h4 {
  font-size: 18px;
  font-weight: 500;
  color: white;
  margin: 0;
}

.balance-bars {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 20px;
}

.balance-item {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.balance-label {
  width: 90px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.balance-bar-bg {
  flex: 1;
  height: 24px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  overflow: hidden;
}

.balance-bar {
  height: 100%;
  border-radius: 12px;
  transition: width 0.3s;
}

.intake-bar {
  background: linear-gradient(90deg, #40E0D0, #2BA0D0);
  opacity: 0.8;
}

.burn-bar {
  background: linear-gradient(90deg, #40E0D0, #2BA0D0);
  opacity: 0.5;
}

.balance-value {
  min-width: 80px;
  font-size: 14px;
  font-weight: 500;
  color: white;
}

.balance-result {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 13px;
  text-align: center;
}

.balance-result.deficit { background: rgba(76, 175, 80, 0.15); color: #4CAF50; }
.balance-result.balance { background: rgba(64, 224, 208, 0.15); color: #40E0D0; }
.balance-result.surplus { background: rgba(244, 67, 54, 0.15); color: #F44336; }

.balance-analogy {
  margin-top: 12px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.04);
  border-radius: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.analogy-icon {
  font-size: 18px;
}

.analogy-text {
  flex: 1;
}

/* 两列布局 */
.two-cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.stat-row {
  display: flex;
  justify-content: space-around;
  margin-bottom: 16px;
}

.stat-item {
  text-align: center;
}

.stat-value-large {
  font-size: 32px;
  font-weight: 700;
  color: #40E0D0;
}

.stat-trend {
  font-size: 12px;
  margin-top: 4px;
}

.stat-trend.positive { color: #4CAF50; }
.stat-trend.negative { color: #F44336; }

.stat-detail {
  text-align: center;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

/* 营养分析 */
.date-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  margin-left: auto;
}

.nutrition-pie {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.pie-item {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.pie-bar {
  width: 0;
  height: 28px;
  border-radius: 14px;
  transition: width 0.5s ease;
  background: linear-gradient(90deg, #40E0D0, #2BA0D0);
}

.pie-bar.protein { opacity: 1; }
.pie-bar.fat { opacity: 0.7; }
.pie-bar.carbs { opacity: 0.5; }

.pie-item span {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
  min-width: 70px;
}

.nutrition-gram {
  font-size: 13px;
  color: #40E0D0;
  font-weight: 600;
  margin-left: auto;
}

.stat-detail {
  text-align: center;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  display: flex;
  justify-content: center;
  gap: 16px;
}

/* 智能建议 */
.insights-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.insight-item {
  padding: 10px 14px;
  background: rgba(64, 224, 208, 0.08);
  border-radius: 12px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  border-left: 3px solid #40E0D0;
}

/* 图表区域 */
.chart-card {
  padding: 20px;
}

.chart-canvas {
  width: 100%;
  height: 280px !important;
  min-height: 280px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: white;
}

.chart-unit {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

/* 健康报告 */
.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.report-header h4 {
  margin: 0;
  color: white;
  font-size: 16px;
  font-weight: 600;
}

.report-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.quick-periods {
  display: flex;
  gap: 8px;
}

.quick-period-btn {
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 30px;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-period-btn:hover {
  background: rgba(64, 224, 208, 0.15);
  color: #40E0D0;
  border-color: #40E0D0;
}

.date-input {
  padding: 8px 12px;
  font-size: 13px;
}

.report-date {
  text-align: center;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 20px;
}

.stats {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 20px;
}

.stat {
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: white;
}

.advice {
  background: rgba(64, 224, 208, 0.1);
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  border: 1px solid rgba(64, 224, 208, 0.2);
}

.empty-report {
  text-align: center;
  padding: 40px;
  color: rgba(255, 255, 255, 0.5);
}

.empty-report span {
  font-size: 48px;
  opacity: 0.5;
}

/* 健康小贴士 */
.tip {
  padding: 10px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.tip:first-child {
  padding-top: 0;
}

.tip:last-child {
  border-bottom: none;
}

/* 筛选按钮 */
.period-filter {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.period-filter button {
  padding: 8px 24px;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 40px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  transition: all 0.2s;
}

.period-filter button:hover {
  background: rgba(64, 224, 208, 0.1);
  border-color: rgba(64, 224, 208, 0.4);
}

.period-filter button.active {
  background: rgba(64, 224, 208, 0.2);
  border-color: #40E0D0;
  color: #40E0D0;
}

/* 自定义日期面板 */
.custom-date-panel {
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(64, 224, 208, 0.2);
  border-radius: 24px;
  padding: 0;
  overflow: hidden;
  max-width: 380px;
  margin: 0 auto;
}

.custom-date-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  background: rgba(0, 0, 0, 0.2);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.panel-icon {
  font-size: 20px;
}

.panel-title {
  font-size: 15px;
  font-weight: 500;
  color: white;
  flex: 1;
}

.panel-close {
  background: rgba(255, 255, 255, 0.08);
  border: none;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.6);
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.panel-close:hover {
  background: rgba(229, 115, 115, 0.3);
  color: #ff8888;
}

.custom-date-body {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 24px 20px;
  flex-wrap: wrap;
}

.date-input-group {
  flex: 1;
  min-width: 120px;
}

.date-input-group label {
  display: block;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 8px;
}

.date-arrow {
  font-size: 20px;
  color: #40E0D0;
  opacity: 0.6;
}

.custom-date-footer {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(0, 0, 0, 0.15);
}

.custom-date-footer .btn-primary,
.custom-date-footer .btn-secondary {
  flex: 1;
  text-align: center;
}

.glass-input {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 40px;
  padding: 10px 16px;
  color: white;
  font-size: 14px;
  outline: none;
}

.btn-primary {
  padding: 10px 24px;
  background: rgba(64, 224, 208, 0.2);
  border: 1px solid rgba(64, 224, 208, 0.4);
  border-radius: 40px;
  color: #40E0D0;
  cursor: pointer;
  font-size: 14px;
}

.btn-secondary {
  padding: 10px 24px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 40px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.2);
  border-top-color: #40E0D0;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 900px) {
  .health-analysis-container {
    padding: 0 16px;
  }
  .core-metrics {
    grid-template-columns: repeat(2, 1fr);
  }
  .two-cols {
    grid-template-columns: 1fr;
  }
  .score-main {
    flex-direction: column;
    text-align: center;
  }
}

@media (max-width: 600px) {
  .core-metrics {
    grid-template-columns: 1fr;
  }
  .report-actions {
    flex-direction: column;
    align-items: stretch;
  }
  .quick-periods {
    justify-content: center;
  }
}
</style>