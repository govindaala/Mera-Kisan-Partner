package `in`.merakisan.app.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import `in`.merakisan.app.R
import `in`.merakisan.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupBottomNavigation(binding.bottomNavigation)
    }

    private fun setupBottomNavigation(bottomNav: BottomNavigationView) {
        // डिफ़ॉल्ट व्यवहार के स्थान पर कस्टम लिसनर ताकि बैक-स्टैक और सही डेस्टिनेशन सुनिश्चित रहे
        bottomNav.setOnItemSelectedListener { item ->
            val builder = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setEnterAnim(android.R.anim.fade_in)
                .setExitAnim(android.R.anim.fade_out)

            // पैरेंट टैब पर लौटने पर चाइल्ड स्क्रीन (Add Crop आदि) को बैक-स्टैक से हटाना
            builder.setPopUpTo(
                navController.graph.findStartDestination().id,
                inclusive = false,
                saveState = true
            )

            val options = builder.build()

            when (item.itemId) {
                R.id.navigation_home -> {
                    navController.navigate(R.id.navigation_home, null, options)
                    true
                }
                R.id.navigation_demand -> {
                    navController.navigate(R.id.navigation_demand, null, options)
                    true
                }
                R.id.navigation_add_product -> {
                    navController.navigate(R.id.navigation_add_product, null, options)
                    true
                }
                R.id.navigation_orders -> {
                    navController.navigate(R.id.navigation_orders, null, options)
                    true
                }
                R.id.navigation_profile -> {
                    // त्रुटि फिक्स: 5वां टैब सीधे प्रोफाइल फ्रैगमेंट पर जाएगा
                    navController.navigate(R.id.navigation_profile, null, options)
                    true
                }
                else -> false
            }
        }

        // स्क्रीन बदलने पर बॉटम नेव विजिबिलिटी नियंत्रित करना
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navigation_home,
                R.id.navigation_demand,
                R.id.navigation_add_product,
                R.id.navigation_orders,
                R.id.navigation_profile -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
                else -> {
                    // डिटेल या लॉगिन स्क्रीन पर बार छुपाएं
                    binding.bottomNavigation.visibility = View.GONE
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
