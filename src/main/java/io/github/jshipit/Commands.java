// SPDX-License-Identifier: GPL-3.0-only

package io.github.jshipit;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.ArrayList;
import java.util.List;

@Parameters(commandDescription = "Open a shell in a container")
class CommandShell {
    @Parameter(names = {"--name", "-n"}, description = "Name of the container", required = true)
    public String containerName;

    @Parameter(names = {"--mount", "-m"}, description = "Mount a directory into the container", required = false)
    public List<String> containerMount = new ArrayList<>();

    @Parameter(names = "--help", help = true)
    private boolean help;
}

@Parameters(commandDescription = "Start a container")
class CommandStart {
    @Parameter(names = {"--name", "-n"}, description = "Name of the container", required = true)
    public String containerName;

    @Parameter(names = {"--command", "-c"}, description = "Command to run in the container", required = false)
    public String containerCommand = null;

    @Parameter(names = {"--mount", "-m"}, description = "Mount a directory into the container", required = false)
    public List<String> containerMount = new ArrayList<>();

    @Parameter(names = "--help", help = true)
    private boolean help;
}

@Parameters(commandDescription = "Create a container")
class CommandCreate {
    @Parameter(names = {"--name", "-n"}, description = "Name of the container")
    public String containerName;

    @Parameter(names = {"--image", "-i"}, description = "Image of the container")
    public String containerImage;

    @Parameter(names = {"--isolated", "-a"}, description = "If the container should be isolated from the host", required = false)
    public boolean containerIsolated = true;

    @Parameter(names = "--help", help = true)
    private boolean help;
}

@Parameters(commandDescription = "Pull a container image")
class CommandPull {
    @Parameter(names = {"--image", "-i"}, description = "Image of the container")
    public String containerImage;

    @Parameter(names = "--help", help = true)
    private boolean help;

}

@Parameters(commandDescription = "Delete a container")
class CommandDelete {
    @Parameter(names = {"--name", "-n"}, description = "Name of the container")
    public String containerName;

    @Parameter(names = "--help", help = true)
    private boolean help;
}
