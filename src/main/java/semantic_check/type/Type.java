// Type interface for type checking system

public interface Type {
    // Check if two types are equal
    boolean equals(Type other);
    
    // Get string representation of the type
    String toString();
    
    // Get the base type (for reference types)
    Type getBaseType();
    
    // Check if type is numeric
    boolean isNumeric();
    
    // Check if type is boolean
    boolean isBoolean();
    
    // Check if type is unit type
    boolean isUnit();
    
    // Check if type is never type
    boolean isNever();
}