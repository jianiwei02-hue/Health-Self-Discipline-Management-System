<template>
  <div class="favorites-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <button class="back-btn" @click="goBack">
        ← 返回
      </button>
      <div class="header-title">
        <h1>我的收藏</h1>
        <p>您收藏的健康文章</p>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- 空状态 -->
    <div v-else-if="articles.length === 0" class="empty-state">
      <div class="empty-icon">⭐</div>
      <h3>暂无收藏文章</h3>
      <p>去浏览文章，收藏你喜欢的健康资讯吧~</p>
      <button class="go-browse-btn" @click="goToArticles">去浏览文章</button>
    </div>

    <!-- 收藏列表 -->
    <div v-else class="articles-grid">
      <div
          v-for="article in articles"
          :key="article.id"
          class="article-card"
          @click="viewArticle(article.id)"
      >
        <div class="article-cover">
          <span class="article-emoji">{{ getCategoryEmoji(article.category) }}</span>
        </div>
        <div class="article-info">
          <div class="article-header">
            <span class="category-tag" :class="getCategoryClass(article.category)">
              {{ getCategoryName(article.category) }}
            </span>
            <button class="uncollect-btn" @click.stop="handleUncollect(article.id)">
              取消收藏
            </button>
          </div>
          <h3 class="article-title">{{ article.title }}</h3>
          <p class="article-summary">{{ article.summary || (article.content ? article.content.substring(0, 80) : '') }}...</p>
          <div class="article-footer">
            <span>👁️ {{ formatNumber(article.viewCount || 0) }}</span>
            <span>❤️ {{ formatNumber(article.likeCount || 0) }}</span>
            <span>📅 {{ formatDate(article.createTime) }}</span>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const props = defineProps({
  userId: {
    type: Number,
    required: true
  }
})

const API_BASE = 'http://localhost:8080/api'

// 数据
const articles = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const currentPage = ref(0)
const hasMore = ref(true)
const pageSize = 10

