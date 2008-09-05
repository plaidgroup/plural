/**
 * Copyright (C) 2007, 2008 by Kevin Bierhoff and Nels E. Beckman
 *
 * This file is part of Plural.
 *
 * Plural is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * Plural is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Plural; if not, see <http://www.gnu.org/licenses>.
 *
 * Linking Plural statically or dynamically with other modules is
 * making a combined work based on Plural. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * In addition, as a special exception, the copyright holders of Plural
 * give you permission to combine Plural with free software programs or
 * libraries that are released under the GNU LGPL and with code
 * included in the standard release of Eclipse under the Eclipse Public
 * License (or modified versions of such code, with unchanged license).
 * You may copy and distribute such a system following the terms of the
 * GNU GPL for Plural and the licenses of the other code concerned.
 *
 * Note that people who make modified versions of Plural are not
 * obligated to grant this special exception for their modified
 * versions; it is their choice whether to do so. The GNU General
 * Public License gives permission to release a modified version
 * without this exception; this exception also makes it possible to
 * release a modified version which carries forward this exception.
 */
package edu.cmu.cs.plural.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.cs.crystal.Crystal;
import edu.cmu.cs.crystal.IAnalysisReporter;
import edu.cmu.cs.crystal.IRunCrystalCommand;
import edu.cmu.cs.crystal.internal.AbstractCrystalPlugin;
import edu.cmu.cs.crystal.internal.Utilities;
import edu.cmu.cs.crystal.internal.WorkspaceUtilities;
import edu.cmu.cs.crystal.test.SystemTestUtils;
import edu.cmu.cs.plural.track.FractionalAnalysis;

/**
 * This class is the old way of testing Plural. 
 * Now use edu.cmu.cs.crystal.test.AnnotatedTest.
 * 
 * @see edu.cmu.cs.crystal.test.AnnotatedTest
 */
@Deprecated
public class PluralSystemTest {
	
	private Crystal crystal;

	@Before
	public void setUp() throws Exception {
		crystal = AbstractCrystalPlugin.getCrystalInstance();
	}

	@Test
	public void testFractionalAnalysis() throws Exception {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("PermissionTest");
		project.open(null /* IProgressMonitor */);
		
		FractionalAnalysis.checkArrays = true; // force array checks
		
		List<String> files = new ArrayList<String>(10);
		files.add("PermissionTest/src/edu.cmu.cs.plural.test/ArrayIterator");
		files.add("PermissionTest/src/edu.cmu.cs.plural.test/BufferedStream");
		files.add("PermissionTest/src/edu.cmu.cs.plural.test/FractionTest");
		files.add("PermissionTest/src/edu.cmu.cs.plural.test/ModifyingIterator");
		files.add("PermissionTest/src/edu.cmu.cs.plural.test/StreamProtocol");
		files.add("PermissionTest/src/edu.cmu.cs.plural.test/Chat");
		files.add("PermissionTest/src/edu.cmu.cs.plural.test/RequestGenerator");
		files.add("PermissionTest/src/edu.cmu.cs.plural.test/SplittingPure");
		files.add("PermissionTest/src/edu.cmu.cs.plural.test/CasesTest");
		files.add("PermissionTest/src/edu.cmu.cs.plural.test/SpecInheritanceTest");
		
		final List<ICompilationUnit> compUnits = WorkspaceUtilities.findCompilationUnits(files);
		Assert.assertEquals("Could not find all test files in workspace; maybe you need to update the test projects?", files.size(), compUnits.size());
		
		IRunCrystalCommand run_command = new IRunCrystalCommand(){
			public Set<String> analyses() {
				return Collections.singleton("FractionalAnalysis");
			}
			public List<ICompilationUnit> compilationUnits() { return compUnits; }
			public IAnalysisReporter reporter() { return Utilities.nyi(); }
		};
		
		crystal.runAnalyses(run_command, null);
		
		SystemTestUtils.assertEqualContent(
				new File("regression/permission-test.expected"), 
				new File("log/regression.log"));
	}

}
