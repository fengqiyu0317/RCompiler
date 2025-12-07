import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class responsible for extracting types from symbols and type nodes
 */
public class TypeExtractor {
    private final TypeErrorCollector errorCollector;
    private final boolean throwOnError;
    private final ConstantEvaluator constantEvaluator;
    private VisitorBase expressionTypeChecker;
    private TypeChecker typeChecker;
    
    public TypeExtractor(TypeErrorCollector errorCollector, boolean throwOnError, ConstantEvaluator constantEvaluator) {
        this.errorCollector = errorCollector;
        this.throwOnError = throwOnError;
        this.constantEvaluator = constantEvaluator;
        this.expressionTypeChecker = null; // Will be set later to avoid circular dependency
    }
    
    public void setExpressionTypeChecker(VisitorBase expressionTypeChecker) {
        this.expressionTypeChecker = expressionTypeChecker;
    }
    
    public void setTypeChecker(TypeChecker typeChecker) {
        this.typeChecker = typeChecker;
    }
    
    /**
     * Extract type from symbol
     */
    public Type extractTypeFromSymbol(Symbol symbol) throws RuntimeException {
        try {
            if (symbol == null) {
                throw new RuntimeException("Cannot extract type from null symbol");
            }
            
            // Check if symbol already has cached type
            Type cachedType = symbol.getType();
            if (cachedType != null) {
                return cachedType;
            }
            
            // Extract type based on symbol kind
            ASTNode declaration = symbol.getDeclaration();
            Type resultType = null;
            
            switch (symbol.getKind()) {
                case FUNCTION:
                    resultType = extractFunctionType(symbol, declaration);
                    break;
                    
                case STRUCT_CONSTRUCTOR:
                    resultType = extractStructConstructorType(symbol, declaration);
                    break;
                    
                case ENUM_VARIANT_CONSTRUCTOR:
                    resultType = extractEnumType(symbol, declaration);
                    break;
                    
                case CONSTANT:
                    resultType = extractConstantType(symbol, declaration);
                    break;
                    
                case PARAMETER:
                    resultType = extractParameterType(symbol, declaration);
                    break;
                    
                case LOCAL_VARIABLE:
                    resultType = extractLocalVariableType(symbol, declaration);
                    break;
                    
                case STRUCT:
                    resultType = extractStructType(symbol, declaration);
                    break;
                    
                case ENUM:
                    resultType = extractEnumType(symbol, declaration);
                    break;
                    
                case TRAIT:
                    resultType = extractTraitType(symbol, declaration);
                    break;
                    
                case BUILTIN_TYPE:
                    resultType = extractBuiltinType(symbol);
                    break;
                    
                case SELF_TYPE:
                    resultType = extractSelfType(symbol);
                    break;
                    
                case SELF_CONSTRUCTOR:
                    resultType = extractSelfConstructorType(symbol);
                    break;
                    
                case FIELD:
                    resultType = extractFieldType(symbol, declaration);
                    break;
                    
                default:
                    throw new RuntimeException(
                        "Unsupported symbol kind for type extraction: " + symbol.getKind()
                    );
            }
            
            if (resultType == null) {
                throw new RuntimeException(
                    "Cannot determine type for symbol: " + symbol.getName()
                );
            }
            
            return resultType;
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Extract type from type node
     */
    public Type extractTypeFromTypeNode(TypeExprNode typeNode) throws RuntimeException {
        try {
            if (typeNode == null) {
                throw new RuntimeException("Type node is null");
            }
            
            // Handle different type expression types
            if (typeNode instanceof TypePathExprNode) {
                return extractTypeFromTypePathExpr((TypePathExprNode) typeNode);
            } else if (typeNode instanceof TypeRefExprNode) {
                return extractTypeFromTypeRefExpr((TypeRefExprNode) typeNode);
            } else if (typeNode instanceof TypeArrayExprNode) {
                return extractTypeFromTypeArrayExpr((TypeArrayExprNode) typeNode);
            } else if (typeNode instanceof TypeUnitExprNode) {
                return UnitType.INSTANCE;
            } else {
                throw new RuntimeException(
                    "Unsupported type expression: " + typeNode.getClass().getSimpleName()
                );
            }
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Helper method to set symbol type
     */
    private void setSymbolType(Symbol symbol, Type type) {
        symbol.setType(type);
    }
    
    /**
     * Extract function type
     */
    private Type extractFunctionType(Symbol symbol, ASTNode declaration) {
        if (declaration instanceof FunctionNode) {
            FunctionNode funcNode = (FunctionNode) declaration;
            List<Type> paramTypes = new ArrayList<>();
            
            // Check if it's a method (has self parameter)
            boolean isMethod = funcNode.selfPara != null;
            
            // Process parameters
            if (funcNode.parameters != null) {
                for (ParameterNode param : funcNode.parameters) {
                    Type paramType = extractTypeFromTypeNode(param.type);
                    paramTypes.add(paramType);
                }
            }
            
            // Process return type
            Type returnType = funcNode.returnType != null ?
                             extractTypeFromTypeNode(funcNode.returnType) :
                             UnitType.INSTANCE;
            
            FunctionType functionType = new FunctionType(paramTypes, returnType, isMethod);
            setSymbolType(symbol, functionType);
            return functionType;
        }
        return null;
    }
    
    /**
     * Extract struct constructor type
     */
    private Type extractStructConstructorType(Symbol symbol, ASTNode declaration) {
        if (declaration instanceof StructNode) {
            StructNode structNode = (StructNode) declaration;
            Map<String, Type> fields = new HashMap<>();
            
            if (structNode.fields != null) {
                for (FieldNode field : structNode.fields) {
                    Type fieldType = extractTypeFromTypeNode(field.type);
                    fields.put(field.name.name, fieldType);
                }
            }
            
            // Get the struct type symbol from the struct node
            Symbol structTypeSymbol = structNode.getSymbol();
            StructType structType = new StructType(symbol.getName(), fields, structTypeSymbol);
            StructConstructorType structConstructorType = new StructConstructorType(structType);
            setSymbolType(symbol, structConstructorType);
            return structConstructorType;
        }
        return null;
    }
    
    /**
     * Extract enum constructor type
     */
    private Type extractEnumConstructorType(Symbol symbol, ASTNode declaration) {
        // The declaration points to the variant node (IdentifierNode), not the enum node
        // We need to traverse up to find the parent EnumNode
        EnumNode enumNode = null;
        if (declaration instanceof IdentifierNode) {
            // The declaration is the variant identifier, we need to find the parent EnumNode
            ASTNode parent = declaration.getFather();
            while (parent != null && !(parent instanceof EnumNode)) {
                parent = parent.getFather();
            }
            if (parent instanceof EnumNode) {
                enumNode = (EnumNode) parent;
            }
        } else if (declaration instanceof EnumNode) {
            // This case might occur if the implementation changes in the future
            enumNode = (EnumNode) declaration;
        }
        
        if (enumNode != null) {
            // Create enum type with variants
            EnumType enumType = new EnumType(symbol.getName(), enumNode.getSymbol());
            // Create enum constructor type that wraps the enum type
            EnumConstructorType enumConstructorType = new EnumConstructorType(enumType);
            setSymbolType(symbol, enumConstructorType);
            return enumConstructorType;
        }
        return null;
    }
    
    /**
     * Extract constant type
     */
    private Type extractConstantType(Symbol symbol, ASTNode declaration) {
        if (declaration instanceof ConstItemNode) {
            ConstItemNode constNode = (ConstItemNode) declaration;
            if (constNode.type != null) {
                Type constType = extractTypeFromTypeNode(constNode.type);
                setSymbolType(symbol, constType);
                return constType;
            }
        }
        return null;
    }
    
    /**
     * Extract parameter type
     */
    private Type extractParameterType(Symbol symbol, ASTNode declaration) {
        // The declaration is an IdentifierNode, we need to traverse up to find the ParameterNode
        if (declaration instanceof IdentifierNode) {
            ASTNode currentNode = declaration;
            // Traverse up to find the ParameterNode
            while (currentNode != null && !(currentNode instanceof ParameterNode)) {
                currentNode = currentNode.getFather();
            }
            
            if (currentNode instanceof ParameterNode) {
                ParameterNode paramNode = (ParameterNode) currentNode;
                if (paramNode.type != null) {
                    Type paramType = extractTypeFromTypeNode(paramNode.type);
                    setSymbolType(symbol, paramType);
                    return paramType;
                }
            }
        }
        return null;
    }
    
    /**
     * Extract local variable type
     */
    private Type extractLocalVariableType(Symbol symbol, ASTNode declaration) {
        // The declaration is an IdentifierNode, we need to traverse up to find the LetStmtNode
        // If we encounter Reference Pattern Nodes, we need to wrap the type in a Reference
        if (declaration instanceof IdentifierNode) {
            ASTNode currentNode = declaration;
            Type varType = null;
            int referenceCount = 0;
            boolean isMutable = false;
            
            // Traverse up to find the LetStmtNode
            while (currentNode != null && !(currentNode instanceof LetStmtNode)) {
                // Check if we encounter a RefPatNode
                if (currentNode instanceof RefPatNode) {
                    RefPatNode refPat = (RefPatNode) currentNode;
                    referenceCount++;
                    if (refPat.isDoubleReference) {
                        referenceCount++; // Add one more for double reference
                    }
                    isMutable = refPat.isMutable;
                }
                currentNode = currentNode.getFather();
            }
            
            if (currentNode instanceof LetStmtNode) {
                LetStmtNode letNode = (LetStmtNode) currentNode;
                if (letNode.type != null) {
                    varType = extractTypeFromTypeNode(letNode.type);
                    
                    // Apply the appropriate number of references
                    for (int i = 0; i < referenceCount; i++) {
                        varType = new ReferenceType(varType, isMutable);
                    }
                    
                    setSymbolType(symbol, varType);
                    return varType;
                }
            }
        }
        return null;
    }
    
    /**
     * Extract struct type
     */
    private Type extractStructType(Symbol symbol, ASTNode declaration) {
        if (declaration instanceof StructNode) {
            StructNode structNode = (StructNode) declaration;
            Map<String, Type> fields = new HashMap<>();
            
            if (structNode.fields != null) {
                for (FieldNode field : structNode.fields) {
                    Type fieldType = extractTypeFromTypeNode(field.type);
                    fields.put(field.name.name, fieldType);
                }
            }
            
            StructType structType = new StructType(symbol.getName(), fields, symbol);
            setSymbolType(symbol, structType);
            return structType;
        }
        return null;
    }
    
    /**
     * Extract enum type
     */
    private Type extractEnumType(Symbol symbol, ASTNode declaration) {
        while (declaration instanceof IdentifierNode) {
            // The declaration is the variant identifier, we need to find the parent EnumNode
            ASTNode parent = declaration.getFather();
            if (parent instanceof EnumNode) {
                declaration = parent;
                break;
            }
            declaration = parent;
        }
        if (declaration instanceof EnumNode) {
            EnumNode enumDeclNode = (EnumNode) declaration;
            // Create enum type with variants
            EnumType enumType = new EnumType(symbol.getName(), symbol);
            
            // Add all variants to the enum type
            if (enumDeclNode.variants != null) {
                for (IdentifierNode variantNode : enumDeclNode.variants) {
                    // For now, we're adding variants without any associated types
                    // In the future, if variants can have associated data, this would need to be extended
                    enumType.addVariant(variantNode.name);
                }
            }
            
            setSymbolType(symbol, enumType);
            return enumType;
        }
        return null;
    }
    
    /**
     * Extract trait type
     */
    private Type extractTraitType(Symbol symbol, ASTNode declaration) {
        if (declaration instanceof TraitNode) {
            TraitNode traitNode = (TraitNode) declaration;
            // Create trait type
            TraitType traitType = new TraitType(symbol.getName(), symbol);
            
            // Add all methods and constants to the trait type
            if (traitNode.items != null) {
                for (AssoItemNode itemNode : traitNode.items) {
                    if (itemNode.function != null) {
                        // Extract function type for the method
                        FunctionNode funcNode = itemNode.function;
                        Symbol funcSymbol = funcNode.getSymbol();
                        
                        if (funcSymbol != null) {
                            // Extract the function type
                            Type funcType = extractFunctionType(funcSymbol, funcNode);
                            if (funcType instanceof FunctionType) {
                                // Add the method to the trait
                                traitType.addMethod(funcNode.name.name, (FunctionType) funcType);
                            }
                        }
                    } else if (itemNode.constant != null) {
                        // Extract constant type
                        ConstItemNode constNode = itemNode.constant;
                        Symbol constSymbol = constNode.getSymbol();
                        
                        if (constSymbol != null) {
                            // Extract the constant type
                            Type constType = extractConstantType(constSymbol, constNode);
                            if (constType != null) {
                                // Add the constant to the trait
                                traitType.addConstant(constNode.name.name, constType);
                            }
                        }
                    }
                }
            }
            
            setSymbolType(symbol, traitType);
            return traitType;
        }
        return null;
    }
    
    /**
     * Extract builtin type
     */
    private Type extractBuiltinType(Symbol symbol) {
        String typeName = symbol.getName();
        switch (typeName) {
            case "i32":
                return PrimitiveType.getI32Type();
            case "u32":
                return PrimitiveType.getU32Type();
            case "usize":
                return PrimitiveType.getUsizeType();
            case "isize":
                return PrimitiveType.getIsizeType();
            case "bool":
                return PrimitiveType.getBoolType();
            case "str":
                return PrimitiveType.getStrType();
            case "String":
                return PrimitiveType.getStringType();
            case "char":
                return PrimitiveType.getCharType();
            default:
                throw new RuntimeException("Unknown builtin type: " + typeName);
        }
    }
    
    /**
     * Extract Self type
     */
    private Type extractSelfType(Symbol symbol) {
        // shouldn't be called directly, Self type is context-dependent
        throw new RuntimeException("Self type extraction should be context-dependent");
    }
    
    /**
     * Extract Self constructor type
     */
    private Type extractSelfConstructorType(Symbol symbol) {
        if (typeChecker == null) {
            throw new RuntimeException("TypeChecker not set in TypeExtractor");
        }
        Type selfType = typeChecker.getCurrentSelfType();
        if (selfType == null) {
            throw new RuntimeException("No current Self type available");
        }
        // Self constructor returns the Self type
        return selfType;
    }
    
    /**
     * Extract field type
     */
    private Type extractFieldType(Symbol symbol, ASTNode declaration) {
        if (declaration instanceof FieldNode) {
            FieldNode fieldNode = (FieldNode) declaration;
            if (fieldNode.type != null) {
                Type fieldType = extractTypeFromTypeNode(fieldNode.type);
                setSymbolType(symbol, fieldType);
                return fieldType;
            }
        }
        return null;
    }
    
    /**
     * Extract type from type path expression
     */
    private Type extractTypeFromTypePathExpr(TypePathExprNode pathExpr) {
        try {
            if (pathExpr.path == null || pathExpr.path.name == null) {
                throw new RuntimeException(
                    "Type path expression is missing path or name"
                );
            }
            
            String typeName = pathExpr.path.name.name;
            
            // Handle primitive types
            switch (typeName) {
                case "i32": return PrimitiveType.getI32Type();
                case "u32": return PrimitiveType.getU32Type();
                case "usize": return PrimitiveType.getUsizeType();
                case "isize": return PrimitiveType.getIsizeType();
                case "bool": return PrimitiveType.getBoolType();
                case "str": return PrimitiveType.getStrType();
                case "String": return PrimitiveType.getStringType();
                case "char": return PrimitiveType.getCharType();
            }
            
            // For non-primitive types, we need to look up the symbol
            // This would typically be a struct, enum, or trait type
            Symbol symbol = pathExpr.path.getSymbol();
            if (symbol == null) {
                throw new RuntimeException(
                    "Unresolved type: " + typeName
                );
            }
            
            // Extract type from symbol
            return extractTypeFromSymbol(symbol);
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e);
                return null;
            }
        }
    }
    
    /**
     * Extract type from type reference expression
     */
    private Type extractTypeFromTypeRefExpr(TypeRefExprNode refExpr) {
        try {
            if (refExpr.innerType == null) {
                throw new RuntimeException(
                    "Reference type expression is missing inner type"
                );
            }
            
            // Extract the inner type
            Type innerType = extractTypeFromTypeNode(refExpr.innerType);
            
            // Create reference type
            return new ReferenceType(innerType, refExpr.isMutable);
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Extract type from type array expression
     */
    private Type extractTypeFromTypeArrayExpr(TypeArrayExprNode arrayExpr) {
        try {
            if (arrayExpr.elementType == null) {
                throw new RuntimeException(
                    "Array type expression is missing element type"
                );
            }
            
            // Extract the element type
            Type elementType = extractTypeFromTypeNode(arrayExpr.elementType);
            
            // For array size, we need to evaluate the size expression
            long arraySize = 0;
            
            if (arrayExpr.size != null) {
                // First, type-check the size expression
                if (typeChecker != null && expressionTypeChecker != null) {
                    // Use the expression type checker to properly type-check the size expression
                    arrayExpr.size.accept(expressionTypeChecker);
                    
                    // Verify that the size expression has a valid numeric type
                    Type sizeType = arrayExpr.size.getType();
                    if (sizeType == null) {
                        throw new RuntimeException(
                            "Array size expression type is null"
                        );
                    }
                    
                    // Check if the type is a valid integer type for array size
                    if (!isIntegerType(sizeType)) {
                        throw new RuntimeException(
                            "Array size must be an integer type, got: " + sizeType
                        );
                    }
                }
                
                // Now evaluate the size expression using the constant evaluator
                ConstantValue sizeValue = constantEvaluator.evaluate(arrayExpr.size);
                
                if (sizeValue == null) {
                    throw new RuntimeException(
                        "Array size expression is not a constant"
                    );
                }
                
                if (!sizeValue.isNumeric()) {
                    throw new RuntimeException(
                        "Array size must be a numeric value, got: " + sizeValue.getType()
                    );
                }
                
                arraySize = sizeValue.getAsLong();
                
                // Check if array size is negative
                if (arraySize < 0) {
                    throw new RuntimeException(
                        "Array size cannot be negative: " + arraySize
                    );
                }
            }
            
            // Create array type
            return new ArrayType(elementType, arraySize);
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
                return null;
            }
        }
    }
    
    /**
     * Check if a type is a valid integer type for array size
     */
    private boolean isIntegerType(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType primType = (PrimitiveType) type;
            switch (primType.getKind()) {
                case I32:
                case U32:
                case USIZE:
                case ISIZE:
                case INT:  // Include undetermined integer type for type inference
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }
}