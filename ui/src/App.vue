<template>
  <router-view />
  <blind-watermark />
</template>

<script setup lang="ts">
import useSettingsStore from '@/store/modules/settings'
import { handleThemeStyle } from '@/utils/theme'
import BlindWatermark from '@/components/BlindWatermark/index.vue'

onMounted(() => {
  nextTick(() => {
    // 初始化主题样式
    handleThemeStyle(useSettingsStore().theme)
  })
})
</script>
