package com.example.timerapp.data;

import com.example.timerapp.data.analytics.JeeScoringCalculator;
import com.example.timerapp.data.analytics.JeeScoringCalculator.JeeAdvQuestionAttempt;
import com.example.timerapp.data.model.ExamProfile;
import com.example.timerapp.data.model.JeeAnalyticsReport;
import com.example.timerapp.data.model.Session;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JUnit 4 Test Suite for JEE Main and JEE Advanced Analytics & Scoring Logic:
 * 1. JEE Main standard marking (+4 correct, -1 incorrect, 0 unattempted).
 * 2. 3-hour exam limit pacing evaluation (180 mins / 75 questions = 144s).
 * 3. JEE Advanced variable / multi-correct marking (+4, +3, +2, +1, -2, 0).
 * 4. Profile separation isolating JEE Main records from JEE Advanced analytics.
 */
public class JeeAnalyticsTest {

    @Test
    public void testJeeMainMarking_standardScoringFormula() {
        // Exam with 75 questions total:
        // 55 Correct (+4 * 55 = 220)
        // 12 Incorrect (-1 * 12 = -12)
        // 8 Unattempted (0 * 8 = 0)
        // Expected Net Score = 208 out of 300
        int correct = 55;
        int incorrect = 12;
        int unattempted = 8;

        double score = JeeScoringCalculator.calculateJeeMainScore(correct, incorrect, unattempted);
        Assert.assertEquals(208.0, score, 0.001);

        // All Correct
        Assert.assertEquals(300.0, JeeScoringCalculator.calculateJeeMainScore(75, 0, 0), 0.001);

        // All Incorrect (Heavy negative)
        Assert.assertEquals(-75.0, JeeScoringCalculator.calculateJeeMainScore(0, 75, 0), 0.001);

        // All Unattempted
        Assert.assertEquals(0.0, JeeScoringCalculator.calculateJeeMainScore(0, 0, 75), 0.001);
    }

    @Test
    public void testQuestionPacingVsThreeHourExamLimit_evaluatesPaceAgainstOneFortyFourSeconds() {
        // Standard JEE Main: 75 questions in 180 minutes
        // 180 mins = 10,800s / 75 questions = 144.0 seconds/question
        Assert.assertEquals(144.0, JeeScoringCalculator.JEE_MAIN_TARGET_PACE_SECONDS, 0.001);

        // Fast pace: 120.0s per question -> Well within 3-hour limit
        Assert.assertTrue(JeeScoringCalculator.isPaceWithinExamLimit(120.0, 75, 180));

        // Exactly on boundary: 144.0s per question -> Within limit
        Assert.assertTrue(JeeScoringCalculator.isPaceWithinExamLimit(144.0, 75, 180));

        // Slow pace: 160.0s per question -> Exceeds 3-hour limit (would require 200 minutes!)
        Assert.assertFalse(JeeScoringCalculator.isPaceWithinExamLimit(160.0, 75, 180));

        // Dynamic checks for partial test: 30 questions in 60 minutes -> target = 120s
        Assert.assertTrue(JeeScoringCalculator.isPaceWithinExamLimit(110.0, 30, 60));
        Assert.assertFalse(JeeScoringCalculator.isPaceWithinExamLimit(130.0, 30, 60));
    }

    @Test
    public void testJeeAdvancedMultiCorrect_variableAndPartialMarkingCalculations() {
        List<JeeAdvQuestionAttempt> attempts = new ArrayList<>();

        // Q1: Multi-correct (4 correct options A,B,C,D). Student chose all 4 -> +4 (Full marks)
        attempts.add(new JeeAdvQuestionAttempt(
                JeeAdvQuestionAttempt.QuestionType.MULTI_CORRECT,
                4, 4, 0, true
        ));

        // Q2: Multi-correct (4 correct options). Student chose 3 correct options and no wrong -> +3 (Partial)
        attempts.add(new JeeAdvQuestionAttempt(
                JeeAdvQuestionAttempt.QuestionType.MULTI_CORRECT,
                4, 3, 0, true
        ));

        // Q3: Multi-correct (3 correct options). Student chose 2 correct options and no wrong -> +2 (Partial)
        attempts.add(new JeeAdvQuestionAttempt(
                JeeAdvQuestionAttempt.QuestionType.MULTI_CORRECT,
                3, 2, 0, true
        ));

        // Q4: Multi-correct (2 correct options). Student chose 1 correct option and no wrong -> +1 (Partial)
        attempts.add(new JeeAdvQuestionAttempt(
                JeeAdvQuestionAttempt.QuestionType.MULTI_CORRECT,
                2, 1, 0, true
        ));

        // Q5: Multi-correct. Student chose 2 correct, but ALSO 1 incorrect option -> -2 (Penalty!)
        attempts.add(new JeeAdvQuestionAttempt(
                JeeAdvQuestionAttempt.QuestionType.MULTI_CORRECT,
                3, 2, 1, true
        ));

        // Q6: Multi-correct. Unattempted -> 0
        attempts.add(new JeeAdvQuestionAttempt(
                JeeAdvQuestionAttempt.QuestionType.MULTI_CORRECT,
                4, 0, 0, false
        ));

        // Q7: Single-correct. Correct -> +3
        attempts.add(new JeeAdvQuestionAttempt(
                JeeAdvQuestionAttempt.QuestionType.SINGLE_CORRECT,
                1, 1, 0, true
        ));

        // Q8: Single-correct. Wrong -> -1
        attempts.add(new JeeAdvQuestionAttempt(
                JeeAdvQuestionAttempt.QuestionType.SINGLE_CORRECT,
                1, 0, 1, true
        ));

        // Q9: Numerical. Correct -> +3
        attempts.add(new JeeAdvQuestionAttempt(
                JeeAdvQuestionAttempt.QuestionType.NUMERICAL,
                1, 1, 0, true
        ));

        // Q10: Numerical. Wrong -> 0 (no negative)
        attempts.add(new JeeAdvQuestionAttempt(
                JeeAdvQuestionAttempt.QuestionType.NUMERICAL,
                1, 0, 1, true
        ));

        // Score breakdown:
        // Q1: +4
        // Q2: +3
        // Q3: +2
        // Q4: +1
        // Q5: -2
        // Q6: 0
        // Q7: +3
        // Q8: -1
        // Q9: +3
        // Q10: 0
        // Sum = 4 + 3 + 2 + 1 - 2 + 0 + 3 - 1 + 3 + 0 = 13.0
        double totalScore = JeeScoringCalculator.calculateJeeAdvancedScore(attempts);
        Assert.assertEquals(13.0, totalScore, 0.001);
    }

