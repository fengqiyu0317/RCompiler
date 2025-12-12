import java.util.Stack;

/**
 * 控制流类型检查器
 * 负责处理控制流表达式的类型检查，如if、loop、break、continue、return等
 */
public class ControlFlowTypeChecker extends VisitorBase {
    private final TypeErrorCollector errorCollector;
    private final boolean throwOnError;
    
    // 联动的组件
    private final TypeExtractor typeExtractor;
    private final ConstantEvaluator constantEvaluator;
    private final TypeChecker typeChecker;
    
    // 控制流上下文栈
    private Stack<ControlFlowContext> contextStack = new Stack<>();
    
    /**
     * 控制流上下文类型
     */
    public enum ControlFlowContextType {
        LOOP,      // 循环上下文（loop, while）
        FUNCTION    // 函数上下文
    }
    
    /**
     * 控制流上下文
     */
    public static class ControlFlowContext {
        private final ControlFlowContextType type;
        private final ASTNode node;
        private java.util.List<Type> breakTypes = new java.util.ArrayList<>();
        
        public ControlFlowContext(ControlFlowContextType type, ASTNode node) {
            this.type = type;
            this.node = node;
        }
        
        public ControlFlowContextType getType() {
            return type;
        }
        
        public ASTNode getNode() {
            return node;
        }
        
        public java.util.List<Type> getBreakTypes() {
            return breakTypes;
        }
        
        public void addBreakType(Type breakType) {
            breakTypes.add(breakType);
        }
    }
    
    
    public ControlFlowTypeChecker(TypeErrorCollector errorCollector, boolean throwOnError,
                                 TypeExtractor typeExtractor, ConstantEvaluator constantEvaluator,
                                 TypeChecker typeChecker) {
        this.errorCollector = errorCollector;
        this.throwOnError = throwOnError;
        this.typeExtractor = typeExtractor;
        this.constantEvaluator = constantEvaluator;
        this.typeChecker = typeChecker;
    }
    
    // 为了向后兼容，保留旧的构造函数
    public ControlFlowTypeChecker(TypeErrorCollector errorCollector, boolean throwOnError) {
        this.errorCollector = errorCollector;
        this.throwOnError = throwOnError;
        this.typeExtractor = null;
        this.constantEvaluator = null;
        this.typeChecker = null;
    }
    
    /**
     * 进入循环上下文
     */
    public void enterLoopContext(LoopExprNode node) {
        contextStack.push(new ControlFlowContext(ControlFlowContextType.LOOP, node));
    }
    
    /**
     * 进入函数上下文
     */
    public void enterFunctionContext(FunctionNode node) {
        contextStack.push(new ControlFlowContext(ControlFlowContextType.FUNCTION, node));
    }
    
    
    /**
     * 退出当前上下文
     */
    public void exitCurrentContext() {
        if (!contextStack.isEmpty()) {
            contextStack.pop();
        }
    }
    
    /**
     * 查找最近的循环上下文
     * 只有上一层恰好是loop context才行，否则就要抛出错误
     */
    public ControlFlowContext findNearestLoopContext() {
        if (contextStack.isEmpty()) {
            return null;
        }
        
        // 检查上一层是否恰好是loop context
        ControlFlowContext immediateParent = contextStack.peek();
        if (immediateParent.getType() == ControlFlowContextType.LOOP) {
            return immediateParent;
        }
        
        // 如果上一层不是loop context，则返回null，调用者将抛出错误
        return null;
    }
    
    /**
     * 查找最近的函数上下文
     * 遍历上下文栈直到找到function context为止
     */
    public ControlFlowContext findNearestFunctionContext() {
        if (contextStack.isEmpty()) {
            return null;
        }
        
        // 遍历上下文栈直到找到function context
        for (int i = contextStack.size() - 1; i >= 0; i--) {
            ControlFlowContext context = contextStack.get(i);
            if (context.getType() == ControlFlowContextType.FUNCTION) {
                return context;
            }
        }
        
        // 如果没有找到function context，返回null，调用者将抛出错误
        return null;
    }
    
    /**
     * 获取当前上下文
     */
    public ControlFlowContext getCurrentContext() {
        return contextStack.isEmpty() ? null : contextStack.peek();
    }
    
    // Visitor methods for base expression nodes
    
    @Override
    public void visit(ExprNode node) {
        // 这是一个抽象基类，在正确的访问者模式实现中不应该到达这里
        throw new RuntimeException(
            "Cannot visit abstract ExprNode directly in ControlFlowTypeChecker"
        );
    }
    
