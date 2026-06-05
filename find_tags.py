#!/usr/bin/env python3
import re

def find_tags_with_line_numbers(filename):
    with open(filename, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    tag_info = []
    for line_num, line in enumerate(lines, 1):
        # 查找所有div标签
        matches = re.finditer(r'<\/?div[^>]*>', line)
        for match in matches:
            tag = match.group(0)
            tag_info.append((line_num, tag))
    
    # 打印所有标签
    print("All div tags (line number, tag):")
    print("-" * 60)
    stack = []
    for i, (line_num, tag) in enumerate(tag_info):
        if tag.startswith('</'):
            if stack:
                stack.pop()
                print(f"{i:3d} | Line {line_num:4d} | {tag:40s} | stack: {len(stack)}")
            else:
                print(f"{i:3d} | Line {line_num:4d} | {tag:40s} | stack: ERROR - UNMATCHED!")
        else:
            stack.append(tag)
            print(f"{i:3d} | Line {line_num:4d} | {tag:40s} | stack: {len(stack)}")
    
    print(f"\nFinal stack length: {len(stack)}")
    print(f"Total tags: {len(tag_info)}")
    
    if stack:
        print("\nUnclosed tags:")
        for tag in stack:
            print(f"  {tag}")

if __name__ == "__main__":
    find_tags_with_line_numbers('moyun-portal/src/pages/HomePage.vue')
