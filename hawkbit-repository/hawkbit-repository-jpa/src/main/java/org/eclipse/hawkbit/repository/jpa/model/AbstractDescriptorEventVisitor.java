/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

/**
 * 
 * Interface defining the action to be performed after entity is
 * created/updated.
 *
 */
public interface AbstractDescriptorEventVisitor {
	void publishEventPostAction(DescriptorEventDetails eventDetails);
}