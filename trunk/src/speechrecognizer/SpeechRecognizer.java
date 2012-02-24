package speechrecognizer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import com.csvreader.CsvWriter;

/**
 * Speech recognizer for a certain given set of files (see data folder).
 * It will parse given files. Files can be given as command line parameters, or
 * if no argument was given trough a prompt. You can do several files at once
 * by given each filename separated by a space. You could also say "all" 
 * (without the quotes of course) to parse all words.
 * 
 * Entering "exit" will quit the program.
 * 
 * A log is created (comma separated) so performance can be checked.
 * @see http://javacsv.sourceforge.net/
 * 
 * @author Chris van Egmond
 * @author Cees-Wilem Hofstede
 *
 */
public class SpeechRecognizer {
	static String root		 = System.getProperty("user.dir");
	static String data  	 = root + "/data";
	static String htk  	 	 = root + "/htk/HTKTools";
    static String wav   	 = data + "/wav/";
    static String hmmsMmf    = data + "/hmms.mmf";
    static String lexiconTxt = data + "/lexicon.txt";
    static String testTxt    = data + "/testset.txt";
    
    static Hashtable<String, Phoneme> phonemes = new Hashtable<String, Phoneme>();
    static ArrayList<Word> words = new ArrayList<Word>();
    
    static ArrayList<String> filenames = new ArrayList<String>();

    static int total = 0;
    static int correct = 0;
    static int corrupt = 0;
    static long runningTime = 0;
    
    public static CsvWriter performanceLog; // file to keep track of performance (best to delete before an "all" run 
	public static String performanceLogFile = "performance.csv";
	public static boolean performanceLogExists;
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		boolean performanceLogExists = new File(performanceLogFile).exists();
		performanceLog = new CsvWriter("performance.csv");
		
		correct = corrupt = total = 0;
		 
		if(args.length > 0) {
			addFileNames(args);
		}
		
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
				performanceLog.write("milliseconds");
				performanceLog.endRecord();
				performanceLog.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//filenames.add("tm001616"); // EASY DEBUG

		// only have to do this once!
		System.out.println("Building Hidden Markov Models");
		buildHmms(hmmsMmf, lexiconTxt);
		
		run();
	}
	
	/**
	 * Run the SpeechRecognizer.
	 */
	public static void run() {
		String filename = getFilename();
		
		// exit gracefully
		if(filename.equals("exit")) {
	        performanceLog.close();
			System.exit(0);
		}
		
		// if there is a filename available, run @link{WordRecognizer} on the filename and keep track of score.
		if(!filename.isEmpty()) {
			WordRecognizer wr = new WordRecognizer(filename);
			if(wr.run()) {
				correct++;
			}
			
			long rtime = wr.getRunningTime();
			runningTime += rtime;

			System.out.println("Time: " + rtime + " ms (total " + runningTime + " ms)");
			System.out.println("correct classifications: [" 
							 + correct + "/" + total + "], " + corrupt + " corrupt files");
		    

			// Force a garbage collection cleanup
			wr = null;
			System.gc();
		}
		
		// restart
		run();
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
	 * If user sets "all", then all files in wav folder are added
	 * @return filename
	 */
	public static String getFilename() {
		String filename = "";
		
		if(filenames.size() == 0) {
			// no filename available, ask for input of user (and reset total running time)
			
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
		
		// user requested all files in testset.txt to be parsed, fill list of filenames based on 
		// filenames in this file.
		if(filename.equals("testset"))
		{
			filenames.clear();
			
			try {
				String[] testset = readFileAsString(testTxt).split("\n");
				addFileNames(testset);
				filename = getFilename();
			} catch (IOException e) {
				System.out.println("Error while reading testset.txt");
				e.printStackTrace();
			}
		}
		
		return filename;
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
	    
			// construct phonemes
	        for(String phonemeData: definitions) {
	        	if (!phonemeData.isEmpty()) {
	        		Phoneme tmp = new Phoneme(phonemeData);
		        	phonemes.put(tmp.getName(), tmp);
	        	}
	        }
	        
	        // load all data from hmms.mmf
	        List<String> lexicon = Arrays.asList(readFileAsString(lexiconFilename).split("\n"));
	        
	        for(String s : lexicon)
	        {
	            if (s.isEmpty())
	            {
	                continue;
	            }
	            
            	String[] parts = s.split(" ");
            	
            	// parts = ["woord", "w", "oo", "r", "d"]
            	String word = parts[0];
            	
            	// create phonemes for all but the first entry in parts
            	ArrayList<Phoneme> pns = new ArrayList<Phoneme>();
            	pns.add(phonemes.get("sil"));
            	for(int j=1; j<parts.length; j++)
            	{
            		if (!parts[j].isEmpty()) {
	            		pns.add(phonemes.get(parts[j]));
            		}
            	}
            	pns.add(phonemes.get("sil"));
            	words.add(new Word(word, pns));
	        }
	        
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * Read a file and return contents as a String
     * 
     * @param filePath
     * @return
     * @throws java.io.IOException
     */
    public static String readFileAsString(String filePath) throws java.io.IOException{
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
