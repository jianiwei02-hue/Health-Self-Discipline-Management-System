<template>
  <div class="article-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>健康资讯</h1>
      <p>为您精选的健康知识，科学养生每一天</p>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar glass-card">
      <div class="search-wrapper">
        <span class="search-icon">🔍</span>
        <input
            type="text"
            v-model="searchKeyword"
            placeholder="搜索文章..."
            class="search-input"
            @keyup.enter="handleSearch"
        />
        <button v-if="searchKeyword" @click="clearSearch" class="clear-btn">✕</button>
      </div>
      <button @click="handleSearch" class="search-btn">搜索</button>
    </div>

    <!-- 分类标签 -->
    <div class="category-tabs">
      <button
          v-for="cat in categories"
          :key="cat.value"
          :class="{active: currentCategory === cat.value}"
          @click="switchCategory(cat.value)"
      >
        <span class="cat-icon">{{ cat.icon }}</span>
        <span>{{ cat.name }}</span>
      </button>
    </div>

    <!-- 文章列表 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else-if="articles.length === 0" class="empty-state glass-card">
      <span>📚</span>
      <p>暂无文章</p>
      <p class="empty-hint">换个分类试试吧~</p>
    </div>

    <div v-else class="article-grid">
      <div
          v-for="article in articles"
          :key="article.id"
          class="article-card glass-card"
          @click="viewArticle(article.id)"
      >
        <div class="article-cover">
          <span class="article-emoji">{{ getCategoryEmoji(article.category) }}</span>
        </div>
        <div class="article-content">
          <div class="article-header">
            <span class="category-tag" :class="getCategoryClass(article.category)">
              {{ getCategoryName(article.category) }}
            </span>
            <span class="article-time">{{ formatTime(article.createTime) }}</span>
          </div>
          <h4 class="article-title">{{ article.title }}</h4>
          <p class="article-summary">{{ article.summary || (article.content ? article.content.substring(0, 80) : '') }}...</p>
          <div class="article-footer">
            <span>👁️ {{ formatNumber(article.viewCount || 0) }}</span>
            <span>❤️ {{ formatNumber(article.likeCount || 0) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore && !loading" class="load-more" @click="loadMore">
      加载更多
    </div>
    <div v-if="loadingMore" class="loading-more">
      <div class="loading-spinner small"></div>
      <span>加载中...</span>
    </div>

    <!-- 文章详情 - 阅读模式 -->
    <Teleport to="body">
      <div v-if="showDetail" class="reader-modal">
        <div class="reader-modal-content">
          <!-- 进度条 -->
          <div class="reader-progress">
            <div class="reader-progress-fill" :style="{ width: readProgress + '%' }"></div>
          </div>

          <!-- 滚动内容区 -->
          <div class="reader-scroll-area" ref="modalBody" @scroll="updateReadProgress">
            <div class="reader-inner">
              <div class="article-wrapper">
                <!-- 顶部标题区 -->
                <div class="article-header-info">
                  <span class="category-badge" :class="getCategoryClass(currentArticle.category)">
                    {{ getCategoryName(currentArticle.category) }}
                  </span>
                  <h1 class="article-title-full">{{ currentArticle.title }}</h1>
                  <div class="article-meta-info">
                    <span>📅 {{ formatFullTime(currentArticle.createTime) }}</span>
                    <span>⏱️ {{ readTime }} 分钟阅读</span>
                    <span>👁️ {{ formatNumber(currentArticle.viewCount || 0) }} 阅读</span>
                  </div>
                </div>

                <!-- 正文区 -->
                <div class="article-body" :style="{ fontSize: fontSize + 'px' }" v-html="formattedContent"></div>

                <!-- 底部操作区 -->
                <div class="action-section">
                  <div class="action-group">
                    <button @click="toggleCollect" class="action-circle" :class="{ active: isCollected }">
                      {{ isCollected ? '⭐' : '☆' }}
                    </button>
                    <button @click="likeCurrentArticle" class="action-circle like">
                      ❤️ {{ formatNumber(currentArticle.likeCount || 0) }}
                    </button>
                    <button @click="shareArticle" class="action-circle">
                      🔗
                    </button>
                  </div>
                </div>

                <!-- 相关推荐 -->
                <div v-if="relatedArticles.length > 0" class="related-section">
                  <div class="related-header">
                    <span class="related-line"></span>
                    <span class="related-title">相关推荐</span>
                    <span class="related-line"></span>
                  </div>
                  <div class="related-cards">
                    <div
                        v-for="related in relatedArticles"
                        :key="related.id"
                        class="related-card"
                        @click="viewArticle(related.id)"
                    >
                      <div class="related-card-emoji">{{ getCategoryEmoji(related.category) }}</div>
                      <div class="related-card-info">
                        <div class="related-card-title">{{ related.title }}</div>
                        <div class="related-card-summary">{{ related.summary || '点击阅读更多内容...' }}</div>
                        <span class="related-card-tag">{{ getCategoryName(related.category) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 顶部工具栏 -->
          <div class="reader-toolbar">
            <div class="font-controls">
              <button @click="decreaseFontSize" class="font-btn">A-</button>
              <span class="font-size-num">{{ fontSize }}</span>
              <button @click="increaseFontSize" class="font-btn">A+</button>
            </div>
            <button @click="closeDetail" class="close-btn">✕</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import axios from 'axios'

const props = defineProps({
  userId: { type: Number, default: null }
})

const API_BASE = 'http://localhost:8080/api'

const categories = [
  { name: '全部', value: '', icon: '📚' },
  { name: '饮食健康', value: 'DIET', icon: '🥗' },
  { name: '运动健身', value: 'SPORT', icon: '🏃' },
  { name: '优质睡眠', value: 'SLEEP', icon: '😴' },
  { name: '心理健康', value: 'MENTAL', icon: '🧠' }
]

const articles = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const currentCategory = ref('')
const searchKeyword = ref('')
const showDetail = ref(false)
const currentArticle = ref({})
const currentPage = ref(0)
const hasMore = ref(true)
const pageSize = 10

const fontSize = ref(17)
const increaseFontSize = () => {
  if (fontSize.value < 24) fontSize.value += 2
}
const decreaseFontSize = () => {
  if (fontSize.value > 14) fontSize.value -= 2
}

const isCollected = ref(false)
const toggleCollect = async () => {
  if (!props.userId) {
    alert('请先登录')
    return
  }
  try {
    if (isCollected.value) {
      await axios.delete(`${API_BASE}/health-article/collect/${currentArticle.value.id}`, {
        params: { userId: props.userId }
      })
      isCollected.value = false
    } else {
      await axios.post(`${API_BASE}/health-article/collect/${currentArticle.value.id}`, null, {
        params: { userId: props.userId }
      })
      isCollected.value = true
    }
  } catch (e) {
    console.error('收藏操作失败', e)
    isCollected.value = !isCollected.value
  }
}

const modalBody = ref(null)
const readProgress = ref(0)
const updateReadProgress = () => {
  if (!modalBody.value) return
  const scrollTop = modalBody.value.scrollTop
  const scrollHeight = modalBody.value.scrollHeight - modalBody.value.clientHeight
  if (scrollHeight > 0) {
    readProgress.value = (scrollTop / scrollHeight) * 100
  }
}

const readTime = computed(() => {
  if (!currentArticle.value.content) return 1
  return Math.max(1, Math.ceil(currentArticle.value.content.length / 450))
})

const relatedArticles = ref([])
const loadRelatedArticles = async () => {
  if (!currentArticle.value.category) return
  try {
    const res = await axios.get(`${API_BASE}/health-article/related/${currentArticle.value.id}`, {
      params: { category: currentArticle.value.category, limit: 3 }
    })
    const data = res.data.data || res.data.articles || res.data
    relatedArticles.value = Array.isArray(data) ? data.slice(0, 3) : []
  } catch (e) {
    console.error('加载相关文章失败', e)
    relatedArticles.value = articles.value.filter(a =>
        a.id !== currentArticle.value.id && a.category === currentArticle.value.category
    ).slice(0, 3)
  }
}

const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  return num.toString()
}

const getCategoryEmoji = (category) => {
  const map = { 'DIET': '🥗', 'SPORT': '🏃', 'SLEEP': '😴', 'MENTAL': '🧠' }
  return map[category] || '📖'
}

let scrollTimeout = null

const formattedContent = computed(() => {
  if (!currentArticle.value.content) return ''
  let content = currentArticle.value.content

  content = content.replace(/\n/g, '<br/>')
  content = content.replace(/【(.+?)】/g, '<h2 class="section-heading">📌 $1</h2>')
  content = content.replace(/^(\d+)\./gm, '<span class="num-badge">$1</span>')
  content = content.replace(/记住：(.*?)(?=<br\/>|$)/g, '<div class="quote-block">💡 记住：$1</div>')

  return content
})
const switchCategory = (category) => {
  currentCategory.value = category
  searchKeyword.value = ''
  currentPage.value = 0
  hasMore.value = true
  articles.value = []
  loadArticles()
}

const handleSearch = () => {
  currentPage.value = 0
  hasMore.value = true
  articles.value = []
  loadArticles()
}

const clearSearch = () => {
  searchKeyword.value = ''
  handleSearch()
}

const loadArticles = async () => {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize }
    if (currentCategory.value) params.category = currentCategory.value
    if (searchKeyword.value) params.keyword = searchKeyword.value
    const res = await axios.get(`${API_BASE}/health-article/list`, { params })

    let newArticles = []
    if (res.data && res.data.data) {
      newArticles = res.data.data
      hasMore.value = res.data.total > (currentPage.value + 1) * pageSize
    } else if (Array.isArray(res.data)) {
      newArticles = res.data
      hasMore.value = newArticles.length === pageSize
    } else if (res.data && res.data.articles) {
      newArticles = res.data.articles
      hasMore.value = newArticles.length === pageSize
    }

    if (currentPage.value === 0) {
      articles.value = newArticles
    } else {
      articles.value = [...articles.value, ...newArticles]
    }
  } catch (e) {
    console.error('加载文章失败', e)
    loadMockData()
  } finally {
    loading.value = false
  }
}

const loadMockData = () => {
  const mockArticles = [
    { id: 1, title: '每天喝8杯水，真的有必要吗？', summary: '揭秘饮水真相，科学补水有讲究', content: '很多人听说过每天要喝8杯水的说法，但这个标准并非适用于所有人。\n\n实际上，每个人的需水量取决于体重、活动量、气候等因素。一般建议成年人每天饮水1.5-2升。\n\n【科学补水建议】\n1. 不要等到口渴才喝水\n2. 少量多次，每次100-200ml\n3. 晨起一杯水，唤醒身体\n4. 运动前后及时补充水分\n\n记住：尿液颜色是判断是否缺水的好指标。', category: 'DIET', viewCount: 1246, likeCount: 89, createTime: '2026-04-29' },
    { id: 2, title: '跑步前后的拉伸指南', summary: '正确拉伸，远离运动损伤', content: '跑步前后的拉伸非常重要，可以有效预防运动损伤...', category: 'SPORT', viewCount: 2345, likeCount: 156, createTime: '2026-04-12' },
    { id: 3, title: '为什么你需要7-8小时睡眠', summary: '睡眠是健康的基石', content: '充足的睡眠对身体健康至关重要...', category: 'SLEEP', viewCount: 3456, likeCount: 234, createTime: '2026-04-12' }
  ]
  articles.value = mockArticles.filter(a => {
    if (currentCategory.value && a.category !== currentCategory.value) return false
    if (searchKeyword.value && !a.title.includes(searchKeyword.value) && !a.content.includes(searchKeyword.value)) return false
    return true
  })
  hasMore.value = false
  loading.value = false
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  loadingMore.value = true
  currentPage.value++
  try {
    const params = { page: currentPage.value, size: pageSize }
    if (currentCategory.value) params.category = currentCategory.value
    if (searchKeyword.value) params.keyword = searchKeyword.value
    const res = await axios.get(`${API_BASE}/health-article/list`, { params })

    let newArticles = []
    if (res.data && res.data.data) {
      newArticles = res.data.data
      hasMore.value = res.data.total > (currentPage.value + 1) * pageSize
    } else if (Array.isArray(res.data)) {
      newArticles = res.data
      hasMore.value = newArticles.length === pageSize
    } else if (res.data && res.data.articles) {
      newArticles = res.data.articles
      hasMore.value = newArticles.length === pageSize
    }
    articles.value = [...articles.value, ...newArticles]
  } catch (e) {
    console.error('加载更多失败', e)
  } finally {
    loadingMore.value = false
  }
}

const viewArticle = async (id) => {
  // 禁用页面滚动
  document.documentElement.style.overflow = 'hidden'
  document.body.style.overflow = 'hidden'

  try {
    const res = await axios.get(`${API_BASE}/health-article/detail/${id}`)
    const articleData = res.data.article || res.data.data || res.data
    currentArticle.value = articleData
    showDetail.value = true
    fontSize.value = 17
    readProgress.value = 0
    relatedArticles.value = []
    setTimeout(() => loadRelatedArticles(), 100)

    if (props.userId) {
      try {
        const collectRes = await axios.get(`${API_BASE}/health-article/collect-status/${id}`, {
          params: { userId: props.userId }
        })
        isCollected.value = collectRes.data.isCollected || false
      } catch {
        isCollected.value = false
      }
    }

    const article = articles.value.find(a => a.id === id)
    if (article) article.viewCount = (article.viewCount || 0) + 1
  } catch (e) {
    console.error('加载文章详情失败', e)
    const mockArticle = articles.value.find(a => a.id === id)
    if (mockArticle) {
      currentArticle.value = { ...mockArticle }
      showDetail.value = true
    }
  }
}

const likeCurrentArticle = async () => {
  try {
    const res = await axios.post(`${API_BASE}/health-article/like/${currentArticle.value.id}`)
    const newLikeCount = res.data.likeCount || (currentArticle.value.likeCount + 1)
    currentArticle.value.likeCount = newLikeCount
    const article = articles.value.find(a => a.id === currentArticle.value.id)
    if (article) article.likeCount = newLikeCount
  } catch (e) {
    console.error('点赞失败', e)
    currentArticle.value.likeCount = (currentArticle.value.likeCount || 0) + 1
  }
}

const shareArticle = () => {
  const url = `${window.location.origin}/article/${currentArticle.value.id}`
  navigator.clipboard.writeText(url)
  alert('链接已复制到剪贴板！')
}

const closeDetail = () => {
  // 恢复页面滚动
  document.documentElement.style.overflow = ''
  document.body.style.overflow = ''

  showDetail.value = false
  currentArticle.value = {}
  relatedArticles.value = []
  isCollected.value = false
}

const getCategoryName = (category) => {
  const map = { 'DIET': '饮食', 'SPORT': '运动', 'SLEEP': '睡眠', 'MENTAL': '心理' }
  return map[category] || category
}

const getCategoryClass = (category) => {
  const map = { 'DIET': 'cat-diet', 'SPORT': 'cat-sport', 'SLEEP': 'cat-sleep', 'MENTAL': 'cat-mental' }
  return map[category] || ''
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const formatFullTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
}

const handleScroll = () => {
  if (scrollTimeout) clearTimeout(scrollTimeout)
  scrollTimeout = setTimeout(() => {
    const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
    const scrollHeight = document.documentElement.scrollHeight || document.body.scrollHeight
    const clientHeight = document.documentElement.clientHeight
    if (scrollTop + clientHeight >= scrollHeight - 100) {
      if (hasMore.value && !loading.value && !loadingMore.value) loadMore()
    }
  }, 100)
}

onMounted(() => {
  loadArticles()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  if (scrollTimeout) clearTimeout(scrollTimeout)
})
</script>

<style scoped>
/* ========== 列表页样式 ========== */
.article-container {
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
  font-weight: 600;
  margin-bottom: 8px;
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
  transition: all 0.2s;
}

.glass-card:hover {
  background: rgba(0, 0, 0, 0.35);
}

.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 16px 24px;
}

