import java.io.*;
import java.nio.file.*;
import java.util.*;

public class TestRunner {
    private static final String TESTCASES_DIR = "RCompiler-Testcases/semantic-1";
    private static final String A_RS_PATH = "a.rs";
    private static final String RESULT_TXT_PATH = "result.txt";
    
    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        runner.runAllTests();
    }
    
    public void runAllTests() {
        File testcasesDir = new File(TESTCASES_DIR);
        if (!testcasesDir.exists() || !testcasesDir.isDirectory()) {
            System.err.println("Testcases directory not found: " + TESTCASES_DIR);
            return;
        }
        
        File[] testDirs = testcasesDir.listFiles(File::isDirectory);
        if (testDirs == null) {
            System.err.println("No test directories found in " + TESTCASES_DIR);
            return;
        }
        
        Arrays.sort(testDirs);
        
        int totalTests = 0;
        int passedTests = 0;
        int failedTests = 0;
        
        for (File testDir : testDirs) {
            totalTests++;
            String testName = testDir.getName();
            System.out.println("Running test: " + testName);
            
            try {
                if (runSingleTest(testDir)) {
                    passedTests++;
                    System.out.println("✓ " + testName + " PASSED");
                } else {
                    failedTests++;
                    System.out.println("✗ " + testName + " FAILED");
                }
            } catch (Exception e) {
                failedTests++;
                System.out.println("✗ " + testName + " ERROR: " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.println("----------------------------------------");
        }
        
        System.out.println("\nTest Summary:");
        System.out.println("Total tests: " + totalTests);
        System.out.println("Passed: " + passedTests);
        System.out.println("Failed: " + failedTests);
        System.out.println("Success rate: " + String.format("%.2f%%", (double)passedTests / totalTests * 100));
    }
    
    private boolean runSingleTest(File testDir) throws IOException, InterruptedException {
        // Find the .rx file in the test directory
        File[] rxFiles = testDir.listFiles((dir, name) -> name.endsWith(".rx"));
        if (rxFiles == null || rxFiles.length == 0) {
            System.err.println("No .rx file found in " + testDir.getName());
            return false;
        }
        
        File rxFile = rxFiles[0]; // Take the first .rx file
        
        // Copy the .rx file to a.rs
        Files.copy(rxFile.toPath(), Paths.get(A_RS_PATH), StandardCopyOption.REPLACE_EXISTING);
        
        // Run the parser
        ProcessBuilder pb = new ProcessBuilder("java", "Main");
        pb.redirectErrorStream(true);
        pb.redirectOutput(new File(RESULT_TXT_PATH));
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            System.err.println("Parser failed with exit code: " + exitCode);
            return false;
        }
        
        // Check if result.txt was created and is not empty
        File resultFile = new File(RESULT_TXT_PATH);
        if (!resultFile.exists() || resultFile.length() == 0) {
            System.err.println("Parser produced no output");
            return false;
        }
        
        // For now, we'll consider a test successful if the parser runs without crashing
        // Later we can add more sophisticated checking
        return true;
    }
}