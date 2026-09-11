package com.sebastiannarvaez.pokedex.dobles

import com.sebastiannarvaez.pokedex.core.AppDispatchers
import com.sebastiannarvaez.pokedex.data.network.PokeApi
import com.sebastiannarvaez.pokedex.data.network.dto.NamedRefDto
import com.sebastiannarvaez.pokedex.data.network.dto.PokemonDetailDto
import com.sebastiannarvaez.pokedex.data.network.dto.PokemonPageDto
import com.sebastiannarvaez.pokedex.data.network.dto.PokemonRefDto
import com.sebastiannarvaez.pokedex.data.network.dto.TypeSlotDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.io.IOException

/**
 * Los dobles de prueba, en un solo sitio.
 *
 * Estaban repetidos en dos ficheros, cada uno con su copia `private`. El
 * compilador lo rechaza con `Redeclaration:` en cuanto los dos source sets se
 * compilan juntos para iOS, aunque sean privados. Y al margen del compilador:
 * dos dobles del mismo tipo se desincronizan.
 *
 * Son `internal` porque implementan `PokeApi`, que tambien lo es: una clase
 * publica no puede exponer un tipo interno en su firma, y el compilador lo
 * dice con «'public' function exposes its 'internal' return type».
 */
internal class FakePokeApi(
    private val total: Int,
    private val falla: Boolean = false,
    private val tipos: Map<Int, List<String>> = emptyMap(),
) : PokeApi {

    var llamadasAlDetalle = 0
        private set

    override suspend fun page(limit: Int, offset: Int): PokemonPageDto {
        // IOException y no error(): es lo que lanza de verdad un socket caido,
        // y es lo unico que el mapeo traduce a «sin conexion».
        if (falla) throw IOException("socket cerrado")
        val hasta = minOf(offset + limit, total)
        return PokemonPageDto(
            count = total,
            results = ((offset + 1)..hasta).map {
                PokemonRefDto(name = "pokemon-$it", url = "https://pokeapi.co/api/v2/pokemon/$it/")
            },
        )
    }

    override suspend fun detail(id: Int): PokemonDetailDto {
        llamadasAlDetalle++
        return PokemonDetailDto(
            id = id,
            name = "pokemon-$id",
            types = (tipos[id] ?: listOf("normal")).mapIndexed { i, t ->
                TypeSlotDto(slot = i + 1, type = NamedRefDto(t))
            },
        )
    }
}

/** Los tres dispatchers apuntando al de prueba, para que `runTest` mande. */
internal class TestDispatchers(private val d: CoroutineDispatcher) : AppDispatchers {
    override val io = d
    override val default = d
    override val main = d
}
