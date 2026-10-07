import { describe, it, expect } from 'vitest'
import {
  isPathMatch,
  isEmpty,
  isHttp,
  isExternal,
  validUsername,
  validURL,
  validLowerCase,
  validUpperCase,
  validAlphabets,
  validEmail,
  isString,
  isArray,
} from '../validate'

describe('validate.js', () => {
  describe('isPathMatch', () => {
    it('精确匹配', () => {
      expect(isPathMatch('/user', '/user')).toBe(true)
      expect(isPathMatch('/user', '/admin')).toBe(false)
    })

    it('单通配符匹配', () => {
      expect(isPathMatch('/user/*', '/user/123')).toBe(true)
      expect(isPathMatch('/user/*', '/user/123/profile')).toBe(false)
    })

    it('双通配符匹配', () => {
      expect(isPathMatch('/user/**', '/user/123/profile')).toBe(true)
      expect(isPathMatch('/user/**', '/user/123')).toBe(true)
    })

    it('问号匹配', () => {
      expect(isPathMatch('/user/?', '/user/1')).toBe(true)
      expect(isPathMatch('/user/?', '/user/12')).toBe(false)
    })
  })

  describe('isEmpty', () => {
    it('空值判断', () => {
      expect(isEmpty(null)).toBe(true)
      expect(isEmpty(undefined)).toBe(true)
      expect(isEmpty('')).toBe(true)
      expect(isEmpty('undefined')).toBe(true)
    })

    it('非空值判断', () => {
      expect(isEmpty('hello')).toBe(false)
      // 注意：false == "" 为 true，因此 isEmpty(false) 返回 true
    })
  })

  describe('isHttp', () => {
    it('HTTP URL 判断', () => {
      expect(isHttp('http://example.com')).toBe(true)
      expect(isHttp('https://example.com')).toBe(true)
      expect(isHttp('/api/user')).toBe(false)
    })
  })

  describe('isExternal', () => {
    it('外链判断', () => {
      expect(isExternal('https://example.com')).toBe(true)
      expect(isExternal('http://example.com')).toBe(true)
      expect(isExternal('mailto:test@test.com')).toBe(true)
      expect(isExternal('tel:123456')).toBe(true)
      expect(isExternal('/api/user')).toBe(false)
    })
  })

  describe('validUsername', () => {
    it('用户名验证', () => {
      expect(validUsername('admin')).toBe(true)
      expect(validUsername('editor')).toBe(true)
      expect(validUsername('user')).toBe(false)
    })
  })

  describe('validURL', () => {
    it('URL 格式验证', () => {
      expect(validURL('http://example.com')).toBe(true)
      expect(validURL('https://example.com')).toBe(true)
      expect(validURL('not-a-url')).toBe(false)
    })
  })

  describe('validLowerCase', () => {
    it('小写字母验证', () => {
      expect(validLowerCase('hello')).toBe(true)
      expect(validLowerCase('Hello')).toBe(false)
    })
  })

  describe('validUpperCase', () => {
    it('大写字母验证', () => {
      expect(validUpperCase('HELLO')).toBe(true)
      expect(validUpperCase('Hello')).toBe(false)
    })
  })

  describe('validAlphabets', () => {
    it('纯字母验证', () => {
      expect(validAlphabets('Hello')).toBe(true)
      expect(validAlphabets('Hello123')).toBe(false)
    })
  })

  describe('validEmail', () => {
    it('邮箱格式验证', () => {
      expect(validEmail('test@example.com')).toBe(true)
      expect(validEmail('invalid-email')).toBe(false)
    })
  })

  describe('isString', () => {
    it('字符串类型判断', () => {
      expect(isString('hello')).toBe(true)
      expect(isString(new String('hello'))).toBe(true)
      expect(isString(123)).toBe(false)
    })
  })

  describe('isArray', () => {
    it('数组类型判断', () => {
      expect(isArray([])).toBe(true)
      expect(isArray([1, 2, 3])).toBe(true)
      expect(isArray({})).toBe(false)
    })
  })
})
