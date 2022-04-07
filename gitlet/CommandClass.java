package gitlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import static gitlet.Utils.*;
import static gitlet.Main.exitWithError;

/** Class for Commands.
 * @author Minjune Kim
 */

public class CommandClass {

    /** Base file CWD.*/
    static final File CWD = new File(".");

    /** Where files and folders are stored.*/
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");

    /** Command init.*/
    public static void init() {

        if (GITLET_FOLDER.exists()) {
            exitWithError("A gitlet version-control system "
                   + "already exists in the current directory");
        } else {
            GITLET_FOLDER.mkdir();
            new File(GITLET_FOLDER, "stage").mkdir();
            new File(GITLET_FOLDER, "log").mkdir();
            new File(GITLET_FOLDER, "object").mkdir();
            new File(GITLET_FOLDER, "commit").mkdir();
            new File(GITLET_FOLDER, "remove").mkdir();
            Commit c = new Commit("initial commit", new HashMap<>());
            c.save();
            branch = "master";
            head = c;
            tree = new Tree(branch, c);
        }
    }

    /** Command add.
     * @param args .*/
    public static void add(String[] args) {
        if (args.length <= 1) {
            throw new Error("Incorrect operands.");
        }
        HashMap<String, String> f = head.getBlobMap();
        String[] files = new File("./").list();
        String[] removefile = new File(".gitlet/remove").list();
        String file = args[1];
        if (!Arrays.asList(files).contains(file)) {
            exitWithError("File does not exist.");
        }
        if (Arrays.asList(removefile).contains(file)) {
            File filetodelete = new File(".gitlet/remove/" + file);
            filetodelete.delete();
        }
        File current = new File("./" + args[1]);
        String currentcode = sha1(readContents(current));
        if (currentcode.equals(f.get(file))) {
            return;
        } else {
            File write = new File(".gitlet/stage/" + file);
            writeContents(write, readContents(current));
        }
    }

    /** Command commit.
     * @param args .*/
    public static void commit(String[] args) {
        if (args.length < 2 || args[1].equals("")) {
            exitWithError("Please enter a commit message.");
        }
        String message = args[1];
        String[] stage = new File(".gitlet/stage").list();
        String[] removefile = new File(".gitlet/remove").list();
        if (removefile.length == 0 && stage.length == 0) {
            exitWithError("No changes added to the commit.");
        }
        HashMap<String, String> map;
        map = new HashMap<>(head.getBlobMap());
        for (int i = 0; i < removefile.length; i++) {
            if (map.containsKey(removefile[i])) {
                map.remove(removefile[i]);
            }
            File filetodelete = new File(".gitlet/remove/"
                    + removefile[i]);
            filetodelete.delete();
        }
        for (int i = 0; i < stage.length; i++) {
            File f = new File(stage[i]);
            map.put(stage[i], sha1(readContents(f)));
            File commitfiles = new File(".gitlet/commit/"
                    + sha1(readContents(f)));
            writeContents(commitfiles, readContents(f));
            File filetodelete = new File(".gitlet/stage/"
                    + stage[i]);
            filetodelete.delete();
        }
        Commit c = new Commit(map, head, message, branch);
        c.save();
        head = c;
        tree.makeHistory(branch, c);
    }

    /** Command log.
     * @param c .*/
    public static void log(Commit c) {
        while (c != null) {
            print(c);
            c = c.getParent();
        }
    }

    /** Command checkout.
     * @param args .*/
    public static void checkout(String[] args) {
        if (args.length == 3) {
            if (!args[1].equals("--")) {
                exitWithError("Incorrect operands.");
            }
            checkoutfile(args[2]);
        } else if (args.length == 2) {
            checkoutbranches(args[1]);
        } else if (args.length == 4) {
            if (!args[2].equals("--")) {
                exitWithError("Incorrect operands.");
            }
            checkoutcommit(args[3], args[1]);
        }
    }

    /** Command checkout to checkout commit.
     * @param file .
     * @param commit .*/
    public static void checkoutcommit(String file, String commit) {
        String[] committing = new File(".gitlet/log/").list();
        boolean check = false;
        Commit c = null;
        for (String str : committing) {
            if (str.startsWith(commit)) {
                c = Commit.load(str);
                check = true;
                break;
            }
        }
        if (!check) {
            exitWithError("No commit with that id exists.");
        } else if (!c.getBlobMap().containsKey(file)) {
            exitWithError("File does not exist in that commit.");
        } else {
            File f = new File(".gitlet/commit/"
                    + c.getBlobMap().get(file));
            File current = new File("./" + file);
            writeContents(current, readContents(f));
        }
    }

