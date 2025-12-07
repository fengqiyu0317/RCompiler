/**
 * 复杂表达式类型检查器
 * 处理函数调用、方法调用、字段访问、索引访问、借用、解引用、类型转换等
 */
public class ComplexExpressionTypeChecker extends BaseExpressionTypeChecker {
    
    public ComplexExpressionTypeChecker(
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
            
            // 访问函数表达式，使用递归调用确保使用正确的类型检查器
            visitExpression(node.function);
            Type functionType = getTypeWithoutNullCheck(node.function);
            
            // 确保函数的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.function);
            
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
                    visitExpression(arg);
                    Type argType = getTypeWithoutNullCheck(arg);
                    Type expectedType = funcType.getParameterTypes().get(i);
                    
                    if (argType == null || expectedType == null || !TypeUtils.isTypeCompatible(argType, expectedType)) {
                        // 尝试查找公共类型
                        Type commonType = TypeUtils.findCommonType(argType, expectedType);
                        if (commonType == null || !TypeUtils.isTypeCompatible(commonType, expectedType)) {
                            reportError("Argument " + (i + 1) + " type mismatch: expected " + expectedType + ", got " + argType + " at " + getCurrentContext());
                            return;
                        }
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
            
            // 访问接收者表达式，使用递归调用确保使用正确的类型检查器
            visitExpression(node.receiver);
            Type receiverType = getTypeWithoutNullCheck(node.receiver);
            
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
            // 检查解引用的接收者类型是否为trait类型
            else if (dereferencedType instanceof TraitType) {
                TraitType traitType = (TraitType) dereferencedType;
                if (traitType.hasMethod(methodName)) {
                    FunctionType methodType = traitType.getMethodType(methodName);
                    methodSymbol = new Symbol(methodName, SymbolKind.FUNCTION, null, 0, false);
                    methodSymbol.setType(methodType);
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
            
            // 确保符号的类型已设置
            if (methodSymbol.getType() == null) {
                typeExtractor.extractTypeFromSymbol(methodSymbol);
            }
            
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
                    visitExpression(arg);
                    Type argType = getTypeWithoutNullCheck(arg);
                    Type expectedType = funcType.getParameterTypes().get(i);
                    
                    if (argType == null || expectedType == null || !TypeUtils.isTypeCompatible(argType, expectedType)) {
                        // 尝试查找公共类型
                        Type commonType = TypeUtils.findCommonType(argType, expectedType);
                        if (commonType == null || !TypeUtils.isTypeCompatible(commonType, expectedType)) {
                            reportError("Argument " + (i + 1) + " type mismatch: expected " + expectedType + ", got " + argType + " at " + getCurrentContext());
                            return;
                        }
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
            
            // 访问接收者表达式，使用递归调用确保使用正确的类型检查器
            visitExpression(node.receiver);
            Type receiverType = getTypeWithoutNullCheck(node.receiver);
            
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
            
            // 访问数组表达式，使用递归调用确保使用正确的类型检查器
            visitExpression(node.array);
            Type arrayType = getTypeWithoutNullCheck(node.array);
            
            // 确保数组的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.array);
            
            // 访问索引表达式，使用递归调用确保使用正确的类型检查器
            visitExpression(node.index);
            Type indexType = getTypeWithoutNullCheck(node.index);
            
            // 确保索引的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.index);
            
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
            
            // 访问内部表达式，使用递归调用确保使用正确的类型检查器
            visitExpression(node.innerExpr);
            Type innerType = getTypeWithoutNullCheck(node.innerExpr);
            
            // 确保内部表达式的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.innerExpr);
            
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
            
            // 访问内部表达式，使用递归调用确保使用正确的类型检查器
            visitExpression(node.innerExpr);
            Type innerType = getTypeWithoutNullCheck(node.innerExpr);
            
            // 确保内部表达式的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.innerExpr);
            
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
            
            // 访问表达式，使用递归调用确保使用正确的类型检查器
            visitExpression(node.expr);
            Type exprType = getTypeWithoutNullCheck(node.expr);
            
            // 确保表达式的符号已设置（如果是PathExprNode）
            ensureSymbolType(node.expr);
            
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
                    visitExpression(element);
                    Type elemType = getTypeWithoutNullCheck(element);
                    
                    // 确保元素的符号已设置（如果是PathExprNode）
                    ensureSymbolType(element);
                    
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
                visitExpression(node.repeatedElement);
                Type elementType = getTypeWithoutNullCheck(node.repeatedElement);
                
                // 确保重复元素的符号已设置（如果是PathExprNode）
                ensureSymbolType(node.repeatedElement);
                
                // 首先，类型检查大小表达式
                visitExpression(node.size);
                Type sizeType = getTypeWithoutNullCheck(node.size);
                
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
            
            // 确保符号设置回structName节点
            node.structName.setSymbol(structSymbol);
            
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

                    visitExpression(fieldVal);
                    
                    Type valueType = getTypeWithoutNullCheck(fieldVal.value);

                    if (valueType == null) {
                        throw new RuntimeException(
                            "Unable to determine type of value for field '" + fieldName + "' in struct " + structTypeInfo.getName()
                        );
                    }
                    
                    // 检查字段值类型是否与期望的字段类型匹配
                    if (valueType == null || fieldType == null || !TypeUtils.isTypeCompatible(valueType, fieldType)) {
                        // 尝试查找公共类型
                        Type commonType = TypeUtils.findCommonType(valueType, fieldType);
                        if (commonType == null || !TypeUtils.isTypeCompatible(commonType, fieldType)) {
                            reportError("Field '" + fieldName + "' type mismatch: expected " + fieldType + ", got " + valueType + " at " + getCurrentContext());
                            return;
                        }
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
                visitExpression(node.value);
            }
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
}