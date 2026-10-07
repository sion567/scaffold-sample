<template>
  <el-col :span="element.span" :class="className" @click.stop="activeItem(element)">
    <el-form-item
:label="element.label" :label-width="element.labelWidth ? element.labelWidth + 'px' : null"
      :required="element.required" v-if="element.layout === 'colFormItem'">
      <render :key="element.tag" :conf="element" :model-value="element.defaultValue" @update:model-value="(v: unknown) => emits('update-field', 'defaultValue', v)" />
    </el-form-item>
    <el-row :gutter="element.gutter" :class="element.class" @click.stop="activeItem(element)" v-else>
      <span class="component-name"> {{ element.componentName }} </span>
      <draggable
group="componentsGroup" :animation="340" :list="element.children" class="drag-wrapper" item-key="label"
        ref="draggableItemRef" :component-data="getComponentData()">
        <template #item="scoped">
          <draggable-item
:key="scoped.element.renderKey" :drawing-list="element.children ?? []" :element="scoped.element"
            :index="index" :active-id="activeId" :form-conf="formConf" @activeItem="activeItem(scoped.element)"
            @copyItem="copyItem(scoped.element, element.children)"
            @deleteItem="deleteItem(scoped.index, element.children)"
            @update-field="(k, v) => setByPath(scoped.element, k, v)" />
        </template>
      </draggable>
    </el-row>
    <span class="drawing-item-copy" title="复制" @click.stop="copyItem(element)">
      <el-icon><CopyDocument /></el-icon>
    </span>
    <span class="drawing-item-delete" title="删除" @click.stop="deleteItem(index)">
      <el-icon><Delete /></el-icon>
    </span>
  </el-col>
</template>
<script setup lang="ts" name="DraggableItem">
import draggable from "vuedraggable/dist/vuedraggable.common"
import render from '@/utils/generator/render'
import type { ElementConfig, FormConf } from '@/utils/generator/config'

interface Props {
  /** 当前渲染的组件配置 */
  element: ElementConfig
  /** 在 drawingList 中的下标 */
  index: number
  /** 所属绘制列表 */
  drawingList: ElementConfig[]
  /** 当前选中组件的 formId */
  activeId?: string | number
  /** 表单全局配置 */
  formConf: FormConf
}

const props = defineProps<Props>()

const className = ref('')
const draggableItemRef = ref<ComponentPublicInstance | null>(null)
const emits = defineEmits<{
  (e: 'activeItem', element: ElementConfig): void
  (e: 'copyItem', element: ElementConfig, parent: ElementConfig[]): void
  (e: 'deleteItem', index: number, parent: ElementConfig[]): void
  (e: 'update-field', key: string, value: unknown): void
}>()

/** 按点分路径设置对象字段（递归子项 update-field 的落点） */
function setByPath(target: ElementConfig, key: string, value: unknown) {
  const keys = key.split('.')
  let cur = target as Record<string, unknown>
  for (let i = 0; i < keys.length - 1; i++) {
    cur = cur[keys[i]] as Record<string, unknown>
  }
  cur[keys[keys.length - 1]] = value
}

function activeItem(item: ElementConfig) {
  emits('activeItem', item)
}
function copyItem(item: ElementConfig, parent?: ElementConfig[]) {
  emits('copyItem', item, parent ?? props.drawingList)
}
function deleteItem(item: number, parent?: ElementConfig[]) {
  emits('deleteItem', item, parent ?? props.drawingList)
}

function getComponentData() {
  return {
    gutter: props.element.gutter,
    justify: props.element.justify,
    align: props.element.align
  }
}

watch(() => props.activeId, (val) => {
  className.value = (props.element.layout === 'rowFormItem' ? 'drawing-row-item' : 'drawing-item') + (val === props.element.formId ? ' active-from-item' : '')
  if (props.formConf.unFocusedComponentBorder) {
    className.value += ' unfocus-bordered'
  }
}, { immediate: true })
</script>