//package com.fz.pocketcloset.temporaryFragments;
//
//import android.content.Context;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.fz.pocketcloset.R;
//
//import java.util.List;
//
//public class TagSuggestionsAdapter extends RecyclerView.Adapter<TagSuggestionsAdapter.TagViewHolder> {
//
//    private final List<String> tags;
//    private TagClickListener listener;
//
//    public TagSuggestionsAdapter(List<String> tags, TagClickListener listener) {
//        this.tags = tags;
//        this.listener = listener;
//    }
//
//    public void setTagClickListener(TagClickListener listener) {
//        this.listener = listener;
//    }
//
//    @NonNull
//    @Override
//    public TagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        TextView tagView = new TextView(parent.getContext());
//        styleTagView(tagView, parent.getContext());
//        return new TagViewHolder(tagView);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull TagViewHolder holder, int position) {
//        String tag = tags.get(position);
//        holder.bind(tag, v -> {
//            if (listener != null) {
//                listener.onTagClick(tag);
//            }
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return tags.size();
//    }
//
//    public void removeTag(String tag) {
//        tags.remove(tag);
//        notifyDataSetChanged();
//    }
//
//    public void addTag(String tag) {
//        if (!tags.contains(tag)) {
//            tags.add(tag);
//            notifyDataSetChanged();
//        }
//    }
//
//    static class TagViewHolder extends RecyclerView.ViewHolder {
//        private final TextView tagView;
//
//        public TagViewHolder(@NonNull View itemView) {
//            super(itemView);
//            this.tagView = (TextView) itemView;
//        }
//
//        public void bind(String tagText, View.OnClickListener clickListener) {
//            tagView.setText(tagText);
//            tagView.setOnClickListener(clickListener);
//        }
//    }
//
//    public interface TagClickListener {
//        void onTagClick(String tag);
//    }
//
//    private void styleTagView(TextView tagView, Context context) {
//        // Set padding inside the TextView
//        tagView.setPadding(16, 8, 16, 8); // Padding inside each tag
//        tagView.setBackgroundResource(R.drawable.tag_background); // Background drawable
//        tagView.setTextSize(14); // Text size
//        tagView.setGravity(android.view.Gravity.CENTER); // Center the text
//
//        // Set layout params with margins
//        ViewGroup.MarginLayoutParams layoutParams = new ViewGroup.MarginLayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//        );
//        layoutParams.setMargins(8, 8, 8, 8); // Margins between tags
//        tagView.setLayoutParams(layoutParams);
//    }
//
//}
//
