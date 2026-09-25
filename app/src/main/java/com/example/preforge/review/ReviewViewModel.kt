package com.example.preforge.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.preforge.data.local.AppDatabase
import com.example.preforge.data.local.ReviewCardDao
import com.example.preforge.data.local.ReviewCardEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReviewUiState(
    val isSaving: Boolean = false,
    val lastUpdatedCard: ReviewCardEntity? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel de repaso: coordina la UI con ReviewUseCase y Room.
 * La sincronización con Firebase puede consumir el historial de
 * ReviewAttemptEntity sin cambiar la lógica de scheduling local.
 */
class ReviewViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val reviewUseCase = ReviewUseCase(database)

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    fun observeDueCards(
        userId: String,
        now: Long = System.currentTimeMillis(),
        limit: Int = ReviewCardDao.DEFAULT_DUE_CARD_LIMIT
    ): Flow<List<ReviewCardEntity>> = reviewUseCase.observeDueCards(
        userId = userId,
        now = now,
        limit = limit
    )

    /**
     * Llamada desde Compose: `reviewViewModel.submitRating(userId, questionId, rating)`.
     * El scheduler y la transacción de Room se ejecutan fuera del hilo principal.
     */
    fun submitRating(
        userId: String,
        questionId: Int,
        rating: Int,
        reviewedAt: Long = System.currentTimeMillis()
    ) {
        if (_uiState.value.isSaving) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val updatedCard = withContext(Dispatchers.IO) {
                    reviewUseCase.review(
                        userId = userId,
                        questionId = questionId,
                        rating = rating,
                        reviewedAt = reviewedAt
                    )
                }
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        lastUpdatedCard = updatedCard,
                        errorMessage = null
                    )
                }
            } catch (excepcion: CancellationException) {
                _uiState.update { it.copy(isSaving = false) }
                throw excepcion
            } catch (excepcion: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = excepcion.message
                            ?: "No se pudo guardar el repaso"
                    )
                }
            }
        }
    }
}
