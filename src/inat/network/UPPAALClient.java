package inat.network;

import inat.analyser.LevelResult;
import inat.analyser.SMCResult;
import inat.model.Model;

import java.rmi.Naming;

public class UPPAALClient {
	private iUPPAALServer server = null;
	
	public UPPAALClient(String serverHost, Integer serverPort) throws Exception {
		System.setSecurityManager(new java.rmi.RMISecurityManager());
		server = (iUPPAALServer) Naming.lookup("rmi://" + serverHost + ":" + serverPort + "/UPPAALServer");
	}
	
	public LevelResult analyze(Model m, int timeTo, int nSimulationRuns, boolean computeStdDev) throws Exception {
		return server.analyze(m, timeTo, nSimulationRuns, computeStdDev);
	}
	
	public SMCResult analyzeSMC(Model m, String smcQuery) throws Exception {
		return server.analyze(m, smcQuery);
	}
}
