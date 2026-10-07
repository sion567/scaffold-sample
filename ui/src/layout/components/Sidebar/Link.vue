<template>
  <component :is="type" v-bind="linkProps()">
    <slot />
  </component>
</template>

<script setup lang="ts">
import { isExternal } from '@/utils/validate'

/** 外链地址或内部路由对象 */
type LinkTo = string | Record<string, unknown>

interface Props {
  to: LinkTo
}

const props = defineProps<Props>()

const isExt = computed<boolean>(() => {
  // 对象形式的 to（内部路由）恒为非外链，与原运行时正则测试对象的结果一致
  return typeof props.to === 'string' && isExternal(props.to)
})

const type = computed<'a' | 'router-link'>(() => {
  if (isExt.value) {
    return 'a'
  }
  return 'router-link'
})

function linkProps(): { href: string; target: string; rel: string } | { to: LinkTo } {
  if (isExt.value) {
    return {
      href: props.to as string,
      target: '_blank',
      rel: 'noopener'
    }
  }
  return {
    to: props.to
  }
}
</script>
