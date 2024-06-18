package com.simsswfcompiler;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "Sims SWF Editor", mixinStandardHelpOptions = true, version = "0.1-alpha",
description = "Processes SWF files.")
public class App implements Runnable {
	private static final String APP_NAME = "Sims SWF Editor";
	
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
        simsSwf.addActionScript();
        System.out.println(String.format("SWF compilation complete", APP_NAME));
    }
}
