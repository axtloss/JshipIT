package io.github.jshipit;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ContainerManager {
    private String containerID;
    private String containerName;
    private String containerRepo;
    private String containerImage;
    private String containerTag;
    private String containerApiRepo;
    private String entryCommand;
    private OCIDataStore dataStore;

    public ContainerManager(String containerName, String containerImage, String containerTag, String entryCommand, String containerApiRepo, String containerRepo, OCIDataStore dataStore) {
        this.containerName = containerName;
        this.containerImage = containerImage;
        this.containerTag = containerTag;
        this.entryCommand = entryCommand;
        this.containerApiRepo = containerApiRepo;
        this.containerRepo = containerRepo;
        this.dataStore = dataStore;
    }

    public void createContainer() {
        System.out.println("Creating container");

        if (!Files.isDirectory(Paths.get(dataStore.getPath() + "/" + this.containerImage + "/" + this.containerTag))) {
            System.out.println("Image does not exist");
            return;
        }

        if (this.containerID == null) {
            this.containerID = genContainerID();
        }

        String containerDirectory = dataStore.createContainerDirectory(this.containerImage, this.containerTag, this.containerName, this.containerID);

        new File(containerDirectory + "/containerOverlay").mkdirs();
        new File(containerDirectory + "/root").mkdirs();
        new File(containerDirectory+"/work").mkdirs();
    }

    public void startContainer() {

        String containerDirectory = dataStore.getContainerPath(this.containerName, this.containerID);
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
        sysUtils.overlayMount(diffs, containerDirectory + "/containerOverlay", containerDirectory + "/root", containerDirectory + "/work");

    }

    public String genContainerID() {
        return UUID.randomUUID().toString();
    }
}