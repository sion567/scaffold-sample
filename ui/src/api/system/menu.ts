import request from '@/utils/request.ts'
import type { AjaxResult, BaseEntity, PageQuery, TreeSelectNode } from '@/types/api'

/** 菜单实体（后端 SysMenu，sys_menu 表） */
export interface SysMenu extends BaseEntity {
  /** 菜单ID */
  menuId?: number
  /** 菜单组（角色授权勾选，提交用） */
  menuIds?: number[]
  /** 菜单名称 */
  menuName?: string
  /** 父菜单名称 */
  parentName?: string
  /** 父菜单ID */
  parentId?: number
  /** 显示顺序 */
  orderNum?: number
  /** 路由地址 */
  path?: string
  /** 组件路径 */
  component?: string
  /** 路由参数 */
  query?: string
  /** 路由名称 */
  routeName?: string
  /** 是否为外链（0是 1否） */
  isFrame?: string
  /** 是否缓存（0缓存 1不缓存） */
  isCache?: string
  /** 菜单类型（M目录 C菜单 F按钮） */
  menuType?: string
  /** 菜单状态（0显示 1隐藏） */
  visible?: string
  /** 菜单状态（0正常 1停用） */
  status?: string
  /** 权限标识 */
  perms?: string
  /** 菜单图标 */
  icon?: string
  /** 子菜单 */
  children?: SysMenu[]
}

/** 菜单列表查询参数 */
export interface SysMenuQuery extends PageQuery {
  /** 菜单名称 */
  menuName?: string
  /** 菜单状态（0正常 1停用） */
  status?: string
  /** 是否显示（菜单树过滤用） */
  visible?: string
}

/** 角色-菜单树响应（后端 roleMenuTreeselect：checkedKeys + menus 平铺） */
export interface RoleMenuTreeResult {
  /** 状态码 */
  code: number
  /** 提示消息 */
  msg: string
  /** 角色已勾选菜单ID */
  checkedKeys: number[]
  /** 菜单下拉树结构 */
  menus: TreeSelectNode[]
}

// 查询菜单列表
export function listMenu(query?: SysMenuQuery): Promise<AjaxResult<SysMenu[]>> {
  return request({
    url: '/system/menu/list',
    method: 'get',
    params: query
  })
}

// 查询菜单详细
export function getMenu(menuId: number | string | number[]): Promise<AjaxResult<SysMenu>> {
  return request({
    url: '/system/menu/' + menuId,
    method: 'get'
  })
}

// 查询菜单下拉树结构
export function treeselect(): Promise<AjaxResult<TreeSelectNode[]>> {
  return request({
    url: '/system/menu/treeselect',
    method: 'get'
  })
}

// 根据角色ID查询菜单下拉树结构
export function roleMenuTreeselect(roleId: number | string | number[]): Promise<RoleMenuTreeResult> {
  return request({
    url: '/system/menu/roleMenuTreeselect/' + roleId,
    method: 'get'
  })
}

// 新增菜单
export function addMenu(data: SysMenu): Promise<AjaxResult<null>> {
  return request({
    url: '/system/menu',
    method: 'post',
    data: data
  })
}

// 修改菜单
export function updateMenu(data: SysMenu): Promise<AjaxResult<null>> {
  return request({
    url: '/system/menu',
    method: 'put',
    data: data
  })
}

// 保存菜单排序
export function updateMenuSort(data: { menuIds: string; orderNums: string }): Promise<AjaxResult<null>> {
  return request({
    url: '/system/menu/updateSort',
    method: 'put',
    data: data
  })
}

// 删除菜单
export function delMenu(menuId: number | string | number[]): Promise<AjaxResult<null>> {
  return request({
    url: '/system/menu/' + menuId,
    method: 'delete'
  })
}
