package com.example.preforge

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min
import kotlin.math.sqrt

internal const val TAMANO_MAXIMO_DOCUMENTO_BYTES = 20L * 1024 * 1024

internal enum class SupportedDocumentFormat {
    PDF,
    DOCX,
    TEXT
}

internal class DocumentExtractionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

internal interface OcrTextRecognizer {
    suspend fun recognize(imagenBitmap: Bitmap): String

    fun close()
}

private class MlKitOcrTextRecognizer : OcrTextRecognizer {
    private val reconocedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun recognize(imagenBitmap: Bitmap): String =
        suspendCancellableCoroutine { continuacion ->
            reconocedor.process(InputImage.fromBitmap(imagenBitmap, 0))
                .addOnSuccessListener { resultado ->
                    if (continuacion.isActive) continuacion.resume(resultado.text)
                }
                .addOnFailureListener { excepcion ->
                    if (continuacion.isActive) continuacion.resumeWithException(excepcion)
                }
                .addOnCanceledListener {
                    continuacion.cancel()
                }
        }

    override fun close() {
        reconocedor.close()
    }
}

internal object DocumentTextExtractor {
    @Volatile
    private var pdfBoxInicializado = false

    suspend fun extract(
        contexto: Context,
        uri: Uri,
        nombreArchivo: String?
    ): String {
        val resolutor = contexto.contentResolver
        val tipoMime = resolutor.getType(uri)
        val formato = detectDocumentFormat(nombreArchivo, tipoMime)
        val tamanoDeclarado = resolutor.openAssetFileDescriptor(uri, "r")
            ?.use { descriptor -> descriptor.length }
            ?: -1L
        if (tamanoDeclarado > TAMANO_MAXIMO_DOCUMENTO_BYTES) {
            throw DocumentExtractionException("El archivo supera el límite de 20 MB")
        }

        if (formato == SupportedDocumentFormat.PDF) {
            inicializarPdfBox(contexto)
        }

        val bytesDocumento = resolutor.openInputStream(uri)?.use { entrada ->
            leerBytesDocumento(entrada)
        } ?: throw DocumentExtractionException("No se pudo abrir el archivo seleccionado")

        return if (formato == SupportedDocumentFormat.PDF) {
            exigirTextoExtraido(extractPdfTextWithOcr(bytesDocumento))
        } else {
            extractDocumentText(ByteArrayInputStream(bytesDocumento), formato)
        }
    }

    @Synchronized
    private fun inicializarPdfBox(contexto: Context) {
        if (!pdfBoxInicializado) {
            PDFBoxResourceLoader.init(contexto.applicationContext)
            pdfBoxInicializado = true
        }
    }
}

internal fun detectDocumentFormat(
    nombreArchivo: String?,
    tipoMime: String?
): SupportedDocumentFormat {
    val tipoMimeNormalizado = tipoMime?.trim()?.lowercase(Locale.ROOT)
    val extensionArchivo = nombreArchivo
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.trim()
        ?.lowercase(Locale.ROOT)
        .orEmpty()

    return when {
        tipoMimeNormalizado == TIPO_MIME_PDF || extensionArchivo == "pdf" -> SupportedDocumentFormat.PDF
        tipoMimeNormalizado == TIPO_MIME_DOCX || extensionArchivo == "docx" -> SupportedDocumentFormat.DOCX
        tipoMimeNormalizado == TIPO_MIME_TXT || extensionArchivo == "txt" -> SupportedDocumentFormat.TEXT
        else -> throw DocumentExtractionException("El formato del archivo no es compatible")
    }
}

internal fun extractDocumentText(
    entrada: InputStream,
    formato: SupportedDocumentFormat
): String {
    val textoExtraido = try {
        when (formato) {
            SupportedDocumentFormat.PDF -> extractPdfText(entrada)
            SupportedDocumentFormat.DOCX -> extractDocxText(entrada)
            SupportedDocumentFormat.TEXT -> entrada.bufferedReader(Charsets.UTF_8).readText()
        }
    } catch (excepcion: DocumentExtractionException) {
        throw excepcion
    } catch (excepcion: Exception) {
        throw DocumentExtractionException(
            "No se pudo leer el archivo. Puede estar dañado o protegido",
            excepcion
        )
    }

    return exigirTextoExtraido(textoExtraido)
}

private fun extractPdfText(entrada: InputStream): String =
    PDDocument.load(entrada).use { documento ->
        extraerTodoTextoPdf(documento)
    }

private fun extraerTodoTextoPdf(documento: PDDocument): String =
    normalizarTextoExtraido(PDFTextStripper().getText(documento))

