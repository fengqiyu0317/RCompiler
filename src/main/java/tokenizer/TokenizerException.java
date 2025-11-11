public class TokenizerException extends RuntimeException {
    public final int line;
    public final int column;
    
    public TokenizerException(String message, int line, int column) {
        super("Error at line " + line + ", column " + column + ": " + message);
        this.line = line;
        this.column = column;
    }
    
    public TokenizerException(String message) {
        super(message);
        this.line = -1;
        this.column = -1;
    }
}