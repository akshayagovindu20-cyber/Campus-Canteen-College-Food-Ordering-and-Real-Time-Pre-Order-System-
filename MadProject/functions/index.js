/**
 * Campus Canteen - Firebase Cloud Functions
 * Automated FCM Push Notifications Triggered on Order Status Updates
 */

const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.onOrderStatusChanged = onDocumentWritten("orders/{orderId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) return;

  const beforeData = snapshot.before ? snapshot.before.data() : null;
  const afterData = snapshot.after ? snapshot.after.data() : null;

  if (!afterData) return; // Order deleted

  const orderId = event.params.orderId;
  const newStatus = afterData.status ? afterData.status.toUpperCase() : "PLACED";
  const oldStatus = beforeData && beforeData.status ? beforeData.status.toUpperCase() : null;

  // Don't resend if status did not change
  if (oldStatus === newStatus) return;

  const shortOrderId = orderId.substring(0, Math.min(8, orderId.length)).toUpperCase();
  const customerId = afterData.customerId;

  if (!customerId) return;

  // Fetch customer FCM token from users collection
  const userDoc = await admin.firestore().collection("users").doc(customerId).get();
  if (!userDoc.exists) return;

  const userData = userDoc.data();
  const fcmToken = userData ? userData.fcmToken : null;

  let title = "";
  let body = "";

  if (newStatus === "READY") {
    title = "Your Order Is Ready!";
    body = `Your order #${shortOrderId} is ready for collection. Please collect it from Counter 1 at the college canteen.`;
  } else if (newStatus === "PREPARING") {
    title = "Order Accepted";
    body = `Your order #${shortOrderId} is being prepared by the canteen staff.`;
  } else if (newStatus === "COMPLETED") {
    title = "Order Completed";
    body = `Thank you for collecting order #${shortOrderId}. Enjoy your meal!`;
  } else if (newStatus === "CANCELLED") {
    title = "Order Cancelled";
    body = `Your order #${shortOrderId} was cancelled. ${afterData.cancelReason ? "Reason: " + afterData.cancelReason : ""}`;
  } else if (newStatus === "PLACED" && !oldStatus) {
    // Notify Server of new order
    await notifyServerOfNewOrder(orderId, afterData);
    return;
  }

  if (fcmToken && title && body) {
    const messagePayload = {
      token: fcmToken,
      notification: {
        title: title,
        body: body,
      },
      data: {
        orderId: orderId,
        status: newStatus,
        title: title,
        message: body,
      },
      android: {
        priority: "high",
        notification: {
          channelId: "canteen_order_channel",
          sound: "default",
          priority: "high",
        },
      },
    };

    try {
      await admin.messaging().send(messagePayload);
      console.log(`Successfully sent FCM notification for order ${orderId} status ${newStatus}`);
    } catch (err) {
      console.error("Error sending FCM message:", err);
    }
  }
});

async function notifyServerOfNewOrder(orderId, orderData) {
  const serverQuery = await admin.firestore().collection("users").where("role", "==", "SERVER").get();
  if (serverQuery.empty) return;

  const shortId = orderId.substring(0, Math.min(8, orderId.length)).toUpperCase();

  for (const doc of serverQuery.docs) {
    const serverToken = doc.data().fcmToken;
    if (serverToken) {
      const payload = {
        token: serverToken,
        notification: {
          title: "New Canteen Order Placed!",
          body: `New order #${shortId} from ${orderData.customerName || "Customer"}. Total: ₹${orderData.totalAmount}`,
        },
        data: {
          orderId: orderId,
          status: "PLACED",
        },
        android: {
          priority: "high",
          notification: {
            channelId: "canteen_order_channel",
            sound: "default",
          },
        },
      };
      try {
        await admin.messaging().send(payload);
      } catch (e) {
        console.error("Error notifying server:", e);
      }
    }
  }
}
