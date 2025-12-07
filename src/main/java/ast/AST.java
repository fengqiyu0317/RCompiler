import java.util.Vector;
// use the tokens we get, construct the AST.

// ASTNode is the base class for all AST nodes.
abstract class ASTNode {
    // Father node in the AST tree
    protected ASTNode father;
    
    // Line and column information for error reporting
    protected int line = -1;
    protected int column = -1;
    
    // Accept a visitor
    public abstract void accept(VisitorBase visitor);
    
    // Get father node
    public ASTNode getFather() {
        return father;
    }
    
    // Set father node
    public void setFather(ASTNode father) {
        this.father = father;
    }
    
    // Get line number
    public int getLine() {
        return line;
    }
    
    // Set line number
    public void setLine(int line) {
        this.line = line;
    }
    
    // Get column number
    public int getColumn() {
        return column;
    }
    
    // Set column number
    public void setColumn(int column) {
        this.column = column;
    }
}

class StmtNode extends ASTNode {
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// ItemNode represents an item <item>.
// There are several kinds of items: function, struct, enum, constant, trait, impl. so <item> = <function> | <structitem> | <enumitem> | <constitem> | <traititem> | <implitem>.
class ItemNode extends StmtNode {
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// LetStmtNode represents a let statement <letstmt>.
// The grammer for let statement is:
// <letstmt> = let <pattern> : <type> (= <expression>)? ;
class LetStmtNode extends StmtNode {
    PatternNode name;
    TypeExprNode type;
    ExprNode value; // can be null
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// ExprStmtNode represents an expression statement <exprstmt>.
// The grammer for expression statement is:
// <exprstmt> = <exprwithblock> ;? | <exprwithoutblock> ;
class ExprStmtNode extends StmtNode {
    ExprNode expr;
    boolean hasSemicolon; // 记录是否有分号
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// FunctionNode represents a function item <function>.
// The grammer for function definition is:
// <function> = (const)? fn <identifier> (<parameters>?) (-> <type>)? ( <blockexpr> | ; )
// <parameters> = <selfpara> ,? | (<selfpara> ,)? <parameter> (, <parameter>)* ,?
class FunctionNode extends ItemNode {
    boolean isConst; // true if it's a const function, false if it's not
    IdentifierNode name;
    Vector<ParameterNode> parameters;
    BlockExprNode body; // can be null if it's a function declaration
    TypeExprNode returnType; // can be null
    SelfParaNode selfPara; // can be null
    
    // 存储该函数对应的符号
    private Symbol symbol;
    
    // set isConst to false initially
    FunctionNode() {
        isConst = false;
    }
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}

// SelfParaNode represents the self parameter in a method <selfpara>.
// The grammer for self parameter is:
// <selfpara> = (<shortself> | <typedself>)
// <shortself> = &? (mut)? self
// <typedself> = (mut)? self : <type>
class SelfParaNode extends ASTNode {
    boolean isMutable;
    boolean isReference;
    TypeExprNode type; // can be null
    
    // 存储该self参数对应的符号
    private Symbol symbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}

// ParameterNode represents a parameter in a function <parameter>.
// The grammer for parameter is:
// <parameter> = <pattern> : <type>
class ParameterNode extends ASTNode {
    PatternNode name;
    TypeExprNode type;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// PatternNode represents a pattern <pattern>.
// The grammer for pattern is:
// <pattern> = <idpat> | <wildpat> | <refpat>
class PatternNode extends ASTNode {
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// IdPatNode represents an identifier pattern <idpat>.
// The grammer for identifier pattern is:
// <idpat> = (ref)? (mut)? <identifier>
class IdPatNode extends PatternNode {
    boolean isReference;
    boolean isMutable;
    IdentifierNode name;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// WildPatNode represents a wildcard pattern <wildpat>.
// The grammer for wildcard pattern is:
// <wildpat> = _
class WildPatNode extends PatternNode {
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// RefPatNode represents a reference pattern <refpat>.
// The grammer for reference pattern is:
// <refpat> = (& | &&) (mut)? <pattern>
class RefPatNode extends PatternNode {
    boolean isMutable;
    boolean isDoubleReference;
    PatternNode innerPattern;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// StructNode represents a struct item <structitem>.
// The grammer for struct definition is:
// structitem = struct <identifier> ({ <fields>? } | ;)
// <fields> = <field> (, <field>)* ,?
class StructNode extends ItemNode {
    IdentifierNode name;
    Vector<FieldNode> fields;
    
