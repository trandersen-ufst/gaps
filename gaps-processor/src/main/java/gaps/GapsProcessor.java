package gaps;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

//@SupportedAnnotationTypes("*")
// @SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GapsProcessor extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            for (var element : roundEnv.getElementsAnnotatedWith(annotation)) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "found @Gaps at " + element);
            }
        }
        return true;
    }
}
