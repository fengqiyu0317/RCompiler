/**
 * 运算符表达式类型检查器
 * 处理算术、比较、逻辑、赋值、复合赋值、取反等运算符表达式
 */
public class OperatorExpressionTypeChecker extends VisitorBase {
    protected final TypeErrorCollector errorCollector;
    protected final boolean throwOnError;
    protected final TypeExtractor typeExtractor;
    protected final ConstantEvaluator constantEvaluator;
    protected final ExpressionTypeContext context;
    protected final TypeChecker mainExpressionChecker;
    protected MutabilityChecker mutabilityChecker;
    
    public OperatorExpressionTypeChecker(
        TypeErrorCollector errorCollector,
        boolean throwOnError,
        TypeExtractor typeExtractor,
        ConstantEvaluator constantEvaluator,
        ExpressionTypeContext context,
        TypeChecker mainExpressionChecker
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
    
    /**
     * 访问基础表达式节点
     */
    public void visit(ExprNode node) {
        // 这是一个抽象基类，在正确的访问者模式实现中不应该到达这里
        throw new RuntimeException(
            "Cannot visit abstract ExprNode directly in OperatorExpressionTypeChecker"
        );
    }
    
    /**
     * 访问不带块的表达式基类
     */
    public void visit(ExprWithoutBlockNode node) {
        throw new RuntimeException(
            "Cannot visit abstract ExprWithoutBlockNode directly in OperatorExpressionTypeChecker"
        );
    }
    
    /**
     * 访问运算符表达式基类
     */
    public void visit(OperExprNode node) {
        // 这是一个抽象基类，在正确的访问者模式实现中不应该到达这里
        throw new RuntimeException(
            "Cannot visit abstract OperExprNode directly in OperatorExpressionTypeChecker"
        );
    }
    
    /**
     * 访问算术表达式
     */
    public void visit(ArithExprNode node) {
        try {
            if (node.left == null || node.right == null) {
                reportError("Arithmetic expression missing operands");
                return;
            }
            
            // 访问左右操作数，使用TypeChecker进行调用
            node.left.accept(mainExpressionChecker);
            node.right.accept(mainExpressionChecker);
            
            Type leftType = node.left.getType();
            Type rightType = node.right.getType();
            
            // 单独处理移位操作和常规算术操作
            if (TypeUtils.isShiftOperation(node.operator)) {
                handleShiftOperation(node, leftType, rightType);
            } else {
                handleRegularArithmeticOperation(node, leftType, rightType);
            }
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 处理移位操作
     */
    private void handleShiftOperation(ArithExprNode node, Type leftType, Type rightType) {
        // 对于移位操作：
        // 左操作数必须是整数类型
        // 右操作数必须是整数类型（可以是有符号或无符号）
        // 结果类型与左操作数类型相同
        
        // 检查左操作数是否为整数类型
        if (!TypeUtils.isIntegerType(leftType)) {
            reportError("Left operand of shift operation must be an integer type: " + leftType);
            return;
        }
        
        // 检查右操作数是否为整数类型（不仅仅是无符号）
        if (!TypeUtils.isIntegerType(rightType)) {
            reportError("Right operand of shift operation must be an integer type: " + rightType);
            return;
        }
        
        // 结果类型与左操作数类型相同
        setType(node, leftType);
    }
    
    /**
     * 处理常规算术操作
     */
    private void handleRegularArithmeticOperation(ArithExprNode node, Type leftType, Type rightType) {
        // 常规算术操作
        // 检查两个操作数是否为数字
        if (!TypeUtils.isNumericType(leftType)) {
            reportError("Left operand of arithmetic expression is not numeric: " + leftType);
            return;
        }
        
        if (!TypeUtils.isNumericType(rightType)) {
            reportError("Right operand of arithmetic expression is not numeric: " + rightType);
            return;
        }
        
        // 为结果查找公共类型
        Type resultType = TypeUtils.findCommonType(leftType, rightType);
        if (resultType == null) {
            reportError("Cannot find common type for arithmetic expression: " + leftType + " and " + rightType);
            return;
        }
        
        setType(node, resultType);
    }
    
    /**
     * 访问比较表达式
     */
    public void visit(CompExprNode node) {
        try {
            if (node.left == null || node.right == null) {
                reportError("Comparison expression missing operands");
                return;
            }
            
            // 访问左右操作数，使用TypeChecker进行调用
            node.left.accept(mainExpressionChecker);
            node.right.accept(mainExpressionChecker);
            
            Type leftType = node.left.getType();
            Type rightType = node.right.getType();
            
            // Check if both types are numeric for comparison
            // if it's <, >, <=, >=
            if (TypeUtils.isRelationalOperator(node.operator)) {
                if (!TypeUtils.isNumericType(leftType)) {
                    throw new RuntimeException("Left operand of comparison expression is not numeric: " + leftType);
                }
                
                if (!TypeUtils.isNumericType(rightType)) {
                    throw new RuntimeException("Right operand of comparison expression is not numeric: " + rightType);
                }
            }
            
            Type commonType = TypeUtils.findCommonType(leftType, rightType);
            if (commonType == null) {
                throw new RuntimeException("Type mismatch in comparison expression: " + leftType + " vs " + rightType);
            }
            
            // 比较表达式总是返回布尔值
            setType(node, PrimitiveType.getBoolType());
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问惰性逻辑表达式（&&, ||）
     */
    public void visit(LazyExprNode node) {
        try {
            if (node.left == null || node.right == null) {
                reportError("Logical expression missing operands");
                return;
            }
            
            // 访问左右操作数，使用TypeChecker进行调用
            node.left.accept(mainExpressionChecker);
            node.right.accept(mainExpressionChecker);
            
            Type leftType = node.left.getType();
            Type rightType = node.right.getType();
            
            // 检查两个操作数是否为布尔值
            if (!leftType.isBoolean()) {
                reportError("Left operand of logical expression is not boolean: " + leftType);
                return;
            }
            
            if (!rightType.isBoolean()) {
                reportError("Right operand of logical expression is not boolean: " + rightType);
                return;
            }
            
            // 逻辑表达式总是返回布尔值
            setType(node, PrimitiveType.getBoolType());
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问取反表达式（!, -）
     */
    public void visit(NegaExprNode node) {
        try {
            if (node.innerExpr == null) {
                reportError("Negation expression missing operand");
                return;
            }
            
            // 访问内部表达式，使用TypeChecker进行调用
            node.innerExpr.accept(mainExpressionChecker);
            Type innerType = node.innerExpr.getType();
            
            if (node.isLogical) {
                // 逻辑取反（!）需要布尔操作数
                if (!innerType.isBoolean()) {
                    reportError("Logical negation requires boolean operand: " + innerType);
                    return;
                }
                // 结果是布尔值
                setType(node, PrimitiveType.getBoolType());
            } else {
                // 算术取反（-）需要数字操作数
                if (!TypeUtils.isNumericType(innerType)) {
                    reportError("Arithmetic negation requires numeric operand: " + innerType);
                    return;
                }
                // 结果与操作数类型相同
                setType(node, innerType);
            }
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问赋值表达式
     */
    public void visit(AssignExprNode node) {
        try {
            if (node.left == null || node.right == null) {
                reportError("Assignment expression missing operands");
                return;
            }
            
            // 检查左侧是否可赋值
            if (!isAssignable(node.left)) {
                reportError("Left side of assignment is not assignable");
                return;
            }
            
            // 访问左右操作数，使用TypeChecker进行调用
            node.left.accept(mainExpressionChecker);
            node.right.accept(mainExpressionChecker);
            
            Type leftType = node.left.getType();
            Type rightType = node.right.getType();
            
            // 检查类型是否兼容
            if (!TypeUtils.isTypeCompatible(rightType, leftType)) {
                throw new RuntimeException("Cannot assign " + rightType + " to " + leftType + " at " + getCurrentContext());
            }
            
            // 赋值表达式返回单元类型
            setType(node, UnitType.INSTANCE);
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问复合赋值表达式（+=, -=, *=, /=, %=, &=, |=, ^=, <<=, >>=）
     */
    public void visit(ComAssignExprNode node) {
        try {
            if (node.left == null || node.right == null) {
                reportError("Compound assignment expression missing operands");
                return;
            }
            
            // 检查左侧是否可赋值
            if (!isAssignable(node.left)) {
                reportError("Left side of compound assignment is not assignable");
                return;
            }
            
            // 访问左右操作数，使用TypeChecker进行调用
            node.left.accept(mainExpressionChecker);
            node.right.accept(mainExpressionChecker);
            
            Type leftType = node.left.getType();
            Type rightType = node.right.getType();
            
            // 单独处理移位复合赋值和常规算术复合赋值
            Type resultType = null;
            if (TypeUtils.isShiftCompoundAssignment(node.operator)) {
                resultType = handleShiftCompoundAssignment(node, leftType, rightType);
            } else if (TypeUtils.isArithmeticCompoundAssignment(node.operator)) {
                resultType = handleArithmeticCompoundAssignment(node, leftType, rightType);
            }
            
            // 检查类型是否兼容
            if (!TypeUtils.isTypeCompatible(resultType, leftType)) {
                throw new RuntimeException("Cannot assign result type " + resultType + " to " + leftType + " in compound assignment at " + getCurrentContext());
            }
            
            // 复合赋值表达式返回单元类型
            setType(node, UnitType.INSTANCE);
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 处理移位复合赋值
     */
    private Type handleShiftCompoundAssignment(ComAssignExprNode node, Type leftType, Type rightType) {
        // 对于移位复合赋值：
        // 左操作数必须是整数类型
        // 右操作数必须是整数类型（可以是有符号或无符号）
        // 类型必须兼容（右操作数可以转换为左操作数类型）
        
        // 检查左操作数是否为整数类型
        if (!TypeUtils.isIntegerType(leftType)) {
            reportError("Left operand of shift compound assignment must be an integer type: " + leftType + " at " + getCurrentContext());
            return null;
        }
        
        // 检查右操作数是否为整数类型（不仅仅是无符号）
        if (!TypeUtils.isIntegerType(rightType)) {
            reportError("Right operand of shift compound assignment must be an integer type: " + rightType + " at " + getCurrentContext());
            return null;
        }

        return leftType;
    }
    
    /**
     * 处理算术复合赋值
     */
    private Type handleArithmeticCompoundAssignment(ComAssignExprNode node, Type leftType, Type rightType) {
        // 常规算术复合赋值
        // 检查两个操作数是否为数字
        if (!TypeUtils.isNumericType(leftType)) {
            reportError("Left operand of arithmetic compound assignment is not numeric: " + leftType + " at " + getCurrentContext());
            return null;
        }
        
        if (!TypeUtils.isNumericType(rightType)) {
            reportError("Right operand of arithmetic compound assignment is not numeric: " + rightType + " at " + getCurrentContext());
            return null;
        }

        // 为结果查找公共类型
        Type resultType = TypeUtils.findCommonType(leftType, rightType);
        if (resultType == null) {
            reportError("Cannot find common type for arithmetic compound assignment: " + leftType + " and " + rightType + " at " + getCurrentContext());
            return null;
        }

        return resultType;
    }
}