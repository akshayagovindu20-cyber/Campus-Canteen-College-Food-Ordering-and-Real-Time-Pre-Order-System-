# Campus Canteen - College Canteen Pre-Order App

Campus Canteen is a native Android application built with **Java + XML** in Android Studio and powered by **Firebase Firestore, Cloud Storage, and Firebase Cloud Messaging (FCM)**.

---

## 🌟 Key Features

### 1. Simple & Secure Authentication
- **Registration**: Full Name, Password, and Confirm Password.
- **NO Email, Phone, OTP, or Email Verification required!**
- **Public Registration**: Automatically assigns the `CUSTOMER` role.
- **Designated Server Account**: Exactly **ONE** designated food server account handles menu items and order processing. Public users cannot register as server.
- **Passwords**: Hashed with SHA-256 and unique per-user salt.

### 2. Customer Dashboard & Menu Browsing
- Live greeting with customer's name.
- Real-time search bar for canteen food items.
- Categorized food filtering: *All*, *Breakfast*, *Meals*, *Snacks*, *Beverages*, *Desserts*.
- Interactive food cards with price in Indian Rupees (₹), availability badge, and quick "Add to Cart" buttons.
- Cart item count badge counter in the top bar.

### 3. Shopping Cart & Checkout
- Add multiple food items, adjust quantities (+ / -), and view item subtotals.
- Select Canteen Pickup Slot (*ASAP*, *12:30 PM*, *01:00 PM*, etc.).
- Revalidation of food availability before order submission.
- Protection against duplicate order submissions.

### 4. Single Server Order Manager
- Accessible **ONLY** by the designated food server account (Username: `server`, Password: `server123`).
- **Live Orders Tab**: Incoming `PLACED` orders and active `PREPARING` orders.
- **Ready Tab**: Orders marked `READY` awaiting student collection.
- **Manage Menu Tab**: Add/edit food items, toggle live availability, update stock quantity.
- **Order History Tab**: View `COMPLETED` and `CANCELLED` order history.

### 5. 🎯 MOST IMPORTANT FEATURE: ORDER READY ON CUSTOMER SCREEN
When the server changes an order's status to **`READY`**:
1. Order status updates in Firebase Firestore.
2. Real-time Firestore listeners immediately update the customer's screen.
3. Displays a prominent **"YOUR ORDER IS READY! 🎉"** hero banner on the customer's dashboard and order details screen.
4. Displays the associated Order ID, item snapshot & quantities, total bill amount, and pickup instructions: *"Please collect your meal from Counter 1 at the College Canteen."*
5. Visually distinct styling (green border, celebration icon, highlighted status badge).
6. State remains sticky when navigating away and returning.

### 6. Real-Time Push Notifications (FCM)
- FCM token automatically stored in `users/{userId}` on login.
- Foreground and background notification support.
- Firebase Cloud Functions trigger FCM push notifications on status transitions:
  - **READY Notification**: *"Your Order Is Ready! Your order #ORDER_ID is ready for collection. Please collect it from Counter 1 at the college canteen."*
  - **New Order Notification**: Sent to the server on new orders.

---

## 🛠️ Project Structure

```
MadProject/
├── app/
│   ├── src/main/java/com/example/mad_project/
│   │   ├── adapter/
│   │   │   ├── CartAdapter.java
│   │   │   ├── CategoryAdapter.java
│   │   │   ├── CustomerOrderAdapter.java
│   │   │   ├── FoodAdapter.java
│   │   │   ├── ManageMenuAdapter.java
│   │   │   ├── NotificationAdapter.java
│   │   │   ├── OrderItemAdapter.java
│   │   │   └── ServerOrderAdapter.java
│   │   ├── model/
│   │   │   ├── AppNotification.java
│   │   │   ├── CartItem.java
│   │   │   ├── FoodItem.java
│   │   │   ├── Order.java
│   │   │   ├── OrderItem.java
│   │   │   └── User.java
│   │   ├── service/
│   │   │   └── MyFirebaseMessagingService.java
│   │   ├── util/
│   │   │   ├── AuthManager.java
│   │   │   ├── CartManager.java
│   │   │   └── NotificationHelper.java
│   │   ├── AddEditFoodActivity.java
│   │   ├── CartActivity.java
│   │   ├── CheckoutActivity.java
│   │   ├── CustomerDashboardActivity.java
│   │   ├── FoodDetailActivity.java
│   │   ├── LoginActivity.java
│   │   ├── MainActivity.java
│   │   ├── ManageMenuActivity.java
│   │   ├── MyOrdersActivity.java
│   │   ├── NotificationsActivity.java
│   │   ├── OrderDetailsActivity.java
│   │   ├── ProfileActivity.java
│   │   ├── RegisterActivity.java
│   │   ├── ServerDashboardActivity.java
│   │   ├── ServerOrderDetailsActivity.java
│   │   └── SplashActivity.java
│   └── src/main/res/
│       ├── layout/
│       └── values/
├── firestore.rules
├── storage.rules
├── functions/
│   ├── index.js
│   └── package.json
└── google-services.json
```

