package com.example.timerapp.data;

import com.example.timerapp.testrunner.ReportRunner;
import org.junit.Test;

import static org.junit.Assert.*;

public class SensitiveDataRedactionTest {

    @Test
    public void testGenericApiKeyRedaction() {
        String msg = "Error communicating with upstream: key sk-1234567890abcdef1234567890 rejected";
        String cleaned = ReportRunner.sanitizeSensitiveData(msg);
        assertFalse("Should not contain API key pattern", cleaned.contains("sk-1234567890abcdef1234567890"));
        assertTrue("Should contain redacted placeholder", cleaned.contains("[REDACTED_API_KEY]"));
    }

    @Test
    public void testCloudServiceApiKeyRedaction() {
        String msg = "Service error: API key AIzaSyD123456789012345678901234567890 invalid";
        String cleaned = ReportRunner.sanitizeSensitiveData(msg);
        assertFalse("Should not contain service API key", cleaned.contains("AIzaSyD123456789012345678901234567890"));
        assertTrue("Should contain redacted placeholder", cleaned.contains("[REDACTED_API_KEY]"));
    }

    @Test
    public void testGitHubTokenRedaction() {
        String msg = "Auth failure for token ghp_123456789012345678901234567890123456";
        String cleaned = ReportRunner.sanitizeSensitiveData(msg);
        assertFalse("Should not contain GitHub token", cleaned.contains("ghp_123456789012345678901234567890123456"));
        assertTrue("Should contain redacted placeholder", cleaned.contains("[REDACTED_TOKEN]"));
    }

    @Test
    public void testBearerTokenRedaction() {
        String trace = "Header: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.e30.t-IDjh Sample trace";
        String cleaned = ReportRunner.sanitizeSensitiveData(trace);
        assertFalse("Should not contain bearer token payload", cleaned.contains("eyJhbGciOiJIUzI1Ni"));
        assertTrue("Should contain redacted placeholder", cleaned.contains("Bearer [REDACTED_TOKEN]"));
    }

    @Test
    public void testPasswordAssignmentRedaction() {
        String msg = "Connecting with user=admin, password=SuperSecretPassword123! to host";
        String cleaned = ReportRunner.sanitizeSensitiveData(msg);
        assertFalse("Should not contain plaintext password", cleaned.contains("SuperSecretPassword123!"));
        assertTrue("Should contain redacted placeholder", cleaned.contains("[REDACTED_SECRET]"));
    }

    @Test
    public void testPrivateKeyBlockRedaction() {
        String block = "-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCAQEA0...\n-----END RSA PRIVATE KEY-----";
        String cleaned = ReportRunner.sanitizeSensitiveData(block);
        assertFalse("Should not contain private key header", cleaned.contains("BEGIN RSA PRIVATE KEY"));
        assertTrue("Should contain redacted placeholder", cleaned.contains("[REDACTED_PRIVATE_KEY]"));
    }

    @Test
    public void testCleanStringUnchanged() {
        String msg = "Standard test assertion failed: expected 5 but got 4";
        String cleaned = ReportRunner.sanitizeSensitiveData(msg);
        assertEquals(msg, cleaned);
    }
}
