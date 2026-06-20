package com.thebiggestdata.infrastructure.adapter.hazelcast;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import com.thebiggestdata.domain.entity.NodeDetails;

public class NodeDetailsSerializer implements CompactSerializer<NodeDetails> {

    @Override
    public NodeDetails read(CompactReader reader) {
        String nodeId = reader.readString("nodeId");
        return new NodeDetails(nodeId);
    }

    @Override
    public void write(CompactWriter writer, NodeDetails nodeInformation) {
        writer.writeString("nodeId", nodeInformation.nodeId());
    }

    @Override
    public String getTypeName() {
        return "NodeInfoProvider";
    }

    @Override
    public Class<NodeDetails> getCompactClass() {
        return NodeDetails.class;
    }
}