    /** Command checkout to checkout file.
     * @param file .*/
    public static void checkoutfile(String file) {
        if (head.getBlobMap().containsKey(file)) {
            File f1 = new File(".gitlet/commit/"
                    + head.getBlobMap().get(file));
            File f2 = new File("./" + file);
            writeContents(f2, readContents(f1));
        } else {
            exitWithError("File does not exist in that commit.");
        }
    }

    /** Command checkout to checkout branches.
     * @param branches .*/
    public static void checkoutbranches(String branches) {
        if (branches.equals(branch)) {
            exitWithError("No need to checkout the current branch.");
        } else if (!tree.getHistory().containsKey(branches)) {
            exitWithError("No such branch exists.");
        } else {
            String[] stage = new File(".gitlet/stage").list();
            String[] files = new File("./").list();
            String[] commit = new File(".gitlet/commit").list();
            HashMap<String, String> map = head.getBlobMap();
            Commit c = Commit.load(tree.getHistory().get(branches) + ".txt");
            for (String file: files) {
                File f = new File("./" + file);
                if (c.getBlobMap().containsKey(file)
                        && !Arrays.asList(stage).contains(file)
                        && !map.containsKey(file)) {
                    exitWithError("There is an untracked file in the way; "
                           + "delete it, or add and commit it first.");
                }
                if (!f.isDirectory() && !file.equals(".gitignore")
                        && !file.equals("Makefile")
                        && !file.equals("proj3.iml")) {
                    f.delete();
                }
            }
            for (String file : commit) {
                if (c.getBlobMap().containsValue(file)) {
                    File f = new File(".gitlet/commit/" + file);
                    File current = new File("./"
                            + getKey(c.getBlobMap(), file));
                    writeContents(current, readContents(f));
                }
            }
            Arrays.stream(new File(".gitlet/remove").listFiles()).
                    forEach(File::delete);
            Arrays.stream(new File(".gitlet/stage").listFiles()).
                    forEach(File::delete);
            head = c;
            branch = branches;
        }
    }

    /** Command global log.*/
    public static void globallog() {
        String[] files = new File(".gitlet/log").list();
        for (int i = 0; i < files.length; i++) {
            print(Commit.load(files[i]));
        }
    }

    /** Command find.
     * @param args .*/
    public static void find(String[] args) {
        String msg = args[1];
        String[] files = new File(".gitlet/log").list();
        int cnt = 0;
        for (int i = 0; i < files.length; i++) {
            Commit c = Commit.load(files[i]);
            if (msg.equals(c.getMessage())) {
                System.out.println(c.getCode());
                cnt += 1;
            }
        }
        if (cnt == 0) {
            exitWithError("Found no commit with that message.");
        }
    }

    /** Command branch.
     * @param args .*/
    public static void branching(String[] args) {
        String filename = args[1];
        if (tree.getHistory().containsKey(filename)) {
            exitWithError("A branch with that name already exists.");
        }
        tree.makeHistory(filename, head);
    }

    /** Command rm-branch.
     * @param args */
    public static void rmbranch(String[] args) {
        String filename = args[1];
        if (!tree.getHistory().containsKey(filename)) {
            exitWithError("A branch with that name does not exists.");
            return;
        }
        if (filename.equals(branch)) {
            exitWithError("Cannot remove the current branch.");
            return;
        }
        tree.delHistory(filename);
    }

    /** Command reset.
     * @param args .*/
    public static void reset(String[] args) {
        String id = args[1];
        String[] logs = new File(".gitlet/log/").list();
        Commit c = Commit.load(id + ".txt");
        if (!Arrays.asList(logs).contains(id + ".txt")) {
            exitWithError("No commit with that id exists.");
        }
        String[] stage = new File(".gitlet/stage/").list();
        String[] file = new File("./").list();
        String[] commitfile = new File(".gitlet/commit/").list();
        HashMap<String, String> commit = c.getBlobMap();
        for (int i = 0; i < file.length; i++) {
            if (!Arrays.asList(stage).contains(file[i])
                    && commit.containsKey(file[i])
                    && !file[i].equals(".gitlet")
                    && !head.getBlobMap().containsKey(file[i])) {
                exitWithError("There is an untracked file in the way;"
                        + " delete it, or add and commit it first.");
            }
        }
        for (int i = 0; i < file.length; i++) {
            File f = new File("./" + file[i]);
            if (!file[i].equals(".gitignore") && !f.isDirectory()
                    && !file[i].equals("Makefile")
                    && !file[i].equals("proj3.iml")) {
                f.delete();
            }
        }
        for (int i = 0; i < commitfile.length; i++) {
            if (commit.containsValue(commitfile[i])) {
                File f = new File(".gitlet/commit/" + commitfile[i]);
                File current = new File("./" + getKey(commit, commitfile[i]));
                writeContents(current, readContents(f));
            }
        }
        Arrays.stream(new File(".gitlet/remove").listFiles()).
                forEach(File::delete);
        Arrays.stream(new File(".gitlet/stage").listFiles()).
                forEach(File::delete);
        head = c;
        branch = c.getBranch();
        tree.makeHistory(branch, c);
    }

