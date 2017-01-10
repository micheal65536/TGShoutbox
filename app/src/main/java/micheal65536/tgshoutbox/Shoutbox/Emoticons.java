package micheal65536.tgshoutbox.Shoutbox;

import android.graphics.drawable.Drawable;
import android.text.Html;
import android.widget.TextView;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import pl.droidsonroids.gif.GifDrawable;

public class Emoticons
{
	private static HashMap<String, byte[]> cachedEmoticons = new HashMap<>();
	private Session mSession;

	public Emoticons(Session session)
	{
		this.mSession = session;
	}

	protected void prefetchEmoticon(String url)
	{
		fetch_emoticon(url);
	}

	private boolean fetch_emoticon(String url)
	{
		try
		{
			Emoticons.cachedEmoticons.put(url, this.mSession.getUrl(new URL(url)));
			return true;
		}
		catch (Exception exception)
		{
			return false;
		}
	}

	public class EmoticonGetter implements Html.ImageGetter
	{
		private TextView view;

		public EmoticonGetter(TextView view)
		{
			this.view = view;
		}

		@Override
		public Drawable getDrawable(String s)
		{
			byte[] data;
			if (Emoticons.cachedEmoticons.containsKey(s))
			{
				data = Emoticons.cachedEmoticons.get(s);
			}
			else
			{
				fetch_emoticon(s);
				data = Emoticons.cachedEmoticons.get(s);
			}

			try
			{
				emoticonDrawable drawable = new emoticonDrawable(data);
				drawable.setCallbackAndKeepReference(new emoticonDrawableCallback(this.view));
				drawable.start();
				drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
				return drawable;
			}
			catch (Exception exception)
			{
				return null;
			}
		}
	}

	private class emoticonDrawable extends GifDrawable
	{
		private Callback callback;

		private emoticonDrawable(byte[] bytes) throws IOException
		{
			super(bytes);
		}

		public void setCallbackAndKeepReference(Callback cb)
		{
			super.setCallback(cb);
			callback = cb;
		}
	}

	private class emoticonDrawableCallback implements Drawable.Callback
	{
		private TextView view;

		private emoticonDrawableCallback(TextView view)
		{
			this.view = view;
		}

		@Override
		public void invalidateDrawable(Drawable drawable)
		{
			this.view.invalidate();
		}

		@Override
		public void scheduleDrawable(Drawable drawable, Runnable runnable, long l)
		{
			this.view.postDelayed(runnable, l);
		}

		@Override
		public void unscheduleDrawable(Drawable drawable, Runnable runnable)
		{
			this.view.removeCallbacks(runnable);
		}
	}
}