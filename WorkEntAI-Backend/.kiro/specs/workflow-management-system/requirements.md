# Requirements Document — Workflow Management System

## Introduction

The Workflow Management System (WMS) is a generic, web-based platform that enables organizations
(CRE, COTAS, banks, universities, etc.) to design, publish, and execute business process policies
(políticas de negocio) modeled as UML 2.5 Activity Diagrams with Swimlanes. The system supports
three user roles — Administrator, Funcionario (Employee), and Cliente (Client) — and provides a
real-time collaborative workflow editor, an automated workflow engine, an employee task dashboard,
a client portal, and an AI-powered assistant layer. The backend is implemented in Spring Boot with
MongoDB; the frontend in Angular with Socket.io; and a Flutter mobile companion app is also
required. Deployment targets Docker on AWS or Google Cloud.

---

## Glossary

- **WMS**: Workflow Management System — the system described in this document.
- **Política / Business Policy**: A named, versioned workflow definition composed of Nodos and
  connections, modeled as a UML 2.5 Activity Diagram with Swimlanes. Stored as `Politica` in the
  database.
- **Trámite**: A single execution instance of a Política, initiated by a Cliente. Corresponds to
  the `Tramite` document in MongoDB.
- **Nodo**: A node in the Activity Diagram. Types: `START`, `END`, `TASK`, `DECISION`, `PARALLEL`.
  Embedded in `Politica.nodos`.
- **Tarea**: A work item assigned to a Funcionario when a Trámite reaches a TASK Nodo. Stored as
  `Tarea` in MongoDB.
- **Departamento**: An organizational unit (swimlane) grouping Funcionarios and Nodos.
- **Funcionario**: An employee user (`RolUsuario.FUNCIONARIO`) assigned to one Departamento.
- **Cliente**: An end-user (`RolUsuario.CLIENTE`) who requests and tracks Trámites.
- **Administrador**: A power user (`RolUsuario.ADMIN`) who designs Políticas, manages users, and
  initiates Trámites on behalf of Clientes.
- **Motor de Workflow / Workflow Engine**: The `WorkflowEngineService` that automatically advances
  a Trámite from Nodo to Nodo upon Tarea completion.
- **EstadoPolitica**: Lifecycle state of a Política — `BORRADOR`, `ACTIVA`, `INACTIVA`.
- **EstadoTramite**: Lifecycle state of a Trámite — `NUEVO`, `EN_PROCESO`, `COMPLETADO`,
  `RECHAZADO`.
- **EstadoTarea**: Lifecycle state of a Tarea — `PENDIENTE`, `EN_PROCESO`, `COMPLETADO`,
  `RECHAZADO`.
- **Historial**: An append-only list of `HistorialTramite` entries recording each completed Nodo
  within a Trámite.
- **NumeroReferencia**: A human-readable unique identifier for a Trámite, formatted `TRM-YYYY-NNNN`.
- **JWT**: JSON Web Token used for stateless authentication.
- **WebSocket**: Persistent bidirectional connection (Socket.io) used for real-time push
  notifications.
- **GoJS**: Third-party JavaScript diagramming library used for the visual workflow editor.
- **AI_Copilot**: The AI assistant component powered by OpenAI API that supports workflow design,
  form filling, and bottleneck analysis.
- **PDF_Generator**: The component responsible for rendering completed Trámite data as a
  downloadable PDF document.
- **Semáforo**: Traffic-light color coding for task urgency — 🔴 PENDIENTE/urgent, 🟡 EN_PROCESO,
  🟢 COMPLETADO.

---

## Requirements

### Requirement 1: User Authentication and Session Management

**User Story:** As any user (Administrador, Funcionario, or Cliente), I want to authenticate
securely and manage my session, so that only authorized users can access the system.

#### Acceptance Criteria

1. WHEN a user submits valid credentials (email and password), THE WMS SHALL return a signed JWT
   access token and the authenticated user's profile (id, nombre, email, rol, departamento).
2. WHEN a user submits invalid credentials, THE WMS SHALL return an HTTP 401 response with a
   descriptive error message and SHALL NOT issue a token.
3. WHEN a request arrives with an expired JWT, THE WMS SHALL return an HTTP 401 response and SHALL
   NOT process the request.
