// SPDX-License-Identifier: GPL-3.0-only

package io.github.jshipit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ContainerManager {
    private String containerName;
    private String containerRepo;
    private String containerImage;
    private String containerTag;
    private String containerApiRepo;
    private String containerCommand;
    private List<String> containerMounts;
    private boolean containerIsolated;
    private OCIDataStore dataStore;

    public ContainerManager(String containerName, String containerImage, String containerTag, String containerApiRepo, String containerRepo, boolean containerIsolated, OCIDataStore dataStore) {
        this.containerName = containerName;
        this.containerImage = containerImage;
        this.containerTag = containerTag;
        this.containerApiRepo = containerApiRepo;
        this.containerRepo = containerRepo;
        this.containerMounts = null;
        this.containerIsolated = containerIsolated;
        this.dataStore = dataStore;
    }

    public ContainerManager(String containerName, String containerCommand, List<String> containerMounts, OCIDataStore dataStore) {
        this.containerName = containerName;
        this.containerImage = dataStore.getContainerImage(containerName);
        this.containerTag = dataStore.getContainerTag(containerName);
        this.containerApiRepo = dataStore.getContainerApiRepo(containerName);
        this.containerRepo = dataStore.getContainerRepo(containerName);
        this.containerCommand = containerCommand;
        this.containerMounts = containerMounts;
        this.dataStore = dataStore;
    }

    /*
     * Create a container directory
     */
    public void createContainer() {
        System.out.println("Creating container");

        if (dataStore.containerExists(this.containerName)) {
            System.out.println("Container already exists");
            return;
        }

        if (!Files.isDirectory(Paths.get(dataStore.getPath() + "/" + this.containerImage + "/" + this.containerTag))) {
            System.out.println("Image does not exist");
            return;
        }

        String containerDirectory = dataStore.createContainerDirectory(this.containerImage, this.containerTag, this.containerName, this.containerApiRepo, this.containerRepo);

        new File(containerDirectory + "/containerOverlay").mkdirs(); // The upper directory of the overlay mount, user data and any changes to root will be stored here
        new File(containerDirectory + "/root").mkdirs(); // The root directory of the overlay mount
        new File(containerDirectory+"/work").mkdirs(); // The work directory of the overlay mount

        // Create the container TOML config file
        try {
            new File(containerDirectory + "/config.toml").createNewFile();
            FileOutputStream fos = new FileOutputStream(containerDirectory + "/config.toml");

            String str = "[container]\n";
            str += "command = \"" + this.containerCommand + "\"\n";
            str += "hostname = \"" + this.containerName + "\"\n\n";
            str += "[permissions]\n";
            str += "unshare-all = false\n";
            str += "unshare-net = false\n";
            str += "unshare-user = false\n";
            str += "unshare-ipc = false\n";
            str += "unshare-pid = false\n";
            str += "unshare-uts = true\n";
            str += "unshare-cgroup = false\n";
            str += "mount-dev = true\n";
            str += "mount-proc = true\n";
            str += "mount-bind = []\n";

            byte[] b = str.getBytes();
            fos.write(b);
            fos.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /*
     * Start a container
     *
     * @param getCommand: If true, return the command to run the container
     *
     * @return: The command to run the container. If getCommand is false, return null
     */
    public String startContainer(boolean getCommand) {

        String containerDirectory = dataStore.getContainerPath(this.containerName);
        List<String> content = null;
        try {
            content = Files.readAllLines(Paths.get(this.dataStore.getPath() + "/" + this.containerImage + "/" + this.containerTag + "/layers"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assert content != null; // assert my beloved

        // We parse the layers file to get the layers of the image
        List<String> layers = new ArrayList<>();
        for (String line : content) {
            layers.add(this.dataStore.getPath() + "/blobs/" + line.replace("sha256:", ""));
        }
        assert layers.size() > 0;

        SysUtils sysUtils = new SysUtils();
        String[] diffs = layers.toArray(new String[0]);
        // Depending on getCommand we either directly mount the layers or return the command to mount the layers
        return sysUtils.overlayMount(diffs, containerDirectory + "/containerOverlay", containerDirectory + "/root", containerDirectory + "/work", !getCommand);

    }

    /*
     * Run a command in a container
     */
    public void runCommand() {
        String containerDirectory = dataStore.getContainerPath(this.containerName);
        String dataStorePath = dataStore.getPath();
        ConfigParser configParser = new ConfigParser(containerDirectory+"/config.toml");

        File configPath = new File(dataStorePath + "/" + this.containerImage + "/" + this.containerTag + "/config"); // Path to the config file of the image
        String content = null;
        try {
            content = Files.readAllLines(Paths.get(configPath.getAbsolutePath())).toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ObjectMapper mapper = new ObjectMapper();
        List<String> env = new ArrayList<>();
        String cmd = null;
        String hostname = null;
        try {
            JsonNode config = mapper.readTree(content.toString()).get(0).get("config");
            config.get("Env").forEach((JsonNode envVar) -> { // Get the environment variables specified in the config file
                env.add(envVar.asText());
            });
            cmd = configParser.getString("container.command").equals("null") ? config.get("Cmd").get(0).asText() : configParser.getString("container.command"); // Get the command specified in the config file
            hostname = configParser.getString("container.hostname").equals("null") ? config.get("Hostname").asText() : configParser.getString("container.hostname"); // Get the hostname specified in the config file
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // why would these be null? I don't know, but I won't bother handling this better
        assert hostname != null;
        assert cmd != null;

        List<String> bwrapCommand = new ArrayList<>();

        for(String envVar : env) {
            bwrapCommand.add("--setenv "+envVar.split("=")[0]+" "+envVar.split("=")[1]); // Create the flags for bwrap to set the environment variables
        }

        bwrapCommand.add("--bind "+containerDirectory+"/root / --chdir /"); // Bind the root to / inside the bwrap namespace

        if (!configParser.getBoolean("permissions.unshare-all")) {
            processBwrapPermissions(bwrapCommand);// Process the permissions specified in the config file
        }

        bwrapCommand.add("/bin/sh -c '"+(this.containerCommand != null ? this.containerCommand : cmd)+"'"); // Run the command specified in the config file or the command specified in the constructor

        SysUtils sysUtils = new SysUtils();
        String bwrapCMD = sysUtils.execInBwrap(bwrapCommand.toArray(new String[0]), false);
        String mountCMD = startContainer(true);

        String CMD = "unshare --user --map-root-user --mount bash -c \""+mountCMD+";"+bwrapCMD+"\""; // I am starting to realize that this project was not a good idea
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", CMD);
        pb.inheritIO();
        try {
            Process p = pb.start();
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void processBwrapPermissions(List<String> bwrapCommand) {
        ConfigParser configParser = new ConfigParser(dataStore.getContainerPath(this.containerName)+"/config.toml");
        List<String> configMounts = configParser.getList("permissions.mount-bind");
        if (configMounts.size() > 0) {
            for (String mount : configMounts) {
                bwrapCommand.add("--bind "+mount.split(":")[0]+" "+mount.split(":")[1]); // Bind the mount to the destination specified in the config file
            }
        }
        if (this.containerMounts != null) {
            for (String mount : containerMounts) {
                bwrapCommand.add("--bind "+mount.split(":")[0]+" "+mount.split(":")[1]); // Bind the mount to the destination specified by the user
            }
        }


        if (!configParser.getBoolean("permissions.unshare-net")) {
            bwrapCommand.add("--ro-bind /etc/resolv.conf /etc/resolv.conf"); // Bind the host resolv.conf to the container
            bwrapCommand.add("--share-net"); // Share the network namespace
        } else {
            bwrapCommand.add("--unshare-net"); // Unshare the network namespace
        }

        if (configParser.getBoolean("permissions.unshare-uts")) {
            bwrapCommand.add("--unshare-uts"); // Unshare the UTS namespace

            bwrapCommand.add("--hostname "+(configParser.getString("container.hostname").equals("null") ? this.containerName : configParser.getString("container.hostname"))); // Set the hostname
        }

        if (configParser.getBoolean("permissions.unshare-user")) {
            bwrapCommand.add("--unshare-user"); // Unshare the user namespace
        }

        if (configParser.getBoolean("permissions.unshare-ipc")) {
            bwrapCommand.add("--unshare-ipc"); // Unshare the IPC namespace
        }

        if (configParser.getBoolean("permissions.unshare-pid")) {
            bwrapCommand.add("--unshare-pid"); // Unshare the PID namespace
        }

        if (configParser.getBoolean("permissions.unshare-uts")) {
            bwrapCommand.add("--unshare-uts"); // Unshare the UTS namespace
        }

        if (configParser.getBoolean("permissions.unshare-cgroup")) {
            bwrapCommand.add("--unshare-cgroup"); // Unshare the cgroup namespace
        }

        if (configParser.getBoolean("permissions.mount-dev")) {
            bwrapCommand.add("--dev-bind /dev /dev"); // Mount /dev
        } else {
	    bwrapCommand.add("--dev /dev"); // Make sure a seperate devfs exists
	}

        if (configParser.getBoolean("permissions.mount-proc")) {
            bwrapCommand.add("--bind /proc /proc"); // Mount /proc
        } else {
	    bwrapCommand.add("--proc /proc"); // Make sure a seperate procfs exists
	}
    }

    /*
     * Delete a container
     */
    public void deleteContainer() {
        String containerDirectory = dataStore.getContainerPath(this.containerName);
        try {
            Files.delete(Paths.get(containerDirectory + "/containerOverlay"));
            Files.delete(Paths.get(containerDirectory + "/root"));
            Files.delete(Paths.get(containerDirectory + "/work"));
            Files.delete(Paths.get(containerDirectory));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        dataStore.deleteContainerFromDatabase(this.containerName);
    }
}
