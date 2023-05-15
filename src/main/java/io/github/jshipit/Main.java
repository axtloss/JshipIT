package io.github.jshipit;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        DockerAPIHelper api = new DockerAPIHelper("registry.docker.io", "https://auth.docker.io/token", "library", "archlinux", "latest");
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
            return;
        }

        JsonNode layers = manifest.get("layers");
        for (JsonNode layer : layers) {
            System.out.println("Layer: " + layer);
            try {
                api.fetchBlob(layer.get("digest").asText(), path.toString());
            } catch (IOException e) {

            }
        }
    }
}