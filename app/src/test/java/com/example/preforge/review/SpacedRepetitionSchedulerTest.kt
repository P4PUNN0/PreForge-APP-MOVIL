package com.example.preforge.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SpacedRepetitionSchedulerTest {
    private val scheduler = SpacedRepetitionScheduler()

    @Test
    fun `first successful review schedules the card for one day`() {
        val reviewedAt = 1_000L

        val result = scheduler.calculate(
            previous = ReviewSchedule.dueAt(reviewedAt),
            rating = 4,
            reviewedAt = reviewedAt
        )

        assertEquals(1, result.intervalDays)
        assertEquals(1, result.repetitions)
        assertEquals(0, result.lapses)
        assertEquals(reviewedAt + DAY, result.nextReviewAt)
        assertEquals(4, result.lastRating)
    }

    @Test
    fun `second and later successful reviews grow the interval`() {
        val first = scheduler.calculate(
            previous = ReviewSchedule.dueAt(0L),
            rating = 5,
            reviewedAt = 0L
        )
        val second = scheduler.calculate(
            previous = first,
            rating = 4,
            reviewedAt = DAY
        )
        val third = scheduler.calculate(
            previous = second,
            rating = 4,
            reviewedAt = DAY * 7
        )

        assertEquals(1, first.intervalDays)
        assertEquals(2.6, first.easeFactor, 0.0001)
        assertEquals(6, second.intervalDays)
        assertEquals(2, second.repetitions)
        assertEquals(16, third.intervalDays)
        assertEquals(3, third.repetitions)
    }

    @Test
    fun `failed review resets repetitions but keeps a personalized ease factor`() {
        val previous = ReviewSchedule(
            easeFactor = 2.5,
            intervalDays = 20,
            repetitions = 4,
            lapses = 1,
            lastRating = 4,
            lastReviewedAt = 1_000L,
            nextReviewAt = 1_000L + 20 * DAY
        )

        val result = scheduler.calculate(
            previous = previous,
            rating = 1,
            reviewedAt = 5_000L
        )

        assertEquals(1, result.intervalDays)
        assertEquals(0, result.repetitions)
        assertEquals(2, result.lapses)
        assertEquals(1.96, result.easeFactor, 0.0001)
        assertEquals(5_000L + DAY, result.nextReviewAt)
    }

    @Test
    fun `ease factor never falls below the SM-2 floor`() {
        var schedule = ReviewSchedule.dueAt(0L)

        repeat(5) {
            schedule = scheduler.calculate(schedule, rating = 0, reviewedAt = 0L)
        }

        assertEquals(ReviewSchedule.MIN_EASE_FACTOR, schedule.easeFactor, 0.0001)
        assertTrue(schedule.lapses >= 5)
    }

    @Test
    fun `rejects ratings outside the zero to five scale`() {
        assertThrows(IllegalArgumentException::class.java) {
            scheduler.calculate(ReviewSchedule.dueAt(0L), rating = -1, reviewedAt = 0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            scheduler.calculate(ReviewSchedule.dueAt(0L), rating = 6, reviewedAt = 0L)
        }
    }

    private companion object {
        const val DAY = ReviewSchedule.MILLIS_PER_DAY
    }
}
