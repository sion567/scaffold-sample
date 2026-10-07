<template>
  <el-scrollbar
    ref="scrollContainer"
    :vertical="false"
    class="scroll-container"
    @wheel.prevent="handleScroll"
  >
    <slot />
  </el-scrollbar>
</template>

<script setup lang="ts">
import type { ScrollbarInstance } from 'element-plus'
import type { TagView } from '../../types'
import useTagsViewStore from '@/store/modules/tagsView'

const tagAndTagSpacing = ref(4)

const scrollContainer = ref<ScrollbarInstance>()

// el-scrollbar 的滚动包裹层（挂载后必然存在，缺失时机与原 proxy.$refs 访问一致）
const scrollWrapper = computed<HTMLDivElement>(() => scrollContainer.value!.wrapRef!)

const emits = defineEmits<{
  (e: 'scroll'): void
  (e: 'updateArrows'): void
}>()

onMounted(() => {
  scrollWrapper.value.addEventListener('scroll', emitScroll, true)
})

onBeforeUnmount(() => {
  scrollWrapper.value.removeEventListener('scroll', emitScroll)
})

const emitScroll = () => {
  emits('scroll')
  emits('updateArrows')
}

function smoothScrollTo(target: number) {
  const $scrollWrapper = scrollWrapper.value
  const start = $scrollWrapper.scrollLeft
  const distance = target - start
  const duration = 300
  let startTime: number | null = null

  function ease(t: number, b: number, c: number, d: number) {
    t /= d / 2
    if (t < 1) return c / 2 * t * t + b
    t--
    return -c / 2 * (t * (t - 2) - 1) + b
  }

  function step(timestamp: number) {
    if (!startTime) startTime = timestamp
    const elapsed = timestamp - startTime
    $scrollWrapper.scrollLeft = ease(elapsed, start, distance, duration)
    if (elapsed < duration) {
      requestAnimationFrame(step)
    } else {
      $scrollWrapper.scrollLeft = target
      emits('updateArrows')
    }
  }

  requestAnimationFrame(step)
}

function handleScroll(e: WheelEvent & { wheelDelta?: number }) {
  const eventDelta = e.wheelDelta || -e.deltaY * 40
  const $scrollWrapper = scrollWrapper.value
  $scrollWrapper.scrollLeft = $scrollWrapper.scrollLeft + eventDelta / 4
  emits('updateArrows')
}

const tagsViewStore = useTagsViewStore()
const visitedViews = computed(() => tagsViewStore.visitedViews)

function moveToTarget(currentTag: TagView) {
  const $container = scrollContainer.value!.$el as HTMLElement
  const $containerWidth = $container.offsetWidth
  const $scrollWrapper = scrollWrapper.value

  let firstTag: TagView | null = null
  let lastTag: TagView | null = null

  if (visitedViews.value.length > 0) {
    firstTag = visitedViews.value[0]
    lastTag = visitedViews.value[visitedViews.value.length - 1]
  }

  if (firstTag === currentTag) {
    smoothScrollTo(0)
  } else if (lastTag === currentTag) {
    smoothScrollTo($scrollWrapper.scrollWidth - $containerWidth)
  } else {
    const tagListDom = document.getElementsByClassName('tags-view-item')
    const currentIndex = visitedViews.value.findIndex(item => item === currentTag)
    let prevTag: HTMLElement | null = null
    let nextTag: HTMLElement | null = null
    for (const k in tagListDom) {
      if (k !== 'length' && Object.hasOwnProperty.call(tagListDom, k)) {
        if ((tagListDom[k] as HTMLElement).dataset.path === visitedViews.value[currentIndex - 1].path) {
          prevTag = tagListDom[k] as HTMLElement
        }
        if ((tagListDom[k] as HTMLElement).dataset.path === visitedViews.value[currentIndex + 1].path) {
          nextTag = tagListDom[k] as HTMLElement
        }
      }
    }
    const afterNextTagOffsetLeft = (nextTag as HTMLElement).offsetLeft + (nextTag as HTMLElement).offsetWidth + tagAndTagSpacing.value
    const beforePrevTagOffsetLeft = (prevTag as HTMLElement).offsetLeft - tagAndTagSpacing.value
    if (afterNextTagOffsetLeft > $scrollWrapper.scrollLeft + $containerWidth) {
      smoothScrollTo(afterNextTagOffsetLeft - $containerWidth)
    } else if (beforePrevTagOffsetLeft < $scrollWrapper.scrollLeft) {
      smoothScrollTo(beforePrevTagOffsetLeft)
    }
  }
}

function scrollToStart() {
  smoothScrollTo(0)
}

function scrollToEnd() {
  const $scrollWrapper = scrollWrapper.value
  smoothScrollTo($scrollWrapper.scrollWidth - $scrollWrapper.clientWidth)
}

function getScrollState() {
  const $scrollWrapper = scrollWrapper.value
  return {
    canLeft: $scrollWrapper.scrollLeft > 0,
    canRight: $scrollWrapper.scrollLeft < $scrollWrapper.scrollWidth - $scrollWrapper.clientWidth - 1
  }
}

defineExpose({
  moveToTarget,
  scrollToStart,
  scrollToEnd,
  getScrollState
})
</script>

<style lang='scss' scoped>
.scroll-container {
  white-space: nowrap;
  position: relative;
  overflow: hidden;
  width: 100%;
  :deep(.el-scrollbar__bar) {
    bottom: 0px;
  }
  :deep(.el-scrollbar__wrap) {
    height: 34px;
    display: flex;
    align-items: center;
  }
}
</style>