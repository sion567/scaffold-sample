<template>
  <div>
    <template v-for="(item, index) in options">
      <template v-if="isValueMatch(item.value)">
        <span
          v-if="(item.elTagType == 'default' || item.elTagType == '') && (item.elTagClass == '' || item.elTagClass == null)"
          :key="item.value"
          :index="index"
          :class="item.elTagClass"
        >{{ item.label + " " }}</span>
        <el-tag
          v-else
          :disable-transitions="true"
          :key="item.value + ''"
          :index="index"
          :type="item.elTagType"
          :class="item.elTagClass"
        >{{ item.label + " " }}</el-tag>
      </template>
    </template>
    <template v-if="unmatch && showValue">
      {{ handleArray(unmatchArray) }}
    </template>
  </div>
</template>

<script setup lang="ts">
import type { DictDataOption } from '@/types/api'

// 记录未匹配的项
const unmatchArray = ref<(string | number)[]>([])

interface DictTagProps {
  // 数据
  options?: DictDataOption[]
  // 当前的值
  value?: number | string | (string | number)[] | null
  // 当未找到匹配的数据时，显示value
  showValue?: boolean
  // 分隔符
  separator?: string
}

const props = withDefaults(defineProps<DictTagProps>(), {
  options: () => [],
  value: undefined,
  showValue: true,
  separator: ','
})

const values = computed<(string | number)[]>(() => {
  if (props.value === null || typeof props.value === 'undefined' || props.value === '') return []
  if (typeof props.value === 'number' || typeof props.value === 'boolean') return [props.value]
  return Array.isArray(props.value) ? props.value.map(item => '' + item) : String(props.value).split(props.separator)
})

const unmatch = computed(() => {
  unmatchArray.value = []
  // 没有value不显示
  if (props.value === null || typeof props.value === 'undefined' || props.value === '' || !Array.isArray(props.options) || props.options.length === 0) return false
  // 传入值为数组
  let unmatch = false // 添加一个标志来判断是否有未匹配项
  values.value.forEach(item => {
    if (!props.options.some(v => v.value == item)) {
      unmatchArray.value.push(item)
      unmatch = true // 如果有未匹配项，将标志设置为true
    }
  })
  return unmatch // 返回标志的值
})

function handleArray(array: (string | number)[]): string {
  if (array.length === 0) return ''
  return array.map(item => String(item)).reduce((pre, cur) => pre + ' ' + cur)
}

function isValueMatch(itemValue: string | number) {
  return values.value.some(val => val == itemValue)
}
</script>

<style scoped>
.el-tag + .el-tag {
  margin-left: 10px;
}
</style>
