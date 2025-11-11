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