// 格式化数字
const formatNumber = (num) => {
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w'
  return num.toString()
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}/${date.getMonth() + 1}/${date.getDate()}`
}

// 获取分类表情
const getCategoryEmoji = (category) => {
  const map = {
    'DIET': '🥗',
    'SPORT': '🏃',
    'SLEEP': '😴',
    'MENTAL': '🧠'
  }
  return map[category] || '📖'
}

// 获取分类名称
const getCategoryName = (category) => {
  const map = {
    'DIET': '饮食健康',
    'SPORT': '运动健身',
    'SLEEP': '优质睡眠',
    'MENTAL': '心理健康'
  }
  return map[category] || category
}

// 获取分类样式类
const getCategoryClass = (category) => {
  const map = {
    'DIET': 'cat-diet',
    'SPORT': 'cat-sport',
    'SLEEP': 'cat-sleep',
    'MENTAL': 'cat-mental'
  }
  return map[category] || ''
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 跳转到文章列表页
const goToArticles = () => {
  router.push('/health-article')
}

// 加载收藏列表
const loadFavorites = async (reset = true) => {
  if (!props.userId) {
    console.error('用户未登录')
    return
  }

  if (reset) {
    loading.value = true
    currentPage.value = 0
    articles.value = []
    hasMore.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const response = await axios.get(`${API_BASE}/health-article/favorites/${props.userId}`, {
      params: {
        page: currentPage.value,
        size: pageSize
      }
    })

    if (response.data.success) {
      let newArticles = response.data.articles || []
      const total = response.data.total || 0

      if (currentPage.value === 0) {
        articles.value = newArticles
      } else {
        articles.value = [...articles.value, ...newArticles]
      }

      hasMore.value = articles.value.length < total
    }
  } catch (error) {
    console.error('加载收藏列表失败', error)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 加载更多
const loadMore = () => {
  if (loadingMore.value || !hasMore.value) return
  currentPage.value++
  loadFavorites(false)
}

// 查看文章详情
const viewArticle = (articleId) => {
  router.push({ path: '/health-article', query: { articleId: articleId, showDetail: 'true' } })
}

// 取消收藏
const handleUncollect = async (articleId) => {
  if (!props.userId) return

  try {
    await axios.delete(`${API_BASE}/health-article/collect/${articleId}`, {
      params: { userId: props.userId }
    })
    articles.value = articles.value.filter(a => a.id !== articleId)
    if (articles.value.length === 0) {
      hasMore.value = false
    }
  } catch (error) {
    console.error('取消收藏失败', error)
    alert('取消收藏失败，请重试')
  }
}

// 初始化
onMounted(() => {
  loadFavorites()
})
</script>

<style scoped>
.favorites-container {
  min-height: 100vh;
  padding: 0;
  background: transparent;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
  flex-wrap: wrap;
}

.back-btn {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 40px;
  padding: 8px 20px;
  color: white;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(64, 224, 208, 0.2);
  border-color: #40E0D0;
  color: #40E0D0;
}

.header-title h1 {
  color: white;
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 2px 0;
}

.header-title p {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  margin: 0;
}

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 60px 20px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(255, 255, 255, 0.15);
  border-top-color: #40E0D0;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

.loading-spinner.small {
  width: 20px;
  height: 20px;
  margin: 0;
}

.loading-state p {
  color: rgba(255, 255, 255, 0.6);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  border-radius: 24px;
  margin-top: 40px;
}

.empty-icon {
  font-size: 56px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-state h3 {
  color: white;
  font-size: 18px;
  margin-bottom: 8px;
  font-weight: 500;
}

.empty-state p {
  color: rgba(255, 255, 255, 0.5);
  margin-bottom: 20px;
  font-size: 14px;
}

.go-browse-btn {
  padding: 10px 28px;
  background: rgba(64, 224, 208, 0.15);
  border: 1px solid rgba(64, 224, 208, 0.4);
  border-radius: 40px;
  color: #40E0D0;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.go-browse-btn:hover {
  background: rgba(64, 224, 208, 0.3);
}

/* 文章列表 */
.articles-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 文章卡片 - 简化版 */
.article-card {
  display: flex;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
}

.article-card:hover {
  background: rgba(0, 0, 0, 0.45);
  border-color: rgba(64, 224, 208, 0.3);
  transform: translateY(-2px);
}

.article-cover {
  width: 100px;
  background: rgba(64, 224, 208, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.article-emoji {
  font-size: 44px;
}

.article-info {
  flex: 1;
  padding: 14px 18px;
}

.article-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  flex-wrap: wrap;
  gap: 8px;
}

.category-tag {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 20px;
  font-weight: 500;
}

.cat-diet {
  background: rgba(124, 179, 66, 0.2);
  color: #8BC34A;
}

.cat-sport {
  background: rgba(64, 224, 208, 0.2);
  color: #40E0D0;
}

.cat-sleep {
  background: rgba(156, 39, 176, 0.2);
  color: #CE93D8;
}

.cat-mental {
  background: rgba(255, 152, 0, 0.2);
  color: #FFB74D;
}

.uncollect-btn {
  background: rgba(255, 100, 100, 0.1);
  border: 1px solid rgba(255, 100, 100, 0.25);
  border-radius: 20px;
  padding: 3px 10px;
  font-size: 11px;
  cursor: pointer;
  color: #ff8888;
  transition: all 0.2s;
}

.uncollect-btn:hover {
  background: rgba(255, 100, 100, 0.2);
}

.article-title {
  font-size: 16px;
  font-weight: 600;
  color: white;
  margin: 0 0 6px 0;
  line-height: 1.4;
}

.article-summary {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
  line-height: 1.45;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-footer {
  display: flex;
  gap: 16px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.45);
}

/* 加载更多 */
.load-more {
  text-align: center;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 40px;
  cursor: pointer;
  color: #40E0D0;
  transition: all 0.2s;
  font-weight: 500;
  margin-top: 24px;
  border: 1px solid rgba(64, 224, 208, 0.2);
}

.load-more:hover {
  background: rgba(64, 224, 208, 0.12);
}

.loading-more {
  text-align: center;
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.6);
}

/* 动画 */
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 响应式 */
@media (max-width: 768px) {
  .article-card {
    flex-direction: column;
  }

  .article-cover {
    width: 100%;
    height: 70px;
  }

  .article-emoji {
    font-size: 36px;
  }

  .article-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-title h1 {
    font-size: 20px;
  }
}
</style>