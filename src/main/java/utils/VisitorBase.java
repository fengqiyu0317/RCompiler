// this is a base class for visitors that can visit different types of AST nodes
// it provides default implementations that traverse all child nodes

public abstract class VisitorBase {
    // Helper method to report null errors
    protected void reportNullError(String nodeName, String fieldName) {
        throw new RuntimeException("Error: " + nodeName + "." + fieldName + " should not be null according to grammar rules");
    }
    
    // visit a generic AST node
    public void visit(ASTNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(StmtNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(ItemNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(LetStmtNode node) {
        // According to grammar: <letstmt> ::= let <pattern> : <type> (= <expression>)? ;
        // name and type are required, value is optional
        if (node.name == null) reportNullError("LetStmtNode", "name");
        else node.name.accept(this);
        
        if (node.type == null) reportNullError("LetStmtNode", "type");
        else node.type.accept(this);
        
        if (node.value != null) node.value.accept(this);
    }
    
    public void visit(ExprStmtNode node) {
        // According to grammar: <exprstmt> ::= <exprwithblock> ;? | <exprwithoutblock> ;
        // expr is required
        if (node.expr == null) reportNullError("ExprStmtNode", "expr");
        else node.expr.accept(this);
    }
    
    public void visit(FunctionNode node) {
        // According to grammar: <function> ::= (const)? fn <identifier> (<parameters>?) (-> <type>)? ( <blockexpr> | ; )
        // name is required, others are optional
        if (node.name == null) reportNullError("FunctionNode", "name");
        else node.name.accept(this);
        
        if (node.selfPara != null) node.selfPara.accept(this);
        
        if (node.parameters != null) {
            for (int i = 0; i < node.parameters.size(); i++) {
                node.parameters.get(i).accept(this);
            }
        }
        
        if (node.returnType != null) node.returnType.accept(this);
        if (node.body != null) node.body.accept(this);
    }
    
    public void visit(SelfParaNode node) {
        // According to grammar: <selfpara> ::= <shortself> | <typedself>
        // type is optional (only for typedself)
        if (node.type != null) node.type.accept(this);
    }
    
    public void visit(ParameterNode node) {
        // According to grammar: <parameter> ::= <pattern> : <type>
        // name and type are required
        if (node.name == null) reportNullError("ParameterNode", "name");
        else node.name.accept(this);
        
        if (node.type == null) reportNullError("ParameterNode", "type");
        else node.type.accept(this);
    }
    
    public void visit(PatternNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(IdPatNode node) {
        // According to grammar: <idpat> ::= (ref)? (mut)? <identifier>
        // name is required
        if (node.name == null) reportNullError("IdPatNode", "name");
        else node.name.accept(this);
    }
    
    public void visit(WildPatNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(RefPatNode node) {
        // According to grammar: <refpat> ::= (& | &&) (mut)? <pattern>
        // innerPattern is required
        if (node.innerPattern == null) reportNullError("RefPatNode", "innerPattern");
        else node.innerPattern.accept(this);
    }
    
    public void visit(StructNode node) {
        // According to grammar: <structitem> ::= struct <identifier> ({ <fields>? } | ;)
        // name is required, fields are optional
        if (node.name == null) reportNullError("StructNode", "name");
        else node.name.accept(this);
        
        if (node.fields != null) {
            for (int i = 0; i < node.fields.size(); i++) {
                node.fields.get(i).accept(this);
            }
        }
    }
    
    public void visit(FieldNode node) {
        // According to grammar: <field> ::= <identifier> : <type> ;
        // name and type are required
        if (node.name == null) reportNullError("FieldNode", "name");
        else node.name.accept(this);
        
        if (node.type == null) reportNullError("FieldNode", "type");
        else node.type.accept(this);
    }
    
    public void visit(EnumNode node) {
        // According to grammar: <enumitem> ::= enum <identifier> { <enum_variants>? }
        // name is required, variants are optional
        if (node.name == null) reportNullError("EnumNode", "name");
        else node.name.accept(this);
        
        if (node.variants != null) {
            for (int i = 0; i < node.variants.size(); i++) {
                node.variants.get(i).accept(this);
            }
        }
    }
    
    public void visit(ConstItemNode node) {
        // According to grammar: <constitem> ::= const <identifier> : <type> (= <expression>)? ;
        // name and type are required, value is optional
        if (node.name == null) reportNullError("ConstItemNode", "name");
        else node.name.accept(this);
        
        if (node.type == null) reportNullError("ConstItemNode", "type");
        else node.type.accept(this);
        
        if (node.value != null) node.value.accept(this);
    }
    
    public void visit(TraitNode node) {
        // According to grammar: <traititem> ::= trait <identifier> { <asso_item>* }
        // name is required, items can be empty
        if (node.name == null) reportNullError("TraitNode", "name");
        else node.name.accept(this);
        
        if (node.items != null) {
            for (int i = 0; i < node.items.size(); i++) {
                node.items.get(i).accept(this);
            }
        }
    }
    
    public void visit(ImplNode node) {
        // According to grammar: <inherentimplitem> ::= impl <type> { <asso_item>* }
        // <traitimplitem> ::= impl <identifier> for <type> { <asso_item>* }
        // typeName is required, trait is optional (only for trait impl)
        if (node.typeName == null) reportNullError("ImplNode", "typeName");
        else node.typeName.accept(this);
        
        if (node.trait != null) node.trait.accept(this);
        
        if (node.items != null) {
            for (int i = 0; i < node.items.size(); i++) {
                node.items.get(i).accept(this);
            }
        }
    }
    
    public void visit(AssoItemNode node) {
        // According to grammar: <asso_item> ::= <function> | <constitem>
        // Either function or constant should be not null
        if (node.function != null) {
            node.function.accept(this);
        } else if (node.constant != null) {
            node.constant.accept(this);
        } else {
            reportNullError("AssoItemNode", "function or constant");
        }
    }
    
    public void visit(ExprNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(ExprWithBlockNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(ExprWithoutBlockNode node) {
        // According to grammar: <groupedexpr> ::= ( <expression> )
        // expr is required
        if (node.expr == null) reportNullError("ExprWithoutBlockNode", "expr");
        else node.expr.accept(this);
    }
    
    public void visit(BlockExprNode node) {
        // According to grammar: <blockexpr> ::= { <statements>? }
        // statements can be empty
        if (node.statements != null) {
            for (int i = 0; i < node.statements.size(); i++) {
                node.statements.get(i).accept(this);
            }
        }
    }
    
    public void visit(IfExprNode node) {
        // According to grammar: <ifexpr> ::= if <expression except structexpr> <blockexpr> (else (<ifexpr> | <blockexpr>))?
        // condition and thenBranch are required, elseBranch and elseifBranch are optional
        if (node.condition == null) reportNullError("IfExprNode", "condition");
        else node.condition.accept(this);
        
        if (node.thenBranch == null) reportNullError("IfExprNode", "thenBranch");
        else node.thenBranch.accept(this);
        
        if (node.elseBranch != null) node.elseBranch.accept(this);
        if (node.elseifBranch != null) node.elseifBranch.accept(this);
    }
    
    public void visit(LoopExprNode node) {
        // According to grammar: <infinite_loop> ::= loop <blockexpr>
        // <conditional_loop> ::= while <expression except structexpr> <blockexpr>
        // body is required, condition is optional (only for conditional loop)
        if (node.body == null) reportNullError("LoopExprNode", "body");
        else node.body.accept(this);
        
        if (node.condition != null) node.condition.accept(this);
    }
    
    public void visit(LiteralExprNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(PathExprNode node) {
        // According to grammar: <pathexpr> ::= <pathseg> (:: <pathseg>)?
        // LSeg is required, RSeg is optional
        if (node.LSeg == null) reportNullError("PathExprNode", "LSeg");
        else node.LSeg.accept(this);
        
        if (node.RSeg != null) node.RSeg.accept(this);
    }
    
    public void visit(PathExprSegNode node) {
        // According to grammar: <pathseg> ::= <identifier> | self | Self
        // name is optional (can be self or Self)
        if (node.name != null) node.name.accept(this);
    }
    
    public void visit(GroupExprNode node) {
        // According to grammar: <groupedexpr> ::= ( <expression> )
        // innerExpr is required
        if (node.innerExpr == null) reportNullError("GroupExprNode", "innerExpr");
        else node.innerExpr.accept(this);
    }
    
    public void visit(OperExprNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(BorrowExprNode node) {
        // According to grammar: <borrowexpr> ::= (& | &&) (mut)? <expression>
        // innerExpr is required
        if (node.innerExpr == null) reportNullError("BorrowExprNode", "innerExpr");
        else node.innerExpr.accept(this);
    }
    
    public void visit(DerefExprNode node) {
        // According to grammar: <derefexpr> ::= * <expression>
        // innerExpr is required
        if (node.innerExpr == null) reportNullError("DerefExprNode", "innerExpr");
        else node.innerExpr.accept(this);
    }
    
    public void visit(NegaExprNode node) {
        // According to grammar: <negaexpr> ::= (! | -) <expression>
        // innerExpr is required
        if (node.innerExpr == null) reportNullError("NegaExprNode", "innerExpr");
        else node.innerExpr.accept(this);
    }
    
    public void visit(ArithExprNode node) {
        // According to grammar: <arithexpr> ::= <expression> (+ | - | * | / | % | & | | | ^ | << | >>) <expression>
        // left and right are required
        if (node.left == null) reportNullError("ArithExprNode", "left");
        else node.left.accept(this);
        
        if (node.right == null) reportNullError("ArithExprNode", "right");
        else node.right.accept(this);
    }
    
    public void visit(CompExprNode node) {
        // According to grammar: <compexpr> ::= <expression> (== | != | > | < | >= | <=) <expression>
        // left and right are required
        if (node.left == null) reportNullError("CompExprNode", "left");
        else node.left.accept(this);
        
        if (node.right == null) reportNullError("CompExprNode", "right");
        else node.right.accept(this);
    }
    
    public void visit(LazyExprNode node) {
        // According to grammar: <lazyexpr> ::= <expression> (&& | ||) <expression>
        // left and right are required
        if (node.left == null) reportNullError("LazyExprNode", "left");
        else node.left.accept(this);
        
        if (node.right == null) reportNullError("LazyExprNode", "right");
        else node.right.accept(this);
    }
    
    public void visit(AssignExprNode node) {
        // According to grammar: <assignexpr> ::= <expression> = <expression>
        // left and right are required
        if (node.left == null) reportNullError("AssignExprNode", "left");
        else node.left.accept(this);
        
        if (node.right == null) reportNullError("AssignExprNode", "right");
        else node.right.accept(this);
    }
    
    public void visit(ComAssignExprNode node) {
        // According to grammar: <comassignexpr> ::= <expression> (+= | -= | *= | /= | %= | &= | |= | ^= | <<= | >>=) <expression>
        // left and right are required
        if (node.left == null) reportNullError("ComAssignExprNode", "left");
        else node.left.accept(this);
        
        if (node.right == null) reportNullError("ComAssignExprNode", "right");
        else node.right.accept(this);
    }
    
    public void visit(TypeCastExprNode node) {
        // According to grammar: <typecastexpr> ::= <expression> as <type>
        // expr and type are required
        if (node.expr == null) reportNullError("TypeCastExprNode", "expr");
        else node.expr.accept(this);
        
        if (node.type == null) reportNullError("TypeCastExprNode", "type");
        else node.type.accept(this);
    }
    
    public void visit(ArrayExprNode node) {
        // According to grammar: <arrayexpr> ::= [ (<elements> | <repeated_element>; <size>)? ]
        // All fields are optional (empty array is allowed)
        if (node.elements != null) {
            for (int i = 0; i < node.elements.size(); i++) {
                node.elements.get(i).accept(this);
            }
        }
        if (node.repeatedElement != null) node.repeatedElement.accept(this);
        if (node.size != null) node.size.accept(this);
    }
    
    public void visit(IndexExprNode node) {
        // According to grammar: <indexexpr> ::= <expression> [ <expression> ]
        // array and index are required
        if (node.array == null) reportNullError("IndexExprNode", "array");
        else node.array.accept(this);
        
        if (node.index == null) reportNullError("IndexExprNode", "index");
        else node.index.accept(this);
    }
    
    public void visit(StructExprNode node) {
        // According to grammar: <structexpr> ::= <pathseg> { <fieldvals>? }
        // structName is required, fieldValues are optional
        if (node.structName == null) reportNullError("StructExprNode", "structName");
        else node.structName.accept(this);
        
        if (node.fieldValues != null) {
            for (int i = 0; i < node.fieldValues.size(); i++) {
                node.fieldValues.get(i).accept(this);
            }
        }
    }
    
    public void visit(FieldValNode node) {
        // According to grammar: <fieldval> ::= <identifier> : <expression>
        // fieldName and value are required
        if (node.fieldName == null) reportNullError("FieldValNode", "fieldName");
        else node.fieldName.accept(this);
        
        if (node.value == null) reportNullError("FieldValNode", "value");
        else node.value.accept(this);
    }
    
    public void visit(FieldExprNode node) {
        // According to grammar: <fieldexpr> ::= <expression> . <identifier>
        // receiver and fieldName are required
        if (node.receiver == null) reportNullError("FieldExprNode", "receiver");
        else node.receiver.accept(this);
        
        if (node.fieldName == null) reportNullError("FieldExprNode", "fieldName");
        else node.fieldName.accept(this);
    }
    
    public void visit(CallExprNode node) {
        // According to grammar: <callexpr> ::= <expression> ( <arguments>? )
        // function is required, arguments are optional
        if (node.function == null) reportNullError("CallExprNode", "function");
        else node.function.accept(this);
        
        if (node.arguments != null) {
            for (int i = 0; i < node.arguments.size(); i++) {
                node.arguments.get(i).accept(this);
            }
        }
    }
    
    public void visit(MethodCallExprNode node) {
        // According to grammar: <methodcallexpr> ::= <expression> . <pathseg> ( <arguments>? )
        // receiver and methodName are required, arguments are optional
        if (node.receiver == null) reportNullError("MethodCallExprNode", "receiver");
        else node.receiver.accept(this);
        
        if (node.methodName == null) reportNullError("MethodCallExprNode", "methodName");
        else node.methodName.accept(this);
        
        if (node.arguments != null) {
            for (int i = 0; i < node.arguments.size(); i++) {
                node.arguments.get(i).accept(this);
            }
        }
    }
    
    public void visit(ContinueExprNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(BreakExprNode node) {
        // According to grammar: <breakexpr> ::= break (<expression>)?
        // value is optional
        if (node.value != null) node.value.accept(this);
    }
    
    public void visit(ReturnExprNode node) {
        // According to grammar: <returnexpr> ::= return (<expression>)?
        // value is optional
        if (node.value != null) node.value.accept(this);
    }
    
    public void visit(UnderscoreExprNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(TypeExprNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(TypePathExprNode node) {
        // According to grammar: <typepathexpr> ::= <pathseg>
        // path is required
        if (node.path == null) reportNullError("TypePathExprNode", "path");
        else node.path.accept(this);
    }
    
    public void visit(TypeRefExprNode node) {
        // According to grammar: <typerefexpr> ::= & (mut)? <type>
        // innerType is required
        if (node.innerType == null) reportNullError("TypeRefExprNode", "innerType");
        else node.innerType.accept(this);
    }
    
    public void visit(TypeArrayExprNode node) {
        // According to grammar: <typearrayexpr> ::= [ <type> ; <expression> ]
        // elementType and size are required
        if (node.elementType == null) reportNullError("TypeArrayExprNode", "elementType");
        else node.elementType.accept(this);
        
        if (node.size == null) reportNullError("TypeArrayExprNode", "size");
        else node.size.accept(this);
    }
    
    public void visit(TypeUnitExprNode node) {
        // Base implementation - no children to traverse
    }
    
    public void visit(IdentifierNode node) {
        // Base implementation - no children to traverse
    }
}