4. WHEN a request arrives with a tampered or malformed JWT, THE WMS SHALL return an HTTP 401
   response and SHALL NOT process the request.
5. WHEN a user ends their session, THE WMS SHALL invalidate the client-side token and redirect the
   user to the login screen.
6. THE WMS SHALL store passwords using a one-way cryptographic hash (bcrypt) and SHALL NOT store
   plaintext passwords.
7. WHILE a user is authenticated, THE WMS SHALL include the user's `RolUsuario` in every JWT claim
   so that role-based access control can be enforced on every request.

---

### Requirement 2: Role-Based Access Control (RBAC)

**User Story:** As a system designer, I want each role to access only its permitted resources, so
that data integrity and security are maintained.

#### Acceptance Criteria

1. WHEN an ADMIN user accesses any endpoint, THE WMS SHALL permit full CRUD operations on
   Políticas, Trámites, Tareas, Departamentos, and Usuarios.
2. WHEN a FUNCIONARIO user accesses an endpoint, THE WMS SHALL permit only GET on their own
   profile, GET on Tareas assigned to their Departamento, and PUT to update Tarea status and form
   data.
3. WHEN a CLIENTE user accesses an endpoint, THE WMS SHALL permit only GET on their own profile,
   GET on published Políticas, GET on their own Trámites, and POST to request a new Trámite.
4. IF a user attempts to access a resource outside their role's permissions, THEN THE WMS SHALL
   return an HTTP 403 response with a descriptive error message.
5. THE WMS SHALL enforce RBAC at the service layer, not only at the controller layer, to prevent
   privilege escalation.

---

### Requirement 3: User and Department Management (Administrator)

**User Story:** As an Administrador, I want to manage users and departments, so that I can
configure the organizational structure before designing workflows.

#### Acceptance Criteria

1. THE WMS SHALL allow an Administrador to create, read, update, and delete Departamentos, each
   identified by a unique name.
2. THE WMS SHALL allow an Administrador to create, read, update, and delete user accounts for all
   roles (ADMIN, FUNCIONARIO, CLIENTE).
3. WHEN an Administrador creates a user, THE WMS SHALL enforce that the email address is unique
   across all users; IF a duplicate email is submitted, THEN THE WMS SHALL return an HTTP 409
   response.
4. THE WMS SHALL allow an Administrador to assign a Funcionario to exactly one Departamento; a
   Departamento MAY have many Funcionarios.
5. WHEN an Administrador deactivates a user (`activo = false`), THE WMS SHALL prevent that user
   from authenticating until reactivated.
6. THE WMS SHALL allow an Administrador to view and edit their own profile (GET own user, PUT
   edit).

---

### Requirement 4: Business Policy Design (Workflow Editor)

**User Story:** As an Administrador, I want to design business process policies using a visual
drag-and-drop editor, so that I can model any organizational workflow without writing code.

#### Acceptance Criteria

1. THE WMS SHALL provide a visual workflow editor powered by GoJS that allows an Administrador to
   add, move, connect, and delete Nodos on a canvas representing UML 2.5 Activity Diagram
   Swimlanes.
2. THE WMS SHALL support the following Nodo types in the editor: `START`, `END`, `TASK`,
   `DECISION`, and `PARALLEL`.
3. WHEN an Administrador defines a `DECISION` Nodo, THE WMS SHALL allow the Administrador to
   specify two outgoing connections with associated boolean conditions (`true` / `false`).
4. WHEN an Administrador defines a `PARALLEL` Nodo, THE WMS SHALL allow the Administrador to
   specify two or more outgoing connections that will be activated simultaneously.
5. THE WMS SHALL allow an Administrador to assign each `TASK` Nodo to a Departamento (swimlane)
   and optionally to a default responsible Funcionario.
6. THE WMS SHALL allow an Administrador to define a dynamic form (`camposFormulario`) for each
   `TASK` Nodo, specifying field name, type (text, boolean, number, date), label, and whether the
   field is required.
7. THE WMS SHALL allow an Administrador to set a `tiempoLimiteHoras` on each Nodo to define the
   maximum allowed processing time before the AI Bottleneck Detector flags it.
