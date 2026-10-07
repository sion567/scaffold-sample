<template>
  <div class="tree-sidebar" :class="{ collapsed: collapsed, resizing: isResizing, 'no-initial-transition': isLoadingFromStorage}" :style="{ width: sidebarWidth + 'px' }">
    <!-- 右侧拖动条 -->
    <div v-if="!collapsed" class="resize-handle" @mousedown="startResize" @touchstart="startResize" :class="{ active: isResizing }" />
    <div class="tree-header">
      <span class="tree-title" v-show="!collapsed">
        <el-icon><component :is="titleIcon" /></el-icon> {{ title }}
      </span>
      <div class="tree-actions" v-show="!collapsed">
        <el-tooltip :content="isExpandedAll ? '收起全部' : '展开全部'" placement="right">
          <el-icon class="tree-action-icon" @click="toggleExpandAll">
            <ArrowDown v-if="isExpandedAll" />
            <ArrowUp v-else />
          </el-icon>
        </el-tooltip>
        <el-tooltip content="刷新" placement="right">
          <el-icon class="tree-action-icon" @click="handleRefresh"><Refresh /></el-icon>
        </el-tooltip>
        <slot name="actions"></slot>
      </div>
    </div>
    
    <!-- 侧边栏展开/收起按钮 -->
    <div class="collapse-button-container">
      <el-tooltip :content="collapsed ? '展开' : '收起'" placement="right">
        <el-icon class="collapse-button" @click="toggleCollapsed">
          <DArrowRight v-if="collapsed" />
          <DArrowLeft v-else />
        </el-icon>
      </el-tooltip>
    </div>

    <div class="tree-search" v-show="!collapsed" v-if="showSearch">
      <el-input v-model="searchKeyword" :placeholder="searchPlaceholder" clearable>
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <div class="tree-wrap" v-show="!collapsed">
      <el-tree 
        ref="treeRef" 
        :data="treeData" 
        :props="treeProps" 
        :expand-on-click-node="expandOnClickNode"
        :filter-node-method="filterNodeMethod"
        :default-expand-all="defaultExpandAll"
        :default-expanded-keys="defaultExpandedKeys"
        :node-key="nodeKey"
        :check-strictly="checkStrictly"
        :show-checkbox="showCheckbox"
        @node-click="onNodeClick"
        @check="onCheck"
        @node-expand="onNodeExpand"
        @node-collapse="onNodeCollapse"
      >
        <template #default="{ node, data }">
          <slot name="node" :node="node" :data="data">
            <span class="tree-node">
              <el-icon class="node-icon">
                <Folder v-if="data.children && data.children.length" />
                <Document v-else />
              </el-icon>
              <span class="node-label" :title="node.label">{{ node.label }}</span>
            </span>
          </slot>
        </template>
      </el-tree>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'

/** 通用树节点结构（组件内部只访问 label/children/id，业务字段由各页面自行扩展） */
type TreeNodeData = {
  /** 节点显示文本 */
  label?: string
  /** 子节点 */
  children?: TreeNodeData[]
  /** 节点标识 */
  id?: string | number
}

/** el-tree 内部节点（仅声明用到的字段；setup 返回的 root 经实例解包后即该结构） */
type ElTreeNodeLike = {
  expanded?: boolean
  childNodes?: ElTreeNodeLike[]
  [key: string]: unknown
}

/** el-tree 实例最小结构（与 element-plus TreeInstance 结构兼容） */
type ElTreeLike = {
  root: ElTreeNodeLike
  filter(value: string): void
  setCurrentKey(key?: string | number | null): void
  getCurrentKey(): string | number | null
  getCurrentNode(): unknown | null
  setCheckedKeys(keys: (string | number)[]): void
  getCheckedKeys(): (string | number)[]
  getCheckedNodes(): unknown[]
}

/** el-tree check 事件的选中信息 */
type TreeCheckedInfo = {
  checkedNodes: unknown[]
  checkedKeys: (string | number)[]
  halfCheckedNodes: unknown[]
  halfCheckedKeys: (string | number)[]
}

