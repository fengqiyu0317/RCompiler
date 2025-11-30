import java.util.Stack;
import java.util.Vector;

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
        try {
            if (current() != null && current().name.equals(expectedTokenName)) {
                return consume();
            }
            throw new ParserException("Expected '" + expectedTokenName + "' but found '" +
                                      (current() != null ? current().name : "EOF") + "'");
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error consuming expected token '" + expectedTokenName + "': " + e.getMessage());
        }
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
        try {
            if (current() != null && current().name.equals(tokenName)) {
                position++;
                return true;
            }
            throw new ParserException("Expected '" + tokenName + "' but found '" +
                                      (current() != null ? current().name : "EOF") + "'");
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error matching token with exception '" + tokenName + "': " + e.getMessage());
        }
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
        try {
            if (current() != null && current().name.equals(tokenName) &&
                peek() != null && peek().name.equals(nextTokenName)) {
                position += 2;
                return true;
            }
            throw new ParserException("Expected '" + tokenName + " " + nextTokenName + "' but found '" +
                                      (current() != null ? current().name : "EOF") +
                                      (peek() != null ? " " + peek().name : "") + "'");
        } catch (ParserException e) {
            throw e;
        } catch (Exception e) {
            throw new ParserException("Error matching tokens with exception '" + tokenName + "' and '" + nextTokenName + "': " + e.getMessage());
        }
    }
    
    // Position management for backtracking
    public void pushPosition() {
        try {
            positionStack.push(position);
        } catch (Exception e) {
            throw new RuntimeException("Error pushing position: " + e.getMessage());
        }
    }
    
    public void popPosition() {
        try {
            if (!positionStack.isEmpty()) {
                position = positionStack.pop();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error popping position: " + e.getMessage());
        }
    }
    
    public void commitPosition() {
        try {
            if (!positionStack.isEmpty()) {
                positionStack.pop();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error committing position: " + e.getMessage());
        }
    }
    
    public int getPosition() {
        try {
            return position;
        } catch (Exception e) {
            throw new RuntimeException("Error getting position: " + e.getMessage());
        }
    }
    
    public void setPosition(int position) {
        try {
            this.position = position;
        } catch (Exception e) {
            throw new RuntimeException("Error setting position: " + e.getMessage());
        }
    }
    
    public token_t get(int index) {
        try {
            if (index >= 0 && index < tokens.size()) {
                return tokens.get(index);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error getting token at index " + index + ": " + e.getMessage());
        }
    }
    
    public int size() {
        try {
            return tokens.size();
        } catch (Exception e) {
            throw new RuntimeException("Error getting token stream size: " + e.getMessage());
        }
    }
    
    public boolean equals(String tokenName) {
        try {
            if (current() != null) {
                return current().name.equals(tokenName);
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error checking if current token equals '" + tokenName + "': " + e.getMessage());
        }
    }
}