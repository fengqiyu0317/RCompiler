public class EscapeSequenceProcessor {
    
    public static int processEscapeInToken(token_t token, String input, int position) {
        char escapeChar = input.charAt(position + 1);
        switch (escapeChar) {
            case TokenizerConstants.LETTER_N:
                token.name += TokenizerConstants.LINE_FEED;
                break;
            case TokenizerConstants.LETTER_T:
                token.name += TokenizerConstants.TAB;
                break;
            case TokenizerConstants.LETTER_R:
                token.name += TokenizerConstants.CARRIAGE_RETURN;
                break;
            case TokenizerConstants.BACKSLASH:
                token.name += TokenizerConstants.BACKSLASH;
                break;
            case TokenizerConstants.SINGLE_QUOTE:
                token.name += TokenizerConstants.SINGLE_QUOTE;
                break;
            case TokenizerConstants.DOUBLE_QUOTE:
                token.name += TokenizerConstants.DOUBLE_QUOTE;
                break;
            case TokenizerConstants.DIGIT_ZERO:
                token.name += TokenizerConstants.NULL;
                break;
            case TokenizerConstants.LETTER_X:
                // hexadecimal escape sequence
                if (position + 3 >= input.length()) {
                    throw new TokenizerException(TokenizerConstants.INCOMPLETE_HEX_ESCAPE);
                }
                String hex = input.substring(position + 2, position + 4);
                try {
                    token.name += (char) Integer.parseInt(hex, 16);
                } catch (NumberFormatException e) {
                    throw new TokenizerException(TokenizerConstants.INVALID_HEX_ESCAPE + hex);
                }
                return position + 3; // Skip x and two hex digits
            default:
                throw new TokenizerException(TokenizerConstants.UNKNOWN_ESCAPE_SEQUENCE + escapeChar);
        }
        return position + 1; // Skip the escape character
    }
    
    private static char processHexEscape(String input, int position) {
        if (position + 3 >= input.length()) {
            throw new TokenizerException(TokenizerConstants.INCOMPLETE_HEX_ESCAPE);
        }
        String hex = input.substring(position + 2, position + 4);
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw new TokenizerException(TokenizerConstants.INVALID_HEX_ESCAPE + hex);
        }
    }
}