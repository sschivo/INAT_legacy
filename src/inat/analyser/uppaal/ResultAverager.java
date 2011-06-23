package inat.analyser.uppaal;

import inat.analyser.AnalysisException;
import inat.model.Model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

import cytoscape.task.TaskMonitor;

public class ResultAverager {
	private TaskMonitor monitor = null;
	
	public ResultAverager(TaskMonitor monitor) {
		this.monitor = monitor;
	}
	
	public SimpleLevelResult analyzeAverage(Model m, int timeTo, int nRuns, boolean computeStdDev) throws AnalysisException, Exception {
		Vector<SimpleLevelResult> results = new Vector<SimpleLevelResult>(nRuns);
		UppaalModelAnalyserFaster analyzer = new UppaalModelAnalyserFaster(monitor);
		for (int i=0;i<nRuns;i++) {
			if (monitor != null) {
				monitor.setPercentCompleted((int)((double)i / nRuns * 100));
			}
			results.add((SimpleLevelResult)(analyzer.analyze(m, timeTo)));
		}
		return average(results, computeStdDev);
	}
	
	public SimpleLevelResult average(Vector<SimpleLevelResult> results, boolean computeStdDev) throws Exception {
		if (results.isEmpty()) throw new Exception("Empty result set");
		Map<String, SortedMap<Double, Double>> result = new HashMap<String, SortedMap<Double, Double>>();
		Set<String> reactantIds = results.firstElement().getReactantIds();
		
		for (String k : reactantIds) {
			result.put(k, new TreeMap<Double, Double>());
			if (computeStdDev) result.put(k + "_StdDev", new TreeMap<Double, Double>());
		}
		
		double finalTime = results.firstElement().getTimeIndices().get(results.firstElement().getTimeIndices().size()-1);
		int avgSize = 0;
		for (SimpleLevelResult l : results) {
			avgSize += l.getTimeIndices().size();
		}
		avgSize = (int)Math.round(1.0 * avgSize / results.size());
		double increment = finalTime / avgSize;
		int nValues = results.size();
		double sum = 0, sumSqrs = 0, average = 0, stdDev = 0;
		for (double i=0;i<finalTime;i+=increment) {
			for (String k : reactantIds) {
				sum = 0;
				if (computeStdDev) sumSqrs = 0;
				for (SimpleLevelResult l : results) {
					double val = l.getConcentration(k, i);
					sum += val;
					if (computeStdDev) sumSqrs += val * val;
				}
				average = sum / nValues;
				if (computeStdDev) stdDev = Math.sqrt((sumSqrs - 2 * average * sum + nValues * average * average)/ nValues);
				result.get(k).put(i, average);
				if (computeStdDev) result.get(k + "_StdDev").put(i, stdDev);
			}
		}
		for (String k : reactantIds) {
			sum = 0;
			if (computeStdDev) sumSqrs = 0;
			for (SimpleLevelResult l : results) {
				double val = l.getConcentration(k, finalTime);
				sum += val;
				if (computeStdDev) sumSqrs += val * val;
			}
			average = sum / nValues;
			if (computeStdDev) stdDev = Math.sqrt((sumSqrs - 2 * average * sum + nValues * average * average)/ nValues);
			result.get(k).put(finalTime, average);
			if (computeStdDev) result.get(k + "_StdDev").put(finalTime, stdDev);
		}
		
		return new SimpleLevelResult(result);
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
