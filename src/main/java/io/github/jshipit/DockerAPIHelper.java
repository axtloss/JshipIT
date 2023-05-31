// SPDX-License-Identifier: GPL-3.0-only

package io.github.jshipit;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class DockerAPIHelper {

    private String apiToken;
    private final String apiRepo;
    private String authURL;
    private String authService;
    private final String repository;
    private final String image;
    private final String tag;

    public DockerAPIHelper(String apiRepo, String repository, String image, String tag) {
        if (apiRepo.contains("registry.docker.io")) {
            apiRepo = apiRepo.replace("registry", "registry-1"); // Docker is just funny like that
        }
        this.apiRepo = apiRepo;
        this.repository = repository;
        this.image = image;
        this.tag = tag;
        this.authURL = "https://auth.docker.io/token";
        this.authService = "registry.docker.io";
        try {
            getAuthenticationUrl();
            apiToken = generateAPIToken();
        } catch (IOException | RuntimeException e) {
            e.printStackTrace();
        }
    }

    /*
     * Get the Authentication URL from the registry
     */
    public void getAuthenticationUrl() throws IOException {
        URL url_obj = null;
        try {
            url_obj = new URI("https://"+this.apiRepo+"/v2/").toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            e.printStackTrace();
        }

        assert url_obj != null;
        HttpURLConnection con = (HttpURLConnection) url_obj.openConnection();
        con.setRequestMethod("GET");
        con.connect();
        if (con.getResponseCode() == 401 ) {
            Map<String, List<String>> headers = con.getHeaderFields();
            List<String> authenticate = headers.get("www-authenticate");
            if (authenticate == null) {
                authenticate = headers.get("Www-Authenticate"); // Some registries (registry.getcryst.al) do this for some reason
            }
            assert authenticate != null;
            this.authURL = authenticate.get(0).replace("Bearer realm=", "").replace("\"", "").split(",")[0];
            this.authService = authenticate.get(0).replace("service=", "").replace("\"", "").split(",")[1];
        } else {
            this.authURL = "https://auth.docker.io/token"; // Just default to docker if the registry does not support or need authentication
            this.authService = "registry.docker.io";
        }
    }

    /*
     * Generate an API token from the authentication URL
     *
     * @return String API token
     */
    public String generateAPIToken() throws IOException, RuntimeException {
        URL url_obj = null;
        try {
            url_obj = new URI(this.authURL + "?scope=repository:" + this.repository + "/" + this.image + ":pull"  + "&service=" + this.authService).toURL();
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
            StringBuilder output = new StringBuilder();
            Scanner scanner = new Scanner(url_obj.openStream());
            while (scanner.hasNext()) {
                output.append(scanner.nextLine());
            }
            scanner.close();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode token = mapper.readTree(output.toString());
            return token.get("token").asText();
        }
    }

    /*
     * Fetch the manifest JSON from the registry
     *
     * @return JsonNode Manifest JSON
     */
    public JsonNode fetchManifestJson() throws IOException, RuntimeException {
        URL url_obj = null;
        try {
            url_obj = new URI("https://" + this.apiRepo + "/v2/" + this.repository + "/" + this.image + "/manifests/"+this.tag).toURL();
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
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();
            con.disconnect();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(content.toString());
        }
    }

    /*
     * Fetch a blob from a specific image
     *
     * @param digest Digest of the blob to fetch
     * @param tmpdir Directory to store the blob
     * @param extract Whether or not to extract the blob
     * @param renameTo Rename the blob to this name
     */
    public void fetchBlob(String digest, String tmpdir, boolean extract, String renameTo) throws IOException, RuntimeException {
        String url = "https://" + this.apiRepo + "/v2/" + this.repository + "/" + this.image + "/blobs/"+digest;
        String[][] headers = {{"Authorization", "Bearer "+this.apiToken}, {"Accept", "application/vnd/docker.distribution.manifest.v2+json"}};
        BlobDownloader downloader = new BlobDownloader(url, digest, headers, tmpdir, extract, renameTo);
        downloader.start();
    }

    public String getImage() {
        return image;
    }

    public String getTag() {
        return tag;
    }
}
