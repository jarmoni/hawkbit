/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.eventbus.event;

import org.eclipse.hawkbit.repository.model.DistributionSet;

/**
 *
 *
 *
 */
public class DistributionDeletedEvent extends AbstractBaseEntityEvent<DistributionSet> {

    private static final long serialVersionUID = 1L;

    /**
     * @param tenant
     *            the tenant for this event
     * @param distributionSetId
     *            the ID of the target which has been deleted
     */
    public DistributionDeletedEvent(final DistributionSet distributionSet) {
        super(distributionSet);
    }

}
