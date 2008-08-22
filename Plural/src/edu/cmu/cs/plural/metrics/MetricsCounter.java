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
package edu.cmu.cs.plural.metrics;

import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;

import edu.cmu.cs.crystal.Crystal;
import edu.cmu.cs.crystal.ICrystalAnalysis;
import edu.cmu.cs.crystal.annotations.CrystalAnnotation;

import edu.cmu.cs.plural.util.NonNullMap;

/**
 * @author Kevin Bierhoff
 * @since Aug 22, 2008
 *
 */
public class MetricsCounter implements ICrystalAnalysis {

	@Override
	public Map<String, Class<? extends CrystalAnnotation>> getAnnotationClasses() {
		return null;
	}

	@Override
	public String getName() {
		return "Metrics Counter";
	}

	@Override
	public void runAnalysis(Crystal crystal, ICompilationUnit compUnit,
			CompilationUnit rootNode) {
		MetricsWalker mw = new MetricsWalker();
		rootNode.accept(mw);
		crystal.userOut().println("**********************************************");
		crystal.userOut().println("Compilation unit: " + compUnit.getElementName());
		crystal.userOut().println("Methods: " + mw.methods);
		if(mw.annos.isEmpty()) {
			crystal.userOut().println("No annotations.");
		}
		else {
			for(Map.Entry<String, Integer> a : mw.annos.entrySet()) {
				crystal.userOut().println(a.getKey() + ": " + a.getValue());
			}
		}
	}

	/**
	 * @author Kevin Bierhoff
	 * @since Aug 22, 2008
	 *
	 */
	public class MetricsWalker extends ASTVisitor {
		
		int methods;
		Map<String, Integer> annos = NonNullMap.createIntMap();

	
		@Override
		public void endVisit(Javadoc node) {
			// TODO Auto-generated method stub
			super.endVisit(node);
		}
		@Override
		public void endVisit(MarkerAnnotation node) {
			String name = node.resolveTypeBinding().getQualifiedName();
			int count = annos.get(name);
			annos.put(name, count+1);
			super.endVisit(node);
		}
		@Override
		public void endVisit(MethodDeclaration node) {
			methods++;
			super.endVisit(node);
		}
		@Override
		public void endVisit(NormalAnnotation node) {
			String name = node.resolveTypeBinding().getQualifiedName();
			int count = annos.get(name);
			annos.put(name, count+1);
			super.endVisit(node);
		}
		@Override
		public void endVisit(SingleMemberAnnotation node) {
			String name = node.resolveTypeBinding().getQualifiedName();
			int count = annos.get(name);
			annos.put(name, count+1);
			super.endVisit(node);
		}
	}

}
