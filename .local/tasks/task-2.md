---
title: Implement Transaction Search API (Mastercom v6)
---
# Implement Transaction Search API

## What & Why
Implement the first Mastercom API in the dispute lifecycle: "Search for the Original Transaction" (`POST /v6/transactions/search`). The existing endpoint at `POST /api/mastercard/transactions/search` uses the wrong Mastercom API path (`/mastercom/transaction/v1/search`). This needs to be corrected to use the Mastercom v6 endpoint (`/mastercom/v6/transactions/search`) and properly tested against the Mastercard sandbox.

## Done looks like
- The `POST /api/mastercard/transactions/search` endpoint correctly calls the Mastercard sandbox at `/mastercom/v6/transactions/search`
- The endpoint accepts the proper request body format required by the Mastercom v6 Transaction Search API
- A successful call to the sandbox returns transaction search results
- The endpoint returns meaningful error messages when the API call fails
- The endpoint is tested via curl from the Shell and returns a valid response from the Mastercard sandbox

## Out of scope
- Other Mastercom APIs (claim creation, chargebacks, etc.) — those will be implemented separately
- Frontend UI for transaction search

## Tasks
1. **Fix the Mastercom API endpoint path** — Update the transaction search endpoint in `MastercardApiController` to call `/mastercom/v6/transactions/search` instead of `/mastercom/transaction/v1/search`.

2. **Update error handling** — Improve the POST method in `MastercardApiClient` to return the error response body from Mastercard (not just the status code) so we can debug sandbox issues. Currently on failure it only logs the error body but throws a generic message.

3. **Test the endpoint** — Start the application and test the transaction search via curl using a sample request body that the Mastercom v6 API expects. Verify a successful response from the Mastercard sandbox. Provide the curl command for the user to test themselves.

## Relevant files
- `src/main/java/com/opus/dispute/management/controller/MastercardApiController.java:66-76`
- `src/main/java/com/opus/dispute/management/service/MastercardApiClient.java:83-97`