
<template>
  <div class="admin-container">
    <div class="admin-header">
      <h2>🛠️ 管理后台</h2>
      <p>管理系统内容</p>
    </div>

    <div class="admin-tabs">
      <button v-for="tab in tabs" :key="tab.key" :class="{ active: currentTab === tab.key }" @click="switchTab(tab.key)">
        {{ tab.label }}
      </button>
    </div>

    <!-- 文章管理 -->
    <div v-if="currentTab === 'articles'" class="admin-panel glass-card">
      <div class="panel-header">
        <h3>📝 文章管理</h3>
        <button class="btn-primary" @click="openArticleModal">+ 新建文章</button>
      </div>
      <div class="search-box">
        <input type="text" v-model="articleSearch" placeholder="搜索文章标题..." class="admin-input" @keyup.enter="loadArticles" />
        <button @click="loadArticles" class="btn-search">搜索</button>

        <!-- 排序下拉框 -->
        <div class="custom-select-sort" ref="sortSelectRef">
          <div class="custom-select-trigger" @click.stop="toggleSortDropdown">
            <span>{{ getSortLabel() }}</span>
            <span class="arrow" :style="{ transform: showSortDropdown ? 'rotate(180deg)' : 'rotate(0deg)' }">▼</span>
          </div>
          <div v-if="showSortDropdown" class="custom-select-dropdown">
            <div class="custom-select-option" :class="{ active: articleSortBy === 'createTime_desc' }" @click.stop="selectSort('createTime_desc')">📅 最新优先</div>
            <div class="custom-select-option" :class="{ active: articleSortBy === 'createTime_asc' }" @click.stop="selectSort('createTime_asc')">📅 最早优先</div>
            <div class="custom-select-option" :class="{ active: articleSortBy === 'viewCount_desc' }" @click.stop="selectSort('viewCount_desc')">👁️ 浏览量最多</div>
            <div class="custom-select-option" :class="{ active: articleSortBy === 'viewCount_asc' }" @click.stop="selectSort('viewCount_asc')">👁️ 浏览量最少</div>
            <div class="custom-select-option" :class="{ active: articleSortBy === 'likeCount_desc' }" @click.stop="selectSort('likeCount_desc')">❤️ 点赞数最多</div>
            <div class="custom-select-option" :class="{ active: articleSortBy === 'likeCount_asc' }" @click.stop="selectSort('likeCount_asc')">❤️ 点赞数最少</div>
          </div>
        </div>
      </div>
      <div class="table-wrapper">
        <table class="admin-table">
          <thead>
          <tr>
            <th>ID</th>
            <th>标题</th>
            <th>分类</th>
            <th>浏览量</th>
            <th>点赞数</th>
            <th>状态</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="article in articles" :key="article.id">
            <td>{{ article.id }}</td>
            <td class="title-cell">{{ article.title }}</td>
            <td>{{ getCategoryName(article.category) }}</td>
            <td>{{ article.viewCount || 0 }}</td>
            <td>{{ article.likeCount || 0 }}</td>
            <td>
                <span :class="['status-badge', article.status === 1 ? 'status-published' : 'status-draft']">
                  {{ article.status === 1 ? '已发布' : '草稿' }}
                </span>
            </td>
            <td>{{ formatDate(article.createTime) }}</td>
            <td class="action-btns">
              <button class="btn-icon edit" @click="editArticle(article)" title="编辑">✏️</button>
              <button class="btn-icon delete" @click="confirmDelete(article.id)" title="删除">🗑️</button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination" v-if="articleTotal > 0">
        <button @click="articlePage--" :disabled="articlePage === 0">上一页</button>
        <span>第 {{ articlePage + 1 }} / {{ articleTotalPages }} 页</span>
        <button @click="articlePage++" :disabled="articlePage >= articleTotalPages - 1">下一页</button>
      </div>
    </div>

    <!-- 用户管理 -->
    <div v-if="currentTab === 'users'" class="admin-panel glass-card">
      <div class="panel-header"><h3>👥 用户管理</h3></div>
      <div class="search-box">
        <input type="text" v-model="userSearch" placeholder="搜索用户名..." class="admin-input" @keyup.enter="loadAllUsers" />
        <button @click="loadAllUsers" class="btn-search">搜索</button>
        <button @click="enterBatchMode" class="btn-batch">📋 批量处理</button>
      </div>
      <div class="table-wrapper">
        <table class="admin-table">
          <thead>
          <tr>
            <th v-if="isBatchMode" style="width: 40px">
              <input type="checkbox" @change="toggleSelectAll" :checked="isAllSelected" />
            </th>
            <th>ID</th>
            <th>用户名</th>
            <th>昵称</th>
            <th>角色</th>
            <th>状态</th>
            <th>禁言状态</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="user in allUsers" :key="user.id">
            <td v-if="isBatchMode" style="width: 40px">
              <input type="checkbox" v-model="selectedUsers" :value="user.id" />
            </td>
            <td>{{ user.id }}</td>
            <td>{{ user.username }}</td>
            <td class="nickname-cell">{{ user.nickname || '-' }}</td>
            <td class="role-cell">
              <span :class="['role-badge', user.role === 'ADMIN' ? 'role-admin' : 'role-user']">
                {{ user.role === 'ADMIN' ? '管理员' : '普通用户' }}
              </span>
            </td>
            <td class="status-cell">
              <span :class="['status-badge', user.status === 1 ? 'status-published' : 'status-draft']">
                {{ user.status === 1 ? '正常' : '禁用' }}
              </span>
            </td>
            <td class="ban-cell">
              <span v-if="user.isBanned === 1" class="banned-badge" :title="'禁言至 ' + formatDate(user.banEndTime)">🔇 禁言中</span>
              <span v-else class="normal-badge">正常</span>
            </td>
            <td class="date-cell">{{ formatDate(user.createTime) }}</td>
            <td class="action-btns">
              <button v-if="!isBatchMode && user.role !== 'ADMIN'" class="btn-icon toggle-status" @click="toggleUserStatus(user.id, user.status)" :title="user.status === 1 ? '禁用' : '启用'">
                {{ user.status === 1 ? '🔴' : '🟢' }}
              </button>
              <button v-if="!isBatchMode && user.role !== 'ADMIN' && user.isBanned !== 1" class="btn-icon ban" @click="openBanModal(user)" title="禁言">🔇</button>
              <button v-if="!isBatchMode && user.role !== 'ADMIN' && user.isBanned === 1" class="btn-icon unban" @click="unbanUser(user.id, user.username)" title="解除禁言">🔊</button>
              <button v-if="!isBatchMode && user.role !== 'ADMIN'" class="btn-icon delete" @click="deleteUser(user.id)" title="删除">🗑️</button>
            </td>
          </tr>
          </tbody>
        </table>
      </div>
      <div class="pagination" v-if="userTotal > 0">
        <button @click="userPage--" :disabled="userPage === 0">上一页</button>
        <span>第 {{ userPage + 1 }} / {{ userTotalPages }} 页</span>
        <button @click="userPage++" :disabled="userPage >= userTotalPages - 1">下一页</button>
      </div>
      <!-- 批量操作栏 -->
      <div v-if="isBatchMode" class="batch-actions-bar">
        <span class="selected-count">已选择 {{ selectedUsers.length }} 个用户</span>
        <button class="btn-batch-delete" @click="batchDelete">🗑️ 批量删除</button>
        <button class="btn-batch-ban" @click="batchBan">🔇 批量禁言</button>
        <button class="btn-batch-cancel" @click="exitBatchMode">取消</button>
      </div>
    </div>

    <!-- 库管理 -->
    <div v-if="currentTab === 'library'" class="admin-panel glass-card">
      <div class="panel-header">
        <h3>📦 运动/食物库管理</h3>
      </div>
      <div class="form-card">
        <h4>➕ 添加项目</h4>
        <div class="form-row">
          <div class="custom-select" style="width: 130px;" @click.stop="toggleLibraryTypeDropdown">
            <div class="custom-select-trigger">
              <span>{{ libraryForm.type === 'FOOD' ? '🍎 食物' : '🏃 运动' }}</span>
              <span class="arrow" :style="{ transform: showLibraryTypeDropdown ? 'rotate(180deg)' : 'rotate(0deg)' }">▼</span>
            </div>
            <div v-if="showLibraryTypeDropdown" class="custom-select-dropdown">
              <div class="custom-select-option" :class="{ active: libraryForm.type === 'FOOD' }" @click.stop="selectLibraryType('FOOD')">🍎 食物</div>
              <div class="custom-select-option" :class="{ active: libraryForm.type === 'SPORT' }" @click.stop="selectLibraryType('SPORT')">🏃 运动</div>
            </div>
          </div>
          <input v-model="libraryForm.name" placeholder="名称" class="admin-input" style="flex: 2;" />
          <input v-model="libraryForm.unit" placeholder="单位" class="admin-input" style="flex: 1;" />
          <input v-model="libraryForm.calories" placeholder="热量/消耗" type="number" class="admin-input" style="flex: 1;" />
          <button @click="addLibraryItem" class="btn-primary">添加</button>
        </div>
      </div>
      <div class="table-wrapper">
        <table class="admin-table">
          <thead>
          <tr>
            <th>ID</th>
            <th>类型</th>
            <th>名称</th>
            <th>单位</th>
            <th>热量/消耗</th>
            <th>操作</th>
          </tr>
          </thead>
          <tbody>
          <tr v-for="item in libraryItems" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.type === 'FOOD' ? '🍎 食物' : '🏃 运动' }}</td>
            <td>{{ item.name }}</td>
            <td>{{ item.unit || '-' }}</td>
            <td>{{ item.calories || 0 }}</td>
            <td class="action-btns"><button class="btn-icon delete" @click="deleteLibraryItem(item.id)" title="删除">🗑️</button></td>
          </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 数据统计 -->
    <div v-if="currentTab === 'stats'" class="admin-panel glass-card">
      <div class="panel-header">
        <h3>📊 数据统计</h3>
        <div class="date-range">
          <button :class="{ active: chartDays === 7 }" @click="setChartDays(7)">近7天</button>
          <button :class="{ active: chartDays === 30 }" @click="setChartDays(30)">近30天</button>
        </div>
      </div>

      <div class="dashboard-stats-grid">
        <div class="stat-card">
          <div class="stat-number">{{ dashboardStats.totalUsers || 0 }}</div>
          <div class="stat-label">总用户数</div>
          <div class="stat-trend">今日新增 +{{ dashboardStats.todayNewUsers || 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-number">{{ dashboardStats.totalPosts || 0 }}</div>
          <div class="stat-label">总帖子数</div>
          <div class="stat-trend">今日新增 +{{ dashboardStats.todayNewPosts || 0 }}</div>
        </div>
        <div class="stat-card">
          <div class="stat-number">{{ stats.totalArticles || 0 }}</div>
          <div class="stat-label">总文章数</div>
        </div>
        <div class="stat-card">
          <div class="stat-number">{{ stats.totalViews || 0 }}</div>
          <div class="stat-label">总浏览量</div>
        </div>
        <div class="stat-card">
          <div class="stat-number">{{ stats.totalLikes || 0 }}</div>
          <div class="stat-label">总点赞数</div>
        </div>
      </div>

      <div class="charts-row">
        <div class="chart-card">
          <h4>📈 用户注册趋势</h4>
          <div id="userTrendChart" class="chart-container"></div>
        </div>
        <div class="chart-card">
          <h4>📝 发帖趋势</h4>
          <div id="postTrendChart" class="chart-container"></div>
        </div>
      </div>

      <div class="charts-row">
        <div class="chart-card">
          <h4>👥 用户活跃度分布</h4>
          <div id="activityChart" class="chart-container-small"></div>
        </div>
        <div class="chart-card">
          <h4>📊 数据概览</h4>
          <div class="overview-stats">
            <div class="overview-item">
              <span class="overview-label">活跃用户(发帖≥5)</span>
              <span class="overview-value">{{ activityStats.activeUsers || 0 }}</span>
            </div>
            <div class="overview-item">
              <span class="overview-label">普通用户(发帖1-4)</span>
              <span class="overview-value">{{ activityStats.normalUsers || 0 }}</span>
            </div>
            <div class="overview-item">
              <span class="overview-label">待激活用户(发帖0)</span>
              <span class="overview-value">{{ activityStats.inactiveUsers || 0 }}</span>
            </div>
            <div class="overview-divider"></div>
            <div class="overview-item">
              <span class="overview-label">总用户数</span>
              <span class="overview-value">{{ activityStats.total || 0 }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 社区管理 -->
    <CommunityManagement
        v-if="currentTab === 'community'"
        :userId="userId"
        @view-profile="goToUserProfile"
        @view-post="goToPost"
    />

    <!-- 文章编辑弹窗 -->
    <Teleport to="body">
      <div v-if="showArticleModal" class="modal-mask" @click.self="closeArticleModal">
        <div class="modal-container article-modal">
          <div class="modal-header">
            <h3>{{ isEditing ? '编辑文章' : '新建文章' }}</h3>
            <button class="close-btn" @click="closeArticleModal">×</button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>📝 标题</label>
              <input v-model="articleForm.title" class="admin-input" placeholder="请输入文章标题" />
            </div>
            <div class="form-group">
              <label>📂 分类</label>
              <div class="custom-select" @click.stop="toggleCategoryDropdown">
                <div class="custom-select-trigger">
                  <span>{{ getCategoryDisplay(articleForm.category) }}</span>
                  <span class="arrow" :style="{ transform: showCategoryDropdown ? 'rotate(180deg)' : 'rotate(0deg)' }">▼</span>
                </div>
                <div v-if="showCategoryDropdown" class="custom-select-dropdown">
                  <div v-for="cat in categoryOptions" :key="cat.value" class="custom-select-option" :class="{ active: articleForm.category === cat.value }" @click.stop="selectCategory(cat.value)">{{ cat.label }}</div>
                </div>
              </div>
            </div>
            <div class="form-group">
              <label>📄 摘要</label>
              <textarea v-model="articleForm.summary" class="admin-textarea" rows="2" placeholder="请输入文章摘要"></textarea>
            </div>
            <div class="form-group">
              <label>📖 内容</label>
              <textarea v-model="articleForm.content" class="admin-textarea" rows="8" placeholder="请输入文章内容"></textarea>
            </div>
            <div class="form-group">
              <label>⚡ 状态</label>
              <div class="status-buttons">
                <button type="button" @click="articleForm.status = 1" :class="['status-option-btn', { active: articleForm.status === 1 }]">📢 发布</button>
                <button type="button" @click="articleForm.status = 0" :class="['status-option-btn', { active: articleForm.status === 0 }]">📝 存草稿</button>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="closeArticleModal">取消</button>
            <button class="btn-primary" @click="saveArticle">保存</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 禁言弹窗 -->
    <Teleport to="body">
      <div v-if="showBanModal" class="modal-mask" @click.self="closeBanModal">
        <div class="modal-container ban-modal">
          <div class="modal-header">
            <h3>🔇 禁言用户：{{ banTarget?.username }}</h3>
            <button class="close-btn" @click="closeBanModal">×</button>
          </div>
          <div class="modal-body">
            <div class="form-group">
              <label>禁言时长</label>
              <div class="custom-select" @click.stop="toggleBanDaysDropdown">
                <div class="custom-select-trigger">
                  <span>{{ getBanDaysDisplay() }}</span>
                  <span class="arrow" :style="{ transform: showBanDaysDropdown ? 'rotate(180deg)' : 'rotate(0deg)' }">▼</span>
                </div>
                <div v-if="showBanDaysDropdown" class="custom-select-dropdown">
                  <div class="custom-select-option" :class="{ active: banDays === '7' }" @click.stop="selectBanDays('7')">7天</div>
                  <div class="custom-select-option" :class="{ active: banDays === '30' }" @click.stop="selectBanDays('30')">30天</div>
                  <div class="custom-select-option" :class="{ active: banDays === '36500' }" @click.stop="selectBanDays('36500')">永久</div>
                </div>
              </div>
            </div>
            <div class="form-group">
              <label>禁言原因（可选）</label>
              <textarea v-model="banReason" class="admin-textarea" rows="3" placeholder="请填写禁言原因..."></textarea>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn-secondary" @click="closeBanModal">取消</button>
            <button class="btn-danger" @click="confirmBan">确认禁言</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'
import CommunityManagement from './CommunityManagement.vue'

const props = defineProps({
  userId: { type: Number, required: true }
})

const emit = defineEmits(['view-profile', 'view-post'])

const API_BASE = 'http://localhost:8080/api'

const currentTab = ref('articles')

const tabs = [
  { key: 'library', label: '库管理' },
  { key: 'articles', label: '文章管理' },
  { key: 'users', label: '用户管理' },
  { key: 'community', label: '社区管理' },
  { key: 'stats', label: '数据统计' }
]

// 图表相关
const chartDays = ref(7)
let userTrendChart = null
let postTrendChart = null
let activityChart = null
const dashboardStats = ref({ totalUsers: 0, totalPosts: 0, totalArticles: 0, todayNewUsers: 0, todayNewPosts: 0 })
const activityStats = ref({ activeUsers: 0, normalUsers: 0, inactiveUsers: 0, total: 0 })

// ========== 文章管理 ==========
const articles = ref([])
const articleSearch = ref('')
const articlePage = ref(0)
const articleTotal = ref(0)
const articleTotalPages = ref(0)
const pageSize = 10
const articleSortBy = ref('createTime_desc')
const showSortDropdown = ref(false)
const sortSelectRef = ref(null)

// 获取排序标签显示
const getSortLabel = () => {
  const sortMap = {
    'createTime_desc': '📅 最新优先',
    'createTime_asc': '📅 最早优先',
    'viewCount_desc': '👁️ 浏览量最多',
    'viewCount_asc': '👁️ 浏览量最少',
    'likeCount_desc': '❤️ 点赞数最多',
    'likeCount_asc': '❤️ 点赞数最少'
  }
  return sortMap[articleSortBy.value] || '📅 最新优先'
}

// 切换排序下拉框
const toggleSortDropdown = () => {
  showSortDropdown.value = !showSortDropdown.value
}

// 选择排序方式
const selectSort = (sort) => {
  articleSortBy.value = sort
  showSortDropdown.value = false
  articlePage.value = 0
  loadArticles()
}

const showArticleModal = ref(false)
const isEditing = ref(false)
const articleForm = ref({
  id: null,
  title: '',
  category: 'DIET',
  summary: '',
  content: '',
  status: 1
})

// 自定义分类下拉框
const showCategoryDropdown = ref(false)
const categoryOptions = [
  { value: 'DIET', label: '🥗 饮食健康' },
  { value: 'SPORT', label: '🏃 运动健身' },
  { value: 'SLEEP', label: '😴 优质睡眠' },
  { value: 'MENTAL', label: '🧠 心理健康' }
]

const getCategoryDisplay = (value) => {
  const option = categoryOptions.find(opt => opt.value === value)
  return option ? option.label : '选择分类'
}

const toggleCategoryDropdown = () => {
  showCategoryDropdown.value = !showCategoryDropdown.value
}

const selectCategory = (value) => {
  articleForm.value.category = value
  showCategoryDropdown.value = false
}

// 库管理类型下拉框
const showLibraryTypeDropdown = ref(false)

const toggleLibraryTypeDropdown = () => {
  showLibraryTypeDropdown.value = !showLibraryTypeDropdown.value
}

const selectLibraryType = (type) => {
  libraryForm.value.type = type
  showLibraryTypeDropdown.value = false
}

// 禁言时长下拉框
const showBanDaysDropdown = ref(false)

const toggleBanDaysDropdown = () => {
  showBanDaysDropdown.value = !showBanDaysDropdown.value
}

const getBanDaysDisplay = () => {
  if (banDays.value === '7') return '7天'
  if (banDays.value === '30') return '30天'
  if (banDays.value === '36500') return '永久'
  return '7天'
}

const selectBanDays = (days) => {
  banDays.value = days
  showBanDaysDropdown.value = false
}

const handleClickOutside = (event) => {
  if (!event.target.closest('.custom-select') && !event.target.closest('.custom-select-sort')) {
    showCategoryDropdown.value = false
    showLibraryTypeDropdown.value = false
    showBanDaysDropdown.value = false
    showSortDropdown.value = false
  }
}

// 加载文章（支持排序）
const loadArticles = async () => {
  try {
    const params = {
      page: articlePage.value,
      size: pageSize,
      sort: articleSortBy.value
    }
    if (articleSearch.value) params.keyword = articleSearch.value
    const res = await axios.get(`${API_BASE}/admin/articles`, { params })
    if (res.data.success) {
      articles.value = res.data.data || []
      articleTotal.value = res.data.total || 0
      articleTotalPages.value = res.data.totalPages || 0
    }
  } catch (e) {
    console.error('加载文章失败', e)
  }
}

const openArticleModal = () => {
  isEditing.value = false
  articleForm.value = { id: null, title: '', category: 'DIET', summary: '', content: '', status: 1 }
  showArticleModal.value = true
}

const editArticle = (article) => {
  isEditing.value = true
  articleForm.value = { ...article }
  showArticleModal.value = true
}

const saveArticle = async () => {
  if (!articleForm.value.title.trim()) { alert('请输入文章标题'); return }
  if (!articleForm.value.content.trim()) { alert('请输入文章内容'); return }
  try {
    if (isEditing.value) {
      await axios.put(`${API_BASE}/admin/article/${articleForm.value.id}`, articleForm.value)
      alert('文章更新成功')
    } else {
      await axios.post(`${API_BASE}/admin/article`, articleForm.value)
      alert('文章创建成功')
    }
    closeArticleModal()
    await loadArticles()
  } catch (e) {
    console.error('保存文章失败', e)
    alert('保存失败')
  }
}

const confirmDelete = async (id) => {
  if (!confirm('确定要删除这篇文章吗？')) return
  try {
    await axios.delete(`${API_BASE}/admin/article/${id}`)
    alert('删除成功')
    await loadArticles()
  } catch (e) { alert('删除失败') }
}

const closeArticleModal = () => {
  showArticleModal.value = false
  isEditing.value = false
}

// ========== 用户管理 ==========
const allUsers = ref([])
const userSearch = ref('')
const userPage = ref(0)
const userTotal = ref(0)
const userTotalPages = ref(0)
const userPageSize = 10

// 批量处理相关
const isBatchMode = ref(false)
const selectedUsers = ref([])

// 是否全选
const isAllSelected = computed(() => {
  return allUsers.value.length > 0 && selectedUsers.value.length === allUsers.value.length
})

// 进入批量模式
const enterBatchMode = () => {
  isBatchMode.value = true
  selectedUsers.value = []
}

// 退出批量模式
const exitBatchMode = () => {
  isBatchMode.value = false
  selectedUsers.value = []
}

// 全选/取消全选
const toggleSelectAll = () => {
  if (selectedUsers.value.length === allUsers.value.length) {
    selectedUsers.value = []
  } else {
    selectedUsers.value = allUsers.value.map(u => u.id)
  }
}

// 批量删除
const batchDelete = async () => {
  if (selectedUsers.value.length === 0) {
    alert('请至少选择一个用户')
    return
  }
  if (!confirm(`确定要删除选中的 ${selectedUsers.value.length} 个用户吗？此操作不可恢复！`)) return

  let successCount = 0
  let failCount = 0
  for (const userId of selectedUsers.value) {
    try {
      await axios.delete(`${API_BASE}/admin/user/${userId}`)
      successCount++
    } catch (e) {
      failCount++
    }
  }
  alert(`批量删除完成：成功 ${successCount} 个，失败 ${failCount} 个`)
  exitBatchMode()
  await loadAllUsers()
}

// 批量禁言
const batchBan = async () => {
  if (selectedUsers.value.length === 0) {
    alert('请至少选择一个用户')
    return
  }
  const days = prompt('请输入禁言天数（输入 36500 表示永久）', '7')
  if (days === null) return

  if (!confirm(`确定要禁言选中的 ${selectedUsers.value.length} 个用户，时长 ${days === '36500' ? '永久' : days + '天'} 吗？`)) return

  let successCount = 0
  let failCount = 0
  for (const userId of selectedUsers.value) {
    try {
      await axios.post(`${API_BASE}/admin/user/${userId}/ban`, null, {
        params: { days: parseInt(days), reason: '批量禁言', adminId: props.userId }
      })
      successCount++
    } catch (e) {
      failCount++
    }
  }
  alert(`批量禁言完成：成功 ${successCount} 个，失败 ${failCount} 个`)
  exitBatchMode()
  await loadAllUsers()
}

const loadAllUsers = async () => {
  try {
    const params = { page: userPage.value, size: userPageSize }
    if (userSearch.value) params.keyword = userSearch.value
    const res = await axios.get(`${API_BASE}/admin/users`, { params })
    if (res.data.success) {
      allUsers.value = res.data.data || []
      userTotal.value = res.data.total || 0
      userTotalPages.value = res.data.totalPages || 0
    } else {
      allUsers.value = res.data || []
    }
  } catch (e) {
    console.error('加载用户列表失败', e)
  }
}

const toggleUserStatus = async (userId, currentStatus) => {
  const newStatus = currentStatus === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  if (!confirm(`确定要${action}该用户吗？`)) return
  try {
    await axios.put(`${API_BASE}/admin/user/${userId}/status?status=${newStatus}`)
    alert(`${action}成功`)
    await loadAllUsers()
  } catch (e) {
    alert('操作失败')
  }
}

const deleteUser = async (userId) => {
  if (!confirm('确定要删除该用户吗？')) return
  try {
    await axios.delete(`${API_BASE}/admin/user/${userId}`)
    alert('删除成功')
    await loadAllUsers()
  } catch (e) { alert('删除失败') }
}

// ========== 禁言功能 ==========
const showBanModal = ref(false)
const banTarget = ref(null)
const banDays = ref('7')
const banReason = ref('')

const openBanModal = (user) => {
  banTarget.value = user
  banDays.value = '7'
  banReason.value = ''
  showBanModal.value = true
}

const closeBanModal = () => {
  showBanModal.value = false
  banTarget.value = null
}

const confirmBan = async () => {
  if (!banTarget.value) return
  try {
    await axios.post(`${API_BASE}/admin/user/${banTarget.value.id}/ban`, null, {
      params: { days: banDays.value, reason: banReason.value, adminId: props.userId }
    })
    alert(`用户 ${banTarget.value.username} 已被禁言 ${banDays.value === '36500' ? '永久' : banDays.value + '天'}`)
    closeBanModal()
    await loadAllUsers()
  } catch (e) {
    alert('禁言失败')
  }
}

const unbanUser = async (userId, username) => {
  if (!confirm(`确定要解除用户 ${username} 的禁言吗？`)) return
  try {
    await axios.post(`${API_BASE}/admin/user/${userId}/unban`)
    alert('已解除禁言')
    await loadAllUsers()
  } catch (e) {
    alert('解除失败')
  }
}

// ========== 库管理 ==========
const libraryItems = ref([])
const libraryForm = ref({ type: 'FOOD', name: '', unit: '', calories: '' })

const loadLibrary = async () => {
  try {
    const res = await axios.get(`${API_BASE}/admin/library`)
    libraryItems.value = res.data || []
  } catch (e) { console.error('加载库失败', e) }
}

const addLibraryItem = async () => {
  if (!libraryForm.value.name) { alert('请填写名称'); return }
  try {
    await axios.post(`${API_BASE}/admin/library/add`, null, { params: libraryForm.value })
    alert('添加成功')
    libraryForm.value = { type: 'FOOD', name: '', unit: '', calories: '' }
    await loadLibrary()
  } catch (e) { alert('添加失败') }
}

const deleteLibraryItem = async (itemId) => {
  if (!confirm('确定要删除吗？')) return
  try {
    await axios.delete(`${API_BASE}/admin/library/${itemId}`)
    alert('删除成功')
    await loadLibrary()
  } catch (e) { alert('删除失败') }
}

// ========== 数据统计 ==========
const stats = ref({ totalUsers: 0, totalArticles: 0, totalViews: 0, totalLikes: 0 })

const loadStats = async () => {
  try {
    const res = await axios.get(`${API_BASE}/admin/stats`)
    stats.value = res.data
  } catch (e) { console.error('加载统计数据失败', e) }
}

// 加载 Dashboard 统计数据
const loadDashboardStats = async () => {
  try {
    const res = await axios.get(`${API_BASE}/admin/statistics/dashboard`)
    if (res.data.success) {
      dashboardStats.value = res.data
    }
  } catch (e) {
    console.error('加载Dashboard统计失败', e)
  }
}

// 加载用户趋势数据
const loadUserTrend = async () => {
  try {
    const res = await axios.get(`${API_BASE}/admin/statistics/user-trend`, {
      params: { days: chartDays.value }
    })
    if (res.data.success && userTrendChart) {
      userTrendChart.setOption({
        xAxis: { data: res.data.dates },
        series: [{ data: res.data.counts }]
      })
    }
  } catch (e) {
    console.error('加载用户趋势失败', e)
  }
}

// 加载发帖趋势数据
const loadPostTrend = async () => {
  try {
    const res = await axios.get(`${API_BASE}/admin/statistics/post-trend`, {
      params: { days: chartDays.value }
    })
    if (res.data.success && postTrendChart) {
      postTrendChart.setOption({
        xAxis: { data: res.data.dates },
        series: [{ data: res.data.counts }]
      })
    }
  } catch (e) {
    console.error('加载发帖趋势失败', e)
  }
}

// 加载用户活跃度数据
const loadActivityStats = async () => {
  try {
    const res = await axios.get(`${API_BASE}/admin/statistics/user-activity`)
    if (res.data.success) {
      activityStats.value = res.data
      if (activityChart) {
        activityChart.setOption({
          series: [{
            data: [
              { value: res.data.activeUsers, name: '活跃用户 (≥5帖)' },
              { value: res.data.normalUsers, name: '普通用户 (1-4帖)' },
              { value: res.data.inactiveUsers, name: '待激活用户 (0帖)' }
            ]
          }]
        })
      }
    }
  } catch (e) {
    console.error('加载活跃度统计失败', e)
  }
}

// 初始化所有图表
const initCharts = () => {
  const userTrendDom = document.getElementById('userTrendChart')
  if (userTrendDom) {
    userTrendChart = echarts.init(userTrendDom)
    userTrendChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', name: '日期' },
      yAxis: { type: 'value', name: '新增用户数' },
      series: [{
        name: '新增用户',
        type: 'line',
        smooth: true,
        data: [],
        lineStyle: { color: '#40E0D0', width: 2 },
        areaStyle: { color: 'rgba(64, 224, 208, 0.2)' },
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { color: '#40E0D0' }
      }]
    })
  }

  const postTrendDom = document.getElementById('postTrendChart')
  if (postTrendDom) {
    postTrendChart = echarts.init(postTrendDom)
    postTrendChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', name: '日期' },
      yAxis: { type: 'value', name: '新增帖子数' },
      series: [{
        name: '新增帖子',
        type: 'line',
        smooth: true,
        data: [],
        lineStyle: { color: '#7CB342', width: 2 },
        areaStyle: { color: 'rgba(124, 179, 66, 0.2)' },
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { color: '#7CB342' }
      }]
    })
  }

  const activityDom = document.getElementById('activityChart')
  if (activityDom) {
    activityChart = echarts.init(activityDom)
    activityChart.setOption({
      tooltip: { trigger: 'item' },
      legend: {
        orient: 'vertical',
        left: 'left',
        textStyle: { color: 'rgba(255, 255, 255, 0.8)' }
      },
      series: [{
        name: '用户活跃度',
        type: 'pie',
        radius: '55%',
        center: ['50%', '50%'],
        data: [],
        itemStyle: {
          borderRadius: 8,
          borderColor: 'rgba(0,0,0,0.3)',
          borderWidth: 2
        },
        label: { color: 'rgba(255, 255, 255, 0.9)' }
      }],
      color: ['#40E0D0', '#7CB342', '#FF9800']
    })
  }
}

