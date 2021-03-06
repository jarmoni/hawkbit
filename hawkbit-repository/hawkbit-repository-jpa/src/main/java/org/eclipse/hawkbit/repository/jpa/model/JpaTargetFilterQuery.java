/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

/**
 * Stored target filter.
 *
 */
@Entity
@Table(name = "sp_target_filter_query", indexes = {
        @Index(name = "sp_idx_target_filter_query_01", columnList = "tenant,name") }, uniqueConstraints = @UniqueConstraint(columnNames = {
                "name", "tenant" }, name = "uk_tenant_custom_filter_name"))
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaTargetFilterQuery extends AbstractJpaTenantAwareBaseEntity implements TargetFilterQuery {
    private static final long serialVersionUID = 7493966984413479089L;

    @Column(name = "name", length = 64)
    private String name;

    @Column(name = "query", length = 1024)
    private String query;

    public JpaTargetFilterQuery() {
        // Default constructor for JPA.
    }

    /**
     * Public constructor.
     * 
     * @param name
     *            of the {@link TargetFilterQuery}.
     * @param query
     *            of the {@link TargetFilterQuery}.
     */
    public JpaTargetFilterQuery(final String name, final String query) {
        this.name = name;
        this.query = query;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getQuery() {
        return query;
    }

    @Override
    public void setQuery(final String query) {
        this.query = query;
    }
}
