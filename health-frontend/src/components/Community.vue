<template>
  <div class="community-container">
    <!-- 公告栏 -->
    <div v-if="announcements.length > 0 && !isAnnouncementClosed" class="announcement-bar" :class="getAnnouncementTypeClass()">
      <div class="announcement-content">
        <div class="announcement-left">
          <span class="announcement-icon">📢</span>
          <div class="announcement-text">
            <span v-for="(ann, index) in announcements" :key="ann.id" class="announcement-item">
              <strong>{{ ann.title }}</strong>: {{ ann.content }}
              <span v-if="index < announcements.length - 1" class="announcement-separator">|</span>
            </span>
          </div>
        </div>
        <button class="announcement-close" @click="closeAnnouncementBar" title="今日不再显示">
          <span class="close-icon">🗙</span>
          <span class="close-text">收起</span>
        </button>
      </div>
    </div>

    <!-- 收起后的提示条 -->
    <div v-if="isAnnouncementClosed && announcements.length > 0" class="announcement-collapsed-tip" @click="showAnnouncementAgain">
      <span class="tip-icon">📢</span>
      <span class="tip-text">有 {{ announcements.length }} 条新公告，点击查看</span>
    </div>

    <!-- 通知图标 -->
    <div class="notification-icon" @click="toggleNotificationPanel">
      <span class="bell-icon">🔔</span>
      <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
    </div>

    <!-- 通知面板 -->
    <div v-if="showNotificationPanel" class="notification-panel glass-card">
      <div class="notification-header">
        <h4>通知</h4>
        <button v-if="unreadCount > 0" class="mark-all-read" @click="markAllNotificationsAsRead">全部已读</button>
      </div>
      <div v-if="notifications.length === 0" class="empty-notification">暂无通知</div>
      <div v-else class="notification-list">
        <div v-for="notif in notifications" :key="notif.id"
             :class="['notification-item', { unread: notif.isRead === 0 }]"
             @click="handleNotificationClick(notif)">
          <div class="notification-avatar">
            <img v-if="notif.fromUserAvatar" :src="getFullImageUrl(notif.fromUserAvatar)" />
            <span v-else>👤</span>
          </div>
          <div class="notification-content">
            <div class="notification-text">
              <strong>{{ notif.fromUsername }}</strong> {{ notif.content }}
            </div>
            <div class="notification-time">{{ formatTime(notif.createTime) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 发布卡片 -->
    <div class="glass-card publish-card" @click="openPublishModal">
      <div class="publish-header">
        <div class="avatar-small">
          <img
              v-if="currentUserAvatar"
              :src="getFullImageUrl(currentUserAvatar)"
              class="avatar-img"
              @error="handleAvatarError"
          />
          <span v-else>👤</span>
        </div>
        <div class="publish-placeholder">分享你的健康生活...</div>
      </div>
    </div>

    <!-- 筛选标签 -->
    <div class="tabs-filter">
      <button :class="{active: activeTab === 'latest'}" @click="switchTab('latest')">📋 最新动态</button>
      <button :class="{active: activeTab === 'hot'}" @click="switchTab('hot')">🔥 热门推荐</button>
    </div>

    <div v-if="loading && posts.length === 0" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="posts.length === 0" class="empty-state glass">
      <div class="empty-icon">📭</div>
      <p>暂无动态，快来发布第一条吧！</p>
    </div>

    <div v-else class="posts-list">
      <div v-for="post in posts" :key="post.id" class="glass-card post-card" :data-post-id="post.id">
        <div class="post-header">
          <div class="user-area">
            <div class="avatar-small clickable" @click="goToUserProfile(post.userId)">
              <img
                  v-if="post.userAvatar"
                  :src="getFullImageUrl(post.userAvatar)"
                  class="avatar-img"
                  @error="handleAvatarError"
              />
              <span v-else>👤</span>
            </div>
            <div class="user-detail">
              <div class="username clickable" @click="goToUserProfile(post.userId)">{{ post.nickname || post.username }}</div>
              <div class="post-time">{{ formatTime(post.createTime) }}</div>
            </div>
            <button
                v-if="post.userId !== userId"
                @click.stop="toggleFollow(post.userId)"
                class="follow-btn"
                :class="{following: followingStatus[post.userId]}"
            >
              {{ followingStatus[post.userId] ? '✓ 已关注' : '+ 关注' }}
            </button>
          </div>
          <div class="post-badges">
            <span v-if="post.isPinned === 1" class="post-badge pinned-badge">📌 置顶</span>
            <span v-if="post.isFeatured === 1" class="post-badge featured-badge">⭐ 精华</span>
          </div>
          <button
              v-if="post.userId === userId"
              @click="deletePost(post.id)"
              class="delete-post-btn"
              title="删除"
          >
            🗑️
          </button>
        </div>

        <div class="post-content">{{ post.content }}</div>

        <div v-if="post.images && post.images.length > 0" class="post-images">
          <div
              v-for="(img, idx) in (typeof post.images === 'string' ? post.images.split(',') : post.images)"
              :key="idx"
              class="post-image-item"
              @click="viewImage(img)"
          >
            <img :src="getFullImageUrl(img)" />
          </div>
        </div>

        <div class="post-actions">
          <button @click="toggleLike(post.id)" :class="{active: post.liked}" class="action-btn">
            <span>{{ post.liked ? '❤️' : '🤍' }}</span>
            <span>{{ post.likeCount || 0 }}</span>
          </button>
          <button @click="toggleComment(post.id)" class="action-btn">
            <span>💬</span>
            <span>{{ post.commentCount || 0 }}</span>
          </button>
          <!-- 举报帖子按钮（不能举报自己的帖子）-->
          <button v-if="post.userId !== userId" @click="openReportModal('POST', post.id, post.userId)" class="action-btn report-btn" title="举报">
            <span>🚩</span>
            <span>举报</span>
          </button>
        </div>

        <!-- 评论区 -->
        <div v-if="activeCommentId === post.id" class="comment-section">
          <div class="comment-list">
            <!-- 顶级评论列表 -->
            <div v-for="(comment, idx) in commentsData[post.id]" :key="comment.id" class="comment-item" :data-comment-id="comment.id">
              <div class="comment-avatar clickable" @click="goToUserProfile(comment.userId)">
                <img
                    v-if="comment.userAvatar"
                    :src="getFullImageUrl(comment.userAvatar)"
                    class="comment-avatar-img"
                    @error="handleAvatarError"
                />
                <span v-else>👤</span>
              </div>
              <div class="comment-content">
                <div class="comment-user-info">
                  <span class="comment-user clickable" @click="goToUserProfile(comment.userId)">{{ comment.nickname || comment.username }}</span>
                  <span v-if="comment.userId !== userId && followingStatus[comment.userId]" class="follow-badge">已关注</span>
                  <!-- 置顶标识 -->
                  <span v-if="comment.isPinned" class="pinned-badge">📌 置顶</span>
                  <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                  <!-- 置顶按钮（仅帖子作者可见）- 样式已修改为和回复一致 -->
                  <button v-if="post.userId === userId" class="pin-btn" @click="togglePinComment(post.id, comment.id, comment.isPinned)">
                    {{ comment.isPinned ? '取消置顶' : '置顶' }}
                  </button>
                  <!-- 回复按钮 -->
                  <button class="reply-btn" @click="openReplyInput(post.id, comment.id, comment.nickname || comment.username, comment.userId)">
                    💬 回复
                  </button>
                  <!-- 举报评论按钮 -->
                  <button v-if="comment.userId !== userId" class="report-comment-btn" @click="openReportModal('COMMENT', comment.id, comment.userId)" title="举报">
                    🚩 举报
                  </button>
                </div>
                <div class="comment-text-wrapper">
                  <span class="comment-text">{{ comment.content }}</span>
                </div>
                <!-- 评论点赞按钮 -->
                <div class="comment-actions">
                  <button @click="toggleCommentLike(comment.id, post.id)" class="comment-like-btn">
                    <span>{{ comment.liked ? '❤️' : '🤍' }}</span>
                    <span>{{ comment.likeCount || 0 }}</span>
                  </button>
                </div>
                <!-- 回复列表 -->
                <div v-if="comment.replies && comment.replies.length > 0" class="replies-list">
                  <div v-for="(reply, ridx) in comment.replies" :key="reply.id" class="reply-item">
                    <div class="reply-avatar clickable" @click="goToUserProfile(reply.userId)">
                      <img
                          v-if="reply.userAvatar"
                          :src="getFullImageUrl(reply.userAvatar)"
                          class="reply-avatar-img"
                          @error="handleAvatarError"
                      />
                      <span v-else>👤</span>
                    </div>
                    <div class="reply-content">
                      <div class="reply-user-info">
                        <span class="reply-user clickable" @click="goToUserProfile(reply.userId)">{{ reply.nickname || reply.username }}</span>
                        <span v-if="reply.userId !== userId && followingStatus[reply.userId]" class="follow-badge-small">已关注</span>
                        <span class="reply-time">{{ formatTime(reply.createTime) }}</span>
                        <button class="reply-to-reply-btn" @click="openReplyInput(post.id, comment.id, reply.nickname || reply.username, reply.userId)">
                          💬 回复
                        </button>
                        <!-- 举报回复按钮 -->
                        <button v-if="reply.userId !== userId" class="report-reply-btn" @click="openReportModal('COMMENT', reply.id, reply.userId)" title="举报">
                          🚩 举报
                        </button>
                      </div>
                      <div class="reply-text">
                        <span v-if="reply.replyToUsername" class="reply-to-text">
                          回复 @{{ reply.replyToUsername }}:
                        </span>
                        <span>{{ reply.content }}</span>
                      </div>
                      <!-- 回复点赞按钮 -->
                      <div class="reply-actions">
                        <button @click="toggleCommentLike(reply.id, post.id)" class="reply-like-btn">
                          <span>{{ reply.liked ? '❤️' : '🤍' }}</span>
                          <span>{{ reply.likeCount || 0 }}</span>
                        </button>
                      </div>
                    </div>
                    <button
                        v-if="reply.userId === userId || post.userId === userId"
                        @click="deleteReply(post.id, reply.id)"
                        class="delete-reply-btn"
                        title="删除"
                    >
                      ✕
                    </button>
                  </div>
                </div>
              </div>
              <button
                  v-if="comment.userId === userId || post.userId === userId"
                  @click="deleteComment(post.id, comment.id)"
                  class="delete-comment-btn"
                  title="删除"
              >
                ✕
              </button>
            </div>
          </div>

          <!-- 评论输入区 -->
          <div class="comment-input-area">
            <input
                type="text"
                v-model="commentInputs[post.id]"
                :placeholder="replyTarget[post.id] ? `回复 ${replyTarget[post.id].userName}...` : '写下你的评论...'"
                class="glass-input"
                @keyup.enter="submitCommentOrReply(post.id)"
            />
            <button v-if="replyTarget[post.id]" class="cancel-reply-btn" @click="cancelReply(post.id)">
              取消
            </button>
            <button @click="submitCommentOrReply(post.id)" class="comment-send-btn">发送</button>
          </div>
        </div>
      </div>

      <!-- 加载更多提示 -->
      <div v-if="loading && posts.length > 0" class="loading-more">
        <div class="loading-spinner-small"></div>
        <span>加载中...</span>
      </div>
      <div v-if="!hasMore && posts.length > 0" class="no-more">
        <span>— 已经到底了 —</span>
      </div>
    </div>

    <!-- 发布弹窗（蒙层已去掉） -->
    <Teleport to="body">
      <div v-if="showModal" class="modal-mask" @click.self="closeModal">
        <div class="modal-container">
          <div class="modal-header">
            <h3>📝 发布新动态</h3>
            <button class="close-btn" @click="closeModal">×</button>
          </div>
          <div class="modal-body">
            <textarea
                v-model="newPost.content"
                placeholder="分享你的健康生活..."
                rows="4"
                class="post-textarea"
            ></textarea>
            <div class="image-upload-section">
              <div class="image-list" v-if="newPost.images.length > 0">
                <div v-for="(img, idx) in newPost.images" :key="idx" class="image-item">
                  <img :src="getFullImageUrl(img)" class="preview-img" />
                  <button class="remove-img" @click="removeImage(idx)">✕</button>
                </div>
              </div>
              <div v-if="uploadingImages" class="uploading-progress">
                <span>⏳ 上传中...</span>
              </div>
              <button v-if="newPost.images.length < 9 && !uploadingImages" class="upload-image-btn" @click="triggerFileUpload">
                📷 添加图片 ({{ newPost.images.length }}/9)
              </button>
              <input
                  type="file"
                  ref="fileInput"
                  accept="image/jpeg,image/png,image/jpg"
                  multiple
                  style="display: none"
                  @change="handleImageSelect"
              />
            </div>
            <div class="tag-section">
              <span class="tag-label">🏷️ 选择标签：</span>
              <div class="tag-group">
                <button
                    v-for="tag in tags"
                    :key="tag"
                    :class="{active: newPost.tag === tag}"
                    @click="newPost.tag = tag"
                    class="tag-btn"
                >{{ tag }}</button>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="closeModal">取消</button>
            <button class="publish-btn" @click="publishPost" :disabled="(!newPost.content.trim() && newPost.images.length === 0) || uploadingImages">
              发布
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 举报弹窗 - 单独使用透明蒙层 -->
    <Teleport to="body">
      <div v-if="showReportModal" class="report-modal-mask" @click.self="closeReportModal">
        <div class="modal-container report-modal">
          <div class="modal-header">
            <h3>🚩 举报</h3>
            <button class="close-btn" @click="closeReportModal">×</button>
          </div>
          <div class="modal-body">
            <p>请选择举报原因：</p>
            <div class="report-reasons">
              <label v-for="reason in reportReasons" :key="reason" class="report-reason">
                <input type="radio" v-model="selectedReportReason" :value="reason" />
                <span>{{ reason }}</span>
              </label>
            </div>
          </div>
          <div class="modal-footer">
            <button class="cancel-btn" @click="closeReportModal">取消</button>
            <button class="publish-btn" @click="submitReport" :disabled="!selectedReportReason">提交举报</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 图片查看器 -->
    <Teleport to="body">
      <div v-if="showImageViewer" class="image-viewer" @click="closeImageViewer">
        <img :src="getFullImageUrl(viewingImage)" class="viewer-img" />
        <button class="close-viewer">✕</button>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import axios from 'axios'

const props = defineProps({
  userId: { type: Number, required: true }
})

const emit = defineEmits(['view-profile'])

const API_BASE = 'http://localhost:8080/api'

// 分页相关
const loading = ref(false)
const page = ref(0)
const hasMore = ref(true)
const pageSize = 20

const posts = ref([])
const activeTab = ref('latest')
const activeCommentId = ref(null)
const commentInputs = ref({})
const commentsData = ref({})
const showModal = ref(false)
const newPost = ref({ content: '', tag: '', images: [] })
const tags = ['#运动打卡', '#健康饮食', '#心情树洞']

const fileInput = ref(null)
const showImageViewer = ref(false)
const viewingImage = ref('')
const uploadingImages = ref(false)
const currentUserAvatar = ref('')
const followingStatus = ref({})

const replyTarget = ref({})

// 通知相关
const showNotificationPanel = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
let notificationTimer = null

// 公告相关
const announcements = ref([])
const isAnnouncementClosed = ref(false)
const ANNOUNCEMENT_CLOSED_KEY = 'announcement_closed_date'

// 举报相关
const showReportModal = ref(false)
const reportTarget = ref({ targetType: '', targetId: null, targetUserId: null })
const selectedReportReason = ref('')
const reportReasons = ['色情内容', '广告骚扰', '辱骂攻击', '虚假信息', '政治敏感', '其他']

const getFullImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('data:image')) return url
  if (url.startsWith('/uploads')) return `http://localhost:8080${url}`
  return url
}

