<template>
  <div class="page" v-loading="loading">
    <!-- 顶部导航 + 操作 -->
    <div class="page-head">
      <el-button :icon="ArrowLeft" text @click="goBack">{{ t('batch.backToList') }}</el-button>
      <div class="head-actions" v-if="data?.batch">
        <el-button type="info" plain :icon="LocationFilled" @click="openTrace">
          {{ t('menu.qcTrace') }}
        </el-button>
        <el-button type="success" @click="pnlVisible = true">
          {{ t('pnl.viewPnl') }}
        </el-button>
        <el-button
          type="primary"
          :icon="Scissor"
          :disabled="!canSplit"
          @click="openSplit"
        >
          {{ t('batch.splitTitle') }}
        </el-button>
      </div>
    </div>

    <!-- 主批次卡片 -->
    <el-card shadow="never" class="info-card" v-if="data?.batch">
      <template #header>
        <div class="card-head">
          <span class="card-title">
            <el-icon><Box /></el-icon>
            <code class="batch-code">{{ data.batch.code }}</code>
          </span>
          <el-tag :type="statusTag(data.batch.status)" effect="dark" size="small">
            {{ statusLabel(data.batch.status) }}
          </el-tag>
        </div>
      </template>

      <el-descriptions :column="3" border size="small">
        <el-descriptions-item :label="t('batch.harvestDate')">
          {{ data.batch.harvestDate || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('batch.cropVariety')">
          <el-tag size="small" type="primary">{{ data.batch.cropName }}</el-tag>
          <span v-if="data.batch.varietyName" class="dim" style="margin-left: 4px">
            {{ data.batch.varietyName }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('batch.plot')">
          <el-tag size="small">{{ data.batch.plotName }}</el-tag>
          <span class="dim" style="margin-left: 4px">{{ data.batch.plotCode }}</span>
        </el-descriptions-item>

        <el-descriptions-item :label="t('batch.plan')">
          <code class="batch-code">{{ data.batch.planCode || '-' }}</code>
        </el-descriptions-item>
        <el-descriptions-item :label="t('batch.source')">
          <code class="batch-code">{{ data.batch.harvestRecordCode || '-' }}</code>
        </el-descriptions-item>
        <el-descriptions-item :label="t('batch.parent')">
          <template v-if="data.parent">
            <router-link
              :to="`/production/batches/${data.parent.id}`"
              class="link-batch"
            >
              <code class="batch-code">{{ data.parent.code }}</code>
            </router-link>
          </template>
          <span v-else class="dim">{{ t('batch.rootBatch') }}</span>
        </el-descriptions-item>

        <el-descriptions-item :label="t('batch.qtyKg')">
          <strong>{{ formatKg(data.batch.qtyKg) }}</strong>
        </el-descriptions-item>
        <el-descriptions-item :label="t('batch.qtyRemainKg')">
          <strong :class="{'remain-low': remainPercent < 30}">
            {{ formatKg(data.batch.qtyRemainKg) }}
          </strong>
          <span class="dim"> ({{ remainPercent }}%)</span>
        </el-descriptions-item>
        <el-descriptions-item :label="t('common.remark')">
          {{ data.batch.remark || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <el-progress
        :percentage="remainPercent"
        :stroke-width="14"
        :status="data.batch.qtyRemainKg <= 0 ? 'success' : (remainPercent > 50 ? '' : 'warning')"
        :format="(p) => t('batch.remainPercent', { p })"
        style="margin-top: 16px"
      />
    </el-card>

    <!-- 子批次表 -->
    <el-card shadow="never" class="info-card">
      <template #header>
        <div class="card-head">
          <span class="card-title">
            <el-icon><Share /></el-icon>
            {{ t('batch.children') }} ({{ data?.children?.length || 0 }})
          </span>
          <span class="dim" v-if="!data?.children?.length">{{ t('batch.notSplitYet') }}</span>
        </div>
      </template>

      <el-table
        :data="data?.children || []"
        border
        stripe
        size="small"
        :empty-text="t('batch.emptyChildren')"
      >
        <el-table-column :label="t('batch.childBatchCode')" min-width="180">
          <template #default="{ row }">
            <router-link :to="`/production/batches/${row.id}`" class="link-batch">
              <code class="batch-code">{{ row.code }}</code>
            </router-link>
          </template>
        </el-table-column>
        <el-table-column :label="t('batch.qtyKgShort')" width="140">
          <template #default="{ row }">
            <strong>{{ formatKg(row.qtyRemainKg) }}</strong>
            <span class="dim"> / {{ formatKg(row.qtyKg) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="dark">
              {{ statusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="harvestDate" :label="t('batch.harvestDate')" width="110" />
        <el-table-column prop="remark" :label="t('common.remark')" min-width="200">
          <template #default="{ row }">
            <span :class="{ dim: !row.remark }">{{ row.remark || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 包装单表 -->
    <el-card shadow="never" class="info-card">
      <template #header>
        <div class="card-head">
          <span class="card-title">
            <el-icon><Goods /></el-icon>
            {{ t('batch.relatedPackings') }} ({{ data?.packings?.length || 0 }})
          </span>
          <span class="dim" v-if="!data?.packings?.length">{{ t('batch.noPackings') }}</span>
        </div>
      </template>

      <el-table
        :data="data?.packings || []"
        border
        stripe
        size="small"
        :empty-text="t('batch.emptyPackings')"
      >
        <el-table-column :label="t('pack.code')" min-width="180">
          <template #default="{ row }">
            <code class="batch-code">{{ row.code }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('pack.sku')" min-width="240">
          <template #default="{ row }">
            <div>
              <el-tag size="small" type="primary">{{ row.grade }}</el-tag>
              <span style="margin-left: 6px">{{ row.skuName }}</span>
            </div>
            <code class="batch-code" style="font-size: 11px">{{ row.skuCode }}</code>
          </template>
        </el-table-column>
        <el-table-column :label="t('pack.spec')" min-width="140">
          <template #default="{ row }">
            {{ row.specName }}
            <span class="dim"> ({{ row.specCode }})</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('batch.unitsAndNet')" width="160">
          <template #default="{ row }">
            <strong>{{ row.qtyUnits }}</strong> {{ t('batch.pieces') }}
            <div class="dim">{{ formatKg(row.netWeightKg) }} {{ t('units.kg') }}</div>
          </template>
        </el-table-column>
        <el-table-column :label="t('pack.location')" min-width="140">
          <template #default="{ row }">
            <el-tag size="small">{{ row.locationName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="packedAt" :label="t('pack.packedAt')" width="160" />
        <el-table-column prop="operatorName" :label="t('pack.operator')" width="110" />
      </el-table>
    </el-card>

    <!-- 拆分对话框 -->
    <el-dialog
      v-model="splitOpen"
      :title="t('batch.splitTitle')"
      width="640px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div class="split-summary" v-if="data?.batch">
        <span>
          {{ t('batch.splitFromCode') }} <code class="batch-code">{{ data.batch.code }}</code>
        </span>
        <span>
          {{ t('batch.splittable') }}
          <strong style="color: #67c23a">
            {{ formatKg(data.batch.qtyRemainKg) }} {{ t('units.kg') }}
          </strong>
        </span>
      </div>

      <el-table :data="splitForm.children" border size="small" style="margin-top: 12px">
        <el-table-column label="#" type="index" width="50" align="center" />
        <el-table-column :label="t('batch.splitChildKg')" width="180">
          <template #default="{ row }">
            <el-input-number
              v-model="row.qtyKg"
              :min="0.001"
              :max="Number(data?.batch?.qtyRemainKg) || 0"
              :precision="3"
              :step="1"
              :controls="false"
              style="width: 100%"
            />
          </template>
        </el-table-column>
        <el-table-column :label="t('common.remark')">
          <template #default="{ row }">
            <el-input v-model="row.remark" maxlength="255" :placeholder="t('batch.splitRemarkPlaceholder')" />
          </template>
        </el-table-column>
        <el-table-column label="" width="60" align="center">
          <template #default="{ $index }">
            <el-button
              link
              type="danger"
              :icon="Delete"
              :disabled="splitForm.children.length <= 1"
              @click="removeChild($index)"
            />
          </template>
        </el-table-column>
      </el-table>

      <div class="split-toolbar">
        <el-button :icon="Plus" link type="primary" @click="addChild">
          {{ t('batch.splitAddChild') }}
        </el-button>
        <div class="split-total">
          {{ t('batch.splitProgress') }}
          <strong :class="{ 'sum-over': sumOver }">{{ formatKg(splitSum) }}</strong>
          / {{ formatKg(data?.batch?.qtyRemainKg) }} {{ t('units.kg') }}
          <el-tag v-if="sumOver" type="danger" size="small" effect="dark" style="margin-left: 6px">
            {{ t('batch.splitOver') }}
          </el-tag>
        </div>
      </div>

      <template #footer>
        <el-button @click="splitOpen = false">{{ t('common.cancel') }}</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="sumOver || splitSum <= 0"
          @click="submitSplit"
        >
          {{ t('batch.splitConfirmBtn', { n: splitForm.children.length }) }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Sprint 12 - P&L dialog -->
    <PnLDialog
      v-model="pnlVisible"
      kind="batch"
      :ref-id="data?.batch?.id"
      :ref-code="data?.batch?.code"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  Box,
  Goods,
  Share,
  Scissor,
  Delete,
  Plus,
  LocationFilled,
} from '@element-plus/icons-vue'
import { getBatchDetail, splitBatch } from '@/api/batch'
import PnLDialog from '@/components/PnLDialog.vue'

// Sprint 12 - P&L dialog state (uses ref from existing vue import)
const pnlVisible = ref(false)

const { t } = useI18n()

const route = useRoute()
const router = useRouter()
const batchId = computed(() => Number(route.params.id))

const loading = ref(false)
const data = ref(null)

const STATUS_MAP = computed(() => ({
  pending:    { label: t('status.pending'),    tag: 'info' },
  processing: { label: t('status.processing'), tag: 'warning' },
  packed:     { label: t('status.packed'),     tag: 'primary' },
  sold_out:   { label: t('status.sold_out'),   tag: 'success' },
  lost:       { label: t('status.lost'),       tag: 'danger' },
}))
function statusLabel(v) { return STATUS_MAP.value[v]?.label || v }
function statusTag(v)   { return STATUS_MAP.value[v]?.tag || 'info' }

function formatKg(v) {
  if (v == null) return '-'
  return Number(v).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 3 })
}

const remainPercent = computed(() => {
  const total = Number(data.value?.batch?.qtyKg) || 0
  const remain = Number(data.value?.batch?.qtyRemainKg) || 0
  if (total <= 0) return 0
  return Math.round((remain / total) * 100)
})

const canSplit = computed(() => {
  const b = data.value?.batch
  if (!b) return false
  if (Number(b.qtyRemainKg) <= 0) return false
  return ['pending', 'processing'].includes(b.status)
})

async function load() {
  loading.value = true
  try {
    data.value = await getBatchDetail(batchId.value)
  } catch (e) {
    ElMessage.error(t('batch.loadFailed'))
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch(batchId, (v) => { if (v) load() })

function goBack() {
  router.push('/production/batches')
}

function openTrace() {
  if (!data.value?.batch?.code) return
  router.push({ path: '/qc/trace', query: { code: data.value.batch.code } })
}

// ----- 拆分对话框 -----
const splitOpen = ref(false)
const submitting = ref(false)
const splitForm = reactive({
  children: [
    { qtyKg: undefined, remark: '' },
    { qtyKg: undefined, remark: '' },
  ],
})

function openSplit() {
  splitForm.children.splice(0, splitForm.children.length,
    { qtyKg: undefined, remark: '' },
    { qtyKg: undefined, remark: '' },
  )
  splitOpen.value = true
}
function addChild() {
  splitForm.children.push({ qtyKg: undefined, remark: '' })
}
function removeChild(i) {
  if (splitForm.children.length <= 1) return
  splitForm.children.splice(i, 1)
}

const splitSum = computed(() => {
  return splitForm.children.reduce((s, c) => s + (Number(c.qtyKg) || 0), 0)
})
const sumOver = computed(() => {
  const max = Number(data.value?.batch?.qtyRemainKg) || 0
  return splitSum.value > max + 1e-9
})

async function submitSplit() {
  // 至少一个有量
  const validChildren = splitForm.children.filter(c => Number(c.qtyKg) > 0)
  if (validChildren.length === 0) {
    ElMessage.warning(t('batch.splitChildAtLeastOne'))
    return
  }
  if (sumOver.value) {
    ElMessage.error(t('batch.splitOverError'))
    return
  }

  await ElMessageBox.confirm(
    t('batch.splitConfirm', { kg: formatKg(splitSum.value), n: validChildren.length }),
    t('batch.splitConfirmTitle'),
    { type: 'warning' }
  ).catch(() => null)

  submitting.value = true
  try {
    const ids = await splitBatch(batchId.value, { children: validChildren })
    ElMessage.success(t('batch.splitSuccess', { n: ids.length }))
    splitOpen.value = false
    await load()
  } catch (e) {
    // 全局拦截器已经 toast
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.info-card :deep(.el-card__body) { padding: 16px; }
.info-card :deep(.el-card__header) { padding: 12px 16px; }

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 14px;
  color: #1f2329;
}

.dim { color: #909399; font-size: 12px; }

.batch-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
  color: #d63384;
  font-size: 12px;
}

.link-batch {
  text-decoration: none;
}
.link-batch .batch-code:hover {
  background: #ecf5ff;
  color: #409eff;
}

.remain-low { color: #e6a23c; }

.split-summary {
  display: flex;
  justify-content: space-between;
  background: #f5f7fa;
  border-radius: 4px;
  padding: 10px 14px;
  font-size: 13px;
}

.split-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}
.split-total { font-size: 13px; color: #606266; }
.sum-over { color: #f56c6c; }
</style>
