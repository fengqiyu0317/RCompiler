# Tokenizer Implementation Details

## Overview

This document provides an in-depth analysis of the `Tokenizer.java` implementation, covering the internal workings, state management, and specific algorithms used in the tokenization process.

## Class Structure

```java
public class Tokenizer extends token_t {
    // Member variables
    public Vector<token_t> tokens;
    int pos, block_comment_level;
    
    // Key methods
    public boolean isCompleted(TokenType_t tokentype)
    public void tokenize(String input)
}
```

### Member Variables

| Variable | Type | Purpose |
|----------|------|---------|
| `tokens` | `Vector<token_t>` | Stores all generated tokens |
| `pos` | `int` | Current position in the tokens vector (currently unused) |
| `block_comment_level` | `int` | Tracks nesting level of block comments |

## Core Methods

### 1. isCompleted(TokenType_t tokentype)

Determines whether a token type represents a completed token or an intermediate state.

```java
public boolean isCompleted(TokenType_t tokentype) {
    if(tokentype == TokenType_t.STRING_LITERAL_MID || tokentype == TokenType_t.RAW_STRING_LITERAL_MID ||
       tokentype == TokenType_t.C_STRING_LITERAL_MID || tokentype == TokenType_t.RAW_C_STRING_LITERAL_MID ||
       tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE || tokentype == TokenType_t.STRING_CONTINUATION_ESCAPE) {
        return false;
    }
    return true;
}
```

**Incomplete Token Types:**
- `STRING_LITERAL_MID` - Regular string literal in progress
- `RAW_STRING_LITERAL_MID` - Raw string literal in progress
- `C_STRING_LITERAL_MID` - C string literal in progress
- `RAW_C_STRING_LITERAL_MID` - Raw C string literal in progress
- `C_STRING_CONTINUATION_ESCAPE` - C string with line continuation
- `STRING_CONTINUATION_ESCAPE` - Regular string with line continuation

### 2. tokenize(String input)

Main tokenization method that processes input line by line.

#### Method Signature
```java
public void tokenize(String input)
```

#### Algorithm Flow

1. **Initialization Phase**
   ```java
   // Extract the last token. If it is not complete, keep it for the next line.
   token_t last_token = null;
   if (!tokens.isEmpty()) {
       last_token = tokens.get(tokens.size() - 1);
       if (isCompleted(last_token.tokentype)) {
           last_token = null;
       } else {
           tokens.remove(tokens.size() - 1);
       }
   }
   ```

2. **Main Processing Loop**
   ```java
   int i = 0;
   while(i < input.length()) {
       char c = input.charAt(i);
       
       // Step 1: Handle block comments (highest priority)
       if (block_comment_level > 0) {
           // Skip characters until block comment ends
           // Handle nested block comments
           // Continue to next iteration
       }
       
       // Step 2: Handle incomplete tokens from previous lines
       if (last_token != null && !isCompleted(last_token.tokentype)) {
           // Continue building the incomplete token
           // Handle escape sequences, string terminators, etc.
           // Continue to next iteration if token is still incomplete
       }
       
       // Step 3: Process current character based on its type
       if (character_check.isPunctuation(c)) {
           // Handle punctuation and comments
       } else if (Character.isDigit(c)) {
           // Handle numeric literals
       } else if (Character.isLetter(c)) {
           // Handle identifiers, keywords, and string literals
       } else if (c == '\'') {
           // Handle character literals
       } else if (c == '\"') {
           // Handle string literals
       } else if (character_check.isWhitespace(c)) {
           // Skip whitespace
       } else {
           // Handle unknown characters
       }
   }
   ```

3. **Finalization Phase**
   ```java
   // if last_token is not completed, then we append a LF to it.
   if (last_token != null) {
       assert !isCompleted(last_token.tokentype): "last_token should not be completed.";
       last_token.name += "\n";
       tokens.add(last_token);
   }
   ```

## State Machine Implementation

The tokenizer follows a hierarchical state machine with three main processing levels in the while loop:

### Level 1: Block Comment Handling (Highest Priority)