    // 存储该结构体对应的符号
    private Symbol symbol;
    
    // 存储该结构体的构造函数符号
    private Symbol constructorSymbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
    
    public Symbol getConstructorSymbol() {
        return constructorSymbol;
    }
    
    public void setConstructorSymbol(Symbol constructorSymbol) {
        this.constructorSymbol = constructorSymbol;
    }
}

// FieldNode represents a field in a struct <field>.
// The grammer for field definition is:
// <field> = <identifier> : <type> ;
class FieldNode extends ItemNode {
    IdentifierNode name;
    TypeExprNode type;
    
    // 存储该字段节点对应的符号
    private Symbol symbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}

// EnumNode represents an enum item <enumitem>.
// The grammer for enum definition is:
// <enumitem> = enum <identifier> { <enum_variants>? }
// <enum_variants> = <identifier> (, <identifier>)* ,?
class EnumNode extends ItemNode {
    IdentifierNode name;
    Vector<IdentifierNode> variants;
    
    // 存储该枚举对应的符号
    private Symbol symbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}

// ConstItemNode represents a constant item <constitem>.
// The grammer for constant definition is:
// <constitem> = const <identifier> : <type> (= <expression>)? ;
class ConstItemNode extends ItemNode {
    IdentifierNode name;
    TypeExprNode type;
    ExprNode value; // can be null
    
    // 存储该常量对应的符号
    private Symbol symbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}

// TraitNode represents a trait item <traititem>.
// The grammer for trait definition is:
// <traititem> = trait <identifier> { <asso_item>* }
class TraitNode extends ItemNode {
    IdentifierNode name;
    Vector<AssoItemNode> items;
    
    // 存储该trait对应的符号
    private Symbol symbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}

// AssoItemNode represents an associated item <asso_item>.
// there are two kinds of associated items: function and constant. so <asso_item> = <function> | <constitem>.
class AssoItemNode extends ItemNode {
    // can be FunctionNode or ConstItemNode
    FunctionNode function;
    ConstItemNode constant;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// ImplNode represents an impl item <implitem>.
// there are two kinds of impl: inherent impl and trait impl. so <implitem> = <inherentimplitem> | <traitimplitem>.
// The grammer for two kinds of impl definition is:
// <inherentimplitem> = impl <type> { <asso_item>* }
// <traitimplitem> = impl <identifier> for <type> { <asso_item>* }
class ImplNode extends ItemNode {
    IdentifierNode trait; // can be null if it's an inherent impl
    TypeExprNode typeName;
    Vector<AssoItemNode> items;
    
    // 存储该impl节点对应的trait符号和type符号
    private Symbol traitSymbol;
    private Symbol typeSymbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getTraitSymbol() {
        return traitSymbol;
    }
    
    public void setTraitSymbol(Symbol traitSymbol) {
        this.traitSymbol = traitSymbol;
    }
    
    public Symbol getTypeSymbol() {
        return typeSymbol;
    }
    
    public void setTypeSymbol(Symbol typeSymbol) {
        this.typeSymbol = typeSymbol;
    }
}

// ExprNode represents an expression <expression>.
// The grammer for expression is:
// <expression> = <exprwithblock> | <exprwithoutblock>
class ExprNode extends ASTNode {
    // 存储该表达式节点对应的上下文
    private Context context;
    
    // 存储该表达式节点的类型
    private Type type;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Context getContext() {
        return context;
    }
    
