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
            assert d.getElementsByTagName("pac4j.version").item(0).getFirstChild().getNodeValue().equals("1.6.0");
            assert d.getElementsByTagName("os.detected.name").item(0).getFirstChild().getNodeValue().equals("linux");
            assert d.getElementsByTagName("os.detected.release").item(0) == null;
            assert d.getElementsByTagName("os.detected.release.like.\"centos\"").item(0) == null;
            assert d.getElementsByTagName("os.detected.release.like.\"rhel").item(0) == null;
            assert d.getElementsByTagName("os.detected.release.like.fedora\"").item(0) == null;


        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }
    }
}
