package org.example.indexing;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.multimap.MultiMap;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;
import org.example.shared.Constants;
import org.example.shared.model.BookMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Subscribes to the {@code book-downloaded} Hazelcast topic.
 * When a message arrives containing the body-file path, it parses the book
 * and updates the distributed inverted index (MultiMap) and metadata map (IMap).
 */
public class IndexingService implements MessageListener<String> {

    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    private final HazelcastInstance hazelcast;
    private final BookParser parser;
    private final MultiMap<String, String> invertedIndex;
    private final IMap<String, BookMetadata> metadataMap;

    public IndexingService(HazelcastInstance hazelcast) {
        this.hazelcast    = hazelcast;
        this.parser       = new BookParser();
        this.invertedIndex = hazelcast.getMultiMap(Constants.INVERTED_INDEX_MAP);
        this.metadataMap   = hazelcast.getMap(Constants.BOOK_METADATA_MAP);
    }

    /** Subscribes to the topic. */
    public void start() {
        ITopic<String> topic = hazelcast.getTopic(Constants.BOOK_DOWNLOADED_TOPIC);
        topic.addMessageListener(this);
        log.info("Subscribed to topic '{}'", Constants.BOOK_DOWNLOADED_TOPIC);
    }

    @Override
    public void onMessage(Message<String> message) {
        String bodyFilePath = message.getMessageObject();
        log.info("Received indexing event for: {}", bodyFilePath);
        try {
            indexBook(bodyFilePath);
        } catch (IOException e) {
            log.error("Failed to index book at {}", bodyFilePath, e);
        }
    }

    /**
     * Parses the book and updates the distributed index.
     *
     * @param bodyFilePath absolute path to the body text file
     */
    public void indexBook(String bodyFilePath) throws IOException {
        Path bodyFile = Paths.get(bodyFilePath);
        if (!Files.exists(bodyFile)) {
            log.warn("Body file not found: {}", bodyFilePath);
            return;
        }

        // Derive bookId from file name (e.g., "1342.body.txt" -> "1342")
        String fileName = bodyFile.getFileName().toString();
        String bookId   = fileName.replace(".body.txt", "");

        // Parse metadata from matching header file
        Path headerFile = bodyFile.resolveSibling(bookId + ".header.txt");
        BookMetadata metadata = new BookMetadata(bookId, "Unknown", "Unknown",
                "Unknown", "Unknown", bodyFile.getParent().toString());
        if (Files.exists(headerFile)) {
            metadata = parser.parseMetadata(headerFile, bookId);
        }

        // Store metadata in distributed map
        metadataMap.put(bookId, metadata);

        // Extract terms and update inverted index
        Set<String> terms = parser.extractTerms(bodyFile);
        log.info("Indexing {} terms for book {}", terms.size(), bookId);

        for (String term : terms) {
            invertedIndex.put(term, bookId);
        }

        log.info("Indexed book {} ('{}') with {} terms", bookId, metadata.getTitle(), terms.size());
    }
}
