package io.github.jshipit;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

@Parameters(commandDescription = "Create a container")
class CommandCreate {
    @Parameter(names = {"--name", "-n"}, description = "Name of the container")
    public String containerName;

    @Parameter(names = {"--image", "-i"}, description = "Image of the container")
    public String containerImage;

    @Parameter(names = {"--tag", "-t"}, description = "Tag of the container")
    public String containerTag;

    @Parameter(names = {"--api-repo", "-a"}, description = "API repository of the container")
    public String containerApiRepo;

    @Parameter(names = {"--repo", "-r"}, description = "Repository of the container")
    public String containerRepo;

}

@Parameters(commandDescription = "Pull a container image")
class CommandPull {
    @Parameter(names = {"--image", "-i"}, description = "Image of the container")
    public String containerImage;

    @Parameter(names = {"--tag", "-t"}, description = "Tag of the container")
    public String containerTag;

    @Parameter(names = {"--api-repo", "-a"}, description = "API repository of the container")
    public String containerApiRepo;

    @Parameter(names = {"--repo", "-r"}, description = "Repository of the container")
    public String containerRepo;

}
