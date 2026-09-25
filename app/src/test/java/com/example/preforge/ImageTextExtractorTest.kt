package com.example.preforge

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImageTextExtractorTest {

    @Test
    fun `detects image formats from names and MIME types`() {
        assertEquals(SupportedDocumentFormat.IMAGE, detectDocumentFormat("foto.jpg", "image/jpeg"))
        assertEquals(SupportedDocumentFormat.IMAGE, detectDocumentFormat("apuntes.heic", null))
        assertEquals(SupportedDocumentFormat.IMAGE, detectDocumentFormat("imagen.txt", "image/jpeg"))
    }

    @Test
    fun `decodes an encoded image before running OCR`() = runBlocking {
        val imagen = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val bytesImagen = ByteArrayOutputStream().use { salida ->
            assertTrue(imagen.compress(Bitmap.CompressFormat.PNG, 100, salida))
            salida.toByteArray()
        }
        var reconocedorCerrado = false

        val texto = extractImageTextFromBytes(bytesImagen) {
            object : OcrTextRecognizer {
                override suspend fun recognize(imagenBitmap: Bitmap): String =
                    "Texto de la imagen"

                override fun close() {
                    reconocedorCerrado = true
                }
            }
        }

        assertEquals("Texto de la imagen", texto)
        assertTrue(reconocedorCerrado)
        imagen.recycle()
    }

    @Test
    @Config(sdk = [24])
    fun `rejects HEIC images on Android versions without platform decoding`() {
        val excepcion = assertThrows(DocumentExtractionException::class.java) {
            runBlocking {
                DocumentTextExtractor.extract(
                    contexto = RuntimeEnvironment.getApplication(),
                    uri = Uri.parse("content://preforge.test/heic"),
                    nombreArchivo = "foto.heic",
                    fuenteEsImagen = true
                )
            }
        }

        assertTrue(excepcion.message.orEmpty().contains("HEIC"))
    }

    @Test
    fun `reads an image through the production URI entry point`() = runBlocking {
        val imagen = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val bytesImagen = ByteArrayOutputStream().use { salida ->
            assertTrue(imagen.compress(Bitmap.CompressFormat.PNG, 100, salida))
            salida.toByteArray()
        }
        val uri = Uri.parse("content://preforge.test/image")
        val contexto = RuntimeEnvironment.getApplication()
        Shadows.shadowOf(contexto.contentResolver).registerInputStream(
            uri,
            ByteArrayInputStream(bytesImagen)
        )

        val texto = DocumentTextExtractor.extract(
            contexto = contexto,
            uri = uri,
            nombreArchivo = "foto.jpg",
            fuenteEsImagen = true,
            crearReconocedor = {
                object : OcrTextRecognizer {
                    override suspend fun recognize(imagenBitmap: Bitmap): String =
                        "Texto desde URI"

                    override fun close() = Unit
                }
            }
        )

        assertEquals("Texto desde URI", texto)
        imagen.recycle()
    }
}
