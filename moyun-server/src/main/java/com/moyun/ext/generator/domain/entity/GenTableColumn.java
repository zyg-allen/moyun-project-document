package com.moyun.ext.generator.domain.entity;

import com.moyun.common.annotation.Excel;
import com.moyun.core.base.BaseEntity;
import com.moyun.util.string.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serial;
import java.io.Serializable;

/**
 * 代码生成业务字段表 gen_table_column
 *
 * @author ruoyi
 */
public class GenTableColumn extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long columnId;

    private Long tableId;

    private String columnName;

    private String columnComment;

    private String columnType;

    private String javaType;

    private String javaField;

    private String isPk;

    private String isIncrement;

    private String isRequired;

    private String isInsert;

    private String isEdit;

    private String isList;

    private String isQuery;

    private String queryType;

    private String htmlType;

    private String dictType;

    private String sort;

    private String createOperation;

    private String updateOperation;

    private String listOperationResult;

    private String operationResult;

    public Long getColumnId() {
        return columnId;
    }

    public void setColumnId(Long columnId) {
        this.columnId = columnId;
    }

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnComment() {
        return columnComment;
    }

    public void setColumnComment(String columnComment) {
        this.columnComment = columnComment;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public String getJavaType() {
        return javaType;
    }

    public void setJavaType(String javaType) {
        this.javaType = javaType;
    }

    public String getJavaField() {
        return javaField;
    }

    public void setJavaField(String javaField) {
        this.javaField = javaField;
    }

    public String getIsPk() {
        return isPk;
    }

    public void setIsPk(String isPk) {
        this.isPk = isPk;
    }

    public String getIsIncrement() {
        return isIncrement;
    }

    public void setIsIncrement(String isIncrement) {
        this.isIncrement = isIncrement;
    }

    public String getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(String isRequired) {
        this.isRequired = isRequired;
    }

    public String getIsInsert() {
        return isInsert;
    }

    public void setIsInsert(String isInsert) {
        this.isInsert = isInsert;
    }

    public String getIsEdit() {
        return isEdit;
    }

    public void setIsEdit(String isEdit) {
        this.isEdit = isEdit;
    }

    public String getIsList() {
        return isList;
    }

    public void setIsList(String isList) {
        this.isList = isList;
    }

    public String getIsQuery() {
        return isQuery;
    }

    public void setIsQuery(String isQuery) {
        this.isQuery = isQuery;
    }

    public String getQueryType() {
        return queryType;
    }

    public void setQueryType(String queryType) {
        this.queryType = queryType;
    }

    public String getHtmlType() {
        return htmlType;
    }

    public void setHtmlType(String htmlType) {
        this.htmlType = htmlType;
    }

    public String getDictType() {
        return dictType;
    }

    public void setDictType(String dictType) {
        this.dictType = dictType;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public boolean isPk() {
        return isPk(this);
    }

    public boolean isIncrement() {
        return isIncrement(this);
    }

    public boolean isRequired() {
        return isRequired(this);
    }

    public boolean isInsert() {
        return isInsert(this);
    }

    public boolean isEdit() {
        return isEdit(this);
    }

    public boolean isList() {
        return isList(this);
    }

    public boolean isQuery() {
        return isQuery(this);
    }

    public String readOperation() {
        return isList(this) ? operationResult : null;
    }

    public String addOperation() {
        return isInsert(this) || isEdit(this) ? operationResult : null;
    }

    public String editOperation() {
        return isEdit(this) ? operationResult : null;
    }

    public String delOperation() {
        return isPk(this) ? operationResult : null;
    }

    public static boolean isPk(GenTableColumn column) {
        return StringUtils.equals("1", column.getIsPk());
    }

    public static boolean isIncrement(GenTableColumn column) {
        return StringUtils.equals("1", column.getIsIncrement());
    }

    public static boolean isRequired(GenTableColumn column) {
        return StringUtils.equals("1", column.getIsRequired());
    }

    public static boolean isInsert(GenTableColumn column) {
        return StringUtils.equals("1", column.getIsInsert());
    }

    public static boolean isEdit(GenTableColumn column) {
        return StringUtils.equals("1", column.getIsEdit());
    }

    public static boolean isList(GenTableColumn column) {
        return StringUtils.equals("1", column.getIsList());
    }

    public static boolean isQuery(GenTableColumn column) {
        return StringUtils.equals("1", column.getIsQuery());
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("columnId", getColumnId())
                .append("tableId", getTableId())
                .append("columnName", getColumnName())
                .append("columnComment", getColumnComment())
                .append("columnType", getColumnType())
                .append("javaType", getJavaType())
                .append("javaField", getJavaField())
                .append("isPk", getIsPk())
                .append("isIncrement", getIsIncrement())
                .append("isRequired", getIsRequired())
                .append("isInsert", getIsInsert())
                .append("isEdit", getIsEdit())
                .append("isList", getIsList())
                .append("isQuery", getIsQuery())
                .append("queryType", getQueryType())
                .append("htmlType", getHtmlType())
                .append("dictType", getDictType())
                .append("sort", getSort())
                .toString();
    }
}
