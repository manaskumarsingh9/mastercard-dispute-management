#!/bin/bash
# Mastercard Mastercom v6 API Test Commands
# Run these from the Replit Shell after starting the app with:
#   ./mvnw spring-boot:run > /tmp/app.log 2>&1 &
# Wait ~10 seconds for startup, then run any command below.

echo "=========================================="
echo "0. HEALTH CHECK"
echo "=========================================="
curl -s http://localhost:5000/api/mastercard/test
echo ""
echo ""

echo "=========================================="
echo "1. SEARCH FOR ORIGINAL TRANSACTION"
echo "=========================================="
curl -s -X POST http://localhost:5000/api/mastercard/transactions/search \
  -H "Content-Type: application/json" \
  -d '{"primaryAccountNum":"5123456789012345","tranStartDate":"2025-01-01","tranEndDate":"2025-01-31"}' | jq . 2>/dev/null || curl -s -X POST http://localhost:5000/api/mastercard/transactions/search \
  -H "Content-Type: application/json" \
  -d '{"primaryAccountNum":"5123456789012345","tranStartDate":"2025-01-01","tranEndDate":"2025-01-31"}'
echo ""
echo ""

echo "=========================================="
echo "3. CREATE A NEW CLAIM"
echo "Use the clearingTransactionId from step 1"
echo "=========================================="
curl -s -X POST http://localhost:5000/api/mastercard/claims \
  -H "Content-Type: application/json" \
  -d '{"disputedAmount":"100.00","disputedCurrency":"USD","claimType":"Standard","clearingTransactionId":"hqCnaMDqmto4wnL+BSUKSdzROqGJ7YELoKhEvluycwKNg3XTzSfaIJhFDkl9hW081B5tTqFFiAwCpcocPdB2My4t7DtSTk63VXDl1CySA8Y="}'
echo ""
echo ""

echo "=========================================="
echo "15. RETRIEVE CLAIM DETAILS"
echo "Replace CLAIM_ID with actual claim ID"
echo "=========================================="
# curl -s http://localhost:5000/api/mastercard/claims/CLAIM_ID
curl -s http://localhost:5000/api/mastercard/claims/200002017946 | head -c 500
echo ""
echo ""

echo "=========================================="
echo "2a. RETRIEVE CLEARING DETAIL"
echo "Replace CLAIM_ID and TRANSACTION_ID"
echo "=========================================="
# curl -s http://localhost:5000/api/mastercard/claims/CLAIM_ID/transactions/clearing/TRANSACTION_ID
echo "(Requires valid claim-id and transaction-id from transaction search)"
echo ""
echo ""

echo "=========================================="
echo "2b. RETRIEVE AUTHORIZATION DETAIL"
echo "Replace CLAIM_ID and TRANSACTION_ID"
echo "=========================================="
# curl -s http://localhost:5000/api/mastercard/claims/CLAIM_ID/transactions/authorization/TRANSACTION_ID
echo "(Requires valid claim-id and transaction-id from transaction search)"
echo ""
echo ""

echo "=========================================="
echo "4a. LOAD DATA FOR RETRIEVAL REQUEST"
echo "=========================================="
curl -s http://localhost:5000/api/mastercard/claims/200002017946/retrievalrequests/loaddataforretrievalrequests | head -c 500
echo ""
echo ""

echo "=========================================="
echo "4b. CREATE RETRIEVAL REQUEST"
echo "=========================================="
curl -s -X POST http://localhost:5000/api/mastercard/claims/200002017946/retrievalrequests \
  -H "Content-Type: application/json" \
  -d '{"retrievalRequestReason":"6343","docNeeded":"2","instructionsForHealthcare":"Please provide transaction documentation"}'
echo ""
echo ""

echo "=========================================="
echo "5a. ISSUER RESPONSE TO RETRIEVAL FULFILLMENT"
echo "Replace CLAIM_ID and REQUEST_ID"
echo "=========================================="
# curl -s -X POST http://localhost:5000/api/mastercard/claims/CLAIM_ID/retrievalrequests/REQUEST_ID/fulfillments/response \
#   -H "Content-Type: application/json" \
#   -d '{"issuerResponseCd":"APPROVE","memo":"Documentation approved"}'
echo "(Requires valid claim-id and request-id from step 4b)"
echo ""
echo ""

echo "=========================================="
echo "5b. GET RETRIEVAL DOCUMENTS"
echo "Replace CLAIM_ID and REQUEST_ID"
echo "=========================================="
# curl -s "http://localhost:5000/api/mastercard/claims/CLAIM_ID/retrievalrequests/REQUEST_ID/documents?format=ORIGINAL"
echo "(Requires valid claim-id and request-id)"
echo ""
echo ""

echo "=========================================="
echo "5c. RETRIEVAL FULFILLMENT STATUS"
echo "=========================================="
# curl -s -X PUT http://localhost:5000/api/mastercard/retrievalrequests/status \
#   -H "Content-Type: application/json" \
#   -d '{"retrievalList":[{"claimId":"CLAIM_ID","requestId":"REQUEST_ID"}]}'
echo "(Requires valid claim-id and request-id)"
echo ""
echo ""

echo "=========================================="
echo "6a. LOAD DATA FOR CHARGEBACK"
echo "=========================================="
curl -s -X POST http://localhost:5000/api/mastercard/claims/200002017946/chargebacks/loaddataforchargebacks \
  -H "Content-Type: application/json" \
  -d '{"chargebackType":"CHARGEBACK"}' | head -c 500
