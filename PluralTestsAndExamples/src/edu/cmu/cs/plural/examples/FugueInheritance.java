package edu.cmu.cs.plural.examples;

import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Full;
import edu.cmu.cs.plural.annot.State;
import edu.cmu.cs.plural.annot.Unique;

public class FugueInheritance {
	
	@ClassStates({
		@State(name = "open", inv = "sock != null"),
		@State(name = "closed", inv = "sock == null")
	})
	class WebPageFetcher {
		
		private Object sock = null;
		
		@Full
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
			super.open();
			cache = new HashMap<String, String>();
		}
		
	}
	

}
