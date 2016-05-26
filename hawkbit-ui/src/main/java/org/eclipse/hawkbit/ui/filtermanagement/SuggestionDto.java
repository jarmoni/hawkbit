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