```java
if (block_comment_level > 0) {
    // we are in a block comment, we need to skip until the end of the block comment
    if (c == '/' && i + 1 < input.length() && input.charAt(i + 1) == '*') {
        block_comment_level++;
        i += 2;
        continue;
    }
    if (c == '*' && i + 1 < input.length() && input.charAt(i + 1) == '/') {
        block_comment_level--;
        i += 2;
        continue;
    }
    i++;
    continue;
}
```

**Key Points:**
- This is the first check in the while loop (highest priority)
- Supports nested block comments
- Increments level on `/*`
- Decrements level on `*/`
- Skips all characters while inside block comment
- Uses `continue` to skip to next iteration immediately

### Level 2: Incomplete Token Processing (Second Priority)

```java
// Check if last_token is uncompleted.
if (last_token != null && !isCompleted(last_token.tokentype)) {
    // Handle continuation escape mode
    if (character_check.isWhitespace(c) && (last_token.tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE || last_token.tokentype == TokenType_t.STRING_CONTINUATION_ESCAPE)) {
        i++;
        continue;
    }
    
    // Exit continuation escape mode
    if (!character_check.isWhitespace(c) && (last_token.tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE || last_token.tokentype == TokenType_t.STRING_CONTINUATION_ESCAPE)) {
        last_token.tokentype = (last_token.tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE) ? TokenType_t.C_STRING_LITERAL_MID : TokenType_t.STRING_LITERAL_MID;
    }
    
    // Handle escape sequences and string termination
    // ... (detailed logic for string processing)
    
    // Continue to next iteration if still processing incomplete token
    continue;
}
```

**Key Points:**
- This is the second check in the while loop
- Only executes if not inside a block comment
- Handles multi-line tokens and escape sequences
- Uses `continue` to skip character type classification when token is still incomplete

### Level 3: Character Type Classification (Lowest Priority)

Only if neither of the above conditions are met, the tokenizer proceeds to classify the current character:

```java
// Check if c is a punctuation
if (character_check.isPunctuation(c)) {
    // Handle punctuation and comments
} else if (Character.isDigit(c)) {
    // Handle numeric literals
} else if (Character.isLetter(c)) {
    // Handle identifiers, keywords, and string literals
} else if (c == '\'') {
    // Handle character literals
} else if (c == '\"') {
    // Handle string literals
} else if (character_check.isWhitespace(c)) {
    // Skip whitespace
} else {
    // Handle unknown characters
}
```

## Token Type Implementations

### 1. Punctuation Tokens

```java
if (character_check.isPunctuation(c)) {
    // check if it's the start of a comment
    if (c == '/' && i + 1 < input.length() && input.charAt(i + 1) == '/') {
        break;  // Skip line comments
    }
    // if it's the start of a block comment, we need to skip until the end of the block comment
    if (c == '/' && i + 1 < input.length() && input.charAt(i + 1) == '*') {
        block_comment_level++;
        i += 2;
        continue;
    }
    
    token_t token = new token_t();
    token.tokentype = TokenType_t.PUNCTUATION;
    String[] multi_char_punctuations = {"==", "<=", ">=", "!=", "&&", "||", "<<", ">>", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", "->", "=>", "::", "..", "...", "..=", "##", "<-" };
    
    // Check for multi-character punctuation first
    boolean found = false;
    for (String p : multi_char_punctuations) {
        if (input.startsWith(p, i)) {
            token.name = p;
            i += p.length();
            found = true;
            break;
        }
    }
    
    // Fall back to single character
    if (!found) {
        token.name = String.valueOf(c);
        i++;
    }
    tokens.add(token);
}
```

**Multi-character Punctuation Priority:**
- Longer operators are checked first
- Uses `String.startsWith()` for efficient matching
- Falls back to single-character punctuation if no match

### 2. Numeric Literals

