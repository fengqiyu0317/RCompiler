// AmbiguousBlockType implementation for blocks that can be either a value type or unit type

public class AmbiguousBlockType implements Type {
    private final Type valueType;
    public static final AmbiguousBlockType INSTANCE = new AmbiguousBlockType(UnitType.INSTANCE);
    
    private AmbiguousBlockType(Type valueType) {
        this.valueType = valueType;
    }
    
    // Factory method to create an instance with a specific value type
    public static AmbiguousBlockType create(Type valueType) {
        return new AmbiguousBlockType(valueType);
    }
    
    // Get the potential value type
    public Type getValueType() {
        return valueType;
    }
    
    // Check if this ambiguous type can be resolved to a specific type
    public boolean canResolveTo(Type type) {
        return valueType.equals(type) || UnitType.INSTANCE.equals(type);
    }
    
    // Resolve the ambiguous type to a specific type based on context
    // If the preferred type is compatible with the value type, use the value type
    // Otherwise, use the unit type
    public Type resolveTo(Type preferredType) {
        if (preferredType != null && valueType.equals(preferredType)) {
            return valueType;
        }
        if (preferredType != null && UnitType.INSTANCE.equals(preferredType)) {
            return UnitType.INSTANCE;
        }
        // If no clear preference, default to value type
        return valueType;
    }
    
    // Merge this ambiguous type with another type
    // Returns a new AmbiguousBlockType if the merge is possible
    // Returns null if the types are incompatible
    public AmbiguousBlockType mergeWith(Type otherType) {
        if (otherType instanceof AmbiguousBlockType) {
            AmbiguousBlockType otherAmbiguous = (AmbiguousBlockType) otherType;
            // Try to merge the value types
            Type mergedValueType = mergeValueTypes(valueType, otherAmbiguous.valueType);
            if (mergedValueType != null) {
                return AmbiguousBlockType.create(mergedValueType);
            }
            return null;
        }
        
        // If the other type is compatible with our value type, keep the value type
        if (valueType.equals(otherType)) {
            return this;
        }
        
        // If the other type is unit type, we can still be ambiguous
        if (UnitType.INSTANCE.equals(otherType)) {
            return this;
        }
        
        // Types are incompatible
        return null;
    }
    
    // Helper method to merge two value types
    private Type mergeValueTypes(Type type1, Type type2) {
        if (type1.equals(type2)) {
            return type1;
        }
        
        // Handle primitive type merging
        if (type1 instanceof PrimitiveType && type2 instanceof PrimitiveType) {
            PrimitiveType prim1 = (PrimitiveType) type1;
            PrimitiveType prim2 = (PrimitiveType) type2;
            
            // If one is INT (undetermined), use the other
            if (prim1.getKind() == PrimitiveType.PrimitiveKind.INT) {
                return type2;
            }
            if (prim2.getKind() == PrimitiveType.PrimitiveKind.INT) {
                return type1;
            }
        }
        
        // Handle reference type merging
        if (type1 instanceof ReferenceType && type2 instanceof ReferenceType) {
            ReferenceType ref1 = (ReferenceType) type1;
            ReferenceType ref2 = (ReferenceType) type2;
            
            // Check if mutability is the same
            if (ref1.isMutable() == ref2.isMutable()) {
                Type mergedInner = mergeValueTypes(ref1.getInnerType(), ref2.getInnerType());
                if (mergedInner != null) {
                    return new ReferenceType(mergedInner, ref1.isMutable());
                }
            }
        }
        
        // Handle array type merging
        if (type1 instanceof ArrayType && type2 instanceof ArrayType) {
            ArrayType arr1 = (ArrayType) type1;
            ArrayType arr2 = (ArrayType) type2;
            
            if (arr1.getSize() == arr2.getSize()) {
                Type mergedElement = mergeValueTypes(arr1.getElementType(), arr2.getElementType());
                if (mergedElement != null) {
                    return new ArrayType(mergedElement, arr1.getSize());
                }
            }
        }
        
        // Types cannot be merged
        return null;
    }
    
    @Override
    public boolean equals(Type other) {
        if (other instanceof AmbiguousBlockType) {
            AmbiguousBlockType otherAmbiguous = (AmbiguousBlockType) other;
            return valueType.equals(otherAmbiguous.valueType);
        }
        // An ambiguous block type is equal to either its value type or unit type
        return valueType.equals(other) || UnitType.INSTANCE.equals(other);
    }
    
    @Override
    public String toString() {
        return "AmbiguousBlock(" + valueType.toString() + " | ())";
    }
    
    @Override
    public Type getBaseType() {
        return valueType.getBaseType();
    }
    
    @Override
    public boolean isNumeric() {
        return valueType.isNumeric();
    }
    
    @Override
    public boolean isBoolean() {
        return valueType.isBoolean();
    }
    
    @Override
    public boolean isUnit() {
        // An ambiguous block can be treated as unit type
        return true;
    }
    
    @Override
    public boolean isNever() {
        return valueType.isNever();
    }
}