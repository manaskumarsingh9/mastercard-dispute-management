const fs = require('fs');
const path = require('path');

const ISSUER_ROOT = 'src/data/sources/issuer';
const ACQUIRER_ROOT = 'src/data/sources/acquirer';

function mkdirp(dir) { fs.mkdirSync(dir, { recursive: true }); }
function writeJson(fp, data) { fs.writeFileSync(fp, JSON.stringify(data, null, 2) + '\n'); }
function rmDir(dir) { try { fs.rmSync(dir, { recursive: true, force: true }); } catch(e) {} }

const allCodes = ["4801","4802","4807","4808","4809","4812","4831","4834","4835","4837","4840","4841","4842","4846","4847","4849","4850","4853","4854","4855","4856","4857","4859","4860","4862","4863","4870","4871","4899","4900","4901","4902","4903","4905","4908"];
for (const c of allCodes) { rmDir(`${ISSUER_ROOT}/${c}`); rmDir(`${ACQUIRER_ROOT}/${c}`); }

const STORIES = {

"4801": {
  fav: "issuer", desc: "Requested Transaction Data Not Received",
  product: "a wireless charging pad from an online electronics retailer",
  merchant: "TechGear Online",
  issuer: {
    dispute: "The cardholder noticed an unfamiliar charge of $42.99 from 'TECHGEAR ONLINE' on their statement. The cardholder does not recall making this purchase. The issuing bank submitted a retrieval request to the acquirer [10 days after the transaction date] to obtain a copy of the sales draft and transaction documentation. As of [45 days after the retrieval request], no response or documentation has been received from the acquirer.",
    contactedMerchant: false,
    merchantResponse: "N/A — the retrieval request was sent to the acquirer through Mastercard channels.",
    resolution: "Full reversal due to non-receipt of requested transaction data within the required timeframe.",
    docs: [
      { type: "Retrieval Request Record", desc: "Copy of retrieval request sent with transmission timestamp and reference number." },
      { type: "Non-Response Log", desc: "System log confirming no response was received within the 45-day fulfillment window." }
    ],
    commentary: "Filing under 4801. Retrieval request sent [10 days after the transaction] and the acquirer has not responded within 45 days. Per Mastercard Rule 6.1, the acquirer must provide requested transaction documentation.",
    cardPresent: false, posEntry: "81",
    avs: { code: "U", desc: "Address information unavailable" },
    cvv: { code: "P", desc: "Not processed" },
    riskFlags: { geoMismatch: "Unknown — insufficient data", deviceTrust: "Unknown" }
  },
  acquirer: {
    items: [{ name: "Wireless Charging Pad — 15W Fast Charge", qty: 1, price: 42.99, sku: "WCP-100" }],
    orderStatus: "Completed",
    shippingAddr: "Cardholder's billing address on file",
    orderNotes: "Order was processed and shipped. Transaction documentation was not retained due to a data archival system migration that occurred [20 days after the transaction].",
    emails: [
      { type: "Order Confirmation", dir: "merchant_to_customer", timing: "Immediately after purchase",
        subject: "Your TechGear Online Order Confirmation", body: "Thank you for your order of the Wireless Charging Pad (15W Fast Charge). Order total: $42.99. Shipping to your address on file. Expected delivery: [5-7 business days after purchase]." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved", entryMode: "Ecommerce",
    threeDS: null,
    authNotes: "Authorization was obtained at time of purchase. However, the full transaction record and sales draft are unavailable due to a system migration.",
    settlementNotes: "Transaction settled normally. Original sales draft lost during records system migration. Merchant unable to provide requested documentation.",
    riskDevice: { status: "Device data no longer available due to system migration", trust: "Unknown", match: false },
    riskIP: { level: "Unknown", proxy: false, geoMatch: false, notes: "IP records not retained." },
    riskScore: "Unable to assess — records unavailable",
    avsCode: "U", avsDesc: "Address unavailable — records not retained",
    cvvCode: "P", cvvDesc: "Not processed or records unavailable",
    refundWindow: "30 days from purchase date",
    refundPolicy: "Full refund within 30 days if item returned in original packaging.",
    refundDisclosure: "Policy on website footer and checkout page.",
    refundAck: "Standard website terms apply.",
    fulfillment: { type: "Physical Shipment", status: "Shipped — tracking details no longer available",
      method: "Standard shipping via carrier", timing: "[5-7 business days after purchase]", confirmed: false,
      notes: "Shipment records indicate order was dispatched. Carrier tracking number not retained after system migration." },
    threeDSRecord: null
  }
},

"4802": {
  fav: "issuer", desc: "Requested Item Not Received",
  product: "a dining charge at a restaurant",
  merchant: "Bistro on Main",
  issuer: {
    dispute: "The cardholder disputes a charge from 'BISTRO ON MAIN' stating the posted amount of $104.50 is higher than the meal total they recall of approximately $85.00. The issuing bank sent a retrieval request to the acquirer [8 days after the transaction] specifically requesting a copy of the signed sales receipt showing the final total and tip. As of [45 days after the request], the acquirer has not provided the signed receipt.",
    contactedMerchant: true,
    merchantResponse: "The cardholder called the restaurant [5 days after dining]. The host said they would check records but never called back with a copy of the receipt.",
    resolution: "Reversal of the disputed amount due to non-receipt of the requested signed sales receipt.",
    docs: [
      { type: "Retrieval Request Record", desc: "Request submitted to acquirer for signed sales receipt." },
      { type: "Cardholder Statement", desc: "Account showing the posted charge of $104.50." }
    ],
    commentary: "Filing under 4802. Issuer requested the signed sales receipt to verify the final amount including tip. Acquirer has not provided the document within the fulfillment window.",
    cardPresent: true, posEntry: "05",
    avs: { code: "N/A", desc: "Card-present EMV" },
    cvv: { code: "N/A", desc: "Chip cryptogram" },
    riskFlags: { geoMismatch: "None", deviceTrust: "N/A — card present" }
  },
  acquirer: {
    items: [{ name: "Dinner for two — food and beverages", qty: 1, price: 85.00, sku: "DINE-0802" }],
    orderStatus: "Completed",
    shippingAddr: null,
    orderNotes: "Dine-in transaction. Subtotal $85.00. The signed receipt was retained at the restaurant but cannot be located after a staff change.",
    emails: [
      { type: "No Communications on File", dir: "system_note", timing: "N/A",
        subject: "N/A", body: "Card-present restaurant transaction. No email exchanged. The cardholder called [5 days after dining] and the manager verbally confirmed the transaction but could not locate the signed receipt. The receipt filing system was disrupted during a recent staff transition." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "EMV Chip — Card Present", threeDS: null,
    authNotes: "Authorization for base meal amount of $85.00. Final settled amount of $104.50 includes a tip added post-authorization. The signed receipt with the tip entry is unavailable.",
    settlementNotes: "Settled at $104.50 including tip. The signed receipt showing the customer's tip entry has been misplaced and cannot be provided.",
    riskDevice: { status: "N/A — card-present", trust: "N/A", match: true },
    riskIP: { level: "N/A", proxy: false, geoMatch: true, notes: "Card present at restaurant." },
    riskScore: "Low — card-present transaction",
    avsCode: "N/A", avsDesc: "Card-present EMV",
    cvvCode: "N/A", cvvDesc: "Chip cryptogram verified",
    refundWindow: "N/A — restaurant services",
    refundPolicy: "Dining charges final. Tip disputes require signed receipt.",
    refundDisclosure: "Tip line presented on receipt for cardholder to complete before signing.",
    refundAck: "Customer signed receipt with final total.",
    fulfillment: null,
    threeDSRecord: null
  }
},

"4807": {
  fav: "issuer", desc: "Warning Bulletin File",
  product: "a fuel purchase at a gas station",
  merchant: "QuickFuel Station #247",
  issuer: {
    dispute: "The cardholder's card was reported stolen [3 days before this transaction] and placed on the Mastercard Warning Bulletin the same day. Despite this, a $68.45 fuel purchase at QuickFuel Station #247 was processed using the stolen card via magnetic stripe at a pay-at-pump terminal. The cardholder did not make this transaction.",
    contactedMerchant: false,
    merchantResponse: "N/A — card was stolen; cardholder had no interaction with this merchant.",
    resolution: "Full reversal. Merchant failed to check the Warning Bulletin.",
    docs: [
      { type: "Warning Bulletin Listing Record", desc: "Confirmation card was listed on Warning Bulletin [3 days before the transaction]." },
      { type: "Stolen Card Report", desc: "Police report and bank notification of stolen card." }
    ],
    commentary: "Filing under 4807. Card was on the Mastercard Warning Bulletin at the time of this transaction. The merchant was obligated to check the bulletin and decline the card.",
    cardPresent: true, posEntry: "90",
    avs: { code: "N/A", desc: "Card-present fuel pump" },
    cvv: { code: "N/A", desc: "Magnetic stripe CVV1" },
    riskFlags: { geoMismatch: "Yes — transaction 200+ miles from cardholder's home", deviceTrust: "N/A" }
  },
  acquirer: {
    items: [{ name: "Unleaded Fuel — 18.5 gallons", qty: 1, price: 68.45, sku: "FUEL-UNL" }],
    orderStatus: "Completed — fuel dispensed",
    shippingAddr: null,
    orderNotes: "Pay-at-pump fuel transaction. The pump terminal does not perform Warning Bulletin checks; it relies on the electronic authorization response.",
    emails: [
      { type: "No Communications", dir: "system_note", timing: "N/A",
        subject: "N/A", body: "Pay-at-pump transaction. Customer inserted card, authorization approved, fuel dispensed. No customer interaction with staff." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Magnetic Stripe — Pay at Pump", threeDS: null,
    authNotes: "Authorization requested and approved by issuer's system. The pump terminal relies solely on the electronic authorization response and does not cross-reference the Warning Bulletin for automated transactions.",
    settlementNotes: "Settled at $68.45 for fuel dispensed. Pay-at-pump terminal processed card automatically upon receiving approval.",
    riskDevice: { status: "N/A — automated fuel pump", trust: "N/A", match: false },
    riskIP: { level: "N/A", proxy: false, geoMatch: false, notes: "Card-present at fuel pump." },
    riskScore: "N/A — automated terminal",
    avsCode: "Y", avsDesc: "ZIP code entered at pump matched billing ZIP",
    cvvCode: "N/A", cvvDesc: "Magnetic stripe CVV1 on track data",
    refundWindow: "N/A — fuel purchase",
    refundPolicy: "Fuel purchases non-refundable once dispensed.",
    refundDisclosure: "Posted at the pump.",
    refundAck: "Customer initiated dispensing by inserting card.",
    fulfillment: null,
    threeDSRecord: null
  }
},

"4808": {
  fav: "acquirer", desc: "Authorization-Related Chargeback",
  product: "a refurbished laptop from an online electronics store",
  merchant: "RenewTech Store",
  issuer: {
    dispute: "The cardholder states they did not authorize a $549.00 purchase from 'RENEWTECH STORE' for a refurbished laptop. The cardholder claims they never visited the merchant's website and their card was in their possession at the time.",
    contactedMerchant: true,
    merchantResponse: "The cardholder contacted RenewTech Store [3 days after the charge appeared]. The merchant said the order was placed from a verified account with 3D Secure and was delivered to the cardholder's address with a signature. The merchant refused a refund.",
    resolution: "Reversal of the $549.00 charge pending review of authorization records.",
    docs: [
      { type: "Cardholder Dispute Form", desc: "Signed form stating cardholder did not authorize the purchase." },
      { type: "Account Activity Report", desc: "Issuer's records showing the authorization approval." }
    ],
    commentary: "Filing under 4808. The cardholder disputes authorizing this transaction. While an authorization approval exists, the cardholder maintains they did not initiate the purchase.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Street address and ZIP match" },
    cvv: { code: "M", desc: "CVV Match" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "Refurbished ThinkPad T14 Laptop — 16GB RAM, 512GB SSD", qty: 1, price: 549.00, sku: "RT-T14-R" }],
    orderStatus: "Completed — delivered and signed for",
    shippingAddr: "Matches cardholder's billing address exactly",
    orderNotes: "Customer created account, completed checkout with 3D Secure authentication, shipped to billing address, signed for at delivery.",
    emails: [
      { type: "Order Confirmation", dir: "merchant_to_customer", timing: "Immediately after purchase",
        subject: "Your RenewTech Order #RT-44821 — Refurbished ThinkPad T14",
        body: "Thank you for your purchase! Your Refurbished ThinkPad T14 (16GB RAM, 512GB SSD) is confirmed. Total: $549.00. Shipping to your address on file. Delivery: [5-7 business days]." },
      { type: "Shipping Notification", dir: "merchant_to_customer", timing: "[2 days after purchase]",
        subject: "Your RenewTech Order #RT-44821 Has Shipped",
        body: "Your laptop has shipped via UPS Ground! Tracking number included. Estimated delivery: [5 days after purchase]." },
      { type: "Delivery Confirmation", dir: "merchant_to_customer", timing: "[5 days after purchase]",
        subject: "Your RenewTech Order #RT-44821 — Delivered",
        body: "Your package has been delivered and signed for at your shipping address. If you have any issues, contact us within 30 days." },
      { type: "Customer Dispute", dir: "customer_to_merchant", timing: "[8 days after purchase]",
        subject: "I did not make this purchase",
        body: "I see a charge of $549.00 from your store on my statement. I did not authorize this purchase. Please reverse this charge immediately." },
      { type: "Merchant Response", dir: "merchant_to_customer", timing: "[9 days after purchase]",
        subject: "RE: I did not make this purchase",
        body: "We have reviewed your order #RT-44821. The purchase was authenticated via 3D Secure (Mastercard Identity Check) with a one-time passcode sent to your registered phone. The billing and shipping addresses match, and the package was delivered and signed for at your address [5 days after purchase]. Our records confirm this was a verified transaction. If you believe your card was used without consent, please contact your bank." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce",
    threeDS: { enrolled: true, authenticated: true, eci: "05", cavv: "Verified — Mastercard Identity Check" },
    authNotes: "Real-time authorization with 3D Secure challenge completed. AVS and CVV both matched. Amount matches settlement exactly.",
    settlementNotes: "Settled for $549.00. Delivered to billing address with signature confirmation.",
    riskDevice: { status: "Known device — matches 2 prior successful purchases on this account", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "IP geolocates to same city as billing address." },
    riskScore: "Low — all verification checks passed",
    avsCode: "Y", avsDesc: "Full match — street and ZIP",
    cvvCode: "M", cvvDesc: "CVV matches issuer records",
    refundWindow: "30 days from delivery date",
    refundPolicy: "Full refund within 30 days if item returned in original condition. Return label provided.",
    refundDisclosure: "Displayed at checkout and in order confirmation email.",
    refundAck: "Customer agreed to terms during checkout.",
    fulfillment: { type: "Physical Shipment", status: "Delivered — signed for at billing address",
      method: "UPS Ground — signature required", timing: "[5 days after purchase]", confirmed: true,
      notes: "Package delivered and signed for. Signature on file matches cardholder name." },
    threeDSRecord: { version: "2.0", enrolled: true, status: "Y — Fully Authenticated", eci: "05",
      cavv: "Verified by issuer", challenge: true, liabilityShift: "Yes — liability shifted to issuer",
      notes: "One-time passcode sent to cardholder's registered phone and entered correctly." }
  }
},

"4809": {
  fav: "acquirer", desc: "Non-Receipt of Requested Information",
  product: "a professional chef's knife set from a cookware retailer",
  merchant: "CulinaryEdge Pro",
  issuer: {
    dispute: "The cardholder does not recognize a $189.95 charge from 'CULINARYEDGE PRO'. The issuer submitted a retrieval request [12 days after the transaction]. The issuer's records show no response was received from the acquirer.",
    contactedMerchant: false,
    merchantResponse: "N/A — retrieval request sent through Mastercard channels to acquirer.",
    resolution: "Full reversal due to non-receipt of requested information.",
    docs: [
      { type: "Retrieval Request", desc: "Copy of retrieval request submitted to acquirer." },
      { type: "Response Tracking", desc: "System log showing no response recorded." }
    ],
    commentary: "Filing under 4809. Retrieval request submitted and no documentation was received in response.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Full match" },
    cvv: { code: "M", desc: "CVV Match" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "Professional Chef's Knife Set — 8-piece, German steel", qty: 1, price: 189.95, sku: "CE-KS8-GS" }],
    orderStatus: "Completed — delivered and signed for",
    shippingAddr: "Cardholder's billing address",
    orderNotes: "Order placed online. Shipped and delivered successfully with signature.",
    emails: [
      { type: "Order Confirmation", dir: "merchant_to_customer", timing: "Immediately after purchase",
        subject: "CulinaryEdge Pro — Order #CE-29184",
        body: "Thank you for your purchase of the Professional Chef's Knife Set (8-piece, German steel). Total: $189.95. Ships within 1-2 business days." },
      { type: "Shipping Confirmation", dir: "merchant_to_customer", timing: "[1 day after purchase]",
        subject: "Your CulinaryEdge Pro Order Has Shipped — #CE-29184",
        body: "Your knife set has shipped via FedEx. Tracking number included. Expected delivery: [4-6 business days after purchase]." },
      { type: "Delivery Confirmation", dir: "merchant_to_customer", timing: "[5 days after purchase]",
        subject: "Your CulinaryEdge Pro Order Delivered — #CE-29184",
        body: "Your package has been delivered and signed for at your address. Enjoy your new knife set!" }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce",
    threeDS: { enrolled: true, authenticated: true, eci: "05", cavv: "Verified" },
    authNotes: "Authorization obtained. 3D Secure completed. Full documentation was sent in response to the retrieval request [5 days after receipt], including sales receipt, auth record, and delivery confirmation. Transmission confirmation on file.",
    settlementNotes: "Settled for $189.95. Acquirer responded to retrieval request with complete documentation within the required timeframe. Transmission log confirms delivery of response.",
    riskDevice: { status: "Recognized device — matches prior orders", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "IP consistent with billing address." },
    riskScore: "Low",
    avsCode: "Y", avsDesc: "Full match",
    cvvCode: "M", cvvDesc: "CVV matches",
    refundWindow: "60 days from delivery",
    refundPolicy: "Full refund within 60 days if items returned in original condition.",
    refundDisclosure: "Displayed at checkout and in confirmation email.",
    refundAck: "Customer agreed to terms during checkout.",
    fulfillment: { type: "Physical Shipment", status: "Delivered — signed for",
      method: "FedEx Ground — signature required", timing: "[5 days after purchase]", confirmed: true,
      notes: "Package delivered and signed for. No delivery exceptions." },
    threeDSRecord: { version: "2.0", enrolled: true, status: "Y — Fully Authenticated", eci: "05",
      cavv: "Verified", challenge: true, liabilityShift: "Yes",
      notes: "3D Secure authentication completed successfully." }
  }
},

"4812": {
  fav: "issuer", desc: "Account Number Not on File",
  product: "clothing from a department store",
  merchant: "Metro Fashion Department Store",
  issuer: {
    dispute: "A $294.00 transaction from 'METRO FASHION DEPT STORE' appeared on the cardholder's account. Upon investigation, the account number used does not correspond to any active or previously issued card for this cardholder. The cardholder confirms they have never shopped at this store.",
    contactedMerchant: false,
    merchantResponse: "N/A — the cardholder has no knowledge of this transaction or this merchant.",
    resolution: "Full reversal. The account number used is not on file with the issuing bank.",
    docs: [
      { type: "Account Verification Report", desc: "Issuer verification confirming the account number does not match any card issued to this cardholder." },
      { type: "Cardholder Statement", desc: "Signed statement confirming cardholder did not make this purchase." }
    ],
    commentary: "Filing under 4812. The account number used does not match any card issued to this cardholder. This appears to be a processing error from a manually keyed transaction.",
    cardPresent: true, posEntry: "01",
    avs: { code: "N", desc: "No match" },
    cvv: { code: "N", desc: "CVV not verified — manually keyed" },
    riskFlags: { geoMismatch: "Yes — cardholder not in the city where store is located", deviceTrust: "N/A" }
  },
  acquirer: {
    items: [{ name: "Women's winter coat — size M", qty: 1, price: 229.00, sku: "MF-WC-M-BK" }, { name: "Cashmere scarf — grey", qty: 1, price: 65.00, sku: "MF-CS-GR" }],
    orderStatus: "Completed — customer left with merchandise",
    shippingAddr: null,
    orderNotes: "In-store purchase. Card number manually keyed by cashier after chip reader failed to read the card. Customer left with items.",
    emails: [
      { type: "No Email Communications", dir: "system_note", timing: "N/A",
        subject: "N/A", body: "In-store card-present transaction. Card number was manually entered at POS after chip read failure. No email collected. The possibility exists that a digit was keyed incorrectly, resulting in a different account being charged." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Manual Key Entry — chip fallback", threeDS: null,
    authNotes: "Cashier manually keyed card number after chip reader failed. Authorization was obtained, but possible data entry error may have charged wrong account number.",
    settlementNotes: "Settled for $294.00. Merchant acknowledges manual key entry introduces the possibility of a digit error.",
    riskDevice: { status: "N/A — card-present POS", trust: "N/A", match: false },
    riskIP: { level: "N/A", proxy: false, geoMatch: false, notes: "Card-present at store." },
    riskScore: "Medium — manual key entry increases error risk",
    avsCode: "N", avsDesc: "Address not verified — manual entry",
    cvvCode: "N", cvvDesc: "CVV not verified during manual entry",
    refundWindow: "30 days from purchase with receipt",
    refundPolicy: "Full refund within 30 days with receipt. Items must be unworn with tags.",
    refundDisclosure: "Printed on receipt and posted at register.",
    refundAck: "Receipt provided at point of sale.",
    fulfillment: null,
    threeDSRecord: null
  }
},

"4831": {
  fav: "acquirer", desc: "Transaction Amount Differs",
  product: "a dinner at a restaurant",
  merchant: "The Olive Garden Terrace",
  issuer: {
    dispute: "The cardholder dined at 'THE OLIVE GARDEN TERRACE' and the bill presented was $85.00. The posted amount is $102.00 — $17.00 more. The cardholder believes they were overcharged and is uncertain about the tip amount shown on the receipt.",
    contactedMerchant: true,
    merchantResponse: "The cardholder called the restaurant [4 days after dining]. The staff said they would check records but never called back.",
    resolution: "Reversal of the $17.00 difference between expected and posted amounts.",
    docs: [
      { type: "Cardholder Dispute Form", desc: "Statement indicating expected amount was $85.00." },
      { type: "Account Statement", desc: "Showing posted amount of $102.00." }
    ],
    commentary: "Filing under 4831. Posted amount of $102.00 differs from the cardholder's expected amount of $85.00.",
    cardPresent: true, posEntry: "05",
    avs: { code: "N/A", desc: "Card-present EMV" },
    cvv: { code: "N/A", desc: "Chip read" },
    riskFlags: { geoMismatch: "None", deviceTrust: "N/A" }
  },
  acquirer: {
    items: [
      { name: "Dinner entrees (2)", qty: 2, price: 28.00, sku: "OGT-ENT" },
      { name: "Appetizer — bruschetta", qty: 1, price: 14.00, sku: "OGT-APP" },
      { name: "House wine (2 glasses)", qty: 2, price: 7.50, sku: "OGT-BEV" }
    ],
    orderStatus: "Completed",
    shippingAddr: null,
    orderNotes: "Dine-in. Subtotal: $85.00. Tip: $17.00 (20%). Total: $102.00. Signed receipt on file with tip amount in cardholder's handwriting.",
    emails: [
      { type: "No Email — Card-Present Dining", dir: "system_note", timing: "N/A",
        subject: "N/A", body: "Card-present restaurant transaction. The cardholder signed a receipt with the itemized subtotal of $85.00 and added a handwritten tip of $17.00 (20%), bringing the total to $102.00. The signed receipt clearly shows the cardholder's handwriting for the tip amount and the final total. The cardholder called [4 days after dining] but the follow-up call was missed by staff." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "EMV Chip — Card Present", threeDS: null,
    authNotes: "Authorization for base amount of $85.00. Final settlement of $102.00 includes $17.00 tip added by cardholder on the signed receipt. Tip is within Mastercard's permitted tolerance for restaurant transactions.",
    settlementNotes: "Base: $85.00. Tip: $17.00. Total settled: $102.00. Signed receipt with tip in cardholder's handwriting is available. The 20% tip is within standard restaurant tip adjustment tolerances.",
    riskDevice: { status: "N/A — card-present restaurant", trust: "N/A", match: true },
    riskIP: { level: "N/A", proxy: false, geoMatch: true, notes: "Card used at restaurant's physical location." },
    riskScore: "Low — standard restaurant tip adjustment",
    avsCode: "N/A", avsDesc: "Card-present EMV",
    cvvCode: "N/A", cvvDesc: "Chip cryptogram verified",
    refundWindow: "N/A — restaurant",
    refundPolicy: "Dining charges final. Tip line presented on receipt for cardholder to fill in and sign.",
    refundDisclosure: "Tip line and total line on receipt.",
    refundAck: "Cardholder signed receipt with $102.00 total including $17.00 tip in their own handwriting.",
    fulfillment: null,
    threeDSRecord: null
  }
},

"4834": {
  fav: "acquirer", desc: "Point of Interaction Error / Duplicate Processing",
  product: "two separate mobile accessory orders",
  merchant: "PhoneShield Pro",
  issuer: {
    dispute: "The cardholder's statement shows two charges of $79.99 each from 'PHONESHIELD PRO' on the same day. The cardholder states they only made one purchase and the second is a duplicate.",
    contactedMerchant: true,
    merchantResponse: "The cardholder emailed PhoneShield Pro [3 days after the charges]. The merchant replied that both are separate orders but the cardholder doesn't recall placing two.",
    resolution: "Reversal of one $79.99 charge as a duplicate.",
    docs: [
      { type: "Account Statement", desc: "Statement showing two identical $79.99 charges from same merchant, same day." },
      { type: "Cardholder Dispute Form", desc: "Signed statement that only one purchase was intended." }
    ],
    commentary: "Filing under 4834. Two charges of $79.99 from the same merchant on the same date. Cardholder asserts only one purchase was made.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Full match" },
    cvv: { code: "M", desc: "CVV Match" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [
      { name: "Order #PS-88210: Premium Leather Phone Case — iPhone 15 Pro Max", qty: 1, price: 79.99, sku: "PS-LC-15PM" },
      { name: "Order #PS-88247: Ultimate Screen Protector Kit — iPhone 15 Pro Max (3-pack with installation tool)", qty: 1, price: 79.99, sku: "PS-SP3-15PM" }
    ],
    orderStatus: "Both orders completed and delivered",
    shippingAddr: "Both shipped to cardholder's billing address",
    orderNotes: "Two separate orders placed 20 minutes apart. Order #PS-88210 at 2:15 PM (phone case) and #PS-88247 at 2:35 PM (screen protector kit). Each has unique order ID, unique SKU, different product, and separate authorization code.",
    emails: [
      { type: "Order Confirmation — First", dir: "merchant_to_customer", timing: "Immediately after first purchase",
        subject: "PhoneShield Pro — Order #PS-88210",
        body: "Thank you! Your Premium Leather Phone Case for iPhone 15 Pro Max is confirmed. Total: $79.99. Delivery: [4-6 business days]." },
      { type: "Order Confirmation — Second", dir: "merchant_to_customer", timing: "[20 minutes after first purchase]",
        subject: "PhoneShield Pro — Order #PS-88247",
        body: "Thank you! Your Ultimate Screen Protector Kit for iPhone 15 Pro Max (3-pack with installation tool) is confirmed. Total: $79.99. Delivery: [4-6 business days]." },
      { type: "Shipping Notification", dir: "merchant_to_customer", timing: "[1 day after purchase]",
        subject: "Your PhoneShield Pro orders have shipped",
        body: "Both orders shipped in separate packages. Order #PS-88210 (phone case) and #PS-88247 (screen protector kit). Two tracking numbers provided." },
      { type: "Customer Inquiry", dir: "customer_to_merchant", timing: "[3 days after purchase]",
        subject: "Duplicate charge on my account",
        body: "I see two charges of $79.99 from your store. I only placed one order for a phone case. Please refund the duplicate charge." },
      { type: "Merchant Response", dir: "merchant_to_customer", timing: "[3 days after purchase]",
        subject: "RE: Duplicate charge on my account",
        body: "Our records show you placed two separate orders 20 minutes apart:\n\n1. Order #PS-88210 at 2:15 PM — Premium Leather Phone Case ($79.99)\n2. Order #PS-88247 at 2:35 PM — Screen Protector Kit ($79.99)\n\nThese are different products with separate order numbers. Both were authenticated via 3D Secure and shipped. If you'd like to return either item, use our return portal within 30 days." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce",
    threeDS: { enrolled: true, authenticated: true, eci: "05", cavv: "Verified on both transactions" },
    authNotes: "Two separate authorizations 20 minutes apart. Auth #1 at 2:15 PM (phone case), Auth #2 at 2:35 PM (screen protector). Each has unique auth code and transaction ID. Different products, not duplicates.",
    settlementNotes: "Two separate settlements of $79.99 each for two distinct orders with unique order IDs, auth codes, and product SKUs. Not duplicate charges.",
    riskDevice: { status: "Same device for both — consistent with one customer placing two orders", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "Same IP for both orders, consistent with cardholder's location." },
    riskScore: "Low — legitimate separate orders",
    avsCode: "Y", avsDesc: "Full match on both",
    cvvCode: "M", cvvDesc: "CVV matched on both",
    refundWindow: "30 days from delivery",
    refundPolicy: "Full refund within 30 days if items returned in unopened condition.",
    refundDisclosure: "Displayed at checkout and in each order confirmation email.",
    refundAck: "Customer agreed to terms during each checkout.",
    fulfillment: { type: "Physical Shipment — two separate packages", status: "Both delivered",
      method: "USPS Priority — separate tracking numbers", timing: "[4 days after purchase] and [5 days after purchase]", confirmed: true,
      notes: "Two packages shipped separately. Both delivered to cardholder's billing address." },
    threeDSRecord: { version: "2.0", enrolled: true, status: "Y — Authenticated on both", eci: "05",
      cavv: "Verified on both", challenge: true, liabilityShift: "Yes — both transactions",
      notes: "3D Secure completed independently on each order." }
  }
},

"4835": {
  fav: "issuer", desc: "Card Not Valid or Expired",
  product: "a gold bracelet from a jewelry store",
  merchant: "Brilliance Jewelers",
  issuer: {
    dispute: "The cardholder's Mastercard expired [2 months before this transaction]. A replacement card with a new number was issued and the expired card was destroyed. A $385.00 charge from 'BRILLIANCE JEWELERS' was processed using the expired card number via magnetic stripe. The cardholder did not visit this store or make this purchase.",
    contactedMerchant: false,
    merchantResponse: "N/A — card was expired and destroyed; cardholder has no knowledge of this transaction.",
    resolution: "Full reversal. The card used was expired and no longer valid.",
    docs: [
      { type: "Card Lifecycle Record", desc: "Records showing card expired [2 months before this transaction] and replacement issued on a different number." },
      { type: "Cardholder Statement", desc: "Cardholder confirms expired card was destroyed." }
    ],
    commentary: "Filing under 4835. The card expired [2 months before this transaction]. Per Mastercard rules, merchants must verify card validity before completing a transaction.",
    cardPresent: true, posEntry: "90",
    avs: { code: "N/A", desc: "Card-present" },
    cvv: { code: "N/A", desc: "Magnetic stripe" },
    riskFlags: { geoMismatch: "Unknown", deviceTrust: "N/A" }
  },
  acquirer: {
    items: [{ name: "14K Gold Rope Bracelet — 7 inch", qty: 1, price: 385.00, sku: "BJ-GRB-7" }],
    orderStatus: "Completed — customer left with merchandise",
    shippingAddr: null,
    orderNotes: "In-store purchase. Card swiped via magnetic stripe. POS terminal did not flag card as expired. Cashier did not manually check expiration date on the card.",
    emails: [
      { type: "No Communications", dir: "system_note", timing: "N/A",
        subject: "N/A", body: "In-store purchase. Card swiped at POS via magnetic stripe, authorization approved, customer left with gold bracelet. Cashier did not manually verify expiration date printed on the physical card." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Magnetic Stripe Swipe", threeDS: null,
    authNotes: "Authorization submitted with expired card number and approved. POS terminal does not independently check expiration — it relies on issuer's auth response. Authorization was approved despite the card being expired.",
    settlementNotes: "Settled for $385.00. Merchant relied on authorization approval and did not verify card's printed expiration date.",
    riskDevice: { status: "N/A — card-present POS", trust: "N/A", match: false },
    riskIP: { level: "N/A", proxy: false, geoMatch: false, notes: "Card-present." },
    riskScore: "Medium-High — expired card accepted via magnetic stripe",
    avsCode: "N/A", avsDesc: "Card-present",
    cvvCode: "N/A", cvvDesc: "CVV1 on magnetic stripe — not independently verified",
    refundWindow: "14 days from purchase for jewelry",
    refundPolicy: "Full refund within 14 days with receipt. Items must be unworn.",
    refundDisclosure: "Printed on receipt and posted at register.",
    refundAck: "Receipt provided at point of sale.",
    fulfillment: null,
    threeDSRecord: null
  }
},

"4837": {
  fav: "issuer", desc: "No Cardholder Authorization (Fraud)",
  product: "designer sunglasses from an online luxury retailer",
  merchant: "LuxeVision Online",
  issuer: {
    dispute: "The cardholder denies authorizing or participating in a $327.00 purchase from 'LUXEVISION ONLINE' for designer sunglasses. The cardholder's card was in their possession at all times. They have never heard of this merchant. The IP address used for the transaction is in a different country from the cardholder's residence, and the device fingerprint does not match any known device.",
    contactedMerchant: false,
    merchantResponse: "N/A — the cardholder has no relationship with this merchant and did not visit their website.",
    resolution: "Full reversal as a fraudulent unauthorized transaction.",
    docs: [
      { type: "Cardholder Affidavit of Fraud", desc: "Sworn statement that cardholder did not authorize or make this purchase." },
      { type: "Device/IP Analysis", desc: "Issuer's analysis showing IP and device mismatch with cardholder's profile." }
    ],
    commentary: "Filing under 4837. The cardholder categorically denies this transaction. IP geolocation and device analysis are inconsistent with the cardholder's known profile. No 3D Secure authentication was performed. This appears to be a card-not-present fraud.",
    cardPresent: false, posEntry: "81",
    avs: { code: "N", desc: "Address does not match" },
    cvv: { code: "M", desc: "CVV matched — but card data may have been compromised" },
    riskFlags: { geoMismatch: "Yes — transaction IP is in a different country from cardholder's home", deviceTrust: "Low — unknown device not in cardholder's profile" }
  },
  acquirer: {
    items: [{ name: "Designer Aviator Sunglasses — Polarized, Gold Frame", qty: 1, price: 327.00, sku: "LV-AV-GLD" }],
    orderStatus: "Completed — shipped to address on order",
    shippingAddr: "Different from cardholder's billing address — shipped to a forwarding address",
    orderNotes: "Order placed online. Shipping address does not match billing address. 3D Secure was not enabled on the merchant's payment gateway at the time of this transaction.",
    emails: [
      { type: "Order Confirmation", dir: "merchant_to_customer", timing: "Immediately after purchase",
        subject: "LuxeVision Online — Order #LV-7829",
        body: "Your order for Designer Aviator Sunglasses (Polarized, Gold Frame) is confirmed. Total: $327.00. Shipping to: [address different from billing]." },
      { type: "Shipping Notification", dir: "merchant_to_customer", timing: "[1 day after purchase]",
        subject: "Your LuxeVision Order Has Shipped — #LV-7829",
        body: "Your sunglasses have shipped. Tracking number included." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce", threeDS: null,
    authNotes: "Authorization obtained. However, 3D Secure was NOT enabled on the merchant's payment gateway at the time. AVS returned a mismatch — the billing address provided did not match the cardholder's address on file. The merchant proceeded despite the AVS mismatch.",
    settlementNotes: "Settled for $327.00. Shipped to an address that does not match the billing address. No 3D Secure authentication performed.",
    riskDevice: { status: "Unknown device — not seen before on any account", trust: "Low", match: false },
    riskIP: { level: "High", proxy: true, geoMatch: false, notes: "IP address originates from a VPN/proxy service in a different country from the billing address." },
    riskScore: "High — AVS mismatch, unknown device, foreign IP via proxy, no 3DS",
    avsCode: "N", avsDesc: "No match — billing address does not match",
    cvvCode: "M", cvvDesc: "CVV matched — card data may have been obtained through a breach",
    refundWindow: "30 days from delivery",
    refundPolicy: "Full refund within 30 days if returned in original condition.",
    refundDisclosure: "Policy on website.",
    refundAck: "Standard website terms.",
    fulfillment: { type: "Physical Shipment", status: "Delivered to shipping address (not billing address)",
      method: "Express shipping", timing: "[3 days after purchase]", confirmed: true,
      notes: "Package delivered to the shipping address on the order, which is different from the cardholder's billing address. Shipped to what appears to be a package forwarding facility." },
    threeDSRecord: null
  }
},

"4840": {
  fav: "issuer", desc: "Fraudulent Processing of Transaction",
  product: "gift cards from an online gift card marketplace",
  merchant: "GiftCardVault",
  issuer: {
    dispute: "The cardholder reports a $500.00 purchase from 'GIFTCARDVAULT' for digital gift cards they did not authorize. The cardholder has never used this merchant. Investigation suggests the card data was compromised in a data breach. Multiple gift cards were purchased and instantly redeemed, a pattern consistent with fraud.",
    contactedMerchant: false,
    merchantResponse: "N/A — the cardholder did not make this purchase and has no account with this merchant.",
    resolution: "Full reversal due to fraudulent processing.",
    docs: [
      { type: "Fraud Investigation Report", desc: "Issuer's fraud analysis showing card data was likely compromised in a third-party breach." },
      { type: "Cardholder Affidavit", desc: "Sworn statement that cardholder did not make or authorize this transaction." }
    ],
    commentary: "Filing under 4840. Transaction was fraudulently processed using compromised card data. Multiple gift cards purchased and immediately redeemed — a known fraud pattern. Cardholder did not participate.",
    cardPresent: false, posEntry: "81",
    avs: { code: "N", desc: "No match" },
    cvv: { code: "M", desc: "CVV matched — card data from breach includes CVV" },
    riskFlags: { geoMismatch: "Yes — IP from a different country", deviceTrust: "Low — unknown device, Tor exit node" }
  },
  acquirer: {
    items: [
      { name: "Digital Gift Card — $100 denomination", qty: 5, price: 100.00, sku: "GCV-DGC-100" }
    ],
    orderStatus: "Completed — gift cards delivered electronically and redeemed",
    shippingAddr: null,
    orderNotes: "Five $100 digital gift cards purchased online and delivered to an email address. All five cards were redeemed within [30 minutes of purchase]. The merchant did not have advanced fraud screening in place for digital goods at the time.",
    emails: [
      { type: "Order Confirmation & Delivery", dir: "merchant_to_customer", timing: "Immediately after purchase",
        subject: "GiftCardVault — Your Gift Cards Are Ready!",
        body: "Your 5 x $100 gift cards have been delivered to the email address provided. Total: $500.00. Gift card codes are included in this email." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce", threeDS: null,
    authNotes: "Authorization obtained. 3D Secure was not implemented for this transaction. AVS did not match. The merchant's fraud rules did not flag this high-risk pattern (multiple gift cards, new account, instant redemption). The merchant acknowledges their fraud screening was insufficient for digital goods.",
    settlementNotes: "Settled for $500.00. Gift cards were instantly delivered and redeemed. Funds are unrecoverable as the gift card balances have been spent.",
    riskDevice: { status: "Unknown device — first-time visitor using a Tor exit node", trust: "Very Low", match: false },
    riskIP: { level: "Critical", proxy: true, geoMatch: false, notes: "IP address identified as a Tor exit node. No match to cardholder's known geography." },
    riskScore: "Critical — all fraud indicators present",
    avsCode: "N", avsDesc: "No match — billing address provided is fictitious",
    cvvCode: "M", cvvDesc: "CVV matched — likely obtained from data breach",
    refundWindow: "N/A — digital goods",
    refundPolicy: "Digital gift cards are non-refundable once redeemed.",
    refundDisclosure: "Terms displayed at checkout.",
    refundAck: "Purchaser agreed to terms.",
    fulfillment: { type: "Digital Delivery", status: "Delivered — all 5 gift cards redeemed",
      method: "Email delivery", timing: "Immediately after purchase", confirmed: true,
      notes: "Gift card codes sent to email address on file. All 5 cards redeemed within [30 minutes of purchase]. The email address used does not appear to belong to the cardholder." },
    threeDSRecord: null
  }
},

"4841": {
  fav: "acquirer", desc: "Cancelled Recurring or Digital Goods Transaction",
  product: "a monthly cloud storage subscription",
  merchant: "CloudVault Premium",
  issuer: {
    dispute: "The cardholder states they cancelled their CloudVault Premium subscription [7 days before the disputed billing date] via the merchant's website. Despite this, a $14.99 monthly charge was processed. The cardholder believes all charges after the cancellation should stop immediately.",
    contactedMerchant: true,
    merchantResponse: "The cardholder emailed CloudVault [7 days before the billing date] to cancel. The merchant responded acknowledging the cancellation but stated it would take effect at the end of the current billing cycle, not immediately.",
    resolution: "Reversal of the $14.99 post-cancellation charge.",
    docs: [
      { type: "Cardholder Dispute Form", desc: "Statement that the subscription was cancelled before the charge date." },
      { type: "Cardholder's Email Record", desc: "Email from cardholder to merchant requesting cancellation." }
    ],
    commentary: "Filing under 4841. The cardholder cancelled their recurring subscription before the disputed charge. The cardholder expected the cancellation to be immediate.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Full match" },
    cvv: { code: "M", desc: "CVV on file from initial signup" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "CloudVault Premium — Monthly Cloud Storage Plan (2TB)", qty: 1, price: 14.99, sku: "CV-PREM-MO" }],
    orderStatus: "Active subscription — billed monthly",
    shippingAddr: null,
    orderNotes: "Monthly recurring subscription. Cardholder signed up [6 months before the disputed charge]. The cancellation request was received [7 days before the next billing date], but per the terms agreed at signup, cancellation takes effect at the end of the current paid period. The disputed charge is for the final billing cycle.",
    emails: [
      { type: "Original Signup Confirmation", dir: "merchant_to_customer", timing: "[6 months before the disputed charge]",
        subject: "Welcome to CloudVault Premium!",
        body: "Your CloudVault Premium subscription is active! Plan: 2TB Monthly ($14.99/month). Your subscription renews automatically each month. You can cancel anytime, and cancellation takes effect at the end of your current billing period. You will retain access through the end of your paid period." },
      { type: "Billing Reminder", dir: "merchant_to_customer", timing: "[3 days before the disputed charge]",
        subject: "Your CloudVault Premium Renewal",
        body: "Your CloudVault Premium subscription ($14.99) will renew in 3 days. If you wish to cancel, please do so through your account settings. Cancellation takes effect at the end of the current billing period." },
      { type: "Cancellation Request", dir: "customer_to_merchant", timing: "[7 days before the disputed charge]",
        subject: "Cancel my subscription",
        body: "Please cancel my CloudVault Premium subscription immediately. I do not want to be charged again." },
      { type: "Cancellation Acknowledgment", dir: "merchant_to_customer", timing: "[7 days before the disputed charge]",
        subject: "RE: Cancel my subscription — Cancellation Confirmed",
        body: "Your cancellation has been processed. As per our terms of service agreed at signup, your cancellation takes effect at the end of your current billing period. You will retain full access to your CloudVault Premium storage until that date. The final charge on your upcoming billing date covers the remainder of your current period. No further charges will be made after that." },
      { type: "Final Billing Notification", dir: "merchant_to_customer", timing: "On the disputed billing date",
        subject: "CloudVault Premium — Final Billing",
        body: "Your final CloudVault Premium charge of $14.99 has been processed. This covers your remaining billing period. Your account will be downgraded to the free plan at the end of this period. Thank you for being a subscriber." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Recurring — card on file",
    threeDS: { enrolled: true, authenticated: true, eci: "05", cavv: "Verified at initial signup" },
    authNotes: "Recurring billing authorized via card-on-file credentials established at initial signup [6 months before]. The initial signup included 3D Secure authentication. The disputed charge is the final billing cycle before cancellation takes effect.",
    settlementNotes: "Settled for $14.99. This is the final charge for the current billing cycle. Cancellation was received but takes effect at end of the paid period per the terms agreed at signup.",
    riskDevice: { status: "Known device — same device used for initial signup and regular logins", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "Login IP consistent with cardholder's location throughout subscription." },
    riskScore: "Low — legitimate recurring charge on established subscription",
    avsCode: "Y", avsDesc: "Full match — on file from initial signup",
    cvvCode: "M", cvvDesc: "CVV on file from initial enrollment",
    refundWindow: "N/A — subscription service",
    refundPolicy: "Cancellation takes effect at end of current billing period. No prorated refunds for partial months. Agreed at signup.",
    refundDisclosure: "Terms displayed during signup, linked in confirmation email, and in account settings.",
    refundAck: "Customer checked 'I agree to the Terms of Service and Billing Policy' during signup.",
    fulfillment: { type: "Digital Service", status: "Active — cardholder continued using service after cancellation request",
      method: "Cloud storage accessible via web and mobile app", timing: "Continuous access since signup", confirmed: true,
      notes: "Usage logs show the cardholder logged into their CloudVault account and accessed files [2 days before the chargeback was filed], [1 day after the disputed charge], and on [4 other occasions after the cancellation request]. Total storage used: 1.2TB of the 2TB plan." },
    threeDSRecord: { version: "2.0", enrolled: true, status: "Y — Authenticated at initial signup", eci: "05",
      cavv: "Verified", challenge: true, liabilityShift: "Yes — at initial enrollment",
      notes: "3D Secure was completed during the initial subscription signup. Subsequent recurring charges use the authenticated card-on-file credentials." }
  }
},

"4842": {
  fav: "issuer", desc: "Late Presentment",
  product: "a hotel stay booked through a travel agency",
  merchant: "SunCoast Travel Agency",
  issuer: {
    dispute: "The cardholder stayed at a hotel booked through 'SUNCOAST TRAVEL AGENCY' [4 months before this charge appeared]. The charge of $312.00 did not appear on the cardholder's statement until now — well beyond the normal processing window. The cardholder had assumed the charge was settled long ago and did not budget for this late posting.",
    contactedMerchant: true,
    merchantResponse: "The cardholder called SunCoast Travel [2 days after noticing the late charge]. The agency acknowledged a processing delay and said their payment system had a backlog. They offered no refund.",
    resolution: "Full reversal due to late presentment outside the Mastercard-permitted window.",
    docs: [
      { type: "Transaction Timeline", desc: "Showing transaction date was [4 months before] the clearing submission." },
      { type: "Cardholder Statement", desc: "Current statement showing the late-posted charge." }
    ],
    commentary: "Filing under 4842. The transaction was presented for clearing [4 months after the transaction date], well beyond Mastercard's maximum presentment window of 30 calendar days from the transaction date.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Match" },
    cvv: { code: "M", desc: "CVV Match" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "Hotel Reservation — 2 nights, Ocean View Room", qty: 1, price: 312.00, sku: "SCT-HVR-2N" }],
    orderStatus: "Completed — guest stayed [4 months ago]",
    shippingAddr: null,
    orderNotes: "Hotel stay was completed [4 months before the clearing submission]. The travel agency's batch processing system experienced an extended outage that delayed the submission of multiple transactions.",
    emails: [
      { type: "Booking Confirmation", dir: "merchant_to_customer", timing: "[4 months before the disputed charge]",
        subject: "SunCoast Travel — Booking Confirmation #SCT-4410",
        body: "Your hotel reservation is confirmed! Ocean View Room for 2 nights. Total: $312.00. Check-in details attached." },
      { type: "Post-Stay Thank You", dir: "merchant_to_customer", timing: "[4 months before the disputed charge, after checkout]",
        subject: "Thank you for staying with SunCoast Travel",
        body: "We hope you enjoyed your stay! Your folio for $312.00 will be charged to the card on file." },
      { type: "Customer Complaint", dir: "customer_to_merchant", timing: "[2 days after the late charge appeared]",
        subject: "Why was I charged 4 months late?",
        body: "I see a charge of $312.00 on my current statement from your agency. My hotel stay was 4 months ago. Why is this being charged now?" },
      { type: "Merchant Response", dir: "merchant_to_customer", timing: "[3 days after the late charge appeared]",
        subject: "RE: Why was I charged 4 months late?",
        body: "We apologize for the delay. Our payment processing system experienced a prolonged technical issue that delayed the submission of your transaction. The charge is for the hotel stay you completed. We understand this is inconvenient and regret the late billing." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce — booking", threeDS: null,
    authNotes: "Authorization was obtained [4 months before the clearing]. The clearing was submitted significantly beyond the permitted window due to a batch processing system outage at the merchant.",
    settlementNotes: "Settlement submitted [4 months after the transaction date]. Mastercard rules require presentment within 30 calendar days. The merchant's system outage caused the delay.",
    riskDevice: { status: "Known device from original booking", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "IP from original booking matches cardholder." },
    riskScore: "Low — legitimate transaction but late presentment",
    avsCode: "Y", avsDesc: "Full match at time of booking",
    cvvCode: "M", cvvDesc: "CVV matched at time of booking",
    refundWindow: "N/A — hotel stay completed",
    refundPolicy: "Hotel stays are non-refundable after checkout.",
    refundDisclosure: "Cancellation policy shown at booking.",
    refundAck: "Customer agreed to cancellation policy during booking.",
    fulfillment: { type: "Hospitality Service", status: "Completed — guest checked in and checked out [4 months before]",
      method: "Hotel stay — 2 nights", timing: "[4 months before the clearing]", confirmed: true,
      notes: "Guest completed the stay. The service was fully rendered but clearing was delayed due to a system issue." },
    threeDSRecord: null
  }
},

"4846": {
  fav: "issuer", desc: "Correct Currency Code Not Provided",
  product: "a leather bag from an international online boutique",
  merchant: "EuroLux Boutique",
  issuer: {
    dispute: "The cardholder purchased a leather bag from 'EUROLUX BOUTIQUE' (based in France). The website displayed the price as $220.00 USD at checkout. However, the cardholder was billed in EUR (€215.00) which, after currency conversion and fees, resulted in a charge of $247.50 — $27.50 more than expected. The cardholder was not informed of the currency change.",
    contactedMerchant: true,
    merchantResponse: "The cardholder contacted EuroLux Boutique [3 days after noticing the charge]. The merchant said their system defaults to EUR for processing regardless of what is displayed in the cart. They offered a €10 store credit but refused a refund of the conversion difference.",
    resolution: "Reversal of the full amount due to incorrect currency code — cardholder expected USD.",
    docs: [
      { type: "Checkout Screenshot", desc: "Cardholder's screenshot of the checkout page showing $220.00 USD." },
      { type: "Account Statement", desc: "Statement showing the charge of $247.50 after EUR conversion." }
    ],
    commentary: "Filing under 4846. The transaction was presented to the cardholder in USD at $220.00 but processed in EUR, resulting in unexpected conversion fees. The merchant did not properly disclose the settlement currency.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Match" },
    cvv: { code: "M", desc: "CVV Match" },
    riskFlags: { geoMismatch: "None — cardholder is in the US, merchant is in France (cross-border is expected)", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "Italian Leather Crossbody Bag — Tan", qty: 1, price: 215.00, sku: "ELB-CB-TAN" }],
    orderStatus: "Completed — delivered",
    shippingAddr: "Cardholder's billing address in the United States",
    orderNotes: "Cross-border transaction. Merchant's payment gateway is configured to process all transactions in EUR regardless of the display currency on the website. The website showed USD but settlement was in EUR.",
    emails: [
      { type: "Order Confirmation", dir: "merchant_to_customer", timing: "Immediately after purchase",
        subject: "EuroLux Boutique — Order Confirmation #ELB-3392",
        body: "Merci for your purchase! Your Italian Leather Crossbody Bag (Tan) is confirmed. Total: €215.00. We will ship to your address within 3-5 business days." },
      { type: "Customer Complaint", dir: "customer_to_merchant", timing: "[3 days after noticing the charge]",
        subject: "Wrong currency charged",
        body: "Your website showed $220.00 USD at checkout but I was charged in EUR which ended up costing me $247.50 after conversion. I agreed to $220 USD, not €215. Please refund the difference." },
      { type: "Merchant Response", dir: "merchant_to_customer", timing: "[4 days after complaint]",
        subject: "RE: Wrong currency charged",
        body: "We apologize for the confusion. Our payment processor handles all transactions in EUR as we are based in France. The USD amount shown on our website is an estimate. We can offer a €10 store credit for the inconvenience but cannot refund the conversion difference." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce", threeDS: null,
    authNotes: "Authorization was requested in EUR (€215.00) despite the website displaying USD. The cardholder's bank applied a currency conversion, resulting in a higher charged amount than what the cardholder saw at checkout.",
    settlementNotes: "Settled in EUR (€215.00). The merchant's payment gateway configuration does not align with the display currency on their website. The cardholder experienced unexpected conversion fees.",
    riskDevice: { status: "Known device", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "US-based cardholder accessing French merchant website — expected for cross-border." },
    riskScore: "Low risk of fraud — but currency configuration error present",
    avsCode: "Y", avsDesc: "Full match",
    cvvCode: "M", cvvDesc: "CVV matched",
    refundWindow: "30 days from delivery",
    refundPolicy: "Full refund within 30 days if item returned in original condition. International return shipping paid by customer.",
    refundDisclosure: "Policy on website in French and English.",
    refundAck: "Customer agreed to terms at checkout.",
    fulfillment: { type: "Physical Shipment — International", status: "Delivered",
      method: "International express — DHL", timing: "[7 days after purchase]", confirmed: true,
      notes: "Package delivered to cardholder's US address via international shipping from France." },
    threeDSRecord: null
  }
},

"4847": {
  fav: "issuer", desc: "Exceeds Floor Limit — Not Authorized and Fraud",
  product: "high-end electronics at a retail store",
  merchant: "ElectroWorld Megastore",
  issuer: {
    dispute: "A $1,250.00 purchase of a high-end television from 'ELECTROWORLD MEGASTORE' was processed without authorization from the issuer. The transaction amount exceeds the applicable floor limit and the merchant was required to obtain real-time authorization. The cardholder's card was stolen [1 day before this transaction] and they did not make this purchase.",
    contactedMerchant: false,
    merchantResponse: "N/A — card was stolen; cardholder did not visit this store.",
    resolution: "Full reversal. Transaction exceeds floor limit, no authorization obtained, and the transaction is fraudulent.",
    docs: [
      { type: "Stolen Card Report", desc: "Police report filed by the cardholder." },
      { type: "Authorization Log", desc: "Issuer's records showing no authorization request was received for this transaction." }
    ],
    commentary: "Filing under 4847. The $1,250.00 transaction exceeded the floor limit and no authorization was obtained. The card was reported stolen. The merchant was obligated to request authorization for above-floor-limit transactions.",
    cardPresent: true, posEntry: "90",
    avs: { code: "N/A", desc: "Card-present" },
    cvv: { code: "N/A", desc: "Magnetic stripe" },
    riskFlags: { geoMismatch: "Yes — different city from cardholder's home", deviceTrust: "N/A" }
  },
  acquirer: {
    items: [{ name: "65-inch 4K OLED Smart TV", qty: 1, price: 1250.00, sku: "EW-TV65-OLED" }],
    orderStatus: "Completed — customer left with merchandise",
    shippingAddr: null,
    orderNotes: "In-store purchase. Merchant's POS terminal was in offline mode due to a network connectivity issue. The transaction was processed without real-time authorization under offline fallback procedures. The amount exceeds the floor limit.",
    emails: [
      { type: "No Communications", dir: "system_note", timing: "N/A",
        subject: "N/A", body: "In-store purchase. Customer paid with a magnetic stripe swipe while the POS terminal was offline. The cashier processed the sale under offline fallback procedures. No authorization was obtained from the issuer because the terminal could not connect. The customer left with the TV." }
    ],
    authObtained: false, authResponse: "N/A", authMessage: "No authorization requested — terminal offline",
    entryMode: "Magnetic Stripe — Offline Mode", threeDS: null,
    authNotes: "No real-time authorization was obtained. The POS terminal was in offline mode due to a network outage. The merchant processed the $1,250.00 transaction without contacting the issuer. This exceeds the applicable floor limit, which requires authorization.",
    settlementNotes: "Settled for $1,250.00 without prior authorization. The merchant's terminal was offline and the transaction was batched for later submission.",
    riskDevice: { status: "N/A — offline POS terminal", trust: "N/A", match: false },
    riskIP: { level: "N/A", proxy: false, geoMatch: false, notes: "Card-present offline transaction." },
    riskScore: "High — no authorization, above floor limit, offline processing",
    avsCode: "N/A", avsDesc: "Not available — offline transaction",
    cvvCode: "N/A", cvvDesc: "Magnetic stripe CVV1 only",
    refundWindow: "30 days from purchase with receipt",
    refundPolicy: "Full refund within 30 days with original receipt and packaging.",
    refundDisclosure: "Printed on receipt and posted at service desk.",
    refundAck: "Receipt provided at POS.",
    fulfillment: null,
    threeDSRecord: null
  }
},

"4849": {
  fav: "issuer", desc: "Questionable Merchant Activity",
  product: "an online weight-loss supplement subscription",
  merchant: "SlimFit Wellness",
  issuer: {
    dispute: "The issuing bank has received complaints from multiple cardholders about unauthorized charges from 'SLIMFIT WELLNESS'. This cardholder reports a $89.99 charge they did not authorize. The cardholder never visited this merchant's website or ordered any supplements. A pattern of unauthorized transactions from this merchant has been identified across several issuing banks.",
    contactedMerchant: false,
    merchantResponse: "N/A — the cardholder has no knowledge of this merchant. Other cardholders report similar unauthorized charges.",
    resolution: "Full reversal due to questionable merchant activity — pattern of unauthorized charges identified.",
    docs: [
      { type: "Multi-Cardholder Complaint Summary", desc: "Summary of complaints received from multiple cardholders about unauthorized charges from this merchant." },
      { type: "Cardholder Affidavit", desc: "Sworn statement from this cardholder that they did not authorize this purchase." }
    ],
    commentary: "Filing under 4849. Pattern of questionable activity identified: multiple cardholders across different issuing banks have reported unauthorized charges from this merchant. This cardholder is one of several affected.",
    cardPresent: false, posEntry: "81",
    avs: { code: "N", desc: "No match" },
    cvv: { code: "N", desc: "CVV not verified" },
    riskFlags: { geoMismatch: "Unknown", deviceTrust: "Low" }
  },
  acquirer: {
    items: [{ name: "SlimFit Advanced Weight Loss Formula — 30-day supply", qty: 1, price: 89.99, sku: "SF-AWL-30" }],
    orderStatus: "Completed — shipped",
    shippingAddr: "Address on file does not match cardholder's billing address",
    orderNotes: "Order placed through the merchant's website. The merchant maintains all transactions were legitimately processed. A recent security audit was conducted.",
    emails: [
      { type: "Order Confirmation", dir: "merchant_to_customer", timing: "Immediately after purchase",
        subject: "SlimFit Wellness — Your Order",
        body: "Thank you for ordering SlimFit Advanced Weight Loss Formula! Total: $89.99. Ships within 2-3 business days." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce", threeDS: null,
    authNotes: "Authorization obtained. No 3D Secure authentication was performed. AVS did not match. The merchant has received multiple chargeback complaints from different cardholders.",
    settlementNotes: "Settled for $89.99. The merchant's overall chargeback ratio has been flagged as elevated by the acquirer.",
    riskDevice: { status: "Unknown device — no prior history", trust: "Low", match: false },
    riskIP: { level: "Medium", proxy: false, geoMatch: false, notes: "IP does not correspond to the cardholder's known location." },
    riskScore: "High — multiple complaints, elevated chargeback ratio",
    avsCode: "N", avsDesc: "No match",
    cvvCode: "N", cvvDesc: "CVV not submitted",
    refundWindow: "30 days from shipment",
    refundPolicy: "Full refund within 30 days if product returned unopened. Fine print: subscription auto-enrolls after trial.",
    refundDisclosure: "Terms on website — small print at bottom of product page.",
    refundAck: "Purchaser agreed to terms — however, this cardholder states they never visited the website.",
    fulfillment: { type: "Physical Shipment", status: "Shipped to address on file (does not match cardholder)",
      method: "Standard USPS", timing: "[3-5 business days after purchase]", confirmed: false,
      notes: "Shipped to the address provided during checkout, which does not match the cardholder's billing address. Delivery confirmation is not available." },
    threeDSRecord: null
  }
},

"4850": {
  fav: "acquirer", desc: "Installment Billing Dispute",
  product: "a home gym equipment package on a 12-month installment plan",
  merchant: "FitPro Equipment Co.",
  issuer: {
    dispute: "The cardholder disputes a $125.00 installment payment from 'FITPRO EQUIPMENT CO.' The cardholder states the per-installment amount was supposed to be $99.00 based on what was shown during checkout. The $125.00 charged is higher than expected.",
    contactedMerchant: true,
    merchantResponse: "The cardholder contacted FitPro [10 days after the disputed installment]. The merchant referenced the signed agreement and said the $99 was a promotional estimate and the actual installment is $125.00 per the terms. The cardholder disagrees.",
    resolution: "Reversal of the $26.00 difference between expected and actual installment amount.",
    docs: [
      { type: "Cardholder Dispute Form", desc: "Statement indicating expected installment of $99.00." },
      { type: "Account Statement", desc: "Showing the $125.00 charge." }
    ],
    commentary: "Filing under 4850. The cardholder disputes the installment amount, claiming it is higher than what was presented during the purchase.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Full match" },
    cvv: { code: "M", desc: "CVV Match" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "FitPro Home Gym Package — Multi-Station, 150lb Stack", qty: 1, price: 1500.00, sku: "FP-HGP-150" }],
    orderStatus: "Active installment plan — installment 4 of 12",
    shippingAddr: "Cardholder's billing address",
    orderNotes: "12-month installment plan at $125.00/month. Total: $1,500.00. Equipment delivered [4 months ago]. The $99/month was a promotional estimate shown during a limited-time sale that had ended before this customer completed checkout. The final agreement signed by the customer shows $125/month.",
    emails: [
      { type: "Order Confirmation & Installment Agreement", dir: "merchant_to_customer", timing: "[4 months before the disputed installment]",
        subject: "FitPro Equipment — Order #FP-9102 & Installment Plan Confirmation",
        body: "Your FitPro Home Gym Package has been confirmed! Total: $1,500.00. Payment plan: 12 monthly installments of $125.00 each. First payment processed today. Your equipment will be delivered within 7-10 business days. Your digitally signed installment agreement is attached." },
      { type: "Delivery Confirmation", dir: "merchant_to_customer", timing: "[8 days after order, 4 months before dispute]",
        subject: "Your FitPro Home Gym Has Been Delivered!",
        body: "Your Home Gym Package has been delivered and set up at your address. Please check all components. Contact us within 7 days if any parts are missing." },
      { type: "Monthly Installment Receipt", dir: "merchant_to_customer", timing: "On the disputed installment date",
        subject: "FitPro Equipment — Installment 4 of 12 Processed",
        body: "Your monthly installment of $125.00 (installment 4 of 12) has been processed. Remaining balance: $1,000.00 (8 installments). Thank you for choosing FitPro." },
      { type: "Customer Complaint", dir: "customer_to_merchant", timing: "[10 days after the disputed installment]",
        subject: "Installment amount is wrong",
        body: "I was told the monthly payment would be $99 when I was shopping on your site. I've been charged $125 each month. This isn't what I agreed to." },
      { type: "Merchant Response", dir: "merchant_to_customer", timing: "[11 days after the disputed installment]",
        subject: "RE: Installment amount is wrong",
        body: "Thank you for reaching out. The $99/month was a limited-time promotional rate that had ended before your order was placed. Your installment agreement, which you digitally signed during checkout, clearly states the rate of $125.00/month for 12 months (total $1,500.00). We have attached a copy of your signed agreement for your reference. All three previous installments were successfully processed at $125.00 without dispute." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Recurring — card on file (installment)",
    threeDS: { enrolled: true, authenticated: true, eci: "05", cavv: "Verified at initial purchase" },
    authNotes: "Recurring installment billing. Initial purchase included 3D Secure. This is installment 4 of 12. The first three installments of $125.00 each were processed without dispute.",
    settlementNotes: "Settled $125.00 for installment 4 of 12. Digitally signed installment agreement specifies $125.00/month. Three prior installments paid at this amount without dispute.",
    riskDevice: { status: "Known device — same as initial purchase", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "IP consistent with cardholder's location." },
    riskScore: "Low — established installment plan with prior payments",
    avsCode: "Y", avsDesc: "Full match",
    cvvCode: "M", cvvDesc: "On file from initial purchase",
    refundWindow: "N/A — installment plan",
    refundPolicy: "Equipment may be returned within 30 days of delivery for a full refund. After 30 days, the installment plan continues per the signed agreement.",
    refundDisclosure: "Terms in the digitally signed installment agreement.",
    refundAck: "Customer digitally signed the installment agreement with the $125/month terms during checkout.",
    fulfillment: { type: "Physical Delivery", status: "Delivered and set up [4 months ago]",
      method: "White-glove delivery and setup", timing: "[8 days after order]", confirmed: true,
      notes: "Equipment delivered, assembled, and inspected by the customer. No issues reported at delivery. Customer has been using the equipment for 4 months." },
    threeDSRecord: { version: "2.0", enrolled: true, status: "Y — Authenticated at initial purchase", eci: "05",
      cavv: "Verified", challenge: true, liabilityShift: "Yes",
      notes: "3D Secure completed at initial purchase. Subsequent installments use authenticated card-on-file." }
  }
},

"4853": {
  fav: "acquirer", desc: "Goods/Services Not as Described or Defective",
  product: "a noise-cancelling wireless headphone from an audio equipment retailer",
  merchant: "AudioPeak Electronics",
  issuer: {
    dispute: "The cardholder purchased noise-cancelling wireless headphones from 'AUDIOPEAK ELECTRONICS' for $199.99. The cardholder states the headphones have significantly weaker noise cancellation than advertised and the battery life is about 12 hours instead of the advertised 30 hours. The cardholder considers the product materially different from the description.",
    contactedMerchant: true,
    merchantResponse: "The cardholder contacted AudioPeak [18 days after receiving the headphones]. The merchant offered a return but the cardholder had already discarded the packaging. The merchant's return policy requires original packaging.",
    resolution: "Full reversal for goods not as described — product does not match advertised specifications.",
    docs: [
      { type: "Cardholder Dispute Form", desc: "Detailed description of how the product differs from the advertisement." },
      { type: "Product Listing Comparison", desc: "Cardholder's notes comparing advertised specs vs actual performance." }
    ],
    commentary: "Filing under 4853. The cardholder received headphones that do not meet the advertised specifications for noise cancellation and battery life.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Full match" },
    cvv: { code: "M", desc: "CVV Match" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "ProSilence ANC Wireless Headphones — Over-Ear, 30hr Battery", qty: 1, price: 199.99, sku: "AP-PS-ANC" }],
    orderStatus: "Completed — delivered",
    shippingAddr: "Cardholder's billing address",
    orderNotes: "Product listing accurately describes specifications. The '30-hour battery life' is rated under standard listening conditions (50% volume, ANC off). With ANC enabled, battery life is approximately 12-15 hours, which is stated in the detailed specifications section. This distinction is disclosed on the product page.",
    emails: [
      { type: "Order Confirmation", dir: "merchant_to_customer", timing: "Immediately after purchase",
        subject: "AudioPeak — Order Confirmation #AP-6674",
        body: "Your ProSilence ANC Wireless Headphones are confirmed! Total: $199.99. Ships within 1-2 business days. Review our return policy: items may be returned within 30 days of delivery in original packaging." },
      { type: "Shipping Notification", dir: "merchant_to_customer", timing: "[1 day after purchase]",
        subject: "Your AudioPeak Order Has Shipped — #AP-6674",
        body: "Your headphones have shipped. Tracking included. Delivery: [3-5 business days after purchase]." },
      { type: "Delivery Confirmation", dir: "merchant_to_customer", timing: "[4 days after purchase]",
        subject: "Delivered — AudioPeak Order #AP-6674",
        body: "Your ProSilence headphones have been delivered! To get the best experience, please update the firmware via our app. Battery life varies by usage: up to 30hrs (ANC off) or 12-15hrs (ANC on). If you have any issues, contact us within 30 days." },
      { type: "Customer Complaint", dir: "customer_to_merchant", timing: "[18 days after delivery]",
        subject: "Headphones not as advertised",
        body: "The noise cancellation on these headphones is much weaker than I expected and the battery only lasts about 12 hours, not 30 as advertised. This product is not as described. I want a refund." },
      { type: "Merchant Response", dir: "merchant_to_customer", timing: "[19 days after delivery]",
        subject: "RE: Headphones not as advertised",
        body: "Thank you for your feedback. The 30-hour battery life is rated under standard conditions (ANC off, 50% volume) as noted in the detailed product specifications on our website. With ANC enabled, the expected battery life is 12-15 hours, which is consistent with your experience. We'd be happy to process a return within our 30-day window — items must be returned in original packaging. We also recommend updating the firmware via our app, which can improve ANC performance." },
      { type: "Customer Follow-up", dir: "customer_to_merchant", timing: "[20 days after delivery]",
        subject: "RE: RE: Headphones not as advertised",
        body: "I already threw away the packaging because I didn't think I'd need to return them. I shouldn't have to keep packaging for a product that doesn't work as advertised." },
      { type: "Merchant Final Response", dir: "merchant_to_customer", timing: "[21 days after delivery]",
        subject: "RE: RE: RE: Headphones not as advertised",
        body: "We understand your frustration. Unfortunately, our return policy, which was shared in your order confirmation and at checkout, requires items to be returned in original packaging for a full refund. We can offer a 15% discount on a future purchase as a goodwill gesture. The product is performing within its documented specifications." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce",
    threeDS: { enrolled: true, authenticated: true, eci: "05", cavv: "Verified" },
    authNotes: "Authorization obtained with 3D Secure authentication. Standard e-commerce transaction.",
    settlementNotes: "Settled for $199.99. Product delivered and used by the cardholder for 18 days before complaint. Product specifications are accurately disclosed on the product page.",
    riskDevice: { status: "Known device — matches prior purchases", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "IP consistent with cardholder." },
    riskScore: "Low — legitimate purchase, dispute is about product expectations",
    avsCode: "Y", avsDesc: "Full match",
    cvvCode: "M", cvvDesc: "CVV matched",
    refundWindow: "30 days from delivery in original packaging",
    refundPolicy: "Full refund within 30 days of delivery. Items must be returned in original, undamaged packaging with all accessories. Opened items accepted only if packaging is intact.",
    refundDisclosure: "Policy displayed at checkout page, included in order confirmation email, and in delivery confirmation email.",
    refundAck: "Customer agreed to return policy during checkout.",
    fulfillment: { type: "Physical Shipment", status: "Delivered",
      method: "FedEx Ground", timing: "[4 days after purchase]", confirmed: true,
      notes: "Package delivered to cardholder's billing address. Product used for 18 days before complaint. Cardholder discarded original packaging." },
    threeDSRecord: { version: "2.0", enrolled: true, status: "Y — Fully Authenticated", eci: "05",
      cavv: "Verified", challenge: true, liabilityShift: "Yes",
      notes: "3D Secure completed at purchase." }
  }
},

"4854": {
  fav: "acquirer", desc: "Cardholder Dispute — Not Elsewhere Classified",
  product: "an online photography course",
  merchant: "MasterLens Academy",
  issuer: {
    dispute: "The cardholder purchased an online photography course from 'MASTERLENS ACADEMY' for $149.00. The cardholder states the course content was poor quality and not worth the price. The cardholder feels the course was misrepresented and requests a refund. This dispute does not fit standard chargeback categories.",
    contactedMerchant: true,
    merchantResponse: "The cardholder contacted MasterLens [12 days after purchase]. The merchant pointed to the course curriculum which matched the advertised outline, and noted the cardholder had completed 8 of 12 modules.",
    resolution: "Full reversal for unsatisfactory service.",
    docs: [
      { type: "Cardholder Dispute Form", desc: "Statement that the course was poor quality and misrepresented." }
    ],
    commentary: "Filing under 4854. The cardholder is dissatisfied with an online course they purchased. The dispute does not fit other reason code categories.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Full match" },
    cvv: { code: "M", desc: "CVV Match" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "Professional Photography Masterclass — 12-Module Online Course", qty: 1, price: 149.00, sku: "MLA-PPM-12" }],
    orderStatus: "Completed — course access granted",
    shippingAddr: null,
    orderNotes: "Digital product. Cardholder enrolled in the course and completed 8 of 12 modules over a period of [12 days]. Course curriculum matches the advertised outline exactly. All video lessons, assignments, and downloadable resources were accessible.",
    emails: [
      { type: "Enrollment Confirmation", dir: "merchant_to_customer", timing: "Immediately after purchase",
        subject: "Welcome to MasterLens Academy — Your Course Is Ready!",
        body: "You're enrolled in the Professional Photography Masterclass! Total: $149.00. Your 12-module course is ready to start. Log in to access Module 1. Course outline: Composition, Lighting, Exposure, Portraits, Landscapes, Night Photography, Editing, Color Theory, Street Photography, Wildlife, Studio Setup, and Final Project. Satisfaction guarantee: Full refund within 7 days if you've completed fewer than 3 modules." },
      { type: "Progress Update", dir: "merchant_to_customer", timing: "[7 days after purchase]",
        subject: "Great progress! You've completed 5 modules",
        body: "Congratulations! You've completed 5 of 12 modules in your Photography Masterclass. Keep up the great work! Next up: Night Photography." },
      { type: "Customer Complaint", dir: "customer_to_merchant", timing: "[12 days after purchase]",
        subject: "Requesting a refund — course not as expected",
        body: "I've gone through 8 modules and the content quality is not what I expected. The course feels basic and the video quality is poor. I want a full refund." },
      { type: "Merchant Response", dir: "merchant_to_customer", timing: "[13 days after purchase]",
        subject: "RE: Requesting a refund",
        body: "Thank you for your feedback. We're sorry the course didn't meet your expectations. Our records show you've completed 8 of 12 modules over the past 12 days. The course curriculum matches our advertised outline exactly. Our satisfaction guarantee allows a full refund within 7 days if fewer than 3 modules have been completed. Since you've completed 8 modules over 12 days, the refund window has passed. We can offer you access to our Advanced Editing Masterclass at no additional charge as a goodwill gesture." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce",
    threeDS: { enrolled: true, authenticated: true, eci: "05", cavv: "Verified" },
    authNotes: "Standard e-commerce authorization with 3D Secure.",
    settlementNotes: "Settled for $149.00. Digital course fully accessible. Cardholder completed 8 of 12 modules before filing dispute.",
    riskDevice: { status: "Known device — consistent across all login sessions", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "All course access from same IP as purchase." },
    riskScore: "Low — legitimate purchase with extensive usage",
    avsCode: "Y", avsDesc: "Full match",
    cvvCode: "M", cvvDesc: "CVV matched",
    refundWindow: "7 days from purchase (fewer than 3 modules completed)",
    refundPolicy: "Full refund within 7 days if fewer than 3 modules completed. After 3 modules, no refund — course access remains permanent.",
    refundDisclosure: "Clearly stated on enrollment page and in confirmation email.",
    refundAck: "Customer agreed to satisfaction guarantee terms at checkout.",
    fulfillment: { type: "Digital Course", status: "Delivered — 8 of 12 modules completed",
      method: "Online learning platform", timing: "Immediate access after purchase", confirmed: true,
      notes: "Cardholder logged in [14 times over 12 days], completed 8 modules, submitted 4 assignments, and downloaded 6 resource packs. Active usage well beyond the refund window." },
    threeDSRecord: { version: "2.0", enrolled: true, status: "Y — Authenticated", eci: "05",
      cavv: "Verified", challenge: true, liabilityShift: "Yes",
      notes: "3D Secure completed at purchase." }
  }
},

"4855": {
  fav: "issuer", desc: "Goods or Services Not Provided",
  product: "custom-made curtains from an online home décor store",
  merchant: "HomeStitch Custom Décor",
  issuer: {
    dispute: "The cardholder ordered custom-made curtains from 'HOMESTITCH CUSTOM DÉCOR' for $275.00. The estimated delivery was [10-14 business days after purchase]. As of [45 days after purchase], the curtains have not been delivered. The cardholder attempted to contact the merchant multiple times with no response. The merchant's website now shows 'temporarily closed'.",
    contactedMerchant: true,
    merchantResponse: "The cardholder emailed [14 days after purchase] and again [21 days after purchase] asking about the order status. The first email received an auto-reply saying 'We are experiencing delays.' The second email received no response. The cardholder also tried calling [30 days after purchase] and the phone number was disconnected.",
    resolution: "Full reversal for non-delivery of goods.",
    docs: [
      { type: "Order Confirmation", desc: "Confirmation email showing order placed and payment collected." },
      { type: "Communication Attempts", desc: "Emails and call logs showing multiple failed attempts to reach the merchant." }
    ],
    commentary: "Filing under 4855. The cardholder paid for custom curtains that were never delivered. The merchant has become unresponsive and their website is now closed. The delivery deadline has long passed.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Match" },
    cvv: { code: "M", desc: "CVV Match" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "Custom Blackout Curtains — 84x52 inches, Navy Blue, Pair", qty: 1, price: 275.00, sku: "HS-CBC-84NB" }],
    orderStatus: "Processing — never shipped",
    shippingAddr: "Cardholder's billing address",
    orderNotes: "Custom order placed. Production was started but the merchant experienced severe supply chain disruptions. The order was never completed or shipped. The merchant's operations have been suspended.",
    emails: [
      { type: "Order Confirmation", dir: "merchant_to_customer", timing: "Immediately after purchase",
        subject: "HomeStitch Custom Décor — Order #HS-4520",
        body: "Thank you for your custom curtain order! Custom Blackout Curtains (84x52, Navy Blue, Pair). Total: $275.00. Estimated delivery: [10-14 business days]. We'll send a shipping notification when your curtains are ready." },
      { type: "Customer Follow-up #1", dir: "customer_to_merchant", timing: "[14 days after purchase]",
        subject: "Order status — #HS-4520",
        body: "My curtains were supposed to arrive by now. Can you please provide an update on my order?" },
      { type: "Auto-Reply", dir: "merchant_to_customer", timing: "[14 days after purchase]",
        subject: "RE: Order status — #HS-4520",
        body: "Thank you for contacting HomeStitch Custom Décor. We are currently experiencing production delays due to supply chain issues. We will update you as soon as possible. We apologize for the inconvenience." },
      { type: "Customer Follow-up #2", dir: "customer_to_merchant", timing: "[21 days after purchase]",
        subject: "URGENT: Where are my curtains? Order #HS-4520",
        body: "It has been 3 weeks and I have not received my curtains or any update. Please ship my order or issue a full refund immediately." },
      { type: "No Response", dir: "system_note", timing: "[21+ days after purchase]",
        subject: "No merchant response",
        body: "No response was sent to the customer's second inquiry. The cardholder also attempted to call [30 days after purchase] and the phone number was disconnected. The merchant's website now displays 'temporarily closed for maintenance'." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce", threeDS: null,
    authNotes: "Authorization obtained at time of order. The custom product was never manufactured or shipped. No refund was issued.",
    settlementNotes: "Settled for $275.00. No product was delivered. No refund was processed. The merchant's operations have been suspended.",
    riskDevice: { status: "Customer's device is known and legitimate", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "IP consistent with cardholder." },
    riskScore: "N/A — risk is on the merchant side (non-delivery)",
    avsCode: "Y", avsDesc: "Full match",
    cvvCode: "M", cvvDesc: "CVV matched",
    refundWindow: "30 days from expected delivery date",
    refundPolicy: "Custom orders: full refund if not delivered within 30 days of estimated delivery date.",
    refundDisclosure: "Policy on website.",
    refundAck: "Customer agreed to terms at checkout.",
    fulfillment: { type: "Custom Manufacturing + Shipment", status: "Never shipped — production halted",
      method: "N/A — order never fulfilled", timing: "Expected [10-14 business days after purchase], never delivered", confirmed: false,
      notes: "The custom curtains were never manufactured to completion or shipped. The merchant has ceased operations. No tracking number was ever generated." },
    threeDSRecord: null
  }
},

"4856": {
  fav: "acquirer", desc: "Defective or Not as Described (Chip Liability Shift)",
  product: "a smart fitness watch from an electronics store",
  merchant: "TechFit Store",
  issuer: {
    dispute: "The cardholder purchased a smart fitness watch from 'TECHFIT STORE' for $249.99 at a chip-enabled terminal. The cardholder states the watch's heart rate sensor stopped working [3 days after purchase] and the screen developed dead pixels [5 days after purchase]. The cardholder considers the product defective.",
    contactedMerchant: true,
    merchantResponse: "The cardholder returned to the store [5 days after purchase]. The store staff tested the watch and confirmed the heart rate sensor was responsive during their test. They suggested a firmware update. The cardholder left with the watch.",
    resolution: "Full reversal for defective product purchased at chip-enabled terminal.",
    docs: [
      { type: "Cardholder Dispute Form", desc: "Description of defects: heart rate sensor failure and dead pixels." },
      { type: "Product Photos", desc: "Cardholder's photos showing dead pixels on the watch screen." }
    ],
    commentary: "Filing under 4856. Product purchased at a chip-enabled terminal is defective. Heart rate sensor intermittently fails and screen has dead pixels.",
    cardPresent: true, posEntry: "05",
    avs: { code: "N/A", desc: "Card-present EMV" },
    cvv: { code: "N/A", desc: "Chip cryptogram" },
    riskFlags: { geoMismatch: "None", deviceTrust: "N/A" }
  },
  acquirer: {
    items: [{ name: "FitTrack Pro Smart Watch — GPS, Heart Rate, 5ATM Water Resistant", qty: 1, price: 249.99, sku: "TF-FTP-BLK" }],
    orderStatus: "Completed — sold in store",
    shippingAddr: null,
    orderNotes: "In-store purchase. Chip transaction processed correctly with full EMV data. Cardholder returned [5 days later] claiming defects. Store staff tested the watch — heart rate sensor worked during the test. Staff recommended a firmware update. Cardholder left with the watch and did not initiate a return.",
    emails: [
      { type: "In-Store Visit Record", dir: "system_note", timing: "[5 days after purchase]",
        subject: "Customer Visit — Defect Complaint",
        body: "Customer returned to store complaining of heart rate sensor issues and screen dead pixels. Staff testing: heart rate sensor functioned normally during a 5-minute test on the customer's wrist. Screen was examined — one very small light spot noted at the edge, within manufacturer's acceptable tolerance (fewer than 3 dead pixels). Staff performed a firmware update on the watch and demonstrated that the heart rate sensor was reading accurately. Customer was informed they could return the watch within 30 days for a full refund or exchange. Customer chose to keep the watch and left the store." },
      { type: "Post-Visit Follow-up", dir: "merchant_to_customer", timing: "[6 days after purchase]",
        subject: "TechFit Store — Following Up on Your Visit",
        body: "Thank you for visiting us regarding your FitTrack Pro Smart Watch. We're glad we could update the firmware. If you continue to experience issues, please bring the watch back within your 30-day return window for a full refund or exchange. We want you to be satisfied with your purchase." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "EMV Chip — Card Present", threeDS: null,
    authNotes: "EMV chip transaction processed correctly. Full chip data captured. Card authenticated via chip cryptogram.",
    settlementNotes: "Settled for $249.99. Product tested in-store and found to be within specifications. Customer chose not to return the watch during the 30-day window.",
    riskDevice: { status: "N/A — card-present POS", trust: "N/A", match: true },
    riskIP: { level: "N/A", proxy: false, geoMatch: true, notes: "Card-present at store." },
    riskScore: "Low — legitimate card-present purchase",
    avsCode: "N/A", avsDesc: "Card-present EMV",
    cvvCode: "N/A", cvvDesc: "Chip cryptogram verified",
    refundWindow: "30 days from purchase",
    refundPolicy: "Full refund or exchange within 30 days. Item must be returned with original packaging and all accessories. Defective items may be exchanged at any time under manufacturer warranty.",
    refundDisclosure: "Printed on receipt and posted in store.",
    refundAck: "Receipt provided at POS. Customer informed of return policy during in-store visit.",
    fulfillment: null,
    threeDSRecord: null
  }
},

"4857": {
  fav: "issuer", desc: "Card-Activated Telephone Transaction",
  product: "a vitamin supplement from a telephone order",
  merchant: "NutriDirect Phone Sales",
  issuer: {
    dispute: "The cardholder found a $74.95 charge from 'NUTRIDIRECT PHONE SALES' on their statement. The cardholder states they never called this company or ordered anything by phone. The cardholder does not recognize the merchant name and did not provide their card details over the phone to anyone.",
    contactedMerchant: false,
    merchantResponse: "N/A — the cardholder denies any telephone interaction with this merchant.",
    resolution: "Full reversal — unauthorized telephone transaction without proper cardholder verification.",
    docs: [
      { type: "Cardholder Affidavit", desc: "Sworn statement that cardholder did not make a telephone order with this merchant." },
      { type: "Phone Records", desc: "Cardholder's phone records showing no calls to/from the merchant's number." }
    ],
    commentary: "Filing under 4857. The cardholder denies placing a telephone order. No call recording or written confirmation exists. The merchant did not follow required verification protocols for telephone orders.",
    cardPresent: false, posEntry: "01",
    avs: { code: "N", desc: "No match" },
    cvv: { code: "N", desc: "CVV not verified" },
    riskFlags: { geoMismatch: "Unknown", deviceTrust: "N/A — telephone order" }
  },
  acquirer: {
    items: [{ name: "Premium Multi-Vitamin Complex — 90-day supply", qty: 1, price: 74.95, sku: "ND-MVC-90" }],
    orderStatus: "Completed — shipped",
    shippingAddr: "Address provided during phone call — does not match cardholder's billing address",
    orderNotes: "Telephone order taken by sales agent. Caller provided card number, expiration date, and billing address. Call was not recorded. No written confirmation was sent before shipment.",
    emails: [
      { type: "No Pre-Order Communication", dir: "system_note", timing: "N/A",
        subject: "N/A", body: "Telephone order. The caller provided card details over the phone to a sales agent. The call was not recorded per company policy at the time. No written or emailed order confirmation was sent before shipment. A shipping confirmation was sent to the email address provided by the caller." },
      { type: "Shipping Confirmation", dir: "merchant_to_customer", timing: "[2 days after order]",
        subject: "NutriDirect — Your Order Has Shipped",
        body: "Your Premium Multi-Vitamin Complex (90-day supply) has shipped. Total: $74.95. Tracking included." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Manual Key Entry — Telephone/Mail Order (MOTO)", threeDS: null,
    authNotes: "Card number manually keyed by telephone sales agent. Authorization obtained. AVS returned no match. CVV was not collected during the phone call. No 3D Secure possible for MOTO transactions. The call was not recorded.",
    settlementNotes: "Settled for $74.95. Telephone order with no call recording and no pre-shipment confirmation sent to cardholder. AVS did not match.",
    riskDevice: { status: "N/A — telephone order", trust: "N/A", match: false },
    riskIP: { level: "N/A", proxy: false, geoMatch: false, notes: "Telephone order — no IP data." },
    riskScore: "Medium-High — MOTO transaction, no recording, AVS mismatch",
    avsCode: "N", avsDesc: "No match — address provided by caller does not match",
    cvvCode: "N", cvvDesc: "CVV not collected during phone call",
    refundWindow: "30 days from shipment",
    refundPolicy: "Full refund within 30 days if product returned unopened.",
    refundDisclosure: "Stated verbally during phone call (not recorded).",
    refundAck: "Verbal acknowledgment only — no written record.",
    fulfillment: { type: "Physical Shipment", status: "Shipped to address provided by caller",
      method: "Standard USPS", timing: "[2 days after order]", confirmed: false,
      notes: "Shipped to address given by the caller. This address does not match the cardholder's billing address on file with the issuing bank. No delivery signature was required." },
    threeDSRecord: null
  }
},

"4859": {
  fav: "acquirer", desc: "Addendum, No-show, or ATM Dispute",
  product: "a hotel reservation with no-show charge",
  merchant: "Seaside Grand Hotel",
  issuer: {
    dispute: "The cardholder was charged a $189.00 no-show fee by 'SEASIDE GRAND HOTEL' for a reservation they claim to have cancelled. The cardholder states they called the hotel [2 days before the reservation] and spoke with a representative who confirmed the cancellation verbally.",
    contactedMerchant: true,
    merchantResponse: "The cardholder called the hotel [3 days after the no-show charge] to dispute it. The hotel said their system shows no cancellation on file and the room was held as reserved. The hotel refused to reverse the charge.",
    resolution: "Reversal of the $189.00 no-show charge.",
    docs: [
      { type: "Cardholder Dispute Form", desc: "Statement that reservation was cancelled by phone [2 days before check-in]." },
      { type: "Cardholder's Phone Record", desc: "Call log showing a call to the hotel's number [2 days before check-in]." }
    ],
    commentary: "Filing under 4859. The cardholder claims they cancelled the reservation by phone before the cancellation deadline. The hotel has no cancellation record.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Full match" },
    cvv: { code: "M", desc: "CVV on file from booking" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "Deluxe Ocean View Room — 1 night", qty: 1, price: 189.00, sku: "SGH-DOV-1N" }],
    orderStatus: "No-show — room held but guest did not arrive",
    shippingAddr: null,
    orderNotes: "Reservation made online [14 days before check-in]. Cancellation policy requires cancellation at least 24 hours before check-in via the booking portal or by calling with the confirmation number. The hotel's reservation system shows no cancellation was recorded. The room was held and could not be sold to other guests.",
    emails: [
      { type: "Booking Confirmation", dir: "merchant_to_customer", timing: "[14 days before check-in]",
        subject: "Seaside Grand Hotel — Reservation Confirmation #SGH-8821",
        body: "Your reservation is confirmed! Deluxe Ocean View Room for 1 night. Total: $189.00. Check-in: 3:00 PM. CANCELLATION POLICY: Free cancellation up to 24 hours before check-in. Cancellations within 24 hours or no-shows will be charged the full room rate. To cancel, use the link below or call us with your confirmation number #SGH-8821." },
      { type: "Check-in Reminder", dir: "merchant_to_customer", timing: "[1 day before check-in]",
        subject: "Seaside Grand Hotel — Tomorrow's Check-in Reminder",
        body: "We're looking forward to welcoming you tomorrow! Check-in begins at 3:00 PM. Your Deluxe Ocean View Room is reserved. If you need to cancel, please do so before 3:00 PM today to avoid charges. Confirmation #SGH-8821." },
      { type: "No-Show Notification", dir: "merchant_to_customer", timing: "[1 day after scheduled check-in]",
        subject: "Seaside Grand Hotel — No-Show Charge for Reservation #SGH-8821",
        body: "You did not check in for your reservation yesterday and no cancellation was recorded in our system. Per our cancellation policy (shared in your booking confirmation), your card has been charged $189.00 for the no-show. If you believe this is an error, please contact us at (555) 123-4567 with your confirmation number." },
      { type: "Customer Dispute Call", dir: "customer_to_merchant", timing: "[3 days after the no-show charge]",
        subject: "Phone Call — Disputing No-Show Charge",
        body: "The cardholder called the hotel to dispute the no-show charge. The cardholder stated they called to cancel [2 days before check-in]. The hotel front desk checked the reservation system and confirmed no cancellation was recorded. The hotel's reservation system has an audit log showing the reservation status remained 'Confirmed' with no modification or cancellation events. The cardholder's phone records show a 2-minute call to the hotel's main number, but the hotel cannot confirm whether the call was a cancellation request, a general inquiry, or was routed to voicemail." },
      { type: "Merchant Written Response", dir: "merchant_to_customer", timing: "[4 days after the no-show charge]",
        subject: "RE: Your No-Show Charge — Reservation #SGH-8821",
        body: "We have thoroughly reviewed your reservation #SGH-8821. Our system shows no cancellation was recorded. The reservation audit log shows it remained in 'Confirmed' status from booking through the no-show date. Your room was held and could not be sold to other guests. Our cancellation policy, which was clearly stated in your booking confirmation and reminder emails, requires cancellation before the deadline. We are unable to reverse the charge." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce — booking guarantee",
    threeDS: { enrolled: true, authenticated: true, eci: "05", cavv: "Verified at booking" },
    authNotes: "Authorization obtained at time of booking as a guarantee. No-show charge processed after the guest failed to appear and no cancellation was recorded.",
    settlementNotes: "Settled for $189.00 as a no-show charge. Reservation system audit log confirms no cancellation was recorded. Cancellation policy was disclosed at booking and in the reminder email.",
    riskDevice: { status: "Known device from booking", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "Booking made from cardholder's usual location." },
    riskScore: "Low — legitimate hotel no-show charge",
    avsCode: "Y", avsDesc: "Full match at booking",
    cvvCode: "M", cvvDesc: "CVV verified at booking",
    refundWindow: "N/A — no-show policy applies",
    refundPolicy: "Free cancellation up to 24 hours before check-in. No-shows charged full room rate.",
    refundDisclosure: "CANCELLATION POLICY clearly stated in booking confirmation and check-in reminder email. Also displayed during online booking before payment.",
    refundAck: "Customer acknowledged cancellation policy during online booking.",
    fulfillment: null,
    threeDSRecord: { version: "2.0", enrolled: true, status: "Y — Authenticated at booking", eci: "05",
      cavv: "Verified", challenge: true, liabilityShift: "Yes",
      notes: "3D Secure completed at time of booking." }
  }
},

"4860": {
  fav: "issuer", desc: "Credit Not Processed",
  product: "a pair of running shoes returned to an online shoe retailer",
  merchant: "StrideFit Running",
  issuer: {
    dispute: "The cardholder purchased running shoes from 'STRIDEFIT RUNNING' for $134.95. The cardholder returned the shoes [10 days after purchase] due to sizing issues. The merchant confirmed receipt of the return and promised a full refund in writing [15 days after purchase]. As of [45 days after purchase], the refund has not appeared on the cardholder's statement.",
    contactedMerchant: true,
    merchantResponse: "The cardholder contacted StrideFit [30 days after purchase] asking about the refund. The merchant said they were processing it and it would appear within 5-7 business days. [45 days after purchase], the credit still has not appeared. The cardholder contacted them again and received no response.",
    resolution: "Full reversal of $134.95. The merchant promised a refund that was never processed.",
    docs: [
      { type: "Return Confirmation Email", desc: "Merchant's email confirming receipt of the returned shoes and promising a full refund." },
      { type: "Account Statements", desc: "Statements from [15 days after purchase] through [45 days after purchase] showing no credit was posted." }
    ],
    commentary: "Filing under 4860. The merchant acknowledged the return and promised a full refund [15 days after purchase] but has not processed the credit. Sufficient time has passed for the credit to appear.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Full match" },
    cvv: { code: "M", desc: "CVV Match" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "CloudRunner Pro Running Shoes — Size 10, Black/Red", qty: 1, price: 134.95, sku: "SF-CRP-10BR" }],
    orderStatus: "Returned — refund pending",
    shippingAddr: "Cardholder's billing address",
    orderNotes: "Customer returned the shoes due to sizing issues. Return was received [14 days after purchase]. Return was approved and a refund was initiated in the merchant's system but was not successfully transmitted to the payment processor.",
    emails: [
      { type: "Order Confirmation", dir: "merchant_to_customer", timing: "Immediately after purchase",
        subject: "StrideFit Running — Order #SF-7723",
        body: "Your CloudRunner Pro Running Shoes (Size 10, Black/Red) are confirmed! Total: $134.95. Free returns within 30 days — just use the return label included in your package." },
      { type: "Delivery Confirmation", dir: "merchant_to_customer", timing: "[4 days after purchase]",
        subject: "Your StrideFit Order Has Been Delivered — #SF-7723",
        body: "Your shoes have been delivered! Try them on and if they don't fit, use the included prepaid return label for a free return within 30 days." },
      { type: "Return Request", dir: "customer_to_merchant", timing: "[10 days after purchase]",
        subject: "Return request — Order #SF-7723 — wrong size",
        body: "The shoes are a bit too narrow. I'd like to return them for a full refund. I'll use the included return label." },
      { type: "Return Acknowledged", dir: "merchant_to_customer", timing: "[10 days after purchase]",
        subject: "RE: Return request — #SF-7723",
        body: "No problem! Please ship the shoes back using the prepaid label. Once we receive them, we'll process your full refund of $134.95 within 5-7 business days." },
      { type: "Return Received & Refund Promise", dir: "merchant_to_customer", timing: "[15 days after purchase]",
        subject: "StrideFit — Return Received, Refund Processing — #SF-7723",
        body: "We've received your returned CloudRunner Pro shoes in good condition. Your full refund of $134.95 will be credited to your original payment method within 5-7 business days. Thank you!" },
      { type: "Customer Follow-up", dir: "customer_to_merchant", timing: "[30 days after purchase]",
        subject: "Where is my refund? Order #SF-7723",
        body: "It's been 2 weeks since you said you received my return and would refund me. I still don't see the $134.95 credit on my statement. When will this be processed?" },
      { type: "Merchant Response", dir: "merchant_to_customer", timing: "[31 days after purchase]",
        subject: "RE: Where is my refund?",
        body: "We apologize for the delay. We're experiencing a technical issue with our refund processing system. Your refund is in the queue and should appear within 5-7 additional business days." },
      { type: "Customer Final Follow-up", dir: "customer_to_merchant", timing: "[45 days after purchase]",
        subject: "STILL no refund — Order #SF-7723",
        body: "It has now been 30 days since you said my refund was processing. I have not received the $134.95 credit. Please process this immediately." },
      { type: "No Response", dir: "system_note", timing: "[45+ days after purchase]",
        subject: "No merchant response",
        body: "The merchant did not respond to the cardholder's final follow-up." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce", threeDS: null,
    authNotes: "Original purchase authorized normally. The return was received and a refund was initiated in the merchant's system, but a processing error prevented the credit from being transmitted to the payment processor.",
    settlementNotes: "Original transaction settled for $134.95. Refund was initiated in merchant's internal system but never reached the payment processor. The credit has not been posted to the cardholder's account.",
    riskDevice: { status: "Known device", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "Consistent with cardholder." },
    riskScore: "N/A — issue is about missing refund, not fraud",
    avsCode: "Y", avsDesc: "Full match",
    cvvCode: "M", cvvDesc: "CVV matched",
    refundWindow: "30 days from purchase — free returns",
    refundPolicy: "Full refund within 30 days. Free return shipping with prepaid label. Refund processed within 5-7 business days of receiving the return.",
    refundDisclosure: "In order confirmation email, on website, and included with the shipment.",
    refundAck: "Customer initiated return within policy window. Merchant confirmed refund in writing.",
    fulfillment: { type: "Physical Shipment", status: "Delivered, then returned to merchant",
      method: "USPS — prepaid return label", timing: "Delivered [4 days after purchase], returned [14 days after purchase]", confirmed: true,
      notes: "Shoes delivered, then returned by cardholder using prepaid label. Merchant confirmed receipt of return [15 days after purchase]. Refund promised but never posted." },
    threeDSRecord: null
  }
},

"4862": {
  fav: "issuer", desc: "Counterfeit Transaction (Magnetic Stripe)",
  product: "a luxury handbag from a boutique store",
  merchant: "Prestige Leather Boutique",
  issuer: {
    dispute: "The cardholder reports a $650.00 charge from 'PRESTIGE LEATHER BOUTIQUE' that they did not make. Forensic analysis of the transaction data indicates the magnetic stripe data was cloned — the Track 2 data contains anomalies consistent with a counterfeit card. The genuine cardholder's card was in their possession at their home city, while this transaction occurred [300 miles away] at a physical store.",
    contactedMerchant: false,
    merchantResponse: "N/A — the cardholder was not present and did not make this transaction.",
    resolution: "Full reversal. Transaction was conducted with a counterfeit card using cloned magnetic stripe data.",
    docs: [
      { type: "Fraud Investigation Report", desc: "Forensic analysis showing Track 2 data anomalies consistent with magnetic stripe cloning." },
      { type: "Cardholder Location Verification", desc: "Evidence that the cardholder was in their home city at the time of the transaction (mobile banking login from home IP)." }
    ],
    commentary: "Filing under 4862. Transaction processed via magnetic stripe at a terminal that does not support EMV chip. The magnetic stripe data shows cloning indicators. The genuine cardholder was in a different city at the time.",
    cardPresent: true, posEntry: "90",
    avs: { code: "N/A", desc: "Card-present" },
    cvv: { code: "N/A", desc: "CVV1 on magnetic stripe — from cloned data" },
    riskFlags: { geoMismatch: "Yes — transaction 300 miles from cardholder's location", deviceTrust: "N/A" }
  },
  acquirer: {
    items: [{ name: "Italian Leather Tote Bag — Limited Edition", qty: 1, price: 650.00, sku: "PLB-ILTB-LE" }],
    orderStatus: "Completed — customer left with merchandise",
    shippingAddr: null,
    orderNotes: "In-store purchase. Card swiped via magnetic stripe. The merchant's POS terminal does not support EMV chip reading — it is an older magnetic stripe-only terminal. The transaction was approved.",
    emails: [
      { type: "No Communications", dir: "system_note", timing: "N/A",
        subject: "N/A", body: "In-store card-present transaction. Customer presented a card and it was swiped. Authorization was approved. The POS terminal is a magnetic stripe-only terminal that does not support EMV chip. The merchant does not have chip-capable terminals installed." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Magnetic Stripe Swipe — Non-EMV Terminal", threeDS: null,
    authNotes: "Authorization obtained via magnetic stripe swipe. The terminal does not support EMV chip reading. If a chip-capable terminal had been used, the counterfeit card's chip would likely have failed authentication.",
    settlementNotes: "Settled for $650.00. The merchant's terminal does not support EMV chip. The card's magnetic stripe data was processed successfully, but forensic analysis by the issuer indicates the stripe data was cloned.",
    riskDevice: { status: "N/A — card-present, non-EMV terminal", trust: "N/A", match: false },
    riskIP: { level: "N/A", proxy: false, geoMatch: false, notes: "Card-present at merchant's store location." },
    riskScore: "High — counterfeit card used at non-chip terminal",
    avsCode: "N/A", avsDesc: "Card-present",
    cvvCode: "N/A", cvvDesc: "CVV1 on cloned magnetic stripe",
    refundWindow: "14 days from purchase",
    refundPolicy: "Exchange or store credit within 14 days with receipt. No cash/card refunds on luxury items.",
    refundDisclosure: "Printed on receipt.",
    refundAck: "Receipt provided at POS.",
    fulfillment: null,
    threeDSRecord: null
  }
},

"4863": {
  fav: "acquirer", desc: "Cardholder Does Not Recognize Transaction",
  product: "a personalized phone case from an online custom accessories store",
  merchant: "CaseArtisan",
  issuer: {
    dispute: "The cardholder does not recognize a $39.99 charge from 'CASEARTISAN' on their statement. The merchant name does not correspond to any purchase the cardholder recalls. The cardholder states they never ordered a phone case or visited this website.",
    contactedMerchant: true,
    merchantResponse: "The cardholder tried to look up 'CASEARTISAN' online [5 days after noticing the charge] but wasn't sure it was the same store. They did not contact the merchant directly.",
    resolution: "Full reversal for unrecognized transaction.",
    docs: [
      { type: "Cardholder Dispute Form", desc: "Statement that the cardholder does not recognize the merchant or the charge." }
    ],
    commentary: "Filing under 4863. The cardholder does not recognize this transaction or the merchant name on their statement.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Full match" },
    cvv: { code: "M", desc: "CVV Match" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "Custom Photo Phone Case — iPhone 15, Matte Finish, personalized with customer's uploaded photo", qty: 1, price: 39.99, sku: "CA-CPC-I15M" }],
    orderStatus: "Completed — delivered",
    shippingAddr: "Cardholder's billing address",
    orderNotes: "Customer created an account using their email address, uploaded a personal photo for the custom phone case design, and completed checkout. The billing descriptor 'CASEARTISAN' matches the business name displayed on the website and in the order confirmation.",
    emails: [
      { type: "Account Creation", dir: "merchant_to_customer", timing: "[2 minutes before purchase]",
        subject: "Welcome to CaseArtisan!",
        body: "Your CaseArtisan account has been created! You can now design and order custom phone cases. Your account email: [cardholder's email address on file with the bank]." },
      { type: "Order Confirmation", dir: "merchant_to_customer", timing: "Immediately after purchase",
        subject: "CaseArtisan — Your Custom Case Order #CA-1147",
        body: "Your custom phone case is confirmed! Design: Your uploaded photo (a family photo with 3 people) on a Matte Finish iPhone 15 case. Total: $39.99. The billing descriptor on your statement will appear as 'CASEARTISAN'. Delivery: [5-7 business days]." },
      { type: "Design Ready Notification", dir: "merchant_to_customer", timing: "[1 day after purchase]",
        subject: "Your Custom Case Design Is Ready for Production — #CA-1147",
        body: "Your custom phone case design has been finalized and is going into production! Preview attached. If this doesn't look right, contact us within 24 hours." },
      { type: "Shipping Notification", dir: "merchant_to_customer", timing: "[3 days after purchase]",
        subject: "Your CaseArtisan Order Has Shipped — #CA-1147",
        body: "Your custom phone case has shipped! Tracking number included. Expected delivery: [5 days after purchase]." },
      { type: "Delivery Confirmation", dir: "merchant_to_customer", timing: "[6 days after purchase]",
        subject: "Delivered — CaseArtisan Order #CA-1147",
        body: "Your custom phone case has been delivered to your address! We hope you love it." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce",
    threeDS: { enrolled: true, authenticated: true, eci: "05", cavv: "Verified" },
    authNotes: "3D Secure authentication completed. AVS and CVV matched. The order included a personalized photo uploaded by the customer.",
    settlementNotes: "Settled for $39.99. Product was a custom phone case with the cardholder's personal photo. Delivered to billing address.",
    riskDevice: { status: "Known device — browser fingerprint matches the cardholder's typical device profile", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "IP address consistent with cardholder's billing address location." },
    riskScore: "Very Low — personalized product with cardholder's own photo, 3DS, AVS match, delivery to billing address",
    avsCode: "Y", avsDesc: "Full match — street and ZIP",
    cvvCode: "M", cvvDesc: "CVV matches issuer records",
    refundWindow: "N/A — custom/personalized products",
    refundPolicy: "Custom personalized products are non-refundable unless there is a manufacturing defect. This was stated at checkout before payment.",
    refundDisclosure: "Non-refundable notice displayed next to the 'Place Order' button and in confirmation email.",
    refundAck: "Customer acknowledged 'Custom items are non-refundable' checkbox at checkout.",
    fulfillment: { type: "Custom Manufacturing + Shipment", status: "Delivered to billing address",
      method: "USPS First Class", timing: "[6 days after purchase]", confirmed: true,
      notes: "Custom phone case featuring the cardholder's uploaded personal family photo. Delivered to billing address." },
    threeDSRecord: { version: "2.0", enrolled: true, status: "Y — Fully Authenticated", eci: "05",
      cavv: "Verified", challenge: true, liabilityShift: "Yes — liability shifted to issuer",
      notes: "3D Secure completed. One-time passcode verified." }
  }
},

"4870": {
  fav: "issuer", desc: "Chip Liability Shift — Counterfeit",
  product: "designer clothing from a fashion retail store",
  merchant: "Vogue Street Fashion",
  issuer: {
    dispute: "A counterfeit card was used at 'VOGUE STREET FASHION' for a $412.00 purchase. The card is chip-enabled, but the merchant's terminal processed it via magnetic stripe fallback instead of reading the chip. Per Mastercard's EMV liability shift rules, when a chip card is processed via magnetic stripe at a non-chip terminal, liability for counterfeit fraud shifts to the acquirer. The genuine cardholder was not present.",
    contactedMerchant: false,
    merchantResponse: "N/A — counterfeit card used; genuine cardholder not involved.",
    resolution: "Full reversal under EMV chip liability shift for counterfeit transactions.",
    docs: [
      { type: "EMV Liability Shift Analysis", desc: "Confirmation that the card is chip-enabled but the transaction was processed via magnetic stripe at a non-chip terminal." },
      { type: "Counterfeit Analysis", desc: "Forensic indicators showing the magnetic stripe data was cloned." }
    ],
    commentary: "Filing under 4870. Chip-enabled card processed via magnetic stripe at a non-EMV terminal. Per the EMV liability shift framework, liability transfers to the acquirer when a chip card is not processed via chip due to merchant terminal limitations.",
    cardPresent: true, posEntry: "90",
    avs: { code: "N/A", desc: "Card-present" },
    cvv: { code: "N/A", desc: "Magnetic stripe CVV1" },
    riskFlags: { geoMismatch: "Yes — different city from cardholder", deviceTrust: "N/A" }
  },
  acquirer: {
    items: [{ name: "Designer Denim Jacket", qty: 1, price: 245.00, sku: "VSF-DDJ-L" }, { name: "Premium Cotton T-Shirt (2-pack)", qty: 1, price: 167.00, sku: "VSF-PCT-2" }],
    orderStatus: "Completed — customer left with merchandise",
    shippingAddr: null,
    orderNotes: "In-store purchase. The merchant's POS terminal does not support EMV chip reading. The card was swiped via magnetic stripe. The merchant is in the process of upgrading terminals but chip capability was not available at the time of this transaction.",
    emails: [
      { type: "No Communications", dir: "system_note", timing: "N/A",
        subject: "N/A", body: "In-store card-present transaction. Card swiped via magnetic stripe at a non-EMV terminal. Authorization approved. Customer left with merchandise. The merchant's terminal does not have chip capability." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Magnetic Stripe Swipe — Non-EMV Terminal", threeDS: null,
    authNotes: "Authorization obtained via magnetic stripe. The terminal does not support EMV chip reading. Under Mastercard's liability shift rules, the acquirer bears liability for counterfeit fraud when a chip card is processed via fallback to magnetic stripe at a non-chip terminal.",
    settlementNotes: "Settled for $412.00. Terminal does not support EMV chip. Liability shifts to acquirer under EMV counterfeit liability shift.",
    riskDevice: { status: "N/A — non-EMV card-present", trust: "N/A", match: false },
    riskIP: { level: "N/A", proxy: false, geoMatch: false, notes: "Card-present at store." },
    riskScore: "High — counterfeit card, non-chip terminal",
    avsCode: "N/A", avsDesc: "Card-present",
    cvvCode: "N/A", cvvDesc: "CVV1 on magnetic stripe",
    refundWindow: "14 days from purchase",
    refundPolicy: "Exchange or refund within 14 days with receipt.",
    refundDisclosure: "Printed on receipt.",
    refundAck: "Receipt provided.",
    fulfillment: null,
    threeDSRecord: null
  }
},

"4871": {
  fav: "acquirer", desc: "Chip/PIN Liability Shift — Lost/Stolen/NRI",
  product: "groceries from a supermarket",
  merchant: "FreshMart Supermarket",
  issuer: {
    dispute: "The cardholder reports their card was stolen [1 day before this transaction]. A $156.78 grocery purchase at 'FRESHMART SUPERMARKET' was made the next day. The cardholder did not make this purchase and reported the card as stolen to the bank.",
    contactedMerchant: false,
    merchantResponse: "N/A — card was stolen; cardholder did not visit the store.",
    resolution: "Reversal of the $156.78 charge made on the stolen card.",
    docs: [
      { type: "Stolen Card Report", desc: "Cardholder's report of the stolen card filed with the bank [1 day before this transaction]." },
      { type: "Transaction Record", desc: "Record showing the transaction at FreshMart." }
    ],
    commentary: "Filing under 4871. The card was reported stolen before this transaction. However, the transaction was processed at a chip-enabled terminal with correct PIN entry.",
    cardPresent: true, posEntry: "05",
    avs: { code: "N/A", desc: "Card-present EMV" },
    cvv: { code: "N/A", desc: "Chip cryptogram" },
    riskFlags: { geoMismatch: "Possible — transaction at a store 5 miles from cardholder's home", deviceTrust: "N/A" }
  },
  acquirer: {
    items: [
      { name: "Assorted groceries — produce, dairy, household items", qty: 1, price: 156.78, sku: "FM-GROC-MIX" }
    ],
    orderStatus: "Completed — customer left with groceries",
    shippingAddr: null,
    orderNotes: "In-store grocery purchase. EMV chip was read successfully. PIN was entered correctly on the first attempt. The terminal is fully chip-and-PIN capable. Per Mastercard's liability shift rules, when a chip card is used with correct PIN verification at a chip-enabled terminal, liability for lost/stolen card fraud shifts to the issuer.",
    emails: [
      { type: "No Communications", dir: "system_note", timing: "N/A",
        subject: "N/A", body: "In-store grocery purchase. EMV chip card inserted into chip-enabled terminal. PIN entered correctly on first attempt. Transaction completed. Customer left with groceries. No email exchange for in-store grocery purchases." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "EMV Chip + PIN — Card Present", threeDS: null,
    authNotes: "EMV chip was read and the chip cryptogram was validated. PIN was entered correctly on the first attempt. The terminal is fully chip-and-PIN capable. Under Mastercard's liability shift rules for lost/stolen cards (4871), liability shifts to the issuer when the chip was read and the correct PIN was entered.",
    settlementNotes: "Settled for $156.78. Chip+PIN transaction at a chip-capable terminal. Liability shifts to issuer per EMV liability shift framework.",
    riskDevice: { status: "N/A — chip-enabled POS terminal", trust: "N/A", match: true },
    riskIP: { level: "N/A", proxy: false, geoMatch: true, notes: "Card-present at store near cardholder's home." },
    riskScore: "Low from acquirer perspective — chip+PIN verified",
    avsCode: "N/A", avsDesc: "Card-present EMV+PIN",
    cvvCode: "N/A", cvvDesc: "Chip cryptogram validated",
    refundWindow: "N/A — grocery purchase",
    refundPolicy: "Grocery items: exchanges or store credit for defective products within 7 days with receipt.",
    refundDisclosure: "Printed on receipt.",
    refundAck: "Receipt provided.",
    fulfillment: null,
    threeDSRecord: null
  }
},

"4899": {
  fav: "issuer", desc: "Mastercard Dispute Resolution — Violation of Rules",
  product: "an annual membership at an online service platform",
  merchant: "ProAccess Business Tools",
  issuer: {
    dispute: "The cardholder was charged $299.00 for an annual membership renewal from 'PROACCESS BUSINESS TOOLS'. The cardholder states they were not notified of the upcoming renewal as required by Mastercard rules for recurring transactions. Mastercard rules require merchants to notify cardholders of upcoming recurring charges at least 7 days before billing. No notification was provided.",
    contactedMerchant: true,
    merchantResponse: "The cardholder contacted ProAccess [5 days after the charge]. The merchant acknowledged they did not send a pre-renewal notification for this billing cycle due to a system error but refused to refund the charge.",
    resolution: "Full reversal. The merchant violated Mastercard's recurring transaction notification requirements.",
    docs: [
      { type: "Cardholder Statement", desc: "No pre-renewal notification email was received." },
      { type: "Mastercard Rule Reference", desc: "Mastercard requires merchants to provide advance notification before recurring charges." }
    ],
    commentary: "Filing under 4899. The merchant failed to comply with Mastercard's rule requiring advance notification before processing recurring charges. The merchant acknowledged the notification failure.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Match" },
    cvv: { code: "M", desc: "CVV on file" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "ProAccess Business Tools — Annual Membership Renewal", qty: 1, price: 299.00, sku: "PA-BT-ANN" }],
    orderStatus: "Renewed — annual membership",
    shippingAddr: null,
    orderNotes: "Annual recurring charge. The cardholder's original signup was [1 year before this charge]. The pre-renewal notification email was not sent due to a system configuration error during a platform update. The merchant acknowledges this was an oversight.",
    emails: [
      { type: "Original Signup Confirmation", dir: "merchant_to_customer", timing: "[1 year before the disputed charge]",
        subject: "Welcome to ProAccess Business Tools — Annual Plan",
        body: "Your annual membership is active! Plan: Annual ($299.00/year). Your membership renews automatically each year. We will notify you before each renewal. You can cancel anytime from your account settings." },
      { type: "Renewal Charge Notification (NOT SENT)", dir: "system_note", timing: "[7 days before the disputed charge — notification was scheduled but NOT sent]",
        subject: "SYSTEM ERROR: Pre-renewal notification email was NOT sent",
        body: "The automated pre-renewal notification email, which should have been sent 7 days before the annual renewal date, failed to send due to a misconfiguration in the email notification system after a platform update. The merchant acknowledges this failure." },
      { type: "Customer Complaint", dir: "customer_to_merchant", timing: "[5 days after the disputed charge]",
        subject: "Unauthorized renewal charge — $299.00",
        body: "I was charged $299.00 for an annual renewal but I was never notified this was coming. I would have cancelled if I had been reminded. I want a full refund." },
      { type: "Merchant Response", dir: "merchant_to_customer", timing: "[6 days after the disputed charge]",
        subject: "RE: Unauthorized renewal charge",
        body: "We sincerely apologize for the lack of notification before your renewal. Due to a system update, the pre-renewal reminder email was not sent. However, your annual membership has been renewed and you have full access to all features. We're unable to process a refund at this time but have escalated your case to our billing department." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Recurring — card on file",
    threeDS: { enrolled: true, authenticated: true, eci: "05", cavv: "Verified at initial signup" },
    authNotes: "Recurring charge using card-on-file from original signup. Authorization obtained. The pre-renewal notification required by Mastercard rules was not sent due to a system error.",
    settlementNotes: "Settled for $299.00. The merchant acknowledges the pre-renewal notification was not sent, which is a violation of Mastercard's recurring transaction rules.",
    riskDevice: { status: "Known device — used during original signup", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "Consistent with cardholder." },
    riskScore: "Low fraud risk — but rule violation present",
    avsCode: "Y", avsDesc: "Match from original signup",
    cvvCode: "M", cvvDesc: "CVV on file from enrollment",
    refundWindow: "N/A — annual plan",
    refundPolicy: "Annual memberships may be cancelled at any time. No prorated refunds after 14 days from renewal.",
    refundDisclosure: "Terms on website and in signup confirmation.",
    refundAck: "Customer agreed during original signup.",
    fulfillment: { type: "Digital Service", status: "Active — cardholder has full access",
      method: "Online platform access", timing: "Continuous since original signup", confirmed: true,
      notes: "The cardholder's account is active and accessible. However, the pre-renewal notification required by Mastercard rules was not sent." },
    threeDSRecord: { version: "2.0", enrolled: true, status: "Y — Authenticated at signup", eci: "05",
      cavv: "Verified", challenge: true, liabilityShift: "Yes — at initial enrollment",
      notes: "3D Secure at original signup. Subsequent recurring charges use authenticated credentials." }
  }
},

"4900": {
  fav: "acquirer", desc: "Mastercard Compliance",
  product: "a software license from an online software vendor",
  merchant: "DevToolsPro",
  issuer: {
    dispute: "The issuer filed a compliance chargeback for a $79.00 transaction from 'DEVTOOLSPRO'. The issuer asserts that the merchant did not include all required data elements in the transaction record as mandated by Mastercard's processing standards.",
    contactedMerchant: false,
    merchantResponse: "N/A — compliance issue identified during routine transaction review by the issuer.",
    resolution: "Reversal for non-compliance with Mastercard processing requirements.",
    docs: [
      { type: "Compliance Review Report", desc: "Issuer's review identifying missing data elements in the transaction record." }
    ],
    commentary: "Filing under 4900. The transaction record is missing data elements required by Mastercard compliance standards.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Full match" },
    cvv: { code: "M", desc: "CVV Match" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "DevToolsPro IDE License — Annual, Single User", qty: 1, price: 79.00, sku: "DTP-IDE-ANN" }],
    orderStatus: "Completed — license delivered",
    shippingAddr: null,
    orderNotes: "Digital product. Software license key delivered via email. Transaction was processed through a PCI-compliant payment gateway with all required Mastercard data elements.",
    emails: [
      { type: "Order Confirmation & License Delivery", dir: "merchant_to_customer", timing: "Immediately after purchase",
        subject: "DevToolsPro — Your License Key for IDE Annual",
        body: "Thank you for purchasing DevToolsPro IDE! Your annual license key is included below. Total: $79.00. Activate your license in the application settings. Support available at support@devtoolspro.com." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce",
    threeDS: { enrolled: true, authenticated: true, eci: "05", cavv: "Verified" },
    authNotes: "Authorization request included all required data fields: merchant category code, terminal ID, acquirer reference number, and all mandatory Mastercard data elements. Transaction processed through PCI-DSS Level 1 certified gateway.",
    settlementNotes: "Settled for $79.00. The acquirer has verified that all required Mastercard data elements are present in the transaction record. The acquirer's compliance team has confirmed the transaction meets all current processing standards.",
    riskDevice: { status: "Known device", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "Consistent with cardholder." },
    riskScore: "Low",
    avsCode: "Y", avsDesc: "Full match",
    cvvCode: "M", cvvDesc: "CVV matched",
    refundWindow: "30 days from purchase",
    refundPolicy: "Full refund within 30 days if license is not activated.",
    refundDisclosure: "On checkout page and in confirmation email.",
    refundAck: "Customer agreed to terms at checkout.",
    fulfillment: { type: "Digital Delivery", status: "Delivered — license key sent via email",
      method: "Email delivery of license key", timing: "Immediately after purchase", confirmed: true,
      notes: "License key delivered and activated by the customer [1 day after purchase]." },
    threeDSRecord: { version: "2.0", enrolled: true, status: "Y — Fully Authenticated", eci: "05",
      cavv: "Verified", challenge: true, liabilityShift: "Yes",
      notes: "3D Secure completed." }
  }
},

"4901": {
  fav: "issuer", desc: "Mastercard Compliance — Violation of Standards",
  product: "an in-store electronics purchase at a retailer not maintaining PCI compliance",
  merchant: "BargainTech Electronics",
  issuer: {
    dispute: "The issuer identified that a $189.00 transaction at 'BARGAINTECH ELECTRONICS' was processed at a terminal that does not meet Mastercard's current security standards. The merchant's payment terminal is running outdated firmware that has known security vulnerabilities, and the merchant's PCI-DSS compliance certification has lapsed.",
    contactedMerchant: false,
    merchantResponse: "N/A — compliance violation identified by the issuer's security review.",
    resolution: "Full reversal due to merchant's failure to maintain Mastercard-required security standards.",
    docs: [
      { type: "Terminal Security Assessment", desc: "Report showing the merchant's terminal firmware is outdated with known vulnerabilities." },
      { type: "PCI Compliance Status", desc: "Records showing the merchant's PCI-DSS certification has expired." }
    ],
    commentary: "Filing under 4901. The merchant is operating with outdated terminal security and lapsed PCI-DSS compliance. This violates Mastercard's standards for transaction processing security.",
    cardPresent: true, posEntry: "90",
    avs: { code: "N/A", desc: "Card-present" },
    cvv: { code: "N/A", desc: "Magnetic stripe" },
    riskFlags: { geoMismatch: "None", deviceTrust: "N/A" }
  },
  acquirer: {
    items: [{ name: "Bluetooth Portable Speaker — Waterproof", qty: 1, price: 189.00, sku: "BTE-BPS-WP" }],
    orderStatus: "Completed",
    shippingAddr: null,
    orderNotes: "In-store purchase. The merchant's POS terminal is running firmware version that is 2 generations behind the current required version. The merchant's PCI-DSS compliance assessment expired [3 months before this transaction]. The acquirer was in the process of working with the merchant on re-certification.",
    emails: [
      { type: "No Communications", dir: "system_note", timing: "N/A",
        subject: "N/A", body: "In-store card-present transaction. The merchant's POS terminal firmware is outdated and PCI-DSS compliance has lapsed. The acquirer acknowledges the compliance gap and has been working with the merchant on re-certification." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Magnetic Stripe — Outdated Terminal", threeDS: null,
    authNotes: "Authorization obtained. However, the terminal's security firmware is outdated and the merchant's PCI compliance has lapsed. The acquirer acknowledges the compliance deficiency.",
    settlementNotes: "Settled for $189.00. The merchant's PCI-DSS compliance expired [3 months before this transaction]. Terminal firmware is outdated with known vulnerabilities.",
    riskDevice: { status: "N/A — card-present, non-compliant terminal", trust: "N/A", match: false },
    riskIP: { level: "N/A", proxy: false, geoMatch: true, notes: "Card-present." },
    riskScore: "Medium-High — non-compliant terminal and lapsed PCI certification",
    avsCode: "N/A", avsDesc: "Card-present",
    cvvCode: "N/A", cvvDesc: "Magnetic stripe",
    refundWindow: "30 days from purchase with receipt",
    refundPolicy: "Full refund within 30 days with receipt.",
    refundDisclosure: "On receipt.",
    refundAck: "Receipt provided.",
    fulfillment: null,
    threeDSRecord: null
  }
},

"4902": {
  fav: "acquirer", desc: "Mastercard Compliance — Below Floor Limit",
  product: "a coffee and pastry at a café",
  merchant: "Morning Brew Café",
  issuer: {
    dispute: "The issuer asserts that a $12.50 transaction at 'MORNING BREW CAFÉ' was processed without authorization. Even though the amount is below the floor limit, the issuer contends that the merchant should have obtained authorization given the card's risk status.",
    contactedMerchant: false,
    merchantResponse: "N/A — compliance issue raised by the issuer.",
    resolution: "Reversal for processing without authorization.",
    docs: [
      { type: "Transaction Record", desc: "Showing no authorization was requested for this transaction." }
    ],
    commentary: "Filing under 4902. Transaction processed below floor limit without authorization.",
    cardPresent: true, posEntry: "05",
    avs: { code: "N/A", desc: "Card-present EMV" },
    cvv: { code: "N/A", desc: "Chip cryptogram" },
    riskFlags: { geoMismatch: "None", deviceTrust: "N/A" }
  },
  acquirer: {
    items: [
      { name: "Large Cappuccino", qty: 1, price: 5.50, sku: "MBC-CAP-L" },
      { name: "Almond Croissant", qty: 1, price: 4.50, sku: "MBC-AC" },
      { name: "Bottled Water", qty: 1, price: 2.50, sku: "MBC-BW" }
    ],
    orderStatus: "Completed",
    shippingAddr: null,
    orderNotes: "In-store café purchase. $12.50 is within the applicable floor limit for this merchant category. Mastercard rules permit transactions below the floor limit to be processed without real-time authorization. The merchant obtained a post-authorization after the sale, which was approved.",
    emails: [
      { type: "No Communications", dir: "system_note", timing: "N/A",
        subject: "N/A", body: "In-store café purchase for $12.50. EMV chip was read. Transaction is below the applicable floor limit for cafés/restaurants. A post-authorization was obtained later and was approved." }
    ],
    authObtained: false, authResponse: "N/A — below floor limit", authMessage: "Post-authorization obtained and approved",
    entryMode: "EMV Chip — Card Present", threeDS: null,
    authNotes: "Transaction amount of $12.50 is within the applicable floor limit for Merchant Category Code 5812 (Eating Places/Restaurants). Mastercard rules permit processing without real-time authorization for below-floor-limit transactions. A post-authorization was submitted and approved, confirming the transaction's legitimacy.",
    settlementNotes: "Settled for $12.50. Below-floor-limit transaction processed per Mastercard's floor limit provisions. Post-authorization approved.",
    riskDevice: { status: "N/A — card-present chip terminal", trust: "N/A", match: true },
    riskIP: { level: "N/A", proxy: false, geoMatch: true, notes: "Card-present at café." },
    riskScore: "Very Low — small amount, chip verified, post-auth approved",
    avsCode: "N/A", avsDesc: "Card-present EMV — chip verified",
    cvvCode: "N/A", cvvDesc: "Chip cryptogram validated",
    refundWindow: "N/A — café purchase",
    refundPolicy: "Café purchases are final.",
    refundDisclosure: "N/A — standard café transaction.",
    refundAck: "Customer initiated purchase.",
    fulfillment: null,
    threeDSRecord: null
  }
},

"4903": {
  fav: "acquirer", desc: "Mastercard Compliance — Exceeds Limit",
  product: "a high-value watch from an authorized dealer",
  merchant: "TimePiece Gallery — Authorized Dealer",
  issuer: {
    dispute: "The issuer asserts that a $3,200.00 transaction at 'TIMEPIECE GALLERY' exceeded the applicable processing limit and authorization was not properly obtained. The issuer claims the transaction should have required enhanced authorization.",
    contactedMerchant: false,
    merchantResponse: "N/A — compliance issue raised by the issuer.",
    resolution: "Reversal for exceeding processing limits without proper authorization.",
    docs: [
      { type: "Transaction Record", desc: "Showing the transaction amount and authorization details." }
    ],
    commentary: "Filing under 4903. The transaction exceeded applicable limits.",
    cardPresent: true, posEntry: "05",
    avs: { code: "N/A", desc: "Card-present EMV" },
    cvv: { code: "N/A", desc: "Chip cryptogram" },
    riskFlags: { geoMismatch: "None", deviceTrust: "N/A" }
  },
  acquirer: {
    items: [{ name: "Swiss Automatic Chronograph Watch — Stainless Steel", qty: 1, price: 3200.00, sku: "TPG-SAC-SS" }],
    orderStatus: "Completed — customer left with merchandise",
    shippingAddr: null,
    orderNotes: "In-store purchase at authorized dealer. EMV chip transaction with PIN verified. Real-time authorization was requested and received with approval code. The authorization request included the full amount of $3,200.00 and was approved by the issuer without a referral or decline.",
    emails: [
      { type: "Purchase Receipt & Warranty", dir: "merchant_to_customer", timing: "After purchase",
        subject: "TimePiece Gallery — Purchase Receipt & Warranty Registration",
        body: "Congratulations on your Swiss Automatic Chronograph! Total: $3,200.00. Your 2-year manufacturer's warranty is registered. Certificate of authenticity included with your purchase." }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved — full amount authorized",
    entryMode: "EMV Chip + PIN — Card Present", threeDS: null,
    authNotes: "Real-time authorization was requested for the full $3,200.00 amount and approved by the issuer with a valid approval code. The issuer's authorization system approved the transaction without a referral or step-up requirement. The merchant followed proper procedures by requesting authorization for the full amount before completing the sale.",
    settlementNotes: "Settled for $3,200.00 — matches the authorized amount exactly. The issuer approved the full amount at the time of authorization. EMV chip was read and PIN was verified. The transaction was processed in full compliance with Mastercard authorization requirements.",
    riskDevice: { status: "N/A — chip+PIN POS terminal", trust: "N/A", match: true },
    riskIP: { level: "N/A", proxy: false, geoMatch: true, notes: "Card-present at authorized dealer." },
    riskScore: "Low — high-value but properly authorized chip+PIN transaction at authorized dealer",
    avsCode: "N/A", avsDesc: "Card-present EMV+PIN",
    cvvCode: "N/A", cvvDesc: "Chip cryptogram validated + PIN verified",
    refundWindow: "14 days from purchase",
    refundPolicy: "Full refund within 14 days if watch returned in original condition with all documentation and packaging. After 14 days, manufacturer warranty applies.",
    refundDisclosure: "On receipt and warranty card.",
    refundAck: "Customer signed receipt acknowledging purchase and return policy.",
    fulfillment: null,
    threeDSRecord: null
  }
},

"4905": {
  fav: "acquirer", desc: "Mastercard Compliance — Card Not Valid",
  product: "monthly gym membership charge",
  merchant: "FitZone Fitness Center",
  issuer: {
    dispute: "The issuer asserts that a $49.99 monthly charge from 'FITZONE FITNESS CENTER' was processed on a card that the issuer had flagged as not valid. The card's status was changed due to a suspected compromise, but the recurring charge continued to process.",
    contactedMerchant: false,
    merchantResponse: "N/A — compliance issue identified by the issuer.",
    resolution: "Reversal for processing on a card flagged as not valid.",
    docs: [
      { type: "Card Status Record", desc: "Showing the card was flagged as not valid before this charge." }
    ],
    commentary: "Filing under 4905. A recurring charge was processed on a card flagged as not valid.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "On file" },
    cvv: { code: "M", desc: "On file from enrollment" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "FitZone Fitness — Monthly Membership (Unlimited Access)", qty: 1, price: 49.99, sku: "FZ-MEM-UNL" }],
    orderStatus: "Active membership — monthly recurring",
    shippingAddr: null,
    orderNotes: "Monthly recurring charge on a membership active for [8 months]. The recurring billing was established with a valid card at signup. The issuer's account updater service provided updated card credentials after the card was reissued, allowing the recurring charge to continue processing on the new credentials. The merchant processed the charge using the updated credentials provided by the issuer's own account updater.",
    emails: [
      { type: "Membership Signup", dir: "merchant_to_customer", timing: "[8 months before the disputed charge]",
        subject: "Welcome to FitZone Fitness!",
        body: "Your unlimited membership is active! $49.99/month, billed on the 1st of each month. Cancel anytime with 30 days notice." },
      { type: "Monthly Billing Receipt", dir: "merchant_to_customer", timing: "On the disputed charge date",
        subject: "FitZone Fitness — Monthly Membership Receipt",
        body: "Your monthly membership of $49.99 has been processed. Thank you for being a FitZone member!" }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Recurring — card on file (updated via Account Updater)",
    threeDS: { enrolled: true, authenticated: true, eci: "05", cavv: "Verified at enrollment" },
    authNotes: "Recurring charge processed using card credentials updated through Mastercard's Account Updater service. The issuer's own Account Updater provided the new card details after the original card was reissued. Authorization was requested with the updated credentials and was approved by the issuer.",
    settlementNotes: "Settled for $49.99. Card credentials were automatically updated via Mastercard Account Updater. The issuer approved the authorization request with the updated credentials. The cardholder has been an active member for 8 months.",
    riskDevice: { status: "N/A — recurring billing", trust: "High", match: true },
    riskIP: { level: "N/A", proxy: false, geoMatch: true, notes: "Recurring — no IP for this transaction." },
    riskScore: "Low — established recurring membership with Account Updater credentials",
    avsCode: "Y", avsDesc: "On file from enrollment",
    cvvCode: "M", cvvDesc: "On file — Account Updater provided updated credentials",
    refundWindow: "N/A — membership",
    refundPolicy: "Cancel anytime with 30 days notice. No refund for the current billing period.",
    refundDisclosure: "In membership agreement and signup confirmation.",
    refundAck: "Customer signed membership agreement at signup.",
    fulfillment: { type: "Physical Access Service", status: "Active — member visited gym [3 days before the disputed charge]",
      method: "Gym key card access", timing: "Ongoing since enrollment", confirmed: true,
      notes: "Access logs show the cardholder scanned into the gym [3 days before this charge], [5 days before this charge], and [8 days before this charge]. Active usage throughout the membership period." },
    threeDSRecord: { version: "2.0", enrolled: true, status: "Y — Authenticated at signup", eci: "05",
      cavv: "Verified", challenge: true, liabilityShift: "Yes",
      notes: "3D Secure at enrollment. Recurring charges use authenticated credentials updated via Account Updater." }
  }
},

"4908": {
  fav: "acquirer", desc: "Mastercard Compliance — Authorization Related",
  product: "a pre-ordered limited edition book",
  merchant: "BookHaven — Rare & Collectible Books",
  issuer: {
    dispute: "The issuer asserts that a $95.00 charge from 'BOOKHAVEN' did not follow proper authorization procedures. The charge was made [60 days after the original authorization], which the issuer claims exceeds the valid authorization window.",
    contactedMerchant: false,
    merchantResponse: "N/A — authorization compliance issue raised by the issuer.",
    resolution: "Reversal for authorization-related compliance violation — authorization may have expired.",
    docs: [
      { type: "Authorization Timeline", desc: "Showing the original authorization date and the clearing date." }
    ],
    commentary: "Filing under 4908. The clearing occurred [60 days after authorization], which may exceed the valid authorization window.",
    cardPresent: false, posEntry: "81",
    avs: { code: "Y", desc: "Full match" },
    cvv: { code: "M", desc: "CVV Match" },
    riskFlags: { geoMismatch: "None", deviceTrust: "High" }
  },
  acquirer: {
    items: [{ name: "Limited Edition Hardcover — 'The Art of Coding' (Signed by Author, #342/500)", qty: 1, price: 95.00, sku: "BH-TAOC-LE" }],
    orderStatus: "Completed — delivered",
    shippingAddr: "Cardholder's billing address",
    orderNotes: "Pre-order for a limited edition book. Customer placed the order [60 days before delivery] with the understanding that the book would ship when published. A new authorization was obtained [2 days before shipment] per Mastercard's pre-order authorization rules. The original authorization was only a verification, not the settlement authorization.",
    emails: [
      { type: "Pre-Order Confirmation", dir: "merchant_to_customer", timing: "[60 days before shipment]",
        subject: "BookHaven — Pre-Order Confirmed: 'The Art of Coding' Limited Edition",
        body: "Your pre-order is confirmed! Limited Edition 'The Art of Coding' (Signed by Author). Total: $95.00. Expected publication and shipment date: approximately [60 days from now]. Your card will be charged when the book ships. A temporary hold may appear and will be released." },
      { type: "Shipping & Charge Notification", dir: "merchant_to_customer", timing: "[2 days before delivery]",
        subject: "BookHaven — Your Pre-Order Has Shipped! 'The Art of Coding'",
        body: "Great news! 'The Art of Coding' Limited Edition has been published and your copy (#342/500) is on its way! Your card has been charged $95.00. Tracking number included." },
      { type: "Delivery Confirmation", dir: "merchant_to_customer", timing: "On delivery date",
        subject: "Delivered — BookHaven Pre-Order 'The Art of Coding'",
        body: "Your signed limited edition copy has been delivered to your address. Enjoy!" }
    ],
    authObtained: true, authResponse: "00", authMessage: "Approved",
    entryMode: "Ecommerce — pre-order",
    threeDS: { enrolled: true, authenticated: true, eci: "05", cavv: "Verified at pre-order" },
    authNotes: "Per Mastercard's delayed/pre-order shipment rules, a new authorization was obtained [2 days before shipment], within the valid authorization window. The original pre-order authorization was used only for card verification and was not used for settlement. The settlement authorization matches the clearing date and is within the permitted timeframe.",
    settlementNotes: "Settled for $95.00. A fresh authorization was obtained [2 days before shipment and clearing], within the valid authorization window. The merchant followed Mastercard's pre-order authorization procedures correctly.",
    riskDevice: { status: "Known device — same as pre-order", trust: "High", match: true },
    riskIP: { level: "Low", proxy: false, geoMatch: true, notes: "Consistent with cardholder." },
    riskScore: "Low — pre-order with proper re-authorization before shipment",
    avsCode: "Y", avsDesc: "Full match",
    cvvCode: "M", cvvDesc: "CVV matched on re-authorization",
    refundWindow: "30 days from delivery",
    refundPolicy: "Full refund within 30 days if book returned in original condition. Pre-orders may be cancelled anytime before shipment for a full refund.",
    refundDisclosure: "In pre-order confirmation email and on website.",
    refundAck: "Customer agreed to pre-order terms including delayed shipment and billing.",
    fulfillment: { type: "Physical Shipment", status: "Delivered",
      method: "USPS Priority — insured", timing: "Delivered [60 days after pre-order]", confirmed: true,
      notes: "Limited edition book (#342/500) delivered to cardholder's billing address." },
    threeDSRecord: { version: "2.0", enrolled: true, status: "Y — Authenticated at pre-order", eci: "05",
      cavv: "Verified", challenge: true, liabilityShift: "Yes",
      notes: "3D Secure at pre-order. Re-authorization obtained before shipment." }
  }
}

};

// ============================================================
// FILE GENERATION
// ============================================================

function generateIssuerFiles(code, story) {
  const dir = `${ISSUER_ROOT}/${code}`;
  mkdirp(`${dir}/customer-comms`);
  mkdirp(`${dir}/merchant`);
  mkdirp(`${dir}/psp`);
  const iss = story.issuer;

  writeJson(`${dir}/customer-comms/cardholder_dispute_statement.json`, {
    source: "Issuing Bank — Cardholder Services",
    dataType: "Cardholder Dispute Statement",
    reasonCode: code,
    reasonCodeDescription: story.desc,
    statement: {
      disputeReason: `${code} - ${story.desc}`,
      detailedDescription: iss.dispute,
      contactedMerchant: iss.contactedMerchant,
      merchantResponseSummary: iss.merchantResponse,
      previousDisputeHistory: "None",
      requestedResolution: iss.resolution,
      cardholderDeclaration: "I declare that the information provided in this dispute statement is true, accurate, and complete to the best of my knowledge and belief."
    },
    bankInternalNotes: {
      riskAssessment: story.fav === "issuer" ? "Medium-High — evidence supports cardholder's position." : "Medium — merchant may have valid defense documentation.",
      recommendedAction: `Proceed with chargeback filing under Mastercard reason code ${code} (${story.desc}).`,
      priority: "Standard"
    }
  });

  writeJson(`${dir}/merchant/issuer_chargeback_documentation.json`, {
    source: "Issuing Bank — Chargeback Department",
    dataType: "Issuer Chargeback Documentation",
    reasonCode: code,
    reasonCodeDescription: story.desc,
    chargebackFiling: {
      reasonCode: code,
      reasonCodeDescription: story.desc,
      supportingDocuments: iss.docs,
      issuerCommentary: iss.commentary,
      arbitrationEligible: true
    },
    transactionDetails: {
      cardPresent: iss.cardPresent,
      ecommerceIndicator: iss.cardPresent ? null : "ECI_07",
      posEntryMode: iss.posEntry
    }
  });

  writeJson(`${dir}/psp/issuer_transaction_record.json`, {
    source: "Issuing Bank — Transaction Processing",
    dataType: "Issuer Transaction Record",
    reasonCode: code,
    reasonCodeDescription: story.desc,
    authorization: {
      responseCode: iss.avs.code === "N/A" && iss.cardPresent ? "00" : (iss.riskFlags.deviceTrust === "Low" ? "00" : "00"),
      merchantCategoryCode: "5999",
      posEntryMode: iss.posEntry,
      cardNotPresent: !iss.cardPresent,
      avsResult: { code: iss.avs.code, description: iss.avs.desc },
      cvvResult: { code: iss.cvv.code, description: iss.cvv.desc }
    },
    riskFlags: {
      geolocationMismatch: iss.riskFlags.geoMismatch,
      deviceTrustScore: iss.riskFlags.deviceTrust
    }
  });
}

function generateAcquirerFiles(code, story) {
  const dir = `${ACQUIRER_ROOT}/${code}`;
  mkdirp(`${dir}/customer-comms`);
  mkdirp(`${dir}/merchant`);
  mkdirp(`${dir}/psp`);
  mkdirp(`${dir}/fraud-tools`);
  mkdirp(`${dir}/identity`);
  const acq = story.acquirer;

  writeJson(`${dir}/merchant/order_details.json`, {
    source: `${story.merchant} — Order Management System`,
    dataType: "Order Details",
    reasonCode: code,
    reasonCodeDescription: story.desc,
    orderRecord: {
      orderStatus: acq.orderStatus,
      items: acq.items.map(i => ({ name: i.name, quantity: i.qty, unitPrice: i.price, sku: i.sku })),
      shippingAddress: acq.shippingAddr,
      orderNotes: acq.orderNotes
    }
  });

  writeJson(`${dir}/customer-comms/email_logs.json`, {
    source: `${story.merchant} — Customer Communications`,
    dataType: "Email Communication Logs",
    reasonCode: code,
    reasonCodeDescription: story.desc,
    communications: acq.emails
  });

  writeJson(`${dir}/psp/auth_log.json`, {
    source: `${story.merchant} — Payment Service Provider`,
    dataType: "Authorization Log",
    reasonCode: code,
    reasonCodeDescription: story.desc,
    authorizationRecord: {
      authorizationObtained: acq.authObtained,
      responseCode: acq.authResponse,
      responseMessage: acq.authMessage,
      entryMode: acq.entryMode,
      threeDSecure: acq.threeDS,
      notes: acq.authNotes
    }
  });

  writeJson(`${dir}/psp/settlement_record.json`, {
    source: `${story.merchant} — Payment Service Provider`,
    dataType: "Settlement Record",
    reasonCode: code,
    reasonCodeDescription: story.desc,
    settlementRecord: { notes: acq.settlementNotes }
  });

  writeJson(`${dir}/fraud-tools/risk_assessment.json`, {
    source: `${story.merchant} — Fraud Prevention`,
    dataType: "Device Fingerprint & Risk Assessment",
    reasonCode: code,
    reasonCodeDescription: story.desc,
    riskAssessment: {
      deviceFingerprint: acq.riskDevice,
      ipAnalysis: acq.riskIP,
      overallRiskScore: acq.riskScore
    }
  });

  writeJson(`${dir}/identity/avs_cvv_check.json`, {
    source: `${story.merchant} — Identity Verification`,
    dataType: "AVS/CVV Verification Results",
    reasonCode: code,
    reasonCodeDescription: story.desc,
    verificationResults: {
      avs: { responseCode: acq.avsCode, description: acq.avsDesc },
      cvv: { responseCode: acq.cvvCode, description: acq.cvvDesc }
    }
  });

  writeJson(`${dir}/merchant/refund_policy.json`, {
    source: `${story.merchant} — Policies`,
    dataType: "Refund & Cancellation Policy",
    reasonCode: code,
    reasonCodeDescription: story.desc,
    policy: {
      refundWindow: acq.refundWindow,
      policy: acq.refundPolicy,
      disclosureMethod: acq.refundDisclosure,
      customerAcknowledgment: acq.refundAck
    }
  });

  if (acq.fulfillment) {
    writeJson(`${dir}/merchant/fulfillment_record.json`, {
      source: `${story.merchant} — Fulfillment`,
      dataType: "Fulfillment & Delivery Record",
      reasonCode: code,
      reasonCodeDescription: story.desc,
      fulfillmentRecord: acq.fulfillment
    });
  }

  if (acq.threeDSRecord) {
    mkdirp(`${dir}/device`);
    writeJson(`${dir}/device/3ds_authentication.json`, {
      source: `${story.merchant} — Authentication`,
      dataType: "3D Secure Authentication Record",
      reasonCode: code,
      reasonCodeDescription: story.desc,
      threeDSecure: acq.threeDSRecord
    });
  }
}

// Generate all files
let fileCount = 0;
for (const [code, story] of Object.entries(STORIES)) {
  generateIssuerFiles(code, story);
  generateAcquirerFiles(code, story);
  const issuerFiles = fs.readdirSync(`${ISSUER_ROOT}/${code}`, { recursive: true }).filter(f => f.endsWith('.json'));
  const acqFiles = fs.readdirSync(`${ACQUIRER_ROOT}/${code}`, { recursive: true }).filter(f => f.endsWith('.json'));
  fileCount += issuerFiles.length + acqFiles.length;
}

console.log(`\nGenerated ${fileCount} evidence files for ${Object.keys(STORIES).length} reason codes.`);
console.log(`Issuer favored: ${Object.values(STORIES).filter(s => s.fav === 'issuer').length}`);
console.log(`Acquirer favored: ${Object.values(STORIES).filter(s => s.fav === 'acquirer').length}`);

// Report which codes favor which side
console.log(`\n=== ISSUER WINS ===`);
Object.entries(STORIES).filter(([,s]) => s.fav === 'issuer').forEach(([c,s]) => console.log(`${c} - ${s.desc}`));
console.log(`\n=== ACQUIRER WINS ===`);
Object.entries(STORIES).filter(([,s]) => s.fav === 'acquirer').forEach(([c,s]) => console.log(`${c} - ${s.desc}`));
