import java.io.*;
import java.util.Vector;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.List;
import java.util.ArrayList;

public class ASTGeneratorBatch {
    
    public static void main(String[] args) {
        try {
            // Get list of all .rx files
            List<String> semantic1Files = getRxFiles("RCompiler-Testcases/semantic-1");
            List<String> semantic2Files = getRxFiles("RCompiler-Testcases/semantic-2");
            
            System.out.println("Found " + semantic1Files.size() + " semantic-1 files");
            System.out.println("Found " + semantic2Files.size() + " semantic-2 files");
            
            // Process semantic-1 files in batches
            processBatch(semantic1Files, "answer/semantic-1", 0, 10);
            
            // Process semantic-2 files in batches
            processBatch(semantic2Files, "answer/semantic-2", 0, 10);
            
            System.out.println("Batch processing completed!");
        } catch (Exception e) {
            System.err.println("Error in main: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static List<String> getRxFiles(String dir) {
        List<String> files = new ArrayList<>();
        try (Stream<java.nio.file.Path> paths = Files.walk(Paths.get(dir))) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> path.toString().endsWith(".rx"))
                 .forEach(path -> files.add(path.toString()));
        } catch (IOException e) {
            System.err.println("Error listing files in " + dir + ": " + e.getMessage());
        }
        return files;
    }
    
    private static void processBatch(List<String> files, String outputDir, int start, int count) {
        try {
            Files.createDirectories(Paths.get(outputDir));
            
            int end = Math.min(start + count, files.size());
            System.out.println("Processing files " + start + " to " + (end - 1) + " out of " + files.size());
            
            for (int i = start; i < end; i++) {
                String inputPath = files.get(i);
                String fileName = Paths.get(inputPath).getFileName().toString().replace(".rx", ".txt");
                String outputPath = outputDir + "/" + fileName;
                
                System.out.println("Processing: " + inputPath + " -> " + outputPath);
                
                // Process the file and generate AST
                generateAST(inputPath, outputPath);
            }
        } catch (Exception e) {
            System.err.println("Error processing batch: " + e.getMessage());
        }
    }
    
    private static void generateAST(String inputPath, String outputPath) {
        try {
            // Read the input file
            String content = new String(Files.readAllBytes(Paths.get(inputPath)));
            
            // Skip processing if file is too large
            if (content.length() > 50000) {
                System.err.println("Skipping large file: " + inputPath + " (size: " + content.length() + ")");
                try (PrintStream errorOut = new PrintStream(new FileOutputStream(outputPath))) {
                    errorOut.println("Error: File too large to process (size: " + content.length() + ")");
                }
                return;
            }
            
            // Create a new tokenizer for each file
            Tokenizer tokenizer = new Tokenizer();
            String[] lines = content.split("\n");
            for (String line : lines) {
                tokenizer.tokenize(line);
            }
            
            // Create a new parser for each file
            Parser parser = new Parser(tokenizer.tokens);
            parser.parse();
            
            // Redirect output to file
            PrintStream originalOut = System.out;
            try (PrintStream fileOut = new PrintStream(new FileOutputStream(outputPath))) {
                System.setOut(fileOut);
                
                // Print the AST with error handling
                PrintAST printAST = new PrintAST();
                for (StmtNode stmt : parser.statements) {
                    try {
                        printAST.visit(stmt);
                    } catch (Exception e) {
                        // Print error message to file and continue
                        System.out.println("Error printing statement: " + e.getMessage());
                    }
                }
            } finally {
                System.setOut(originalOut);
            }
            
        } catch (OutOfMemoryError e) {
            System.err.println("OutOfMemoryError for " + inputPath + ": " + e.getMessage());
            // Create an error file
            try (PrintStream errorOut = new PrintStream(new FileOutputStream(outputPath))) {
                errorOut.println("Error: OutOfMemoryError - file too large to process");
            } catch (Exception ex) {
                System.err.println("Error creating error file: " + ex.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error generating AST for " + inputPath + ": " + e.getMessage());
            // Create an error file
            try (PrintStream errorOut = new PrintStream(new FileOutputStream(outputPath))) {
                errorOut.println("Error: " + e.getMessage());
                e.printStackTrace(errorOut);
            } catch (Exception ex) {
                System.err.println("Error creating error file: " + ex.getMessage());
            }
        }
    }
}