.search-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 40px;
  padding: 0 16px;
  transition: all 0.2s;
}

.search-wrapper:focus-within {
  border-color: #40E0D0;
  background: rgba(255, 255, 255, 0.12);
}

.search-icon {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.5);
  margin-right: 8px;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  padding: 12px 0;
  color: white;
  font-size: 14px;
  outline: none;
}

.search-input::placeholder {
  color: rgba(255, 255, 255, 0.5);
}

.clear-btn {
  background: rgba(255, 255, 255, 0.1);
  border: none;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-btn {
  padding: 10px 28px;
  background: rgba(64, 224, 208, 0.2);
  border: 1px solid rgba(64, 224, 208, 0.4);
  border-radius: 40px;
  color: #40E0D0;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  white-space: nowrap;
}

.search-btn:hover {
  background: rgba(64, 224, 208, 0.35);
}

.category-tabs {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
}

.category-tabs button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 40px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  transition: all 0.2s;
}

.category-tabs button:hover {
  background: rgba(64, 224, 208, 0.1);
  border-color: rgba(64, 224, 208, 0.4);
}

.category-tabs button.active {
  background: rgba(64, 224, 208, 0.2);
  border-color: #40E0D0;
  color: #40E0D0;
}

.cat-icon {
  font-size: 16px;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 24px;
}

