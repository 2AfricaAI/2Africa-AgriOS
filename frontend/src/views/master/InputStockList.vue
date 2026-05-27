<template>
  <div class="page">
    <!-- Filters -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('inputStock.item')">
          <el-select v-model="query.inputItemId" :placeholder="t('common.all')" clearable filterable style="width: 200px">
            <el-option v-for="ii in inputItems" :key="ii.id" :value="ii.id"
              :label="`${ii.code} · ${ii.nameEn || ii.name}`" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inputStock.warehouse')">
          <el-select v-model="query.warehouseId" :placeholder="t('common.all')" clearable filterable style="width: 180px">
            <el-option v-for="w in warehouses" :key="w.id" :value="w.id"
              :label="`${w.code} · ${w.name}`" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inputItem.type')">
          <el-select v-model="query.inputType" :placeholder="t('common.all')" clearable style="width: 130px">
            <el-option v-for="t_ in INPUT_TYPES" :key="t_.value" :label="t(`inputItem.type${cap(t_.value)}`)" :value="t_.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.lowStockOnly">{{ t('inputStock.lowStockOnly') }}</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Table -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <span class="table-title">{{ t('inputStock.title') }}</span>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column prop="inputItemCode" :label="t('inputItem.code')" width="100" />
        <el-table-column prop="inputItemName" :label="t('inputItem.name')" min-width="160" show-overflow-tooltip />
        <el-table-column :label="t('inputItem.type')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTagColor(row.inputType)" size="small">
              {{ t(`inputItem.type${cap(row.inputType)}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="warehouseCode" :label="t('inputStock.whCode')" width="100" />
        <el-table-column prop="warehouseName" :label="t('inputStock.warehouse')" min-width="130" show-overflow-tooltip />
        <el-table-column :label="t('inputStock.onHand')" width="110" align="right">
          <template #default="{ row }">
            <strong>{{ fmtQty(row.qtyOnHand) }}</strong>
            <span class="unit">{{ row.unit }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('inputStock.reserved')" width="100" align="right">
          <template #default="{ row }">
            <span :class="{ dim: Number(row.qtyReserved) === 0 }">{{ fmtQty(row.qtyReserved) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('inputStock.available')" width="110" align="right">
          <template #default="{ row }">
            <strong :class="availClass(row)">{{ fmtQty(row.qtyAvailable) }}</strong>
            <span class="unit">{{ row.unit }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('inputStock.lastMove')" width="160" align="center">
          <template #default="{ row }">
            <span v-if="row.lastStockAt">{{ row.lastStockAt.replace('T', ' ').slice(0, 16) }}</span>
            <span v-else class="dim">-</span>
          </template>
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
import { listInputStock } from '@/api/inputStock'
import { listInputItems } from '@/api/inputItem'
import { listWarehouses } from '@/api/warehouse'

const { t } = useI18n()

const INPUT_TYPES = [
  { value: 'seed' }, { value: 'fertilizer' }, { value: 'pesticide' },
  { value: 'construction' }, { value: 'spare_parts' }, { value: 'tools' },
  { value: 'packaging' }, { value: 'other' },
]
const cap = (s) => {
  if (!s) return ''
  return s.split('_').map(p => p.charAt(0).toUpperCase() + p.slice(1)).join('')
}
const typeTagColor = (t_) => ({
  seed: 'warning', fertilizer: 'success', pesticide: 'danger',
  construction: 'info', spare_parts: 'info', tools: 'info',
  packaging: 'primary', other: 'info',
})[t_] || 'info'

function fmtQty(v) {
  if (v == null) return '0'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 3 })
}
function availClass(row) {
  const avail = Number(row.qtyAvailable || 0)
  if (avail <= 0) return 'stock-zero'
  return ''
}

// ----- query -----
const query = reactive({
  inputItemId: null, warehouseId: null, inputType: '', lowStockOnly: false,
  page: 1, size: 20,
})
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function reload(p) {
  if (p) query.page = p
  loading.value = true
  try {
    const data = await listInputStock(query)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}
function onReset() {
  Object.assign(query, { inputItemId: null, warehouseId: null, inputType: '', lowStockOnly: false, page: 1, size: 20 })
  reload()
}

// ----- dropdown data -----
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
.stock-zero { color: #f56c6c; font-weight: 700; }
</style>
