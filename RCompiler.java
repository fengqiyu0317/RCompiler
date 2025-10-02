import java.util.Scanner;
import java.util.Vector;


public class Main {
    public static void main(String[] args) {
        ReadRustFile.read_init();
    }
}

public class token_t {
    public enum TokenType_t {
        IDENTIFIER_OR_KEYWORD, 
        CHAR_LITERAL, 
        STRING_LITERAL, 
        RAW_STRING_LITERAL, 
        C_STRING_LITERAL, 
        RAW_C_STRING_LITERAL, 
        INTEGER_LITERAL, 
        STRING_LITERAL_MID, 
        RAW_STRING_LITERAL_MID, 
        C_STRING_LITERAL_MID, 
        RAW_C_STRING_LITERAL_MID, 
        C_STRING_CONTINUATION_ESCAPE,
        STRING_CONTINUATION_ESCAPE,
        PUNCTUATION;
    }
    public TokenType_t tokentype;
    public String name;
    public int number_raw; // for raw literal, record the number of '#'s before and after the quotes.
    public token_t() {
        tokentype = null;
        name = "";
        number_raw = 0;
    }
}

public class character_check {
    public static boolean isPunctuation(char c) {
        String punctuations = "=<!>&|~+-*/%^@.,:;#$?_{}()[]";
        return punctuations.indexOf(c) != -1;
    }
    public static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
}

public class Tokenizer extends token_t {
    public boolean isCompleted(TokenType_t tokentype) {
        if(tokentype == TokenType_t.STRING_LITERAL_MID || tokentype == TokenType_t.RAW_STRING_LITERAL_MID ||
           tokentype == TokenType_t.C_STRING_LITERAL_MID || tokentype == TokenType_t.RAW_C_STRING_LITERAL_MID ||
           tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE || tokentype == TokenType_t.STRING_CONTINUATION_ESCAPE) {
            return false;
        }
        return true;
    }

