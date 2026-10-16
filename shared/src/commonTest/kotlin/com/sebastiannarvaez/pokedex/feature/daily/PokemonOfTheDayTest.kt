package com.sebastiannarvaez.pokedex.feature.daily

import kotlinx.datetime.DateTimeUnit   // la unidad «día» para sumar días a una fecha
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus           // fecha.plus(3, DateTimeUnit.DAY)
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PokemonOfTheDayTest {

    @Test
    fun elMismoDiaDaSiempreElMismoPokemon() {
        val fecha = LocalDate(2026, 9, 11)

        // Diez llamadas, un resultado. Con `Random` esto fallaría.
        val resultados = List(10) { PokemonOfTheDay.idParaFecha(fecha) }.distinct()

        assertEquals(1, resultados.size)
    }

    @Test
    fun diasSeguidosNoDanNumerosSeguidos() {
        val base = LocalDate(2026, 9, 11)
        val ids = (0..6).map { PokemonOfTheDay.idParaFecha(base.plus(it, DateTimeUnit.DAY)) }

        // Sin la mezcla, el identificador avanzaría de uno en uno.
        val consecutivos = ids.zipWithNext().count { (a, b) -> b == a + 1 }
        assertTrue(consecutivos <= 1, "la secuencia se nota: $ids")
    }

    @Test
    fun siempreCaeDentroDelRangoValido() {
        // Cinco años de fechas: ninguna puede dar 0 ni pasarse del total.
        var fecha = LocalDate(2026, 1, 1)
        repeat(5 * 365) {
            val id = PokemonOfTheDay.idParaFecha(fecha)
            assertTrue(id in 1..PokemonOfTheDay.TOTAL, "fuera de rango en $fecha: $id")
            fecha = fecha.plus(1, DateTimeUnit.DAY)
        }
    }

    @Test
    fun daElMismoNumeroEnTodasLasPlataformas() {
        // Este test corre en la JVM, en Android y en iOS. Si la mezcla usara una
        // aritmética que se comporta distinto en alguna, el número cambiaría.
        assertEquals(248, PokemonOfTheDay.idParaFecha(LocalDate(2026, 9, 11)))
        assertEquals(417, PokemonOfTheDay.idParaFecha(LocalDate(2026, 10, 1)))
    }
}
