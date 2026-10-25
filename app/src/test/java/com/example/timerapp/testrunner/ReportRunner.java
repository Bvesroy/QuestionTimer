package com.example.timerapp.testrunner;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Custom JUnit test runner wrapper that executes test suites and produces:
 * 1. Standard JUnit XML report (for CI/CD and JUnit XML parsers)
 * 2. Structured JSON report (for metric ingestion and benchmark analysis)
 * 3. Human-readable console test execution summary
 *
 * Exits with code 0 on test success, or code 1 on failure.
 */
public class ReportRunner {

    public static class TestCaseResult {
        public String name;
        public String className;
        public long durationMs;
        public String status = "PASSED"; // PASSED, FAILURE, ERROR, SKIPPED
        public String failureMessage;
        public String failureType;
        public String failureTrace;
    }

    public static class TestSuiteResult {
        public String name;
        public long startTimeMs;
        public long durationMs;
        public List<TestCaseResult> testCases = new ArrayList<>();
        public int passed = 0;
        public int failed = 0;
        public int errors = 0;
        public int skipped = 0;

        public TestSuiteResult(String name) {
            this.name = name;
            this.startTimeMs = System.currentTimeMillis();
        }

        public int getTotal() {
            return passed + failed + errors + skipped;
        }
    }

    public static class ReportListener extends RunListener {
        private final Map<String, TestSuiteResult> suites = new LinkedHashMap<>();
        private final Map<String, Long> testStartTimes = new LinkedHashMap<>();
        private final Map<String, Failure> testFailures = new LinkedHashMap<>();
        private final Map<String, Boolean> isErrorMap = new LinkedHashMap<>();
        private long runStartTimeMs;
        private long runDurationMs;

        public Map<String, TestSuiteResult> getSuites() {
            return suites;
        }

        public long getRunDurationMs() {
            return runDurationMs;
        }

        private TestSuiteResult getOrCreateSuite(String className) {
            TestSuiteResult suite = suites.get(className);
            if (suite == null) {
                suite = new TestSuiteResult(className);
                suites.put(className, suite);
            }
            return suite;
        }

        @Override
        public void testRunStarted(Description description) {
            runStartTimeMs = System.currentTimeMillis();
        }

        @Override
        public void testRunFinished(Result result) {
            runDurationMs = System.currentTimeMillis() - runStartTimeMs;
            // Finalize suite durations
            for (TestSuiteResult suite : suites.values()) {
                long suiteDuration = 0;
                for (TestCaseResult tc : suite.testCases) {
                    suiteDuration += tc.durationMs;
                }
                suite.durationMs = suiteDuration;
            }
        }

        @Override
        public void testStarted(Description description) {
            testStartTimes.put(description.getDisplayName(), System.nanoTime());
        }

        @Override
        public void testFinished(Description description) {
            String key = description.getDisplayName();
            Long startNano = testStartTimes.get(key);
            long durationMs = (startNano != null)
                    ? Math.max(0, (System.nanoTime() - startNano) / 1_000_000L)
                    : 0;

            String className = description.getClassName();
            if (className == null || className.isEmpty()) {
                className = "UnknownClass";
            }
            TestSuiteResult suite = getOrCreateSuite(className);

            TestCaseResult tc = new TestCaseResult();
            tc.name = description.getMethodName();
            if (tc.name == null || tc.name.isEmpty()) {
                tc.name = description.getDisplayName();
            }
            tc.className = className;
            tc.durationMs = durationMs;

            Failure failure = testFailures.get(key);
            if (failure != null) {
                Boolean isError = isErrorMap.get(key);
                if (isError != null && isError) {
                    tc.status = "ERROR";
                    suite.errors++;
                } else {
                    tc.status = "FAILURE";
                    suite.failed++;
                }
                tc.failureMessage = sanitizeSensitiveData(failure.getMessage());
                tc.failureType = (failure.getException() != null)
                        ? failure.getException().getClass().getName()
                        : "java.lang.Throwable";
                tc.failureTrace = sanitizeSensitiveData(failure.getTrace());
            } else {
                tc.status = "PASSED";
                suite.passed++;
            }

            suite.testCases.add(tc);
        }

        @Override
        public void testFailure(Failure failure) {
            Description desc = failure.getDescription();
            String key = desc.getDisplayName();
            testFailures.put(key, failure);

            Throwable exception = failure.getException();
            boolean isAssertion = (exception instanceof AssertionError);
            isErrorMap.put(key, !isAssertion);
        }