```java
else if (Character.isDigit(c)) {
    token_t token = new token_t();
    token.tokentype = TokenType_t.INTEGER_LITERAL;
    token.name = String.valueOf(c);
    
    // Check if this is a non-decimal literal (binary, octal, or hex)
    if (c == '0' && i + 1 < input.length()) {
        char next_c = input.charAt(i + 1);
        if (next_c == 'b' || next_c == 'o' || next_c == 'x') {
            // Handle non-decimal literals
            token.name += next_c;
            i += 2;
            
            // Process digits based on base
            boolean hasValidDigit = false;
            while (i < input.length()) {
                char digit = input.charAt(i);
                if (digit == '_') {
                    token.name += digit;
                    i++;
                    continue;
                }
                
                boolean isValidDigit = false;
                if (next_c == 'b') {
                    isValidDigit = (digit == '0' || digit == '1');
                } else if (next_c == 'o') {
                    isValidDigit = (digit >= '0' && digit <= '7');
                } else if (next_c == 'x') {
                    isValidDigit = Character.isDigit(digit) ||
                                  (digit >= 'a' && digit <= 'f') ||
                                  (digit >= 'A' && digit <= 'F');
                }
                
                if (isValidDigit) {
                    token.name += digit;
                    hasValidDigit = true;
                    i++;
                } else {
                    break;
                }
            }
            
            if (!hasValidDigit) {
                assert false: "Invalid " + 
                             (next_c == 'b' ? "binary" :
                              next_c == 'o' ? "octal" : "hexadecimal") + 
                             " literal: no valid digits found";
            }
        }
    }
    
    // Handle suffixes
    if (i < input.length() && Character.isLetter(input.charAt(i))) {
        char first_suffix_char = input.charAt(i);
        if (first_suffix_char != 'e' && first_suffix_char != 'E') {
            // Process suffix
            token.name += first_suffix_char;
            i++;
            while (i < input.length()) {
                char suffix_char = input.charAt(i);
                if (Character.isLetterOrDigit(suffix_char) || suffix_char == '_') {
                    token.name += suffix_char;
                    i++;
                } else {
                    break;
                }
            }
        }
    }
    tokens.add(token);
}
```

**Numeric Literal Features:**
- Base prefixes: `0b` (binary), `0o` (octal), `0x` (hexadecimal)
- Underscore separators for readability
- Type suffixes (excluding 'e' and 'E' to avoid confusion with scientific notation)
- Validation for digit correctness in each base

### 3. String and Character Literals

The tokenizer supports multiple string and character literal formats with comprehensive escape sequence handling.

#### String Literal Types

1. **Regular String Literals** (`"Hello"`)
   - Starts with `"` character
   - Supports escape sequences
   - Can span multiple lines with continuation escapes

2. **Raw String Literals** (`r#"Hello"#`)
   - Starts with `r` followed by one or more `#` characters, then `"`
   - No escape sequence processing
   - Ends with `"` followed by the same number of `#` characters
   - Tracks hash count in `token.number_raw`

3. **C String Literals** (`c"Hello"`)
   - Starts with `c"` prefix
   - Similar to regular strings but with C-style semantics
   - Supports escape sequences and line continuations

4. **Raw C String Literals** (`cr#"Hello"#`)
   - Starts with `cr` followed by one or more `#` characters, then `"`
   - Combines raw string behavior with C string semantics

#### String Literal Processing

**Incomplete Token Handling (Level 2 Processing):**

```java
// Handle continuation escape mode
if (character_check.isWhitespace(c) && (last_token.tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE || last_token.tokentype == TokenType_t.STRING_CONTINUATION_ESCAPE)) {
    i++;
    continue;
}

// Exit continuation escape mode
if (!character_check.isWhitespace(c) && (last_token.tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE || last_token.tokentype == TokenType_t.STRING_CONTINUATION_ESCAPE)) {
    last_token.tokentype = (last_token.tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE) ? TokenType_t.C_STRING_LITERAL_MID : TokenType_t.STRING_LITERAL_MID;
}
```

**Escape Sequence Processing:**

