package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SalesViewModel
import kotlinx.coroutines.flow.collectLatest

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    object Vendas : Screen("vendas", "Vendas", Icons.Filled.ShoppingCart, "tab_vendas")
    object Catalogo : Screen("catalogo", "Catálogo", Icons.Filled.Collections, "tab_catalogo")
    object Cadastros : Screen("cadastros", "Cadastros", Icons.Filled.AddCircle, "tab_cadastros")
    object Gerenciar : Screen("gerenciar", "Gerenciar", Icons.Filled.Assignment, "tab_gerenciar")
    object Historico : Screen("historico", "Histórico", Icons.Filled.Schedule, "tab_historico")
    object Relatorios : Screen("relatorios", "Relatórios", Icons.Filled.Article, "tab_relatorios")

    companion object {
        val items: List<Screen>
            get() = listOf(Vendas, Catalogo, Cadastros, Gerenciar, Historico, Relatorios)
    }
}

class MainActivity : ComponentActivity() {
    private val viewModel: SalesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var isSplashActive by remember { mutableStateOf(true) }

                Crossfade(
                    targetState = isSplashActive,
                    animationSpec = tween(durationMillis = 400),
                    label = "splash_crossfade"
                ) { showSplash ->
                    if (showSplash) {
                        IntroView(onEnterApp = { isSplashActive = false })
                    } else {
                        MainAppScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: SalesViewModel
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is SalesViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Vendas.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = IosBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            IosBottomTabBar(
                currentRoute = currentRoute,
                onSelectTab = { screen ->
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Vendas.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Vendas.route) {
                SaleRegistrationScreen(
                    viewModel = viewModel,
                    onNavigateToHistory = {
                        navController.navigate(Screen.Historico.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Catalogo.route) {
                CatalogScreen(
                    viewModel = viewModel,
                    onSelectProductForSale = { product ->
                        viewModel.addToCart(product)
                        navController.navigate(Screen.Vendas.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Cadastros.route) {
                ProductRegistrationScreen(viewModel = viewModel)
            }
            composable(Screen.Gerenciar.route) {
                ManageScreen(viewModel = viewModel)
            }
            composable(Screen.Historico.route) {
                SalesHistoryScreen(viewModel = viewModel)
            }
            composable(Screen.Relatorios.route) {
                ReportsScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun IosBottomTabBar(
    currentRoute: String,
    onSelectTab: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF121215),
        border = BorderStroke(0.5.dp, Color(0xFF27272A)),
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("main_navigation_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Screen.items.forEach { screen ->
                val isSelected = currentRoute == screen.route

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onSelectTab(screen) }
                        .padding(vertical = 4.dp)
                        .testTag(screen.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isSelected) {
                        // iOS Active Pill Badge
                        Surface(
                            color = MagentaContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AccentMagenta.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .width(40.dp)
                                .height(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = AccentMagenta,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(40.dp)
                                .height(26.dp)
                        ) {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = TextMuted,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = screen.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) AccentMagenta else TextMuted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
