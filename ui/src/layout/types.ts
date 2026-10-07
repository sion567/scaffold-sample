/**
 * 布局模块共享类型（仅 src/layout 内部使用）
 *
 * TagView 统一使用 tagsView store 的权威定义，保证 tab 插件返回值与
 * 布局组件入参完全同型，无需任何类型转换。
 */
export type { TagView } from '@/store/modules/tagsView'
