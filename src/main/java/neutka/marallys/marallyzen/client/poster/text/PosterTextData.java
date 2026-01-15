package neutka.marallys.marallyzen.client.poster.text;

import java.util.List;

/**
 * Immutable data class for poster text content.
 * Contains title, pages (body text), author, and style information.
 * No rendering logic here - only data.
 */
public record PosterTextData(
    String title,
    List<String> pages,
    String author,
    PosterStyle style
) {
    /**
     * Creates a PosterTextData with empty content.
     */
    public static PosterTextData empty(PosterStyle style) {
        return new PosterTextData("", List.of(), "", style);
    }
    
    /**
     * Checks if this data contains any text to render.
     */
    public boolean isEmpty() {
        return (title == null || title.isEmpty()) && 
               (pages == null || pages.isEmpty() || pages.stream().allMatch(String::isEmpty)) &&
               (author == null || author.isEmpty());
    }
}

