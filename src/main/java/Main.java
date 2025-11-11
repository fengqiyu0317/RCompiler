import java.util.Scanner;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

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
                // System.out.println(line);
                tokenizer.tokenize(line);
            }
        }
        // output the tokens
        for (token_t token : tokenizer.tokens) {
            System.out.println("Token Type: " + token.tokentype + ", Name: " + token.name);
        }
        // parse the tokens
        try {
            Parser parser = new Parser(new Vector<token_t>(tokenizer.tokens));
            
            // 可以选择使用异常模式或传统模式
            // parser.setThrowExceptions(true); // 启用异常模式
            
            parser.parse(); // 或 parser.parseLegacy()
            
            // 如果使用传统模式，检查是否有错误
            if (parser.hasErrors()) {
                System.err.println("解析错误:");
                for (String error : parser.getErrors()) {
                    System.err.println("  " + error);
                }
            }
            
            // print the AST
            PrintAST printAST = new PrintAST();
            for (StmtNode stmt : parser.getStatements()) {
                printAST.visit(stmt);
            }
        } catch (ParserException e) {
            // Output parsing error to standard error
            System.err.println(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // Catch other possible exceptions
            System.err.println("System error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}