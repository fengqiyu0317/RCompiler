import java.util.Scanner;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
// import tokenizer.Tokenizer;
// import tokenizer.token_t;
// import parser.Parser;
// import parser.ParseException;
// import utils.PrintAST;
// import ast.StmtNode;

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
        // for (token_t token : tokenizer.tokens) {
        //     System.out.println("Token Type: " + token.tokentype + ", Name: " + token.name);
        // }
        // parse the tokens
        try {
            Parser parser = new Parser(new Vector<token_t>(tokenizer.tokens));
            parser.parse();
            // print the AST
            PrintAST printAST = new PrintAST();
            for (StmtNode stmt : parser.statements) {
                printAST.visit(stmt);
            }
        } catch (ParseException e) {
            // Output parsing error to standard error
            System.err.println("Parse error: " + e.getMessage());
        } catch (Exception e) {
            // Catch other possible exceptions
            System.err.println("System error: " + e.getMessage());
        }
    }
}

