import { describe, it, expect } from 'vitest'
import {
  parseTime,
  addDateRange,
  selectDictLabel,
  selectDictLabels,
  parseStrEmpty,
  mergeRecursive,
  handleTree,
  tansParams,
  getNormalPath,
  blobValidate,
} from '../ct'

describe('ct.js', () => {
  describe('parseTime', () => {
    it('时间戳转日期字符串', () => {
      const result = parseTime(1704067200000, '{y}-{m}-{d}')
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2}$/)
    })

    it('日期对象转换', () => {
      const date = new Date(2024, 0, 1)
      const result = parseTime(date, '{y}-{m}-{d}')
      expect(result).toBe('2024-01-01')
    })

    it('空值返回 null', () => {
      expect(parseTime()).toBe(null)
      expect(parseTime(null)).toBe(null)
    })
  })

  describe('addDateRange', () => {
    it('添加日期范围参数', () => {
      const params: Record<string, Record<string, unknown>> = {}
      const result = addDateRange(params, ['2024-01-01', '2024-01-31'])
      expect(result.params.beginTime).toBe('2024-01-01')
      expect(result.params.endTime).toBe('2024-01-31')
    })

    it('自定义属性名', () => {
      const params: Record<string, Record<string, unknown>> = {}
      const result = addDateRange(params, ['2024-01-01', '2024-01-31'], 'Time')
      expect(result.params.beginTime).toBe('2024-01-01')
      expect(result.params.endTime).toBe('2024-01-31')
    })

    it('空数组处理', () => {
      const params: Record<string, Record<string, unknown>> = {}
      const result = addDateRange(params, [])
      expect(result.params.beginTime).toBeUndefined()
    })
  })

  describe('selectDictLabel', () => {
    const datas = [
      { value: '0', label: '正常' },
      { value: '1', label: '停用' },
    ]

    it('匹配字典值', () => {
      expect(selectDictLabel(datas, '0')).toBe('正常')
      expect(selectDictLabel(datas, '1')).toBe('停用')
    })

    it('未匹配返回原值', () => {
      expect(selectDictLabel(datas, '999')).toBe('999')
    })

    it('undefined 返回空字符串', () => {
      expect(selectDictLabel(datas, undefined)).toBe('')
    })
  })

  describe('selectDictLabels', () => {
    const datas = [
      { value: '0', label: '正常' },
      { value: '1', label: '停用' },
    ]

    it('字符串多选', () => {
      expect(selectDictLabels(datas, '0,1')).toBe('正常,停用')
    })

    it('数组多选', () => {
      expect(selectDictLabels(datas, ['0', '1'])).toBe('正常,停用')
    })

    it('自定义分隔符', () => {
      expect(selectDictLabels(datas, '0;1', ';')).toBe('正常;停用')
    })
  })

  describe('parseStrEmpty', () => {
    it('null 和 undefined 转空字符串', () => {
      expect(parseStrEmpty(null)).toBe('')
      expect(parseStrEmpty(undefined)).toBe('')
    })

    it('特殊字符串转空', () => {
      expect(parseStrEmpty('null')).toBe('')
      expect(parseStrEmpty('undefined')).toBe('')
    })

    it('正常字符串不变', () => {
      expect(parseStrEmpty('hello')).toBe('hello')
    })
  })

  describe('mergeRecursive', () => {
    it('合并对象', () => {
      const source = { a: 1, b: { c: 2 } }
      const target = { b: { c: 3, d: 4 }, e: 5 }
      const result = mergeRecursive(source, target)
      expect(result.b.c).toBe(3)
      expect(result.b.d).toBe(4)
      expect(result.e).toBe(5)
    })
  })

  describe('handleTree', () => {
    it('构建树形结构', () => {
      interface TestTreeNode { id: number; parentId: number; name?: string; children?: TestTreeNode[] }
      const data: TestTreeNode[] = [
        { id: 1, parentId: 0, name: '根节点' },
        { id: 2, parentId: 1, name: '子节点' },
        { id: 3, parentId: 2, name: '孙节点' },
      ]
      const tree = handleTree(data)
      expect(tree.length).toBe(1)
      expect(tree[0].children!.length).toBe(1)
      expect(tree[0].children![0].children!.length).toBe(1)
    })

    it('根节点返回', () => {
      const data: Array<{ id: number; parentId: number }> = [{ id: 1, parentId: 0 }]
      const tree = handleTree(data)
      expect(tree.length).toBe(1)
    })
  })

  describe('tansParams', () => {
    it('序列化参数', () => {
      const params = { name: 'test', value: 123 }
      const result = tansParams(params)
      expect(result).toContain('name=test')
      expect(result).toContain('value=123')
    })

    it('忽略空值', () => {
      const params = { name: 'test', value: null }
      const result = tansParams(params)
      expect(result).toContain('name=test')
      expect(result).not.toContain('value=')
    })
  })

  describe('getNormalPath', () => {
    it('路径规范化', () => {
      // 只替换一次相邻斜杠，不处理尾部斜杠
      expect(getNormalPath('//user/profile')).toBe('/user/profile')
      expect(getNormalPath('/user//profile')).toBe('/user/profile')
      expect(getNormalPath('')).toBe('')
    })
  })

  describe('blobValidate', () => {
    it('判断是否为 blob 格式', () => {
      const blob = new Blob([''], { type: 'application/octet-stream' })
      expect(blobValidate(blob)).toBe(true)
      const jsonBlob = new Blob([''], { type: 'application/json' })
      expect(blobValidate(jsonBlob)).toBe(false)
    })
  })
})
