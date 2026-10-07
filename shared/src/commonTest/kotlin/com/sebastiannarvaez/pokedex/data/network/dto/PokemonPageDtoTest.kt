package com.sebastiannarvaez.pokedex.data.network.dto

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PokemonPageDtoTest {

    // Un lector de JSON que ignora los campos que la clase no declara: el mismo
    // ajuste que lleva el cliente.
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun leeElListadoYIgnoraLoQueSobra() {
        // Entre tres comillas (""") cabe un texto de varias lineas con sus propias
        // comillas, tal cual. El campo "sobra" no existe en PokemonPageDto.
        val texto = """
            { "count": 1351, "next": null, "sobra": true,
              "results": [ { "name": "bulbasaur", "url": "https://pokeapi.co/api/v2/pokemon/1/" } ] }
        """.trimIndent()

        // Convierte el texto en un PokemonPageDto.
        val pagina = json.decodeFromString<PokemonPageDto>(texto)

        assertEquals(1351, pagina.count)
        assertEquals("bulbasaur", pagina.results.single().name)
    }

    @Test
    fun elIdentificadorSaleDeLaUrl() {
        // La direccion termina en «/25/»: el 25 es el identificador.
        val pikachu = PokemonRefDto(name = "pikachu", url = "https://pokeapi.co/api/v2/pokemon/25/")

        assertEquals(25, pikachu.id)
    }
}
