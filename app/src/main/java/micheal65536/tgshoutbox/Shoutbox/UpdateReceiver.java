package micheal65536.tgshoutbox.Shoutbox;

public interface UpdateReceiver
{
	public void onBeginShoutboxUpdate();

	public void onNewMessage(Message message);

	public void onEndShoutboxUpdate();
}