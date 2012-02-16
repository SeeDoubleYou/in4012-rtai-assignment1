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

import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;


/**
 * Speech recognizer for a certain given set of files (see data folder).
 * It will parse given files. Files can be given as command line parameters, or
 * if no argument was given trough a prompt. You can do several files at once
 * by given ech filename seperated by a space. You could also say "all" 
 * (without the quotes of couorse) to parse all words.
 * 
 * Entering "exit" wil quit the
 * program.
 * 
 * A log is created (comma seperated) so performance can be checked.
 * @see http://javacsv.sourceforge.net/
 * 
 * @author Chris van Egmond
 * @author Cees-Wilem Hofstede
 *
 */
public class SpeechRecognizer {
	static String data  	 = "data";
	static String htk  	 	 = "htk";
    static String conf  	 = data + "/hcopy_mfcc.cfg";
    static String hcopy 	 = htk  + "/HCopy";
    static String hlist 	 = htk  + "/HList";
    static String mfc   	 = data + "/mfc/";
    static String wav   	 = data + "/wav/";
    static String label	 	 = data + "/label/";
    static String hmmsMmf    = data + "/hmms.mmf";
    static String lexiconTxt = data + "/lexicon.txt";
    
    static double[][] featureset;

    static Hashtable<String, Phoneme> phonemes = new Hashtable<String, Phoneme>();
    static ArrayList<Word> words = new ArrayList<Word>();
    
    static ArrayList<String> filenames = new ArrayList<String>();

    static CsvWriter performanceLog; // file to keep track of performance (best to delete before an "all" run 
    static String performanceLogFile = "performance.csv";
    static boolean performanceLogExists;
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    boolean performanceLogExists = new File(performanceLogFile).exists();
		performanceLog = new CsvWriter("performance.csv");
		 
		try {
			// if the performance logfile didn't already exist then we need to write out the header line
			// else assume that the file already has the correct header line
			if (!performanceLogExists)
			{
				performanceLog.write("File");
				performanceLog.write("Label");
				performanceLog.write("recognized");
				performanceLog.write("probability");
				performanceLog.write("correct");
				performanceLog.endRecord();
				performanceLog.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(args.length > 0) {
			addFileNames(args);
		}
		
		// filenames.add("tm001616"); // EASY DEBUG
		
		run(getFilename());
	}
	
	/**
	 * Add a list of files to parse (does a clear first)
	 * @param args
	 */
	public static void addFileNames(String[] args) {
		filenames.clear();
		if(args.length > 0) {
			for(String arg : args) {
				addFileName(arg);
			}
		}
	}

	/**
	 * Add a file to parse
	 * @param filename
	 */
	public static void addFileName(String filename) {
		// add filename with optional extension removed
		filenames.add(filename.trim().replaceFirst("[.][^.]+$", ""));
	}
	
	/**
	 * Get the next filename in line to parse.
	 * If no filename is available, ask user for input.
	 * If user sets "all", then al files in wav folder are added
	 * @return filename
	 */
	public static String getFilename() {
		String filename = "";
		
		if(filenames.size() == 0) {
			// no filename available, ask for input of user
			BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Which file(s) would you like to try?");
			try {
				String[] args = console.readLine().split(" ");
				if(args.length > 0) {
					addFileNames(args);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else {
			filename = filenames.remove(0);
		}
		
		// user requested all files to be parsed, fill list of filenames based on 
		// folder and call self again.
		if(filename.equals("all"))
		{
			filenames.clear();
			File dir = new File(wav);
			for (File child : dir.listFiles()) {
				if (".".equals(child.getName()) || "..".equals(child.getName())) {
					continue;  // Ignore the self and parent aliases.
				}
				addFileName(child.getName());  
			}
			filename = getFilename();
		}
		
		return filename;
	}
	
	public static void run(String filename) {
		if(filename.equals("exit")) {
	        performanceLog.close();
			System.exit(0);
		}
		
		if(!filename.equals("")) {
			System.out.println("Investigating audiofile: " + filename);
			extractFeaturesFromAudioFile(filename);
			
			System.out.println("Building a featureset");
			buildFeatureSet(filename);
			
			System.out.println("Building Hidden Markov Models");
			buildHmms(hmmsMmf, lexiconTxt);
			
			System.out.println("Calculating probabilities");
			Word   bestWord  = null;
			double  bestScore = Double.NEGATIVE_INFINITY;
		            
		    for(Word word: words) {		    	
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
		    	String actualLabel = readFileAsString(label + filename + ".lab").trim();
				performanceLog.write(filename);
				performanceLog.write(actualLabel);
				performanceLog.write(bestWord.getWord());
				performanceLog.write("" + bestScore);
				performanceLog.write(actualLabel.equals(bestLabel) ? "1" : "0");
				performanceLog.endRecord();
				performanceLog.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
<<<<<<< .mine
		
		run(getFilename());
=======
>>>>>>> .r18
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
