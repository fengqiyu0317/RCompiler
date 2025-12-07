// SymbolChecker class - Second phase of namespace analysis
// This class is responsible for checking all symbol references
// and adding temporary symbols for let statements and parameters

import java.util.Vector;
import java.util.Stack;

public class SymbolChecker extends VisitorBase {
    // Global scope (from SymbolAdder)
    private NamespaceSymbolTable globalScope;
    
    // Current scope
    private NamespaceSymbolTable currentScope;
    
    // Current context
    private Context currentContext;
    
    // Current type name (for field processing)
    private String currentTypeName;
    
    // Stack to track current child scope indices at each level
    private Stack<Integer> childScopeIndexStack;
    
    // Constructor
    public SymbolChecker(NamespaceSymbolTable globalScope) {
        this.globalScope = globalScope;
        this.currentScope = globalScope;
        this.currentContext = Context.VALUE_CONTEXT; // Default value context
        this.childScopeIndexStack = new Stack<>(); // Initialize stack
        this.childScopeIndexStack.push(0); // Start with index 0 for global scope
    }
    
    // Set context
    private void setContext(Context context) {
        this.currentContext = context;
    }
    
    // Enter new scope - navigate to existing child scope created by SymbolAdder
    private void enterScope() {
        // Check if there are any child scopes
        if (currentScope.getChildren().isEmpty()) {
            throw new RuntimeException("No child scope available to enter");
        }
        
        // Get the current child index from the top of the stack
        int currentChildIndex = childScopeIndexStack.peek();
        
        // Check if the pointer is within bounds
        if (currentChildIndex >= currentScope.getChildren().size()) {
            throw new RuntimeException("Child scope pointer out of bounds");
        }
        
        // Navigate to the child scope using the pointer
        currentScope = currentScope.getChildren().get(currentChildIndex);
        
        // Push a new index for the new scope (starting at 0)
        childScopeIndexStack.push(0);
    }
    
    // Exit current scope - navigate back to parent scope
    private void exitScope() {
        // Pop the current scope's index from the stack
        childScopeIndexStack.pop();
        
        // Get the parent scope
        NamespaceSymbolTable parentScope = currentScope.getParent();
        if (parentScope == null) {
            throw new RuntimeException("Attempting to exit global scope");
        }
        
        // Increment the parent's child scope pointer (now at top of stack)
        if (!childScopeIndexStack.isEmpty()) {
            int parentNextIndex = childScopeIndexStack.peek() + 1;
            childScopeIndexStack.pop();
            childScopeIndexStack.push(parentNextIndex);
        }
        
        currentScope = parentScope;
    }
    
    // Get segment text
    private String getSegmentText(PathExprSegNode segment) {
        if (segment.patternType == patternSeg_t.IDENT) {
            return segment.name.name;
        } else if (segment.patternType == patternSeg_t.SELF) {
            return "self";
        } else if (segment.patternType == patternSeg_t.SELF_TYPE) {
            return "Self";
        }
        return "";
    }
    
    // Process identifier
    @Override
    public void visit(IdentifierNode node) {
        String identifierName = node.name;
        Symbol resolvedSymbol = null;
        
        switch (currentContext) {
            case TYPE_CONTEXT:
                // Lookup in type namespace
                Symbol typeSymbol = currentScope.lookupType(identifierName);
                if (typeSymbol == null) {
                    throw new SemanticException(
                        String.format("Type '%s' not found",
                                     identifierName),
                        node
                    );
                } else {
                    resolvedSymbol = typeSymbol;
                }
                break;
                
            case VALUE_CONTEXT:
                // Lookup in value namespace
                Symbol valueSymbol = currentScope.lookupValue(identifierName);
                if (valueSymbol == null) {
                    throw new SemanticException(
                        String.format("Value '%s' not found",
                                     identifierName),
                        node
                    );
                } else {
                    resolvedSymbol = valueSymbol;
                }
                break;
                
            case FIELD_CONTEXT:
                // Field context requires special handling, look up field in currentTypeName's symbol
                if (currentTypeName == null) {
                    throw new SemanticException(
                        String.format("Field context used without a current type name for identifier '%s'",
                                     identifierName),
                        node
                    );
                }
                
                // Look up the field in the field namespace for the current type
                Symbol fieldSymbol = currentScope.lookupField(currentTypeName, identifierName);
                if (fieldSymbol == null) {
                    throw new SemanticException(
                        String.format("Field '%s' not found in type '%s'",
                                     identifierName, currentTypeName),
                        node
                    );
                } else {
                    resolvedSymbol = fieldSymbol;
                }
                break;
        }
        
        // Record identifier pointing to symbol
        node.setSymbol(resolvedSymbol);
    }
    
