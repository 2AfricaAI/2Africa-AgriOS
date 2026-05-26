<template>
  <div class="page" v-loading="loading">
    <!-- Hero header -->
    <div class="hero">
      <div>
        <div class="hero-title">
          <el-icon><Bell /></el-icon>
          {{ t('actionBoard.title') }}
        </div>
        <div class="hero-subtitle">{{ t('actionBoard.subtitle') }}</div>
      </div>
      <el-button type="primary" :icon="RefreshIcon" :loading="refreshing" @click="onRefresh">
        {{ refreshing ? t('actionBoard.refreshing') : t('actionBoard.refresh') }}
      </el-button>
    </div>

    <!-- Tabs -->
    <el-card shadow="never" class="board-card">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane
          v-for="tab in TABS"
          :key="tab.key"
          :name="tab.key"
        >
          <template #label>
            <span class="tab-label">
              {{ tab.label }}
              <el-badge
                v-if="counts[tab.key] > 0"
                :value="counts[tab.key]"
                :type="tab.badgeType"
                class="tab-badge"
              />
            </span>
          </template>

          <div v-if="(tabItems[tab.key] || []).length === 0" class="empty">
            {{ tab.empty }}
          </div>

          <div v-else class="action-list">
            <div
              v-for="item in tabItems[tab.key]"
              :key="item.id"
              class="action-card"
              :class="`severity-${item.severity}`"
            >
              <div class="action-left">
                <div class="severity-dot" :class="`dot-${item.severity}`"></div>
              </div>

              <div class="action-body">
                <div class="action-head">
                  <span class="action-title">{{ item.title }}</span>
                  <el-tag size="small" type="info" effect="plain" class="rule-chip">
                    {{ item.ruleCode }} · {{ ruleLabel(item.ruleCode) }}
                  </el-tag>
                  <el-tag size="small" :type="severityTag(item.severity)" effect="dark" class="severity-chip">
                    {{ severityLabel(item.severity) }}
                  </el-tag>
                  <el-tag v-if="item.ownerRole" size="small" type="warning" effect="plain">
                    {{ t('actionBoard.ownerRole') }}: {{ ownerLabel(item.ownerRole) }}
                  </el-tag>
                </div>
                <div v-if="item.description" class="action-desc">{{ item.description }}</div>
                <div class="action-meta">
                  <span v-if="item.refCode" class="meta-item">
                    <code class="ref-code">{{ item.refCode }}</code>
                  </span>
                  <a
                    v-if="refLink(item)"
                    class="meta-link"
                    :href="refLink(item)"
                    @click.prevent="goToRef(item)"
                  >
                    {{ t('actionBoard.refLink') }} →
                  </a>
                  <span v-if="item.dueDate" class="meta-item dim">
                    {{ t('actionBoard.dueDate') }}: {{ item.dueDate }}
                  </span>
                </div>
              </div>

              <div class="action-right">
                <el-button
                  type="success" size="small" :icon="CheckIcon"
                  @click="onDone(item)"
                >
                  {{ t('actionBoard.markDone') }}
                </el-button>
                <el-button
                  size="small" :icon="CloseIcon"
                  @click="onDismiss(item)"
                >
                  {{ t('actionBoard.dismiss') }}
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Bell,
  Refresh as RefreshIcon,
  Check as CheckIcon,
  Close as CloseIcon,
} from '@element-plus/icons-vue'
import { listActions, refreshActions, markActionDone, dismissAction } from '@/api/operations'

const { t } = useI18n()
const router = useRouter()

const TABS = computed(() => [
  { key: 'today',     label: t('actionBoard.tabToday'),     badgeType: 'danger',  empty: t('actionBoard.emptyToday') },
  { key: 'week_risk', label: t('actionBoard.tabWeekRisk'),  badgeType: 'warning', empty: t('actionBoard.emptyWeekRisk') },
  { key: 'followup',  label: t('actionBoard.tabFollowup'),  badgeType: 'primary', empty: t('actionBoard.emptyFollowup') },
  { key: 'pause',     label: t('actionBoard.tabPause'),     badgeType: 'info',    empty: t('actionBoard.emptyPause') },
])

const activeTab = ref('today')
const loading = ref(false)
const refreshing = ref(false)

// 每个 tab 的数据 + 计数
const tabItems = reactive({ today: [], week_risk: [], followup: [], pause: [] })
const counts = reactive({ today: 0, week_risk: 0, followup: 0, pause: 0 })

function severityTag(s) {
  return s === 'high' ? 'danger' : s === 'medium' ? 'warning' : 'info'
}
function severityLabel(s) {
  return t(`actionBoard.severity${s.charAt(0).toUpperCase() + s.slice(1)}`)
}
function ownerLabel(role) {
  return t(`actionBoard.owner${role.charAt(0).toUpperCase() + role.slice(1)}`, role)
}
function ruleLabel(code) {
  return t(`rule.${code}`, code)
}

