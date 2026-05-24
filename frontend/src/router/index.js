import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppLayout from '@/layouts/AppLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, title: '登录' },
  },
  // 所有受保护路由都套在 AppLayout 下
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        name: 'home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'master/crops',
        name: 'crops',
        component: () => import('@/views/master/CropList.vue'),
        meta: { title: '作物管理' },
      },
      {
        path: 'master/varieties',
        name: 'varieties',
        component: () => import('@/views/master/VarietyList.vue'),
        meta: { title: '品种管理' },
      },
      {
        path: 'master/packaging-specs',
        name: 'packaging-specs',
        component: () => import('@/views/master/PackagingSpecList.vue'),
        meta: { title: '包装规格' },
      },
      {
        path: 'master/warehouses',
        name: 'warehouses',
        component: () => import('@/views/master/WarehouseList.vue'),
        meta: { title: '仓库库位' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  document.title = to.meta.title ? `${to.meta.title} · Albert's Farm` : "Albert's Farm"

  if (!to.meta.public && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.isLoggedIn) {
    return { path: '/' }
  }
})

export default router
