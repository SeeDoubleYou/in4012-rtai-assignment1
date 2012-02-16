package tests;

import speechrecognizer.*;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import speechrecognizer.Phoneme;
import speechrecognizer.State;

/**
 * 
 * @author Chris van Egmond
 * @author Cees-Wilem Hofstede
 *
 */
public class StateTest {
	private State state;
	private Phoneme pn = new Phoneme("random datastring");
	private String statename = "state";
	
	@Before
	public void setUp(){
		double[] means = {1,2,3,4};
		double[] variances = {1,1,1,1};
		state = new State(means, variances, statename, pn);
	}

	@Test
	public void testLogGaussianProbability(){
		assertEquals( state.logGaussianProbability(0, 1, 0), 0.3989f, 0.0001f);
	}
	
	@Test
	public void testObservationLikelihood() {
		double[] obs = {1,2,3,4};
		double[] obs2 = {1,2};
		assertEquals(state.observationLikelihood(obs2), 0.0f, 0.0001f);
		assertEquals(Math.exp(state.observationLikelihood(obs)), 0.02533f, 0.0001f);
	}
	
	@Test
	public void testGetPhoneme(){
		assertEquals(state.getPhoneme(), pn);
	}
	
	@Test
	public void testToString(){
		assertEquals(state.toString(), statename);
	}
}
