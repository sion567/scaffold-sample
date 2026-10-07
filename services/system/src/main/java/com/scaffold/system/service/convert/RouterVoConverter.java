package com.scaffold.system.service.convert;

import org.springframework.stereotype.Component;
import com.scaffold.system.domain.SysMenu;
import com.scaffold.system.domain.vo.MetaVo;
import com.scaffold.system.domain.vo.RouterVo;

/**
 * SysMenu → RouterVo / MetaVo 映射（手写实现；MapStruct 注解处理器未接入本仓库，勿用 org.mapstruct.Mapper）。
 *
 * <p>映射语义：path/component/query 同名拷贝；name/hidden/redirect/alwaysShow/meta/children
 * 不在此设置（由调用方按菜单类型装配）。</p>
 *
 * @author ct
 */
@Component
public class RouterVoConverter
{
    /** 菜单 → 路由（仅拷贝 path/component/query） */
    public RouterVo toRouterVo(SysMenu menu)
    {
        RouterVo router = new RouterVo();
        router.setPath(menu.getPath());
        router.setComponent(menu.getComponent());
        router.setQuery(menu.getQuery());
        return router;
    }

    /** 菜单 → 元信息（title=menuName，icon 同名；link 不设置） */
    public MetaVo toMetaVo(SysMenu menu)
    {
        MetaVo meta = new MetaVo();
        meta.setTitle(menu.getMenuName());
        meta.setIcon(menu.getIcon());
        return meta;
    }

    /** 菜单 → 元信息（外链形态：link 取菜单 path） */
    public MetaVo toMetaVoWithLink(SysMenu menu)
    {
        MetaVo meta = toMetaVo(menu);
        meta.setLink(menu.getPath());
        return meta;
    }
}
