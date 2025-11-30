public class StringLiteralProcessor {
    
    public static boolean isStringStart(char c, String input, int position) {
        if (c == TokenizerConstants.DOUBLE_QUOTE) return true;
        if (c == TokenizerConstants.LETTER_R && position + 1 < input.length() && input.charAt(position + 1) == TokenizerConstants.HASH) return true;
        if (c == TokenizerConstants.LETTER_C && position + 1 < input.length() && input.charAt(position + 1) == TokenizerConstants.DOUBLE_QUOTE) return true;
        if (c == TokenizerConstants.LETTER_C && position + 2 < input.length() &&
            input.charAt(position + 1) == TokenizerConstants.LETTER_R && input.charAt(position + 2) == TokenizerConstants.HASH) return true;
        return false;
    }
    
    public static token_t createStringToken(String input, int position) {
        char c = input.charAt(position);
        if (c == TokenizerConstants.DOUBLE_QUOTE) {
            return createRegularStringToken();
        } else if (c == TokenizerConstants.LETTER_R) {
            return createRawStringToken(input, position);
        } else if (c == TokenizerConstants.LETTER_C && input.charAt(position + 1) == TokenizerConstants.DOUBLE_QUOTE) {
            return createCStringToken();
        } else if (c == TokenizerConstants.LETTER_C && input.charAt(position + 1) == TokenizerConstants.LETTER_R) {
            return createRawCStringToken(input, position);
        }
        return null;
    }
    
    private static token_t createRegularStringToken() {
        token_t token = new token_t();
        token.tokentype = token_t.TokenType_t.STRING_LITERAL_MID;
        return token;
    }
    
    private static token_t createRawStringToken(String input, int position) {
        token_t token = new token_t();
        token.tokentype = token_t.TokenType_t.RAW_STRING_LITERAL_MID;
        token.number_raw = countHashes(input, position + 1);
        return token;
    }
    
    private static token_t createCStringToken() {
        token_t token = new token_t();
        token.tokentype = token_t.TokenType_t.C_STRING_LITERAL_MID;
        return token;
    }
    
    private static token_t createRawCStringToken(String input, int position) {
        token_t token = new token_t();
        token.tokentype = token_t.TokenType_t.RAW_C_STRING_LITERAL_MID;
        token.number_raw = countHashes(input, position + 2);
        return token;
    }
    
    private static int countHashes(String input, int startPosition) {
        int count = 0;
        int j = startPosition;
        while (j < input.length() && input.charAt(j) == TokenizerConstants.HASH) {
            count++;
            j++;
        }
        return count;
    }
    
    public static int processStringStart(String input, int position) {
        char c = input.charAt(position);
        if (c == TokenizerConstants.DOUBLE_QUOTE) {
            return position + 1;
        } else if (c == TokenizerConstants.LETTER_R) {
            int j = position + 1;
            while (j < input.length() && input.charAt(j) == TokenizerConstants.HASH) {
                j++;
            }
            return j + 1; // Skip the opening quote
        } else if (c == TokenizerConstants.LETTER_C && input.charAt(position + 1) == TokenizerConstants.DOUBLE_QUOTE) {
            return position + 2;
        } else if (c == TokenizerConstants.LETTER_C && input.charAt(position + 1) == TokenizerConstants.LETTER_R) {
            int j = position + 2;
            while (j < input.length() && input.charAt(j) == TokenizerConstants.HASH) {
                j++;
            }
            return j + 1; // Skip the opening quote
        }
        return position;
    }
}