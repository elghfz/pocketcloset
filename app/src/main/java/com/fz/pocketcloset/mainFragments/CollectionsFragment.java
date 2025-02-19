package com.fz.pocketcloset.mainFragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fz.pocketcloset.MainActivity;
import com.fz.pocketcloset.R;
import com.fz.pocketcloset.helpers.DatabaseHelper;
import com.fz.pocketcloset.items.Collection;
import com.fz.pocketcloset.temporaryFragments.AddCollectionFragment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectionsFragment extends Fragment {

    private static final String TAG = "CollectionsFragment";
    private RecyclerView recyclerView;
    private CollectionAdapter adapter;

    private ImageButton addCollectionButton, deleteButton;
    private boolean isSelectionMode = false;
    private final Set<Collection> selectedItems = new HashSet<>();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_collections, container, false);

        try {
            DatabaseHelper dbHelper = new DatabaseHelper(requireContext());
            recyclerView = view.findViewById(R.id.collectionRecyclerView);
            recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));


            addCollectionButton = view.findViewById(R.id.addCollectionButton);
            deleteButton = view.findViewById(R.id.deleteButton);

            addCollectionButton.setOnClickListener(v -> showAddCollectionFragment());
            deleteButton.setOnClickListener(v -> deleteSelectedCollections());

            updateButtonVisibility();
            loadCollections();

        } catch (Exception e) {
            Log.e(TAG, "Error initializing CollectionsFragment: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading collections.", Toast.LENGTH_SHORT).show();
        }

        return view;
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Create and register callback for back press
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (isSelectionMode) {
                            exitSelectionMode();
                        } else {
                            setEnabled(false);
                            requireActivity().getOnBackPressedDispatcher().onBackPressed();
                            setEnabled(true);
                        }
                    }
                });
    }

    private void loadCollections() {
        try {
            List<Collection> collections = new CollectionsManager(requireContext()).getAllCollections();

            if (adapter == null) {
                adapter = new CollectionAdapter(
                        collections,
                        this::handleItemClick,
                        this::handleItemLongClick,
                        isSelectionMode,
                        null
                );
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateData(collections); // Dynamically update the data
                adapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading collections: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error loading collections.", Toast.LENGTH_SHORT).show();
        }
    }


    private void handleItemClick(Collection collection) {
        if (isSelectionMode) {
            toggleItemSelection(collection);
        } else {
            openCollection(collection);
        }
    }

    private void handleItemLongClick(Collection collection) {
        if (collection == null) {
            exitSelectionMode();
            return;
        }
        if (!isSelectionMode) {
            isSelectionMode = true;
            updateButtonVisibility(); // Show Delete Button
        }
        toggleItemSelection(collection);
    }


    private void toggleItemSelection(Collection collection) {
        if (selectedItems.contains(collection)) {
            selectedItems.remove(collection);
        } else {
            selectedItems.add(collection);
        }

        // Exit selection mode if all items are deselected
        if (selectedItems.isEmpty() && isSelectionMode) {
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


    private void deleteSelectedCollections() {
        try {
            for (Collection collection : selectedItems) {
                new CollectionsManager(requireContext()).deleteCollection(collection.getId());
            }
            Toast.makeText(requireContext(), "Selected collections deleted!", Toast.LENGTH_SHORT).show();
            exitSelectionMode();
            loadCollections();
        } catch (Exception e) {
            Log.e(TAG, "Error deleting selected collections: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Error deleting collections.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddCollectionFragment() {
        AddCollectionFragment fragment = AddCollectionFragment.newInstance();

        // Display the dynamic container
        View container = requireActivity().findViewById(R.id.dynamic_fragment_container);
        if (container != null) {
            container.setVisibility(View.VISIBLE);
        }

        // Replace the container with the fragment
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.dynamic_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }



    public void reloadData() {
        if (!isAdded()) {
            Log.e(TAG, "Fragment not attached to context, skipping reloadData.");
            return;
        }
        try {
            loadCollections();
        } catch (Exception e) {
            Log.e(TAG, "Error reloading collection data: " + e.getMessage(), e);
        }
    }



    private void openCollection(Collection collection) {
        if (!isSelectionMode) {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openCollectionDetail(collection.getId(), collection.getName(), "CollectionsFragment");
            }
        }
    }

    private void updateButtonVisibility() {
        if (isSelectionMode) {
            addCollectionButton.setVisibility(View.GONE); // Hide "Add Collection" button
            deleteButton.setVisibility(View.VISIBLE);    // Show "Delete" button
        } else {
            addCollectionButton.setVisibility(View.VISIBLE); // Show "Add Collection" button
            deleteButton.setVisibility(View.GONE);           // Hide "Delete" button
        }
    }

}
