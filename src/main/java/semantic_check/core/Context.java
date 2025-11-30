// Context enumeration for semantic analysis

public enum Context {
    TYPE_CONTEXT,
    VALUE_CONTEXT,
    FIELD_CONTEXT,
    LET_PATTERN_CONTEXT,  // Special context for let statement patterns
    PARAMETER_PATTERN_CONTEXT,  // Special context for parameter patterns
    FUNCTION_DECLARATION,  // Special context for function declarations
    TYPE_DECLARATION,  // Special context for type declarations (struct, enum, trait)
    FIELD_DECLARATION,  // Special context for field declarations
    CONST_DECLARATION  // Special context for const declarations
}