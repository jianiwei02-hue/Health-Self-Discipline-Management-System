<template>
  <div class="diet-container">
    <!-- 顶部统计卡片 -->
    <div class="stats-row">
      <div class="stat-card glass">
        <div class="stat-value">{{ dailyDiet.totalCalories || 0 }}</div>
        <div class="stat-label">今日摄入 (大卡)</div>
      </div>
      <div class="stat-card glass">
        <div class="stat-value">{{ dailyDiet.records.length }}</div>
        <div class="stat-label">今日餐数</div>
      </div>
      <div class="stat-card glass">
        <div class="stat-value">{{ calorieProgress }}%</div>
        <div class="stat-label">目标进度</div>
        <div class="progress-bar"><div class="progress-fill" :style="{ width: calorieProgress + '%' }"></div></div>
      </div>
    </div>

    <!-- 添加饮食记录卡片 -->
    <div class="glass-card add-record-card">
      <div class="card-header">
        <span class="card-icon">🍽️</span>
        <h4>添加饮食记录</h4>
      </div>

      <!-- 图片上传识别区域 -->
      <div class="upload-area glass" @click="triggerFileUpload">
        <input
            type="file"
            ref="fileInput"
            accept="image/jpeg,image/png,image.jpg"
            @change="handleImageUpload"
            style="display: none"
        />
        <div v-if="!uploading && !previewImage" class="upload-placeholder">
          <span class="upload-icon">📷</span>
          <span>拍照/上传识图</span>
          <small>点击上传食物图片，AI自动识别</small>
        </div>
        <div v-if="uploading" class="uploading">
          <span class="loading-icon">⏳</span>
          <span>AI识别中...</span>
        </div>
        <div v-if="previewImage && !uploading" class="preview-area">
          <img :src="previewImage" class="preview-img" />
          <button class="cancel-preview" @click.stop="cancelPreview">✕</button>
        </div>
      </div>

      <!-- 识别结果（候选列表） -->
      <div v-if="recognizeResult && showCandidates" class="recognize-result glass">
        <div class="result-icon">🤖</div>
        <div class="result-info">
          <div class="candidates-list">
            <div class="candidates-title">🤔 AI识别结果，请选择正确的食物：</div>
            <div class="candidates-buttons">
              <button v-for="candidate in recognizeResult.candidates" :key="candidate.name" @click="selectCandidate(candidate)" class="candidate-btn glass">
                {{ candidate.name }}
                <span class="candidate-cal">{{ candidate.calories }}大卡/100g</span>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 识别结果（单个） -->
      <div v-else-if="recognizeResult && !showCandidates" class="recognize-result glass">
        <div class="result-icon">🤖</div>
        <div class="result-info">
          <div class="result-food">识别到：{{ recognizeResult.foodName }}</div>
          <div class="result-calories">预估热量：{{ recognizeResult.estimatedCalories }} 大卡/100g</div>
          <div class="result-actions">
            <button @click="applyRecognizeResult" class="apply-btn">✓ 使用</button>
            <button @click="clearRecognizeResult" class="cancel-btn">✕ 取消</button>
          </div>
        </div>
      </div>

      <!-- 表单行 -->
      <div class="form-row four-columns">
        <!-- 餐别下拉菜单 -->
        <div class="search-select" ref="mealSelectRef">
          <input
              type="text"
              :value="selectedMealLabel"
              @focus="showMealDropdown = true"
              @input="onMealInput"
              placeholder="选择餐次"
              class="glass-input"
              readonly
          />
          <div v-if="showMealDropdown && mealOptions.length > 0" class="dropdown-list">
            <div v-for="item in mealOptions" :key="item.value" @click="selectMeal(item)" class="dropdown-item">
              {{ item.label }}
            </div>
          </div>
        </div>

        <!-- 食物名称 -->
        <input
            v-model="dietForm.foodName"
            type="text"
            placeholder="食物名称"
            class="glass-input"
            @input="updateCurrentNutrition"
        />

        <!-- 重量 -->
        <input
            v-model="dietForm.weight"
            type="number"
            placeholder="重量(g)"
            class="glass-input no-spinner"
            @input="updateCurrentNutrition"
        />

        <!-- 热量/100g -->
        <input
            v-model="dietForm.caloriesPer100g"
            type="number"
            placeholder="热量/100g"
            class="glass-input no-spinner"
            @input="calculateCalories"
        />

        <!-- 热量预览 -->
        <div class="cal-preview" v-if="calculatedCalories > 0">
          🔥 {{ calculatedCalories }}
        </div>

        <!-- 添加按钮 -->
        <button class="glass-btn primary" @click="addDietRecord">➕ 添加</button>
      </div>

      <!-- 实时营养预览 -->
      <div class="nutrition-preview glass" v-if="currentNutrition && dietForm.weight > 0">
        <div class="nutrition-title">📊 营养分析 (按 {{ dietForm.weight }}g 计算)</div>
        <div class="nutrition-grid">
          <div class="nutrition-item">
            <span class="nutri-icon">💪</span>
            <span class="nutri-label">蛋白质</span>
            <span class="nutri-value">{{ currentNutrition.protein || 0 }}</span>
            <span class="nutri-unit">g</span>
          </div>
          <div class="nutrition-item">
            <span class="nutri-icon">🧈</span>
            <span class="nutri-label">脂肪</span>
            <span class="nutri-value">{{ currentNutrition.fat || 0 }}</span>
            <span class="nutri-unit">g</span>
          </div>
          <div class="nutrition-item">
            <span class="nutri-icon">🍚</span>
            <span class="nutri-label">碳水</span>
            <span class="nutri-value">{{ currentNutrition.carbs || 0 }}</span>
            <span class="nutri-unit">g</span>
          </div>
          <div class="nutrition-item">
            <span class="nutri-icon">🌾</span>
            <span class="nutri-label">膳食纤维</span>
            <span class="nutri-value">{{ currentNutrition.fiber || 0 }}</span>
            <span class="nutri-unit">g</span>
          </div>
          <div class="nutrition-item">
            <span class="nutri-icon">🍬</span>
            <span class="nutri-label">糖</span>
            <span class="nutri-value">{{ currentNutrition.sugar || 0 }}</span>
            <span class="nutri-unit">g</span>
          </div>
          <div class="nutrition-item">
            <span class="nutri-icon">🧂</span>
            <span class="nutri-label">钠</span>
            <span class="nutri-value">{{ currentNutrition.sodium !== undefined ? currentNutrition.sodium : 0 }}</span>
            <span class="nutri-unit">mg</span>
          </div>
          <div class="nutrition-item">
            <span class="nutri-icon">🦴</span>
            <span class="nutri-label">钙</span>
            <span class="nutri-value">{{ currentNutrition.calcium !== undefined ? currentNutrition.calcium : 0 }}</span>
            <span class="nutri-unit">mg</span>
          </div>
        </div>
      </div>

      <!-- 快捷食物标签 -->
      <div class="quick-foods">
        <span class="quick-label">⚡ 快捷选择：</span>
        <button v-for="food in quickFoods" :key="food.name" @click="selectQuickFood(food)" class="quick-tag">
          {{ food.name }}
          <span class="quick-cal">{{ food.calories }}大卡/100g</span>
        </button>
      </div>
    </div>

    <!-- 今日饮食记录列表 -->
    <div class="glass-card">
      <div class="card-header">
        <span class="card-icon">📋</span>
        <h4>今日饮食记录</h4>
        <button @click="exportDietData" class="export-btn">📤 导出Excel</button>
      </div>

      <div v-if="dailyDiet.records.length === 0" class="empty-state">
        <div class="empty-icon">🍽️</div>
        <p>还没有饮食记录</p>
        <p class="empty-hint">添加今天的食物吧~</p>
      </div>

      <div v-else class="record-list">
        <div v-for="record in dailyDiet.records" :key="record.id" class="record-item">
          <div class="record-info">
            <span class="meal-badge" :class="getMealClass(record.mealType)">
              {{ getMealName(record.mealType) }}
            </span>
            <span class="food-name">{{ record.foodName }}</span>
            <span class="food-weight">({{ record.quantity || 100 }}g)</span>
            <div class="record-nutrition-tags">
              <span class="nutri-tag">💪 {{ record.protein || 0 }}g</span>
              <span class="nutri-tag">🧈 {{ record.fat || 0 }}g</span>
              <span class="nutri-tag">🍚 {{ record.carbs || 0 }}g</span>
            </div>
          </div>
          <div class="record-actions">
            <span class="calorie-badge">🔥 {{ record.calories }} 大卡</span>
            <button @click="showNutritionDetail(record)" class="action-btn info" title="营养详情">📊</button>
            <button @click="deleteDietRecord(record.id)" class="action-btn delete" title="删除">🗑️</button>
          </div>
        </div>
      </div>

      <div v-if="dailyDiet.records.length > 0" class="stats-footer">
        <div class="stat-summary-item">
          <span class="summary-label">总热量</span>
          <span class="summary-value">{{ dailyDiet.totalCalories }} 大卡</span>
        </div>
        <div class="stat-summary-item">
          <span class="summary-label">剩余热量</span>
          <span class="summary-value">{{ remainingCalories }} 大卡</span>
        </div>
        <div class="stat-summary-item">
          <span class="summary-label">记录餐数</span>
          <span class="summary-value">{{ dailyDiet.records.length }} 餐</span>
        </div>
      </div>
    </div>

    <!-- 今日营养汇总卡片 -->
    <div class="glass-card" v-if="dailyDiet.records.length > 0">
      <div class="card-header">
        <span class="card-icon">🥗</span>
        <h4>今日营养分析</h4>
        <span class="update-time">实时更新</span>
      </div>
      <div class="nutrition-summary-grid">
        <div class="summary-item">
          <div class="summary-icon">💪</div>
          <div class="summary-info">
            <div class="summary-label">蛋白质</div>
            <div class="summary-value">{{ dailyNutritionTotal.protein }}<span class="unit">g</span></div>
            <div class="progress-bar"><div class="progress-fill" :style="{width: proteinPercent + '%', background: '#40E0D0'}"></div></div>
            <div class="summary-target">目标 {{ nutritionGoals.protein }}g</div>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">🧈</div>
          <div class="summary-info">
            <div class="summary-label">脂肪</div>
            <div class="summary-value">{{ dailyNutritionTotal.fat }}<span class="unit">g</span></div>
            <div class="progress-bar"><div class="progress-fill" :style="{width: fatPercent + '%', background: '#F59E0B'}"></div></div>
            <div class="summary-target">目标 {{ nutritionGoals.fat }}g</div>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">🍚</div>
          <div class="summary-info">
            <div class="summary-label">碳水</div>
            <div class="summary-value">{{ dailyNutritionTotal.carbs }}<span class="unit">g</span></div>
            <div class="progress-bar"><div class="progress-fill" :style="{width: carbsPercent + '%', background: '#10B981'}"></div></div>
            <div class="summary-target">目标 {{ nutritionGoals.carbs }}g</div>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">🌾</div>
          <div class="summary-info">
            <div class="summary-label">膳食纤维</div>
            <div class="summary-value">{{ dailyNutritionTotal.fiber }}<span class="unit">g</span></div>
            <div class="progress-bar"><div class="progress-fill" :style="{width: fiberPercent + '%', background: '#8B5CF6'}"></div></div>
            <div class="summary-target">目标 {{ nutritionGoals.fiber }}g</div>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">🍬</div>
          <div class="summary-info">
            <div class="summary-label">糖</div>
            <div class="summary-value">{{ dailyNutritionTotal.sugar }}<span class="unit">g</span></div>
            <div class="progress-bar"><div class="progress-fill" :style="{width: sugarPercent + '%', background: '#EF4444'}"></div></div>
            <div class="summary-target">目标 {{ nutritionGoals.sugar }}g</div>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">🧂</div>
          <div class="summary-info">
            <div class="summary-label">钠</div>
            <div class="summary-value">{{ dailyNutritionTotal.sodium || 0 }}<span class="unit">mg</span></div>
            <div class="progress-bar"><div class="progress-fill" :style="{width: sodiumPercent + '%', background: '#3B82F6'}"></div></div>
            <div class="summary-target">上限 {{ nutritionGoals.sodium }}mg</div>
          </div>
        </div>
        <div class="summary-item">
          <div class="summary-icon">🦴</div>
          <div class="summary-info">
            <div class="summary-label">钙</div>
            <div class="summary-value">{{ dailyNutritionTotal.calcium || 0 }}<span class="unit">mg</span></div>
            <div class="progress-bar"><div class="progress-fill" :style="{width: calciumPercent + '%', background: '#EC4899'}"></div></div>
            <div class="summary-target">目标 {{ nutritionGoals.calcium }}mg</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 营养详情弹窗 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="showDetailModal = false">
      <div class="modal-content glass" @click.stop>
        <div class="modal-header">
          <span class="modal-icon">📊</span>
          <h3>{{ selectedRecord?.foodName }} 营养详情</h3>
          <button class="modal-close" @click="showDetailModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="detail-row">
            <span class="detail-label">重量：</span>
            <span class="detail-value">{{ selectedRecord?.quantity }}g</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">🔥 热量：</span>
            <span class="detail-value">{{ selectedRecord?.calories }} 大卡</span>
          </div>
          <div class="detail-divider"></div>
          <div class="detail-grid">
            <div class="detail-item"><span class="detail-icon">💪</span> 蛋白质 <span class="detail-num">{{ selectedRecord?.protein || 0 }}g</span></div>
            <div class="detail-item"><span class="detail-icon">🧈</span> 脂肪 <span class="detail-num">{{ selectedRecord?.fat || 0 }}g</span></div>
            <div class="detail-item"><span class="detail-icon">🍚</span> 碳水 <span class="detail-num">{{ selectedRecord?.carbs || 0 }}g</span></div>
            <div class="detail-item"><span class="detail-icon">🌾</span> 膳食纤维 <span class="detail-num">{{ selectedRecord?.fiber || 0 }}g</span></div>
            <div class="detail-item"><span class="detail-icon">🍬</span> 糖 <span class="detail-num">{{ selectedRecord?.sugar || 0 }}g</span></div>
            <div class="detail-item"><span class="detail-icon">🧂</span> 钠 <span class="detail-num">{{ selectedRecord?.sodium || 0 }}mg</span></div>
            <div class="detail-item"><span class="detail-icon">🦴</span> 钙 <span class="detail-num">{{ selectedRecord?.calcium || 0 }}mg</span></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import axios from 'axios'

