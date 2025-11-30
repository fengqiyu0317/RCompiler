# Expression Type Checking Design Document

## 概述

本文档设计了一个全面的Expression类型检查系统，用于为RCompiler中的每个Expression确定其类型。该系统将扩展现有的命名空间语义检查，添加类型推断和类型验证功能，确保所有Expression都有明确的类型，并且类型操作符合Rust的类型规则。

## 目标

1. 为每个Expression节点确定其类型
2. 实现类型推断机制，处理隐式类型推导
3. 验证类型操作的合法性（如算术运算、比较运算等）
4. 处理类型转换和类型强制转换
5. 支持泛型和trait相关的类型检查
6. 提供详细的类型错误报告

## 当前状态

RCompiler目前具备：
- 完整的词法分析器和语法分析器
- 基于命名空间的语义检查系统
- AST节点定义和访问者模式基础
- 类型表达式解析器（TypeParser）

## 类型系统设计

### 0. AST节点扩展

为了支持类型缓存，需要扩展ExprNode类，添加唯一ID：

```java
// ExprNode represents an expression <expression>.
// The grammer for expression is:
// <expression> = <exprwithblock> | <exprwithoutblock>
class ExprNode extends ASTNode {
    // 存储该表达式节点对应的上下文
    private Context context;
    
    // 存储该表达式节点的类型
    private Type type;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Context getContext() {
        return context;
    }
    
    public void setContext(Context context) {
        this.context = context;
    }
    
    public Type getType() {
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
}

// IdentifierNode represents an identifier <identifier>.
// an identifier is just a string, so we just need to store the string here.
class IdentifierNode extends ASTNode {
    String name;
    
    // 存储该标识符对应的符号
    private Symbol symbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}

// PathExprNode represents a path expression <pathexpr>.
// The grammer for path expression is:
// <pathexpr> = <pathseg> (:: <pathseg>)?
class PathExprNode extends ExprWithoutBlockNode {
    PathExprSegNode LSeg;
    PathExprSegNode RSeg; // can be null
    
    // 存储该路径表达式对应的符号
    private Symbol symbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}

// PathExprSegNode represents a segment in a path expression <pathseg>.
// The grammer for path segment is:
// <pathseg> = <identifier> | self | Self
// patternType can be one of the following: IDENT, SELF, SELF_TYPE.
class PathExprSegNode extends ASTNode {
    patternSeg_t patternType;
    IdentifierNode name;
    
    // 存储该路径段对应的符号
    private Symbol symbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}
```

### 1. 类型表示

```java
// 基础类型接口
public interface Type {
    boolean equals(Type other);
    String toString();
    Type getBaseType(); // 获取基础类型，处理引用类型
}

// 具体类型实现
public class PrimitiveType implements Type {
    public enum PrimitiveKind {
        INT,     // 未确定的整型（用于类型推断）
        I32, U32, USIZE, ISIZE, BOOL, CHAR, STR
    }
    
    private final PrimitiveKind kind;
    
    // 构造函数和方法实现
    
}

public class ReferenceType implements Type {
    private final Type innerType;
    private final boolean isMutable;
    
    // 构造函数和方法实现
    
    public boolean isMutable() {
        return isMutable;
    }
    
    public Type getInnerType() {
        return innerType;
    }
}

public class ArrayType implements Type {
    private final Type elementType;
    private final long size;
    
    // 构造函数和方法实现
    
}

public class StructType implements Type {
    private final String name;
    private final Map<String, Type> fields;
    private final Symbol structSymbol;
    
    // 构造函数和方法实现
    
}

public class EnumType implements Type {
    private final String name;
    private final List<String> variants;
    private final Symbol enumSymbol;
    
    // 构造函数和方法实现
    
}

public class TraitType implements Type {
    private final String name;
    private final Symbol traitSymbol;
    
    // 构造函数和方法实现
    
}

public class FunctionType implements Type {
    private final List<Type> parameterTypes;
    private final Type returnType;
    private final boolean isMethod;
    
    public FunctionType(List<Type> parameterTypes, Type returnType, boolean isMethod) {
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
        this.isMethod = isMethod;
    }
    
    public FunctionType(List<Type> parameterTypes, Type returnType) {
        this(parameterTypes, returnType, false);
    }
    
    // Getter方法
    public List<Type> getParameterTypes() {
        return parameterTypes;
    }
    
    public Type getReturnType() {
        return returnType;
    }
    
    public boolean isMethod() {
        return isMethod;
    }
    
    // 其他方法实现...
}

// 结构体构造函数类型
public class StructConstructorType implements Type {
    private final StructType structType;
    
    public StructConstructorType(StructType structType) {
        this.structType = structType;
    }
    
    public StructType getStructType() {
        return structType;
    }
    
    @Override
    public boolean equals(Type other) {
        if (other instanceof StructConstructorType) {
            return structType.equals(((StructConstructorType) other).structType);
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "StructConstructor<" + structType.toString() + ">";
    }
    
    @Override
    public Type getBaseType() {
        return this;
    }
}

// 特殊类型
public class UnitType implements Type {
    public static final UnitType INSTANCE = new UnitType();
    
    private UnitType() {}
    
    // 方法实现
    
}

// Never类型（用于break、continue、return等控制流语句）
public class NeverType implements Type {
    public static final NeverType INSTANCE = new NeverType();
    
    private NeverType() {}
    
    @Override
    public boolean equals(Type other) {
        return other instanceof NeverType;
    }
    
    @Override
    public String toString() {
        return "!";
    }
    
    @Override
    public Type getBaseType() {
        return this;
    }
}
```

## 类型检查器设计

### 1. 类型检查器基础结构

