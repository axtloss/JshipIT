package io.github.jshipit;

import me.tongfei.progressbar.ProgressBar;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

public class BlobDownloader extends Thread {

    private final String url;
    private final String digest;
    private final String[][] headers;
    private final String tmpdir;

    public BlobDownloader(String url, String digest, String[][] headers, String tmpdir) {
        this.url = url;
        this.digest = digest;
        this.headers = headers;
        this.tmpdir = tmpdir;
    }

    public void run() throws RuntimeException {
        URL url_obj = null;
        try {
            url_obj = new URI(this.url).toURL();
            System.out.println(url_obj.toString());
        } catch(URISyntaxException | MalformedURLException e) {
            System.out.println("URISyntaxException | MalformedURLException");
            e.printStackTrace();
        }

        assert url_obj != null;
        HttpURLConnection con;

        try {
            con = (HttpURLConnection) url_obj.openConnection();
            con.setRequestMethod("GET");
            for (String[] header : this.headers) {
                con.setRequestProperty(header[0], header[1]);
            }
            con.connect();

            if (con.getResponseCode() != 200) {
                System.out.println("Error: " + con.getResponseCode());
                throw new RuntimeException("Failed : HTTP error code : "
                        + con.getResponseCode());
            } else {
                System.out.println("Success: " + con.getResponseCode());
            }
        } catch (IOException e) {
            System.out.println("Failed to connect");
            e.printStackTrace();
            return;
        }


        int fileSize = con.getContentLength();

        try (InputStream in = con.getInputStream();
             OutputStream out = new FileOutputStream(tmpdir + "/" + digest.replace("sha256:", "") + "layer.tar")) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            try (ProgressBar pb = new ProgressBar("Download blob "+digest.replace("sha256:", ""), fileSize)) {
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    pb.stepBy(bytesRead);
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to download blob "+digest);
            e.printStackTrace();
        }
    }
}
