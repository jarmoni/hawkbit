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

import org.eclipse.hawkbit.mgmt.json.model.action.MgmtActionStatus;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
@Mapper
public abstract class MgmtActionStatusMapper {

    @Mapping(target = "reportedAt", source = "createdAt")
    @Mapping(target = "statusId", source = "id")
    @Mapping(target = "type", ignore = true)
    public abstract MgmtActionStatus actionStatustoMgmtActionStatus(final ActionStatus actionStatus);

    public abstract List<MgmtActionStatus> actionStatustoMgmtActionStatus(final List<ActionStatus> actionStatus);

    protected String toLowerCase(final Action.Status type) {
        return type.toString().toLowerCase();
    }
}
