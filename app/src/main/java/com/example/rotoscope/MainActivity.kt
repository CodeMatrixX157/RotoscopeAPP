package com.example.rotoscope

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.rotoscope.ui.navigation.RotoscopeNavGraph
import com.example.rotoscope.ui.theme.RotoscopeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RotoscopeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RotoscopeNavGraph()
                }
            }
        }
    }
}
