package io.github.jshipit;

import com.sun.jna.Platform;

import java.io.File;

public class SysUtils {


    public void untar(String in, String out) {
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

    public String execInBwrap(String[] args, boolean execute) {
        if (!execute) {
            return "bwrap "+String.join(" ", args);
        }
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

    public String overlayMount(String[] lower, String upper, String target, String work, boolean execute) {
        if (!execute) {
            return "mount -t overlay overlay -o lowerdir="+String.join(":", lower)+",upperdir="+upper+",workdir="+work+" "+target;
        }
        if (Platform.isLinux()) {

            ProcessBuilder pb = new ProcessBuilder("unshare", "--user", "--map-root-user", "--mount", "mount", "-t", "overlay", "overlay", "-o", "lowerdir="+String.join(":", lower)+",upperdir="+upper+",workdir="+work, target);
            pb.inheritIO();
            try {
                Process p = pb.start();
                p.waitFor();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {
            System.out.println("Platform not supported.");
            System.out.println("mount -t overlay overlay -o lowerdir="+String.join(":", lower)+",upperdir="+upper+",workdir="+work+" "+target);
        }
        return "";
    }
}