interface TreePanelProps {
  // 树形数据
  treeData?: TreeNodeData[]
  // 标题
  title?: string
  // 标题图标
  titleIcon?: string | Component
  // 是否显示搜索框
  showSearch?: boolean
  // 搜索框占位符
  searchPlaceholder?: string
  // 是否默认收起侧边栏
  defaultCollapsed?: boolean
  // 树配置项
  treeProps?: { children?: string; label?: string }
  // 节点唯一标识字段
  nodeKey?: string
  // 是否在点击节点时展开或收起
  expandOnClickNode?: boolean
  // 是否显示复选框
  showCheckbox?: boolean
  // 是否严格的遵循父子不互相关联
  checkStrictly?: boolean
  // 是否默认展开所有节点
  defaultExpandAll?: boolean
  // 默认展开的节点的key数组
  defaultExpandedKeys?: (string | number)[]
  // 默认宽度
  defaultWidth?: number
  // 收起时的宽度
  collapsedWidth?: number
  // 最小宽度
  minWidth?: number
  // 最大宽度
  maxWidth?: number
  // 本地存储的宽度key
  storageKey?: string
  // 是否启用本地存储宽度
  enableStorage?: boolean
  // 自定义过滤方法
  filterMethod?: (value: string, data: TreeNodeData) => boolean
}

const props = withDefaults(defineProps<TreePanelProps>(), {
  treeData: () => [],
  title: '树形结构',
  titleIcon: 'OfficeBuilding',
  showSearch: true,
  searchPlaceholder: '请输入名称',
  defaultCollapsed: false,
  treeProps: () => ({ children: 'children', label: 'label' }),
  nodeKey: 'id',
  expandOnClickNode: false,
  showCheckbox: false,
  checkStrictly: false,
  defaultExpandAll: false,
  defaultExpandedKeys: () => [],
  defaultWidth: 220,
  collapsedWidth: 20,
  minWidth: 180,
  maxWidth: 400,
  storageKey: 'tree-sidebar-width',
  enableStorage: true,
  filterMethod: undefined
})

const emit = defineEmits<{
  (e: 'collapsed-change', collapsed: boolean): void
  (e: 'expanded-all-change', expanded: boolean): void
  (e: 'refresh'): void
  (e: 'node-click', data: TreeNodeData, node: unknown, event: Event): void
  (e: 'check', data: TreeNodeData, checkedInfo: TreeCheckedInfo): void
  (e: 'node-expand', data: TreeNodeData, node: unknown, event: Event): void
  (e: 'node-collapse', data: TreeNodeData, node: unknown, event: Event): void
  (e: 'search', keyword: string): void
}>()

const treeRef = ref<ElTreeLike | null>(null)

// 响应式数据
const searchKeyword = ref('')
const collapsed = ref(props.defaultCollapsed)
const sidebarWidth = ref(props.defaultCollapsed ? props.collapsedWidth : props.defaultWidth)
const isResizing = ref(false)
const startX = ref(0)
const startWidth = ref(0)
const saveWidthTimer = ref<ReturnType<typeof setTimeout> | null>(null)
const rafId = ref<number | null>(null)
const isLoadingFromStorage = ref(false)
const expandedAll = ref(props.defaultExpandAll)

// 计算属性
const isExpandedAll = computed({
  get: () => expandedAll.value,
  set: (val: boolean) => {
    expandedAll.value = val
  }
})

// 节点过滤方法
const filterNodeMethod = (value: string, data: TreeNodeData): boolean => {
  if (props.filterMethod) {
    return props.filterMethod(value, data)
  }
  if (!value) return true
  return !!data.label && data.label.indexOf(value) !== -1
}

// 监听折叠状态
watch(collapsed, (newVal, oldVal) => {
  if (newVal !== oldVal) {
    handleCollapseChange(newVal)
    emit('collapsed-change', newVal)
  }
})

