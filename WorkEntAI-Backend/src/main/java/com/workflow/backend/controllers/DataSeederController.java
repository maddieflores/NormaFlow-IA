package com.workflow.backend.controllers;

import com.workflow.backend.enums.EstadoPolitica;
import com.workflow.backend.enums.EstadoTarea;
import com.workflow.backend.enums.EstadoTramite;
import com.workflow.backend.enums.RolUsuario;
import com.workflow.backend.models.*;
import com.workflow.backend.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
public class DataSeederController {

        private final UsuarioRepository usuarioRepository;
        private final PoliticaRepository politicaRepository;
        private final TramiteRepository tramiteRepository;
        private final TareaRepository tareaRepository;
        private final NotificacionRepository notificacionRepository;
        private final DepartamentoRepository departamentoRepository;
        private final PasswordEncoder passwordEncoder;

        @PostMapping("/all")
        public ResponseEntity<Map<String, Object>> seedAll() {

                // Limpiar todo en orden correcto
                tareaRepository.deleteAll();
                notificacionRepository.deleteAll();
                tramiteRepository.deleteAll();
                politicaRepository.deleteAll();
                usuarioRepository.deleteAll();
                departamentoRepository.deleteAll();

                // Limpiar también las sesiones del agente para no dejar IDs huérfanos de
                // políticas
                org.springframework.context.ApplicationContext context = org.springframework.web.context.support.WebApplicationContextUtils
                                .getRequiredWebApplicationContext(
                                                ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder
                                                                .getRequestAttributes()).getRequest()
                                                                .getServletContext());
                context.getBean(AgenteRepository.class).deleteAll();

                LocalDateTime ahora = LocalDateTime.now();

                // ── 1. DEPARTAMENTOS ──────────────────────────────────────
                Departamento deptAtencion = departamentoRepository.save(Departamento.builder()
                                .nombre("Atención al Cliente")
                                .descripcion("Recepción y gestión de solicitudes de clientes")
                                .fechaCreacion(ahora.minusMonths(6)).build());

                Departamento deptTecnico = departamentoRepository.save(Departamento.builder()
                                .nombre("Dept. Técnico")
                                .descripcion("Evaluaciones técnicas e inspecciones de campo")
                                .fechaCreacion(ahora.minusMonths(6)).build());

                Departamento deptLegal = departamentoRepository.save(Departamento.builder()
                                .nombre("Dept. Legal")
                                .descripcion("Revisión contractual y cumplimiento normativo")
                                .fechaCreacion(ahora.minusMonths(6)).build());

                Departamento deptAlmacen = departamentoRepository.save(Departamento.builder()
                                .nombre("Almacén")
                                .descripcion("Gestión y despacho de materiales e insumos")
                                .fechaCreacion(ahora.minusMonths(6)).build());

                Departamento deptFinanzas = departamentoRepository.save(Departamento.builder()
                                .nombre("Finanzas")
                                .descripcion("Gestión de pagos, cobros y facturación")
                                .fechaCreacion(ahora.minusMonths(6)).build());

                // ── 2. USUARIOS ──────────────────────────────────────────
                Usuario admin = usuarioRepository.save(crearUsuario("Carlos Mendoza", "admin@workflow.com",
                                RolUsuario.ADMIN, "Administración"));
                Usuario admin2 = usuarioRepository.save(crearUsuario("Sofía Ramírez", "admin2@workflow.com",
                                RolUsuario.ADMIN, "Administración"));

                Usuario func1 = usuarioRepository.save(crearUsuario("Juan Pérez", "juan@workflow.com",
                                RolUsuario.FUNCIONARIO, "Atención al Cliente"));
                Usuario func1b = usuarioRepository.save(crearUsuario("Lucía Vargas", "lucia@workflow.com",
                                RolUsuario.FUNCIONARIO, "Atención al Cliente"));
                Usuario func2 = usuarioRepository.save(
                                crearUsuario("Ana López", "ana@workflow.com", RolUsuario.FUNCIONARIO, "Dept. Técnico"));
                Usuario func2b = usuarioRepository.save(crearUsuario("Roberto Silva", "roberto@workflow.com",
                                RolUsuario.FUNCIONARIO, "Dept. Técnico"));
                Usuario func3 = usuarioRepository.save(crearUsuario("María Torres", "maria@workflow.com",
                                RolUsuario.FUNCIONARIO, "Dept. Legal"));
                Usuario func4 = usuarioRepository.save(
                                crearUsuario("Pedro Gómez", "pedro@workflow.com", RolUsuario.FUNCIONARIO, "Almacén"));
                Usuario func5 = usuarioRepository.save(crearUsuario("Elena Castillo", "elena@workflow.com",
                                RolUsuario.FUNCIONARIO, "Finanzas"));

                Usuario cliente1 = usuarioRepository.save(crearUsuario("Empresa ABC S.R.L.", "cliente@workflow.com",
                                RolUsuario.CLIENTE, "Externo"));
                Usuario cliente2 = usuarioRepository.save(crearUsuario("Constructora Norte S.A.",
                                "constructora@workflow.com", RolUsuario.CLIENTE, "Externo"));
                Usuario cliente3 = usuarioRepository.save(crearUsuario("Hospital Regional", "hospital@workflow.com",
                                RolUsuario.CLIENTE, "Externo"));
                Usuario cliente4 = usuarioRepository.save(crearUsuario("Supermercado El Sol",
                                "supermercado@workflow.com", RolUsuario.CLIENTE, "Externo"));
                Usuario cliente5 = usuarioRepository.save(crearUsuario("Residencial Los Pinos",
                                "residencial@workflow.com", RolUsuario.CLIENTE, "Externo"));

                // Asignar responsables a departamentos
                deptAtencion.setResponsableId(func1.getId());
                deptTecnico.setResponsableId(func2.getId());
                deptLegal.setResponsableId(func3.getId());
                deptAlmacen.setResponsableId(func4.getId());
                deptFinanzas.setResponsableId(func5.getId());
                departamentoRepository
                                .saveAll(List.of(deptAtencion, deptTecnico, deptLegal, deptAlmacen, deptFinanzas));

                // ── 3. POLÍTICA 1: Instalación de Medidor Eléctrico ──────
                Nodo p1Start = Nodo.builder()
                                .id("p1n1").nombre("INICIO").tipo("START")
                                .descripcion("Inicio del proceso de instalación de medidor")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .posX(120).posY(80)
                                .conexiones(List.of("p1n2"))
                                .color("#4CAF50").build();

                Nodo p1Recepcion = Nodo.builder()
                                .id("p1n2").nombre("Recibir Solicitud").tipo("TASK")
                                .descripcion("Recibir y verificar la documentación del cliente para la solicitud de instalación")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(120).posY(220)
                                .conexiones(List.of("p1n3"))
                                .camposFormulario(List.of(
                                                campo("observacion", "textarea", "Observaciones generales", true),
                                                campo("aprobado", "boolean", "¿Documentación completa?", true)))
                                .color("#2196F3").build();

                Nodo p1Tecnico = Nodo.builder()
                                .id("p1n3").nombre("Evaluación Técnica").tipo("TASK")
                                .descripcion("Evaluar la viabilidad técnica de la instalación en la ubicación indicada")
                                .departamento("Dept. Técnico")
                                .responsableId(func2.getId()).nombreResponsable(func2.getNombre())
                                .tiempoLimiteHoras(48)
                                .posX(370).posY(220)
                                .conexiones(List.of("p1n4"))
                                .camposFormulario(List.of(
                                                campo("informe_tecnico", "textarea", "Informe técnico de factibilidad",
                                                                true),
                                                campo("aprobado", "boolean", "¿Factible técnicamente?", true),
                                                campo("observacion", "textarea", "Observaciones adicionales", false)))
                                .color("#2196F3").build();

                Nodo p1Legal = Nodo.builder()
                                .id("p1n4").nombre("Revisión Legal").tipo("TASK")
                                .descripcion("Revisar aspectos legales y contractuales de la instalación")
                                .departamento("Dept. Legal")
                                .responsableId(func3.getId()).nombreResponsable(func3.getNombre())
                                .tiempoLimiteHoras(72)
                                .posX(620).posY(220)
                                .conexiones(List.of("p1n5"))
                                .camposFormulario(List.of(
                                                campo("numero_contrato", "text", "Número de contrato", true),
                                                campo("observacion", "textarea", "Observaciones legales", false),
                                                campo("aprobado", "boolean", "¿Aprobado legalmente?", true)))
                                .color("#FF9800").build();

                Nodo p1Almacen = Nodo.builder()
                                .id("p1n5").nombre("Despacho de Materiales").tipo("TASK")
                                .descripcion("Preparar y despachar los materiales necesarios para la instalación")
                                .departamento("Almacén")
                                .responsableId(func4.getId()).nombreResponsable(func4.getNombre())
                                .tiempoLimiteHoras(48)
                                .posX(870).posY(220)
                                .conexiones(List.of("p1n6"))
                                .camposFormulario(List.of(
                                                campo("materiales", "textarea", "Lista de materiales despachados",
                                                                true),
                                                campo("aprobado", "boolean", "¿Materiales despachados?", true)))
                                .color("#2196F3").build();

