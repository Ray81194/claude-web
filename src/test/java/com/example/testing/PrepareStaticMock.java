package com.example.testing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies which classes should have their static methods mocked by {@link StaticMockExtension}.
 *
 * <p>Usage:
 * <pre>{@code
 * @ExtendWith(StaticMockExtension.class)
 * @PrepareStaticMock({Utils.class, Config.class})
 * class MyServiceTest {
 *
 *     @Test
 *     void test() {
 *         staticMock(Utils.class).when(Utils::getEnv).thenReturn("test");
 *     }
 * }
 * }</pre>
 *
 * <p>This annotation is inherited, so placing it on a base class will apply to all subclasses.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface PrepareStaticMock {

    /**
     * The classes whose static methods should be mocked.
     */
    Class<?>[] value();
}
