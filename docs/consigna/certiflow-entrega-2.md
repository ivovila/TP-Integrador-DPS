# CertiFlow · Entrega 2

## Plataforma de inspección, habilitación y certificación de activos

### Introducción

El trabajo integrador se desarrolla en grupos de cinco integrantes sobre esta consigna. La complejidad de negocio es suficiente para que la calidad del modelado, la separación de responsabilidades y la extensibilidad resulten determinantes: no alcanza con que el sistema funcione.

### Enunciado

Una entidad de certificación inspecciona instalaciones, laboratorios, equipos y otros activos técnicos. Cada tipo de activo está sujeto a un esquema de inspección compuesto por criterios, evidencias obligatorias, niveles de severidad y reglas de aprobación.

Los esquemas cambian con el tiempo. Una inspección debe conservar las reglas que estaban vigentes cuando fue iniciada, aun cuando posteriormente se publique una versión nueva. Los inspectores registran mediciones, respuestas, fotografías, documentos y observaciones. A partir de esos datos, el sistema determina hallazgos, acciones correctivas y la posibilidad de emitir un certificado.

El equipo deberá desarrollar una aplicación para administrar activos, definir esquemas versionados, ejecutar inspecciones, registrar evidencias, evaluar resultados y gestionar el ciclo de vida de certificados y acciones correctivas.

### Requisitos funcionales obligatorios

| Capacidad | Comportamiento esperado |
| :--- | :--- |
| Catálogo de activos | Dar de alta y consulta de activos con tipo, ubicación, responsable y características |
| Esquemas de inspección | Creación de plantillas formadas por secciones y criterios |
| Versionado | Publicación de nuevas versiones sin modificar inspecciones anteriores |
| Asignación de inspecciones | Designación de inspector, fecha prevista y alcance |
| Ejecución | Registro progresivo de respuestas, mediciones, evidencias y observaciones |
| Evaluación | Determinación automática de criterios aprobados, observados o rechazados |
| Hallazgos | Creación de no conformidades con severidad, evidencia y responsable |
| Acciones correctivas | Planificación, vencimiento, verificación y cierre |
| Certificación | Emisión, suspensión, renovación y vencimiento de certificados |
| Auditoría | Historial de modificaciones, decisiones y transiciones |
| Informes | Acta de inspección, resumen de hallazgos y certificado |

Una inspección no puede alterarse libremente luego de cerrarse. Las correcciones posteriores deben realizarse mediante una rectificación auditable.

### Funcionalidades nuevas de esta entrega

Además de la implementación completa de los requisitos funcionales anteriores y de los requisitos no funcionales, esta entrega incorpora las funcionalidades descritas a continuación.

#### F1 - Certificaciones parciales por subsistema

Un activo puede descomponerse en subsistemas (por ejemplo, instalación eléctrica, sistema de presión, seguridad edilicia). El resultado de una inspección debe poder certificar algunos subsistemas y rechazar otros, en lugar de emitir únicamente un veredicto global.

**Criterios de aceptación:** un esquema puede asociar secciones o criterios a subsistemas; una misma inspección produce certificados parciales por subsistema con vigencias independientes; el certificado global se deriva de los parciales mediante una política y no de un condicional ad hoc.

#### F2 - Versiones con vigencia futura

Una versión nueva de un esquema debe poder publicarse con una fecha de entrada en vigencia futura. Hasta esa fecha, las inspecciones nuevas siguen usando la versión vigente; a partir de ella, la nueva. Las inspecciones ya iniciadas nunca cambian de versión.

**Criterios de aceptación:** puede publicarse una versión con vigencia futura; una inspección iniciada antes de esa fecha usa la versión anterior y una iniciada después usa la nueva; el sistema puede responder qué versión estaba vigente en una fecha dada.

#### F3 - Políticas de certificación por jurisdicción

La entidad comienza a operar en varias jurisdicciones, cada una con su propia política de certificación: qué severidades bloquean la emisión, qué plazos de vencimiento aplican y si se admiten certificados condicionales.

**Criterios de aceptación:** el mismo resultado de inspección puede producir decisiones distintas según la jurisdicción del activo; agregar una jurisdicción nueva no exige modificar el servicio central de certificación; la decisión registra qué política se aplicó.

### Requisitos no funcionales

#### Arquitectura

La arquitectura del proyecto debe seguir una de las vistas en clase: Clean Architecture, Domain Driven Design o Hexagonal.

El backend debe exponer una API REST. El frontend debe contener lo mínimo indispensable para llevar a cabo las funcionalidades pedidas.

#### Testing e integración continua

Deben existir pruebas unitarias, de repositorio y de API. Los mocks sustituyen colaboradores en pruebas unitarias; no reemplazan las pruebas reales de persistencia ni de API.

La integración continua debe compilar, ejecutar las pruebas y bloquear la integración de pull requests que fallen.

### Entrega 2 - Aplicación completa

**Objetivo:** comprobar que la aplicación resuelve los principales casos de uso, que su arquitectura respeta límites claros y que el diseño tolera una evolución significativa: las funcionalidades nuevas descritas en este documento.

El entregable central es la aplicación completa: un proyecto ejecutable que implementa todos los requisitos funcionales, incluidas las funcionalidades nuevas, sobre el módulo de dominio, y que cumple los requisitos no funcionales descritos en este documento.

### Entregables obligatorios

| Entregable | Contenido mínimo |
| :--- | :--- |
| Repositorio | URL del repositorio de GitHub y hash del último commit que corresponde a la entrega |
| Código de la aplicación | Módulo de dominio, casos de uso, infraestructura (persistencia, integraciones, procesos) y API REST, organizados según la arquitectura elegida |
| Funcionalidades nuevas | F1, F2 y F3 implementadas, probadas y documentadas |
| Tests | Unitarios, parametrizados, de repositorio y de API; deben ejecutarse en la integración continua |
| Decisiones de diseño | Archivo `DESIGN.md` en la raíz del repositorio que enumera las decisiones de diseño principales: qué patrón o principio se aplicó, dónde y por qué; qué alternativas se descartaron; y qué patrones se decidió no aplicar, con sus consecuencias. Por cada funcionalidad nueva incluye además las clases agregadas y modificadas, las refactorizaciones realizadas y la deuda técnica que se decidió no resolver |

### Demostración sugerida

La demostración puede incluir un esquema con secciones anidadas, al menos ocho tipos de criterios, dos versiones del mismo esquema (una con vigencia futura), una inspección que conserve la versión anterior, hallazgos de distinta severidad, certificados parciales por subsistema y decisiones de certificación bajo dos jurisdicciones diferentes.

### Qué no se exige en esta entrega

No se exigen microservicios ni despliegue en la nube. El esfuerzo debe concentrarse en el backend, que es lo que se evalúa aquí.