// Unified Namespace Analyzer class combining namespace checking and AST visiting functionality
// This class now uses a two-phase approach: SymbolAdder then SymbolChecker

import java.util.List;
import java.util.Vector;

public class NamespaceAnalyzer extends VisitorBase {
    // Global scope
    private NamespaceSymbolTable globalScope;
    
    // Symbol adder for first phase
    private SymbolAdder symbolAdder;
    
    // Symbol checker for second phase
    private SymbolChecker symbolChecker;
    
    // Constructor
    public NamespaceAnalyzer() {
        // Initialize components
        this.symbolAdder = new SymbolAdder();
        this.symbolChecker = null; // Will be initialized after symbol addition
    }
    
    // Initialize global scope
    public void initializeGlobalScope() {
        // Initialize global scope through symbol adder
        symbolAdder.initializeGlobalScope();
        globalScope = symbolAdder.getGlobalScope();
        
        // Initialize symbol checker with global scope
        symbolChecker = new SymbolChecker(globalScope);
    }
    
    // Get global scope
    public NamespaceSymbolTable getGlobalScope() {
        return globalScope;
    }
    
    // Main analysis method - performs two-phase analysis
    public void analyze(Vector<StmtNode> stmts) {
        if (symbolAdder == null || symbolChecker == null) {
            throw new RuntimeException("NamespaceAnalyzer not properly initialized");
        }
        
        // Phase 1: Add all symbols to symbol table
        for (StmtNode stmt : stmts) {
            stmt.accept(symbolAdder);
        }
        
        // Phase 2: Check all symbol references
        for (StmtNode stmt : stmts) {
            stmt.accept(symbolChecker);
        }
    }
    
    // Get segment text (utility method)
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
    
    // All visit methods are now delegated to either SymbolAdder or SymbolChecker
    // This class only serves as a coordinator for the two-phase analysis
    
    // Process let statement - delegate to checker for symbol addition
    @Override
    public void visit(LetStmtNode node) {
        // Phase 2: Check references and add symbols
        if (node.value != null) {
            node.value.accept(symbolChecker);
        }
        
        if (node.type != null) {
            node.type.accept(symbolChecker);
        }
        
        // Process pattern and add symbol in phase 2
        if (node.name != null) {
            node.name.accept(symbolChecker);
        }
    }
    
    // Process expression statement - delegate to checker
    @Override
    public void visit(ExprStmtNode node) {
        // Only check references in phase 2
        node.expr.accept(symbolChecker);
    }
    
    // Process function declaration - delegate to both phases
    @Override
    public void visit(FunctionNode node) {
        // Phase 1: Add function symbol only
        node.name.accept(symbolAdder);
        
        if (node.selfPara != null) {
            node.selfPara.accept(symbolAdder);
        }
        
        // Phase 2: Check references and add parameter symbols
        node.accept(symbolChecker);
    }
    
    // Process self parameter - delegate to both phases
    @Override
    public void visit(SelfParaNode node) {
        // Phase 1: Add symbol
        // Self symbol is added in SymbolAdder
        
        // Phase 2: Check type reference
        if (node.type != null) {
            node.type.accept(symbolChecker);
        }
    }
    
    // Process parameter - delegate to checker only
    @Override
    public void visit(ParameterNode node) {
        // Phase 2: Check type reference and add symbol
        node.accept(symbolChecker);
    }
    
    // Process struct declaration - delegate to both phases
    @Override
    public void visit(StructNode node) {
        // Phase 1: Add symbols
        node.name.accept(symbolAdder);
        
        if (node.fields != null) {
            for (FieldNode field : node.fields) {
                field.accept(symbolAdder);
            }
        }
        
        // Phase 2: Check field type references
        if (node.fields != null) {
            for (FieldNode field : node.fields) {
                if (field.type != null) {
                    field.type.accept(symbolChecker);
                }
            }
        }
    }
    
    // Process field declaration - delegate to both phases
    @Override
    public void visit(FieldNode node) {
        // Phase 1: Add symbol
        node.name.accept(symbolAdder);
        
        // Phase 2: Check type reference
        if (node.type != null) {
            node.type.accept(symbolChecker);
        }
    }
    
    // Process enum declaration - delegate to both phases
    @Override
    public void visit(EnumNode node) {
        // Phase 1: Add symbols
        node.name.accept(symbolAdder);
        
        if (node.variants != null) {
            for (IdentifierNode variant : node.variants) {
                variant.accept(symbolAdder);
            }
        }
    }
    
    // Process constant declaration - delegate to both phases
    @Override
    public void visit(ConstItemNode node) {
        // Phase 1: Add symbol
        node.name.accept(symbolAdder);
        
        // Phase 2: Check references
        if (node.type != null) {
            node.type.accept(symbolChecker);
        }
        
        if (node.value != null) {
            node.value.accept(symbolChecker);
        }
    }
    