    public void setContext(Context context) {
        this.context = context;
    }
    
    public Type getType() {
        if (type == null) {
            throw new RuntimeException("Type of expression node is not set yet.");
        }
        return type;
    }
    
    public void setType(Type type) {
        this.type = type;
    }
}

// ExprWithBlockNode represents an expression with block <exprwithblock>.
// the grammer for expression with block is:
// <exprwithblock> = <blockexpr> | <ifexpr> | <loopexpr>
class ExprWithBlockNode extends ExprNode {
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// ExprWithoutBlockNode represents an expression without block <exprwithoutblock>.
// the grammer for expression without block is:
// <exprwithoutblock> = <literalexpr> | <pathexpr> | <operexpr> | <arrayexpr> | <indexexpr> | <structexpr> | <callexpr> | <methodcallexpr> | <fieldexpr> | <continueexpr> | <breakexpr> | <returnexpr> | <underscoreexpr> | <groupedexpr>
// among them, <groupedexpr> represents an expression in parentheses, which is just an expression itself, so we don't need a separate node for it. the grammer for grouped expression is:
// <groupedexpr> = ( <expression> )
class ExprWithoutBlockNode extends ExprNode {
    ExprNode expr;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// LiteralExprNode represents a literal expression <literalexpr>.
// a literal has several types: char_literal, string_literal, raw_string_literal, c_string_literal, raw_c_string_literal, integer_literal, boolean_literal. These literal already exist in the token level, so we just need to store their type and value here.
// the literalType can be one of the following: CHAR, STRING, CSTRING, INT_I32, INT_U32, INT_USIZE, INT_ISIZE, BOOL.
class LiteralExprNode extends ExprWithoutBlockNode {
    literal_t literalType;
    String value_string; // for string and char literal
    long value_long; // for all integer literals (INT_I32, INT_U32, INT_USIZE, INT_ISIZE)
    boolean value_bool; // for boolean literal
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}



// PathExprNode represents a path expression <pathexpr>.
// the grammer for path expression is:
// <pathexpr> = <pathseg> (:: <pathseg>)?
class PathExprNode extends ExprWithoutBlockNode {
    PathExprSegNode LSeg;
    PathExprSegNode RSeg; // can be null
    
    // 存储该路径表达式对应的符号
    private Symbol symbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}
// PathExprSegNode represents a segment in a path expression <pathseg>.
// the grammer for path segment is:
// <pathseg> = <identifier> | self | Self
// patternType can be one of the following: IDENT, SELF, SELF_TYPE.
class PathExprSegNode extends ASTNode {
    patternSeg_t patternType;
    IdentifierNode name;
    