const props = defineProps({ userId: Number })
const API_BASE = 'http://localhost:8080/api'

// 数据
const dailyDiet = ref({ records: [], totalCalories: 0 })
const dietForm = ref({
  mealType: 'BREAKFAST',
  foodName: '',
  weight: 100,
  caloriesPer100g: ''
})

// 弹窗
const showDetailModal = ref(false)
const selectedRecord = ref(null)

// 营养目标值（从后端动态获取）
const nutritionGoals = ref({
  protein: 60,
  fat: 50,
  carbs: 200,
  fiber: 25,
  sugar: 30,
  sodium: 2000,
  calcium: 800
})

const targetCalories = ref(2000)

// 加载用户营养目标
const loadNutritionGoal = async () => {
  if (!props.userId) return
  try {
    const res = await axios.get(`${API_BASE}/user/nutrition-goal/${props.userId}`)
    if (res.data.success) {
      nutritionGoals.value = {
        protein: res.data.protein,
        fat: res.data.fat,
        carbs: res.data.carbs,
        fiber: res.data.fiber,
        sugar: res.data.sugar,
        sodium: res.data.sodium,
        calcium: res.data.calcium
      }
      targetCalories.value = res.data.calories
    }
  } catch (e) {
    console.error('加载营养目标失败', e)
  }
}