                Nodo p1Facturacion = Nodo.builder()
                                .id("p1n6").nombre("Facturación").tipo("TASK")
                                .descripcion("Emitir factura y registrar el cobro por la instalación del medidor")
                                .departamento("Finanzas")
                                .responsableId(func5.getId()).nombreResponsable(func5.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(1120).posY(220)
                                .conexiones(List.of("p1n7"))
                                .camposFormulario(List.of(
                                                campo("monto_factura", "text", "Monto total facturado (Bs.)", true),
                                                campo("observacion", "textarea", "Observaciones de facturación", false),
                                                campo("aprobado", "boolean", "¿Factura emitida?", true)))
                                .color("#9C27B0").build();

                Nodo p1End = Nodo.builder()
                                .id("p1n7").nombre("FIN").tipo("END")
                                .descripcion("Proceso de instalación completado exitosamente")
                                .departamento("").responsableId("")
                                .posX(620).posY(420)
                                .color("#F44336").build();

                Politica politica1 = politicaRepository.save(Politica.builder()
                                .nombre("Instalación de Medidor Eléctrico")
                                .descripcion("Proceso completo para solicitar e instalar un medidor eléctrico en COTAS Energy")
                                .categoria("Servicios Eléctricos")
                                .organizacion("COTAS Energy")
                                .tiempoEstimadoDias(7)
                                .nodos(new ArrayList<>(List.of(p1Start, p1Recepcion, p1Tecnico, p1Legal, p1Almacen,
                                                p1Facturacion, p1End)))
                                .estado(EstadoPolitica.ACTIVA)
                                .creadoPorId(admin.getId()).nombreCreadoPor(admin.getNombre())
                                .version(1).tramitesActivos(5).tramitesCompletados(2)
                                .fechaCreacion(ahora.minusMonths(3))
                                .fechaActualizacion(ahora.minusDays(1))
                                .fechaActivacion(ahora.minusMonths(3))
                                .requisitosIniciales(List.of(
                                                RequisitoTramite.builder().nombre("Cédula de Identidad").descripcion(
                                                                "Copia del carnet de identidad en PDF o JPG")
                                                                .obligatorio(true).build(),
                                                RequisitoTramite.builder().nombre("Plano de Ubicación").descripcion(
                                                                "Croquis o plano del inmueble en Word, Excel o PDF")
                                                                .obligatorio(true).build(),
                                                RequisitoTramite.builder().nombre("Video o Foto de la Acometida")
                                                                .descripcion("Video MP4 o foto clara del punto de instalación (opcional)")
                                                                .obligatorio(false).build()))
                                .build());

                // ── 4. POLÍTICA 2: Reconexión de Servicio Eléctrico ──────
                Nodo p2Start = Nodo.builder()
                                .id("p2n1").nombre("INICIO").tipo("START")
                                .descripcion("Inicio del proceso de reconexión")
                                .departamento("Finanzas")
                                .responsableId(func5.getId()).nombreResponsable(func5.getNombre())
                                .posX(120).posY(80)
                                .conexiones(List.of("p2n2"))
                                .color("#4CAF50").build();

                Nodo p2VerDeuda = Nodo.builder()
                                .id("p2n2").nombre("Verificar Deuda").tipo("TASK")
                                .descripcion("Verificar el estado de deuda del cliente y confirmar pago")
                                .departamento("Finanzas")
                                .responsableId(func5.getId()).nombreResponsable(func5.getNombre())
                                .tiempoLimiteHoras(8)
                                .posX(120).posY(220)
                                .conexiones(List.of("p2n3"))
                                .camposFormulario(List.of(
                                                campo("monto_deuda", "text", "Monto de deuda pendiente (Bs.)", true),
                                                campo("fecha_pago", "text", "Fecha de último pago", false),
                                                campo("aprobado", "boolean", "¿Deuda cancelada en su totalidad?", true),
                                                campo("observacion", "textarea", "Observaciones", false)))
                                .color("#9C27B0").build();

                Map<String, String> condicionesDecision = new HashMap<>();
                condicionesDecision.put("true", "p2n4");
                condicionesDecision.put("false", "p2n5");

                Nodo p2Decision = Nodo.builder()
                                .id("p2n3").nombre("¿Deuda pagada?").tipo("DECISION")
                                .descripcion("Verificar si el cliente ha cancelado la deuda pendiente")
                                .departamento("Finanzas")
                                .responsableId(func5.getId()).nombreResponsable(func5.getNombre())
                                .posX(370).posY(220)
                                .conexiones(List.of("p2n4", "p2n5"))
                                .condiciones(condicionesDecision)
                                .color("#FF5722").build();

                Nodo p2Reconexion = Nodo.builder()
                                .id("p2n4").nombre("Reconexión Técnica").tipo("TASK")
                                .descripcion("Realizar la reconexión física del servicio eléctrico en campo")
                                .departamento("Dept. Técnico")
                                .responsableId(func2.getId()).nombreResponsable(func2.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(620).posY(120)
                                .conexiones(List.of("p2n6"))
                                .camposFormulario(List.of(
                                                campo("observacion", "textarea", "Informe de reconexión", true),
                                                campo("aprobado", "boolean", "¿Reconexión realizada?", true)))
                                .color("#2196F3").build();

                Nodo p2Rechazo = Nodo.builder()
                                .id("p2n5").nombre("Notificar Rechazo").tipo("TASK")
                                .descripcion("Notificar al cliente que su solicitud fue rechazada por deuda pendiente")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .tiempoLimiteHoras(8)
                                .posX(620).posY(320)
                                .conexiones(List.of("p2n6"))
                                .camposFormulario(List.of(
                                                campo("observacion", "textarea", "Motivo del rechazo", true),
                                                campo("aprobado", "boolean", "¿Notificación enviada?", true)))
                                .color("#F44336").build();

                Nodo p2End = Nodo.builder()
                                .id("p2n6").nombre("FIN").tipo("END")
                                .descripcion("Proceso de reconexión finalizado")
                                .departamento("").responsableId("")
                                .posX(870).posY(220)
                                .color("#F44336").build();

                Politica politica2 = politicaRepository.save(Politica.builder()
                                .nombre("Reconexión de Servicio Eléctrico")
                                .descripcion("Proceso para reconexión de servicio eléctrico suspendido por falta de pago")
                                .categoria("Servicios Eléctricos")
                                .organizacion("COTAS Energy")
                                .tiempoEstimadoDias(3)
                                .nodos(new ArrayList<>(List.of(p2Start, p2VerDeuda, p2Decision, p2Reconexion, p2Rechazo,
                                                p2End)))
                                .estado(EstadoPolitica.ACTIVA)
                                .creadoPorId(admin.getId()).nombreCreadoPor(admin.getNombre())
                                .version(1).tramitesActivos(2).tramitesCompletados(2)
                                .fechaCreacion(ahora.minusMonths(2))
                                .fechaActualizacion(ahora.minusDays(2))
                                .fechaActivacion(ahora.minusMonths(2))
                                .requisitosIniciales(List.of(
                                                RequisitoTramite.builder().nombre("Comprobante de Pago").descripcion(
                                                                "Recibo o comprobante de pago de la deuda en PDF, JPG o PNG")
                                                                .obligatorio(true).build()))
                                .build());

                // ── 5. POLÍTICA 3: Cambio de Medidor por Avería (BORRADOR) ─
                Nodo p3Start = Nodo.builder()
                                .id("p3n1").nombre("INICIO").tipo("START")
                                .descripcion("Inicio del proceso de cambio de medidor")
                                .departamento("Dept. Técnico")
                                .responsableId(func2.getId()).nombreResponsable(func2.getNombre())
                                .posX(120).posY(80)
                                .conexiones(List.of("p3n2"))
                                .color("#4CAF50").build();

                Nodo p3Inspeccion = Nodo.builder()
                                .id("p3n2").nombre("Inspección Técnica").tipo("TASK")
                                .descripcion("Inspeccionar el medidor averiado y determinar el tipo de reemplazo necesario")
                                .departamento("Dept. Técnico")
                                .responsableId(func2.getId()).nombreResponsable(func2.getNombre())
                                .tiempoLimiteHoras(48)
                                .posX(370).posY(220)
                                .conexiones(List.of("p3n3"))
                                .camposFormulario(List.of(
                                                campo("tipo_averia", "text", "Tipo de avería detectada", true),
                                                campo("observacion", "textarea", "Informe de inspección", true),
                                                campo("aprobado", "boolean", "¿Requiere cambio inmediato?", true)))
                                .color("#2196F3").build();

                Nodo p3End = Nodo.builder()
                                .id("p3n3").nombre("FIN").tipo("END")
                                .descripcion("Proceso de cambio de medidor finalizado")
                                .departamento("").responsableId("")
                                .posX(620).posY(220)
                                .color("#F44336").build();

                politicaRepository.save(Politica.builder()
                                .nombre("Cambio de Medidor por Avería")
                                .descripcion("Proceso para solicitar el cambio de un medidor eléctrico dañado o averiado")
                                .categoria("Servicios Eléctricos")
                                .organizacion("COTAS Energy")
                                .tiempoEstimadoDias(5)
                                .nodos(new ArrayList<>(List.of(p3Start, p3Inspeccion, p3End)))
                                .estado(EstadoPolitica.BORRADOR)
                                .creadoPorId(admin2.getId()).nombreCreadoPor(admin2.getNombre())
                                .version(1).tramitesActivos(0).tramitesCompletados(0)
                                .fechaCreacion(ahora.minusDays(5))
                                .fechaActualizacion(ahora.minusDays(5))
                                .build());

                // ── 5.1. POLÍTICA 4: Solicitud de Reembolso de Facturación ──────
                Nodo p4Start = Nodo.builder()
                                .id("p4n1").nombre("INICIO").tipo("START")
                                .descripcion("Inicio de solicitud de reembolso")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .posX(120).posY(80)
                                .conexiones(List.of("p4n2"))
                                .color("#4CAF50").build();

                Nodo p4Recepcion = Nodo.builder()
                                .id("p4n2").nombre("Recibir Reclamación").tipo("TASK")
                                .descripcion("Recibir solicitud y comprobar la factura de cobro")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(120).posY(220)
                                .conexiones(List.of("p4n3"))
                                .camposFormulario(List.of(
                                                campo("motivo", "textarea", "Motivo detallado de la reclamación", true),
                                                campo("aprobado", "boolean", "¿Factura adjunta y válida?", true)))
                                .color("#2196F3").build();

                Nodo p4Evaluacion = Nodo.builder()
                                .id("p4n3").nombre("Analizar Facturación").tipo("TASK")
                                .descripcion("Auditar lecturas y consumos históricos para validar cobro excesivo")
                                .departamento("Finanzas")
                                .responsableId(func5.getId()).nombreResponsable(func5.getNombre())
                                .tiempoLimiteHoras(48)
                                .posX(370).posY(220)
                                .conexiones(List.of("p4n4"))
                                .camposFormulario(List.of(
                                                campo("consumo_calculado", "text", "Consumo real auditado (kWh)", true),
                                                campo("aprobado", "boolean", "¿Aplica devolución/crédito?", true),
                                                campo("monto_devolucion", "text", "Monto a devolver (Bs.)", false)))
                                .color("#9C27B0").build();

                Nodo p4AprobacionLegal = Nodo.builder()
                                .id("p4n4").nombre("Aprobación de Reembolso").tipo("TASK")
                                .descripcion("Validar la procedencia jurídica del reembolso económico")
                                .departamento("Dept. Legal")
                                .responsableId(func3.getId()).nombreResponsable(func3.getNombre())
                                .tiempoLimiteHoras(48)
                                .posX(620).posY(220)
                                .conexiones(List.of("p4n5"))
                                .camposFormulario(List.of(
                                                campo("informe_legal", "textarea", "Informe jurídico de aprobación",
                                                                true),
                                                campo("aprobado", "boolean", "¿Conforme con la normativa?", true)))
                                .color("#FF9800").build();

                Nodo p4EmisionPago = Nodo.builder()
                                .id("p4n5").nombre("Emitir Pago").tipo("TASK")
                                .descripcion("Generar orden de pago o acreditación en cuenta")
                                .departamento("Finanzas")
                                .responsableId(func5.getId()).nombreResponsable(func5.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(870).posY(220)
                                .conexiones(List.of("p4n6"))
                                .camposFormulario(List.of(
                                                campo("nro_transferencia", "text",
                                                                "Número de transferencia/acreditación", true),
                                                campo("aprobado", "boolean", "¿Pago emitido exitosamente?", true)))
                                .color("#9C27B0").build();

                Nodo p4End = Nodo.builder()
                                .id("p4n6").nombre("FIN").tipo("END")
                                .descripcion("Proceso de reembolso finalizado")
                                .departamento("").responsableId("")
                                .posX(1120).posY(220)
                                .color("#F44336").build();

                politicaRepository.save(Politica.builder()
                                .nombre("Reembolso por Cobro Excesivo")
                                .descripcion("Proceso de reclamación y reembolso por lecturas erróneas o facturación excesiva")
                                .categoria("Atención al Cliente")
                                .organizacion("COTAS Energy")
                                .tiempoEstimadoDias(6)
                                .nodos(new ArrayList<>(List.of(p4Start, p4Recepcion, p4Evaluacion, p4AprobacionLegal,
                                                p4EmisionPago, p4End)))
                                .estado(EstadoPolitica.ACTIVA)
                                .creadoPorId(admin.getId()).nombreCreadoPor(admin.getNombre())
                                .version(1).tramitesActivos(0).tramitesCompletados(0)
                                .fechaCreacion(ahora.minusDays(10))
                                .fechaActualizacion(ahora.minusDays(2))
                                .fechaActivacion(ahora.minusDays(10))
                                .requisitosIniciales(List.of(
                                                RequisitoTramite.builder().nombre("Factura Reclamada").descripcion(
                                                                "Copia digital de la factura con cobro excesivo")
                                                                .obligatorio(true).build(),
                                                RequisitoTramite.builder().nombre("Cédula de Identidad").descripcion(
                                                                "Carnet de identidad del titular del servicio")
                                                                .obligatorio(true).build()))
                                .build());

                // ── 5.2. POLÍTICA 5: Mantenimiento Preventivo de Acometida ──────
                Nodo p5Start = Nodo.builder()
                                .id("p5n1").nombre("INICIO").tipo("START")
                                .descripcion("Inicio de solicitud de mantenimiento")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .posX(120).posY(80)
                                .conexiones(List.of("p5n2"))
                                .color("#4CAF50").build();

                Nodo p5Agendar = Nodo.builder()
                                .id("p5n2").nombre("Agendar Inspección").tipo("TASK")
                                .descripcion("Asignar fecha y hora para la visita técnica preventiva")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(120).posY(220)
                                .conexiones(List.of("p5n3"))
                                .camposFormulario(List.of(
                                                campo("fecha_visita", "text", "Fecha programada (DD/MM/AAAA)", true),
                                                campo("aprobado", "boolean", "¿Agenda confirmada con el cliente?",
                                                                true)))
                                .color("#2196F3").build();

                Nodo p5Tecnico = Nodo.builder()
                                .id("p5n3").nombre("Mantenimiento de Campo").tipo("TASK")
                                .descripcion("Inspección técnica física y reemplazo preventivo de cables/conectores")
                                .departamento("Dept. Técnico")
                                .responsableId(func2.getId()).nombreResponsable(func2.getNombre())
                                .tiempoLimiteHoras(48)
                                .posX(370).posY(220)
                                .conexiones(List.of("p5n4"))
                                .camposFormulario(List.of(
                                                campo("estado_acometida", "text",
                                                                "Estado de acometida (Bueno/Regular/Malo)", true),
                                                campo("acciones_tomadas", "textarea",
                                                                "Detalle de reparaciones efectuadas", true),
                                                campo("aprobado", "boolean", "¿Mantenimiento concluido con éxito?",
                                                                true)))
                                .color("#2196F3").build();

                Nodo p5Materiales = Nodo.builder()
                                .id("p5n4").nombre("Registro de Consumos").tipo("TASK")
                                .descripcion("Registrar repuestos y materiales del stock de almacén usados en la visita")
                                .departamento("Almacén")
                                .responsableId(func4.getId()).nombreResponsable(func4.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(620).posY(220)
                                .conexiones(List.of("p5n5"))
                                .camposFormulario(List.of(
                                                campo("materiales_usados", "textarea",
                                                                "Listado de insumos utilizados del inventario", true),
                                                campo("aprobado", "boolean", "¿Carga de inventario correcta?", true)))
                                .color("#2196F3").build();

                Nodo p5End = Nodo.builder()
                                .id("p5n5").nombre("FIN").tipo("END")
                                .descripcion("Mantenimiento preventivo completado")
                                .departamento("").responsableId("")
                                .posX(870).posY(220)
                                .color("#F44336").build();

                politicaRepository.save(Politica.builder()
                                .nombre("Mantenimiento Preventivo de Acometida")
                                .descripcion("Inspección y refacción preventiva de cables, conectores y cajas de derivación exterior")
                                .categoria("Servicios Técnicos")
                                .organizacion("COTAS Energy")
                                .tiempoEstimadoDias(4)
                                .nodos(new ArrayList<>(List.of(p5Start, p5Agendar, p5Tecnico, p5Materiales, p5End)))
                                .estado(EstadoPolitica.ACTIVA)
                                .creadoPorId(admin2.getId()).nombreCreadoPor(admin2.getNombre())
                                .version(1).tramitesActivos(0).tramitesCompletados(0)
                                .fechaCreacion(ahora.minusDays(15))
                                .fechaActualizacion(ahora.minusDays(5))
                                .fechaActivacion(ahora.minusDays(15))
                                .requisitosIniciales(List.of(
                                                RequisitoTramite.builder().nombre("Fotografía de Acometida")
                                                                .descripcion("Foto clara de la caja o cables exteriores a revisar")
                                                                .obligatorio(false).build()))
                                .build());

                // ── 5.3. POLÍTICA 6: Solicitud de Cambio de Titularidad ──────
                Nodo p6Start = Nodo.builder()
                                .id("p6n1").nombre("INICIO").tipo("START")
                                .descripcion("Inicio de cambio de titularidad")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .posX(120).posY(80)
                                .conexiones(List.of("p6n2"))
                                .color("#4CAF50").build();

                Nodo p6Recepcion = Nodo.builder()
                                .id("p6n2").nombre("Recibir Documentación").tipo("TASK")
                                .descripcion("Revisar carnet de identidad, documento de compra-venta o sucesión y facturas al día")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(120).posY(220)
                                .conexiones(List.of("p6n3"))
                                .camposFormulario(List.of(
                                                campo("nuevo_titular", "text", "Nombre completo del nuevo titular",
                                                                true),
                                                campo("documento_propiedad", "boolean",
                                                                "¿Presenta título de propiedad/sucesión?", true)))
                                .color("#2196F3").build();

                Nodo p6VerificacionLegal = Nodo.builder()
                                .id("p6n3").nombre("Validación Jurídica").tipo("TASK")
                                .descripcion("Validar la legalidad de los documentos presentados para acreditar la propiedad")
                                .departamento("Dept. Legal")
                                .responsableId(func3.getId()).nombreResponsable(func3.getNombre())
                                .tiempoLimiteHoras(48)
                                .posX(370).posY(220)
                                .conexiones(List.of("p6n4"))
                                .camposFormulario(List.of(
                                                campo("aprobado", "boolean", "¿Documentación legal conforme?", true),
                                                campo("observaciones", "textarea", "Observaciones del dictamen legal",
                                                                false)))
                                .color("#FF9800").build();

                Nodo p6ActualizacionFinanzas = Nodo.builder()
                                .id("p6n4").nombre("Actualizar Registro").tipo("TASK")
                                .descripcion("Actualizar el nombre y datos de facturación en el sistema comercial")
                                .departamento("Finanzas")
                                .responsableId(func5.getId()).nombreResponsable(func5.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(620).posY(220)
                                .conexiones(List.of("p6n5"))
                                .camposFormulario(List.of(
                                                campo("codigo_cliente_nuevo", "text",
                                                                "Nuevo código de cliente comercial", true),
                                                campo("aprobado", "boolean", "¿Base de datos actualizada?", true)))
                                .color("#9C27B0").build();

                Nodo p6End = Nodo.builder()
                                .id("p6n5").nombre("FIN").tipo("END")
                                .descripcion("Trámite de cambio de titularidad completado")
                                .departamento("").responsableId("")
                                .posX(870).posY(220)
                                .color("#F44336").build();

                politicaRepository.save(Politica.builder()
                                .nombre("Cambio de Titularidad de Suministro")
                                .descripcion("Trámite para cambiar el nombre del propietario o titular del servicio eléctrico")
                                .categoria("Trámites Administrativos")
                                .organizacion("COTAS Energy")
                                .tiempoEstimadoDias(4)
                                .nodos(new ArrayList<>(List.of(p6Start, p6Recepcion, p6VerificacionLegal,
                                                p6ActualizacionFinanzas, p6End)))
                                .estado(EstadoPolitica.ACTIVA)
                                .creadoPorId(admin.getId()).nombreCreadoPor(admin.getNombre())
                                .version(1).tramitesActivos(0).tramitesCompletados(0)
                                .fechaCreacion(ahora.minusDays(20))
                                .fechaActualizacion(ahora.minusDays(5))
                                .fechaActivacion(ahora.minusDays(20))
                                .requisitosIniciales(List.of(
                                                RequisitoTramite.builder().nombre("Cédula de Identidad").descripcion(
                                                                "Carnet de identidad del nuevo propietario en formato legible")
                                                                .obligatorio(true).build(),
                                                RequisitoTramite.builder().nombre("Título de Propiedad").descripcion(
                                                                "Testimonio de propiedad, contrato de compraventa o contrato de alquiler del inmueble")
                                                                .obligatorio(true).build(),
                                                RequisitoTramite.builder().nombre("Factura de Luz Anterior")
                                                                .descripcion("Última factura de luz emitida sin deudas pendientes")
                                                                .obligatorio(true).build()))
                                .build());

                // ── 5.4. POLÍTICA 7: Denuncia por Conexión Clandestina ──────
                Nodo p7Start = Nodo.builder()
                                .id("p7n1").nombre("INICIO").tipo("START")
                                .descripcion("Inicio de proceso de denuncia")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .posX(120).posY(80)
                                .conexiones(List.of("p7n2"))
                                .color("#4CAF50").build();

                Nodo p7Recepcion = Nodo.builder()
                                .id("p7n2").nombre("Registrar Denuncia").tipo("TASK")
                                .descripcion("Registrar la ubicación y detalles de la posible conexión irregular")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .tiempoLimiteHoras(8)
                                .posX(120).posY(220)
                                .conexiones(List.of("p7n3"))
                                .camposFormulario(List.of(
                                                campo("direccion_denuncia", "text", "Dirección exacta del hecho", true),
                                                campo("referencias", "textarea", "Puntos de referencia del domicilio",
                                                                true)))
                                .color("#2196F3").build();

                Nodo p7Inspeccion = Nodo.builder()
                                .id("p7n3").nombre("Inspección de Control").tipo("TASK")
                                .descripcion("Visita técnica de campo para verificar la existencia de conexiones clandestinas o manipulación de medidor")
                                .departamento("Dept. Técnico")
                                .responsableId(func2.getId()).nombreResponsable(func2.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(370).posY(220)
                                .conexiones(List.of("p7n4"))
                                .camposFormulario(List.of(
                                                campo("clandestino_verificado", "boolean",
                                                                "¿Se constató la conexión clandestina?", true),
                                                campo("detalles_inspeccion", "textarea",
                                                                "Informe técnico e imágenes del fraude", true)))
                                .color("#2196F3").build();

                Nodo p7Sancion = Nodo.builder()
                                .id("p7n4").nombre("Dictamen de Sanción").tipo("TASK")
                                .descripcion("Cálculo legal de la sanción económica de acuerdo a la normativa vigente")
                                .departamento("Dept. Legal")
                                .responsableId(func3.getId()).nombreResponsable(func3.getNombre())
                                .tiempoLimiteHoras(48)
                                .posX(620).posY(220)
                                .conexiones(List.of("p7n5"))
                                .camposFormulario(List.of(
                                                campo("monto_multa_calculada", "text", "Multa total a cobrar (Bs.)",
                                                                true),
                                                campo("plazo_dias_desconexion", "text",
                                                                "Plazo otorgado para regularización (días)", true),
                                                campo("aprobado", "boolean",
                                                                "¿Procedimiento legal de sanción conforme?", true)))
                                .color("#FF9800").build();

                Nodo p7CorteYCobro = Nodo.builder()
                                .id("p7n5").nombre("Desconexión y Facturación").tipo("TASK")
                                .descripcion("Realizar la desconexión física del suministro clandestino y cargar la multa en el sistema de facturación")
                                .departamento("Finanzas")
                                .responsableId(func5.getId()).nombreResponsable(func5.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(870).posY(220)
                                .conexiones(List.of("p7n6"))
                                .camposFormulario(List.of(
                                                campo("nro_comprobante_multa", "text", "Número de comprobante de deuda",
                                                                true),
                                                campo("corte_realizado", "boolean", "¿Corte técnico realizado?", true)))
                                .color("#9C27B0").build();

                Nodo p7End = Nodo.builder()
                                .id("p7n6").nombre("FIN").tipo("END")
                                .descripcion("Proceso de denuncia y sanción finalizado")
                                .departamento("").responsableId("")
                                .posX(1120).posY(220)
                                .color("#F44336").build();

                politicaRepository.save(Politica.builder()
                                .nombre("Denuncia de Conexiones Clandestinas")
                                .descripcion("Procedimiento para reportar, inspeccionar y sancionar las instalaciones eléctricas clandestinas o manipuladas")
                                .categoria("Seguridad y Control")
                                .organizacion("COTAS Energy")
                                .tiempoEstimadoDias(5)
                                .nodos(new ArrayList<>(List.of(p7Start, p7Recepcion, p7Inspeccion, p7Sancion,
                                                p7CorteYCobro, p7End)))
                                .estado(EstadoPolitica.ACTIVA)
                                .creadoPorId(admin.getId()).nombreCreadoPor(admin.getNombre())
                                .version(1).tramitesActivos(0).tramitesCompletados(0)
                                .fechaCreacion(ahora.minusDays(30))
                                .fechaActualizacion(ahora.minusDays(10))
                                .fechaActivacion(ahora.minusDays(30))
                                .requisitosIniciales(List.of(
                                                RequisitoTramite.builder().nombre("Fotografía de la Infracción")
                                                                .descripcion("Foto clara del poste, medidor manipulado o acometida ilegal (opcional)")
                                                                .obligatorio(false).build()))
                                .build());

                // ── 5.5. POLÍTICA 8: Retiro Definitivo de Suministro Eléctrico ──────
                Nodo p8Start = Nodo.builder()
                                .id("p8n1").nombre("INICIO").tipo("START")
                                .descripcion("Inicio de solicitud de retiro")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .posX(120).posY(80)
                                .conexiones(List.of("p8n2"))
                                .color("#4CAF50").build();

                Nodo p8Solicitud = Nodo.builder()
                                .id("p8n2").nombre("Verificar Requisitos").tipo("TASK")
                                .descripcion("Revisar la solicitud del propietario y corroborar datos de la acometida")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(120).posY(220)
                                .conexiones(List.of("p8n3"))
                                .camposFormulario(List.of(
                                                campo("motivo_retiro", "textarea", "Motivo de la baja definitiva",
                                                                true),
                                                campo("es_propietario", "boolean", "¿Titular/Propietario verificado?",
                                                                true)))
                                .color("#2196F3").build();

                Nodo p8Liquidacion = Nodo.builder()
                                .id("p8n3").nombre("Liquidación de Cuentas").tipo("TASK")
                                .descripcion("Auditar consumos pendientes y emitir liquidación final de saldos")
                                .departamento("Finanzas")
                                .responsableId(func5.getId()).nombreResponsable(func5.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(370).posY(220)
                                .conexiones(List.of("p8n4"))
                                .camposFormulario(List.of(
                                                campo("saldo_final_deuda", "text", "Monto final de liquidación (Bs.)",
                                                                true),
                                                campo("aprobado", "boolean", "¿Cerrado sin deudas?", true)))
                                .color("#9C27B0").build();

                Nodo p8RetiroFisico = Nodo.builder()
                                .id("p8n4").nombre("Retirar Medidor").tipo("TASK")
                                .descripcion("Retirar físicamente el medidor eléctrico y desconectar la acometida aérea")
                                .departamento("Dept. Técnico")
                                .responsableId(func2.getId()).nombreResponsable(func2.getNombre())
                                .tiempoLimiteHoras(48)
                                .posX(620).posY(220)
                                .conexiones(List.of("p8n5"))
                                .camposFormulario(List.of(
                                                campo("lectura_retiro", "text", "Lectura final del medidor retirado",
                                                                true),
                                                campo("medidor_recuperado", "boolean", "¿Medidor retornado a almacén?",
                                                                true)))
                                .color("#2196F3").build();

                Nodo p8BajaContrato = Nodo.builder()
                                .id("p8n5").nombre("Baja de Contrato").tipo("TASK")
                                .descripcion("Anulación del contrato de suministro y archivo digital del expediente")
                                .departamento("Dept. Legal")
                                .responsableId(func3.getId()).nombreResponsable(func3.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(870).posY(220)
                                .conexiones(List.of("p8n6"))
                                .camposFormulario(List.of(
                                                campo("nro_resolucion_baja", "text",
                                                                "Número de resolución administrativa de baja", true),
                                                campo("aprobado", "boolean", "¿Contrato anulado en el sistema?", true)))
                                .color("#FF9800").build();

                Nodo p8End = Nodo.builder()
                                .id("p8n6").nombre("FIN").tipo("END")
                                .descripcion("Suministro dado de baja definitiva")
                                .departamento("").responsableId("")
                                .posX(1120).posY(220)
                                .color("#F44336").build();

                politicaRepository.save(Politica.builder()
                                .nombre("Retiro Definitivo de Suministro")
                                .descripcion("Trámite administrativo y técnico para dar de baja definitiva un punto de suministro de energía eléctrica")
                                .categoria("Trámites Administrativos")
                                .organizacion("COTAS Energy")
                                .tiempoEstimadoDias(5)
                                .nodos(new ArrayList<>(List.of(p8Start, p8Solicitud, p8Liquidacion, p8RetiroFisico,
                                                p8BajaContrato, p8End)))
                                .estado(EstadoPolitica.ACTIVA)
                                .creadoPorId(admin.getId()).nombreCreadoPor(admin.getNombre())
                                .version(1).tramitesActivos(0).tramitesCompletados(0)
                                .fechaCreacion(ahora.minusDays(18))
                                .fechaActualizacion(ahora.minusDays(4))
                                .fechaActivacion(ahora.minusDays(18))
                                .requisitosIniciales(List.of(
                                                RequisitoTramite.builder().nombre("Carta de Solicitud de Baja")
                                                                .descripcion("Nota de solicitud firmada por el propietario en PDF")
                                                                .obligatorio(true).build(),
                                                RequisitoTramite.builder().nombre("Cédula de Identidad").descripcion(
                                                                "Copia del documento de identidad del titular")
                                                                .obligatorio(true).build()))
                                .build());

                // ── 5.6. POLÍTICA 9: Ampliación de Red de Distribución (BORRADOR) ──────
                Nodo p9Start = Nodo.builder()
                                .id("p9n1").nombre("INICIO").tipo("START")
                                .posX(120).posY(80)
                                .conexiones(List.of("p9n2"))
                                .color("#4CAF50").build();

                Nodo p9Estudio = Nodo.builder()
                                .id("p9n2").nombre("Inspección de Factibilidad").tipo("TASK")
                                .descripcion("Estudiar la factibilidad de la red y dimensionamiento de transformadores")
                                .departamento("Dept. Técnico")
                                .responsableId(func2.getId()).nombreResponsable(func2.getNombre())
                                .tiempoLimiteHoras(72)
                                .posX(120).posY(220)
                                .conexiones(List.of("p9n3"))
                                .camposFormulario(List.of(
                                                campo("longitud_extension_m", "text",
                                                                "Extensión estimada de la red (metros)", true),
                                                campo("postes_requeridos", "text",
                                                                "Número de postes de concreto requeridos", true)))
                                .color("#2196F3").build();

                Nodo p9End = Nodo.builder()
                                .id("p9n3").nombre("FIN").tipo("END")
                                .posX(370).posY(220)
                                .color("#F44336").build();

                politicaRepository.save(Politica.builder()
                                .nombre("Ampliación de Red de Distribución")
                                .descripcion("Flujo de ingeniería para estudiar, presupuestar y aprobar la extensión de líneas de distribución de media y baja tensión")
                                .categoria("Proyectos e Ingeniería")
                                .organizacion("COTAS Energy")
                                .tiempoEstimadoDias(15)
                                .nodos(new ArrayList<>(List.of(p9Start, p9Estudio, p9End)))
                                .estado(EstadoPolitica.BORRADOR)
                                .creadoPorId(admin2.getId()).nombreCreadoPor(admin2.getNombre())
                                .version(1).tramitesActivos(0).tramitesCompletados(0)
                                .fechaCreacion(ahora.minusDays(8))
                                .fechaActualizacion(ahora.minusDays(8))
                                .build());

                // ── 5.7. POLÍTICA 10: Instalación de Suministro Trifásico Comercial ──────
                Nodo p10Start = Nodo.builder()
                                .id("p10n1").nombre("INICIO").tipo("START")
                                .descripcion("Inicio de solicitud de medidor comercial")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .posX(120).posY(80)
                                .conexiones(List.of("p10n2"))
                                .color("#4CAF50").build();

                Nodo p10Recepcion = Nodo.builder()
                                .id("p10n2").nombre("Recepción de Requisitos").tipo("TASK")
                                .descripcion("Revisar Licencia de funcionamiento (NIT) y plano eléctrico de carga")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .tiempoLimiteHoras(24)
                                .posX(120).posY(220)
                                .conexiones(List.of("p10n3"))
                                .camposFormulario(List.of(
                                                campo("razon_social", "text", "Razón Social de la empresa", true),
                                                campo("plano_carga_aprobado", "boolean",
                                                                "¿Plano de instalaciones eléctricas firmado?", true)))
                                .color("#2196F3").build();

                Nodo p10Tecnico = Nodo.builder()
                                .id("p10n3").nombre("Factibilidad Técnica Comercial").tipo("TASK")
                                .descripcion("Evaluar factibilidad de potencia de red para soportar carga comercial")
                                .departamento("Dept. Técnico")
                                .responsableId(func2.getId()).nombreResponsable(func2.getNombre())
                                .tiempoLimiteHoras(48)
                                .posX(370).posY(220)
                                .conexiones(List.of("p10n4"))
                                .camposFormulario(List.of(
                                                campo("potencia_factible_kva", "text", "Potencia máxima factible (kVA)",
                                                                true),
                                                campo("aprobado", "boolean", "¿Factible técnicamente?", true)))
                                .color("#2196F3").build();

                Nodo p10Legal = Nodo.builder()
                                .id("p10n4").nombre("Firma de Contrato Comercial").tipo("TASK")
                                .descripcion("Firma del contrato de suministro de media tensión por apoderados")
                                .departamento("Dept. Legal")
                                .responsableId(func3.getId()).nombreResponsable(func3.getNombre())
                                .tiempoLimiteHoras(48)
                                .posX(620).posY(220)
                                .conexiones(List.of("p10n5"))
                                .camposFormulario(List.of(
                                                campo("nro_contrato_comercial", "text",
                                                                "Número de contrato comercial MT", true),
                                                campo("aprobado", "boolean", "¿Representantes legales acreditados?",
                                                                true)))
                                .color("#FF9800").build();

                Nodo p10Ejecucion = Nodo.builder()
                                .id("p10n5").nombre("Instalación de Suministro").tipo("TASK")
                                .descripcion("Montaje físico del medidor trifásico y conexión del transformador comercial en campo")
                                .departamento("Dept. Técnico")
                                .responsableId(func2.getId()).nombreResponsable(func2.getNombre())
                                .tiempoLimiteHoras(72)
                                .posX(870).posY(220)
                                .conexiones(List.of("p10n6"))
                                .camposFormulario(List.of(
                                                campo("nro_serie_medidor", "text",
                                                                "Número de serie del medidor comercial", true),
                                                campo("aprobado", "boolean", "¿Prueba de carga y encendido exitosos?",
                                                                true)))
                                .color("#2196F3").build();

                Nodo p10End = Nodo.builder()
                                .id("p10n6").nombre("FIN").tipo("END")
                                .descripcion("Suministro trifásico comercial activado")
                                .departamento("").responsableId("")
                                .posX(1120).posY(220)
                                .color("#F44336").build();

                politicaRepository.save(Politica.builder()
                                .nombre("Instalación de Suministro Trifásico Comercial")
                                .descripcion("Flujo integral para la solicitud, inspección y montaje de conexiones trifásicas comerciales y de media tensión")
                                .categoria("Servicios Eléctricos")
                                .organizacion("COTAS Energy")
                                .tiempoEstimadoDias(10)
                                .nodos(new ArrayList<>(List.of(p10Start, p10Recepcion, p10Tecnico, p10Legal,
                                                p10Ejecucion, p10End)))
                                .estado(EstadoPolitica.ACTIVA)
                                .creadoPorId(admin.getId()).nombreCreadoPor(admin.getNombre())
                                .version(1).tramitesActivos(0).tramitesCompletados(0)
                                .fechaCreacion(ahora.minusDays(25))
                                .fechaActualizacion(ahora.minusDays(3))
                                .fechaActivacion(ahora.minusDays(25))
                                .requisitosIniciales(List.of(
                                                RequisitoTramite.builder().nombre("Licencia de Funcionamiento")
                                                                .descripcion("Copia legalizada de licencia o NIT de la empresa")
                                                                .obligatorio(true).build(),
                                                RequisitoTramite.builder().nombre("Plano Eléctrico").descripcion(
                                                                "Plano de carga de instalaciones del edificio firmado por un ingeniero eléctrico")
                                                                .obligatorio(true).build(),
                                                RequisitoTramite.builder().nombre("Poder Legal del Representante")
                                                                .descripcion("Poder notariado que acredite las facultades del firmante")
                                                                .obligatorio(true).build()))
                                .build());

                // ── 5.8. POLÍTICA 11: Certificado de No Adeudar ──────
                Nodo p11Start = Nodo.builder()
                                .id("p11n1").nombre("INICIO").tipo("START")
                                .descripcion("Solicitud de certificado")
                                .departamento("Atención al Cliente")
                                .responsableId(func1.getId()).nombreResponsable(func1.getNombre())
                                .posX(120).posY(80)
                                .conexiones(List.of("p11n2"))
                                .color("#4CAF50").build();

                Nodo p11Auditoria = Nodo.builder()
                                .id("p11n2").nombre("Auditoría de Saldos").tipo("TASK")
                                .descripcion("Auditar facturas y depósitos del cliente para certificar saldo cero en su historial")
                                .departamento("Finanzas")
                                .responsableId(func5.getId()).nombreResponsable(func5.getNombre())
                                .tiempoLimiteHoras(16)
                                .posX(120).posY(220)
                                .conexiones(List.of("p11n3"))
                                .camposFormulario(List.of(
                                                campo("saldo_pendiente_bs", "text", "Saldo total adeudado actual",
                                                                true),
                                                campo("aprobado", "boolean",
                                                                "¿Certificado apto para emitir (saldo cero)?", true)))
                                .color("#9C27B0").build();

                Nodo p11Emision = Nodo.builder()
                                .id("p11n3").nombre("Emisión y Firma Digital").tipo("TASK")
                                .descripcion("Firmar digitalmente el documento de no adeudar y enviarlo al repositorio")
                                .departamento("Finanzas")
                                .responsableId(func5.getId()).nombreResponsable(func5.getNombre())
                                .tiempoLimiteHoras(8)
                                .posX(370).posY(220)
                                .conexiones(List.of("p11n4"))
                                .camposFormulario(List.of(
                                                campo("codigo_verificacion_digital", "text",
                                                                "Código QR de verificación digital", true),
                                                campo("aprobado", "boolean", "¿Firmado y archivado?", true)))
                                .color("#9C27B0").build();

                Nodo p11End = Nodo.builder()
                                .id("p11n4").nombre("FIN").tipo("END")
                                .descripcion("Certificado de no adeudar generado")
                                .departamento("").responsableId("")
                                .posX(620).posY(220)
                                .color("#F44336").build();

                politicaRepository.save(Politica.builder()
                                .nombre("Certificado de No Adeudar")
                                .descripcion("Trámite exprés para auditar la cuenta de un cliente y certificar que no tiene deudas pendientes con la empresa")
                                .categoria("Trámites Administrativos")
                                .organizacion("COTAS Energy")
                                .tiempoEstimadoDias(1)
                                .nodos(new ArrayList<>(List.of(p11Start, p11Auditoria, p11Emision, p11End)))
                                .estado(EstadoPolitica.ACTIVA)
                                .creadoPorId(admin.getId()).nombreCreadoPor(admin.getNombre())
                                .version(1).tramitesActivos(0).tramitesCompletados(0)
                                .fechaCreacion(ahora.minusDays(12))
                                .fechaActualizacion(ahora.minusDays(1))
                                .fechaActivacion(ahora.minusDays(12))
                                .requisitosIniciales(List.of(
                                                RequisitoTramite.builder().nombre("Cédula de Identidad").descripcion(
                                                                "Copia del carnet de identidad del titular del servicio")
                                                                .obligatorio(true).build()))
                                .build());

                // ── 6. TRÁMITES — Política 1: Instalación de Medidor ─────

                // datosFormulario para trámites completados
                Map<String, Object> datos001 = new HashMap<>();
                datos001.put("observacion", "Documentación completa y verificada");
                datos001.put("aprobado", "true");
                datos001.put("informe_tecnico", "Factibilidad confirmada - zona residencial, acometida disponible");
                datos001.put("numero_contrato", "CONT-2026-0089");
                datos001.put("materiales", "2x medidor monofásico 10A, 10m cable calibre 10, 1x caja de medidor");
                datos001.put("monto_factura", "850.00");

                Map<String, Object> datos002 = new HashMap<>();
                datos002.put("observacion", "Solicitud procesada sin inconvenientes");
                datos002.put("aprobado", "true");
                datos002.put("informe_tecnico", "Zona comercial - capacidad trifásica disponible, sin restricciones");
                datos002.put("numero_contrato", "CONT-2026-0091");
                datos002.put("materiales", "1x medidor trifásico 30A, 15m cable calibre 8, 1x tablero 4 circuitos");
                datos002.put("monto_factura", "1240.00");

                // TRM-2026-0001: COMPLETADO — cliente1 — todos los pasos completados
                Tramite trm001 = tramiteRepository.save(Tramite.builder()
                                .politicaId(politica1.getId()).nombrePolitica(politica1.getNombre())
                                .clienteId(cliente1.getId()).nombreCliente(cliente1.getNombre())
                                .nodoActualId("p1n7").nombreNodoActual("FIN").departamentoActual("")
                                .estado(EstadoTramite.COMPLETADO)
                                .descripcion("Solicitud de instalación de medidor monofásico para vivienda en Av. Libertad 456")
                                .numeroReferencia("TRM-2026-0001").prioridad("MEDIA")
                                .datosFormulario(datos001)
                                .historial(new ArrayList<>(List.of(
                                                historial("p1n2", "Recibir Solicitud", "Atención al Cliente", func1,
                                                                "COMPLETADO", "Documentación completa y verificada",
                                                                null, 35L, ahora.minusDays(14)),
                                                historial("p1n3", "Evaluación Técnica", "Dept. Técnico", func2,
                                                                "COMPLETADO",
                                                                "Factibilidad confirmada - zona residencial", null,
                                                                120L, ahora.minusDays(12)),
                                                historial("p1n4", "Revisión Legal", "Dept. Legal", func3, "COMPLETADO",
                                                                "Contrato CONT-2026-0089 firmado y registrado", null,
                                                                180L, ahora.minusDays(10)),
                                                historial("p1n5", "Despacho de Materiales", "Almacén", func4,
                                                                "COMPLETADO",
                                                                "Materiales despachados según lista aprobada", null,
                                                                60L, ahora.minusDays(8)),
                                                historial("p1n6", "Facturación", "Finanzas", func5, "COMPLETADO",
                                                                "Factura N° 0089 emitida por Bs. 850.00", null, 25L,
                                                                ahora.minusDays(7)))))
                                .fechaInicio(ahora.minusDays(14))
                                .fechaFin(ahora.minusDays(7))
                                .fechaUltimaActualizacion(ahora.minusDays(7))
                                .duracionMinutos(10080L)
                                .build());

                // TRM-2026-0002: COMPLETADO — cliente2 — todos los pasos completados
                Tramite trm002 = tramiteRepository.save(Tramite.builder()
                                .politicaId(politica1.getId()).nombrePolitica(politica1.getNombre())
                                .clienteId(cliente2.getId()).nombreCliente(cliente2.getNombre())
                                .nodoActualId("p1n7").nombreNodoActual("FIN").departamentoActual("")
                                .estado(EstadoTramite.COMPLETADO)
                                .descripcion("Instalación de medidor trifásico para oficinas de Constructora Norte en Zona Industrial")
                                .numeroReferencia("TRM-2026-0002").prioridad("ALTA")
                                .datosFormulario(datos002)
                                .historial(new ArrayList<>(List.of(
                                                historial("p1n2", "Recibir Solicitud", "Atención al Cliente", func1b,
                                                                "COMPLETADO",
                                                                "Documentación completa, cliente prioritario", null,
                                                                20L, ahora.minusDays(20)),
                                                historial("p1n3", "Evaluación Técnica", "Dept. Técnico", func2b,
                                                                "COMPLETADO",
                                                                "Zona comercial - capacidad trifásica disponible", null,
                                                                95L, ahora.minusDays(18)),
                                                historial("p1n4", "Revisión Legal", "Dept. Legal", func3, "COMPLETADO",
                                                                "Contrato CONT-2026-0091 aprobado", null, 150L,
                                                                ahora.minusDays(16)),
                                                historial("p1n5", "Despacho de Materiales", "Almacén", func4,
                                                                "COMPLETADO", "Materiales trifásicos despachados", null,
                                                                75L, ahora.minusDays(14)),
                                                historial("p1n6", "Facturación", "Finanzas", func5, "COMPLETADO",
                                                                "Factura N° 0091 emitida por Bs. 1240.00", null, 30L,
                                                                ahora.minusDays(13)))))
                                .fechaInicio(ahora.minusDays(20))
                                .fechaFin(ahora.minusDays(13))
                                .fechaUltimaActualizacion(ahora.minusDays(13))
                                .duracionMinutos(10080L)
                                .build());

                // TRM-2026-0003: EN_PROCESO — cliente3 — en Revisión Legal (paso 3)
                Tramite trm003 = tramiteRepository.save(Tramite.builder()
                                .politicaId(politica1.getId()).nombrePolitica(politica1.getNombre())
                                .clienteId(cliente3.getId()).nombreCliente(cliente3.getNombre())
                                .nodoActualId("p1n4").nombreNodoActual("Revisión Legal")
                                .departamentoActual("Dept. Legal")
                                .estado(EstadoTramite.EN_PROCESO)
                                .descripcion("Instalación de medidor trifásico de alta capacidad para Hospital Regional")
                                .numeroReferencia("TRM-2026-0003").prioridad("ALTA")
                                .historial(new ArrayList<>(List.of(
                                                historial("p1n2", "Recibir Solicitud", "Atención al Cliente", func1,
                                                                "COMPLETADO",
                                                                "Documentación hospitalaria verificada y completa",
                                                                null, 40L, ahora.minusDays(5)),
                                                historial("p1n3", "Evaluación Técnica", "Dept. Técnico", func2,
                                                                "COMPLETADO",
                                                                "Factibilidad confirmada - requiere acometida especial",
                                                                null, 180L, ahora.minusDays(3)))))
                                .fechaInicio(ahora.minusDays(5))
                                .fechaUltimaActualizacion(ahora.minusDays(3))
                                .build());

                // TRM-2026-0004: EN_PROCESO — cliente4 — en Evaluación Técnica (paso 2)
                Tramite trm004 = tramiteRepository.save(Tramite.builder()
                                .politicaId(politica1.getId()).nombrePolitica(politica1.getNombre())
                                .clienteId(cliente4.getId()).nombreCliente(cliente4.getNombre())
                                .nodoActualId("p1n3").nombreNodoActual("Evaluación Técnica")
                                .departamentoActual("Dept. Técnico")
                                .estado(EstadoTramite.EN_PROCESO)
                                .descripcion("Medidor para nuevo local de Supermercado El Sol en Av. Comercial 789")
                                .numeroReferencia("TRM-2026-0004").prioridad("MEDIA")
                                .historial(new ArrayList<>(List.of(
                                                historial("p1n2", "Recibir Solicitud", "Atención al Cliente", func1b,
                                                                "COMPLETADO", "Solicitud recibida, documentos en orden",
                                                                null, 25L, ahora.minusDays(2)))))
                                .fechaInicio(ahora.minusDays(2))
                                .fechaUltimaActualizacion(ahora.minusDays(2))
                                .build());

                // TRM-2026-0005: EN_PROCESO — cliente5 — en Despacho de Materiales (paso 4) —
                // CUELLO DE BOTELLA
                Tramite trm005 = tramiteRepository.save(Tramite.builder()
                                .politicaId(politica1.getId()).nombrePolitica(politica1.getNombre())
                                .clienteId(cliente5.getId()).nombreCliente(cliente5.getNombre())
                                .nodoActualId("p1n5").nombreNodoActual("Despacho de Materiales")
                                .departamentoActual("Almacén")
                                .estado(EstadoTramite.EN_PROCESO)
                                .descripcion("Instalación urgente para Residencial Los Pinos — 12 unidades habitacionales")
                                .numeroReferencia("TRM-2026-0005").prioridad("ALTA")
                                .historial(new ArrayList<>(List.of(
                                                historial("p1n2", "Recibir Solicitud", "Atención al Cliente", func1,
                                                                "COMPLETADO", "Solicitud masiva aprobada", null, 30L,
                                                                ahora.minusDays(9)),
                                                historial("p1n3", "Evaluación Técnica", "Dept. Técnico", func2b,
                                                                "COMPLETADO",
                                                                "Factibilidad confirmada para 12 unidades", null, 210L,
                                                                ahora.minusDays(8)),
                                                historial("p1n4", "Revisión Legal", "Dept. Legal", func3, "COMPLETADO",
                                                                "Contratos colectivos aprobados", null, 240L,
                                                                ahora.minusDays(6)))))
                                .fechaInicio(ahora.minusDays(9))
                                .fechaUltimaActualizacion(ahora.minusDays(5))
                                .build());

                // TRM-2026-0006: NUEVO — cliente1 — recién enviado
                Tramite trm006 = tramiteRepository.save(Tramite.builder()
                                .politicaId(politica1.getId()).nombrePolitica(politica1.getNombre())
                                .clienteId(cliente1.getId()).nombreCliente(cliente1.getNombre())
                                .nodoActualId("p1n2").nombreNodoActual("Recibir Solicitud")
                                .departamentoActual("Atención al Cliente")
                                .estado(EstadoTramite.NUEVO)
                                .descripcion("Solicitud de medidor para segunda sucursal de Empresa ABC en Zona Sur")
                                .numeroReferencia("TRM-2026-0006").prioridad("MEDIA")
                                .historial(new ArrayList<>())
                                .fechaInicio(ahora.minusHours(3))
                                .fechaUltimaActualizacion(ahora.minusHours(3))
                                .build());

                // TRM-2026-0007: NUEVO — cliente2 — recién enviado
                Tramite trm007 = tramiteRepository.save(Tramite.builder()
                                .politicaId(politica1.getId()).nombrePolitica(politica1.getNombre())
                                .clienteId(cliente2.getId()).nombreCliente(cliente2.getNombre())
                                .nodoActualId("p1n2").nombreNodoActual("Recibir Solicitud")
                                .departamentoActual("Atención al Cliente")
                                .estado(EstadoTramite.NUEVO)
                                .descripcion("Medidor para nueva obra de Constructora Norte en Residencial Primavera")
                                .numeroReferencia("TRM-2026-0007").prioridad("BAJA")
                                .historial(new ArrayList<>())
                                .fechaInicio(ahora.minusHours(1))
                                .fechaUltimaActualizacion(ahora.minusHours(1))
                                .build());

                // ── 7. TRÁMITES — Política 2: Reconexión de Servicio ─────

                Map<String, Object> datos008 = new HashMap<>();
                datos008.put("monto_deuda", "320.50");
                datos008.put("fecha_pago", "2026-04-15");
                datos008.put("observacion", "Deuda cancelada en su totalidad");
                datos008.put("aprobado", "true");

                Map<String, Object> datos009 = new HashMap<>();
                datos009.put("monto_deuda", "875.00");
                datos009.put("observacion", "Cliente no ha cancelado la deuda pendiente. Reconexión denegada.");
                datos009.put("aprobado", "false");

                // TRM-2026-0008: COMPLETADO — cliente3 — camino VERDADERO (deuda pagada →
                // reconexión)
                Tramite trm008 = tramiteRepository.save(Tramite.builder()
                                .politicaId(politica2.getId()).nombrePolitica(politica2.getNombre())
                                .clienteId(cliente3.getId()).nombreCliente(cliente3.getNombre())
                                .nodoActualId("p2n6").nombreNodoActual("FIN").departamentoActual("")
                                .estado(EstadoTramite.COMPLETADO)
                                .descripcion("Reconexión de servicio eléctrico para Hospital Regional — deuda cancelada")
                                .numeroReferencia("TRM-2026-0008").prioridad("ALTA")
                                .datosFormulario(datos008)
                                .historial(new ArrayList<>(List.of(
                                                historial("p2n2", "Verificar Deuda", "Finanzas", func5, "COMPLETADO",
                                                                "Deuda de Bs. 320.50 cancelada el 15/04/2026", null,
                                                                45L, ahora.minusDays(4)),
                                                historial("p2n3", "¿Deuda pagada?", "Finanzas", func5, "COMPLETADO",
                                                                "Pago verificado en sistema", "APROBADO", 5L,
                                                                ahora.minusDays(4)),
                                                historial("p2n4", "Reconexión Técnica", "Dept. Técnico", func2,
                                                                "COMPLETADO",
                                                                "Reconexión realizada exitosamente en campo", null, 90L,
                                                                ahora.minusDays(3)))))
                                .fechaInicio(ahora.minusDays(4))
                                .fechaFin(ahora.minusDays(3))
                                .fechaUltimaActualizacion(ahora.minusDays(3))
                                .duracionMinutos(1440L)
                                .build());

                // TRM-2026-0009: COMPLETADO — cliente4 — camino FALSO (deuda no pagada →
                // rechazo)
                Tramite trm009 = tramiteRepository.save(Tramite.builder()
                                .politicaId(politica2.getId()).nombrePolitica(politica2.getNombre())
                                .clienteId(cliente4.getId()).nombreCliente(cliente4.getNombre())
                                .nodoActualId("p2n6").nombreNodoActual("FIN").departamentoActual("")
                                .estado(EstadoTramite.COMPLETADO)
                                .descripcion("Solicitud de reconexión de Supermercado El Sol — deuda pendiente")
                                .numeroReferencia("TRM-2026-0009").prioridad("MEDIA")
                                .datosFormulario(datos009)
                                .historial(new ArrayList<>(List.of(
                                                historial("p2n2", "Verificar Deuda", "Finanzas", func5, "COMPLETADO",
                                                                "Deuda de Bs. 875.00 sin cancelar", null, 30L,
                                                                ahora.minusDays(6)),
                                                historial("p2n3", "¿Deuda pagada?", "Finanzas", func5, "COMPLETADO",
                                                                "Deuda no cancelada, se rechaza reconexión",
                                                                "RECHAZADO", 5L, ahora.minusDays(6)),
                                                historial("p2n5", "Notificar Rechazo", "Atención al Cliente", func1,
                                                                "COMPLETADO",
                                                                "Cliente notificado vía correo y llamada telefónica",
                                                                null, 20L, ahora.minusDays(6)))))
                                .fechaInicio(ahora.minusDays(6))
                                .fechaFin(ahora.minusDays(6))
                                .fechaUltimaActualizacion(ahora.minusDays(6))
                                .duracionMinutos(330L)
                                .build());

                // TRM-2026-0010: EN_PROCESO — cliente5 — en Reconexión Técnica (camino true)
                Tramite trm010 = tramiteRepository.save(Tramite.builder()
                                .politicaId(politica2.getId()).nombrePolitica(politica2.getNombre())
                                .clienteId(cliente5.getId()).nombreCliente(cliente5.getNombre())
                                .nodoActualId("p2n4").nombreNodoActual("Reconexión Técnica")
                                .departamentoActual("Dept. Técnico")
                                .estado(EstadoTramite.EN_PROCESO)
                                .descripcion("Reconexión de servicio para Residencial Los Pinos — pago confirmado")
                                .numeroReferencia("TRM-2026-0010").prioridad("MEDIA")
                                .historial(new ArrayList<>(List.of(
                                                historial("p2n2", "Verificar Deuda", "Finanzas", func5, "COMPLETADO",
                                                                "Pago de Bs. 210.00 confirmado en sistema", null, 35L,
                                                                ahora.minusDays(1)),
                                                historial("p2n3", "¿Deuda pagada?", "Finanzas", func5, "COMPLETADO",
                                                                "Deuda cancelada, se procede con reconexión",
                                                                "APROBADO", 5L, ahora.minusDays(1)))))
                                .fechaInicio(ahora.minusDays(1))
                                .fechaUltimaActualizacion(ahora.minusDays(1))
                                .build());

                // TRM-2026-0011: NUEVO — cliente1 — recién enviado
                Tramite trm011 = tramiteRepository.save(Tramite.builder()
                                .politicaId(politica2.getId()).nombrePolitica(politica2.getNombre())
                                .clienteId(cliente1.getId()).nombreCliente(cliente1.getNombre())
                                .nodoActualId("p2n2").nombreNodoActual("Verificar Deuda").departamentoActual("Finanzas")
                                .estado(EstadoTramite.NUEVO)
                                .descripcion("Solicitud de reconexión de Empresa ABC — alega haber realizado el pago")
                                .numeroReferencia("TRM-2026-0011").prioridad("ALTA")
                                .historial(new ArrayList<>())
                                .fechaInicio(ahora.minusHours(4))
                                .fechaUltimaActualizacion(ahora.minusHours(4))
                                .build());

                // TRM-2026-0012: EN_PROCESO — cliente2 — en Verificar Deuda
                Tramite trm012 = tramiteRepository.save(Tramite.builder()
                                .politicaId(politica2.getId()).nombrePolitica(politica2.getNombre())
                                .clienteId(cliente2.getId()).nombreCliente(cliente2.getNombre())
                                .nodoActualId("p2n2").nombreNodoActual("Verificar Deuda").departamentoActual("Finanzas")
                                .estado(EstadoTramite.EN_PROCESO)
                                .descripcion("Reconexión para Constructora Norte — verificando estado de cuenta")
                                .numeroReferencia("TRM-2026-0012").prioridad("MEDIA")
                                .historial(new ArrayList<>())
                                .fechaInicio(ahora.minusHours(6))
                                .fechaUltimaActualizacion(ahora.minusHours(5))
                                .build());

                // ── 8. TAREAS ─────────────────────────────────────────────

                // TRM-0003 → Revisión Legal (EN_PROCESO, func3) — EN_PROCESO (ya empezó)
                tareaRepository.save(Tarea.builder()
                                .tramiteId(trm003.getId()).politicaId(politica1.getId())
                                .nodoId("p1n4").nombreNodo("Revisión Legal").departamento("Dept. Legal")
                                .funcionarioId(func3.getId()).nombreFuncionario(func3.getNombre())
                                .numeroReferenciaTramite("TRM-2026-0003").nombrePolitica(politica1.getNombre())
                                .instrucciones("Revisar aspectos legales y contractuales para instalación en Hospital Regional")
                                .estado(EstadoTarea.EN_PROCESO).prioridad("ALTA")
                                .fechaAsignacion(ahora.minusDays(3))
                                .build());

                // TRM-0004 → Evaluación Técnica (EN_PROCESO, func2) — PENDIENTE
                tareaRepository.save(Tarea.builder()
                                .tramiteId(trm004.getId()).politicaId(politica1.getId())
                                .nodoId("p1n3").nombreNodo("Evaluación Técnica").departamento("Dept. Técnico")
                                .funcionarioId(func2.getId()).nombreFuncionario(func2.getNombre())
                                .numeroReferenciaTramite("TRM-2026-0004").nombrePolitica(politica1.getNombre())
                                .instrucciones("Evaluar viabilidad técnica para local comercial de Supermercado El Sol")
                                .estado(EstadoTarea.PENDIENTE).prioridad("MEDIA")
                                .fechaAsignacion(ahora.minusDays(2))
                                .build());

                // TRM-0005 → Despacho de Materiales (EN_PROCESO, func4) — EN_PROCESO (cuello de
                // botella)
                tareaRepository.save(Tarea.builder()
                                .tramiteId(trm005.getId()).politicaId(politica1.getId())
                                .nodoId("p1n5").nombreNodo("Despacho de Materiales").departamento("Almacén")
                                .funcionarioId(func4.getId()).nombreFuncionario(func4.getNombre())
                                .numeroReferenciaTramite("TRM-2026-0005").nombrePolitica(politica1.getNombre())
                                .instrucciones("Preparar y despachar 12 medidores monofásicos y materiales para Residencial Los Pinos")
                                .estado(EstadoTarea.EN_PROCESO).prioridad("ALTA")
                                .fechaAsignacion(ahora.minusDays(5))
                                .build());

                // TRM-0006 → Recibir Solicitud (NUEVO, func1) — PENDIENTE
                tareaRepository.save(Tarea.builder()
                                .tramiteId(trm006.getId()).politicaId(politica1.getId())
                                .nodoId("p1n2").nombreNodo("Recibir Solicitud").departamento("Atención al Cliente")
                                .funcionarioId(func1.getId()).nombreFuncionario(func1.getNombre())
                                .numeroReferenciaTramite("TRM-2026-0006").nombrePolitica(politica1.getNombre())
                                .instrucciones("Recibir y verificar documentación para segunda sucursal de Empresa ABC")
                                .estado(EstadoTarea.PENDIENTE).prioridad("MEDIA")
                                .fechaAsignacion(ahora.minusHours(3))
                                .build());

                // TRM-0007 → Recibir Solicitud (NUEVO, func1b) — PENDIENTE
                tareaRepository.save(Tarea.builder()
                                .tramiteId(trm007.getId()).politicaId(politica1.getId())
                                .nodoId("p1n2").nombreNodo("Recibir Solicitud").departamento("Atención al Cliente")
                                .funcionarioId(func1b.getId()).nombreFuncionario(func1b.getNombre())
                                .numeroReferenciaTramite("TRM-2026-0007").nombrePolitica(politica1.getNombre())
                                .instrucciones("Recibir documentación para nueva obra de Constructora Norte")
                                .estado(EstadoTarea.PENDIENTE).prioridad("BAJA")
                                .fechaAsignacion(ahora.minusHours(1))
                                .build());

                // TRM-0010 → Reconexión Técnica (EN_PROCESO, func2b) — PENDIENTE
                tareaRepository.save(Tarea.builder()
                                .tramiteId(trm010.getId()).politicaId(politica2.getId())
                                .nodoId("p2n4").nombreNodo("Reconexión Técnica").departamento("Dept. Técnico")
                                .funcionarioId(func2b.getId()).nombreFuncionario(func2b.getNombre())
                                .numeroReferenciaTramite("TRM-2026-0010").nombrePolitica(politica2.getNombre())
                                .instrucciones("Realizar reconexión física del servicio eléctrico en Residencial Los Pinos")
                                .estado(EstadoTarea.PENDIENTE).prioridad("MEDIA")
                                .fechaAsignacion(ahora.minusDays(1))
                                .build());

                // TRM-0011 → Verificar Deuda (NUEVO, func5) — PENDIENTE
                tareaRepository.save(Tarea.builder()
                                .tramiteId(trm011.getId()).politicaId(politica2.getId())
                                .nodoId("p2n2").nombreNodo("Verificar Deuda").departamento("Finanzas")
                                .funcionarioId(func5.getId()).nombreFuncionario(func5.getNombre())
                                .numeroReferenciaTramite("TRM-2026-0011").nombrePolitica(politica2.getNombre())
                                .instrucciones("Verificar estado de deuda de Empresa ABC y confirmar pago alegado")
                                .estado(EstadoTarea.PENDIENTE).prioridad("ALTA")
                                .fechaAsignacion(ahora.minusHours(4))
                                .build());

                // TRM-0012 → Verificar Deuda (EN_PROCESO, func5) — EN_PROCESO
                tareaRepository.save(Tarea.builder()
                                .tramiteId(trm012.getId()).politicaId(politica2.getId())
                                .nodoId("p2n2").nombreNodo("Verificar Deuda").departamento("Finanzas")
                                .funcionarioId(func5.getId()).nombreFuncionario(func5.getNombre())
                                .numeroReferenciaTramite("TRM-2026-0012").nombrePolitica(politica2.getNombre())
                                .instrucciones("Verificar estado de cuenta de Constructora Norte en sistema de facturación")
                                .estado(EstadoTarea.EN_PROCESO).prioridad("MEDIA")
                                .fechaAsignacion(ahora.minusHours(6))
                                .build());

                // Tarea adicional para func2b — Evaluación Técnica TRM-0003 (ya completada,
                // referencia)
                tareaRepository.save(Tarea.builder()
                                .tramiteId(trm003.getId()).politicaId(politica1.getId())
                                .nodoId("p1n3").nombreNodo("Evaluación Técnica").departamento("Dept. Técnico")
                                .funcionarioId(func2b.getId()).nombreFuncionario(func2b.getNombre())
                                .numeroReferenciaTramite("TRM-2026-0003").nombrePolitica(politica1.getNombre())
                                .instrucciones("Apoyo en evaluación técnica para instalación en Hospital Regional")
                                .estado(EstadoTarea.PENDIENTE).prioridad("ALTA")
                                .fechaAsignacion(ahora.minusDays(3))
                                .build());

                // ── 9. NOTIFICACIONES ─────────────────────────────────────

                // Para funcionarios — NUEVA_TAREA (no leídas)
                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(func3.getId())
                                .mensaje("Nueva tarea asignada: Revisión Legal — Trámite TRM-2026-0003 (Hospital Regional)")
                                .tipo("NUEVA_TAREA").referenciaId(trm003.getId()).tipoReferencia("TRAMITE")
                                .leida(false).fechaCreacion(ahora.minusDays(3)).build());

                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(func2.getId())
                                .mensaje("Nueva tarea asignada: Evaluación Técnica — Trámite TRM-2026-0004 (Supermercado El Sol)")
                                .tipo("NUEVA_TAREA").referenciaId(trm004.getId()).tipoReferencia("TRAMITE")
                                .leida(false).fechaCreacion(ahora.minusDays(2)).build());

                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(func1.getId())
                                .mensaje("Nueva tarea asignada: Recibir Solicitud — Trámite TRM-2026-0006 (Empresa ABC sucursal)")
                                .tipo("NUEVA_TAREA").referenciaId(trm006.getId()).tipoReferencia("TRAMITE")
                                .leida(false).fechaCreacion(ahora.minusHours(3)).build());

                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(func1b.getId())
                                .mensaje("Nueva tarea asignada: Recibir Solicitud — Trámite TRM-2026-0007 (Constructora Norte)")
                                .tipo("NUEVA_TAREA").referenciaId(trm007.getId()).tipoReferencia("TRAMITE")
                                .leida(false).fechaCreacion(ahora.minusHours(1)).build());

                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(func2b.getId())
                                .mensaje("Nueva tarea asignada: Reconexión Técnica — Trámite TRM-2026-0010 (Residencial Los Pinos)")
                                .tipo("NUEVA_TAREA").referenciaId(trm010.getId()).tipoReferencia("TRAMITE")
                                .leida(false).fechaCreacion(ahora.minusDays(1)).build());

                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(func5.getId())
                                .mensaje("Nueva tarea asignada: Verificar Deuda — Trámite TRM-2026-0011 (Empresa ABC)")
                                .tipo("NUEVA_TAREA").referenciaId(trm011.getId()).tipoReferencia("TRAMITE")
                                .leida(false).fechaCreacion(ahora.minusHours(4)).build());

                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(func5.getId())
                                .mensaje("Nueva tarea asignada: Verificar Deuda — Trámite TRM-2026-0012 (Constructora Norte)")
                                .tipo("NUEVA_TAREA").referenciaId(trm012.getId()).tipoReferencia("TRAMITE")
                                .leida(false).fechaCreacion(ahora.minusHours(6)).build());

                // Para clientes — TRAMITE_COMPLETADO
                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(cliente1.getId())
                                .mensaje("Su trámite TRM-2026-0001 ha sido completado exitosamente. Medidor instalado.")
                                .tipo("TRAMITE_COMPLETADO").referenciaId(trm001.getId()).tipoReferencia("TRAMITE")
                                .leida(true).fechaCreacion(ahora.minusDays(7)).fechaLeida(ahora.minusDays(7)).build());

                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(cliente2.getId())
                                .mensaje("Su trámite TRM-2026-0002 ha sido completado exitosamente. Medidor trifásico instalado.")
                                .tipo("TRAMITE_COMPLETADO").referenciaId(trm002.getId()).tipoReferencia("TRAMITE")
                                .leida(true).fechaCreacion(ahora.minusDays(13)).fechaLeida(ahora.minusDays(12))
                                .build());

                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(cliente3.getId())
                                .mensaje("Su trámite TRM-2026-0008 ha sido completado. Servicio reconectado exitosamente.")
                                .tipo("TRAMITE_COMPLETADO").referenciaId(trm008.getId()).tipoReferencia("TRAMITE")
                                .leida(false).fechaCreacion(ahora.minusDays(3)).build());

                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(cliente4.getId())
                                .mensaje("Su solicitud TRM-2026-0009 fue rechazada. Deuda pendiente de Bs. 875.00. Contáctenos para regularizar.")
                                .tipo("TRAMITE_COMPLETADO").referenciaId(trm009.getId()).tipoReferencia("TRAMITE")
                                .leida(true).fechaCreacion(ahora.minusDays(6)).fechaLeida(ahora.minusDays(5)).build());

                // Para admin — CUELLO_BOTELLA
                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(admin.getId())
                                .mensaje("⚠️ Cuello de botella detectado: TRM-2026-0005 lleva 5 días en 'Despacho de Materiales' (Almacén). Requiere atención inmediata.")
                                .tipo("CUELLO_BOTELLA").referenciaId(trm005.getId()).tipoReferencia("TRAMITE")
                                .leida(false).fechaCreacion(ahora).build());

                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(admin.getId())
                                .mensaje("Resumen diario: 5 trámites EN_PROCESO, 4 trámites NUEVOS pendientes de asignación")
                                .tipo("SISTEMA").referenciaId(null).tipoReferencia("SISTEMA")
                                .leida(true).fechaCreacion(ahora.minusDays(1)).fechaLeida(ahora.minusDays(1)).build());

                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(admin2.getId())
                                .mensaje("Nueva política 'Cambio de Medidor por Avería' creada en estado BORRADOR. Pendiente de revisión.")
                                .tipo("SISTEMA").referenciaId(null).tipoReferencia("SISTEMA")
                                .leida(false).fechaCreacion(ahora.minusDays(5)).build());

                // Notificación leída para func4 (cuello de botella previo)
                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(func4.getId())
                                .mensaje("⚠️ Tarea vencida: Despacho de Materiales — TRM-2026-0005 superó el límite de 48 horas")
                                .tipo("TAREA_VENCIDA").referenciaId(trm005.getId()).tipoReferencia("TRAMITE")
                                .leida(true).fechaCreacion(ahora.minusDays(3)).fechaLeida(ahora.minusDays(3)).build());

                notificacionRepository.save(Notificacion.builder()
                                .usuarioId(func2b.getId())
                                .mensaje("Nueva tarea asignada: Evaluación Técnica — Trámite TRM-2026-0003 (Hospital Regional)")
                                .tipo("NUEVA_TAREA").referenciaId(trm003.getId()).tipoReferencia("TRAMITE")
                                .leida(true).fechaCreacion(ahora.minusDays(3)).fechaLeida(ahora.minusDays(2)).build());

                // ── 9. GENERAR TAREAS COMPLETADAS A PARTIR DEL HISTORIAL DE TRÁMITES ──
                List<Tramite> todosLosTramites = tramiteRepository.findAll();
                for (Tramite tramite : todosLosTramites) {
                        if (tramite.getHistorial() != null) {
                                for (HistorialTramite hist : tramite.getHistorial()) {
                                        tareaRepository.save(Tarea.builder()
                                                        .tramiteId(tramite.getId())
                                                        .politicaId(tramite.getPoliticaId())
                                                        .nodoId(hist.getNodoId())
                                                        .nombreNodo(hist.getNombreNodo())
                                                        .departamento(hist.getDepartamento())
                                                        .funcionarioId(hist.getFuncionarioId())
                                                        .nombreFuncionario(hist.getNombreFuncionario())
                                                        .numeroReferenciaTramite(tramite.getNumeroReferencia())
                                                        .nombrePolitica(tramite.getNombrePolitica())
                                                        .instrucciones("Completado durante la simulación")
                                                        .estado(EstadoTarea.COMPLETADO)
                                                        .prioridad("MEDIA")
                                                        .duracionMinutos(hist.getDuracionMinutos())
                                                        .fechaAsignacion(hist.getFecha() != null
                                                                        ? (hist.getDuracionMinutos() != null ? hist
                                                                                        .getFecha()
                                                                                        .minusMinutes(hist
                                                                                                        .getDuracionMinutos())
                                                                                        : hist.getFecha().minusHours(1))
                                                                        : LocalDateTime.now().minusHours(1))
                                                        .fechaCompletado(hist.getFecha() != null ? hist.getFecha()
                                                                        : LocalDateTime.now())
                                                        .build());
                                }
                        }
                }

                // ── 10. RESPUESTA ─────────────────────────────────────────
                Map<String, Object> credenciales = new LinkedHashMap<>();
                credenciales.put("admin", "admin@workflow.com / 123456");
                credenciales.put("admin2", "admin2@workflow.com / 123456");
                credenciales.put("funcionarios", List.of("juan@workflow.com", "lucia@workflow.com", "ana@workflow.com",
                                "roberto@workflow.com", "maria@workflow.com", "pedro@workflow.com",
                                "elena@workflow.com"));
                credenciales.put("clientes",
                                List.of("cliente@workflow.com", "constructora@workflow.com", "hospital@workflow.com",
                                                "supermercado@workflow.com", "residencial@workflow.com",
                                                "industria@workflow.com"));
                credenciales.put("password_todos", "123456");

                Map<String, Object> resumen = new LinkedHashMap<>();
                resumen.put("departamentos", 5);
                resumen.put("usuarios", 15);
                resumen.put("politicas", 11);
                resumen.put("tramites", 12);
                resumen.put("tareas_activas", 10);
                resumen.put("notificaciones", 18);

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("mensaje", "✅ Datos de demostración cargados exitosamente");
                response.put("credenciales", credenciales);
                response.put("resumen", resumen);
                return ResponseEntity.ok(response);
        }

        @DeleteMapping("/clear")
        public ResponseEntity<String> clearAll() {
                tareaRepository.deleteAll();
                notificacionRepository.deleteAll();
                tramiteRepository.deleteAll();
                politicaRepository.deleteAll();
                usuarioRepository.deleteAll();
                departamentoRepository.deleteAll();
                return ResponseEntity.ok("🗑️ Todos los datos eliminados");
        }

        // ── Helpers privados ─────────────────────────────────────────

        private Usuario crearUsuario(String nombre, String email, RolUsuario rol, String departamento) {
                Usuario u = new Usuario();
                u.setNombre(nombre);
                u.setEmail(email);
                u.setPassword(passwordEncoder.encode("123456"));
                u.setRol(rol);
                u.setDepartamento(departamento);
                u.setActivo(true);
                u.setFechaCreacion(LocalDateTime.now());
                return u;
        }

        private Map<String, Object> campo(String nombre, String tipo, String etiqueta, boolean requerido) {
                Map<String, Object> c = new HashMap<>();
                c.put("nombre", nombre);
                c.put("tipo", tipo);
                c.put("etiqueta", etiqueta);
                c.put("requerido", requerido);
                return c;
        }

        private HistorialTramite historial(String nodoId, String nombreNodo, String departamento,
                        Usuario funcionario, String accion, String observacion,
                        String resultadoDecision, Long duracion, LocalDateTime fecha) {
                return HistorialTramite.builder()
                                .nodoId(nodoId).nombreNodo(nombreNodo).departamento(departamento)
                                .funcionarioId(funcionario.getId()).nombreFuncionario(funcionario.getNombre())
                                .accion(accion).observacion(observacion)
                                .resultadoDecision(resultadoDecision)
                                .duracionMinutos(duracion).fecha(fecha)
                                .build();
        }
}
