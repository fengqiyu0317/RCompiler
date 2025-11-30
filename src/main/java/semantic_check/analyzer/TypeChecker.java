import java.util.Stack;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

// Type checker class for expression type checking
public class TypeChecker extends VisitorBase {
    private final TypeErrorCollector errorCollector;
    private final boolean throwOnError;
    private Type currentType; // Represents Self type in methods
    
    // Control flow context stack
    private Stack<ControlFlowContext> contextStack = new Stack<>();
    
    // Cache for containsReturnStatement results to avoid repeated computation
    private Map<ASTNode, Boolean> containsReturnCache = new HashMap<>();
    
    // Constant evaluator for evaluating constant expressions
    public final ConstantEvaluator constantEvaluator;
    
    // Control flow context type
    private enum ControlFlowContextType {
        LOOP,      // loop loop
        WHILE,     // while loop
        FUNCTION,   // function
    }
    
    // Control flow context class
    private static class ControlFlowContext {
        private final ControlFlowContextType type;
        private final ASTNode node;
        private List<Type> breakTypes;
        private List<Type> returnTypes; // Track return types for better control flow analysis
        
        public ControlFlowContext(ControlFlowContextType type, ASTNode node) {
            this.type = type;
            this.node = node;
            this.breakTypes = new ArrayList<>();
            this.returnTypes = new ArrayList<>(); // Initialize return types list
        }
        
        public ControlFlowContextType getType() {
            return type;
        }
        
        public ASTNode getNode() {
            return node;
        }
        
        public List<Type> getBreakTypes() {
            return breakTypes;
        }
        
        public List<Type> getReturnTypes() {
            return returnTypes;
        }
    }
    
    public TypeChecker(boolean throwOnError) {
        this.errorCollector = new TypeErrorCollector();
        this.throwOnError = throwOnError;
        this.currentType = null;
        this.constantEvaluator = new ConstantEvaluator(throwOnError);
    }
    
    // Get expression node's type with null check and error reporting
    private Type getType(ExprNode expr) {
        if (expr == null) {
            throw new RuntimeException(
                "Cannot get type from null expression at " + getCurrentContext()
            );
        }
        Type type = expr.getType();
        if (type == null) {
            throw new RuntimeException(
                "Expression type is null for " + getNodeDescription(expr) + " at " + getCurrentContext()
            );
        }
        return type;
    }
    
    // Set expression node's type
    private void setType(ExprNode expr, Type type) {
        expr.setType(type);
    }
    
    // Get error collector
    public TypeErrorCollector getErrorCollector() {
        return errorCollector;
    }
    
    // Check if has errors
    public boolean hasErrors() {
        return errorCollector.hasErrors();
    }
    
    // Throw first error if any
    public void throwFirstError() throws RuntimeException {
        errorCollector.throwFirstError();
    }
    
    // Check if expression is assignable (left value)
    private boolean isAssignable(ExprNode expr) {
        // In Rust, these four types of expressions can be left values:
        // 1. PathExprNode - path expressions (variables, fields, etc.)
        // 2. FieldExprNode - field access expressions
        // 3. IndexExprNode - index access expressions
        // 4. DerefExprNode - dereference expressions
        return expr instanceof PathExprNode ||
               expr instanceof FieldExprNode ||
               expr instanceof IndexExprNode ||
               expr instanceof DerefExprNode;
    }
    
    // Get current Self type
    private Type getCurrentSelfType() {
        return currentType;
    }
    
    // Set current Self type
    private void setCurrentType(Type type) {
        this.currentType = type;
    }
    
    // Clear current Self type
    private void clearCurrentType() {
        this.currentType = null;
    }
    
    // Enter loop context
    private void enterLoopContext(LoopExprNode node) {
        contextStack.push(new ControlFlowContext(ControlFlowContextType.LOOP, node));
    }
    
    // Enter while context
    private void enterWhileContext(LoopExprNode node) {
        contextStack.push(new ControlFlowContext(ControlFlowContextType.WHILE, node));
    }
    
    // Exit loop context
    private void exitLoopContext() {
        if (!contextStack.isEmpty()) {
            ControlFlowContextType type = contextStack.peek().getType();
            if (type == ControlFlowContextType.LOOP ||
                type == ControlFlowContextType.WHILE) {
                contextStack.pop();
            }
        }
    }
    
    // Enter function context
    private void enterFunctionContext(FunctionNode node) {
        contextStack.push(new ControlFlowContext(ControlFlowContextType.FUNCTION, node));
    }
    
    // Exit function context
    private void exitFunctionContext() {
        if (!contextStack.isEmpty() &&
            contextStack.peek().getType() == ControlFlowContextType.FUNCTION) {
            contextStack.pop();
        }
    }
    
    // Find nearest loop context
    private ControlFlowContext findNearestLoopContext() {
        for (int i = contextStack.size() - 1; i >= 0; i--) {
            ControlFlowContext context = contextStack.get(i);
            if (context.getType() == ControlFlowContextType.LOOP ||
                context.getType() == ControlFlowContextType.WHILE) {
                return context;
            }
        }
        return null;
    }
    
    // Find nearest function context
    private ControlFlowContext findNearestFunctionContext() {
        for (int i = contextStack.size() - 1; i >= 0; i--) {
            ControlFlowContext context = contextStack.get(i);
            if (context.getType() == ControlFlowContextType.FUNCTION) {
                return context;
            }
        }
        return null;
    }
    
    // Helper method to get node description for error reporting
    private String getNodeDescription(ASTNode node) {
        if (node == null) {
            return "null node";
        }
        
        String className = node.getClass().getSimpleName();
        
        // Try to get more specific information based on node type
        if (node instanceof PathExprNode) {
            PathExprNode pathExpr = (PathExprNode) node;
            String name = pathExpr.LSeg != null && pathExpr.LSeg.name != null ?
                         pathExpr.LSeg.name.name : "unknown";
            return "PathExprNode('" + name + "')";
        } else if (node instanceof LiteralExprNode) {
            LiteralExprNode literal = (LiteralExprNode) node;
            String valueStr = "";
            switch (literal.literalType) {
                case STRING:
                case CSTRING:
                case CHAR:
                    valueStr = literal.value_string;
                    break;
                case I32:
                case U32:
                case USIZE:
                case ISIZE:
                case INT:
                    valueStr = String.valueOf(literal.value_long);
                    break;
                case BOOL:
                    valueStr = String.valueOf(literal.value_bool);
                    break;
                default:
                    valueStr = "unknown";
                    break;
            }
            return "LiteralExprNode(" + valueStr + ")";
        } else if (node instanceof ArithExprNode) {
            ArithExprNode arith = (ArithExprNode) node;
            return "ArithExprNode(" + arith.operator + ")";
        } else if (node instanceof CompExprNode) {
            CompExprNode comp = (CompExprNode) node;
            return "CompExprNode(" + comp.operator + ")";
        } else if (node instanceof CallExprNode) {
            CallExprNode call = (CallExprNode) node;
            String funcName = call.function instanceof PathExprNode ?
                           ((PathExprNode)call.function).LSeg.name.name : "unknown";
            return "CallExprNode('" + funcName + "')";
        } else if (node instanceof IdentifierNode) {
            IdentifierNode id = (IdentifierNode) node;
            return "IdentifierNode('" + id.name + "')";
        }
        
        return className;
    }
    
    // Helper method to get current context for error reporting
    private String getCurrentContext() {
        StringBuilder context = new StringBuilder();
        
        // Add function context if available
        if (!contextStack.isEmpty()) {
            ControlFlowContext topContext = contextStack.peek();
            if (topContext.getType() == ControlFlowContextType.FUNCTION) {
                FunctionNode funcNode = (FunctionNode) topContext.getNode();
                if (funcNode.name != null) {
                    context.append("in function '").append(funcNode.name.name).append("' ");
                }
            }
        }
        
        // Add current Self type if available
        if (currentType != null) {
            context.append("(Self: ").append(currentType).append(") ");
        }
        
        return context.toString();
    }
    
    // Find common type between two types with null checks and detailed error reporting
    private Type findCommonType(Type type1, Type type2) {
        // Check for null types with detailed error information
        if (type1 == null) {
            throw new RuntimeException(
                "Cannot find common type because first type is null at " + getCurrentContext()
            );
        }
        if (type2 == null) {
            throw new RuntimeException(
                "Cannot find common type because second type is null at " + getCurrentContext()
            );
        }
        
        // Handle AmbiguousBlockType cases
        if (type1 instanceof AmbiguousBlockType) {
            return findCommonTypeWithAmbiguous((AmbiguousBlockType) type1, type2);
        }
        if (type2 instanceof AmbiguousBlockType) {
            return findCommonTypeWithAmbiguous((AmbiguousBlockType) type2, type1);
        }
        
        // If two types are the same, return directly
        if (type1.equals(type2)) {
            return type1;
        }
        
        // If both are reference types, check their inner types
        if (type1 instanceof ReferenceType && type2 instanceof ReferenceType) {
            ReferenceType ref1 = (ReferenceType) type1;
            ReferenceType ref2 = (ReferenceType) type2;
            
            // Check if mutability is the same
            // if (ref1.isMutable() != ref2.isMutable()) {
            //     return null; // Different mutability levels
            // }
            
            // Find common type for inner types
            Type commonInnerType = findCommonType(ref1.getInnerType(), ref2.getInnerType());
            if (commonInnerType == null) {
                return null; // Inner types are incompatible
            }
            
            // Return a new reference type with common inner type and same mutability
            return new ReferenceType(commonInnerType, ref1.isMutable());
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
    
    // Helper method to find common type when one type is AmbiguousBlockType
    private Type findCommonTypeWithAmbiguous(AmbiguousBlockType ambiguousType, Type otherType) {
        Type valueType = ambiguousType.getValueType();
        
        // Try to find common type with the value type first
        Type commonWithValueType = findCommonType(valueType, otherType);
        if (commonWithValueType != null) {
            return commonWithValueType;
        }
        
        // Try to find common type with unit type
        Type commonWithUnitType = findCommonType(UnitType.INSTANCE, otherType);
        if (commonWithUnitType != null) {
            return commonWithUnitType;
        }
        
        // If the other type is also ambiguous, try to merge them
        if (otherType instanceof AmbiguousBlockType) {
            AmbiguousBlockType otherAmbiguous = (AmbiguousBlockType) otherType;
            AmbiguousBlockType merged = ambiguousType.mergeWith(otherAmbiguous);
            if (merged != null) {
                return merged;
            }
        }
        
        // No common type found
        return null;
    }
    
    // Resolve an ambiguous type based on context
    private Type resolveAmbiguousType(AmbiguousBlockType ambiguousType, Type expectedType) {
        if (expectedType != null) {
            return ambiguousType.resolveTo(expectedType);
        }
        
        // If no expected type, default to the value type
        return ambiguousType.getValueType();
    }
    
    // Disambiguate type if it's an AmbiguousBlockType
    private Type disambiguateType(Type type, Type expectedType) {
        if (type instanceof AmbiguousBlockType) {
            return resolveAmbiguousType((AmbiguousBlockType) type, expectedType);
        }
        return type;
    }
    
    // Check if a type needs disambiguation
    private boolean needsDisambiguation(Type type) {
        return type instanceof AmbiguousBlockType;
    }
    
    // Check if type is numeric
    private boolean isNumericType(Type type) {
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
    
    // Extract type from symbol
    private Type extractTypeFromSymbol(Symbol symbol) throws RuntimeException {
        try {
            if (symbol == null) {
                throw new RuntimeException(
                    "Cannot extract type from null symbol"
                );
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
                    // Function type: extract from function declaration
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
                        resultType = functionType;
                    }
                    break;
                    
                case STRUCT_CONSTRUCTOR:
                    // Struct constructor returns StructConstructorType
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
                        StructConstructorType structConstructorType = new StructConstructorType(structType);
                        setSymbolType(symbol, structConstructorType);
                        resultType = structConstructorType;
                    }
                    break;
                    
                case ENUM_VARIANT_CONSTRUCTOR:
                    // Enum variant constructor returns EnumConstructorType
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
                        resultType = enumConstructorType;
                    }
                    break;
                    
                case CONSTANT:
                    // Constant type: extract from constant declaration
                    if (declaration instanceof ConstItemNode) {
                        ConstItemNode constNode = (ConstItemNode) declaration;
                        if (constNode.type != null) {
                            Type constType = extractTypeFromTypeNode(constNode.type);
                            setSymbolType(symbol, constType);
                            resultType = constType;
                        }
                    }
                    break;
                    
                case PARAMETER:
                    // Parameter type: extract from parameter declaration
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
                                resultType = paramType;
                            }
                        }
                    }
                    break;
                    
