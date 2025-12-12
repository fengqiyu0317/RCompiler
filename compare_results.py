#!/usr/bin/env python3
import re

def parse_result_out(filename):
    """解析 result.out 文件，提取测试名称和状态"""
    results = {}
    with open(filename, 'r') as f:
        lines = f.readlines()
    
    i = 0
    while i < len(lines):
        line = lines[i]
        # 匹配 "Processing test X: testname" 行
        match = re.match(r'Processing test \d+: (\w+)', line)
        if match:
            test_name = match.group(1)
            # 查找状态行，可能在后面几行
            j = i + 1
            while j < len(lines) and j < i + 5:  # 最多检查后面4行
                status_line = lines[j]
                if "Status:" in status_line:
                    if "SUCCESS" in status_line:
                        results[test_name] = "SUCCESS"
                    elif "FAILED" in status_line:
                        results[test_name] = "FAILED"
                    break
                j += 1
        i += 1
    
    return results

def parse_answer_out(filename):
    """解析 answer.out 文件，提取测试名称和评判"""
    results = {}
    with open(filename, 'r') as f:
        lines = f.readlines()
    
    current_test = None
    for line in lines:
        # 匹配 "File: .../testname/testname.rx" 行
        match = re.search(r'/(\w+)/\1\.rx$', line)
        if match:
            current_test = match.group(1)
        # 匹配 "Verdict: value" 行
        elif current_test and "Verdict:" in line:
            verdict = line.split(":")[1].strip()
            results[current_test] = verdict
            current_test = None
    
    return results

def compare_results(result_file, answer_file):
    """比较两个结果文件，找出不一致的数据点"""
    result_data = parse_result_out(result_file)
    answer_data = parse_answer_out(answer_file)
    
    # 创建映射关系
    # SUCCESS -> Success
    # FAILED -> Fail
    # Pass 在 result.out 中没有直接对应，需要特殊处理
    
    inconsistencies = []
    
    # 检查所有在 answer.out 中的测试
    for test_name, answer_verdict in answer_data.items():
        if test_name in result_data:
            result_status = result_data[test_name]
            
            # 映射关系
            if answer_verdict == "Success" and result_status != "SUCCESS":
                inconsistencies.append((test_name, answer_verdict, result_status))
            elif answer_verdict == "Fail" and result_status != "FAILED":
                inconsistencies.append((test_name, answer_verdict, result_status))
            elif answer_verdict == "Pass" and result_status != "SUCCESS":  # Pass 应该对应 SUCCESS
                inconsistencies.append((test_name, answer_verdict, result_status))
        else:
            inconsistencies.append((test_name, answer_verdict, "NOT_FOUND"))
    
    # 检查在 result.out 中但不在 answer.out 中的测试
    for test_name, result_status in result_data.items():
        if test_name not in answer_data:
            inconsistencies.append((test_name, "NOT_FOUND", result_status))
    
    return inconsistencies

def main():
    result_file = "result.out"
    answer_file = "answer.out"
    
    inconsistencies = compare_results(result_file, answer_file)
    
    print(f"找到 {len(inconsistencies)} 个不一致的数据点:")
    print("=" * 80)
    
    for test_name, expected, actual in inconsistencies:
        print(f"测试: {test_name}")
        print(f"  预期: {expected}")
        print(f"  实际: {actual}")
        print()
    
    # 按类别统计
    categories = {}
    for test_name, expected, actual in inconsistencies:
        # 提取类别（测试名前的部分）
        if '_' in test_name:
            category = test_name.split('_')[0]
        else:
            # 对于纯数字命名的测试，如 basic1, basic2 等
            match = re.match(r'([a-zA-Z]+)', test_name)
            if match:
                category = match.group(1)
            else:
                category = "unknown"
        
        if category not in categories:
            categories[category] = []
        categories[category].append((test_name, expected, actual))
    
    print("\n按类别统计:")
    print("=" * 80)
    for category, items in sorted(categories.items()):
        print(f"{category} 类别: {len(items)} 个不一致")
        for test_name, expected, actual in items:
            print(f"  {test_name}: 预期 {expected}, 实际 {actual}")
        print()

if __name__ == "__main__":
    main()