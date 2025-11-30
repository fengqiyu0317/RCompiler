// Namespace enumeration

public enum Namespace {
    TYPE("Type Namespace"),
    VALUE("Value Namespace"),
    FIELD("Field Namespace");
    
    private final String description;
    
    Namespace(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}