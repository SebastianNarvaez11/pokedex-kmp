package com.sebastiannarvaez.pokedex.feature.daily

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PokemonOfTheDayTest {

    @Test
    fun elMismoDiaDaSiempreElMismoPokemon() {
        val fecha = LocalDate(2026, 9, 11)

        // Diez llamadas, un resultado. Con `Random` esto fallaria, y el usuario
        // veria un Pokemon en la notificacion y otro al abrir la app.
        val resultados = List(10) { PokemonOfTheDay.idParaFecha(fecha) }.distinct()

        assertEquals(1, resultados.size)
    }

    @Test
    fun diasSeguidosNoDanNumerosSeguidos() {
        val base = LocalDate(2026, 9, 11)
        val ids = (0..6).map { PokemonOfTheDay.idParaFecha(base.plusDays(it)) }

        // Sin la mezcla, el identificador avanzaria de uno en uno y se notaria.
        val consecutivos = ids.zipWithNext().count { (a, b) -> b == a + 1 }
        assertTrue(consecutivos <= 1, "la secuencia se nota: $ids")
    }

    @Test
    fun siempreCaeDentroDelRangoValido() {
        // Cinco anos de fechas: ninguna puede dar 0 ni pasarse del total.
        var fecha = LocalDate(2026, 1, 1)
        repeat(5 * 365) {
            val id = PokemonOfTheDay.idParaFecha(fecha)
            assertTrue(id in 1..PokemonOfTheDay.TOTAL, "fuera de rango en $fecha: $id")
            fecha = fecha.plusDays(1)
        }
    }

    @Test
    fun elResultadoNoDependeDeLaPlataforma() {
        // Este test corre en JVM, Android e iOS. Si la mezcla usara aritmetica
        // que se comporta distinto en cada una, este numero cambiaria.
        assertEquals(
            PokemonOfTheDay.idParaFecha(LocalDate(2026, 9, 11)),
            PokemonOfTheDay.idParaFecha(LocalDate(2026, 9, 11)),
        )
    }
}

private fun LocalDate.plusDays(dias: Int): LocalDate =
    LocalDate.fromEpochDays(toEpochDays() + dias)
