package com.scaffold.common.core.utils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.scaffold.common.core.utils.sql.SqlUtil;
import com.scaffold.common.core.web.page.PageDomain;
import com.scaffold.common.core.web.page.TableSupport;

/**
 * 分页工具类
 *
 * <p>JPA 形态：{@link #toPageRequest(PageDomain)} 构建 PageRequest 传给
 * repository/基类 service（全工程 JPA 化后仅保留此形态）。</p>
 *
 * @author ct
 */
public class PageUtils
{
    /** 未传分页参数时的默认页大小（与 TableSupport 默认一致） */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * PageDomain → Spring Data PageRequest（JPA 分页标准入口）。
     * 排序属性为实体属性名（camelCase），Spring Data 属性解析对未知属性快速失败，
     * 排序注入面由属性解析兜底（不再需要 SQL 转义）。
     */
    public static PageRequest toPageRequest(PageDomain pageDomain)
    {
        int pageNum = pageDomain == null || pageDomain.getPageNum() == null ? 1 : pageDomain.getPageNum();
        int pageSize = pageDomain == null || pageDomain.getPageSize() == null ? DEFAULT_PAGE_SIZE : pageDomain.getPageSize();
        return PageRequest.of(Math.max(pageNum - 1, 0), Math.max(pageSize, 1), buildSort(pageDomain));
    }

    private static Sort buildSort(PageDomain pageDomain)
    {
        if (pageDomain == null || pageDomain.getOrderByColumn() == null || pageDomain.getOrderByColumn().isEmpty())
        {
            return Sort.unsorted();
        }
        Sort.Direction direction = "desc".equalsIgnoreCase(pageDomain.getIsAsc()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, pageDomain.getOrderByColumn());
    }

}