    Vector<token_t> tokens;
    // pos record the current position in the tokens vector.
    // int pos, block_comment_level;
    // public Tokenizer() {
    //     pos = 0;
    //     block_comment_level = 0;
    //     tokens = new Vector<token_t>();
    // }
    // public token_t next_token() {
    //     if (pos >= tokens.size()) {
    //         return null;
    //     }
    //     return tokens.get(pos++);
    // }
    // public token_t peek_token() {
    //     if (pos >= tokens.size()) {
    //         return null;
    //     }
    //     return tokens.get(pos);
    // }
    // With a string as input, tokenize it and store the tokens in the tokens vector.
    public void tokenize(String input) {
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
        // scan each character in part
        // Use a variable to record the position of the input string.
        int i = 0;
        while(i < input.length()) {
            char c = input.charAt(i);
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
            // Check if last_token is uncompleted.
            if (last_token != null && !isCompleted(last_token.tokentype)) {
                // if the character c is whitespace and last_token is in continuation escape mode, then we skip it.
                if (character_check.isWhitespace(c) && (last_token.tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE || last_token.tokentype == TokenType_t.STRING_CONTINUATION_ESCAPE)) {
                    i++;
                    continue;
                }
                // if the character c is not whitespace and last_token is in continuation escape mode, then we exit the continuation escape mode.
                if (!character_check.isWhitespace(c) && (last_token.tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE || last_token.tokentype == TokenType_t.STRING_CONTINUATION_ESCAPE)) {
                    last_token.tokentype = (last_token.tokentype == TokenType_t.C_STRING_CONTINUATION_ESCAPE) ? TokenType_t.C_STRING_LITERAL_MID : TokenType_t.STRING_LITERAL_MID;
                }
                // if the character c is '\' and last_token is not "raw", then we need to escape the next character.
                if (c == '\\' && last_token.tokentype != TokenType_t.RAW_STRING_LITERAL_MID && last_token.tokentype != TokenType_t.RAW_C_STRING_LITERAL_MID) {
                    if (i + 1 == input.length()) {
                        last_token.tokentype = (last_token.tokentype == TokenType_t.C_STRING_LITERAL_MID) ? TokenType_t.C_STRING_CONTINUATION_ESCAPE : TokenType_t.STRING_CONTINUATION_ESCAPE;
                        i++;
                        continue;
                    }
                    char next_c = input.charAt(i + 1);
                    switch (next_c) {
                        case 'n':
                            last_token.name += '\n';
                            break;
                        case 't':
                            last_token.name += '\t';
                            break;
                        case 'r':
                            last_token.name += '\r';
                            break;
                        case '\\':
                            last_token.name += '\\';
                            break;
                        case '\'':  
                            last_token.name += '\'';
                            break;
                        case '\"':
                            last_token.name += '\"';
                            break;
                        case '0':
                            last_token.name += '\0';
                            break;
                        case 'x':
                            // hexadecimal escape sequence
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
                // if the character c is the ending quote and last_token is not raw, then we complete the token.
                if (c == '\"' && (last_token.tokentype == TokenType_t.STRING_LITERAL_MID || last_token.tokentype == TokenType_t.C_STRING_LITERAL_MID)) {
                    last_token.tokentype = (last_token.tokentype == TokenType_t.C_STRING_LITERAL_MID) ? TokenType_t.C_STRING_LITERAL : TokenType_t.STRING_LITERAL;
                    tokens.add(last_token);
                    last_token = null;
                    i++;
                    continue;
                }
                // if the character c is the ending quote and last_token is raw, then we need to check the number of '#'s after the quote.
                if (c == '\"' && (last_token.tokentype == TokenType_t.RAW_STRING_LITERAL_MID || last_token.tokentype == TokenType_t.RAW_C_STRING_LITERAL_MID)) {
                    int j = i + 1;
                    int count_raw = 0;
                    while (j < input.length() && input.charAt(j) == '#') {
                        count_raw++;
                        j++;
                    }
                    // if the number of '#'s is equal to last_token.number_raw, then we complete the token.
                    if (count_raw == last_token.number_raw) {
                        last_token.tokentype = (last_token.tokentype == TokenType_t.RAW_C_STRING_LITERAL_MID) ? TokenType_t.RAW_C_STRING_LITERAL : TokenType_t.RAW_STRING_LITERAL;
                        tokens.add(last_token);
                        last_token = null;
                        i = j;
                        continue;
                    } else {
                        // if not, we just append the character to the last_token
                        last_token.name += c;
                        for (int k = 0; k < count_raw; k++) {
                            last_token.name += '#';
                        }
                        i = j;
                        continue;
                    }
                }
                // if not, we just append the character to the last_token
                last_token.name += c;
                i++;
                continue;
            }

            // check if c is a punctuation
            if (character_check.isPunctuation(c)) {
                // check if it's the start of a comment
                if (c == '/' && i + 1 < input.length() && input.charAt(i + 1) == '/') {
                    break;
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
                boolean found = false;
                for (String p : multi_char_punctuations) {
                    if (input.startsWith(p, i)) {
                        token.name = p;
                        i += p.length();
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    token.name = String.valueOf(c);
                    i++;
                }
                tokens.add(token);
                continue;
            } else if (Character.isDigit(c)) {
                // check whether the next character is also a digit
                char next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
                token_t token = new token_t();
                token.tokentype = TokenType_t.INTEGER_LITERAL;
                token.name = String.valueOf(c);
                if (next_c != '\0' && !Character.isDigit(next_c)) {
                    if(next_c == 'b' || next_c == 'o' || next_c == 'x') {
                        token.name += next_c;
                        i++;
                    }
                    else {
                        assert false: "Invalid character after digit: " + next_c;
                    }
                }
                while (next_c != '\0' && (Character.isDigit(next_c) || next_c == '_')) {
                    token.name += next_c;
                    i++;
                    next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
                }
                // check if the next char is 'e' or 'E'
                if (next_c == 'e' || next_c == 'E') {
                    assert false: "Scientific notation is not supported yet.";
                }
                // then we need to get the suffix, which is an identifier
                if (next_c != '\0' && Character.isLetter(next_c)) {
                    token.name += next_c;
                    i++;
                    next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
                    while (next_c != '\0' && (Character.isLetterOrDigit(next_c) || next_c == '_')) {
                        token.name += next_c;
                        i++;
                        next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
                    }
                } else {
                    assert false: "Invalid suffix after integer literal.";
                }
                tokens.add(token);
            } else if (Character.isLetter(c)) {
                char next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
                // check if it is the head of a literal
                if ((c == 'r' && next_c == '#') || (c == 'c' && (next_c == '\"' || (next_c == 'r' && i + 2 < input.length() && input.charAt(i + 2) == '#')))) {
                    // determine the type of the literal
                    token_t token = new token_t();
                    if (c == 'r' && next_c == '#') {
                        // raw string literal
                        int j = i + 1;
                        int count_raw = 0;
                        while (j < input.length() && input.charAt(j) == '#') {
                            count_raw++;
                            j++;
                        }
                        token.tokentype = TokenType_t.RAW_STRING_LITERAL_MID;
                        token.number_raw = count_raw;
                        i = j;
                        assert input.charAt(i) == '\"': "Invalid raw string literal.";
                        i++;
                        continue;
                    } else if (c == 'c' && next_c == '\"') {
                        // C string literal
                        token.tokentype = TokenType_t.C_STRING_LITERAL_MID;
                        last_token = token;
                        i += 2;
                        continue;
                    } else if (c == 'c' && next_c == 'r' && i + 2 < input.length() && input.charAt(i + 2) == '#') {
                        // raw C string literal
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
                token_t token = new token_t();
                token.tokentype = TokenType_t.IDENTIFIER_OR_KEYWORD;
                token.name = String.valueOf(c);
                while (next_c != '\0' && (Character.isLetterOrDigit(next_c) || next_c == '_')) {
                    token.name += next_c;
                    i++;
                    next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
                }
                tokens.add(token);
            } else if (c == '\'') {
                // char literal
                token_t token = new token_t();
                token.tokentype = TokenType_t.CHAR_LITERAL;
                char next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
                if (next_c == '\0') {
                    assert false: "Invalid char literal.";
                }
                // if the next character is '\', then we need to escape the next character
                if (next_c == '\\') {
                    i++;
                    next_c = (i + 1 < input.length()) ? input.charAt(i + 1) : '\0';
                    if (next_c == '\0') {
                        assert false: "Invalid char literal.";
                    }
                    switch (next_c) {
                        case 'n':
                            token.name += '\n';
                            break;
                        case 't':
                            token.name += '\t';
                            break;
                        case 'r':
                            token.name += '\r';
                            break;
                        case '\\':
                            token.name += '\\';
                            break;
                        case '\'':
                            token.name += '\'';
                            break;
                        case '\"':
                            token.name += '\"';
                            break;
                        case '0':
                            token.name += '\0';
                            break;
                        case 'x':
                            // hexadecimal escape sequence
                            String hex = input.substring(i + 2, i + 4);
                            last_token.name += (char) Integer.parseInt(hex, 16);
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
                // the next character must be the ending quote
                if (next_c != '\'') {
                    assert false: "Invalid char literal.";
                }
                i++;
                token.name += next_c;
                tokens.add(token);
            } else if (c == '\"') {
                // string literal
                token_t token = new token_t();
                token.tokentype = TokenType_t.STRING_LITERAL_MID;
                last_token = token;
            } else if (character_check.isWhitespace(c)) {
                // do nothing
            } else {
                assert false: "Unknown character: " + c;
            }
            i++;
        }
        // if last_token is not completed, then we append a LF to it.
        if (last_token != null) {
            assert !isCompleted(last_token.tokentype): "last_token should not be completed.";
            last_token.name += "\n";
            tokens.add(last_token);
        }
    }
}

public class ReadRustFile {
    public static void read_init() {
        Tokenizer tokenizer = new Tokenizer();
        try (Scanner scanner = new Scanner(System.in)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                tokenizer.tokenize(line);
            }
        }
        // output the tokens
        for (token_t token : tokenizer.tokens) {
            System.out.println("Token Type: " + token.tokentype + ", Name: " + token.name);
        }
        // then we need to use the parser to parse the tokens into an AST
        Parser parser = new Parser(tokenizer.tokens);
        ASTNode ast = parser.parse();
        // output the AST
    }
}

