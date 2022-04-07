package gitlet;

import java.io.Serializable;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import static gitlet.Utils.*;
import static gitlet.Main.exitWithError;
import java.text.SimpleDateFormat;

/** Commit Class for files.
 * @author Minjune Kim
 */
public class Commit implements Serializable {

    /** Declaring Commit after initial.
     * @param head .
     * @param blobs .
     * @param messages .
     * @param branches .
     * */
    Commit(HashMap<String, String> blobs,
           Commit head, String messages, String branches) {
        time = DATEOFCOMMIT.format(new Date());
        parent = head;
        String file = "";
        for (String f: blobs.keySet()) {
            file += f;
        }
        message = messages;
        code = sha1(file, parent.getCode(), message, time);
        blobmap = blobs;
        branch = branches;
    }

    /** Declaring Commit for the firs time.
     * @param str .
     * @param blob .
     * */
    Commit(String str, HashMap<String, String> blob) {
        time = DATEOFCOMMIT.format(new Date());
        code = sha1(str, time);
        message = str;
        blobmap = blob;
        branch = "master";
    }

    /** Loading Commit function.
     * @return null .
     * @param str .
     * */
    public static Commit load(String str) {
        File file = new File(".gitlet/log/" + str);
        if (file.exists()) {
            try {
                FileInputStream fIn = new FileInputStream(file);
                ObjectInputStream oIn = new ObjectInputStream(fIn);
                Commit c = (Commit) oIn.readObject();
                return c;
            } catch (IOException e) {
                exitWithError("IOException while loading commit.");
            } catch (ClassNotFoundException e) {
                exitWithError("ClassNotFoundException while loading commit.");
            }
        }
        return null;
    }

    /** Saving Commit.*/
    public void save() {
        try {
            File commiting = new File(".gitlet/log/" + code + ".txt");
            FileOutputStream fo = new FileOutputStream(commiting);
            ObjectOutputStream oo = new ObjectOutputStream(fo);
            oo.writeObject(this);
            oo.close();
        } catch (IOException e) {
            System.out.println("Error while saving " + code);
        }

    }

    /** Return current message.*/
    public String getMessage() {
        return message;
    }

    /** Return current time.*/
    public String getTime() {
        return time;
    }

    /** Return current sha1 code.*/
    public String getCode() {
        return code;
    }

    /** Return current parent.*/
    public Commit getParent() {
        return parent;
    }

    /** Return current blob.*/
    public HashMap<String, String> getBlobMap() {
        return blobmap;
    }

    /** Return current branch.*/
    public String getBranch() {
        return branch;
    }

    /** Keeping track of the parents.*/
    private Commit parent;

    /** Keeping track of the sha1 code.*/
    private String code;

    /** Keeping track of the commit message.*/
    private String message;

    /** Keeping track of the branch.*/
    private String branch;

    /** Keeping track of time.*/
    private String time;

    /** Declaring blob.*/
    private HashMap<String, String> blobmap;

    /** Formatting of the date.*/
    static final SimpleDateFormat DATEOFCOMMIT =
            new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
}
