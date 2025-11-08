g pRCompiler 详细测试计划

本文档基于`testcase.md`的要求提供了详细的测试计划，包括所需的测试程序、函数和步骤。

## parseStmtNode() 实现步骤

根据grammar.md中第25-28行的语句定义，parseStmtNode()方法的实现步骤如下：

### 语法规则回顾
根据grammar.md的第25-28行，语句的语法规则是：
```
<statement> ::= <item> | <letstmt> | <exprstmt> | ;
```

根据grammar.md的第42-45行，Item的定义是：
```
<item> ::= <function> | <structitem> | <enumitem> | <constitem> | <traititem> | <implitem>
```

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in statement"

2. **获取当前Token**
   - 获取当前token以确定语句类型

3. **空语句处理**
   - 如果当前token是分号(`;`)：
     - 消费分号token
     - 返回null（表示空语句）
     - 跳过后续处理

4. **Let语句处理**
   - 如果当前token是`let`关键字：
     - 调用`parseLetStmtNode()`方法解析let语句
     - 返回解析结果

5. **Item类型处理**
   - 如果当前token匹配任何Item类型关键字，需要进一步区分：
     - **函数Item**：如果当前token是`fn`
       - 直接调用`parseFunctionNode()`解析函数定义
       - 注意：根据grammar.md，`const fn`应该被识别为函数而不是constitem
     - **结构体Item**：如果当前token是`struct`
       - 调用`parseStructNode()`解析结构体定义
     - **枚举Item**：如果当前token是`enum`
       - 调用`parseEnumNode()`解析枚举定义
     - **常量Item和常量函数区分**：如果当前token是`const`
       - 需要检查下一个token来确定是常量item还是常量函数：
       - 如果下一个token是`fn`，那么它是常量函数：
         - 消费const token
         - 调用`parseFunctionNode()`（它会在内部处理const前缀）
       - 如果下一个token不是`fn`，那么它是常规常量item：
         - 调用`parseConstItemNode()`解析常量定义
     - **Trait Item**：如果当前token是`trait`
       - 调用`parseTraitNode()`解析trait定义
     - **Impl Item**：如果当前token是`impl`
       - 调用`parseImplNode()`解析impl定义
       - 在`parseImplNode()`内部，会进一步区分固有实现和trait实现

6. **表达式语句处理（默认情况）**
   - 如果当前token不匹配以上任何关键字：
     - 调用`parseExprStmtNode()`方法解析表达式语句
     - 返回解析结果

### 代码实现示例

```java
public StmtNode parseStmtNode() {
    // 1. Input validation
    assert i < tokens.size() : "No more tokens to parse in statement";
    
    // 2. Get current token
    token_t token = tokens.get(i);
    
    // 3. Empty statement handling
    if (token.name.equals(";")) {
        i++; // consume semicolon
        return null; // return null for empty statement
    }
    
    // 4. Let statement handling
    if (token.name.equals("let")) {
        return parseLetStmtNode();
    }
    // 5. Item type handling
    else if (token.name.equals("fn")) {
        // Function item
        return parseFunctionNode();
    }
    else if (token.name.equals("struct")) {
        // Struct item
        return parseStructNode();
    }
    else if (token.name.equals("enum")) {
        // Enum item
        return parseEnumNode();
    }
    else if (token.name.equals("const")) {
        // Const item or const function
        // Need to check next token to determine if it's a const item or const function
        if (i + 1 < tokens.size() && tokens.get(i + 1).name.equals("fn")) {
            // Const function
            i++; // consume const
            return parseFunctionNode(); // parseFunctionNode() will handle const prefix
        } else {
            // Regular const item
            return parseConstItemNode();
        }
    }
    else if (token.name.equals("trait")) {
        // Trait item
        return parseTraitNode();
    }
    else if (token.name.equals("impl")) {
        // Impl item
        return parseImplNode();
    }
    // 6. Expression statement handling (default case)
    else {
        return parseExprStmtNode();
    }
}
```

### 注意事项

1. **空语句处理**：根据语法规则，单个分号是有效的语句，需要特殊处理
2. **顺序检查**：应按特定顺序执行关键字检查，以确保优先匹配
3. **错误处理**：每个分支应正确处理错误情况并提供有意义的错误消息
4. **返回值处理**：调用者需要处理parseStmtNode()可能返回null（空语句）的情况

### 相关方法调用

- `parseLetStmtNode()`: 解析let语句
- `parseFunctionNode()`: 解析函数定义
- `parseStructNode()`: 解析结构体定义
- `parseEnumNode()`: 解析枚举定义
- `parseConstItemNode()`: 解析常量定义
- `parseTraitNode()`: 解析trait定义
- `parseImplNode()`: 解析impl定义
- `parseExprStmtNode()`: 解析表达式语句

### 语法树节点类型

基于解析结果，返回的StmtNode可能是以下类型之一：
- LetStmtNode (let语句)
- FunctionNode (函数定义)
- StructNode (结构体定义)
- EnumNode (枚举定义)
- ConstItemNode (常量定义)
- TraitNode (trait定义)
- ImplNode (impl定义)
- ExprStmtNode (表达式语句)
- null (空语句)

### 特殊情况处理细节

#### 1. 常量函数和常量项的区别

根据grammar.md中的语法定义：
- `<function>` ::= `(const)? fn <identifier> (<parameters>?) (-> <type>)? ( <blockexpr> | ; )`
- `<constitem>` ::= `const <identifier> : <type> (= <expression>)? ;`

关键区别：
- `const fn`应该被识别为函数，因为函数的语法定义已经包含可选的const前缀
- 当`const`单独出现时，需要检查下一个token来确定是常量函数还是常规常量

实现策略：
1. 遇到`const`关键字时，不要立即调用`parseConstItemNode()`
2. 检查下一个token是否是`fn`
3. 如果是`fn`，那么它是常量函数，消费`const`并调用`parseFunctionNode()`
4. 如果不是`fn`，那么它是常规常量，调用`parseConstItemNode()`

#### 2. Impl类型的区分

根据grammar.md中的语法定义：
- `<implitem>` ::= `<inherentimplitem>` | `<traitimplitem>`
- `<inherentimplitem>` ::= `impl <type> { <asso_item>* }`
- `<traitimplitem>` ::= `impl <identifier> for <type> { <asso_item>* }`

实现策略：
1. 遇到`impl`关键字时，调用`parseImplNode()`
2. 在`parseImplNode()`内部，检查impl后面的标识符
3. 继续检查是否有`for`关键字
4. 如果有`for`，那么它是trait实现
5. 如果没有`for`，那么它是固有实现

#### 3. 函数项的const前缀处理

根据grammar.md，函数的语法定义已经包含可选的const前缀：
`<function> ::= (const)? fn <identifier> (<parameters>?) (-> <type>)? ( <blockexpr> | ; )`

这意味着：
- `const fn`应该被识别为函数，而不是constitem
- `parseFunctionNode()`方法需要在内部处理const前缀
- 这种处理应该在`parseFunctionNode()`内部实现，而不是在`parseStmtNode()`中

## parseFunctionNode() 实现步骤

根据grammar.md的第47-52行，函数的语法规则是：
```
<function> ::= (const)? fn <identifier> (<parameters>?) (-> <type>)? ( <blockexpr> | ; )

<parameters> ::= <selfpara> ,? | (<selfpara> ,)? <parameter> (, <parameter>)* ,?
```

### 方法重载描述

Parser类提供了两个parseFunctionNode()方法：
1. `parseFunctionNode()` - 无参数版本，调用`parseFunctionNode(false)`
2. `parseFunctionNode(boolean isConst)` - 带参数版本，实际执行解析工作

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in function"

2. **创建函数节点**
   - 创建一个新的FunctionNode对象
   - 设置isConst标志（根据传入的isConst参数）

3. **消费fn关键字**
   - 消费当前token（应该是"fn"）
   - 注意：const前缀已在parseStmtNode()或parseAssoItemNode()中处理

4. **解析函数名**
   - 检查当前token是否是标识符
   - 调用`parseIdentifierNode()`解析函数名
   - 如果不是标识符，抛出断言错误："Expected function name after 'fn'"

5. **解析参数列表开始**
   - 检查当前token是否是左括号"("
   - 如果是，消费左括号
   - 如果不是，抛出断言错误："Expected '(' after function name"

6. **解析Self参数**
   - 调用`parseSelfParaNode()`解析可能的self参数
   - parseSelfParaNode()会在内部尝试解析self参数，如果不是self参数，会回溯并返回null
   - 如果有self参数和后续参数，检查逗号分隔符

7. **解析常规参数列表**
   - 创建参数向量
   - 循环解析参数直到遇到右括号")"
   - 调用`parseParameterNode()`解析每个参数
   - 在每个参数后检查逗号分隔符（除了最后一个参数）

8. **解析参数列表结束**
   - 检查当前token是否是右括号")"
   - 如果是，消费右括号
   - 如果不是，抛出断言错误："Expected ')' at end of parameter list"

9. **解析返回类型**
   - 检查当前token是否是箭头"->"
   - 如果是，消费箭头并调用`parseTypeExprNode()`解析返回类型
   - 如果不是，将返回类型设置为null

10. **解析函数体**
    - 检查当前token是否是左大括号"{"
    - 如果是，调用`parseBlockExprNode()`解析函数体
      - 注意：`parseBlockExprNode()`会消费左大括号本身，调用者不需要预先消费
    - 如果当前token是分号";"，消费分号并将函数体设置为null
    - 如果都不是，抛出断言错误："Expected '{' or ';' after function signature"
    
11. **返回函数节点**
    - 返回构造的FunctionNode对象

## parseStructNode() 实现步骤

根据grammar.md的第66-71行，结构体的语法规则是：
```
<structitem> ::= struct <identifier> ({ <fields>? } | ;)

<fields> ::= <field> (, <field>)* ,?
```

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in struct"

2. **消费struct关键字**
   - 消费当前token（应该是"struct"）

3. **创建结构体节点**
   - 创建一个新的StructNode对象

4. **解析结构体名**
   - 检查当前token是否是标识符
   - 调用`parseIdentifierNode()`解析结构体名
   - 如果不是标识符，抛出断言错误："Expected struct name after 'struct'"

5. **解析结构体定义**
   - 检查当前token：
     - 如果是左大括号"{"，表示字段定义：
       - 消费左大括号
       - 创建字段向量
       - 循环解析字段直到遇到右大括号"}"
       - 调用`parseFieldNode()`解析每个字段
       - 在每个字段后检查逗号分隔符
       - 检查并消费右大括号"}"
     - 如果是分号";"，表示单元结构体：
       - 消费分号
       - 将字段列表设置为null
     - 如果都不是，抛出断言错误："Expected '{' or ';' after struct name"

6. **返回结构体节点**
   - 返回构造的StructNode对象

## parseEnumNode() 实现步骤

根据grammar.md的第78-83行，枚举的语法规则是：
```
<enumitem> ::= enum <identifier> { <enum_variants>? }

<enum_variants> ::= <identifier> (, <identifier>)* ,?
```

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in enum"

2. **消费enum关键字**
   - 消费当前token（应该是"enum"）

3. **创建枚举节点**
   - 创建一个新的EnumNode对象

4. **解析枚举名**
   - 检查当前token是否是标识符
   - 调用`parseIdentifierNode()`解析枚举名
   - 如果不是标识符，抛出断言错误："Expected enum name after 'enum'"

