package com.company.voltmate.ui.auth

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.voltmate.data.repository.AuthRepository
import com.company.voltmate.ui.theme.InkPrimary
import com.company.voltmate.ui.theme.InkSecondary
import com.company.voltmate.ui.theme.VoltMateLogo
import com.company.voltmate.ui.theme.WhiteCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun SplashScreen(
    onNavigateNext: (destination: String) -> Unit,
    currentUser: com.google.firebase.auth.FirebaseUser?,
    authRepository: AuthRepository,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
        delay(400)

        if (currentUser == null) {
            onNavigateNext("login")
        } else {
            val uid = currentUser.uid
            val profile = withTimeoutOrNull(4000) {
                authRepository.resolveUserProfile(uid)
            }
            val isOnboarded = profile?.onboarded ?: authRepository.isLocalOnboarded(uid)
            if (isOnboarded) {
                onNavigateNext("main")
            } else {
                onNavigateNext("role_pick")
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WhiteCanvas),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale.value)
        ) {
            VoltMateLogo(size = 96.dp)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "VOLTMATE",
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                letterSpacing = 2.sp,
                color = InkPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Community EV Charging for Tata Owners",
                fontSize = 13.sp,
                color = InkSecondary,
                modifier = Modifier.alpha(alpha.value)
            )
        }
    }
}
