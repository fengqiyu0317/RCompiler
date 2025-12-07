import java.util.Scanner;
import java.util.Vector;

public class Main {
    public static void main(String[] args) {
        ReadRustFile.read_init();
    }
}

class ReadRustFile {
    public static void read_init() {
        Tokenizer tokenizer = new Tokenizer();
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                tokenizer.tokenize(line);
            }
        }
        
        try {
            Parser parser = new Parser(new Vector<token_t>(tokenizer.tokens));
            parser.parse();
            
            // Check for parsing errors
            if (parser.hasErrors()) {
                System.err.println("解析错误:");
                for (String error : parser.getErrors()) {
                    System.err.println("  " + error);
                }
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
                return;
            } catch (Exception e) {
                System.err.println("Error during self checking: " + e.getMessage());
                e.printStackTrace();
                return;
            }
            
            // Perform namespace semantic checking
            try {
                NamespaceAnalyzer namespaceAnalyzer = new NamespaceAnalyzer();
                namespaceAnalyzer.initializeGlobalScope();
                
                for (ASTNode stmt : parser.getStatements()) {
                    stmt.accept(namespaceAnalyzer);
                }
                
                System.out.println("Namespace semantic checking completed successfully.");
                
            } catch (Exception e) {
                System.err.println("Error during namespace checking: " + e.getMessage());
                e.printStackTrace();
                return;
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
                } else {
                    System.out.println("Type checking completed successfully.");
                }
                
                // Check for constant evaluation errors
                if (typeChecker.hasConstantEvaluationErrors()) {
                    System.err.println("Constant evaluation errors:");
                    typeChecker.getConstantEvaluationErrorCollector().printErrors();
                } else {
                    System.out.println("Constant evaluation completed successfully.");
                }
                
            } catch (Exception e) {
                System.err.println("Error during type checking: " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (ParserException e) {
            System.err.println("Parsing error: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("System error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}