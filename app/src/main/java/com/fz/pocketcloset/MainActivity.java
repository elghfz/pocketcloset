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
                Fragment selectedFragment = null;

                if (item.getItemId() == R.id.nav_clothes) {
                    selectedFragment = getSupportFragmentManager().findFragmentByTag("CLOTHES_FRAGMENT");
                    if (selectedFragment == null) {
                        selectedFragment = new ClothesFragment();
                        getSupportFragmentManager().beginTransaction()
                                .add(R.id.fragment_container, selectedFragment, "CLOTHES_FRAGMENT")
                                .commit();
                    }
                } else if (item.getItemId() == R.id.nav_collections) {
                    selectedFragment = getSupportFragmentManager().findFragmentByTag("COLLECTIONS_FRAGMENT");
                    if (selectedFragment == null) {
                        selectedFragment = new CollectionsFragment();
                        getSupportFragmentManager().beginTransaction()
                                .add(R.id.fragment_container, selectedFragment, "COLLECTIONS_FRAGMENT")
                                .commit();
                    }
                }

                showFragment(selectedFragment);
                return true;
            });




        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
        }
    }

    private void showFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        for (Fragment f : fragmentManager.getFragments()) {
            if (f.equals(fragment)) {
                fragmentManager.beginTransaction().show(f).commit();
            } else {
                fragmentManager.beginTransaction().hide(f).commit();
            }
        }
    }
    private boolean loadFragment(Fragment fragment, String tag) {
        try {
            Fragment existingFragment = getSupportFragmentManager().findFragmentByTag(tag);
            if (existingFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, existingFragment)
                        .commit();
            } else {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment, tag)
                        .addToBackStack(null) // Ensure proper navigation
                        .commit();
            }
            return true;
        } catch (Exception e) {
            Log.e("MainActivity", "Error loading fragment: " + e.getMessage(), e);
            return false;
        }
    }


    public void openCollection(Collection collection) {
        Fragment collectionDetailFragment = new CollectionDetailFragment();

        Bundle bundle = new Bundle();
        bundle.putInt("collection_id", collection.getId());
        collectionDetailFragment.setArguments(bundle);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, collectionDetailFragment)
                .addToBackStack(null) // Ensure proper navigation
                .commit();
    }


}
