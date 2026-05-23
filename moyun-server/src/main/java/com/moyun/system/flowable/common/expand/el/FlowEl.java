package com.moyun.system.flowable.common.expand.el;

import org.springframework.stereotype.Component;

/**
 * 流程EL表达式
 *
 * @author Tony
 * @date 2021-04-03
 */
@Component("flowEl")
public class FlowEl {

    /**
     * 判断是否为部门领导
     */
    public boolean isDeptLeader(String userId) {
        // 这里需要根据实际业务逻辑实现
        return false;
    }

    /**
     * 判断是否为上级领导
     */
    public boolean isSuperiorLeader(String userId) {
        // 这里需要根据实际业务逻辑实现
        return false;
    }

    /**
     * 获取部门领导
     */
    public String getDeptLeader(String deptId) {
        // 这里需要根据实际业务逻辑实现
        return null;
    }

    /**
     * 获取上级领导
     */
    public String getSuperiorLeader(String userId) {
        // 这里需要根据实际业务逻辑实现
        return null;
    }

    /**
     * 获取部门成员
     */
    public java.util.List<String> getDeptUsers(String deptId) {
        // 这里需要根据实际业务逻辑实现
        return new java.util.ArrayList<>();
    }
}