package com.workflow.backend.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Servicio para enviar notificaciones push via Firebase Cloud Messaging.
 * Requiere el archivo firebase-service-account.json en src/main/resources/
 */
@Slf4j
@Service
public class FcmService {

    @Value("${fcm.enabled:false}")
    private boolean fcmEnabled;

    @PostConstruct
    public void init() {
        if (!fcmEnabled) {
            log.info("FCM deshabilitado. Para habilitarlo, configura fcm.enabled=true y agrega firebase-service-account.json");
            return;
        }
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount =
                    new ClassPathResource("firebase-service-account.json").getInputStream();
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase inicializado correctamente");
            }
        } catch (IOException e) {
            log.warn("⚠️ No se pudo inicializar Firebase: {}. Las notificaciones push estarán deshabilitadas.", e.getMessage());
        }
    }

    /**
     * Envía una notificación push a un dispositivo específico.
     *
     * @param fcmToken Token FCM del dispositivo destino
     * @param titulo   Título de la notificación
     * @param cuerpo   Cuerpo del mensaje
     * @param tipo     Tipo de notificación (TRAMITE_COMPLETADO, NUEVA_TAREA, etc.)
     * @param referenciaId ID del recurso relacionado
     */
    public void enviarNotificacion(String fcmToken, String titulo, String cuerpo,
                                    String tipo, String referenciaId) {
        if (!fcmEnabled || fcmToken == null || fcmToken.isBlank()) return;

        try {
            Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle(titulo)
                    .setBody(cuerpo)
                    .build())
                .putData("tipo", tipo != null ? tipo : "SISTEMA")
                .putData("referenciaId", referenciaId != null ? referenciaId : "")
                .setAndroidConfig(AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .setNotification(AndroidNotification.builder()
                        .setChannelId("workentai_channel")
                        .setIcon("ic_launcher_foreground")
                        .setColor("#2563EB")
                        .setSound("default")
                        .build())
                    .build())
                .setApnsConfig(ApnsConfig.builder()
                    .setAps(Aps.builder()
                        .setSound("default")
                        .setBadge(1)
                        .build())
                    .build())
                .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.debug("Notificación FCM enviada: {}", response);

        } catch (FirebaseMessagingException e) {
            log.warn("Error enviando notificación FCM a token {}: {}", 
                fcmToken.substring(0, Math.min(20, fcmToken.length())), e.getMessage());
        }
    }

    /**
     * Envía notificaciones a múltiples dispositivos.
     */
    public void enviarNotificacionMultiple(List<String> tokens, String titulo,
                                            String cuerpo, String tipo, String referenciaId) {
        if (!fcmEnabled || tokens == null || tokens.isEmpty()) return;
        tokens.stream()
            .filter(t -> t != null && !t.isBlank())
            .forEach(token -> enviarNotificacion(token, titulo, cuerpo, tipo, referenciaId));
    }
}
