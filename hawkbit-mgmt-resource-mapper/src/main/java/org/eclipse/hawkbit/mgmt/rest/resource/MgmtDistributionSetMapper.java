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

import java.util.List;

import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSet;
import org.eclipse.hawkbit.mgmt.json.model.distributionset.MgmtDistributionSetRequestBodyPost;
import org.eclipse.hawkbit.mgmt.json.model.softwaremodule.MgmtSoftwareModuleAssigment;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.SoftwareManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
@Mapper
public abstract class MgmtDistributionSetMapper {

    @Autowired
    private SoftwareManagement softwareManagement;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    // @Autowired
    // public MgmtDistributionSetMapper(final SoftwareManagement
    // softwareManagement,
    // final DistributionSetManagement distributionSetManagement) {
    // this.softwareManagement = softwareManagement;
    // this.distributionSetManagement = distributionSetManagement;
    // }

    // TODO modules!!!wiederverwenden uses
    /**
     * Create a response for distribution set.
     * 
     * @param distributionSet
     *            the ds set
     * @return the response
     */
    @Mapping(target = "dsId", source = "id")
    @Mapping(target = "type", source = "type.key")
    @Mapping(target = "modules", ignore = true)
    @Mapping(target = "links", ignore = true)
    public abstract MgmtDistributionSet distributionSetToMgmtDistributionSet(final DistributionSet distributionSet);

    public abstract List<MgmtDistributionSet> distributionSetToMgmtDistributionSet(
            final List<DistributionSet> distributionSet);

    public abstract List<MgmtDistributionSet> distributionSetToMgmtDistributionSet(
            final Iterable<DistributionSet> distributionSet);

    @AfterMapping
    protected void addLinks(@MappingTarget final MgmtDistributionSet mgmtTarget,
            final DistributionSet distributionSet) {
        mgmtTarget.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getDistributionSet(mgmtTarget.getDsId()))
                .withRel("self"));

        mgmtTarget.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class)
                .getDistributionSetType(distributionSet.getType().getId())).withRel("type"));

        mgmtTarget.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getMetadata(mgmtTarget.getDsId(),
                Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET),
                Integer.parseInt(MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT), null, null))
                        .withRel("metadata"));
    }

    /**
     * {@link MgmtDistributionSetRequestBodyPost} to {@link DistributionSet}.
     *
     * @param mgmtDistributionSetRequestBodyPost
     *            to convert
     * @return converted {@link DistributionSet}
     */
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastModifiedAt", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "tenant", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "actions", ignore = true)
    @Mapping(target = "assignedTargets", ignore = true)
    @Mapping(target = "installedTargets", ignore = true)
    @Mapping(target = "metadata", ignore = true)
    @Mapping(target = "modules", ignore = true)
    public abstract DistributionSet mgmtDistributionSetToDistributionSet(
            final MgmtDistributionSetRequestBodyPost mgmtDistributionSetRequestBodyPost);

    public abstract List<DistributionSet> mgmtDistributionSetToDistributionSet(
            final List<MgmtDistributionSetRequestBodyPost> mgmtDistributionSetRequestBodyPost);

    @AfterMapping
    protected void addSoftwareModules(@MappingTarget final DistributionSet distributionSet,
            final MgmtDistributionSetRequestBodyPost mgmtDistributionSetRequestBodyPost) {
        if (mgmtDistributionSetRequestBodyPost.getOs() != null) {
            distributionSet.addModule(
                    findSoftwareModuleWithExceptionIfNotFound(mgmtDistributionSetRequestBodyPost.getOs().getId()));
        }

        if (mgmtDistributionSetRequestBodyPost.getApplication() != null) {
            distributionSet.addModule(findSoftwareModuleWithExceptionIfNotFound(
                    mgmtDistributionSetRequestBodyPost.getApplication().getId()));
        }

        if (mgmtDistributionSetRequestBodyPost.getRuntime() != null) {
            distributionSet.addModule(
                    findSoftwareModuleWithExceptionIfNotFound(mgmtDistributionSetRequestBodyPost.getRuntime().getId()));
        }

        if (mgmtDistributionSetRequestBodyPost.getModules() != null) {
            mgmtDistributionSetRequestBodyPost.getModules().forEach(
                    module -> distributionSet.addModule(findSoftwareModuleWithExceptionIfNotFound(module.getId())));
        }
    }

    protected void addOs(final MgmtSoftwareModuleAssigment os) {
    }

    protected DistributionSetType findDistributionSetTypeWithExceptionIfNotFound(final String distributionSetTypekey) {
        final DistributionSetType module = distributionSetManagement
                .findDistributionSetTypeByKey(distributionSetTypekey);
        if (module == null) {
            throw new EntityNotFoundException(
                    "DistributionSetType with key {" + distributionSetTypekey + "} does not exist");
        }
        return module;
    }

    protected SoftwareModule findSoftwareModuleWithExceptionIfNotFound(final Long softwareModuleId) {
        final SoftwareModule module = softwareManagement.findSoftwareModuleById(softwareModuleId);
        if (module == null) {
            throw new EntityNotFoundException("SoftwareModule with Id {" + softwareModuleId + "} does not exist");
        }

        return module;
    }

}
