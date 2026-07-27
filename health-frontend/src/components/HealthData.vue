<template>
  <div class="tab-content">
    <!-- 记录健康数据卡片 -->
    <div class="card">
      <h4>📝 记录健康数据</h4>
      <div class="grid-container">
        <div class="grid-item">
          <label>👤 性别</label>
          <div class="custom-select" ref="genderSelectRef">
            <div class="custom-select-trigger" @click.stop="toggleGenderDropdown">
              <span>{{ genderDisplay }}</span>
              <span class="select-arrow">▼</span>
            </div>
            <div class="custom-select-dropdown" v-if="showGenderDropdown">
              <div class="custom-select-option" :class="{ active: healthForm.gender === '0' }" @click="selectGender('0')">女</div>
              <div class="custom-select-option" :class="{ active: healthForm.gender === '1' }" @click="selectGender('1')">男</div>
            </div>
          </div>
        </div>
        <div class="grid-item"><label>🎂 年龄</label><input v-model="healthForm.age" placeholder="年龄" type="number" /></div>
        <div class="grid-item"><label>📏 身高(cm)</label><input v-model="healthForm.height" placeholder="身高" type="number" /></div>
        <div class="grid-item"><label>⚖️ 体重(kg)</label><input v-model="healthForm.weight" placeholder="体重" type="number" /></div>
        <div class="grid-item"><label>📊 体脂率(%)</label><div class="calculated-field"><input v-model="healthForm.bodyFat" placeholder="自动计算" type="number" readonly /><span class="calc-hint">自动计算</span></div></div>
        <div class="grid-item"><label>📐 胸围(cm)</label><input v-model="healthForm.chest" placeholder="胸围" type="number" /></div>
        <div class="grid-item"><label>📏 腰围(cm)</label><input v-model="healthForm.waist" placeholder="腰围" type="number" /></div>
        <div class="grid-item"><label>📐 臀围(cm)</label><input v-model="healthForm.hip" placeholder="臀围" type="number" /></div>
        <div class="grid-item"><label>🦵 大腿围(cm)</label><input v-model="healthForm.thigh" placeholder="大腿围" type="number" /></div>
        <div class="grid-item"><label>❤️ 心率</label><input v-model="healthForm.restingHeartRate" placeholder="心率" type="number" /></div>
        <div class="grid-item"><label>😴 睡眠(小时)</label><input v-model="healthForm.sleepDuration" placeholder="睡眠" type="number" /></div>
        <div class="grid-item"><label>💧 饮水量(ml)</label><input v-model="healthForm.waterIntake" placeholder="饮水量" type="number" /></div>
      </div>
      <button @click="submitHealthRecord">记录健康数据</button>
    </div>

    <!-- 目标设置卡片 -->
    <div class="card">
      <h4>🎯 目标设置</h4>
      <div class="target-row">
        <div class="target-item"><label>⚖️ 目标体重 (kg)</label><input v-model="targetWeightInput" placeholder="目标体重" type="number" /></div>
        <div class="target-item"><label>📊 目标体脂 (%)</label><input v-model="targetBodyFatInput" placeholder="目标体脂" type="number" /></div>
      </div>
      <div class="target-buttons"><button @click="saveTargets">保存目标设置</button></div>

      <div class="progress-section">
        <div class="progress-item" v-if="props.userInfo?.currentWeight && targetWeight">
          <div class="progress-label">⚖️ 体重进度</div>
          <div class="progress-bar-bg">
            <div class="progress-bar-fill" :style="{width: weightProgressPercent + '%'}"></div>
          </div>
          <div class="progress-value">{{ props.userInfo?.currentWeight || 0 }} / {{ targetWeight }} kg</div>
        </div>
        <div class="progress-item" v-if="targetBodyFat">
          <div class="progress-label">📊 体脂进度</div>
          <div class="progress-bar-bg">
            <div class="progress-bar-fill" :style="{width: bodyFatProgressPercent + '%', background: bodyFatProgressPercent >= 100 ? '#40E0D0' : '#FF6B6B'}"></div>
          </div>
          <div class="progress-value">
            {{ props.healthForm.bodyFat || '?' }}% / {{ targetBodyFat }}%
            <span v-if="props.healthForm.bodyFat && props.healthForm.bodyFat > targetBodyFat" style="color: #ffa500; font-size: 10px;"> 还差 {{ (props.healthForm.bodyFat - targetBodyFat).toFixed(1) }}%</span>
            <span v-else-if="props.healthForm.bodyFat && props.healthForm.bodyFat <= targetBodyFat" style="color: #40E0D0; font-size: 10px;"> ✓ 已达到目标</span>
          </div>
        </div>
      </div>
      <div class="health-advice"><p>{{ healthAdvice }}</p></div>
    </div>

    <!-- 3D肌肉训练热力图 -->
    <div class="card">
      <h4>💪 3D肌肉训练热力图</h4>
      <p style="font-size: 12px; color: rgba(255,255,255,0.5); text-align: center; margin-bottom: 16px;">🖱️ 鼠标拖拽旋转视角 | 点击查看肌肉详情</p>
      <MuscleHeatmap3D :muscle-data="realMuscleCount" />
    </div>

    <!-- 健康数据趋势分析卡片 -->
    <div class="card">
      <div class="chart-header">
        <h4>📈 健康数据多维趋势分析</h4>
        <div class="chart-controls">
          <div class="date-range-selector">
            <button :class="{ active: dateRange === 'week' }" @click="setDateRange('week')">本周</button>
            <button :class="{ active: dateRange === 'month' }" @click="setDateRange('month')">本月</button>
            <button :class="{ active: dateRange === 'year' }" @click="setDateRange('year')">本年</button>
            <button :class="{ active: dateRange === 'all' }" @click="setDateRange('all')">全部</button>
          </div>
          <button class="export-btn" @click="exportHealthData">导出Excel</button>
        </div>
      </div>
      <div class="charts-grid" v-if="hasChartData">
        <div class="chart-item"><div id="weightChartContent" class="chart-content"></div></div>
        <div class="chart-item"><div id="bodyFatChartContent" class="chart-content"></div></div>
        <div class="chart-item"><div id="dimensionChartContent" class="chart-content"></div></div>
        <div class="chart-item"><div id="heartRateSleepChartContent" class="chart-content"></div></div>
      </div>
      <div v-else class="empty-chart"><div class="empty-icon">📭</div><p>暂无健康数据</p><p class="empty-hint">请先记录健康数据，图表将自动生成 📊</p></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'
