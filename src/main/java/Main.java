import java.util.Scanner;
import java.util.Vector;

import codegen.ir.IRModule;

public class Main {
    // 设置为 true 输出调试信息，false 只输出 IR
    public static final boolean IS_DEBUG = false;

    public static void main(String[] args) {
        int exitCode = ReadRustFile.read_init();
        System.exit(exitCode);
    }
}

class ReadRustFile {
    public static int read_init() {
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
                return 1;
            }

            // Set up father relationships before semantic checking
            try {
                FatherSetterVisitor fatherSetter = new FatherSetterVisitor();
                for (ASTNode stmt : parser.getStatements()) {
                    stmt.accept(fatherSetter);
                }
                if (Main.IS_DEBUG) System.out.println("Father relationships set successfully.");
            } catch (Exception e) {
                System.err.println("Error during father setting: " + e.getMessage());
                if (Main.IS_DEBUG) e.printStackTrace();
                return 1;
            }

            // Perform self and Self semantic checking
            try {
                SelfSemanticAnalyzer selfAnalyzer = new SelfSemanticAnalyzer();
                for (ASTNode stmt : parser.getStatements()) {
                    stmt.accept(selfAnalyzer);
                }
                if (Main.IS_DEBUG) System.out.println("Self and Self semantic checking completed successfully.");
            } catch (SemanticException e) {
                System.err.println("Self/Self semantic error: " + e.getMessage());
                if (e.getNode() != null) {
                    System.err.println("  at node: " + e.getNode().getClass().getSimpleName());
                }
                return 1;
            } catch (Exception e) {
                System.err.println("Error during self checking: " + e.getMessage());
                if (Main.IS_DEBUG) e.printStackTrace();
                return 1;
            }

            // Perform namespace semantic checking
            try {
                NamespaceAnalyzer namespaceAnalyzer = new NamespaceAnalyzer();
                namespaceAnalyzer.initializeGlobalScope();

                java.util.Vector<StmtNode> stmts = new java.util.Vector<>();
                for (ASTNode stmt : parser.getStatements()) {
                    stmts.add((StmtNode) stmt);
                }
                namespaceAnalyzer.analyze(stmts);

                if (Main.IS_DEBUG) System.out.println("Namespace semantic checking completed successfully.");

            } catch (Exception e) {
                System.err.println("Error during namespace checking: " + e.getMessage());
                if (Main.IS_DEBUG) e.printStackTrace();
                return 1;
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
                    return 1;
                } else {
                    if (Main.IS_DEBUG) System.out.println("Type checking completed successfully.");
                }

                // Check for constant evaluation errors
                if (typeChecker.hasConstantEvaluationErrors()) {
                    System.err.println("Constant evaluation errors:");
                    typeChecker.getConstantEvaluationErrorCollector().printErrors();
                    return 1;
                } else {
                    if (Main.IS_DEBUG) System.out.println("Constant evaluation completed successfully.");
                }

            } catch (Exception e) {
                System.err.println("Error during type checking: " + e.getMessage());
                if (Main.IS_DEBUG) e.printStackTrace();
                return 1;
            }

            // Generate IR code
            try {
                IRGenerator irGenerator = new IRGenerator();
                IRModule module = irGenerator.generate(parser.getStatements());

                if (Main.IS_DEBUG) System.out.println("\n========== Generated IR ==========");
                IRPrinter printer = new IRPrinter();
                printer.print(module);
                if (Main.IS_DEBUG) System.out.println("===================================");

            } catch (Exception e) {
                System.err.println("Error during IR generation: " + e.getMessage());
                if (Main.IS_DEBUG) e.printStackTrace();
                return 1;
            }

            return 0;

        } catch (ParserException e) {
            System.err.println("Parsing error: " + e.getMessage());
            if (Main.IS_DEBUG) e.printStackTrace();
            return 1;
        } catch (Exception e) {
            System.err.println("System error: " + e.getMessage());
            if (Main.IS_DEBUG) e.printStackTrace();
            return 1;
        }
    }
}
