# Implementation Plan: Workflow Management System

## Overview

Incremental implementation of the missing and incomplete components in the existing Spring Boot backend. The codebase already has core models, repositories, and basic controllers. Tasks focus on filling gaps identified in the design: persistent NumeroReferencia generation, Departamento management, Trámite initiation separation, required-field validation, PDF generation, analytics, centralized notifications, collaborative WebSocket editing, and property-based tests.

## Tasks

- [x] 1. Add jqwik dependency and fix GlobalExceptionHandler for DuplicateKeyException
  - Add `net.jqwik:jqwik:1.8.4` (test scope) to `pom.xml`
  - Add `com.github.librepdf:openpdf:1.3.30` to `pom.xml` for PDF generation
  - Add handler for `org.springframework.dao.DuplicateKeyException` in `GlobalExceptionHandler` returning HTTP 409
  - Add handler for `MethodArgumentNotValidException` returning HTTP 422
  - _Requirements: 3.3, 14.2, 16.4_

- [x] 2. Implement persistent NumeroReferencia generation
  - [x] 2.1 Create `SequenceService` using `MongoTemplate.findAndModify()` with `$inc` on a `sequences` collection keyed by year
    - Replace the in-memory `AtomicLong` counter in `TramiteService` with a call to `SequenceService.nextVal(year)`
    - Ensure the generated reference matches pattern `TRM-YYYY-NNNN`
    - _Requirements: 14.1, 14.2_

  - [x] 2.2 Write property test for NumeroReferencia uniqueness
    - **Property 5: NumeroReferencia Uniqueness**
    - **Validates: Requirements 14.2**
    - Generate N ≥ 2 sequential calls to `SequenceService.nextVal(year)` and assert all values are distinct

  - [x] 2.3 Write property test for NumeroReferencia format invariant
    - **Property 4: New Trámite State and Reference Invariant**
    - **Validates: Requirements 6.2, 14.1**
    - Assert generated reference matches regex `TRM-\d{4}-\d{4}`

- [x] 3. Implement Departamento management
  - [x] 3.1 Create `Departamento` model (`@Document(collection = "departamentos")`) with fields: `id`, `nombre` (unique index), `descripcion`, `responsableId`, `fechaCreacion`
    - Create `DepartamentoRepository` extending `MongoRepository<Departamento, String>`
    - _Requirements: 3.1_

  - [x] 3.2 Create `DepartamentoService` with CRUD methods
    - `delete()` must call `usuarioRepository.findByDepartamento(nombre)` and throw `BusinessException` if active users are assigned
    - _Requirements: 3.1, 3.4_

  - [x] 3.3 Create `DepartamentoController` at `/api/departamentos`
    - `GET /api/departamentos` — ADMIN, FUNCIONARIO
    - `POST /api/departamentos` — ADMIN
    - `PUT /api/departamentos/{id}` — ADMIN
    - `DELETE /api/departamentos/{id}` — ADMIN
    - _Requirements: 3.1_

  - [x] 3.4 Write unit tests for `DepartamentoService`
    - Test delete rejection when active users are assigned
    - Test successful delete when no users assigned
    - _Requirements: 3.1, 3.4_

- [x] 4. Fix AuthService and UsuarioService to use proper exceptions and unique email enforcement
  - [x] 4.1 Update `AuthService.login()` to throw `ResourceNotFoundException` instead of `RuntimeException`
    - Update `AuthService.register()` to throw `BusinessException` (HTTP 400) or rely on MongoDB unique index (HTTP 409 via `DuplicateKeyException` handler added in task 1)
    - Update `UsuarioService.getById()` to throw `ResourceNotFoundException`
    - _Requirements: 1.2, 3.3_

  - [x] 4.2 Write property test for unique email invariant
    - **Property 14: Unique Email Invariant**
    - **Validates: Requirements 3.3**
    - For any sequence of register requests with the same email, assert only the first succeeds and subsequent ones return HTTP 409

- [x] 5. Implement Trámite initiation separation (NUEVO → EN_PROCESO)
  - [x] 5.1 Refactor `TramiteService.iniciarTramite()` to create Trámite in `NUEVO` state without creating the first Tarea
    - Send WebSocket notification to `/topic/admin` after creation
    - _Requirements: 7.1, 6.2_

  - [x] 5.2 Add `TramiteService.iniciarPorAdmin(String tramiteId)` method
    - Validate `estado == NUEVO`, throw `BusinessException` if not
    - Transition to `EN_PROCESO`, create first Tarea via `WorkflowEngineService` or inline
    - Send WebSocket notification to the assigned Funcionario
    - _Requirements: 7.2, 7.3_

  - [x] 5.3 Add `PUT /api/tramites/{id}/iniciar` endpoint to `TramiteController` (ADMIN only)
    - Add `GET /api/tramites/referencia/{ref}` endpoint for NumeroReferencia lookup
    - _Requirements: 7.2, 14.3_

  - [x] 5.4 Write property test for new Trámite state invariant
    - **Property 4: New Trámite State and Reference Invariant**
    - **Validates: Requirements 6.2, 14.1**
    - Assert that after `iniciarTramite()` the Trámite has `estado = NUEVO` and a non-null `numeroReferencia`

