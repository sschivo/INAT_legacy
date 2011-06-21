package inat.analyser.uppaal;


import inat.InatBackend;
import inat.analyser.AnalysisException;
import inat.analyser.LevelResult;
import inat.analyser.ModelAnalyser;
import inat.analyser.SMCResult;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import cytoscape.Cytoscape;
import cytoscape.task.TaskMonitor;


public class UppaalModelAnalyserFaster implements ModelAnalyser<LevelResult> {
	
	public static double TIME_SCALE = 0.2; //the factor by which time values are mutiplied before being output on the .csv file (it answers the question "how many real-life minutes does a time unit of the model represent?")
	
	/**
	 * The configuration key for the tracer path property.
	 */
	private static final String TRACER_KEY = "/Inat/UppaalInvoker/tracer";

	/**
	 * The configuration key for the verifyta path property.
	 */
	private static final String VERIFY_KEY = "/Inat/UppaalInvoker/verifyta";
	
	private static final String VERIFY_SMC_KEY = "/Inat/UppaalInvoker/verifytaSMC";
	
	private String verifytaPath, verifytaSMCPath, tracerPath;
	private TaskMonitor monitor;
	
	public UppaalModelAnalyserFaster(TaskMonitor monitor) {
		XmlConfiguration configuration = InatBackend.get().configuration();

		this.monitor = monitor;
		this.verifytaPath = configuration.get(VERIFY_KEY);
		this.verifytaSMCPath = configuration.get(VERIFY_SMC_KEY);
		this.tracerPath = configuration.get(TRACER_KEY);
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
			//t0 = System.currentTimeMillis();
			Process proc = rt.exec(cmd);
			try {
				proc.waitFor();
			} catch (InterruptedException ex){
				proc.destroy();
				throw new Exception("Interrupted (1)");
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
			//t = System.currentTimeMillis();
			//System.out.println("Executed " + cmd[0] + " " + cmd[1] + " " + cmd[2] + " in " + (t-t0) + " ms.");
			
			result = new UppaalModelAnalyserFaster.VariablesInterpreter().analyseSMC(m, new FileInputStream(nomeFileOutput));
			
			new File(nomeFileOutput).delete();
			
			//System.err.println(averageCSV.getAbsolutePath() + " --> " + nomeFileModello);
		} catch (Exception e) {
			throw new AnalysisException("Error during analysis", e);
		}
		
		if (result == null) {
			throw new AnalysisException("Error during analysis: empty result");
		}
		
		return result;
	}
	
