package ir.codecrafter.plasticproducts.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.AdminUser
import ir.codecrafter.plasticproducts.ui.common.ErrorWithRetry

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(Unit) { viewModel.loadUsers() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.btn_users), fontWeight = FontWeight.Bold) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues: PaddingValues ->
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
                        ErrorWithRetry(
                            message = state.errorMessage.orEmpty(),
                            onRetry = viewModel::loadUsers,
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
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (!user.isActive) it.background(MaterialTheme.colorScheme.surfaceVariant) else it },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.fullName.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (user.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                UserRoleBadge(user.role)
            }
            
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (user.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.phone}"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            if (!user.isActive) {
                Text(
                    text = stringResource(R.string.label_product_inactive_badge),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                                if (user.isActive) stringResource(R.string.btn_deactivate_product) else stringResource(R.string.btn_activate_product)
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
fun UserRoleBadge(role: String) {
    val (bgColor, textColor) = when (role) {
        "admin" -> Color(0xFFFFEBEE) to Color(0xFFC62828)
        "visitor" -> Color(0xFFE3F2FD) to Color(0xFF1565C0)
        "buyer" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        else -> Color.LightGray to Color.Black
    }
    
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = roleLabel(role),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun roleLabel(role: String): String = when (role) {
    "buyer" -> stringResource(R.string.role_label_buyer)
    "visitor" -> stringResource(R.string.role_label_visitor)
    "admin" -> stringResource(R.string.role_label_admin)
    else -> role
}
