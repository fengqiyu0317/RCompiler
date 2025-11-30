import java.util.ArrayList;
import java.util.List;

// Type error collector class

public class TypeErrorCollector {
    private final List<RuntimeException> errors = new ArrayList<>();
    
    public void addError(RuntimeException error) {
        errors.add(error);
    }
    
    public void addError(String message) {
        errors.add(new RuntimeException(message));
    }
    
    public List<RuntimeException> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public void throwFirstError() throws RuntimeException {
        if (!errors.isEmpty()) {
            throw errors.get(0);
        }
    }
    
    public void throwAllErrors() throws RuntimeException {
        if (!errors.isEmpty()) {
            // Throw the first error
            throw errors.get(0);
        }
    }
    
    public void clearErrors() {
        errors.clear();
    }
    
    public int getErrorCount() {
        return errors.size();
    }
    
    public void printErrors() {
        for (RuntimeException error : errors) {
            System.err.println("  " + error.getMessage());
        }
    }
}