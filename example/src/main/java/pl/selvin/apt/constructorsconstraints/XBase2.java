package pl.selvin.apt.constructorsconstraints;

import pl.selvin.apt.constructorsconstraints.annotations.ConstructorConstraint;

@ConstructorConstraint(arguments = {String.class})
public abstract class XBase2 {
	public XBase2(String arg) {
	}
}