                case LOCAL_VARIABLE:
                    // Local variable type: extract from let statement
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
                                resultType = varType;
                            }
                        }
                    }
                    break;
                    
                case STRUCT:
                    // Struct type: extract from struct declaration
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
                        resultType = structType;
                    }
                    break;
                    
                case ENUM:
                    // Enum type: extract from enum declaration
                    if (declaration instanceof EnumNode) {
                        EnumNode enumDeclNode = (EnumNode) declaration;
                        // Create enum type with variants
                        EnumType enumType = new EnumType(symbol.getName(), symbol);
                        setSymbolType(symbol, enumType);
                        resultType = enumType;
                    }
                    break;
                    
                case TRAIT:
                    // Trait type: extract from trait declaration
                    if (declaration instanceof TraitNode) {
                        TraitNode traitNode = (TraitNode) declaration;
                        // Create trait type
                        TraitType traitType = new TraitType(symbol.getName(), symbol);
                        setSymbolType(symbol, traitType);
                        resultType = traitType;
                    }
                    break;
                    
                case BUILTIN_TYPE:
                    // Builtin type: handle based on type name
                    String typeName = symbol.getName();
                    switch (typeName) {
                        case "i32":
                            resultType = PrimitiveType.getI32Type();
                            break;
                        case "u32":
                            resultType = PrimitiveType.getU32Type();
                            break;
                        case "usize":
                            resultType = PrimitiveType.getUsizeType();
                            break;
                        case "isize":
                            resultType = PrimitiveType.getIsizeType();
                            break;
                        case "bool":
                            resultType = PrimitiveType.getBoolType();
                            break;
                        case "str":
                            resultType = PrimitiveType.getStrType();
                            break;
                        case "char":
                            resultType = PrimitiveType.getCharType();
                            break;
                        default:
                            throw new RuntimeException("Unknown builtin type: " + typeName);
                    }
                    setSymbolType(symbol, resultType);
                    break;
                    
                case SELF_TYPE:
                    // Self type: use current Self type from context
                    resultType = getCurrentSelfType();
                    if (resultType == null) {
                        throw new RuntimeException("Self type used outside of method context");
                    }
                    setSymbolType(symbol, resultType);
                    break;
                    
                case SELF_CONSTRUCTOR:
                    // Self constructor: returns Self type
                    resultType = getCurrentSelfType();
                    if (resultType == null) {
                        throw new RuntimeException("Self constructor used outside of method context");
                    }
                    setSymbolType(symbol, resultType);
                    break;
                    
                case FIELD:
                    // Field type: extract from field declaration
                    if (declaration instanceof FieldNode) {
                        FieldNode fieldNode = (FieldNode) declaration;
                        if (fieldNode.type != null) {
                            Type fieldType = extractTypeFromTypeNode(fieldNode.type);
                            setSymbolType(symbol, fieldType);
                            resultType = fieldType;
                        }
                    }
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
    
    // Helper method to set symbol type
    private void setSymbolType(Symbol symbol, Type type) {
        symbol.setType(type);
    }
    
    // Extract type from type node
    private Type extractTypeFromTypeNode(TypeExprNode typeNode) throws RuntimeException {
        try {
            if (typeNode == null) {
                throw new RuntimeException(
                    "Type node is null"
                );
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
    
    // Extract type from type path expression
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
            
            // Ensure that symbol is set back to the path
            pathExpr.path.setSymbol(symbol);
            
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
    
    // Extract type from type reference expression
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
    
    // Extract type from type array expression
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
                arrayExpr.size.accept(this);
                Type sizeType = getType(arrayExpr.size);
                
                // Check if the size expression is of numeric type
                if (!isNumericType(sizeType)) {
                    throw new RuntimeException(
                        "Array size must be a numeric type, got: " + sizeType
                    );
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
    
    // Visit methods for simple expressions
    
    // Visit literal expression
    public void visit(LiteralExprNode node) {
        Type literalType;
        
        switch (node.literalType) {
            case I32:
                literalType = PrimitiveType.getI32Type();
                break;
            case U32:
                literalType = PrimitiveType.getU32Type();
                break;
            case USIZE:
                literalType = PrimitiveType.getUsizeType();
                break;
            case ISIZE:
                literalType = PrimitiveType.getIsizeType();
                break;
            case INT:
                literalType = PrimitiveType.getIntType();
                break;
            case BOOL:
                literalType = PrimitiveType.getBoolType();
                break;
            case CHAR:
                literalType = PrimitiveType.getCharType();
                break;
            case STRING:
                literalType = PrimitiveType.getStrType();
                break;
            case CSTRING:
                literalType = PrimitiveType.getStrType(); // Treat as string for now
                break;
            default:
                // Default to undetermined integer type
                literalType = PrimitiveType.getIntType();
                break;
        }
        
        setType(node, literalType);
    }
    
    // Visit path expression (identifier)
    public void visit(PathExprNode node) throws RuntimeException {
        try {
            // Get symbol from the path expression
            Symbol symbol = node.getSymbol();
            if (symbol == null) {
                RuntimeException error = new RuntimeException(
                    "Unresolved symbol: " + (node.LSeg != null && node.LSeg.name != null ? node.LSeg.name.name : "unknown")
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Extract type from symbol
            Type type = extractTypeFromSymbol(symbol);
            setType(node, type);
            
            // Ensure the symbol is set back to the node
            node.setSymbol(symbol);
        
            // Handle RSeg if it exists
            if (node.RSeg != null) {
                // Get the symbol for RSeg based on LSeg's symbol
                Symbol rSegSymbol = node.RSeg != null ? node.RSeg.getSymbol() : null;
                if (rSegSymbol == null) {
                    // If RSeg doesn't have a symbol, we need to resolve it based on LSeg's symbol
                    // This typically happens when accessing fields, methods, or associated items
                    if (node.getSymbol() != null) {
                        Symbol lSegSymbol = node.getSymbol();
                        // Ensure LSeg symbol is set back to LSeg
                        if (node.LSeg != null) {
                            node.LSeg.setSymbol(lSegSymbol);
                        }
                        Type lSegType = extractTypeFromSymbol(lSegSymbol);
                    
                        // If LSeg is a struct constructor type, RSeg might be a field or an implemented function
                        if (lSegType instanceof StructConstructorType) {
                            StructType structType = (StructType) lSegType;
                            String rSegName = node.RSeg != null && node.RSeg.name != null ? node.RSeg.name.name : null;
                            
                            if (rSegName == null) {
                                RuntimeException error = new RuntimeException(
                                    "Unresolved field or method name in path expression"
                                );
                                if (throwOnError) {
                                    throw error;
                                } else {
                                    errorCollector.addError(error.getMessage());
                                }
                                return;
                            }
                            
                            // If not a field, look for it in the impl's associated items
                            Symbol structSymbol = structType.getStructSymbol();
                            if (structSymbol != null) {
                                // Look through the impl symbols to find a function or constant with the matching name
                                for (Symbol implSymbol : structSymbol.getImplSymbols()) {
                                    if ((implSymbol.getKind() == SymbolKind.FUNCTION ||
                                            implSymbol.getKind() == SymbolKind.CONSTANT) &&
                                        implSymbol.getName().equals(rSegName)) {
                                        // Found the associated item, use it as the RSeg symbol
                                        node.RSeg.setSymbol(implSymbol);
                                        break;
                                    }
                                }
                                
                                // If we didn't find it in the impl symbols, report an error
                                if (node.RSeg.getSymbol() == null) {
                                    RuntimeException error = new RuntimeException(
                                        "Field or associated item '" + rSegName + "' not found in struct " + structType.getName()
                                    );
                                    if (throwOnError) {
                                        throw error;
                                    } else {
                                        errorCollector.addError(error.getMessage());
                                    }
                                    return;
                                }
                            }
                        }
                        // If LSeg is an enum constructor type, RSeg might be a variant or an impl item
                        else if (lSegType instanceof EnumConstructorType) {
                            EnumType enumType = (EnumType) lSegType;
                            String rSegName = node.RSeg != null && node.RSeg.name != null ? node.RSeg.name.name : null;
                            
                            if (rSegName == null) {
                                RuntimeException error = new RuntimeException(
                                    "Unresolved variant name in path expression"
                                );
                                if (throwOnError) {
                                    throw error;
                                } else {
                                    errorCollector.addError(error.getMessage());
                                }
                                return;
                            }
                            
                            // First check if it's a variant
                            if (enumType.hasVariant(rSegName)) {
                                // Create a symbol for the variant
                                Symbol variantSymbol = new Symbol(rSegName, SymbolKind.ENUM_VARIANT_CONSTRUCTOR, null, 0, false);
                                node.RSeg.setSymbol(variantSymbol);
                                // The type of the variant is an EnumConstructorType that wraps the enum type
                                EnumConstructorType enumConstructorType = new EnumConstructorType(enumType);
                                variantSymbol.setType(enumConstructorType);
                            } else {
                                // If not a variant, look for it in the enum's impl items
                                Symbol enumSymbol = enumType.getEnumSymbol();
                                if (enumSymbol != null) {
                                    // Look through the impl symbols to find a function or constant with the matching name
                                    for (Symbol implSymbol : enumSymbol.getImplSymbols()) {
                                        if ((implSymbol.getKind() == SymbolKind.FUNCTION ||
                                                implSymbol.getKind() == SymbolKind.CONSTANT) &&
                                            implSymbol.getName().equals(rSegName)) {
                                            // Found the associated item, use it as the RSeg symbol
                                            node.RSeg.setSymbol(implSymbol);
                                            break;
                                        }
                                    }
                                    
                                    // If we didn't find it in the impl symbols, report an error
                                    if (node.RSeg.getSymbol() == null) {
                                        RuntimeException error = new RuntimeException(
                                            "Variant or associated item '" + rSegName + "' not found in enum " + enumType.getName()
                                        );
                                        if (throwOnError) {
                                            throw error;
                                        } else {
                                            errorCollector.addError(error.getMessage());
                                        }
                                        return;
                                    }
                                } else {
                                    RuntimeException error = new RuntimeException(
                                        "Variant '" + rSegName + "' not found in enum " + enumType.getName()
                                    );
                                    if (throwOnError) {
                                        throw error;
                                    } else {
                                        errorCollector.addError(error.getMessage());
                                    }
                                    return;
                                }
                            }
                        }
                        // If LSeg is a trait type, RSeg might be an associated item
                        else if (lSegType instanceof TraitType) {
                            TraitType traitType = (TraitType) lSegType;
                            String itemName = node.RSeg != null && node.RSeg.name != null ? node.RSeg.name.name : null;
                            
                            if (itemName == null) {
                                RuntimeException error = new RuntimeException(
                                    "Unresolved associated item name in path expression"
                                );
                                if (throwOnError) {
                                    throw error;
                                } else {
                                    errorCollector.addError(error.getMessage());
                                }
                                return;
                            }
                            
                            // Check if it's a method
                            if (traitType.hasMethod(itemName)) {
                                FunctionType methodType = traitType.getMethodType(itemName);
                                Symbol methodSymbol = new Symbol(itemName, SymbolKind.FUNCTION, null, 0, false);
                                node.RSeg.setSymbol(methodSymbol);
                                methodSymbol.setType(methodType);
                            }
                            // Check if it's a constant
                            else if (traitType.hasConstant(itemName)) {
                                Type constantType = traitType.getConstantType(itemName);
                                Symbol constantSymbol = new Symbol(itemName, SymbolKind.CONSTANT, null, 0, false);
                                node.RSeg.setSymbol(constantSymbol);
                                constantSymbol.setType(constantType);
                            }
                            else {
                                RuntimeException error = new RuntimeException(
                                    "Associated item '" + itemName + "' not found in trait " + traitType.getName()
                                );
                                if (throwOnError) {
                                    throw error;
                                } else {
                                    errorCollector.addError(error.getMessage());
                                }
                                return;
                            }
                        }
                        // If LSeg is a module/namespace, RSeg might be an item in that module
                        // This would require namespace resolution which is beyond the scope of this change
                    }
                }
            
                // If RSeg has a symbol, extract its type
                Symbol rSegSymbolForType = node.RSeg != null ? node.RSeg.getSymbol() : null;
                if (rSegSymbolForType != null) {
                    Type rSegType = extractTypeFromSymbol(rSegSymbolForType);
                    // The type of the entire path expression is the type of the RSeg
                    setType(node, rSegType);
                    // Ensure that the entire PathExprNode's symbol is set to RSeg's symbol
                    node.setSymbol(rSegSymbolForType);
                }
            }
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e);
            }
        }
    }
    
    // Visit path expression segment
    public void visit(PathExprSegNode node) throws RuntimeException {
        try {
            // Get symbol from the path segment
            Symbol symbol = node.getSymbol();
            if (symbol == null) {
                RuntimeException error = new RuntimeException(
                    "Unresolved symbol: " + (node.name != null ? node.name.name : "unknown")
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Extract type from symbol
            Type type = extractTypeFromSymbol(symbol);
            // Note: PathExprSegNode doesn't have a setType method, so we just store it in the symbol
            symbol.setType(type);
            
            // Ensure that symbol is set back to the node
            node.setSymbol(symbol);
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit grouped expression
    public void visit(GroupExprNode node) throws RuntimeException {
        try {
            if (node.innerExpr != null) {
                node.innerExpr.accept(this);
                Type innerType = getType(node.innerExpr);
                setType(node, innerType);
            } else {
                RuntimeException error = new RuntimeException(
                    "Grouped expression has no inner expression"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
            }
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit underscore expression
    public void visit(UnderscoreExprNode node) {
        // Underscore is used as a wildcard pattern that matches any value but doesn't bind it
        // It's a placeholder that can be inferred to any type in context
        // For now, we use INT as a placeholder type that can be inferred to other numeric types
        setType(node, PrimitiveType.getIntType());
    }
    
    // Visit methods for operator expressions
    
    // Visit arithmetic expression
    public void visit(ArithExprNode node) throws RuntimeException {
        try {
            if (node.left == null || node.right == null) {
                RuntimeException error = new RuntimeException(
                    "Arithmetic expression missing operands"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Visit left and right operands
            node.left.accept(this);
            node.right.accept(this);
            
            // Ensure that left operand's symbol is set if it's a PathExprNode
            if (node.left instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.left;
                if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                    extractTypeFromSymbol(pathExpr.getSymbol());
                }
            }
            
            // Ensure that the right operand's symbol is set if it's a PathExprNode
            if (node.right instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.right;
                if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                    extractTypeFromSymbol(pathExpr.getSymbol());
                }
            }
            
            Type leftType = getType(node.left);
            Type rightType = getType(node.right);
            
            // Handle shift operations separately from regular arithmetic operations
            if (isShiftOperation(node.operator)) {
                // For shift operations:
                // Left operand must be an integer type
                // Right operand must be an integer type (can be signed or unsigned)
                // Result type is the same as the left operand type
                
                // Check if left operand is an integer type
                if (!isIntegerType(leftType)) {
                    RuntimeException error = new RuntimeException(
                        "Left operand of shift operation must be an integer type: " + leftType
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                // Check if right operand is an integer type (not just unsigned)
                if (!isIntegerType(rightType)) {
                    RuntimeException error = new RuntimeException(
                        "Right operand of shift operation must be an integer type: " + rightType
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                // Result type is the same as the left operand type
                setType(node, leftType);
            } else {
                // Regular arithmetic operations
                // Check if both operands are numeric
                if (!isNumericType(leftType)) {
                    RuntimeException error = new RuntimeException(
                        "Left operand of arithmetic expression is not numeric: " + leftType
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                if (!isNumericType(rightType)) {
                    RuntimeException error = new RuntimeException(
                        "Right operand of arithmetic expression is not numeric: " + rightType
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                // Find common type for the result
                Type resultType = findCommonType(leftType, rightType);
                if (resultType == null) {
                    RuntimeException error = new RuntimeException(
                        "Cannot find common type for arithmetic expression: " + leftType + " and " + rightType
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                setType(node, resultType);
            }
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e);
            }
        }
    }
    
    // Visit comparison expression
    public void visit(CompExprNode node) throws RuntimeException {
        try {
            if (node.left == null || node.right == null) {
                RuntimeException error = new RuntimeException(
                    "Comparison expression missing operands"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Visit left and right operands
            node.left.accept(this);
            node.right.accept(this);
            
            // Ensure that left operand's symbol is set if it's a PathExprNode
            if (node.left instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.left;
                if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                    extractTypeFromSymbol(pathExpr.getSymbol());
                }
            }
            
            // Ensure that the right operand's symbol is set if it's a PathExprNode
            if (node.right instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.right;
                if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                    extractTypeFromSymbol(pathExpr.getSymbol());
                }
            }
            
            Type leftType = getType(node.left);
            Type rightType = getType(node.right);
            
            // Check if operands are comparable
            // For now, we'll allow comparison of any two types of the same kind
            // In a full implementation, this would be more restrictive
            if (leftType == null || rightType == null || !isTypeCompatible(leftType, rightType)) {
                // Try to find a common type
                Type commonType = findCommonType(leftType, rightType);
                if (commonType == null) {
                    RuntimeException error = new RuntimeException(
                        "Cannot compare incompatible types: " + leftType + " and " + rightType + " at " + getCurrentContext()
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
            }
            
            // Comparison expressions always return boolean
            setType(node, PrimitiveType.getBoolType());
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit lazy logical expression (&&, ||)
    public void visit(LazyExprNode node) throws RuntimeException {
        try {
            if (node.left == null || node.right == null) {
                RuntimeException error = new RuntimeException(
                    "Logical expression missing operands"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Visit left and right operands
            node.left.accept(this);
            node.right.accept(this);
            
            // Ensure that left operand's symbol is set if it's a PathExprNode
            if (node.left instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.left;
                if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                    extractTypeFromSymbol(pathExpr.getSymbol());
                }
            }
            
            // Ensure that the right operand's symbol is set if it's a PathExprNode
            if (node.right instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.right;
                if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                    extractTypeFromSymbol(pathExpr.getSymbol());
                }
            }
            
            Type leftType = getType(node.left);
            Type rightType = getType(node.right);
            
            // Check if both operands are boolean
            if (!leftType.isBoolean()) {
                RuntimeException error = new RuntimeException(
                    "Left operand of logical expression is not boolean: " + leftType
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            if (!rightType.isBoolean()) {
                RuntimeException error = new RuntimeException(
                    "Right operand of logical expression is not boolean: " + rightType
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Logical expressions always return boolean
            setType(node, PrimitiveType.getBoolType());
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit negation expression (!, -)
    public void visit(NegaExprNode node) throws RuntimeException {
        try {
            if (node.innerExpr == null) {
                RuntimeException error = new RuntimeException(
                    "Negation expression missing operand"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Visit inner expression
            node.innerExpr.accept(this);
            Type innerType = getType(node.innerExpr);
            
            // Ensure that the inner expression's symbol is set if it's a PathExprNode
            if (node.innerExpr instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.innerExpr;
                if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                    extractTypeFromSymbol(pathExpr.getSymbol());
                }
            }
            
            if (node.isLogical) {
                // Logical negation (!) requires boolean operand
                if (!innerType.isBoolean()) {
                    RuntimeException error = new RuntimeException(
                        "Logical negation requires boolean operand: " + innerType
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                // Result is boolean
                setType(node, PrimitiveType.getBoolType());
            } else {
                // Arithmetic negation (-) requires numeric operand
                if (!isNumericType(innerType)) {
                    RuntimeException error = new RuntimeException(
                        "Arithmetic negation requires numeric operand: " + innerType
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                // Result is same type as operand
                setType(node, innerType);
            }
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit assignment expression
    public void visit(AssignExprNode node) {
        if (node.left == null || node.right == null) {
            RuntimeException error = new RuntimeException(
                "Assignment expression missing operands"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Check if left side is assignable
        if (!isAssignable(node.left)) {
            RuntimeException error = new RuntimeException(
                "Left side of assignment is not assignable"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Visit left and right operands
        node.left.accept(this);
        node.right.accept(this);
        
        // Ensure that the left operand's symbol is set if it's a PathExprNode
        if (node.left instanceof PathExprNode) {
            PathExprNode pathExpr = (PathExprNode) node.left;
            if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                extractTypeFromSymbol(pathExpr.getSymbol());
            }
        }
        
        // Ensure that the right operand's symbol is set if it's a PathExprNode
        if (node.right instanceof PathExprNode) {
            PathExprNode pathExpr = (PathExprNode) node.right;
            if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                extractTypeFromSymbol(pathExpr.getSymbol());
            }
        }
        
        Type leftType = getType(node.left);
        Type rightType = getType(node.right);
        
        // Check if types are compatible
        if (leftType == null || rightType == null || !isTypeCompatible(leftType, rightType)) {
            // Try to find a common type
            Type commonType = findCommonType(leftType, rightType);
            if (commonType == null || !isTypeCompatible(commonType, leftType)) {
                RuntimeException error = new RuntimeException(
                    "Cannot assign " + rightType + " to " + leftType + " at " + getCurrentContext()
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
        }
        
        // Assignment expression returns the unit type
        setType(node, UnitType.INSTANCE);
    }
    
    // Visit compound assignment expression (+=, -=, *=, /=, %=, &=, |=, ^=, <<=, >>=)
    public void visit(ComAssignExprNode node) throws RuntimeException {
        try {
            if (node.left == null || node.right == null) {
                RuntimeException error = new RuntimeException(
                    "Compound assignment expression missing operands"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Check if left side is assignable
            if (!isAssignable(node.left)) {
                RuntimeException error = new RuntimeException(
                    "Left side of compound assignment is not assignable"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Visit left and right operands
            node.left.accept(this);
            node.right.accept(this);
            
            // Ensure that the left operand's symbol is set if it's a PathExprNode
            if (node.left instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.left;
                if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                    extractTypeFromSymbol(pathExpr.getSymbol());
                }
            }
            
            // Ensure that the right operand's symbol is set if it's a PathExprNode
            if (node.right instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.right;
                if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                    extractTypeFromSymbol(pathExpr.getSymbol());
                }
            }
            
            Type leftType = getType(node.left);
            Type rightType = getType(node.right);
            
            // Handle shift compound assignments separately from regular arithmetic compound assignments
            if (isShiftCompoundAssignment(node.operator)) {
                // For shift compound assignments:
                // Left operand must be an integer type
                // Right operand must be an integer type (can be signed or unsigned)
                // Types must be compatible (right operand can be converted to left operand type)
                
                // Check if left operand is an integer type
                if (!isIntegerType(leftType)) {
                    RuntimeException error = new RuntimeException(
                        "Left operand of shift compound assignment must be an integer type: " + leftType + " at " + getCurrentContext()
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                // Check if right operand is an integer type (not just unsigned)
                if (!isIntegerType(rightType)) {
                    RuntimeException error = new RuntimeException(
                        "Right operand of shift compound assignment must be an integer type: " + rightType + " at " + getCurrentContext()
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
            } else if (isArithmeticCompoundAssignment(node.operator)) {
                // Regular arithmetic compound assignments
                // Check if both operands are numeric
                if (!isNumericType(leftType)) {
                    RuntimeException error = new RuntimeException(
                        "Left operand of arithmetic compound assignment is not numeric: " + leftType + " at " + getCurrentContext()
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                if (!isNumericType(rightType)) {
                    RuntimeException error = new RuntimeException(
                        "Right operand of arithmetic compound assignment is not numeric: " + rightType + " at " + getCurrentContext()
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
            }
            
            // Check if types are compatible
            if (leftType == null || rightType == null || !isTypeCompatible(leftType, rightType)) {
                // Try to find a common type
                Type commonType = findCommonType(leftType, rightType);
                if (commonType == null || !isTypeCompatible(commonType, leftType)) {
                    RuntimeException error = new RuntimeException(
                        "Cannot perform compound assignment with " + rightType + " and " + leftType + " at " + getCurrentContext()
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
            }
            
            // Compound assignment expression returns the unit type
            setType(node, UnitType.INSTANCE);
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e);
            }
        }
    }
    
    // Helper method to check if operator is arithmetic compound assignment
    private boolean isArithmeticCompoundAssignment(oper_t operator) {
        return operator == oper_t.PLUS_ASSIGN ||
               operator == oper_t.MINUS_ASSIGN ||
               operator == oper_t.MUL_ASSIGN ||
               operator == oper_t.DIV_ASSIGN ||
               operator == oper_t.MOD_ASSIGN;
    }
    
    // Helper method to check if operator is shift operation
    private boolean isShiftOperation(oper_t operator) {
        return operator == oper_t.SHL || operator == oper_t.SHR;
    }
    
    // Helper method to check if operator is shift compound assignment
    private boolean isShiftCompoundAssignment(oper_t operator) {
        return operator == oper_t.SHL_ASSIGN || operator == oper_t.SHR_ASSIGN;
    }
    
    // Helper method to check if type is unsigned integer
    private boolean isUnsignedIntegerType(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType.PrimitiveKind kind = ((PrimitiveType)type).getKind();
            return kind == PrimitiveType.PrimitiveKind.U32 ||
                   kind == PrimitiveType.PrimitiveKind.USIZE;
        }
        return false;
    }
    
    // Helper method to check if type is signed integer
    private boolean isSignedIntegerType(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType.PrimitiveKind kind = ((PrimitiveType)type).getKind();
            return kind == PrimitiveType.PrimitiveKind.I32 ||
                   kind == PrimitiveType.PrimitiveKind.ISIZE ||
                   kind == PrimitiveType.PrimitiveKind.INT;
        }
        return false;
    }
    
    // Helper method to check if type is integer (either signed or unsigned)
    private boolean isIntegerType(Type type) {
        return isSignedIntegerType(type) || isUnsignedIntegerType(type);
    }
    
    // Helper method to check if type is String or str type
    private boolean isStringOrStrType(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType.PrimitiveKind kind = ((PrimitiveType)type).getKind();
            return kind == PrimitiveType.PrimitiveKind.STR;
        }
        return false;
    }
    
    // Helper method to check if type is array type
    private boolean isArrayType(Type type) {
        return type instanceof ArrayType;
    }
    
    // Helper method to check if type is u32 or usize
    private boolean isU32OrUsizeType(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType.PrimitiveKind kind = ((PrimitiveType)type).getKind();
            return kind == PrimitiveType.PrimitiveKind.U32 ||
                   kind == PrimitiveType.PrimitiveKind.USIZE;
        }
        return false;
    }
    
    // Helper method to lookup builtin methods for String, str, array, u32 and usize types
    private Symbol lookupBuiltinMethod(String methodName, Type receiverType) {
        // Check if this is a valid builtin method for the given receiver type
        boolean isValidMethod = false;
        
        // Check String and str methods
        if (isStringOrStrType(receiverType)) {
            isValidMethod = methodName.equals("to_string") ||
                         methodName.equals("as_str") ||
                         methodName.equals("as_mut_str") ||
                         methodName.equals("len") ||
                         methodName.equals("append");
        }
        // Check array methods
        else if (isArrayType(receiverType)) {
            isValidMethod = methodName.equals("len");
        }
        // Check u32 and usize methods
        else if (isU32OrUsizeType(receiverType)) {
            isValidMethod = methodName.equals("to_string");
        }
        
        if (!isValidMethod) {
            return null; // Not a valid builtin method for this type
        }
        
        // Create a BuiltinFunctionNode to represent the builtin method
        BuiltinFunctionNode builtinNode = new BuiltinFunctionNode(methodName, BuiltinFunctionNode.BuiltinType.METHOD);
        
        // Configure the builtin method based on the method name
        switch (methodName) {
            case "to_string":
                builtinNode.configureToString();
                break;
            case "as_str":
                builtinNode.configureAsStr();
                break;
            case "as_mut_str":
                builtinNode.configureAsMutStr();
                break;
            case "len":
                builtinNode.configureLen();
                break;
            case "append":
                builtinNode.configureAppend();
                break;
            default:
                return null; // Unknown builtin method
        }
        
        // Set the symbol for the builtin method node
        Symbol builtinMethod = new Symbol(
            methodName,
            SymbolKind.FUNCTION, // Methods are treated as functions
            builtinNode, // Use BuiltinFunctionNode as declaration node
            0, // Global scope
            false // Immutable
        );
        
        // Set the symbol in the builtin method node
        builtinNode.setSymbol(builtinMethod);
        
        return builtinMethod;
    }
    
    // Helper method to check if a cast is valid
    private boolean isValidCast(Type sourceType, Type targetType) {
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
    
    // Helper method to check if two types are compatible
    private boolean isTypeCompatible(Type actualType, Type expectedType) {
        // If either type is null, they're not compatible
        if (actualType == null || expectedType == null) {
            return false;
        }
        
        // Handle AmbiguousBlockType cases
        if (actualType instanceof AmbiguousBlockType) {
            AmbiguousBlockType ambiguousType = (AmbiguousBlockType) actualType;
            // Check if ambiguous type can resolve to the expected type
            return ambiguousType.canResolveTo(expectedType);
        }
        
        if (expectedType instanceof AmbiguousBlockType) {
            AmbiguousBlockType ambiguousType = (AmbiguousBlockType) expectedType;
            // Check if actual type is compatible with either the value type or unit type
            return isTypeCompatible(actualType, ambiguousType.getValueType()) ||
                   isTypeCompatible(actualType, UnitType.INSTANCE);
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
    
    // Visit methods for complex expressions
    
    // Visit function call expression
    public void visit(CallExprNode node) {
        if (node.function == null) {
            RuntimeException error = new RuntimeException(
                "Function call missing function expression"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Visit function expression
        node.function.accept(this);
        Type functionType = getType(node.function);
        
        // Ensure that the function's symbol is set if it's a PathExprNode
        if (node.function instanceof PathExprNode) {
            PathExprNode pathExpr = (PathExprNode) node.function;
            if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                extractTypeFromSymbol(pathExpr.getSymbol());
            }
        }
        
        // Check if function type is a function type
        if (!(functionType instanceof FunctionType)) {
            RuntimeException error = new RuntimeException(
                "Called expression is not a function: " + functionType
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        FunctionType funcType = (FunctionType) functionType;
        
        // Check argument count
        int expectedArgs = funcType.getParameterTypes().size();
        int actualArgs = node.arguments != null ? node.arguments.size() : 0;
        
        if (expectedArgs != actualArgs) {
            RuntimeException error = new RuntimeException(
                "Function expects " + expectedArgs + " arguments, but got " + actualArgs
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Check argument types
        if (node.arguments != null) {
            for (int i = 0; i < node.arguments.size(); i++) {
                ExprNode arg = node.arguments.get(i);
                arg.accept(this);
                Type argType = getType(arg);
                Type expectedType = funcType.getParameterTypes().get(i);
                
                if (argType == null || expectedType == null || !isTypeCompatible(argType, expectedType)) {
                    // Try to find a common type
                    Type commonType = findCommonType(argType, expectedType);
                    if (commonType == null || !isTypeCompatible(commonType, expectedType)) {
                        RuntimeException error = new RuntimeException(
                            "Argument " + (i + 1) + " type mismatch: expected " + expectedType + ", got " + argType + " at " + getCurrentContext()
                        );
                        if (throwOnError) {
                            throw error;
                        } else {
                            errorCollector.addError(error.getMessage());
                        }
                        return;
                    }
                }
            }
        }
        
        // Set result type to function's return type
        setType(node, funcType.getReturnType());
    }
    
    // Visit method call expression
    public void visit(MethodCallExprNode node) throws RuntimeException {
        try {
            if (node.receiver == null || node.methodName == null) {
                RuntimeException error = new RuntimeException(
                    "Method call missing receiver or method name"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Visit receiver expression
            node.receiver.accept(this);
            Type receiverType = getType(node.receiver);
            
            // Automatically dereference receiver if it's a reference type
            Type dereferencedType = receiverType;
            while (dereferencedType instanceof ReferenceType) {
                dereferencedType = ((ReferenceType) dereferencedType).getInnerType();
            }
            
            // Look up method symbol based on dereferenced receiver type
            String methodName = node.methodName.name != null ? node.methodName.name.name : null;
            if (methodName == null) {
                RuntimeException error = new RuntimeException(
                    "Method name is null"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            Symbol methodSymbol = null;
            
            // Check if dereferenced receiver type is a struct type
            if (dereferencedType instanceof StructType) {
                StructType structType = (StructType) dereferencedType;
                // For struct methods, we need to look up method in the struct's implementation
                Symbol structSymbol = structType.getStructSymbol();
                if (structSymbol != null) {
                    // Look through the impl symbols to find a function with the matching name
                    for (Symbol implSymbol : structSymbol.getImplSymbols()) {
                        if (implSymbol.getKind() == SymbolKind.FUNCTION &&
                            implSymbol.getName().equals(methodName)) {
                            // Found the method, use it as the method symbol
                            methodSymbol = implSymbol;
                            break;
                        }
                    }
                }
            }
            // Check if dereferenced receiver type is a trait type
            else if (dereferencedType instanceof TraitType) {
                TraitType traitType = (TraitType) dereferencedType;
                if (traitType.hasMethod(methodName)) {
                    FunctionType methodType = traitType.getMethodType(methodName);
                    methodSymbol = new Symbol(methodName, SymbolKind.FUNCTION, null, 0, false);
                    methodSymbol.setType(methodType);
                }
            }
            // Check if dereferenced receiver type is String or str type and look for builtin methods
            else if (isStringOrStrType(dereferencedType)) {
                // Look for builtin method
                methodSymbol = lookupBuiltinMethod(methodName, dereferencedType);
            }
            // Check if dereferenced receiver type is array type and look for builtin methods
            else if (isArrayType(dereferencedType)) {
                // Look for builtin method
                methodSymbol = lookupBuiltinMethod(methodName, dereferencedType);
            }
            // Check if dereferenced receiver type is u32 or usize and look for builtin methods
            else if (isU32OrUsizeType(dereferencedType)) {
                // Look for builtin method
                methodSymbol = lookupBuiltinMethod(methodName, dereferencedType);
            }
            
            if (methodSymbol == null) {
                RuntimeException error = new RuntimeException(
                    "Method '" + methodName + "' not found for type " + dereferencedType
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Set the method symbol on the methodName node for future reference
            node.methodName.setSymbol(methodSymbol);
            
            // Ensure that the symbol's type is set
            if (methodSymbol.getType() == null) {
                extractTypeFromSymbol(methodSymbol);
            }
            
            // Extract method type
            Type methodType = extractTypeFromSymbol(methodSymbol);
            
            // Check if method type is a function type
            if (!(methodType instanceof FunctionType)) {
                RuntimeException error = new RuntimeException(
                    "Method is not a function: " + methodType
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            FunctionType funcType = (FunctionType) methodType;
            
            // Check if it's a method (should have self parameter)
            if (!funcType.isMethod()) {
                RuntimeException error = new RuntimeException(
                    "Called function is not a method: " + methodSymbol.getName()
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Check argument count (excluding self parameter)
            int expectedArgs = funcType.getParameterTypes().size();
            int actualArgs = node.arguments != null ? node.arguments.size() : 0;
            
            if (expectedArgs != actualArgs) {
                RuntimeException error = new RuntimeException(
                    "Method expects " + expectedArgs + " arguments, but got " + actualArgs
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Check argument types (excluding self parameter)
            if (node.arguments != null) {
                for (int i = 0; i < node.arguments.size(); i++) {
                    ExprNode arg = node.arguments.get(i);
                    arg.accept(this);
                    Type argType = getType(arg);
                    Type expectedType = funcType.getParameterTypes().get(i);
                    
                    if (argType == null || expectedType == null || !isTypeCompatible(argType, expectedType)) {
                        // Try to find a common type
                        Type commonType = findCommonType(argType, expectedType);
                        if (commonType == null || !isTypeCompatible(commonType, expectedType)) {
                            RuntimeException error = new RuntimeException(
                                "Argument " + (i + 1) + " type mismatch: expected " + expectedType + ", got " + argType + " at " + getCurrentContext()
                            );
                            if (throwOnError) {
                                throw error;
                            } else {
                                errorCollector.addError(error.getMessage());
                            }
                            return;
                        }
                    }
                }
            }
            
            // Set result type to method's return type
            setType(node, funcType.getReturnType());
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e);
            }
        }
    }
    
    // Visit field access expression
    public void visit(FieldExprNode node) {
        if (node.receiver == null || node.fieldName == null) {
            RuntimeException error = new RuntimeException(
                "Field access missing receiver or field name"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Visit receiver expression
        node.receiver.accept(this);
        Type receiverType = getType(node.receiver);
        
        // Automatically dereference receiver if it's a reference type
        Type dereferencedType = receiverType;
        while (dereferencedType instanceof ReferenceType) {
            dereferencedType = ((ReferenceType) dereferencedType).getInnerType();
        }
        
        // Check if dereferenced receiver type is a struct type
        if (!(dereferencedType instanceof StructType)) {
            RuntimeException error = new RuntimeException(
                "Cannot access field on non-struct type: " + receiverType
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        StructType structType = (StructType) dereferencedType;
        String fieldName = node.fieldName.name;
        
        // Check if field exists
        Type fieldType = structType.getFieldType(fieldName);
        if (fieldType == null) {
            RuntimeException error = new RuntimeException(
                "Field '" + fieldName + "' not found in struct " + structType.getName()
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Set result type to field type
        setType(node, fieldType);
        
        // Ensure that the field name's symbol is set if it exists
        // Note: fieldName is an IdentifierNode, not a PathExprNode, so it might not have a symbol
        // This is just a precaution in case the implementation changes
    }
    
    // Visit index expression
    public void visit(IndexExprNode node) throws RuntimeException {
        try {
            if (node.array == null || node.index == null) {
                RuntimeException error = new RuntimeException(
                    "Index expression missing array or index"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Visit array expression
            node.array.accept(this);
            Type arrayType = getType(node.array);
            
            // Ensure that the array's symbol is set if it's a PathExprNode
            if (node.array instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.array;
                if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                    extractTypeFromSymbol(pathExpr.getSymbol());
                }
            }
            
            // Visit index expression
            node.index.accept(this);
            Type indexType = getType(node.index);
            
            // Ensure that the index's symbol is set if it's a PathExprNode
            if (node.index instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.index;
                if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                    extractTypeFromSymbol(pathExpr.getSymbol());
                }
            }
            
            // Automatically dereference array type if it's a reference type
            Type dereferencedArrayType = arrayType;
            while (dereferencedArrayType instanceof ReferenceType) {
                dereferencedArrayType = ((ReferenceType) dereferencedArrayType).getInnerType();
            }
            
            // Check if dereferenced array type is an array type
            if (!(dereferencedArrayType instanceof ArrayType)) {
                RuntimeException error = new RuntimeException(
                    "Cannot index non-array type: " + arrayType
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Check if index type is numeric
            if (!isNumericType(indexType)) {
                RuntimeException error = new RuntimeException(
                    "Array index must be numeric: " + indexType
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Set result type to array element type
            ArrayType arrType = (ArrayType) dereferencedArrayType;
            setType(node, arrType.getElementType());
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit borrow expression (&, &&)
    public void visit(BorrowExprNode node) throws RuntimeException {
        try {
            if (node.innerExpr == null) {
                RuntimeException error = new RuntimeException(
                    "Borrow expression missing inner expression"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Visit inner expression
            node.innerExpr.accept(this);
            Type innerType = getType(node.innerExpr);
            
            // Ensure that the inner expression's symbol is set if it's a PathExprNode
            if (node.innerExpr instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.innerExpr;
                if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                    extractTypeFromSymbol(pathExpr.getSymbol());
                }
            }
            
            // Ensure that the inner expression's symbol is set if it's a PathExprNode
            if (node.innerExpr instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.innerExpr;
                if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                    extractTypeFromSymbol(pathExpr.getSymbol());
                }
            }
            
            // Create reference type using nested references instead of isDoubleReference flag
            Type refType = new ReferenceType(innerType, node.isMutable);
            
            // If it's a double reference (&&), wrap it in another reference
            if (node.isDoubleReference) {
                refType = new ReferenceType(refType, node.isMutable);
            }
            
            setType(node, refType);
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e);
            }
        }
    }
    
    // Visit dereference expression (*)
    public void visit(DerefExprNode node) throws RuntimeException {
        try {
            if (node.innerExpr == null) {
                RuntimeException error = new RuntimeException(
                    "Dereference expression missing inner expression"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Visit inner expression
            node.innerExpr.accept(this);
            Type innerType = getType(node.innerExpr);
            
            // Ensure that the inner expression's symbol is set if it's a PathExprNode
            if (node.innerExpr instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.innerExpr;
                if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                    extractTypeFromSymbol(pathExpr.getSymbol());
                }
            }
            
            // Check if inner type is a reference type
            if (!(innerType instanceof ReferenceType)) {
                RuntimeException error = new RuntimeException(
                    "Cannot dereference non-reference type: " + innerType
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Set result type to inner type of reference
            ReferenceType refType = (ReferenceType) innerType;
            setType(node, refType.getInnerType());
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e);
            }
        }
    }
    
    // Visit type cast expression (as)
    public void visit(TypeCastExprNode node) {
        if (node.expr == null || node.type == null) {
            RuntimeException error = new RuntimeException(
                "Type cast expression missing expression or target type"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Visit expression
        node.expr.accept(this);
        Type exprType = getType(node.expr);
        
        // Ensure that the expression's symbol is set if it's a PathExprNode
        if (node.expr instanceof PathExprNode) {
            PathExprNode pathExpr = (PathExprNode) node.expr;
            if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                extractTypeFromSymbol(pathExpr.getSymbol());
            }
        }
        
        // Extract target type
        Type targetType = extractTypeFromTypeNode(node.type);
        
        // Check if cast is valid
        if (!isValidCast(exprType, targetType)) {
            RuntimeException error = new RuntimeException(
                "Invalid cast from " + exprType + " to " + targetType
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
                return;
            }
        }
        
        setType(node, targetType);
    }
    
    // Visit methods for control flow expressions
    
    // Visit block expression
    public void visit(BlockExprNode node) throws RuntimeException {
        try {
            if (node.statements != null) {
                // Visit all statements in block
                for (StmtNode stmt : node.statements) {
                    stmt.accept(this);
                }
            }
            
            // Check if block contains a return statement
            boolean hasReturn = containsReturnStatement(node);
            
            // Determine block type based on context and content
            if (hasReturn) {
                // Block contains return statement, set to never type
                setType(node, NeverType.INSTANCE);
            } else if (node.returnValue != null) {
                // Block has an explicit return value, visit it
                node.returnValue.accept(this);
                Type valueType = getType(node.returnValue);
                setType(node, valueType);
            } else if (node.statements != null && !node.statements.isEmpty()) {
                // Check if the last statement is an expression statement
                StmtNode lastStmt = node.statements.get(node.statements.size() - 1);
                if (lastStmt instanceof ExprStmtNode) {
                    ExprStmtNode exprStmt = (ExprStmtNode) lastStmt;
                    if (exprStmt.expr != null) {
                        // Check if the expression is an ExpressionWithBlock without semicolon
                        if (exprStmt.expr instanceof ExprWithBlockNode) {
                            // The block could be treated as either a value type or unit type
                            Type lastExprType = getType(exprStmt.expr);
                            Type ambiguousType = AmbiguousBlockType.create(lastExprType);
                            setType(node, ambiguousType);
                            return;
                        } else {
                            // If the expression has an inner expression, process it similarly to ExprWithBlockNode
                            if (exprStmt.expr.innerExpr != null) {
                                // The block could be treated as either a value type or unit type
                                Type lastExprType = getType(exprStmt.expr);
                                Type ambiguousType = AmbiguousBlockType.create(lastExprType);
                                setType(node, ambiguousType);
                                return;
                            } else {
                                // No inner expression, so it's unit type
                                setType(node, UnitType.INSTANCE);
                                return;
                            }
                        }
                    }
                }
            }
            
            // Default case: empty block or block with only non-expression statements
            setType(node, UnitType.INSTANCE);
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit if expression
    public void visit(IfExprNode node) throws RuntimeException {
        try {
            if (node.condition == null || node.thenBranch == null) {
                RuntimeException error = new RuntimeException(
                    "If expression missing condition or then branch"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Visit condition
            node.condition.accept(this);
            Type conditionType = getType(node.condition);
            
            // Check if condition is boolean
            if (!conditionType.isBoolean()) {
                RuntimeException error = new RuntimeException(
                    "If condition must be boolean: " + conditionType
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Visit then branch
            node.thenBranch.accept(this);
            node.thenBranch.accept(this);
            Type thenType = getType(node.thenBranch);
            
            // Check if all branches contain return statements
            boolean thenHasReturn = containsReturnStatement(node.thenBranch);
            boolean elseifHasReturn = node.elseifBranch != null ?
                                   containsReturnStatement(node.elseifBranch) : false;
            boolean elseHasReturn = node.elseBranch != null ?
                                  containsReturnStatement(node.elseBranch) : false;
            
            // If all branches contain return statements, the if expression has never type
            // This is used when the if expression is used in a value context
            if (thenHasReturn &&
                (node.elseifBranch == null || elseifHasReturn) &&
                (node.elseBranch == null || elseHasReturn)) {
                // All branches contain return statements, so the if expression never completes normally
                setType(node, NeverType.INSTANCE);
                return;
            }
            
            // Initialize result type with then branch type
            Type resultType = thenType;
            
            // Check if there's an elseif branch
            if (node.elseifBranch != null) {
                // Visit elseif branch
                node.elseifBranch.accept(this);
                node.elseifBranch.accept(this);
                Type elseifType = getType(node.elseifBranch);
                
                // Find common type between then and elseif branches
                resultType = findCommonType(thenType, elseifType);
                if (resultType == null) {
                    RuntimeException error = new RuntimeException(
                        "If and elseif branches have incompatible types: " + thenType + " and " + elseifType
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
            }
            
            // Check if there's an else branch
            if (node.elseBranch != null) {
                // Visit else branch
                node.elseBranch.accept(this);
                node.elseBranch.accept(this);
                Type elseType = getType(node.elseBranch);
                
                // Find common type between current result type and else branch
                Type newResultType = findCommonType(resultType, elseType);
                if (newResultType == null) {
                    RuntimeException error = new RuntimeException(
                        "If branches and else branch have incompatible types: " + resultType + " and " + elseType
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                resultType = newResultType;
            }
            // Disambiguate result type if needed
            Type disambiguatedType = disambiguateType(resultType, null);
            setType(node, disambiguatedType);
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit loop expression
    public void visit(LoopExprNode node) throws RuntimeException {
        try {
            if (node.body == null) {
                RuntimeException error = new RuntimeException(
                    "Loop expression missing body"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Enter appropriate loop context
            if (node.isInfinite) {
                enterLoopContext(node);
            } else {
                enterWhileContext(node);
                
                // Check condition for while loop
                if (node.condition != null) {
                    node.condition.accept(this);
                    Type conditionType = getType(node.condition);
                    
                    if (!conditionType.isBoolean()) {
                        RuntimeException error = new RuntimeException(
                            "While condition must be boolean: " + conditionType
                        );
                        if (throwOnError) {
                            throw error;
                        } else {
                            errorCollector.addError(error.getMessage());
                        }
                        return;
                    }
                }
            }
            
            // Visit loop body
            node.body.accept(this);
            node.body.accept(this);
            
            // Collect break types from loop context
            ControlFlowContext loopContext = contextStack.peek();
            List<Type> breakTypes = loopContext.getBreakTypes();
            
            // Exit loop context
            exitLoopContext();
            
            // Determine loop type based on break types
            if (breakTypes.isEmpty()) {
                // No break statements, loop type is never
                setType(node, NeverType.INSTANCE);
            } else {
                // Find common type among all break types
                Type resultType = breakTypes.get(0);
                for (int i = 1; i < breakTypes.size(); i++) {
                    resultType = findCommonType(resultType, breakTypes.get(i));
                    if (resultType == null) {
                        RuntimeException error = new RuntimeException(
                            "Break statements have incompatible types in loop"
                        );
                        if (throwOnError) {
                            throw error;
                        } else {
                            errorCollector.addError(error.getMessage());
                        }
                        return;
                    }
                }
                // Disambiguate result type if needed
                Type disambiguatedType = disambiguateType(resultType, null);
                setType(node, disambiguatedType);
            }
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e);
            }
        }
    }
    
    // Visit break expression
    public void visit(BreakExprNode node) throws RuntimeException {
        try {
            // Find nearest loop context
            ControlFlowContext loopContext = findNearestLoopContext();
            if (loopContext == null) {
                RuntimeException error = new RuntimeException(
                    "Break statement outside of loop"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Set target node
            node.setTargetNode(loopContext.getNode());
            
            // Check if break is in a while loop and has a value
            if (loopContext.getType() == ControlFlowContextType.WHILE && node.value != null) {
                RuntimeException error = new RuntimeException(
                    "Break statements in while loops cannot have values"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // If break has a value, visit it
            if (node.value != null) {
                node.value.accept(this);
                Type valueType = getType(node.value);
                
                // Ensure that the value's symbol is set if it's a PathExprNode
                if (node.value instanceof PathExprNode) {
                    PathExprNode pathExpr = (PathExprNode) node.value;
                    if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                        extractTypeFromSymbol(pathExpr.getSymbol());
                    }
                }
                
                // Add break type to loop context
                loopContext.getBreakTypes().add(valueType);
                setType(node, NeverType.INSTANCE);
            } else {
                // Break without value is unit type
                loopContext.getBreakTypes().add(UnitType.INSTANCE);
                setType(node, NeverType.INSTANCE);
            }
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit continue expression
    public void visit(ContinueExprNode node) {
        // Find nearest loop context
        ControlFlowContext loopContext = findNearestLoopContext();
        if (loopContext == null) {
            RuntimeException error = new RuntimeException(
                "Continue statement outside of loop"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Set target node
        node.setTargetNode(loopContext.getNode());
        
        // Continue expression has never type
        setType(node, NeverType.INSTANCE);
    }
    
    // Visit return expression
    public void visit(ReturnExprNode node) throws RuntimeException {
        try {
            // Find nearest function context
            ControlFlowContext functionContext = findNearestFunctionContext();
            if (functionContext == null) {
                RuntimeException error = new RuntimeException(
                    "Return statement outside of function"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Set target node
            node.setTargetNode(functionContext.getNode());
            
            // Get function return type
            FunctionNode funcNode = (FunctionNode) functionContext.getNode();
            Type expectedReturnType = funcNode.returnType != null ?
                                     extractTypeFromTypeNode(funcNode.returnType) :
                                     UnitType.INSTANCE;
            
            // If return has a value, visit it
            if (node.value != null) {
                node.value.accept(this);
                Type valueType = getType(node.value);
                
                // Ensure that the value's symbol is set if it's a PathExprNode
                if (node.value instanceof PathExprNode) {
                    PathExprNode pathExpr = (PathExprNode) node.value;
                    if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                        extractTypeFromSymbol(pathExpr.getSymbol());
                    }
                }
                
                // Check if return type matches expected type
                if (valueType == null || expectedReturnType == null || !isTypeCompatible(valueType, expectedReturnType)) {
                    // Try to find a common type
                    Type commonType = findCommonType(valueType, expectedReturnType);
                    if (commonType == null || !isTypeCompatible(commonType, expectedReturnType)) {
                        RuntimeException error = new RuntimeException(
                            "Return type mismatch: expected " + expectedReturnType + ", got " + valueType + " at " + getCurrentContext()
                        );
                        if (throwOnError) {
                            throw error;
                        } else {
                            errorCollector.addError(error.getMessage());
                        }
                        return;
                    }
                }
                
                // Add return type to function context for better control flow analysis
                functionContext.getReturnTypes().add(valueType);
                // Return expression itself has never type since it doesn't complete normally
                setType(node, NeverType.INSTANCE);
            } else {
                // Return without value is unit type
                if (!expectedReturnType.isUnit()) {
                    RuntimeException error = new RuntimeException(
                        "Return type mismatch: expected " + expectedReturnType + ", got ()"
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                // Add unit type to function context for better control flow analysis
                functionContext.getReturnTypes().add(UnitType.INSTANCE);
                // Return expression itself has never type since it doesn't complete normally
                setType(node, NeverType.INSTANCE);
            }
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit array expression
    public void visit(ArrayExprNode node) throws RuntimeException {
        try {
            if (node.elements != null && !node.elements.isEmpty()) {
                // Array with explicit elements
                Type elementType = null;
                
                // Visit all elements and determine common type
                for (ExprNode element : node.elements) {
                    element.accept(this);
                    Type elemType = getType(element);
                    
                    // Ensure that the element's symbol is set if it's a PathExprNode
                    if (element instanceof PathExprNode) {
                        PathExprNode pathExpr = (PathExprNode) element;
                        if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                            extractTypeFromSymbol(pathExpr.getSymbol());
                        }
                    }
                    
                    if (elementType == null) {
                        elementType = elemType;
                    } else {
                        // Find common type between elements
                        elementType = findCommonType(elementType, elemType);
                        if (elementType == null) {
                            RuntimeException error = new RuntimeException(
                                "Array elements have incompatible types"
                            );
                            if (throwOnError) {
                                throw error;
                            } else {
                                errorCollector.addError(error.getMessage());
                            }
                            return;
                        }
                    }
                }
                
                // Create array type with determined element type and size
                ArrayType arrayType = new ArrayType(elementType, node.elements.size());
                setType(node, arrayType);
                
            } else if (node.repeatedElement != null && node.size != null) {
                // Array with repeated element [expr; size]
                node.repeatedElement.accept(this);
                Type elementType = getType(node.repeatedElement);
                
                // Ensure that the repeated element's symbol is set if it's a PathExprNode
                if (node.repeatedElement instanceof PathExprNode) {
                    PathExprNode pathExpr = (PathExprNode) node.repeatedElement;
                    if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                        extractTypeFromSymbol(pathExpr.getSymbol());
                    }
                }
                
                // First, type-check the size expression
                node.size.accept(this);
                Type sizeType = getType(node.size);
                
                // Check if size expression is of numeric type
                if (!isNumericType(sizeType)) {
                    RuntimeException error = new RuntimeException(
                        "Array size must be a numeric type, got: " + sizeType
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                // Now check if size is a constant expression
                ConstantValue sizeValue = constantEvaluator.evaluate(node.size);
                
                if (sizeValue == null) {
                    RuntimeException error = new RuntimeException(
                        "Array size expression is not a constant"
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                if (!sizeValue.isNumeric()) {
                    RuntimeException error = new RuntimeException(
                        "Array size must be numeric: " + sizeValue.getType()
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                long arraySize = sizeValue.getAsLong();
                
                // Check if array size is negative
                if (arraySize < 0) {
                    RuntimeException error = new RuntimeException(
                        "Array size cannot be negative: " + arraySize
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                // Create array type with repeated element type and evaluated size
                ArrayType arrayType = new ArrayType(elementType, arraySize);
                setType(node, arrayType);
                
            } else {
                // Empty array []
                // For empty arrays, we can't determine element type
                // In a full implementation, this would require type inference or explicit type annotation
                RuntimeException error = new RuntimeException(
                    "Empty array requires type annotation"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
            }
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit struct expression
    public void visit(StructExprNode node) throws RuntimeException {
        try {
            if (node.structName == null) {
                RuntimeException error = new RuntimeException(
                    "Struct expression missing struct name"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Get struct symbol
            Symbol structSymbol = node.structName.getSymbol();
            if (structSymbol == null) {
                RuntimeException error = new RuntimeException(
                    "Unresolved struct: " + (node.structName.name != null ? node.structName.name.name : "unknown")
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Extract struct type
            Type structType = extractTypeFromSymbol(structSymbol);
            
            // Ensure that symbol is set back to the structName node
            node.structName.setSymbol(structSymbol);
            
            // Check if it's a struct type
            if (!(structType instanceof StructConstructorType)) {
                RuntimeException error = new RuntimeException(
                    "Expression is not a struct constructor: " + structType
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            StructConstructorType structConstructorType = (StructConstructorType) structType;
            StructType structTypeInfo = structConstructorType.getStructType();
            
            // Check field values if provided
            if (node.fieldValues != null) {
                // Check for duplicate field names
                java.util.Set<String> fieldNames = new java.util.HashSet<>();
                for (FieldValNode fieldVal : node.fieldValues) {
                    if (fieldVal.fieldName == null || fieldVal.value == null) {
                        RuntimeException error = new RuntimeException(
                            "Struct field value missing field name or value"
                        );
                        if (throwOnError) {
                            throw error;
                        } else {
                            errorCollector.addError(error.getMessage());
                        }
                        return;
                    }
                    
                    String fieldName = fieldVal.fieldName.name;
                    
                    // Check for duplicate field names
                    if (fieldNames.contains(fieldName)) {
                        RuntimeException error = new RuntimeException(
                            "Duplicate field '" + fieldName + "' in struct initialization"
                        );
                        if (throwOnError) {
                            throw error;
                        } else {
                            errorCollector.addError(error.getMessage());
                        }
                        return;
                    }
                    fieldNames.add(fieldName);
                    
                    Type fieldType = structTypeInfo.getFieldType(fieldName);
                    
                    if (fieldType == null) {
                        RuntimeException error = new RuntimeException(
                            "Field '" + fieldName + "' not found in struct " + structTypeInfo.getName()
                        );
                        if (throwOnError) {
                            throw error;
                        } else {
                            errorCollector.addError(error.getMessage());
                        }
                        return;
                    }

                    fieldVal.accept(this);
                    
                    Type valueType = getType(fieldVal.value);

                    if (valueType == null) {
                        throw new RuntimeException(
                            "Unable to determine type of value for field '" + fieldName + "' in struct " + structTypeInfo.getName()
                        );
                    }
                    
                    // Disambiguate value type if needed
                    Type disambiguatedValueType = disambiguateType(valueType, fieldType);
                    // Check if field value type matches expected field type
                    if (disambiguatedValueType == null || fieldType == null || !isTypeCompatible(disambiguatedValueType, fieldType)) {
                        // Try to find a common type
                        Type commonType = findCommonType(disambiguatedValueType, fieldType);
                        if (commonType == null || !isTypeCompatible(commonType, fieldType)) {
                            RuntimeException error = new RuntimeException(
                                "Field '" + fieldName + "' type mismatch: expected " + fieldType + ", got " + disambiguatedValueType + " at " + getCurrentContext()
                            );
                            if (throwOnError) {
                                throw error;
                            } else {
                                errorCollector.addError(error.getMessage());
                            }
                            return;
                        }
                    }
                }
                
                // Check if number of field values matches number of fields in struct
                int structFieldCount = structTypeInfo.getFields().size();
                int providedFieldCount = node.fieldValues.size();
                
                if (structFieldCount != providedFieldCount) {
                    RuntimeException error = new RuntimeException(
                        "Struct " + structTypeInfo.getName() + " expects " + structFieldCount +
                        " fields, but " + providedFieldCount + " were provided"
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
            }
            
            // Set result type to struct type
            setType(node, structTypeInfo);
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e);
            }
        }
    }
    
    // Visit field value node (used in struct expressions)
    public void visit(FieldValNode node) throws RuntimeException {
        try {
            if (node.value != null) {
                node.value.accept(this);
            }
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit methods for statements
    
    // Visit function node
    public void visit(FunctionNode node) throws RuntimeException {
        try {
            // Enter function context
            enterFunctionContext(node);
            
            // Set current Self type if it's a method
            if (node.selfPara != null) {
                // For methods, Self refers to the receiver type
                // We need to determine the type of the receiver based on the impl block
                
                // First, find the parent impl node to determine the receiver type
                ASTNode parent = node.getFather();
                Type receiverType = null;
                
                while (parent != null && !(parent instanceof ImplNode)) {
                    parent = parent.getFather();
                }
                
                if (parent instanceof ImplNode) {
                    ImplNode implNode = (ImplNode) parent;
                    // Get the type symbol from the impl node
                    Symbol typeSymbol = implNode.getTypeSymbol();
                    if (typeSymbol != null) {
                        try {
                            receiverType = extractTypeFromSymbol(typeSymbol);
                        } catch (RuntimeException e) {
                            // Handle error extracting type from symbol
                            if (throwOnError) {
                                throw e;
                            } else {
                                errorCollector.addError(e);
                            }
                        }
                    }
                }
                
                // If we couldn't determine the receiver type, use a placeholder
                if (receiverType == null) {
                    throw new RuntimeException(
                        "Unable to determine receiver type for method"
                    );
                }
                
                // Handle self parameter based on its type and mutability
                Type selfType = receiverType;
                
                // If self is a reference, create a reference type
                if (node.selfPara.isReference) {
                    selfType = new ReferenceType(receiverType, node.selfPara.isMutable);
                }
                
                // Set current Self type for use in the method body
                setCurrentType(selfType);
            }
            
            // Visit function body if it exists
            if (node.body != null) {
                // Visit function body
                node.body.accept(this);
                node.body.accept(this);
                
                // Get the expected return type
                Type expectedReturnType = node.returnType != null ?
                                       extractTypeFromTypeNode(node.returnType) :
                                       UnitType.INSTANCE;
                
                // Get the actual body type
                Type bodyType = getType(node.body);
                
                // Check if body type matches expected return type
                // Check if body type matches expected return type, considering type compatibility
                if (bodyType == null || expectedReturnType == null || !isTypeCompatible(bodyType, expectedReturnType)) {
                    // Try to find a common type
                    Type commonType = findCommonType(bodyType, expectedReturnType);
                    if (commonType == null || !isTypeCompatible(commonType, expectedReturnType)) {
                        RuntimeException error = new RuntimeException(
                            "Function body type mismatch: expected " + expectedReturnType +
                            ", got " + bodyType + " in function '" +
                            (node.name != null ? node.name.name : "anonymous") + "' at " + getCurrentContext()
                        );
                        if (throwOnError) {
                            throw error;
                        } else {
                            errorCollector.addError(error.getMessage());
                        }
                    }
                }
                
                // Disambiguate body type if needed
                Type disambiguatedBodyType = disambiguateType(bodyType, expectedReturnType);
            }
            
            // Clear current Self type
            clearCurrentType();
            
            // Exit function context
            exitFunctionContext();
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e);
            }
        }
    }
    
    // Visit let statement node
    public void visit(LetStmtNode node) throws RuntimeException {
        try {
            // Visit the value if it exists
            if (node.value != null) {
                node.value.accept(this);
                Type valueType = getType(node.value);
                
                // If there's an explicit type, check compatibility
                if (node.type != null) {
                    Type declaredType = extractTypeFromTypeNode(node.type);
                    // Disambiguate value type if needed
                    Type disambiguatedValueType = disambiguateType(valueType, declaredType);
                    if (disambiguatedValueType == null || declaredType == null || !isTypeCompatible(disambiguatedValueType, declaredType)) {
                        // Try to find a common type
                        Type commonType = findCommonType(disambiguatedValueType, declaredType);
                        if (commonType == null || !isTypeCompatible(commonType, declaredType)) {
                            RuntimeException error = new RuntimeException(
                                "Cannot assign " + disambiguatedValueType + " to variable of type " + declaredType + " at " + getCurrentContext()
                            );
                            if (throwOnError) {
                                throw error;
                            } else {
                                errorCollector.addError(error.getMessage());
                            }
                            return;
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e);
            }
        }
    }
    
    // Visit identifier node
    public void visit(IdentifierNode node) {
        // Get symbol from identifier
        Symbol symbol = node.getSymbol();
        if (symbol == null) {
            RuntimeException error = new RuntimeException(
                "Unresolved symbol: " + (node.name != null ? node.name : "unknown")
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Extract type from symbol
        Type type = extractTypeFromSymbol(symbol);
        // Note: IdentifierNode doesn't have a setType method, so we just store it in the symbol
        symbol.setType(type);
        
        // Ensure that symbol is set back to the node
        node.setSymbol(symbol);
    }
    
    // Visit type path expression node
    public void visit(TypePathExprNode node) {
        // TypePathExprNode is handled by extractTypeFromTypeNode when called by parent nodes
        // No need to recursively process children here as extractTypeFromTypePathExpr does the work
        // This method is kept for consistency with the visitor pattern
    }
    
    // Visit associated item node
    public void visit(AssoItemNode node) {
        // Either function or constant should be not null
        if (node.function != null) {
            node.function.accept(this);
        } else if (node.constant != null) {
            node.constant.accept(this);
        }
    }
    
    // Visit impl node
    public void visit(ImplNode node) {
        // Type and trait symbols are already set by SymbolAdder
        // Set Self type context for associated items (constants) that might use Self
        Type previousSelfType = getCurrentSelfType();
        
        try {
            // Extract the type from the impl node's type symbol
            Symbol typeSymbol = node.getTypeSymbol();
            if (typeSymbol != null) {
                Type implType = extractTypeFromSymbol(typeSymbol);
                // Set Self type for the impl block
                setCurrentType(implType);
            }
            
            // Visit items in impl block
            if (node.items != null) {
                for (AssoItemNode item : node.items) {
                    item.accept(this);
                }
            }
        } finally {
            // Restore previous Self type
            if (previousSelfType != null) {
                setCurrentType(previousSelfType);
            } else {
                clearCurrentType();
            }
        }
    }
    
    // Visit const item node
    public void visit(ConstItemNode node) throws RuntimeException {
        try {
            // Visit the value if it exists
            if (node.value != null) {
                // First, type-check the value expression
                node.value.accept(this);
                Type valueType = getType(node.value);
                
                // If there's an explicit type, check compatibility
                if (node.type != null) {
                    Type declaredType = extractTypeFromTypeNode(node.type);
                    // Disambiguate value type if needed
                    Type disambiguatedValueType = disambiguateType(valueType, declaredType);
                    if (disambiguatedValueType == null || declaredType == null || !isTypeCompatible(disambiguatedValueType, declaredType)) {
                        // Try to find a common type
                        Type commonType = findCommonType(disambiguatedValueType, declaredType);
                        if (commonType == null || !isTypeCompatible(commonType, declaredType)) {
                            RuntimeException error = new RuntimeException(
                                "Cannot assign " + disambiguatedValueType + " to constant of type " + declaredType + " at " + getCurrentContext()
                            );
                            if (throwOnError) {
                                throw error;
                            } else {
                                errorCollector.addError(error.getMessage());
                            }
                            return;
                        }
                    }
                }
                
                // Now check if the value is a constant expression
                ConstantValue constValue = constantEvaluator.evaluate(node.value);
                
                if (constValue == null) {
                    RuntimeException error = new RuntimeException(
                        "Constant value is not a constant expression"
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                // Get the type of the constant value
                Type constValueType = constValue.getType();
                
                // Verify that the evaluated constant type matches the type-checked type
                // Try to find a common type between the evaluated constant type and the type-checked value type
                Type commonType = findCommonType(constValueType, valueType);
                if (commonType == null || !isTypeCompatible(commonType, valueType)) {
                    RuntimeException error = new RuntimeException(
                        "Constant expression type mismatch: type-checked as " + valueType +
                        " but evaluated as " + constValueType
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                // Store the evaluated constant value in the symbol for future reference
                if (node.getSymbol() != null) {
                    node.getSymbol().setConstantValue(constValue);
                }
            }
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit struct node
    public void visit(StructNode node) {
        // Struct definitions don't need type checking themselves
        // The struct type is created when the symbol is processed
        // Just need to visit fields if they exist
        if (node.fields != null) {
            for (FieldNode field : node.fields) {
                field.accept(this);
            }
        }
    }
    
    // Visit enum node
    public void visit(EnumNode node) {
        // Enum definitions don't need type checking themselves
        // The enum type is created when the symbol is processed
        // Just need to visit variants if they exist
        if (node.variants != null) {
            for (IdentifierNode variant : node.variants) {
                variant.accept(this);
            }
        }
    }
    
    // Visit field node
    public void visit(FieldNode node) {
        // Field definitions don't need type checking themselves
        // The field type is processed when the struct type is created
        // Just need to visit the type if it exists
        if (node.type != null) {
            node.type.accept(this);
        }
    }
    
    // Visit trait node
    public void visit(TraitNode node) {
        // Trait definitions don't need type checking themselves
        // Just need to visit items if they exist
        if (node.items != null) {
            for (AssoItemNode item : node.items) {
                item.accept(this);
            }
        }
    }
    
    // Visit self parameter node
    public void visit(SelfParaNode node) {
        // Self parameter type is determined by the impl block
        // No additional type checking needed here
    }
    
    // Visit parameter node
    public void visit(ParameterNode node) {
        // Parameter type is already processed when function type is created
        // Just need to visit the pattern and type if they exist
        if (node.name != null) {
            node.name.accept(this);
        }
        if (node.type != null) {
            node.type.accept(this);
        }
    }
    
    // Visit pattern node
    public void visit(PatternNode node) {
        // Pattern node is abstract, actual implementation is in subclasses
        // This method should not be called directly
    }
    
    // Visit identifier pattern node
    public void visit(IdPatNode node) {
        // Identifier pattern type is determined by the context
        // No additional type checking needed here
    }
    
    // Visit wildcard pattern node
    public void visit(WildPatNode node) {
        // Wildcard pattern matches any type
        // No additional type checking needed here
    }
    
    // Visit reference pattern node
    public void visit(RefPatNode node) {
        // Reference pattern type is determined by the context
        // Just need to visit the inner pattern if it exists
        if (node.innerPattern != null) {
            node.innerPattern.accept(this);
        }
    }
    
    // Helper method to check if an expression contains a return statement (with memoization)
    private boolean containsReturnStatement(ASTNode node) {
        if (node == null) {
            return false;
        }
        
        // Check cache first
        Boolean cached = containsReturnCache.get(node);
        if (cached != null) {
            return cached;
        }
        
        boolean result;
        
        // If it's a return statement itself
        if (node instanceof ReturnExprNode) {
            result = true;
        }
        // If it's a block expression, check all statements
        else if (node instanceof BlockExprNode) {
            BlockExprNode block = (BlockExprNode) node;
            if (block.statements != null) {
                for (StmtNode stmt : block.statements) {
                    if (containsReturnStatement(stmt)) {
                        result = true;
                        // Cache and return
                        containsReturnCache.put(node, result);
                        return result;
                    }
                }
            }
            // Check return value expression
            if (block.returnValue != null && containsReturnStatement(block.returnValue)) {
                result = true;
            } else {
                result = false;
            }
        }
        // If it's an if expression, recursively check branches
        else if (node instanceof IfExprNode) {
            IfExprNode ifExpr = (IfExprNode) node;
            if (containsReturnStatement(ifExpr.thenBranch)) {
                result = true;
            } else if (ifExpr.elseifBranch != null && containsReturnStatement(ifExpr.elseifBranch)) {
                result = true;
            } else if (ifExpr.elseBranch != null && containsReturnStatement(ifExpr.elseBranch)) {
                result = true;
            } else {
                result = false;
            }
        }
        // If it's an expression statement, check the expression
        else if (node instanceof ExprStmtNode) {
            ExprStmtNode exprStmt = (ExprStmtNode) node;
            result = containsReturnStatement(exprStmt.expr);
        }
        // Other types don't contain return statements
        else {
            result = false;
        }
        
        // Cache the result
        containsReturnCache.put(node, result);
        return result;
    }
    
    // Visit expression statement node
    public void visit(ExprStmtNode node) {
        // Visit the expression if it exists
        if (node.expr != null) {
            // Visit expression
            node.expr.accept(this);
            node.expr.accept(this);
        }
    }
    
    // Visit type reference expression node
    public void visit(TypeRefExprNode node) {
        // Type reference expressions are processed in extractTypeFromTypeRefExpr
        // No additional type checking needed here
    }
    
    // Visit type array expression node
    public void visit(TypeArrayExprNode node) {
        // Type array expressions are processed in extractTypeFromTypeArrayExpr
        // Just need to visit the element type and size if they exist
        if (node.elementType != null) {
            node.elementType.accept(this);
        }
        if (node.size != null) {
            node.size.accept(this);
        }
    }
    
    // Visit type unit expression node
    public void visit(TypeUnitExprNode node) {
        // Unit type expressions are processed in extractTypeFromTypeNode
        // No additional type checking needed here
    }
    
    // Visit builtin function node
    public void visit(BuiltinFunctionNode node) {
        // Builtin functions are handled by the parent FunctionNode visit method
        // No additional type checking needed here
        super.visit(node);
    }
}