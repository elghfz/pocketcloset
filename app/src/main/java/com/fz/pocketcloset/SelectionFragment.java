package com.fz.pocketcloset;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SelectionFragment extends Fragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_ITEMS = "items";
    private static final String ARG_SELECTED = "selected";
    private static final String TAG = "SelectionFragment";

    private String title;
    private List<SelectableItem> items;
    private boolean[] selectedItems;

    private SelectionListener listener;

    public interface SelectionListener {
        void onSelectionSaved(boolean[] selected);
        void onSelectionCancelled();
    }

    public static SelectionFragment newInstance(String title, List<SelectableItem> items, boolean[] selected) {
        SelectionFragment fragment = new SelectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putParcelableArrayList(ARG_ITEMS, new ArrayList<>(items));
        args.putBooleanArray(ARG_SELECTED, selected);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (getParentFragment() instanceof SelectionListener) {
            listener = (SelectionListener) getParentFragment();
        } else if (context instanceof SelectionListener) {
            listener = (SelectionListener) context;
        } else {
            throw new RuntimeException("Parent must implement SelectionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            title = getArguments().getString(ARG_TITLE);
            items = getArguments().getParcelableArrayList(ARG_ITEMS); // Retrieve the passed SelectableItem list
            selectedItems = getArguments().getBooleanArray(ARG_SELECTED); // Retrieve selected state array
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_selection, container, false);

        // RecyclerView setup
        RecyclerView recyclerView = view.findViewById(R.id.selectionRecyclerView);

        // Set GridLayoutManager with span count as 4
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
        recyclerView.setLayoutManager(layoutManager);

        if (items != null && selectedItems != null) {
            SelectionAdapter adapter = new SelectionAdapter(items, selectedItems);
            recyclerView.setAdapter(adapter);
            Log.d(TAG, "Adapter initialized and set to RecyclerView.");

        } else {
            Log.e(TAG, "Items or selectedItems not initialized.");
        }

        // Save and Cancel button setup
        ImageButton saveButton = view.findViewById(R.id.buttonSave);
        ImageButton cancelButton = view.findViewById(R.id.buttonCancel);

        saveButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSelectionSaved(selectedItems); // Notify the listener with the selected states
            }
        });

        cancelButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSelectionCancelled(); // Notify the listener on cancel
            }
        });

        return view;
    }

}