5. **解析枚举变体列表**
   - 检查当前token是否是左大括号"{"
   - 如果是，消费左大括号
   - 如果不是，抛出断言错误："Expected '{' after enum name"

6. **解析变体**
   - 创建变体向量
   - 循环解析变体直到遇到右大括号"}"
   - 调用`parseIdentifierNode()`解析每个变体
   - 在每个变体后检查逗号分隔符

7. **解析枚举定义结束**
   - 检查当前token是否是右大括号"}"
   - 如果是，消费右大括号
   - 如果不是，抛出断言错误："Expected '}' at end of enum definition"

8. **返回枚举节点**
   - 返回构造的EnumNode对象

## parseConstItemNode() 实现步骤

根据grammar.md的第85-88行，常量项的语法规则是：
```
<constitem> ::= const <identifier> : <type> (= <expression>)? ;
```

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in const item"

2. **消费const关键字**
   - 消费当前token（应该是"const"）

3. **创建常量节点**
   - 创建一个新的ConstItemNode对象

4. **解析常量名**
   - 检查当前token是否是标识符
   - 调用`parseIdentifierNode()`解析常量名
   - 如果不是标识符，抛出断言错误："Expected const name after 'const'"

5. **解析类型注解**
   - 检查当前token是否是冒号":"
   - 如果是，消费冒号并调用`parseTypeExprNode()`解析类型
   - 如果不是，抛出断言错误："Expected ':' after const name"

6. **解析初始值（可选）**
   - 检查当前token是否是等号"="
   - 如果是，消费等号并调用`parseExprNode()`解析初始值
   - 如果不是，将值设置为null

7. **解析常量定义结束**
   - 检查当前token是否是分号";"
   - 如果是，消费分号
   - 如果不是，抛出断言错误："Expected ';' at end of const item"

8. **返回常量节点**
   - 返回构造的ConstItemNode对象

## parseTraitNode() 实现步骤

根据grammar.md的第90-93行，trait的语法规则是：
```
<traititem> ::= trait <identifier> { <asso_item>* }
```

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in trait"

2. **消费trait关键字**
   - 消费当前token（应该是"trait"）

3. **创建Trait节点**
   - 创建一个新的TraitNode对象

4. **解析Trait名**
   - 检查当前token是否是标识符
   - 调用`parseIdentifierNode()`解析trait名
   - 如果不是标识符，抛出断言错误："Expected trait name after 'trait'"

5. **解析Trait定义开始**
   - 检查当前token是否是左大括号"{"
   - 如果是，消费左大括号
   - 如果不是，抛出断言错误："Expected '{' after trait name"

6. **解析关联项列表**
   - 创建关联项向量
   - 循环解析关联项直到遇到右大括号"}"
   - 调用`parseAssoItemNode()`解析每个关联项
   - 跳过null项（空语句）

7. **解析Trait定义结束**
   - 检查当前token是否是右大括号"}"
   - 如果是，消费右大括号
   - 如果不是，抛出断言错误："Expected '}' at end of trait body"

8. **返回Trait节点**
   - 返回构造的TraitNode对象

## parseImplNode() 实现步骤

根据grammar.md的第100-106行，实现的语法规则是：
```
<implitem> ::= <inherentimplitem> | <traitimplitem>

<inherentimplitem> ::= impl <type> { <asso_item>* }
<traitimplitem> ::= impl <identifier> for <type> { <asso_item>* }
```

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in impl"

2. **消费impl关键字**
   - 消费当前token（应该是"impl"）

3. **创建实现节点**
   - 创建一个新的ImplNode对象

4. **区分实现类型**
    - 检查当前token是否是标识符
    - 如果是，检查下一个token是否是"for"关键字：
      - 如果是"for"，那么它是trait实现：
        - 调用`parseIdentifierNode()`解析trait名
        - 将trait名设置到节点的traitName属性
        - 消费"for"关键字
        - 调用`parseTypeExprNode()`解析类型名
        - 将类型名设置到节点的typeName属性
      - 如果不是"for"，那么它是固有实现：
        - 调用`parseTypeExprNode()`解析类型名
        - 将类型名设置到节点的typeName属性
        - 将trait名设置为null
    - 如果当前token不是标识符：
      - 调用`parseTypeExprNode()`解析类型名
      - 将类型名设置到节点的typeName属性
      - 将trait名设置为null

6. **解析实现定义开始**
   - 检查当前token是否是左大括号"{"
   - 如果是，消费左大括号
   - 如果不是，抛出断言错误："Expected '{' after impl type"

7. **解析关联项列表**
   - 创建关联项向量
   - 循环解析关联项直到遇到右大括号"}"
   - 调用`parseAssoItemNode()`解析每个关联项
   - 跳过null项（空语句）

8. **解析实现定义结束**
   - 检查当前token是否是右大括号"}"
   - 如果是，消费右大括号
   - 如果不是，抛出断言错误："Expected '}' at end of impl body"

9. **返回实现节点**
   - 返回构造的ImplNode对象

## parseAssoItemNode() 实现步骤

根据grammar.md的第95-98行，关联项的语法规则是：
```
<asso_item> ::= <function> | <constitem>
```

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in associated item"

2. **创建关联项节点**
   - 创建一个新的AssoItemNode对象

3. **区分关联项类型**
    - 检查当前token：
      - 如果是"fn"关键字：
        - 调用`parseFunctionNode(false)`解析函数
        - 返回解析的函数包装在AssoItemNode中
      - 如果是"const"关键字：
        - 检查下一个token是否是"fn"：
          - 如果是"fn"，那么它是常量函数：
            - 消费"const"关键字
            - 调用`parseFunctionNode(true)`解析常量函数
            - 返回解析的函数包装在AssoItemNode中
          - 如果不是"fn"，那么它是常规常量项：
            - 调用`parseConstItemNode()`解析常量项
            - 返回解析的常量项包装在AssoItemNode中
      - 如果是分号";"：
        - 消费分号
        - 返回null（空语句）
      - 如果都不是，抛出断言错误："Expected 'fn' or 'const' in associated item"

4. **返回关联项节点**
   - 返回构造的AssoItemNode对象

## parseBlockExprNode() 实现步骤

根据grammar.md中块表达式的语法规则，parseBlockExprNode()方法的实现步骤如下：

### 语法规则回顾
根据grammar.md，块表达式的语法规则是：
```
<blockexpr> ::= { <statements>? }

<statements> ::= <statement>+
               | <statement>+ <expressionwithoutblock>
               | <expressionwithoutblock>
```

### 重要说明

**parseBlockExprNode()会消费左大括号"{"**

这是一个重要的设计决策，调用parseBlockExprNode()的函数不需要在调用前消费左大括号，因为parseBlockExprNode()会自己消费左大括号。

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in block expression"

2. **创建块表达式节点**
   - 创建一个新的BlockExprNode对象

3. **消费左大括号**
   - 检查当前token是否是左大括号"{"
   - 如果是，消费左大括号
   - 如果不是，抛出断言错误："Expected '{' at start of block expression"

4. **解析语句列表**
    - 检查当前token是否是右大括号"}"：
      - 如果是，表示空块，跳过语句解析
      - 如果不是，继续解析语句列表
    - 创建语句向量
    - 循环解析直到遇到右大括号"}"：
      - 尝试解析语句：
        - 调用`parseStmtNode()`解析语句
        - 如果成功解析语句，将其添加到语句向量
        - 如果解析语句失败，尝试解析表达式：
          - 调用`parseExprWithoutBlockNode()`解析表达式
          - 将表达式包装为表达式语句并添加到向量
      - 跳过null项（空语句）

5. **解析块表达式结束**
   - 检查当前token是否是右大括号"}"
   - 如果是，消费右大括号
   - 如果不是，抛出断言错误："Expected '}' at end of block expression"

6. **返回块表达式节点**
   - 返回构造的BlockExprNode对象

### 代码实现示例

```java
public BlockExprNode parseBlockExprNode() {
    // 1. Input validation
    assert i < tokens.size() : "No more tokens to parse in block expression";
    
    // 2. Create block expression node
    BlockExprNode node = new BlockExprNode();
    
    // 3. Consume left brace
    if (i < tokens.size() && tokens.get(i).name.equals("{")) {
        i++; // consume left brace
    } else {
        assert false : "Expected '{' at start of block expression";
    }
    
    // 4. Parse statement list
    Vector<StmtNode> statements = new Vector<>();
    
    // Check for empty block
    if (i < tokens.size() && !tokens.get(i).name.equals("}")) {
        // Parse statements and expressions
        while (i < tokens.size() && !tokens.get(i).name.equals("}")) {
            try {
                // Try to parse a statement first
                StmtNode stmt = parseStmtNode();
                if (stmt != null) {
                    statements.add(stmt);
                }
            } catch (Exception e) {
                // If statement parsing fails, try to parse an expression
                try {
                    ExprWithoutBlockNode expr = parseExprWithoutBlockNode();
                    if (expr != null) {
                        // Wrap expression in an expression statement
                        ExprStmtNode exprStmt = new ExprStmtNode();
                        exprStmt.expr = expr;
                        statements.add(exprStmt);
                    }
                } catch (Exception exprE) {
                    // If both fail, rethrow the original exception
                    throw e;
                }
            }
        }
    }
    node.statements = statements;
    
    // 5. Parse end of block expression
    if (i < tokens.size() && tokens.get(i).name.equals("}")) {
        i++; // consume right brace
    } else {
        assert false : "Expected '}' at end of block expression";
    }
    
    // 6. Return block expression node
    return node;
}
```

### 调用示例

```java
// 正确的调用方式
if (i < tokens.size() && tokens.get(i).name.equals("{")) {
    // parseBlockExprNode()会自己消费左大括号，调用者不需要预先消费
    node.body = parseBlockExprNode(); // 直接调用parseBlockExprNode()
}
```

### 注意事项

1. **左大括号处理**：parseBlockExprNode()会消费左大括号，调用者不需要预先消费左大括号
2. **右大括号处理**：parseBlockExprNode()会消费右大括号
3. **空语句处理**：该方法会跳过parseStmtNode()返回的null值（空语句）
4. **错误处理**：如果找不到右大括号，将抛出断言错误
5. **Statements语法处理**：根据新的语法规则，块内可以包含：
   - 一个或多个语句
   - 一个或多个语句后跟一个表达式
   - 仅一个表达式
   - 空块（无内容）

### Statements语法说明

根据更新的语法规则，Statements有以下三种形式：

1. **仅语句**：
   ```
   {
       let x = 5;
       println!("Hello");
   }
   ```

2. **语句后跟表达式**：
   ```
   {
       let x = 5;
       let y = 10;
       x + y  // 这是块的表达式，也是返回值
   }
   ```

3. **仅表达式**：
   ```
   {
       42  // 整个块仅包含一个表达式，其值为42
   }
   ```

### 解析策略

为了正确处理这三种情况，解析器需要：

1. 首先尝试解析语句
2. 如果语句解析失败，尝试解析表达式
3. 如果成功解析表达式，将其包装为表达式语句
4. 继续解析直到遇到右大括号

### 相关方法调用

- `parseStmtNode()`: 解析块中的每个语句
- `parseExprWithoutBlockNode()`: 解析块中的表达式

## parseExprWithBlockNode() 实现步骤

根据grammar.md中表达式的语法规则，parseExprWithBlockNode()方法的实现步骤如下：

