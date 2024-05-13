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
package tests.eu.qualimaster.coordination;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.coordination.MavenMetaInfo;
import eu.qualimaster.coordination.MavenMetaInfo.SnapshotVersion;

/**
 * Implements a test for {@link MavenMetaInfo}.
 * 
 * @author Holger Eichelberger
 */
public class MavenMetaInfoTest {

    /**
     * Tests the snapshot meta information.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testSnapshot() throws IOException {
        FileInputStream in = new FileInputStream(new File(Utils.getTestdataDir(), "snapshot-maven-metadata.xml"));
        MavenMetaInfo info = new MavenMetaInfo(in);
        in.close();
        Assert.assertEquals("eu.qualimaster", info.getGroupId());
        Assert.assertEquals("infrastructureModel", info.getArtifactId());
        Assert.assertEquals("0.0.1-SNAPSHOT", info.getArtifactVersion());
        Assert.assertEquals("1.1.0", info.getMetaVersion());
        Assert.assertEquals("20150504150401", info.getLastUpdated());
        Assert.assertEquals("34", info.getSnapshotBuild());
        Assert.assertEquals("20150504.150401", info.getSnapshotVersion());

        SnapshotVersion ver = info.getSnapshotVersion("jar");
        Assert.assertNotNull(ver);
        Assert.assertEquals("jar", ver.getExtension());
        Assert.assertEquals("0.0.1-20150504.150401-34", ver.getValue());
        Assert.assertEquals("20150504150401", ver.getUpdated());
        
        ver = info.getSnapshotVersion("pom");
        Assert.assertNotNull(ver);
        Assert.assertEquals("pom", ver.getExtension());
        Assert.assertEquals("0.0.1-20150504.150401-34", ver.getValue());
        Assert.assertEquals("20150504150401", ver.getUpdated());

        ver = info.getSnapshotVersion("pdf");
        Assert.assertNull(ver);
        
        Set<String> expectedExtension = new HashSet<String>();
        expectedExtension.add("jar");
        expectedExtension.add("pom");
        for (SnapshotVersion v : info.snapshots()) {
            expectedExtension.remove(v.getExtension());
        }
        Assert.assertTrue(expectedExtension.isEmpty());
    }
    
    /**
     * Tests a real snapshot.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testSnapshot0() throws IOException {
        FileInputStream in = new FileInputStream(new File(Utils.getTestdataDir(), "maven/maven-metadata-0.xml"));
        MavenMetaInfo info = new MavenMetaInfo(in);
        in.close();
        SnapshotVersion ver = info.getSnapshotVersion("jar");
        Assert.assertEquals("0.5.0-20160420.131718-217", ver.getValue());
    }

    /**
     * Tests a real snapshot - zip version problem.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testSnapshot1() throws IOException {
        FileInputStream in = new FileInputStream(new File(Utils.getTestdataDir(), "maven/maven-metadata-1.xml"));
        MavenMetaInfo info = new MavenMetaInfo(in);
        in.close();
        SnapshotVersion ver = info.getSnapshotVersion("zip");
        Assert.assertEquals("3.1-20160419.160108-162", ver.getValue());
        ver = info.getSnapshotVersion("zip", "profiling");
        Assert.assertEquals("3.1-20160419.160108-162", ver.getValue());
    }

}