    /** Command rm.
     * @param args */
    public static void rm(String[] args) {
        String file = args[1];
        File f = new File("./" + file);
        String[] stage = new File(".gitlet/stage").list();
        HashMap<String, String> map = head.getBlobMap();
        if (!f.exists()) {
            File remove = new File(".gitlet/remove/" + file);
            try {
                remove.createNewFile();
            } catch (IOException e) {
                return;
            }
            return;
        }
        if (!Arrays.asList(stage).contains(file)
                && !map.containsKey(file)) {
            exitWithError("No reason to remove the file.");
        }
        if (Arrays.asList(stage).contains(file)) {
            File deleted = new File(".gitlet/stage/" + file);
            deleted.delete();
        }
        if (map.containsKey(file)) {
            File rmfile = new File(".gitlet/remove/" + file);
            writeContents(rmfile, readContents(f));
            File f1 = new File("./" + file);
            f1.delete();
        }
    }

    /** Command status.*/
    public static void status() {
        System.out.println("=== Branches ===");
        HashMap<String, String> trees = tree.getHistory();
        for (String key : trees.keySet()) {
            if (branch.equals(key)) {
                System.out.println("*" + key);
            } else {
                System.out.println(key);
            }
        }
        System.out.println("\n=== Staged Files ===");
        String[] stage = new File(".gitlet/stage").list();
        for (String file: stage) {
            System.out.println(file);
        }
        System.out.println("\n=== Removed Files ===");
        HashMap<String, String> map = head.getBlobMap();
        String[] remove = new File(".gitlet/remove").list();
        for (String file: remove) {
            System.out.println(file);
        }
        String[] files = new File("./").list();
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        for (String file: map.keySet()) {
            if (!Arrays.asList(files).contains(file)
                    && !Arrays.asList(remove).contains(file)) {
                System.out.println(file + " (deleted)");
            }
            if (!Arrays.asList(stage).contains(file)
                    && Arrays.asList(files).contains(file)) {
                File f = new File("./" + file);
                if (!Arrays.asList(files).contains(file)) {
                    System.out.println(file + " (modified)");
                }
            }
        }
        for (String file: stage) {
            if (Arrays.asList(files).contains(file)) {
                File f1 = new File("./" + file);
                File f2 = new File(".gitlet/stage/" + file);
                if (!sha1(readContents(f1)).equals(sha1(readContents(f2)))) {
                    System.out.println(file + " (modified)");
                }
            } else if (!Arrays.asList(files).contains(file)
                    && !Arrays.asList(remove).contains(file)) {
                System.out.println(file + " (deleted)");
            }
        }
        System.out.println("\n=== Untracked Files ===");
        for (String file: files) {
            File f = new File("./" + file);
            if (!map.containsKey(file) && !file.equals(".gitignore")
                    && !file.equals("Makefile") && !file.equals("proj3.iml")
                    && !Arrays.asList(stage).contains(file)
                    && !f.isDirectory()) {
                System.out.println(file);
            }
        }
    }

