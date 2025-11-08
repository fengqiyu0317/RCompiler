# RCompiler 详细测试计划

## 概述
本文档提供了基于 `testcase.md` 要求的详细测试计划，包括所需的测试程序、函数和步骤。

## 测试目标
对 RCompiler-Testcases 文件夹中的所有 `.rx` 文件进行词法分析，并将结果分类保存到 `result` 文件夹中。

## 测试环境准备

### 1. 编译词法分析程序
```bash
# 编译所有必要的 Java 文件
javac *.java
```

### 2. 创建结果目录结构
```bash
# 创建主结果目录
mkdir -p result

# 创建子目录结构
mkdir -p result/semantic-1
mkdir -p result/semantic-2
```

## 测试程序设计

### 1. 批处理测试脚本 (BatchTestRunner.java)

```java
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BatchTestRunner {
    private static final String TESTCASES_DIR = "RCompiler-Testcases";
    private static final String RESULT_DIR = "result";
    private static final String[] SEMANTIC_STAGES = {"semantic-1", "semantic-2"};
    
    public static void main(String[] args) {
        try {
            // 创建结果目录
            createResultDirectories();
            
            // 处理每个语义阶段
            for (String stage : SEMANTIC_STAGES) {
                processStage(stage);
            }
            
            System.out.println("测试完成，结果已保存到 " + RESULT_DIR + " 目录");
        } catch (Exception e) {
            System.err.println("测试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createResultDirectories() throws IOException {
        Files.createDirectories(Paths.get(RESULT_DIR));
        for (String stage : SEMANTIC_STAGES) {
            Files.createDirectories(Paths.get(RESULT_DIR, stage));
        }
    }
    
    private static void processStage(String stage) throws IOException {
        Path stagePath = Paths.get(TESTCASES_DIR, stage);
        
        if (!Files.exists(stagePath)) {
            System.err.println("警告: 目录不存在 " + stagePath);
            return;
        }
        
        // 获取所有子目录
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(stagePath)) {
            for (Path testCaseDir : stream) {
                if (Files.isDirectory(testCaseDir)) {
                    processTestCase(testCaseDir, stage);
                }
            }
        }
    }
    
    private static void processTestCase(Path testCaseDir, String stage) throws IOException {
        String testCaseName = testCaseDir.getFileName().toString();
        
        // 查找 .rx 文件
        Path rxFile = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(testCaseDir, "*.rx")) {
            for (Path file : stream) {
                rxFile = file;
                break; // 只取第一个 .rx 文件
            }
        }
        
        if (rxFile == null) {
            System.err.println("警告: 在 " + testCaseDir + " 中未找到 .rx 文件");
            return;
        }
        
        // 运行词法分析并保存结果
        runLexicalAnalysis(rxFile, testCaseName, stage);
    }
    
    private static void runLexicalAnalysis(Path rxFile, String testCaseName, String stage) throws IOException {
        // 构建输出文件路径
        Path outputFile = Paths.get(RESULT_DIR, stage, testCaseName + ".txt");
        
        // 构建命令
        ProcessBuilder pb = new ProcessBuilder("java", "Main");
        pb.redirectInput(rxFile.toFile());
        pb.redirectOutput(outputFile.toFile());
        pb.redirectError(ProcessBuilder.Redirect.INHERIT); // 错误输出到控制台
        
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                System.err.println("警告: 处理 " + rxFile + " 时退出码为 " + exitCode);
            } else {
                System.out.println("成功处理: " + rxFile + " -> " + outputFile);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("处理 " + rxFile + " 时被中断", e);
        }
    }
}
```

### 2. 测试结果验证器 (TestResultValidator.java)

