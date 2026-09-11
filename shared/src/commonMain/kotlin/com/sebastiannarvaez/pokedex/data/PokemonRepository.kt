package com.sebastiannarvaez.pokedex.data

import com.sebastiannarvaez.pokedex.core.AppDispatchers
import com.sebastiannarvaez.pokedex.core.idiomasPreferidos
import com.sebastiannarvaez.pokedex.data.network.PokeApi
import com.sebastiannarvaez.pokedex.data.network.dto.PokemonDetailDto
import com.sebastiannarvaez.pokedex.data.network.dto.SpeciesDto
import com.sebastiannarvaez.pokedex.domain.Pokemon
import com.sebastiannarvaez.pokedex.domain.PokemonRef
import com.sebastiannarvaez.pokedex.domain.PokemonDetail
import com.sebastiannarvaez.pokedex.domain.PokemonStat
import com.sebastiannarvaez.pokedex.domain.PokemonType
import com.sebastiannarvaez.pokedex.domain.StatKind
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Traduce lo que da PokeAPI a lo que la app necesita.
 *
 * El listado de PokeAPI no trae ni tipos ni imagen: solo nombre y URL. Para
 * pintar una tarjeta decente hacen falta los tipos, asi que por cada pagina se
 * piden tambien los detalles. Es un N+1, y se hace con los ojos abiertos: las
 * peticiones van **en paralelo**, la pagina es de veinte, y mas adelante habra
 * cache. Hacerlas en serie multiplicaria por veinte la espera.
 */
internal class PokemonRepository(
    private val api: PokeApi,
    private val dispatchers: AppDispatchers,
    /**
     * Los idiomas del usuario, como dependencia y no como llamada directa.
     *
     * Por el mismo motivo que los dispatchers y el reloj: **es entorno**. Con
     * `idiomasPreferidos()` llamado a pelo aqui dentro, un test que comprueba
     * el texto en espanol pasa en un portatil en espanol y falla en un runner
     * de CI en ingles, con un mensaje que no menciona el idioma por ninguna
     * parte. Paso exactamente eso.
     */
    private val idiomas: () -> List<String> = ::idiomasPreferidos,
) {

    private val candado = Mutex()
    private var indice: List<PokemonRef>? = null

    suspend fun page(limit: Int, offset: Int): List<Pokemon> = withContext(dispatchers.io) {
        val pagina = api.page(limit = limit, offset = offset)

        coroutineScope {
            pagina.results
                .mapNotNull { it.id }
                .map { id -> async { pokemon(id) } }
                .awaitAll()
        }
    }

    /**
     * La ficha completa: dos peticiones **en paralelo**.
     *
     * `/pokemon/{id}` trae tipos, medidas y estadisticas; la descripcion y el
     * genero viven en `/pokemon-species/{id}`. En serie, el detalle tardaria el
     * doble sin ninguna razon: ninguna de las dos depende de la otra.
     *
     * Si una falla, falla el detalle entero. Es una decision: una ficha a
     * medias, sin descripcion, se parece demasiado a una ficha correcta y el
     * usuario no sabria que le falta algo.
     */
    suspend fun detail(id: Int): PokemonDetail = withContext(dispatchers.io) {
        coroutineScope {
            val basico = async { api.detail(id) }
            val especie = async { api.species(id) }
            unirDetalle(basico.await(), especie.await())
        }
    }

    /**
     * El indice de nombres, pedido una sola vez y guardado en memoria.
     *
     * **PokeAPI no tiene busqueda.** No hay `?q=pika`: `/pokemon/{nombre}` solo
     * acierta con el nombre exacto. La unica forma es traerse la lista entera
     * de nombres —son unos mil trescientos, apenas 100 KB— y filtrar aqui.
     *
     * El `Mutex` evita que dos busquedas simultaneas pidan el indice dos veces
     * al arrancar. Sin el, la primera letra que escribe el usuario dispara dos
     * descargas identicas.
     */
    suspend fun searchIndex(): List<PokemonRef> = withContext(dispatchers.io) {
        indice ?: candado.withLock {
            indice ?: api.page(limit = INDICE_COMPLETO, offset = 0).results
                .mapNotNull { ref -> ref.id?.let { PokemonRef(id = it, name = ref.name) } }
                .also { indice = it }
        }
    }

    suspend fun pokemon(id: Int): Pokemon = withContext(dispatchers.io) {
        val detalle = api.detail(id)
        Pokemon(
            id = detalle.id,
            name = detalle.name,
            types = detalle.types
                .sortedBy { it.slot }
                .mapNotNull { PokemonType.deApi(it.type.name) },
        )
    }

    private companion object {
        /** PokeAPI declara 1351 hoy; se pide de sobra y se acabo. */
        const val INDICE_COMPLETO = 2000
    }

    private fun unirDetalle(basico: PokemonDetailDto, especie: SpeciesDto): PokemonDetail =
        PokemonDetail(
            id = basico.id,
            name = basico.name,
            types = basico.types.sortedBy { it.slot }.mapNotNull { PokemonType.deApi(it.type.name) },
            // PokeAPI mide en decimetros y hectogramos, que no los usa nadie.
            // La conversion vive aqui y no en la pantalla: si estuviera alla,
            // habria que escribirla dos veces y una de las dos se equivocaria.
            heightCm = basico.height * 10,
            weightG = basico.weight * 100,
            genus = especie.enElIdioma(idiomas()) { it.genera.map { g -> g.genus to g.language.name } },
            description = especie.enElIdioma(idiomas()) { it.flavorTexts.map { f -> f.text to f.language.name } }
                .limpiarSaltos(),
            stats = basico.stats.mapNotNull { s ->
                StatKind.deApi(s.stat.name)?.let { PokemonStat(it, s.baseStat) }
            },
            isLegendary = especie.isLegendary || especie.isMythical,
        )
}

/**
 * El texto en el idioma del usuario, con los de repuesto detras.
 *
 * PokeAPI devuelve el mismo texto en once idiomas dentro del mismo array y no
 * hay endpoint por idioma: se filtra aqui. Antes estaba fijado a espanol, asi
 * que quien tuviera el telefono en ingles veia la ficha en espanol igualmente.
 *
 * Se recorre la lista de preferencias en orden y **no** al reves: buscar por
 * cada texto disponible cual encaja daria el primero del array, que es el que
 * PokeAPI devuelva primero, no el que el usuario quiere.
 */
private fun SpeciesDto.enElIdioma(
    preferidos: List<String>,
    extraer: (SpeciesDto) -> List<Pair<String, String>>,
): String {
    val textos = extraer(this)
    preferidos.forEach { idioma ->
        textos.firstOrNull { it.second == idioma }?.let { return it.first }
    }
    return textos.firstOrNull()?.first.orEmpty()
}

/**
 * Los textos de PokeAPI vienen con saltos de linea y saltos de pagina metidos
 * donde cortaba el cartucho original. Pintados tal cual, la ficha sale con
 * frases partidas por la mitad.
 */
private fun String.limpiarSaltos(): String =
    replace('\n', ' ').replace('\u000c', ' ').replace(Regex("\\s+"), " ").trim()