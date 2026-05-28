<template>
  <div class="page">
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query" @submit.prevent>
        <el-form-item :label="t('recall.status')">
          <el-select v-model="query.status" :placeholder="t('common.all')" clearable style="width: 180px">
            <el-option v-for="s in STATUSES" :key="s" :label="t(`recall.st_${s}`)" :value="s" />
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
        <span class="table-title">{{ t('recall.title') }}</span>
        <el-button type="danger" size="small" :icon="WarningIcon" @click="openTrigger">{{ t('recall.newRecall') }}</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe row-key="id" @row-click="onRowClick">
        <el-table-column :label="t('recall.code')" width="180">
          <template #default="{ row }"><code class="code-tag">{{ row.code }}</code></template>
        </el-table-column>
        <el-table-column prop="triggeredAt" :label="t('recall.triggeredAt')" width="160" />
        <el-table-column :label="t('recall.batch')" min-width="200">
          <template #default="{ row }">
            <code class="link-code" @click.stop="goBatch(row.batchId)">{{ row.batchCode }}</code>
            <span class="dim small" style="margin-left:4px">{{ row.cropName }} <em v-if="row.varietyName">/ {{ row.varietyName }}</em></span>
          </template>
        </el-table-column>
        <el-table-column :label="t('recall.status')" width="170" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" effect="dark" size="small">{{ t(`recall.st_${row.status}`, row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('recall.affectedOrders')" width="110" align="center">
          <template #default="{ row }"><strong>{{ row.affectedOrderCount }}</strong></template>
        </el-table-column>
        <el-table-column :label="t('recall.affectedCustomers')" width="110" align="center">
          <template #default="{ row }"><strong>{{ row.affectedCustomerCount }}</strong></template>
        </el-table-column>
        <el-table-column :label="t('recall.affectedQty')" width="120" align="right">
          <template #default="{ row }"><strong>{{ row.affectedQty }}</strong></template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="openDetail(row)">{{ t('common.detail') }}</el-button>
            <el-button link size="small" @click.stop="onDownloadPdf(row)">{{ t('recall.downloadPdf') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50]"
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @size-change="onSizeChange"
        @current-change="onPageChange"
      />
    </el-card>

    <!-- Trigger dialog -->
    <el-dialog v-model="triggerDlg" :title="t('recall.newRecall')" width="600px">
      <el-alert type="error" :closable="false" show-icon style="margin-bottom: 12px">
        {{ t('recall.warningHint') }}
      </el-alert>
      <el-form :model="triggerForm" label-width="120px">
        <el-form-item :label="t('recall.batchId')" required>
          <el-input v-model.number="triggerForm.batchId" type="number" :placeholder="t('recall.batchIdHint')" />
        </el-form-item>
        <el-form-item :label="t('recall.reason')" required>
          <el-input v-model="triggerForm.reason" type="textarea" :rows="4" maxlength="2000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="triggerDlg = false">{{ t('common.cancel') }}</el-button>
        <el-button type="danger" :loading="triggering" @click="onTriggerSubmit">{{ t('recall.triggerNow') }}</el-button>
      </template>
    </el-dialog>

    <!-- Detail drawer -->
    <el-drawer v-model="detailDlg" :title="detail?.recall?.code" size="60%">
      <div v-if="detail" class="detail-wrap">
        <div class="detail-head">
          <div>
            <div class="dim small">{{ t('recall.triggeredAt') }}</div>
            <div>{{ detail.recall.triggeredAt }} · {{ detail.recall.initiatedByName || '-' }}</div>
          </div>
          <el-tag :type="statusTag(detail.recall.status)" effect="dark">
            {{ t(`recall.st_${detail.recall.status}`, detail.recall.status) }}
          </el-tag>
        </div>

        <h4>{{ t('recall.batch') }}</h4>
        <p><code class="code-tag">{{ detail.recall.batchCode }}</code>
           <span class="dim">{{ detail.recall.cropName }} {{ detail.recall.varietyName ? '/ ' + detail.recall.varietyName : '' }}</span></p>

        <h4>{{ t('recall.reason') }}</h4>
        <p class="reason-box">{{ detail.recall.reason }}</p>

        <h4>{{ t('recall.affectedOrders') }} ({{ detail.affectedOrders.length }})</h4>
        <el-table :data="detail.affectedOrders" size="small" border>
          <el-table-column type="index" label="#" width="40" align="center" />
          <el-table-column prop="orderCode" :label="t('recall.orderCode')" width="160">
            <template #default="{ row }"><code class="link-code" @click="goOrder(row.orderId)">{{ row.orderCode }}</code></template>
          </el-table-column>
          <el-table-column prop="customerName" :label="t('recall.customer')" min-width="160" />
          <el-table-column prop="qty" :label="t('recall.qty')" width="100" align="right" />
          <el-table-column prop="deliveredAt" :label="t('recall.deliveredAt')" width="170" />
          <el-table-column :label="t('recall.notified')" width="130" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.notifiedAt" type="success" size="small">{{ row.notifiedAt }}</el-tag>
              <el-tag v-else type="danger" size="small">{{ t('recall.notNotified') }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.actions')" width="120" align="center">
            <template #default="{ row }">
              <el-button v-if="!row.notifiedAt" link type="primary" size="small" @click="onNotify(row)">{{ t('recall.markNotified') }}</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="footer-actions">
          <el-button @click="onDownloadPdf(detail.recall)">{{ t('recall.downloadPdf') }}</el-button>
          <el-button v-if="detail.recall.status !== 'closed'" type="primary" @click="onClose">{{ t('recall.closeRecall') }}</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search as SearchIcon, Refresh as RefreshIcon, Warning as WarningIcon } from '@element-plus/icons-vue'
import { listRecalls, getRecall, triggerRecall, notifyAffectedOrder, closeRecall, downloadRecallPdf } from '@/api/recall'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const STATUSES = ['initiated', 'quarantined', 'customers_notified', 'closed']

const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const loading = ref(false)
const query = reactive({ status: '' })

function statusTag(s) {
  return ({ initiated: 'danger', quarantined: 'warning', customers_notified: 'success', closed: 'info' })[s] || 'info'
}

async function reload(toPage) {
  if (toPage) page.value = toPage
  loading.value = true
  try {
    const data = await listRecalls({ ...query, page: page.value, size: pageSize.value })
    list.value = data.list
    total.value = data.total
  } finally { loading.value = false }
}
function onReset() { query.status = ''; reload(1) }
function onPageChange(p) { page.value = p; reload() }
function onSizeChange(s) { pageSize.value = s; reload(1) }

// ----- trigger -----
const triggerDlg = ref(false)
const triggering = ref(false)
const triggerForm = reactive({ batchId: null, reason: '' })
function openTrigger() {
  triggerForm.batchId = null
  triggerForm.reason = ''
  triggerDlg.value = true
}
async function onTriggerSubmit() {
  if (!triggerForm.batchId || !triggerForm.reason) {
    ElMessage.warning(t('recall.fillAllFields'))
    return
  }
  await ElMessageBox.confirm(t('recall.confirmTrigger'), t('common.tip'), { type: 'warning' }).catch(() => null)
  triggering.value = true
  try {
    await triggerRecall({ batchId: triggerForm.batchId, reason: triggerForm.reason, scope: 'batch_only' })
    ElMessage.success(t('recall.triggerSuccess'))
    triggerDlg.value = false
    reload(1)
  } finally { triggering.value = false }
}

// ----- detail drawer -----
const detailDlg = ref(false)
const detail = ref(null)
async function openDetail(row) { detail.value = await getRecall(row.id); detailDlg.value = true }
function onRowClick(row) { openDetail(row) }

async function onNotify(row) {
  await notifyAffectedOrder(detail.value.recall.id, row.id)
  ElMessage.success(t('recall.notifySuccess'))
  detail.value = await getRecall(detail.value.recall.id)
  reload()
}

async function onClose() {
  const { value: remark } = await ElMessageBox.prompt(t('recall.closeRemarkPrompt'), t('common.tip'), { inputType: 'textarea' }).catch(() => ({}))
  await closeRecall(detail.value.recall.id, remark || '')
  ElMessage.success(t('common.saveSuccess'))
  detail.value = await getRecall(detail.value.recall.id)
  reload()
}

async function onDownloadPdf(recall) {
  const blob = await downloadRecallPdf(recall.id)
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${recall.code}.pdf`
  a.click()
  window.URL.revokeObjectURL(url)
}

function goOrder(id) { if (id) router.push(`/sales/orders/${id}`) }
function goBatch(id) { if (id) router.push(`/production/batches/${id}`) }

onMounted(async () => {
  // ?highlight=<recallId>  → auto-open detail
  if (route.query.highlight) {
    detail.value = await getRecall(Number(route.query.highlight))
    detailDlg.value = true
  }
  reload(1)
})
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.filter-card :deep(.el-card__body),
.table-card :deep(.el-card__body) { padding: 16px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.table-title { font-size: 14px; font-weight: 600; color: #1f2937; }
.code-tag { font-family: 'Monaco', 'Consolas', monospace; font-size: 12px; background: #fef3c7; padding: 2px 8px; border-radius: 4px; color: #b91c1c; }
.link-code { cursor: pointer; color: #2563eb; }
.link-code:hover { text-decoration: underline; }
.dim { color: #94a3b8; }
.small { font-size: 11px; }
.pager { margin-top: 14px; justify-content: flex-end; }

.detail-wrap { padding: 0 16px; }
.detail-head { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #e5e7eb; }
.detail-wrap h4 { font-size: 13px; color: #b91c1c; margin: 14px 0 6px; padding-bottom: 4px; border-bottom: 1px solid #fcd7d2; }
.reason-box { background: #fffbeb; border: 1px solid #f7c873; border-radius: 4px; padding: 10px 14px; line-height: 1.6; }
.footer-actions { margin-top: 20px; padding-top: 14px; border-top: 1px dashed #e5e7eb; display: flex; gap: 10px; justify-content: flex-end; }
</style>
