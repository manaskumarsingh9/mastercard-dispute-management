const fs = require('fs');
const path = require('path');

const ACQUIRER_ROOT = path.join(__dirname, '..', 'src', 'data', 'sources', 'acquirer');
const ISSUER_ROOT = path.join(__dirname, '..', 'src', 'data', 'sources', 'issuer');

const cases = [
  { id: 2, case: 20, claim: '900002077882', merchant: 'Digital Goods LLC', reason: '4834', amount: 34.99, currency: 'USD', type: 'Processing Error', merchantId: '009403780074', acquirerId: '009222', issuerId: '949456', pan: '9123435392786516424', txnId: '983841698', createDate: '2026-03-15' },
  { id: 3, case: 21, claim: '900002081009', merchant: 'Enterprise Solutions', reason: '4834', amount: 24000, currency: 'USD', type: 'Processing Error', merchantId: '009403091254', acquirerId: '009222', issuerId: '953309', pan: '9123435216996590909', txnId: '980145359', createDate: '2026-03-16' },
  { id: 4, case: 22, claim: '900002084136', merchant: 'FreeTrial Corp', reason: '4834', amount: 0, currency: 'USD', type: 'Processing Error', merchantId: '009403638252', acquirerId: '009222', issuerId: '916334', pan: '9123435673607581739', txnId: '927809341', createDate: '2026-03-17' },
  { id: 5, case: 23, claim: '900002087263', merchant: 'Cloud Services Inc', reason: '4863', amount: 8, currency: 'USD', type: 'Fraud', merchantId: '009403620499', acquirerId: '009222', issuerId: '970767', pan: '9123435828224172092', txnId: '931889038', createDate: '2026-03-18' },
  { id: 6, case: 24, claim: '900002090390', merchant: 'SaaS Platform', reason: '4841', amount: 8, currency: 'USD', type: 'Consumer Dispute', merchantId: '009403586477', acquirerId: '009222', issuerId: '918823', pan: '9123435534752987685', txnId: '940584054', createDate: '2026-03-19' },
  { id: 7, case: 25, claim: '900002093517', merchant: 'Payment Gateway', reason: '4834', amount: 8, currency: 'USD', type: 'Processing Error', merchantId: '009403546917', acquirerId: '009222', issuerId: '945046', pan: '9123435949669614946', txnId: '980382766', createDate: '2026-03-20' },
  { id: 8, case: 26, claim: '900002096644', merchant: 'Luxury Goods Ltd', reason: '4837', amount: 1250, currency: 'USD', type: 'Fraud', merchantId: '009403037985', acquirerId: '009222', issuerId: '941745', pan: '9123435611837928380', txnId: '953344667', createDate: '2026-03-21' },
  { id: 9, case: 27, claim: '900002099771', merchant: 'Electronics Hub', reason: '4855', amount: 567.50, currency: 'USD', type: 'Consumer Dispute', merchantId: '009403041036', acquirerId: '009222', issuerId: '928445', pan: '9123435029209802141', txnId: '972850548', createDate: '2026-03-22' },
  { id: 10, case: 28, claim: '900002102898', merchant: 'Online Marketplace', reason: '4855', amount: 89.99, currency: 'USD', type: 'Consumer Dispute', merchantId: '009403818752', acquirerId: '009222', issuerId: '977452', pan: '9123435906146823005', txnId: '982137321', createDate: '2026-03-23' },
  { id: 11, case: 29, claim: '900002106025', merchant: 'Retail Store', reason: '4871', amount: 320, currency: 'USD', type: 'Fraud', merchantId: '009403304411', acquirerId: '009222', issuerId: '910204', pan: '9123435856715347946', txnId: '919016207', createDate: '2026-03-24' },
  { id: 12, case: 30, claim: '900002109152', merchant: 'Travel Agency', reason: '4859', amount: 2100, currency: 'USD', type: 'Consumer Dispute', merchantId: '009403384445', acquirerId: '009222', issuerId: '948762', pan: '9123435944092636435', txnId: '956366942', createDate: '2026-03-25' },
  { id: 13, case: 31, claim: '900002112279', merchant: 'Subscription Box Co', reason: '4831', amount: 150, currency: 'USD', type: 'Processing Error', merchantId: '009403312323', acquirerId: '009222', issuerId: '979228', pan: '9123435212373010727', txnId: '955547661', createDate: '2026-03-26' },
  { id: 14, case: 32, claim: '900002115406', merchant: 'Food Delivery App', reason: '4853', amount: 45, currency: 'USD', type: 'Consumer Dispute', merchantId: '009403096761', acquirerId: '009222', issuerId: '965123', pan: '9123435873649807903', txnId: '952871447', createDate: '2026-03-27' },
  { id: 15, case: 33, claim: '900002118533', merchant: 'Auto Parts Store', reason: '4808', amount: 780, currency: 'USD', type: 'Authorization', merchantId: '009403137236', acquirerId: '009222', issuerId: '933649', pan: '9123435746554559399', txnId: '938155939', createDate: '2026-03-28' },
  { id: 16, case: 34, claim: '900002121660', merchant: 'Jewelry Online', reason: '4863', amount: 3500, currency: 'USD', type: 'Fraud', merchantId: '009403221909', acquirerId: '009222', issuerId: '938744', pan: '9123435793360035277', txnId: '921598787', createDate: '2026-03-29' },
  { id: 17, case: 35, claim: '900002124787', merchant: 'Streaming Service', reason: '4841', amount: 99.99, currency: 'USD', type: 'Consumer Dispute', merchantId: '009403605824', acquirerId: '009222', issuerId: '938465', pan: '9123435404959988344', txnId: '970960742', createDate: '2026-03-30' },
  { id: 18, case: 36, claim: '900002127914', merchant: 'Fashion Outlet', reason: '4853', amount: 210.50, currency: 'USD', type: 'Consumer Dispute', merchantId: '009403150424', acquirerId: '009222', issuerId: '968950', pan: '9123435894154493924', txnId: '904712821', createDate: '2026-03-31' }
];

