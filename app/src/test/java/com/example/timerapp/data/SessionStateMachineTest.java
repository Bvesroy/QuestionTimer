package com.example.timerapp.data;

import com.example.timerapp.data.engine.SessionStateMachine;
import com.example.timerapp.data.model.BreakLog;
import com.example.timerapp.data.model.QuestionLog;
import com.example.timerapp.model.QuestionOutcome;
import com.example.timerapp.model.SessionState;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive Unit Test Suite for Explicit Session State Machine.
 * Verifies all 7 states:
 * 1. IDLE
 * 2. RUNNING_QUESTION
 * 3. PAUSED
 * 4. EXPLICIT_BREAK
 * 5. RECORDING_OUTCOME
 * 6. ADVANCING_QUESTION
 * 7. SESSION_ENDED
 *
 * Verifies all allowed and guarded illegal transitions, dual-timer continuity,
 * accidental reset recovery, and target completion triggers.
 */
public class SessionStateMachineTest {

    private SessionStateMachine sm;

    @Before
    public void setUp() {
        sm = new SessionStateMachine(1001L, 3);
    }

    @Test
    public void testInitialStateIsIdle() {
        Assert.assertEquals(SessionState.IDLE, sm.getCurrentState());
        Assert.assertEquals(1, sm.getCurrentQuestionNumber());
        Assert.assertEquals(0L, sm.getCurrentQuestionElapsedMs());
        Assert.assertEquals(0L, sm.getTotalStudyTimeMs());
        Assert.assertEquals(0L, sm.getTotalBreakTimeMs());
        Assert.assertEquals(3, sm.getTargetQuestionCount());
        Assert.assertFalse(sm.canUndoReset());
        Assert.assertTrue(sm.getRecordedQuestions().isEmpty());
        Assert.assertTrue(sm.getRecordedBreaks().isEmpty());
    }

