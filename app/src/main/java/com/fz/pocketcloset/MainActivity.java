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
        transaction.add(R.id.fragment_container, clothesFragment, "ClothesFragment");
        transaction.add(R.id.fragment_container, collectionsFragment, "CollectionsFragment").hide(collectionsFragment);
        activeFragment = clothesFragment; // Start with ClothesFragment as the active fragment
        transaction.commit();

        // Bottom navigation setup
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_clothes) {
                switchToFragment(clothesFragment);
            } else if (item.getItemId() == R.id.nav_collections) {
                switchToFragment(collectionsFragment);
            }
            return true;
        });
    }

    private void switchToFragment(Fragment targetFragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Clear back stack to remove any detail fragments
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        // Begin transaction to hide current and show target fragment
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.hide(activeFragment).show(targetFragment).commit();

        activeFragment = targetFragment;
    }

    public void openCollectionDetail(int collectionId, String collectionName) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        CollectionDetailFragment collectionDetailFragment = new CollectionDetailFragment();
        Bundle args = new Bundle();
        args.putInt("collection_id", collectionId);
        args.putString("collection_name", collectionName); // Pass the name
        collectionDetailFragment.setArguments(args);

        fragmentManager.beginTransaction()
                .hide(activeFragment)
                .add(R.id.fragment_container, collectionDetailFragment, "CollectionDetailFragment")
                .addToBackStack(null)
                .commit();

        activeFragment = collectionDetailFragment;
    }


    public void closeCollectionDetail() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Remove the detail fragment and show CollectionsFragment
        fragmentManager.popBackStack(); // Pop the CollectionDetailFragment from back stack
        fragmentManager.beginTransaction()
                .show(collectionsFragment)
                .commit();

        activeFragment = collectionsFragment;
    }

    public void openClothingDetail(int clothingId) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Create a new instance of ClothingDetailFragment
        ClothingDetailFragment clothingDetailFragment = new ClothingDetailFragment();
        Bundle args = new Bundle();
        args.putInt("clothing_id", clothingId);
        clothingDetailFragment.setArguments(args);

        // Add the new fragment and hide the active one
        fragmentManager.beginTransaction()
                .hide(activeFragment)
                .add(R.id.fragment_container, clothingDetailFragment, "ClothingDetailFragment")
                .addToBackStack(null) // Add to back stack to allow back navigation
                .commit();

        activeFragment = clothingDetailFragment;
    }

    public void navigateBackToClothesFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Clear back stack and switch back to clothes fragment
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(clothesFragment)
                .commit();

        activeFragment = clothesFragment;
    }

    public void navigateBackToCollectionDetail() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Remove the current active fragment and return to CollectionDetailFragment
        fragmentManager.popBackStack(); // Pop the ClothingDetailFragment from back stack
        fragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(collectionsFragment) // Ensure CollectionsFragment is shown first
                .commit();

        activeFragment = collectionsFragment;
    }


    public void refreshClothingList() {
        if (clothesFragment instanceof ClothesFragment) {
            ((ClothesFragment) clothesFragment).reloadData(); // Call a new method in ClothesFragment
        }
    }


}
