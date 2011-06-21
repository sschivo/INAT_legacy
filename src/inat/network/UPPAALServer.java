package inat.network;

import inat.InatBackend;
import inat.analyser.LevelResult;
import inat.analyser.SMCResult;
import inat.analyser.uppaal.ResultAverager;
import inat.analyser.uppaal.UppaalModelAnalyserFaster;
import inat.model.Model;

import java.io.File;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UPPAALServer extends UnicastRemoteObject implements iUPPAALServer {
	private static final long serialVersionUID = 5030971508567718530L;
	private static final DateFormat df = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");

	protected UPPAALServer() throws RemoteException {
		super();
		int portRMI = 1234;
		try {
			LocateRegistry.createRegistry(portRMI);
			Naming.bind("rmi://localhost:" + portRMI + "/UPPAALServer", this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public LevelResult analyze(Model m, int timeTo, int nSimulationRuns, boolean computeStdDev) throws Exception {
		System.out.println(df.format(new Date(System.currentTimeMillis())) + " Analysing \"normal\" model with simulation up to " + timeTo);
		LevelResult result;
		if (nSimulationRuns > 1) {
			result = new ResultAverager(null).analyzeAverage(m, timeTo, nSimulationRuns, computeStdDev);
		} else {
			result = new UppaalModelAnalyserFaster(null).analyze(m, timeTo);
		}
		System.out.println(df.format(new Date(System.currentTimeMillis())) + " Done.");
		System.out.println();
		return result;
	}

	@Override
	public SMCResult analyze(Model m, String smcQuery) throws Exception {
		System.out.println(df.format(new Date(System.currentTimeMillis())) + " Analysing \"SMC\" model with query " + smcQuery);
		SMCResult result = new UppaalModelAnalyserFaster(null).analyzeSMC(m, smcQuery);
		System.out.println(df.format(new Date(System.currentTimeMillis())) + " Done.");
		System.out.println();
		return result;
	}
	
	public static void main(String[] args) {
		try {
			InatBackend.initialise(new File("inat-configuration.xml"));

			@SuppressWarnings("unused")
			UPPAALServer server = new UPPAALServer();
			System.out.println(df.format(new Date(System.currentTimeMillis())) + " Server started.");
		} catch (Exception ex) {
			System.err.println("Problems in starting server!");
			ex.printStackTrace();
		}
	}

}