    @Test
    public void testStartSession_transitionsToRunningQuestion() {
        sm.startSession();
        Assert.assertEquals(SessionState.RUNNING_QUESTION, sm.getCurrentState());
        Assert.assertTrue(sm.getCurrentState().isRunning());
        Assert.assertTrue(sm.getCurrentState().isActiveSession());

        // Attempting to startSession again while RUNNING must throw IllegalStateException
        try {
            sm.startSession();
            Assert.fail("Expected IllegalStateException on duplicate startSession");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("expected IDLE"));
        }
    }

    @Test
    public void testTick_onlyAccumulatesInRunningQuestion() {
        // In IDLE: tick should not accumulate
        sm.tick(5000L);
        Assert.assertEquals(0L, sm.getCurrentQuestionElapsedMs());

        sm.startSession();
        Assert.assertEquals(SessionState.RUNNING_QUESTION, sm.getCurrentState());

        // In RUNNING_QUESTION: tick accumulates
        sm.tick(12500L);
        Assert.assertEquals(12500L, sm.getCurrentQuestionElapsedMs());
        Assert.assertEquals(12500L, sm.getTotalStudyTimeMs());

        // In PAUSED: tick should not accumulate
        sm.pauseQuestion();
        Assert.assertEquals(SessionState.PAUSED, sm.getCurrentState());
        sm.tick(7000L);
        Assert.assertEquals(12500L, sm.getCurrentQuestionElapsedMs());
        Assert.assertEquals(12500L, sm.getTotalStudyTimeMs());

        // Resume and tick again
        sm.resumeQuestion();
        sm.tick(3500L);
        Assert.assertEquals(16000L, sm.getCurrentQuestionElapsedMs());
        Assert.assertEquals(16000L, sm.getTotalStudyTimeMs());
    }

    @Test
    public void testPauseAndResumeQuestion_guardsIllegalTransitions() {
        sm.startSession();

        // Cannot resume while already running
        try {
            sm.resumeQuestion();
            Assert.fail("Expected IllegalStateException on resuming while RUNNING");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("expected PAUSED"));
        }

        sm.pauseQuestion();
        Assert.assertEquals(SessionState.PAUSED, sm.getCurrentState());
        Assert.assertTrue(sm.getCurrentState().isPaused());

        // Cannot pause while already paused
        try {
            sm.pauseQuestion();
            Assert.fail("Expected IllegalStateException on pausing while PAUSED");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("expected RUNNING_QUESTION"));
        }

        sm.resumeQuestion();
        Assert.assertEquals(SessionState.RUNNING_QUESTION, sm.getCurrentState());
    }

    @Test
    public void testExplicitBreak_freezesQuestionAndRecordsBreakContinuity() {
        sm.startSession();
        sm.tick(45000L); // 45s on Q1
        Assert.assertEquals(45000L, sm.getCurrentQuestionElapsedMs());

        // Start explicit break at t=100000
        long breakStart = 100000L;
        sm.startExplicitBreak(breakStart);
        Assert.assertEquals(SessionState.EXPLICIT_BREAK, sm.getCurrentState());
        Assert.assertTrue(sm.getCurrentState().isBreak());

        // Ticks during break do NOT affect question elapsed time
        sm.tick(15000L);
        Assert.assertEquals(45000L, sm.getCurrentQuestionElapsedMs());

        // End break 10 minutes later (600,000 ms)
        long breakEnd = breakStart + 600000L;
        sm.endExplicitBreak(breakEnd);
        Assert.assertEquals(SessionState.PAUSED, sm.getCurrentState());

        // Break recorded accurately
        Assert.assertEquals(1, sm.getRecordedBreaks().size());
        BreakLog breakLog = sm.getRecordedBreaks().get(0);
        Assert.assertEquals(600000L, breakLog.getDurationMs());
        Assert.assertEquals(600000L, sm.getTotalBreakTimeMs());

        // Question elapsed time remains exactly 45,000 ms
        Assert.assertEquals(45000L, sm.getCurrentQuestionElapsedMs());
    }

    @Test
    public void testPromptRecordingOutcome_andRecordOutcomeCommit() {
        sm.startSession();
        sm.tick(62000L); // 62s on Q1

        sm.promptRecordingOutcome();
        Assert.assertEquals(SessionState.RECORDING_OUTCOME, sm.getCurrentState());

        // Committing question outcome
        QuestionLog committed = sm.recordOutcome(QuestionOutcome.CORRECT, 4.0, "Kinematics", "None");
        Assert.assertNotNull(committed);
        Assert.assertEquals(1, committed.getQuestionNum());
        Assert.assertEquals(62000L, committed.getDurationMs());
        Assert.assertEquals("Kinematics", committed.getTopic());
        Assert.assertEquals(QuestionOutcome.CORRECT.name(), committed.getOutcome());

        // State transitioned to ADVANCING_QUESTION
        Assert.assertEquals(SessionState.ADVANCING_QUESTION, sm.getCurrentState());
        Assert.assertEquals(QuestionOutcome.CORRECT, sm.getLastRecordedOutcome());
        Assert.assertEquals(4.0, sm.getLastRecordedMarks(), 0.001);
        Assert.assertEquals(1, sm.getRecordedQuestions().size());
        Assert.assertEquals(62000L, sm.getTotalStudyTimeMs());
    }

    @Test
    public void testAdvanceQuestion_advancesQuestionNumberAndResetsTimer() {
        sm.startSession();
        sm.tick(30000L);
        sm.promptRecordingOutcome();
        sm.recordOutcome(QuestionOutcome.INCORRECT, -1.0, "Rotation", "Sign Error");

        Assert.assertEquals(SessionState.ADVANCING_QUESTION, sm.getCurrentState());
        Assert.assertEquals(1, sm.getCurrentQuestionNumber());

        sm.advanceQuestion();
        Assert.assertEquals(SessionState.RUNNING_QUESTION, sm.getCurrentState());
        Assert.assertEquals(2, sm.getCurrentQuestionNumber());
        Assert.assertEquals(0L, sm.getCurrentQuestionElapsedMs());
    }

    @Test
    public void testTargetQuestionCompletion_transitionsToSessionEnded() {
        // Target is 3 questions
        sm.startSession();

        // Question 1
        sm.tick(40000L);
        sm.promptRecordingOutcome();
        sm.recordOutcome(QuestionOutcome.CORRECT, 4.0, "Organic", "");
        sm.advanceQuestion();
        Assert.assertEquals(2, sm.getCurrentQuestionNumber());
        Assert.assertEquals(SessionState.RUNNING_QUESTION, sm.getCurrentState());

        // Question 2
        sm.tick(50000L);
        sm.promptRecordingOutcome();
        sm.recordOutcome(QuestionOutcome.CORRECT, 4.0, "Inorganic", "");
        sm.advanceQuestion();
        Assert.assertEquals(3, sm.getCurrentQuestionNumber());
        Assert.assertEquals(SessionState.RUNNING_QUESTION, sm.getCurrentState());

        // Question 3 (Final target question)
        sm.tick(60000L);
        sm.promptRecordingOutcome();
        sm.recordOutcome(QuestionOutcome.CORRECT, 4.0, "Physical", "");

        // On advancing past target: must transition to SESSION_ENDED!
        sm.advanceQuestion();
        Assert.assertEquals(SessionState.SESSION_ENDED, sm.getCurrentState());
        Assert.assertTrue(sm.getCurrentState().isTerminal());
        Assert.assertFalse(sm.getCurrentState().isActiveSession());

        // Total study time: 40s + 50s + 60s = 150,000 ms
        Assert.assertEquals(150000L, sm.getTotalStudyTimeMs());
        Assert.assertEquals(3, sm.getRecordedQuestions().size());
    }

    @Test
    public void testEndSessionFromVariousStates() {
        // 1. End session from RUNNING_QUESTION
        sm.startSession();
        sm.endSession();
        Assert.assertEquals(SessionState.SESSION_ENDED, sm.getCurrentState());

        // 2. Cannot end session from SESSION_ENDED
        try {
            sm.endSession();
            Assert.fail("Expected IllegalStateException on ending already ended session");
        } catch (IllegalStateException expected) {
            Assert.assertTrue(expected.getMessage().contains("Cannot endSession"));
        }

        // 3. Reset to IDLE
        sm.resetToIdle();
        Assert.assertEquals(SessionState.IDLE, sm.getCurrentState());

        // 4. End session from EXPLICIT_BREAK automatically ends active break
        sm.startSession();
        sm.startExplicitBreak(System.currentTimeMillis());
        Assert.assertEquals(SessionState.EXPLICIT_BREAK, sm.getCurrentState());
        sm.endSession();
        Assert.assertEquals(SessionState.SESSION_ENDED, sm.getCurrentState());
        Assert.assertEquals(1, sm.getRecordedBreaks().size());
    }

    @Test
    public void testAccidentalResetAndUndo() {
        sm.startSession();
        sm.tick(88500L);
        Assert.assertEquals(88500L, sm.getCurrentQuestionElapsedMs());

        // Accidental reset triggered
        sm.resetCurrentQuestion();
        Assert.assertEquals(0L, sm.getCurrentQuestionElapsedMs());
        Assert.assertTrue(sm.canUndoReset());

        // Perform undo
        boolean restored = sm.undoResetCurrentQuestion();
        Assert.assertTrue(restored);
        Assert.assertEquals(88500L, sm.getCurrentQuestionElapsedMs());
        Assert.assertFalse(sm.canUndoReset());

        // Second undo attempt returns false
        Assert.assertFalse(sm.undoResetCurrentQuestion());
    }

    @Test
    public void testOnStateChangeListenersReceiveAllTransitions() {
        List<String> transitions = new ArrayList<>();
        sm.addListener((oldState, newState) -> transitions.add(oldState + "->" + newState));

        sm.startSession();               // IDLE -> RUNNING_QUESTION
        sm.pauseQuestion();              // RUNNING_QUESTION -> PAUSED
        sm.resumeQuestion();             // PAUSED -> RUNNING_QUESTION
        sm.startExplicitBreak(100L);     // RUNNING_QUESTION -> EXPLICIT_BREAK
        sm.endExplicitBreak(200L);       // EXPLICIT_BREAK -> PAUSED
        sm.promptRecordingOutcome();     // PAUSED -> RECORDING_OUTCOME
        sm.recordOutcome(QuestionOutcome.CORRECT, 4.0, "", ""); // RECORDING_OUTCOME -> ADVANCING_QUESTION
        sm.endSession();                 // ADVANCING_QUESTION -> SESSION_ENDED
        sm.resetToIdle();                // SESSION_ENDED -> IDLE

        Assert.assertEquals(9, transitions.size());
        Assert.assertEquals("IDLE->RUNNING_QUESTION", transitions.get(0));
        Assert.assertEquals("RUNNING_QUESTION->PAUSED", transitions.get(1));
        Assert.assertEquals("PAUSED->RUNNING_QUESTION", transitions.get(2));
        Assert.assertEquals("RUNNING_QUESTION->EXPLICIT_BREAK", transitions.get(3));
        Assert.assertEquals("EXPLICIT_BREAK->PAUSED", transitions.get(4));
        Assert.assertEquals("PAUSED->RECORDING_OUTCOME", transitions.get(5));
        Assert.assertEquals("RECORDING_OUTCOME->ADVANCING_QUESTION", transitions.get(6));
        Assert.assertEquals("ADVANCING_QUESTION->SESSION_ENDED", transitions.get(7));
        Assert.assertEquals("SESSION_ENDED->IDLE", transitions.get(8));
    }
}