8. WHEN an Administrador saves a Política, THE WMS SHALL persist the complete Nodo graph
   (including positions, connections, conditions, and form definitions) and increment the version
   number.
9. THE WMS SHALL support 100% real-time collaborative editing: WHEN two or more Administradores
   edit the same Política simultaneously, THE WMS SHALL synchronize all diagram changes to all
   connected editors via WebSocket within 500 ms.
10. THE WMS SHALL validate that a Política has exactly one `START` Nodo and at least one `END`
    Nodo before allowing it to be published.

---

### Requirement 5: Business Policy Lifecycle Management

**User Story:** As an Administrador, I want to manage the lifecycle of business policies, so that
only validated workflows are available for client requests.

#### Acceptance Criteria

1. WHEN an Administrador creates a new Política, THE WMS SHALL set its initial state to `BORRADOR`
   and record the creator's id and timestamp.
2. WHEN an Administrador publishes a Política in `BORRADOR` state, THE WMS SHALL transition its
   state to `ACTIVA` and record the activation timestamp; THE WMS SHALL reject publication if the
   graph validation in Requirement 4.10 fails.
3. WHEN an Administrador deactivates an `ACTIVA` Política, THE WMS SHALL transition its state to
   `INACTIVA`; existing in-progress Trámites SHALL continue to completion under the previous
   version.
4. IF an Administrador attempts to publish a Política that is already `ACTIVA` or `INACTIVA`,
   THEN THE WMS SHALL return an HTTP 400 response with a descriptive error message.
5. THE WMS SHALL maintain a version counter on each Política; WHEN a saved Política is modified
   and saved again, THE WMS SHALL increment the version number.
6. THE WMS SHALL allow an Administrador to perform full CRUD on Políticas (create, read all, read
   by id, update, delete); DELETE SHALL be permitted only on `BORRADOR` Políticas with zero active
   Trámites.

---

### Requirement 6: Client Portal — Trámite Request

**User Story:** As a Cliente, I want to browse available trámites and submit requests, so that I
can initiate organizational procedures without visiting in person.

#### Acceptance Criteria

1. THE WMS SHALL allow a Cliente to retrieve a list of all `ACTIVA` Políticas, including name,
   description, category, organization, and estimated duration.
2. WHEN a Cliente submits a Trámite request for an `ACTIVA` Política, THE WMS SHALL create a
   Trámite record with state `NUEVO`, assign a unique `NumeroReferencia` in the format
   `TRM-YYYY-NNNN`, and record the request timestamp.
3. THE WMS SHALL allow a Cliente to submit multiple simultaneous Trámite requests for different
   Políticas.
4. THE WMS SHALL allow a Cliente to view all their own Trámites, including current state,
   `nombreNodoActual`, `departamentoActual`, and full `historial`.
5. WHEN a Trámite is `COMPLETADO`, THE WMS SHALL allow the Cliente to download the result as a
   PDF document containing the Trámite reference, policy name, historial, and all accumulated
   `datosFormulario`.
6. THE WMS SHALL provide a visual flow map showing the Cliente the current position of their
   Trámite within the Política diagram, highlighting the active Nodo.

---

### Requirement 7: Trámite Initiation by Administrator

**User Story:** As an Administrador, I want to review and initiate client trámite requests, so
that I can verify the request before the workflow begins execution.

#### Acceptance Criteria

1. WHEN a Cliente submits a Trámite request, THE WMS SHALL send a real-time WebSocket notification
   to all connected Administradores with the Trámite reference and client name.
2. WHEN an Administrador initiates a Trámite, THE WMS SHALL transition the Trámite state from
   `NUEVO` to `EN_PROCESO`, identify the first `TASK` Nodo after the `START` Nodo, create the
   first Tarea, and assign it to the Departamento and default Funcionario defined in that Nodo.
3. IF an Administrador attempts to initiate a Trámite that is not in `NUEVO` state, THEN THE WMS
   SHALL return an HTTP 400 response with a descriptive error message.
4. THE WMS SHALL allow an Administrador to view all Trámites regardless of state, filtered by
   state, Política, or date range.

---

### Requirement 8: Workflow Engine — Automatic Routing

