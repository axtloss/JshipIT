package io.github.jshipit;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JshipIT {

    public JshipIT() {
        /*
        DockerAPIHelper api = new DockerAPIHelper("registry.getcryst.al","crystal/misc", "docker", "latest");
        JsonNode manifest = null;

        System.out.println("API Token: " + api.getApiToken());
        try {
            manifest = api.fetchManifestJson();
        } catch (IOException ignored) {} // Proper error handling is bloat

        System.out.println("Manifest: " + manifest);

        Path path = Path.of("./tmp_"+api.getImage()+"_"+api.getTag());
        try {
            Files.createDirectory(path);
        } catch (IOException e) {
            System.out.println("Failed to create directory: " + path);
            e.printStackTrace();
            return;
        }

        assert manifest != null;
        JsonNode layers = manifest.get("layers");
        for (JsonNode layer : layers) {
            System.out.println("Layer: " + layer);
            try {
                api.fetchBlob(layer.get("digest").asText(), path.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

         */

        OCIDataStore dataStore = new OCIDataStore("./tmp");
        //dataStore.createImage("registry.docker.io","library", "bash", "devel-alpine3.18");
        ContainerManager containerManager = new ContainerManager("testContainer", "bash", "devel-alpine3.18", "bash", "registry.docker.io", "library", dataStore);
        containerManager.createContainer();
        containerManager.startContainer();
    }
}