// 食物营养数据库（每100g含量）
const foodNutritionDB = {
  '鸡胸肉': { protein: 31, fat: 3.6, carbs: 0, fiber: 0, sugar: 0, sodium: 74, calcium: 15, calories: 165 },
  '鸡胸': { protein: 31, fat: 3.6, carbs: 0, fiber: 0, sugar: 0, sodium: 74, calcium: 15, calories: 165 },
  '苹果': { protein: 0.3, fat: 0.2, carbs: 13.8, fiber: 2.4, sugar: 10.4, sodium: 1, calcium: 6, calories: 52 },
  '香蕉': { protein: 1.1, fat: 0.3, carbs: 22.8, fiber: 2.6, sugar: 12.2, sodium: 1, calcium: 5, calories: 89 },
  '米饭': { protein: 2.5, fat: 0.3, carbs: 25.6, fiber: 0.4, sugar: 0.1, sodium: 1, calcium: 5, calories: 116 },
  '鸡蛋': { protein: 6.3, fat: 4.8, carbs: 0.6, fiber: 0, sugar: 0.6, sodium: 70, calcium: 28, calories: 70 },
  '牛奶': { protein: 3.2, fat: 3.3, carbs: 4.8, fiber: 0, sugar: 4.8, sodium: 44, calcium: 120, calories: 60 },
  '全麦面包': { protein: 8, fat: 1.5, carbs: 42, fiber: 6, sugar: 2, sodium: 200, calcium: 50, calories: 80 },
  '沙拉': { protein: 1.5, fat: 0.3, carbs: 5, fiber: 1.5, sugar: 2.5, sodium: 15, calcium: 20, calories: 45 },
  '牛肉': { protein: 26, fat: 12, carbs: 0, fiber: 0, sugar: 0, sodium: 50, calcium: 10, calories: 250 },
  '三文鱼': { protein: 20, fat: 13, carbs: 0, fiber: 0, sugar: 0, sodium: 50, calcium: 10, calories: 208 },
  '豆腐': { protein: 8, fat: 4, carbs: 2, fiber: 0.5, sugar: 0.5, sodium: 5, calcium: 130, calories: 76 },
  '西兰花': { protein: 2.8, fat: 0.4, carbs: 7, fiber: 2.6, sugar: 1.7, sodium: 33, calcium: 47, calories: 34 },
  '红薯': { protein: 1.6, fat: 0.1, carbs: 20, fiber: 3, sugar: 4.2, sodium: 55, calcium: 30, calories: 86 },
  '鸡腿': { protein: 25, fat: 8, carbs: 0, fiber: 0, sugar: 0, sodium: 70, calcium: 10, calories: 180 },
  '猪肉': { protein: 20, fat: 15, carbs: 0, fiber: 0, sugar: 0, sodium: 50, calcium: 5, calories: 242 },
  '鱼肉': { protein: 18, fat: 5, carbs: 0, fiber: 0, sugar: 0, sodium: 60, calcium: 15, calories: 120 },
  '豆浆': { protein: 3, fat: 1.8, carbs: 1.8, fiber: 0.5, sugar: 1, sodium: 5, calcium: 25, calories: 54 }
}

