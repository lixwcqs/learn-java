package org.wcong.test.autovalue;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.Generated;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * process for MyAutoValue
 * Created by wcong on 2016/11/25.
 */
public class MyAutoValueProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotationTypes = new HashSet<>();
        annotationTypes.add(MyAutoValue.class.getName());
        return annotationTypes;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(MyAutoValue.class);
        if (elements == null || elements.isEmpty()) {
            return false;
        }
        for (Element element : elements) {
            if (!(element instanceof TypeElement)) {
                continue;
            }
            try {
                processType(element);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }
        return false;
    }

    private void processType(Element element) {
        TypeElement typeElement = (TypeElement) element;
        String className = element.getSimpleName() + "_MyAutoValue";
        TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(className);
        typeSpecBuilder.addAnnotation(makeAnnotationSpec());
        String packageName = getPackageName(typeElement);
        try {
            makeFields(typeElement, typeSpecBuilder);
        } catch (ClassNotFoundException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        JavaFile.Builder javaFileBuilder = JavaFile.builder(packageName, typeSpecBuilder.build());
        String text = javaFileBuilder.build().toString();
        try {
            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(className, element);
            Writer writer = sourceFile.openWriter();
            try {
                writer.write(text);
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
    }

    private String getPackageName(Element typeElement) {
        while (true) {
            Element enclosingElement = typeElement.getEnclosingElement();
            if (enclosingElement instanceof PackageElement) {
                return ((PackageElement) enclosingElement).getQualifiedName().toString();
            }
            typeElement = enclosingElement;
        }
    }

    private void makeFields(Element element, TypeSpec.Builder typeSpecBuilder) throws ClassNotFoundException {
        List<VariableElement> elementList = ElementFilter.fieldsIn(element.getEnclosedElements());
        if (elementList == null || elementList.isEmpty()) {
            return;
        }
        for (VariableElement variableElement : elementList) {
            String fieldName = variableElement.getSimpleName().toString();
            Type type = Class.forName(variableElement.asType().toString());
            typeSpecBuilder.addField(makeFieldSpec(fieldName, type));
            typeSpecBuilder.addMethod(makeSetMethod(fieldName, type));
            typeSpecBuilder.addMethod(makeGetMethod(fieldName, type));
        }
    }

    private AnnotationSpec makeAnnotationSpec() {
        AnnotationSpec.Builder builder = AnnotationSpec.builder(Generated.class);
        CodeBlock.Builder codeBlockBuilder = CodeBlock.builder().add("$S", "MyAutoValue");
        builder.addMember("value", codeBlockBuilder.build());
        return builder.build();
    }

    private static FieldSpec makeFieldSpec(String fieldName, Type type) {
        FieldSpec.Builder fileSpecBuilder = FieldSpec.builder(type, fieldName, Modifier.PRIVATE);
        return fileSpecBuilder.build();
    }

    private MethodSpec makeSetMethod(String fieldName, Type type) {
        String upperFieldName = getUpperString(fieldName);
        MethodSpec.Builder setMethodSpecBuilder = MethodSpec.methodBuilder("set" + upperFieldName);
        setMethodSpecBuilder.addModifiers(Modifier.PUBLIC);
        setMethodSpecBuilder.returns(TypeName.VOID);
        ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(type, fieldName);
        setMethodSpecBuilder.addParameter(parameterBuilder.build());
        setMethodSpecBuilder.addCode(CodeBlock.builder().add("this." + fieldName + " = " + fieldName + ";\n").build());
        return setMethodSpecBuilder.build();
    }

    private String getUpperString(String fieldName) {
        if (fieldName.length() == 1) {
            fieldName = fieldName.toUpperCase();
        } else {
            fieldName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        }
        return fieldName;
    }

    private MethodSpec makeGetMethod(String fieldName, Type type) {
        String upperFieldName = getUpperString(fieldName);
        MethodSpec.Builder getMethodSpecBuilder = MethodSpec.methodBuilder("get" + upperFieldName);
        getMethodSpecBuilder.addModifiers(Modifier.PUBLIC);
        getMethodSpecBuilder.returns(type);
        getMethodSpecBuilder.addCode(CodeBlock.builder().add("return " + fieldName + ";\n").build());
        return getMethodSpecBuilder.build();
    }
}