```java
import java.util.function.Supplier;

public class TypeChecker extends VisitorBase {
    private final TypeErrorCollector errorCollector;
    private boolean throwOnError;
    private Type currentType; // 表示Self指代的类型
    
    // 控制流上下文栈
    private Stack<ControlFlowContext> contextStack = new Stack<>();
    
    // 控制流上下文类型
    private enum ControlFlowContextType {
        LOOP,      // loop循环
        WHILE,     // while循环
        FUNCTION,   // 函数
    }
    
    // 控制流上下文类
    private static class ControlFlowContext {
        private final ControlFlowContextType type;
        private final ASTNode node;  // 对应的AST节点
        private List<Type> breakTypes;  // 收集的break类型（仅对loop有效）
        
        public ControlFlowContext(ControlFlowContextType type, ASTNode node) {
            this.type = type;
            this.node = node;
            this.breakTypes = new ArrayList<>();
        }
        
        public ControlFlowContextType getType() {
            return type;
        }
        
        public ASTNode getNode() {
            return node;
        }
        
        public List<Type> getBreakTypes() {
            return breakTypes;
        }
    }
    
    public TypeChecker(boolean throwOnError) {
        this.errorCollector = new TypeErrorCollector();
        this.throwOnError = throwOnError;
        this.currentType = null;
    }
    
    // 获取表达式节点的上下文
    private Context getContext(ExprNode expr) {
        return expr.getContext();
    }
    
    // 获取表达式节点的类型
    private Type getType(ExprNode expr) {
        return expr.getType();
    }
    
    // 设置表达式节点的类型
    private void setType(ExprNode expr, Type type) {
        expr.setType(type);
    }
    
    // 抛出类型错误
    private void throwTypeError(TypeCheckException.Type errorType, String message, ASTNode node)
            throws TypeCheckException {
        TypeCheckException error = new TypeCheckException(errorType, message, node);
        if (throwOnError) {
            throw error;
        } else {
            errorCollector.addError(error);
        }
    }
    
    // 获取错误收集器
    public TypeErrorCollector getErrorCollector() {
        return errorCollector;
    }
    
    // 检查是否有错误
    public boolean hasErrors() {
        return errorCollector.hasErrors();
    }
    
    // 抛出第一个错误（如果有）
    public void throwFirstError() throws TypeCheckException {
        errorCollector.throwFirstError();
    }
    
    /**
     * 检查Expression是否可以作为赋值目标（左值）
     * 在Rust中，只有以下四种表达式可以作为左值：
     * 1. PathExprNode - 路径表达式（变量、字段等）
     * 2. FieldExprNode - 字段访问表达式
     * 3. IndexExprNode - 索引访问表达式
     * 4. DerefExprNode - 解引用表达式
     */
    private boolean isAssignable(ExprNode expr) throws TypeCheckException {
        // 在Rust中，这四种类型的表达式一定可以作为左值
        // 其他类型的表达式一定不可以作为左值
        return expr instanceof PathExprNode ||
               expr instanceof FieldExprNode ||
               expr instanceof IndexExprNode ||
               expr instanceof DerefExprNode;
    }
    
    // 获取当前Self的类型（在方法中）和Self构造器类型（在impl块中）
    private Type getCurrentSelfType() throws TypeCheckException {
        // 返回当前Self指代的类型
        return currentType;
    }
    
    // 设置当前Self指代的类型
    private void setCurrentType(Type type) {
        this.currentType = type;
    }
    
    // 清除当前Self指代的类型
    private void clearCurrentType() {
        this.currentType = null;
    }
    
    // 进入loop上下文
    private void enterLoopContext(LoopExprNode node) {
        contextStack.push(new ControlFlowContext(ControlFlowContextType.LOOP, node));
    }
    
    // 进入while上下文
    private void enterWhileContext(LoopExprNode node) {
        contextStack.push(new ControlFlowContext(ControlFlowContextType.WHILE, node));
    }
    
    // 退出循环上下文（loop/while）
    private void exitLoopContext() {
        if (!contextStack.isEmpty()) {
            ControlFlowContextType type = contextStack.peek().getType();
            if (type == ControlFlowContextType.LOOP ||
                type == ControlFlowContextType.WHILE) {
                contextStack.pop();
            }
        }
    }
    
    // 进入函数上下文
    private void enterFunctionContext(FunctionNode node) {
        contextStack.push(new ControlFlowContext(ControlFlowContextType.FUNCTION, node));
    }
    
    // 退出函数上下文
    private void exitFunctionContext() {
        if (!contextStack.isEmpty() &&
            contextStack.peek().getType() == ControlFlowContextType.FUNCTION) {
            contextStack.pop();
        }
    }
    
    // 查找最近的循环上下文（loop/while）
    private ControlFlowContext findNearestLoopContext() throws TypeCheckException {
        for (int i = contextStack.size() - 1; i >= 0; i--) {
            ControlFlowContext context = contextStack.get(i);
            if (context.getType() == ControlFlowContextType.LOOP ||
                context.getType() == ControlFlowContextType.WHILE) {
                return context;
            }
        }
        return null; // 没有找到循环上下文
    }
    
    // 查找最近的函数上下文
    private ControlFlowContext findNearestFunctionContext() throws TypeCheckException {
        for (int i = contextStack.size() - 1; i >= 0; i--) {
            ControlFlowContext context = contextStack.get(i);
            if (context.getType() == ControlFlowContextType.FUNCTION) {
                return context;
            }
        }
        return null; // 没有找到函数上下文
    }
    
    // 从符号中提取类型（只处理value_namespace中的符号）
    // 使用记忆化搜索：如果symbol的type字段不为空，直接返回
    private Type extractTypeFromSymbol(Symbol symbol) throws TypeCheckException {
        if (symbol == null) {
            throw new TypeCheckException(
                TypeCheckException.Type.NULL_SYMBOL,
                "Cannot extract type from null symbol",
                null
            );
        }
        
        // 记忆化搜索：如果type已经设置，直接返回
        Type cachedType = symbol.getType();
        if (cachedType != null) {
            return cachedType;
        }
        
        // 获取符号的声明节点
        ASTNode declaration = symbol.getDeclaration();
        
        switch (symbol.getKind()) {
            case FUNCTION:
                // 函数类型：从函数声明中提取参数和返回类型
                if (declaration instanceof FunctionNode) {
                    FunctionNode funcNode = (FunctionNode) declaration;
                    List<Type> paramTypes = new ArrayList<>();
                    
                    // 判断是否是方法（有self参数）
                    boolean isMethod = funcNode.selfPara != null;
                    
                    // 处理普通参数
                    if (funcNode.parameters != null) {
                        for (ParameterNode param : funcNode.parameters) {
                            Type paramType = extractTypeFromTypeNode(param.type);
                            paramTypes.add(paramType);
                        }
                    }
                    
                    // 处理返回类型
                    Type returnType = funcNode.returnType != null ?
                                     extractTypeFromTypeNode(funcNode.returnType) :
                                     new UnitType();
                    
                    FunctionType functionType = new FunctionType(paramTypes, returnType, isMethod);
                    setSymbolType(symbol, functionType);
                    return functionType;
                }
                break;
                
            case STRUCT_CONSTRUCTOR:
                // 结构体构造函数返回StructConstructorType
                // 直接从symbol的declaration字段获取对应的StructNode
                ASTNode structDecl = symbol.getDeclaration();
                if (structDecl instanceof StructNode) {
                    // 从StructNode中提取结构体类型
                    StructNode structNode = (StructNode) structDecl;
                    Map<String, Type> fields = new HashMap<>();
                    
                    if (structNode.fields != null) {
                        for (FieldNode field : structNode.fields) {
                            Type fieldType = extractTypeFromTypeNode(field.type);
                            fields.put(field.name.name, fieldType);
                        }
                    }
                    
                    StructType structType = new StructType(symbol.getName(), fields, symbol);
                    StructConstructorType constructorType = new StructConstructorType(structType);
                    setSymbolType(symbol, constructorType);
                    return constructorType;
                }
                break;
                
            case ENUM_VARIANT_CONSTRUCTOR:
                // 枚举变体构造函数返回枚举类型
                // 现在enum_variant_constructor的ASTNode直接指向enum节点
                ASTNode enumDecl = symbol.getDeclaration();
                if (enumDecl instanceof EnumNode) {
                    // 从EnumNode中提取枚举类型
                    EnumNode enumNode = (EnumNode) enumDecl;
                    List<String> variants = new ArrayList<>();
                    
                    if (enumNode.variants != null) {
                        for (IdentifierNode variant : enumNode.variants) {
                            variants.add(variant.name);
                        }
                    }
                    
                    // 直接使用enumNode的名称作为枚举名称
                    String enumName = enumNode.name.name;
                    
                    EnumType enumType = new EnumType(enumName, variants, symbol);
                    setSymbolType(symbol, enumType);
                    return enumType;
                }
                throw new TypeCheckException(TypeCheckException.Type.INVALID_TYPE_EXTRACTION,
                                           "Cannot determine enum type for variant: " + symbol.getName(),
                                           symbol.getDeclaration());
                
            case SELF_CONSTRUCTOR:
                // Self构造函数返回当前类型
                Type selfType = getCurrentSelfType();
                setSymbolType(symbol, selfType);
                return selfType;
                
            case CONSTANT:
                // 常量类型：从常量声明中提取类型信息
                if (declaration instanceof ConstItemNode) {
                    ConstItemNode constNode = (ConstItemNode) declaration;
                    if (constNode.type != null) {
                        Type constType = extractTypeFromTypeNode(constNode.type);
                        setSymbolType(symbol, constType);
                        return constType;
                    }
                    // 如果没有显式类型，尝试从值推断
                    if (constNode.value != null) {
                        throw new TypeCheckException(
                            TypeCheckException.Type.TYPE_INFERENCE_NOT_SUPPORTED,
                            "Cannot infer type from constant value without explicit type annotation",
                            constNode
                        );
                    }
                }
                break;
                
            case PARAMETER:
                // 参数类型：从参数声明中提取类型信息
                if (declaration instanceof ParameterNode) {
                    ParameterNode paramNode = (ParameterNode) declaration;
                    if (paramNode.type != null) {
                        Type paramType = extractTypeFromTypeNode(paramNode.type);
                        setSymbolType(symbol, paramType);
                        return paramType;
                    }
                }
                break;
                
            case LOCAL_VARIABLE:
                // 局部变量类型：从let语句中提取类型信息
                if (declaration instanceof LetStmtNode) {
                    LetStmtNode letNode = (LetStmtNode) declaration;
                    if (letNode.type != null) {
                        Type varType = extractTypeFromTypeNode(letNode.type);
                        setSymbolType(symbol, varType);
                        return varType;
                    }
                    // 如果没有显式类型，尝试从值推断
                    if (letNode.value != null) {
                        throw new TypeCheckException(
                            TypeCheckException.Type.TYPE_INFERENCE_NOT_SUPPORTED,
                            "Cannot infer type from let value without explicit type annotation",
                            letNode
                        );
                    }
                }
                break;
                
            default:
                throw new TypeCheckException(
                    TypeCheckException.Type.INVALID_TYPE_EXTRACTION,
                    "Cannot extract type from symbol kind: " + symbol.getKind(),
                    symbol.getDeclaration()
                );
        }
        
        throw new TypeCheckException(
            TypeCheckException.Type.UNSUPPORTED_SYMBOL_KIND,
            "Unsupported symbol kind for type extraction: " + symbol.getKind(),
            symbol.getDeclaration()
        );
    }
    
    // 辅助方法：设置符号的类型（用于记忆化搜索）
    private void setSymbolType(Symbol symbol, Type type) {
        if (symbol != null) {
            symbol.setType(type);
        }
    }
    
    /**
     * 查找两个类型的共同类型
     * 用于类型推断，例如在if表达式、数组元素类型统一等场景
     *
     * 规则：
     * 1. 如果两个类型相同，直接返回该类型
     * 2. 如果两个都是整型，那么看看其中是否有int，如果有就可以和别的整型兼容
     * 3. 其他情况返回null
     *
     * @param type1 第一个类型
     * @param type2 第二个类型
     * @return 共同类型，如果不存在则返回null
     */
    private Type findCommonType(Type type1, Type type2) {
        // 如果两个类型相同，直接返回
        if (type1.equals(type2)) {
            return type1;
        }
        
        // 如果两个都是整型，那么看看其中是否有int，如果有就可以和别的整型兼容
        if (type1 instanceof PrimitiveType && type2 instanceof PrimitiveType) {
            PrimitiveType.PrimitiveKind kind1 = ((PrimitiveType)type1).getKind();
            PrimitiveType.PrimitiveKind kind2 = ((PrimitiveType)type2).getKind();
            
            // 检查是否为整型
            if (isNumericType(type1) && isNumericType(type2)) {
                // 如果其中一个是未确定的整型(INT)，返回另一个类型
                if (kind1 == PrimitiveType.PrimitiveKind.INT) {
                    return type2;
                }
                if (kind2 == PrimitiveType.PrimitiveKind.INT) {
                    return type1;
                }
            }
        }
        
        // 其他情况返回null
        return null;
    }
    
    /**
     * 检查类型是否为数值类型（整型）
     *
     * @param type 要检查的类型
     * @return 如果是数值类型返回true，否则返回false
     */
    private boolean isNumericType(Type type) {
        if (type instanceof PrimitiveType) {
            PrimitiveType.PrimitiveKind kind = ((PrimitiveType)type).getKind();
            return kind == PrimitiveType.PrimitiveKind.INT ||
                   kind == PrimitiveType.PrimitiveKind.I32 ||
                   kind == PrimitiveType.PrimitiveKind.U32 ||
                   kind == PrimitiveType.PrimitiveKind.USIZE ||
                   kind == PrimitiveType.PrimitiveKind.ISIZE;
        }
        return false;
    }
    
    /**
     * 从类型节点中提取类型信息
     *
     * @param typeNode 类型节点
     * @return 类型信息
     */
    private Type extractTypeFromTypeNode(ASTNode typeNode) throws TypeCheckException {
        // 这里应该根据实际的类型节点结构来提取类型信息
        // 简化实现，实际需要根据具体的AST节点类型来实现
        if (typeNode == null) {
            throw new TypeCheckException(
                TypeCheckException.Type.NULL_TYPE_NODE,
                "Type node is null",
                typeNode
            );
        }
        
        // 简化实现，返回一个默认的整型
        // 实际实现需要根据typeNode的具体类型来解析
        return new PrimitiveType(PrimitiveType.PrimitiveKind.INT);
    }
    
    /**
     * 解析类型
     *
     * @param typeNode 类型节点
     * @return 解析后的类型
     */
    private Type resolveType(ASTNode typeNode) throws TypeCheckException {
        // 简化实现，直接调用extractTypeFromTypeNode
        return extractTypeFromTypeNode(typeNode);
    }
```

