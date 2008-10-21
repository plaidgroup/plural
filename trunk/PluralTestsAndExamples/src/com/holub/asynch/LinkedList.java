package com.holub.asynch;

import java.util.Collection;

import edu.cmu.cs.plural.annot.Perm;

// Exists so I can annotate the constructor 
public class LinkedList<E> extends java.util.LinkedList<E> {

	@Perm(ensures="unique(this!fr)")
	public LinkedList() {
		super();
	}

	public LinkedList(Collection<? extends E> c) {
		super(c);
		// TODO Auto-generated constructor stub
	}

}