const loadCurrentUserAvatar = async () => {
  try {
    const res = await axios.get(`${API_BASE}/user/${props.userId}`)
    if (res.data && res.data.avatar) {
      currentUserAvatar.value = res.data.avatar
    }
  } catch (e) {
    console.error('加载用户头像失败', e)
  }
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

const switchTab = (tab) => {
  activeTab.value = tab
  loadPosts(true)
}

// 加载帖子（支持分页和无限滚动）
const loadPosts = async (reset = true) => {
  if (reset) {
    page.value = 0
    posts.value = []
    hasMore.value = true
  }

  if (!hasMore.value || loading.value) return

  loading.value = true
  try {
    const url = activeTab.value === 'latest'
        ? `${API_BASE}/community/posts`
        : `${API_BASE}/community/hot-posts`

    const res = await axios.get(url, {
      params: {
        currentUserId: props.userId,
        page: page.value,
        size: pageSize
      }
    })

    let postsData = []
    if (Array.isArray(res.data)) {
      postsData = res.data
    } else if (res.data.data && Array.isArray(res.data.data)) {
      postsData = res.data.data
    }

    if (postsData.length < pageSize) {
      hasMore.value = false
    }

    const newPosts = postsData.map(post => ({
      ...post,
      liked: post.liked || false,
      images: post.images ? (typeof post.images === 'string' ? post.images.split(',') : post.images) : []
    }))

    if (reset) {
      posts.value = newPosts
    } else {
      posts.value.push(...newPosts)
    }

    page.value++

    const uniqueUserIds = [...new Set(posts.value.map(p => p.userId).filter(id => id !== props.userId))]
    for (const userId of uniqueUserIds) {
      await checkFollowStatus(userId)
    }
  } catch (error) {
    console.error('加载失败', error)
    if (reset) posts.value = []
  } finally {
    loading.value = false
  }
}

const checkFollowStatus = async (targetUserId) => {
  if (followingStatus.value[targetUserId] !== undefined) return
  try {
    const res = await axios.get(`${API_BASE}/follow/status`, {
      params: { followerId: props.userId, followedId: targetUserId }
    })
    followingStatus.value[targetUserId] = res.data.isFollowing
  } catch (e) {
    followingStatus.value[targetUserId] = false
  }
}

const toggleFollow = async (targetUserId) => {
  try {
    if (followingStatus.value[targetUserId]) {
      await axios.delete(`${API_BASE}/follow/unfollow`, {
        params: { followerId: props.userId, followedId: targetUserId }
      })
      followingStatus.value[targetUserId] = false
    } else {
      await axios.post(`${API_BASE}/follow/follow`, null, {
        params: { followerId: props.userId, followedId: targetUserId }
      })
      followingStatus.value[targetUserId] = true
    }
  } catch (e) {
    alert('操作失败，请重试')
  }
}

const goToUserProfile = (targetUserId) => {
  if (targetUserId === props.userId) {
    emit('view-profile', 'self')
  } else {
    emit('view-profile', targetUserId)
  }
}

const uploadImageToServer = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  const res = await axios.post(`${API_BASE}/community/upload-image`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 30000
  })
  if (res.data.success) {
    return res.data.url
  } else {
    throw new Error(res.data.message || '上传失败')
  }
}

