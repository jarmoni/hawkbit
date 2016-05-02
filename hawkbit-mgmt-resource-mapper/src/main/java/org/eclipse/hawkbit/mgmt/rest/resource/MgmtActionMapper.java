/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import org.eclipse.hawkbit.mgmt.json.model.action.MgmtAction;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.eclipse.hawkbit.repository.model.Action;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.TargetType;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
@Mapper
public abstract class MgmtActionMapper {

    @Mapping(target = "actionId", source = "action.id")
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "links", ignore = true)
    public abstract MgmtAction actiontoMgmtAction(final Action action, final String targetId);

    public void actiontoMgmtAction(@MappingTarget final MgmtAction mgmtAction, @TargetType final Action action) {
        mgmtAction.setStatus(getStatus(action));
        mgmtAction.setType(getType(action));
    }

    @AfterMapping
    protected void addSelfLink(final String targetId, @MappingTarget final MgmtAction mgmtAction) {
        mgmtAction.add(linkTo(methodOn(MgmtTargetRestApi.class).getAction(targetId, mgmtAction.getActionId()))
                .withRel("self"));
    }

    public String getStatus(final Action action) {
        return action.isActive() ? MgmtAction.ACTION_PENDING : MgmtAction.ACTION_FINISHED;
    }

    protected String getType(final Action action) {
        if (action.isCancelingOrCanceled()) {
            return MgmtAction.ACTION_CANCEL;
        }

        return MgmtAction.ACTION_UPDATE;
    }

}
