<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('batch.code')">
          <el-input v-model="query.code" :placeholder="t('batch.codePlaceholder')" clearable style="width: 220px" />
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-select v-model="query.status" :placeholder="t('common.all')" clearable style="width: 130px">
            <el-option
              v-for="s in STATUS_OPTIONS"
              :key="s.value"
              :label="s.label"
              :value="s.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('batch.harvestDate')">
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
      <el-table :data="list" v-loading="loading" border stripe row-key="id">
        <el-table-column :label="t('batch.code')" min-width="200">
          <template #default="{ row }">
            <router-link :to="`/production/batches/${row.id}`" class="link-batch">
              <code class="batch-code">{{ row.code }}</code>
            </router-link>
            <div v-if="row.parentBatchCode" class="dim" style="margin-top: 2px">
              {{ t('batch.splitFrom', { code: row.parentBatchCode }) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="harvestDate" :label="t('batch.harvestDate')" width="110" />
        <el-table-column :label="t('batch.relation')" min-width="200">
          <template #default="{ row }">
            <div class="dim" style="margin-bottom: 2px">{{ row.planCode }}</div>
            <el-tag size="small">{{ row.plotName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('batch.cropVariety')" min-width="160">
          <template #default="{ row }">
            <el-tag size="small" type="primary">{{ row.cropName }}</el-tag>
            <span v-if="row.varietyName" class="dim" style="margin-left: 4px">{{ row.varietyName }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('batch.qtyRemainTotal')" min-width="220">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 8px">
              <el-progress
                :percentage="remainPercent(row)"
                :stroke-width="10"
                :status="row.qtyRemainKg <= 0 ? 'success' : (remainPercent(row) > 50 ? '' : 'warning')"
                :show-text="false"
                style="flex: 1"
              />
              <span class="qty">
                <strong>{{ formatKg(row.qtyRemainKg) }}</strong>
                <span class="dim"> / {{ formatKg(row.qtyKg) }}</span>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="dark">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('batch.source')" width="160">
          <template #default="{ row }">
            <code class="batch-code">{{ row.harvestRecordCode }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goDetail(row)">
              {{ t('common.view') }}
            </el-button>
            <el-dropdown @command="(s) => onChangeStatus(row, s)">
              <el-button link type="primary" size="small">
                {{ t('batch.changeStatus') }}<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="s in STATUS_OPTIONS"
                    :key="s.value"
                    :command="s.value"
                    :disabled="s.value === row.status"
                  >
                    <el-tag :type="s.tag" size="small" style="margin-right: 6px">{{ s.label }}</el-tag>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Search as SearchIcon,
  Refresh as RefreshIcon,
  ArrowDown,
} from '@element-plus/icons-vue'
import { listBatches, changeBatchStatus } from '@/api/batch'

const { t } = useI18n()

const router = useRouter()
function goDetail(row) {
  router.push(`/production/batches/${row.id}`)
}

const STATUS_OPTIONS = computed(() => [
  { value: 'pending',    label: t('status.pending'),    tag: 'info' },
  { value: 'processing', label: t('status.processing'), tag: 'warning' },
  { value: 'packed',     label: t('status.packed'),     tag: 'primary' },
  { value: 'sold_out',   label: t('status.sold_out'),   tag: 'success' },
  { value: 'lost',       label: t('status.lost'),       tag: 'danger' },
])
const STATUS_MAP = computed(() => Object.fromEntries(STATUS_OPTIONS.value.map(s => [s.value, s])))
function statusLabel(v) { return STATUS_MAP.value[v]?.label || v }
function statusTag(v)   { return STATUS_MAP.value[v]?.tag || 'info' }

function formatKg(v) {
  if (v == null) return '-'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 3 })
}

function remainPercent(row) {
  const total = Number(row.qtyKg) || 0
  const remain = Number(row.qtyRemainKg) || 0
  if (total <= 0) return 0
  return Math.round((remain / total) * 100)
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const query = reactive({ code: '', status: '', dateFrom: null, dateTo: null })

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
    const data = await listBatches({ ...query, page: page.value, size: pageSize.value })
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}
function onReset() {
  query.code = ''
  query.status = ''
  dateRange.value = null
  reload(1)
}
function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }

onMounted(() => reload(1))

async function onChangeStatus(row, newStatus) {
  if (newStatus === row.status) return
  try {
    await changeBatchStatus(row.id, newStatus)
    ElMessage.success(t('batch.statusChanged', { label: statusLabel(newStatus) }))
    reload()
  } catch {}
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) { padding: 16px; }
.pager { margin-top: 14px; justify-content: flex-end; }
.dim { color: #909399; font-size: 12px; }

.batch-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #d63384;
  font-size: 12px;
}

.link-batch { text-decoration: none; }
.link-batch .batch-code:hover {
  background: #ecf5ff;
  color: #409eff;
  cursor: pointer;
}

.qty {
  min-width: 110px;
  text-align: right;
  font-size: 12px;
  white-space: nowrap;
}
</style>