    /** Command merge.
     * @param args .*/
    public static void merge(String[] args) {
        String branches = args[1];
        String id = tree.getHistory().get(branches);
        Commit c = Commit.load(id + ".txt");
        Commit sp = split(head, c);
        chk = true;
        String[] stage = new File(".gitlet/stage").list();
        if (checkmerge(sp, c, stage, id)) {
            String[] commit = new File(".gitlet/commit").list();
            HashMap<String, String> mapc = c.getBlobMap();
            HashMap<String, String> mapsp = sp.getBlobMap();
            HashMap<String, String> maphead = head.getBlobMap();
            LinkedList<String> visited = new LinkedList<>();
            for (String file : commit) {
                String name = "";
                if (getKey(mapc, file) != null) {
                    name = getKey(mapc, file);
                } else if (getKey(mapsp, file) != null) {
                    name = getKey(mapsp, file);
                } else if (getKey(maphead, file) != null) {
                    name = getKey(maphead, file);
                }
                if (!visited.contains(name)) {
                    visited.add(name);
                    boolean finc = mapc.containsKey(name);
                    boolean finsp = mapsp.containsKey(name);
                    boolean finhead = maphead.containsKey(name);
                    boolean compare1 = compare(mapc.get(name),
                            mapsp.get(name));
                    boolean compare2 = compare(maphead.get(name),
                            mapsp.get(name));
                    if (finc && finsp && finhead) {
                        mergehelp(compare1, compare2, name, id, mapc,
                                mapc.get(name), maphead, maphead.get(name));
                    } else if (!finc && finhead && !finsp) {
                        checkoutcommit(getKey(maphead,
                                maphead.get(name)), head.getCode());
                        add(new String[]{"add", name});
                    } else if (finc && !finhead && !finsp) {
                        checkoutcommit(getKey(mapc,
                                mapc.get(name)), c.getCode());
                        add(new String[]{"add", name});
                    } else if ((!finc && finhead && finsp)) {
                        if (compare2) {
                            rm(new String[]{"rm", name});
                        } else {
                            mergefile(name, mapc.get(name), maphead.get(name));
                        }
                    } else if ((finc && !finhead && finsp)) {
                        if (!compare1) {
                            mergefile(name, mapc.get(name), maphead.get(name));
                        }
                    } else if (finc && finhead && !finsp) {
                        mergefile(name, mapc.get(name), maphead.get(name));
                    }
                }
            }
            printmerge(c);
        }
    }

    /** Command merge to merging two files.
     * @param cfile .
     * @param headfile .
     * @param namefile .*/
    private static void mergefile(String namefile,
                                  String cfile, String headfile) {
        try {
            chk = false;
            File f1 = new File("./" + namefile);
            f1.delete();
            File f2 = new File(".gitlet/commit/" + headfile);
            FileWriter f3 = new FileWriter(f1, true);
            if (f2.length() != 0) {
                f3.write("<<<<<<< HEAD\n");
            } else {
                f3.write("<<<<<<< HEAD");
            }
            f3.write(readContentsAsString(f2));
            f3.close();
            File f4 = new File(".gitlet/commit/" + cfile);
            FileWriter f5 = new FileWriter(f1, true);
            f5.write("=======\n");
            f5.write(readContentsAsString(f4));
            f5.close();
            FileWriter f6 = new FileWriter(f1, true);
            f6.write(">>>>>>>\n");
            f6.close();
        } catch (IOException e) {
            System.exit(-1);
        }
    }

    /** Command merge to helping two files merge.
     * @param id .
     * @param compare1 .
     * @param compare2 .
     * @param map1file .
     * @param map1 .
     * @param map2 .
     * @param map2file .
     * @param name .
     * */
    private static void mergehelp(boolean compare1, boolean compare2,
                                  String name, String id,
                                  HashMap<String, String> map1,
                                  String map1file, HashMap<String, String> map2,
                                  String map2file) {
        if (compare1 && !compare2) {
            checkoutcommit(getKey(map2, map2file), head.getCode());
            add(new String[]{"add", name});
        } else if (!compare1 && compare2) {
            checkoutcommit(getKey(map1, map1file), id);
            File f = new File(".gitlet/commit/" + map1file);
            File curr = new File(".gitlet/stage/" + name);
            writeContents(curr, readContents(f));
        } else if (!compare1 && !compare2 && !map1file.equals(map2file)) {
            chk = false;
            mergefile(name, map1file, map2file);
        }
    }

    /** Compare function for two sha1 code.
     * @param x .
     * @param y .
     * @return .*/
    private static boolean compare(String x, String y) {
        if (x != null) {
            return x.equals(y);
        } else {
            try {
                new File(".gitlet/commit/null").createNewFile();
                return false;
            } catch (IOException e) {
                return false;
            }
        }
    }

    /** Printing result of merge.
     * @param c .*/
    private static void printmerge(Commit c) {
        commit(new String[] {"commit", "Merged "
                + c.getBranch() + " into " + head.getBranch()
                + ".  Merge: " + c.getCode().substring(0, 7)
                + " " + head.getCode().substring(0, 7)});
        if (!chk) {
            System.out.println("Encountered a merger conflict.");
        }
    }

