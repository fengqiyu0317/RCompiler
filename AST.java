// use the tokens we get, construct the AST.

// ASTNode is the base class for all AST nodes.
abstract class ASTNode {
}

// StmtNode represents a statement <statement>.
// The grammer for statement is:
// <statement> = <item> | <letstmt> | <exprstmt> | ;
abstract class StmtNode extends ASTNode {
}
// ItemNode represents an item <item>.
// There are several kinds of items: function, struct, enum, constant, trait, impl. so <item> = <function> | <structitem> | <enumitem> | <constitem> | <traititem> | <implitem>.
abstract class ItemNode extends StmtNode {
}
// LetStmtNode represents a let statement <letstmt>.
// The grammer for let statement is:
// <letstmt> = let <pattern> : <type> (= <expression>)? ;
class LetStmtNode extends StmtNode {
    PatternNode name;
    TypeExprNode type;
    ExprNode value; // can be null
}

// ExprStmtNode represents an expression statement <exprstmt>.
// The grammer for expression statement is:
// <exprstmt> = <exprwithblock> ;? | <exprwithoutblock> ;
class ExprStmtNode extends StmtNode {
    ExprNode expr;
}

// FunctionNode represents a function item <function>.
// The grammer for function definition is:
// <function> = (const)? fn <identifier> (<parameters>?) (-> <type>)? ( <blockexpr> | ; )
// <parameters> = <selfpara> ,? | (<selfpara> ,)? <parameter> (, <parameter>)* ,?
class FunctionNode extends ItemNode {
    boolean isConst; // true if it's a const function, false if it's not
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

// SelfParaNode represents the self parameter in a method <selfpara>.
// The grammer for self parameter is:
// <selfpara> = (<shortself> | <typedself>)
// <shortself> = &? (mut)? self
// <typedself> = (mut)? self : <type>
class SelfParaNode extends ASTNode {
    boolean isMutable;
    boolean isReference;
    TypeExprNode type; // can be null
}

// ParameterNode represents a parameter in a function <parameter>.
// The grammer for parameter is:
// <parameter> = <pattern> : <type>
class ParameterNode extends ASTNode {
    PatternNode name;
    TypeExprNode type;
}

// PatternNode represents a pattern <pattern>.
// The grammer for pattern is:
// <pattern> = <idpat> | <wildpat> | <refpat>
class PatternNode extends ASTNode {
}
// IdPatNode represents an identifier pattern <idpat>.
// The grammer for identifier pattern is:
// <idpat> = (ref)? (mut)? <identifier>
class IdPatNode extends PatternNode {
    boolean isReference;
    IdentifierNode name;
}
// WildPatNode represents a wildcard pattern <wildpat>.
// The grammer for wildcard pattern is:
// <wildpat> = _
class WildPatNode extends PatternNode {
}
// RefPatNode represents a reference pattern <refpat>.
// The grammer for reference pattern is:
// <refpat> = (& | &&) (mut)? <pattern>
class RefPatNode extends PatternNode {
    boolean isMutable;
    PatternNode innerPattern;
}

// StructNode represents a struct item <structitem>.
// The grammer for struct definition is:
// structitem = struct <identifier> ({ <fields>? } | ;)
// <fields> = <field> (, <field>)* ,?
class StructNode extends ItemNode {
    IdentifierNode name;
    Vector<FieldNode> fields;
}

// FieldNode represents a field in a struct <field>.
// The grammer for field definition is:
// <field> = <identifier> : <type> ;
class FieldNode extends ItemNode {
    IdentifierNode name;
    TypeExprNode type;
}

// EnumNode represents an enum item <enumitem>.
// The grammer for enum definition is:
// <enumitem> = enum <identifier> { <enum_variants>? }
// <enum_variants> = <identifier> (, <identifier>)* ,?
class EnumNode extends ItemNode {
    IdentifierNode name;
    Vector<IdentifierNode> variants;
}

// ConstItemNode represents a constant item <constitem>.
// The grammer for constant definition is:
// <constitem> = const <identifier> : <type> (= <expression>)? ;
class ConstItemNode extends ItemNode {
    IdentifierNode name;
    TypeExprNode type;
    ExprNode value; // can be null
}

// TraitNode represents a trait item <traititem>.
// The grammer for trait definition is:
// <traititem> = trait <identifier> { <asso_item>* }
class TraitNode extends ItemNode {
    IdentifierNode name;
    Vector<AssoItemNode> items;
}

// AssoItemNode represents an associated item <asso_item>.
// there are two kinds of associated items: function and constant. so <asso_item> = <function> | <constitem>.
class AssoItemNode extends ItemNode {
    // can be FunctionNode or ConstItemNode
    FunctionNode function;
    ConstItemNode constant;
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
}

// ExprNode represents an expression <expression>.
// The grammer for expression is:
// <expression> = <exprwithblock> | <exprwithoutblock>
abstract class ExprNode extends ASTNode {
}

// ExprWithBlockNode represents an expression with block <exprwithblock>.
// the grammer for expression with block is:
// <exprwithblock> = <blockexpr> | <ifexpr> | <loopexpr>
abstract class ExprWithBlockNode extends ExprNode {
}

// ExprWithoutBlockNode represents an expression without block <exprwithoutblock>.
// the grammer for expression without block is:
// <exprwithoutblock> = <literalexpr> | <pathexpr> | <operexpr> | <arrayexpr> | <indexexpr> | <structexpr> | <callexpr> | <methodcallexpr> | <fieldexpr> | <continueexpr> | <breakexpr> | <returnexpr> | <underscoreexpr> | <groupedexpr>
// among them, <groupedexpr> represents an expression in parentheses, which is just an expression itself, so we don't need a separate node for it. the grammer for grouped expression is:
// <groupedexpr> = ( <expression> )
abstract class ExprWithoutBlockNode extends ExprNode {
}
// LiteralExprNode represents a literal expression <literalexpr>.
// a literal has several types: char_literal, string_literal, raw_string_literal, c_string_literal, raw_c_string_literal, integer_literal, boolean_literal. These literal already exist in the token level, so we just need to store their type and value here. 
// the literalType can be one of the following: CHAR, STRING, CSTRING, INT, BOOL.
class LiteralExprNode extends ExprWithoutBlockNode {
    type_t literalType;
    String value_string; // for string and char literal
    int value_int; // for integer literal
    boolean value_bool; // for boolean literal
}

// the type_t enum is defined here.
enum type_t {
    CHAR,
    STRING,
    CSTRING,
    INT,
    BOOL
}

// PathExprNode represents a path expression <pathexpr>.
// the grammer for path expression is:
// <pathexpr> = <pathseg> (:: <pathseg>)?
class PathExprNode extends ExprWithoutBlockNode {
    PathExprSegNode LSeg;
    PathExprSegNode RSeg; // can be null
}
// PathExprSegNode represents a segment in a path expression <pathseg>.
// the grammer for path segment is:
// <pathseg> = <identifier> | self | Self
// patternType can be one of the following: IDENT, SELF, SELF_TYPE.
enum patternSeg_t {
    IDENT,
    SELF,
    SELF_TYPE
}
class PathExprSegNode extends ASTNode {
    patternSeg_t patternType;
    IdentifierNode name;
}

// OperExprNode represents an operator expression <operexpr>.
// the grammer for operator expression is:
// <operexpr> = <borrowexpr> | <derefexpr> | <negaexpr> | <arithexpr> | <compexpr> | <lazyexpr> | <typecastexpr> | <assignexpr> | <comassignexpr>
abstract class OperExprNode extends ExprWithoutBlockNode {
}
// BorrowExprNode represents a borrow expression <borrowexpr>.
// the grammer for borrow expression is:
// <borrowexpr> = (& | &&) (mut)? <expression>
class BorrowExprNode extends OperExprNode {
    boolean isMutable; // true if it's mut, false if it's not
    boolean isDouble; // true if it's &&, false if it's &
    ExprNode innerExpr;
}
// DerefExprNode represents a dereference expression <derefexpr>.
// the grammer for dereference expression is:
// <derefexpr> = * <expression>
class DerefExprNode extends OperExprNode {
    ExprNode innerExpr;
}
// NegaExprNode represents a negation expression <negaexpr>.
// the grammer for negation expression is:
// <negaexpr> = (! | -) <expression>
class NegaExprNode extends OperExprNode {
    boolean isLogical; // true if it's !, false if it's -
    ExprNode innerExpr;
}
// ArithExprNode represents an arithmetic expression <arithexpr>.
// the grammer for arithmetic expression is:
// <arithexpr> = <expression> (+ | - | * | / | % | & | | | ^ | << | >>) <expression>
class ArithExprNode extends OperExprNode {
    oper_t operator;
    ExprNode left;
    ExprNode right;
}
// CompExprNode represents a comparison expression <compexpr>.
// the grammer for comparison expression is:
// <compexpr> = <expression> (== | != | > | < | >= | <=) <expression>
class CompExprNode extends OperExprNode {
    oper_t operator; 
    ExprNode left; 
    ExprNode right; 
}
// LazyExprNode represents a lazy expression <lazyexpr>.
// the grammer for lazy expression is:
// <lazyexpr> = <expression> (&& | ||) <expression>
class LazyExprNode extends OperExprNode {
    oper_t operator; 
    ExprNode left; 
    ExprNode right; 
}
// TypeCastExprNode represents a type cast expression <typecastexpr>.
// the grammer for type cast expression is:
// <typecastexpr> = <expression> as <type>
class TypeCastExprNode extends OperExprNode {
    ExprNode expr;
    TypeExprNode type;
}
// AssignExprNode represents an assignment expression <assignexpr>.
// the grammer for assignment expression is:
// <assignexpr> = <expression> = <expression>
class AssignExprNode extends OperExprNode {
    ExprNode left;
    ExprNode right;
}
// ComAssignExprNode represents a compound assignment expression <comassignexpr>.
// the grammer for compound assignment expression is:
// <comassignexpr> = <expression> (+= | -= | *= | /= | %= | &= | |= | ^= | <<= | >>=) <expression>
class ComAssignExprNode extends OperExprNode {
    oper_t operator; 
    ExprNode left; 
    ExprNode right; 
}
// the oper_t enum is defined here.
enum oper_t {
    ADD, // +
    SUB, // -
    MUL, // *
    DIV, // /
    MOD, // %
    BITAND, // &
    BITOR, // |
    BITXOR, // ^
    SHL, // <<
    SHR, // >>
    EQ, // ==
    NEQ, // !=
    GT, // >
    LT, // <
    GTE, // >=
    LTE, // <=
    LOGAND, // &&
    LOGOR, // ||
    ASSIGN, // =
    ADD_ASSIGN, // +=
    SUB_ASSIGN, // -=
    MUL_ASSIGN, // *=
    DIV_ASSIGN, // /=
    MOD_ASSIGN, // %=
    BITAND_ASSIGN, // &=
    BITOR_ASSIGN, // |=
    BITXOR_ASSIGN, // ^=
    SHL_ASSIGN, // <<=
    SHR_ASSIGN // >>=
}

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
}

// IndexExprNode represents an index expression <indexexpr>.
// the grammer for index expression is:
// <indexexpr> = <expression> [ <expression> ]
class IndexExprNode extends ExprWithoutBlockNode {
    ExprNode array;
    ExprNode index;
}


// StructExprNode represents a struct expression <structexpr>.
// the grammer for struct expression is:
// <structexpr> = <pathseg> { <fieldvals>? }
// <fieldvals> = <fieldval> (, <fieldval>)* ,?
// <fieldval> = <identifier> : <expression>
class StructExprNode extends ExprWithoutBlockNode {
    PathExprSegNode structName;
    Vector<FieldValNode> fieldValues; // can be null
}
class FieldValNode extends ASTNode {
    IdentifierNode fieldName;
    ExprNode value;
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
    ExprStmtNode value; // can be null
}
class ReturnExprNode extends ExprWithoutBlockNode {
    ExprStmtNode value; // can be null
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
}


class TypePathExprNode extends TypeExprNode {
    PathExprSegNode path;
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