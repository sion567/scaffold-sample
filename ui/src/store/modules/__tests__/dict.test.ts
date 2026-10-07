import { describe, it, expect, vi, beforeEach } from 'vitest'

type DictStoreDefinition = (typeof import('@/store/modules/dict'))['default']

describe('dict store', () => {
  let useDictStore: DictStoreDefinition

  beforeEach(async () => {
    vi.resetModules()
    const { createPinia, setActivePinia } = await import('pinia')
    const pinia = createPinia()
    setActivePinia(pinia)
    const module = await import('@/store/modules/dict')
    useDictStore = module.default
  })

  describe('state', () => {
    it('初始化为空数组', () => {
      const store = useDictStore()
      expect(store.dict).toEqual([])
    })
  })

  describe('getDict', () => {
    it('获取不存在的字典返回 undefined', () => {
      const store = useDictStore()
      // 实际返回 undefined，不是 null
      expect(store.getDict('non_exist')).toBeUndefined()
    })

    it('获取已存在的字典返回值', () => {
      const store = useDictStore()
      const testData = [{ label: '男', value: '0' }, { label: '女', value: '1' }]
      store.setDict('gender', testData)

      expect(store.getDict('gender')).toEqual(testData)
    })

    it('null 或空字符串返回 undefined', () => {
      const store = useDictStore()
      expect(store.getDict(null)).toBeUndefined()
      expect(store.getDict('')).toBeUndefined()
    })
  })

  describe('setDict', () => {
    it('设置字典', () => {
      const store = useDictStore()
      const testData = [{ label: '启用', value: '0' }, { label: '停用', value: '1' }]
      store.setDict('status', testData)

      expect(store.dict).toHaveLength(1)
      expect(store.dict[0].key).toBe('status')
    })

    it('null 或空字符串不设置', () => {
      const store = useDictStore()
      store.setDict(null, [])
      store.setDict('', [])

      expect(store.dict).toHaveLength(0)
    })

    it('可设置多个字典', () => {
      const store = useDictStore()
      store.setDict('gender', [{ label: '男', value: '0' }])
      store.setDict('status', [{ label: '启用', value: '0' }])

      expect(store.dict).toHaveLength(2)
    })
  })

  describe('removeDict', () => {
    it('删除存在的字典返回 true', () => {
      const store = useDictStore()
      store.setDict('gender', [{ label: '男', value: '0' }])
      expect(store.removeDict('gender')).toBe(true)
      expect(store.dict).toHaveLength(0)
    })

    it('删除不存在的字典返回 false', () => {
      const store = useDictStore()
      expect(store.removeDict('non_exist')).toBe(false)
    })
  })

  describe('cleanDict', () => {
    it('清空所有字典', () => {
      const store = useDictStore()
      store.setDict('gender', [{ label: '男', value: '0' }])
      store.setDict('status', [{ label: '启用', value: '0' }])

      store.cleanDict()

      expect(store.dict).toEqual([])
    })
  })
})
