package io.github.jshipit;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public ContainerManager(String containerName, String containerImage, String containerTag, String entryCommand, OCIDataStore dataStore) {
        this.containerName = containerName;
        this.containerImage = containerImage;
        this.containerTag = containerTag;
        this.entryCommand = entryCommand;
        this.dataStore = dataStore;
    }

    public void createContainer() {
        System.out.println("Creating container");

        if (!Files.isDirectory(Paths.get(dataStore.getPath() + this.containerImage + "/" + this.containerTag))) {
            System.out.println("Image does not exist, pulling now");
            dataStore.createImage(this.containerApiRepo, this.containerRepo, this.containerImage, this.containerTag);
        }

        if (this.containerID == null) {
            this.containerID = genContainerID();
        }

        String containerDirectory = dataStore.createContainerDirectory(this.containerImage, this.containerTag, this.containerName, this.containerID);

        System.out.println("Container directory: " + containerDirectory);
        System.out.println("Container ID: " + this.containerID);

        new File(containerDirectory + "/containerOverlay").mkdirs();
        new File(containerDirectory + "/root").mkdirs();




    }

    public String genContainerID() {
        System.out.println("Generating container ID");
        return UUID.randomUUID().toString();
    }
}