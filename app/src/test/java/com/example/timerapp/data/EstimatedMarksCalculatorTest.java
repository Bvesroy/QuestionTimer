package com.example.timerapp.data;

import com.example.timerapp.data.analytics.EstimatedMarksCalculator;
import com.example.timerapp.data.analytics.EstimatedMarksCalculator.JeeAdvancedResult;
import com.example.timerapp.data.analytics.EstimatedMarksCalculator.JeeMainResult;
import com.example.timerapp.data.analytics.JeeScoringCalculator.JeeAdvQuestionAttempt;
import com.example.timerapp.data.analytics.JeeScoringCalculator.JeeAdvQuestionAttempt.QuestionType;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive Unit Test Suite for JEE Main (+4/-1/0) and
 * JEE Advanced (+4/+3/+2/+1/-2/0) marking schemes and the Estimated Marks Calculator.
 */
public class EstimatedMarksCalculatorTest {

    // ==========================================
    // JEE Main Marking Scheme Tests
    // ==========================================

    @Test
    public void testJeeMainStandardScoring_allPermutations() {
        // Exam with 75 questions total:
        // 55 Correct (+4 * 55 = 220)
        // 10 Incorrect (-1 * 10 = -10)
        // 10 Skipped (0 * 10 = 0)
        // Net score = 210 out of 300
        JeeMainResult res = EstimatedMarksCalculator.calculateJeeMain(55, 10, 10, 75);

        Assert.assertEquals(75, res.totalQuestions);
        Assert.assertEquals(55, res.correctCount);
        Assert.assertEquals(10, res.incorrectCount);
        Assert.assertEquals(10, res.skippedCount);
        Assert.assertEquals(210.0, res.netScore, 0.001);
        Assert.assertEquals(300.0, res.maxPossibleScore, 0.001);

        // Accuracy: 55 / 65 = 84.615%
        Assert.assertEquals((55.0 / 65.0) * 100.0, res.accuracyPercentage, 0.001);

        // Attempt Rate: 65 / 75 = 86.667%
        Assert.assertEquals((65.0 / 75.0) * 100.0, res.attemptRatePercentage, 0.001);

        // Negative mark penalty: 10 * 1.0 = 10 marks
        Assert.assertEquals(10.0, res.negativeMarksPenalty, 0.001);

        // Marks lost from potential:
        // 10 incorrect * 5 marks lost + 10 skipped * 4 marks lost = 50 + 40 = 90 marks lost!
        Assert.assertEquals(90.0, res.marksLostFromPotential, 0.001);
    }

    @Test
    public void testJeeMainBoundaries_perfectAndZeroScores() {
        // Perfect 300 score
        JeeMainResult perfect = EstimatedMarksCalculator.calculateJeeMain(75, 0, 0, 75);
        Assert.assertEquals(300.0, perfect.netScore, 0.001);
        Assert.assertEquals(100.0, perfect.accuracyPercentage, 0.001);
        Assert.assertEquals(100.0, perfect.attemptRatePercentage, 0.001);
        Assert.assertEquals(0.0, perfect.negativeMarksPenalty, 0.001);
        Assert.assertEquals(0.0, perfect.marksLostFromPotential, 0.001);

        // Disaster score: All 75 wrong
        JeeMainResult allWrong = EstimatedMarksCalculator.calculateJeeMain(0, 75, 0, 75);
        Assert.assertEquals(-75.0, allWrong.netScore, 0.001);
        Assert.assertEquals(0.0, allWrong.accuracyPercentage, 0.001);
        Assert.assertEquals(100.0, allWrong.attemptRatePercentage, 0.001);
        Assert.assertEquals(75.0, allWrong.negativeMarksPenalty, 0.001);
        Assert.assertEquals(375.0, allWrong.marksLostFromPotential, 0.001); // 75 * 5 = 375

        // All skipped
        JeeMainResult allSkipped = EstimatedMarksCalculator.calculateJeeMain(0, 0, 75, 75);
        Assert.assertEquals(0.0, allSkipped.netScore, 0.001);
        Assert.assertEquals(0.0, allSkipped.accuracyPercentage, 0.001);
        Assert.assertEquals(0.0, allSkipped.attemptRatePercentage, 0.001);
        Assert.assertEquals(0.0, allSkipped.negativeMarksPenalty, 0.001);
        Assert.assertEquals(300.0, allSkipped.marksLostFromPotential, 0.001);
    }

