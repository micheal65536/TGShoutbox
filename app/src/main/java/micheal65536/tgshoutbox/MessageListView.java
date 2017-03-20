package micheal65536.tgshoutbox;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ListView;

public class MessageListView extends ListView
{
	public MessageListView(Context context)
	{
		super(context);
	}

	public MessageListView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}

	public MessageListView(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh)
	{
		int last_item = getLastVisiblePosition();
		int item_height = 0;
		if (last_item != -1)
		{
			item_height = getChildAt(last_item - getFirstVisiblePosition()).getHeight();
		}
		super.onSizeChanged(w, h, oldw, oldh);
		if (last_item != -1)
		{
			setSelectionFromTop(last_item, h - item_height);
		}
	}

	public void scrollToMessage(final int index)
	{
		final int h = getHeight();
		setSelectionFromTop(index, h);
		post(new Runnable()
		{
			@Override
			public void run()
			{
				int item_height = getChildAt(index - getFirstVisiblePosition()).getHeight();
				setSelectionFromTop(index, h - item_height);
			}
		});
	}
}