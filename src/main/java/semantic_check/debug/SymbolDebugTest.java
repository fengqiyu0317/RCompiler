import java.util.Scanner;
import java.util.Vector;
import java.util.HashMap;
import java.util.Map;

/**
 * SymbolDebugTest is a test class that demonstrates how to use SymbolDebugVisitor
 * to output symbol information for each node in the AST.
 */
public class SymbolDebugTest {
    
    /**
     * Main method to run the symbol debug test
     */
    public static void main(String[] args) {
        System.out.println("=== Symbol Debug Test ===");
        
        // Check if a file path is provided as argument
        if (args.length > 0) {
            String filePath = args[0];
            System.out.println("Processing file: " + filePath);
            processFile(filePath);
            return;
        }
        
        // If no file provided, read from stdin
        System.out.println("Enter Rust code (Ctrl+D to finish):");
        
        Tokenizer tokenizer = new Tokenizer();
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                tokenizer.tokenize(line);
            }
        }
        
        processTokenizer(tokenizer, true);
    }
    
    /**
     * Method to run symbol debug on a specific test file
     */
    public static void runTestOnFile(String filePath) {
        System.out.println("=== Running Symbol Debug Test on: " + filePath + " ===");
        processFile(filePath);
    }
    
    /**
     * Process a file by reading its content and running the full analysis pipeline
     */
    private static void processFile(String filePath) {
        try {
            Tokenizer tokenizer = new Tokenizer();
            
            // Read file content
            java.nio.file.Path path = java.nio.file.Paths.get(filePath);
            String content = java.nio.file.Files.readString(path);
            
            // Tokenize the content
            tokenizer.tokenize(content);
            
            processTokenizer(tokenizer, false);
            
        } catch (ParserException e) {
            System.err.println("Parsing error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (TokenizerException e) {
            System.err.println("Tokenization error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (java.io.IOException e) {
            System.err.println("IO error reading file: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("System error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Process the tokenized content through the full analysis pipeline
     * @param tokenizer The tokenizer with tokens to process
     * @param exitOnError Whether to exit the program on errors (for stdin input)
     */
    private static void processTokenizer(Tokenizer tokenizer, boolean exitOnError) {
        try {
            Parser parser = new Parser(new Vector<token_t>(tokenizer.tokens));
            parser.parse();
            
            // Check for parsing errors
            if (parser.hasErrors()) {
                System.err.println("解析错误:");
                for (String error : parser.getErrors()) {
                    System.err.println("  " + error);
                }
                System.exit(1);
                return;
            }
            
            // Set up father relationships before semantic checking
            try {
                FatherSetterVisitor fatherSetter = new FatherSetterVisitor();
                for (ASTNode stmt : parser.getStatements()) {
                    stmt.accept(fatherSetter);
                }
                System.out.println("Father relationships set successfully.");
            } catch (Exception e) {
                System.err.println("Error during father setting: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
                return;
            }
            
            // Perform self and Self semantic checking
            try {
                SelfSemanticAnalyzer selfAnalyzer = new SelfSemanticAnalyzer();
                for (ASTNode stmt : parser.getStatements()) {
                    stmt.accept(selfAnalyzer);
                }
                System.out.println("Self and Self semantic checking completed successfully.");
            } catch (SemanticException e) {
                System.err.println("Self/Self semantic error: " + e.getMessage());
                if (e.getNode() != null) {
                    System.err.println("  at node: " + e.getNode().getClass().getSimpleName());
                }
                System.exit(1);
                return;
            } catch (Exception e) {
                System.err.println("Error during self checking: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
                return;
            }
            
            // Perform namespace semantic checking
            try {
                NamespaceAnalyzer namespaceAnalyzer = new NamespaceAnalyzer();
                namespaceAnalyzer.initializeGlobalScope();
                
                // Perform two-phase analysis using the analyze method
                namespaceAnalyzer.analyze(parser.getStatements());
                
                System.out.println("Namespace semantic checking completed successfully.");
                
                // Now use SymbolDebugVisitor to output symbol information
                System.out.println("\n=== Symbol Debug Output ===");
                SymbolDebugVisitor debugVisitor = new SymbolDebugVisitor();
                
                for (ASTNode stmt : parser.getStatements()) {
                    stmt.accept(debugVisitor);
                }
                
                // Print summary statistics
                debugVisitor.printSummary();
                
            } catch (SemanticException e) {
                System.err.println("Semantic error during namespace checking: " + e.getMessage());
                if (e.getNode() != null) {
                    System.err.println("  at node: " + e.getNode().getClass().getSimpleName());
                }
                System.exit(1);
            } catch (Exception e) {
                System.err.println("Error during namespace checking: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
            
            // Perform type checking
            try {
                TypeChecker typeChecker = new TypeChecker(false); // Don't throw on error, collect all errors
                
                for (ASTNode stmt : parser.getStatements()) {
                    stmt.accept(typeChecker);
                }
                
                // Check for type errors
                if (typeChecker.hasErrors()) {
                    System.err.println("Type checking errors:");
                    typeChecker.getErrorCollector().printErrors();
                    System.exit(1);
                } else {
                    System.out.println("Type checking completed successfully.");
                }
                
                // Check for constant evaluation errors
                if (typeChecker.constantEvaluator.hasErrors()) {
                    System.err.println("Constant evaluation errors:");
                    typeChecker.constantEvaluator.getErrorCollector().printErrors();
                    System.exit(1);
                } else {
                    System.out.println("Constant evaluation completed successfully.");
                }
                
            } catch (Exception e) {
                System.err.println("Error during type checking: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
            
        } catch (ParserException e) {
            System.err.println("Parsing error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("System error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}