```java
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TestResultValidator {
    private static final String RESULT_DIR = "result";
    private static final String[] SEMANTIC_STAGES = {"semantic-1", "semantic-2"};
    
    public static void main(String[] args) {
        try {
            validateResults();
        } catch (Exception e) {
            System.err.println("验证过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void validateResults() throws IOException {
        Map<String, Integer> stageCounts = new HashMap<>();
        int totalFiles = 0;
        int totalEmptyFiles = 0;
        
        for (String stage : SEMANTIC_STAGES) {
            Path stagePath = Paths.get(RESULT_DIR, stage);
            
            if (!Files.exists(stagePath)) {
                System.err.println("警告: 结果目录不存在 " + stagePath);
                continue;
            }
            
            int fileCount = 0;
            int emptyFileCount = 0;
            
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(stagePath, "*.txt")) {
                for (Path file : stream) {
                    fileCount++;
                    totalFiles++;
                    
                    // 检查文件是否为空
                    if (Files.size(file) == 0) {
                        emptyFileCount++;
                        totalEmptyFiles++;
                        System.err.println("警告: 空结果文件 " + file);
                    }
                }
            }
            
            stageCounts.put(stage, fileCount);
            System.out.println(stage + ": " + fileCount + " 个文件，其中 " + emptyFileCount + " 个为空");
        }
        
        System.out.println("\n总计:");
        System.out.println("处理文件数: " + totalFiles);
        System.out.println("空结果文件数: " + totalEmptyFiles);
        
        if (totalEmptyFiles > 0) {
            System.err.println("警告: 发现 " + totalEmptyFiles + " 个空结果文件，可能需要检查词法分析器");
        }
    }
}
```

### 3. 测试统计报告生成器 (TestReportGenerator.java)

```java
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class TestReportGenerator {
    private static final String RESULT_DIR = "result";
    private static final String[] SEMANTIC_STAGES = {"semantic-1", "semantic-2"};
    
    public static void main(String[] args) {
        try {
            generateReport();
        } catch (Exception e) {
            System.err.println("生成报告时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void generateReport() throws IOException {
        Path reportFile = Paths.get("test_report.txt");
        
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(reportFile))) {
            writer.println("RCompiler 词法分析测试报告");
            writer.println("生成时间: " + new Date());
            writer.println("=====================================");
            writer.println();
            
            int totalFiles = 0;
            long totalSize = 0;
            
            for (String stage : SEMANTIC_STAGES) {
                writer.println("阶段: " + stage);
                writer.println("-------------------");
                
                Path stagePath = Paths.get(RESULT_DIR, stage);
                
                if (!Files.exists(stagePath)) {
                    writer.println("目录不存在");
                    writer.println();
                    continue;
                }
                
                List<Path> files;
                try (Stream<Path> stream = Files.list(stagePath)) {
                    files = stream
                        .filter(p -> p.toString().endsWith(".txt"))
                        .sorted()
                        .collect(Collectors.toList());
                }
                
                writer.println("文件数量: " + files.size());
                totalFiles += files.size();
                
                long stageSize = 0;
                for (Path file : files) {
                    long size = Files.size(file);
                    stageSize += size;
                    writer.println("  " + file.getFileName() + ": " + size + " 字节");
                }
                
                writer.println("总大小: " + stageSize + " 字节");
                totalSize += stageSize;
                writer.println();
            }
            
            writer.println("=====================================");
            writer.println("总计:");
            writer.println("文件数量: " + totalFiles);
            writer.println("总大小: " + totalSize + " 字节");
            writer.println("平均文件大小: " + (totalFiles > 0 ? totalSize / totalFiles : 0) + " 字节");
        }
        
        System.out.println("测试报告已生成: " + reportFile);
    }
}
```

## 测试执行步骤

### 步骤 1: 编译所有测试程序
```bash
# 编译所有必要的 Java 文件
javac *.java

# 或者分别编译（如果需要）
# javac Main.java
# javac BatchTestRunner.java
# javac TestResultValidator.java
# javac TestReportGenerator.java
# javac DebugSingleTest.java
```

### 步骤 2: 执行批量测试
```bash
# 运行批量测试
java BatchTestRunner
```

### 步骤 3: 验证测试结果
```bash
# 验证结果
java TestResultValidator
```

### 步骤 4: 生成测试报告
```bash
# 生成报告
java TestReportGenerator
```

## 预期结果结构

执行测试后，预期将生成以下目录结构：

```
result/
├── semantic-1/
│   ├── misc6.txt
│   ├── misc7.txt
│   ├── misc8.txt
│   ├── ...
│   ├── return1.txt
│   ├── return2.txt
│   ├── ...
│   ├── type1.txt
│   ├── type2.txt
│   └── ...
└── semantic-2/
    ├── comprehensive1.txt
    ├── comprehensive2.txt
    ├── ...
    └── comprehensive50.txt
```

## 测试脚本 (run_tests.sh)