const handleImageSelect = async (e) => {
  const files = Array.from(e.target.files)
  const maxImages = 9 - newPost.value.images.length
  if (files.length === 0) return
  uploadingImages.value = true
  try {
    for (let i = 0; i < Math.min(files.length, maxImages); i++) {
      const file = files[i]
      if (file.size > 2 * 1024 * 1024) {
        alert(`图片 "${file.name}" 太大，请选择小于 2MB 的图片`)
        continue
      }
      if (!file.type.startsWith('image/')) {
        alert(`文件 "${file.name}" 不是图片格式`)
        continue
      }
      try {
        const imageUrl = await uploadImageToServer(file)
        newPost.value.images.push(imageUrl)
      } catch (error) {
        alert(`图片 "${file.name}" 上传失败: ${error.message}`)
      }
    }
  } finally {
    uploadingImages.value = false
  }
  e.target.value = ''
}

const triggerFileUpload = () => {
  fileInput.value.click()
}

const removeImage = (index) => {
  newPost.value.images.splice(index, 1)
}

// ========== 禁言检查 ==========
const checkBanStatus = async () => {
  try {
    const res = await axios.get(`${API_BASE}/admin/user/${props.userId}/ban-status`)
    if (res.data.isBanned) {
      const banEndTime = new Date(res.data.banEndTime)
      const endDateStr = `${banEndTime.getFullYear()}/${banEndTime.getMonth() + 1}/${banEndTime.getDate()}`
      alert(`您已被禁言，禁言至 ${endDateStr}，期间无法发布动态和评论`)
      return false
    }
    return true
  } catch (e) {
    console.error('检查禁言状态失败', e)
    return true
  }
}

