

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


exports.notifyContacted = functions.firestore
  .document("users/{uid}").onWrite((change, context) => { 
  
  const data = change.after.data();

  const uid = data.uid;
  const case_id = data.notified_by;
  const token = data.device_token;
  
   // Notification details.
  const payload = {
    notification: {
      title: 'Avis de proximité!',
      body: 'Rendez-vous dans votre profile plus de details'
    }
  };

  // Send notifications to token.
  admin.messaging().sendToDevice(token, payload)
   .then((response) => {
    // Response is a message ID string.
    console.log('Successfully sent message:', response);
  })
  .catch((error) => {
    console.log('Error sending message:', error);
  });

});
