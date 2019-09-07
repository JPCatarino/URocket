
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.addNewRocket = functions.https.onRequest(async (req, res) => {
    // const owner = req.query.uid;
    // const name = req.query.name;
    
    let data = req.body 

    if(req.method !== "POST"){
        res.status(400).send('Please send a POST request');
        return;
       }
    
    let reference = admin.firestore().doc('users/' + data.owner)
       
    admin.firestore().collection('rockets').add({owner: reference, name: data.name}).then(function(){res.send('OK').status(200)});
});

exports.addNewFlight = functions.https.onRequest(async (req, res) => {
    let data = req.body;

    if(req.method !== "POST"){
        res.status(400).send('Please send a POST request');
        return;
       }

    // res.send(data[1]._id);
    admin.firestore().collection('flights').add({launch_timestamp: data.launch_timestamp, rocket: admin.firestore().doc('rockets/' + data.rocket), telemetry: data.telemetry}).then(function(){res.send('OK').status(200)});
});

exports.addNewUser = functions.auth.user().onCreate((user) => {
    if(user.displayName != null){
        admin.firestore().collection('users').doc(user.uid).set({name: user.displayName});
    }
    else{
        admin.firestore().collection('users').doc(user.uid).set({name: user.email});
    }
});

