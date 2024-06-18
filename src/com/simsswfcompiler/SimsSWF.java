package com.simsswfcompiler;

import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.SwfOpenException;
import com.jpexs.decompiler.flash.abc.ScriptPack;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    	
    }
}