import MuscleHeatmap3D from './MuscleHeatmap3D.vue'
import eventBus from '@/utils/eventBus'

const props = defineProps({ userInfo: Object, healthForm: Object })
const emit = defineEmits(['update:healthForm', 'refresh'])

const API_BASE = 'http://localhost:8080/api'

const realMuscleCount = ref({
  chest: 0, lats: 0, biceps: 0, triceps: 0, abs: 0, quads: 0, calves: 0, glutes: 0
})

const targetWeightInput = ref('')
const targetWeight = ref('')
const targetBodyFatInput = ref('')
const targetBodyFat = ref('')
const dateRange = ref('week')
let weightChart = null, bodyFatChart = null, dimensionChart = null, heartRateSleepChart = null, resizeHandler = null
let rawChartData = null
const hasChartData = ref(false)

const localHealthForm = ref({
  height: '', weight: '', gender: '0', age: '', bodyFat: '',
  chest: '', waist: '', hip: '', thigh: '', restingHeartRate: '',
  sleepDuration: '', waterIntake: ''
})

const showGenderDropdown = ref(false)
const genderSelectRef = ref(null)
const genderDisplay = computed(() => props.healthForm.gender === '0' ? '女' : '男')
const toggleGenderDropdown = () => { showGenderDropdown.value = !showGenderDropdown.value }
const selectGender = (value) => {
  emit('update:healthForm', { ...props.healthForm, gender: value })
  showGenderDropdown.value = false
}

