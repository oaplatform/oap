package oap.testng;

import org.testng.ITestResult;
import org.testng.internal.IResultListener;

public class TeamcityTestNGListener implements IResultListener {
    @Override
    public void onTestStart( ITestResult result ) {
        Teamcity.testStarted( getTestName( result ) );
    }

    @Override
    public void onTestSuccess( ITestResult result ) {
        Teamcity.testFinished( getTestName( result ), result.getStartMillis() - result.getEndMillis() );
    }

    @Override
    public void onTestSkipped( ITestResult result ) {
        Teamcity.testIgnored( getTestName( result ), "Skipped" );
    }

    @Override
    public void onTestFailure( ITestResult result ) {
        Teamcity.testFailed( getTestName( result ), result.getThrowable(), result.getStartMillis() - result.getEndMillis() );
    }

    private String getTestName( ITestResult result ) {
        return result.getTestClass().getRealClass().getSimpleName() + "." + result.getName();
    }
}
