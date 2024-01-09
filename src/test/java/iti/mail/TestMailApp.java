package iti.mail;

import static iti.mail.MailClient.*;

import static org.junit.Assert.assertEquals;

import iti.mail.MailClient.MailFilter;

import org.junit.Test;

import java.io.File;

import javax.mail.search.SearchTerm;

public class TestMailApp {

    @Test
    public void testFilterMatch() {
        final SearchTerm mf = MailFilter.parse("subject:hello+from:test@gmail.com|size_le:4kb");

        final String expectedOutput =
                "((subject contains \"hello\" and sender is \"test@gmail.com\") or size less than"
                        + " \"4096\" bytes)";
        final String output = MailFilter.toString(mf);

        assertEquals("Search strings are not equal!", expectedOutput, output);
    }
    
    @Test
    public void testFlagFilterMatch() {
        final SearchTerm mf = MailFilter.parse("flag:starred|flag:!seen");

        final String expectedOutput = "(flag \"starred\" set or flag \"Seen\" not set)";
        final String output = MailFilter.toString(mf);
        
        System.out.println(output);
        assertEquals("Search strings with flag are not equal!", expectedOutput, output);
    }

    @Test
    public void testFileExtension() {
        final File exampleFile = new File("/path/to/archive.zip");
        final File exampleFilename = new File("archive.zip");
        
        final String expectedOutput = "zip";
        
        final String outputFile = getFileExtension(exampleFile);
        final String outputFilename = getFileExtension(exampleFilename);
            
        assertEquals("File extension mismatch!", expectedOutput, outputFile);
        assertEquals("File extension mismatch!", expectedOutput, outputFilename);
    }
}
