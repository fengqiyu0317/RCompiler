import java.util.Vector;

public class Tokenizer extends token_t {
    public Vector<token_t> tokens;
    int pos, block_comment_level;
    private int currentLine = 1;
    private int currentColumn = 0;
    private token_t last_token = null;
    
    public Tokenizer() {
        pos = 0;
        block_comment_level = 0;
        tokens = new Vector<token_t>();
        last_token = null;
    }
    
    public boolean isCompleted(TokenType_t tokentype) {
        if(tokentype == TokenType_t.STRING_LITERAL_MID || tokentype == TokenType_t.RAW_STRING_LITERAL_MID ||
           tokentype == TokenType_t.C_STRING_LITERAL_MID || tokentype == TokenType_t.RAW_C_STRING_LITERAL_MID ||
           tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE || tokentype == TokenType_t.STRING_CONTINUATION_ESCAPE) {
            return false;
        }
        return true;
    }
    
    public void tokenize(String input) {
        initializeTokenization();
        processInput(input);
        finalizeIncompleteTokens();
    }
    
    private void initializeTokenization() {
        // Extract the last token. If it is not complete, keep it for the next line.
        if (!tokens.isEmpty()) {
            last_token = tokens.get(tokens.size() - 1);
            if (isCompleted(last_token.tokentype)) {
                last_token = null;
            } else {
                tokens.remove(tokens.size() - 1);
            }
        }
    }
    
    private void processInput(String input) {
        int i = 0;
        while (i < input.length()) {
            i = processCharacter(input, i);
        }
    }
    
    private int processCharacter(String input, int i) {
        char c = input.charAt(i);
        
        // Update position tracking
        updatePosition(c);
        
        if (block_comment_level > 0) {
            return handleBlockComment(input, i);
        }
        
        if (last_token != null && !isCompleted(last_token.tokentype)) {
            return handleIncompleteToken(input, i);
        }
        
        return handleNewToken(input, i);
    }
    
    private void updatePosition(char c) {
        if (c == TokenizerConstants.LINE_FEED) {
            currentLine++;
            currentColumn = 0;
        } else {
            currentColumn++;
        }
    }
    
    private int handleBlockComment(String input, int i) {
        char c = input.charAt(i);
        if (c == TokenizerConstants.FORWARD_SLASH && i + 1 < input.length() && input.charAt(i + 1) == TokenizerConstants.ASTERISK) {
            block_comment_level++;
            return i + 2;
        }
        if (c == TokenizerConstants.ASTERISK && i + 1 < input.length() && input.charAt(i + 1) == TokenizerConstants.FORWARD_SLASH) {
            block_comment_level--;
            return i + 2;
        }
        return i + 1;
    }
    
    private int handleIncompleteToken(String input, int i) {
        char c = input.charAt(i);
        
        // Handle continuation escape mode
        if (character_check.isWhitespace(c) && (last_token.tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE || last_token.tokentype == TokenType_t.STRING_CONTINUATION_ESCAPE)) {
            return i + 1;
        }
        
        // Exit continuation escape mode
        if (!character_check.isWhitespace(c) && (last_token.tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE || last_token.tokentype == TokenType_t.STRING_CONTINUATION_ESCAPE)) {
            last_token.tokentype = (last_token.tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE) ? TokenType_t.C_STRING_LITERAL_MID : TokenType_t.STRING_LITERAL_MID;
        }
        
        // Handle escape sequences
        if (c == TokenizerConstants.BACKSLASH && last_token.tokentype != TokenType_t.RAW_STRING_LITERAL_MID && last_token.tokentype != TokenType_t.RAW_C_STRING_LITERAL_MID) {
            if (i + 1 == input.length()) {
                last_token.tokentype = (last_token.tokentype == TokenType_t.C_STRING_LITERAL_MID) ? TokenType_t.C_STRING_CONTINUATION_ESCAPE : TokenType_t.STRING_CONTINUATION_ESCAPE;
                return i + 1;
            }
            int newPosition = EscapeSequenceProcessor.processEscapeInToken(last_token, input, i);
            return newPosition + 1;
        }
        
        // Handle string termination
        if (c == TokenizerConstants.DOUBLE_QUOTE && (last_token.tokentype == TokenType_t.STRING_LITERAL_MID || last_token.tokentype == TokenType_t.C_STRING_LITERAL_MID)) {
            last_token.tokentype = (last_token.tokentype == TokenType_t.C_STRING_LITERAL_MID) ? TokenType_t.C_STRING_LITERAL : TokenType_t.STRING_LITERAL;
            tokens.add(last_token);
            last_token = null;
            return i + 1;
        }
        
        // Handle raw string termination
        if (c == TokenizerConstants.DOUBLE_QUOTE && (last_token.tokentype == TokenType_t.RAW_STRING_LITERAL_MID || last_token.tokentype == TokenType_t.RAW_C_STRING_LITERAL_MID)) {
            return handleRawStringTermination(input, i);
        }
        
        // Add character to incomplete token
        last_token.name += c;
        return i + 1;
    }
    
