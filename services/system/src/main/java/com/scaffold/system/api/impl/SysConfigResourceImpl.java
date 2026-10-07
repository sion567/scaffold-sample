package com.scaffold.system.api.impl;

import java.util.List;
import com.scaffold.common.core.utils.PageUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.apache.dubbo.config.annotation.DubboService;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.core.utils.poi.ExcelUtil;
import com.scaffold.common.core.web.controller.BaseController;
import com.scaffold.common.core.web.domain.AjaxResult;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableDataInfo;
import com.scaffold.common.log.annotation.Log;
import com.scaffold.common.log.enums.BusinessType;
import com.scaffold.common.security.annotation.RequiresPermissions;
import com.scaffold.common.security.utils.SecurityUtils;
import com.scaffold.system.api.SysConfigResource;
import com.scaffold.system.domain.SysConfig;
import com.scaffold.system.service.ISysConfigService;

/**
 * 参数配置信息服务实现（Triple REST）
 *
 * @author ct
 */
@DubboService(protocol = "tri")
public class SysConfigResourceImpl extends BaseController implements SysConfigResource
{
    private final ISysConfigService configService;

    public SysConfigResourceImpl(ISysConfigService configService) {
        this.configService = configService;
    }

    @RequiresPermissions("system:config:list")
    @Override
    public TableDataInfo list(Integer pageNum, Integer pageSize, String orderByColumn, String isAsc,
                              String configName, String configKey, String configType)
    {
        PageDomain page = new PageDomain();
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setOrderByColumn(orderByColumn);
        page.setIsAsc(isAsc);
        SysConfig config = new SysConfig();
        config.setConfigName(configName);
        config.setConfigKey(configKey);
        config.setConfigType(configType);
        return configService.selectConfigPage(config, page);
    }

    @Log(title = "参数管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("system:config:export")
    @Override
    public byte[] export(String configName, String configKey, String configType)
    {
        SysConfig config = new SysConfig();
        config.setConfigName(configName);
        config.setConfigKey(configKey);
        config.setConfigType(configType);
        List<SysConfig> list = configService.selectConfigList(config);
        ExcelUtil<SysConfig> util = new ExcelUtil<SysConfig>(SysConfig.class);
        return util.exportExcel(list, "参数数据");
    }

    @Override
    public AjaxResult getInfo(Long configId)
    {
        return success(configService.selectConfigById(configId));
    }

    @Override
    public AjaxResult getConfigKey(@PathVariable String configKey)
    {
        return success(configService.selectConfigByKey(configKey));
    }

    @RequiresPermissions("system:config:add")
    @Log(title = "参数管理", businessType = BusinessType.INSERT)
    @Override
    public AjaxResult add(SysConfig config)
    {
        if (!configService.checkConfigKeyUnique(config))
        {
            return error("新增参数'" + config.getConfigName() + "'失败，参数键名已存在");
        }
        config.setCreateBy(SecurityUtils.getUsername());
        return toAjax(configService.insertConfig(config));
    }

    @RequiresPermissions("system:config:edit")
    @Log(title = "参数管理", businessType = BusinessType.UPDATE)
    @Override
    public AjaxResult edit(SysConfig config)
    {
        if (!configService.checkConfigKeyUnique(config))
        {
            return error("修改参数'" + config.getConfigName() + "'失败，参数键名已存在");
        }
        config.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(configService.updateConfig(config));
    }

    @RequiresPermissions("system:config:remove")
    @Log(title = "参数管理", businessType = BusinessType.DELETE)
    @Override
    public AjaxResult remove(String configIds)
    {
        configService.deleteConfigByIds(Convert.toLongArray(configIds));
        return success();
    }

    @RequiresPermissions("system:config:remove")
    @Log(title = "参数管理", businessType = BusinessType.CLEAN)
    @Override
    public AjaxResult refreshCache()
    {
        configService.resetConfigCache();
        return success();
    }
}