    // 存储该路径段对应的符号
    private Symbol symbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}

// GroupExprNode represents a grouped expression <groupedexpr>.
// the grammer for grouped expression is:
// <groupedexpr> = ( <expression> )
class GroupExprNode extends ExprWithoutBlockNode {
    ExprNode innerExpr;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// OperExprNode represents an operator expression <operexpr>.
// the grammer for operator expression is:
// <operexpr> = <borrowexpr> | <derefexpr> | <negaexpr> | <arithexpr> | <compexpr> | <lazyexpr> | <typecastexpr> | <assignexpr> | <comassignexpr>
abstract class OperExprNode extends ExprWithoutBlockNode {
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// BorrowExprNode represents a borrow expression <borrowexpr>.
// the grammer for borrow expression is:
// <borrowexpr> = (& | &&) (mut)? <expression>
class BorrowExprNode extends OperExprNode {
    boolean isMutable; // true if it's mut, false if it's not
    boolean isDoubleReference; // true if it's &&, false if it's &
    ExprNode innerExpr;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// DerefExprNode represents a dereference expression <derefexpr>.
// the grammer for dereference expression is:
// <derefexpr> = * <expression>
class DerefExprNode extends OperExprNode {
    ExprNode innerExpr;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// NegaExprNode represents a negation expression <negaexpr>.
// the grammer for negation expression is:
// <negaexpr> = (! | -) <expression>
class NegaExprNode extends OperExprNode {
    boolean isLogical; // true if it's !, false if it's -
    ExprNode innerExpr;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// ArithExprNode represents an arithmetic expression <arithexpr>.
// the grammer for arithmetic expression is:
// <arithexpr> = <expression> (+ | - | * | / | % | & | | | ^ | << | >>) <expression>
class ArithExprNode extends OperExprNode {
    oper_t operator;
    ExprNode left;
    ExprNode right;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// CompExprNode represents a comparison expression <compexpr>.
// the grammer for comparison expression is:
// <compexpr> = <expression> (== | != | > | < | >= | <=) <expression>
class CompExprNode extends OperExprNode {
    oper_t operator;
    ExprNode left;
    ExprNode right;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// LazyExprNode represents a lazy expression <lazyexpr>.
// the grammer for lazy expression is:
// <lazyexpr> = <expression> (&& | ||) <expression>
class LazyExprNode extends OperExprNode {
    oper_t operator;
    ExprNode left;
    ExprNode right;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// TypeCastExprNode represents a type cast expression <typecastexpr>.
// the grammer for type cast expression is:
// <typecastexpr> = <expression> as <type>
class TypeCastExprNode extends OperExprNode {
    ExprNode expr;
    TypeExprNode type;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// AssignExprNode represents an assignment expression <assignexpr>.
// the grammer for assignment expression is:
// <assignexpr> = <expression> = <expression>
class AssignExprNode extends OperExprNode {
    oper_t operator; // always ASSIGN
    ExprNode left;
    ExprNode right;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// ComAssignExprNode represents a compound assignment expression <comassignexpr>.
// the grammer for compound assignment expression is:
// <comassignexpr> = <expression> (+= | -= | *= | /= | %= | &= | |= | ^= | <<= | >>=) <expression>
class ComAssignExprNode extends OperExprNode {
    oper_t operator;
    ExprNode left;
    ExprNode right;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}
// the oper_t enum is defined here.

// ArrayExprNode represents an array expression <arrayexpr>.
// the grammer for array expression is:
// <arrayexpr> = [ (<elements> | <repeated_element>; <size>)? ]
// <elements> = <expression> (, <expression>)* ,?
// <repeated_element> = <expression>
// <size> = <expression>

class ArrayExprNode extends ExprWithoutBlockNode {
    Vector<ExprNode> elements; // can be null
    ExprNode repeatedElement; // can be null
    ExprNode size; // can be null
    // if elements is not null, then repeatedElement and size must be null
    // if repeatedElement and size are not null, then elements must be null
    // if all of them are null, then it's an empty array
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// IndexExprNode represents an index expression <indexexpr>.
// the grammer for index expression is:
// <indexexpr> = <expression> [ <expression> ]
class IndexExprNode extends ExprWithoutBlockNode {
    ExprNode array;
    ExprNode index;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}


// StructExprNode represents a struct expression <structexpr>.
// the grammer for struct expression is:
// <structexpr> = <pathseg> { <fieldvals>? }
// <fieldvals> = <fieldval> (, <fieldval>)* ,?
// <fieldval> = <identifier> : <expression>
class StructExprNode extends ExprWithoutBlockNode {
    PathExprSegNode structName;
    Vector<FieldValNode> fieldValues; // can be null
    
    // 存储该结构体表达式对应的符号
    private Symbol symbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}
class FieldValNode extends ASTNode {
    IdentifierNode fieldName;
    ExprNode value;
    
