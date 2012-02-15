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
	private Phoneme[] phonemes;
	private ArrayList<State> states = new ArrayList<State>();
	private float[][] transition_probabilities;
	private Hashtable<Integer, Hashtable<Integer, Float>> trps;
	
	// These 2 are only initialized after executing the viterbi algorithm
	private ArrayList<State> best_path;
	private float probability;
	
	public Word(String word, Phoneme[] phonemes){
		this.word = word;
		this.phonemes = phonemes;
		int num_states = phonemes.length * 3;
		transition_probabilities = new float[num_states][num_states];
		trps = new Hashtable<Integer, Hashtable<Integer, Float>>();
		
		float temp = -1;

		Hashtable<Integer, Float> hts1;
		Hashtable<Integer, Float> hts2;
		Hashtable<Integer, Float> hts3;
		
		for(int i=0; i < phonemes.length; i++){
			State s2 = phonemes[i].getState(2);
			State s3 = phonemes[i].getState(3);
			State s4 = phonemes[i].getState(4);
			states.add( s2 );
			states.add( s3 );
			states.add( s4 );
			
			
			float[][] tps = phonemes[i].getTransitionProbabilities();
			if(temp != -1){
				try{
					trps.get(i*3-1).put(i*3, temp);
				}catch(Exception e){ 
					trps.put(i*3-1, new Hashtable<Integer, Float>()); 
					trps.get(i*3-1).put(i*3, temp); 
				}
				//try{ transition_probabilities[i*3-1][i*3] = temp; }catch( Exception e ){}
			}

			hts1 = new Hashtable<Integer, Float>();
			hts2 = new Hashtable<Integer, Float>();
			hts3 = new Hashtable<Integer, Float>();
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
/*
			transition_probabilities[i*3][i*3] = tps[1][1];
			transition_probabilities[i*3][i*3+1] = tps[1][2];
			transition_probabilities[i*3][i*3+2] = tps[1][3];

			transition_probabilities[i*3+1][i*3] = tps[2][1];
			transition_probabilities[i*3+1][i*3+1] = tps[2][2];
			transition_probabilities[i*3+1][i*3+2] = tps[2][3];

			transition_probabilities[i*3+2][i*3] = tps[3][1];
			transition_probabilities[i*3+2][i*3+1] = tps[3][2];
			transition_probabilities[i*3+2][i*3+2] = tps[3][3];
	*/		
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
	public float viterbi(float[][] obs){
		Hashtable<Integer, Hashtable<State, Float>>   V    = new Hashtable<Integer, Hashtable<State, Float>>();
		Hashtable<State,   ArrayList<State>>          path = new Hashtable<State,   ArrayList<State>>();
		Hashtable<State,   Hashtable<Integer, Float>> ol   = new Hashtable<State,   Hashtable<Integer, Float>>();

		V.put(0, new Hashtable<State, Float>());
		
		for(State state : states) {
			V.get(0).put(state, state.observationLikelihood(obs[0]));
			
			path.put(state, new ArrayList<State>());
			path.get(state).add(state);
			Hashtable<Integer, Float> tempobs = new Hashtable<Integer, Float>();
			for(int i=0; i<obs.length; i++){
				tempobs.put(i, state.observationLikelihood(obs[i]));
			} 
			ol.put(state, tempobs);
		}

		Hashtable<State, ArrayList<State>> newpath;
		float temp_tp;
		
		for(int i=1; i<obs.length; i++) {
			V.put(i, new Hashtable<State, Float>());
			newpath = new Hashtable<State, ArrayList<State>>();
			
			for(int s1=0; s1<states.size(); s1++){
				
				State temp_state = null;
				float max_prob = 0.0f;
				
				for(int s2=0; s2<states.size(); s2++){
					// get transition probability
					try{ temp_tp = trps.get(s1).get(s2); }
					catch(Exception e){ temp_tp = 0.0f; }
					if(temp_tp > 0.0f){
						try{
							float prob = (float) (Math.log(V.get(i-1).get(states.get(s2)))
										+ Math.log(temp_tp) 
										+ Math.log(ol.get(states.get(s2)).get(i)));
							if(prob > max_prob){
								max_prob = prob;
								temp_state = states.get(s2);
							}
						}catch(Exception e){}
					}
				}
				if(temp_state != null){
					V.get(i).put(states.get(s1), max_prob);
					path.get(temp_state).add(states.get(s1));
					newpath.put(states.get(s1), path.get(temp_state));
				}
			}
			// no need to remember old path
			path = newpath;
		}

		float max_prob = 0.0f;
		State returnstate = null;
		Hashtable<State, Float> probs = V.get(obs.length-1);
		for(State state : states){
			if(probs.get(state) > max_prob){
				max_prob = probs.get(state);
				returnstate = state;
			}
		}
		best_path = path.get(returnstate);
		probability = max_prob;
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
	
	public Phoneme[] getPhonemes(){
		return phonemes;
	}
	
	public String toString(){
		return word;
	}
}