    private int handleRawStringTermination(String input, int i) {
        int j = i + 1;
        int count_raw = 0;
        while (j < input.length() && input.charAt(j) == TokenizerConstants.HASH) {
            count_raw++;
            j++;
        }
        
        if (count_raw == last_token.number_raw) {
            last_token.tokentype = (last_token.tokentype == TokenType_t.RAW_C_STRING_LITERAL_MID) ? TokenType_t.RAW_C_STRING_LITERAL : TokenType_t.RAW_STRING_LITERAL;
            tokens.add(last_token);
            last_token = null;
            return j;
        } else {
            last_token.name += TokenizerConstants.DOUBLE_QUOTE;
            for (int k = 0; k < count_raw; k++) {
                last_token.name += TokenizerConstants.HASH;
            }
            return j;
        }
    }
    
    private int handleNewToken(String input, int i) {
        char c = input.charAt(i);
        
        if (character_check.isPunctuation(c)) {
            return processPunctuation(input, i);
        } else if (Character.isDigit(c)) {
            return processNumber(input, i);
        } else if (Character.isLetter(c) || c == TokenizerConstants.DOUBLE_QUOTE) {
            return processIdentifierOrString(input, i);
        } else if (c == TokenizerConstants.SINGLE_QUOTE) {
            return processCharacterLiteral(input, i);
        } else if (character_check.isWhitespace(c)) {
            return i + 1;
        } else {
            handleError(TokenizerConstants.UNKNOWN_CHARACTER + c);
            return i + 1;
        }
    }
    
    private int processPunctuation(String input, int i) {
        char c = input.charAt(i);
        
        // Check for line comments
        if (c == TokenizerConstants.FORWARD_SLASH && i + 1 < input.length() && input.charAt(i + 1) == TokenizerConstants.FORWARD_SLASH) {
            return input.length(); // Skip rest of line
        }
        
        // Check for block comment start
        if (c == TokenizerConstants.FORWARD_SLASH && i + 1 < input.length() && input.charAt(i + 1) == TokenizerConstants.ASTERISK) {
            block_comment_level++;
            return i + 2;
        }
        
        token_t token = createToken(TokenType_t.PUNCTUATION, "");
        
        // Check for multi-character punctuation
        boolean found = false;
        for (String p : TokenizerConstants.MULTI_CHAR_PUNCTUATIONS) {
            if (input.startsWith(p, i)) {
                token.name = p;
                tokens.add(token);
                return i + p.length();
            }
        }
        
        // Single character punctuation
        token.name = String.valueOf(c);
        tokens.add(token);
        return i + 1;
    }
    
    private int processNumber(String input, int i) {
        char c = input.charAt(i);
        token_t token = createToken(TokenType_t.INTEGER_LITERAL, String.valueOf(c));
        
        // Check for non-decimal literals
        if (c == TokenizerConstants.DIGIT_ZERO && i + 1 < input.length()) {
            char next_c = input.charAt(i + 1);
            if (next_c == TokenizerConstants.LETTER_B || next_c == TokenizerConstants.LETTER_O || next_c == TokenizerConstants.LETTER_X) {
                return processNonDecimalNumber(input, i, token, next_c);
            }
        }
        
        // Process decimal number
        return processDecimalNumber(input, i, token);
    }
    
    private int processNonDecimalNumber(String input, int i, token_t token, char baseChar) {
        token.name += baseChar;
        int newPosition = i + 2;
        
        boolean hasValidDigit = false;
        while (newPosition < input.length()) {
            char digit = input.charAt(newPosition);
            if (digit == TokenizerConstants.UNDERSCORE) {
                newPosition++;
                continue;
            }
            
            boolean isValidDigit = false;
            if (baseChar == TokenizerConstants.LETTER_B) {
                isValidDigit = (digit == TokenizerConstants.DIGIT_ZERO || digit == TokenizerConstants.DIGIT_ONE);
            } else if (baseChar == TokenizerConstants.LETTER_O) {
                isValidDigit = (digit >= TokenizerConstants.DIGIT_ZERO && digit <= TokenizerConstants.DIGIT_SEVEN);
            } else if (baseChar == TokenizerConstants.LETTER_X) {
                isValidDigit = Character.isDigit(digit) ||
                              (digit >= 'a' && digit <= 'f') ||
                              (digit >= 'A' && digit <= 'F');
            }
            
            if (isValidDigit) {
                token.name += digit;
                hasValidDigit = true;
                newPosition++;
            } else {
                break;
            }
        }
        
        if (!hasValidDigit) {
            handleError("Invalid " +
                       (baseChar == TokenizerConstants.LETTER_B ? TokenizerConstants.BINARY_TYPE :
                        baseChar == TokenizerConstants.LETTER_O ? TokenizerConstants.OCTAL_TYPE : TokenizerConstants.HEXADECIMAL_TYPE) +
                       TokenizerConstants.NO_VALID_DIGITS);
        }
        
        return processNumberSuffix(input, newPosition, token);
    }
    
