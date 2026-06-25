package com.thebiggestdata.search.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchConfigurationTest {

    @Test
    void loadsDefaultsAndExplicitSortOrder() {
        SearchConfiguration defaults = SearchConfiguration.load(Map.of());
        SearchConfiguration byId = SearchConfiguration.load(Map.of("SORTING_CRITERIA", "id"));

        assertEquals(7003, defaults.servicePort());
        assertEquals(SearchConfiguration.SortOrder.FREQUENCY, defaults.sortOrder());
        assertEquals(SearchConfiguration.SortOrder.ID, byId.sortOrder());
    }

    @Test
    void rejectsUnknownSortOrder() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SearchConfiguration.load(Map.of("SORTING_CRITERIA", "random"))
        );
    }
}