const setChartDays = (days) => {
  chartDays.value = days
  loadUserTrend()
  loadPostTrend()
}

const refreshStats = async () => {
  await loadDashboardStats()
  await loadUserTrend()
  await loadPostTrend()
  await loadActivityStats()
}

const handleResize = () => {
  if (userTrendChart) userTrendChart.resize()
  if (postTrendChart) postTrendChart.resize()
  if (activityChart) activityChart.resize()
}

// ========== 通用方法 ==========
const switchTab = (tab) => {
  currentTab.value = tab
  if (tab === 'articles') { articlePage.value = 0; loadArticles() }
  else if (tab === 'users') { userPage.value = 0; loadAllUsers() }
  else if (tab === 'library') loadLibrary()
  else if (tab === 'stats') {
    loadStats()
    nextTick(() => {
      initCharts()
      refreshStats()
    })
  }
}

const getCategoryName = (category) => {
  const map = { 'DIET': '饮食健康', 'SPORT': '运动健身', 'SLEEP': '优质睡眠', 'MENTAL': '心理健康' }
  return map[category] || category
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
}

const goToUserProfile = (targetUserId) => {
  emit('view-profile', targetUserId)
}

const goToPost = (postId) => {
  emit('view-post', postId)
}

watch(articlePage, () => loadArticles())
watch(userPage, () => loadAllUsers())

