package com.isprid.turnchargelk.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.isprid.turnchargelk.R;
import com.isprid.turnchargelk.model.User;
import com.isprid.turnchargelk.util.Constant;
import com.isprid.turnchargelk.util.PrefUtils;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private Button btnLogin;
    private ProgressBar progressbar;
    private PrefUtils pref;
    private TextView txtSignedUp;
    private DatabaseReference userRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setTitle("Login");

        mAuth = FirebaseAuth.getInstance();
        userRoot = FirebaseDatabase.getInstance().getReference().child(Constant.USERS);
        pref = new PrefUtils(LoginActivity.this);


        if (pref.checkFromPrefs(Constant.UUID)
                && pref.checkFromPrefs(Constant.IS_LOGGED_IN)
                && pref.checkFromPrefs(Constant.PHONE)
                && pref.getEncyptedPrefsValue(Constant.UUID) != null
                && pref.getEncyptedPrefsValue(Constant.PHONE) != null
                && pref.getEncyptedPrefsValue(Constant.IS_LOGGED_IN).equals("true")) {
            redirectToHome();
        }

        final EditText edtEmail = findViewById(R.id.edt_email);
        final EditText edtPassword = findViewById(R.id.edt_password);
        btnLogin = findViewById(R.id.btn_login);
        progressbar = findViewById(R.id.login_progress_bar);
        txtSignedUp = findViewById(R.id.txt_link_signed_up);

//        edtEmail.setText("admin@gmail.com");
//        edtPassword.setText("12345678");

        btnLogin.setOnClickListener(v -> {
            String emailAddress = edtEmail.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();

            if (TextUtils.isEmpty(emailAddress)) {
                edtEmail.setError("Please fill email fields");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                edtEmail.setError("Please fill password fields");
                return;
            }

            showProgressBar(true);
            mAuth.signInWithEmailAndPassword(emailAddress, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            getUserDetails();
                        } else {
                            showProgressBar(false);
                            Toast.makeText(getApplicationContext(), "E-mail or password is wrong: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        txtSignedUp.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), RegisterActivity.class)));

    }

    private void getUserDetails() {
        String uuid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        userRoot.child(uuid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                if (user != null && user.getEmail() != null) {
                    pref.saveEncryptedPrefsValue(Constant.IS_LOGGED_IN, "true");
                    pref.saveEncryptedPrefsValue(Constant.UUID, user.getUid());
                    pref.saveEncryptedPrefsValue(Constant.EMAIL, user.getEmail());
                    pref.saveEncryptedPrefsValue(Constant.PHONE, user.getPhone());

                    Toast.makeText(getApplicationContext(), "Login Successful", Toast.LENGTH_SHORT).show();

                    redirectToHome();
                } else {
                    showProgressBar(false);
                    Toast.makeText(LoginActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                }
            }


            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                showProgressBar(false);
            }
        });
    }

    private void showProgressBar(Boolean show) {
        btnLogin.setVisibility((show) ? View.GONE : View.VISIBLE);
        progressbar.setVisibility((show) ? View.VISIBLE : View.GONE);
    }

    private void redirectToHome() {
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }
}
