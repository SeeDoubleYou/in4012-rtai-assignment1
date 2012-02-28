package speechrecognizer;

/**
 * 
 * @author Chris van Egmond
 * @author Cees-Willem Hofstede
 *
 */
public class Phoneme {

	private String name;
	private State state2, state3, state4;
	private double[][] transition_probabilities = new double[5][5];
	
	public Phoneme(String datastring){
		String[] parts = datastring.split("<");
		name = parts[0].replace("\n", "").replace("\\", "").replace("\"", "");
		name = name.trim();
		
		double[][] means     = new double[5][39];
		double[][] variances = new double[5][39];
        
		int index = -1;
        for(String part : parts) {
        	if(part.startsWith("STATE")) {
        		index++;
        	}
        	else if(part.startsWith("MEAN")) {
            	part = part.replace("MEAN> 39\n", "");
            	part = part.trim();
            	String[] subparts = part.split(" ");            	
            	int subindex = 0;
            	for(String x : subparts) {
            		means[index][subindex] = Double.valueOf(x.trim()).doubleValue();
            		subindex++;
            	}
            }
            else if (part.startsWith("VARIANCE")) {
            	part = part.replace("VARIANCE> 39\n", "");
            	part = part.trim();
            	String[] subparts = part.split(" ");
            	int subindex = 0;
            	for(String x : subparts) {
            		variances[index][subindex] = Double.valueOf(x.trim()).doubleValue();
            		subindex++;
            	}
            }
            else if (part.startsWith("TRANSP")) {
            	part = part.replace("TRANSP> 5\n", "");
            	part = part.trim();
            	String[] lines = part.split("\n");
            	int row = 0;
            	int column = 0;
            	for(String line : lines) {
            		line = line.trim();
            		String[] cells = line.split(" ");
            		for(String cell : cells){
            			transition_probabilities[row][column] = Double.valueOf(cell.trim()).doubleValue();
            			column++;
            		}
            		column = 0;
            		row++;
            	}
            }
        }           
        
        state2 = new State(means[0], variances[0], name + "2");
        state3 = new State(means[1], variances[1], name + "3");
        state4 = new State(means[2], variances[2], name + "4");
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
	
	public double[][] getTransitionProbabilities(){
		return transition_probabilities;
	}
	
	public String toString(){
		return name;
	}
}