const weightProgressPercent = computed(() => {
  if (!props.userInfo?.currentWeight || !targetWeight.value) return 0
  const current = props.userInfo.currentWeight, target = parseFloat(targetWeight.value)
  if (current <= target) return 100
  const start = target + 10
  return Math.min(100, Math.max(0, ((start - current) / (start - target)) * 100))
})

const bodyFatProgressPercent = computed(() => {
  let current = props.healthForm.bodyFat
  let target = targetBodyFat.value
  if (!target) return 0
  if (!current) return 0
  current = parseFloat(current) || 0
  target = parseFloat(target) || 0
  if (target <= 0) return 0
  if (current <= target) return 100
  let progress = (target / current) * 100
  return Math.min(100, Math.max(0, Math.round(progress)))
})

const getHealthyBodyFatRange = () => {
  const age = parseInt(props.healthForm.age) || 30
  const gender = props.healthForm.gender === '1' ? 'male' : 'female'
  const ranges = {
    male: [
      { min: 18, max: 30, low: 8, high: 19 },
      { min: 31, max: 40, low: 10, high: 21 },
      { min: 41, max: 50, low: 12, high: 23 },
      { min: 51, max: 60, low: 14, high: 25 },
      { min: 61, max: 100, low: 15, high: 26 }
    ],
    female: [
      { min: 18, max: 30, low: 16, high: 24 },
      { min: 31, max: 40, low: 17, high: 26 },
      { min: 41, max: 50, low: 18, high: 28 },
      { min: 51, max: 60, low: 19, high: 30 },
      { min: 61, max: 100, low: 20, high: 32 }
    ]
  }
  const ageRanges = ranges[gender]
  const range = ageRanges.find(r => age >= r.min && age <= r.max) || ageRanges[0]
  return { low: range.low, high: range.high }
}

const evaluateBodyFat = () => {
  const bodyFat = parseFloat(props.healthForm.bodyFat)
  if (!bodyFat) return null
  const { low, high } = getHealthyBodyFatRange()
  if (bodyFat < low) return { status: '偏低', color: '#40E0D0', message: '体脂率偏低，注意营养摄入和适当增肌' }
  if (bodyFat <= high) return { status: '健康', color: '#40E0D0', message: '体脂率在健康范围内，继续保持！' }
  if (bodyFat <= high * 1.2) return { status: '偏高', color: '#FFA500', message: '体脂率略高，建议增加有氧运动' }
  return { status: '过高', color: '#FF6B6B', message: '体脂率过高，建议调整饮食并增加运动' }
}

const calculateBMI = () => {
  if (!props.userInfo?.currentHeight || !props.userInfo?.currentWeight) return null
  return Math.round((props.userInfo.currentWeight / ((props.userInfo.currentHeight/100)**2)) * 10) / 10
}

const healthAdvice = computed(() => {
  const bmi = calculateBMI()
  const bodyFatEval = evaluateBodyFat()
  if (!bmi && !bodyFatEval) return '请先记录身高体重'
  if (bodyFatEval) {
    const { low, high } = getHealthyBodyFatRange()
    const genderText = props.healthForm.gender === '1' ? '男性' : '女性'
    const age = props.healthForm.age || '?'
    return `${bodyFatEval.message} (${genderText} ${age}岁健康范围: ${low}-${high}%)`
  }
  if (bmi < 18.5) return '体重偏轻，建议增加健康饮食和力量训练'
  if (bmi < 24) return '体重正常，继续保持良好习惯'
  if (bmi < 28) return '体重偏重，建议控制饮食 + 规律运动'
  return '体重过重，建议咨询专业营养师或医生'
})

const loadTargetsFromStorage = () => {
  const savedWeight = localStorage.getItem('targetWeight')
  const savedBodyFat = localStorage.getItem('targetBodyFat')
  if (savedWeight) {
    targetWeightInput.value = savedWeight
    targetWeight.value = savedWeight
  }
  if (savedBodyFat) {
    targetBodyFatInput.value = savedBodyFat
    targetBodyFat.value = savedBodyFat
  }
}

