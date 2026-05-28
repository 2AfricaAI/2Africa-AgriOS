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
  async (response) => {
    const data = response.data
    // 二进制/文件流 (responseType: blob/arraybuffer/stream) - 直接放行
    if (response.config?.responseType === 'blob' ||
        response.config?.responseType === 'arraybuffer' ||
        response.config?.responseType === 'stream') {
      // 兜底: 如果 server 返回了 JSON 错误信封 (而非 PDF), 在这里转抛
      if (data instanceof Blob && data.type === 'application/json') {
        try {
          const text = await data.text()
          const json = JSON.parse(text)
          ElMessage.error(json.msg || `Download failed (code=${json.code})`)
          return Promise.reject(new Error(json.msg || `code=${json.code}`))
        } catch {/* 解析失败就当 PDF 处理 */}
      }
      return data
    }
    // 旧逻辑保底
    if (!data || typeof data !== 'object' || data.code === undefined) {
      return data
    }
    // 业务成功: 返回 data 字段而不是整个信封,业务代码更干净
    if (data.code === 200) {
      return data.data
    }
    // 业务失败 (HTTP 200 但 body code 不是 200)
    ElMessage.error(data.msg || `Request failed (code=${data.code})`)
    return Promise.reject(new Error(data.msg || `code=${data.code}`))
  },
  async (error) => {
    const status = error.response?.status
    let body = error.response?.data

    // 如果是 responseType=blob 的错误响应, body 是 Blob, 需要先解
    if (body instanceof Blob) {
      try { body = JSON.parse(await body.text()) } catch { body = null }
    }

    if (status === 401 || status === 403) {
      // token 失效 / 权限不足
      const auth = useAuthStore()
      auth.clear()
      ElMessage.warning(body?.msg || 'Session expired, please log in again')
      router.push({
        path: '/login',
        query: { redirect: router.currentRoute.value.fullPath },
      })
    } else {
      ElMessage.error(body?.msg || error.message || 'Network error')
    }
    return Promise.reject(error)
  },
)

export default request
