package io.github.jshipit;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.beust.jcommander.Parameter;

public class JshipIT {

    public JshipIT(String[] args) {
        CommandShell commandShell = new CommandShell();
        CommandStart commandStart = new CommandStart();
        CommandCreate commandCreate = new CommandCreate();
        CommandPull commandPull = new CommandPull();
        JCommander commands = JCommander.newBuilder()
                .addCommand("create", commandCreate)
                .addCommand("pull", commandPull)
                .addCommand("start", commandStart)
                .addCommand("shell", commandShell)
                .build();

        commands.parse(args);

        OCIDataStore dataStore = new OCIDataStore("./tmp");

        if (commands.getParsedCommand() == null) {
            commands.usage();
            System.exit(1);
        } else if (commands.getParsedCommand().equals("create")) {
            ContainerManager containerManager = new ContainerManager(commandCreate.containerName, commandCreate.containerImage, commandCreate.containerTag, commandCreate.containerApiRepo, commandCreate.containerRepo, dataStore);
            containerManager.createContainer();
        } else if (commands.getParsedCommand().equals("pull")) {
            dataStore.createImage(commandPull.containerApiRepo, commandPull.containerRepo, commandPull.containerImage, commandPull.containerTag);
        } else if (commands.getParsedCommand().equals("start")) {
            ContainerManager containerManager = new ContainerManager(commandStart.containerName, commandStart.containerCommand, dataStore);
            containerManager.runCommand();
        } else if (commands.getParsedCommand().equals("shell")) {
            ContainerManager containerManager = new ContainerManager(commandShell.containerName, "/bin/sh", dataStore); // A proper linux system should always have /bin/sh, skill issue if it doesn't
            containerManager.runCommand();
        }



    }
}