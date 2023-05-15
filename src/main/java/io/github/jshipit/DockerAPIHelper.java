package io.github.jshipit;



import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class DockerAPIHelper {

    private String apiToken;
    private String apiRepo;
    private String authURL;
    private String repository;
    private String image;

    private String tag;

    public DockerAPIHelper(String apiRepo, String authURL, String repository, String image, String tag) {
        System.out.println("DockerAPIHelper constructor");
        this.apiRepo = apiRepo;
        this.repository = repository;
        this.authURL = authURL;
        this.image = image;
        this.tag = tag;
        try {
            apiToken = generateAPIToken();
        } catch (IOException | RuntimeException e) {
            System.out.println("IOException | RuntimeException");
            e.printStackTrace();
        }
    }

    public String generateAPIToken() throws IOException, RuntimeException {
        URL url_obj = null;
        try {
            url_obj = new URI(this.authURL + "?scope=repository:" + this.repository + "/" + this.image + ":pull"  + "&service=" + this.apiRepo).toURL();
            System.out.println(url_obj.toString());
        } catch (URISyntaxException | MalformedURLException e) {
            System.out.println("URISyntaxException | MalformedURLException");
            e.printStackTrace();
        }


        assert url_obj != null;
        HttpURLConnection con = (HttpURLConnection) url_obj.openConnection();
        con.setRequestMethod("GET");
        con.connect();
        if (con.getResponseCode() != 200) {
            System.out.println("Error: " + con.getResponseCode());
            throw new RuntimeException("Failed : HTTP error code : "
                    + con.getResponseCode());
        } else {
            System.out.println("Success: " + con.getResponseCode());
            String output = "";
            Scanner scanner = new Scanner(url_obj.openStream());
            while (scanner.hasNext()) {
                output += scanner.nextLine();
            }
            scanner.close();
            System.out.println(output);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode token = mapper.readTree(output);
            System.out.println(token.get("token").asText());
            return token.get("token").asText();
        }
    }

    public JsonNode fetchManifestJson() throws IOException, RuntimeException {
        URL url_obj = null;
        try {
            String repo = this.apiRepo.replace("registry", "registry-1");
            url_obj = new URI("https://" + repo + "/v2/" + this.repository + "/" + this.image + "/manifests/"+this.tag).toURL();
            System.out.println(url_obj.toString());
        } catch(URISyntaxException | MalformedURLException e) {
            System.out.println("URISyntaxException | MalformedURLException");
            e.printStackTrace();
        }

        assert url_obj != null;
        HttpURLConnection con = (HttpURLConnection) url_obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "Bearer " + this.apiToken);
        con.setRequestProperty("Accept", "application/vnd.docker.distribution.manifest.v2+json");
        con.connect();

        if (con.getResponseCode() != 200) {
            System.out.println("Error: " + con.getResponseCode());
            throw new RuntimeException("Failed : HTTP error code : "
                    + con.getResponseCode());
        } else {
            System.out.println("Success: " + con.getResponseCode());
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer content = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode manifest = mapper.readTree(content.toString());
            return manifest;
        }
    }

    public void fetchBlob(String digest, String tmpdir) throws IOException, RuntimeException {
        URL url_obj = null;
        try {
            String repo = this.apiRepo.replace("registry", "registry-1");
            url_obj = new URI("https://" + repo + "/v2/" + this.repository + "/" + this.image + "/blobs/"+digest).toURL();
            System.out.println(url_obj.toString());
        } catch(URISyntaxException | MalformedURLException e) {
            System.out.println("URISyntaxException | MalformedURLException");
            e.printStackTrace();
        }

        assert url_obj != null;
        HttpURLConnection con = (HttpURLConnection) url_obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "Bearer " + this.apiToken);
        con.setRequestProperty("Accept", "application/vnd.docker.distribution.manifest.v2+json");
        con.connect();

        if (con.getResponseCode() != 200) {
            System.out.println("Error: " + con.getResponseCode());
            throw new RuntimeException("Failed : HTTP error code : "
                    + con.getResponseCode());
        } else {
            System.out.println("Success: " + con.getResponseCode());
        }
        // Stream file download and output progress
        InputStream inputStream = con.getInputStream();
        FileOutputStream outputStream = new FileOutputStream(tmpdir+"/"+digest.replace("sha256:", "")+"layer.tar");

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();
        System.out.println("File downloaded successfully.");
    }

    public String getApiToken() {
        return apiToken;
    }

    public String getImage() {
        return image;
    }

    public String getTag() {
        return tag;
    }
}
