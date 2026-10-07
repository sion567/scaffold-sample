package com.scaffold.common.core.web.page;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.domain.Page;

/**
 * 表格分页数据对象
 *
 * @author ct
 */
public class TableDataInfo implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private long total;

    /** 列表数据 */
    private List<?> rows;

    /** 消息状态码 */
    private int code;

    /** 消息内容 */
    private String msg;

    /**
     * 表格数据对象
     */
    public TableDataInfo()
    {
    }

    /**
     * 分页
     *
     * @param list 列表数据
     * @param total 总记录数
     */
    public TableDataInfo(List<?> list, long total)
    {
        this.rows = list;
        this.total = total;
    }

    /**
     * Spring Data 分页结果 → 对外契约（total/rows 语义与 PageHelper 形态一致）
     */
    public static TableDataInfo from(Page<?> page)
    {
        TableDataInfo info = new TableDataInfo(page.getContent(), page.getTotalElements());
        info.setCode(com.scaffold.common.core.constant.HttpStatus.SUCCESS);
        info.setMsg("查询成功");
        return info;
    }

    public long getTotal()
    {
        return total;
    }

    public void setTotal(long total)
    {
        this.total = total;
    }

    public List<?> getRows()
    {
        return rows;
    }

    public void setRows(List<?> rows)
    {
        this.rows = rows;
    }

    public int getCode()
    {
        return code;
    }

    public void setCode(int code)
    {
        this.code = code;
    }

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
    }
}