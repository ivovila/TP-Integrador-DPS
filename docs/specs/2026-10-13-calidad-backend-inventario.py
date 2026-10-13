import re, glob, os, subprocess, sys, collections
ROOT = sys.argv[1]
MODULES = ['domain', 'application', 'infrastructure', 'web']
PREFIX = 'src/main/java/ar/edu/itba/certiflow/'
STATIC = re.compile(r'^\s*(?:@\w+\s+)*(?:(?:public|private|protected)\s+)?static\s+(?!final\b|class\b|record\b|interface\b|enum\b)(?:<[^>]+>\s+)?[\w<>\[\],.? ]+?\s+(\w+)\s*\(')
def strip_strings(line):
    return re.sub(r'"(?:\\.|[^"\\])*"', '""', line)
def files(module):
    return sorted(glob.glob(f'{ROOT}/certiflow-{module}/src/main/java/**/*.java', recursive=True))
def short(path):
    return path.split(PREFIX)[1]
def scan():
    found = collections.defaultdict(list)
    sizes = {}
    for module in MODULES:
        count_files = 0; count_lines = 0
        for path in files(module):
            count_files += 1
            lines = open(path, encoding='utf-8').read().split('\n')
            count_lines += len(lines)
            in_equals = False
            for number, raw in enumerate(lines, 1):
                text = raw.strip()
                if text.startswith(('import ', 'package ', '*', '//', '/*')):
                    if text.startswith('import ') and text.endswith('.*;'):
                        found['wildcard'].append((module, path, number, text))
                    continue
                code = strip_strings(raw)
                where = (module, path, number, text)
                if 'boolean equals(Object' in code: in_equals = True
                if re.search(r'\bsealed (interface|class)\b', code):
                    found['sealed'].append(where)
                if re.search(r'\binstanceof\b', code):
                    key = 'instanceof'
                    if in_equals: key = 'instanceof_equals'
                    elif path.endswith('TransactionalUseCases.java'): key = 'instanceof_proxy'
                    found[key].append(where)
                if in_equals and text == '}': in_equals = False
                if re.search(r'\bswitch\s*\(', code): found['switch'].append(where)
                if re.search(r'::isInstance|::cast|\.getClass\(\)|getDeclaringClass\(\)|\.isInstance\(|\.cast\(', code): found['reflective'].append(where)
                if re.search(r'[=(,>]\s*\(\(?[A-Z][\w.]*(?:<[^>()]*>)?\)\s*[a-z(]', code) and not re.search(r'\)\s*->', code.split('(')[0]):
                    if not re.search(r'\((Function|Class|[A-Z]\w*Exception)\b', code): found['cast'].append(where)
                if STATIC.match(raw): found['static'].append(where)
                if re.search(r'^\s*static\s*\{', raw): found['static'].append(where)
                for _ in re.findall(r'\bnull\b', code): found['null'].append(where)
                if 'Optional.ofNullable' in code: found['ofNullable'].append(where)
                if 'requireNonNull' in code: found['requireNonNull'].append(where)
                if re.search(r'new Illegal(Argument|State)Exception', code): found['illegal'].append(where)
                if re.search(r'\.orElseThrow\(\)', code): found['bareOrElseThrow'].append(where)
                if 'required = false' in code: found['optionalParam'].append(where)
                if re.search(r'Map<(Class<|String,\s*(Function|Boolean|AuditAction))', code) or re.search(r'List<(Map\.Entry<Predicate|ValueCodec|RuleCodec)', code):
                    found['registry'].append(where)
                if re.search(r'\bObject\b', code) and not 'equals(Object' in code and not path.endswith('TransactionalUseCases.java'):
                    found['object'].append(where)
                if re.search(r'^\s*(?:\}\s*else\s+)?if\s*\(', raw):
                    following = ' '.join(l.strip() for l in lines[number:number + 3])
                    same = code.split('{', 1)[1] if '{' in code else ''
                    opens_block = code.rstrip().endswith('{')
                    body_first = next((l.strip() for l in lines[number:] if l.strip()), '')
                    if 'throw ' in same: pass
                    elif opens_block and body_first.startswith('throw '): pass
                    elif not opens_block and '{' not in code:
                        j = number
                        while j < len(lines) and not lines[j - 1].rstrip().endswith('{'): j += 1
                        nxt = next((l.strip() for l in lines[j:] if l.strip()), '')
                        if not nxt.startswith('throw '): found['if'].append(where)
                    else: found['if'].append(where)
                if re.search(r'\belse\b', code): found['else'].append(where)
                if re.search(r'^\s*(final\s+)?var\s+\w+\s*=', raw) or re.search(r'\(var \w+', raw): found['var'].append(where)
                if len(raw) > 150: found['long'].append(where)
        sizes[module] = (count_files, count_lines)
    return found, sizes