**User Story:** As a system operator, I want the workflow engine to automatically route trámites
between departments upon task completion, so that no manual intervention is required.

#### Acceptance Criteria

1. WHEN a Funcionario completes a Tarea on a `TASK` Nodo, THE WMS SHALL automatically determine
   the next Nodo using the Nodo's `conexiones` list and create a new Tarea for that Nodo.
2. WHEN the current Nodo is a `DECISION` Nodo and the completed Tarea's form data contains
   `aprobado = true`, THE WMS SHALL route to the first connection; WHEN `aprobado = false`, THE
   WMS SHALL route to the second connection.
3. WHEN the current Nodo is a `PARALLEL` Nodo, THE WMS SHALL create one Tarea for each outgoing
   connection simultaneously.
4. WHEN the next Nodo is of type `END`, THE WMS SHALL finalize the Trámite: set state to
   `COMPLETADO`, record `fechaFin`, and calculate `duracionMinutos` as the elapsed time since
   `fechaInicio`.
5. WHEN a Tarea is completed, THE WMS SHALL append a `HistorialTramite` entry recording the Nodo
   id, name, department, responsible Funcionario, action, decision result (if DECISION), duration
   in minutes, and timestamp.
6. WHEN a Tarea is completed, THE WMS SHALL merge the Tarea's `formularioDatos` into the parent
   Trámite's `datosFormulario` map.
7. THE WMS SHALL update `nodoActualId`, `nombreNodoActual`, and `departamentoActual` on the
   Trámite after each routing step.
8. IF a Tarea has already been marked `COMPLETADO`, THEN THE WMS SHALL reject a second completion
   attempt and return an HTTP 400 response.

---

### Requirement 9: Employee Task Dashboard

**User Story:** As a Funcionario, I want a real-time collaborative task board for my department,
so that my team can coordinate and complete assigned tasks efficiently.

#### Acceptance Criteria

1. THE WMS SHALL display to a Funcionario all Tareas assigned to their Departamento, regardless of
   which specific Funcionario within the department is the default responsible.
2. THE WMS SHALL render Tareas using a Semáforo color system: 🔴 `PENDIENTE` (new/urgent), 🟡
   `EN_PROCESO` (in progress), 🟢 `COMPLETADO` (done).
3. WHEN a Funcionario updates a Tarea's state or form data, THE WMS SHALL push the update to all
   other Funcionarios in the same Departamento via WebSocket within 500 ms, without requiring a
   page refresh.
4. THE WMS SHALL allow a Funcionario to update the status of any Tarea in their Departamento
   (PENDIENTE → EN_PROCESO → COMPLETADO or RECHAZADO) using drag-and-drop.
5. THE WMS SHALL allow a Funcionario to view (read-only) Tareas from other Departamentos for
   coordination purposes, but SHALL NOT allow them to modify those Tareas.
6. WHEN a new Tarea is assigned to a Funcionario's Departamento, THE WMS SHALL deliver a real-time
   WebSocket notification to all Funcionarios in that Departamento.
7. THE WMS SHALL allow a Funcionario to fill in the `camposFormulario` defined for the Tarea's
   Nodo; IF a required field is left empty, THEN THE WMS SHALL prevent form submission and display
   a validation error.
8. THE WMS SHALL allow a Funcionario to view and edit their own profile (GET own user, PUT edit).

---

### Requirement 10: Real-Time Notification System

**User Story:** As any user, I want to receive real-time notifications about relevant events, so
that I can act immediately without polling the system.

#### Acceptance Criteria

1. THE WMS SHALL deliver WebSocket notifications to Clientes for the following events: Trámite
   request received, Trámite advanced to a new Departamento, Trámite completed.
2. THE WMS SHALL deliver WebSocket notifications to Funcionarios for the following events: new
   Tarea assigned to their Departamento, Tarea updated by a colleague.
3. THE WMS SHALL deliver WebSocket notifications to Administradores for the following events: new
   Trámite request submitted by a Cliente, AI Bottleneck Detector alert triggered.
4. WHEN a notification is delivered, THE WMS SHALL persist it in the `notificaciones` collection
   with the recipient user id, message text, type, reference id, reference type, and creation
   timestamp.
