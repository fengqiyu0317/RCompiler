/**
 * Class responsible for type checking statements
 */
public class StatementTypeChecker extends VisitorBase {
    private final TypeErrorCollector errorCollector;
    private final boolean throwOnError;
    private final TypeExtractor typeExtractor;
    private final TypeChecker typeChecker;
    private final ControlFlowTypeChecker controlFlowTypeChecker;
    
    // Mutability检查器
    private final MutabilityChecker mutabilityChecker;
    
    // Ownership检查器
    private final OwnershipChecker ownershipChecker;
    
    // This constructor is deprecated - use the one with TypeChecker
    public StatementTypeChecker(TypeErrorCollector errorCollector, boolean throwOnError,
                             TypeExtractor typeExtractor, ExpressionTypeChecker expressionTypeChecker,
                             ControlFlowTypeChecker controlFlowTypeChecker) {
        throw new RuntimeException("This constructor is deprecated. Use the constructor with TypeChecker.");
    }
    
    public StatementTypeChecker(TypeErrorCollector errorCollector, boolean throwOnError,
                             TypeExtractor typeExtractor, TypeChecker typeChecker,
                             ControlFlowTypeChecker controlFlowTypeChecker) {
        this.errorCollector = errorCollector;
        this.throwOnError = throwOnError;
        this.typeExtractor = typeExtractor;
        this.typeChecker = typeChecker;
        this.controlFlowTypeChecker = controlFlowTypeChecker;
        this.mutabilityChecker = new MutabilityChecker(errorCollector, throwOnError);
        this.ownershipChecker = new OwnershipChecker(errorCollector, throwOnError);
    }
    
    // ========================================
    // 顶层定义和容器节点
    // ========================================
    
    // Visit base item node
    public void visit(ItemNode node) {
        // 这是一个抽象基类，在正确的访问者模式实现中不应该到达这里
        throw new RuntimeException(
            "Cannot visit abstract ItemNode directly in StatementTypeChecker"
        );
    }
    