	public LevelResult analyze(Model m, int timeTo) throws AnalysisException {
		LevelResult result = null;
		try {
			final String uppaalModel = new VariablesModel().transform(m);
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
			
			
			//the following string is used in order to make sure that the name of .xtr output files is unique even when we are called by an application which is multi-threaded itself (it supposes of course that the input file is unique =))
			final String inputUppaalNoExtension = nomeFileModello.substring(nomeFileModello.lastIndexOf("/") + 1, nomeFileModello.indexOf("."));
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
			cmd[2] += " -t0 -o2 -y -f" + prefix + " \"" + nomeFileModello + "\" \"" + nomeFileQuery + "\"";
			Runtime rt = Runtime.getRuntime();
			//t0 = System.currentTimeMillis();
			if (monitor != null) {
				monitor.setStatus("Analyzing model with UPPAAL.");
			}
			Process proc = rt.exec(cmd);
			try {
				proc.waitFor();
			} catch (InterruptedException ex){
				proc.destroy();
				throw new Exception("Interrupted (1)");
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
			//t = System.currentTimeMillis();
			//System.out.println("Executed " + cmd[0] + " " + cmd[1] + " " + cmd[2] + " in " + (t-t0) + " ms.");
			
			if (areWeUnderWindows()) {
				cmd[0] = "cmd";
				cmd[1] = "/c";
				cmd[2] = " \"" + verifytaPath + "\"";
			} else {
				cmd[0] = "bash";
				cmd[1] = "-c";
				cmd[2] = verifytaPath;				
			}
			cmd[2] += " -t0 -o2 -y \"" + nomeFileModello + "\" - | ";
			if (areWeUnderWindows()) {
				if (!new File(tracerPath).exists()) {
					throw new FileNotFoundException("Cannot locate tracer executable! (tried in " + tracerPath + ")");
				}
				cmd[2] += " \"" + tracerPath + "\"";
			} else {
				if (!new File(tracerPath).exists()) {
					throw new FileNotFoundException("Cannot locate tracer executable! (tried in " + tracerPath + ")");
				}
				cmd[2] += tracerPath;				
			}
			cmd[2] += " - " + prefix + "-1.xtr";
			new File(prefix + "-1.xtr").deleteOnExit();
			//t0 = System.currentTimeMillis();
			if (monitor != null) {
				monitor.setStatus("Analysing UPPAAL output trace.");
			}
			proc = rt.exec(cmd, new String[]{"UPPAAL_COMPILE_ONLY=TRUE"});
			result = new UppaalModelAnalyserFaster.VariablesInterpreter().analyse(m, proc.getInputStream(), timeTo);
			try {
				proc.waitFor();
			} catch (InterruptedException ex) {
				proc.destroy();
				throw new Exception("Interrupted (2)");
			}
			if (proc.exitValue() != 0) {
				StringBuilder errorBuilder = new StringBuilder();
				errorBuilder.append(cmd[0] + cmd[1] + cmd[2] + " failed.");
				errorBuilder.append("[" + nomeFileModello + "] Tracer result: " + proc.exitValue() + "\n");
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
			//t = System.currentTimeMillis();
			//System.out.println("Executed " + cmd[0] + " " + cmd[1] + " " + cmd[2] + " in " + (t-t0) + " ms.");
			new File("output" + inputUppaalNoExtension + "-1.xtr").delete();
			
			
			//System.err.println(averageCSV.getAbsolutePath() + " --> " + nomeFileModello);
			
		} catch (Exception e) {
			throw new AnalysisException("Error during analysis", e);
		}
		
		if (result == null || result.isEmpty()) {
			throw new AnalysisException("Error during analysis: empty result");
		}
		
		return result;
	}
	
	
	
	//This is slightly different from the "official" one in the sense that it reads data directly from the input stream. This way, we don't have to read the whole stream to a string (with the consequent waste of memory) before giving an input to the interpreter
	public class VariablesInterpreter {

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
			//Pattern globalTimePattern = Pattern.compile("t\\(0\\)-globalTime<[=]?[-]?[0-9]+");
			Pattern tempoP = Pattern.compile("<[=]?[-]?[0-9]+");
			//Pattern cronometroPattern = Pattern.compile("Crono[.]metro[' ']*[=][' ']*[-]?[0-9]+");
			Pattern statePattern = Pattern.compile("[A-Za-z0-9_]+[' ']*[=][' ']*[0-9]+");
			int time = 0;
			int maxNumberOfLevels = m.getProperties().get("levels").as(Integer.class);
			HashMap<String, Double> numberOfLevels = new HashMap<String, Double>();

			while ((line = br.readLine()) != null) {
				if (!line.startsWith("State"))
					continue;
				Matcher stateMatcher = statePattern.matcher(line);
				String s = null;
				while (stateMatcher.find()) {
					s = stateMatcher.group();
					if (s.contains("_nonofficial") || s.contains("counter") || s.contains("metro"))
						continue;
					String reactantId = null;
					if (s.indexOf(' ') < s.indexOf('=')) {
						reactantId = s.substring(0, s.indexOf(' '));
					} else {
						reactantId = s.substring(0, s.indexOf('='));
					}
					if (reactantId.equals("r") || reactantId.equals("r1") || reactantId.equals("r2")) continue; //private variables are not taken into account
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

			String oldLine = null;
			while ((line = br.readLine()) != null) {
				while (!(line.startsWith("Transition") && line.contains("update?"))) { //wait until the next time we update all the official variables
					line = br.readLine();
					if (line == null) break;
				}
				if (line == null) break;
				br.readLine(); //riga vuota
				line = br.readLine();
				if (line == null) break;
				/*if (!line.startsWith("State") || !line.contains("Coord.updated"))
					continue;*/
				//Matcher timeMatcher = globalTimePattern.matcher(line);
				//Matcher timeMatcher = cronometroPattern.matcher(line);
				if (oldLine == null)
					oldLine = line;
				/*
				 * As sometimes the time is very difficult to guess (the values of clocks are NEVER explicitly present in UPPAAL traces)
				 * we look for the part of the line where all clock values are "hinted" showing the differences between clocks
				 * The meaning of t(0) is not very clear, but it surely is one of the first things you see when the trace starts to
				 * list clock values.
				 * We suppose that each of the differences shown in this way is never higher than the actual value of globalTime
				 * (the clock telling us the "time of the simulation"). So, we simply look in this part of the line for all the values of
				 * clock differences, and take the largest one as "as good an approximation as we can get" for the current value of globalTime. 
				 */
				if (line.contains("t(0)")) {//timeMatcher.find()) {
					/*String value = (timeMatcher.group().split("<")[1]);
					//String value = (timeMatcher.group().split(" = ")[1]);
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
					}*/
					String parteCoiTempi = line.substring(line.indexOf("t(0)"));
					Matcher tempoM = tempoP.matcher(parteCoiTempi);
					int max = 0;
					while (tempoM.find()) {
						String value = tempoM.group();
						if (value == null || value.equals("")) continue;
						value = value.substring(1); //skipping "<"
						int tempo = -1;
						if (value.substring(0, 1).equals("=")) {
							if (value.substring(1, 2).equals("-")) {
								tempo = Integer.parseInt(value.substring(2, value.length()));
							} else {
								tempo = Integer.parseInt(value.substring(1, value.length()));
							}
						} else {
							if (value.substring(0, 1).equals("-")) {
								tempo = Integer.parseInt(value.substring(1, value.length())) + 1;
							} else {
								tempo = Integer.parseInt(value.substring(0, value.length())) + 1;
							}
						}
						if (tempo > max) max = tempo;
					}
					int newTime = max;
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
							if (s.indexOf(' ') < s.indexOf('=')) {
								reactantId = s.substring(0, s.indexOf(' '));
							} else {
								reactantId = s.substring(0, s.indexOf('='));
							}
							if (reactantId.equals("r") || reactantId.equals("r1") || reactantId.equals("r2") || numberOfLevels.get(reactantId) == null) continue; //we check whether it is a private variable
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