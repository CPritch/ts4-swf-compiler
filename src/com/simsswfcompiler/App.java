package com.simsswfcompiler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

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

@Command(name = "Sims SWF Compiler", mixinStandardHelpOptions = true, version = "0.1-alpha",
description = "Processes SWF files.")
public class App implements Runnable {
	@Option(names = {"-src"}, description = "The folder containing assets to import.", required = true)
    private String src;
	
	@Option(names = {"-dst"}, description = "The SWF file location.", required = true)
    private String dst;
	
	@Option(names = {"-out"}, description = "The filepath for saving the modified SWF.", required = true)
    private String out;
	
	private SimsSWF simsSwf;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println(String.format("Decompiling SWF: %s.", dst));
        simsSwf = new SimsSWF(dst);
        modifyActionScript();
        modifyTags();
        simsSwf.saveTo(out);
        System.out.println(String.format("Modified SWF saved to %s", out));
    }
    
    private void modifyActionScript() {
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
    }
    
    private void modifyTags() {
        Path tagsDir = Paths.get(src, "tags");
        try (Stream<Path> paths = Files.walk(tagsDir)) {
            paths
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json") || path.toString().endsWith(".yml"))
                .forEach(this::processTagFile);
        } catch (IOException e) {
            System.err.println("Error walking through tags directory.");
            e.printStackTrace();
        }
    }
    
    private void processTagFile(Path tagFile) {
    	try {
    		ObjectMapper objectMapper = new ObjectMapper();
    	    InputStream inputStream = getClass().getClassLoader().getResourceAsStream("TagModel.json");
    	    String tagModel = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    	    
    	    JsonNode tagNode = objectMapper.readTree(Files.readString(tagFile));
    		JsonNode schemaNode = objectMapper.readTree(tagModel);
    		
    		JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault();
    		JsonSchema schema = schemaFactory.getJsonSchema(schemaNode);
    		ProcessingReport report = schema.validate(tagNode);
    		if (report.isSuccess()) {
			   System.out.println(String.format("JSON config for %s is valid.", tagNode.get("instanceName")));
			   String srcInstanceName = tagNode.get("copy").get("instanceName").asText();
			   String srcInstanceType = tagNode.get("copy").get("instanceName").asText();
			   String dstInstanceName = tagNode.get("instanceName").asText();
			   String dstClassName = tagNode.get("className") != null ? tagNode.get("className").asText() : null;
			   simsSwf.copyTag(srcInstanceName, srcInstanceType, dstInstanceName, dstClassName);
    		} else {
			    System.err.println("JSON config is invalid for: " + tagFile);
			    for (ProcessingMessage item : report) {
			    	System.err.println(item.getMessage());
			    }
			    System.exit(1);
			}   
    		
    	} catch (IOException e) {
            System.err.println("Error reading tag file: " + tagFile.getFileName());
            System.err.println(e.toString());
            e.printStackTrace();
            System.exit(1);
        } catch (ProcessingException e) {
			System.err.println("Error processing JSON schema: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		}

    }
}
