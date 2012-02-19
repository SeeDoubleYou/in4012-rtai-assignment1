package speechrecognizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.csvreader.CsvWriter;

public class WordRecognizer {
	private String filename;
	private double[][] featureset;
	
	private static String data   = "data";
	private static String htk  	 = "htk/HTKTools";
	private static String conf   = data + "/hcopy_mfcc.cfg";
	private static String hcopy  = htk  + "/HCopy";
	private static String hlist  = htk  + "/HList";
	private static String mfc    = data + "/mfc/";
	private static String wav    = data + "/wav/";
	private static String label	 = data + "/label/";
	
	public static CsvWriter performanceLog;
    
    public WordRecognizer(String filename) {
		this.filename = filename;
	}
    
	public Boolean run() {
	 	System.out.println("\n/////////////////////////////////////////////////////////////");
		System.out.println("Investigating audiofile: " + filename);
		extractFeaturesFromAudioFile(filename);

		boolean gotFeatureset = buildFeatureSet(filename);
		boolean isCorrect = false;
		
		// We don't have to go through all this trouble if we don't have a proper featureset
		if(gotFeatureset){
			
			System.out.println("Calculating probabilities");
			Word   bestWord  = null;
			double  bestScore = Double.NEGATIVE_INFINITY;
		            
		    for(Word word: SpeechRecognizer.words) {		    	
		    	double probability = word.viterbi(featureset);
		    	if(probability > bestScore)
		    	{
		    		bestWord = word;
		    		bestScore = probability;
		    	}
		    }
		    String bestLabel = bestWord.getWord();
	    
		    System.out.println("\nBest word is " + bestLabel + " with a probability of " + bestScore);
	    
		    try {
		    	String actualLabel = SpeechRecognizer.readFileAsString(label + filename + ".lab").trim();
				SpeechRecognizer.performanceLog.write(filename);
				SpeechRecognizer.performanceLog.write(actualLabel);
				SpeechRecognizer.performanceLog.write(bestWord.getWord());
				SpeechRecognizer.performanceLog.write("" + bestScore);
				SpeechRecognizer.performanceLog.write(actualLabel.equals(bestLabel) ? "1" : "0");
				SpeechRecognizer.performanceLog.endRecord();
				SpeechRecognizer.performanceLog.flush();
				System.out.println("Given:\t\t" + actualLabel);
				System.out.println("Recognized:\t" + bestLabel);
				if(actualLabel.equals(bestLabel)) { 
					isCorrect = true;
				}
				
				SpeechRecognizer.total++;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{ SpeechRecognizer.corrupt++; }
		return isCorrect;
	}
	
	/**
	 * Extract features. It will create a .mfc file for it
	 * 
	 * @param fname The name of the file (without extension)
	 */
    public void extractFeaturesFromAudioFile(String filename) {
    	String source = wav + filename + ".wav";
    	String target = mfc + filename + ".mfc";   
    	if(!new File(target).exists()) {
    		exec(new String[] {hcopy, "-C", conf, source, target});
    	}
    }
    
    /**
     * Creates a list of vectors where each vector represents a time-slice, 
     * containing 39 Decimal objects representing features of that time-slice.
     * 
     * @param filename name of the file to extract features from
     * @return true whether features were successfully extracted
     */
    public boolean buildFeatureSet(String filename) {
    	filename += ".mfc";
    	String[] featuresRV = exec(new String[] {hlist, "-r", mfc + filename});
    	
    	if(featuresRV[0].equals("0")) {
    		String[] timeslices = featuresRV[1].split(" ");
    		
    		int tsl = timeslices.length/39;
    		featureset = new double[tsl][39];
    		for(int i=0; i<tsl; i++){
	    		for(int j=0; j<39; j++){
	    			featureset[i][j] = Double.valueOf(timeslices[i*39 + j].trim()).doubleValue();
	    		}
    		}
    	}
    	else {
    		System.out.println("Something went wrong with building the features, please retry");
    		System.out.println("-------------------------------------------------------------");
    		return false;
    	}
    	return true;
    }
    
    
    /**
     * Execute a system function
     * 
     * @param 	cmd
     * @return 	An array containing the return value and output
     */
    public String[] exec(String[] args) {
    	int exitVal = -1;
    	String output = "";
    	
    	ProcessBuilder builder = new ProcessBuilder(args);
    	builder.directory(new File(new File(".").getAbsolutePath()));

        Process process;
		try {
			process = builder.start();
			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null) {
				output += line;
			}
			
			try {
				exitVal = process.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
        
		String[] rv = new String[2];
		rv[0] = Integer.toString(exitVal);  // the return state of the command (0 if all went well)
		rv[1] = output;						// possible output of the command
		
		return rv;
    }
}
