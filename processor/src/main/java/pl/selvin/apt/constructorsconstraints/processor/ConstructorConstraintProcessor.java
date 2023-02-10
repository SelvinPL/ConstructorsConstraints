package pl.selvin.apt.constructorsconstraints.processor;

import com.sun.tools.javac.code.Attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.SimpleTypeVisitor7;
import javax.tools.Diagnostic;

import pl.selvin.apt.constructorsconstraints.annotations.ConstructorConstraint;

@SupportedAnnotationTypes("pl.selvin.apt.constructorsconstraints.annotations.ConstructorConstraint")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ConstructorConstraintProcessor extends AbstractProcessor {
	private static final TypeVisitor<Boolean, ArrayList<String>> constraintArgsVisitor =
			new SimpleTypeVisitor7<Boolean, ArrayList<String>>() {
				public Boolean visitExecutable(ExecutableType t, ArrayList<String> args) {
					final List<? extends TypeMirror> types = t.getParameterTypes();
					if (args.size() != types.size()) {
						return false;
					}
					for (int i = 0; i < args.size(); i++) {
						if (!args.get(i).equals(types.get(i).toString()))
							return false;
					}
					return true;
				}
			};

	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
		for (final TypeElement type : annotations) {
			processConstructorConstraintClasses(env, type);
		}
		return true;
	}

	private void processConstructorConstraintClasses(final RoundEnvironment env, final TypeElement type) {
		final Element constructorConstraintElement = processingEnv.getElementUtils().getTypeElement(ConstructorConstraint.class.getName());
		final TypeMirror constructorConstraintType = constructorConstraintElement.asType();
		final ArrayList<String> constructorConstraints = new ArrayList<>();
		for (final Element element : env.getElementsAnnotatedWith(type)) {
			for (AnnotationMirror am : element.getAnnotationMirrors()) {
				if (am.getAnnotationType().equals(constructorConstraintType)) {
					for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
						if ("arguments".equals(entry.getKey().getSimpleName().toString()) && entry.getValue() instanceof Attribute.Array) {
							final Attribute.Array array = (Attribute.Array) entry.getValue();
							for (final Attribute a : array.values) {
								constructorConstraints.add(a.getValue().toString());
							}
						}
					}
					break;
				}
			}
			processClass(element, constructorConstraints);
		}
	}

	private void processClass(Element element, ArrayList<String> arguments) {
		if (!doesClassContainConstructorWithConstraint(element, arguments)) {
			final String needs;
			if (arguments == null || arguments.size() == 0) {
				needs = "a no-args constructor";
			} else {
				needs = "a constructor with arguments: (" + String.join(", ", arguments) + ")";
			}
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Class " + element + " needs " + needs, element);
		}
	}

	private boolean doesClassContainConstructorWithConstraint(Element element, ArrayList<String> arguments) {
		for (final Element subElement : element.getEnclosedElements()) {
			if (subElement.getKind() == ElementKind.CONSTRUCTOR && subElement.getModifiers().contains(Modifier.PUBLIC)) {
				final TypeMirror mirror = subElement.asType();
				if (mirror.accept(constraintArgsVisitor, arguments))
					return true;
			}
		}
		return false;
	}
}