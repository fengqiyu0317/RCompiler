import java.util.ArrayList;
import java.util.List;

public class ErrorReporter {
    private final List<String> errors;
    private boolean throwExceptions = true;
    
    public ErrorReporter() {
        this.errors = new ArrayList<>();
    }
    
    // Configure whether to throw exceptions
    public void setThrowExceptions(boolean throwExceptions) {
        this.throwExceptions = throwExceptions;
    }
    
    public boolean isThrowExceptions() {
        return throwExceptions;
    }
    
    public void reportError(String message, token_t token) {
        if (throwExceptions) {
            throw new ParserException(message, token);
        } else {
            // Keep original logic
            if (token != null) {
                errors.add("Error at token '" + token.name + "' (position " + getPositionInfo(token) + "): " + message);
            } else {
                errors.add("Error: " + message);
            }
        }
    }
    
    public void reportError(String message) {
        if (throwExceptions) {
            throw new ParserException(message);
        } else {
            // Keep original logic
            errors.add("Error: " + message);
        }
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public void clearErrors() {
        errors.clear();
    }
    
    private String getPositionInfo(token_t token) {
        // This is a placeholder - in a real implementation, you might have
        // line and column information in the token
        return "unknown";
    }
}