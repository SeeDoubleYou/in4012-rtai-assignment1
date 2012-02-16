package speechrecognizer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Chris van Egmond
 * @author Cees-Wilem Hofstede
 *
 */
public class SpeechRecognizer {
	static String data  	 = "data";
	static String htk  	 	 = "htk/HTKTools";
    static String conf  	 = data + "/hcopy_mfcc.cfg";
    static String hcopy 	 = htk  + "/HCopy";
    static String hlist 	 = htk  + "/HList";
    static String mfc   	 = data + "/mfc/";
    static String wav   	 = data + "/wav/";
    static String hmmsMmf    = data + "/hmms.mmf";
    static String lexiconTxt = data + "/lexicon.txt";
    
    static double[][] featureset;

    static Hashtable<String, Phoneme> phonemes = new Hashtable<String, Phoneme>();
    static ArrayList<Word> words = new ArrayList<Word>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String filename = "tf005416";
//		if(args.length < 1) {
//			// if no filename was given, ask for input of user
//			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
//			System.out.println("Please enter a filename without the extension.");
//			try {
//				filename = console.readLine();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//		else {
//			filename = args[0]; 
//		}
		
		if(filename != "") {
			System.out.println("Investigating audiofile: " + filename);
			extractFeaturesFromAudioFile(filename);
			
			System.out.println("Building a featureset");
			buildFeatureSet(filename);
			
			
			System.out.println("Building Hidden Markov Models");
			buildHmms(hmmsMmf, lexiconTxt);
			
			System.out.println("Calculating probabilities");
			Word   bestWord  = null;
			double  bestScore = -10000.0f;
		            
		    for(Word word: words) {
		    	double probability = word.viterbi(featureset);
		    	if(probability > bestScore)
		    	{
		    		bestWord = word;
		    		bestScore = probability;
		    	}
		    }
		    System.out.println("Best word is " + bestWord.getWord() + " with a probability of " + bestScore);
		}
		else {
			System.out.println("No valid filename was given, please retry");
		}
		
		/* imitate speech_recognizer.py here... how to do command line stuff? */
		
		/* 1. somehow build up all phonemes based on hmms.mmf (see phoneme.py)
		 * 2. build up all words using lexicon.txt
		 * 3. run word.viterbi(observations) on each word and for each set of
		 * observations (set of observations is the featureset from mfc files) 
		 * 4. keep track of the maximum value and which word this is
		 */
	}
	
	/**
	 * Extract features. It will create a .mfc file for it
	 * 
	 * @param fname The name of the file (without extension)
	 */
    public static void extractFeaturesFromAudioFile(String filename) {
    	String source = wav + filename + ".wav";
    	String target = mfc + filename + ".mfc";    	
    	exec(new String[] {hcopy, "-C", conf, source, target});
    }
    
    /**
     * Returns a list of vectors where each vector represents a time-slice, 
     * containing 39 Decimal objects representing features of that time-slice.
     */
    public static void buildFeatureSet(String filename) {
    	filename += ".mfc";
    	String[] featuresRV = exec(new String[] {hlist, "-r", mfc + filename});
    	
    	if(featuresRV[0].equals("0")) {
    		System.out.println("Building the features..");
    		String[] timeslices = featuresRV[1].split(" ");
    		
    		int tsl = timeslices.length/39;
    		featureset = new double[tsl][39];
    		for(int i=0; i<tsl; i++){
	    		for(int j=0; j<39; j++){
	    			featureset[i][j] = Double.valueOf(timeslices[i*39 + j].trim()).doubleValue();
	    		}
    		}
    		/*
    		int index_time = 0;
    		featureset = new double[timeslices.length][39];
    		for(String ts : timeslices) {
    			ts = ts.replace("\n", "");
    			ts = ts.trim();
    			String[] features = ts.split(" ");
    			int index_feature = 0;
    			for(String feature : features)
    			{
    				featureset[index_time][index_feature] = Double.valueOf(feature.trim()).doubleValue();
    				index_feature++;
    			}
    			
    			index_time++;
    		}
    		*/
    		System.out.println("Finished building features");
    	}
    	else {
    		System.out.println("Something went wrong with building the features, please retry");
    	}
    }
    
    /**
     * Builds HMMs for every word in the lexicon.
     * 
     * @param definitionFilename path to hmms.hmmf, contains phoneme descriptions
     * @param lexiconFilenam path to lexicon.txt, contains words and their phonemes
     */
    public static void buildHmms(String definitionFilename, String lexiconFilename) {
    	// load all data from hmms.mmf
		String[] definitions;
		try {
			definitions = readFileAsString(definitionFilename).split("~h");
	    
			phonemes = new Hashtable<String, Phoneme>();
	       
			// construct phonemes
	        for(String phonemeData: definitions) {
	        	if (!phonemeData.equals("")) {
		        	Phoneme tmp = new Phoneme(phonemeData);
		        	phonemes.put(tmp.getName(), tmp);
	        	}
	        }
	                
	        // load all data from hmms.mmf
	        List<String> lexicon = Arrays.asList(readFileAsString(lexiconFilename).split("\n"));
	        
	        // remove empty items
	        Iterator<String> i = lexicon.iterator();
	        while (i.hasNext())
	        {
	            String s = i.next();
	            if (s == null || s.isEmpty())
	            {
	                i.remove();
	            }
	            else
	            {
	            	String[] parts = s.split(" ");
	            	// parts = ["woord", "w", "oo", "r", "d"]
	            	String word = parts[0];
	            	
	            	// create phonemes for all but the first entry in parts
	            	ArrayList<Phoneme> pns = new ArrayList<Phoneme>();
	            	
	            	for(int j=1; j<parts.length; j++)
	            	{
	            		if (!parts[j].equals("")) {
		            		pns.add(phonemes.get(parts[j]));
	            		}
	            	}
	            	words.add(new Word(word, pns));
	            }
	        }
	        
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * Execute a system function
     * 
     * @param 	cmd
     * @return 	An array containing the return value and output
     */
    public static String[] exec(String[] args) {
    	Runtime RT = Runtime.getRuntime();
    	int exitVal = -1;
    	String output = "";
    	
    	ProcessBuilder builder = new ProcessBuilder(args);
    	builder.directory(new File(new File(".").getAbsolutePath()));

    	String arguments = "";
    	for(String arg: args)
    	{
    		arguments += arg + " ";
    	}
    	
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
     * Read a file and return contents as a String
     * 
     * @param filePath
     * @return
     * @throws java.io.IOException
     */
    private static String readFileAsString(String filePath) throws java.io.IOException{
        byte[] buffer = new byte[(int) new File(filePath).length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(filePath));
            f.read(buffer);
        } finally {
            if (f != null) try { f.close(); } catch (IOException ignored) { }
        }
        return new String(buffer);
    }
}
