import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'LandingPage',
      component: () => import('@/components/LandingPage.vue')
    },
    {
      path: '/health-data',
      name: 'HealthData',
      component: () => import('@/components/HealthData.vue')
    },
    {
      path: '/task-page',
      name: 'TaskPage',
      component: () => import('@/components/TaskPage.vue')
    },
    {
      path: '/sport-page',
      name: 'SportPage',
      component: () => import('@/components/SportPage.vue')
    },
    {
      path: '/diet-page',
      name: 'DietPage',
      component: () => import('@/components/DietPage.vue')
    },
    {
      path: '/community',
      name: 'Community',
      component: () => import('@/components/Community.vue')
    },
    {
      path: '/health-analysis',
      name: 'HealthAnalysis',
      component: () => import('@/components/HealthAnalysis.vue')
    },
    // 个人中心路由
    {
      path: '/profile/:userId',
      name: 'UserProfile',
      component: () => import('@/components/UserProfile.vue'),
      props: true
    },
    // 我的收藏路由
    {
      path: '/favorites/:userId',
      name: 'MyFavorites',
      component: () => import('@/components/MyFavorites.vue'),
      props: true
    }
  ]
})

export default router