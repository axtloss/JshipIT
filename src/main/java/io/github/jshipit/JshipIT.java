package io.github.jshipit;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JshipIT {

    public int finishedCount;
    public int downloadThreads;

    public JshipIT(String[] args) {
        DockerAPIHelper api = new DockerAPIHelper("registry.getcryst.al","crystal/misc", "docker", "latest");
        JsonNode manifest = null;

        System.out.println("API Token: " + api.getApiToken());
        try {
            manifest = api.fetchManifestJson();
        } catch (IOException e) {

        }

        System.out.println("Manifest: " + manifest);

        Path path = Path.of("./tmp_"+api.getImage()+"_"+api.getTag());
        try {
            Files.createDirectory(path);
        } catch (IOException e) {
            System.out.println("Failed to create directory: " + path);
            e.printStackTrace();
            return;
        }

        finishedCount = 0;
        downloadThreads = 0;

        JsonNode layers = manifest.get("layers");
        for (JsonNode layer : layers) {
            System.out.println("Layer: " + layer);
            try {
                api.fetchBlob(layer.get("digest").asText(), path.toString());
                downloadThreads = downloadThreads+1;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}