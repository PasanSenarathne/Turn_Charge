package com.isprid.turnchargelk.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.isprid.turnchargelk.R;
import com.isprid.turnchargelk.model.User;
import com.isprid.turnchargelk.util.Constant;
import com.isprid.turnchargelk.util.PrefUtils;

import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = RegisterActivity.class.getSimpleName();
    private PrefUtils pref;
    private DatabaseReference databaseUser;
    private EditText edtFname, edtEmail;
    private EditText edtVehicleNo, edtPhone;
    private EditText edtPassword, edtConfirmPassword;
    private FirebaseAuth mAuth;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        setTitle("Register");

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        pref = new PrefUtils(this);

        mAuth = FirebaseAuth.getInstance();
        databaseUser = FirebaseDatabase.getInstance().getReference(Constant.USERS);

        edtFname = findViewById(R.id.edtFname);
        edtEmail = findViewById(R.id.edtEmail);
        edtPhone = findViewById(R.id.edtPhone);
        edtVehicleNo = findViewById(R.id.edtVehicleNo);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        Button signUpBtn = findViewById(R.id.signUpBtn);
        TextView alreadyUser = findViewById(R.id.already_user);

//        edtFname.setText("Admin");
//        edtEmail.setText("admin@gmail.com");
//        edtPhone.setText("0745236547");
//        edtVehicleNo.setText("CBA-1234");
//        edtPassword.setText("12345678");
//        edtConfirmPassword.setText("12345678");

        signUpBtn.setOnClickListener(view -> {
            String fullName = edtFname.getText().toString().trim();
            String emailAddress = edtEmail.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String vehicle_no = edtVehicleNo.getText().toString().trim();
            String password = edtPassword.getText().toString().trim();
            String confirmPassword = edtConfirmPassword.getText().toString().trim();

            if (password.length() < 6) {
                Toast.makeText(RegisterActivity.this, "Password should have minimum 6 characters", Toast.LENGTH_LONG).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(RegisterActivity.this, "Passwords do ot matched", Toast.LENGTH_LONG).show();
                return;
            }

            if (TextUtils.isEmpty(fullName) || TextUtils.isEmpty(emailAddress) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(password)) {
                Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_LONG).show();
                return;
            }

            User newUser = new User(fullName, emailAddress, phone, vehicle_no, password);
            registerUser(newUser);
            dialog = new ProgressDialog(RegisterActivity.this);
            dialog.setMessage("Please wait...");
            dialog.show();
        });

        alreadyUser.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
            finish();
        });
    }


    private void registerUser(final User user) {
        mAuth.createUserWithEmailAndPassword(user.getEmail(), user.getPassword()).addOnCompleteListener(RegisterActivity.this, task -> {

            if (task.isSuccessful()) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                // Sign in success, update UI with the signed-in authCurrentUser's information
                Log.d(TAG, "createUserWithEmail:success");
                FirebaseUser authCurrentUser = mAuth.getCurrentUser();

                try {
                    if (authCurrentUser != null) {
                        user.setUid(authCurrentUser.getUid());
                        user.setPassword(null);

                        saveUserDetails(user);
                    } else {
                        Toast.makeText(RegisterActivity.this, "Something went wrong.", Toast.LENGTH_SHORT).show();
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                // If sign in fails, display a message to the user.
                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                Toast.makeText(RegisterActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();

                if (task.getException() != null) {
                    Log.e(TAG, task.getException().getMessage());
                    Toast.makeText(RegisterActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }

            }

        });
    }

    private void saveUserDetails(User user) {
        //getting a unique id using push().getKey() method
        //it will create a unique id and we will use it as the Primary Key for row
        //Saving the record
        databaseUser.child(user.getUid()).setValue(user);

        pref.saveEncryptedPrefsValue(Constant.UUID, user.getUid());

        //setting edit text to blank again
        clearFields();

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

        //displaying a success toast
        Toast.makeText(this, "User Successfully registered", Toast.LENGTH_LONG).show();
        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
        finish();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    private void clearFields() {
        edtFname.setText("");
        edtEmail.setText("");
        edtPassword.setText("");
        edtConfirmPassword.setText("");
    }
}
