/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.qualimaster.coordination;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import eu.qualimaster.XmlFactory;

/**
 * Represents a (snapshot) Maven metadata.
 * 
 * @author Holger Eichelberger
 */
public class MavenMetaInfo {

    private String metaVersion;
    private String groupId;
    private String artifactId;
    private String artifactVersion;
    private String snapshotVersion;
    private String snapshotBuild;
    private String lastUpdated;
    private Map<String, SnapshotVersion> snapshotVersions = new HashMap<String, SnapshotVersion>();
    
    /**
     * Represents the snapshot version for an artifact with a certain extension.
     * 
     * @author Holger Eichelberger
     */
    public class SnapshotVersion {
        private String extension;
        private String value;
        private String updated;
        
        /**
         * Creates a snapshot version instance.
         * 
         * @param extension the (file name) extension
         * @param value the (version number) value
         * @param updated the last updated timestamp
         */
        private SnapshotVersion(String extension, String value, String updated) {
            this.extension = extension;
            this.value = value;
            this.updated = updated;
        }

        /**
         * Returns the extension.
         * 
         * @return the extension if not found / given
         */
        public String getExtension() {
            return extension;
        }

        /**
         * Returns the (version number) value.
         * 
         * @return the version number or <b>null</b> if not found / given
         */
        public String getValue() {
            return value;
        }

        /**
         * Returns the last updated timestamp.
         * 
         * @return the last updated timestamp or <b>null</b> if not found / given
         */
        public String getUpdated() {
            return updated;
        }
        
        @Override
        public String toString()  {
            return extension + " " + value + " " + updated;
        }
        
    }

    /**
     * Creates a metadata information object from <code>in</code>.
     * 
     * @param in the input stream to parse from
     * @throws IOException in case of I/O reading problems
     */
    public MavenMetaInfo(InputStream in) throws IOException {
        try {
            DocumentBuilderFactory factory = XmlFactory.getDefaultXmlDocumentBuilderFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(in);
            NodeList list = document.getElementsByTagName("metadata");
            if (1 == list.getLength()) {
                parseMetadata(list.item(0));
            }
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } catch (SAXException e) {
            throw new IOException(e);
        }
    }

    /**
     * Parses the metadata element from <code>node</code>.
     * 
     * @param node the node to be parsed
     */
    private void parseMetadata(Node node) {
        NamedNodeMap attributes = node.getAttributes();
        if (null != attributes) {
            Node versionAttr = attributes.getNamedItem("modelVersion");
            if (null != versionAttr) {
                metaVersion = versionAttr.getTextContent();
            }
        }
        NodeList childs = node.getChildNodes();
        for (int c = 0; c < childs.getLength(); c++) {
            Node child = childs.item(c);
            String name = child.getNodeName();
            if ("groupId".equals(name)) {
                groupId = child.getTextContent();
            } else if ("artifactId".equals(name)) {
                artifactId = child.getTextContent();
            } else if ("version".equals(name)) {
                artifactVersion = child.getTextContent();
            } else if ("versioning".equals(name)) {
                parseVersioning(child);
            }
        }
    }
    
    /**
     * Parses a versioning section from <code>node</code>.
     * 
     * @param node the node to be parsed
     */
    private void parseVersioning(Node node) {
        NodeList childs = node.getChildNodes();
        for (int c = 0; c < childs.getLength(); c++) {
            Node child = childs.item(c);
            String name = child.getNodeName();
            if ("snapshot".equals(name)) {
                parseSnapshot(child);
            } else if ("snapshotVersions".equals(name)) {
                parseSnapshotVersions(child);
            } else if ("lastUpdated".equals(name)) {
                lastUpdated = child.getTextContent();
            }
        }
    }
    
    /**
     * Parses a snapshot from <code>node</code>.
     * 
     * @param node the node to be parsed
     */
    private void parseSnapshot(Node node) {
        NodeList childs = node.getChildNodes();
        for (int c = 0; c < childs.getLength(); c++) {
            Node child = childs.item(c);
            String name = child.getNodeName();
            if ("timestamp".equals(name)) {
                snapshotVersion = child.getTextContent();
            } else if ("buildNumber".equals(name)) {
                snapshotBuild = child.getTextContent();
            }
        }        
    }

