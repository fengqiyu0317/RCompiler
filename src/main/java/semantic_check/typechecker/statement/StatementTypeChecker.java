
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
                    selfType = new ReferenceType(receiverType, node.selfPara.isMutable);
                }
                
                // Set current Self type for use in method body
                typeChecker.setCurrentType(selfType);
            }
            
            // Visit function body if it exists
            if (node.body != null) {
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
            // 首先进行mutability检查
            // if (mutabilityChecker != null) {
            //     mutabilityChecker.checkMutability(node);
            // }
            
            // Visit value if it exists
            if (node.value != null) {
                node.value.accept(typeChecker);
                Type valueType = node.value.getType();
                
                // If there's an explicit type, check compatibility
                if (node.type != null) {
                    Type declaredType = typeExtractor.extractTypeFromTypeNode(node.type);
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
                }
            }
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
        // Parameter type is already processed when function type is created
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
}