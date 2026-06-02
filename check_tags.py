#!/usr/bin/env python3
import re

def check_html_tags(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 找到所有标签
    open_tags = []
    
    # 使用正则表达式找到所有标签
    tag_pattern = r'<\/?div[^>]*>'
    tags = re.findall(tag_pattern, content)
    
    # 跟踪标签位置
    stack = []
    tag_positions = []
    
    for i, tag in enumerate(tags):
        if tag.startswith('</'):
            # 闭合标签
            if stack:
                stack.pop()
            else:
                print(f"Error: Unmatched closing tag at position {i}: {tag}")
        else:
            # 开始标签
            stack.append(tag)
        
        tag_positions.append((i, tag, len(stack)))
    
    print(f"Total tags found: {len(tags)}")
    print(f"Open tags at end: {len(stack)}")
    
    if stack:
        print("\nUnclosed tags:")
        for tag in stack:
            print(f"  {tag}")
    
    # 打印标签统计
    open_count = sum(1 for tag in tags if not tag.startswith('</'))
    close_count = sum(1 for tag in tags if tag.startswith('</'))
    print(f"\nOpen div tags: {open_count}")
    print(f"Close div tags: {close_count}")
    
    return open_count == close_count

if __name__ == "__main__":
    result = check_html_tags('moyun-portal/src/pages/HomePage.vue')
    print(f"\nTags balanced: {result}")
