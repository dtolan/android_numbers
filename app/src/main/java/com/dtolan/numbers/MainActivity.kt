package com.dtolan.numbers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dtolan.numbers.ui.NumbersApp
import com.dtolan.numbers.ui.NumbersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NumbersTheme {
                NumbersApp()
            }
        }
    }
}
