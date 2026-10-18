// app/src/main/java/in/merakisan/app/ui/auth/RoleSelectionFragment.kt
package `in`.merakisan.app.ui.auth

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import `in`.merakisan.app.core.state.RoleStateManager

class RoleSelectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F4F6F8"))
            setPadding(dp(24), dp(24), dp(24), dp(24))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        root.addView(TextView(ctx).apply {
            text = "🌾 MERA KISAN"
            setTextColor(Color.parseColor("#1B5E20"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        })

        root.addView(TextView(ctx).apply {
            text = "किसान से सीधे आपकी जरूरत तक\nअपनी भूमिका चुनें (बाद में प्रोफ़ाइल से बदल सकते हैं)"
            setTextColor(Color.parseColor("#616161"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(28))
        })

        root.addView(createRoleButton(ctx, "👨‍🌾 किसान - विक्रेता (फसलें व उत्पाद बेचें)", "#1B5E20") {
            selectRoleAndProceed(RoleStateManager.ROLE_FARMER)
        })

        root.addView(createRoleButton(ctx, "🛒 खरीदार (सीधे किसान से ताज़ा खरीदें)", "#0D47A1") {
            selectRoleAndProceed(RoleStateManager.ROLE_BUYER)
        })

        root.addView(createRoleButton(ctx, "⚡ दोनों (खरीदें और बेचें — डबल विकल्प)", "#E65100") {
            selectRoleAndProceed(RoleStateManager.ROLE_BOTH)
        })

        return root
    }

    private fun createRoleButton(ctx: android.content.Context, title: String, colorHex: String, onClick: () -> Unit): Button {
        return Button(ctx).apply {
            text = title
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(colorHex))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply { bottomMargin = dp(14) }
            setOnClickListener { onClick() }
        }
    }

    private fun selectRoleAndProceed(role: String) {
        val ctx = context ?: return
        RoleStateManager.setRole(ctx, role)
        Toast.makeText(ctx, "भूमिका सक्रिय: $role", Toast.LENGTH_SHORT).show()
        val homeId = ctx.resources.getIdentifier("homeFragment", "id", ctx.packageName)
        if (homeId != 0) {
            try {
                findNavController().navigate(homeId)
            } catch (_: Throwable) {}
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
