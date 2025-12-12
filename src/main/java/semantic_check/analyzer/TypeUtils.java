import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for type checking operations
 */
public class TypeUtils {
    
    /**
     * Check if a type is numeric
     */
    public static boolean isNumericType(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType.PrimitiveKind kind = ((PrimitiveType)type).getKind();
            return kind == PrimitiveType.PrimitiveKind.INT ||
                   kind == PrimitiveType.PrimitiveKind.I32 ||
                   kind == PrimitiveType.PrimitiveKind.U32 ||
                   kind == PrimitiveType.PrimitiveKind.USIZE ||
                   kind == PrimitiveType.PrimitiveKind.ISIZE;
        }
        return false;
    }
    
    /**
     * Check if type is unsigned integer
     */
    public static boolean isUnsignedIntegerType(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType.PrimitiveKind kind = ((PrimitiveType)type).getKind();
            return kind == PrimitiveType.PrimitiveKind.U32 ||
                   kind == PrimitiveType.PrimitiveKind.USIZE;
        }
        return false;
    }
    
    /**
     * Check if type is signed integer
     */
    public static boolean isSignedIntegerType(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType.PrimitiveKind kind = ((PrimitiveType)type).getKind();
            return kind == PrimitiveType.PrimitiveKind.I32 ||
                   kind == PrimitiveType.PrimitiveKind.ISIZE ||
                   kind == PrimitiveType.PrimitiveKind.INT;
        }
        return false;
    }
    
    /**
     * Check if type is integer (either signed or unsigned)
     */
    public static boolean isIntegerType(Type type) {
        return isSignedIntegerType(type) || isUnsignedIntegerType(type);
    }
    
    /**
     * Check if type is String or str type
     */
    public static boolean isStringOrStrType(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType.PrimitiveKind kind = ((PrimitiveType)type).getKind();
            return kind == PrimitiveType.PrimitiveKind.STR ||
                   kind == PrimitiveType.PrimitiveKind.STRING;
        }
        return false;
    }
    
    /**
     * Check if type is array type
     */
    public static boolean isArrayType(Type type) {
        return type instanceof ArrayType;
    }
    
    /**
     * Check if type is u32 or usize
     */
    public static boolean isU32OrUsizeType(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType.PrimitiveKind kind = ((PrimitiveType)type).getKind();
            return kind == PrimitiveType.PrimitiveKind.U32 ||
                   kind == PrimitiveType.PrimitiveKind.USIZE ||
                     kind == PrimitiveType.PrimitiveKind.INT;
        }
        return false;
    }
    
    /**
     * Find common type between two types
     */
    public static Type findCommonType(Type type1, Type type2) {
        // Check for null types
        if (type1 == null || type2 == null) {
            return null;
        }

        if (type1 instanceof UnderscoreType || type2 instanceof UnderscoreType) {
            return null; // Both are undetermined types
        }
        
        // Special case: if one type is NeverType, return the other type
        if (type1.isNever()) {
            return type2;
        }
        if (type2.isNever()) {
            return type1;
        }
        
        // If two types are the same, return directly
        if (type1.equals(type2)) {
            return type1;
        }
        
        // If both are reference types, check their inner types
        if (type1 instanceof ReferenceType && type2 instanceof ReferenceType) {
            ReferenceType ref1 = (ReferenceType) type1;
            ReferenceType ref2 = (ReferenceType) type2;
            
            // Find common type for inner types
            Type commonInnerType = findCommonType(ref1.getInnerType(), ref2.getInnerType());
            if (commonInnerType == null) {
                return null; // Inner types are incompatible
            }
            
            // For common reference type, use the most restrictive mutability
            // Reference is mutable only if both are mutable
            boolean isRefMutable = ref1.isReferenceMutable() && ref2.isReferenceMutable();
            // Value is mutable only if both are mutable
            boolean isValueMutable = ref1.isValueMutable() && ref2.isValueMutable();
            
            // Return a new reference type with common inner type and combined mutability
            return new ReferenceType(commonInnerType, isRefMutable, isValueMutable);
        }
        
        // If both are array types, check element types and sizes
        if (type1 instanceof ArrayType && type2 instanceof ArrayType) {
            ArrayType array1 = (ArrayType) type1;
            ArrayType array2 = (ArrayType) type2;
            
            // Check if element types have a common type
            Type commonElementType = findCommonType(array1.getElementType(), array2.getElementType());
            if (commonElementType == null) {
                return null; // Element types are incompatible
            }
            
            // Check if array sizes are the same
            if (array1.getSize() != array2.getSize()) {
                return null; // Array sizes are different
            }
            
            // Return a new array type with the common element type and the same size
            return new ArrayType(commonElementType, array1.getSize());
        }
        
        // If both are numeric types, check if one is int (undetermined)
        if (type1 instanceof PrimitiveType && type2 instanceof PrimitiveType) {
            PrimitiveType.PrimitiveKind kind1 = ((PrimitiveType)type1).getKind();
            PrimitiveType.PrimitiveKind kind2 = ((PrimitiveType)type2).getKind();
            
            // Check if both are numeric
            if (isNumericType(type1) && isNumericType(type2)) {
                // If one is undetermined integer (INT), return the other type
                if (kind1 == PrimitiveType.PrimitiveKind.INT) {
                    return type2;
                }
                if (kind2 == PrimitiveType.PrimitiveKind.INT) {
                    return type1;
                }
            }
        }
        
        // Other cases return null
        return null;
    }
    
    /**
     * Check if two types are compatible
     */
    public static boolean isTypeCompatible(Type actualType, Type expectedType) {
        // If expected type is UnderscoreType, any actual type is compatible
        if (expectedType instanceof UnderscoreType) {
            return true;
        }
        
        // If actual type is UnderscoreType, it cannot be compatible with any expected type
        if (actualType instanceof UnderscoreType) {
            return false;
        }
        
        // If types are exactly the same, they're compatible
        if (actualType.equals(expectedType)) {
            return true;
        }
        
        // Try to find a common type between the two types
        // If a common type exists and it matches the expected type, they're compatible
        Type commonType = findCommonType(actualType, expectedType);
        if (commonType != null && commonType.equals(expectedType)) {
            return true;
        }
        
        // Special case for never type: never can be coerced to any type
        if (actualType.isNever()) {
            return true;
        }
        
        // Otherwise, types are not compatible
        return false;
    }
    
    /**
     * Check if a cast is valid
     */
    public static boolean isValidCast(Type sourceType, Type targetType) {
        // If types are the same, cast is always valid
        if (sourceType.equals(targetType)) {
            return true;
        }
        
        // Integer types can be cast to each other
        if (isIntegerType(sourceType) && isIntegerType(targetType)) {
            return true;
        }
        
        // All other cases: only allow casts between the same type
        return false;
    }
    
    /**
     * Check if operator is arithmetic compound assignment
     */
    public static boolean isArithmeticCompoundAssignment(oper_t operator) {
        return operator == oper_t.PLUS_ASSIGN ||
               operator == oper_t.MINUS_ASSIGN ||
               operator == oper_t.MUL_ASSIGN ||
               operator == oper_t.DIV_ASSIGN ||
               operator == oper_t.MOD_ASSIGN;
    }
    
    /**
     * Check if operator is shift operation
     */
    public static boolean isShiftOperation(oper_t operator) {
        return operator == oper_t.SHL || operator == oper_t.SHR;
    }
    
    /**
     * Check if operator is shift compound assignment
     */
    public static boolean isShiftCompoundAssignment(oper_t operator) {
        return operator == oper_t.SHL_ASSIGN || operator == oper_t.SHR_ASSIGN;
    }

    public static boolean isRelationalOperator(oper_t operator) {
        return operator == oper_t.LT ||
               operator == oper_t.LTE ||
               operator == oper_t.GT ||
               operator == oper_t.GTE;
    }
    
    /**
     * 创建类型的可变版本（默认为可变）
     */
    public static Type createMutableType(Type type) {
        return createMutableType(type, true);
    }
    
    /**
     * 创建具有指定可变性的类型
     */
    public static Type createMutableType(Type type, boolean isMutable) {
        if (type instanceof PrimitiveType) {
            PrimitiveType primitiveType = (PrimitiveType) type;
            return new PrimitiveType(primitiveType.getKind(), isMutable);
        } else if (type instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) type;
            return new ArrayType(arrayType.getElementType(), arrayType.getSize(), isMutable);
        } else if (type instanceof StructType) {
            StructType structType = (StructType) type;
            return new StructType(structType.getName(), structType.getFields(), structType.getStructSymbol(), isMutable);
        } else if (type instanceof UnitType) {
            return isMutable ? UnitType.MUTABLE_INSTANCE : UnitType.INSTANCE;
        } else if (type instanceof NeverType) {
            return isMutable ? NeverType.MUTABLE_INSTANCE : NeverType.INSTANCE;
        } else if (type instanceof FunctionType) {
            FunctionType functionType = (FunctionType) type;
            return new FunctionType(functionType.getParameterTypes(), functionType.getReturnType(), functionType.isMethod(), isMutable, functionType.getSelfType());
        } else if (type instanceof EnumType) {
            EnumType enumType = (EnumType) type;
            return new EnumType(enumType.getName(), enumType.getVariants(), enumType.getEnumSymbol(), isMutable);
        } else if (type instanceof TraitType) {
            TraitType traitType = (TraitType) type;
            return new TraitType(traitType.getName(), traitType.getTraitSymbol(), isMutable);
        } else if (type instanceof StructConstructorType) {
            StructConstructorType constructorType = (StructConstructorType) type;
            return new StructConstructorType(constructorType.getStructType(), isMutable);
        } else if (type instanceof EnumConstructorType) {
            EnumConstructorType constructorType = (EnumConstructorType) type;
            return new EnumConstructorType(constructorType.getEnumType(), isMutable);
        } else if (type instanceof ReferenceType) {
            ReferenceType refType = (ReferenceType) type;
            refType.setMutability(isMutable);
            // 引用类型的可变性由其自身决定，不需要修改
            return refType;
        }
        
        // 如果类型不支持可变性，返回原类型
        return type;
    }
}