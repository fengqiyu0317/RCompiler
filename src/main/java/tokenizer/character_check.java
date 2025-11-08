public class character_check {
    public static boolean isPunctuation(char c) {
        String punctuations = "=<!>&|~+-*/%^@.,:;#$?_{}()[]";
        return punctuations.indexOf(c) != -1;
    }
    public static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
}