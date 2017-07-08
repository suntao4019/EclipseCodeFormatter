package krasa.formatter.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.codeStyle.CodeStyleManager;

import net.sf.cglib.proxy.InvocationHandler;

public class CodeStyleManagerDelegator implements InvocationHandler {
	private static final Logger log = Logger.getInstance(CodeStyleManagerDelegator.class.getName());

	private final CodeStyleManager delegatedObject;
	private final EclipseCodeStyleManager overridingObject;

	public <T> CodeStyleManagerDelegator(CodeStyleManager delegatedObject, EclipseCodeStyleManager overridingObject) {
		this.delegatedObject = delegatedObject;
		this.overridingObject = overridingObject;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] rawArguments) throws Throwable {
		if (!overridingObject.isEnabled()) {
			return PLEASE_REPORT_BUGS_TO_JETBRAINS_IF_IT_FAILS_HERE____ORIGINAL_INTELLIJ_FORMATTER_WAS_USED(method, rawArguments);
		} else {
			try {
				Method overridingMethod = getOverridingMethod(method);

				if (!compatibleReturnTypes(method.getReturnType(), overridingMethod.getReturnType())) {
					overridingMethodHasWrongReturnType(method, overridingMethod, proxy, overridingObject);
					return PLEASE_REPORT_BUGS_TO_JETBRAINS_IF_IT_FAILS_HERE____ORIGINAL_INTELLIJ_FORMATTER_WAS_USED(method, rawArguments);
				}
				if (log.isDebugEnabled()) {
					log.debug("invoking overriding {}({})", method.getName(), Arrays.toString(rawArguments));
				}
				return overridingMethod.invoke(overridingObject, rawArguments);
			} catch (NoSuchMethodException e) {
				if (log.isDebugEnabled()) {
					log.debug("invoking original {}({})", method.getName(), Arrays.toString(rawArguments));
				}

				return PLEASE_REPORT_BUGS_TO_JETBRAINS_IF_IT_FAILS_HERE____ORIGINAL_INTELLIJ_FORMATTER_WAS_USED(method, rawArguments);
			} catch (InvocationTargetException e) {
				// propagate the original exception from the delegate
				throw e.getCause();
			}
		}
	}

	private Object PLEASE_REPORT_BUGS_TO_JETBRAINS_IF_IT_FAILS_HERE____ORIGINAL_INTELLIJ_FORMATTER_WAS_USED(Method invokedMethod, Object[] rawArguments)
			throws Throwable {
		try {
			return invokedMethod.invoke(delegatedObject, rawArguments);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	public void overridingMethodHasWrongReturnType(Method mockMethod, Method overridingMethod, Object mock, Object overridingObject) {
		log.error("IntelliJ API changed, install proper/updated version of Eclipse Formatter plugin. " + "Incompatible return types when calling: " + mockMethod
				+ " on: " + mock.getClass().getSimpleName() + " return type should be: " + mockMethod.getReturnType().getSimpleName() + ", but was: "
				+ overridingMethod.getReturnType().getSimpleName() + " (delegate instance had type: " + overridingObject.getClass().getSimpleName() + ")");
	}

	private Method getOverridingMethod(Method mockMethod) throws NoSuchMethodException {
		return overridingObject.getClass().getMethod(mockMethod.getName(), mockMethod.getParameterTypes());
	}

	private static boolean compatibleReturnTypes(Class<?> superType, Class<?> subType) {
		return superType.equals(subType) || superType.isAssignableFrom(subType);
	}

}
