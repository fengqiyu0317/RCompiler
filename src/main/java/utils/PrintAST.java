

// a program that prints structure of AST
// way to print is following:
// for each node, first print node type
// then print its children nodes recursively
// output is indented according to the depth of node in the AST
// this class extends VisitorBase and implements visit methods
// for some node types, it has some specific field to print
// for other node types, it just prints node type and visits its children
public class PrintAST extends VisitorBase {
    private int indentLevel = 0;
    private void printIndent() {
        for (int i = 0; i < indentLevel; i++) {
            System.out.print("  ");
        }
    }
    private void indent() {
        indentLevel++;
    }
    private void dedent() {
        indentLevel--;
    }
    @Override
    public void visit(ASTNode node) {}
    @Override
    public void visit(StmtNode node) {
        // System.out.println(node instanceof FunctionNode);
        // decide which specific StmtNode it is
        if (node instanceof LetStmtNode n) {
            visit(n);
        } else if (node instanceof ExprStmtNode n) {
            visit(n);
        } else if (node instanceof ItemNode n) {
            visit(n);
        } else {
            throw new ASTPrintException("unknown StmtNode");
        }
    }
    @Override
    public void visit(ItemNode node) {
        // decide which specific ItemNode it is
        if (node instanceof FunctionNode n) {
            visit(n);
        } else if (node instanceof StructNode n) {
            visit(n);
        } else if (node instanceof EnumNode n) {
            visit(n);
        } else if (node instanceof ConstItemNode n) {
            visit(n);
        } else if (node instanceof TraitNode n) {
            visit(n);
        } else if (node instanceof ImplNode n) {
            visit(n);
        } else {
            throw new ASTPrintException("unknown ItemNode");
        }
    }
    @Override
    public void visit(LetStmtNode node) {
        printIndent();
        System.out.println("LetStmtNode");
        indent();
        node.name.accept(this);
        node.type.accept(this);
        if (node.value != null) node.value.accept(this);
        dedent();
    }
    @Override
    public void visit(ExprStmtNode node) {
        printIndent();
        System.out.println("ExprStmtNode");
        indent();
        node.expr.accept(this);
        dedent();
    }
    @Override
    public void visit(FunctionNode node) {
        printIndent();
        System.out.print("FunctionNode: ");
        System.out.println("isConst = " + node.isConst);
        indent();
        node.name.accept(this);
        if (node.selfPara != null) node.selfPara.accept(this);
        for (ParameterNode param : node.parameters) {
            param.accept(this);
        }
        if (node.returnType != null) node.returnType.accept(this);
        if (node.body != null) node.body.accept(this);
        dedent();
    }
    @Override
    public void visit(SelfParaNode node) {
        printIndent();
        System.out.print("SelfParaNode: ");
        System.out.print("isMutable = " + node.isMutable + ", ");
        System.out.println("isReference = " + node.isReference);
        indent();
        if (node.type != null) node.type.accept(this);
        dedent();
    }
    @Override
    public void visit(ParameterNode node) {
        printIndent();
        System.out.println("ParameterNode");
        indent();
        node.name.accept(this);
        node.type.accept(this);
        dedent();
    }
    @Override
    public void visit(PatternNode node) {
        // decide which specific PatternNode it is
        if (node instanceof IdPatNode n) {
            visit(n);
        } else if (node instanceof WildPatNode n) {
            visit(n);
        } else if (node instanceof RefPatNode n) {
            visit(n);
        } else {
            throw new ASTPrintException("unknown PatternNode");
        }
    }
    @Override
    public void visit(IdPatNode node) {
        printIndent();
        System.out.print("IdPatNode: ");
        System.out.print("isMutable = " + node.isMutable + ", ");
        System.out.println("isReference = " + node.isReference);
        indent();
        node.name.accept(this);
        dedent();
    }
    @Override
    public void visit(WildPatNode node) {
        printIndent();
        System.out.println("WildPatNode");
    }
    @Override
    public void visit(RefPatNode node) {
        printIndent();
        System.out.print("RefPatNode: ");
        System.out.println("isMutable = " + node.isMutable);
        indent();
        node.innerPattern.accept(this);
        dedent();
    }
    @Override
    public void visit(StructNode node) {
        printIndent();
        System.out.println("StructNode");
        indent();
        node.name.accept(this);
        for (FieldNode field : node.fields) {
            field.accept(this);
        }
        dedent();
    }
    @Override
    public void visit(FieldNode node) {
        printIndent();
        System.out.println("FieldNode");
        indent();
        node.name.accept(this);
        node.type.accept(this);
        dedent();
    }
    @Override
    public void visit(EnumNode node) {
        printIndent();
        System.out.println("EnumNode");
        indent();
        node.name.accept(this);
        for (IdentifierNode variant : node.variants) {
            variant.accept(this);
        }
        dedent();
    }
    @Override
    public void visit(ConstItemNode node) {
        printIndent();
        System.out.println("ConstItem");
        indent();
        node.name.accept(this);
        node.type.accept(this);
        if (node.value != null) node.value.accept(this);
        dedent();
    }
    @Override
    public void visit(TraitNode node) {
        printIndent();
        System.out.println("TraitNode");
        indent();
        node.name.accept(this);
        for (AssoItemNode item : node.items) {
            item.accept(this);
        }
        dedent();
    }
    @Override
    public void visit(ImplNode node) {
        printIndent();
        System.out.println("ImplNode");
        indent();
        if (node.trait != null) node.trait.accept(this);
        node.typeName.accept(this);
        for (AssoItemNode item : node.items) {
            item.accept(this);
        }
        dedent();
    }
    @Override
    public void visit(AssoItemNode node) {
        // decide which specific AssoItemNode it is
        if (node.function != null) {
            node.function.accept(this);
            return;
        } else {
            node.constant.accept(this);
            return;
        }
    }
    @Override
    public void visit(ExprNode node) {
        // decide which specific ExprNode it is
        if (node instanceof ExprWithBlockNode n) {
            visit(n);
        } else if (node instanceof ExprWithoutBlockNode n) {
            visit(n);
        } else {
            throw new ASTPrintException("unknown ExprNode");
        }
    }
    @Override
    public void visit(ExprWithBlockNode node) {
        // decide which specific ExprWithBlockNode it is
        if (node instanceof BlockExprNode n) {
            visit(n);
        } else if (node instanceof IfExprNode n) {
            visit(n);
        } else if (node instanceof LoopExprNode n) {
            visit(n);
        } else {
            throw new ASTPrintException("unknown ExprWithBlockNode");
        }
    }
    @Override
    public void visit(ExprWithoutBlockNode node) {
        // decide which specific ExprWithoutBlockNode it is
        if (node.expr != null) {
            node.expr.accept(this);
            return;
        }
        if (node instanceof LiteralExprNode n) {
            n.accept(this);
        } else if (node instanceof PathExprNode n) {
            n.accept(this);
        } else if (node instanceof GroupExprNode n) {
            n.accept(this);
        } else if (node instanceof OperExprNode n) {
            n.accept(this);
        } else if (node instanceof ArrayExprNode n) {
            n.accept(this);
        } else if (node instanceof IndexExprNode n) {
            n.accept(this);
        } else if (node instanceof StructExprNode n) {
            n.accept(this);
        } else if (node instanceof FieldExprNode n) {
            n.accept(this);
        } else if (node instanceof CallExprNode n) {
            n.accept(this);
        } else if (node instanceof MethodCallExprNode n) {
            n.accept(this);
        } else if (node instanceof ContinueExprNode n) {
            n.accept(this);
        } else if (node instanceof BreakExprNode n) {
            n.accept(this);
        } else if (node instanceof ReturnExprNode n) {
            n.accept(this);
        } else if (node instanceof UnderscoreExprNode n) {
            n.accept(this);
        } else {
            throw new ASTPrintException("unknown ExprWithoutBlockNode");
        }
    }
    @Override
    public void visit(BlockExprNode node) {
        printIndent();
        System.out.println("BlockExprNode");
        indent();
        if (node.statements != null) {
            for (StmtNode stmt : node.statements) {
                stmt.accept(this);
            }
        }
        if (node.returnValue != null) {
            printIndent();
            System.out.println("ReturnValue:");
            indent();
            node.returnValue.accept(this);
            dedent();
        }
        dedent();
    }
    @Override
    public void visit(IfExprNode node) {
        printIndent();
        System.out.println("IfExprNode");
        indent();
        node.condition.accept(this);
        node.thenBranch.accept(this);
        if (node.elseBranch != null) node.elseBranch.accept(this);
        if (node.elseifBranch != null) node.elseifBranch.accept(this);
        dedent();
    }
    @Override
    public void visit(LoopExprNode node) {
        printIndent();
        System.out.println("LoopExprNode");
        indent();
        if (node.condition != null) node.condition.accept(this);
        node.body.accept(this);
        dedent();
    }
    @Override
    public void visit(LiteralExprNode node) {
        printIndent();
        System.out.print("LiteralExprNode: ");
        print(node.literalType);
        System.out.print(" = ");
        switch (node.literalType) {
            case INT -> System.out.println(node.value_long);
            case U32 -> System.out.println(node.value_long);
            case USIZE -> System.out.println(node.value_long);
            case ISIZE -> System.out.println(node.value_long);
            case BOOL -> System.out.println(node.value_bool);
            case CHAR -> System.out.println("'" + node.value_string + "'");
            case STRING -> System.out.println("\"" + node.value_string + "\"");
            case CSTRING -> System.out.println("\"" + node.value_string + "\"");
            default -> {throw new ASTPrintException("unknown literal type");}
        }
    }
    private void print(literal_t type) {
        switch (type) {
            case CHAR -> System.out.print("CHAR");
            case STRING -> System.out.print("STRING");
            case CSTRING -> System.out.print("CSTRING");
            case INT -> System.out.print("I32");
            case U32 -> System.out.print("U32");
            case USIZE -> System.out.print("USIZE");
            case ISIZE -> System.out.print("ISIZE");
            case BOOL -> System.out.print("BOOL");
            default -> {throw new ASTPrintException("unknown literal type");}
        }
    }
    @Override
    public void visit(PathExprNode node) {
        printIndent();
        System.out.println("PathExprNode");
        indent();
        node.LSeg.accept(this);
        if (node.RSeg != null) node.RSeg.accept(this);
        dedent();
    }
    @Override
    public void visit(PathExprSegNode node) {
        printIndent();
        System.out.print("PathExprSegNode: ");
        indent();
        switch (node.patternType) {
            case IDENT -> {
                System.out.println("patternType = IDENT");
                node.name.accept(this);
            }
            case SELF -> System.out.println("patternType = SELF");
            case SELF_TYPE -> System.out.println("patternType = SELF_TYPE");
            default -> {throw new ASTPrintException("unknown pattern type");}
        }
        dedent();
    }
    @Override
    public void visit(GroupExprNode node) {
        printIndent();
        System.out.println("GroupExprNode");
        indent();
        node.innerExpr.accept(this);
        dedent();
    }
    @Override
    public void visit(OperExprNode node) {
        // decide which specific OperExprNode it is
        if (node instanceof BorrowExprNode n) {
            n.accept(this);
        } else if (node instanceof DerefExprNode n) {
            n.accept(this);
        } else if (node instanceof NegaExprNode n) {
            n.accept(this);
        } else if (node instanceof ArithExprNode n) {
            n.accept(this);
        } else if (node instanceof CompExprNode n) {
            n.accept(this);
        } else if (node instanceof LazyExprNode n) {
            n.accept(this);
        } else if (node instanceof AssignExprNode n) {
            n.accept(this);
        } else if (node instanceof ComAssignExprNode n) {
            n.accept(this);
        } else if (node instanceof TypeCastExprNode n) {
            n.accept(this);
        } else {
            throw new ASTPrintException("unknown OperExprNode");
        }
    }
    @Override
    public void visit(BorrowExprNode node) {
        printIndent();
        System.out.print("BorrowExprNode: ");
        System.out.print("isMutable = " + node.isMutable + ", ");
        System.out.println("isDouble = " + node.isDoubleReference);
        indent();
        node.innerExpr.accept(this);
        dedent();
    }
    @Override
    public void visit(DerefExprNode node) {
        printIndent();
        System.out.println("DerefExprNode");
        indent();
        node.innerExpr.accept(this);
        dedent();
    }
    @Override
    public void visit(NegaExprNode node) {
        printIndent();
        System.out.print("NegaExprNode: ");
        System.out.println("isLogical = " + node.isLogical);
        indent();
        node.innerExpr.accept(this);
        dedent();
    }
    @Override
    public void visit(ArithExprNode node) {
        printIndent();
        System.out.print("ArithExprNode: ");
        switch (node.operator) {
            case PLUS -> System.out.println("operator = ADD");
            case MINUS -> System.out.println("operator = SUB");
            case MUL -> System.out.println("operator = MUL");
            case DIV -> System.out.println("operator = DIV");
            case MOD -> System.out.println("operator = MOD");
            case AND -> System.out.println("operator = AND");
            case OR -> System.out.println("operator = OR");
            case XOR -> System.out.println("operator = XOR");
            case SHL -> System.out.println("operator = SHL");
            case SHR -> System.out.println("operator = SHR");
            default -> {throw new ASTPrintException("unknown arithmetic operator");}
        }
        indent();
        node.left.accept(this);
        node.right.accept(this);
        dedent();
    }
    @Override
    public void visit(CompExprNode node) {
        printIndent();
        System.out.print("CompExprNode: ");
        switch (node.operator) {
            case EQ -> System.out.println("operator = EQ");
            case NEQ -> System.out.println("operator = NEQ");
            case GT -> System.out.println("operator = GT");
            case LT -> System.out.println("operator = LT");
            case GTE -> System.out.println("operator = GTE");
            case LTE -> System.out.println("operator = LTE");
            default -> {throw new ASTPrintException("unknown comparison operator");}
        }
        indent();
        node.left.accept(this);
        node.right.accept(this);
        dedent();
    }
    @Override
    public void visit(LazyExprNode node) {
        printIndent();
        System.out.print("LazyExprNode: ");
        switch (node.operator) {
            case LOGICAL_AND -> System.out.println("operator = LOGICAL_AND");
            case LOGICAL_OR -> System.out.println("operator = LOGICAL_OR");
            default -> {throw new ASTPrintException("unknown lazy operator");}
        }
        indent();
        node.left.accept(this);
        node.right.accept(this);
        dedent();
    }
    @Override
    public void visit(AssignExprNode node) {
        printIndent();
        System.out.println("AssignExprNode");
        indent();
        node.left.accept(this);
        node.right.accept(this);
        dedent();
    }
    @Override
    public void visit(ComAssignExprNode node) {
        printIndent();
        System.out.print("ComAssignExprNode: ");
        switch (node.operator) {
            case PLUS_ASSIGN -> System.out.println("operator = ADD_ASSIGN");
            case MINUS_ASSIGN -> System.out.println("operator = SUB_ASSIGN");
            case MUL_ASSIGN -> System.out.println("operator = MUL_ASSIGN");
            case DIV_ASSIGN -> System.out.println("operator = DIV_ASSIGN");
            case MOD_ASSIGN -> System.out.println("operator = MOD_ASSIGN");
            case AND_ASSIGN -> System.out.println("operator = AND_ASSIGN");
            case OR_ASSIGN -> System.out.println("operator = OR_ASSIGN");
            case XOR_ASSIGN -> System.out.println("operator = XOR_ASSIGN");
            case SHL_ASSIGN -> System.out.println("operator = SHL_ASSIGN");
            case SHR_ASSIGN -> System.out.println("operator = SHR_ASSIGN");
            default -> {throw new ASTPrintException("unknown compound assignment operator");}
        }
        indent();
        node.left.accept(this);
        node.right.accept(this);
        dedent();
    }
    @Override
    public void visit(TypeCastExprNode node) {
        printIndent();
        System.out.println("TypeCastExprNode");
        indent();
        node.expr.accept(this);
        node.type.accept(this);
        dedent();
    }
    @Override
    public void visit(ArrayExprNode node) {
        printIndent();
        System.out.print("ArrayExprNode: ");
        indent();
        if (node.repeatedElement != null) {
            System.out.println("repeated element");
            node.repeatedElement.accept(this);
            node.size.accept(this);
        } else {
            System.out.println("elements");
            for (ExprNode element : node.elements) {
                element.accept(this);
            }
        }
        dedent();
    }
    @Override
    public void visit(IndexExprNode node) {
        printIndent();
        System.out.println("IndexExprNode");
        indent();
        node.array.accept(this);
        node.index.accept(this);
        dedent();
    }
    @Override
    public void visit(StructExprNode node) {
        printIndent();
        System.out.println("StructExprNode");
        indent();
        node.structName.accept(this);
        for (FieldValNode field : node.fieldValues) {
            field.accept(this);
        }
        dedent();
    }
    @Override
    public void visit(FieldValNode node) {
        printIndent();
        System.out.println("FieldValNode");
        indent();
        node.fieldName.accept(this);
        node.value.accept(this);
        dedent();
    }
    @Override
    public void visit(CallExprNode node) {
        printIndent();
        System.out.println("CallExprNode");
        indent();
        node.function.accept(this);
        for (ExprNode arg : node.arguments) {
            arg.accept(this);
        }
        dedent();
    }
    @Override
    public void visit(MethodCallExprNode node) {
        printIndent();
        System.out.println("MethodCallExprNode");
        indent();
        node.receiver.accept(this);
        node.methodName.accept(this);
        for (ExprNode arg : node.arguments) {
            arg.accept(this);
        }
        dedent();
    }
    @Override
    public void visit(FieldExprNode node) {
        printIndent();
        System.out.println("FieldExprNode");
        indent();
        node.receiver.accept(this);
        node.fieldName.accept(this);
        dedent();
    }
    @Override
    public void visit(ContinueExprNode node) {
        printIndent();
        System.out.println("ContinueExprNode");
    }
    @Override
    public void visit(BreakExprNode node) {
        printIndent();
        System.out.println("BreakExprNode");
        indent();
        if (node.value != null) node.value.accept(this);
        dedent();
    }
    @Override
    public void visit(ReturnExprNode node) {
        printIndent();
        System.out.println("ReturnExprNode");
        indent();
        if (node.value != null) node.value.accept(this);
        dedent();
    }
    @Override
    public void visit(UnderscoreExprNode node) {
        printIndent();
        System.out.println("UnderscoreExprNode");
    }
    @Override
    public void visit(TypeExprNode node) {
        // decide which specific TypeExprNode it is
        if (node instanceof TypePathExprNode n) {
            n.accept(this);
        } else if (node instanceof TypeRefExprNode n) {
            n.accept(this);
        } else if (node instanceof TypeArrayExprNode n) {
            n.accept(this);
        } else if (node instanceof TypeUnitExprNode n) {
            n.accept(this);
        } else {
            throw new ASTPrintException("unknown TypeExprNode");
        }
    }
    @Override
    public void visit(TypePathExprNode node) {
        printIndent();
        System.out.println("TypePathExprNode");
        indent();
        node.path.accept(this);
        dedent();
    }
    @Override
    public void visit(TypeRefExprNode node) {
        printIndent();
        System.out.print("TypeRefExprNode: ");
        System.out.println("isMutable = " + node.isMutable);
        indent();
        node.innerType.accept(this);
        dedent();
    }
    @Override
    public void visit(TypeArrayExprNode node) {
        printIndent();
        System.out.println("TypeArrayExprNode");
        indent();
        node.elementType.accept(this);
        node.size.accept(this);
        dedent();
    }
    @Override
    public void visit(TypeUnitExprNode node) {
        printIndent();
        System.out.println("TypeUnitExprNode");
    }
    @Override
    public void visit(IdentifierNode node) {
        printIndent();
        if (node != null && node.name != null) {
            System.out.println("IdentifierNode: " + node.name);
        } else {
            System.out.println("IdentifierNode: null");
        }
    }
}