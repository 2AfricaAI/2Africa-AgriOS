<template>
  <div class="ma">
    <!-- 页头 -->
    <div class="ma-head">
      <button class="ma-back" @click="goBack">←</button>
      <span class="ma-head-title">{{ t('m.recordActivity') }}</span>
    </div>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <!-- 种植计划 -->
      <el-form-item :label="t('m.plan')" prop="planId">
        <el-select
          v-model="form.planId" filterable :placeholder="t('m.pickPlan')"
          size="large" style="width: 100%"
          :no-data-text="t('m.noPlans')"
        >
          <el-option
            v-for="p in plans"
            :key="p.id"
            :value="p.id"
            :label="`${p.plotName || ('Plot#' + p.plotId)} · ${p.code}`"
          />
        </el-select>
      </el-form-item>

      <!-- 活动类型 (大按钮) -->
      <el-form-item :label="t('m.activityType')" prop="activityType">
        <div class="ma-type-grid">
          <button
            v-for="opt in ACT_TYPES" :key="opt.value"
            type="button"
            class="ma-type-btn"
            :class="{ active: form.activityType === opt.value }"
            @click="form.activityType = opt.value"
          >
            <span class="ma-type-emoji">{{ opt.emoji }}</span>
            <span class="ma-type-label">{{ t('actType.' + opt.value) }}</span>
          </button>
        </div>
      </el-form-item>

      <!-- 日期 -->
      <el-form-item :label="t('m.date')" prop="occurDate">
        <el-date-picker
          v-model="form.occurDate" type="date" value-format="YYYY-MM-DD"
          size="large" style="width: 100%"
        />
      </el-form-item>

      <!-- GPS -->
      <el-form-item :label="t('m.gps')">
        <div class="ma-gps">
          <el-input v-model="form.locationGps" size="large" :placeholder="t('m.gpsPlaceholder')" :readonly="gpsLoading" />
          <button type="button" class="ma-gps-btn" :class="{ loading: gpsLoading }" @click="grabGps">
            {{ gpsLoading ? '⌛' : '📍' }} {{ t('m.locate') }}
          </button>
        </div>
        <div v-if="gpsAccuracy" class="ma-gps-note">
          {{ t('m.gpsAccuracy', { m: Math.round(gpsAccuracy) }) }}
        </div>
      </el-form-item>

      <!-- 拍照 -->
      <el-form-item :label="t('m.photos')">
        <div class="ma-photos">
          <div v-for="(p, i) in photos" :key="i" class="ma-photo">
            <img :src="p.preview" :alt="`photo-${i}`" />
            <button type="button" class="ma-photo-x" @click="removePhoto(i)">×</button>
          </div>
          <label v-if="photos.length < 6" class="ma-photo-add">
            <input
              type="file" accept="image/*" capture="environment"
              @change="onPhotoCapture" multiple style="display:none"
            />
            <span class="ma-photo-add-icon">📷</span>
            <span class="ma-photo-add-text">{{ t('m.addPhoto') }}</span>
          </label>
        </div>
      </el-form-item>

      <!-- 备注 -->
      <el-form-item :label="t('common.remark')">
        <el-input
          v-model="form.remark" type="textarea" :rows="3" maxlength="500"
          show-word-limit :placeholder="t('m.remarkPlaceholder')"
        />
      </el-form-item>

      <!-- 提交按钮 -->
      <button type="button" class="ma-submit" :disabled="saving" @click="onSubmit">
        {{ saving ? '⌛ ' + t('common.loading') : '✓ ' + t('common.save') }}
      </button>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { listPlantingPlans } from '@/api/plantingPlan'
import { createActivity } from '@/api/activity'
import { enqueue as enqueueOffline } from '@/utils/offlineQueue'
import { uploadFile } from '@/api/file'

const { t } = useI18n()
const router = useRouter()

