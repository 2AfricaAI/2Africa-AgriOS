<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('pack.batch')">
          <el-select v-model="query.batchId" :placeholder="t('common.all')" clearable filterable style="width: 220px">
            <el-option
              v-for="b in batches"
              :key="b.id"
              :label="`${b.code} · ${b.cropName || ''}`"
              :value="b.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('pack.grade')">
          <el-select v-model="query.grade" :placeholder="t('common.all')" clearable style="width: 110px">
            <el-option label="A" value="A" />
            <el-option label="B" value="B" />
            <el-option label="C" value="C" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="PlusIcon" @click="onCreate">{{ t('pack.new') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="code" :label="t('pack.code')" width="170" />
        <el-table-column :label="t('pack.batch')" min-width="200">
          <template #default="{ row }">
            <code class="code">{{ row.batchCode }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('pack.crop')" prop="cropName" min-width="100" />
        <el-table-column :label="t('pack.grade')" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="gradeTag(row.grade)" effect="dark">{{ row.grade }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('pack.spec')" min-width="170">
          <template #default="{ row }">
            <span>{{ row.specName }}</span>
            <span class="dim" style="margin-left: 4px">{{ row.specCode }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('pack.qtyUnits')" prop="qtyUnits" width="90" align="right">
          <template #default="{ row }"><strong>{{ row.qtyUnits }}</strong></template>
        </el-table-column>
        <el-table-column :label="t('pack.netWeightKg')" width="110" align="right">
          <template #default="{ row }">{{ formatKg(row.netWeightKg) }}</template>
        </el-table-column>
        <el-table-column :label="t('pack.location')" min-width="150">
          <template #default="{ row }">
            <el-tag size="small">{{ row.locationName }}</el-tag>
            <span class="dim" style="margin-left: 4px">{{ row.locationCode }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('pack.completedAt')" prop="packedAt" width="170" />
        <el-table-column :label="t('pack.operator')" prop="operatorName" width="100" />
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
      :title="t('pack.new')"
      width="640"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="t('pack.sourceBatch')" prop="batchId">
          <el-select v-model="form.batchId" :placeholder="t('pack.selectBatch')" filterable style="width: 100%" @change="onBatchChange">
            <el-option
              v-for="b in availableBatches"
              :key="b.id"
              :label="t('pack.batchOptionLabel', { code: b.code, crop: b.cropName, variety: b.varietyName || '', remain: formatKg(b.qtyRemainKg) })"
              :value="b.id"
            />
          </el-select>
          <div v-if="selectedBatch" class="hint">
            {{ t('pack.batchRemain', { remain: formatKg(selectedBatch.qtyRemainKg), total: formatKg(selectedBatch.qtyKg) }) }}
            · {{ t('pack.batchStatus') }} <el-tag size="small">{{ selectedBatch.status }}</el-tag>
          </div>
        </el-form-item>

        <el-form-item :label="t('pack.grade')" prop="grade">
          <el-radio-group v-model="form.grade">
            <el-radio-button label="A">{{ t('pack.gradeA') }}</el-radio-button>
            <el-radio-button label="B">{{ t('pack.gradeB') }}</el-radio-button>
            <el-radio-button label="C">{{ t('pack.gradeC') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item :label="t('pack.spec')" prop="specId">
          <el-select v-model="form.specId" :placeholder="t('pack.selectSpec')" filterable style="width: 100%">
            <el-option
              v-for="s in specs"
              :key="s.id"
              :label="t('pack.specOptionLabel', { name: s.name, code: s.code, netKg: Number(s.unitNetKg).toFixed(3) })"
              :value="s.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('pack.qtyUnits')" prop="qtyUnits">
          <el-input-number v-model="form.qtyUnits" :min="1" :step="10" style="width: 200px" />
          <span v-if="autoNetKg" class="dim" style="margin-left: 12px">
            {{ t('pack.autoNetKg') }} <strong>{{ formatKg(autoNetKg) }}</strong> kg
          </span>
        </el-form-item>

        <el-form-item :label="t('pack.inLocation')" prop="locationId">
          <el-select v-model="form.locationId" :placeholder="t('pack.selectLocation')" filterable style="width: 100%">
            <el-option
              v-for="w in warehouses"
              :key="w.id"
              :label="t('pack.locationOptionLabel', { name: w.name, code: w.code, type: w.type })"
              :value="w.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('pack.packedAt')" prop="packedAt">
          <el-date-picker
            v-model="form.packedAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            :placeholder="t('pack.pickDateTime')"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item :label="t('pack.remark')">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="onSubmit">
          {{ t('pack.saveAction') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Plus as PlusIcon,
} from '@element-plus/icons-vue'
import { listPackings, createPacking } from '@/api/packing'
import { listBatches } from '@/api/batch'
import { listPackagingSpecs } from '@/api/packagingSpec'
import { listWarehouses } from '@/api/warehouse'

const { t } = useI18n()

function gradeTag(g) {
  return g === 'A' ? 'success' : g === 'B' ? 'warning' : 'info'
}
function formatKg(v) {
  if (v == null) return '0'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 3 })
}

// 字典 / 关联数据
const batches = ref([])
const specs = ref([])
const warehouses = ref([])

const availableBatches = computed(() =>
  batches.value.filter(b => Number(b.qtyRemainKg) > 0 && b.status !== 'sold_out' && b.status !== 'lost')
)

async function loadRelated() {
  const [bData, sData, wData] = await Promise.all([
    listBatches({ page: 1, size: 500 }),
    listPackagingSpecs({ page: 1, size: 200, status: 1 }),
    listWarehouses({ page: 1, size: 200, status: 1 }),
  ])
  batches.value = bData.list
  specs.value = sData.list
  warehouses.value = wData.list
}

// 列表
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const query = reactive({ batchId: null, grade: '' })

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await listPackings({ ...query, page: page.value, size: pageSize.value })
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}
function onReset() { query.batchId = null; query.grade = ''; reload(1) }
function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }

onMounted(async () => { await loadRelated(); await reload(1) })

// 对话框
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref(null)

const emptyForm = () => ({
  batchId: null,
  grade: 'A',
  specId: null,
  qtyUnits: 1,
  locationId: null,
  packedAt: new Date().toISOString().slice(0, 19),
  remark: '',
})
const form = reactive(emptyForm())

const rules = computed(() => ({
  batchId:    [{ required: true, message: t('pack.pleaseSelectBatch'), trigger: 'change' }],
  grade:      [{ required: true, message: t('pack.pleaseSelectGrade'), trigger: 'change' }],
  specId:     [{ required: true, message: t('pack.pleaseSelectSpec'), trigger: 'change' }],
  qtyUnits:   [{ required: true, message: t('pack.pleaseEnterUnits'), trigger: 'change' }],
  locationId: [{ required: true, message: t('pack.pleaseSelectLocation'), trigger: 'change' }],
  packedAt:   [{ required: true, message: t('pack.pleaseSelectTime'), trigger: 'change' }],
}))

const selectedBatch = computed(() => batches.value.find(b => b.id === form.batchId))
const selectedSpec  = computed(() => specs.value.find(s => s.id === form.specId))
const autoNetKg = computed(() => {
  if (!selectedSpec.value || !form.qtyUnits) return null
  return Number(selectedSpec.value.unitNetKg) * form.qtyUnits
})

function onBatchChange() {
  // 切换批次时清空规格(可能要重新选)
}

function onCreate() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}
function onDialogClosed() {
  Object.assign(form, emptyForm())
  formRef.value?.clearValidate()
}

async function onSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await createPacking(form)
    ElMessage.success(t('pack.createSuccess'))
    dialogVisible.value = false
    await loadRelated()  // 重新拉批次(余量变了)
    reload()
  } catch {}
  finally { saving.value = false }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) { padding: 16px; }
.toolbar { margin-bottom: 12px; display: flex; justify-content: flex-end; }
.pager { margin-top: 14px; justify-content: flex-end; }
.dim { color: #909399; font-size: 12px; }
.hint { font-size: 12px; color: #606266; margin-top: 6px; }
.code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #d63384;
  font-size: 12px;
}
</style>
