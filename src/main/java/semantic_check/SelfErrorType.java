// self and Self related error types
public enum SelfErrorType {
    SELF_OUTSIDE_METHOD("self can only be used in method bodies"),
    SELF_IN_ASSOCIATED_FUNCTION("self cannot be used in associated functions"),
    SELF_NOT_FIRST_PARAMETER("self must be the first parameter of a method"),
    SELF_OUTSIDE_IMPL_OR_TRAIT("self can only be used in impl blocks or trait definitions"),
    
    SELF_TYPE_OUTSIDE_CONTEXT("Self can only be used in impl blocks or trait definitions"),
    SELF_TYPE_IN_FREE_FUNCTION("Self cannot be used in global context or free functions"),
    SELF_TYPE_WITH_PREFIX("Self can only be used as the first segment, without a preceding ::");
    
    private final String description;
    
    SelfErrorType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
}