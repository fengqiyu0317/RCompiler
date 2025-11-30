# Semantic Check 重构执行计划

## 概述

本文档提供了将semantic_check目录中的16个文件重新组织为6个子目录的详细执行计划。

## 执行步骤

### 阶段1：准备工作

#### 1.1 创建备份
```bash
# 创建完整备份
cp -r src/main/java/semantic_check src/main/java/semantic_check_backup
```

#### 1.2 创建新目录结构
```bash
mkdir -p src/main/java/semantic_check/core
mkdir -p src/main/java/semantic_check/symbol
mkdir -p src/main/java/semantic_check/error
mkdir -p src/main/java/semantic_check/self
mkdir -p src/main/java/semantic_check/analyzer
mkdir -p src/main/java/semantic_check/context
```

### 阶段2：移动核心基础组件 (core/)

#### 2.1 移动Context.java
```bash
mv src/main/java/semantic_check/Context.java src/main/java/semantic_check/core/
```

**需要更新的package声明：**
```java
// 原始
// package semantic_check;

// 更新为
package semantic_check.core;
```

#### 2.2 移动ContextType.java
```bash
mv src/main/java/semantic_check/ContextType.java src/main/java/semantic_check/core/
```

**需要更新的package声明：**
```java
package semantic_check.core;
```

#### 2.3 移动Namespace.java
```bash
mv src/main/java/semantic_check/Namespace.java src/main/java/semantic_check/core/
```

**需要更新的package声明：**
```java
package semantic_check.core;
```

#### 2.4 移动Type.java
```bash
mv src/main/java/semantic_check/Type.java src/main/java/semantic_check/core/
```

**需要更新的package声明：**
```java
package semantic_check.core;
```

### 阶段3：移动符号表管理 (symbol/)

#### 3.1 移动Symbol.java
```bash
mv src/main/java/semantic_check/Symbol.java src/main/java/semantic_check/symbol/
```

**需要更新的package声明：**
```java
package semantic_check.symbol;
```

**需要更新的import语句：**
```java
import semantic_check.core.Namespace;
import semantic_check.core.Type;
```

#### 3.2 移动SymbolKind.java
```bash
mv src/main/java/semantic_check/SymbolKind.java src/main/java/semantic_check/symbol/
```

**需要更新的package声明：**
```java
package semantic_check.symbol;
```

**需要更新的import语句：**
```java
import semantic_check.core.Namespace;
```

#### 3.3 移动NamespaceSymbolTable.java
```bash
mv src/main/java/semantic_check/NamespaceSymbolTable.java src/main/java/semantic_check/symbol/
```

**需要更新的package声明：**
```java
package semantic_check.symbol;
```

**需要更新的import语句：**
```java
import semantic_check.core.Namespace;
import semantic_check.error.SemanticException;
```

#### 3.4 移动NamespaceScope.java
```bash
mv src/main/java/semantic_check/NamespaceScope.java src/main/java/semantic_check/symbol/
```

**需要更新的package声明：**
```java
package semantic_check.symbol;
```

**需要更新的import语句：**
```java
import semantic_check.core.ContextType;
```

### 阶段4：移动错误处理 (error/)

#### 4.1 移动ErrorType.java
```bash
mv src/main/java/semantic_check/ErrorType.java src/main/java/semantic_check/error/
```

**需要更新的package声明：**
```java
package semantic_check.error;
```

#### 4.2 移动SemanticError.java
```bash
mv src/main/java/semantic_check/SemanticError.java src/main/java/semantic_check/error/
```

**需要更新的package声明：**
```java
package semantic_check.error;
```

#### 4.3 移动SemanticException.java
```bash
mv src/main/java/semantic_check/SemanticException.java src/main/java/semantic_check/error/
```

**需要更新的package声明：**
```java
package semantic_check.error;
```

**需要更新的import语句：**
```java
import semantic_check.self.SelfErrorType;
```

### 阶段5：移动Self关键字处理 (self/)

#### 5.1 移动SelfChecker.java
```bash
mv src/main/java/semantic_check/SelfChecker.java src/main/java/semantic_check/self/
```

**需要更新的package声明：**
```java
package semantic_check.self;
```

**需要更新的import语句：**
```java
import semantic_check.core.ContextType;
import semantic_check.context.ContextInfo;
import semantic_check.error.SemanticException;
import semantic_check.self.SelfErrorType;
```

#### 5.2 移动SelfErrorType.java
```bash
mv src/main/java/semantic_check/SelfErrorType.java src/main/java/semantic_check/self/
```

**需要更新的package声明：**
```java
package semantic_check.self;
```

#### 5.3 移动SelfSemanticAnalyzer.java
```bash
mv src/main/java/semantic_check/SelfSemanticAnalyzer.java src/main/java/semantic_check/self/
```

**需要更新的package声明：**
```java
package semantic_check.self;
```

**需要更新的import语句：**
```java
import semantic_check.core.ContextType;
import semantic_check.context.ContextInfo;
import semantic_check.error.SemanticException;
```

### 阶段6：移动上下文信息 (context/)

#### 6.1 移动ContextInfo.java
```bash
mv src/main/java/semantic_check/ContextInfo.java src/main/java/semantic_check/context/
```

**需要更新的package声明：**
```java
package semantic_check.context;
```

**需要更新的import语句：**
```java
import semantic_check.core.ContextType;
```

### 阶段7：移动分析器实现 (analyzer/)

