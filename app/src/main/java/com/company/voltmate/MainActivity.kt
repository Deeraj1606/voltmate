package com.company.voltmate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.company.voltmate.data.model.UserProfile
import com.company.voltmate.di.AppContainer
import com.company.voltmate.ui.AppViewModelFactory
import com.company.voltmate.ui.auth.AuthViewModel
import com.company.voltmate.ui.auth.LoginScreen
import com.company.voltmate.ui.auth.SplashScreen
import com.company.voltmate.ui.driver.*
import com.company.voltmate.ui.onboarding.*
import com.company.voltmate.ui.owner.*
import com.company.voltmate.ui.theme.VoltMateTheme
import com.company.voltmate.ui.theme.WhiteCanvas

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        val appContainer = (application as VoltMateApp).container

        setContent {
            VoltMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = WhiteCanvas
                ) {
                    VoltMateAppNav(appContainer = appContainer)
                }
            }
        }
    }
}

@Composable
fun VoltMateAppNav(
    appContainer: AppContainer
) {
    val navController = rememberNavController()
    val basicFactory = AppViewModelFactory(appContainer)
    val authViewModel: AuthViewModel = viewModel(factory = basicFactory)
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

    // Observe user profile when logged in
    val userProfileFlow = remember(currentUser?.uid) {
        currentUser?.uid?.let { appContainer.authRepository.observeUserProfile(it) }
    }
    val userProfile by (userProfileFlow?.collectAsStateWithLifecycle(initialValue = null)
        ?: remember { mutableStateOf<UserProfile?>(null) })

    // Factories with userId
    val uid = currentUser?.uid ?: ""
    val userFactory = remember(uid) { AppViewModelFactory(appContainer, uid) }

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(
                onNavigateNext = { dest ->
                    navController.navigate(dest) {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                currentUser = currentUser,
                authRepository = appContainer.authRepository
            )
        }

        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    val currentUid = authViewModel.currentUser.value?.uid ?: ""
                    val isOnboarded = userProfile?.onboarded ?: appContainer.authRepository.isLocalOnboarded(currentUid)
                    if (isOnboarded) {
                        navController.navigate("main") {
                            popUpTo(0) { inclusive = true }
                        }
                    } else {
                        navController.navigate("role_pick") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                }
            )
        }

        composable("role_pick") {
            val onboardingViewModel: OnboardingViewModel = viewModel(factory = basicFactory)
            RolePickScreen(
                onSelectRole = { role ->
                    onboardingViewModel.setRole(role)
                    if (role == "DRIVER") {
                        navController.navigate("driver_onboarding")
                    } else {
                        navController.navigate("owner_onboarding")
                    }
                }
            )
        }

        composable("driver_onboarding") {
            val onboardingViewModel: OnboardingViewModel = viewModel(factory = basicFactory)
            DriverOnboardingScreen(
                viewModel = onboardingViewModel,
                onComplete = {
                    navController.navigate("main") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("owner_onboarding") {
            val onboardingViewModel: OnboardingViewModel = viewModel(factory = basicFactory)
            OwnerOnboardingScreen(
                viewModel = onboardingViewModel,
                onComplete = {
                    navController.navigate("main") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            val activeRole by authViewModel.activeRole.collectAsStateWithLifecycle()
            val role = activeRole ?: userProfile?.role ?: appContainer.authRepository.getLocalRole(uid)
            val driverVm: DriverViewModel = viewModel(factory = userFactory)

            if (role == "OWNER") {
                val ownerVm: OwnerViewModel = viewModel(factory = userFactory)
                OwnerDashboardScreen(
                    viewModel = ownerVm,
                    userProfile = userProfile,
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onSwitchToDriver = {
                        authViewModel.switchRole("DRIVER")
                    },
                    onNavigateToCharger = { charger ->
                        driverVm.selectCharger(charger)
                        navController.navigate("charger_detail/${charger.id}")
                    },
                    onNavigateToWallet = {
                        navController.navigate("wallet")
                    }
                )
            } else {
                DriverDashboardScreen(
                    viewModel = driverVm,
                    userProfile = userProfile,
                    onChargerClick = { charger ->
                        driverVm.selectCharger(charger)
                        navController.navigate("charger_detail/${charger.id}")
                    },
                    onBookingClick = { booking ->
                        navController.navigate("charging_session/${booking.chargerId}/${booking.id}")
                    },
                    onActiveSessionClick = { bookingId ->
                        // Find charger for the booking
                        val booking = driverVm.bookings.value.find { it.id == bookingId }
                        if (booking != null) {
                            navController.navigate("charging_session/${booking.chargerId}/${booking.id}")
                        }
                    },
                    onProfileClick = { },
                    onSwitchToOwner = {
                        authViewModel.switchRole("OWNER")
                    },
                    onNavigateToWallet = {
                        navController.navigate("wallet")
                    },
                    onSignOut = {
                        authViewModel.signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable("wallet") {
            val walletVm: com.company.voltmate.ui.wallet.WalletViewModel = viewModel(factory = userFactory)
            com.company.voltmate.ui.wallet.WalletScreen(
                viewModel = walletVm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "charger_detail/{chargerId}",
            arguments = listOf(navArgument("chargerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chargerId = backStackEntry.arguments?.getString("chargerId") ?: ""
            val driverVm: DriverViewModel = viewModel(factory = userFactory)
            ChargerDetailScreen(
                viewModel = driverVm,
                chargerId = chargerId,
                onBack = { navController.popBackStack() },
                onBookSlot = { charger ->
                    navController.navigate("booking/${charger.id}")
                }
            )
        }

        composable(
            route = "booking/{chargerId}",
            arguments = listOf(navArgument("chargerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chargerId = backStackEntry.arguments?.getString("chargerId") ?: ""
            val driverVm: DriverViewModel = viewModel(factory = userFactory)
            val charger by driverVm.selectedCharger.collectAsStateWithLifecycle()

            LaunchedEffect(chargerId) {
                if (charger == null || charger?.id != chargerId) {
                    driverVm.loadChargerById(chargerId)
                }
            }

            charger?.let { c ->
                BookingScreen(
                    charger = c,
                    viewModel = driverVm,
                    onBack = { navController.popBackStack() },
                    onBookingConfirmed = { bookingId ->
                        navController.navigate("charging_session/${c.id}/$bookingId") {
                            popUpTo("main")
                        }
                    }
                )
            }
        }

        composable(
            route = "charging_session/{chargerId}/{bookingId}",
            arguments = listOf(
                navArgument("chargerId") { type = NavType.StringType },
                navArgument("bookingId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chargerId = backStackEntry.arguments?.getString("chargerId") ?: ""
            val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
            val driverVm: DriverViewModel = viewModel(factory = userFactory)
            val charger by driverVm.selectedCharger.collectAsStateWithLifecycle()

            LaunchedEffect(chargerId) {
                if (charger == null || charger?.id != chargerId) {
                    driverVm.loadChargerById(chargerId)
                }
            }

            charger?.let { c ->
                ChargingSessionScreen(
                    viewModel = driverVm,
                    charger = c,
                    bookingId = bookingId,
                    userProfile = userProfile,
                    onBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate("main") {
                            popUpTo("main") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
