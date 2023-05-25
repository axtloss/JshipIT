package io.github.jshipit;

import com.sun.jna.Platform;

import java.io.File;

public class SysUtils {

    public void chmod(String path, int mode) {
        ProcessBuilder pb = new ProcessBuilder("chmod", Integer.toString(mode), path);
        pb.inheritIO();
        try {
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    public void overlayMount(String[] lower, String upper, String target, String work) {
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
    }
}
