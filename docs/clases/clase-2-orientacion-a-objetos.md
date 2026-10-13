# Clase 2 · Orientación a objetos

ITBA 2026 · Fer Cargnelutti

## Recap: clean code

- El código es el principal documento de diseño.
- El mal código no se comenta, se arregla.
- Usar nombres que se puedan decir y buscar.
- Métodos: una sola cosa, 3-4 líneas.
- La duplicación es el origen de todos los males.

## Un poco de historia

| Año | Hito | Referente |
|---|---|---|
| 1843 | Primer algoritmo | Ada Lovelace |
| 1936 | Lambdas | Alonzo Church |
| 1936 | Máquina de Turing | Alan Turing |
| 1952 | Primer compilador | Grace Hopper |
| 1957 | Programación funcional: LISP | John McCarthy |
| 1966 | Orientación a objetos: Simula 67 | Ole-Johan Dahl y Kristen Nygaard |
| 1968 | Programación estructurada | Edsger W. Dijkstra |
| 1969 | Ingeniería de software | Margaret Hamilton |
| 1970 | GUI | Adele Goldberg |
| 1972 | C | Dennis Ritchie |
| 1973 | Orientación a objetos: Smalltalk | Alan Kay |
| 1974 | Abstracción de datos | Barbara Liskov |
| 1975 | Programación funcional: Scheme | Steele y Sussman |

## Paradigmas

| Paradigma | Principios |
|---|---|
| Estructurado | Secuencias, selecciones e iteraciones (sin `GOTO`) |
| Funcional | Funciones puras, variables inmutables, funciones de primera clase y de orden superior, recursión, transparencia referencial |
| Orientado a objetos | Abstracción, encapsulamiento, herencia, polimorfismo |

Ejemplo funcional de la clase:

```java
double calculateProductValue(Producto producto) {
    return producto.getPrecio() * producto.getStock();
}

double calculateFilteredInventoryValue(List<Producto> products, Predicate<Producto> filter) {
    final var filteredProducts = filterProducts(products, filter);
    final var filteredProductValues = transformProducts(filteredProducts, this::calculateProductValue);
    final double totalFilteredInventoryValue = sumValues(filteredProductValues);
    return totalFilteredInventoryValue;
}
```

Ejemplo orientado a objetos: `account.debit(100);`

## Dependencias

- **Sin inversión:** `Main` → módulos de alto nivel (HL) → nivel medio (ML) → bajo nivel (LL). Las dependencias de
  código fuente siguen el mismo sentido que el flujo de control.
- **Con inversión:** `CurrencyConverter` depende de la interfaz `HttpClient`, y `UniRestHttpClient` la implementa.
  El flujo de control va de `CurrencyConverter` a `UniRestHttpClient`, pero la dependencia de código fuente apunta
  al revés, hacia la interfaz.
- `Main` queda del otro lado del límite: crea `UniRestHttpClient` y `CurrencyConverter` y los conecta. Las
  dependencias de código fuente suben hacia la política; el flujo de control baja.

## Qué NO hacer: cada paradigma es una disciplina

| Año | Paradigma | Disciplina impuesta sobre |
|---|---|---|
| 1957 | Funcional | la asignación (mutabilidad) |
| 1966 | Orientado a objetos | la transferencia indirecta de control |
| 1968 | Estructurado | la transferencia directa de control |
