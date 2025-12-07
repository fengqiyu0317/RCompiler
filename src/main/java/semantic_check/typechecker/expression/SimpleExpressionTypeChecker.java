/**
 * 简单表达式类型检查器
 * 处理字面量、路径表达式、分组表达式、下划线表达式等
 */
public class SimpleExpressionTypeChecker extends BaseExpressionTypeChecker {
    
    public SimpleExpressionTypeChecker(
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
            "Cannot visit abstract ExprNode directly in SimpleExpressionTypeChecker"
        );
    }
    
    /**
     * 访问不带块的表达式基类
     */
    public void visit(ExprWithoutBlockNode node) {
        // 这是一个抽象基类，在正确的访问者模式实现中不应该到达这里
        throw new RuntimeException(
            "Cannot visit abstract ExprWithoutBlockNode directly in SimpleExpressionTypeChecker"
        );
    }
    
    /**
     * 访问字面量表达式
     */
    public void visit(LiteralExprNode node) {
        Type literalType;
        
        switch (node.literalType) {
            case I32:
                literalType = PrimitiveType.getI32Type();
                break;
            case U32:
                literalType = PrimitiveType.getU32Type();
                break;
            case USIZE:
                literalType = PrimitiveType.getUsizeType();
                break;
            case ISIZE:
                literalType = PrimitiveType.getIsizeType();
                break;
            case INT:
                literalType = PrimitiveType.getIntType();
                break;
            case BOOL:
                literalType = PrimitiveType.getBoolType();
                break;
            case CHAR:
                literalType = PrimitiveType.getCharType();
                break;
            case STRING:
                literalType = PrimitiveType.getStrType();
                break;
            case CSTRING:
                literalType = PrimitiveType.getStrType(); // 暂时视为字符串
                break;
            default:
                // 默认为未确定的整数类型
                literalType = PrimitiveType.getIntType();
                break;
        }
        
        setType(node, literalType);
    }
    
    /**
     * 访问路径表达式（标识符）
     */
    public void visit(PathExprNode node) {
        try {
            // 从路径表达式获取符号
            Symbol symbol = node.getSymbol();
            if (symbol == null) {
                RuntimeException error = new RuntimeException(
                    "Unresolved symbol: " + (node.LSeg != null && node.LSeg.name != null ? node.LSeg.name.name : "unknown")
                );
                handleError(error);
                return;
            }
            
            // 从符号提取类型
            Type type = typeExtractor.extractTypeFromSymbol(symbol);
            setType(node, type);
            
            // 确保符号设置回节点
            node.setSymbol(symbol);
        
            // 处理RSeg（如果存在）
            if (node.RSeg != null) {
                // 基于LSeg的符号获取RSeg的符号
                Symbol rSegSymbol = node.RSeg != null ? node.RSeg.getSymbol() : null;
                if (rSegSymbol == null) {
                    // 如果RSeg没有符号，我们需要基于LSeg的符号来解析它
                    // 这通常在访问字段、方法或关联项时发生
                    if (node.getSymbol() != null) {
                        Symbol lSegSymbol = node.getSymbol();
                        // 确保LSeg符号设置回LSeg
                        if (node.LSeg != null) {
                            node.LSeg.setSymbol(lSegSymbol);
                        }
                        Type lSegType = typeExtractor.extractTypeFromSymbol(lSegSymbol);
                        
                        // 如果LSeg是结构体构造函数类型，RSeg可能是字段或实现的函数
                        if (lSegType instanceof StructConstructorType) {
                            StructConstructorType structConstructorType = (StructConstructorType) lSegType;
                            StructType structType = structConstructorType.getStructType();
                            handleStructRSeg(node, structType);
                        }
                        // 如果LSeg是枚举构造函数类型，RSeg可能是变体或impl项
                        else if (lSegType instanceof EnumConstructorType) {
                            EnumConstructorType enumConstructorType = (EnumConstructorType) lSegType;
                            EnumType enumType = enumConstructorType.getEnumType();
                            handleEnumRSeg(node, enumType);
                        }
                        // 如果LSeg是trait类型，RSeg可能是关联项
                        else if (lSegType instanceof TraitType) {
                            TraitType traitType = (TraitType) lSegType;
                            handleTraitRSeg(node, traitType);
                        }
                    }
                }
            
                // 如果RSeg有符号，提取其类型
                Symbol rSegSymbolForType = node.RSeg != null ? node.RSeg.getSymbol() : null;
                if (rSegSymbolForType != null) {
                    Type rSegType = typeExtractor.extractTypeFromSymbol(rSegSymbolForType);
                    // 整个路径表达式的类型是RSeg的类型
                    setType(node, rSegType);
                    // 确保整个PathExprNode的符号设置为RSeg的符号
                    node.setSymbol(rSegSymbolForType);
                }
            }
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 处理结构体的RSeg
     */
    private void handleStructRSeg(PathExprNode node, StructType structType) {
        String rSegName = node.RSeg != null && node.RSeg.name != null ? node.RSeg.name.name : null;
        
        if (rSegName == null) {
            RuntimeException error = new RuntimeException(
                "Unresolved field or method name in path expression"
            );
            handleError(error);
            return;
        }
        
        // 如果不是字段，在impl的关联项中查找
        Symbol structSymbol = structType.getStructSymbol();
        if (structSymbol != null) {
            // 遍历impl符号以查找匹配名称的函数或常量
            for (Symbol implSymbol : structSymbol.getImplSymbols()) {
                if ((implSymbol.getKind() == SymbolKind.FUNCTION ||
                        implSymbol.getKind() == SymbolKind.CONSTANT) &&
                    implSymbol.getName().equals(rSegName)) {
                    // 找到关联项，将其用作RSeg符号
                    node.RSeg.setSymbol(implSymbol);
                    break;
                }
            }
            
            // 如果我们在impl符号中没有找到它，报告错误
            if (node.RSeg.getSymbol() == null) {
                RuntimeException error = new RuntimeException(
                    "Field or associated item '" + rSegName + "' not found in struct " + structType.getName()
                );
                handleError(error);
            }
        }
    }
    
    /**
     * 处理枚举的RSeg
     */
    private void handleEnumRSeg(PathExprNode node, EnumType enumType) {
        String rSegName = node.RSeg != null && node.RSeg.name != null ? node.RSeg.name.name : null;
        
        if (rSegName == null) {
            RuntimeException error = new RuntimeException(
                "Unresolved variant name in path expression"
            );
            handleError(error);
            return;
        }
        
        // 首先检查它是否是变体
        if (enumType.hasVariant(rSegName)) {
            // 为变体创建符号
            Symbol variantSymbol = new Symbol(rSegName, SymbolKind.ENUM_VARIANT_CONSTRUCTOR, null, 0, false);
            node.RSeg.setSymbol(variantSymbol);
            // 变体的类型是包装枚举类型的EnumConstructorType
            EnumConstructorType enumConstructorType = new EnumConstructorType(enumType);
            variantSymbol.setType(enumConstructorType);
        } else {
            // 如果不是变体，在枚举的impl项中查找
            Symbol enumSymbol = enumType.getEnumSymbol();
            if (enumSymbol != null) {
                // 遍历impl符号以查找匹配名称的函数或常量
                for (Symbol implSymbol : enumSymbol.getImplSymbols()) {
                    if ((implSymbol.getKind() == SymbolKind.FUNCTION ||
                            implSymbol.getKind() == SymbolKind.CONSTANT) &&
                        implSymbol.getName().equals(rSegName)) {
                        // 找到关联项，将其用作RSeg符号
                        node.RSeg.setSymbol(implSymbol);
                        break;
                    }
                }
                
                // 如果我们在impl符号中没有找到它，报告错误
                if (node.RSeg.getSymbol() == null) {
                    RuntimeException error = new RuntimeException(
                        "Variant or associated item '" + rSegName + "' not found in enum " + enumType.getName()
                    );
                    handleError(error);
                }
            } else {
                RuntimeException error = new RuntimeException(
                    "Variant '" + rSegName + "' not found in enum " + enumType.getName()
                );
                handleError(error);
            }
        }
    }
    
    /**
     * 处理trait的RSeg
     */
    private void handleTraitRSeg(PathExprNode node, TraitType traitType) {
        String itemName = node.RSeg != null && node.RSeg.name != null ? node.RSeg.name.name : null;
        
        if (itemName == null) {
            RuntimeException error = new RuntimeException(
                "Unresolved associated item name in path expression"
            );
            handleError(error);
            return;
        }
        
        // 检查它是否是方法
        if (traitType.hasMethod(itemName)) {
            FunctionType methodType = traitType.getMethodType(itemName);
            Symbol methodSymbol = new Symbol(itemName, SymbolKind.FUNCTION, null, 0, false);
            node.RSeg.setSymbol(methodSymbol);
            methodSymbol.setType(methodType);
        }
        // 检查它是否是常量
        else if (traitType.hasConstant(itemName)) {
            Type constantType = traitType.getConstantType(itemName);
            Symbol constantSymbol = new Symbol(itemName, SymbolKind.CONSTANT, null, 0, false);
            node.RSeg.setSymbol(constantSymbol);
            constantSymbol.setType(constantType);
        }
        else {
            RuntimeException error = new RuntimeException(
                "Associated item '" + itemName + "' not found in trait " + traitType.getName()
            );
            handleError(error);
        }
    }
    
    /**
     * 访问路径表达式段
     */
    public void visit(PathExprSegNode node) {
        try {
            // 从路径段获取符号
            Symbol symbol = node.getSymbol();
            if (symbol == null) {
                RuntimeException error = new RuntimeException(
                    "Unresolved symbol: " + (node.name != null ? node.name.name : "unknown")
                );
                handleError(error);
                return;
            }
            
            // 从符号提取类型
            Type type = typeExtractor.extractTypeFromSymbol(symbol);
            // 注意：PathExprSegNode没有setType方法，所以我们只将它存储在符号中
            symbol.setType(type);
            
            // 确保符号设置回节点
            node.setSymbol(symbol);
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问分组表达式
     */
    public void visit(GroupExprNode node) {
        try {
            if (node.innerExpr != null) {
                // 使用递归访问确保使用正确的类型检查器
                visitExpression(node.innerExpr);
                Type innerType = getTypeWithoutNullCheck(node.innerExpr);
                setType(node, innerType);
            } else {
                RuntimeException error = new RuntimeException(
                    "Grouped expression has no inner expression"
                );
                handleError(error);
            }
        } catch (RuntimeException e) {
            handleError(e);
        }
    }
    
    /**
     * 访问下划线表达式
     */
    public void visit(UnderscoreExprNode node) {
        // 下划线用作通配符模式，匹配任何值但不绑定它
        // 它是一个占位符，可以在上下文中推断为任何类型
        // 目前，我们使用INT作为可以推断为其他数字类型的占位符类型
        setType(node, PrimitiveType.getIntType());
    }
}