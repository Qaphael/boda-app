package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.BodaViewModel
import com.example.ui.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @OptIn(ExperimentalCoroutinesApi::class)
  private val testDispatcher = UnconfinedTestDispatcher()

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Boda Gulu", appName)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `viewmodel referral profile registration flow`() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = BodaViewModel(app)
    
    // Set up signup inputs
    viewModel.signupName = "Amone Richard"
    viewModel.phoneInput = "+256 789 555666"
    viewModel.referralCodeInput = "GULU-BODA-256"
    viewModel.locationPermissionGranted = true
    viewModel.notificationPermissionGranted = true
    
    // Complete signup
    viewModel.completeProfileSetup()
    
    // Verify navigating to Home screen
    assertEquals(Screen.Home, viewModel.currentScreen)
    
    // Expose referral code simulation triggers
    viewModel.simulateNewReferralSignUp()
    val refs = viewModel.referrals.value
    assertNotNull(refs)
  }
}
