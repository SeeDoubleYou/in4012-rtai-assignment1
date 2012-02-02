package speechrecognizer;

import java.io.*;

public class SpeechRecognizer {

    String data = "../data/";
    String conf = data+"hcopy_mfcc.cfg";
    String hcopy = "HCopy -C "+conf;
    String mfc = data+"mfc/";
    String wav = data+"wav/";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String filename = "tf000116.wav";
		try{ filename = args[0]; }catch(Exception e){}
		
		/* imitate speech_recognizer.py here... how to do command line stuff? */
		
		/* TODO figure out how to do the same commandline type stuff and String
		 * 		parsing like in speech_recognizer.py and phoneme.py 
		 */
		
		/* 1. somehow build up all phonemes based on hmms.mmf (see phoneme.py)
		 * 2. build up all words using lexicon.txt
		 * 3. run word.viterbi(observations) on each word and for each set of
		 * observations (set of observations is the featureset from mfc files) 
		 * 4. keep track of the maximum value and which word this is
		 */
		
		try {
			
			Runtime.getRuntime().exec("ls -la");

		} catch (IOException e) {}
		
		
	}
	
	

    public void _extract_features_from_audio_file(String fname){
    	String source = wav+fname+".wav";
    	String target = mfc+fname+".mfc";
    	try {
			Runtime.getRuntime().exec(hcopy+" "+source+" "+target);
		} catch (IOException e) {}
    }

}