    @Test
    public void testJeeMainPercentileEstimation_monotonicallyNonDecreasing() {
        double p300 = EstimatedMarksCalculator.estimateJeeMainPercentile(300.0);
        double p280 = EstimatedMarksCalculator.estimateJeeMainPercentile(280.0);
        double p250 = EstimatedMarksCalculator.estimateJeeMainPercentile(250.0);
        double p200 = EstimatedMarksCalculator.estimateJeeMainPercentile(200.0);
        double p160 = EstimatedMarksCalculator.estimateJeeMainPercentile(160.0);
        double p120 = EstimatedMarksCalculator.estimateJeeMainPercentile(120.0);
        double p90  = EstimatedMarksCalculator.estimateJeeMainPercentile(90.0);
        double p60  = EstimatedMarksCalculator.estimateJeeMainPercentile(60.0);
        double p30  = EstimatedMarksCalculator.estimateJeeMainPercentile(30.0);
        double p0   = EstimatedMarksCalculator.estimateJeeMainPercentile(0.0);
        double pNeg = EstimatedMarksCalculator.estimateJeeMainPercentile(-25.0);

        // Verification of strict ranking hierarchy
        Assert.assertTrue(p300 >= 99.9);
        Assert.assertTrue(p280 >= 99.9);
        Assert.assertTrue(p250 >= 99.5);
        Assert.assertTrue(p200 >= 98.8);
        Assert.assertTrue(p160 >= 97.0);
        Assert.assertTrue(p120 >= 94.0);
        Assert.assertTrue(p90 >= 88.0);
        Assert.assertTrue(p60 >= 75.0);
        Assert.assertTrue(p30 >= 50.0);
        Assert.assertEquals(0.0, p0, 0.001);
        Assert.assertEquals(0.0, pNeg, 0.001);

        // Monotonic ordering
        Assert.assertTrue(p300 >= p280);
        Assert.assertTrue(p280 > p250);
        Assert.assertTrue(p250 > p200);
        Assert.assertTrue(p200 > p160);
        Assert.assertTrue(p160 > p120);
        Assert.assertTrue(p120 > p90);
        Assert.assertTrue(p90 > p60);
        Assert.assertTrue(p60 > p30);
        Assert.assertTrue(p30 > p0);
    }

    // ==========================================
    // JEE Advanced Marking Scheme Tests
    // ==========================================

    @Test
    public void testJeeAdvancedMultiCorrect_partialAndNegativeMarkingRules() {
        // Case 1: Full marks (+4) -> All 4 correct options chosen, 0 incorrect
        JeeAdvQuestionAttempt qFull = new JeeAdvQuestionAttempt(QuestionType.MULTI_CORRECT, 4, 4, 0, true);
        Assert.assertEquals(4.0, qFull.calculateMarks(), 0.001);

        // Case 2: Partial marks (+3) -> 3 out of 4 correct options chosen, 0 incorrect
        JeeAdvQuestionAttempt qPartial3 = new JeeAdvQuestionAttempt(QuestionType.MULTI_CORRECT, 4, 3, 0, true);
        Assert.assertEquals(3.0, qPartial3.calculateMarks(), 0.001);

        // Case 3: Partial marks (+2) -> 2 out of 3 correct options chosen, 0 incorrect
        JeeAdvQuestionAttempt qPartial2 = new JeeAdvQuestionAttempt(QuestionType.MULTI_CORRECT, 3, 2, 0, true);
        Assert.assertEquals(2.0, qPartial2.calculateMarks(), 0.001);

        // Case 4: Partial marks (+1) -> 1 out of 2 correct options chosen, 0 incorrect
        JeeAdvQuestionAttempt qPartial1 = new JeeAdvQuestionAttempt(QuestionType.MULTI_CORRECT, 2, 1, 0, true);
        Assert.assertEquals(1.0, qPartial1.calculateMarks(), 0.001);

        // Case 5: Heavy Negative Penalty (-2) -> Even if 3 correct options chosen, choosing 1 incorrect gives -2
        JeeAdvQuestionAttempt qNeg = new JeeAdvQuestionAttempt(QuestionType.MULTI_CORRECT, 4, 3, 1, true);
        Assert.assertEquals(-2.0, qNeg.calculateMarks(), 0.001);

        // Case 6: Unattempted (0)
        JeeAdvQuestionAttempt qUnattempted = new JeeAdvQuestionAttempt(QuestionType.MULTI_CORRECT, 4, 0, 0, false);
        Assert.assertEquals(0.0, qUnattempted.calculateMarks(), 0.001);
    }