    @Override
    public void visit(ExprWithBlockNode node) {
        // 这是一个抽象基类，在正确的访问者模式实现中不应该到达这里
        throw new RuntimeException(
            "Cannot visit abstract ExprWithBlockNode directly in ControlFlowTypeChecker"
        );
    }
    
    @Override
    public void visit(ExprWithoutBlockNode node) {
        // 这是一个抽象基类，在正确的访问者模式实现中不应该到达这里
        throw new RuntimeException(
            "Cannot visit abstract ExprWithoutBlockNode directly in ControlFlowTypeChecker"
        );
    }
    
    // Visitor methods for control flow nodes
    
    @Override
    public void visit(IfExprNode node) {
        // 访问条件表达式
        if (node.condition != null) {
            // 条件必须是布尔类型
            // 检查条件表达式类型
            try {
                // 使用表达式类型检查器检查条件表达式
                if (typeChecker != null) {
                    node.condition.accept(typeChecker);
                    Type conditionType = node.condition.getType();
                    
                    // 检查条件是否为布尔类型
                    if (!conditionType.isBoolean()) {
                        SemanticException error = new SemanticException(
                            "If condition must be boolean: " + conditionType, node
                        );
                        if (throwOnError) {
                            throw error;
                        } else {
                            errorCollector.addError(error.getMessage());
                        }
                        return;
                    }
                } else {
                    throw new RuntimeException("No expression type checker is available");
                }
            } catch (RuntimeException e) {
                if (throwOnError) {
                    throw e;
                } else {
                    errorCollector.addError(e.getMessage());
                }
                return;
            }
        }
        
        // 分析then分支
        Type thenBranchType = null;
        if (node.thenBranch != null) {
            node.thenBranch.accept(this);
            thenBranchType = node.thenBranch.getType();
        }
        
        // 分析elseif分支（如果存在）
        Type elseifBranchType = null;
        if (node.elseifBranch != null) {
            node.elseifBranch.accept(this);
            elseifBranchType = node.elseifBranch.getType();
        }
        
        // 分析else分支（如果存在）
        Type elseBranchType = null;
        if (node.elseBranch != null) {
            node.elseBranch.accept(this);
            elseBranchType = node.elseBranch.getType();
        }
        
        // 确定if表达式的类型
        Type ifType = determineIfExpressionType(
            thenBranchType, elseifBranchType, elseBranchType, node.elseBranch != null, node
        );
        
        // 设置if表达式的类型
        Type mutableType = TypeUtils.createMutableType(ifType, true);
        node.setType(mutableType);
    }
    
    /**
     * 确定if表达式的类型
     */
    private Type determineIfExpressionType(
        Type thenBranchType, Type elseifBranchType, Type elseBranchType, boolean hasElseBranch, ASTNode node
    ) {
        // 收集所有分支类型
        java.util.List<Type> allTypes = new java.util.ArrayList<>();
        
        if (thenBranchType != null) {
            allTypes.add(thenBranchType);
        }
        
        if (elseifBranchType != null) {
            allTypes.add(elseifBranchType);
        }
        
        if (elseBranchType != null) {
            allTypes.add(elseBranchType);
        }
        
        // 如果没有分支类型，返回UnitType
        if (allTypes.isEmpty()) {
            return UnitType.INSTANCE;
        }
        
        // 合并所有分支类型（包括NeverType）
        Type commonType = allTypes.get(0);
        for (int i = 1; i < allTypes.size(); i++) {
            commonType = TypeUtils.findCommonType(commonType, allTypes.get(i));
            if (commonType == null) {
                // 类型不兼容，抛出错误
                SemanticException error = new SemanticException(
                    "Incompatible types in if expression branches", node
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                    return NeverType.INSTANCE;
                }
            }
        }

        if (commonType instanceof NeverType && elseBranchType == null && elseifBranchType == null) {
            // 所有分支都是NeverType且没有else分支，返回UnitType
            return UnitType.INSTANCE;
        }
        
        return commonType;
    }
    
    /**
     * 确定循环表达式的类型
     */
    private Type determineLoopType(java.util.List<Type> breakTypes, ASTNode node) {
        // 如果没有break类型，返回NeverType（无限循环）
        if (breakTypes.isEmpty()) {
            return NeverType.INSTANCE;
        }
        
        // 合并所有break类型（包括NeverType）
        Type commonType = breakTypes.get(0);
        for (int i = 1; i < breakTypes.size(); i++) {
            commonType = TypeUtils.findCommonType(commonType, breakTypes.get(i));
            if (commonType == null) {
                // 类型不兼容，抛出错误
                SemanticException error = new SemanticException(
                    "Incompatible break types in loop", node
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                    return NeverType.INSTANCE;
                }
            }
        }
        
        return commonType;
    }
    