const publishPost = async () => {
  const canPost = await checkBanStatus()
  if (!canPost) return

  if (!newPost.value.content.trim() && newPost.value.images.length === 0) {
    alert('请输入内容或添加图片')
    return
  }
  if (uploadingImages.value) {
    alert('请等待图片上传完成')
    return
  }
  try {
    const response = await axios.post(`${API_BASE}/community/post`, null, {
      params: {
        userId: props.userId,
        content: newPost.value.content || '',
        tag: newPost.value.tag || '',
        syncHealthData: 0,
        images: newPost.value.images.join(',')
      },
      timeout: 30000
    })
    if (response.data.success) {
      alert('发布成功！')
      closeModal()
      loadPosts(true)
    } else {
      alert('发布失败: ' + (response.data.message || '未知错误'))
    }
  } catch (error) {
    alert('发布失败: ' + (error.response?.data?.message || error.message || '网络错误'))
  }
}

const toggleLike = async (postId) => {
  const post = posts.value.find(p => p.id === postId)
  if (!post) return
  try {
    if (post.liked) {
      await axios.delete(`${API_BASE}/community/like`, { params: { postId, userId: props.userId } })
      post.liked = false
      post.likeCount = Math.max(0, (post.likeCount || 1) - 1)
    } else {
      await axios.post(`${API_BASE}/community/like`, null, { params: { postId, userId: props.userId } })
      post.liked = true
      post.likeCount = (post.likeCount || 0) + 1
    }
  } catch (error) {
    alert('操作失败，请重试')
  }
}

const deletePost = async (postId) => {
  if (!confirm('确定要删除这条动态吗？')) return
  const index = posts.value.find(p => p.id === postId)
  if (index === -1) return
  try {
    await axios.delete(`${API_BASE}/community/post/${postId}`, { params: { userId: props.userId } })
    posts.value.splice(index, 1)
    alert('删除成功')
  } catch (error) {
    alert('删除失败')
  }
}

const toggleComment = async (postId) => {
  if (activeCommentId.value === postId) {
    activeCommentId.value = null
  } else {
    activeCommentId.value = postId
    await loadCommentsWithReplies(postId)
  }
}

// 加载带回复的评论列表
const loadCommentsWithReplies = async (postId) => {
  try {
    const res = await axios.get(`${API_BASE}/community/comments-with-replies/${postId}`)
    if (Array.isArray(res.data)) {
      const comments = res.data
      for (const comment of comments) {
        try {
          const likeStatusRes = await axios.get(`${API_BASE}/community/comment-like-status`, {
            params: { commentId: comment.id, userId: props.userId }
          })
          comment.liked = likeStatusRes.data.liked || false
        } catch {
          comment.liked = false
        }
        comment.likeCount = comment.likeCount || 0
        if (comment.replies) {
          for (const reply of comment.replies) {
            try {
              const replyLikeRes = await axios.get(`${API_BASE}/community/comment-like-status`, {
                params: { commentId: reply.id, userId: props.userId }
              })
              reply.liked = replyLikeRes.data.liked || false
            } catch {
              reply.liked = false
            }
            reply.likeCount = reply.likeCount || 0
          }
        }
      }
      commentsData.value[postId] = comments
    } else {
      commentsData.value[postId] = []
    }

    const commentUserIds = new Set()
    for (const comment of commentsData.value[postId]) {
      commentUserIds.add(comment.userId)
      if (comment.replies) {
        for (const reply of comment.replies) {
          commentUserIds.add(reply.userId)
          if (reply.replyToUserId) commentUserIds.add(reply.replyToUserId)
        }
      }
    }
    for (const uid of commentUserIds) {
      if (uid !== props.userId) {
        await checkFollowStatus(uid)
      }
    }
  } catch (error) {
    console.error('加载评论失败', error)
    commentsData.value[postId] = []
  }
}

// 评论点赞/取消点赞
const toggleCommentLike = async (commentId, postId) => {
  try {
    let targetComment = null
    const comments = commentsData.value[postId]
    for (const comment of comments) {
      if (comment.id === commentId) {
        targetComment = comment
        break
      }
      if (comment.replies) {
        const reply = comment.replies.find(r => r.id === commentId)
        if (reply) {
          targetComment = reply
          break
        }
      }
    }
    if (!targetComment) return

    if (targetComment.liked) {
      await axios.delete(`${API_BASE}/community/comment-like`, {
        params: { commentId, userId: props.userId }
      })
      targetComment.liked = false
      targetComment.likeCount = Math.max(0, (targetComment.likeCount || 1) - 1)
    } else {
      await axios.post(`${API_BASE}/community/comment-like`, null, {
        params: { commentId, userId: props.userId }
      })
      targetComment.liked = true
      targetComment.likeCount = (targetComment.likeCount || 0) + 1
    }
  } catch (error) {
    console.error('评论点赞操作失败', error)
    alert('操作失败，请重试')
  }
}

