package com.mifinanza.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mifinanza.app.ui.AppNavigation
import com.mifinanza.app.ui.theme.MiFinanzaTheme
import com.mifinanza.app.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val vm: AppViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val state by vm.state.collectAsState()
            MiFinanzaTheme(darkMode = state.darkMode) {
                AppNavigation(vm = vm)
            }
        }
    }
}