    @Test
    public void testProfileSeparation_assertsJeeMainDoesNotBleedIntoJeeAdvanced() {
        long now = System.currentTimeMillis();

        Session main1 = new Session("JEE Main Mock 1", 75, false);
        main1.setExamProfile("JEE_MAIN");
        main1.setProfileId("JEE_MAIN");

        Session main2 = new Session("JEE Main Mock 2", 75, false);
        main2.setExamProfile("JEE_MAIN");
        main2.setProfileId("JEE_MAIN");

        Session adv1 = new Session("JEE Advanced Paper 1", 54, false);
        adv1.setExamProfile("JEE_ADVANCED");
        adv1.setProfileId("JEE_ADVANCED");

        Session adv2 = new Session("JEE Advanced Paper 2", 54, false);
        adv2.setExamProfile("JEE_ADVANCED");
        adv2.setProfileId("JEE_ADVANCED");

        Session general = new Session("General Physics Practice", 30, false);
        general.setExamProfile("PRACTICE");
        general.setProfileId("default");

        List<Session> allSessions = Arrays.asList(main1, main2, adv1, adv2, general);

        // Filter for JEE Main
        List<Session> mainSessions = JeeScoringCalculator.filterSessionsByProfile(allSessions, "JEE_MAIN");
        Assert.assertEquals(2, mainSessions.size());
        for (Session s : mainSessions) {
            Assert.assertEquals("JEE_MAIN", s.getExamProfile());
            Assert.assertFalse("JEE Advanced sessions must not bleed into JEE Main", s.getName().contains("Advanced"));
        }

        // Filter for JEE Advanced
        List<Session> advSessions = JeeScoringCalculator.filterSessionsByProfile(allSessions, "JEE_ADVANCED");
        Assert.assertEquals(2, advSessions.size());
        for (Session s : advSessions) {
            Assert.assertEquals("JEE_ADVANCED", s.getExamProfile());
            Assert.assertFalse("JEE Main sessions must not bleed into JEE Advanced", s.getName().contains("Main"));
        }

        // Filter for Practice
        List<Session> practiceSessions = JeeScoringCalculator.filterSessionsByProfile(allSessions, "PRACTICE");
        Assert.assertEquals(1, practiceSessions.size());
        Assert.assertEquals("General Physics Practice", practiceSessions.get(0).getName());
    }

    @Test
    public void testGenerateJeeReport_aggregatesMetricsAccurately() {
        List<Double> paces = Arrays.asList(110.0, 130.0, 160.0, 150.0, 100.0); // 2 paces exceed 144s
        JeeAnalyticsReport report = JeeScoringCalculator.generateJeeReport(
                "JEE_MAIN",
                paces,
                5,
                4,
                1,
                75,
                Collections.singletonMap("Physics", 650000L),
                Collections.singletonMap("Calculation Error", 1)
        );

        Assert.assertEquals("JEE_MAIN", report.getProfileName());
        Assert.assertEquals(5, report.getQuestionsAttempted());
        Assert.assertEquals(4, report.getQuestionsCorrect());
        Assert.assertEquals(1, report.getQuestionsIncorrect());
        Assert.assertEquals(70, report.getQuestionsSkipped());
        Assert.assertEquals(80.0, report.getAccuracyPercentage(), 0.001); // 4/5 = 80%
        Assert.assertEquals(2, report.getQuestionsExceedingTargetPace()); // 160s and 150s
        Assert.assertEquals(15.0, report.getProjectedScore(), 0.001); // (4 * 4) - 1 = 15.0
        Assert.assertEquals(300.0, report.getMaxPossibleScore(), 0.001); // 75 * 4 = 300.0
    }
}
