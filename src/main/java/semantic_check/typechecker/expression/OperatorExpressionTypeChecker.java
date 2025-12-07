/**
 * 运算符表达式类型检查器
 * 处理算术、比较、逻辑、赋值、复合赋值、取反等运算符表达式
 */
public class OperatorExpressionTypeChecker extends BaseExpressionTypeChecker {
    
    public OperatorExpressionTypeChecker(
        TypeErrorCollector errorCollector,
        boolean throwOnError,
        TypeExtractor typeExtractor,
        ConstantEvaluator constantEvaluator,
        ExpressionTypeContext context,
        TypeCheckerRefactored mainExpressionChecker
    ) {
        super(errorCollector, throwOnError, typeExtractor, constantEvaluator, context, mainExpressionChecker);
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
        // 这是一个抽象基类，在正确的访问者模式实现中不应该到达这里
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
            
            // 访问左右操作数，使用递归调用确保使用正确的类型检查器
            visitExpression(node.left);
            visitExpression(node.right);
            
            // 确保左操作数的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.left);
            
            // 确保右操作数的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.right);
            
            Type leftType = getTypeWithoutNullCheck(node.left);
            Type rightType = getTypeWithoutNullCheck(node.right);
            
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
            
            // 访问左右操作数，使用递归调用确保使用正确的类型检查器
            visitExpression(node.left);
            visitExpression(node.right);
            
            // 确保左操作数的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.left);
            
            // 确保右操作数的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.right);
            
            Type leftType = getTypeWithoutNullCheck(node.left);
            Type rightType = getTypeWithoutNullCheck(node.right);
            
            // 检查操作数是否可比较
            // 目前，我们允许比较任何两种相同类型的操作数
            // 在完整实现中，这会更严格
            if (leftType == null || rightType == null || !TypeUtils.isTypeCompatible(leftType, rightType)) {
                // 尝试查找公共类型
                Type commonType = TypeUtils.findCommonType(leftType, rightType);
                if (commonType == null) {
                    reportError("Cannot compare incompatible types: " + leftType + " and " + rightType + " at " + getCurrentContext());
                    return;
                }
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
            
            // 访问左右操作数，使用递归调用确保使用正确的类型检查器
            visitExpression(node.left);
            visitExpression(node.right);
            
            // 确保左操作数的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.left);
            
            // 确保右操作数的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.right);
            
            Type leftType = getTypeWithoutNullCheck(node.left);
            Type rightType = getTypeWithoutNullCheck(node.right);
            
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
            
            // 访问内部表达式，使用递归调用确保使用正确的类型检查器
            visitExpression(node.innerExpr);
            Type innerType = getTypeWithoutNullCheck(node.innerExpr);
            
            // 确保内部表达式的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.innerExpr);
            
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
            
            // 访问左右操作数，使用递归调用确保使用正确的类型检查器
            visitExpression(node.left);
            visitExpression(node.right);
            
            // 确保左操作数的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.left);
            
            // 确保右操作数的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.right);
            
            Type leftType = getTypeWithoutNullCheck(node.left);
            Type rightType = getTypeWithoutNullCheck(node.right);
            
            // 检查类型是否兼容
            if (leftType == null || rightType == null || !TypeUtils.isTypeCompatible(leftType, rightType)) {
                // 尝试查找公共类型
                Type commonType = TypeUtils.findCommonType(leftType, rightType);
                if (commonType == null || !TypeUtils.isTypeCompatible(commonType, leftType)) {
                    reportError("Cannot assign " + rightType + " to " + leftType + " at " + getCurrentContext());
                    return;
                }
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
            
            // 访问左右操作数，使用递归调用确保使用正确的类型检查器
            visitExpression(node.left);
            visitExpression(node.right);
            
            // 确保左操作数的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.left);
            
            // 确保右操作数的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.right);
            
            Type leftType = getTypeWithoutNullCheck(node.left);
            Type rightType = getTypeWithoutNullCheck(node.right);
            
            // 单独处理移位复合赋值和常规算术复合赋值
            if (TypeUtils.isShiftCompoundAssignment(node.operator)) {
                handleShiftCompoundAssignment(node, leftType, rightType);
            } else if (TypeUtils.isArithmeticCompoundAssignment(node.operator)) {
                handleArithmeticCompoundAssignment(node, leftType, rightType);
            }
            
            // 检查类型是否兼容
            if (leftType == null || rightType == null || !TypeUtils.isTypeCompatible(leftType, rightType)) {
                // 尝试查找公共类型
                Type commonType = TypeUtils.findCommonType(leftType, rightType);
                if (commonType == null || !TypeUtils.isTypeCompatible(commonType, leftType)) {
                    reportError("Cannot perform compound assignment with " + rightType + " and " + leftType + " at " + getCurrentContext());
                    return;
                }
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
    private void handleShiftCompoundAssignment(ComAssignExprNode node, Type leftType, Type rightType) {
        // 对于移位复合赋值：
        // 左操作数必须是整数类型
        // 右操作数必须是整数类型（可以是有符号或无符号）
        // 类型必须兼容（右操作数可以转换为左操作数类型）
        
        // 检查左操作数是否为整数类型
        if (!TypeUtils.isIntegerType(leftType)) {
            reportError("Left operand of shift compound assignment must be an integer type: " + leftType + " at " + getCurrentContext());
            return;
        }
        
        // 检查右操作数是否为整数类型（不仅仅是无符号）
        if (!TypeUtils.isIntegerType(rightType)) {
            reportError("Right operand of shift compound assignment must be an integer type: " + rightType + " at " + getCurrentContext());
            return;
        }
    }
    
    /**
     * 处理算术复合赋值
     */
    private void handleArithmeticCompoundAssignment(ComAssignExprNode node, Type leftType, Type rightType) {
        // 常规算术复合赋值
        // 检查两个操作数是否为数字
        if (!TypeUtils.isNumericType(leftType)) {
            reportError("Left operand of arithmetic compound assignment is not numeric: " + leftType + " at " + getCurrentContext());
            return;
        }
        
        if (!TypeUtils.isNumericType(rightType)) {
            reportError("Right operand of arithmetic compound assignment is not numeric: " + rightType + " at " + getCurrentContext());
            return;
        }
    }
}