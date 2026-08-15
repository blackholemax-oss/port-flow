import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/DashboardView.vue'),
        },
        {
          path: 'portfolios',
          name: 'portfolios',
          component: () => import('@/views/PortfoliosView.vue'),
        },
        {
          path: 'users',
          name: 'users',
          component: () => import('@/views/UsersView.vue'),
        },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
})

// 登录守卫：未登录跳转 /login，已登录访问 /login 跳回仪表盘
router.beforeEach((to) => {
  const authed = localStorage.getItem('pf-admin-user') !== null
  if (!to.meta.public && !authed) return { name: 'login' }
  if (to.name === 'login' && authed) return { path: '/dashboard' }
})

export default router