    // 存储该字段值节点对应的符号
    private Symbol symbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}

// CallExprNode represents a function call expression <callexpr>.
// the grammer for function call expression is:
// <callexpr> = <expression> ( <arguments>? )
// <arguments> = <expression> (, <expression>)* ,?
class CallExprNode extends ExprWithoutBlockNode {
    ExprNode function;
    Vector<ExprNode> arguments; // can be null
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// MethodCallExprNode represents a method call expression <methodcallexpr>.
// the grammer for method call expression is:
// <methodcallexpr> = <expression> . <pathseg> ( <arguments>? )
// <arguments> = <expression> (, <expression>)* ,?
class MethodCallExprNode extends ExprWithoutBlockNode {
    ExprNode receiver;
    PathExprSegNode methodName;
    Vector<ExprNode> arguments; // can be null
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// FieldExprNode represents a field access expression <fieldexpr>.
// the grammer for field access expression is:
// <fieldexpr> = <expression> . <identifier>
class FieldExprNode extends ExprWithoutBlockNode {
    ExprNode receiver;
    IdentifierNode fieldName;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// ContinueExprNode represents a continue expression <continueexpr>.
// the grammer for continue expression is:
// <continueexpr> = continue
class ContinueExprNode extends ExprWithoutBlockNode {
    // 存储目标循环AST节点
    private ASTNode targetNode;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public ASTNode getTargetNode() {
        return targetNode;
    }
    
    public void setTargetNode(ASTNode targetNode) {
        this.targetNode = targetNode;
    }
}
// BreakExprNode represents a break expression <breakexpr>.
// the grammer for break expression is:
// <breakexpr> = break (<expression>)?
class BreakExprNode extends ExprWithoutBlockNode {
    ExprNode value; // can be null
    
    // 存储目标循环AST节点
    private ASTNode targetNode;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public ASTNode getTargetNode() {
        return targetNode;
    }
    
    public void setTargetNode(ASTNode targetNode) {
        this.targetNode = targetNode;
    }
}
// ReturnExprNode represents a return expression <returnexpr>.
// the grammer for return expression is:
// <returnexpr> = return (<expression>)?
class ReturnExprNode extends ExprWithoutBlockNode {
    ExprNode value; // can be null
    
    // 存储目标函数AST节点
    private ASTNode targetNode;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public ASTNode getTargetNode() {
        return targetNode;
    }
    
    public void setTargetNode(ASTNode targetNode) {
        this.targetNode = targetNode;
    }
}
// UnderscoreExprNode represents an underscore expression <underscoreexpr>.
// the grammer for underscore expression is:
// <underscoreexpr> = _
class UnderscoreExprNode extends ExprWithoutBlockNode {
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// BlockExprNode represents a block expression <blockexpr>.
// the grammer for block expression is:
// <blockexpr> = { <statements>* }
class BlockExprNode extends ExprWithBlockNode {
    Vector<StmtNode> statements;
    ExprNode returnValue; // 表示块表达式的返回值
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// IfExprNode represents an if expression <ifexpr>.
// the grammer for if expression is:
// <ifexpr> = if <expression except structexpr> <blockexpr> (else (<ifexpr> | <blockexpr>))?
class IfExprNode extends ExprWithBlockNode {
    ExprNode condition;
    BlockExprNode thenBranch;
    ExprWithBlockNode elseBranch; // can be null
    IfExprNode elseifBranch; // can be null
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// LoopExprNode represents a loop expression <loopexpr>.
// the grammer for loop expression is:
// <loopexpr> = <infinite_loop> | <conditional_loop>
// <infinite_loop> = loop <blockexpr>
// <conditional_loop> = while <expression except structexpr> <blockexpr>
class LoopExprNode extends ExprWithBlockNode {
    boolean isInfinite; // true if it's an infinite loop, false if it's a conditional loop
    ExprNode condition; // can be null if it's an infinite loop
    BlockExprNode body;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// IdentifierNode represents an identifier <identifier>.
// an identifier is just a string, so we just need to store the string here.
class IdentifierNode extends ASTNode {
    String name;
    
    // 存储该标识符对应的符号
    private Symbol symbol;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
    
    public Symbol getSymbol() {
        return symbol;
    }
    
