package ir.codecrafter.plasticproducts.ui.invoice

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.Invoice
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import ir.codecrafter.plasticproducts.data.repository.OrderRepository
import ir.codecrafter.plasticproducts.ui.navigation.InvoiceRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject

data class InvoiceUiState(
    val invoice: Invoice? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isDownloading: Boolean = false,
)

sealed class InvoiceEvent {
    data class ActionFailed(val message: String) : InvoiceEvent()
}

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: Int = checkNotNull(savedStateHandle[InvoiceRoutes.ORDER_ID_ARG])

    private val _uiState = MutableStateFlow(InvoiceUiState())
    val uiState: StateFlow<InvoiceUiState> = _uiState.asStateFlow()

    private val _events = Channel<InvoiceEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadInvoice()
    }

    fun loadInvoice() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = orderRepository.getInvoice(orderId)) {
                is AuthResult.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    invoice = result.data,
                )
                else -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = describeFailure(result),
                )
            }
        }
    }

    fun downloadPdf() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true)
            when (val result = orderRepository.downloadInvoicePdf(orderId)) {
                is AuthResult.Success -> {
                    val file = withContext(Dispatchers.IO) { writePdfToCacheFile(result.data) }
                    _uiState.value = _uiState.value.copy(isDownloading = false)
                    if (file == null) {
                        _events.send(InvoiceEvent.ActionFailed(context.getString(R.string.error_generic)))
                        return@launch
                    }
                    openPdf(file)
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isDownloading = false)
                    _events.send(InvoiceEvent.ActionFailed(describeFailure(result)))
                }
            }
        }
    }

    /** Runs on Dispatchers.IO (called via withContext from downloadPdf()). Writes into cacheDir, never external storage. */
    private fun writePdfToCacheFile(body: ResponseBody): File? {
        return try {
            val file = File(context.cacheDir, "invoice_$orderId.pdf")
            body.byteStream().use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            file
        } catch (e: IOException) {
            null
        }
    }

    private suspend fun openPdf(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            // Started from an application (not activity) Context, and the receiving
            // app needs read access to a content:// URI it doesn't already own.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            _events.send(InvoiceEvent.ActionFailed(context.getString(R.string.error_no_pdf_viewer)))
        }
    }

    private fun describeFailure(result: AuthResult<*>): String = when (result) {
        is AuthResult.RateLimited -> result.message ?: context.getString(R.string.error_rate_limited)
        is AuthResult.Error -> when (val message = result.message) {
            is ErrorMessage.StringMessage -> message.value
            is ErrorMessage.FieldErrors -> message.fields.values.flatten().firstOrNull()
                ?: context.getString(R.string.error_generic)
            null -> context.getString(R.string.error_generic)
        }
        AuthResult.NetworkError -> context.getString(R.string.error_network)
        is AuthResult.Success -> "" // never reached — callers only pass non-Success results here
    }
}
