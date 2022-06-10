package gaps;

import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Set;

@SupportedAnnotationTypes("gaps.Gaps")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
@com.google.auto.service.AutoService(Processor.class)
public class GapsProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element element : elements) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, ">>> found @Gaps at " + element);
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

                    // Notify in Maven log.
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, ">>> writing " + fullGapsClassName);

                    JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(fullGapsClassName);

                    // We need _some_ way to locate the git repository in the file system.
                    // The annotation processor API hides this very well, so we just hope
                    // that the current classloader points somewhere inside the repository.
                    // This was found in https://stackoverflow.com/q/25192381/18619318 comment

                    URL resource = this.getClass().getClassLoader().getResource(".");

                    Path somewhereInGitRepository = Paths.get(resource.toURI());

                    Repository repository = new FileRepositoryBuilder()
                            .findGitDir(somewhereInGitRepository.toFile()) // scan up the file system tree
                            .build();

                    // Found by experimentation.
                    ObjectId head = repository.resolve(Constants.HEAD);
                    RevWalk revWalk = new RevWalk(repository);
                    RevCommit revCommit = revWalk.parseCommit(head);

                    // Generate Java source using https://github.com/square/javapoet

                    FieldSpec gitBranchField = FieldSpec.builder(String.class, "GIT_BRANCH")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .initializer("$S", repository.getBranch())
                            .build();

                    FieldSpec gitCommitterDate = FieldSpec.builder(java.util.Date.class, "GIT_COMMITTER_DATE")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .addJavadoc("$L", new Date(revCommit.getCommitTime() * 1000L))
                            .initializer("new Date($L * 1000L)", revCommit.getCommitTime())
                            .build();

                    FieldSpec gitAuthorNameField = FieldSpec.builder(String.class, "GIT_AUTHOR_NAME")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .initializer("$S", revCommit.getAuthorIdent().getName())
                            .build();

                    FieldSpec gitAuthorDateField = FieldSpec.builder(String.class, "GIT_AUTHOR_DATE")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .initializer("$S", revCommit.getAuthorIdent().getWhen())
                            .build();

                    FieldSpec gitAuthorEmailField = FieldSpec.builder(String.class, "GIT_AUTHOR_EMAIL")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .initializer("$S", revCommit.getAuthorIdent().getEmailAddress())
                            .build();

                    FieldSpec gitFullMessageField = FieldSpec.builder(String.class, "GIT_MESSAGE")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .initializer("$S", revCommit.getFullMessage())
                            .build();

                    TypeSpec helloWorld = TypeSpec.classBuilder(gapsClassName)
                            .addModifiers(Modifier.PUBLIC)
                            .addField(gitBranchField)
                            .addField(gitCommitterDate)
                            .addField(gitAuthorNameField)
                            .addField(gitAuthorDateField)
                            .addField(gitAuthorEmailField)
                            .addField(gitFullMessageField)
                            .build();

                    JavaFile javaFile = JavaFile.builder(packageName, helloWorld)
                            .addFileComment("$L", "Automatically generated.  Do not edit!")
                            .build();

                    try (var ensureRepositoryClosed = repository;
                         Writer builderWriter = builderFile.openWriter()) {
                        javaFile.writeTo(builderWriter);
                    }
                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException(e); // just fail the build.
                }
            }
        }
        return true;
    }
}
