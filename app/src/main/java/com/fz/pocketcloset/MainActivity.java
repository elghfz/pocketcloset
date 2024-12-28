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
    private Fragment collectionDetailFragment; // Tracks the CollectionDetailFragment
    private Fragment activeFragment; // Tracks the currently active fragment
    private boolean isInDetailView = false; // Tracks if user is in CollectionDetailFragment

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

        // Add and show ClothesFragment initially
        transaction.add(R.id.fragment_container, clothesFragment, "ClothesFragment");
        transaction.add(R.id.fragment_container, collectionsFragment, "CollectionsFragment").hide(collectionsFragment);
        activeFragment = clothesFragment; // Start with ClothesFragment as active
        transaction.commit();

        // Bottom navigation setup
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_clothes) {
                switchToFragment(clothesFragment);
            } else if (item.getItemId() == R.id.nav_collections) {
                if (isInDetailView && collectionDetailFragment != null) {
                    // If detail view is active, switch to it
                    switchToFragment(collectionDetailFragment);
                } else {
                    // Otherwise, show the main collections list
                    switchToFragment(collectionsFragment);
                }
            }
            return true;
        });

        // Set initial selected item to ClothesFragment
        bottomNavigation.setSelectedItemId(R.id.nav_clothes);
    }

    private void switchToFragment(Fragment targetFragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.hide(activeFragment).show(targetFragment).commit();
        activeFragment = targetFragment;
    }

    public void openCollectionDetail(int collectionId) {
        // Open CollectionDetailFragment
        if (collectionDetailFragment == null) {
            collectionDetailFragment = new CollectionDetailFragment();
        }

        // Pass the collection ID to the detail fragment
        Bundle args = new Bundle();
        args.putInt("collection_id", collectionId);
        collectionDetailFragment.setArguments(args);

        FragmentManager fragmentManager = getSupportFragmentManager();
        if (!collectionDetailFragment.isAdded()) {
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, collectionDetailFragment, "CollectionDetailFragment")
                    .hide(activeFragment)
                    .commit();
        } else {
            fragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(collectionDetailFragment)
                    .commit();
        }
        activeFragment = collectionDetailFragment;
        isInDetailView = true; // Mark that the detail view is active
    }

    public void closeCollectionDetail() {
        // Navigate back to the collections list
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .hide(collectionDetailFragment)
                .show(collectionsFragment)
                .commit();
        activeFragment = collectionsFragment;
        collectionDetailFragment = null; // Clear reference to detail fragment
        isInDetailView = false; // Mark that we're no longer in the detail view
    }
}
