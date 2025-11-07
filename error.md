# 错误处理方案

## 第一部分：Main.java 中的错误处理（已实现）

### 当前状态分析

1. **Main.java** 中的 `ReadRustFile.read_init()` 方法：
   - 创建 Tokenizer 并对输入进行词法分析
   - 创建 Parser 并对 tokens 进行语法分析
   - 使用 PrintAST 打印 AST
   - 已添加异常处理机制

2. **Parser.java** 中的解析方法：
   - 使用 `ParseException` 抛出解析错误
   - 错误信息通过异常抛出，现在会被 Main.java 中的 try-catch 块捕获

3. **ParseException.java**：
   - 继承自 RuntimeException
   - 提供了两个构造函数，支持带原因的异常

### 已实现的修改

在 `ReadRustFile.read_init()` 方法中添加了 try-catch 块，捕获并处理解析过程中可能出现的异常：

```java
public static void read_init() {
    Tokenizer tokenizer = new Tokenizer();
    try (Scanner scanner = new Scanner(System.in)) {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            tokenizer.tokenize(line);
        }
    }
    
    try {
        // parse the tokens
        Parser parser = new Parser(tokenizer.tokens);
        parser.parse();
        
        // print the AST
        PrintAST printAST = new PrintAST();
        for (StmtNode stmt : parser.statements) {
            printAST.visit(stmt);
        }
    } catch (ParseException e) {
        // 输出解析错误到标准错误
        System.err.println("解析错误: " + e.getMessage());
    } catch (Exception e) {
        // 捕获其他可能的异常
        System.err.println("系统错误: " + e.getMessage());
        e.printStackTrace();
    }
}
```

## 第二部分：BatchTestRunner 中的错误流重定向方案

### 当前状态分析

1. **BatchTestRunner.java** 中的 `runLexicalAnalysis` 方法：
   - 使用 ProcessBuilder 创建并运行 Main 程序
   - 已设置 `pb.redirectErrorStream(true)` 将错误流合并到输出流
   - 输出被重定向到文件，但错误信息也会被写入文件而不是标准输出

2. 问题：
   - 错误信息被重定向到文件，无法在控制台实时查看
   - 需要同时将错误信息输出到标准输出和文件

### 解决方案

#### 方案一：修改 ProcessBuilder 配置（推荐）

修改 `runLexicalAnalysis` 方法，不合并错误流，而是分别处理标准输出和错误流：

```java
private static void runLexicalAnalysis(Path rxFile, String testCaseName, String stage) throws IOException {
    // 构建输出文件路径
    Path outputFile = Paths.get(RESULT_DIR, stage, testCaseName + ".txt");
    
    // 构建命令
    ProcessBuilder pb = new ProcessBuilder("java", "Main");
    pb.redirectInput(rxFile.toFile());
    pb.redirectOutput(outputFile.toFile());
    // 不合并错误流，保持错误流独立
    pb.redirectErrorStream(false);
    
    try {
        Process process = pb.start();
        
        // 创建线程来读取错误流并输出到标准输出
        Thread errorThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[错误] " + rxFile.getFileName() + ": " + line);
                }
            } catch (IOException e) {
                System.err.println("读取错误流时发生异常: " + e.getMessage());
            }
        });
        errorThread.start();
        
        int exitCode = process.waitFor();
        errorThread.join(); // 等待错误流读取线程完成
        
        if (exitCode != 0) {
            System.err.println("警告: 处理 " + rxFile + " 时退出码为 " + exitCode);
            System.out.println("警告: 处理 " + rxFile + " 时退出码为 " + exitCode);
        } else {
            System.err.println("成功处理: " + rxFile + " -> " + outputFile);
            System.out.println("成功处理: " + rxFile + " -> " + outputFile);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("处理 " + rxFile + " 时被中断", e);
    }
}
```

#### 方案二：使用临时文件和复制

如果需要将错误信息同时保存到文件和输出到控制台，可以使用临时文件方法：

```java
private static void runLexicalAnalysis(Path rxFile, String testCaseName, String stage) throws IOException {
    // 构建输出文件路径
    Path outputFile = Paths.get(RESULT_DIR, stage, testCaseName + ".txt");
    Path errorFile = Paths.get(RESULT_DIR, stage, testCaseName + "_error.txt");
    
    // 构建命令
    ProcessBuilder pb = new ProcessBuilder("java", "Main");
    pb.redirectInput(rxFile.toFile());
    pb.redirectOutput(outputFile.toFile());
    pb.redirectError(errorFile.toFile());
    
    try {
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        // 读取错误文件并输出到标准输出
        if (Files.exists(errorFile)) {
            List<String> errorLines = Files.readAllLines(errorFile);
            for (String line : errorLines) {
                System.out.println("[错误] " + rxFile.getFileName() + ": " + line);
            }
        }
        
        if (exitCode != 0) {
            System.err.println("警告: 处理 " + rxFile + " 时退出码为 " + exitCode);
            System.out.println("警告: 处理 " + rxFile + " 时退出码为 " + exitCode);
        } else {
            System.err.println("成功处理: " + rxFile + " -> " + outputFile);
            System.out.println("成功处理: " + rxFile + " -> " + outputFile);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("处理 " + rxFile + " 时被中断", e);
    }
}
```

