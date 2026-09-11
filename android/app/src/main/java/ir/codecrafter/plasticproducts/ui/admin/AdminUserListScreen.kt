package ir.codecrafter.plasticproducts.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.AdminUser

@Composable
fun AdminUserListScreen(
    onViewVisitorPerformance: (Int) -> Unit,
    viewModel: AdminUserListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCreateVisitorDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AdminUserListEvent.ActionFailed -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Same refresh-on-return pattern as AdminProductListScreen — keeps the list
    // from going stale after creating a visitor or navigating away and back.
    LaunchedEffect(Unit) { viewModel.loadUsers() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues: PaddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Button(
                onClick = { showCreateVisitorDialog = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.btn_add_new_visitor))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.roleFilter == null,
                    onClick = { viewModel.onRoleFilterChange(null) },
                    label = { Text(stringResource(R.string.filter_quality_all)) },
                )
                FilterChip(
                    selected = state.roleFilter == "buyer",
                    onClick = { viewModel.onRoleFilterChange("buyer") },
                    label = { Text(stringResource(R.string.role_label_buyer)) },
                )
                FilterChip(
                    selected = state.roleFilter == "visitor",
                    onClick = { viewModel.onRoleFilterChange("visitor") },
                    label = { Text(stringResource(R.string.role_label_visitor)) },
                )
                FilterChip(
                    selected = state.roleFilter == "admin",
                    onClick = { viewModel.onRoleFilterChange("admin") },
                    label = { Text(stringResource(R.string.role_label_admin)) },
                )
            }

            val filteredUsers = state.users.filter { state.roleFilter == null || it.role == state.roleFilter }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
            ) {
                when {
                    state.isLoading && state.users.isEmpty() ->
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                    state.errorMessage != null ->
                        Text(
                            text = state.errorMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                        )

                    filteredUsers.isEmpty() ->
                        Text(
                            text = stringResource(R.string.empty_users_list),
                            modifier = Modifier.align(Alignment.Center),
                        )

                    else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredUsers, key = { "user_${it.id}" }) { user ->
                            AdminUserRow(
                                user = user,
                                isCurrentUser = user.id == viewModel.currentUserId,
                                isActionInProgress = state.actionInProgressId == user.id,
                                onToggleActive = { viewModel.toggleUserActive(user.id) },
                                onViewPerformance = { onViewVisitorPerformance(user.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateVisitorDialog) {
        CreateVisitorDialog(
            onDismiss = { showCreateVisitorDialog = false },
            onCreated = {
                showCreateVisitorDialog = false
                viewModel.loadUsers()
            },
        )
    }
}

@Composable
private fun AdminUserRow(
    user: AdminUser,
    isCurrentUser: Boolean,
    isActionInProgress: Boolean,
    onToggleActive: () -> Unit,
    onViewPerformance: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (!user.isActive) it.background(MaterialTheme.colorScheme.surfaceVariant) else it },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = user.fullName.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = if (user.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = user.phone, style = MaterialTheme.typography.bodyMedium)
            Text(text = roleLabel(user.role), style = MaterialTheme.typography.bodyMedium)
            if (!user.isActive) {
                Text(
                    text = stringResource(R.string.label_product_inactive_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // The signed-in admin can't toggle their own account — the server
                // rejects it (see AdminUserListViewModel's KDoc) — so this button is
                // hidden entirely for that row rather than shown disabled with no
                // explanation.
                if (!isCurrentUser) {
                    Button(
                        onClick = onToggleActive,
                        enabled = !isActionInProgress,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (isActionInProgress) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(
                                if (user.isActive) {
                                    stringResource(R.string.btn_deactivate_product)
                                } else {
                                    stringResource(R.string.btn_activate_product)
                                }
                            )
                        }
                    }
                }
                if (user.role == "visitor") {
                    TextButton(onClick = onViewPerformance) {
                        Text(stringResource(R.string.btn_view_visitor_performance))
                    }
                }
            }
        }
    }
}

@Composable
private fun roleLabel(role: String): String = when (role) {
    "buyer" -> stringResource(R.string.role_label_buyer)
    "visitor" -> stringResource(R.string.role_label_visitor)
    "admin" -> stringResource(R.string.role_label_admin)
    else -> role
}
