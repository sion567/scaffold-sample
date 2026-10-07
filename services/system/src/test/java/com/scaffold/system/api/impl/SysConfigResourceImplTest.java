package com.scaffold.system.api.impl;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.scaffold.common.core.constant.HttpStatus;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.domain.SysConfig;
import com.scaffold.system.service.ISysConfigService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 例子 A：Triple REST 前端接口单元测试（SysConfigResourceImpl，@DubboService(protocol="tri")）。
 *
 * <p>Triple 接口的实现本质是普通 Spring Bean：HTTP 映射（@GetMapping 等）由 Dubbo Triple 在
 * 框架层消费，业务逻辑用 Mockito 打桩 service 即可覆盖，无需启动 Dubbo/Spring 容器。
 * HTTP 路径与参数绑定的契约变化，靠编译期对 SysConfigResource 接口的实现来保证。</p>
 *
 * @author ct
 */
@ExtendWith(MockitoExtension.class)
class SysConfigResourceImplTest
{
    @Mock
    private ISysConfigService configService;

    @InjectMocks
    private SysConfigResourceImpl resource;

    private SysConfig config(Long id, String name, String key)
    {
        SysConfig c = new SysConfig();
        c.setConfigId(id);
        c.setConfigName(name);
        c.setConfigKey(key);
        c.setConfigValue("value-of-" + key);
        c.setConfigType("Y");
        return c;
    }

    @Test
    @DisplayName("list：分页参数透传，返回 TableDataInfo")
    void list_returnsTableData()
    {
        SysConfig stored = config(1L, "主框架页-默认皮肤", "sys.index.skinName");
        TableDataInfo stub = new TableDataInfo(List.of(stored), 1);
        stub.setCode(com.scaffold.common.core.constant.HttpStatus.SUCCESS);
        when(configService.selectConfigPage(any(SysConfig.class), any())).thenReturn(stub);

        TableDataInfo table = resource.list(1, 10, null, null, "皮肤", null, null);

        assertEquals(HttpStatus.SUCCESS, table.getCode());
        assertEquals(1, table.getTotal());
        assertEquals(1, table.getRows().size());

        // 验证查询条件被正确装进 SysConfig
        verify(configService).selectConfigPage(any(SysConfig.class), any());
    }

    @Test
    @DisplayName("getInfo：按 configId 查询并包在 AjaxResult.data 里")
    void getInfo_wrapsData()
    {
        SysConfig stored = config(1L, "主题", "sys.index.skinName");
        when(configService.selectConfigById(1L)).thenReturn(stored);

        AjaxResult result = resource.getInfo(1L);

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        assertEquals(stored, result.get(AjaxResult.DATA_TAG));
    }

    @Test
    @DisplayName("getConfigKey：按 key 查询返回字符串值")
    void getConfigKey_returnsValue()
    {
        when(configService.selectConfigByKey("sys.index.skinName")).thenReturn("skin-default");

        AjaxResult result = resource.getConfigKey("sys.index.skinName");

        // 注意：BaseController.success(String) 命中的是 msg 重载，值落在 msg 而非 data（历史行为，前端按 msg 取值）
        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        assertEquals("skin-default", result.get(AjaxResult.MSG_TAG));
    }

    @Test
    @DisplayName("add：configKey 重复时返回错误且不落库")
    void add_duplicateKey_rejected()
    {
        when(configService.checkConfigKeyUnique(any(SysConfig.class))).thenReturn(false);

        AjaxResult result = resource.add(config(null, "新参数", "sys.index.skinName"));

        assertEquals(HttpStatus.ERROR, result.get(AjaxResult.CODE_TAG));
        assertTrue(((String) result.get(AjaxResult.MSG_TAG)).contains("参数键名已存在"));
        verify(configService, never()).insertConfig(any(SysConfig.class));
    }

    @Test
    @DisplayName("add：校验通过后填充创建人并落库")
    void add_uniqueKey_savedWithCreator()
    {
        try (MockedStatic<SecurityUtils> security = org.mockito.Mockito.mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::getUsername).thenReturn("admin");
            when(configService.checkConfigKeyUnique(any(SysConfig.class))).thenReturn(true);
            when(configService.insertConfig(any(SysConfig.class))).thenReturn(1);

            SysConfig input = config(null, "新参数", "sys.demo.key");
            AjaxResult result = resource.add(input);

            assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
            assertEquals("admin", input.getCreateBy());
            verify(configService).insertConfig(input);
        }
    }

    @Test
    @DisplayName("remove：批量删除后返回成功")
    void remove_deletesAndReturnsSuccess()
    {
        AjaxResult result = resource.remove("1,2,3");

        assertEquals(HttpStatus.SUCCESS, result.get(AjaxResult.CODE_TAG));
        verify(configService).deleteConfigByIds(new Long[] { 1L, 2L, 3L });
    }
}
