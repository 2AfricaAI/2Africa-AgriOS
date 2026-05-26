<template>
  <div class="page" v-loading="loading">
    <div class="hero">
      <div>
        <div class="hero-title">
          <el-icon><Wallet /></el-icon>
          {{ t('ar.title') }}
        </div>
        <div class="hero-subtitle">{{ t('ar.subtitle') }}</div>
      </div>
      <el-button type="primary" :icon="RefreshIcon" @click="load">{{ t('common.refresh') }}</el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table
        :data="rows" border stripe size="small"
        :empty-text="t('ar.empty')"
        row-key="customer_id"
        @expand-change="onExpand"
      >
        <!-- 展开行: 跟催时间线 -->
        <el-table-column type="expand" width="36">
          <template #default="{ row }">
            <div class="timeline-wrap" v-loading="timelineLoading[row.customer_id]">
              <div class="timeline-head">
                <el-icon><ChatLineRound /></el-icon>
                {{ t('collection.timelineTitle') }}
                <el-tag size="small" type="info" v-if="timelines[row.customer_id]">
                  {{ timelines[row.customer_id].length }} {{ t('collection.records') }}
                </el-tag>
              </div>

              <div v-if="!timelines[row.customer_id] || timelines[row.customer_id].length === 0"
                   class="timeline-empty">
                {{ t('collection.timelineEmpty') }}
              </div>
              <el-timeline v-else class="cl-timeline">
                <el-timeline-item
                  v-for="r in timelines[row.customer_id]"
                  :key="r.id"
                  :type="outcomeTimelineType(r.outcome)"
                  :timestamp="r.logDate"
                  placement="top"
                  size="normal"
                >
                  <div class="cl-row">
                    <el-tag size="small" :type="channelTag(r.channel)">{{ channelLabel(r.channel) }}</el-tag>
                    <el-tag size="small" :type="outcomeTag(r.outcome)" effect="dark">
                      {{ outcomeLabel(r.outcome) }}
                    </el-tag>
                    <span v-if="r.orderCode" class="dim small">
                      → <code>{{ r.orderCode }}</code>
                    </span>
                    <span v-if="r.contactPerson" class="dim small">
                      · {{ t('collection.contactPerson') }}: {{ r.contactPerson }}
                    </span>
                    <span class="dim small spacer">
                      {{ t('collection.by') }} <strong>{{ r.operatorName || '-' }}</strong>
                    </span>
                  </div>
                  <div v-if="r.outcome === 'promised'" class="cl-promise">
                    💰 {{ t('collection.promisedTo') }}
                    <strong>{{ r.promisedDate || '-' }}</strong>
                    <span v-if="r.promisedAmount">
                      · <strong>{{ fmt(r.promisedAmount) }} KES</strong>
                    </span>
                  </div>
                  <div v-if="r.content" class="cl-content">{{ r.content }}</div>
                  <div v-if="r.nextActionDate" class="cl-next">
                    ⏰ {{ t('collection.nextActionDate') }}:
                    <strong>{{ r.nextActionDate }}</strong>
                  </div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </template>
        </el-table-column>

        <el-table-column :label="t('ar.customer')" min-width="220">
          <template #default="{ row }">
            <strong>{{ row.customer_name }}</strong>
            <code class="dim small"> {{ row.customer_code }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('ar.customerType')" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small">{{ row.customer_type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('ar.totalBilled')" min-width="120" align="right">
          <template #default="{ row }">{{ fmt(row.total_billed) }}</template>
        </el-table-column>
        <el-table-column :label="t('ar.totalReceived')" min-width="120" align="right">
          <template #default="{ row }">
            <span class="dim">{{ fmt(row.total_received) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('ar.arOutstanding')" min-width="130" align="right">
          <template #default="{ row }">
            <strong class="outstanding">{{ fmt(row.ar_outstanding) }}</strong>
          </template>
        </el-table-column>
        <el-table-column :label="t('ar.aging07')" min-width="90" align="right">
          <template #default="{ row }">{{ fmt(row.aging_0_7) }}</template>
        </el-table-column>
        <el-table-column :label="t('ar.aging814')" min-width="90" align="right">
          <template #default="{ row }">
            <span :class="{ amber: Number(row.aging_8_14) > 0 }">{{ fmt(row.aging_8_14) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('ar.aging1530')" min-width="100" align="right">
          <template #default="{ row }">
            <span :class="{ orange: Number(row.aging_15_30) > 0 }">{{ fmt(row.aging_15_30) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('ar.aging30plus')" min-width="110" align="right">
          <template #default="{ row }">
            <strong :class="{ red: Number(row.aging_30_plus) > 0 }">{{ fmt(row.aging_30_plus) }}</strong>
          </template>
        </el-table-column>

        <el-table-column :label="t('common.actions')" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="onOpenCollection(row)">
              📞 {{ t('collection.btn') }}
            </el-button>
            <el-button link type="success" size="small" @click="onOpenStatement(row)">
              📄 {{ t('statement.btn') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 跟催弹窗 -->
    <CollectionLogDialog
      v-model="collDialogVisible"
      :customer-info="collTarget"
      @saved="onCollectionSaved"
    />

    <!-- 对账单弹窗 -->
    <el-dialog
      v-model="stmtDialogVisible"
      :title="t('statement.title')"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="stmtTarget" class="stmt-target">
        <el-tag size="small" type="primary">{{ stmtTarget.name }}</el-tag>
        <code class="dim small" style="margin-left: 6px">{{ stmtTarget.code }}</code>
      </div>
      <el-form label-width="100px" style="margin-top: 14px">
        <el-form-item :label="t('statement.period')">
          <el-date-picker
            v-model="stmtRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            :start-placeholder="t('statement.from')"
            :end-placeholder="t('statement.to')"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stmtDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="stmtDownloading" @click="onDownloadStatement">
          📥 {{ t('statement.download') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Wallet, Refresh as RefreshIcon, ChatLineRound } from '@element-plus/icons-vue'
import {
  getArAging,
  listCollectionsByCustomer,
  downloadStatementPdf,
} from '@/api/finance'
import CollectionLogDialog from '@/components/CollectionLogDialog.vue'

const { t } = useI18n()
const loading = ref(false)
const rows = ref([])

function fmt(v) {
  if (v == null) return '0.00'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function load() {
  loading.value = true
  try { rows.value = await getArAging() }
  catch {} finally { loading.value = false }
}
onMounted(load)

// ============================================================
// 跟催时间线 (展开行)
// ============================================================
const timelines = reactive({})           // { customer_id: [...rows] }
const timelineLoading = reactive({})

async function onExpand(row, expandedRows) {
  // 仅在展开时加载, 折叠时不动
  const isExpanded = expandedRows.some(r => r.customer_id === row.customer_id)
  if (!isExpanded) return
  await loadTimeline(row.customer_id)
}

async function loadTimeline(customerId) {
  timelineLoading[customerId] = true
  try {
    timelines[customerId] = await listCollectionsByCustomer(customerId)
  } catch {
    timelines[customerId] = []
  } finally {
    timelineLoading[customerId] = false
  }
}

// 渠道 / 结果 显示辅助
function channelLabel(v) { return t(`collection.channel${capitalize(v)}`, v) }
function outcomeLabel(v) { return t(`collection.outcome${capitalize(v?.replace('_', ''))}`, v) }
function capitalize(s) { return s ? s.charAt(0).toUpperCase() + s.slice(1).toLowerCase().replace(/_/g, '') : '' }

const CHANNEL_TAG = { phone: 'primary', whatsapp: 'success', sms: 'warning', email: 'info', visit: 'danger', other: '' }
function channelTag(v) { return CHANNEL_TAG[v] || '' }

const OUTCOME_TAG = {
  promised: 'success', refused: 'danger', no_answer: 'info',
  disputed: 'warning', paid: 'success', other: ''
}
function outcomeTag(v) { return OUTCOME_TAG[v] || '' }

const OUTCOME_TIMELINE = {
  promised: 'success', refused: 'danger', no_answer: 'info',
  disputed: 'warning', paid: 'success', other: 'primary'
}
function outcomeTimelineType(v) { return OUTCOME_TIMELINE[v] || 'primary' }

// ============================================================
// 录入跟催
// ============================================================
const collDialogVisible = ref(false)
const collTarget = ref(null)

function onOpenCollection(row) {
  collTarget.value = {
    id: row.customer_id,
    code: row.customer_code,
    name: row.customer_name,
    outstanding: row.ar_outstanding,
  }
  collDialogVisible.value = true
}

async function onCollectionSaved() {
  // 重新拉对应客户的时间线
  if (collTarget.value) {
    await loadTimeline(collTarget.value.id)
  }
  // AR 余额不变, 不用刷整个列表
}

// ============================================================
// 对账单下载
// ============================================================
const stmtDialogVisible = ref(false)
const stmtTarget = ref(null)
const stmtRange = ref(defaultStmtRange())
const stmtDownloading = ref(false)

function defaultStmtRange() {
  const today = new Date()
  const first = new Date(today.getFullYear(), today.getMonth(), 1)
  const fmt = d => d.toISOString().slice(0, 10)
  return [fmt(first), fmt(today)]
}

function onOpenStatement(row) {
  stmtTarget.value = { id: row.customer_id, code: row.customer_code, name: row.customer_name }
  stmtRange.value = defaultStmtRange()
  stmtDialogVisible.value = true
}

async function onDownloadStatement() {
  if (!stmtTarget.value) return
  if (!stmtRange.value || stmtRange.value.length !== 2) {
    ElMessage.warning(t('valid.required', { field: t('statement.period') }))
    return
  }
  const [from, to] = stmtRange.value
  stmtDownloading.value = true
  try {
    const blob = await downloadStatementPdf(stmtTarget.value.id, { from, to })
    const url = window.URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }))
    const a = document.createElement('a')
    a.href = url
    a.download = `statement-${stmtTarget.value.code}-${from}-${to}.pdf`
    document.body.appendChild(a)
    a.click()
    a.remove()
    setTimeout(() => window.URL.revokeObjectURL(url), 1000)
    ElMessage.success(t('statement.downloadSuccess'))
    stmtDialogVisible.value = false
  } catch {
    ElMessage.error(t('statement.downloadFail'))
  } finally {
    stmtDownloading.value = false
  }
}
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }
.hero {
  background:
    radial-gradient(circle at 90% 20%, rgba(255, 255, 255, 0.15), transparent 40%),
    linear-gradient(135deg, #0f3a26 0%, #1f7a35 55%, #2BA84A 100%);
  color: #fff; padding: 20px 24px; border-radius: 10px;
  display: flex; align-items: center; justify-content: space-between;
  box-shadow: 0 6px 20px rgba(15, 58, 38, 0.22);
}
.hero-title { font-size: 20px; font-weight: 600; display: inline-flex; align-items: center; gap: 8px; }
.hero-subtitle { font-size: 13px; opacity: .85; margin-top: 4px; }
.hero :deep(.el-button) { background: rgba(255,255,255,.18); border-color: rgba(255,255,255,.3); color: #fff; }

.dim { color: #909399; font-family: 'Consolas', monospace; }
.small { font-size: 11px; }
.outstanding { color: #1f7a35; }
.amber  { color: #e6a23c; font-weight: 600; }
.orange { color: #fa8c16; font-weight: 600; }
.red    { color: #f56c6c; font-weight: 700; }

.timeline-wrap {
  padding: 12px 18px 18px 28px;
  background: #f7faf8;
}
.timeline-head {
  display: inline-flex; align-items: center; gap: 6px;
  font-weight: 600; font-size: 13px; color: #1f7a35;
  margin-bottom: 10px;
}
.timeline-empty {
  color: #909399; font-style: italic; font-size: 12px; padding: 8px 0;
}
.cl-timeline { padding-left: 4px; }
.cl-timeline :deep(.el-timeline-item__timestamp) {
  font-size: 12px; color: #606266; font-weight: 600;
}
.cl-row { display: flex; flex-wrap: wrap; align-items: center; gap: 6px; }
.cl-row .spacer { margin-left: auto; }
.cl-promise {
  margin-top: 4px; padding: 4px 8px;
  background: #ecf9ef; border-left: 3px solid #2BA84A;
  font-size: 12px; border-radius: 2px;
}
.cl-content {
  margin-top: 4px; font-size: 12px; color: #606266;
  white-space: pre-wrap;
}
.cl-next {
  margin-top: 4px; font-size: 11px; color: #e6a23c;
}

.stmt-target {
  padding: 8px 12px;
  background: #f7faf8;
  border-left: 3px solid #1f7a35;
}
</style>
