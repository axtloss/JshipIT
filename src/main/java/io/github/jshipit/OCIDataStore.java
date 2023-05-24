package io.github.jshipit;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OCIDataStore {

    private String path;
    private String databasePath;

    public OCIDataStore(String path) {
        this.path = path;
        this.databasePath = path + "/datastore.db";
        createStore();
        try {
            createStoreDatabase();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    private void createStore() {
        System.out.println("Creating OCI Data Store");
        Path path = Path.of(this.path);
        try {
            Files.createDirectory(path);
        } catch (IOException e) {
            if (!(e instanceof FileAlreadyExistsException)) {
                System.out.println("Failed to create directory: " + path);
                e.printStackTrace();
            }
        }
    }

    private void createStoreDatabase() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        String url = "jdbc:sqlite:" + this.databasePath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                statement.executeUpdate("CREATE TABLE IF NOT EXISTS blobs (id INTEGER PRIMARY KEY AUTOINCREMENT, digest TEXT, path TEXT)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS containers (containerID INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, id TEXT, path TEXT)");
                System.out.println("A new database has been created.");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void addBlobToDatabase(String blob) {
        String url = "jdbc:sqlite:" + this.databasePath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                statement.executeUpdate("INSERT INTO blobs (digest, path) VALUES ('" + blob + "', '" + this.path + "/blobs/" + blob + "')");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean isBlobInDatabase(String blob) {
        String url = "jdbc:sqlite:" + this.databasePath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                ResultSet rs = statement.executeQuery("SELECT * FROM blobs WHERE digest = '" + blob + "'");
                return rs.next();
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    public void addContainerToDatabase(String path, String name, String id) {
        String url = "jdbc:sqlite:" + this.databasePath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                statement.executeUpdate("INSERT INTO containers (name, id, path) VALUES ('" + name + "', '" + id + "', '" + path + "')");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public String getContainerPath(String name, String id) {
        String url = "jdbc:sqlite:" + this.databasePath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                ResultSet rs = statement.executeQuery("SELECT * FROM containers WHERE name = '" + name + "' AND id = '" + id + "'");
                return rs.getString("path");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public String createContainerDirectory(String image, String tag, String name, String id) {
        Path containerPath = Path.of(this.path+"/"+image+"/"+tag+"/"+name+"_"+id);
        try {
            Files.createDirectory(containerPath);
        } catch (IOException e) {
            System.out.println("Failed to create directory: " + containerPath);
            e.printStackTrace();
            return null;
        }
        addContainerToDatabase(containerPath.toString(), name, id);
        return containerPath.toString();
    }

    public void createImage(String apiRepo, String repo, String image, String tag) {

        Path imgPath = Path.of(this.path+"/"+image);
        try {
            Files.createDirectory(imgPath);
        } catch (IOException e) {
            System.out.println("Failed to create directory: " + imgPath);
        }

        Path tagPath = Path.of(this.path+"/"+image+"/"+tag);
        try {
            Files.createDirectory(tagPath);
        } catch (IOException e) {
            System.out.println("Failed to create directory: " + tagPath);
            e.printStackTrace();
            return;
        }

        DockerAPIHelper api = new DockerAPIHelper(apiRepo, repo, image, tag);

        System.out.println("API Token: " + api.getApiToken());

        JsonNode manifest = null;

        try {
            manifest = api.fetchManifestJson();
        } catch (IOException ignored) {} // Proper error handling is bloat

        System.out.println("Manifest: " + manifest);

        Path path = Path.of(this.path+"/"+api.getImage()+"/"+api.getTag());
        try {
            Files.createDirectory(path);
        } catch (IOException e) {
            if (!(e instanceof FileAlreadyExistsException)) {
                System.out.println("Failed to create directory: " + path);
                e.printStackTrace();
                return;
            }
        }

        assert manifest != null;
        JsonNode layers = manifest.get("layers");
        String layerpath = this.path+"/blobs";
        try {
            Files.createDirectory(Path.of(layerpath));
        } catch (IOException e) {
            if (!(e instanceof FileAlreadyExistsException)) {
                System.out.println("Failed to create directory: " + layerpath);
                e.printStackTrace();
                return;
            }
        }
        List <String> layerDigests = new ArrayList<>();
        for (JsonNode layer : layers) {
            System.out.println("Layer: " + layer);

            try {
                if (!isBlobInDatabase(layer.get("digest").asText())) {
                    api.fetchBlob(layer.get("digest").asText(), layerpath, true, null);
                    addBlobToDatabase(layer.get("digest").asText());
                    layerDigests.add(layer.get("digest").asText());
                } else {
                    System.out.println("Blob already in database: " + layer.get("digest").asText());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Files.write(Path.of(this.path+"/"+image+"/"+tag+"/layers"), layerDigests);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            api.fetchBlob(manifest.get("config").get("digest").asText(), this.path+"/"+image+"/"+tag, false, this.path+"/"+image+"/"+tag+"/config");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDatabasePath() {
        return databasePath;
    }

    public void setDatabasePath(String databasePath) {
        this.databasePath = databasePath;
    }
}