// 监听内部展开状态变化，触发实际树的展开/收起
watch(expandedAll, (newVal) => {
  nextTick(() => {
    if (newVal) {
      expandAllNodes()
    } else {
      collapseAllNodes()
    }
  })
  emit('expanded-all-change', newVal)
})

// 监听搜索关键词
watch(searchKeyword, (val) => {
  if (treeRef.value) {
    treeRef.value.filter(val)
    emit('search', val)
  }
})

// 清理定时器和动画帧
const cleanup = () => {
  if (rafId.value) {
    cancelAnimationFrame(rafId.value)
    rafId.value = null
  }
  if (saveWidthTimer.value) {
    clearTimeout(saveWidthTimer.value)
    saveWidthTimer.value = null
  }
}

// 处理收起/展开状态变化
const handleCollapseChange = (isCollapsed: boolean) => {
  if (isCollapsed) {
    saveWidthToStorage()
    sidebarWidth.value = props.collapsedWidth
  } else {
    const savedWidth = getSavedWidth()
    sidebarWidth.value = savedWidth !== null ? savedWidth : props.defaultWidth
  }
}

// 获取保存的宽度
const getSavedWidth = () => {
  if (!props.enableStorage) {
    return null
  }
  try {
    const savedWidth = localStorage.getItem(props.storageKey)
    if (savedWidth) {
      const width = parseInt(savedWidth, 10)
      if (!isNaN(width) && width >= props.minWidth && width <= props.maxWidth) {
        return width
      }
    }
  } catch (error) {
    console.warn(`Failed to load sidebar width from storage with key ${props.storageKey}:`, error)
  }
  return null
}

// 保存宽度到本地存储
const saveWidthToStorage = () => {
  if (collapsed.value || !props.enableStorage) return
  try {
    localStorage.setItem(props.storageKey, sidebarWidth.value.toString())
  } catch (error) {
    console.warn(`Failed to save sidebar width to storage with key ${props.storageKey}:`, error)
  }
}

// 切换侧边栏收起/展开状态
const toggleCollapsed = () => {
  collapsed.value = !collapsed.value
}

// 切换展开/折叠所有节点
const toggleExpandAll = () => {
  expandedAll.value = !expandedAll.value
}

// 展开所有节点
const expandAllNodes = () => {
  if (!treeRef.value) return
  const allNodes = getAllNodes(treeRef.value.root)
  allNodes.forEach(node => {
    if (node.expanded !== undefined && !node.expanded) {
      node.expanded = true
    }
  })
}

// 获取所有节点
const getAllNodes = (rootNode: ElTreeNodeLike) => {
  const nodes: ElTreeNodeLike[] = []
  const traverse = (node: ElTreeNodeLike) => {
    if (!node) return
    nodes.push(node)
    if (node.childNodes && node.childNodes.length) {
      node.childNodes.forEach(child => traverse(child))
    }
  }
  traverse(rootNode)
  return nodes
}

// 收起所有节点
const collapseAllNodes = () => {
  if (!treeRef.value) return
  const allNodes = getAllNodes(treeRef.value.root)
  allNodes.forEach(node => {
    if (node.expanded !== undefined && node.expanded) {
      node.expanded = false
    }
  })
}

// 处理刷新操作
const handleRefresh = () => {
  emit('refresh')
}

// 节点点击事件
const onNodeClick = (data: TreeNodeData, node: unknown, e: Event) => {
  emit('node-click', data, node, e)
}

// 复选框选中事件
const onCheck = (data: TreeNodeData, checkedInfo: TreeCheckedInfo) => {
  emit('check', data, checkedInfo)
}

// 节点展开事件
const onNodeExpand = (data: TreeNodeData, node: unknown, e: Event) => {
  emit('node-expand', data, node, e)
}

// 节点折叠事件
const onNodeCollapse = (data: TreeNodeData, node: unknown, e: Event) => {
  emit('node-collapse', data, node, e)
}

const setCurrentKey = (key: string | number | null) => {
  if (treeRef.value) {
    treeRef.value.setCurrentKey(key)
  }
}

