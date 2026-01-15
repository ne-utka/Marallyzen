package neutka.marallys.marallyzen.client.poster.text;

import java.util.Objects;

/**
 * Key for caching poster text textures.
 * Contains hash of title + pages + style for efficient lookup.
 */
public record PosterTextKey(int hash) {
    /**
     * Creates a key from PosterTextData.
     */
    public static PosterTextKey from(PosterTextData data) {
        if (data == null) {
            return new PosterTextKey(0);
        }
        
        // Compute hash from title, pages, author, and style
        int hash = Objects.hash(data.title(), data.pages(), data.author(), data.style());
        return new PosterTextKey(hash);
    }
}

