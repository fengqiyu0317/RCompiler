import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Mutability检查器
 * 负责检查Rust代码中的mutability规则，确保内存安全和防止数据竞争
 */
public class MutabilityChecker extends VisitorBase {
    private final TypeErrorCollector errorCollector;
    private final boolean throwOnError;
    
    // 上下文信息，用于跟踪当前作用域的mutability状态
    private final Stack<MutabilityContext> contextStack = new Stack<>();
    
    public MutabilityChecker(TypeErrorCollector errorCollector, boolean throwOnError) {
        this.errorCollector = errorCollector;
        this.throwOnError = throwOnError;
    }
    
    /**
     * 检查AST节点的mutability
     */
    public void checkMutability(ASTNode node) {
        // 创建初始上下文
        contextStack.push(new MutabilityContext());
        
        try {
            node.accept(this);
        } finally {
            // 确保上下文被清理
            if (!contextStack.isEmpty()) {
                contextStack.pop();
            }
        }
    }
    
    /**
     * 获取当前上下文
     */
    private MutabilityContext getCurrentContext() {
        return contextStack.isEmpty() ? null : contextStack.peek();
    }
    
    /**
     * 进入新的作用域
     */
    private void enterScope() {
        contextStack.push(new MutabilityContext(getCurrentContext()));
    }
    
    /**
     * 退出当前作用域
     */
    private void exitScope() {
        if (!contextStack.isEmpty()) {
            contextStack.pop();
        }
    }
    
    /**
     * 报告错误
     */
    private void reportError(String message, ASTNode node) {
        RuntimeException error = new RuntimeException(message);
        if (throwOnError) {
            throw error;
        } else {
            errorCollector.addError(error.getMessage());
        }
    }
    
    // ==================== 访问方法 ====================
    
    @Override
    public void visit(LetStmtNode node) {
        MutabilityContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查变量绑定的mutability
        if (node.name instanceof IdPatNode) {
            IdPatNode idPat = (IdPatNode) node.name;
            String varName = idPat.name != null ? idPat.name.name : null;
            
            if (varName != null) {
                // 设置变量的可变性
                boolean isMutable = idPat.isMutable;
                context.setVariableMutability(varName, isMutable);
                
                // 如果有初始值，检查赋值的mutability
                if (node.value != null) {
                    node.value.accept(this);
                }
            }
        } else if (node.name instanceof RefPatNode) {
            // 处理引用模式
            RefPatNode refPat = (RefPatNode) node.name;
            if (refPat.innerPattern instanceof IdPatNode) {
                IdPatNode idPat = (IdPatNode) refPat.innerPattern;
                String varName = idPat.name != null ? idPat.name.name : null;
                
                if (varName != null) {
                    // 引用模式创建的变量总是不可变的，即使引用本身是可变的
                    context.setVariableMutability(varName, false);
                    
                    // 检查引用的mutability
                    if (node.value != null) {
                        node.value.accept(this);
                    }
                }
            }
        }
    }
    
    @Override
    public void visit(AssignExprNode node) {
        MutabilityContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查赋值目标的mutability
        if (node.left != null) {
            node.left.accept(this);
            
            // 检查是否可以赋值
            if (!isAssignableTarget(node.left)) {
                reportError("Cannot assign to immutable expression", node);
                return;
            }
        }
        
        // 检查赋值表达式
        if (node.right != null) {
            node.right.accept(this);
        }
    }
    
    @Override
    public void visit(ComAssignExprNode node) {
        MutabilityContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查复合赋值目标的mutability
        if (node.left != null) {
            node.left.accept(this);
            
            // 检查是否可以赋值
            if (!isAssignableTarget(node.left)) {
                reportError("Cannot perform compound assignment on immutable expression", node);
                return;
            }
        }
        
        // 检查赋值表达式
        if (node.right != null) {
            node.right.accept(this);
        }
    }
    