```bash
#!/bin/bash

# RCompiler 测试执行脚本

echo "开始 RCompiler 词法分析测试..."

# 检查必要文件是否存在
if [ ! -f "Main.class" ]; then
    echo "编译所有 Java 文件..."
    javac *.java
    if [ $? -ne 0 ]; then
        echo "错误: 编译 Java 文件失败"
        exit 1
    fi
fi

# 编译测试工具（如果尚未编译）
echo "确保测试工具已编译..."
javac BatchTestRunner.java TestResultValidator.java TestReportGenerator.java DebugSingleTest.java
if [ $? -ne 0 ]; then
    echo "错误: 编译测试工具失败"
    exit 1
fi

# 执行测试
echo "执行批量测试..."
java BatchTestRunner

# 验证结果
echo "验证测试结果..."
java TestResultValidator

# 生成报告
echo "生成测试报告..."
java TestReportGenerator

echo "测试完成！查看 test_report.txt 获取详细报告。"
```

## 错误处理和调试

### 1. 常见问题处理

#### 问题: 编译错误
```bash
# 检查 Java 版本
java -version
javac -version

# 清理并重新编译所有文件
rm -f *.class
javac *.java

# 如果仍有问题，尝试单独编译主程序
javac Main.java Parser.java AST.java PrintAST.java VisitorBase.java literal_t.java oper_t.java patternSeg_t.java

# 然后编译测试工具
javac BatchTestRunner.java TestResultValidator.java TestReportGenerator.java DebugSingleTest.java
```

#### 问题: 运行时错误
```bash
# 检查文件权限
ls -la RCompiler-Testcases/

# 检查目录结构
find RCompiler-Testcases/ -name "*.rx" | head -10
```

#### 问题: 空结果文件
```bash
# 检查特定测试用例
java Main < RCompiler-Testcases/semantic-1/misc6/misc6.rx

# 检查错误输出
java Main < RCompiler-Testcases/semantic-1/misc6/misc6.rx 2>error.log
cat error.log
```

### 2. 调试工具函数