### 2. Expression Type Checking Methods

#### 2.1 LiteralExprNode

```java
@Override
public void visit(LiteralExprNode node) throws TypeCheckException {
    Type type;
    
    switch (node.literalType) {
        case INT:
            // 未确定的整型，用于类型推断
            type = new PrimitiveType(PrimitiveType.PrimitiveKind.INT);
            break;
        case I32:
            type = new PrimitiveType(PrimitiveType.PrimitiveKind.I32);
            break;
        case U32:
            type = new PrimitiveType(PrimitiveType.PrimitiveKind.U32);
            break;
        case USIZE:
            type = new PrimitiveType(PrimitiveType.PrimitiveKind.USIZE);
            break;
        case ISIZE:
            type = new PrimitiveType(PrimitiveType.PrimitiveKind.ISIZE);
            break;
        case BOOL:
            type = new PrimitiveType(PrimitiveType.PrimitiveKind.BOOL);
            break;
        case CHAR:
            type = new PrimitiveType(PrimitiveType.PrimitiveKind.CHAR);
            break;
        case STRING:
        case CSTRING:
            type = new PrimitiveType(PrimitiveType.PrimitiveKind.STR);
            break;
        default:
            throwTypeError(TypeCheckException.Type.UNKNOWN_LITERAL_TYPE,
                         "Unknown literal type: " + node.literalType,
                         node);
            return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    setType(node, type);
}
```

#### 2.2 IdentifierNode

```java
@Override
public void visit(IdentifierNode node) throws TypeCheckException {
    
    // 直接从IdentifierNode获取已存储的符号信息
    Symbol symbol = node.getSymbol();
    if (symbol != null) {
        // 直接从存储的符号中获取类型
        Type type = extractTypeFromSymbol(symbol);
        setType(node, type);
        return;
    }
    
    // 如果没有存储符号信息，则报错
    throwTypeError(TypeCheckException.Type.UNDEFINED_VARIABLE,
                  "Undefined variable: " + node.name,
                  node);
    return; // 不会执行，因为throwOnError默认为true时会抛出异常
}
```

#### 2.3 ArithExprNode

```java
@Override
public void visit(ArithExprNode node) throws TypeCheckException {
    // 首先检查操作数的类型
    node.left.accept(this);
    node.right.accept(this);
    
    Type leftType = getType(node.left);
    Type rightType = getType(node.right);
    
    // 检查操作数是否有效
    if (leftType == null || rightType == null) {
        throwTypeError(TypeCheckException.Type.NULL_OPERAND_TYPE,
                      "Cannot perform arithmetic operation on null type",
                      node);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    // 使用findCommonType判断合并是否合法
    Type resultType = findCommonType(leftType, rightType);
    
    // 检查是否为整型
    if (resultType != null && isNumericType(resultType)) {
        setType(node, resultType);
        return;
    }
    
    // 类型不匹配，不支持自动类型提升
    throwTypeError(TypeCheckException.Type.INVALID_ARITHMETIC_OPERATION,
                  String.format("Invalid arithmetic operation: %s %s %s",
                               leftType, node.operator, rightType),
                  node);
}
```

#### 2.4 CompExprNode

```java
@Override
public void visit(CompExprNode node) throws TypeCheckException {
    // 首先检查操作数的类型
    node.left.accept(this);
    node.right.accept(this);
    
    Type leftType = getType(node.left);
    Type rightType = getType(node.right);
    
    // 检查操作数是否有效
    if (leftType == null || rightType == null) {
        throwTypeError(TypeCheckException.Type.NULL_OPERAND_TYPE,
                      "Cannot perform comparison on null type",
                      node);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    // 使用findCommonType判断合并是否合法
    Type commonType = findCommonType(leftType, rightType);
    
    // 检查是否为整型
    if (commonType != null && isNumericType(commonType)) {
        // 比较Expression总是返回bool类型
        setType(node, new PrimitiveType(PrimitiveType.PrimitiveKind.BOOL));
        return;
    }
    
    // 类型不匹配，不支持比较操作
    throwTypeError(TypeCheckException.Type.INVALID_COMPARISON,
                  String.format("Invalid comparison: %s %s %s",
                               leftType, node.operator, rightType),
                  node);
}
```

#### 2.5 LazyExprNode