    @Override
    public void visit(BlockExprNode node) {
        if (node.statements != null) {
            // 遍历块中的所有语句
            for (int i = 0; i < node.statements.size(); i++) {
                ASTNode stmt = node.statements.get(i);

                // 使用typeChecker访问语句
                if (typeChecker != null) {
                    stmt.accept(typeChecker);
                    if (stmt instanceof ExprStmtNode) {
                        ExprStmtNode exprStmt = (ExprStmtNode) stmt;
                        if (!exprStmt.hasSemicolon && !(exprStmt.expr.getType() instanceof UnitType) && !(exprStmt.expr.getType() instanceof NeverType)) {
                            // 抛出错误
                            SemanticException error = new SemanticException(
                                "Expression statements in blocks must end with a semicolon unless they are of unit type",
                                exprStmt
                            );
                            if (throwOnError) {
                                throw error;
                            } else {
                                errorCollector.addError(error.getMessage());
                            }
                        }
                    }
                } else {
                    throw new RuntimeException("TypeChecker is not available");
                }
            }
        }

        // 确定块的类型
        if (node.returnValue != null) {
            // 块有显式返回值
            if (typeChecker != null) {
                node.returnValue.accept(typeChecker);
                Type returnValueType = node.returnValue.getType();
                if (returnValueType == null) {
                    SemanticException error = new SemanticException(
                        "Block return value type is null", node
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
                Type mutableType = TypeUtils.createMutableType(returnValueType, true);
                node.setType(mutableType);
            } else {
                throw new RuntimeException("No expression type checker is available");
            }
        } else if (node.statements != null && !node.statements.isEmpty()) {
            // 使用最后一条语句的类型
            ASTNode lastStmt = node.statements.get(node.statements.size() - 1);
            if (lastStmt instanceof ExprStmtNode) {
                ExprStmtNode exprStmt = (ExprStmtNode) lastStmt;
                if (exprStmt.expr != null) {
                    if (exprStmt.expr.getType() instanceof NeverType) {
                        node.setType(NeverType.MUTABLE_INSTANCE);
                        return;
                    } else {
                        node.setType(UnitType.MUTABLE_INSTANCE);
                        return;
                    }
                } else {
                    node.setType(UnitType.MUTABLE_INSTANCE);
                }
            } else {
                node.setType(UnitType.MUTABLE_INSTANCE);
            }
        } else {
            // 空块返回UnitType
            node.setType(UnitType.MUTABLE_INSTANCE);
        }
    }
    
    @Override
    public void visit(LoopExprNode node) {
        // 进入循环上下文
        enterLoopContext(node);
        
        try {
            // 检查循环条件（如果存在）
            if (node.condition != null && !node.isInfinite) {
                // 对于while循环，条件必须是布尔类型
                try {
                    if (typeChecker != null) {
                        node.condition.accept(typeChecker);
                        Type conditionType = node.condition.getType();
                        
                        // 检查条件是否为布尔类型
                        if (!conditionType.isBoolean()) {
                            SemanticException error = new SemanticException(
                                "Loop condition must be boolean: " + conditionType, node
                            );
                            if (throwOnError) {
                                throw error;
                            } else {
                                errorCollector.addError(error.getMessage());
                            }
                            return;
                        }
                    } else {
                        throw new RuntimeException("No expression type checker is available");
                    }
                } catch (RuntimeException e) {
                    if (throwOnError) {
                        throw e;
                    } else {
                        errorCollector.addError(e.getMessage());
                    }
                    return;
                }
            }
            
            // 分析循环体
            if (node.body != null) {
                node.body.accept(this);
                
                // 收集break类型
                ControlFlowContext loopContext = getCurrentContext();
                if (loopContext != null) {
                    // 收集break表达式类型并确定循环的类型
                    java.util.List<Type> breakTypes = loopContext.getBreakTypes();
                    
                    if (!breakTypes.isEmpty()) {
                        // 如果有break表达式，确定循环的类型
                        Type loopType = determineLoopType(breakTypes, node);
                        Type mutableType = TypeUtils.createMutableType(loopType, true);
                        node.setType(mutableType);
                    } else {
                        // 没有break表达式，循环是无限循环，返回never类型
                        node.setType(NeverType.MUTABLE_INSTANCE);
                    }
                } else {
                    // 不应该发生，抛出错误
                    SemanticException error = new SemanticException(
                        "Loop context not found when determining loop type", node
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                }
            } else {
                // 空循环体，返回never类型
                node.setType(NeverType.MUTABLE_INSTANCE);
            }
        } finally {
            // 退出循环上下文
            exitCurrentContext();
        }
    }
    
    @Override
    public void visit(BreakExprNode node) {
        // 查找最近的循环上下文
        ControlFlowContext loopContext = findNearestLoopContext();
        if (loopContext == null) {
            SemanticException error = new SemanticException(
                "Break expression not in a loop context", node
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }

        // 设置break表达式的目标节点
        node.setTargetNode(loopContext.getNode());

        // 检查break表达式是否有值
        Type breakValueType = null;
        if (node.value != null) {
            try {
                if (typeChecker != null) {
                    node.value.accept(typeChecker);
                    breakValueType = node.value.getType();
                } else {
                    throw new RuntimeException("No expression type checker is available");
                }

                // 将break表达式的类型添加到循环上下文中
                if (breakValueType != null) {
                    loopContext.addBreakType(breakValueType);
                } else {
                    SemanticException error = new SemanticException(
                        "Break value type is null", node
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                    return;
                }
            } catch (RuntimeException e) {
                if (throwOnError) {
                    throw e;
                } else {
                    errorCollector.addError(e.getMessage());
                }
                return;
            }
        } else {
            // break没有值，默认为unit类型
            loopContext.addBreakType(UnitType.INSTANCE);
        }

        // 设置break表达式的类型为NeverType，因为它不会返回
        node.setType(NeverType.INSTANCE);
    }
    
    @Override
    public void visit(ContinueExprNode node) {
        // 查找最近的循环上下文
        ControlFlowContext loopContext = findNearestLoopContext();
        if (loopContext == null) {
            SemanticException error = new SemanticException(
                "Continue expression not in a loop context", node
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }

        // 设置continue表达式的目标节点
        node.setTargetNode(loopContext.getNode());

        // 设置continue表达式的类型为NeverType，因为它不会返回
        node.setType(NeverType.INSTANCE);
    }
    
    @Override
    public void visit(ReturnExprNode node) {
        // 查找最近的函数上下文
        ControlFlowContext functionContext = findNearestFunctionContext();
        if (functionContext == null) {
            SemanticException error = new SemanticException(
                "Return expression not in a function context", node
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }

        // 设置return表达式的目标节点
        node.setTargetNode(functionContext.getNode());

        // 获取函数的返回类型
        Type functionReturnType = null;
        if (functionContext.getNode() instanceof FunctionNode) {
            FunctionNode functionNode = (FunctionNode) functionContext.getNode();
            if (typeExtractor != null) {
                Symbol functionSymbol = functionNode.getSymbol();
                if (functionSymbol != null) {
                    Type functionType = typeExtractor.extractTypeFromSymbol(functionSymbol);
                    if (functionType instanceof FunctionType) {
                        functionReturnType = ((FunctionType) functionType).getReturnType();
                    }
                }
            } else {
                throw new RuntimeException("TypeExtractor is not available");
            }
        } else {
            throw new RuntimeException("Function context node is not a FunctionNode");
        }

        // 检查返回值类型
        if (node.value != null) {
            try {
                if (typeChecker != null) {
                    node.value.accept(typeChecker);
                    Type returnType = node.value.getType();

                    if (returnType == null) {
                        SemanticException error = new SemanticException(
                            "Return value type is null", node
                        );
                        if (throwOnError) {
                            throw error;
                        } else {
                            errorCollector.addError(error.getMessage());
                        }
                        return;
                    }

                    if (!TypeUtils.isTypeCompatible(returnType, functionReturnType)) {
                        Type commonType = TypeUtils.findCommonType(returnType, functionReturnType);
                        if (commonType == null || !commonType.equals(functionReturnType)) {
                            SemanticException error = new SemanticException(
                                "Return value type mismatch: expected " + functionReturnType +
                                ", got " + returnType, node
                            );
                            if (throwOnError) {
                                throw error;
                            } else {
                                errorCollector.addError(error.getMessage());
                            }
                            return;
                        }
                    }
                } else {
                    throw new RuntimeException("No expression type checker is available");
                }
            } catch (RuntimeException e) {
                if (throwOnError) {
                    throw e;
                } else {
                    errorCollector.addError(e.getMessage());
                }
                return;
            }
        } else {
            if (!functionReturnType.isUnit()) {
                SemanticException error = new SemanticException(
                    "Empty return statement in function returning non-unit type: expected " +
                    functionReturnType, node
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                    return;
                }
            }
        }

        // 设置return表达式的类型为NeverType
        node.setType(NeverType.INSTANCE);
    }
    
}