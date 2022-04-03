package com.arrayliststudent.qrhunt;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Firebase controller; responsible for getting and setting Firebase data
 *
 * Built using the lab guide at: https://github.com/AbdulAli/FirestoreTutorialW22/
 * Firebase Docs: https://firebase.google.com/docs/guides?authuser=0&hl=en
 * Firebase Samples: https://firebase.google.com/docs/samples?authuser=0&hl=en
 *
 * @author jmgraham
 * @see User
 **/
public class FirebaseData {
    final String TAG = "dunno what to put here";
    FirebaseFirestore database;
    CollectionReference collectionReference;
    CollectionReference codeCollection;
    CollectionReference ownerCollection;


    private static boolean successFlag;

    public FirebaseData() {
        database = FirebaseFirestore.getInstance();
        collectionReference = database.collection("Users");
        codeCollection = database.collection("ScannableCodes");
        ownerCollection = database.collection("Owners");
    }

    public void addUserData(User user) {//adds or overwrites User data on the firebase

        Map<String, Object> userData = new HashMap<>();
        userData.put("contactInfo", user.getContactInfo());
        userData.put("name", user.getName());
        userData.put("numCodes", user.getNumCodes());
        userData.put("totalScore", user.getTotalScore());
        userData.put("userCodeList", user.getUserCodeList());
        userData.put("userId", user.getUserId());

        collectionReference
                .document(user.getUserId())
                .set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //if data is successfully uploaded
                        Log.d(TAG, "User " + user.getUserId() + "  successfully uploaded");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //if data upload fails
                        Log.d(TAG, "User " + user.getUserId() + " data failed to upload: " + e.toString());
                    }
                });
    }

    public void addCode(ScannableCode code) {
        // When a new code is added is must be added to the ScannableCodes collection and appended
        // to the codeList of the current user document from the Users collection

        // Add to ScannableCodes collection
        // First check if the code already exists

        Map<String, Object> codeData = new HashMap<>();
        codeData.put("codeName", code.getCodeName());
        codeData.put("codeScore", code.getCodeScore());
        codeData.put("Location", code.getLocation());
        codeData.put("Comment",code.getComments());


        codeCollection
                .document(code.getId())
                .set(codeData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //if data is successfully uploaded
                        Log.d(TAG, "Code " + code.getId() + "  successfully uploaded");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //if data upload fails
                        Log.d(TAG, "User " + code.getId() + " data failed to upload: " + e.toString());
                    }
                });


    }


    public ScannableCode getCode(String id) {
        ScannableCode code = new ScannableCode();
        DocumentReference docRef = database.collection("ScannableCodes").document(id);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        Map<String, Object> user = document.getData();
                        code.setId(id);
                        for (Map.Entry<String, Object> pair : user.entrySet()) {
                            String key = pair.getKey();
                            if (key.equals("codeScore")) {
                                Integer codeScore;
                                codeScore = ((Long) pair.getValue()).intValue();
                                code.setCodeScore((Integer) codeScore);
                            }
                            if (key.equals("Location")) {
                                code.setLocation((List<Double>) pair.getValue());
                            }
                            if (key.equals("Comment")){
                                List<HashMap<String,String>> list =
                                        (List<HashMap<String, String>>) pair.getValue();
                                ArrayList<Comment> comments = new ArrayList<>();
                                for (HashMap<String,String> map:list){
                                    comments.add(new Comment(map.get("author"),map.get("body")));
                                }
                                code.setComments(comments);
                            }

                        }
                        Log.d(TAG, "Code document exists");

                    } else {
                        Log.d(TAG, "No such code document " + id);
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
        return code;
    }

    public void removerUserData(User user) {//removes User data from the firebase
        collectionReference
                .document(user.getUserId())
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //if data is successfully uploaded
                        Log.d(TAG, "User " + user.getUserId() + "  successfully deleted");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //if data upload fails
                        Log.d(TAG, "User " + user.getUserId() + " data failed to delete: " + e.toString());
                    }
                });
    }


    // Returns a list of every User from the the "Users" collection
    public HashMap<String, User> getAllUserData() {
        HashMap<String, User> userDataList = new HashMap<>();
        //snapshot listener to watch for changes in the database
        collectionReference.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException error) {
                // Iterate over all documents in the collection
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                    List<Map> userCodeList = null;
                    String userId = new String();
                    User user = new User();
                    Map<String, Object> userData = doc.getData();
                    boolean success = false;
                    // Get all fields from the current document and construct a User
                    for (Map.Entry<String, Object> pair : userData.entrySet()) {
                        String key = pair.getKey();

                        if (key.equals("userId")) {
                            user.setId((String) pair.getValue());
                            userId = (String) pair.getValue();
                        }
                        if (key.equals("name")) {
                            user.setName((String) pair.getValue());
                        }
                        if (key.equals("contactInfo")) {
                            user.setContactInfo((String) pair.getValue());
                        }
                        if (key == "userCodeList") {
                            user.setCodeList((List) pair.getValue());
                        }
                        if (key.equals("numCodes")) {
                            Integer numCodes;
                            numCodes = ((Long) pair.getValue()).intValue();
                            user.setNumCodes(numCodes);
                        }
                        if (key.equals("totalScore")) {
                            Integer totalScore;
                            totalScore = ((Long) pair.getValue()).intValue();
                            user.setTotalScore(totalScore);
                        }
                    }
                    userDataList.put(userId, user);
                    Log.d(TAG, "User downloaded");
                    Log.d(TAG, "Server document data: " + doc.getData());


                }
            }
        });
        return userDataList;
    }

    // Returns a list of every code from the the "ScannableCodes" collection
    public ArrayList<ScannableCode> getAllCodeData() {
        ArrayList<ScannableCode> codeList = new ArrayList<>();
        //snapshot listener to watch for changes in the database

        codeCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException error) {
                // Iterate over all documents in the collection
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                    List<Map> userCodeList = null;
                    String userId = new String();
                    ScannableCode code = new ScannableCode();
                    Map<String, Object> codeData = doc.getData();
                    boolean success = false;
                    // Get all fields from the current document and construct a User
                    for (Map.Entry<String, Object> pair : codeData.entrySet()) {
                        String key = pair.getKey();

                        if (key.equals("codeScore")) {
                            Integer codeScore;
                            codeScore = ((Long) pair.getValue()).intValue();
                            code.setCodeScore(codeScore);
                        }
                        if (key.equals("Location")) {
                            code.setLocation((List<Double>) pair.getValue());
                        }
                        if (key.equals("Comment")){
                            List<HashMap<String,String>> list =
                                    (List<HashMap<String, String>>) pair.getValue();
                            ArrayList<Comment> comments = new ArrayList<>();
                            for (HashMap<String,String> map:list){
                                comments.add(new Comment(map.get("author"),map.get("body")));
                            }

                        }
                    }
                    codeList.add(code);
                    Log.d(TAG, "Code downloaded");
                    Log.d(TAG, "Server document data: " + doc.getData());
                }
            }
        });
        return codeList;
    }


    public ArrayList<User> getUsersByName(String userName) {
        ArrayList<User> userDataList = new ArrayList<>();
        //snapshot listener to watch for changes in the database
        database.collection("Users")
                .whereEqualTo("name", userName)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                User user = new User();
                                Map<String, Object> userData = document.getData();
                                // Get all fields from the current document and construct a User
                                for (Map.Entry<String, Object> pair : userData.entrySet()) {

                                    String key = pair.getKey();
                                    if (key.equals("userId")) {
                                        user.setId((String) pair.getValue());
                                    }
                                    if (pair.getKey().equals("name")) {
                                        user.setName((String) pair.getValue());
                                    }
                                    if (pair.getKey().equals("contactInfo")) {
                                        user.setContactInfo((String) pair.getValue());
                                    }
                                    if (pair.getKey().equals("userCodeList")) {
                                        user.setCodeList((List) pair.getValue());
                                    }
                                }
                                // Add the user from the current document to userDataList
                                if (user.getUserId() != null && user.getName() != null && user.getUserCodeList() != null
                                        && user.getContactInfo() != null) {
                                    userDataList.add(user);
                                    Log.d(TAG, "User " + user.getUserId() + " downloaded");
                                } else {
                                    Log.d(TAG, "User " + user.getUserId() + " not downloaded");

                                }
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
        return userDataList;
    }

    public ArrayList<User> getUsersByCode(String codeId) {
        ArrayList<User> userDataList = new ArrayList<>();
        //snapshot listener to watch for changes in the database
        database.collection("Users")
                .whereArrayContains("userCodeList", codeId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                User user = new User();
                                Map<String, Object> userData = document.getData();
                                // Get all fields from the current document and construct a User
                                for (Map.Entry<String, Object> pair : userData.entrySet()) {

                                    String key = pair.getKey();
                                    if (key.equals("userId")) {
                                        user.setId((String) pair.getValue());
                                    }
                                    if (pair.getKey().equals("name")) {
                                        user.setName((String) pair.getValue());
                                    }
                                    if (pair.getKey().equals("contactInfo")) {
                                        user.setContactInfo((String) pair.getValue());
                                    }
                                    if (pair.getKey().equals("userCodeList")) {
                                        user.setCodeList((List) pair.getValue());
                                    }
                                }
                                // Add the user from the current document to userDataList
                                if (user.getUserId() != null && user.getName() != null && user.getUserCodeList() != null
                                        && user.getContactInfo() != null) {
                                    userDataList.add(user);
                                    Log.d(TAG, "User " + user.getUserId() + " downloaded");
                                } else {
                                    Log.d(TAG, "User " + user.getUserId() + " not downloaded");

                                }
                            }
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
        return userDataList;
    }

    public User getUserById(String userId) {

        User user = new User();

        DocumentReference docRef = database.collection("Users").document(userId);

        // https://cloud.google.com/firestore/docs/query-data/get-data
        // Source can be CACHE, SERVER, or DEFAULT.
        Source source = Source.SERVER;

        // Get the document, forcing the SDK to use the offline cache
        docRef.get(source).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    // Document found on the server
                    DocumentSnapshot document = task.getResult();
                    Map<String, Object> userData = document.getData();
                    if (userData != null) {
                        for (Map.Entry<String, Object> pair : userData.entrySet()) {
                            String key = pair.getKey();

                            if (key.equals("userId")) {
                                user.setId((String) pair.getValue());
                            }
                            if (key.equals("name")) {
                                user.setName((String) pair.getValue());
                            }
                            if (key.equals("contactInfo")) {
                                user.setContactInfo((String) pair.getValue());
                            }
                            if (key.equals("userCodeList")) {
                                user.setCodeList((List<Map>) pair.getValue());
                            }
                            if (key.equals("numCodes")) {
                                Integer numCodes;
                                numCodes = ((Long) pair.getValue()).intValue();
                                user.setNumCodes(numCodes);
                            }
                            if (key.equals("totalScore")) {
                                Integer totalScore;
                                totalScore = ((Long) pair.getValue()).intValue();
                                user.setTotalScore(totalScore);
                            }
                        }
                    }

                    Log.d(TAG, "User downloaded");
                    Log.d(TAG, "Server document data: " + document.getData());
                } else {
                    Log.d(TAG, "Server get failed: ", task.getException());
                }
            }
        });
        return user;
    }

    public User getOwnerById(String androidId) {

        User user = new User();

        DocumentReference docRef = database.collection("Owners").document(androidId);

        // https://cloud.google.com/firestore/docs/query-data/get-data
        // Source can be CACHE, SERVER, or DEFAULT.
        Source source = Source.SERVER;

        // Get the document, forcing the SDK to use the offline cache
        docRef.get(source).addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    // Document found on the server
                    DocumentSnapshot document = task.getResult();
                    Map<String, Object> userData = document.getData();
                    if (userData != null) {
                        for (Map.Entry<String, Object> pair : userData.entrySet()) {
                            String key = pair.getKey();
                            if (key.equals("name")) {
                                user.setName((String) pair.getValue());
                            }
                        }
                    }

                    Log.d(TAG, "User downloaded");
                    Log.d(TAG, "Server document data: " + document.getData());
                } else {
                    Log.d(TAG, "Server get failed: ", task.getException());
                }
            }
        });
        return user;
    }

    public void addOwner(String android_id, String name) {
        // When a new code is added is must be added to the ScannableCodes collection and appended
        // to the codeList of the current user document from the Users collection

        // Add to ScannableCodes collection
        // First check if the code already exists

        Map<String, Object> ownerData = new HashMap<>();
        ownerData.put("name", name);


        ownerCollection
                .document(android_id)
                .set(ownerData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //if data is successfully uploaded
                        Log.d(TAG, "Code " + android_id + "  successfully uploaded");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //if data upload fails
                        Log.d(TAG, "User " + android_id + " data failed to upload: " + e.toString());
                    }
                });


    }

    public void removeOwner(String android_id) {
        ownerCollection
                .document(android_id)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //if data is successfully uploaded
                        Log.d(TAG, "User " + android_id + "  successfully deleted");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //if data upload fails
                        Log.d(TAG, "User " + android_id + " data failed to delete: " + e.toString());
                    }
                });
    }

    public void deleteCode(String id) {
        codeCollection
                .document(id)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        //if data is successfully uploaded
                        Log.d(TAG, "Code " + id + "  successfully deleted");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //if data upload fails
                        Log.d(TAG, "Code " + id + " data failed to delete: " + e.toString());
                    }
                });
    }

    // https://firebase.google.com/docs/firestore/query-data/queries
    public ArrayList<Map> getsUserByCode(String id) {
        // Create a reference to the cities collection

        // Create a query against the collection.
        ArrayList<Map> users = new ArrayList<>();
        database.collection("Users")
                .whereArrayContains("userCodeList", id)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                Map<String, Object> userData = document.getData();
                                if (userData != null) {
                                    users.add(userData);
                                } else {
                                    Log.d(TAG, "Error getting documents: ", task.getException());
                                }
                            }
                        }
                    }
                });
        return users;
    }

    public void addUsers(ArrayList<User> users) {
        for (User user : users) {
            addUserData(user);
        }
    }

}
/*
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if false;
    }
  }
}*/