    private int processDecimalNumber(String input, int i, token_t token) {
        int newPosition = i + 1;
        while (newPosition < input.length()) {
            char digit = input.charAt(newPosition);
            if (Character.isDigit(digit) || digit == TokenizerConstants.UNDERSCORE) {
                token.name += digit;
                newPosition++;
            } else {
                break;
            }
        }
        
        return processNumberSuffix(input, newPosition, token);
    }
    
    private int processNumberSuffix(String input, int i, token_t token) {
        if (i < input.length() && Character.isLetter(input.charAt(i))) {
            char first_suffix_char = input.charAt(i);
            if (first_suffix_char != TokenizerConstants.LETTER_E && first_suffix_char != Character.toUpperCase(TokenizerConstants.LETTER_E)) {
                token.name += first_suffix_char;
                i++;
                while (i < input.length()) {
                    char suffix_char = input.charAt(i);
                    if (Character.isLetterOrDigit(suffix_char) || suffix_char == TokenizerConstants.UNDERSCORE) {
                        token.name += suffix_char;
                        i++;
                    } else {
                        break;
                    }
                }
            }
        }
        // Add completed token to list
        tokens.add(token);
        return i;
    }
    
    private int processIdentifierOrString(String input, int i) {
        char c = input.charAt(i);
        char next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
        
        // Check for string literals
        if (StringLiteralProcessor.isStringStart(c, input, i)) {
            last_token = StringLiteralProcessor.createStringToken(input, i);
            return StringLiteralProcessor.processStringStart(input, i);
        }
        
        // Handle regular string literal starting with "
        if (c == TokenizerConstants.DOUBLE_QUOTE) {
            last_token = createToken(TokenType_t.STRING_LITERAL_MID, "");
            return i + 1;
        }
        
        // Process identifier
        token_t token = createToken(TokenType_t.IDENTIFIER_OR_KEYWORD, String.valueOf(c));
        while (next_c != '\0' && (Character.isLetterOrDigit(next_c) || next_c == TokenizerConstants.UNDERSCORE)) {
            token.name += next_c;
            i++;
            next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
        }
        tokens.add(token);
        return i + 1;
    }
    
    private int processCharacterLiteral(String input, int i) {
        token_t token = createToken(TokenType_t.CHAR_LITERAL, "");
        char next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
        
        if (next_c == '\0') {
            handleError(TokenizerConstants.MISSING_CONTENT);
        }
        
        // Handle escape sequences
        if (next_c == TokenizerConstants.BACKSLASH) {
            i++;
            if (i + 1 >= input.length()) {
                handleError(TokenizerConstants.INCOMPLETE_ESCAPE);
            }
            int newPosition = EscapeSequenceProcessor.processEscapeInToken(token, input, i);
            i = newPosition;
        } else {
            token.name += next_c;
            i++;
        }
        
        // Check for closing quote
        if (i + 1 >= input.length() || input.charAt(i + 1) != TokenizerConstants.SINGLE_QUOTE) {
            handleError(TokenizerConstants.MISSING_CLOSING_QUOTE);
        }
        
        token.name += TokenizerConstants.SINGLE_QUOTE;
        tokens.add(token);
        return i + 2;
    }
    
    private void finalizeIncompleteTokens() {
        if (last_token != null) {
            if (isCompleted(last_token.tokentype)) {
                handleError(TokenizerConstants.LAST_TOKEN_COMPLETED);
            }
            last_token.name += TokenizerConstants.LINE_FEED;
            tokens.add(last_token);
            last_token = null;
        }
    }
    
    private token_t createToken(TokenType_t type, String value) {
        token_t token = new token_t();
        token.tokentype = type;
        token.name = value;
        return token;
    }
    
    private void handleError(String message) {
        throw new TokenizerException(message, currentLine, currentColumn);
    }
}