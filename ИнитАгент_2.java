package ru.dvo.iacp.is.iacpaas.mas.agents;

import ru.dvo.iacp.is.iacpaas.common.exceptions.PlatformException;
import ru.dvo.iacp.is.iacpaas.mas.IRunningAuthority;
import ru.dvo.iacp.is.iacpaas.mas.messages.TaskMessage;
import ru.dvo.iacp.is.iacpaas.mas.messages.system.FinalizeMessage;
import ru.dvo.iacp.is.iacpaas.storage.IInforesource;

public final class MyInitAgentImpl extends MyInitAgent {

	public MyInitAgentImpl(IRunningAuthority runningAuthority, IInforesource agentInforesource) {
		super(runningAuthority, agentInforesource);
	}

	public void runProduction(ru.dvo.iacp.is.iacpaas.mas.messages.system.InitMessage msg, InitMessageResultCreator rc) throws PlatformException {
		
		TaskMessage tm = rc.taskMessage.create("ќнтологии и свойства / область медицины / MyAgent");
		
	}

	public void runProduction(ru.dvo.iacp.is.iacpaas.mas.messages.StatusMessage msg, StatusMessageResultCreator rc) throws PlatformException {

		FinalizeMessage finalizeMessage = rc.finalizeMessage.create();

	
	}

}