    // Process trait declaration - delegate to both phases
    @Override
    public void visit(TraitNode node) {
        // Phase 1: Add symbol
        node.name.accept(symbolAdder);
        
        if (node.items != null) {
            for (AssoItemNode item : node.items) {
                item.accept(symbolAdder);
            }
        }
    }
    
    // Process impl declaration - delegate to both phases
    @Override
    public void visit(ImplNode node) {
        // Phase 1: Add symbols
        if (node.items != null) {
            for (AssoItemNode item : node.items) {
                item.accept(symbolAdder);
            }
        }
        
        // Phase 2: Check references
        node.typeName.accept(symbolChecker);
        
        if (node.trait != null) {
            node.trait.accept(symbolChecker);
        }
        
        if (node.items != null) {
            for (AssoItemNode item : node.items) {
                if (item.function != null) {
                    item.function.accept(symbolChecker);
                }
                if (item.constant != null) {
                    item.constant.accept(symbolChecker);
                }
            }
        }
    }
    
    // Process associated item - delegate to both phases
    @Override
    public void visit(AssoItemNode node) {
        // Phase 1: Add symbols
        if (node.function != null) {
            node.function.accept(symbolAdder);
        }
        
        if (node.constant != null) {
            node.constant.accept(symbolAdder);
        }
    }
    
    // Process block expression - delegate to checker
    @Override
    public void visit(BlockExprNode node) {
        // Only check references in phase 2
        if (node.statements != null) {
            for (StmtNode stmt : node.statements) {
                stmt.accept(symbolChecker);
            }
        }
        
        if (node.returnValue != null) {
            node.returnValue.accept(symbolChecker);
        }
    }
    
    // Process path expression - delegate to checker
    @Override
    public void visit(PathExprNode node) {
        // Only check references in phase 2
        node.LSeg.accept(symbolChecker);
        
        if (node.LSeg != null && node.LSeg.getSymbol() != null) {
            node.setSymbol(node.LSeg.getSymbol());
        }
    }
    
    // Process path expression segment - delegate to checker
    @Override
    public void visit(PathExprSegNode node) {
        // Only check references in phase 2
        if (node.patternType == patternSeg_t.IDENT) {
            node.name.accept(symbolChecker);
            if (node.name.getSymbol() != null) {
                node.setSymbol(node.name.getSymbol());
            }
        } else if (node.patternType == patternSeg_t.SELF || node.patternType == patternSeg_t.SELF_TYPE) {
            // Self and Self handling is done in SymbolChecker
            node.accept(symbolChecker);
        }
    }
    
    // Process struct expression - delegate to checker
    @Override
    public void visit(StructExprNode node) {
        // Only check references in phase 2
        node.structName.accept(symbolChecker);
        
        if (node.structName.getSymbol() != null) {
            node.setSymbol(node.structName.getSymbol());
        }
        
        if (node.fieldValues != null) {
            for (FieldValNode fieldVal : node.fieldValues) {
                fieldVal.accept(symbolChecker);
            }
        }
    }
    
    // Process field value in struct expression - delegate to checker
    @Override
    public void visit(FieldValNode node) {
        // Only check references in phase 2
        node.fieldName.accept(symbolChecker);
        
        if (node.fieldName.getSymbol() != null) {
            node.setSymbol(node.fieldName.getSymbol());
        }
        
        node.value.accept(symbolChecker);
    }
    
    // Process type array expression - delegate to checker
    @Override
    public void visit(TypeArrayExprNode node) {
        // Only check references in phase 2
        node.elementType.accept(symbolChecker);
        
        if (node.size != null) {
            node.size.accept(symbolChecker);
        }
    }
    
    // Process type cast expression - delegate to checker
    @Override
    public void visit(TypeCastExprNode node) {
        // Only check references in phase 2
        if (node.expr != null) {
            node.expr.accept(symbolChecker);
        }
        
        if (node.type != null) {
            node.type.accept(symbolChecker);
        }
    }
    
    // Process field access expression - delegate to checker
    @Override
    public void visit(FieldExprNode node) {
        // Only check references in phase 2
        if (node.receiver == null) {
            throw new SemanticException(
                "Field expression receiver should not be null",
                node
            );
        } else {
            node.receiver.accept(symbolChecker);
        }
    }
    
    // Process method call expression - delegate to checker
    @Override
    public void visit(MethodCallExprNode node) {
        // Only check references in phase 2
        if (node.receiver == null) {
            throw new SemanticException(
                "Method call receiver should not be null",
                node
            );
        } else {
            node.receiver.accept(symbolChecker);
        }
        
        // NOTE: We do NOT recursively visit methodName as requested
        // This prevents the analyzer from entering the method name
        
        // Process arguments in value context
        if (node.arguments != null) {
            for (ExprNode arg : node.arguments) {
                arg.accept(symbolChecker);
            }
        }
    }
}