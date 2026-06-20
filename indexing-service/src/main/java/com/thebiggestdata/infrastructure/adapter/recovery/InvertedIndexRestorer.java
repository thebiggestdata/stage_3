package com.thebiggestdata.infrastructure.adapter.recovery;


import com.thebiggestdata.usecase.IndexBookUseCase;
import com.thebiggestdata.domain.gateway.BookRepository;
import com.thebiggestdata.domain.gateway.ReindexRunner;
import com.thebiggestdata.domain.entity.BookText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class InvertedIndexRestorer implements ReindexRunner {

    private static final Logger log = LoggerFactory.getLogger(InvertedIndexRestorer.class);

    private final String dataVolumePath;
    private final IndexBookUseCase indexBookUseCase;
    private final BookRepository bookStore;

    public InvertedIndexRestorer(String dataVolumePath, IndexBookUseCase indexBookUseCase, BookRepository bookStore) {
        this.dataVolumePath = dataVolumePath;
        this.indexBookUseCase = indexBookUseCase;
        this.bookStore = bookStore;
    }

    @Override
    public int executeRecovery() {
        Path datalakePath = Paths.get(this.dataVolumePath);
        final int[] maxBookId = {0};

        try (Stream<Path> paths = Files.walk(datalakePath)) {
            paths.filter(p -> p.getFileName().toString().endsWith("_body.txt"))
                    .forEach(bodyPath -> {
                        try {
                            int bookId = extractBookId(bodyPath.getFileName().toString());
                            if (bookId > maxBookId[0]) maxBookId[0] = bookId;

                            Path headerPath = bodyPath.getParent().resolve(bookId + "_header.txt");
                            String header = Files.readString(headerPath);
                            String body = Files.readString(bodyPath);

                            bookStore.save(bookId, new BookText(header, body));

                            indexBookUseCase.execute(bookId);

                        } catch (Exception e) {
                            log.error("Failed to recover book from path: {}", bodyPath, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("No local data found in disk: {}", dataVolumePath);
        }
        return maxBookId[0];
    }

    private int extractBookId(String filename) {
        String suffix = "_body.txt";
        int index = filename.indexOf(suffix);
        return Integer.parseInt(filename.substring(0, index));
    }
}
