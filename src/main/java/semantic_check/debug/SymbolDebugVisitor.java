import java.util.HashMap;
import java.util.Map;

/**
 * SymbolDebugVisitor is a visitor that traverses the AST and outputs the symbol
 * associated with each node (if any). This is useful for debugging namespace
 * semantic checking and understanding how symbols are resolved.
 */
public class SymbolDebugVisitor extends VisitorBase {
    // Map to store node type to symbol information
    private Map<String, Integer> nodeCounts = new HashMap<>();
    private int totalNodes = 0;
    private int nodesWithSymbols = 0;
    
    /**
     * Visit a generic AST node and print its symbol information
     */
    @Override
    public void visit(ASTNode node) {
        printNodeSymbol(node);
        super.visit(node);
    }
    
    /**
     * Visit IdentifierNode and print its symbol information
     */
    @Override
    public void visit(IdentifierNode node) {
        printNodeSymbol(node);
        super.visit(node);
    }
    
    /**
     * Visit PathExprNode and print its symbol information
     */
    @Override
    public void visit(PathExprNode node) {
        printNodeSymbol(node);
        super.visit(node);
    }
    
    /**
     * Visit PathExprSegNode and print its symbol information
     */
    @Override
    public void visit(PathExprSegNode node) {
        printNodeSymbol(node);
        super.visit(node);
    }
    
    /**
     * Visit StructExprNode and print its symbol information
     */
    @Override
    public void visit(StructExprNode node) {
        printNodeSymbol(node);
        super.visit(node);
    }
    
    /**
     * Visit FieldValNode and print its symbol information
     */
    @Override
    public void visit(FieldValNode node) {
        printNodeSymbol(node);
        super.visit(node);
    }
    
    /**
     * Visit FunctionNode and print its symbol information
     */
    @Override
    public void visit(FunctionNode node) {
        printNodeSymbol(node);
        super.visit(node);
    }
    
    /**
     * Visit StructNode and print its symbol information
     */
    @Override
    public void visit(StructNode node) {
        printNodeSymbol(node);
        super.visit(node);
    }
    
    /**
     * Visit EnumNode and print its symbol information
     */
    @Override
    public void visit(EnumNode node) {
        printNodeSymbol(node);
        super.visit(node);
    }
    
    /**
     * Visit TraitNode and print its symbol information
     */
    @Override
    public void visit(TraitNode node) {
        printNodeSymbol(node);
        super.visit(node);
    }
    
    /**
     * Visit ConstItemNode and print its symbol information
     */
    @Override
    public void visit(ConstItemNode node) {
        printNodeSymbol(node);
        super.visit(node);
    }
    
    /**
     * Visit ImplNode and print its symbol information
     */
    @Override
    public void visit(ImplNode node) {
        printNodeSymbol(node);
        super.visit(node);
    }
    
    /**
     * Visit SelfParaNode and print its symbol information
     */
    @Override
    public void visit(SelfParaNode node) {
        printNodeSymbol(node);
        super.visit(node);
    }
    
