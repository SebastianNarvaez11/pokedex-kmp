package com.sebastiannarvaez.pokedex.android.navigation

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.test.junit4.createComposeRule
import com.sebastiannarvaez.pokedex.android.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class InsigniaFavoritosTest {

    @get:Rule
    val compose = createComposeRule()

    // Lo que TalkBack dice de la insignia: con uno, en singular.
    @Test
    fun `con un favorito se dice en singular`() {
        var uno = ""
        var tres = ""
        compose.setContent {
            uno = pluralStringResource(R.plurals.insignia_favoritos, 1, "1")
            tres = pluralStringResource(R.plurals.insignia_favoritos, 3, "3")
        }

        assertEquals("1 favorito", uno)
        assertEquals("3 favoritos", tres)
    }
}
