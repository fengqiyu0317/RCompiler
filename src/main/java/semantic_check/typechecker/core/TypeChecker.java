

import semantic_check.analyzer.TraitImplChecker;

/**
 * Main type checker class that coordinates type checking operations
 */
public class TypeChecker extends VisitorBase {
    private final TypeErrorCollector errorCollector;
    private final boolean throwOnError;
    private final TypeExtractor typeExtractor;
    private final TypeCheckerRefactored typeCheckerRefactored;
    private final StatementTypeChecker statementTypeChecker;
    
    // Mutability检查器
    private final MutabilityChecker mutabilityChecker;
    
    // Trait impl checker for checking trait implementations
    private final TraitImplChecker traitImplChecker;
    
    // Constant evaluator for evaluating constant expressions
    public final ConstantEvaluator constantEvaluator;
    
    public TypeChecker(boolean throwOnError) {
        this.errorCollector = new TypeErrorCollector();
        this.throwOnError = throwOnError;
        this.constantEvaluator = new ConstantEvaluator(throwOnError);
        
        // Initialize the helper classes
        this.typeExtractor = new TypeExtractor(errorCollector, throwOnError, constantEvaluator);
        this.expressionTypeChecker = new ExpressionTypeChecker(errorCollector, throwOnError, typeExtractor, constantEvaluator);
        this.typeCheckerRefactored = new TypeCheckerRefactored(errorCollector, throwOnError, typeExtractor, constantEvaluator);
        // 创建ControlFlowTypeChecker实例供StatementTypeChecker使用
        ControlFlowTypeChecker controlFlowTypeChecker = new ControlFlowTypeChecker(errorCollector, throwOnError, typeExtractor, constantEvaluator, typeCheckerRefactored);
        this.statementTypeChecker = new StatementTypeChecker(errorCollector, throwOnError, typeExtractor, typeCheckerRefactored, controlFlowTypeChecker);
        this.traitImplChecker = new TraitImplChecker(errorCollector, throwOnError, typeExtractor);
        
        // Set up circular dependency between TypeExtractor and ExpressionTypeChecker
        this.typeExtractor.setExpressionTypeChecker(expressionTypeChecker);
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
    
    // Delegate expression visits to TypeCheckerRefactored
    public void visit(LiteralExprNode node) {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(PathExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(PathExprSegNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(GroupExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(UnderscoreExprNode node) {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(ArithExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(CompExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(LazyExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(NegaExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(AssignExprNode node) {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(ComAssignExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(CallExprNode node) {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(MethodCallExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(FieldExprNode node) {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(IndexExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(BorrowExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(DerefExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(TypeCastExprNode node) {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(BlockExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(IfExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(LoopExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(BreakExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(ContinueExprNode node) {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(ReturnExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(ArrayExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(StructExprNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    public void visit(FieldValNode node) throws RuntimeException {
        typeCheckerRefactored.visit(node);
    }
    
    
    public void visit(ExprStmtNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(ItemNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(FunctionNode node) throws RuntimeException {
        statementTypeChecker.visit(node);
    }
    
    public void visit(LetStmtNode node) throws RuntimeException {
        statementTypeChecker.visit(node);
    }
    
    public void visit(IdentifierNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(TypePathExprNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(AssoItemNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(ImplNode node) {
        statementTypeChecker.visit(node);
        traitImplChecker.visit(node);
    }
    
    public void visit(ConstItemNode node) throws RuntimeException {
        statementTypeChecker.visit(node);
    }
    
    public void visit(StructNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(EnumNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(FieldNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(TraitNode node) {
        statementTypeChecker.visit(node);
        traitImplChecker.visit(node);
    }
    
    public void visit(SelfParaNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(ParameterNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(PatternNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(IdPatNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(WildPatNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(RefPatNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(TypeRefExprNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(TypeArrayExprNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(TypeUnitExprNode node) {
        statementTypeChecker.visit(node);
    }
    
    public void visit(BuiltinFunctionNode node) {
        statementTypeChecker.visit(node);
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
}