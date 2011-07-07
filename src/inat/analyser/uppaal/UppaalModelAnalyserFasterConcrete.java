package inat.analyser.uppaal;


import inat.InatBackend;
import inat.analyser.AnalysisException;
import inat.analyser.LevelResult;
import inat.analyser.ModelAnalyser;
import inat.analyser.SMCResult;
import inat.cytoscape.RunAction;
import inat.model.Model;
import inat.model.Property;
import inat.model.Reactant;
import inat.util.XmlConfiguration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import cytoscape.Cytoscape;
import cytoscape.task.TaskMonitor;


public class UppaalModelAnalyserFasterConcrete implements ModelAnalyser<LevelResult> {
	
	public static double TIME_SCALE = 0.2; //the factor by which time values are mutiplied before being output on the .csv file (it answers the question "how many real-life minutes does a time unit of the model represent?")
	
	/**
	 * The configuration key for the verifyta path property.
	 */
	private static final String VERIFY_KEY = "/Inat/UppaalInvoker/verifyta";
	
	private static final String VERIFY_SMC_KEY = "/Inat/UppaalInvoker/verifytaSMC";
	
	private String verifytaPath, verifytaSMCPath;//, tracerPath;
	private TaskMonitor monitor;
	private RunAction runAction;
	private int taskStatus = 0; //1 = process completed, 2 = user pressed Cancel
	
	public UppaalModelAnalyserFasterConcrete(TaskMonitor monitor, RunAction runAction) {
		XmlConfiguration configuration = InatBackend.get().configuration();

		this.monitor = monitor;
		this.runAction = runAction;
		this.verifytaPath = configuration.get(VERIFY_KEY);
		this.verifytaSMCPath = configuration.get(VERIFY_SMC_KEY);
	}
	
	public static boolean areWeUnderWindows() {
		if (System.getProperty("os.name").startsWith("Windows")) return true;
		return false;
	}
	
	public SMCResult analyzeSMC(Model m, String probabilisticQuery) throws AnalysisException {
		SMCResult result = null;
		try {
			final String uppaalModel = new VariablesModelSMC().transform(m);
			
			File modelFile = File.createTempFile("inat", ".xml");
			final String prefix = modelFile.getAbsolutePath().replace(".xml", "");
			File queryFile = new File(prefix + ".q");

			// write out strings to file
			FileWriter modelFileOut = new FileWriter(modelFile);
			modelFileOut.append(uppaalModel);
			modelFileOut.close();
			modelFile.deleteOnExit();
			
			FileWriter queryFileOut = new FileWriter(queryFile);
			queryFileOut.append(probabilisticQuery);
			queryFileOut.close();
			queryFile.deleteOnExit();
	
			String nomeFileModello = modelFile.getAbsolutePath(),
				   nomeFileQuery = queryFile.getAbsolutePath(),
				   nomeFileOutput = nomeFileModello.substring(0, nomeFileModello.indexOf(".")) + ".output";
						
			//the following string is used in order to make sure that the name of .xtr output files is unique even when we are called by an application which is multi-threaded itself (it supposes of course that the input file is unique =))
			String[] cmd = new String[3];
			
			if (areWeUnderWindows()) {
				if (!new File(verifytaSMCPath).exists()) {
					throw new FileNotFoundException("Cannot locate verifyta executable! (tried in " + verifytaSMCPath + ")");
				}
				cmd[0] = "cmd";
				cmd[1] = "/c";
				cmd[2] = " \"" + verifytaSMCPath + "\"";
			} else {
				if (!new File(verifytaSMCPath).exists()) {
					throw new FileNotFoundException("Cannot locate verifyta executable! (tried in " + verifytaSMCPath + ")");
				}
				cmd[0] = "bash";
				cmd[1] = "-c";
				cmd[2] = verifytaSMCPath;				
			}
			cmd[2] += " \"" + nomeFileModello + "\" \"" + nomeFileQuery + "\" > \"" + nomeFileOutput + "\"";
			Runtime rt = Runtime.getRuntime();
			final Process proc = rt.exec(cmd);
			if (runAction != null) {
				taskStatus = 0;
				new Thread() { //wait for the process to end correctly
					@Override
					public void run() {
						try {
							proc.waitFor();
						} catch (InterruptedException ex) {
							taskStatus = 2;
						}
						taskStatus = 1;
					}
				}.start();
				new Thread() { //wait for the process to end by user cancellation
					@Override
					public void run() {
						while (taskStatus == 0) {
							if (runAction.needToStop()) {
								taskStatus = 2;
								return;
							}
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								
							}
						}
					}
				}.start();
				while (taskStatus == 0) {
					Thread.sleep(100);
				}
				if (taskStatus == 2) { //the process has been cancelled: we need to exit
					throw new AnalysisException("User interrupted");
				}
			} else {
				try {
					proc.waitFor();
				} catch (InterruptedException ex){
					proc.destroy();
					throw new Exception("Interrupted (1)");
				}
			}
			if (proc.exitValue() != 0) {
				StringBuilder errorBuilder = new StringBuilder();
				errorBuilder.append("[" + nomeFileModello + "] Verify result: " + proc.exitValue() + "\n");
				BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				String line = null;
				while ((line = br.readLine()) != null) {
					errorBuilder.append(line + "\n");
				}
				errorBuilder.append(" (current directory: " + new File(".").getAbsolutePath() + ")\n");
				throw new Exception(errorBuilder.toString());
			}
			//N B: it is responsibility of the caller to close all streams when the process is done!!!
			proc.getErrorStream().close();
			proc.getInputStream().close();
			proc.getOutputStream().close();
			
			result = new UppaalModelAnalyserFasterConcrete.VariablesInterpreterConcrete(monitor).analyseSMC(m, new FileInputStream(nomeFileOutput));
			
			new File(nomeFileOutput).delete();
			
		} catch (Exception e) {
			throw new AnalysisException("Error during analysis", e);
		}
		
