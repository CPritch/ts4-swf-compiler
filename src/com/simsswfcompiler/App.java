package com.simsswfcompiler;
import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.SwfOpenException;
import com.jpexs.decompiler.flash.tags.Tag;
import com.jpexs.decompiler.flash.tags.base.CharacterIdTag;
import java.io.FileInputStream;
import java.io.IOException;

public class App {
	private static final String APP_NAME = "Sims SWF Editor";
	
	public static void main(String[] args) {
		System.out.println(String.format("%s started.", APP_NAME));
		readSWFTags("data/export/escapemenu.swf");
		System.out.println(String.format("%s ended.", APP_NAME));
	}
	
	private static void readSWFTags(String filePath) {
		try ( FileInputStream fis = new FileInputStream(filePath)) {
            SWF swf = new SWF(fis, true); 

            System.out.println("SWF version = " + swf.version);
            System.out.println("FrameCount = " + swf.frameCount);

            for (Tag t : swf.getTags()) {                
                if (t instanceof CharacterIdTag) {
                    System.out.println("Tag " + t.getTagName() + " (" + ((CharacterIdTag) t).getCharacterId() + ")");
                } else {
                    System.out.println("Tag " + t.getTagName());
                }
            }

            System.out.println("OK");
        } catch (SwfOpenException ex) {
            System.out.println("ERROR: Invalid SWF file");
        } catch (IOException ex) {
            System.out.println("ERROR: Error during SWF opening");
        } catch (InterruptedException ex) {
            System.out.println("ERROR: Parsing interrupted");
        }
	}

}
