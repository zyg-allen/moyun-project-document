#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
墨韵智库项目 SQL/Entity/Mapper 一致性扫描脚本
"""

import os
import re
import glob
from collections import defaultdict

BASE_DIR = "/workspace/moyun-project-document/moyun-server"
SQL_DIR = os.path.join(BASE_DIR, "src/main/resources/sql")
ENTITY_DIR = os.path.join(BASE_DIR, "src/main/java/com/moyun/portal/domain/entity")
MAPPER_DIR = os.path.join(BASE_DIR, "src/main/java/com/moyun/portal/mapper")
XML_DIR = os.path.join(BASE_DIR, "src/main/resources/mapper/portal")

issues = []

def add_issue(severity, file_path, description, suggestion):
    issues.append({
        "severity": severity,
        "file_path": file_path,
        "description": description,
        "suggestion": suggestion
    })

def scan_sql_files():
    """扫描SQL脚本完整性"""
    print("=" * 60)
    print("1. 扫描 SQL 脚本完整性")
    print("=" * 60)
    
    sql_files = sorted(glob.glob(os.path.join(SQL_DIR, "*.sql")))
    print(f"发现 {len(sql_files)} 个 SQL 文件")
    
    # 检查编号连续性
    numbers = []
    for f in sql_files:
        basename = os.path.basename(f)
        match = re.match(r'^(\d+)_', basename)
        if match:
            numbers.append(int(match.group(1)))
    
    numbers_sorted = sorted(set(numbers))
    print(f"SQL 编号范围: {min(numbers_sorted)} - {max(numbers_sorted)}")
    
    # 检查缺失的编号
    expected = set(range(min(numbers_sorted), max(numbers_sorted) + 1))
    missing = expected - set(numbers_sorted)
    if missing:
        add_issue("P2", "src/main/resources/sql/", 
                  f"SQL 脚本编号不连续，缺失编号: {sorted(missing)}",
                  "确认这些编号的脚本是否遗漏，或调整命名保持连续")
    
    # 检查重复编号
    from collections import Counter
    num_counts = Counter(numbers)
    duplicates = {k: v for k, v in num_counts.items() if v > 1}
    if duplicates:
        dup_files = {}
        for num in duplicates:
            dup_files[num] = [os.path.basename(f) for f in sql_files if os.path.basename(f).startswith(f"{num:02d}_") or os.path.basename(f).startswith(f"{num}_")]
        add_issue("P2", "src/main/resources/sql/",
                  f"存在重复编号的 SQL 脚本: {dup_files}",
                  "重命名文件以保证编号唯一")
    
    # 提取所有表名和建表语句
    all_tables = defaultdict(list)  # table_name -> list of (file, line)
    table_columns = defaultdict(dict)  # table_name -> {col_name: col_def}
    table_indexes = defaultdict(list)  # table_name -> list of index defs
    table_primary_keys = defaultdict(list)
    
    for sql_file in sql_files:
        rel_path = os.path.relpath(sql_file, BASE_DIR)
        with open(sql_file, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
            lines = content.split('\n')
        
        # 检查缺少分号的语句（粗略检查 CREATE TABLE/ALTER TABLE 结尾）
        # 查找 CREATE TABLE 语句
        create_table_pattern = re.compile(
            r'CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[`"]?(\w+)[`"]?\s*\(',
            re.IGNORECASE
        )
        
        for match in create_table_pattern.finditer(content):
            table_name = match.group(1).lower()
            line_num = content[:match.start()].count('\n') + 1
            all_tables[table_name].append((rel_path, line_num))
            
            # 提取表定义（从开头到结尾的括号）
            # 找到匹配的闭合括号
            start_idx = match.end() - 1  # '(' 的位置
            depth = 1
            end_idx = start_idx
            for i in range(start_idx + 1, len(content)):
                if content[i] == '(':
                    depth += 1
                elif content[i] == ')':
                    depth -= 1
                    if depth == 0:
                        end_idx = i
                        break
            
            table_def = content[start_idx + 1:end_idx]
            
            # 解析列和索引
            col_lines = table_def.split(',')
            for col_line in col_lines:
                col_line = col_line.strip()
                if not col_line or col_line.upper().startswith('PRIMARY KEY') or col_line.upper().startswith('UNIQUE') or col_line.upper().startswith('KEY ') or col_line.upper().startswith('INDEX') or col_line.upper().startswith('CONSTRAINT'):
                    # 索引/约束
                    if 'PRIMARY KEY' in col_line.upper():
                        table_primary_keys[table_name].append(col_line)
                    elif 'KEY ' in col_line.upper() or 'INDEX' in col_line.upper():
                        table_indexes[table_name].append(col_line)
                    continue
                
                # 列定义
                col_match = re.match(r'[`"]?(\w+)[`"]?\s+', col_line)
                if col_match:
                    col_name = col_match.group(1).lower()
                    table_columns[table_name][col_name] = col_line
            
            # 检查 ENGINE
            after_table = content[end_idx:end_idx + 200]
            if 'ENGINE' not in after_table.upper():
                add_issue("P3", rel_path,
                          f"表 {table_name} 的 CREATE TABLE 语句缺少 ENGINE 子句",
                          "添加 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
    
    # 检查重复建表
    for table_name, occurrences in all_tables.items():
        if len(occurrences) > 1:
            files_info = ", ".join([f"{f}:{l}" for f, l in occurrences])
            add_issue("P1", "src/main/resources/sql/",
                      f"表 {table_name} 被重复创建，出现在: {files_info}",
                      "使用 CREATE TABLE IF NOT EXISTS，或删除重复的建表语句")
    
    print(f"共发现 {len(all_tables)} 个不同的表")
    
    return all_tables, table_columns, table_indexes, table_primary_keys

def scan_entities(all_tables, table_columns):
    """扫描实体与表对应关系"""
    print("\n" + "=" * 60)
    print("2. 扫描实体与表对应关系")
    print("=" * 60)
    
    entity_files = glob.glob(os.path.join(ENTITY_DIR, "*.java"))
    entity_files = [f for f in entity_files if not f.endswith("package-info.java")]
    print(f"发现 {len(entity_files)} 个 Entity 文件")
    
    entity_tables = {}  # table_name -> (entity_file, class_name)
    entity_fields = defaultdict(list)
    entity_extends_base = {}
    entity_table_field_annotations = defaultdict(list)
    
    for entity_file in entity_files:
        rel_path = os.path.relpath(entity_file, BASE_DIR)
        with open(entity_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 提取 @TableName
        table_match = re.search(r'@TableName\s*\(\s*"(\w+)"\s*\)', content)
        if not table_match:
            add_issue("P1", rel_path, "Entity 缺少 @TableName 注解", "添加 @TableName 注解指定表名")
            continue
        
        table_name = table_match.group(1).lower()
        
        # 提取类名
        class_match = re.search(r'public\s+class\s+(\w+)\s+extends\s+(\w+)', content)
        if not class_match:
            class_match = re.search(r'public\s+class\s+(\w+)', content)
            if class_match:
                class_name = class_match.group(1)
                extends_base = False
            else:
                continue
        else:
            class_name = class_match.group(1)
            parent_class = class_match.group(2)
            extends_base = parent_class == "BaseEntity" or parent_class == "TreeEntity"
        
        entity_tables[table_name] = (rel_path, class_name)
        entity_extends_base[table_name] = extends_base
        
        # 提取字段（带有 @TableField 或普通字段）
        # 先提取所有 private 字段
        field_pattern = re.finditer(
            r'(?:@TableField\(.*?\)\s+)?private\s+[\w<>\[\]]+\s+(\w+)\s*;',
            content
        )
        for fm in field_pattern:
            field_name = fm.group(1)
            entity_fields[table_name].append(field_name)
        
        # 提取 @TableField(exist = false) 的字段
        exist_false_pattern = re.finditer(
            r'@TableField\(.*?exist\s*=\s*false.*?\)\s+private\s+[\w<>\[\]]+\s+(\w+)\s*;',
            content,
            re.DOTALL
        )
        for fm in exist_false_pattern:
            entity_table_field_annotations[table_name].append(fm.group(1))
    
    # 检查：Entity 的表在 SQL 中是否存在
    for table_name, (rel_path, class_name) in entity_tables.items():
        if table_name not in all_tables:
            add_issue("P0", rel_path,
                      f"Entity {class_name} 的 @TableName({table_name}) 在 SQL 脚本中未找到对应表定义",
                      "添加 SQL 建表脚本，或修正 @TableName 注解")
    
    # 检查：SQL 中的表是否有对应 Entity（只检查 portal_ 开头的表）
    tables_without_entity = []
    for table_name in all_tables:
        if table_name.startswith('portal_') and table_name not in entity_tables:
            tables_without_entity.append(table_name)
    
    if tables_without_entity:
        add_issue("P2", "src/main/java/com/moyun/portal/domain/entity/",
                  f"以下表缺少对应 Entity: {tables_without_entity}",
                  "为这些表创建对应的 Entity 类")
    
    print(f"共发现 {len(entity_tables)} 个 Entity 对应表")
    
    return entity_tables, entity_fields, entity_extends_base, entity_table_field_annotations

def scan_mappers(entity_tables):
    """扫描Mapper与实体对应关系"""
    print("\n" + "=" * 60)
    print("3. 扫描 Mapper 与实体对应关系")
    print("=" * 60)
    
    mapper_files = glob.glob(os.path.join(MAPPER_DIR, "*.java"))
    xml_files = glob.glob(os.path.join(XML_DIR, "*.xml"))
    
    print(f"发现 {len(mapper_files)} 个 Mapper 接口")
    print(f"发现 {len(xml_files)} 个 Mapper XML")
    
    mapper_names = set()
    mapper_entity_map = {}  # mapper_name -> entity_class
    
    for mapper_file in mapper_files:
        rel_path = os.path.relpath(mapper_file, BASE_DIR)
        basename = os.path.basename(mapper_file).replace('.java', '')
        mapper_names.add(basename)
        
        with open(mapper_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 提取泛型中的 Entity 类名
        match = re.search(r'extends\s+BaseMapper<(\w+)>', content)
        if match:
            entity_class = match.group(1)
            mapper_entity_map[basename] = entity_class
    
    # XML 文件名集合
    xml_names = set(os.path.basename(f).replace('.xml', '') for f in xml_files)
    
    # 检查 Entity 是否都有 Mapper
    entities_without_mapper = []
    for table_name, (rel_path, class_name) in entity_tables.items():
        expected_mapper = class_name + "Mapper"
        if expected_mapper not in mapper_names:
            entities_without_mapper.append(class_name)
    
    if entities_without_mapper:
        add_issue("P2", "src/main/java/com/moyun/portal/mapper/",
                  f"以下 Entity 缺少对应 Mapper: {entities_without_mapper}",
                  "为这些 Entity 创建对应的 Mapper 接口")
    
    # 检查 Mapper 是否都有 XML
    mappers_without_xml = mapper_names - xml_names
    if mappers_without_xml:
        add_issue("P3", "src/main/resources/mapper/portal/",
                  f"以下 Mapper 缺少对应 XML 文件: {sorted(mappers_without_xml)}",
                  "创建对应的 Mapper XML 文件（如果需要自定义 SQL）")
    
    # 检查 XML 是否都有 Mapper
    xml_without_mapper = xml_names - mapper_names
    if xml_without_mapper:
        add_issue("P2", "src/main/resources/mapper/portal/",
                  f"以下 XML 文件缺少对应 Mapper 接口: {sorted(xml_without_mapper)}",
                  "创建对应的 Mapper 接口，或删除无用的 XML 文件")
    
    # 检查 XML 中的 resultMap/resultType
    for xml_file in xml_files:
        rel_path = os.path.relpath(xml_file, BASE_DIR)
        with open(xml_file, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 检查 resultType 引用的类是否存在
        result_types = re.findall(r'resultType\s*=\s*"([^"]+)"', content)
        for rt in result_types:
            if rt.startswith('com.moyun.'):
                class_name = rt.split('.')[-1]
                java_path = os.path.join(BASE_DIR, "src/main/java", rt.replace('.', '/') + ".java")
                if not os.path.exists(java_path):
                    # 可能是内部类等，不完全准确，先标记为P3
                    pass
    
    return mapper_names, xml_names, mapper_entity_map

def scan_menu_permissions():
    """扫描菜单与权限SQL"""
    print("\n" + "=" * 60)
    print("4. 扫描菜单与权限 SQL")
    print("=" * 60)
    
    sql_files = sorted(glob.glob(os.path.join(SQL_DIR, "*.sql")))
    menu_entries = []
    
    for sql_file in sql_files:
        rel_path = os.path.relpath(sql_file, BASE_DIR)
        with open(sql_file, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        
        # 查找 INSERT INTO sys_menu 语句
        if 'sys_menu' in content.lower() and 'insert' in content.lower():
            # 简单提取 perms 字段
            perms_matches = re.findall(r"INSERT\s+INTO\s+.*?sys_menu.*?VALUES\s*\((.+?)\);", content, re.IGNORECASE | re.DOTALL)
            for match in perms_matches:
                menu_entries.append((rel_path, match[:200]))
    
    print(f"发现包含 sys_menu 的 SQL 脚本（含 INSERT）")
    
    # 查找 Controller 中的 @PreAuthorize
    controller_dir = os.path.join(BASE_DIR, "src/main/java/com/moyun/portal/controller")
    controller_files = glob.glob(os.path.join(controller_dir, "*.java"))
    
    preauthorize_perms = set()
    for cf in controller_files:
        with open(cf, 'r', encoding='utf-8') as f:
            content = f.read()
        perms = re.findall(r'@PreAuthorize\(.*?hasAuthority\s*\(\s*"([^"]+)"\s*\).*?\)', content)
        preauthorize_perms.update(perms)
    
    print(f"Controller 中发现 {len(preauthorize_perms)} 个权限标识")
    
    return preauthorize_perms

def scan_indexes(all_tables, table_columns, table_indexes, table_primary_keys):
    """扫描索引与约束"""
    print("\n" + "=" * 60)
    print("5. 扫描索引与约束")
    print("=" * 60)
    
    # 列出完全没有二级索引的表
    tables_no_secondary_index = []
    tables_with_userid_no_index = []
    
    for table_name in all_tables:
        pk_count = len(table_primary_keys.get(table_name, []))
        idx_count = len(table_indexes.get(table_name, []))
        
        if idx_count == 0:
            tables_no_secondary_index.append(table_name)
        
        # 检查有 user_id 字段但没有 idx_user 索引
        columns = table_columns.get(table_name, {})
        if 'user_id' in columns:
            has_user_index = False
            for idx in table_indexes.get(table_name, []):
                if 'user_id' in idx.lower() or 'idx_user' in idx.lower():
                    has_user_index = True
                    break
            if not has_user_index:
                tables_with_userid_no_index.append(table_name)
    
    if tables_no_secondary_index:
        add_issue("P3", "src/main/resources/sql/",
                  f"以下表完全没有二级索引（可能影响查询性能）: {sorted(tables_no_secondary_index)}",
                  "根据查询场景添加合适的索引")
    
    if tables_with_userid_no_index:
        add_issue("P2", "src/main/resources/sql/",
                  f"以下表有 user_id 字段但缺少用户相关索引: {sorted(tables_with_userid_no_index)}",
                  "添加 idx_user_id 索引以优化用户维度查询")

def scan_base_entity(entity_fields, entity_extends_base, table_columns, entity_table_field_annotations):
    """扫描BaseEntity字段一致性"""
    print("\n" + "=" * 60)
    print("6. 扫描 BaseEntity 字段一致性")
    print("=" * 60)
    
    base_entity_fields = ['create_by', 'create_time', 'update_by', 'update_time', 'remark']
    
    # 检查 Entity 是否正确 extend BaseEntity（有这些字段但没 extend）
    # 和：extend 了 BaseEntity 但 SQL 表中没有这些字段
    
    entities_missing_base_extend = []
    tables_missing_base_fields = []
    tables_have_base_fields_wrong = []
    
    for table_name, extends in entity_extends_base.items():
        if not extends:
            # 检查实体是否有类似 create_time 的字段
            fields = [f.lower() for f in entity_fields.get(table_name, [])]
            has_base_fields = any(f in fields for f in base_entity_fields)
            if has_base_fields:
                entities_missing_base_extend.append(table_name)
    
    if entities_missing_base_extend:
        add_issue("P2", "src/main/java/com/moyun/portal/domain/entity/",
                  f"以下 Entity 包含 BaseEntity 字段但未 extend BaseEntity: {entities_missing_base_extend}",
                  "让这些 Entity 继承 BaseEntity 以统一公共字段处理")
    
    # 检查 SQL 表中有 create_by/create_time/update_by/update_time/remark（业务表应该用自己的）
    # 这一条需要根据项目约定来判断，先不报告
    
    # 检查 extend BaseEntity 的表是否有对应字段
    for table_name, extends in entity_extends_base.items():
        if extends:
            columns = table_columns.get(table_name, {})
            missing = []
            for f in base_entity_fields:
                if f not in columns:
                    missing.append(f)
            if missing and table_name.startswith('portal_'):
                tables_missing_base_fields.append((table_name, missing))
    
    if tables_missing_base_fields:
        table_info = ", ".join([f"{t}(缺: {m})" for t, m in tables_missing_base_fields[:5]])
        add_issue("P2", "src/main/resources/sql/",
                  f"以下表缺少 BaseEntity 公共字段（实体继承了 BaseEntity 但表中无对应列）: 共 {len(tables_missing_base_fields)} 个表，例如: {table_info}",
                  "在表中添加 create_by, create_time, update_by, update_time, remark 字段")

def main():
    print("墨韵智库项目 - SQL/Entity/Mapper 一致性扫描")
    print(f"项目路径: {BASE_DIR}")
    print()
    
    # 1. 扫描 SQL
    all_tables, table_columns, table_indexes, table_primary_keys = scan_sql_files()
    
    # 2. 扫描 Entity
    entity_tables, entity_fields, entity_extends_base, entity_table_field_annotations = scan_entities(all_tables, table_columns)
    
    # 3. 扫描 Mapper
    mapper_names, xml_names, mapper_entity_map = scan_mappers(entity_tables)
    
    # 4. 扫描菜单权限
    preauthorize_perms = scan_menu_permissions()
    
    # 5. 扫描索引
    scan_indexes(all_tables, table_columns, table_indexes, table_primary_keys)
    
    # 6. 扫描 BaseEntity
    scan_base_entity(entity_fields, entity_extends_base, table_columns, entity_table_field_annotations)
    
    # 输出报告
    print("\n" + "=" * 60)
    print("扫描结果汇总")
    print("=" * 60)
    
    severity_order = ['P0', 'P1', 'P2', 'P3']
    severity_names = {'P0': '阻断', 'P1': '严重', 'P2': '中等', 'P3': '轻微'}
    
    sorted_issues = sorted(issues, key=lambda x: severity_order.index(x['severity']))
    
    for sev in severity_order:
        sev_issues = [i for i in sorted_issues if i['severity'] == sev]
        if sev_issues:
            print(f"\n【{sev} - {severity_names[sev]}】共 {len(sev_issues)} 个问题")
            print("-" * 60)
            for idx, issue in enumerate(sev_issues, 1):
                print(f"\n{idx}. [{issue['severity']}] {issue['file_path']}")
                print(f"   问题: {issue['description']}")
                print(f"   建议: {issue['suggestion']}")
    
    print(f"\n总计: {len(issues)} 个问题")
    print(f"  P0 (阻断): {len([i for i in issues if i['severity']=='P0'])}")
    print(f"  P1 (严重): {len([i for i in issues if i['severity']=='P1'])}")
    print(f"  P2 (中等): {len([i for i in issues if i['severity']=='P2'])}")
    print(f"  P3 (轻微): {len([i for i in issues if i['severity']=='P3'])}")
    
    return sorted_issues

if __name__ == "__main__":
    main()
