package de.lmu.ifi.medien.mime;

import java.util.ArrayList;

import org.opencv.core.Point;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class PoseListHelper {
	
	private ListView mListView;
	private PoseListAdapter mAdapter;
	
	private static PoseListHelper instance = null;
	
	private PoseListHelper() { }
	
	/**
	 * Returns the singleton instance of this class
	 * @return Instance of this class
	 */
	public static PoseListHelper getInstance() {
		if (instance == null) {
			instance = new PoseListHelper();
		}
		return instance;
	}
	
	/**
	 * Creates and initializes the ListView
	 * @param ctx Application context
	 * @param pr Instance of the PoseRecognizer
	 * @param list ListView
	 * @param onLongClickCallback Action that will be executed when an item is long clicked
	 */
	@SuppressLint("InflateParams")
	public void init(final Context ctx, PoseRecognizer pr, ListView list, final Util.Callback<Integer> onLongClickCallback) {
		LayoutInflater li = LayoutInflater.from(ctx);
		View headerView = li.inflate(R.layout.list_header, null);
		list.addHeaderView(headerView);
		
		mAdapter = new PoseListAdapter(ctx, pr.getPoses());
		list.setAdapter(mAdapter);
    	
		list.setOnItemLongClickListener(new OnItemLongClickListener() {
    		// Long click listener for list items to delete single items from list
            public boolean onItemLongClick(AdapterView<?> parent, View strings, final int position, long id) {
            	Util.vibrate(ctx, 150);
            	// Show confirmation dialog; header view = position 0
            	if (position >= 1) {
	            	Util.confirm(ctx, R.string.message_delete_pose, new Util.Callback<Void>(){
						@Override
						public void call(Void param) {
							onLongClickCallback.call(position-1);
							mAdapter.notifyDataSetChanged();
						}
	            	});
            	}
            	return true;
            }
    	});
		
		mListView = list;
	}
	
	/**
	 * Toggle visibility
	 * @param visible Set visible
	 */
	public void setVisible(boolean visible) {
		mListView.setVisibility(visible ? View.VISIBLE : View.GONE);
	}
	
	/**
	 * Frees resources to prevent memory leaks
	 */
	public void clean() {
		instance = null;
	}
	
	
	private class PoseListAdapter extends ArrayAdapter<Pose> {
		public PoseListAdapter(Context context, ArrayList<Pose> data) {
			super(context, R.layout.list_item, data);
		}
		
		@SuppressLint("InflateParams")
		@Override
		public View getView(int position, View row, ViewGroup parent) {
			RowHolder holder;

			if (row == null) {
				LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row = inflater.inflate(R.layout.list_item, null, false);
				holder = new RowHolder(row);
				row.setTag(holder);
			}
			else {
				holder = (RowHolder) row.getTag();
			}

			Pose pose = getItem(position);
			String desc = PoseRecognizer.getDescription(pose.getType());
			holder.getNameView().setText(desc);
			
			Point[][] strokes = pose.getMultistroke().getOrigStrokes();
			new PoseRenderTask<RowHolder>().execute(row, strokes);
			
			return row;
		}
	}
	
	private class RowHolder implements Holder {
		View mRow;
		TextView mNameView;
		ImageView mImageView;

		public RowHolder(View row) {
			mRow = row;
		}

		public ImageView getImageView() {
			if (mImageView == null) {
				mImageView = (ImageView) mRow.findViewById(R.id.pose_image);
			}
			return mImageView;
		}

		public TextView getNameView() {
			if (mNameView == null) {
				mNameView = (TextView) mRow.findViewById(R.id.pose_title);
			}
			return mNameView;
		}
	}
	
	private class PoseRenderTask<T extends Holder> extends AsyncTask<Object,Void,Bitmap> {
		private View row;
		
		@Override
		protected Bitmap doInBackground(Object... args) {
			row = (View) args[0];
			Point[][] strokes = (Point[][]) args[1];
			
			Bitmap.Config conf = Bitmap.Config.RGB_565;
			Bitmap bitmap = Bitmap.createBitmap(200, 200, conf);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(0xFFDDDDDD);
			
			Paint paint = new Paint();
			paint.setStyle(Style.STROKE);
			paint.setStrokeWidth(10.0f);
			paint.setAntiAlias(true);
			paint.setColor(0xFF9ED231);
			
			for (Point[] points : strokes) {
				for (int i = 0; i < points.length-1; ++i) {
					canvas.drawLine((float) points[i].x+40, (float) points[i].y, (float) points[i+1].x+40, (float) points[i+1].y, paint);
				}
			}
			
			return bitmap;
		}
		
		@SuppressWarnings("unchecked")
		protected void onPostExecute(Bitmap img) {
			if (img != null) {
				((T) row.getTag()).getImageView().setImageBitmap(img);
			}
		}
	}
	
	private interface Holder {
		ImageView getImageView();
	}
	
}