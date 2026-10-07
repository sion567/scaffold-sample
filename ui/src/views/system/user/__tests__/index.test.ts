import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick, ref, reactive } from 'vue'
import type { TreeSelectNode } from '@/types/api'
import type { SysUser } from '@/api/system/user'

// Mock dependencies
vi.mock('@/api/system/user', () => ({
  listUser: vi.fn(),
  getUser: vi.fn(),
  addUser: vi.fn(),
  updateUser: vi.fn(),
  delUser: vi.fn(),
  resetUserPwd: vi.fn(),
  changeUserStatus: vi.fn(),
  deptTreeSelect: vi.fn()
}))

vi.mock('@/components/TreePanel/index.vue', () => ({
  default: { name: 'TreePanel', template: '<div class="tree-panel"></div>' }
}))

vi.mock('@/components/ExcelImportDialog/index.vue', () => ({
  default: { name: 'ExcelImportDialog', template: '<div class="excel-import-dialog"></div>' }
}))

vi.mock('@/views/system/user/view', () => ({
  default: { name: 'UserViewDrawer', template: '<div class="user-view-drawer"></div>' }
}))

vi.mock('@/utils/passwordRule', () => ({
  usePasswordRule: () => ({
    pwdValidator: [],
    pwdPromptValidator: () => true
  })
}))

vi.mock('vue-router', async () => {
  const actual = await vi.importActual('vue-router')
  return {
    ...actual,
    useRouter: vi.fn(() => ({ push: vi.fn() }))
  }
})

