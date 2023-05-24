package io.github.jshipit;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;

public class Mount {
    public interface CLibrary extends Library {
        int mount(String source, String target,
                  String filesystemtype, int mountflags,
                  String data);

        int umount(String target);
    }

    public void mount(String source, String target,
                      String filesystemtype, int mountflags,
                      String data) {
        CLibrary libc;

        if (Platform.isLinux()) {
            libc = Native.load("c", CLibrary.class);
            int result = libc.mount(source, target, filesystemtype, mountflags, data);
            if (result == 0) {
                System.out.println("Device mounted successfully.");
            } else {
                System.out.println("Device mount failed.");
            }
        }
    }

    public void overlayMount(String[] lower, String upper, String target) {
        CLibrary libc;

        if (Platform.isLinux()) {
            libc = Native.load("c", CLibrary.class);
            int result = libc.mount("overlay", target, "overlay", 0, "lowerdir="+String.join(":", lower)+",upperdir="+upper+",workdir="+target+"/work");
            if (result == 0) {
                System.out.println("Device mounted successfully.");
            } else {
                System.out.println("Device mount failed.");
            }
        } else {
            System.out.println("Platform not supported.");
            System.out.println("mount -t overlay overlay -o lowerdir="+String.join(":", lower)+",upperdir="+upper+",workdir="+target+"/work "+target);
        }
    }
}
