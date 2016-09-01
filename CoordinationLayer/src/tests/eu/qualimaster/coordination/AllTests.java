package tests.eu.qualimaster.coordination;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * The test suite for the Adaptation Layer. Do not rename this class. Set environment variable 
 * "STORM_TEST_TIMEOUT_MS" to a value greater than 15.000 (ms).
 * 
 * @author Holger Eichelberger
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ManagerTests.class, NameMappingTest.class, 
    TaskAssignmentTest.class, StormUtilsTests.class, 
    //StormTests.class, disable for now 
    ProfileControlTests.class,
    // always at the end
    CoordinationConfigurationTests.class })
public class AllTests {

}
