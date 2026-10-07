import type { ToRefs } from 'vue'
import useDictStore from '@/store/modules/dict.ts'
import { getDicts } from '@/api/system/dict/data.ts'
import type { DictDataOption } from '@/types/api'

/** 字典接口返回项中本函数用到的字段（后端 SysDictData 投影） */
interface DictRespItem {
  dictLabel: string
  dictValue: string
  listClass: string
  cssClass?: string
}

/**
 * 获取字典数据
 */
export function useDict(...args: string[]): ToRefs<Record<string, DictDataOption[]>> {
  const res = ref<Record<string, DictDataOption[]>>({})
  return (() => {
    args.forEach((dictType) => {
      res.value[dictType] = []
      const dicts = useDictStore().getDict(dictType)
      if (dicts) {
        res.value[dictType] = dicts
      } else {
        getDicts(dictType).then((resp: unknown) => {
          const data = (resp as { data: DictRespItem[] }).data
          res.value[dictType] = data.map(p => ({ label: p.dictLabel, value: p.dictValue, elTagType: p.listClass as DictDataOption['elTagType'], elTagClass: p.cssClass }))
          useDictStore().setDict(dictType, res.value[dictType])
        })
      }
    })
    return toRefs(res.value)
  })()
}
