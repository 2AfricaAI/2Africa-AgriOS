<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <h2 class="page-title">{{ t('gap.title') }}</h2>
      <p class="page-sub">{{ t('gap.subtitle') }}</p>

      <el-form :inline="true" :model="form" @submit.prevent>
        <el-form-item :label="t('gap.from')">
          <el-date-picker v-model="form.from" type="date" value-format="YYYY-MM-DD" :placeholder="t('gap.from')" style="width: 160px" />
        </el-form-item>
        <el-form-item :label="t('gap.to')">
          <el-date-picker v-model="form.to" type="date" value-format="YYYY-MM-DD" :placeholder="t('gap.to')" style="width: 160px" />
        </el-form-item>
        <el-form-item :label="t('gap.crop')">
          <el-select v-model="form.cropId" :placeholder="t('common.all')" clearable filterable style="width: 200px">
            <el-option v-for="c in crops" :key="c.id" :label="`${c.name} (${c.code})`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="DownloadIcon" :loading="downloading" @click="onDownload">
            {{ t('gap.downloadXlsx') }}
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="hint-card">
      <h3>{{ t('gap.howItWorks') }}</h3>
      <ol>
        <li>{{ t('gap.step1') }}</li>
        <li>{{ t('gap.step2') }}</li>
        <li>{{ t('gap.step3') }}</li>
      </ol>
      <el-divider />
      <p class="tip">{{ t('gap.perBatchHint') }}</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Download as DownloadIcon } from '@element-plus/icons-vue'
import { downloadPeriodXlsx } from '@/api/gapReport'
import { listCrops } from '@/api/crop'

const { t } = useI18n()
const form = reactive({ from: '', to: '', cropId: null })
const crops = ref([])
const downloading = ref(false)

async function loadCrops() {
  const data = await listCrops({ page: 1, size: 500 })
  crops.value = data.list
}

async function onDownload() {
  if (!form.from && !form.to) {
    ElMessage.warning(t('gap.dateRequired'))
    return
  }
  downloading.value = true
  try {
    const blob = await downloadPeriodXlsx({
      from: form.from || undefined,
      to: form.to || undefined,
      cropId: form.cropId || undefined,
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `GAP-period-${form.from || 'all'}-to-${form.to || 'all'}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success(t('gap.downloadSuccess'))
  } finally { downloading.value = false }
}

onMounted(loadCrops)
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.page-title { font-size: 18px; font-weight: 600; margin: 0 0 4px; color: #1f2937; }
.page-sub { color: #6b7280; font-size: 13px; margin: 0 0 14px; }
.filter-card :deep(.el-card__body),
.hint-card   :deep(.el-card__body) { padding: 16px; }
.hint-card h3 { margin: 0 0 8px; font-size: 13px; color: #15803d; font-weight: 600; }
.hint-card ol { margin: 0; padding-left: 22px; font-size: 13px; line-height: 1.8; color: #374151; }
.hint-card .tip { margin: 0; font-size: 12px; color: #6b7280; font-style: italic; }
</style>
