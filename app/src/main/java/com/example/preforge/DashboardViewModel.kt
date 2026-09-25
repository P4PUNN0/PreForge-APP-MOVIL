package com.example.preforge

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.preforge.data.local.AppDatabase
import com.example.preforge.data.local.ExamEntity
import com.example.preforge.data.local.toEntity
import com.example.preforge.data.local.toQuestion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal sealed interface DashboardEvent {
    val userId: String

    data class Completado(
        override val userId: String,
        val preguntas: List<Question>
    ) : DashboardEvent

    data class Error(
        override val userId: String,
        val mensaje: String
    ) : DashboardEvent
}

internal class DashboardViewModel : ViewModel() {
    private val _procesando = MutableStateFlow(false)
    val procesando: StateFlow<Boolean> = _procesando.asStateFlow()

    private val canalEventos = Channel<DashboardEvent>(capacity = Channel.BUFFERED)
    val eventos = canalEventos.receiveAsFlow()

    private var trabajoGeneracion: Job? = null

    fun generar(
        contexto: Context,
        uri: Uri,
        nombreArchivo: String?,
        fuenteEsImagen: Boolean,
        userId: String,
        userName: String,
        cantidadPreguntas: Int
    ) {
        if (_procesando.value) return

        val uriSolicitado = uri
        val nombreSolicitado = nombreArchivo
        val fuenteSolicitada = fuenteEsImagen
        val userIdSolicitado = userId
        val userNameSolicitado = userName
        val cantidadSolicitada = cantidadPreguntas
        val tituloSolicitado = crearTituloExamen(nombreSolicitado)

        _procesando.value = true
        trabajoGeneracion = viewModelScope.launch {
            try {
                val texto = withContext(Dispatchers.IO) {
                    DocumentTextExtractor.extract(
                        contexto = contexto.applicationContext,
                        uri = uriSolicitado,
                        nombreArchivo = nombreSolicitado,
                        fuenteEsImagen = fuenteSolicitada
                    )
                }
                val preguntas = withContext(Dispatchers.IO) {
                    AiEngine.generateQuestions(texto, cantidadSolicitada)
                }
                val examen = ExamEntity(
                    title = tituloSolicitado,
                    userId = userIdSolicitado,
                    ownerName = userNameSolicitado,
                    sourceFileName = nombreSolicitado,
                    questionCount = preguntas.size
                )

                val examId = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(contexto.applicationContext)
                        .examDao()
                        .insertExamWithQuestions(
                            exam = examen,
                            questions = preguntas.map { pregunta -> pregunta.toEntity() }
                        )
                }
                val preguntasGuardadas = withContext(Dispatchers.IO) {
                    AppDatabase.getDatabase(contexto.applicationContext)
                        .examDao()
                        .getExam(examId.toInt())
                        ?.questions
                        ?.map { pregunta -> pregunta.toQuestion() }
                        ?: preguntas
                }

                canalEventos.send(
                    DashboardEvent.Completado(
                        userId = userIdSolicitado,
                        preguntas = preguntasGuardadas
                    )
                )
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (excepcion: Exception) {
                canalEventos.send(
                    DashboardEvent.Error(
                        userId = userIdSolicitado,
                        mensaje = mensajeError(excepcion)
                    )
                )
            } finally {
                _procesando.value = false
            }
        }
    }

    fun cancelar() {
        trabajoGeneracion?.cancel()
    }

    private fun mensajeError(excepcion: Exception): String = when (excepcion) {
        is DocumentExtractionException -> excepcion.message
        is QuestionGenerationException -> excepcion.message
        else -> null
    } ?: "Ocurrió un error inesperado. Inténtalo de nuevo"

    private fun crearTituloExamen(nombreArchivo: String?): String {
        val nombreBase = nombreArchivo
            ?.substringBeforeLast('.')
            ?.trim()
            ?.takeIf { valor -> valor.isNotBlank() }
            ?: "Examen"
        val marcaTiempo = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date())
        return "$nombreBase - $marcaTiempo"
    }
}
