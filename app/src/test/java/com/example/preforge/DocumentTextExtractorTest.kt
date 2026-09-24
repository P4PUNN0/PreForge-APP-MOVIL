package com.example.preforge

import android.graphics.Bitmap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DocumentTextExtractorTest {

    @Before
    fun initializePdfResources() {
        PDFBoxResourceLoader.init(RuntimeEnvironment.getApplication())
    }

    @Test
    fun `detects supported formats from names and MIME types`() {
        assertEquals(SupportedDocumentFormat.PDF, detectDocumentFormat("apuntes.pdf", null))
        assertEquals(SupportedDocumentFormat.DOCX, detectDocumentFormat(null, TIPO_MIME_DOCX))
        assertEquals(SupportedDocumentFormat.TEXT, detectDocumentFormat("notas.TXT", "text/plain"))
    }

    @Test
    fun `extracts visible PDF text instead of PDF operators`() {
        val bytesPdf = decodePdf(PDF_CON_TEXTO)
        assertTrue(String(bytesPdf, Charsets.ISO_8859_1).contains("/Filter /FlateDecode"))

        val textoExtraido = extractDocumentText(
            ByteArrayInputStream(bytesPdf),
            SupportedDocumentFormat.PDF
        )

        assertTrue(textoExtraido.contains("La mitocondria produce ATP"))
        assertFalse(textoExtraido.contains("FlateDecode"))
    }

    @Test
    fun `extracts paragraphs from a DOCX archive`() {
        val bytesDocx = ByteArrayOutputStream().use { salida ->
            ZipOutputStream(salida).use { salidaZip ->
                salidaZip.putNextEntry(ZipEntry("word/document.xml"))
                salidaZip.write(
                    """
                        <?xml version="1.0" encoding="UTF-8"?>
                        <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                          <w:body>
                            <w:p><w:r><w:t>La mitocondria</w:t></w:r></w:p>
                            <w:p><w:r><w:t>produce ATP</w:t></w:r></w:p>
                          </w:body>
                        </w:document>
                    """.trimIndent().toByteArray()
                )
                salidaZip.closeEntry()
            }
            salida.toByteArray()
        }

        val textoExtraido = extractDocumentText(
            ByteArrayInputStream(bytesDocx),
            SupportedDocumentFormat.DOCX
        )

        assertEquals("La mitocondria\nproduce ATP", textoExtraido)
    }

    @Test
    fun `uses OCR for a PDF page without native text`() = runBlocking {
        var llamadasReconocimiento = 0
        var reconocedorCerrado = false

        val textoExtraido = extractPdfTextWithOcr(decodePdf(PDF_VACIO)) {
            object : OcrTextRecognizer {
                override suspend fun recognize(imagenBitmap: Bitmap): String {
                    llamadasReconocimiento++
                    assertTrue(imagenBitmap.width > 0 && imagenBitmap.height > 0)
                    return "Texto reconocido por OCR"
                }

                override fun close() {
                    reconocedorCerrado = true
                }
            }
        }

        assertEquals("Texto reconocido por OCR", textoExtraido)
        assertEquals(1, llamadasReconocimiento)
        assertTrue(reconocedorCerrado)
    }

    @Test
    fun `does not invoke OCR when PDF text is already extractable`() = runBlocking {
        var llamadasReconocimiento = 0

        val textoExtraido = extractPdfTextWithOcr(decodePdf(PDF_CON_TEXTO)) {
            object : OcrTextRecognizer {
                override suspend fun recognize(imagenBitmap: Bitmap): String {
                    llamadasReconocimiento++
                    return "OCR inesperado"
                }

                override fun close() = Unit
            }
        }

        assertTrue(textoExtraido.contains("La mitocondria produce ATP"))
        assertEquals(0, llamadasReconocimiento)
    }

    @Test
    fun `rejects a PDF that has no extractable text`() {
        val bytesPdf = decodePdf(PDF_VACIO)

        val excepcion = assertThrows(DocumentExtractionException::class.java) {
            extractDocumentText(ByteArrayInputStream(bytesPdf), SupportedDocumentFormat.PDF)
        }
        assertTrue(excepcion.message.orEmpty().contains("texto legible"))
    }

    private fun decodePdf(pdfBase64: String): ByteArray =
        Base64.getDecoder().decode(pdfBase64)

    private companion object {
        const val TIPO_MIME_DOCX =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

        const val PDF_CON_TEXTO =
            "JVBERi0xLjQKMSAwIG9iago8PCAvVHlwZSAvQ2F0YWxvZyAvUGFnZXMgMiAwIFIgPj4K" +
                "ZW5kb2JqCjIgMCBvYmoKPDwgL1R5cGUgL1BhZ2VzIC9LaWRzIFszIDAgUl0gL0NvdW50IDEgPj4K" +
                "ZW5kb2JqCjMgMCBvYmoKPDwgL1R5cGUgL1BhZ2UgL1BhcmVudCAyIDAgUiAvTWVkaWFCb3ggWzAgMCA2MTIgNzkyXSAv" +
                "UmVzb3VyY2VzIDw8IC9Gb250IDw8IC9GMSA1IDAgUiA+PiA+PiAvQ29udGVudHMgNCAwIFIgPj4KZW5kb2JqCjQgMCBvYmoK" +
                "PDwgL0xlbmd0aCA1NyA+PgpzdHJlYW0KQlQgL0YxIDEyIFRmIDcyIDcyMCBUZCAoTGEgbWl0b2NvbmRyaWEgcHJvZHVjZSBB" +
                "VFApIFRqIEVUCmVuZHN0cmVhbQplbmRvYmoKNSAwIG9iago8PCAvVHlwZSAvRm9udCAvU3VidHlwZSAvVHlwZTEgL0Jhc2VG" +
                "b250IC9IZWx2ZXRpY2EgPj4KZW5kb2JqCjYgMCBvYmoKPDwgL0xlbmd0aCAzIDAgUiAvRmlsdGVyIC9GbGF0ZURlY29kZSA+PgplbmRv" +
                "YmoKeHJlZgowIDcKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDA5IDAwMDAwIG4gCjAwMDAwMDAwNTggMDAwMDAgbiAKMDAw" +
                "MDAwMDExNSAwMDAwMCBuIAowMDAwMDAwMjQxIDAwMDAwIG4gCjAwMDAwMDAzNDggMDAwMDAgbiAKMDAwMDAwMDQxOCAwMDAwMCBu" +
                "IAp0cmFpbGVyCjw8IC9TaXplIDcgL1Jvb3QgMSAwIFIgPj4Kc3RhcnR4cmVmCjQ3NAolJUVPRgo="

        const val PDF_VACIO =
            "JVBERi0xLjQKMSAwIG9iago8PCAvVHlwZSAvQ2F0YWxvZyAvUGFnZXMgMiAwIFIgPj4KZW5kb2JqCjIgMCBvYmoK" +
                "PDwgL1R5cGUgL1BhZ2VzIC9LaWRzIFszIDAgUl0gL0NvdW50IDEgPj4KZW5kb2JqCjMgMCBvYmoKPDwgL1R5cGUgL1BhZ2Ug" +
                "L1BhcmVudCAyIDAgUiAvTWVkaWFCb3ggWzAgMCA2MTIgNzkyXSAvUmVzb3VyY2VzIDw8ID4+IC9Db250ZW50cyA0IDAgUiA+PgplbmRv" +
                "YmoKNCAwIG9iago8PCAvTGVuZ3RoIDAgPj4Kc3RyZWFtCgplbmRzdHJlYW0KZW5kb2JqCnhyZWYKMCA1CjAwMDAwMDAwMDAgNjU1" +
                "MzUgZiAKMDAwMDAwMDAwOSAwMDAwMCBuIAowMDAwMDAwMDU4IDAwMDAwIG4gCjAwMDAwMDAxMTUgMDAwMDAgbiAKMDAwMDAwMDIx" +
                "OSAwMDAwMCBuIAp0cmFpbGVyCjw8IC9TaXplIDUgL1Jvb3QgMSAwIFIgPj4Kc3RhcnR4cmVmCjI2OAolJUVPRgo="
    }
}
