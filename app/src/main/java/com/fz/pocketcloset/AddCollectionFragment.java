package com.fz.pocketcloset;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AddCollectionFragment extends Fragment {

    private EditText collectionNameEditText;
    private TextView collectionEmojiTextView;
    private ImageButton saveButton, cancelButton;

    public static AddCollectionFragment newInstance() {
        return new AddCollectionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_collection, container, false);

        collectionNameEditText = view.findViewById(R.id.collectionNameEditText);
        collectionEmojiTextView = view.findViewById(R.id.collectionEmojiTextView);
        saveButton = view.findViewById(R.id.saveButton);
        cancelButton = view.findViewById(R.id.cancelButton);

        saveButton.setOnClickListener(v -> handleSaveClick());
        cancelButton.setOnClickListener(v -> hideFragment());
        collectionEmojiTextView.setOnClickListener(v -> updateEmoji());

        return view;
    }

    private void handleSaveClick() {
        String name = collectionNameEditText.getText().toString().trim();
        String emoji = collectionEmojiTextView.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(requireContext(), "Collection name cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save collection to the database
        new CollectionsManager(requireContext()).addCollection(name, emoji);

        Toast.makeText(requireContext(), "Collection added!", Toast.LENGTH_SHORT).show();
        hideFragment();

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshCollections();
        }
    }

    private void hideFragment() {
        requireActivity().findViewById(R.id.dynamic_fragment_container).setVisibility(View.GONE);
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void updateEmoji() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Pick an Emoji");

        final EditText input = new EditText(requireContext());
        input.setHint("Enter an emoji");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String emoji = input.getText().toString().trim();
            if (isEmoji(emoji)) {
                collectionEmojiTextView.setText(emoji);
                Toast.makeText(requireContext(), "Emoji updated!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "Invalid emoji. Please enter a valid emoji.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    private boolean isEmoji(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        int codePoint = input.codePointAt(0);
        return Character.isSupplementaryCodePoint(codePoint);
    }
}