#### 单个测试用例调试 (DebugSingleTest.java)
```java
import java.io.*;
import java.nio.file.*;

public class DebugSingleTest {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("用法: java DebugSingleTest <rx文件路径>");
            System.exit(1);
        }
        
        Path rxFile = Paths.get(args[0]);
        
        if (!Files.exists(rxFile)) {
            System.err.println("文件不存在: " + rxFile);
            System.exit(1);
        }
        
        try {
            System.out.println("调试文件: " + rxFile);
            System.out.println("文件大小: " + Files.size(rxFile) + " 字节");
            System.out.println("文件内容:");
            System.out.println("----------------------------------------");
            
            // 显示文件内容
            try (BufferedReader reader = Files.newBufferedReader(rxFile)) {
                String line;
                int lineNum = 1;
                while ((line = reader.readLine()) != null) {
                    System.out.printf("%3d: %s%n", lineNum++, line);
                }
            }
            
            System.out.println("----------------------------------------");
            System.out.println("词法分析结果:");
            System.out.println("----------------------------------------");
            
            // 运行词法分析
            ProcessBuilder pb = new ProcessBuilder("java", "Main");
            pb.redirectInput(rxFile.toFile());
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            
            Process process = pb.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
            
            int exitCode = process.waitFor();
            System.out.println("----------------------------------------");
            System.out.println("退出码: " + exitCode);
            
        } catch (Exception e) {
            System.err.println("调试过程中发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

## 性能优化建议

1. **并行处理**: 对于大量测试用例，可以考虑使用多线程并行处理
2. **内存管理**: 对于大型测试文件，考虑使用流式处理
3. **缓存机制**: 对重复测试用例实现结果缓存
4. **增量测试**: 只处理修改过的测试用例

## 扩展功能

1. **差异比较**: 比较不同版本的词法分析结果
2. **性能基准**: 测量词法分析器的处理速度
3. **错误分类**: 对词法分析错误进行分类统计
4. **可视化报告**: 生成图表形式的测试报告

## 总结

本测试计划提供了完整的测试框架，包括：
- 批量处理所有测试用例
- 结果验证和报告生成
- 错误处理和调试工具
- 性能优化和扩展建议

按照此计划执行，可以全面测试 RCompiler 的词法分析功能，并生成详细的测试报告。

## parseStmtNode() 实现步骤

根据grammar.md中的语句定义，parseStmtNode()方法的实现步骤如下：

### 语法规则回顾
根据grammar.md第25-28行，语句的语法规则为：
```
<statement> ::= <item> | <letstmt> | <exprstmt> | ;
```

### 实现步骤

1. **输入验证**
   - 检查是否还有token可供解析
   - 如果没有token，抛出断言错误："No more tokens to parse in statement"

2. **获取当前token**
   - 获取当前token用于判断语句类型

3. **空语句处理**
   - 如果当前token是分号(`;`)：
     - 消耗分号token
     - 返回null（表示空语句）
     - 跳过后续处理

4. **Let语句处理**
   - 如果当前token是`let`关键字：
     - 调用`parseLetStmtNode()`方法解析let语句
     - 返回解析结果

5. **函数项处理**
   - 如果当前token是`fn`关键字：
     - 调用`parseFunctionNode()`方法解析函数定义
     - 返回解析结果

6. **结构体项处理**
   - 如果当前token是`struct`关键字：
     - 调用`parseStructNode()`方法解析结构体定义
     - 返回解析结果

7. **枚举项处理**
   - 如果当前token是`enum`关键字：
     - 调用`parseEnumNode()`方法解析枚举定义
     - 返回解析结果

8. **常量项处理**
   - 如果当前token是`const`关键字：
     - 调用`parseConstItemNode()`方法解析常量定义
     - 返回解析结果

9. **Trait项处理**
   - 如果当前token是`trait`关键字：
     - 调用`parseTraitNode()`方法解析trait定义
     - 返回解析结果

10. **Impl项处理**
    - 如果当前token是`impl`关键字：
      - 调用`parseImplNode()`方法解析impl定义
      - 返回解析结果

11. **表达式语句处理（默认情况）**
    - 如果当前token不匹配任何上述关键字：
      - 调用`parseExprStmtNode()`方法解析表达式语句
      - 返回解析结果

### 代码实现示例

```java
public StmtNode parseStmtNode() {
    // 1. 输入验证
    assert i < tokens.size() : "No more tokens to parse in statement";
    
    // 2. 获取当前token
    token_t token = tokens.get(i);
    
    // 3. 空语句处理
    if (token.name.equals(";")) {
        i++; // 消耗分号
        return null; // 返回null表示空语句
    }
    
    // 4. Let语句处理
    if (token.name.equals("let")) {
        return parseLetStmtNode();
    }
    // 5. 函数项处理
    else if (token.name.equals("fn")) {
        return parseFunctionNode();
    }
    // 6. 结构体项处理
    else if (token.name.equals("struct")) {
        return parseStructNode();
    }
    // 7. 枚举项处理
    else if (token.name.equals("enum")) {
        return parseEnumNode();
    }
    // 8. 常量项处理
    else if (token.name.equals("const")) {
        return parseConstItemNode();
    }
    // 9. Trait项处理
    else if (token.name.equals("trait")) {
        return parseTraitNode();
    }
    // 10. Impl项处理
    else if (token.name.equals("impl")) {
        return parseImplNode();
    }
    // 11. 表达式语句处理（默认情况）
    else {
        return parseExprStmtNode();
    }
}
```

### 注意事项

1. **空语句处理**：根据语法规则，单独的分号是合法的语句，需要特殊处理
2. **顺序检查**：关键字检查应按照特定顺序进行，确保优先匹配
3. **错误处理**：每个分支都应正确处理错误情况，提供有意义的错误信息
4. **返回值处理**：调用方需要处理parseStmtNode()可能返回null的情况（空语句）

### 相关方法调用

- `parseLetStmtNode()`: 解析let语句
- `parseFunctionNode()`: 解析函数定义
- `parseStructNode()`: 解析结构体定义
- `parseEnumNode()`: 解析枚举定义
- `parseConstItemNode()`: 解析常量定义
- `parseTraitNode()`: 解析trait定义
- `parseImplNode()`: 解析impl定义
- `parseExprStmtNode()`: 解析表达式语句

### 语法树节点类型

根据解析结果，返回的StmtNode可能是以下类型之一：
- LetStmtNode (let语句)
- FunctionNode (函数定义)
- StructNode (结构体定义)
- EnumNode (枚举定义)
- ConstItemNode (常量定义)
- TraitNode (trait定义)
- ImplNode (impl定义)
- ExprStmtNode (表达式语句)
- null (空语句)