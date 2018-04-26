package mercator;

import org.junit.Test;
import org.w3c.dom.Document;

import java.io.File;
import java.net.URI;
import java.net.URL;

public class MavenUtilsTest  {

    @Test
    public void testXMLCleanup () {
        URL resource = this.getClass().getResource("/mercator/osio_3182_pom-expanded.xml");
        assert resource != null;
        try {
            URI uri = resource.toURI();
            assert uri != null;
            File f = new File(uri);
            File f2 = mercator.MavenUtils.removeDuplicateXMLTag(f);
            Document d = mercator.MavenUtils.readFileAsDocument(f2);
            assert d != null;
        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }
}
