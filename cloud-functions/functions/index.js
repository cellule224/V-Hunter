

'use strict';

const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

const db = admin.firestore();

let usersRef = db.collection("users");
let query;

exports.receivePositif = functions.firestore.document('positives/{doc}').onCreate(
	async (snapshot) => {
		const uid = snapshot.data().uploaded_by;
			const timestamp = snapshot.data().timestamp;
			const mac_list = snapshot.data().mac_list;

		//Get the macadresse in each item in mac_list and compare to users' 
		//-> macs in 'users'
		mac_list.forEach(note => 
			query = usersRef.where('user_mac', '==', note.macAddress).get()
				.then(snapshot => {
					if (snapshot.empty) {
						console.log("No docs");
						return;
					}	

					snapshot.forEach(doc => {
						let ref = usersRef.document(doc.id);
						ref.update("notified_by_uid", uid);
						console.log("Contact node updated")

					});
					return query;
				})
		);
	}
);



/*****************************************************************
*                                                                *
*                        NOTIFICATION                            *
*                                                                *
*                                                                *
*****************************************************************/   


exports.notifyContacted = functions.region('europe-west1').database.ref(`/users/{user_id}/user_trans_history/{tr_id}`)
   .onCreate(async (snap, context) => {    

    const transaction = snap.val();
    const receiverUid = transaction.tr_receiver_id;
    const senderPhone = transaction.tr_sender_phone_num;
    const amountReceived = transaction.tr_amount;
    const tempReceiverRef = snap.ref.parent;
    const temp = tempReceiverRef.parent;

    //If it's the sender's node do not send notification
    if (new String(temp).valueOf() !== new String(admin.database().ref(`/users/${receiverUid}`)).valueOf()) {
      console.log("No need to send notification to sender. Notification Canceled");
      console.log("This: " + temp + " is the same..");
      console.log("..as this: " + admin.database().ref(`/users/${receiverUid}`));
      return;
    }

// Get the list of device notification tokens.
    const getDeviceTokensPromise = admin.database()
        .ref(`/users/${receiverUid}/user_tokens`).once('value');

    // Get the follower profile.
    const getSenderProfilePromise = admin.auth().getUser(receiverUid);

    // The snapshot to the user's tokens.
    let tokensSnapshot;

    // The array containing all the user's tokens.
    let tokens;

    const results = await Promise.all([getDeviceTokensPromise, getSenderProfilePromise]);
    tokensSnapshot = results[0];
    const sender = results[1];

    // Check if there are any device tokens.
    if (!tokensSnapshot.hasChildren()) {
      return console.log('There are no notification tokens to send to.');
    }
    console.log('There are', tokensSnapshot.numChildren(), 'tokens to send notifications to.');


  // Notification details.
  const payload = {
    notification: {
      title: 'Paiement reçu!',
      body: `${senderPhone} vous a envoyer `.concat(amountReceived).concat("FG")
    }
  };


// Listing all tokens as an array.
  tokens = Object.keys(tokensSnapshot.val());
  // Send notifications to all tokens.
  const response = await admin.messaging().sendToDevice(tokens, payload);
  // For each message check if there was an error.
  const tokensToRemove = [];
  response.results.forEach((result, index) => {
    const error = result.error;
    if (error) {
      console.error('Failure sending notification to', tokens[index], error);
      // Cleanup the tokens who are not registered anymore.
      if (error.code === 'messaging/invalid-registration-token' ||
          error.code === 'messaging/registration-token-not-registered') {
        tokensToRemove.push(tokensSnapshot.ref.child(tokens[index]).remove());
      }
    }
  });
  return Promise.all(tokensToRemove);
});