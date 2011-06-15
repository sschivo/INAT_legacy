package inat.analyser;

import java.io.Serializable;

public class SMCResult implements Serializable {
	
	private static final long serialVersionUID = -1032066046223183090L;

	private boolean isBoolean,
					booleanResult;
	
	private double confidence,
				   lowerBound,
				   upperBound;
	
	public SMCResult(boolean result, double confidence) {
		this.isBoolean = true;
		this.booleanResult = result;
		this.confidence = confidence;
	}
	
	public SMCResult(double lowerBound, double upperBound, double confidence) {
		this.isBoolean = false;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.confidence = confidence;
	}
	
	public boolean isBoolean() {
		return isBoolean;
	}
		
	public double getLowerBound() {
		return this.lowerBound;
	}
	
	public double getUpperBound() {
		return this.upperBound;
	}
	
	public double getConfidence() {
		return this.confidence;
	}
	
	public boolean getBooleanResult() {
		return this.booleanResult;
	}
	
	public String toString() {
		if (isBoolean) {
			return (booleanResult?"TRUE":"FALSE") + " with confidence " + confidence;
		} else {
			return "Probability in [" + lowerBound + ", " + upperBound + "] with confidence " + confidence;
		}
	}
}