echo ""
echo ""

echo "=========================================="
echo "6b. CREATE CHARGEBACK"
echo "=========================================="
curl -s -X POST http://localhost:5000/api/mastercard/claims/200002017946/chargebacks \
  -H "Content-Type: application/json" \
  -d '{"amount":"100.00","currency":"USD","documentIndicator":"true","reasonCode":"4853","chargebackType":"CHARGEBACK","messageText":"Test chargeback"}'
echo ""
echo ""

echo "=========================================="
echo "7. CHARGEBACK REVERSAL"
echo "Replace CLAIM_ID and CHARGEBACK_ID"
echo "=========================================="
# curl -s -X POST http://localhost:5000/api/mastercard/claims/CLAIM_ID/chargebacks/CHARGEBACK_ID/reversal
echo "(Requires valid claim-id and chargeback-id from step 6b)"
echo ""
echo ""

echo "=========================================="
echo "8. UPDATE CHARGEBACK (Add Documents)"
echo "Replace CLAIM_ID and CHARGEBACK_ID"
echo "=========================================="
# curl -s -X PUT http://localhost:5000/api/mastercard/claims/CLAIM_ID/chargebacks/CHARGEBACK_ID \
#   -H "Content-Type: application/json" \
#   -d '{"memo":"Additional documentation","creditVoucherAction":"ACCEPT"}'
echo "(Requires valid claim-id and chargeback-id)"
echo ""
echo ""

echo "=========================================="
echo "9a. CHARGEBACK STATUS"
echo "Replace CLAIM_ID and CHARGEBACK_ID"
echo "=========================================="
curl -s -X PUT http://localhost:5000/api/mastercard/chargebacks/status \
  -H "Content-Type: application/json" \
  -d '{"chargebackList":[{"claimId":"200002017946","chargebackId":"300003030802"}]}'
echo ""
echo ""

echo "=========================================="
echo "9b. ACKNOWLEDGE CHARGEBACKS"
echo "Replace CLAIM_ID and CHARGEBACK_ID"
echo "=========================================="
curl -s -X PUT http://localhost:5000/api/mastercard/chargebacks/acknowledge \
  -H "Content-Type: application/json" \
  -d '{"chargebackList":[{"claimId":"200002017946","chargebackId":"300003030802"}]}'
echo ""
echo ""

echo "=========================================="
echo "10. CREATE CASE FILING"
echo "=========================================="
curl -s -X POST http://localhost:5000/api/mastercard/cases \
  -H "Content-Type: application/json" \
  -d '{"caseType":"1","chargebackRefNum":["1111423456"],"disputeAmount":"100.00","currencyCode":"USD","filedAgainstIca":"005323","filingAs":"I","filingIca":"001111","memo":"Test pre-arbitration case","chargebackReasonCode":"4853","merchantName":"Test Merchant"}'
echo ""
echo ""

echo "=========================================="
echo "11. UPDATE CASE FILING"
echo "Replace CASE_ID"
echo "=========================================="
# curl -s -X PUT http://localhost:5000/api/mastercard/cases/CASE_ID \
#   -H "Content-Type: application/json" \
#   -d '{"action":"ACCEPT","memo":"Accepting the case resolution"}'
echo "(Requires valid case-id from step 10)"
echo ""
echo ""

echo "=========================================="
echo "12. GET CASE DOCUMENTS"
echo "Replace CASE_ID"
echo "=========================================="
# curl -s "http://localhost:5000/api/mastercard/cases/CASE_ID/documents?format=ORIGINAL"
echo "(Requires valid case-id)"
echo ""
echo ""

echo "=========================================="
echo "13a. LOAD DATA FOR FRAUD"
echo "=========================================="
curl -s http://localhost:5000/api/mastercard/claims/200002017946/fraud/loaddataforfraud | head -c 500
echo ""
echo ""

echo "=========================================="
echo "13b. CREATE FRAUD EVENT"
echo "=========================================="
curl -s -X POST http://localhost:5000/api/mastercard/claims/200002017946/fraud/mastercard \
  -H "Content-Type: application/json" \
  -d '{"deviceType":"1","acctStatus":"ACCT_IS_OPEN","reportDate":"2025-02-11","fraudType":"00","subType":"K","cvcInvalidIndicator":"Y","chgbkIndicator":"1"}'
echo ""
echo ""

echo "=========================================="
echo "14a. LOAD DATA FOR FEE"
echo "=========================================="
curl -s -X POST http://localhost:5000/api/mastercard/claims/200002017946/fees/loaddataforfees \
  -H "Content-Type: application/json" \
  -d '{"reasonCode":"4853"}' | head -c 500
echo ""
echo ""

echo "=========================================="
echo "14b. CREATE FEE"
echo "=========================================="
curl -s -X POST http://localhost:5000/api/mastercard/claims/200002017946/fee \
  -H "Content-Type: application/json" \
  -d '{"cardAcceptorIdCode":"1","cardNumber":"5123456789012345","countryCode":"USA","currency":"USD","feeDate":"2025-02-11","destinationMember":"005323","feeAmount":"100.00","creditSender":"true","creditReceiver":"false","message":"Test fee collection","reason":"7604"}'
echo ""
echo ""

echo "=========================================="
echo "ALL TESTS COMPLETE"
echo "=========================================="
