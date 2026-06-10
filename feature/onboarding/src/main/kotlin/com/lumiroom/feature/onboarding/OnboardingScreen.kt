package com.lumiroom.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lumiroom.core.ui.theme.LumiroomBackground
import com.lumiroom.core.ui.theme.LumiroomPrimary
import com.lumiroom.core.ui.theme.LumiroomSecondary
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val emoji: String,
)

private val pages = listOf(
    OnboardingPage(
        emoji    = "🏠",
        title    = "See It Before You Buy It",
        subtitle = "Place furniture in your real room using Augmented Reality. No guesswork, no returns.",
    ),
    OnboardingPage(
        emoji    = "🛋️",
        title    = "Thousands of Pieces",
        subtitle = "Browse a curated catalog of premium furniture. Filter by style, material, and price.",
    ),
    OnboardingPage(
        emoji    = "✨",
        title    = "AI-Powered Design",
        subtitle = "Get personalized design recommendations from Lumi, your AI interior design assistant.",
    ),
    OnboardingPage(
        emoji    = "📷",
        title    = "AR & Voice Access",
        subtitle = "Lumiroom needs Camera access for AR furniture placement, and Microphone access to talk with Lumi.",
    ),
)

/**
 * Onboarding carousel screen shown on first launch.
 * Uses [HorizontalPager] for swipeable pages with animated dot indicators.
 */
@Composable
fun OnboardingScreen(
    onNavigateToSignIn: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState { pages.size }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(LumiroomBackground, LumiroomBackground),
                )
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                OnboardingPageContent(page = pages[page])
            }

            // Dots + CTA
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Page indicators
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 24.dp else 8.dp)
                                .height(8.dp)
                                .background(
                                    color = if (isSelected) LumiroomPrimary else LumiroomPrimary.copy(alpha = 0.3f),
                                    shape = MaterialTheme.shapes.extraSmall,
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            viewModel.onOnboardingComplete()
                            onNavigateToSignIn()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = LumiroomPrimary),
                ) {
                    Text(
                        text = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(onClick = { viewModel.onOnboardingComplete(); onNavigateToSignIn() }) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = page.emoji, style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