// 置顶/取消置顶评论
const togglePinComment = async (postId, commentId, isPinned) => {
  try {
    const res = await axios.post(`${API_BASE}/community/pin-comment`, null, {
      params: { commentId, userId: props.userId }
    })
    if (res.data.success !== false) {
      alert(res.data.message || (res.data.pinned ? '置顶成功' : '已取消置顶'))
      await loadCommentsWithReplies(postId)
    } else {
      alert(res.data.message || '操作失败')
    }
  } catch (error) {
    alert('操作失败: ' + (error.response?.data?.message || error.message))
  }
}

// 提交评论或回复
const submitCommentOrReply = async (postId) => {
  const canPost = await checkBanStatus()
  if (!canPost) return

  const content = commentInputs.value[postId]
  if (!content || !content.trim()) return

  const target = replyTarget.value[postId]

  try {
    if (target) {
      await axios.post(`${API_BASE}/community/reply`, null, {
        params: {
          postId: postId,
          userId: props.userId,
          content: content.trim(),
          parentId: target.commentId,
          replyToUserId: target.replyToUserId
        }
      })
      alert('回复成功！')
    } else {
      await axios.post(`${API_BASE}/community/comment`, null, {
        params: { postId, userId: props.userId, content: content.trim() }
      })
      alert('评论成功！')
    }

    commentInputs.value[postId] = ''
    cancelReply(postId)
    await loadCommentsWithReplies(postId)
    const post = posts.value.find(p => p.id === postId)
    if (post) {
      post.commentCount = (post.commentCount || 0) + 1
    }
  } catch (error) {
    alert('操作失败: ' + (error.response?.data?.message || error.message))
  }
}

const openReplyInput = (postId, commentId, userName, replyToUserId) => {
  replyTarget.value[postId] = {
    commentId: commentId,
    userName: userName,
    replyToUserId: replyToUserId
  }
  commentInputs.value[postId] = ''
  setTimeout(() => {
    const input = document.querySelector(`.comment-input-area input`)
    if (input) input.focus()
  }, 100)
}

const cancelReply = (postId) => {
  delete replyTarget.value[postId]
}

const deleteComment = async (postId, commentId) => {
  if (!confirm('确定要删除这条评论吗？')) return
  try {
    await axios.delete(`${API_BASE}/community/comment/${commentId}`, { params: { userId: props.userId } })
    alert('删除成功')
    await loadCommentsWithReplies(postId)
    const post = posts.value.find(p => p.id === postId)
    if (post) {
      const countRes = await axios.get(`${API_BASE}/community/comments-with-replies/${postId}`)
      if (Array.isArray(countRes.data)) {
        let totalCount = 0
        for (const comment of countRes.data) {
          totalCount++
          if (comment.replies) totalCount += comment.replies.length
        }
        post.commentCount = totalCount
      }
    }
  } catch (error) {
    alert('删除失败')
  }
}

const deleteReply = async (postId, replyId) => {
  if (!confirm('确定要删除这条回复吗？')) return
  try {
    await axios.delete(`${API_BASE}/community/reply/${replyId}`, { params: { userId: props.userId } })
    alert('删除成功')
    await loadCommentsWithReplies(postId)
    const post = posts.value.find(p => p.id === postId)
    if (post) {
      const countRes = await axios.get(`${API_BASE}/community/comments-with-replies/${postId}`)
      if (Array.isArray(countRes.data)) {
        let totalCount = 0
        for (const comment of countRes.data) {
          totalCount++
          if (comment.replies) totalCount += comment.replies.length
        }
        post.commentCount = totalCount
      }
    }
  } catch (error) {
    alert('删除失败')
  }
}

// ========== 举报功能 ==========
const openReportModal = (targetType, targetId, targetUserId) => {
  reportTarget.value = { targetType, targetId, targetUserId }
  selectedReportReason.value = ''
  showReportModal.value = true
}

const closeReportModal = () => {
  showReportModal.value = false
  reportTarget.value = { targetType: '', targetId: null, targetUserId: null }
  selectedReportReason.value = ''
}

const submitReport = async () => {
  if (!selectedReportReason.value) {
    alert('请选择举报原因')
    return
  }
  try {
    await axios.post(`${API_BASE}/report`, null, {
      params: {
        reporterId: props.userId,
        targetType: reportTarget.value.targetType,
        targetId: reportTarget.value.targetId,
        targetUserId: reportTarget.value.targetUserId,
        reason: selectedReportReason.value
      }
    })
    alert('举报已提交，我们会尽快处理')
    closeReportModal()
  } catch (error) {
    alert('举报失败：' + (error.response?.data?.message || error.message))
  }
}

// ========== 通知功能 ==========
const loadNotifications = async () => {
  try {
    const res = await axios.get(`${API_BASE}/community/notifications`, {
      params: { userId: props.userId, page: 0, size: 20 }
    })
    notifications.value = res.data.notifications || []
    unreadCount.value = res.data.unreadCount || 0
  } catch (error) {
    console.error('加载通知失败', error)
  }
}

const toggleNotificationPanel = () => {
  showNotificationPanel.value = !showNotificationPanel.value
  if (showNotificationPanel.value) {
    loadNotifications()
  }
}

const markAllNotificationsAsRead = async () => {
  try {
    await axios.put(`${API_BASE}/community/notifications/read-all`, null, {
      params: { userId: props.userId }
    })
    unreadCount.value = 0
    for (const notif of notifications.value) {
      notif.isRead = 1
    }
    alert('已全部标记为已读')
  } catch (error) {
    console.error('标记失败', error)
  }
}

// ========== 跳转到帖子详情 ==========
const viewPostDetail = async (postId, commentId) => {
  showNotificationPanel.value = false
  let targetPost = posts.value.find(p => p.id === postId)

  if (targetPost) {
    const postElement = document.querySelector(`.post-card[data-post-id="${postId}"]`)
    if (postElement) {
      postElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
      postElement.classList.add('highlight-post')
      setTimeout(() => {
        postElement.classList.remove('highlight-post')
      }, 2000)
    }

    if (commentId) {
      if (activeCommentId.value !== postId) {
        activeCommentId.value = postId
        await loadCommentsWithReplies(postId)
      }

      setTimeout(() => {
        const commentElement = document.querySelector(`.comment-item[data-comment-id="${commentId}"]`)
        if (commentElement) {
          commentElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
          commentElement.classList.add('highlight-comment')
          setTimeout(() => {
            commentElement.classList.remove('highlight-comment')
          }, 2000)
        } else {
          const replyElement = document.querySelector(`.reply-item[data-comment-id="${commentId}"]`)
          if (replyElement) {
            replyElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
            replyElement.classList.add('highlight-comment')
            setTimeout(() => {
              replyElement.classList.remove('highlight-comment')
            }, 2000)
          }
        }
      }, 500)
    }
  } else {
    alert('正在跳转到帖子...')
    await loadPosts(true)
    setTimeout(() => {
      const newPost = posts.value.find(p => p.id === postId)
      if (newPost) {
        viewPostDetail(postId, commentId)
      } else {
        alert('帖子不存在或已被删除')
      }
    }, 1000)
  }
}

