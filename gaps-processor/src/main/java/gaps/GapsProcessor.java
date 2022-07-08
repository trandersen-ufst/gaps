package gaps;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.ParameterizedTypeName;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
                    // The Baeldung tutorial did it roughly this way.  Can probably be much nicer.
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
                    // The annotation processor API hides these details _very_ well, so we just hope
                    // that the current classloader now that we are compiling to a target-folder inside the repository,
                    // actually _points_ to somewhere inside the repository. The proper way is still to get metadata for
                    // the class being compiled, but could not find a way to do so.
                    // This was found in https://stackoverflow.com/q/25192381/18619318 comment

                    URL resource = this.getClass().getClassLoader().getResource(".");

                    Path somewhereInGitRepository = Paths.get(resource.toURI());

                    Repository repository = new FileRepositoryBuilder()
                            .findGitDir(somewhereInGitRepository.toFile()) // scans towards the root of the file system
                            .build();

                    // FIXME:  Found by experimentation.  Rather slow.  Perhaps the information can be found faster?

                    ObjectId head = repository.resolve(Constants.HEAD);
                    RevWalk revWalk = new RevWalk(repository);
                    RevCommit revCommit = revWalk.parseCommit(head);

                    var sha1 = revCommit.getName();

                    // -- Generate Java source using https://github.com/square/javapoet

                    // We need to use the builder for the constants to be able to set their initializers.  This code is still
                    // rather naive.  There might be better ways to express this.

                    // $S means quoted string variable, $L means literal value.

                    FieldSpec gitBranchField = FieldSpec.builder(String.class, "GIT_BRANCH")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .initializer("$S", repository.getBranch())
                            .addJavadoc("branch name if HEAD is on branch, sha1 if detached HEAD")
                            .build();

                    FieldSpec gitSha1Field = FieldSpec.builder(String.class, "GIT_SHA1")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                            .initializer("$S", sha1)
                            .addJavadoc("SHA1 of commit, if available.  Can be used to uniquely identify commit.")
                            .build();

                    FieldSpec gitCommitterDate = FieldSpec.builder(java.util.Date.class, "GIT_COMMITTER_DATE")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                            .addJavadoc("$L", new Date(revCommit.getCommitTime() * 1000L))
                            .initializer("new Date($L * 1000L)", revCommit.getCommitTime())
                            .build();

                    FieldSpec gitAuthorNameField = FieldSpec.builder(String.class, "GIT_AUTHOR_NAME")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                            .initializer("$S", revCommit.getAuthorIdent().getName())
                            .addJavadoc("Full name of commit author.")
                            .build();

                    FieldSpec gitAuthorDateField = FieldSpec.builder(String.class, "GIT_AUTHOR_DATE")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                            .initializer("$S", revCommit.getAuthorIdent().getWhen())
                            .build();

                    FieldSpec gitAuthorEmailField = FieldSpec.builder(String.class, "GIT_AUTHOR_EMAIL")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                            .initializer("$S", revCommit.getAuthorIdent().getEmailAddress())
                            .build();

                    FieldSpec gitFullMessageField = FieldSpec.builder(String.class, "GIT_MESSAGE")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                            .initializer("$S", revCommit.getFullMessage())
                            .addJavadoc("Commit message from $L", sha1)
                            .build();

                    Set<String> remoteNames = repository.getRemoteNames();
                    var config = repository.getConfig();

                    // generate the _inside_ of the Map.of("..."), by flattening (name,url) pairs.
                    // get URL for remote name: https://stackoverflow.com/a/38062680/18619318
                    var quote = "\"";
                    var remotesCodeBlock = "Map.of(\n" + quote + remoteNames.stream()
                            .limit(5) // Map.of(...) API has limit 5.
                            .flatMap(
                                    name -> List.of(name, config.getString("remote", name, "url")).stream()
                            )
                            .collect(Collectors.joining(quote + ",\n" + quote))
                            + quote + ")";

                    FieldSpec gitRemoteField = FieldSpec.builder(ParameterizedTypeName.get(
                                    ClassName.get("java.util", "Map"),
                                    ClassName.get("java.lang", "String"),
                                    ClassName.get("java.lang", "String")), "GIT_REMOTES")
                            .addModifiers(Modifier.PUBLIC, Modifier.FINAL, Modifier.STATIC)
                            .initializer("$L", remotesCodeBlock)
                            .addJavadoc("Remote names:  $S", remoteNames)
                            .build();

                    TypeSpec gapsTypeSpec = TypeSpec.classBuilder(gapsClassName)
                            .addModifiers(Modifier.PUBLIC)
                            .addJavadoc("Automatically generated at compile time from git.")
                            .addField(gitBranchField)
                            .addField(gitSha1Field)
                            .addField(gitCommitterDate)
                            .addField(gitAuthorNameField)
                            .addField(gitAuthorDateField)
                            .addField(gitAuthorEmailField)
                            .addField(gitFullMessageField)
                            .addField(gitRemoteField)
                            .build();

                    JavaFile javaFile = JavaFile.builder(packageName, gapsTypeSpec)
                            .addFileComment("$L", "Automatically generated.  Do not edit!  Use UTF-8 encoding for sources.")
                            .skipJavaLangImports(true)
                            .build();

                    try (Writer builderWriter = builderFile.openWriter()) {
                        javaFile.writeTo(builderWriter);
                        revWalk.dispose();  // appears to be very necessary.
                    }
                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException(e); // just fail the build if anything goes wrong.
                }
            }
        }
        return true;
    }
}
