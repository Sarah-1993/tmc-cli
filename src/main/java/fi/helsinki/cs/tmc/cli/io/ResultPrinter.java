package fi.helsinki.cs.tmc.cli.io;

import fi.helsinki.cs.tmc.core.domain.submission.SubmissionResult;
import fi.helsinki.cs.tmc.langs.domain.RunResult;
import fi.helsinki.cs.tmc.langs.domain.SpecialLogs;
import fi.helsinki.cs.tmc.langs.domain.TestResult;

import java.util.List;

public class ResultPrinter {

    private static final String COMPILE_ERROR_MESSAGE
            = Color.colorString("Failed to compile project", Color.AnsiColor.ANSI_PURPLE);
    private static final String FAIL = Color.colorString("Failed: ", Color.AnsiColor.ANSI_RED);
    private static final String PASS = Color.colorString("Passed: ", Color.AnsiColor.ANSI_GREEN);
    private static final String TAB = "        ";
    private static final char LF = '\n';

    private final Io io;

    private boolean showDetails;
    private boolean showPassed;
    private int passed;
    private int total;

    public ResultPrinter(Io io, boolean showDetails, boolean showPassed) {
        this.io = io;
        this.showDetails = showDetails;
        this.showPassed = showPassed;
    }

    public boolean isShowDetails() {
        return showDetails;
    }

    public boolean isShowPassed() {
        return showPassed;
    }

    public void setShowDetails(boolean showDetails) {
        this.showDetails = showDetails;
    }

    public void setShowPassed(boolean showPassed) {
        this.showPassed = showPassed;
    }

    public void printSubmissionResult(SubmissionResult result, Boolean printProgressBar,
                                      Color.AnsiColor color1, Color.AnsiColor color2) {
        if (result == null) {
            return;
        }

        this.total = result.getTestCases().size();
        this.passed = passedTests(result.getTestCases());

        printTestResults(result.getTestCases());

        switch (result.getStatus()) {
            case ERROR:
                io.println("");
                io.println(result.getError());
                break;
            case FAIL:
                String valgrind = result.getValgrind();
                if (valgrind != null && !valgrind.isEmpty()) {
                    io.println(Color.colorString("Failed due to errors in valgrind log:",
                            Color.AnsiColor.ANSI_RED));
                    io.println(valgrind);
                    return;
                }
                break;
            case PROCESSING:
                io.println("PROCESSING");
                break;
            case OK:
                //io.println("OK");
                break;
            default:
        }

        if (printProgressBar && this.total > 0) {
            io.println(TmcCliProgressObserver.getPassedTestsBar(passed, total, color1, color2));
        }
        String msg = null;
        switch (result.getTestResultStatus()) {
            case NONE_FAILED:
                msg = "All tests passed on server!";
                msg = Color.colorString(msg, Color.AnsiColor.ANSI_GREEN)
                        + "\nPoints permanently awarded: " + result.getPoints()
                        + "\nModel solution: " + result.getSolutionUrl();
                break;
            case ALL_FAILED:
                msg = Color.colorString("All tests failed on server.", Color.AnsiColor.ANSI_RED)
                        + " Please review your answer";
                break;
            case SOME_FAILED:
                msg = Color.colorString("Some tests failed on server.", Color.AnsiColor.ANSI_RED)
                        + " Please review your answer";
                break;
            default:
        }
        if (msg != null) {
            io.println(msg);
        }
    }

    public void printRunResult(RunResult result, Boolean printProgressBar,
                               Color.AnsiColor color1, Color.AnsiColor color2) {
        printTestResults(result.testResults);
        this.total = result.testResults.size();
        this.passed = passedTests(result.testResults);

        if (printProgressBar && this.total > 0) {
            io.println(TmcCliProgressObserver.getPassedTestsBar(passed, total, color1, color2));
        }

        String msg = null;
        switch (result.status) {
            case PASSED:
                msg = "All tests passed!";
                msg = Color.colorString(msg, Color.AnsiColor.ANSI_GREEN)
                        + " Submit to server with 'tmc submit'";
                break;
            case TESTS_FAILED:
                msg = "Please review your answer before submitting";
                break;
            case COMPILE_FAILED:
                msg = ResultPrinter.COMPILE_ERROR_MESSAGE;
                break;
            case TESTRUN_INTERRUPTED:
                msg = "Testrun interrupted";
                break;
            case GENERIC_ERROR:
                msg = new String(result.logs.get(SpecialLogs.GENERIC_ERROR_MESSAGE));
                break;
            default:
        }
        if (msg != null) {
            io.println(msg);
        }
    }

    public static int passedTests(List<TestResult> testResults) {
        int passed = 0;
        for (TestResult testResult : testResults) {
            if (testResult.isSuccessful()) {
                passed++;
            }
        }
        return passed;
    }

    private void printTestResults(List<TestResult> testResults) {
        for (TestResult testResult : testResults) {
            if (!testResult.isSuccessful()) {
                io.println(createFailMessage(testResult));
            } else if (showPassed) {
                io.println(createPassMessage(testResult));
            }
        }
        io.println("Test results: "
                + passedTests(testResults) + "/"
                + testResults.size() + " tests passed");

    }

    private String createFailMessage(TestResult testResult) {
        StringBuilder sb = new StringBuilder();
        sb.append(FAIL).append(testResult.getName()).append(LF);
        sb.append(TAB).append(testResult.getMessage()).append(LF);

        if (showDetails) {
            String details = listToString(testResult.getDetailedMessage(), LF);
            if (details != null) {
                sb.append(LF).append("Detailed message:").append(LF).append(details);
            }
            String exception = listToString(testResult.getException(), LF);
            if (exception != null) {
                sb.append(LF).append("Exception:").append(LF).append(exception);
            }
        }
        return sb.toString();
    }

    private String createPassMessage(TestResult testResult) {
        return PASS + testResult.getName() + LF;
    }

    private String listToString(List<String> strings, char separator) {
        if (strings == null || strings.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (String string : strings) {
            sb.append(string).append(separator);
        }
        return sb.toString();
    }

}