5. WHEN a user clicks a notification, THE WMS SHALL redirect the user to the relevant section
   (Tarea detail, Trámite detail, or Política editor).
6. THE WMS SHALL allow any authenticated user to retrieve their own unread notifications via a
   REST endpoint.

---

### Requirement 11: AI Design Copilot

**User Story:** As an Administrador, I want an AI assistant to help me modify workflow diagrams
using natural language commands, so that I can design complex policies faster.

#### Acceptance Criteria

1. THE WMS SHALL provide a text and voice input interface within the workflow editor that accepts
   natural language commands in Spanish or English.
2. WHEN an Administrador issues a command such as "Add a legal review node after technical
   approval, and if it fails return to start", THE AI_Copilot SHALL interpret the command and
   apply the corresponding structural changes to the Política diagram.
3. WHEN THE AI_Copilot applies a diagram change, THE WMS SHALL display a preview of the change
   and require explicit confirmation from the Administrador before persisting it.
4. IF THE AI_Copilot cannot interpret a command, THEN THE WMS SHALL display a descriptive error
   message and leave the diagram unchanged.
5. THE WMS SHALL log all AI_Copilot interactions (input command, interpreted action, outcome) for
   audit purposes.

---

### Requirement 12: AI Form Assistant

**User Story:** As a Funcionario, I want an AI assistant to extract data from uploaded documents
and auto-fill task forms, so that I can complete tasks faster and with fewer errors.

#### Acceptance Criteria

1. THE WMS SHALL allow a Funcionario to upload a plain-text or PDF report within a Tarea form.
2. WHEN a Funcionario uploads a document, THE AI_Copilot SHALL extract relevant field values from
   the document and pre-populate the matching `camposFormulario` fields in the Tarea form.
3. WHEN THE AI_Copilot pre-populates form fields, THE WMS SHALL visually distinguish AI-filled
   values from manually entered values and allow the Funcionario to review and correct each field.
4. IF THE AI_Copilot cannot extract a value for a required field, THEN THE WMS SHALL leave that
   field empty and highlight it for manual entry.

---

### Requirement 13: AI Bottleneck Detector

**User Story:** As an Administrador, I want the AI to analyze workflow execution times and
identify bottlenecks, so that I can optimize department performance.

#### Acceptance Criteria

1. THE WMS SHALL calculate the average `duracionMinutos` per Nodo and per Departamento across all
   completed Tareas.
2. WHEN the average duration of a Nodo exceeds its configured `tiempoLimiteHoras`, THE
   AI_Copilot SHALL generate a bottleneck alert and send a WebSocket notification to all
   Administradores.
3. THE WMS SHALL provide an analytics dashboard showing average task duration per Departamento,
   per Nodo, and per Funcionario, ranked from slowest to fastest.
4. THE WMS SHALL allow an Administrador to compare individual Funcionario efficiency (average
   task completion time) within the same Departamento.
5. WHEN THE AI_Copilot detects a bottleneck, THE WMS SHALL suggest corrective actions (e.g.,
   reassign tasks, increase staff in department) based on historical data.

---

### Requirement 14: Trámite Reference Number Generation

**User Story:** As any user, I want each trámite to have a unique, human-readable reference
number, so that I can identify and communicate about specific procedures easily.

#### Acceptance Criteria

1. WHEN a new Trámite is created, THE WMS SHALL assign a `NumeroReferencia` in the format
   `TRM-YYYY-NNNN`, where `YYYY` is the four-digit creation year and `NNNN` is a zero-padded
   sequential counter.
2. THE WMS SHALL guarantee that no two Trámites share the same `NumeroReferencia`; the
   `numeroReferencia` field SHALL be indexed as unique in MongoDB.
3. THE WMS SHALL allow any authenticated user to look up a Trámite by its `NumeroReferencia`.

---

### Requirement 15: PDF Result Generation

**User Story:** As a Cliente, I want to download the result of a completed trámite as a PDF, so
that I have an official record of the procedure.

#### Acceptance Criteria

1. WHEN a Trámite reaches `COMPLETADO` state, THE PDF_Generator SHALL produce a PDF document
   containing: Trámite reference number, policy name, client name, start and end dates, total
   duration, full historial (each step with department, funcionario, action, and timestamp), and
   all accumulated `datosFormulario` values.
