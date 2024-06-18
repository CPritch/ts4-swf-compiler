package com.simsswfcompiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.SwfOpenException;
import com.jpexs.decompiler.flash.abc.ScriptPack;
import com.jpexs.decompiler.flash.configuration.Configuration;
import com.jpexs.decompiler.flash.flexsdk.MxmlcAs3ScriptReplacer;
import com.jpexs.decompiler.flash.gui.Main;
import com.jpexs.decompiler.flash.importers.As3ScriptReplaceException;
import com.jpexs.decompiler.flash.importers.As3ScriptReplaceExceptionItem;
import com.jpexs.decompiler.flash.importers.As3ScriptReplacerFactory;
import com.jpexs.decompiler.flash.importers.As3ScriptReplacerInterface;
import com.jpexs.decompiler.flash.importers.FFDecAs3ScriptReplacer;

public class SimsSWF {
    
    private SWF swf;
    private List<ScriptPack> packs;
    
    public SimsSWF(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            this.swf = new SWF(fis, true);
            this.packs = swf.getAS3Packs();            
        } catch(SwfOpenException ex) {
        	ex.printStackTrace();
        	// Handle exceptions appropriately
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            // Handle exceptions appropriately
        }
    }
    
    public void saveTo(String filePath) {
    	if (swf == null) {
        	System.err.println("SWF not open");
        	System.exit(1);
        }
    	
    	File outFile = new File(filePath);
    	try {
            try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(outFile))) {
                swf.saveTo(fos);
            }
        } catch (IOException e) {
            System.err.println("I/O error during writing: " + e.toString());
            System.exit(2);
        }
    }
    
    public void addAS3(String fileName, String fileContent) {
        if (swf == null) {
        	System.err.println("SWF not open");
        	System.exit(1);
        }
        if (packs == null) {
        	System.err.println("AS3 not decompiled");
        	System.exit(1);
        }
        String className = getClassNameFromFileContent(fileContent);
        boolean found = false;
        for (ScriptPack pack : packs) {
        	if (pack.getClassPath().toString().equals(className)) {
        		found = true;
        		System.out.println(String.format("Replacing %s with %s", pack.getClassPath().toString(), fileName));
        		replaceAS3(fileContent, pack);
        	}
        }
        if (found == false) {
        	System.err.println(
    			String.format("Could not find a matching class for %s. Adding new AS3 is unsupported for now.", className)
        	);
        	System.exit(1);
        }
    }
    
    private static String getClassNameFromFileContent(String fileContent) {
    	Pattern packagePattern = Pattern.compile("package\\s+([\\w\\.]+)\\s*\\{");
        Pattern classPattern = Pattern.compile("public\\s+(final\\s+)?class\\s+(\\w+)");

        Matcher packageMatcher = packagePattern.matcher(fileContent);
        Matcher classMatcher = classPattern.matcher(fileContent);

        if (packageMatcher.find() && classMatcher.find()) {
        	return packageMatcher.group(1) + "." + classMatcher.group(2);
        }
        throw new Error("ClassName could not be parsed");
    }
    
    private static void replaceAS3(String fileContent, ScriptPack pack) {
    	As3ScriptReplacerInterface scriptReplacer = As3ScriptReplacerFactory.createByConfig(false);
    	if (!scriptReplacer.isAvailable()) {
            System.err.println("Current script replacer is not available.");
            if (scriptReplacer instanceof FFDecAs3ScriptReplacer) {
                System.err.println("Current replacer: FFDec");
                final String adobePage = "http://www.adobe.com/support/flashplayer/downloads.html";
                System.err.println("For ActionScript 3 direct editation, a library called \"PlayerGlobal.swc\" needs to be downloaded from Adobe homepage:");
                System.err.println(adobePage);
                System.err.println("Download the library called PlayerGlobal(.swc), and place it to directory");
                System.err.println(Configuration.getFlashLibPath().getAbsolutePath());
            } else if (scriptReplacer instanceof MxmlcAs3ScriptReplacer) {
                System.err.println("Current replacer: Flex SDK");
                final String flexPage = "http://www.adobe.com/devnet/flex/flex-sdk-download.html";
                System.err.println("For ActionScript 3 direct editation, Flex SDK needs to be download");
                System.err.println(flexPage);
                System.err.println("Download FLEX Sdk, unzip it to some directory and set its directory path in the configuration");
            }
            System.exit(1);
        }
    	
    	try {
            pack.abc.replaceScriptPack(scriptReplacer, pack, fileContent, Main.getDependencies(pack.abc.getSwf()));
        } catch (As3ScriptReplaceException asre) {
            for (As3ScriptReplaceExceptionItem item : asre.getExceptionItems()) {
                String r = "%error% on line %line%, column %col%, file: %file%".replace("%error%", "" + item.getMessage());
                r = r.replace("%line%", Long.toString(item.getLine()));
                r = r.replace("%file%", "" + item.getFile());
                r = r.replace("%col%", "" + item.getCol());
                System.err.println(r);
            }
            System.exit(1);
        } catch (InterruptedException | IOException ex) {
        	System.err.println("Error getting script dependencies: " + ex.toString());
        }
    }
}
