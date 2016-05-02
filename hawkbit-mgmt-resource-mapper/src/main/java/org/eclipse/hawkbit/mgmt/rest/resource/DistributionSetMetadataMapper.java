/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.MgmtMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
@Mapper
public abstract class DistributionSetMetadataMapper {

    @Mapping(target = "key", source = "id.key")
    public abstract MgmtMetadata distributionSetMetadataToMgmtMetadata(final DistributionSetMetadata metadata);

    public abstract List<MgmtMetadata> distributionSetMetadataToMgmtMetadata(
            final List<DistributionSetMetadata> metadata);
}
