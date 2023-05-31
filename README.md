# JshipIT

a docker "reimplementation" in java.

supported/planned features:
- `jshipit pull` - pull a docker image
- `jshipit run` - run a command in a container
- `jshipit shell` - open a shell in a container
- `jshipit create` - create a container
- `jshipit delete` - delete a container


## Dependencies
NOTE: JShipIT only works on linux using the kernel >= 5.11.0

JShipIT requires the following dependencies:
- Java >= 17
- unshare
- bwrap
- maven

## Building
```bash
git clone https://github.com/axtloss/jshipit.git
cd jshipit
mvn compile package
```

## Usage

### Pulling an image
```bash
java -jar JavaShipit-1.0-SNAPSHOT-jar-with-dependencies.jar pull -i <image>
```

### Creating a container
```bash
java -jar JavaShipit-1.0-SNAPSHOT-jar-with-dependencies.jar create -i <image> -n <name>
```

### Running a command in a container
```bash
java -jar JavaShipit-1.0-SNAPSHOT-jar-with-dependencies.jar run -n <name> -c <command>
```

### Opening a shell in a container
```bash
java -jar JavaShipit-1.0-SNAPSHOT-jar-with-dependencies.jar shell -n <name>
```