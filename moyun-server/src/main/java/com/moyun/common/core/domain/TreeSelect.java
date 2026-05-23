package com.moyun.common.core.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.moyun.common.core.domain.entity.SysDept;
import com.moyun.common.core.domain.entity.SysMenu;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Treeselect树结构实体类
 *
 * @author ruoyi
 */
@Data
public class TreeSelect implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 节点ID */
    private Long id;

    /** 节点名称 */
    private String label;

    /** 子节点 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<TreeSelect> children;

    public TreeSelect() {
    }

    public TreeSelect(SysMenu menu) {
        this.id = menu.getMenuId();
        this.label = menu.getMenuName();
        this.children = menu.getChildren().stream().map(SysMenu.class::cast).map(TreeSelect::new).collect(Collectors.toList());
    }

    public TreeSelect(SysDept dept) {
        this.id = dept.getDeptId();
        this.label = dept.getDeptName();
        this.children = dept.getChildren().stream().map(SysDept.class::cast).map(TreeSelect::new).collect(Collectors.toList());
    }
}