```java
@Override
public void visit(LazyExprNode node) throws TypeCheckException {
    // 首先检查操作数的类型
    node.left.accept(this);
    node.right.accept(this);
    
    Type leftType = getType(node.left);
    Type rightType = getType(node.right);
    
    // 检查操作数是否为bool类型
    if (!(leftType instanceof PrimitiveType &&
          ((PrimitiveType)leftType).getKind() == PrimitiveType.PrimitiveKind.BOOL)) {
        throwTypeError(TypeCheckException.Type.INVALID_LOGICAL_OPERATION,
                      String.format("Left operand of logical operation must be bool, got %s", leftType),
                      node.left);
    }
    
    if (!(rightType instanceof PrimitiveType &&
          ((PrimitiveType)rightType).getKind() == PrimitiveType.PrimitiveKind.BOOL)) {
        throwTypeError(TypeCheckException.Type.INVALID_LOGICAL_OPERATION,
                      String.format("Right operand of logical operation must be bool, got %s", rightType),
                      node.right);
    }
    
    // 逻辑Expression总是返回bool类型
    setType(node, new PrimitiveType(PrimitiveType.PrimitiveKind.BOOL));
}
```

#### 2.6 AssignExprNode

```java
@Override
public void visit(AssignExprNode node) throws TypeCheckException {
    // 首先检查左右操作数的类型
    node.left.accept(this);
    node.right.accept(this);
    
    Type leftType = getType(node.left);
    Type rightType = getType(node.right);
    
    // 检查左操作数是否是可赋值的
    if (!isAssignable(node.left)) {
        throwTypeError(TypeCheckException.Type.INVALID_ASSIGNMENT_TARGET,
                      "Left side of assignment is not assignable",
                      node.left);
    }
    
    // 检查类型是否兼容
    if (leftType == null || rightType == null) {
        throwTypeError(TypeCheckException.Type.NULL_OPERAND_TYPE,
                      "Cannot perform assignment on null type",
                      node);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    // 使用findCommonType判断合并是否合法
    Type commonType = findCommonType(leftType, rightType);
    
    // 检查是否为整型
    if (commonType != null && isNumericType(commonType)) {
        // 赋值Expression的类型是单元类型
        setType(node, UnitType.INSTANCE);
        return;
    }
    
    // 类型不匹配，不支持赋值操作
    throwTypeError(TypeCheckException.Type.TYPE_MISMATCH,
                  String.format("Type mismatch in assignment: %s = %s", leftType, rightType),
                  node);
}
```

#### 2.7 ComAssignExprNode

```java
@Override
public void visit(ComAssignExprNode node) throws TypeCheckException {
    // 首先检查左右操作数的类型
    node.left.accept(this);
    node.right.accept(this);
    
    Type leftType = getType(node.left);
    Type rightType = getType(node.right);
    
    // 检查左操作数是否是可赋值的
    if (!isAssignable(node.left)) {
        throwTypeError(TypeCheckException.Type.INVALID_ASSIGNMENT_TARGET,
                      "Left side of compound assignment is not assignable",
                      node.left);
    }
    
    // 检查操作数是否有效
    if (leftType == null || rightType == null) {
        throwTypeError(TypeCheckException.Type.NULL_OPERAND_TYPE,
                      "Cannot perform compound assignment on null type",
                      node);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    // 使用findCommonType判断合并是否合法
    Type commonType = findCommonType(leftType, rightType);
    
    // 检查是否为整型
    if (commonType != null && isNumericType(commonType)) {
        // 复合赋值Expression的类型是单元类型
        setType(node, UnitType.INSTANCE);
        return;
    }
    
    // 类型不匹配，不支持复合赋值操作
    throwTypeError(TypeCheckException.Type.INVALID_COMPOUND_ASSIGNMENT,
                  String.format("Invalid compound assignment: %s %s %s",
                               leftType, node.operator, rightType),
                  node);
}
```

#### 2.8 CallExprNode

```java
@Override
public void visit(CallExprNode node) throws TypeCheckException {
    // 首先检查函数表达式的类型
    node.function.accept(this);
    
    Type funcType = getType(node.function);
    
    // 检查参数的类型
    List<Type> argTypes = new ArrayList<>();
    if (node.arguments != null) {
        for (ExprNode arg : node.arguments) {
            arg.accept(this);
            argTypes.add(getType(arg));
        }
    }
    
    // 检查函数类型是否有效
    if (!(funcType instanceof FunctionType)) {
        throwTypeError(TypeCheckException.Type.NOT_A_FUNCTION,
                      String.format("Attempt to call non-function value of type %s", funcType),
                      node.function);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    FunctionType functionType = (FunctionType) funcType;
    List<Type> paramTypes = functionType.getParameterTypes();
    
    // 检查参数数量
    if (argTypes.size() != paramTypes.size()) {
        throwTypeError(TypeCheckException.Type.WRONG_ARGUMENT_COUNT,
                      String.format("Wrong number of arguments: expected %d, got %d",
                                   paramTypes.size(), argTypes.size()),
                      node);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    // 检查每个参数的类型
    for (int i = 0; i < argTypes.size(); i++) {
        Type argType = argTypes.get(i);
        Type paramType = paramTypes.get(i);
        
        if (!argType.equals(paramType)) {
            throwTypeError(TypeCheckException.Type.ARGUMENT_TYPE_MISMATCH,
                          String.format("Argument %d type mismatch: expected %s, got %s",
                                       i + 1, paramType, argType),
                          node.arguments.get(i));
        }
    }
    
    // 函数调用的类型是函数的返回类型
    setType(node, functionType.getReturnType());
}
```

#### 2.9 MethodCallExprNode

```java
@Override
public void visit(MethodCallExprNode node) throws TypeCheckException {
    // 首先检查接收器的类型
    node.receiver.accept(this);
    
    Type receiverType = getType(node.receiver);
    
    // 检查参数的类型
    List<Type> argTypes = new ArrayList<>();
    if (node.arguments != null) {
        for (ExprNode arg : node.arguments) {
            arg.accept(this);
            argTypes.add(getType(arg));
        }
    }
    
    // 直接访问methodName的符号信息
    // methodName是PathExprSegNode，所以调用它之后可以根据extractFromSymbol函数得到methodName的type
    node.methodName.accept(this);
    Type methodType = getType(node.methodName);
    
    // 检查方法类型是否有效
    if (!(methodType instanceof FunctionType)) {
        throwTypeError(TypeCheckException.Type.NOT_A_FUNCTION,
                      String.format("Method '%s' is not a function, got %s",
                                   node.methodName.name, methodType),
                      node.methodName);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    FunctionType functionType = (FunctionType) methodType;
    List<Type> paramTypes = functionType.getParameterTypes();
    
    // 检查参数数量（不包括接收器）
    if (argTypes.size() != paramTypes.size()) {
        throwTypeError(TypeCheckException.Type.WRONG_ARGUMENT_COUNT,
                      String.format("Wrong number of arguments for method '%s': expected %d, got %d",
                                   node.methodName.name, paramTypes.size(), argTypes.size()),
                      node);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    // 检查每个参数的类型
    for (int i = 0; i < argTypes.size(); i++) {
        Type argType = argTypes.get(i);
        Type paramType = paramTypes.get(i);
        
        if (!argType.equals(paramType)) {
            throwTypeError(TypeCheckException.Type.ARGUMENT_TYPE_MISMATCH,
                          String.format("Argument %d type mismatch for method '%s': expected %s, got %s",
                                       i + 1, node.methodName.name, paramType, argType),
                          node.arguments.get(i));
        }
    }
    
    // 方法调用的类型是方法的返回类型
    setType(node, functionType.getReturnType());
}
```

#### 2.10 FieldExprNode

```java
@Override
public void visit(FieldExprNode node) throws TypeCheckException {
    // 首先通过递归调用找到receiver的类型
    node.receiver.accept(this);
    
    Type receiverType = getType(node.receiver);
    
    // 检查receiver是否是struct constructor类型
    if (!(receiverType instanceof StructConstructorType)) {
        throwTypeError(TypeCheckException.Type.NOT_A_STRUCT_CONSTRUCTOR,
                      String.format("Field access on non-struct type: %s", receiverType),
                      node.receiver);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    // 从StructConstructorType中获取StructType
    StructConstructorType constructorType = (StructConstructorType) receiverType;
    StructType structType = constructorType.getStructType();
    
    // 检查字段是否在struct的字段列表中
    String fieldName = node.fieldName.name;
    Map<String, Type> fields = structType.getFields();
    
    if (!fields.containsKey(fieldName)) {
        throwTypeError(TypeCheckException.Type.FIELD_NOT_FOUND,
                      String.format("Field '%s' not found in struct '%s'", fieldName, structType.getName()),
                      node.fieldName);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    // 字段访问的类型是字段的类型
    Type fieldType = fields.get(fieldName);
    setType(node, fieldType);
}
```

