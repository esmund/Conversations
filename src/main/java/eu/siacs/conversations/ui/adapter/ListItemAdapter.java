package eu.siacs.conversations.ui.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.wefika.flowlayout.FlowLayout;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import eu.siacs.conversations.R;
import eu.siacs.conversations.entities.Contact;
import eu.siacs.conversations.entities.ListItem;
import eu.siacs.conversations.ui.XmppActivity;
import eu.siacs.conversations.utils.UIHelper;

public class ListItemAdapter extends ArrayAdapter<ListItem> {

	protected XmppActivity activity;
	protected boolean showDynamicTags = false;
    private int defaultTextColor = 0;
	private View.OnClickListener onTagTvClick = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
            if (view instanceof  TextView && mOnTagClickedListener != null) {
				TextView tv = (TextView) view;
				final String tag = tv.getText().toString();
				mOnTagClickedListener.onTagClicked(tag);
			}
		}
	};
	private OnTagClickedListener mOnTagClickedListener = null;

	public ListItemAdapter(XmppActivity activity, List<ListItem> objects) {
		super(activity, 0, objects);
		this.activity = activity;
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
		this.showDynamicTags = preferences.getBoolean("show_dynamic_tags",false);
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		ListItem item = getItem(position);
		if (view == null) {
			view = inflater.inflate(R.layout.contact, parent, false);
		}
		TextView tvName = (TextView) view.findViewById(R.id.contact_display_name);
		TextView tvJid = (TextView) view.findViewById(R.id.contact_jid);
		ImageView picture = (ImageView) view.findViewById(R.id.contact_photo);
		FlowLayout tagLayout = (FlowLayout) view.findViewById(R.id.tags);
        if(defaultTextColor == 0){
            defaultTextColor = tvName.getCurrentTextColor();
        }

		List<ListItem.Tag> tags = item.getTags(activity);
		if (tags.size() == 0 || !this.showDynamicTags) {
			tagLayout.setVisibility(View.GONE);
		} else {
			tagLayout.setVisibility(View.VISIBLE);
			tagLayout.removeAllViewsInLayout();
			for(ListItem.Tag tag : tags) {
				TextView tv = (TextView) inflater.inflate(R.layout.list_item_tag,tagLayout,false);
				tv.setText(tag.getName());
				tv.setBackgroundColor(tag.getColor());
				tv.setOnClickListener(this.onTagTvClick);
				tagLayout.addView(tv);
			}
		}
		final String jid = item.getDisplayJid();
		if (jid != null) {
			tvJid.setVisibility(View.VISIBLE);
			tvJid.setText(jid);
		} else {
			tvJid.setVisibility(View.GONE);
		}
		tvName.setText(item.getDisplayName());
		loadAvatar(item,picture);

        if(((Contact)getItem(position)).getIsChecked()) {
            ((CheckBox) view.findViewById(R.id.selected_checkbox)).setChecked(true);
            tvName.setTextColor(0xFF3366BB);
            tvJid.setTextColor(0xFF3366BB);
        }else {
            ((CheckBox) view.findViewById(R.id.selected_checkbox)).setChecked(false);
            tvName.setTextColor(defaultTextColor);
            tvJid.setTextColor(defaultTextColor);

        }

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                return false;
            }
        });

		return view;
	}

	public void setOnTagClickedListener(OnTagClickedListener listener) {
		this.mOnTagClickedListener = listener;
	}

	public interface OnTagClickedListener {
		void onTagClicked(String tag);
	}

	class BitmapWorkerTask extends AsyncTask<ListItem, Void, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;
		private ListItem item = null;

		public BitmapWorkerTask(ImageView imageView) {
			imageViewReference = new WeakReference<>(imageView);
		}

		@Override
		protected Bitmap doInBackground(ListItem... params) {
			return activity.avatarService().get(params[0], activity.getPixel(48), isCancelled());
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null && !isCancelled()) {
				final ImageView imageView = imageViewReference.get();
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
					imageView.setBackgroundColor(0x00000000);
				}
			}
		}
	}

	public void loadAvatar(ListItem item, ImageView imageView) {
		if (cancelPotentialWork(item, imageView)) {
			final Bitmap bm = activity.avatarService().get(item,activity.getPixel(48),true);
			if (bm != null) {
				cancelPotentialWork(item, imageView);
				imageView.setImageBitmap(bm);
				imageView.setBackgroundColor(0x00000000);
			} else {
				imageView.setBackgroundColor(UIHelper.getColorForName(item.getDisplayName()));
				imageView.setImageDrawable(null);
				final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
				final AsyncDrawable asyncDrawable = new AsyncDrawable(activity.getResources(), null, task);
				imageView.setImageDrawable(asyncDrawable);
				try {
					task.execute(item);
				} catch (final RejectedExecutionException ignored) {
				}
			}
		}
	}

	public static boolean cancelPotentialWork(ListItem item, ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null) {
			final ListItem oldItem = bitmapWorkerTask.item;
			if (oldItem == null || item != oldItem) {
				bitmapWorkerTask.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}

	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}

}
