package mercator;
/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Mercator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mercator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with Mercator. If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.Invoker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MavenUtils {
    
    private MavenUtils() {
    }

    public static Document getParsedExpandedPom(File pomFile) {
        // TODO: improve error handling/error reporting
        Document parsedPom = null;
        File resolvedPom;
        try {
            resolvedPom = File.createTempFile("resolvedpom", ".xml");
        } catch (java.io.IOException ex) {
            return parsedPom;
        }
        resolvedPom.deleteOnExit();
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("pom.xml"));
        File tempRepoDir = null;
        try {
            tempRepoDir = Files.createTempDirectory(null).toFile();
            request.setLocalRepositoryDirectory(tempRepoDir);
        } catch (java.io.IOException ex) {
            return parsedPom;
        }
        // FIXME: "-q" is a hack as support for the option is currently missing in maven-invoker
        request.setGoals(Collections.singletonList("org.apache.maven.plugins:maven-help-plugin:2.2:effective-pom -q"));
        Properties properties = new Properties();
        properties.setProperty("output", resolvedPom.getAbsolutePath());
        request.setProperties(properties);
        request.setPomFile(pomFile);

        Invoker invoker = new DefaultInvoker();
        // TODO: try to be smarter here
        invoker.setMavenHome(new File("/usr/share/maven"));
        try {
            invoker.execute(request);
            recursiveDeleteOnExit(tempRepoDir);
        } catch (org.apache.maven.shared.invoker.MavenInvocationException ex) {
            // unfortunately we have to call this both if invoker.execute runs fine and when it raises;
            //   even if it raises, it still might have downloaded something
            recursiveDeleteOnExit(tempRepoDir);
            return parsedPom;
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
        } catch (javax.xml.parsers.ParserConfigurationException ex) {
            return parsedPom;
        }
        try {
            parsedPom = db.parse(resolvedPom);
        } catch (org.xml.sax.SAXException ex) {
            return parsedPom;
        } catch (java.io.IOException ex) {
            return parsedPom;
        }

        return parsedPom;
    }

    public static Map<String, Map> getPomXmlDependencies(Document parsedPom) {
        Map<String, Map> depMap = new HashMap<String, Map>();
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        XPathExpression xpe = null;
        NodeList dependencies = null;
        try {
            xpe = xpath.compile("/project/dependencies/dependency");
            dependencies = (NodeList) xpe.evaluate(parsedPom, XPathConstants.NODESET);
        } catch (javax.xml.xpath.XPathExpressionException ex) {
            return depMap;
        }
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element e = (Element) dependencies.item(i);
            String groupId = e.getElementsByTagName("groupId").item(0).getTextContent();
            String artifactId = e.getElementsByTagName("artifactId").item(0).getTextContent();
            String version = e.getElementsByTagName("version").item(0).getTextContent();
            String classifier = "", type = "";
            if (e.getElementsByTagName("classifier").getLength() > 0) {
                classifier = e.getElementsByTagName("classifier").item(0).getTextContent();
            }
            if (e.getElementsByTagName("type").getLength() > 0) {
                type = e.getElementsByTagName("type").item(0).getTextContent();
            }
            NodeList scopes = e.getElementsByTagName("scope");
            for (int j = 0; j < scopes.getLength(); j++) {
                String scopeName = scopes.item(j).getTextContent();
                if (!depMap.containsKey(scopeName)) {
                    depMap.put(scopeName, new HashMap<String, Map>());
                }
                depMap.get(scopeName).put(
                        String.format("%s:%s:%s:%s", groupId, artifactId, type, classifier),
                        version);
            }
        }
        return depMap;
    }

    public static Map<String, Map> getPomXmlEntries(File pomFile) {
        // TODO: improve error handling/error reporting
        Document parsedPom = getParsedExpandedPom(pomFile);
        Map<String, Map> result = new HashMap<String, Map>();
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        XPathExpression xpe = null;

        if (parsedPom == null) {
            return result;
        }

        result.put("pom.xml", new HashMap<String, Map>());

        // get dependencies
        result.get("pom.xml").put("dependencies", getPomXmlDependencies(parsedPom));

        // get licenses
        result.get("pom.xml").put("licenses", new ArrayList<String>());
        try {
            xpe = xpath.compile("/project/licenses/license/name");
            NodeList nl = (NodeList) xpe.evaluate(parsedPom, XPathConstants.NODESET);
            for (int i = 0; i < nl.getLength(); i++) {
                ArrayList<String> licenses = (ArrayList<String>) result.get("pom.xml").get("licenses");
                licenses.add(nl.item(i).getTextContent());
            }
        } catch (javax.xml.xpath.XPathExpressionException ex) {}

        // get scm url
        try {
            xpe = xpath.compile("/project/scm/url");
            Node nd = (Node) xpe.evaluate(parsedPom, XPathConstants.NODE);
            if (nd != null) {
                result.get("pom.xml").put("scm_url", nd.getTextContent());
            }
        } catch (javax.xml.xpath.XPathExpressionException ex) {}

        // get other metadata
        String[] keys = {"groupId", "artifactId", "version", "name", "description", "url"};
        Node nd = null;
        for (int i = 0; i < keys.length; i++) {
            try {
                xpe = xpath.compile(String.format("/project/%s[1]", keys[i]));
                nd = (Node) xpe.evaluate(parsedPom, XPathConstants.NODE);
            } catch (javax.xml.xpath.XPathExpressionException ex) {
                return result;
            }

            if (nd != null) {
                result.get("pom.xml").put(keys[i], nd.getTextContent());
            }
        }
        return result;
    }

    public static boolean isPomProperties(ZipEntry entry) {
        return isMavenMetadataFile(entry, "pom.properties");
    }

    public static boolean isPomXml(ZipEntry entry) {
        return isMavenMetadataFile(entry, "pom.xml");
    }

    private static boolean isMavenMetadataFile(ZipEntry entry, String fname) {
        String pattern = String.format("META-INF/maven/[^/]+/[^/]+/%s$", fname);
        return Pattern.matches(pattern, entry.getName());
    }

    // borrowed from https://coderanch.com/t/278832/java/delete-directory-VM-exits
    private static void recursiveDeleteOnExit(File dir) {
        // call deleteOnExit for the folder first, so it will get deleted last
        dir.deleteOnExit();
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    recursiveDeleteOnExit(f);
                } else {
                    f.deleteOnExit();
                }
            }
        }
    }
}