2. THE WMS SHALL expose a REST endpoint that returns the PDF as a binary download with
   `Content-Type: application/pdf`.
3. IF a Cliente requests a PDF for a Trámite that is not yet `COMPLETADO`, THEN THE WMS SHALL
   return an HTTP 400 response with a descriptive error message.
4. IF a Cliente requests a PDF for a Trámite that does not belong to them, THEN THE WMS SHALL
   return an HTTP 403 response.

---

### Requirement 16: Non-Functional — Performance and Reliability

**User Story:** As a system operator, I want the WMS to perform efficiently under normal load, so
that users experience a responsive and reliable service.

#### Acceptance Criteria

1. WHEN a REST API request is received under normal load (up to 100 concurrent users), THE WMS
   SHALL respond within 500 ms for 95% of requests.
2. WHEN a WebSocket notification is triggered, THE WMS SHALL deliver it to all connected
   recipients within 500 ms.
3. THE WMS SHALL handle at least 50 simultaneous active Trámites without degradation in routing
   accuracy.
4. WHILE the system is running, THE WMS SHALL log all unhandled exceptions with stack traces and
   return a structured JSON error response to the client instead of exposing internal details.

---

### Requirement 17: Non-Functional — Security and Data Integrity

**User Story:** As a system operator, I want the WMS to protect user data and prevent
unauthorized access, so that the system meets basic security standards.

#### Acceptance Criteria

1. THE WMS SHALL enforce HTTPS for all client-server communication in production deployments.
2. THE WMS SHALL sanitize all user-supplied input before persisting it to MongoDB to prevent
   injection attacks.
3. THE WMS SHALL use environment variables or a secrets manager for all sensitive configuration
   (database URI, JWT secret, OpenAI API key) and SHALL NOT hardcode secrets in source code.
4. THE WMS SHALL implement CORS policies that restrict API access to the registered frontend
   origins.
5. THE WMS SHALL ensure that `datosFormulario` accumulated in a Trámite is never overwritten by a
   subsequent task — only new keys are added or existing keys updated with the latest value.

---

### Requirement 18: Non-Functional — Maintainability and Portability

**User Story:** As a developer, I want the codebase to follow SOLID principles and be deployable
across environments, so that the system is easy to extend and operate.

#### Acceptance Criteria

1. THE WMS SHALL apply the Single Responsibility Principle: each Spring Boot service class SHALL
   have one primary responsibility (e.g., `WorkflowEngineService` handles only routing logic).
2. THE WMS SHALL apply the Open/Closed Principle: adding a new Nodo type SHALL require only
   adding a new handler class, not modifying existing routing logic.
3. THE WMS SHALL be packaged as Docker containers (backend, frontend, database) with a
   `docker-compose.yml` file for local development and a production deployment configuration for
   AWS or Google Cloud.
4. THE WMS SHALL provide a web interface (Angular) and a mobile interface (Flutter) that share the
   same REST and WebSocket API.
5. THE WMS SHALL use a light mode (white theme) as the default visual design across all
   interfaces.

---

## Correctness Properties for Property-Based Testing

The following properties are derived from the acceptance criteria above and are suitable for
implementation as property-based tests (e.g., using JUnit 5 + jqwik or a similar PBT library).

### Property 1: JWT Round-Trip (Requirement 1)

FOR ALL valid user credentials `(email, password)`, encoding the user claims into a JWT and then
decoding the JWT SHALL produce the original claims (`userId`, `rol`, `email`) without loss or
mutation.

- Pattern: Round-Trip
- Relevant criteria: 1.1, 1.7

### Property 2: New Política Always Starts in BORRADOR (Requirement 5)

FOR ALL valid Política creation requests with any combination of `nombre`, `descripcion`,
`categoria`, and `organizacion`, THE WMS SHALL create the Política with `estado = BORRADOR` and
`version = 1`.

- Pattern: Invariant
- Relevant criteria: 5.1

### Property 3: Workflow Graph Validity Invariant (Requirement 4)

FOR ALL Política graphs with any number of Nodos and connections, THE WMS SHALL reject publication
(transition to `ACTIVA`) if and only if the graph does NOT contain exactly one `START` Nodo and
at least one `END` Nodo.

