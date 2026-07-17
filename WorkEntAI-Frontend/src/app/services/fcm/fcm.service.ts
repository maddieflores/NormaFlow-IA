import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { initializeApp } from 'firebase/app';
import { getMessaging, getToken, onMessage, Messaging } from 'firebase/messaging';
import { environment } from '../../../environments/environment';
import { AuthService } from '../auth/auth.service';

@Injectable({
  providedIn: 'root'
})
export class FcmService {
  private messaging: Messaging | null = null;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
    try {
      if (environment.firebase) {
        const app = initializeApp(environment.firebase);
        this.messaging = getMessaging(app);
      }
    } catch (e) {
      console.error('Error inicializando Firebase en Angular:', e);
    }
  }

  requestPermission() {
    if (!this.messaging) return;

    Notification.requestPermission().then((permission) => {
      if (permission === 'granted') {
        console.log('Permiso de notificación concedido.');
        this.getTokenAndSave();
      } else {
        console.log('No se concedió permiso para notificaciones.');
      }
    });
  }

  private getTokenAndSave() {
    if (!this.messaging) return;

    getToken(this.messaging).then((currentToken) => {
      if (currentToken) {
        console.log('FCM Token obtenido:', currentToken);
        this.sendTokenToBackend(currentToken);
      } else {
        console.log('No se pudo obtener el token de registro. Asegúrate de que el Service Worker esté configurado.');
      }
    }).catch((err) => {
      console.log('Ocurrió un error al obtener el token.', err);
    });
  }

  private sendTokenToBackend(token: string) {
    const user = this.authService.getUser();
    if (user && user.id) {
      this.http.post(`${environment.apiUrl}/usuarios/${user.id}/fcm-token`, { token })
        .subscribe({
          next: () => console.log('Token FCM enviado al backend.'),
          error: (err) => console.error('Error enviando token FCM', err)
        });
    }
  }

  listenForMessages() {
    if (!this.messaging) return;

    onMessage(this.messaging, (payload) => {
      console.log('Mensaje FCM recibido en primer plano:', payload);

      if (payload.notification) {
        const title = payload.notification.title || 'NormaFlow Notificación';
        const options = {
          body: payload.notification.body,
          icon: '/favicon.ico'
        };

        // Muestra notificación emergente en el navegador si está en primer plano
        new Notification(title, options);
      }
    });
  }
}
