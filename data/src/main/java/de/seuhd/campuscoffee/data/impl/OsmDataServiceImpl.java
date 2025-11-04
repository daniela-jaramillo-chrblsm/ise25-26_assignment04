package de.seuhd.campuscoffee.data.impl;

import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

/**
 * OSM import service.
 */
@Service
@Slf4j
class OsmDataServiceImpl implements OsmDataService {

    @Override
    public @NonNull OsmNode fetchNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.warn("Using stub OSM import service - returning hardcoded data for node {}", nodeId);

        // TODO: This returns hardcoded data and should be replaced with a real HTTP client implementation that calls
        //  the OpenStreetMap API: https://www.openstreetmap.org/api/0.6/node/{id}
        if (nodeId.equals(5589879349L)) {
            return OsmNode.builder()
                    .nodeId(nodeId)
                    .build();
        } else {
            // For any other node ID, throw not found exception
            throw new OsmNodeNotFoundException(nodeId);
        }
    }
}
