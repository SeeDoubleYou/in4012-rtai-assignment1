package tests;

import speechrecognizer.*;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import speechrecognizer.Phoneme;
import speechrecognizer.State;

public class StateTest {
	
	private State state;
	private Phoneme pn = new Phoneme("random datastring");
	private String statename = "state";
	
	@Before
	public void setUp(){
		float[] means = {1,2,3,4};
		float[] variances = {1,1,1,1};
		state = new State(means, variances, statename, pn);
	}

	@Test
	public void testLogGaussianProbability(){
		assertEquals( state.logGaussianProbability(0, 1, 0), 0.3989f, 0.0001f);
	}
	
	@Test
	public void testObservationLikelihood() {
		float[] obs = {1,2,3,4};
		float[] obs2 = {1,2};
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
