package com.fz.pocketcloset;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class MainFragmentAdapter extends FragmentStateAdapter {
    private final List<Fragment> fragmentList;

    public MainFragmentAdapter(FragmentActivity fragmentActivity) {
        super(fragmentActivity);

        // Initialize the fragment list with instances of the fragments
        fragmentList = new ArrayList<>();
        fragmentList.add(new ClothesFragment());    // Position 0
        fragmentList.add(new OutfitsFragment());    // Position 1
        fragmentList.add(new CollectionsFragment()); // Position 2
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentList.get(position); // Return the fragment from the list
    }

    @Override
    public int getItemCount() {
        return fragmentList.size(); // Number of main fragments
    }

    public Fragment getFragmentAtPosition(int position) {
        if (position >= 0 && position < fragmentList.size()) {
            return fragmentList.get(position);
        } else {
            throw new IndexOutOfBoundsException("Invalid position: " + position);
        }
    }
}
