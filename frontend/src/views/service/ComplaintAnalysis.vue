<template>
  <StubPage
    :icon="Warning"
    :title="t('cs.complaintsStubTitle')"
    :description="t('cs.complaintsStubDesc')"
    sprint="Sprint 50"
    :callout="{
      title: t('cs.complaintsStubCalloutTitle'),
      body: t('cs.complaintsStubCalloutBody'),
      linkText: t('cs.complaintsStubCalloutLink'),
      linkTo: '/qc/complaints',
    }"
  />
</template>

<script setup>
import { useI18n } from 'vue-i18n'
import { Warning } from '@element-plus/icons-vue'
import StubPage from './stubs/StubPage.vue'

const { t } = useI18n()
</script>
