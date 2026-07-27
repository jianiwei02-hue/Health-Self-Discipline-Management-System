<template>
  <div class="muscle-heatmap-wrapper">
    <div class="muscle-3d-container">
      <div ref="canvasContainer" class="canvas-container"></div>

      <div class="gender-switch">
        <button :class="{ active: currentGender === 'male' }" @click="switchGender('male')">♂ 男性</button>
        <button :class="{ active: currentGender === 'female' }" @click="switchGender('female')">♀ 女性</button>
      </div>

      <div class="title-overlay">
        <h3>💪 全身肌肉热力图</h3>
        <p>点击任意部位查看肌肉详情</p>
      </div>

      <div class="controls-tip">
        <span>🖱️ 拖拽旋转视角</span>
        <span>✨ 正面/背面均可识别</span>
      </div>
    </div>

    <div class="muscle-report">
      <div class="report-header">
        <h2>📊 肌肉分析报告</h2>
        <span class="current-gender">{{ currentGender === 'male' ? '男性' : '女性' }}</span>
      </div>

      <div class="current-muscle" v-if="currentMuscle.name">
        <div class="muscle-icon">{{ getMuscleIcon(currentMuscle.name) }}</div>
        <div class="muscle-info">
          <h4>{{ currentMuscle.name }}</h4>
          <div class="intensity-bar"><div class="intensity-fill" :style="{ width: currentMuscle.percent + '%' }"></div></div>
          <p>{{ currentMuscle.intensity }}</p>
        </div>
      </div>

      <div class="current-muscle empty" v-else>
        <div class="muscle-icon">👉</div>
        <div class="muscle-info">
          <h4>点击任意肌肉</h4>
          <p>点击左侧 3D 模型上的部位查看详情</p>
        </div>
      </div>

      <div class="training-tips">
        <h4>📌 今日推荐训练</h4>
        <ul>
          <li v-if="weakMuscles.length">🔥 加强 {{ weakMuscles.slice(0, 3).join('、') }}，提升力量平衡</li>
          <li v-else>🏆 全身发展均衡，继续保持</li>
          <li>💧 今日饮水目标 2000ml</li>
          <li>😴 建议睡眠 7.5 小时</li>
        </ul>
      </div>

      <div class="muscle-progress">
        <h4>📊 全身肌肉发展</h4>
        <div class="progress-bars">
          <div class="progress-item" v-for="(value, muscle) in muscleData" :key="muscle">
            <span>{{ getMuscleCnName(muscle) }}</span>
            <div class="progress-bar"><div class="progress-fill" :style="{ width: Math.min((value / 10) * 100, 100) + '%' }"></div></div>
            <span class="progress-value">{{ value }}次</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { GLTFLoader } from 'three/examples/jsm/loaders/GLTFLoader.js'

const props = defineProps({
  muscleData: {
    type: Object,
    default: () => ({
      chest: 5, abs: 4, shoulders: 3, lats: 1, traps: 2,
      biceps: 3, triceps: 2, forearms: 1,
      quads: 8, hamstrings: 4, calves: 6, glutes: 2
    })
  }
})

const canvasContainer = ref(null)
const currentMuscle = ref({ name: '', intensity: '', percent: 0 })
const currentGender = ref('male')

let scene, camera, renderer, controls, model, raycaster, animationId
let resizeHandler = null
let modelBox = null

const muscleNameMap = {
  chest: '胸肌', abs: '腹肌', shoulders: '三角肌', lats: '背阔肌', traps: '斜方肌',
  biceps: '肱二头肌', triceps: '肱三头肌', forearms: '前臂',
  quads: '股四头肌', hamstrings: '腘绳肌', calves: '小腿', glutes: '臀肌'
}

const getMuscleCnName = (key) => muscleNameMap[key] || key

const getMuscleIcon = (name) => {
  const icons = {
    '胸肌': '💪', '腹肌': '🔥', '三角肌': '🏋️', '背阔肌': '🔙', '斜方肌': '🦾',
    '肱二头肌': '💪', '肱三头肌': '💪', '前臂': '✋', '股四头肌': '🦵',
    '腘绳肌': '🦵', '小腿': '🦵', '臀肌': '🍑'
  }
  return icons[name] || '💪'
}

