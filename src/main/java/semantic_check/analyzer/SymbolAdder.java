// SymbolAdder class - First phase of namespace analysis
// This class is responsible for adding all symbols to the symbol table
// without performing any symbol resolution or checking

import java.util.Vector;

public class SymbolAdder extends VisitorBase {
    // Global scope
    private NamespaceSymbolTable globalScope;
    
    // Current scope
    private NamespaceSymbolTable currentScope;
    
    // Current type name (for field processing)
    private String currentTypeName;
    
    // Constructor
    public SymbolAdder() {
        // No initialization needed
    }
    
    // Initialize global scope
    public void initializeGlobalScope() {
        globalScope = new NamespaceSymbolTable(0);
        currentScope = globalScope;
        
        // Add builtin types
        addBuiltinTypes();
        
        // Add builtin functions
        addBuiltinFunctions();
        
        // Add builtin methods
        addBuiltinMethods();
    }
    
    // Get global scope
    public NamespaceSymbolTable getGlobalScope() {
        return globalScope;
    }
    
    // Add builtin types
    private void addBuiltinTypes() {
        // Add basic types to type namespace
        addBuiltinType("i32");
        addBuiltinType("u32");
        addBuiltinType("usize");
        addBuiltinType("isize");
        addBuiltinType("bool");
        addBuiltinType("char");
        addBuiltinType("str");  // String slice type
    }
    
    private void addBuiltinType(String typeName) {
        Symbol builtinType = new Symbol(
            typeName,
            SymbolKind.BUILTIN_TYPE,
            null, // Builtin types have no declaration node
            0, // Global scope
            false // Immutable
        );
        globalScope.addTypeSymbol(builtinType);
    }
    
    // Add builtin functions
    private void addBuiltinFunctions() {
        // I/O functions
        addBuiltinFunction("print");
        addBuiltinFunction("println");
        addBuiltinFunction("printInt");
        addBuiltinFunction("printlnInt");
        addBuiltinFunction("getString");
        addBuiltinFunction("getInt");
        addBuiltinFunction("exit");
    }
    
    // Add builtin methods
    private void addBuiltinMethods() {
        // We don't add builtin methods here to avoid errors
        // Instead, we handle them directly in TypeChecker
    }
    
    private void addBuiltinFunction(String functionName) {
        // Create a BuiltinFunctionNode to represent the builtin function
        BuiltinFunctionNode builtinNode = new BuiltinFunctionNode(functionName);
        
        // Set the symbol for the builtin function node
        Symbol builtinFunction = new Symbol(
            functionName,
            SymbolKind.FUNCTION,
            builtinNode, // Use BuiltinFunctionNode as declaration node
            0, // Global scope
            false // Immutable
        );
        
        // Set the symbol in the builtin function node
        builtinNode.setSymbol(builtinFunction);
        
        // Add to value namespace
        globalScope.addValueSymbol(builtinFunction);
    }
    
    // Enter new scope
    private void enterScope() {
        currentScope = currentScope.enterScope();
    }
    
    // Exit current scope
    private void exitScope() {
        currentScope = currentScope.exitScope();
        if (currentScope == null) {
            throw new RuntimeException("Attempting to exit global scope");
        }
    }
    
    // Process function declaration
    @Override
    public void visit(FunctionNode node) {
        // Add function symbol to current scope
        Symbol functionSymbol = new Symbol(
            node.name.name,
            SymbolKind.FUNCTION,
            node,
            currentScope.getScopeLevel(),
            false
        );
        
        // Add to value namespace
        currentScope.addValueSymbol(functionSymbol);
        
        // Set the function node's symbol
        node.setSymbol(functionSymbol);
        
        // Set the identifier's symbol
        node.name.setSymbol(functionSymbol);
        
        // Visit the return type field if it exists
        if (node.returnType != null) {
            node.returnType.accept(this);
        }
        
        // Enter function scope for parameters and body
        enterScope();
        
        try {
            // Process self parameter (if any)
            if (node.selfPara != null) {
                node.selfPara.accept(this);
            }
            
            // Process parameters
            if (node.parameters != null) {
                for (ParameterNode param : node.parameters) {
                    param.accept(this);
                }
            }
            
            // Process function body (if any)
            if (node.body != null) {
                node.body.accept(this);
            }
        } finally {
            // Exit function scope
            exitScope();
        }
    }
    
