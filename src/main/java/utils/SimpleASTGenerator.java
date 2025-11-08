import java.io.*;
import java.util.Vector;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;

public class SimpleASTGenerator {
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java SimpleASTGenerator <input-file>");
            return;
        }
        
        String inputFile = args[0];
        String outputFile = inputFile.replace(".rx", ".txt");
        
        // Adjust output path to be in answer folder
        if (inputFile.contains("semantic-1")) {
            outputFile = "answer/semantic-1/" + Paths.get(inputFile).getFileName().toString().replace(".rx", ".txt");
        } else if (inputFile.contains("semantic-2")) {
            outputFile = "answer/semantic-2/" + Paths.get(inputFile).getFileName().toString().replace(".rx", ".txt");
        }
        
        System.out.println("Processing: " + inputFile + " -> " + outputFile);
        
        try {
            // Create output directory if it doesn't exist
            Files.createDirectories(Paths.get(outputFile).getParent());
            
            // Read the input file
            String content = new String(Files.readAllBytes(Paths.get(inputFile)));
            
            // Skip processing if file is too large
            if (content.length() > 50000) {
                System.err.println("Skipping large file: " + inputFile + " (size: " + content.length() + ")");
                try (PrintStream errorOut = new PrintStream(new FileOutputStream(outputFile))) {
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
            try (PrintStream fileOut = new PrintStream(new FileOutputStream(outputFile))) {
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
            
            System.out.println("Successfully processed: " + inputFile);
            
        } catch (OutOfMemoryError e) {
            System.err.println("OutOfMemoryError for " + inputFile + ": " + e.getMessage());
            // Create an error file
            try (PrintStream errorOut = new PrintStream(new FileOutputStream(outputFile))) {
                errorOut.println("Error: OutOfMemoryError - file too large to process");
            } catch (Exception ex) {
                System.err.println("Error creating error file: " + ex.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error generating AST for " + inputFile + ": " + e.getMessage());
            // Create an error file
            try (PrintStream errorOut = new PrintStream(new FileOutputStream(outputFile))) {
                errorOut.println("Error: " + e.getMessage());
                e.printStackTrace(errorOut);
            } catch (Exception ex) {
                System.err.println("Error creating error file: " + ex.getMessage());
            }
        }
    }
}