// use the tokens we get, construct the AST.
abstract class ASTNode {
}

class StmtNode extends ASTNode {
}

class ItemNode extends StmtNode {
}
class LetStmtNode extends StmtNode {
    PatternNode variable;
    TypeExprNode type;
    ExprStmtNode value; // can be null
}
class ExprStmtNode extends StmtNode {
}


class FunctionNode extends ItemNode {
    boolean isConst;
    IdentifierNode name;
    Vector<ParameterNode> parameters;
    BlockExprNode body;
    TypeExprNode returnType; // can be null
    SelfParaNode selfPara; // can be null
    // set isConst to false initially
    FunctionNode() {
        isConst = false;
    }
}
class SelfParaNode extends ASTNode {
    boolean isMutable;
    boolean isReference;
    TypeExprNode type; // can be null
}
class ParameterNode extends ASTNode {
    PatternNode name;
    TypeExprNode type;
}
class PatternNode extends ASTNode {
}
class IdPatNode extends PatternNode {
    boolean isReference;
    IdentifierNode name;
}
class WildPatNode extends PatternNode {
}
class RefPatNode extends PatternNode {
    boolean isMutable;
    PatternNode innerPattern;
}

class StructNode extends ItemNode {
    IdentifierNode name;
    Vector<FieldNode> fields;
}


class FieldNode extends ItemNode {
    IdentifierNode name;
    TypeExprNode type;
}


class EnumNode extends ItemNode {
    IdentifierNode name;
    Vector<IdentifierNode> variants;
}


class ConstItemNode extends ItemNode {
    IdentifierNode name;
    TypeExprNode type;
    ExprStmtNode value;
}

class TraitNode extends ItemNode {
    IdentifierNode name;
    Vector<AssoItemNode> items;
}
class AssoItemNode extends ItemNode {
    // can be FunctionNode or ConstItemNode
    FunctionNode function;
    ConstItemNode constant;
}


class ImplNode extends ItemNode {
    IdentifierNode trait; // can be null if it's an inherent impl
    TypeExprNode typeName;
    Vector<AssoItemNode> items;
}


class ExprWithBlockNode extends ExprStmtNode {
}
class ExprWithoutBlockNode extends ExprStmtNode {
    ASTNode expression;
}


class LiteralExprNode extends ExprWithoutBlockNode {
    type_t literalType;
    String value_string; // for string and char literal
    int value_int; // for integer literal
    boolean value_bool; // for boolean literal
}

class PathExprNode extends ExprWithoutBlockNode {
    PathExprSegNode LSeg;
    PathExprSegNode RSeg; // can be null
}
class PathExprSegNode extends ASTNode {
    patternSeg_t patternType;
    IdentifierNode name;
}

class OperExprNode extends ExprWithoutBlockNode {
}
class BorrowExprNode extends OperExprNode {
    boolean isMutable;
    ExprStmtNode innerExpr;
}
class DerefExprNode extends OperExprNode {
    ExprStmtNode innerExpr;
}
class NegaExprNode extends OperExprNode {
    boolean isBitwise; // true if it's bitwise negation '~', false if it's arithmetic negation '-'
    ExprStmtNode innerExpr;
}
class ArithExprNode extends OperExprNode {
    oper_t operator; 
    ASTNode left;
    ASTNode right; 
}
class LazyExprNode extends OperExprNode {
    oper_t operator; 
    ASTNode left; 
    ASTNode right; 
}
class TypeCastExprNode extends OperExprNode {
    ExprStmtNode expression; 
    TypeExprNode targetType; 
}
class AssignExprNode extends OperExprNode {
    oper_t operator;
    ASTNode left; 
    ASTNode right; 
}


class ArrayExprNode extends ExprWithoutBlockNode {
    boolean isList; // true if it's a list, false if it's a repeated element
    Vector<ExprStmtNode> elements; 
    ExprStmtNode repeatedElement;
    ExprStmtNode size; 
}


class IndexExprNode extends ExprWithoutBlockNode {
    ExprStmtNode array; 
    ExprStmtNode index;
}


class StructExprNode extends ExprWithoutBlockNode {
    PathExprNode structName; 
    Vector<FieldValNode> fieldAssignments;
}
class FieldValNode extends ASTNode {
    IdentifierNode name;
    ExprStmtNode value;
}


class CallExprNode extends ExprWithoutBlockNode {
    ExprStmtNode function;
    Vector<ExprStmtNode> arguments;
}


class MethodCallExprNode extends ExprWithoutBlockNode {
    ExprStmtNode receiver;
    PathExprSegNode methodName;
    Vector<ExprStmtNode> arguments;
}


class FieldExprNode extends ExprWithoutBlockNode {
    ExprStmtNode receiver;
    IdentifierNode fieldName;
}


class ContinueExprNode extends ExprWithoutBlockNode {
}
class BreakExprNode extends ExprWithoutBlockNode {
    ASTNode value; // can be null
}
class ReturnExprNode extends ExprWithoutBlockNode {
    ASTNode value; // can be null
}


class UnderscoreExprNode extends ExprWithoutBlockNode {
}


class BlockExprNode extends ExprWithBlockNode {
    Vector<StmtNode> statements;
}

class IfExprNode extends ExprWithBlockNode {
    ExprStmtNode condition;  
    BlockExprNode thenBranch;
    BlockExprNode elseBranch; // can be null
    IfExprNode elseifBranch; // can be null
}

class LoopExprNode extends ExprWithBlockNode {
    ExprStmtNode contidion; // can be null
    BlockExprNode body;
}

class IdentifierNode extends ASTNode {
    String name;
}


class TypeExprNode extends ExprStmtNode {
    String value; 
}

class TypePathExprNode extends TypeExprNode {
    PathExprSegNode Seg;
}

class TypeRefExprNode extends TypeExprNode {
    boolean isMutable;
    TypeExprNode innerType;
}

class TypeArrayExprNode extends TypeExprNode {
    TypeExprNode elementType;
    ExprStmtNode size;
}

class TypeUnitExprNode extends TypeExprNode {
}