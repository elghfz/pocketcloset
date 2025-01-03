package com.fz.pocketcloset;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

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
    private Fragment outfitsFragment;
    private Fragment activeFragment; // Tracks the currently active fragment
    private Fragment currentCollectionDetailFragment; // Tracks the active CollectionDetailFragment


    @Override
    protected void onResume() {
        super.onResume();
        if (activeFragment instanceof ClothesFragment) {
            ((ClothesFragment) activeFragment).reloadData();
        }
    }

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
                    .hide(clothesFragment)
                    .commit();
        } else {
            clothesFragment = fragmentManager.findFragmentByTag("ClothesFragment");
        }

        if (fragmentManager.findFragmentByTag("OutfitsFragment") == null) {
            outfitsFragment = new OutfitsFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, outfitsFragment, "OutfitsFragment")
                    .commit();
        } else {
            outfitsFragment = fragmentManager.findFragmentByTag("OutfitsFragment");
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

        activeFragment = outfitsFragment; // Start with OutfitsFragment as the active fragment

        // Setup navigation
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);

        // Set the default selected item to Outfits
        bottomNavigation.setSelectedItemId(R.id.nav_outfits);

        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_clothes) {
                switchToFragment(clothesFragment);
            } else if (item.getItemId() == R.id.nav_outfits) {
                switchToFragment(outfitsFragment);
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

        String fragmentTag = "CollectionDetailFragment_" + collectionId;
        CollectionDetailFragment collectionDetailFragment = (CollectionDetailFragment) fragmentManager.findFragmentByTag(fragmentTag);

        if (collectionDetailFragment == null) {
            collectionDetailFragment = new CollectionDetailFragment();
            Bundle args = new Bundle();
            args.putInt("collection_id", collectionId);
            args.putString("collection_name", collectionName);
            collectionDetailFragment.setArguments(args);

            fragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .add(R.id.fragment_container, collectionDetailFragment, fragmentTag)
                    .addToBackStack(null)
                    .commit();
        } else {
            fragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(collectionDetailFragment)
                    .commit();
        }

        activeFragment = collectionDetailFragment;
        currentCollectionDetailFragment = collectionDetailFragment;
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


    public void openClothingDetail(int clothingId, @Nullable String originTag, int collectionId, @Nullable String collectionName) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        ClothingDetailFragment clothingDetailFragment = (ClothingDetailFragment)
                fragmentManager.findFragmentByTag("ClothingDetailFragment");

        if (clothingDetailFragment == null) {
            clothingDetailFragment = new ClothingDetailFragment();
            Bundle args = new Bundle();
            args.putInt("clothing_id", clothingId);
            args.putString("origin", originTag);
            args.putInt("collection_id", collectionId);
            args.putString("collection_name", collectionName);
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
        Log.d(TAG, "Navigating back to CollectionDetailFragment.");
        if (currentCollectionDetailFragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();

            fragmentManager.popBackStack(); // Remove the ClothingDetailFragment from the back stack
            fragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(currentCollectionDetailFragment)
                    .commit();

            activeFragment = currentCollectionDetailFragment;
        } else {
            Log.e(TAG, "No CollectionDetailFragment is currently active!");
        }
    }


    public void refreshCollections() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment collectionsFragment = fragmentManager.findFragmentByTag("CollectionsFragment");

        if (collectionsFragment instanceof CollectionsFragment) {
            ((CollectionsFragment) collectionsFragment).reloadData();
        }
    }

    public void refreshClothingList() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = fragmentManager.findFragmentByTag("ClothesFragment");

        if (fragment instanceof ClothesFragment) {
            ((ClothesFragment) fragment).reloadData();
        } else {
            Log.e(TAG, "ClothesFragment not found for refreshing.");
        }
    }

    public void handleClothingDeletion(String originFragment, Bundle args) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        if ("CollectionDetailFragment".equals(originFragment)) {
            int collectionId = args.getInt("collection_id", -1);
            String tag = "CollectionDetailFragment_" + collectionId;

            // Find the CollectionDetailFragment and reload its data
            CollectionDetailFragment fragment = (CollectionDetailFragment) fragmentManager.findFragmentByTag(tag);
            if (fragment != null) {
                fragment.reloadData();
            } else {
                Log.e(TAG, "CollectionDetailFragment not found for collection ID: " + collectionId);
            }
        }

        // Always refresh the ClothesFragment
        refreshClothingList();

        // Navigate back to the appropriate fragment based on origin
        if ("ClothesFragment".equals(originFragment)) {
            navigateBackToClothesFragment();
        } else if ("CollectionDetailFragment".equals(originFragment)) {
            navigateBackToCollectionDetail();
        }
    }







}
