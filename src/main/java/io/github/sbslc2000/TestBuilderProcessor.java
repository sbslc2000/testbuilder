package io.github.sbslc2000;

import com.google.auto.service.AutoService;
import io.github.sbslc2000.annotation.TestBuilder;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import java.util.Set;

@AutoService(Processor.class)
class TestBuilderProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(TestBuilder.class.getName());
    }
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(TestBuilder.class);

        for (Element element : annotatedElements) {
            // Check if the annotated element is a class or an interface
            if (element.getKind() == ElementKind.INTERFACE) {
                // If it is an interface, print an error message
                processingEnv.getMessager().printMessage(Kind.ERROR, "TestBuilder annotation can not be used on "+ element.getSimpleName());
            } else {
                TestBuilderCodeGenerator.generateBuilderClass(element, processingEnv);
            }
        }
        return true;
    }
}