#### 2.11 IndexExprNode

```java
@Override
public void visit(IndexExprNode node) throws TypeCheckException {
    // 首先检查数组和索引的类型
    node.array.accept(this);
    node.index.accept(this);
    
    Type arrayType = getType(node.array);
    Type indexType = getType(node.index);
    
    // 检查数组类型
    if (!(arrayType instanceof ArrayType)) {
        throwTypeError(TypeCheckException.Type.NOT_AN_ARRAY,
                      String.format("Attempt to index non-array type %s", arrayType),
                      node.array);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    // 检查索引类型：只能是usize或int
    if (!(indexType instanceof PrimitiveType)) {
        throwTypeError(TypeCheckException.Type.INVALID_INDEX_TYPE,
                      String.format("Array index must be usize or integer, got %s", indexType),
                      node.index);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    PrimitiveType.PrimitiveKind indexKind = ((PrimitiveType)indexType).getKind();
    if (indexKind != PrimitiveType.PrimitiveKind.USIZE &&
        indexKind != PrimitiveType.PrimitiveKind.INT) {
        throwTypeError(TypeCheckException.Type.INVALID_INDEX_TYPE,
                      String.format("Array index must be usize or integer, got %s", indexType),
                      node.index);
    }
    
    // 索引访问的类型是数组的元素类型
    ArrayType array = (ArrayType) arrayType;
    setType(node, array.getElementType());
}
```

#### 2.12 GroupExprNode

```java
@Override
public void visit(GroupExprNode node) throws TypeCheckException {
    // 首先检查内部表达式的类型
    node.expr.accept(this);
    
    // 分组表达式的类型就是内部表达式的类型
    Type innerType = getType(node.expr);
    setType(node, innerType);
}
```

#### 2.13 BorrowExprNode

```java
@Override
public void visit(BorrowExprNode node) throws TypeCheckException {
    // 首先检查内部表达式的类型
    node.innerExpr.accept(this);
    
    Type innerType = getType(node.innerExpr);
    
    // 创建引用类型
    Type refType = new ReferenceType(innerType, node.isMutable);
    
    // 如果是双重引用，则创建嵌套的引用类型
    if (node.isDoubleReference) {
        refType = new ReferenceType(refType, node.isMutable);
    }
    
    setType(node, refType);
}
```

#### 2.14 DerefExprNode

```java
@Override
public void visit(DerefExprNode node) throws TypeCheckException {
    // 首先检查内部表达式的类型
    node.innerExpr.accept(this);
    
    Type innerType = getType(node.innerExpr);
    
    // 检查是否是引用类型
    if (!(innerType instanceof ReferenceType)) {
        throwTypeError(TypeCheckException.Type.NOT_A_REFERENCE,
                      String.format("Attempt to dereference non-reference type %s", innerType),
                      node.innerExpr);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    // 解引用的类型是引用的内部类型
    ReferenceType refType = (ReferenceType) innerType;
    setType(node, refType.getInnerType());
}
```

#### 2.15 TypeCastExprNode

根据Rust Reference中的"Type cast expressions"部分，类型转换表达式使用`as`操作符将左侧表达式的值转换为右侧指定的类型。

在RCompiler中，`TypeCastExprNode`的`node.type`只能是四种确定性整型中的一种，其余的全都不合法。这四种确定性整型分别是：
- `I32` - 32位有符号整数
- `U32` - 32位无符号整数
- `USIZE` - 与指针大小相同的无符号整数
- `ISIZE` - 与指针大小相同的有符号整数

注意：根据AST定义，`node.type`是`TypeExprNode`类型。对于类型转换表达式，它必须是`TypePathExprNode`类型，其`path`字段必须是上述四种整型名称之一。

```java
@Override
public void visit(TypeCastExprNode node) throws TypeCheckException {
    // 首先检查表达式的类型
    node.expr.accept(this);
    
    Type exprType = getType(node.expr);
    
    // 验证node.type是否为TypePathExprNode
    if (!(node.type instanceof TypePathExprNode)) {
        throwTypeError(TypeCheckException.Type.INVALID_CAST_TARGET,
                      "Cast target must be a type path",
                      node.type);
    }
    
    TypePathExprNode typePathExpr = (TypePathExprNode) node.type;
    PathExprSegNode pathSeg = typePathExpr.path;
    
    // 验证pathSeg是否为IdentifierNode
    if (!(pathSeg.name instanceof IdentifierNode)) {
        throwTypeError(TypeCheckException.Type.INVALID_CAST_TARGET,
                      "Cast target must be a simple identifier",
                      node.type);
    }
    
    String typeName = pathSeg.name.name;
    
    // 检查类型名称是否为四种确定性整型之一
    Type targetType;
    switch (typeName) {
        case "i32":
            targetType = new PrimitiveType(PrimitiveType.PrimitiveKind.I32);
            break;
        case "u32":
            targetType = new PrimitiveType(PrimitiveType.PrimitiveKind.U32);
            break;
        case "usize":
            targetType = new PrimitiveType(PrimitiveType.PrimitiveKind.USIZE);
            break;
        case "isize":
            targetType = new PrimitiveType(PrimitiveType.PrimitiveKind.ISIZE);
            break;
        default:
            throwTypeError(TypeCheckException.Type.INVALID_CAST_TARGET,
                          String.format("Invalid cast target type: %s. Only i32, u32, usize, and isize are allowed", typeName),
                          node.type);
            return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    // 根据Rust类型转换规则验证转换是否有效
    if (!isValidCast(exprType, targetType)) {
        throwTypeError(TypeCheckException.Type.INVALID_CAST,
                      String.format("Invalid cast from %s to %s", exprType, targetType),
                      node);
    }
    
    // 类型转换Expression的类型是目标类型
    setType(node, targetType);
}

/**
 * 检查类型转换是否有效，根据Rust Reference中的类型转换规则
 *
 * @param fromType 源类型
 * @param toType 目标类型
 * @return 如果转换有效返回true，否则返回false
 */
private boolean isValidCast(Type fromType, Type toType) {
    // 1. 数值类型之间的转换
    if (isNumericType(fromType) && isNumericType(toType)) {
        return true; // 所有数值类型之间可以相互转换
    }
    
    // 2. bool/char到整型的转换
    if ((isPrimitiveType(fromType, PrimitiveType.PrimitiveKind.BOOL) ||
         isPrimitiveType(fromType, PrimitiveType.PrimitiveKind.CHAR)) &&
        isNumericType(toType)) {
        return true;
    }
    
    // 5. 其他情况暂不支持
    return false;
}

/**
 * 检查类型是否为指定的原始类型
 */
private boolean isPrimitiveType(Type type, PrimitiveType.PrimitiveKind kind) {
    if (type instanceof PrimitiveType) {
        PrimitiveType.PrimitiveType primitiveType = (PrimitiveType) type;
        return primitiveType.getKind() == kind;
    }
    return false;
}
```

#### 2.16 ArrayExprNode

