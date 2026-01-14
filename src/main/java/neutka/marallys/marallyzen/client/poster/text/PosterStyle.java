package neutka.marallys.marallyzen.client.poster.text;

/**
 * Style enum for poster text rendering.
 * Determines visual appearance of text on different poster types.
 */
public enum PosterStyle {
    /**
     * Old poster style (posterNumber 11) - no text display
     */
    OLD,
    
    /**
     * Paper poster style (posterNumber 12-13) - white background
     * Text color: #1A1A1A
     * Title: scale x1.25, no shadow
     * Author: gray #555555, right-aligned
     */
    PAPER,
    
    /**
     * Modern poster style (posterNumber 1-10) - orange/red background
     * Text color: #2B1B0E
     * Title: scale x1.3, ALL CAPS
     * Author: small, centered
     */
    MODERN;
    
    /**
     * Determines PosterStyle from poster number.
     */
    public static PosterStyle fromPosterNumber(int posterNumber) {
        if (posterNumber == 11) {
            return OLD;
        } else if (posterNumber == 12 || posterNumber == 13) {
            return PAPER;
        } else {
            return MODERN; // posterNumber 1-10
        }
    }
}