    /** Command merge into checking if merge is valid.
     * @param first .
     * @param id .
     * @param second .
     * @param stage .
     * @return .*/
    private static boolean checkmerge(Commit first, Commit second,
                                      String[] stage, String id) {
        HashMap<String, String> map = head.getBlobMap();
        String[] files = new File("./").list();
        String[] remove = new File(".gitlet/remove").list();
        boolean check = false;
        for (String file: files) {
            File f = new File("./" + file);
            if (!map.containsKey(file) && !file.equals(".gitignore")
                    && !file.equals("Makefile") && !file.equals("proj3.iml")
                    && !Arrays.asList(stage).contains(file)
                    && !f.isDirectory()) {
                check = true;
                break;
            }
        }
        if (check) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it or add and commit it first.");
            return false;
        } else if (remove.length != 0 || stage.length != 0) {
            exitWithError("You have uncommitted changes.");
        }  else if (id == null) {
            System.out.println("A branch with that name does not exist.");
        } else if (id.equals(head.getCode())) {
            System.out.println("Cannot merge a branch with itself.");
        } else if (first.getCode().equals(second.getCode())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
        }  else if (first.getCode().equals(head.getCode())) {
            head = second;
            System.out.println("Current branch fast-forwarded.");
        } else {
            return !first.getCode().equals(second.getCode());
        }
        return false;
    }

    /** Finding connection between Commit x and Commit y.
     * @param x .
     * @param y .
     * @return .*/
    private static Commit split(Commit x, Commit y) {
        Commit copy1 = x;
        Commit copy2 = y;
        while (copy1 != null) {
            while (copy2 != null) {
                if (copy2.getCode().equals(copy1.getCode())) {
                    return copy1;
                }
                copy2 = copy2.getParent();
            }
            copy2 = y;
            copy1 = copy1.getParent();
        }
        return null;
    }

    /** Returns the key of the HashMap given the value.
     * @param commit .
     * @param file .
     * @return .*/
    private static String getKey(HashMap<String, String> commit,
                                 String file) {
        for (Entry<String, String> entry: commit.entrySet()) {
            if (entry.getValue().equals(file)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** Printing log.
     * @param print .*/
    private static void print(Commit print) {
        System.out.println("===");
        System.out.println("commit " + print.getCode());
        if (print.getMessage().startsWith("Merged")) {
            String[] str = print.getMessage().split("  ");
            System.out.println(str[1]);
            System.out.println("Date: " + print.getTime());
            System.out.println(str[0]);
        } else {
            System.out.println("Date: " + print.getTime());
            System.out.println(print.getMessage());
        }
        System.out.println();
    }

    /** Return head.*/
    public static Commit getHead() {
        return head;
    }

    /** Serialize.*/
    public static void serialize() throws IOException {
        File f1 = new File(".gitlet/head.txt");
        File f2 = new File(".gitlet/tree.txt");
        File f3 = new File(".gitlet/branch.txt");
        FileOutputStream hIn = new FileOutputStream(f1);
        FileOutputStream tIn = new FileOutputStream(f2);
        FileOutputStream bIn = new FileOutputStream(f3);
        ObjectOutputStream hOut = new ObjectOutputStream(hIn);
        ObjectOutputStream tOut = new ObjectOutputStream(tIn);
        ObjectOutputStream bOut = new ObjectOutputStream(bIn);
        hOut.writeObject(head);
        tOut.writeObject(tree);
        bOut.writeObject(branch);
    }

    /** Deserialize.*/
    public static void deserialize()
            throws IOException, ClassNotFoundException {
        if (!GITLET_FOLDER.exists()) {
            exitWithError("Not in an initialized Gitlet directory.");
        }
        File f1 = new File(".gitlet/head.txt");
        File f2 = new File(".gitlet/tree.txt");
        File f3 = new File(".gitlet/branch.txt");
        FileInputStream hIn = new FileInputStream(f1);
        FileInputStream tIn = new FileInputStream(f2);
        FileInputStream bIn = new FileInputStream(f3);
        ObjectInputStream hOut = new ObjectInputStream(hIn);
        ObjectInputStream tOut = new ObjectInputStream(tIn);
        ObjectInputStream bOut = new ObjectInputStream(bIn);
        head = (Commit) hOut.readObject();
        tree = (Tree) tOut.readObject();
        branch = (String) bOut.readObject();
    }

    /** Keeping track of Tree.*/
    private static Tree tree;

    /** Keeping track of Commit.*/
    private static Commit head;

    /** Keeping track of branch.*/
    private static String branch;

    /** Keeping track if merge will commit.*/
    private static boolean chk;
}
