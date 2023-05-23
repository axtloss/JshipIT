package io.github.jshipit;

public class ImageBuilder {

    private String[] layers;

    public ImageBuilder(String[] layers) {
        this.layers = layers;
    }

    /*
        * Assembles a tarball from the layers
     */
    public void assemble() {

        System.out.println("Assembling image");
    }

}
