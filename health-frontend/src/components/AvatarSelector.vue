<template>
  <div class="avatar-selector">
    <div class="current-avatar" @click="openModal">
      <img :src="currentAvatar" class="avatar-img" />
      <div class="edit-overlay">
        <span>✏️</span>
      </div>
    </div>

    <Teleport to="body">
      <div v-if="showModal" class="avatar-modal-overlay" @click.self="closeModal">
        <div class="avatar-modal">
          <div class="avatar-modal-header">
            <h3>选择头像</h3>
            <button class="avatar-modal-close" @click="closeModal">×</button>
          </div>

          <div class="avatar-segmented">
            <button :class="{ active: activeTab === 'default' }" @click="activeTab = 'default'">
              默认头像
            </button>
            <button :class="{ active: activeTab === 'upload' }" @click="activeTab = 'upload'">
              上传照片
            </button>
          </div>

          <div class="avatar-modal-body">
            <div v-if="activeTab === 'default'" class="avatar-grid">
              <div
                  v-for="avatar in defaultAvatars"
                  :key="avatar.id"
                  class="avatar-option"
                  :class="{ selected: selectedAvatarId === avatar.id }"
                  @click="selectDefaultAvatar(avatar)"
              >
                <div class="avatar-emoji" :style="{ backgroundColor: avatar.bgColor }">
                  {{ avatar.emoji }}
                </div>
                <div class="avatar-check" v-if="selectedAvatarId === avatar.id">✓</div>
              </div>
            </div>

            <div v-else class="avatar-upload-section">
              <input
                  type="file"
                  ref="fileInput"
                  accept="image/jpeg,image/png,image/gif,image/webp"
                  @change="handleFileChange"
                  style="display: none"
              />
              <div v-if="!previewImage" class="avatar-upload-area" @click="triggerUpload">
                <div class="avatar-upload-icon">📷</div>
                <p>点击上传照片</p>
                <span>支持 JPG、PNG，不超过2MB</span>
              </div>
              <div v-else class="avatar-preview-section">
                <div class="avatar-preview-circle">
                  <img :src="previewImage" class="avatar-preview-img" />
                  <div class="avatar-reupload" @click.stop="triggerUpload">
                    <span>🔄</span>
                  </div>
                </div>
                <button class="avatar-confirm-btn" @click="confirmUpload">确认使用</button>
              </div>
            </div>
          </div>

          <div class="avatar-modal-footer">
            <button class="avatar-cancel-btn" @click="closeModal">取消</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import axios from 'axios'

const props = defineProps({
  userId: {
    type: Number,
    required: true
  },
  currentAvatarUrl: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['avatar-updated'])

const API_BASE = 'http://localhost:8080/api'

// 默认头像数据
const defaultAvatars = ref([
  { id: 1, name: '笑脸', emoji: '😊', bgColor: '#7CB342' },
  { id: 2, name: '爱心', emoji: '❤️', bgColor: '#FF6B6B' },
  { id: 3, name: '星星', emoji: '⭐', bgColor: '#4ECDC4' },
  { id: 4, name: '太阳', emoji: '☀️', bgColor: '#FFE66D' },
  { id: 5, name: '月亮', emoji: '🌙', bgColor: '#6C5CE7' },
  { id: 6, name: '火焰', emoji: '🔥', bgColor: '#FF8C42' },
  { id: 7, name: '水滴', emoji: '💧', bgColor: '#52B3D9' },
  { id: 8, name: '树叶', emoji: '🍃', bgColor: '#63B14D' },
  { id: 9, name: '皇冠', emoji: '👑', bgColor: '#F5A9B8' },
  { id: 10, name: '奖杯', emoji: '🏆', bgColor: '#FFAA44' },
  { id: 11, name: '点赞', emoji: '👍', bgColor: '#20B2AA' },
  { id: 12, name: '加油', emoji: '💪', bgColor: '#DC143C' }
])

// 将 Emoji 转换为 PNG DataURL
const emojiToPNG = (emoji, bgColor) => {
  const canvas = document.createElement('canvas')
  canvas.width = 200
  canvas.height = 200
  const ctx = canvas.getContext('2d')

  ctx.fillStyle = bgColor
  ctx.fillRect(0, 0, 200, 200)

  ctx.fillStyle = '#ffffff'
  ctx.font = 'bold 100px "Segoe UI Emoji", "Apple Color Emoji", "Noto Color Emoji", sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(emoji, 100, 100)

  return canvas.toDataURL('image/png')
}

const showModal = ref(false)
const activeTab = ref('default')
const selectedAvatarId = ref(null)
const previewImage = ref('')
const selectedFile = ref(null)
const fileInput = ref(null)

// 当前显示的头像
const currentAvatar = computed(() => {
  const url = props.currentAvatarUrl
  if (!url) {
    // 默认显示第一个头像
    return emojiToPNG('😊', '#7CB342')
  }
  if (url.startsWith('data:image')) return url
  if (url.startsWith('http')) return url
  if (url.startsWith('/uploads')) return `http://localhost:8080${url}`
  return url
})

const openModal = () => {
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  activeTab.value = 'default'
  previewImage.value = ''
  selectedFile.value = null
  selectedAvatarId.value = null
  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

// 选择默认头像 - 接收整个 avatar 对象
const selectDefaultAvatar = async (avatar) => {
  selectedAvatarId.value = avatar.id
  try {
    // 生成 PNG base64
    const pngData = emojiToPNG(avatar.emoji, avatar.bgColor)

    const res = await axios.post(`${API_BASE}/user/uploadAvatar`, null, {
      params: {
        userId: props.userId,
        avatarUrl: pngData
      }
    })
    if (res.data.success) {
      emit('avatar-updated', res.data.avatarUrl)
      closeModal()
    } else {
      alert('保存失败: ' + res.data.message)
    }
  } catch (error) {
    console.error('保存头像失败', error)
    alert('保存失败: ' + (error.response?.data?.message || error.message))
  }
}

const triggerUpload = () => {
  fileInput.value.click()
}

const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (!file) return

  if (file.size > 2 * 1024 * 1024) {
    alert('图片不能超过2MB')
    return
  }

  if (!file.type.startsWith('image/')) {
    alert('请选择图片文件')
    return
  }

  selectedFile.value = file
  const reader = new FileReader()
  reader.onload = (ev) => {
    previewImage.value = ev.target.result
  }
  reader.readAsDataURL(file)
}

const confirmUpload = async () => {
  if (!selectedFile.value) {
    alert('请先选择图片')
    return
  }

  const formData = new FormData()
  formData.append('file', selectedFile.value)
  formData.append('userId', props.userId)

  try {
    const res = await axios.post(`${API_BASE}/user/uploadAvatar`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (res.data.success) {
      emit('avatar-updated', res.data.avatarUrl)
      closeModal()
    } else {
      alert('上传失败: ' + res.data.message)
    }
  } catch (error) {
    console.error('上传失败', error)
    alert('上传失败')
  }
}
</script>

<style scoped>
.avatar-selector {
  display: inline-block;
}

.current-avatar {
  position: relative;
  cursor: pointer;
}

.avatar-img {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #7CB342;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.edit-overlay {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 30px;
  height: 30px;
  background: #7CB342;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
}
</style>

<style>
.avatar-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100000;
}

.avatar-modal {
  width: 480px;
  max-width: 90vw;
  background: white;
  border-radius: 28px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  animation: modalFadeIn 0.2s ease-out;
}

@keyframes modalFadeIn {
  from { opacity: 0; transform: scale(0.95); }
  to { opacity: 1; transform: scale(1); }
}

.avatar-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px 12px 24px;
}

