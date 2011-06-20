package inat.network;
import inat.analyser.LevelResult;
import inat.analyser.SMCResult;
import inat.model.Model;

import java.rmi.Remote;

public interface iUPPAALServer extends Remote {
	
	public LevelResult analyze(Model m, int timeTo, int nSimulationRuns) throws Exception;
	
	public SMCResult analyze(Model m, String smcQuery) throws Exception;
}
