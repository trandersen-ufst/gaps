package gaps;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Set;

@SupportedAnnotationTypes("gaps.Gaps")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@com.google.auto.service.AutoService(Processor.class)
public class GapsProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : elements) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, ">>> found @Gaps at " + element);
                // FIXME:  Generate source and write to disk.

                try {
                    // The Baeldung tutorial did it this way.
                    String fullClassName = ((TypeElement) element).getQualifiedName().toString();

                    int lastDotInClassName = fullClassName.lastIndexOf('.');

                    String packageName;
                    String className;

                    if (lastDotInClassName > -1) {
                        packageName = fullClassName.substring(0, lastDotInClassName);
                        className = fullClassName.substring(lastDotInClassName + 1);
                    } else {
                        packageName = null;
                        className = fullClassName;
                    }

                    String fullGapsClassName = fullClassName + "Gaps";
                    String gapsClassName = className + "Gaps";
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, ">>> writing " + fullGapsClassName);

                    JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(fullGapsClassName); // JavacFiler$FilerOutputJavaFileObject

                    System.err.println("builderFile = " + builderFile + ", " + builderFile.getClass());

                    // get private field in builderFile to see location on disk.
                    Field field = builderFile.getClass().getDeclaredField("javaFileObject");
                    field.setAccessible(true);
                    Object fileObject = field.get(builderFile);
                    System.err.println("fileObject -> " + fileObject);

                    Writer builderWriter = builderFile.openWriter();

                    try(PrintWriter out = new PrintWriter(builderWriter)) {
                        if (packageName != null) {
                            out.println("package " + packageName + ";");
                        }
                        out.println("public class " + gapsClassName + " { // " + new Date());
                        out.println("};");
                    }
                } catch (IOException | NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }
}
