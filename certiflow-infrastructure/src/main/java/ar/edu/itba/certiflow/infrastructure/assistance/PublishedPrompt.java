package ar.edu.itba.certiflow.infrastructure.assistance;

import ar.edu.itba.certiflow.assistance.model.PromptVersion;

/**
 * Las versiones publicadas del prompt de revisión, cada una con la suma SHA-256 de su texto (con fin de línea LF).
 * Una versión publicada no se edita: cambiar el prompt es agregar un archivo y una constante, como una migración.
 * Si alguien edita el texto de una versión, la suma deja de coincidir y la versión no carga.
 */
enum PublishedPrompt {

    V1("v1", "310c8b85d96a635952ff998dc8e5be03628cc516f7bb0cb0cafcf1c813064ac6");

    private final PromptVersion version;
    private final String checksum;

    PublishedPrompt(String version, String checksum) {
        this.version = new PromptVersion(version);
        this.checksum = checksum;
    }

    boolean is(PromptVersion candidate) {
        return version.equals(candidate);
    }

    boolean matches(String textChecksum) {
        return checksum.equals(textChecksum);
    }
}
