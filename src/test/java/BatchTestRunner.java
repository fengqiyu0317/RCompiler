import java.io.*;
import java.nio.file.*;
import java.util.*;

public class BatchTestRunner {
    private static final String TESTCASES_DIR = "tests/testcases/RCompiler-Testcases";
    private static final String RESULT_DIR = "results/result";
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
    
    private static void processStage(String stage) {
        try {
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
        } catch (IOException e) {
            System.err.println("处理阶段 " + stage + " 时发生IO异常: " + e.getMessage());
            System.out.println("处理阶段 " + stage + " 时发生IO异常: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("处理阶段 " + stage + " 时发生未预期异常: " + e.getMessage());
            System.out.println("处理阶段 " + stage + " 时发生未预期异常: " + e.getMessage());
        }
    }
    
    private static void processTestCase(Path testCaseDir, String stage) {
        String testCaseName = testCaseDir.getFileName().toString();
        
        try {
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
        } catch (IOException e) {
            System.err.println("处理测试用例 " + testCaseName + " 时发生IO异常: " + e.getMessage());
            System.out.println("处理测试用例 " + testCaseName + " 时发生IO异常: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("处理测试用例 " + testCaseName + " 时发生未预期异常: " + e.getMessage());
            System.out.println("处理测试用例 " + testCaseName + " 时发生未预期异常: " + e.getMessage());
        }
    }
    
    private static void runLexicalAnalysis(Path rxFile, String testCaseName, String stage) {
        // 构建输出文件路径
        Path outputFile = Paths.get(RESULT_DIR, stage, testCaseName + ".txt");
        
        try {
            // 构建命令
            ProcessBuilder pb = new ProcessBuilder("java", "-cp", "target/classes", "Main");
            pb.redirectInput(rxFile.toFile());
            pb.redirectOutput(outputFile.toFile());
            // 不合并错误流，保持错误流独立
            pb.redirectErrorStream(false);
            
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
            
            // 设置超时时间（60秒）
            long timeout = 60; // 秒
            boolean finished = process.waitFor(timeout, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!finished) {
                // 进程超时，强制终止
                System.err.println("错误: 处理 " + rxFile + " 超时，强制终止进程");
                process.destroyForcibly();
                
                // 等待进程真正终止
                try {
                    if (!process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        System.err.println("错误: 无法终止进程 " + rxFile);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // 记录超时错误到输出文件
                try (PrintWriter writer = new PrintWriter(outputFile.toFile())) {
                    writer.println("错误: 处理超时");
                    writer.println("测试文件: " + rxFile);
                }
            } else {
                // 进程正常结束
                int exitCode = process.exitValue();
                
                // 等待错误流读取线程完成，但也要设置超时
                try {
                    errorThread.join(5000); // 最多等待5秒
                    if (errorThread.isAlive()) {
                        errorThread.interrupt();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    if (errorThread.isAlive()) {
                        errorThread.interrupt();
                    }
                }
                
                if (exitCode != 0) {
                    System.err.println("警告: 处理 " + rxFile + " 时退出码为 " + exitCode);
                    System.out.println("警告: 处理 " + rxFile + " 时退出码为 " + exitCode);
                } else {
                    System.err.println("成功处理: " + rxFile + " -> " + outputFile);
                    System.out.println("成功处理: " + rxFile + " -> " + outputFile);
                }
            }
        } catch (IOException e) {
            System.err.println("处理 " + rxFile + " 时发生IO异常: " + e.getMessage());
            System.out.println("处理 " + rxFile + " 时发生IO异常: " + e.getMessage());
            
            // 记录异常到输出文件
            try (PrintWriter writer = new PrintWriter(outputFile.toFile())) {
                writer.println("错误: IO异常");
                writer.println("测试文件: " + rxFile);
                writer.println("异常信息: " + e.getMessage());
            } catch (IOException ex) {
                System.err.println("无法写入错误信息到输出文件: " + ex.getMessage());
            }
        } catch (Exception e) {
            System.err.println("处理 " + rxFile + " 时发生未预期异常: " + e.getMessage());
            System.out.println("处理 " + rxFile + " 时发生未预期异常: " + e.getMessage());
            
            // 记录异常到输出文件
            try (PrintWriter writer = new PrintWriter(outputFile.toFile())) {
                writer.println("错误: 未预期异常");
                writer.println("测试文件: " + rxFile);
                writer.println("异常信息: " + e.getMessage());
            } catch (IOException ex) {
                System.err.println("无法写入错误信息到输出文件: " + ex.getMessage());
            }
        }
    }
}