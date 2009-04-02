package edu.cmu.cs.plural.examples;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import edu.cmu.cs.crystal.annotations.PassingTest;
import edu.cmu.cs.crystal.annotations.UseAnalyses;
import edu.cmu.cs.plural.annot.ClassStates;
import edu.cmu.cs.plural.annot.Perm;
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
	
	public static final String OPEN = "open";
	public static final String CLOSED = "closed";
	
	@ClassStates({
		@State(name = OPEN, inv = "sock != null"),
		@State(name = CLOSED, inv = "sock == null")
	})
	class WebPageFetcher {
		
		private String domain;
		private Socket sock = null;
		
		@Perm(requires = "#0 != null", ensures = "unique(this!fr) in " + CLOSED)
		public WebPageFetcher(String domain) {
			this.domain = domain;
		}
		
		@Unique(requires = CLOSED, ensures = OPEN, use = Use.FIELDS)
		public void open() throws IOException {
			sock = new Socket(domain, 80);
		}
		
		@Unique(requires = OPEN, ensures = CLOSED, use = Use.FIELDS)
		public void close() throws IOException {
			sock.close();
			sock = null;
		}
		
		@Unique(guarantee = OPEN, use = Use.FIELDS)
		public String getPage(String path) {
			// send GET request; receive response
			return "content";
		}
		
		@Perm(requires = "#0 != null")
		@Unique(guarantee = CLOSED, use = Use.FIELDS)
		public void setDomain(String domain) {
			this.domain = domain;
		}
		
	}
	
	@ClassStates({
		@State(name = OPEN, inv = "unique(super) in open * cache != null"),
		@State(name = CLOSED, inv = "unique(super) in closed * cache == null")
	})
	class CachingFetcher extends WebPageFetcher {
		
		private Map<String, String> cache = null;
		
		@Perm(requires = "#0 != null", ensures = "unique(this!fr) in " + CLOSED)
		public CachingFetcher(String url) {
			super(url);
		}
		
		@Override
		public void open() throws IOException {
			super.open();
			cache = new HashMap<String, String>();
		}

		@Override
		public void close() throws IOException {
			super.close();
			cache = null;
		}

		@Override
		public String getPage(String path) {
			if(cache.containsKey(path))
				return cache.get(path);
			String result = super.getPage(path);
			cache.put(path, result);
			return result;
		}

		@Override
		public void setDomain(String domain) {
			super.setDomain(domain);
		}
		
	}
	

}
