# gaps

Git information As Plain Source

WORK IN PROGRESS:  "mvn clean package" triggers annotation processor.  Main reads git information.  No real work is done yet.  Need to find out how to create a source file related to the class having the annotation, 
as a generated source file properly placed in the Maven source tree.  Consider looking at how Dagger2 does it.

We want to know:

* Current branch name (or sha1 if detached).
* Last commit
  * Who (full name GIT_AUTHOR_NAME, email GIT_AUTHOR_EMAIL)
  * When (GIT_AUTHOR_DATE)
  * What (message)

(from https://git-scm.com/book/en/v2/Git-Internals-Environment-Variables)

Anything else needs to be on a "are you sure this is needed?" basis.

Links:

* <https://cloudogu.com/en/blog/Java-Annotation-Processors_1-Intro>
* <https://github.com/pellaton/spring-configuration-validation-processor>