onMounted(() => {
  loadArticles()
  loadAllUsers()
  loadLibrary()
  loadStats()
  document.addEventListener('click', handleClickOutside)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('resize', handleResize)
  if (userTrendChart) userTrendChart.dispose()
  if (postTrendChart) postTrendChart.dispose()
  if (activityChart) activityChart.dispose()
})
</script>

<style scoped>
.admin-container {
  max-width: 1200px;
  margin: 0 auto;
}

.admin-header {
  text-align: center;
  margin-bottom: 24px;
}

.admin-header h2 {
  color: white;
  font-size: 24px;
  margin-bottom: 8px;
}

.admin-header p {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

.admin-tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
  justify-content: center;
}

.admin-tabs button {
  padding: 10px 24px;
  background: rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  transition: all 0.2s;
}

.admin-tabs button.active {
  background: rgba(64, 224, 208, 0.2);
  border-color: #40E0D0;
  color: #40E0D0;
}

.glass-card {
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 24px;
  padding: 20px;
}

.admin-panel {
  margin-top: 20px;
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

.date-range {
  display: flex;
  gap: 8px;
}

.date-range button {
  padding: 6px 16px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  transition: all 0.2s;
}

.date-range button.active {
  background: rgba(64, 224, 208, 0.25);
  border-color: #40E0D0;
  color: #40E0D0;
}

.dashboard-stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  text-align: center;
  padding: 20px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 20px;
  transition: transform 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #40E0D0;
}

