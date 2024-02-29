package io.github.sbslc2000;

import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

class TestBuilderCodeGenerator {
    private TestBuilderCodeGenerator() {
        throw new AssertionError("No instances.");
    }

    private static final ClassName builderClassName = ClassName.get("", "Builder");

    public static void generateBuilderClass(Element element, ProcessingEnvironment processingEnv) {
        TypeElement typeElement = (TypeElement) element;
        TypeName typeName = TypeName.get(typeElement.asType());
        String packageName = processingEnv.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
        String simpleTypeName = typeElement.getSimpleName().toString();

        TypeSpec innerBuilderType = generateInnerBuilderClass(typeElement, typeName, processingEnv);

        MethodSpec builderMethod = MethodSpec.methodBuilder("builder")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("return new $T()", builderClassName)
                .returns(builderClassName)
                .build();

        TypeSpec testBuilderType = TypeSpec.classBuilder("T" + simpleTypeName)
                .addModifiers(Modifier.PUBLIC)
                .addType(innerBuilderType)
                .addMethod(builderMethod)
                .build();

        processingEnv.getMessager().printMessage(Kind.NOTE, "testBuilderType: " + testBuilderType);

        try {
            JavaFile.builder("testbuilder."+packageName, testBuilderType)
                    .build()
                    .writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Kind.ERROR, e.getMessage());
        }
    }

    private static TypeSpec generateInnerBuilderClass(TypeElement typeElement, TypeName typeName, ProcessingEnvironment processingEnv) {

        FieldSpec builderObjField = FieldSpec.builder(typeName, "builderObj")
                .addModifiers(Modifier.PRIVATE)
                .build();

        MethodSpec constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .beginControlFlow("try")
                .addStatement("$T<$T> constructor = $T.class.getDeclaredConstructor()", Constructor.class, typeName, typeName)
                .addStatement("constructor.setAccessible(true)")
                .addStatement("builderObj = constructor.newInstance()")
                .nextControlFlow("catch ($T | $T | $T | $T e)", NoSuchMethodException.class, InstantiationException.class,
                        IllegalAccessException.class, InvocationTargetException.class)
                .addStatement("throw new $T(e)", RuntimeException.class)
                .endControlFlow()
                .build();

        MethodSpec setFieldMethod = MethodSpec.methodBuilder("setField")
                .addModifiers(Modifier.PRIVATE)
                .addParameter(String.class, "fieldName")
                .addParameter(Object.class, "value")
                .returns(builderClassName)
                .beginControlFlow("try")
                .addStatement("$T field = null", Field.class)
                .addStatement("$T targetClass = $T.class", Class.class, typeName)
                .beginControlFlow("while (field == null && targetClass != null)")
                .beginControlFlow("try")
                .addStatement("field = targetClass.getDeclaredField(fieldName)")
                .addStatement("field.setAccessible(true)")
                .nextControlFlow("catch ($T e)", NoSuchFieldException.class)
                .addStatement("targetClass = targetClass.getSuperclass()")
                .endControlFlow()
                .endControlFlow()
                .beginControlFlow("if (field == null)")
                .addStatement("throw new $T(\"Field not found\")", NoSuchFieldException.class)
                .endControlFlow()
                .addStatement("field.set(builderObj, value)")
                .nextControlFlow("catch ($T | $T e)", NoSuchFieldException.class, IllegalAccessException.class)
                .addStatement("throw new $T(e)", RuntimeException.class)
                .endControlFlow()
                .addStatement("return this")
                .build();

        MethodSpec buildMethod = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .addStatement("return builderObj")
                .returns(typeName)
                .build();

        List<MethodSpec> allSetters = getAllSetters(typeElement, processingEnv);

        return TypeSpec.classBuilder(builderClassName)
                .addMethod(constructor)
                .addMethod(setFieldMethod)
                .addMethod(buildMethod)
                .addField(builderObjField)
                .addMethods(allSetters)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .build();
    }


    //get All Field from TypeElement without reflection
    private static List<MethodSpec> getAllSetters(TypeElement typeElement, ProcessingEnvironment processingEnv) {
        List<Element> setterElement = new ArrayList<>();

        //상속된 요소들의 필드를 가져와 setterElement에 넣는다.
        Queue<TypeMirror> queue = new java.util.LinkedList<>();
        queue.add(typeElement.asType());

        while (!queue.isEmpty()) {
            TypeMirror typeMirror = queue.poll();
            TypeElement currentTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(typeMirror);

            for (Element element : currentTypeElement.getEnclosedElements()) {
                if (element.getKind().isField()) {
                    setterElement.add(element);
                }
            }

            TypeMirror superclass = currentTypeElement.getSuperclass();

            //superclass가 Object가 아니면 queue에 추가한다.
            if (!superclass.toString().equals(Object.class.getName()))
                queue.add(currentTypeElement.getSuperclass());
        }

        List<MethodSpec> setters = new ArrayList<>();

        for (Element element: setterElement) {
            String simpleName = element.getSimpleName().toString();
            MethodSpec setFieldMethodSpec = MethodSpec.methodBuilder(simpleName)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(TypeName.get(element.asType()), simpleName)
                    .addStatement("return setField($S, $L)", simpleName, simpleName)
                    .returns(builderClassName)
                    .build();

            setters.add(setFieldMethodSpec);
        }

        return setters;
    }
}

