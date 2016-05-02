/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadata;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtTargetAssignmentResponseBody;
import org.eclipse.hawkbit.repository.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
public final class MgmtDistributionSetMapper {
    private MgmtDistributionSetMapper() {
        // Utility class
    }

    /**
     * From {@link MgmtMetadata} to {@link DistributionSetMetadata}.
     *
     * @param ds
     * @param metadata
     * @return
     */
    static List<DistributionSetMetadata> fromRequestDsMetadata(final DistributionSet ds,
            final List<MgmtMetadata> metadata) {
        final List<DistributionSetMetadata> mappedList = new ArrayList<>(metadata.size());
        for (final MgmtMetadata metadataRest : metadata) {
            if (metadataRest.getKey() == null) {
                throw new IllegalArgumentException("the key of the metadata must be present");
            }
            mappedList.add(new DistributionSetMetadata(metadataRest.getKey(), ds, metadataRest.getValue()));
        }
        return mappedList;
    }

    static MgmtTargetAssignmentResponseBody toResponse(final DistributionSetAssignmentResult dsAssignmentResult) {
        final MgmtTargetAssignmentResponseBody result = new MgmtTargetAssignmentResponseBody();
        result.setAssigned(dsAssignmentResult.getAssigned());
        result.setAlreadyAssigned(dsAssignmentResult.getAlreadyAssigned());
        result.setTotal(dsAssignmentResult.getTotal());
        return result;
    }

    static List<MgmtMetadata> toResponseDsMetadata(final List<DistributionSetMetadata> metadata) {

        final List<MgmtMetadata> mappedList = new ArrayList<>(metadata.size());
        for (final DistributionSetMetadata distributionSetMetadata : metadata) {
            mappedList.add(toResponseDsMetadata(distributionSetMetadata));
        }
        return mappedList;
    }

}