    // Helper method to add identifier symbol
    private void addIdentifierSymbol(IdentifierNode node, SymbolKind kind, boolean isMutable) {
        // Create symbol
        Symbol symbol = new Symbol(
            node.name,
            kind,
            node,
            currentScope.getScopeLevel(),
            isMutable
        );
        
        // Add to value namespace
        currentScope.addValueSymbol(symbol);
        
        // Set the identifier's symbol
        node.setSymbol(symbol);
    }
    
    // Process let statement
    @Override
    public void visit(LetStmtNode node) {
        // First process value expression (if any)
        if (node.value != null) {
            Context valueContext = currentContext;
            setContext(Context.VALUE_CONTEXT);
            
            try {
                node.value.accept(this);
            } finally {
                // Restore context
                setContext(valueContext);
            }
        }
        
        // Then process type (if any)
        if (node.type != null) {
            Context typeContext = currentContext;
            setContext(Context.TYPE_CONTEXT);
            
            try {
                node.type.accept(this);
            } finally {
                // Restore context
                setContext(typeContext);
            }
        }
        
        // Then process pattern and add symbol after processing value and type
        Context previousContext = currentContext;
        setContext(Context.VALUE_CONTEXT);
        
        try {
            // Add the symbol for the let statement
            if (node.name != null) {
                // Process the pattern to extract identifier
                processLetPattern(node.name);
            }
        } finally {
            // Restore context
            setContext(previousContext);
        }
    }
    
    // Helper method to process let pattern and add symbol
    private void processLetPattern(PatternNode pattern) {
        if (pattern instanceof IdPatNode) {
            IdPatNode idPat = (IdPatNode) pattern;
            if (idPat.name != null) {
                // Create let variable symbol
                Symbol letSymbol = new Symbol(
                    idPat.name.name,
                    SymbolKind.LOCAL_VARIABLE,
                    idPat.name,
                    currentScope.getScopeLevel(),
                    idPat.isMutable
                );
                
                // Add to value namespace
                currentScope.addValueSymbol(letSymbol);
                
                // Set the identifier's symbol
                idPat.name.setSymbol(letSymbol);
            }
        } else if (pattern instanceof RefPatNode) {
            RefPatNode refPat = (RefPatNode) pattern;
            // Recursively process the inner pattern
            processLetPattern(refPat.innerPattern);
        }
        // WildPatNode doesn't need symbol addition
    }
    
    // Process expression statement
    @Override
    public void visit(ExprStmtNode node) {
        // Set value context
        Context previousContext = currentContext;
        setContext(Context.VALUE_CONTEXT);
        
        try {
            // Process expression
            node.expr.accept(this);
        } finally {
            // Restore context
            setContext(previousContext);
        }
    }
    
