

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

exports.notifyContacts = 