package com.fz.pocketcloset;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private Fragment clothesFragment;
    private Fragment collectionsFragment;
    private Fragment activeFragment; // Tracks the currently active fragment
    private Fragment currentCollectionDetailFragment; // Tracks the active CollectionDetailFragment


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize fragments
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (fragmentManager.findFragmentByTag("ClothesFragment") == null) {
            clothesFragment = new ClothesFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, clothesFragment, "ClothesFragment")
                    .commit();
        } else {
            clothesFragment = fragmentManager.findFragmentByTag("ClothesFragment");
        }

        if (fragmentManager.findFragmentByTag("CollectionsFragment") == null) {
            collectionsFragment = new CollectionsFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, collectionsFragment, "CollectionsFragment")
                    .hide(collectionsFragment)
                    .commit();
        } else {
            collectionsFragment = fragmentManager.findFragmentByTag("CollectionsFragment");
        }

        activeFragment = clothesFragment; // Start with ClothesFragment as the active fragment

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

        // Hide all fragments and show the target
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (fragment != null) {
                transaction.hide(fragment);
            }
        }
        transaction.show(targetFragment).commit();

        // Update the active fragment
        activeFragment = targetFragment;
    }



    public void openCollectionDetail(int collectionId, String collectionName) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        CollectionDetailFragment collectionDetailFragment = (CollectionDetailFragment)
                fragmentManager.findFragmentByTag("CollectionDetailFragment_" + collectionId);

        if (collectionDetailFragment == null) {
            collectionDetailFragment = new CollectionDetailFragment();
            Bundle args = new Bundle();
            args.putInt("collection_id", collectionId);
            args.putString("collection_name", collectionName);
            collectionDetailFragment.setArguments(args);

            fragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .add(R.id.fragment_container, collectionDetailFragment, "CollectionDetailFragment_" + collectionId)
                    .addToBackStack(null)
                    .commit();
        } else {
            fragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(collectionDetailFragment)
                    .addToBackStack(null)
                    .commit();
        }

        activeFragment = collectionDetailFragment;
        currentCollectionDetailFragment = collectionDetailFragment; // Track the active CollectionDetailFragment
    }




    public void closeCollectionDetail() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Remove the detail fragment and show CollectionsFragment
        fragmentManager.popBackStack();
        fragmentManager.beginTransaction()
                .show(collectionsFragment)
                .commit();

        activeFragment = collectionsFragment;
    }


    public void openClothingDetail(int clothingId, @Nullable String originTag) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        ClothingDetailFragment clothingDetailFragment = (ClothingDetailFragment)
                fragmentManager.findFragmentByTag("ClothingDetailFragment");

        if (clothingDetailFragment == null) {
            clothingDetailFragment = new ClothingDetailFragment();
            Bundle args = new Bundle();
            args.putInt("clothing_id", clothingId);
            args.putString("origin", originTag); // Pass the origin fragment's tag
            clothingDetailFragment.setArguments(args);

            fragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .add(R.id.fragment_container, clothingDetailFragment, "ClothingDetailFragment")
                    .addToBackStack(null)
                    .commit();
        } else {
            fragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(clothingDetailFragment)
                    .addToBackStack(null)
                    .commit();
        }

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
        if (currentCollectionDetailFragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();

            fragmentManager.popBackStack(); // Remove the ClothingDetailFragment from the back stack
            fragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(currentCollectionDetailFragment)
                    .commit();

            activeFragment = currentCollectionDetailFragment;
        } else {
            Log.e("MainActivity", "No CollectionDetailFragment is currently active!");
        }
    }




    public void refreshClothingList() {
        if (clothesFragment instanceof ClothesFragment) {
            ((ClothesFragment) clothesFragment).reloadData(); // Call a new method in ClothesFragment
        }
    }


}