const saveTargets = () => {
  if (!targetWeightInput.value && !targetBodyFatInput.value) {
    alert('请输入目标值');
    return
  }
  if (targetWeightInput.value) {
    localStorage.setItem('targetWeight', targetWeightInput.value);
    targetWeight.value = targetWeightInput.value
  }
  if (targetBodyFatInput.value) {
    localStorage.setItem('targetBodyFat', targetBodyFatInput.value);
    targetBodyFat.value = targetBodyFatInput.value
  }
  alert('目标设置已保存')
}

const updateParent = () => { emit('update:healthForm', { ...localHealthForm.value }) }

const filterDataByDateRange = (data, range) => {
  if (!data || !data.dates || data.dates.length === 0) return data

  const now = new Date()
  let startDate

  switch (range) {
    case 'week':
      startDate = new Date(now)
      startDate.setDate(now.getDate() - 7)
      break
    case 'month':
      startDate = new Date(now)
      startDate.setMonth(now.getMonth() - 1)
      break
    case 'year':
      startDate = new Date(now)
      startDate.setFullYear(now.getFullYear() - 1)
      break
    case 'all':
    default:
      startDate = new Date(now)
      startDate.setMonth(now.getMonth() - 6)
      break
  }

  const filtered = {
    dates: [],
    weights: [],
    bodyFats: [],
    chests: [],
    waists: [],
    hips: [],
    heartRates: [],
    sleepDurations: []
  }

  for (let i = 0; i < data.dates.length; i++) {
    const date = new Date(data.dates[i])
    if (date >= startDate) {
      filtered.dates.push(data.dates[i])
      filtered.weights.push(data.weights && data.weights[i] !== undefined ? data.weights[i] : null)
      filtered.bodyFats.push(data.bodyFats && data.bodyFats[i] !== undefined ? data.bodyFats[i] : null)
      filtered.chests.push(data.chests && data.chests[i] !== undefined ? data.chests[i] : null)
      filtered.waists.push(data.waists && data.waists[i] !== undefined ? data.waists[i] : null)
      filtered.hips.push(data.hips && data.hips[i] !== undefined ? data.hips[i] : null)
      filtered.heartRates.push(data.heartRates && data.heartRates[i] !== undefined ? data.heartRates[i] : null)
      filtered.sleepDurations.push(data.sleepDurations && data.sleepDurations[i] !== undefined ? data.sleepDurations[i] : null)
    }
  }

  return filtered
}

const setDateRange = async (range) => {
  dateRange.value = range
  if (rawChartData) {
    const filteredData = filterDataByDateRange(rawChartData, range)
    await initAllCharts(filteredData)
  }
}

const loadUserHealthData = async () => {
  if (!props.userInfo?.id) return
  try {
    const res = await axios.get(`${API_BASE}/user/${props.userInfo.id}`)
    const userData = res.data
    if (userData.currentHeight) localHealthForm.value.height = userData.currentHeight
    if (userData.currentWeight) localHealthForm.value.weight = userData.currentWeight
    if (userData.age) localHealthForm.value.age = userData.age
    if (userData.gender) localHealthForm.value.gender = userData.gender
    if (userData.bodyFat) {
      localHealthForm.value.bodyFat = userData.bodyFat
      emit('update:healthForm', { ...props.healthForm, bodyFat: userData.bodyFat })
    }
    if (userData.chest) localHealthForm.value.chest = userData.chest
    if (userData.waist) localHealthForm.value.waist = userData.waist
    if (userData.hip) localHealthForm.value.hip = userData.hip
    if (userData.thigh) localHealthForm.value.thigh = userData.thigh
    if (userData.restingHeartRate) localHealthForm.value.restingHeartRate = userData.restingHeartRate
    if (userData.sleepDuration) localHealthForm.value.sleepDuration = userData.sleepDuration
    if (userData.waterIntake) localHealthForm.value.waterIntake = userData.waterIntake
    updateParent()
  } catch (e) { console.error(e) }
}