const getCurrentNode = () => {
  if (treeRef.value) {
    return treeRef.value.getCurrentNode()
  }
  return null
}

const getCurrentKey = () => {
  if (treeRef.value) {
    return treeRef.value.getCurrentKey()
  }
  return null
}

const setCheckedKeys = (keys: (string | number)[]) => {
  if (treeRef.value && props.showCheckbox) {
    treeRef.value.setCheckedKeys(keys)
  }
}

const getCheckedKeys = (): (string | number)[] => {
  if (treeRef.value && props.showCheckbox) {
    return treeRef.value.getCheckedKeys()
  }
  return []
}

const getCheckedNodes = (): unknown[] => {
  if (treeRef.value && props.showCheckbox) {
    return treeRef.value.getCheckedNodes()
  }
  return []
}

const clearSearch = () => {
  searchKeyword.value = ""
  if (treeRef.value) {
    treeRef.value.filter("")
  }
}

const filter = (value: string) => {
  searchKeyword.value = value
}

const startResize = (e: MouseEvent | TouchEvent) => {
  e.preventDefault()
  e.stopPropagation()
  isResizing.value = true
  startX.value = e instanceof MouseEvent ? e.clientX : e.touches[0].clientX
  startWidth.value = sidebarWidth.value

  if (e instanceof MouseEvent) {
    document.addEventListener('mousemove', handleResizeMove)
    document.addEventListener('mouseup', stopResize)
  } else {
    document.addEventListener('touchmove', handleResizeMove, { passive: false })
    document.addEventListener('touchend', stopResize)
  }
  disableUserSelect()
}

const handleResizeMove = (e: MouseEvent | TouchEvent) => {
  if (!isResizing.value) return
  if (rafId.value) {
    cancelAnimationFrame(rafId.value)
  }
  rafId.value = requestAnimationFrame(() => {
    e.preventDefault()
    e.stopPropagation()
    const clientX = e instanceof MouseEvent ? e.clientX : e.touches[0].clientX
    const deltaX = clientX - startX.value
    const newWidth = startWidth.value + deltaX
    const clampedWidth = Math.max(props.minWidth, Math.min(props.maxWidth, newWidth))
    if (Math.abs(clampedWidth - sidebarWidth.value) >= 1) {
      sidebarWidth.value = clampedWidth
    }
  })
}

const stopResize = () => {
  if (!isResizing.value) return
  isResizing.value = false
  if (rafId.value) {
    cancelAnimationFrame(rafId.value)
    rafId.value = null
  }
  startX.value = 0
  startWidth.value = 0
  document.removeEventListener('mousemove', handleResizeMove)
  document.removeEventListener('mouseup', stopResize)
  document.removeEventListener('touchmove', handleResizeMove)
  document.removeEventListener('touchend', stopResize)
  enableUserSelect()
  saveWidthToStorage()
}

// document.body.style 的浏览器前缀属性不在标准 CSSStyleDeclaration 类型里，这里做结构扩展
const bodyStyle = document.body.style as CSSStyleDeclaration & {
  webkitUserSelect: string
  mozUserSelect: string
  msUserSelect: string
}

const disableUserSelect = () => {
  bodyStyle.userSelect = 'none'
  bodyStyle.webkitUserSelect = 'none'
  bodyStyle.mozUserSelect = 'none'
  bodyStyle.msUserSelect = 'none'
}

const enableUserSelect = () => {
  bodyStyle.userSelect = ''
  bodyStyle.webkitUserSelect = ''
  bodyStyle.mozUserSelect = ''
  bodyStyle.msUserSelect = ''
}

const resetWidth = () => {
  sidebarWidth.value = props.defaultWidth
  saveWidthToStorage()
}

const getCurrentWidth = () => {
  return sidebarWidth.value
}

const setWidth = (width: number) => {
  if (typeof width === 'number' && width >= props.minWidth && width <= props.maxWidth) {
    sidebarWidth.value = width
    if (!collapsed.value) {
      saveWidthToStorage()
    }
  }
}

