package micheal65536.tgshoutbox.Shoutbox;

import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

public class Members
{
	private static HashMap<String, Long> mIds = new HashMap<>();
	private static HashMap<Long, String> mNames = new HashMap<>();
	private static HashMap<Long, Style> mStyles = new HashMap<>();
	private static HashMap<Long, String> mTheriotypes = new HashMap<>();
	private Session mSession;

	public Members(Session session)
	{
		this.mSession = session;
	}

	public long getId(String name)
	{
		if (name.equals(""))
		{
			return -1;
		}

		if (Members.mIds.containsKey(name))
		{
			return Members.mIds.get(name);
		}
		else
		{
			try
			{
				Document document = Jsoup.parse(get_profile(name));
				Element link_element = document.body().getElementById("profile-container").select("#profile-container > table:nth-of-type(2) td:first-of-type > table:first-of-type span.smalltext:first-of-type > a").get(0);
				long id = -1;
				for (String parameter : link_element.attributes().get("href").split("\\?")[1].split("&"))
				{
					if (parameter.split("=")[0].equals("uid"))
					{
						id = Long.parseLong(parameter.split("=")[1]);
					}
				}

				if (id == -1)
				{
					return -1;
				}
				else
				{
					Element name_element = document.body().getElementById("profile-container").select("#profile-container > table:first-of-type table:first-of-type span.largetext > strong > span").get(0);
					addNameFromHtml(id, name_element.outerHtml());
					return id;
				}
			}
			catch (Exception exception)
			{
				return -1;
			}
		}
	}

	public String getName(long id)
	{
		if (id == -1)
		{
			return "";
		}

		if (!get_info(id))
		{
			return "";
		}
		else
		{
			return Members.mNames.get(id);
		}
	}

	public Style getStyle(long id)
	{
		if (id == -1)
		{
			return new Style(Color.BLACK, false, false);
		}

		if (!get_info(id))
		{
			return new Style(Color.BLACK, false, false);
		}
		else
		{
			return Members.mStyles.get(id);
		}
	}

	public String getTheriotype(long id)
	{
		if (id == -1)
		{
			return "";
		}

		if (!get_theriotype(id))
		{
			return "";
		}
		else
		{
			return Members.mTheriotypes.get(id);
		}
	}

	protected void addNameFromHtml(long id, String html)
	{
		Document document = Jsoup.parseBodyFragment(html);
		Element element = document.body().children().get(0);
		String name = element.text();
		Style style = new Style(element.outerHtml());

		Members.mIds.put(name, id);
		Members.mNames.put(id, name);
		Members.mStyles.put(id, style);
	}

	private boolean get_info(long id)
	{
		if (!Members.mNames.containsKey(id) || !Members.mStyles.containsKey(id))
		{
			try
			{
				Document document = Jsoup.parse(get_profile(id));
				Element name_element = document.body().getElementById("profile-container").select("#profile-container > table:first-of-type table:first-of-type span.largetext > strong > span").get(0);
				addNameFromHtml(id, name_element.outerHtml());
				return true;
			}
			catch (Exception exception)
			{
				return false;
			}
		}
		else
		{
			return true;
		}
	}

	/**
	 * There's an important reason why theriotypes are cached separately from the other info, and it's because it reduces the number of profile lookups to almost zero.
	 * <p/>
	 * If theriotypes were cached with the other info, then get_info would fail every time there's a missing theriotype and perform a profile lookup to find it.
	 * This means that every time a new user appears in the shoutbox (including all the users that are present when the app is opened) their profile will have to be looked up.
	 * <p/>
	 * As it is, the id, name, and name style info is available during the shoutbox update itself, and the shoutbox update routine adds it here with addNameFromHtml without a profile lookup.
	 * This means that the ONLY time a profile lookup will ever be required is when the user sends a private message to a user that hasn't been seen yet.
	 * <p/>
	 * So please don't change this.
	 */
	private boolean get_theriotype(long id)
	{
		if (!Members.mTheriotypes.containsKey(id))
		{
			try
			{
				Document document = Jsoup.parse(get_profile(id));
				Element theriotype_element = document.body().getElementById("profile-container").select("#profile-container > table:nth-of-type(2) td:nth-of-type(3) > table:first-of-type > tbody > tr:nth-of-type(3) > td:last-of-type").get(0);
				Members.mTheriotypes.put(id, theriotype_element.text());
				return true;
			}
			catch (Exception exception)
			{
				return false;
			}
		}
		else
		{
			return true;
		}
	}

	private String get_profile(long id)
	{
		try
		{
			return new String(this.mSession.getUrl(new URL("https://forums.therian-guide.com/member.php?action=profile&uid=" + Long.toString(id))));
		}
		catch (Exception exception)
		{
			return "";
		}
	}

	private String get_profile(String name)
	{
		try
		{
			return new String(this.mSession.getUrl(new URL("https://forums.therian-guide.com/User-" + URLEncoder.encode(name.replace(" ", "-"), "UTF-8"))));
		}
		catch (Exception exception)
		{
			return "";
		}
	}

	public static class Style
	{
		private int color;
		private boolean bold;
		private boolean italic;

		private Style(int color, boolean bold, boolean italic)
		{
			this.color = color;
			this.bold = bold;
			this.italic = italic;
		}

		private Style(String html)
		{
			this.color = 0;
			this.bold = false;
			this.italic = false;

			Document document = Jsoup.parseBodyFragment(html);
			for (Element element : document.getAllElements())
			{
				if (element.tagName().equals("strong") || element.tagName().equals("b"))
				{
					this.bold = true;
				}
				else if (element.tagName().equals("em") || element.tagName().equals("i"))
				{
					this.italic = true;
				}
				else if (element.tagName().equals("span"))
				{
					if (element.hasAttr("style"))
					{
						String color_string = element.attributes().get("style").replace("color: ", "").replace(";", "");
						this.color = Color.parseColor(color_string);
					}
				}
			}
		}

		public int getColor()
		{
			return this.color;
		}

		public boolean isBold()
		{
			return this.bold;
		}

		public boolean isItalic()
		{
			return this.italic;
		}

		public void applyToTextView(TextView view)
		{
			view.setTextColor(this.color);
			if (this.bold && this.italic)
			{
				view.setTypeface(null, Typeface.BOLD_ITALIC);
			}
			else if (this.bold)
			{
				view.setTypeface(null, Typeface.BOLD);
			}
			else if (this.italic)
			{
				view.setTypeface(null, Typeface.ITALIC);
			}
			else
			{
				view.setTypeface(null, Typeface.NORMAL);
			}
		}
	}
}