### 语法规则回顾
根据grammar.md，包含块的表达式的语法规则包括：
```
<exprwithblock> ::= <ifexpr> | <loopexpr> | <blockexpr>
```

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in expression with block"

2. **创建表达式节点**
   - 创建一个新的ExprWithBlockNode对象

3. **区分表达式类型**
   - 检查当前token：
     - 如果是"if"关键字：
       - 调用`parseIfExprNode()`解析if表达式
       - 将ExprWithBlockNode的expr属性设置为返回的IfExprNode
     - 如果是"while"关键字：
       - 调用`parseLoopExprNode()`解析while循环表达式
       - 将ExprWithBlockNode的expr属性设置为返回的LoopExprNode
     - 如果是"loop"关键字：
       - 调用`parseLoopExprNode()`解析loop表达式
       - 将ExprWithBlockNode的expr属性设置为返回的LoopExprNode
     - 如果是左大括号"{"：
       - 调用`parseBlockExprNode()`解析块表达式
       - 将ExprWithBlockNode的expr属性设置为返回的BlockExprNode
       - 注意：`parseBlockExprNode()`会自己消费左大括号，调用者不需要预先消费
     - 如果都不是，抛出断言错误："Expected 'if', 'while', 'loop' or '{' in block expression"

4. **返回表达式节点**
   - 返回构造的ExprWithBlockNode对象

### 注意事项

1. **左大括号处理**：解析块表达式时，不需要在调用`parseBlockExprNode()`前消费左大括号，因为该方法会自己消费
2. **表达式类型区分**：根据起始关键字区分不同类型的包含块的表达式
3. **错误处理**：如果遇到不支持的token，抛出断言错误

### 相关方法调用

- `parseIfExprNode()`: 解析if表达式
- `parseLoopExprNode()`: 解析循环表达式
- `parseBlockExprNode()`: 解析块表达式

## parseIfExprNode() 实现步骤

根据grammar.md中if表达式的语法规则，parseIfExprNode()方法的实现步骤如下：

### 语法规则回顾
根据grammar.md，if表达式的语法规则是：
```
<ifexpr> ::= if <expressionwithoutblock> <blockexpr> (else <blockexpr> | else <ifexpr>)?
```

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in if expression"

2. **消费if关键字**
   - 消费当前token（应该是"if"）

3. **创建If表达式节点**
   - 创建一个新的IfExprNode对象

4. **解析条件表达式**
   - 调用`parseExprNode()`解析条件表达式
   - 注意：在Rust中，if条件不需要括号，但允许使用括号

5. **解析Then分支**
   - 检查当前token是否是左大括号"{"
   - 如果是，调用`parseBlockExprNode()`解析then分支
     - 注意：`parseBlockExprNode()`会自己消费左大括号，调用者不需要预先消费
   - 如果不是，抛出断言错误："Expected '{' after if condition"

6. **解析Else分支（可选）**
   - 检查当前token是否是"else"关键字
   - 如果是：
     - 消费"else"关键字
     - 检查下一个token：
       - 如果是"if"关键字，那么它是else if分支：
         - 调用`parseIfExprNode()`解析else if分支
       - 如果是左大括号"{"，那么它是else分支：
         - 调用`parseBlockExprNode()`解析else分支
         - 注意：`parseBlockExprNode()`会自己消费左大括号，调用者不需要预先消费
   - 如果不是"else"关键字，将else分支设置为null

7. **返回If表达式节点**
   - 返回构造的IfExprNode对象

### 注意事项

1. **左大括号处理**：不需要在调用`parseBlockExprNode()`前消费左大括号，因为该方法会自己消费
2. **条件表达式**：条件表达式必须用括号括起来
3. **Else If处理**：Else if被视为嵌套的if表达式
4. **错误处理**：如果语法不正确，抛出相应的断言错误

### 相关方法调用

- `parseGroupExprNode()`: 解析条件表达式
- `parseBlockExprNode()`: 解析then分支和else分支

## parseLoopExprNode() 实现步骤

根据grammar.md中循环表达式的语法规则，parseLoopExprNode()方法的实现步骤如下：

### 语法规则回顾
根据grammar.md，循环表达式的语法规则是：
```
<loopexpr> ::= while <expressionwithoutblock> <blockexpr> | loop <blockexpr>
```

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in loop expression"

2. **创建循环表达式节点**
   - 创建一个新的LoopExprNode对象

3. **区分循环类型**
   - 检查当前token：
     - 如果是"while"关键字：
       - 消费"while"关键字
       - 调用`parseExprNode()`解析条件表达式
       - 将条件表达式设置到节点的condition属性
     - 如果是"loop"关键字：
       - 消费"loop"关键字
       - 将条件表达式设置为null（无限循环）
     - 如果都不是，抛出断言错误："Expected 'while' or 'loop' at start of loop expression"

4. **解析循环体**
   - 检查当前token是否是左大括号"{"
   - 如果是，调用`parseBlockExprNode()`解析循环体
     - 注意：`parseBlockExprNode()`会自己消费左大括号，调用者不需要预先消费
   - 如果不是，抛出断言错误："Expected '{' after loop condition"

5. **返回循环表达式节点**
   - 返回构造的LoopExprNode对象

### 注意事项

1. **左大括号处理**：不需要在调用`parseBlockExprNode()`前消费左大括号，因为该方法会自己消费
2. **循环类型区分**：根据起始关键字区分while循环和无限循环
3. **条件表达式**：while循环的条件表达式在Rust中不需要括号
4. **错误处理**：如果语法不正确，抛出相应的断言错误

### 相关方法调用

- `parseGroupExprNode()`: 解析while循环的条件表达式
- `parseBlockExprNode()`: 解析循环体

## parseLetStmtNode() 实现步骤

根据grammar.md的第30-33行，let语句的语法规则是：
```
<letstmt> ::= let <pattern> : <type> (= <expression>)? ;
```

### 实现步骤

1. **消费let关键字**
   - 消费当前token（应该是"let"）

2. **创建Let语句节点**
   - 创建一个新的LetStmtNode对象

3. **解析模式**
   - 调用`parsePatternNode()`解析模式
   - 将模式设置到节点的name属性

4. **解析类型注解**
   - 检查当前token是否是冒号":"
   - 如果是，消费冒号并调用`parseTypeExprNode()`解析类型
   - 将类型设置到节点的type属性
   - 如果不是，抛出断言错误："Expected ':' after pattern in let statement"

5. **解析初始值（可选）**
   - 检查当前token是否是等号"="
   - 如果是，消费等号并调用`parseExprNode()`解析初始值
   - 将值设置到节点的value属性
   - 如果不是，将值设置为null

6. **解析Let语句结束**
   - 检查当前token是否是分号";"
   - 如果是，消费分号
   - 如果不是，抛出断言错误："Expected ';' at end of let statement"

7. **返回Let语句节点**
   - 返回构造的LetStmtNode对象

## parseSelfParaNode() 实现步骤

根据grammar.md的第54-59行，self参数的语法规则是：
```
<selfpara> ::= <shortself> | <typedself>
<shortself> ::= &? (mut)? self
<typedself> ::= (mut)? self : <type>
```

### 实现步骤

1. **保存起始位置**
   - 保存当前token索引以备可能的回溯
   - 初始化布尔标志`istyped`为true

2. **创建Self参数节点**
   - 创建一个新的SelfParaNode对象

3. **解析引用前缀**
   - 检查当前token是否是"&"
   - 如果是，将节点的isReference属性设置为true并消费"&"
   - 将istyped设置为false（表示这不是类型化的self参数）
   - 如果不是，将isReference设置为false

4. **解析可变前缀**
   - 检查当前token是否是"mut"关键字
   - 如果是，将节点的isMutable属性设置为true并消费"mut"关键字
   - 如果不是，将isMutable设置为false

5. **解析self关键字**
   - 检查当前token是否是"self"关键字
   - 如果是，消费"self"关键字
   - 如果不是，回溯到保存的位置并返回null（不是self参数）

6. **解析类型注解（用于类型化Self）**
   - 如果istyped为true且当前token是冒号":"：
     - 消费冒号
     - 调用`parseTypeExprNode()`解析类型
     - 将类型设置到节点的type属性

7. **返回Self参数节点**
   - 返回构造的SelfParaNode对象

## parseParameterNode() 实现步骤

根据grammar.md的第61-64行，参数的语法规则是：
```
<parameter> ::= <pattern> : <type>
```

### 实现步骤

1. **创建参数节点**
   - 创建一个新的ParameterNode对象

2. **解析参数模式**
   - 调用`parsePatternNode()`解析参数模式
   - 将模式设置到节点的name属性

3. **解析类型注解**
   - 检查当前token是否是冒号":"
   - 如果是，消费冒号并调用`parseTypeExprNode()`解析类型
   - 将类型设置到节点的type属性
   - 如果不是，抛出断言错误："Expected ':' after parameter pattern"

4. **返回参数节点**
   - 返回构造的ParameterNode对象

## parsePatternNode() 实现步骤

根据grammar.md的第110-128行，模式的语法规则是：
```
<pattern> ::= <idpat> | <wildpat> | <refpat>

<idpat> ::= (ref)? (mut)? <identifier>
<wildpat> ::= _
<refpat> ::= (& | &&) (mut)? <pattern>
```

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in pattern"

2. **获取当前Token**
   - 获取当前token以确定模式类型

3. **解析引用模式**
   - 如果当前token是"&"或"&&"：
     - 创建一个新的RefPatNode对象
     - 消费"&"token(s)
     - 检查下一个token是否是"mut"：
       - 如果是，将isMutable设置为true并消费"mut"
     - 递归调用`parsePatternNode()`解析内部模式
     - 将内部模式设置到节点的innerPattern属性
     - 返回RefPatNode

4. **解析通配符模式**
   - 如果当前token是"_"：
     - 创建一个新的WildPatNode对象
     - 消费下划线token
     - 返回WildPatNode

5. **解析标识符模式（默认情况）**
   - 创建一个新的IdPatNode对象
   - 检查当前token是否是"ref"：
     - 如果是，将isReference设置为true并消费"ref"
   - 检查当前token是否是"mut"：
     - 如果是，将isMutable设置为true并消费"mut"
   - 检查当前token是否是标识符：
     - 如果是，调用`parseIdentifierNode()`解析标识符
     - 将标识符设置到节点的name属性
     - 返回IdPatNode
   - 如果不是，抛出断言错误："Expected identifier in pattern"

## parseFieldNode() 实现步骤

根据grammar.md的第73-76行，字段的语法规则是：
```
<field> ::= <identifier> : <type> ;
```

### 实现步骤

1. **创建字段节点**
   - 创建一个新的FieldNode对象

2. **解析字段名**
   - 检查当前token是否是标识符
   - 如果是，调用`parseIdentifierNode()`解析字段名
   - 将标识符设置到节点的name属性
   - 如果不是，抛出断言错误："Expected field name in struct"

3. **解析类型注解**
   - 检查当前token是否是冒号":"
   - 如果是，消费冒号
   - 如果不是，抛出断言错误："Expected ':' after field name in struct"

4. **解析字段类型**
   - 调用`parseTypeExprNode()`解析字段类型
   - 将类型设置到节点的type属性

5. **解析分号**
    - 检查当前token是否是分号";"
    - 如果是，消费分号
    - 如果不是，抛出断言错误："Expected ';' at end of field definition"

