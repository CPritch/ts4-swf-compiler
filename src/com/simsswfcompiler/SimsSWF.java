package com.simsswfcompiler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jpexs.decompiler.flash.ReadOnlyTagList;
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
import com.jpexs.decompiler.flash.tags.Tag;
import com.jpexs.decompiler.flash.tags.base.PlaceObjectTypeTag;
import com.jpexs.decompiler.flash.timeline.Timelined;

public class SimsSWF {
    
    private SWF swf;
    private List<ScriptPack> packs;
    
    public SimsSWF(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            this.swf = new SWF(fis, true);
            this.packs = swf.getAS3Packs();            
        } catch(SwfOpenException ex) {
        	ex.printStackTrace();
            System.err.println("Error opening swf: " + ex.toString());
            System.exit(1);
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            System.err.println("Error reading swf file: " + ex.toString());
            System.exit(1);
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
            System.err.println("Error writing: " + e.toString());
            System.exit(1);
        }
    }
    
    public void copyTag(String srcInstanceName, String srcTagType, String dstInstanceName, String dstClassName) {
		List<Tag> foundTags = findTag(swf, srcInstanceName);
		if (foundTags.size() == 0) {
			System.err.println("Error: could not find tag with name: " + srcInstanceName);
			System.exit(1);
		}
		if (foundTags.size() > 1) {
			System.err.println(
				String.format("Error: found %s tags for %s, please be more specific", foundTags.size(), srcInstanceName)
			);
			System.exit(1);
		}
		PlaceObjectTypeTag foundTag = (PlaceObjectTypeTag)foundTags.getFirst();
		System.out.println("Found matching tag: " + foundTag.getName());
		try {
			PlaceObjectTypeTag copiedTag = (PlaceObjectTypeTag)foundTag.cloneTag();
			copiedTag.setInstanceName(dstInstanceName);
			if (dstClassName != null && dstClassName.length() > 0) {
				copiedTag.setClassName(dstClassName);
			}
			copiedTag.setModified(true);
			Timelined timeline = foundTag.getTimelined();
			timeline.addTag(timeline.indexOfTag(foundTag) + 1, copiedTag);
			System.out.println("Created new tag: " + copiedTag.getInstanceName());
		} catch(IOException | InterruptedException ex) {
			System.err.println("Error copying tag: " + ex.toString());
		}
    }
    
    public List<Tag> findTag(Timelined parent, String name) {
    	ReadOnlyTagList tags = parent.getTags();
    	List<Tag> foundTags = new ArrayList<Tag>();
    	for (Tag t : tags) {
    		if (t instanceof Timelined) {
    			foundTags.addAll(findTag((Timelined) t, name));
    		} else if(t instanceof PlaceObjectTypeTag) {
    			PlaceObjectTypeTag pt = (PlaceObjectTypeTag) t;
    			String tagInstanceName = pt.getInstanceName();
    			if (tagInstanceName != null && tagInstanceName.contains(name)) {
    				foundTags.add(pt);
    			}
    		}
    	}
    	return foundTags;
    }
    
    public void addAS3(String fileName, String fileContent) {
        if (swf == null) {
        	System.err.println("Error: SWF not open");
        	System.exit(1);
        }
        if (packs == null) {
        	System.err.println("Error: AS3 not decompiled");
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
    			String.format("Error: Could not find a matching class for %s. Adding new AS3 is unsupported for now.", className)
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
            System.err.println("Error: Current script replacer is not available.");
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
