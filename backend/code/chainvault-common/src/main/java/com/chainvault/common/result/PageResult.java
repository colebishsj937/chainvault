package com.chainvault.common.result;

import lombok.Data;

import java.util.List;

/**
 * 分页查询结果
 *
 * @param <T> 列表元素类型
 * @author chainvault
 * @date 2026-06-05
 */
@Data
public class PageResult<T> {

    /** 当前页码，从 1 开始 */
    private long page;

    /** 每页条数 */
    private long size;

    /** 总记录数 */
    private long total;

    /** 当前页数据 */
    private List<T> records;

    /**
     * 构建分页结果
     *
     * @param page    页码
     * @param size    每页条数
     * @param total   总记录数
     * @param records 数据列表
     * @param <T>     元素类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(long page, long size, long total, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setPage(page);
        result.setSize(size);
        result.setTotal(total);
        result.setRecords(records);
        return result;
    }
}