6. **返回字段节点**
    - 返回构造的FieldNode对象

## parseExprStmtNode() 实现步骤

根据grammar.md的第35-38行，表达式语句的语法规则是：
```
<exprstmt> ::= <exprwithblock> ;? | <exprwithoutblock> ;
```

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in expression statement"

2. **创建表达式语句节点**
   - 创建一个新的ExprStmtNode对象

3. **获取当前Token**
   - 获取当前token以确定表达式类型

4. **解析包含块的表达式**
   - 如果当前token是"if"、"while"、"loop"或"{"：
     - 调用`parseExprWithBlockNode()`解析包含块的表达式
     - 将表达式设置到节点的expr属性
     - 返回ExprStmtNode（不需要分号）

5. **解析不包含块的表达式**
   - 否则：
     - 调用`parseExprWithoutBlockNode()`解析不包含块的表达式
     - 将表达式设置到节点的expr属性
     - 检查当前token是否是分号";"
       - 如果是，消费分号
       - 如果不是，抛出断言错误："Expected ';' at end of expression statement"

6. **返回表达式语句节点**
   - 返回构造的ExprStmtNode对象

## parseExprNode() 实现步骤

根据grammar.md的第132-135行，表达式的语法规则是：
```
<expression> ::= <exprwithblock> | <exprwithoutblock>
```

### 方法重载描述

Parser类提供了两个parseExprNode()方法：
1. `parseExprNode()` - 无参数版本，调用`parseExprNode(0)`
2. `parseExprNode(int precedence)` - 带参数版本，实际执行解析工作

### 核心问题

当前实现存在一个关键问题：简单地根据起始token来决定是调用`parseExprWithBlockNode()`还是`parseExprWithoutBlockNode()`是不正确的。因为：

1. **所有block expression都有歧义性**：不只是带`{`的block expression，所有block expression（if、while、loop、block）都可能存在这个问题。
2. **表达式类型的不确定性**：一个表达式可能以看起来像ExprWithBlockNode的token开始，但实际上最终解析为ExprWithoutBlockNode。
3. **语法上下文依赖**：表达式的最终类型取决于完整的解析过程，而不仅仅是起始token。

### 解决策略

使用基于下一个token判断的解析策略：当我们选择调用parseExprWithBlockNode()后，通过检查解析完成后的下一个token内容来确定表达式类型，从而避免错误的类型判断。

### 具体实现方法

1. **保存起始位置**：在尝试解析为ExprWithBlockNode之前，保存当前token的位置，以便在需要时回溯。

2. **尝试解析为ExprWithBlockNode**：
   - 如果当前token是"if"、"while"、"loop"或"{"，先尝试解析为ExprWithBlockNode
   - 完成解析后，检查下一个token

3. **检查下一个token以确定真实类型**：
   - 根据grammar.md，检查哪些without block expression语法的第一个成分是expression或expression的子类
   - 如果下一个token匹配这些成分，说明当前block expression实际上是without block expression的一部分
   - 需要回溯并重新解析为ExprWithoutBlockNode

4. **回溯与重新解析**：
   - 如果确定需要回溯，恢复到保存的起始位置
   - 调用parseExprWithoutBlockNode(precedence)重新解析
   - 确保parseExprWithoutBlockNode能够处理从block expression回溯的情况

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in expression"

2. **保存起始位置**
   - 保存当前token索引，以便在需要时回溯

3. **检查当前token类型**
   - 获取当前token以确定可能的表达式类型

4. **尝试解析为ExprWithBlockNode**
   - 如果当前token是"if"、"while"、"loop"或"{"：
     - 调用`parseExprWithBlockNode()`解析包含块的表达式
     - 记录解析结束后的位置

5. **分析下一个token**
   - 检查解析完成后的下一个token：
     - 如果下一个token是运算符（根据grammar.md中的without block expression语法的第一个成分是expression或expression的子类），说明这是一个更大的表达式的一部分
     - 需要回溯并重新解析为ExprWithoutBlockNode

6. **类型确认与可能的回溯**
   - 如果下一个token表明当前解析的类型不正确：
     - 回溯到保存的起始位置
     - 调用`parseExprWithoutBlockNode(precedence)`重新解析
   - 如果下一个token确认当前解析类型正确：
     - 返回已解析的表达式

7. **默认处理**
   - 如果当前token不是"if"、"while"、"loop"或"{"：
     - 直接调用`parseExprWithoutBlockNode(precedence)`解析

8. **错误处理**
   - 如果两种解析方式都失败，抛出解析错误："Failed to parse expression as either ExprWithBlockNode or ExprWithoutBlockNode"

### 运算符Token判断

根据grammar.md第144行，`<exprwithoutblock>`包含多种表达式类型。查看这些表达式的语法定义，第一个成分是expression或expression的子类的有：

