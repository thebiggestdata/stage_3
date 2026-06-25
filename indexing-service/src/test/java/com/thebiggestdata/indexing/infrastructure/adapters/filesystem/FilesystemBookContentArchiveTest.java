package com.thebiggestdata.indexing.infrastructure.adapters.filesystem;

import com.thebiggestdata.indexing.model.BookContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilesystemBookContentArchiveTest {

    @TempDir
    Path root;

    @Test
    void readsArchivedBookByIdWithoutScanningTheWholeDatalake() throws IOException {
        Path shard = root.resolve("12");
        Files.createDirectories(shard);
        Files.writeString(shard.resolve("123_header.txt"), "header");
        Files.writeString(shard.resolve("123_body.txt"), "body");

        Optional<BookContent> content = new FilesystemBookContentArchive(root).find(123);

        assertTrue(content.isPresent());
        assertEquals(new BookContent("header", "body"), content.orElseThrow());
    }

    @Test
    void returnsEmptyWhenArchivedFilesAreMissing() {
        Optional<BookContent> content = new FilesystemBookContentArchive(root).find(123);

        assertTrue(content.isEmpty());
    }
}
