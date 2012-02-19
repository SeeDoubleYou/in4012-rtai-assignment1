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
	
//	private Phoneme[] phonemes;
	private ArrayList<Phoneme> phonemes;
	private ArrayList<State> states = new ArrayList<State>();
	
	//transition probabilities.
	private Hashtable<Integer, Hashtable<Integer, Double>> trps;
	
	// These 2 are only initialized after executing the viterbi algorithm
	private ArrayList<State> best_path;
	private double probability;
	
//	public Word(String word, Phoneme[] phonemes){
	public Word(String word, ArrayList<Phoneme> phonemes){
		this.word = word;
		this.phonemes = phonemes;
		trps = new Hashtable<Integer, Hashtable<Integer, Double>>();
		
		double temp = -1;

		Hashtable<Integer, Double> hts1;
		Hashtable<Integer, Double> hts2;
		Hashtable<Integer, Double> hts3;
		
		for(int i=0; i < phonemes.size(); i++){
			State s2 = phonemes.get(i).getState(2);
			State s3 = phonemes.get(i).getState(3);
			State s4 = phonemes.get(i).getState(4);
			states.add( s2 );
			states.add( s3 );
			states.add( s4 );
			
			double[][] tps = phonemes.get(i).getTransitionProbabilities();
			if(temp != -1){
				try{
					trps.get(i*3-1).put(i*3, temp);
				}catch(Exception e){ 
					trps.put(i*3-1, new Hashtable<Integer, Double>()); 
					trps.get(i*3-1).put(i*3, temp); 
				}
			}

			hts1 = new Hashtable<Integer, Double>();
			hts2 = new Hashtable<Integer, Double>();
			hts3 = new Hashtable<Integer, Double>();
			hts1.put(i*3, tps[1][1]);
			hts1.put(i*3+1, tps[1][2]);
			hts1.put(i*3+2, tps[1][3]);
			
			hts2.put(i*3, tps[2][1]);
			hts2.put(i*3+1, tps[2][2]);
			hts2.put(i*3+2, tps[2][3]);
			
			hts3.put(i*3, tps[3][1]);
			hts3.put(i*3+1, tps[3][2]);
			hts3.put(i*3+2, tps[3][3]);
			
			
			trps.put(i*3, hts1);
			trps.put(i*3+1, hts2);
			trps.put(i*3+2, hts3);
			
			temp = tps[3][4];
		}
	}

	/**
	 * Takes a set of observations and calculates the most likely path through my states, 
	 * and returns the probability of it. The path is stored in this.best_path
	 * 
	 * @param observations 
	 * @return probability of the best path given the observations
	 */
	public double viterbi(double[][] obs){
		Hashtable<Integer, Hashtable<State, Double>>   V    = new Hashtable<Integer, Hashtable<State, Double>>();
		Hashtable<State,   ArrayList<State>>          path = new Hashtable<State,   ArrayList<State>>();
		Hashtable<State,   Hashtable<Integer, Double>> ol   = new Hashtable<State,   Hashtable<Integer, Double>>();

		V.put(0, new Hashtable<State, Double>());
		
		for(State state : states) {
			V.get(0).put(state, state.observationLikelihood(obs[0]));
			
			path.put(state, new ArrayList<State>());
			path.get(state).add(state);
			Hashtable<Integer, Double> tempobs = new Hashtable<Integer, Double>();
			for(int i=0; i<obs.length; i++){
				tempobs.put(i, state.observationLikelihood(obs[i]));
			} 
			ol.put(state, tempobs);
		}

		Hashtable<State, ArrayList<State>> newpath;
		double temp_tp;
		
		for(int i=1; i<obs.length; i++) {
			V.put(i, new Hashtable<State, Double>());
			newpath = new Hashtable<State, ArrayList<State>>();

			for(int s1=0; s1<states.size(); s1++){
				
				State temp_state = null;
				double max_prob = Double.NEGATIVE_INFINITY;
				
				for(int s2=0; s2<states.size(); s2++){
					// get transition probability (if it doesnt exist, p = 0)
					try{ 
						temp_tp = trps.get(s2).get(s1);
					}catch(Exception e){ 
						temp_tp = 0.0f;
					}
					if(temp_tp > 0.0f){
						try{
							double prob = (double) (V.get(i-1).get(states.get(s2)))	// probability so far
											 + Math.log(temp_tp) 					// transition probability from s2 to s1
												 + (ol.get(states.get(s1)).get(i)); // observation probability of s1
							
							// keep track of which state is most probably next in this path
							if(prob > max_prob){
								max_prob = prob;
								temp_state = states.get(s2);
							}
						}catch(Exception e){
						}
					}
				}
				if(temp_state != null){
					V.get(i).put(states.get(s1), max_prob);
					
					// NB: instead of previous code: we add a cloned copy, because
					// we should not add s1 to the list in path.get(temp_state).
					ArrayList<State> templist = (ArrayList<State>) path.get(temp_state).clone();
					templist.add(states.get(s1));
					newpath.put(states.get(s1), templist);
				}
			}
			// no need to remember old path
			path = newpath;
		}

		double max_prob = Double.NEGATIVE_INFINITY;
		State returnstate = null;
		Hashtable<State, Double> probs = V.get(obs.length-1);
		for(State state : states){
			if(probs.get(state) > max_prob){
				max_prob = probs.get(state);
				returnstate = state;
			}
		}
		best_path = path.get(returnstate);
		probability = max_prob;
		
		// show a dot (as a sort of progressbar, spinner doesn't work in eclipe (\b and \r are broken, sigh)
		System.out.print(".");

		return probability;
	}
	
	public ArrayList<State> getBestPath(){
		return best_path;
	}
	
	public State getFirstState(){
		if(states.isEmpty()){ return null; }
		return states.get(0);
	}
	
	public ArrayList<State> getStates(){
		return states;
	}
	
	public String getWord(){
		return word;
	}
	
	public ArrayList<Phoneme> getPhonemes(){
		return phonemes;
	}
	
	public String toString(){
		return word;
	}
}