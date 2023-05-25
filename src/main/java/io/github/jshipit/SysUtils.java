package io.github.jshipit;

import com.sun.jna.Platform;

import java.io.File;

/*
* A class for running system commands
 */
public class SysUtils {

    /*
     * Untar a tarball to a directory
     *
     * @param in The tarball to untar
     * @param out The directory to untar to
     */
    public void untar(String in, String out) {
        // We do not use the Java tar library because it does not care about unix permissions. Common Java L
        new File(out).mkdirs();
        ProcessBuilder pb = new ProcessBuilder("tar", "-xf", in, "-C", out);
        pb.inheritIO();
        try {
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Execute a command in a bwrap sandbox
     *
     * @param args The arguments to pass to bwrap
     * @param execute Whether to execute the command or just return the command
     *
     * @return The command that was executed. Empty string if execute is true
     */
    public String execInBwrap(String[] args, boolean execute) {
        if (!execute) {
            return "bwrap "+String.join(" ", args);
        }
        // ProcessBuilder wraps the args in quotes, since all args are passed at once, they are all quoted together
        // This breaks bwrap (and many other commands), so we use bash -c to execute the command with the entire command as the arg
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", "bwrap "+String.join(" ", args));
        pb.inheritIO();
        try {
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "";
    }

    /*
     * Creates an overlay mount
     *
     * @param lower The lower directories to use
     * @param upper The upper directory to use
     * @param target The target directory to mount to
     * @param work The work directory to use
     * @param execute Whether to execute the command or just return the command
     *
     * @return The command that was executed. Empty string if execute is true
     */
    public String overlayMount(String[] lower, String upper, String target, String work, boolean execute) {
        if (!execute) {
            return "mount -t overlay overlay -o lowerdir="+String.join(":", lower)+",upperdir="+upper+",workdir="+work+" "+target;
        }
        if (Platform.isLinux()) {
            // unshare creates a new user namespace, so we can mount without root
            // This only works on Linux version 5.11 and above, which everyone should have by now
            ProcessBuilder pb = new ProcessBuilder("unshare", "--user", "--map-root-user", "--mount", "mount", "-t", "overlay", "overlay", "-o", "lowerdir="+String.join(":", lower)+",upperdir="+upper+",workdir="+work, target);
            pb.inheritIO();
            try {
                Process p = pb.start();
                p.waitFor();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {
            // Since other *nix systems do not have overlayfs or unshare, we just print an error
            // I would be surprised if you even got this far on Windows
            System.out.println("Platform not supported.");
        }
        return "";
    }
}