const handleNotificationClick = async (notif) => {
  if (notif.isRead === 0) {
    try {
      await axios.put(`${API_BASE}/community/notification/${notif.id}/read`, null, {
        params: { userId: props.userId }
      })
      notif.isRead = 1
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    } catch (error) {
      console.error('标记失败', error)
    }
  }

  if (notif.postId) {
    await viewPostDetail(notif.postId, notif.commentId)
  } else {
    showNotificationPanel.value = false
    alert('该通知对应的帖子可能已被删除，请刷新页面重试')
  }
}

const startNotificationPolling = () => {
  if (notificationTimer) clearInterval(notificationTimer)
  notificationTimer = setInterval(async () => {
    try {
      const res = await axios.get(`${API_BASE}/community/notifications/unread-count`, {
        params: { userId: props.userId }
      })
      unreadCount.value = res.data.count || 0
    } catch (error) {
      console.error('获取未读数量失败', error)
    }
  }, 30000)
}

// ========== 公告功能 ==========
const loadAnnouncements = async () => {
  try {
    const res = await axios.get(`${API_BASE}/admin/announcements`, {
      params: { page: 0, size: 10 }
    })
    if (res.data.success && res.data.data) {
      announcements.value = res.data.data.filter(a => a.isActive === 1)
    }
  } catch (error) {
    console.error('加载公告失败', error)
  }
}

const getAnnouncementTypeClass = () => {
  if (announcements.value.length === 0) return ''
  const firstAnn = announcements.value[0]
  if (firstAnn.type === 'WARNING') return 'announcement-warning'
  if (firstAnn.type === 'SUCCESS') return 'announcement-success'
  return 'announcement-info'
}

const closeAnnouncementBar = () => {
  isAnnouncementClosed.value = true
  const today = new Date().toDateString()
  localStorage.setItem(ANNOUNCEMENT_CLOSED_KEY, today)
}

const showAnnouncementAgain = () => {
  localStorage.removeItem(ANNOUNCEMENT_CLOSED_KEY)
  isAnnouncementClosed.value = false
}

const checkAnnouncementClosed = () => {
  const closedDate = localStorage.getItem(ANNOUNCEMENT_CLOSED_KEY)
  const today = new Date().toDateString()
  if (closedDate === today) {
    isAnnouncementClosed.value = true
  }
}

const viewImage = (imgUrl) => {
  viewingImage.value = imgUrl
  showImageViewer.value = true
}

const closeImageViewer = () => {
  showImageViewer.value = false
  viewingImage.value = ''
}

const openPublishModal = () => {
  console.log('打开发布弹窗')
  newPost.value = { content: '', tag: '', images: [] }
  showModal.value = true
  document.body.style.overflow = 'hidden'
}

