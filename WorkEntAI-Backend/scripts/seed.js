const { MongoClient, ObjectId } = require('mongodb');
const bcrypt = require('bcryptjs');
require('dotenv').config({ path: '../.env' });

const uri = process.env.MONGODB_URI;

async function run() {
  const client = new MongoClient(uri);
  try {
    await client.connect();
    const db = client.db('workflow_db');

    console.log("Limpiando colecciones...");
    await db.collection('usuarios').deleteMany({});
    await db.collection('politicas').deleteMany({});
    await db.collection('tramites').deleteMany({});
    await db.collection('tareas').deleteMany({});
    await db.collection('departamentos').deleteMany({});
    await db.collection('documentos').deleteMany({});
    await db.collection('notificaciones').deleteMany({});
    await db.collection('alertas_anomalias').deleteMany({});

    console.log("Creando Usuarios y Departamentos...");
    const hashPwd = bcrypt.hashSync('123456', 10);
    const dateNow = new Date();

    const depts = ["Atención al Cliente", "Técnico", "Legal", "Almacén", "Finanzas"];
    
    // Admin
    const adminId = new ObjectId();
    let usuarios = [{ _id: adminId, _class: 'com.workflow.backend.models.Usuario', nombre: 'Administrador Principal', email: 'admin@workent.com', password: hashPwd, rol: 'ADMIN', departamentos: [], activo: true, fechaCreacion: dateNow }];

    // Funcionarios
    const funcionarios = {};
    for (const dept of depts) {
      const fId = new ObjectId();
      funcionarios[dept] = fId.toString();
      usuarios.push({
        _id: fId,
        _class: 'com.workflow.backend.models.Usuario',
        nombre: `Funcionario ${dept}`,
        email: `${dept.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().replace(/ /g, '')}@workent.com`,
        password: hashPwd,
        rol: 'FUNCIONARIO',
        departamento: dept,
        departamentos: [dept],
        activo: true,
        fechaCreacion: dateNow
      });
    }

    // Clientes
    const clientesIds = [];
    const clientesNombres = ["Juan Perez", "Maria Lopez", "Carlos Ramirez", "Ana Torres", "Luis Gomez", "Sofia Vargas"];
    for (let i = 0; i < clientesNombres.length; i++) {
      const cId = new ObjectId();
      clientesIds.push(cId.toString());
      usuarios.push({
        _id: cId,
        _class: 'com.workflow.backend.models.Usuario',
        nombre: clientesNombres[i],
        email: `cliente${i+1}@correo.com`,
        password: hashPwd,
        rol: 'CLIENTE',
        departamentos: [],
        activo: true,
        fechaCreacion: dateNow
      });
    }

    await db.collection('usuarios').insertMany(usuarios);

    console.log("Creando Políticas de Negocio (Workflows)...");

    const politicas = [];
    
    // Helpers para crear nodos
    const createNode = (nombre, tipo, dept, pos, campos = [], desc = "") => ({
      _id: new ObjectId().toString(),
      nombre,
      descripcion: desc || `Tarea de ${nombre}`,
      tipo, // START, TASK, END, GATEWAY
      departamento: dept,
      tiempoLimiteHoras: Math.floor(Math.random() * 48) + 12,
      posX: pos * 200,
      posY: 100,
      conexiones: [],
      camposFormulario: campos
    });

    const buildPolitica = (nombre, categoria, etiquetas, nodosList, requisitos = []) => {
      // Connect nodes sequentially
      for (let i = 0; i < nodosList.length - 1; i++) {
        nodosList[i].conexiones.push(nodosList[i+1]._id);
      }
      return {
        _id: new ObjectId(),
        _class: 'com.workflow.backend.models.Politica',
        nombre,
        descripcion: `Proceso para ${nombre}`,
        categoria,
        organizacion: "Empresa Demo S.A.",
        tiempoEstimadoDias: 10,
        etiquetas,
        estado: 'ACTIVA',
        creadoPorId: adminId.toString(),
        nombreCreadoPor: 'Administrador Principal',
        version: 1,
        tramitesActivos: 0,
        tramitesCompletados: 0,
        fechaCreacion: dateNow,
        nodos: nodosList,
        requisitosIniciales: requisitos
      };
    };

    // 1. Instalación de Luz (CRE)
    const luzNodos = [
      createNode("Recepción Solicitud Luz", "START", "Atención al Cliente", 1, [{ nombre: "dir", etiqueta: "Dirección", tipo: "text", requerido: true }]),
      createNode("Evaluación de Ubicación", "TASK", "Técnico", 2, [{ nombre: "factible", etiqueta: "Factibilidad", tipo: "select", opciones: ["Sí", "No"], requerido: true }]),
      createNode("Evaluación Legal/Terreno", "TASK", "Legal", 3, [{ nombre: "docs_ok", etiqueta: "Papeles en Orden", tipo: "boolean", requerido: true }]),
      createNode("Asignación de Medidor", "TASK", "Almacén", 4, [{ nombre: "nro_medidor", etiqueta: "Nro. Medidor", tipo: "number", requerido: true }]),
      createNode("Instalación Completada", "END", "Técnico", 5, [])
    ];
    politicas.push(buildPolitica("Instalación de Luz Eléctrica (CRE)", "Servicios Públicos", ["luz", "cre", "medidor", "electricidad"], luzNodos, [
      { id: "req-1", nombre: "Carnet de Identidad", descripcion: "Copia legible de CI vigente", tipoArchivo: "application/pdf", obligatorio: true },
      { id: "req-2", nombre: "Aviso de Cobranza del Vecino", descripcion: "Para referencia de ubicación", tipoArchivo: "application/pdf", obligatorio: true },
      { id: "req-3", nombre: "Plano de Ubicación", descripcion: "Plano firmado", tipoArchivo: "image/jpeg", obligatorio: false }
    ]));

    // 2. Instalación de Medidor COTAS
    const cotasNodos = [
      createNode("Atención Cliente COTAS", "START", "Atención al Cliente", 1, [{ nombre: "req", etiqueta: "Requisitos Completos", tipo: "checkbox", opciones: ["CI", "Aviso Cobranza", "Croquis"], requerido: true }]),
      createNode("Viabilidad Técnica (Espacio)", "TASK", "Técnico", 2, [{ nombre: "viabilidad", etiqueta: "Espacio en Poste", tipo: "grid", requerido: false }]),
      createNode("Aprobación Legal", "TASK", "Legal", 3, [{ nombre: "firma", etiqueta: "Firma Autorizada", tipo: "boolean", requerido: true }]),
      createNode("Instalación y Pruebas", "END", "Técnico", 4, [])
    ];
    politicas.push(buildPolitica("Instalación de Medidor (COTAS)", "Telecomunicaciones", ["cotas", "telefono", "medidor", "internet"], cotasNodos, [
      { id: "req-cot-1", nombre: "Carnet de Identidad Titular", descripcion: "Copia legible", tipoArchivo: "application/pdf", obligatorio: true },
      { id: "req-cot-2", nombre: "Croquis de Domicilio", descripcion: "Hecho a mano o mapa", tipoArchivo: "image/jpeg", obligatorio: true }
    ]));

    // 3. Instalación de Gas (YPFB)
    const gasNodos = [
      createNode("Solicitud Gas", "START", "Atención al Cliente", 1, [{ nombre: "zona", etiqueta: "Zona", tipo: "text", requerido: true }]),
      createNode("Inspección Técnica Domiciliaria", "TASK", "Técnico", 2, [{ nombre: "estado", etiqueta: "Condiciones Seguras", tipo: "select", opciones: ["Aprobado", "Rechazado"], requerido: true }]),
      createNode("Cotización e Insumos", "TASK", "Finanzas", 3, [{ nombre: "presupuesto", etiqueta: "Presupuesto", tipo: "number", requerido: true }]),
      createNode("Habilitación de Gas", "END", "Técnico", 4, [])
    ];
    politicas.push(buildPolitica("Instalación Gas Domiciliario", "Servicios Públicos", ["gas", "ypfb", "domicilio"], gasNodos, [
      { id: "req-gas-1", nombre: "Folio Real o Titulo de Propiedad", descripcion: "Documento de propiedad", tipoArchivo: "application/pdf", obligatorio: true }
    ]));

    // 4. Apertura Cuenta Bancaria
    const bancoNodos = [
      createNode("Atención en Plataforma", "START", "Atención al Cliente", 1, [{ nombre: "tipo_cuenta", etiqueta: "Tipo de Cuenta", tipo: "select", opciones: ["Ahorro", "Corriente"], requerido: true }]),
      createNode("Verificación Legal y Riesgo", "TASK", "Legal", 2, [{ nombre: "riesgo", etiqueta: "Nivel Riesgo", tipo: "select", opciones: ["Bajo", "Medio", "Alto"], requerido: true }]),
      createNode("Aprobación y Entrega Tarjeta", "END", "Finanzas", 3, [{ nombre: "tarjeta_entregada", etiqueta: "Tarjeta Entregada", tipo: "boolean", requerido: true }])
    ];
    politicas.push(buildPolitica("Apertura de Cuenta Bancaria", "Finanzas", ["banco", "cuenta", "ahorro", "credito"], bancoNodos, [
      { id: "req-bnc-1", nombre: "Documento de Identidad", descripcion: "CI o Pasaporte", tipoArchivo: "application/pdf", obligatorio: true }
    ]));

    // 5. Titulación Universidad
    const uniNodos = [
      createNode("Recepción de Documentos", "START", "Atención al Cliente", 1, [{ nombre: "docs", etiqueta: "Docs Listos", tipo: "checkbox", opciones: ["Carnet", "Certificado"], requerido: true }]),
      createNode("Revisión de Malla Curricular", "TASK", "Técnico", 2, [{ nombre: "materias", etiqueta: "Materias", tipo: "grid", requerido: false }]),
      createNode("Pago de Aranceles", "TASK", "Finanzas", 3, [{ nombre: "pago_ok", etiqueta: "Pago Realizado", tipo: "boolean", requerido: true }]),
      createNode("Emisión Legal del Título", "END", "Legal", 4, [{ nombre: "fecha", etiqueta: "Fecha de Emisión", tipo: "fecha", requerido: true }])
    ];
    politicas.push(buildPolitica("Trámite de Titulación Universitario", "Educación", ["titulo", "universidad", "graduacion", "diploma"], uniNodos, [
      { id: "req-uni-1", nombre: "Certificado de Conclusión de Estudios", descripcion: "Original", tipoArchivo: "application/pdf", obligatorio: true },
      { id: "req-uni-2", nombre: "Fotografía 4x4", descripcion: "Fondo Rojo", tipoArchivo: "image/jpeg", obligatorio: true }
    ]));

    await db.collection('politicas').insertMany(politicas);

    console.log("Generando Trámites e Históricos (LSTM/KPIs)...");

    const tramites = [];
    const tareas = [];

    // Para cada política, creamos unos 10 trámites históricos (Completados) y 3 activos
    for (const pol of politicas) {
      let tActivos = 0;
      let tCompletados = 0;

      for (let i = 0; i < 15; i++) {
        const isCompleted = i < 10; // 10 completados, 5 activos
        const clienteId = clientesIds[i % clientesIds.length];
        
        const tramiteId = new ObjectId();
        const startDaysAgo = Math.floor(Math.random() * 60) + 5; // Empezó hace 5-65 días
        const fechaInicio = new Date(Date.now() - startDaysAgo * 24 * 3600 * 1000);
        
        const historial = [];
        const tiemposNodos = [];
        let currentNode = pol.nodos[0];
        let currentDate = new Date(fechaInicio);

        // Progreso
        const maxNodos = isCompleted ? pol.nodos.length : Math.floor(Math.random() * (pol.nodos.length - 1)) + 1;
        
        for (let n = 0; n < maxNodos; n++) {
          const nodo = pol.nodos[n];
          const funcId = funcionarios[nodo.departamento] || adminId.toString();
          
          const duracionHoras = Math.floor(Math.random() * 40) + 1; // 1 a 40 horas en ese nodo
          const salidaDate = new Date(currentDate.getTime() + duracionHoras * 3600 * 1000);

          historial.push({
            nodoId: nodo._id,
            nombreNodo: nodo.nombre,
            departamento: nodo.departamento,
            funcionarioId: n === maxNodos - 1 && !isCompleted ? null : funcId,
            nombreFuncionario: n === maxNodos - 1 && !isCompleted ? null : `Funcionario ${nodo.departamento}`,
            accion: "COMPLETADO",
            observacion: "Aprobado sin novedades",
            resultadoDecision: nodo.tipo === "GATEWAY" ? "APROBADO" : null,
            duracionMinutos: duracionHoras * 60,
            fecha: n === maxNodos - 1 && !isCompleted ? null : salidaDate
          });

          if (n < maxNodos - 1 || isCompleted) {
            tiemposNodos.push({
              nodoId: nodo._id,
              tiempoEnMinutos: duracionHoras * 60
            });
            currentDate = salidaDate; // El siguiente nodo empieza cuando este termina
          }

          currentNode = nodo;
        }

        const tramite = {
          _id: tramiteId,
          _class: 'com.workflow.backend.models.Tramite',
          politicaId: pol._id.toString(),
          nombrePolitica: pol.nombre,
          clienteId: clienteId,
          nombreCliente: clientesNombres[i % clientesNombres.length],
          estado: isCompleted ? "COMPLETADO" : "EN_PROCESO",
          fechaInicio: fechaInicio,
          fechaFin: isCompleted ? currentDate : null,
          progreso: Math.floor((maxNodos / pol.nodos.length) * 100),
          nodoActualId: isCompleted ? null : currentNode._id,
          historial: historial,
          tiemposNodos: tiemposNodos,
          datosFormulario: { "Motivo de Solicitud": "Nueva Instalación", "Observaciones Cliente": "Ninguna", "Pre-Aprobado": "Sí" },
          duracionMinutos: isCompleted ? Math.floor((currentDate - fechaInicio) / 60000) : null
        };

        tramites.push(tramite);

        if (isCompleted) {
          tCompletados++;
        } else {
          tActivos++;
          // Crear la Tarea Pendiente para el funcionario de ese nodo
          tareas.push({
            _class: 'com.workflow.backend.models.Tarea',
            tramiteId: tramiteId.toString(),
            politicaId: pol._id.toString(),
            nombrePolitica: pol.nombre,
            nodoId: currentNode._id,
            nombreNodo: currentNode.nombre,
            departamento: currentNode.departamento,
            descripcionInstrucciones: currentNode.descripcion,
            clienteId: clienteId,
            nombreCliente: clientesNombres[i % clientesNombres.length],
            estado: "PENDIENTE",
            fechaAsignacion: new Date(),
            fechaLimite: new Date(Date.now() + (currentNode.tiempoLimiteHoras || 24) * 3600 * 1000),
            camposFormulario: currentNode.camposFormulario
          });
        }
      }

      // Actualizar contadores en la política
      await db.collection('politicas').updateOne(
        { _id: pol._id },
        { $set: { tramitesActivos: tActivos, tramitesCompletados: tCompletados } }
      );
    }

    await db.collection('tramites').insertMany(tramites);
    if (tareas.length > 0) {
      await db.collection('tareas').insertMany(tareas);
    }

    console.log("Generando Notificaciones...");
    const notificaciones = [];
    for (const dept of depts) {
      const fId = funcionarios[dept];
      notificaciones.push({
        _class: 'com.workflow.backend.models.Notificacion',
        usuarioId: fId,
        departamento: dept,
        titulo: "¡Bienvenido a WorkEntAI!",
        mensaje: `Hola Funcionario de ${dept}, tienes nuevas tareas asignadas en tu bandeja.`,
        tipo: "INFO",
        entidadId: "",
        entidadTipo: "SISTEMA",
        leida: false,
        fechaCreacion: new Date()
      });
      notificaciones.push({
        _class: 'com.workflow.backend.models.Notificacion',
        usuarioId: fId,
        departamento: dept,
        titulo: "Trámites Pendientes",
        mensaje: "No olvides revisar los trámites de Alta Prioridad de hoy.",
        tipo: "TAREA_ASIGNADA",
        entidadId: "",
        entidadTipo: "TAREA",
        leida: false,
        fechaCreacion: new Date()
      });
    }
    await db.collection('notificaciones').insertMany(notificaciones);

    console.log("Generando Anomalías de Detección LSTM...");
    const anomalias = [];
    const tramitesActivos = tramites.filter(t => t.estado === "EN_PROCESO");
    if (tramitesActivos.length >= 2) {
      anomalias.push({
        _class: 'com.workflow.backend.models.AlertaAnomalia',
        tramiteId: tramitesActivos[0]._id.toString(),
        politicaId: tramitesActivos[0].politicaId,
        nodoId: tramitesActivos[0].nodoActualId,
        nivelGravedad: "CRITICO",
        descripcion: `🚨 El modelo LSTM detectó una anomalía severa. El tiempo de permanencia en el nodo '${tramitesActivos[0].nombreNodoActual || 'Evaluación Técnica'}' excede el 300% del promedio histórico. Riesgo inminente de incumplimiento de SLA.`,
        score: 95.5,
        resuelta: false,
        fechaDeteccion: new Date()
      });
      anomalias.push({
        _class: 'com.workflow.backend.models.AlertaAnomalia',
        tramiteId: tramitesActivos[1]._id.toString(),
        politicaId: tramitesActivos[1].politicaId,
        nodoId: tramitesActivos[1].nodoActualId,
        nivelGravedad: "ALTO",
        descripcion: `⚠️ El agente de IA ha detectado un posible cuello de botella. El rendimiento del departamento para este tipo de trámite muestra una degradación del 40% en las últimas 48 horas.`,
        score: 82.0,
        resuelta: false,
        fechaDeteccion: new Date()
      });
      await db.collection('alertas_anomalias').insertMany(anomalias);
    }

    console.log("¡Datos maestros y transaccionales generados con éxito!");
  } finally {
    await client.close();
  }
}

run().catch(console.dir);
