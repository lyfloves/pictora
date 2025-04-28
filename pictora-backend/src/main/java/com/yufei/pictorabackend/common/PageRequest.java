package com.yufei.pictorabackend.common;

import lombok.Data;

/**
 * 分页请求参数类
 */
@Data
public class PageRequest {

    /**
     * 当前页
     */
    private int current = 1;

    /**
     * 每页记录数
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = "ascend";
}