- `<operexpr>`: 各种运算符表达式
- `<arrayexpr>`: `[`...
- `<indexexpr>`: `<expression>` [...
- `<structexpr>`: `<pathseg>` {...
- `<callexpr>`: `<expression>` (...
- `<methodcallexpr>`: `<expression>` ....
- `<fieldexpr>`: `<expression>` ....
- `<groupedexpr>`: ( `<expression>` )

因此，如果block expression后面跟着以下token，说明它可能是这些without block expression的一部分：

- `[` (arrayexpr的开始)
- `.` (fieldexpr或methodcallexpr的开始)
- `(` (callexpr或groupedexpr的开始)
- `::` (pathexpr的扩展)
- `&`、`*`、`!`、`-` (operexpr的开始)
- `as` (typecastexpr的开始)
- `=`、`+=`、`-=`、`*=`、`/=`、`%=`、`&=`、`|=`、`^=`、`<<=`、`>>=` (assignexpr或comassignexpr的开始)
- `==`、`!=`、`>`、`<`、`>=`、`<=` (compexpr的开始)
- `&&`、`||` (lazyexpr的开始)
- `+`、`-`、`*`、`/`、`%`、`|`、`^`、`<<`、`>>` (arithexpr的开始)
- `->` (函数类型的返回类型)

### 代码实现示例

```java
public ExprNode parseExprNode(int precedence) {
    if (!(i < tokens.size())) throw new ParseException("No more tokens to parse in expression");
    
    int startPos = i; // 保存起始位置
    token_t token = tokens.get(i);
    
    // 检查是否可能是ExprWithBlockNode
    if (token.name.equals("if") || token.name.equals("while") || token.name.equals("loop") || token.name.equals("{")) {
        // 尝试解析为ExprWithBlockNode
        ExprWithBlockNode withBlockNode = parseExprWithBlockNode();
        
        // 检查下一个token以确认类型
        if (i < tokens.size()) {
            token_t nextToken = tokens.get(i);
            
            // 检查是否是without block expression的一部分
            // 根据grammar.md，检查哪些without block expression语法的第一个成分是expression或expression的子类
            if (isOperatorToken(nextToken.name)) {
                // 这应该是without block expression的一部分，回溯并重新解析
                i = startPos;
                return parseExprWithoutBlockNode(precedence);
            }
        }
        
        // 确认是ExprWithBlockNode，返回结果
        return withBlockNode;
    }
    
    // 默认解析为ExprWithoutBlockNode
    return parseExprWithoutBlockNode(precedence);
}

private boolean isOperatorToken(String tokenName) {
    // 检查token是否是运算符，根据grammar.md中的without block expression语法
    // 这些token表示without block expression的开始，其第一个成分是expression或expression的子类
    return tokenName.equals("[") || tokenName.equals(".") || tokenName.equals("(") ||
           tokenName.equals("::") ||
           tokenName.equals("&") || tokenName.equals("*") ||
           tokenName.equals("!") || tokenName.equals("-") || tokenName.equals("as") ||
           tokenName.equals("=") || tokenName.equals("+=") || tokenName.equals("-=") ||
           tokenName.equals("*=") || tokenName.equals("/=") || tokenName.equals("%=") ||
           tokenName.equals("&=") || tokenName.equals("|=") || tokenName.equals("^=") ||
           tokenName.equals("<<=") || tokenName.equals(">>=") || tokenName.equals("==") ||
           tokenName.equals("!=") || tokenName.equals(">") || tokenName.equals("<") ||
           tokenName.equals(">=") || tokenName.equals("<=") || tokenName.equals("&&") ||
           tokenName.equals("||") || tokenName.equals("+") || tokenName.equals("-") ||
           tokenName.equals("*") || tokenName.equals("/") || tokenName.equals("%") ||
           tokenName.equals("|") || tokenName.equals("^") ||
           tokenName.equals("<<") || tokenName.equals(">>") || tokenName.equals("->");
}
```

### 注意事项

1. **回溯开销**：策略会有一定的回溯开销，但在大多数情况下可以接受
2. **错误消息**：需要确保错误消息清晰，能够帮助用户理解解析失败的原因
3. **性能考虑**：对于大型代码库，可能需要考虑优化回溯策略
4. **测试覆盖**：需要确保所有边界情况都有适当的测试覆盖
5. **所有block expression的处理**：需要确保对所有类型的block expression（if、while、loop、block）都进行正确的判断

## parseExprWithoutBlockNode() 实现步骤

根据grammar.md的第142-145行，不包含块的表达式的语法规则是：
```
<exprwithoutblock> ::= <literalexpr> | <pathexpr> | <operexpr> | <arrayexpr> | <indexexpr> | <structexpr> | <callexpr> | <methodcallexpr> | <fieldexpr> | <continueexpr> | <breakexpr> | <returnexpr> | <underscoreexpr> | <groupedexpr>
```

### 方法重载描述

Parser类提供了两个parseExprWithoutBlockNode()方法：
1. `parseExprWithoutBlockNode()` - 无参数版本，重置递归深度并调用`parseExprWithoutBlockNode(0)`
2. `parseExprWithoutBlockNode(int precedence)` - 带参数版本，实际执行解析工作

### 实现步骤

1. **递归深度检查**
   - 检查递归深度是否超过MAX_RECURSION_DEPTH
   - 如果是，抛出RuntimeException："Maximum recursion depth exceeded in expression parsing"
   - 增加递归深度

2. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出ParseException："No more tokens to parse in expression without block"

3. **检查ExprWithBlock起始标志**
   - 获取当前token
   - 如果当前token是"if"、"while"、"loop"或"{"：
     - 调用`parseExprWithBlockNode()`解析表达式
     - 创建一个新的ExprWithoutBlockNode变量来存储解析结果
     - 注意：ExprWithBlockNode和ExprWithoutBlockNode是不同的类型，不能直接转换
     - 将解析结果作为初始表达式节点
     - 继续执行后续的后缀表达式解析步骤

4. **解析主表达式**
   - 创建ExprWithoutBlockNode对象
   - 根据当前token类型解析不同的表达式：
     - **字面量表达式**：如果token是字面量（整数、字符、字符串等）或"true"/"false"：
       - 调用`parseLiteralExprNode()`解析字面量
     - **路径表达式**：如果token是标识符、"self"或"Self"：
       - 创建PathExprNode对象
       - 使用`parsePathExprSegNode()`解析路径段
       - 将路径段设置到PathExprNode的LSeg属性
     - **数组表达式**：如果token是"["：
       - 调用`parseArrayExprNode()`解析数组
     - **分组表达式**：如果token是"("：
       - 调用`parseGroupExprNode()`解析分组表达式
     - **借用表达式**：如果token是"&"或"&&"：
       - 调用`parseBorrowExprNode()`解析借用表达式
     - **解引用表达式**：如果token是"*"：
       - 调用`parseDerefExprNode()`解析解引用表达式
     - **否定表达式**：如果token是"-"或"!"：
       - 调用`parseNegaExprNode()`解析否定表达式
     - **Continue表达式**：如果token是"continue"：
       - 调用`parseContinueExprNode()`解析continue表达式
     - **Break表达式**：如果token是"break"：
       - 调用`parseBreakExprNode()`解析break表达式
     - **Return表达式**：如果token是"return"：
       - 调用`parseReturnExprNode()`解析return表达式
     - **下划线表达式**：如果token是"_"：
       - 调用`parseUnderscoreExprNode()`解析下划线表达式

4. **解析后缀表达式**
   - 循环处理后续token直到遇到表达式结束符：
     - **字段访问或方法调用**：如果当前token是"."：
       - 消费点
       - 使用`parsePathExprSegNode()`解析路径段
       - 如果下一个token是"("，那么它是方法调用：
         - 创建MethodCallExprNode对象
         - 将接收者设置为当前表达式
         - 将方法名设置为解析的路径段
         - 使用`parseFunctionArgs()`解析参数
       - 否则，它是字段访问：
         - 创建FieldExprNode对象
         - 将接收者设置为当前表达式
         - 将字段名设置为解析的路径段
     - **索引表达式**：如果当前token是"["：
       - 创建IndexExprNode对象
       - 将数组设置为当前表达式
       - 使用`parseExprNode(precedence)`解析索引表达式
     - **函数调用**：如果当前token是"("：
       - 创建CallExprNode对象
       - 将函数设置为当前表达式
       - 使用`parseFunctionArgs()`解析参数
     - **结构体表达式**：如果当前token是"{"：
       - 创建StructExprNode对象
       - 检查当前表达式是否是PathExprNode
       - 将结构体名设置为PathExprNode的LSeg
       - 解析字段值直到遇到"}"
     - **路径扩展**：如果当前token是"::"：
       - 检查优先级是否小于170
       - 如果是，消费"::"并扩展PathExprNode的RSeg
     - **类型转换**：如果当前token是"as"：
       - 检查优先级是否小于120
       - 如果是，创建TypeCastExprNode对象
       - 将表达式设置为当前表达式
       - 使用`parseTypeExprNode()`解析目标类型
     - **二元运算符**：如果当前token是运算符：
       - 获取运算符优先级
       - 如果运算符优先级大于当前优先级：
         - 消费运算符
         - 使用运算符的优先级解析右操作数
         - 根据运算符类型创建适当的二元表达式节点：
           - 复合赋值运算符：创建ComAssignExprNode
           - 赋值运算符：创建AssignExprNode
           - 比较运算符：创建CompExprNode
           - 算术运算符：创建ArithExprNode
           - 惰性布尔运算符：创建LazyExprNode
       - 否则，中断循环
     - **表达式结束符**：如果当前token是")"、"]"、"}"、","或";"：
       - 中断循环，表达式解析完成
     - **非法token**：如果遇到不支持的token：
       - 抛出ParseException："Unexpected token 'xxx' in expression"

5. **递减递归深度**
   - 递减递归深度

6. **返回表达式节点**
   - 返回构造的ExprWithoutBlockNode对象

### 代码实现示例

```java
public ExprWithoutBlockNode parseExprWithoutBlockNode(int precedence) {
    // 1. 递归深度检查
    if (recursionDepth > MAX_RECURSION_DEPTH) {
        throw new RuntimeException("Maximum recursion depth exceeded in expression parsing");
    }
    recursionDepth++;
    
    try {
        // 2. 输入验证
        if (!(i < tokens.size())) throw new ParseException("No more tokens to parse in expression without block");
        token_t token = tokens.get(i);
        ExprWithoutBlockNode node = new ExprWithoutBlockNode();
        
        // 3. 检查ExprWithBlock起始标志
        if (token.name.equals("if") || token.name.equals("while") ||
            token.name.equals("loop") || token.name.equals("{")) {
            // 调用parseExprWithBlockNode()解析表达式
            ExprWithBlockNode withBlockNode = parseExprWithBlockNode();
            // 创建一个新的ExprWithoutBlockNode变量来存储解析结果
            // 注意：ExprWithBlockNode和ExprWithoutBlockNode是不同的类型，不能直接转换
            ExprWithoutBlockNode blockAsExpr = new ExprWithoutBlockNode();
            blockAsExpr.expr = withBlockNode;
            node = blockAsExpr;
        }
        
        // 4. 解析主表达式
        if (token.tokentype == token_t.TokenType_t.INTEGER_LITERAL ||
            token.tokentype == token_t.TokenType_t.CHAR_LITERAL ||
            token.tokentype == token_t.TokenType_t.STRING_LITERAL ||
            token.tokentype == token_t.TokenType_t.RAW_STRING_LITERAL ||
            token.tokentype == token_t.TokenType_t.C_STRING_LITERAL ||
            token.tokentype == token_t.TokenType_t.RAW_C_STRING_LITERAL ||
            token.name.equals("true") || token.name.equals("false")) {
            node = parseLiteralExprNode();
        } else if (isIdentifier(token) || token.name.equals("self") || token.name.equals("Self")) {
            PathExprNode path = new PathExprNode();
            path.LSeg = parsePathExprSegNode();
            node = path;
        } else if (token.name.equals("[")) {
            node = parseArrayExprNode();
        } else if (token.name.equals("(")) {
            node = parseGroupExprNode();
        } else if (token.name.equals("&") || token.name.equals("&&")) {
            node = parseBorrowExprNode();
        } else if (token.name.equals("*")) {
            node = parseDerefExprNode();
        } else if (token.name.equals("-") || token.name.equals("!")) {
            node = parseNegaExprNode();
        } else if (token.name.equals("continue")) {
            node = parseContinueExprNode();
        } else if (token.name.equals("break")) {
            node = parseBreakExprNode();
        } else if (token.name.equals("return")) {
            node = parseReturnExprNode();
        } else if (token.name.equals("_")) {
            node = parseUnderscoreExprNode();
        }
        
        // 5. 解析后缀表达式
        while (i < tokens.size()) {
            token = tokens.get(i);
            
            if (token.name.equals(".")) {
                // 字段访问或方法调用
                i++;
                PathExprSegNode pathSeg = parsePathExprSegNode();
                if (i < tokens.size() && tokens.get(i).name.equals("(")) {
                    // 方法调用
                    MethodCallExprNode methodCallNode = new MethodCallExprNode();
                    methodCallNode.receiver = node;
                    methodCallNode.methodName = pathSeg;
                    Vector<ExprNode> arguments = parseFunctionArgs();
                    methodCallNode.arguments = arguments;
                    node = methodCallNode;
                } else {
                    // 字段访问
                    FieldExprNode fieldNode = new FieldExprNode();
                    fieldNode.receiver = node;
                    if (pathSeg.patternType == patternSeg_t.IDENT) {
                        fieldNode.fieldName = pathSeg.name;
                    } else {
                        throw new ParseException("Expected identifier after '.' in field access");
                    }
                    node = fieldNode;
                }
            } else if (isOper(token)) {
                // 二元运算符
                int opPrecedence = getPrecedence(token);
                if (opPrecedence <= precedence) {
                    break;
                }
                oper_t operator = getOper(token.name);
                
                if (isComAssignOper(token)) {
                    ComAssignExprNode comAssignNode = new ComAssignExprNode();
                    comAssignNode.operator = operator;
                    i++;
                    comAssignNode.left = node;
                    comAssignNode.right = parseExprNode(opPrecedence);
                    node = comAssignNode;
                } else if (isAssignOper(token)) {
                    AssignExprNode assignNode = new AssignExprNode();
                    assignNode.operator = operator;
                    i++;
                    assignNode.left = node;
                    assignNode.right = parseExprNode(opPrecedence);
                    node = assignNode;
                } else if (isComp(token)) {
                    CompExprNode compNode = new CompExprNode();
                    compNode.operator = operator;
                    i++;
                    compNode.left = node;
                    compNode.right = parseExprNode(opPrecedence);
                    node = compNode;
                } else if (isArith(token)) {
                    ArithExprNode arithNode = new ArithExprNode();
                    arithNode.operator = operator;
                    i++;
                    arithNode.left = node;
                    arithNode.right = parseExprNode(opPrecedence);
                    node = arithNode;
                } else if (isLazy(token)) {
                    LazyExprNode lazyNode = new LazyExprNode();
                    lazyNode.operator = operator;
                    i++;
                    lazyNode.left = node;
                    lazyNode.right = parseExprNode(opPrecedence);
                    node = lazyNode;
                }
            } else if (token.name.equals("as")) {
                // 类型转换
                if (precedence >= 120) {
                    break;
                }
                TypeCastExprNode typeCastNode = new TypeCastExprNode();
                i++;
                typeCastNode.expr = node;
                typeCastNode.type = parseTypeExprNode();
                node = typeCastNode;
            } else if (token.name.equals("::")) {
                // 路径扩展
                if (precedence >= 170) {
                    break;
                }
                i++;
                if (node instanceof PathExprNode) {
                    if (((PathExprNode)node).RSeg != null) {
                        throw new ParseException("Unexpected state: PathExprNode already has RSeg in path expression");
                    }
                    ((PathExprNode)node).RSeg = parsePathExprSegNode();
                    continue;
                }
                throw new ParseException("Expected path segment before '::' in path expression");
            } else if (token.name.equals("[")) {
                // 索引表达式
                IndexExprNode indexNode = new IndexExprNode();
                indexNode.array = node;
                indexNode.index = parseExprNode(precedence);
                node = indexNode;
            } else if (token.name.equals("(")) {
                // 函数调用
                CallExprNode callNode = new CallExprNode();
                callNode.function = node;
                Vector<ExprNode> arguments = parseFunctionArgs();
                callNode.arguments = arguments;
                node = callNode;
            } else if (token.name.equals("{")) {
                // 结构体表达式
                StructExprNode structNode = new StructExprNode();
                if (node instanceof PathExprNode) {
                    structNode.structName = ((PathExprNode)node).LSeg;
                } else {
                    throw new ParseException("Expected path expression before '{' in struct expression");
                }
                i++;
                Vector<FieldValNode> fieldValues = new Vector<>();
                while (i < tokens.size()) {
                    if (tokens.get(i).name.equals("}")) {
                        break;
                    }
                    fieldValues.add(parseFieldValNode());
                    if (i < tokens.size() && tokens.get(i).name.equals(",")) {
                        i++;
                    } else if (i < tokens.size() && tokens.get(i).name.equals("}")) {
                        break;
                    } else {
                        throw new ParseException("Expected ',' or '}' in field assignment list");
                    }
                }
                node = structNode;
                structNode.fieldValues = fieldValues;
                // expect }
                if (i < tokens.size() && tokens.get(i).name.equals("}")) {
                    i++;
                } else {
                    throw new ParseException("Expected '}' at end of struct expression");
                }
            } else {
                // 表达式结束符或非法token
                if (token.name.equals(")") || token.name.equals("]") || token.name.equals("}") ||
                    token.name.equals(",") || token.name.equals(";")) {
                    break;
                }
                throw new ParseException("Unexpected token '" + token.name + "' in expression");
            }
        }
        
        // 6. 递减递归深度
        recursionDepth--;
        
        // 7. 返回表达式节点
        return node;
    } catch (Exception e) {
        // 确保在异常情况下也递减递归深度
        recursionDepth--;
        throw e;
    }
}
```

### 运算符优先级

根据Parser.java中的getPrecedence()方法，运算符优先级如下（数值越高，优先级越高）：

1. **110**：`*`, `/`, `%` (乘法、除法、取模)
2. **100**：`+`, `-` (加法、减法)
3. **90**：`<<`, `>>` (左移、右移)
4. **80**：`&` (按位与)
5. **70**：`^` (按位异或)
6. **60**：`|` (按位或)
7. **50**：`==`, `!=`, `<`, `<=`, `>`, `>=` (比较运算符)
8. **40**：`&&` (逻辑与)
9. **30**：`||` (逻辑或)
10. **20**：`=`, `+=`, `-=`, `*=`, `/=`, `%=`, `&=`, `|=`, `^=`, `<<=`, `>>=` (赋值运算符)

### 特殊优先级处理

1. **路径扩展(`::`)**：优先级170，高于所有运算符
2. **类型转换(`as`)**：优先级120，高于所有运算符但低于路径扩展
3. **索引访问(`[`)**：使用当前传入的优先级
4. **函数调用(`(`)**：使用当前传入的优先级

### 注意事项

1. **递归深度管理**：使用try-finally确保在异常情况下也能正确递减递归深度
2. **优先级处理**：只有当运算符优先级大于当前优先级时才继续解析
3. **表达式类型检查**：在处理结构体表达式时检查当前表达式是否是PathExprNode
4. **错误处理**：对每种错误情况提供明确的错误消息
5. **表达式结束符**：正确识别表达式结束符以避免过度解析

## parseGroupExprNode() 实现步骤

根据grammar.md的第182-185行，分组表达式的语法规则是：
```
<groupedexpr> ::= ( <expression> )
```

### 实现步骤

1. **解析左括号**
   - 检查当前token是否是左括号"("
   - 如果是，消费左括号
   - 如果不是，抛出断言错误："Expected '(' at start of grouped expression"

2. **创建分组表达式节点**
   - 创建一个新的GroupExprNode对象

3. **解析内部表达式**
   - 调用`parseExprNode()`解析内部表达式
   - 将内部表达式设置到节点的innerExpr属性

4. **解析右括号**
   - 检查当前token是否是右括号")"
   - 如果是，消费右括号
   - 如果不是，抛出断言错误："Expected ')' at end of grouped expression"

5. **返回分组表达式节点**
   - 返回构造的GroupExprNode对象

## parseBorrowExprNode() 实现步骤

根据grammar.md的第192-195行，借用表达式的语法规则是：
```
<borrowexpr> ::= (& | &&) (mut)? <expression>
```

### 实现步骤

1. **创建借用表达式节点**
   - 创建一个新的BorrowExprNode对象

2. **解析引用前缀**
   - 检查当前token是否是"&&"：
     - 如果是，将节点的isDoubleReference属性设置为true并消费"&&"
     - 如果不是，检查当前token是否是"&"：
       - 如果是，将节点的isDoubleReference属性设置为false并消费"&"
       - 如果不是，抛出断言错误："Expected '&' in borrow expression"

3. **解析可变前缀**
   - 检查当前token是否是"mut"关键字
   - 如果是，将节点的isMutable属性设置为true并消费"mut"关键字
   - 如果不是，将节点的isMutable属性设置为false

4. **解析内部表达式**
   - 调用`parseExprWithoutBlockNode(130)`解析内部表达式
   - 将内部表达式设置到节点的innerExpr属性

5. **返回借用表达式节点**
   - 返回构造的BorrowExprNode对象

## parseDerefExprNode() 实现步骤

根据grammar.md的第197-200行，解引用表达式的语法规则是：
```
<derefexpr> ::= * <expression>
```

### 实现步骤

1. **消费星号**
   - 消费当前token（应该是"*"）

2. **创建解引用表达式节点**
   - 创建一个新的DerefExprNode对象

3. **解析内部表达式**
   - 调用`parseExprWithoutBlockNode(130)`解析内部表达式
   - 将内部表达式设置到节点的innerExpr属性

4. **返回解引用表达式节点**
   - 返回构造的DerefExprNode对象

## parseNegaExprNode() 实现步骤

根据grammar.md的第204-207行，否定表达式的语法规则是：
```
<negaexpr> ::= (! | -) <expression>
```

### 实现步骤

1. **创建否定表达式节点**
   - 创建一个新的NegaExprNode对象

2. **解析否定运算符**
   - 检查当前token是否是"-"：
     - 如果是，将节点的isLogical属性设置为false
   - 检查当前token是否是"!"：
     - 如果是，将节点的isLogical属性设置为true
   - 如果都不是，抛出断言错误："Expected '-' or '!' in negation expression"
   - 消费否定运算符

3. **解析内部表达式**
   - 调用`parseExprWithoutBlockNode(130)`解析内部表达式
   - 将内部表达式设置到节点的innerExpr属性

4. **返回否定表达式节点**
   - 返回构造的NegaExprNode对象

## parseLiteralExprNode() 实现步骤

根据grammar.md的第167-170行，字面量表达式的语法规则是：
```
<literalexpr> ::= <char_literal> | <string_literal> | <raw_string_literal> | <c_string_literal> | <raw_c_string_literal> | <integer_literal> | <boolean_literal>
```

### 实现步骤

1. **创建字面量表达式节点**
   - 创建一个新的LiteralExprNode对象

2. **获取当前Token**
   - 获取当前token以确定字面量类型

3. **根据Token类型解析字面量**
   - **整数字面量**：如果token类型是INTEGER_LITERAL：
     - 将节点的literalType设置为literal_t.INT
     - 尝试将token名称解析为整数
     - 将节点的value_int设置为解析的整数
     - 消费token
   - **字符字面量**：如果token类型是CHAR_LITERAL：
     - 将节点的literalType设置为literal_t.CHAR
     - 将节点的value_string设置为token名称
     - 消费token
   - **字符串字面量**：如果token类型是STRING_LITERAL或RAW_STRING_LITERAL：
     - 将节点的literalType设置为literal_t.STRING
     - 将节点的value_string设置为token名称
     - 消费token
   - **C字符串字面量**：如果token类型是C_STRING_LITERAL或RAW_C_STRING_LITERAL：
     - 将节点的literalType设置为literal_t.CSTRING
     - 将节点的value_string设置为token名称
     - 消费token
   - **布尔字面量**：如果token名称是"true"：
     - 将节点的literalType设置为literal_t.BOOL
     - 将节点的value_bool设置为true
     - 消费token
   - **布尔字面量**：如果token名称是"false"：
     - 将节点的literalType设置为literal_t.BOOL
     - 将节点的value_bool设置为false
     - 消费token
   - 如果都不是，抛出断言错误："Expected literal in literal expression"

4. **返回字面量表达式节点**
   - 返回构造的LiteralExprNode对象

## parsePathExprNode() 实现步骤

根据grammar.md的第172-175行，路径表达式的语法规则是：
```
<pathexpr> ::= <pathseg> (:: <pathseg>)?
```

### 实现步骤

1. **创建路径表达式节点**
   - 创建一个新的PathExprNode对象

2. **解析左路径段**
   - 检查是否有可用于解析的token
   - 如果有，调用`parsePathExprSegNode()`解析左路径段
   - 将左路径段设置到节点的LSeg属性
   - 如果没有token，抛出断言错误："Expected path segment in path expression"

3. **解析可选的右路径段**
   - 检查当前token是否是"::"
   - 如果是：
     - 消费"::" token
     - 调用`parsePathExprSegNode()`解析右路径段
     - 将右路径段设置到节点的RSeg属性
   - 如果不是，将右路径段设置为null

4. **返回路径表达式节点**
   - 返回构造的PathExprNode对象

## parsePathExprSegNode() 实现步骤

根据grammar.md的第177-180行，路径段的语法规则是：
```
<pathseg> ::= <identifier> | self | Self
```

### 实现步骤

1. **创建路径段节点**
   - 创建一个新的PathExprSegNode对象

2. **根据Token解析路径段**
   - **标识符**：如果当前token是标识符：
     - 将节点的patternType设置为patternSeg_t.IDENT
     - 调用`parseIdentifierNode()`解析标识符
     - 将节点的name设置为解析的标识符
     - 消费token
   - **Self**：如果当前token是"self"：
     - 将节点的patternType设置为patternSeg_t.SELF
     - 消费"self" token
   - **Self类型**：如果当前token是"Self"：
     - 将节点的patternType设置为patternSeg_t.SELF_TYPE
     - 消费"Self" token
   - 如果都不是，抛出断言错误："Expected identifier, 'self' or 'Self' in path segment"

3. **返回路径段节点**
   - 返回构造的PathExprSegNode对象

## parseArrayExprNode() 实现步骤

根据grammar.md的第237-244行，数组表达式的语法规则是：
```
<arrayexpr> ::= [ (<elements> | <repeated_element>; <size>)? ]

<elements> ::= <expression> (, <expression>)* ,?
<repeated_element> ::= <expression>
<size> ::= <expression>
```

### 实现步骤

1. **消费左括号**
   - 消费当前token（应该是"["）

2. **创建数组表达式节点**
   - 创建一个新的ArrayExprNode对象

3. **检查空数组**
   - 如果当前token是右括号"]"：
     - 消费右括号
     - 将元素列表设置为null
     - 返回ArrayExprNode

4. **确定数组类型**
    - 使用`parseExprNode()`解析第一个元素
    - 检查当前token是否是分号";"：
      - 如果是，它是重复元素数组：
        - 消费分号
        - 使用`parseExprNode()`解析大小
        - 将重复元素和大小设置到节点
      - 如果不是，它是常规元素数组：
        - 创建元素向量
        - 将第一个元素添加到向量
        - 循环解析附加元素：
          - 检查当前token是否是逗号","
          - 如果是，消费逗号并检查下一个token：
            - 如果下一个token是右括号"]"，表示有尾随逗号，跳出循环
            - 否则，解析下一个元素并将元素添加到向量
        - 将元素列表设置到节点

5. **解析右括号**
   - 检查当前token是否是右括号"]"
   - 如果是，消费右括号
   - 如果不是，抛出断言错误："Expected ']' at end of array expression"

6. **返回数组表达式节点**
   - 返回构造的ArrayExprNode对象

## parseIndexExprNode() 实现步骤

根据grammar.md的第246-249行，索引表达式的语法规则是：
```
<indexexpr> ::= <expression> [ <expression> ]
```

### 重要说明：与parseExprWithoutBlockNode()的冲突

**问题**：`parseIndexExprNode()`的实现与`parseExprWithoutBlockNode()`中的索引表达式处理存在严重冲突。

**具体冲突**：
1. **Token消费冲突**：
   - `parseExprWithoutBlockNode()`在第785-797行已经处理了索引表达式作为后缀操作
   - `parseIndexExprNode()`也会尝试消费`[`token，导致token被重复消费
   - 如果`parseExprWithoutBlockNode()`先解析索引表达式，`parseIndexExprNode()`将找不到`[`token

2. **解析逻辑不一致**：
   - `parseExprWithoutBlockNode()`将索引视为后缀操作，基于已解析的表达式
   - `parseIndexExprNode()`将索引视为独立表达式，需要重新解析数组表达式
   - 两种方式可能导致不同的解析结果和AST结构

3. **调用关系混乱**：
   - `parseExprWithoutBlockNode()`已经包含了完整的索引表达式解析逻辑
   - `parseIndexExprNode()`似乎是一个冗余函数，可能导致重复解析

### 解决方案

#### 方案1：移除parseIndexExprNode()函数
- **优点**：消除冲突，简化代码结构
- **实现**：删除`parseIndexExprNode()`函数，所有索引表达式由`parseExprWithoutBlockNode()`处理
- **影响**：需要检查是否有其他地方直接调用`parseIndexExprNode()`

#### 方案2：修改parseExprWithoutBlockNode()调用parseIndexExprNode()
- **优点**：保持函数分离，明确职责
- **实现**：修改`parseExprWithoutBlockNode()`第785-797行，改为调用`parseIndexExprNode()`
- **代码示例**：
```java
} else if (token.name.equals("[")) {
    // 不直接解析，而是调用parseIndexExprNode()
    // 注意：需要回溯到表达式开始位置
    int exprStart = findExpressionStart(node); // 需要实现此辅助函数
    i = exprStart;
    return parseIndexExprNode();
}
```

#### 方案3：重构为统一的索引表达式处理
- **优点**：最清晰的解决方案，避免重复代码
- **实现**：
  1. 创建`parseIndexExpression(ExprWithoutBlockNode arrayExpr)`辅助函数
  2. `parseExprWithoutBlockNode()`调用此辅助函数
  3. `parseIndexExprNode()`也调用此辅助函数
- **代码示例**：
```java
// 辅助函数
private IndexExprNode parseIndexExpression(ExprWithoutBlockNode arrayExpr) {
    assert tokens.get(i).name.equals("[") : "Expected '[' in index expression";
    i++; // 消费[
    
    IndexExprNode indexNode = new IndexExprNode();
    indexNode.array = arrayExpr;
    indexNode.index = parseExprNode();
    
    assert tokens.get(i).name.equals("]") : "Expected ']' at end of index expression";
    i++; // 消费]
    
    return indexNode;
}

// 在parseExprWithoutBlockNode()中使用
} else if (token.name.equals("[")) {
    node = parseIndexExpression(node);
}

// 在parseIndexExprNode()中使用
public IndexExprNode parseIndexExprNode() {
    ExprWithoutBlockNode arrayExpr = parseExprWithoutBlockNode(140);
    return parseIndexExpression(arrayExpr);
}
```

### 推荐方案

**推荐方案1（移除parseIndexExprNode()）**，因为：
1. `parseExprWithoutBlockNode()`已经完整实现了索引表达式解析
2. 索引表达式在语法上是后缀操作，不是独立表达式
3. 移除冗余函数可以简化代码结构，减少维护负担

### 实现步骤（如果采用方案1）

1. **搜索所有parseIndexExprNode()的调用**
   - 在Parser.java中搜索所有调用`parseIndexExprNode()`的地方
   - 确定这些调用是否必要

2. **修改调用点**
   - 如果有其他地方调用`parseIndexExprNode()`，修改为调用`parseExprWithoutBlockNode()`
   - 确保修改后的代码能正确处理索引表达式

3. **删除parseIndexExprNode()函数**
   - 从Parser.java中删除整个`parseIndexExprNode()`函数
   - 更新相关文档和注释

4. **测试验证**
   - 运行所有测试用例，确保索引表达式解析正确
   - 特别测试复杂的索引表达式，如嵌套索引

### 实现步骤（如果采用方案3）

1. **创建辅助函数**
   - 在Parser.java中添加`parseIndexExpression(ExprWithoutBlockNode arrayExpr)`辅助函数
   - 实现索引表达式的核心解析逻辑

2. **修改parseExprWithoutBlockNode()**
   - 将第785-797行的索引表达式解析代码替换为调用`parseIndexExpression(node)`

3. **修改parseIndexExprNode()**
   - 重写`parseIndexExprNode()`函数，使用`parseIndexExpression()`辅助函数

4. **测试验证**
   - 运行所有测试用例，确保两种解析方式结果一致
   - 验证没有token消费冲突

### 注意事项

1. **优先级处理**：确保索引表达式的优先级（140）在两种实现中保持一致
2. **错误处理**：统一错误消息和异常处理方式
3. **AST结构**：确保生成的AST节点结构一致
4. **性能影响**：评估重构对解析性能的影响

### 原实现步骤（仅供参考，不推荐直接使用）

1. **解析数组表达式**
    - 使用`parseExprWithoutBlockNode(140)`解析数组表达式

2. **解析左括号**
    - 检查当前token是否是左括号"["
    - 如果是，消费左括号
    - 如果不是，抛出断言错误："Expected '[' in index expression"

3. **解析索引表达式**
    - 使用`parseExprNode()`解析索引表达式

4. **解析右括号**
    - 检查当前token是否是右括号"]"
    - 如果是，消费右括号
    - 如果不是，抛出断言错误："Expected ']' at end of index expression"

5. **创建索引表达式节点**
    - 创建一个新的IndexExprNode对象
    - 将数组表达式设置到节点的array属性
    - 将索引表达式设置到节点的index属性

6. **返回索引表达式节点**
    - 返回构造的IndexExprNode对象

## parseStructExprNode() 实现步骤

根据grammar.md的第251-257行，结构体表达式的语法规则是：
```
<structexpr> ::= <pathseg> { <fieldvals>? }

<fieldvals> ::= <fieldval> (, <fieldval>)* ,?
<fieldval> ::= <identifier> : <expression>
```

### 实现步骤

1. **创建结构体表达式节点**
    - 创建一个新的StructExprNode对象

2. **解析结构体名**
    - 使用`parsePathExprNode()`解析结构体名
    - 将结构体名设置到节点的name属性

3. **解析左大括号**
    - 检查当前token是否是左大括号"{"
    - 如果是，消费左大括号
    - 如果不是，抛出断言错误："Expected '{' in struct expression"

4. **检查空结构体**
   - 如果当前token是右大括号"}"：
     - 消费右大括号
     - 将字段值列表设置为null
     - 返回StructExprNode

5. **解析字段值**
   - 创建字段值向量
   - 循环解析字段值直到遇到右大括号"}"：
     - 使用`parseIdentifierNode()`解析字段名
     - 解析冒号":"
     - 使用`parseExprNode()`解析字段值
     - 使用名称和值创建一个新的FieldValNode
     - 将字段值添加到向量
     - 在每个字段值后检查逗号分隔符（除了最后一个）

6. **解析右大括号**
   - 检查当前token是否是右大括号"}"
   - 如果是，消费右大括号
   - 如果不是，抛出断言错误："Expected '}' at end of struct expression"

7. **返回结构体表达式节点**
   - 返回构造的StructExprNode对象

## parseCallExprNode() 实现步骤

根据grammar.md的第259-264行，函数调用表达式的语法规则是：
```
<callexpr> ::= <expression> ( <arguments>? )

<arguments> ::= <expression> (, <expression>)* ,?
```

### 实现步骤

1. **解析函数表达式**
    - 使用`parseExprWithoutBlockNode(160)`解析函数表达式

2. **解析左括号**
    - 检查当前token是否是左括号"("
    - 如果是，消费左括号
    - 如果不是，抛出断言错误："Expected '(' in function call expression"

3. **创建调用表达式节点**
    - 创建一个新的CallExprNode对象
    - 将函数表达式设置到节点的function属性

4. **检查空参数**
    - 如果当前token是右括号")"：
      - 消费右括号
      - 将参数列表设置为null
      - 返回CallExprNode

5. **解析参数**
    - 创建参数向量
    - 使用`parseExprNode()`解析第一个参数
    - 将参数添加到向量
    - 循环解析附加参数：
      - 检查当前token是否是逗号","
      - 如果是，消费逗号并解析下一个参数
      - 将参数添加到向量
    - 将参数列表设置到节点的arguments属性

6. **解析右括号**
    - 检查当前token是否是右括号")"
    - 如果是，消费右括号
    - 如果不是，抛出断言错误："Expected ')' at end of function call expression"

7. **返回调用表达式节点**
    - 返回构造的CallExprNode对象

## parseMethodCallExprNode() 实现步骤

根据grammar.md的第266-271行，方法调用表达式的语法规则是：
```
<methodcallexpr> ::= <expression> . <pathseg> ( <arguments>? )

<arguments> ::= <expression> (, <expression>)* ,?
```

### 实现步骤

1. **解析接收者表达式**
    - 使用`parseExprWithoutBlockNode(160)`解析接收者表达式

2. **解析点**
    - 检查当前token是否是点"."
    - 如果是，消费点
    - 如果不是，抛出断言错误："Expected '.' in method call expression"

3. **解析方法名**
    - 使用`parsePathExprSegNode()`解析方法名

4. **解析左括号**
    - 检查当前token是否是左括号"("
    - 如果是，消费左括号
    - 如果不是，抛出断言错误："Expected '(' in method call expression"

5. **创建方法调用表达式节点**
    - 创建一个新的MethodCallExprNode对象
    - 将接收者表达式设置到节点的receiver属性
    - 将方法名设置到节点的method属性

6. **检查空参数**
    - 如果当前token是右括号")"：
      - 消费右括号
      - 将参数列表设置为null
      - 返回MethodCallExprNode

7. **解析参数**
    - 创建参数向量
    - 使用`parseExprNode()`解析第一个参数
    - 将参数添加到向量
    - 循环解析附加参数：
      - 检查当前token是否是逗号","
      - 如果是，消费逗号并解析下一个参数
      - 将参数添加到向量
    - 将参数列表设置到节点的arguments属性

8. **解析右括号**
    - 检查当前token是否是右括号")"
    - 如果是，消费右括号
    - 如果不是，抛出断言错误："Expected ')' at end of method call expression"

9. **返回方法调用表达式节点**
    - 返回构造的MethodCallExprNode对象

## parseFieldExprNode() 实现步骤

根据grammar.md的第273-276行，字段访问表达式的语法规则是：
```
<fieldexpr> ::= <expression> . <identifier>
```

### 实现步骤

1. **解析接收者表达式**
   - 使用`parseExprWithoutBlockNode(160)`解析接收者表达式
   - 将接收者表达式设置到节点的receiver属性

2. **解析点**
   - 检查当前token是否是点"."
   - 如果是，消费点
   - 如果不是，抛出断言错误："Expected '.' in field access expression"

3. **创建字段表达式节点**
   - 创建一个新的FieldExprNode对象
   - 将接收者表达式设置到节点

4. **解析字段名**
   - 使用`parseIdentifierNode()`解析字段名
   - 将字段名设置到节点的field属性

5. **返回字段表达式节点**
   - 返回构造的FieldExprNode对象

## parseContinueExprNode() 实现步骤

根据grammar.md的第278-281行，continue表达式的语法规则是：
```
<continueexpr> ::= continue
```

### 实现步骤

1. **消费continue关键字**
   - 消费当前token（应该是"continue"）

2. **创建Continue表达式节点**
   - 创建一个新的ContinueExprNode对象

3. **返回Continue表达式节点**
   - 返回构造的ContinueExprNode对象

## parseBreakExprNode() 实现步骤

根据grammar.md的第283-286行，break表达式的语法规则是：
```
<breakexpr> ::= break (<expression>)?
```

### 实现步骤

1. **消费break关键字**
   - 消费当前token（应该是"break"）

2. **创建Break表达式节点**
   - 创建一个新的BreakExprNode对象

3. **解析Break值（可选）**
    - 检查当前token是否是表达式（不是分号或右大括号）
    - 如果是：
      - 使用`parseExprNode()`解析break值
      - 将值设置到节点的value属性
    - 如果不是，将值设置为null

4. **返回Break表达式节点**
   - 返回构造的BreakExprNode对象

## parseReturnExprNode() 实现步骤

根据grammar.md的第288-291行，return表达式的语法规则是：
```
<returnexpr> ::= return (<expression>)?
```

### 实现步骤

1. **消费return关键字**
   - 消费当前token（应该是"return"）

2. **创建Return表达式节点**
   - 创建一个新的ReturnExprNode对象

3. **解析Return值（可选）**
    - 检查当前token是否是表达式（不是分号或右大括号）
    - 如果是：
      - 使用`parseExprNode()`解析return值
      - 将值设置到节点的value属性
    - 如果不是，将值设置为null

4. **返回Return表达式节点**
   - 返回构造的ReturnExprNode对象

## parseUnderscoreExprNode() 实现步骤

根据grammar.md的第293-296行，下划线表达式的语法规则是：
```
<underscoreexpr> ::= _
```

### 实现步骤

1. **消费下划线**
   - 消费当前token（应该是"_"）

2. **创建下划线表达式节点**
   - 创建一个新的UnderscoreExprNode对象

3. **返回下划线表达式节点**
   - 返回构造的UnderscoreExprNode对象

## parseArithExprNode() 实现步骤

根据grammar.md的第207-210行，算术表达式的语法规则是：
```
<arithexpr> ::= <expression> (+ | - | * | / | % | & | | | ^ | << | >>) <expression>
```

### 实现步骤

1. **解析左操作数**
   - 使用`parseExprWithoutBlockNode(precedence)`解析左操作数
   - 将左操作数设置到节点的left属性

2. **解析运算符**
   - 检查当前token是否是算术运算符：
     - 如果是，确定运算符类型并将其设置到节点的op属性
     - 消费运算符token
   - 如果不是，抛出断言错误："Expected arithmetic operator in arithmetic expression"

3. **解析右操作数**
   - 使用`parseExprWithoutBlockNode(precedence)`解析右操作数
   - 将右操作数设置到节点的right属性

4. **创建算术表达式节点**
   - 使用左操作数、运算符和右操作数创建一个新的ArithExprNode对象

5. **返回算术表达式节点**
   - 返回构造的ArithExprNode对象

## parseCompExprNode() 实现步骤

根据grammar.md的第212-215行，比较表达式的语法规则是：
```
<compexpr> ::= <expression> (== | != | > | < | >= | <=) <expression>
```

### 实现步骤

1. **解析左操作数**
   - 使用`parseExprWithoutBlockNode(precedence)`解析左操作数
   - 将左操作数设置到节点的left属性

2. **解析运算符**
   - 检查当前token是否是比较运算符：
     - 如果是，确定运算符类型并将其设置到节点的op属性
     - 消费运算符token
   - 如果不是，抛出断言错误："Expected comparison operator in comparison expression"

3. **解析右操作数**
   - 使用`parseExprWithoutBlockNode(precedence)`解析右操作数
   - 将右操作数设置到节点的right属性

4. **创建比较表达式节点**
   - 使用左操作数、运算符和右操作数创建一个新的CompExprNode对象

5. **返回比较表达式节点**
   - 返回构造的CompExprNode对象

## parseLazyExprNode() 实现步骤

根据grammar.md的第217-220行，惰性布尔表达式的语法规则是：
```
<lazyexpr> ::= <expression> (&& | ||) <expression>
```

### 实现步骤

1. **解析左操作数**
   - 使用`parseExprWithoutBlockNode(precedence)`解析左操作数
   - 将左操作数设置到节点的left属性

2. **解析运算符**
   - 检查当前token是否是惰性布尔运算符：
     - 如果是，确定运算符类型并将其设置到节点的op属性
     - 消费运算符token
   - 如果不是，抛出断言错误："Expected lazy boolean operator in lazy expression"

3. **解析右操作数**
   - 使用`parseExprWithoutBlockNode(precedence)`解析右操作数
   - 将右操作数设置到节点的right属性

4. **创建惰性表达式节点**
   - 使用左操作数、运算符和右操作数创建一个新的LazyExprNode对象

5. **返回惰性表达式节点**
   - 返回构造的LazyExprNode对象

## parseTypeCastExprNode() 实现步骤

根据grammar.md的第222-225行，类型转换表达式的语法规则是：
```
<typecastexpr> ::= <expression> as <type>
```

### 实现步骤

1. **解析表达式**
   - 使用`parseExprWithoutBlockNode(precedence)`解析表达式
   - 将表达式设置到节点的expr属性

2. **解析as关键字**
   - 检查当前token是否是"as"关键字
   - 如果是，消费"as"关键字
   - 如果不是，抛出断言错误："Expected 'as' in type cast expression"

3. **解析目标类型**
   - 使用`parseTypeExprNode()`解析目标类型
   - 将目标类型设置到节点的type属性

4. **创建类型转换表达式节点**
   - 使用表达式和目标类型创建一个新的TypeCastExprNode对象

5. **返回类型转换表达式节点**
   - 返回构造的TypeCastExprNode对象

## parseAssignExprNode() 实现步骤

根据grammar.md的第227-230行，赋值表达式的语法规则是：
```
<assignexpr> ::= <expression> = <expression>
```

### 实现步骤

1. **解析左操作数**
   - 使用`parseExprWithoutBlockNode(precedence)`解析左操作数
   - 将左操作数设置到节点的left属性

2. **解析赋值运算符**
   - 检查当前token是否是等号"="
   - 如果是，消费等号
   - 如果不是，抛出断言错误："Expected '=' in assignment expression"

3. **解析右操作数**
   - 使用`parseExprWithoutBlockNode(precedence)`解析右操作数
   - 将右操作数设置到节点的right属性

4. **创建赋值表达式节点**
   - 使用左操作数和右操作数创建一个新的AssignExprNode对象

5. **返回赋值表达式节点**
   - 返回构造的AssignExprNode对象

## parseComAssignExprNode() 实现步骤

根据grammar.md的第232-235行，复合赋值表达式的语法规则是：
```
<comassignexpr> ::= <expression> (+= | -= | *= | /= | %= | &= | |= | ^= | <<= | >>=) <expression>
```

### 实现步骤

1. **解析左操作数**
   - 使用`parseExprWithoutBlockNode(precedence)`解析左操作数
   - 将左操作数设置到节点的left属性

2. **解析复合赋值运算符**
   - 检查当前token是否是复合赋值运算符：
     - 如果是，确定运算符类型并将其设置到节点的op属性
     - 消费运算符token
   - 如果不是，抛出断言错误："Expected compound assignment operator in compound assignment expression"

3. **解析右操作数**
   - 使用`parseExprWithoutBlockNode(precedence)`解析右操作数
   - 将右操作数设置到节点的right属性

4. **创建复合赋值表达式节点**
   - 使用左操作数、运算符和右操作数创建一个新的ComAssignExprNode对象

5. **返回复合赋值表达式节点**
   - 返回构造的ComAssignExprNode对象

## parseTypeExprNode() 实现步骤

根据grammar.md的第300-303行，类型表达式的语法规则是：
```
<type> ::= <typepathexpr> | <typerefexpr> | <typearrayexpr> | <typeunitexpr>
```

### 实现步骤

1. **输入验证**
   - 检查是否有可用于解析的token
   - 如果没有token，抛出断言错误："No more tokens to parse in type expression"

2. **获取当前Token**
   - 获取当前token以确定类型表达式类型

3. **区分类型表达式类型**
    - 如果当前token是左括号"("：
      - 调用`parseTypeUnitExprNode()`解析单元类型表达式
    - 如果当前token是"&"：
      - 调用`parseTypeRefExprNode()`解析引用类型表达式
    - 如果当前token是左括号"["：
      - 调用`parseTypeArrayExprNode()`解析数组类型表达式
    - 否则：
      - 调用`parseTypePathExprNode()`解析路径类型表达式

4. **返回类型表达式节点**
   - 返回构造的TypeExprNode对象

## parseTypePathExprNode() 实现步骤

根据grammar.md的第305-308行，路径类型表达式的语法规则是：
```
<typepathexpr> ::= <pathseg>
```

### 实现步骤

1. **创建路径类型表达式节点**
   - 创建一个新的TypePathExprNode对象

2. **解析路径段**
   - 使用`parsePathExprSegNode()`解析路径段
   - 将路径段设置到节点的path属性

3. **返回路径类型表达式节点**
   - 返回构造的TypePathExprNode对象

## parseTypeRefExprNode() 实现步骤

根据grammar.md的第310-313行，引用类型表达式的语法规则是：
```
<typerefexpr> ::= & (mut)? <type>
```

### 实现步骤

1. **消费&符号**
   - 消费当前token（应该是"&"）

2. **创建引用类型表达式节点**
   - 创建一个新的TypeRefExprNode对象

3. **解析可变前缀**
   - 检查当前token是否是"mut"关键字
   - 如果是，将节点的isMutable属性设置为true并消费"mut"关键字
   - 如果不是，将节点的isMutable属性设置为false

4. **解析内部类型**
   - 使用`parseTypeExprNode()`解析内部类型
   - 将内部类型设置到节点的innerType属性

5. **返回引用类型表达式节点**
   - 返回构造的TypeRefExprNode对象

## parseTypeArrayExprNode() 实现步骤

根据grammar.md的第315-318行，数组类型表达式的语法规则是：
```
<typearrayexpr> ::= [ <type> ; <expression> ]
```

### 实现步骤

1. **消费左括号**
   - 消费当前token（应该是"["）

2. **创建数组类型表达式节点**
   - 创建一个新的TypeArrayExprNode对象

3. **解析元素类型**
   - 使用`parseTypeExprNode()`解析元素类型
   - 将元素类型设置到节点的elementType属性

4. **解析分号**
   - 检查当前token是否是分号";"
   - 如果是，消费分号
   - 如果不是，抛出断言错误："Expected ';' in array type expression"

5. **解析数组大小**
   - 使用`parseExprNode()`解析数组大小
   - 将数组大小设置到节点的size属性

6. **解析右括号**
   - 检查当前token是否是右括号"]"
   - 如果是，消费右括号
   - 如果不是，抛出断言错误："Expected ']' at end of array type expression"

7. **返回数组类型表达式节点**
   - 返回构造的TypeArrayExprNode对象

## parseTypeUnitExprNode() 实现步骤

根据grammar.md的第320-323行，单元类型表达式的语法规则是：
```
<typeunitexpr> ::= ()
```

### 实现步骤

1. **消费左括号**
   - 检查当前token是否是左括号"("
   - 如果是，消费左括号
   - 如果不是，抛出断言错误："Expected '(' at start of unit type expression"

2. **创建单元类型表达式节点**
   - 创建一个新的TypeUnitExprNode对象

3. **解析右括号**
   - 检查当前token是否是右括号")"
   - 如果是，消费右括号
   - 如果不是，抛出断言错误："Expected ')' at end of unit type expression"

4. **返回单元类型表达式节点**
   - 返回构造的TypeUnitExprNode对象