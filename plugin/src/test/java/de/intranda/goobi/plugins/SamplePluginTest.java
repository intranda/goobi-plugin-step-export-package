package de.intranda.goobi.plugins;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
        long millis = 1605166369678l;
        String format = ExportPackageStepPlugin.getDateFormat(millis);
        assertTrue("20201112_083249".equals(format) || "20201112_073249".equals(format)); // check two formats because jenkins sits in a different time zone

    }

}
