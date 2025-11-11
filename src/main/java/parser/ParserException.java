public class ParserException extends RuntimeException {
    public final String tokenName;
    
    public ParserException(String message, token_t token) {
        super(buildMessage(message, token));
        this.tokenName = token != null ? token.name : null;
    }
    
    public ParserException(String message) {
        super(message);
        this.tokenName = null;
    }
    
    public ParserException(String message, Throwable cause) {
        super(message, cause);
        this.tokenName = null;
    }
    
    private static String buildMessage(String message, token_t token) {
        if (token != null) {
            return "Parse error at token '" + token.name + "': " + message;
        }
        return "Parse error: " + message;
    }
}