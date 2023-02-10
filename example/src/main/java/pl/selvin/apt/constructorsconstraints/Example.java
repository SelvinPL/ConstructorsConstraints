package pl.selvin.apt.constructorsconstraints;

import pl.selvin.apt.constructorsconstraints.annotations.ConstructorConstraint;

public class Example {
	public static  void main (String[] args ) {
		System.out.println("Hello world!");
	}

	@ConstructorConstraint(arguments = { int.class })
	public static abstract class Base {
		public Base(int ok) {

		}
	}

	public static class DerivedOk extends Base {
		public DerivedOk(int ok) {
			super(ok);

		}
	}

	public static class DerivedBad extends Base {
		public DerivedBad(String bad) {
			super(0);
		}
	}
}
