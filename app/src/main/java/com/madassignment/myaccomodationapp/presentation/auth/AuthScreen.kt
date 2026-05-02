package com.madassignment.myaccomodationapp.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madassignment.myaccomodationapp.domain.model.UserRole
import com.madassignment.myaccomodationapp.presentation.root.RootViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthRoute(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user by rootViewModel.authUser.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user) {
        if (user != null) onAuthenticated()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            if (event is AuthEvent.SignedIn) onAuthenticated()
        }
    }

    LaunchedEffect(state) {
        if (state is AuthUiState.Error) {
            scope.launch { snackbar.showSnackbar((state as AuthUiState.Error).message) }
            viewModel.consumeError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        AuthScreen(
            modifier = Modifier.padding(padding),
            loading = state is AuthUiState.Loading,
            onSignIn = { e, p -> viewModel.signIn(e, p) },
            onRegister = { e, p, n, r -> viewModel.signUp(e, p, n, r) },
        )
    }
}

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    loading: Boolean,
    onSignIn: (String, String) -> Unit,
    onRegister: (String, String, String, UserRole) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.Student) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Campus Stay Gaborone", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Sign in") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Register") })
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
        )
        if (tab == 1) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text("Role", style = MaterialTheme.typography.labelLarge)
            TabRow(selectedTabIndex = if (role == UserRole.Student) 0 else 1) {
                Tab(
                    selected = role == UserRole.Student,
                    onClick = { role = UserRole.Student },
                    text = { Text("Student") },
                )
                Tab(
                    selected = role == UserRole.Provider,
                    onClick = { role = UserRole.Provider },
                    text = { Text("Provider") },
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        if (loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (tab == 0) onSignIn(email, password)
                    else onRegister(email, password, name, role)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (tab == 0) "Sign in" else "Create account")
            }
        }
    }
}