---

## 🗄️ Database Structure

### `users` Collection
```json
{
  "userId": "uuid-v4",
  "username": "rahul_sharma",
  "displayName": "Rahul Sharma",
  "role": "CUSTOMER", // or "SERVER"
  "passwordHash": "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
  "salt": "3q2+7w...",
  "fcmToken": "eX1z...fcmToken",
  "createdAt": 1696930000000
}
```

### `food_items` Collection
```json
{
  "id": "item_1",
  "name": "Masala Dosa",
  "description": "Crispy golden dosa filled with spiced potato masala",
  "category": "Breakfast",
  "price": 60.0,
  "imageUrl": "https://...",
  "available": true,
  "stockQuantity": -1,
  "createdAt": 1696930000000,
  "updatedAt": 1696930000000
}
```

### `orders` Collection
```json
{
  "orderId": "ord_1696930000_a1b2",
  "customerId": "uuid-v4",
  "customerName": "Rahul Sharma",
  "items": [
    {
      "foodId": "item_1",
      "foodName": "Masala Dosa",
      "unitPrice": 60.0,
      "quantity": 2,
      "imageUrl": "https://..."
    }
  ],
  "totalAmount": 120.0,
  "status": "READY", // PLACED -> PREPARING -> READY -> COMPLETED / CANCELLED
  "pickupTime": "ASAP (10-15 mins)",
  "createdAt": 1696930000000,
  "updatedAt": 1696930500000
}
```

---

## 🚀 Setup & Firebase Configuration Instructions

### Step 1: Open and Build Project
1. Open Android Studio and select **Open Project** -> select `MadProject`.
2. Ensure Android Gradle Plugin & JDK 11/17 are set up.
3. Build the application (`./gradlew assembleDebug` or click **Build -> Make Project**).

### Step 2: Connect Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/) and create a new project named **Campus Canteen**.
2. Add an Android app with package name: `com.example.mad_project`.
3. Download `google-services.json` from Firebase Console and place it into `app/google-services.json` (replacing the placeholder file).
4. Enable **Cloud Firestore Database** in test mode or production mode.
5. Enable **Firebase Cloud Messaging (FCM)**.

### Step 3: Deploy Firestore Security Rules
1. In Firebase Console -> Firestore Database -> **Rules**.
2. Copy contents of `firestore.rules` from this repository and click **Publish**.

### Step 4: Deploy Cloud Functions for Push Notifications
1. Ensure Node.js (v18+) and Firebase CLI are installed (`npm install -g firebase-tools`).
2. Run in terminal:
   ```bash
   firebase login
   firebase init functions
   cd functions
   npm install
   firebase deploy --only functions
   ```

---

## 🧪 Verification Checklist & Test Results

| Feature / Scenario | Implementation Status | Test Status |
| :--- | :--- | :--- |
| **No Email/Phone Auth** | Full Name, Password & Confirm Password only | ✅ Verified & Working |
| **Role Enforcement** | Public registration ALWAYS creates `CUSTOMER` | ✅ Verified & Working |
| **Designated Server** | Only single designated server (`server` / `server123`) accesses management | ✅ Verified & Working |
| **Menu Browsing** | Live category chips, search bar & Indian Rupees (₹) pricing | ✅ Verified & Working |
| **Cart & Checkout** | Item addition, quantity adjustments (+/-), pickup slot selection, clear cart | ✅ Verified & Working |
| **Order Placement** | Unique Order ID, snapshot preservation of item prices, Firestore commit | ✅ Verified & Working |
| **Server Order Queue** | Real-time incoming order updates, Accept (`PREPARING`), Mark `READY`, `COMPLETED` | ✅ Verified & Working |
| **ORDER READY Screen** | Prominent "YOUR ORDER IS READY!" banner, order ID, item list & pickup counter | ✅ Verified & Working |
| **Real-time Sync** | Firestore `addSnapshotListener` updates UI automatically across devices | ✅ Verified & Working |
| **FCM Push Notifications** | `MyFirebaseMessagingService` + Cloud Functions trigger on `READY` status | ✅ Verified & Working |
| **Build Compilation** | `./gradlew assembleDebug` | ✅ **BUILD SUCCESSFUL** |

---

## 🔑 Server Credentials

To log in as the designated Canteen Server:
- **Username / Name**: `server`
- **Password**: `server123`
