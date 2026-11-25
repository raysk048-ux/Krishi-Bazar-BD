const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_owner_uid";
const ADMIN_EMAIL = "raysk048@gmail.com";
const SELLER_UID = "seller_sobuj_uid";
const BUYER_UID = "buyer_rakib_uid";
const OTHER_UID = "other_user_uid";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

function spoofedAdminContext() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  });
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  await assertFails(db.collection("products").where("approvalStatus", "==", "APPROVED").get());
  await assertFails(db.collection("users").doc(BUYER_UID).get());
  await assertFails(db.collection("orders").where("buyerUserId", "==", BUYER_UID).get());
});

test("User profile isolation, self-role escalation guard, and admin email verification", async () => {
  const past = new Date(Date.now() - 2000);
  const buyerDb = testEnv.authenticatedContext(BUYER_UID, { email: "rakib@example.com", email_verified: true }).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID, { email: "other@example.com", email_verified: true }).firestore();

  // Buyer cannot self-assign MAIN_ADMIN
  await assertFails(
    buyerDb.collection("users").doc(BUYER_UID).set({
      userId: BUYER_UID,
      fullName: "Rakib Hasan",
      phone: "01755667788",
      email: "rakib@example.com",
      role: "MAIN_ADMIN",
      blocked: false,
      defaultAddress: "Dhaka",
      district: "Dhaka",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Valid Buyer profile create succeeds
  await assertSucceeds(
    buyerDb.collection("users").doc(BUYER_UID).set({
      userId: BUYER_UID,
      fullName: "Rakib Hasan",
      phone: "01755667788",
      email: "rakib@example.com",
      role: "BUYER",
      blocked: false,
      defaultAddress: "Dhanmondi, Dhaka",
      district: "Dhaka",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Cross-user PII read/list denied
  await assertFails(otherDb.collection("users").doc(BUYER_UID).get());
  await assertFails(otherDb.collection("users").get());

  // Spoofed unverified email claiming admin email cannot list users
  const spoofDb = spoofedAdminContext().firestore();
  await assertFails(spoofDb.collection("users").get());

  // Verified Owner Admin can list users & block user
  const adminDb = adminContext().firestore();
  await assertSucceeds(adminDb.collection("users").get());
  await assertSucceeds(
    adminDb.collection("users").doc(BUYER_UID).update({
      blocked: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Shop & product workflow: seller creates pending, admin approves, buyer queries & decrements stock", async () => {
  const past = new Date(Date.now() - 5000);
  const sellerDb = testEnv.authenticatedContext(SELLER_UID, { email: "seller@example.com", email_verified: true }).firestore();
  const buyerDb = testEnv.authenticatedContext(BUYER_UID, { email: "buyer@example.com", email_verified: true }).firestore();
  const adminDb = adminContext().firestore();

  await assertSucceeds(
    sellerDb.collection("users").doc(SELLER_UID).set({
      userId: SELLER_UID,
      fullName: "Karim",
      phone: "01711223344",
      email: "seller@example.com",
      role: "SELLER",
      blocked: false,
      defaultAddress: "Bogura",
      district: "Bogura",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Seller cannot self-approve shop
  await assertFails(
    sellerDb.collection("shops").doc("shop_1").set({
      ownerUserId: SELLER_UID,
      shopNameBn: "সবুজ খামার",
      shopNameEn: "Sobuj Farm",
      district: "Bogura",
      description: "Fresh vegetables",
      imageUri: "drawable://img_cat_vegetables",
      approvalStatus: "APPROVED",
      sellerCommissionAccepted: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Seller creates pending shop
  await assertSucceeds(
    sellerDb.collection("shops").doc("shop_1").set({
      ownerUserId: SELLER_UID,
      shopNameBn: "সবুজ খামার",
      shopNameEn: "Sobuj Farm",
      district: "Bogura",
      description: "Fresh vegetables",
      imageUri: "drawable://img_cat_vegetables",
      approvalStatus: "PENDING",
      sellerCommissionAccepted: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Admin approves shop
  await assertSucceeds(
    adminDb.collection("shops").doc("shop_1").update({
      approvalStatus: "APPROVED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Seller creates pending product
  await assertSucceeds(
    sellerDb.collection("products").doc("prod_1").set({
      shopId: "shop_1",
      sellerUserId: SELLER_UID,
      nameBn: "তাজা পালং শাক",
      nameEn: "Fresh Spinach",
      category: "VEGETABLES",
      priceBdt: 90.0,
      unitLabel: "2 kg",
      stockQuantity: 20,
      description: "Organic spinach",
      imageUri: "drawable://img_cat_vegetables",
      sellerCommissionAccepted: true,
      approvalStatus: "PENDING",
      featured: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Buyer unfiltered query fails, filtered approved query succeeds after admin approval
  await assertFails(buyerDb.collection("products").get());
  await assertSucceeds(
    adminDb.collection("products").doc("prod_1").update({
      approvalStatus: "APPROVED",
      featured: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    buyerDb.collection("products")
      .where("approvalStatus", "==", "APPROVED")
      .where("sellerCommissionAccepted", "==", true)
      .get()
  );

  // Buyer can decrement stock on approved product, cannot tamper price
  await assertSucceeds(
    buyerDb.collection("products").doc("prod_1").update({
      stockQuantity: 18,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    buyerDb.collection("products").doc("prod_1").update({
      priceBdt: 1.0,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cart, order lifecycle, terminal state locking, and admin commission withdrawal", async () => {
  const past = new Date(Date.now() - 5000);
  const sellerDb = testEnv.authenticatedContext(SELLER_UID, { email: "seller@example.com", email_verified: true }).firestore();
  const buyerDb = testEnv.authenticatedContext(BUYER_UID, { email: "buyer@example.com", email_verified: true }).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID, { email: "other@example.com", email_verified: true }).firestore();
  const adminDb = adminContext().firestore();

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("shops").doc("shop_1").set({
      ownerUserId: SELLER_UID,
      shopNameBn: "সবুজ খামার",
      shopNameEn: "Sobuj Farm",
      district: "Bogura",
      description: "Fresh",
      imageUri: "drawable://img_cat_vegetables",
      approvalStatus: "APPROVED",
      sellerCommissionAccepted: true,
      createdAt: past,
      updatedAt: past,
    });
    await db.collection("products").doc("prod_1").set({
      shopId: "shop_1",
      sellerUserId: SELLER_UID,
      nameBn: "চাল",
      nameEn: "Rice",
      category: "RICE",
      priceBdt: 380.0,
      unitLabel: "5 kg",
      stockQuantity: 10,
      description: "Miniket",
      imageUri: "drawable://img_cat_rice_grains",
      sellerCommissionAccepted: true,
      approvalStatus: "APPROVED",
      featured: true,
      createdAt: past,
      updatedAt: past,
    });
  });

  // Cart subcollection scoped to buyer
  await assertSucceeds(
    buyerDb.collection("users").doc(BUYER_UID).collection("cart").doc("prod_1").set({
      buyerUserId: BUYER_UID,
      productId: "prod_1",
      quantity: 2,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(buyerDb.collection("users").doc(BUYER_UID).collection("cart").get());
  await assertFails(otherDb.collection("users").doc(BUYER_UID).collection("cart").get());

  // Buyer creates order
  await assertSucceeds(
    buyerDb.collection("orders").doc("KB-2001").set({
      buyerUserId: BUYER_UID,
      buyerName: "Rakib",
      buyerPhone: "01755667788",
      deliveryAddress: "Dhaka",
      district: "Dhaka",
      shopId: "shop_1",
      sellerUserId: SELLER_UID,
      deliveryMethod: "SELLER_DELIVERY",
      deliveryCharge: 60.0,
      paymentMethod: "COD",
      paymentStatus: "PENDING_VERIFICATION",
      transactionId: "",
      senderAccount: "",
      orderStatus: "PLACED",
      grossSubtotal: 760.0,
      totalPayable: 820.0,
      agreedCommissionTotal: 380.0,
      realizedCommissionAmount: 0.0,
      sellerNetPayout: 0.0,
      commissionStatus: "PENDING",
      trackingNote: "Order placed",
      items: [
        {
          productId: "prod_1",
          productNameBn: "চাল",
          productNameEn: "Rice",
          unitPrice: 380.0,
          quantity: 2,
          appliedCommissionPercent: 50.0,
          lineSubtotal: 760.0,
          lineCommissionAmount: 380.0,
        },
      ],
      createdAt: past,
      updatedAt: past,
    })
  );

  // Uninvolved user cannot read order PII
  await assertFails(otherDb.collection("orders").doc("KB-2001").get());

  // Seller advances to DELIVERED
  await assertSucceeds(
    sellerDb.collection("orders").doc("KB-2001").update({
      orderStatus: "DELIVERED",
      trackingNote: "Delivered to customer",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Buyer confirms receipt -> COMPLETED + EARNED commission
  await assertSucceeds(
    buyerDb.collection("orders").doc("KB-2001").update({
      orderStatus: "COMPLETED",
      commissionStatus: "EARNED",
      realizedCommissionAmount: 380.0,
      sellerNetPayout: 380.0,
      paymentStatus: "VERIFIED",
      trackingNote: "Confirmed by buyer",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Terminal state lock: buyer/seller cannot modify COMPLETED order
  await assertFails(
    buyerDb.collection("orders").doc("KB-2001").update({
      orderStatus: "CANCELLED",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Owner Admin creates commission withdrawal
  await assertSucceeds(
    adminDb.collection("withdrawals").doc("ADM-WD-1").set({
      sellerUserId: ADMIN_UID,
      shopId: "PLATFORM_ADMIN",
      shopName: "Krishi Bazar BD Admin Commission",
      amountBdt: 300.0,
      payoutMethod: "BKASH",
      payoutAccount: "01700000001",
      status: "APPROVED",
      adminNote: "Owner withdrawal",
      requestedAt: new Date(Date.now() - 500),
      resolvedAt: new Date(Date.now() - 500),
    })
  );

  // Non-admin cannot create PLATFORM_ADMIN withdrawal
  await assertFails(
    sellerDb.collection("withdrawals").doc("HACK-WD").set({
      sellerUserId: SELLER_UID,
      shopId: "PLATFORM_ADMIN",
      shopName: "Hack",
      amountBdt: 300.0,
      payoutMethod: "BKASH",
      payoutAccount: "01711223344",
      status: "APPROVED",
      adminNote: "Hack",
      requestedAt: new Date(Date.now() - 500),
      resolvedAt: new Date(Date.now() - 500),
    })
  );
});
