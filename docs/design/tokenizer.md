# Tokenizer Design Document

## Overview

The Tokenizer is a fundamental component of the RCompiler project, responsible for converting raw source code input into a sequence of tokens that can be processed by the parser. This document describes the design, implementation, and behavior of the tokenizer.

## Architecture

### Core Components

1. **Tokenizer Class** (`src/main/java/tokenizer/Tokenizer.java`)
   - Main tokenizer implementation
   - Extends `token_t` class for token type definitions
   - Processes input line by line

2. **Token Definition** (`src/main/java/tokenizer/token_t.java`)
   - Defines the `TokenType_t` enumeration
   - Contains token structure with type, name, and metadata

3. **Character Utilities** (`src/main/java/tokenizer/character_check.java`)
   - Helper methods for character classification
   - Determines punctuation and whitespace characters

4. **Supporting Enums**
   - `literal_t.java`: Defines literal types
   - `oper_t.java`: Defines operator types

## Token Types

The tokenizer recognizes the following token types:

| Token Type | Description |
|------------|-------------|
| `IDENTIFIER_OR_KEYWORD` | Variable names, function names, and language keywords |
| `CHAR_LITERAL` | Single character literals (e.g., `'a'`) |
| `STRING_LITERAL` | Double-quoted string literals (e.g., `"hello"`) |
| `RAW_STRING_LITERAL` | Raw string literals with hash prefixes (e.g., `r#"hello"#`) |
| `C_STRING_LITERAL` | C-style string literals (e.g., `c"hello"`) |
| `RAW_C_STRING_LITERAL` | Raw C-style string literals (e.g., `cr#"hello"#`) |
| `INTEGER_LITERAL` | Integer literals in various bases (decimal, binary, octal, hex) |
| `STRING_LITERAL_MID` | Intermediate state for incomplete string literals |
| `RAW_STRING_LITERAL_MID` | Intermediate state for incomplete raw string literals |
| `C_STRING_LITERAL_MID` | Intermediate state for incomplete C string literals |
| `RAW_C_STRING_LITERAL_MID` | Intermediate state for incomplete raw C string literals |
| `C_STRING_CONTINUATION_ESCAPE` | Line continuation state for C strings |
| `STRING_CONTINUATION_ESCAPE` | Line continuation state for regular strings |
| `PUNCTUATION` | Operators, delimiters, and other punctuation marks |

## Token Structure

Each token contains the following fields:

```java
public class token_t {
    public TokenType_t tokentype;  // Type of the token
    public String name;            // Lexeme content
    public int number_raw;         // Number of '#' characters for raw literals
}
```

## Tokenization Process

### Main Algorithm

1. **Input Processing**
   - Reads input line by line from standard input
   - Maintains state between lines for multi-line tokens

2. **State Management**
   - Tracks incomplete tokens across line boundaries
   - Handles block comment nesting levels
   - Manages string literal continuation states

3. **Character-by-Character Analysis**
   - Processes each character based on current context
   - Handles special cases for literals, comments, and punctuation

### Special Handling

#### String Literals

The tokenizer supports multiple string literal formats:

1. **Regular Strings**: `"Hello, world!"`
   - Supports escape sequences (`\n`, `\t`, `\r`, `\\`, `\'`, `\"`, `\0`, `\xHH`)
   - Handles line continuations with backslash

2. **Raw Strings**: `r#"Hello, world!"#`
   - No escape sequence processing
   - Customizable delimiter with hash marks
   - Tracks hash count for proper termination

3. **C Strings**: `c"Hello, world!"`
   - Similar to regular strings but with C-style semantics
   - Supports line continuations

4. **Raw C Strings**: `cr#"Hello, world!"#`
   - Combines raw string behavior with C string semantics

#### Numeric Literals

The tokenizer recognizes various integer literal formats:

1. **Decimal**: `123`, `1_234_567`
2. **Binary**: `0b1010`, `0b10_10`
3. **Octal**: `0o755`, `0o7_55`
4. **Hexadecimal**: `0xFF`, `0xFF_FF`
5. **Suffixes**: Supports type suffixes (e.g., `123u32`)

#### Comments

1. **Line Comments**: `// This is a comment`
   - Terminates at end of line
   - Content is ignored

2. **Block Comments**: `/* This is a block comment */`
   - Supports nesting
   - Tracks nesting level with `block_comment_level`

#### Punctuation

Recognizes single and multi-character punctuation:

- Single character: `+`, `-`, `*`, `/`, etc.
- Multi-character: `==`, `<=`, `>=`, `!=`, `&&`, `||`, `<<`, `>>`, `+=`, `-=`, `*=`, `/=`, `%=`, `&=`, `|=`, `^=`, `<<=`, `>>=`, `->`, `=>`, `::`, `..`, `...`, `..=`, `##`, `<-`

## Error Handling

The tokenizer uses assertions for error detection:

- Invalid escape sequences
- Malformed numeric literals
- Unclosed string literals
- Invalid character literals
- Unknown characters

## Integration with Parser

The tokenizer works closely with the parser:

1. **Token Stream**: Produces a `Vector<token_t>` for parser consumption
2. **State Preservation**: Maintains token state across input lines
3. **Error Propagation**: Parser handles tokenization errors through exceptions

## Performance Considerations

1. **Linear Processing**: Single pass through input
2. **State Tracking**: Minimal state overhead
3. **String Operations**: Efficient string building for literals
4. **Vector Storage**: Dynamic array for token storage

## Usage Example

```java
Tokenizer tokenizer = new Tokenizer();
tokenizer.tokenize("let x = 42; // Initialize x");
// tokenizer.tokens now contains:
// 1. IDENTIFIER_OR_KEYWORD: "let"
// 2. IDENTIFIER_OR_KEYWORD: "x"
// 3. PUNCTUATION: "="
// 4. INTEGER_LITERAL: "42"
// 5. PUNCTUATION: ";"
```

## Future Enhancements

Potential improvements to consider:

1. **Location Tracking**: Add line and column information to tokens
2. **Unicode Support**: Enhanced Unicode character handling
3. **Custom Error Recovery**: Graceful handling of syntax errors
4. **Performance Optimization**: Buffer management for large inputs
5. **Token Attributes**: Additional metadata for advanced parsing

## Dependencies

- Java Standard Library
- No external dependencies required

## Testing

The tokenizer should be tested with:

1. Valid Rust code samples
2. Edge cases for all literal types
3. Nested block comments
4. Multi-line string literals
5. Invalid input scenarios