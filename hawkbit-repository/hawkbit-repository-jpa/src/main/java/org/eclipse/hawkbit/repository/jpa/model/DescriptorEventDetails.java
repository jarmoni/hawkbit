package org.eclipse.hawkbit.repository.jpa.model;

import org.eclipse.persistence.descriptors.DescriptorEvent;

public class DescriptorEventDetails {

	enum ActionType{
		CREATE , UPDATE;
	}
	
	private DescriptorEvent descriptorEvent;
	
	private ActionType actiontype;

	public DescriptorEventDetails(ActionType actionType, DescriptorEvent descriptorEvent) {
		this.descriptorEvent = descriptorEvent;
		this.actiontype = actionType;
	}
	
	public DescriptorEvent getDescriptorEvent() {
		return descriptorEvent;
	}
	
	public ActionType getActiontype() {
		return actiontype;
	}
	
	
}
