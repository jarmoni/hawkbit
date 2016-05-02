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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.MgmtPollStatus;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTarget;
import org.eclipse.hawkbit.mgmt.json.model.target.MgmtTargetRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo.PollStatus;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.rest.data.SortDirection;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
@Mapper(uses = { LocalDateTimeMapper.class })
public abstract class MgmtTargetMapper {

    @Mapping(target = "pollStatus", source = "targetInfo.pollStatus")
    @Mapping(target = "address", source = "targetInfo.address")
    @Mapping(target = "ipAddress", source = "targetInfo.address.host")
    @Mapping(target = "lastControllerRequestAt", source = "targetInfo.lastTargetQuery")
    @Mapping(target = "installedAt", source = "targetInfo.installationDate")
    @Mapping(target = "updateStatus", source = "targetInfo.updateStatus")
    @Mapping(target = "links", ignore = true)
    public abstract MgmtTarget targetToMgmtTarget(Target target);

    public abstract List<MgmtTarget> targetToMgmtTarget(Iterable<Target> target);

    @Mapping(target = "lastRequestAt", source = "lastPollDate")
    @Mapping(target = "nextExpectedRequestAt", source = "nextPollDate")
    public abstract MgmtPollStatus pollStatusToMgmtPollStatus(PollStatus pollStatus);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "assignedDistributionSet", ignore = true)
    @Mapping(target = "new", ignore = true)
    @Mapping(target = "securityToken", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "targetInfo", ignore = true)
    @Mapping(target = "actions", ignore = true)
    public abstract Target mgmtTargetToTarget(MgmtTargetRequestBody mgmtTargetRequestBody);

    // public Target createTarget(final MgmtTargetRequestBody
    // mgmtTargetRequestBody) {
    // return new Target(mgmtTargetRequestBody.getControllerId());
    // }

    // public abstract List<Target>
    // mgmtTargetToTarget(Iterable<MgmtTargetRequestBody>
    // mgmtTargetRequestBody);

    protected String toString(final URI uri) {
        return uri.toString();
    }

    protected String toLowerCase(final TargetUpdateStatus targetUpdateStatus) {
        return targetUpdateStatus.toString().toLowerCase();
    }

    @AfterMapping
    protected void addSelfLink(@MappingTarget final MgmtTarget mgmtTarget) {
        mgmtTarget
                .add(linkTo(methodOn(MgmtTargetRestApi.class).getTarget(mgmtTarget.getControllerId())).withRel("self"));
    }

    /**
     * Warum unterschiedliches Methode mit Links?
     */
    public static List<MgmtTarget> toResponseWithLinksAndPollStatus(final Iterable<Target> targets) {
        final List<MgmtTarget> mappedList = new ArrayList<>();
        if (targets != null) {
            for (final Target target : targets) {
                // final MgmtTarget response = toResponse(target);
                // addPollStatus(target, response);
                // addTargetLinks(response);
                // mappedList.add(response);
            }
        }
        return mappedList;
    }

    // TODO
    /**
     * Add links to a target response.
     *
     * @param response
     *            the target response
     */
    public void addTargetLinks(final MgmtTarget response) {
        response.add(linkTo(methodOn(MgmtTargetRestApi.class).getAssignedDistributionSet(response.getControllerId()))
                .withRel(MgmtRestConstants.TARGET_V1_ASSIGNED_DISTRIBUTION_SET));
        response.add(linkTo(methodOn(MgmtTargetRestApi.class).getInstalledDistributionSet(response.getControllerId()))
                .withRel(MgmtRestConstants.TARGET_V1_INSTALLED_DISTRIBUTION_SET));
        response.add(linkTo(methodOn(MgmtTargetRestApi.class).getAttributes(response.getControllerId()))
                .withRel(MgmtRestConstants.TARGET_V1_ATTRIBUTES));
        response.add(linkTo(methodOn(MgmtTargetRestApi.class).getActionHistory(response.getControllerId(), 0,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE,
                ActionFields.ID.getFieldName() + ":" + SortDirection.DESC, null))
                        .withRel(MgmtRestConstants.TARGET_V1_ACTIONS));
    }

}
