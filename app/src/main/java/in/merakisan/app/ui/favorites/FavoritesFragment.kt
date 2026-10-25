// app/src/main/java/in/merakisan/app/ui/favorites/FavoritesFragment.kt
package `in`.merakisan.app.ui.favorites

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import `in`.merakisan.app.core.network.ApiClient
import `in`.merakisan.app.ui.main.MainActivity
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * MERA KISAN — FAVORITE PRODUCTS & FOLLOWED FARMERS SCREEN
 * Exact Repository Path: app/src/main/java/in/merakisan/app/ui/favorites/FavoritesFragment.kt
 *
 * Implements (Master Spec Sec 9 & merged.txt Sec 15, 16, 40):
 * - Separate tabs for ❤️ Favorite Products (`PRODUCT`) and 👨‍🌾 Followed Farmers (`FARMER`).
 * - Direct navigation to Product Detail or Farmer Public Profile.
 * - 1-tap toggle to remove from favorites/following via ApiClient.
 */
class FavoritesFragment : Fragment() {

    private var activeTabType: String = "PRODUCT" // "PRODUCT" or "FARMER"
    private val allFavorites = mutableListOf<JSONObject>()

    private lateinit var btnTabProducts: Button
    private lateinit var btnTabFarmers: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var listContainer: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        val scroll = ScrollView(ctx).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#F2F6F2"))
        }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(28))
        }

        val headerCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#D4E2D4", dp(14))
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }

        headerCard.addView(
            TextView(ctx).apply {
                text = "❤️ मेरे पसंदीदा उत्पाद व फॉलो किए गए किसान"
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )
        headerCard.addView(
            TextView(ctx).apply {
                text = "जिन किसानों को आप फॉलो करते हैं, उनकी नई फसल आते ही आपको सूचना मिलती है।"
                setTextColor(Color.parseColor("#4A5A4A"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(4), 0, dp(10))
            }
        )

        val tabRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        btnTabProducts = Button(ctx).apply {
            text = "❤️ पसंदीदा उत्पाद"
            isAllCaps = false
            setTypeface(typeface, Typeface.BOLD)
            minHeight = dp(42)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6)
            }
            setOnClickListener {
                activeTabType = "PRODUCT"
                updateTabStyles()
                renderFilteredList()
            }
        }

        btnTabFarmers = Button(ctx).apply {
            text = "👨‍🌾 फॉलो किए गए किसान"
            isAllCaps = false
            setTypeface(typeface, Typeface.BOLD)
            minHeight = dp(42)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(6)
            }
            setOnClickListener {
                activeTabType = "FARMER"
                updateTabStyles()
                renderFilteredList()
            }
        }

        tabRow.addView(btnTabProducts)
        tabRow.addView(btnTabFarmers)
        headerCard.addView(tabRow)
        root.addView(headerCard)

        progressBar = ProgressBar(ctx).apply {
            visibility = View.GONE
            setPadding(0, dp(12), 0, dp(12))
        }
        root.addView(progressBar)

        listContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, 0)
        }
        root.addView(listContainer)

        scroll.addView(root)
        return scroll
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateTabStyles()
        fetchFavoritesFromServer()
    }

    private fun updateTabStyles() {
        if (activeTabType == "PRODUCT") {
            btnTabProducts.background = roundedBg("#1B5E20", "#1B5E20", dp(10))
            btnTabProducts.setTextColor(Color.WHITE)
            btnTabFarmers.background = roundedBg("#E8F5E9", "#81C784", dp(10))
            btnTabFarmers.setTextColor(Color.parseColor("#1B5E20"))
        } else {
            btnTabFarmers.background = roundedBg("#1B5E20", "#1B5E20", dp(10))
            btnTabFarmers.setTextColor(Color.WHITE)
            btnTabProducts.background = roundedBg("#E8F5E9", "#81C784", dp(10))
            btnTabProducts.setTextColor(Color.parseColor("#1B5E20"))
        }
    }

    private fun fetchFavoritesFromServer() {
        val ctx = context ?: return
        progressBar.visibility = View.VISIBLE
        listContainer.removeAllViews()

        viewLifecycleOwner.lifecycleScope.launch {
            val res = ApiClient.request(ctx, "/modules/favorites", method = "GET")
            if (!isAdded) return@launch
            progressBar.visibility = View.GONE

            allFavorites.clear()
            val arr = res.dataArray ?: JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                allFavorites.add(obj)
            }

            renderFilteredList()
        }
    }

    private fun renderFilteredList() {
        val ctx = context ?: return
        listContainer.removeAllViews()

        val filtered = allFavorites.filter {
            it.optString("target_type", "PRODUCT").uppercase() == activeTabType
        }

        if (filtered.isEmpty()) {
            val emptyMsg = if (activeTabType == "PRODUCT") {
                "आपने अभी कोई पसंदीदा उत्पाद सेव नहीं किया है।"
            } else {
                "आपने अभी किसी किसान को फॉलो नहीं किया है।"
            }
            listContainer.addView(
                TextView(ctx).apply {
                    text = emptyMsg
                    setTextColor(Color.parseColor("#555555"))
                    gravity = Gravity.CENTER
                    setPadding(dp(12), dp(24), dp(12), dp(24))
                }
            )
            return
        }

        for (item in filtered) {
            listContainer.addView(buildFavoriteCard(item))
        }
    }

    private fun buildFavoriteCard(item: JSONObject): View {
        val ctx = requireContext()
        val targetId = item.optString("target_id", "")
        val targetType = item.optString("target_type", "PRODUCT").uppercase()
        val title = item.optString("title", if (targetType == "FARMER") "किसान" else "कृषि उत्पाद")
        val subtitle = item.optString("subtitle", "")

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg("#FFFFFF", "#C8E6C9", dp(12))
            setPadding(dp(14), dp(12), dp(14), dp(12))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(10)
            layoutParams = lp
        }

        card.addView(
            TextView(ctx).apply {
                text = if (targetType == "FARMER") "👨‍🌾 किसान: $title" else "🌾 $title"
                setTextColor(Color.parseColor("#124116"))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, Typeface.BOLD)
            }
        )

        if (subtitle.isNotBlank() && subtitle != "null") {
            card.addView(
                TextView(ctx).apply {
                    text = subtitle
                    setTextColor(Color.parseColor("#BF360C"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTypeface(typeface, Typeface.BOLD)
                    setPadding(0, dp(3), 0, dp(8))
                }
            )
        }

        val btnRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(6), 0, 0)
        }

        val btnOpen = Button(ctx).apply {
            text = if (targetType == "FARMER") "👤 किसान प्रोफ़ाइल व फसलें देखें" else "🛒 उत्पाद खोलें और खरीदें"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = roundedBg("#1B5E20", "#1B5E20", dp(8))
            minHeight = dp(40)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(6)
            }
            setOnClickListener {
                if (targetType == "FARMER") {
                    (activity as? MainActivity)?.openFarmerPublicProfileScreen(targetId)
                } else {
                    (activity as? MainActivity)?.openProductDetailScreen(targetId)
                }
            }
        }

        val btnRemove = Button(ctx).apply {
            text = "🗑️ हटाएँ"
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            background = roundedBg("#B71C1C", "#B71C1C", dp(8))
            minHeight = dp(40)
            setOnClickListener {
                removeFavoriteItem(targetId, targetType, title)
            }
        }

        btnRow.addView(btnOpen)
        btnRow.addView(btnRemove)
        card.addView(btnRow)
        return card
    }

    private fun removeFavoriteItem(targetId: String, targetType: String, title: String) {
        val ctx = context ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val body = JSONObject().apply {
                put("target_id", targetId)
                put("target_type", targetType)
            }
            val res = ApiClient.request(ctx, "/modules/favorites", method = "POST", bodyJson = body)
            if (res.isSuccess) {
                Toast.makeText(ctx, "'$title' सूची से हटा दिया गया।", Toast.LENGTH_SHORT).show()
                fetchFavoritesFromServer()
            }
        }
    }

    private fun roundedBg(fillHex: String, strokeHex: String, radiusPx: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx.toFloat()
            setColor(Color.parseColor(fillHex))
            setStroke(dp(1), Color.parseColor(strokeHex))
        }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
