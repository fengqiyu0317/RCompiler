import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

/**
 * 所有权检查器核心逻辑
 * 负责检查Rust代码中的所有权规则，包括所有权转移、借用规则等
 */
public class OwnershipCheckerCore extends VisitorBase {
    private final TypeErrorCollector errorCollector;
    private final boolean throwOnError;
    
    // 上下文信息，用于跟踪当前作用域的所有权状态
    private final Stack<OwnershipContext> contextStack = new Stack<>();
    
    public OwnershipCheckerCore(TypeErrorCollector errorCollector, boolean throwOnError) {
        this.errorCollector = errorCollector;
        this.throwOnError = throwOnError;
    }
    
    /**
     * 检查AST节点的所有权
     */
    public void checkOwnership(ASTNode node) {
        // 创建初始上下文
        contextStack.push(new OwnershipContext());
        
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
    private OwnershipContext getCurrentContext() {
        return contextStack.isEmpty() ? null : contextStack.peek();
    }
    
    /**
     * 进入新的作用域
     */
    private void enterScope() {
        contextStack.push(new OwnershipContext(getCurrentContext()));
    }
    
    /**
     * 退出当前作用域
     */
    private void exitScope() {
        if (!contextStack.isEmpty()) {
            // 直接弹出当前上下文，不需要合并状态
            // 因为通过作用域链，变量的状态变化已经实时反映到父作用域
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
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 处理变量绑定
        if (node.name instanceof IdPatNode) {
            IdPatNode idPat = (IdPatNode) node.name;
            String varName = idPat.name != null ? idPat.name.name : null;
            
            if (varName != null) {
                // 如果有初始值，检查所有权转移
                if (node.value != null) {
                    node.value.accept(this);
                    
                    // 检查是否发生了所有权转移
                    ExpressionPath valuePath = getExpressionPath(node.value);
                    if (valuePath != null) {
                        checkOwnershipTransfer(node.value);
                    }
                }
                
                // 注册新变量
                context.declareVariable(varName, idPat.isMutable);
            }
        }
    }
    
    @Override
    public void visit(AssignExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查赋值表达式
        if (node.right != null) {
            node.right.accept(this);
            
            // 检查右侧表达式的所有权转移
            ExpressionPath targetPath = getExpressionPath(node.left);
            if (targetPath != null) {
                checkOwnershipTransfer(node.right);
            }
        }
        
        // 检查赋值目标
        if (node.left != null) {
            node.left.accept(this);
            
        }
    }
    
    @Override
    public void visit(BorrowExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查内部表达式
        if (node.innerExpr != null) {
            node.innerExpr.accept(this);
            
            // 检查借用的所有权规则
            ExpressionPath path = getExpressionPath(node.innerExpr);
            if (path != null) {
                // 常规借用检查
                checkBorrowing(node.innerExpr, path, node.isMutable);
            }
        }
    }
    
    @Override
    public void visit(DerefExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查内部表达式
        if (node.innerExpr != null) {
            node.innerExpr.accept(this);
            
            // 检查解引用表达式的所有权转移
            // 解引用表达式可能导致所有权转移，取决于内部表达式的类型
            // 如果内部表达式是可变引用或Box，则解引用会移动
            // 如果内部表达式是共享引用，则解引用不会移动（除非类型实现了Copy）
            // 使用 checkOwnershipTransfer 来处理解引用的所有权转移
            checkOwnershipTransfer(node.innerExpr);
        }
    }
    
    @Override
    public void visit(IndexExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查数组表达式
        if (node.array != null) {
            node.array.accept(this);
        }
        
        // 检查索引表达式
        if (node.index != null) {
            node.index.accept(this);
        }
        
        // 检查数组/向量是否已被移动
        String arrayVar = getTargetVariable(node.array);
        if (arrayVar != null) {
            if (context.isVariableMoved(arrayVar)) {
                reportError("Use of moved value: '" + arrayVar + "'", node);
                return;
            }
        }
    }
    
    @Override
    public void visit(PathExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查变量访问
        ExpressionPath path = getExpressionPath(node);
        if (path != null) {
            // 检查表达式路径是否已被移动
            if (context.isExpressionMoved(path)) {
                reportError("Use of moved value: '" + path.getFullPath() + "'", node);
                return;
            }
        }
        
        // 检查RSeg（字段访问等）
        if (node.RSeg != null) {
            node.RSeg.accept(this);
        }
    }
    
    @Override
    public void visit(CallExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查函数调用
        if (node.function != null) {
            node.function.accept(this);
        }
        
        // 检查参数的所有权转移
        if (node.arguments != null) {
            for (ExprNode arg : node.arguments) {
                arg.accept(this);
                // 函数参数可能导致所有权转移
                checkFunctionArgumentOwnership(arg);
            }
        }
    }
    
    @Override
    public void visit(MethodCallExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查方法调用
        if (node.receiver != null) {
            node.receiver.accept(this);
            
            // 方法调用可能导致接收者的所有权转移
            checkMethodReceiverOwnership(node.receiver);
        }
        
        // 检查参数的所有权转移
        if (node.arguments != null) {
            for (ExprNode arg : node.arguments) {
                arg.accept(this);
                checkFunctionArgumentOwnership(arg);
            }
        }
    }
    
    @Override
    public void visit(FieldExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查接收者
        if (node.receiver != null) {
            node.receiver.accept(this);
        }
        
        // 检查字段名
        if (node.fieldName != null) {
            node.fieldName.accept(this);
        }
        
        // 字段访问本身不转移所有权，但需要检查接收者是否已被移动
        // 这部分逻辑已在PathExprNode中处理
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
                            // 函数参数拥有所有权
                            getCurrentContext().declareVariable(paramName, idPat.isMutable);
                        }
                    }
                }
            }
            
            // 处理self参数
            if (node.selfPara != null) {
                node.selfPara.accept(this);
            }
            
            // 处理函数体
            if (node.body != null) {
                node.body.accept(this);
            }
        } finally {
            // 在退出函数作用域前，释放self的借用
            if (node.selfPara != null && node.selfPara.isReference) {
                OwnershipContext context = getCurrentContext();
                if (context != null) {
                    context.releaseBorrow("self");
                    // 释放所有与self相关的借用路径
                    context.mutableBorrowPaths.removeIf(path -> "self".equals(path.getBaseVariable()));
                    context.immutableBorrowPaths.removeIf(path -> "self".equals(path.getBaseVariable()));
                }
            }
            // 退出函数作用域
            exitScope();
        }
    }
    
    @Override
    public void visit(SelfParaNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 处理self参数的所有权规则
        boolean isMutable = node.isMutable;
        boolean isReference = node.isReference;
        
        // 根据self参数的类型决定所有权规则
        if (isReference) {
            // 如果是引用(&self 或 &mut self)，则不会转移所有权
            // 但需要根据可变性设置借用规则
            if (isMutable) {
                // &mut self - 可变借用
                context.declareVariable("self", true);
                context.addMutableBorrow("self");
            } else {
                // &self - 不可变借用
                context.declareVariable("self", false);
                context.addImmutableBorrow("self");
            }
        } else {
            // 如果是值(self: Self)，则会转移所有权
            // self参数拥有所有权，并且是可变的（如果声明为mut）
            context.declareVariable("self", isMutable);
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
                
                // 检查返回值的所有权转移
                checkOwnershipTransfer(node.returnValue);
            }
        } finally {
            // 退出块作用域
            exitScope();
        }
    }
    
    @Override
    public void visit(IfExprNode node) {
        // 处理条件（在当前作用域中）
        if (node.condition != null) {
            node.condition.accept(this);
        }
        
        // 处理then分支（BlockExprNode会处理自己的作用域）
        if (node.thenBranch != null) {
            node.thenBranch.accept(this);
        }
        
        // 处理else分支（BlockExprNode会处理自己的作用域）
        if (node.elseBranch != null) {
            node.elseBranch.accept(this);
        }
        
        // 处理elseif分支（BlockExprNode会处理自己的作用域）
        if (node.elseifBranch != null) {
            node.elseifBranch.accept(this);
        }
    }
    
    @Override
    public void visit(LoopExprNode node) {
        // 处理循环条件（在循环作用域中）
        if (node.condition != null) {
            node.condition.accept(this);
        }
        
        // 处理循环体（在循环作用域中）
        if (node.body != null) {
            node.body.accept(this);
        }
    }
    
    @Override
    public void visit(ReturnExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查返回值
        if (node.value != null) {
            node.value.accept(this);
            
            // 常规所有权转移检查
            checkOwnershipTransfer(node.value);
        }
    }
    
    @Override
    public void visit(BreakExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查break表达式的值
        if (node.value != null) {
            node.value.accept(this);
            
            // 常规所有权转移检查
            checkOwnershipTransfer(node.value);
        }
    }
    
    @Override
    public void visit(GroupExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查内部表达式
        if (node.innerExpr != null) {
            node.innerExpr.accept(this);
            
            // 检查所有权转移
            checkOwnershipTransfer(node.innerExpr);
        }
    }
    
    @Override
    public void visit(NegaExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查内部表达式
        if (node.innerExpr != null) {
            node.innerExpr.accept(this);
            
            // 检查所有权转移
            checkOwnershipTransfer(node.innerExpr);
        }
    }
    
    @Override
    public void visit(TypeCastExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查表达式
        if (node.expr != null) {
            node.expr.accept(this);
            
            // 检查所有权转移
            checkOwnershipTransfer(node.expr);
        }
    }
    
    @Override
    public void visit(ComAssignExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查赋值目标
        if (node.left != null) {
            node.left.accept(this);
        }
        
        // 检查赋值表达式
        if (node.right != null) {
            node.right.accept(this);
            
            // 检查右侧表达式的所有权转移
            ExpressionPath targetPath = getExpressionPath(node.left);
            if (targetPath != null) {
                checkOwnershipTransfer(node.right);
            }
        }
    }
    
    @Override
    public void visit(ArrayExprNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查数组元素
        if (node.elements != null) {
            for (ExprNode element : node.elements) {
                element.accept(this);
            }
        }
        
        // 检查重复元素
        if (node.repeatedElement != null) {
            node.repeatedElement.accept(this);
        }
        
        // 检查数组大小
        if (node.size != null) {
            node.size.accept(this);
        }
        
        // 检查数组元素的所有权转移
        if (node.elements != null) {
            for (ExprNode element : node.elements) {
                checkOwnershipTransfer(element);
            }
        }
        if (node.repeatedElement != null) {
            checkOwnershipTransfer(node.repeatedElement);
        }
    }
    
    @Override
    public void visit(FieldValNode node) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 检查字段名
        if (node.fieldName != null) {
            node.fieldName.accept(this);
        }
        
        // 检查字段值
        if (node.value != null) {
            node.value.accept(this);
            
            // 检查字段值的所有权转移
            checkOwnershipTransfer(node.value);
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 检查所有权转移
     */
    private void checkOwnershipTransfer(ExprNode expr) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        // 获取源表达式路径
        ExpressionPath sourcePath = getExpressionPath(expr);
        if (sourcePath == null) {
            return; // 无法解析表达式路径
        }
        
        String baseVar = sourcePath.getBaseVariable();
        
        // 1. 检查源值是否已经被移动 (Use of Moved Value)
        if (context.isExpressionMoved(sourcePath)) {
            reportError("Use of moved value: '" + sourcePath.getFullPath() + "'", expr);
            return;
        }
        
        // 2. 检查源值是否处于"被借用"状态 (Borrowing Conflict)
        if (context.isVariableBorrowed(baseVar)) {
            reportError("Cannot move '" + baseVar + "' while it is borrowed", expr);
            return;
        }
        
        // 3. 检查是否试图从"引用"中移出所有权 (Moving out of Reference)
        if (isMovingOutOfReference(expr)) {
            reportError("Cannot move out of reference: '" + sourcePath.getFullPath() + "'", expr);
            return;
        }
        
        // 4. 检查部分移动 (Partial Move) 的合法性
        if (!isPartialMoveLegal(expr, sourcePath)) {
            reportError("Illegal partial move: '" + sourcePath.getFullPath() + "'", expr);
            return;
        }
        
        // 5. 检查类型是否实现了 Copy Trait
        if (hasCopyTrait(expr)) {
            // 如果类型实现了Copy trait，则不会发生所有权转移
            return;
        }
        
        context.moveExpressionPath(sourcePath);
    }
    
    /**
     * 检查是否试图从引用中移出所有权
     *
     * 在Rust中，不能从引用中移出所有权，例如：
     * - 不能从 *ref 中移出，其中 ref 是 &T 或 &mut T
     * - 不能从 (*ref).field 中移出
     * - 不能从 (*ref)[index] 中移出
     *
     * 注意：不能简单地检查是否是解引用节点，因为：
     * 1. 不是所有解引用都是从引用中移出（如 *Box::new(5)）
     * 2. 需要递归检查字段访问和索引访问中的引用
     */
    private boolean isMovingOutOfReference(ExprNode expr) {
        // 检查解引用表达式
        if (expr instanceof DerefExprNode) {
            DerefExprNode derefExpr = (DerefExprNode) expr;
            // 检查解引用操作是否作用于引用类型的值
            // 需要检查内部表达式的类型是否是引用类型
            return isReferenceType(derefExpr.innerExpr);
        }
        
        // 检查字段访问中的引用
        if (expr instanceof FieldExprNode) {
            FieldExprNode fieldExpr = (FieldExprNode) expr;
            return isMovingOutOfReference(fieldExpr.receiver);
        }
        
        // 检查索引访问中的引用
        if (expr instanceof IndexExprNode) {
            IndexExprNode indexExpr = (IndexExprNode) expr;
            return isMovingOutOfReference(indexExpr.array);
        }
        
        return false;
    }
    
    /**
     * 检查表达式是否是引用类型
     */
    private boolean isReferenceType(ExprNode expr) {
        if (expr == null) {
            return false;
        }
        
        // 检查表达式类型是否已设置
        if (expr.getType() == null) {
            return false;
        }
        
        // 检查是否是引用类型
        Type exprType = expr.getType();
        return exprType instanceof ReferenceType;
    }
    
    /**
     * 检查部分移动的合法性
     */
    private boolean isPartialMoveLegal(ExprNode expr, ExpressionPath sourcePath) {
        // 对于字段访问，检查父结构体是否被借用或已移动
        if (sourcePath instanceof FieldAccessPath) {
            FieldAccessPath fieldPath = (FieldAccessPath) sourcePath;
            String baseVar = fieldPath.getBaseVariable();
            OwnershipContext context = getCurrentContext();
            
            // 检查父结构体是否被借用
            if (context != null && context.isVariableBorrowed(baseVar)) {
                return false;
            }
            
            // 检查父结构体是否已被移动
            if (context != null && context.isVariableMoved(baseVar)) {
                return false;
            }
        }
        
        // 对于索引访问，Rust通常不允许直接移出元素
        if (sourcePath instanceof IndexAccessPath) {
            // 索引访问通常不允许直接移出所有权
            // 除非使用特殊方法如 std::mem::replace
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查类型是否实现了Copy trait
     * 这是一个简化实现，实际应该查询类型系统
     */
    private boolean hasCopyTrait(ExprNode expr) {
        // 检查表达式类型是否为基本类型
        if (expr.getType() != null && expr.getType() instanceof PrimitiveType) {
            PrimitiveType primitiveType = (PrimitiveType) expr.getType();
            PrimitiveType.PrimitiveKind kind = primitiveType.getKind();
            
            // 基本类型（如整数、布尔值等）实现了Copy trait
            // 但字符串类型（STR和STRING）不实现Copy trait
            if (kind == PrimitiveType.PrimitiveKind.STR || kind == PrimitiveType.PrimitiveKind.STRING) {
                return false;
            }
            
            return true;
        }
        
        // 引用类型总是可以复制（复制的是引用本身）
        if (expr instanceof BorrowExprNode) {
            return true;
        }
        
        // 对于其他类型，这里简化处理
        // 实际实现应该查询类型系统以确定是否实现了Copy trait
        return false;
    }
    
    /**
     * 检查借用规则
     */
    private void checkBorrowing(ExprNode expr, ExpressionPath path, boolean isMutableBorrow) {
        OwnershipContext context = getCurrentContext();
        if (context == null) return;
        
        String baseVar = path.getBaseVariable();
        
        // 1. 检查目标值是否已经被移动 (Liveness Check)
        // 不能借用一个所有权已经转移走的值，因为它现在是未初始化的
        if (context.isExpressionMoved(path)) {
            reportError("Borrow of moved value: '" + path.getFullPath() + "'", expr);
            return;
        }
        
        // 2. 检查基础变量（Parent）的状态
        // 如果借用的是字段（s.a）或索引（a[0]），必须检查其所属的"基础变量"（Base Variable）的状态
        
        // 2.1 检查"如果父结构体被移动了，子字段也不能被借用"
        // 核心逻辑：当你把一个结构体（Parent）的所有权转移（Move）给别人时，你转移的是整个结构体。
        // 这意味着原来的变量变成了"未初始化"状态。既然整个箱子都送人了，你自然不能再伸手去箱子里拿东西（借用里面的字段）。
        if (path instanceof FieldAccessPath) {
            FieldAccessPath fieldPath = (FieldAccessPath) path;
            
            // 检查基础变量是否已被移动
            if (context.isVariableMoved(baseVar)) {
                reportError("Borrow of moved value: '" + baseVar + "'", expr);
                return;
            }
            
            // 检查父结构体的借用状态与子字段借用的兼容性
            if (context.mutableBorrows.contains(baseVar)) {
                // 父结构体被可变借用，不能借用任何字段（无论是可变还是不可变）
                reportError("Cannot borrow field '" + fieldPath.getFieldName() + "' of '" + baseVar + "' while it is mutably borrowed", expr);
                return;
            } else if (context.immutableBorrows.contains(baseVar) && isMutableBorrow) {
                // 父结构体被不可变借用，不能可变借用其字段
                reportError("Cannot mutably borrow field '" + fieldPath.getFieldName() + "' of '" + baseVar + "' while it is immutably borrowed", expr);
                return;
            }
        } else if (path instanceof IndexAccessPath) {
            IndexAccessPath indexPath = (IndexAccessPath) path;
            
            // 检查基础变量是否已被移动
            if (context.isVariableMoved(baseVar)) {
                reportError("Borrow of moved value: '" + baseVar + "'", expr);
                return;
            }
            
            // 检查父结构体的借用状态与索引访问的兼容性
            if (context.mutableBorrows.contains(baseVar)) {
                // 父数组被可变借用，不能索引访问任何元素（无论是可变还是不可变）
                reportError("Cannot index '" + baseVar + "' while it is mutably borrowed", expr);
                return;
            } else if (context.immutableBorrows.contains(baseVar) && isMutableBorrow) {
                // 父数组被不可变借用，不能可变借用其索引
                reportError("Cannot mutably index '" + baseVar + "' while it is immutably borrowed", expr);
                return;
            }
        } else if (path instanceof DerefAccessPath) {
            DerefAccessPath derefPath = (DerefAccessPath) path;
            
            // 检查基础变量是否已被移动
            if (context.isVariableMoved(baseVar)) {
                reportError("Borrow of moved value: '" + baseVar + "'", expr);
                return;
            }

            if (expr instanceof DerefExprNode) {
                DerefExprNode derefNode = (DerefExprNode) expr;
                if (derefNode.innerExpr != null) {
                    Type pointerType = derefNode.innerExpr.getType();
                    
                    // 检查：不能通过不可变引用修改数据
                    // 如果指针是 &T (不可变引用)，但试图进行 &mut *ptr (可变借用)，则报错
                    if (isMutableBorrow && isImmutableReference(pointerType)) {
                        reportError("Cannot borrow as mutable content of an immutable reference", expr);
                        return;
                    }
                }
            }
            
            // 检查父结构体的借用状态与解引用访问的兼容性
            if (context.mutableBorrows.contains(baseVar)) {
                // 父指针被可变借用，不能解引用访问（无论是可变还是不可变）
                reportError("Cannot dereference '" + baseVar + "' while it is mutably borrowed", expr);
                return;
            } else if (context.immutableBorrows.contains(baseVar) && isMutableBorrow) {
                // 父指针被不可变借用，不能可变借用其解引用
                reportError("Cannot mutably dereference '" + baseVar + "' while it is immutably borrowed", expr);
                return;
            }
        }
        
        // 3. 检查借用冲突 (Borrowing Rules / Aliasing XOR Mutation)
        // 这是 Rust 借用检查器最核心的部分。规则取决于你是要创建不可变借用 (&T) 还是可变借用 (&mut T)
        
        if (isMutableBorrow) {
            // B. 如果是可变借用 (&mut T)
            // Rust 规则: 当前不能有任何活跃的借用（既不能有 &T，也不能有 &mut T）。必须拥有独占访问权。
            
            // 4. 检查变量的可变性声明 (Mutability Declaration)
            // 如果要创建一个可变借用 &mut x，变量 x 本身必须被声明为 mut
            if (!context.isVariableMutable(baseVar)) {
                reportError("Cannot create mutable borrow of immutable variable '" + baseVar + "'", expr);
                return;
            }
            
            // 检查是否可以创建可变借用
            if (!context.canCreateMutableBorrow(path)) {
                // 检查具体是什么类型的借用冲突
                if (context.mutableBorrowPaths.contains(path)) {
                    reportError("Cannot create mutable borrow of '" + path.getFullPath() + "' while it is already mutably borrowed", expr);
                } else if (context.immutableBorrowPaths.contains(path)) {
                    reportError("Cannot create mutable borrow of '" + path.getFullPath() + "' while it is immutably borrowed", expr);
                } else {
                    reportError("Cannot create mutable borrow of '" + path.getFullPath() + "'", expr);
                }
                return;
            }
            
            context.addMutableBorrowPath(path);
        } else {
            // A. 如果是不可变借用 (&T)
            // Rust 规则: 当前不能有任何活跃的可变借用 (&mut T)。允许同时存在多个其他不可变借用
            
            // 检查是否可以创建不可变借用
            if (!context.canCreateImmutableBorrow(path)) {
                // 检查具体是什么类型的借用冲突
                if (context.mutableBorrowPaths.contains(path)) {
                    reportError("Cannot create immutable borrow of '" + path.getFullPath() + "' while it is already mutably borrowed", expr);
                } else {
                    reportError("Cannot create immutable borrow of '" + path.getFullPath() + "'", expr);
                }
                return;
            }
            
            context.addImmutableBorrowPath(path);
        }
    }
    
    // LegacyFieldAccessPath and checkFieldBorrowing method have been removed
    // Field borrowing is now handled through VariablePath with full path
    
    /**
     * 检查函数参数的所有权
     */
    private void checkFunctionArgumentOwnership(ExprNode arg) {
        // 直接调用 checkOwnershipTransfer 来处理函数参数的所有权转移
        checkOwnershipTransfer(arg);
    }
    
    /**
     * 检查方法接收者的所有权
     */
    private void checkMethodReceiverOwnership(ExprNode receiver) {
        // 直接调用 checkOwnershipTransfer 来处理方法接收者的所有权转移
        checkOwnershipTransfer(receiver);
    }
    
    /**
     * 从表达式获取变量名
     */
    private String getVariableName(ExprNode expr) {
        if (expr instanceof PathExprNode) {
            PathExprNode pathExpr = (PathExprNode) expr;
            if (pathExpr.LSeg != null && pathExpr.LSeg.name != null) {
                return pathExpr.LSeg.name.name;
            }
        }
        return null;
    }
    
    /**
     * 从表达式获取表达式路径
     */
    private ExpressionPath getExpressionPath(ExprNode expr) {
        if (expr == null) {
            return null;
        }
        
        if (expr instanceof PathExprNode) {
            // 简单变量路径，如 x 或 self
            String varName = getVariableName(expr);
            return varName != null ? new VariablePath(varName) : null;
        } else if (expr instanceof FieldExprNode) {
            // 字段访问，如 obj.field 或 self.field
            FieldExprNode fieldExpr = (FieldExprNode) expr;
            ExpressionPath receiverPath = getExpressionPath(fieldExpr.receiver);
            if (receiverPath != null && fieldExpr.fieldName != null && fieldExpr.fieldName.name != null) {
                return new FieldAccessPath(receiverPath, fieldExpr.fieldName.name);
            }
            return null;
        } else if (expr instanceof IndexExprNode) {
            // 索引访问，如 array[0] 或 self[0]
            IndexExprNode indexExpr = (IndexExprNode) expr;
            ExpressionPath arrayPath = getExpressionPath(indexExpr.array);
            if (arrayPath != null) {
                return new IndexAccessPath(arrayPath, indexExpr.index);
            }
            return null;
        } else if (expr instanceof DerefExprNode) {
            // 解引用，如 *ptr 或 *self
            DerefExprNode derefExpr = (DerefExprNode) expr;
            ExpressionPath pointerPath = getExpressionPath(derefExpr.innerExpr);
            if (pointerPath != null) {
                return new DerefAccessPath(pointerPath);
            }
            return null;
        } 
        
        // 其他类型暂时返回null
        return null;
    }
    
    // LegacyFieldAccessPath and getTargetVariablePath method have been removed
    // Field access paths are now handled directly through getExpressionPath
    
    /**
     * 从赋值目标获取变量名（保持向后兼容）
     */
    private String getTargetVariable(ExprNode target) {
        ExpressionPath path = getExpressionPath(target);
        return path != null ? path.getBaseVariable() : null;
    }
    
    /**
     * 检查类型是否是不可变引用
     */
    private boolean isImmutableReference(Type type) {
        if (type == null) {
            return false;
        }
        // 检查是否是引用类型且不可变
        if (type instanceof ReferenceType) {
            ReferenceType refType = (ReferenceType) type;
            return !refType.isReferenceMutable();
        }
        return false;
    }
}