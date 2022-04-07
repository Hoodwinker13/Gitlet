package gitlet;

import java.io.IOException;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Minjune Kim
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args)
            throws IOException, ClassNotFoundException {
        if (args.length == 0) {
            exitWithError("Please enter a command.");
        } else if (!args[0].equals("init")) {
            CommandClass.deserialize();
        }
        switch (args[0]) {
        case "init":
            CommandClass.init();
            break;
        case "commit":
            CommandClass.commit(args);
            break;
        case "add":
            CommandClass.add(args);
            break;
        case "log":
            CommandClass.log(CommandClass.getHead());
            break;
        case "checkout":
            CommandClass.checkout(args);
            break;
        case "rm":
            CommandClass.rm(args);
            break;
        case "global-log":
            CommandClass.globallog();
            break;
        case "find":
            CommandClass.find(args);
            break;
        case "status":
            CommandClass.deserialize();
            CommandClass.status();
            break;
        case "branch":
            CommandClass.branching(args);
            break;
        case "rm-branch":
            CommandClass.rmbranch(args);
            break;
        case "reset":
            CommandClass.reset(args);
            break;
        case "merge":
            CommandClass.merge(args);
            break;
        default:
            exitWithError("No command with that name exists.");
        }
        CommandClass.serialize();
    }

    /** Function for exiting with error of status 0.
     * @param message .*/
    public static void exitWithError(String message) {
        if (message != null && !message.equals("")) {
            System.out.println(message);
        }
        System.exit(0);
    }

}
