package micheal65536.tgshoutbox.Shoutbox;

import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

import javax.net.ssl.HttpsURLConnection;

public class Session implements Parcelable
{
	public static Creator<Session> CREATOR = new Creator<Session>()
	{
		@Override
		public Session createFromParcel(Parcel parcel)
		{
			Session session = new Session();
			session.mUid = parcel.readLong();
			session.mAuthenticated = (parcel.readInt() == 1);
			session.mCookieSID = parcel.readString();
			session.mCookieMYBBUSER = parcel.readString();
			session.mLastId = parcel.readLong();
			return session;
		}

		@Override
		public Session[] newArray(int i)
		{
			return new Session[0];
		}
	};

	private ArrayList<UpdateReceiver> mReceivers = new ArrayList<>();
	private long mUid = -1;
	private boolean mAuthenticated = false;
	private String mCookieSID = "";
	private String mCookieMYBBUSER = "";
	private long mLastId = 0;
	private Semaphore mUpdateLock = new Semaphore(1);

	public boolean authenticate(String username, String password)
	{
		try
		{
			String parameters = "username=" + URLEncoder.encode(username, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8") + "&action=do_login";
			HttpsURLConnection connection = (HttpsURLConnection) new URL("https://forums.therian-guide.com/member.php").openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("User-Agent", "micheal65536.tgshoutbox");
			connection.setRequestProperty("Content-Length", Integer.toString(parameters.length()));
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setDoOutput(true);
			connection.setDoInput(true);

			OutputStream output_stream = connection.getOutputStream();
			output_stream.write(parameters.getBytes());
			output_stream.close();

			List<String> cookies_strings = connection.getHeaderFields().get("Set-Cookie");
			ArrayList<HttpCookie> cookies = new ArrayList<>();
			for (String cookie_string : cookies_strings)
			{
				cookies.addAll(HttpCookie.parse(cookie_string));
			}
			for (HttpCookie cookie : cookies)
			{
				if (cookie.getName().equals("sid"))
				{
					this.mCookieSID = cookie.getValue();
				}
				if (cookie.getName().equals("mybbuser"))
				{
					this.mCookieMYBBUSER = cookie.getValue();
				}
			}

			if (!this.mCookieSID.equals("") && !this.mCookieMYBBUSER.equals(""))
			{
				this.mAuthenticated = true;
				this.mUid = new Members(this).getId(username);
				return true;
			}
			else
			{
				return false;
			}
		}
		catch (Exception exception)
		{
			return false;
		}
	}

	public boolean isAuthenticated()
	{
		return this.mAuthenticated;
	}

	public long getUid()
	{
		return this.mUid;
	}

	public void startListening(UpdateReceiver receiver)
	{
		if (!this.mReceivers.contains(receiver))
		{
			this.mReceivers.add(receiver);
		}
	}

	public void stopListening(UpdateReceiver receiver)
	{
		this.mReceivers.remove(receiver);
	}

	public void update(boolean clear)
	{
		if (clear)
		{
			this.mLastId = 0;
		}

		new AsyncTask<Void, Void, List<Message>>()
		{
			@Override
			protected List<Message> doInBackground(Void... voids)
			{
				Session.this.mUpdateLock.acquireUninterruptibly();

				if (!Session.this.mAuthenticated)
				{
					return null;
				}

				try
				{
					HttpsURLConnection connection = (HttpsURLConnection) new URL("https://forums.therian-guide.com/xmlhttp.php?action=show_shouts&last_id=" + Long.toString(Session.this.mLastId)).openConnection();
					connection.setRequestMethod("GET");
					connection.setRequestProperty("User-Agent", "micheal65536.tgshoutbox");
					connection.setRequestProperty("Cookie", "sid=" + Session.this.mCookieSID + ";mybbuser=" + Session.this.mCookieMYBBUSER);
					connection.setDoOutput(false);
					connection.setDoInput(true);

					InputStream input_stream = connection.getInputStream();
					byte[] data = IOUtils.toByteArray(input_stream);
					input_stream.close();
					String response = new String(data);

					ArrayList<Message> messages = new ArrayList<>();
					Members members = new Members(Session.this);

					String html = response.substring(response.indexOf("<"));
					html = html.substring(0, html.lastIndexOf(">") + 1);
					Document document = Jsoup.parseBodyFragment(html);
					for (Element element : document.body().children())
					{
						if (element.tagName().equals("span"))
						{
							List<Element> elements = element.children();
							long id;
							long sender_id;
							boolean is_private;
							long recipient_id;
							String recipient_name;
							String time;
							boolean is_action;
							String content;
							boolean is_deleted;

							if (elements.get(0).tagName().equals("strong") && elements.get(0).text().equals("DELETED"))
							{
								is_deleted = true;
							}
							else
							{
								is_deleted = false;
							}
							int start_index = 0;
							while (!elements.get(start_index).hasAttr("id"))
							{
								start_index++;
							}

							id = Long.parseLong(elements.get(start_index).attributes().get("id").replace("report_", ""));
							Element sender_tag;
							if (elements.get(start_index + 1).tagName().equals("a"))
							{
								sender_tag = elements.get(start_index + 1);
								is_private = false;
							}
							else
							{
								sender_tag = elements.get(start_index + 1).children().get(0);
								is_private = true;
							}
							sender_id = Long.parseLong(sender_tag.attributes().get("onClick").replace("javascript: ShoutBox.pvtAdd(", "").replace("); return false;", ""));
							members.addNameFromHtml(sender_id, sender_tag.children().get(0).outerHtml());
							if (is_private)
							{
								recipient_name = elements.get(start_index + 2).text().replace("Private Shout to ", "");
								recipient_name = recipient_name.substring(0, recipient_name.indexOf(":"));
								recipient_id = members.getId(recipient_name);
							}
							else
							{
								recipient_name = "";
								recipient_id = -1;
							}
							time = element.textNodes().get(start_index + 2).text().substring(3, 14);
							if (is_private)
							{
								is_action = false;
								content = elements.get(start_index + 2).html().substring(19 + recipient_name.length());
							}
							else
							{
								if (elements.size() >= start_index + 3 && elements.get(start_index + 2).tagName().equals("span") && elements.get(start_index + 2).hasAttr("style") && elements.get(start_index + 2).attributes().get("style").equals("color: red;"))
								{
									is_action = true;
									content = elements.get(start_index + 2).html();
								}
								else
								{
									is_action = false;
									content = element.html();
									content = content.substring(content.indexOf(time) + 15);
								}
							}
							content = prune_content(content);

							messages.add(new Message(id, sender_id, is_private, recipient_id, time, is_action, content, is_deleted));
						}
					}

					return messages;
				}
				catch (Exception exception)
				{
					return null;
				}
			}

			@Override
			protected void onPostExecute(List<Message> messages)
			{
				if (messages != null && messages.size() > 0)
				{
					Session.this.mLastId = messages.get(0).getId();

					Collections.reverse(messages);
					for (UpdateReceiver receiver : Session.this.mReceivers)
					{
						receiver.onBeginShoutboxUpdate();
					}
					for (Message message : messages)
					{
						for (UpdateReceiver receiver : Session.this.mReceivers)
						{
							receiver.onNewMessage(message);
						}
					}
					for (UpdateReceiver receiver : Session.this.mReceivers)
					{
						receiver.onEndShoutboxUpdate();
					}
				}

				Session.this.mUpdateLock.release();
			}
		}.execute();
	}

	public boolean postMessage(String content, boolean isAction)
	{
		if (content.length() == 0)
		{
			return false;
		}
		if (isAction && content.substring(0, 1).equals("/"))
		{
			return false;
		}
		String message;
		if (isAction)
		{
			message = "/me " + content;
		}
		else
		{
			message = content;
		}
		return post_message(message);
	}

	public boolean postMessage(String content, long recipientId)
	{
		if (content.length() == 0)
		{
			return false;
		}
		if (content.substring(0, 1).equals("/"))
		{
			return false;
		}
		String message = "/pvt " + Long.toString(recipientId) + " " + content;
		return post_message(message);
	}

	public byte[] getUrl(URL url)
	{
		if (this.mCookieSID.equals("") || this.mCookieMYBBUSER.equals(""))
		{
			return null;
		}

		return new downloadHelper().download(url);
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i)
	{
		parcel.writeLong(this.mUid);
		parcel.writeInt(this.mAuthenticated ? 1 : 0);
		parcel.writeString(this.mCookieSID);
		parcel.writeString(this.mCookieMYBBUSER);
		parcel.writeLong(this.mLastId);
	}

	private String prune_content(String content)
	{
		StringBuilder result = new StringBuilder();

		Document document = Jsoup.parseBodyFragment(content);
		for (Node node : document.body().childNodes())
		{
			if (node instanceof TextNode)
			{
				result.append(TextUtils.htmlEncode(((TextNode) node).text()));
			}
			else if (node instanceof Element)
			{
				Element element = (Element) node;
				if (element.tagName().equals("span") && element.hasAttr("style"))
				{
					String out_tag;
					switch (element.attributes().get("style"))
					{
						case "font-weight: bold;":
							out_tag = "b";
							break;
						case "font-style: italic;":
							out_tag = "i";
							break;
						case "text-decoration: underline;":
							out_tag = "u";
							break;
						default:
							out_tag = "";
							break;
					}
					if (!out_tag.equals(""))
					{
						result.append("<" + out_tag + ">" + prune_content(element.html()) + "</" + out_tag + ">");
					}
					else
					{
						result.append(prune_content(element.html()));
					}
				}
				else if (element.tagName().equals("del"))    // TODO: not supported by Html.fromHtml
				{
					result.append("<s>" + prune_content(element.html()) + "</s>");
				}
				else if (element.tagName().equals("a") && element.hasAttr("href"))
				{
					result.append("<a href=\"" + element.attributes().get("href") + "\">" + prune_content(element.html()) + "</a>");
				}
				else if (element.tagName().equals("img") && element.hasAttr("src"))
				{
					String src = element.attributes().get("src");
					if (src.substring(0, 15).equals("images/smilies/"))
					{
						src = "https://forums.therian-guide.com/" + src;
						result.append("<img src=\"" + src + "\"/>");
						new Emoticons(this).prefetchEmoticon(src);    // TODO: find a better place for this
					}
				}
				else
				{
					result.append(prune_content(element.html()));
				}
			}
		}

		return result.toString();
	}

	private boolean post_message(String message)
	{
		try
		{
			String parameters = "shout_data=" + URLEncoder.encode(message, "UTF-8");

			HttpsURLConnection connection = (HttpsURLConnection) new URL("https://forums.therian-guide.com/xmlhttp.php?action=add_shout").openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("User-Agent", "micheal65536.tgshoutbox");
			connection.setRequestProperty("Cookie", "sid=" + Session.this.mCookieSID + ";mybbuser=" + Session.this.mCookieMYBBUSER);
			connection.setRequestProperty("Content-Length", Integer.toString(parameters.length()));
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setDoOutput(true);
			connection.setDoInput(true);

			OutputStream output_stream = connection.getOutputStream();
			output_stream.write(parameters.getBytes());
			output_stream.close();

			InputStream input_stream = connection.getInputStream();
			byte[] data = IOUtils.toByteArray(input_stream);
			input_stream.close();

			if (!new String(data).equals("success!!"))
			{
				return false;
			}

			update(false);

			return true;
		}
		catch (Exception exception)
		{
			return false;
		}
	}

	private class downloadHelper
	{
		private volatile byte[] data;

		private byte[] download(final URL url)
		{
			this.data = null;
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
						connection.setRequestMethod("GET");
						connection.setRequestProperty("User-Agent", "micheal65536.tgshoutbox");
						connection.setRequestProperty("Cookie", "sid=" + Session.this.mCookieSID + ";mybbuser=" + Session.this.mCookieMYBBUSER);
						connection.setDoOutput(false);
						connection.setDoInput(true);

						InputStream input_stream = connection.getInputStream();
						downloadHelper.this.data = IOUtils.toByteArray(input_stream);
						input_stream.close();
					}
					catch (Exception exception)
					{
						downloadHelper.this.data = new byte[0];
					}

				}
			}).start();
			while (this.data == null)
			{
			}
			return this.data;
		}
	}
}