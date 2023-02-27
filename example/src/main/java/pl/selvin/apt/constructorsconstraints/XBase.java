package pl.selvin.apt.constructorsconstraints;

import pl.selvin.apt.constructorsconstraints.annotations.ConstructorConstraint;

@ConstructorConstraint(arguments = {int.class})
public abstract class XBase {
	public XBase(int ok) {
	}
}