<template>
  <div class="mt-wrap">
    <header class="mt-header">
      <h2>{{ t('m.myTasks') }}</h2>
      <button class="mt-refresh" :class="{ spinning: refreshing }" @click="onRefresh">
        ↻
      </button>
    </header>

    <!-- segmented tabs -->
    <div class="mt-tabs">
      <button
        v-for="tab in TABS" :key="tab.key"
        class="mt-tab" :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        <span>{{ t(tab.labelKey) }}</span>
        <span v-if="counts[tab.key]" class="mt-tab-count">{{ counts[tab.key] }}</span>
      </button>
    </div>

    <!-- list -->
    <div v-if="loading" class="mt-empty">{{ t('common.loading') }}</div>
    <div v-else-if="visible.length === 0" class="mt-empty">
      <div class="mt-empty-icon">✅</div>
      <div>{{ t('m.noTasks') }}</div>
    </div>
    <div v-else class="mt-list">
      <article
        v-for="item in visible" :key="item.id"
        class="mt-card" :class="`sev-${item.severity}`"
        @click="toggle(item.id)"
      >
        <div class="mt-card-row">
          <span class="mt-sev-chip" :class="`chip-${item.severity}`">
            {{ t('m.sev' + cap(item.severity)) }}
          </span>
          <span class="mt-rule-chip">{{ item.ruleCode }}</span>
          <span v-if="item.ownerRole" class="mt-owner-chip">{{ item.ownerRole }}</span>
          <span v-if="item.dueDate" class="mt-due">{{ daysToDue(item.dueDate) }}</span>
        </div>
        <div class="mt-card-title">{{ item.title }}</div>
        <!-- collapse: description + actions -->
        <div v-if="expandedId === item.id" class="mt-card-detail" @click.stop>
          <div class="mt-card-desc">{{ item.description }}</div>
          <div class="mt-actions">
            <button class="mt-btn mt-btn-ghost" :disabled="busyId === item.id" @click="onDismiss(item)">
              ✕ {{ t('m.dismiss') }}
            </button>
            <button class="mt-btn mt-btn-primary" :disabled="busyId === item.id" @click="onDone(item)">
              ✓ {{ t('m.markDone') }}
            </button>
          </div>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { listActions, markActionDone, dismissAction, refreshActions } from '@/api/operations'

const { t } = useI18n()

const TABS = [
  { key: 'today',     labelKey: 'm.tabToday' },
  { key: 'week_risk', labelKey: 'm.tabRisk'  },
  { key: 'followup',  labelKey: 'm.tabFollow'},
]
const activeTab = ref('today')
const items = ref([])
const loading = ref(false)
const refreshing = ref(false)
const expandedId = ref(null)
const busyId = ref(null)

const counts = computed(() => {
  const out = { today: 0, week_risk: 0, followup: 0 }
  for (const it of items.value) {
    if (out[it.category] !== undefined) out[it.category] += 1
  }
  return out
})

const visible = computed(() =>
  items.value
    .filter(it => it.category === activeTab.value)
    // sort by severity (high > medium > low), then due date asc
    .sort((a, b) => {
      const sevOrder = { high: 0, medium: 1, low: 2 }
      const sa = sevOrder[a.severity] ?? 99
      const sb = sevOrder[b.severity] ?? 99
      if (sa !== sb) return sa - sb
      return (a.dueDate || '9999') < (b.dueDate || '9999') ? -1 : 1
    })
)

