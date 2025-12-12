/**
 * 所有权检查器
 * 负责检查Rust代码中的所有权规则，包括所有权转移、借用规则等
 */
public class OwnershipChecker extends VisitorBase {
    private final OwnershipCheckerCore core;
    
    public OwnershipChecker(TypeErrorCollector errorCollector, boolean throwOnError) {
        this.core = new OwnershipCheckerCore(errorCollector, throwOnError);
    }
    
    /**
     * 检查AST节点的所有权
     */
    public void checkOwnership(ASTNode node) {
        core.checkOwnership(node);
    }
    
    // 委托所有访问方法给核心实现
    @Override
    public void visit(LetStmtNode node) {
        core.visit(node);
    }
    
    @Override
    public void visit(AssignExprNode node) {
        core.visit(node);
    }
    
    @Override
    public void visit(BorrowExprNode node) {
        core.visit(node);
    }
    
    @Override
    public void visit(DerefExprNode node) {
        core.visit(node);
    }
    
    @Override
    public void visit(IndexExprNode node) {
        core.visit(node);
    }
    
    @Override
    public void visit(PathExprNode node) {
        core.visit(node);
    }
    
    @Override
    public void visit(CallExprNode node) {
        core.visit(node);
    }
    
    @Override
    public void visit(MethodCallExprNode node) {
        core.visit(node);
    }
    
    @Override
    public void visit(FunctionNode node) {
        core.visit(node);
    }
    
    @Override
    public void visit(BlockExprNode node) {
        core.visit(node);
    }
    
    @Override
    public void visit(IfExprNode node) {
        core.visit(node);
    }
    
    @Override
    public void visit(LoopExprNode node) {
        core.visit(node);
    }
}