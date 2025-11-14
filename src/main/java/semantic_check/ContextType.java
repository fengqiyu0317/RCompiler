// Context type enumeration
public enum ContextType {
    GLOBAL,           // Global context (including inside regular functions)
    IMPL_BLOCK,       // impl block context
    TRAIT_BLOCK,      // trait block context
    METHOD,           // method context
    ASSOCIATED_FUNC   // associated function context
}