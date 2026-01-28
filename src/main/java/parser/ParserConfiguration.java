import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ParserConfiguration {
    private final Set<String> keywords;
    
    public ParserConfiguration() {
        this.keywords = new HashSet<>(Arrays.asList(
            "as", "break", "const", "continue", "crate", "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in", 
            "let", "loop", "match", "mod", "move", "mut", "ref", "return", "self", "Self", "static", "struct", 
            "super", "trait", "true", "type", "unsafe", "use", "where", "while"
        ));
    }
    
    public boolean isKeyword(String tokenName) {
        return keywords.contains(tokenName);
    }
    
    public boolean isIdentifier(token_t token) {
        return token.tokentype == token_t.TokenType_t.IDENTIFIER_OR_KEYWORD && !isKeyword(token.name);
    }
    
    public boolean isOperatorToken(String tokenName) {
        return tokenName.equals("[") || tokenName.equals(".") || tokenName.equals("(") ||
               tokenName.equals("::") || tokenName.equals("as") ||
               tokenName.equals("=") || tokenName.equals("+=") || tokenName.equals("-=") ||
               tokenName.equals("*=") || tokenName.equals("/=") || tokenName.equals("%=") ||
               tokenName.equals("&=") || tokenName.equals("|=") || tokenName.equals("^=") ||
               tokenName.equals("<<=") || tokenName.equals(">>=") || tokenName.equals("==") ||
               tokenName.equals("!=") || tokenName.equals(">") || tokenName.equals("<") ||
               tokenName.equals(">=") || tokenName.equals("<=") || tokenName.equals("&&") ||
               tokenName.equals("||") || tokenName.equals("+") || tokenName.equals("-") ||
               tokenName.equals("*") || tokenName.equals("/") || tokenName.equals("%") ||
               tokenName.equals("|") || tokenName.equals("^") || tokenName.equals("{") ||
               tokenName.equals("<<") || tokenName.equals(">>") || tokenName.equals("&");
    }
    
    public boolean isArithmeticOperator(String name) {
        return name.equals("+") || name.equals("-") || name.equals("*") || 
               name.equals("/") || name.equals("%") || name.equals("&") || 
               name.equals("|") || name.equals("^") || name.equals("<<") || 
               name.equals(">>");
    }
    
    public boolean isComparisonOperator(String name) {
        return name.equals("==") || name.equals("!=") || name.equals("<") || 
               name.equals("<=") || name.equals(">") || name.equals(">=");
    }
    
    public boolean isAssignmentOperator(String name) {
        return name.equals("=");
    }
    
    public boolean isLogicalOperator(String name) {
        return name.equals("&&") || name.equals("||");
    }
    
    public boolean isCompoundAssignmentOperator(String name) {
        return name.equals("+=") || name.equals("-=") || name.equals("*=") || 
               name.equals("/=") || name.equals("%=") || name.equals("&=") || 
               name.equals("|=") || name.equals("^=") || name.equals("<<=") || 
               name.equals(">>=");
    }
}
