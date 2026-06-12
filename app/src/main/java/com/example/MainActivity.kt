package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.SadiqiMainScreen
import com.example.ui.SadiqiViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Retrieve ViewModel with Application lifecycle
        val viewModel = ViewModelProvider(this)[SadiqiViewModel::class.java]
        
        setContent {
            MyApplicationTheme {
                SadiqiMainScreen(viewModel = viewModel)
            }
        }
    }
}
