# Clase 1 · El diseño

ITBA 2026 · Fernando Cargnelutti

## ¿Qué cambió? ¿Se puede ver?

La clase abre con dos versiones del mismo método ilegible:

```java
String aAndH(int[] gs){double ac=0.0;double a=0.0;double hg=gs[0];
for(int i=0;i<gs.length;i++){ac+=gs[i];if(gs[i]>hg){hg=gs[i];}}a=ac/gs.length;return "A&H:"+a+","+hg;}
```

En la segunda versión el acumulador cambió (`a+=gs[i]` en lugar de `ac+=gs[i]`), y con ese código no se puede ver:
el mal código esconde los errores.

## El software

Modelo computable de un dominio de problema de la realidad.

## Representación del conocimiento

| Etapa | Lenguaje | Características |
|---|---|---|
| Análisis | Lenguaje natural | Informal, incompleto, tácito |
| Diseño | Diagramas | Informal, incompleto, explícito |
| Código | Lenguaje formal | Formal, completo, explícito |

- El análisis y el diseño **no compilan**.
- **2.0:** se suma el *AI prompt*, otra vez en lenguaje natural (informal, incompleto, tácito).
- **El código es el único documento lo suficientemente preciso para construir el sistema.**

## Software development

Es un proceso de aprendizaje, lo que implica que:

- Es iterativo.
- Es incremental.
- El conocimiento se genera a partir de hechos concretos.
- El conocimiento generado debe ser organizado.

## Clean code

- El código es el principal documento de diseño.
- El mal código no se comenta, se arregla.
- Usar nombres que se puedan decir y buscar.
- Métodos: una sola cosa, 3-4 líneas.
- La duplicación es el origen de todos los males.