- [x] 6. Implement required-field validation in WorkflowEngineService
  - [x] 6.1 Add `validarCamposRequeridos(Nodo nodo, Map<String, Object> datos)` private method to `WorkflowEngineService`
    - Iterate `nodo.getCamposFormulario()`, check `requerido = true` fields are present and non-blank in `datos`
    - Throw `BusinessException("Campo requerido faltante: " + nombre)` on violation
    - Call this method inside `completarTarea()` before marking the Tarea as `COMPLETADO`
    - _Requirements: 9.7_

  - [x] 6.2 Write property test for required form field validation
    - **Property 12: Required Form Field Validation**
    - **Validates: Requirements 9.7**
    - For any Nodo with at least one required field, assert that submitting form data missing that field throws `BusinessException` and does NOT advance the Trámite

- [x] 7. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement centralized NotificacionService
  - [x] 8.1 Create `NotificacionService` that centralizes notification persistence and WebSocket dispatch
    - Move the inline `enviarNotificacion()` logic from `WorkflowEngineService` into `NotificacionService`
    - Add methods: `notificarFuncionario()`, `notificarCliente()`, `notificarAdmin()`, `notificarDepartamento()`
    - Inject `NotificacionService` into `WorkflowEngineService` and `TramiteService`, removing duplicated logic
    - _Requirements: 10.1, 10.2, 10.3, 10.4_

  - [x] 8.2 Add WebSocket notification to `/topic/departamento/{dept}` when a Tarea is updated within a department
    - Update `TareaController.completar()` to trigger department-level notification via `NotificacionService`
    - _Requirements: 10.2_

  - [x] 8.3 Write unit tests for `NotificacionService`
    - Test that each notification type persists a `Notificacion` document with correct fields
    - Test that WebSocket dispatch is called with correct topic destination
    - _Requirements: 10.4_

- [x] 9. Add task state update endpoint and department task query
  - [x] 9.1 Add `PUT /api/tareas/{id}/estado` endpoint to `TareaController`
    - Accept `{ "estado": "EN_PROCESO" | "RECHAZADO" }` in request body
    - Update `TareaService` with `actualizarEstado(String tareaId, EstadoTarea nuevoEstado)` method
    - Trigger department WebSocket notification via `NotificacionService`
    - _Requirements: 9.4_

  - [x] 9.2 Add `GET /api/tareas/departamento/{dept}` endpoint to `TareaController`
    - Add `findByDepartamento(String departamento)` to `TareaRepository`
    - _Requirements: 9.1_

- [x] 10. Implement AnalyticsService and AnalyticsController
  - [x] 10.1 Create `AnalyticsService` with three methods:
    - `promediosPorNodo(String politicaId)` — average `duracionMinutos` per `nodoId` for COMPLETADO Tareas of that policy
    - `promediosPorDepartamento()` — average `duracionMinutos` per `departamento`
    - `eficienciaFuncionarios(String departamento)` — per-funcionario average within a department, sorted slowest→fastest
    - _Requirements: 13.1, 13.3, 13.4_

  - [x] 10.2 Create `AnalyticsController` at `/api/analytics` (ADMIN only)
    - `GET /api/analytics/nodos/{politicaId}`
    - `GET /api/analytics/departamentos`
    - `GET /api/analytics/funcionarios/{dept}`
    - _Requirements: 13.3_

  - [x] 10.3 Write unit tests for `AnalyticsService`
    - Test average computation with known duration values
    - Test empty result when no completed Tareas exist
    - _Requirements: 13.1_

- [x] 11. Implement bottleneck detection alert in AIService
  - [x] 11.1 Update `AIService.detectarCuellosBottella()` to compare average `duracionMinutos` per Nodo against `nodo.tiempoLimiteHoras * 60`
    - When threshold exceeded, call `NotificacionService.notificarAdmin()` with type `CUELLO_BOTELLA`
    - _Requirements: 13.2, 13.5_

  - [x] 11.2 Write property test for bottleneck detection threshold
    - **Property 13: Bottleneck Detection Threshold**
    - **Validates: Requirements 13.2**
    - For any Nodo with `tiempoLimiteHoras = T`, assert alert is generated when average > T×60 and NOT generated when average ≤ T×60

