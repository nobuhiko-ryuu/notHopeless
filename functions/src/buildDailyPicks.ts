/**
 * buildDailyPicks.ts
 * Cloud Scheduler Function: Build daily picks (runs at 21:00 UTC = 06:00 JST).
 */

import * as admin from 'firebase-admin';
import * as functions from 'firebase-functions/v2/scheduler';

const JST_OFFSET_MS = 9 * 60 * 60 * 1000; // UTC+9

/**
 * Get the current date string in JST (YYYY-MM-DD).
 */
function getJSTDateString(now: Date): string {
  const jstMs = now.getTime() + JST_OFFSET_MS;
  const jstDate = new Date(jstMs);
  const year = jstDate.getUTCFullYear();
  const month = String(jstDate.getUTCMonth() + 1).padStart(2, '0');
  const day = String(jstDate.getUTCDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Sum all reaction counts for a post document.
 */
function totalReactions(
  reactionCounts: Record<string, number> | undefined
): number {
  if (!reactionCounts) return 0;
  return Object.values(reactionCounts).reduce((sum, v) => sum + v, 0);
}

export const buildDailyPicks = functions.onSchedule(
  {
    schedule: '0 21 * * *',
    timeZone: 'UTC',
  },
  async (_event) => {
    const db = admin.firestore();
    const now = new Date();
    const date = getJSTDateString(now);

    // Idempotency check: skip if dailyPicks/{date} already exists
    const picksRef = db.collection('dailyPicks').doc(date);
    const existingSnap = await picksRef.get();
    if (existingSnap.exists) {
      console.log(`dailyPicks/${date} already exists. Skipping.`);
      return;
    }

    const pickIds: string[] = [];

    // --- Candidate 1: Top reaction posts in the last 24h ---
    const since24h = new Date(now.getTime() - 24 * 60 * 60 * 1000);
    const posts24hSnap = await db
      .collection('posts')
      .where('status', '==', 'visible')
      .where('createdAt', '>=', admin.firestore.Timestamp.fromDate(since24h))
      .orderBy('createdAt', 'desc')
      .limit(10)
      .get();

    // Sort by total reaction count descending, take top 3
    const posts24h = posts24hSnap.docs.map((doc) => ({
      id: doc.id,
      total: totalReactions(doc.data().reactionCounts as Record<string, number>),
    }));
    posts24h.sort((a, b) => b.total - a.total);
    const top3 = posts24h.slice(0, 3);
    for (const post of top3) {
      if (!pickIds.includes(post.id)) {
        pickIds.push(post.id);
      }
    }

    // --- Candidate 2: Recent posts in last 72h to fill up to 3 ---
    if (pickIds.length < 3) {
      const since72h = new Date(now.getTime() - 72 * 60 * 60 * 1000);
      const posts72hSnap = await db
        .collection('posts')
        .where('status', '==', 'visible')
        .where('createdAt', '>=', admin.firestore.Timestamp.fromDate(since72h))
        .orderBy('createdAt', 'desc')
        .limit(20)
        .get();

      for (const doc of posts72hSnap.docs) {
        if (pickIds.length >= 3) break;
        if (!pickIds.includes(doc.id)) {
          pickIds.push(doc.id);
        }
      }
    }

    // --- Candidate 3: Stocks by usedAt ascending to fill remaining slots ---
    if (pickIds.length < 3) {
      const needed = 3 - pickIds.length;
      const stocksSnap = await db
        .collection('stocks')
        .orderBy('usedAt', 'asc')
        .limit(needed * 2)
        .get();

      const batch = db.batch();
      let added = 0;

      for (const doc of stocksSnap.docs) {
        if (added >= needed) break;
        const postId: string = doc.data().postId as string;
        if (postId && !pickIds.includes(postId)) {
          pickIds.push(postId);
          // Update usedAt to now
          batch.update(doc.ref, {
            usedAt: admin.firestore.FieldValue.serverTimestamp(),
          });
          added++;
        }
      }

      if (added > 0) {
        await batch.commit();
      }
    }

    // Write dailyPicks/{date}
    await picksRef.set({
      date,
      pickIds,
      generatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    console.log(`dailyPicks/${date} created with ${pickIds.length} picks: ${pickIds.join(', ')}`);
  }
);
