package speechrecognizer;

import java.lang.Math;
import java.util.Hashtable;

/**
 * 
 * @author Chris van Egmond
 * @author Cees-Willem Hofstede
 *
 */
public class State {
	
	private double[] means;
	private double[] variances;
	private String identifier;
	
	public State(double[] means, double[] variances, String identifier) {
		this.means = means;
		this.variances = variances;
		this.identifier = identifier;
	}
	
//	public Phoneme getPhoneme(){
//		return phoneme;
//	}
	
	/**
	 * Calculate and return the likelihood of the given observation for this state
	 * 
	 * @param observation
	 * @return
	 */
	public double observationLikelihood(double[] observation){
		if(observation.length != means.length) { 
			return 0.0d; 
		}
		
		double result = 0.0d;
		int nrObservations = observation.length;
		for (int i=0; i < nrObservations; i++) {
			result += logGaussianProbability(means[i], variances[i], observation[i]);
		}
		return result;
	}
	
	/**
	 * Calculate and return the natural logarithm for the Gaussian Probability
	 * 
	 * @param mu
	 * @param sigma
	 * @param x
	 * @return
	 */
	public double logGaussianProbability(double mu, double sigma, double x){
		double twosigma = 2*sigma;
		double xminmu = x-mu;
		//
		//             -(x-mu^2)/(2mu)                                               leave out constant
		//            e 										     					    |                             
		// return ln -----------------  = -((x-mu)^2)/(2sigma) - ln sqrt(2mu) + 0.572364943 = -((x-mu)^2)/(2sigma) - ln sqrt(2mu);
		//			   sqrt(2mu * PI)          
		//
		return -(((xminmu)*(xminmu))/(twosigma)) - Math.log(Math.sqrt(twosigma));
	}

	public String toString(){
		return identifier;
	}
}
