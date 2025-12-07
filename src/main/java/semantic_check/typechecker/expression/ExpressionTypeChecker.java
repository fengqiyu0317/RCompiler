import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class responsible for type checking expressions
 */
public class ExpressionTypeChecker extends VisitorBase {
    private final TypeErrorCollector errorCollector;
    private final boolean throwOnError;
    private final TypeExtractor typeExtractor;
    private final ConstantEvaluator constantEvaluator;
    private final ControlFlowTypeChecker controlFlowTypeChecker;
    
    // Current Self type
    private Type currentType;
    
    public ExpressionTypeChecker(TypeErrorCollector errorCollector, boolean throwOnError,
                             TypeExtractor typeExtractor, ConstantEvaluator constantEvaluator) {
        this.errorCollector = errorCollector;
        this.throwOnError = throwOnError;
        this.typeExtractor = typeExtractor;
        this.constantEvaluator = constantEvaluator;
        this.controlFlowTypeChecker = new ControlFlowTypeChecker(errorCollector, throwOnError,
                                                               typeExtractor, constantEvaluator, this);
        this.currentType = null;
    }
    
    // Get expression node's type with null check and error reporting
    public Type getType(ExprNode expr) {
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
    
    // Get current Self type
    public Type getCurrentSelfType() {
        return currentType;
    }
    
    // Set current Self type
    public void setCurrentType(Type type) {
        this.currentType = type;
    }
    
    // Clear current Self type
    public void clearCurrentType() {
        this.currentType = null;
    }
    
    // Enter function context
    public void enterFunctionContext(FunctionNode node) {
        controlFlowTypeChecker.enterFunctionContext(node);
    }
    
    // Exit function context
    public void exitFunctionContext() {
        controlFlowTypeChecker.exitCurrentContext();
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
        
        // Add current Self type if available
        if (currentType != null) {
            context.append("(Self: ").append(currentType).append(") ");
        }
        
        return context.toString();
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
    
    // Helper method to lookup builtin methods for String, str, array, u32 and usize types
    private Symbol lookupBuiltinMethod(String methodName, Type receiverType) {
        // Check if this is a valid builtin method for the given receiver type
        boolean isValidMethod = false;
        
        // Check String and str methods
        if (TypeUtils.isStringOrStrType(receiverType)) {
            isValidMethod = methodName.equals("to_string") ||
                         methodName.equals("as_str") ||
                         methodName.equals("as_mut_str") ||
                         methodName.equals("len") ||
                         methodName.equals("append");
        }
        // Check array methods
        else if (TypeUtils.isArrayType(receiverType)) {
            isValidMethod = methodName.equals("len");
        }
        // Check u32 and usize methods
        else if (TypeUtils.isU32OrUsizeType(receiverType)) {
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
    
    // Ensure that the symbol's type is set if it's a PathExprNode
    private void ensureSymbolType(ExprNode expr) {
        if (expr instanceof PathExprNode) {
            PathExprNode pathExpr = (PathExprNode) expr;
            if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                typeExtractor.extractTypeFromSymbol(pathExpr.getSymbol());
            }
        }
    }
    
    // Visit methods for base expression nodes
    
    // Visit base expression node
    public void visit(ExprNode node) {
        // This is an abstract base class, so we shouldn't reach here
        // in a proper visitor pattern implementation
        throw new RuntimeException(
            "Cannot visit abstract ExprNode directly"
        );
    }
    
    // Visit expression with block node
    public void visit(ExprWithBlockNode node) {
        // This is an abstract base class, so we shouldn't reach here
        // in a proper visitor pattern implementation
        throw new RuntimeException(
            "Cannot visit abstract ExprWithBlockNode directly"
        );
    }
    
    // Visit expression without block node
    public void visit(ExprWithoutBlockNode node) {
        // This is an abstract base class, so we shouldn't reach here
        // in a proper visitor pattern implementation
        if (node.innerExpr != null) {
            node.innerExpr.accept(this);
            Type innerType = getType(node.innerExpr);
            setType(node, innerType);
        } else {
            throw new RuntimeException(
                "Cannot visit abstract ExprWithoutBlockNode directly"
            );
        }
    }
    
    // Visit operator expression node
    public void visit(OperExprNode node) {
        // This is an abstract base class, so we shouldn't reach here
        // in a proper visitor pattern implementation
        throw new RuntimeException(
            "Cannot visit abstract OperExprNode directly"
        );
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
            Type type = typeExtractor.extractTypeFromSymbol(symbol);
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
                        Type lSegType = typeExtractor.extractTypeFromSymbol(lSegSymbol);
                        // If LSeg is a struct constructor type, RSeg might be a field or an implemented function
                        if (lSegType instanceof StructType) {
                            StructType structType = (StructType)lSegType;

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
                    Type rSegType = typeExtractor.extractTypeFromSymbol(rSegSymbolForType);
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
            Type type = typeExtractor.extractTypeFromSymbol(symbol);
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
            ensureSymbolType(node.left);
            
            // Ensure that the right operand's symbol is set if it's a PathExprNode
            ensureSymbolType(node.right);
            
            Type leftType = getType(node.left);
            Type rightType = getType(node.right);
            
            // Handle shift operations separately from regular arithmetic operations
            if (TypeUtils.isShiftOperation(node.operator)) {
                // For shift operations:
                // Left operand must be an integer type
                // Right operand must be an integer type (can be signed or unsigned)
                // Result type is the same as the left operand type
                
                // Check if left operand is an integer type
                if (!TypeUtils.isIntegerType(leftType)) {
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
                if (!TypeUtils.isIntegerType(rightType)) {
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
                if (!TypeUtils.isNumericType(leftType)) {
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
                
                if (!TypeUtils.isNumericType(rightType)) {
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
                Type resultType = TypeUtils.findCommonType(leftType, rightType);
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
            ensureSymbolType(node.left);
            
            // Ensure that the right operand's symbol is set if it's a PathExprNode
            ensureSymbolType(node.right);
            
            Type leftType = getType(node.left);
            Type rightType = getType(node.right);
            
            // Check if operands are comparable
            // For now, we'll allow comparison of any two types of the same kind
            // In a full implementation, this would be more restrictive
            if (leftType == null || rightType == null || !TypeUtils.isTypeCompatible(leftType, rightType)) {
                // Try to find a common type
                Type commonType = TypeUtils.findCommonType(leftType, rightType);
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
            ensureSymbolType(node.left);
            
            // Ensure that the right operand's symbol is set if it's a PathExprNode
            ensureSymbolType(node.right);
            
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
            ensureSymbolType(node.innerExpr);
            
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
                if (!TypeUtils.isNumericType(innerType)) {
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
        ensureSymbolType(node.left);
        
        // Ensure that the right operand's symbol is set if it's a PathExprNode
        ensureSymbolType(node.right);
        
        Type leftType = getType(node.left);
        Type rightType = getType(node.right);
        
        // Check if types are compatible
        if (leftType == null || rightType == null || !TypeUtils.isTypeCompatible(leftType, rightType)) {
            // Try to find a common type
            Type commonType = TypeUtils.findCommonType(leftType, rightType);
            if (commonType == null || !TypeUtils.isTypeCompatible(commonType, leftType)) {
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
            ensureSymbolType(node.left);
            
            // Ensure that the right operand's symbol is set if it's a PathExprNode
            ensureSymbolType(node.right);
            
            Type leftType = getType(node.left);
            Type rightType = getType(node.right);
            
            // Handle shift compound assignments separately from regular arithmetic compound assignments
            if (TypeUtils.isShiftCompoundAssignment(node.operator)) {
                // For shift compound assignments:
                // Left operand must be an integer type
                // Right operand must be an integer type (can be signed or unsigned)
                // Types must be compatible (right operand can be converted to left operand type)
                
                // Check if left operand is an integer type
                if (!TypeUtils.isIntegerType(leftType)) {
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
                if (!TypeUtils.isIntegerType(rightType)) {
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
            } else if (TypeUtils.isArithmeticCompoundAssignment(node.operator)) {
                // Regular arithmetic compound assignments
                // Check if both operands are numeric
                if (!TypeUtils.isNumericType(leftType)) {
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
                
                if (!TypeUtils.isNumericType(rightType)) {
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
            if (leftType == null || rightType == null || !TypeUtils.isTypeCompatible(leftType, rightType)) {
                // Try to find a common type
                Type commonType = TypeUtils.findCommonType(leftType, rightType);
                if (commonType == null || !TypeUtils.isTypeCompatible(commonType, leftType)) {
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
        ensureSymbolType(node.function);
        
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
                
                if (argType == null || expectedType == null || !TypeUtils.isTypeCompatible(argType, expectedType)) {
                    // Try to find a common type
                    Type commonType = TypeUtils.findCommonType(argType, expectedType);
                    if (commonType == null || !TypeUtils.isTypeCompatible(commonType, expectedType)) {
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
            else if (TypeUtils.isStringOrStrType(dereferencedType)) {
                // Look for builtin method
                methodSymbol = lookupBuiltinMethod(methodName, dereferencedType);
            }
            // Check if dereferenced receiver type is array type and look for builtin methods
            else if (TypeUtils.isArrayType(dereferencedType)) {
                // Look for builtin method
                methodSymbol = lookupBuiltinMethod(methodName, dereferencedType);
            }
            // Check if dereferenced receiver type is u32 or usize and look for builtin methods
            else if (TypeUtils.isU32OrUsizeType(dereferencedType)) {
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
                typeExtractor.extractTypeFromSymbol(methodSymbol);
            }
            
            // Extract method type
            Type methodType = typeExtractor.extractTypeFromSymbol(methodSymbol);
            
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
                    
                    if (argType == null || expectedType == null || !TypeUtils.isTypeCompatible(argType, expectedType)) {
                        // Try to find a common type
                        Type commonType = TypeUtils.findCommonType(argType, expectedType);
                        if (commonType == null || !TypeUtils.isTypeCompatible(commonType, expectedType)) {
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
            ensureSymbolType(node.array);
            
            // Visit index expression
            node.index.accept(this);
            Type indexType = getType(node.index);
            
            // Ensure that the index's symbol is set if it's a PathExprNode
            ensureSymbolType(node.index);
            
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
            if (!TypeUtils.isNumericType(indexType)) {
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
            ensureSymbolType(node.innerExpr);
            
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
            ensureSymbolType(node.innerExpr);
            
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
                errorCollector.addError(e.getMessage());
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
        
        // Ensure that expression's symbol is set if it's a PathExprNode
        ensureSymbolType(node.expr);
        
        // Extract target type
        Type targetType = typeExtractor.extractTypeFromTypeNode(node.type);
        
        // Check if cast is valid
        if (!TypeUtils.isValidCast(exprType, targetType)) {
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
                    
                    // Ensure that element's symbol is set if it's a PathExprNode
                    ensureSymbolType(element);
                    
                    if (elementType == null) {
                        elementType = elemType;
                    } else {
                        // Find common type between elements
                        elementType = TypeUtils.findCommonType(elementType, elemType);
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
                
                // Ensure that repeated element's symbol is set if it's a PathExprNode
                ensureSymbolType(node.repeatedElement);
                
                // First, type-check size expression
                node.size.accept(this);
                Type sizeType = getType(node.size);
                
                // Check if size expression is of numeric type
                if (!TypeUtils.isNumericType(sizeType)) {
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
            Type structType = typeExtractor.extractTypeFromSymbol(structSymbol);
            
            // Ensure that symbol is set back to structName node
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
                    
                    // Check if field value type matches expected field type
                    if (valueType == null || fieldType == null || !TypeUtils.isTypeCompatible(valueType, fieldType)) {
                        // Try to find a common type
                        Type commonType = TypeUtils.findCommonType(valueType, fieldType);
                        if (commonType == null || !TypeUtils.isTypeCompatible(commonType, fieldType)) {
                            RuntimeException error = new RuntimeException(
                                "Field '" + fieldName + "' type mismatch: expected " + fieldType + ", got " + valueType + " at " + getCurrentContext()
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
    
}