function readIssuerStatement(caseNum) {
  const file = path.join(ISSUER_ROOT, 'customer-comms', `cardholder_dispute_statement_case_${caseNum}.json`);
  if (fs.existsSync(file)) return JSON.parse(fs.readFileSync(file, 'utf8'));
  return null;
}

function getIssuerFacts(issuer) {
  if (!issuer || !issuer.statement) return {};
  const s = issuer.statement;
  return {
    cardholderName: s.cardholderName,
    disputeReason: s.disputeReason,
    description: s.detailedDescription,
    contactedMerchant: s.contactedMerchant,
    merchantResponse: s.merchantResponseSummary
  };
}

function isDigitalService(merchant, description) {
  const digital = ['SaaS', 'Cloud', 'Streaming', 'Digital', 'Software', 'Platform', 'Subscription', 'FreeTrial', 'Payment Gateway'];
  return digital.some(k => merchant.includes(k)) || (description && /subscription|digital|software|license|saas|streaming|cloud|online service/i.test(description));
}

function isPhysicalGoods(description) {
  if (!description) return false;
  return /headphones|mug|sweater|jewelry|parts|item|product|purchase|order|goods|shipped|deliver/i.test(description);
}

function hasShipping(c, issuerFacts) {
  if (['4841', '4834', '4808'].includes(c.reason)) return false;
  if (isDigitalService(c.merchant, issuerFacts.description)) return false;
  if (['4855', '4853'].includes(c.reason) && isPhysicalGoods(issuerFacts.description)) return true;
  if (c.reason === '4837' || c.reason === '4863' || c.reason === '4871') return isPhysicalGoods(issuerFacts.description);
  return false;
}

function genEmail(name) {
  const parts = name.replace(/Ms\.\s*/g, '').replace(/\s+/g, ' ').trim().split(' ');
  const first = parts[0].toLowerCase().replace(/[^a-z]/g, '');
  const last = parts[parts.length - 1].toLowerCase().replace(/[^a-z]/g, '');
  return `${first}.${last}@email.com`;
}

function genPhone(caseNum) {
  return `+1${(2000000000 + caseNum * 111111).toString()}`;
}

function genAddress(caseNum) {
  const streets = ['Oak St', 'Maple Ave', 'Pine Rd', 'Cedar Ln', 'Elm Dr', 'Birch Way', 'Willow Ct', 'Ash Blvd', 'Spruce Pl', 'Cherry Ln', 'Walnut Dr', 'Hickory Rd', 'Juniper Ave', 'Sycamore St', 'Poplar Way', 'Laurel Ct', 'Alder Ln'];
  const cities = ['New York', 'Los Angeles', 'Chicago', 'Houston', 'Phoenix', 'Philadelphia', 'San Antonio', 'San Diego', 'Dallas', 'San Jose', 'Austin', 'Jacksonville', 'Columbus', 'Charlotte', 'Seattle', 'Denver', 'Portland'];
  const states = ['NY', 'CA', 'IL', 'TX', 'AZ', 'PA', 'TX', 'CA', 'TX', 'CA', 'TX', 'FL', 'OH', 'NC', 'WA', 'CO', 'OR'];
  const zips = ['10001', '90001', '60601', '77001', '85001', '19101', '78201', '92101', '75201', '95101', '73301', '32099', '43085', '28201', '98101', '80201', '97201'];
  const idx = (caseNum - 20) % 17;
  return { street: `${100 + caseNum * 7} ${streets[idx]}`, city: cities[idx], state: states[idx], zip: zips[idx], country: 'US' };
}

