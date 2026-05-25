<template>
  <span class="date-range" :class="{ 'is-empty': !start && !end }">
    <span v-if="start" class="d">{{ start }}</span>
    <span v-else class="d-empty">—</span>

    <span class="sep">{{ separator }}</span>

    <span v-if="end" class="d">{{ end }}</span>
    <span v-else class="d-empty">—</span>
  </span>
</template>

<script setup>
/**
 * 通用日期范围展示
 *
 * 用法:
 *   <DateRange :start="row.planStartDate" :end="row.planHarvestDate" />
 *
 * Props:
 *   - start (string)     起始日期, 通常 'YYYY-MM-DD'. null 显示为 —
 *   - end   (string)     结束日期
 *   - separator (string) 分隔符,默认 →. 可选: →, ~, —, to
 */
defineProps({
  start:     { type: String, default: '' },
  end:       { type: String, default: '' },
  separator: { type: String, default: '→' },
})
</script>

<style scoped>
.date-range {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-variant-numeric: tabular-nums;  /* 数字等宽,日期对齐更整齐 */
}

.d {
  color: #1f2329;
}

.d-empty {
  color: #c0c4cc;
}

.sep {
  color: #909399;
  font-size: 13px;
  user-select: none;
}

.is-empty {
  color: #c0c4cc;
}
</style>
