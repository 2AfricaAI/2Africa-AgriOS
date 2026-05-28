<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('harvest.plan')">
          <el-select v-model="query.planId" :placeholder="t('common.all')" clearable filterable style="width: 240px">
            <el-option
              v-for="p in plans"
              :key="p.id"
              :label="`${p.code} · ${p.plotName} · ${p.cropName}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('harvest.date')">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            :range-separator="t('date.rangeSep')"
            :start-placeholder="t('date.start')"
            :end-placeholder="t('date.end')"
            value-format="YYYY-MM-DD"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">{{ t('harvest.new') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="code" :label="t('harvest.code')" width="160" />
        <el-table-column prop="harvestDate" :label="t('harvest.date')" width="110" />
        <el-table-column :label="t('harvest.relation')" min-width="220">
          <template #default="{ row }">
            <div class="dim" style="margin-bottom: 2px">{{ row.planCode }}</div>
            <el-tag size="small">{{ row.plotName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('harvest.cropVariety')" min-width="160">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.cropName }}</el-tag>
            <span v-if="row.varietyName" class="dim" style="margin-left: 4px">{{ row.varietyName }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('harvest.qtyKg')" width="120" align="right">
          <template #default="{ row }">
            <strong>{{ Number(row.qtyKg).toLocaleString(undefined, { minimumFractionDigits: 2 }) }}</strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('harvest.batchAuto')" min-width="170">
          <template #default="{ row }">
            <code class="batch-code">{{ row.batchCode }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('harvest.operator')" width="110">
          <template #default="{ row }">
            <span v-if="row.operatorName">{{ row.operatorName }}</span>
            <span v-else class="dim">#{{ row.operatorId }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('activity.photos')" width="140">
          <template #default="{ row }">
            <div v-if="row.photos?.length" class="photo-row">
              <el-image
                v-for="(p, i) in row.photos.slice(0, 3)"
                :key="p.id"
                :src="p.downloadUrl"
                :preview-src-list="row.photos.map(x => x.downloadUrl)"
                :initial-index="i"
                fit="cover"
                class="thumb"
                preview-teleported
              />
              <span v-if="row.photos.length > 3" class="more">+{{ row.photos.length - 3 }}</span>
            </div>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" :label="t('common.remark')" min-width="160" show-overflow-tooltip />
        <el-table-column :label="t('common.actions')" width="100" fixed="right" align="center">
          <template #default="{ row }">
            <el-popconfirm :title="t('harvest.confirmDelete')" @confirm="onDelete(row)">
              <template #reference>
                <el-button link type="danger" size="small">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @size-change="onSizeChange"
        @current-change="onPageChange"
      />
    </el-card>

    <!-- 新建对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="t('harvest.createTitle')"
      width="640"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('harvest.plan')" prop="planId">
          <el-select v-model="form.planId" :placeholder="t('harvest.planPlaceholder')" filterable style="width: 100%">
            <el-option
              v-for="p in plans"
              :key="p.id"
              :label="`${p.code} · ${p.plotName} · ${p.cropName}`"
              :value="p.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('harvest.date')" prop="harvestDate">
          <el-date-picker
            v-model="form.harvestDate"
            type="date"
            value-format="YYYY-MM-DD"
            :placeholder="t('harvest.datePlaceholder')"
            style="width: 100%"
          />
        </el-form-item>

        <!-- Sprint 23 / Phase 5: PHI banner -->
        <el-alert
          v-if="phi.blocked"
          :title="t('phi.blockedTitle', { date: phi.earliestSafeDate, days: phi.daysRemaining })"
          type="error"
          :closable="false"
          show-icon
          style="margin: 0 0 16px 100px; width: calc(100% - 100px)"
        >
          <template #default>
            <div style="font-size: 12px; line-height: 1.5">
              {{ t('phi.blockedDetail', {
                code: phi.blockingSprays?.[0]?.inputItemCode,
                ingredient: phi.blockingSprays?.[0]?.activeIngredient || '-',
                phiDays: phi.blockingSprays?.[0]?.phiDays,
                sprayDate: phi.blockingSprays?.[0]?.sprayDate
              }) }}
            </div>
          </template>
        </el-alert>
        <el-alert
          v-else-if="phi.earliestSafeDate"
          :title="t('phi.safeTitle', { date: phi.earliestSafeDate })"
          type="success"
          :closable="false"
          show-icon
          style="margin: 0 0 16px 100px; width: calc(100% - 100px)"
        />

        <el-form-item :label="t('harvest.qty')" prop="qtyKg">
          <el-input-number
            v-model="form.qtyKg"
            :min="0.001"
            :precision="3"
            :step="10"
            controls-position="right"
            style="width: 100%"
          />
          <span class="unit">{{ t('units.kg') }}</span>
        </el-form-item>
        <el-form-item :label="t('common.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('activity.photos')">
          <FileUploader
            v-model="photos"
            biz-type="harvest_photo"
            accept="image/*"
            :limit="9"
            :max-size-mb="10"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">
          {{ t('harvest.saveAndBatch') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Plus as PlusIcon,
} from '@element-plus/icons-vue'
import { listHarvests, createHarvest, deleteHarvest } from '@/api/harvest'
import { listPlantingPlans } from '@/api/plantingPlan'
import { checkPhi } from '@/api/phi'
import FileUploader from '@/components/FileUploader.vue'

const { t } = useI18n()

const plans = ref([])
async function loadPlans() {
  const data = await listPlantingPlans({ page: 1, size: 500 })
  plans.value = data.list
}

// list
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const query = reactive({ planId: null, dateFrom: null, dateTo: null })

const dateRange = ref(null)
watch(dateRange, (v) => {
  if (Array.isArray(v) && v.length === 2) {
    query.dateFrom = v[0]; query.dateTo = v[1]
  } else { query.dateFrom = null; query.dateTo = null }
})

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await listHarvests({ ...query, page: page.value, size: pageSize.value })
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}
function onReset() {
  query.planId = null
  dateRange.value = null
  reload(1)
}
function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }

onMounted(async () => { await loadPlans(); await reload(1) })

// dialog
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)
const photos = ref([])

const emptyForm = () => ({ planId: null, harvestDate: null, qtyKg: null, remark: '' })
const form = reactive(emptyForm())

// Sprint 23: PHI 实时检查
const phi = reactive({ blocked: false, earliestSafeDate: null, daysRemaining: 0, blockingSprays: [] })
let phiTimer = null
async function runPhiCheck() {
  if (!form.planId || !form.harvestDate) {
    Object.assign(phi, { blocked: false, earliestSafeDate: null, daysRemaining: 0, blockingSprays: [] })
    return
  }
  try {
    const data = await checkPhi(form.planId, form.harvestDate)
    Object.assign(phi, data)
  } catch (e) { console.warn('[PHI]', e) }
}
watch(() => [form.planId, form.harvestDate], () => {
  if (phiTimer) clearTimeout(phiTimer)
  phiTimer = setTimeout(runPhiCheck, 200)
}, { flush: 'post' })

const rules = computed(() => ({
  planId:      [{ required: true, message: t('harvest.pickPlan'), trigger: 'change' }],
  harvestDate: [{ required: true, message: t('harvest.pickDate'), trigger: 'change' }],
  qtyKg:       [{ required: true, message: t('harvest.pickQty'), trigger: 'change' }],
}))

function onCreate() {
  Object.assign(form, emptyForm())
  photos.value = []
  dialogVisible.value = true
}
function onDialogClosed() {
  Object.assign(form, emptyForm())
  photos.value = []
  formRef.value?.clearValidate()
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload = {
      ...form,
      photos: photos.value.map(p => p.id).filter(Boolean),
      clientUuid: crypto.randomUUID(),
    }
    await createHarvest(payload)
    ElMessage.success(t('harvest.created'))
    dialogVisible.value = false
    reload()
  } catch {}
  finally { saving.value = false }
}

async function onDelete(row) {
  try {
    await deleteHarvest(row.id)
    ElMessage.success(t('common.deleteSuccess'))
    reload()
  } catch {}
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) { padding: 16px; }
.toolbar { margin-bottom: 12px; display: flex; justify-content: flex-end; }
.pager { margin-top: 14px; justify-content: flex-end; }
.dim { color: #909399; font-size: 12px; }
.unit { margin-left: 8px; color: #606266; font-size: 13px; }

.batch-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #d63384;
  font-size: 12px;
}

.photo-row { display: flex; align-items: center; gap: 4px; }
.thumb {
  width: 36px; height: 36px;
  border-radius: 4px; cursor: pointer;
  border: 1px solid #ebeef5;
}
.more { font-size: 11px; color: #909399; margin-left: 4px; }
</style>
