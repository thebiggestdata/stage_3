package com.thebiggestdata.search.infrastructure.ports;

import com.thebiggestdata.search.model.HealthStatus;

public interface HealthCheck {
    HealthStatus check();
}
