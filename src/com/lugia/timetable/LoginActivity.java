/*
 * Copyright (c) 2014 Lugia Programming Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.lugia.timetable;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Activity that allow user to enter MMU ID and password to retrieve their course data.
 */
public class LoginActivity extends Activity implements OnClickListener, TextWatcher
{
    private Button mContinueButton;
    
    private EditText mMmuIdInput;
    private EditText mPasswordInput;
    
    private ProgressDialog mProgressDialog;
    
    public static final String LOGIN_URL           = "https://icems.mmu.edu.my/sic/vlogin.jsp";
    public static final String COURSE_URL          = "https://icems.mmu.edu.my/sic/courses/crdetails_02.jsp";
    public static final String LOGIN_ID_NAME       = "id";
    public static final String LOGIN_PASSWORD_NAME = "pwd";
    
    private static final String TAG = "LoginActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        mMmuIdInput = (EditText)findViewById(R.id.input_mmu_id);
        mMmuIdInput.addTextChangedListener(LoginActivity.this);
        
        mPasswordInput = (EditText)findViewById(R.id.input_password);
        mPasswordInput.addTextChangedListener(LoginActivity.this);
        
        mContinueButton = (Button)findViewById(R.id.button_continue);
        mContinueButton.setOnClickListener(this);
    }

    public void onClick(View v)
    {
        // not continue button? i really not sure what you have clicked.
        if (v.getId() != R.id.button_continue)
            return;
        
        String mmuid = mMmuIdInput.getText().toString();
        String password = mPasswordInput.getText().toString();
        
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
        
        mProgressDialog = ProgressDialog.show(LoginActivity.this, "", "Please wait...", true, false);
        mProgressDialog.setCancelable(false);
        
        LoginThread thread = new LoginThread(mmuid, password);
        thread.start();
    }
    
    public void afterTextChanged(Editable s)
    {
        // only enable Continue button when both input is not empty
        if (mMmuIdInput.getText().length() > 0 && mPasswordInput.getText().length() > 0)
            mContinueButton.setEnabled(true);
        else
            mContinueButton.setEnabled(false);
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Do Nothing */ }
    public void onTextChanged(CharSequence s, int start, int before, int count) { /* Do Nothing */ }
    
    private void setProgressMessage(final CharSequence message)
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                mProgressDialog.setMessage(message);
            }
        });
    }
    
    private void dismissProgressDialog()
    {
        runOnUiThread(new Runnable()
        {
            public void run() { mProgressDialog.dismiss(); }
        });
    }
    
    private void displayLoginFailDialog()
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                // notify user
                new AlertDialog.Builder(LoginActivity.this)
                               .setTitle("Opps!")
                               .setMessage("Fail to download course data, please make sure you have entered a correct MMU ID and password.")
                               .setPositiveButton("Close", new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        
                                    }
                                })
                               .create()
                               .show();

            }
        });
    }
    
    private void showSuccessToast()
    {
        runOnUiThread(new Runnable()
        {
            public void run()
            {
                Toast.makeText(LoginActivity.this, "Course detail downloaded successfully.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private final class LoginThread extends Thread
    {
        private String mMmuId;
        private String mPassword;
        
        public LoginThread(String mmuId, String password)
        {
            mMmuId = mmuId;
            mPassword = password;
        }
        
        public void run()
        {
            try
            {
                SSLHttpClient client = SSLHttpClient.getHttpClient();
                
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair(LOGIN_ID_NAME,       mMmuId   ));
                nameValuePairs.add(new BasicNameValuePair(LOGIN_PASSWORD_NAME, mPassword));
                
                HttpPost loginPost = new HttpPost(LOGIN_URL);
                loginPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                
                setProgressMessage("Logging in...");        
                
                // try to login
                client.executeResponse(loginPost);
                
                setProgressMessage("Getting course detail...");
                
                HttpGet courseGet = new HttpGet(COURSE_URL);
                HttpResponse response = client.executeResponse(courseGet);
                
                // try to get the course detail
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
                {
                    Log.d(TAG, String.format("HTTP Status: %d", response.getStatusLine().getStatusCode()));
                    return;
                }
                
                HttpEntity entity = response.getEntity();
                
                if (entity == null)
                {
                    Log.d(TAG, "HTTP Entity is null!");
                    return;
                }
                
                setProgressMessage("Reading course detail...");
                
                String content = EntityUtils.toString(entity);
                
                ArrayList<Subject> temp = CourseParser.tryParse(content);
                
                dismissProgressDialog();
                
                // subject list is null? there must be a problem
                if (temp == null)
                {
                    displayLoginFailDialog();
                    return;
                }

                SubjectList subjectList = SubjectList.getInstance(LoginActivity.this);
                subjectList.replace(temp);
                
                // for debug purpose
                subjectList.displaySubjectListContent();
                
                subjectList.saveToFile(LoginActivity.this);
                
                showSuccessToast();
                
                setResult(RESULT_OK);
                
                finish();
            }
            catch (Exception e)
            {
                // Something went wrong
                Log.e(TAG, "Error on fetching data!", e);
            }
        }
    }
}