internal suspend fun extractPdfTextWithOcr(
    bytesDocumento: ByteArray,
    creadorReconocedor: () -> OcrTextRecognizer = { MlKitOcrTextRecognizer() }
): String = try {
    PDDocument.load(ByteArrayInputStream(bytesDocumento)).use { documento ->
        val cantidadPaginas = documento.numberOfPages
        val textoNativoCompleto = extraerTodoTextoPdf(documento)
        if (cantidadPaginas == 0 || textoNativoCompleto.length >= cantidadPaginas * MIN_CARACTERES_PAGINA_NATIVA) {
            textoNativoCompleto
        } else {
            extraerPaginasConOcr(documento, creadorReconocedor)
        }
    }
} catch (excepcion: CancellationException) {
    throw excepcion
} catch (excepcion: DocumentExtractionException) {
    throw excepcion
} catch (excepcion: OutOfMemoryError) {
    throw DocumentExtractionException(
        "El PDF es demasiado grande para procesarlo en este dispositivo",
        excepcion
    )
} catch (excepcion: Exception) {
    throw DocumentExtractionException(
        "No se pudo leer el PDF. Puede estar dañado o protegido",
        excepcion
    )
}

private suspend fun extraerPaginasConOcr(
    documento: PDDocument,
    creadorReconocedor: () -> OcrTextRecognizer
): String {
    val renderizador = PDFRenderer(documento)
    val paginasExtraidas = StringBuilder()
    var reconocedor: OcrTextRecognizer? = null
    var paginasProcesadasConOcr = 0

    try {
        for (indicePagina in 0 until documento.numberOfPages) {
            currentCoroutineContext().ensureActive()
            val textoNativo = extraerTextoNativoPagina(documento, indicePagina)
            val textoPagina = if (textoNativo.length >= MIN_CARACTERES_PAGINA_NATIVA) {
                textoNativo
            } else {
                paginasProcesadasConOcr++
                if (paginasProcesadasConOcr > MAX_PAGINAS_OCR) {
                    throw DocumentExtractionException(
                        "El PDF escaneado supera las $MAX_PAGINAS_OCR páginas. Divídelo en archivos más pequeños"
                    )
                }

                val ppp = calcularPppOcr(documento.getPage(indicePagina))
                Log.d(
                    "DocumentTextExtractor",
                    "Aplicando OCR a la página ${indicePagina + 1}/${documento.numberOfPages}"
                )
                val imagenBitmap = renderizador.renderImageWithDPI(indicePagina, ppp, ImageType.RGB)
                var reciclarImagen = true
                try {
                    currentCoroutineContext().ensureActive()
                    val reconocedorActivo = reconocedor ?: creadorReconocedor()
                        .also { reconocedor = it }
                    normalizarTextoExtraido(reconocedorActivo.recognize(imagenBitmap))
                } catch (excepcion: CancellationException) {
                    reciclarImagen = false
                    throw excepcion
                } catch (excepcion: DocumentExtractionException) {
                    throw excepcion
                } catch (excepcion: OutOfMemoryError) {
                    throw DocumentExtractionException(
                        "Una página del PDF es demasiado grande para procesarla con OCR",
                        excepcion
                    )
                } catch (excepcion: Exception) {
                    Log.e("DocumentTextExtractor", "OCR falló en la página ${indicePagina + 1}", excepcion)
                    throw DocumentExtractionException(
                        "No se pudo completar el OCR del PDF escaneado",
                        excepcion
                    )
                } finally {
                    if (reciclarImagen && !imagenBitmap.isRecycled) {
                        imagenBitmap.recycle()
                    }
                }
            }

            if (textoPagina.isNotBlank()) {
                if (paginasExtraidas.isNotEmpty()) paginasExtraidas.append('\n')
                paginasExtraidas.append(textoPagina)
            }
        }
    } finally {
        runCatching { reconocedor?.close() }
    }

    return normalizarTextoExtraido(paginasExtraidas.toString())
}

private fun extraerTextoNativoPagina(documento: PDDocument, indicePagina: Int): String {
    val extractorTexto = PDFTextStripper().apply {
        startPage = indicePagina + 1
        endPage = indicePagina + 1
    }
    return normalizarTextoExtraido(extractorTexto.getText(documento))
}

private fun calcularPppOcr(pagina: PDPage): Float {
    val cajaRecorte = pagina.cropBox
    var anchoPuntos = cajaRecorte.width.toDouble()
    var altoPuntos = cajaRecorte.height.toDouble()
    if (pagina.rotation % 180 != 0) {
        val anchoAnterior = anchoPuntos
        anchoPuntos = altoPuntos
        altoPuntos = anchoAnterior
    }

    val areaPagina = anchoPuntos * altoPuntos
    if (anchoPuntos <= 0 || altoPuntos <= 0 || areaPagina <= 0) {
        throw DocumentExtractionException("El PDF contiene una página con dimensiones inválidas")
    }

    val pppMaximo = sqrt(MAX_PIXELES_RENDERIZADO_OCR / areaPagina) * PUNTOS_POR_PULGADA
    val ppp = min(DPI_RENDERIZADO_OCR, pppMaximo)
    if (ppp < DPI_MINIMO_RENDERIZADO_OCR) {
        throw DocumentExtractionException(
            "Una página del PDF es demasiado grande para aplicar OCR con calidad suficiente"
        )
    }
    return ppp.toFloat()
}