    @Test
    public void testJeeAdvancedSingleCorrectAndNumerical() {
        // Single Correct: +3 for correct, -1 for incorrect, 0 for unattempted
        JeeAdvQuestionAttempt scCorrect = new JeeAdvQuestionAttempt(QuestionType.SINGLE_CORRECT, 1, 1, 0, true);
        Assert.assertEquals(3.0, scCorrect.calculateMarks(), 0.001);

        JeeAdvQuestionAttempt scWrong = new JeeAdvQuestionAttempt(QuestionType.SINGLE_CORRECT, 1, 0, 1, true);
        Assert.assertEquals(-1.0, scWrong.calculateMarks(), 0.001);

        JeeAdvQuestionAttempt scSkip = new JeeAdvQuestionAttempt(QuestionType.SINGLE_CORRECT, 1, 0, 0, false);
        Assert.assertEquals(0.0, scSkip.calculateMarks(), 0.001);

        // Numerical: +3 for correct, 0 for incorrect (no negative), 0 for unattempted
        JeeAdvQuestionAttempt numCorrect = new JeeAdvQuestionAttempt(QuestionType.NUMERICAL, 1, 1, 0, true);
        Assert.assertEquals(3.0, numCorrect.calculateMarks(), 0.001);

        JeeAdvQuestionAttempt numWrong = new JeeAdvQuestionAttempt(QuestionType.NUMERICAL, 1, 0, 1, true);
        Assert.assertEquals(0.0, numWrong.calculateMarks(), 0.001);

        JeeAdvQuestionAttempt numSkip = new JeeAdvQuestionAttempt(QuestionType.NUMERICAL, 1, 0, 0, false);
        Assert.assertEquals(0.0, numSkip.calculateMarks(), 0.001);
    }

    @Test
    public void testJeeAdvancedCutoffQualification_subjectAndAggregateRules() {
        List<JeeAdvQuestionAttempt> attempts = new ArrayList<>();
        // 5 questions: 2 full (+8), 1 partial (+3), 1 wrong (-2), 1 unattempted (0)
        attempts.add(new JeeAdvQuestionAttempt(QuestionType.MULTI_CORRECT, 4, 4, 0, true)); // +4
        attempts.add(new JeeAdvQuestionAttempt(QuestionType.MULTI_CORRECT, 4, 4, 0, true)); // +4
        attempts.add(new JeeAdvQuestionAttempt(QuestionType.MULTI_CORRECT, 4, 3, 0, true)); // +3
        attempts.add(new JeeAdvQuestionAttempt(QuestionType.MULTI_CORRECT, 4, 2, 1, true)); // -2
        attempts.add(new JeeAdvQuestionAttempt(QuestionType.MULTI_CORRECT, 4, 0, 0, false)); // 0

        // Subject Max = 120 marks each (Total = 360 marks)
        // Scenario A: Meets all subject cutoffs (>= 10% = 12 marks) and aggregate (>= 35% = 126 marks)
        JeeAdvancedResult passed = EstimatedMarksCalculator.calculateJeeAdvanced(
                attempts, 45.0, 42.0, 48.0, 120.0, 35.0, 10.0
        );
        Assert.assertEquals(9.0, passed.netScore, 0.001); // 4 + 4 + 3 - 2 + 0 = 9.0
        Assert.assertEquals(11.0, passed.positiveMarks, 0.001); // 4 + 4 + 3 = 11.0
        Assert.assertEquals(2.0, passed.negativeDeductions, 0.001);
        Assert.assertTrue(passed.qualifiedSubjectPhysics);
        Assert.assertTrue(passed.qualifiedSubjectChemistry);
        Assert.assertTrue(passed.qualifiedSubjectMathematics);
        Assert.assertTrue(passed.qualifiedAggregate);
        Assert.assertTrue(passed.isFullyQualified());

        // Scenario B: Fails Physics cutoff (8.0 < 12.0 req) even though aggregate is 138/360 (38.3%)
        JeeAdvancedResult failedPhysics = EstimatedMarksCalculator.calculateJeeAdvanced(
                attempts, 8.0, 65.0, 65.0, 120.0, 35.0, 10.0
        );
        Assert.assertFalse(failedPhysics.qualifiedSubjectPhysics);
        Assert.assertTrue(failedPhysics.qualifiedAggregate);
        Assert.assertFalse(failedPhysics.isFullyQualified());
    }
}
