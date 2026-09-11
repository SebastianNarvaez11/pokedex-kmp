package com.sebastiannarvaez.pokedex.data

/**
 * Un sitio donde guardar cadenas pequenas y privadas.
 *
 * **Sin `suspend` a proposito.** El Llavero de iOS es sincrono, y en Android se
 * leen unos cientos de bytes de un fichero propio de la app. Poner `suspend`
 * aqui obligaria a cada implementacion a inventarse una corrutina, y en Swift
 * convertiria una funcion de dos lineas en una con manejador de finalizacion.
 * El salto de hilo se hace **una sola vez**, en el adaptador de abajo.
 *
 * Es publica e implementable desde fuera: en iOS la escribe **Swift**, porque
 * el Llavero es suyo, y se inyecta al arrancar Koin.
 */
interface SecureStorage {
    fun leer(clave: String): String?
    fun guardar(clave: String, valor: String)
    fun borrar(clave: String)
}

/** En memoria. Para tests y para el target de JVM, que no tiene donde guardar. */
class InMemorySecureStorage : SecureStorage {
    private val datos = mutableMapOf<String, String>()
    override fun leer(clave: String): String? = datos[clave]
    override fun guardar(clave: String, valor: String) { datos[clave] = valor }
    override fun borrar(clave: String) { datos.remove(clave) }
}
