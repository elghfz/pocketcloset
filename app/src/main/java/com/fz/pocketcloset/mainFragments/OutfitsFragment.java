package com.fz.pocketcloset.mainFragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fz.pocketcloset.R;
import com.fz.pocketcloset.items.ClothingItem;
import com.fz.pocketcloset.items.Outfit;
import com.fz.pocketcloset.items.SelectableItem;
import com.fz.pocketcloset.temporaryFragments.OutfitCreationFragment;
import com.fz.pocketcloset.temporaryFragments.SelectionAdapter;
import com.fz.pocketcloset.temporaryFragments.SelectionFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OutfitsFragment extends Fragment implements SelectionFragment.SelectionListener {

    private static final String TAG = "OutfitFragment";
    private RecyclerView recyclerView;
    private static OutfitAdapter adapter;

    private ImageButton addOutfitButton, deleteButton;
    private boolean isSelectionMode = false;
    private final Set<Outfit> selectedItems = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_outfits, container, false);

        try {
            recyclerView = view.findViewById(R.id.outfitRecyclerView);
            recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

            addOutfitButton = view.findViewById(R.id.addOutfitButton);
            deleteButton = view.findViewById(R.id.deleteButton);

            addOutfitButton.setOnClickListener(v -> showAddOutfitFragment());
            deleteButton.setOnClickListener(v -> deleteSelectedOutfits());

            updateButtonVisibility();
            loadOutfits();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing OutfitFragment: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading outfits.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    private void showAddOutfitFragment() {
        // Fetch all clothing items
        ClothingManager clothingManager = new ClothingManager(requireContext());
        List<ClothingItem> availableClothes = clothingManager.getAllClothingItems();

        // If no clothes are available, show a message and return
        if (availableClothes.isEmpty()) {
            Toast.makeText(requireContext(), "No clothes available to select.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the clothing items directly as selectable items (no additional wrapping needed)
        List<SelectableItem> selectableItems = new ArrayList<>(availableClothes);
        boolean[] selectedItems = new boolean[availableClothes.size()];

        // Create and show the SelectionFragment
        SelectionFragment fragment = SelectionFragment.newInstance(
                "Select Clothes for Outfit",
                selectableItems,
                selectedItems
        );

        getChildFragmentManager().beginTransaction()
                .replace(R.id.dynamic_fragment_container, fragment, "SelectionFragment")
                .addToBackStack(null)
                .commit();

        // Make the container visible
        if (getView() != null) {
            getView().findViewById(R.id.dynamic_fragment_container).setVisibility(View.VISIBLE);
        }
    }



    @Override
    public void onSelectionSaved(boolean[] selectedItems) {
        try {
            // Get all selected clothes
            List<ClothingItem> selectedClothes = new ArrayList<>();
            List<ClothingItem> allClothes = new ClothingManager(requireContext()).getAllClothingItems();

            for (int i = 0; i < selectedItems.length; i++) {
                if (selectedItems[i]) {
                    selectedClothes.add(allClothes.get(i));
                }
            }

            if (selectedClothes.isEmpty()) {
                Toast.makeText(requireContext(), "No clothes selected to create an outfit!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hide the SelectionFragment
            hideSelectionFragment();

            // Navigate to the OutfitCreationFragment with the selected clothes
            OutfitCreationFragment creationFragment = OutfitCreationFragment.newInstance(selectedClothes);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.dynamic_fragment_container, creationFragment, "OutfitCreationFragment")
                    .addToBackStack(null)
                    .commit();

            if (getView() != null) {
                getView().findViewById(R.id.dynamic_fragment_container).setVisibility(View.VISIBLE);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error saving outfit: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error saving the outfit.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSelectionCancelled() {
        Toast.makeText(requireContext(), "Selection cancelled.", Toast.LENGTH_SHORT).show();
        hideSelectionFragment();
    }

    private void hideSelectionFragment() {
        Fragment fragment = getChildFragmentManager().findFragmentByTag("SelectionFragment");
        if (fragment != null) {
            getChildFragmentManager().beginTransaction()
                    .remove(fragment)
                    .commit();
        }

        if (getView() != null) {
            View selectionContainer = getView().findViewById(R.id.dynamic_fragment_container);
            if (selectionContainer != null) {
                selectionContainer.setVisibility(View.GONE);
            }
        }
    }

    private void loadOutfits() {
        try {
            List<Outfit> outfits = new OutfitManager(requireContext()).getAllOutfits();

            if (adapter == null) {
                adapter = new OutfitAdapter(
                        outfits,
                        this::handleItemClick,
                        this::handleItemLongClick,
                        isSelectionMode
                );
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateData(outfits); // Dynamically update the data
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading outfits: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading outfits.", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleItemClick(Outfit outfit) {
        if (isSelectionMode) {
            toggleItemSelection(outfit);
        } else {
            Toast.makeText(requireContext(), "Clicked on outfit: " + outfit.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleItemLongClick(Outfit outfit) {
        if (outfit == null) {
            exitSelectionMode();
            return;
        }
        if (!isSelectionMode) {
            isSelectionMode = true;
            updateButtonVisibility(); // Show Delete Button
        }
        toggleItemSelection(outfit);
    }

    private void toggleItemSelection(Outfit outfit) {
        if (selectedItems.contains(outfit)) {
            selectedItems.remove(outfit);
        } else {
            selectedItems.add(outfit);
        }

        // Exit selection mode if all items are deselected
        if (selectedItems.isEmpty()) {
            exitSelectionMode();
        }

        adapter.notifyDataSetChanged();
    }

    private void exitSelectionMode() {
        isSelectionMode = false;
        selectedItems.clear();
        adapter.setSelectionMode(false);
        updateButtonVisibility(); // This hides the Delete Button
    }

    private void deleteSelectedOutfits() {
        try {
            for (Outfit outfit : selectedItems) {
                new OutfitManager(requireContext()).deleteOutfit(outfit.getId());
            }
            Toast.makeText(requireContext(), "Selected outfits deleted!", Toast.LENGTH_SHORT).show();
            exitSelectionMode();
            loadOutfits();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting outfits: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error deleting outfits.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateButtonVisibility() {
        if (isSelectionMode) {
            addOutfitButton.setVisibility(View.GONE); // Hide "Add Outfit" button
            deleteButton.setVisibility(View.VISIBLE); // Show "Delete" button
        } else {
            addOutfitButton.setVisibility(View.VISIBLE); // Show "Add Outfit" button
            deleteButton.setVisibility(View.GONE); // Hide "Delete" button
        }
    }
    public void reloadOutfits() {
        try {
            if (isAdded()) { // Ensure the fragment is attached
                loadOutfits();
                Log.d(TAG, "Outfits reloaded successfully.");
            } else {
                Log.e(TAG, "OutfitsFragment is not attached to the activity.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reloading outfits: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error reloading outfits.", Toast.LENGTH_SHORT).show();
        }
    }



}
