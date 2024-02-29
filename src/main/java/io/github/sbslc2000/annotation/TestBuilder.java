package io.github.sbslc2000.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 이 애노테이션은 빌더 클래스를 자동으로 생성하기 위해 사용됩니다.
 * This annotation is used to generate a builder class for the annotated class.
 *
 * 생성된 빌더 클래스는 주석이 달린 클래스와 동일한 필드를 가질 것입니다.
 * The generated builder class will have the same fields as the annotated class.
 *
 * 빌더는 내부적으로 리플렉션을 사용하여 값을 조작합니다.
 * The builder will manipulate the values using reflection internally.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({java.lang.annotation.ElementType.TYPE})
public @interface TestBuilder {
}
