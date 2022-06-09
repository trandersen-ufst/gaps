package demo;

import gaps.Gaps;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.util.Date;

@Gaps
public class Main {

    public static void main(String[] args) throws Exception {
        //Repository repository = builder.setGitDir(new File("/Users/ravn/git/gaps/src/main/java/demo"))
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(System.getProperty("user.home"),"git/gaps/.git"))
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();
        System.out.println("GIT_BRANCH=" + repository.getBranch());
        System.out.println(repository.getFullBranch());
        StoredConfig config = repository.getConfig();
        System.out.println(config);
        File here = repository.getWorkTree();
        System.out.println(here);
        ObjectId head = repository.resolve(Constants.HEAD);
        System.out.println(head);
        RevWalk revWalk = new RevWalk(repository);
        RevCommit revCommit = revWalk.parseCommit(head);
        System.out.println(revCommit);
        System.out.println(new Date(revCommit.getCommitTime() * 1000L));
        System.out.println(revCommit.getAuthorIdent());
        System.out.println("GIT_AUTHOR_NAME=" + revCommit.getAuthorIdent().getName()); // protect all non-ascii characters, and quotes.
        System.out.println("GIT_AUTHOR_DATE=" + revCommit.getAuthorIdent().getWhen()); //.getTime());
        System.out.println("GIT_AUTHOR_EMAIL=" + revCommit.getAuthorIdent().getEmailAddress());// protect all non-ascii characters, and quotes.
        System.out.println(revCommit.getAuthorIdent().toExternalString());
        System.out.println("GIT_MESSAGE=" + revCommit.getFullMessage()); // protect all non-ascii characters, and quotes.
        System.out.println(revCommit.getName());
        System.out.println(revCommit);
        revWalk.dispose();
    }

}
