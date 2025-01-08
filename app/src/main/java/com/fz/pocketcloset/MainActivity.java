package com.fz.pocketcloset;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.fz.pocketcloset.detailFragments.ClothingDetailFragment;
import com.fz.pocketcloset.detailFragments.CollectionDetailFragment;
import com.fz.pocketcloset.mainFragments.ClothingFragment;
import com.fz.pocketcloset.mainFragments.CollectionsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ViewPager2 viewPager;
    private View fragmentContainer;
    private Fragment activeFragment;
    private Fragment currentDetailFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ViewPager2 for swiping between main fragments
        viewPager = findViewById(R.id.view_pager);
        fragmentContainer = findViewById(R.id.fragment_container); // For detail fragments
        viewPager.setAdapter(new MainFragmentAdapter(this));

        // Initialize BottomNavigationView
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);

        // Sync BottomNavigationView with ViewPager2
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == 0) {
                    bottomNavigation.setSelectedItemId(R.id.nav_clothes);
                } else if (position == 1) {
                    bottomNavigation.setSelectedItemId(R.id.nav_outfits);
                } else if (position == 2) {
                    bottomNavigation.setSelectedItemId(R.id.nav_collections);
                }
                closeDetailFragment(); // Automatically close detail fragments when swiping
            }
        });

        bottomNavigation.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_clothes) {
                viewPager.setCurrentItem(0, true);
            } else if (item.getItemId() == R.id.nav_outfits) {
                viewPager.setCurrentItem(1, true);
            } else if (item.getItemId() == R.id.nav_collections) {
                viewPager.setCurrentItem(2, true);
            }
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the active fragment
        if (activeFragment instanceof ClothingFragment) {
            refreshClothingList();
        } else if (activeFragment instanceof CollectionsFragment) {
            refreshCollections();
        }
    }

    /**
     * Handles deletion of a clothing item.
     */
    public void handleClothingDeletion(String originFragment, Bundle args) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        if ("CollectionDetailFragment".equals(originFragment)) {
            int collectionId = args.getInt("collection_id", -1);
            String tag = "CollectionDetailFragment_" + collectionId;

            // Find the CollectionDetailFragment and reload its data
            CollectionDetailFragment fragment = (CollectionDetailFragment) fragmentManager.findFragmentByTag(tag);
            if (fragment != null) {
                fragment.reloadData(); // Refresh collection details
            } else {
                Log.e(TAG, "CollectionDetailFragment not found for collection ID: " + collectionId);
            }
        }

        // Always refresh the Main Fragments
        refreshClothingList();
        refreshCollections();

        // Navigate back to the appropriate fragment based on origin
        if ("ClothesFragment".equals(originFragment)) {
            navigateBackToClothesFragment();
        } else if ("CollectionDetailFragment".equals(originFragment)) {
            navigateBackToCollectionDetail();
        }
    }


    public void handleCollectionDeletion(int collectionId, String originFragment) {
        // Refresh the CollectionsFragment
        refreshCollections();

        // Navigate back to the correct fragment
        if ("ClothingDetailFragment".equals(originFragment)) {
            navigateBackToClothingDetail();
        } else {
            navigateBackToCollectionsFragment();
        }

        Log.d(TAG, "Collection deletion handled. Collection ID: " + collectionId + ", Origin: " + originFragment);
    }


    /**
     * Opens a ClothingDetailFragment.
     */
    public void openClothingDetail(int clothingId, @Nullable String originTag, int collectionId, @Nullable String collectionName) {
        String tag = "ClothingDetailFragment_" + clothingId;

        // Log dynamic tag for debugging
        Log.d(TAG, "Opening ClothingDetailFragment with tag: " + tag);

        FragmentManager fragmentManager = getSupportFragmentManager();
        ClothingDetailFragment clothingDetailFragment = (ClothingDetailFragment) fragmentManager.findFragmentByTag(tag);

        if (clothingDetailFragment == null) {
            // Create a new instance if it doesn't exist
            clothingDetailFragment = new ClothingDetailFragment();
        }

        // Update arguments
        Bundle args = new Bundle();
        args.putInt("clothing_id", clothingId);
        args.putString("origin", originTag);
        args.putInt("collection_id", collectionId);
        args.putString("collection_name", collectionName);
        clothingDetailFragment.setArguments(args);

        // Show the fragment
        showDetailFragment(clothingDetailFragment, tag);

        // Update active fragment state
        activeFragment = clothingDetailFragment;
        currentDetailFragment = clothingDetailFragment;
    }



    /**
     * Opens a CollectionDetailFragment.
     */
    public void openCollectionDetail(int collectionId, String collectionName, @Nullable String originTag) {
        String tag = "CollectionDetailFragment_" + collectionId;

        FragmentManager fragmentManager = getSupportFragmentManager();
        CollectionDetailFragment collectionDetailFragment = (CollectionDetailFragment) fragmentManager.findFragmentByTag(tag);

        if (collectionDetailFragment == null) {
            collectionDetailFragment = new CollectionDetailFragment();
        }

        // Update arguments
        Bundle args = new Bundle();
        args.putInt("collection_id", collectionId);
        args.putString("collection_name", collectionName);
        args.putString("origin", originTag);

        // Pass clothing_id if coming from a ClothingDetailFragment
        if (activeFragment instanceof ClothingDetailFragment) {
            int clothingId = activeFragment.getArguments() != null ? activeFragment.getArguments().getInt("clothing_id", -1) : -1;
            if (clothingId != -1) {
                args.putInt("clothing_id", clothingId);
            }
        }

        collectionDetailFragment.setArguments(args);

        // Show the fragment
        showDetailFragment(collectionDetailFragment, tag);
    }


    /**
     * Handles showing a detail fragment.
     */
    private void showDetailFragment(Fragment detailFragment, String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        // Ensure ViewPager2 is hidden and fragment container is visible
        viewPager.setVisibility(View.GONE);
        fragmentContainer.setVisibility(View.VISIBLE);

        // Remove existing fragment if necessary
        Fragment existingFragment = fragmentManager.findFragmentById(R.id.fragment_container);
        if (existingFragment != null) {
            fragmentManager.beginTransaction().remove(existingFragment).commitNow(); // Commit immediately
        }

        // Add the new fragment
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, detailFragment, tag)
                .addToBackStack(tag)
                .commit();

        currentDetailFragment = detailFragment;
        activeFragment = detailFragment;
    }


    /**
     * Closes the currently open detail fragment.
     */
    private void closeDetailFragment() {
        if (currentDetailFragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.popBackStack(); // Remove from back stack
            currentDetailFragment = null; // Clear current detail fragment reference

            // Show ViewPager2 again and hide fragment container
            viewPager.setVisibility(View.VISIBLE);
            fragmentContainer.setVisibility(View.GONE);
        }
    }

    public void refreshClothingList() {
        MainFragmentAdapter adapter = (MainFragmentAdapter) viewPager.getAdapter();

        if (adapter != null) {
            ClothingFragment clothingFragment = (ClothingFragment) adapter.getFragmentAtPosition(0);

            if (clothingFragment != null) {
                if (clothingFragment.isAdded()) { // Ensure the fragment is attached to the activity
                    clothingFragment.reloadData();
                    Log.d(TAG, "ClothesFragment refreshed.");
                } else {
                    Log.e(TAG, "ClothesFragment not found.");
                }
            } else {
                Log.e(TAG, "MainFragmentAdapter not initialized.");
            }
        }
    }

    public void refreshCollections() {
        MainFragmentAdapter adapter = (MainFragmentAdapter) viewPager.getAdapter();

        if (adapter != null) {
            CollectionsFragment collectionsFragment = (CollectionsFragment) adapter.getFragmentAtPosition(2);

            if (collectionsFragment != null) {
                if (collectionsFragment.isAdded()) { // Ensure the fragment is attached to the activity
                    collectionsFragment.reloadData();
                    Log.d(TAG, "CollectionsFragment refreshed.");
                } else {
                    Log.e(TAG, "CollectionsFragment is not attached to the activity.");
                }
            } else {
                Log.e(TAG, "CollectionsFragment not found.");
            }
        } else {
            Log.e(TAG, "MainFragmentAdapter not initialized.");
        }
    }



    public void navigateBackToClothingDetail() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        String tag = "ClothingDetailFragment";

        // Check if the fragment exists in the FragmentManager
        ClothingDetailFragment clothingDetailFragment = (ClothingDetailFragment) fragmentManager.findFragmentByTag(tag);

        if (clothingDetailFragment != null && clothingDetailFragment.isAdded()) {
            // Show the existing fragment
            fragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(clothingDetailFragment)
                    .commit();
            currentDetailFragment = clothingDetailFragment;
            activeFragment = clothingDetailFragment;
        } else {
            // Re-create the fragment with arguments stored in the active fragment's Bundle
            if (activeFragment != null && activeFragment.getArguments() != null) {
                Bundle args = activeFragment.getArguments();

                int clothingId = args.getInt("clothing_id", -1);
                String origin = args.getString("origin", "ClothesFragment");
                int collectionId = args.getInt("collection_id", -1);
                String collectionName = args.getString("collection_name", "");

                if (clothingId != -1) {
                    ClothingDetailFragment newFragment = new ClothingDetailFragment();
                    Bundle newArgs = new Bundle();
                    newArgs.putInt("clothing_id", clothingId);
                    newArgs.putString("origin", origin);
                    newArgs.putInt("collection_id", collectionId);
                    newArgs.putString("collection_name", collectionName);
                    newFragment.setArguments(newArgs);

                    fragmentManager.beginTransaction()
                            .hide(activeFragment)
                            .add(R.id.fragment_container, newFragment, tag)
                            .addToBackStack(tag)
                            .commit();
                    currentDetailFragment = newFragment;
                    activeFragment = newFragment;
                } else {
                    Log.e(TAG, "Missing clothing ID for recreating ClothingDetailFragment.");
                }
            } else {
                Log.e(TAG, "No arguments available to recreate ClothingDetailFragment.");
            }
        }
    }

    public void navigateBackToCollectionDetail() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        String tag = "CollectionDetailFragment";

        CollectionDetailFragment collectionDetailFragment = (CollectionDetailFragment) fragmentManager.findFragmentByTag(tag);

        if (collectionDetailFragment != null && collectionDetailFragment.isAdded()) {
            // Show the existing fragment
            fragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(collectionDetailFragment)
                    .commit();
            currentDetailFragment = collectionDetailFragment;
            activeFragment = collectionDetailFragment;
        } else {
            // Re-create the fragment with arguments stored in the active fragment's Bundle
            if (activeFragment != null && activeFragment.getArguments() != null) {
                Bundle args = activeFragment.getArguments();

                int collectionId = args.getInt("collection_id", -1);
                String collectionName = args.getString("collection_name", "");
                String origin = args.getString("origin", "CollectionsFragment");

                if (collectionId != -1) {
                    CollectionDetailFragment newFragment = new CollectionDetailFragment();
                    Bundle newArgs = new Bundle();
                    newArgs.putInt("collection_id", collectionId);
                    newArgs.putString("collection_name", collectionName);
                    newArgs.putString("origin", origin);
                    newFragment.setArguments(newArgs);

                    fragmentManager.beginTransaction()
                            .hide(activeFragment)
                            .add(R.id.fragment_container, newFragment, tag)
                            .addToBackStack(tag)
                            .commit();
                    currentDetailFragment = newFragment;
                    activeFragment = newFragment;
                } else {
                    Log.e(TAG, "Missing collection ID for recreating CollectionDetailFragment.");
                }
            } else {
                Log.e(TAG, "No arguments available to recreate CollectionDetailFragment.");
            }
        }
    }



    public void navigateBackToCollectionsFragment() {
        closeDetailFragment();
        viewPager.setCurrentItem(2, true);
    }

    public void navigateBackToClothesFragment() {
        closeDetailFragment();
        viewPager.setCurrentItem(0, true);
    }
}
