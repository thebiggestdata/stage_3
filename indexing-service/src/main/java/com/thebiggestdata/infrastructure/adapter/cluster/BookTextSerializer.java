package com.thebiggestdata.infrastructure.adapter.cluster;

import com.thebiggestdata.domain.entity.BookText;
import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

public class BookTextSerializer implements CompactSerializer<BookText> {

    @Override
    public BookText read(CompactReader reader) {
        String header = reader.readString("header");
        String body = reader.readString("body");
        return new BookText(header, body);
    }

    @Override
    public void write(CompactWriter writer, BookText bookContent) {
        writer.writeString("header", bookContent.header());
        writer.writeString("body", bookContent.body());
    }

    @Override
    public String getTypeName() {
        return "BookContent";
    }

    @Override
    public Class<BookText> getCompactClass() {
        return BookText.class;
    }
}
