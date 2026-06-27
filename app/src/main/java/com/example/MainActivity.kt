package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.lifecycle.ViewModelProvider
import com.example.ui.BodaAppContent
import com.example.ui.BodaViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val viewModel = ViewModelProvider(this)[BodaViewModel::class.java]
    viewModel.handleDeepLink(intent)
    setContent {
      val themeSetting = viewModel.appThemeSetting
      val darkTheme = when (themeSetting) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
      }
      MyApplicationTheme(darkTheme = darkTheme) {
        BodaAppContent(viewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    val viewModel = ViewModelProvider(this)[BodaViewModel::class.java]
    viewModel.handleDeepLink(intent)
  }
}
