package com.thebiggestdata.infrastructure.adapters.hazelcast;

import com.thebiggestdata.model.BookContent;
import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

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
