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
package eu.qualimaster;

import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Aims at overriding additional XML factories if required.
 * 
 * @author Holger Eichelberger
 */
public class XmlFactory {

    /**
     * Returns the default XML document builder factory ignoring additions such as the GNU dependency.
     * 
     * @return the factory instance
     */
    public static DocumentBuilderFactory getDefaultXmlDocumentBuilderFactory() {
        DocumentBuilderFactory result = null;
        try {
            Class<?> cls = Class.forName("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            Object obj = cls.newInstance();
            if (obj instanceof DocumentBuilderFactory) {
                result = (DocumentBuilderFactory) obj;
            }
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
        if (null == result) {
            result = DocumentBuilderFactory.newInstance();
        }
        return result;
    }

}
