package com.sebastiannarvaez.pokedex.data.network

import com.sebastiannarvaez.pokedex.data.network.dto.PokemonDetailDto
import com.sebastiannarvaez.pokedex.data.network.dto.PokemonPageDto
import com.sebastiannarvaez.pokedex.data.network.dto.SpeciesDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Lo que la app necesita de PokeAPI, en una interfaz.
 *
 * Es una interfaz para poder sustituirla por una falsa en los tests de las
 * capas de arriba, sin montar un servidor ni un motor de mentira.
 */
internal interface PokeApi {
    suspend fun page(limit: Int, offset: Int): PokemonPageDto
    suspend fun detail(id: Int): PokemonDetailDto
    suspend fun species(id: Int): SpeciesDto
}

internal class KtorPokeApi(private val client: HttpClient) : PokeApi {

    override suspend fun page(limit: Int, offset: Int): PokemonPageDto =
        client.get("pokemon") {
            parameter("limit", limit)
            parameter("offset", offset)
        }.body()

    override suspend fun detail(id: Int): PokemonDetailDto =
        client.get("pokemon/$id").body()

    override suspend fun species(id: Int): SpeciesDto =
        client.get("pokemon-species/$id").body()
}
