
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.addNewRocketC = functions.https.onRequest(async (req, res) => {
    // const owner = req.query.uid;
    // const name = req.query.name;
    
    let data = req.body; 

    if(req.method !== "POST"){
        res.status(400).send('Please send a POST request');
        return;
       }
    
    let reference = admin.firestore().doc('users/' + data.owner);
       
    admin.firestore().collection('rockets').add({owner: reference, name: data.name, is_public: data.is_public}).then(function(){res.send('OK').status(200)});
});

exports.addNewRocket = functions.https.onCall((data) => {
    // const owner = req.query.uid;
    // const name = req.query.name;
    
    let reference = admin.firestore().doc('users/' + data.owner);
       
    admin.firestore().collection('rockets').add({owner: reference, name: data.name, is_public: data.is_public, address: data.address}).then(function(){return {res:'OK'}});
});

exports.addNewFlightC = functions.https.onRequest(async (req, res) => {
    let data = req.body;

    if(req.method !== "POST"){
        res.status(400).send('Please send a POST request');
        return;
       }

    // res.send(data[1]._id);
    admin.firestore().collection('flights').add({launch_timestamp: data.launch_timestamp, rocket: admin.firestore().doc('rockets/' + data.rocket), telemetry: data.telemetry}).then(function(){res.send('OK').status(200)});
});

exports.addNewFlight = functions.https.onCall((data) => {
    // res.send(data[1]._id);
    admin.firestore().collection('flights').add({name: data.name, launch_timestamp: admin.firestore.Timestamp.fromDate(new Date(data.launch_timestamp)), rocket: admin.firestore().doc('rockets/' + data.rocket), telemetry: data.telemetry}).then(function(){return {res:'OK'}});
});

exports.addNewUser = functions.auth.user().onCreate((user) => {
    if(user.displayName != null){
        admin.firestore().collection('users').doc(user.uid).set({name: user.displayName});
    }
    else{
        admin.firestore().collection('users').doc(user.uid).set({name: user.email});
    }
});

