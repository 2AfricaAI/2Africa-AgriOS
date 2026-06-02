<template>
  <StubPage
    :icon="DataLine"
    :title="t('cs.analyticsStubTitle')"
    :description="t('cs.analyticsStubDesc')"
    sprint="Sprint 49"
    :callout="{
      title: t('cs.analyticsStubCalloutTitle'),
      body: t('cs.analyticsStubCalloutBody'),
    }"
  />
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { DataLine } from '@element-plus/icons-vue'
import StubPage from './stubs/StubPage.vue'

const { t } = useI18n()
</script>
