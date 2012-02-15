package speechrecognizer;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;


/**
 * 
 * @author Chris van Egmond
 * @author Cees-Wilem Hofstede
 *
 */
public class SpeechRecognizer {
	static String data  = "data/";
	static String htk  = "htk/";
    static String conf  = data + "hcopy_mfcc.cfg";
    static String hcopy = htk + "HCopy -C " + conf;
    static String hlist = htk + "HList -r ";
    static String mfc   = data + "mfc/";
    static String wav   = data + "wav/";
    
    static float[][] featureset;

    static Hashtable<String, Phoneme> phonemes = new Hashtable<String, Phoneme>();
    static ArrayList<Word> words = new ArrayList<Word>();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String filename = "tf000416";
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
			buildHmms(filename, filename);
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
    public static void extractFeaturesFromAudioFile(String fname) {
    	String source = wav + fname + ".wav";
    	String target = mfc + fname + ".mfc";
    	exec(hcopy + "  " + source + " " + target);
    }
    
    /**
     * Returns a list of vectors where each vector represents a time-slice, 
     * containing 39 Decimal objects representing features of that time-slice.
     */
    public static void buildFeatureSet(String filename) {
    	filename += ".mfc";
    	
    	String[] featuresRV = exec(hlist + mfc + filename);
    	
    	if(featuresRV[0].equals("0")) {
    		System.out.println("Succesfully build the features");
    		String[] timeslices = featuresRV[1].split(" ");
    		int index_time = 0;
    		for(String ts : timeslices) {
    			ts = ts.replace("\n", "");
    			ts = ts.trim();
    			String[] features = ts.split(" ");
    			int index_feature = 0;
    			for(String feature : features)
    			{
    				featureset[index_time][index_feature] = Float.valueOf(feature.trim()).floatValue();
    				index_feature++;
    			}
    			
    			index_time++;
    		}
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
	        	Phoneme tmp = new Phoneme(phonemeData);
	        	phonemes.put(tmp.getName(), tmp);
	        }
	                
	        // load all data from hmms.mmf
	        ArrayList<String> lexicon =  (ArrayList<String>) Arrays.asList(readFileAsString(lexiconFilename).split("\n"));
	        
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
	            	Phoneme[] pns = new Phoneme[parts.length-1];
	            	for(int j=1; j<parts.length; j++)
	            	{
	            		pns[j-1] = phonemes.get(parts[j]);
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
    public static String[] exec(String cmd) {
    	Runtime RT = Runtime.getRuntime();
    	int exitVal = -1;
    	String output = "";
    	
		try {
			// execute the command
			Process proc = RT.exec(cmd); 
			
			// read output from the command (only default output, errors are left out)
			BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line;
		    while ((line = in.readLine()) != null) {
		    	output += line;
		    }
		    
			try {
				// wait for the command to finish, then record the return state
				exitVal = proc.waitFor(); 
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		catch (IOException e) {
			System.out.println("ERROR PROCESSING: "+cmd);
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
