package com.sebastiannarvaez.pokedex.data

import com.sebastiannarvaez.pokedex.core.AppDispatchers
import com.sebastiannarvaez.pokedex.data.network.PokeApi
import com.sebastiannarvaez.pokedex.data.network.dto.NamedRefDto
import com.sebastiannarvaez.pokedex.data.network.dto.PokemonDetailDto
import com.sebastiannarvaez.pokedex.data.network.dto.PokemonPageDto
import com.sebastiannarvaez.pokedex.data.network.dto.PokemonRefDto
import com.sebastiannarvaez.pokedex.data.network.dto.TypeSlotDto
import com.sebastiannarvaez.pokedex.domain.PokemonType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Un doble escrito a mano, no un mock.
 *
 * Los mocks por reflexion no funcionan en Kotlin/Native, asi que en KMP los
 * dobles se escriben. Este ademas cuenta las llamadas, que es lo que permite
 * comprobar el N+1 sin adivinarlo.
 */
private class FakePokeApi(
    private val nombres: List<String>,
    private val tipos: Map<Int, List<String>> = emptyMap(),
) : PokeApi {

    var llamadasAlDetalle = 0
        private set

    override suspend fun page(limit: Int, offset: Int) = PokemonPageDto(
        count = nombres.size,
        next = null,
        results = nombres.drop(offset).take(limit).mapIndexed { i, nombre ->
            PokemonRefDto(name = nombre, url = "https://pokeapi.co/api/v2/pokemon/${offset + i + 1}/")
        },
    )

    override suspend fun detail(id: Int): PokemonDetailDto {
        llamadasAlDetalle++
        return PokemonDetailDto(
            id = id,
            name = nombres[id - 1],
            types = (tipos[id] ?: listOf("normal")).mapIndexed { i, t ->
                TypeSlotDto(slot = i + 1, type = NamedRefDto(t))
            },
        )
    }
}

private class TestDispatchers(private val d: CoroutineDispatcher) : AppDispatchers {
    override val io = d
    override val default = d
    override val main = d
}

class PokemonRepositoryTest {

    private val nombres = listOf("bulbasaur", "ivysaur", "venusaur", "charmander")

    @Test
    fun laPaginaLlegaTraducidaADominio() = runTest {
        val api = FakePokeApi(nombres, tipos = mapOf(1 to listOf("grass", "poison")))
        val repo = PokemonRepository(api, TestDispatchers(StandardTestDispatcher(testScheduler)))

        val pagina = repo.page(limit = 2, offset = 0)

        assertEquals(2, pagina.size)
        assertEquals("bulbasaur", pagina[0].name)
        assertEquals(listOf(PokemonType.GRASS, PokemonType.POISON), pagina[0].types)
    }

    @Test
    fun laIlustracionSaleDelIdentificadorDeLaUrl() = runTest {
        val api = FakePokeApi(nombres)
        val repo = PokemonRepository(api, TestDispatchers(StandardTestDispatcher(testScheduler)))

        val pagina = repo.page(limit = 1, offset = 2)

        // offset 2 -> el tercero de la lista, con id 3
        assertEquals(3, pagina[0].id)
        assertEquals(
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/3.png",
            pagina[0].artworkUrl,
        )
    }

    @Test
    fun cadaPokemonDeLaPaginaCuestaUnaPeticionDeDetalle() = runTest {
        // El N+1 esta asumido, no escondido: este test lo deja escrito para que
        // nadie lo descubra por sorpresa mirando el trafico de red.
        val api = FakePokeApi(nombres)
        val repo = PokemonRepository(api, TestDispatchers(StandardTestDispatcher(testScheduler)))

        repo.page(limit = 4, offset = 0)

        assertEquals(4, api.llamadasAlDetalle)
    }

    @Test
    fun unTipoQueNoConocemosNoRompeLaTarjeta() = runTest {
        val api = FakePokeApi(nombres, tipos = mapOf(1 to listOf("grass", "cosmico")))
        val repo = PokemonRepository(api, TestDispatchers(StandardTestDispatcher(testScheduler)))

        val pagina = repo.page(limit = 1, offset = 0)

        assertEquals(listOf(PokemonType.GRASS), pagina[0].types)
    }
}
