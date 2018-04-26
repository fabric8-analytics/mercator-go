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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;

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

        System.err.println(String.format("Attempting expansion of POM: %s", pomFile));

        File resolvedPom;
        try {
            resolvedPom = File.createTempFile("resolvedpom", ".xml");
        } catch (java.io.IOException ex) {
            return null;
        }
        resolvedPom.deleteOnExit();
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File("pom.xml"));
        File tempRepoDir = null;
        try {
            tempRepoDir = Files.createTempDirectory(null).toFile();
            request.setLocalRepositoryDirectory(tempRepoDir);
        } catch (java.io.IOException ex) {
            return null;
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
            return null;
        }

        try {
            File resolvedPomFixed = removeDuplicateXMLTag(resolvedPom);
            Document parsedPom = readFileAsDocument(resolvedPomFixed);
            return parsedPom;
        } catch (java.io.IOException ex) {
            return null;
        }

    }

    /**
     * Remove duplicate XML start tag from given XML file.
     * @param resolvedPom
     * @return File handle having only one XML start tag
     * @throws IOException
     */
    public static File removeDuplicateXMLTag(File resolvedPom) throws IOException {
        File resolvedPomFixed;
        resolvedPomFixed = File.createTempFile("resolvedpom-fixed", ".xml");
        resolvedPomFixed.deleteOnExit();

        Scanner sc = new Scanner(new FileInputStream(resolvedPom), StandardCharsets.UTF_8.name());
        String xmlTag = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
        int xmlTagCount = 0;

        PrintWriter pw = new PrintWriter(resolvedPomFixed);
        while(sc.hasNextLine()) {
            String line = sc.nextLine();
            if(line.equals(xmlTag)) {
                xmlTagCount ++;
                if(xmlTagCount > 1) {
                    continue;
                }
            }
            if(line.contains("os.detected.release")) {
                continue; // skip lines with detected os release as they might break the parser
            }
            pw.println(line);
        }
        pw.close();
        return resolvedPomFixed;
    }

    public static  Document readFileAsDocument(File inputFile) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = dbf.newDocumentBuilder();
            return docBuilder.parse(inputFile);
        } catch (javax.xml.parsers.ParserConfigurationException ex) {
            ex.printStackTrace();
            return null;
        } catch (org.xml.sax.SAXException ex) {
            ex.printStackTrace();
            return null;
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            return null;
        }
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
            System.err.println( String.format("Dependencies: %s", dependencies));
        } catch (javax.xml.xpath.XPathExpressionException ex) {
            ex.printStackTrace();
            return depMap;
        }
        for (int i = 0; i < dependencies.getLength(); i++) {
            Element e = (Element) dependencies.item(i);
            System.err.println( String.format("Element: %s", e));
            String groupId = e.getElementsByTagName("groupId").item(0).getTextContent();
            String artifactId = e.getElementsByTagName("artifactId").item(0).getTextContent();

            NodeList versions = e.getElementsByTagName("version");
            String version = "";
            if(versions.getLength() > 0) {
                version = versions.item(0).getTextContent();
            }
            String classifier = "", type = "";
            if (e.getElementsByTagName("classifier").getLength() > 0) {
                classifier = e.getElementsByTagName("classifier").item(0).getTextContent();
            }
            if (e.getElementsByTagName("type").getLength() > 0) {
                type = e.getElementsByTagName("type").item(0).getTextContent();
            }
            NodeList scopes = e.getElementsByTagName("scope");

            Set<String> dependencyScopes = new HashSet<>();

            if(scopes.getLength() == 0) {
                String scopeName = "compile";
                System.err.println(String.format("Defaulting to scope: %s", scopeName));
                dependencyScopes.add(scopeName);
            } else {
                for (int j = 0; j < scopes.getLength(); j++) {
                    dependencyScopes.add(scopes.item(j).getTextContent());
                }
            }

            for (String scopeName: dependencyScopes) {
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

    public static boolean resolvePomsEnabled() {
        return Boolean.parseBoolean(System.getenv("MERCATOR_JAVA_RESOLVE_POMS"));
    }

    private static Boolean ignoreDescription(Document pomDocument)  {
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        XPathExpression xpe = null;
        Node nd = null;

        try {
            xpe = xpath.compile("/project/description");
            nd = (Node) xpe.evaluate(pomDocument, XPathConstants.NODE);
        } catch (javax.xml.xpath.XPathExpressionException e) {
            e.printStackTrace();
            return false;
        }

        if (nd == null) {
            return true;
        }
        return false;
    }

    public static Map<String, Map> getPomXmlEntries(File pomFile) {
        // TODO: improve error handling/error reporting

        System.err.println(String.format("Processing file: %s", pomFile));
        Document parsedPom = readFileAsDocument(pomFile);
        Boolean ignoreDescription = ignoreDescription(parsedPom);

        if(resolvePomsEnabled()) {
            System.err.println("NOTICE: Resolving POMs is enabled.");

            parsedPom = getParsedExpandedPom(pomFile);
        }

        Map<String, Map> result = new HashMap<String, Map>();

        if (parsedPom == null) {
            return result;
        }

        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        XPathExpression xpe = null;


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
        List<String> keys = new ArrayList<>(Arrays.asList("groupId", "artifactId", "version", "name", "url"));
        if (!ignoreDescription) {
            keys.add("description");
        }
        Node nd = null;
        for (String key : keys) {
            try {
                xpe = xpath.compile(String.format("/project/%s[1]", key));
                nd = (Node) xpe.evaluate(parsedPom, XPathConstants.NODE);
            } catch (javax.xml.xpath.XPathExpressionException ex) {
                return result;
            }

            if (nd != null) {
                result.get("pom.xml").put(key, nd.getTextContent());
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