// 计算营养成分
const calculateNutritionByWeight = (foodName, weight) => {
  if (!foodName || !weight || weight <= 0) return null

  let nutrition = null
  if (foodNutritionDB[foodName]) {
    nutrition = foodNutritionDB[foodName]
  } else {
    for (const [key, value] of Object.entries(foodNutritionDB)) {
      if (foodName.includes(key)) {
        nutrition = value
        break
      }
    }
  }

  if (!nutrition) return null

  const ratio = weight / 100
  return {
    protein: (nutrition.protein * ratio).toFixed(1),
    fat: (nutrition.fat * ratio).toFixed(1),
    carbs: (nutrition.carbs * ratio).toFixed(1),
    fiber: (nutrition.fiber * ratio).toFixed(1),
    sugar: (nutrition.sugar * ratio).toFixed(1),
    sodium: Math.round(nutrition.sodium * ratio),
    calcium: Math.round(nutrition.calcium * ratio),
    calories: Math.round(nutrition.calories * ratio)
  }
}

// 当前食物的营养预览
const currentNutrition = ref(null)

const updateCurrentNutrition = () => {
  const weight = parseFloat(dietForm.value.weight)
  currentNutrition.value = calculateNutritionByWeight(dietForm.value.foodName, weight)
}

// 计算每日营养总和
const dailyNutritionTotal = computed(() => {
  const records = dailyDiet.value.records || []
  const totals = { protein: 0, fat: 0, carbs: 0, fiber: 0, sugar: 0, sodium: 0, calcium: 0 }
  records.forEach(record => {
    totals.protein += parseFloat(record.protein) || 0
    totals.fat += parseFloat(record.fat) || 0
    totals.carbs += parseFloat(record.carbs) || 0
    totals.fiber += parseFloat(record.fiber) || 0
    totals.sugar += parseFloat(record.sugar) || 0
    totals.sodium += parseInt(record.sodium) || 0
    totals.calcium += parseInt(record.calcium) || 0
  })
  return {
    protein: totals.protein.toFixed(1),
    fat: totals.fat.toFixed(1),
    carbs: totals.carbs.toFixed(1),
    fiber: totals.fiber.toFixed(1),
    sugar: totals.sugar.toFixed(1),
    sodium: totals.sodium,
    calcium: totals.calcium
  }
})

