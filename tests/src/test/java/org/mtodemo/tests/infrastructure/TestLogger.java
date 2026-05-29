package org.mtodemo.tests.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IClassListener;
import org.testng.ITestClass;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestLogger implements ITestListener, IClassListener {

    private static final Logger log = LoggerFactory.getLogger("TEST");

    private static final String RESET  = "[0m";
    private static final String BOLD   = "[1m";
    private static final String CYAN   = "[36m";
    private static final String GREEN  = "[32m";
    private static final String RED    = "[31m";
    private static final String YELLOW = "[33m";
    private static final String DIM    = "[2m";

    @Override
    public void onBeforeClass(ITestClass testClass) {
        log.info("");
        log.info("");
        log.info(BOLD + CYAN + "┌─ " + testClass.getRealClass().getSimpleName() + RESET);
        log.info("");
    }

    @Override
    public void onAfterClass(ITestClass testClass) {
        log.info("");
        log.info(BOLD + CYAN + "└─ " + testClass.getRealClass().getSimpleName() + RESET);
        log.info("");
    }

    @Override
    public void onTestStart(ITestResult result) {
        log.info(DIM + "   ▶ " + result.getMethod().getMethodName() + RESET);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        log.info(GREEN + "   ✓ " + result.getMethod().getMethodName() + RESET);
        log.info("");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        Throwable t = result.getThrowable();
        log.info(RED + "   ✗ " + result.getMethod().getMethodName() + RESET);
        if (t != null) {
            log.info(RED + "     " + t.getMessage() + RESET);
        }
        log.info("");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        log.info(YELLOW + "   ⊘ " + result.getMethod().getMethodName() + RESET);
        log.info("");
    }
}
