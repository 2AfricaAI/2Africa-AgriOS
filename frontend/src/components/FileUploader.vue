<template>
  <div class="uploader">
    <!-- 已上传文件列表 -->
    <div v-if="files.length > 0" class="file-grid">
      <div v-for="(f, i) in files" :key="f.id || f._tempId" class="file-item">
        <!-- 图片预览 -->
        <el-image
          v-if="isImage(f.mimeType)"
          :src="f.downloadUrl"
          :preview-src-list="imageUrls"
          :initial-index="imagePreviewIndex(f)"
          fit="cover"
          class="thumb"
          preview-teleported
        >
          <template #error>
            <div class="thumb-error">
              <el-icon><PictureIcon /></el-icon>
              <span>加载失败</span>
            </div>
          </template>
        </el-image>

        <!-- 非图片显示文件图标 -->
        <div v-else class="file-icon">
          <el-icon :size="36"><DocumentIcon /></el-icon>
        </div>

        <!-- 上传中的进度遮罩 -->
        <div v-if="f._status === 'uploading'" class="progress-mask">
          <el-progress
            type="circle"
            :percentage="f._progress || 0"
            :width="60"
            :stroke-width="6"
          />
        </div>

        <!-- 上传失败遮罩 -->
        <div v-else-if="f._status === 'error'" class="error-mask">
          <el-icon :size="20"><WarningIcon /></el-icon>
          <span>失败</span>
          <el-button size="small" type="primary" @click="retry(i)">重试</el-button>
        </div>

        <!-- 文件名 / 大小 -->
        <div class="meta">
          <div class="name" :title="f.originalName">{{ f.originalName || '上传中...' }}</div>
          <div class="size">{{ humanSize(f.sizeBytes) }}</div>
        </div>

        <!-- 删除按钮 (仅成功后才允许) -->
        <el-button
          v-if="f._status === 'done' || !f._status"
          class="del-btn"
          circle
          size="small"
          type="danger"
          :icon="DeleteIcon"
          @click="onDelete(i)"
        />
      </div>
    </div>

    <!-- 上传区域 (达到上限时隐藏) -->
    <el-upload
      v-if="files.length < limit"
      class="dropzone"
      drag
      :show-file-list="false"
      :multiple="limit > 1"
      :accept="accept"
      :before-upload="onBeforeUpload"
      :http-request="customUpload"
    >
      <el-icon :size="48" class="dropzone-icon"><UploadIcon /></el-icon>
      <div class="dropzone-text">
        拖拽文件到这里,或<em>点击选择</em>
      </div>
      <div class="dropzone-hint">
        最多 {{ limit }} 个 · 单个 ≤ {{ maxSizeMb }} MB · {{ accept || '任意类型' }}
      </div>
    </el-upload>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  UploadFilled as UploadIcon,
  Document as DocumentIcon,
  Picture as PictureIcon,
  Delete as DeleteIcon,
  WarningFilled as WarningIcon,
} from '@element-plus/icons-vue'
import { uploadFile, deleteFile } from '@/api/file'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  bizType:    { type: String, default: 'misc' },
  accept:     { type: String, default: 'image/*' },
  limit:      { type: Number, default: 5 },
  maxSizeMb:  { type: Number, default: 10 },
})
const emit = defineEmits(['update:modelValue', 'success'])

// 用本地副本同步外部 v-model
const files = ref([...props.modelValue])
watch(() => props.modelValue, (v) => {
  // 父组件覆盖时同步过来,但忽略我们自己 emit 引起的回写
  if (v !== files.value) files.value = [...v]
})

function emitChange() {
  // 只把"完成"的文件 emit 出去,上传中的不算
  emit('update:modelValue', files.value.filter(f => !f._status || f._status === 'done'))
}

// ============================================================
// 工具
// ============================================================
function isImage(mime) {
  return mime && mime.startsWith('image/')
}

function humanSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