    /**
     * Parses a snapshot versions from <code>node</code>.
     * 
     * @param node the node to be parsed
     */
    private void parseSnapshotVersions(Node node) {
        NodeList childs = node.getChildNodes();
        for (int c = 0; c < childs.getLength(); c++) {
            Node child = childs.item(c);
            String name = child.getNodeName();
            if ("snapshotVersion".equals(name)) {
                parseSnapshotVersion(child);
            }
        }
    }
    
    /**
     * Parses a snapshot version from <code>node</code>.
     * 
     * @param node the node to be parsed
     */
    private void parseSnapshotVersion(Node node) {
        String extension = null;
        String value = null;
        String updated = null;
        String classifier = null;
        NodeList childs = node.getChildNodes();
        for (int c = 0; c < childs.getLength(); c++) {
            Node child = childs.item(c);
            String name = child.getNodeName();
            if ("extension".equals(name)) {
                extension = child.getTextContent();
            } else if ("value".equals(name)) {
                value = child.getTextContent();
            } else if ("updated".equals(name)) {
                updated = child.getTextContent();
            } else if ("classifier".equals(name)) {
                classifier = child.getTextContent();
            }
        }        
        if (null != extension) {
            SnapshotVersion version = new SnapshotVersion(extension, value, updated);
            String key = extension;
            if (null != classifier) {
                key += "-" + classifier;
                if (!snapshotVersions.containsKey(extension)) {
                    snapshotVersions.put(extension, version);
                }
            }
            snapshotVersions.put(key, version);
        }
    }

    /**
     * Returns the group of the described artifact.
     * 
     * @return the group identifier or <b>null</b> if not found / given
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Returns the identifier of the described artifact.
     * 
     * @return the artifact identifier or <b>null</b> if not found / given
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Returns the version of the described artifact.
     * 
     * @return the artifact version or <b>null</b> if not found / given
     */
    public String getArtifactVersion() {
        return artifactVersion;
    }
    
    /**
     * Returns the version of the document.
     * 
     * @return the version of the document or <b>null</b> if not found / given
     */
    public String getMetaVersion() {
        return metaVersion;
    }

    /**
     * Returns the last updated timestamp.
     * 
     * @return the last updated timestamp or <b>null</b> if not found / given
     */
    public String getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Returns the snapshot version.
     * 
     * @return the snapshot version or <b>null</b> if not found / given
     */
    public String getSnapshotVersion() {
        return snapshotVersion;
    }
    
    /**
     * Returns the snapshot build.
     * 
     * @return the snapshot build or <b>null</b> if not found / given
     */
    public String getSnapshotBuild() {
        return snapshotBuild;
    }

    /**
     * Returns the snapshot version for a given extension.
     * 
     * @param extension the extension to return the snapshot version for
     * @return the snapshot version or <b>null</b> if not found
      */
    public SnapshotVersion getSnapshotVersion(String extension) {
        return null == extension ? null : snapshotVersions.get(extension);
    }
    
    /**
     * Returns the snapshot version for a given extension.
     * 
     * @param extension the extension to return the snapshot version for
     * @param classifier the classifier to search for (may be <b>null</b>)
     * @return the snapshot version or <b>null</b> if not found
      */
    public SnapshotVersion getSnapshotVersion(String extension, String classifier) {
        String key = extension;
        if (null != classifier) {
            if (null != key) {
                key += "-" + classifier;
            }
        }
        return null == key ? null : snapshotVersions.get(key);
    }
    
    /**
     * Returns all snapshots.
     * 
     * @return an iterable over all snapshots
     */
    public Iterable<SnapshotVersion> snapshots() {
        return snapshotVersions.values();
    }
    
    @Override
    public String toString() {
        return groupId + " " + artifactId + " " + artifactVersion + " " + snapshotVersion + " " + snapshotVersion 
            + " " + lastUpdated + " " + snapshotVersions;
    }
    
}
