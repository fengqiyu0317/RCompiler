

import semantic_check.type.*;
import semantic_check.error.TypeErrorCollector;

/**
 * 重构后的表达式类型检查器
 * 作为各个专门检查器的协调器，提供统一的接口给外部使用
 */
public class TypeCheckerRefactored extends VisitorBase {
    private final TypeErrorCollector errorCollector;
    private final boolean throwOnError;
    private final TypeExtractor typeExtractor;
    private final ConstantEvaluator constantEvaluator;
    private final ExpressionTypeContext context;
    private final ControlFlowTypeChecker controlFlowTypeChecker;
    private final StatementTypeChecker statementTypeChecker;
    
    // 专门的类型检查器
    private final SimpleExpressionTypeChecker simpleExpressionChecker;
    private final OperatorExpressionTypeChecker operatorExpressionChecker;
    private final ComplexExpressionTypeChecker complexExpressionChecker;
    
    // Mutability检查器
    private final MutabilityChecker mutabilityChecker;
    
    public TypeCheckerRefactored(
        TypeErrorCollector errorCollector,
        boolean throwOnError,
        TypeExtractor typeExtractor,
        ConstantEvaluator constantEvaluator
    ) {
        this.errorCollector = errorCollector;
        this.throwOnError = throwOnError;
        this.typeExtractor = typeExtractor;
        this.constantEvaluator = constantEvaluator;
        
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
        
        // 设置mutability检查器到各个专门的检查器
        this.simpleExpressionChecker.setMutabilityChecker(mutabilityChecker);
        this.operatorExpressionChecker.setMutabilityChecker(mutabilityChecker);
        this.complexExpressionChecker.setMutabilityChecker(mutabilityChecker);
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
        StringBuilder context = new StringBuilder();
        
        // 添加函数上下文（如果可用）
        if (context.getCurrentFunction() != null) {
            FunctionNode funcNode = context.getCurrentFunction();
            if (funcNode.name != null) {
                context.append("in function '").append(funcNode.name.name).append("' ");
            }
        }
        
        // 添加当前Self类型（如果可用）
        if (context.getCurrentSelfType() != null) {
            context.append("(Self: ").append(context.getCurrentSelfType()).append(") ");
        }
        
        return context.toString();
    }
    
    // ==================== 访问方法 ====================
    
    // 基础表达式节点 - 仅作为协调器，委托给适当的专门检查器
    public void visit(ExprNode node) {
        // 这是一个抽象基类，在正确的访问者模式实现中不应该到达这里
        throw new RuntimeException(
            "Cannot visit abstract ExprNode directly"
        );
    }
    
    public void visit(ExprWithBlockNode node) {
        // 委托给控制流类型检查器处理
        controlFlowTypeChecker.visit(node);
    }
    
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
    
    public void visit(OperExprNode node) {
        // 委托给运算符表达式检查器处理
        operatorExpressionChecker.visit(node);
    }
    
    
    // 控制流表达式 - 使用ControlFlowTypeChecker
    public void visit(BlockExprNode node) {
        node.accept(controlFlowTypeChecker);
    }
    
    public void visit(IfExprNode node) {
        controlFlowTypeChecker.visit(node);
    }
    
    public void visit(LoopExprNode node) {
        node.accept(controlFlowTypeChecker);
    }
    
    public void visit(BreakExprNode node) {
        node.accept(controlFlowTypeChecker);
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
    public void visit(ContinueExprNode node) {
        node.accept(controlFlowTypeChecker);
    }
    
    // ==================== 类型表达式访问方法 ====================
    
    public void visit(TypeExprNode node) {
        // 这是一个抽象基类，在正确的访问者模式实现中不应该到达这里
        throw new RuntimeException(
            "Cannot visit abstract TypeExprNode directly"
        );
    }
    
    public void visit(TypePathExprNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(TypeRefExprNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(TypeArrayExprNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(TypeUnitExprNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    // ==================== 其他节点访问方法 ====================
    
    public void visit(IdentifierNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(PatternNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(IdPatNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(WildPatNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(RefPatNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    // ==================== 其他节点访问方法 ====================
    
    public void visit(ItemNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(LetStmtNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(ExprStmtNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(SelfParaNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(ParameterNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    // ==================== Item 子类访问方法 ====================
    
    public void visit(FunctionNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(StructNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(FieldNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(EnumNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(ConstItemNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(TraitNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(AssoItemNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(ImplNode node) {
        // 委托给语句类型检查器处理
        statementTypeChecker.visit(node);
    }
    
    public void visit(ReturnExprNode node) {
        node.accept(controlFlowTypeChecker);
    }
    
}