        @Override
        public void testAssumptionFailure(Failure failure) {
            Description desc = failure.getDescription();
            String className = desc.getClassName();
            TestSuiteResult suite = getOrCreateSuite(className);

            TestCaseResult tc = new TestCaseResult();
            tc.name = desc.getMethodName();
            tc.className = className;
            tc.durationMs = 0;
            tc.status = "SKIPPED";
            tc.failureMessage = sanitizeSensitiveData(failure.getMessage());
            suite.skipped++;
            suite.testCases.add(tc);
        }

        @Override
        public void testIgnored(Description description) {
            String className = description.getClassName();
            TestSuiteResult suite = getOrCreateSuite(className);

            TestCaseResult tc = new TestCaseResult();
            tc.name = description.getMethodName();
            tc.className = className;
            tc.durationMs = 0;
            tc.status = "SKIPPED";
            suite.skipped++;
            suite.testCases.add(tc);
        }
    }

    public static void main(String[] args) {
        String xmlOutPath = "build-tests/junit-report.xml";
        String jsonOutPath = "build-tests/test-results.json";
        List<String> classNames = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            if ("--xml".equals(args[i]) && i + 1 < args.length) {
                xmlOutPath = args[++i];
            } else if ("--json".equals(args[i]) && i + 1 < args.length) {
                jsonOutPath = args[++i];
            } else if (args[i].startsWith("--")) {
                System.err.println("Unknown option: " + args[i]);
            } else {
                classNames.add(args[i]);
            }
        }

        if (classNames.isEmpty()) {
            System.err.println("Usage: java ... ReportRunner [--xml <path>] [--json <path>] <TestClass1> [<TestClass2> ...]");
            System.exit(1);
        }

        List<Class<?>> testClasses = new ArrayList<>();
        for (String cn : classNames) {
            try {
                testClasses.add(Class.forName(cn));
            } catch (ClassNotFoundException e) {
                System.err.println("ERROR: Could not load test class: " + cn);
                e.printStackTrace();
                System.exit(1);
            }
        }

        System.out.println("======================================================================");
        System.out.println("TimerStorage Test Runner with JUnit XML & JSON Reporting");
        System.out.println("Suites: " + testClasses.size() + " test classes");
        System.out.println("======================================================================");

        JUnitCore junit = new JUnitCore();
        ReportListener listener = new ReportListener();
        junit.addListener(listener);

        Result result = junit.run(testClasses.toArray(new Class<?>[0]));

        // Generate reports
        try {
            writeXmlReport(new File(xmlOutPath), listener, result);
            System.out.println("Generated JUnit XML report: " + xmlOutPath);
        } catch (Exception e) {
            System.err.println("Failed to write JUnit XML report: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            writeJsonReport(new File(jsonOutPath), listener, result);
            System.out.println("Generated JSON report:      " + jsonOutPath);
        } catch (Exception e) {
            System.err.println("Failed to write JSON report: " + e.getMessage());
            e.printStackTrace();
        }

        // Print detailed suite summary
        System.out.println("----------------------------------------------------------------------");
        for (TestSuiteResult suite : listener.getSuites().values()) {
            String status = (suite.failed == 0 && suite.errors == 0) ? "[PASS]" : "[FAIL]";
            System.out.printf(Locale.US, "%-6s %s (%d tests, %d passed, %d failed, %d err, %d skip, %d ms)%n",
                    status, suite.name, suite.getTotal(), suite.passed, suite.failed, suite.errors, suite.skipped, suite.durationMs);
            if (suite.failed > 0 || suite.errors > 0) {
                for (TestCaseResult tc : suite.testCases) {
                    if (!"PASSED".equals(tc.status) && !"SKIPPED".equals(tc.status)) {
                        System.out.printf(Locale.US, "       * %s: %s%n", tc.name, tc.failureMessage);
                    }
                }
            }
        }
        System.out.println("----------------------------------------------------------------------");
        System.out.printf(Locale.US, "Summary: %d tests run, %d passed, %d failed, %d errors, %d skipped in %.3f s%n",
                result.getRunCount(),
                (result.getRunCount() - result.getFailureCount() - listener.getSuites().values().stream().mapToInt(s -> s.errors).sum()),
                listener.getSuites().values().stream().mapToInt(s -> s.failed).sum(),
                listener.getSuites().values().stream().mapToInt(s -> s.errors).sum(),
                result.getIgnoreCount(),
                result.getRunTime() / 1000.0);
        System.out.println("Result: " + (result.wasSuccessful() ? "SUCCESS" : "FAILURE"));
        System.out.println("======================================================================");

        System.exit(result.wasSuccessful() ? 0 : 1);
    }

