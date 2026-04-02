package com.example.testing;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JUnit 5 Extension that automates static mocking lifecycle using Mockito's {@link MockedStatic}.
 *
 * <p>Annotate your test class with {@link PrepareStaticMock} to declare which classes should be
 * statically mocked, then use {@link #staticMock(Class)} inside test methods to access the mock:
 *
 * <pre>{@code
 * @ExtendWith(StaticMockExtension.class)
 * @PrepareStaticMock({Utils.class, Config.class})
 * class MyServiceTest {
 *
 *     @Test
 *     void test1() {
 *         staticMock(Utils.class).when(Utils::getEnv).thenReturn("test");
 *     }
 *
 *     // Alternatively, inject MockedStatic directly as a parameter
 *     @Test
 *     void test2(MockedStatic<Utils> utils) {
 *         utils.when(Utils::getEnv).thenReturn("staging");
 *     }
 * }
 * }</pre>
 *
 * <p>The extension handles opening and closing of each {@link MockedStatic} automatically —
 * no try-with-resources needed.
 *
 * <p>Annotations are collected from the entire class hierarchy, so a base class annotated with
 * {@code @PrepareStaticMock} will apply to all subclasses.
 */
public class StaticMockExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    /**
     * Namespace used to store mocks in the JUnit extension context store.
     */
    private static final ExtensionContext.Namespace NAMESPACE =
            ExtensionContext.Namespace.create(StaticMockExtension.class);

    private static final String MOCKS_KEY = "staticMocks";

    /**
     * ThreadLocal holding the active mocks for the current test, keyed by target class.
     * Populated in {@link #beforeEach} and cleared in {@link #afterEach}.
     */
    private static final ThreadLocal<Map<Class<?>, MockedStatic<?>>> THREAD_LOCAL_MOCKS =
            new ThreadLocal<>();

    // -------------------------------------------------------------------------
    // BeforeEachCallback
    // -------------------------------------------------------------------------

    @Override
    public void beforeEach(ExtensionContext context) {
        List<Class<?>> targetClasses = collectTargetClasses(context.getRequiredTestClass());

        Map<Class<?>, MockedStatic<?>> mocks = new LinkedHashMap<>();
        for (Class<?> targetClass : targetClasses) {
            mocks.put(targetClass, Mockito.mockStatic(targetClass));
        }

        // Store in both the extension context store (for cleanup) and the ThreadLocal (for access).
        context.getStore(NAMESPACE).put(MOCKS_KEY, mocks);
        THREAD_LOCAL_MOCKS.set(mocks);
    }

    // -------------------------------------------------------------------------
    // AfterEachCallback
    // -------------------------------------------------------------------------

    @Override
    public void afterEach(ExtensionContext context) {
        @SuppressWarnings("unchecked")
        Map<Class<?>, MockedStatic<?>> mocks =
                (Map<Class<?>, MockedStatic<?>>) context.getStore(NAMESPACE).remove(MOCKS_KEY);

        if (mocks != null) {
            // Close in reverse order to mirror natural resource-release semantics.
            List<MockedStatic<?>> list = new ArrayList<>(mocks.values());
            for (int i = list.size() - 1; i >= 0; i--) {
                list.get(i).close();
            }
        }

        THREAD_LOCAL_MOCKS.remove();
    }

    // -------------------------------------------------------------------------
    // ParameterResolver — inject MockedStatic<T> as a test-method parameter
    // -------------------------------------------------------------------------

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext) {
        return resolveTargetClass(parameterContext) != null;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) {
        Class<?> targetClass = resolveTargetClass(parameterContext);
        if (targetClass == null) {
            throw new ParameterResolutionException(
                    "Cannot resolve MockedStatic parameter: " + parameterContext.getParameter());
        }

        Map<Class<?>, MockedStatic<?>> mocks = currentMocks();
        MockedStatic<?> mock = mocks.get(targetClass);
        if (mock == null) {
            throw new ParameterResolutionException(
                    "MockedStatic<" + targetClass.getSimpleName() + "> was requested as a parameter "
                    + "but " + targetClass.getName() + " is not listed in @PrepareStaticMock. "
                    + "Add it to @PrepareStaticMock on your test class.");
        }
        return mock;
    }

    /**
     * Extracts the generic type argument {@code T} from a {@code MockedStatic<T>} parameter,
     * or returns {@code null} if the parameter is not of that type.
     */
    private static Class<?> resolveTargetClass(ParameterContext parameterContext) {
        Type paramType = parameterContext.getParameter().getParameterizedType();
        if (!(paramType instanceof ParameterizedType)) {
            return null;
        }
        ParameterizedType pt = (ParameterizedType) paramType;
        if (!MockedStatic.class.equals(pt.getRawType())) {
            return null;
        }
        Type[] args = pt.getActualTypeArguments();
        if (args.length != 1 || !(args[0] instanceof Class)) {
            return null;
        }
        return (Class<?>) args[0];
    }

    // -------------------------------------------------------------------------
    // Static helper API
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link MockedStatic} instance for the given class within the current test.
     *
     * <p>Must be called from a test method after the extension has set up the mocks (i.e. after
     * {@code beforeEach}). The class must be declared in {@code @PrepareStaticMock}.
     *
     * @param <T>         the class type
     * @param targetClass the class whose static mock is requested
     * @return the active {@link MockedStatic} instance
     * @throws IllegalStateException if called outside of a test managed by this extension
     * @throws IllegalArgumentException if {@code targetClass} is not in {@code @PrepareStaticMock}
     */
    @SuppressWarnings("unchecked")
    public static <T> MockedStatic<T> staticMock(Class<T> targetClass) {
        Map<Class<?>, MockedStatic<?>> mocks = THREAD_LOCAL_MOCKS.get();
        if (mocks == null) {
            throw new IllegalStateException(
                    "staticMock() was called outside of a test managed by StaticMockExtension. "
                    + "Ensure your test class is annotated with @ExtendWith(StaticMockExtension.class).");
        }
        MockedStatic<?> mock = mocks.get(targetClass);
        if (mock == null) {
            throw new IllegalArgumentException(
                    "No static mock found for " + targetClass.getName() + ". "
                    + "Add it to @PrepareStaticMock on your test class (or its superclass).");
        }
        return (MockedStatic<T>) mock;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Walks the class hierarchy of {@code testClass} (including the class itself) and collects
     * all classes declared in {@link PrepareStaticMock} annotations, preserving declaration order
     * and deduplicating.
     */
    private static List<Class<?>> collectTargetClasses(Class<?> testClass) {
        // Use LinkedHashMap as an ordered set.
        Map<Class<?>, Void> seen = new LinkedHashMap<>();

        // Walk from the most-derived class up to Object, giving priority to the subclass.
        Class<?> current = testClass;
        while (current != null && !current.equals(Object.class)) {
            PrepareStaticMock annotation = current.getDeclaredAnnotation(PrepareStaticMock.class);
            if (annotation != null) {
                for (Class<?> cls : annotation.value()) {
                    seen.putIfAbsent(cls, null);
                }
            }
            current = current.getSuperclass();
        }

        return new ArrayList<>(seen.keySet());
    }

    private static Map<Class<?>, MockedStatic<?>> currentMocks() {
        Map<Class<?>, MockedStatic<?>> mocks = THREAD_LOCAL_MOCKS.get();
        if (mocks == null) {
            throw new IllegalStateException(
                    "No active static mocks found. "
                    + "Ensure the test is running under StaticMockExtension.");
        }
        return mocks;
    }
}
