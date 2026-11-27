package com.company.voltmate.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EvStation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.company.voltmate.ui.theme.BorderHairline
import com.company.voltmate.ui.theme.DeepGreen
import com.company.voltmate.ui.theme.InkMuted
import com.company.voltmate.ui.theme.InkPrimary
import com.company.voltmate.ui.theme.InkSecondary
import com.company.voltmate.ui.theme.PrimaryGreen
import com.company.voltmate.ui.theme.SurfaceBg
import com.company.voltmate.ui.theme.TintGreen
import com.company.voltmate.ui.theme.VoltMateLogo
import com.company.voltmate.ui.theme.WhiteCanvas

@Composable
fun RolePickScreen(
    onSelectRole: (role: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRole by remember { mutableStateOf("DRIVER") }

    Scaffold(
        containerColor = WhiteCanvas,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            VoltMateLogo(size = 54.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "HOW WILL YOU USE VOLTMATE?",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = PrimaryGreen
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Choose your journey",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                color = InkPrimary
            )

            Text(
                text = "Select your primary role. You can switch or add chargers anytime in your profile.",
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Option 1: Driver
            RoleCard(
                title = "I Need to Charge",
                subtitle = "Tata EV Driver",
                description = "Find idle community chargers nearby, book dedicated slots, tap NFC to charge and pay seamlessly.",
                icon = Icons.Outlined.DirectionsCar,
                isSelected = selectedRole == "DRIVER",
                onClick = { selectedRole = "DRIVER" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Option 2: Host
            RoleCard(
                title = "I Own a Charger",
                subtitle = "Charger Host / Owner",
                description = "Monetize your idle home or shop charger. Set custom prices, open hours, and earn weekly payouts.",
                icon = Icons.Outlined.EvStation,
                isSelected = selectedRole == "OWNER",
                onClick = { selectedRole = "OWNER" }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onSelectRole(selectedRole) },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = WhiteCanvas
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = if (selectedRole == "DRIVER") "Continue as EV Driver" else "Continue as Host",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = WhiteCanvas
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    subtitle: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PrimaryGreen else BorderHairline,
        label = "roleBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) TintGreen.copy(alpha = 0.5f) else SurfaceBg,
        label = "roleBg"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (isSelected) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (isSelected) PrimaryGreen else WhiteCanvas, CircleShape)
                        .border(1.dp, if (isSelected) PrimaryGreen else BorderHairline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) WhiteCanvas else InkPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subtitle.uppercase(),
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = if (isSelected) PrimaryGreen else InkMuted
                    )
                    Text(
                        text = title,
                        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                        color = InkPrimary
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = "Selected",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = InkSecondary,
                lineHeight = 20.sp
            )
        }
    }
}
