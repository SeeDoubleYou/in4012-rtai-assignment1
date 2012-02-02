package speechrecognizer;

import java.lang.Math;

public class State {
	
	private float[] means;
	private float[] variances;
	private String identifier;
	private Phoneme phoneme;

	public State(float[] means, float[] variances, String identifier, Phoneme phoneme) {
		this.means = means;
		this.variances = variances;
		this.identifier = identifier;
		this.phoneme = phoneme;
	}
	
	public Phoneme getPhoneme(){
		return phoneme;
	}
	
	public float observationLikelihood(float[] observation){
		if(observation.length != means.length){ return 0.0f; }
		float result = 0.0f;
		for (int i=0; i<observation.length; i++) {
			result += Math.log( logGaussianProbability(means[i], variances[i], observation[i]) );
		}
		return result;
	}
	
	public float logGaussianProbability(float mu, float sigma, float x){
		return (float) (Math.exp( -( ((x-mu)*(x-mu))/(2*sigma) ) ) / (Math.sqrt(sigma) * Math.sqrt(2*Math.PI)));
	}
	
	public String toString(){
		return identifier;
	}
	
}