const getIntensityText = (count) => {
  if (count >= 8) return '高强度 🔥'
  if (count >= 5) return '中高强度 💪'
  if (count >= 3) return '中等强度 ⚡'
  if (count >= 1) return '低强度 🌱'
  return '待加强 📌'
}

const getIntensityPercent = (count) => {
  if (count >= 8) return 90
  if (count >= 5) return 70
  if (count >= 3) return 50
  if (count >= 1) return 30
  return 10
}

const weakMuscles = computed(() => {
  const weak = []
  for (const [muscle, val] of Object.entries(props.muscleData)) {
    if (val < 3) weak.push(getMuscleCnName(muscle))
  }
  return weak
})

const updateModelBox = () => {
  if (!model) return
  const box = new THREE.Box3().setFromObject(model)
  modelBox = box
}

const getNormalizedPoint = (point) => {
  if (!modelBox) return { x: 0, y: 0, z: 0 }

  const localPoint = point.clone()
  const range = new THREE.Vector3()
  modelBox.getSize(range)
  const center = new THREE.Vector3()
  modelBox.getCenter(center)

  const nx = (localPoint.x - center.x) / range.x
  const ny = (localPoint.y - center.y) / range.y
  const nz = (localPoint.z - center.z) / range.z

  return { x: nx, y: ny, z: nz }
}

// ========== 只修改了正面区域的腹肌和股四头肌范围 ==========
const getMuscleFromPoint = (point) => {
  const { x, y, z } = getNormalizedPoint(point)

  console.log(`归一化坐标: x=${x.toFixed(2)}, y=${y.toFixed(2)}, z=${z.toFixed(2)}`)

  // ========== 手臂区域 ==========
  const isArm = Math.abs(x) > 0.25

  if (isArm) {
    // 上臂区域
    if (y > -0.15 && y < 0.45) {
      if (z > -0.1) return 'biceps'
      else return 'triceps'
    }
    // 前臂区域
    if (y <= -0.15 && y > -0.55) return 'forearms'
  }

  // ========== 正面区域 ==========
  if (z > 0) {
    // 胸部
    if (y > 0.15 && y < 0.55 && Math.abs(x) < 0.38) return 'chest'
    // 腹部（调整范围：y > 0.05 到 0.15，只覆盖腹部，不覆盖大腿）
    if (y > 0.05 && y <= 0.15 && Math.abs(x) < 0.4) return 'abs'
    // 三角肌
    if (y > 0.45 && y < 0.75 && (x > 0.32 || x < -0.32)) return 'shoulders'
    // 股四头肌（大腿前侧）- 范围 y > -0.55 到 0.05，覆盖整个大腿区域
    if (y > -0.55 && y <= 0.05 && Math.abs(x) < 0.4) return 'quads'
    // 小腿前侧
    if (y > -0.85 && y <= -0.55 && Math.abs(x) < 0.38) return 'calves'
  }

  // ========== 背面区域 ==========
  else {
    // 斜方肌（上背部）
    if (y > 0.45 && y < 0.75 && Math.abs(x) < 0.42) return 'traps'
    // 背阔肌（中背部）
    if (y > 0.1 && y <= 0.45 && Math.abs(x) < 0.48) return 'lats'
    // 下背部（腰部）
    if (y > 0.03 && y <= 0.1 && Math.abs(x) < 0.42) return 'lats'
    // 臀部
    if (y > -0.08 && y <= 0.03 && Math.abs(x) < 0.42) return 'glutes'
    // 大腿后侧（腘绳肌）
    if (y > -0.28 && y <= -0.08 && Math.abs(x) < 0.45) return 'hamstrings'
    // 小腿后侧
    if (y > -0.7 && y <= -0.28 && Math.abs(x) < 0.38) return 'calves'
  }

  return null
}

