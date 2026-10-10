// app/src/main/java/in/merakisan/app/ui/share/ShareBottomSheetFragment.kt
package in.merakisan.app.ui.share

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import in.merakisan.app.core.network.model.ProductDto
import in.merakisan.app.core.utils.PosterGenerator
import in.merakisan.app.databinding.BottomSheetShareBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ShareBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetShareBinding? = null
    private val binding get() = _binding!!

    private var product: ProductDto? = null
    private var generatedPosterFile: File? = null

    companion object {
        fun newInstance(product: ProductDto): ShareBottomSheetFragment {
            return ShareBottomSheetFragment().apply {
                this.product = product
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetShareBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prod = product ?: run {
            dismiss()
            return
        }

        binding.tvShareCropTitle.text = "${prod.name} • ${prod.district}"
        startPosterGeneration(prod)
        setupClickListeners(prod)
    }

    private fun startPosterGeneration(prod: ProductDto) {
        binding.pbPosterGenerating.visibility = View.VISIBLE

        lifecycleScope.launch {
            // यदि उत्पाद में फ़ोटो उपलब्ध हो तो Coil से बिटमैप फेच करना
            val productBitmap = if (prod.photos.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    try {
                        val request = ImageRequest.Builder(requireContext())
                            .data(prod.photos[0])
                            .allowHardware(false)
                            .build()
                        val result = requireContext().imageLoader.execute(request)
                        if (result is SuccessResult) {
                            (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            } else null

            // डिवाइस पर पोस्टर रेंडर करना
            val posterResult = PosterGenerator.generatePoster(requireContext(), prod, productBitmap)
            if (posterResult.isSuccess) {
                generatedPosterFile = posterResult.getOrNull()
                val file = generatedPosterFile
                if (file != null && file.exists()) {
                    val previewBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    binding.ivPosterPreview.setImageBitmap(previewBitmap)
                }
            } else {
                Toast.makeText(requireContext(), "पोस्टर बनाने में विफलता", Toast.LENGTH_SHORT).show()
            }
            binding.pbPosterGenerating.visibility = View.GONE
        }
    }

    private fun setupClickListeners(prod: ProductDto) {
        binding.btnShareWhatsApp.setOnClickListener {
            shareToWhatsApp(prod)
        }

        binding.btnDownloadPoster.setOnClickListener {
            if (generatedPosterFile != null) {
                Toast.makeText(requireContext(), "पोस्टर डिवाइस कैश में सुरक्षित हो गया!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "पोस्टर अभी तैयार हो रहा है...", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnShareMore.setOnClickListener {
            shareUniversal(prod)
        }
    }

    private fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
    }

    private fun shareToWhatsApp(prod: ProductDto) {
        val message = buildShareMessage(prod)
        val intent = Intent(Intent.ACTION_SEND).apply {
            setPackage("com.whatsapp")
            putExtra(Intent.EXTRA_TEXT, message)
            if (generatedPosterFile != null) {
                val uri = getFileUri(generatedPosterFile!!)
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "WhatsApp इंस्टॉल नहीं है। सामान्य शेयरिंग खोली जा रही है।", Toast.LENGTH_SHORT).show()
            shareUniversal(prod)
        }
    }

    private fun shareUniversal(prod: ProductDto) {
        val message = buildShareMessage(prod)
        val intent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, message)
            if (generatedPosterFile != null) {
                val uri = getFileUri(generatedPosterFile!!)
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                type = "text/plain"
            }
        }
        startActivity(Intent.createChooser(intent, "फसल शेयर करें"))
    }

    private fun buildShareMessage(prod: ProductDto): String {
        val priceRupees = prod.pricePaise / 100.0
        val link = "https://merakisan.in/product/${prod.productId}"

        val loc = if (!prod.village.isNullOrBlank()) "${prod.village}, ${prod.district}" else prod.district
        val verification = when (prod.verificationStatus) {
            "organic_certified" -> "🌿 Organic Certified"
            "verified_farmer" -> "✔ Mera Kisan Verified Farmer"
            else -> "🌾 ताज़ी देशी उपज"
        }

        return """
            🌾 MERA KISAN — किसान से सीधे
            
            फसल: ${prod.name} ${if (!prod.variety.isNullOrBlank()) "(${prod.variety})" else ""}
            दर: ₹$priceRupees / ${prod.unit}
            उपलब्ध मात्रा: ${prod.stockQuantity} ${prod.unit}
            किसान: ${prod.sellerName} (${loc})
            स्थिति: $verification
            
            सीधे खरीदने या विवरण देखने के लिए लिंक खोलें:
            $link
        """.trimIndent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