    // Process function declaration
    @Override
    public void visit(FunctionNode node) {
        // Function symbol is already set by SymbolAdder
        
        try {
            // Enter function scope
            enterScope();
            
            // Process self parameter (if any)
            if (node.selfPara != null) {
                node.selfPara.accept(this);
            }
            
            // Process parameter types first, but don't add symbols yet
            if (node.parameters != null) {
                for (ParameterNode param : node.parameters) {
                    // Process parameter type in type context first
                    if (param.type != null) {
                        Context typeContext = currentContext;
                        setContext(Context.TYPE_CONTEXT);
                        
                        try {
                            param.type.accept(this);
                        } finally {
                            // Restore context
                            setContext(typeContext);
                        }
                    }
                }
                
                // Now add all parameter symbols just before processing the body
                for (ParameterNode param : node.parameters) {
                    if (param.name != null) {
                        processParameterPattern(param.name);
                    }
                }
            }
            
            // Process return type (if any)
            if (node.returnType != null) {
                Context returnContext = currentContext;
                setContext(Context.TYPE_CONTEXT);
                
                try {
                    node.returnType.accept(this);
                } finally {
                    // Restore context
                    setContext(returnContext);
                }
            }
            
            // Process function body in value context
            if (node.body != null) {
                Context bodyContext = currentContext;
                setContext(Context.VALUE_CONTEXT);
                
                try {
                    node.body.accept(this);
                } finally {
                    // Restore context
                    setContext(bodyContext);
                }
            }
            
            // Exit function scope
            exitScope();
            
        } catch (SemanticException e) {
            throw e;
        }
    }
    
    
    // Process self parameter
    @Override
    public void visit(SelfParaNode node) {
        // Process self parameter type (if any) in type context first
        if (node.type != null) {
            Context typeContext = currentContext;
            setContext(Context.TYPE_CONTEXT);
            
            try {
                node.type.accept(this);
            } finally {
                // Restore context
                setContext(typeContext);
            }
        }
        
        // Self symbol is already set by SymbolAdder
    }
    
    // Process parameter
    @Override
    public void visit(ParameterNode node) {
        // Recursively process parameter type (if any)
        if (node.type != null) {
            Context typeContext = currentContext;
            setContext(Context.TYPE_CONTEXT);
            try {
                node.type.accept(this);
            } finally {
                // Restore context
                setContext(typeContext);
            }
        }
    }
    
    // Helper method to process parameter pattern and add symbol
    private void processParameterPattern(PatternNode pattern) {
        if (pattern instanceof IdPatNode) {
            IdPatNode idPat = (IdPatNode) pattern;
            if (idPat.name != null) {
                // Create parameter symbol
                Symbol paramSymbol = new Symbol(
                    idPat.name.name,
                    SymbolKind.PARAMETER,
                    idPat.name,
                    currentScope.getScopeLevel(),
                    idPat.isMutable
                );
                
                // Add to value namespace
                currentScope.addValueSymbol(paramSymbol);
                
                // Set the identifier's symbol
                idPat.name.setSymbol(paramSymbol);
            }
        } else if (pattern instanceof RefPatNode) {
            RefPatNode refPat = (RefPatNode) pattern;
            // Recursively process the inner pattern
            processParameterPattern(refPat.innerPattern);
        }
        // WildPatNode doesn't need symbol addition
    }
    
    // Process struct declaration
    @Override
    public void visit(StructNode node) {
        // Struct symbol is already set by SymbolAdder
        
        try {
            // Set current type name for field processing
            String previousTypeName = currentTypeName;
            currentTypeName = node.name.name;
            
            // Process fields
            if (node.fields != null) {
                for (FieldNode field : node.fields) {
                    field.accept(this);
                }
            }
            
            // Restore current type name
            currentTypeName = previousTypeName;
            
        } catch (SemanticException e) {
            throw e;
        }
    }
    
    // Process field declaration
    @Override
    public void visit(FieldNode node) {
        // Field symbol is already set by SymbolAdder
        
        // Process field type in type context
        if (node.type != null) {
            Context typeContext = currentContext;
            setContext(Context.TYPE_CONTEXT);
            
            try {
                node.type.accept(this);
            } finally {
                // Restore context
                setContext(typeContext);
            }
        }
    }
    
