package com.example.logisticsystem;

import static android.content.ContentValues.TAG;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();

        EditText login = findViewById(R.id.username);
        EditText password = findViewById(R.id.password);

        findViewById(R.id.btnLogin).setOnClickListener(view -> {
            if (login.getText().toString().isEmpty() || password.getText().toString().isEmpty()){
                Toast.makeText(this, "Укажите пароль и логин", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.signInWithEmailAndPassword(login.getText().toString(), password.getText().toString())
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            assert user != null;
                            ((TextView) findViewById(R.id.signAs))
                                    .setText(String.format("Вы вошли как: %s", user.getEmail()));
                            Toast.makeText(getBaseContext(),
                                    String.format("Вы вошли как: %s", user.getEmail()), Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(this, "Неверный пароль или логин.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        findViewById(R.id.btnBack).setOnClickListener(view ->
                startActivity(new Intent(this, MainActivity.class)));

        findViewById(R.id.btnSignOut).setOnClickListener(view -> {
            mAuth.signOut();
            ((TextView) findViewById(R.id.signAs)).setText("Вы не авторизованы");
            Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null){
            ((TextView) findViewById(R.id.signAs)).setText(String.format("Вы вошли как: %s", user.getEmail()));
        }
    }
}