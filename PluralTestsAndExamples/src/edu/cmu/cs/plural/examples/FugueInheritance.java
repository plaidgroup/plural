package edu.cmu.cs.plural.examples;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.PluralAnalysis;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.Unique;
import edu.cmu.cs.plural.annot.Use;

/**
 * This example is modeled after the running example in the
 * ECOOP'04 paper by on Fugue, 
 * <a href="http://www.springerlink.com/content/xkyypj4843qywwlv/">"Typestates for Objects"</a>.
 * It includes a base class, {@link WebPageFetcher}, and a
 * subclass, {@link CachingFetcher}, that overrides inherited
 * methods to add caching.
 * @author Kevin Bierhoff
 * @since Apr 1, 2009
 *
 */
@PassingTest
@UseAnalyses({PluralAnalysis.SYNTAX, PluralAnalysis.EFFECT, PluralAnalysis.PLURAL})
public class FugueInheritance {
	
	@ClassStates({
		@State(name = "open", inv = "sock != null"),
		@State(name = "closed", inv = "sock == null")
	})
	class WebPageFetcher {
		
		@SuppressWarnings("unused")
		private Object sock = null;
		
		@Unique(requires = "closed", ensures = "open", use = Use.FIELDS)
		public void open() {
			this.sock = new Object();
		}
		
	}
	
	@ClassStates({
		@State(name = "open", inv = "unique(super) in open * cache != null"),
		@State(name = "closed", inv = "unique(super) in closed * cache == null")
	})
	class CachingFetcher extends WebPageFetcher {
		
		@SuppressWarnings("unused")
		private Map<String, String> cache = null;
		
		@Override
		public void open() {
			super.open();
			cache = new HashMap<String, String>();
		}
		
	}
	

}
