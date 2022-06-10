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
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Field;
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

                    // https://stackoverflow.com/q/25192381/18619318 comment
                    URL resource = this.getClass().getClassLoader().getResource(".");
                    String resourceFileString = resource.getFile();

                    Path somewhereInGitRepository = Paths.get(resource.toURI());

                    System.err.println("resource=" + resource + ", resourceFileString=" + resourceFileString + ", somewhereInGitCheckout=" + somewhereInGitRepository);

                    Repository repository = new FileRepositoryBuilder()
                            .readEnvironment() // scan environment GIT_* variables
                            .findGitDir(somewhereInGitRepository.toFile()) // scan up the file system tree
                            .build();

                    ObjectId head = repository.resolve(Constants.HEAD);
                    System.err.println("head=" + head);
                    RevWalk revWalk = new RevWalk(repository);
                    RevCommit revCommit = revWalk.parseCommit(head);
                    System.err.println("revCommit=" + revCommit);
                    System.err.println("GIT_COMMITTER_DATE=" + new Date(revCommit.getCommitTime() * 1000L));
                    System.err.println("authorIdent=" + revCommit.getAuthorIdent());
                    System.err.println("GIT_AUTHOR_NAME=" + revCommit.getAuthorIdent().getName()); // protect all non-ascii characters, and quotes.
                    System.err.println("GIT_AUTHOR_DATE=" + revCommit.getAuthorIdent().getWhen()); //.getTime());
                    System.err.println("GIT_AUTHOR_EMAIL=" + revCommit.getAuthorIdent().getEmailAddress());// protect all non-ascii characters, and quotes.
                    System.err.println(revCommit.getAuthorIdent().toExternalString());
                    System.err.println("GIT_MESSAGE=" + revCommit.getFullMessage()); // protect all non-ascii characters, and quotes.

                    System.err.println("GitDir=" + repository.getWorkTree());
                    System.err.println("GIT_BRANCH=" + repository.getBranch());



                    // https://github.com/square/javapoet

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
                            .initializer("$S", repository.getBranch())
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
                            .build();

                    try(Writer builderWriter = builderFile.openWriter()) {
                        javaFile.writeTo(builderWriter);
                    }
                    try(var ensureRepositoryClosed = repository) {
                        // --
                    }
                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }
}
