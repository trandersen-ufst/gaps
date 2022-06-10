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
        System.out.println(MainGaps.GIT_AUTHOR_NAME);
        System.out.println(MainGaps.GIT_AUTHOR_EMAIL);
        System.out.println(MainGaps.GIT_AUTHOR_DATE);
        System.out.println(MainGaps.GIT_MESSAGE);
    }

}
