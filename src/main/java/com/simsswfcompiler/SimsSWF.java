package com.simsswfcompiler;

import com.jpexs.decompiler.flash.ReadOnlyTagList;
import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.SwfOpenException;
import com.jpexs.decompiler.flash.abc.ScriptPack;
import com.jpexs.decompiler.flash.gui.Main;
import com.jpexs.decompiler.flash.importers.As3ScriptReplaceException;
import com.jpexs.decompiler.flash.importers.As3ScriptReplaceExceptionItem;
import com.jpexs.decompiler.flash.importers.As3ScriptReplacerFactory;
import com.jpexs.decompiler.flash.importers.As3ScriptReplacerInterface;
import com.jpexs.decompiler.flash.tags.Tag;
import com.jpexs.decompiler.flash.tags.base.PlaceObjectTypeTag;
import com.jpexs.decompiler.flash.timeline.Timelined;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimsSWF {
    private SWF swf;
    private List<ScriptPack> packs;

    public SimsSWF(Path filePath) {
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
            this.swf = new SWF(fis, true);
            this.packs = swf.getAS3Packs();
        } catch (SwfOpenException ex) {
            throw new RuntimeException("Error opening SWF: " + ex.getMessage(), ex);
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Error reading SWF file: " + ex.getMessage(), ex);
        }
    }

    public void saveTo(Path filePath) {
        if (swf == null) {
            throw new IllegalStateException("SWF not open");
        }

        try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
            swf.saveTo(fos);
        } catch (IOException e) {
            throw new RuntimeException("Error writing SWF: " + e.getMessage(), e);
        }
    }

    public void copyTag(String srcInstanceName, String srcTagType, String dstInstanceName, String dstClassName) {
        List<Tag> foundTags = findTag(swf, srcInstanceName);
        if (foundTags.isEmpty()) {
            throw new IllegalArgumentException("Could not find tag with name: " + srcInstanceName);
        }
        if (foundTags.size() > 1) {
            throw new IllegalArgumentException(String.format("Found %s tags for %s, please be more specific", foundTags.size(), srcInstanceName));
        }

        PlaceObjectTypeTag foundTag = (PlaceObjectTypeTag) foundTags.get(0);
        try {
            PlaceObjectTypeTag copiedTag = (PlaceObjectTypeTag) foundTag.cloneTag();
            copiedTag.setInstanceName(dstInstanceName);
            if (dstClassName != null && !dstClassName.isEmpty()) {
                copiedTag.setClassName(dstClassName);
            }
            copiedTag.setModified(true);
            Timelined timeline = foundTag.getTimelined();
            timeline.addTag(timeline.indexOfTag(foundTag) + 1, copiedTag);
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Error copying tag: " + ex.getMessage(), ex);
        }
    }

    public List<Tag> findTag(Timelined parent, String name) {
        List<Tag> foundTags = new ArrayList<>();
        ReadOnlyTagList tags = parent.getTags();
        for (Tag tag : tags) {
            if (tag instanceof Timelined) {
                foundTags.addAll(findTag((Timelined) tag, name));
            } else if (tag instanceof PlaceObjectTypeTag) {
                PlaceObjectTypeTag placedTag = (PlaceObjectTypeTag) tag;
                String tagInstanceName = placedTag.getInstanceName();
                if (tagInstanceName != null && tagInstanceName.contains(name)) {
                    foundTags.add(placedTag);
                }
            }
        }
        return foundTags;
    }

    public void addAS3(String fileName, String fileContent) {
        if (swf == null) {
            throw new IllegalStateException("SWF not open");
        }
        if (packs == null) {
            throw new IllegalStateException("AS3 not decompiled");
        }

        String className = getClassNameFromFileContent(fileContent);
        boolean found = false;
        for (ScriptPack pack : packs) {
            if (pack.getClassPath().toString().equals(className)) {
                found = true;
                replaceAS3(fileContent, pack);
            }
        }

        if (!found) {
            throw new IllegalArgumentException(String.format("Could not find a matching class for %s. Adding new AS3 is unsupported for now.", className));
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
        throw new IllegalArgumentException("ClassName could not be parsed");
    }

    private void replaceAS3(String fileContent, ScriptPack pack) {
        As3ScriptReplacerInterface scriptReplacer = As3ScriptReplacerFactory.createByConfig(false);
        if (!scriptReplacer.isAvailable()) {
            throw new IllegalStateException("Current script replacer is not available.");
        }

        try {
            pack.abc.replaceScriptPack(scriptReplacer, pack, fileContent, Main.getDependencies(pack.abc.getSwf()));
        } catch (As3ScriptReplaceException asre) {
            for (As3ScriptReplaceExceptionItem item : asre.getExceptionItems()) {
                System.err.println(formatErrorMessage(item));
            }
            throw new RuntimeException("Error replacing ActionScript 3 script", asre);
        } catch (InterruptedException | IOException ex) {
            throw new RuntimeException("Error getting script dependencies: " + ex.getMessage(), ex);
        }
    }

    private static String formatErrorMessage(As3ScriptReplaceExceptionItem item) {
        String message = "%error% on line %line%, column %col%, file: %file%";
        message = message.replace("%error%", item.getMessage());
        message = message.replace("%line%", Long.toString(item.getLine()));
        message = message.replace("%file%", item.getFile());
        message = message.replace("%col%", Long.toString(item.getCol()));
        return message;
    }
}
