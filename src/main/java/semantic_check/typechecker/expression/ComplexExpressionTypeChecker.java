/**
 * 复杂表达式类型检查器
 * 处理函数调用、方法调用、字段访问、索引访问、借用、解引用、类型转换等
 */
public class ComplexExpressionTypeChecker extends VisitorBase {
    protected final TypeErrorCollector errorCollector;
    protected final boolean throwOnError;
    protected final TypeExtractor typeExtractor;
    protected final ConstantEvaluator constantEvaluator;
    protected final ExpressionTypeContext context;
    protected final TypeChecker mainExpressionChecker;
    protected MutabilityChecker mutabilityChecker;
    
    public ComplexExpressionTypeChecker(
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
            "Cannot visit abstract ExprNode directly in ComplexExpressionTypeChecker"
        );
    }
    
    /**
     * 访问不带块的表达式基类
     */
    public void visit(ExprWithoutBlockNode node) {
        // 这是一个抽象基类，在正确的访问者模式实现中不应该到达这里
        throw new RuntimeException(
            "Cannot visit abstract ExprWithoutBlockNode directly in ComplexExpressionTypeChecker"
        );
    }
    
    /**
     * 访问函数调用表达式
     */
    public void visit(CallExprNode node) {
        try {
            if (node.function == null) {
                reportError("Function call missing function expression");
                return;
            }
            
            // 访问函数表达式，使用TypeChecker进行调用
            node.function.accept(mainExpressionChecker);
            Type functionType = node.function.getType();
            
            // 检查函数类型是否为函数类型
            if (!(functionType instanceof FunctionType)) {
                reportError("Called expression is not a function: " + functionType);
                return;
            }
            
            FunctionType funcType = (FunctionType) functionType;
            
            // 检查参数数量
            int expectedArgs = funcType.getParameterTypes().size();
            int actualArgs = node.arguments != null ? node.arguments.size() : 0;
            
            if (expectedArgs != actualArgs) {
                reportError("Function expects " + expectedArgs + " arguments, but got " + actualArgs);
                return;
            }
            
            // 检查参数类型
            if (node.arguments != null) {
                for (int i = 0; i < node.arguments.size(); i++) {
                    ExprNode arg = node.arguments.get(i);
                    arg.accept(mainExpressionChecker);
                    Type argType = arg.getType();
                    Type expectedType = funcType.getParameterTypes().get(i);
                    
                    if (!TypeUtils.isTypeCompatible(argType, expectedType)) {
                        throw new RuntimeException(
                            "Argument " + (i + 1) + " type mismatch: expected " + expectedType + ", got " + argType + " at " + getCurrentContext()
                        );
                    }
                }
            }
            
            // 设置结果类型为函数的返回类型
            setType(node, funcType.getReturnType());
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问方法调用表达式
     */
    public void visit(MethodCallExprNode node) {
        try {
            if (node.receiver == null || node.methodName == null) {
                reportError("Method call missing receiver or method name");
                return;
            }
            
            // 访问接收者表达式，使用TypeChecker进行调用
            node.receiver.accept(mainExpressionChecker);
            Type receiverType = node.receiver.getType();
            
            // 自动解引用接收者（如果是引用类型）
            Type dereferencedType = receiverType;
            while (dereferencedType instanceof ReferenceType) {
                dereferencedType = ((ReferenceType) dereferencedType).getInnerType();
            }
            
            // 基于解引用的接收者类型查找方法符号
            String methodName = node.methodName.name != null ? node.methodName.name.name : null;
            if (methodName == null) {
                reportError("Method name is null");
                return;
            }
            
            Symbol methodSymbol = null;
            
            // 检查解引用的接收者类型是否为结构体类型
            if (dereferencedType instanceof StructType) {
                StructType structType = (StructType) dereferencedType;
                // 对于结构体方法，我们需要在结构的实现中查找方法
                Symbol structSymbol = structType.getStructSymbol();
                if (structSymbol != null) {
                    // 遍历impl符号以查找匹配名称的函数
                    for (Symbol implSymbol : structSymbol.getImplSymbols()) {
                        if (implSymbol.getKind() == SymbolKind.FUNCTION &&
                            implSymbol.getName().equals(methodName)) {
                            // 找到方法，将其用作方法符号
                            methodSymbol = implSymbol;
                            break;
                        }
                    }
                }
            }
            // 检查解引用的接收者类型是否为String或str类型并查找内置方法
            else if (TypeUtils.isStringOrStrType(dereferencedType)) {
                // 查找内置方法
                methodSymbol = BuiltinMethodResolver.lookupBuiltinMethod(methodName, dereferencedType);
            }
            // 检查解引用的接收者类型是否为数组类型并查找内置方法
            else if (TypeUtils.isArrayType(dereferencedType)) {
                // 查找内置方法
                methodSymbol = BuiltinMethodResolver.lookupBuiltinMethod(methodName, dereferencedType);
            }
            // 检查解引用的接收者类型是否为u32或usize并查找内置方法
            else if (TypeUtils.isU32OrUsizeType(dereferencedType)) {
                // 查找内置方法
                methodSymbol = BuiltinMethodResolver.lookupBuiltinMethod(methodName, dereferencedType);
            }
            
            if (methodSymbol == null) {
                reportError("Method '" + methodName + "' not found for type " + dereferencedType);
                return;
            }
            
            // 在methodName节点上设置方法符号以供将来参考
            node.methodName.setSymbol(methodSymbol);
            
            // 提取方法类型
            Type methodType = typeExtractor.extractTypeFromSymbol(methodSymbol);
            
            // 检查方法类型是否为函数类型
            if (!(methodType instanceof FunctionType)) {
                reportError("Method is not a function: " + methodType);
                return;
            }
            
            FunctionType funcType = (FunctionType) methodType;
            
            // 检查它是否是方法（应该有self参数）
            if (!funcType.isMethod()) {
                reportError("Called function is not a method: " + methodSymbol.getName());
                return;
            }
            
            // 检查参数数量（不包括self参数）
            int expectedArgs = funcType.getParameterTypes().size();
            int actualArgs = node.arguments != null ? node.arguments.size() : 0;
            
            if (expectedArgs != actualArgs) {
                reportError("Method expects " + expectedArgs + " arguments, but got " + actualArgs);
                return;
            }
            
            // 检查参数类型（不包括self参数）
            if (node.arguments != null) {
                for (int i = 0; i < node.arguments.size(); i++) {
                    ExprNode arg = node.arguments.get(i);
                    arg.accept(mainExpressionChecker);
                    Type argType = arg.getType();
                    Type expectedType = funcType.getParameterTypes().get(i);
                    
                    if (!TypeUtils.isTypeCompatible(argType, expectedType)) {
                        throw new RuntimeException(
                            "Argument " + (i + 1) + " type mismatch: expected " + expectedType + ", got " + argType + " at " + getCurrentContext()
                        );
                    }
                }
            }
            
            // 设置结果类型为方法的返回类型
            setType(node, funcType.getReturnType());
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问字段访问表达式
     */
    public void visit(FieldExprNode node) {
        try {
            if (node.receiver == null || node.fieldName == null) {
                reportError("Field access missing receiver or field name");
                return;
            }
            
            // 访问接收者表达式，使用TypeChecker进行调用
            node.receiver.accept(mainExpressionChecker);
            Type receiverType = node.receiver.getType();
            
            // 自动解引用接收者（如果是引用类型）
            Type dereferencedType = receiverType;
            while (dereferencedType instanceof ReferenceType) {
                dereferencedType = ((ReferenceType) dereferencedType).getInnerType();
            }
            
            // 检查解引用的接收者类型是否为结构体类型
            if (!(dereferencedType instanceof StructType)) {
                reportError("Cannot access field on non-struct type: " + receiverType);
                return;
            }
            
            StructType structType = (StructType) dereferencedType;
            String fieldName = node.fieldName.name;
            
            // 检查字段是否存在
            Type fieldType = structType.getFieldType(fieldName);
            if (fieldType == null) {
                reportError("Field '" + fieldName + "' not found in struct " + structType.getName());
                return;
            }
            
            // 设置结果类型为字段类型
            setType(node, fieldType);
            
            // 确保字段名的符号已设置（如果存在）
            // 注意：fieldName是IdentifierNode，不是PathExprNode，所以可能没有符号
            // 这只是以防实现更改的预防措施
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问索引表达式
     */
    public void visit(IndexExprNode node) {
        try {
            if (node.array == null || node.index == null) {
                reportError("Index expression missing array or index");
                return;
            }
            
            // 访问数组表达式，使用TypeChecker进行调用
            node.array.accept(mainExpressionChecker);
            Type arrayType = node.array.getType();
            
            // 访问索引表达式，使用TypeChecker进行调用
            node.index.accept(mainExpressionChecker);
            Type indexType = node.index.getType();
            
            // 自动解引用数组类型（如果是引用类型）
            Type dereferencedArrayType = arrayType;
            while (dereferencedArrayType instanceof ReferenceType) {
                dereferencedArrayType = ((ReferenceType) dereferencedArrayType).getInnerType();
            }
            
            // 检查解引用的数组类型是否为数组类型
            if (!(dereferencedArrayType instanceof ArrayType)) {
                reportError("Cannot index non-array type: " + arrayType);
                return;
            }
            
            // 检查索引类型是否为数字
            if (!TypeUtils.isNumericType(indexType)) {
                reportError("Array index must be numeric: " + indexType);
                return;
            }
            
            // 设置结果类型为数组元素类型
            ArrayType arrType = (ArrayType) dereferencedArrayType;
            setType(node, arrType.getElementType());
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问借用表达式（&, &&）
     */
    public void visit(BorrowExprNode node) {
        try {
            if (node.innerExpr == null) {
                reportError("Borrow expression missing inner expression");
                return;
            }
            
            // 访问内部表达式，使用TypeChecker进行调用
            node.innerExpr.accept(mainExpressionChecker);
            Type innerType = node.innerExpr.getType();
            
            // 使用嵌套引用而不是isDoubleReference标志创建引用类型
            Type refType = new ReferenceType(innerType, node.isMutable);
            
            // 如果是双重引用（&&），用另一个引用包装它
            if (node.isDoubleReference) {
                refType = new ReferenceType(refType, node.isMutable);
            }
            
            setType(node, refType);
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问解引用表达式（*）
     */
    public void visit(DerefExprNode node) {
        try {
            if (node.innerExpr == null) {
                reportError("Dereference expression missing inner expression");
                return;
            }
            
            // 访问内部表达式，使用TypeChecker进行调用
            node.innerExpr.accept(mainExpressionChecker);
            Type innerType = node.innerExpr.getType();
            
            // 检查内部类型是否为引用类型
            if (!(innerType instanceof ReferenceType)) {
                reportError("Cannot dereference non-reference type: " + innerType);
                return;
            }
            
            // 设置结果类型为引用的内部类型
            ReferenceType refType = (ReferenceType) innerType;
            setType(node, refType.getInnerType());
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问类型转换表达式（as）
     */
    public void visit(TypeCastExprNode node) {
        try {
            if (node.expr == null || node.type == null) {
                reportError("Type cast expression missing expression or target type");
                return;
            }
            
            // 访问表达式，使用TypeChecker进行调用
            node.expr.accept(mainExpressionChecker);
            Type exprType = node.expr.getType();
            
            // 提取目标类型
            Type targetType = typeExtractor.extractTypeFromTypeNode(node.type);
            
            // 检查转换是否有效
            if (!TypeUtils.isValidCast(exprType, targetType)) {
                reportError("Invalid cast from " + exprType + " to " + targetType);
                return;
            }
            
            setType(node, targetType);
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问数组表达式
     */
    public void visit(ArrayExprNode node) {
        try {
            if (node.elements != null && !node.elements.isEmpty()) {
                // 带有显式元素的数组
                Type elementType = null;
                
                // 访问所有元素并确定公共类型
                for (ExprNode element : node.elements) {
                    element.accept(mainExpressionChecker);
                    Type elemType = element.getType();
                    
                    if (elementType == null) {
                        elementType = elemType;
                    } else {
                        // 查找元素之间的公共类型
                        elementType = TypeUtils.findCommonType(elementType, elemType);
                        if (elementType == null) {
                            reportError("Array elements have incompatible types");
                            return;
                        }
                    }
                }
                
                // 使用确定的元素类型和大小创建数组类型
                ArrayType arrayType = new ArrayType(elementType, node.elements.size());
                setType(node, arrayType);
                
            } else if (node.repeatedElement != null && node.size != null) {
                // 带有重复元素的数组 [expr; size]
                node.repeatedElement.accept(mainExpressionChecker);
                Type elementType = node.repeatedElement.getType();
                
                // 首先，类型检查大小表达式
                node.size.accept(mainExpressionChecker);
                Type sizeType = node.size.getType();
                
                // 检查大小表达式是否为数字类型
                if (!TypeUtils.isNumericType(sizeType)) {
                    reportError("Array size must be a numeric type, got: " + sizeType);
                    return;
                }
                
                // 现在检查大小是否为常量表达式
                ConstantValue sizeValue = constantEvaluator.evaluate(node.size);
                
                if (sizeValue == null) {
                    reportError("Array size expression is not a constant");
                    return;
                }
                
                if (!sizeValue.isNumeric()) {
                    reportError("Array size must be numeric: " + sizeValue.getType());
                    return;
                }
                
                long arraySize = sizeValue.getAsLong();
                
                // 检查数组大小是否为负数
                if (arraySize < 0) {
                    reportError("Array size cannot be negative: " + arraySize);
                    return;
                }
                
                // 使用重复元素类型和评估的大小创建数组类型
                ArrayType arrayType = new ArrayType(elementType, arraySize);
                setType(node, arrayType);
                
            } else {
                // 空数组 []
                // 对于空数组，我们无法确定元素类型
                // 在完整实现中，这将需要类型推断或显式类型注释
                reportError("Empty array requires type annotation");
            }
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问结构体表达式
     */
    public void visit(StructExprNode node) {
        try {
            if (node.structName == null) {
                reportError("Struct expression missing struct name");
                return;
            }
            
            // 获取结构体符号
            Symbol structSymbol = node.structName.getSymbol();
            if (structSymbol == null) {
                reportError("Unresolved struct: " + (node.structName.name != null ? node.structName.name.name : "unknown"));
                return;
            }
            
            // 提取结构体类型
            Type structType = typeExtractor.extractTypeFromSymbol(structSymbol);
            
            // 检查它是否是结构体类型
            if (!(structType instanceof StructConstructorType)) {
                reportError("Expression is not a struct constructor: " + structType);
                return;
            }
            
            StructConstructorType structConstructorType = (StructConstructorType) structType;
            StructType structTypeInfo = structConstructorType.getStructType();
            
            // 检查字段值（如果提供）
            if (node.fieldValues != null) {
                // 检查重复的字段名
                java.util.Set<String> fieldNames = new java.util.HashSet<>();
                for (FieldValNode fieldVal : node.fieldValues) {
                    if (fieldVal.fieldName == null || fieldVal.value == null) {
                        reportError("Struct field value missing field name or value");
                        return;
                    }
                    
                    String fieldName = fieldVal.fieldName.name;
                    
                    // 检查重复的字段名
                    if (fieldNames.contains(fieldName)) {
                        reportError("Duplicate field '" + fieldName + "' in struct initialization");
                        return;
                    }
                    fieldNames.add(fieldName);
                    
                    Type fieldType = structTypeInfo.getFieldType(fieldName);
                    
                    if (fieldType == null) {
                        reportError("Field '" + fieldName + "' not found in struct " + structTypeInfo.getName());
                        return;
                    }

                    fieldVal.value.accept(mainExpressionChecker);
                    
                    Type valueType = fieldVal.value.getType();

                    if (valueType == null) {
                        throw new RuntimeException(
                            "Unable to determine type of value for field '" + fieldName + "' in struct " + structTypeInfo.getName()
                        );
                    }
                    
                    // 检查字段值类型是否与期望的字段类型匹配
                    if (!TypeUtils.isTypeCompatible(valueType, fieldType)) {
                        throw new RuntimeException(
                            "Type mismatch for field '" + fieldName + "' in struct " + structTypeInfo.getName() +
                            ": expected " + fieldType + ", got " + valueType + " at " + getCurrentContext()
                        );
                    }
                }
                
                // 检查提供的字段值数量是否与结构体中的字段数量匹配
                int structFieldCount = structTypeInfo.getFields().size();
                int providedFieldCount = node.fieldValues.size();
                
                if (structFieldCount != providedFieldCount) {
                    reportError("Struct " + structTypeInfo.getName() + " expects " + structFieldCount +
                               " fields, but " + providedFieldCount + " were provided");
                    return;
                }
            }
            
            // 设置结果类型为结构体类型
            setType(node, structTypeInfo);
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问字段值节点（在结构体表达式中使用）
     */
    public void visit(FieldValNode node) {
        try {
            if (node.value != null) {
                node.value.accept(mainExpressionChecker);
            }
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
}