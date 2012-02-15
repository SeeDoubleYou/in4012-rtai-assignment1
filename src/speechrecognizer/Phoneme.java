package speechrecognizer;

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
	
	public Phoneme(String datastring){
		String[] parts = datastring.split("<");
		name = parts[0].replace("\n", "").replace("\\", "");
		
		float[][] means     = new float[5][5];
		float[][] variances = new float[5][5];
        
		int index = 0;
        for(String part : parts) {
        	if(part.startsWith("MEAN")) {
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
            	part.replace("TRANSP> 5\n", "");
            	String[] lines = part.split(" ");
            	int row = 0;
            	int column = 0;
            	for(String line : lines) {
            		String[] cells = line.split(" ");
            		for(String cell : cells){
            			transition_probabilities[row][column] = Float.valueOf(cell.trim()).floatValue();
            			column++;
            		}
            		column = 0;
            		row++;
            	}
            }
        	index++;
        }           

        state2 = new State(means[0], variances[0], name + "2", this);
        state3 = new State(means[1], variances[1], name + "3", this);
        state4 = new State(means[2], variances[2], name + "4", this);
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
