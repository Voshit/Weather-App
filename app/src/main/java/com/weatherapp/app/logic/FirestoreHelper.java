package com.weatherapp.app.logic;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.weatherapp.app.model.User;

public class FirestoreHelper {

    private static final String USERS_COLLECTION = "users";
    private final FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onFailure(Exception e);
    }

    public void saveUser(User user, final OnSuccessListener<Void> onSuccess, final OnFailureListener onFailure) {
        db.collection(USERS_COLLECTION).document(user.getUid())
                .set(user)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void getUser(String uid, final UserCallback callback) {
        db.collection(USERS_COLLECTION).document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure(new Exception("User not found"));
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
    }

    public void updatePreference(String uid, String field, Object value, final OnSuccessListener<Void> onSuccess, final OnFailureListener onFailure) {
        db.collection(USERS_COLLECTION).document(uid)
                .update(field, value)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}
