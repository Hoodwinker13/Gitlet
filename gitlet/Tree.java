package gitlet;

import java.io.Serializable;
import java.util.LinkedHashMap;

/** For keeping track of the branches with the corresponding sha1 code.
 * @author Minjune Kim
 */
public class Tree implements Serializable {
    /** Initialize the Tree.
     * @param branch .
     * @param commit .*/
    Tree(String branch, Commit commit) {
        history = new LinkedHashMap<String, String>();
        history.put(branch, commit.getCode());
    }

    /** Creating a new branch in Hashmap.
     * @param branch .
     * @param head */
    public void makeHistory(String branch, Commit head) {
        history.put(branch, head.getCode());
    }

    /** Deleting a certain branch from Hashmap.
     * @param branch .*/
    public void delHistory(String branch) {
        history.remove(branch);
    }

    /** return the Tree.*/
    public LinkedHashMap<String, String> getHistory() {
        return history;
    }

    /** Used LinkedHashMap to keep things in order.*/
    private LinkedHashMap<String, String> history;
}
