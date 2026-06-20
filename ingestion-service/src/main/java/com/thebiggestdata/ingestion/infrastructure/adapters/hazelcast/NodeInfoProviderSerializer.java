package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import com.thebiggestdata.ingestion.model.Node;

public class NodeInfoProviderSerializer implements CompactSerializer<Node> {

    @Override
    public Node read(CompactReader reader) {
        String nodeId = reader.readString("nodeId");
        return new Node(nodeId);
    }

    @Override
    public void write(CompactWriter writer, Node node) {
        writer.writeString("nodeId", node.nodeId());
    }

    @Override
    public String getTypeName() {
        return "NodeInfoProvider";
    }

    @Override
    public Class<Node> getCompactClass() {
        return Node.class;
    }
}