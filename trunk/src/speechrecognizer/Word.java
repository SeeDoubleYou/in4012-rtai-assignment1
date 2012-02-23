package speechrecognizer;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * 
 * @author Chris van Egmond
 * @author Cees-Wilem Hofstede
 *
 */
public class Word {

	private String word;
	
	private ArrayList<Phoneme> phonemes;
	private ArrayList<State> states = new ArrayList<State>();
	
	//transition probabilities.
	private Hashtable<Integer, Hashtable<Integer, Double>> trps = new Hashtable<Integer, Hashtable<Integer, Double>>();
	
	// These 2 are only initialized after executing the viterbi algorithm
	private double probability;
	
//	public Word(String word, Phoneme[] phonemes){
	public Word(String word, ArrayList<Phoneme> phonemes) {
		this.word = word;
		this.phonemes = phonemes;
		
		//TODO: add comments on these
		Hashtable<Integer, Double> hts1;
		Hashtable<Integer, Double> hts2;
		Hashtable<Integer, Double> hts3;
		
		int i = 0;
		double temp = -1;
		for(Phoneme phoneme: phonemes) {
			states.add(phoneme.getState(2));
			states.add(phoneme.getState(3));
			states.add(phoneme.getState(4));
			
			double[][] tps = phonemes.get(i).getTransitionProbabilities();
			if(temp != -1) {
				trps.get(i*3-1).put(i*3, temp);
			}

			hts1 = new Hashtable<Integer, Double>();
			hts2 = new Hashtable<Integer, Double>();
			hts3 = new Hashtable<Integer, Double>();
			
			hts1.put(i*3,   tps[1][1]);
			hts1.put(i*3+1, tps[1][2]);
			hts1.put(i*3+2, tps[1][3]);
			
			hts2.put(i*3,   tps[2][1]);
			hts2.put(i*3+1, tps[2][2]);
			hts2.put(i*3+2, tps[2][3]);
			
			hts3.put(i*3,   tps[3][1]);
			hts3.put(i*3+1, tps[3][2]);
			hts3.put(i*3+2, tps[3][3]);
			
			trps.put(i*3,   hts1);
			trps.put(i*3+1, hts2);
			trps.put(i*3+2, hts3);
			
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
	public double viterbi(double[][] oservations) {
		int nrObservations = oservations.length;
		
		/**
		 * contains mappings from time-order [0,1,2,...,<Ti>,...,obs.length-1] to another Hashtable which contains
		 * mappings from each state <Sx> in the HMM to a probability denoting the highest probability of paths
		 * ending in state <Sx> if we only consider observations [0,1,2,...,<Ti>]
		 */
		Hashtable<Integer, Hashtable<State, Double>> V = new Hashtable<Integer, Hashtable<State, Double>>();

		/**
		 * ol (observation likelihood) is a helper hashtable to contain observation likelihoods. Each State will
		 * contain a Hashtable mapping time-order to the likelihood that time-instance occurred at that state.
		 */
		Hashtable<State, Hashtable<Integer, Double>> ol = new Hashtable<State, Hashtable<Integer, Double>>();

		// add new hashtable (that maps states to a probability) for time-instance 0
		V.put(0, new Hashtable<State, Double>());
		
		// anaylize <T0> separate from the other time-instances to set up some things
		for(State state : states) {
			// observation-likelihood of <T0> occurring for this state
			V.get(0).put(state, state.observationLikelihood(oservations[0]));
			
			// each state <Sx> will get a list of most probable paths ending in <Sx>. Because we have
			// only yet analyzed <T0> at this point, the most likely path ending in <Sx> is itself: [<Sx>]
			ArrayList<State> stateList = new ArrayList<State>();
			stateList.add(state);
			
			// pre-calculate all observation likelihoods (all time-instances for all states)
			// because this will save a lot of time in the inner of the for-loops later on
			Hashtable<Integer, Double> tempobs = new Hashtable<Integer, Double>();
			for(int i=0; i<nrObservations; i++) {
				tempobs.put(i, state.observationLikelihood(oservations[i]));
			} 
			ol.put(state, tempobs);
		}

		// helper var
		double temp_tp;
		
		// analyze every observation in order, starting at <T1> (we handled <T0> separately above)
		
		for(int i=1; i<nrObservations; i++) {
			
			V.put(i, new Hashtable<State, Double>());
			
			for(int s1 = 0; s1 < states.size(); s1++) {
				State stateS1 = states.get(s1);

				/**
				 *  helper var to keep track of the state that is most likely to contain the path that continues at s1
				 */
				State temp_state = null;
				
				// negative infinity so the first probability when looking for the max will always be larger
				double max_prob = Double.NEGATIVE_INFINITY;
				
				for(int s2 = 0; s2 < states.size(); s2++) {
					State stateS2 = states.get(s2);
					
					try{
						// get transition probability from s2 to s1
						temp_tp = trps.get(s2).get(s1);
					}
					catch(Exception e) { 
						// if it doesn't exist, p = 0
						temp_tp = 0.0f;
					}
					
					// if the transition probability is 0, we can just skip this step (since its an impossible sequence)
					if(temp_tp > 0.0f) {
						try{
							// calculate probability of the most likely path leading to s1 by trying for every state (s2)
							// to find the probability if we extend the most likely path to s2 with s1 (i.e. find probabilities
							// [<Sx>,<Sy>,...,<s2>,s1] for every <s2>). This calculation consists of 3 parts:
							
							// probability = <p1: probability so far> * <p2: trans.prob. from s2 to s1> * <p3: obs. probability of s1>
							// because we work with log's, we ADD them instead of using multiplication.
							
							// p1: because all values in V are already calculated from log's, we dont have to take the log of p1.
							// p2: the transition probabilities however are still 'regular' probabilities in [0,1] so we take the log.
							// p3: the observation probability is already calculated as a log in the method State.observationlikelihood.
							double prob = (double) (V.get(i-1).get(stateS2))	// p1: probability so far
											   + Math.log(temp_tp) 					// p2: transition probability from s2 to s1
											   + (ol.get(stateS1).get(i));   // p3: observation probability of s1
							
							// keep track of which path is most likely to lead up to this state
							if(prob > max_prob) {
								max_prob = prob;
								temp_state = stateS2;
							}
						}
						catch(Exception e) {}
					}
				}//END inner for loop
				
				// just a precaution to make sure we do have a valid path
				if(temp_state != null) {
					// get the hashmap at the current timeinstance <Ti> and put the 
					// probability of the most likely path ending at s1 in <Ti>
					V.get(i).put(stateS1, max_prob);
				}
			}//END outer for loop
			
			// no need to remember old path, write the temp path on 'path' for the next time-iteration
		}

		// max_prob will keep track of the highest probability of paths
		double max_prob = Double.NEGATIVE_INFINITY;
		
		// get the probabilities for each end-state at the last time-instance in V.
		Hashtable<State, Double> probs = V.get(nrObservations-1);
		for(State state : states) {
			if(probs.get(state) > max_prob) {
				max_prob = probs.get(state);
			}
		}
		probability = max_prob;
		
		return probability;
	}
	
	public State getFirstState(){
		if(states.isEmpty()) { 
			return null; 
		}
		return states.get(0);
	}
	
	public ArrayList<State> getStates() {
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