function txnDate(c, issuerFacts) {
  const desc = issuerFacts.description || '';
  const dateMatch = desc.match(/(?:on|dated|around)\s+(January|February|March|April|May|June|July|August|September|October|November|December)\s+(\d{1,2}),?\s+(\d{4})/i);
  if (dateMatch) {
    const months = { january: '01', february: '02', march: '03', april: '04', may: '05', june: '06', july: '07', august: '08', september: '09', october: '10', november: '11', december: '12' };
    const m = months[dateMatch[1].toLowerCase()];
    const d = dateMatch[2].padStart(2, '0');
    return `${dateMatch[3]}-${m}-${d}`;
  }
  return '2026-01-15';
}

function writeFile(filePath, data) {
  const dir = path.dirname(filePath);
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(filePath, JSON.stringify(data, null, 2) + '\n');
}

let filesWritten = 0;

for (const c of cases) {
  const issuer = readIssuerStatement(c.case);
  const facts = getIssuerFacts(issuer);
  const name = facts.cardholderName || 'Unknown Cardholder';
  const email = genEmail(name);
  const phone = genPhone(c.case);
  const addr = genAddress(c.case);
  const tDate = txnDate(c, facts);
  const tDateTime = tDate + 'T14:30:00Z';
  const orderId = `ORD-${c.case.toString().padStart(6, '0')}`;
  const txnRef = `TXN-${c.txnId}`;
  const cardLast4 = c.pan.slice(-4);
  const isDigital = isDigitalService(c.merchant, facts.description);
  const needsShipping = hasShipping(c, facts);

  let itemName, itemDesc, sku, mcc, mccDesc;
  const desc = (facts.description || '').toLowerCase();

  if (c.merchant === 'Digital Goods LLC') { itemName = 'Software License'; itemDesc = 'Single-use software license key'; sku = `SKU-DG-${c.case}`; mcc = '5818'; mccDesc = 'Digital Goods'; }
  else if (c.merchant === 'Enterprise Solutions') { itemName = 'Annual Analytics Subscription'; itemDesc = 'Advanced analytics suite - annual plan'; sku = `SKU-ES-${c.case}`; mcc = '5817'; mccDesc = 'Computer Software Stores'; }
  else if (c.merchant === 'FreeTrial Corp') { itemName = 'Free Trial Enrollment'; itemDesc = '7-day free trial with $0.00 authorization'; sku = `SKU-FT-${c.case}`; mcc = '5818'; mccDesc = 'Digital Goods'; }
  else if (c.merchant === 'Cloud Services Inc') { itemName = 'Cloud Storage Plan'; itemDesc = 'Monthly cloud storage subscription'; sku = `SKU-CS-${c.case}`; mcc = '5817'; mccDesc = 'Computer Software'; }
  else if (c.merchant === 'SaaS Platform') { itemName = 'SaaS Basic Plan'; itemDesc = 'Monthly SaaS subscription service'; sku = `SKU-SP-${c.case}`; mcc = '5817'; mccDesc = 'SaaS / Software'; }
  else if (c.merchant === 'Payment Gateway') { itemName = 'Payment Processing Service'; itemDesc = 'Online payment processing fee'; sku = `SKU-PG-${c.case}`; mcc = '5816'; mccDesc = 'Digital Services'; }
  else if (c.merchant === 'Luxury Goods Ltd') { itemName = 'Luxury Handbag'; itemDesc = 'Designer leather handbag'; sku = `SKU-LG-${c.case}`; mcc = '5944'; mccDesc = 'Jewelry/Watch/Silverware'; }
  else if (c.merchant === 'Electronics Hub') { itemName = 'Noise-Cancelling Headphones'; itemDesc = 'High-end wireless noise-cancelling headphones'; sku = `SKU-EH-${c.case}`; mcc = '5732'; mccDesc = 'Electronics Stores'; }
  else if (c.merchant === 'Online Marketplace') { itemName = 'Custom Hand-Painted Ceramic Mug'; itemDesc = 'Artisan handcrafted ceramic mug from ArtisanCrafts seller'; sku = `SKU-OM-${c.case}`; mcc = '5999'; mccDesc = 'Miscellaneous Retail'; }
  else if (c.merchant === 'Retail Store') { itemName = 'In-Store Purchase'; itemDesc = 'Chip/PIN in-store transaction'; sku = `SKU-RS-${c.case}`; mcc = '5411'; mccDesc = 'Grocery/Retail'; }
  else if (c.merchant === 'Travel Agency') { itemName = 'Hotel Reservation - Grand Vista Hotel Miami'; itemDesc = '5-night hotel stay, Feb 20-25 2026'; sku = `SKU-TA-${c.case}`; mcc = '4722'; mccDesc = 'Travel Agencies'; }
  else if (c.merchant === 'Subscription Box Co') { itemName = 'Premium Subscription Box'; itemDesc = 'Monthly premium subscription box delivery'; sku = `SKU-SB-${c.case}`; mcc = '5947'; mccDesc = 'Gift/Card/Novelty'; }
  else if (c.merchant === 'Food Delivery App') { itemName = 'Food Delivery Order - The Pasta Spot'; itemDesc = 'Signature Truffle Pasta meal delivery'; sku = `SKU-FD-${c.case}`; mcc = '5812'; mccDesc = 'Eating Places/Restaurants'; }
  else if (c.merchant === 'Auto Parts Store') { itemName = 'Auto Parts Bundle'; itemDesc = 'Multiple automotive parts and accessories'; sku = `SKU-AP-${c.case}`; mcc = '5533'; mccDesc = 'Automotive Parts'; }
  else if (c.merchant === 'Jewelry Online') { itemName = 'Fine Jewelry Piece'; itemDesc = 'High-value fine jewelry item'; sku = `SKU-JO-${c.case}`; mcc = '5944'; mccDesc = 'Jewelry Stores'; }
  else if (c.merchant === 'Streaming Service') { itemName = 'Annual Streaming Subscription'; itemDesc = 'Annual premium streaming plan'; sku = `SKU-SS-${c.case}`; mcc = '4899'; mccDesc = 'Cable/Streaming Services'; }
  else if (c.merchant === 'Fashion Outlet') { itemName = 'Deluxe Merino Wool Sweater'; itemDesc = 'Premium merino wool sweater, online order'; sku = `SKU-FO-${c.case}`; mcc = '5651'; mccDesc = 'Family Clothing'; }
  else { itemName = 'Product'; itemDesc = 'Purchase'; sku = `SKU-${c.case}`; mcc = '5999'; mccDesc = 'Retail'; }

  const is3dsApplicable = !['4871', '4808'].includes(c.reason);
  const isCardPresent = c.reason === '4871' || c.reason === '4808';
  const entryMode = isCardPresent ? 'Chip' : 'Ecommerce';

  const emailLogs = {
    source: `${c.merchant} — Customer Communications`,
    dataType: 'Email Communication Logs',
    caseReference: `CASE-${c.case}`,
    records: [{
      email: email,
      customerName: name,
      merchantEmail: `support@${c.merchant.toLowerCase().replace(/[^a-z]/g, '')}.com`,
      merchantName: c.merchant,
      transactionReference: txnRef,
      communications: []
    }]
  };

  if (facts.contactedMerchant) {
    const comms = emailLogs.records[0].communications;
    if (desc.match(/cancel/i) && ['4841'].includes(c.reason)) {
      const cancelDateMatch = desc.match(/cancel[a-z]*\s+(?:on\s+)?(?:my\s+)?(?:subscription\s+)?(?:on\s+)?(January|February|March|April|May|June|July|August|September|October|November|December)\s+(\d{1,2}),?\s+(\d{4})/i);
      let cancelDate = '2026-01-10';
      if (cancelDateMatch) {
        const months = { january: '01', february: '02', march: '03', april: '04', may: '05', june: '06', july: '07', august: '08', september: '09', october: '10', november: '11', december: '12' };
        cancelDate = `${cancelDateMatch[3]}-${months[cancelDateMatch[1].toLowerCase()]}-${cancelDateMatch[2].padStart(2, '0')}`;
      }
      comms.push({
        type: 'Cancellation Request', direction: 'customer_to_merchant',
        sentAt: cancelDate + 'T09:15:00Z', opened: true, openedAt: cancelDate + 'T09:20:00Z',
        subject: 'Cancellation Request',
        body: `Hi,\n\nI would like to cancel my subscription with ${c.merchant}. Please confirm cancellation and ensure I will not be charged again.\n\nThank you,\n${name}`
      });
      const nextDay = new Date(cancelDate); nextDay.setDate(nextDay.getDate() + 1);
      const respDate = nextDay.toISOString().split('T')[0];
      comms.push({
        type: 'Cancellation Confirmation', direction: 'merchant_to_customer',
        sentAt: respDate + 'T11:00:00Z', opened: true, openedAt: respDate + 'T11:30:00Z',
        subject: 'RE: Cancellation Request',
        body: `Hi ${name.split(' ')[0]},\n\nYour cancellation request has been received and processed. Your service will remain active until the end of your current billing period. You will not be charged after that date.\n\nCustomer Support\n${c.merchant}`
      });
    }
    comms.push({
      type: 'Customer Inquiry', direction: 'customer_to_merchant',
      sentAt: tDate + 'T16:00:00Z', opened: true, openedAt: tDate + 'T16:05:00Z',
      subject: `Charge inquiry - ${txnRef}`,
      body: `Hi,\n\nI noticed a charge of $${c.amount} on my statement from ${c.merchant}. I would like to understand this charge and request a resolution.\n\nRegards,\n${name}`
    });
    const respDate2 = new Date(tDate); respDate2.setDate(respDate2.getDate() + 1);
    comms.push({
      type: 'Merchant Response', direction: 'merchant_to_customer',
      sentAt: respDate2.toISOString().split('T')[0] + 'T10:30:00Z', opened: true, openedAt: respDate2.toISOString().split('T')[0] + 'T11:00:00Z',
      subject: `RE: Charge inquiry - ${txnRef}`,
      body: facts.merchantResponse || `Hi ${name.split(' ')[0]},\n\nThank you for reaching out. We are looking into your inquiry regarding the $${c.amount} charge. Our team will review and follow up.\n\nCustomer Support\n${c.merchant}`
    });
  }
  writeFile(path.join(ACQUIRER_ROOT, 'customer-comms', `email_logs_case_${c.case}.json`), emailLogs);
  filesWritten++;

  const avsCvv = {
    source: `${c.merchant} — Payment Processor`,
    dataType: 'AVS/CVV Verification Results',
    caseReference: `CASE-${c.case}`,
    records: [{
      transactionId: txnRef,
      cardLastFour: cardLast4,
      avsResult: 'Y', streetMatch: true, zipMatch: true,
      cvvMatch: true, cvvResult: 'M',
      cardholderNameMatch: true, cardholderName: name,
      emailVerified: true, email: email,
      phoneVerified: true,
      cvc2Result: 'M', cvc2Match: true,
      cardNetwork: 'Mastercard'
    }]
  };
  writeFile(path.join(ACQUIRER_ROOT, 'identity', `avs_cvv_check_case_${c.case}.json`), avsCvv);
  filesWritten++;

  const authLog = {
    source: `${c.merchant} — Payment Service Provider`,
    dataType: 'Authorization Log',
    caseReference: `CASE-${c.case}`,
    records: [{
      transactionId: txnRef,
      authorizationCode: `AUTH${(100000 + c.case * 7919).toString().slice(-6)}`,
      requestTimestamp: tDateTime,
      responseTimestamp: new Date(new Date(tDateTime).getTime() + 2000).toISOString(),
      amount: c.amount, currency: c.currency,
      cardLastFour: cardLast4,
      cardholderName: name,
      entryMode: entryMode,
      responseCode: '00', responseMessage: 'Approved',
      merchantId: c.merchantId,
      merchantName: c.merchant,
      terminalId: isCardPresent ? `TERM-${c.case}` : `ECOM-${c.case}`,
      acquirerBIN: c.acquirerId,
      issuerBIN: c.issuerId
    }]
  };
  writeFile(path.join(ACQUIRER_ROOT, 'psp', `auth_log_case_${c.case}.json`), authLog);
  filesWritten++;

  const settlement = {
    source: `${c.merchant} — Payment Service Provider`,
    dataType: 'Settlement Record',
    caseReference: `CASE-${c.case}`,
    records: [{
      transactionId: txnRef,
      settlementDate: new Date(new Date(tDate).getTime() + 86400000 * 2).toISOString().split('T')[0],
      settlementAmount: c.amount, settlementCurrency: c.currency,
      merchantId: c.merchantId,
      merchantName: c.merchant,
      batchId: `BATCH-${c.case}-001`,
      status: 'Settled',
      cardholderName: name,
      cardLastFour: cardLast4
    }]
  };
  writeFile(path.join(ACQUIRER_ROOT, 'psp', `settlement_record_case_${c.case}.json`), settlement);
  filesWritten++;

  if (is3dsApplicable && !isCardPresent) {
    const threeds = {
      source: `${c.merchant} — 3DS Authentication Provider`,
      dataType: '3D Secure Authentication Record',
      caseReference: `CASE-${c.case}`,
      records: [{
        transactionId: txnRef,
        threeDSVersion: '2.2.0',
        authenticationStatus: c.type === 'Fraud' ? 'Attempted' : 'Authenticated',
        eci: c.type === 'Fraud' ? '06' : '05',
        cavv: `CAVV${c.case}${c.txnId.slice(0, 8)}`,
        dsTransactionId: `DS-${c.txnId}`,
        challengeRequired: false,
        challengeCompleted: false,
        cardholderName: name,
        merchantName: c.merchant,
        amount: c.amount, currency: c.currency,
        timestamp: tDateTime
      }]
    };
    writeFile(path.join(ACQUIRER_ROOT, 'device', `3ds_authentication_case_${c.case}.json`), threeds);
    filesWritten++;
  }

  const ipAddr = `${72 + (c.case % 50)}.${100 + (c.case * 3) % 155}.${(c.case * 7) % 255}.${(c.case * 13) % 255}`;
  const deviceFp = {
    source: `${c.merchant} — Fraud Detection Service`,
    dataType: 'Device Fingerprint Analysis',
    caseReference: `CASE-${c.case}`,
    records: [{
      transactionId: txnRef,
      sessionId: `SES-${c.case}-${c.txnId.slice(0, 6)}`,
      ipAddress: isCardPresent ? null : ipAddr,
      deviceType: isCardPresent ? 'POS Terminal' : 'Desktop',
      browser: isCardPresent ? null : 'Chrome 121.0',
      os: isCardPresent ? null : 'Windows 11',
      screenResolution: isCardPresent ? null : '1920x1080',
      vpnDetected: false, proxyDetected: false, torDetected: false,
      riskScore: c.type === 'Fraud' ? 25 : 10,
      riskLevel: 'Low',
      knownDevice: true,
      previousTransactions: 3 + (c.case % 10),
      geolocation: isCardPresent ? null : { country: 'US', region: addr.state, city: addr.city },
      cardholderName: name
    }]
  };
  writeFile(path.join(ACQUIRER_ROOT, 'fraud-tools', `device_fingerprint_case_${c.case}.json`), deviceFp);
  filesWritten++;

  const ipRisk = {
    source: `${c.merchant} — IP Intelligence Service`,
    dataType: 'IP Risk Assessment',
    caseReference: `CASE-${c.case}`,
    records: [{
      transactionId: txnRef,
      ipAddress: isCardPresent ? null : ipAddr,
      riskScore: c.type === 'Fraud' ? 20 : 5,
      riskLevel: 'Low',
      country: 'US', region: addr.state, city: addr.city,
      isp: 'Major US ISP',
      isVPN: false, isProxy: false, isTor: false, isHosting: false,
      threatCategory: 'None',
      previousActivity: 'Normal consumer behavior',
      cardholderName: name
    }]
  };
  writeFile(path.join(ACQUIRER_ROOT, 'fraud-tools', `ip_risk_report_case_${c.case}.json`), ipRisk);
  filesWritten++;

  const orderDetails = {
    source: `${c.merchant} — Order Management System`,
    dataType: 'Order Details',
    caseReference: `CASE-${c.case}`,
    records: [{
      orderId: orderId,
      transactionId: txnRef,
      orderDate: tDateTime,
      orderStatus: needsShipping ? 'Fulfilled' : (isDigital ? 'Completed' : 'Completed'),
      amount: c.amount, currency: c.currency,
      items: [{ name: itemName, quantity: 1, unitPrice: c.amount, sku: sku }],
      billingDescriptor: `${c.merchant.toUpperCase().replace(/[^A-Z ]/g, '').slice(0, 20)}*${orderId}`,
      billingAddress: { name: name, ...addr },
      shippingAddress: needsShipping ? { name: name, ...addr } : null,
      ipAddress: isCardPresent ? null : ipAddr,
      customerEmail: email,
      customerPhone: phone
    }]
  };
  writeFile(path.join(ACQUIRER_ROOT, 'merchant', `order_details_case_${c.case}.json`), orderDetails);
  filesWritten++;

  if (needsShipping) {
    const shipDate = new Date(tDate); shipDate.setDate(shipDate.getDate() + 1);
    const delivDate = new Date(tDate); delivDate.setDate(delivDate.getDate() + 5);

    const tracking = {
      source: `${c.merchant} — Shipping Provider`,
      dataType: 'Shipping Tracking Data',
      caseReference: `CASE-${c.case}`,
      records: [{
        orderId: orderId,
        transactionId: txnRef,
        carrier: 'UPS',
        trackingNumber: `1Z${(999000000 + c.case * 12345).toString()}`,
        shippedDate: shipDate.toISOString().split('T')[0] + 'T10:00:00Z',
        estimatedDelivery: delivDate.toISOString().split('T')[0],
        status: 'Delivered',
        weight: '0.5 kg',
        dimensions: '25x15x10 cm',
        shippingAddress: { name: name, ...addr },
        customerName: name
      }]
    };
    writeFile(path.join(ACQUIRER_ROOT, 'shipping', `tracking_data_case_${c.case}.json`), tracking);
    filesWritten++;

    const delivery = {
      source: `${c.merchant} — Delivery Service`,
      dataType: 'Delivery Confirmation',
      caseReference: `CASE-${c.case}`,
      records: [{
        orderId: orderId,
        transactionId: txnRef,
        deliveryDate: delivDate.toISOString().split('T')[0] + 'T14:22:00Z',
        deliveryStatus: 'Delivered',
        recipientName: name,
        signatureOnFile: true,
        deliveryAddress: { name: name, ...addr },
        photoProof: true,
        deliveryNotes: 'Left with resident, signature obtained',
        carrier: 'UPS'
      }]
    };
    writeFile(path.join(ACQUIRER_ROOT, 'shipping', `delivery_confirmation_case_${c.case}.json`), delivery);
    filesWritten++;
  } else {
    const trackingPath = path.join(ACQUIRER_ROOT, 'shipping', `tracking_data_case_${c.case}.json`);
    const deliveryPath = path.join(ACQUIRER_ROOT, 'shipping', `delivery_confirmation_case_${c.case}.json`);
    if (fs.existsSync(trackingPath)) fs.unlinkSync(trackingPath);
    if (fs.existsSync(deliveryPath)) fs.unlinkSync(deliveryPath);

    if (isDigital) {
      const serviceProof = {
        source: `${c.merchant} — Service Platform`,
        dataType: 'Service Delivery Proof',
        caseReference: `CASE-${c.case}`,
        records: [{
          orderId: orderId,
          transactionId: txnRef,
          serviceType: itemDesc,
          activationDate: tDateTime,
          serviceStatus: 'Active',
          lastAccessDate: new Date(new Date(tDate).getTime() + 86400000 * 3).toISOString(),
          accessLogs: [
            { date: tDateTime, action: 'Service activated', ip: ipAddr },
            { date: new Date(new Date(tDate).getTime() + 86400000).toISOString(), action: 'User login', ip: ipAddr }
          ],
          customerName: name,
          customerEmail: email
        }]
      };
      writeFile(path.join(ACQUIRER_ROOT, 'merchant', `service_delivery_proof_case_${c.case}.json`), serviceProof);
      filesWritten++;
    }
  }

  const fulfillment = {
    source: `${c.merchant} — Fulfillment System`,
    dataType: 'Fulfillment Record',
    caseReference: `CASE-${c.case}`,
    records: [{
      fulfillmentId: `FUL-${c.case.toString().padStart(6, '0')}`,
      orderId: orderId,
      fulfillmentType: needsShipping ? 'Physical Shipment' : (isDigital ? 'Digital Delivery' : 'In-Store'),
      fulfillmentStatus: 'Completed',
      fulfilledDate: needsShipping ?
        new Date(new Date(tDate).getTime() + 86400000).toISOString() :
        tDateTime,
      carrier: needsShipping ? 'UPS' : null,
      trackingNumber: needsShipping ? `1Z${(999000000 + c.case * 12345).toString()}` : null,
      itemsFulfilled: 1,
      customerName: name,
      customerEmail: email
    }]
  };
  writeFile(path.join(ACQUIRER_ROOT, 'merchant', `fulfillment_record_case_${c.case}.json`), fulfillment);
  filesWritten++;

  if (isCardPresent) {
    const posLog = {
      source: `${c.merchant} — Point of Sale System`,
      dataType: 'POS Terminal Log',
      caseReference: `CASE-${c.case}`,
      records: [{
        transactionId: txnRef,
        terminalId: `TERM-${c.case}`,
        storeLocation: `${c.merchant} - ${addr.city}, ${addr.state}`,
        timestamp: tDateTime,
        entryMode: 'Chip/EMV',
        pinVerified: true,
        amount: c.amount, currency: c.currency,
        cardLastFour: cardLast4,
        cardholderName: name,
        approvalCode: `AUTH${(100000 + c.case * 7919).toString().slice(-6)}`,
        receiptGenerated: true
      }]
    };
    writeFile(path.join(ACQUIRER_ROOT, 'merchant', `pos_terminal_log_case_${c.case}.json`), posLog);
    filesWritten++;
  } else {
    const posPath = path.join(ACQUIRER_ROOT, 'merchant', `pos_terminal_log_case_${c.case}.json`);
    if (fs.existsSync(posPath)) fs.unlinkSync(posPath);
  }

  const refundPolicy = {
    source: `${c.merchant} — Policy Department`,
    dataType: 'Refund/Cancellation Policy',
    caseReference: `CASE-${c.case}`,
    records: [{
      merchantName: c.merchant,
      policyVersion: '2025-12-01',
      refundWindow: '30 days from purchase',
      cancellationPolicy: isDigital ?
        'Subscriptions can be cancelled at any time. Cancellation takes effect at the end of the current billing period. No prorated refunds for partial months.' :
        'Items may be returned within 30 days of delivery for a full refund. Items must be in original condition.',
      chargebackPolicy: 'All chargebacks are reviewed and responded to with supporting documentation within the required timeframe.',
      termsAcceptedByCustomer: true,
      termsAcceptedDate: tDateTime,
      customerName: name,
      customerEmail: email
    }]
  };
  writeFile(path.join(ACQUIRER_ROOT, 'merchant', `refund_policy_case_${c.case}.json`), refundPolicy);
  filesWritten++;

  if (['4841'].includes(c.reason)) {
    const cancelPolicy = {
      source: `${c.merchant} — Subscription Management`,
      dataType: 'Cancellation Policy & Records',
      caseReference: `CASE-${c.case}`,
      records: [{
        merchantName: c.merchant,
        subscriptionId: `SUB-${c.case}-${c.txnId.slice(0, 4)}`,
        customerName: name,
        customerEmail: email,
        planName: itemName,
        monthlyAmount: c.amount,
        currency: c.currency,
        cancellationPolicy: 'Cancellations take effect at end of current billing period. Must be submitted before renewal date.',
        cancellationReceived: true,
        cancellationDate: (() => {
          const match = (facts.description || '').match(/cancel[a-z]*\s+(?:on\s+)?(?:my\s+)?(?:subscription\s+)?(?:on\s+)?(January|February|March|April|May|June|July|August|September|October|November|December)\s+(\d{1,2}),?\s+(\d{4})/i);
          if (match) {
            const months = { january: '01', february: '02', march: '03', april: '04', may: '05', june: '06', july: '07', august: '08', september: '09', october: '10', november: '11', december: '12' };
            return `${match[3]}-${months[match[1].toLowerCase()]}-${match[2].padStart(2, '0')}`;
          }
          return '2026-01-10';
        })(),
        billingCycleEnd: (() => {
          const match = (facts.description || '').match(/(?:after|until|ending|ended)\s+(?:on\s+)?(January|February|March|April|May|June|July|August|September|October|November|December)\s+(\d{1,2}),?\s+(\d{4})/i);
          if (match) {
            const months = { january: '01', february: '02', march: '03', april: '04', may: '05', june: '06', july: '07', august: '08', september: '09', october: '10', november: '11', december: '12' };
            return `${match[3]}-${months[match[1].toLowerCase()]}-${match[2].padStart(2, '0')}`;
          }
          return '2026-01-31';
        })(),
        chargeAfterCancellation: true,
        merchantNotes: 'System processed recurring charge after cancellation was received. Reviewing billing system for processing delay.'
      }]
    };
    writeFile(path.join(ACQUIRER_ROOT, 'merchant', `cancellation_policy_case_${c.case}.json`), cancelPolicy);
    filesWritten++;
  } else {
    const cancelPath = path.join(ACQUIRER_ROOT, 'merchant', `cancellation_policy_case_${c.case}.json`);
    if (fs.existsSync(cancelPath)) fs.unlinkSync(cancelPath);
  }
}

console.log(`Done. ${filesWritten} acquirer evidence files regenerated for ${cases.length} cases.`);