    // Process enum declaration
    @Override
    public void visit(EnumNode node) {
        // Enum symbol is already set by SymbolAdder
        
        try {
            // Enter enum scope for variants
            enterScope();
            
            // Process enum variants
            if (node.variants != null) {
                for (IdentifierNode variant : node.variants) {
                    // Variant symbols are already set by SymbolAdder
                    variant.accept(this);
                }
            }
            
            // Exit enum scope
            exitScope();
            
        } catch (SemanticException e) {
            throw e;
        }
    }
    
    // Process constant declaration
    @Override
    public void visit(ConstItemNode node) {
        // Const symbol is already set by SymbolAdder
        
        try {
            // Process constant type (if any)
            if (node.type != null) {
                Context typeContext = currentContext;
                setContext(Context.TYPE_CONTEXT);
                
                try {
                    node.type.accept(this);
                } finally {
                    // Restore context
                    setContext(typeContext);
                }
            }
            
            // Process constant value (if any)
            if (node.value != null) {
                Context valueContext = currentContext;
                setContext(Context.VALUE_CONTEXT);
                
                try {
                    node.value.accept(this);
                } finally {
                    // Restore context
                    setContext(valueContext);
                }
            }
            
        } catch (SemanticException e) {
            throw e;
        }
    }
    
    // Process trait declaration
    @Override
    public void visit(TraitNode node) {
        // Trait symbol is already set by SymbolAdder
        
        try {
            // Enter trait scope for associated items
            enterScope();
            
            // Process associated items in trait
            if (node.items != null) {
                for (AssoItemNode item : node.items) {
                    item.accept(this);
                }
            }
            
            // Exit trait scope
            exitScope();
            
        } catch (SemanticException e) {
            throw e;
        }
    }
    
    // Process impl declaration
    @Override
    public void visit(ImplNode node) {
        // Type and trait symbols are already set by SymbolAdder
        
        // Process type name in type context
        Context typeContext = currentContext;
        setContext(Context.TYPE_CONTEXT);
        
        try {
            node.typeName.accept(this);
        } finally {
            // Restore context
            setContext(typeContext);
        }
        
        // Process trait name (if any)
        if (node.trait != null) {
            // Process trait name in type context
            Context traitContext = currentContext;
            setContext(Context.TYPE_CONTEXT);
            
            try {
                node.trait.accept(this);
            } finally {
                // Restore context
                setContext(traitContext);
            }
        }
        
        // Enter impl scope for associated items
        enterScope();
        
        try {
            // Process associated items
            if (node.items != null) {
                for (AssoItemNode item : node.items) {
                    item.accept(this);
                }
            }
        } finally {
            // Exit impl scope
            exitScope();
        }
    }
    
    // Process block expression
    @Override
    public void visit(BlockExprNode node) {
        // Enter new scope
        enterScope();

        try {
            // Process statements in block
            if (node.statements != null) {
                for (ASTNode stmt : node.statements) {
                    stmt.accept(this);
                }
            }
            
            // Process return value (if any) in value context
            if (node.returnValue != null) {
                Context valueContext = currentContext;
                setContext(Context.VALUE_CONTEXT);
                
                try {
                    node.returnValue.accept(this);
                } finally {
                    // Restore context
                    setContext(valueContext);
                }
            }
        } finally {
            // Exit scope
            exitScope();
        }
    }
    
    // Process path expression
    @Override
    public void visit(PathExprNode node) {
        // Process left segment in type context only if RSeg is not null
        Context previousContext = currentContext;
        if (node.RSeg != null) {
            setContext(Context.TYPE_CONTEXT);
        }
        
        try {
            node.LSeg.accept(this);
        } finally {
            // Restore context
            setContext(previousContext);
        }
        
        // If left segment has resolved symbol, set it to path expression node
        if (node.LSeg != null && node.LSeg.getSymbol() != null) {
            node.setSymbol(node.LSeg.getSymbol());
        }
    }
    