```java
if (c == '\\' && last_token.tokentype != TokenType_t.RAW_STRING_LITERAL_MID && last_token.tokentype != TokenType_t.RAW_C_STRING_LITERAL_MID) {
    if (i + 1 == input.length()) {
        // Line continuation at end of line
        last_token.tokentype = (last_token.tokentype == TokenType_t.C_STRING_LITERAL_MID) ? TokenType_t.C_STRING_CONTINUATION_ESCAPE : TokenType_t.STRING_CONTINUATION_ESCAPE;
        i++;
        continue;
    }
    char next_c = input.charAt(i + 1);
    switch (next_c) {
        case 'n': last_token.name += '\n'; break;
        case 't': last_token.name += '\t'; break;
        case 'r': last_token.name += '\r'; break;
        case '\\': last_token.name += '\\'; break;
        case '\'': last_token.name += '\''; break;
        case '\"': last_token.name += '\"'; break;
        case '0': last_token.name += '\0'; break;
        case 'x':
            // hexadecimal escape sequence (2 hex digits)
            String hex = input.substring(i + 2, i + 4);
            last_token.name += (char) Integer.parseInt(hex, 16);
            i += 2;
            break;
        default:
            assert false: "Unknown escape sequence: \\" + next_c;
    }
    i += 2;
    continue;
}
```

**String Termination:**

```java
// Regular and C string termination
if (c == '\"' && (last_token.tokentype == TokenType_t.STRING_LITERAL_MID || last_token.tokentype == TokenType_t.C_STRING_LITERAL_MID)) {
    last_token.tokentype = (last_token.tokentype == TokenType_t.C_STRING_LITERAL_MID) ? TokenType_t.C_STRING_LITERAL : TokenType_t.STRING_LITERAL;
    tokens.add(last_token);
    last_token = null;
    i++;
    continue;
}

// Raw string termination (requires matching hash count)
if (c == '\"' && (last_token.tokentype == TokenType_t.RAW_STRING_LITERAL_MID || last_token.tokentype == TokenType_t.RAW_C_STRING_LITERAL_MID)) {
    int j = i + 1;
    int count_raw = 0;
    while (j < input.length() && input.charAt(j) == '#') {
        count_raw++;
        j++;
    }
    // Check if hash count matches opening delimiter
    if (count_raw == last_token.number_raw) {
        last_token.tokentype = (last_token.tokentype == TokenType_t.RAW_C_STRING_LITERAL_MID) ? TokenType_t.RAW_C_STRING_LITERAL : TokenType_t.RAW_STRING_LITERAL;
        tokens.add(last_token);
        last_token = null;
        i = j;
        continue;
    } else {
        // Not the end - include quotes and hashes in content
        last_token.name += c;
        for (int k = 0; k < count_raw; k++) {
            last_token.name += '#';
        }
        i = j;
        continue;
    }
}
```

**String Literal Initiation (Level 3 Processing):**

```java
// Regular string literal
else if (c == '\"') {
    token_t token = new token_t();
    token.tokentype = TokenType_t.STRING_LITERAL_MID;
    last_token = token;
}

// Raw and C-style string literals (handled in Character.isLetter branch)
if ((c == 'r' && next_c == '#') || (c == 'c' && (next_c == '\"' || (next_c == 'r' && i + 2 < input.length() && input.charAt(i + 2) == '#')))) {
    token_t token = new token_t();
    if (c == 'r' && next_c == '#') {
        // Raw string: r#"..."#
        int j = i + 1;
        int count_raw = 0;
        while (j < input.length() && input.charAt(j) == '#') {
            count_raw++;
            j++;
        }
        token.tokentype = TokenType_t.RAW_STRING_LITERAL_MID;
        token.number_raw = count_raw;
        last_token = token;
        i = j;
        assert input.charAt(i) == '\"': "Invalid raw string literal.";
        i++;
        continue;
    } else if (c == 'c' && next_c == '\"') {
        // C string: c"..."
        token.tokentype = TokenType_t.C_STRING_LITERAL_MID;
        last_token = token;
        i += 2;
        continue;
    } else if (c == 'c' && next_c == 'r' && i + 2 < input.length() && input.charAt(i + 2) == '#') {
        // Raw C string: cr#"..."#
        int j = i + 2;
        int count_raw = 0;
        while (j < input.length() && input.charAt(j) == '#') {
            count_raw++;
            j++;
        }
        token.tokentype = TokenType_t.RAW_C_STRING_LITERAL_MID;
        token.number_raw = count_raw;
        last_token = token;
        i = j;
        assert input.charAt(i) == '\"': "Invalid raw C string literal.";
        i++;
        continue;
    }
}
```

