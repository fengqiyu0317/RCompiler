// Symbol kind enumeration

public enum SymbolKind {
    // Type namespace
    STRUCT("struct"),
    ENUM("enum"),
    TRAIT("trait"),
    BUILTIN_TYPE("builtin_type"),
    SELF_TYPE("self_type"),
    
    // Value namespace
    FUNCTION("function"),
    CONSTANT("constant"),
    STRUCT_CONSTRUCTOR("struct_constructor"),
    ENUM_VARIANT_CONSTRUCTOR("enum_variant_constructor"),
    SELF_CONSTRUCTOR("self_constructor"),
    PARAMETER("parameter"),
    LOCAL_VARIABLE("local_variable"),
    
    // Fields (no namespace)
    FIELD("field");
    
    private final String description;
    
    SymbolKind(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    // Determine which namespace the symbol kind belongs to
    public Namespace getNamespace() {
        switch (this) {
            case STRUCT:
            case ENUM:
            case TRAIT:
            case BUILTIN_TYPE:
            case SELF_TYPE:
                return Namespace.TYPE;
                
            case FUNCTION:
            case CONSTANT:
            case STRUCT_CONSTRUCTOR:
            case ENUM_VARIANT_CONSTRUCTOR:
            case SELF_CONSTRUCTOR:
            case PARAMETER:
            case LOCAL_VARIABLE:
                return Namespace.VALUE;
                
            case FIELD:
                return Namespace.FIELD;
                
            default:
                throw new IllegalArgumentException("Unknown symbol kind: " + this);
        }
    }
}