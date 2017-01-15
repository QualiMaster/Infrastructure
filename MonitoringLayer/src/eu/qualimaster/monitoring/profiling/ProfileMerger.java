/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.monitoring.profiling;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;

import eu.qualimaster.file.Utils;
import eu.qualimaster.monitoring.profiling.MapFile.MergeStatus;
import eu.qualimaster.monitoring.profiling.approximation.IApproximator;
import eu.qualimaster.monitoring.profiling.approximation.IApproximatorCreator;
import eu.qualimaster.monitoring.profiling.approximation.IStorageStrategy;

/**
 * Allows merging multiple profiles.
 * 
 * @author Holger Eichelberger
 */
public class ProfileMerger {

    private Map<String, MergeInfo> data = new HashMap<String, MergeInfo>();
    private IStorageStrategy strategy;
    
    /**
     * Denotes a set of folders on the same nesting level to be merged.
     * 
     * @author Holger Eichelberger
     */
    private abstract static class MergeInfo {

        private List<File> elements = new ArrayList<File>();
        private String path;
        
        /**
         * Creates a merge information object.
         * 
         * @param path the (relative) path
         */
        protected MergeInfo(String path) {
            this.path = path;
        }

        /**
         * Adds a folder/file element for merging.
         * 
         * @param element the file/folder
         */
        protected void add(File element) {
            elements.add(element);
        }

        /**
         * Merges the collected information into <code>target</code>.
         * 
         * @param target the target
         * @param strategy the storage strategy
         * @throws IOException in case of I/O problem
         */
        protected abstract void merge(File target, IStorageStrategy strategy) throws IOException;
        
        /**
         * Returns all known elements.
         * 
         * @return the elements
         */
        protected Iterable<File> elements() {
            return elements;
        }
        
        /**
         * Returns the path for the elements represented by this instance relocated to <code>target</code>.
         * 
         * @param target the target (base) folder
         * @return the relocated path
         */
        protected File getPath(File target) {
            return new File(target, path); // path is already in the right format
        }
        
        @Override
        public String toString() {
            return path + " " + elements;
        }
        
    }
    
    /**
     * Represents a set of approximator folders.
     * 
     * @author Holger Eichelberger
     */
    private static class ApproximatorInfo extends MergeInfo {

        private String folder;
        
        /**
         * Creates an approximator information object.
         * 
         * @param path the (relative) path
         * @param folder the approximators folder
         */
        private ApproximatorInfo(String path, String folder) {
            super(path);
            this.folder = folder;
        }

        @Override
        protected void merge(File target, IStorageStrategy strategy) throws IOException {
            File targetFolder = new File(getPath(target), folder);
            targetFolder.mkdirs();
            Utils.setDefaultPermissions(targetFolder);
            Map<String, IApproximator> approximators = new HashMap<String, IApproximator>();
            for (File srcFolder : elements()) {
                File[] files = srcFolder.listFiles();
                if (null != files) {
                    for (File f : files) {
                        IApproximator approx = approximators.get(f.getName());
                        if (null == approx) {
                            eu.qualimaster.monitoring.profiling.approximation.IStorageStrategy.ApproximatorInfo info 
                                = strategy.parseApproximatorFileName(f.getName());
                            IApproximatorCreator creator = ProfilingRegistry.getApproximatorCreator(
                                info.getParameterName(), info.getObservable());
                            if (null != creator) {
                                approx = creator.createApproximator(strategy, srcFolder, info.getParameterName(), 
                                    info.getObservable());
                                approximators.put(f.getName(), approx);
                            }
                        }
                        approx.merge(f);
                    }
                }
            }
            for (IApproximator approx : approximators.values()) {
                approx.store(targetFolder);
            }
        }

        @Override
        public String toString() {
            return "ApproxInfo " + super.toString();
        }
        
    }
    
    /**
     * Represents a set of predictor folders.
     * 
     * @author Holger Eichelberger
     */
    private static class PredictorInfo extends MergeInfo {

        /**
         * Creates a predictor information object.
         * 
         * @param path the (relative) path
         */
        private PredictorInfo(String path) {
            super(path);
        }

        @Override
        public void merge(File target, IStorageStrategy strategy) throws IOException {
            // files are map files
            File targetFolder = getPath(target);
            targetFolder.mkdirs();
            Utils.setDefaultPermissions(targetFolder);
            MapFile targetMf = new MapFile(targetFolder);
            for (File f : elements()) {
                MapFile mf = new MapFile(f);
                mf.load();
                Map<File, MergeStatus> mapping = targetMf.merge(mf);
                for (Map.Entry<File, MergeStatus> entry : mapping.entrySet()) {
                    if (MergeStatus.TAKE_OVER == entry.getValue()) {
                        File src = entry.getKey();
                        FileUtils.copyFile(src, new File(targetFolder, src.getName()));
                    } // REMOVE is not relevant as we copy into a new folder
                }
            }
            targetMf.store();
        }

        @Override
        public String toString() {
            return "ProfileInfo " + super.toString();
        }

    }
    
    /**
     * Creates a profile merger instance for the default storage strategy.
     */
    public ProfileMerger() {
        this(null);
    }

    /**
     * Creates a profile merger instance for a specific storage strategy.
     * 
     * @param strategy the storage strategy (the default storage strategy if <b>null</b>
     */
    public ProfileMerger(IStorageStrategy strategy) {
        this.strategy = null == strategy ? DefaultStorageStrategy.INSTANCE : strategy;
    }
    
    /**
     * Indexes a given <code>folder</code> for profiling information.
     * 
     * @param folder the folder
     */
    public void index(File folder) {
        index(folder, "");
    }

    /**
     * Indexes <code>folder</code> with current (nested) path <code>path</code>.
     * 
     * @param folder the folder to be indexed
     * @param path the current path
     */
    private void index(File folder, String path) {
        File[] files = folder.listFiles();
        if (null != files) {
            for (File f : files) {
                if (f.isDirectory()) {
                    if (strategy.isApproximatorsFolder(f)) {
                        MergeInfo info = data.get(path);
                        if (null == info) {
                            info = new ApproximatorInfo(path, f.getName());
                            data.put(path, info);
                        }
                        info.add(f);
                    }
                    String p = path;
                    if (p.length() > 0) {
                        p += "/";
                    }
                    p += f.getName();
                    index(f, p);
                } else if (f.isFile()) {
                    if (strategy.getMapFileName().equals(f.getName())) {
                        MergeInfo info = data.get(path);
                        if (null == info) {
                            info = new PredictorInfo(path);
                            data.put(path, info);
                        }
                        info.add(f.getParentFile()); // MapFile adds the name itself
                    }
                }
            }
        }
    }
    
    /**
     * Merges the indexed profiles into <code>target</code>.
     * 
     * @param target the target folder
     * @throws IOException if merging fails
     * @see #index(File)
     */
    public void merge(File target) throws IOException {
        for (MergeInfo info : data.values()) {
            info.merge(target, strategy);
        }
    }
    
    /**
     * Performs a default merge of two profiles.
     * 
     * @param args the first profile, the second profile, the target folder
     * @throws IOException if merging fails
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Requires p1, p2 and pt (two profiles to merge and one target profile)");
        } else {
            File source1 = new File(args[0]);
            File source2 = new File(args[1]);
            File target = new File(args[2]);
            
            ProfileMerger merger = new ProfileMerger();
            merger.index(source1);
            merger.index(source2);
            merger.merge(target);
        }
    }

}
