package ir.codecrafter.plasticproducts.ui.admin

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ir.codecrafter.plasticproducts.R
import ir.codecrafter.plasticproducts.data.model.ProductWriteBody
import ir.codecrafter.plasticproducts.data.network.ErrorMessage
import ir.codecrafter.plasticproducts.data.repository.AdminProductRepository
import ir.codecrafter.plasticproducts.data.repository.AuthResult
import ir.codecrafter.plasticproducts.data.repository.ProductRepository
import ir.codecrafter.plasticproducts.ui.navigation.AdminProductRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject

data class AdminProductFormUiState(
    val isEditMode: Boolean = false,
    val title: String = "",
    val price: String = "",
    val weight: String = "",
    val color: String = "",
    val quality: String? = null,
    val description: String = "",
    val stock: String = "",
    val isActive: Boolean = true,
    /** Only populated/meaningful in edit mode — create mode has no product id to upload images against yet. */
    val imageUrls: List<String> = emptyList(),
    /** Only meaningful in edit mode, while the existing product is being fetched to prefill the form. */
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingImage: Boolean = false,
    val errorMessage: String? = null,
)

sealed class AdminProductFormEvent {
    data class ActionFailed(val message: String) : AdminProductFormEvent()
    data object Saved : AdminProductFormEvent()
}

/**
 * Shared by both AdminProductRoutes.CREATE (no productId nav arg — savedStateHandle
 * yields null) and AdminProductRoutes.EDIT_PATTERN (productId present). Edit mode
 * prefills via ProductRepository.getProductDetail() — GET products/{id}/, which
 * (unlike the list action) already returns inactive products to an admin caller
 * with no extra query param needed (see AdminProductApi.getProducts's KDoc for the
 * list-action contrast).
 */
@HiltViewModel
class AdminProductFormViewModel @Inject constructor(
    private val adminProductRepository: AdminProductRepository,
    private val productRepository: ProductRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val productId: Int? = savedStateHandle[AdminProductRoutes.PRODUCT_ID_ARG]

    private val _uiState = MutableStateFlow(AdminProductFormUiState(isEditMode = productId != null))
    val uiState: StateFlow<AdminProductFormUiState> = _uiState.asStateFlow()

    private val _events = Channel<AdminProductFormEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        productId?.let(::loadProduct)
    }

    private fun loadProduct(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = productRepository.getProductDetail(id)) {
                is AuthResult.Success -> {
                    val product = result.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = product.title,
                            price = product.price,
                            weight = product.weight,
                            color = product.color.orEmpty(),
                            quality = product.quality,
                            description = product.description,
                            stock = product.stock,
                            isActive = product.isActive,
                            imageUrls = product.imageUrls,
                        )
                    }
                }
                else -> _uiState.update { it.copy(isLoading = false, errorMessage = describeFailure(result)) }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value) }

    fun onPriceChange(value: String) = _uiState.update { it.copy(price = value) }

    fun onWeightChange(value: String) = _uiState.update { it.copy(weight = value) }

    fun onColorChange(value: String) = _uiState.update { it.copy(color = value) }

    fun onQualityChange(value: String) = _uiState.update { it.copy(quality = value) }

    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun onStockChange(value: String) = _uiState.update { it.copy(stock = value) }

    fun onIsActiveChange(value: Boolean) = _uiState.update { it.copy(isActive = value) }

    fun save() {
        val state = _uiState.value
        val quality = state.quality ?: return
        val body = ProductWriteBody(
            title = state.title.trim(),
            price = state.price.trim(),
            weight = state.weight.trim(),
            color = state.color.trim().ifBlank { null },
            quality = quality,
            description = state.description.trim(),
            stock = state.stock.trim(),
            isActive = if (state.isEditMode) state.isActive else true,
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val result = if (productId != null) {
                adminProductRepository.updateProduct(productId, body)
            } else {
                adminProductRepository.createProduct(body)
            }
            when (result) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(AdminProductFormEvent.Saved)
                }
                else -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(AdminProductFormEvent.ActionFailed(describeFailure(result)))
                }
            }
        }
    }

    /**
     * Only callable in edit mode (productId != null) — the UI only shows the
     * "افزودن تصویر" button once a real product id exists to upload against.
     */
    fun uploadImage(uri: Uri) {
        val id = productId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingImage = true) }
            val part = withContext(Dispatchers.IO) { buildImagePart(uri) }
            if (part == null) {
                _uiState.update { it.copy(isUploadingImage = false) }
                _events.send(AdminProductFormEvent.ActionFailed(context.getString(R.string.error_generic)))
                return@launch
            }
            when (val result = adminProductRepository.uploadImage(id, part)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(isUploadingImage = false, imageUrls = result.data.imageUrls)
                }
                else -> {
                    _uiState.update { it.copy(isUploadingImage = false) }
                    _events.send(AdminProductFormEvent.ActionFailed(describeFailure(result)))
                }
            }
        }
    }

    /**
     * Runs on Dispatchers.IO (called via withContext from uploadImage()). The
     * filename must carry a real extension — products/views.py upload_image()
     * validates request.FILES['image'].name's extension against an allow-list
     * (jpg/jpeg/png/gif/webp), and a content:// URI's own path never reliably
     * carries one, so it's derived from the MIME type instead.
     */
    private fun buildImagePart(uri: Uri): MultipartBody.Part? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
            val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
            MultipartBody.Part.createFormData("image", "upload.$extension", requestBody)
        } catch (e: IOException) {
            null
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
