package com.scaffold.system.service.impl;

import java.util.Collection;
import java.util.List;
import jakarta.annotation.PostConstruct;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import com.scaffold.common.core.constant.CacheConstants;
import com.scaffold.common.core.constant.UserConstants;
import com.scaffold.common.core.exception.ServiceException;
import com.scaffold.common.core.jpa.JpaSpecs;
import com.scaffold.common.core.text.Convert;
import com.scaffold.common.core.utils.StringUtils;
import com.scaffold.common.redis.service.RedisService;
import com.scaffold.system.domain.SysConfig;
import com.scaffold.system.repository.SysConfigRepository;
import com.scaffold.system.service.ISysConfigService;

/**
 * 参数配置 服务层实现（JPA：SysConfigRepository + Specification 动态条件）。
 *
 * @author ct
 */
@Service
public class SysConfigServiceImpl implements ISysConfigService
{
    private final SysConfigRepository configRepository;
    private final RedisService redisService;

    public SysConfigServiceImpl(SysConfigRepository configRepository, RedisService redisService)
    {
        this.configRepository = configRepository;
        this.redisService = redisService;
    }

    /**
     * 项目启动时，初始化参数到缓存
     */
    @PostConstruct
    public void init()
    {
        loadingConfigCache();
    }

    /**
     * 查询参数配置信息
     *
     * @param configId 参数配置ID
     * @return 参数配置信息
     */
    @Override
    public SysConfig selectConfigById(Long configId)
    {
        return configRepository.findById(configId).orElse(null);
    }

    /**
     * 根据键名查询参数配置信息
     *
     * @param configKey 参数key
     * @return 参数键值
     */
    @Override
    public String selectConfigByKey(String configKey)
    {
        String configValue = Convert.toStr(redisService.getCacheObject(getCacheKey(configKey)));
        if (StringUtils.isNotEmpty(configValue))
        {
            return configValue;
        }
        SysConfig retConfig = configRepository.findByConfigKey(configKey).orElse(null);
        if (StringUtils.isNotNull(retConfig))
        {
            redisService.setCacheObject(getCacheKey(configKey), retConfig.getConfigValue());
            return retConfig.getConfigValue();
        }
        return StringUtils.EMPTY;
    }

    /**
     * 查询参数配置列表（configName/configKey 模糊、configType 等值、createTime 区间）
     *
     * @param config 参数配置信息
     * @return 参数配置集合
     */
    @Override
    public List<SysConfig> selectConfigList(SysConfig config)
    {
        return configRepository.list(toSpec(config));
    }

    /**
     * 动态条件（对齐原 SysConfigMapper.xml selectConfigList）
     */
    @Override
    public com.scaffold.common.core.web.page.TableDataInfo selectConfigPage(SysConfig config, com.scaffold.common.core.web.page.PageDomain page)
    {
        return com.scaffold.common.core.web.page.TableDataInfo.from(
                configRepository.page(toSpec(config), com.scaffold.common.core.utils.PageUtils.toPageRequest(page)));
    }

    private Specification<?> toSpec(SysConfig config)
    {
        if (config == null)
        {
            config = new SysConfig();
        }
        return JpaSpecs.likeIf("configName", config.getConfigName())
                .and(JpaSpecs.eqIfNotBlank("configType", config.getConfigType()))
                .and(JpaSpecs.likeIf("configKey", config.getConfigKey()))
                .and(JpaSpecs.dateRangeIf("createTime", config.getParams()));
    }

    /**
     * 新增参数配置
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int insertConfig(SysConfig config)
    {
        configRepository.save(config);
        redisService.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
        return 1;
    }

    /**
     * 修改参数配置
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public int updateConfig(SysConfig config)
    {
        SysConfig temp = configRepository.findById(config.getConfigId()).orElseThrow(
                () -> new ServiceException("参数配置不存在或已被删除"));
        if (!StringUtils.equals(temp.getConfigKey(), config.getConfigKey()))
        {
            redisService.deleteObject(getCacheKey(temp.getConfigKey()));
        }

        configRepository.save(config);
        redisService.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
        return 1;
    }

    /**
     * 批量删除参数信息
     *
     * @param configIds 需要删除的参数ID
     */
    @Override
    public void deleteConfigByIds(Long[] configIds)
    {
        for (Long configId : configIds)
        {
            SysConfig config = selectConfigById(configId);
            if (StringUtils.equals(UserConstants.YES, config.getConfigType()))
            {
                throw new ServiceException(String.format("内置参数【%1$s】不能删除 ", config.getConfigKey()));
            }
            configRepository.deleteById(configId);
            redisService.deleteObject(getCacheKey(config.getConfigKey()));
        }
    }

    /**
     * 加载参数缓存数据
     */
    @Override
    public void loadingConfigCache()
    {
        List<SysConfig> configsList = configRepository.findAll();
        for (SysConfig config : configsList)
        {
            redisService.setCacheObject(getCacheKey(config.getConfigKey()), config.getConfigValue());
        }
    }

    /**
     * 清空参数缓存数据
     */
    @Override
    public void clearConfigCache()
    {
        Collection<String> keys = redisService.keys(CacheConstants.SYS_CONFIG_KEY + "*");
        redisService.deleteObject(keys);
    }

    /**
     * 重置参数缓存数据
     */
    @Override
    public void resetConfigCache()
    {
        clearConfigCache();
        loadingConfigCache();
    }

    /**
     * 校验参数键名是否唯一
     *
     * @param config 参数配置信息
     * @return 结果
     */
    @Override
    public boolean checkConfigKeyUnique(SysConfig config)
    {
        Long configId = StringUtils.isNull(config.getConfigId()) ? -1L : config.getConfigId();
        SysConfig info = configRepository.findByConfigKey(config.getConfigKey()).orElse(null);
        if (StringUtils.isNotNull(info) && info.getConfigId().longValue() != configId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 设置cache key
     *
     * @param configKey 参数键
     * @return 缓存键key
     */
    private String getCacheKey(String configKey)
    {
        return CacheConstants.SYS_CONFIG_KEY + configKey;
    }
}
