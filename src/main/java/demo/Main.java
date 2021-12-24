package demo;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.util.Date;

public class Main {

    public static void main(String args[]) throws Exception {
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        //Repository repository = builder.setGitDir(new File("/Users/ravn/git/gaps/src/main/java/demo"))
        Repository repository = builder.setGitDir(new File("/Users/ravn/git/gaps/.git"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();
        System.out.println(repository.getBranch());
        var here = repository.getWorkTree();
        System.out.println(here);
        var head = repository.resolve(Constants.HEAD);
        System.out.println(head);
        var revWalk = new RevWalk(repository);
        RevCommit revCommit = revWalk.parseCommit(head);
        System.out.println(revCommit);
        System.out.println(new Date(revCommit.getCommitTime() * 1000l));
        System.out.println(revCommit.getAuthorIdent().getName());
        System.out.println(revCommit.getFullMessage());
        System.out.println(revCommit.getName());
        System.out.println(revCommit);
        revWalk.dispose();
    }

}
