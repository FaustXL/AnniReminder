package com.faust.annireminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.faust.annireminder.ui.AppViewModel
import com.faust.annireminder.ui.AnniTheme
import com.faust.annireminder.ui.C
import com.faust.annireminder.ui.EditScreen
import com.faust.annireminder.ui.HomeScreen
import com.faust.annireminder.ui.Screen
import com.faust.annireminder.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnniTheme {
                AppRoot()
            }
        }
    }

    @Composable
    private fun AppRoot(vm: AppViewModel = viewModel()) {
        val hasPermission = remember { androidx.compose.runtime.mutableStateOf(false) }
        hasPermission.value = hasCalendarPermission()

        val permLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            hasPermission.value = result.values.all { it }
            vm.refreshCalendars()
        }

        LaunchedEffect(Unit) {
            if (!hasPermission.value) {
                permLauncher.launch(
                    arrayOf(
                        Manifest.permission.READ_CALENDAR,
                        Manifest.permission.WRITE_CALENDAR
                    )
                )
            }
        }

        val snackbar = remember { SnackbarHostState() }
        val message = vm.message
        LaunchedEffect(message) {
            message?.let {
                snackbar.showSnackbar(it)
                vm.message = null
            }
        }

        Box(Modifier.fillMaxSize()) {
            when (val s = vm.screen) {
                is Screen.Home -> HomeScreen(
                    vm = vm,
                    hasPermission = hasPermission.value,
                    onRequestPermission = {
                        permLauncher.launch(
                            arrayOf(
                                Manifest.permission.READ_CALENDAR,
                                Manifest.permission.WRITE_CALENDAR
                            )
                        )
                    }
                )
                is Screen.Edit -> EditScreen(vm, s.personId)
                is Screen.Settings -> SettingsScreen(vm)
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
            ) { data ->
                Snackbar(
                    containerColor = C.Orange,
                    contentColor = C.Black,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) { Text(data.visuals.message, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
            }
        }
    }

    private fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }
}
