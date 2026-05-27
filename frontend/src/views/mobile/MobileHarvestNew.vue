<template>
  <div class="mh2-wrap">
    <header class="mh2-header">
      <button class="mh2-back" @click="router.back()">←</button>
      <h2>{{ t('m.recordHarvest') }}</h2>
    </header>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
      <!-- Planting plan -->
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

      <!-- Harvest date -->
      <el-form-item :label="t('m.harvestDate')" prop="harvestDate">
        <el-date-picker
          v-model="form.harvestDate" type="date" value-format="YYYY-MM-DD"
          size="large" style="width: 100%" :clearable="false"
        />
      </el-form-item>

      <!-- Quantity kg (BIG number input, mobile friendly) -->
      <el-form-item :label="t('m.qtyKg')" prop="qtyKg">
        <el-input
          v-model.number="form.qtyKg" type="number" step="0.5" min="0.001"
          size="large" :placeholder="t('m.qtyPlaceholder')" class="mh2-qty"
        >
          <template #append>kg</template>
        </el-input>
      </el-form-item>

      <!-- GPS -->
      <el-form-item :label="t('m.gps')">
        <div class="mh2-gps">
          <el-input
            v-model="form.locationGps" size="large"
            :placeholder="t('m.gpsPlaceholder')" :readonly="gpsLoading"
          />
          <button type="button" class="mh2-gps-btn" :class="{ loading: gpsLoading }" @click="grabGps">
            {{ gpsLoading ? '⌛' : '📍' }} {{ t('m.locate') }}
          </button>
        </div>
        <div v-if="gpsAccuracy" class="mh2-gps-note">
          {{ t('m.gpsAccuracy', { m: Math.round(gpsAccuracy) }) }}
        </div>
      </el-form-item>

      <!-- Photos -->
      <el-form-item :label="t('m.photos')">
        <div class="mh2-photo-grid">
          <div v-for="(p, i) in photos" :key="i" class="mh2-photo-cell">
            <img :src="p.preview" class="mh2-photo-img" />
            <button class="mh2-photo-x" type="button" @click="removePhoto(i)">×</button>
          </div>
          <label v-if="photos.length < 6" class="mh2-photo-add">
            <input type="file" accept="image/*" capture="environment" multiple
                   class="mh2-photo-input" @change="onPhotoCapture" />
            <span class="mh2-photo-icon">📷</span>
            <span class="mh2-photo-label">{{ t('m.addPhoto') }}</span>
          </label>
        </div>
      </el-form-item>

      <!-- Remark -->
      <el-form-item :label="t('common.remark')">
        <el-input
          v-model="form.remark" type="textarea" :rows="3" maxlength="255"
          show-word-limit :placeholder="t('m.remarkPlaceholder')"
        />
      </el-form-item>

      <button type="button" class="mh2-submit" :disabled="saving" @click="onSubmit">
        {{ saving ? t('common.loading') : t('common.submit') }}
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
import { createHarvest } from '@/api/harvest'
import { uploadFile } from '@/api/file'
import { enqueue as enqueueOffline } from '@/utils/offlineQueue'

const { t } = useI18n()
const router = useRouter()

// ----- form -----
const formRef = ref(null)
const saving = ref(false)
const todayIso = () => new Date().toISOString().slice(0, 10)
const form = reactive({
  planId: null,
  harvestDate: todayIso(),
  qtyKg: null,
  locationGps: '',
  remark: '',
})
const rules = computed(() => ({
  planId:      [{ required: true, message: t('valid.required', { field: t('m.plan') }), trigger: 'change' }],
  harvestDate: [{ required: true, message: t('valid.required', { field: t('m.harvestDate') }), trigger: 'change' }],
  qtyKg: [
    { required: true, message: t('valid.required', { field: t('m.qtyKg') }), trigger: 'blur' },
    { validator: (_, v, cb) => {
        const n = Number(v)
        if (Number.isFinite(n) && n > 0) cb()
        else cb(new Error(t('valid.mustBePositive', { field: t('m.qtyKg') })))
      }, trigger: 'blur' },
  ],
}))

// ----- plans -----
const plans = ref([])
async function loadPlans() {
  try {
    const [running, planned] = await Promise.all([
      listPlantingPlans({ status: 'in_progress', page: 1, size: 200 }).catch(() => ({ list: [] })),
      listPlantingPlans({ status: 'planned',     page: 1, size: 200 }).catch(() => ({ list: [] })),
    ])
    plans.value = [...(running?.list || []), ...(planned?.list || [])]
    if (!plans.value.length) {
      console.warn('[MobileHarvestNew] No active plans (in_progress/planned).')
    } else {
      console.log('[MobileHarvestNew] loaded plans:', plans.value.length)
    }
  } catch (e) {
    console.error('[MobileHarvestNew] loadPlans failed:', e)
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
      ElMessage.success(t('m.gpsSuccess', { m: Math.round(pos.coords.accuracy) }))
    },
    (err) => {
      gpsLoading.value = false
      ElMessage.error(t('m.gpsFailed') + ': ' + err.message)
    },
    { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 }
  )
}

