package com.simsswfcompiler;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.stream.Stream;

@Command(name = "Sims SWF Compiler", mixinStandardHelpOptions = true, version = "0.1-alpha",
description = "Processes SWF files.")
public class App implements Runnable {
	@Option(names = {"-src"}, description = "The folder containing assets to import.", required = true)
    private String src;
	
	@Option(names = {"-dst"}, description = "The SWF file location.", required = true)
    private String dst;
	
	@Option(names = {"-out"}, description = "The filepath of the modified SWF file.", required = true)
    private String out;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println(String.format("Decompiling SWF: %s.", dst));
        SimsSWF simsSwf = new SimsSWF(dst);
        
        // Read .as files from src/scripts
        Path scriptsDir = Paths.get(src, "scripts");
        try (Stream<Path> paths = Files.walk(scriptsDir)) {
            paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".as"))
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String fileContent = "";
                    try {
                        fileContent = Files.readString(path);
                    } catch (IOException e) {
                        System.err.println("Error reading file: " + fileName);
                        e.printStackTrace();
                    }
                    simsSwf.addAS3(fileName, fileContent);
                });
        } catch (IOException e) {
            System.err.println("Error walking through scripts directory.");
            e.printStackTrace();
        }
        
        System.out.println("SWF compilation complete");
    }
}