#### Character Literals

Character literals are enclosed in single quotes and support the same escape sequences as strings:

```java
else if (c == '\'') {
    token_t token = new token_t();
    token.tokentype = TokenType_t.CHAR_LITERAL;
    char next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
    if (next_c == '\0') {
        assert false: "Invalid char literal.";
    }
    
    // Handle escape sequences
    if (next_c == '\\') {
        i++;
        next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
        if (next_c == '\0') {
            assert false: "Invalid char literal.";
        }
        switch (next_c) {
            case 'n': token.name += '\n'; break;
            case 't': token.name += '\t'; break;
            case 'r': token.name += '\r'; break;
            case '\\': token.name += '\\'; break;
            case '\'': token.name += '\''; break;
            case '\"': token.name += '\"'; break;
            case '0': token.name += '\0'; break;
            case 'x':
                // hexadecimal escape sequence
                String hex = input.substring(i + 2, i + 4);
                token.name += (char) Integer.parseInt(hex, 16);
                i += 2;
                break;
            default:
                assert false: "Unknown escape sequence: \\" + next_c;
        }
        i++;
        next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
    } else {
        token.name += next_c;
        i++;
        next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
    }
    
    // Must end with closing quote
    if (next_c != '\'') {
        assert false: "Invalid char literal.";
    }
    i++;
    token.name += next_c;
    tokens.add(token);
}
```

**Key Features:**
- Supports all standard escape sequences
- Validates proper termination with closing quote
- Handles hexadecimal escape sequences with exactly 2 digits
- Uses assertions for error detection

### 4. Identifiers and Keywords

```java
else if (Character.isLetter(c)) {
    token_t token = new token_t();
    token.tokentype = TokenType_t.IDENTIFIER_OR_KEYWORD;
    token.name = String.valueOf(c);
    char next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
    while (next_c != '\0' && (Character.isLetterOrDigit(next_c) || next_c == '_')) {
        token.name += next_c;
        i++;
        next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
    }
    tokens.add(token);
}
```

## Error Handling Strategies

### Assertion-Based Validation

The tokenizer uses Java assertions for error detection:

1. **Invalid Escape Sequences**
   ```java
   assert false: "Unknown escape sequence: \\" + next_c;
   ```

2. **Invalid Numeric Literals**
   ```java
   assert false: "Invalid binary literal: no valid digits found";
   ```

3. **Invalid String Literals**
   ```java
   assert input.charAt(i) == '\"': "Invalid raw string literal.";
   ```

4. **Invalid Character Literals**
   ```java
   assert false: "Invalid char literal.";
   ```

## Performance Optimizations

### 1. Early Termination
- Line comments (`//`) cause immediate break from processing loop
- Block comments skip all characters until termination

### 2. Efficient String Operations
- Uses `String.startsWith()` for multi-character punctuation
- Builds string literals incrementally
- Minimizes string copying

### 3. State Preservation
- Maintains incomplete tokens between lines
- Avoids reprocessing already analyzed characters

## Memory Management

### Vector Usage
```java
public Vector<token_t> tokens;
```
- Dynamic array for token storage
- Automatic capacity expansion
- Efficient random access for parser

### Token Reuse
- Incomplete tokens are removed and re-added
- Minimizes object allocation

## Thread Safety Considerations

The current implementation is **not thread-safe**:
- Shared mutable state (`tokens`, `pos`, `block_comment_level`)
- No synchronization mechanisms
- Designed for single-threaded use in compiler pipeline

## Integration Points

### Input Interface
```java
public void tokenize(String input)
```
- Accepts one line at a time
- Maintains state between calls
- Designed for streaming input

### Output Interface
```java
public Vector<token_t> tokens;
```
- Direct access to token vector
- Consumed by Parser class
- No iterator interface provided

## Debugging and Maintenance

### State Visibility
- `block_comment_level` can be inspected for debugging
- `tokens` vector provides complete tokenization history
- `pos` variable (currently unused) could be utilized for debugging