    // Process self parameter
    @Override
    public void visit(SelfParaNode node) {
        // Create a special symbol for self
        Symbol selfSymbol = new Symbol(
            "self",
            SymbolKind.SELF_CONSTRUCTOR, // Treat self as a self constructor
            node,
            currentScope.getScopeLevel(),
            node.isMutable // Use the mutability from the SelfParaNode
        );
        
        // Add to value namespace
        currentScope.addValueSymbol(selfSymbol);
        
        // Set the self parameter's symbol
        node.setSymbol(selfSymbol);
        
        // Visit the type field if it exists
        if (node.type != null) {
            node.type.accept(this);
        }
    }
    
    // Process struct declaration
    @Override
    public void visit(StructNode node) {
        // Add struct symbol to current scope
        Symbol structSymbol = new Symbol(
            node.name.name,
            SymbolKind.STRUCT,
            node,
            currentScope.getScopeLevel(),
            false
        );
        
        // Add to type namespace
        currentScope.addTypeSymbol(structSymbol);
        
        // Set the struct node's symbol
        node.setSymbol(structSymbol);
        
        // Set the identifier's symbol
        node.name.setSymbol(structSymbol);
        
        // Save current type name
        String previousTypeName = currentTypeName;
        currentTypeName = node.name.name;
        
        try {
            // Process fields
            if (node.fields != null) {
                for (FieldNode field : node.fields) {
                    field.accept(this);
                }
            }
        } finally {
            // Restore current type name
            currentTypeName = previousTypeName;
        }
        
        // Create struct constructor symbol
        Symbol constructorSymbol = new Symbol(
            node.name.name,
            SymbolKind.STRUCT_CONSTRUCTOR,
            node,
            currentScope.getScopeLevel(),
            false
        );
        
        // Set the constructor symbol in the StructNode
        node.setConstructorSymbol(constructorSymbol);
        
        // Add to value namespace
        currentScope.addValueSymbol(constructorSymbol);
    }
    
    // Process field declaration
    @Override
    public void visit(FieldNode node) {
        // Create field symbol
        Symbol fieldSymbol = new Symbol(
            node.name.name,
            SymbolKind.FIELD,
            node,
            currentScope.getScopeLevel(),
            true, // Fields are mutable by default (accessed through struct instance)
            currentTypeName
        );
        
        // Add to field namespace
        currentScope.addFieldSymbol(currentTypeName, fieldSymbol);
        
        // Set the field node's symbol
        node.setSymbol(fieldSymbol);
        
        // Set the identifier's symbol
        node.name.setSymbol(fieldSymbol);
        
        // Visit the type field
        if (node.type != null) {
            node.type.accept(this);
        }
    }
    
    // Process enum declaration
    @Override
    public void visit(EnumNode node) {
        // Add enum symbol to current scope
        Symbol enumSymbol = new Symbol(
            node.name.name,
            SymbolKind.ENUM,
            node,
            currentScope.getScopeLevel(),
            false
        );
        
        // Add to type namespace
        currentScope.addTypeSymbol(enumSymbol);
        
        // Set the enum node's symbol
        node.setSymbol(enumSymbol);
        
        // Set the identifier's symbol
        node.name.setSymbol(enumSymbol);
        
        // Enter enum scope for variants
        enterScope();
        
        try {
            // Process enum variants
            if (node.variants != null) {
                for (IdentifierNode variant : node.variants) {
                    // Create enum variant constructor symbol with just the variant name
                    Symbol variantConstructorSymbol = new Symbol(
                        variant.name, // Use only the variant name
                        SymbolKind.ENUM_VARIANT_CONSTRUCTOR,
                        variant, // Point to the variant node instead of the enum node
                        currentScope.getScopeLevel(),
                        false
                    );

                    // Add to value namespace
                    currentScope.addValueSymbol(variantConstructorSymbol);
                    
                    // Set the variant's symbol to point to the constructor symbol
                    variant.setSymbol(variantConstructorSymbol);
                }
            }
        } finally {
            // Exit enum scope
            exitScope();
        }
    }
    
