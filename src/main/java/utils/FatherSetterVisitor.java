// FatherSetterVisitor is a visitor that sets the father field for all AST nodes
// It traverses the AST and sets the father field of each child node to its parent

public class FatherSetterVisitor extends VisitorBase {
    // Current parent node
    private ASTNode currentParent;
    
    // Helper method to set father and visit child
    private void setFatherAndVisit(ASTNode child) {
        if (child != null) {
            child.setFather(currentParent);
            ASTNode previousParent = currentParent;
            child.accept(this);
            currentParent = previousParent;
        }
    }
    
    // Visit a generic AST node
    public void visit(ASTNode node) {
        // Base implementation - no children to traverse
    }
    
    
    public void visit(ItemNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(LetStmtNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        setFatherAndVisit(node.name);
        setFatherAndVisit(node.type);
        setFatherAndVisit(node.value);
        
        currentParent = previousParent;
    }
    
    public void visit(ExprStmtNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        setFatherAndVisit(node.expr);
        
        currentParent = previousParent;
    }
    
    public void visit(FunctionNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        setFatherAndVisit(node.name);
        setFatherAndVisit(node.selfPara);
        
        if (node.parameters != null) {
            for (int i = 0; i < node.parameters.size(); i++) {
                setFatherAndVisit(node.parameters.get(i));
            }
        }
        
        setFatherAndVisit(node.returnType);
        setFatherAndVisit(node.body);
        
        currentParent = previousParent;
    }
    
    public void visit(SelfParaNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        setFatherAndVisit(node.type);
        
        currentParent = previousParent;
    }
    
    public void visit(ParameterNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        setFatherAndVisit(node.name);
        setFatherAndVisit(node.type);
        
        currentParent = previousParent;
    }
    
    public void visit(PatternNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(IdPatNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        setFatherAndVisit(node.name);
        
        currentParent = previousParent;
    }
    
    public void visit(WildPatNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(RefPatNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        setFatherAndVisit(node.innerPattern);
        
        currentParent = previousParent;
    }
    
    public void visit(StructNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        setFatherAndVisit(node.name);
        
        if (node.fields != null) {
            for (int i = 0; i < node.fields.size(); i++) {
                setFatherAndVisit(node.fields.get(i));
            }
        }
        
        currentParent = previousParent;
    }
    
    public void visit(FieldNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        setFatherAndVisit(node.name);
        setFatherAndVisit(node.type);
        
        currentParent = previousParent;
    }
    
    public void visit(EnumNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        setFatherAndVisit(node.name);
        
        if (node.variants != null) {
            for (int i = 0; i < node.variants.size(); i++) {
                setFatherAndVisit(node.variants.get(i));
            }
        }
        
        currentParent = previousParent;
    }
    
    public void visit(ConstItemNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        setFatherAndVisit(node.name);
        setFatherAndVisit(node.type);
        setFatherAndVisit(node.value);
        
        currentParent = previousParent;
    }
    
    public void visit(TraitNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.name != null) {
            node.name.setFather(node);
            node.name.accept(this);
        }
        
        if (node.items != null) {
            for (int i = 0; i < node.items.size(); i++) {
                node.items.get(i).setFather(node);
                node.items.get(i).accept(this);
            }
        }
        
        currentParent = previousParent;
    }
    
    public void visit(ImplNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.typeName != null) {
            node.typeName.setFather(node);
            node.typeName.accept(this);
        }
        
        if (node.trait != null) {
            node.trait.setFather(node);
            node.trait.accept(this);
        }
        
        if (node.items != null) {
            for (int i = 0; i < node.items.size(); i++) {
                node.items.get(i).setFather(node);
                node.items.get(i).accept(this);
            }
        }
        
        currentParent = previousParent;
    }
    
    public void visit(AssoItemNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.function != null) {
            node.function.setFather(node);
            node.function.accept(this);
        } else if (node.constant != null) {
            node.constant.setFather(node);
            node.constant.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(ExprNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(ExprWithBlockNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(ExprWithoutBlockNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.expr != null) {
            node.expr.setFather(node);
            node.expr.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(BlockExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.statements != null) {
            for (int i = 0; i < node.statements.size(); i++) {
                node.statements.get(i).setFather(node);
                node.statements.get(i).accept(this);
            }
        }
        
        if (node.returnValue != null) {
            node.returnValue.setFather(node);
            node.returnValue.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(IfExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.condition != null) {
            node.condition.setFather(node);
            node.condition.accept(this);
        }
        
        if (node.thenBranch != null) {
            node.thenBranch.setFather(node);
            node.thenBranch.accept(this);
        }
        
        if (node.elseBranch != null) {
            node.elseBranch.setFather(node);
            node.elseBranch.accept(this);
        }
        if (node.elseifBranch != null) {
            node.elseifBranch.setFather(node);
            node.elseifBranch.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(LoopExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.body != null) {
            node.body.setFather(node);
            node.body.accept(this);
        }
        
        if (node.condition != null) {
            node.condition.setFather(node);
            node.condition.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(LiteralExprNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(PathExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.LSeg != null) {
            node.LSeg.setFather(node);
            node.LSeg.accept(this);
        }
        
        if (node.RSeg != null) {
            node.RSeg.setFather(node);
            node.RSeg.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(PathExprSegNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.name != null) {
            node.name.setFather(node);
            node.name.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(GroupExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.innerExpr != null) {
            node.innerExpr.setFather(node);
            node.innerExpr.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(OperExprNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(BorrowExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.innerExpr != null) {
            node.innerExpr.setFather(node);
            node.innerExpr.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(DerefExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.innerExpr != null) {
            node.innerExpr.setFather(node);
            node.innerExpr.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(NegaExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.innerExpr != null) {
            node.innerExpr.setFather(node);
            node.innerExpr.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(ArithExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.left != null) {
            node.left.setFather(node);
            node.left.accept(this);
        }
        
        if (node.right != null) {
            node.right.setFather(node);
            node.right.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(CompExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.left != null) {
            node.left.setFather(node);
            node.left.accept(this);
        }
        
        if (node.right != null) {
            node.right.setFather(node);
            node.right.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(LazyExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.left != null) {
            node.left.setFather(node);
            node.left.accept(this);
        }
        
        if (node.right != null) {
            node.right.setFather(node);
            node.right.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(AssignExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.left != null) {
            node.left.setFather(node);
            node.left.accept(this);
        }
        
        if (node.right != null) {
            node.right.setFather(node);
            node.right.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(ComAssignExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.left != null) {
            node.left.setFather(node);
            node.left.accept(this);
        }
        
        if (node.right != null) {
            node.right.setFather(node);
            node.right.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(TypeCastExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.expr != null) {
            node.expr.setFather(node);
            node.expr.accept(this);
        }
        
        if (node.type != null) {
            node.type.setFather(node);
            node.type.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(ArrayExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.elements != null) {
            for (int i = 0; i < node.elements.size(); i++) {
                node.elements.get(i).setFather(node);
                node.elements.get(i).accept(this);
            }
        }
        if (node.repeatedElement != null) {
            node.repeatedElement.setFather(node);
            node.repeatedElement.accept(this);
        }
        if (node.size != null) {
            node.size.setFather(node);
            node.size.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(IndexExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.array != null) {
            node.array.setFather(node);
            node.array.accept(this);
        }
        
        if (node.index != null) {
            node.index.setFather(node);
            node.index.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(StructExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.structName != null) {
            node.structName.setFather(node);
            node.structName.accept(this);
        }
        
        if (node.fieldValues != null) {
            for (int i = 0; i < node.fieldValues.size(); i++) {
                node.fieldValues.get(i).setFather(node);
                node.fieldValues.get(i).accept(this);
            }
        }
        
        currentParent = previousParent;
    }
    
    public void visit(FieldValNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.fieldName != null) {
            node.fieldName.setFather(node);
            node.fieldName.accept(this);
        }
        
        if (node.value != null) {
            node.value.setFather(node);
            node.value.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(FieldExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.receiver != null) {
            node.receiver.setFather(node);
            node.receiver.accept(this);
        }
        
        if (node.fieldName != null) {
            node.fieldName.setFather(node);
            node.fieldName.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(CallExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.function != null) {
            node.function.setFather(node);
            node.function.accept(this);
        }
        
        if (node.arguments != null) {
            for (int i = 0; i < node.arguments.size(); i++) {
                node.arguments.get(i).setFather(node);
                node.arguments.get(i).accept(this);
            }
        }
        
        currentParent = previousParent;
    }
    
    public void visit(MethodCallExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.receiver != null) {
            node.receiver.setFather(node);
            node.receiver.accept(this);
        }
        
        if (node.methodName != null) {
            node.methodName.setFather(node);
            node.methodName.accept(this);
        }
        
        if (node.arguments != null) {
            for (int i = 0; i < node.arguments.size(); i++) {
                node.arguments.get(i).setFather(node);
                node.arguments.get(i).accept(this);
            }
        }
        
        currentParent = previousParent;
    }
    
    public void visit(ContinueExprNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(BreakExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.value != null) {
            node.value.setFather(node);
            node.value.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(ReturnExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.value != null) {
            node.value.setFather(node);
            node.value.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(UnderscoreExprNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(TypeExprNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(TypePathExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.path != null) {
            node.path.setFather(node);
            node.path.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(TypeRefExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.innerType != null) {
            node.innerType.setFather(node);
            node.innerType.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(TypeArrayExprNode node) {
        ASTNode previousParent = currentParent;
        currentParent = node;
        
        if (node.elementType != null) {
            node.elementType.setFather(node);
            node.elementType.accept(this);
        }
        
        if (node.size != null) {
            node.size.setFather(node);
            node.size.accept(this);
        }
        
        currentParent = previousParent;
    }
    
    public void visit(TypeUnitExprNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(IdentifierNode node) {
        // Base implementation - no children to traverse
    }
}