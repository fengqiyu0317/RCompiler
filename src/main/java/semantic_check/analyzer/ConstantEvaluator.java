// Constant expression evaluator for evaluating constant expressions at compile time
// This is essential for array type size evaluation and other constant contexts

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class ConstantEvaluator extends VisitorBase {
    private final TypeErrorCollector errorCollector;
    private final boolean throwOnError;
    
    // Store evaluated constant values
    private Map<ExprNode, ConstantValue> evaluatedValues = new HashMap<>();
    
    // Constructor
    public ConstantEvaluator(boolean throwOnError) {
        this.errorCollector = new TypeErrorCollector();
        this.throwOnError = throwOnError;
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
    
    // Evaluate an expression to a constant value
    public ConstantValue evaluate(ExprNode expr) {
        // Check if already evaluated
        if (evaluatedValues.containsKey(expr)) {
            return evaluatedValues.get(expr);
        }
        
        // Check if the expression already has type information from TypeChecker
        Type exprType = null;
        try {
            exprType = expr.getType();
        } catch (RuntimeException e) {
            // Type not set yet; fall back to evaluation without type info.
            if (!"Type of expression node is not set yet.".equals(e.getMessage())) {
                throw e;
            }
        }
        if (exprType == null) {
            // If no type information is available, evaluate the expression
            expr.accept(this);
        } else {
            // If type information is available, use it to guide evaluation
            evaluateWithTypeInfo(expr, exprType);
        }
        
        // Return the evaluated value
        return evaluatedValues.get(expr);
    }
    
    // Helper method to evaluate expression with type information
    private void evaluateWithTypeInfo(ExprNode expr, Type type) {
        // Use the type information to guide evaluation
        // This allows us to skip redundant type checking in the evaluator
        if (expr instanceof LiteralExprNode) {
            visit((LiteralExprNode) expr);
        } else if (expr instanceof PathExprNode) {
            visit((PathExprNode) expr);
        } else if (expr instanceof GroupExprNode) {
            visit((GroupExprNode) expr);
        } else if (expr instanceof ArithExprNode) {
            visit((ArithExprNode) expr);
        } else if (expr instanceof NegaExprNode) {
            visit((NegaExprNode) expr);
        } else if (expr instanceof CompExprNode) {
            visit((CompExprNode) expr);
        } else if (expr instanceof LazyExprNode) {
            visit((LazyExprNode) expr);
        } else if (expr instanceof TypeCastExprNode) {
            visit((TypeCastExprNode) expr);
        } else if (expr instanceof ArrayExprNode) {
            visit((ArrayExprNode) expr);
        } else {
            // For other expression types, fall back to regular evaluation
            expr.accept(this);
        }
    }
    
    // Helper method to store evaluated value
    private void setEvaluatedValue(ExprNode expr, ConstantValue value) {
        evaluatedValues.put(expr, value);
    }
    
    // Visit literal expression
    public void visit(LiteralExprNode node) {
        ConstantValue value;
        
        switch (node.literalType) {
            case I32:
                value = new ConstantValue(node.value_long, PrimitiveType.getI32Type());
                break;
            case U32:
                value = new ConstantValue(node.value_long, PrimitiveType.getU32Type());
                break;
            case USIZE:
                value = new ConstantValue(node.value_long, PrimitiveType.getUsizeType());
                break;
            case ISIZE:
                value = new ConstantValue(node.value_long, PrimitiveType.getIsizeType());
                break;
            case BOOL:
                value = new ConstantValue(node.value_bool, PrimitiveType.getBoolType());
                break;
            case CHAR:
                value = new ConstantValue(node.value_string, PrimitiveType.getCharType());
                break;
            case STRING:
                value = new ConstantValue(node.value_string, PrimitiveType.getStringType());
                break;
            case CSTRING:
                value = new ConstantValue(node.value_string, PrimitiveType.getStrType());
                break;
            default:
                // Default to undetermined integer type
                value = new ConstantValue(node.value_long, PrimitiveType.getIntType());
                break;
        }
        
        setEvaluatedValue(node, value);
    }
    
    // Visit path expression (identifier)
    public void visit(PathExprNode node) {
        // Get symbol from the path expression
        Symbol symbol = node.getSymbol();
        if (symbol == null) {
            RuntimeException error = new RuntimeException(
                "Unresolved symbol in constant expression: " + 
                (node.LSeg != null && node.LSeg.name != null ? node.LSeg.name.name : "unknown")
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Check if it's a constant
        if (symbol.getKind() != SymbolKind.CONSTANT) {
            RuntimeException error = new RuntimeException(
                "Non-constant symbol used in constant expression: " + symbol.getName()
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Get the constant declaration
        ASTNode declaration = symbol.getDeclaration();
        if (declaration instanceof ConstItemNode) {
            ConstItemNode constNode = (ConstItemNode) declaration;
            
            // If the constant has a value, evaluate it
            if (constNode.value != null) {
                ConstantValue value = evaluate(constNode.value);
                setEvaluatedValue(node, value);
            } else {
                RuntimeException error = new RuntimeException(
                    "Constant '" + symbol.getName() + "' has no value"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
            }
        } else {
            RuntimeException error = new RuntimeException(
                "Invalid constant declaration for symbol: " + symbol.getName()
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
        }
    }
    
    // Visit grouped expression
    public void visit(GroupExprNode node) {
        if (node.innerExpr != null) {
            ConstantValue value = evaluate(node.innerExpr);
            setEvaluatedValue(node, value);
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
    }
    
    // Visit arithmetic expression
    public void visit(ArithExprNode node) {
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
        
        // Evaluate left and right operands
        ConstantValue leftValue = evaluate(node.left);
        ConstantValue rightValue = evaluate(node.right);
        
        // Check if both operands are constant
        if (leftValue == null || rightValue == null) {
            RuntimeException error = new RuntimeException(
                "Non-constant operand in arithmetic expression"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Perform the operation
        ConstantValue result = evaluateArithmeticOperation(node.operator, leftValue, rightValue);
        if (result != null) {
            setEvaluatedValue(node, result);
        }
    }
    
    // Visit negation expression
    public void visit(NegaExprNode node) {
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
        
        // Evaluate inner expression
        ConstantValue value = evaluate(node.innerExpr);
        
        // Check if operand is constant
        if (value == null) {
            RuntimeException error = new RuntimeException(
                "Non-constant operand in negation expression"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Perform the negation
        ConstantValue result = evaluateNegation(node.isLogical, value);
        if (result != null) {
            setEvaluatedValue(node, result);
        }
    }
    
    // Visit comparison expression
    public void visit(CompExprNode node) {
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
        
        // Evaluate left and right operands
        ConstantValue leftValue = evaluate(node.left);
        ConstantValue rightValue = evaluate(node.right);
        
        // Check if both operands are constant
        if (leftValue == null || rightValue == null) {
            RuntimeException error = new RuntimeException(
                "Non-constant operand in comparison expression"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Perform the comparison
        ConstantValue result = evaluateComparison(node.operator, leftValue, rightValue);
        if (result != null) {
            setEvaluatedValue(node, result);
        }
    }
    
    // Visit lazy logical expression (&&, ||)
    public void visit(LazyExprNode node) {
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
        
        // Evaluate left and right operands
        ConstantValue leftValue = evaluate(node.left);
        ConstantValue rightValue = evaluate(node.right);
        
        // Check if both operands are constant
        if (leftValue == null || rightValue == null) {
            RuntimeException error = new RuntimeException(
                "Non-constant operand in logical expression"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Type checking is already done by TypeChecker, so we can skip redundant checks
        // Just perform the logical operation
        boolean leftBool = (Boolean) leftValue.getValue();
        boolean rightBool = (Boolean) rightValue.getValue();
        boolean result;
        
        if (node.operator == oper_t.LOGICAL_AND) {
            result = leftBool && rightBool;
        } else if (node.operator == oper_t.LOGICAL_OR) {
            result = leftBool || rightBool;
        } else {
            RuntimeException error = new RuntimeException(
                "Unsupported logical operator: " + node.operator
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        setEvaluatedValue(node, new ConstantValue(result, PrimitiveType.getBoolType()));
    }
    
    // Visit type cast expression
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
        
        // Evaluate the expression
        ConstantValue value = evaluate(node.expr);
        
        // Check if operand is constant
        if (value == null) {
            RuntimeException error = new RuntimeException(
                "Non-constant operand in type cast expression"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Extract target type
        Type targetType = extractTypeFromTypeNode(node.type);
        if (targetType == null) {
            RuntimeException error = new RuntimeException(
                "Invalid target type in type cast expression"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Perform the cast
        ConstantValue result = evaluateCast(value, targetType);
        if (result != null) {
            setEvaluatedValue(node, result);
        }
    }
    
    // Helper method to extract type from type node
    private Type extractTypeFromTypeNode(TypeExprNode typeNode) {
        // This is a simplified version of the method in TypeChecker
        // In a full implementation, this would be more comprehensive
        if (typeNode instanceof TypePathExprNode) {
            TypePathExprNode pathExpr = (TypePathExprNode) typeNode;
            if (pathExpr.path == null || pathExpr.path.name == null) {
                return null;
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
        }
        
        return null;
    }
    
    // Visit array expression
    public void visit(ArrayExprNode node) {
        if (node.elements != null && !node.elements.isEmpty()) {
            // Array with explicit elements
            List<ConstantValue> evaluatedElements = new ArrayList<>();
            Type elementType = null;
            
            // Evaluate all elements
            for (ExprNode element : node.elements) {
                ConstantValue elementValue = evaluate(element);
                
                if (elementValue == null) {
                    RuntimeException error = new RuntimeException(
                        "Non-constant element in array expression"
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                
                evaluatedElements.add(elementValue);
                
                // Determine element type from first element
                if (elementType == null) {
                    elementType = elementValue.getType();
                }
            }
            
            // Create array type with determined element type and size
            if (elementType != null) {
                ArrayType arrayType = new ArrayType(elementType, node.elements.size());
                setEvaluatedValue(node, new ConstantValue(evaluatedElements, arrayType));
            } else {
                RuntimeException error = new RuntimeException(
                    "Cannot determine element type for empty array"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
            }
            
        } else if (node.repeatedElement != null && node.size != null) {
            // Array with repeated element [expr; size]
            ConstantValue elementValue = evaluate(node.repeatedElement);
            
            if (elementValue == null) {
                RuntimeException error = new RuntimeException(
                    "Non-constant repeated element in array expression"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return;
            }
            
            // Evaluate size expression
            ConstantValue sizeValue = evaluate(node.size);
            
            if (sizeValue == null) {
                RuntimeException error = new RuntimeException(
                    "Non-constant size in array expression"
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
            
            // Create array with repeated elements
            List<ConstantValue> evaluatedElements = new ArrayList<>();
            for (int i = 0; i < arraySize; i++) {
                evaluatedElements.add(elementValue);
            }
            
            // Create array type with repeated element type and evaluated size
            ArrayType arrayType = new ArrayType(elementValue.getType(), arraySize);
            setEvaluatedValue(node, new ConstantValue(evaluatedElements, arrayType));
            
        } else {
            // Empty array []
            RuntimeException error = new RuntimeException(
                "Empty array requires type annotation"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
        }
    }
    
    // Helper method to evaluate arithmetic operations
    private ConstantValue evaluateArithmeticOperation(oper_t operator, ConstantValue left, ConstantValue right) {
        // Type checking is already done by TypeChecker, so we can skip redundant checks
        // Just check if both operands are constant
        if (left == null || right == null) {
            RuntimeException error = new RuntimeException(
                "Non-constant operand in arithmetic operation"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return null;
        }
        
        // Get the values
        long leftLong = ((Number) left.getValue()).longValue();
        long rightLong = ((Number) right.getValue()).longValue();
        
        // Perform the operation
        long result;
        switch (operator) {
            case PLUS:
                result = leftLong + rightLong;
                break;
            case MINUS:
                result = leftLong - rightLong;
                break;
            case MUL:
                result = leftLong * rightLong;
                break;
            case DIV:
                if (rightLong == 0) {
                    RuntimeException error = new RuntimeException(
                        "Division by zero in constant expression"
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return null;
                }
                result = leftLong / rightLong;
                break;
            case MOD:
                if (rightLong == 0) {
                    RuntimeException error = new RuntimeException(
                        "Modulo by zero in constant expression"
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return null;
                }
                result = leftLong % rightLong;
                break;
            case AND:
                result = leftLong & rightLong;
                break;
            case OR:
                result = leftLong | rightLong;
                break;
            case XOR:
                result = leftLong ^ rightLong;
                break;
            case SHL:
                result = leftLong << rightLong;
                break;
            case SHR:
                result = leftLong >> rightLong;
                break;
            default:
                RuntimeException error = new RuntimeException(
                    "Unsupported arithmetic operator: " + operator
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
                return null;
        }
        
        // Determine the result type
        Type resultType = findCommonType(left.getType(), right.getType());
        if (resultType == null) {
            resultType = PrimitiveType.getIntType(); // Default to int
        }
        
        return new ConstantValue(result, resultType);
    }
    
    // Helper method to evaluate negation
    private ConstantValue evaluateNegation(boolean isLogical, ConstantValue value) {
        // Type checking is already done by TypeChecker, so we can skip redundant checks
        // Just check if operand is constant
        if (value == null) {
            RuntimeException error = new RuntimeException(
                "Non-constant operand in negation expression"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return null;
        }
        
        if (isLogical) {
            // Logical negation (!)
            boolean boolValue = (Boolean) value.getValue();
            return new ConstantValue(!boolValue, PrimitiveType.getBoolType());
        } else {
            // Arithmetic negation (-)
            long numValue = ((Number) value.getValue()).longValue();
            return new ConstantValue(-numValue, value.getType());
        }
    }
    
    // Helper method to evaluate comparison
    private ConstantValue evaluateComparison(oper_t operator, ConstantValue left, ConstantValue right) {
        // Type checking is already done by TypeChecker, so we can skip redundant checks
        // Just check if both operands are constant
        if (left == null || right == null) {
            RuntimeException error = new RuntimeException(
                "Non-constant operand in comparison expression"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return null;
        }
        
        // Get the values
        Object leftValue = left.getValue();
        Object rightValue = right.getValue();
        
        // Perform the comparison
        boolean result;
        if (leftValue instanceof Number && rightValue instanceof Number) {
            long leftLong = ((Number) leftValue).longValue();
            long rightLong = ((Number) rightValue).longValue();
            
            switch (operator) {
                case EQ:
                    result = leftLong == rightLong;
                    break;
                case NEQ:
                    result = leftLong != rightLong;
                    break;
                case GT:
                    result = leftLong > rightLong;
                    break;
                case LT:
                    result = leftLong < rightLong;
                    break;
                case GTE:
                    result = leftLong >= rightLong;
                    break;
                case LTE:
                    result = leftLong <= rightLong;
                    break;
                default:
                    RuntimeException error = new RuntimeException(
                        "Unsupported comparison operator: " + operator
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return null;
            }
        } else if (leftValue instanceof Boolean && rightValue instanceof Boolean) {
            boolean leftBool = (Boolean) leftValue;
            boolean rightBool = (Boolean) rightValue;
            
            switch (operator) {
                case EQ:
                    result = leftBool == rightBool;
                    break;
                case NEQ:
                    result = leftBool != rightBool;
                    break;
                default:
                    RuntimeException error = new RuntimeException(
                        "Invalid comparison operator for boolean values: " + operator
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return null;
            }
        } else if (leftValue instanceof String && rightValue instanceof String) {
            String leftStr = (String) leftValue;
            String rightStr = (String) rightValue;
            
            switch (operator) {
                case EQ:
                    if (leftStr == null || rightStr == null) {
                        result = leftStr == rightStr;
                    } else {
                        result = leftStr.equals(rightStr);
                    }
                    break;
                case NEQ:
                    if (leftStr == null || rightStr == null) {
                        result = leftStr != rightStr;
                    } else {
                        result = !leftStr.equals(rightStr);
                    }
                    break;
                default:
                    RuntimeException error = new RuntimeException(
                        "Invalid comparison operator for string values: " + operator
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return null;
            }
        } else {
            RuntimeException error = new RuntimeException(
                "Unsupported comparison for values of type: " + left.getType()
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return null;
        }
        
        return new ConstantValue(result, PrimitiveType.getBoolType());
    }
    
    // Helper method to evaluate type cast
    private ConstantValue evaluateCast(ConstantValue value, Type targetType) {
        Object sourceValue = value.getValue();
        Type sourceType = value.getType();
        
        // If types are the same, return the value as is
        if (sourceType.equals(targetType)) {
            return value;
        }
        
        // Handle numeric casts
        if (sourceType.isNumeric() && targetType.isNumeric()) {
            long sourceLong = ((Number) sourceValue).longValue();
            return new ConstantValue(sourceLong, targetType);
        }
        
        // Handle other casts
        RuntimeException error = new RuntimeException(
            "Unsupported cast from " + sourceType + " to " + targetType
        );
        if (throwOnError) {
            throw error;
        } else {
            errorCollector.addError(error.getMessage());
        }
        return null;
    }
    
    // Helper method to find common type between two types
    private Type findCommonType(Type type1, Type type2) {
        // If two types are the same, return directly
        if (type1 == null || type2 == null) {
            return null;
        }
        if (type1.equals(type2)) {
            return type1;
        }
        
        // If both are numeric types, check if one is int (undetermined)
        if (type1 instanceof PrimitiveType && type2 instanceof PrimitiveType) {
            PrimitiveType.PrimitiveKind kind1 = ((PrimitiveType)type1).getKind();
            PrimitiveType.PrimitiveKind kind2 = ((PrimitiveType)type2).getKind();
            
            // Check if both are numeric
            if (type1.isNumeric() && type2.isNumeric()) {
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
}
