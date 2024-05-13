package eu.qualimaster.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Marker annotation to mark types/classes/methods/fields as internal to the QualiMaster infrastructure, 
 * specifically marking them as invisible for rt-VIL. For mapping types into rt-VIL, only 
 * public classes / members unmarked events, coordination commands and observables will be 
 * considered. For enums, automatically the defined "constants" become visible as read-only 
 * fields. For classes, the type becomes and the public methods become visible, potentially 
 * (depending on the implementation of the mapping) also the constructor.
 * 
 * @author Holger Eichelberger
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface QMInternal {
}