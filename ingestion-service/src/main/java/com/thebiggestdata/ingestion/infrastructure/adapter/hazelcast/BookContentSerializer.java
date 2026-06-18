package com.thebiggestdata.ingestion.infrastructure.adapter.hazelcast;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import com.thebiggestdata.ingestion.model.BookContent;

public class BookContentSerializer implements CompactSerializer<BookContent> {

    @Override
    public BookContent read(CompactReader reader) {
        String header = reader.readString("header");
        String body = reader.readString("body");
        return new BookContent(header, body);
    }

    @Override
    public void write(CompactWriter writer, BookContent bookContent) {
        writer.writeString("header", bookContent.header());
        writer.writeString("body", bookContent.body());
    }

    @Override
    public String getTypeName() {
        return "BookContent";
    }

    @Override
    public Class<BookContent> getCompactClass() {
        return BookContent.class;
    }
}