    @Override
    public void visit(BorrowExprNode node) {
        MutabilityContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查内部表达式
        if (node.innerExpr != null) {
            node.innerExpr.accept(this);
            
            // 检查借用的mutability
            if (node.innerExpr instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) node.innerExpr;
                String varName = getVariableName(pathExpr);
                
                if (varName != null) {
                    if (node.isMutable) {
                        // 可变借用
                        if (!context.canCreateMutableBorrow(varName)) {
                            reportError("Cannot create mutable borrow of variable '" + varName + "'", node);
                            return;
                        }
                        context.addMutableBorrow(varName);
                    } else {
                        // 不可变借用
                        if (!context.canCreateImmutableBorrow(varName)) {
                            reportError("Cannot create immutable borrow of variable '" + varName + "'", node);
                            return;
                        }
                        context.addImmutableBorrow(varName);
                    }
                }
            }
        }
    }
    
    @Override
    public void visit(DerefExprNode node) {
        MutabilityContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查内部表达式
        if (node.innerExpr != null) {
            node.innerExpr.accept(this);
        }
    }
    
    @Override
    public void visit(PathExprNode node) {
        MutabilityContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查变量访问
        String varName = getVariableName(node);
        if (varName != null) {
            // 这里不需要特别的检查，只是记录访问
            // 实际的mutability检查在使用变量的地方进行
        }
        
        // 检查RSeg（字段访问等）
        if (node.RSeg != null) {
            node.RSeg.accept(this);
        }
    }
    
    @Override
    public void visit(FunctionNode node) {
        // 进入新的函数作用域
        enterScope();
        
        try {
            // 处理参数
            if (node.parameters != null) {
                for (ParameterNode param : node.parameters) {
                    if (param.name instanceof IdPatNode) {
                        IdPatNode idPat = (IdPatNode) param.name;
                        String paramName = idPat.name != null ? idPat.name.name : null;
                        
                        if (paramName != null) {
                            // 函数参数总是可变的（在函数内部）
                            getCurrentContext().setVariableMutability(paramName, true);
                        }
                    }
                }
            }
            
            // 处理self参数
            if (node.selfPara != null) {
                // self参数的可变性由selfPara决定
                // 这里不需要特别处理，因为它在类型检查阶段已经处理
            }
            
            // 处理函数体
            if (node.body != null) {
                node.body.accept(this);
            }
        } finally {
            // 退出函数作用域
            exitScope();
        }
    }
    
    @Override
    public void visit(BlockExprNode node) {
        // 进入新的块作用域
        enterScope();
        
        try {
            // 处理块中的语句
            if (node.statements != null) {
                for (ASTNode stmt : node.statements) {
                    stmt.accept(this);
                }
            }
            
            // 处理返回值
            if (node.returnValue != null) {
                node.returnValue.accept(this);
            }
        } finally {
            // 退出块作用域
            exitScope();
        }
    }
    
    @Override
    public void visit(IfExprNode node) {
        // 处理条件
        if (node.condition != null) {
            node.condition.accept(this);
        }
        
        // 处理then分支
        if (node.thenBranch != null) {
            node.thenBranch.accept(this);
        }
        
        // 处理else分支
        if (node.elseBranch != null) {
            node.elseBranch.accept(this);
        }
        
        // 处理elseif分支
        if (node.elseifBranch != null) {
            node.elseifBranch.accept(this);
        }
    }
    
    @Override
    public void visit(LoopExprNode node) {
        // 进入新的循环作用域
        enterScope();
        
        try {
            // 处理循环条件
            if (node.condition != null) {
                node.condition.accept(this);
            }
            
            // 处理循环体
            if (node.body != null) {
                node.body.accept(this);
            }
        } finally {
            // 退出循环作用域
            exitScope();
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 检查表达式是否是可赋值的目标
     */
    private boolean isAssignableTarget(ExprNode expr) {
        // 在Rust中，这些类型的表达式可以是左值：
        // 1. PathExprNode - 路径表达式（变量、字段等）
        // 2. FieldExprNode - 字段访问表达式
        // 3. IndexExprNode - 索引访问表达式
        // 4. DerefExprNode - 解引用表达式
        
        if (expr instanceof PathExprNode) {
            PathExprNode pathExpr = (PathExprNode) expr;
            String varName = getVariableName(pathExpr);
            
            if (varName != null) {
                MutabilityContext context = getCurrentContext();
                if (context != null) {
                    return context.isVariableMutable(varName);
                }
            }
        }
        
        return expr instanceof FieldExprNode ||
               expr instanceof IndexExprNode ||
               expr instanceof DerefExprNode;
    }
    
    /**
     * 从路径表达式获取变量名
     */
    private String getVariableName(PathExprNode pathExpr) {
        if (pathExpr.LSeg != null && pathExpr.LSeg.name != null) {
            return pathExpr.LSeg.name.name;
        }
        return null;
    }
    
    /**
     * Mutability上下文类
     */
    public static class MutabilityContext {
        private final Map<String, Boolean> variableMutability = new HashMap<>();
        private final Set<String> mutableBorrows = new HashSet<>();
        private final Set<String> immutableBorrows = new HashSet<>();
        
        /**
         * 构造函数，用于创建新上下文
         */
        public MutabilityContext() {
            // 空构造函数
        }
        
        /**
         * 构造函数，用于从父上下文继承
         */
        public MutabilityContext(MutabilityContext parent) {
            if (parent != null) {
                // 继承父上下文的变量可变性
                variableMutability.putAll(parent.variableMutability);
            }
        }
        
        /**
         * 检查变量是否可变
         */
        public boolean isVariableMutable(String variableName) {
            return variableMutability.getOrDefault(variableName, false);
        }
        
        /**
         * 设置变量的可变性
         */
        public void setVariableMutability(String variableName, boolean isMutable) {
            variableMutability.put(variableName, isMutable);
        }
        
        /**
         * 添加可变借用
         */
        public void addMutableBorrow(String variableName) {
            mutableBorrows.add(variableName);
        }
        
        /**
         * 添加不可变借用
         */
        public void addImmutableBorrow(String variableName) {
            immutableBorrows.add(variableName);
        }
        
        /**
         * 检查是否可以创建可变借用
         */
        public boolean canCreateMutableBorrow(String variableName) {
            // 如果变量不可变，则不能创建可变借用
            if (!isVariableMutable(variableName)) {
                return false;
            }
            
            // 如果变量已经有任何借用，则不能创建新的借用
            return !mutableBorrows.contains(variableName) && !immutableBorrows.contains(variableName);
        }
        
        /**
         * 检查是否可以创建不可变借用
         */
        public boolean canCreateImmutableBorrow(String variableName) {
            // 如果变量已经有可变借用，则不能创建不可变借用
            return !mutableBorrows.contains(variableName);
        }
        
        /**
         * 释放借用
         */
        public void releaseBorrow(String variableName) {
            mutableBorrows.remove(variableName);
            immutableBorrows.remove(variableName);
        }
    }
}