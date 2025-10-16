// a program that prints the structure of the AST
// the way to print is following:
// for each node, first print the node type
// then print its children nodes recursively
// the output is indented according to the depth of the node in the AST
// this class extends VisitorBase and implements the visit methods
// for some node types, it has some specific field to print
// for other node types, it just prints the node type and visits its children
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
    public void visit(StmtNode node) {
        // decide which specific StmtNode it is
        switch node {
            case LetStmtNode n -> visit(n);
            case ExprStmtNode n -> visit(n);
            case ItemNode n -> visit(n);
            default -> assert false : "unknown StmtNode";
        }
    }
    @Override
    public void visit(ItemNode node) {
        // decide which specific ItemNode it is
        switch node {
            case FunctionNode n -> visit(n);
            case StructNode n -> visit(n);
            case EnumNode n -> visit(n);
            case ConstItem n -> visit(n);
            case TraitNode n -> visit(n);
            case ImplNode n -> visit(n);
            default -> assert false : "unknown ItemNode";
        }
    }
    @Override
    public void visit(LetStmtNode node) {
        printIndent();
        System.out.println("LetStmtNode");
        indent();
        visit(node.name);
        visit(node.type);
        if (node.value != null) visit(node.value);
        dedent();
    }
    @Override
    public void visit(ExprStmtNode node) {
        printIndent();
        System.out.println("ExprStmtNode");
        indent();
        visit(node.expr);
        dedent();
    }
    @Override
    public void visit(FunctionNode node) {
        printIndent();
        System.out.print("FunctionNode: ");
        System.out.println("isConst = " + node.isConst);
        indent();
        visit(node.name);
        if (node.selfPara != null) visit(node.selfPara);
        for (ParameterNode param : node.parameters) {
            visit(param);
        }
        if (node.returnType != null) visit(node.returnType);
        if (node.body != null) visit(node.body);
        dedent();
    }
    @Override
    public void visit(SelfParaNode node) {
        printIndent();
        System.out.print("SelfParaNode: ");
        System.out.print("isMutable = " + node.isMutable + ", ");
        System.out.println("isReference = " + node.isReference);
        indent();
        if (node.type != null) visit(node.type);
        dedent();
    }
    @Override
    public void visit(ParameterNode node) {
        printIndent();
        System.out.println("ParameterNode");
        indent();
        visit(node.name);
        visit(node.type);
        dedent();
    }
    @Override
    public void visit(PatternNode node) {
        // decide which specific PatternNode it is
        switch node {
            case IdPatNode n -> visit(n);
            case WildPatNode n -> visit(n);
            case RefPatNode n -> visit(n);
            default -> assert false : "unknown PatternNode";
        }
    }
    @Override
    public void visit(IdPatNode node) {
        printIndent();
        System.out.print("IdPatNode: ");
        System.out.print("isMutable = " + node.isMutable + ", ");
        System.out.println("isReference = " + node.isReference);
        indent();
        visit(node.name);
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
        visit(node.innerPattern);
        dedent();
    }
    @Override
    public void visit(StructNode node) {
        printIndent();
        System.out.println("StructNode");
        indent();
        visit(node.name);
        for (FieldNode field : node.fields) {
            visit(field);
        }
        dedent();
    }
    @Override
    public void visit(FieldNode node) {
        printIndent();
        System.out.println("FieldNode");
        indent();
        visit(node.name);
        visit(node.type);
        dedent();
    }
    @Override
    public void visit(EnumNode node) {
        printIndent();
        System.out.println("EnumNode");
        indent();
        visit(node.name);
        for (IdentifierNode variant : node.variants) {
            visit(variant);
        }
        dedent();
    }
    @Override
    public void visit(ConstItem node) {
        printIndent();
        System.out.println("ConstItem");
        indent();
        visit(node.name);
        visit(node.type);
        if (node.value != null) visit(node.value);
        dedent();
    }
    @Override
    public void visit(TraitNode node) {
        printIndent();
        System.out.println("TraitNode");
        indent();
        visit(node.name);
        for (AssoItemNode item : node.items) {
            visit(item);
        }
        dedent();
    }
    @Override
    public void visit(ImplNode node) {
        printIndent();
        System.out.println("ImplNode");
        indent();
        if (node.trait != null) visit(node.trait);
        visit(node.typeName);
        for (AssoItemNode item : node.items) {
            visit(item);
        }
        dedent();
    }
    @Override
    public void visit(AssoItemNode node) {
        // decide which specific AssoItemNode it is
        switch node {
            case FunctionNode n -> visit(n);
            case ConstItem n -> visit(n);
            default -> assert false : "unknown AssoItemNode";
        }
    }
    @Override
    public void visit(ExprNode node) {
        // decide which specific ExprNode it is
        switch node {
            case ExprWithBlockNode n -> visit(n);
            case ExprWithoutBlockNode n -> visit(n);
            default -> assert false : "unknown ExprNode";
        }
    }
    @Override
    public void visit(ExprWithBlockNode node) {
        // decide which specific ExprWithBlockNode it is
        switch node {
            case BlockExprNode n -> visit(n);
            case IfExprNode n -> visit(n);
            case LoopExprNode n -> visit(n);
            default -> assert false : "unknown ExprWithBlockNode";
        }
    }
    @Override
    public void visit(ExprWithoutBlockNode node) {
        // decide which specific ExprWithoutBlockNode it is
        switch (node) {
            case LiteralExprNode n -> visit(n);
            case PathExprNode n -> visit(n);
            case GroupExprNode n -> visit(n);
            case OperExprNode n -> visit(n);
            case TypeCastExprNode n -> visit(n);
            case ArrayExprNode n -> visit(n);
            case IndexExprNode n -> visit(n);
            case StructExprNode n -> visit(n);
            case FieldExprNode n -> visit(n);
            case CallExprNode n -> visit(n);
            case MethodCallExprNode n -> visit(n);
            case ContinueExprNode n -> visit(n);
            case BreakExprNode n -> visit(n);
            case ReturnExprNode n -> visit(n);
            case UnderscoreExprNode n -> visit(n);
            default -> assert false : "unknown ExprWithoutBlockNode";
        }
    }
    @Override
    public void visit(BlockExprNode node) {
        printIndent();
        System.out.println("BlockExprNode");
        indent();
        for (StmtNode stmt : node.statements) {
            visit(stmt);
        }
        dedent();
    }
    @Override
    public void visit(IfExprNode node) {
        printIndent();
        System.out.println("IfExprNode");
        indent();
        visit(node.condition);
        visit(node.thenBranch);
        if (node.elseBranch != null) visit(node.elseBranch);
        if (node.elseifBranch != null) visit(node.elseifBranch);
        dedent();
    }
    @Override
    public void visit(LoopExprNode node) {
        printIndent();
        System.out.println("LoopExprNode");
        indent();
        if (node.condition != null) visit(node.condition);
        visit(node.body);
        dedent();
    }
    @Override
    public void visit(LiteralExprNode node) {
        printIndent();
        System.out.print("LiteralExprNode: ");
        print(node.literalType);
        System.out.print(" = ");
        switch (node.literalType) {
            case INT -> System.out.println(node.value_int);
            case BOOL -> System.out.println(node.value_bool);
            case CHAR -> System.out.println("'" + node.value_string[0] + "'");
            case STRING -> System.out.println("\"" + node.value_string + "\"");
            case CSTRING -> System.out.println("\"" + node.value_string + "\"");
            default -> assert false : "unknown literal type";
        }
    }
    private void print(literal_t type) {
        switch (type) {
            case CHAR -> System.out.print("CHAR");
            case STRING -> System.out.print("STRING");
            case CSTRING -> System.out.print("CSTRING");
            case INT -> System.out.print("INT");
            case BOOL -> System.out.print("BOOL");
            default -> assert false : "unknown literal type";
        }
    }
    @Override
    public void visit(PathExprNode node) {
        printIndent();
        System.out.println("PathExprNode");
        indent();
        visit(node.LSeg);
        if (node.RSeg != null) visit(node.RSeg);
        dedent();
    }
    @Override
    public void visit(PathExprSegNode node) {
        printIndent();
        System.out.print("PathExprSegNode: ");
        indent();
        switch (node.patternType) {
            case IDENT {
                System.out.println("patternType = IDENT");
                visit(node.name);
            }
            case SELF -> System.out.println("patternType = SELF");
            case SELF_TYPE -> System.out.println("patternType = SELF_TYPE");
            default -> assert false : "unknown pattern type";
        }
        dedent();
    }
    @Override
    public void visit(GroupExprNode node) {
        printIndent();
        System.out.println("GroupExprNode");
        indent();
        visit(node.innerExpr);
        dedent();
    }
    @Override
    public void visit(OperExprNode node) {
        // decide which specific OperExprNode it is
        switch node {
            case BorrowExprNode n -> visit(n);
            case DerefExprNode n -> visit(n);
            case NegaExprNode n -> visit(n);
            case ArithExprNode n -> visit(n);
            case CompExprNode n -> visit(n);
            case LazyExprNode n -> visit(n);
            case AssignExprNode n -> visit(n);
            case ComAssignExprNode n -> visit(n);
            default -> assert false : "unknown OperExprNode";
        }
    }
    @Override
    public void visit(BorrowExprNode node) {
        printIndent();
        System.out.print("BorrowExprNode: ");
        System.out.print("isMutable = " + node.isMutable + ", ");
        System.out.println("isDouble = " + node.isDouble);
        indent();
        visit(node.innerExpr);
        dedent();
    }
    @Override
    public void visit(DerefExprNode node) {
        printIndent();
        System.out.println("DerefExprNode");
        indent();
        visit(node.innerExpr);
        dedent();
    }
    @Override
    public void visit(NegaExprNode node) {
        printIndent();
        System.out.print("NegaExprNode: ");
        System.out.println("isLogical = " + node.isLogical);
        indent();
        visit(node.innerExpr);
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
            default -> assert false : "unknown arithmetic operator";
        }
        indent();
        visit(node.left);
        visit(node.right);
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
            default -> assert false : "unknown comparison operator";
        }
        indent();
        visit(node.left);
        visit(node.right);
        dedent();
    }
    @Override
    public void visit(LazyExprNode node) {
        printIndent();
        System.out.print("LazyExprNode: ");
        switch (node.operator) {
            case LOGICAL_AND -> System.out.println("operator = LOGICAL_AND");
            case LOGICAL_OR -> System.out.println("operator = LOGICAL_OR");
            default -> assert false : "unknown lazy operator";
        }
        indent();
        visit(node.left);
        visit(node.right);
        dedent();
    }
    @Override
    public void visit(AssignExprNode node) {
        printIndent();
        System.out.println("AssignExprNode");
        indent();
        visit(node.left);
        visit(node.right);
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
            default -> assert false : "unknown compound assignment operator";
        }
        indent();
        visit(node.left);
        visit(node.right);
        dedent();
    }
    @Override
    public void visit(TypeCastExprNode node) {
        printIndent();
        System.out.println("TypeCastExprNode");
        indent();
        visit(node.expr);
        visit(node.type);
        dedent();
    }
    @Override
    public void visit(ArrayExprNode node) {
        printIndent();
        System.out.print("ArrayExprNode: ");
        indent();
        if (node.repeatedElement != null) {
            System.out.println("repeated element");
            visit(node.repeatedElement);
            visit(node.size);
        } else {
            System.out.println("elements");
            for (ExprNode element : node.elements) {
                visit(element);
            }
        }
        dedent();
    }
    @Override
    public void visit(IndexExprNode node) {
        printIndent();
        System.out.println("IndexExprNode");
        indent();
        visit(node.array);
        visit(node.index);
        dedent();
    }
    @Override
    public void visit(StructExprNode node) {
        printIndent();
        System.out.println("StructExprNode");
        indent();
        visit(node.structName);
        for (FieldValNode field : node.fieldValues) {
            visit(field);
        }
        dedent();
    }
    @Override
    public void visit(FieldValNode node) {
        printIndent();
        System.out.println("FieldValNode");
        indent();
        visit(node.fieldName);
        visit(node.value);
        dedent();
    }
    @Override
    public void visit(FieldExprNode node) {
        printIndent();
        System.out.println("FieldExprNode");
        indent();
        visit(node.receiver);
        visit(node.fieldName);
        dedent();
    }
    @Override
    public void visit(CallExprNode node) {
        printIndent();
        System.out.println("CallExprNode");
        indent();
        visit(node.function);
        for (ExprNode arg : node.arguments) {
            visit(arg);
        }
        dedent();
    }
    @Override
    public void visit(MethodCallExprNode node) {
        printIndent();
        System.out.println("MethodCallExprNode");
        indent();
        visit(node.receiver);
        visit(node.methodName);
        for (ExprNode arg : node.arguments) {
            visit(arg);
        }
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
        if (node.value != null) visit(node.value);
        dedent();
    }
    @Override
    public void visit(ReturnExprNode node) {
        printIndent();
        System.out.println("ReturnExprNode");
        indent();
        if (node.value != null) visit(node.value);
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
        switch node {
            case TypePathExprNode n -> visit(n);
            case TypeRefExprNode n -> visit(n);
            case TypeArrayExprNode n -> visit(n);
            case TypeUnitExprNode n -> visit(n);
            default -> assert false : "unknown TypeExprNode";
        }
    }
    @Override
    public void visit(TypePathExprNode node) {
        printIndent();
        System.out.println("TypePathExprNode");
        indent();
        visit(node.path);
        dedent();
    }
    @Override
    public void visit(TypeRefExprNode node) {
        printIndent();
        System.out.print("TypeRefExprNode: ");
        System.out.println("isMutable = " + node.isMutable);
        indent();
        visit(node.innerType);
        dedent();
    }
    @Override
    public void visit(TypeArrayExprNode node) {
        printIndent();
        System.out.println("TypeArrayExprNode");
        indent();
        visit(node.elementType);
        visit(node.size);
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
        System.out.println("IdentifierNode: " + node.name);
    }
}