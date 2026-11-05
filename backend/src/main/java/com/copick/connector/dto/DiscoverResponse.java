package com.copick.connector.dto;

import java.util.List;

public record DiscoverResponse(
        List<ConnectorMovie> movies,
        int total_count
) {}