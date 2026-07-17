importScripts('https://www.gstatic.com/firebasejs/10.8.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.8.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "AIzaSyA7z4igPlRM-xgb0P0uNihMadk478FWAgw",
  authDomain: "workentai-71e87.firebaseapp.com",
  projectId: "workentai-71e87",
  storageBucket: "workentai-71e87.firebasestorage.app",
  messagingSenderId: "359881974140",
  appId: "1:359881974140:web:2b2169d843323fc8bfc4f9"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
  console.log('[firebase-messaging-sw.js] Mensaje recibido en segundo plano ', payload);
  const notificationTitle = payload.notification.title || "Notificación de WorkEntAI";
  const notificationOptions = {
    body: payload.notification.body,
    icon: '/favicon.ico'
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});