    /**
     * Visit FieldNode and print its symbol information
     */
    @Override
    public void visit(FieldNode node) {
        printNodeSymbol(node);
        super.visit(node);
    }
    
    
    /**
     * Print symbol information for a given node
     */
    private void printNodeSymbol(ASTNode node) {
        totalNodes++;
        
        // Count node types
        String nodeType = node.getClass().getSimpleName();
        nodeCounts.put(nodeType, nodeCounts.getOrDefault(nodeType, 0) + 1);
        
        // Get symbol information
        Symbol symbol = null;
        String nodeName = "";
        
        if (node instanceof IdentifierNode) {
            IdentifierNode idNode = (IdentifierNode) node;
            symbol = idNode.getSymbol();
            nodeName = idNode.name;
        } else if (node instanceof PathExprNode) {
            PathExprNode pathNode = (PathExprNode) node;
            symbol = pathNode.getSymbol();
            nodeName = "PathExpr";
        } else if (node instanceof PathExprSegNode) {
            PathExprSegNode segNode = (PathExprSegNode) node;
            symbol = segNode.getSymbol();
            nodeName = "PathExprSeg";
            if (segNode.name != null) {
                nodeName += "(" + segNode.name.name + ")";
            } else if (segNode.patternType != null) {
                nodeName += "(" + segNode.patternType + ")";
            }
        } else if (node instanceof StructExprNode) {
            StructExprNode structNode = (StructExprNode) node;
            symbol = structNode.getSymbol();
            nodeName = "StructExpr";
        } else if (node instanceof FieldValNode) {
            FieldValNode fieldValNode = (FieldValNode) node;
            symbol = fieldValNode.getSymbol();
            nodeName = "FieldVal";
            if (fieldValNode.fieldName != null) {
                nodeName += "(" + fieldValNode.fieldName.name + ")";
            }
        } else if (node instanceof FunctionNode) {
            FunctionNode funcNode = (FunctionNode) node;
            symbol = funcNode.getSymbol();
            nodeName = "Function";
            if (funcNode.name != null) {
                nodeName += "(" + funcNode.name.name + ")";
            }
        } else if (node instanceof StructNode) {
            StructNode structNode = (StructNode) node;
            symbol = structNode.getSymbol();
            nodeName = "Struct";
            if (structNode.name != null) {
                nodeName += "(" + structNode.name.name + ")";
            }
        } else if (node instanceof EnumNode) {
            EnumNode enumNode = (EnumNode) node;
            symbol = enumNode.getSymbol();
            nodeName = "Enum";
            if (enumNode.name != null) {
                nodeName += "(" + enumNode.name.name + ")";
            }
        } else if (node instanceof TraitNode) {
            TraitNode traitNode = (TraitNode) node;
            symbol = traitNode.getSymbol();
            nodeName = "Trait";
            if (traitNode.name != null) {
                nodeName += "(" + traitNode.name.name + ")";
            }
        } else if (node instanceof ConstItemNode) {
            ConstItemNode constNode = (ConstItemNode) node;
            symbol = constNode.getSymbol();
            nodeName = "Const";
            if (constNode.name != null) {
                nodeName += "(" + constNode.name.name + ")";
            }
        } else if (node instanceof ImplNode) {
            ImplNode implNode = (ImplNode) node;
            // ImplNode doesn't have a direct symbol, but has typeSymbol and traitSymbol
            Symbol typeSymbol = implNode.getTypeSymbol();
            Symbol traitSymbol = implNode.getTraitSymbol();
            nodeName = "Impl";
            if (implNode.typeName != null) {
                nodeName += "(for " + implNode.typeName.toString() + ")";
            }
            if (implNode.trait != null) {
                nodeName += "(trait " + implNode.trait.name + ")";
            }
            
            System.out.println(String.format("Node: %-20s | Type: %-25s | TypeSymbol: %-30s | TraitSymbol: %-30s", 
                nodeName, 
                nodeType,
                typeSymbol != null ? typeSymbol.toString() : "null",
                traitSymbol != null ? traitSymbol.toString() : "null"));
            return;
        } else if (node instanceof SelfParaNode) {
            SelfParaNode selfNode = (SelfParaNode) node;
            symbol = selfNode.getSymbol();
            nodeName = "SelfPara";
        } else if (node instanceof FieldNode) {
            FieldNode fieldNode = (FieldNode) node;
            symbol = fieldNode.getSymbol();
            nodeName = "Field";
            if (fieldNode.name != null) {
                nodeName += "(" + fieldNode.name.name + ")";
            }
        }
        
        if (symbol != null) {
            nodesWithSymbols++;
            System.out.println(String.format("Node: %-20s | Type: %-25s | Symbol: %s", 
                nodeName, 
                nodeType,
                symbol.toString()));
        } else {
            System.out.println(String.format("Node: %-20s | Type: %-25s | Symbol: null", 
                nodeName, 
                nodeType));
        }
    }
    
    /**
     * Print summary statistics
     */
    public void printSummary() {
        System.out.println("\n=== Symbol Debug Summary ===");
        System.out.println("Total nodes visited: " + totalNodes);
        System.out.println("Nodes with symbols: " + nodesWithSymbols);
        System.out.println("Nodes without symbols: " + (totalNodes - nodesWithSymbols));
        System.out.println("\nNode type distribution:");
        for (Map.Entry<String, Integer> entry : nodeCounts.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
    }
    
    /**
     * Reset the visitor state
     */
    public void reset() {
        nodeCounts.clear();
        totalNodes = 0;
        nodesWithSymbols = 0;
    }
}