    // Process path expression segment
    @Override
    public void visit(PathExprSegNode node) {
        if (node.patternType == patternSeg_t.IDENT) {
            // Regular identifier - process in current context
            node.name.accept(this);
            // If identifier has resolved symbol, set it to path segment node
            if (node.name.getSymbol() != null) {
                node.setSymbol(node.name.getSymbol());
            }
        } else if (node.patternType == patternSeg_t.SELF) {
            // self - look up "self" symbol based on current context
            Symbol selfSymbol = null;
            
            switch (currentContext) {
                case TYPE_CONTEXT:
                    // In type context, throw error
                    throw new SemanticException(
                        String.format("'self' cannot be used in type context"),
                        node
                    );
                case VALUE_CONTEXT:
                    // In value context (or any other context), look up "self" in value namespace
                    selfSymbol = currentScope.lookupValue("self");
                    break;
                default:
                    // In other contexts, throw error
                    throw new SemanticException(
                        String.format("'self' cannot be used in this context"),
                        node
                    );
            }
            
            if (selfSymbol != null) {
                node.setSymbol(selfSymbol);
            }
        } else if (node.patternType == patternSeg_t.SELF_TYPE) {
            // Self - find the first ImplNode upwards and use its typeSymbol or constructor
            ASTNode current = node;
            Symbol implTypeSymbol = null;
            
            // Traverse up father nodes to find the first ImplNode
            while (current != null) {
                if (current instanceof ImplNode) {
                    ImplNode implNode = (ImplNode) current;
                    implTypeSymbol = implNode.getTypeSymbol();
                    break;
                }
                current = current.getFather();
            }
            
            // If we found an ImplNode with a typeSymbol, set it to the current node
            if (implTypeSymbol != null) {
                // Handle Self differently based on context
                switch (currentContext) {
                    case TYPE_CONTEXT:
                        // In type context, use the type symbol
                        node.setSymbol(implTypeSymbol);
                        break;
                        
                    case VALUE_CONTEXT:
                        // In value context, look for the constructor symbol
                        if (implTypeSymbol.getDeclaration() instanceof StructNode) {
                            // For structs, find the struct constructor symbol using the struct node
                            StructNode structNode = (StructNode) implTypeSymbol.getDeclaration();
                            Symbol constructorSymbol = structNode.getConstructorSymbol();
                            if (constructorSymbol != null) {
                                node.setSymbol(constructorSymbol);
                            } else {
                                throw new SemanticException(
                                    String.format("Constructor for struct '%s' not found",
                                                 implTypeSymbol.getName()),
                                    node
                                );
                            }
                        } else {
                            // If the declaration node is not a struct or enum, throw an error
                            throw new SemanticException(
                                String.format("'Self' does not refer to a valid constructor"),
                                node
                            );
                        }
                        break;
                        
                    default:
                        // Handle unsupported context for Self keyword
                        throw new SemanticException(
                            String.format("'Self' is not supported in %s context",
                                         currentContext.toString().toLowerCase()),
                            node
                        );
                }
            } else {
                throw new SemanticException(
                    String.format("'Self' used outside of an impl context"),
                    node
                );
            }
        }
    }
    
    // Process struct expression
    @Override
    public void visit(StructExprNode node) {
        // Set value context for struct name
        Context previousContext = currentContext;
        setContext(Context.VALUE_CONTEXT);
        
        try {
            // Process struct name (should resolve to a struct constructor)
            node.structName.accept(this);
            
            // Validate that the struct name resolves to a struct constructor
            Symbol structSymbol = node.structName.getSymbol();
            if (structSymbol == null) {
                throw new SemanticException(
                    String.format("Struct name '%s' could not be resolved",
                                 getSegmentText(node.structName)),
                    node
                );
            }
            
            // Verify it's a struct constructor
            if (structSymbol.getKind() != SymbolKind.STRUCT_CONSTRUCTOR) {
                throw new SemanticException(
                    String.format("'%s' is not a struct constructor",
                                 getSegmentText(node.structName)),
                    node
                );
            }
            
            // Get the struct definition from the constructor symbol
            Symbol structTypeSymbol = null;
            if (structSymbol.getDeclaration() instanceof StructNode) {
                StructNode structNode = (StructNode) structSymbol.getDeclaration();
                structTypeSymbol = structNode.getSymbol();
            }
            
            if (structTypeSymbol == null) {
                throw new SemanticException(
                    String.format("Could not find struct definition for '%s'",
                                 getSegmentText(node.structName)),
                    node
                );
            }
            
            // Set the struct expression's symbol to the struct constructor symbol
            node.setSymbol(structSymbol);
            
            // Set current type name for field processing
            String previousTypeName = currentTypeName;
            currentTypeName = structTypeSymbol.getName();
            
            // Process field values
            if (node.fieldValues != null) {
                for (FieldValNode fieldVal : node.fieldValues) {
                    fieldVal.accept(this);
                }
            }
            
            // Restore current type name
            currentTypeName = previousTypeName;
        } finally {
            // Restore context
            setContext(previousContext);
        }
    }
    
