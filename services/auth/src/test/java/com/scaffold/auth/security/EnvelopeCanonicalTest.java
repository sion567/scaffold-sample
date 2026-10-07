package com.scaffold.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * EnvelopeCanonical 签名串拼装工具测试。
 *
 * <p>覆盖维度：
 * <ul>
 *   <li>ASCII 升序排列</li>
 *   <li>signature/null/空值 自动剔除</li>
 *   <li>边界：空 map、单个参数</li>
 * </ul>
 */
class EnvelopeCanonicalTest
{
    @Test
    @DisplayName("ASCII 升序排列：b < n < t")
    void build_asciiOrder()
    {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("timestamp", "1700000000000");
        params.put("nonce", "abc123");
        params.put("bizType", "ship");

        String result = EnvelopeCanonical.build(params);

        // b < n < t（ASCII）
        assertEquals("bizType=ship&nonce=abc123&timestamp=1700000000000", result);
    }

    @Test
    @DisplayName("signature 字段自动剔除，不参与签名")
    void build_excludesSignature()
    {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("signature", "SIG_DATA");
        params.put("nonce", "n1");
        params.put("timestamp", "1700000000000");

        String result = EnvelopeCanonical.build(params);

        assertEquals("nonce=n1&timestamp=1700000000000", result);
    }

    @Test
    @DisplayName("null 值跳过不参与签名")
    void build_skipsNull()
    {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("a", "1");
        params.put("b", null);
        params.put("c", "3");

        String result = EnvelopeCanonical.build(params);

        assertEquals("a=1&c=3", result);
    }

    @Test
    @DisplayName("空字符串跳过不参与签名")
    void build_skipsEmptyString()
    {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("x", "100");
        params.put("y", "");

        String result = EnvelopeCanonical.build(params);

        assertEquals("x=100", result);
    }

    @Test
    @DisplayName("空 map 返回空字符串")
    void build_emptyMap()
    {
        String result = EnvelopeCanonical.build(new HashMap<>());
        assertEquals("", result);
    }

    @Test
    @DisplayName("单个参数正常拼接")
    void build_singleParam()
    {
        Map<String, String> params = new HashMap<>();
        params.put("timestamp", "1700000000000");

        String result = EnvelopeCanonical.build(params);

        assertEquals("timestamp=1700000000000", result);
    }
}