const closeModal = () => {
  showModal.value = false
  uploadingImages.value = false
  document.body.style.overflow = ''
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

const handleClickOutside = (event) => {
  if (showNotificationPanel.value && !event.target.closest('.notification-panel') && !event.target.closest('.notification-icon')) {
    showNotificationPanel.value = false
  }
}

// 滚动加载更多
const handleScroll = () => {
  const scrollTop = window.scrollY || document.documentElement.scrollTop
  const windowHeight = window.innerHeight
  const documentHeight = document.documentElement.scrollHeight

  // 滚动到底部前200px时加载更多
  if (scrollTop + windowHeight >= documentHeight - 200 && !loading.value && hasMore.value) {
    loadPosts(false)
  }
}

onMounted(() => {
  loadPosts(true)
  loadCurrentUserAvatar()
  loadNotifications()
  loadAnnouncements()
  checkAnnouncementClosed()
  startNotificationPolling()
  document.addEventListener('click', handleClickOutside)
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  if (notificationTimer) clearInterval(notificationTimer)
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.community-container {
  max-width: 800px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ========== 公告栏样式 ========== */
.announcement-bar {
  position: relative;
  background: linear-gradient(135deg, rgba(64, 224, 208, 0.15), rgba(64, 224, 208, 0.05));
  border: 1px solid rgba(64, 224, 208, 0.3);
  border-radius: 16px;
  padding: 10px 16px;
  margin-bottom: 4px;
  backdrop-filter: blur(8px);
  animation: slideDown 0.3s ease-out;
}

.announcement-bar.announcement-warning {
  background: linear-gradient(135deg, rgba(255, 152, 0, 0.15), rgba(255, 152, 0, 0.05));
  border-color: rgba(255, 152, 0, 0.3);
}

.announcement-bar.announcement-success {
  background: linear-gradient(135deg, rgba(124, 179, 66, 0.15), rgba(124, 179, 66, 0.05));
  border-color: rgba(124, 179, 66, 0.3);
}

.announcement-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.announcement-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.announcement-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.announcement-text {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.4;
}

.announcement-item {
  display: inline;
}

.announcement-separator {
  margin: 0 8px;
  color: rgba(255, 255, 255, 0.3);
}

.announcement-close {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  padding: 4px 12px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.6);
  display: flex;
  align-items: center;
  gap: 4px;
  transition: all 0.2s;
  flex-shrink: 0;
  font-size: 12px;
}

.announcement-close:hover {
  background: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.9);
}

.close-icon {
  font-size: 14px;
}

.close-text {
  font-size: 12px;
}

/* 收起后的提示条 */
.announcement-collapsed-tip {
  background: rgba(64, 224, 208, 0.1);
  border: 1px dashed rgba(64, 224, 208, 0.3);
  border-radius: 30px;
  padding: 8px 16px;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s;
  animation: fadeIn 0.3s ease-out;
}

.announcement-collapsed-tip:hover {
  background: rgba(64, 224, 208, 0.2);
  border-color: rgba(64, 224, 208, 0.5);
}

.tip-icon {
  font-size: 16px;
}

.tip-text {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.glass-card {
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 20px;
  transition: all 0.2s;
}

.glass {
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.clickable {
  cursor: pointer;
  transition: opacity 0.2s;
}

.clickable:hover {
  opacity: 0.8;
}

.publish-card {
  cursor: pointer;
}

.publish-card:hover {
  background: rgba(0, 0, 0, 0.35);
  transform: translateY(-2px);
}

.publish-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-small {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #40E0D0, #2BA0D0);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: white;
  flex-shrink: 0;
  overflow: hidden;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.publish-placeholder {
  flex: 1;
  padding: 12px 18px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 40px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
}

.tabs-filter {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.tabs-filter button {
  padding: 10px 28px;
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 40px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  transition: all 0.2s;
}

.tabs-filter button.active {
  background: rgba(64, 224, 208, 0.2);
  border-color: #40E0D0;
  color: #40E0D0;
}

.post-card {
  margin-bottom: 0;
}

.post-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.username {
  font-weight: 600;
  color: white;
  font-size: 15px;
}

.post-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.follow-btn {
  padding: 4px 12px;
  border-radius: 30px;
  font-size: 12px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.2s;
}

.follow-btn:hover {
  background: rgba(64, 224, 208, 0.2);
  border-color: #40E0D0;
  color: #40E0D0;
}

.follow-btn.following {
  background: rgba(64, 224, 208, 0.15);
  border-color: #40E0D0;
  color: #40E0D0;
}

/* 帖子徽章 */
.post-badges {
  display: flex;
  gap: 8px;
  margin-left: auto;
  margin-right: 12px;
}

.post-badge {
  padding: 2px 8px;
  border-radius: 20px;
  font-size: 10px;
  font-weight: 500;
}

.pinned-badge {
  background: rgba(255, 152, 0, 0.2);
  border: 1px solid rgba(255, 152, 0, 0.3);
  color: #FF9800;
}

.featured-badge {
  background: rgba(255, 215, 0, 0.2);
  border: 1px solid rgba(255, 215, 0, 0.3);
  color: #FFD700;
}

.delete-post-btn {
  background: rgba(255, 255, 255, 0.08);
  border: none;
  font-size: 16px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 20px;
  color: rgba(255, 255, 255, 0.5);
}

.delete-post-btn:hover {
  background: rgba(229, 115, 115, 0.2);
  color: #ff8888;
}

.post-content {
  font-size: 15px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 16px;
}

.post-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.post-image-item {
  width: 100px;
  height: 100px;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  background: rgba(0, 0, 0, 0.3);
}

.post-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.post-actions {
  display: flex;
  gap: 32px;
  padding-top: 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  cursor: pointer;
}

.action-btn.active {
  color: #ff6b6b;
}

.report-btn {
  margin-left: auto;
}

.report-btn:hover {
  color: #ff9800;
}

.comment-section {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.comment-list {
  margin-bottom: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.comment-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  font-size: 13px;
}

.comment-avatar {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #40E0D0, #2BA0D0);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: white;
  flex-shrink: 0;
  overflow: hidden;
}

.comment-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.comment-content {
  flex: 1;
}

.comment-user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 4px;
}

.comment-user {
  font-weight: 600;
  color: #40E0D0;
  cursor: pointer;
}

.comment-time {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.4);
}

.follow-badge {
  font-size: 10px;
  padding: 2px 8px;
  background: rgba(64, 224, 208, 0.2);
  border-radius: 20px;
  color: #40E0D0;
}

.follow-badge-small {
  font-size: 9px;
  padding: 1px 5px;
  background: rgba(64, 224, 208, 0.2);
  border-radius: 12px;
  color: #40E0D0;
}

.reply-btn, .report-comment-btn, .report-reply-btn {
  background: none;
  border: none;
  font-size: 11px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.5);
  padding: 2px 6px;
  border-radius: 12px;
}

.reply-btn:hover, .report-comment-btn:hover, .report-reply-btn:hover {
  color: #40E0D0;
  background: rgba(64, 224, 208, 0.1);
}

.report-comment-btn:hover, .report-reply-btn:hover {
  color: #ff9800;
}

.reply-to-reply-btn {
  background: none;
  border: none;
  font-size: 10px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.4);
  padding: 1px 5px;
  border-radius: 10px;
}

.reply-to-reply-btn:hover {
  color: #40E0D0;
}

/* 置顶按钮样式 - 和回复、举报按钮一致 */
.pin-btn {
  background: none;
  border: none;
  font-size: 11px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.5);
  padding: 2px 6px;
  border-radius: 12px;
  transition: all 0.2s;
}

.pin-btn:hover {
  color: #ffc107;
  background: rgba(255, 193, 7, 0.1);
}

.comment-text-wrapper {
  margin-top: 2px;
}

.comment-text {
  color: rgba(255, 255, 255, 0.7);
  word-break: break-word;
}

.replies-list {
  margin-top: 10px;
  margin-left: 10px;
  padding-left: 10px;
  border-left: 2px solid rgba(64, 224, 208, 0.3);
}

.reply-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 0;
  font-size: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.03);
}

.reply-avatar {
  width: 24px;
  height: 24px;
  background: linear-gradient(135deg, #40E0D0, #2BA0D0);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: white;
  flex-shrink: 0;
  overflow: hidden;
}

.reply-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.reply-content {
  flex: 1;
}

.reply-user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 2px;
}

.reply-user {
  font-weight: 600;
  color: #40E0D0;
  font-size: 11px;
  cursor: pointer;
}

.reply-time {
  font-size: 9px;
  color: rgba(255, 255, 255, 0.4);
}

.reply-text {
  color: rgba(255, 255, 255, 0.7);
  font-size: 11px;
}

.reply-to-text {
  color: #40E0D0;
  font-size: 11px;
}

.delete-reply-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.3);
  font-size: 10px;
  padding: 2px 5px;
  border-radius: 10px;
}

.delete-reply-btn:hover {
  color: #ff8888;
  background: rgba(229, 115, 115, 0.15);
}

.delete-comment-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.3);
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 12px;
}

.delete-comment-btn:hover {
  color: #ff8888;
  background: rgba(229, 115, 115, 0.15);
}

