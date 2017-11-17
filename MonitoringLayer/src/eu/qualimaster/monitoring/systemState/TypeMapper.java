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
package eu.qualimaster.monitoring.systemState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.qualimaster.easy.extension.QmConstants;
import eu.qualimaster.easy.extension.internal.PipelineHelper;
import eu.qualimaster.easy.extension.internal.VariableHelper;
import eu.qualimaster.monitoring.MonitoringManager;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.model.ModelQueryException;
import net.ssehub.easy.varModel.model.datatypes.Compound;
import net.ssehub.easy.varModel.model.datatypes.IDatatype;
import net.ssehub.easy.varModel.model.datatypes.Reference;

/**
 * Implements a mapper from IVML type (names) to frozen and actual system state.
 * 
 * @author Holger Eichelberger
 */
public class TypeMapper {

    private static final Map<String, TypeCharacterizer> TYPE_PREFIX = new HashMap<String, TypeCharacterizer>();

    /**
     * Implements a type characterizer for the monitoring state side.
     * 
     * @author Holger Eichelberger
     */
    public abstract static class TypeCharacterizer {
        
        private String frozenStatePrefix;
        private int minPathLen;

        /**
         * Creates a characterizer (required for anonymous types).
         */
        private TypeCharacterizer() {
        }
        
        /**
         * Creates a type characterizer with frozen state prefix an required minimum system state path length.
         * 
         * @param prefix the frozen state prefix
         * @param minPathLen the minimum path length
         */
        private TypeCharacterizer(String prefix, int minPathLen) {
            this.frozenStatePrefix = prefix; // link to part type?
            this.minPathLen = Math.max(1, minPathLen);
        }
        
        /**
         * Returns the frozen state prefix.
         * 
         * @return the frozen state prefix
         */
        public String getFrozenStatePrefix() {
            return frozenStatePrefix;
        }
        
        /**
         * Returns the frozen state key of <code>var</code>.
         * 
         * @param var the variable
         * @return the frozen state key (may be <b>null</b> if the key cannot be determined)
         */
        public String getFrozenStateKey(IDecisionVariable var) {
            return VariableHelper.getName(var);
        }
        
        /**
         * Returns the related system part based on the given <code>path</code>, i.e., this instance defines the 
         * access to the last element in <code>path</code>.
         * 
         * @param path the path
         * @return the system part (may be <b>null</b> if not found)
         */
        public SystemPart getSystemPart(List<String> path) {
            SystemPart result;
            if (path.size() >= minPathLen) {
                result = getSystemPartImpl(path);
            } else {
                result = null;
            }
            return result;
        }
        
        /**
         * Implements the access to the related system part. Thereby, the minimum path length is already ensured.
         * 
         * @param path the path with required minimum length
         * @return the system part (may be <b>null</b> if not found)
         */
        protected abstract SystemPart getSystemPartImpl(List<String> path);
        
    }
    
    /**
     * Specific type characterizer for parts contained in a pipeline, such as pipeline nodes or algorithms.
     * 
     * @author Holger Eichelberger
     */
    private abstract static class PipelineEltTypeCharacterizer extends TypeCharacterizer {

        /**
         * Creates a characterizer (required for anonymous types).
         */
        private PipelineEltTypeCharacterizer() {
        }

        /**
         * Creates a type characterizer with frozen state prefix an required minimum system state path length.
         * 
         * @param prefix the frozen state prefix
         * @param minPathLen the minimum path length
         */
        private PipelineEltTypeCharacterizer(String prefix, int minPathLen) {
            super(prefix, Math.min(2, minPathLen));
        }

        @Override
        protected SystemPart getSystemPartImpl(List<String> path) {
            PipelineSystemPart pip = MonitoringManager.getSystemState().getPipeline(path.get(0));
            return null == pip ? null : getPipelinePart(pip, path);
        }
        
        /**
         * Implements the access to the second (or following) path segment. The first one has already been
         * resolved to <code>pipeline</code>.
         * 
         * @param pipeline the pipeline system part
         * @param path the path (including the already resolved path element)
         * @return the pipeline part (may be <b>null</b> if not found)
         */
        protected abstract SystemPart getPipelinePart(PipelineSystemPart pipeline, List<String> path);

