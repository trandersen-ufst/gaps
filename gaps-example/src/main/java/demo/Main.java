package demo;

import gaps.Gaps;

@Gaps // This triggers the generation of MainGaps.
public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println(MainGaps.GIT_AUTHOR_NAME);
        System.out.println(MainGaps.GIT_AUTHOR_EMAIL);
        System.out.println(MainGaps.GIT_AUTHOR_DATE);
        System.out.println(MainGaps.GIT_MESSAGE);
        MainGaps.GIT_REMOTES.forEach(
                (k, v) -> System.out.println(k + " -> " + v)
        );
    }
}
