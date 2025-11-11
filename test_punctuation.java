import java.util.Vector;
import static tokenizer.TokenizerConstants.*;

public class test_punctuation {
    public static void main(String[] args) {
        // Test that MULTI_CHAR_PUNCTUATIONS is sorted by length (longest first)
        System.out.println("Testing MULTI_CHAR_PUNCTUATIONS order:");
        
        for (int i = 0; i < MULTI_CHAR_PUNCTUATIONS.length; i++) {
            System.out.println(MULTI_CHAR_PUNCTUATIONS[i] + " (length: " + MULTI_CHAR_PUNCTUATIONS[i].length() + ")");
            
            // Verify that each punctuation is not longer than the previous one
            if (i > 0) {
                int prevLength = MULTI_CHAR_PUNCTUATIONS[i-1].length();
                int currLength = MULTI_CHAR_PUNCTUATIONS[i].length();
                if (currLength > prevLength) {
                    System.err.println("ERROR: Punctuation " + MULTI_CHAR_PUNCTUATIONS[i] + 
                                      " is longer than previous punctuation " + MULTI_CHAR_PUNCTUATIONS[i-1]);
                    return;
                }
            }
        }
        
        System.out.println("All punctuations are correctly sorted by length (longest first)!");
        
        // Test the tokenizer with some examples
        System.out.println("\nTesting tokenizer with examples:");
        Tokenizer tokenizer = new Tokenizer();
        
        String testInput = "a <<= b >>= c ... d .. e == f != g && h || i";
        tokenizer.tokenize(testInput);
        
        System.out.println("Input: " + testInput);
        System.out.println("Tokens:");
        for (token_t token : tokenizer.tokens) {
            System.out.println("  " + token.name);
        }
    }
}