    public void setSymbol(Symbol symbol) {
        this.symbol = symbol;
    }
}


// TypeExprNode represents a type expression <type>.
// the grammer for type expression is:
// <type> = <typepathexpr> | <typerefexpr> | <typearrayexpr> | <typeunitexpr>
class TypeExprNode extends ASTNode {
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// TypePathExprNode represents a path type expression <typepathexpr>.
// the grammer for path type expression is:
// <typepathexr> = <pathseg>
class TypePathExprNode extends TypeExprNode {
    PathExprSegNode path;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// TypeRefExprNode represents a reference type expression <typerefexpr>.
// the grammer for reference type expression is:
// <typerefexpr> = & (mut)? <type>
class TypeRefExprNode extends TypeExprNode {
    boolean isMutable;
    TypeExprNode innerType;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// TypeArrayExprNode represents an array type expression <typearrayexpr>.
// the grammer for array type expression is:
// <typearrayexpr> = [ <type> ; <expression> ]
class TypeArrayExprNode extends TypeExprNode {
    TypeExprNode elementType;
    ExprNode size;
    
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// TypeUnitExprNode represents a unit type expression <typeunitexpr>.
// the grammer for unit type expression is:
// <typeunitexpr> = ()
class TypeUnitExprNode extends TypeExprNode {
    public void accept(VisitorBase visitor) {
        visitor.visit(this);
    }
}

// BuiltinFunctionNode represents a builtin function or method.
// This node is used for builtin functions like print, println, etc.
// and builtin methods like to_string, as_str, len, etc.
// It inherits from FunctionNode to maintain consistency with regular functions.
class BuiltinFunctionNode extends FunctionNode {
    
    // Enum to distinguish between builtin functions and methods
    public enum BuiltinType {
        FUNCTION,
        METHOD
    }
    
    private BuiltinType builtinType;
    
    public BuiltinFunctionNode(String name) {
        this(name, BuiltinType.FUNCTION);
    }
    
    public BuiltinFunctionNode(String name, BuiltinType type) {
        // Set the name using the parent class field
        this.name = new IdentifierNode();
        this.name.name = name;
        
        // Store the builtin type
        this.builtinType = type;
        
        // Builtin functions are always const
        this.isConst = true;
        
        // Initialize with default values
        this.parameters = new Vector<>();
        this.body = null;
        this.returnType = null;
        this.selfPara = null;
        
        // Configure the specific builtin function or method
        configureBuiltinFunction(name, type);
    }
    
    private void configureBuiltinFunction(String name, BuiltinType type) {
        if (type == BuiltinType.FUNCTION) {
            switch (name) {
                case "print":
                    configurePrint();
                    break;
                case "println":
                    configurePrintln();
                    break;
                case "printInt":
                    configurePrintInt();
                    break;
                case "printlnInt":
                    configurePrintlnInt();
                    break;
                case "getString":
                    configureGetString();
                    break;
                case "getInt":
                    configureGetInt();
                    break;
                case "exit":
                    configureExit();
                    break;
                default:
                    // Unknown builtin function, keep default values
                    break;
            }
        } else if (type == BuiltinType.METHOD) {
            switch (name) {
                case "to_string":
                    configureToString();
                    break;
                case "as_str":
                    configureAsStr();
                    break;
                case "as_mut_str":
                    configureAsMutStr();
                    break;
                case "len":
                    configureLen();
                    break;
                default:
                    // Unknown builtin method, keep default values
                    break;
            }
        }
    }
    
    private void configurePrint() {
        // print(s: &str) -> ()
        ParameterNode param = new ParameterNode();
        param.name = new IdPatNode();
        ((IdPatNode) param.name).name = new IdentifierNode();
        ((IdPatNode) param.name).name.name = "s";
        
        param.type = new TypeRefExprNode();
        ((TypeRefExprNode) param.type).isMutable = false;
        ((TypeRefExprNode) param.type).innerType = new TypePathExprNode();
        ((TypeRefExprNode) param.type).innerType = new TypePathExprNode();
        ((TypePathExprNode) ((TypeRefExprNode) param.type).innerType).path = new PathExprSegNode();
        ((TypePathExprNode) ((TypeRefExprNode) param.type).innerType).path.patternType = patternSeg_t.IDENT;
        ((TypePathExprNode) ((TypeRefExprNode) param.type).innerType).path.name = new IdentifierNode();
        ((TypePathExprNode) ((TypeRefExprNode) param.type).innerType).path.name.name = "str";
        
        this.parameters.add(param);
        
        // Return type is ()
        this.returnType = new TypeUnitExprNode();
    }
    
