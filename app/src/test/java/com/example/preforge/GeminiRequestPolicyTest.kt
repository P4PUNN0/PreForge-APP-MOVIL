package com.example.preforge

import com.google.ai.client.generativeai.type.QuotaExceededException
import com.google.ai.client.generativeai.type.ServerException
import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiRequestPolicyTest {

    @Test
    fun `waits for the quota retry interval before trying again`() {
        val exception = QuotaExceededException(
            "Quota exceeded. Please retry in 42.332829817s."
        )

        val delayMillis = GeminiRequestPolicy.retryDelayMillis(
            exception = exception,
            attemptIndex = 0
        )

        assertEquals(43_000L, delayMillis)
    }

    @Test
    fun `backs off when Gemini reports temporary high demand`() {
        val exception = ServerException(
            "This model is currently experiencing high demand. Please try again later."
        )

        val firstDelayMillis = GeminiRequestPolicy.retryDelayMillis(exception, attemptIndex = 0)
        val secondDelayMillis = GeminiRequestPolicy.retryDelayMillis(exception, attemptIndex = 1)

        assertEquals(5_000L, firstDelayMillis)
        assertEquals(10_000L, secondDelayMillis)
    }

    @Test
    fun `explains when Gemini quota is exhausted`() {
        val exception = QuotaExceededException("Quota exceeded. Please retry in 42s.")

        val message = GeminiRequestPolicy.userMessage(exception)

        assertEquals(
            "Se alcanzó la cuota de uso de Gemini. Espera a que se libere o usa una clave de API con facturación habilitada.",
            message
        )
    }

    @Test
    fun `explains when Gemini is temporarily saturated`() {
        val exception = ServerException(
            "This model is currently experiencing high demand. Please try again later."
        )

        val message = GeminiRequestPolicy.userMessage(exception)

        assertEquals(
            "Gemini está temporalmente saturado. Inténtalo de nuevo en unos minutos.",
            message
        )
    }
}
