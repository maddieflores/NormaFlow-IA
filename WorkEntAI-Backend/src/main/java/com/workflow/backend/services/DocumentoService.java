package com.workflow.backend.services;

import com.workflow.backend.exception.ResourceNotFoundException;
import com.workflow.backend.models.AuditoriaDocumento;
import com.workflow.backend.models.DocumentoTramite;
import com.workflow.backend.models.DocumentoTramite.PermisoDocumento;
import com.workflow.backend.repositories.AuditoriaDocumentoRepository;
import com.workflow.backend.repositories.DocumentoRepository;
import com.workflow.backend.repositories.PoliticaRepository;
import com.workflow.backend.repositories.TramiteRepository;
import com.workflow.backend.models.PermisoDocumental;
import com.workflow.backend.models.Politica;
import com.workflow.backend.models.Tramite;
import com.workflow.backend.models.Nodo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Servicio de Gestión Documental (Ciclo 2 — CU-24 al CU-27).
 *
 * Responsabilidades:
 *  - CU-24: Subir documentos → S3 + metadatos en MongoDB
 *  - CU-25: Versionamiento de documentos
 *  - CU-26: Control de permisos RBAC por documento
 *  - CU-27: Registro de auditoría de cada acceso
 *
 * Principio SRP: lógica de negocio separada de S3ArchivoStorage (infraestructura).
 * Principio DIP: depende de ArchivoStorage (interfaz), no de S3 directamente.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final AuditoriaDocumentoRepository auditoriaRepository;
    private final ArchivoStorage archivoStorage;  // Inyecta S3ArchivoStorage (@Primary)
    private final TramiteRepository tramiteRepository;
    private final PoliticaRepository politicaRepository;

    // ── CU-24: Subir documento ────────────────────────────────────────────────

    /**
     * Sube un archivo a S3 y persiste sus metadatos en MongoDB.
     * Registra evento de auditoría SUBIR.
     *
     * @param archivo    Archivo multipart recibido del frontend
     * @param tramiteId  ID del trámite al que se asocia
     * @param nodoId     ID del nodo del workflow (puede ser null)
     * @param descripcion Descripción opcional del documento
     * @param usuarioId  ID del usuario que sube el archivo
     * @param nombreUsuario Nombre del usuario (para desnormalización)
     * @return DocumentoTramite creado con todos sus metadatos
     */
    public DocumentoTramite subirDocumento(MultipartFile archivo, String tramiteId,
                                            String nodoId, String descripcion,
                                            String usuarioId, String nombreUsuario) throws IOException {
        String rolActual = getRolActual();
        
        // Verificar permisos del nodo (CU-25)
        if (!"ADMIN".equals(rolActual)) {
            Tramite tramite = tramiteRepository.findById(tramiteId)
                .orElseThrow(() -> new ResourceNotFoundException("Trámite no encontrado"));
            Politica politica = politicaRepository.findById(tramite.getPoliticaId())
                .orElseThrow(() -> new ResourceNotFoundException("Política no encontrada"));
            Nodo nodo = politica.getNodos().stream()
                .filter(n -> n.getId().equals(tramite.getNodoActualId())).findFirst()
                .orElse(null);
            
            if (nodo != null && nodo.getPermisosDocumentales() != null && !nodo.getPermisosDocumentales().isEmpty()) {
                PermisoDocumental pd = nodo.getPermisosDocumentales().get("USER:" + usuarioId);
                if (pd == null) pd = nodo.getPermisosDocumentales().get("ROL:" + rolActual);
                if (pd == null) pd = nodo.getPermisosDocumentales().get(rolActual); // fallback
                if (pd == null || !pd.isSubida()) {
                    registrarAuditoria(null, tramiteId, "SUBIR", usuarioId, nombreUsuario, rolActual, "DENEGADO", "No tiene permiso de SUBIDA en el nodo " + nodo.getNombre());
                    throw new SecurityException("No tiene permisos para subir documentos en esta etapa del trámite.");
                }
            } else {
                // Lógica por defecto si no hay RBAC estricto configurado en el nodo:
                // El cliente propietario del trámite puede subir archivos, o los funcionarios autorizados.
                if ("CLIENTE".equals(rolActual) && !tramite.getClienteId().equals(usuarioId)) {
                    registrarAuditoria(null, tramiteId, "SUBIR", usuarioId, nombreUsuario, rolActual, "DENEGADO", "El cliente no es el propietario del trámite");
                    throw new SecurityException("No tiene permisos para subir documentos a este trámite.");
                }
            }
        }

        // 1. Subir a S3 → obtener key
        String s3Key = archivoStorage.guardar(archivo, tramiteId);

        // 2. Calcular versión (CU-25)
        long versionesExistentes = documentoRepository.countByTramiteIdAndNombre(
                tramiteId, archivo.getOriginalFilename());
        int version = (int) versionesExistentes + 1;

        // 3. Permisos por defecto (CU-26): ADMIN puede todo; FUNCIONARIO puede ver/descargar
        List<PermisoDocumento> permisosDefecto = List.of(
            PermisoDocumento.builder()
                .rol("ADMIN")
                .acciones(Arrays.asList("VER", "DESCARGAR", "ELIMINAR", "CAMBIAR_PERMISOS"))
                .build(),
            PermisoDocumento.builder()
                .rol("FUNCIONARIO")
                .acciones(Arrays.asList("VER", "DESCARGAR"))
                .build(),
            PermisoDocumento.builder()
                .rol("CLIENTE")
                .acciones(List.of("VER", "DESCARGAR"))
                .build()
        );

        // 4. Persistir metadatos en MongoDB
        DocumentoTramite doc = DocumentoTramite.builder()
                .tramiteId(tramiteId)
                .nodoId(nodoId)
                .nombre(archivo.getOriginalFilename())
                .descripcion(descripcion)
                .tipoMime(archivo.getContentType())
                .tamanoBytes(archivo.getSize())
                .version(version)
                .s3Key(s3Key)
                .subidoPorId(usuarioId)
                .subidoPorNombre(nombreUsuario)
                .activo(true)
                .permisos(permisosDefecto)
                .fechaSubida(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();

        doc = documentoRepository.save(doc);

        // 5. Auditoría (CU-27)
        registrarAuditoria(doc.getId(), tramiteId, "SUBIR", usuarioId, nombreUsuario,
                getRolActual(), "OK", "Archivo subido: v" + version);

        log.info("📄 Documento subido: {} v{} para trámite {}", doc.getNombre(), version, tramiteId);
        return doc;
    }

    // ── CU-24: Listar documentos de un trámite ───────────────────────────────

    public List<DocumentoTramite> listarDocumentos(String tramiteId) {
        return documentoRepository.findByTramiteIdAndActivoTrue(tramiteId);
    }

    // ── CU-24: Obtener URL pre-firmada (CU-26: verificar permisos) ───────────

    /**
     * Genera una URL pre-firmada de descarga directa desde S3.
     * Verifica que el rol del usuario tenga permiso DESCARGAR sobre el documento.
     *
     * @param documentoId ID del documento
     * @param usuarioId   ID del usuario solicitante
     * @param nombreUsuario Nombre del usuario
     * @param rolUsuario  Rol del usuario (ADMIN, FUNCIONARIO, CLIENTE)
     * @return URL pre-firmada válida por la duración configurada
     */
    public String obtenerUrlDescarga(String documentoId, String usuarioId,
                                     String nombreUsuario, String rolUsuario) {
        DocumentoTramite doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado: " + documentoId));

        if (!doc.isActivo()) {
            throw new ResourceNotFoundException("El documento fue eliminado");
        }

        // Verificar permiso DESCARGAR (CU-26) y permiso de Nodo (CU-25)
        boolean tienePermiso = doc.getPermisos().stream()
                .anyMatch(p -> p.getRol().equals(rolUsuario)
                        && p.getAcciones().contains("DESCARGAR"));

        if (!"ADMIN".equals(rolUsuario)) {
            Tramite tramite = tramiteRepository.findById(doc.getTramiteId()).orElse(null);
            if (tramite != null) {
                Politica politica = politicaRepository.findById(tramite.getPoliticaId()).orElse(null);
                if (politica != null) {
                    Nodo nodo = politica.getNodos().stream()
                        .filter(n -> n.getId().equals(tramite.getNodoActualId())).findFirst().orElse(null);
                    if (nodo != null && nodo.getPermisosDocumentales() != null && !nodo.getPermisosDocumentales().isEmpty()) {
                        PermisoDocumental pd = nodo.getPermisosDocumentales().get("USER:" + usuarioId);
                        if (pd == null) pd = nodo.getPermisosDocumentales().get("ROL:" + rolUsuario);
                        if (pd == null) pd = nodo.getPermisosDocumentales().get(rolUsuario); // fallback
                        if (pd != null && pd.isLectura()) {
                            tienePermiso = true;
                        }
                    } else {
                        // Lógica por defecto si no hay RBAC configurado:
                        // Permitir a funcionarios y al propietario del trámite
                        if ("FUNCIONARIO".equals(rolUsuario) || 
                            ("CLIENTE".equals(rolUsuario) && doc.getTramiteId() != null && tramite.getClienteId().equals(usuarioId))) {
                            tienePermiso = true;
                        }
                    }
                }
            }
        }

        if (!tienePermiso && !"ADMIN".equals(rolUsuario)) {
            registrarAuditoria(documentoId, doc.getTramiteId(), "VER_URL",
                    usuarioId, nombreUsuario, rolUsuario, "DENEGADO",
                    "Sin permiso DESCARGAR/LECTURA para rol " + rolUsuario);
            throw new SecurityException("No tiene permiso para descargar este documento");
        }

        String url = archivoStorage.generarUrl(doc.getS3Key());

        // Auditoría (CU-27)
        registrarAuditoria(documentoId, doc.getTramiteId(), "VER_URL",
                usuarioId, nombreUsuario, rolUsuario, "OK", "URL pre-firmada generada");

        return url;
    }

    // ── CU-26: Actualizar permisos ────────────────────────────────────────────

    public DocumentoTramite actualizarPermisos(String documentoId,
                                               List<PermisoDocumento> nuevosPermisos,
                                               String adminId, String adminNombre) {
        DocumentoTramite doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado: " + documentoId));

        doc.setPermisos(nuevosPermisos);
        doc.setFechaActualizacion(LocalDateTime.now());
        doc = documentoRepository.save(doc);

        registrarAuditoria(documentoId, doc.getTramiteId(), "CAMBIAR_PERMISOS",
                adminId, adminNombre, "ADMIN", "OK",
                "Permisos actualizados: " + nuevosPermisos.size() + " entradas");

        return doc;
    }

    // ── CU-24: Eliminar (soft delete) ────────────────────────────────────────

    public void eliminarDocumento(String documentoId, String usuarioId, String nombreUsuario) {
        DocumentoTramite doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado: " + documentoId));

        doc.setActivo(false);
        doc.setFechaActualizacion(LocalDateTime.now());
        documentoRepository.save(doc);

        // El archivo en S3 se mantiene por auditoría; solo se oculta lógicamente
        registrarAuditoria(documentoId, doc.getTramiteId(), "ELIMINAR",
                usuarioId, nombreUsuario, getRolActual(), "OK", "Eliminación lógica");
        log.info("🗑️  Documento {} marcado como inactivo", documentoId);
    }

    // ── CU-27: Historial de auditoría ────────────────────────────────────────

    public List<AuditoriaDocumento> obtenerAuditoria(String documentoId) {
        return auditoriaRepository.findByDocumentoIdOrderByTimestampDesc(documentoId);
    }

    public List<AuditoriaDocumento> obtenerAuditoriaTramite(String tramiteId) {
        return auditoriaRepository.findByTramiteIdOrderByTimestampDesc(tramiteId);
    }

    // ── Helper privado: registrar auditoría ──────────────────────────────────

    private void registrarAuditoria(String documentoId, String tramiteId, String accion,
                                    String usuarioId, String nombreUsuario, String rol,
                                    String resultado, String detalle) {
        AuditoriaDocumento evento = AuditoriaDocumento.builder()
                .documentoId(documentoId)
                .tramiteId(tramiteId)
                .accion(accion)
                .usuarioId(usuarioId)
                .nombreUsuario(nombreUsuario)
                .rolUsuario(rol)
                .resultado(resultado)
                .detalle(detalle)
                .timestamp(LocalDateTime.now())
                .build();
        auditoriaRepository.save(evento);
    }

    private String getRolActual() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getAuthorities() != null) {
                return auth.getAuthorities().stream()
                        .findFirst()
                        .map(a -> a.getAuthority().replace("ROLE_", ""))
                        .orElse("DESCONOCIDO");
            }
        } catch (Exception ignored) {}
        return "DESCONOCIDO";
    }
}
