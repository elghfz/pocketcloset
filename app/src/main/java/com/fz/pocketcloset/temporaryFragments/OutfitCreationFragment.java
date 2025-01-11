package com.fz.pocketcloset.temporaryFragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fz.pocketcloset.MainActivity;
import com.fz.pocketcloset.R;
import com.fz.pocketcloset.items.ClothingItem;
import com.fz.pocketcloset.mainFragments.OutfitManager;
import com.fz.pocketcloset.mainFragments.OutfitsFragment;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class OutfitCreationFragment extends Fragment {

    private static final String TAG = "OutfitCreationFragment";
    private static final String ARG_SELECTED_ITEMS = "selected_items";

    private FrameLayout canvasContainer;
    private ImageButton buttonSave, buttonCancel;
    private List<ClothingItem> selectedClothes;

    public static OutfitCreationFragment newInstance(List<ClothingItem> selectedClothes) {
        OutfitCreationFragment fragment = new OutfitCreationFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_SELECTED_ITEMS, new ArrayList<>(selectedClothes));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            selectedClothes = getArguments().getParcelableArrayList(ARG_SELECTED_ITEMS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_outfit_creation, container, false);

        canvasContainer = view.findViewById(R.id.canvasContainer);
        buttonSave = view.findViewById(R.id.buttonSave);
        buttonCancel = view.findViewById(R.id.buttonCancel);

        renderSelectedClothes();

        buttonCancel.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Outfit creation canceled.", Toast.LENGTH_SHORT).show();
            showRegularFragment();
        });
        buttonSave.setOnClickListener(v -> saveOutfit());

        return view;
    }

    private void renderSelectedClothes() {
        canvasContainer.post(() -> {
            int centerX = canvasContainer.getWidth() / 2;
            int centerY = canvasContainer.getHeight() / 2;

            for (ClothingItem item : selectedClothes) {
                ImageView imageView = new ImageView(requireContext());
                imageView.setImageBitmap(BitmapFactory.decodeFile(item.getImagePath()));
                imageView.setLayoutParams(new FrameLayout.LayoutParams(300, 300)); // Set default size

                // Center the image
                imageView.setX(centerX - 150); // Adjust by half width
                imageView.setY(centerY - 150); // Adjust by half height

                imageView.setOnTouchListener(new ImageTouchListener());
                canvasContainer.addView(imageView);
            }
        });
    }

    private class ImageTouchListener implements View.OnTouchListener {
        private float dX, dY;
        private float scale = 1.0f;
        private float initialDistance = 0;
        private float initialAngle = 0;

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            view.getParent().requestDisallowInterceptTouchEvent(true); // Disable ViewPager2 swipe

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    dX = view.getX() - event.getRawX();
                    dY = view.getY() - event.getRawY();
                    view.bringToFront(); // Bring to front on interaction
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (event.getPointerCount() == 1) { // Single touch for dragging
                        view.setX(event.getRawX() + dX);
                        view.setY(event.getRawY() + dY);
                    } else if (event.getPointerCount() == 2) { // Multitouch for scaling and rotation
                        float currentDistance = getDistance(event);
                        float currentAngle = getAngle(event);

                        if (initialDistance > 0) {
                            scale *= currentDistance / initialDistance;
                            view.setScaleX(scale);
                            view.setScaleY(scale);
                        }

                        if (initialAngle > 0) {
                            float rotationDelta = currentAngle - initialAngle;
                            view.setRotation(view.getRotation() + rotationDelta);
                        }

                        initialDistance = currentDistance;
                        initialAngle = currentAngle;
                    }
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    if (event.getPointerCount() == 2) {
                        initialDistance = getDistance(event);
                        initialAngle = getAngle(event);
                    }
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                    initialDistance = 0;
                    initialAngle = 0;
                    break;

                default:
                    return false;
            }
            return true;
        }

        private float getDistance(MotionEvent event) {
            float dx = event.getX(1) - event.getX(0);
            float dy = event.getY(1) - event.getY(0);
            return (float) Math.sqrt(dx * dx + dy * dy);
        }

        private float getAngle(MotionEvent event) {
            double dx = event.getX(1) - event.getX(0);
            double dy = event.getY(1) - event.getY(0);
            return (float) Math.toDegrees(Math.atan2(dy, dx));
        }
    }

    private void saveOutfit() {
        try {
            // Define output dimensions
            int outputWidth = 120;
            int outputHeight = 200;

            // Create a bitmap for the visible canvas content
            Bitmap visibleContentBitmap = Bitmap.createBitmap(canvasContainer.getWidth(), canvasContainer.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(visibleContentBitmap);
            canvasContainer.draw(canvas); // Draw the canvas and its children onto the bitmap

            // Create a scaled bitmap with the desired dimensions
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(visibleContentBitmap, outputWidth, outputHeight, true);

            // Save the scaled bitmap
            File outputDir = new File(requireContext().getFilesDir(), "outfits");
            if (!outputDir.exists()) {
                boolean created = outputDir.mkdirs();
                if (!created) {
                    throw new IllegalStateException("Failed to create directory: " + outputDir.getAbsolutePath());
                }
            }

            File outputFile = new File(outputDir, System.currentTimeMillis() + ".png");
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, out); // Reduced compression to 90%
            }

            // Save to database
            String outfitName = "Outfit " + System.currentTimeMillis();
            new OutfitManager(requireContext()).addOutfit(outfitName, outputFile.getAbsolutePath(), selectedClothes);

            Toast.makeText(requireContext(), "Outfit saved!", Toast.LENGTH_SHORT).show();
            showRegularFragment(); // Ensure the regular fragment is shown after saving

        } catch (Exception e) {
            Log.e(TAG, "Error saving outfit: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Failed to save outfit.", Toast.LENGTH_SHORT).show();
        }
    }



    private void showRegularFragment() {
        getParentFragmentManager().popBackStack(); // Go back to the previous fragment
        if (getActivity() != null) {
            View container = getActivity().findViewById(R.id.dynamic_fragment_container);
            if (container != null) {
                container.setVisibility(View.GONE); // Hide dynamic container
            }
        }

        // Ensure the OutfitsFragment is refreshed
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshOutfitsFragment();
        }
    }




}
