# CertiFlow · Entrega 3

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

### Propuesta de valor: Asistente con IA y harness de evaluación

La propuesta de valor es un requerimiento adicional que se implementa sobre la aplicación existente y se integra a su arquitectura. Es propia de esta consigna.

La propuesta de valor de esta consigna es incorporar un **asistente basado en un modelo de lenguaje (LLM) que ayude al inspector, junto con el harness de evaluación que permite confiar en él**. El valor no está solo en llamar a un modelo: está en integrarlo detrás de un puerto propio, con límites claros y con una forma verificable de medir su calidad, que es lo que diferencia un experimento de una funcionalidad profesional.

| Elemento | Requisito |
| :--- | :--- |
| Funcionalidad asistida | Al menos una: sugerir hallazgos y severidades a partir de las observaciones y evidencias registradas, o redactar el borrador del acta de inspección |
| Guardrails | Las sugerencias nunca cierran una inspección ni emiten certificados; un humano las acepta o descarta, y queda trazado qué fue sugerido por la IA y qué decidió el inspector |

### Requisitos no funcionales

#### Arquitectura

La aplicación conserva la arquitectura elegida (Clean Architecture, Domain Driven Design o Hexagonal). La propuesta de valor se integra detrás de contratos propios: el módulo de dominio sigue libre de dependencias de framework y de proveedores externos, y la lógica de negocio no se duplica en la capa nueva.

#### Requisitos de la propuesta de valor

| Elemento | Requisito |
| :--- | :--- |
| Puerto propio | Un contrato como `InspectionAssistant` definido por el dominio; la aplicación no depende del proveedor del modelo |
| Harness de evaluación | Un conjunto dorado de al menos diez casos reales o realistas (observaciones de entrada y hallazgos esperados), ejecutable de forma automatizada, con métricas simples: hallazgos correctos, severidades acertadas y hallazgos inventados |
| Versionado de prompts | Los prompts se tratan como configuración versionada; un cambio de prompt vuelve a ejecutar el harness y compara resultados |
| CI determinista | La CI usa una implementación stub del puerto; el harness contra el modelo real se ejecuta por separado |

### Entrega 3 - Propuesta de valor y presentación final

**Objetivo:** incorporar al sistema la propuesta de valor descrita en este documento - o justificar técnicamente por qué no aplica al dominio - y presentarla oralmente, junto con el dominio y las decisiones clave de implementación.

El entregable central es la aplicación completa con la propuesta de valor integrada: el requerimiento adicional se implementa sobre la aplicación existente, respeta su arquitectura y se presenta como se haría ante un cliente o un comité técnico: qué problema resuelve, qué costo tuvo y qué valor agrega.

### Entregables obligatorios

| Entregable | Contenido mínimo |
| :--- | :--- |
| Repositorio | URL del repositorio de GitHub y hash del último commit que corresponde a la entrega |
| Código de la aplicación | Aplicación completa con la propuesta de valor integrada, cumpliendo los requisitos funcionales y no funcionales de este documento |
| Tests | Los indicados en los requisitos no funcionales, ejecutables en la integración continua |
| Justificación escrita | Solo si el equipo sostiene que la propuesta no aplica al dominio, según se describe más abajo |
| Presentación oral | Frente a toda la clase, con el formato descrito más abajo; incluye demostración en vivo |
| Decisiones de diseño | Archivo `DESIGN.md` en la raíz del repositorio que enumera las decisiones de diseño principales: qué patrón o principio se aplicó, dónde y por qué; qué alternativas se descartaron; y qué patrones se decidió no aplicar, con sus consecuencias |

### Si la propuesta no aplica al dominio

Si el equipo considera que la propuesta no aplica a su dominio, debe explicar por qué: entregar una justificación técnica escrita y defenderla en la presentación, indicando qué problemas o costos tendría incorporarla y qué alternativa aportaría un valor equivalente. Una justificación sólida y honesta vale tanto como una implementación; una excusa para evitar trabajo, no.

### Presentación oral

La presentación es oral y frente a toda la clase. Presenta un integrante por grupo, elegido aleatoriamente al comienzo de la clase de presentaciones. Por lo tanto, todos los integrantes deben estar preparados para presentar el trabajo completo. El resto del grupo puede participar durante la ronda de preguntas. La gestión del tiempo disponible es parte de la evaluación.

| Momento | Contenido |
| :--- | :--- |
| Dominio | Problema, actores, reglas centrales y puntos de extensión del diseño. La audiencia no conoce este dominio, así que debe poder entenderlo desde cero |
| Propuesta de valor | Qué es, por qué aporta valor en este dominio, cómo se integró a la arquitectura y demostración. Si el grupo argumentó que no aplica, presentación de esa justificación |
| Decisiones clave | Las decisiones de implementación más importantes: alternativas consideradas, decisión tomada y consecuencias observadas |
| Preguntas | Preguntas del docente y de la clase; pueden dirigirse a cualquier integrante del grupo |

En la presentación, el equipo debe explicar qué es un harness de evaluación y por qué un componente no determinista no puede probarse solo con tests clásicos de igualdad, mostrar los resultados de su conjunto dorado y discutir los límites: qué errores comete el asistente, cómo se detectan y por qué la decisión final sigue siendo humana.

### Qué no se exige en esta entrega

No se exige rehacer la aplicación ni incorporar funcionalidades ajenas a la propuesta de valor. El esfuerzo debe concentrarse en integrarla bien a la arquitectura existente y en poder explicar cada decisión.

---

## Indicaciones de entrega de la cátedra

Comunicadas aparte del enunciado. Definen el formato de la presentación.

Se deben entregar dos enlaces:

1. La URL del repositorio y su hash, donde deben estar los entregables obligatorios del enunciado.
2. La URL del video de YouTube con la presentación grupal.

**Condiciones de la presentación.** Tiene que ser el enlace de un video de YouTube (no listado) con una presentación de entre 8 y 12 minutos donde se muestren todos los integrantes del equipo hablando de al menos una parte diferente del sistema cada uno. La estética y la organización quedan a libre ideación y diseño de cada grupo. Puede ser simplemente una llamada de Meet grabada y presentada correctamente, o puede grabarse y editarse un video si alguien tiene ese conocimiento. Esto es parte del TPI y no se va a evaluar ningún TPI sin este video de presentación.

**Contenido mínimo de la presentación:**

- Decisiones de dominio.
- Implementación de los endpoints.
- Refactors entre entregas.
- Propuesta de valor.

Para cada uno, las alternativas que se exploraron y el porqué de la elegida.
