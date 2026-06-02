<template>
  <!--
    Reusable "Coming Soon" placeholder used by CS module stub pages
    (Team / Analytics / Complaints / Settings) introduced in Sprint 48b.
    Each consuming page passes its own icon / title / description / version /
    optional callout via props. Keeps stub design consistent so when the real
    feature lands the page can be swapped wholesale.
  -->
  <div class="stub-page">
    <div class="stub-card">
      <div class="stub-icon">
        <el-icon :size="56" color="#0f3a26">
          <component :is="icon" />
        </el-icon>
      </div>
      <h2 class="stub-title">{{ title }}</h2>
      <p class="stub-desc">{{ description }}</p>
      <div class="stub-meta">
        <el-tag size="small" type="info">{{ version }}</el-tag>
        <el-tag v-if="sprint" size="small" type="warning" effect="plain">{{ sprint }}</el-tag>
      </div>

      <div v-if="callout" class="stub-callout">
        <el-icon><InfoFilled /></el-icon>
        <div>
          <div class="stub-callout-title">{{ callout.title }}</div>
          <div class="stub-callout-body">
            {{ callout.body }}
            <router-link v-if="callout.linkTo" :to="callout.linkTo" class="stub-callout-link">
              {{ callout.linkText }}
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { InfoFilled } from '@element-plus/icons-vue'

defineProps({
  /** Element-Plus icon component (passed in by parent). */
  icon: { type: [Object, Function], required: true },
  title: { type: String, required: true },
  description: { type: String, default: '' },
  version: { type: String, default: 'Coming Soon' },
  sprint: { type: String, default: '' },
  /**
   * Optional inline callout for a related feature or workaround.
   * Shape: { title, body, linkText, linkTo }
   */
  callout: { type: Object, default: null },
})
</script>

<style scoped>
.stub-page {
  min-height: calc(100vh - 100px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.stub-card {
  max-width: 560px;
  width: 100%;
  background: #fff;
  border: 1px solid #e6ece9;
  border-radius: 12px;
  padding: 48px 40px;
  text-align: center;
}
.stub-icon {
  margin-bottom: 16px;
}
.stub-title {
  margin: 0 0 12px 0;
  font-size: 22px;
  color: #0f3a26;
  font-weight: 600;
}
.stub-desc {
  margin: 0 0 20px 0;
  color: #5b6b62;
  font-size: 14px;
  line-height: 1.7;
}
.stub-meta {
  display: flex;
  gap: 8px;
  justify-content: center;
  margin-bottom: 24px;
}
.stub-callout {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  text-align: left;
  background: #f6faf8;
  border: 1px solid #d6e3dc;
  border-radius: 8px;
  padding: 14px 16px;
  margin-top: 8px;
  font-size: 13px;
}
.stub-callout :deep(.el-icon) {
  color: #0f3a26;
  margin-top: 2px;
}
.stub-callout-title {
  font-weight: 600;
  color: #1c2e25;
  margin-bottom: 4px;
}
.stub-callout-body {
  color: #5b6b62;
  line-height: 1.6;
}
.stub-callout-link {
  color: #0f3a26;
  font-weight: 600;
  text-decoration: none;
  margin-left: 4px;
}
.stub-callout-link:hover {
  text-decoration: underline;
}
</style>
