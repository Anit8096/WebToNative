package com.kmp.webtonative

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.kmp.webtonative.navigation.NavGraph
import com.kmp.webtonative.ui.theme.WebToNativeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebToNativeTheme {
                Scaffold { paddingValues ->
                    NavGraph(Modifier.padding(paddingValues))
                }

            }
        }
    }
}