    private void configurePrintln() {
        // println(s: &str) -> ()
        configurePrint(); // Same as print
    }
    
    private void configurePrintInt() {
        // printInt(n: i32) -> ()
        ParameterNode param = new ParameterNode();
        param.name = new IdPatNode();
        ((IdPatNode) param.name).name = new IdentifierNode();
        ((IdPatNode) param.name).name.name = "n";
        
        param.type = new TypePathExprNode();
        ((TypePathExprNode) param.type).path = new PathExprSegNode();
        ((TypePathExprNode) param.type).path.patternType = patternSeg_t.IDENT;
        ((TypePathExprNode) param.type).path.name = new IdentifierNode();
        ((TypePathExprNode) param.type).path.name.name = "i32";
        
        this.parameters.add(param);
        
        // Return type is ()
        this.returnType = new TypeUnitExprNode();
    }
    
    private void configurePrintlnInt() {
        // printlnInt(n: i32) -> ()
        configurePrintInt(); // Same as printInt
    }
    
    private void configureGetString() {
        // getString() -> String
        // No parameters
        
        // Return type is String
        this.returnType = new TypePathExprNode();
        ((TypePathExprNode) this.returnType).path = new PathExprSegNode();
        ((TypePathExprNode) this.returnType).path.patternType = patternSeg_t.IDENT;
        ((TypePathExprNode) this.returnType).path.name = new IdentifierNode();
        ((TypePathExprNode) this.returnType).path.name.name = "String";
    }
    
    private void configureGetInt() {
        // getInt() -> i32
        // No parameters
        
        // Return type is i32
        this.returnType = new TypePathExprNode();
        ((TypePathExprNode) this.returnType).path = new PathExprSegNode();
        ((TypePathExprNode) this.returnType).path.patternType = patternSeg_t.IDENT;
        ((TypePathExprNode) this.returnType).path.name = new IdentifierNode();
        ((TypePathExprNode) this.returnType).path.name.name = "i32";
    }
    
    private void configureExit() {
        // exit(code: i32) -> ()
        ParameterNode param = new ParameterNode();
        param.name = new IdPatNode();
        ((IdPatNode) param.name).name = new IdentifierNode();
        ((IdPatNode) param.name).name.name = "code";
        
        param.type = new TypePathExprNode();
        ((TypePathExprNode) param.type).path = new PathExprSegNode();
        ((TypePathExprNode) param.type).path.patternType = patternSeg_t.IDENT;
        ((TypePathExprNode) param.type).path.name = new IdentifierNode();
        ((TypePathExprNode) param.type).path.name.name = "i32";
        
        this.parameters.add(param);
        
        // Return type is ()
        this.returnType = new TypeUnitExprNode();
    }
    
    public String getName() {
        return name.name;
    }
    
    public BuiltinType getBuiltinType() {
        return builtinType;
    }
    
    // Configuration methods for builtin methods
    
    public void configureToString() {
        // to_string(&self) -> String
        configureSelfParameter(false, false);
        
        // Return type is String
        this.returnType = new TypePathExprNode();
        ((TypePathExprNode) this.returnType).path = new PathExprSegNode();
        ((TypePathExprNode) this.returnType).path.patternType = patternSeg_t.IDENT;
        ((TypePathExprNode) this.returnType).path.name = new IdentifierNode();
        ((TypePathExprNode) this.returnType).path.name.name = "String";
    }
    