		if (result == null) {
			throw new AnalysisException("Error during analysis: empty result");
		}
		
		return result;
	}
	
	public LevelResult analyze(final Model m, final int timeTo) throws AnalysisException {
		LevelResult result = null;
		try {
			final String uppaalModel = new VariablesModelSMC().transform(m);
			final String uppaalQuery = "E<> (globalTime > " + timeTo + ")";
			
			File modelFile = File.createTempFile("inat", ".xml");
			final String prefix = modelFile.getAbsolutePath().replace(".xml", "");
			File queryFile = new File(prefix + ".q");
	
			// write out strings to file
			FileWriter modelFileOut = new FileWriter(modelFile);
			modelFileOut.append(uppaalModel);
			modelFileOut.close();
			modelFile.deleteOnExit();
			
			FileWriter queryFileOut = new FileWriter(queryFile);
			queryFileOut.append(uppaalQuery);
			queryFileOut.close();
			queryFile.deleteOnExit();
	
			String nomeFileModello = modelFile.getAbsolutePath(),
				   nomeFileQuery = queryFile.getAbsolutePath();
			
			
			String[] cmd = new String[3];
			
			if (areWeUnderWindows()) {
				if (!new File(verifytaPath).exists()) {
					throw new FileNotFoundException("Cannot locate verifyta executable! (tried in " + verifytaPath + ")");
				}
				cmd[0] = "cmd";
				cmd[1] = "/c";
				cmd[2] = " \"" + verifytaPath + "\"";
			} else {
				if (!new File(verifytaPath).exists()) {
					throw new FileNotFoundException("Cannot locate verifyta executable! (tried in " + verifytaPath + ")");
				}
				cmd[0] = "bash";
				cmd[1] = "-c";
				cmd[2] = verifytaPath;				
			}
			cmd[2] += " -t0 -o2 \"" + nomeFileModello + "\" \"" + nomeFileQuery + "\"";
			Runtime rt = Runtime.getRuntime();
			if (monitor != null) {
				monitor.setStatus("Analyzing model with UPPAAL.");
			}
			final Process proc = rt.exec(cmd);
			final Vector<LevelResult> resultVector = new Vector<LevelResult>(1); //this has no other reason than to hack around the fact that an internal class needs to have all variables it uses declared as final
			final Vector<Exception> errors = new Vector<Exception>(); //same reason as above
			new Thread() {
				@Override
				public void run() {
					try {
						resultVector.add(new UppaalModelAnalyserFasterConcrete.VariablesInterpreterConcrete(monitor).analyse(m, proc.getErrorStream(), timeTo));
					} catch (Exception e) {
						errors.add(e);
					}
				}
			}.start();
			if (runAction != null) {
				taskStatus = 0;
				new Thread() { //wait for the process to end correctly
					@Override
					public void run() {
						try {
							proc.waitFor();
						} catch (InterruptedException ex) {
							taskStatus = 2;
						}
						taskStatus = 1;
					}
				}.start();
				new Thread() { //wait for the process to end by user cancellation
					@Override
					public void run() {
						while (taskStatus == 0) {
							if (runAction.needToStop()) {
								taskStatus = 2;
								return;
							}
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								
							}
						}
					}
				}.start();
				while (taskStatus == 0) {
					Thread.sleep(100);
				}
				if (taskStatus == 2) {
					throw new AnalysisException("User interrupted");
				}
				while (resultVector.isEmpty()) { //if the verifyta process is completed, we may still need to wait for the analysis thread to complete
					Thread.sleep(100);
				}
			} else {
				try {
					proc.waitFor();
				} catch (InterruptedException ex){
					proc.destroy();
					throw new Exception("Interrupted (1)");
				}
			}
			if (!errors.isEmpty()) {
				Exception ex = errors.firstElement();
				throw new AnalysisException("Error during analysis", ex);
			}
			result = resultVector.firstElement();
			if (proc.exitValue() != 0) {
				StringBuilder errorBuilder = new StringBuilder();
				errorBuilder.append("[" + nomeFileModello + "] Verify result: " + proc.exitValue() + "\n");
				BufferedReader br = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
				String line = null;
				while ((line = br.readLine()) != null) {
					errorBuilder.append(line + "\n");
				}
				errorBuilder.append(" (current directory: " + new File(".").getAbsolutePath() + ")\n");
				throw new Exception(errorBuilder.toString());
			}
			//N B: it is responsibility of the caller to close all streams when the process is done!!!
			proc.getErrorStream().close();
			proc.getInputStream().close();
			proc.getOutputStream().close();
			
		} catch (Exception e) {
			throw new AnalysisException("Error during analysis", e);
		}
		
		if (result == null || result.isEmpty()) {
			throw new AnalysisException("Error during analysis: empty result");
		}
		
		return result;
	}
	
	
	//This is slightly different from the "official" one in the sense that it reads data directly from the input stream. This way, we don't have to read the whole stream to a string (with the consequent waste of memory) before giving an input to the interpreter
	public class VariablesInterpreterConcrete {
		
		private TaskMonitor monitor = null;
		
		public VariablesInterpreterConcrete(TaskMonitor monitor) {
			this.monitor = monitor;
		}
		

		public SMCResult analyseSMC(Model m, InputStream smcOutput) throws Exception {
			BufferedReader br = new BufferedReader(new InputStreamReader(smcOutput));
			String line = null;
			String objective = "-- Property is";
			
			try {
				while ((line = br.readLine()) != null) {
					if (!line.contains(objective)) {
						continue;
					}
					if (line.contains("NOT")) {
						return new SMCResult(false, findConfidence(line, br));
					} else {
						line = br.readLine();
						if ((line.indexOf("runs) H") != -1) || (line.indexOf("runs) Pr(..)/Pr(..)") != -1)) { //it has boolean result
							return new SMCResult(true, findConfidence(line, br));
						} else { //the result is between lower and upper bound
							String lowerBoundS = line.substring(line.indexOf("[") + 1, line.lastIndexOf(",")),
								   upperBoundS = line.substring(line.lastIndexOf(",") + 1, line.lastIndexOf("]"));
							double lowerBound, upperBound;
							try {
								lowerBound = Double.parseDouble(lowerBoundS);
								upperBound = Double.parseDouble(upperBoundS);
							} catch (Exception ex) {
								throw new Exception("Unable to understand probability bounds for the result \"" + line + "\"");
							}
							return new SMCResult(lowerBound, upperBound, findConfidence(line, br));
						}
					}
				}
			} catch (Exception ex) {
				throw new Exception("Unable to understand UPPAAL SMC output: " + readTheRest(line, br), ex);
			}

			throw new Exception("Unable to understand UPPAAL SMC output: " + readTheRest(line, br));
		}
		
		private String readTheRest(String line, BufferedReader br) throws Exception {
			StringBuilder content = new StringBuilder();
			String endLine = System.getProperty("line.separator");
			content.append(line + endLine);
			while ((line = br.readLine()) != null) {
				content.append(line + endLine);
			}
			return content.toString();
		}
		
		private double findConfidence(String currentLine, BufferedReader br) throws Exception {
			double confidence = 0;
			boolean weHaveAProblem = false;
			String objective = "with confidence ";
			String line = currentLine;
			StringBuilder savedOutput = new StringBuilder();
			String endLine = System.getProperty("line.separator");
			savedOutput.append(currentLine + endLine);
			if (!line.contains(objective)) {
				while ((line = br.readLine()) != null) {
					savedOutput.append(line + endLine);
					if (line.contains(objective)) {
						break;
					} else if (line.startsWith("State")) {
						weHaveAProblem = true;
					}
				}
			}
			
			if (weHaveAProblem) {
				String errorMsg = savedOutput.toString();
				if (errorMsg.length() > 200) {
					errorMsg = errorMsg.substring(0, 100) + endLine + " [...] " + endLine + errorMsg.substring(errorMsg.length() - 100);
				}
				JOptionPane.showMessageDialog(Cytoscape.getDesktop(), errorMsg, "UPPAAL SMC Exception", JOptionPane.ERROR_MESSAGE);
			}
			if (line == null) {
				throw new Exception("Unable to understand UPPAAL SMC output: " + savedOutput);
			}
			if (line.endsWith(".")) {
				line = line.substring(0, line.length() - 1);
			}
			try {
				confidence = Double.parseDouble(line.substring(line.indexOf(objective) + objective.length()));
			} catch (Exception ex) {
				confidence = -1;
			}
			
			return confidence;
		}
		
		
		public LevelResult analyse(Model m, InputStream output, int timeTo) throws Exception {
			Map<String, SortedMap<Double, Double>> levels = new HashMap<String, SortedMap<Double, Double>>();

			BufferedReader br = new BufferedReader(new InputStreamReader(output));
			String line = null;
			Pattern globalTimePattern = Pattern.compile("globalTime[=][0-9]+");
			Pattern statePattern = Pattern.compile("[A-Za-z0-9_]+[' ']*[=][' ']*[0-9]+");
			int time = 0;
			int maxNumberOfLevels = m.getProperties().get("levels").as(Integer.class);
			HashMap<String, Double> numberOfLevels = new HashMap<String, Double>();

			while ((line = br.readLine()) != null) {
				if (!line.startsWith("State"))
					continue;
				line = br.readLine(); //the "State:" string has a \n at the end, so we need to read the next line
				line = br.readLine(); //the second line contains informations about which we don't care. We want variable values
				Matcher stateMatcher = statePattern.matcher(line);
				String s = null;
				while (stateMatcher.find()) {
					s = stateMatcher.group();
					if (s.contains("_nonofficial") || s.contains("counter") || s.contains("metro"))
						continue;
					String reactantId = null;
					if (s.indexOf(' ') >= 0 && s.indexOf(' ') < s.indexOf('=')) {
						reactantId = s.substring(0, s.indexOf(' '));
					} else {
						reactantId = s.substring(0, s.indexOf('='));
					}
					if (reactantId.equals("c") || reactantId.equals("globalTime") || reactantId.equals("r") || reactantId.equals("r1") || reactantId.equals("r2")) continue; //private variables are not taken into account
					// put the reactant into the result map
					levels.put(reactantId, new TreeMap<Double, Double>());
				}
				break;
			}

			// add initial concentrations and get number of levels
			for (Reactant r : m.getReactants()) {
				Integer nLvl = r.get("levels").as(Integer.class);
				if (nLvl == null) {
					Property nameO = r.get("alias");
					String name;
					if (nameO == null) {
						name = r.getId();
					} else {
						name = nameO.as(String.class);
					}
					String inputLevels = JOptionPane.showInputDialog("Missing number of levels for reactant \"" + name + "\" (" + r.getId() + ").\nPlease insert the max number of levels for \"" + name + "\"", maxNumberOfLevels);
					if (inputLevels != null) {
						try {
							nLvl = new Integer(inputLevels);
						} catch (Exception ex) {
							nLvl = maxNumberOfLevels;
						}
					} else {
						nLvl = maxNumberOfLevels;
					}
				}
				numberOfLevels.put(r.getId(), (double)nLvl);
				
				if (levels.containsKey(r.getId())) {
					double initialLevel = r.get("initialConcentration").as(Integer.class);
					initialLevel = initialLevel / (double)nLvl * (double)maxNumberOfLevels; //of course, the initial "concentration" itself needs to be rescaled correctly
					levels.get(r.getId()).put(0.0, initialLevel);
				}
			}
			
			if (monitor != null) {
				monitor.setStatus("Analysing UPPAAL output trace.");
			}

			String oldLine = null;
			while ((line = br.readLine()) != null) {
				/*while (line != null && !line.contains("inform_reacting")) {
					line = br.readLine();
				}
				while ((line = br.readLine()) != null) {
					if (line.startsWith("State")) break;
				}
				if (line == null) break;*/
				if (!line.startsWith("State")) continue;
				br.readLine(); //as said before, the "State:" string ends with \n, so we need to read the next line in order to get the actual state data
				line = br.readLine(); //and the line after that contains only the states of the processes, while we are interested in variable values, which are in the 3rd line
				Matcher timeMatcher = globalTimePattern.matcher(line);
				if (oldLine == null)
					oldLine = line;
				if (timeMatcher.find()) {
					String value = (timeMatcher.group().split("=")[1]);
					int newTime = -1;
					if (value.substring(0, 1).equals("=")) {
						if (value.substring(1, 2).equals("-")) {
							newTime = Integer.parseInt(value.substring(2, value.length()));
						} else {
							newTime = Integer.parseInt(value.substring(1, value.length()));
						}
					} else {
						if (value.substring(0, 1).equals("-")) {
							newTime = Integer.parseInt(value.substring(1, value.length())) + 1;
						} else {
							newTime = Integer.parseInt(value.substring(0, value.length())) + 1;
						}
					}
					
					if (time < newTime) {
						time = newTime;
						// we now know the time
						Matcher stateMatcher = statePattern.matcher(oldLine);
						String s = null;
						while (stateMatcher.find()) {
							s = stateMatcher.group();
							if (s.contains("_nonofficial") || s.contains("counter") || s.contains("metro"))
								continue;
							String reactantId = null;
							if (s.indexOf(' ') >= 0 && s.indexOf(' ') < s.indexOf('=')) {
								reactantId = s.substring(0, s.indexOf(' '));
							} else {
								reactantId = s.substring(0, s.indexOf('='));
							}
							if (reactantId.equals("c") || reactantId.equals("globalTime") || reactantId.equals("r") || reactantId.equals("r1") || reactantId.equals("r2") || numberOfLevels.get(reactantId) == null) continue; //we check whether it is a private variable
							// we can determine the level of activation
							int level = Integer.valueOf(s.substring(s.indexOf("=") + 1).trim());
							if (numberOfLevels.get(reactantId) != maxNumberOfLevels) {
								level = (int)(level / (double)numberOfLevels.get(reactantId) * (double)maxNumberOfLevels);
							}
							
							SortedMap<Double, Double> rMap = levels.get(reactantId);
							if (rMap.get(rMap.lastKey()) != level) {
								if (rMap.lastKey() < time - 1) { //We use this piece to explicitly keep a level constant when it is not varying (i.e., the graph will never contain non-vertical,non-horizontal lines)
									rMap.put((double)(time - 1), rMap.get(rMap.lastKey()));
								}
								
								rMap.put((double)time, (double)level);
							}
						}
						oldLine = line;
					} else if (newTime == time) {
						oldLine = line;
					}
				} else {
					throw new AnalysisException("New state without globalTime. Offending line: \"" + line + "\"");
				}
			}
			if (time < timeTo) { //if the state of the system remains unchanged from a certain time on (and so UPPAAL terminates on that point), but we asked for a later time, we add a final point where all data remain unchanged, so that the user can see the "evolution" up to the requested point
				for (String reactantName : levels.keySet()) {
					SortedMap<Double, Double> values = levels.get(reactantName);
					double lastValue = values.get(values.lastKey());
					values.put((double)timeTo, lastValue);
				}
			}
			
			return new SimpleLevelResult(levels);
		}
	}
}
