/*
 * Copyright 2009-2019 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.plugins;

/**
 * Parses plugin manifest entries.
 * 
 * @author Holger Eichelberger
 */
public class EntryParser {

    /**
     * Stores the actual parser state.
     * 
     * @author Holger Eichelberger
     */
    private static class ParserState {
        private String entry;
        private String clsName;
        private String startLayer;
        private String endLayer;
        private boolean inLayers = false;
        private int pos = 0;
        private int lastPos = 0;
        private IPluginEntryHandler handler;
        
        /**
         * Creates a state with {@code handler}.
         * 
         * @param entry the entry text
         * @param handler the entry handler
         */
        private ParserState(String entry, IPluginEntryHandler handler) {
            this.entry = entry;
            this.handler = handler;
        }

        /**
         * Clears the entry and calls the {@link #handler} if appropriate.
         */
        private void clearEntry() {
            if (null != clsName) {
                String[] layers;
                if (null == startLayer && null == endLayer) {
                    layers = new String[0];
                } else {
                    layers = new String[2];
                    layers[0] = startLayer;
                    if (null == endLayer) {
                        layers[1] = startLayer;
                    } else {
                        layers[1] = endLayer;
                    }
                }
                handler.handle(clsName, layers);
            }
            clsName = null;
            startLayer = null;
            endLayer = null;
            lastPos = pos + 1;
        }
        
        /**
         * Consumes a layer between {@link #lastPos} and {@code #pos}.
         */
        private void consumeLayer() {
            if (pos > lastPos) {
                String tmp = entry.substring(lastPos, pos).trim();
                if (null == startLayer) {
                    startLayer = tmp;
                } else {
                    endLayer = tmp;
                }
                lastPos = pos + 1;
            }
        }

        /**
         * Consumes a class name.
         * 
         */
        private void consumeClassName() {
            if (pos > lastPos) {
                clsName = entry.substring(lastPos, pos).trim();
                lastPos = pos + 1;
            }
        }
        
        /**
         * Returns the current character at {@link #pos}.
         * 
         * @return the current character
         */
        private char currentChar() {
            return entry.charAt(pos);
        }
        
        /**
         * Returns whether there are more characters to parse.
         * 
         * @return {@code true} for more characters, {@code false} else
         */
        private boolean hasMoreCharacters() {
            return pos < entry.length();
        }

    }
    
    /**
     * Defines a plugin entry handler.
     * 
     * @author Holger Eichelberger
     */
    public interface IPluginEntryHandler {

        /**
         * Called when a plugin entry shall be handled.
         * 
         * @param cls the class name
         * @param layers the layers (may be null, or two entries, start/stop, start/stop may be the same)
         */
        public void handle(String cls, String[] layers);
        
    }
    
    /**
     * Parses a plugin manifest {@code entry} and delegates the result handing to {@code handler}.
     * 
     * @param entry the manifest entry string
     * @param handler the handler
     */
    public static void parseManifestPluginEntry(String entry, IPluginEntryHandler handler) {
        ParserState state = new ParserState(entry, handler);
        while (state.hasMoreCharacters()) {
            char c = state.currentChar();
            switch (c) {
            case ',':
                if (state.inLayers) { // layer separator
                    state.consumeLayer();
                } else { // plugin separator
                    state.consumeClassName();
                    state.clearEntry();
                }
                break;
            case '[': // optional
                if (!state.inLayers) {
                    state.consumeClassName();
                    state.inLayers = true;
                }
                break;
            case ']':
                if (state.inLayers) {
                    state.consumeLayer();
                    state.inLayers = false;
                }
                break;
            default:
                break;
            }
            state.pos++;
        }
        state.consumeClassName();
        state.clearEntry();
    }

}
