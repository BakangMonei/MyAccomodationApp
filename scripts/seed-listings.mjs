/**
 * Standalone seed script — run with Firebase Admin after `npm install` in scripts/.
 * Usage: export GOOGLE_APPLICATION_CREDENTIALS=/path/to/serviceAccount.json
 *        node seed-listings.mjs
 */
import { initializeApp, applicationDefault, cert } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { readFileSync, existsSync } from "fs";

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
  const batch = db.batch();
  const providerId = process.env.SEED_PROVIDER_UID || "seed-provider-001";
  const providerName = "Seed Provider";

  const count = 55;
  for (let i = 0; i < count; i++) {
    const ref = db.collection("listings").doc();
    const region = pick(rng, regions);
    const typ = pick(rng, types);
    const price = Math.round(800 + rng() * 3200);
    const deposit = Math.round(price * (0.3 + rng() * 0.4));
    const dayOffset = Math.floor(rng() * 120);
    const avail = new Date();
    avail.setDate(avail.getDate() + dayOffset);

    batch.set(ref, {
      title: `${typ} near ${region} · listing ${i + 1}`,
      price,
      depositAmount: deposit,
      location: region,
      type: typ,
      amenities: ["Wi-Fi", "Prepaid utilities", "Borehole water"].slice(0, 2 + Math.floor(rng() * 2)),
      availabilityDate: Timestamp.fromDate(avail),
      imageUrls: [`https://picsum.photos/seed/${ref.id}/800/600`],
      status: "Available",
      providerId,
      providerDisplayName: providerName,
      createdAt: Timestamp.now(),
    });
  }

  await batch.commit();
  console.log(`Seeded ${count} listings for provider ${providerId}.`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
