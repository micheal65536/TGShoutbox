package micheal65536.tgshoutbox.Shoutbox;

import android.os.Parcel;
import android.os.Parcelable;

public class Message implements Parcelable
{
	public static Creator<Message> CREATOR = new Creator<Message>()
	{
		@Override
		public Message createFromParcel(Parcel parcel)
		{
			return new Message(parcel.readLong(), parcel.readLong(), parcel.readInt() == 1, parcel.readLong(), parcel.readString(), parcel.readInt() == 1, parcel.readString(), parcel.readInt() == 1);
		}

		@Override
		public Message[] newArray(int i)
		{
			return new Message[0];
		}
	};

	private long id;
	private long senderId;
	private boolean isPrivate;
	private long recipientId;
	private String time;
	private boolean isAction;
	private String content;
	private boolean isDeleted;

	public Message(long id, long senderId, boolean isPrivate, long recipientId, String time, boolean isAction, String content, boolean isDeleted)
	{
		this.id = id;
		this.senderId = senderId;
		this.isPrivate = isPrivate;
		this.recipientId = recipientId;
		this.time = time;
		this.isAction = isAction;
		this.content = content;
		this.isDeleted = isDeleted;
	}

	public long getId()
	{
		return this.id;
	}

	public long getSenderId()
	{
		return this.senderId;
	}

	public boolean getIsPrivate()
	{
		return this.isPrivate;
	}

	public long getRecipientId()
	{
		if (this.isPrivate)
		{
			return this.recipientId;
		}
		else
		{
			return -1;
		}
	}

	public String getTime()
	{
		return this.time;
	}

	public boolean getIsAction()
	{
		return this.isAction;
	}

	public String getContent()
	{
		return this.content;
	}

	public boolean getIsDeleted()
	{
		return this.isDeleted;
	}

	public boolean containsLinks()
	{
		return this.content.matches(".*<a href=\"[^\"]*\">.*");
	}

	@Override
	public int describeContents()
	{
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i)
	{
		parcel.writeLong(this.id);
		parcel.writeLong(this.senderId);
		parcel.writeInt(this.isPrivate ? 1 : 0);
		parcel.writeLong(this.recipientId);
		parcel.writeString(this.time);
		parcel.writeInt(this.isAction ? 1 : 0);
		parcel.writeString(this.content);
		parcel.writeInt(this.isDeleted ? 1 : 0);
	}
}