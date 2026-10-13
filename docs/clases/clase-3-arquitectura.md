# Clase 3 · Arquitectura

ITBA 2026 · Fer Cargnelutti

## Qué NO hacer: cada paradigma es una disciplina

| Año | Paradigma | Disciplina impuesta sobre |
|---|---|---|
| 1957 | Funcional | la asignación (mutabilidad) |
| 1966 | Orientado a objetos | la transferencia indirecta de control |
| 1968 | Estructurado | la transferencia directa de control |

## Clean Architecture

### Las responsabilidades del arquitecto

Decisiones que suelen atribuírsele: el paradigma de programación, el lenguaje de programación, los frameworks,
los patrones de diseño, la base de datos, la interfaz del sistema, el lenguaje de documentación, la metodología
de desarrollo y el hardware de despliegue.

> El rol del arquitecto es **posponer las decisiones el mayor tiempo posible**.

### Los valores del software

- **Estructura** y **funcionalidad**.
- *Software is meant to be soft*: mantener abiertas la mayor cantidad de opciones durante el mayor tiempo posible.

### Descomposición del software

- **Políticas:** las reglas de negocio.
- **Detalles:** todo lo demás (entrada/salida, base de datos, frameworks…).

### Planos y estructura de carpetas

La clase compara planos de arquitectura (un estadio, un restaurante), en los que se reconoce para qué es el
edificio, con dos estructuras de carpetas:

- Un proyecto MVC organizado por tipo de componente: `App_Data`, `App_Start`, `Content`, `Controllers`, `Filters`,
  `Images`, `Models`, `Scripts`, `Views`.
- Un proyecto organizado por casos de uso: `src/UseCases/` con `AddComment`, `AddDonation`, `AddSubscription`,
  `ApplyForMembership`, `CancelDonation`, `CheckIban`, `ConfirmSubscription`… junto a `DataAccess`, `Domain`,
  `Factories`, `Infrastructure`, `Presentation` y `ResponseModel`.

### Otra vez: dependencias

#### Ejemplo: cifrado César

Versión acoplada: la regla de negocio (`CesarCipher`) depende directamente de los detalles (`Scanner` y
`System.out`).

```java
public class CesarCipher {
    public void encrypt() {
        Scanner input = new Scanner(System.in);
        String textToEncrypt = input.nextLine();

        String encryptedText = cesarCipher(textToEncrypt);

        System.out.println("Original text: " + textToEncrypt);
        System.out.println("Encrypted text: " + encryptedText);
    }

    private String cesarCipher(String text) {...}
}
```

#### Depender de interfaces

```java
public class CesarCipher {
    private final Reader reader;
    private final Writer writer;

    public CesarCipher(Reader reader, Writer writer) {
        this.reader = reader;
        this.writer = writer;
    }

    public void encrypt() {
        String textToEncrypt = reader.readLine();
        String encryptedText = cesarCipher(textToEncrypt);
        writer.writeLine("Original text: " + textToEncrypt);
        writer.writeLine("Encrypted text: " + encryptedText);
    }

    private String cesarCipher(String text) {...}
}
```

> **Los detalles dependen de las reglas de negocio.**

#### Detalles y políticas

Los detalles implementan las interfaces que define el negocio:

```java
package edu.cleanarch.cesarcipher.io;

import edu.cleanarch.cesarcipher.entities.Reader;
import java.util.Scanner;

public class ConsoleReader implements Reader {
    @Override
    public String readLine() {
        return new Scanner(System.in).nextLine();
    }
}
```

```java
package edu.cleanarch.cesarcipher.io;

import edu.cleanarch.cesarcipher.entities.Writer;

public class ConsoleWriter implements Writer {
    @Override
    public void writeLine(String line) {
        System.out.println(line);
    }
}
```

#### El componente Main

`Main` es el detalle de más bajo nivel: crea los detalles concretos y los entrega a la regla de negocio.

```java
package edu.cleanarch.cesarcipher.main;

import edu.cleanarch.cesarcipher.entities.CesarCipher;
import edu.cleanarch.cesarcipher.entities.Reader;
import edu.cleanarch.cesarcipher.entities.Writer;
import edu.cleanarch.cesarcipher.io.ConsoleReader;
import edu.cleanarch.cesarcipher.io.ConsoleWriter;

public class Main {
    public static void main(String[] args) {
        Writer writer = new ConsoleWriter();
        Reader reader = new ConsoleReader();

        CesarCipher cipher = new CesarCipher(reader, writer);
        cipher.encrypt();
    }
}
```

#### Detalles y negocio

| Lado | Elementos | Depende de |
|---|---|---|
| Negocio (`entities`) | `CesarCipher`, interfaces `Reader` y `Writer` | nada externo |
| Detalles (`io`) | `ConsoleReader`, `ConsoleWriter` | implementan `Reader` y `Writer` |
| Detalles (`main`) | `Main` | crea `CesarCipher`, `ConsoleReader` y `ConsoleWriter` |

Todas las dependencias cruzan el límite hacia el negocio; ninguna sale de él.
