/**
 * Base class for expression type checkers
 * Provides common type checking functionality and error handling
 */
public abstract class BaseExpressionTypeChecker {
    protected final TypeErrorCollector errorCollector;
    protected final boolean throwOnError;
    protected final TypeExtractor typeExtractor;
    protected final ConstantEvaluator constantEvaluator;
    protected final ExpressionTypeContext context;
    protected final TypeCheckerRefactored mainExpressionChecker;
    protected MutabilityChecker mutabilityChecker;
    
    public BaseExpressionTypeChecker(
        TypeErrorCollector errorCollector,
        boolean throwOnError,
        TypeExtractor typeExtractor,
        ConstantEvaluator constantEvaluator,
        ExpressionTypeContext context,
        TypeCheckerRefactored mainExpressionChecker
    ) {
        this.errorCollector = errorCollector;
        this.throwOnError = throwOnError;
        this.typeExtractor = typeExtractor;
        this.constantEvaluator = constantEvaluator;
        this.context = context;
        this.mainExpressionChecker = mainExpressionChecker;
        this.mutabilityChecker = new MutabilityChecker(errorCollector, throwOnError);
    }
    
    /**
     * Get expression node's type with null check and error reporting
     */
    protected Type getType(ExprNode expr) {
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
    
    /**
     * Get expression node's type without null checks
     * This method is used in sub-TypeCheckers where we've already verified the expression is not null
     */
    protected Type getTypeWithoutNullCheck(ExprNode expr) {
        return expr.getType();
    }
    
    /**
     * Set expression node's type
     */
    protected void setType(ExprNode expr, Type type) {
        expr.setType(type);
    }
    
    /**
     * 确保符号的类型已设置（如果是PathExprNode）
     */
    protected void ensureSymbolType(ExprNode expr) {
        if (expr instanceof PathExprNode) {
            PathExprNode pathExpr = (PathExprNode) expr;
            if (pathExpr.getSymbol() != null && pathExpr.getSymbol().getType() == null) {
                typeExtractor.extractTypeFromSymbol(pathExpr.getSymbol());
            }
        }
    }
    
    /**
     * Get node description for error reporting
     */
    protected String getNodeDescription(ASTNode node) {
        if (node == null) {
            return "null node";
        }
        
        String className = node.getClass().getSimpleName();
        
        // 尝试根据节点类型获取更具体的信息
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
    
    /**
     * Get current context for error reporting
     */
    protected String getCurrentContext() {
        StringBuilder contextDescription = new StringBuilder(); // Renamed variable to avoid conflict with the field

        // 添加函数上下文（如果可用）
        if (this.context.getCurrentFunction() != null) { // Fixed: Use `this.context` to refer to the field
            FunctionNode funcNode = this.context.getCurrentFunction();
            if (funcNode.name != null) {
                contextDescription.append("in function '").append(funcNode.name.name).append("' ");
            }
        }

        // 添加当前Self类型（如果可用）
        if (this.context.getCurrentSelfType() != null) { // Fixed: Use `this.context` to refer to the field
            contextDescription.append("(Self: ").append(this.context.getCurrentSelfType()).append(") ");
        }

        return contextDescription.toString();
    }
    
    /**
     * Report error
     */
    protected void reportError(String message) {
        RuntimeException error = new RuntimeException(message);
        if (throwOnError) {
            throw error;
        } else {
            errorCollector.addError(error.getMessage());
        }
    }
    
    /**
     * Handle exception
     */
    protected void handleError(RuntimeException e) {
        if (throwOnError) {
            throw e;
        } else {
            errorCollector.addError(e.getMessage());
        }
    }
    
    /**
     * Check if expression is assignable (left value)
     */
    protected boolean isAssignable(ExprNode expr) {
        // 在Rust中，这四种类型的表达式可以是左值：
        // 1. PathExprNode - 路径表达式（变量、字段等）
        // 2. FieldExprNode - 字段访问表达式
        // 3. IndexExprNode - 索引访问表达式
        // 4. DerefExprNode - 解引用表达式
        return expr instanceof PathExprNode ||
               expr instanceof FieldExprNode ||
               expr instanceof IndexExprNode ||
               expr instanceof DerefExprNode;
    }
    
    /**
     * 递归访问表达式节点，使用主表达式检查器
     * 这确保了递归表达式使用正确的类型检查器
     */
    protected void visitExpression(ExprNode expr) {
        if (mainExpressionChecker != null) {
            expr.accept(mainExpressionChecker);
        } else {
            throw new RuntimeException("Main expression checker is not available for recursive call");
        }
    }
    
    /**
     * 检查可变访问
     */
    protected void checkMutableAccess(ExprNode expr) {
        if (mutabilityChecker != null) {
            mutabilityChecker.checkMutability(expr);
        }
    }
    
    /**
     * 检查可变赋值
     */
    protected void checkMutableAssignment(ExprNode target, ExprNode value) {
        if (mutabilityChecker != null) {
            mutabilityChecker.checkMutability(target);
            if (value != null) {
                mutabilityChecker.checkMutability(value);
            }
        }
    }
    
    /**
     * 设置mutability检查器
     */
    public void setMutabilityChecker(MutabilityChecker mutabilityChecker) {
        this.mutabilityChecker = mutabilityChecker;
    }
}