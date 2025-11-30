public class TokenizerConstants {
    public static final String[] MULTI_CHAR_PUNCTUATIONS = {
        "<<=", ">>=", "...", "..=", "##",  // 3-character punctuations
        "==", "<=", ">=", "!=", "&&", "||", "<<", ">>",
        "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=",
        "->", "=>", "::", "..", "<-"        // 2-character punctuations
    };
    
    public static final char[] VALID_ESCAPE_CHARS = {
        'n', 't', 'r', '\\', '\'', '\"', '0', 'x'
    };
    
    public static final String BINARY_PREFIX = "0b";
    public static final String OCTAL_PREFIX = "0o";
    public static final String HEX_PREFIX = "0x";
    
    public static final char LINE_FEED = '\n';
    public static final char CARRIAGE_RETURN = '\r';
    public static final char TAB = '\t';
    public static final char NULL = '\0';
    public static final char BACKSLASH = '\\';
    public static final char SINGLE_QUOTE = '\'';
    public static final char DOUBLE_QUOTE = '\"';
    public static final char HASH = '#';
    public static final char FORWARD_SLASH = '/';
    public static final char ASTERISK = '*';
    public static final char UNDERSCORE = '_';
    
    // Additional character constants
    public static final char LETTER_B = 'b';
    public static final char LETTER_O = 'o';
    public static final char LETTER_X = 'x';
    public static final char LETTER_R = 'r';
    public static final char LETTER_C = 'c';
    public static final char LETTER_E = 'e';
    public static final char LETTER_N = 'n';
    public static final char LETTER_T = 't';
    public static final char DIGIT_ZERO = '0';
    public static final char DIGIT_ONE = '1';
    public static final char DIGIT_SEVEN = '7';
    public static final char DIGIT_NINE = '9';
    
    // String constants for error messages
    public static final String UNKNOWN_ESCAPE_SEQUENCE = "Unknown escape sequence: \\";
    public static final String INVALID_CHAR_LITERAL = "Invalid character literal";
    public static final String INVALID_RAW_STRING_LITERAL = "Invalid raw string literal";
    public static final String INCOMPLETE_HEX_ESCAPE = "Incomplete hexadecimal escape sequence";
    public static final String INVALID_HEX_ESCAPE = "Invalid hexadecimal escape sequence: \\";
    public static final String UNKNOWN_CHARACTER = "Unknown character: ";
    public static final String MISSING_CONTENT = "Invalid character literal: missing content";
    public static final String INCOMPLETE_ESCAPE = "Invalid character literal: incomplete escape sequence";
    public static final String MISSING_CLOSING_QUOTE = "Invalid character literal: missing closing quote";
    public static final String LAST_TOKEN_COMPLETED = "last_token should not be completed.";
    
    // Base type strings for error messages
    public static final String BINARY_TYPE = "binary";
    public static final String OCTAL_TYPE = "octal";
    public static final String HEXADECIMAL_TYPE = "hexadecimal";
    public static final String NO_VALID_DIGITS = " literal: no valid digits found";
}