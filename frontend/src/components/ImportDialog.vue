<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    :title="t('import.title', { entity: title })"
    width="720px"
    @closed="reset"
  >
    <!-- ==================================================== -->
    <!-- Step 1: pick a file (default) or step 2: show result -->
    <!-- ==================================================== -->
    <div v-if="!result" class="step-pick">
      <p class="hint">{{ t('import.howTo') }}</p>

      <div class="actions-row">
        <el-button :icon="DownloadIcon" @click="downloadTemplate">
          {{ t('import.downloadTemplate') }}
        </el-button>
        <span class="dim small">{{ t('import.templateHint') }}</span>
      </div>

      <div
        class="drop-zone"
        :class="{ dragging }"
        @dragover.prevent="dragging = true"
        @dragleave="dragging = false"
        @drop.prevent="onDrop"
      >
        <el-icon class="drop-icon"><UploadFilled /></el-icon>
        <div>
          <strong>{{ t('import.dropOr') }}</strong>
          <el-button type="primary" link @click="pickFile">{{ t('import.browse') }}</el-button>
        </div>
        <div v-if="picked" class="picked">
          <el-icon><Document /></el-icon>
          {{ picked.name }}
          <span class="dim small">({{ formatSize(picked.size) }})</span>
          <el-button text :icon="DeleteIcon" @click.stop="picked = null" />
        </div>
        <input ref="fileInput" type="file" accept=".xlsx,.xls" hidden @change="onFileChange" />
      </div>
    </div>

    <!-- ==================================================== -->
    <!-- Step 2: result                                       -->
    <!-- ==================================================== -->
    <div v-else class="step-result">
      <el-alert
        v-if="result.errors.length === 0"
        type="success"
        show-icon
        :closable="false"
        :title="t('import.allGood')"
      >
        <template #default>
          <div class="result-stats">
            <span><strong>{{ result.parsed }}</strong> {{ t('import.parsed') }}</span>
            <span><strong>{{ result.success }}</strong> {{ t('import.success') }}</span>
            <span><strong>{{ result.skipped }}</strong> {{ t('import.skipped') }}</span>
          </div>
        </template>
      </el-alert>

      <el-alert
        v-else
        type="warning"
        show-icon
        :closable="false"
        :title="t('import.errorsFound', { n: result.errors.length })"
      >
        <template #default>
          <p>{{ t('import.allOrNothing') }}</p>
          <div class="result-stats">
            <span>{{ t('import.parsed') }}: <strong>{{ result.parsed }}</strong></span>
            <span class="dim">|</span>
            <span>{{ t('import.errors') }}: <strong>{{ result.errors.length }}</strong></span>
          </div>
        </template>
      </el-alert>

      <el-table v-if="result.errors.length > 0" :data="result.errors" max-height="280" border size="small" stripe>
        <el-table-column prop="rowNumber" :label="t('import.row')" width="70" align="center" />
        <el-table-column prop="field" :label="t('import.field')" width="160">
          <template #default="{ row }"><code class="code">{{ row.field }}</code></template>
        </el-table-column>
        <el-table-column prop="message" :label="t('import.message')" min-width="220" show-overflow-tooltip />
        <el-table-column prop="rawRow" :label="t('import.rawRow')" min-width="240" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="dim small">{{ row.rawRow }}</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <template #footer>
      <el-button v-if="result" @click="reset">{{ t('import.tryAgain') }}</el-button>
      <el-button @click="close">{{ t('common.close') }}</el-button>
      <el-button
        v-if="!result"
        type="primary"
        :loading="uploading"
        :disabled="!picked"
        @click="onUpload"
      >
        {{ t('import.upload') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  UploadFilled, Document, Delete as DeleteIcon, Download as DownloadIcon,
} from '@element-plus/icons-vue'
import request from '@/utils/request'

const props = defineProps({
  modelValue: Boolean,
  title: { type: String, required: true },        // "Crops", "Customers", ...
  templateUrl: { type: String, required: true },  // GET endpoint for blank xlsx
  importUrl: { type: String, required: true },    // POST endpoint with multipart file
})
const emit = defineEmits(['update:modelValue', 'imported'])

const { t } = useI18n()
const fileInput = ref(null)
const picked = ref(null)
const dragging = ref(false)
const uploading = ref(false)
const result = ref(null)

function pickFile() { fileInput.value?.click() }
function onFileChange(e) { picked.value = e.target.files?.[0] || null }
function onDrop(e) {
  dragging.value = false
  const f = e.dataTransfer.files?.[0]
  if (!f) return
  if (!/\.(xlsx|xls)$/i.test(f.name)) {
    ElMessage.warning(t('import.onlyXlsx'))
    return
  }
  picked.value = f
}
function formatSize(n) {
  if (n < 1024) return `${n} B`
  if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)} KB`
  return `${(n / 1024 / 1024).toFixed(1)} MB`
}

async function downloadTemplate() {
  try {
    const resp = await request.get(props.templateUrl, { responseType: 'blob' })
    const blob = resp instanceof Blob ? resp : new Blob([resp])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${props.title.toLowerCase()}_template.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error(t('import.templateFailed'))
  }
}

async function onUpload() {
  if (!picked.value) return
  uploading.value = true
  try {
    const form = new FormData()
    form.append('file', picked.value)
    result.value = await request.post(props.importUrl, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    if (result.value.errors.length === 0) {
      ElMessage.success(t('import.allGood'))
      emit('imported', result.value)
    }
  } catch (e) {
    ElMessage.error(t('import.uploadFailed'))
  } finally {
    uploading.value = false
  }
}

function reset() {
  picked.value = null
  result.value = null
  if (fileInput.value) fileInput.value.value = ''
}
function close() {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.hint { color: #475569; font-size: 13px; margin: 0 0 12px; }
.actions-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 10px 14px;
  background: #f8fafc;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}
.dim { color: #94a3b8; }
.small { font-size: 12px; }
.code {
  font-family: 'Monaco', 'Consolas', monospace; font-size: 12px;
  background: #f5f7fa; padding: 2px 6px; border-radius: 4px; color: #1f7a35;
}
.drop-zone {
  border: 2px dashed #cbd5e1;
  border-radius: 8px;
  padding: 36px 16px;
  text-align: center;
  cursor: pointer;
  transition: all .15s;
  background: #fafbfc;
}
.drop-zone.dragging { border-color: #16a34a; background: #f0fdf4; }
.drop-zone:hover { border-color: #16a34a; }
.drop-icon { font-size: 44px; color: #94a3b8; margin-bottom: 8px; }
.picked {
  margin-top: 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}
.result-stats {
  display: flex; gap: 14px; align-items: center;
  margin-top: 6px; font-size: 13px;
}
.step-result :deep(.el-alert) { margin-bottom: 14px; }
</style>
