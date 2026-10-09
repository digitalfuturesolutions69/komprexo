package com.komprexo.app
import com.komprexo.app.ui.documentLocale
import org.junit.Assert.assertEquals
import org.junit.Test
class LocaleMappingTest {
    @Test fun indonesianModern() { assertEquals("id",documentLocale("id-ID")) }
    @Test fun indonesianLegacy() { assertEquals("id",documentLocale("in-ID")) }
    @Test fun english() { assertEquals("en",documentLocale("en-GB")) }
    @Test fun spanish() { assertEquals("es",documentLocale("es-MX")) }
    @Test fun brazilianPortuguese() { assertEquals("pt-BR",documentLocale("pt-BR")) }
    @Test fun portugueseFallbackUsesSupportedBrazilianCatalog() { assertEquals("pt-BR",documentLocale("pt-PT")) }
    @Test fun hindi() { assertEquals("hi",documentLocale("hi-IN")) }
    @Test fun unsupportedSystemLanguageDefaultsEnglish() { assertEquals("en",documentLocale("fr-FR")) }
}