const calculateBodyFat = () => {
  const { height, weight, age, gender } = props.healthForm
  if (!height || !weight || !age || gender === undefined) {
    emit('update:healthForm', { ...props.healthForm, bodyFat: '' });
    return
  }
  const bmi = weight / ((height/100)**2)
  let bodyFat = 1.20 * bmi + 0.23 * age - 10.8 * gender - 5.4
  bodyFat = Math.min(45, Math.max(5, bodyFat))
  emit('update:healthForm', { ...props.healthForm, bodyFat: Math.round(bodyFat * 10) / 10 })
}

const submitHealthRecord = async () => {
  try {
    await axios.post(`${API_BASE}/health-analysis/record`, null, {
      params: { userId: props.userInfo.id, ...props.healthForm }
    })
    alert('记录成功')
    emit('refresh')
    await loadChartData()
    await loadUserHealthData()
    await nextTick()
  } catch(e) {
    console.error('记录失败:', e)
    alert('记录失败')
  }
}

const exportHealthData = () => window.open(`${API_BASE}/export/health/${props.userInfo.id}`)

const updateMuscleHighlights = async () => {
  try {
    const res = await axios.get(`${API_BASE}/sport/records/${props.userInfo.id}`)
    const records = res.data || []
    const muscleCount = { chest:0, lats:0, biceps:0, triceps:0, abs:0, quads:0, calves:0, glutes:0 }
    records.forEach(r => {
      const name = r.sportName
      if (name.includes('卧推') || name.includes('俯卧撑')) muscleCount.chest++
      if (name.includes('引体') || name.includes('划船')) { muscleCount.lats++; muscleCount.biceps++ }
      if (name.includes('深蹲')) muscleCount.quads++
      if (name.includes('跑') || name.includes('跳绳')) { muscleCount.calves++; muscleCount.quads++ }
      if (name.includes('骑行')) muscleCount.quads++
      if (name.includes('卷腹') || name.includes('仰卧起坐')) muscleCount.abs++
    })
    realMuscleCount.value = { ...muscleCount }
  } catch(e) { console.error(e) }
}

const disposeCharts = () => {
  [weightChart, bodyFatChart, dimensionChart, heartRateSleepChart].forEach(c => c?.dispose());
  [weightChart, bodyFatChart, dimensionChart, heartRateSleepChart] = [null, null, null, null]
}