async function load() {
  loading.value = true
  try {
    const data = await listActions({ status: 'open', page: 1, size: 200 })
    items.value = data?.list || []
    console.log('[MobileTasks] loaded:', items.value.length)
  } catch (e) {
    console.error('[MobileTasks] load failed:', e)
    items.value = []
  } finally {
    loading.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  try {
    const res = await refreshActions().catch(() => null)
    if (res) console.log('[MobileTasks] rules recomputed:', res)
    await load()
    ElMessage.success(t('common.refresh') + ' ✓')
  } finally {
    refreshing.value = false
  }
}

function toggle(id) {
  expandedId.value = expandedId.value === id ? null : id
}

async function onDone(item) {
  busyId.value = item.id
  try {
    await markActionDone(item.id, '')
    items.value = items.value.filter(x => x.id !== item.id)
    ElMessage.success(t('m.markedDone'))
  } catch (e) {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    busyId.value = null
    expandedId.value = null
  }
}
async function onDismiss(item) {
  busyId.value = item.id
  try {
    await dismissAction(item.id, '')
    items.value = items.value.filter(x => x.id !== item.id)
    ElMessage.success(t('m.dismissed'))
  } catch (e) {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    busyId.value = null
    expandedId.value = null
  }
}

// helpers
function cap(s) { return s ? s.charAt(0).toUpperCase() + s.slice(1) : '' }
function daysToDue(iso) {
  if (!iso) return ''
  const today = new Date(); today.setHours(0, 0, 0, 0)
  const d = new Date(iso); d.setHours(0, 0, 0, 0)
  const diff = Math.round((d - today) / 86400000)
  if (diff === 0)  return t('m.dueToday')
  if (diff < 0)    return t('m.overdueDays', { n: -diff })
  if (diff === 1)  return t('m.dueTomorrow')
  return t('m.inDays', { n: diff })
}

onMounted(load)
</script>

<style scoped>
.mt-wrap {
  padding: 12px 12px 20px;
  background: #f5f7fa;
  min-height: 100%;
}

/* header */
.mt-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 4px 4px 12px;
}
.mt-header h2 { font-size: 20px; font-weight: 700; color: #1f2937; margin: 0; }
.mt-refresh {
  width: 36px; height: 36px; border-radius: 50%;
  background: #fff; border: 1px solid #e4e7ed;
  font-size: 18px; cursor: pointer; color: #1f7a35;
  display: flex; align-items: center; justify-content: center;
  transition: transform 0.3s ease;
}
.mt-refresh.spinning { transform: rotate(360deg); }

/* tabs */
.mt-tabs {
  display: flex;
  gap: 6px;
  background: #fff;
  padding: 4px;
  border-radius: 10px;
  margin-bottom: 12px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}
.mt-tab {
  flex: 1;
  padding: 8px 4px;
  background: transparent;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  display: flex; align-items: center; justify-content: center; gap: 6px;
}
.mt-tab.active {
  background: #1f7a35;
  color: #fff;
  font-weight: 600;
}
.mt-tab-count {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 9px;
  background: rgba(255,255,255,0.25);
}
.mt-tab:not(.active) .mt-tab-count {
  background: #f0f2f5;
}

/* list */
.mt-list { display: flex; flex-direction: column; gap: 8px; }
.mt-card {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05);
  border-left: 3px solid transparent;
  transition: transform 0.1s ease;
}
.mt-card:active { transform: scale(0.99); }
.mt-card.sev-high   { border-left-color: #f56c6c; }
.mt-card.sev-medium { border-left-color: #e6a23c; }
.mt-card.sev-low    { border-left-color: #909399; }

.mt-card-row {
  display: flex; flex-wrap: wrap; gap: 6px;
  margin-bottom: 6px;
  font-size: 11px;
}
.mt-sev-chip {
  padding: 1px 6px; border-radius: 9px;
  color: #fff; font-weight: 600;
}
.chip-high   { background: #f56c6c; }
.chip-medium { background: #e6a23c; }
.chip-low    { background: #909399; }
.mt-rule-chip {
  padding: 1px 6px;
  background: #f0f2f5;
  border-radius: 4px;
  color: #909399;
  font-family: monospace;
}
.mt-owner-chip {
  padding: 1px 6px;
  background: #ecfdf5;
  color: #16a34a;
  border-radius: 4px;
  font-size: 10px;
  text-transform: uppercase;
}
.mt-due { margin-left: auto; color: #909399; font-size: 11px; }

.mt-card-title {
  font-size: 14px;
  color: #1f2937;
  line-height: 1.4;
}

.mt-card-detail {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e4e7ed;
}
.mt-card-desc {
  font-size: 12px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 10px;
}
.mt-actions {
  display: flex; gap: 8px;
}
.mt-btn {
  flex: 1;
  padding: 10px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  border: none;
}
.mt-btn-ghost {
  background: #f0f2f5;
  color: #909399;
}
.mt-btn-primary {
  background: linear-gradient(135deg, #1f7a35 0%, #15803d 100%);
  color: #fff;
}
.mt-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.mt-btn:active { transform: scale(0.98); }

/* empty */
.mt-empty {
  padding: 60px 20px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}
.mt-empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}
</style>
