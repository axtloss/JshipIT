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

        OCIDataStore dataStore = new OCIDataStore(System.getenv("HOME") + "/.local/share/dataStore");

        if (commands.getParsedCommand() == null) {
            commands.usage();
            System.exit(1);
        } else if (commands.getParsedCommand().equals("create")) {
            if (commandCreate.containerName == null) {
                System.out.println("Container name is required");
                System.exit(1);
            }
            List<String> image = new ArrayList<String>(Arrays.asList(commandCreate.containerImage.split("/")));
            String containerImage = image.get(image.size() - 1).split(":")[0];
            String apiRepo = image.get(0);
            image.remove(0);
            image.remove(image.size() - 1);
            String containerRepo = String.join("/", image);

            switch (apiRepo) {
                case "docker.io":
                    apiRepo = "registry.docker.io";
                    break;
                case "ghcr.io":
                    apiRepo = "ghcr.io";
                    break;
                case "quay.io":
                    apiRepo = "quay.io";
                    break;
                default:
                    break;
            }


            ContainerManager containerManager = new ContainerManager(commandCreate.containerName, containerImage, commandCreate.containerImage.split(":")[1], apiRepo, containerRepo, dataStore);
            containerManager.createContainer();
        } else if (commands.getParsedCommand().equals("pull")) {

            List<String> image = new ArrayList<String>(Arrays.asList(commandPull.containerImage.split("/")));
            String containerImage = image.get(image.size() - 1).split(":")[0];
            String apiRepo = image.get(0);
            image.remove(0);
            image.remove(image.size() - 1);
            String containerRepo = String.join("/", image);

            switch (apiRepo) {
                case "docker.io":
                    apiRepo = "registry.docker.io";
                    break;
                case "ghcr.io":
                    apiRepo = "ghcr.io";
                    break;
                case "quay.io":
                    apiRepo = "quay.io";
                    break;
                default:
                    break;
            }
            System.out.println("Pulling image " + containerImage + " from " + apiRepo + "/" + containerRepo);
            dataStore.createImage(apiRepo, containerRepo, containerImage, commandPull.containerImage.split(":")[1]);
        } else if (commands.getParsedCommand().equals("start")) {
            ContainerManager containerManager = new ContainerManager(commandStart.containerName, commandStart.containerCommand, dataStore);
            containerManager.runCommand();
        } else if (commands.getParsedCommand().equals("shell")) {
            ContainerManager containerManager = new ContainerManager(commandShell.containerName, "/bin/sh", dataStore); // A proper linux system should always have /bin/sh, skill issue if it doesn't
            containerManager.runCommand();
        } else if (commands.getParsedCommand().equals("delete")) {
            ContainerManager containerManager = new ContainerManager(commandDelete.containerName, null, dataStore);
            containerManager.deleteContainer();
        }
    }
}