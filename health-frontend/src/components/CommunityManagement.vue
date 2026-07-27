<template>
  <div class="community-management">
    <div class="sub-tabs">
      <button v-for="subTab in communitySubTabs" :key="subTab.key" :class="{ active: communitySubTab === subTab.key }" @click="switchCommunitySubTab(subTab.key)">
        {{ subTab.label }}
      </button>
    </div>

    <!-- 帖子管理 -->
    <div v-if="communitySubTab === 'posts'">
      <div class="panel-header">
        <h3>📋 帖子管理</h3>
        <div class="header-actions">
          <div class="sort-select">
            <span>排序：</span>
            <div class="custom-select-sort" ref="sortSelectRef">
              <div class="custom-select-trigger" @click.stop="toggleSortDropdown">
                <span>{{ getSortLabel() }}</span>
                <span class="arrow" :style="{ transform: showSortDropdown ? 'rotate(180deg)' : 'rotate(0deg)' }">▼</span>
              </div>
              <div v-if="showSortDropdown" class="custom-select-dropdown">
                <div class="custom-select-option" :class="{ active: postSortBy === 'time_desc' }" @click.stop="selectSort('time_desc')">📅 最新优先</div>
                <div class="custom-select-option" :class="{ active: postSortBy === 'time_asc' }" @click.stop="selectSort('time_asc')">📅 最早优先</div>
                <div class="custom-select-option" :class="{ active: postSortBy === 'likes_desc' }" @click.stop="selectSort('likes_desc')">❤️ 点赞最多</div>
                <div class="custom-select-option" :class="{ active: postSortBy === 'likes_asc' }" @click.stop="selectSort('likes_asc')">❤️ 点赞最少</div>
                <div class="custom-select-option" :class="{ active: postSortBy === 'comments_desc' }" @click.stop="selectSort('comments_desc')">💬 评论最多</div>
                <div class="custom-select-option" :class="{ active: postSortBy === 'comments_asc' }" @click.stop="selectSort('comments_asc')">💬 评论最少</div>
              </div>
            </div>
          </div>
          <button v-if="!showPostCheckbox" class="btn-batch" @click="enterBatchMode">📋 批量删除</button>
          <template v-if="showPostCheckbox">
            <button class="btn-danger" @click="batchDeletePosts" :disabled="selectedPostIds.length === 0">
              确认删除 ({{ selectedPostIds.length }})
            </button>
            <button class="btn-secondary" @click="exitBatchMode">取消</button>
          </template>
        </div>
      </div>
      <div class="search-box">
        <input type="text" v-model="postSearch" placeholder="搜索帖子内容..." class="admin-input" @keyup.enter="loadPosts" />
        <button @click="loadPosts" class="btn-search">搜索</button>
      </div>
      <div class="table-wrapper">
        <table class="admin-table">
          <thead>
          <tr>
            <th v-if="showPostCheckbox" style="width: 40px;">
              <input type="checkbox" @change="toggleSelectAllPosts" :checked="selectAllPosts" />
            </th>
            <th style="width: 60px;">ID</th>
            <th>内容</th>
            <th style="width: 100px;">用户</th>
            <th style="width: 70px;">点赞数</th>
            <th style="width: 70px;">评论数</th>
            <th style="width: 90px;">置顶</th>
            <th style="width: 90px;">加精</th>
            <th style="width: 140px;">创建时间</th>
            <th style="width: 100px;">操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="post in posts" :key="post.id">
            <td v-if="showPostCheckbox" style="text-align: center;">
              <input type="checkbox" v-model="selectedPostIds" :value="post.id" />
            </td>
            <td style="text-align: center;">{{ post.id }}</td>
            <td class="content-cell clickable" @click="viewPostDetail(post)" :title="'点击查看完整内容'">
              {{ truncate(post.content, 80) }}
            </td>
            <td class="clickable" @click="goToUserProfile(post.userId)">{{ post.nickname || post.username || '用户' + post.userId }}</td>
            <td style="text-align: center;">{{ post.likeCount || 0 }}</td>
            <td style="text-align: center;">{{ post.commentCount || 0 }}</td>
            <td style="text-align: center;">
              <button class="pin-btn" :class="{ active: post.isPinned === 1 }" @click="togglePinPost(post.id, post.isPinned)">
                {{ post.isPinned === 1 ? '📌 已置顶' : '📍 置顶' }}
              </button>
            </td>
            <td style="text-align: center;">
              <button class="feature-btn" :class="{ active: post.isFeatured === 1 }" @click="toggleFeaturePost(post.id, post.isFeatured)">
                {{ post.isFeatured === 1 ? '⭐ 已加精' : '☆ 加精' }}
              </button>
            </td>
            <td>{{ formatDateTime(post.createTime) }}</td>
            <td class="action-btns">
              <button class="btn-icon view" @click="viewPostDetail(post)" title="查看详情">👁️</button>
              <button class="btn-icon delete" @click="deletePost(post.id)" title="删除">🗑️</button>
            </td>
          </tr>
          <tr v-if="posts.length === 0">
            <td :colspan="showPostCheckbox ? 11 : 10" style="text-align: center; padding: 40px;">暂无帖子数据</td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination" v-if="postTotal > 0">
        <button @click="postPage--" :disabled="postPage === 0">上一页</button>
        <span>第 {{ postPage + 1 }} / {{ postTotalPages }} 页</span>
        <button @click="postPage++" :disabled="postPage >= postTotalPages - 1">下一页</button>
      </div>
    </div>

    <!-- 评论管理 -->
    <div v-if="communitySubTab === 'comments'">
      <div class="panel-header">
        <h3>💬 评论管理</h3>
        <div class="header-actions">
          <button v-if="!showCommentCheckbox" class="btn-batch" @click="enterCommentBatchMode">📋 批量删除</button>
          <template v-if="showCommentCheckbox">
            <button class="btn-danger" @click="batchDeleteComments" :disabled="selectedCommentIds.length === 0">
              确认删除 ({{ selectedCommentIds.length }})
            </button>
            <button class="btn-secondary" @click="exitCommentBatchMode">取消</button>
          </template>
        </div>
      </div>
      <div class="search-box">
        <input type="text" v-model="commentSearch" placeholder="搜索评论内容..." class="admin-input" @keyup.enter="loadComments" />
        <button @click="loadComments" class="btn-search">搜索</button>
      </div>
      <div class="table-wrapper">
        <table class="admin-table">
          <thead>
          <tr>
            <th v-if="showCommentCheckbox" style="width: 40px;">
              <input type="checkbox" @change="toggleSelectAllComments" :checked="selectAllComments" />
            </th>
            <th style="width: 60px;">ID</th>
            <th>内容</th>
            <th style="width: 100px;">用户</th>
            <th style="width: 100px;">所属帖子</th>
            <th style="width: 140px;">创建时间</th>
            <th style="width: 80px;">操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="comment in comments" :key="comment.id">
            <td v-if="showCommentCheckbox" style="text-align: center;">
              <input type="checkbox" v-model="selectedCommentIds" :value="comment.id" />
            </td>
            <td style="text-align: center;">{{ comment.id }}</td>
            <td class="title-cell" :title="comment.content">{{ truncate(comment.content, 50) }}</td>
            <td class="clickable" @click="goToUserProfile(comment.userId)">{{ comment.nickname || comment.username || '用户' + comment.userId }}</td>
            <td class="clickable" @click="goToPost(comment.postId)" style="text-align: center;">{{ comment.postId }}</td>
            <td>{{ formatDateTime(comment.createTime) }}</td>
            <td class="action-btns">
              <button class="btn-icon delete" @click="deleteComment(comment.id)" title="删除">🗑️</button>
            </td>
          </tr>
          <tr v-if="comments.length === 0">
            <td :colspan="showCommentCheckbox ? 8 : 7" style="text-align: center; padding: 40px;">暂无评论数据</td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination" v-if="commentTotal > 0">
        <button @click="commentPage--" :disabled="commentPage === 0">上一页</button>
        <span>第 {{ commentPage + 1 }} / {{ commentTotalPages }} 页</span>
        <button @click="commentPage++" :disabled="commentPage >= commentTotalPages - 1">下一页</button>
      </div>
    </div>

    <!-- 举报处理 -->
    <div v-if="communitySubTab === 'reports'">
      <div class="panel-header">
        <h3>🚨 举报处理</h3>
      </div>
      <div class="filter-bar">
        <label>状态筛选：</label>
        <div class="custom-select-sort" ref="reportStatusSelectRef">
          <div class="custom-select-trigger" @click.stop="toggleReportStatusDropdown">
            <span>{{ getReportStatusLabel() }}</span>
            <span class="arrow" :style="{ transform: showReportStatusDropdown ? 'rotate(180deg)' : 'rotate(0deg)' }">▼</span>
          </div>
          <div v-if="showReportStatusDropdown" class="custom-select-dropdown">
            <div class="custom-select-option" :class="{ active: reportStatus === null }" @click.stop="selectReportStatus(null)">全部</div>
            <div class="custom-select-option" :class="{ active: reportStatus === 0 }" @click.stop="selectReportStatus(0)">待处理</div>
            <div class="custom-select-option" :class="{ active: reportStatus === 1 }" @click.stop="selectReportStatus(1)">已处理</div>
            <div class="custom-select-option" :class="{ active: reportStatus === 2 }" @click.stop="selectReportStatus(2)">已驳回</div>
          </div>
        </div>
      </div>
      <div class="table-wrapper">
        <table class="admin-table">
          <thead>
          <tr>
            <th>ID</th>
            <th>举报人</th>
            <th>被举报内容</th>
            <th>举报原因</th>
            <th>状态</th>
            <th>举报时间</th>
            <th>操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="report in reports" :key="report.id">
            <td style="text-align: center;">{{ report.id }}</td>
            <td class="clickable" @click="goToUserProfile(report.reporterId)">{{ report.reporterName || '用户' + report.reporterId }}</td>
            <td class="title-cell" :title="report.targetContent">{{ truncate(report.targetContent, 60) || '-' }}</td>
            <td class="title-cell" :title="report.reason">{{ truncate(report.reason, 30) || '-' }}</td>
            <td style="text-align: center;">
                <span :class="['status-badge', report.status === 0 ? 'status-pending' : report.status === 1 ? 'status-approved' : 'status-rejected']">
                  {{ report.status === 0 ? '待处理' : report.status === 1 ? '已处理' : '已驳回' }}
                </span>
            </td>
            <td>{{ formatDateTime(report.createTime) }}</td>
            <td class="action-btns" v-if="report.status === 0">
              <button class="btn-icon approve" @click="handleReport(report.id, 1, '内容违规，已删除')" title="通过并删除">✅ 通过</button>
              <button class="btn-icon reject" @click="handleReport(report.id, 2, '举报不成立')" title="驳回">❌ 驳回</button>
            </td>
            <td v-else class="action-btns" style="color: rgba(255,255,255,0.4);">-</td>
          </tr>
          <tr v-if="reports.length === 0">
            <td colspan="7" style="text-align: center; padding: 40px;">暂无举报数据</td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination" v-if="reportTotal > 0">
        <button @click="reportPage--" :disabled="reportPage === 0">上一页</button>
        <span>第 {{ reportPage + 1 }} / {{ reportTotalPages }} 页</span>
        <button @click="reportPage++" :disabled="reportPage >= reportTotalPages - 1">下一页</button>
      </div>
    </div>

    <!-- 公告管理 -->
    <div v-if="communitySubTab === 'announcements'">
      <div class="panel-header">
        <h3>📢 公告管理</h3>
        <button class="btn-primary" @click="openAnnouncementModal">+ 新建公告</button>
      </div>
      <div class="table-wrapper">
        <table class="admin-table">
          <thead>
          <tr>
            <th>ID</th>
            <th>标题</th>
            <th>内容</th>
            <th>类型</th>
            <th>状态</th>
            <th>排序</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="announcement in announcements" :key="announcement.id">
            <td style="text-align: center;">{{ announcement.id }}</td>
            <td class="title-cell" :title="announcement.title">{{ truncate(announcement.title, 30) }}</td>
            <td class="title-cell" :title="announcement.content">{{ truncate(announcement.content, 40) }}</td>
            <td style="text-align: center;">
                <span :class="['type-badge', announcement.type === 'WARNING' ? 'type-warning' : announcement.type === 'SUCCESS' ? 'type-success' : 'type-info']">
                  {{ announcement.type === 'WARNING' ? '⚠️ 警告' : announcement.type === 'SUCCESS' ? '✅ 成功' : 'ℹ️ 信息' }}
                </span>
            </td>
            <td style="text-align: center;">
                <span :class="['status-badge', announcement.isActive === 1 ? 'status-published' : 'status-draft']" style="cursor: pointer;" @click="toggleAnnouncementStatus(announcement.id)">
                  {{ announcement.isActive === 1 ? '已启用' : '已禁用' }}
                </span>
            </td>
            <td style="text-align: center;">
              <button class="sort-btn" @click="increaseSortOrder(announcement.id)">⬆️ {{ announcement.sortOrder || 0 }} ⬇️</button>
            </td>
            <td>{{ formatDateTime(announcement.createTime) }}</td>
            <td class="action-btns">
              <button class="btn-icon edit" @click="editAnnouncement(announcement)" title="编辑">✏️</button>
              <button class="btn-icon delete" @click="deleteAnnouncement(announcement.id)" title="删除">🗑️</button>
            </td>
          </tr>
          <tr v-if="announcements.length === 0">
            <td colspan="8" style="text-align: center; padding: 40px;">暂无公告数据</td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination" v-if="announcementTotal > 0">
        <button @click="announcementPage--" :disabled="announcementPage === 0">上一页</button>
        <span>第 {{ announcementPage + 1 }} / {{ announcementTotalPages }} 页</span>
        <button @click="announcementPage++" :disabled="announcementPage >= announcementTotalPages - 1">下一页</button>
      </div>
    </div>

    <!-- 帖子详情弹窗 -->
    <Teleport to="body">
      <div v-if="showPostDetailModal" class="modal-mask" @click.self="closePostDetailModal">
        <div class="modal-container post-detail-modal">
          <div class="modal-header">
            <h3>📄 帖子详情</h3>
            <button class="close-btn" @click="closePostDetailModal">×</button>
          </div>
          <div class="modal-body post-detail-body">
            <div class="detail-item">
              <span class="detail-label">帖子ID：</span>
              <span class="detail-value">{{ currentPost.id }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">作者：</span>
              <span class="detail-value clickable" @click="goToUserProfile(currentPost.userId)">{{ currentPost.nickname || currentPost.username || '用户' + currentPost.userId }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">内容：</span>
              <div class="detail-content">{{ currentPost.content }}</div>
            </div>
            <div v-if="currentPost.images && currentPost.images.length > 0" class="detail-item">
              <span class="detail-label">图片：</span>
              <div class="detail-images">
                <img v-for="(img, idx) in currentPost.images" :key="idx" :src="getFullImageUrl(img)" class="detail-img" @click="viewImage(img)" />
              </div>
            </div>
            <div class="detail-item">
              <span class="detail-label">点赞数：</span>
              <span class="detail-value">{{ currentPost.likeCount || 0 }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">评论数：</span>
              <span class="detail-value">{{ currentPost.commentCount || 0 }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">创建时间：</span>
              <span class="detail-value">{{ formatDateTime(currentPost.createTime) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">状态：</span>
              <span class="detail-value">
                <span v-if="currentPost.isPinned === 1" class="post-badge pinned-badge">📌 置顶</span>
                <span v-if="currentPost.isFeatured === 1" class="post-badge featured-badge">⭐ 精华</span>
              </span>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="closePostDetailModal">关闭</button>
            <button class="btn-danger" @click="deletePost(currentPost.id)" v-if="currentPost.id">删除帖子</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 公告编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showAnnouncementModal" class="modal-mask" @click.self="closeAnnouncementModal">
        <div class="modal-container announcement-modal">
          <div class="modal-header">
            <h3>{{ isEditingAnnouncement ? '编辑公告' : '新建公告' }}</h3>
            <button class="close-btn" @click="closeAnnouncementModal">×</button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>标题</label>
              <input v-model="announcementForm.title" class="admin-input" placeholder="请输入公告标题" />
            </div>
            <div class="form-group">
              <label>内容</label>
              <textarea v-model="announcementForm.content" class="admin-textarea" rows="4" placeholder="请输入公告内容"></textarea>
            </div>
            <div class="form-group">
              <label>类型</label>
              <div class="custom-select-sort" ref="announcementTypeSelectRef">
                <div class="custom-select-trigger" @click.stop="toggleAnnouncementTypeDropdown">
                  <span>{{ getAnnouncementTypeLabel() }}</span>
                  <span class="arrow" :style="{ transform: showAnnouncementTypeDropdown ? 'rotate(180deg)' : 'rotate(0deg)' }">▼</span>
                </div>
                <div v-if="showAnnouncementTypeDropdown" class="custom-select-dropdown">
                  <div class="custom-select-option" :class="{ active: announcementForm.type === 'INFO' }" @click.stop="selectAnnouncementType('INFO')">ℹ️ 信息</div>
                  <div class="custom-select-option" :class="{ active: announcementForm.type === 'WARNING' }" @click.stop="selectAnnouncementType('WARNING')">⚠️ 警告</div>
                  <div class="custom-select-option" :class="{ active: announcementForm.type === 'SUCCESS' }" @click.stop="selectAnnouncementType('SUCCESS')">✅ 成功</div>
                </div>
              </div>
            </div>
            <div class="form-group">
              <label>排序（数字越小越靠前）</label>
              <input type="number" v-model="announcementForm.sortOrder" class="admin-input" />
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="closeAnnouncementModal">取消</button>
            <button class="btn-primary" @click="saveAnnouncement">保存</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import axios from 'axios'

const props = defineProps({
  userId: { type: Number, required: true }
})

const emit = defineEmits(['view-profile', 'view-post'])

const API_BASE = 'http://localhost:8080/api'
const pageSize = 10

// ========== 社区管理相关变量 ==========
const communitySubTab = ref('posts')
const communitySubTabs = [
  { key: 'posts', label: '📋 帖子管理' },
  { key: 'comments', label: '💬 评论管理' },
  { key: 'reports', label: '🚨 举报处理' },
  { key: 'announcements', label: '📢 公告管理' }
]

// 帖子管理
const posts = ref([])
const postSearch = ref('')
const postPage = ref(0)
const postTotal = ref(0)
const postTotalPages = ref(0)
const postSortBy = ref('time_desc')
const selectedPostIds = ref([])
const selectAllPosts = ref(false)
const showPostCheckbox = ref(false)
const showPostDetailModal = ref(false)
const currentPost = ref({})

// 排序下拉框
const showSortDropdown = ref(false)
const sortSelectRef = ref(null)

const getSortLabel = () => {
  const sortMap = {
    'time_desc': '📅 最新优先',
    'time_asc': '📅 最早优先',
    'likes_desc': '❤️ 点赞最多',
    'likes_asc': '❤️ 点赞最少',
    'comments_desc': '💬 评论最多',
    'comments_asc': '💬 评论最少'
  }
  return sortMap[postSortBy.value] || '📅 最新优先'
}

const toggleSortDropdown = () => {
  showSortDropdown.value = !showSortDropdown.value
}

const selectSort = (sort) => {
  postSortBy.value = sort
  showSortDropdown.value = false
  loadPosts()
}

// 评论管理
const comments = ref([])
const commentSearch = ref('')
const commentPage = ref(0)
const commentTotal = ref(0)
const commentTotalPages = ref(0)
const selectedCommentIds = ref([])
const selectAllComments = ref(false)
const showCommentCheckbox = ref(false)

// 举报处理
const reports = ref([])
const reportStatus = ref(null)
const reportPage = ref(0)
const reportTotal = ref(0)
const reportTotalPages = ref(0)

// 举报状态下拉框
const showReportStatusDropdown = ref(false)
const reportStatusSelectRef = ref(null)

const getReportStatusLabel = () => {
  const statusMap = {
    null: '全部',
    0: '待处理',
    1: '已处理',
    2: '已驳回'
  }
  return statusMap[reportStatus.value] || '全部'
}

const toggleReportStatusDropdown = () => {
  showReportStatusDropdown.value = !showReportStatusDropdown.value
}

const selectReportStatus = (status) => {
  reportStatus.value = status
  showReportStatusDropdown.value = false
  reportPage.value = 0
  loadReports()
}

// 公告管理
const announcements = ref([])
const announcementPage = ref(0)
const announcementTotal = ref(0)
const announcementTotalPages = ref(0)
const showAnnouncementModal = ref(false)
const isEditingAnnouncement = ref(false)
const announcementForm = ref({
  id: null,
  title: '',
  content: '',
  type: 'INFO',
  sortOrder: 0
})

// 公告类型下拉框
const showAnnouncementTypeDropdown = ref(false)
const announcementTypeSelectRef = ref(null)

const getAnnouncementTypeLabel = () => {
  const typeMap = {
    'INFO': 'ℹ️ 信息',
    'WARNING': '⚠️ 警告',
    'SUCCESS': '✅ 成功'
  }
  return typeMap[announcementForm.value.type] || 'ℹ️ 信息'
}

const toggleAnnouncementTypeDropdown = () => {
  showAnnouncementTypeDropdown.value = !showAnnouncementTypeDropdown.value
}

const selectAnnouncementType = (type) => {
  announcementForm.value.type = type
  showAnnouncementTypeDropdown.value = false
}

// ========== 通用方法 ==========
const truncate = (text, maxLen) => {
  if (!text) return '-'
  if (text.length <= maxLen) return text
  return text.substring(0, maxLen) + '...'
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

const getFullImageUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http')) return url
  if (url.startsWith('/uploads')) return `http://localhost:8080${url}`
  return url
}

const viewImage = (imgUrl) => {
  window.open(getFullImageUrl(imgUrl), '_blank')
}

const goToUserProfile = (targetUserId) => {
  emit('view-profile', targetUserId)
}

const goToPost = (postId) => {
  emit('view-post', postId)
}

// ========== 帖子管理方法 ==========
const loadPosts = async () => {
  try {
    const params = { page: postPage.value, size: pageSize, sort: postSortBy.value }
    if (postSearch.value) params.keyword = postSearch.value
    const res = await axios.get(`${API_BASE}/admin/posts`, { params })
    if (res.data.success) {
      posts.value = res.data.data || []
      postTotal.value = res.data.total || 0
      postTotalPages.value = res.data.totalPages || 0
      if (!showPostCheckbox.value) {
        selectedPostIds.value = []
        selectAllPosts.value = false
      }
    }
  } catch (e) {
    console.error('加载帖子失败', e)
  }
}

const enterBatchMode = () => {
  showPostCheckbox.value = true
  selectedPostIds.value = []
  selectAllPosts.value = false
}

const exitBatchMode = () => {
  showPostCheckbox.value = false
  selectedPostIds.value = []
  selectAllPosts.value = false
}

const toggleSelectAllPosts = () => {
  if (selectAllPosts.value) {
    selectedPostIds.value = []
    selectAllPosts.value = false
  } else {
    selectedPostIds.value = posts.value.map(p => p.id)
    selectAllPosts.value = true
  }
}

const deletePost = async (postId) => {
  if (!confirm('确定要删除这条帖子吗？')) return
  try {
    await axios.delete(`${API_BASE}/admin/post/${postId}`)
    alert('删除成功')
    if (showPostDetailModal.value) closePostDetailModal()
    await loadPosts()
  } catch (e) {
    alert('删除失败')
  }
}

const batchDeletePosts = async () => {
  if (selectedPostIds.value.length === 0) {
    alert('请先选择要删除的帖子')
    return
  }
  if (!confirm(`确定要删除选中的 ${selectedPostIds.value.length} 条帖子吗？此操作不可恢复！`)) return
  try {
    const res = await axios.post(`${API_BASE}/admin/posts/batch-delete`, selectedPostIds.value)
    if (res.data.success) {
      alert(res.data.message)
      exitBatchMode()
      await loadPosts()
    } else {
      alert(res.data.message)
    }
  } catch (e) {
    alert('批量删除失败')
  }
}

const togglePinPost = async (postId, currentStatus) => {
  const newStatus = currentStatus === 1 ? 0 : 1
  try {
    await axios.put(`${API_BASE}/admin/post/${postId}/pin`, null, { params: { isPinned: newStatus } })
    alert(newStatus === 1 ? '置顶成功' : '取消置顶成功')
    await loadPosts()
  } catch (e) {
    alert('操作失败')
  }
}

const toggleFeaturePost = async (postId, currentStatus) => {
  const newStatus = currentStatus === 1 ? 0 : 1
  try {
    await axios.put(`${API_BASE}/admin/post/${postId}/feature`, null, { params: { isFeatured: newStatus } })
    alert(newStatus === 1 ? '加精成功' : '取消加精成功')
    await loadPosts()
  } catch (e) {
    alert('操作失败')
  }
}

const viewPostDetail = (post) => {
  currentPost.value = { ...post, images: post.images ? (typeof post.images === 'string' ? post.images.split(',') : post.images) : [] }
  showPostDetailModal.value = true
}

const closePostDetailModal = () => {
  showPostDetailModal.value = false
  currentPost.value = {}
}

// ========== 评论管理方法 ==========
const loadComments = async () => {
  try {
    const params = { page: commentPage.value, size: pageSize }
    if (commentSearch.value) params.keyword = commentSearch.value
    const res = await axios.get(`${API_BASE}/admin/comments`, { params })
    if (res.data.success) {
      comments.value = res.data.data || []
      commentTotal.value = res.data.total || 0
      commentTotalPages.value = res.data.totalPages || 0
      if (!showCommentCheckbox.value) {
        selectedCommentIds.value = []
        selectAllComments.value = false
      }
    }
  } catch (e) {
    console.error('加载评论失败', e)
  }
}

const enterCommentBatchMode = () => {
  showCommentCheckbox.value = true
  selectedCommentIds.value = []
  selectAllComments.value = false
}

const exitCommentBatchMode = () => {
  showCommentCheckbox.value = false
  selectedCommentIds.value = []
  selectAllComments.value = false
}

const toggleSelectAllComments = () => {
  if (selectAllComments.value) {
    selectedCommentIds.value = []
    selectAllComments.value = false
  } else {
    selectedCommentIds.value = comments.value.map(c => c.id)
    selectAllComments.value = true
  }
}

const deleteComment = async (commentId) => {
  if (!confirm('确定要删除这条评论吗？')) return
  try {
    await axios.delete(`${API_BASE}/admin/comment/${commentId}`)
    alert('删除成功')
    await loadComments()
  } catch (e) {
    alert('删除失败')
  }
}

const batchDeleteComments = async () => {
  if (selectedCommentIds.value.length === 0) {
    alert('请先选择要删除的评论')
    return
  }
  if (!confirm(`确定要删除选中的 ${selectedCommentIds.value.length} 条评论吗？`)) return
  try {
    await axios.post(`${API_BASE}/admin/comments/batch-delete`, selectedCommentIds.value)
    alert('批量删除成功')
    exitCommentBatchMode()
    await loadComments()
  } catch (e) {
    alert('删除失败')
  }
}

// ========== 举报处理方法 ==========
const loadReports = async () => {
  try {
    const params = { page: reportPage.value, size: pageSize }
    if (reportStatus.value !== null) params.status = reportStatus.value
    const res = await axios.get(`${API_BASE}/admin/reports`, { params })
    if (res.data.success) {
      reports.value = res.data.data || []
      reportTotal.value = res.data.total || 0
      reportTotalPages.value = res.data.totalPages || 0
    }
  } catch (e) {
    console.error('加载举报失败', e)
  }
}

const handleReport = async (reportId, action, handleNote) => {
  const actionText = action === 1 ? '通过并删除内容' : '驳回'
  if (!confirm(`确认${actionText}这条举报吗？`)) return
  try {
    await axios.post(`${API_BASE}/admin/report/${reportId}/handle`, null, {
      params: { action, handleNote, adminId: props.userId }
    })
    alert(action === 1 ? '已通过举报，内容已删除' : '已驳回举报')
    await loadReports()
  } catch (e) {
    alert('操作失败')
  }
}

// ========== 公告管理方法 ==========
const loadAnnouncements = async () => {
  try {
    const params = { page: announcementPage.value, size: pageSize }
    const res = await axios.get(`${API_BASE}/admin/announcements`, { params })
    if (res.data.success) {
      announcements.value = res.data.data || []
      announcementTotal.value = res.data.total || 0
      announcementTotalPages.value = res.data.totalPages || 0
    }
  } catch (e) {
    console.error('加载公告失败', e)
  }
}

const openAnnouncementModal = () => {
  isEditingAnnouncement.value = false
  announcementForm.value = { id: null, title: '', content: '', type: 'INFO', sortOrder: 0 }
  showAnnouncementModal.value = true
}

const editAnnouncement = (announcement) => {
  isEditingAnnouncement.value = true
  announcementForm.value = { ...announcement }
  showAnnouncementModal.value = true
}

const saveAnnouncement = async () => {
  if (!announcementForm.value.title.trim()) { alert('请输入公告标题'); return }
  if (!announcementForm.value.content.trim()) { alert('请输入公告内容'); return }
  try {
    if (isEditingAnnouncement.value) {
      await axios.put(`${API_BASE}/admin/announcement/${announcementForm.value.id}`, announcementForm.value)
      alert('公告更新成功')
    } else {
      await axios.post(`${API_BASE}/admin/announcement`, announcementForm.value)
      alert('公告创建成功')
    }
    closeAnnouncementModal()
    await loadAnnouncements()
  } catch (e) {
    alert('保存失败')
  }
}

const deleteAnnouncement = async (id) => {
  if (!confirm('确定要删除这条公告吗？')) return
  try {
    await axios.delete(`${API_BASE}/admin/announcement/${id}`)
    alert('删除成功')
    await loadAnnouncements()
  } catch (e) {
    alert('删除失败')
  }
}

const toggleAnnouncementStatus = async (id) => {
  try {
    await axios.put(`${API_BASE}/admin/announcement/${id}/toggle`)
    alert('状态已更新')
    await loadAnnouncements()
  } catch (e) {
    alert('操作失败')
  }
}

const increaseSortOrder = async (id) => {
  const newOrder = prompt('请输入排序值（数字越小越靠前）')
  if (newOrder === null) return
  const announcement = announcements.value.find(a => a.id === id)
  if (announcement) {
    announcement.sortOrder = parseInt(newOrder)
    try {
      await axios.put(`${API_BASE}/admin/announcement/${id}`, announcement)
      alert('排序已更新')
      await loadAnnouncements()
    } catch (e) {
      alert('更新失败')
    }
  }
}

const closeAnnouncementModal = () => {
  showAnnouncementModal.value = false
  isEditingAnnouncement.value = false
}

// ========== 切换子Tab ==========
const switchCommunitySubTab = (subTab) => {
  communitySubTab.value = subTab
  exitBatchMode()
  exitCommentBatchMode()
  if (subTab === 'posts') loadPosts()
  else if (subTab === 'comments') loadComments()
  else if (subTab === 'reports') loadReports()
  else if (subTab === 'announcements') loadAnnouncements()
}

// ========== 点击外部关闭下拉框 ==========
const handleClickOutside = (event) => {
  if (sortSelectRef.value && !sortSelectRef.value.contains(event.target)) {
    showSortDropdown.value = false
  }
  if (reportStatusSelectRef.value && !reportStatusSelectRef.value.contains(event.target)) {
    showReportStatusDropdown.value = false
  }
  if (announcementTypeSelectRef.value && !announcementTypeSelectRef.value.contains(event.target)) {
    showAnnouncementTypeDropdown.value = false
  }
}

// ========== 监听分页变化 ==========
watch(postPage, () => loadPosts())
watch(commentPage, () => loadComments())
watch(reportPage, () => loadReports())
watch(announcementPage, () => loadAnnouncements())

watch(() => selectedCommentIds.value, (newVal) => {
  selectAllComments.value = newVal.length === comments.value.length && comments.value.length > 0
})

watch(() => selectedPostIds.value, (newVal) => {
  selectAllPosts.value = newVal.length === posts.value.length && posts.value.length > 0
})

// ========== 初始化加载 ==========
onMounted(() => {
  loadPosts()
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
/* 社区管理组件样式 */
.community-management {
  width: 100%;
}

.sub-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sub-tabs button {
  padding: 8px 20px;
  background: transparent;
  border: none;
  border-radius: 24px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  transition: all 0.2s;
}

.sub-tabs button:hover {
  color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.05);
}

.sub-tabs button.active {
  background: rgba(64, 224, 208, 0.2);
  color: #40E0D0;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.panel-header h3 {
  color: white;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.sort-select {
  display: flex;
  align-items: center;
  gap: 8px;
  color: white;
  font-size: 13px;
}

/* 统一的下拉框样式 - 与 AdminPanel 中的 .custom-select-sort 一致 */
.custom-select-sort {
  position: relative;
  width: 160px;
}

.custom-select-sort .custom-select-trigger {
  background: rgba(30, 30, 50, 0.85);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(64, 224, 208, 0.5);
  border-radius: 24px;
  padding: 8px 32px 8px 16px;
  color: #40E0D0;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s;
}

.custom-select-sort .custom-select-trigger:hover {
  background: rgba(40, 40, 60, 0.9);
  border-color: #40E0D0;
}

.custom-select-sort .custom-select-trigger .arrow {
  font-size: 10px;
  transition: transform 0.2s;
  color: #40E0D0;
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
}

.custom-select-sort .custom-select-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 8px;
  background: rgba(30, 30, 50, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(64, 224, 208, 0.3);
  border-radius: 20px;
  overflow: hidden;
  z-index: 100;
}

.custom-select-sort .custom-select-option {
  padding: 10px 16px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.custom-select-sort .custom-select-option:hover {
  background: rgba(64, 224, 208, 0.2);
  color: #40E0D0;
}

.custom-select-sort .custom-select-option.active {
  background: rgba(64, 224, 208, 0.15);
  color: #40E0D0;
}

.search-box {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.admin-input {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 24px;
  padding: 10px 18px;
  color: white;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
}

.admin-input:focus {
  border-color: rgba(64, 224, 208, 0.5);
  background: rgba(255, 255, 255, 0.12);
}

.btn-primary {
  padding: 10px 24px;
  background: rgba(64, 224, 208, 0.2);
  border: 1px solid rgba(64, 224, 208, 0.4);
  border-radius: 24px;
  color: #40E0D0;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-primary:hover {
  background: rgba(64, 224, 208, 0.4);
  transform: scale(1.02);
}

.btn-secondary {
  padding: 10px 24px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 24px;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-secondary:hover {
  background: rgba(255, 255, 255, 0.2);
}

.btn-danger {
  padding: 8px 20px;
  background: rgba(229, 115, 115, 0.2);
  border: 1px solid rgba(229, 115, 115, 0.4);
  border-radius: 24px;
  color: #ff8888;
  cursor: pointer;
  font-size: 14px;
}

.btn-danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-batch {
  padding: 8px 20px;
  background: rgba(255, 152, 0, 0.2);
  border: 1px solid rgba(255, 152, 0, 0.4);
  border-radius: 24px;
  color: #FF9800;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.btn-batch:hover {
  background: rgba(255, 152, 0, 0.4);
  transform: scale(1.02);
}

.btn-search {
  padding: 8px 20px;
  background: rgba(64, 224, 208, 0.2);
  border: 1px solid rgba(64, 224, 208, 0.4);
  border-radius: 24px;
  color: #40E0D0;
  cursor: pointer;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.filter-bar label {
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
}

.table-wrapper {
  overflow-x: auto;
}

.admin-table {
  width: 100%;
  border-collapse: collapse;
}

.admin-table th,
.admin-table td {
  padding: 12px 8px;
  text-align: left;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.85);
}

.admin-table th {
  color: #40E0D0;
  font-weight: 600;
  background: rgba(0, 0, 0, 0.2);
}

.title-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content-cell {
  max-width: 350px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-btns {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px 8px;
  border-radius: 8px;
  transition: all 0.2s;
  color: rgba(255, 255, 255, 0.7);
}

.btn-icon.edit:hover { background: rgba(64, 224, 208, 0.2); }
.btn-icon.delete:hover { background: rgba(229, 115, 115, 0.2); }
.btn-icon.approve:hover { background: rgba(124, 179, 66, 0.2); }
.btn-icon.reject:hover { background: rgba(229, 115, 115, 0.2); }
.btn-icon.view:hover { background: rgba(33, 150, 243, 0.2); }

.status-badge, .role-badge, .banned-badge, .normal-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  display: inline-block;
}

.status-published { background: rgba(124, 179, 66, 0.2); color: #7CB342; }
.status-draft { background: rgba(255, 152, 0, 0.2); color: #FF9800; }
.status-pending { background: rgba(255, 152, 0, 0.2); color: #FF9800; }
.status-approved { background: rgba(124, 179, 66, 0.2); color: #7CB342; }
.status-rejected { background: rgba(229, 115, 115, 0.2); color: #ff8888; }

.type-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  display: inline-block;
}

.type-info { background: rgba(33, 150, 243, 0.2); color: #42a5f5; }
.type-warning { background: rgba(255, 152, 0, 0.2); color: #FF9800; }
.type-success { background: rgba(124, 179, 66, 0.2); color: #7CB342; }

.pin-btn, .feature-btn, .sort-btn {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.2s;
}

.pin-btn:hover, .feature-btn:hover, .sort-btn:hover {
  background: rgba(64, 224, 208, 0.2);
  border-color: #40E0D0;
  transform: scale(1.02);
}

.pin-btn.active, .feature-btn.active {
  background: rgba(64, 224, 208, 0.2);
  border-color: #40E0D0;
  color: #40E0D0;
}

.post-badge {
  padding: 2px 8px;
  border-radius: 20px;
  font-size: 10px;
  font-weight: 500;
  display: inline-block;
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

.clickable {
  cursor: pointer;
  transition: opacity 0.2s;
}

.clickable:hover {
  opacity: 0.8;
  text-decoration: underline;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 20px;
}

.pagination button {
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 24px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.admin-textarea {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 20px;
  padding: 12px;
  color: white;
  font-size: 14px;
  outline: none;
  width: 100%;
  resize: vertical;
  font-family: inherit;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  color: rgba(255, 255, 255, 0.8);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 8px;
}

.modal-mask {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.post-detail-modal, .announcement-modal {
  max-width: 650px;
  width: 90%;
  max-height: 85vh;
  overflow-y: auto;
  background: rgba(25, 25, 40, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(64, 224, 208, 0.3);
  border-radius: 28px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.4);
}

.announcement-modal { max-width: 550px; }
.post-detail-modal { max-width: 600px; }

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  position: sticky;
  top: 0;
  background: rgba(25, 25, 40, 0.98);
  backdrop-filter: blur(20px);
  z-index: 10;
  border-radius: 28px 28px 0 0;
}

.modal-header h3 {
  margin: 0;
  color: white;
  font-size: 18px;
  font-weight: 600;
}

.close-btn {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 18px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.close-btn:hover {
  background: rgba(229, 115, 115, 0.3);
  color: #ff8888;
  transform: scale(1.05);
}

.modal-body { padding: 24px; }

.post-detail-body {
  max-height: calc(85vh - 120px);
  overflow-y: auto;
}

.detail-item {
  margin-bottom: 16px;
  display: flex;
  flex-wrap: wrap;
}

.detail-label {
  font-weight: 600;
  color: #40E0D0;
  width: 80px;
  flex-shrink: 0;
}

.detail-value {
  color: rgba(255, 255, 255, 0.85);
  flex: 1;
}

.detail-content {
  color: rgba(255, 255, 255, 0.85);
  flex: 1;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-img {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  object-fit: cover;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: transform 0.2s;
}

.detail-img:hover {
  transform: scale(1.05);
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  background: rgba(25, 25, 40, 0.95);
  position: sticky;
  bottom: 0;
  border-radius: 0 0 28px 28px;
}

@media (max-width: 768px) {
  .header-actions {
    flex-direction: column;
    align-items: stretch;
  }
  .search-box {
    flex-direction: column;
  }
  .post-detail-modal, .announcement-modal {
    width: 95%;
  }
  .detail-label {
    width: 100%;
    margin-bottom: 4px;
  }
  .sort-select {
    flex-wrap: wrap;
  }
  .custom-select-sort {
    width: 100%;
  }
}
</style>