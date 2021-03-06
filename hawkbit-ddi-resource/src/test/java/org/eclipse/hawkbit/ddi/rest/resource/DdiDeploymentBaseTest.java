/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomUtils;
import org.eclipse.hawkbit.repository.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Action.ActionType;
import org.eclipse.hawkbit.repository.model.Action.Status;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.LocalArtifact;
import org.eclipse.hawkbit.repository.model.RepositoryModelConstants;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTestWithMongoDB;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.fest.assertions.core.Condition;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 * Test deployment base from the controller.
 */
@Features("Component Tests - Direct Device Integration API")
@Stories("Deployment Action Resource")
public class DdiDeploymentBaseTest extends AbstractRestIntegrationTestWithMongoDB {

    private static final String HTTP_LOCALHOST = "http://localhost:8080/";
    private static final String HTTPS_LOCALHOST = "https://localhost:8080/";

    @Test()
    @Description("Ensures that artifacts are not found, when softare module does not exists.")
    public void artifactsNotFound() throws Exception {
        final Target target = testdataFactory.createTarget();
        final Long softwareModuleIdNotExist = 1l;
        mvc.perform(get("/{tenant}/controller/v1/{targetNotExist}/softwaremodules/{softwareModuleId}/artifacts",
                tenantAware.getCurrentTenant(), target.getName(), softwareModuleIdNotExist))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test()
    @Description("Ensures that artifacts are found, when software module exists.")
    public void artifactsExists() throws Exception {
        final Target target = testdataFactory.createTarget();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("");

        deploymentManagement.assignDistributionSet(distributionSet.getId(), new String[] { target.getName() });

        final Long softwareModuleId = distributionSet.getModules().stream().findFirst().get().getId();
        mvc.perform(get("/{tenant}/controller/v1/{targetNotExist}/softwaremodules/{softwareModuleId}/artifacts",
                tenantAware.getCurrentTenant(), target.getName(), softwareModuleId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        testdataFactory.createLocalArtifacts(softwareModuleId);

        mvc.perform(get("/{tenant}/controller/v1/{targetNotExist}/softwaremodules/{softwareModuleId}/artifacts",
                tenantAware.getCurrentTenant(), target.getName(), softwareModuleId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[?(@.filename==filename0)]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.filename==filename1)]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.filename==filename2)]", hasSize(1)));

    }

    @Test
    @Description("Forced deployment to a controller. Checks if the resource reponse payload for a given deployment is as expected.")
    public void deplomentForceAction() throws Exception {
        // Prepare test data
        final Target target = entityFactory.generateTarget("4712");
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);

        final byte random[] = RandomUtils.nextBytes(5 * 1024);
        final LocalArtifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "test1", false);
        final LocalArtifact artifactSignature = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "test1.signature", false);

        final Target savedTarget = targetManagement.createTarget(target);

        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).isEmpty();
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(0);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(0);

        List<Target> saved = deploymentManagement.assignDistributionSet(ds.getId(), ActionType.FORCED,
                RepositoryModelConstants.NO_FORCE_TIME, savedTarget.getControllerId()).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);
        saved = deploymentManagement.assignDistributionSet(ds2, saved).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(2);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(2);

        // Run test
        long current = System.currentTimeMillis();
        mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$_links.deploymentBase.href", startsWith("http://localhost/"
                        + tenantAware.getCurrentTenant() + "/controller/v1/4712/deploymentBase/" + uaction.getId())));
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        current = System.currentTimeMillis();

        final DistributionSet findDistributionSetByAction = distributionSetManagement
                .findDistributionSetByAction(action);

        mvc.perform(
                get("/{tenant}/controller/v1/4712/deploymentBase/" + uaction.getId(), tenantAware.getCurrentTenant())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$id", equalTo(String.valueOf(action.getId()))))
                .andExpect(jsonPath("$deployment.download", equalTo("forced")))
                .andExpect(jsonPath("$deployment.update", equalTo("forced")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==jvm)][0].name",
                        equalTo(ds.findFirstModuleByType(runtimeType).getName())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==jvm)][0].version",
                        equalTo(ds.findFirstModuleByType(runtimeType).getVersion())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].name",
                        equalTo(ds.findFirstModuleByType(osType).getName())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].version",
                        equalTo(ds.findFirstModuleByType(osType).getVersion())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0].size", equalTo(5 * 1024)))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0].filename", equalTo("test1")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0].hashes.md5",
                        equalTo(artifact.getMd5Hash())))
                .andExpect(
                        jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0].hashes.sha1",
                                equalTo(artifact.getSha1Hash())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0]._links.download.href",
                        equalTo(HTTPS_LOCALHOST + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0]._links.md5sum.href",
                        equalTo(HTTPS_LOCALHOST + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.MD5SUM")))

                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0]._links.download-http.href",
                        equalTo(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0]._links.md5sum-http.href",
                        equalTo(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.MD5SUM")))

                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1].size", equalTo(5 * 1024)))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1].filename",
                        equalTo("test1.signature")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1].hashes.md5",
                        equalTo(artifactSignature.getMd5Hash())))
                .andExpect(
                        jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1].hashes.sha1",
                                equalTo(artifactSignature.getSha1Hash())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1]._links.download.href",
                        equalTo(HTTPS_LOCALHOST + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.signature")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1]._links.md5sum.href",
                        equalTo(HTTPS_LOCALHOST + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.signature.MD5SUM")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1]._links.download-http.href",
                        equalTo(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.signature")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1]._links.md5sum-http.href",
                        equalTo(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.signature.MD5SUM")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==bApp)][0].version",
                        equalTo(ds.findFirstModuleByType(appType).getVersion())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==bApp)][0].name",
                        equalTo(ds.findFirstModuleByType(appType).getName())));
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);

        // Retrieved is reported
        final Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(new PageRequest(0, 100, Direction.DESC, "id"), uaction);
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Status.RETRIEVED);
    }

    @Test
    @Description("Checks that the deployementBase URL changes when the action is switched from soft to forced in TIMEFORCED case.")
    public void changeEtagIfActionSwitchesFromSoftToForced() throws Exception {
        // Prepare test data
        final Target target = targetManagement.createTarget(entityFactory.generateTarget("4712"));
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);

        final DistributionSetAssignmentResult result = deploymentManagement.assignDistributionSet(ds.getId(),
                ActionType.TIMEFORCED, System.currentTimeMillis() + 1_000, target.getControllerId());

        final Action action = deploymentManagement.findActiveActionsByTarget(result.getAssignedEntity().get(0)).get(0);

        MvcResult mvcResult = mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON)).andReturn();

        final String urlBeforeSwitch = JsonPath.compile("_links.deploymentBase.href")
                .read(mvcResult.getResponse().getContentAsString()).toString();

        // Time is not yet over, so we should see the same URL
        mvcResult = mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON)).andReturn();
        assertThat(JsonPath.compile("_links.deploymentBase.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).isEqualTo(urlBeforeSwitch)
                        .startsWith("http://localhost/" + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/deploymentBase/" + action.getId());

        // After the time is over we should see a new etag
        TimeUnit.MILLISECONDS.sleep(1_000);

        mvcResult = mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON)).andReturn();

        assertThat(JsonPath.compile("_links.deploymentBase.href").read(mvcResult.getResponse().getContentAsString())
                .toString()).isNotEqualTo(urlBeforeSwitch);
    }

    @Test
    @Description("Attempt/soft deployment to a controller. Checks if the resource reponse payload  for a given deployment is as expected.")
    public void deplomentAttemptAction() throws Exception {
        // Prepare test data
        final Target target = entityFactory.generateTarget("4712");
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);

        final byte random[] = RandomUtils.nextBytes(5 * 1024);
        final LocalArtifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "test1", false);
        final LocalArtifact artifactSignature = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "test1.signature", false);

        final Target savedTarget = targetManagement.createTarget(target);

        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).isEmpty();
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(0);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(0);

        List<Target> saved = deploymentManagement.assignDistributionSet(ds.getId(), ActionType.SOFT,
                RepositoryModelConstants.NO_FORCE_TIME, savedTarget.getControllerId()).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);
        saved = deploymentManagement.assignDistributionSet(ds2, saved).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(2);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(2);

        // Run test

        long current = System.currentTimeMillis();
        mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$_links.deploymentBase.href", startsWith("http://localhost/"
                        + tenantAware.getCurrentTenant() + "/controller/v1/4712/deploymentBase/" + uaction.getId())));
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        current = System.currentTimeMillis();

        final DistributionSet findDistributionSetByAction = distributionSetManagement
                .findDistributionSetByAction(action);

        mvc.perform(
                get("/{tenant}/controller/v1/4712/deploymentBase/" + uaction.getId(), tenantAware.getCurrentTenant())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$id", equalTo(String.valueOf(action.getId()))))
                .andExpect(jsonPath("$deployment.download", equalTo("attempt")))
                .andExpect(jsonPath("$deployment.update", equalTo("attempt")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==jvm)][0].name",
                        equalTo(ds.findFirstModuleByType(runtimeType).getName())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==jvm)][0].version",
                        equalTo(ds.findFirstModuleByType(runtimeType).getVersion())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].name",
                        equalTo(ds.findFirstModuleByType(osType).getName())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].version",
                        equalTo(ds.findFirstModuleByType(osType).getVersion())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0].size", equalTo(5 * 1024)))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0].filename", equalTo("test1")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0].hashes.md5",
                        equalTo(artifact.getMd5Hash())))
                .andExpect(
                        jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0].hashes.sha1",
                                equalTo(artifact.getSha1Hash())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0]._links.download.href",
                        equalTo(HTTPS_LOCALHOST + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0]._links.md5sum.href",
                        equalTo(HTTPS_LOCALHOST + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.MD5SUM")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1].size", equalTo(5 * 1024)))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1].filename",
                        equalTo("test1.signature")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1].hashes.md5",
                        equalTo(artifactSignature.getMd5Hash())))
                .andExpect(
                        jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1].hashes.sha1",
                                equalTo(artifactSignature.getSha1Hash())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1]._links.download.href",
                        equalTo(HTTPS_LOCALHOST + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.signature")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1]._links.md5sum.href",
                        equalTo(HTTPS_LOCALHOST + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.signature.MD5SUM")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1]._links.download-http.href",
                        equalTo(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.signature")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1]._links.md5sum-http.href",
                        equalTo(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.signature.MD5SUM")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==bApp)][0].version",
                        equalTo(ds.findFirstModuleByType(appType).getVersion())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==bApp)][0].name",
                        equalTo(ds.findFirstModuleByType(appType).getName())));
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);

        // Retrieved is reported
        final List<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(new PageRequest(0, 100, Direction.DESC, "id"), uaction).getContent();
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Status.RETRIEVED);
    }

    @Test
    @Description("Attempt/soft deployment to a controller including automated switch to hard. Checks if the resource reponse payload  for a given deployment is as expected.")
    public void deplomentAutoForceAction() throws Exception {
        // Prepare test data
        final Target target = entityFactory.generateTarget("4712");
        final DistributionSet ds = testdataFactory.createDistributionSet("", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);

        final byte random[] = RandomUtils.nextBytes(5 * 1024);
        final LocalArtifact artifact = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "test1", false);
        final LocalArtifact artifactSignature = artifactManagement.createLocalArtifact(new ByteArrayInputStream(random),
                ds.findFirstModuleByType(osType).getId(), "test1.signature", false);

        final Target savedTarget = targetManagement.createTarget(target);

        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).isEmpty();
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(0);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(0);

        List<Target> saved = deploymentManagement.assignDistributionSet(ds.getId(), ActionType.TIMEFORCED,
                System.currentTimeMillis(), savedTarget.getControllerId()).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);
        saved = deploymentManagement.assignDistributionSet(ds2, saved).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(2);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(savedTarget)).hasSize(2);

        // Run test

        long current = System.currentTimeMillis();
        mvc.perform(get("/{tenant}/controller/v1/4712", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$config.polling.sleep", equalTo("00:01:00")))
                .andExpect(jsonPath("$_links.deploymentBase.href", startsWith("http://localhost/"
                        + tenantAware.getCurrentTenant() + "/controller/v1/4712/deploymentBase/" + uaction.getId())));
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        current = System.currentTimeMillis();

        final DistributionSet findDistributionSetByAction = distributionSetManagement
                .findDistributionSetByAction(action);

        mvc.perform(get("/{tenant}/controller/v1/4712/deploymentBase/{actionId}", tenantAware.getCurrentTenant(),
                uaction.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$id", equalTo(String.valueOf(action.getId()))))
                .andExpect(jsonPath("$deployment.download", equalTo("forced")))
                .andExpect(jsonPath("$deployment.update", equalTo("forced")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==jvm)][0].name",
                        equalTo(ds.findFirstModuleByType(runtimeType).getName())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==jvm)][0].version",
                        equalTo(ds.findFirstModuleByType(runtimeType).getVersion())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].name",
                        equalTo(ds.findFirstModuleByType(osType).getName())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].version",
                        equalTo(ds.findFirstModuleByType(osType).getVersion())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0].size", equalTo(5 * 1024)))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0].filename", equalTo("test1")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0].hashes.md5",
                        equalTo(artifact.getMd5Hash())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0].hashes.sha1",
                        equalTo(artifact.getSha1Hash())))

                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0]._links.download.href",
                        equalTo(HTTPS_LOCALHOST + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0]._links.md5sum.href",
                        equalTo(HTTPS_LOCALHOST + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.MD5SUM")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0]._links.download-http.href",
                        equalTo(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[0]._links.md5sum-http.href",
                        equalTo(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.MD5SUM")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1].size", equalTo(5 * 1024)))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1].filename",
                        equalTo("test1.signature")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1].hashes.md5",
                        equalTo(artifactSignature.getMd5Hash())))
                .andExpect(
                        jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1].hashes.sha1",
                                equalTo(artifactSignature.getSha1Hash())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1]._links.download.href",
                        equalTo(HTTPS_LOCALHOST + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.signature")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1]._links.md5sum.href",
                        equalTo(HTTPS_LOCALHOST + tenantAware.getCurrentTenant()
                                + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.signature.MD5SUM")))

                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1]._links.download-http.href",
                        equalTo(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.signature")))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==os)][0].artifacts[1]._links.md5sum-http.href",
                        equalTo(HTTP_LOCALHOST + tenantAware.getCurrentTenant() + "/controller/v1/4712/softwaremodules/"
                                + findDistributionSetByAction.findFirstModuleByType(osType).getId()
                                + "/artifacts/test1.signature.MD5SUM")))

                .andExpect(jsonPath("$deployment.chunks[?(@.part==bApp)][0].version",
                        equalTo(ds.findFirstModuleByType(appType).getVersion())))
                .andExpect(jsonPath("$deployment.chunks[?(@.part==bApp)][0].name",
                        equalTo(ds.findFirstModuleByType(appType).getName())));
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);

        // Retrieved is reported
        final Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(new PageRequest(0, 100, Direction.DESC, "id"), uaction).getContent();
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(Status.RETRIEVED);
    }

    @Test
    @Description("Test various invalid access attempts to the deployment resource und the expected behaviour of the server.")
    public void badDeploymentAction() throws Exception {
        final Target target = targetManagement.createTarget(entityFactory.generateTarget("4712"));

        // not allowed methods
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/1", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(put("/{tenant}/controller/v1/4712/deploymentBase/1", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/v1/4712/deploymentBase/1", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        // non existing target
        mvc.perform(get("/{tenant}/controller/v1/4715/deploymentBase/1", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // no deployment
        mvc.perform(get("/controller/v1/4712/deploymentBase/1", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // wrong media type
        final List<Target> toAssign = new ArrayList<>();
        toAssign.add(target);
        final DistributionSet savedSet = testdataFactory.createDistributionSet("");

        final Action action1 = deploymentManagement.findActionWithDetails(
                deploymentManagement.assignDistributionSet(savedSet, toAssign).getActions().get(0));
        mvc.perform(
                get("/{tenant}/controller/v1/4712/deploymentBase/" + action1.getId(), tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        mvc.perform(
                get("/{tenant}/controller/v1/4712/deploymentBase/" + action1.getId(), tenantAware.getCurrentTenant())
                        .accept(MediaType.APPLICATION_ATOM_XML))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotAcceptable());
    }

    @Test
    @Description("The server protects itself against to many feedback upload attempts. The test verfies that "
            + "it is not possible to exceed the configured maximum number of feedback uplods.")
    public void toMuchDeplomentActionFeedback() throws Exception {
        final Target target = targetManagement.createTarget(entityFactory.generateTarget("4712"));
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final List<Target> toAssign = new ArrayList<>();
        toAssign.add(target);

        deploymentManagement.assignDistributionSet(ds.getId(), new String[] { "4712" });
        final Action action = deploymentManagement.findActionsByTarget(target).get(0);

        final String feedback = JsonBuilder.deploymentActionFeedback(action.getId().toString(), "proceeding");
        // assign distribution set creates an action status, so only 99 left
        for (int i = 0; i < 99; i++) {
            mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                    tenantAware.getCurrentTenant()).content(feedback).contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant()).content(feedback).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @Description("Multiple uploads of deployment status feedback to the server.")
    public void multipleDeplomentActionFeedback() throws Exception {
        final Target target1 = entityFactory.generateTarget("4712");
        final Target target2 = entityFactory.generateTarget("4713");
        final Target target3 = entityFactory.generateTarget("4714");
        final Target savedTarget1 = targetManagement.createTarget(target1);
        targetManagement.createTarget(target2);
        targetManagement.createTarget(target3);

        final DistributionSet ds1 = testdataFactory.createDistributionSet("1", true);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true);
        final DistributionSet ds3 = testdataFactory.createDistributionSet("3", true);

        final List<Target> toAssign = new ArrayList<>();
        toAssign.add(savedTarget1);

        final Action action1 = deploymentManagement.findActionWithDetails(
                deploymentManagement.assignDistributionSet(ds1.getId(), new String[] { "4712" }).getActions().get(0));
        final Action action2 = deploymentManagement.findActionWithDetails(
                deploymentManagement.assignDistributionSet(ds2.getId(), new String[] { "4712" }).getActions().get(0));
        final Action action3 = deploymentManagement.findActionWithDetails(
                deploymentManagement.assignDistributionSet(ds3.getId(), new String[] { "4712" }).getActions().get(0));

        Target myT = targetManagement.findTargetByControllerID("4712");
        assertThat(myT.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.findActiveActionsByTarget(myT)).hasSize(3);
        assertThat(myT.getAssignedDistributionSet()).isEqualTo(ds3);
        assertThat(myT.getTargetInfo().getInstalledDistributionSet()).isNull();
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.UNKNOWN))
                .hasSize(2);

        // action1 done

        long current = System.currentTimeMillis();
        long lastModified = targetManagement.findTargetByControllerID("4712").getLastModifiedAt();
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action1.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action1.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.findTargetByControllerIDWithDetails("4712");
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isGreaterThanOrEqualTo(current);
        // assertThat( myT.getLastModifiedAt() ).isEqualTo( lastModified );

        final long timeDiff = Math
                .abs(myT.getTargetInfo().getLastTargetQuery() - myT.getTargetInfo().getInstallationDate());

        assertThat(myT.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.findActiveActionsByTarget(myT)).hasSize(2);
        assertThat(myT.getTargetInfo().getInstalledDistributionSet().getId()).isEqualTo(ds1.getId());
        assertThat(myT.getAssignedDistributionSet()).isEqualTo(ds3);

        Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusAll(new PageRequest(0, 100, Direction.DESC, "id")).getContent();
        assertThat(actionStatusMessages).hasSize(4);
        assertThat(actionStatusMessages.iterator().next().getStatus()).isEqualTo(Status.FINISHED);

        // action2 done
        current = System.currentTimeMillis();
        lastModified = targetManagement.findTargetByControllerID("4712").getLastModifiedAt();
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action2.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action2.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.findTargetByControllerIDWithDetails("4712");
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isGreaterThanOrEqualTo(current);

        assertThat(myT.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.findActiveActionsByTarget(myT)).hasSize(1);
        assertThat(myT.getTargetInfo().getInstalledDistributionSet().getId()).isEqualTo(ds2.getId());
        assertThat(myT.getAssignedDistributionSet()).isEqualTo(ds3);
        actionStatusMessages = deploymentManagement.findActionStatusAll(new PageRequest(0, 100, Direction.DESC, "id"))
                .getContent();
        assertThat(actionStatusMessages).hasSize(5);
        assertThat(actionStatusMessages).haveAtLeast(1, new ActionStatusCondition(Status.FINISHED));

        // action3 done
        current = System.currentTimeMillis();
        lastModified = targetManagement.findTargetByControllerID("4712").getLastModifiedAt();
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action3.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action3.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.findTargetByControllerID("4712");
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isGreaterThanOrEqualTo(current);
        assertThat(myT.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.IN_SYNC);
        assertThat(deploymentManagement.findActiveActionsByTarget(myT)).hasSize(0);
        assertThat(myT.getTargetInfo().getInstalledDistributionSet()).isEqualTo(ds3);
        assertThat(myT.getAssignedDistributionSet()).isEqualTo(ds3);
        actionStatusMessages = deploymentManagement.findActionStatusAll(new PageRequest(0, 100, Direction.DESC, "id"))
                .getContent();
        assertThat(actionStatusMessages).hasSize(6);
        assertThat(actionStatusMessages).haveAtLeast(1, new ActionStatusCondition(Status.FINISHED));

    }

    @Test
    @Description("Verfies that an update action is correctly set to error if the controller provides error feedback.")
    public void rootRsSingleDeplomentActionWithErrorFeedback() throws Exception {
        final Target target = entityFactory.generateTarget("4712");
        DistributionSet ds = testdataFactory.createDistributionSet("");

        final Target savedTarget = targetManagement.createTarget(target);

        List<Target> toAssign = new ArrayList<>();
        toAssign.add(savedTarget);

        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.UNKNOWN);
        deploymentManagement.assignDistributionSet(ds, toAssign);
        final Action action = deploymentManagement.findActionsByDistributionSet(pageReq, ds).getContent().get(0);

        long current = System.currentTimeMillis();
        long lastModified = targetManagement.findTargetByControllerID("4712").getLastModifiedAt();
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action.getId().toString(), "closed", "failure",
                                "error message"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        Target myT = targetManagement.findTargetByControllerID("4712");
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.ERROR);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.PENDING))
                .hasSize(0);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.ERROR))
                .hasSize(1);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.IN_SYNC))
                .hasSize(0);
        assertThat(deploymentManagement.findActiveActionsByTarget(myT)).hasSize(0);
        assertThat(deploymentManagement.findActionsByTarget(myT)).hasSize(1);
        final Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusAll(new PageRequest(0, 100, Direction.DESC, "id")).getContent();
        assertThat(actionStatusMessages).hasSize(2);
        assertThat(actionStatusMessages).haveAtLeast(1, new ActionStatusCondition(Status.ERROR));

        // redo
        toAssign = new ArrayList<Target>();
        toAssign.add(targetManagement.findTargetByControllerID("4712"));
        ds = distributionSetManagement.findDistributionSetByIdWithDetails(ds.getId());
        deploymentManagement.assignDistributionSet(ds, toAssign);
        final Action action2 = deploymentManagement.findActiveActionsByTarget(myT).get(0);
        current = System.currentTimeMillis();
        lastModified = targetManagement.findTargetByControllerID("4712").getLastModifiedAt();

        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action2.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action2.getId().toString(), "closed", "success"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        myT = targetManagement.findTargetByControllerID("4712");
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isLessThanOrEqualTo(System.currentTimeMillis());
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isGreaterThanOrEqualTo(current);
        assertThat(myT.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.IN_SYNC);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.PENDING))
                .hasSize(0);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.ERROR))
                .hasSize(0);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.IN_SYNC))
                .hasSize(1);
        assertThat(deploymentManagement.findActiveActionsByTarget(myT)).hasSize(0);
        assertThat(deploymentManagement.findInActiveActionsByTarget(myT)).hasSize(2);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(4);
        assertThat(deploymentManagement.findActionStatusByAction(pageReq, action).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.ERROR));
        assertThat(deploymentManagement.findActionStatusByAction(pageReq, action2).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.FINISHED));

    }

    @Test
    @Description("Verfies that the controller can provided as much feedback entries as necessry as long as it is in the configured limites.")
    public void rootRsSingleDeplomentActionFeedback() throws Exception {
        final Target target = entityFactory.generateTarget("4712");
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final Target savedTarget = targetManagement.createTarget(target);

        final List<Target> toAssign = new ArrayList<>();
        toAssign.add(savedTarget);

        Target myT = targetManagement.findTargetByControllerID("4712");
        assertThat(myT.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.UNKNOWN);
        deploymentManagement.assignDistributionSet(ds, toAssign);
        final Action action = deploymentManagement.findActionsByDistributionSet(pageReq, ds).getContent().get(0);

        myT = targetManagement.findTargetByControllerID("4712");
        assertThat(myT.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(targetManagement.findTargetByInstalledDistributionSet(ds.getId(), pageReq)).hasSize(0);
        assertThat(targetManagement.findTargetByAssignedDistributionSet(ds.getId(), pageReq)).hasSize(1);

        // Now valid Feedback

        long current = System.currentTimeMillis();

        for (int i = 0; i < 4; i++) {
            mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                    tenantAware.getCurrentTenant())
                            .content(JsonBuilder.deploymentActionFeedback(action.getId().toString(), "proceeding"))
                            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                    .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        }

        myT = targetManagement.findTargetByControllerID("4712");
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isGreaterThanOrEqualTo(current);
        assertThat(myT.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.PENDING))
                .hasSize(1);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.ERROR))
                .hasSize(0);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.IN_SYNC))
                .hasSize(0);
        assertThat(deploymentManagement.findActiveActionsByTarget(myT)).hasSize(1);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(5);
        assertThat(deploymentManagement.findActionStatusAll(pageReq).getContent()).haveAtLeast(5,
                new ActionStatusCondition(Status.RUNNING));

        current = System.currentTimeMillis();
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action.getId().toString(), "scheduled"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.findTargetByControllerID("4712");
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isGreaterThanOrEqualTo(current);
        assertThat(myT.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.PENDING))
                .hasSize(1);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.ERROR))
                .hasSize(0);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.IN_SYNC))
                .hasSize(0);
        assertThat(deploymentManagement.findActiveActionsByTarget(myT)).hasSize(1);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(6);
        assertThat(deploymentManagement.findActionStatusAll(pageReq).getContent()).haveAtLeast(5,
                new ActionStatusCondition(Status.RUNNING));

        current = System.currentTimeMillis();
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action.getId().toString(), "resumed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.findTargetByControllerID("4712");
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isGreaterThanOrEqualTo(current);
        assertThat(myT.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.PENDING))
                .hasSize(1);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.ERROR))
                .hasSize(0);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.IN_SYNC))
                .hasSize(0);
        assertThat(deploymentManagement.findActiveActionsByTarget(myT)).hasSize(1);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(7);
        assertThat(deploymentManagement.findActionStatusAll(pageReq).getContent()).haveAtLeast(6,
                new ActionStatusCondition(Status.RUNNING));

        current = System.currentTimeMillis();
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action.getId().toString(), "canceled"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.findTargetByControllerID("4712");
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isGreaterThanOrEqualTo(current);
        assertThat(myT.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.findActiveActionsByTarget(myT)).hasSize(1);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.PENDING))
                .hasSize(1);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.ERROR))
                .hasSize(0);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.IN_SYNC))
                .hasSize(0);

        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(8);
        assertThat(deploymentManagement.findActionStatusAll(pageReq).getContent()).haveAtLeast(7,
                new ActionStatusCondition(Status.RUNNING));
        assertThat(deploymentManagement.findActionStatusAll(pageReq).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.CANCELED));

        current = System.currentTimeMillis();
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action.getId().toString(), "rejected"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.findTargetByControllerID("4712");
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isGreaterThanOrEqualTo(current);
        assertThat(myT.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
        assertThat(deploymentManagement.findActiveActionsByTarget(myT)).hasSize(1);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(9);
        assertThat(deploymentManagement.findActionStatusAll(pageReq).getContent()).haveAtLeast(6,
                new ActionStatusCondition(Status.RUNNING));
        assertThat(deploymentManagement.findActionStatusAll(pageReq).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.WARNING));
        assertThat(deploymentManagement.findActionStatusAll(pageReq).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.CANCELED));

        current = System.currentTimeMillis();
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/" + action.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionFeedback(action.getId().toString(), "closed"))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        myT = targetManagement.findTargetByControllerID("4712");
        assertThat(myT.getTargetInfo().getLastTargetQuery()).isGreaterThanOrEqualTo(current);
        assertThat(myT.getTargetInfo().getUpdateStatus()).isEqualTo(TargetUpdateStatus.IN_SYNC);
        assertThat(deploymentManagement.findActiveActionsByTarget(myT)).hasSize(0);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.ERROR))
                .hasSize(0);
        assertThat(targetManagement.findTargetByUpdateStatus(new PageRequest(0, 10), TargetUpdateStatus.IN_SYNC))
                .hasSize(1);

        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(10);
        assertThat(deploymentManagement.findActionStatusAll(pageReq).getContent()).haveAtLeast(7,
                new ActionStatusCondition(Status.RUNNING));
        assertThat(deploymentManagement.findActionStatusAll(pageReq).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.WARNING));
        assertThat(deploymentManagement.findActionStatusAll(pageReq).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.CANCELED));
        assertThat(deploymentManagement.findActionStatusAll(pageReq).getContent()).haveAtLeast(1,
                new ActionStatusCondition(Status.FINISHED));

        assertThat(targetManagement.findTargetByInstalledDistributionSet(ds.getId(), pageReq)).hasSize(1);
        assertThat(targetManagement.findTargetByAssignedDistributionSet(ds.getId(), pageReq)).hasSize(1);
    }

    @Test
    @Description("Various forbidden request appempts on the feedback resource. Ensures correct answering behaviour as expected to these kind of errors.")
    public void badDeplomentActionFeedback() throws Exception {
        final Target target = entityFactory.generateTarget("4712");
        final Target target2 = entityFactory.generateTarget("4713");
        final DistributionSet savedSet = testdataFactory.createDistributionSet("");
        final DistributionSet savedSet2 = testdataFactory.createDistributionSet("1");

        // target does not exist
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/1234/feedback", tenantAware.getCurrentTenant())
                .content(JsonBuilder.deploymentActionInProgressFeedback("1234")).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        Target savedTarget = targetManagement.createTarget(target);
        final Target savedTarget2 = targetManagement.createTarget(target2);

        // Action does not exists
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/1234/feedback", tenantAware.getCurrentTenant())
                .content(JsonBuilder.deploymentActionInProgressFeedback("1234")).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        final List<Target> toAssign = new ArrayList<Target>();
        toAssign.add(savedTarget);
        final List<Target> toAssign2 = new ArrayList<Target>();
        toAssign2.add(savedTarget2);

        assertThat(targetManagement.findTargetByControllerID("4712").getTargetInfo().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.UNKNOWN);
        savedTarget = deploymentManagement.assignDistributionSet(savedSet, toAssign).getAssignedEntity().iterator()
                .next();
        deploymentManagement.assignDistributionSet(savedSet2, toAssign2);

        // wrong format
        mvc.perform(post("/{tenant}/controller/v1/4712/deploymentBase/AAAA/feedback", tenantAware.getCurrentTenant())
                .content(JsonBuilder.deploymentActionInProgressFeedback("AAAA")).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final Action updateAction = deploymentManagement.findActiveActionsByTarget(savedTarget).get(0);

        // action exists but is not assigned to this target
        mvc.perform(post("/{tenant}/controller/v1/4713/deploymentBase/" + updateAction.getId() + "/feedback",
                tenantAware.getCurrentTenant())
                        .content(JsonBuilder.deploymentActionInProgressFeedback(updateAction.getId().toString()))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // not allowed methods
        mvc.perform(get("/{tenant}/controller/v1/4712/deploymentBase/2/feedback", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(put("/{tenant}/controller/v1/4712/deploymentBase/2/feedback", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete("/{tenant}/controller/v1/4712/deploymentBase/2/feedback", tenantAware.getCurrentTenant()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

    }

    private class ActionStatusCondition extends Condition<ActionStatus> {
        private final Status status;

        /**
         * @param status
         */
        public ActionStatusCondition(final Status status) {
            this.status = status;
        }

        @Override
        public boolean matches(final ActionStatus actionStatus) {
            return actionStatus.getStatus() == status;
        }
    }
}
