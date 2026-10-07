import type { DictDataOption } from '@/types/api'

/** 字典缓存项 */
interface DictItem {
  /** 字典类型名 */
  key: string
  /** 字典数据列表 */
  value: DictDataOption[]
}

const useDictStore = defineStore(
  'dict',
  {
    state: (): { dict: DictItem[] } => ({
      dict: []
    }),
    actions: {
      // 获取字典
      getDict(_key: string | null): DictDataOption[] | null | undefined {
        if (_key == null && _key == "") {
          return null
        }
        try {
          for (let i = 0; i < this.dict.length; i++) {
            if (this.dict[i].key == _key) {
              return this.dict[i].value
            }
          }
        } catch (e) {
          return null
        }
      },
      // 设置字典
      setDict(_key: string | null, value: DictDataOption[]) {
        if (_key !== null && _key !== "") {
          this.dict.push({
            key: _key,
            value: value
          })
        }
      },
      // 删除字典
      removeDict(_key: string | null): boolean {
        let bln = false
        try {
          for (let i = 0; i < this.dict.length; i++) {
            if (this.dict[i].key == _key) {
              this.dict.splice(i, 1)
              return true
            }
          }
        } catch (e) {
          bln = false
        }
        return bln
      },
      // 清空字典
      cleanDict() {
        this.dict = []
      },
      // 初始字典
      initDict() {
      }
    }
  })

export default useDictStore
