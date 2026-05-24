package com.albertsfarm.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页响应
 */
@Data
public class PageResult<T> {
    private List<T> list;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(IPage<T> p) {
        PageResult<T> r = new PageResult<>();
        r.setList(p.getRecords());
        r.setTotal(p.getTotal());
        r.setPage(p.getCurrent());
        r.setSize(p.getSize());
        return r;
    }

    /** 实体 → VO 转换 */
    public static <E, V> PageResult<V> of(IPage<E> p, Function<E, V> mapper) {
        PageResult<V> r = new PageResult<>();
        List<V> list = p.getRecords() == null ? Collections.emptyList()
                : p.getRecords().stream().map(mapper).collect(Collectors.toList());
        r.setList(list);
        r.setTotal(p.getTotal());
        r.setPage(p.getCurrent());
        r.setSize(p.getSize());
        return r;
    }
}
