import java.io.*;
import java.util.Vector;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class ASTGenerator {
    
    public static void main(String[] args) {
        try {
            // Process semantic-1 files
            processDirectory("RCompiler-Testcases/semantic-1", "answer/semantic-1");
            
            // Process semantic-2 files
            processDirectory("RCompiler-Testcases/semantic-2", "answer/semantic-2");
            
            System.out.println("AST generation completed!");
        } catch (Exception e) {
            System.err.println("Error in main: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void processDirectory(String inputDir, String outputDir) {
        try {
            Files.createDirectories(Paths.get(outputDir));
            
            // Find all .rx files in the input directory
            try (Stream<java.nio.file.Path> paths = Files.walk(Paths.get(inputDir))) {
                paths.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".rx"))
                     .forEach(rxFile -> {
                         try {
                             String inputPath = rxFile.toString();
                             String fileName = rxFile.getFileName().toString().replace(".rx", ".txt");
                             String outputPath = outputDir + "/" + fileName;
                             
                             System.out.println("Processing: " + inputPath + " -> " + outputPath);
                             
                             // Process the file and generate AST
                             generateAST(inputPath, outputPath);
                         } catch (Exception e) {
                             System.err.println("Error processing file " + rxFile + ": " + e.getMessage());
                         }
                     });
            }
        } catch (IOException e) {
            System.err.println("Error processing directory " + inputDir + ": " + e.getMessage());
        }
    }
    
    private static void generateAST(String inputPath, String outputPath) {
        try {
            // Read the input file
            String content = new String(Files.readAllBytes(Paths.get(inputPath)));
            
            // Skip processing if file is too large
            if (content.length() > 100000) {
                System.err.println("Skipping large file: " + inputPath + " (size: " + content.length() + ")");
                return;
            }
            
            // Create a tokenizer and parse the content
            Tokenizer tokenizer = new Tokenizer();
            String[] lines = content.split("\n");
            for (String line : lines) {
                tokenizer.tokenize(line);
            }
            
            // Parse the tokens
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