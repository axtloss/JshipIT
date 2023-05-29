package io.github.jshipit;

import com.beust.jcommander.JCommander;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class JshipIT {

    public JshipIT(String[] args) {
        CommandShell commandShell = new CommandShell();
        CommandStart commandStart = new CommandStart();
        CommandCreate commandCreate = new CommandCreate();
        CommandPull commandPull = new CommandPull();
        CommandDelete commandDelete = new CommandDelete();
        JCommander commands = JCommander.newBuilder()
                .addCommand("create", commandCreate)
                .addCommand("pull", commandPull)
                .addCommand("start", commandStart)
                .addCommand("shell", commandShell)
                .addCommand("delete", commandDelete)
                .build();

        commands.parse(args);

        OCIDataStore dataStore = new OCIDataStore(System.getenv("HOME") + "/.local/share/jshipit/dataStore");

        if (commands.getParsedCommand() == null) {
            commands.usage();
            System.exit(1);
        } else if (commands.getParsedCommand().equals("create")) {
            if (commandCreate.containerName == null) {
                System.out.println("Container name is required");
                System.exit(1);
            }

            // Parse the image name into the registry, repo, image name and tag
            List<String> image = new ArrayList<String>(Arrays.asList(commandCreate.containerImage.split("/")));
            String containerImage = image.get(image.size() - 1).split(":")[0];
            String apiRepo = image.get(0);
            image.remove(0);
            image.remove(image.size() - 1);
            String containerRepo = String.join("/", image);

            // Convert the registry name to the OCI registry name
            if (apiRepo.equalsIgnoreCase("docker.io")) {
                apiRepo = "registry.docker.io";
            }

            ContainerManager containerManager = new ContainerManager(commandCreate.containerName, containerImage, commandCreate.containerImage.split(":")[1], apiRepo, containerRepo, null, dataStore);
            containerManager.createContainer();
        } else if (commands.getParsedCommand().equals("pull")) {

            // Parse the image name into the registry, repo, image name and tag
            List<String> image = new ArrayList<String>(Arrays.asList(commandPull.containerImage.split("/")));
            String containerImage = image.get(image.size() - 1).split(":")[0];
            String apiRepo = image.get(0);
            image.remove(0);
            image.remove(image.size() - 1);
            String containerRepo = String.join("/", image);

            // Convert the registry name to the OCI registry name
            if (apiRepo.equalsIgnoreCase("docker.io")) {
                apiRepo = "registry.docker.io";
            }

            System.out.println("Pulling image " + containerImage + " from " + apiRepo + "/" + containerRepo);
            dataStore.createImage(apiRepo, containerRepo, containerImage, commandPull.containerImage.split(":")[1]);
        } else if (commands.getParsedCommand().equals("start")) {
            ContainerManager containerManager = new ContainerManager(commandStart.containerName, commandStart.containerCommand, commandStart.containerMount, dataStore);
            containerManager.runCommand();
        } else if (commands.getParsedCommand().equals("shell")) {
            ContainerManager containerManager = new ContainerManager(commandShell.containerName, "/bin/sh", commandShell.containerMount, dataStore); // A proper linux system should always have /bin/sh, skill issue if it doesn't
            containerManager.runCommand();
        } else if (commands.getParsedCommand().equals("delete")) {
            ContainerManager containerManager = new ContainerManager(commandDelete.containerName, null, null, dataStore);
            containerManager.deleteContainer();
        }
    }
}