const ACT_TYPES = [
  { value: 'sow',       emoji: '🌱' },
  { value: 'fertilize', emoji: '🧪' },
  { value: 'spray',     emoji: '💨' },
  { value: 'weed',      emoji: '🌾' },
  { value: 'water',     emoji: '💧' },
  { value: 'prune',     emoji: '✂️' },
  { value: 'other',     emoji: '⋯' },
]

// ----- form -----
const formRef = ref(null)
const saving = ref(false)
const todayIso = () => new Date().toISOString().slice(0, 10)
const form = reactive({
  planId: null,
  activityType: '',
  occurDate: todayIso(),
  locationGps: '',
  remark: '',
})
const rules = computed(() => ({
  planId:       [{ required: true, message: t('valid.required', { field: t('m.plan') }), trigger: 'change' }],
  activityType: [{ required: true, message: t('valid.required', { field: t('m.activityType') }), trigger: 'change' }],
  occurDate:    [{ required: true, message: t('valid.required', { field: t('m.date') }), trigger: 'change' }],
}))

// ----- plans -----
const plans = ref([])
async function loadPlans() {
  // 工人能记农事的计划: planned 或 in_progress (排除 draft/harvested/cancelled)
  // 先拉 in_progress, 再拉 planned, 合并去重
  try {
    const [running, planned] = await Promise.all([
      listPlantingPlans({ status: 'in_progress', page: 1, size: 200 }).catch(() => ({ list: [] })),
      listPlantingPlans({ status: 'planned',     page: 1, size: 200 }).catch(() => ({ list: [] })),
    ])
    const merged = [...(running?.list || []), ...(planned?.list || [])]
    plans.value = merged
    if (!plans.value.length) {
      console.warn('[MobileActivityNew] No active plans (in_progress/planned). Maybe demo-data.sql not loaded.')
    } else {
      console.log('[MobileActivityNew] loaded plans:', plans.value.length, '(in_progress + planned)')
    }
  } catch (e) {
    console.error('[MobileActivityNew] loadPlans failed:', e)
    plans.value = []
  }
}
onMounted(loadPlans)

// ----- GPS -----
const gpsLoading = ref(false)
const gpsAccuracy = ref(null)
function grabGps() {
  if (!navigator.geolocation) {
    ElMessage.warning(t('m.gpsUnsupported'))
    return
  }
  gpsLoading.value = true
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      const lat = pos.coords.latitude.toFixed(6)
      const lng = pos.coords.longitude.toFixed(6)
      form.locationGps = `${lat},${lng}`
      gpsAccuracy.value = pos.coords.accuracy
      gpsLoading.value = false
      ElMessage.success(t('m.gpsSuccess'))
    },
    (err) => {
      gpsLoading.value = false
      ElMessage.error(t('m.gpsFailed') + ': ' + err.message)
    },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 }
  )
}

// ----- 照片 (本地预览, 提交时上传) -----
const photos = ref([])  // { file, preview, fileId? }
function onPhotoCapture(e) {
  const files = Array.from(e.target.files || [])
  for (const f of files) {
    if (photos.value.length >= 6) break
    photos.value.push({
      file: f,
      preview: URL.createObjectURL(f),
    })
  }
  e.target.value = '' // 清空, 让同一文件可重选
}
function removePhoto(i) {
  URL.revokeObjectURL(photos.value[i].preview)
  photos.value.splice(i, 1)
}

// ----- 提交 -----
async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    // 1) 先上传照片拿 fileId
    const fileIds = []
    for (const p of photos.value) {
      try {
        const fileVo = await uploadFile(p.file, 'activity_photo')
        fileIds.push(fileVo.id)
      } catch (e) {
        console.warn('photo upload failed', e)
      }
    }
    // 2) 创建 activity
    await createActivity({
      ...form,
      photos: fileIds,
      clientUuid: generateUuid(),
    })
    ElMessage.success(t('m.submitOk'))
    router.push('/m/')
  } catch (e) {
    // 20.7: 失败时入 IndexedDB 待提交队列 (跳过照片上传, 整批入队保留原始 Blob)
    try {
      await enqueueOffline({
        kind: 'activity',
        form: { ...form, clientUuid: generateUuid() },
        photoFiles: photos.value.map(p => p.file),
      })
      ElMessage.warning(t('m.savedLocally'))
      router.push('/m/')
    } catch (qe) {
      console.error('[MobileActivityNew] enqueue failed:', qe)
      ElMessage.error(t('m.submitFail') + ' - ' + (e?.message || ''))
    }
  } finally {
    saving.value = false
  }
}

