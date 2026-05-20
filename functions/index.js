const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Server-side preference fan-out: on new listing, finds users whose stored
 * Firestore preferences overlap and sends an FCM notification.
 * Deploy: firebase deploy --only functions
 */
exports.notifyMatchingUsers = functions.firestore
  .document("listings/{listingId}")
  .onCreate(async (snap, context) => {
    const listing = snap.data();
    const db = admin.firestore();
    const messaging = admin.messaging();

    const users = await db
      .collection("users")
      .where("preferences.maxPriceBwp", ">=", listing.price)
      .limit(500)
      .get();

    const tokens = [];
    for (const doc of users.docs) {
      const prefs = doc.get("preferences") || {};
      const min = prefs.minPriceBwp ?? 0;
      const max = prefs.maxPriceBwp ?? 1e9;
      const locs = prefs.locations || [];
      const types = prefs.types || [];
      if (listing.price < min || listing.price > max) continue;
      if (locs.length && !locs.includes(listing.location)) continue;
      if (types.length && !types.includes(listing.type)) continue;
      const cutoff = prefs.availabilityOnOrBefore;
      if (cutoff) {
        const cutoffDate = cutoff.toDate ? cutoff.toDate() : new Date(cutoff);
        const listingAvail = listing.availabilityDate;
        if (listingAvail) {
          const availDate = listingAvail.toDate
            ? listingAvail.toDate()
            : new Date(listingAvail);
          if (availDate > cutoffDate) continue;
        }
      }
      const token = prefs.fcmToken;
      if (token) tokens.push(token);
    }

    if (!tokens.length) return null;

    await messaging.sendEachForMulticast({
      tokens,
      notification: {
        title: "New place in " + listing.location,
        body: listing.title,
      },
      data: { listingId: context.params.listingId },
    });
    return null;
  });
