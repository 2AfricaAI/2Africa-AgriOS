<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('inv.location')">
          <el-select v-model="query.locationId" :placeholder="t('common.all')" clearable filterable style="width: 200px">
            <el-option
              v-for="w in warehouses"
              :key="w.id"
              :label="`${w.name} (${w.code})`"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inv.grade')">
          <el-select v-model="query.grade" :placeholder="t('common.all')" clearable style="width: 110px">
            <el-option label="A" value="A" />
            <el-option label="B" value="B" />
            <el-option label="C" value="C" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inv.status')">
          <el-select v-model="query.status" :placeholder="t('common.all')" clearable style="width: 130px">
            <el-option v-for="s in statusOptions" :key="s.value" :label="s.label" :value="s.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="query.nearExpiryOnly">{{ t('inv.nearExpiryOnly') }}</el-checkbox>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="reload(1)">{{ t('common.search') }}</el-button>
          <el-button :icon="RefreshIcon" @click="onReset">{{ t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column :label="t('inv.sku')" min-width="220">
          <template #default="{ row }">
            <code class="code">{{ row.skuCode }}</code>
            <div class="dim" style="margin-top: 2px">{{ row.skuName }}</div>
          </template>
        </el-table-column>
        <el-table-column :label="t('inv.cropVariety')" min-width="160">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.cropName }}</el-tag>
            <span v-if="row.varietyName" class="dim" style="margin-left: 4px">{{ row.varietyName }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('inv.grade')" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="gradeTag(row.grade)" effect="dark">{{ row.grade }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('inv.batch')" min-width="180">
          <template #default="{ row }"><code class="code">{{ row.batchCode }}</code></template>
        </el-table-column>
        <el-table-column :label="t('inv.location')" min-width="160">
          <template #default="{ row }">
            <el-tag size="small">{{ row.locationName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('inv.qtyAvail')" prop="qtyAvail" width="100" align="right">
          <template #default="{ row }"><strong style="color: #52c41a">{{ Number(row.qtyAvail).toLocaleString() }}</strong></template>
        </el-table-column>
        <el-table-column :label="t('inv.qtyLocked')" prop="qtyLocked" width="100" align="right">
          <template #default="{ row }"><span style="color: #fa8c16">{{ Number(row.qtyLocked).toLocaleString() }}</span></template>
        </el-table-column>
        <el-table-column :label="t('inv.qtyInTransit')" prop="qtyInTransit" width="100" align="right">
          <template #default="{ row }"><span style="color: #1890ff">{{ Number(row.qtyInTransit).toLocaleString() }}</span></template>
        </el-table-column>
        <el-table-column :label="t('inv.unit')" prop="unit" width="70" align="center" />
        <el-table-column :label="t('inv.prodDate')" prop="prodDate" width="110" />
        <el-table-column :label="t('inv.expiryDate')" prop="expiryDate" width="160">
          <template #default="{ row }">
            <div v-if="row.expiryDate">
              <span>{{ row.expiryDate }}</span>
              <el-tag
                v-if="row.daysToExpiry != null"
                :type="expiryTag(row.daysToExpiry)"
                size="small"
                effect="dark"
                style="margin-left: 4px"
              >
                {{ expiryLabel(row.daysToExpiry) }}
              </el-tag>
            </div>
            <span v-else class="dim small">-</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('inv.status')" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('inv.lastChangeAt')" prop="lastOpAt" width="170" />
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
} from '@element-plus/icons-vue'
import { listInventory } from '@/api/inventory'
import { listWarehouses } from '@/api/warehouse'

const { t } = useI18n()

function gradeTag(g) { return g === 'A' ? 'success' : g === 'B' ? 'warning' : 'info' }

// Sprint 26 / FEFO — days-to-expiry color + label
function expiryTag(d) {
  if (d < 0) return 'danger'        // already expired
  if (d <= 3) return 'danger'        // critical
  if (d <= 7) return 'warning'       // warning
  return 'success'                   // healthy
}
function expiryLabel(d) {
  if (d < 0)  return t('inv.expiredDays',   { n: Math.abs(d) })
  if (d === 0) return t('inv.expiresToday')
  return t('inv.daysLeft', { n: d })
}

function statusLabel(s) {
  return {
    normal: t('status.normal'),
    frozen: t('status.frozen'),
    lost: t('status.lost'),
  }[s] || s
}
function statusTag(s) {
  return { normal: 'success', frozen: 'info', lost: 'danger' }[s] || 'info'
}

const statusOptions = computed(() => [
  { value: 'normal', label: t('status.normal') },
  { value: 'frozen', label: t('status.frozen') },
  { value: 'lost',   label: t('status.lost') },
])

const warehouses = ref([])
async function loadWarehouses() {
  const data = await listWarehouses({ page: 1, size: 200 })
  warehouses.value = data.list
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const query = reactive({ locationId: null, grade: '', status: '', nearExpiryOnly: false })

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    // Backend doesn't yet filter on near-expiry — do it client-side after fetch.
    const data = await listInventory({
      locationId: query.locationId,
      grade: query.grade,
      status: query.status,
      page: page.value,
      size: pageSize.value,
    })
    let rows = data.list
    if (query.nearExpiryOnly) {
      rows = rows.filter(r => r.daysToExpiry != null && r.daysToExpiry <= 3)
    }
    list.value = rows
    total.value = data.total
  } finally { loading.value = false }
}
function onReset() {
  query.locationId = null
  query.grade = ''
  query.status = ''
  query.nearExpiryOnly = false
  reload(1)
}
function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }

onMounted(async () => { await loadWarehouses(); await reload(1) })
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) { padding: 16px; }
.pager { margin-top: 14px; justify-content: flex-end; }
.dim { color: #909399; font-size: 12px; }
.code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #d63384;
  font-size: 12px;
}
</style>