function generateUuid() {
  // 简易 UUID v4 (crypto 可用时优先)
  if (window.crypto?.randomUUID) return window.crypto.randomUUID()
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = Math.random() * 16 | 0
    return (c === 'x' ? r : (r & 0x3) | 0x8).toString(16)
  })
}

function goBack() { router.push('/m/') }
</script>

<style scoped>
.ma { display: flex; flex-direction: column; gap: 8px; }
.ma-head {
  display: flex; align-items: center; gap: 10px;
  margin-bottom: 6px;
}
.ma-back {
  width: 36px; height: 36px; border-radius: 50%;
  background: #fff; border: 1px solid #e4e7ed; font-size: 18px;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer;
}
.ma-head-title { font-weight: 700; font-size: 18px; }

:deep(.el-form-item__label) { font-size: 13px; font-weight: 600; color: #1f2329; }
:deep(.el-input__inner), :deep(.el-textarea__inner), :deep(.el-select__wrapper) { font-size: 15px; }

/* 类型大按钮 */
.ma-type-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  width: 100%;
}
.ma-type-btn {
  background: #fff;
  border: 2px solid #e4e7ed;
  border-radius: 10px;
  padding: 12px 4px;
  display: flex; flex-direction: column; align-items: center; gap: 4px;
  cursor: pointer;
  transition: all 0.15s;
}
.ma-type-btn:active { transform: scale(0.95); }
.ma-type-btn.active {
  border-color: #1f7a35;
  background: #ecf9ef;
  box-shadow: 0 2px 8px rgba(31, 122, 53, 0.15);
}
.ma-type-emoji { font-size: 22px; }
.ma-type-label { font-size: 11px; font-weight: 600; }

/* GPS */
.ma-gps { display: flex; gap: 8px; width: 100%; }
.ma-gps :deep(.el-input) { flex: 1; }
.ma-gps-btn {
  background: #fff5e6;
  border: 1px solid #f7c873;
  color: #b88230;
  padding: 0 14px;
  border-radius: 6px;
  font-size: 13px; font-weight: 600;
  cursor: pointer; white-space: nowrap;
}
.ma-gps-btn.loading { opacity: 0.6; }
.ma-gps-note { font-size: 11px; color: #909399; margin-top: 4px; }

/* 照片 */
.ma-photos {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  width: 100%;
}
.ma-photo {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px; overflow: hidden;
}
.ma-photo img { width: 100%; height: 100%; object-fit: cover; }
.ma-photo-x {
  position: absolute; top: 4px; right: 4px;
  width: 22px; height: 22px; border-radius: 50%;
  background: rgba(0,0,0,0.55); color: #fff;
  border: none; font-size: 16px; line-height: 1;
  cursor: pointer;
}
.ma-photo-add {
  aspect-ratio: 1;
  border: 2px dashed #b3d8be;
  border-radius: 8px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 4px; color: #1f7a35; cursor: pointer;
  background: #f7faf8;
}
.ma-photo-add-icon { font-size: 26px; }
.ma-photo-add-text { font-size: 11px; font-weight: 600; }

/* 提交 */
.ma-submit {
  background: linear-gradient(135deg, #1f7a35 0%, #15803d 100%);
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 14px;
  width: 100%;
  font-size: 17px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
}
.ma-submit:disabled { opacity: 0.6; }
.ma-submit:active { transform: scale(0.98); }
</style>