.article-card {
  display: flex;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 0;
}

.article-card:hover {
  transform: translateY(-4px);
  background: rgba(0, 0, 0, 0.4);
  border-color: rgba(64, 224, 208, 0.3);
}

.article-cover {
  width: 100px;
  background: linear-gradient(135deg, rgba(64, 224, 208, 0.1), rgba(64, 224, 208, 0.05));
  display: flex;
  align-items: center;
  justify-content: center;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
}

.article-emoji {
  font-size: 42px;
}

.article-content {
  flex: 1;
  padding: 16px 20px;
}

.article-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.category-tag {
  font-size: 11px;
  padding: 4px 12px;
  border-radius: 20px;
  font-weight: 500;
}

.cat-diet { background: rgba(124, 179, 66, 0.2); color: #7CB342; }
.cat-sport { background: rgba(64, 224, 208, 0.2); color: #40E0D0; }
.cat-sleep { background: rgba(156, 39, 176, 0.2); color: #9C27B0; }
.cat-mental { background: rgba(255, 152, 0, 0.2); color: #FF9800; }

.article-time {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.article-title {
  font-size: 16px;
  font-weight: 600;
  color: white;
  margin-bottom: 8px;
  line-height: 1.4;
}

.article-summary {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.5;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-footer {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.load-more {
  text-align: center;
  padding: 14px;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 40px;
  cursor: pointer;
  color: #40E0D0;
  transition: all 0.2s;
  font-weight: 500;
}

.load-more:hover {
  background: rgba(64, 224, 208, 0.2);
}

.loading-more {
  text-align: center;
  padding: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: rgba(255, 255, 255, 0.7);
}

.loading-state, .empty-state {
  text-align: center;
  padding: 60px 20px;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(12px);
  border-radius: 24px;
}

.loading-state p, .empty-state p {
  color: rgba(255, 255, 255, 0.7);
  margin-top: 16px;
}

.empty-hint {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.4);
  margin-top: 8px !important;
}

.empty-state span {
  font-size: 48px;
  opacity: 0.5;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.2);
  border-top-color: #40E0D0;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto;
}

.loading-spinner.small {
  width: 20px;
  height: 20px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ========== 阅读模式 ========== */
.reader-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #F7F5F0;
  z-index: 10000;
  overflow: hidden;
}

.reader-modal-content {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 进度条 */
.reader-progress {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #E8E5E0;
  z-index: 10002;
}

.reader-progress-fill {
  height: 100%;
  background: #7CB342;
  transition: width 0.1s ease;
}

/* 滚动区域 */
.reader-scroll-area {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0;
  scrollbar-width: thin;
}

.reader-scroll-area::-webkit-scrollbar {
  width: 6px;
}

.reader-scroll-area::-webkit-scrollbar-track {
  background: #EDEBE7;
}

.reader-scroll-area::-webkit-scrollbar-thumb {
  background: #CCC8C2;
  border-radius: 4px;
}

.reader-inner {
  padding: 50px 0 80px;
}

.article-wrapper {
  max-width: 680px;
  margin: 0 auto;
  padding: 0 30px;
}

/* 顶部工具栏 */
.reader-toolbar {
  position: fixed;
  top: 20px;
  right: 30px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(247, 245, 240, 0.9);
  backdrop-filter: blur(12px);
  padding: 8px 18px;
  border-radius: 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.05);
  z-index: 10001;
}

.font-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.font-btn {
  background: transparent;
  border: none;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
}

.font-btn:hover {
  background: #E8E5DF;
  color: #2C2C2C;
}

.font-size-num {
  font-size: 13px;
  color: #888;
  min-width: 36px;
  text-align: center;
}

.close-btn {
  background: transparent;
  border: none;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  font-size: 20px;
  cursor: pointer;
  color: #999;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #E8E5DF;
  color: #E57373;
}

/* 文章头部 */
.article-header-info {
  margin-bottom: 40px;
}

.category-badge {
  display: inline-block;
  font-size: 12px;
  padding: 4px 14px;
  border-radius: 30px;
  margin-bottom: 20px;
  background: rgba(124, 179, 66, 0.12);
  color: #7CB342;
}

.article-title-full {
  font-size: 34px;
  font-weight: 600;
  color: #2C2C2C;
  line-height: 1.3;
  margin-bottom: 20px;
  letter-spacing: -0.3px;
  font-family: "Georgia", "Times New Roman", "Songti SC", serif;
}

.article-meta-info {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #999;
  padding-bottom: 24px;
  border-bottom: 1px solid #E8E5E0;
}

/* 文章正文 */
.article-body {
  font-size: 17px;
  line-height: 1.9;
  color: #3D3A36;
  font-family: "Georgia", "Times New Roman", "Songti SC", serif;
}

.article-body h2.section-heading {
  font-size: 24px;
  font-weight: 600;
  color: #2C2C2C;
  margin: 44px 0 20px;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;
  letter-spacing: -0.2px;
}

.article-body p {
  margin-bottom: 28px;
}

.article-body strong {
  color: #2C2C2C;
  font-weight: 600;
}

.article-body .num-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: #7CB342;
  color: white;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  margin-right: 12px;
  position: relative;
  top: -1px;
}

.article-body .quote-block {
  background: rgba(124, 179, 66, 0.06);
  border-left: 4px solid #7CB342;
  padding: 20px 24px;
  margin: 32px 0;
  border-radius: 8px;
  font-style: italic;
  color: #5A5A5A;
  font-size: 16px;
  font-family: "Georgia", serif;
}

/* 底部操作区 */
.action-section {
  margin: 48px 0 32px;
  text-align: center;
}

.action-group {
  display: flex;
  justify-content: center;
  gap: 24px;
}

.action-circle {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(0, 0, 0, 0.06);
  font-size: 20px;
  cursor: pointer;
  color: #888;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-circle:hover {
  background: rgba(124, 179, 66, 0.1);
  border-color: #7CB342;
  color: #7CB342;
  transform: translateY(-2px);
}

.action-circle.active {
  color: #FFD700;
  border-color: #FFD700;
}

.action-circle.like:hover {
  color: #E57373;
  border-color: #E57373;
}

/* 相关推荐 */
.related-section {
  margin-top: 50px;
  padding-top: 20px;
}

.related-header {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 32px;
}

.related-line {
  width: 40px;
  height: 1px;
  background: #D4D1CC;
}

.related-title {
  font-size: 14px;
  font-weight: 500;
  color: #999;
  letter-spacing: 2px;
}

.related-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.related-card {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.7);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid rgba(0, 0, 0, 0.03);
}

.related-card:hover {
  background: white;
  transform: translateX(6px);
  box-shadow: 0 4px 15px rgba(0, 0, 0, 0.05);
}

.related-card-emoji {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, rgba(124, 179, 66, 0.1), rgba(124, 179, 66, 0.05));
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
}

.related-card-info {
  flex: 1;
}

.related-card-title {
  font-size: 16px;
  font-weight: 600;
  color: #2C2C2C;
  margin-bottom: 6px;
  line-height: 1.4;
}

.related-card-summary {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
  line-height: 1.4;
}

.related-card-tag {
  font-size: 10px;
  padding: 2px 10px;
  border-radius: 20px;
  background: rgba(124, 179, 66, 0.1);
  color: #7CB342;
  display: inline-block;
}

/* 响应式 */
@media (max-width: 768px) {
  .article-container {
    padding: 16px;
  }

  .article-grid {
    grid-template-columns: 1fr;
  }

  .search-bar {
    flex-direction: column;
  }

  .search-wrapper {
    width: 100%;
  }

  .search-btn {
    width: 100%;
    text-align: center;
  }

  .category-tabs button {
    padding: 6px 14px;
    font-size: 12px;
  }

  .article-wrapper {
    padding: 0 20px;
  }

  .article-title-full {
    font-size: 26px;
  }

  .article-body {
    font-size: 16px;
  }

  .reader-toolbar {
    top: 12px;
    right: 12px;
    padding: 6px 12px;
  }

  .action-group {
    gap: 16px;
  }

  .action-circle {
    width: 42px;
    height: 42px;
    font-size: 18px;
  }

  .related-card {
    padding: 12px;
  }

  .related-card-emoji {
    width: 48px;
    height: 48px;
    font-size: 24px;
  }
}
</style>