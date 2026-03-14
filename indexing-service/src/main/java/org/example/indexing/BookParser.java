package org.example.indexing;

import org.example.shared.model.BookMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Parses a book's header and body files, extracting:
 * <ul>
 *   <li>Metadata (title, author, language, release date) from the header.</li>
 *   <li>Unique, normalised terms from the body for the inverted index.</li>
 * </ul>
 */
public class BookParser {

    /** Common English stop-words excluded from the index. */
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a","an","the","and","or","but","in","on","at","to","for","of","with",
            "is","was","are","were","be","been","being","have","has","had","do","does",
            "did","will","would","could","should","may","might","shall","can","need",
            "dare","ought","used","it","its","this","that","these","those","i","you",
            "he","she","we","they","me","him","her","us","them","my","your","his",
            "our","their","not","no","so","if","as","by","from","up","about","into"
    ));

    private static final int MIN_TERM_LENGTH = 3;

    /**
     * Extracts index terms from the body file.
     *
     * @param bodyFile path to the {@code ID.body.txt} file
     * @return set of normalised terms
     */
    public Set<String> extractTerms(Path bodyFile) throws IOException {
        String text = Files.readString(bodyFile);
        return Arrays.stream(text.split("[^a-zA-Z]+"))
                .map(String::toLowerCase)
                .filter(w -> w.length() >= MIN_TERM_LENGTH)
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());
    }

    /**
     * Parses basic metadata from the header file.
     *
     * @param headerFile path to the {@code ID.header.txt} file
     * @param bookId     Gutenberg book ID
     * @return populated {@link BookMetadata}
     */
    public BookMetadata parseMetadata(Path headerFile, String bookId) throws IOException {
        List<String> lines = Files.readAllLines(headerFile);

        String title       = extractField(lines, "Title:");
        String author      = extractField(lines, "Author:");
        String language    = extractField(lines, "Language:");
        String releaseDate = extractField(lines, "Release Date:");

        return new BookMetadata(bookId, title, author, language, releaseDate,
                headerFile.getParent().toString());
    }

    // -------------------------------------------------------------------------

    private String extractField(List<String> lines, String prefix) {
        return lines.stream()
                .filter(l -> l.startsWith(prefix))
                .map(l -> l.substring(prefix.length()).trim())
                .findFirst()
                .orElse("Unknown");
    }
}