### Common Issues
1. **Multi-line String Literals**: Requires proper state management
2. **Nested Block Comments**: Tracked with `block_comment_level`
3. **Escape Sequence Complexity**: Multiple escape types to handle
4. **Numeric Literal Validation**: Base-specific digit checking

## Recommended Structure Improvements

Based on the current implementation, here are some practical structural improvements that would enhance maintainability without overcomplicating the design:

### 1. Extract Token Processing Methods

**Current Issue**: The `tokenize()` method is too long and handles multiple responsibilities.

**Recommended Solution**: Break down the main method into smaller, focused methods:

```java
public class Tokenizer extends token_t {
    // ... existing members ...
    
    public void tokenize(String input) {
        initializeTokenization();
        processInput(input);
        finalizeIncompleteTokens();
    }
    
    private void initializeTokenization() {
        // Extract and handle incomplete token from previous line
        // ... existing initialization logic
    }
    
    private void processInput(String input) {
        int i = 0;
        while (i < input.length()) {
            i = processCharacter(input, i);
        }
    }
    
    private int processCharacter(String input, int i) {
        char c = input.charAt(i);
        
        if (block_comment_level > 0) {
            return handleBlockComment(input, i);
        }
        
        if (last_token != null && !isCompleted(last_token.tokentype)) {
            return handleIncompleteToken(input, i);
        }
        
        return handleNewToken(input, i);
    }
    
    private int handleBlockComment(String input, int i) {
        // ... existing block comment logic
        return i + 1; // or appropriate increment
    }
    
    private int handleIncompleteToken(String input, int i) {
        // ... existing incomplete token logic
        return i + 1; // or appropriate increment
    }
    
    private int handleNewToken(String input, int i) {
        char c = input.charAt(i);
        
        if (character_check.isPunctuation(c)) {
            return processPunctuation(input, i);
        } else if (Character.isDigit(c)) {
            return processNumber(input, i);
        } else if (Character.isLetter(c)) {
            return processIdentifierOrString(input, i);
        } else if (c == '\'') {
            return processCharacterLiteral(input, i);
        } else if (c == '\"') {
            return processStringLiteral(input, i);
        } else if (character_check.isWhitespace(c)) {
            return i + 1;
        } else {
            throw new RuntimeException("Unknown character: " + c);
        }
    }
    
    private int processPunctuation(String input, int i) {
        // ... existing punctuation logic
        return i + 1; // or appropriate increment
    }
    
    private int processNumber(String input, int i) {
        // ... existing number logic
        return i + 1; // or appropriate increment
    }
    
    private int processIdentifierOrString(String input, int i) {
        // ... existing identifier/string logic
        return i + 1; // or appropriate increment
    }
    
    private int processCharacterLiteral(String input, int i) {
        // ... existing char literal logic
        return i + 1; // or appropriate increment
    }
    
    private int processStringLiteral(String input, int i) {
        // ... existing string literal logic
        return i + 1; // or appropriate increment
    }
    
    private void finalizeIncompleteTokens() {
        // ... existing finalization logic
    }
}
```

### 2. Create Helper Classes for String Processing

**Current Issue**: String literal processing logic is complex and mixed with other concerns.

**Recommended Solution**: Create dedicated helper classes:

```java
class StringLiteralProcessor {
    public static boolean isStringStart(char c, String input, int position) {
        if (c == '\"') return true;
        if (c == 'r' && position + 1 < input.length() && input.charAt(position + 1) == '#') return true;
        if (c == 'c' && position + 1 < input.length() && input.charAt(position + 1) == '\"') return true;
        if (c == 'c' && position + 2 < input.length() &&
            input.charAt(position + 1) == 'r' && input.charAt(position + 2) == '#') return true;
        return false;
    }
    
    public static token_t createStringToken(String input, int position) {
        char c = input.charAt(position);
        if (c == '\"') {
            return createRegularStringToken();
        } else if (c == 'r') {
            return createRawStringToken(input, position);
        } else if (c == 'c' && input.charAt(position + 1) == '\"') {
            return createCStringToken();
        } else if (c == 'c' && input.charAt(position + 1) == 'r') {
            return createRawCStringToken(input, position);
        }
        return null;
    }
    
    private static token_t createRegularStringToken() {
        token_t token = new token_t();
        token.tokentype = TokenType_t.STRING_LITERAL_MID;
        return token;
    }
    
    private static token_t createRawStringToken(String input, int position) {
        token_t token = new token_t();
        token.tokentype = TokenType_t.RAW_STRING_LITERAL_MID;
        token.number_raw = countHashes(input, position + 1);
        return token;
    }
    
    // ... other helper methods
}

class EscapeSequenceProcessor {
    public static char processEscape(String input, int position) {
        char escapeChar = input.charAt(position + 1);
        switch (escapeChar) {
            case 'n': return '\n';
            case 't': return '\t';
            case 'r': return '\r';
            case '\\': return '\\';
            case '\'': return '\'';
            case '\"': return '\"';
            case '0': return '\0';
            case 'x': return processHexEscape(input, position);
            default: throw new RuntimeException("Unknown escape sequence: \\" + escapeChar);
        }
    }
    
    private static char processHexEscape(String input, int position) {
        String hex = input.substring(position + 2, position + 4);
        return (char) Integer.parseInt(hex, 16);
    }
}
```

### 3. Improve Error Handling

**Current Issue**: Uses assertions that crash the program.

**Recommended Solution**: Replace assertions with proper exceptions:

```java
public class Tokenizer extends token_t {
    // ... existing members ...
    private int currentLine = 1;
    private int currentColumn = 0;
    
    // ... existing methods ...
    
    private void handleError(String message) {
        throw new TokenizerException(message, currentLine, currentColumn);
    }
    
    private void validateEscapeSequence(char escapeChar) {
        if (!isValidEscapeChar(escapeChar)) {
            handleError("Unknown escape sequence: \\" + escapeChar);
        }
    }
    
    private void validateCharLiteral() {
        // ... validation logic
        if (!isValid) {
            handleError("Invalid character literal");
        }
    }
}

class TokenizerException extends RuntimeException {
    public final int line;
    public final int column;
    
    public TokenizerException(String message, int line, int column) {
        super("Error at line " + line + ", column " + column + ": " + message);
        this.line = line;
        this.column = column;
    }
}
```

### 4. Add Position Tracking

**Current Issue**: No line/column information for tokens.

**Recommended Solution**: Track and store position information:

```java
public class Tokenizer extends token_t {
    // ... existing members ...
    private int currentLine = 1;
    private int currentColumn = 0;
    
    private int processCharacter(String input, int i) {
        char c = input.charAt(i);
        
        // Update position
        if (c == '\n') {
            currentLine++;
            currentColumn = 0;
        } else {
            currentColumn++;
        }
        
        // ... existing processing logic
    }
    
    private token_t createToken(TokenType_t type, String value) {
        token_t token = new token_t();
        token.tokentype = type;
        token.name = value;
        // Add position information to token_t class
        token.line = currentLine;
        token.column = currentColumn - value.length();
        return token;
    }
}
```

### 5. Extract Constants

**Current Issue**: Hard-coded strings and arrays scattered throughout code.

**Recommended Solution**: Centralize constants:

```java
class TokenizerConstants {
    public static final String[] MULTI_CHAR_PUNCTUATIONS = {
        "==", "<=", ">=", "!=", "&&", "||", "<<", ">>",
        "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=",
        "<<=", ">>=", "->", "=>", "::", "..", "...", "..=", "##", "<-"
    };
    
    public static final char[] VALID_ESCAPE_CHARS = {
        'n', 't', 'r', '\\', '\'', '\"', '0', 'x'
    };
    
    public static final String BINARY_PREFIX = "0b";
    public static final String OCTAL_PREFIX = "0o";
    public static final String HEX_PREFIX = "0x";
    
    // ... other constants
}
```

These improvements maintain the simplicity of the current design while making the code more maintainable, testable, and robust.

## Future Implementation Considerations

1. **Position Tracking**: Add line/column information to tokens
2. **Error Recovery**: Replace assertions with exception handling
3. **Performance Profiling**: Identify bottlenecks in large files
4. **Unicode Support**: Enhanced character classification
5. **Thread Safety**: Add synchronization for concurrent use