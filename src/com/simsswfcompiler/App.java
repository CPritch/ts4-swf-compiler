package com.simsswfcompiler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Command(name = "Sims SWF Compiler", mixinStandardHelpOptions = true, version = "0.1-alpha",
        description = "Processes SWF files.")
public class App implements Runnable {
    @Option(names = {"-src"}, description = "The folder containing assets to import.", required = true)
    private String srcDir;

    @Option(names = {"-dst"}, description = "The SWF file location.", required = true)
    private String dstFile;

    @Option(names = {"-out"}, description = "The filepath for saving the modified SWF.", required = true)
    private String outFile;

    @Option(names = {"-verbose"}, description = "Enable verbose logging.")
    private boolean verbose;

    private SimsSWF simsSwf;
    private ObjectMapper objectMapper;
    private JsonSchema jsonSchema;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        try {
            initializeComponents();
            logMessage(String.format("Decompiling SWF: %s.", dstFile));
            simsSwf = new SimsSWF(Paths.get(dstFile));
            modifyActionScript();
            modifyTags();
            simsSwf.saveTo(Paths.get(outFile));
            logMessage(String.format("Modified SWF saved to %s", outFile));
        } catch (IOException | ProcessingException e) {
            logError("Error occurred during processing: " + e.getMessage());
        }
    }

    private void initializeComponents() throws IOException, ProcessingException {
        objectMapper = new ObjectMapper();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("TagModel.json");
        String tagModel = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        JsonNode schemaNode = objectMapper.readTree(tagModel);
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault();
        jsonSchema = schemaFactory.getJsonSchema(schemaNode);
    }

    private void modifyActionScript() {
        Path scriptsDir = Paths.get(srcDir, "scripts");
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
                            logError("Error reading file: " + fileName + " - " + e.getMessage());
                        }
                        simsSwf.addAS3(fileName, fileContent);
                    });
        } catch (IOException e) {
            logError("Error walking through scripts directory: " + e.getMessage());
        }
    }

    private void modifyTags() {
        Path tagsDir = Paths.get(srcDir, "tags");
        try (Stream<Path> paths = Files.walk(tagsDir)) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json") || path.toString().endsWith(".yml"))
                    .forEach(this::processTagFile);
        } catch (IOException e) {
            logError("Error walking through tags directory: " + e.getMessage());
        }
    }

    private void processTagFile(Path tagFile) {
        try {
            JsonNode tagNode = objectMapper.readTree(Files.readString(tagFile));
            ProcessingReport report = jsonSchema.validate(tagNode);
            if (report.isSuccess()) {
                logInfo(String.format("JSON config for %s is valid.", tagNode.get("instanceName")));
                String srcInstanceName = tagNode.get("copy").get("instanceName").asText();
                String srcInstanceType = tagNode.get("copy").get("type").asText();
                String dstInstanceName = tagNode.get("instanceName").asText();
                String dstClassName = tagNode.get("className") != null ? tagNode.get("className").asText() : null;
                simsSwf.copyTag(srcInstanceName, srcInstanceType, dstInstanceName, dstClassName);
            } else {
                logError("JSON config is invalid for: " + tagFile);
                for (ProcessingMessage item : report) {
                    logError(item.getMessage());
                }
            }
        } catch (IOException e) {
            logError("Error reading tag file: " + tagFile.getFileName() + " - " + e.getMessage());
        } catch (ProcessingException e) {
        	logError("Error processing tag json: " + tagFile.getFileName() + " - " + e.getMessage());
		}
    }
    
    private void logMessage(String message) {
    	System.out.println(message);
    }

    private void logInfo(String message) {
        if (verbose) {
        	logMessage(message);
        }
    }

    private void logError(String message) {
        System.err.println(message);
    }
}