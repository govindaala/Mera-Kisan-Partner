<?xml version="1.0" encoding="utf-8"?>
<!-- app/src/main/res/layout/fragment_marketplace.xml -->
<androidx.coordinatorlayout.widget.CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F8FAF8">

    <com.google.android.material.appbar.AppBarLayout
        android:id="@+id/appBarLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="#FFFFFF"
        app:elevation="2dp">

        <!-- Search Bar -->
        <com.google.android.material.textfield.TextInputLayout
            android:id="@+id/tilSearch"
            style="@style/Widget.MaterialComponents.TextInputLayout.OutlinedBox"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginHorizontal="12dp"
            android:layout_marginTop="8dp"
            app:boxCornerRadiusBottomEnd="8dp"
            app:boxCornerRadiusBottomStart="8dp"
            app:boxCornerRadiusTopEnd="8dp"
            app:boxCornerRadiusTopStart="8dp"
            app:boxStrokeColor="#1B5E20"
            app:hintEnabled="false"
            app:startIconDrawable="@android:drawable/ic_menu_search">

            <com.google.android.material.textfield.TextInputEditText
                android:id="@+id/etSearch"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:hint="फसल, किस्म या किसान खोजें (उदा. देशी गेहूं, सोयाबीन)..."
                android:imeOptions="actionSearch"
                android:inputType="text"
                android:maxLines="1"
                android:textSize="14sp" />
        </com.google.android.material.textfield.TextInputLayout>

        <!-- Horizontal Filter Chips -->
        <HorizontalScrollView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:layout_marginBottom="8dp"
            android:paddingHorizontal="12dp"
            android:scrollbars="none">

            <com.google.android.material.chip.ChipGroup
                android:id="@+id/chipGroupFilters"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                app:singleLine="true"
                app:singleSelection="true">

                <com.google.android.material.chip.Chip
                    android:id="@+id/chipAll"
                    style="@style/Widget.MaterialComponents.Chip.Choice"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:checked="true"
                    android:text="सभी फसलें"
                    app:chipBackgroundColor="@color/selector_nav_colors"
                    app:chipStrokeColor="#1B5E20" />

                <com.google.android.material.chip.Chip
                    android:id="@+id/chipSmallQuantity"
                    style="@style/Widget.MaterialComponents.Chip.Choice"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="🌾 1–25 kg छोटी मात्रा"
                    app:chipStrokeColor="#1B5E20" />

                <com.google.android.material.chip.Chip
                    android:id="@+id/chipOrganic"
                    style="@style/Widget.MaterialComponents.Chip.Choice"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="🌿 जैविक प्रमाणित"
                    app:chipStrokeColor="#1B5E20" />

                <com.google.android.material.chip.Chip
                    android:id="@+id/chipGrains"
                    style="@style/Widget.MaterialComponents.Chip.Choice"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="अनाज व दालें"
                    app:chipStrokeColor="#1B5E20" />

                <com.google.android.material.chip.Chip
                    android:id="@+id/chipVegetables"
                    style="@style/Widget.MaterialComponents.Chip.Choice"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="ताज़ी सब्जियां"
                    app:chipStrokeColor="#1B5E20" />

                <com.google.android.material.chip.Chip
                    android:id="@+id/chipOil"
                    style="@style/Widget.MaterialComponents.Chip.Choice"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="कच्ची घानी तेल"
                    app:chipStrokeColor="#1B5E20" />

            </com.google.android.material.chip.ChipGroup>
        </HorizontalScrollView>

    </com.google.android.material.appbar.AppBarLayout>

    <!-- Swipe Refresh Layout & Produce List -->
    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:id="@+id/swipeRefresh"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <FrameLayout
            android:layout_width="match_parent"
            android:layout_height="match_parent">

            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rvProducts"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:clipToPadding="false"
                android:paddingTop="6dp"
                android:paddingBottom="80dp"
                tools:listitem="@layout/item_product_card" />

            <!-- Empty State View -->
            <LinearLayout
                android:id="@+id/llEmptyState"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center"
                android:gravity="center"
                android:orientation="vertical"
                android:padding="24dp"
                android:visibility="gone">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="🌾"
                    android:textSize="48sp" />

                <TextView
                    android:id="@+id/tvEmptyTitle"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:text="कोई फसल उपलब्ध नहीं है"
                    android:textColor="#212121"
                    android:textSize="16sp"
                    android:textStyle="bold" />

                <TextView
                    android:id="@+id/tvEmptySubtitle"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="4dp"
                    android:gravity="center"
                    android:text="फ़िल्टर बदलकर देखें या 'मुझे चाहिए' बोर्ड पर अपनी मांग दर्ज करें।"
                    android:textColor="#757575"
                    android:textSize="13sp" />
            </LinearLayout>

        </FrameLayout>

    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

</androidx.coordinatorlayout.widget.CoordinatorLayout>