#### 方案三：使用自定义输出流

创建一个自定义的 OutputStream，同时写入文件和控制台：

```java
private static class TeeOutputStream extends OutputStream {
    private final OutputStream fileOut;
    private final OutputStream consoleOut;
    
    public TeeOutputStream(OutputStream fileOut, OutputStream consoleOut) {
        this.fileOut = fileOut;
        this.consoleOut = consoleOut;
    }
    
    @Override
    public void write(int b) throws IOException {
        fileOut.write(b);
        consoleOut.write(b);
    }
    
    @Override
    public void write(byte[] b) throws IOException {
        fileOut.write(b);
        consoleOut.write(b);
    }
    
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        fileOut.write(b, off, len);
        consoleOut.write(b, off, len);
    }
    
    @Override
    public void flush() throws IOException {
        fileOut.flush();
        consoleOut.flush();
    }
    
    @Override
    public void close() throws IOException {
        try {
            fileOut.close();
        } finally {
            consoleOut.close();
        }
    }
}

private static void runLexicalAnalysis(Path rxFile, String testCaseName, String stage) throws IOException {
    // 构建输出文件路径
    Path outputFile = Paths.get(RESULT_DIR, stage, testCaseName + ".txt");
    
    // 构建命令
    ProcessBuilder pb = new ProcessBuilder("java", "Main");
    pb.redirectInput(rxFile.toFile());
    
    try {
        Process process = pb.start();
        
        // 创建自定义输出流，同时写入文件和控制台
        try (OutputStream fileOut = Files.newOutputStream(outputFile);
             TeeOutputStream teeOut = new TeeOutputStream(fileOut, System.out)) {
            
            // 将进程的输出流复制到自定义输出流
            try (InputStream processOut = process.getInputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = processOut.read(buffer)) != -1) {
                    teeOut.write(buffer, 0, bytesRead);
                }
            }
            
            // 读取错误流并输出到标准输出
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    System.out.println("[错误] " + rxFile.getFileName() + ": " + line);
                }
            }
        }
        
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            System.err.println("警告: 处理 " + rxFile + " 时退出码为 " + exitCode);
            System.out.println("警告: 处理 " + rxFile + " 时退出码为 " + exitCode);
        } else {
            System.err.println("成功处理: " + rxFile + " -> " + outputFile);
            System.out.println("成功处理: " + rxFile + " -> " + outputFile);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("处理 " + rxFile + " 时被中断", e);
    }
}
```

### 推荐实施方案

**方案一**是最简单且有效的解决方案，它：
1. 保持标准输出重定向到文件
2. 单独处理错误流，实时输出到控制台
3. 不需要额外的文件操作
4. 实现简单，性能良好

### 实施步骤

1. 修改 `runLexicalAnalysis` 方法，将 `pb.redirectErrorStream(true)` 改为 `pb.redirectErrorStream(false)`
2. 添加错误流读取线程，将错误信息输出到标准输出
3. 确保在进程结束后等待错误流读取线程完成
4. 测试修改后的代码，确保错误信息能够正确显示在控制台上

### 预期效果

实施此方案后，当运行 BatchTestRunner 时：

1. Main 程序的标准输出将继续保存到文件
2. Main 程序的错误信息将实时显示在控制台上
3. 错误信息会包含文件名前缀，便于识别是哪个测试用例的错误
4. 测试过程更加透明，便于调试和监控

## 第三部分：重新测试result.out中错误数据的实现方案

### 方案概述

本方案旨在创建一个统一的错误测试程序，用于重新测试result.out中记录的所有导致错误的测试用例。该程序将错误测试用例提取、测试运行和结果输出功能合并到一个类中，简化了代码结构和使用流程。

**核心要求**：
- 不需要输出详细的测试信息
- 仅对每个错误测试用例进行重新测试
- 如果仍然存在错误，按照原本的格式输出错误信息到result.out中

### 实现方案

#### 统一错误测试程序 (ErrorTestRunner)

**功能**：从result.out文件中提取错误测试用例，运行测试并将仍然存在的错误按原格式输出到result.out

