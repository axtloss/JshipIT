package io.github.jshipit;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

public class SysUtils {

    interface CLibrary extends Library {
        public int chmod(String path, int mode);
    }

    public void chmod(String path, int mode) {
        CLibrary libc = (CLibrary) Native.load("c", CLibrary.class);
        libc.chmod(path, mode);
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