```java
@Override
public void visit(ArrayExprNode node) throws TypeCheckException {
    if (node.elements != null) {
        // 处理元素列表
        List<Type> elementTypes = new ArrayList<>();
        Type commonElementType = null;
        
        for (ExprNode element : node.elements) {
            element.accept(this);
            Type elementType = getType(element);
            elementTypes.add(elementType);
            
            if (commonElementType == null) {
                commonElementType = elementType;
            } else if (!elementType.equals(commonElementType)) {
                // 尝试找到共同的超类型
                commonElementType = findCommonType(commonElementType, elementType);
                if (commonElementType == null) {
                    throwTypeError(TypeCheckException.Type.ARRAY_ELEMENT_TYPE_MISMATCH,
                                  String.format("Array element type mismatch: %s and %s",
                                               commonElementType, elementType),
                                  element);
                }
            }
        }
        
        // 数组类型是元素类型的数组
        if (commonElementType != null) {
            Type arrayType = new ArrayType(commonElementType, elementTypes.size());
            setType(node, arrayType);
        } else {
            throwTypeError(TypeCheckException.Type.EMPTY_ARRAY,
                          "Cannot determine type of empty array",
                          node);
            return; // 不会执行，因为throwOnError默认为true时会抛出异常
        }
    } else if (node.repeatedElement != null && node.size != null) {
        // 处理重复元素数组 [expr; size]
        node.repeatedElement.accept(this);
        node.size.accept(this);
        
        Type elementType = getType(node.repeatedElement);
        Type sizeType = getType(node.size);
        
        // 检查大小是否是usize或int类型
        if (!(sizeType instanceof PrimitiveType)) {
            throwTypeError(TypeCheckException.Type.INVALID_ARRAY_SIZE,
                          String.format("Array size must be usize or integer, got %s", sizeType),
                          node.size);
        }
        
        PrimitiveType.PrimitiveKind sizeKind = ((PrimitiveType)sizeType).getKind();
        if (sizeKind != PrimitiveType.PrimitiveKind.USIZE &&
            sizeKind != PrimitiveType.PrimitiveKind.INT) {
            throwTypeError(TypeCheckException.Type.INVALID_ARRAY_SIZE,
                          String.format("Array size must be usize or integer, got %s", sizeType),
                          node.size);
        }
        
        // 数组类型是元素类型的数组
        if (elementType == null) {
            throwTypeError(TypeCheckException.Type.NULL_ELEMENT_TYPE,
                          "Cannot determine element type for array",
                          node.repeatedElement);
            return; // 不会执行，因为throwOnError默认为true时会抛出异常
        } else {
            // 注意：这里的大小是编译时常量，实际实现中需要计算
            Type arrayType = new ArrayType(elementType, 0); // 大小暂时设为0
            setType(node, arrayType);
        }
    } else {
        // 空数组，抛出错误
        throwTypeError(TypeCheckException.Type.EMPTY_ARRAY,
                      "Cannot determine type of empty array",
                      node);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
}
```

#### 2.17 StructExprNode

```java
@Override
public void visit(StructExprNode node) throws TypeCheckException {
    // 首先处理结构体名称
    node.structName.accept(this);
    
    // 获取结构体名称的类型
    Type structNameType = getType(node.structName);
    
    // 检查结构体名称是否是StructConstructorType
    if (!(structNameType instanceof StructConstructorType)) {
        throwTypeError(TypeCheckException.Type.NOT_A_STRUCT_CONSTRUCTOR,
                      String.format("'%s' is not a struct constructor, got %s",
                                   node.structName.toString(), structNameType),
                      node.structName);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    // 从StructConstructorType中获取StructType
    StructConstructorType constructorType = (StructConstructorType) structNameType;
    StructType structType = constructorType.getStructType();
    String structName = structType.getName();
    
    // 检查字段值
    if (node.fieldValues != null) {
        Map<String, Type> fieldTypes = structType.getFields();
        Set<String> providedFields = new HashSet<>();
        
        for (FieldValNode fieldVal : node.fieldValues) {
            String fieldName = fieldVal.fieldName.name;
            providedFields.add(fieldName);
            
            // 检查字段是否存在
            if (!fieldTypes.containsKey(fieldName)) {
                throwTypeError(TypeCheckException.Type.FIELD_NOT_FOUND,
                              String.format("Struct '%s' has no field '%s'", structName, fieldName),
                              fieldVal.fieldName);
                continue;
            }
            
            // 检查字段值的类型
            fieldVal.value.accept(this);
            Type valueType = getType(fieldVal.value);
            Type expectedType = fieldTypes.get(fieldName);
            
            if (!valueType.equals(expectedType)) {
                throwTypeError(TypeCheckException.Type.FIELD_TYPE_MISMATCH,
                              String.format("Field '%s' type mismatch: expected %s, got %s",
                                           fieldName, expectedType, valueType),
                              fieldVal.value);
            }
        }
        
        // 检查是否有未提供的字段
        for (String fieldName : fieldTypes.keySet()) {
            if (!providedFields.contains(fieldName)) {
                throwTypeError(TypeCheckException.Type.MISSING_FIELD,
                              String.format("Missing field '%s' in struct initialization", fieldName),
                              node);
            }
        }
    }
    
    // 结构体Expression的类型是结构体类型
    setType(node, structType);
}
```

#### 2.18 BlockExprNode

```java
@Override
public void visit(BlockExprNode node) throws TypeCheckException {
    // 处理块中的语句
    if (node.statements != null) {
        for (StmtNode stmt : node.statements) {
            stmt.accept(this);
        }
    }
    
    // 块Expression的类型是returnValue的类型，如果没有returnValue则是单元类型
    Type blockType = UnitType.INSTANCE;
    
    if (node.returnValue != null) {
        // 如果有显式的返回值表达式，使用其类型
        node.returnValue.accept(this);
        blockType = getType(node.returnValue);
    } 
    
    setType(node, blockType);
}
```

#### 2.19 ImplNode

```java
@Override
public void visit(ImplNode node) throws TypeCheckException {
    // 处理类型名称
    node.typeName.accept(this);
    
    // 获取impl目标的类型
    Type implType = getType(node.typeName);
    
    // 保存之前的currentType
    Type previousCurrentType = currentType;
    
    try {
        // 设置当前Self指代的类型
        setCurrentType(implType);
        
        // 处理trait名称（如果有的话）
        if (node.trait != null) {
            node.trait.accept(this);
        }
        
        // 处理关联项
        if (node.items != null) {
            for (AssoItemNode item : node.items) {
                item.accept(this);
            }
        }
    } finally {
        // 恢复之前的currentType
        setCurrentType(previousCurrentType);
    }
    
    // ImplNode本身不是表达式，不需要设置类型
}
```

#### 2.20 LetStmtNode

```java
@Override
public void visit(LetStmtNode node) throws TypeCheckException {
    // 首先检查值的类型（如果有的话）
    Type valueType = null;
    if (node.value != null) {
        node.value.accept(this);
        valueType = getType(node.value);
    }
    
    // 确定变量的类型
    Type varType = null;
    if (node.type != null) {
        // 如果有显式类型，使用显式类型
        varType = extractTypeFromTypeNode(node.type);
        
        // 如果有值，检查值的类型是否与显式类型匹配
        if (valueType != null && findCommonType(varType, valueType) == null) {
            throwTypeError(TypeCheckException.Type.TYPE_MISMATCH,
                          String.format("Type mismatch in let binding: expected %s, got %s",
                                       varType, valueType),
                          node);
        }
    } else if (valueType != null) {
        // 如果没有显式类型但有值，使用值的类型
        varType = valueType;
    } else {
        // 既没有显式类型也没有值，报错
        throwTypeError(TypeCheckException.Type.TYPE_INFERENCE_NOT_SUPPORTED,
                      "Cannot infer type without explicit type annotation or value",
                      node);
    }
    
    // 不再需要将变量类型添加到类型环境中
    if (!(node.pattern instanceof IdPatNode)) {
        // 复杂模式暂不支持
        throwTypeError(TypeCheckException.Type.UNSUPPORTED_PATTERN,
                      "Complex patterns in let statements are not supported",
                      node.pattern);
    }
    
    // LetStmtNode不是表达式，不需要设置类型
}
```

#### 2.21 FunctionNode

```java
@Override
public void visit(FunctionNode node) throws TypeCheckException {
    // 保存之前的currentType
    Type previousCurrentType = currentType;
    
    // 进入函数上下文
    enterFunctionContext(node);
    
    try {
        // 如果是方法（有self参数），设置当前Self指代的类型
        if (node.selfPara != null) {
            // 从self参数的类型推断Self的类型
            if (node.selfPara.type != null) {
                // 如果有显式类型，使用显式类型
                setCurrentType(extractTypeFromTypeNode(node.selfPara.type));
            } else {
                // 如果没有显式类型，使用当前Self指代的类型
                setCurrentType(getCurrentSelfType());
            }
        }
        
        // 处理self参数（如果有的话）
        if (node.selfPara != null) {
            node.selfPara.accept(this);
        }
        
        // 处理参数
        if (node.parameters != null) {
            for (ParameterNode param : node.parameters) {
                param.accept(this);
            }
        }
        
        // 处理返回类型（如果有的话）
        if (node.returnType != null) {
            node.returnType.accept(this);
        }
        
        // 处理函数体
        if (node.body != null) {
            node.body.accept(this);
        }
        
        
        // FunctionNode不是ExprNode，不需要设置类型
    } finally {
        // 退出函数上下文
        exitFunctionContext();
        // 恢复之前的currentType
        setCurrentType(previousCurrentType);
    }
}
```

#### 2.22 IfExprNode