    // Process field value in struct expression
    @Override
    public void visit(FieldValNode node) {
        // Set field context for field name
        Context previousContext = currentContext;
        setContext(Context.FIELD_CONTEXT);
        
        try {
            // Process field name
            node.fieldName.accept(this);
            
            // Set the FieldValNode's symbol to the field name's symbol
            if (node.fieldName.getSymbol() != null) {
                node.setSymbol(node.fieldName.getSymbol());
            }
        } finally {
            // Restore context for value expression
            setContext(previousContext);
        }
        
        // Process value expression in value context
        setContext(Context.VALUE_CONTEXT);
        
        try {
            // Process value expression
            node.value.accept(this);
        } finally {
            // Restore original context
            setContext(previousContext);
        }
    }
    
    // Process type array expression
    @Override
    public void visit(TypeArrayExprNode node) {
        // Process element type in type context
        Context elementTypeContext = currentContext;
        setContext(Context.TYPE_CONTEXT);
        
        try {
            node.elementType.accept(this);
        } finally {
            // Restore context
            setContext(elementTypeContext);
        }
        
        // Process size expression in value context
        if (node.size != null) {
            Context valueContext = currentContext;
            setContext(Context.VALUE_CONTEXT);
            
            try {
                node.size.accept(this);
            } finally {
                // Restore context
                setContext(valueContext);
            }
        }
    }
    
    // Process type cast expression
    @Override
    public void visit(TypeCastExprNode node) {
        // Process the expression in the current context
        if (node.expr != null) {
            node.expr.accept(this);
        }
        
        // Process the type in TYPE_CONTEXT
        if (node.type != null) {
            Context previousContext = currentContext;
            setContext(Context.TYPE_CONTEXT);
            
            try {
                node.type.accept(this);
            } finally {
                // Restore context
                setContext(previousContext);
            }
        }
    }
    
    // Process field access expression
    @Override
    public void visit(FieldExprNode node) {
        // Process the receiver first
        if (node.receiver == null) {
            throw new SemanticException(
                "Field expression receiver should not be null",
                node
            );
        } else {
            node.receiver.accept(this);
        }
    }
    
    // Process method call expression
    @Override
    public void visit(MethodCallExprNode node) {
        // According to grammar: <methodcallexpr> ::= <expression> . <pathseg> ( <arguments>? )
        // receiver and methodName are required, arguments are optional
        
        // Process receiver in value context
        if (node.receiver == null) {
            throw new SemanticException(
                "Method call receiver should not be null",
                node
            );
        } else {
            Context previousContext = currentContext;
            setContext(Context.VALUE_CONTEXT);
            
            try {
                node.receiver.accept(this);
            } finally {
                // Restore context
                setContext(previousContext);
            }
        }
        
        // NOTE: We do NOT recursively visit methodName as requested
        // This prevents the analyzer from entering the method name
        
        // Process arguments in value context
        if (node.arguments != null) {
            Context previousContext = currentContext;
            setContext(Context.VALUE_CONTEXT);
            
            try {
                for (ExprNode arg : node.arguments) {
                    arg.accept(this);
                }
            } finally {
                // Restore context
                setContext(previousContext);
            }
        }
    }
}