package com.thebiggestdata.ingestion.infrastructure.ports;

import com.thebiggestdata.ingestion.model.BookIngestedEvent;

public interface BookIngestedNotifierPort {
    void notify(BookIngestedEvent event); //TODO maybe change it to 'public' (to the broker)
}
