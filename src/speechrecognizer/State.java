package speechrecognizer;

import java.lang.Math;

/**
 * 
 * @author Chris van Egmond
 * @author Cees-Wilem Hofstede
 *
 */
public class State {
	
	private double[] means;
	private double[] variances;
	private String identifier;
	private Phoneme phoneme;

	public State(double[] means, double[] variances, String identifier, Phoneme phoneme) {
		this.means = means;
		this.variances = variances;
		this.identifier = identifier;
		this.phoneme = phoneme;
	}
	
	public Phoneme getPhoneme(){
		return phoneme;
	}
	
	public double observationLikelihood(double[] observation){
		if(observation.length != means.length){ return 0.0f; }
		double result = 0.0f;
		for (int i=0; i<observation.length; i++) {
			result += Math.log( logGaussianProbability(means[i], variances[i], observation[i]) );
		}
		return result;
	}
	
	public double logGaussianProbability(double mu, double sigma, double x){
		return (double) (Math.exp( -( ((x-mu)*(x-mu))/(2*sigma) ) ) / (Math.sqrt(sigma) * Math.sqrt(2*Math.PI)));
	}
	
	public String toString(){
		return identifier;
	}
	
}
