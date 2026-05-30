package com.omaster.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omaster.app.service.AiService
import com.omaster.app.ui.OMasterApp
import com.omaster.app.ui.theme.OMasterTheme
import com.omaster.app.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var aiService: AiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            OMasterTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OMasterApp(
                        viewModel = viewModel,
                        aiService = aiService
                    )
                }
            }
        }
    }
}
