/**
 * 简单表达式类型检查器
 * 处理字面量、路径表达式、分组表达式、下划线表达式等
 */

public class SimpleExpressionTypeChecker extends VisitorBase {
    protected final TypeErrorCollector errorCollector;
    protected final boolean throwOnError;
    protected final TypeExtractor typeExtractor;
    protected final ConstantEvaluator constantEvaluator;
    protected final ExpressionTypeContext context;
    protected final TypeChecker mainExpressionChecker;
    protected MutabilityChecker mutabilityChecker;
    
    public SimpleExpressionTypeChecker(
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
                literalType = new ReferenceType(PrimitiveType.getStrType(), true, true); // &mut str
                break;
            case CSTRING:
                literalType = new ReferenceType(PrimitiveType.getStrType(), true, true); // &mut str
                break;
            default:
                // 默认为未确定的整数类型
                literalType = PrimitiveType.getIntType();
                break;
        }
        
        // For STRING and CSTRING, we already created a mutable reference type
        // For other types, create a mutable version
        if (node.literalType == literal_t.STRING || node.literalType == literal_t.CSTRING) {
            setType(node, literalType);
        } else {
            Type mutableLiteralType = TypeUtils.createMutableType(literalType, true);
            setType(node, mutableLiteralType);
        }
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
        
            // 处理RSeg（如果存在）
            if (node.RSeg != null) {
                // 基于LSeg的符号获取RSeg的符号
                Symbol rSegSymbol = node.RSeg != null ? node.RSeg.getSymbol() : null;
                if (rSegSymbol == null) {
                    // 如果RSeg没有符号，我们需要基于LSeg的符号来解析它
                    // 这通常在访问字段、方法或关联项时发生
                    if (node.getSymbol() != null) {
                        Symbol lSegSymbol = node.getSymbol();
                        Type lSegType = typeExtractor.extractTypeFromSymbol(lSegSymbol);
                        
                        // 如果LSeg是结构体构造函数类型，RSeg可能是字段或实现的函数
                        if (lSegType instanceof StructType) {
                            StructType structType = (StructType) lSegType;
                            handleStructRSeg(node, structType);
                        }
                        // 如果LSeg是枚举构造函数类型，RSeg可能是变体或impl项
                        else if (lSegType instanceof EnumType) {
                            EnumType enumType = (EnumType) lSegType;
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
            // 改进：通过LSeg符号找到枚举AST节点，然后找到对应的变体符号
            Symbol enumSymbol = enumType.getEnumSymbol();

            if (enumSymbol == null) {
                throw new RuntimeException(
                    "Unresolved symbol for enum: " + enumType.getName()
                );
            }
            
            EnumNode enumNode = (EnumNode) enumSymbol.getDeclaration();
            
            for (IdentifierNode variantNode : enumNode.variants) {
                if (variantNode.name.equals(rSegName)) {
                    // 使用变体AST节点的符号
                    Symbol variantSymbol = variantNode.getSymbol();
                    if (variantSymbol == null) {
                        throw new RuntimeException(
                            "Unresolved symbol for enum variant: " + rSegName
                        );
                    }
                    
                    variantSymbol.setType(enumType); // 变体的类型是枚举类型
                    
                    // 将符号设置到RSeg
                    node.RSeg.setSymbol(variantSymbol);
                    break;
                }
            }
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
        
        // 从trait的impl符号中查找关联项
        Symbol traitSymbol = traitType.getTraitSymbol();
        if (traitSymbol != null) {
            // 遍历impl符号以查找匹配名称的函数或常量
            for (Symbol implSymbol : traitSymbol.getImplSymbols()) {
                if ((implSymbol.getKind() == SymbolKind.FUNCTION ||
                        implSymbol.getKind() == SymbolKind.CONSTANT) &&
                    implSymbol.getName().equals(itemName)) {
                    // 找到关联项，将其用作RSeg符号
                    node.RSeg.setSymbol(implSymbol);
                    return;
                }
            }
        }
        
        // 如果我们在impl符号中没有找到它，报告错误
        RuntimeException error = new RuntimeException(
            "Associated item '" + itemName + "' not found in trait " + traitType.getName()
        );
        handleError(error);
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
                // 使用TypeChecker进行调用
                node.innerExpr.accept(mainExpressionChecker);
                Type innerType = node.innerExpr.getType();
                Type mutableType = TypeUtils.createMutableType(innerType, true);
                setType(node, mutableType);
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
        // 使用 UnderscoreType，它不能和其它任何type兼容但任何type都能兼容它
        setType(node, UnderscoreType.INSTANCE);
    }
}