private fun extractDocxText(entrada: InputStream): String {
    val bytesXmlDocumento = ZipInputStream(BufferedInputStream(entrada)).use { entradaZip ->
        generateSequence { entradaZip.nextEntry }
            .firstOrNull { entrada ->
                !entrada.isDirectory && entrada.name.equals(ENTRADA_DOCUMENTO_DOCX, ignoreCase = true)
            }
            ?.let { entradaZip.readBytes() }
    } ?: throw DocumentExtractionException("El documento DOCX no contiene un archivo principal válido")

    if (String(bytesXmlDocumento, Charsets.UTF_8).contains("<!DOCTYPE", ignoreCase = true)) {
        throw DocumentExtractionException("El documento DOCX contiene una estructura XML no válida")
    }

    val fabricaParser = SAXParserFactory.newInstance().apply {
        isNamespaceAware = true
        try {
            isXIncludeAware = false
        } catch (_: Exception) {
            // Algunos analizadores de Android no exponen XInclude.
        }
        configurarCaracteristicaSegura("http://apache.org/xml/features/disallow-doctype-decl")
        configurarCaracteristicaSegura("http://xml.org/sax/features/external-general-entities", false)
        configurarCaracteristicaSegura("http://xml.org/sax/features/external-parameter-entities", false)
    }
    val manejador = DocxTextHandler()
    fabricaParser.newSAXParser().parse(
        InputSource(ByteArrayInputStream(bytesXmlDocumento)),
        manejador
    )
    return manejador.texto
}

private fun SAXParserFactory.configurarCaracteristicaSegura(
    caracteristica: String,
    valor: Boolean = true
) {
    try {
        setFeature(caracteristica, valor)
    } catch (_: Exception) {
        // Algunos analizadores de Android no exponen todas las características SAX.
    }
}

private class DocxTextHandler : DefaultHandler() {
    private val textoExtraido = StringBuilder()
    private var esNodoTexto = false

    val texto: String
        get() = textoExtraido.toString()

    override fun startElement(
        uri: String?,
        localName: String?,
        qName: String?,
        attributes: Attributes?
    ) {
        when (nombreElemento(localName, qName)) {
            "p" -> textoExtraido.append('\n')
            "t" -> esNodoTexto = true
            "tab" -> textoExtraido.append('\t')
            "br", "cr" -> textoExtraido.append('\n')
        }
    }

    override fun characters(ch: CharArray, start: Int, length: Int) {
        if (esNodoTexto) {
            textoExtraido.append(ch, start, length)
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        if (nombreElemento(localName, qName) == "t") {
            esNodoTexto = false
        }
    }

    private fun nombreElemento(nombreLocal: String?, nombreCalificado: String?): String =
        (nombreLocal?.takeIf(String::isNotBlank) ?: nombreCalificado.orEmpty())
            .substringAfterLast(':')
}

private fun leerBytesDocumento(entrada: InputStream): ByteArray {
    val salida = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0L

    while (true) {
        val bytesLeidos = entrada.read(buffer)
        if (bytesLeidos < 0) break
        totalBytes += bytesLeidos
        if (totalBytes > TAMANO_MAXIMO_DOCUMENTO_BYTES) {
            throw DocumentExtractionException("El archivo supera el límite de 20 MB")
        }
        salida.write(buffer, 0, bytesLeidos)
    }

    return salida.toByteArray()
}

private fun normalizarTextoExtraido(texto: String): String =
    texto.replace("\r\n", "\n")
        .replace('\r', '\n')
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString("\n")

private fun exigirTextoExtraido(texto: String): String =
    normalizarTextoExtraido(texto).takeIf(String::isNotBlank)
        ?: throw DocumentExtractionException("No se encontró texto legible en el archivo")

private const val TIPO_MIME_PDF = "application/pdf"
private const val TIPO_MIME_DOCX =
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
private const val TIPO_MIME_TXT = "text/plain"
private const val ENTRADA_DOCUMENTO_DOCX = "word/document.xml"
private const val MIN_CARACTERES_PAGINA_NATIVA = 25
private const val MAX_PAGINAS_OCR = 50
private const val DPI_RENDERIZADO_OCR = 180.0
private const val DPI_MINIMO_RENDERIZADO_OCR = 120.0
private const val MAX_PIXELES_RENDERIZADO_OCR = 4_000_000.0
private const val PUNTOS_POR_PULGADA = 72.0