**实现思路**：
```java
import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;

public class ErrorTestRunner {
    private static final String TESTCASES_DIR = "RCompiler-Testcases";
    
    // 错误测试用例数据结构
    public static class ErrorTestCase {
        String fileName;
        String errorType;
        String errorMessage;
        
        public ErrorTestCase(String fileName, String errorType, String errorMessage) {
            this.fileName = fileName;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
        }
    }
    
    /**
     * 从result.out文件中提取所有失败的测试用例信息
     */
    public static List<ErrorTestCase> extractErrorTestCases(String resultFilePath) throws IOException {
        List<ErrorTestCase> errorCases = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\[错误\\] (.+?): (.+?): (.+)");
        
        try (BufferedReader reader = new BufferedReader(new FileReader(resultFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String fileName = matcher.group(1);
                    String errorType = matcher.group(2);
                    String errorMessage = matcher.group(3);
                    errorCases.add(new ErrorTestCase(fileName, errorType, errorMessage));
                }
            }
        }
        
        return errorCases;
    }
    
    /**
     * 运行所有失败的测试用例并收集仍然存在的错误
     */
    public static List<ErrorTestCase> runErrorTests(List<ErrorTestCase> errorCases)
            throws IOException, InterruptedException {
        
        List<ErrorTestCase> persistentErrors = new ArrayList<>();
        
        for (ErrorTestCase errorCase : errorCases) {
            String testCaseName = errorCase.fileName.replace(".rx", "");
            String testCasePath = findTestCasePath(testCaseName);
            
            if (testCasePath != null) {
                boolean stillHasError = runSingleTest(testCaseName, testCasePath);
                
                if (stillHasError) {
                    // 如果仍然有错误，保留到结果列表中
                    persistentErrors.add(errorCase);
                }
            }
        }
        
        return persistentErrors;
    }
    
    /**
     * 在RCompiler-Testcases目录中递归查找测试用例
     */
    private static String findTestCasePath(String testCaseName) {
        try {
            return Files.walk(Paths.get(TESTCASES_DIR))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(testCaseName + ".rx"))
                .map(Path::toString)
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 运行单个测试用例，检查是否仍然有错误
     */
    private static boolean runSingleTest(String testCaseName, String testCasePath)
            throws IOException, InterruptedException {
        
        // 构建命令
        ProcessBuilder pb = new ProcessBuilder("java", "Main");
        pb.redirectInput(new File(testCasePath));
        
        // 捕获错误流
        ByteArrayOutputStream errorCapture = new ByteArrayOutputStream();
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        
        Process process = pb.start();
        
        // 读取错误流
        try (InputStream errorStream = process.getErrorStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            
            while ((bytesRead = errorStream.read(buffer)) != -1) {
                errorCapture.write(buffer, 0, bytesRead);
            }
        }
        
        int exitCode = process.waitFor();
        String errorOutput = errorCapture.toString();
        
        // 判断是否仍然有错误（有错误输出或退出码非0）
        return !errorOutput.isEmpty() || exitCode != 0;
    }
    
    /**
     * 将仍然存在的错误按原格式输出到result.out
     */
    public static void writeErrorsToResult(List<ErrorTestCase> persistentErrors, String resultFilePath)
            throws IOException {
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(resultFilePath))) {
            for (ErrorTestCase error : persistentErrors) {
                // 按照原格式输出错误信息
                writer.printf("[错误] %s: %s: %s%n",
                    error.fileName, error.errorType, error.errorMessage);
            }
        }
    }
    
    /**
     * 主程序入口，整合所有功能
     */
    public static void main(String[] args) {
        try {
            String resultFilePath = args.length > 0 ? args[0] : "result.out";
            
            // 提取错误测试用例
            List<ErrorTestCase> errorCases = extractErrorTestCases(resultFilePath);
            
            if (errorCases.isEmpty()) {
                // 如果没有错误测试用例，创建空的result.out
                new File(resultFilePath).createNewFile();
                return;
            }
            
            // 运行错误测试
            List<ErrorTestCase> persistentErrors = runErrorTests(errorCases);
            
            // 将仍然存在的错误写入result.out
            writeErrorsToResult(persistentErrors, resultFilePath);
            
        } catch (Exception e) {
            System.err.println("错误测试过程中发生异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### 使用流程

1. **编译程序**：
   ```bash
   javac ErrorTestRunner.java
   ```

2. **运行测试**：
   ```bash
   java ErrorTestRunner
   ```
   
   或者指定自定义的输入文件：
   ```bash
   java ErrorTestRunner custom_result.out
   ```

3. **查看结果**：
   - 程序会自动更新result.out文件，只包含仍然存在的错误
   - 如果所有错误都已修复，result.out将为空文件

### 扩展功能

1. **错误过滤**：可以添加过滤功能，只测试特定类型的错误
   - 实现方式：在main方法中添加命令行参数，支持按错误类型过滤
   
2. **批量处理**：支持处理多个result.out文件
   - 实现方式：修改main方法，接受多个文件路径参数
   
3. **日志记录**：添加简单的日志记录功能，记录测试过程
   - 实现方式：添加日志输出到单独的文件，不影响result.out格式
   
4. **持续集成**：集成到CI/CD流程中，自动运行错误测试
   - 实现方式：通过命令行参数支持，便于集成到自动化脚本中

### 预期效果

通过这个简化的错误测试程序，可以：
1. 快速验证已知错误的修复情况
2. 自动更新result.out，只包含仍然存在的错误
3. 简化部署和使用流程，只需一个Java文件即可完成所有功能
4. 保持与现有系统的兼容性，不改变result.out的格式

这个方案专注于核心功能，不生成额外的报告文件，只更新result.out文件，确保修复的错误不会再次出现。简化的设计使得程序更易于维护和集成到现有工作流中。