- Pattern: Invariant / Error Condition
- Relevant criteria: 4.10, 5.2

### Property 4: New Trámite State and Reference Invariant (Requirement 6, 14)

FOR ALL valid Trámite creation requests against any `ACTIVA` Política, THE WMS SHALL create the
Trámite with `estado = NUEVO`, a `NumeroReferencia` matching the pattern `TRM-\d{4}-\d{4}`, and
`fechaInicio` set to the current timestamp.

- Pattern: Invariant
- Relevant criteria: 6.2, 14.1

### Property 5: NumeroReferencia Uniqueness (Requirement 14)

FOR ALL sequences of N Trámite creation requests (N ≥ 2), all generated `NumeroReferencia` values
SHALL be distinct.

- Pattern: Invariant
- Relevant criteria: 14.2

### Property 6: Workflow Routing Correctness — Sequential (Requirement 8)

FOR ALL Políticas with a linear chain of TASK Nodos (no DECISION or PARALLEL), completing each
Tarea in sequence SHALL advance the Trámite through every Nodo exactly once and SHALL finalize the
Trámite with `estado = COMPLETADO` after the last Tarea is completed.

- Pattern: Metamorphic / Round-Trip
- Relevant criteria: 8.1, 8.4, 8.7

### Property 7: DECISION Routing Correctness (Requirement 8)

FOR ALL Políticas containing a `DECISION` Nodo with two outgoing connections, WHEN the completed
Tarea's form data contains `aprobado = true`, THE WMS SHALL route to `conexiones[0]`; WHEN
`aprobado = false`, THE WMS SHALL route to `conexiones[1]`.

- Pattern: Metamorphic
- Relevant criteria: 8.2

### Property 8: Historial Append-Only Invariant (Requirement 8)

FOR ALL Trámites in any state, the length of `historial` SHALL be monotonically non-decreasing
over time; no existing `HistorialTramite` entry SHALL be modified or removed after insertion.

- Pattern: Invariant
- Relevant criteria: 8.5

### Property 9: Form Data Accumulation (Requirement 8)

FOR ALL sequences of Tarea completions on the same Trámite, the Trámite's `datosFormulario` SHALL
contain the union of all `formularioDatos` maps from all completed Tareas; no previously written
key SHALL be absent after a subsequent merge.

- Pattern: Invariant
- Relevant criteria: 8.6, 17.5

### Property 10: Completed Task Rejection (Requirement 8)

FOR ALL Tareas with `estado = COMPLETADO`, a second call to complete the same Tarea SHALL return
an HTTP 400 error and SHALL NOT create a duplicate `HistorialTramite` entry.

- Pattern: Idempotence / Error Condition
- Relevant criteria: 8.8

### Property 11: Trámite Duration Positivity (Requirement 8)

FOR ALL completed Trámites, `duracionMinutos` SHALL be greater than zero and SHALL equal the
elapsed minutes between `fechaInicio` and `fechaFin`.

- Pattern: Invariant
- Relevant criteria: 8.4

### Property 12: Required Form Field Validation (Requirement 9)

FOR ALL Tarea form submissions where one or more `camposFormulario` fields have `requerido = true`
and the submitted data omits those fields, THE WMS SHALL reject the submission and SHALL NOT
advance the Trámite.

- Pattern: Error Condition
- Relevant criteria: 9.7

### Property 13: Bottleneck Detection Threshold (Requirement 13)

FOR ALL Nodos with a configured `tiempoLimiteHoras` value T, WHEN the average `duracionMinutos`
of completed Tareas for that Nodo exceeds T × 60, THE WMS SHALL generate a bottleneck alert;
WHEN the average is below or equal to T × 60, THE WMS SHALL NOT generate an alert.

- Pattern: Metamorphic
- Relevant criteria: 13.2

### Property 14: Unique Email Invariant (Requirement 3)

FOR ALL sequences of user creation requests, THE WMS SHALL reject any request whose email address
matches an existing user's email with an HTTP 409 response, ensuring no two users share the same
email.

- Pattern: Invariant / Error Condition
- Relevant criteria: 3.3
