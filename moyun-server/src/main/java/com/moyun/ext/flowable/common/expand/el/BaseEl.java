package com.moyun.ext.flowable.common.expand.el;

import org.springframework.stereotype.Component;

/**
 * 基础EL表达式
 *
 * @author Tony
 * @date 2021-04-03
 */
@Component("baseEl")
public class BaseEl {

    /**
     * 判断是否为空
     */
    public boolean isEmpty(Object obj) {
        return obj == null || obj.toString().isEmpty();
    }

    /**
     * 判断是否不为空
     */
    public boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    /**
     * 判断是否相等
     */
    public boolean equals(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) {
            return true;
        }
        if (obj1 == null || obj2 == null) {
            return false;
        }
        return obj1.equals(obj2);
    }

    /**
     * 判断是否不相等
     */
    public boolean notEquals(Object obj1, Object obj2) {
        return !equals(obj1, obj2);
    }

    /**
     * 判断是否包含
     */
    public boolean contains(String str, String searchStr) {
        if (str == null || searchStr == null) {
            return false;
        }
        return str.contains(searchStr);
    }
}