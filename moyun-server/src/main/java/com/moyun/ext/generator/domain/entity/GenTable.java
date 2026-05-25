package com.moyun.ext.generator.domain.entity;

import com.moyun.common.annotation.Excel;
import com.moyun.core.base.BaseEntity;
import com.moyun.util.string.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 业务表 gen_table
 *
 * @author ruoyi
 */
public class GenTable extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long tableId;

    @Excel(name = "表名称")
    private String tableName;

    @Excel(name = "表描述")
    private String tableComment;

    @Excel(name = "实体类名称")
    private String className;

    private String tplCategory;

    private String packageName;

    private String moduleName;

    private String businessName;

    private String functionName;

    private String functionAuthor;

    private String genType;

    private String genPath;

    private String options;

    private List<GenTableColumn> columns;

    private String businessNameInit;

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableComment() {
        return tableComment;
    }

    public void setTableComment(String tableComment) {
        this.tableComment = tableComment;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTplCategory() {
        return tplCategory;
    }

    public void setTplCategory(String tplCategory) {
        this.tplCategory = tplCategory;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getFunctionAuthor() {
        return functionAuthor;
    }

    public void setFunctionAuthor(String functionAuthor) {
        this.functionAuthor = functionAuthor;
    }

    public String getGenType() {
        return genType;
    }

    public void setGenType(String genType) {
        this.genType = genType;
    }

    public String getGenPath() {
        return genPath;
    }

    public void setGenPath(String genPath) {
        this.genPath = genPath;
    }

    public String getOptions() {
        return options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public List<GenTableColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<GenTableColumn> columns) {
        this.columns = columns;
    }

    public String getBusinessNameInit() {
        return StringUtils.convertToCamelCase(businessName);
    }

    public void setBusinessNameInit(String businessNameInit) {
        this.businessNameInit = businessNameInit;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("tableId", getTableId())
                .append("tableName", getTableName())
                .append("tableComment", getTableComment())
                .append("className", getClassName())
                .append("tplCategory", getTplCategory())
                .append("packageName", getPackageName())
                .append("moduleName", getModuleName())
                .append("businessName", getBusinessName())
                .append("functionName", getFunctionName())
                .append("functionAuthor", getFunctionAuthor())
                .append("genType", getGenType())
                .append("genPath", getGenPath())
                .toString();
    }
}
