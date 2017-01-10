package micheal65536.tgshoutbox;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends AppCompatActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		((Button) findViewById(R.id.button_login)).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				String username = ((EditText) findViewById(R.id.edittext_username)).getText().toString();
				String password = ((EditText) findViewById(R.id.edittext_password)).getText().toString();

				SharedPreferences.Editor editor = getSharedPreferences("authentication", MODE_PRIVATE).edit();
				editor.putString("username", username);
				editor.putString("password", password);
				editor.commit();

				setResult(RESULT_OK);
				finish();
			}
		});
		setResult(RESULT_CANCELED);
	}
}