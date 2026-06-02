<template>
  <!--
    CS Settings hub. Sprint 48b ships only the Inbox tab (which reuses the
    existing InboxList page). Future tabs (AI Agent, Business Hours,
    WhatsApp Policy) appear as disabled placeholders so users see the
    roadmap without seeing dead links.
  -->
  <div class="cs-settings">
    <header class="page-head">
      <h2 class="page-title">{{ t('cs.settingsTitle') }}</h2>
      <p class="page-sub">{{ t('cs.settingsSub') }}</p>
    </header>

    <el-tabs v-model="active" class="settings-tabs">
      <el-tab-pane name="inboxes" :label="t('cs.settingsTabInboxes')">
        <InboxList />
      </el-tab-pane>
      <el-tab-pane name="ai" :label="t('cs.settingsTabAi')" disabled>
        <div class="tab-stub">
          <el-icon><MagicStick /></el-icon>
          <span>{{ t('cs.settingsTabAiSoon') }}</span>
        </div>
      </el-tab-pane>
      <el-tab-pane name="hours" :label="t('cs.settingsTabHours')" disabled>
        <div class="tab-stub">
          <el-icon><Clock /></el-icon>
          <span>{{ t('cs.settingsTabHoursSoon') }}</span>
        </div>
      </el-tab-pane>
      <el-tab-pane name="policy" :label="t('cs.settingsTabPolicy')" disabled>
        <div class="tab-stub">
          <el-icon><Lock /></el-icon>
          <span>{{ t('cs.settingsTabPolicySoon') }}</span>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { MagicStick, Clock, Lock } from '@element-plus/icons-vue'
import InboxList from './InboxList.vue'

const { t } = useI18n()
const active = ref('inboxes')
</script>

<style scoped>
.cs-settings {
  padding: 0 4px;
}
.page-head {
  margin-bottom: 16px;
}
.page-title {
  margin: 0;
  font-size: 18px;
  color: #0f3a26;
}
.page-sub {
  margin: 4px 0 0 0;
  color: #5b6b62;
  font-size: 13px;
}
.settings-tabs :deep(.el-tabs__nav-wrap)::after {
  background-color: #e6ece9;
}
.tab-stub {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 40px;
  color: #8a9690;
  font-size: 14px;
  background: #f8faf9;
  border-radius: 8px;
  justify-content: center;
}
</style>
