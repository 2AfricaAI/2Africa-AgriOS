<template>
  <div class="mh">
    <!-- 问候卡 -->
    <div class="mh-hero">
      <div class="mh-greet">{{ greeting }} 👋</div>
      <div class="mh-name">{{ auth.nickname || auth.username }}</div>
      <div class="mh-date">{{ todayLabel }}</div>
    </div>

    <!-- 4 个快捷入口 -->
    <div class="mh-grid">
      <router-link to="/m/activity/new" class="mh-card mh-act">
        <div class="mh-card-emoji">📝</div>
        <div class="mh-card-label">{{ t('m.recordActivity') }}</div>
      </router-link>
      <router-link to="/m/harvest/new" class="mh-card mh-harv">
        <div class="mh-card-emoji">🌾</div>
        <div class="mh-card-label">{{ t('m.recordHarvest') }}</div>
      </router-link>
      <router-link to="/m/tasks" class="mh-card mh-tasks">
        <div class="mh-card-emoji">✓</div>
        <div class="mh-card-label">{{ t('m.myTasks') }}</div>
        <div v-if="taskCount > 0" class="mh-card-badge">{{ taskCount }}</div>
      </router-link>
      <a href="#" class="mh-card mh-desktop" @click.prevent="goDesktop">
        <div class="mh-card-emoji">💻</div>
        <div class="mh-card-label">{{ t('m.desktop') }}</div>
      </a>
    </div>

    <!-- 今日要做 (前 3 条 action_item) -->
    <div class="mh-section">
      <div class="mh-section-head">
        <span class="mh-section-title">📌 {{ t('m.todayList') }}</span>
        <router-link to="/m/tasks" class="mh-section-more">{{ t('m.viewAll') }} →</router-link>
      </div>
      <div v-if="loadingTasks" class="mh-loading">{{ t('common.loading') }}</div>
      <div v-else-if="topTasks.length === 0" class="mh-empty">
        🎉 {{ t('m.noTasks') }}
      </div>
      <div v-else class="mh-task-list">
        <router-link
          v-for="ai in topTasks" :key="ai.id"
          :to="`/m/tasks`" class="mh-task"
          :class="`sev-${ai.severity}`"
        >
          <div class="mh-task-head">
            <span class="mh-task-rule">{{ ai.ruleCode }}</span>
            <span class="mh-task-sev" :class="`sev-tag-${ai.severity}`">{{ sevLabel(ai.severity) }}</span>
          </div>
          <div class="mh-task-title">{{ ai.title }}</div>
        </router-link>
      </div>
    </div>

    <!-- 同步队列状态 -->
    <div v-if="pendingSubmits > 0" class="mh-sync-banner">
      ⏳ {{ t('m.syncPending', { n: pendingSubmits }) }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { listActions } from '@/api/operations'

const { t } = useI18n()
const router = useRouter()
const auth = useAuthStore()

// 问候语
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 4)            return t('m.greetNight')           // 00-04 deep night
  if (h < 7)            return t('m.greetEarlyMorning')    // 04-07 farm wakeup
  if (h < 12)           return t('m.greetMorning')         // 07-12 morning
  if (h < 18)           return t('m.greetAfternoon')       // 12-18 afternoon
  if (h < 22)           return t('m.greetEvening')         // 18-22 evening
  return t('m.greetNight')                                  // 22-24 working late
})
const todayLabel = computed(() => {
  const d = new Date()
  return d.toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
})

// 今日任务 (category=today, 前 3)
const loadingTasks = ref(false)
const topTasks = ref([])
const taskCount = ref(0)

async function loadTasks() {
  loadingTasks.value = true
  try {
    const data = await listActions({ status: 'open', category: 'today', page: 1, size: 3 })
    topTasks.value = data.list || []
    taskCount.value = data.total || 0
  } catch { topTasks.value = [] } finally { loadingTasks.value = false }
}
onMounted(loadTasks)

function sevLabel(s) {
  const map = { high: t('m.sevHigh'), medium: t('m.sevMedium'), low: t('m.sevLow') }
  return map[s] || s
}

function goDesktop() { router.push('/') }

// 待提交队列 (待 20.7 接入 IndexedDB)
const pendingSubmits = ref(0)
</script>

<style scoped>
.mh { display: flex; flex-direction: column; gap: 16px; }

/* 问候卡 */
.mh-hero {
  background:
    radial-gradient(circle at 90% 20%, rgba(255,255,255,0.2), transparent 40%),
    linear-gradient(135deg, #1f7a35 0%, #2BA84A 70%);
  color: #fff;
  padding: 18px 20px;
  border-radius: 16px;
  box-shadow: 0 6px 16px rgba(31, 122, 53, 0.25);
}
.mh-greet { font-size: 14px; opacity: 0.85; }
.mh-name { font-size: 22px; font-weight: 700; margin: 4px 0; }
.mh-date { font-size: 12px; opacity: 0.85; }

/* 4 个快捷入口 */
.mh-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.mh-card {
  position: relative;
  background: #fff;
  border-radius: 14px;
  padding: 18px;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  gap: 8px;
  text-decoration: none;
  color: #1f2329;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  min-height: 100px;
  transition: transform 0.1s, box-shadow 0.15s;
}
.mh-card:active { transform: scale(0.97); }
.mh-card-emoji { font-size: 32px; }
.mh-card-label { font-size: 13px; font-weight: 600; text-align: center; }
.mh-card-badge {
  position: absolute;
  top: 10px; right: 12px;
  min-width: 22px; height: 22px;
  padding: 0 6px;
  background: #f56c6c;
  color: #fff; font-size: 12px; font-weight: 700;
  border-radius: 11px;
  display: flex; align-items: center; justify-content: center;
}
.mh-act    { border-top: 3px solid #1f7a35; }
.mh-harv   { border-top: 3px solid #fa8c16; }
.mh-tasks  { border-top: 3px solid #e6a23c; }
.mh-desktop { border-top: 3px solid #909399; }

/* 今日要做 */
.mh-section { background: #fff; border-radius: 14px; padding: 14px; box-shadow: 0 2px 8px rgba(0,0,0,0.06); }
.mh-section-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.mh-section-title { font-weight: 700; font-size: 14px; color: #1f2329; }
.mh-section-more { color: #1f7a35; text-decoration: none; font-size: 12px; }
.mh-loading, .mh-empty { color: #909399; font-size: 13px; text-align: center; padding: 16px; }

.mh-task-list { display: flex; flex-direction: column; gap: 8px; }
.mh-task {
  display: block;
  text-decoration: none;
  background: #f7faf8;
  border-left: 3px solid #909399;
  padding: 10px 12px;
  border-radius: 6px;
  color: #1f2329;
}
.mh-task.sev-high   { border-left-color: #f56c6c; background: #fef0f0; }
.mh-task.sev-medium { border-left-color: #e6a23c; background: #fff8e6; }
.mh-task.sev-low    { border-left-color: #909399; }
.mh-task-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px; }
.mh-task-rule { font-family: monospace; font-size: 10px; color: #909399; }
.mh-task-sev { font-size: 10px; padding: 2px 6px; border-radius: 4px; font-weight: 600; }
.sev-tag-high   { background: #f56c6c; color: #fff; }
.sev-tag-medium { background: #e6a23c; color: #fff; }
.sev-tag-low    { background: #909399; color: #fff; }
.mh-task-title { font-size: 13px; font-weight: 500; line-height: 1.4; }

/* 同步队列 */
.mh-sync-banner {
  background: #fff8e6;
  border: 1px dashed #e6a23c;
  color: #b88230;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  text-align: center;
}
</style>
