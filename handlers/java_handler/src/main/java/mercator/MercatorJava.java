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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.json.simple.JSONObject;

public class MercatorJava {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("{\"error\": \"not enough arguments\"}");
            System.exit(1);
        }

        String result = "{\"error\": \"No output.\"}";
        int retcode = 0;
        try {
            if ((new File(args[0])).getName().equals("pom.xml")) {
                // POM
                result = handlePomFile(args[0]).toString();
            } else {
                // JAR
                try (InputStream is = new BufferedInputStream(new FileInputStream(args[0]))) {
                    Map<String, Object> resultMap = handleZipFile(is);
                    if (!resultMap.isEmpty()) {
                        resultMap.put("kind", getJarKind(args[0]));
                    }
                    result = new JSONObject(resultMap).toString();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            result = "{\"error\": \"Error processing the input file: " + e.getMessage() + "\"}";
            retcode = 1;
        }

        System.out.println(result);
        System.exit(retcode);
    }

    private static Map<String, Object> handleZipFile(InputStream is) throws IOException {
        Map<String, Object> resultMap = new HashMap<String, Object>();

        JarInputStream jis = new JarInputStream(is);

        // fetch data from jar manifest
        Manifest manifest = jis.getManifest();
        for (Map.Entry<String, Attributes> entry : manifest.getEntries().entrySet()) {
            resultMap.put(entry.getKey(), new JSONObject(entry.getValue()));
        }
        Attributes attributes = manifest.getMainAttributes();
        resultMap.put("manifest", new JSONObject(attributes));

        boolean foundPomProperties = false;
        boolean foundPomXml = false;
        ZipEntry entry = null;
        while ((entry = jis.getNextEntry()) != null) {

            if (entry.isDirectory()) {
                continue;
            }

            if (MavenUtils.isPomProperties(entry)) {
                String result_key = "maven_id";
                if (!foundPomProperties) {
                    foundPomProperties = true;
                    Properties props = new Properties();
                    props.load(jis);
                    resultMap.put(result_key, new JSONObject(props));
                } else {
                    // we don't know how to handle multiple pom.properties files
                    resultMap.remove(result_key);
                }
                continue;
            }

            if (MavenUtils.resolvePomsEnabled()) {
                String result_key = "pom.xml";
                // fetch data from pom.xml
                if (MavenUtils.isPomXml(entry)) {
                    if (!foundPomXml) {
                        foundPomXml = true;
                        File tmpPom;
                        tmpPom = File.createTempFile("pom", ".xml");
                        tmpPom.deleteOnExit();
                        Files.copy(jis, tmpPom.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        Map pomData = MavenUtils.getPomXmlEntries(tmpPom);
                        resultMap.putAll(new JSONObject(pomData));
                    } else {
                        // we don't know how to handle multiple pom.xml files
                        resultMap.remove(result_key);
                    }
                }
                continue;
            }

            // look for bundled JAR files
            try {
                Map<String, Object> bundledResultMap = handleZipFile(jis);
                if (bundledResultMap != null && !bundledResultMap.isEmpty()) {
                    bundledResultMap.put("kind", getJarKind(entry.getName()));
                    if (!resultMap.containsKey("bundled")) {
                        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
                        resultMap.put("bundled", list);
                    }
                    ((List<Map<String, Object>>) resultMap.get("bundled")).add(bundledResultMap);
                }
            } catch (Exception e) {
                // ok, not a JAR file
            }
        }

        return resultMap;
    }

    private static JSONObject handlePomFile(String pomPath) {
        JSONObject output = new JSONObject(MavenUtils.getPomXmlEntries(new File(pomPath)));
        return output;
    }

    private static String getJarKind(String name) {
        String lcName = name.toLowerCase();

        // TODO: this is a very naive approach, it can be improved
        if (lcName.endsWith(".jar")) {
            return "JAR";
        } else if (lcName.endsWith(".war")) {
            return "WAR";
        }
        else if (lcName.endsWith(".ear")) {
            return "EAR";
        }
        return "UNKNOWN";
    }
}
