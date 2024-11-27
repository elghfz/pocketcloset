package com.fz.pocketcloset;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private Fragment clothesFragment;
    private Fragment collectionsFragment;
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);

            BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);

            // Initialize FragmentManager and fragments
            fragmentManager = getSupportFragmentManager();
            clothesFragment = new ClothesFragment();
            collectionsFragment = new CollectionsFragment();

            // Add fragments once and show the ClothesFragment by default
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.add(R.id.fragment_container, clothesFragment, "ClothesFragment");
            transaction.add(R.id.fragment_container, collectionsFragment, "CollectionsFragment");
            transaction.hide(collectionsFragment); // Start with CollectionsFragment hidden
            transaction.commit();

            // Handle navigation item clicks
            bottomNavigation.setOnItemSelectedListener(item -> {
                if (item.getItemId() == R.id.nav_clothes) {
                    showFragment(clothesFragment, collectionsFragment);
                } else if (item.getItemId() == R.id.nav_collections) {
                    showFragment(collectionsFragment, clothesFragment);
                }
                return true;
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
        }
    }

    /**
     * Show one fragment and hide the other.
     *
     * @param fragmentToShow The fragment to show.
     * @param fragmentToHide The fragment to hide.
     */
    private void showFragment(Fragment fragmentToShow, Fragment fragmentToHide) {
        try {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.hide(fragmentToHide).show(fragmentToShow).commit();
        } catch (Exception e) {
            Log.e(TAG, "Error switching fragments: " + e.getMessage(), e);
        }
    }
}
