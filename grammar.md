# Grammar for the RCompiler Language

This document describes the grammar for the language implemented in RCompiler, based on the AST node definitions.

## Table of Contents

1. [Basic Structure](#basic-structure)
2. [Statements](#statements)
3. [Items](#items)
4. [Patterns](#patterns)
5. [Expressions](#expressions)
6. [Types](#types)
7. [Literals and Identifiers](#literals-and-identifiers)

## Basic Structure

### Program
```
<program> ::= <statement>*
```

## Statements

### Statement
```
<statement> ::= <item> | <letstmt> | <exprstmt> | ;
```

### Let Statement
```
<letstmt> ::= let <pattern> : <type> (= <expression>)? ;
```

### Expression Statement
```
<exprstmt> ::= <exprwithblock> ;? | <exprwithoutblock> ;
```

## Items

### Item
```
<item> ::= <function> | <structitem> | <enumitem> | <constitem> | <traititem> | <implitem>
```

### Function
```
<function> ::= (const)? fn <identifier> (<parameters>?) (-> <type>)? ( <blockexpr> | ; )

<parameters> ::= <selfpara> ,? | (<selfpara> ,)? <parameter> (, <parameter>)* ,?
```

### Self Parameter
```
<selfpara> ::= <shortself> | <typedself>
<shortself> ::= &? (mut)? self
<typedself> ::= (mut)? self : <type>
```

### Parameter
```
<parameter> ::= <pattern> : <type>
```

### Struct Item
```
<structitem> ::= struct <identifier> ({ <fields>? } | ;)

<fields> ::= <field> (, <field>)* ,?
```

### Field
```
<field> ::= <identifier> : <type> ;
```

### Enum Item
```
<enumitem> ::= enum <identifier> { <enum_variants>? }

<enum_variants> ::= <identifier> (, <identifier>)* ,?
```

### Constant Item
```
<constitem> ::= const <identifier> : <type> (= <expression>)? ;
```

### Trait Item
```
<traititem> ::= trait <identifier> { <asso_item>* }
```

### Associated Item
```
<asso_item> ::= <function> | <constitem>
```

### Implementation Item
```
<implitem> ::= <inherentimplitem> | <traitimplitem>

<inherentimplitem> ::= impl <type> { <asso_item>* }
<traitimplitem> ::= impl <identifier> for <type> { <asso_item>* }
```

## Patterns

### Pattern
```
<pattern> ::= <idpat> | <wildpat> | <refpat>
```

### Identifier Pattern
```
<idpat> ::= (ref)? (mut)? <identifier>
```

### Wildcard Pattern
```
<wildpat> ::= _
```

### Reference Pattern
```
<refpat> ::= (& | &&) (mut)? <pattern>
```

## Expressions

### Expression
```
<expression> ::= <exprwithblock> | <exprwithoutblock>
```

### Expression With Block
```
<exprwithblock> ::= <blockexpr> | <ifexpr> | <loopexpr>
```

### Expression Without Block
```
<exprwithoutblock> ::= <literalexpr> | <pathexpr> | <operexpr> | <arrayexpr> | <indexexpr> | <structexpr> | <callexpr> | <methodcallexpr> | <fieldexpr> | <continueexpr> | <breakexpr> | <returnexpr> | <underscoreexpr> | <groupedexpr>
```

### Block Expression
```
<blockexpr> ::= { <statements>* }

<statements> ::= <statement>*
```

### If Expression
```
<ifexpr> ::= if <expression except structexpr> <blockexpr> (else (<ifexpr> | <blockexpr>))?
```

### Loop Expression
```
<loopexpr> ::= <infinite_loop> | <conditional_loop>

<infinite_loop> ::= loop <blockexpr>
<conditional_loop> ::= while <expression except structexpr> <blockexpr>
```

### Literal Expression
```
<literalexpr> ::= <char_literal> | <string_literal> | <raw_string_literal> | <c_string_literal> | <raw_c_string_literal> | <integer_literal> | <boolean_literal>
```

### Path Expression
```
<pathexpr> ::= <pathseg> (:: <pathseg>)?
```

### Path Segment
```
<pathseg> ::= <identifier> | self | Self
```

### Grouped Expression
```
<groupedexpr> ::= ( <expression> )
```

### Operator Expression
```
<operexpr> ::= <borrowexpr> | <derefexpr> | <negaexpr> | <arithexpr> | <compexpr> | <lazyexpr> | <typecastexpr> | <assignexpr> | <comassignexpr>
```

### Borrow Expression
```
<borrowexpr> ::= (& | &&) (mut)? <expression>
```

### Dereference Expression
```
<derefexpr> ::= * <expression>
```

### Negation Expression
```
<negaexpr> ::= (! | -) <expression>
```

### Arithmetic Expression
```
<arithexpr> ::= <expression> (+ | - | * | / | % | & | | | ^ | << | >>) <expression>
```

### Comparison Expression
```
<compexpr> ::= <expression> (== | != | > | < | >= | <=) <expression>
```

### Lazy Expression
```
<lazyexpr> ::= <expression> (&& | ||) <expression>
```

### Type Cast Expression
```
<typecastexpr> ::= <expression> as <type>
```

### Assignment Expression
```
<assignexpr> ::= <expression> = <expression>
```

### Compound Assignment Expression
```
<comassignexpr> ::= <expression> (+= | -= | *= | /= | %= | &= | |= | ^= | <<= | >>=) <expression>
```

### Array Expression
```
<arrayexpr> ::= [ (<elements> | <repeated_element>; <size>)? ]

<elements> ::= <expression> (, <expression>)* ,?
<repeated_element> ::= <expression>
<size> ::= <expression>
```

### Index Expression
```
<indexexpr> ::= <expression> [ <expression> ]
```

### Struct Expression
```
<structexpr> ::= <pathseg> { <fieldvals>? }

<fieldvals> ::= <fieldval> (, <fieldval>)* ,?
<fieldval> ::= <identifier> : <expression>
```

### Function Call Expression
```
<callexpr> ::= <expression> ( <arguments>? )

<arguments> ::= <expression> (, <expression>)* ,?
```

### Method Call Expression
```
<methodcallexpr> ::= <expression> . <pathseg> ( <arguments>? )

<arguments> ::= <expression> (, <expression>)* ,?
```

### Field Access Expression
```
<fieldexpr> ::= <expression> . <identifier>
```

### Continue Expression
```
<continueexpr> ::= continue
```

### Break Expression
```
<breakexpr> ::= break (<expression>)?
```

### Return Expression
```
<returnexpr> ::= return (<expression>)?
```

### Underscore Expression
```
<underscoreexpr> ::= _
```

## Types

### Type Expression
```
<type> ::= <typepathexpr> | <typerefexpr> | <typearrayexpr> | <typeunitexpr>
```

### Path Type Expression
```
<typepathexpr> ::= <pathseg>
```

### Reference Type Expression
```
<typerefexpr> ::= & (mut)? <type>
```

### Array Type Expression
```
<typearrayexpr> ::= [ <type> ; <expression> ]
```

### Unit Type Expression
```
<typeunitexpr> ::= ()
```

## Literals and Identifiers

### Identifier
```
<identifier> ::= [a-zA-Z_][a-zA-Z0-9_]*
```

### Literals
- **Character Literal**: `'c'`
- **String Literal**: `"string"`
- **Raw String Literal**: `r"string"`
- **C String Literal**: `c"string"`
- **Raw C String Literal**: `cr"string"`
- **Integer Literal**: `[0-9]+`
- **Boolean Literal**: `true` | `false`