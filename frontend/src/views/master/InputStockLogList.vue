<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('stockLog.item')">
          <el-select v-model="query.inputItemId" :placeholder="t('common.all')" clearable filterable style="width: 200px">
            <el-option v-for="ii in inputItems" :key="ii.id" :value="ii.id"
              :label="`${ii.code} · ${ii.nameEn || ii.name}`" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('stockLog.warehouse')">
          <el-select v-model="query.warehouseId" :placeholder="t('common.all')" clearable filterable style="width: 180px">
            <el-option v-for="w in warehouses" :key="w.id" :value="w.id"
              :label="`${w.code} · ${w.name}`" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('stockLog.direction')">
          <el-select v-model="query.direction" :placeholder="t('common.all')" clearable style="width: 100px">
            <el-option label="IN" value="IN" />
            <el-option label="OUT" value="OUT" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('stockLog.reason')">
          <el-select v-model="query.reasonType" :placeholder="t('common.all')" clearable style="width: 160px">
            <el-option v-for="r in REASON_TYPES" :key="r" :label="t(`stockLog.reason_${r}`)" :value="r" />
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
        <span class="table-title">{{ t('stockLog.title') }}</span>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column :label="t('stockLog.time')" width="150" align="center">
          <template #default="{ row }">
            {{ row.createdAt ? row.createdAt.replace('T', ' ').slice(0, 16) : '-' }}
          </template>
        </el-table-column>
        <el-table-column :label="t('stockLog.direction')" width="70" align="center">
          <template #default="{ row }">
            <el-tag :type="row.direction === 'IN' ? 'success' : 'danger'" size="small" effect="dark">
              {{ row.direction }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="inputItemCode" :label="t('inputItem.code')" width="90" />
        <el-table-column prop="inputItemName" :label="t('inputItem.name')" min-width="140" show-overflow-tooltip />
        <el-table-column :label="t('stockLog.qty')" width="110" align="right">
          <template #default="{ row }">
            <strong :class="row.direction === 'IN' ? 'text-green' : 'text-red'">
              {{ row.direction === 'IN' ? '+' : '-' }}{{ fmtQty(row.qty) }}
            </strong>
            <span class="unit">{{ row.unit }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('stockLog.afterQty')" width="100" align="right">
          <template #default="{ row }">{{ fmtQty(row.qtyAfter) }}</template>
        </el-table-column>
        <el-table-column prop="warehouseCode" :label="t('stockLog.whCode')" width="90" />
        <el-table-column prop="warehouseName" :label="t('stockLog.warehouse')" min-width="120" show-overflow-tooltip />
        <el-table-column :label="t('stockLog.reason')" width="140">
          <template #default="{ row }">
            <el-tag size="small">{{ t(`stockLog.reason_${row.reasonType}`, row.reasonType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('stockLog.ref')" width="140">
          <template #default="{ row }">
            <span v-if="row.referenceType">{{ row.referenceType }}#{{ row.referenceId }}</span>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" :label="t('stockLog.operator')" width="100">
          <template #default="{ row }">{{ row.operatorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" :label="t('common.remark')" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background layout="total, sizes, prev, pager, next, jumper"
          :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
          :page-sizes="[20, 50, 100]"
          @current-change="() => reload()" @size-change="() => reload(1)"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { Search as SearchIcon, Refresh as RefreshIcon } from '@element-plus/icons-vue'
import { listStockLog } from '@/api/inputStockLog'
import { listInputItems } from '@/api/inputItem'
import { listWarehouses } from '@/api/warehouse'

const { t } = useI18n()

const REASON_TYPES = [
  'po_receive', 'activity_consume', 'stocktake_adjust',
  'damage', 'return_in', 'transfer_in', 'transfer_out', 'manual',
]

function fmtQty(v) {
  if (v == null) return '0'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 3 })
}

const query = reactive({
  inputItemId: null, warehouseId: null, direction: '', reasonType: '',
  page: 1, size: 20,
})
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function reload(p) {
  if (p) query.page = p
  loading.value = true
  try {
    const data = await listStockLog(query)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}
function onReset() {
  Object.assign(query, { inputItemId: null, warehouseId: null, direction: '', reasonType: '', page: 1, size: 20 })
  reload()
}

const inputItems = ref([])
const warehouses = ref([])
async function loadDropdowns() {
  try {
    const [iiData, whData] = await Promise.all([
      listInputItems({ status: 'active', page: 1, size: 500 }),
      listWarehouses({ status: 1, page: 1, size: 200 }),
    ])
    inputItems.value = iiData.list || []
    warehouses.value = whData.list || []
  } catch {/* ignore */}
}

onMounted(() => { loadDropdowns(); reload() })
</script>

<style scoped>
.page { padding: 16px; }
.filter-card { margin-bottom: 12px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.table-title { font-size: 16px; font-weight: 600; color: #1f2937; }
.pager { margin-top: 12px; text-align: right; }
.dim { color: #909399; }
.unit { color: #909399; font-size: 11px; margin-left: 2px; }
.text-green { color: #16a34a; }
.text-red { color: #ef4444; }
</style>
