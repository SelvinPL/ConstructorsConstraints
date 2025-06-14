package pl.selvin.apt.constructorsconstraints.processor;

import com.sun.tools.javac.code.Attribute;

import java.util.ArrayList;
import java.util.HashMap;
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
import javax.lang.model.util.SimpleTypeVisitor8;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import pl.selvin.apt.constructorsconstraints.annotations.ConstructorConstraint;

@SupportedAnnotationTypes("pl.selvin.apt.constructorsconstraints.annotations.ConstructorConstraint")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ConstructorConstraintProcessor extends AbstractProcessor {
	@SuppressWarnings("Convert2Diamond")
	private static final TypeVisitor<Pair<Boolean, String>, Pair<Types, ArrayList<TypeMirror>>> constraintArgsVisitor =
			new SimpleTypeVisitor8<Pair<Boolean, String>, Pair<Types, ArrayList<TypeMirror>>>() {
				public Pair<Boolean, String> visitExecutable(ExecutableType t, Pair<Types, ArrayList<TypeMirror>> args) {
					final ArrayList<TypeMirror> argTypes = args.second;
					final Types typeUtils = args.first;
					final List<? extends TypeMirror> types = t.getParameterTypes();
					if (argTypes.size() != types.size()) {
						return new Pair<>(false, typeMirrorListToString(types));
					}
					for (int i = 0; i < argTypes.size(); i++) {
						if (!typeUtils.isAssignable(types.get(i), argTypes.get(i))) {
							return new Pair<>(false, typeMirrorListToString(types));
						}
					}
					return new Pair<>(true, null);
				}
			};

	private static String typeMirrorListToString(final List<? extends TypeMirror> types) {
		if(types == null || types.isEmpty()) {
			return "(empty)";
		}
		final StringBuilder stringBuilder = new StringBuilder();
		boolean addComma = false;
		for (TypeMirror type : types) {
			if (addComma) {
				stringBuilder.append(", ");
			} else {
				addComma = true;
			}
			stringBuilder.append(type.toString());
		}
		return stringBuilder.toString();
	}

	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
		for (final TypeElement type : annotations) {
			processConstructorConstraintClasses(env, type);
		}
		return true;
	}

	private void processConstructorConstraintClasses(final RoundEnvironment env, final TypeElement type) {
		final Element constructorConstraintElement = processingEnv.getElementUtils().getTypeElement(ConstructorConstraint.class.getName());
		final TypeMirror constructorConstraintType = constructorConstraintElement.asType();
		final HashMap<String, ArrayList<TypeMirror>> constructorConstraints = new HashMap<>();
		final ArrayList<Element> elements = new ArrayList<>();
		for (final Element element : env.getElementsAnnotatedWith(type)) {
			elements.add(element);
			for (AnnotationMirror am : element.getAnnotationMirrors()) {
				if (am.getAnnotationType().equals(constructorConstraintType)) {
					for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
						if ("arguments".equals(entry.getKey().getSimpleName().toString()) && entry.getValue() instanceof Attribute.Array) {
							//noinspection PatternVariableCanBeUsed
							final Attribute.Array array = (Attribute.Array) entry.getValue();
							for (final Attribute a : array.values) {
								final String className = element.toString();
								final ArrayList<TypeMirror> arguments;
								if (constructorConstraints.containsKey(className)) {
									arguments = constructorConstraints.get(className);
								} else {
									arguments = new ArrayList<>();
									constructorConstraints.put(className, arguments);
								}
								arguments.add((TypeMirror) a.getValue());
							}
						}
					}
					break;
				}
			}
		}
		for (Element element : elements) {
			final TypeMirror derived = element.asType();
			for (String className : constructorConstraints.keySet()) {
				final TypeMirror baseType = processingEnv.getElementUtils().getTypeElement(className).asType();
				if (derived.equals(baseType)) {
					continue;
				}
				final Types typeUtils = processingEnv.getTypeUtils();
				if (typeUtils.isAssignable(derived, baseType)) {
					processClass(element, new Pair<>(typeUtils, constructorConstraints.get(className)));
				}
			}
		}
	}

	private void processClass(Element element, Pair<Types, ArrayList<TypeMirror>> arguments) {
		final Pair<Boolean, String> result = doesClassContainConstructorWithConstraint(element, arguments);
		if (!result.first) {
			final String needs;
			if (arguments.second == null || arguments.second.isEmpty()) {
				needs = "a no-args constructor";
			} else {
				needs = "a constructor with arguments: (" + typeMirrorListToString(arguments.second) + ")\nLast found: " + result.second;
			}
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Class " + element + " needs " + needs, element);
		}
	}

	private Pair<Boolean, String> doesClassContainConstructorWithConstraint(Element element, Pair<Types, ArrayList<TypeMirror>> arguments) {
		Pair<Boolean, String> result = new Pair<>(false, null);
		for (final Element subElement : element.getEnclosedElements()) {
			if (subElement.getKind() == ElementKind.CONSTRUCTOR && subElement.getModifiers().contains(Modifier.PUBLIC)) {
				final TypeMirror mirror = subElement.asType();
				result = mirror.accept(constraintArgsVisitor, arguments);
				if (result.first)
					return result;
			}
		}
		return result;
	}

	@SuppressWarnings("ClassCanBeRecord")
	private static final class Pair<T1, T2> {
		public final T1 first;
		public final T2 second;

		public Pair(T1 first, T2 second) {
			this.first = first;
			this.second = second;
		}
	}
}