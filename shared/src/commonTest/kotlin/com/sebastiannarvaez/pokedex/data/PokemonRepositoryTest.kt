package com.sebastiannarvaez.pokedex.data

import com.sebastiannarvaez.pokedex.dobles.FakePokeApi
import com.sebastiannarvaez.pokedex.dobles.TestDispatchers
import com.sebastiannarvaez.pokedex.domain.PokemonType
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PokemonRepositoryTest {

    @Test
    fun laPaginaLlegaTraducidaADominio() = runTest {
        val api = FakePokeApi(total = 10, tipos = mapOf(1 to listOf("grass", "poison")))
        val repo = PokemonRepository(api, TestDispatchers(StandardTestDispatcher(testScheduler)))

        val pagina = repo.page(limit = 2, offset = 0)

        assertEquals(2, pagina.size)
        assertEquals("pokemon-1", pagina[0].name)
        assertEquals(listOf(PokemonType.GRASS, PokemonType.POISON), pagina[0].types)
    }

    @Test
    fun laIlustracionSaleDelIdentificadorDeLaUrl() = runTest {
        val api = FakePokeApi(total = 10)
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
        val api = FakePokeApi(total = 10)
        val repo = PokemonRepository(api, TestDispatchers(StandardTestDispatcher(testScheduler)))

        repo.page(limit = 4, offset = 0)

        assertEquals(4, api.llamadasAlDetalle)
    }

    @Test
    fun unTipoQueNoConocemosNoRompeLaTarjeta() = runTest {
        val api = FakePokeApi(total = 10, tipos = mapOf(1 to listOf("grass", "cosmico")))
        val repo = PokemonRepository(api, TestDispatchers(StandardTestDispatcher(testScheduler)))

        val pagina = repo.page(limit = 1, offset = 0)

        assertEquals(listOf(PokemonType.GRASS), pagina[0].types)
    }
}
