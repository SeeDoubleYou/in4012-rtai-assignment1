package speechrecognizer;

public class Phoneme {

	private String name;
	private State state2, state3, state4;
	private float[][] transition_probabilities = new float[5][5];
	
	public Phoneme(String datastring){
		// TODO parse to data.. maybe write PhonemeFactory class for this
		String[] parts = datastring.split("<");
		//name = parts[0].
		
		/*
        self._name = parts[0].strip("\" \n")
        
        # list of 3 vectors, though they are offset by 1 at the start and end because we want 
        # state 2, 3, 4 this means the first 2 lists at index 0 and 1 are empty dummies. just
        # access the appropriate list at index = state number-1 (eg. means = [[], M2, M3, M4, []])
        self._means = [[]]*5
        self._variances = [[]]*5
        
        self._tp = []
        
        # TODO make this more generic? e.g. this messes stuff up if there are more than 39 values
        
        index = 1;
        for part in parts:
            if part.startswith("STATE"):
                index = int(part.strip("STATE> \n"))-1
            elif part.startswith("MEAN"):
                self._means[index] = [float(x) for x in part.strip("MEAN> 39\n").split(" ")]
            elif part.startswith("VARIANCE"):
                self._variances[index] = [float(x) for x in part.strip("VARIANCE> 39\n").split(" ")]
            elif part.startswith("TRANSP"):
                wholething = part.strip("TRANSP> 5\n").split("\n ")
                for thing in wholething:
                    self._tp.append( [float(x) for x in thing.split(" ")] )
                    
        self._states = []
        self._states.append("start")
        for i in range(1, len(self._means)-1):
            self._states.append(State( means=self._means[i], 
                                       variances=self._variances[i], 
                                       identifier=self._name+str(i+1),
                                       phoneme=self ))
        self._states.append("end")
            
        self._transp = {}
        for i in range(len(self._states)):
            self._transp[self._states[i]] = {}
            for j in range(len(self._states)):
                self._transp[self._states[i]][self._states[j]] = self._tp[i][j] 
		 */
		
	}
	
	public String getName(){
		return name;
	}
	
	public State getState(int statenumber){
		switch(statenumber){
			case 2:
				return state2;
			case 3:
				return state3;
			case 4:
				return state4;
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
