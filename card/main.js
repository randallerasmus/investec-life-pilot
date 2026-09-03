/**
 * LifePilot card guardrail.
 *
 * Investec gives beforeTransaction two seconds for everything, including the
 * network round trip. A LifePilot forecast needs an OAuth token, a balance
 * call, six months of transactions and a day-by-day walk, so the card never
 * asks for one. It asks a service that already built it, and that service
 * answers from cache.
 *
 * The refresh happens in afterTransaction, which has fifteen seconds.
 *
 * Every failure path approves. A guardrail that declines when it is unsure
 * strands someone at a till, which is worse than the overdraft it was trying
 * to prevent.
 */

var DEFAULT_TIMEOUT_MS = 1200;

/**
 * Reads a nested value without assuming the shape of the authorisation object.
 * The runtime is not one we can test against, so this avoids both optional
 * chaining and a throw on a missing branch.
 */
function get(source, path) {
  var current = source;
  var parts = path.split(".");
  for (var i = 0; i < parts.length; i += 1) {
    if (current === null || current === undefined) return null;
    current = current[parts[i]];
  }
  return current === undefined ? null : current;
}

function config() {
  return {
    baseUrl: process.env.lifePilotBaseUrl,
    accountId: process.env.lifePilotAccountId,
    apiKey: process.env.lifePilotApiKey,
    timeoutMs: Number(process.env.lifePilotTimeoutMs) || DEFAULT_TIMEOUT_MS
  };
}

/**
 * Resolves to null instead of rejecting when the deadline passes, so the caller
 * treats a slow answer the same way it treats no answer.
 */
function withDeadline(promise, ms) {
  return Promise.race([
    promise,
    new Promise(function (resolve) {
      setTimeout(function () {
        resolve(null);
      }, ms);
    })
  ]);
}

function askLifePilot(authorization, settings) {
  return fetch(settings.baseUrl + "/api/lifepilot/cards/authorization", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-LifePilot-Card-Key": settings.apiKey
    },
    body: JSON.stringify({
      accountId: settings.accountId,
      centsAmount: get(authorization, "centsAmount"),
      currencyCode: get(authorization, "currencyCode"),
      merchantName: get(authorization, "merchant.name"),
      merchantCategoryCode: get(authorization, "merchant.category.code"),
      cardId: get(authorization, "card.id"),
      reference: get(authorization, "reference")
    })
  }).then(function (response) {
    if (!response.ok) return null;
    return response.json();
  });
}

function refreshSnapshot(settings) {
  return fetch(
    settings.baseUrl + "/api/lifepilot/cards/accounts/" + encodeURIComponent(settings.accountId) + "/snapshot",
    {
      method: "POST",
      headers: { "X-LifePilot-Card-Key": settings.apiKey }
    }
  ).catch(function (error) {
    console.log("LifePilot snapshot refresh failed: " + error);
    return null;
  });
}

const beforeTransaction = async (authorization) => {
  var settings = config();

  if (!settings.baseUrl || !settings.accountId) {
    console.log("LifePilot is not configured; approving.");
    return true;
  }

  try {
    var verdict = await withDeadline(askLifePilot(authorization, settings), settings.timeoutMs);

    if (verdict === null) {
      console.log("LifePilot did not answer in time; approving.");
      return true;
    }

    console.log(
      "LifePilot " + verdict.decision + " (" + verdict.assessment + "): " + verdict.reason
    );

    if (verdict.monitorOnly) {
      console.log("Monitor mode is on, so this verdict was recorded but not enforced.");
    }

    // Only an explicit decline stops the transaction. Anything unexpected in
    // this field means the service and the card disagree about the contract,
    // and the safe reading of a disagreement is to let the payment through.
    return verdict.decision !== "DECLINE";
  } catch (error) {
    console.log("LifePilot guardrail errored; approving. " + error);
    return true;
  }
};

const afterTransaction = async (transaction) => {
  // Fifteen seconds here, so this is where the expensive rebuild belongs. The
  // spend that just happened moves the balance, and the next swipe should see it.
  var settings = config();
  if (!settings.baseUrl || !settings.accountId) return;
  await refreshSnapshot(settings);
};

const afterDecline = async (declined) => {
  // A decline still changes what is known about the account, and leaving a
  // stale snapshot behind would make the next decision worse.
  var settings = config();
  if (!settings.baseUrl || !settings.accountId) return;
  await refreshSnapshot(settings);
};
