package tests.eu.qualimaster.adaptation;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * The test suite for the Adaptation Layer. Do not rename this class.
 * 
 * @author Holger Eichelberger
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({ManagerTests.class, ExternalTests.class, TopLevelStormTest.class, EventTests.class, 
    TestAuthenticationSupport.class,
    // must be last
    AdaptationConfigurationTests.class})
public class AllTests {

}