    // Visit function node
    public void visit(FunctionNode node) throws RuntimeException {
        // Save previous Self type to restore later
        Type previousSelfType = typeChecker.getCurrentSelfType();
        
        try {
            // Enter function context in type checker
            typeChecker.enterFunctionContext(node);
            
            // Enter function context in control flow checker
            controlFlowTypeChecker.enterFunctionContext(node);
            
            // Set current Self type if it's a method
            if (node.selfPara != null) {
                // For methods, Self refers to receiver type
                // We need to determine type of receiver based on impl block
                
                // First, find parent impl node to determine receiver type
                ASTNode parent = node.getFather();
                Type receiverType = null;
                
                while (parent != null && !(parent instanceof ImplNode)) {
                    parent = parent.getFather();
                }
                
                if (parent instanceof ImplNode) {
                    ImplNode implNode = (ImplNode) parent;
                    // Get type symbol from impl node
                    Symbol typeSymbol = implNode.getTypeSymbol();
                    if (typeSymbol != null) {
                        try {
                            receiverType = typeExtractor.extractTypeFromSymbol(typeSymbol);
                        } catch (RuntimeException e) {
                            // Handle error extracting type from symbol
                            if (throwOnError) {
                                throw e;
                            } else {
                                errorCollector.addError(e);
                            }
                        }
                    } else {
                        // Handle missing type symbol
                        RuntimeException error = new RuntimeException(
                            "Impl block missing type symbol for method receiver"
                        );
                        if (throwOnError) {
                            throw error;
                        } else {
                            errorCollector.addError(error.getMessage());
                        }
                    }
                }
                
                // If we couldn't determine receiver type, use a placeholder
                if (receiverType == null) {
                    throw new RuntimeException(
                        "Unable to determine receiver type for method"
                    );
                }
                
                // Handle self parameter based on its type and mutability
                Type selfType = receiverType;
                
                // If self is a reference, create a reference type
                if (node.selfPara.isReference) {
                    // For self parameters, both reference and value mutability match the parameter mutability
                    selfType = new ReferenceType(receiverType, true, node.selfPara.isMutable);
                }
                
                // Set current Self type for use in method body
                typeChecker.setCurrentType(selfType);
            }
            
            // Visit function parameters to extract their types
            if (node.parameters != null) {
                for (ParameterNode param : node.parameters) {
                    param.accept(this);
                }
            }
            
            // Visit function body if it exists
            if (node.body != null) {
                // Check if this is the main function
                boolean isMainFunction = node.name != null && "main".equals(node.name.name);
                
                // For main function, check exit function usage before visiting body
                if (isMainFunction && node.body instanceof BlockExprNode) {
                    checkMainFunctionExitUsage((BlockExprNode) node.body);
                }
                
                // Visit function body
                node.body.accept(typeChecker);
                
                // Get expected return type
                Type expectedReturnType = node.returnType != null ?
                                       typeExtractor.extractTypeFromTypeNode(node.returnType) :
                                       UnitType.INSTANCE;
                
                // Get actual body type
                Type bodyType;
                bodyType = node.body.getType();
                
                // Check compatibility between body type and expected return type
                if (!TypeUtils.isTypeCompatible(bodyType, expectedReturnType)) {
                    RuntimeException error = new RuntimeException(
                        "Function body type mismatch: expected " + expectedReturnType +
                        ", got " + bodyType + " in function '" +
                        (node.name != null ? node.name.name : "anonymous") + "'"
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                }
                
            }
            
            // Restore previous Self type
            if (previousSelfType != null) {
                typeChecker.setCurrentType(previousSelfType);
            } else {
                typeChecker.clearCurrentType();
            }
            typeChecker.exitFunctionContext();
            
            // Exit function context in control flow checker
            controlFlowTypeChecker.exitCurrentContext();
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e);
            }
        }
    }
    
    // Visit impl node
    public void visit(ImplNode node) {
        // Type and trait symbols are already set by SymbolAdder
        // Set Self type context for associated items (constants) that might use Self
        Type previousSelfType = typeChecker.getCurrentSelfType();
        
        try {
            // Extract type from impl node's type symbol
            Symbol typeSymbol = node.getTypeSymbol();
            if (typeSymbol != null) {
                Type implType = typeExtractor.extractTypeFromSymbol(typeSymbol);
                // Set Self type for impl block
                typeChecker.setCurrentType(implType);
            } else {
                // Handle missing type symbol
                RuntimeException error = new RuntimeException(
                    "Impl block missing type symbol"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
            }
            
            // Visit items in impl block
            if (node.items != null) {
                for (AssoItemNode item : node.items) {
                    item.accept(typeChecker);
                }
            }
        } finally {
            // Restore previous Self type
            if (previousSelfType != null) {
                typeChecker.setCurrentType(previousSelfType);
            } else {
                typeChecker.clearCurrentType();
            }
        }
    }
    
    // Visit associated item node
    public void visit(AssoItemNode node) {
        // Either function or constant should be not null
        if (node.function != null) {
            node.function.accept(typeChecker);
        } else if (node.constant != null) {
            node.constant.accept(typeChecker);
        }
    }
    
    // Visit const item node
    public void visit(ConstItemNode node) throws RuntimeException {
        try {
            // Visit value if it exists
            if (node.value != null) {
                // First, type-check value expression
                node.value.accept(typeChecker);
                Type valueType = node.value.getType();
                
                // If there's an explicit type, check compatibility
                if (node.type != null) {
                    Type declaredType = typeExtractor.extractTypeFromTypeNode(node.type);
                    if (!TypeUtils.isTypeCompatible(valueType, declaredType)) {
                        RuntimeException error = new RuntimeException(
                            "Cannot assign " + valueType + " to constant of type " + declaredType
                        );
                        if (throwOnError) {
                            throw error;
                        } else {
                            errorCollector.addError(error.getMessage());
                        }
                        return;
                    }
                } else {
                    // we throw error if no explicit type for constant
                    RuntimeException error = new RuntimeException(
                        "Constant must have an explicit type"
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                }
                
                // Now check if value is a constant expression
                // This will be handled by the ConstantEvaluator in the main TypeChecker
            }
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit struct node
    public void visit(StructNode node) {
        // Struct definitions don't need type checking themselves
        // The struct type is created when symbol is processed
    }
    
    // Visit enum node
    public void visit(EnumNode node) {
        // Enum definitions don't need type checking themselves
        // The enum type is created when symbol is processed
    }
    
    // Visit trait node
    public void visit(TraitNode node) {
        // Trait definitions don't need type checking themselves
    }
    
    // Visit builtin function node
    public void visit(BuiltinFunctionNode node) {
        // Builtin functions are handled by parent FunctionNode visit method
        // No additional type checking needed here
        node.accept(this);
    }
    
    // ========================================
    // 语句节点
    // ========================================
    
    // Visit let statement node
    public void visit(LetStmtNode node) throws RuntimeException {
        try {
            Type variableType = null;
            
            // Visit value if it exists
            if (node.value != null) {
                node.value.accept(typeChecker);
                Type valueType = node.value.getType();
                
                // If there's an explicit type, check compatibility
                if (node.type != null) {
                    Type declaredType = typeExtractor.extractTypeFromTypeNode(node.type);
                    // 如果变量声明为可变，确保声明类型也是可变的
                    if (node.name instanceof IdPatNode) {
                        // 需判断declaredType是否为reference type再设置mutability
                        IdPatNode idPat = (IdPatNode) node.name;
                        if (idPat.isMutable && declaredType != null) {
                            declaredType.setMutability(true);
                        } 
                    }
                    if (!TypeUtils.isTypeCompatible(valueType, declaredType)) {
                        RuntimeException error = new RuntimeException(
                            "Cannot assign " + valueType + " to variable of type " + declaredType
                        );
                        if (throwOnError) {
                            throw error;
                        } else {
                            errorCollector.addError(error.getMessage());
                        }
                        return;
                    }
                    variableType = declaredType;
                } else {
                    // No explicit type, use inferred type from value
                    variableType = valueType;
                }
            } else if (node.type != null) {
                // No value but explicit type
                variableType = typeExtractor.extractTypeFromTypeNode(node.type);
                // 如果变量声明为可变，确保声明类型也是可变的
                if (node.name instanceof IdPatNode) {
                    IdPatNode idPat = (IdPatNode) node.name;
                    if (idPat.isMutable && variableType != null) {
                        variableType.setMutability(true);
                    }
                }
            }
            
            // Store the variable type in the LetStmtNode for later retrieval
            node.setVariableType(variableType);
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit expression statement node
    public void visit(ExprStmtNode node) {
        try {
            // Visit expression if it exists
            if (node.expr != null) {
                node.expr.accept(typeChecker);
                Type exprType = node.expr.getType();
                
                // Set the type of the statement based on the expression
                // For expression statements, the type is typically unit type unless it's the last expression in a block
                if (exprType != null) {
                    // If the expression statement has a semicolon, it's unit type
                    // If it doesn't have a semicolon, it's the expression's type
                    if (node.hasSemicolon) {
                        // ExprStmtNode doesn't have setType method, so we need to handle this differently
                        // We'll store the type in the expression node itself
                        if(node.expr.getType() != NeverType.INSTANCE) {
                            node.expr.setType(UnitType.INSTANCE);
                        }
                    } else {
                        // The expression already has its type set
                        // No need to do anything here
                    }
                } else {
                    // throw error if expression type is null
                    RuntimeException error = new RuntimeException(
                        "Expression in expression statement has no type"
                    );
                    if (throwOnError) {
                        throw error;
                    } else {
                        errorCollector.addError(error.getMessage());
                    }
                }
            } else {
                // No expression, nothing to do
            }
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // ========================================
    // 表达式和标识符节点
    // ========================================
    
    // Visit identifier node
    public void visit(IdentifierNode node) {
        // Get symbol from identifier
        Symbol symbol = node.getSymbol();
        if (symbol == null) {
            RuntimeException error = new RuntimeException(
                "Unresolved symbol: " + (node.name != null ? node.name : "unknown")
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
            return;
        }
        
        // Extract type from symbol
        Type type = typeExtractor.extractTypeFromSymbol(symbol);
        // Note: IdentifierNode doesn't have a setType method, so we just store it in the symbol
        symbol.setType(type);
        
        // Ensure that symbol is set back to node
        node.setSymbol(symbol);
    }
    
    // Visit type path expression node
    public void visit(TypePathExprNode node) {
        // TypePathExprNode is handled by TypeExtractor when called by parent nodes
        // No need to recursively process children here as TypeExtractor does the work
        // This method is kept for consistency with visitor pattern
    }
    
    // Visit type reference expression node
    public void visit(TypeRefExprNode node) {
        // Type reference expressions are processed in TypeExtractor
        // No additional type checking needed here
    }
    
    // Visit type array expression node
    public void visit(TypeArrayExprNode node) {
        // Type array expressions are processed in TypeExtractor
        // Just need to visit element type and size if they exist
        if (node.elementType != null) {
            node.elementType.accept(typeChecker);
        }
        if (node.size != null) {
            node.size.accept(typeChecker);
        }
    }
    
    // Visit type unit expression node
    public void visit(TypeUnitExprNode node) {
        // Unit type expressions are processed in TypeExtractor
        // No additional type checking needed here
    }
    
    // ========================================
    // 模式相关节点
    // ========================================
    
    // Visit pattern node
    public void visit(PatternNode node) {
        // Pattern node is abstract, actual implementation is in subclasses
        // This method should not be called directly
    }
    
    // Visit identifier pattern node
    public void visit(IdPatNode node) {
        // Identifier pattern type is determined by context
        // No additional type checking needed here
    }
    
    // Visit wildcard pattern node
    public void visit(WildPatNode node) {
        // Wildcard pattern matches any type
        // No additional type checking needed here
    }
    
    // Visit reference pattern node
    public void visit(RefPatNode node) {
        // Reference pattern type is determined by context
        // Just need to visit inner pattern if it exists
        if (node.innerPattern != null) {
            node.innerPattern.accept(typeChecker);
        }
    }
    
    // ========================================
    // 参数和字段节点
    // ========================================
    
    // Visit self parameter node
    public void visit(SelfParaNode node) {
        // Self parameter type is determined by impl block
        // No additional type checking needed here
    }
    
    // Visit parameter node
    public void visit(ParameterNode node) {
        try {
            Type paramType = null;
            
            // Extract type from type annotation if it exists
            if (node.type != null) {
                paramType = typeExtractor.extractTypeFromTypeNode(node.type);
                
                // Handle mutability for parameter type
                if (node.name instanceof IdPatNode) {
                    IdPatNode idPat = (IdPatNode) node.name;
                    if (idPat.isMutable && paramType != null) {
                        paramType.setMutability(true);
                    }
                }
            }
            
            // Store the parameter type in the ParameterNode for later retrieval
            node.setParameterType(paramType);
        } catch (RuntimeException e) {
            if (throwOnError) {
                throw e;
            } else {
                errorCollector.addError(e.getMessage());
            }
        }
    }
    
    // Visit field node
    public void visit(FieldNode node) {
        // we shouldn't reach here because we won't visit fields directly
        throw new RuntimeException(
            "Cannot visit FieldNode directly in StatementTypeChecker"
        );
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
     * 检查main函数中exit函数的使用情况
     * 确保exit函数只在main函数的最后一条语句中被调用
     */
    private void checkMainFunctionExitUsage(BlockExprNode mainBody) {
        if (mainBody.returnValue != null) {
            // 将这个returnValue作为最后一条语句插入检查
            ExprStmtNode returnStmt = new ExprStmtNode();
            returnStmt.expr = mainBody.returnValue;
            mainBody.statements.add(returnStmt);
        }
        // 检查除最后一条语句外的所有语句，确保它们不包含exit调用
        for (int i = 0; i < mainBody.statements.size() - 1; i++) {
            ASTNode stmt = mainBody.statements.get(i);
            if (containsExitCall(stmt)) {
                RuntimeException error = new RuntimeException(
                    "exit() function must be the last statement in main function"
                );
                if (throwOnError) {
                    throw error;
                } else {
                    errorCollector.addError(error.getMessage());
                }
            }
        }
        
        // 检查最后一条语句，确保它包含exit调用或者是返回值中的exit调用
        ASTNode lastStmt = mainBody.statements.get(mainBody.statements.size() - 1);
        boolean lastStmtHasExit = containsExitCall(lastStmt);
        
        if (!lastStmtHasExit) {
            RuntimeException error = new RuntimeException(
                "exit() function must be the last statement in main function"
            );
            if (throwOnError) {
                throw error;
            } else {
                errorCollector.addError(error.getMessage());
            }
        }
    }
    
    /**
     * 检查节点是否包含exit函数调用
     */
    private boolean containsExitCall(ASTNode node) {
        if (node == null || !(node instanceof ExprStmtNode)) {
            return false;
        }

        node = ((ExprStmtNode) node).expr;
        
        // 如果是CallExprNode，检查是否调用exit函数
        if (node instanceof CallExprNode) {
            CallExprNode callExpr = (CallExprNode) node;
            if (callExpr.function instanceof PathExprNode) {
                PathExprNode pathExpr = (PathExprNode) callExpr.function;
                if (pathExpr.LSeg != null && pathExpr.LSeg.name != null &&
                    "exit".equals(pathExpr.LSeg.name.name)) {
                    return true;
                }
            }
        }

        return false;
    }
    
}