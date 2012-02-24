package speechrecognizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import com.csvreader.CsvWriter;

public class WordRecognizer {
	private String filename;
	private double[][] observations;
	
	private static String root		   = System.getProperty("user.dir");
	private static final String data   = root + "/data";
	private static final String htk    = root + "/htk/HTKTools";
	private static final String conf   = data + "/hcopy_mfcc.cfg";
	private static final String hcopy  = htk  + "/HCopy";
	private static final String hlist  = htk  + "/HList";
	private static final String mfc    = data + "/mfc/";
	private static final String wav    = data + "/wav/";
	private static final String label  = data + "/label/";
	
	private static final int nrFeatures = 39;
	
	private static long runningTime;
	
	public static CsvWriter performanceLog;
    
    public WordRecognizer(String filename) {
		this.filename = filename;
		this.runningTime = 0;
	}
    
	public Boolean run() {

		long lStartTime = new Date().getTime();
		
	 	System.out.println("\n/////////////////////////////////////////////////////////////");
		System.out.println("Investigating audiofile: " + filename);
		buildMFCFileFromAudio(filename);

		boolean gotObservations = buildObservations(filename);
		boolean isCorrect = false;
		
		// We don't have to go through all this trouble if we don't have a proper featureset
		if(gotObservations){
			
			System.out.println("Calculating probabilities");
			Word   bestWord  = null;
			double  bestScore = Double.NEGATIVE_INFINITY;
			
			int wordCount = 0;
		    for(Word word: SpeechRecognizer.words) {		    	
		    	double probability = word.viterbi(observations);
		    	if(probability > bestScore)
		    	{
		    		bestWord = word;
		    		bestScore = probability;
		    	}
		    	
		    	// some sort of progress bar (a spinner doesn't work in eclipse since it lacks support for \r and \b)
		    	if(wordCount % 10 == 0) {
		    		System.out.print(".");
		    	}
		    	wordCount++;
		    }
		    String bestLabel = bestWord.getWord();
		    
			long lEndTime = new Date().getTime();
			
			// time difference in seconds
			this.runningTime = lEndTime - lStartTime;
			
			System.out.println("\nBest word is " + bestLabel + " with a log(p) of " + bestScore);
	    
		    try {
		    	String actualLabel = SpeechRecognizer.readFileAsString(label + filename + ".lab").trim();
				SpeechRecognizer.performanceLog.write(filename);
				SpeechRecognizer.performanceLog.write(actualLabel);
				SpeechRecognizer.performanceLog.write(bestWord.getWord());
				SpeechRecognizer.performanceLog.write("" + bestScore);
				SpeechRecognizer.performanceLog.write(actualLabel.equals(bestLabel) ? "1" : "0");
				SpeechRecognizer.performanceLog.write("" + this.runningTime);
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
	 * Extract features and create a .mfc file for it
	 * 
	 * @param fname The name of the file (without extension)
	 */
    public void buildMFCFileFromAudio(String filename) {
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
    public boolean buildObservations(String filename) {
    	filename += ".mfc";
    	String[] featuresRV = exec(new String[] {hlist, "-r", mfc + filename});
    	
    	if(featuresRV[0].equals("0")) {
    		String[] timeslices = featuresRV[1].split(" ");
    		
    		int nrTimeslices = timeslices.length / nrFeatures;
    		observations = new double[nrTimeslices][nrFeatures];
    		
    		for(int i = 0; i < nrTimeslices; i++) {
	    		for(int j = 0; j < nrFeatures; j++) {
	    			observations[i][j] = Double.valueOf(timeslices[i * nrFeatures + j].trim()).doubleValue();
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
    
    /**
     * Print a String Array for debugging
     * 
     * @param array
     */
    public void printStringArray(String[] array) {
    	for (int i=0; i < array.length; i++) {
    		System.out.print(" " + array[i]);
    	}
    	System.out.println("");
    }
    
    /**
     * Print a 2D String Array for debugging
     * 
     * @param array
     */
    public void print2DStringArray(String[][] array) {
    	for (int i=0; i < array.length; i++) {
    		for (int j=0; j < array[i].length; j++) {
    			System.out.print(" " + array[i][j]);
    		}
    		System.out.println("");
    	}
    }
    
    /**
     * Returns the time (in ms) it took to recognize the word
     * 
     * @return long
     */
    public long getRunningTime(){
    	return this.runningTime;
    }
}