```java
@Override
public void visit(IfExprNode node) throws TypeCheckException {
    // 首先检查条件的类型
    node.condition.accept(this);
    
    Type conditionType = getType(node.condition);
    
    // 检查条件是否是bool类型
    if (!(conditionType instanceof PrimitiveType &&
          ((PrimitiveType)conditionType).getKind() == PrimitiveType.PrimitiveKind.BOOL)) {
        throwTypeError(TypeCheckException.Type.NON_BOOL_CONDITION,
                      String.format("If condition must be bool, got %s", conditionType),
                      node.condition);
    }
    
    // 检查then分支的类型
    node.thenBranch.accept(this);
    Type thenType = getType(node.thenBranch);
    
    // 检查else分支的类型（如果存在）
    Type elseType = null;
    if (node.elseBranch != null) {
        node.elseBranch.accept(this);
        elseType = getType(node.elseBranch);
    } else if (node.elseifBranch != null) {
        node.elseifBranch.accept(this);
        elseType = getType(node.elseifBranch);
    }
    
    // 如果没有else分支，If表达式的类型就是then分支的类型
    if (elseType == null) {
        setType(node, thenType);
        return;
    }
    
    // 如果有else分支，则尝试找到共同类型
    Type resultType = findCommonType(thenType, elseType);
    if (resultType == null) {
        throwTypeError(TypeCheckException.Type.IF_BRANCH_TYPE_MISMATCH,
                      String.format("If branches have incompatible types: %s and %s", thenType, elseType),
                      node);
        return; // 不会执行，因为throwOnError默认为true时会抛出异常
    }
    
    setType(node, resultType);
}
```

#### 2.23 LoopExprNode

```java
@Override
public void visit(LoopExprNode node) throws TypeCheckException {
    // 如果是while循环，检查条件
    if (!node.isInfinite && node.condition != null) {
        node.condition.accept(this);
        
        Type conditionType = getType(node.condition);
        
        // 检查条件是否是bool类型
        if (!(conditionType instanceof PrimitiveType &&
              ((PrimitiveType)conditionType).getKind() == PrimitiveType.PrimitiveKind.BOOL)) {
            throwTypeError(TypeCheckException.Type.NON_BOOL_CONDITION,
                          String.format("Loop condition must be bool, got %s", conditionType),
                          node.condition);
        }
    }
    
    // 根据循环类型进入相应的上下文
    if (node.isInfinite) {
        enterLoopContext(node);  // loop循环
    } else {
        enterWhileContext(node);  // while循环
    }
    
    try {
        // 检查循环体
        node.body.accept(this);
        
        // 获取当前循环上下文
        ControlFlowContext loopContext = contextStack.peek();
        List<Type> breakTypes = loopContext.getBreakTypes();
        
        // 对于while循环，break不能带值
        if (!node.isInfinite && !breakTypes.isEmpty()) {
            for (Type breakType : breakTypes) {
                if (!(breakType instanceof UnitType)) {
                    throwTypeError(TypeCheckException.Type.BREAK_WITH_VALUE_IN_WHILE,
                                  "Break with value is not allowed in while loop",
                                  node);
                }
            }
        }
        
        // while循环的类型总是单元类型
        if (!node.isInfinite) {
            setType(node, UnitType.INSTANCE);
        } else {
            // loop循环的类型处理
            if (breakTypes.isEmpty()) {
                // 没有break，循环类型是!（never类型）
                setType(node, NeverType.INSTANCE);
            } else {
                // 有break，找到所有break类型的共同类型
                Type commonType = breakTypes.get(0);
                for (int i = 1; i < breakTypes.size(); i++) {
                    commonType = findCommonType(commonType, breakTypes.get(i));
                    if (commonType == null) {
                        throwTypeError(TypeCheckException.Type.INCOMPATIBLE_BREAK_TYPES,
                                      "Break expressions in loop have incompatible types",
                                      node);
                    }
                }
                setType(node, commonType);
            }
        }
    } finally {
        // 退出循环上下文
        exitLoopContext();
    }
}
```

#### 2.24 其他表达式

```java
@Override
public void visit(BreakExprNode node) throws TypeCheckException {
    // 查找最近的循环上下文
    ControlFlowContext loopContext = findNearestLoopContext();
    if (loopContext == null) {
        throwTypeError(TypeCheckException.Type.BREAK_OUTSIDE_LOOP,
                      "Break statement outside of loop",
                      node);
        return;
    }
    
    // 设置目标AST节点
    node.setTargetNode(loopContext.getNode());
    
    // 检查break值的类型
    Type breakValueType = UnitType.INSTANCE; // 默认是单元类型
    if (node.value != null) {
        node.value.accept(this);
        breakValueType = getType(node.value);
    }
    
    // 将break类型添加到循环上下文中
    loopContext.getBreakTypes().add(breakValueType);
    
    // break Expression的类型是never类型
    setType(node, NeverType.INSTANCE);
}

@Override
public void visit(ContinueExprNode node) throws TypeCheckException {
    // 查找最近的循环上下文
    ControlFlowContext loopContext = findNearestLoopContext();
    if (loopContext == null) {
        throwTypeError(TypeCheckException.Type.CONTINUE_OUTSIDE_LOOP,
                      "Continue statement outside of loop",
                      node);
        return;
    }
    
    // 设置目标AST节点
    node.setTargetNode(loopContext.getNode());
    
    // continue不能带值
    if (node.value != null) {
        throwTypeError(TypeCheckException.Type.CONTINUE_WITH_VALUE,
                      "Continue statement cannot have a value",
                      node.value);
    }
    
    // continue Expression的类型是never类型
    setType(node, NeverType.INSTANCE);
}

@Override
public void visit(ReturnExprNode node) throws TypeCheckException {
    // 查找最近的函数上下文
    ControlFlowContext functionContext = findNearestFunctionContext();
    if (functionContext == null) {
        throwTypeError(TypeCheckException.Type.RETURN_OUTSIDE_FUNCTION,
                      "Return statement outside of function",
                      node);
        return;
    }
    
    // 设置目标AST节点
    node.setTargetNode(functionContext.getNode());
    
    // 获取函数的返回类型
    FunctionNode functionNode = (FunctionNode) functionContext.getNode();
    Type expectedReturnType = functionNode.returnType != null ?
                              extractTypeFromTypeNode(functionNode.returnType) :
                              UnitType.INSTANCE;
    
    // 检查return值的类型
    if (node.value != null) {
        node.value.accept(this);
        Type actualReturnType = getType(node.value);
        
        // 检查返回类型是否匹配
        if (findCommonType(expectedReturnType, actualReturnType) == null) {
            throwTypeError(TypeCheckException.Type.RETURN_TYPE_MISMATCH,
                          String.format("Return type mismatch: expected %s, got %s",
                                       expectedReturnType, actualReturnType),
                          node.value);
        }
    } else {
        // 没有返回值，检查函数是否期望返回值
        if (!(expectedReturnType instanceof UnitType)) {
            throwTypeError(TypeCheckException.Type.MISSING_RETURN_VALUE,
                          String.format("Missing return value: expected %s", expectedReturnType),
                          node);
        }
    }
    
    // return Expression的类型是never类型
    setType(node, NeverType.INSTANCE);
}

@Override
public void visit(UnderscoreExprNode node) throws TypeCheckException {
    // 下划线Expression用于忽略值，不应该有类型
    throwTypeError(TypeCheckException.Type.UNDERSCORE_NOT_ALLOWED,
                  "Underscore expression is not allowed in this context",
                  node);
}
```

#### 2.25 PathExprNode

```java
@Override
public void visit(PathExprNode node) throws TypeCheckException {
    // 直接从PathExprNode获取已存储的符号信息
    Symbol symbol = node.getSymbol();
    if (symbol != null) {
        // 直接从存储的符号中获取类型
        Type type = extractTypeFromSymbol(symbol);
        setType(node, type);
        return;
    }
    
    // 如果没有存储符号信息，则报错
    throwTypeError(TypeCheckException.Type.UNDEFINED_VARIABLE,
                  "Undefined path expression",
                  node);
    return; // 不会执行，因为throwOnError默认为true时会抛出异常
}
```

#### 2.26 PathExprSegNode

