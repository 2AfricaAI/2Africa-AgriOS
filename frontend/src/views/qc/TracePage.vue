<template>
  <div class="trace-page" :class="{ 'public-mode': publicMode }">
    <!-- header -->
    <div class="trace-header">
      <div>
        <h2 class="trace-title">{{ t('trace.title') }}</h2>
        <p class="trace-sub">{{ t('trace.subtitle') }}</p>
      </div>
      <el-button v-if="publicMode" link @click="reload">{{ t('common.refresh') }}</el-button>
    </div>

    <!-- batch search bar (internal only) -->
    <el-card v-if="!publicMode" shadow="never" class="search-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item :label="t('trace.batchCode')">
          <el-input v-model="batchCodeInput" :placeholder="t('trace.batchCodeHint')" style="width: 280px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="SearchIcon" @click="onSearch">{{ t('common.search') }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div v-if="!trace && !loading" class="empty">
      <el-icon :size="48" color="#cbd5e1"><InfoFilled /></el-icon>
      <p>{{ t('trace.emptyHint') }}</p>
    </div>

    <div v-if="trace" v-loading="loading">
      <!-- BATCH card with QR -->
      <el-card shadow="never" class="batch-card">
        <div class="batch-grid">
          <div class="batch-info">
            <div class="batch-label">{{ t('trace.batch') }}</div>
            <div class="batch-code">{{ trace.batch.code }}</div>
            <div class="batch-meta">
              <span class="meta-item"><el-tag size="small" type="success">{{ trace.batch.cropName }}</el-tag></span>
              <span class="meta-item">{{ trace.batch.varietyName || '-' }}</span>
              <span class="meta-item dim">{{ trace.batch.createdDate }}</span>
            </div>
            <div class="batch-qty">{{ fmtQty(trace.batch.qtyKg) }} <span class="dim small">kg</span></div>
            <el-tag :type="batchStatusTag(trace.batch.status)" effect="dark" size="small">{{ trace.batch.status }}</el-tag>
          </div>
          <div class="qr-box" v-if="!publicMode">
            <canvas ref="qrCanvas" width="140" height="140"></canvas>
            <el-button link type="primary" size="small" @click="downloadQr">{{ t('trace.downloadQr') }}</el-button>
            <div class="qr-url">{{ qrUrl }}</div>
            <el-dropdown trigger="click" @command="onGapExport" style="margin-top: 8px">
              <el-button size="small" type="warning" plain>
                {{ t('gap.exportBatch') }}<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="pdf">{{ t('gap.asPdf') }}</el-dropdown-item>
                  <el-dropdown-item command="xlsx">{{ t('gap.asXlsx') }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-card>

      <!-- TRACE CHAIN -->
      <div class="chain">

        <!-- 1. Plot & Plan -->
        <div class="node node-origin">
          <div class="node-icon"><el-icon :size="22"><LocationFilled /></el-icon></div>
          <div class="node-body">
            <div class="node-title">{{ t('trace.origin') }}</div>
            <div v-if="trace.plot" class="node-row">
              <span class="label">{{ t('trace.plot') }}</span>
              <span><code>{{ trace.plot.code }}</code> · {{ trace.plot.name }} · {{ trace.plot.regionName }}
                <span class="dim">({{ trace.plot.areaHa }} ha)</span></span>
            </div>
            <div v-if="trace.plan" class="node-row">
              <span class="label">{{ t('trace.plan') }}</span>
              <span><code>{{ trace.plan.code }}</code></span>
            </div>
            <div v-if="trace.plan" class="node-row">
              <span class="label">{{ t('trace.sow') }}</span>
              <span>{{ trace.plan.plannedSowDate || '-' }}</span>
              <span class="label" style="margin-left:16px">{{ t('trace.harvestPlan') }}</span>
              <span>{{ trace.plan.plannedHarvestDate || '-' }}</span>
            </div>
          </div>
        </div>

        <!-- 2. Inbound (PO) -->
        <div v-if="trace.inbounds?.length" class="node node-inbound">
          <div class="node-icon"><el-icon :size="22"><Box /></el-icon></div>
          <div class="node-body">
            <div class="node-title">{{ t('trace.inputSource') }}</div>
            <p class="node-hint">{{ t('trace.inputSourceHint') }}</p>
            <div v-for="ib in trace.inbounds" :key="ib.id" class="sub-card">
              <div><code class="code-tag">{{ ib.code }}</code> · {{ ib.confirmedAt || '-' }}
                <span v-if="ib.sourceCode" class="dim">from PO {{ ib.sourceCode }}</span></div>
              <ul class="input-list">
                <li v-for="it in ib.items" :key="it.inputItemId">
                  <code>{{ it.inputItemCode }}</code> {{ it.inputItemName }}
                  <span class="dim">{{ fmtQty(it.qty) }}</span>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 3. Activities -->
        <div v-if="trace.activities?.length" class="node node-activity">
          <div class="node-icon"><el-icon :size="22"><Sunny /></el-icon></div>
          <div class="node-body">
            <div class="node-title">{{ t('trace.fieldActivities') }} ({{ trace.activities.length }})</div>
            <el-timeline>
              <el-timeline-item v-for="a in trace.activities" :key="a.id"
                :timestamp="a.occurDate" :type="actType(a.activityType)" placement="top">
                <div class="act-line">
                  <el-tag size="small" :type="actType(a.activityType)">{{ a.activityType }}</el-tag>
                  <span class="dim">{{ a.operatorName || '-' }}</span>
                </div>
                <ul v-if="a.inputs?.length" class="input-list">
                  <li v-for="iu in a.inputs" :key="iu.inputItemId">
                    <code>{{ iu.inputItemCode }}</code> {{ iu.inputItemName }}
                    <span v-if="iu.activeIngredient" class="dim small">({{ iu.activeIngredient }})</span>
                    <span class="dim">{{ fmtQty(iu.qty) }} {{ iu.unit }}</span>
                    <el-tag v-if="iu.phiDays > 0" size="small" type="warning" style="margin-left:4px">PHI {{ iu.phiDays }}d</el-tag>
                  </li>
                </ul>
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>

        <!-- 4. Harvest -->
        <div v-if="trace.harvest" class="node node-harvest">
          <div class="node-icon"><el-icon :size="22"><Wheat /></el-icon></div>
          <div class="node-body">
            <div class="node-title">{{ t('trace.harvest') }}</div>
            <div class="node-row">
              <span class="label">{{ t('trace.code') }}</span>
              <code>{{ trace.harvest.code }}</code>
              <span class="label" style="margin-left:16px">{{ t('trace.date') }}</span>
              <span>{{ trace.harvest.harvestDate }}</span>
              <span class="label" style="margin-left:16px">{{ t('trace.qty') }}</span>
              <strong>{{ fmtQty(trace.harvest.qtyKg) }} kg</strong>
            </div>
            <div v-if="!publicMode && trace.harvest.operatorName" class="node-row">
              <span class="label">{{ t('trace.operator') }}</span>
              <span>{{ trace.harvest.operatorName }}</span>
              <span v-if="trace.harvest.locationGps" class="label" style="margin-left:16px">{{ t('trace.gps') }}</span>
              <span v-if="trace.harvest.locationGps" class="dim small">{{ trace.harvest.locationGps }}</span>
            </div>
          </div>
        </div>

        <!-- 5. Packing -->
        <div v-if="trace.packings?.length" class="node node-pack">
          <div class="node-icon"><el-icon :size="22"><Box /></el-icon></div>
          <div class="node-body">
            <div class="node-title">{{ t('trace.packing') }} ({{ trace.packings.length }})</div>
            <table class="trace-table">
              <thead><tr>
                <th>{{ t('trace.code') }}</th><th>{{ t('trace.date') }}</th>
                <th>SKU</th><th>{{ t('trace.qty') }}</th>
              </tr></thead>
              <tbody>
                <tr v-for="p in trace.packings" :key="p.id">
                  <td><code>{{ p.code }}</code></td>
                  <td>{{ p.packDate }}</td>
                  <td>{{ p.skuCode }} <span class="dim">{{ p.skuName }}</span></td>
                  <td><strong>{{ fmtQty(p.qty) }} {{ p.unit }}</strong></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 6. Sales orders -->
        <div v-if="!publicMode && trace.orders?.length" class="node node-sale">
          <div class="node-icon"><el-icon :size="22"><Wallet /></el-icon></div>
          <div class="node-body">
            <div class="node-title">{{ t('trace.sales') }} ({{ trace.orders.length }})</div>
            <table class="trace-table">
              <thead><tr>
                <th>{{ t('trace.code') }}</th><th>{{ t('trace.date') }}</th>
                <th>{{ t('trace.customer') }}</th><th>{{ t('common.status') }}</th>
              </tr></thead>
              <tbody>
                <tr v-for="o in trace.orders" :key="o.id">
                  <td><code>{{ o.code }}</code></td>
                  <td>{{ o.orderDate }}</td>
                  <td>{{ o.customerName }}</td>
                  <td><el-tag size="small">{{ o.status }}</el-tag></td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

      </div>

      <div v-if="publicMode" class="footer-cert">
        <el-icon><CircleCheckFilled /></el-icon>
        <span>{{ t('trace.publicFooter') }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search as SearchIcon, LocationFilled, Box, Sunny, Wallet,
  InfoFilled, CircleCheckFilled, ArrowDown } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { getTrace, getPublicTrace } from '@/api/trace'
import { downloadBatchPdf, downloadBatchXlsx } from '@/api/gapReport'

// Custom icon stub (lucide Wheat doesn't exist in Element Plus, use Sunny again)
const Wheat = Sunny

const props = defineProps({
  publicMode: { type: Boolean, default: false },
})

const { t } = useI18n()
const route = useRoute()
const trace = ref(null)
const loading = ref(false)
const batchCodeInput = ref('')
const qrCanvas = ref(null)
const qrUrl = ref('')

function fmtQty(v) { return v==null?'-':Number(v).toLocaleString(undefined,{maximumFractionDigits:3}) }
function batchStatusTag(s) { return ({active:'success', sold:'info', closed:'info', draft:'warning'}[s]) || 'info' }
function actType(t_) { return ({spray:'danger', fertilize:'success', sow:'info', water:'primary', weed:'warning'})[t_] || 'info' }

async function load(code) {
  if (!code) return
  loading.value = true
  try {
    const fn = props.publicMode ? getPublicTrace : getTrace
    trace.value = await fn(code)
    // build QR
    qrUrl.value = `${window.location.origin}/trace/${encodeURIComponent(code)}`
    await nextTick()
    if (qrCanvas.value) {
      QRCode.toCanvas(qrCanvas.value, qrUrl.value, { width: 140, margin: 1 })
    }
  } catch (e) {
    ElMessage.error(e?.message || 'Trace not found')
    trace.value = null
  } finally {
    loading.value = false
  }
}

function onSearch() {
  if (!batchCodeInput.value) return
  load(batchCodeInput.value.trim())
}

function reload() {
  if (trace.value?.batch?.code) load(trace.value.batch.code)
}

function downloadQr() {
  if (!qrCanvas.value) return
  const link = document.createElement('a')
  link.href = qrCanvas.value.toDataURL('image/png')
  link.download = `qr-${trace.value.batch.code}.png`
  link.click()
}

async function onGapExport(format) {
  const code = trace.value?.batch?.code
  if (!code) return
  const fn = format === 'pdf' ? downloadBatchPdf : downloadBatchXlsx
  const ext = format === 'pdf' ? 'pdf' : 'xlsx'
  const blob = await fn(code)
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `GAP-${code}.${ext}`
  a.click()
  window.URL.revokeObjectURL(url)
  ElMessage.success(t('gap.downloadSuccess'))
}

onMounted(() => {
  // route param :code (public route) or query ?code= (internal direct link)
  const code = route.params.code || route.query.code
  if (code) {
    batchCodeInput.value = code
    load(code)
  }
})
</script>

<style scoped>
.trace-page { padding: 16px; max-width: 980px; margin: 0 auto; }
.trace-page.public-mode { padding: 24px 16px; }
.trace-header { display:flex; justify-content:space-between; align-items:flex-end; margin-bottom: 16px; }
.trace-title { font-size: 22px; font-weight: 700; color: #1f2937; margin: 0; }
.trace-sub { color: #6b7280; font-size: 13px; margin: 4px 0 0; }
.search-card { margin-bottom: 16px; }
.empty { text-align: center; padding: 48px; color: #94a3b8; }
.empty p { margin-top: 12px; font-size: 14px; }

.batch-card { margin-bottom: 20px; background: linear-gradient(135deg, #f0fdf4 0%, #fff 60%); border: 1px solid #dcfce7; }
.batch-grid { display: grid; grid-template-columns: 1fr auto; gap: 24px; align-items: center; }
.batch-label { font-size: 12px; color: #16a34a; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
.batch-code { font-size: 22px; font-weight: 700; color: #14532d; margin: 4px 0 8px; font-family: 'Monaco', 'Consolas', monospace; }
.batch-meta { display: flex; gap: 12px; align-items: center; margin-bottom: 10px; font-size: 13px; }
.meta-item { color: #475569; }
.batch-qty { font-size: 28px; font-weight: 700; color: #15803d; margin-bottom: 6px; }
.qr-box { text-align: center; }
.qr-box canvas { display: block; }
.qr-url { font-size: 10px; color: #94a3b8; word-break: break-all; max-width: 140px; margin-top: 4px; }

.chain { display: flex; flex-direction: column; gap: 16px; }
.node { display: grid; grid-template-columns: 48px 1fr; gap: 12px; padding: 16px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; }
.node-icon { display: flex; align-items: flex-start; justify-content: center; color: #3266ad; }
.node-origin .node-icon { color: #16a34a; }
.node-inbound .node-icon { color: #92400e; }
.node-activity .node-icon { color: #b91c1c; }
.node-harvest .node-icon { color: #15803d; }
.node-pack .node-icon { color: #1e40af; }
.node-sale .node-icon { color: #7e22ce; }
.node-title { font-size: 15px; font-weight: 600; color: #1f2937; margin-bottom: 8px; }
.node-hint { font-size: 12px; color: #94a3b8; margin: 0 0 8px; }
.node-row { display: flex; align-items: center; gap: 8px; font-size: 13px; margin-bottom: 4px; flex-wrap: wrap; }
.label { color: #94a3b8; font-size: 11px; text-transform: uppercase; }
.sub-card { background: #fafafa; border: 1px dashed #e5e7eb; border-radius: 4px; padding: 8px 12px; margin-bottom: 8px; font-size: 13px; }
.input-list { list-style: none; padding: 0; margin: 6px 0 0 0; font-size: 12px; }
.input-list li { padding: 2px 0; color: #475569; }
.trace-table { width: 100%; font-size: 13px; border-collapse: collapse; margin-top: 6px; }
.trace-table th, .trace-table td { padding: 6px 10px; border-bottom: 1px solid #f1f5f9; text-align: left; }
.trace-table th { color: #94a3b8; font-weight: 500; font-size: 11px; text-transform: uppercase; }

.act-line { display: flex; gap: 8px; align-items: center; margin-bottom: 4px; font-size: 13px; }
.code-tag { font-size: 11px; background: #fef3c7; padding: 1px 6px; border-radius: 3px; color: #92400e; }
code { font-family: 'Monaco', 'Consolas', monospace; font-size: 12px; }
.dim { color: #94a3b8; }
.small { font-size: 11px; }

.footer-cert { display: flex; gap: 8px; align-items: center; justify-content: center; margin-top: 24px; padding: 16px; background: #f0fdf4; border-radius: 8px; color: #166534; font-size: 13px; }
</style>
