This module contains a set or BEAM ProductReader acceptance tests.

To be able to run the tests, two VM properties need to be present:

-Dbeam.reader.tests.execute=true
    This property enables the acceptance test runner. If not set, all tests are skipped and a message is printed to the
    console window.

-Dbeam.reader.tests.data.dir=<Path_To_Data>
    This property defines the root directory for the test dataset. All test-product definitions are referenced relative to
    this root directory. If the property is not set or does not denote a valid directory, the test setup fails.

-Dbeam.reader.tests.failOnMissingData=false
    By default the reader tests fail if test data is missing. This property can be set to false to avoid this. It is helpful
    for developer if they don't have the complete test data set on their developer machine.