found, sizes = scan()
head = subprocess.run(['git', '-C', ROOT, 'rev-parse', '--short', 'HEAD'], capture_output=True, text=True).stdout.strip()
out = []
out.append(f'# Calidad del backend · Inventario mecánico\n')
out.append(f'Generado sobre el commit `{head}` de `entrega-2-spec1` con una búsqueda de texto sobre el código principal (`src/main/java`) de los cuatro módulos. Es el anexo de la [spec](2026-10-13-calidad-backend.md): sirve para tildar cada ocurrencia y para no volver a buscar. No incluye tests.\n')
out.append('Las rutas son relativas a `certiflow-<módulo>/src/main/java/ar/edu/itba/certiflow/`. Los números de línea valen para ese commit: la rama se mueve, así que conviene regenerar el inventario antes de empezar cada tarea y al cerrarla:\n')
out.append('```bash\npython3 docs/specs/2026-10-13-calidad-backend-inventario.py . docs/specs/2026-10-13-calidad-backend-inventario.md\n```\n')
out.append('## Resumen\n')
cols = [('sealed', '`sealed`'), ('instanceof', '`instanceof`'), ('switch', '`switch`'), ('reflective', 'tipo por reflexión'), ('cast', 'casts'), ('registry', 'registros'), ('static', 'estáticos'), ('null', '`null`'), ('illegal', '`Illegal*`'), ('bareOrElseThrow', '`orElseThrow()`'), ('if', '`if` sin `throw`'), ('object', '`Object`')]
out.append('| Módulo | Archivos | Líneas | ' + ' | '.join(label for _, label in cols) + ' |')
out.append('|---|---|---|' + '---|' * len(cols))
for module in MODULES:
    row = [str(sum(1 for w in found[key] if w[0] == module)) for key, _ in cols]
    out.append(f'| {module} | {sizes[module][0]} | {sizes[module][1]} | ' + ' | '.join(row) + ' |')
row = [str(len(found[key])) for key, _ in cols]
out.append(f'| **total** | {sum(s[0] for s in sizes.values())} | {sum(s[1] for s in sizes.values())} | ' + ' | '.join(row) + ' |')
out.append('')
out.append('`instanceof` no cuenta los que están dentro de `equals` ni los del proxy de transacciones, aceptados por el equipo; figuran aparte en la sección 2. Las columnas «casts», «registros», «`if` sin `throw`» y «`Object`» salen de una heurística: pueden traer algún falso positivo y hay que leer la línea.\n')
SECTIONS = [
    ('sealed', '1. Interfaces `sealed`', '2'),
    ('instanceof', '2. `instanceof`', '2 (alcance, base y alcance de inspección), 6 (`ApiExceptionHandler`), 11 (seguridad)'),
    ('instanceof_equals', '2.1 `instanceof` dentro de `equals` (aceptado)', 'ninguna'),
    ('instanceof_proxy', '2.2 `instanceof` del proxy de transacciones (aceptado)', 'ninguna'),
    ('switch', '3. `switch`', '2'),
    ('reflective', '4. Tipo consultado por reflexión (`isInstance`, `cast`, `getClass`)', '3 (`RawAnswer`), 5 (`SectionsJson`, `SchemaPayloads`, `AuditActions`), 6 (`ApiExceptionHandler`), 8 (bloqueo). Las de `TransactionalUseCases` están aceptadas'),
    ('cast', '5. Casts a un tipo concreto (heurística)', '2, 5, 10 (`DemoDataSeeder`)'),
    ('registry', '6. Registros por clase o por texto (heurística)', '3, 5, 6, 8, 10 (`CLOSED_BY_STATUS`)'),
    ('static', '7. Métodos y bloques estáticos', '1, 3, 5, 7, 8, 9, 10, 11'),
    ('null', '8. Literales `null`', '1, 2, 4, 9, 10, 11, 12'),
    ('ofNullable', '8.1 `Optional.ofNullable`', '4, 9, 10'),
    ('requireNonNull', '8.2 `Objects.requireNonNull`', '6'),
    ('optionalParam', '8.3 Parámetros de query opcionales', '10'),
    ('illegal', '9. `IllegalArgumentException` e `IllegalStateException`', '1, 6, 11'),
    ('bareOrElseThrow', '10. `orElseThrow()` sin excepción propia', '1, 6'),
    ('if', '11. `if` cuyo cuerpo no lanza (heurística)', '2, 9, 10, 11. El de `Finding.verifyAction` está aceptado (D8) y los de `TransactionalUseCases` también'),
    ('object', '12. `Object` en firmas o campos (heurística)', '3, 8, 10. El de `AllowedRoleInterceptor` es la frontera F4'),
    ('wildcard', '13. Imports con comodín', '13'),
    ('var', '14. `var`', '13'),
]
for key, title, tasks in SECTIONS:
    items = found[key]
    out.append(f'## {title}\n')
    out.append(f'{len(items)} ocurrencias. Tareas del plan: {tasks}.\n')
    if not items:
        out.append('Ninguna.\n'); continue
    grouped = collections.OrderedDict()
    for module, path, number, text in items:
        grouped.setdefault((module, short(path)), []).append((number, text))
    if key in ('static', 'null', 'ofNullable', 'object', 'if') and len(items) > 40:
        out.append('| Módulo | Archivo | Líneas |'); out.append('|---|---|---|')
        for (module, name), occurrences in grouped.items():
            numbers = []
            for number, _ in occurrences:
                if str(number) not in numbers: numbers.append(str(number))
            out.append(f'| {module} | `{name}` | {", ".join(numbers)} |')
    else:
        out.append('| Módulo | Ubicación | Línea |'); out.append('|---|---|---|')
        for (module, name), occurrences in grouped.items():
            for number, text in occurrences:
                shown = text.replace('|', '\\|')
                if len(shown) > 110: shown = shown[:107] + '...'
                out.append(f'| {module} | `{name}:{number}` | `{shown}` |')
    out.append('')
out.append('## 15. Líneas de más de 150 caracteres\n')
longs = collections.Counter((w[0], short(w[1])) for w in found['long'])
out.append(f'{len(found["long"])} líneas en {len(longs)} archivos. Tareas del plan: 13.\n')
out.append('| Módulo | Archivo | Cantidad |'); out.append('|---|---|---|')
for (module, name), count in sorted(longs.items(), key=lambda item: (-item[1], item[0])):
    out.append(f'| {module} | `{name}` | {count} |')
out.append('')
open(sys.argv[2], 'w', encoding='utf-8').write('\n'.join(out))
print({key: len(occurrences) for key, occurrences in found.items()})
