package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.ingestion.infrastructure.ports.Datalake;
import com.thebiggestdata.ingestion.model.Book;
import com.thebiggestdata.ingestion.model.BookContent;

public final class HazelcastDatalake implements Datalake {

    private final IMap<Integer, BookContent> books;

    public HazelcastDatalake(HazelcastInstance hazelcast) {
        this.books = hazelcast.getMap(HazelcastNames.DATALAKE);
    }

    @Override
    public void save(Book book) {
        books.put(book.bookId(), book.content());
    }

    BookContent get(int bookId) {
        BookContent content = books.get(bookId);
        if (content == null) {
            throw new HazelcastAdapterException("Book not found in datalake: " + bookId);
        }
        return content;
    }
}
