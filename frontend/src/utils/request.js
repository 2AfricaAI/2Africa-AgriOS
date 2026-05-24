import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// ============================================================
// 请求拦截器: 自动加 Authorization: Bearer <token>
// ============================================================
request.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

// ============================================================
// 响应拦截器: 拆 R<T> 信封 + 统一错误处理
// 后端约定: HTTP 200 + body code=200 才算成功; code!=200 是业务错误
// ============================================================
request.interceptors.response.use(
  (response) => {
    const data = response.data
    // 二进制/文件流之类直接放行
    if (!data || typeof data !== 'object' || data.code === undefined) {
      return data
    }
    // 业务成功: 返回 data 字段而不是整个信封,业务代码更干净
    if (data.code === 200) {
      return data.data
    }
    // 业务失败 (HTTP 200 但 body code 不是 200)
    ElMessage.error(data.msg || `请求失败 (code=${data.code})`)
    return Promise.reject(new Error(data.msg || `code=${data.code}`))
  },
  (error) => {
    const status = error.response?.status
    const body = error.response?.data

    if (status === 401 || status === 403) {
      // token 失效 / 权限不足
      const auth = useAuthStore()
      auth.clear()
      ElMessage.warning(body?.msg || '登录已过期,请重新登录')
      router.push({
        path: '/login',
        query: { redirect: router.currentRoute.value.fullPath },
      })
    } else {
      ElMessage.error(body?.msg || error.message || '网络错误')
    }
    return Promise.reject(error)
  },
)

export default request
