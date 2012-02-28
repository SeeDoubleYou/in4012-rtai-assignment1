package speechrecognizer;

import java.util.ArrayList;

/**
 * 
 * @author Chris van Egmond
 * @author Cees-Willem Hofstede
 *
 */
public class Word {

	private String word;
	private ArrayList<Phoneme> phonemes;
	private State[] states;
	
	//transition probabilities
	private double[][] trps;
	
	// These 2 are only initialized after executing the viterbi algorithm
	private double probability;
	
	public Word(String word, ArrayList<Phoneme> phonemes) {
		this.word = word;
		this.phonemes = phonemes;
		
		this.trps = new double[this.phonemes.size()*3+2][this.phonemes.size()*3+2];
		
		this.states = new State[this.phonemes.size()*3];
		
		int i = 0;
		double temp = -1;
		for(Phoneme phoneme: phonemes) {
			// add the three relevant states
			states[i*3] = phoneme.getState(2);
			states[i*3+1] = phoneme.getState(3);
			states[i*3+2] = phoneme.getState(4);
			
			// build the transition probabilities table
			double[][] tps = phonemes.get(i).getTransitionProbabilities();
			if(temp != -1) {
				trps[i*3-1][i*3] = temp;
			}

			trps[i*3][i*3] = tps[1][1];
			trps[i*3][i*3+1] = tps[1][2];
			trps[i*3][i*3+2] = tps[1][3];
			
			trps[i*3+1][i*3] = tps[2][1];
			trps[i*3+1][i*3+1] = tps[2][2];
			trps[i*3+1][i*3+2] = tps[2][3];

			trps[i*3+2][i*3] = tps[3][1];
			trps[i*3+2][i*3+1] = tps[3][2];
			trps[i*3+2][i*3+2] = tps[3][3];
			
			temp = tps[3][4];

			i++;
		}
	}
	
	/**
	 * Takes a set of observations and calculates the most likely path through my states, 
	 * and returns the probability of it. The path is stored in this.best_path
	 * 
	 * @param observations 
	 * @return probability of the best path given the observations
	 */
	public double viterbi(double[][] observations) {
		int nrObservations = observations.length;
		
		/**
		 * contains mappings from time-order [0,1,2,...,<Ti>,...,obs.length-1] and state <Sx> in the HMM to a probability 
		 * denoting the highest probability of paths ending in state <Sx> if we only consider observations [0,1,2,...,<Ti>]
		 */
		double[][] V = new double[nrObservations][states.length];
		
		// ol (observation likelihood) is a helper var to contain observation likelihoods for each observation at each State.
		double[][] ol = new double[states.length][nrObservations];
		
		// anaylize <T0> separate from the other time-instances to set up some things
		for(int s0 = 0; s0 < states.length; s0++) {
			
			State state = this.states[s0];
			
			// pre-calculate all observation likelihoods (all time-instances for all states)
			// because this will save a lot of time in the inner of the for-loops later on
			for(int i=0; i<nrObservations; i++) {
				ol[s0][i] = state.observationLikelihood(observations[i]);
			}
			
			if(s0 == 0){
				// observation-likelihood of <T0> occurring for this state			
				V[0][s0] = state.observationLikelihood(observations[0]);
			}else{
				V[0][s0] = Double.NEGATIVE_INFINITY;
			}
		}

		// helper var
		double temp_tp;
		
		// analyze every observation in order, starting at <T1> (we handled <T0> separately above)
		for(int i=1; i<nrObservations; i++) {
			
			for(int s1 = 0; s1 < states.length; s1++) {
				
				// negative infinity so the first probability when looking for the max will always be larger
				double max_prob = Double.NEGATIVE_INFINITY;
				
				for(int s2 = 0; s2 < states.length; s2++) {
					try{
						// get transition probability from s2 to s1
						temp_tp = trps[s2][s1];
					}
					catch(Exception e) { 
						// if it doesn't exist, p = 0
						temp_tp = 0.0d;
					}
					
					// if the transition probability is 0, we can just skip this step (since its an impossible sequence)
					if(temp_tp > 0.0d) {
						try{
							/**
							 * calculate probability of the most likely path leading to s1 by trying for every state (s2)
							 * to find the probability if we extend the most likely path to s2 with s1 (i.e. find probabilities
							 * [<Sx>,<Sy>,...,<s2>,s1] for every <s2>). This calculation consists of 3 parts:
							 * probability = <p1: probability so far> * <p2: trans.prob. from s2 to s1> * <p3: obs. probability of s1>
							 * because we work with log's, we ADD them instead of using multiplication.
							 * p1: because all values in V are already calculated from log's, we dont have to take the log of p1.
							 * p2: the transition probabilities however are still 'regular' probabilities in [0,1] so we take the log.
							 * p3: the observation probability is already calculated as a log in the method State.observationlikelihood.
							 * Since p3 is the same for each state <s2>, it is only added after this loop.
							 */
							double prob = (double) (V[i-1][s2])	// p1: probability so far
										+ Math.log(temp_tp); 	// p2: transition probability from s2 to s1
							
							// keep track of which path is most likely to lead up to s1
							max_prob = Math.max(max_prob, prob);
						}
						catch(Exception e) {}
					}
				}//END inner for loop (stateS1)
				
				// store the probability of the most likely path ending at s1 in <Ti>
				V[i][s1] = max_prob + (ol[s1][i]); // add p3: observation probability of s1
				
			}//END outer for loop (stateS2)
			
		}//END time-forloop

		// max_prob will keep track of the highest probability of paths
		double max_prob = Double.NEGATIVE_INFINITY;
		
		// get the probabilities for each end-state at the last time-instance in V and return the highest
		double[] probs = V[nrObservations-1];
		for(int sIndex = 0; sIndex < probs.length; sIndex++){
			max_prob = Math.max(max_prob, probs[sIndex]);
		}

		probability = max_prob;
		
		return probability;
	}
	
	public State getFirstState(){
		if(states.length == 0) { 
			return null; 
		}
		return states[0];
	}
	
	public State[] getStates() {
		return states;
	}
	
	public String getWord() {
		return word;
	}
	
	public ArrayList<Phoneme> getPhonemes() {
		return phonemes;
	}
	
	public String toString() {
		return word;
	}
}