// 跳到来源对象的详情/列表
function refLink(item) {
  if (!item.refType) return null
  if (item.refType === 'inventory') return '/packhouse/inventory'
  if (item.refType === 'customer')  return '/sales/customers'
  if (item.refType === 'batch' && item.refId) return `/production/batches/${item.refId}`
  if (item.refType === 'sku')       return '/packhouse/packings'
  if (item.refType === 'order' && item.refId)  return `/sales/orders/${item.refId}`
  return null
}
function goToRef(item) {
  const href = refLink(item)
  if (href) router.push(href)
}

async function loadAll() {
  loading.value = true
  try {
    // 4 个 tab 各拉一次,size=200 够看了
    const results = await Promise.all(TABS.value.map(t =>
      listActions({ category: t.key, status: 'open', page: 1, size: 200 })
    ))
    TABS.value.forEach((tab, i) => {
      tabItems[tab.key] = results[i].list || []
      counts[tab.key]   = results[i].total || 0
    })
  } catch (e) {
    // 全局 axios 拦截器已 toast
  } finally {
    loading.value = false
  }
}

async function onRefresh() {
  refreshing.value = true
  try {
    const { triggered } = await refreshActions()
    await loadAll()
    ElMessage.success(t('actionBoard.refreshed', { n: triggered }))
  } catch {} finally {
    refreshing.value = false
  }
}

async function onDone(item) {
  try {
    await markActionDone(item.id, '')
    ElMessage.success(t('actionBoard.doneSuccess'))
    await loadAll()
  } catch {}
}

async function onDismiss(item) {
  await ElMessageBox.confirm(
    t('actionBoard.confirmDismiss'),
    t('common.tip'),
    { type: 'warning' }
  ).catch(() => Promise.reject('cancel'))
  try {
    await dismissAction(item.id, '')
    ElMessage.success(t('actionBoard.dismissSuccess'))
    await loadAll()
  } catch (e) { if (e === 'cancel') return }
}

function onTabChange(_) { /* no-op, data already loaded */ }

onMounted(loadAll)
</script>

<style scoped>
.page { display: flex; flex-direction: column; gap: 16px; }

.hero {
  background:
    radial-gradient(circle at 90% 20%, rgba(255, 255, 255, 0.15), transparent 40%),
    linear-gradient(135deg, #0f3a26 0%, #1f7a35 55%, #2BA84A 100%);
  color: #fff;
  padding: 20px 24px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 6px 20px rgba(15, 58, 38, 0.22);
}
.hero-title {
  font-size: 20px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.hero-subtitle { font-size: 13px; opacity: .85; margin-top: 4px; }
.hero :deep(.el-button) {
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(255, 255, 255, 0.3);
  color: #fff;
}

.board-card :deep(.el-card__body) { padding: 8px 16px 16px; }

.tab-label { display: inline-flex; align-items: center; gap: 6px; }
.tab-badge { margin-left: 4px; }

.empty {
  color: #c0c4cc;
  font-size: 14px;
  text-align: center;
  padding: 60px 0;
}

.action-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 6px 2px;
}

.action-card {
  display: flex;
  gap: 14px;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  background: #fff;
  align-items: flex-start;
  transition: box-shadow .15s;
}
.action-card:hover { box-shadow: 0 4px 12px rgba(0, 21, 41, 0.06); }
.action-card.severity-high   { border-left: 3px solid #f56c6c; }
.action-card.severity-medium { border-left: 3px solid #e6a23c; }
.action-card.severity-low    { border-left: 3px solid #909399; }

.action-left { padding-top: 6px; }
.severity-dot {
  width: 10px; height: 10px; border-radius: 50%;
}
.dot-high   { background: #f56c6c; }
.dot-medium { background: #e6a23c; }
.dot-low    { background: #909399; }

.action-body { flex: 1; min-width: 0; }
.action-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}
.action-title {
  font-weight: 600;
  font-size: 14px;
  color: #1f2329;
  margin-right: 4px;
}
.rule-chip, .severity-chip { flex-shrink: 0; }

.action-desc {
  margin-top: 4px;
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
}

.action-meta {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #606266;
}
.meta-item { display: inline-flex; align-items: center; }
.meta-link {
  color: #2BA84A;
  text-decoration: none;
  font-weight: 600;
}
.meta-link:hover { text-decoration: underline; }

.ref-code {
  font-family: 'Consolas', monospace;
  background: #f5f7fa;
  padding: 1px 6px;
  border-radius: 3px;
  color: #1f7a35;
  font-size: 11px;
}

.dim { color: #909399; }

.action-right {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
  padding-top: 2px;
}
</style>
