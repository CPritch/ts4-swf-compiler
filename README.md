# Sims SWF Compiler

A command-line tool for modifying and compiling SWF files used in The Sims 4 (TS4).

I've thrown this together to simplify the build process for [TS4 Ultimate UX Mod](https://github.com/CPritch/ts4-ultimate-ux).
Since I've built this to do exactly what I need I'd advise against using this for your own project quite yet.

## Features

- Decompile/Recompile SWF files using [FFDEC](https://github.com/jindrapetrik/jpexs-decompiler)
- Replace ActionScript 3 (AS3) code based on AS3 package + classname
- Clone and modify PlaceObject tags based on a [custom JSON schema](./src/main/java/resources/TagModel.json)

## Prerequisites

- Java 8 or later
- Maven (for building the project)

## Installation

1. Download the Jar from [Releases](https://github.com/CPritch/ts4-swf-compiler/releases)
2. [Use it](#usage)

## Building

1. Clone the repository:

```
git clone https://github.com/cpritch/sims-swf-compiler.git
```

2. Build the project:

```
cd sims-swf-compiler
mvn clean package
```

## Usage

```
java -jar target/sims-swf-compiler.jar -src /path/to/src -dst /path/to/input.swf -out /path/to/output.swf
```

### Command-line Options

- `-src`: The folder containing assets to import (ActionScript files and tag json definitions).
- `-dst`: The input SWF file location.
- `-out`: The filepath for saving the modified SWF.

## Configuration

The tool expects the following directory structure under the `-src` folder:

```
src/
├── scripts/
│   ├── Class1.as
│   ├── folder1/
│   │   ├── Class2.as
│   │   └── ...
│   └── ...
└── tags/
    ├── tag1.json
    ├── tag2.json
    └── ...
```

- `scripts/`: Contains ActionScript 3 (AS3) files to be replaced or added to the SWF. Correct script to replace is determined automatically from AS3 code.
- `tags/`: Contains tag configuration files in JSON.

## Tag Configuration

The tag configuration files must follow a specific JSON schema. See the [Tag Configuration Schema](./src/main/java/resources/TagModel.json) for more details.

## Contributing

Contributions are welcome! Please follow the [contributing guidelines]()(Coming Soon!) for more information.

## License

This project is licensed under the [MIT License](./LICENSE).
```

This README provides an overview of the project, its features, installation instructions, usage examples, and information about configuration and contributing. You can customize and expand on this structure based on your project's specific requirements.

Additionally, you should consider creating separate documentation files for the tag configuration schema and contributing guidelines, as referenced in the README.