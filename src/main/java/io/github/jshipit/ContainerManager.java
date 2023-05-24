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

        System.out.println("Container directory: " + containerDirectory);
        System.out.println("Container ID: " + this.containerID);

        new File(containerDirectory + "/containerOverlay").mkdirs();
        new File(containerDirectory + "/root").mkdirs();
    }

    public void startContainer() {
        System.out.println("Starting container");
        System.out.println("Container ID: " + this.containerID);
        System.out.println("Container name: " + this.containerName);
        System.out.println("Container image: " + this.containerImage);
        System.out.println("Container tag: " + this.containerTag);
        System.out.println("Container entry command: " + this.entryCommand);
        System.out.println("Container data store: " + this.dataStore.getPath());


        String containerDirectory = dataStore.getContainerPath(this.containerName, this.containerID);
        System.out.println("Container directory: " + containerDirectory);

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

        Mount mount = new Mount();
        String[] diffs = layers.toArray(new String[0]);
        mount.overlayMount(diffs, containerDirectory + "/containerOverlay", containerDirectory + "/root");

    }

    public String genContainerID() {
        System.out.println("Generating container ID");
        return UUID.randomUUID().toString();
    }
}