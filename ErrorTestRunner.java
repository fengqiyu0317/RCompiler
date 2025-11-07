import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

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

        // 输出一行"正在处理: testCaseName"
        System.out.println("正在处理: " + testCaseName + " " + testCasePath);
        
        // 构建命令
        ProcessBuilder pb = new ProcessBuilder("java", "Main");
        pb.redirectInput(new File(testCasePath));
        
        // 捕获错误流和标准输出流
        ByteArrayOutputStream errorCapture = new ByteArrayOutputStream();
        pb.redirectError(ProcessBuilder.Redirect.PIPE);
        pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
        
        Process process = pb.start();
        
        // 使用单独的线程读取流，避免阻塞
        Thread errorThread = new Thread(() -> {
            try (InputStream errorStream = process.getErrorStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = errorStream.read(buffer)) != -1) {
                    errorCapture.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                // 忽略异常
            }
        });
        
        Thread outputThread = new Thread(() -> {
            try (InputStream outputStream = process.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = outputStream.read(buffer)) != -1) {
                    // 丢弃标准输出，只读取避免阻塞
                }
            } catch (IOException e) {
                // 忽略异常
            }
        });
        
        errorThread.start();
        outputThread.start();
        
        // 添加超时控制
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            System.err.println("测试用例超时: " + testCaseName);
            errorThread.interrupt();
            outputThread.interrupt();
            return true; // 视为仍有错误
        }
        
        // 等待线程完成
        errorThread.join(1000);
        outputThread.join(1000);
        
        int exitCode = process.exitValue();
        String errorOutput = errorCapture.toString();
        
        // 如果输出过大，截断以避免内存问题
        if (errorOutput.length() > 10000) {
            errorOutput = errorOutput.substring(0, 10000) + "\n... (truncated)";
        }
        
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