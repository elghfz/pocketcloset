package com.fz.pocketcloset;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private Fragment clothesFragment;
    private Fragment collectionsFragment;
    private Fragment activeFragment; // Tracks the currently active fragment
    private Fragment collectionDetailFragment; // Tracks the CollectionDetailFragment if opened
    private boolean isInDetailView = false; // Tracks if the user is in a CollectionDetailFragment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize fragments
        clothesFragment = new ClothesFragment();
        collectionsFragment = new CollectionsFragment();

        // Add fragments to the FragmentManager
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.fragment_container, clothesFragment, "ClothesFragment").hide(clothesFragment);
        transaction.add(R.id.fragment_container, collectionsFragment, "CollectionsFragment");
        activeFragment = collectionsFragment; // Start with CollectionsFragment
        transaction.commit();

        // Bottom navigation setup
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_clothes) {
                switchToFragment(clothesFragment);
            } else if (item.getItemId() == R.id.nav_collections) {
                if (isInDetailView && collectionDetailFragment != null) {
                    // Go back to the CollectionDetailFragment
                    switchToFragment(collectionDetailFragment);
                } else {
                    // Otherwise, go back to CollectionsFragment
                    switchToFragment(collectionsFragment);
                }
            }
            return true;
        });
    }

    private void switchToFragment(Fragment targetFragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.hide(activeFragment).show(targetFragment).commit();
        activeFragment = targetFragment;
    }


    public void saveAndReturnToCollections() {
        // Navigate back to CollectionsFragment from CollectionDetailFragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .hide(collectionDetailFragment)
                .show(collectionsFragment)
                .commit();
        activeFragment = collectionsFragment;
        collectionDetailFragment = null; // Clear detail fragment reference
        isInDetailView = false; // Reset detail view tracking
    }
}
