// Error type enumeration

public enum ErrorType {
    // Type namespace errors
    UNDECLARED_TYPE_IDENTIFIER("Undeclared type identifier"),
    DUPLICATE_TYPE_DECLARATION("Duplicate type declaration"),
    
    // Value namespace errors
    UNDECLARED_VALUE_IDENTIFIER("Undeclared value identifier"),
    DUPLICATE_VALUE_DECLARATION("Duplicate value declaration"),
    
    // Field errors
    UNDECLARED_FIELD("Undeclared field"),
    DUPLICATE_FIELD_DECLARATION("Duplicate field declaration"),
    
    // Namespace violations
    NAMESPACE_VIOLATION("Namespace violation"),
    
    // Associated item errors
    UNDECLARED_ASSOCIATED_ITEM("Undeclared associated item"),
    INVALID_ASSOCIATED_ACCESS("Invalid associated access"),
    
    
    // Other errors
    GENERAL_SEMANTIC_ERROR("General semantic error");
    
    private final String description;
    
    ErrorType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}