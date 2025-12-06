package com.thebiggestdata.ingestion.infrastructure.adapter.hazlecast;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.map.MapEvent;

public abstract class HzlcstEntryListener<K,V> implements EntryListener<K, V> {
    @Override public void entryAdded(EntryEvent<K, V> entryEvent) {}
    @Override public void entryEvicted(EntryEvent<K, V> entryEvent) {}
    @Override public void entryExpired(EntryEvent<K, V> entryEvent) {}
    @Override public void entryRemoved(EntryEvent<K, V> entryEvent) {}
    @Override public void entryUpdated(EntryEvent<K, V> entryEvent) {}
    @Override public void mapCleared(MapEvent mapEvent) {}
    @Override public void mapEvicted(MapEvent mapEvent) {}
}