        @Override
        public String getFrozenStateKey(IDecisionVariable var) {
            String result;
            try {
                String pipName = VariableHelper.getName(PipelineHelper.obtainPipeline(var.getConfiguration(), var));
                result = FrozenSystemState.obtainPipelineElementSubkey(pipName, VariableHelper.getName(var));
            } catch (ModelQueryException e) {
                result = null;
            }
            return result;
        }

    }
    
    static {
        // map only basic types
        TYPE_PREFIX.put(QmConstants.TYPE_ALGORITHM, new PipelineEltTypeCharacterizer(FrozenSystemState.ALGORITHM, 2) {
            
            @Override
            public SystemPart getPipelinePart(PipelineSystemPart pipeline, List<String> path) {
                return pipeline.getAlgorithm(path.get(1));
            }
        });
        TYPE_PREFIX.put(QmConstants.TYPE_DATASINK, new PipelineEltTypeCharacterizer(FrozenSystemState.DATASINK, 2) {

            @Override
            public SystemPart getPipelinePart(PipelineSystemPart pipeline, List<String> path) {
                return pipeline.getSink(path.get(1));
            }

        });
        TYPE_PREFIX.put(QmConstants.TYPE_DATASOURCE, new PipelineEltTypeCharacterizer(FrozenSystemState.DATASOURCE, 2) {

            @Override
            public SystemPart getPipelinePart(PipelineSystemPart pipeline, List<String> path) {
                return pipeline.getSource(path.get(1));
            }
            
        });
        TYPE_PREFIX.put(QmConstants.TYPE_HWNODE, new TypeCharacterizer(FrozenSystemState.HWNODE, 1) {
            
            @Override
            protected SystemPart getSystemPartImpl(List<String> path) {
                return MonitoringManager.getSystemState().getPlatform().getHwNode(path.get(0));
            }
        });
        TYPE_PREFIX.put(QmConstants.TYPE_MACHINE, new TypeCharacterizer(FrozenSystemState.MACHINE, 1) {
            
            @Override
            protected SystemPart getSystemPartImpl(List<String> path) {
                return MonitoringManager.getSystemState().getPlatform().getMachine(path.get(0));
            }
        });
        TYPE_PREFIX.put(QmConstants.TYPE_PIPELINE, new TypeCharacterizer(FrozenSystemState.PIPELINE, 1) {

            @Override
            public SystemPart getSystemPartImpl(List<String> path) {
                return MonitoringManager.getSystemState().getPipeline(path.get(0));
            }
        });
        TYPE_PREFIX.put(QmConstants.TYPE_CLOUDRESOURCE, new TypeCharacterizer(FrozenSystemState.CLOUDENV, 1) {
            
            @Override
            protected SystemPart getSystemPartImpl(List<String> path) {
                return MonitoringManager.getSystemState().getPlatform().getCloudEnvironment(path.get(0));
            }
        });
        TypeCharacterizer tmp = new PipelineEltTypeCharacterizer(FrozenSystemState.PIPELINE_ELEMENT, 2) {

            @Override
            public SystemPart getPipelinePart(PipelineSystemPart pipeline, List<String> path) {
                return pipeline.getPipelineNode(path.get(1));
            }

        };
        TYPE_PREFIX.put(QmConstants.TYPE_PIPELINE_NODE, tmp);
        TYPE_PREFIX.put(QmConstants.TYPE_FLOW, tmp); // preliminary -> mapping.rtvil
    }
    
    /**
     * Finds the frozen state prefix for <code>type</code>, also taking refined types into account.
     * 
     * @param type the type
     * @return the prefix (may be <b>null</b> if there is none)
     */
    public static final TypeCharacterizer findCharacterizer(IDatatype type) {
        TypeCharacterizer result = null;
        do {
            result = TYPE_PREFIX.get(type.getName());
            if (null == result) {
                if (type instanceof Compound) {
                    Compound cType = (Compound) type;
                    for (int r = 0; null == result && r < cType.getRefinesCount(); r++) {
                        type = cType.getRefines(r);
                        if (TYPE_PREFIX.containsKey(type.getName())) {
                            break;
                        }
                    }
                } else if (type instanceof Reference) {
                    type = Reference.dereference(type);
                } else {
                    // Not handled by the TypeMapper -> stop loop
                    type = null;
                }
            }
        } while (null == result && null != type);
        return result;
    }

}