const switchGender = (gender) => {
  currentGender.value = gender
  if (model) scene.remove(model)

  const loader = new GLTFLoader()
  const modelPath = `/models/${gender === 'male' ? 'male.glb' : 'female.glb'}`

  const loadingDiv = document.createElement('div')
  loadingDiv.textContent = '加载模型中...'
  loadingDiv.style.position = 'absolute'
  loadingDiv.style.top = '50%'
  loadingDiv.style.left = '50%'
  loadingDiv.style.transform = 'translate(-50%, -50%)'
  loadingDiv.style.color = '#40E0D0'
  loadingDiv.style.zIndex = '100'
  canvasContainer.value?.appendChild(loadingDiv)

  loader.load(modelPath, (gltf) => {
    if (loadingDiv) loadingDiv.remove()
    model = gltf.scene
    scene.add(model)
    model.scale.set(0.18, 0.18, 0.18)
    model.position.set(0, 0.05, 0)

    setTimeout(() => {
      updateModelBox()
    }, 100)

    console.log(`${gender}模型加载成功`)
  }, undefined, (error) => {
    if (loadingDiv) loadingDiv.remove()
    console.error('模型加载失败:', error)
  })
}

const initScene = () => {
  if (!canvasContainer.value) return

  const width = canvasContainer.value.clientWidth
  const height = canvasContainer.value.clientHeight
  if (width === 0 || height === 0) {
    setTimeout(initScene, 200)
    return
  }

  scene = new THREE.Scene()
  scene.background = null

  camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000)
  camera.position.set(0, 0.8, 5.0)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(width, height)
  renderer.setClearColor(0x000000, 0)
  canvasContainer.value.appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.target.set(0, 0.3, 0)
  controls.autoRotate = true
  controls.autoRotateSpeed = 0.5
  controls.enableZoom = false
  controls.enablePan = false

  raycaster = new THREE.Raycaster()

  const ambientLight = new THREE.AmbientLight(0xffffff, 0.7)
  scene.add(ambientLight)

  const mainLight = new THREE.DirectionalLight(0xffffff, 1.2)
  mainLight.position.set(0, 2, 3)
  scene.add(mainLight)

  const backLight = new THREE.DirectionalLight(0xffffff, 0.6)
  backLight.position.set(0, 1, -3)
  scene.add(backLight)

  const fillLight = new THREE.PointLight(0x40E0D0, 0.4)
  fillLight.position.set(1, 1, 1)
  scene.add(fillLight)

  const rimLight = new THREE.PointLight(0x40E0D0, 0.3)
  rimLight.position.set(-1, 1, -2)
  scene.add(rimLight)

  switchGender('male')

  const onClick = (event) => {
    if (!model) return

    const rect = renderer.domElement.getBoundingClientRect()
    const mouse = new THREE.Vector2()
    mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
    mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

    raycaster.setFromCamera(mouse, camera)
    const intersects = raycaster.intersectObjects(model.children, true)

    if (intersects.length > 0) {
      const point = intersects[0].point
      const muscleKey = getMuscleFromPoint(point)

      if (muscleKey) {
        const count = props.muscleData[muscleKey] || 0
        currentMuscle.value = {
          name: getMuscleCnName(muscleKey),
          intensity: getIntensityText(count),
          percent: getIntensityPercent(count)
        }
        console.log(`✅ 识别到: ${muscleKey}`)
      } else {
        console.log(`❌ 未识别`)
      }
    }
  }
  renderer.domElement.addEventListener('click', onClick)

  const animate = () => {
    animationId = requestAnimationFrame(animate)
    controls.update()
    renderer.render(scene, camera)
  }
  animate()
}

onMounted(() => {
  setTimeout(initScene, 200)

  resizeHandler = () => {
    if (renderer && camera && canvasContainer.value) {
      const w = canvasContainer.value.clientWidth
      const h = canvasContainer.value.clientHeight
      if (w > 0 && h > 0) {
        camera.aspect = w / h
        camera.updateProjectionMatrix()
        renderer.setSize(w, h)
      }
    }
  }
  window.addEventListener('resize', resizeHandler)
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
  if (renderer) renderer.dispose()
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
})
</script>

