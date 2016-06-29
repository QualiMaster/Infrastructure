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
package eu.qualimaster.common.hardware;

/**
 * Translates additional information provided with the response messages into readable text.
 * 
 * @author Gregory Chrysos
 * @author Holger Eichelberger
 */
public class MessageTable {

    /**
     * The error/executoin codes.
     * 
     * @author Holger Eichelberger
     */
    public enum Code {

        /**
         * Indicates success for all commands.
         */
        SUCCESS(0, "Success"),

        /**
         * Indicates an algorithm upload error.
         */
        UPLOAD_ERROR(-1, "Error during uploading the requested algorithm."),

        /**
         * Indicates an algorithm stop error.
         */
        STOP_ERROR(-2, "Error during stopping the requested algorithm.");
        
        private int code;
        private String message;

        /**
         * Creates a code "constant".
         * 
         * @param code the numeric error code
         * @param message the explaining message
         */
        private Code(int code, String message) {
            this.code = code;
            this.message = message;
        }
        
        
        /**
         * Returns the explaining mesage.
         * 
         * @return the message
         */
        public String getMessage() {
            return message;
        }
        
        /**
         * Returns the string-based error msg.
         * 
         * @return the error msg
         */
        public String toMsg() {
            return String.valueOf(code);
        }
        
        /**
         * Returns the error code.
         * 
         * @return the error code
         */
        public int getCode() {
            return code;
        }
        
    }

    /**
     * Returns the code for a msg.
     * 
     * @param msg the message
     * @return the code constant
     */
    public static final Code getCode(String msg) {
        Code result = null;
        for (Code tmp : Code.values()) {
            if (tmp.toMsg().equals(msg)) {
                result = tmp;
            }
        }
        return result;
    }
    
    /**
     * Returns a message according to the error codes in this class.
     * 
     * @param code the code
     * @return the explaining error message (may be <b>null</b> if none was found)
     */
    public static final String getMessage(int code) {
        String result = null;
        for (Code tmp : Code.values()) {
            if (code == tmp.getCode()) {
                result = tmp.getMessage();
            }
        }
        return result;
    }
    
    /**
     * Returns a message according to the string-based error information of the individual messages.
     * 
     * @param errorMsg the error message
     * @return the explaining error message (may be <b>null</b> if none was found)
     */
    public static final String getMessage(String errorMsg) {
        String result = null;
        if (null != errorMsg) {
            try {
                result = getMessage(Integer.parseInt(errorMsg.trim()));
            } catch (NumberFormatException e) {
                // -> result = null
            }
        }
        return result;
    }

}
