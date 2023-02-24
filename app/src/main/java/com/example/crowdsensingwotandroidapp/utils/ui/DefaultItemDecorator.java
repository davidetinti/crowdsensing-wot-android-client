package com.example.crowdsensingwotandroidapp.utils.ui;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DefaultItemDecorator extends RecyclerView.ItemDecoration {

	private final int horizontalSpacing;
	private final int verticalSpacing;

	public DefaultItemDecorator(int horizontalSpacing, int verticalSpacing){
		this.horizontalSpacing = horizontalSpacing;
		this.verticalSpacing = verticalSpacing;
	}

	@Override
	public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
		super.getItemOffsets(outRect, view, parent, state);
		outRect.left = horizontalSpacing;
		outRect.right = horizontalSpacing;
		if (parent.getChildLayoutPosition(view) == 0){
			outRect.top = verticalSpacing;
		}
		outRect.bottom = verticalSpacing;
	}
}