// 百分比计算
const proteinPercent = computed(() => Math.min(100, (parseFloat(dailyNutritionTotal.value.protein) / nutritionGoals.value.protein) * 100))
const fatPercent = computed(() => Math.min(100, (parseFloat(dailyNutritionTotal.value.fat) / nutritionGoals.value.fat) * 100))
const carbsPercent = computed(() => Math.min(100, (parseFloat(dailyNutritionTotal.value.carbs) / nutritionGoals.value.carbs) * 100))
const fiberPercent = computed(() => Math.min(100, (parseFloat(dailyNutritionTotal.value.fiber) / nutritionGoals.value.fiber) * 100))
const sugarPercent = computed(() => Math.min(100, (parseFloat(dailyNutritionTotal.value.sugar) / nutritionGoals.value.sugar) * 100))
const sodiumPercent = computed(() => Math.min(100, (dailyNutritionTotal.value.sodium / nutritionGoals.value.sodium) * 100))
const calciumPercent = computed(() => Math.min(100, (dailyNutritionTotal.value.calcium / nutritionGoals.value.calcium) * 100))

// 餐别下拉菜单
const mealOptions = [
  { value: 'BREAKFAST', label: '🍳 早餐' },
  { value: 'LUNCH', label: '🍱 午餐' },
  { value: 'DINNER', label: '🍲 晚餐' },
  { value: 'SNACK', label: '🍪 加餐' }
]
const selectedMealLabel = ref('🍳 早餐')
const showMealDropdown = ref(false)
const mealSelectRef = ref(null)

const onMealInput = () => {}

const selectMeal = (item) => {
  dietForm.value.mealType = item.value
  selectedMealLabel.value = item.label
  showMealDropdown.value = false
}

const calculateCalories = () => {}

// 图片上传相关
const fileInput = ref(null)
const previewImage = ref('')
const uploading = ref(false)
const recognizeResult = ref(null)
const showCandidates = ref(false)

