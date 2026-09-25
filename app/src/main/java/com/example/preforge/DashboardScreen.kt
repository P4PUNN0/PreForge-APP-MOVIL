package com.example.preforge

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.preforge.data.local.AppDatabase
import com.example.preforge.data.local.toQuestion
import com.example.preforge.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class TipoFuenteSeleccionada {
    DOCUMENTO,
    IMAGEN
}

private fun supportedImageMimeTypes(): Array<String> = buildList {
    add("image/jpeg")
    add("image/png")
    add("image/webp")
    add("image/bmp")
    add("image/gif")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        add("image/heic")
        add("image/heif")
        add("image/x-heic")
        add("image/x-heif")
    }
}.toTypedArray()

private fun obtenerNombreArchivo(contexto: Context, uri: Uri): String? {
    val nombre = runCatching {
        contexto.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val indiceNombre = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (indiceNombre != -1 && cursor.moveToFirst()) {
                cursor.getString(indiceNombre)
            } else {
                null
            }
        }
    }.getOrNull()
    return nombre?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
}

@Composable
internal fun DashboardScreen(
    userId: String = "local-user",
    userName: String = "Estudiante",
    onNavigateToSimulator: (List<Question>) -> Unit = {},
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val contexto = LocalContext.current
    val ambitoCorrutina = rememberCoroutineScope()
    val procesando by dashboardViewModel.procesando.collectAsState()
    var cargandoCargaUltimo by remember { mutableStateOf(false) }
    var nombreArchivoSeleccionado by rememberSaveable { mutableStateOf<String?>(null) }
    var uriArchivoSeleccionadoTexto by rememberSaveable { mutableStateOf<String?>(null) }
    var tipoFuenteSeleccionadaTexto by rememberSaveable { mutableStateOf<String?>(null) }
    var cantidadPreguntas by rememberSaveable { mutableStateOf(5) }
    val uriArchivoSeleccionado = uriArchivoSeleccionadoTexto?.let(Uri::parse)
    val tipoFuenteSeleccionada = tipoFuenteSeleccionadaTexto?.let { nombre ->
        runCatching { TipoFuenteSeleccionada.valueOf(nombre) }.getOrNull()
    }
    val cargando = procesando || cargandoCargaUltimo
    val onNavigateToSimulatorActual by rememberUpdatedState(onNavigateToSimulator)

    LaunchedEffect(dashboardViewModel, userId) {
        dashboardViewModel.eventos.collect { evento ->
            if (evento.userId != userId) return@collect
            when (evento) {
                is DashboardEvent.Completado -> {
                    Toast.makeText(
                        contexto,
                        "Examen guardado automáticamente en Room",
                        Toast.LENGTH_SHORT
                    ).show()
                    onNavigateToSimulatorActual(evento.preguntas)
                }

                is DashboardEvent.Error -> {
                    Toast.makeText(
                        contexto,
                        "No se pudo generar el simulador: ${evento.mensaje}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    var mostrarSelectorImagen by remember { mutableStateOf(false) }
    var rutaArchivoCamaraPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var uriCamaraPendiente by rememberSaveable { mutableStateOf<String?>(null) }

    fun seleccionarFuente(uri: Uri, nombre: String?, tipo: TipoFuenteSeleccionada) {
        uriArchivoSeleccionadoTexto = uri.toString()
        nombreArchivoSeleccionado = nombre
        tipoFuenteSeleccionadaTexto = tipo.name
    }

    val selectorArchivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (!cargando) {
            uri?.let { archivoUri ->
                seleccionarFuente(
                    uri = archivoUri,
                    nombre = obtenerNombreArchivo(contexto, archivoUri) ?: "Apuntes",
                    tipo = TipoFuenteSeleccionada.DOCUMENTO
                )
            }
        }
    }

    val tiposMimeImagen = remember { supportedImageMimeTypes() }
    val selectorImagen = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (!cargando) {
            uri?.let { imagenUri ->
                runCatching {
                    contexto.contentResolver.takePersistableUriPermission(
                        imagenUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                seleccionarFuente(
                    uri = imagenUri,
                    nombre = obtenerNombreArchivo(contexto, imagenUri) ?: "Imagen de apuntes.jpg",
                    tipo = TipoFuenteSeleccionada.IMAGEN
                )
            }
        }
    }

    val selectorCamara = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { imagenCapturada ->
        val uri = uriCamaraPendiente?.let(Uri::parse)
        val archivo = rutaArchivoCamaraPendiente?.let(::File)
        uriCamaraPendiente = null
        rutaArchivoCamaraPendiente = null

        if (imagenCapturada && uri != null) {
            seleccionarFuente(
                uri = uri,
                nombre = "Foto de apuntes.jpg",
                tipo = TipoFuenteSeleccionada.IMAGEN
            )
        } else {
            archivo?.delete()
        }
    }

    fun abrirSelectorImagen() {
        if (!cargando) mostrarSelectorImagen = true
    }

    fun abrirCamara() {
        if (cargando) return
        try {
            val directorio = File(contexto.cacheDir, "images").apply { mkdirs() }
            val archivo = File.createTempFile("preforge_", ".jpg", directorio)
            val uri = FileProvider.getUriForFile(
                contexto,
                "${contexto.packageName}.fileprovider",
                archivo
            )
            rutaArchivoCamaraPendiente = archivo.absolutePath
            uriCamaraPendiente = uri.toString()
            selectorCamara.launch(uri)
        } catch (_: ActivityNotFoundException) {
            rutaArchivoCamaraPendiente?.let { File(it).delete() }
            rutaArchivoCamaraPendiente = null
            uriCamaraPendiente = null
            Toast.makeText(
                contexto,
                "No se encontró una aplicación de cámara disponible",
                Toast.LENGTH_LONG
            ).show()
        } catch (_: Exception) {
            rutaArchivoCamaraPendiente?.let { File(it).delete() }
            rutaArchivoCamaraPendiente = null
            uriCamaraPendiente = null
            Toast.makeText(
                contexto,
                "No se pudo preparar la cámara. Inténtalo de nuevo",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGreen)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil",
                    tint = DarkGreen,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Sesión activa", fontSize = 12.sp, color = PrimaryGreen)
                    Text(
                        text = userName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen
                    )
                    Text(
                        text = "ID: $userId",
                        fontSize = 10.sp,
                        color = PrimaryGreen,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Sube tus apuntes",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )
            Text(
                text = "Cada simulador se guardará automáticamente en tu cuenta local.",
                fontSize = 14.sp,
                color = PrimaryGreen
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp)
                    .border(2.dp, LightGreen, RoundedCornerShape(16.dp))
                    .clickable(enabled = !cargando) {
                        selectorArchivo.launch(
                            arrayOf(
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "text/plain"
                            )
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Subir apuntes",
                        tint = if (nombreArchivoSeleccionado != null) DarkGreen else PrimaryGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (nombreArchivoSeleccionado != null) {
                        Text(
                            text = if (tipoFuenteSeleccionada == TipoFuenteSeleccionada.IMAGEN) {
                                "Imagen seleccionada:"
                            } else {
                                "Documento seleccionado:"
                            },
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = nombreArchivoSeleccionado.orEmpty(),
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    } else {
                        Text(
                            text = "Selecciona un documento",
                            fontWeight = FontWeight.Bold,
                            color = DarkGreen
                        )
                        Text(
                            text = "Admite PDF con OCR, DOCX y TXT hasta 20 MB",
                            fontSize = 12.sp,
                            color = PrimaryGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { abrirSelectorImagen() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !cargando,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkGreen),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White,
                    contentColor = DarkGreen
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = DarkGreen
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (tipoFuenteSeleccionada == TipoFuenteSeleccionada.IMAGEN) {
                        "Cambiar imagen"
                    } else {
                        "Subir imagen"
                    },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Número de preguntas:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )
            Spacer(modifier = Modifier.height(8.dp))

            val opcionesCantidad = listOf(5, 10, 15, 20)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                opcionesCantidad.forEach { cantidad ->
                    val estaSeleccionada = cantidadPreguntas == cantidad
                    OutlinedButton(
                        onClick = { cantidadPreguntas = cantidad },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (estaSeleccionada) DarkGreen else Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (estaSeleccionada) DarkGreen else LightGreen
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        enabled = !cargando
                    ) {
                        Text(
                            text = "$cantidad",
                            color = if (estaSeleccionada) Color.White else DarkGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    uriArchivoSeleccionado?.let { uriArchivo ->
                        val cantidadSolicitada = cantidadPreguntas
                        dashboardViewModel.generar(
                            contexto = contexto,
                            uri = uriArchivo,
                            nombreArchivo = nombreArchivoSeleccionado,
                            fuenteEsImagen = tipoFuenteSeleccionada == TipoFuenteSeleccionada.IMAGEN,
                            userId = userId,
                            userName = userName,
                            cantidadPreguntas = cantidadSolicitada
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                shape = RoundedCornerShape(12.dp),
                enabled = !cargando && uriArchivoSeleccionado != null
            ) {
                Text(
                    text = if (cargando) "Procesando y generando..." else "Generar Simulador",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    cargandoCargaUltimo = true
                    ambitoCorrutina.launch {
                        try {
                            val ultimoExamen = withContext(Dispatchers.IO) {
                                AppDatabase.getDatabase(contexto)
                                    .examDao()
                                    .getLatestExamForUser(userId)
                            }
                            cargandoCargaUltimo = false
                            if (ultimoExamen != null && ultimoExamen.questions.isNotEmpty()) {
                                onNavigateToSimulatorActual(
                                    ultimoExamen.questions.map { pregunta -> pregunta.toQuestion() }
                                )
                            } else {
                                Toast.makeText(
                                    contexto,
                                    "No hay exámenes guardados para este usuario",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (excepcion: CancellationException) {
                            cargandoCargaUltimo = false
                            throw excepcion
                        } catch (excepcion: Exception) {
                            cargandoCargaUltimo = false
                            Toast.makeText(
                                contexto,
                                "Error al cargar desde Room: ${excepcion.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DarkGreen),
                shape = RoundedCornerShape(12.dp),
                enabled = !cargando
            ) {
                Text(
                    text = "Cargar Último Examen (Room)",
                    color = DarkGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (cargando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DarkGreen)
            }
        }

        if (mostrarSelectorImagen) {
            SelectorImagenSheet(
                onDismissRequest = { mostrarSelectorImagen = false },
                onGallery = {
                    mostrarSelectorImagen = false
                    selectorImagen.launch(tiposMimeImagen)
                },
                onCamera = {
                    mostrarSelectorImagen = false
                    abrirCamara()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorImagenSheet(
    onDismissRequest: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color.White,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Subir imagen",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreen
            )
            Text(
                text = "Elige una fuente para extraer el texto de tus apuntes.",
                fontSize = 14.sp,
                color = PrimaryGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = onGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LightGreen),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null,
                    tint = DarkGreen
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Elegir de la galería",
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedButton(
                onClick = onCamera,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, LightGreen),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = DarkGreen
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Tomar una foto",
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancelar",
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    val dashboardViewModel = remember { DashboardViewModel() }
    PreForgeTheme {
        DashboardScreen(
            userId = "preview-user",
            userName = "Estudiante",
            dashboardViewModel = dashboardViewModel
        )
    }
}
