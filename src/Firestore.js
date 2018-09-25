import firebase from 'firebase'

var config = {
    apiKey: "AIzaSyD8h7Sm_NXw6qxVsQKO86--OA9I0qeRQCw",
    authDomain: "analyze-naneos.firebaseapp.com",
    databaseURL: "https://analyze-naneos.firebaseio.com",
    projectId: "analyze-naneos",
    storageBucket: "analyze-naneos.appspot.com",
    messagingSenderId: "870856191016"
};

firebase.initializeApp(config);

export default firebase;