const quickFoods = [
  { name: '🍎 苹果', calories: 52 },
  { name: '🍌 香蕉', calories: 89 },
  { name: '🍚 米饭', calories: 116 },
  { name: '🥚 鸡蛋', calories: 70 },
  { name: '🥛 牛奶', calories: 60 },
  { name: '🍞 全麦面包', calories: 80 },
  { name: '🥗 沙拉', calories: 45 },
  { name: '🍗 鸡胸肉', calories: 165 }
]

const calculatedCalories = computed(() => {
  const weight = parseFloat(dietForm.value.weight) || 0
  const caloriesPer100g = parseFloat(dietForm.value.caloriesPer100g) || 0
  if (weight > 0 && caloriesPer100g > 0) {
    return Math.round((caloriesPer100g / 100) * weight)
  }
  if (currentNutrition.value && currentNutrition.value.calories) {
    return currentNutrition.value.calories
  }
  return 0
})

const calorieProgress = computed(() => {
  const total = dailyDiet.value.totalCalories || 0
  return Math.min(100, Math.round((total / targetCalories.value) * 100))
})

const remainingCalories = computed(() => {
  const total = dailyDiet.value.totalCalories || 0
  return Math.max(0, targetCalories.value - total)
})

const showNutritionDetail = (record) => {
  selectedRecord.value = record
  showDetailModal.value = true
}

const triggerFileUpload = () => {
  fileInput.value.click()
}

