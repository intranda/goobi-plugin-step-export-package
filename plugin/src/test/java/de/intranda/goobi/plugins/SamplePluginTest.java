package de.intranda.goobi.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Test;

public class SamplePluginTest {

    @Test
    public void testVersion() throws IOException {
        String s = "xyz";
        assertNotNull(s);
    }


    @Test

    public void testDateConversion() {
        long millis =1605166369678l;
        String format = ExportPackageStepPlugin.getDateFormat(millis);
        assertEquals("20201112_083249", format);

    }

}