<style scoped>
.muscle-heatmap-wrapper {
  display: flex;
  gap: 24px;
  min-height: 650px;
  background: transparent;
  border-radius: 24px;
}

.muscle-3d-container {
  position: relative;
  flex: 2;
  border-radius: 24px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(64, 224, 208, 0.2);
  height: 650px;
}

.canvas-container {
  width: 100%;
  height: 100%;
}

.gender-switch {
  position: absolute;
  top: 20px;
  right: 20px;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
  border-radius: 40px;
  padding: 6px;
  display: flex;
  gap: 4px;
  z-index: 20;
  border: 1px solid rgba(64, 224, 208, 0.3);
}

.gender-switch button {
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.8);
  padding: 8px 20px;
  border-radius: 32px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.gender-switch button.active {
  background: #40E0D0;
  color: #0a0a0f;
}

.title-overlay {
  position: absolute;
  top: 20px;
  left: 20px;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
  border-radius: 20px;
  padding: 12px 20px;
  border-left: 4px solid #40E0D0;
  z-index: 10;
}

.title-overlay h3 {
  font-size: 18px;
  margin: 0;
  color: white;
}

.title-overlay p {
  font-size: 12px;
  margin: 4px 0 0;
  color: rgba(255, 255, 255, 0.6);
}

.controls-tip {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
  border-radius: 40px;
  padding: 8px 20px;
  font-size: 12px;
  display: flex;
  gap: 20px;
  z-index: 10;
  color: rgba(255, 255, 255, 0.6);
}

.muscle-report {
  flex: 1;
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(16px);
  border-radius: 24px;
  padding: 24px;
  border: 1px solid rgba(64, 224, 208, 0.15);
  color: white;
  overflow-y: auto;
  max-height: 650px;
}

.report-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.report-header h2 {
  font-size: 20px;
  margin: 0;
  color: #40E0D0;
}

.current-gender {
  padding: 4px 12px;
  background: #40E0D0;
  color: #0a0a0f;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

.current-muscle {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 16px;
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  border: 1px solid rgba(64, 224, 208, 0.1);
}

.current-muscle.empty {
  background: rgba(64, 224, 208, 0.05);
  border: 1px dashed rgba(64, 224, 208, 0.3);
}

.muscle-icon {
  font-size: 40px;
}

.muscle-info h4 {
  font-size: 18px;
  font-weight: 600;
  color: #40E0D0;
  margin: 0 0 8px 0;
}

.intensity-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
  overflow: hidden;
  margin: 8px 0;
}

.intensity-fill {
  height: 100%;
  background: linear-gradient(90deg, #40E0D0, #2BA0D0);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.training-tips {
  background: rgba(64, 224, 208, 0.08);
  border-radius: 20px;
  padding: 16px;
  margin-bottom: 24px;
  border: 1px solid rgba(64, 224, 208, 0.1);
}

.training-tips h4 {
  font-size: 14px;
  font-weight: 500;
  color: #40E0D0;
  margin: 0 0 12px 0;
}

.training-tips ul {
  margin: 0;
  padding-left: 20px;
}

.training-tips li {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 6px;
}

.muscle-progress h4 {
  font-size: 14px;
  font-weight: 500;
  color: white;
  margin: 0 0 16px 0;
}

.progress-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 380px;
  overflow-y: auto;
  padding-right: 8px;
}

/* ========== 滚动条样式 ========== */
.progress-bars::-webkit-scrollbar {
  width: 6px;
}

.progress-bars::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.progress-bars::-webkit-scrollbar-thumb {
  background: rgba(64, 224, 208, 0.3);
  border-radius: 3px;
}

.progress-bars::-webkit-scrollbar-thumb:hover {
  background: rgba(64, 224, 208, 0.6);
}

/* 全局滚动条样式 */
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

.progress-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 0;
}

.progress-item span:first-child {
  width: 80px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #40E0D0, #2BA0D0);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-value {
  width: 45px;
  font-size: 12px;
  color: #40E0D0;
  text-align: right;
  font-weight: 500;
}

@media (max-width: 900px) {
  .muscle-heatmap-wrapper {
    flex-direction: column;
  }
}
</style>