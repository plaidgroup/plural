package edu.cmu.cs.plural.states;

import edu.cmu.cs.crystal.annotations.CrystalAnnotation;
import edu.cmu.cs.crystal.annotations.ICrystalAnnotation;

public class InStateMappingAnnotation implements ICrystalAnnotation {

	private final CrystalAnnotation crystalAnnotation;
	
	public InStateMappingAnnotation() {
		crystalAnnotation = new CrystalAnnotation();
	}

	public InStateMappingAnnotation(String s) {
		crystalAnnotation = new CrystalAnnotation(s);
	}

	public String getName() {
		return crystalAnnotation.getName();
	}

	public Object getObject(String key) {
		return crystalAnnotation.getObject(key);
	}

	public void setName(String name) {
		crystalAnnotation.setName(name);
	}

	public void setObject(String key, Object value) {
		crystalAnnotation.setObject(key, value);
	}
	
	public String getStateName() {
		return this.crystalAnnotation.getObject("value").toString();
	}

}
