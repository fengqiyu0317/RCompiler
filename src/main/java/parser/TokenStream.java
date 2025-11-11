import java.util.Vector;
import java.util.Stack;

public class TokenStream {
    private final Vector<token_t> tokens;
    private int position;
    private Stack<Integer> positionStack;
    
    public TokenStream(Vector<token_t> tokens) {
        this.tokens = tokens;
        this.position = 0;
        this.positionStack = new Stack<>();
    }
    
    public token_t current() {
        if (position >= tokens.size()) {
            return null;
        }
        return tokens.get(position);
    }
    
    public token_t peek() {
        if (position + 1 >= tokens.size()) {
            return null;
        }
        return tokens.get(position + 1);
    }
    
    public token_t consume() {
        if (position >= tokens.size()) {
            return null;
        }
        return tokens.get(position++);
    }
    
    public token_t consume(String expectedTokenName) throws ParserException {
        if (current() != null && current().name.equals(expectedTokenName)) {
            return consume();
        }
        throw new ParserException("Expected '" + expectedTokenName + "' but found '" +
                                  (current() != null ? current().name : "EOF") + "'");
    }
    
    public boolean isAtEnd() {
        return position >= tokens.size();
    }
    
    public boolean matches(String tokenName) {
        if (current() != null && current().name.equals(tokenName)) {
            position++;
            return true;
        }
        return false;
    }
    
    public boolean matchesWithException(String tokenName) throws ParserException {
        if (current() != null && current().name.equals(tokenName)) {
            position++;
            return true;
        }
        throw new ParserException("Expected '" + tokenName + "' but found '" +
                                  (current() != null ? current().name : "EOF") + "'");
    }
    
    public boolean matches(String tokenName, String nextTokenName) {
        if (current() != null && current().name.equals(tokenName) &&
            peek() != null && peek().name.equals(nextTokenName)) {
            position += 2;
            return true;
        }
        return false;
    }
    
    public boolean matchesWithException(String tokenName, String nextTokenName) throws ParserException {
        if (current() != null && current().name.equals(tokenName) &&
            peek() != null && peek().name.equals(nextTokenName)) {
            position += 2;
            return true;
        }
        throw new ParserException("Expected '" + tokenName + " " + nextTokenName + "' but found '" +
                                  (current() != null ? current().name : "EOF") +
                                  (peek() != null ? " " + peek().name : "") + "'");
    }
    
    // Position management for backtracking
    public void pushPosition() {
        positionStack.push(position);
    }
    
    public void popPosition() {
        if (!positionStack.isEmpty()) {
            position = positionStack.pop();
        }
    }
    
    public void commitPosition() {
        if (!positionStack.isEmpty()) {
            positionStack.pop();
        }
    }
    
    public int getPosition() {
        return position;
    }
    
    public void setPosition(int position) {
        this.position = position;
    }
    
    public token_t get(int index) {
        if (index >= 0 && index < tokens.size()) {
            return tokens.get(index);
        }
        return null;
    }
    
    public int size() {
        return tokens.size();
    }
    
    public boolean equals(String tokenName) {
        if (current() != null) {
            return current().name.equals(tokenName);
        }
        return false;
    }
}