#### 7.1 移动NamespaceAnalyzer.java
```bash
mv src/main/java/semantic_check/NamespaceAnalyzer.java src/main/java/semantic_check/analyzer/
```

**需要更新的package声明：**
```java
package semantic_check.analyzer;
```

**需要更新的import语句：**
```java
import semantic_check.core.Context;
import semantic_check.core.ContextType;
import semantic_check.core.Type;
import semantic_check.error.ErrorType;
// 注意：SemanticError 不再被 NamespaceAnalyzer 使用
import semantic_check.symbol.NamespaceSymbolTable;
import semantic_check.symbol.NamespaceScope;
import semantic_check.symbol.Symbol;
import semantic_check.symbol.SymbolKind;
```

#### 7.2 移动SemanticAnalyzerVisits.java
```bash
mv src/main/java/semantic_check/SemanticAnalyzerVisits.java src/main/java/semantic_check/analyzer/
```

**需要更新的package声明：**
```java
package semantic_check.analyzer;
```

**需要更新的import语句：**
```java
import semantic_check.core.Context;
import semantic_check.core.ContextType;
import semantic_check.error.ErrorType;
// 注意：SemanticError 不再被 SemanticAnalyzerVisits 使用
import semantic_check.symbol.NamespaceSymbolTable;
import semantic_check.symbol.NamespaceScope;
import semantic_check.symbol.Symbol;
import semantic_check.symbol.SymbolKind;
```

### 阶段8：更新外部引用

#### 8.1 查找所有引用semantic_check包的文件
```bash
find src -name "*.java" -exec grep -l "import semantic_check\." {} \;
```

#### 8.2 更新import语句
根据文件的新位置，更新所有import语句。例如：

```java
// 原始
import semantic_check.Context;
import semantic_check.Symbol;
import semantic_check.SemanticAnalyzer;

// 更新后
import semantic_check.core.Context;
import semantic_check.symbol.Symbol;
import semantic_check.analyzer.NamespaceAnalyzer;
```

### 阶段9：验证和测试

#### 9.1 编译检查
```bash
# 清理并重新编译
make clean && make
```

#### 9.2 运行测试
```bash
# 运行所有相关测试
make test
```

#### 9.3 功能验证
```bash
# 运行语义检查测试用例
java -cp build/classes src.main.java.Main tests/inputs/test.rs
```

### 阶段10：清理工作

#### 10.1 删除空目录
```bash
# 确保原目录为空后删除
rmdir src/main/java/semantic_check_backup
```

#### 10.2 更新文档
- 更新README.md中的目录结构说明
- 更新API文档
- 更新开发者指南

## 常见问题和解决方案

### 问题1：循环依赖
**症状**：编译时出现循环依赖错误
**解决方案**：
1. 检查依赖关系图，确保没有循环
2. 如果存在循环，考虑重新设计类关系
3. 使用接口解耦循环依赖

### 问题2：import语句错误
**症状**：编译时找不到类
**解决方案**：
1. 检查package声明是否正确
2. 检查import语句是否指向正确的路径
3. 确保类名拼写正确

### 问题3：访问权限问题
**症状**：运行时出现非法访问错误
**解决方案**：
1. 检查类的访问修饰符
2. 考虑将某些类或方法设为public
3. 重新设计包的访问边界

## 回滚计划

如果重构过程中出现严重问题，可以使用以下命令回滚：

```bash
# 删除新结构
rm -rf src/main/java/semantic_check

# 恢复备份
mv src/main/java/semantic_check_backup src/main/java/semantic_check
```

## 时间估算

| 阶段 | 预估时间 | 说明 |
|------|----------|------|
| 准备工作 | 0.5小时 | 创建备份和目录结构 |
| 移动文件 | 2小时 | 移动16个文件并更新package声明 |
| 更新引用 | 3小时 | 更新所有import语句 |
| 验证测试 | 2小时 | 编译、测试和功能验证 |
| 清理工作 | 0.5小时 | 删除备份和更新文档 |
| **总计** | **8小时** | |

## 注意事项

1. **备份重要性**：在开始之前务必创建完整备份
2. **渐进式重构**：建议按阶段进行，每个阶段完成后进行测试
3. **团队协作**：如果是团队项目，确保团队成员了解重构计划
4. **文档同步**：及时更新相关文档，保持代码和文档的一致性

## 更新历史

### 2025-11-15: 移除错误处理功能
- 从`NamespaceAnalyzer`类中完全移除了错误处理相关功能
- 移除了`private final List<SemanticError> errors`字段
- 移除了所有错误相关的方法：`addError()`, `hasErrors()`, `getErrors()`, `printErrors()`, `generateErrorReport()`, `addTypeError()`, `addValueError()`, `addFieldError()`
- 移除了构造函数中的错误列表初始化
- 将所有错误处理调用替换为注释，保留了错误检测逻辑
- 这次重构简化了代码结构，减少了内存使用，提高了执行效率

### 2025-11-15: 类重构
- 将`SemanticAnalyzer`重命名为`NamespaceAnalyzer`，以更好地反映其主要职责
- 删除了`NamespaceSemanticAnalyzer`类，该类只是继承`SemanticAnalyzer`而没有添加任何功能
- 更新了`Main.java`以直接使用新的`NamespaceAnalyzer`类

这次重构简化了类层次结构，通过消除不必要的继承层使代码更易于维护。