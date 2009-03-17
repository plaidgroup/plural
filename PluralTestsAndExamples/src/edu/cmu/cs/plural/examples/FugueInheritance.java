package edu.cmu.cs.plural.examples;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.PluralAnalysis;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.Unique;

@PassingTest
@UseAnalyses({PluralAnalysis.SYNTAX, PluralAnalysis.EFFECT, PluralAnalysis.PLURAL})
public class FugueInheritance {
	
	@ClassStates({
		@State(name = "open", inv = "sock != null"),
		@State(name = "closed", inv = "sock == null")
	})
	class WebPageFetcher {
		
		private Object sock = null;
		
		@Unique(requires = "closed", ensures = "open", fieldAccess = true)
		public void open() {
			this.sock = new Object();
		}
		
	}
	
	@ClassStates({
		@State(name = "open", inv = "unique(super) in open * cache != null"),
		@State(name = "closed", inv = "unique(super) in closed * cache == null")
	})
	class CachingFetcher extends WebPageFetcher {
		
		private Map<String, String> cache = null;
		
		@Override
		public void open() {
			// TODO avoid forced unpacking with field access
			Map<String, String> c = this.cache;
			super.open();
			cache = new HashMap<String, String>();
		}
		
	}
	

}