```java
@Override
public void visit(PathExprSegNode node) throws TypeCheckException {
    // 直接从PathExprSegNode获取已存储的符号信息
    Symbol symbol = node.getSymbol();
    if (symbol != null) {
        // 直接从存储的符号中获取类型
        Type type = extractTypeFromSymbol(symbol);
        setType(node, type);
        return;
    }
    
    // 如果没有存储符号信息，则报错
    throwTypeError(TypeCheckException.Type.UNDEFINED_VARIABLE,
                  "Undefined path segment",
                  node);
    return; // 不会执行，因为throwOnError默认为true时会抛出异常
}
```

### 3. 错误处理

```java
// 类型检查异常类
public class TypeCheckException extends Exception {
    public enum Type {
        UNKNOWN_LITERAL_TYPE,
        UNDEFINED_VARIABLE,
        INVALID_ARITHMETIC_OPERATION,
        INVALID_COMPARISON,
        INVALID_LOGICAL_OPERATION,
        INVALID_ASSIGNMENT_TARGET,
        INVALID_COMPOUND_ASSIGNMENT,
        TYPE_MISMATCH,
        NOT_A_FUNCTION,
        WRONG_ARGUMENT_COUNT,
        ARGUMENT_TYPE_MISMATCH,
        METHOD_NOT_FOUND,
        FIELD_NOT_FOUND,
        NOT_AN_ARRAY,
        INVALID_INDEX_TYPE,
        NOT_A_REFERENCE,
        INVALID_CAST,
        INVALID_CAST_TARGET,
        ARRAY_ELEMENT_TYPE_MISMATCH,
        INVALID_ARRAY_SIZE,
        NOT_A_STRUCT,
        NOT_A_STRUCT_CONSTRUCTOR,
        FIELD_TYPE_MISMATCH,
        MISSING_FIELD,
        NON_BOOL_CONDITION,
        IF_BRANCH_TYPE_MISMATCH,
        SELF_OUTSIDE_METHOD,
        SELF_TYPE_OUTSIDE_IMPL,
        UNKNOWN_PATH_SEGMENT,
        INVALID_TYPE_EXTRACTION,
        NULL_SYMBOL,
        TYPE_INFERENCE_NOT_SUPPORTED,
        UNSUPPORTED_SYMBOL_KIND,
        NULL_TYPE_NODE,
        UNSUPPORTED_TYPE_NODE,
        MULTI_SEGMENT_PATH_NOT_SUPPORTED,
        ASSOCIATED_ITEM_NOT_FOUND,
        NULL_OPERAND_TYPE,
        EMPTY_ARRAY,
        NULL_ELEMENT_TYPE,
        UNDERSCORE_NOT_ALLOWED,
        NULL_IDENTIFIER
    }
    
    private final Type errorType;
    private final ASTNode node;
    
    public TypeCheckException(Type errorType, String message, ASTNode node) {
        super(message);
        this.errorType = errorType;
        this.node = node;
    }
    
    public TypeCheckException(Type errorType, String message, ASTNode node, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.node = node;
    }
    
    // Getter方法
    public Type getErrorType() { return errorType; }
    public ASTNode getNode() { return node; }
    public int getLine() { return node.getLine(); }
    public int getColumn() { return node.getColumn(); }
    
    @Override
    public String toString() {
        return String.format("Type Error [%s] at line %d, column %d: %s",
                           errorType, getLine(), getColumn(), getMessage());
    }
}

// 类型错误收集器（用于收集多个错误）
class TypeErrorCollector {
    private final List<TypeCheckException> errors = new ArrayList<>();
    
    public void addError(TypeCheckException error) {
        errors.add(error);
    }
    
    public void addError(TypeCheckException.Type errorType, String message, ASTNode node) {
        errors.add(new TypeCheckException(errorType, message, node));
    }
    
    public List<TypeCheckException> getErrors() {
        return new ArrayList<>(errors);
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public void throwFirstError() throws TypeCheckException {
        if (!errors.isEmpty()) {
            throw errors.get(0);
        }
    }
    
    public void throwAllErrors() throws TypeCheckException {
        if (!errors.isEmpty()) {
            // 可以抛出第一个错误，或者创建一个包含所有错误的复合异常
            throw errors.get(0);
        }
    }
}
```

## 实现计划

### 第一阶段：基础类型系统实现
1. 实现基础类型接口和具体类型类
2. 实现类型环境和类型缓存
3. 实现类型检查器基础结构

### 第二阶段：简单Expression类型检查
1. 实现字面量Expression的类型检查
2. 实现标识符Expression的类型检查
3. 实现简单二元运算Expression的类型检查

### 第三阶段：复杂Expression类型检查
1. 实现函数调用和方法调用的类型检查
2. 实现字段访问和索引访问的类型检查
3. 实现引用和解引用的类型检查

### 第四阶段：控制流Expression类型检查
1. 实现块Expression的类型检查
2. 实现if Expression的类型检查
3. 实现循环Expression的类型检查

### 第五阶段：高级类型特性
1. 实现类型转换的类型检查
2. 实现数组Expression的类型检查
3. 实现结构体Expression的类型检查

### 第六阶段：集成与测试
1. 将类型检查器集成到主编译流程
2. 使用现有测试用例验证实现
3. 添加特定类型检查的测试用例

## 与现有代码的集成

### namespace_check阶段的准备工作

在namespace_check阶段，需要为每个ExprNode设置context和symbol信息：

```java
// 在SemanticAnalyzer的visit方法中
@Override
public void visit(IdentifierNode node) {
    String identifierName = node.name;
    
    // 根据当前上下文查找符号
    Symbol symbol = null;
    switch (currentContext) {
        case TYPE_CONTEXT:
        case VALUE_CONTEXT:
            // 直接从标识符节点获取已存储的符号信息
            symbol = node.getSymbol();
            break;
        case FIELD_CONTEXT:
            // 字段上下文需要特殊处理
            break;
    }
    
    // 设置IdentifierNode的符号信息
    if (symbol != null) {
        node.setSymbol(symbol);
    }
    
    // 设置所有ExprNode的context
    setExprNodeInfo(node);
}

// 辅助方法：为所有ExprNode设置context
private void setExprNodeInfo(ExprNode expr) {
    if (expr != null) {
        expr.setContext(currentContext);
        
        // 递归设置子节点的信息
        for (ASTNode child : expr.getChildren()) {
            if (child instanceof ExprNode) {
                setExprNodeInfo((ExprNode) child);
            }
        }
    }
}
```

### 类型检查阶段的集成

类型检查器将集成到`Main.java`的主编译流程中：

```java
// 解析后
Parser parser = new Parser(new Vector<token_t>(tokenizer.tokens));
parser.parse();

// 执行命名空间语义分析
SemanticAnalyzer namespaceAnalyzer = new SemanticAnalyzer();
for (StmtNode stmt : parser.getStatements()) {
    namespaceAnalyzer.visit(stmt);
}

// 执行类型检查
TypeChecker typeChecker = new TypeChecker(false);
try {
    for (StmtNode stmt : parser.getStatements()) {
        typeChecker.visit(stmt);
    }
    
    // 报告类型错误（如果有）
    if (typeChecker.hasErrors()) {
        for (TypeCheckException error : typeChecker.getErrorCollector().getErrors()) {
            System.err.println(error);
        }
        // 以错误代码退出
    }
} catch (TypeCheckException e) {
    // 处理类型检查异常
    System.err.println("Type check failed: " + e);
    // 以错误代码退出
}
```

## 测试策略

使用现有测试用例并添加新的测试用例：
1. 验证基本类型的正确识别
2. 验证算术运算的类型推断
3. 验证函数调用的类型检查
4. 验证类型转换的正确性
5. 验证复杂Expression的类型推断

## 示例场景

```rust
let x = 5;        // x: i32
let y = 10.0;      // y: f64 (假设支持)
let z = x + y;     // 类型错误：i32和f64不能直接相加
let a = x + 3;     // a: i32
let b = if x > 0 { 1 } else { 0 };  // b: i32
let c = if x > 0 { 1 } else { "0" }; // 类型错误：分支类型不兼容
```

## 未来扩展

实现基本类型检查后，可以扩展以支持：
1. 泛型类型参数
2. Trait约束和实现检查
3. 生命周期检查
4. 模式匹配的类型检查
5. 更复杂的类型推断算法

## 结论

本设计为RCompiler提供了全面的表达式类型检查实现。通过为每个表达式节点确定类型，并验证类型操作的合法性，可以确保编译器正确处理Rust的类型系统，为后续的代码生成阶段提供准确的类型信息。