const imageUrls = computed(() =>
  files.value.filter(f => isImage(f.mimeType)).map(f => f.downloadUrl)
)
function imagePreviewIndex(f) {
  return imageUrls.value.indexOf(f.downloadUrl)
}

// ============================================================
// 上传前校验
// ============================================================
function onBeforeUpload(file) {
  const sizeMb = file.size / 1024 / 1024
  if (sizeMb > props.maxSizeMb) {
    ElMessage.warning(`文件超过 ${props.maxSizeMb} MB`)
    return false
  }
  if (files.value.length >= props.limit) {
    ElMessage.warning(`最多上传 ${props.limit} 个文件`)
    return false
  }
  return true
}

// ============================================================
// 自定义上传 (替代 el-upload 默认行为,走我们的 axios)
// ============================================================
let tempIdSeq = 0
async function customUpload({ file }) {
  const tempId = `tmp-${++tempIdSeq}`
  // 先占位一条
  const placeholder = {
    _tempId: tempId,
    _status: 'uploading',
    _progress: 0,
    _file: file,         // 留着重试用
    originalName: file.name,
    sizeBytes: file.size,
    mimeType: file.type,
  }
  files.value.push(placeholder)

  try {
    const data = await uploadFile(file, props.bizType, (p) => {
      const idx = files.value.findIndex(f => f._tempId === tempId)
      if (idx >= 0) files.value[idx]._progress = p
    })
    // 成功 - 用后端返回替换占位
    const idx = files.value.findIndex(f => f._tempId === tempId)
    if (idx >= 0) {
      files.value[idx] = { ...data, _status: 'done' }
    }
    emit('success', data)
    emitChange()
  } catch {
    // 失败 - 标记错误状态,保留占位以便重试
    const idx = files.value.findIndex(f => f._tempId === tempId)
    if (idx >= 0) files.value[idx]._status = 'error'
  }
}

async function retry(i) {
  const f = files.value[i]
  if (!f._file) return
  const file = f._file
  // 移除旧占位重新上传
  files.value.splice(i, 1)
  await customUpload({ file })
}

// ============================================================
// 删除
// ============================================================
async function onDelete(i) {
  const f = files.value[i]
  // 上传成功的需要调后端删
  if (f.id) {
    try {
      await deleteFile(f.id)
    } catch {
      return  // 后端报错就别从前端移除
    }
  }
  files.value.splice(i, 1)
  emitChange()
  ElMessage.success('已删除')
}
</script>

<style scoped>
.uploader {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 14px;
}

.file-item {
  position: relative;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 8px;
  transition: box-shadow .2s;
}

.file-item:hover {
  box-shadow: 0 4px 12px rgba(0, 21, 41, .08);
}

.thumb {
  width: 100%;
  height: 140px;
  border-radius: 6px;
  background: #f5f7fa;
  overflow: hidden;
}

.thumb-error {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #c0c4cc;
  font-size: 12px;
}

.file-icon {
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
  border-radius: 6px;
}

.progress-mask,
.error-mask {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, .85);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 8px;
  border-radius: 8px;
}

.error-mask {
  color: #f56c6c;
  font-size: 13px;
}

.meta {
  font-size: 12px;
  color: #606266;
  line-height: 1.4;
  min-height: 32px;
}

.meta .name {
  font-weight: 500;
  color: #1f2329;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta .size {
  color: #909399;
  font-size: 11px;
}

.del-btn {
  position: absolute;
  top: -8px;
  right: -8px;
  opacity: 0;
  transition: opacity .2s;
}

.file-item:hover .del-btn {
  opacity: 1;
}

.dropzone :deep(.el-upload-dragger) {
  padding: 28px 20px;
  border-radius: 8px;
  border-color: #d4d7de;
}

.dropzone-icon {
  color: #c0c4cc;
}

.dropzone-text {
  color: #606266;
  font-size: 14px;
  margin-top: 8px;
}

.dropzone-text em {
  color: #409eff;
  font-style: normal;
  margin: 0 4px;
}

.dropzone-hint {
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
}
</style>