const handleImageUpload = async (e) => {
  const file = e.target.files[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (event) => {
    previewImage.value = event.target.result
  }
  reader.readAsDataURL(file)

  uploading.value = true
  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await axios.post(`${API_BASE}/diet/recognize`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    console.log('后端返回的识别结果:', res.data)

    if (res.data.success) {
      if (res.data.candidates && res.data.candidates.length > 0) {
        recognizeResult.value = {
          foodName: res.data.foodName,
          estimatedCalories: res.data.estimatedCalories,
          candidates: res.data.candidates
        }
        showCandidates.value = true
      } else {
        recognizeResult.value = {
          foodName: res.data.foodName,
          estimatedCalories: res.data.estimatedCalories,
          candidates: []
        }
        showCandidates.value = false
      }
    } else {
      previewImage.value = ''
      recognizeResult.value = null
      alert('识别失败：' + (res.data.message || '未知错误'))
    }
  } catch (error) {
    console.error('识别失败', error)
    previewImage.value = ''
    recognizeResult.value = null
    alert('识别失败，请检查网络')
  } finally {
    uploading.value = false
  }
}

const selectCandidate = (candidate) => {
  console.log('选中的候选食物:', candidate)
  dietForm.value.foodName = candidate.name
  // 尝试多种可能的字段名
  const caloriesPer100g = candidate.calories || candidate.estimatedCalories || 100
  dietForm.value.caloriesPer100g = caloriesPer100g
  dietForm.value.weight = 100
  recognizeResult.value = null
  previewImage.value = ''
  showCandidates.value = false
  updateCurrentNutrition()
  console.log('设置后的dietForm:', dietForm.value)
}

const applyRecognizeResult = () => {
  if (recognizeResult.value) {
    console.log('应用识别结果:', recognizeResult.value)
    dietForm.value.foodName = recognizeResult.value.foodName
    const caloriesPer100g = recognizeResult.value.estimatedCalories || 100
    dietForm.value.caloriesPer100g = caloriesPer100g
    dietForm.value.weight = 100
    clearRecognizeResult()
    updateCurrentNutrition()
    console.log('设置后的dietForm:', dietForm.value)
  }
}

const clearRecognizeResult = () => {
  recognizeResult.value = null
  previewImage.value = ''
  showCandidates.value = false
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const cancelPreview = () => {
  previewImage.value = ''
  recognizeResult.value = null
  showCandidates.value = false
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

const selectQuickFood = (food) => {
  dietForm.value.foodName = food.name
  dietForm.value.caloriesPer100g = food.calories
  dietForm.value.weight = 100
  updateCurrentNutrition()
}

const loadDailyDiet = async () => {
  if (!props.userId) return
  try {
    const res = await axios.get(`${API_BASE}/diet/daily/${props.userId}`)
    dailyDiet.value = res.data
  } catch (e) {
    console.error('加载饮食记录失败', e)
  }
}

const addDietRecord = async () => {
  if (!dietForm.value.foodName) {
    alert('请填写食物名称')
    return
  }

  // 重新计算热量
  const weight = parseFloat(dietForm.value.weight) || 0
  const caloriesPer100g = parseFloat(dietForm.value.caloriesPer100g) || 0
  let calories = 0

  if (weight > 0 && caloriesPer100g > 0) {
    calories = Math.round((caloriesPer100g / 100) * weight)
  } else if (currentNutrition.value && currentNutrition.value.calories) {
    calories = currentNutrition.value.calories
  }

  if (calories <= 0) {
    alert('请填写有效的热量（每100g热量）或重量')
    return
  }

  const nutrition = calculateNutritionByWeight(dietForm.value.foodName, weight)

  try {
    await axios.post(`${API_BASE}/diet/add`, null, {
      params: {
        userId: props.userId,
        mealType: dietForm.value.mealType,
        foodName: dietForm.value.foodName,
        quantity: dietForm.value.weight,
        calories: calories,
        protein: nutrition?.protein || 0,
        fat: nutrition?.fat || 0,
        carbs: nutrition?.carbs || 0,
        fiber: nutrition?.fiber || 0,
        sugar: nutrition?.sugar || 0,
        sodium: nutrition?.sodium || 0,
        calcium: nutrition?.calcium || 0
      }
    })
    // 重置表单
    dietForm.value = {
      mealType: 'BREAKFAST',
      foodName: '',
      weight: 100,
      caloriesPer100g: ''
    }
    selectedMealLabel.value = '🍳 早餐'
    currentNutrition.value = null
    await loadDailyDiet()
    alert('添加成功')
  } catch (e) {
    console.error('添加失败', e)
    alert('添加失败')
  }
}

const deleteDietRecord = async (recordId) => {
  if (!confirm('确定要删除这条记录吗？')) return
  try {
    await axios.delete(`${API_BASE}/diet/delete/${recordId}`, {
      params: { userId: props.userId }
    })
    await loadDailyDiet()
  } catch (e) {
    alert('删除失败')
  }
}

const exportDietData = () => window.open(`${API_BASE}/export/diet/${props.userId}`)

const getMealName = (type) => {
  const map = { 'BREAKFAST': '早餐', 'LUNCH': '午餐', 'DINNER': '晚餐', 'SNACK': '加餐' }
  return map[type] || type
}

const getMealClass = (type) => {
  const map = {
    'BREAKFAST': 'meal-breakfast',
    'LUNCH': 'meal-lunch',
    'DINNER': 'meal-dinner',
    'SNACK': 'meal-snack'
  }
  return map[type] || 'meal-default'
}

const handleClickOutside = (e) => {
  if (mealSelectRef.value && !mealSelectRef.value.contains(e.target)) {
    showMealDropdown.value = false
  }
}

watch([() => dietForm.value.foodName, () => dietForm.value.weight], () => {
  updateCurrentNutrition()
})

onMounted(() => {
  if (props.userId) {
    loadDailyDiet()
    loadNutritionGoal()
  }
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.diet-container {
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
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.stat-card {
  text-align: center;
  padding: 20px;
  border-radius: 20px;
}

.stat-value {
  font-size: 36px;
  font-weight: 700;
  color: #40E0D0;
}

.stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
  margin-top: 8px;
}

.progress-bar {
  margin-top: 12px;
  height: 4px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #40E0D0;
  border-radius: 2px;
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

.update-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
}

.form-row.four-columns {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr auto auto;
  gap: 16px;
  align-items: center;
}

@media (max-width: 900px) {
  .form-row.four-columns {
    grid-template-columns: 1fr;
    gap: 12px;
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
  box-sizing: border-box;
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

.dropdown-item {
  padding: 12px 16px;
  cursor: pointer;
  color: white;
  font-size: 14px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  transition: all 0.2s;
}

.dropdown-item:hover {
  background: rgba(64, 224, 208, 0.2);
  color: #40E0D0;
}

.dropdown-item:last-child {
  border-bottom: none;
}

.cal-preview {
  background: rgba(64, 224, 208, 0.15);
  border-radius: 40px;
  padding: 12px 20px;
  font-size: 13px;
  color: #40E0D0;
  white-space: nowrap;
  text-align: center;
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

.nutrition-preview {
  margin-top: 20px;
  padding: 16px 20px;
  border-radius: 20px;
  background: rgba(64, 224, 208, 0.08);
  border: 1px solid rgba(64, 224, 208, 0.2);
}

.nutrition-title {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 12px;
}

.nutrition-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.nutrition-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
  background: rgba(255, 255, 255, 0.05);
  padding: 6px 12px;
  border-radius: 30px;
}

.nutri-icon {
  font-size: 14px;
}

.nutri-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.nutri-value {
  font-size: 16px;
  font-weight: 600;
  color: #40E0D0;
}

.nutri-unit {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.4);
}

.quick-foods {
  margin-top: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.quick-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.quick-tag {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 30px;
  padding: 6px 14px;
  font-size: 12px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.quick-tag:hover {
  background: rgba(64, 224, 208, 0.2);
  border-color: #40E0D0;
  color: #40E0D0;
}

.quick-cal {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
}

.nutrition-summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
}

.summary-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
}

.summary-icon {
  font-size: 28px;
}

.summary-info {
  flex: 1;
}

.summary-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.summary-value {
  font-size: 18px;
  font-weight: 600;
  color: white;
}

.summary-value .unit {
  font-size: 11px;
  font-weight: normal;
  color: rgba(255, 255, 255, 0.4);
  margin-left: 2px;
}

.summary-target {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.35);
  margin-top: 4px;
}

.upload-area {
  background: rgba(255, 255, 255, 0.05);
  border: 2px dashed rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  margin-bottom: 20px;
  transition: all 0.2s;
}

.upload-area:hover {
  border-color: #40E0D0;
  background: rgba(255, 255, 255, 0.08);
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-icon {
  font-size: 32px;
}

.recognize-result {
  border-radius: 16px;
  padding: 12px;
  margin-bottom: 20px;
  display: flex;
  gap: 12px;
}

.result-icon {
  font-size: 32px;
}

.result-food {
  font-size: 14px;
  font-weight: 500;
  color: #40E0D0;
}

.result-calories {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.candidates-list {
  width: 100%;
}

.candidates-title {
  font-size: 12px;
  color: #40E0D0;
  margin-bottom: 12px;
}

.candidates-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  max-height: 200px;
  overflow-y: auto;
}

.candidate-btn {
  padding: 8px 14px;
  border-radius: 30px;
  font-size: 12px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  color: white;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  transition: all 0.2s;
}

.candidate-btn:hover {
  background: rgba(64, 224, 208, 0.2);
  border-color: #40E0D0;
}

.candidate-cal {
  font-size: 10px;
  color: #40E0D0;
}

.result-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.apply-btn, .cancel-btn {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  border: none;
}

.apply-btn {
  background: rgba(64, 224, 208, 0.2);
  color: #40E0D0;
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.7);
}

.export-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 40px;
  padding: 6px 16px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
}

.export-btn:hover {
  border-color: #40E0D0;
  background: rgba(255, 255, 255, 0.12);
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.record-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.record-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.record-nutrition-tags {
  display: flex;
  gap: 8px;
  margin-left: 8px;
}

.nutri-tag {
  font-size: 11px;
  padding: 2px 8px;
  background: rgba(64, 224, 208, 0.15);
  border-radius: 20px;
  color: #40E0D0;
}

.meal-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 500;
}

.meal-breakfast {
  background: rgba(255, 152, 0, 0.2);
  color: #FF9800;
}

.meal-lunch {
  background: rgba(33, 150, 243, 0.2);
  color: #64B5F6;
}

.meal-dinner {
  background: rgba(76, 175, 80, 0.2);
  color: #81C784;
}

.meal-snack {
  background: rgba(156, 39, 176, 0.2);
  color: #CE93D8;
}

.food-name {
  font-size: 15px;
  color: white;
}

.food-weight {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.record-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.calorie-badge {
  font-size: 13px;
  color: #40E0D0;
  font-weight: 500;
}

.action-btn {
  background: none;
  border: none;
  font-size: 16px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 20px;
  transition: all 0.2s;
}

.action-btn.delete {
  background: rgba(229, 115, 115, 0.15);
  color: #ff8888;
}

.action-btn.info {
  background: rgba(64, 224, 208, 0.15);
  color: #40E0D0;
}

.action-btn:hover {
  transform: scale(1.05);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p {
  color: rgba(255, 255, 255, 0.5);
  margin: 8px 0;
}

.empty-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.3);
}

.stats-footer {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.stat-summary-item {
  text-align: center;
}

.summary-label {
  display: block;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 4px;
}

.summary-value {
  font-size: 18px;
  font-weight: 600;
  color: #40E0D0;
}

.loading-icon {
  display: inline-block;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.preview-area {
  position: relative;
  display: inline-block;
}

.preview-img {
  max-height: 80px;
  border-radius: 12px;
}

.cancel-preview {
  position: absolute;
  top: -8px;
  right: -8px;
  background: rgba(0,0,0,0.6);
  border: none;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  color: white;
  cursor: pointer;
  font-size: 14px;
}

.uploading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #40E0D0;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000000;
}

.modal-content {
  width: 90%;
  max-width: 380px;
  border-radius: 28px;
  padding: 0;
  overflow: hidden;
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
}

.modal-body {
  padding: 20px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
}

.detail-label {
  color: rgba(255, 255, 255, 0.5);
}

.detail-value {
  color: #40E0D0;
  font-weight: 500;
}

.detail-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.1);
  margin: 12px 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  font-size: 13px;
}

.detail-icon {
  font-size: 16px;
}

.detail-num {
  margin-left: auto;
  font-weight: 600;
  color: #40E0D0;
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
</style>