.comment-input-area {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.glass-input {
  flex: 1;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 40px;
  padding: 10px 16px;
  color: white;
  font-size: 14px;
  outline: none;
}

.cancel-reply-btn {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 40px;
  color: rgba(255, 255, 255, 0.7);
  cursor: pointer;
  font-size: 12px;
}

.comment-send-btn {
  padding: 8px 24px;
  background: rgba(64, 224, 208, 0.2);
  border: 1px solid rgba(64, 224, 208, 0.4);
  border-radius: 40px;
  color: #40E0D0;
  cursor: pointer;
}

/* 发布弹窗蒙层 - 透明背景，无毛玻璃效果 */
.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

/* 举报弹窗蒙层 - 透明无背景 */
.report-modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.modal-container {
  background: white;
  border-radius: 28px;
  width: 90%;
  max-width: 520px;
  max-height: 90vh;
  overflow-y: auto;
  position: relative;
  z-index: 10000;
}

.report-modal {
  max-width: 420px;
}

.report-reasons {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 16px;
}

.report-reason {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 12px;
  background: rgba(0, 0, 0, 0.05);
}

.report-reason:hover {
  background: rgba(0, 0, 0, 0.1);
}

.report-reason input {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.report-reason span {
  font-size: 14px;
  color: #333;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  color: #333;
  font-size: 20px;
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  cursor: pointer;
  color: #999;
}

.modal-body {
  padding: 24px;
}

.modal-body p {
  margin: 0 0 8px 0;
  color: #333;
}

.post-textarea {
  width: 100%;
  padding: 14px;
  border: 1px solid #ddd;
  border-radius: 16px;
  font-size: 14px;
  resize: none;
  font-family: inherit;
}

.image-upload-section {
  margin: 16px 0;
}

.image-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.image-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
}

.image-item .preview-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.remove-img {
  position: absolute;
  top: -8px;
  right: -8px;
  width: 22px;
  height: 22px;
  background: #e74c3c;
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  font-size: 12px;
}

.upload-image-btn {
  padding: 8px 16px;
  background: #f5f7fa;
  border: 1px dashed #ccc;
  border-radius: 30px;
  cursor: pointer;
  font-size: 13px;
}

.tag-section {
  margin: 20px 0;
}

.tag-label {
  font-size: 13px;
  color: #666;
}

.tag-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 10px;
}

.tag-btn {
  padding: 6px 16px;
  background: #f5f7fa;
  border: none;
  border-radius: 30px;
  cursor: pointer;
}

.tag-btn.active {
  background: #42b983;
  color: white;
}

.modal-footer {
  padding: 16px 24px 24px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-btn {
  padding: 10px 24px;
  background: #f5f7fa;
  border: none;
  border-radius: 30px;
  cursor: pointer;
}

.publish-btn {
  padding: 10px 28px;
  background: #42b983;
  color: white;
  border: none;
  border-radius: 30px;
  cursor: pointer;
}

.publish-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #eee;
  border-top-color: #42b983;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  border-radius: 28px;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.image-viewer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 11000;
  cursor: pointer;
}

.viewer-img {
  max-width: 90%;
  max-height: 90%;
  object-fit: contain;
}

.close-viewer {
  position: absolute;
  top: 20px;
  right: 20px;
  background: rgba(255, 255, 255, 0.2);
  border: none;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  font-size: 20px;
  cursor: pointer;
  color: white;
}

.uploading-progress {
  text-align: center;
  padding: 12px;
  color: #42b983;
}

/* 加载更多样式 */
.loading-more {
  text-align: center;
  padding: 20px;
  color: rgba(255, 255, 255, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.loading-spinner-small {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.2);
  border-top-color: #40E0D0;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

.no-more {
  text-align: center;
  padding: 20px;
  color: rgba(255, 255, 255, 0.3);
  font-size: 12px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .community-container {
    padding: 0 12px;
  }

  .post-image-item {
    width: 80px;
    height: 80px;
  }
}

.notification-icon {
  position: fixed;
  top: 80px;
  right: 20px;
  width: 44px;
  height: 44px;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(10px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 100;
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.2s;
}

.notification-icon:hover {
  background: rgba(0, 0, 0, 0.8);
  transform: scale(1.05);
}

.bell-icon {
  font-size: 22px;
}

.unread-badge {
  position: absolute;
  top: -5px;
  right: -5px;
  background: #ff4444;
  color: white;
  font-size: 10px;
  font-weight: bold;
  padding: 2px 6px;
  border-radius: 20px;
  min-width: 18px;
  text-align: center;
}

.notification-panel {
  position: fixed;
  top: 130px;
  right: 20px;
  width: 350px;
  max-height: 500px;
  background: rgba(30, 30, 40, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  z-index: 101;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.3);
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.notification-header h4 {
  margin: 0;
  color: white;
  font-size: 16px;
}

.mark-all-read {
  background: none;
  border: none;
  color: #40E0D0;
  font-size: 12px;
  cursor: pointer;
}

.notification-list {
  max-height: 450px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  cursor: pointer;
  transition: background 0.2s;
}

.notification-item:hover {
  background: rgba(255, 255, 255, 0.05);
}

.notification-item.unread {
  background: rgba(64, 224, 208, 0.1);
}

.notification-avatar {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #40E0D0, #2BA0D0);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.notification-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.notification-content {
  flex: 1;
}

.notification-text {
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
}

.notification-text strong {
  color: #40E0D0;
}

.notification-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 4px;
}

.empty-notification {
  text-align: center;
  padding: 40px;
  color: rgba(255, 255, 255, 0.5);
}

.highlight-post {
  animation: highlightFlash 0.5s ease-in-out 3;
  box-shadow: 0 0 0 2px #40E0D0, 0 0 0 4px rgba(64, 224, 208, 0.3);
}

.highlight-comment {
  animation: highlightFlash 0.5s ease-in-out 3;
  background: rgba(64, 224, 208, 0.2);
  border-radius: 12px;
}

@keyframes highlightFlash {
  0% { background-color: rgba(64, 224, 208, 0); }
  50% { background-color: rgba(64, 224, 208, 0.3); }
  100% { background-color: rgba(64, 224, 208, 0); }
}

.comment-actions {
  margin-top: 6px;
}

.comment-like-btn, .reply-like-btn {
  background: none;
  border: none;
  font-size: 11px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.5);
  padding: 2px 6px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.comment-like-btn:hover, .reply-like-btn:hover {
  color: #ff6b6b;
  background: rgba(255, 107, 107, 0.1);
}

.reply-actions {
  margin-top: 4px;
}

@media (max-width: 768px) {
  .notification-panel {
    width: calc(100% - 40px);
    right: 20px;
    left: 20px;
  }
  .notification-icon {
    top: 70px;
    right: 15px;
    width: 40px;
    height: 40px;
  }
  .post-actions {
    gap: 16px;
    flex-wrap: wrap;
  }
  .report-btn {
    margin-left: 0;
  }
}
</style>