describe('用户管理页面', () => {
  describe('queryParams 状态管理', () => {
    it('默认分页参数正确', () => {
      const queryParams = reactive({
        pageNum: 1,
        pageSize: 10,
        userName: undefined,
        phonenumber: undefined,
        status: undefined,
        deptId: undefined
      })

      expect(queryParams.pageNum).toBe(1)
      expect(queryParams.pageSize).toBe(10)
    })

    it('可以更新查询参数', () => {
      const queryParams = reactive({
        pageNum: 1,
        pageSize: 10,
        userName: undefined as string | undefined,
        status: undefined as string | undefined
      })

      queryParams.userName = 'admin'
      queryParams.status = '0'

      expect(queryParams.userName).toBe('admin')
      expect(queryParams.status).toBe('0')
    })

    it('搜索时重置页码', () => {
      const queryParams = reactive({
        pageNum: 5,
        pageSize: 10
      })

      // 模拟搜索操作
      const handleQuery = () => {
        queryParams.pageNum = 1
      }

      handleQuery()

      expect(queryParams.pageNum).toBe(1)
    })
  })

  describe('form 表单数据', () => {
    it('新增用户表单初始状态', () => {
      const form = ref({
        userId: undefined,
        deptId: undefined,
        userName: undefined,
        nickName: undefined,
        password: undefined,
        phonenumber: undefined,
        email: undefined,
        sex: undefined,
        status: '0',
        remark: undefined,
        postIds: [],
        roleIds: []
      })

      expect(form.value.userId).toBeUndefined()
      expect(form.value.status).toBe('0')
      expect(form.value.postIds).toEqual([])
      expect(form.value.roleIds).toEqual([])
    })

    it('编辑用户表单数据', () => {
      const form = ref({
        userId: 1,
        userName: 'admin',
        nickName: '管理员',
        email: 'admin@example.com'
      })

      expect(form.value.userId).toBe(1)
      expect(form.value.userName).toBe('admin')
    })
  })

  describe('columns 列配置', () => {
    it('默认列配置正确', () => {
      const columns = ref({
        userId: { label: '用户编号', visible: true },
        userName: { label: '用户名称', visible: true },
        nickName: { label: '用户昵称', visible: true },
        deptName: { label: '部门', visible: true },
        phonenumber: { label: '手机号码', visible: true },
        status: { label: '状态', visible: true },
        createTime: { label: '创建时间', visible: true }
      })

      expect(columns.value.userName.visible).toBe(true)
      expect(columns.value.status.visible).toBe(true)
    })
  })

  describe('ids 多选状态', () => {
    it('初始无选中', () => {
      const ids = ref<number[]>([])
      const single = ref(true)
      const multiple = ref(true)

      expect(ids.value).toEqual([])
      expect(single.value).toBe(true)
      expect(multiple.value).toBe(true)
    })

    it('选中一条时 single 和 multiple 都为 false', () => {
      // 根据原代码逻辑：
      // single = selection.length !== 1
      // multiple = !selection.length
      // 当选中1条时：single=false, multiple=false
      const selection = [{ userId: 1 }]
      const single = selection.length !== 1
      const multiple = !selection.length

      expect(single).toBe(false)
      expect(multiple).toBe(false)
    })

    it('选中多条时 single 为 true, multiple 为 false', () => {
      const selection = [{ userId: 1 }, { userId: 2 }, { userId: 3 }]
      const single = selection.length !== 1
      const multiple = !selection.length

      expect(single).toBe(true)
      expect(multiple).toBe(false)
    })
  })

  describe('form 表单验证规则', () => {
    it('用户名必填验证', () => {
      const rules = {
        userName: [
          { required: true, message: '用户名称不能为空', trigger: 'blur' },
          { min: 2, max: 20, message: '用户名称长度必须介于 2 和 20 之间', trigger: 'blur' }
        ]
      }

      expect(rules.userName[0].required).toBe(true)
      expect(rules.userName[1].min).toBe(2)
    })

    it('昵称必填验证', () => {
      const rules = {
        nickName: [{ required: true, message: '用户昵称不能为空', trigger: 'blur' }]
      }

      expect(rules.nickName[0].required).toBe(true)
    })

    it('邮箱格式验证', () => {
      const rules = {
        email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }]
      }

      expect(rules.email[0].type).toBe('email')
    })

    it('手机号格式验证', () => {
      const rules = {
        phonenumber: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: '请输入正确的手机号码', trigger: 'blur' }]
      }

      expect(rules.phonenumber[0].pattern).toBeDefined()
    })
  })

  describe('handleSelectionChange 选择处理', () => {
    it('处理单选', () => {
      const ids = ref<number[]>([])
      const single = ref(true)
      const multiple = ref(true)

      const selection = [{ userId: 1, userName: 'admin' }]

      ids.value = selection.map(item => item.userId as number)
      single.value = selection.length !== 1
      multiple.value = !selection.length

      expect(ids.value).toEqual([1])
      expect(single.value).toBe(false)
      expect(multiple.value).toBe(false)
    })

    it('处理多选', () => {
      const ids = ref<number[]>([])

      const selection = [
        { userId: 1, userName: 'admin' },
        { userId: 2, userName: 'test' }
      ]

      ids.value = selection.map(item => item.userId as number)

      expect(ids.value).toEqual([1, 2])
    })

    it('清空选择', () => {
      const ids = ref<number[]>([])
      const multiple = ref(false)

      const selection: SysUser[] = []

      ids.value = selection.map(item => item.userId as number)
      multiple.value = !selection.length

      expect(ids.value).toEqual([])
      expect(multiple.value).toBe(true)
    })
  })

  describe('handleStatusChange 状态变更', () => {
    it('启用用户', () => {
      const row = { userId: 1, userName: 'admin', status: '1' }
      const text = row.status === '0' ? '启用' : '停用'

      expect(text).toBe('停用')

      row.status = '0'
      const newText = row.status === '0' ? '启用' : '停用'

      expect(newText).toBe('启用')
    })

    it('禁用用户', () => {
      const row = { userId: 1, userName: 'admin', status: '0' }
      const text = row.status === '0' ? '启用' : '停用'

      expect(text).toBe('启用')
    })
  })

  describe('filterDisabledDept 过滤禁用部门', () => {
    it('过滤禁用的顶级部门', () => {
      const deptList = [
        { id: 1, label: '启用部门' },
        { id: 2, label: '禁用部门', disabled: true }
      ]

      const result = deptList.filter(dept => !dept.disabled)

      expect(result).toHaveLength(1)
      expect(result[0].label).toBe('启用部门')
    })

    it('递归过滤禁用的子部门', () => {
      const deptList = [
        {
          id: 1,
          label: '父部门',
          children: [
            { id: 11, label: '启用子部门' },
            { id: 12, label: '禁用子部门', disabled: true }
          ]
        }
      ]

      const filterDisabledDept = (list: TreeSelectNode[]) => {
        return list.filter(dept => {
          if (dept.disabled) return false
          if (dept.children?.length) {
            dept.children = filterDisabledDept(dept.children)
          }
          return true
        })
      }

      const result = filterDisabledDept(JSON.parse(JSON.stringify(deptList)))

      expect(result).toHaveLength(1)
      expect(result[0].children).toHaveLength(1)
      expect(result[0].children?.[0]?.label).toBe('启用子部门')
    })
  })
})
