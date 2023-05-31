// SPDX-License-Identifier: GPL-3.0-only

package io.github.jshipit;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/*
 * OCI Data Store
 * This class is responsible for managing the OCI data store
 */
public class OCIDataStore {

    private String path;
    private String databasePath;

    public OCIDataStore(String path) {
        this.path = path;
        this.databasePath = path + "/datastore.db";
        // Create OCI Data Store if it does not exist
        if (!Files.isDirectory(Path.of(path))) {
            createStore();
        }
        // Create OCI Data Store database if it does not exist
        if (!Files.exists(Path.of(this.databasePath))) {
            try {
                createStoreDatabase();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /*
     * Creates the OCI Data Store
     */
    private void createStore() {
        System.out.println("Creating OCI Data Store");
        Path path = Path.of(this.path);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            if (!(e instanceof FileAlreadyExistsException)) {
                System.out.println("Failed to create directory: " + path);
                e.printStackTrace();
            }
        }
    }

    /*
     * Creates the OCI Data Store database
     */
    private void createStoreDatabase() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        String url = "jdbc:sqlite:" + this.databasePath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                DatabaseMetaData meta = conn.getMetaData();
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                // We use two tables so that we can deduplicate blobs and keep track of containers that the user created
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS blobs (id INTEGER PRIMARY KEY AUTOINCREMENT, digest TEXT, path TEXT)");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS containers (containerID INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, path TEXT, image TEXT, tag TEXT, apiRepo TEXT, repo TEXT)");
                System.out.println("A new database has been created.");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * Adds a downloaded blob to the database
     *
     * @param blob The blob to add
     */
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

    /*
     * Checks if a blob is in the database
     *
     * @param blob The blob to check
     *
     * @return True if the blob is in the database, false otherwise
     */
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

    /*
     * Adds a container to the database
     *
     * @param path The path to the container
     * @param name The name of the container
     * @param image The image that the container uses
     * @param tag The image tag that the container uses
     * @param apiRepo The API repository of the container
     * @param repo The repository of the container
     */
    public void addContainerToDatabase(String path, String name, String image, String tag, String apiRepo, String repo) {
        String url = "jdbc:sqlite:" + this.databasePath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                statement.executeUpdate("INSERT INTO containers (name, path, image, tag, apiRepo, repo) VALUES ('" + name + "', '" + path + "', '" + image + "', '" + tag + "', '" + apiRepo + "', '" + repo + "')");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * Deletes a container from the database
     *
     * @param name The name of the container to delete
     */
    public void deleteContainerFromDatabase(String name) {
        String url = "jdbc:sqlite:" + this.databasePath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                statement.executeUpdate("DELETE FROM containers WHERE name = '" + name + "'");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /*
     * Checks if a container exists in the database
     *
     * @param name The name of the container to check
     *
     * @return True if the container exists, false otherwise
     */
    public boolean containerExists(String name) {
        String url = "jdbc:sqlite:" + this.databasePath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                ResultSet rs = statement.executeQuery("SELECT * FROM containers WHERE name = '" + name + "'");
                return rs.next();
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }

    /*
     * Gets the path to a container
     *
     * @param name The name of the container
     *
     * @return The path to the container
     */
    public String getContainerPath(String name) {
        String url = "jdbc:sqlite:" + this.databasePath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                ResultSet rs = statement.executeQuery("SELECT * FROM containers WHERE name = '" + name + "'");
                return rs.getString("path");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    /*
     * Gets the image of a container
     *
     * @param name The name of the container
     *
     * @return The image of the container
     */
    public String getContainerImage(String name) {
        String url = "jdbc:sqlite:" + this.databasePath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                ResultSet rs = statement.executeQuery("SELECT * FROM containers WHERE name = '" + name + "'");
                return rs.getString("image");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }


    /*
     * Gets the tag of a container
     *
     * @param name The name of the container
     *
     * @return The tag of the container
     */
    public String getContainerTag(String name) {
        String url = "jdbc:sqlite:" + this.databasePath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                ResultSet rs = statement.executeQuery("SELECT * FROM containers WHERE name = '" + name + "'");
                return rs.getString("tag");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }


    /*
     * Gets the API repository of a container
     *
     * @param name The name of the container
     *
     * @return The API repository of the container
     */
    public String getContainerApiRepo(String name) {
        String url = "jdbc:sqlite:" + this.databasePath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                ResultSet rs = statement.executeQuery("SELECT * FROM containers WHERE name = '" + name + "'");
                return rs.getString("apiRepo");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    /*
     * Gets the repository of a container
     *
     * @param name The name of the container
     *
     * @return The repository of the container
     */
    public String getContainerRepo(String name) {
        String url = "jdbc:sqlite:" + this.databasePath;

        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement statement = conn.createStatement();
                statement.setQueryTimeout(30);  // set timeout to 30 sec.

                ResultSet rs = statement.executeQuery("SELECT * FROM containers WHERE name = '" + name + "'");
                return rs.getString("repo");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    /*
     * Creates the directory for a container and adds it to the database
     *
     * @param image The image of the container
     * @param tag The tag of the container
     * @param name The name of the container
     * @param apiRepo The API repository of the container
     * @param repo The repository of the container
     *
     * @return The path to the container
     */
    public String createContainerDirectory(String image, String tag, String name, String apiRepo, String repo) {
        Path containerPath = Path.of(this.path+"/"+image+"/"+tag+"/"+name);
        try {
            Files.createDirectory(containerPath);
        } catch (IOException e) {
            System.out.println("Failed to create directory: " + containerPath);
            e.printStackTrace();
            return null;
        }
        addContainerToDatabase(containerPath.toString(), name, image, tag, apiRepo, repo);
        return containerPath.toString();
    }

    /*
     * Downlaods an oci-image, extracts it and adds it to the blob database
     *
     * @param apiRepo The API repository of the container
     * @param repo The repository of the container
     * @param image The image of the container
     * @param tag The tag of the container
     */
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

        JsonNode manifest = null;

        try {
            manifest = api.fetchManifestJson();
        } catch (IOException ignored) {} // Proper error handling is bloat

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

        List <String> layerDigests = new ArrayList<>(); // We store the blobs an image uses in a file, so that we know which blobs are used by which images
        for (JsonNode layer : layers) {
            try {
                if (!isBlobInDatabase(layer.get("digest").asText())) { // We only download blobs that are not already in the database
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

        // We download the config file of the image (I was too lazy to make a separate download function for that (having this part async is not good))
        try {
            api.fetchBlob(manifest.get("config").get("digest").asText(), this.path+"/"+image+"/"+tag, false, this.path+"/"+image+"/"+tag+"/config");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String getPath() {
        return path;
    }
}
