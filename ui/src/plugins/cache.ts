/** 单个存储介质（sessionStorage / localStorage）的统一缓存封装 */
interface CacheLike {
  /** 写入字符串缓存 */
  set(key: string, value: string): void
  /** 读取字符串缓存 */
  get(key: string): string | null
  /** 写入 JSON 缓存（自动序列化） */
  setJSON<T>(key: string, jsonValue: T): void
  /** 读取 JSON 缓存（自动反序列化） */
  getJSON<T = unknown>(key: string): T | null
  /** 移除缓存 */
  remove(key: string): void
}

const sessionCache: CacheLike = {
  set (key: string, value: string) {
    if (!sessionStorage) {
      return
    }
    if (key != null && value != null) {
      sessionStorage.setItem(key, value)
    }
  },
  get (key: string) {
    if (!sessionStorage) {
      return null
    }
    if (key == null) {
      return null
    }
    return sessionStorage.getItem(key)
  },
  setJSON<T>(key: string, jsonValue: T) {
    if (jsonValue != null) {
      this.set(key, JSON.stringify(jsonValue))
    }
  },
  getJSON<T = unknown>(key: string) {
    const value = this.get(key)
    if (value != null) {
      return JSON.parse(value) as T
    }
    return null
  },
  remove (key: string) {
    sessionStorage.removeItem(key)
  }
}
const localCache: CacheLike = {
  set (key: string, value: string) {
    if (!localStorage) {
      return
    }
    if (key != null && value != null) {
      localStorage.setItem(key, value)
    }
  },
  get (key: string) {
    if (!localStorage) {
      return null
    }
    if (key == null) {
      return null
    }
    return localStorage.getItem(key)
  },
  setJSON<T>(key: string, jsonValue: T) {
    if (jsonValue != null) {
      this.set(key, JSON.stringify(jsonValue))
    }
  },
  getJSON<T = unknown>(key: string) {
    const value = this.get(key)
    if (value != null) {
      return JSON.parse(value) as T
    }
    return null
  },
  remove (key: string) {
    localStorage.removeItem(key)
  }
}

export default {
  /**
   * 会话级缓存
   */
  session: sessionCache,
  /**
   * 本地缓存
   */
  local: localCache
}
