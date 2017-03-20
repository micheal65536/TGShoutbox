package micheal65536.tgshoutbox;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import micheal65536.tgshoutbox.Shoutbox.Emoticons;
import micheal65536.tgshoutbox.Shoutbox.Members;
import micheal65536.tgshoutbox.Shoutbox.Message;
import micheal65536.tgshoutbox.Shoutbox.Session;
import micheal65536.tgshoutbox.Shoutbox.UpdateReceiver;

public class MainActivity extends AppCompatActivity implements UpdateReceiver
{
	private static final int REQUEST_CODE_LOGIN = 1;

	private messageAdapter mAdapter;
	private Session mSession;
	private Members mMembers;
	private Emoticons mEmoticons;
	private Thread mUpdateThread;
	private boolean currentIsAction = false;
	private long currentPMRecipient = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.mAdapter = new messageAdapter(this);
		final MessageListView messages = ((MessageListView) findViewById(R.id.listview_messages));
		messages.setAdapter(this.mAdapter);
		messages.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
			{
				Message message = MainActivity.this.mAdapter.getItem(i);
				MainActivity.this.set_pm_from_message(message);
			}
		});
		messages.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
		{
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l)
			{
				Message message = MainActivity.this.mAdapter.getItem(i);
				new AsyncTask<Message, Void, String[]>()
				{
					@Override
					protected String[] doInBackground(Message... messages)
					{
						long sender_id = messages[0].getSenderId();
						return new String[]{MainActivity.this.mMembers.getName(sender_id), MainActivity.this.mMembers.getTheriotype(sender_id)};
					}

					@Override
					protected void onPostExecute(String[] strings)
					{
						AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
						builder.setTitle(strings[0]);
						builder.setMessage(strings[1]);
						builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialogInterface, int i)
							{
								dialogInterface.dismiss();
							}
						});
						builder.create().show();
					}
				}.execute(message);
				return true;
			}
		});

		findViewById(R.id.button_chooserecipient).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				if (MainActivity.this.currentPMRecipient != -1)
				{
					MainActivity.this.set_pm(-1, true);
				}
				else
				{
					AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					View dialog_view = ((LayoutInflater) builder.getContext().getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_choose_recipient, null);
					final EditText recipient = (EditText) dialog_view.findViewById(R.id.edittext_recipient);
					builder.setTitle(R.string.choose_recipient);
					builder.setView(dialog_view);
					builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							String recipient_name = recipient.getText().toString();
							new AsyncTask<String, Void, Long>()
							{
								@Override
								protected Long doInBackground(String... strings)
								{
									return MainActivity.this.mMembers.getId(strings[0]);
								}

								@Override
								protected void onPostExecute(Long aLong)
								{
									if (aLong != -1)
									{
										MainActivity.this.set_pm(aLong, true);
									}
									else
									{
										Toast.makeText(MainActivity.this, R.string.user_not_found, Toast.LENGTH_SHORT).show();
									}
								}
							}.execute(recipient_name);
							dialogInterface.dismiss();
						}
					});
					builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							dialogInterface.cancel();
						}
					});
					AlertDialog dialog = builder.create();
					dialog.show();
				}
			}
		});
		findViewById(R.id.button_action).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				MainActivity.this.set_action(!MainActivity.this.currentIsAction);
			}
		});
		findViewById(R.id.button_send).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(final View view)
			{
				view.setEnabled(false);
				new AsyncTask<Void, Void, Boolean>()
				{
					@Override
					protected Boolean doInBackground(Void... voids)
					{
						String content = ((EditText) MainActivity.this.findViewById(R.id.edittext_message)).getText().toString();
						boolean result;
						if (MainActivity.this.currentPMRecipient != -1)
						{
							result = MainActivity.this.mSession.postMessage(content, MainActivity.this.currentPMRecipient);
						}
						else
						{
							result = MainActivity.this.mSession.postMessage(content, MainActivity.this.currentIsAction);
						}
						return result;
					}

					@Override
					protected void onPostExecute(Boolean aBoolean)
					{
						view.setEnabled(true);
						if (aBoolean)
						{
							MainActivity.this.set_action(false);
							MainActivity.this.set_pm(-1, true);
							((EditText) MainActivity.this.findViewById(R.id.edittext_message)).setText("");
							Toast.makeText(MainActivity.this, R.string.sent, Toast.LENGTH_SHORT).show();
						}
						else
						{
							Toast.makeText(MainActivity.this, R.string.send_failed, Toast.LENGTH_SHORT).show();
						}
					}
				}.execute();
			}
		});
		((EditText) findViewById(R.id.edittext_message)).setOnEditorActionListener(new TextView.OnEditorActionListener()
		{
			@Override
			public boolean onEditorAction(TextView view, int i, KeyEvent keyEvent)
			{
				if (i == EditorInfo.IME_ACTION_SEND)
				{
					MainActivity.this.findViewById(R.id.button_send).callOnClick();
					return true;
				}
				else
				{
					return false;
				}
			}
		});

		if (savedInstanceState == null)
		{
			this.mSession = new Session();
		}
		else
		{
			this.mSession = savedInstanceState.getParcelable("session");
		}
		this.mMembers = new Members(this.mSession);
		this.mEmoticons = new Emoticons(this.mSession);
		if (!this.mSession.isAuthenticated())
		{
			authenticate();
		}

		if (savedInstanceState != null)
		{
			for (Message message : (Message[]) savedInstanceState.getParcelableArray("messages"))
			{
				this.mAdapter.add(message);
			}
			this.mAdapter.notifyDataSetChanged();
			final int last_visible_message = savedInstanceState.getInt("lastVisibleMessage");
			messages.post(new Runnable()
			{
				@Override
				public void run()
				{
					messages.scrollToMessage(last_visible_message);
				}
			});

			set_action(savedInstanceState.getBoolean("isAction"));
			set_pm(savedInstanceState.getLong("PMRecipient"), false);
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		this.mSession.startListening(this);
		this.mUpdateThread = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while (true)
				{
					MainActivity.this.mSession.update(false);
					try
					{
						Thread.sleep(15000);
					}
					catch (InterruptedException exception)
					{
						break;
					}
				}
			}
		});
		this.mUpdateThread.start();
	}

	@Override
	protected void onStop()
	{
		super.onStop();

		this.mUpdateThread.interrupt();
		this.mSession.stopListening(this);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK && (this.currentIsAction || this.currentPMRecipient != -1))
		{
			if (this.currentIsAction)
			{
				set_action(false);
			}
			else
			{
				set_pm(-1, true);
			}
			return true;
		}
		else
		{
			return super.onKeyDown(keyCode, event);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putParcelable("session", this.mSession);

		Message[] messages = new Message[this.mAdapter.getCount()];
		for (int index = 0; index < this.mAdapter.getCount(); index++)
		{
			messages[index] = this.mAdapter.getItem(index);
		}
		outState.putParcelableArray("messages", messages);
		outState.putInt("lastVisibleMessage", ((ListView) findViewById(R.id.listview_messages)).getLastVisiblePosition());

		outState.putBoolean("isAction", this.currentIsAction);
		outState.putLong("PMRecipient", this.currentPMRecipient);

		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == MainActivity.REQUEST_CODE_LOGIN)
		{
			if (resultCode == RESULT_OK)
			{
				authenticate();
			}
			else
			{
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menuitem_logout:
				SharedPreferences.Editor editor = getSharedPreferences("authentication", MODE_PRIVATE).edit();
				editor.remove("username");
				editor.remove("password");
				editor.commit();
				finish();
				return true;
			case R.id.menuitem_refresh:
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.refresh);
				builder.setMessage(R.string.refresh_message);
				builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialogInterface, int i)
					{
						MainActivity.this.mAdapter.clear();
						MainActivity.this.mAdapter.notifyDataSetChanged();
						MainActivity.this.mSession.update(true);
						dialogInterface.dismiss();
					}
				});
				builder.create().show();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onBeginShoutboxUpdate()
	{
	}

	@Override
	public void onNewMessage(final Message message)
	{
		MainActivity.this.mAdapter.add(message);
	}

	@Override
	public void onEndShoutboxUpdate()
	{
		while (MainActivity.this.mAdapter.getCount() > 30)
		{
			MainActivity.this.mAdapter.remove(MainActivity.this.mAdapter.getItem(0));
		}
		MainActivity.this.mAdapter.notifyDataSetChanged();
	}

	private void authenticate()
	{
		SharedPreferences preferences = getSharedPreferences("authentication", MODE_PRIVATE);
		new AsyncTask<String, Void, Boolean>()
		{
			@Override
			protected Boolean doInBackground(String... strings)
			{
				String username = strings[0];
				String password = strings[1];
				if (username.equals("") || password.equals(""))
				{
					return false;
				}
				return MainActivity.this.mSession.authenticate(username, password);
			}

			@Override
			protected void onPostExecute(Boolean aBoolean)
			{
				if (aBoolean)
				{
					MainActivity.this.mSession.startListening(MainActivity.this);
					MainActivity.this.mSession.update(true);
				}
				else
				{
					login();
				}
			}
		}.execute(preferences.getString("username", ""), preferences.getString("password", ""));
	}

	private void login()
	{
		Intent intent = new Intent(this, LoginActivity.class);
		startActivityForResult(intent, MainActivity.REQUEST_CODE_LOGIN);
	}

	private void set_action(boolean action)
	{
		if (this.currentPMRecipient == -1)
		{
			if (action && !this.currentIsAction)
			{
				findViewById(R.id.button_action).setBackground(getResources().getDrawable(R.drawable.button_message));
				((Button) findViewById(R.id.button_action)).setText("");
				findViewById(R.id.button_action).setContentDescription(getResources().getString(R.string.message));
			}
			else if (!action && this.currentIsAction)
			{
				findViewById(R.id.button_action).setBackground(getResources().getDrawable(R.drawable.imagebutton_background));
				((Button) findViewById(R.id.button_action)).setText(R.string.action);
				findViewById(R.id.button_action).setContentDescription(getResources().getString(R.string.action));
			}
			this.currentIsAction = action;
		}
	}

	private void set_pm_from_message(Message message)
	{
		if (message.getIsPrivate() && message.getSenderId() == MainActivity.this.mSession.getUid())
		{
			MainActivity.this.set_pm(message.getRecipientId(), true);
		}
		else
		{
			MainActivity.this.set_pm(message.getSenderId(), true);
		}
	}

	@TargetApi(19)
	private void set_pm(long id, boolean animate)
	{
		if (id != -1)
		{
			if (this.currentPMRecipient == -1)
			{
				if (animate && Build.VERSION.SDK_INT >= 19)
				{
					Transition transition = new AutoTransition();
					transition.setDuration(100);
					TransitionManager.beginDelayedTransition((ViewGroup) findViewById(R.id.linearlayout_message_compose), transition);
				}

				set_action(false);
				findViewById(R.id.button_action).setVisibility(View.GONE);

				findViewById(R.id.linearlayout_message_compose).setBackgroundColor(getResources().getColor(R.color.message_private));
				findViewById(R.id.linearlayout_message_compose_private).setVisibility(View.VISIBLE);
				((ImageButton) findViewById(R.id.button_chooserecipient)).setImageDrawable(getResources().getDrawable(R.drawable.ic_clear));
				findViewById(R.id.button_chooserecipient).setContentDescription(getResources().getString(R.string.cancel_private_shout));
			}
			this.currentPMRecipient = id;
			TextView recipient = (TextView) findViewById(R.id.textview_message_compose_recipient);
			recipient.setText(this.mMembers.getName(id));
			this.mMembers.getStyle(id).applyToTextView(recipient);
		}
		else
		{
			if (this.currentPMRecipient != -1)
			{
				if (animate && Build.VERSION.SDK_INT >= 19)
				{
					Transition transition = new AutoTransition();
					transition.setDuration(100);
					TransitionManager.beginDelayedTransition((ViewGroup) findViewById(R.id.linearlayout_message_compose), transition);
				}

				((ImageButton) findViewById(R.id.button_chooserecipient)).setImageDrawable(getResources().getDrawable(R.drawable.ic_person));
				findViewById(R.id.button_chooserecipient).setContentDescription(getResources().getString(R.string.choose_recipient));
				findViewById(R.id.linearlayout_message_compose_private).setVisibility(View.GONE);
				findViewById(R.id.linearlayout_message_compose).setBackgroundColor(getResources().getColor(R.color.message_compose));

				findViewById(R.id.button_action).setVisibility(View.VISIBLE);

				this.currentPMRecipient = -1;
			}
		}
	}

	private class messageAdapter extends ArrayAdapter<Message>
	{
		private Context mContext;

		private messageAdapter(Context context)
		{
			super(context, R.layout.listitem_message);
			this.mContext = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view;
			if (convertView != null)
			{
				view = convertView;
			}
			else
			{
				view = View.inflate(this.mContext, R.layout.listitem_message, null);
			}
			Message message = this.getItem(position);
			TextView sender = ((TextView) view.findViewById(R.id.textview_message_sender));
			TextView recipient = ((TextView) view.findViewById(R.id.textview_message_recipient));
			TextView time = ((TextView) view.findViewById(R.id.textview_message_time));
			TextView content = ((TextView) view.findViewById(R.id.textview_message_content));

			if (message.getIsDeleted())
			{
				view.findViewById(R.id.imageview_message_deleted).setVisibility(View.VISIBLE);
			}
			else
			{
				view.findViewById(R.id.imageview_message_deleted).setVisibility(View.GONE);
			}
			sender.setText(MainActivity.this.mMembers.getName(message.getSenderId()));
			MainActivity.this.mMembers.getStyle(message.getSenderId()).applyToTextView(sender);
			if (message.getIsPrivate() && message.getSenderId() == MainActivity.this.mSession.getUid())
			{
				recipient.setText(MainActivity.this.mMembers.getName(message.getRecipientId()));
				MainActivity.this.mMembers.getStyle(message.getRecipientId()).applyToTextView(recipient);
				view.findViewById(R.id.linearlayout_message_private).setVisibility(View.VISIBLE);
			}
			else
			{
				view.findViewById(R.id.linearlayout_message_private).setVisibility(View.INVISIBLE);
			}
			if (message.getIsPrivate())
			{
				view.setBackgroundColor(getResources().getColor(R.color.message_private));
			}
			else
			{
				view.setBackgroundColor(Color.TRANSPARENT);
			}
			time.setText(message.getTime());
			content.setText(Html.fromHtml(message.getContent(), MainActivity.this.mEmoticons.new EmoticonGetter((TextView) view.findViewById(R.id.textview_message_content)), null));
			if (message.getIsAction())
			{
				content.setTextColor(getResources().getColor(R.color.message_action));
			}
			else
			{
				content.setTextColor(getResources().getColor(R.color.message_default));
			}
			if (message.containsLinks())
			{
				content.setMovementMethod(LinkMovementMethod.getInstance());
			}
			else
			{
				content.setMovementMethod(null);
			}
			return view;
		}
	}
}