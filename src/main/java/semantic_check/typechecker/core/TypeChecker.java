
/**
 * Main type checker class that coordinates type checking operations
 */
public class TypeChecker extends VisitorBase {
    protected final TypeErrorCollector errorCollector;
    protected final boolean throwOnError;
    protected final TypeExtractor typeExtractor;
    public final ConstantEvaluator constantEvaluator;
    protected final ExpressionTypeContext context;
    protected final ControlFlowTypeChecker controlFlowTypeChecker;
    protected final StatementTypeChecker statementTypeChecker;
    
    // 专门的类型检查器
    protected final SimpleExpressionTypeChecker simpleExpressionChecker;
    protected final OperatorExpressionTypeChecker operatorExpressionChecker;
    protected final ComplexExpressionTypeChecker complexExpressionChecker;
    
    // Mutability检查器
    protected final MutabilityChecker mutabilityChecker;
    
    // Trait impl checker for checking trait implementations
    private final TraitImplChecker traitImplChecker;
    
    // Expression type checker for checking expression types
    private final ExpressionTypeChecker expressionTypeChecker;
    
    public TypeChecker(boolean throwOnError) {
        this.errorCollector = new TypeErrorCollector();
        this.throwOnError = throwOnError;
        this.constantEvaluator = new ConstantEvaluator(throwOnError);
        
        // Initialize the helper classes
        this.typeExtractor = new TypeExtractor(errorCollector, throwOnError, constantEvaluator);
        // Set the TypeChecker reference after creating the TypeExtractor
        this.typeExtractor.setTypeChecker(this);
        this.expressionTypeChecker = new ExpressionTypeChecker(errorCollector, throwOnError, typeExtractor, constantEvaluator);
        
        // 创建表达式类型上下文
        this.context = new ExpressionTypeContext();
        
        // 创建mutability检查器
        this.mutabilityChecker = new MutabilityChecker(errorCollector, throwOnError);
        
        // 创建专门的类型检查器，传递自身作为主检查器
        this.simpleExpressionChecker = new SimpleExpressionTypeChecker(
            errorCollector, throwOnError, typeExtractor, constantEvaluator, context, this);
        this.operatorExpressionChecker = new OperatorExpressionTypeChecker(
            errorCollector, throwOnError, typeExtractor, constantEvaluator, context, this);
        this.complexExpressionChecker = new ComplexExpressionTypeChecker(
            errorCollector, throwOnError, typeExtractor, constantEvaluator, context, this);
            
        // 创建控制流类型检查器，传递自身作为表达式类型检查器
        this.controlFlowTypeChecker = new ControlFlowTypeChecker(errorCollector, throwOnError,
                                                               typeExtractor, constantEvaluator, this);
        
        // 创建语句类型检查器，传递自身作为表达式类型检查器
        this.statementTypeChecker = new StatementTypeChecker(errorCollector, throwOnError,
                                                             typeExtractor, this, controlFlowTypeChecker);
        
        this.traitImplChecker = new TraitImplChecker(errorCollector, throwOnError, typeExtractor);
        
        // 设置mutability检查器到各个专门的检查器
        this.simpleExpressionChecker.setMutabilityChecker(mutabilityChecker);
        this.operatorExpressionChecker.setMutabilityChecker(mutabilityChecker);
        this.complexExpressionChecker.setMutabilityChecker(mutabilityChecker);
        
        // Set up circular dependency between TypeExtractor and ExpressionTypeChecker
        this.typeExtractor.setExpressionTypeChecker(expressionTypeChecker);
    }
    
    // ==================== 公共方法 ====================
    
    /**
     * 获取当前Self类型
     */
    public Type getCurrentSelfType() {
        return context.getCurrentSelfType();
    }
    
    /**
     * 设置当前Self类型
     */
    public void setCurrentType(Type type) {
        context.setCurrentSelfType(type);
    }
    
    /**
     * 清除当前Self类型
     */
    public void clearCurrentType() {
        context.clearCurrentSelfType();
    }
    
    
    /**
     * 进入函数上下文
     */
    public void enterFunctionContext(FunctionNode node) {
        context.enterFunctionContext(node);
    }
    
    /**
     * 退出函数上下文
     */
    public void exitFunctionContext() {
        context.exitFunctionContext();
    }
    
