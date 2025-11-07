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
}