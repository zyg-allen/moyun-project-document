#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
墨韵智库前后端 API 链路对齐扫描工具
"""
import os
import re
import json
from typing import List, Dict

PORTAL_API_DIR = "/workspace/moyun-project-document/moyun-portal/src/api"
ADMIN_API_DIR = "/workspace/moyun-project-document/moyun-admin-vue/src/api"
CONTROLLER_DIR = "/workspace/moyun-project-document/moyun-server/src/main/java"
PORTAL_ROUTER = "/workspace/moyun-project-document/moyun-portal/src/router/index.ts"
PORTAL_VIEWS = "/workspace/moyun-project-document/moyun-portal/src/pages"


def clean_url(url: str) -> str:
    """清理 URL，移除查询参数，标准化路径"""
    url = url.split('?')[0]
    url = url.rstrip('/')
    if not url.startswith('/'):
        url = '/' + url
    return url


def extract_portal_apis() -> List[Dict]:
    """提取前台 moyun-portal 的所有 API 调用"""
    apis = []
    for root, dirs, files in os.walk(PORTAL_API_DIR):
        for fname in files:
            if not fname.endswith('.ts'):
                continue
            fpath = os.path.join(root, fname)
            with open(fpath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            lines = content.split('\n')
            current_func = None
            
            for i, line in enumerate(lines):
                func_match = re.match(r'\s*export\s+const\s+(\w+)\s*=', line)
                if func_match:
                    current_func = func_match.group(1)
                
                http_match = re.search(
                    r'http(Get|Post|Put|Delete|GetList|Upload)\s*(?:<[^>]*>)?\s*\(\s*[\'`"]([^\'`"]+)[\'`"]',
                    line
                )
                if http_match and current_func:
                    method_raw = http_match.group(1).upper()
                    url_raw = http_match.group(2)
                    method = 'GET' if method_raw == 'GETLIST' else method_raw
                    method = 'POST' if method_raw == 'UPLOAD' else method
                    
                    url = url_raw
                    url = re.sub(r'\$\{[^}]+\}', ':param', url)
                    url = clean_url(url)
                    
                    apis.append({
                        'func_name': current_func,
                        'method': method,
                        'url': url,
                        'file': fpath,
                        'line': i + 1,
                        'source': 'portal'
                    })
    return apis


def extract_admin_apis() -> List[Dict]:
    """提取后台 moyun-admin-vue 的所有 API 调用"""
    apis = []
    for root, dirs, files in os.walk(ADMIN_API_DIR):
        for fname in files:
            if not fname.endswith('.js'):
                continue
            fpath = os.path.join(root, fname)
            with open(fpath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            func_pattern = r'(?:export\s+)?(?:function|const)\s+(\w+)\s*(?:\([^)]*\))?\s*(?:\{|=)'
            for func_match in re.finditer(func_pattern, content):
                func_name = func_match.group(1)
                func_start = func_match.end()
                
                brace_count = 0
                func_end = len(content)
                started = False
                for j in range(func_start, len(content)):
                    if content[j] == '{':
                        brace_count += 1
                        started = True
                    elif content[j] == '}':
                        brace_count -= 1
                        if started and brace_count == 0:
                            func_end = j + 1
                            break
                
                func_body = content[func_start:func_end]
                
                url = None
                method = 'GET'
                
                url_match = re.search(r'url\s*:\s*[\'"]([^\'"]+)[\'"]', func_body)
                if url_match:
                    url = url_match.group(1)
                
                method_match = re.search(r'method\s*:\s*[\'"]([^\'"]+)[\'"]', func_body)
                if method_match:
                    method = method_match.group(1).upper()
                
                if not url:
                    single_match = re.search(r'return\s+request\s*\(\s*[\'"]([^\'"]+)[\'"]', func_body)
                    if single_match:
                        url = single_match.group(1)
                
                if url:
                    url = re.sub(r"'\s*\+\s*[^+]+", '', url)
                    url = re.sub(r'`\$\{[^}]+\}', '', url)
                    url = clean_url(url)
                    
                    apis.append({
                        'func_name': func_name,
                        'method': method,
                        'url': url,
                        'file': fpath,
                        'source': 'admin'
                    })
    return apis


def extract_controllers() -> List[Dict]:
    """提取后端所有 Controller 接口"""
    apis = []
    
    controller_files = []
    for root, dirs, files in os.walk(CONTROLLER_DIR):
        for fname in files:
            if fname.endswith('Controller.java'):
                controller_files.append(os.path.join(root, fname))
    
    for fpath in controller_files:
        with open(fpath, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        class_prefix = ''
        for line in lines:
            m = re.search(r'@RequestMapping\s*\(\s*[\'"]([^\'"]+)[\'"]', line)
            if m:
                class_prefix = m.group(1)
                break
        
        for i, line in enumerate(lines):
            m = re.search(r'@(Get|Post|Put|Delete)Mapping(\s*\((.*?)\))?', line)
            if m:
                method = m.group(1).upper()
                attr_str = m.group(3) or ''
                
                method_path = ''
                if attr_str:
                    path_match = re.search(r'(?:value\s*=\s*)?[\'"]([^\'"]+)[\'"]', attr_str)
                    if path_match:
                        method_path = path_match.group(1)
                
                full_path = class_prefix
                if method_path:
                    full_path = class_prefix + '/' + method_path.lstrip('/')
                
                full_path = clean_url(full_path)
                
                method_name = ''
                for j in range(i+1, min(i+10, len(lines))):
                    mm = re.search(r'public\s+[\w<>\[\]\s,?]+\s+(\w+)\s*\(', lines[j])
                    if mm:
                        method_name = mm.group(1)
                        break
                
                apis.append({
                    'method_name': method_name,
                    'method': method,
                    'url': full_path,
                    'file': fpath,
                    'line': i + 1,
                    'class_prefix': class_prefix
                })
    
    return apis


def normalize_url(url: str) -> str:
    """标准化 URL，将路径参数统一替换"""
    normalized = re.sub(r'\{[^}]+\}', ':param', url)
    normalized = re.sub(r':\w+', ':param', normalized)
    normalized = clean_url(normalized)
    return normalized


def url_match(frontend_url: str, backend_url: str) -> bool:
    """判断前端 URL 和后端 URL 是否匹配（考虑路径参数）"""
    f_norm = normalize_url(frontend_url)
    b_norm = normalize_url(backend_url)
    return f_norm == b_norm


def analyze_alignment(portal_apis: List[Dict], admin_apis: List[Dict], backend_apis: List[Dict]):
    """分析 API 对齐情况"""
    issues = {
        'P0': [],
        'P1': [],
        'P2': []
    }
    
    print("=" * 80)
    print("【前台 Portal API 扫描结果】")
    print(f"共提取 {len(portal_apis)} 个前台 API 调用")
    print()
    
    portal_missing = []
    portal_method_mismatch = []
    portal_matched = 0
    
    for f_api in portal_apis:
        found_exact = False
        found_url_only = None
        
        for b_api in backend_apis:
            if url_match(f_api['url'], b_api['url']):
                if f_api['method'] == b_api['method']:
                    found_exact = True
                    break
                else:
                    if not found_url_only:
                        found_url_only = b_api
        
        if found_exact:
            portal_matched += 1
        elif found_url_only:
            portal_method_mismatch.append({
                'frontend': f_api,
                'backend': found_url_only
            })
        else:
            portal_missing.append(f_api)
    
    print(f"✓ 完全匹配: {portal_matched}")
    print(f"⚠ HTTP 方法不匹配: {len(portal_method_mismatch)}")
    print(f"✗ 后端缺失（404风险）: {len(portal_missing)}")
    print()
    
    if portal_missing:
        print("--- P0: 前端调用了但后端不存在的接口 ---")
        for item in portal_missing:
            issues['P0'].append({
                'type': '前端调用缺失后端接口',
                'frontend_func': item['func_name'],
                'method': item['method'],
                'url': item['url'],
                'frontend_file': item['file'].replace('/workspace/moyun-project-document/', ''),
                'frontend_line': item.get('line', 0),
            })
            print(f"  {item['method']} {item['url']}")
            print(f"    函数: {item['func_name']}")
            print(f"    文件: {item['file'].replace('/workspace/moyun-project-document/', '')}")
        print()
    
    if portal_method_mismatch:
        print("--- P1: HTTP 方法不匹配的接口 ---")
        for item in portal_method_mismatch:
            issues['P1'].append({
                'type': 'HTTP方法不匹配',
                'frontend_func': item['frontend']['func_name'],
                'backend_method': item['backend']['method_name'],
                'frontend_method': item['frontend']['method'],
                'backend_method_http': item['backend']['method'],
                'url': item['frontend']['url'],
                'frontend_file': item['frontend']['file'].replace('/workspace/moyun-project-document/', ''),
                'backend_file': item['backend']['file'].replace('/workspace/moyun-project-document/', ''),
            })
            print(f"  URL: {item['frontend']['url']}")
            print(f"    前端: {item['frontend']['method']} {item['frontend']['func_name']}")
            print(f"    后端: {item['backend']['method']} {item['backend']['method_name']}")
        print()
    
    print("=" * 80)
    print("【后台 Admin API 扫描结果】")
    print(f"共提取 {len(admin_apis)} 个后台 API 调用")
    print()
    
    admin_missing = []
    admin_method_mismatch = []
    admin_matched = 0
    
    for f_api in admin_apis:
        found_exact = False
        found_url_only = None
        
        for b_api in backend_apis:
            if url_match(f_api['url'], b_api['url']):
                if f_api['method'] == b_api['method']:
                    found_exact = True
                    break
                else:
                    if not found_url_only:
                        found_url_only = b_api
        
        if found_exact:
            admin_matched += 1
        elif found_url_only:
            admin_method_mismatch.append({
                'frontend': f_api,
                'backend': found_url_only
            })
        else:
            admin_missing.append(f_api)
    
    print(f"✓ 完全匹配: {admin_matched}")
    print(f"⚠ HTTP 方法不匹配: {len(admin_method_mismatch)}")
    print(f"✗ 后端缺失（404风险）: {len(admin_missing)}")
    print()
    
    if admin_missing:
        print("--- P0: 后台调用了但后端不存在的接口 ---")
        for item in admin_missing:
            issues['P0'].append({
                'type': '后台调用缺失后端接口',
                'frontend_func': item['func_name'],
                'method': item['method'],
                'url': item['url'],
                'frontend_file': item['file'].replace('/workspace/moyun-project-document/', ''),
            })
            print(f"  {item['method']} {item['url']}")
            print(f"    函数: {item['func_name']}")
            print(f"    文件: {item['file'].replace('/workspace/moyun-project-document/', '')}")
        print()
    
    if admin_method_mismatch:
        print("--- P1: 后台 HTTP 方法不匹配的接口 ---")
        for item in admin_method_mismatch:
            issues['P1'].append({
                'type': '后台HTTP方法不匹配',
                'frontend_func': item['frontend']['func_name'],
                'backend_method': item['backend']['method_name'],
                'frontend_method': item['frontend']['method'],
                'backend_method_http': item['backend']['method'],
                'url': item['frontend']['url'],
                'frontend_file': item['frontend']['file'].replace('/workspace/moyun-project-document/', ''),
                'backend_file': item['backend']['file'].replace('/workspace/moyun-project-document/', ''),
            })
            print(f"  URL: {item['frontend']['url']}")
            print(f"    前端: {item['frontend']['method']} {item['frontend']['func_name']}")
            print(f"    后端: {item['backend']['method']} {item['backend']['method_name']}")
        print()
    
    print("=" * 80)
    print("【后端接口分析】")
    print(f"共提取 {len(backend_apis)} 个后端接口")
    print()
    
    backend_portal_apis = [api for api in backend_apis if '/portal/' in api['url']]
    backend_cms_apis = [api for api in backend_apis if '/cms/' in api['url']]
    backend_system_apis = [api for api in backend_apis if '/system/' in api['url']]
    
    print(f"  Portal 前缀接口: {len(backend_portal_apis)}")
    print(f"  CMS 前缀接口: {len(backend_cms_apis)}")
    print(f"  System 前缀接口: {len(backend_system_apis)}")
    print(f"  其他接口: {len(backend_apis) - len(backend_portal_apis) - len(backend_cms_apis) - len(backend_system_apis)}")
    print()
    
    portal_unused = []
    for b_api in backend_portal_apis:
        used = False
        for f_api in portal_apis:
            if url_match(f_api['url'], b_api['url']) and f_api['method'] == b_api['method']:
                used = True
                break
        if not used:
            portal_unused.append(b_api)
    
    print(f"--- P2: 后端 Portal 接口但前台未调用 (可能废弃或未启用) ---")
    print(f"共 {len(portal_unused)} 个")
    for item in portal_unused[:30]:
        issues['P2'].append({
            'type': '后端Portal接口前台未调用',
            'backend_method': item['method_name'],
            'method': item['method'],
            'url': item['url'],
            'backend_file': item['file'].replace('/workspace/moyun-project-document/', ''),
        })
    if len(portal_unused) > 30:
        print(f"  ... 还有 {len(portal_unused) - 30} 个")
    print()
    
    cms_unused = []
    for b_api in backend_cms_apis:
        used = False
        for f_api in admin_apis:
            if url_match(f_api['url'], b_api['url']) and f_api['method'] == b_api['method']:
                used = True
                break
        if not used:
            cms_unused.append(b_api)
    
    print(f"--- P2: 后端 CMS 接口但后台未调用 (可能废弃或未启用) ---")
    print(f"共 {len(cms_unused)} 个")
    for item in cms_unused[:20]:
        issues['P2'].append({
            'type': '后端CMS接口后台未调用',
            'backend_method': item['method_name'],
            'method': item['method'],
            'url': item['url'],
            'backend_file': item['file'].replace('/workspace/moyun-project-document/', ''),
        })
    if len(cms_unused) > 20:
        print(f"  ... 还有 {len(cms_unused) - 20} 个")
    print()
    
    return issues, portal_apis, admin_apis, backend_apis, portal_unused, cms_unused


def analyze_router_alignment():
    """分析路由与页面对齐情况"""
    print("=" * 80)
    print("【路由与页面对齐分析】")
    print()
    
    router_issues = []
    
    if not os.path.exists(PORTAL_ROUTER):
        print(f"路由文件不存在: {PORTAL_ROUTER}")
        
        router_files = []
        portal_src = os.path.dirname(PORTAL_ROUTER)
        if os.path.exists(portal_src):
            for root, dirs, files in os.walk(portal_src):
                for fname in files:
                    if 'router' in fname.lower() and (fname.endswith('.ts') or fname.endswith('.js')):
                        router_files.append(os.path.join(root, fname))
        
        if router_files:
            print(f"找到以下可能的路由文件:")
            for rf in router_files:
                print(f"  {rf}")
        return router_issues
    
    with open(PORTAL_ROUTER, 'r', encoding='utf-8') as f:
        content = f.read()
    
    component_pattern = r'component\s*:\s*\(\)\s*=>\s*import\s*\(\s*[\'"]@/pages/([^\'"]+)(?:\.vue)?[\'"]'
    
    route_components = []
    for m in re.finditer(component_pattern, content):
        comp_path = m.group(1)
        route_components.append(comp_path)
    
    print(f"共找到 {len(route_components)} 个路由组件引用")
    print()
    
    missing_pages = []
    for comp in route_components:
        full_path = os.path.join(PORTAL_VIEWS, comp + '.vue')
        if not os.path.exists(full_path):
            alt_path = os.path.join(PORTAL_VIEWS, comp, 'index.vue')
            if not os.path.exists(alt_path):
                missing_pages.append(comp)
    
    if missing_pages:
        print("--- P1: 有路由但页面文件不存在 ---")
        for page in missing_pages:
            router_issues.append({
                'type': '路由指向不存在的页面',
                'component': page,
            })
            print(f"  {page}.vue")
        print()
    else:
        print("✓ 所有路由引用的页面文件都存在")
        print()
    
    vue_files = []
    if os.path.exists(PORTAL_VIEWS):
        for root, dirs, files in os.walk(PORTAL_VIEWS):
            for fname in files:
                if fname.endswith('.vue'):
                    full = os.path.join(root, fname)
                    rel = os.path.relpath(full, PORTAL_VIEWS)
                    rel = rel.replace('\\', '/').replace('.vue', '')
                    vue_files.append(rel)
        
        unused_pages = []
        for vf in vue_files:
            vf_base = vf.replace('/index', '')
            found = False
            for rc in route_components:
                rc_base = rc.replace('/index', '')
                if vf_base == rc_base:
                    found = True
                    break
            if not found and 'components/' not in vf and 'common/' not in vf:
                unused_pages.append(vf)
        
        if unused_pages:
            print(f"--- P2: 有页面但无路由引用 (可能是废弃页面或组件) ---")
            print(f"共 {len(unused_pages)} 个")
            for page in unused_pages[:20]:
                router_issues.append({
                    'type': '页面无路由引用',
                    'component': page,
                })
                print(f"  {page}.vue")
            if len(unused_pages) > 20:
                print(f"  ... 还有 {len(unused_pages) - 20} 个")
            print()
    
    return router_issues


def main():
    print("墨韵智库前后端 API 链路对齐扫描")
    print("=" * 80)
    print()
    
    print("正在提取前台 API...")
    portal_apis = extract_portal_apis()
    
    print("正在提取后台 API...")
    admin_apis = extract_admin_apis()
    
    print("正在提取后端 Controller...")
    backend_apis = extract_controllers()
    
    print()
    
    issues, portal_apis, admin_apis, backend_apis, portal_unused, cms_unused = analyze_alignment(
        portal_apis, admin_apis, backend_apis
    )
    
    router_issues = analyze_router_alignment()
    
    print("=" * 80)
    print("【汇总报告】")
    print()
    print(f"前台 API 调用数: {len(portal_apis)}")
    print(f"后台 API 调用数: {len(admin_apis)}")
    print(f"后端接口总数: {len(backend_apis)}")
    print()
    print(f"P0 阻断问题: {len(issues['P0'])} 个")
    print(f"P1 严重问题: {len(issues['P1'])} 个")
    print(f"P2 中等问题: {len(issues['P2'])} 个")
    print(f"路由问题: {len(router_issues)} 个")
    print()
    
    report = {
        'summary': {
            'P0': len(issues['P0']),
            'P1': len(issues['P1']),
            'P2': len(issues['P2']),
            'router_issues': len(router_issues),
            'portal_api_count': len(portal_apis),
            'admin_api_count': len(admin_apis),
            'backend_api_count': len(backend_apis),
        },
        'P0_issues': issues['P0'],
        'P1_issues': issues['P1'],
        'P2_issues': issues['P2'],
        'router_issues': router_issues,
    }
    
    report_path = '/workspace/moyun-project-document/api-alignment-report.json'
    with open(report_path, 'w', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    
    print(f"详细报告已保存到: {report_path}")


if __name__ == '__main__':
    main()