.stat-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  margin-top: 6px;
}

.stat-trend {
  font-size: 11px;
  color: #7CB342;
  margin-top: 8px;
}

.charts-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 20px;
  padding: 16px;
}

.chart-card h4 {
  color: white;
  font-size: 15px;
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.chart-container {
  width: 100%;
  height: 300px;
}

.chart-container-small {
  width: 100%;
  height: 280px;
}

.overview-stats {
  padding: 16px;
}

.overview-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.overview-label {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
}

.overview-value {
  font-size: 20px;
  font-weight: bold;
  color: #40E0D0;
}

.overview-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.15);
  margin: 12px 0;
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

.btn-danger:hover {
  background: rgba(229, 115, 115, 0.4);
}

.btn-search {
  padding: 8px 20px;
  background: rgba(64, 224, 208, 0.2);
  border: 1px solid rgba(64, 224, 208, 0.4);
  border-radius: 24px;
  color: #40E0D0;
  cursor: pointer;
}

.btn-batch {
  padding: 10px 24px;
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

.admin-input::placeholder {
  color: rgba(255, 255, 255, 0.4);
}

/* 排序下拉框样式 - 与系统风格一致 */
.custom-select-sort {
  position: relative;
  width: 160px;
}

.custom-select-sort .custom-select-trigger {
  background: rgba(30, 30, 50, 0.85);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(64, 224, 208, 0.5);
  border-radius: 24px;
  padding: 10px 40px 10px 20px;
  color: #40E0D0;
  font-size: 14px;
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
  padding: 12px 20px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
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

.custom-select {
  position: relative;
  width: 100%;
}

.custom-select-trigger {
  background: rgba(30, 30, 50, 0.85);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(64, 224, 208, 0.5);
  border-radius: 24px;
  padding: 10px 40px 10px 20px;
  color: #40E0D0;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s;
}

.custom-select-trigger:hover {
  background: rgba(40, 40, 60, 0.9);
  border-color: #40E0D0;
}

.custom-select-trigger .arrow {
  font-size: 10px;
  transition: transform 0.2s;
  color: #40E0D0;
}

.custom-select-dropdown {
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

.custom-select-option {
  padding: 12px 20px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.custom-select-option:hover {
  background: rgba(64, 224, 208, 0.2);
  color: #40E0D0;
}

.custom-select-option.active {
  background: rgba(64, 224, 208, 0.15);
  color: #40E0D0;
}

.form-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
  background: rgba(0, 0, 0, 0.2);
  border-radius: 20px;
  padding: 16px;
}

.form-row .admin-input {
  flex: 1;
  min-width: 100px;
}

.form-row .custom-select {
  flex: 0 0 auto;
  width: 130px;
}

.form-row .btn-primary {
  flex: 0 0 auto;
}

.form-card {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 20px;
  padding: 16px;
  margin-bottom: 20px;
}

.form-card h4 {
  color: white;
  margin-bottom: 12px;
  font-size: 16px;
}

.status-buttons {
  display: flex;
  gap: 12px;
}

.status-option-btn {
  flex: 1;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 24px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.status-option-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.status-option-btn.active {
  background: rgba(64, 224, 208, 0.25);
  border-color: #40E0D0;
  color: #40E0D0;
}

.status-option-btn:last-child.active {
  background: rgba(255, 152, 0, 0.2);
  border-color: #FF9800;
  color: #FF9800;
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

.admin-textarea:focus {
  border-color: rgba(64, 224, 208, 0.5);
  background: rgba(255, 255, 255, 0.12);
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
/* 解决用户管理表格操作列横线对不齐问题 */
.admin-table th:last-child,
.admin-table td:last-child {
  min-width: 160px;
  white-space: nowrap;
}

.title-cell {
  max-width: 300px;
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

.role-admin { background: rgba(64, 224, 208, 0.2); color: #40E0D0; }
.role-user { background: rgba(255, 255, 255, 0.1); color: rgba(255, 255, 255, 0.7); }
.banned-badge { background: rgba(229, 115, 115, 0.2); color: #ff8888; }
.normal-badge { background: rgba(124, 179, 66, 0.2); color: #7CB342; }

.type-badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  display: inline-block;
}

.type-info { background: rgba(33, 150, 243, 0.2); color: #42a5f5; }
.type-warning { background: rgba(255, 152, 0, 0.2); color: #FF9800; }
.type-success { background: rgba(124, 179, 66, 0.2); color: #7CB342; }

.pin-btn, .feature-btn {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.2s;
}

.pin-btn.active, .feature-btn.active {
  background: rgba(64, 224, 208, 0.2);
  border-color: #40E0D0;
  color: #40E0D0;
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

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.admin-select {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 24px;
  padding: 8px 16px;
  color: white;
  font-size: 14px;
  outline: none;
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

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
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

.edit-input, .edit-select {
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(64, 224, 208, 0.4);
  border-radius: 20px;
  padding: 4px 8px;
  color: white;
  font-size: 13px;
  outline: none;
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

.article-modal, .ban-modal {
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

.ban-modal { max-width: 450px; }

.article-modal::-webkit-scrollbar, .ban-modal::-webkit-scrollbar { width: 6px; }
.article-modal::-webkit-scrollbar-track, .ban-modal::-webkit-scrollbar-track { background: rgba(255, 255, 255, 0.05); border-radius: 3px; }
.article-modal::-webkit-scrollbar-thumb, .ban-modal::-webkit-scrollbar-thumb { background: rgba(64, 224, 208, 0.4); border-radius: 3px; }

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

/* 批量操作栏样式 */
.batch-actions-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: flex-end;
  margin-top: 16px;
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 24px;
}

.selected-count {
  color: #40E0D0;
  font-size: 14px;
  flex: 1;
}

.btn-batch-delete {
  padding: 8px 20px;
  background: rgba(229, 115, 115, 0.2);
  border: 1px solid rgba(229, 115, 115, 0.4);
  border-radius: 24px;
  color: #ff8888;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-batch-delete:hover {
  background: rgba(229, 115, 115, 0.4);
}

.btn-batch-ban {
  padding: 8px 20px;
  background: rgba(255, 152, 0, 0.2);
  border: 1px solid rgba(255, 152, 0, 0.4);
  border-radius: 24px;
  color: #FF9800;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-batch-ban:hover {
  background: rgba(255, 152, 0, 0.4);
}

.btn-batch-cancel {
  padding: 8px 20px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 24px;
  color: rgba(255, 255, 255, 0.8);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-batch-cancel:hover {
  background: rgba(255, 255, 255, 0.2);
}

@media (max-width: 768px) {
  .stats-grid,
  .dashboard-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .charts-row {
    grid-template-columns: 1fr;
  }
  .form-row {
    flex-direction: column;
    align-items: stretch;
  }
  .form-row .admin-input, .form-row .custom-select, .form-row .btn-primary {
    width: 100%;
    flex: auto;
  }
  .admin-tabs button {
    padding: 6px 16px;
    font-size: 12px;
  }
  .search-box {
    flex-direction: column;
  }
  .article-modal, .ban-modal {
    width: 95%;
  }
  .status-buttons {
    flex-direction: column;
  }
  .batch-actions-bar {
    flex-wrap: wrap;
    justify-content: center;
  }
  .custom-select-sort {
    width: 100%;
  }
}
</style>
