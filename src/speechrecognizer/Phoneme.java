package speechrecognizer;

import java.util.ArrayList;

/**
 * 
 * @author Chris van Egmond
 * @author Cees-Wilem Hofstede
 *
 */
public class Phoneme {

	private String name;
	private State state2, state3, state4;
	private float[][] transition_probabilities = new float[5][5];
	
	// list of 3 vectors, though they are offset by 1 at the start and end because we want 
    // state 2, 3, 4 this means the first 2 lists at index 0 and 1 are empty dummies. just
    // access the appropriate list at index = state number-1 (eg. means = [[], M2, M3, M4, []])
	private float[][] means     = new float[5][5];
	private float[][] variances = new float[5][5];
	private float[][] tp        = new float[5][5];;
	
	private ArrayList<State> states = new ArrayList<State>();
	
	public Phoneme(String datastring){
		// TODO parse to data.. maybe write PhonemeFactory class for this
		String[] parts = datastring.split("<");
		name = parts[0].replace("\n", "").replace("\\", "");
        
		int index = 1;
        for(String part : parts) {
            if(part.startsWith("STATE")) {
                index = Integer.parseInt(part.replace("STATE> \n", ""))-1;
            }
            else if(part.startsWith("MEAN")) {
            	part.replace("MEAN> 39\n", "");
            	String[] subparts = part.split(" ");
            	int subindex = 0;
            	for(String x : subparts) {
            		means[index][subindex] = Float.valueOf(x.trim()).floatValue();
            		subindex++;
            	}
            }
            else if (part.startsWith("VARIANCE")) {
            	part.replace("VARIANCE> 39\n", "");
            	String[] subparts = part.split(" ");
            	int subindex = 0;
            	for(String x : subparts) {
            		variances[index][subindex] = Float.valueOf(x.trim()).floatValue();
            		subindex++;
            	}
            }
            else if (part.startsWith("TRANSP")) {
            	//TODO: THIS MAY BE A WRONG TRANSLATION FROM PYTHON!!!
            	part.replace("TRANSP> 5\n", "");
            	String[] subparts = part.split(" ");
            	int subindex = 0;
            	for(String x : subparts) {
            		tp[index][subindex] = Float.valueOf(x.trim()).floatValue();
            		subindex++;
            	}
            }
        }           
        
        //TODO: cannot add string to a State array
        //states.add("start")
        for(int i = 1; i < means.length; i++) {
        	states.add(new State(means[i], variances[i], name + (i+1), this));
        }
        //states.append("end")

//      COMPLETELY UNCLEAR WHAT HAPPENS HERE!!!
//        				|
//       				V 
//        for i in range(len(self._states)):
//            self._transp[self._states[i]] = {}
//            for j in range(len(self._states)):
//                self._transp[self._states[i]][self._states[j]] = self._tp[i][j] 
       
        //current translation from python
        int statesSize = states.size();
        for(int i = 0; i < statesSize; i++) {
        	for(int j = 0; j < statesSize; j++) {
        		 transition_probabilities = tp;
        	}
        }
	}
	
	public String getName(){
		return name;
	}
	
	public State getState(int statenumber){
		switch(statenumber){
			case 4:
				return state4;
			case 3:
				return state3;
			case 2:
			default:
				return state2; // if useless parameter, just return our first state (state2)
		}
	}
	
	public float[][] getTransitionProbabilities(){
		return transition_probabilities;
	}
	
	public String toString(){
		return name;
	}
}
