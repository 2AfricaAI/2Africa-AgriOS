import request from '@/utils/request'

const BASE = '/v1/files'

/**
 * 上传文件
 * @param {File} file      浏览器原生 File 对象
 * @param {string} bizType 业务分类: avatar / crop_image / activity_photo / ...
 * @param {(percent: number) => void} onProgress 进度回调 (0-100)
 */
export function uploadFile(file, bizType, onProgress) {
  const fd = new FormData()
  fd.append('file', file)
  if (bizType) fd.append('bizType', bizType)
  return request.post(`${BASE}/upload`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (e) => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded / e.total) * 100))
      }
    },
    timeout: 60_000,  // 大文件需要更长
  })
}

export function getFile(id) {
  return request.get(`${BASE}/${id}`)
}

export function refreshDownloadUrl(id) {
  return request.get(`${BASE}/${id}/download-url`)
}

export function deleteFile(id) {
  return request.delete(`${BASE}/${id}`)
}
