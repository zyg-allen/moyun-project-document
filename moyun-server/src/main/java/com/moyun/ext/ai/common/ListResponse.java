package com.moyun.ext.ai.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 通用列表响应类
 *
 * <p>用于所有返回列表数据的接口，避免创建重复的ListResponse类</p>
 *
 * @param <T> 列表元素类型
 * @author laomao
 * @time 2025/11/23
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListResponse<T> {

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 总数量
     */
    private Integer total;

    /**
     * 构造方法 - 自动计算总数
     *
     * @param list 数据列表
     */
    public ListResponse(List<T> list) {
        this.list = list;
        this.total = list != null ? list.size() : 0;
    }
}