    // Process constant declaration
    @Override
    public void visit(ConstItemNode node) {
        // Add const symbol to current scope
        Symbol constSymbol = new Symbol(
            node.name.name,
            SymbolKind.CONSTANT,
            node,
            currentScope.getScopeLevel(),
            false // Constants are immutable
        );
        
        // Add to value namespace
        currentScope.addValueSymbol(constSymbol);
        
        // Set the const node's symbol
        node.setSymbol(constSymbol);
        
        // Set the identifier's symbol
        node.name.setSymbol(constSymbol);
        
        // Visit the type field
        if (node.type != null) {
            node.type.accept(this);
        }
        
        // Visit the value field if it exists
        if (node.value != null) {
            node.value.accept(this);
        }
    }
    
    // Process trait declaration
    @Override
    public void visit(TraitNode node) {
        // Add trait symbol to current scope
        Symbol traitSymbol = new Symbol(
            node.name.name,
            SymbolKind.TRAIT,
            node,
            currentScope.getScopeLevel(),
            false
        );
        
        // Add to type namespace
        currentScope.addTypeSymbol(traitSymbol);
        
        // Set the trait node's symbol
        node.setSymbol(traitSymbol);
        
        // Set the identifier's symbol
        node.name.setSymbol(traitSymbol);
        
        // Enter trait scope for associated items
        enterScope();
        
        try {
            // Process associated items in trait
            if (node.items != null) {
                for (AssoItemNode item : node.items) {
                    item.accept(this);
                }
            }
        } finally {
            // Exit trait scope
            exitScope();
        }
    }
    
    // Process impl declaration
    @Override
    public void visit(ImplNode node) {
        // Visit the typeName field first
        if (node.typeName != null) {
            node.typeName.accept(this);
        }
        
        // Extract type symbol directly from the type path
        Symbol typeSymbol = null;
        
        // Only TypePathExprNode can be used as impl target
        if (node.typeName instanceof TypePathExprNode) {
            TypePathExprNode typePath = (TypePathExprNode) node.typeName;
            if (typePath.path != null && typePath.path.patternType == patternSeg_t.IDENT) {
                // Look up the type symbol in the current scope
                typeSymbol = currentScope.lookupType(typePath.path.name.name);
            }
        }
        
        // Check if type symbol is valid
        if (typeSymbol == null) {
            throw new SemanticException(
                String.format("Invalid type name in impl declaration"),
                node
            );
        }
        
        // Set the type symbol in the ImplNode
        node.setTypeSymbol(typeSymbol);
        
        // Process trait name (if any)
        Symbol traitSymbol = null;
        if (node.trait != null) {
            // Visit the trait field
            node.trait.accept(this);
            
            // Look up the trait symbol in the current scope
            traitSymbol = currentScope.lookupType(node.trait.name);
            if (traitSymbol == null) {
                throw new SemanticException(
                    String.format("Trait '%s' not found for impl declaration",
                                 node.trait.name),
                    node
                );
            }
            
            // Set the trait symbol in the ImplNode
            node.setTraitSymbol(traitSymbol);
            
            // Set the trait identifier's symbol
            node.trait.setSymbol(traitSymbol);
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
            // After processing all items, add all symbols in current scope to type's implSymbols
            if (typeSymbol != null) {
                for (Symbol symbol : currentScope.getValueNamespace().values()) {
                    // Only add value namespace symbols (functions, constants) to implSymbols
                    if (symbol.getKind() == SymbolKind.FUNCTION ||
                        symbol.getKind() == SymbolKind.CONSTANT) {
                        typeSymbol.addImplSymbol(symbol);
                    }
                }
            }
            
            // Exit impl scope
            exitScope();
        }
    }
    
    // Process associated item
    @Override
    public void visit(AssoItemNode node) {
        // Either function or constant should be not null
        if (node.function != null) {
            node.function.accept(this);
        } else if (node.constant != null) {
            node.constant.accept(this);
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
            
            // Process return value (if any)
            if (node.returnValue != null) {
                node.returnValue.accept(this);
            }
        } finally {
            // Exit scope
            exitScope();
        }
    }
}