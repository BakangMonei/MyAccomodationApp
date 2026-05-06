/**
 * Seeds 50 listings with images in Firebase Storage + metadata in Firestore.
 *
 * Prereqs:
 *   cd scripts && npm install
 *   export GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json
 *
 * Usage: node seed-listings.mjs
 *
 * Storage path: listings/seed/{listingId}/cover.jpg
 * Ensure Storage rules allow admin SDK writes (service account bypasses client rules;
 * Emulator: start Storage emulator if testing locally).
 */
import { initializeApp, applicationDefault, cert } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { getStorage } from "firebase-admin/storage";
import { readFileSync, existsSync } from "fs";
import { randomUUID } from "crypto";

const regions = [
  "Gaborone CBD",
  "Block 3/6/8/9",
  "Tlokweng",
  "Mogoditshane",
  "Ruretse",
];
const types = ["Single Room", "Sharing", "Flat", "Bachelor"];

function pick(rng, arr) {
  return arr[Math.floor(rng() * arr.length)];
}

function mulberry32(a) {
  return function () {
    let t = (a += 0x6d2b79f5);
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

async function uploadCoverImage(bucket, objectPath, seed) {
  const url = `https://picsum.photos/seed/${encodeURIComponent(seed)}/800/600`;
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Image fetch failed ${res.status}`);
  const buffer = Buffer.from(await res.arrayBuffer());
  const token = randomUUID();
  const file = bucket.file(objectPath);
  await file.save(buffer, {
    metadata: {
      contentType: "image/jpeg",
      metadata: {
        firebaseStorageDownloadTokens: token,
      },
    },
  });
  const encoded = encodeURIComponent(objectPath);
  return `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encoded}?alt=media&token=${token}`;
}

async function main() {
  const seed = Number(process.env.SEED || "42");
  const rng = mulberry32(seed);

  if (process.env.GOOGLE_APPLICATION_CREDENTIALS && existsSync(process.env.GOOGLE_APPLICATION_CREDENTIALS)) {
    const json = JSON.parse(readFileSync(process.env.GOOGLE_APPLICATION_CREDENTIALS, "utf8"));
    initializeApp({ credential: cert(json) });
  } else {
    initializeApp({ credential: applicationDefault() });
  }

  const db = getFirestore();
  const bucket = getStorage().bucket();
  const providerId = process.env.SEED_PROVIDER_UID || "seed-provider-001";
  const providerName = "Seed Provider";
  const count = Number(process.env.SEED_COUNT || "50");

  for (let i = 0; i < count; i++) {
    const ref = db.collection("listings").doc();
    const region = pick(rng, regions);
    const typ = pick(rng, types);
    const price = Math.round(800 + rng() * 3200);
    const deposit = Math.round(price * (0.3 + rng() * 0.4));
    const dayOffset = Math.floor(rng() * 120);
    const avail = new Date();
    avail.setDate(avail.getDate() + dayOffset);

    const storagePath = `listings/seed/${ref.id}/cover.jpg`;
    const imageUrl = await uploadCoverImage(bucket, storagePath, `${ref.id}-${i}`);

    await ref.set({
      title: `${typ} near ${region} · listing ${i + 1}`,
      price,
      depositAmount: deposit,
      location: region,
      type: typ,
      amenities: ["Wi-Fi", "Prepaid utilities", "Borehole water"].slice(0, 2 + Math.floor(rng() * 2)),
      availabilityDate: Timestamp.fromDate(avail),
      imageUrls: [imageUrl],
      status: "Available",
      providerId,
      providerDisplayName: providerName,
      createdAt: Timestamp.now(),
    });

    if ((i + 1) % 10 === 0) {
      console.log(`Uploaded ${i + 1}/${count}…`);
    }
  }

  console.log(`Done: ${count} listings with Storage images for provider ${providerId}.`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
