/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.repository;

import org.eclipse.hawkbit.EnableJpaRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-Configuration for enabling the REST-Resources.
 *
 */
@Configuration
@ConditionalOnClass({ EnableJpaRepository.class })
@Import({ EnableJpaRepository.class })
public class JpaRepositoryAutoConfiguration {

}