const initAllCharts = async (data) => {
  if (!data?.dates?.length) {
    hasChartData.value = false;
    return
  }
  hasChartData.value = true
  await nextTick()
  let retryCount = 0
  const tryInit = () => {
    const weightDom = document.getElementById('weightChartContent'),
        bodyFatDom = document.getElementById('bodyFatChartContent'),
        dimensionDom = document.getElementById('dimensionChartContent'),
        hrDom = document.getElementById('heartRateSleepChartContent')
    if (!weightDom || !bodyFatDom || !dimensionDom || !hrDom) {
      if (retryCount < 10) {
        retryCount++;
        setTimeout(tryInit, 300)
      } else {
        console.error('DOM元素未找到')
      }
      return
    }
    disposeCharts()
    const dateCount = data.dates.length
    let labelInterval = 0
    if (dateCount > 30) labelInterval = Math.floor(dateCount / 8)
    else if (dateCount > 20) labelInterval = Math.floor(dateCount / 6)
    else if (dateCount > 12) labelInterval = Math.floor(dateCount / 4)
    const commonXAxis = { type: 'category', data: data.dates, axisLabel: { rotate: 45, fontSize: 10, color: '#ccc', interval: labelInterval, margin: 15 } }

    weightChart = echarts.init(weightDom)
    weightChart.setOption({
      title: { text: '📉 体重变化趋势 (kg)', left: 'center', top: 0, textStyle: { color: 'white', fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: commonXAxis,
      yAxis: { type: 'value', name: 'kg', nameTextStyle: { color: '#ccc' } },
      series: [{ name: '体重', type: 'line', data: data.weights, smooth: true, lineStyle: { color: '#40E0D0', width: 3 }, symbol: 'circle', symbolSize: 7, areaStyle: { opacity: 0.1, color: '#40E0D0' } }]
    })

    bodyFatChart = echarts.init(bodyFatDom)
    bodyFatChart.setOption({
      title: { text: '📊 体脂率变化趋势 (%)', left: 'center', top: 0, textStyle: { color: 'white', fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      xAxis: commonXAxis,
      yAxis: { type: 'value', name: '%', nameTextStyle: { color: '#ccc' } },
      series: [{ name: '体脂率', type: 'line', data: data.bodyFats || [], smooth: true, lineStyle: { color: '#059669', width: 3 }, symbol: 'circle', symbolSize: 7, areaStyle: { opacity: 0.1, color: '#059669' } }]
    })

    dimensionChart = echarts.init(dimensionDom)
    dimensionChart.setOption({
      title: { text: '📏 围度变化趋势 (cm)', left: 'center', top: 0, textStyle: { color: 'white', fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      legend: { data: ['胸围', '腰围', '臀围'], textStyle: { color: '#ccc' }, top: 25, right: 10, itemWidth: 25, itemHeight: 12 },
      xAxis: commonXAxis,
      yAxis: { type: 'value', name: 'cm', nameTextStyle: { color: '#ccc' } },
      series: [
        { name: '胸围', type: 'line', data: data.chests || [], smooth: true, lineStyle: { color: '#FF6B6B', width: 3, shadowBlur: 8, shadowColor: '#FF6B6B' }, symbol: 'circle', symbolSize: 8, itemStyle: { color: '#FF6B6B', borderColor: '#fff', borderWidth: 2 } },
        { name: '腰围', type: 'line', data: data.waists || [], smooth: true, lineStyle: { color: '#FFA500', width: 2.5 }, symbol: 'diamond', symbolSize: 7, itemStyle: { color: '#FFA500', borderColor: '#fff', borderWidth: 1.5 } },
        { name: '臀围', type: 'line', data: data.hips || [], smooth: true, lineStyle: { color: '#9B59B6', width: 2.5 }, symbol: 'triangle', symbolSize: 7, itemStyle: { color: '#9B59B6', borderColor: '#fff', borderWidth: 1.5 } }
      ]
    })

    heartRateSleepChart = echarts.init(hrDom)
    heartRateSleepChart.setOption({
      title: { text: '❤️ 心率 & 睡眠趋势', left: 'center', top: 0, textStyle: { color: 'white', fontSize: 14 } },
      tooltip: { trigger: 'axis' },
      legend: { data: ['静息心率', '睡眠时长'], textStyle: { color: '#ccc' }, bottom: 0, left: 'center', itemWidth: 35, itemHeight: 12 },
      grid: { top: '15%', left: '12%', right: '8%', bottom: '15%', containLabel: true },
      xAxis: { type: 'category', data: data.dates, axisLabel: { rotate: 45, fontSize: 10, color: '#ccc', interval: labelInterval, margin: 15 } },
      yAxis: [{ type: 'value', name: '次/分', nameTextStyle: { color: '#ccc' } }, { type: 'value', name: '小时', nameTextStyle: { color: '#ccc' }, min: 0, max: 10 }],
      series: [{ name: '静息心率', type: 'line', data: data.heartRates || [], smooth: true, yAxisIndex: 0, lineStyle: { color: '#0891B2', width: 2.5 }, symbol: 'circle', symbolSize: 6 }, { name: '睡眠时长', type: 'bar', data: data.sleepDurations || [], yAxisIndex: 1, barWidth: '30%', itemStyle: { color: '#10B981', borderRadius: [6,6,0,0] }, label: { show: true, position: 'top', formatter: '{c}h', fontSize: 10, color: '#10B981' } }]
    })
  }
  tryInit()
}

const setupResizeListener = () => {
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  resizeHandler = () => { [weightChart, bodyFatChart, dimensionChart, heartRateSleepChart].forEach(c => c?.resize()) }
  window.addEventListener('resize', resizeHandler)
}

const loadChartData = async () => {
  if (!props.userInfo?.id) return
  try {
    const res = await axios.get(`${API_BASE}/health-analysis/all-history/${props.userInfo.id}`)
    rawChartData = res.data
    const filteredData = filterDataByDateRange(rawChartData, dateRange.value)
    await initAllCharts(filteredData)
  } catch(e) {
    console.error(e);
    hasChartData.value = false
  }
}

const handleClickOutside = (event) => {
  if (genderSelectRef.value && !genderSelectRef.value.contains(event.target)) showGenderDropdown.value = false
}

const handleSportDataChange = () => {
  updateMuscleHighlights()
}

watch(() => props.healthForm.bodyFat, () => {}, { immediate: true })
watch([() => props.healthForm.height, () => props.healthForm.weight, () => props.healthForm.age, () => props.healthForm.gender], () => calculateBodyFat())
watch(() => props.userInfo?.id, async (id) => {
  if(id) {
    await updateMuscleHighlights();
    await loadChartData();
    await loadUserHealthData()
  }
})

onMounted(async () => {
  loadTargetsFromStorage()
  if(props.userInfo?.id) {
    await updateMuscleHighlights();
    await loadChartData();
    await loadUserHealthData()
  }
  setupResizeListener()
  document.addEventListener('click', handleClickOutside)
  eventBus.on('sport-data-changed', handleSportDataChange)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  eventBus.off('sport-data-changed', handleSportDataChange)
})
</script>

<style scoped>
.tab-content { background: transparent; padding: 0; }
.card { background: rgba(0, 0, 0, 0.25); backdrop-filter: blur(12px); border: 1px solid rgba(255, 255, 255, 0.1); border-radius: 24px; padding: 24px; margin-bottom: 24px; color: white; }
.grid-container { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 20px; margin-bottom: 20px; }
.grid-item { display: flex; flex-direction: column; gap: 6px; }
.grid-item label { font-size: 13px; color: rgba(255, 255, 255, 0.7); }
.grid-item input, .target-item input { background: rgba(255, 255, 255, 0.08) !important; border: 1px solid rgba(255, 255, 255, 0.15) !important; border-radius: 40px !important; padding: 12px 20px !important; color: white !important; font-size: 14px !important; outline: none !important; transition: all 0.2s ease !important; width: 100% !important; box-sizing: border-box !important; }
.grid-item input:hover, .target-item input:hover { border-color: #40E0D0 !important; background: rgba(255, 255, 255, 0.12) !important; }
.grid-item input:focus, .target-item input:focus { border-color: #40E0D0 !important; box-shadow: 0 0 0 2px rgba(64, 224, 208, 0.2) !important; }
.custom-select { position: relative; width: 100%; }
.custom-select-trigger { background: rgba(255, 255, 255, 0.08); backdrop-filter: blur(12px); border: 1px solid rgba(255, 255, 255, 0.15); border-radius: 40px; padding: 12px 20px; color: white; font-size: 14px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; transition: all 0.2s ease; }
.custom-select-trigger:hover { border-color: #40E0D0; background: rgba(255, 255, 255, 0.12); }
.select-arrow { font-size: 10px; transition: transform 0.2s; color: rgba(255, 255, 255, 0.6); }
.custom-select-dropdown { position: absolute; top: 100%; left: 0; right: 0; margin-top: 8px; background: rgba(20, 20, 35, 0.95); backdrop-filter: blur(16px); border: 1px solid rgba(255, 255, 255, 0.15); border-radius: 16px; overflow: hidden; z-index: 100; animation: dropdownFadeIn 0.2s ease; }
.custom-select-option { padding: 12px 20px; cursor: pointer; color: white; font-size: 14px; transition: all 0.2s; }
.custom-select-option:hover { background: rgba(64, 224, 208, 0.2); color: #40E0D0; }
.custom-select-option.active { background: rgba(64, 224, 208, 0.15); color: #40E0D0; }
@keyframes dropdownFadeIn { from { opacity: 0; transform: translateY(-8px); } to { opacity: 1; transform: translateY(0); } }
.grid-item input[type="number"]::-webkit-inner-spin-button, .grid-item input[type="number"]::-webkit-outer-spin-button, .target-item input[type="number"]::-webkit-inner-spin-button, .target-item input[type="number"]::-webkit-outer-spin-button { -webkit-appearance: none; margin: 0; }
.grid-item input[type="number"], .target-item input[type="number"] { -moz-appearance: textfield; appearance: textfield; }
.calculated-field { position: relative; }
.calc-hint { position: absolute; right: 12px; top: 50%; transform: translateY(-50%); font-size: 11px; color: rgba(255, 255, 255, 0.4); }
button { background: rgba(255, 255, 255, 0.1); border: 1px solid rgba(255, 255, 255, 0.2); border-radius: 40px; padding: 8px 20px; color: white; cursor: pointer; transition: all 0.2s; }
button:hover { background: rgba(64, 224, 208, 0.25); border-color: #40E0D0; transform: translateY(-1px); }
.target-row { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; margin-bottom: 16px; }
.target-item { display: flex; flex-direction: column; gap: 8px; }
.target-item label { font-size: 13px; color: rgba(255, 255, 255, 0.7); }
.target-buttons { display: flex; justify-content: flex-end; margin-bottom: 20px; }
.progress-section { margin: 20px 0; padding: 16px; background: rgba(255, 255, 255, 0.05); border-radius: 16px; }
.progress-item { margin-bottom: 16px; }
.progress-item:last-child { margin-bottom: 0; }
.progress-label { font-size: 12px; color: rgba(255, 255, 255, 0.6); margin-bottom: 6px; }
.progress-bar-bg { background: rgba(255, 255, 255, 0.15); border-radius: 20px; height: 8px; overflow: hidden; }
.progress-bar-fill { background: #40E0D0; height: 100%; border-radius: 20px; transition: width 0.3s ease; }
.progress-value { font-size: 11px; color: rgba(255, 255, 255, 0.5); margin-top: 4px; text-align: right; }
.chart-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 16px; margin-bottom: 20px; }
.chart-controls { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.date-range-selector { display: flex; gap: 8px; background: rgba(255, 255, 255, 0.08); border-radius: 40px; padding: 4px; }
.date-range-selector button { background: transparent; border: none; border-radius: 36px; padding: 6px 16px; font-size: 13px; cursor: pointer; color: rgba(255, 255, 255, 0.6); }
.date-range-selector button.active { background: #40E0D0; color: #1a1a2e; }
.export-btn { background: rgba(64, 224, 208, 0.15); border-color: rgba(64, 224, 208, 0.4); }
.export-btn:hover { background: rgba(64, 224, 208, 0.3); }
.health-advice { background: rgba(255, 255, 255, 0.05); border-radius: 16px; padding: 12px; font-size: 13px; margin-top: 16px; }
.charts-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; margin-top: 16px; }
.chart-item { background: rgba(0, 0, 0, 0.25); backdrop-filter: blur(12px); border-radius: 24px; padding: 16px; }
.chart-content { width: 100%; height: 320px; }
.empty-chart { text-align: center; padding: 60px 20px; }
.empty-icon { font-size: 48px; margin-bottom: 16px; opacity: 0.5; }
.empty-chart p { color: rgba(255, 255, 255, 0.5); font-size: 14px; margin: 8px 0; }
.empty-hint { font-size: 12px; color: rgba(255, 255, 255, 0.3); }
::-webkit-scrollbar { width: 6px; height: 6px; }
::-webkit-scrollbar-track { background: rgba(255, 255, 255, 0.05); border-radius: 3px; }
::-webkit-scrollbar-thumb { background: rgba(64, 224, 208, 0.3); border-radius: 3px; }
::-webkit-scrollbar-thumb:hover { background: rgba(64, 224, 208, 0.5); }
@media (max-width: 768px) { .charts-grid { grid-template-columns: 1fr; } .target-row { grid-template-columns: 1fr; gap: 12px; } .chart-header { flex-direction: column; align-items: flex-start; } }
</style>