- [x] 12. Implement PdfGeneratorService and PdfController
  - [x] 12.1 Create `PdfGeneratorService` using OpenPDF
    - `generarPdfTramite(Tramite tramite)` returns `byte[]`
    - Validate `estado == COMPLETADO`, throw `BusinessException` if not
    - Build PDF with: header (title + reference), metadata table (policy, client, dates, duration), historial table (step, dept, funcionario, action, date, duration), datosFormulario key-value section
    - _Requirements: 15.1_

  - [x] 12.2 Create `PdfController` at `/api/pdf`
    - `GET /api/pdf/tramite/{id}` — CLIENTE (own trámite) or ADMIN
    - Validate ownership: if caller is CLIENTE, assert `tramite.clienteId == principal.name` (resolved from JWT), else HTTP 403
    - Return `ResponseEntity<byte[]>` with `Content-Type: application/pdf` and `Content-Disposition: attachment; filename=tramite-{ref}.pdf`
    - _Requirements: 15.2, 15.3, 15.4_

  - [x] 12.3 Write unit tests for `PdfGeneratorService`
    - Test that PDF byte array is non-empty for a valid completed Trámite
    - Test that `BusinessException` is thrown for non-COMPLETADO Trámite
    - _Requirements: 15.1, 15.3_

- [x] 13. Implement collaborative diagram editing WebSocket handler
  - [x] 13.1 Create `DiagramaCambioDTO` record with fields: `tipo`, `editorId`, `nodo`, `desdeId`, `hastaId`, `posX`, `posY`
    - Create `DiagramaWebSocketController` with `@MessageMapping("/politica/{id}/editar")` and `@SendTo("/topic/politica/{id}")`
    - _Requirements: 4.9_

  - [x] 13.2 Write unit tests for `DiagramaWebSocketController`
    - Test that a received `DiagramaCambioDTO` is broadcast back unchanged to the topic
    - _Requirements: 4.9_

- [x] 14. Implement workflow engine property-based tests
  - [x] 14.1 Write property test for workflow routing correctness — sequential
    - **Property 6: Workflow Routing Correctness — Sequential**
    - **Validates: Requirements 8.1, 8.4, 8.7**
    - Build a Política with a linear chain of N TASK Nodos (N ≥ 2), complete each Tarea in sequence, assert Trámite reaches `COMPLETADO` and `historial.size() == N`

  - [x] 14.2 Write property test for DECISION routing correctness
    - **Property 7: DECISION Routing Correctness**
    - **Validates: Requirements 8.2**
    - For a Política with a DECISION Nodo, assert `aprobado=true` routes to `conexiones[0]` and `aprobado=false` routes to `conexiones[1]`

  - [x] 14.3 Write property test for historial append-only invariant
    - **Property 8: Historial Append-Only Invariant**
    - **Validates: Requirements 8.5**
    - After each Tarea completion, assert `historial.size()` is strictly greater than before and no prior entry is mutated

  - [x] 14.4 Write property test for form data accumulation
    - **Property 9: Form Data Accumulation**
    - **Validates: Requirements 8.6, 17.5**
    - After N Tarea completions each with distinct form keys, assert `datosFormulario` contains all keys from all completed Tareas

  - [x] 14.5 Write property test for completed task rejection (idempotence)
    - **Property 10: Completed Task Rejection**
    - **Validates: Requirements 8.8**
    - Assert that calling `completarTarea()` twice on the same Tarea throws `BusinessException` on the second call and does NOT add a second `HistorialTramite` entry

  - [x] 14.6 Write property test for Trámite duration positivity
    - **Property 11: Trámite Duration Positivity**
    - **Validates: Requirements 8.4**
    - For any completed Trámite, assert `duracionMinutos > 0` and equals elapsed minutes between `fechaInicio` and `fechaFin`

- [x] 15. Implement authentication and policy property-based tests
  - [x] 15.1 Write property test for JWT round-trip
    - **Property 1: JWT Round-Trip**
    - **Validates: Requirements 1.1, 1.7**
    - For any `(email, rol)` pair, assert `JwtUtil.extractEmail(generateToken(email, rol)) == email` and `extractRol(...) == rol`

  - [x] 15.2 Write property test for new Política always starts in BORRADOR
    - **Property 2: New Política Always Starts in BORRADOR**
    - **Validates: Requirements 5.1**
    - For any valid Política creation input, assert `estado == BORRADOR` and `version == 1` after `PoliticaService.create()`

  - [x] 15.3 Write property test for workflow graph validity invariant
    - **Property 3: Workflow Graph Validity Invariant**
    - **Validates: Requirements 4.10, 5.2**
    - For any Política graph missing a START or END Nodo, assert `PoliticaService.activar()` throws `BusinessException`; for graphs with exactly one START and at least one END, assert activation succeeds

- [x] 16. Checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 17. Wire SecurityConfig to protect new endpoints
  - Update `SecurityConfig` to add authorization rules for new endpoints:
    - `/api/departamentos/**` — ADMIN (write), ADMIN+FUNCIONARIO (read)
    - `/api/analytics/**` — ADMIN only
    - `/api/pdf/**` — authenticated (ownership enforced in controller)
    - `/api/tramites/{id}/iniciar` (PUT) — ADMIN only
    - `/api/tareas/departamento/**` — ADMIN+FUNCIONARIO
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 18. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- The existing `WorkflowEngineService`, `TramiteService`, `PoliticaService`, `AuthService`, and all models/repositories are already in place — tasks build on top of them
- Property tests use jqwik (added in task 1) and are tagged with the property number from the requirements document
- Checkpoints at tasks 7, 16, and 18 ensure incremental validation