    public void configureAsStr() {
        // as_str(&self) -> &str
        configureSelfParameter(false, false);
        
        // Return type is &str
        this.returnType = new TypeRefExprNode();
        ((TypeRefExprNode) this.returnType).isMutable = false;
        ((TypeRefExprNode) this.returnType).innerType = new TypePathExprNode();
        ((TypePathExprNode) ((TypeRefExprNode) this.returnType).innerType).path = new PathExprSegNode();
        ((TypePathExprNode) ((TypeRefExprNode) this.returnType).innerType).path.patternType = patternSeg_t.IDENT;
        ((TypePathExprNode) ((TypeRefExprNode) this.returnType).innerType).path.name = new IdentifierNode();
        ((TypePathExprNode) ((TypeRefExprNode) this.returnType).innerType).path.name.name = "str";
    }
    
    public void configureAsMutStr() {
        // as_mut_str(&mut self) -> &mut str
        configureSelfParameter(true, false);
        
        // Return type is &mut str
        this.returnType = new TypeRefExprNode();
        ((TypeRefExprNode) this.returnType).isMutable = true;
        ((TypeRefExprNode) this.returnType).innerType = new TypePathExprNode();
        ((TypePathExprNode) ((TypeRefExprNode) this.returnType).innerType).path = new PathExprSegNode();
        ((TypePathExprNode) ((TypeRefExprNode) this.returnType).innerType).path.patternType = patternSeg_t.IDENT;
        ((TypePathExprNode) ((TypeRefExprNode) this.returnType).innerType).path.name = new IdentifierNode();
        ((TypePathExprNode) ((TypeRefExprNode) this.returnType).innerType).path.name.name = "str";
    }
    
    public void configureLen() {
        // len(&self) -> usize
        configureSelfParameter(false, false);
        
        // Return type is usize
        this.returnType = new TypePathExprNode();
        ((TypePathExprNode) this.returnType).path = new PathExprSegNode();
        ((TypePathExprNode) this.returnType).path.patternType = patternSeg_t.IDENT;
        ((TypePathExprNode) this.returnType).path.name = new IdentifierNode();
        ((TypePathExprNode) this.returnType).path.name.name = "usize";
    }
    
    public void configureAppend() {
        // append(&mut self, s: &str) -> ()
        configureSelfParameter(true, false);
        
        // Add second parameter: s: &str
        ParameterNode param = new ParameterNode();
        param.name = new IdPatNode();
        ((IdPatNode) param.name).name = new IdentifierNode();
        ((IdPatNode) param.name).name.name = "s";
        
        param.type = new TypeRefExprNode();
        ((TypeRefExprNode) param.type).isMutable = false;
        ((TypeRefExprNode) param.type).innerType = new TypePathExprNode();
        ((TypePathExprNode) ((TypeRefExprNode) param.type).innerType).path = new PathExprSegNode();
        ((TypePathExprNode) ((TypeRefExprNode) param.type).innerType).path.patternType = patternSeg_t.IDENT;
        ((TypePathExprNode) ((TypeRefExprNode) param.type).innerType).path.name = new IdentifierNode();
        ((TypePathExprNode) ((TypeRefExprNode) param.type).innerType).path.name.name = "str";
        
        this.parameters.add(param);
        
        // Return type is ()
        this.returnType = new TypeUnitExprNode();
    }
    
    // Helper method to configure self parameter for methods
    private void configureSelfParameter(boolean isMutable, boolean isValue) {
        this.selfPara = new SelfParaNode();
        this.selfPara.isMutable = isMutable;
        this.selfPara.isReference = !isValue;
        
        if (isValue) {
            // self: Self
            this.selfPara.type = new TypePathExprNode();
            ((TypePathExprNode) this.selfPara.type).path = new PathExprSegNode();
            ((TypePathExprNode) this.selfPara.type).path.patternType = patternSeg_t.SELF_TYPE;
        } else {
            // No explicit type needed for &self or &mut self
            this.selfPara.type = null;
        }
    }
}
