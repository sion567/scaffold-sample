<template>
  <inner-link
    v-for="(item, index) in tagsViewStore.iframeViews"
    :key="item.path"
    :iframeId="'iframe' + index"
    v-show="route.path === item.path"
    :src="iframeUrl(item.meta.link, item.query)"
  ></inner-link>
</template>

<script setup lang="ts">
import InnerLink from "../InnerLink/index.vue"
import useTagsViewStore from "@/store/modules/tagsView"

const route = useRoute()
const tagsViewStore = useTagsViewStore()

function iframeUrl(url?: unknown, query?: Record<string, unknown>) {
  // meta.link 运行时为 URL 字符串（store 类型标注为 boolean 仅为兼容），非字符串按空串兜底
  const base = typeof url === 'string' ? url : ''
  if (query && Object.keys(query).length > 0) {
    let params = Object.keys(query).map((key) => key + "=" + String(query[key])).join("&")
    return base + "?" + params
  }
  return base
}
</script>
