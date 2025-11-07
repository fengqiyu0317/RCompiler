# Grammar for the RCompiler Language

This document describes the grammar for the language implemented in RCompiler, based on the AST node definitions.

## Table of Contents

1. [Basic Structure](#basic-structure)
2. [Statements](#statements)
3. [Items](#items)
4. [Patterns](#patterns)
5. [Expressions](#expressions)
6. [Types](#types)
7. [Tokens](#tokens)
8. [Literals and Identifiers](#literals-and-identifiers)

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
<blockexpr> ::= { <statements>? }

<statements> ::= <statement>+
               | <statement>+ <expressionwithoutblock>
               | <expressionwithoutblock>
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

## Tokens

### Token
```
<token> ::= <identifier_or_keyword> | <char_literal> | <string_literal> | <raw_string_literal> | <c_string_literal> | <raw_c_string_literal> | <integer_literal> | <punctuation>
```

### Suffix
```
<suffix> ::= <identifier_or_keyword>

<suffix_no_e> ::= <suffix> (not beginning with 'e' or 'E')
```

### Character Literal
```
<char_literal> ::= ' ( ~[' \ <lf> <cr> <tab>] | <quote_escape> | <ascii_escape> ) '

<quote_escape> ::= \' | \"

<ascii_escape> ::= \x <oct_digit> <hex_digit>
                | \n | \r | \t | \\ | \0
```

### String Literal
```
<string_literal> ::= " ( ~[" \ <cr>] | <quote_escape> | <ascii_escape> | <string_continue> )* "

<string_continue> ::= \ <lf>
```

### Raw String Literal
```
<raw_string_literal> ::= r <raw_string_content>

<raw_string_content> ::= " ( ~<cr> )* " (non-greedy)
                       | # <raw_string_content> #
```

### C String Literal
```
<c_string_literal> ::= c" ( ~[" \ <cr> <nul>] | <byte_escape> | <string_continue> )* "

<byte_escape> ::= \x <oct_digit> <hex_digit>
```

### Raw C String Literal
```
<raw_c_string_literal> ::= cr <raw_c_string_content>

<raw_c_string_content> ::= " ( ~[<cr> <nul>] )* " (non-greedy)
                          | # <raw_c_string_content> #
```

### Integer Literal
```
<integer_literal> ::= <dec_literal> | <bin_literal> | <oct_literal> | <hex_literal> <suffix_no_e>?

<dec_literal> ::= <dec_digit> ( <dec_digit> | _ )*

<bin_literal> ::= 0b ( <bin_digit> | _ )* <bin_digit> ( <bin_digit> | _ )*

<oct_literal> ::= 0o ( <oct_digit> | _ )* <oct_digit> ( <oct_digit> | _ )*

<hex_literal> ::= 0x ( <hex_digit> | _ )* <hex_digit> ( <hex_digit> | _ )*

<bin_digit> ::= 0 | 1

<oct_digit> ::= 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7

<dec_digit> ::= 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9

<hex_digit> ::= <dec_digit> | a | b | c | d | e | f | A | B | C | D | E | F
```

### Punctuation
```
<punctuation> ::= = | + | - | * | / | % | ^ | ! | & | | | && | || | << | >>
                | == | != | > | < | >= | <= | @ | . | .. | ... | ..= | : | ::
                | ; | , | -> | <- | => | # | $ | ? | [ | ] | ( | ) | { | } | _
```

## Literals and Identifiers

### Identifier
```
<identifier> ::= [a-zA-Z_][a-zA-Z0-9_]*
```

### Literals
- **Character Literal**: `<char_literal>`
- **String Literal**: `<string_literal>`
- **Raw String Literal**: `<raw_string_literal>`
- **C String Literal**: `<c_string_literal>`
- **Raw C String Literal**: `<raw_c_string_literal>`
- **Integer Literal**: `<integer_literal>`
- **Boolean Literal**: `true` | `false`

