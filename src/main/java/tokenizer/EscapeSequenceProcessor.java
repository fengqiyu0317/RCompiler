import static tokenizer.TokenizerConstants.*;

public class EscapeSequenceProcessor {
    
    public static int processEscapeInToken(token_t token, String input, int position) {
        char escapeChar = input.charAt(position + 1);
        switch (escapeChar) {
            case LETTER_N:
                token.name += LINE_FEED;
                break;
            case LETTER_T:
                token.name += TAB;
                break;
            case LETTER_R:
                token.name += CARRIAGE_RETURN;
                break;
            case BACKSLASH:
                token.name += BACKSLASH;
                break;
            case SINGLE_QUOTE:
                token.name += SINGLE_QUOTE;
                break;
            case DOUBLE_QUOTE:
                token.name += DOUBLE_QUOTE;
                break;
            case DIGIT_ZERO:
                token.name += NULL;
                break;
            case LETTER_X:
                // hexadecimal escape sequence
                if (position + 3 >= input.length()) {
                    throw new TokenizerException(INCOMPLETE_HEX_ESCAPE);
                }
                String hex = input.substring(position + 2, position + 4);
                try {
                    token.name += (char) Integer.parseInt(hex, 16);
                } catch (NumberFormatException e) {
                    throw new TokenizerException(INVALID_HEX_ESCAPE + hex);
                }
                return position + 3; // Skip x and two hex digits
            default:
                throw new TokenizerException(UNKNOWN_ESCAPE_SEQUENCE + escapeChar);
        }
        return position + 1; // Skip the escape character
    }
    
    private static char processHexEscape(String input, int position) {
        if (position + 3 >= input.length()) {
            throw new TokenizerException(INCOMPLETE_HEX_ESCAPE);
        }
        String hex = input.substring(position + 2, position + 4);
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw new TokenizerException(INVALID_HEX_ESCAPE + hex);
        }
    }
}