<template>
  <div class="page">
    <!-- Filters -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('common.status')">
          <el-select v-model="query.status" :placeholder="t('common.all')" clearable style="width: 130px">
            <el-option :label="t('inbound.statusDraft')" value="draft" />
            <el-option :label="t('inbound.statusConfirmed')" value="confirmed" />
            <el-option :label="t('inbound.statusCancelled')" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inbound.warehouse')">
          <el-select v-model="query.warehouseId" :placeholder="t('common.all')" clearable filterable style="width: 180px">
            <el-option v-for="w in warehouses" :key="w.id" :value="w.id" :label="`${w.code} · ${w.name}`" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('inbound.sourceType')">
          <el-select v-model="query.sourceType" :placeholder="t('common.all')" clearable style="width: 140px">
            <el-option label="PO Receive" value="po_receive" />
            <el-option label="Return In" value="return_in" />
            <el-option label="Transfer In" value="transfer_in" />
            <el-option label="Manual" value="manual" />
          </el-select>
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
        <span class="table-title">{{ t('inbound.title') }}</span>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id" @expand-change="onExpand">
        <el-table-column type="expand" width="36">
          <template #default="{ row }">
            <div class="expand-wrap" v-loading="detailLoading[row.id]">
              <el-table v-if="detailItems[row.id]" :data="detailItems[row.id]" size="small" border>
                <el-table-column prop="inputItemCode" :label="t('inputItem.code')" width="100" />
                <el-table-column prop="inputItemName" :label="t('inputItem.name')" min-width="160" />
                <el-table-column prop="unit" :label="t('inputItem.unit')" width="60" align="center" />
                <el-table-column :label="t('inbound.expectedQty')" width="120" align="right">
                  <template #default="{ row: r }">
                    <strong>{{ fmtQty(r.expectedQty) }}</strong>
                  </template>
                </el-table-column>
                <el-table-column :label="t('inbound.actualQty')" width="120" align="right">
                  <template #default="{ row: r }">
                    <span v-if="r.actualQty != null" :class="r.actualQty < r.expectedQty ? 'text-warn' : 'text-ok'">
                      {{ fmtQty(r.actualQty) }}
                    </span>
                    <span v-else class="dim">{{ t('inbound.pending') }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="remark" :label="t('common.remark')" min-width="120">
                  <template #default="{ row: r }">{{ r.remark || '-' }}</template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="t('inbound.code')" width="180">
          <template #default="{ row }">
            <code class="code-tag">{{ row.code }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('inbound.sourceType')" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ t(`inbound.source_${row.sourceType}`, row.sourceType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('inbound.sourceRef')" width="170">
          <template #default="{ row }">
            <span v-if="row.sourceCode">{{ row.sourceCode }}</span>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="warehouseName" :label="t('inbound.warehouse')" min-width="140" show-overflow-tooltip />
        <el-table-column :label="t('inbound.items')" width="70" align="center">
          <template #default="{ row }">{{ row.itemCount }}</template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="dark">
              {{ t(`inbound.status${cap(row.status)}`) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('inbound.confirmedBy')" width="120">
          <template #default="{ row }">{{ row.confirmedByName || '-' }}</template>
        </el-table-column>
        <el-table-column :label="t('inbound.createdAt')" width="150" align="center">
          <template #default="{ row }">{{ fmtTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <template v-if="row.status === 'draft'">
              <el-button type="success" link size="small" @click="openConfirmDialog(row)">{{ t('inbound.confirm') }}</el-button>
              <el-button type="danger" link size="small" @click="onCancel(row)">{{ t('common.cancel') }}</el-button>
            </template>
            <span v-else class="dim">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper"
          :total="total" v-model:current-page="query.page" v-model:page-size="query.size"
          :page-sizes="[20, 50, 100]"
          @current-change="() => reload()" @size-change="() => reload(1)" />
      </div>
    </el-card>

    <!-- Confirm dialog -->
    <el-dialog v-model="confirmDlg.visible" :title="t('inbound.confirmTitle')" width="700px" :close-on-click-modal="false">
      <p class="confirm-hint">{{ t('inbound.confirmHint') }}</p>
      <el-table :data="confirmDlg.items" border size="small">
        <el-table-column prop="inputItemCode" :label="t('inputItem.code')" width="100" />
        <el-table-column prop="inputItemName" :label="t('inputItem.name')" min-width="160" />
        <el-table-column prop="unit" :label="t('inputItem.unit')" width="60" align="center" />
        <el-table-column :label="t('inbound.expectedQty')" width="110" align="right">
          <template #default="{ row }"><strong>{{ fmtQty(row.expectedQty) }}</strong></template>
        </el-table-column>
        <el-table-column :label="t('inbound.actualQty')" width="130">
          <template #default="{ row, $index }">
            <el-input-number v-model="confirmDlg.items[$index].actualQty"
              :min="0" :precision="3" :controls="false" size="small" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column :label="t('common.remark')" min-width="120">
          <template #default="{ row, $index }">
            <el-input v-model="confirmDlg.items[$index].remark" size="small" maxlength="255" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="confirmDlg.visible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="success" :loading="confirming" @click="onConfirmSubmit">{{ t('inbound.confirmSubmit') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search as SearchIcon, Refresh as RefreshIcon } from '@element-plus/icons-vue'
import { listInbound, getInbound, confirmInbound, cancelInbound } from '@/api/inbound'
import { listWarehouses } from '@/api/warehouse'

const { t } = useI18n()

const cap = (s) => s ? s.charAt(0).toUpperCase() + s.slice(1) : ''
const statusTag = (s) => ({ draft: 'warning', confirmed: 'success', cancelled: 'info' })[s] || 'info'
function fmtQty(v) {
  if (v == null) return '-'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 3 })
}
function fmtTime(v) { return v ? v.replace('T', ' ').slice(0, 16) : '-' }

// ----- query -----
const query = reactive({ status: '', warehouseId: null, sourceType: '', page: 1, size: 20 })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function reload(p) {
  if (p) query.page = p
  loading.value = true
  try {
    const data = await listInbound(query)
    list.value = data.list || []
    total.value = data.total || 0
  } catch (e) { console.error(e) }
  finally { loading.value = false }
}
function onReset() {
  Object.assign(query, { status: '', warehouseId: null, sourceType: '', page: 1, size: 20 })
  reload()
}

// ----- warehouses -----
const warehouses = ref([])
async function loadWarehouses() {
  try {
    const data = await listWarehouses({ status: 1, page: 1, size: 200 })
    warehouses.value = data.list || []
  } catch {/* */}
}

// ----- expand detail -----
const detailItems = reactive({})
const detailLoading = reactive({})
async function onExpand(row, expandedRows) {
  if (!expandedRows.some(r => r.id === row.id)) return
  detailLoading[row.id] = true
  try {
    const data = await getInbound(row.id)
    detailItems[row.id] = data.items || []
  } catch { detailItems[row.id] = [] }
  finally { detailLoading[row.id] = false }
}

// ----- confirm dialog -----
const confirmDlg = reactive({ visible: false, id: null, items: [] })
const confirming = ref(false)

async function openConfirmDialog(row) {
  detailLoading[row.id] = true
  try {
    const data = await getInbound(row.id)
    confirmDlg.id = row.id
    confirmDlg.items = (data.items || []).map(it => ({
      ...it,
      actualQty: it.actualQty ?? it.expectedQty, // default to expected
      remark: it.remark || '',
    }))
    confirmDlg.visible = true
  } catch { ElMessage.error('Failed to load inbound detail') }
  finally { detailLoading[row.id] = false }
}

async function onConfirmSubmit() {
  confirming.value = true
  try {
    await confirmInbound(confirmDlg.id, {
      items: confirmDlg.items.map(it => ({
        itemId: it.id,
        actualQty: it.actualQty,
        remark: it.remark,
      })),
    })
    ElMessage.success(t('inbound.confirmSuccess'))
    confirmDlg.visible = false
    reload()
  } catch (e) { console.error(e) }
  finally { confirming.value = false }
}

// ----- cancel -----
async function onCancel(row) {
  await ElMessageBox.confirm(
    t('inbound.cancelConfirm', { code: row.code }), t('common.tip'), { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await cancelInbound(row.id)
    ElMessage.success(t('inbound.cancelSuccess'))
    reload()
  } catch (e) { if (e === 'cancel') return }
}

onMounted(() => { loadWarehouses(); reload() })
</script>

<style scoped>
.page { padding: 16px; }
.filter-card { margin-bottom: 12px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.table-title { font-size: 16px; font-weight: 600; color: #1f2937; }
.pager { margin-top: 12px; text-align: right; }
.expand-wrap { padding: 8px 12px; background: #fafafa; }
.code-tag { font-size: 12px; background: #f0fdf4; padding: 2px 6px; border-radius: 3px; color: #1f7a35; }
.dim { color: #909399; }
.text-ok { color: #16a34a; font-weight: 600; }
.text-warn { color: #d97706; font-weight: 600; }
.confirm-hint { color: #6b7280; font-size: 13px; margin-bottom: 12px; }
</style>