.avatar-modal-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.avatar-modal-close {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.avatar-modal-close:hover {
  background: #f5f5f5;
  color: #333;
}

.avatar-segmented {
  display: flex;
  margin: 8px 24px 20px 24px;
  background: #f5f5f5;
  border-radius: 40px;
  padding: 4px;
  gap: 4px;
}

.avatar-segmented button {
  flex: 1;
  padding: 10px 0;
  background: transparent;
  border: none;
  border-radius: 36px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  color: #999;
  transition: all 0.2s;
}

.avatar-segmented button.active {
  background: #7CB342;
  color: white;
  box-shadow: 0 2px 8px rgba(124, 179, 66, 0.3);
}

.avatar-segmented button:not(.active):hover {
  color: #7CB342;
}

.avatar-modal-body {
  padding: 0 24px;
  max-height: 400px;
  overflow-y: auto;
}

.avatar-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  padding: 8px 0 20px 0;
}

.avatar-option {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.2s;
  width: 100%;
  aspect-ratio: 1;
}

.avatar-option.selected {
  transform: scale(1.08);
}

.avatar-emoji {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  transition: all 0.2s;
}

.avatar-option.selected .avatar-emoji {
  border: 3px solid #7CB342;
  box-shadow: 0 0 0 2px rgba(124, 179, 66, 0.3);
}

.avatar-check {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 22px;
  height: 22px;
  background: #7CB342;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  font-weight: bold;
  border: 2px solid white;
}

.avatar-upload-section {
  padding: 8px 0 24px 0;
}

.avatar-upload-area {
  border: 2px dashed #ddd;
  border-radius: 20px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}

.avatar-upload-area:hover {
  border-color: #7CB342;
  background: #f5f8f2;
}

.avatar-upload-icon {
  font-size: 52px;
  margin-bottom: 12px;
}

.avatar-upload-area p {
  margin: 0 0 8px 0;
  font-size: 15px;
  font-weight: 500;
  color: #555;
}

.avatar-upload-area span {
  font-size: 12px;
  color: #aaa;
}

.avatar-preview-section {
  text-align: center;
}

.avatar-preview-circle {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto 12px;
  cursor: pointer;
}

.avatar-preview-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #7CB342;
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.avatar-reupload {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 36px;
  height: 36px;
  background: #7CB342;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 2px solid white;
  transition: all 0.2s;
}

.avatar-reupload span {
  font-size: 18px;
}

.avatar-preview-tip {
  font-size: 12px;
  color: #aaa;
  margin: 0 0 24px 0;
}

.avatar-confirm-btn {
  width: 100%;
  padding: 14px 0;
  background: #7CB342;
  color: white;
  border: none;
  border-radius: 40px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.avatar-confirm-btn:hover {
  background: #689F38;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(124, 179, 66, 0.3);
}

.avatar-modal-footer {
  padding: 16px 24px 24px 24px;
  display: flex;
  justify-content: center;
}

.avatar-cancel-btn {
  width: 100%;
  padding: 12px 0;
  background: #f5f5f5;
  border: none;
  border-radius: 40px;
  font-size: 15px;
  cursor: pointer;
  transition: all 0.2s;
  color: #666;
}

.avatar-cancel-btn:hover {
  background: #e8e8e8;
}
</style>