// ----- photos -----
const photos = ref([])
function onPhotoCapture(e) {
  const files = Array.from(e.target.files || [])
  for (const f of files) {
    if (photos.value.length >= 6) break
    photos.value.push({ file: f, preview: URL.createObjectURL(f) })
  }
  e.target.value = ''
}
function removePhoto(i) {
  URL.revokeObjectURL(photos.value[i].preview)
  photos.value.splice(i, 1)
}

// ----- submit -----
function generateUuid() {
  if (window.crypto?.randomUUID) return window.crypto.randomUUID()
  return 'hv-' + Date.now() + '-' + Math.random().toString(36).slice(2, 10)
}

async function onSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    // 1) upload photos serially (each gets fileId)
    const fileIds = []
    for (const p of photos.value) {
      const res = await uploadFile(p.file, 'harvest_photo')
      if (res?.id) fileIds.push(res.id)
    }
    // 2) create harvest record
    await createHarvest({
      ...form,
      qtyKg: Number(form.qtyKg),
      photos: fileIds,
      clientUuid: generateUuid(),
    })
    ElMessage.success(t('m.submitOk'))
    router.replace('/m/')
  } catch (e) {
    console.error('[MobileHarvestNew] submit failed, queueing offline:', e)
    try {
      await enqueueOffline({
        kind: 'harvest',
        form: {
          ...form,
          qtyKg: Number(form.qtyKg),
          clientUuid: generateUuid(),
        },
        photoFiles: photos.value.map(p => p.file),
      })
      ElMessage.warning(t('m.savedLocally'))
      router.replace('/m/')
    } catch (qe) {
      console.error('[MobileHarvestNew] enqueue failed:', qe)
      ElMessage.error(t('m.submitFail') + ' - ' + (e?.message || ''))
    }
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.mh2-wrap {
  padding: 16px 16px 20px;
  background: #f5f7fa;
  min-height: 100%;
}

/* header */
.mh2-header {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 16px;
}
.mh2-header h2 { font-size: 20px; font-weight: 700; color: #1f2937; margin: 0; }
.mh2-back {
  width: 40px; height: 40px;
  border-radius: 50%; background: #fff;
  border: 1px solid #e4e7ed;
  font-size: 20px; cursor: pointer;
  display: flex; align-items: center; justify-content: center;
}

/* qty input: big number style for thumb-friendly entry */
:deep(.mh2-qty .el-input__inner) {
  font-size: 22px;
  font-weight: 600;
  text-align: right;
}
:deep(.mh2-qty .el-input-group__append) {
  font-size: 14px; font-weight: 500;
  color: #1f7a35;
  background: #ecfdf5;
  border-color: #e4e7ed;
}

/* GPS */
.mh2-gps { display: flex; gap: 8px; width: 100%; }
.mh2-gps :deep(.el-input) { flex: 1; }
.mh2-gps-btn {
  white-space: nowrap;
  padding: 0 14px;
  border: 1px solid #e4e7ed;
  background: #fff7eb;
  color: #c46500;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}
.mh2-gps-btn.loading { opacity: 0.6; }
.mh2-gps-note { font-size: 11px; color: #909399; margin-top: 4px; }

/* photos */
.mh2-photo-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  width: 100%;
}
.mh2-photo-cell, .mh2-photo-add {
  aspect-ratio: 1 / 1;
  border-radius: 6px;
  background: #f0fdf4;
  border: 2px dashed #86efac;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
  cursor: pointer;
}
.mh2-photo-cell { border-style: solid; border-color: #d1fae5; }
.mh2-photo-img { width: 100%; height: 100%; object-fit: cover; }
.mh2-photo-x {
  position: absolute; top: 4px; right: 4px;
  width: 22px; height: 22px; border-radius: 50%;
  background: rgba(0, 0, 0, 0.6); color: #fff;
  border: none; font-size: 16px; line-height: 1;
  cursor: pointer;
}
.mh2-photo-input { position: absolute; opacity: 0; pointer-events: none; }
.mh2-photo-icon { font-size: 28px; }
.mh2-photo-label { font-size: 11px; color: #16a34a; margin-top: 2px; }

/* submit */
.mh2-submit {
  wid