defineExpose({
  setCurrentKey,
  getCurrentNode,
  getCurrentKey,
  setCheckedKeys,
  getCheckedKeys,
  getCheckedNodes,
  clearSearch,
  filter,
  resetWidth,
  getCurrentWidth,
  setWidth,
  expandAllNodes,
  collapseAllNodes,
  toggleCollapsed,
  treeRef
})

onMounted(() => {
  isLoadingFromStorage.value = true
  if (!collapsed.value && props.enableStorage) {
    const savedWidth = getSavedWidth()
    if (savedWidth !== null) {
      sidebarWidth.value = savedWidth
    }
  }
  nextTick(() => {
    isLoadingFromStorage.value = false
  })
  if (expandedAll.value) {
    nextTick(() => {
      expandAllNodes()
    })
  }
})

onBeforeUnmount(() => {
  cleanup()
})
</script>

<style lang="scss" scoped>
.tree-sidebar {
  flex-shrink: 0;
  width: 220px;
  background: #fff;
  border-right: 1px solid #e8eaed;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  transition: width 0.25s ease;
  
  &.collapsed {
    width: 42px;
  }
  
  &.resizing {
    transition: none;
    will-change: width;
    
    * {
      pointer-events: none !important;
    }
  }
  
  &.no-initial-transition {
    transition: none;
  }
}

.resize-handle {
  position: absolute;
  top: 0;
  right: 0;
  width: 6px;
  height: 100%;
  cursor: col-resize;
  z-index: 20;
  background: transparent;
  transition: background 0.2s;
  
  &:hover {
    background: rgba(64, 158, 255, 0.3);
  }
  
  &.active {
    background: rgba(64, 158, 255, 0.5);
  }
}

.collapse-button-container {
  position: absolute;
  top: 50%;
  right: 0;
  transform: translateY(-50%);
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 15px;
  height: 20px;
  background: #fff;
  border-radius: 0 4px 4px 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.2s ease;
  
  .tree-sidebar.collapsed & {
    right: 0;
    background: #f7f8fa;
    border-radius: 0 4px 4px 0;
  }
  
  .tree-sidebar.resizing & {
    pointer-events: none;
  }
}

.collapse-button {
  font-size: 20px;
  color: #909399;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
  
  &:hover {
    color: #409eff;
    background: #ecf5ff;
  }
}

.tree-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 10px;
  height: 40px;
  border-bottom: 1px solid #e8eaed;
  background: #f7f8fa;
  flex-shrink: 0;

  .tree-title {
    font-size: 13px;
    font-weight: 600;
    color: #303133;
    white-space: nowrap;
    overflow: hidden;
    display: flex;
    align-items: center;
    gap: 5px;

    .el-icon {
      color: #409eff;
      font-size: 16px;
    }
  }

  .tree-actions {
    display: flex;
    align-items: center;
    gap: 4px;
    flex-shrink: 0;
  }
}

.tree-action-icon {
  font-size: 20px;
  color: #909399;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;

  &:hover {
    color: #409eff;
    background: #ecf5ff;
  }
}

.tree-search {
  padding: 10px 10px 4px;
  flex-shrink: 0;
}

.tree-wrap {
  flex: 1;
  overflow-y: auto;
  padding: 6px 6px 12px;
  
  .tree-sidebar.resizing & {
    overflow: hidden;
  }

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    background: #dcdfe6;
    border-radius: 4px;
    
    &:hover {
      background: #c0c4cc;
    }
  }

  :deep(.el-tree-node__content) {
    height: 32px;
    border-radius: 4px;
    margin-bottom: 1px;

    &:hover {
      background: #f0f7ff;
    }
  }

  :deep(.el-tree-node.is-current > .el-tree-node__content) {
    background: #e6f0fd;
    color: #409eff;
    font-weight: 600;

    .node-icon {
      color: #409eff !important;
    }
  }
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  overflow: hidden;

  .node-icon {
    font-size: 14px;
    color: #f5a623;
    flex-shrink: 0;
  }

  .node-label {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
