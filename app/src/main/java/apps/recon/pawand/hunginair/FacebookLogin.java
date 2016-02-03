package apps.recon.pawand.hunginair;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by pawanD on 1/26/2016.
 */
public class FacebookLogin extends Fragment {

    final String TAG = "LoginScreen     ";
    private LoginButton loginButton;
    private CallbackManager callbackManager;
    private ProfileTracker profileTracker;
    private String userId;
    private String userName;
    private String userEmail;
    private String userGender;
    private String userBirthday;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FacebookSdk.sdkInitialize(this.getActivity());
        callbackManager = CallbackManager.Factory.create();
        View view = inflater.inflate(R.layout.fragment_facebook, container, false);
        loginButton = (LoginButton) view.findViewById(R.id.login_button);
        loginButton.setReadPermissions("public_profile,user_birthday,email");

        loginButton.setFragment(this);
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {
                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                                Log.w(TAG, response.toString());
                                Log.w(TAG, object.toString());
                                try {
                                    userId = object.getString("id");
                                } catch (JSONException e) {
                                    Log.w(TAG, e.toString());
                                }
                                try {
                                    userName = object.getString("name");
                                } catch (JSONException e) {
                                    Log.w(TAG, e.toString());
                                }
                                try {
                                    userEmail = object.getString("email");
                                } catch (JSONException e) {
                                    Log.w(TAG, e.toString());
                                }
                                try {
                                    userGender = object.getString("gender");
                                } catch (JSONException e) {
                                    Log.w(TAG, e.toString());
                                }
                                saveUserInfoParse();
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,birthday,gender");
                request.setParameters(parameters);
                request.executeAsync();
            }
            @Override
            public void onCancel() {
                Log.w(TAG,"Login Cancelled");
            }
            @Override
            public void onError(FacebookException e) {
                Log.w(TAG,e.toString());
            }
        });

        profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                if (oldProfile != null) {
                    Toast.makeText(getActivity(),"log out", Toast.LENGTH_SHORT).show();
                    deleteUserInfoParse();
                }
                if (currentProfile != null) {
                    Toast.makeText(getActivity(),"log in", Toast.LENGTH_SHORT).show();
                }

                // App code
            }
        };
        return view;
    }

    public void saveUserInfoParse(){
        saveUserInfoParseLocal();
        saveUserInfoParseCloud();
    }
    public void saveUserInfoParseLocal(){
        ParseObject object = new ParseObject("UserInfoParse");
        object.put("userId", userId);
        object.put("userName", userName);
        object.put("userEmail", userEmail);
        object.put("userGender", userGender);
        try {
            object.pin();
        } catch (ParseException e) {
            Log.w(TAG, "UserInfoParse failed");
            e.printStackTrace();
        }
    }
    public void saveUserInfoParseCloud(){
        ParseUser user = new ParseUser();
        user.put("userId", userId);
        user.setUsername(userName);
        user.setPassword(userId);
        user.setEmail(userEmail);
        user.put("userGender", userGender);
        user.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    // Hooray! Let them use the app now.
                } else {
                    Log.w(TAG, e.toString());
                    // Sign up didn't succeed. Look at the ParseException to figure out what went wrong
                }
            }
        });
    }

    public void deleteUserInfoParse(){
        deleteUserInfoParseLocal();
    }
    public void deleteUserInfoParseLocal(){
        try {
            ParseObject.unpinAll("UserInfoParse");
        } catch (ParseException e) {
            Log.w(TAG, e.toString());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

}