    /**
     * 获取节点描述用于错误报告
     */
    private String getNodeDescription(ExprNode node) {
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
        }
        
        return className;
    }
    
    /**
     * 获取当前上下文用于错误报告
     */
    private String getCurrentContext() {
        StringBuilder contextStr = new StringBuilder();
        
        // 添加函数上下文（如果可用）
        if (context.getCurrentFunction() != null) {
            FunctionNode funcNode = context.getCurrentFunction();
            if (funcNode.name != null) {
                contextStr.append("in function '").append(funcNode.name.name).append("' ");
            }
        }
        
        // 添加当前Self类型（如果可用）
        if (context.getCurrentSelfType() != null) {
            contextStr.append("(Self: ").append(context.getCurrentSelfType()).append(") ");
        }
        
        return contextStr.toString();
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
    
    /**
     * 获取常量求值器
     */
    public ConstantEvaluator getConstantEvaluator() {
        return constantEvaluator;
    }
    
    /**
     * 检查是否有常量求值错误
     */
    public boolean hasConstantEvaluationErrors() {
        return constantEvaluator.hasErrors();
    }
    
    /**
     * 获取常量求值错误收集器
     */
    public TypeErrorCollector getConstantEvaluationErrorCollector() {
        return constantEvaluator.getErrorCollector();
    }
    
    /**
     * 获取mutability检查器
     */
    public MutabilityChecker getMutabilityChecker() {
        return mutabilityChecker;
    }
    
    /**
     * 检查mutability
     */
    public void checkMutability(ASTNode node) {
        if (mutabilityChecker != null) {
            mutabilityChecker.checkMutability(node);
        }
    }
    
    /**
     * 获取表达式类型上下文
     */
    public ExpressionTypeContext getContext() {
        return context;
    }
    
    // ==================== 访问方法 ====================
    
    // ==================== ControlFlowTypeChecker 访问方法 ====================
    
    public void visit(ExprWithBlockNode node) {
        // 委托给控制流类型检查器处理
        node.accept(controlFlowTypeChecker);
    }
    
    public void visit(BlockExprNode node) throws RuntimeException {
        // 委托给控制流类型检查器处理
        node.accept(controlFlowTypeChecker);
    }
    
    public void visit(IfExprNode node) throws RuntimeException {
        // 委托给控制流类型检查器处理
        node.accept(controlFlowTypeChecker);
    }
    
    public void visit(LoopExprNode node) throws RuntimeException {
        // 委托给控制流类型检查器处理
        node.accept(controlFlowTypeChecker);
    }
    
    public void visit(BreakExprNode node) throws RuntimeException {
        // 委托给控制流类型检查器处理
        node.accept(controlFlowTypeChecker);
    }
    
    public void visit(ContinueExprNode node) {
        // 委托给控制流类型检查器处理
        node.accept(controlFlowTypeChecker);
    }
    
    public void visit(ReturnExprNode node) throws RuntimeException {
        // 委托给控制流类型检查器处理
        node.accept(controlFlowTypeChecker);
    }
    
    // ==================== SimpleExpressionTypeChecker 访问方法 ====================
    
    public void visit(ExprWithoutBlockNode node) {
        // 委托给简单表达式检查器处理
        if (node.expr != null) {
            node.expr.accept(this);
        } else {
            throw new RuntimeException(
                "ExprWithoutBlockNode has no expr to visit"
            );
        }
    }
    
    public void visit(LiteralExprNode node) {
        // 委托给简单表达式检查器处理
        node.accept(simpleExpressionChecker);
    }
    
    public void visit(PathExprNode node) throws RuntimeException {
        // 委托给简单表达式检查器处理
        node.accept(simpleExpressionChecker);
    }
    
    public void visit(PathExprSegNode node) throws RuntimeException {
        // 委托给简单表达式检查器处理
        node.accept(simpleExpressionChecker);
    }
    
    public void visit(GroupExprNode node) throws RuntimeException {
        // 委托给简单表达式检查器处理
        node.accept(simpleExpressionChecker);
    }
    
    public void visit(UnderscoreExprNode node) {
        // 委托给简单表达式检查器处理
        node.accept(simpleExpressionChecker);
    }
    
    // ==================== OperatorExpressionTypeChecker 访问方法 ====================
    
    public void visit(OperExprNode node) {
        // 这是一个抽象基类，在正确的访问者模式实现中不应该到达这里
        throw new RuntimeException(
            "Cannot visit abstract OperExprNode directly"
        );
    }
    
    public void visit(NegaExprNode node) throws RuntimeException {
        // 委托给运算符表达式检查器处理
        node.accept(operatorExpressionChecker);
    }
    
    public void visit(ArithExprNode node) throws RuntimeException {
        // 委托给运算符表达式检查器处理
        node.accept(operatorExpressionChecker);
    }
    
    public void visit(CompExprNode node) throws RuntimeException {
        // 委托给运算符表达式检查器处理
        node.accept(operatorExpressionChecker);
    }
    
    public void visit(LazyExprNode node) throws RuntimeException {
        // 委托给运算符表达式检查器处理
        node.accept(operatorExpressionChecker);
    }
    
    public void visit(AssignExprNode node) {
        // 委托给运算符表达式检查器处理
        node.accept(operatorExpressionChecker);
    }
    
    public void visit(ComAssignExprNode node) throws RuntimeException {
        // 委托给运算符表达式检查器处理
        node.accept(operatorExpressionChecker);
    }
    
    // ==================== ComplexExpressionTypeChecker 访问方法 ====================
    
    public void visit(BorrowExprNode node) throws RuntimeException {
        // 委托给复杂表达式检查器处理
        node.accept(complexExpressionChecker);
    }
    
    public void visit(DerefExprNode node) throws RuntimeException {
        // 委托给复杂表达式检查器处理
        node.accept(complexExpressionChecker);
    }
    
    public void visit(TypeCastExprNode node) {
        // 委托给复杂表达式检查器处理
        node.accept(complexExpressionChecker);
    }
    
    public void visit(CallExprNode node) {
        // 委托给复杂表达式检查器处理
        node.accept(complexExpressionChecker);
    }
    
    public void visit(MethodCallExprNode node) throws RuntimeException {
        // 委托给复杂表达式检查器处理
        node.accept(complexExpressionChecker);
    }
    
    public void visit(FieldExprNode node) {
        // 委托给复杂表达式检查器处理
        node.accept(complexExpressionChecker);
    }
    
    public void visit(IndexExprNode node) throws RuntimeException {
        // 委托给复杂表达式检查器处理
        node.accept(complexExpressionChecker);
    }
    
    public void visit(ArrayExprNode node) throws RuntimeException {
        // 委托给复杂表达式检查器处理
        node.accept(complexExpressionChecker);
    }
    
    public void visit(StructExprNode node) throws RuntimeException {
        // 委托给复杂表达式检查器处理
        node.accept(complexExpressionChecker);
    }
    
    public void visit(FieldValNode node) throws RuntimeException {
        // 委托给复杂表达式检查器处理
        node.accept(complexExpressionChecker);
    }
    
    // ==================== StatementTypeChecker 访问方法 ====================
    
    public void visit(TypeExprNode node) {
        // 这是一个抽象基类，在正确的访问者模式实现中不应该到达这里
        throw new RuntimeException(
            "Cannot visit abstract TypeExprNode directly"
        );
    }
    
    public void visit(TypePathExprNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(TypeRefExprNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(TypeArrayExprNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(TypeUnitExprNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(IdentifierNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(PatternNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(IdPatNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(WildPatNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(RefPatNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(ItemNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(LetStmtNode node) throws RuntimeException {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(ExprStmtNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(SelfParaNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(ParameterNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(FunctionNode node) throws RuntimeException {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(StructNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(FieldNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(EnumNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(ConstItemNode node) throws RuntimeException {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(TraitNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
        node.accept(traitImplChecker);
    }
    
    public void visit(AssoItemNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    public void visit(ImplNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
        node.accept(traitImplChecker);
    }
    
    public void visit(BuiltinFunctionNode node) {
        // 委托给语句类型检查器处理
        node.accept(statementTypeChecker);
    }
    
    // ==================== 基础表达式节点访问方法 ====================
    
    public void visit(ExprNode node) {
        // 这是一个抽象基类，在正确的访问者模式实现中不应该到达这里
        throw new RuntimeException(
            "Cannot visit abstract ExprNode directly"
        );
    }
    
}