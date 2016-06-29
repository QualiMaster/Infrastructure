package tests.eu.qualimaster.dataManagement;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * The test suite for the Data Management Layer. Do not rename / move this class.
 * In case that you need files from the project for testing, access them either
 * through as a resource or through the relative name from the project root directory,
 * prefixed by the path system property "qm.base.dir" if configured (required for Jenkins).
 * 
 * @author Holger Eichelberger
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({StringSerializationTests.class, ReplayMechanismTests.class, PasswordStoreTests.class,
    // must be last
    DataManagementConfigurationTests.class })
public class AllTests {

}
