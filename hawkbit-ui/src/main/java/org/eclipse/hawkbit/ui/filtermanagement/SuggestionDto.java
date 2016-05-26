/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.io.Serializable;

public class SuggestionDto implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2897970088477628900L;
	private int itemId;
	private String displayString;

	public SuggestionDto(int itemId, String displayString) {
		this.itemId = itemId;
		this.displayString = displayString;
	}

	public int getItemId() {
		return itemId;
	}

	public void setItemId(int itemId) {
		this.itemId = itemId;
	}

	public String getDisplayString() {
		return displayString;
	}

	public void setDisplayString(String displayString) {
		this.displayString = displayString;
	}


}
