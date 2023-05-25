package io.github.jshipit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContainerManager {
    private String containerName;
    private String containerRepo;
    private String containerImage;
    private String containerTag;
    private String containerApiRepo;
    private OCIDataStore dataStore;

    public ContainerManager(String containerName, String containerImage, String containerTag, String containerApiRepo, String containerRepo, OCIDataStore dataStore) {
        this.containerName = containerName;
        this.containerImage = containerImage;
        this.containerTag = containerTag;
        this.containerApiRepo = containerApiRepo;
        this.containerRepo = containerRepo;
        this.dataStore = dataStore;
    }

    public ContainerManager(String containerName, OCIDataStore dataStore) {
        this.containerName = containerName;
        this.containerImage = dataStore.getContainerImage(containerName);
        this.containerTag = dataStore.getContainerTag(containerName);
        this.containerApiRepo = dataStore.getContainerApiRepo(containerName);
        this.containerRepo = dataStore.getContainerRepo(containerName);
        this.dataStore = dataStore;
    }

    public void createContainer() {
        System.out.println("Creating container");

        if (!Files.isDirectory(Paths.get(dataStore.getPath() + "/" + this.containerImage + "/" + this.containerTag))) {
            System.out.println("Image does not exist");
            return;
        }

        String containerDirectory = dataStore.createContainerDirectory(this.containerImage, this.containerTag, this.containerName, this.containerApiRepo, this.containerRepo);

        new File(containerDirectory + "/containerOverlay").mkdirs();
        new File(containerDirectory + "/root").mkdirs();
        new File(containerDirectory+"/work").mkdirs();
    }

    public String startContainer(boolean getCommand) {

        String containerDirectory = dataStore.getContainerPath(this.containerName);
        List<String> content = null;
        try {
            content = Files.readAllLines(Paths.get(this.dataStore.getPath() + "/" + this.containerImage + "/" + this.containerTag + "/layers"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assert content != null;
        List<String> layers = new ArrayList<>();
        for (String line : content) {
            layers.add(this.dataStore.getPath() + "/blobs/" + line.replace("sha256:", ""));
        }
        assert layers.size() > 0;

        SysUtils sysUtils = new SysUtils();
        String[] diffs = layers.toArray(new String[0]);
        return sysUtils.overlayMount(diffs, containerDirectory + "/containerOverlay", containerDirectory + "/root", containerDirectory + "/work", !getCommand);

    }

    public void runCommand(String command) {
        String containerDirectory = dataStore.getContainerPath(this.containerName);
        String dataStorePath = dataStore.getPath();

        File configPath = new File(dataStorePath + "/" + this.containerImage + "/" + this.containerTag + "/config");
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
            config.get("Env").forEach((JsonNode envVar) -> {
                env.add(envVar.asText());
            });
            cmd = config.get("Cmd").get(0).asText();
            hostname = config.get("Hostname").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // why would these be null? I don't know, but I won't bother handling this better
        assert hostname != null;
        assert cmd != null;

        List<String> bwrapCommand = new ArrayList<>();

        for(String envVar : env) {
            bwrapCommand.add("--setenv "+envVar.split("=")[0]+" "+envVar.split("=")[1]);
        }

        bwrapCommand.add("--ro-bind "+containerDirectory+"/root / --chdir /");
        bwrapCommand.add("--share-net");
        bwrapCommand.add("--unshare-uts --hostname "+ (!hostname.isBlank() ? hostname : this.containerName+"-"+this.containerImage));
        bwrapCommand.add("/bin/sh -c "+cmd);
        SysUtils sysUtils = new SysUtils();
        String bwrapCMD = sysUtils.execInBwrap(bwrapCommand.toArray(new String[0]), false);
        String mountCMD = startContainer(true);

        String CMD = "unshare --user --map-root-user --mount bash -c \""+mountCMD+";"+bwrapCMD+";"+command+"\""; // I am starting to realize that this project was not a good idea
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", CMD);
        pb.inheritIO();
        try {
            Process p = pb.start();
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public String genContainerID() {
        return UUID.randomUUID().toString();
    }
}