# LifePilot card guardrail

Card code that asks "can I afford this?" at the till, against the forecast
rather than the balance.

## The problem this solves

A balance says you have R13,200. It does not say that R12,400 of it is already
promised to a bond, a car payment and three debit orders that land before your
salary does. Every banking app will let you spend it anyway and tell you what
happened afterwards.

This declines the swipe instead.

## Why it is split in two

`beforeTransaction` gets **two seconds**, and that budget covers the network
round trip. A LifePilot forecast needs an OAuth token, a balance call, six
months of transaction history and a day-by-day projection. It does not fit, and
no amount of tuning makes it fit.

So the card never computes. It asks a service that already did:

| Hook | Window | What runs |
| --- | --- | --- |
| `beforeTransaction` | 2s | One POST. The service answers from a cached snapshot, no outbound calls. |
| `afterTransaction` | 15s | Rebuilds the snapshot, so the next swipe sees the spend that just happened. |
| `afterDecline` | 15s | Same rebuild — a decline still changes what is known. |

The snapshot reduces a whole forecast to the one number a swipe needs:
**discretionary headroom** — balance, less the recurring debits due before the
next detected payday, less an emergency buffer.

## Setup

1. Deploy the LifePilot backend somewhere the Investec card sandbox can reach.
2. Set `LIFEPILOT_CARD_SHARED_SECRET` on the backend. **The card endpoints
   return 503 until you do.** They face the public internet and answer with
   balance data, so an unconfigured deployment refuses to answer rather than
   answering anyone.
3. Copy `env.example.json` into the card editor's `env.json` and fill it in.
4. Paste `main.js` into the card editor.
5. Prime the cache once, so the first swipe has something to read:

   ```bash
   curl -X POST \
     -H "X-LifePilot-Card-Key: $LIFEPILOT_CARD_SHARED_SECRET" \
     https://your-lifepilot-host/api/lifepilot/cards/accounts/YOUR_ACCOUNT_ID/snapshot
   ```

## Trying it without a card

The guardrail can be exercised end to end against the demo account, with no
Investec credentials and no card:

```bash
curl -X POST -H "X-LifePilot-Card-Key: $LIFEPILOT_CARD_SHARED_SECRET" \
  http://localhost:8080/api/lifepilot/cards/accounts/demo-account/snapshot

curl -X POST -H "Content-Type: application/json" \
  -H "X-LifePilot-Card-Key: $LIFEPILOT_CARD_SHARED_SECRET" \
  -d '{"accountId":"demo-account","centsAmount":1200000,"currencyCode":"ZAR","merchantName":"Incredible Connection","merchantCategoryCode":"5732"}' \
  http://localhost:8080/api/lifepilot/cards/authorization
```

The demo account holds about R14,800 with roughly R4,700 already committed
before payday, so R12,000 comes back as `EXCEEDS_HEADROOM` — approved anyway,
because monitor mode is on.

## It will not decline anything yet

`lifepilot.card.monitor-only` defaults to `true`. The guardrail reaches its
verdict, logs it, and approves anyway. Watch the card logs against real
spending for a week before you trust it with a real decline.

To arm it, set `LIFEPILOT_CARD_MONITOR_ONLY=false`.

## What it will never decline

- **Essential categories**, whatever the forecast says: groceries, fuel,
  transport, pharmacy, medical and utilities. Declining someone their petrol or
  their medication to protect a projected balance does more harm than the
  shortfall it avoids. Configurable via
  `lifepilot.card.essential-merchant-category-codes`.
- **Anything it is unsure about.** No snapshot, a stale one, a timeout, an HTTP
  error, an unparseable response, an unreachable host — all approve. A guardrail
  that declines when it is unsure strands someone at a checkout, which is worse
  than the overdraft it was trying to prevent.

Only an explicit `DECLINE` from the service stops a transaction.

## Reading the logs

```
LifePilot DECLINE (EXCEEDS_HEADROOM): 1200.00 exceeds the 300.00 free to spend
before income lands on 2026-09-25 by 900.00. The balance of 13200.00 is already
committed to 12400.00 of upcoming debits.
```

`assessment` records what the forecast concluded; `decision` is what the card
was told to do. In monitor mode they diverge on purpose — that gap is the thing
worth watching before arming it.

## Known gaps

- The snapshot cache is in memory, so a restart empties it and the next swipe
  approves until something refreshes it.
- One account per card, taken from `env.json`.
- The shared secret is the only authentication. It is proportionate for a
  single-user prototype and not for anything beyond that.