    public static void writeXmlReport(File file, ReportListener listener, Result result) throws Exception {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        isoFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = isoFmt.format(new Date());

        int totalFailures = 0;
        int totalErrors = 0;
        int totalSkipped = 0;
        for (TestSuiteResult suite : listener.getSuites().values()) {
            totalFailures += suite.failed;
            totalErrors += suite.errors;
            totalSkipped += suite.skipped;
        }

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.printf(Locale.US,
                    "<testsuites name=\"TimerStorage Unit Tests\" tests=\"%d\" failures=\"%d\" errors=\"%d\" skipped=\"%d\" time=\"%.3f\" timestamp=\"%s\">%n",
                    result.getRunCount(), totalFailures, totalErrors, totalSkipped,
                    result.getRunTime() / 1000.0, timestamp);

            for (TestSuiteResult suite : listener.getSuites().values()) {
                writer.printf(Locale.US,
                        "  <testsuite name=\"%s\" tests=\"%d\" failures=\"%d\" errors=\"%d\" skipped=\"%d\" time=\"%.3f\" timestamp=\"%s\">%n",
                        escapeXml(suite.name), suite.getTotal(), suite.failed, suite.errors, suite.skipped,
                        suite.durationMs / 1000.0, timestamp);

                for (TestCaseResult tc : suite.testCases) {
                    writer.printf(Locale.US,
                            "    <testcase classname=\"%s\" name=\"%s\" time=\"%.3f\"",
                            escapeXml(tc.className), escapeXml(tc.name), tc.durationMs / 1000.0);

                    if ("PASSED".equals(tc.status)) {
                        writer.println(" />");
                    } else if ("SKIPPED".equals(tc.status)) {
                        writer.println(">");
                        writer.println("      <skipped />");
                        writer.println("    </testcase>");
                    } else if ("ERROR".equals(tc.status)) {
                        writer.println(">");
                        writer.printf(Locale.US, "      <error message=\"%s\" type=\"%s\"><![CDATA[%s]]></error>%n",
                                escapeXml(tc.failureMessage != null ? tc.failureMessage : ""),
                                escapeXml(tc.failureType != null ? tc.failureType : "java.lang.Throwable"),
                                tc.failureTrace != null ? tc.failureTrace : "");
                        writer.println("    </testcase>");
                    } else { // FAILURE
                        writer.println(">");
                        writer.printf(Locale.US, "      <failure message=\"%s\" type=\"%s\"><![CDATA[%s]]></failure>%n",
                                escapeXml(tc.failureMessage != null ? tc.failureMessage : ""),
                                escapeXml(tc.failureType != null ? tc.failureType : "java.lang.AssertionError"),
                                tc.failureTrace != null ? tc.failureTrace : "");
                        writer.println("    </testcase>");
                    }
                }
                writer.println("  </testsuite>");
            }
            writer.println("</testsuites>");
        }
    }

    public static void writeJsonReport(File file, ReportListener listener, Result result) throws Exception {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        isoFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        String timestamp = isoFmt.format(new Date());

        int totalFailures = 0;
        int totalErrors = 0;
        int totalSkipped = 0;
        int totalPassed = 0;
        for (TestSuiteResult suite : listener.getSuites().values()) {
            totalFailures += suite.failed;
            totalErrors += suite.errors;
            totalSkipped += suite.skipped;
            totalPassed += suite.passed;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"timestamp\": \"").append(escapeJson(timestamp)).append("\",\n");
        sb.append("  \"success\": ").append(result.wasSuccessful()).append(",\n");
        sb.append("  \"totalTests\": ").append(result.getRunCount()).append(",\n");
        sb.append("  \"passed\": ").append(totalPassed).append(",\n");
        sb.append("  \"failed\": ").append(totalFailures).append(",\n");
        sb.append("  \"errors\": ").append(totalErrors).append(",\n");
        sb.append("  \"skipped\": ").append(totalSkipped).append(",\n");
        sb.append("  \"durationMs\": ").append(result.getRunTime()).append(",\n");
        sb.append("  \"suites\": [\n");

        List<TestSuiteResult> suiteList = new ArrayList<>(listener.getSuites().values());
        for (int i = 0; i < suiteList.size(); i++) {
            TestSuiteResult suite = suiteList.get(i);
            sb.append("    {\n");
            sb.append("      \"name\": \"").append(escapeJson(suite.name)).append("\",\n");
            sb.append("      \"total\": ").append(suite.getTotal()).append(",\n");
            sb.append("      \"passed\": ").append(suite.passed).append(",\n");
            sb.append("      \"failed\": ").append(suite.failed).append(",\n");
            sb.append("      \"errors\": ").append(suite.errors).append(",\n");
            sb.append("      \"skipped\": ").append(suite.skipped).append(",\n");
            sb.append("      \"durationMs\": ").append(suite.durationMs).append(",\n");
            sb.append("      \"tests\": [\n");

            for (int j = 0; j < suite.testCases.size(); j++) {
                TestCaseResult tc = suite.testCases.get(j);
                sb.append("        {\n");
                sb.append("          \"name\": \"").append(escapeJson(tc.name)).append("\",\n");
                sb.append("          \"className\": \"").append(escapeJson(tc.className)).append("\",\n");
                sb.append("          \"status\": \"").append(escapeJson(tc.status)).append("\",\n");
                sb.append("          \"durationMs\": ").append(tc.durationMs);

                if (tc.failureMessage != null || tc.failureTrace != null) {
                    sb.append(",\n          \"failure\": {\n");
                    sb.append("            \"message\": \"").append(escapeJson(tc.failureMessage != null ? tc.failureMessage : "")).append("\",\n");
                    sb.append("            \"type\": \"").append(escapeJson(tc.failureType != null ? tc.failureType : "")).append("\",\n");
                    sb.append("            \"trace\": \"").append(escapeJson(tc.failureTrace != null ? tc.failureTrace : "")).append("\"\n");
                    sb.append("          }\n");
                } else {
                    sb.append("\n");
                }

                sb.append("        }").append(j < suite.testCases.size() - 1 ? "," : "").append("\n");
            }
            sb.append("      ]\n");
            sb.append("    }").append(i < suiteList.size() - 1 ? "," : "").append("\n");
        }

        sb.append("  ]\n");
        sb.append("}\n");

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.write(sb.toString());
        }
    }

    public static String escapeXml(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':  sb.append("&amp;"); break;
                case '<':  sb.append("&lt;"); break;
                case '>':  sb.append("&gt;"); break;
                case '\"': sb.append("&quot;"); break;
                case '\'': sb.append("&apos;"); break;
                default:
                    if (c >= 0x20 || c == '\n' || c == '\r' || c == '\t') {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    public static String escapeJson(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }
        return sb.toString();
    }

    /**
     * Scrubs credentials, API tokens, and secrets from test failure messages and stack traces.
     */
    public static String sanitizeSensitiveData(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String sanitized = text;
        // Redact OpenAI / Anthropic / Generic API keys
        sanitized = sanitized.replaceAll("(?i)(?:sk-[A-Za-z0-9_-]{20,})", "[REDACTED_API_KEY]");
        // Redact Google / Firebase API keys
        sanitized = sanitized.replaceAll("AIza[0-9A-Za-z-_]{30,40}", "[REDACTED_API_KEY]");
        // Redact GitHub Personal Access Tokens
        sanitized = sanitized.replaceAll("(ghp_[A-Za-z0-9]{36}|github_pat_[A-Za-z0-9_]{82})", "[REDACTED_TOKEN]");
        // Redact Slack tokens
        sanitized = sanitized.replaceAll("xox[baprs]-[0-9a-zA-Z-]+", "[REDACTED_TOKEN]");
        // Redact AWS access keys
        sanitized = sanitized.replaceAll("AKIA[0-9A-Z]{16}", "[REDACTED_AWS_KEY]");
        // Redact Bearer tokens
        sanitized = sanitized.replaceAll("(?i)Bearer\\s+[A-Za-z0-9\\-\\._~\\+\\/]+=*", "Bearer [REDACTED_TOKEN]");
        // Redact explicit password / secret key-value assignments
        sanitized = sanitized.replaceAll("(?i)(password|passwd|secret|api[_-]?key|credential|private[_-]?key)\\s*[:=]\\s*['\"]?([^'\"\\s,;]+)", "$1=[REDACTED_SECRET]");
        // Redact PEM private keys
        sanitized = sanitized.replaceAll("-----BEGIN [A-Z ]*PRIVATE KEY-----[\\s\\S]*?-----END [A-Z ]*PRIVATE KEY-----", "[REDACTED_PRIVATE_KEY]");
        return sanitized;
    }
}
